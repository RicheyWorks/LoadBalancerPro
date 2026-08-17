# LoadBalancerPro

LoadBalancerPro is a Java 17 / Spring Boot reverse proxy plus a controlled, opt-in enterprise lab. The default deployable artifact contains the real proxy runtime and its operator status surface; calculation, simulation, experiment evidence, demos, and guarded cloud-management tools are isolated in a Maven `lab` profile.

The default posture is conservative: API-key authentication is selected, proxying and LASE shadow mode are disabled, cloud mutation is dry-run, telemetry export is off, and only health/info Actuator endpoints are exposed.

## Current capabilities

- Opt-in reverse proxy data plane with configured routes, bounded request size and timeout, live per-upstream routing
  telemetry, exact rolling tail latency, active health checks, retry budgets/backoff, cooldown/slow-start recovery,
  process-local concurrency/load-shedding controls, status, recent decisions, and guarded reload.
- Production-boundary executable Spring Boot JAR, non-root proxy Docker image, hardened Compose deployment, CycloneDX
  SBOM generation, and load smoke/soak helpers.
- Fail-closed reviewed-staging qualification plus a CI-gated two-replica active-active topology proof covering
  distribution, configuration convergence, rollout, rollback, replica loss, recovery, and aggregate upstream limits.
- API-key and OAuth2 resource-server modes with deny-by-default API classification.
- Actuator health/readiness, optional Prometheus metrics, and optional OTLP metrics export with endpoint validation.
- Capacity-aware, predictive, and evaluation-only allocation APIs in the separate source/Lab Tools runtime.
- Deterministic request-level comparison, Decision Explorer/browser cockpit surfaces, Enterprise Lab scenarios, durable
  evidence, and packaged proof tools in the opt-in Lab Tools artifact.
- A guarded AWS `CloudManager` boundary using AWS SDK v2; live mutation requires explicit operator and
  account/region/capacity gates.

## Current Phase: Qualify Forecast Load

The live `/proxy/**` data plane and production-only artifact are built. The project is now qualifying an exact release
for forecast traffic. Work in this phase is load-balancer engineering and deployment proof:

1. Lock the expected request rate, concurrency, route and payload mix, upstream behavior, failure cases, and objectives.
2. Run a repeatable capacity staircase on deployment-equivalent resources and locate the first saturation point.
3. Prove TLS, authentication, target policy, observability, reload, drain, restart, and certificate rotation in a
   reviewed staging boundary.
4. Choose single-active or multiple-active topology and verify its failover, configuration, connection, and state
   behavior.
5. Promote an immutable image through controlled traffic steps with tested abort and rollback thresholds.

[`LOAD_BALANCER_BUILD_OUT.md`](docs/LOAD_BALANCER_BUILD_OUT.md) is the execution plan and readiness gate for this phase.
The capacity, staging, and active-active runners now exist. The next engineering action is to run the exact release and
reviewed forecast profile on deployment-equivalent resources, then replace the loopback ingress fixture with the
reviewed deployment ingress for immutable-image rollout and rollback. Local results establish regression and topology
mechanics only; they do not establish deployment capacity or production-ingress behavior. Lab, demo, and general
documentation work are supporting scope only when they directly enable load qualification, deployment, or operation.

Authentication, TLS verification, secret handling, bounded concurrency, dependency/image scanning, and protected
merge gates remain mandatory while this work advances.

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

This default JAR is the production proxy artifact. It serves `/proxy-status.html` and omits lab, CLI, demo, GUI, Decision Explorer/replay/evidence-training services, `ServerMonitor`, and lab-only dependencies. Build simulation and CLI tooling explicitly:

```bash
mvn -B -P lab -DskipTests package
java -jar "$(bash scripts/resolve-executable-jar.sh --lab)" --lase-demo=healthy
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

`cloud-sandbox` and `proxy-demo-*` are source/Lab Tools profiles and are not packaged in the default production JAR.

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
| `loadbalancerpro.proxy.retry.budget-percent` | `20` | Process-local retry credits per 100 admitted primary requests |
| `loadbalancerpro.proxy.retry.backoff.base` | `50ms` | Initial full-jitter exponential backoff ceiling |
| `loadbalancerpro.proxy.retry.backoff.max` | `1s` | Maximum retry backoff ceiling |
| `loadbalancerpro.proxy.retry.retry-non-idempotent` | `false` | No default non-idempotent retry |
| `loadbalancerpro.proxy.cooldown.enabled` | `false` | Opt-in backend cooldown |
| `loadbalancerpro.proxy.slow-start.duration` | `0s` | Opt-in linear effective-weight ramp for new/recovered upstreams |
| `management.prometheus.metrics.export.enabled` | `false` | No default Prometheus export |
| `management.otlp.metrics.export.enabled` | `false` | No default OTLP export |
| `management.endpoints.web.exposure.include` | `health,info` | Minimal Actuator exposure |

In the source/Lab Tools API runtime, `GET /api/health` and unauthenticated `OPTIONS` are the public API exceptions. Other `/api/**` routes, `/proxy/**`, OpenAPI, and Swagger require the configured key. The production artifact uses `/actuator/health` for health checks. Use:

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

The production proxy exposes forwarding under `/proxy/**`, read-only state at `GET /api/proxy/status`, the newest 100 process-local forwarding decisions at `GET /api/proxy/decisions/recent`, read-only analysis for a retained attempt at `GET /api/proxy/decisions/{decisionId}/explain`, and guarded configuration reload at `POST /api/proxy/reload`. Named routes support exact host/header predicates and deterministic percentage split groups in addition to path prefixes. The explanation uses score and factor evidence captured with the actual selection; it does not rerun a stateful, sampled, keyed, positional, or affinity strategy later. Opt-in LASE shadow evaluation and `GET /api/lase/shadow` remain available only in the source/Lab Tools runtime. Operator configuration examples are under [`docs/examples/proxy`](docs/examples/proxy).

Use these as the forecast-load qualification and proxy-operation path:

- [`LOAD_BALANCER_BUILD_OUT.md`](docs/LOAD_BALANCER_BUILD_OUT.md)
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
| `GET /api/proxy/decisions/recent` | Bounded process-local records of actual upstream forwarding attempts |
| `GET /api/proxy/decisions/{decisionId}/explain` | Dominant-factor, delta, counterfactual, and score-tie analysis from one retained actual forwarding attempt |
| `GET /api/lase/shadow` | Bounded process-local allocation and opt-in live-proxy shadow observations plus dispatch counters |
| `POST /api/proxy/reload` | Guarded proxy configuration reload |
| `/actuator/health` | Spring Boot health and readiness |

Request and response contracts live in [`API_CONTRACTS.md`](docs/API_CONTRACTS.md). Generated OpenAPI is available at `/v3/api-docs` when permitted by the selected auth mode.

## Build and verification

Run tests, build the executable artifact, or generate the JaCoCo verification report:

```bash
mvn -B test
mvn -B package
mvn -B verify
```

Run the bounded local proxy regression scenarios (Docker Compose, Bash, `curl`, `jq`, OpenSSL, and Vegeta required):

```bash
bash scripts/bench/proxy-benchmark-soak.sh --mode smoke
```

The smoke and scheduled one-hour soak gates use only the TLS-authenticated loopback Compose stack and ignored
`target/bench/` output. See [`scripts/bench/README.md`](scripts/bench/README.md) for scenarios, thresholds, and the
evidence boundary; local results do not establish production SLOs, capacity, public-ingress safety, or p95/p99
guarantees.

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
curl -fsS http://127.0.0.1:8080/actuator/health
```

The Docker image runs as a non-root user and defaults to the `prod` profile. Keep the host port loopback-bound for local checks. See [`CONTAINER_DEPLOYMENT.md`](docs/CONTAINER_DEPLOYMENT.md) for networking and TLS boundaries.

## Forecast-load workflow

1. Record the workload contract and acceptance thresholds from the build-out.
2. Build and verify the exact production artifact with its SBOM and image scans.
3. Supply TLS, authentication, trust, identity, upstream, and monitoring configuration through deployment secret and
   configuration management.
4. Pass the loopback regression smoke and one-hour soak without weakening verification or safety limits.
5. Run the capacity staircase on deployment-equivalent resources and select an operating envelope with agreed
   headroom below saturation.
6. Repeat the forecast mix and failure cases in the reviewed staging boundary.
7. Prove the selected single-active or multiple-active topology, including drain, replacement, and configuration
   consistency behavior.
8. Roll out an immutable image in controlled traffic steps; abort and restore the prior image/configuration when an
   agreed threshold is exceeded.

For the packaged-application proof path use [`DEPLOYMENT_SMOKE_KIT.md`](docs/DEPLOYMENT_SMOKE_KIT.md). For controlled lab tooling use [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](docs/LOCAL_LAB_MANUAL_TOOLING_INDEX.md).

## Troubleshooting

- **Startup says the API key is missing:** set `LOADBALANCERPRO_API_KEY` for `prod`/default API-key mode, or explicitly use `local` only on loopback.
- **The executable JAR cannot be found:** run `mvn -B package`, then use the resolver script; do not hard-code an artifact filename.
- **A protected route returns 401:** include `X-API-Key` or configure the intended OAuth2 issuer, JWK source, and roles.
- **Proxy status says disabled:** enable proxy mode only through a reviewed profile and provide valid routes/upstreams.
- **An upstream is skipped or retry is suppressed:** inspect health, retry-budget counters, cooldown, slow-start weight, timeout, and route status in `/api/proxy/status` and application logs.
- **OTLP startup validation fails:** use a trusted private/internal collector URL without credentials, query parameters, or fragments.
- **Docker cannot reach a host backend:** `127.0.0.1` inside a container is the container; use an explicit Docker network or platform host gateway.
- **A port is already in use:** select another `server.port` and keep the bind address explicit.
- **Local Maven trust errors occur:** repair the workstation/JDK trust store; do not disable TLS verification.

## Load-balancer deployment references

- [`LOAD_BALANCER_BUILD_OUT.md`](docs/LOAD_BALANCER_BUILD_OUT.md)
- [`DEPLOYMENT.md`](docs/DEPLOYMENT.md)
- [`REVERSE_PROXY_MODE.md`](docs/REVERSE_PROXY_MODE.md)
- [`SECURITY.md`](SECURITY.md)
- [`API_SECURITY.md`](docs/API_SECURITY.md)
- [`DEPLOYMENT_HARDENING_GUIDE.md`](docs/DEPLOYMENT_HARDENING_GUIDE.md)
- [`OPERATIONS_RUNBOOK.md`](docs/OPERATIONS_RUNBOOK.md)
