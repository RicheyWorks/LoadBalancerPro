# Lightweight Reverse Proxy Mode

LoadBalancerPro includes an optional Spring MVC reverse proxy path for local and simulated upstreams. It forwards real HTTP requests through existing request-level routing strategy concepts so reviewers can validate practical forwarding behavior without cloud mutation.

This mode is disabled by default and is intentionally small. It can terminate configured inbound TLS, accept ordinary HTTP traffic over HTTP/2, and opt in to bounded WebSocket passthrough, but it is not an internet-edge gateway, benchmark harness, WAF, distributed rate limiter, or identity system.

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
loadbalancerpro.proxy.connect-timeout=1s
loadbalancerpro.proxy.request-timeout=2s
loadbalancerpro.proxy.max-request-bytes=65536
loadbalancerpro.proxy.max-response-bytes=0
loadbalancerpro.proxy.forwarded.mode=strip-and-set
loadbalancerpro.proxy.websocket.enabled=false
loadbalancerpro.proxy.websocket.connect-timeout=5s
loadbalancerpro.proxy.websocket.idle-timeout=5m
loadbalancerpro.proxy.websocket.send-timeout=10s
loadbalancerpro.proxy.websocket.max-text-message-bytes=65536
loadbalancerpro.proxy.websocket.max-binary-message-bytes=65536
loadbalancerpro.proxy.websocket.send-buffer-bytes=262144
loadbalancerpro.proxy.limits.max-in-flight=0
loadbalancerpro.proxy.limits.adaptive=false
loadbalancerpro.proxy.shedding.enabled=false
loadbalancerpro.proxy.shedding.soft-utilization-threshold=0.75
loadbalancerpro.proxy.shedding.hard-utilization-threshold=0.90
loadbalancerpro.proxy.shedding.max-p95-latency-millis=250
loadbalancerpro.proxy.shedding.max-error-rate=0.10
loadbalancerpro.proxy.shedding.priority-header=
loadbalancerpro.proxy.shedding.retry-after=1s
loadbalancerpro.proxy.health-check.enabled=false
loadbalancerpro.proxy.health-check.path=/health
loadbalancerpro.proxy.health-check.timeout=1s
loadbalancerpro.proxy.health-check.interval=30s
loadbalancerpro.proxy.health-check.healthy-threshold=2
loadbalancerpro.proxy.health-check.unhealthy-threshold=3
loadbalancerpro.proxy.retry.enabled=false
loadbalancerpro.proxy.retry.max-attempts=2
loadbalancerpro.proxy.retry.budget-percent=20
loadbalancerpro.proxy.retry.backoff.base=50ms
loadbalancerpro.proxy.retry.backoff.max=1s
loadbalancerpro.proxy.retry.retry-non-idempotent=false
loadbalancerpro.proxy.retry.methods=GET,HEAD
loadbalancerpro.proxy.retry.retry-statuses=502,503,504
loadbalancerpro.proxy.cooldown.enabled=false
loadbalancerpro.proxy.cooldown.consecutive-failure-threshold=2
loadbalancerpro.proxy.cooldown.duration=30s
loadbalancerpro.proxy.cooldown.recover-on-successful-health-check=true
loadbalancerpro.proxy.slow-start.duration=0s
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
loadbalancerpro.proxy.routes.api.hash-on=client-ip
loadbalancerpro.proxy.routes.api.request-timeout=750ms
loadbalancerpro.proxy.routes.api.headers.remove.x-internal-only=true
loadbalancerpro.proxy.routes.api.headers.set.x-route-name=api
loadbalancerpro.proxy.routes.api.headers.add.x-proxy-hop=loadbalancerpro
loadbalancerpro.proxy.routes.api.targets[0].id=local-a
loadbalancerpro.proxy.routes.api.targets[0].url=http://127.0.0.1:18081
loadbalancerpro.proxy.routes.api.targets[0].weight=1
loadbalancerpro.proxy.routes.api.targets[1].id=local-b
loadbalancerpro.proxy.routes.api.targets[1].url=http://127.0.0.1:18082
loadbalancerpro.proxy.routes.api.targets[1].weight=1
```

When `routes` are configured, matching uses the path after removing `/proxy` plus optional exact `match.host` and
`match.header.<name>` predicates. A route's predicates use AND semantics. Matching routes are ordered by exact-host
presence first, then longest `path-prefix`, then greatest header-predicate count, then lexicographically by route name
as a stable tie-break. Host matching is case-insensitive, ignores an optional numeric port, and does not support
wildcards. Header values match exactly and case-sensitively. Sensitive, forwarding, and hop-by-hop header names are
rejected as match predicates because these routing inputs are not authentication or trusted tenant identity.

A request to `/proxy/api/widgets` matches the `api` route above and forwards `/api/widgets` to one configured target.
A route-level `request-timeout` overrides the global request timeout for that route; routes without it inherit the
global value. The separate connection timeout applies when the shared HTTP client establishes an upstream connection
and requires an application restart to change. If `routes` are absent, the legacy global upstream list acts as a
single `/` route so existing demos keep working.

Named routes can partition their existing targets into deterministic percentage groups:

```properties
loadbalancerpro.proxy.routes.api.match.host=api.example.test
loadbalancerpro.proxy.routes.api.match.header.x-release-channel=preview
loadbalancerpro.proxy.routes.api.hash-on=header:X-Request-Bucket

loadbalancerpro.proxy.routes.api.split.stable.percentage=90
loadbalancerpro.proxy.routes.api.split.stable.target-ids[0]=local-a
loadbalancerpro.proxy.routes.api.split.canary.percentage=10
loadbalancerpro.proxy.routes.api.split.canary.target-ids[0]=local-b
```

Split group names are ordered lexicographically before their cumulative percentage ranges are built. Positive integer
percentages must total exactly 100, and the groups must partition every route target exactly once. The proxy hashes
the route's existing routing key into one group once per request. Affinity, health/capacity filtering, strategy
selection, and retries remain inside that group; an unavailable group does not spill into another percentage group.
Each group owns independent process-local strategy state. The protected status and admin configuration responses show
the normalized host, header names, group percentages, and target IDs but never configured header match values.
Percentage behavior is deterministic for a routing key; it is not an exact small-sample or fleet-wide traffic
guarantee and is not an authentication boundary.

Forwarding metadata defaults to `loadbalancerpro.proxy.forwarded.mode=strip-and-set`: inbound `Forwarded`, `X-Forwarded-For`, `X-Forwarded-Proto`, and `X-Forwarded-Host` values are removed and replaced with values derived from the immediate caller. `append` preserves and appends to those headers only when the immediate peer's literal address matches `loadbalancerpro.proxy.forwarded.trusted-proxies`; entries are IPv4 or IPv6 literal CIDRs such as `10.0.0.0/8`, `127.0.0.1/32`, or `2001:db8::/32`, and hostnames are rejected. An untrusted peer in `append` mode still gets strip-and-set behavior. `off` strips the four forwarding headers and emits no replacements. Configure trusted CIDRs narrowly; this trust is about the immediate peer, not every address claimed inside an inbound chain.

Named routes can apply static header rules through `headers.remove.<name>=true`, `headers.set.<name>=<value>`, and `headers.add.<name>=<value>`. The proxy removes configured headers first, replaces `set` headers second, and appends `add` headers last, after forwarding metadata is constructed. Header names and values are startup/reload validated, and hop-by-hop headers such as `Host`, `Connection`, `Content-Length`, and `Transfer-Encoding` cannot be added or set. Because route rules are trusted operator configuration, they may deliberately replace forwarding metadata after the anti-spoofing policy.

Named routes can select `CONSISTENT_HASH` and derive the routing key from the immediate peer address (the default `hash-on=client-ip`) or from `hash-on=header:<name>`. A blank or missing configured header falls back to the immediate peer address. The proxy does not treat `X-Forwarded-For` as the client-IP hash source, and forwarding, hop-by-hop, cookie, authorization, and API-key headers are rejected as hash sources. Any other configured hash header is still caller-controlled unless a trusted ingress removes and sets it, so header-keyed routing must be deployed only behind that boundary. The key affects selection only and is not logged, included in strategy explanations, or returned by proxy status.

Cookie affinity is independent of the selected strategy and is disabled while `affinity.cookie-name` is empty. Enable it only with an operator-supplied HMAC key of at least 32 UTF-8 bytes from a secret source:

```properties
loadbalancerpro.proxy.routes.api.strategy=CONSISTENT_HASH
loadbalancerpro.proxy.routes.api.hash-on=header:X-Tenant-ID
loadbalancerpro.proxy.routes.api.affinity.cookie-name=LB_AFFINITY
loadbalancerpro.proxy.routes.api.affinity.hmac-key=${LOADBALANCERPRO_PROXY_AFFINITY_HMAC_KEY}
```

The proxy verifies a route-bound HMAC-SHA-256 value before the strategy runs. A valid cookie pins only to a currently healthy, positive-weight configured target; missing, malformed, tampered, removed, unhealthy, drained, capacity-excluded, or retry-excluded targets fall back to normal strategy selection. A successful non-retryable upstream response adds or replaces the affinity cookie with `Path=/proxy`, `HttpOnly`, and `SameSite=Lax`; `Secure` is added when the inbound request is secure. Other upstream `Set-Cookie` values are preserved. Transport failures and retryable responses do not create a new pin. The affinity cookie is routing metadata, not authentication, authorization, an application session, CSRF protection, or a tenant-isolation control. Do not reuse auth/session cookie names or HMAC keys, do not commit the key, and re-evaluate browser security controls before introducing ambient-cookie authentication.

When proxy mode is enabled, startup validation requires either at least one named route with at least one target or one legacy upstream target. Connection and request timeouts must be greater than zero. Forwarding mode, trusted literal CIDRs, route header rules, hash sources, cookie names, affinity-key length, retry budget/backoff, and slow-start duration are validated before activation. Retry budget is bounded to 0–100 percent, backoff delays to 60 seconds, and slow start to 24 hours. Route names must be simple ids, path prefixes must be absolute paths, target ids must be non-blank, target URLs must be valid `http` or `https` URIs with a host, and weights must be finite and non-negative. In `WEIGHTED_ROUND_ROBIN`, `WEIGHTED_LEAST_CONNECTIONS`, `CONSISTENT_HASH`, and cookie-affinity selection, weight `0` is an operator drain signal: the target stays configured and observable but receives no new selections. Every positive finite weight remains eligible without a minimum clamp.

Global and per-upstream concurrency controls are opt-in. Set `loadbalancerpro.proxy.limits.max-in-flight` to a positive value for the process-local global cap and `upstreams[n].max-in-flight` or `routes.<name>.targets[n].max-in-flight` for process-local upstream caps; `0` means unlimited. A request holds one global permit across all of its retry attempts, while each actual upstream attempt holds that upstream's permit. If a selected upstream is full, the proxy tries another eligible target without consuming a retry; when the global cap or all eligible upstream caps are reached, it returns a fast HTTP 503 with `Retry-After` and `proxy_concurrency_limit` or `proxy_upstream_concurrency_limit`. Upstream ids share runtime state across routes, so the strictest positive cap configured for the same id is enforced.

Set `loadbalancerpro.proxy.shedding.enabled=true` only with a positive global cap. The configured soft/hard utilization, p95-latency, and recent-error thresholds map to the existing load-shedding policy; the proxy has no internal request queue, so its queue-depth signal is always zero even though `max-queue-depth` remains part of the shared policy configuration. Requests are `USER` by default. To map `CRITICAL`, `USER`, `BACKGROUND`, or `PREFETCH`, configure `loadbalancerpro.proxy.shedding.priority-header`; unknown values fail safe to `USER`. That header is trusted admission metadata and must be stripped and set by a trusted ingress rather than accepted directly from untrusted clients. `CRITICAL` requests bypass shedding when configured but never bypass the strict concurrency cap. Optional `loadbalancerpro.proxy.limits.adaptive=true` evaluates bounded process-local latency/error samples every 20 completions and adjusts the effective global cap between 1 and the configured maximum; reload starts a new adaptive policy at the configured cap.

## Operator Config Reload

When proxy mode is already enabled, operators can submit a full replacement proxy route/backend config to `POST /api/proxy/reload`. The endpoint validates the candidate config before activation, then atomically swaps the in-memory route snapshot only on success. Invalid reloads fail safe: the last known-good active config stays in use, the active config generation does not advance, and `/api/proxy/status.reload.lastReloadValidationErrors` reports the validation failure without API-key values or backend credentials.

In API-key mode, reload and the rest of the proxy/API surface require `X-API-Key` regardless of profile. In explicit `auth.mode=none`, the established reload-specific key check remains an additional local mutation boundary. In OAuth2 mode, reload requires the configured allocation/operator role. Do not expose reload outside localhost or trusted networks without the same deployment-level access control and TLS termination used for `/proxy/**`.

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

The in-flight, queue-depth, latency, and recent-error fields above are cold-start seed/fallback values. Once the proxy
has corresponding live observations for an upstream id, routing candidates use the process-local runtime measurements;
configured health, weight, capacity, and concurrency-limit values remain authoritative.

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

Responses preserve the upstream status code, body bytes, and safe response headers while streaming through a fixed buffer. `loadbalancerpro.proxy.max-response-bytes=0` leaves the streamed size unlimited by policy; a positive value rejects a known oversize before downstream commitment and aborts an unknown-length overflow after commitment without appending JSON. `text/event-stream` chunks are flushed incrementally. Retries remain possible only before downstream commitment, so bytes from separate upstream attempts are never combined. LoadBalancerPro adds:

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
- `CONSISTENT_HASH`

Applications can register an external strategy without modifying the built-in `RoutingStrategyId` enum. Give the
strategy a stable `RoutingStrategyIdentifier`, register a factory as a Spring bean, and use that identifier in the
same proxy strategy property:

```java
@Bean
RoutingStrategyRegistry routingStrategyRegistry() {
    RoutingStrategyIdentifier id = RoutingStrategyIdentifier.of("DAEDALUS_TOPOLOGY");
    return RoutingStrategyRegistry.defaultRegistry()
            .withFactory(id, () -> new DaedalusTopologyStrategy(id));
}
```

Each factory invocation must return a strategy with the registered identifier and a fresh instance when the strategy
has mutable route state. External identifiers accept letters, digits, `_`, and `.`, with `-` normalized to `_`; lookup
is case-insensitive. The enum-based routing comparison API continues to report built-in strategies only.

An external strategy that maintains an incremental index can implement `StatefulRoutingStrategy`. Before each
selection, proxy mode supplies the complete currently eligible candidate snapshot and then invokes the no-list keyed
selection hook. The same route-owned instance is retained across requests and compatible reloads while its strategy
identifier and configured target set are unchanged. Implementations must be thread-safe and should override the batch
snapshot hook when they need to remove candidates that are no longer eligible; the one-at-a-time default is intended
for stable-membership indexes. Proxy mode serializes each snapshot-and-selection cycle on the route-owned strategy
instance so concurrent requests cannot select from each other's partially applied snapshots.

`CONSISTENT_HASH` uses a process-local 128-virtual-node ring per configured route. The same key maps to the same healthy ring member while route membership is unchanged; when one member is removed, only keys that belonged to that member are remapped. Unhealthy, drained, retry-excluded, and capacity-excluded members are skipped clockwise without granting them traffic. Ring and affinity state are not shared across processes or replicas, and this local deterministic behavior is not multi-node consistency, distributed session storage, benchmark evidence, or production certification.

Configured upstreams with `healthy=false` are skipped before forwarding because the existing routing strategies consider only healthy candidates. This manual flag remains a hard disabled signal even when active health checks are enabled.

Optional active health checks can be enabled with:

```properties
loadbalancerpro.proxy.health-check.enabled=true
loadbalancerpro.proxy.health-check.path=/health
loadbalancerpro.proxy.health-check.timeout=1s
loadbalancerpro.proxy.health-check.interval=30s
loadbalancerpro.proxy.health-check.healthy-threshold=2
loadbalancerpro.proxy.health-check.unhealthy-threshold=3
```

Health checks run on dedicated daemon workers with a jittered initial delay, one scheduled task per configured upstream, and process-local in-memory snapshots. Request forwarding and status inspection read the latest snapshot without performing probe I/O. Probe responses with 2xx or 3xx status count as successes; other responses or probe failures count as failures. A healthy upstream becomes unhealthy after the configured consecutive failure threshold, and an unhealthy upstream recovers after the configured consecutive success threshold. The proxy does not start service discovery, persist health state, or contact any cloud service.

Optional bounded retries can be enabled with `loadbalancerpro.proxy.retry.enabled=true`. Retries are disabled by default, capped by `loadbalancerpro.proxy.retry.max-attempts`, and limited to `GET` and `HEAD` unless `loadbalancerpro.proxy.retry.retry-non-idempotent=true` is set. The process-local budget adds `budget-percent` credits for each admitted primary request, stores at most one retry's credits, and requires 100 credits before another attempt; the default `20` therefore sustains at most one retry per five primary requests without banking a later burst. `0` suppresses every retry and `100` permits at most one retry per primary request. A granted retry waits for full-jitter exponential backoff whose ceiling starts at `backoff.base`, doubles per retry, and stops at `backoff.max`. Budget and backoff state reset on accepted config reload and process restart. Be careful with non-idempotent methods: retrying `POST`, `PUT`, `PATCH`, or `DELETE` can duplicate upstream side effects.

Optional cooldown can be enabled with `loadbalancerpro.proxy.cooldown.enabled=true`. When an upstream reaches the configured consecutive-failure threshold, it is process-locally cooled down and skipped until the duration expires or a successful active health check recovers it. Expiry keeps half of the prior failure memory, with at least one remembered failure when the count was positive, instead of resetting the counter. Optional `loadbalancerpro.proxy.slow-start.duration` linearly ramps the effective routing weight of newly added or cooldown-recovered upstreams from zero to the configured weight; `0s` disables the ramp. The ramp affects weight-aware strategies, remains process-local, and is not a traffic-rate or capacity guarantee. Successful reload preserves resilience/ramp state for unchanged upstream ids, starts new ids at the reload instant, and removes state for deleted ids. A configured `healthy=false` upstream remains hard disabled and is not recovered by cooldown state.

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

The response reports the proxy enabled flag, selected strategy, configured routes, health-check configuration, retry budget/backoff and cooldown/slow-start configuration, current budget counters, configured/effective/current global concurrency, adaptive decision state, load-shedding configuration, per-upstream caps, configured and effective weights, ramp state, effective health state, consecutive failure and cooldown state, total forwarded count, total failure count, retry attempts, cooldown activations, per-upstream counters, status-class counters (`2xx`, `3xx`, `4xx`, `5xx`, `other`), the last selected upstream id, and reload status fields such as active config generation, last reload status, validation errors, and active route/backend counts.

Each upstream also reports process-local runtime statistics: current in-flight attempts; completed-attempt count; latency EWMA and p50/p95/p99 from the newest 256 completions; successes, failures, and error rate from the trailing 30 seconds; and the last completion timestamp. Statistics remain attached when a successful reload keeps the same upstream id. Weighted least-connections and tail-latency-aware proxy routes consume the applicable live measurements as described above.

Inspect actual recent forwarding decisions with:

```bash
curl -s http://127.0.0.1:8080/api/proxy/decisions/recent
```

The endpoint retains the newest 100 actual upstream attempts in a synchronized process-local FIFO buffer. Each retry is a separate record with a monotonic process-local decision id, capture time, active config generation, route, strategy, attempt number, strategy-or-affinity selection source, chosen upstream id, the candidate-state snapshot used for selection, response status, measured attempt latency, retryability, and outcome. Snapshot metadata reports the fixed bound, current retained count, total captured count, and total dropped count. Reads do not mutate or clear the buffer. Accepted config reloads keep prior records so their config generation remains meaningful; process restart clears records and counters.

Decision records deliberately omit request paths, queries, methods, bodies, headers, cookies, routing keys, affinity-cookie values, upstream URLs, and credentials. They are bounded local diagnostic evidence only: there is no persistence, cross-replica stream, cryptographic signing, identity attribution, request replay, load/stress evidence, throughput/p95/p99 claim, or production certification.

For a browser view of the same read-only status data, open:

```text
http://localhost:8080/proxy-status.html
```

The page uses same-origin `GET /api/proxy/status` only. It shows the upstream table, counters, retry-budget/backoff, cooldown/slow-start state, raw JSON, copyable status summary, and local demo curl commands without browser storage or backend mutation controls.

Counters, Micrometer measurements, runtime statistics, and active health state are local memory only. They are reset when the app process restarts. When a protected Prometheus Actuator endpoint is enabled, the proxy measurements are exported as fixed `lbp.proxy.*` series with bounded tags derived from closed outcomes/reasons and validated configured logical ids. There is no persistence, reset/admin mutation endpoint, external metrics store, generated runtime report, cloud mutation, distributed correlation, SLO evidence, or p95/p99 claim.

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

## HTTP/2 And WebSocket Transport

Set `server.http2.enabled=true` to enable Tomcat's inbound HTTP/2 support. A TLS listener negotiates HTTP/2 with ALPN;
Tomcat can also negotiate h2c on a cleartext development listener. This does not make gRPC or WebSocket-over-HTTP/2
extended CONNECT part of the proxy contract.

WebSocket passthrough requires both proxy mode and `loadbalancerpro.proxy.websocket.enabled=true`. The proxy selects one
upstream with the same route, split, affinity, health, DNS-discovery, admission, and concurrency rules as HTTP traffic,
then converts the configured target scheme to `ws` or `wss` while preserving authority, raw path, and query. Configure
accepted subprotocols with `loadbalancerpro.proxy.websocket.subprotocols[...]` and exact browser origins with
`loadbalancerpro.proxy.websocket.allowed-origins[...]`; an empty origin list retains Spring's same-origin policy.

Text and binary messages, the downstream send queue, connect time, send time, and idle time are bounded by the
`websocket.*` settings above. Oversized messages close with status 1009, send/connect failures close the tunnel, and
shutdown closes active tunnels. WebSocket connections are not retried. Hop-by-hop and handshake headers are rebuilt;
ordinary application headers still follow route header-removal/set/add policy. In API-key mode, configure the route to
remove the proxy credential when the upstream must not receive it:

```properties
loadbalancerpro.proxy.routes.chat.headers.remove.x-api-key=true
```

Changing WebSocket transport limits or allow-lists requires application restart. Existing tunnels retain their route
snapshot until close; removed upstreams drain under the same bounded reload lifecycle. See
[`adr/ADR-streaming-stack.md`](adr/ADR-streaming-stack.md) for the transport decision and explicit non-goals.

## Safety Boundaries

- Disabled by default: `loadbalancerpro.proxy.enabled=false`.
- No `CloudManager` construction.
- No cloud mutation.
- No external services are required by tests.
- No backend writes beyond forwarding the caller's request to the configured upstream.
- No generated runtime reports.
- No persistent proxy health or metrics state.
- No persistent or distributed retry-budget, backoff, cooldown, or slow-start state.
- No distributed/global-across-replicas concurrency enforcement or persistent adaptive state.
- No external or distributed config backend.
- No hot-reload production-readiness claim.
- No WAF, distributed rate limiting, credential rotation, RFC 8441 extended CONNECT, or production gateway guarantee.
- No benchmark, certification, legal compliance, identity, or production-readiness claim.

## Auth And TLS Boundary

Checked-in loopback proxy demos explicitly select warned `auth.mode=none` and are not a security boundary. The unqualified default API-key mode instead refuses startup without a key. Keep proxy demos bound to localhost or a trusted private network unless deployment-level access control is in place.

In API-key mode, `/proxy/**`, `GET /api/proxy/status`, and `GET /api/proxy/decisions/recent` require the configured `X-API-Key` regardless of profile. In OAuth2 mode, the same proxy surfaces require the configured allocation role, which defaults to `operator`. `/proxy-status.html` is a static same-origin page, so expose it only where callers are allowed to read the status JSON it uses.

LoadBalancerPro can terminate inbound TLS and validate TLS or mTLS to configured upstreams through named Spring SSL bundles. Certificate issuance, secret delivery, network policy, rate limiting, and public-edge hardening remain deployment responsibilities. Configure forwarded headers only when the deployment owns that trust boundary.

Do not expose `/proxy/**`, `GET /api/proxy/status`, `GET /api/proxy/decisions/recent`, `/proxy-status.html`, or Actuator endpoints publicly without deployment-level authentication, TLS termination, network policy, and rate limiting appropriate to the environment.

## Test Evidence

`ReverseProxyDisabledTest`, `ReverseProxyControllerTest`, `ReverseProxyHttp2IntegrationTest`, `ReverseProxyWebSocketIntegrationTest`, `ReverseProxyLiveDecisionCaptureTest`, `LiveRoutingDecisionStoreTest`, `ReverseProxyHealthAwareTest`, `ReverseProxyHealthMetricsTest`, `ReverseProxyFailureTest`, `ReverseProxyRetrySafetyTest`, `ReverseProxyRetryCooldownTest`, `ReverseProxyStrategyDemoLabTest`, `ProxyDemoFixtureLauncherTest`, `ProdApiKeyProtectionTest`, `OAuth2AuthorizationTest`, and `OperatorAuthTlsBoundaryDocumentationTest` use local in-process servers, unused loopback ports, MockMvc requests, or static docs assertions. They prove:

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
- process-local retry credits cap brownout retries and exponential full-jitter backoff is bounded
- non-idempotent methods are not retried by default
- cooldown activates after configured consecutive failures
- cooled-down upstreams are skipped
- cooldown expiry retains half of positive failure memory
- new and cooldown-recovered upstreams ramp effective weight under slow start
- healthy active probes can recover cooldown state and begin the recovery ramp
- strategy-specific real HTTP demos expose selected-upstream and strategy headers for round-robin, weighted round-robin, and health-aware failover behavior
- unreachable upstreams return controlled HTTP 502
- prod API-key mode protects proxy forwarding/status surfaces with `X-API-Key`
- OAuth2 mode requires the configured operator/allocation role for proxy forwarding/status surfaces
- a real Tomcat listener negotiates an inbound HTTP/2 request
- an authenticated WebSocket tunnel forwards bounded text/binary messages, headers, query, and subprotocol while rejecting unauthenticated handshakes and oversized messages
- TLS termination and public exposure controls are documented as deployment responsibilities
- proxy requests do not construct `CloudManager`

These are local/no-cloud integration tests. They reduce the simulator-only gap, but they do not prove production throughput, public internet safety, RFC 8441, browser-fleet compatibility, live certificate operations, or end-to-end identity-provider operation.

## Next Steps

Good follow-up slices are documented deployment-hardening checklists, richer real-backend walkthroughs, and packaging/operator usability around local proxy demos.
