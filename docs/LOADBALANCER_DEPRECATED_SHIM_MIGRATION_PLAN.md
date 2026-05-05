# LoadBalancer Deprecated Shim Migration Plan

## Purpose

Plan caller migration for deprecated `LoadBalancer` compatibility shims before any removal or behavior change.

Current decision: do not remove deprecated shims in patch or minor maintenance without an explicit compatibility notice. The v2.4.0 namespace migration is complete and should not be combined with shim removals.

## Current Caller Table

| Shim | Definition | Production callers | Test callers | Docs/README callers | Current replacement | Migration readiness |
| --- | --- | --- | --- | --- | --- | --- |
| `getCloudManager()` | `src/main/java/com/richmond423/loadbalancerpro/core/LoadBalancer.java:78` | None found. | `src/test/java/com/richmond423/loadbalancerpro/core/ServerMonitorTest.java:387` and `:389`. | Audit note only: `docs/LOADBALANCER_DEPRECATED_SHIMS_AUDIT.md`. | `getCloudManagerOptional()` or `hasCloudManager()`. | Test migration is enough before considering removal, but public Java API compatibility remains. |
| `updateMetricsFromCloud()` | `src/main/java/com/richmond423/loadbalancerpro/core/LoadBalancer.java:437` | Internal wrapper caller: `LoadBalancer.java:430` through `updateCloudMetricsIfAvailable()`. | None found. | Audit note only. | `updateCloudMetricsIfAvailable()`. | Implementation must move first; the non-deprecated wrapper currently delegates to the deprecated method. |
| `handleFailover()` | `src/main/java/com/richmond423/loadbalancerpro/core/LoadBalancer.java:494` | None found. | `src/test/java/com/richmond423/loadbalancerpro/core/LoadBalancerTest.java:766`. | Audit note only. | `checkServerHealth()`. | Lowest-risk first candidate; the test already calls `checkServerHealth()` immediately before the shim. |
| `balanceLoad()` | `src/main/java/com/richmond423/loadbalancerpro/core/LoadBalancer.java:519` | No direct `LoadBalancer.balanceLoad()` callers found. `LoadBalancerCLI` has a private same-name helper, not a shim caller. | None found. | Audit note only. | `rebalanceExistingLoad()` or explicit strategy methods. | Do not remove now; public compatibility and legacy GUI intent need a removal policy. |
| `getServerMonitor()` | `src/main/java/com/richmond423/loadbalancerpro/core/LoadBalancer.java:527` | None found. | `src/test/java/com/richmond423/loadbalancerpro/core/LoadBalancerTest.java:1005`. | Audit note only. | Public lifecycle behavior checks, or a narrower monitor-status API if needed. | Not ready; removal would break lifecycle coverage and exposes a broader API-design question. |

## Replacement API Table

| Deprecated shim | Preferred API | Notes |
| --- | --- | --- |
| `getCloudManager()` | `hasCloudManager()` for boolean checks; `getCloudManagerOptional()` when the configured manager must be inspected. | Prefer avoiding direct CloudManager inspection in behavior tests unless the test is specifically about cloud configuration. |
| `updateMetricsFromCloud()` | `updateCloudMetricsIfAvailable()`. | Move implementation into the non-deprecated wrapper, then leave the deprecated method as a delegating shim. |
| `handleFailover()` | `checkServerHealth()`. | The shim is a direct delegate and can be removed from local tests first. |
| `balanceLoad()` | `rebalanceExistingLoad()` or explicit allocation strategy methods. | No local direct callers were found, but public API compatibility remains the blocker. |
| `getServerMonitor()` | Public shutdown/lifecycle behavior checks, or a narrow status API. | Avoid exposing the full internal monitor solely for tests. |

## Phased Migration Sequence

### Phase A: Migrate Low-Risk Test Callers

1. Update `LoadBalancerTest` failover coverage to rely on `checkServerHealth()` instead of `handleFailover()`.
2. Update `ServerMonitorTest` to use `hasCloudManager()` and `getCloudManagerOptional()` instead of nullable `getCloudManager()`.
3. Re-run focused core tests, then the full Maven suite.

### Phase B: Move Cloud Metrics Implementation

1. Move the body of `updateMetricsFromCloud()` into `updateCloudMetricsIfAvailable()`.
2. Leave `updateMetricsFromCloud()` as a deprecated delegate to `updateCloudMetricsIfAvailable()`.
3. Add or confirm behavior tests for cloud-metric skip, success, retry, interruption, and safe failure paths through the non-deprecated wrapper.

### Phase C: Cover Replacement Behavior Before Any Removal

1. Confirm `checkServerHealth()` behavior covers failover/removal expectations.
2. Confirm `getCloudManagerOptional()` and `hasCloudManager()` cover cloud-manager presence checks without nullable access.
3. Add lifecycle coverage that verifies `shutdown()` stops monitor activity without exposing `getServerMonitor()`, or introduce a deliberately narrow monitor-status API.
4. Confirm `rebalanceExistingLoad()` is the intended replacement for `balanceLoad()` and document any legacy GUI compatibility decision.

### Phase D: Decide Removal Timeline

1. Keep all deprecated shims until local callers are migrated and replacement behavior is covered.
2. Treat removal as a compatibility-affecting change because these methods are public Java API.
3. Prefer keeping the shims through the next minor maintenance line and remove only in a major release or after an explicit compatibility notice.

## Removal Risk Classification

| Shim | Removal risk | Reason |
| --- | --- | --- |
| `handleFailover()` | Low to medium | Local migration is simple, but public Java API compatibility still matters. |
| `getCloudManager()` | Medium | Local test migration is straightforward, but nullable public API removal can break external callers. |
| `updateMetricsFromCloud()` | Medium | Public API plus implementation currently lives in the deprecated method. |
| `balanceLoad()` | Low to medium | No local direct callers found, but comments identify legacy GUI compatibility. |
| `getServerMonitor()` | Medium to high | Exposes lifecycle internals and current shutdown test depends on it. |

## Recommendation

Handle `handleFailover()` first as a test-only caller migration, then migrate `getCloudManager()` tests. Do not remove any shim in the same change. Move `updateMetricsFromCloud()` implementation only after tests cover the non-deprecated wrapper. Leave `balanceLoad()` and `getServerMonitor()` for a separate compatibility decision.

