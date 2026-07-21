# LoadBalancerPro — Deep Audit: Lab, Shadow & Analysis Subsystems

**Repo:** RicheyWorks/LoadBalancerPro · commit `e800ba06` (post-v2.5.0)
**Date:** 2026-07-21
**Companion to:** `AUDIT_2026-07-21.md` (the live proxy + basic simulation core). This document covers everything that audit deliberately set aside: the LASE shadow/adaptive experiment cluster, the Enterprise Lab apparatus (`lab/`), the DecisionExplorer/Replay/Evidence API surface, and the CLI/GUI/demo/cockpit/docs surfaces.

Defect IDs here are namespaced by area (LASE `L#`, Lab `C#`/`S#`, Explorer `E#`, Operator `O#`) so they don't collide with the proxy audit's `D#`. All findings verified against source; representative file:line references included.

---

## 1. Executive summary

If the first audit's headline was "the real load balancer is 3.5% of the codebase," this one explains the other ~96%. It divides cleanly:

- **~40k lines (`lab/`)** — an offline crash-recovery / durable-evidence harness that arms a routing decision over three fake loopback backends (`http://127.0.0.1:1`), evaluates it, and records everything in hash-chained, fsync'd, OS-locked evidence files. The durability engineering is real and some of it is genuinely good; but roughly half is duplication and proof-runners-proving-proof-runners, and it ships integration-test tooling inside the production jar.
- **~24k lines (DecisionExplorer + RoutingDecisionReplay + Evidence services in `api/`)** — a analysis layer that takes one routing-comparison result and restates it through ~40 derived "evidence" objects. About 90% is derivational restatement; there is a service whose sole job is to report whether *other response objects* have null fields.
- **~10k lines (LASE shadow + Adaptive experiment + Cloud in `core/`)** — a "shadow evaluation" pipeline plus real AWS autoscaling integration. The plumbing is production-grade but it evaluates **fabricated telemetry** (latency/error rates synthesized from load score), so its output is currently hollow; the AWS side has several dangerous-when-enabled defects.

**The single most important structural finding:** the same idea — "compare/explain routing strategies on synthetic traffic" — is implemented **at least five times** (LASE agreement tracking, AdaptiveRoutingExperiment fixtures, AdaptiveRoutingScenario matrix, `core/RoutingComparisonEngine`, and the DecisionExplorer/Replay stack). Consolidating these would delete an estimated **20,000+ lines with zero capability loss**, and — more valuably — the pieces worth keeping (factor-level explainability, counterfactual weight scenarios, shadow evaluation) become genuinely useful the moment they're fed **real proxy decisions** instead of synthetic inputs. That's the throughline of the build plan: *delete the ceremony, wire the survivors to live traffic.*

**Security note that spans all three areas:** in the default profile the Spring security chain is `anyRequest().permitAll()` and the only credential filter is `@Profile("prod","cloud-sandbox")` — so on a default run, the destructive lab endpoints (`POST /api/lab/.../retention` can delete all evidence), the DoS-amplifying explorer endpoint, and the status surfaces are all unauthenticated on 0.0.0.0:8080. This is the same root cause as the proxy audit's D9 and is the top cross-cutting fix.

---

## 2. LASE shadow / adaptive / cloud cluster (`core/`)

**What it is.** After each load distribution, `LaseShadowAdvisor.observe()` synthesizes an evaluation input, runs five sub-evaluators (routing choice, concurrency AIMD, load shedding, autoscaling, failure scenario), records a bounded (100) synchronized event, and compares its pick to the actual allocation. Off by default everywhere (`loadbalancerpro.lase.shadow.enabled=false` in all three property files — verified). Reachable via `LoadBalancer` when the flag is set, or `GET /api/allocator/lase/shadow`. The Adaptive policy engine (`off|observe|shadow|recommend|active-experiment`) gates whether a recommendation may replace baseline — but **no caller ever applies a passing decision to traffic**; `AdaptiveTrafficDecisionOrchestrator` literally appends "decision record only; no traffic action performed."

### Verified defects

**L1 (High, feature-hollow). Shadow telemetry is fabricated, not measured.** `LaseShadowAdvisor.java:279-290` computes `averageLatency = 50.0 + loadScore`, `p95 = avg + 40 + queueDepth*0.5`, `errorRate = healthy ? min(0.10, loadScore/1000) : 0.30`. Every downstream signal (concurrency, shedding, autoscaling, failure) is thus a linear transform of `Server.getLoadScore()`. The advisor never receives real latency or error data, even on the live path. The entire evaluation output is decorative.

**L2 (High, feature-broken). The AIMD concurrency limiter's feedback loop never closes.** `LaseEvaluationEngine.java:37-39` constructs a fresh `AdaptiveConcurrencyLimiter` per call and discards `nextLimit` (used only in report strings); `currentConcurrencyLimit` is re-derived from server capacities every time. The "adaptive" limit never walks — same transition reported every event, nothing enforces it.

**L3 (High→Med, wrong analytics). Utilization unit mismatch.** `LaseShadowAdvisor.java:246-275` feeds *load units* (e.g. 300.0) where `AutoscalingSignal.utilization()` expects a 0–1 ratio (`inFlight/capacity`), and clamps the concurrency limit to `min(100, …)` regardless of real capacity. Result: 3 servers + load 100 → "utilization 33.3 exceeded threshold 0.85" → SCALE_UP always; nearly every load classifies as CAPACITY_SATURATION.

**L4 (Med). Agreement rate is statistically meaningless.** `:131-135` compares the argmax of a full distribution against a `TailLatencyPowerOfTwo` pick that samples only 2 random candidates — expected agreement ≤ ~2/N even for a perfect strategy. The headline metric is uninterpretable.

**Cloud (real AWS integration) — dangerous when enabled:**

**L5 (High, live+autonomous). `predictCapacityWithAI` is nonsense with a NaN path.** `CloudManager.java:431-440`: `totalLoad / (totalLoad/n * 1.5)` = `n/1.5`, independent of load; `totalLoad==0` → `0/0=NaN` → `ceil(NaN)`→0; fetches `getCurrentCapacity()` (a live AWS call) and discards it.

**L6 (High, live+autonomous). `preemptiveInstancePooling` is an unbounded capacity ratchet.** `:651-662` adds `preemptivePoolSize` every 300s forever with no scale-down and no demand check, until it hits `maxDesiredCapacity`. Pure cost burn.

**L7 (High, live). Orphaned billable ASGs.** `CloudConfig.java:99-100` regenerates the ASG name with `UUID.randomUUID()` per process; `canDeleteCloudResources` requires the tag to equal the *current* name. A killed process's ASG (with running EC2 instances) can never be seen or deleted by any later run → indefinite billing. This is the standout cloud risk.

**L8–L16 (Med/Low, live).** Metric-cache TTL mismatch causing uncached CloudWatch double-fetches (`:953-955`); resource/thread leak + accumulating shutdown hooks on failed cloud init (`:76-83`); live init holding the `LoadBalancer` write lock for up to 5 min (`LoadBalancer.java:102-120`); memory/disk metrics fabricated from CPU (`:410-416`); nested retry amplification (~9 attempts/metric); synchronous LASE evaluation on the request path; brittle gate evaluator hard-coded to `EXPECTED_TOTAL_DECISIONS=100`; a replay format with **no producer** (the export endpoint emits a different JSON shape than `--lase-replay` consumes).

**Safe-by-default confirmed:** live AWS requires `liveMode=true` AND `allowLiveMutation=true` AND `operatorIntent=LOADBALANCERPRO_LIVE_MUTATION` AND account-ID allow-list AND region allow-list AND non-zero caps — all default off/empty. Credentials are static keys from props/env (no default-credential-chain), held as plain strings with public getters (mild exposure). Event logs and audit logs are bounded and synchronized (no unbounded per-request growth). Experiment/replay determinism holds (fixed clocks, seeded RNG).

**Value verdict:** `AdaptiveTrafficDecisionOrchestrator` + `LoadDistributionPlanner.recommendTrafficShares` + `TrafficAllocation` guardrails are the **best code in this audit** — real evidence-windowing, score→share allocation with movement caps, feasibility-checked projection. It only ever sees fixed-clock fixtures; it could genuinely drive weighted allocation of live traffic. The decision functions (concurrency limiter, load-shedding policy, shadow autoscaler, failure classifier) are clean in isolation but none is actuated, and the failure/shedding/autoscaler triplicate the same threshold-pressure logic three times.

---

## 3. Enterprise Lab apparatus (`lab/`, ~41.6k lines)

**What it is.** A self-contained offline crash-recovery/evidence harness. "Experiments" arm an adaptive allocation decision over three fake backends, drive bounded HTTP GETs at literal loopback stubs (`127.0.0.1:1`, deliberately unroutable), evaluate/roll back, and record everything in hash-chained durable evidence. Moving parts: evidence ownership (OS `FileLock` + lease + inode/generation fencing, ~3.3k lines), an optional separate supervisor JVM over loopback TCP (~4k), per-side append-only hash-chained JSONL command ledgers, per-experiment journals with compaction/quarantine, an allocation transaction coordinator with restart reconciliation, and five "proof runners." Invoked via CLI early-exit flags and `/api/lab/**`; zero GUI usage.

### Verified defects

**C1 (Med-High). Cross-process torn reads are structurally guaranteed.** `EnterpriseLabApplicationCommandLedger.java:56` sets `MAX_WRITE_CHUNK_BYTES = 256` (verified) and deliberately splits each ~600–1000-byte event line into ≤256-byte `write()` calls, so a frame is visible half-written between chunks. The peer process reads the file **lock-free** (`SupervisorAllocationBridge.java:593-606`, `SupervisorService.java:985`) and `replayLocked` then throws `TRUNCATED_TAIL` — the *same* code used for genuine crash damage — which escalates to `failAllocationAdmission("SUPERVISOR_RECONCILIATION_FAILED")`. So allocation admission fails on a healthy system with a corruption-shaped reason. This is the exact "cross-process coordination" the recent Codex PRs claim to have solved, and it is not sound for concurrent reader/writer. No test exercises two live processes appending/reading one file.

**C2 (Med). Supervisor lock is vulnerable to lock-file-deletion split-brain.** `EnterpriseLabSupervisorOwnership.java:90-98` checks only `lock.isValid()` and stats the path — it never compares the locked channel's inode to the file currently at the path. Delete `supervisor.lock` (e.g. a `target/` cleanup) → supervisor A's lock on the unlinked inode stays valid, supervisor B recreates and locks the new inode, both pass `requireHeld()`, both append → chain break → ledger permanently bricked with no repair path. The application side got the correct inode+creationTime identity check; the supervisor side didn't — the asymmetry itself is evidence of copy-paste divergence.

**C3 (Med). Fail-stop at hard caps with no rotation and no recovery.** Ledgers and the allocation state store cap at 8MB/4096 events and then throw `EVENT_LIMIT_EXCEEDED` forever. Journals have compaction; **the ledgers and state store have none**. After ~500–1000 lifetime commands, every future experiment fails permanently; the only remedy is manually deleting files the system treats as tamper-evident. "Bounded by design" here means "designed to brick."

**C5 (Med, ops). Restart within the lease window crash-loops the whole app.** `classifyPrior` (`:445-449`) refuses takeover on an unexpired lease even when the taker holds the exclusive OS lock (which on a single host proves the prior owner is dead); the controller then throws from bean creation and the app exits. `kill -9` + fast systemd restart → multi-restart outage of the entire API (not just the lab) for up to 30s.

**C6 (Med-Low). Orphaned temp files brick listing endpoints.** A crash between write and `ATOMIC_MOVE` leaves a `.installing` temp; `compactedManifests()` then throws `VERIFICATION_FAILED` for *every* future call, and `replayedExperimentStates` throws on any non-VERIFIED journal — one stray file prevents the app from booting in allocation mode. No cleanup path exists.

**C4/C7/C8 (Low).** Three full-file replays per append (O(n²) decode+re-encode+SHA per append); directory entries not fsync'd despite `FORCE_DATA_AND_METADATA` receipts (a durability lie the codebase spends thousands of lines claiming to preclude); static `PROCESS_MUTEXES` maps never evicted.

**S1 (High, config-dependent). `/api/lab/**` unauthenticated in the default profile.** Same root cause as proxy-audit D9. Anyone on the network can arm/cancel experiments, `POST .../durable/retention {maximumTerminalJournals:0, dryRun:false}` (deletes all terminal journals), and hammer `GET .../durable` which re-verifies up to 256×16MB of journals per request — a cheap CPU/IO DoS. Prod profile fails closed correctly.

**Path traversal: none** — experiment IDs are whitelisted then SHA-256-hashed into filenames, with `NOFOLLOW_LINKS` + `toRealPath` parent checks. This part is genuinely well done. Supervisor TCP is loopback-pinned with a 64-byte SecureRandom credential and constant-time compare — sound.

**Engineering quality.** `SupervisorCommandLedger` is a ~90% verbatim copy of `ApplicationCommandLedger` (~1,100 duplicated lines); `AllocationStateStore` is a third copy of the same JSONL engine and `LocalJournal` a fourth variant — C2's inode-check asymmetry is the live cost of that copy-paste. The five proof runners are **7,250 lines of integration-test tooling shipped in the production jar** and reachable via production CLI flags. Of the 41.6k lines, ~18k is substance; ~23k is duplication (~3k), proof tooling in main (~7k), and layered evidence/receipt ceremony (~13k) whose failure mode is validating its own validators. Tests are bimodal: the `Enterprise*` family (real second-JVM forking, torn-tail injection, tamper rejection) is strong; the `LocalLab*` family (~96 files) is ~75–80% ceremony — 28 tests assert markdown files contain specific English sentences, 18 assert source strings don't contain certain words.

---

## 4. DecisionExplorer / Replay / Evidence API (`api/`, ~24k lines)

**What it is.** Every service here is a pure reshaper of one `RoutingComparisonResponse` (which runs `core/RoutingComparisonEngine` over client-supplied synthetic `ServerStateVector`s). For each result it chains ~20 derived "evidence" objects (snapshot → trace → capsule → readiness-checklist → source-map → field-inventory → null-safety-summary → … → closure-checklist), and DecisionExplorer adds a parallel confidence→diagnostics→tradeoff→shadow→counterfactual chain. Stateless per request (good) — except the shared strategy registry carries cross-request state (see E3).

### Verified defects

**E1 (Critical DoS). Uncapped candidate list × ~O(n²) payload amplification.** `RoutingComparisonService.java:158` validates only non-empty — no upper bound, no `@Size`/`@Max` (verified). A ~150-byte server entry means ~100 candidates fit in the 16KB body cap. Measured against `target/classes`: n=10 → 8.2MB payload; n=50 → 141MB; **n=100 → 533MB per strategy**, and with 5 default strategies that's multi-GB heap in one request. Amplification ≈ 16KB in → >500MB out (~32,000×). Unauthenticated in the default profile, rate-limiting off by default → trivial remote OOM. CI doesn't catch it (the perf test uses 3-server inputs).

**E2 (High, wrong analytics). Explainability fuses two unrelated scoring models.** `RoutingComparisonService.java:687-739` always computes factor contributions with `core.ServerScoreCalculator` (the tail-latency model: P95 weight 0.45, P99 0.35…) *regardless of strategy*, then pairs them with `explanation.scores()` — which for `WEIGHTED_LEAST_LOAD` is a different formula, for WRR is *effective weights*, and for ROUND_ROBIN is empty. So for 4 of 5 strategies the reported factor contributions don't sum to the reported score, and "the factor that separated the candidates" was never part of the strategy's selection math. The core value proposition — "explain this routing decision" — is wrong for most strategies.

**E3 (Med). Cross-request state → nondeterministic `/compare` + unstable fingerprints.** The singleton service holds stateful WRR `currentWeights` and RR `cursor` (and a seeded `Random` for tie-breaks). Two byte-identical `POST /compare` requests can return different `chosenServerId`, so the SHA-256 "deterministic fingerprint" differs for identical input — directly contradicting the reproducibility contract, and leaking prior-request routing history.

**E4 (Low-Med). Fingerprint delimiter injection.** Builders join `candidateIds` with `,` / fields with `\n` after only trimming; a serverId like `a,b` collides with two ids `a`,`b`. Fingerprints are advisory, so low exploitability, but it breaks the uniqueness claim.

**N1 — "Null-safety theater" (the audit's explicit question: confirmed).** `RoutingDecisionReplayEvidenceNullSafetySummaryService` (864 lines) exists solely to emit metadata about whether *other response objects the same request just built* have non-null fields — nullness that is statically knowable. It even asserts its own boundary strings contain the phrase "not production certification." Companions `FieldInventoryService` (1007 lines, 201 `field()` calls), `BoundarySummaryService`, `StatusRollupService`, and five `Lane*SummaryService`s / four `Reviewer*Service`s are the same metadata-about-metadata pattern. It's HTTP-reachable, computes nothing, and materially inflates the E1 payload.

**Duplication & value.** Five overlapping "explain/compare a decision" stacks (§1). The Replay/ReplayEvidence chain (~11k lines) and the DecisionExplorer chain (~12k lines) are ~90% derivational restatement of the ~1.9k lines of *actual* factor data in `RoutingComparisonService`. Realistically **~18k–20k lines are deletable/consolidatable** into a ~2–3k-line explainability module; the only non-derivational pieces are dominant/delta factor analysis and counterfactual ±10% weight scenarios (<2k lines combined). Both would have real value as an **"explain this actual proxied request"** endpoint if fed the real `RoutingDecision` the proxy made (with each strategy's own score model, fixing E2) — the proxy makes real decisions today that are captured into none of this machinery.

---

## 5. Operator surfaces (CLI / GUI / demo / cockpits / docs / build)

**O0 (High, structural). The shipped jar cannot reach the flagship CLI.** `pom.xml:268` sets the main class to `LoadBalancerApiApplication`, whose `main` dispatches only EnterpriseLab/Lase commands and whose `shouldStartApi()` ignores `RemediationReportCli.isRequested`. `RemediationReportCli` is wired only into `cli/LoadBalancerCLI.main`, which nothing references. So `java -jar …jar --remediation-report --input … --output …` (the exact form used ~40× in `docs/REMEDIATION_REPORT_CLI.md`) silently boots a web server and writes nothing. All 31KB of that doc is non-runnable as written.

**O1 (High). CLI "Scale Cloud" can scale down on an aborted prompt.** `LoadBalancerCLI.java:757` guards only `adjust == Integer.MIN_VALUE`, but `promptForInt` returns **-1** on abort/max-attempts, and -1 is a valid adjustment in range → three fat-fingered inputs execute `scaleServersAsync(current-1)`, an unrequested cloud scale-down (a real AWS mutation in live mode).

**O2 (Critical for the feature, but the feature is dead). JavaFX GUI action buttons are runtime-broken.** All five commands run via `runAsync` on the common pool and then call `dialog.showAndWait()` off the FX thread → `IllegalStateException: Not on FX application thread`, surfaced as "Command failed." Add/Fail/Balance/InitCloud/ScaleCloud never work. Plus off-thread table mutation races, an unbounded alerts list, a no-op `GuiConfig.Builder` (16 setters that `return this` and ignore their value), and `javafx-controls` at **compile scope** so it's baked into the headless server jar.

**O3 (High). Dead undo persistence + unfiltered deserialization.** `Command` isn't `Serializable`, so `UndoManager` truncates `undo_history.ser` then throws `NotSerializableException` (swallowed) every exit — history never survives restart. Worse, load path is `ObjectInputStream.readObject()` on a CWD-relative file with no `ObjectInputFilter` → a planted gadget-chain file in the launch directory is code execution at startup (mitigated only by the write path being dead).

**O4 (Med). XSS in two cockpit pages.** `enterprise-lab.html` (5 `innerHTML` sinks — verified) interpolates server JSON (`scenario.displayName`, `event.rollbackReason`, `policy.warning`, guardrail reasons) into template literals without escaping; `enterprise-lab-reviewer.html:608` does the same with reviewer-summary path fields. The other 11 pages correctly use `textContent`/`escapeHtml` — the two vulnerable ones are precisely those missing the helper.

**O5 (Med/Low). Overwrite & tooling inconsistencies.** `RemediationReportCli` outputs (`--output`, `--manifest`, redaction sidecars, bundle zips) clobber without a `--force` prompt, while `EvidencePolicyExampleService` correctly checks-and-forces — inconsistent in a tool selling evidence integrity. Jar-selection drifts three ways across `operator-distribution-smoke.sh` (lexical sort → picks 2.5.0 over 2.10.0), the `.ps1` (mtime), and Docker/CI (`ls -t`); `local-artifact-verify` hardcodes `2.5.0`. CLI idle-timeout is inverted (only checked between prompts, but prompts block on `nextLine()`), menu items 11/12 are unimplemented stubs, and a `checkMonitorStatus` sleeps up to ~31s on the input thread.

**Well-built (keep as-is):** `demo/ProxyDemoFixtureLauncher` (loopback-enforced, clean lifecycle — the best operator surface in the repo), the Dockerfile (digest-pinned, non-root, healthcheck), all bash scripts (`set -euo pipefail`, loopback-only), the Postman collection, and the zip-slip guard in bundle verification.

**Docs.** ~75–80% agent-generated ceremony: ~113 files matching PLAN/CHECKLIST/ROLLUP/LANE/CLOSURE patterns (a 260KB `REVIEWER_TRUST_MAP.md`, a 97KB `DECISION_VECTOR.md`), plus stale evidence (`RELEASE_ARTIFACT_EVIDENCE.md` still at v1.9.0, `PERFORMANCE_BASELINE.md` with no measured numbers). ~30 files are genuinely operator-useful (OPERATIONS_GUIDE, RUNBOOK, DEPLOYMENT_HARDENING, CONTAINER_*, API_CONTRACTS).

---

## 6. Cross-cutting themes

1. **Fabricated inputs everywhere.** LASE synthesizes latency from load score (L1); the simulation `ServerMonitor` random-walks metrics (proxy audit); cloud memory/disk are `cpu*1.2`/`cpu*0.8` (L15); explorer runs on client-supplied synthetic candidates (E-cluster); lab experiments hit `127.0.0.1:1`. **Nothing in ~96% of the codebase observes a real request.** The highest-value move across all four areas is the same: capture the real `RoutingDecision` + per-upstream latency/error the proxy already produces, and feed *that* into the shadow evaluator, the explainability layer, and the traffic-share orchestrator.
2. **Five parallel compare/explain stacks** (§1) — consolidate to one.
3. **Ceremony that validates itself** — null-safety-summary services, evidence-of-evidence proof runners, tests that assert prose. ~30k+ lines deletable across lab + explorer with no capability loss.
4. **Default-open security** — one `permitAll` + one `@Profile`-gated filter leaves destructive and DoS-amplifying endpoints unauthenticated by default. One fix (proxy-audit D9 / lab S1) closes it everywhere.
5. **Real durability engineering worth keeping** — the ownership lease/fencing, the journal store, and the transaction coordinator are sound designs; they're just wrapped in duplication and pointed at a simulator.

---

## 7. Consolidated verdict table

| Subsystem | Lines (approx) | Verdict | One-line rationale |
|---|---|---|---|
| `AdaptiveTrafficDecisionOrchestrator` + `recommendTrafficShares` + TrafficAllocation guardrails | ~3k | **KEEP & WIRE LIVE** | Best code in the audit; could drive real weighted allocation |
| LASE shadow advisor + event log + endpoint | ~4k | **KEEP shell, FIX inputs (L1–L4)** | Production-grade plumbing evaluating fabricated telemetry |
| AIMD limiter / shedding / autoscaler / failure classifier | ~2k | **CONSOLIDATE (3 dup pressure classifiers) + ACTUATE** | Clean functions, none wired to anything |
| Cloud* (AWS) | ~3k | **KEEP guardrails, FIX L5–L12 before any live use** | Good gating, broken math + orphan-ASG cost risk |
| Evidence ownership (lease/fence/paths) | ~3.3k | **KEEP (fix C5)** | Genuinely sound single-host design |
| Command ledgers ×2 + AllocationStateStore + LocalJournal | ~5k → ~1.5k | **CONSOLIDATE to one JSONL engine, then fix C1/C3** | Four copies of one store |
| Journal directory / replay / verifier | ~3k | **KEEP (fix C6)** | Core durable evidence; ID-hashing is correct |
| Allocation transaction coordinator + reconcilers | ~4k | **KEEP** | The actual crash-window state machine; well tested |
| Supervisor server/service/client/protocol | ~4k | **KEEP if external-supervisor is a requirement, else QUARANTINE** | Sound, but exists to prove process separation of a simulator |
| 5 proof runners + reports + exporters | ~7.2k | **MOVE to src/test or a tool module** | Integration tests shipped in the prod jar |
| DecisionExplorer + Replay + ReplayEvidence chains | ~23k → ~2-3k | **CONSOLIDATE to one explainability module** | ~90% derivational restatement |
| Null-safety/field-inventory/lane/reviewer "evidence" services | ~6k | **DELETE** | Metadata about metadata; computes nothing |
| `RoutingComparisonEngine` + dominant/delta factor analysis | ~1k | **KEEP & WIRE LIVE** | The one real explainability capability |
| Interactive `LoadBalancerCLI` + UndoManager | ~2k | **QUARANTINE/DELETE** | Synthetic state, dead undo, deser risk, dangerous sentinel |
| Evidence/report CLI tooling (`RemediationReportCli` +svcs) | ~4k | **KEEP (fix O0 wiring, O5 overwrite)** | Real file work, reuses api service |
| JavaFX GUI | ~2k | **DELETE (or fix O2/O3 if UI is wanted)** | All action buttons runtime-broken; drags CVE surface into server jar |
| `ProxyDemoFixtureLauncher`, Dockerfile, scripts, Postman | — | **KEEP** | Best-built surfaces |
| docs/ ceremony (~113 files) | — | **ARCHIVE ~80%** | Agent-generated plans/rollups; keep ~30 operator docs |

Top-priority fixes across this whole surface, in order: **S1/D9 default-deny → E1 explorer OOM cap → O1 CLI scale-down sentinel → O4 cockpit XSS → C1 ledger torn reads → L7 orphan-ASG guard → O0 CLI wiring → C2/C3/C5/C6 lab durability.**
