# Local Lab Docker Compose App Service Skeleton

This page documents the first gated, optional local-lab app service skeleton in [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml). The preflight result is pass: the skeleton uses the existing Compose readiness gate in [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md) and the app-service preflight checklist in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md) before adding the smallest app-under-test service.

The skeleton is optional. It is manual-only. It is local-lab-only. It is not CI-gated. It is not wired into Maven. It does not change production runtime behavior. It is not production Docker packaging. It does not add a Dockerfile. It does not build an image. It does not publish an image. It does not add k6 runner service behavior. It does not add Bruno runner service behavior. It does not add automated execution.

## What Changed

- The Compose file still contains the existing `toxiproxy` service.
- The Compose file now contains one local-lab-only `app-under-test` service.
- The app service uses the public `eclipse-temurin:21-jre` JRE image.
- The app service mounts the local `target/` directory read-only at `/opt/loadbalancerpro`.
- The app service starts `/opt/loadbalancerpro/LoadBalancerPro-2.5.0.jar`.
- The app service publishes `127.0.0.1:8080:8080` only.
- The existing Toxiproxy service remains intact.

The user must manually package first before choosing to run the app service:

```powershell
mvn -q "-DskipTests" package
```

That command is a manual prerequisite only. It is not added to Compose, Maven lifecycle automation, CI, scripts, or production packaging.

## Optional Manual Use

Inspection remains the preferred reviewer path. A reviewer may inspect [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml), this page, and the readiness gate without running Docker.

If a reviewer deliberately chooses to run the app service manually, the path remains local-only:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml up app-under-test
```

To stop the optional local-lab service:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml down
```

These commands are optional manual local-lab commands only. They are not CI steps, not Maven steps, not production deployment steps, and not verification requirements.

## Safety Checklist

- Confirm the Compose file remains under `lab/docker-compose/`.
- Confirm the app service is named `app-under-test` and is local-lab-only.
- Confirm all published ports bind to `127.0.0.1` only.
- Confirm no `0.0.0.0` default exposure.
- Confirm no production, cloud, tenant, private-network, or external targets.
- Confirm no secrets or credentials.
- Confirm no Dockerfile changes.
- Confirm no production Compose changes.
- Confirm no k6 runner service.
- Confirm no Bruno runner service.
- Confirm the Toxiproxy service remains present and unchanged except for shared local-lab Compose coexistence.
- Confirm the packaged JAR exists under `target/` before any manual run.
- Confirm no CI-gating, Maven wiring, scripts, or automated execution were added.

## Relationship To Existing Guardrails

- The Compose skeleton doc now covers the combined local-lab Compose skeleton: [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md).
- The app-service boundary design remains the design basis for this bounded addition: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md).
- The readiness gate remains the checklist for any future Compose change: [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).
- The app-service preflight checklist remains the exact proof reviewers should use before future app-service expansion: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md).
- The Compose manual runbook remains the optional inspection-first operator path: [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).
- The k6 smoke script remains manual and separate: [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md).
- The Bruno collection remains manual and separate: [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md).
- Toxiproxy remains manual/local-only: [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md).
- The end-of-day Compose handoff remains the reviewer progress context: [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).
- The app-service manual smoke checklist gives reviewers an inspection-first optional manual smoke path: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md).

Future app-service expansion, k6 runner services, Bruno automation, expanded Toxiproxy fault execution, CI wiring, Maven wiring, Dockerfile changes, production Compose changes, Docker image publishing, registry push, or production packaging still require separate review.

## What This Does Not Prove

This skeleton does not prove production readiness. It does not prove production certification. It does not prove live-cloud validation. It does not prove real-tenant validation. It does not prove runtime enforcement. It does not perform replay execution, evidence/report generation, storage, or export behavior.

This skeleton is not load testing. It is not stress testing. It is not benchmarking. It provides no throughput evidence. It provides no p95 evidence. It provides no p99 evidence.

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
- p95/p99 evidence;
- autonomous production traffic shifting;
- carbon-aware routing;
- GPU orchestration;
- power/grid control;
- facility automation;
- broader automation.

## Post-App-Service Compose Handoff Update

PR #284 is now the current local-lab Compose baseline. This handoff update is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, or production runtime behavior.

Current state:

- `app-under-test` service now exists in local-lab Compose.
- It is optional/manual/local-lab-only.
- It uses the local `target/` mount read-only and requires a manual package step before optional use.
- The published app port is loopback-bound at `127.0.0.1:8080:8080`.
- The existing Toxiproxy service remains present and loopback/local.
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
- no production readiness/certification claim.
- no live-cloud or real-tenant validation claim.
- no runtime enforcement claim.
- no replay/evidence/report/storage/export behavior claim.
- no load/stress/benchmark claim.
- no throughput/p95/p99 evidence claim.

Next safe expansion lanes:

- app-service manual smoke checklist docs;
- Compose manual runbook update for app service;
- app-service health/readiness documentation only;
- future k6/Bruno runner design docs only;
- no runner services until separate gates are created.

Use [LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md), [LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md), and this app-service skeleton doc before considering any later Compose change.

## App-Service Manual Smoke Checklist Update

The app-service manual smoke checklist is now the next docs/test-only reviewer lane: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md). It does not change `lab/docker-compose/local-lab-compose.yml`, app behavior, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, production Compose, production Docker packaging, or production runtime behavior.

It documents that `app-under-test` already exists in local-lab Compose, remains optional/manual/local-lab-only, uses the local `target/` mount read-only, requires a manual package step before optional Compose use, publishes `127.0.0.1:8080:8080`, keeps Toxiproxy present, keeps k6 manual and separate, keeps Bruno manual and separate, adds no k6 runner service, adds no Bruno runner service, adds no CI-gating, adds no Maven wiring, adds no Dockerfile change, adds no production Docker packaging, adds no production Compose change, and adds no production runtime behavior change.

The manual smoke checklist does not create production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.

## App-Service Health/Readiness Documentation Update

The app-service health/readiness documentation lane is now available at [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md). It is docs/test-only and does not change the local-lab Compose file, the app service, Dockerfiles, Maven, CI, runtime resources, production Compose, production Docker packaging, production runtime behavior, or application endpoints. It adds no health endpoint and no readiness endpoint.

The health/readiness lane documents inspection-only review and optional manual local-only observations for the existing `app-under-test` service. It keeps the service optional/manual/local-lab-only, package-first, read-only mounted from `target/`, and loopback-bound at `127.0.0.1:8080:8080`; it keeps Toxiproxy present, k6 manual and separate, Bruno manual and separate, no runner services, no CI-gating, and no Maven wiring.

The lane does not create production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.
