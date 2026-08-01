# LoadBalancerPro

LoadBalancerPro is a Java 17 / Spring Boot adaptive-routing service and controlled enterprise lab. It combines a calculation API, deterministic routing comparison, an opt-in HTTP reverse proxy, operator status surfaces, durable local experiment evidence, and guarded cloud-management code.

The default posture is conservative: API-key authentication is selected, proxying and LASE shadow mode are disabled, cloud mutation is dry-run, telemetry export is off, and only health/info Actuator endpoints are exposed.

## Current capabilities

- Capacity-aware, predictive, and evaluation-only allocation APIs.
- Deterministic request-level comparison for round-robin, weighted round-robin, weighted least-load, weighted least-connections, and tail-latency-aware strategies.
- Read-only Decision Explorer and browser cockpit surfaces for local review.
- Optional reverse proxy with configured routes, bounded request size and timeout, live per-upstream routing telemetry, active health checks, retry controls, cooldown/recovery, process-local concurrency/load-shedding controls, status, and guarded reload.
- Enterprise Lab scenarios, decisions, experiments, allocation supervision, durable chained JSONL evidence, compaction, recovery, ownership, and packaged proof tools.
- API-key and OAuth2 resource-server modes with deny-by-default API classification.
- Actuator health/readiness, optional Prometheus metrics, and optional OTLP metrics export with endpoint validation.
- A guarded AWS `CloudManager` boundary using AWS SDK v2; live mutation requires explicit operator and account/region/capacity gates.
- Executable Spring Boot JAR, non-root Docker image, CycloneDX SBOM generation, and local smoke helpers.

## Honest boundaries

This repository provides deployable software and controlled local evidence; it is not production certification. It does not by itself prove public-ingress safety, high availability, real-tenant or live-cloud operation, production SLOs, sustained load/soak capacity, p95/p99 guarantees, identity lifecycle, secret rotation, or environment-specific disaster recovery.

The `local` profile disables authentication and is for loopback development only. Lab decisions and Decision Explorer output do not shift production traffic. Cloud mutation remains disabled unless every live-mode guard is deliberately configured. TLS termination and network access control are deployment responsibilities.

## Requirements

- Java 17 or later
- Maven 3.9 or later
- Docker, optional
- PowerShell or Bash for the supplied smoke helpers

## Quick start

Run the local profile:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.address=127.0.0.1 --spring.profiles.active=local"
```

Verify the service:

```bash
curl -fsS http://127.0.0.1:8080/api/health
```

Open:

- [Landing page](http://127.0.0.1:8080/)
- [Load-balancing cockpit](http://127.0.0.1:8080/load-balancing-cockpit.html)
- [Decision Explorer](http://127.0.0.1:8080/decision-explorer.html)
- [Enterprise Lab](http://127.0.0.1:8080/enterprise-lab.html)
- [Proxy status](http://127.0.0.1:8080/proxy-status.html)
- [Swagger UI](http://127.0.0.1:8080/swagger-ui.html)

Build and run the executable JAR:

```bash
mvn -B package
java -jar "$(bash scripts/resolve-executable-jar.sh)" \
  --server.address=127.0.0.1 \
  --server.port=18080 \
  --spring.profiles.active=local
```

PowerShell resolves the same Maven `project.build.finalName`:

```powershell
$jar = & .\scripts\resolve-executable-jar.ps1
java -jar $jar --server.address=127.0.0.1 --server.port=18080 --spring.profiles.active=local
```

## Run profiles

| Profile | Intended use | Authentication | Proxy |
| --- | --- | --- | --- |
| default | Protected configuration baseline | API key; startup fails without a configured key | Disabled |
| `local` | Loopback development and browser review | Disabled with a startup warning | Disabled |
| `prod` | Protected deployment baseline | API key from `LOADBALANCERPRO_API_KEY` | Disabled |
| `cloud-sandbox` | Guarded sandbox configuration | API key; cloud dry-run and mutation disabled | Disabled |
| OAuth2 configuration | Trusted issuer/JWK deployment | JWT roles | Disabled unless explicitly enabled |
| `proxy-demo-*` | Scripted loopback proxy fixtures | Disabled; loopback only | Explicitly enabled |

Copyable profile examples are under [`docs/examples/operator-run-profiles`](docs/examples/operator-run-profiles). See [`OPERATOR_RUN_PROFILES.md`](docs/OPERATOR_RUN_PROFILES.md) for the full run matrix.

## Configuration and secure defaults

Important defaults in `application.properties`:

| Property | Default | Effect |
| --- | --- | --- |
| `loadbalancerpro.auth.mode` | `api-key` | Protected API mode |
| `loadbalancerpro.proxy.enabled` | `false` | No forwarding until explicitly enabled |
| `loadbalancerpro.lase.shadow.enabled` | `false` | No shadow evaluation by default |
| `loadbalancerpro.api.max-request-bytes` | `16384` | Bounded API request bodies |
| `loadbalancerpro.proxy.max-request-bytes` | `65536` | Bounded proxied request bodies |
| `loadbalancerpro.proxy.connect-timeout` | `1s` | Bounded upstream connection establishment |
| `loadbalancerpro.proxy.request-timeout` | `2s` | Bounded upstream request |
| `loadbalancerpro.proxy.routes.<name>.request-timeout` | inherits global | Per-route upstream request bound |
| `loadbalancerpro.proxy.routes.<name>.hash-on` | `client-ip` | Consistent-hash key source; immediate peer address or an operator-configured header that requires a trusted ingress boundary |
| `loadbalancerpro.proxy.routes.<name>.affinity.cookie-name` | empty | Enables route-local signed routing affinity only when paired with an operator-supplied HMAC key |
| `loadbalancerpro.proxy.forwarded.mode` | `strip-and-set` | Replace caller-supplied forwarding metadata by default |
| `loadbalancerpro.proxy.limits.max-in-flight` | `0` | Process-local global cap; `0` leaves it unlimited |
| `loadbalancerpro.proxy.limits.adaptive` | `false` | Opt-in latency-feedback adjustment below the configured cap |
| `loadbalancerpro.proxy.shedding.enabled` | `false` | Opt-in priority-aware process-local load shedding |
| `loadbalancerpro.proxy.health-check.enabled` | `false` | Opt-in active checks |
| `loadbalancerpro.proxy.health-check.healthy-threshold` | `2` | Successful background probes required to recover |
| `loadbalancerpro.proxy.health-check.unhealthy-threshold` | `3` | Failed background probes required to mark unhealthy |
| `loadbalancerpro.proxy.retry.enabled` | `false` | Opt-in retries |
| `loadbalancerpro.proxy.retry.retry-non-idempotent` | `false` | No default non-idempotent retry |
| `loadbalancerpro.proxy.cooldown.enabled` | `false` | Opt-in backend cooldown |
| `management.prometheus.metrics.export.enabled` | `false` | No default Prometheus export |
| `management.otlp.metrics.export.enabled` | `false` | No default OTLP export |
| `management.endpoints.web.exposure.include` | `health,info` | Minimal Actuator exposure |

In API-key mode, `GET /api/health` and unauthenticated `OPTIONS` are the public API exceptions. Other `/api/**` routes, `/proxy/**`, OpenAPI, and Swagger require the configured key. Use:

```bash
export LOADBALANCERPRO_API_KEY='supply-from-a-secret-manager'
java -jar "$(bash scripts/resolve-executable-jar.sh)" --spring.profiles.active=prod
```

Do not commit API keys, OAuth tokens, AWS credentials, telemetry headers, private keys, or production targets. Terminate TLS at a trusted reverse proxy, ingress, managed load balancer, platform edge, or service mesh before shared-network exposure.

OTLP metrics are opt-in. When enabled, the endpoint validator rejects blank or malformed URLs, embedded credentials, query strings, fragments, disallowed localhost, and obvious public hosts when private endpoints are required:

```properties
management.otlp.metrics.export.enabled=true
management.otlp.metrics.export.url=http://localhost:4318/v1/metrics
loadbalancerpro.telemetry.otlp.require-private-endpoint=true
```

## Proxy operation

Proxy mode requires explicit routes or upstreams. Start with the loopback smoke instead of adapting production targets directly:

```powershell
pwsh ./scripts/smoke/operator-run-profiles-smoke.ps1 -Package
```

The proxy exposes forwarding under `/proxy/**`, read-only state at `GET /api/proxy/status`, and guarded configuration reload at `POST /api/proxy/reload`. Operator configuration examples are under [`docs/examples/proxy`](docs/examples/proxy).

Before enabling private-network validation, review:

- [`REVERSE_PROXY_MODE.md`](docs/REVERSE_PROXY_MODE.md)
- [`LIVE_PROXY_CONTAINMENT.md`](docs/LIVE_PROXY_CONTAINMENT.md)
- [`PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md`](docs/PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md)
- [`API_SECURITY.md`](docs/API_SECURITY.md)

## API and operator surfaces

| Surface | Purpose |
| --- | --- |
| `GET /api/health` | Lightweight application health |
| `POST /api/allocate/capacity-aware` | Capacity-aware calculation |
| `POST /api/allocate/predictive` | Predictive calculation |
| `POST /api/allocate/evaluate` | Allocation evaluation with optional LASE summaries |
| `POST /api/routing/compare` | Read-only strategy comparison |
| `POST /api/routing/decision-explorer` | Compact routing explanation |
| `/api/lab/**` | Controlled lab scenarios, decisions, runs, policy, metrics, and experiments |
| `/api/enterprise-lab/**` | Reviewer summaries and evidence views |
| `/proxy/**` | Optional HTTP forwarding |
| `GET /api/proxy/status` | Proxy configuration, health, and live per-upstream runtime statistics |
| `POST /api/proxy/reload` | Guarded proxy configuration reload |
| `/actuator/health` | Spring Boot health and readiness |

Request and response contracts live in [`API_CONTRACTS.md`](docs/API_CONTRACTS.md). Generated OpenAPI is available at `/v3/api-docs` when permitted by the selected auth mode.

## Build and verification

Run the full test suite:

```bash
mvn -B test
```

Build the executable artifact:

```bash
mvn -B package
```

Run verification and generate JaCoCo reports:

```bash
mvn -B verify
```

Generate CycloneDX JSON and XML SBOMs:

```bash
mvn -B org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom \
  -DoutputFormat=all \
  -DoutputDirectory=target \
  -DoutputName=bom \
  -DincludeProvidedScope=false \
  -Dcyclonedx.skipAttach=true
```

Inspect the packaged artifact:

```bash
bash scripts/local-artifact-verify.sh --build
bash scripts/operator-distribution-smoke.sh --package --run-jar-smoke
```

PowerShell equivalents:

```powershell
pwsh ./scripts/local-artifact-verify.ps1 -Build
pwsh ./scripts/operator-distribution-smoke.ps1 -Package -RunJarSmoke
```

Build and smoke the protected container:

```bash
docker build -t loadbalancerpro:local .
docker run --rm --name loadbalancerpro-local \
  -p 127.0.0.1:8080:8080 \
  -e LOADBALANCERPRO_API_KEY=CHANGE_ME_LOCAL_API_KEY \
  loadbalancerpro:local
curl -fsS http://127.0.0.1:8080/api/health
```

The Docker image runs as a non-root user and defaults to the `prod` profile. Keep the host port loopback-bound for local checks. See [`CONTAINER_DEPLOYMENT.md`](docs/CONTAINER_DEPLOYMENT.md) for networking and TLS boundaries.

## Operator workflow

1. Choose a profile and review its effective properties.
2. Supply secrets through deployment secret management.
3. Keep proxy, cloud mutation, telemetry export, and expanded Actuator exposure disabled unless required.
4. Build and verify the exact artifact.
5. Start on loopback, verify `/api/health` and `/actuator/health`, then test protected routes.
6. If proxying is enabled, validate route targets, health behavior, retries, cooldown, status, and reload before widening access.
7. Review logs and metrics for sanitized configuration summaries and failures.
8. Apply deployment-specific TLS, network policy, authentication, rate limits, resource limits, monitoring, backup, and rollback controls.

For the packaged-application proof path use [`DEPLOYMENT_SMOKE_KIT.md`](docs/DEPLOYMENT_SMOKE_KIT.md). For controlled lab tooling use [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](docs/LOCAL_LAB_MANUAL_TOOLING_INDEX.md).

## Troubleshooting

- **Startup says the API key is missing:** set `LOADBALANCERPRO_API_KEY` for `prod`/default API-key mode, or explicitly use `local` only on loopback.
- **The executable JAR cannot be found:** run `mvn -B package`, then use the resolver script; do not hard-code an artifact filename.
- **A protected route returns 401:** include `X-API-Key` or configure the intended OAuth2 issuer, JWK source, and roles.
- **Proxy status says disabled:** enable proxy mode only through a reviewed profile and provide valid routes/upstreams.
- **An upstream is skipped:** inspect health, retry, cooldown, timeout, and route status in `/api/proxy/status` and application logs.
- **OTLP startup validation fails:** use a trusted private/internal collector URL without credentials, query parameters, or fragments.
- **Docker cannot reach a host backend:** `127.0.0.1` inside a container is the container; use an explicit Docker network or platform host gateway.
- **A port is already in use:** select another `server.port` and keep the bind address explicit.
- **Local Maven trust errors occur:** repair the workstation/JDK trust store; do not disable TLS verification.

## Architecture and security references

- [`LOADBALANCERPRO_NEXT_LEVEL_ARCHITECTURE.md`](docs/architecture/LOADBALANCERPRO_NEXT_LEVEL_ARCHITECTURE.md)
- [`ENTERPRISE_LAB_DURABLE_EVIDENCE.md`](docs/architecture/ENTERPRISE_LAB_DURABLE_EVIDENCE.md)
- [`ENTERPRISE_LAB_INDEPENDENT_ALLOCATION_SUPERVISOR.md`](docs/architecture/ENTERPRISE_LAB_INDEPENDENT_ALLOCATION_SUPERVISOR.md)
- [`SECURITY.md`](SECURITY.md)
- [`API_SECURITY.md`](docs/API_SECURITY.md)
- [`DEPLOYMENT_HARDENING_GUIDE.md`](docs/DEPLOYMENT_HARDENING_GUIDE.md)
- [`OPERATIONS_RUNBOOK.md`](docs/OPERATIONS_RUNBOOK.md)
- [`REVIEWER_TRUST_MAP.md`](docs/REVIEWER_TRUST_MAP.md)

Repository contribution and agent rules are in [`CONTRIBUTING.md`](CONTRIBUTING.md), [`AGENTS.md`](AGENTS.md), and [`BUILD_CONTRACT.md`](BUILD_CONTRACT.md).
