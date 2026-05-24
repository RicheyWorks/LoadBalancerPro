# Local Lab k6 Bruno Toxiproxy Boundary Plan

This document is a docs/test-only k6/Bruno/Toxiproxy implementation boundary plan for local-lab tooling. It now distinguishes the first separately scoped optional k6 smoke script skeleton, first separately scoped optional Bruno collection skeleton, first separately scoped optional Toxiproxy config skeleton, and [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md) from still-future expanded k6, expanded Bruno, expanded Toxiproxy fault execution, and actual Docker/Docker Compose work.

PR #270 added a docs/test-only k6/Bruno/Toxiproxy implementation boundary plan and scenario design specs before tool files existed. PR #272 added one optional local-lab k6 smoke script skeleton at [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js). PR #273 added one optional local-lab Bruno collection skeleton at [`../lab/bruno/local-lab-smoke/`](../lab/bruno/local-lab-smoke/). PR #274 added one optional local-lab Toxiproxy config skeleton at [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json). [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md) is the unified docs/test-only reviewer index for those current manual tool skeletons, and [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md) is the companion manual reviewer/operator runbook for inspection-only review and optional local-only manual use. See [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md), [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md), [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md), [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). The skeletons are not CI-gated, not Dockerized, and must target local/lab-owned loopback endpoints by default. Bruno is not Toxiproxy integration and not k6 execution. Toxiproxy is manual-only, not Docker Compose orchestration, not wired into the application, not wired into Maven, not wired into k6 execution, not wired into Bruno execution, does not start Toxiproxy, and does not start the application. This follow-up adds no Docker Compose files, no automatically run scripts, no CI jobs, no Maven dependencies, no production endpoints, no production listeners, no runtime behavior, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence.

Expanded Toxiproxy fault execution remains future-only tooling in this plan. Docker/Docker Compose is future-only unless separately scoped. [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md) is design-only and future-only; actual Docker/Docker Compose work still requires a separate implementation PR. k6 is limited to the one optional local-lab smoke script skeleton until a later sprint separately scopes expanded k6 scenario files. Bruno is limited to the one optional local-lab Bruno collection skeleton until a later sprint separately scopes expanded Bruno collections. Toxiproxy is limited to the one optional manual loopback-only config skeleton until a later sprint separately scopes fault execution. Passing this documentation guard only means the implementation boundary is reviewer-readable; it is not production proof.

## Purpose

Future local-lab tooling should be introduced only after reviewers can see why the tool exists and where it must stop.

- k6 has one optional local-lab smoke script skeleton and remains a future local-lab tool candidate for expanded controlled traffic scenarios.
- Bruno has one optional local-lab Bruno collection skeleton and remains a future local-lab tool candidate for expanded API check collections.
- Toxiproxy has one optional local-lab Toxiproxy config skeleton and remains a future local-lab tool candidate for expanded network degradation simulation.
- Docker/Docker Compose is a future local service orchestration candidate only after separate boundary review.

This plan does not implement expanded Bruno collections, expanded Toxiproxy fault execution, Docker, Docker Compose, or expanded k6 scenarios. It defines the safe documentation boundary for considering them later.

PR #270 also added docs/test-only scenario design specs for future tool lanes: [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). Future tool work must be separately scoped and must target local/lab endpoints first. The k6 follow-up adds only the optional smoke skeleton described in [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md). The Bruno follow-up adds only the optional manual collection skeleton described in [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md). The Toxiproxy follow-up adds only the optional manual config skeleton described in [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md).

## Relationship To Current Local Lab

The current local-lab stack already includes:

- test-scope loopback fake backend servers;
- test-scope multi-backend loopback harness;
- deterministic loopback traffic smoke client;
- deterministic traffic matrix tests;
- bounded request burst smoke tests;
- optional local-lab k6 smoke script skeleton;
- optional local-lab Bruno collection skeleton;
- optional local-lab Toxiproxy config skeleton;
- in-memory smoke, matrix, and burst summaries;
- reviewer checklist mappings for passive, smoke, matrix, and bounded burst summaries;
- progress handoff and next-step boundary docs.

The current local lab has one optional k6 smoke script skeleton, one optional local-lab Bruno collection skeleton, and one optional local-lab Toxiproxy config skeleton only. Bruno is manual only, not CI-gated, not Dockerized, not Toxiproxy integration, not k6 execution, and must use local/lab-owned loopback endpoints by default. Toxiproxy is manual-only, not CI-gated, not Dockerized, not Docker Compose orchestration, not wired into the application, not wired into Maven, not wired into k6 execution, not wired into Bruno execution, does not start Toxiproxy, does not start the application, and must use local/lab-owned loopback endpoints by default. The current local lab still does not use Docker, Docker Compose, automatically run scripts, CI jobs, production infrastructure, production traffic, replay execution, evidence/report generation, storage, or export behavior.

## Future Implementation Sequence

Each future lane must be separately scoped and reviewed.

- Lane A1: docs-only k6 scenario design.
- Lane A2: docs-only Bruno collection design.
- Lane A3: docs-only Toxiproxy fault model design.
- Lane A4a: first optional local-lab k6 smoke script skeleton.
- Lane A4b: future expanded k6 scenario files only after separate review.
- Lane A5a: first optional local-lab Bruno collection skeleton.
- Lane A5b: future expanded Bruno collection files only after separate review.
- Lane A6a: first optional local-lab Toxiproxy config skeleton.
- Lane A6b: future expanded Toxiproxy fault execution only after separate review.
- Lane A7: future Docker Compose only after a separate Docker boundary plan.
- Lane A7a: docs-only Docker Compose boundary design in [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md).
- Lane A7b: future actual Docker Compose only after a separate implementation PR.

No lane here authorizes expanded Bruno collections, expanded Toxiproxy fault execution, Docker, Docker Compose, CI, Maven, production runtime, replay, report, storage, export, or expanded k6 scenario implementation in this PR.

## Tool-Specific Boundaries

### k6 Future Boundary

- one optional local-lab k6 smoke script skeleton only;
- no automatic Maven or CI execution;
- no load/stress/benchmark claims;
- no throughput/p95/p99 evidence claims until separately implemented and validated in a future lane;
- future k6 work must remain local and controlled first.

### Bruno Current And Future Boundary

- one optional local-lab Bruno collection skeleton only;
- no automatic Maven or CI execution;
- no Toxiproxy integration;
- no k6 execution;
- no load/stress/benchmark claims;
- no throughput/p95/p99 evidence claims;
- no external/live API claims;
- default `baseUrl` must remain local/lab-owned loopback only;
- future Bruno checks must target local/lab endpoints first.

### Toxiproxy Current And Future Boundary

- one optional local-lab Toxiproxy config skeleton only;
- manual-only and no automatic Maven or CI execution;
- not Dockerized and not Docker Compose orchestration;
- not wired into the application, not wired into Maven, not wired into k6 execution, and not wired into Bruno execution;
- does not start Toxiproxy and does not start the application;
- no network damage claims yet;
- default listen/upstream values must remain local/lab-owned loopback only;
- future expanded Toxiproxy fault execution must remain local/lab-only first.

### Docker/Docker Compose Future Boundary

- no current Compose file;
- no container orchestration yet;
- docs-only future Compose boundary design lives in [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md);
- future Compose work must be separately scoped and not treated as production deployment.

## Stop Conditions

Stop before merge if:

- a PR adds expanded k6, expanded Bruno, or expanded Toxiproxy fault files without separate approval;
- the optional k6 smoke skeleton stops defaulting to loopback/local targets;
- the optional k6 smoke skeleton becomes CI-gated or Dockerized;
- the optional Bruno collection skeleton stops defaulting to loopback/local targets;
- the optional Bruno collection skeleton becomes CI-gated, Dockerized, Toxiproxy integration, or k6 execution;
- the optional Toxiproxy config skeleton stops defaulting to loopback/local targets;
- the optional Toxiproxy config skeleton binds to `0.0.0.0`;
- the optional Toxiproxy config skeleton becomes CI-gated, Dockerized, Docker Compose orchestration, wired into the application, wired into Maven, wired into k6 execution, or wired into Bruno execution;
- a PR adds Docker Compose before the Docker boundary plan;
- a PR changes `src/main/java`;
- a PR adds production endpoint wiring;
- a PR changes Maven dependencies;
- a PR changes CI;
- a PR claims production validation, certification, live-cloud validation, or real-tenant validation;
- a PR claims load/stress/benchmark/p95/p99/throughput evidence without a separately validated test lane.

## Reviewer Checklist

- Is this limited to the one optional k6 smoke skeleton, one optional Bruno collection skeleton, one optional Toxiproxy config skeleton, plus docs/tests?
- Is expanded Toxiproxy fault execution still separately scoped?
- Is expanded k6 work still separately scoped?
- Is expanded Bruno work still separately scoped?
- Are Docker/Compose still future-only?
- Are load/stress/benchmark claims absent?
- Are throughput/p95/p99 evidence claims absent?
- Are production/live-cloud/real-tenant claims absent?
- Are storage/export/replay/report claims absent?
- Are next implementation steps separately scoped?

## Not-Proven Boundaries

This boundary plan does not prove:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime enforcement;
- Docker/Toxiproxy platform implementation;
- Docker/Toxiproxy execution;
- expanded k6 scenario implementation;
- expanded Bruno collection implementation;
- expanded Toxiproxy fault execution;
- automatic k6 execution;
- automatic Bruno execution;
- automatic Toxiproxy execution;
- Docker Compose implementation;
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
