# LoadBalancer Decomposition Plan

## Baseline

- Version: v2.4.2
- Baseline branch: loadbalancerpro-clean
- Baseline HEAD: 8fcc749ee967e07454949f60544ce634bceaa52f
- Purpose: document safe future decomposition of LoadBalancer.java before refactoring

## Current Responsibility Map

`LoadBalancer.java` currently owns:

- server registration
- duplicate replacement
- serverMap / servers snapshots
- legacy batch distribution
- current allocation accumulation
- legacy rebalance strategy selection state
- synthetic-key consistent hashing
- health/failover removal
- cloud replacement scaling
- cloud metric update retries
- monitor/executor shutdown
- deprecated public compatibility shims
- import/export orchestration
- LASE shadow observation for capacity/predictive allocation

## Critical Boundary

Legacy batch routing in `LoadBalancer.java` must remain separate from request-level `RoutingStrategy`, `RoutingStrategyRegistry`, `RoutingComparisonEngine`, and API routing unless a future dedicated design explicitly unifies them.

## Extraction Candidates

| Candidate | Current code involved | Risk | Timing |
| --- | --- | --- | --- |
| ConsistentHashRing | `hashReplicas`, `consistentHashRing`, `precomputeHashRingEntries`, `removeServerFromHashRing`, `consistentHashing` | Medium | After RoundRobinRoutingStrategy |
| LoadDistributionEngine | `roundRobin`, `leastLoaded`, `weightedDistribution`, `capacityAware*`, `predictive*`, `currentDistribution`, `DomainMetrics`, LASE hooks | Medium/High | After characterization tests |
| ServerHealthCoordinator | `checkServerHealth`, `removeFailedServersAndRecover`, `redistributeLoad`, `replaceFailedCloudCapacity` | High | Later |
| CloudMetricsCoordinator | `initializeCloud`, `cloudManager`, retry constants, `updateCloudMetricsIfAvailable`, `scaleCloudServers`, `shutdown` | High | Later |
| ServerRegistry | `servers`, `serverMap`, `loadQueue`, `serverLock`, `addServer`, `removeServer`, getters | Medium | After tests |
| LaseShadowAdvisorBridge | LASE constructors, test hooks, `observeLaseShadow` | Medium | After behavior tests |
| LoadBalancerLifecycleCoordinator | `monitor`, `executor`, `importServerLogs`, `exportReport`, `shutdown` | Medium/High | Later |
| Deprecated shim policy | `getCloudManager`, `updateMetricsFromCloud`, `handleFailover`, `balanceLoad`, `getServerMonitor` | Low docs / High removal | Do not remove yet |

## Audit Notes

- `loadQueue` is maintained on add/remove but appears unread locally.
- This is an audit note only, not a removal recommendation.
- Deprecated public shims are intentionally preserved.
- `balanceLoad()` remains a compatibility shim.
- `updateMetricsFromCloud()` remains a deprecated shim behind `updateCloudMetricsIfAvailable()`.

## Characterization Tests Needed Before Extraction

- duplicate server replacement
- hash ring updates on add/remove
- legacy equal-allocation behavior
- accumulation/rebalance totals
- all-unhealthy handling
- negative/invalid input handling
- cloud dry-run / guardrail behavior
- cloud metric retry behavior
- monitor shutdown idempotency
- import/export behavior
- LASE shadow observation without live behavior changes

## Recommended Sequence

1. Keep this decomposition plan as the roadmap.
2. Implement request-level RoundRobinRoutingStrategy first because it is isolated from LoadBalancer.java.
3. Add characterization tests for LoadBalancer.java behavior.
4. Extract only one helper at a time.
5. Start with the least stateful helper after tests.
6. Do not remove deprecated shims.
7. Do not change cloud mutation behavior.
8. Do not merge legacy batch routing with request-level strategy routing without a separate design.

## Do Not Do Yet

- Do not remove deprecated shims.
- Do not change `balanceLoad()` behavior.
- Do not alter least-loaded/equal-allocation legacy behavior.
- Do not change cloud mutation or dry-run guardrails.
- Do not change monitor lifecycle semantics.
- Do not add dependencies.
- Do not merge legacy batch routing into `RoutingStrategyRegistry`/API.
