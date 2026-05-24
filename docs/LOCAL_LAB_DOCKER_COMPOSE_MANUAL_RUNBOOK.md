# Local Lab Docker Compose Manual Runbook

This docs/test-only page is a manual reviewer/operator runbook for the optional local-lab Compose skeleton at [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml). It complements [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md), [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md), [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md), [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md), [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md), [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md), [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md), and [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md).

Repository paths referenced by this runbook include `docs/LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`, `docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`, `docs/LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`, `docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`, `docs/LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`, `docs/LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`, `docs/LOCAL_LAB_MANUAL_TOOLING_INDEX.md`, and `docs/LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`.

This runbook now covers the gated local-lab app-under-test skeleton. It does not add k6 runner behavior, Bruno runner behavior, CI wiring, Maven wiring, production Docker packaging, production runtime behavior, or automated execution.

The end-of-day Compose handoff is summarized in [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md). That handoff keeps this runbook as an inspection-first path and keeps any future app-service PR behind the Compose readiness gate and app-service preflight checklist.

## Purpose

- Provide a manual reviewer/operator checklist for the existing optional local-lab Compose skeleton.
- Keep review inspection-first and safe when Docker is not installed.
- Keep any optional manual run local-lab-only and loopback-only.
- Preserve that Compose is not CI-gated and not wired into Maven.
- Preserve that the Compose skeleton is not production Docker packaging and not production runtime behavior.

## Inspection-Only Path

This path requires no Docker execution, no Compose execution, no tool execution, no server startup, and no network calls.

1. Inspect [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml).
2. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md).
3. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md).
4. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md).
5. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).
6. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md) before reviewing any future app-service expansion.
7. Inspect [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md).
8. Inspect [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md).
9. Run Maven documentation/file guard tests only if local Maven is available.

Example documentation guard command:

```powershell
mvn test "-Dtest=LocalLabDockerComposeManualRunbookDocumentationTest,LocalLabDockerComposeSkeletonDocumentationTest,LocalLabDockerComposeBoundaryDesignDocumentationTest"
```

## Optional Manual Local-Only Path

These commands are optional manual local-only examples for reviewers who already have Docker available. They are not Maven steps. They are not CI steps. They are not required for verification. They do not prove performance, readiness, runtime enforcement, platform implementation beyond the optional local-lab Compose skeleton, replay execution, evidence/report generation, storage, or export behavior.

Before any optional manual command, confirm every target remains `127.0.0.1` / loopback only and confirm no production, cloud, tenant, private-network, or external endpoints are present.

Inspect the rendered Compose configuration:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml config
```

Optionally start the existing local-lab Toxiproxy service skeleton by hand:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml up toxiproxy
```

Optionally start the gated app service skeleton by hand after packaging first:

```powershell
mvn -q "-DskipTests" package
docker compose -f lab/docker-compose/local-lab-compose.yml up app-under-test
```

Stop the optional manual local-lab Compose skeleton:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml down
```

These commands must stay local-only and reviewer-operated. They must not be added to Maven, CI, scripts, production Docker packaging, production Compose profiles, app runtime behavior, k6 runner behavior, Bruno runner behavior, or automated execution.

## Safety Checklist

- Confirm the Compose file is under `lab/docker-compose/`.
- Confirm published ports bind to `127.0.0.1` only.
- Confirm no `0.0.0.0`.
- Confirm no 0.0.0.0 all-interface bind.
- Confirm no external/cloud/tenant/production endpoint.
- Confirm no secrets/credentials.
- Confirm the only app service is the gated local-lab-only `app-under-test` skeleton.
- Confirm no additional new services.
- Confirm no k6 runner service.
- Confirm no Bruno runner service.
- Confirm the app service uses a read-only local `target/` mount and requires manual package first.
- Confirm not CI-gated.
- Confirm not wired into Maven.
- Confirm not production Docker packaging.
- Confirm no throughput evidence and no p95/p99 evidence.
- Confirm no load/stress/benchmark evidence.
- Confirm no production readiness/certification conclusion.
- Confirm no live-cloud or real-tenant validation.
- Confirm no runtime enforcement.
- Confirm no replay execution, evidence/report generation, storage, or export behavior.

## Troubleshooting

- Docker not installed: use the inspection-only path; do not add CI, Maven, script, or production packaging automation to compensate.
- Wrong working directory: run commands from the repository root or use the inspection-only path.
- Port already in use: stop and choose a separately reviewed local-only adjustment rather than binding to a wider interface.
- Target not loopback: stop; do not use production, cloud, tenant, private-network, or external endpoints.
- Toxiproxy config path missing: inspect [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json) and [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md); do not invent a production target to make it work.
- Need to make it work quickly: do not bypass safety boundaries; keep the review optional, manual-only, local-lab-only, and loopback-only.

## Relationship To Current Manual Tooling

- The k6 smoke script remains a separate optional manual tool: [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md).
- The Bruno collection remains a separate optional manual tool: [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md).
- The Toxiproxy config remains a separate optional manual config: [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md).
- The app service skeleton remains a separate optional manual local-lab service: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md).
- The app service boundary design remains future-only and docs/test-only: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md).
- The app-service preflight checklist remains required before any future app-service expansion PR: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md).
- The Compose readiness gate defines the future-change checklist before editing the Compose file or adding services: [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).
- The manual tooling index remains the top-level checklist: [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md).
- The broader manual tooling runbook remains the inspection-first reviewer path: [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md).
- The end-of-day Compose handoff summarizes the current guardrail chain: [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

## Remaining Not-Proven Boundaries

The following remain not proven:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime enforcement;
- Docker/k6/Bruno/Toxiproxy platform implementation beyond optional local-lab Compose skeleton;
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
