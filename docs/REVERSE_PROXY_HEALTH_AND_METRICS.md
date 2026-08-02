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
loadbalancerpro.proxy.health-check.healthy-threshold=2
loadbalancerpro.proxy.health-check.unhealthy-threshold=3
loadbalancerpro.proxy.upstreams[0].id=backend-a
loadbalancerpro.proxy.upstreams[0].url=http://127.0.0.1:18081
loadbalancerpro.proxy.upstreams[0].healthy=true
loadbalancerpro.proxy.upstreams[1].id=backend-b
loadbalancerpro.proxy.upstreams[1].url=http://127.0.0.1:18082
loadbalancerpro.proxy.upstreams[1].healthy=true
```

Health checks run on dedicated daemon workers with a jittered initial delay and one scheduled task per configured upstream. Forwarding and status inspection read the latest process-local snapshot and do not perform probe I/O. A 2xx or 3xx probe response counts as a success; other statuses and probe failures count as failures. The configured consecutive rise/fall thresholds prevent a single result from changing effective health. A configured `healthy=false` upstream remains manually disabled and is not made healthy by active probes.

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
- `observability` summary with route count, backend target count, effective healthy/unhealthy backend counts, cooldown-active backend count, request totals, retry/cooldown totals, last selected upstream, and a compact readiness signal
- `securityBoundary` summary with auth mode, active profiles, whether an API-key value is configured, and whether `/proxy/**` plus `GET /api/proxy/status` are protected in the active mode
- `reload` summary with config reload support, active config generation, last reload attempt/success/failure timestamps, last reload status, validation errors, active route count, and active backend target count
- health-check path, timeout, interval, healthy threshold, and unhealthy threshold
- retry enabled flag, maximum attempts, budget percentage, backoff bounds, retry methods/statuses, and budget grant/rejection counters
- cooldown enabled flag, consecutive failure threshold, duration, health-check recovery setting, and slow-start duration
- upstream id, sanitized URL, configured/effective weights, slow-start state, configured/effective health, and last probe outcome
- per-upstream consecutive failure count, cooldown active flag, cooldown remaining milliseconds, and slow-start remaining milliseconds
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

## Operator Observability Summary

The `observability` block is intended for fast deployment checks before reading the full upstream table. Use `routeCount` and `backendTargetCount` to confirm the loaded proxy configuration, then compare `effectiveHealthyBackendCount`, `effectiveUnhealthyBackendCount`, and `cooldownActiveBackendCount` before sending traffic through `/proxy/**`.

The `readiness` value is a local process signal only:

- `proxy_disabled`: proxy mode is off.
- `proxy_enabled_without_configured_targets`: proxy mode is on but no route/target summary is visible.
- `no_effective_healthy_backends`: configured targets are currently unavailable for routing.
- `cooldown_active`: at least one backend is temporarily cooled down.
- `request_failures_observed`: the process-local counters include failed forwarding outcomes.
- `retries_observed`: retry attempts have occurred.
- `ready`: the current process sees at least one effective healthy backend and no failure/cooldown signal in the summary.

The `securityBoundary` block reports mode and booleans only. It does not expose API-key values, bearer tokens, or backend credentials. In prod or cloud-sandbox API-key mode, `apiKeyConfigured` reports only whether a key value is present.

The `reload` block is read-only status for operator-controlled proxy config reload. `activeConfigGeneration` starts at the startup config and increments only after a successful validated reload. `lastReloadStatus` is `not_attempted`, `success`, `failure`, or `unsupported`. On `failure`, `lastReloadValidationErrors` explains why the candidate config was rejected while the active route/backend counts continue to describe the last known-good config.

Structured startup and failure markers are written to the application log when proxy mode is enabled:

- `proxy.observability.startup` summarizes proxy enabled state, route count, backend target count, and health/retry/cooldown toggles.
- `proxy.observability.route` lists route name, path prefix, strategy, target count, and target ids without target URLs.
- `proxy.forward.failure`, `proxy.forward.retry`, `proxy.forward.retryable_status`, and `proxy.cooldown.activated` identify retry/cooldown/failure paths without logging secrets.

During smoke validation, correlate the smoke script's `PASS` lines with `/api/proxy/status` and these log markers. This is a retry/cooldown/failure interpretation aid, not durable monitoring or external telemetry. No external telemetry service is required.

When a protected Prometheus Actuator endpoint is enabled, Micrometer exports fixed `lbp.proxy.requests`, `lbp.proxy.latency`, `lbp.proxy.inflight`, `lbp.proxy.attempts`, `lbp.proxy.retries`, `lbp.proxy.request.bytes`, `lbp.proxy.response.bytes`, `lbp.proxy.limit.rejections`, `lbp.proxy.sheds`, `lbp.proxy.health`, and `lbp.proxy.cooldown.trips` meters. Route and upstream tags are validated logical configuration ids with bounded configuration counts; remaining tags are closed status, outcome, attempt, reason, direction, or phase values. Timing spans the complete downstream lifecycle, retries represent actual additional dispatches, and byte summaries record consumed request bytes and successfully delivered response bytes. These process-local meters do not add persistence, distributed tracing, external telemetry, SLO evidence, or p95/p99 proof.

## Retry Budget, Backoff, Cooldown, And Slow-Start Visibility

Retries, cooldown, and slow start are optional resilience aids for the lightweight local proxy path. Their behavior switches are disabled by default:

```properties
loadbalancerpro.proxy.retry.enabled=false
loadbalancerpro.proxy.cooldown.enabled=false
loadbalancerpro.proxy.slow-start.duration=0s
```

When retries are enabled, attempts remain bounded by `loadbalancerpro.proxy.retry.max-attempts`, consume process-local percentage credits, and wait with capped full-jitter exponential backoff. They default to idempotent methods (`GET`, `HEAD`); non-idempotent retries remain disabled by default because they can duplicate upstream side effects. When cooldown is enabled, consecutive forwarding or probe failures can temporarily remove an upstream from routing. Cooldown expiry retains half of a positive failure count instead of erasing all failure memory. A positive slow-start duration linearly ramps newly added or cooldown-recovered upstream weights for weight-aware strategies. These states are process-local and can recover after the configured duration or after a successful active health check when `recover-on-successful-health-check=true`.

## Local Two-Backend Demo

For the single documented demo-stack path with the Java fixture launcher, Windows and Unix commands, checked-in proxy profiles, curl recipes, status-page verification, and cleanup steps, use [`PROXY_DEMO_STACK.md`](PROXY_DEMO_STACK.md). The launcher itself is documented in [`PROXY_DEMO_FIXTURE_LAUNCHER.md`](PROXY_DEMO_FIXTURE_LAUNCHER.md).

Start two loopback fixture backends:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode round-robin"
```

or:

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

For local/private real-backend examples, see [`REAL_BACKEND_PROXY_EXAMPLES.md`](REAL_BACKEND_PROXY_EXAMPLES.md), [`OPERATOR_PACKAGING.md`](OPERATOR_PACKAGING.md), and the copy/adapt property files under `docs/examples/proxy`.

## Safety Boundaries

- Optional mode only.
- No `CloudManager` construction.
- No cloud mutation.
- No persistent metrics state.
- No database, broker, service discovery system, or external observability stack.
- No external or distributed config backend.
- No external telemetry required for these local status/log checks.
- No external network dependency in tests.
- No TLS, WebSocket, WAF, distributed rate limiting, identity, production-grade gateway, benchmark, certification, legal compliance, or security guarantee.

## Test Evidence

The reverse proxy test suite uses loopback-only JDK `HttpServer` fixtures and unused local ports. It covers disabled defaults, GET forwarding, POST/body forwarding, query preservation, configured unhealthy upstream skipping, dedicated daemon probe workers, rise/fall thresholds, stale-generation rejection during reconfiguration, dynamic unhealthy skipping, side-effect-free request/status snapshot reads, forwarding counters, failure counters, retry-budget grant/rejection counters, cooldown counters, status-class counters, unreachable upstream behavior, deterministic brownout retry capping, bounded full-jitter backoff, non-idempotent no-retry defaults, cooldown failure-memory retention/recovery, new/recovered upstream slow-start weighting, strategy-specific selected-upstream evidence, and no `CloudManager` construction.

JaCoCo coverage and skipped-test counts remain surfaced by the `Build, Test, Package, Smoke` workflow artifacts and logs.
