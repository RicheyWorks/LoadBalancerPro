# Reverse Proxy Health And Metrics

LoadBalancerPro's optional lightweight reverse proxy now includes local active health probes and in-memory forwarding counters. This strengthens the real HTTP forwarding path without adding cloud services, external observability stacks, persistent storage, or production gateway claims.

## Enable Active Health Checks

Proxy mode remains disabled by default. Active health checks are also disabled by default.

```properties
loadbalancerpro.proxy.enabled=true
loadbalancerpro.proxy.strategy=ROUND_ROBIN
loadbalancerpro.proxy.health-check.enabled=true
loadbalancerpro.proxy.health-check.path=/health
loadbalancerpro.proxy.health-check.timeout=1s
loadbalancerpro.proxy.health-check.interval=30s
loadbalancerpro.proxy.upstreams[0].id=backend-a
loadbalancerpro.proxy.upstreams[0].url=http://127.0.0.1:18081
loadbalancerpro.proxy.upstreams[0].healthy=true
loadbalancerpro.proxy.upstreams[1].id=backend-b
loadbalancerpro.proxy.upstreams[1].url=http://127.0.0.1:18082
loadbalancerpro.proxy.upstreams[1].healthy=true
```

Health checks are lazy and on-demand. Forwarding and status inspection refresh a probe only when the configured interval has elapsed. A 2xx or 3xx probe response is healthy; other statuses and probe failures are unhealthy. A configured `healthy=false` upstream remains manually disabled and is not made healthy by active probes.

## Status Endpoint

Inspect current proxy state:

```bash
curl -s http://127.0.0.1:8080/api/proxy/status
```

Or open the read-only browser view:

```text
http://localhost:8080/proxy-status.html
```

The endpoint is read-only and reports:

- proxy enabled flag
- configured strategy
- health-check path, timeout, and interval
- retry enabled flag, maximum attempts, retry methods, and retry statuses
- cooldown enabled flag, consecutive failure threshold, duration, and health-check recovery setting
- upstream id, sanitized URL, configured health, effective health, and last probe outcome
- per-upstream consecutive failure count, cooldown active flag, and cooldown remaining milliseconds
- total forwarded count
- total failure count
- total retry attempt count
- total cooldown activation count
- per-upstream forwarded and failure counters
- per-upstream retry and cooldown counters
- status-class counters for `2xx`, `3xx`, `4xx`, `5xx`, and `other`
- last selected upstream id

Counters are process-local and in memory only. They reset on restart and are not persisted or exported as a generated report.

The browser status page displays the same endpoint output as tables and raw JSON, supports manual refresh, has opt-in in-memory live refresh, and provides copyable demo commands. It does not add backend writes, reset controls, or browser storage.

## Retry And Cooldown Visibility

Retries and cooldown are optional resilience aids for the lightweight local proxy path. They are disabled by default:

```properties
loadbalancerpro.proxy.retry.enabled=false
loadbalancerpro.proxy.cooldown.enabled=false
```

When retries are enabled, attempts are bounded by `loadbalancerpro.proxy.retry.max-attempts` and default to idempotent methods (`GET`, `HEAD`). Non-idempotent retries remain disabled by default because they can duplicate upstream side effects. When cooldown is enabled, consecutive forwarding or probe failures can temporarily remove an upstream from routing. Cooldown state is process-local and can recover after the configured duration or after a successful active health check when `recover-on-successful-health-check=true`.

## Local Two-Backend Demo

For the single documented demo-stack path with the Java fixture launcher, Windows and Unix commands, checked-in proxy profiles, curl recipes, status-page verification, and cleanup steps, use [`PROXY_DEMO_STACK.md`](PROXY_DEMO_STACK.md). The launcher itself is documented in [`PROXY_DEMO_FIXTURE_LAUNCHER.md`](PROXY_DEMO_FIXTURE_LAUNCHER.md).

Start two loopback fixture backends:

```powershell
.\scripts\proxy-demo.ps1
```

In a second terminal, use the command printed by the Java launcher or helper script to start LoadBalancerPro with the selected `proxy-demo-*` profile and active health checks enabled. Unix users can start the same fixture with `bash scripts/proxy-demo.sh --mode round-robin`. Then try:

```bash
curl -i http://127.0.0.1:8080/proxy/demo
curl -s http://127.0.0.1:8080/api/proxy/status
curl http://127.0.0.1:18082/fixture/health/fail
curl -i http://127.0.0.1:8080/proxy/demo
curl -s http://127.0.0.1:8080/api/proxy/status
http://localhost:8080/proxy-status.html
```

The fixture has no cloud dependency and no public internet dependency. It is meant for local reviewer demos of routing and failover visibility, not throughput proof.

For strategy-specific loopback recipes, run the same fixture with `-Mode round-robin`, `-Mode weighted-round-robin`, or `-Mode failover` and follow [`PROXY_STRATEGY_DEMO_LAB.md`](PROXY_STRATEGY_DEMO_LAB.md). Those flows pair forwarded response headers with `/proxy-status.html` counters so reviewers can verify selected-upstream behavior without cloud dependencies.

## Safety Boundaries

- Optional mode only.
- No `CloudManager` construction.
- No cloud mutation.
- No persistent metrics state.
- No database, broker, service discovery system, or external observability stack.
- No external network dependency in tests.
- No TLS, WebSocket, WAF, distributed rate limiting, identity, production-grade gateway, benchmark, certification, legal compliance, or security guarantee.

## Test Evidence

The reverse proxy test suite uses loopback-only JDK `HttpServer` fixtures and unused local ports. It covers disabled defaults, GET forwarding, POST/body forwarding, query preservation, configured unhealthy upstream skipping, active health probes, dynamic unhealthy skipping, read-only status output, forwarding counters, failure counters, retry counters, cooldown counters, status-class counters, unreachable upstream behavior, bounded retry behavior, non-idempotent no-retry defaults, cooldown recovery, strategy-specific selected-upstream evidence, and no `CloudManager` construction.

JaCoCo coverage and skipped-test counts remain surfaced by the `Build, Test, Package, Smoke` workflow artifacts and logs.
