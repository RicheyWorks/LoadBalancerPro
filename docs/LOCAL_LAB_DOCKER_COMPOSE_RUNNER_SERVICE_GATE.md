# Local Lab Docker Compose Runner Service Gate

This page is documentation only. It is a future gate for runner services before any PR adds k6 or Bruno automation to local-lab Docker Compose.

It adds no k6 runner service, no Bruno runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, and no production Compose change.

The app-service runbook remains the reviewer path for the existing local-lab app-under-test service: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md). The broader Compose readiness gate remains [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md), and the manual Compose runbook remains [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).

## Current State

- [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml) already contains `app-under-test` and Toxiproxy.
- k6 remains manual and separate: [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md).
- Bruno remains manual and separate: [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md).
- no k6 runner service is added.
- no Bruno runner service is added.
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
- no production runtime behavior change.

## Gate Checklist Before Any Future Runner-Service PR

Before any future PR adds a k6 runner service, Bruno runner service, or other Compose runner automation, it must prove:

- separate PR;
- local-lab-only service name;
- future runner services must stay local-lab-only and loopback/local-targeted;
- future runner services must not target production/cloud/tenant/external endpoints;
- no external endpoints;
- no secrets;
- future runner services must not introduce secrets/credentials;
- no CI/Maven wiring unless separately approved;
- no automated execution unless separately approved;
- no performance claims;
- no production validation claims;
- no automated artifact generation/storage/export;
- explicit stop conditions.

## k6 Runner Service Gate

Any future k6 runner PR must be separately scoped. It must keep k6 local-lab-only and loopback/local-targeted, and it must preserve the current boundary that k6 remains manual and separate until that future PR is reviewed.

Across both runner lanes, future runner services must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.

A future k6 runner PR must not:

- target production/cloud/tenant/external endpoints;
- introduce secrets/credentials;
- imply load/stress/benchmark evidence;
- claim throughput/p95/p99 evidence;
- claim production readiness/certification;
- claim live-cloud or real-tenant validation;
- claim runtime enforcement;
- claim replay/evidence/report/storage/export behavior;
- add CI-gating or Maven wiring unless separately approved.

## Bruno Runner Service Gate

Any future Bruno runner PR must be separately scoped. It must keep Bruno local-lab-only and loopback/local-targeted, and it must preserve the current boundary that Bruno remains manual and separate until that future PR is reviewed.

A future Bruno runner PR must not:

- target production/cloud/tenant/external endpoints;
- introduce secrets/credentials;
- imply load/stress/benchmark evidence;
- claim throughput/p95/p99 evidence;
- claim production readiness/certification;
- claim live-cloud or real-tenant validation;
- claim runtime enforcement;
- claim replay/evidence/report/storage/export behavior;
- add CI-gating or Maven wiring unless separately approved.

## Stop Conditions

Stop a future runner-service PR if any of the following appear:

- the PR is not separately scoped;
- the service name is not local-lab-only;
- the runner target is not loopback/local;
- production/cloud/tenant/external endpoints appear;
- secrets/credentials appear;
- CI/Maven wiring appears without separate approval;
- automated execution appears without separate approval;
- automated artifact generation/storage/export appears;
- load/stress/benchmark evidence is implied;
- throughput/p95/p99 evidence is claimed;
- production readiness/certification is claimed;
- live-cloud or real-tenant validation is claimed;
- runtime enforcement is claimed;
- replay/evidence/report/storage/export behavior is claimed;
- app behavior, endpoint behavior, Dockerfile behavior, production Compose, production Docker packaging, production runtime behavior, k6 behavior, Bruno behavior, or Toxiproxy behavior changes unexpectedly.

## Relationship To Existing Reviewer Paths

- The app-service runbook remains the reviewer path for the existing local-lab app service.
- The app-service health/readiness lane remains manual/local only: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md).
- The app-service manual smoke checklist remains manual/local only: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md).
- The manual tooling index remains the cross-tool entry point: [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md).
- The local-lab progress handoff and next steps remain reviewer context: [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

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
