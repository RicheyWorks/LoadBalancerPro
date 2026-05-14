# Lightweight Reverse Proxy Mode

LoadBalancerPro includes an optional Spring MVC reverse proxy path for local and simulated upstreams. It forwards real HTTP requests through existing request-level routing strategy concepts so reviewers can validate practical forwarding behavior without cloud mutation.

This mode is disabled by default and is intentionally small. It is not an internet-edge gateway, benchmark harness, TLS terminator, WebSocket proxy, WAF, distributed rate limiter, or identity system.

For copyable run profiles that combine local demo, prod API-key, cloud-sandbox API-key, OAuth2, and proxy-loopback validation, see [`OPERATOR_RUN_PROFILES.md`](OPERATOR_RUN_PROFILES.md).

## Enable Proxy Mode

Start the API with proxy mode enabled and two local upstreams:

```bash
java -jar target/LoadBalancerPro-2.5.0.jar \
  --server.address=127.0.0.1 \
  --server.port=8080 \
  --loadbalancerpro.proxy.enabled=true \
  --loadbalancerpro.proxy.strategy=ROUND_ROBIN \
  --loadbalancerpro.proxy.upstreams[0].id=backend-a \
  --loadbalancerpro.proxy.upstreams[0].url=http://127.0.0.1:18081 \
  --loadbalancerpro.proxy.upstreams[0].healthy=true \
  --loadbalancerpro.proxy.upstreams[1].id=backend-b \
  --loadbalancerpro.proxy.upstreams[1].url=http://127.0.0.1:18082 \
  --loadbalancerpro.proxy.upstreams[1].healthy=true
```

Equivalent properties:

```properties
loadbalancerpro.proxy.enabled=true
loadbalancerpro.proxy.strategy=ROUND_ROBIN
loadbalancerpro.proxy.request-timeout=2s
loadbalancerpro.proxy.max-request-bytes=65536
loadbalancerpro.proxy.health-check.enabled=false
loadbalancerpro.proxy.health-check.path=/health
loadbalancerpro.proxy.health-check.timeout=1s
loadbalancerpro.proxy.health-check.interval=30s
loadbalancerpro.proxy.retry.enabled=false
loadbalancerpro.proxy.retry.max-attempts=2
loadbalancerpro.proxy.retry.retry-non-idempotent=false
loadbalancerpro.proxy.retry.methods=GET,HEAD
loadbalancerpro.proxy.retry.retry-statuses=502,503,504
loadbalancerpro.proxy.cooldown.enabled=false
loadbalancerpro.proxy.cooldown.consecutive-failure-threshold=2
loadbalancerpro.proxy.cooldown.duration=30s
loadbalancerpro.proxy.cooldown.recover-on-successful-health-check=true
loadbalancerpro.proxy.upstreams[0].id=backend-a
loadbalancerpro.proxy.upstreams[0].url=http://127.0.0.1:18081
loadbalancerpro.proxy.upstreams[0].healthy=true
loadbalancerpro.proxy.upstreams[1].id=backend-b
loadbalancerpro.proxy.upstreams[1].url=http://127.0.0.1:18082
loadbalancerpro.proxy.upstreams[1].healthy=true
```

The global `upstreams` list remains supported for the packaged demo profiles and existing operator examples. New operator-managed configurations can instead use named routes:

```properties
loadbalancerpro.proxy.enabled=true
loadbalancerpro.proxy.routes.api.path-prefix=/api
loadbalancerpro.proxy.routes.api.strategy=ROUND_ROBIN
loadbalancerpro.proxy.routes.api.targets[0].id=local-a
loadbalancerpro.proxy.routes.api.targets[0].url=http://127.0.0.1:18081
loadbalancerpro.proxy.routes.api.targets[0].weight=1
loadbalancerpro.proxy.routes.api.targets[1].id=local-b
loadbalancerpro.proxy.routes.api.targets[1].url=http://127.0.0.1:18082
loadbalancerpro.proxy.routes.api.targets[1].weight=1
```

When `routes` are configured, the proxy selects the longest matching `path-prefix` after removing `/proxy`. A request to `/proxy/api/widgets` matches the `api` route above and forwards `/api/widgets` to one configured target. If `routes` are absent, the legacy global upstream list acts as a single `/` route so existing demos keep working.

When proxy mode is enabled, startup validation requires either at least one named route with at least one target or one legacy upstream target. Route names must be simple ids, path prefixes must be absolute paths, target ids must be non-blank, target URLs must be valid `http` or `https` URIs with a host, and weights must be greater than zero.

## Operator Config Reload

When proxy mode is already enabled, operators can submit a full replacement proxy route/backend config to `POST /api/proxy/reload`. The endpoint validates the candidate config before activation, then atomically swaps the in-memory route snapshot only on success. Invalid reloads fail safe: the last known-good active config stays in use, the active config generation does not advance, and `/api/proxy/status.reload.lastReloadValidationErrors` reports the validation failure without API-key values or backend credentials.

In API-key mode, reload requires `X-API-Key` even for local/default profile use. In prod or cloud-sandbox API-key mode the existing API-key boundary also protects this mutation. In OAuth2 mode, reload requires the configured allocation/operator role. Do not expose reload outside localhost or trusted networks without the same deployment-level access control and TLS termination used for `/proxy/**`.

Reload is local and process-scoped. It does not read remote URLs, does not contact cloud config backends, does not persist config, does not coordinate across replicas, and does not replace restart-based deployment controls. Restart remains the clearest path after changing TLS, auth, deployment secrets, JVM settings, or any config outside `loadbalancerpro.proxy.*`.

Optional upstream telemetry fields are available for strategies that use them:

```properties
loadbalancerpro.proxy.upstreams[0].weight=2.0
loadbalancerpro.proxy.upstreams[0].in-flight-request-count=3
loadbalancerpro.proxy.upstreams[0].configured-capacity=100.0
loadbalancerpro.proxy.upstreams[0].estimated-concurrency-limit=100.0
loadbalancerpro.proxy.upstreams[0].average-latency-millis=10.0
loadbalancerpro.proxy.upstreams[0].p95-latency-millis=20.0
loadbalancerpro.proxy.upstreams[0].p99-latency-millis=30.0
loadbalancerpro.proxy.upstreams[0].recent-error-rate=0.0
loadbalancerpro.proxy.upstreams[0].queue-depth=0
```

## Forward Requests

Requests under `/proxy/**` are forwarded to the selected upstream with the `/proxy` prefix removed:

```bash
curl -i http://127.0.0.1:8080/proxy/api/widgets?color=blue
```

POST bodies are forwarded:

```bash
curl -i -X POST http://127.0.0.1:8080/proxy/orders?source=demo \
  -H "Content-Type: application/json" \
  -d '{"order":42}'
```

The proxy forwards the HTTP method, path suffix, query string, request body, and practical safe headers. Hop-by-hop headers such as `Connection`, `Host`, `Content-Length`, and `Transfer-Encoding` are not forwarded.

Responses preserve the upstream status code, body, and safe response headers. LoadBalancerPro adds:

```text
X-LoadBalancerPro-Upstream: backend-a
X-LoadBalancerPro-Strategy: ROUND_ROBIN
```

## Routing And Health Behavior

Proxy mode reuses the request-level routing strategy registry. Supported configured strategy IDs are:

- `ROUND_ROBIN`
- `TAIL_LATENCY_POWER_OF_TWO`
- `WEIGHTED_LEAST_LOAD`
- `WEIGHTED_LEAST_CONNECTIONS`
- `WEIGHTED_ROUND_ROBIN`

Configured upstreams with `healthy=false` are skipped before forwarding because the existing routing strategies consider only healthy candidates. This manual flag remains a hard disabled signal even when active health checks are enabled.

Optional active health checks can be enabled with:

```properties
loadbalancerpro.proxy.health-check.enabled=true
loadbalancerpro.proxy.health-check.path=/health
loadbalancerpro.proxy.health-check.timeout=1s
loadbalancerpro.proxy.health-check.interval=30s
```

Health checks are on-demand and in-memory: the proxy probes an upstream when forwarding or status inspection needs health data and the configured interval has elapsed. Probe responses with 2xx or 3xx status are treated as healthy; other responses or probe failures are treated as unhealthy. The proxy does not start service discovery, persist health state, or contact any cloud service.

Optional bounded retries can be enabled with `loadbalancerpro.proxy.retry.enabled=true`. Retries are disabled by default, capped by `loadbalancerpro.proxy.retry.max-attempts`, and limited to `GET` and `HEAD` unless `loadbalancerpro.proxy.retry.retry-non-idempotent=true` is set. Be careful with non-idempotent methods: retrying `POST`, `PUT`, `PATCH`, or `DELETE` can duplicate upstream side effects.

Optional cooldown can be enabled with `loadbalancerpro.proxy.cooldown.enabled=true`. When an upstream reaches the configured consecutive-failure threshold, it is process-locally cooled down and skipped until the duration expires or a successful active health check recovers it. A configured `healthy=false` upstream remains hard disabled and is not recovered by cooldown state.

If no healthy upstream is available, the proxy returns HTTP 503 with a deterministic JSON error:

```json
{"error":"proxy_unavailable","message":"No healthy proxy upstreams are available."}
```

If the selected upstream cannot be reached, the proxy returns HTTP 502:

```json
{"error":"proxy_upstream_failure","message":"Proxy could not reach upstream backend-a"}
```

Failed forwarded requests increment local failure counters but do not override configured `healthy=false`; active health checks are the supported dynamic health input.

## Status And Metrics

Inspect the read-only proxy status endpoint:

```bash
curl -s http://127.0.0.1:8080/api/proxy/status
```

The response reports the proxy enabled flag, selected strategy, configured routes, health-check configuration, retry/cooldown configuration, configured upstreams, effective health state, consecutive failure and cooldown state, total forwarded count, total failure count, retry attempts, cooldown activations, per-upstream counters, status-class counters (`2xx`, `3xx`, `4xx`, `other`), the last selected upstream id, and reload status fields such as active config generation, last reload status, validation errors, and active route/backend counts.

For a browser view of the same read-only status data, open:

```text
http://localhost:8080/proxy-status.html
```

The page uses same-origin `GET /api/proxy/status` only. It shows the upstream table, counters, retry/cooldown state, raw JSON, copyable status summary, and local demo curl commands without browser storage or backend mutation controls.

Counters and active health state are local memory only. They are reset when the app process restarts. There is no persistence, reset/admin mutation endpoint, Prometheus compatibility claim, external metrics store, generated runtime report, or cloud mutation.

## Local Two-Backend Demo Fixture

For the single Windows/Unix quick-start path, checked-in demo profiles, startup commands, curl recipes, status-page verification, and cleanup steps, start with [`PROXY_DEMO_STACK.md`](PROXY_DEMO_STACK.md).

For release-free distribution smoke checks that validate packaged jar startup, static resources, proxy demo profiles, and real-backend examples without creating release assets, use [`OPERATOR_DISTRIBUTION_SMOKE_KIT.md`](OPERATOR_DISTRIBUTION_SMOKE_KIT.md).

For local SHA-256, manifest, `jar tf`, static page, proxy demo profile, and fixture launcher class verification of the built jar, use [`LOCAL_ARTIFACT_VERIFICATION.md`](LOCAL_ARTIFACT_VERIFICATION.md).

For a local no-cloud reviewer demo, use the Java fixture launcher:

```bash
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher" "-Dexec.args=--mode round-robin"
```

Classpath fallback:

```bash
mvn -q -DskipTests compile
java -cp target/classes com.richmond423.loadbalancerpro.demo.ProxyDemoFixtureLauncher --mode round-robin
```

The helper scripts also point to this Java launcher:

```powershell
.\scripts\proxy-demo.ps1
```

The launcher starts two loopback-only HTTP backends on ports `18081` and `18082` with distinct response headers and `/health` probes. It prints a `mvn spring-boot:run` command using one of the explicit demo profiles in `src/main/resources`: `proxy-demo-round-robin`, `proxy-demo-weighted-round-robin`, or `proxy-demo-failover`. Use `--mode round-robin`, `--mode weighted-round-robin`, or `--mode failover` for strategy-specific recipes. Unix users can use `bash scripts/proxy-demo.sh --mode round-robin` for the same Java fixture flow. Example review commands:

```bash
curl -i http://127.0.0.1:8080/proxy/demo
curl -s http://127.0.0.1:8080/api/proxy/status
# Browser: http://localhost:8080/proxy-status.html
curl http://127.0.0.1:18082/fixture/health/fail
curl -i http://127.0.0.1:8080/proxy/demo
```

After marking `backend-b` unhealthy through the fixture, the active probe should report it unhealthy and the proxy should continue forwarding to the healthy backend. This is an illustrative local fixture, not a benchmark or production failover proof.

For deterministic strategy-specific walkthroughs, see [`PROXY_STRATEGY_DEMO_LAB.md`](PROXY_STRATEGY_DEMO_LAB.md). It documents `ROUND_ROBIN`, `WEIGHTED_ROUND_ROBIN`, and health-aware failover flows using real forwarded HTTP responses, `X-LoadBalancerPro-Upstream`, `X-LoadBalancerPro-Strategy`, and `/proxy-status.html` evidence. For launcher-specific arguments and fixture behavior, see [`PROXY_DEMO_FIXTURE_LAUNCHER.md`](PROXY_DEMO_FIXTURE_LAUNCHER.md).

## Real-Backend Examples

When operators want to test local services instead of the fixture launcher, use the copy/adapt examples under `docs/examples/proxy`:

```text
docs/examples/proxy/application-proxy-real-backend-example.properties
docs/examples/proxy/application-proxy-real-backend-round-robin-example.properties
docs/examples/proxy/application-proxy-real-backend-weighted-example.properties
docs/examples/proxy/application-proxy-real-backend-failover-example.properties
docs/examples/proxy/application-proxy-real-backend-resilience-example.properties
```

The examples target loopback placeholders `http://localhost:9001` and `http://localhost:9002`, include explicit proxy strategy, health-check, retry, and cooldown settings, and are not active unless imported or copied into a local run configuration. See [`REAL_BACKEND_PROXY_EXAMPLES.md`](REAL_BACKEND_PROXY_EXAMPLES.md) for the copy/adapt walkthrough and [`OPERATOR_PACKAGING.md`](OPERATOR_PACKAGING.md) for packaged-jar and Maven exec recipes.

## Safety Boundaries

- Disabled by default: `loadbalancerpro.proxy.enabled=false`.
- No `CloudManager` construction.
- No cloud mutation.
- No external services are required by tests.
- No backend writes beyond forwarding the caller's request to the configured upstream.
- No generated runtime reports.
- No persistent proxy health or metrics state.
- No persistent retry or cooldown state.
- No external or distributed config backend.
- No hot-reload production-readiness claim.
- No TLS termination, WebSocket support, WAF, distributed rate limiting, credential rotation, or production gateway guarantee.
- No benchmark, certification, legal compliance, identity, or production-readiness claim.

## Auth And TLS Boundary

Local/default API-key mode stays demo-friendly for loopback demos and is not a security boundary. Keep proxy mode bound to localhost or a trusted private network unless deployment-level access control is in place.

In prod or cloud-sandbox API-key mode, `/proxy/**` and `GET /api/proxy/status` require the configured `X-API-Key`. In OAuth2 mode, the same proxy surfaces require the configured allocation role, which defaults to `operator`. `/proxy-status.html` is a static same-origin page, so expose it only where callers are allowed to read the status JSON it uses.

LoadBalancerPro does not terminate TLS for proxy traffic and does not provide end-to-end encryption between clients, this app, and upstreams. Terminate TLS at a trusted reverse proxy, ingress, managed load balancer, platform edge, or service mesh before exposing proxy mode beyond a private review environment. Configure forwarded headers only when the deployment owns that trust boundary.

Do not expose `/proxy/**`, `GET /api/proxy/status`, `/proxy-status.html`, or Actuator endpoints publicly without deployment-level authentication, TLS termination, network policy, and rate limiting appropriate to the environment.

## Test Evidence

`ReverseProxyDisabledTest`, `ReverseProxyControllerTest`, `ReverseProxyHealthAwareTest`, `ReverseProxyHealthMetricsTest`, `ReverseProxyFailureTest`, `ReverseProxyRetrySafetyTest`, `ReverseProxyRetryCooldownTest`, `ReverseProxyStrategyDemoLabTest`, `ProxyDemoFixtureLauncherTest`, `ProdApiKeyProtectionTest`, `OAuth2AuthorizationTest`, and `OperatorAuthTlsBoundaryDocumentationTest` use local in-process JDK `HttpServer` fixtures, unused loopback ports, MockMvc requests, or static docs assertions. They prove:

- proxy mode is disabled by default
- GET requests are forwarded to local upstreams
- POST bodies are forwarded
- query strings are preserved
- round-robin upstream selection is deterministic
- configured-unhealthy upstreams are skipped
- optional active health probes mark 2xx/3xx backends healthy
- optional active health probes mark failing backends unhealthy
- dynamically unhealthy upstreams are skipped
- proxy status and metrics counters are exposed read-only
- retries are disabled by default and bounded when enabled
- non-idempotent methods are not retried by default
- cooldown activates after configured consecutive failures
- cooled-down upstreams are skipped
- healthy active probes can recover cooldown state
- strategy-specific real HTTP demos expose selected-upstream and strategy headers for round-robin, weighted round-robin, and health-aware failover behavior
- unreachable upstreams return controlled HTTP 502
- prod API-key mode protects proxy forwarding/status surfaces with `X-API-Key`
- OAuth2 mode requires the configured operator/allocation role for proxy forwarding/status surfaces
- TLS termination and public exposure controls are documented as deployment responsibilities
- proxy requests do not construct `CloudManager`

These are local/no-cloud integration tests. They reduce the simulator-only gap, but they do not prove production throughput, public internet safety, TLS behavior, WebSocket behavior, or end-to-end identity-provider operation.

## Next Steps

Good follow-up slices are documented deployment-hardening checklists, richer real-backend walkthroughs, and packaging/operator usability around local proxy demos.
