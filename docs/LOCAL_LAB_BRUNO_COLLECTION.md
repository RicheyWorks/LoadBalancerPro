# Local Lab Bruno Collection

This document describes the first separately scoped optional local-lab Bruno collection skeleton: [`../lab/bruno/local-lab-smoke/`](../lab/bruno/local-lab-smoke/).

The collection is local-lab-only tooling. It targets an already-running local app or local-lab-owned endpoint only. It is optional and manual. It is not CI-gated. It is not Dockerized. It is not Toxiproxy integration. It is not k6 execution. It is not a benchmark. It is not a stress test. It is not load testing. It does not prove throughput, p95, p99, production readiness, production certification, live-cloud validation, or real-tenant validation.

The default `baseUrl` is `http://127.0.0.1:8080` in `environments/local.bru`. The collection must not target external hosts by default. Reviewers may override `baseUrl` only for local/lab-owned loopback endpoints, such as another `127.0.0.1` or `localhost` port. Non-loopback targets require a separate review and must not be treated as default behavior.

## Relationship To Existing Tooling

This Bruno skeleton is the first narrow collection step after the docs/test-only design phase and after the optional k6 smoke script:

- [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md) defines the future Bruno collection shapes and now points at this first optional collection skeleton.
- [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md) describes the separate optional k6 smoke script. The Bruno collection does not run k6, and the k6 script does not run Bruno.
- [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md) remains the k6 scenario design reference.
- [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md) remains the Toxiproxy fault-model design reference, and [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md) describes the separate optional local-lab Toxiproxy config skeleton. The Bruno collection does not run Toxiproxy, and the Toxiproxy config does not run Bruno.
- [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md) is the unified docs/test-only reviewer index for the current k6, Bruno, and Toxiproxy manual tool skeletons.
- [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md) is the companion manual reviewer/operator runbook for inspection-only review and optional local-only manual use.
- [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md) is the Docker Compose boundary design, [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md) documents the separate optional local-lab Compose skeleton, and [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md) gives the Compose-specific manual checklist. The Compose skeleton does not add Bruno execution, Bruno automation, CI automation, Maven wiring, or production runtime behavior.
- [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md) is future-only app-service boundary design. It does not add an app service, Bruno runner service, CI automation, Maven wiring, Docker packaging, or production runtime behavior.

This sprint does not add Docker, Docker Compose, Toxiproxy execution, Toxiproxy wiring, k6 behavior changes, CI workflow changes, Maven dependency changes, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage, export, upload, download, PDF, or ZIP behavior.

## Manual Use Boundary

The app or local-lab endpoint must already be running before a reviewer manually opens or sends these Bruno requests. Maven tests do not run Bruno, and CI does not run Bruno.

The collection includes tiny read-only requests:

- `API Health` -> `{{baseUrl}}/api/health`
- `Actuator Health` -> `{{baseUrl}}/actuator/health`
- `Local Lab Scenarios` -> `{{baseUrl}}/api/lab/scenarios`

These requests are smoke navigation helpers only. They do not create throughput evidence, p95 evidence, p99 evidence, production proof, live-cloud proof, or real-tenant proof.

## Reviewer Stop Conditions

Stop before merge or use if:

- the default `baseUrl` is changed away from `127.0.0.1` or `localhost`;
- an external host appears as a default;
- the collection becomes automatic Maven or CI execution;
- Docker or Docker Compose is added;
- Toxiproxy execution or Toxiproxy wiring is added;
- k6 behavior is changed beyond documentation cross-links;
- production endpoint wiring, production listeners, or production runtime behavior changes are required;
- documentation starts claiming benchmark, stress, load, throughput, p95, p99, production readiness, production certification, live-cloud validation, or real-tenant validation evidence.

## Remaining Not-Proven Boundaries

The following remain not proven: production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, Docker/k6/Toxiproxy platform implementation, Docker Compose implementation, expanded Bruno collection implementation, expanded k6 scenario implementation, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, p95 evidence, p99 evidence, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, and facility automation.
