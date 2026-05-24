# Local Lab Docker Compose App Service Health Readiness

This docs/test-only lane describes how reviewers may inspect and, if they choose, manually observe the existing local-lab Compose `app-under-test` service. It adds no health endpoint. It adds no readiness endpoint. It changes no app behavior. It changes no Compose behavior. It does not change Dockerfiles, Maven, CI, runtime resources, production Docker packaging, production Compose, or production runtime behavior.

The current local-lab app service already exists in [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml). It is optional/manual/local-lab-only, uses the local `target/` directory read-only, requires a manual Maven package step before optional Compose use, and publishes the app port as `127.0.0.1:8080:8080`.

This page complements [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md), [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md), [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md), [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md), and [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).

## Purpose

- Document a manual/local-only health/readiness observation lane for the existing local-lab app service.
- Keep the lane documentation only.
- Preserve that no health endpoint is added.
- Preserve that no readiness endpoint is added.
- Preserve that no app behavior changes and no Compose behavior changes.
- Preserve that health/readiness observations are manual/local only and do not prove production readiness.

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

## Inspection-Only Path

This path requires no Docker execution, no Compose execution, no server startup, no tool execution, no network call, and no environment dependency.

1. Inspect [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml).
2. Confirm `app-under-test` is loopback-bound at `127.0.0.1:8080:8080`.
3. Confirm no `0.0.0.0`.
4. Confirm no secrets or credentials.
5. Confirm no external, cloud, tenant, production, or private-network target.
6. Confirm no k6 runner service.
7. Confirm no Bruno runner service.
8. Confirm no CI/Maven wiring.
9. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md).
10. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md).
11. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).
12. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md).
13. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).

## Optional Local-Only Manual Checks

These examples are optional manual local-only checks. They are not CI steps. They are not Maven lifecycle wiring. They are not required verification. They do not prove production readiness, production certification, runtime enforcement, broader platform implementation, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, p95 evidence, or p99 evidence.

Package manually first if choosing to run the app service:

```powershell
mvn -q "-DskipTests" package
```

Inspect rendered local Compose configuration:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml config
```

Optionally start only the existing local-lab app service:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml up app-under-test
```

Optionally observe the already-documented local health endpoint:

```powershell
curl -fsS http://127.0.0.1:8080/api/health
```

`/api/health` is referenced here only because the repository README already documents `GET /api/health`. This page does not invent a new endpoint, does not add a health endpoint, does not add a readiness endpoint, and does not define a new readiness contract.

Stop the optional local-lab service:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml down
```

## What Reviewers Can Infer

- The Compose file can be inspected for loopback/local-only app exposure.
- A manual local response from `http://127.0.0.1:8080/api/health` can show that the locally packaged app responded on loopback in that workstation context.
- The observation is manual/local only.
- The observation does not prove production readiness.
- The observation does not prove production certification.
- The observation does not prove live-cloud validation.
- The observation does not prove real-tenant validation.
- The observation does not prove runtime enforcement.
- The observation does not prove replay execution, evidence/report generation, storage, or export behavior.
- The observation does not prove load/stress/benchmark behavior, throughput evidence, p95 evidence, or p99 evidence.

## Troubleshooting

- Docker not installed: use the inspection-only path; do not add CI, Maven, scripts, or automation.
- `target/` is empty: run the manual package command only if choosing the optional manual path.
- App port already in use: stop and keep inspection-only; do not widen the bind.
- Target is not loopback: stop; do not use production, cloud, tenant, external, or private-network endpoints.
- `/api/health` does not respond locally: stop and keep inspection-only; do not add a new endpoint or change app behavior in this lane.
- k6 or Bruno automation is desired: stop; runner services require separate future design and gates.

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

## App-Service Runbook Update

The app-service runbook is now available at [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md). It is documentation only and acts as a reviewer runbook/refinement that ties together this health/readiness lane, the app-service skeleton, and the manual smoke checklist.

The runbook adds no Compose behavior changes, no app behavior changes, no endpoint changes, no health endpoint, no readiness endpoint, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no production runtime behavior change.

It preserves optional/manual/local-lab-only scope, manual package-first operation, read-only `target/` mounting, `127.0.0.1:8080:8080`, Toxiproxy presence, k6 and Bruno as manual separate tools, no k6 runner service, no Bruno runner service, and manual/local-only health/readiness observations. It does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.
