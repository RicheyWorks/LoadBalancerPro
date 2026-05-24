# Local Lab Docker Compose App Service Runbook

This documentation-only reviewer runbook/refinement ties together the existing local-lab Compose app-service skeleton, the app-service manual smoke checklist, and the app-service health/readiness documentation lane. It adds no Compose behavior changes, no app behavior changes, and no endpoint changes.

The existing `app-under-test` service is documented in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md), manually checked through [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md), and bounded for manual/local health/readiness observations in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md). The broader Compose runbook remains [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).

## Current Local-Lab State

- `app-under-test` already exists in local-lab Compose.
- The service is optional/manual/local-lab-only.
- The service uses the local `target/` directory read-only.
- The service requires a manual Maven package step before optional Compose use.
- The app published port is `127.0.0.1:8080:8080`.
- Toxiproxy remains present.
- k6 remains manual and separate.
- Bruno remains manual and separate.
- no k6 runner service exists.
- no Bruno runner service exists.
- no CI-gating.
- no Maven wiring.
- no Dockerfile change.
- no production Docker packaging.
- no production Compose change.
- no production runtime behavior change.
- health/readiness observations are manual/local only.
- health/readiness observations do not prove production readiness.
- no production readiness/certification claim.
- no live-cloud or real-tenant validation claim.
- no runtime enforcement claim.
- no replay/evidence/report/storage/export behavior claim.
- no load/stress/benchmark claim.
- no throughput/p95/p99 evidence claim.

## Inspection-Only Path

This path requires no Docker execution, no Compose execution, no server startup, no network call, no tool execution, and no local application run.

1. Inspect [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml).
2. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md).
3. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md).
4. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md).
5. Confirm `app-under-test` uses the local `target/` directory read-only.
6. Confirm the app published port is `127.0.0.1:8080:8080`.
7. Confirm no `0.0.0.0`.
8. Confirm no secrets or credentials.
9. Confirm no external, cloud, tenant, production, or private-network target.
10. Confirm no k6 runner service.
11. Confirm no Bruno runner service.
12. Confirm no CI/Maven wiring.

## Optional Manual Package-First Path

Use this only if a reviewer chooses to run the existing local-lab app service manually. It is optional, manual-only, and local-lab-only.

```powershell
mvn -q "-DskipTests" package
```

This command is a manual prerequisite only. It is not CI-gating. It is not Maven wiring. It is not a production packaging change. It does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, platform implementation, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, p95 evidence, or p99 evidence.

## Optional Manual Compose Config Path

Inspect the rendered local-lab Compose configuration:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml config
```

Before proceeding, confirm all published ports remain `127.0.0.1` only and confirm no production, cloud, tenant, private-network, external endpoint, secret, credential, k6 runner service, or Bruno runner service appears.

## Optional Manual App-Service Smoke Path

Optionally start the existing local-lab services by hand:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml up
```

This is not CI. It is not Maven lifecycle wiring. It is not automated execution. It is not production Docker packaging. It is not production Compose. It is not production runtime behavior.

## Optional Manual Health/Readiness Observation Path

Use [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md) for the bounded health/readiness observation lane. That lane is manual/local only and adds no health endpoint, no readiness endpoint, no app behavior change, no Compose behavior change, and no endpoint change.

## What Reviewers Can And Cannot Infer

Reviewers may infer only that a local workstation observation happened against the existing local-lab app service under loopback/local constraints. Reviewers cannot infer production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, platform implementation, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, p95 evidence, or p99 evidence.

## Shutdown And Cleanup Path

Stop the optional local-lab Compose services:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml down
```

Do not add cleanup automation to CI, Maven, scripts, production Docker packaging, production Compose, or app runtime behavior in this lane.

## Troubleshooting Path

- Docker not installed: use the inspection-only path.
- `target/` is empty: run the manual package command only if choosing the optional manual path.
- App port already in use: stop and keep inspection-only; do not widen the bind.
- Target is not loopback: stop; do not use production, cloud, tenant, external, or private-network endpoints.
- Health/readiness observation is desired: use the health/readiness doc, keep it manual/local only, and do not add endpoints.
- k6 or Bruno automation is desired: stop; runner services remain behind future gates.
- Toxiproxy expansion is desired: stop; future Toxiproxy expansion remains separately gated.

## Relationship To Future Gates

- Future Compose changes remain behind [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).
- Future app-service expansion remains behind [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md).
- k6 runner services remain behind future gates.
- Bruno runner services remain behind future gates.
- Expanded Toxiproxy behavior remains behind future gates.

## Remaining Not-Proven Boundaries

The following remain not proven:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime enforcement;
- broader Docker/k6/Bruno/Toxiproxy platform implementation beyond optional local-lab skeletons;
- replay execution;
- evidence/report generation;
- storage/export behavior;
- load testing;
- stress testing;
- benchmarking;
- throughput evidence;
- p95 evidence;
- p99 evidence;
- autonomous production traffic shifting;
- carbon-aware routing;
- GPU orchestration;
- power/grid control;
- facility automation;
- broader automation.

## Runner-Service Gate Update

The runner-service gate is now available at [`LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md). It is documentation only and a future gate for runner services. It adds no k6 runner service, no Bruno runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, and no production Compose change.

k6 remains manual and separate. Bruno remains manual and separate. The app-service runbook remains the reviewer path. Any future k6 runner PR must be separately scoped. Any future Bruno runner PR must be separately scoped. Future runner services must stay local-lab-only and loopback/local-targeted, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.

## k6 Runner Service Design Gate Update

The k6 runner service design gate is now available at [`LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md). It is documentation only and a future design gate for a k6 Compose runner service. It adds no k6 runner service, no Bruno runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no automated execution.

k6 remains manual and separate. Bruno remains manual and separate. The app-service runbook remains reviewer path. Any future k6 runner PR must be separately scoped, must stay local-lab-only, must target only loopback/local services, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.
