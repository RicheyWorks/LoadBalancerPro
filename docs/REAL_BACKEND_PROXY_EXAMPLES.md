# Real-Backend Proxy Examples

Use this guide when you want to adapt the lightweight `/proxy/**` mode to local or private HTTP services instead of the packaged fixture backends.

These examples are copy/adapt starting points. They do not change `src/main/resources/application.properties`, do not enable proxy mode by default, do not contact public upstreams, do not require cloud services, and do not construct or mutate `CloudManager`.

For the top-level reviewer path that connects these examples to proxy docs, status UI evidence, CI artifacts, and release-free checklists, see [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md).

## What Counts Here

A real backend in this guide means one of these operator-controlled HTTP targets:

- a local service on the same workstation, such as `http://localhost:9001`;
- a private/internal HTTP service that you control;
- a test service with a health endpoint you can safely probe.

Do not put public upstream URLs, credentials, tokens, or cloud-managed mutation paths in these example files. Keep production exposure decisions outside this guide.

## Example Profiles

The copy/adapt files live under `docs/examples/proxy`:

```text
docs/examples/proxy/application-proxy-real-backend-round-robin-example.properties
docs/examples/proxy/application-proxy-real-backend-example.properties
docs/examples/proxy/application-proxy-real-backend-weighted-example.properties
docs/examples/proxy/application-proxy-real-backend-failover-example.properties
docs/examples/proxy/application-proxy-real-backend-resilience-example.properties
```

`application-proxy-real-backend-example.properties` remains the compatibility round-robin example used by older operator docs. Prefer the strategy-named files for new reviews.

Each profile uses loopback placeholders:

```properties
loadbalancerpro.proxy.upstreams[0].url=http://localhost:9001
loadbalancerpro.proxy.upstreams[1].url=http://localhost:9002
```

Replace those values only with loopback or private HTTP services you control.

## Start Two Local Services

Start two local HTTP services on ports `9001` and `9002`. They should return a recognizable body and, for health-aware examples, expose `GET /health`.

You can use your own service, a small internal test app, or the Java fixture launcher from [`PROXY_DEMO_FIXTURE_LAUNCHER.md`](PROXY_DEMO_FIXTURE_LAUNCHER.md) if you need a deterministic fallback.

## ROUND_ROBIN Example

Use `application-proxy-real-backend-round-robin-example.properties` for explicit strategy-named review, or the legacy compatibility file `application-proxy-real-backend-example.properties`.

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --spring.config.import=optional:file:docs/examples/proxy/application-proxy-real-backend-round-robin-example.properties
```

Send repeated requests through the proxy:

```bash
curl -i http://127.0.0.1:8080/proxy/health
curl -i http://127.0.0.1:8080/proxy/health
curl -i http://127.0.0.1:8080/proxy/api/example
```

Verify these response headers:

```text
X-LoadBalancerPro-Upstream: local-api-a or local-api-b
X-LoadBalancerPro-Strategy: ROUND_ROBIN
```

Open `http://localhost:8080/proxy-status.html` and confirm the proxy is enabled, the strategy is `ROUND_ROBIN`, both upstreams are visible, and counters increment.

## WEIGHTED_ROUND_ROBIN Example

Use `application-proxy-real-backend-weighted-example.properties` when one local service should receive a higher share of selected requests.

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --spring.config.import=optional:file:docs/examples/proxy/application-proxy-real-backend-weighted-example.properties
```

The example sets `local-api-a` weight to `3.0` and `local-api-b` weight to `1.0`. Send a short request sequence:

```bash
curl -i http://127.0.0.1:8080/proxy/health
curl -i http://127.0.0.1:8080/proxy/health
curl -i http://127.0.0.1:8080/proxy/health
curl -i http://127.0.0.1:8080/proxy/health
```

Verify:

```text
X-LoadBalancerPro-Upstream: local-api-a or local-api-b
X-LoadBalancerPro-Strategy: WEIGHTED_ROUND_ROBIN
```

Treat the headers and `/api/proxy/status` counters as selected-upstream evidence for this local configuration. Do not treat a short sequence as a benchmark or capacity proof.

## Health-Aware Failover Example

Use `application-proxy-real-backend-failover-example.properties` when your local services expose `GET /health`.

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --spring.config.import=optional:file:docs/examples/proxy/application-proxy-real-backend-failover-example.properties
```

With both services healthy, requests can select either backend. Then stop one service or make its `/health` endpoint return a failing status. Send another request:

```bash
curl -i http://127.0.0.1:8080/proxy/health
```

Verify the healthy backend is selected and the failing backend appears unhealthy in:

```text
http://localhost:8080/proxy-status.html
http://127.0.0.1:8080/api/proxy/status
```

The `healthy=false` upstream setting remains a hard disable. Do not use health recovery to bypass a backend that you intentionally disabled in configuration.

## Retry And Cooldown Example

Use `application-proxy-real-backend-resilience-example.properties` when you want local retry/cooldown evidence against services you control.

```bash
java -jar target/LoadBalancerPro-2.4.2.jar --spring.config.import=optional:file:docs/examples/proxy/application-proxy-real-backend-resilience-example.properties
```

This example enables bounded `GET`/`HEAD` retries and process-local cooldown:

```properties
loadbalancerpro.proxy.retry.enabled=true
loadbalancerpro.proxy.retry.max-attempts=2
loadbalancerpro.proxy.retry.retry-non-idempotent=false
loadbalancerpro.proxy.cooldown.enabled=true
```

Stop one backend or make it return a retryable failure, then send requests through `/proxy/**`. Verify retry counters, cooldown activations, consecutive failure counts, and cooldown-active state in `/proxy-status.html` or `/api/proxy/status`.

## Status And Evidence Checklist

For each example, capture the same evidence:

- response status and body from `curl -i http://127.0.0.1:8080/proxy/...`;
- `X-LoadBalancerPro-Upstream` selected-upstream header;
- `X-LoadBalancerPro-Strategy` strategy header;
- `/proxy-status.html` upstream table, counters, health state, retry/cooldown state, and last selected upstream;
- `GET /api/proxy/status` raw JSON when a text artifact is easier to attach to review notes.

Tie this evidence back to release-free operator review:

- [`CI_ARTIFACT_CONSUMER_GUIDE.md`](CI_ARTIFACT_CONSUMER_GUIDE.md) for JaCoCo, packaged artifact smoke, and SBOM workflow artifacts.
- [`LOCAL_ARTIFACT_VERIFICATION.md`](LOCAL_ARTIFACT_VERIFICATION.md) for local checksum and jar resource inspection.
- [`RELEASE_CANDIDATE_DRY_RUN.md`](RELEASE_CANDIDATE_DRY_RUN.md) for the go/no-go packet.
- [`RELEASE_INTENT_CHECKLIST.md`](RELEASE_INTENT_CHECKLIST.md) for the hard stop before any future release action.

## Limitations

- The proxy is lightweight and optional.
- Proxy mode remains disabled by default.
- These examples are not production gateway configurations.
- These examples do not prove throughput, latency, availability, TLS termination, WebSocket support, WAF behavior, identity enforcement, legal compliance, or security posture.
- Metrics and cooldown state are process-local.
- Public exposure, TLS, authentication, ingress, rate limits, and service ownership checks belong to the deployment environment.
- This guide does not create tags, GitHub Releases, release assets, or `release-downloads/` evidence.
