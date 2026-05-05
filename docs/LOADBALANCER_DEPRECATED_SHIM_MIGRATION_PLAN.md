# LoadBalancer Deprecated Shim Migration Plan

## Purpose

Plan caller migration for deprecated `LoadBalancer` compatibility shims before any removal or behavior change.

Current decision: do not remove deprecated shims in patch or minor maintenance without an explicit compatibility notice. Local caller migration work is complete where practical, `updateMetricsFromCloud()` now delegates to the non-deprecated wrapper, and `balanceLoad()` is intentionally left untouched as public legacy compatibility API. The v2.4.0 namespace migration is complete and should not be combined with shim removals.

## Current Caller Table

| Shim | Definition | Production callers | Test callers | Docs/README callers | Current replacement | Migration readiness |
| --- | --- | --- | --- | --- | --- | --- |
| `getCloudManager()` | `src/main/java/com/richmond423/loadbalancerpro/core/LoadBalancer.java` | None found. | None remain; former `ServerMonitorTest` usage now calls `getCloudManagerOptional()`. | Audit and migration notes only. | `getCloudManagerOptional()` or `hasCloudManager()`. | Local migration complete; removal still requires public API compatibility policy. |
| `updateMetricsFromCloud()` | `src/main/java/com/richmond423/loadbalancerpro/core/LoadBalancer.java` | None found for the deprecated method; `ServerMonitor` calls `updateCloudMetricsIfAvailable()`. | None found. | Audit and migration notes only. | `updateCloudMetricsIfAvailable()`. | Implementation move complete; deprecated method remains as compatibility delegate. |
| `handleFailover()` | `src/main/java/com/richmond423/loadbalancerpro/core/LoadBalancer.java` | None found. | None remain; former `LoadBalancerTest` usage now calls `checkServerHealth()`. | Audit and migration notes only. | `checkServerHealth()`. | Local migration complete; removal still requires public API compatibility policy. |
| `balanceLoad()` | `src/main/java/com/richmond423/loadbalancerpro/core/LoadBalancer.java:519` | No direct `LoadBalancer.balanceLoad()` callers found. `LoadBalancerCLI` has a private same-name helper, not a shim caller. | None found. | Audit note only. | `rebalanceExistingLoad()` or explicit strategy methods. | Do not remove now; public compatibility and legacy GUI intent need a removal policy. |
| `getServerMonitor()` | `src/main/java/com/richmond423/loadbalancerpro/core/LoadBalancer.java` | None found. | None remain; former shutdown test now verifies public shutdown behavior. | Audit and migration notes only. | Public lifecycle behavior checks, or a narrow monitor-status API if needed. | Local migration complete; removal still requires public API compatibility policy. |

## Replacement API Table

| Deprecated shim | Preferred API | Notes |
| --- | --- | --- |
| `getCloudManager()` | `hasCloudManager()` for boolean checks; `getCloudManagerOptional()` when the configured manager must be inspected. | Prefer avoiding direct CloudManager inspection in behavior tests unless the test is specifically about cloud configuration. |
| `updateMetricsFromCloud()` | `updateCloudMetricsIfAvailable()`. | Implementation now lives in the non-deprecated wrapper; the deprecated method remains as a delegating shim. |
| `handleFailover()` | `checkServerHealth()`. | The shim is a direct delegate and can be removed from local tests first. |
| `balanceLoad()` | `rebalanceExistingLoad()` or explicit allocation strategy methods. | No local direct callers were found, but public API compatibility remains the blocker. |
| `getServerMonitor()` | Public shutdown/lifecycle behavior checks, or a narrow status API. | Avoid exposing the full internal monitor solely for tests. |

## Phased Migration Sequence

### Phase A: Migrate Low-Risk Test Callers

Status: complete.

1. `LoadBalancerTest` failover coverage now relies on `checkServerHealth()` instead of `handleFailover()`.
2. `ServerMonitorTest` now uses `getCloudManagerOptional()` instead of nullable `getCloudManager()`.
3. `LoadBalancerTest` shutdown coverage now verifies public shutdown behavior instead of using `getServerMonitor()`.
4. Focused core tests and the full Maven suite passed during each migration slice.

### Phase B: Move Cloud Metrics Implementation

Status: complete.

1. The body of `updateMetricsFromCloud()` now lives in `updateCloudMetricsIfAvailable()`.
2. `updateMetricsFromCloud()` remains as a deprecated delegate to `updateCloudMetricsIfAvailable()`.
3. Retry behavior, delay, log text, `IOException` messages, interrupt handling, and cloud guardrails were preserved.
4. Existing `ServerMonitor` cloud metrics tests exercise the non-deprecated wrapper path.

### Phase C: Cover Replacement Behavior Before Any Removal

Status: mostly complete for local callers.

1. `checkServerHealth()` behavior covers failover/removal expectations in local tests.
2. `getCloudManagerOptional()` covers the remaining local cloud-manager inspection need.
3. Shutdown lifecycle coverage now uses public shutdown behavior and no longer exposes `getServerMonitor()`.
4. `rebalanceExistingLoad()` is already covered directly and is the local replacement for `balanceLoad()`.
5. `balanceLoad()` remains untouched because it has no local callers and is public legacy compatibility API.

### Phase D: Decide Removal Timeline

1. Keep all deprecated shims until a public compatibility/removal policy is approved.
2. Treat removal as a compatibility-affecting change because these methods are public Java API.
3. Prefer keeping the shims through the next minor maintenance line and remove only in a major release or after an explicit compatibility notice.
4. Do not combine shim removals with dependency upgrades, release evidence work, or namespace migration follow-ups.

## Removal Risk Classification

| Shim | Removal risk | Reason |
| --- | --- | --- |
| `handleFailover()` | Low to medium | Local migration is complete, but public Java API compatibility still matters. |
| `getCloudManager()` | Medium | Local migration is complete, but nullable public API removal can break external callers. |
| `updateMetricsFromCloud()` | Medium | Implementation moved behind the wrapper, but public API compatibility still matters. |
| `balanceLoad()` | Low to medium | No local direct callers found, but comments identify legacy GUI compatibility. |
| `getServerMonitor()` | Medium to high | Local migration is complete, but the method exposes lifecycle internals and external callers may depend on it. |

## Recommendation

Local deprecated-shim caller cleanup is complete for `handleFailover()`, `getCloudManager()`, and `getServerMonitor()`, and `updateMetricsFromCloud()` now delegates to `updateCloudMetricsIfAvailable()`. Leave `balanceLoad()` untouched. The remaining decision is whether and when to remove public deprecated shims, which should be handled only with explicit compatibility notice, preferably in a major compatibility release.
