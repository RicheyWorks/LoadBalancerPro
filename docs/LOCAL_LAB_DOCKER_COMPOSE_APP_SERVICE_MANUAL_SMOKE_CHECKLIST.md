# Local Lab Docker Compose App Service Manual Smoke Checklist

This page is a manual smoke checklist only for the existing `app-under-test` service in [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml). It is optional. It is manual-only. It is local-lab-only. It does not change Compose behavior, app behavior, Dockerfiles, Maven, CI, runtime behavior, k6, Bruno, Toxiproxy, production Docker packaging, or production Compose.

The app-under-test service already exists in local-lab Compose. It uses the public JRE image documented in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md), mounts the local `target/` directory read-only, requires a manual Maven package step before optional use, and publishes `127.0.0.1:8080:8080`. Toxiproxy remains present. k6 remains manual and separate. Bruno remains manual and separate. No k6 runner service exists. No Bruno runner service exists.

This checklist complements the Compose manual runbook in [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md), the app-service preflight checklist in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md), and the Compose readiness gate in [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).

## Purpose

- Provide a manual smoke checklist only for reviewers who choose to inspect or manually run the existing local-lab app service.
- Keep the path optional/manual/local-lab-only.
- Keep the manual Maven package step explicit before running Compose.
- Keep the app published port loopback-bound at `127.0.0.1:8080:8080`.
- Preserve that this is not CI-gating, not Maven wiring, not a Dockerfile change, not production Docker packaging, not production Compose, and not production runtime behavior.

## Inspection-Only Path

This path requires no Docker execution, no Compose execution, no server startup, no network call, no tool execution, and no local application run.

1. Inspect [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml).
2. Confirm `app-under-test` exists in local-lab Compose.
3. Confirm the app service mounts the local `target/` directory read-only.
4. Confirm the app published port is `127.0.0.1:8080:8080`.
5. Confirm `app-under-test` is loopback-bound.
6. Confirm no `0.0.0.0`.
7. Confirm no secrets.
8. Confirm no external, cloud, tenant, or production target.
9. Confirm Toxiproxy remains present.
10. Confirm no k6 runner service.
11. Confirm no Bruno runner service.
12. Confirm no CI/Maven wiring.
13. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md).
14. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).
15. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).

## Optional Manual Smoke Path

These commands are optional and manual. They are local-lab-only. They are not part of CI. They are not part of Maven lifecycle wiring. They are not required for verification. They do not prove production readiness. They do not prove production certification. They do not prove live-cloud validation. They do not prove real-tenant validation. They do not prove runtime enforcement. They do not prove platform implementation. They do not prove replay/evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, p95 evidence, or p99 evidence.

Manually package first if a reviewer chooses to run the app service:

```powershell
mvn -q "-DskipTests" package
```

Inspect the rendered Compose configuration:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml config
```

Optionally start the local-lab Compose services by hand:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml up
```

Stop the optional local-lab Compose services:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml down
```

Before running any optional command, confirm all targets remain `127.0.0.1` or localhost only. Do not use external, cloud, tenant, private-network, or production endpoints.

## Safety Checklist

- Confirm this is a manual smoke checklist only.
- Confirm the path remains optional/manual/local-lab-only.
- Confirm manual Maven package is required before running Compose.
- Confirm app-under-test already exists in local-lab Compose.
- Confirm app-under-test uses the local `target/` mount read-only.
- Confirm the app published port is `127.0.0.1:8080:8080`.
- Confirm Toxiproxy remains present.
- Confirm k6 remains manual and separate.
- Confirm Bruno remains manual and separate.
- Confirm no k6 runner service.
- Confirm no Bruno runner service.
- Confirm no CI-gating.
- Confirm no Maven wiring.
- Confirm no Dockerfile change.
- Confirm no production Docker packaging.
- Confirm no production Compose change.
- Confirm no production runtime behavior change.
- Confirm no production readiness/certification claim.
- Confirm no live-cloud or real-tenant validation claim.
- Confirm no runtime enforcement claim.
- Confirm no replay/evidence/report/storage/export behavior claim.
- Confirm no load/stress/benchmark claim.
- Confirm no throughput/p95/p99 evidence claim.

## Troubleshooting Boundaries

- Docker not installed: use the inspection-only path.
- `target/` is empty: run the manual package command only if choosing the optional manual path.
- Port already in use: stop and keep the checklist inspection-only; do not widen the bind.
- Target is not loopback: stop; do not use production, cloud, tenant, private-network, or external endpoints.
- k6 or Bruno automation is desired: stop; those runner services require separate future design/gate work.
- A result looks useful: do not treat it as performance proof, readiness proof, runtime enforcement proof, or evidence/report/storage/export behavior.

## Relationship To Existing Guardrails

- The app-service skeleton documents the existing local-lab app service: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md).
- The Compose manual runbook remains the broader inspection-first operator path: [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).
- The app-service preflight checklist remains the gate before any future app-service expansion: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md).
- The Compose readiness gate remains required before future Compose changes: [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).
- The post-app-service handoff keeps the current state and next lanes visible: [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

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

## App-Service Health/Readiness Documentation Update

The app-service health/readiness documentation lane is now available at [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md). This is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, app behavior, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, production Compose, production Docker packaging, production runtime behavior, or application endpoints. It adds no health endpoint and no readiness endpoint.

The lane keeps health/readiness observations optional, manual, and local-only for the existing `app-under-test` service. It preserves the manual package step, the read-only local `target/` mount, the `127.0.0.1:8080:8080` app port, Toxiproxy presence, k6 and Bruno as manual separate tools, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no production runtime behavior change.

The lane does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.

## App-Service Runbook Update

The app-service runbook is now available at [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md). It is documentation only and acts as a reviewer runbook/refinement that ties together the app-service skeleton, this manual smoke checklist, and the health/readiness lane.

The runbook adds no Compose behavior changes, no app behavior changes, no endpoint changes, no health endpoint, no readiness endpoint, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no production runtime behavior change.

It preserves optional/manual/local-lab-only scope, manual package-first operation, read-only `target/` mounting, `127.0.0.1:8080:8080`, Toxiproxy presence, k6 and Bruno as manual separate tools, no k6 runner service, no Bruno runner service, and manual/local-only health/readiness observations. It does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.

## Runner-Service Gate Update

The runner-service gate is now available at [`LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md). It is documentation only and a future gate for runner services. It adds no k6 runner service, no Bruno runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, and no production Compose change.

k6 remains manual and separate. Bruno remains manual and separate. The app-service runbook remains the reviewer path. Any future k6 runner PR must be separately scoped. Any future Bruno runner PR must be separately scoped. Future runner services must stay local-lab-only and loopback/local-targeted, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.

## k6 Runner Service Design Gate Update

The k6 runner service design gate is now available at [`LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md). It is documentation only and a future design gate for a k6 Compose runner service. It adds no k6 runner service, no Bruno runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no automated execution.

k6 remains manual and separate. Bruno remains manual and separate. The app-service runbook remains reviewer path. Any future k6 runner PR must be separately scoped, must stay local-lab-only, must target only loopback/local services, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.

## Bruno Runner Service Design Gate Update

The Bruno runner service design gate is now available at [`LOCAL_LAB_DOCKER_COMPOSE_BRUNO_RUNNER_SERVICE_DESIGN_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_BRUNO_RUNNER_SERVICE_DESIGN_GATE.md). It is documentation only and a future design gate for a Bruno Compose runner service. It adds no Bruno runner service, no k6 runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no automated execution.

Bruno remains manual and separate. k6 remains manual and separate. The app-service runbook remains reviewer path. Any future Bruno runner PR must be separately scoped, must stay local-lab-only, must target only loopback/local services, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply production API validation, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.
