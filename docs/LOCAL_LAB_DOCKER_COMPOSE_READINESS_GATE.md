# Local Lab Docker Compose Readiness Gate

This page is a readiness gate for local-lab Docker Compose changes. It defines the checklist a PR must satisfy before changing [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml), adding or expanding an app service, adding runner services, or wiring Compose into CI or Maven.

The gated app-service skeleton PR applies this readiness gate before adding one small local-lab-only app-under-test service. It does not add k6 runner services. No k6 runner service added. It does not add Bruno runner services. No Bruno runner service added. It does not add CI-gating. It does not add Maven wiring. It does not change production runtime behavior.

The current Compose skeleton includes the existing Toxiproxy service plus the gated app-under-test service. The k6 smoke script remains manual and separate. The Bruno collection remains manual and separate. Toxiproxy remains manual/local-only. This readiness gate complements [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md), [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md), [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md), [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md), [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md), [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md), and the app-service preflight checklist in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md).

The end-of-day Compose handoff in [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md) summarizes today's merged local-lab tooling and Compose guardrail chain. It does not change this readiness gate, does not change Compose behavior, does not add an app service, and does not add CI/Maven wiring.

## Purpose

- Define a local-lab-only readiness gate for future Compose file changes.
- Keep each Compose change separately scoped.
- Keep the app service optional, manual-only, local-lab-only, loopback-bound, and read-only mounted.
- Keep k6 runner service, Bruno runner service, and broader new Compose services out of this PR.
- Keep CI-gating, Maven wiring, automated execution, production Docker packaging, Docker image publishing, registry login/push, production runtime behavior, and production Compose profiles out of this PR.

## Gate For Any Future Compose File Change

A future PR touching [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml) must prove:

- changed services remain local-lab-only;
- all published ports bind to `127.0.0.1` only;
- defaults remain `127.0.0.1 / loopback-only`;
- no `0.0.0.0` default exposure;
- no production/cloud/tenant/external endpoints;
- no cloud/private network/tenant targets;
- no secrets or credentials;
- no secrets/credentials;
- no production Docker packaging impact;
- no CI/Maven wiring unless explicitly scoped;
- no CI-gating unless explicitly scoped and reviewed;
- no benchmark/load/stress/performance evidence claims;
- no throughput evidence;
- no p95/p99 evidence;
- no load/stress/benchmark evidence;
- no production readiness/certification claims;
- no runtime enforcement claims;
- no replay/evidence/report/storage/export claims.

## Gate For Future App Service Expansion

Before expanding the app service beyond the current gated skeleton, a future PR must prove:

- future app service expansion PR must be separately scoped;
- separately scoped PR;
- preflight proof in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md) is satisfied;
- local-lab-only service name;
- loopback/local exposure;
- no production profile;
- no production Dockerfile mutation unless separately reviewed;
- no secrets/credentials;
- no external/cloud/tenant targets;
- no CI execution unless separately scoped;
- no Maven wiring unless separately scoped;
- startup command and health/readiness expectations are documented;
- stop conditions from [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md) are satisfied.

## Gate For Future k6 Runner Service

Before adding a k6 service, a future PR must prove:

- future k6 runner PR must be separately scoped;
- separately scoped PR;
- local-only target;
- no performance thresholds that imply production proof;
- no throughput/p95/p99 claims;
- no load/stress/benchmark claim unless explicitly and separately scoped as lab-only;
- no CI execution unless separately reviewed;
- k6 remains manual and separate until that later review changes the boundary.

## Gate For Future Bruno Runner Or Service

Before adding Bruno automation, a future PR must prove:

- future Bruno automation PR must be separately scoped;
- separately scoped PR;
- local-only target;
- no external/tenant/cloud endpoint;
- no secrets/credentials;
- no CI/Maven wiring unless separately reviewed;
- no production API validation claim;
- Bruno remains manual and separate until that later review changes the boundary.

## Gate For Future Toxiproxy Expansion

Before adding more Toxiproxy services or toxics, a future PR must prove:

- future Toxiproxy expansion PR must be separately scoped;
- loopback-only listen/upstream;
- no `0.0.0.0`;
- no external/cloud/tenant/private-network targets;
- no claim that fault injection proves production resilience;
- no runtime enforcement claim;
- Toxiproxy remains manual/local-only until that later review changes the boundary.

## Current Status

- Current Compose skeleton contains the existing Toxiproxy service and the gated app-under-test service: [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml).
- The app service skeleton is optional, manual-only, local-lab-only, loopback-bound, not CI-gated, not Maven-wired, not production Docker packaging, and not production runtime behavior: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md).
- The Compose skeleton remains optional, manual-only, local-lab-only, loopback-bound, not CI-gated, not Maven-wired, not production Docker packaging, and not production runtime behavior.
- k6 remains manual and separate: [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md).
- Bruno remains manual and separate: [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md).
- Toxiproxy remains manual/local-only: [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md).
- Manual tooling index and runbook remain current reviewer entry points: [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md) and [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md).
- The end-of-day Compose handoff remains reviewer context only: [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

## Reviewer Checklist For A Future Compose PR

- Confirm the PR is separately scoped to the exact Compose change.
- Confirm any changed service remains local-lab-only.
- Confirm all published ports bind to `127.0.0.1` only.
- Confirm no `0.0.0.0` default exposure.
- Confirm no production/cloud/tenant/external endpoints.
- Confirm no secrets/credentials.
- Confirm any app service remains within the app-service gate and preflight checklist.
- Confirm no k6 runner service unless the k6 runner gate is satisfied.
- Confirm no Bruno runner service unless the Bruno gate is satisfied.
- Confirm no expanded Toxiproxy services or toxics unless the Toxiproxy expansion gate is satisfied.
- Confirm no CI-gating or Maven wiring unless separately scoped and reviewed.
- Confirm no production Docker packaging, Docker image publishing, registry login/push, or production Compose profile is added.
- Confirm no throughput evidence, no p95/p99 evidence, and no load/stress/benchmark evidence is claimed.
- Confirm no production readiness/certification conclusion is drawn.
- Confirm no live-cloud or real-tenant validation is claimed.
- Confirm no runtime enforcement is claimed.
- Confirm no replay execution, evidence/report generation, storage, or export behavior is claimed.

## Remaining Not-Proven Boundaries

The following remain not proven:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime enforcement;
- Docker/k6/Bruno/Toxiproxy platform implementation beyond optional local-lab skeletons;
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

PR #284 is now the current local-lab Compose baseline. This readiness-gate update is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, or production runtime behavior.

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

Reapply this readiness gate together with [LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md) and [LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md) before considering any later Compose change.

## App-Service Manual Smoke Checklist Update

The app-service manual smoke checklist is now available as a docs/test-only inspection lane: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md). It does not change Compose behavior, app behavior, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, production Compose, production Docker packaging, or production runtime behavior.

The checklist documents that `app-under-test` already exists in local-lab Compose, remains optional/manual/local-lab-only, uses the local `target/` mount read-only, requires manual package first, publishes `127.0.0.1:8080:8080`, keeps Toxiproxy present, keeps k6 manual and separate, keeps Bruno manual and separate, has no k6 runner service, has no Bruno runner service, has no CI-gating, has no Maven wiring, has no Dockerfile change, has no production Docker packaging, has no production Compose change, and has no production runtime behavior change.

The checklist does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.

## App-Service Health/Readiness Documentation Update

The app-service health/readiness documentation lane is now available at [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md). It is docs/test-only and does not change Compose behavior, app behavior, Dockerfiles, Maven, CI, runtime resources, production Compose, production Docker packaging, production runtime behavior, or application endpoints. It adds no health endpoint and no readiness endpoint.

That lane is the next docs-only companion to the manual smoke checklist. It keeps health/readiness observations optional, manual, local-only, package-first, and loopback-bound for the existing `app-under-test` service, and it keeps k6 and Bruno separate manual tools with no runner services.

The lane does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.

## App-Service Runbook Update

The app-service runbook is now available at [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md). It is documentation only and acts as a reviewer runbook/refinement that ties together the app-service skeleton, the manual smoke checklist, and the health/readiness lane while leaving this readiness gate unchanged.

The runbook adds no Compose behavior changes, no app behavior changes, no endpoint changes, no health endpoint, no readiness endpoint, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no production runtime behavior change.

It preserves optional/manual/local-lab-only scope, manual package-first operation, read-only `target/` mounting, `127.0.0.1:8080:8080`, Toxiproxy presence, k6 and Bruno as manual separate tools, no k6 runner service, no Bruno runner service, and manual/local-only health/readiness observations. It does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.
