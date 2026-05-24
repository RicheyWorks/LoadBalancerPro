# Local Lab Docker Compose Readiness Gate

This docs/test-only page is a readiness gate for future local-lab Docker Compose changes. It is the future Compose changes checklist for this local-lab lane. It defines the checklist a future PR must satisfy before changing [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml), adding an app service, adding runner services, or wiring Compose into CI or Maven.

This PR does not change Compose. It makes no Compose behavior changes in this PR. It does not add an app service. No app service added. It does not add k6 runner services. No k6 runner service added. It does not add Bruno runner services. No Bruno runner service added. It does not add new Compose services. It does not add CI-gating. It does not add Maven wiring. It does not change production runtime behavior.

The current Compose skeleton remains Toxiproxy-only. The k6 smoke script remains manual and separate. The Bruno collection remains manual and separate. Toxiproxy remains manual/local-only. This readiness gate complements [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md), [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md), [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md), and [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md).

## Purpose

- Define a local-lab-only readiness gate for future Compose file changes.
- Keep this sprint documentation-only and test-only.
- Keep the existing Compose file behavior unchanged.
- Keep app service, k6 runner service, Bruno runner service, and new Compose services out of this PR.
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

## Gate For Future App Service

Before adding an app service, a future PR must prove:

- future app service PR must be separately scoped;
- separately scoped PR;
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

- Current Compose skeleton remains Toxiproxy-only: [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml).
- The Compose skeleton remains optional, manual-only, local-lab-only, loopback-bound, not CI-gated, not Maven-wired, not production Docker packaging, and not production runtime behavior.
- k6 remains manual and separate: [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md).
- Bruno remains manual and separate: [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md).
- Toxiproxy remains manual/local-only: [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md).
- Manual tooling index and runbook remain current reviewer entry points: [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md) and [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md).

## Reviewer Checklist For A Future Compose PR

- Confirm the PR is separately scoped to the exact Compose change.
- Confirm any changed service remains local-lab-only.
- Confirm all published ports bind to `127.0.0.1` only.
- Confirm no `0.0.0.0` default exposure.
- Confirm no production/cloud/tenant/external endpoints.
- Confirm no secrets/credentials.
- Confirm no app service unless the app-service gate is satisfied.
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
