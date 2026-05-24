# Local Lab k6 Smoke Script

This document describes the first separately scoped optional local-lab k6 smoke script skeleton: [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js).

The script is local-lab-only tooling. It targets an already-running local app or local-lab-owned endpoint only. It is not CI-gated. It is not Dockerized. It is not a benchmark. It is not a stress test. It is not load testing. It does not prove throughput, p95, p99, production readiness, production certification, live-cloud validation, or real-tenant validation.

The default base URL is `http://127.0.0.1:8080`. The script must not target external hosts by default. Reviewers may set `LOCAL_LAB_BASE_URL` only for local/lab-owned loopback endpoints, such as another `127.0.0.1` or `localhost` port. Non-loopback targets require a separate review and must not be treated as default behavior.

## Relationship To The Design Specs

This smoke skeleton is the first narrow step after the docs/test-only design phase:

- [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md) defines future k6 scenario shapes and now points at this first optional smoke skeleton.
- [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md) defines Bruno collection shapes, and [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md) describes the separate optional local-lab Bruno collection skeleton. The k6 script does not run Bruno, and the Bruno collection does not run k6.
- [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md) remains the Toxiproxy fault-model design reference, and [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md) describes the separate optional local-lab Toxiproxy config skeleton. The k6 script does not run Toxiproxy, and the Toxiproxy config does not run k6.
- [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md) is the unified docs/test-only reviewer index for the current k6, Bruno, and Toxiproxy manual tool skeletons.
- [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md) is the companion manual reviewer/operator runbook for inspection-only review and optional local-only manual use.
- [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md) is the Docker Compose boundary design, [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md) documents the separate optional local-lab Compose skeleton, and [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md) gives the Compose-specific manual checklist. The Compose skeleton does not add k6 execution, k6 automation, CI automation, Maven wiring, or production runtime behavior.
- [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md) is future-only app-service boundary design. It does not add an app service, k6 runner service, CI automation, Maven wiring, Docker packaging, or production runtime behavior.
- [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md) defines preflight proof before any future app-service PR. It does not add k6 runner behavior, app service behavior, CI automation, Maven wiring, Docker packaging, or production runtime behavior.
- [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md) defines the future-change checklist before any Compose file change or k6 runner service. It does not add k6 runner behavior, CI automation, Maven wiring, Docker packaging, or production runtime behavior.

This k6 sprint does not add Docker, Docker Compose, Bruno execution, Toxiproxy execution, CI workflow changes, Maven dependency changes, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage, export, upload, download, PDF, or ZIP behavior. The later Bruno skeleton and later Toxiproxy config skeleton are separate optional manual local-lab tools.

## Manual Use Boundary

The app or local-lab endpoint must already be running before a reviewer manually invokes k6. Maven tests do not run this script, and CI does not run this script.

Example local-only manual command:

```powershell
k6 run lab/k6/local-lab-smoke.js
```

Example loopback override for a locally owned port:

```powershell
$env:LOCAL_LAB_BASE_URL = "http://127.0.0.1:8080"
k6 run lab/k6/local-lab-smoke.js
```

The script performs a tiny smoke walkthrough against `/actuator/health` and checks only that the endpoint responds with status `200` and a body. The tiny request count is for local smoke confidence only. It is not throughput evidence, not p95 evidence, not p99 evidence, not production proof, not live-cloud proof, and not real-tenant proof.

## Reviewer Stop Conditions

Stop before merge or use if:

- the default target is changed away from `127.0.0.1` or `localhost`;
- an external host appears as a default;
- the script becomes automatic Maven or CI execution;
- Docker or Docker Compose is added;
- Bruno execution, automatic Bruno use, Toxiproxy execution, or Toxiproxy wiring are added;
- production endpoint wiring, production listeners, or production runtime behavior changes are required;
- documentation starts claiming benchmark, stress, load, throughput, p95, p99, production readiness, production certification, live-cloud validation, or real-tenant validation evidence.

## Remaining Not-Proven Boundaries

The following remain not proven: production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, Docker/Toxiproxy platform implementation, Docker Compose implementation, expanded k6 scenario implementation, expanded Bruno collection implementation, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, p95 evidence, p99 evidence, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, and facility automation.

## Runner-Service Gate Update

The runner-service gate is now available at [`LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md). It is documentation only and a future gate for runner services. It adds no k6 runner service, no Bruno runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, and no production Compose change.

k6 remains manual and separate. Bruno remains manual and separate. The app-service runbook remains the reviewer path. Any future k6 runner PR must be separately scoped. Any future Bruno runner PR must be separately scoped. Future runner services must stay local-lab-only and loopback/local-targeted, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.

## k6 Runner Service Design Gate Update

The k6 runner service design gate is now available at [`LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md). It is documentation only and a future design gate for a k6 Compose runner service. It adds no k6 runner service, no Bruno runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no automated execution.

k6 remains manual and separate. Bruno remains manual and separate. The app-service runbook remains reviewer path. Any future k6 runner PR must be separately scoped, must stay local-lab-only, must target only loopback/local services, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.
