# Local Lab Docker Compose App Service Boundary Design

This docs/test-only page is a future-only and design-only boundary document for a possible `app-under-test` service in the optional local-lab Docker Compose lane. It does not add an app service. No app service is added in this PR. It does not change Compose behavior. It makes no Compose behavior changes. No Compose behavior changes are made. It does not add Docker packaging. It makes no Docker packaging changes. No Docker packaging changes are made. It does not wire Compose into CI or Maven. It is not CI-gated and not Maven-wired. It does not change production runtime behavior.

The current local-lab Compose skeleton remains [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml), documented by [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md). The readiness gate for any future Compose change is [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md), and the exact preflight proof for any future app-service PR is [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md). The existing Compose skeleton remains Toxiproxy-only, optional, manual-only, local-lab-only, loopback-bound, not CI-gated, not Maven-wired, not production Docker packaging, and not production runtime behavior.

The end-of-day Compose handoff in [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md) summarizes the current completed state and stop conditions before any future app-service PR. It does not add an app service and does not change this boundary design.

## Purpose

- Describe what must be true before a future PR adds an `app-under-test` service.
- Keep this sprint documentation-only and test-only.
- Keep Dockerfiles, production Docker packaging, CI wiring, Maven wiring, production runtime behavior, and automated execution out of this sprint.
- Keep the current local-lab Compose skeleton Toxiproxy-only.
- Keep k6, Bruno, and Toxiproxy as separate manual/local-only tools unless a later PR explicitly changes that boundary.

This page does not add new Compose services, app container orchestration, k6 runner service behavior, Bruno runner service behavior, Docker image publishing, registry login/push behavior, scripts, runtime app config, app endpoints, or harness/client/server implementation.

## Future App Service Prerequisites

Before a separately scoped future PR adds an `app-under-test` service, reviewers should require:

- explicit local-lab-only service naming;
- loopback/local-only exposure;
- no `0.0.0.0` default exposure;
- no 0.0.0.0 default exposure;
- no production profile;
- no production Dockerfile mutation unless separately reviewed;
- no Docker image publishing, registry login, or registry push;
- no secrets or credentials;
- no production/cloud/tenant/external endpoints;
- no cloud/private network/tenant targets;
- no CI execution unless separately scoped;
- no Maven wiring unless separately scoped;
- documented startup command and health/readiness expectation;
- documented dependency on an existing packaged artifact only if safe and reviewed;
- documented stop conditions.

If a future app service needs any production code, production routing/scoring/strategy/proxy/API behavior, production Docker packaging, production Compose profile, runtime app config/resource change, or externally reachable network target, it must be split into a separate review before merge.

## Future Stop Conditions

A future app-service PR must stop before merge if:

- it changes `src/main/java`;
- it changes production routing/scoring/proxy/API behavior;
- it changes production Docker packaging unexpectedly;
- it exposes ports on `0.0.0.0` by default;
- it points to production/cloud/tenant/private-network targets;
- it introduces secrets/credentials;
- it wires Compose into CI without separate approval;
- it wires Compose into Maven without separate approval;
- it adds k6 runner service behavior or Bruno runner service behavior without separate scope;
- it implies benchmark/load/stress/performance evidence;
- it implies throughput evidence or p95/p99 evidence;
- it implies production readiness/certification;
- it implies live-cloud or real-tenant validation;
- it implies runtime enforcement;
- it implies replay execution, evidence/report generation, storage, or export behavior.

## Relationship To Current Manual Tooling

- The current Compose skeleton remains Toxiproxy-only: [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml).
- The Compose skeleton doc remains the current implementation boundary for that file: [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md).
- The broader Compose boundary remains [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md).
- The Compose manual runbook remains the inspection-only and optional manual local-only reviewer path: [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).
- The Compose readiness gate defines what a future PR must prove before changing the Compose file, adding services, or adding CI/Maven wiring: [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).
- The app-service preflight checklist defines the exact proof required before a future app-service PR: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md).
- The k6 smoke script remains manual and separate: [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md).
- The Bruno collection remains manual and separate: [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md).
- Toxiproxy remains manual/local-only: [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md).
- The manual tooling index and runbook remain the current reviewer entry points: [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md) and [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md).
- Adding an app service is a future separate PR, not this PR. A future app service PR must be separately scoped.
- The end-of-day Compose handoff points the next app-service sprint back to this design, the readiness gate, and the app-service preflight checklist: [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

## Reviewer Checklist For A Future App Service PR

- Confirm the service name is explicitly local-lab-only.
- Confirm all default exposure is loopback/local-only.
- Confirm there is no `0.0.0.0` default exposure.
- Confirm there are no production/cloud/tenant/external endpoints.
- Confirm there are no secrets/credentials.
- Confirm there is no production profile.
- Confirm there is no production Dockerfile mutation unless separately reviewed.
- Confirm Compose is not CI-gated unless separately scoped.
- Confirm Compose is not Maven-wired unless separately scoped.
- Confirm k6 remains manual and separate unless separately scoped.
- Confirm Bruno remains manual and separate unless separately scoped.
- Confirm Toxiproxy remains manual/local-only unless separately scoped.
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
- Docker/k6/Bruno/Toxiproxy platform implementation beyond optional local-lab skeleton;
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
- facility automation.
