# Local Lab Docker Compose Boundary Design

This docs/test-only page is a design-only and future-only boundary document for a possible local-lab Docker Compose orchestration lane. This PR does not add Docker Compose. This PR does not add Dockerfiles. It is not CI-gated, not wired into Maven, not production runtime behavior, and not tool execution.

The current reviewer path remains the manual tooling index and runbook: [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md) and [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md). The existing optional tools remain separate and manual: [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md), [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md), and [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md).

The first separately scoped local-lab Compose skeleton is documented in [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md) and lives at [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml). The Compose-specific manual reviewer/operator checklist is [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md). The skeleton remains optional, manual-only, local-lab-only, loopback-bound, not CI-gated, not wired into Maven, and not production runtime behavior.

## Purpose

- Describe how a future local-lab Compose lane could be reviewed before implementation.
- Keep this PR documentation-only and test-only.
- Keep Docker Compose, Dockerfiles, compose profiles, CI automation, Maven wiring, runtime app behavior, and tool execution out of this sprint.
- Preserve local-lab-only, loopback/local defaults with no production/cloud/tenant/external endpoint defaults.
- Make future stop conditions visible before any Compose file exists.

This page does not add Docker Compose, does not add Dockerfiles, does not add CI automation, does not run tools, does not start servers, and does not alter production runtime behavior.

## Future Local-Lab Components

A later separately scoped Compose PR could consider these services as design-only candidates:

| Future service | Design-only role | Boundary |
| --- | --- | --- |
| `app-under-test` | Run the already-built local application inside a local-lab container | local-lab-only; no production packaging or runtime change from this PR |
| `fake-backend-a` | Represent a healthy or baseline local fake backend | loopback/local lab target only; no real tenant or cloud endpoint |
| `fake-backend-b` | Represent a degraded, slow, or error-prone local fake backend | local simulation only; no production traffic |
| `toxiproxy` | Optionally sit between local clients and local fake backends | future-only; not wired into the current app, Maven, k6, or Bruno execution |
| `k6-runner` | Optionally run a local-lab k6 smoke path after explicit review | no load/stress/benchmark evidence and no automatic performance conclusions |
| Bruno/manual API client path | Keep Bruno as a reviewer-operated manual path | not a Compose service unless a future PR explicitly scopes it |
| reviewer artifact volume or logs directory | Optionally collect local run logs for human inspection | no replay execution, evidence/report generation, storage, or export behavior in this PR |

These names are planning labels only. This design does not create Docker Compose files, Dockerfiles, compose profiles, containers, networks, volumes, scripts, or runtime configuration.

## Boundary Rules

- Local-lab-only.
- Loopback/local defaults.
- No production/cloud/tenant/external endpoint defaults.
- No external default network targets.
- No production traffic.
- Not CI-gated until a future explicit PR.
- Not wired into Maven in this PR.
- Not wired into production runtime behavior.
- No automated performance conclusions.
- No production readiness conclusions.
- No throughput evidence.
- No p95/p99 evidence.
- No load/stress/benchmark evidence.

Any future override must remain local/lab-owned unless another separately reviewed boundary explicitly allows more.

## Relationship To Existing Tool Docs

- The k6 skeleton remains manual/local optional: [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js) and [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md).
- The Bruno skeleton remains manual/local optional: [`../lab/bruno/local-lab-smoke/`](../lab/bruno/local-lab-smoke/) and [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md).
- The Toxiproxy config remains manual/local optional: [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json) and [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md).
- The first Compose skeleton remains manual/local optional: [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml) and [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md).
- The Compose manual runbook remains inspection-first and optional: [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).
- The manual tooling index and runbook remain the current reviewer path: [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md) and [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md).
- The broader local-lab tool boundary remains [`LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md`](LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md).

This design remains the preparation lane for Compose work. The first skeleton does not make k6, Bruno, or Toxiproxy automatic. It does not make Docker/k6/Bruno/Toxiproxy platform implementation exist.

## Future Stop Conditions

A future actual Compose PR must stop before merge if:

- any service points to non-loopback or production-looking targets by default;
- any service points to production, cloud, tenant, external, private customer, or secret-bearing endpoints by default;
- Docker config changes production packaging or production runtime behavior;
- CI starts executing Compose without a separate review;
- Maven starts executing Compose without a separate review;
- k6, Bruno, or Toxiproxy are treated as benchmark/proof tooling;
- throughput, p95, p99, load, stress, or benchmark conclusions are implied;
- storage/export/report generation is implied without implementation and review;
- replay execution is implied without implementation and review;
- secrets, credentials, cloud URLs, tenant URLs, or private network targets appear;
- compose profiles, Dockerfiles, scripts, or runtime resources are added outside a separately scoped PR.

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
