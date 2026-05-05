# LoadBalancer Deprecated Shims Audit

## Purpose

Document deprecated `LoadBalancer` compatibility shims before any removal work. This note records the current callers, intended replacement APIs, and removal risks so future cleanup can be planned without surprising tests, CLI/GUI behavior, or public Java API consumers.

## Current Decision

No deprecated `LoadBalancer` shim should be removed immediately. Local test callers have been migrated away from the deprecated shims where practical, and `updateMetricsFromCloud()` now delegates to the non-deprecated wrapper. The remaining decision is public compatibility and removal policy, not urgent code cleanup.

| Method | Replacement or intended API | Current behavior | Known callers | Removal risk | Recommendation |
| --- | --- | --- | --- | --- | --- |
| `getCloudManager()` | `getCloudManagerOptional()` or `hasCloudManager()` | Returns the nullable `CloudManager` field. | No local production or test callers remain. The former `ServerMonitorTest` caller now uses `getCloudManagerOptional()`. | Medium: the method is public Java API and nullable accessor removal can break external consumers. | Keep for compatibility until a public deprecation/removal policy is chosen. |
| `updateMetricsFromCloud()` | `updateCloudMetricsIfAvailable()` | Compatibility shim that delegates to `updateCloudMetricsIfAvailable()`. The retry, delay, logging, interrupt handling, and `IOException` behavior now live behind the non-deprecated wrapper. | No local production or test callers of the deprecated method remain. `ServerMonitor` calls `updateCloudMetricsIfAvailable()`. | Medium: public API compatibility still matters even though implementation no longer lives in the deprecated method. | Keep for compatibility; no urgent code cleanup remains. |
| `handleFailover()` | `checkServerHealth()` | Delegates to `checkServerHealth()`. | No local production or test callers remain. The former `LoadBalancerTest` caller now uses `checkServerHealth()`. | Low-to-medium: local callers are migrated, but the method is public Java API. | Keep for compatibility until a public deprecation/removal policy is chosen. |
| `balanceLoad()` | `rebalanceExistingLoad()` or explicit strategy methods. | Delegates to `rebalanceExistingLoad()`. | No direct local callers found. | Low-to-medium: no local callers, but it is public Java API and documented as a legacy GUI entry point. | Do not remove now; document a public compatibility and removal timeline. |
| `getServerMonitor()` | Public behavior checks or a narrower monitor-status API. | Returns the internal `ServerMonitor` instance. | No local production or test callers remain. The former shutdown test now verifies public shutdown behavior. | Medium/high: the method exposes internals and external callers may depend on it. | Keep for compatibility until a public deprecation/removal policy is chosen. |

## Completed Local Migration Work

- Migrated `handleFailover()` test coverage to `checkServerHealth()`.
- Migrated `getCloudManager()` test coverage to `getCloudManagerOptional()`.
- Migrated `getServerMonitor()` test coverage to public shutdown behavior.
- Moved the `updateMetricsFromCloud()` implementation behind `updateCloudMetricsIfAvailable()`.
- Kept all deprecated shims in place.

## Remaining Pre-Removal Work

- Treat shim removal as a public compatibility decision, not routine cleanup.
- Decide the public compatibility policy and removal timeline for `balanceLoad()`.
- Decide whether deprecated shims should remain through the next major compatibility release.
- Add release-note guidance before removing any public shim.

## Safety Notes

- Audit note only.
- No behavior changes.
- No deprecated methods removed.
- Namespace migration is complete as of `v2.4.0`; this note now uses post-migration source paths.
- `public/main` untouched.
