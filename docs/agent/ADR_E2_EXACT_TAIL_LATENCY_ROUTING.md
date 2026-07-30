# ADR — Exact Tail-Latency Routing via Order Statistics (CSRBT lane E2, with E1/E5 appendices)

**Status:** Proposed (WARN-classified planning surface — elaborates `docs/agent/CSRBT_ECOSYSTEM_INTEGRATION_PROPOSAL.md`)
**Date:** 2026-07-21
**Scope:** LoadBalancerPro. Lab-mode, off by default. No endpoint, CI, Dockerfile, or production-default changes. One-way dependency (LoadBalancerPro consumes; the CSRBT ecosystem takes nothing back).
**Trust note:** This is a design record, not implementation permission. No lane is complete until its own scoped PR is merged with green main checks. This ADR does not relax the README trust contract; it adds no production capability, throughput/p95/p99 production evidence, or supported dependency.

---

## 1. Context — the defect this closes

The independent audit (`docs/AUDIT_2026-07-21.md`, defect **D1**) established that LoadBalancerPro's adaptive routing strategies do not adapt on the live path. `ReverseProxyService.toCandidate()` builds each `ServerStateVector` from **static configuration** fields on `ReverseProxyProperties.Upstream` — `inFlightRequestCount` (default 0), `averageLatencyMillis`, `p95LatencyMillis`, `p99LatencyMillis`, `recentErrorRate`, `queueDepth`. Nothing on the request path ever updates them (`setInFlightRequestCount` is called only when copying config). Consequently `WEIGHTED_LEAST_CONNECTIONS` and `TAIL_LATENCY_POWER_OF_TWO` — the flagship strategies — decide on frozen numbers, and three of five strategies are decorative in the shipped proxy.

The `BUILD_PLAN_DEPLOYABLE.md` roadmap opens (PR-1.2/1.3) with "close the live-telemetry loop": track real per-upstream in-flight counts and rolling latency, and feed them into `ServerStateVector`. The open question that roadmap left is *how to compute the percentiles*. Naïve options are a full sort per read (O(n log n) on the hot path) or a bounded histogram / t-digest sketch (approximate, and a new dependency of its own).

CSRBT resolves this precisely. Its headline capability is **exact O(log n) order statistics over a subtree-size augmentation** — `select(rank)`, `rank(key)`, and derived `percentileKey(p)`. A per-backend sliding window backed by an order-statistics tree yields **exact** p95/p99 in O(log n) per query and O(log n) per sample, no sort, no sketch error. This is lane **E2** of the ecosystem proposal, and it is the cleanest fit in either codebase: one project's headline feature closes the other's headline defect.

## 2. Decision

Introduce a **lab-mode, off-by-default per-upstream latency window** exposing exact rolling p50/p95/p99, and let it (when enabled) supply the telemetry fields of `ServerStateVector` in place of the frozen config values. Do it behind a narrow **SPI seam** so the merge is not gated on the CSRBT dependency (see §4, which resolves proposal precondition P0).

### 2.1 The seam: `RankedLatencyWindow`

Define a small interface in LoadBalancerPro (new package `…loadbalancerpro.telemetry`):

```java
/** Bounded, thread-safe rolling window of latency samples with exact order statistics. */
public interface RankedLatencyWindow {
    void record(long latencyNanos);   // O(log n): insert sample, evict oldest if over capacity
    long percentileNanos(double p);   // O(log n): exact p in [0,100]; 0 if empty
    long p50(); long p95(); long p99();
    int size();
    void clear();
}
```

Two implementations, chosen at wiring time by config:

1. **`SelfContainedRankedLatencyWindow` (default, zero new dependency).** A ~200-line size-augmented balanced BST (AVL or WB — the audit-verified rotation+size-maintenance logic can be lifted directly) storing composite keys `(latencyNanos, monotonicSeq)` so duplicate latencies coexist, plus a FIFO ring of the live keys for O(1) eviction of the oldest sample. `percentileNanos(p)` = `select(ceil(p/100 · n))`. This ships in-repo, needs no CSRBT artifact, and **lets every E2 PR merge and pass CI with no external dependency** — directly satisfying precondition P0 without waiting on Maven Central.
2. **`CsrbtRankedLatencyWindow` (adapter, lab-only).** Delegates to CSRBT's windowed ordered set / `RankedSet.percentileKey`. Available only when the `io.github.richeyworks:csrbt:0.1.0` artifact resolves (mavenLocal in lab, or Maven Central once the ecosystem's Phase 9 publishes). Selected by config; never the CI/default path until P0's option (a) or (b) is formally met.

Rationale: the interface is the contract (mirroring CSRBT's own "the contract is the JSON, deliberately" stance in its visualizer). The self-contained default makes E2 shippable and CI-safe today; the CSRBT adapter is a drop-in upgrade that carries the ecosystem's tested engine when the dependency posture allows. This is the load-bearing design decision of the ADR — it dissolves the P0 blocker instead of waiting on it.

### 2.2 Wiring into the proxy hot path

- **Sample capture.** In `ReverseProxyService.forwardOnce(...)`, in the `finally`/completion block that already knows the attempt's outcome, record `window(upstreamId).record(elapsedNanos)`. Windows live in a `ConcurrentHashMap<String, RankedLatencyWindow>` on the service, created per configured upstream and **carried across reload for unchanged upstream ids** (this also fixes half of audit F-D14, where reload wipes runtime state). Pair with the in-flight `LongAdder` from build-plan PR-1.2 so `queueDepth`/`inFlightRequestCount` are live too.
- **Consumption.** In `toCandidate()`, when `loadbalancerpro.proxy.telemetry.exact-percentiles.enabled=true`, populate `ServerStateVector`'s `averageLatencyMillis`/`p95`/`p99`/`queueDepth` from the window + in-flight counters; when `false`, behave exactly as today (config values). The config fields become documented seed/fallback values, not the live source.
- **No endpoint change.** `GET /api/proxy/status` may *optionally* surface the live percentiles in its existing JSON (additive, no new route). No new controller, no schema break.

### 2.3 Calculation-core path (optional, later)

The allocation core already models `ServerObservationWindow`; E2's `RankedLatencyWindow` can back it too, giving `LoadDistributionPlanner`/`Evaluator` exact percentiles for the `/api/allocate/*` scoring experiments. Deferred to a follow-up PR; the proxy path is the primary deliverable.

## 3. Flag & configuration surface

All new keys default to the inert value; the shipped and `prod` profiles are unchanged.

| Key | Default | Effect |
|---|---|---|
| `loadbalancerpro.proxy.telemetry.exact-percentiles.enabled` | `false` | Master switch. `false` = today's config-driven behavior, bit-for-bit. |
| `loadbalancerpro.proxy.telemetry.window-size` | `256` | Samples retained per upstream (bounded memory: O(upstreams × window-size)). |
| `loadbalancerpro.proxy.telemetry.impl` | `self-contained` | `self-contained` (default, no dep) or `csrbt` (lab-only, requires the artifact). |
| `loadbalancerpro.proxy.telemetry.min-samples` | `20` | Below this, fall back to config/seed percentiles (avoids cold-start noise; pairs with slow-start, build-plan PR-1.8). |

## 4. Dependency posture (satisfies precondition P0)

The proposal's P0 forbids merging a Maven dependency until the ecosystem publishes to Maven Central *or* a reviewer-approved local-lab resolution posture is documented. This ADR's SPI design means **the E2 feature does not require the dependency to merge at all**: the `self-contained` implementation is the CI/default path and pulls nothing external. The `csrbt` implementation is an *optional lab adapter*, added in its own PR that (a) declares the dependency `provided`/optional so it is never on the default resolution or CI classpath, and (b) documents mavenLocal resolution as manual, local, and not CI-proof — the same explicitness the Compose readiness gate uses. Dependency direction stays one-way. This keeps the whole lane green and honest before Maven Central exists.

## 5. Test strategy

Following `docs/agent/VERIFICATION_PROTOCOL.md` — focused checks while editing, full local verification before merge, current-head + post-merge main checks.

1. **Percentile correctness (property test).** For random sample streams and window sizes, assert `RankedLatencyWindow.percentileNanos(p)` equals a brute-force percentile over the *same* live window (sort the current contents, index by the same `ceil` convention) for p ∈ {50, 90, 95, 99, 100} across thousands of seeded iterations. Run against **both** implementations so the CSRBT adapter and the self-contained tree are held to the identical oracle.
2. **Windowing / eviction.** Assert size never exceeds capacity, that eviction is FIFO (oldest sample leaves), and that percentiles track a shifting distribution (e.g. a step change from 40ms to 200ms is reflected within `window-size` samples).
3. **Order-statistics invariants.** For the self-contained tree, reuse the audit's approach: randomized mixed insert/evict against a sorted-list oracle with structural + subtree-size validation after every op (the audit's companion visualizer already demonstrates this passes for the lifted rotation logic).
4. **Concurrency.** Hammer one window from N threads (`record` on Tomcat request threads, `percentile*` on the routing thread) and assert no exception, no lost update beyond the window bound, and monotonic-ish readings — the interface must be thread-safe (the self-contained impl guards with a lock or a stamped lock; the CSRBT adapter inherits the engine's `StampedLock`).
5. **Flag inertness.** With the flag `false`, a golden test asserts routing decisions and `ServerStateVector` contents are identical to the pre-E2 build for a fixed scenario — proving zero behavior change when off.
6. **Scenario-evidence lab run.** In the existing local-lab manner, a one-slow-backend scenario under `TAIL_LATENCY_POWER_OF_TWO` with the flag on shows traffic shifting away from the slow upstream (the behavior the audit proved is *absent* today), captured as lab evidence — explicitly **not** throughput/p95/p99 production evidence.

## 6. Consequences

**Positive.** Closes audit D1 on the proxy path; makes least-connections and tail-latency-P2C genuinely adaptive; adds exact (not sketch) percentiles at O(log n); ships with no new dependency; leaves a clean upgrade seam to the CSRBT engine; partially fixes reload state loss (F-D14). Converts existing-but-decorative strategy code into live capability — the highest capability-per-line change identified in the build plan.

**Negative / risks.** Memory grows O(upstreams × window-size) (bounded, configurable). A second order-statistics implementation now exists in-repo (the self-contained tree) — acceptable, and by design swappable for CSRBT. Hot-path cost rises from O(1) config read to O(log window) per candidate per request — negligible at window≤256, but the `min-samples` and flag gates keep it opt-in. The CSRBT adapter's correctness depends on the artifact; the shared oracle test (§5.1) guards against drift.

**Neutral.** No endpoint, CI, Docker, or default-profile change. Prod behavior identical unless an operator sets the flag.

## 7. Sequencing (PR-by-PR, each independently mergeable and green)

- **PR-E2.1** — `RankedLatencyWindow` interface + `SelfContainedRankedLatencyWindow` + tests §5.1–5.4. Pure library, no wiring. Merges with zero dependency.
- **PR-E2.2** — Capture: per-upstream windows in `ReverseProxyService`, sample on `forwardOnce`, in-flight counters (build-plan PR-1.2), reload-carryover. Still no consumption; flag absent from decisions.
- **PR-E2.3** — Consume: `toCandidate()` reads the window behind `exact-percentiles.enabled`; golden flag-inertness test §5.5; scenario-evidence run §5.6. **This is the PR that closes D1.**
- **PR-E2.4** — Optional additive status surfacing of live percentiles in existing `/api/proxy/status` JSON.
- **PR-E2.5** — Lab-only `CsrbtRankedLatencyWindow` adapter behind `impl=csrbt`, dependency declared optional/provided with the documented mavenLocal posture (P0 option b). Shared oracle test runs both impls.

---

## Appendix A — Lane E1 (crash-safe allocation-evidence log, SmokeHouse)

The lab-side audit (`docs/AUDIT_LAB_SHADOW_2026-07-21.md`) found the evidence ledger has structurally-possible torn cross-process reads (C1) and non-atomic, fsync-less persistence that can silently destroy the prior snapshot (F-P1). SmokeHouse's doctrine — *the append-only CRC'd log is the only truth; every index is a rebuildable cache; a durably-written record cannot be lost to a crash* — is that exact posture as a storage engine. E1 proposes appending each allocation decision (request descriptor, candidate readouts, decision vector, chosen target, metrics) to an embedded SmokeHouse store behind an off-by-default lab flag, with seeded oracle tests (TreeMap reference) and reopen/replay-after-crash tests. Same SPI discipline applies: define an `AllocationEvidenceSink` interface with a self-contained append-only-file default and a SmokeHouse-backed adapter, so E1 merges without the dependency and upgrades cleanly. Claim boundary: lab evidence capture, not replay/evidence/export proof in the README's sense.

## Appendix B — Lane E5 (anti-thrash promotion gates, MorphPolicy)

CSRBT's `control.MorphPolicy` — verified sound in the audit — gates strategy changes with **cooldown**, a **minimum-improvement margin** (a candidate must be ≥20% cheaper to win, making A→B→A oscillation impossible at a fixed workload), and **stability wins** (N consecutive agreeing evaluations before acting). This is precisely the hysteresis the LoadBalancer's adaptive weight/strategy selection needs to avoid thrash under regime shifts, which the build plan flagged as missing (alongside slow-start, PR-1.8). E5 is a *design-pattern* transfer, not a code dependency: port the gate shape (three parameters, a `MorphHistory` cooldown counter) into the routing policy that acts on E2's now-live telemetry. Ledger-only until E2 lands and a reviewer names the requirement.

## Appendix C — Why order statistics, not a sketch

A t-digest / DDSketch gives approximate percentiles in O(1) space and is the usual production choice at massive scale. E2 chooses exact order statistics because (a) per-backend windows are small (≤256) so O(log n) is trivially cheap and exactness is free; (b) the project's entire trust posture is "controlled, reproducible, exact — not estimated," and CSRBT's exact percentiles are that posture expressed as a data structure; (c) it reuses an audited, tested engine from the same author rather than adding an approximate third-party dependency. If windows ever need to be large (thousands+), a sketch-backed `RankedLatencyWindow` implementation can be added behind the same interface without touching the proxy.
