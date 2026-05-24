# Local Lab Manual Tooling Runbook

This docs/test-only runbook is a manual reviewer/operator guide for the current optional local-lab tooling only. It complements [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md) by describing how to inspect the current k6, Bruno, Toxiproxy, and Docker Compose skeletons, what a reviewer may optionally run by hand, and which conclusions must remain out of bounds. Docker Compose boundaries remain in [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md), the first optional skeleton is documented in [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md), the Compose-specific inspection checklist is [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md), the future app-service boundary design is [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md), and the future-change readiness gate is [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).

The runbook does not make any local-lab tool CI-gated, wired into Maven, Dockerized, wired into production runtime, or required for verification. The k6, Bruno, and Toxiproxy tool skeletons remain not Dockerized and not Docker Compose orchestration. The Compose skeleton is optional, manual-only, local-lab-only, not CI-gated, not wired into Maven, not production runtime behavior, and not production Docker packaging. It adds no automated execution, no scripts, no app endpoints, no harness/client/server implementation, no replay execution, no evidence/report generation, no storage, and no export behavior.

## Purpose

- Provide a manual reviewer/operator path for local-lab-only tooling.
- Keep the existing tooling optional and manual.
- Keep default targets on `127.0.0.1` or `localhost`.
- Make inspection useful even when k6, Bruno, Toxiproxy, or the app are not running.
- Restate that this runbook does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, throughput evidence, p95 evidence, p99 evidence, p95/p99 evidence, load testing, stress testing, or benchmarking.

## Prerequisites

- A repository checkout.
- Optional local tool installs only if the reviewer chooses to run a tool manually.
- An already-running local application only if the reviewer chooses to hit local endpoints.
- Default targets must stay on `127.0.0.1` or `localhost`.
- Do not use production, cloud, tenant, or external endpoints.

The current local-lab skeletons are:

- k6 smoke script: [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js)
- Bruno collection: [`../lab/bruno/local-lab-smoke/`](../lab/bruno/local-lab-smoke/)
- Toxiproxy config: [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json)
- Docker Compose skeleton: [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml)

## Inspection-Only Path

This path requires no tool execution, no server startup, and no network calls.

1. Inspect [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md).
2. Inspect [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js) and [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md).
3. Inspect [`../lab/bruno/local-lab-smoke/`](../lab/bruno/local-lab-smoke/) and [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md).
4. Inspect [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json) and [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md).
5. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md) as Compose boundary design.
6. Inspect [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml) and [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md) without running Docker.
7. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md) for the Compose-specific inspection-only path and optional manual local-only path.
8. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md) as future-only app service boundary design; it does not add an app service.
9. Inspect [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md) as the future-change checklist before editing Compose or adding services.
10. Run Maven documentation guard tests only if local Maven is available.

Example documentation guard command:

```powershell
mvn test "-Dtest=LocalLabManualToolingRunbookDocumentationTest,LocalLabManualToolingIndexDocumentationTest,LocalLabToxiproxyConfigDocumentationTest,LocalLabBrunoCollectionDocumentationTest,LocalLabK6SmokeScriptDocumentationTest"
```

## Optional Manual Run Path

This path is optional manual run path guidance only. These commands are examples for a reviewer who already has the chosen tool installed and has intentionally started any required local app or local lab endpoint. They are not Maven steps, not CI steps, and not required for verification.

Before any optional manual run:

- Confirm the target is `127.0.0.1` or `localhost`.
- Confirm the app or tool was started manually by the reviewer.
- Confirm no production, cloud, tenant, or external endpoint is used.

Example k6 local-only smoke command after the app is already running on `127.0.0.1:8080`:

```powershell
k6 run lab/k6/local-lab-smoke.js
```

Example local-only k6 override for a reviewer-owned loopback port:

```powershell
$env:LOCAL_LAB_BASE_URL = "http://127.0.0.1:8080"
k6 run lab/k6/local-lab-smoke.js
```

Example Bruno manual review path:

```text
Open lab/bruno/local-lab-smoke/ in Bruno, select the local environment, confirm baseUrl is http://127.0.0.1:8080, then send only the local requests the reviewer chooses to inspect.
```

Example Toxiproxy config inspection:

```powershell
Get-Content lab\toxiproxy\local-lab-toxiproxy.json
```

Example Docker Compose skeleton inspection:

```powershell
Get-Content lab\docker-compose\local-lab-compose.yml
```

Optional manual Compose command after a reviewer confirms all targets are loopback/local-only:

```powershell
docker compose -f lab/docker-compose/local-lab-compose.yml up toxiproxy
```

This runbook does not require running Docker Compose. It does not start Toxiproxy automatically, does not create proxies automatically, does not start the application, does not wire Toxiproxy into k6 or Bruno execution, and does not require Toxiproxy to be installed for inspection-only review.

## Safety Checklist

- Confirm the target is `127.0.0.1` or `localhost`.
- Confirm no production, cloud, tenant, or external endpoint is used.
- Confirm tools are started manually only.
- Confirm no CI or Maven automation is added.
- Confirm the Compose skeleton remains manual-only and local-lab-only if Docker is used by hand.
- Confirm no throughput evidence, no p95 evidence, no p99 evidence, and no p95/p99 evidence is drawn.
- Confirm no load/stress/benchmark evidence is drawn.
- Confirm no production readiness/certification conclusion is drawn.
- Confirm no live-cloud or real-tenant validation is claimed.
- Confirm no runtime enforcement is claimed.
- Confirm no replay execution, evidence/report generation, storage, or export behavior is claimed.

## Troubleshooting

- App not running: use the inspection-only path or start the local app manually through an existing documented local path.
- Wrong port: update only a reviewer-owned loopback value such as `127.0.0.1` or `localhost`.
- Tool not installed: use the inspection-only path; do not add Maven, CI, script, Docker, or Docker Compose automation to compensate.
- Target not loopback: stop and return to the safety checklist.
- Need to make it work quickly: do not bypass safety boundaries; keep the review manual, local-lab-only, and optional.

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
