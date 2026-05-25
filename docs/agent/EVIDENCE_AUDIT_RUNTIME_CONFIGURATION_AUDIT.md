# Evidence Audit Runtime Configuration Audit

This note is slot 9 of the **LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign**. It is documentation/test-only. It audits runtime configuration source files without changing runtime resources, application behavior, endpoints, Maven, CI, Dockerfile, Compose behavior, scripts, k6, Bruno, Toxiproxy, runner services, automation, secrets, external targets, or production behavior.

## Audit Timestamp

- Audit timestamp: 2026-05-25T03:44-07:00.
- Audited repository: `RicheyWorks/LoadBalancerPro`.
- Audited base branch: `main`.
- Slot 9 branch: `codex/evidence-audit-runtime-config`.
- Starting main HEAD: `0fc6a5431f400eb4e5f71a70805b3fcb317f1c69`.
- Prior slot fact: PR #323 merged as `0fc6a5431f400eb4e5f71a70805b3fcb317f1c69`; post-merge main CI and CodeQL were green before slot 9 started.

## Scope

This audit is an inspection-only reviewer record for `src/main/resources/application.properties`, `src/main/resources/application-prod.properties`, and adjacent source-visible runtime configuration references. It does not edit any runtime configuration file, does not start the app, does not call endpoints, and does not turn configuration observations into production readiness proof.

## Default Local Configuration

The default `application.properties` is local/developer-oriented:

- Actuator exposure includes `health,info,metrics,prometheus`.
- Prometheus metrics export is enabled with `management.prometheus.metrics.export.enabled=true`.
- OTLP metrics export is disabled with `management.otlp.metrics.export.enabled=false`.
- OTLP metrics URL is read from `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` and defaults to blank.
- Health probes are enabled and health details are `when_authorized`.
- Management metrics are tagged with `environment=local`.
- The app version is `2.5.0`.
- The default auth mode is `loadbalancerpro.auth.mode=api-key`, while local/default security remains developer-friendly unless a production-like profile activates the prod/cloud-sandbox API-key filter or OAuth2 mode is explicitly configured.
- Local CORS allows `http://localhost:3000` and `http://localhost:8080` with credentials disabled by `WebConfig`.
- The process-local API rate limiter is disabled by default with `loadbalancerpro.api.rate-limit.enabled=false`.
- LASE shadow mode is disabled by default with `loadbalancerpro.lase.shadow.enabled=false`.
- Reverse proxy mode is disabled by default with `loadbalancerpro.proxy.enabled=false`.
- Proxy private-network validation and private-network live validation are disabled by default, and live validation also requires explicit operator approval when separately enabled.
- Proxy active health checks, retries, and cooldown are disabled by default.

Reviewer interpretation: the default profile is useful for local development, CI smoke paths, and lab review. It should not be exposed as a public or shared-network deployment default.

## Prod Profile Narrowing

The `application-prod.properties` profile narrows several surfaces:

- Actuator exposure is limited to `health,info`.
- Prometheus metrics export is disabled with `management.prometheus.metrics.export.enabled=false`.
- OTLP metrics export remains opt-in through `LOADBALANCERPRO_OTLP_METRICS_ENABLED`.
- OTLP metrics URL still comes from `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` and defaults to blank.
- Metrics environment is tagged `prod`.
- CORS origins are read from `LOADBALANCERPRO_CORS_ALLOWED_ORIGINS` and default to blank.
- `loadbalancerpro.api.key` is read from `LOADBALANCERPRO_API_KEY` and defaults to blank.
- LASE shadow mode remains disabled.
- `cloud.liveMode=false`.
- Forwarded-header handling is documented as an operator opt-in comment rather than enabled by default.

Reviewer interpretation: the prod profile is a production-like starting point, not production readiness. It narrows observability exposure and uses operator-supplied runtime configuration, but it does not prove secret management, ingress controls, TLS, identity lifecycle, monitoring, incident response, or production deployment governance.

## API Auth Mode And API Key Default Behavior Question

The runtime source declares `loadbalancerpro.auth.mode=api-key` by default. In `prod` and `cloud-sandbox` profiles, `ProdApiKeyFilter` protects `/api/**` except `GET /api/health` and unauthenticated `OPTIONS`, protects `/proxy/**`, and protects OpenAPI/Swagger routes when API-key mode is active.

The prod profile maps `loadbalancerpro.api.key=${LOADBALANCERPRO_API_KEY:}`. If the environment variable is missing or blank, protected requests fail closed with HTTP 401. That is an intentional safer failure mode, but it remains an operator configuration question before any shared-network or production-like use: reviewers should confirm how the key is supplied, rotated, logged, and protected by deployment secret management. This audit does not supply or validate a real secret.

OAuth2 mode is separately configurable through `loadbalancerpro.auth.mode=oauth2` plus issuer or JWK settings. This audit does not configure OAuth2, call an IdP, validate a real tenant, or prove enterprise identity integration.

## Telemetry And Actuator Boundary

The default/local profile exposes Actuator metrics and Prometheus for local review. The prod profile exposes only health/info and disables Prometheus by default. OTLP export is disabled unless explicitly enabled, and the startup guard validates the configured OTLP metrics endpoint shape before accepting enabled export.

The startup guard is a configuration safety check. It does not contact a collector, validate TLS, prove collector security, provide production telemetry, or replace deployment-specific network, IAM, firewall, or egress policy.

## CORS Boundary

The default profile allows local browser origins only: `http://localhost:3000` and `http://localhost:8080`. The prod profile defaults configured CORS origins to blank, and `WebConfig` treats blank origin lists as empty. Credentials remain disabled for `/api/**` and `/proxy/**`.

This audit does not prove public browser deployment safety, production CORS review, or edge authentication. Reviewers should require explicit deployment-specific CORS decisions before shared-network exposure.

## Rate Limit, LASE, Proxy, And Private-Network Defaults

The inspected default configuration keeps these optional behaviors disabled unless separately configured:

- `loadbalancerpro.api.rate-limit.enabled=false`.
- `loadbalancerpro.lase.shadow.enabled=false`.
- `loadbalancerpro.proxy.enabled=false`.
- `loadbalancerpro.proxy.private-network-validation.enabled=false`.
- `loadbalancerpro.proxy.private-network-live-validation.enabled=false`.
- `loadbalancerpro.proxy.private-network-live-validation.operator-approved=false`.
- `loadbalancerpro.proxy.health-check.enabled=false`.
- `loadbalancerpro.proxy.retry.enabled=false`.
- `loadbalancerpro.proxy.cooldown.enabled=false`.

Reviewer interpretation: these defaults keep optional runtime features off by default. They do not prove runtime enforcement, production gateway readiness, private-network live validation, traffic shifting, load/stress testing, or benchmark evidence.

## Reviewer Questions

- Does the default profile remain clearly local/developer-oriented?
- Does the prod profile still narrow Actuator exposure to `health,info`?
- Is Prometheus disabled in prod unless separately enabled elsewhere?
- Does OTLP remain opt-in and source-visible through runtime environment variables?
- Does prod API-key mode fail protected requests closed when `LOADBALANCERPRO_API_KEY` is missing or blank?
- Are real secrets absent from source-controlled properties?
- Are prod CORS origins empty unless explicitly configured?
- Are rate limiting, LASE shadow mode, proxy mode, private-network validation, private-network live validation, health checks, retries, and cooldown disabled by default?
- Is `cloud.liveMode=false` preserved in the prod profile?
- Has any future PR changed runtime resources or application behavior under the cover of a docs-only audit?

## Remaining Limits

This audit is static and source-visible only. It does not run the application, inspect a deployed environment, call APIs, validate a real IdP, validate a real secret manager, exercise TLS, prove edge controls, inspect production telemetry, or validate production monitoring.

This audit does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, registry publication, container signing, production telemetry, production monitoring, or broader automation.

## Navigation

- Repository evidence map: [`EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md`](EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md).
- Campaign board: [`EVIDENCE_AUDIT_CAMPAIGN_BOARD.md`](EVIDENCE_AUDIT_CAMPAIGN_BOARD.md).
- CI workflow audit: [`EVIDENCE_AUDIT_CI_WORKFLOW_AUDIT.md`](EVIDENCE_AUDIT_CI_WORKFLOW_AUDIT.md).
- CodeQL and dependency-review audit: [`EVIDENCE_AUDIT_CODEQL_DEPENDENCY_REVIEW_AUDIT.md`](EVIDENCE_AUDIT_CODEQL_DEPENDENCY_REVIEW_AUDIT.md).
- Dockerfile runtime audit: [`EVIDENCE_AUDIT_DOCKERFILE_RUNTIME_AUDIT.md`](EVIDENCE_AUDIT_DOCKERFILE_RUNTIME_AUDIT.md).
- Compose/local-lab audit: [`EVIDENCE_AUDIT_COMPOSE_LOCAL_LAB_AUDIT.md`](EVIDENCE_AUDIT_COMPOSE_LOCAL_LAB_AUDIT.md).
- API security posture: [`../API_SECURITY.md`](../API_SECURITY.md).
- Container deployment guide: [`../CONTAINER_DEPLOYMENT.md`](../CONTAINER_DEPLOYMENT.md).
