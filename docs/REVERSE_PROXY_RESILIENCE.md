# Reverse Proxy Retry Budget, Backoff, Cooldown, And Slow Start

LoadBalancerPro's lightweight reverse proxy has optional bounded retry, full-jitter backoff, process-local cooldown, and effective-weight slow-start behavior for local and simulated upstream demos. The feature is intentionally small: it does not add cloud services, persistent state, a service-discovery system, or production gateway guarantees.

## Retry Configuration

Retries are disabled by default.

```properties
loadbalancerpro.proxy.retry.enabled=false
loadbalancerpro.proxy.retry.max-attempts=2
loadbalancerpro.proxy.retry.budget-percent=20
loadbalancerpro.proxy.retry.backoff.base=50ms
loadbalancerpro.proxy.retry.backoff.max=1s
loadbalancerpro.proxy.retry.retry-non-idempotent=false
loadbalancerpro.proxy.retry.methods=GET,HEAD
loadbalancerpro.proxy.retry.retry-statuses=502,503,504
```

When enabled, the first attempt is the original forward and the maximum attempt count remains a hard ceiling. Each admitted primary request adds `budget-percent` process-local credits, at most 100 credits are stored, and an additional attempt requires 100 credits. At the default `20`, sustained failures can therefore trigger at most one retry per five primary requests without banking a large healthy-period burst. `0` suppresses retries; `100` allows at most one retry per primary request. A granted retry waits for full jitter between zero and an exponential ceiling: `backoff.base` for the first retry, doubling for later retries, capped at `backoff.max`. The base and maximum may be `0ms` for a no-delay local test and may not exceed 60 seconds. By default, only `GET` and `HEAD` are retried. Enabling retries for `POST`, `PUT`, `PATCH`, or `DELETE` can duplicate upstream side effects and should only be used with upstream-specific idempotency controls.

## Cooldown Configuration

Cooldown is disabled by default.

```properties
loadbalancerpro.proxy.cooldown.enabled=false
loadbalancerpro.proxy.cooldown.consecutive-failure-threshold=2
loadbalancerpro.proxy.cooldown.duration=30s
loadbalancerpro.proxy.cooldown.recover-on-successful-health-check=true
loadbalancerpro.proxy.slow-start.duration=0s
```

When enabled, consecutive forwarding or active-probe failures can temporarily move an upstream into cooldown. Cooled-down upstreams are skipped by routing. On duration expiry, half of a positive consecutive-failure count is retained, with a minimum of one, so recovery no longer erases all failure memory. A successful active probe can still end cooldown early when configured. `slow-start.duration` is disabled at `0s`; when positive, newly added and cooldown-recovered upstreams linearly ramp from zero to configured routing weight for weight-aware strategies. Successful reload preserves resilience state for unchanged upstream ids while starting only newly added ids at the reload instant. These states are process-local memory only and reset when the application restarts. A configured `healthy=false` upstream remains hard disabled and is not recovered by cooldown expiration or health success.

## Status Endpoint Fields

`GET /api/proxy/status` remains read-only and includes:

- retry enabled flag, maximum attempts, budget percentage, bounded backoff, method allow-list, and retry statuses
- budget primary/granted/rejected counters and currently available credits
- cooldown enabled flag, threshold, duration, health-check recovery, and slow-start duration
- total and per-upstream retry attempt counters
- total and per-upstream cooldown activation counters
- per-upstream consecutive failure counts
- per-upstream cooldown active and remaining-duration fields
- per-upstream configured/effective weights and slow-start active/remaining-duration fields
- existing forwarding, failure, status-class, selected-upstream, and effective-health fields

No reset endpoint, database, metrics export service, generated runtime report, or cloud mutation is added.

## Browser Status View

Open `http://localhost:8080/proxy-status.html` for a read-only browser view of the same status data. The page reads same-origin `GET /api/proxy/status` only, shows retry-budget/backoff counters, per-upstream cooldown/slow-start state, raw JSON, and copyable local demo commands, and keeps optional live-refresh state in memory only.

## Local Demo Flow

Use [`PROXY_DEMO_STACK.md`](PROXY_DEMO_STACK.md) when you want the complete Windows/Unix demo-stack path with checked-in profiles, the Java fixture launcher, startup commands, curl verification, status-page review, and cleanup. See [`PROXY_DEMO_FIXTURE_LAUNCHER.md`](PROXY_DEMO_FIXTURE_LAUNCHER.md) for launcher arguments and fixture responses.

Use the loopback fixture:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode round-robin"
```

or:

```powershell
.\scripts\proxy-demo.ps1
```

Start LoadBalancerPro with the command printed by the Java launcher or helper script, or use `bash scripts/proxy-demo.sh --mode round-robin` on Unix. The printed command activates a demo profile such as `proxy-demo-round-robin`; add retry/cooldown arguments when you want to demonstrate resilience behavior:

```text
--loadbalancerpro.proxy.retry.enabled=true
--loadbalancerpro.proxy.retry.max-attempts=2
--loadbalancerpro.proxy.retry.budget-percent=100
--loadbalancerpro.proxy.retry.backoff.base=50ms
--loadbalancerpro.proxy.retry.backoff.max=1s
--loadbalancerpro.proxy.cooldown.enabled=true
--loadbalancerpro.proxy.cooldown.consecutive-failure-threshold=1
--loadbalancerpro.proxy.cooldown.duration=30s
--loadbalancerpro.proxy.slow-start.duration=10s
```

Use `curl -s http://127.0.0.1:8080/api/proxy/status` to watch retry and cooldown counters while you mark a fixture backend unhealthy or healthy again through the fixture endpoints.

For strategy-specific startup and curl recipes, use `.\scripts\proxy-demo.ps1 -Mode round-robin`, `.\scripts\proxy-demo.ps1 -Mode weighted-round-robin`, or `.\scripts\proxy-demo.ps1 -Mode failover` and follow [`PROXY_STRATEGY_DEMO_LAB.md`](PROXY_STRATEGY_DEMO_LAB.md). The lab keeps retry/cooldown behavior optional and focuses on selected-upstream evidence from real forwarded traffic.

For packaged-jar commands, Maven exec launcher recipes, and real-backend example property files, see [`OPERATOR_PACKAGING.md`](OPERATOR_PACKAGING.md). For local/private retry and cooldown examples beyond the fixture launcher, use [`REAL_BACKEND_PROXY_EXAMPLES.md`](REAL_BACKEND_PROXY_EXAMPLES.md).

## Test Evidence

`ReverseProxyRetrySafetyTest` proves that non-idempotent methods are not retried by default and that retry attempts remain bounded. `ProxyRetryPolicyTest` and `ReverseProxyRetryBudgetSlowStartTest` prove deterministic 20-percent brownout capping, exponential full-jitter ceilings, fail-closed configuration, linear new-upstream weight ramp, cooldown failure-memory retention, and recovery ramp behavior. `ReverseProxyRetryCooldownTest` proves that a granted retry can select an alternate healthy upstream, cooldown activates after a configured failure threshold, cooled-down upstreams are skipped, active health success can recover cooldown state, and no `CloudManager` is constructed.

These are deterministic local loopback tests. They do not require public internet, external services, real cloud resources, or persistent state.

## Limitations

- No production gateway, benchmark, certification, legal, identity, or security guarantee.
- No distributed retry-budget, cooldown, or slow-start state.
- No persistent metrics.
- No retry budget shared across app instances.
- No service discovery.
- No TLS termination or WebSocket support.
- No public internet dependency in tests.
