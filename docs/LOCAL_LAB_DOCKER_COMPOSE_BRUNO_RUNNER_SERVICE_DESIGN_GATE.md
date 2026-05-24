# Local Lab Docker Compose Bruno Runner Service Design Gate

This page is documentation only. It is a future design gate for a Bruno Compose runner service.

It adds no Bruno runner service, no k6 runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, and no production Compose change.

The current Bruno collection remains manual and separate: [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md). The broader runner-service gate remains [`LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md). The k6 runner service design gate remains [`LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md). The app-service runbook remains the reviewer path for the existing local-lab app-under-test service: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md).

## Current State

- [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml) already contains `app-under-test` and Toxiproxy.
- Bruno remains manual and separate.
- k6 remains manual and separate.
- the app-service runbook remains reviewer path.
- no Bruno runner service is added.
- no k6 runner service is added.
- no runner service is added.
- no new Compose services are added.
- no Compose behavior changes.
- no app behavior changes.
- no endpoint changes.
- no CI-gating.
- no Maven wiring.
- no Dockerfile change.
- no production Docker packaging.
- no production Compose change.
- no automated execution.

## Gate Checklist Before Any Future Bruno Runner PR

Before any future Bruno runner PR, reviewers must require:

- separate PR;
- local-lab-only service name;
- future Bruno runner PR must be separately scoped;
- future Bruno runner must stay local-lab-only;
- future Bruno runner must target only loopback/local services;
- future Bruno runner must not target production/cloud/tenant/external endpoints;
- no external endpoints;
- no secrets;
- future Bruno runner must not introduce secrets/credentials;
- no CI/Maven wiring unless separately approved;
- no automated execution unless separately approved;
- no performance claims;
- no production validation claims;
- no automated artifact generation/storage/export;
- explicit stop conditions.

## Required Future Bruno Runner Boundaries

A future Bruno Compose runner service must preserve all of these boundaries:

- Bruno remains manual and separate until a separately scoped future PR is reviewed.
- k6 remains manual and separate.
- The future runner service must stay local-lab-only and loopback/local-targeted.
- The future runner service must target only loopback/local services.
- The future runner service must not target production/cloud/tenant/external endpoints.
- The future runner service must not introduce secrets/credentials.
- The future runner service must not imply production API validation.
- The future runner service must not imply load/stress/benchmark evidence.
- The future runner service must not claim throughput/p95/p99 evidence.
- The future runner service must not claim production readiness/certification.
- The future runner service must not claim live-cloud or real-tenant validation.
- The future runner service must not claim runtime enforcement.
- The future runner service must not claim replay/evidence/report/storage/export behavior.

## Stop Conditions

Stop a future Bruno runner PR if any of the following appear:

- the PR is not separately scoped;
- the service name is not local-lab-only;
- the runner target is not loopback/local;
- production/cloud/tenant/external endpoints appear;
- secrets/credentials appear;
- CI/Maven wiring appears without separate approval;
- automated execution appears without separate approval;
- automated artifact generation/storage/export appears;
- production API validation is implied;
- load/stress/benchmark evidence is implied;
- throughput/p95/p99 evidence is claimed;
- production readiness/certification is claimed;
- live-cloud or real-tenant validation is claimed;
- runtime enforcement is claimed;
- replay/evidence/report/storage/export behavior is claimed;
- Compose behavior, app behavior, endpoint behavior, Dockerfile behavior, production Compose, production Docker packaging, production runtime behavior, k6 behavior, Bruno behavior, or Toxiproxy behavior changes unexpectedly.

## Relationship To Existing Reviewer Paths

- The broad runner-service gate remains the parent gate: [`LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md).
- The Bruno collection remains manual and separate: [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md).
- The k6 runner service design gate remains documentation only: [`LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md).
- The app-service runbook remains reviewer path: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md).
- The app-service health/readiness lane remains manual/local only: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md).
- The app-service manual smoke checklist remains manual/local only: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md).
- The Compose readiness gate remains the future-change checklist: [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).
- The manual Compose runbook remains manual/local only: [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).

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
- load/stress/benchmark evidence;
- throughput evidence;
- p95 evidence;
- p99 evidence;
- throughput/p95/p99 evidence;
- autonomous production traffic shifting;
- carbon-aware routing;
- GPU orchestration;
- power/grid control;
- facility automation;
- broader automation.
