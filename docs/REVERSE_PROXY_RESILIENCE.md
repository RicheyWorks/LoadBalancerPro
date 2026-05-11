# Reverse Proxy Retry And Cooldown Resilience

LoadBalancerPro's lightweight reverse proxy has optional bounded retry and process-local cooldown behavior for local and simulated upstream demos. The feature is intentionally small: it does not add cloud services, persistent state, a service-discovery system, or production gateway guarantees.

## Retry Configuration

Retries are disabled by default.

```properties
loadbalancerpro.proxy.retry.enabled=false
loadbalancerpro.proxy.retry.max-attempts=2
loadbalancerpro.proxy.retry.retry-non-idempotent=false
loadbalancerpro.proxy.retry.methods=GET,HEAD
loadbalancerpro.proxy.retry.retry-statuses=502,503,504
```

When enabled, the proxy still uses a bounded maximum attempt count. The first attempt is the original forward. Additional attempts select another eligible upstream when one is available. By default, only `GET` and `HEAD` are retried. Enabling retries for `POST`, `PUT`, `PATCH`, or `DELETE` can duplicate upstream side effects and should only be used with upstream-specific idempotency controls.

## Cooldown Configuration

Cooldown is disabled by default.

```properties
loadbalancerpro.proxy.cooldown.enabled=false
loadbalancerpro.proxy.cooldown.consecutive-failure-threshold=2
loadbalancerpro.proxy.cooldown.duration=30s
loadbalancerpro.proxy.cooldown.recover-on-successful-health-check=true
```

When enabled, consecutive forwarding or active-probe failures can temporarily move an upstream into cooldown. Cooled-down upstreams are skipped by routing. Cooldown state is process-local memory only and resets when the application restarts. A configured `healthy=false` upstream remains hard disabled and is not recovered by cooldown expiration or health success.

## Status Endpoint Fields

`GET /api/proxy/status` remains read-only and includes:

- retry enabled flag, maximum attempts, method allow-list, and retry statuses
- cooldown enabled flag, threshold, duration, and health-check recovery setting
- total and per-upstream retry attempt counters
- total and per-upstream cooldown activation counters
- per-upstream consecutive failure counts
- per-upstream cooldown active and remaining-duration fields
- existing forwarding, failure, status-class, selected-upstream, and effective-health fields

No reset endpoint, database, metrics export service, generated runtime report, or cloud mutation is added.

## Browser Status View

Open `http://localhost:8080/proxy-status.html` for a read-only browser view of the same status data. The page reads same-origin `GET /api/proxy/status` only, shows retry/cooldown counters, per-upstream cooldown state, raw JSON, and copyable local demo commands, and keeps optional live-refresh state in memory only.

## Local Demo Flow

Use the loopback fixture:

```powershell
.\scripts\proxy-demo.ps1
```

Start LoadBalancerPro with the command printed by the script, then add retry/cooldown arguments when you want to demonstrate resilience behavior:

```text
--loadbalancerpro.proxy.retry.enabled=true
--loadbalancerpro.proxy.retry.max-attempts=2
--loadbalancerpro.proxy.cooldown.enabled=true
--loadbalancerpro.proxy.cooldown.consecutive-failure-threshold=1
--loadbalancerpro.proxy.cooldown.duration=30s
```

Use `curl -s http://127.0.0.1:8080/api/proxy/status` to watch retry and cooldown counters while you mark a fixture backend unhealthy or healthy again through the fixture endpoints.

For strategy-specific startup and curl recipes, use `.\scripts\proxy-demo.ps1 -Mode round-robin`, `.\scripts\proxy-demo.ps1 -Mode weighted-round-robin`, or `.\scripts\proxy-demo.ps1 -Mode failover` and follow [`PROXY_STRATEGY_DEMO_LAB.md`](PROXY_STRATEGY_DEMO_LAB.md). The lab keeps retry/cooldown behavior optional and focuses on selected-upstream evidence from real forwarded traffic.

## Test Evidence

`ReverseProxyRetrySafetyTest` proves that non-idempotent methods are not retried by default and that retry attempts remain bounded. `ReverseProxyRetryCooldownTest` proves that a retry can select an alternate healthy upstream, cooldown activates after a configured failure threshold, cooled-down upstreams are skipped, active health success can recover cooldown state, and no `CloudManager` is constructed.

These are deterministic local loopback tests. They do not require public internet, external services, real cloud resources, or persistent state.

## Limitations

- No production gateway, benchmark, certification, legal, identity, or security guarantee.
- No distributed cooldown state.
- No persistent metrics.
- No retry budget shared across app instances.
- No service discovery.
- No TLS termination or WebSocket support.
- No public internet dependency in tests.
