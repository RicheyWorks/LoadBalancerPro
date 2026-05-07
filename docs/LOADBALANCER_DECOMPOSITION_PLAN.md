# LoadBalancer Decomposition Plan

## Baseline

- Version: v2.4.2
- Baseline branch: loadbalancerpro-clean
- Baseline HEAD: 4d6153be7395ef14cd8ffc7863686b06e96686f4
- Purpose: document the current `LoadBalancer.java` responsibility map and a conservative, docs-first decomposition path after the routing strategy current-state audit

## Current-State Summary

`LoadBalancer.java` is no longer the single owner of every private responsibility. The following low-risk helper extractions have already landed and should be treated as the current baseline:

- `ConsistentHashRing`
- `ServerRegistry`
- `LoadDistributionEngine`
- `ServerHealthCoordinator`

At the current baseline, `LoadBalancer.java` is about 500 lines and mostly acts as a public facade/orchestrator. It still owns public constructors, public API compatibility, locking, cloud manager lifetime, monitor/executor lifetime, import/export orchestration, legacy batch routing entry points, LASE shadow observation wiring, and deprecated compatibility shims.

The request-level routing strategy registry/API is a separate layer. `RoutingStrategy`, `RoutingStrategyRegistry`, `RoutingComparisonEngine`, `RoutingController`, and `RoutingComparisonService` are not currently delegated through `LoadBalancer.java`. Do not merge those two routing layers during decomposition without a separate design.

## Responsibility Map

| Responsibility | Current owner / path | Current status |
| --- | --- | --- |
| Public constructors and API surface | `LoadBalancer.java` | Keep public signatures stable. Constructors also choose the LASE shadow advisor setup. |
| Server registration and lookup | `LoadBalancer.java` facade over `ServerRegistry` | `ServerRegistry` is already package-private and focused. `LoadBalancer` still coordinates locks, hash ring updates, and public snapshots. |
| Server removal | `LoadBalancer.removeServer`, `ServerHealthCoordinator.removeFailedServer` | Removal is coupled to registry, accumulated load, and hash ring cleanup. |
| Health checks and failover | `LoadBalancer.checkServerHealth`, `ServerHealthCoordinator` | Coordinator is extracted. `LoadBalancer` keeps the public health/failover facade and lock boundary. |
| Round-robin state and rebalance | `LoadBalancer.Strategy`, `setStrategy`, `rebalanceExistingLoad`, `LoadDistributionEngine` | Legacy batch-only behavior. Do not confuse with request-level `ROUND_ROBIN`. |
| Legacy batch distribution | `LoadBalancer` public methods, `LoadDistributionEngine`, `LoadDistributionPlanner` | Core algorithms are delegated, but `LoadBalancer` still owns public entry points and healthy-server wrappers. |
| Weighted distribution | `LoadBalancer.weightedDistribution`, `LoadDistributionEngine` | Legacy proportional batch distribution, not request-level weighted round robin. |
| Consistent hashing batch behavior | `LoadBalancer.consistentHashing`, `ConsistentHashRing` | Ring helper is extracted, but the public batch loop and synthetic `data-N` key behavior remain in `LoadBalancer`. |
| Cloud initialization | `LoadBalancer.initializeCloud`, `CloudManager` | Public cloud setup remains in `LoadBalancer`. Do not change dry-run or live guardrail behavior during decomposition. |
| Cloud metrics update coordination | `LoadBalancer.updateCloudMetricsIfAvailable`, `updateMetricsFromCloud` | Retry constants, retry loop, and deprecated shim remain in `LoadBalancer`. |
| Cloud scale and shutdown boundary | `LoadBalancer.scaleCloudServers`, `cloudManagerOptionalShutdown` | Thin public boundary over `CloudManager`; still safety-sensitive. |
| Monitor/executor lifecycle | `LoadBalancer.monitor`, `executor`, `shutdown` | Lifecycle remains in `LoadBalancer`, including repeated shutdown behavior. |
| Import/export orchestration | `importServerLogs`, `exportReport`, `alertLog` | Public facade over `Utils`, executor, registry snapshot, and alert log. |
| LASE shadow observation | LASE constructors, test hooks, `observeLaseShadow` | Shadow-only advisory path; must not mutate routing, allocation, cloud, or AWS behavior. |
| Deprecated compatibility shims | `getCloudManager`, `updateMetricsFromCloud`, `handleFailover`, `balanceLoad`, `getServerMonitor` | Preserve until a documented removal policy exists. |
| Request-level routing strategy API | `RoutingComparisonService`, `RoutingController`, core routing strategy classes | Separate from `LoadBalancer.java`; no direct production allocation delegation through this layer today. |

## Existing Helper Baseline

| Helper | Status | Notes |
| --- | --- | --- |
| `ConsistentHashRing` | Extracted | Package-private helper for ring maintenance and healthy server selection. |
| `ServerRegistry` | Extracted | Package-private helper for server list/map snapshots and typed/healthy views. It still contains `loadQueue`, which remains an audit item only. |
| `LoadDistributionEngine` | Extracted | Package-private helper for legacy batch allocation, accumulated load, and metrics recording. |
| `LoadDistributionPlanner` | Existing helper | Static planner for legacy batch allocation formulas. |
| `ServerHealthCoordinator` | Extracted | Package-private helper for health detection, removal, redistribution, and cloud replacement scaling coordination. |

## Proposed Extraction Targets

| Target | Current status | Proposed scope | Recommendation |
| --- | --- | --- | --- |
| `ServerRegistry` or `ServerPool` | `ServerRegistry` already exists | Keep registry focused on storage/snapshots. Consider a broader `ServerPool` only if lock ownership or public snapshot ownership is deliberately moved later. | Do not start here unless new tests show registry ownership is still too coupled. |
| `ServerHealthCoordinator` | Already exists | Keep as health/failover coordinator. Future work could reduce callback coupling only with tests around cloud failover and redistribution. | Do not re-extract now. |
| `LegacyBatchDistributionEngine` | Partially covered by `LoadDistributionEngine` and `LoadDistributionPlanner` | If needed later, move `consistentHashing`, healthy-server wrappers, and rebalance strategy selection behind one package-private coordinator while preserving public `LoadBalancer` methods. | Medium risk; defer until cloud/LASE/lifecycle edges are clearer. |
| `CloudMetricsCoordinator` | Not extracted | Isolate `cloudManager`, `initializeCloud`, `updateCloudMetricsIfAvailable`, retry constants, `scaleCloudServers`, and cloud shutdown boundary behind a package-private coordinator. | Best first implementation candidate, but add or refresh characterization around retries and no-cloud no-op first. |
| `RoutingStrategyBridge` or `RequestRoutingCoordinator` | Request-level routing already lives outside `LoadBalancer` | Would bridge legacy batch allocation and request-level strategy comparison only if a future design explicitly requires it. | Defer. This is not a decomposition-only change. |
| `LaseShadowAdvisorBridge` | Not extracted | Isolate LASE constructors, `laseShadowAdvisor`, test hooks, and `observeLaseShadow` fail-safe behavior. | Good later candidate after preserving shadow-only behavior with focused tests. |
| `CompatibilityShimPolicy` | Not code-extracted | Document and test deprecated shim behavior. Do not remove shims during decomposition. | Docs/test policy first; no production removal. |
| `LoadBalancerLifecycleCoordinator` | Not extracted | Isolate monitor, executor, async import ownership, export snapshot boundary, and shutdown retry semantics. | Later candidate; lifecycle behavior is easy to drift. |

## Safe Migration Sequence

1. Keep docs and evidence current before each implementation branch.
2. Add or refresh characterization tests for the next specific responsibility before production refactor.
3. Extract one responsibility at a time.
4. Keep `LoadBalancer.java` public method signatures, constructor behavior, and deprecated shims intact.
5. Preserve package-private helper boundaries unless a public API change is explicitly approved.
6. Keep legacy batch routing separate from request-level `RoutingStrategyRegistry` and `/api/routing/compare`.
7. Run full verification after each extraction: `mvn -q test`, `mvn -q -DskipTests package`, packaged JAR smoke checks where relevant, `git diff --check`.
8. Do not remove deprecated shims until a documented removal policy exists and downstream compatibility risk is accepted.

## Non-Goals

- No routing behavior changes in decomposition PRs.
- No Spring Boot, Spring Security, or authentication changes.
- No dependency upgrades.
- No LASE execution-mode changes.
- No request-level routing API expansion.
- No cloud mutation, dry-run, account, region, capacity, or deletion guardrail changes.
- No release, tag, or public/main work.
- No package namespace migration.

## Risk Table

| Risk | Why it matters | Mitigation |
| --- | --- | --- |
| Behavior drift | `LoadBalancerTest` preserves legacy allocation and compatibility behavior, including some surprising least-loaded/equal-allocation semantics. | Characterize first, keep public facade methods unchanged, and compare pre/post results. |
| Hidden coupling | Removal, hash ring cleanup, accumulated load, and cloud failover replacement are linked. | Extract only one boundary per PR and keep helper constructors package-private. |
| Deprecated shim breakage | GUI/tests/downstream callers may still use deprecated accessors and rebalance/failover names. | Preserve shims and add policy/tests before any removal. |
| Cloud guardrail regression | Cloud initialization, metrics, scaling, and shutdown touch safety-sensitive paths. | Use mocked/dry-run tests only; no live AWS behavior; keep CloudManager semantics unchanged. |
| Routing terminology confusion | Legacy batch distribution and request-level routing strategy comparison share names like round robin and weighted. | Keep docs explicit and avoid bridging layers without design approval. |
| LASE overreach | LASE is shadow-only in public allocation flows today. | Keep LASE bridge shadow-only and fail-safe; do not mutate routing or cloud behavior. |
| Lifecycle race/regression | `shutdown`, monitor ownership, executor ownership, and async import can change timing. | Defer lifecycle extraction until dedicated tests cover idempotency and async behavior. |
| `LoadBalancer.java` growth | Adding new strategies or bridges directly to the facade can reverse prior decomposition work. | Add new behavior behind focused helpers and keep `LoadBalancer` as the compatibility facade. |

## Recommended First Implementation Branch

Recommended first implementation branch after this docs refresh:

`codex/extract-cloud-metrics-coordinator`

Rationale:

- `ServerRegistry`, `ConsistentHashRing`, `LoadDistributionEngine`, and `ServerHealthCoordinator` are already extracted.
- Cloud metrics/update coordination is still visibly concentrated in `LoadBalancer.java` through retry constants, cloud manager setup, metric update retries, scale boundary, and shutdown boundary.
- A package-private coordinator can keep the public `LoadBalancer` API unchanged while reducing cloud-facing responsibility in the facade.
- The branch should include focused characterization for no-cloud no-op behavior, retry failure behavior, deprecated `updateMetricsFromCloud()` compatibility, and scale input validation before or alongside extraction.

Do not choose `codex/extract-server-registry` as the next branch unless a later audit identifies a new, specific registry responsibility. The registry helper already exists.

## Additional Characterization Needed Before Future Extraction

- cloud metrics retry behavior without live AWS calls
- deprecated `updateMetricsFromCloud()` compatibility over the cloud metrics coordinator
- `scaleCloudServers()` input validation and no-cloud behavior
- LASE shadow observation and fail-safe behavior without live routing changes
- monitor shutdown idempotency and repeated shutdown behavior
- async import behavior and executor ownership
- export snapshot behavior with alerts and registry state
- public API snapshot and lookup behavior if registry coordination changes
- consistent hashing synthetic-key behavior if moved out of `LoadBalancer`

## Do Not Do Yet

- Do not remove deprecated shims.
- Do not change `balanceLoad()` behavior.
- Do not alter least-loaded/equal-allocation legacy behavior.
- Do not change cloud mutation or dry-run guardrails.
- Do not change monitor lifecycle semantics.
- Do not add dependencies.
- Do not merge legacy batch routing into `RoutingStrategyRegistry` or `/api/routing/compare`.
- Do not wire LASE recommendations into live allocation decisions.
