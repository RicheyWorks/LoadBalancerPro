# LoadBalancerPro — Build Plan: Lab, Shadow & Analysis Subsystems

**Companion to:** `AUDIT_LAB_SHADOW_2026-07-21.md` (defect IDs L#/C#/S#/E#/O# below come from that document) and `BUILD_PLAN_DEPLOYABLE.md` (the proxy roadmap; its milestones are referenced where they interlock).

**Framing.** The proxy build plan makes the load balancer *real*. This plan decides the fate of the other ~96% of the codebase. It has three distinct goals, and every PR serves exactly one:

- **SECURE & STABILIZE** — close the defects that are dangerous *today* (unauthenticated destructive endpoints, a remote-OOM endpoint, a CLI that can scale down AWS by accident, cockpit XSS, ledger torn reads, orphan-ASG cost risk).
- **CONSOLIDATE & QUARANTINE** — collapse the five duplicate compare/explain stacks and the four duplicate JSONL stores into one each, and move test tooling out of the production jar. Target: delete ~30k lines with zero capability loss.
- **WIRE TO LIVE TRAFFIC** — the payoff. Feed the real `RoutingDecision` + per-upstream telemetry the proxy produces (once `BUILD_PLAN_DEPLOYABLE.md` Milestone 1 lands) into the survivors: shadow evaluation, explainability, and the traffic-share orchestrator. This is what turns dormant machinery into product.

Sequencing rule: **Milestone L0 (security) can and should land immediately**, in parallel with the proxy's Milestone 0. Consolidation (L1–L2) should precede wiring (L3) so we wire *one* clean component, not five. Live-wiring (L3) depends on proxy Milestone 1 (live telemetry, PR-1.2/1.3) existing.

---

## Milestone L0 — Secure & stabilize (land now, parallel to proxy M0)

### PR-L0.1 Default-deny security posture (closes S1; shared with proxy D9)
- **Change:** this is the *same* fix as proxy `PR-0.5` — do it once and both plans depend on it. `ProdApiKeyFilter` no longer `@Profile`-gated; active whenever auth-mode=api-key; app fails startup in api-key mode with no key unless `auth-mode=none` is explicit. Add explicit matchers so `/api/enterprise-lab/**`, `/api/evidence-training/**`, `/api/remediation`, `/api/scenarios/replay` require `allocationRole` (they currently fall through to `authenticated()`), and destructive lab endpoints (`/durable/retention`, `/durable/*/compact`) require an admin role.
- **Files:** `api/config/ApiSecurityConfiguration.java`, `api/config/ProdApiKeyFilter.java`, `application.properties`.
- **Accept:** default profile + no key → refuses to start; `POST /api/lab/.../durable/retention` returns 401/403 without credentials; role matrix test covers every `/api/**` prefix.

### PR-L0.2 Cap explorer input size (closes E1 — critical remote OOM)
- **Change:** `@Size(max=…)` on the server/candidate list in `RoutingServerStateInput` (default cap e.g. 32; configurable `loadbalancerpro.api.max-candidates`), and a hard cap on strategies-per-request. Reject over-cap with 400 *before* building any payload. Add a response-size guard on the decision-explorer path.
- **Files:** `api/RoutingController.java`, `api/RoutingComparisonService.java` (`:158`), the input DTO.
- **Accept:** the audit's PoC (n=100, 5 strategies) returns 400, not multi-GB heap; new CI test asserts the cap at the large-N boundary (the current perf test uses n=3 and misses this).

### PR-L0.3 CLI scale-down sentinel + prompt-abort safety (closes O1)
- **Change:** `promptForInt` returns a dedicated sentinel (or `OptionalInt.empty()`) on abort, distinct from any valid value; `LoadBalancerCLI.java:757` treats abort as "cancel, no action." Audit every caller of `promptForInt` for the same -1-in-range trap.
- **Accept:** aborting the Scale Cloud prompt performs zero mutation; unit test for abort → no `scaleServersAsync` call.

### PR-L0.4 Cockpit XSS (closes O4)
- **Change:** add the shared `escapeHtml` helper (already present in two other pages) to `enterprise-lab.html` and `enterprise-lab-reviewer.html`; escape every server-derived value at the 5 + 1 `innerHTML` sinks, or switch to `textContent`/`createElement`.
- **Accept:** a guardrail-reason / event-id containing `<img onerror>` renders inert; manual check of both pages against a malicious fixture.

### PR-L0.5 Orphan-ASG guard (closes L7 — cloud cost risk)
- **Change:** ASG name must be **deterministic and recoverable** — derive from `resourceNamePrefix + environment` (stable across restarts), or persist the generated name to the trusted evidence dir and reload it on startup. `canDeleteCloudResources` matches on the stable name + ownership tag. Add a startup reconciliation that lists ASGs by ownership tag and adopts/cleans prior-run groups.
- **Files:** `core/CloudConfig.java:99-100`, `core/CloudManager.java` (describe/delete/ownership).
- **Accept:** restart re-discovers the prior run's ASG; simulated killed-process ASG is adoptable, not orphaned. (Test with mocked AWS clients — `liveMode=false` path.)

### PR-L0.6 Ledger torn-read fix (closes C1 — the "cross-process coordination" bug)
- **Change:** two parts. (1) Make each event frame a **single atomic write** (remove the 256-byte chunk splitting in `EnterpriseLabApplicationCommandLedger.java:56-772` and the supervisor twin) so a reader never sees a half-frame; if a single `write()` can still short-write, hold a shared file-region read lock for cross-process reads. (2) In `replayLocked`, distinguish a *transient* tail (last line incomplete, no newline) from genuine corruption — retry-on-transient with a short backoff before classifying `TRUNCATED_TAIL`, and never escalate a transient to `SUPERVISOR_RECONCILIATION_FAILED`.
- **Files:** `lab/EnterpriseLabApplicationCommandLedger.java`, `lab/EnterpriseLabSupervisorCommandLedger.java`, `lab/EnterpriseLabSupervisorAllocationBridge.java`, `lab/EnterpriseLabSupervisorService.java`.
- **Accept:** new test — two live JVMs, one appending continuously while the other replays in a tight loop for N seconds → zero false `TRUNCATED_TAIL`/`CONCURRENT_CHANGE`; genuine mid-frame corruption still detected.

**L0 exit:** nothing in the lab/shadow/analysis surface is remotely dangerous in the default profile, the cloud integration can't orphan billable resources, and the ledger no longer fails healthy systems.

---

## Milestone L1 — Consolidate the analysis layer (delete ~20k lines)

Do this *before* live-wiring so we wire one clean explainability module, not five stacks.

### PR-L1.1 Delete the metadata-about-metadata services (N1)
- **Change:** remove `RoutingDecisionReplayEvidenceNullSafetySummaryService`, `FieldInventoryService`, `BoundarySummaryService`, `StatusRollupService`, the five `Lane*SummaryService`s, and the four `Reviewer*Service`s, plus their DTOs and the ~30 `*DocumentationTest`s that assert prose. Strip their fields from the response objects.
- **Accept:** build green; the DecisionExplorer payload shrinks by an order of magnitude; no remaining service references the deleted ones. (~6k lines out.)

### PR-L1.2 Collapse Replay + DecisionExplorer into one explainability module
- **Change:** define one `RoutingExplanation` result carrying the *non-derivational* content only: per-candidate factor contributions (dominant + delta analysis) and counterfactual ±weight scenarios. Delete the ~20-object derivational chain (snapshot→trace→capsule→…→closure) and the parallel confidence/diagnostics/tradeoff/shadow restatements — keep a single confidence score and a single tradeoff summary if they carry real signal. `/api/routing/decision-explorer` returns the new compact shape.
- **Files:** new `api/explain/RoutingExplanationService.java`; retire `DecisionExplorer*` (80 files) and `RoutingDecisionReplay*` (54 files) down to the survivors.
- **Accept:** the counterfactual weight-scenario test and dominant/delta factor tests still pass against the new module; a golden-payload test documents the new (much smaller) contract. (~12–14k lines out.)

### PR-L1.3 Fix explainability correctness (E2) + determinism (E3, E4)
- **Change:** compute factor contributions using **each strategy's own score model**, not always `ServerScoreCalculator` — the explanation must reflect what the strategy actually optimized (weights for WRR, load formula for least-load, empty/positional for RR). Make `/compare` deterministic: either use per-request fresh strategy instances (aligns with proxy PR-0.3) or exclude cross-request cursor state from the fingerprint; seed tie-break RNG deterministically from the request. Fix fingerprint delimiter injection (length-prefix or hash-per-field instead of `join`).
- **Accept:** for a WRR decision the reported factors reconcile with the strategy's selection; two identical `/compare` requests return identical `chosenServerId` and fingerprint; `a,b` vs `a`+`b` no longer collide.

### PR-L1.4 Collapse the duplicate compare/experiment surfaces
- **Change:** route `RoutingComparisonService`, `AdaptiveRoutingExperimentService`, and `AdaptiveRoutingScenarioRunner` through the single `core/RoutingComparisonEngine`. Keep one experiment/fixture entry point; delete `AdaptiveRoutingStrategyComparisonMatrixBuilder` and the self-referential `GateEvaluator` (L14) or reduce it to a real assertion. Merge the three pressure classifiers (`FailureScenarioRunner`, `ShadowAutoscaler`, `LoadSheddingPolicy` predicates) into one.
- **Accept:** one comparison code path; experiment/scenario endpoints produce equivalent output via the shared engine; pressure-classification tests consolidated.

**L1 exit:** one explainability module, one comparison engine, one pressure classifier — correct and deterministic. Estimated ~20k lines removed.

---

## Milestone L2 — Consolidate & quarantine the lab (`lab/`)

### PR-L2.1 One JSONL store engine (fixes C2 asymmetry structurally)
- **Change:** extract a single `ChainedJsonlStore` (bounded, hash-chained, OS-locked, inode+creationTime identity, atomic single-frame append from PR-L0.6) and re-express `ApplicationCommandLedger`, `SupervisorCommandLedger`, `AllocationStateStore`, and `LocalJournal` on top of it. The supervisor lock automatically inherits the correct inode identity check (C2 fixed by construction).
- **Accept:** all four stores' existing tests pass against the shared engine; a lock-file-deletion split-brain test (delete lock file, second locker) is refused. (~2.5–3k lines out.)

### PR-L2.2 Rotation & recovery for ledgers/state store (C3, C6)
- **Change:** add compaction/rotation to the shared engine (ledgers and state store, not just journals) — archive-and-truncate at a soft threshold, keeping the hash chain across a rotation boundary. Add `.installing`/temp-file cleanup on startup (C6) so an orphaned temp doesn't brick listing endpoints. Provide an operator repair command for a bricked chain.
- **Accept:** 10k-command soak never hits `EVENT_LIMIT_EXCEEDED`; an injected orphan `.installing` file is cleaned on boot and endpoints stay healthy.

### PR-L2.3 Takeover honors the OS lock (C5)
- **Change:** at takeover, if the taker holds the exclusive OS `FileLock`, treat an unexpired lease as stale (the lock proves the prior single-host owner is dead) instead of refusing and crashing the app. Keep lease semantics for the genuinely-multi-host case behind an explicit flag.
- **Accept:** `kill -9` + immediate restart → clean single takeover, no crash-loop; multi-writer exclusion still holds.

### PR-L2.4 Move proof runners out of the production jar
- **Change:** relocate the five proof runners + reports + exporters (~7.2k lines) to `src/test` or a separate `lab-tools` Maven module; remove their CLI early-exit dispatch from `LoadBalancerApiApplication` (or gate behind a `-tools` classifier artifact). Drop `javafx-controls` to `provided`/`runtime` scope so it leaves the server jar.
- **Accept:** production jar no longer contains proof-runner or JavaFX classes; the proofs still run in CI from their new home.

### PR-L2.5 Durability honesty (C7) + logging (diagnosability)
- **Change:** fsync parent directories on create/rename/delete so `FORCE_DATA_AND_METADATA` receipts don't outlive their directory entries; add real logging to the lab storage classes (currently zero) so a corruption is diagnosable without a debugger; evict `PROCESS_MUTEXES` entries (C8).
- **Accept:** a crash-consistency test (fsync fault injection) no longer produces a receipt for a lost entry; log output present on every failure path.

**L2 exit:** the durability engineering worth keeping is deduplicated, rotatable, recoverable, honest, and diagnosable; test tooling is out of the shipped artifact.

---

## Milestone L3 — Wire the survivors to live traffic (the payoff)

**Depends on `BUILD_PLAN_DEPLOYABLE.md` Milestone 1 (PR-1.2 runtime stats, PR-1.3 live telemetry).** Until the proxy produces real per-upstream latency/error/in-flight data and captures its real `RoutingDecision`, these components have nothing real to consume.

### PR-L3.1 Capture real proxy decisions
- **Change:** in `ReverseProxyService.forward`, after a routing decision + response, emit a `LiveRoutingDecisionRecord` (chosen upstream, candidate states from PR-1.2 runtime stats, actual latency/status) into a bounded ring buffer / event stream.
- **Accept:** `GET /api/proxy/decisions/recent` returns real decisions from live traffic; bounded memory.

### PR-L3.2 Feed real telemetry into LASE shadow (fixes L1–L4)
- **Change:** replace the fabricated telemetry in `LaseShadowAdvisor.java:279-290` with the real per-upstream latency/error/in-flight from PR-1.2. Fix the unit mismatch (L3 — pass ratios, not load units), close the AIMD feedback loop by persisting limiter state across evaluations (L2), and compute agreement against the *same* candidate set the proxy actually chose from (L4). Run shadow evaluation async off the request path (L13).
- **Accept:** shadow recommendations track real backend degradation; agreement rate is interpretable; no per-request latency added.

### PR-L3.3 "Explain this actual request" endpoint
- **Change:** point the consolidated `RoutingExplanationService` (L1.2) at `LiveRoutingDecisionRecord`s instead of client-supplied synthetic candidates. `GET /api/proxy/decisions/{id}/explain` returns dominant-factor + counterfactual analysis for a real decision, using the correct per-strategy score model (L1.3).
- **Accept:** explaining a real proxied request shows why *that* upstream was chosen and what weight/telemetry shift would have changed it.

### PR-L3.4 Actuate the traffic-share orchestrator (optional, gated)
- **Change:** the `AdaptiveTrafficDecisionOrchestrator` (the best code in the audit) currently only records decisions. Behind a default-off `loadbalancerpro.lase.policy.mode=active-experiment` flag with the existing guardrails, let it adjust per-upstream weights on the live proxy within movement caps — a real, guardrailed adaptive-routing capability. Wire `ShadowAutoscaler` recommendations to the (now-safe, L5–L7-fixed) `CloudManager.scaleServersAsync` similarly gated.
- **Accept:** with the flag on in a test harness, sustained backend degradation shifts weight away from the bad upstream within guardrail limits; flag off = today's record-only behavior exactly.

**L3 exit:** shadow evaluation, explainability, and adaptive allocation all operate on *real* traffic — the machinery that was 96% dormant becomes a genuine adaptive-routing + observability product.

---

## Milestone L4 — Operator surface cleanup

- **PR-L4.1** Fix or retire the CLI (O0 jar wiring so `--remediation-report` works from the shipped jar, or correct the docs to `java -cp`; consistent `--force` overwrite policy O5; fix inverted idle-timeout; remove dead undo persistence + add `ObjectInputFilter` for O3, or delete the interactive menu entirely and keep only the evidence tooling).
- **PR-L4.2** Delete or fix the JavaFX GUI (O2/O3). Recommendation: delete — all action buttons are runtime-broken and it drags a CVE surface into the server jar; the cockpit HTML pages already cover the UI need.
- **PR-L4.3** Unify jar-selection across `operator-distribution-smoke.sh` / `.ps1` / Docker / CI (O5) and de-hardcode the version.
- **PR-L4.4** Consolidate the five ~600–1080-line evidence-viewer cockpit pages into one parameterized page + a shared `lib.js`; archive ~80% of `docs/` (the PLAN/ROLLUP/LANE/CLOSURE ceremony) into a `docs/archive/`, keep the ~30 operator docs, refresh the stale evidence stubs (v1.9.0, empty performance baseline).

---

## Sequencing & dependency graph

```
L0 (security/stabilize): L0.1 L0.2 L0.3 L0.4 L0.5 L0.6   ← land NOW, parallel to proxy M0; independent
L1 (consolidate analysis): L1.1 → L1.2 → L1.3 → L1.4      ← before L3
L2 (consolidate lab):     L2.1 → L2.2 → L2.3 → L2.4 → L2.5 (L2.1 needs L0.6's atomic-append)
L3 (wire live):           needs proxy PR-1.2/1.3 → L3.1 → L3.2, L3.3 (need L1.2/L1.3) → L3.4 (needs L0.5, L2 cloud fixes)
L4 (operator):            L4.1 L4.2 L4.3 L4.4               ← independent, any time after L0
```

**Recommended order for Codex:** land all of L0 first (six small, high-value security/stability PRs — several overlap the proxy plan's M0, so coordinate `L0.1`=`proxy PR-0.5`). Then L1 to shrink the analysis surface before wiring, L2 in parallel to dedup the lab. Hold L3 until the proxy's live-telemetry PRs exist — that's the dependency that unlocks the entire "wire to real traffic" payoff. L4 is cleanup, fit it in opportunistically.

**Definition of done for this plan:** L0 complete (nothing dangerous by default) + L1/L2 complete (~30k lines removed, one of each core abstraction) + at least L3.1–L3.3 (shadow + explainability operating on real proxy decisions). L3.4 and L4 are stretch.

**Estimated net effect:** roughly **-30k lines** (deletions/consolidation) and **+~3k lines** (live-wiring), leaving a codebase where the proxy is real (first plan), the analysis layer explains real decisions, and the lab is a lean, correct durability harness rather than 41k lines of self-validating ceremony.
