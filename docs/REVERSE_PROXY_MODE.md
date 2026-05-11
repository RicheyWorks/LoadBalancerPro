# Lightweight Reverse Proxy Mode

LoadBalancerPro includes an optional Spring MVC reverse proxy path for local and simulated upstreams. It forwards real HTTP requests through existing request-level routing strategy concepts so reviewers can validate practical forwarding behavior without cloud mutation.

This mode is disabled by default and is intentionally small. It is not a production-grade enterprise gateway, benchmark harness, TLS terminator, WebSocket proxy, WAF, distributed rate limiter, or identity system.

## Enable Proxy Mode

Start the API with proxy mode enabled and two local upstreams:

```bash
java -jar target/LoadBalancerPro-2.4.2.jar \
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
loadbalancerpro.proxy.upstreams[0].id=backend-a
loadbalancerpro.proxy.upstreams[0].url=http://127.0.0.1:18081
loadbalancerpro.proxy.upstreams[0].healthy=true
loadbalancerpro.proxy.upstreams[1].id=backend-b
loadbalancerpro.proxy.upstreams[1].url=http://127.0.0.1:18082
loadbalancerpro.proxy.upstreams[1].healthy=true
```

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

The response reports the proxy enabled flag, selected strategy, health-check configuration, configured upstreams, effective health state, total forwarded count, total failure count, per-upstream forwarded/failure counters, status-class counters (`2xx`, `3xx`, `4xx`, `5xx`, `other`), and the last selected upstream id.

Counters and active health state are local memory only. They are reset when the app process restarts. There is no persistence, reset/admin mutation endpoint, Prometheus compatibility claim, external metrics store, generated runtime report, or cloud mutation.

## Local Two-Backend Demo Fixture

For a local no-cloud reviewer demo, run the PowerShell fixture:

```powershell
.\scripts\proxy-demo.ps1
```

The script starts two loopback-only HTTP backends on ports `18081` and `18082` with distinct response headers and `/health` probes. It prints a `mvn spring-boot:run` command that enables proxy mode and active health checks. Example review commands:

```bash
curl -i http://127.0.0.1:8080/proxy/demo
curl -s http://127.0.0.1:8080/api/proxy/status
curl http://127.0.0.1:18082/fixture/health/fail
curl -i http://127.0.0.1:8080/proxy/demo
```

After marking `backend-b` unhealthy through the fixture, the active probe should report it unhealthy and the proxy should continue forwarding to the healthy backend. This is an illustrative local fixture, not a benchmark or production failover proof.

## Safety Boundaries

- Disabled by default: `loadbalancerpro.proxy.enabled=false`.
- No `CloudManager` construction.
- No cloud mutation.
- No external services are required by tests.
- No backend writes beyond forwarding the caller's request to the configured upstream.
- No generated runtime reports.
- No persistent proxy health or metrics state.
- No TLS termination, WebSocket support, WAF, distributed rate limiting, credential rotation, or production gateway guarantee.
- No benchmark, certification, legal compliance, identity, or production-readiness claim.

In OAuth2 mode, `/proxy/**` requires the configured allocation role, which defaults to `operator`. In local/default API-key mode, the app remains demo-friendly; keep proxy mode loopback-bound or behind trusted local network controls unless deployment-specific authentication and network policy are in place.

## Test Evidence

`ReverseProxyDisabledTest`, `ReverseProxyControllerTest`, `ReverseProxyHealthAwareTest`, `ReverseProxyHealthMetricsTest`, and `ReverseProxyFailureTest` use local in-process JDK `HttpServer` fixtures or unused loopback ports. They prove:

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
- unreachable upstreams return controlled HTTP 502
- proxy requests do not construct `CloudManager`

These are local/no-cloud integration tests. They reduce the simulator-only gap, but they do not prove production throughput, public internet safety, TLS behavior, WebSocket behavior, or end-to-end identity-provider operation.

## Next Steps

Good follow-up slices are strategy-specific proxy examples, documented production-hardening checklists, bounded retry policy experiments, and richer operator UI visibility for the live proxy status endpoint.
