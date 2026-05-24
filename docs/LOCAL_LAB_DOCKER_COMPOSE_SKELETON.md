# Local Lab Docker Compose Skeleton

This page documents the first optional local-lab Docker Compose skeleton: [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml). It follows the earlier boundary design in [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md). The manual reviewer/operator checklist for inspecting or optionally running this skeleton by hand is [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md). The future-only app-service boundary design is [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md). The future-change readiness gate is [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).

The skeleton is optional. It is manual-only. It is local-lab-only. It is not CI-gated. It is not wired into Maven. It is not production runtime behavior. It is not production Docker packaging. It is not wired into k6 execution. It is not wired into Bruno execution. It is not wired into automated execution.

This skeleton is not a benchmark/load/stress setup. It does not prove throughput, p95, p99, production readiness, production certification, live-cloud validation, real-tenant validation, or runtime enforcement. It does not perform replay execution, evidence/report generation, storage, or export behavior.

Reviewers may inspect the Compose file without running Docker. If reviewers manually run it, they must keep every target loopback/local-only and must not use production, cloud, tenant, private-network, or external endpoints.

## What Exists

- Compose skeleton: [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml)
- Mounted Toxiproxy config: [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json)
- Boundary design: [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md)
- Compose manual runbook: [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md)
- App service boundary design: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md)
- Compose readiness gate: [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md)
- Manual reviewer index: [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md)
- Manual reviewer runbook: [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md)

The Compose skeleton contains one manually run Toxiproxy service. It uses host-side port publishing bound to `127.0.0.1` only and mounts the existing local-lab Toxiproxy config read-only. It does not add an app service, k6 runner service, Bruno runner service, Dockerfile, Compose profile, script, Maven wiring, CI wiring, production runtime wiring, or new endpoint.

## Optional Manual Use Boundary

Inspection is the preferred review path. A reviewer can read the file and run Maven guard tests without Docker installed.

If a reviewer deliberately chooses to run the skeleton manually, the run remains outside Maven and CI:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml up toxiproxy
```

Before any manual run, confirm:

- the published ports are bound to `127.0.0.1`;
- the mounted config uses loopback/local upstreams only;
- the local app or local backend target is reviewer-owned and local if used;
- no production, cloud, tenant, private-network, or external endpoint is introduced;
- no throughput, p95, p99, load, stress, benchmark, production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, replay, report, storage, or export conclusion is drawn.

## Relationship To Existing Tool Docs

- The k6 smoke script remains a separate optional manual tool: [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md). The Compose skeleton does not add a k6 runner service and does not run k6.
- The Bruno collection remains a separate optional manual tool: [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md). The Compose skeleton does not add a Bruno runner service and does not run Bruno.
- The Toxiproxy config remains a separate optional manual config: [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md). The Compose skeleton may mount that config read-only when a reviewer manually runs Docker Compose.
- The broader local-lab boundary remains [`LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md`](LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md).

## Stop Conditions

Stop before merge or manual use if:

- a published port is not bound to `127.0.0.1`;
- `0.0.0.0` appears;
- a production-looking domain, cloud host, tenant host, private-network host, external URL, secret, credential, token, or password appears;
- an app service, k6 runner service, or Bruno runner service is added;
- CI, Maven, scripts, production Docker packaging, production Compose profiles, runtime app config, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage, or export behavior changes are required;
- docs claim benchmark, load, stress, throughput, p95, p99, production readiness, production certification, live-cloud validation, real-tenant validation, or runtime enforcement evidence.

## Remaining Not-Proven Boundaries

The following remain not proven:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime enforcement;
- Docker/k6/Bruno/Toxiproxy platform implementation;
- k6 execution;
- Bruno execution;
- expanded Toxiproxy fault execution;
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
