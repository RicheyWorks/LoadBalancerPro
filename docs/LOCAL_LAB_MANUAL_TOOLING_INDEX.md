# Local Lab Manual Tooling Index

This docs/test-only documentation-only index gives reviewers one bounded place to inspect the current optional local-lab manual tooling. [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md) is the companion manual reviewer/operator runbook for inspection-only review and optional local-only manual use. [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md) is the Docker Compose boundary design, [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md) documents the first optional local-lab Compose skeleton, [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md) is the Compose-specific manual checklist, and [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md) is the future-only app-service boundary design. The skeleton is manual-only, loopback/local-only, not CI-gated, not wired into Maven, not production runtime behavior, and not tool automation.

## What Exists Now

| Tool skeleton | Local path | Individual doc | Current boundary |
| --- | --- | --- | --- |
| k6 local-lab smoke script | [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js) | [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md) | optional, manual-only, local-lab-only smoke walkthrough against an already-running loopback/local app endpoint |
| Bruno local-lab collection | [`../lab/bruno/local-lab-smoke/`](../lab/bruno/local-lab-smoke/) | [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md) | optional, manual-only, local-lab-only request collection against an already-running loopback/local app endpoint |
| Toxiproxy local-lab config | [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json) | [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md) | optional, manual-only, local-lab-only loopback proxy config skeleton that does not start Toxiproxy or the application |
| Docker Compose local-lab skeleton | [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml) | [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md) | optional, manual-only, local-lab-only Toxiproxy service skeleton with host-side ports bound to `127.0.0.1` only |

The supporting boundary and design docs remain [`LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md`](LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md), [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md), [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md), [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md), and [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md).

Use [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md) when a reviewer wants a step-by-step manual path that remains optional, local-lab-only, inspection-first, and not CI-gated.

## Shared Boundaries

The k6, Bruno, and Toxiproxy tool skeletons are optional, manual-only, local-lab-only reviewer aids. They use loopback/local defaults and must keep default targets at `127.0.0.1` or `localhost`. They are not CI-gated, not Dockerized, not Docker Compose orchestration, not wired into Maven, not wired into production runtime, and not production traffic automation. The Compose skeleton is also optional, manual-only, local-lab-only, loopback-bound, not CI-gated, not wired into Maven, not wired into production runtime, not production Docker packaging, and not production traffic automation.

The tools are separate from one another: the k6 script does not run Bruno or Toxiproxy; the Bruno collection does not run k6 or Toxiproxy; the Toxiproxy config does not run k6 or Bruno. None of these files should be treated as automatic Maven execution, CI execution, Docker execution, Docker Compose execution, replay execution, report generation, storage, export, or production runtime behavior.

Docker Compose boundaries are described in [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md), the first optional skeleton is documented in [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md), the Compose-specific manual checklist is [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md), and app-service prerequisites are documented in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md). The skeleton does not add Dockerfiles, compose profiles, CI-gating, Maven wiring, runtime behavior, or automatic k6/Bruno/Toxiproxy execution.

## Reviewer Checklist

- Confirm each default target is `127.0.0.1` or `localhost`.
- Confirm the reviewer understands the app and any chosen tool must be started manually if they choose to run it.
- Confirm no external endpoint is used by default.
- Confirm no throughput evidence, no p95/p99 evidence, and no load/stress/benchmark evidence is claimed.
- Confirm no production readiness/certification conclusion is drawn.
- Confirm no live-cloud or real-tenant validation is claimed.
- Confirm no runtime enforcement is claimed.
- Confirm no replay execution, evidence/report generation, storage, or export behavior is claimed.

## Suggested Manual Review Order

1. Read [`LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md`](LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md) and the design docs first.
2. Use [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md) for the inspection-only path and optional local-only manual run path.
3. Inspect [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js) and [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md).
4. Inspect [`../lab/bruno/local-lab-smoke/`](../lab/bruno/local-lab-smoke/) and [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md).
5. Inspect [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json) and [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md).
6. Inspect [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml) and [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md).
7. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md) for the Compose-specific inspection-only path and optional manual local-only commands.
8. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md) for future app-service stop conditions without adding an app service.
9. Use the existing Maven documentation guard tests and local-lab docs as boundary evidence; actually running k6, Bruno, or Toxiproxy is not required, and running Docker Compose is not required for this review.

## Remaining Not-Proven Boundaries

The following remain not proven:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime enforcement;
- Docker/k6/Bruno/Toxiproxy platform implementation;
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
