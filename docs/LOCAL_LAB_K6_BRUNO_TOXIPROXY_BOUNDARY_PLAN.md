# Local Lab k6 Bruno Toxiproxy Boundary Plan

This document is a docs/test-only k6/Bruno/Toxiproxy implementation boundary plan for local-lab tooling. It now distinguishes the first separately scoped optional k6 smoke script skeleton from still-future Bruno, Toxiproxy, and Docker/Docker Compose work.

PR #270 added a docs/test-only k6/Bruno/Toxiproxy implementation boundary plan and scenario design specs before tool files existed. The first separately scoped k6 follow-up adds one optional local-lab k6 smoke script skeleton at [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js). See [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md), [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). The skeleton is not CI-gated, not Dockerized, and must target local/lab-owned loopback endpoints by default. This follow-up adds no Bruno collections, no Toxiproxy config, no Docker Compose files, no automatically run scripts, no CI jobs, no Maven dependencies, no production endpoints, no production listeners, no runtime behavior, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence.

Bruno and Toxiproxy remain future-only tooling in this plan. Docker/Docker Compose is future-only unless separately scoped. k6 is limited to the one optional local-lab smoke script skeleton until a later sprint separately scopes expanded k6 scenario files. Passing this documentation guard only means the implementation boundary is reviewer-readable; it is not production proof.

## Purpose

Future local-lab tooling should be introduced only after reviewers can see why the tool exists and where it must stop.

- k6 has one optional local-lab smoke script skeleton and remains a future local-lab tool candidate for expanded controlled traffic scenarios.
- Bruno is a future local-lab tool candidate for API check collections.
- Toxiproxy is a future local-lab tool candidate for network degradation simulation.
- Docker/Docker Compose is a future local service orchestration candidate only after separate boundary review.

This plan does not implement Bruno, Toxiproxy, Docker, Docker Compose, or expanded k6 scenarios. It defines the safe documentation boundary for considering them later.

PR #270 also added docs/test-only scenario design specs for future tool lanes: [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). Future tool work must be separately scoped and must target local/lab endpoints first. The current k6 follow-up adds only the optional smoke skeleton described in [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md).

## Relationship To Current Local Lab

The current local-lab stack already includes:

- test-scope loopback fake backend servers;
- test-scope multi-backend loopback harness;
- deterministic loopback traffic smoke client;
- deterministic traffic matrix tests;
- bounded request burst smoke tests;
- optional local-lab k6 smoke script skeleton;
- in-memory smoke, matrix, and burst summaries;
- reviewer checklist mappings for passive, smoke, matrix, and bounded burst summaries;
- progress handoff and next-step boundary docs.

The current local lab has one optional k6 smoke script skeleton only. It still does not use Bruno, Toxiproxy, Docker, Docker Compose, automatically run scripts, CI jobs, production infrastructure, production traffic, replay execution, evidence/report generation, storage, or export behavior.

## Future Implementation Sequence

Each future lane must be separately scoped and reviewed.

- Lane A1: docs-only k6 scenario design.
- Lane A2: docs-only Bruno collection design.
- Lane A3: docs-only Toxiproxy fault model design.
- Lane A4a: first optional local-lab k6 smoke script skeleton.
- Lane A4b: future expanded k6 scenario files only after separate review.
- Lane A5: future actual Bruno collection only after boundary approval.
- Lane A6: future actual Toxiproxy config only after boundary approval.
- Lane A7: future Docker Compose only after a separate Docker boundary plan.

No lane here authorizes Bruno, Toxiproxy, Docker, Docker Compose, CI, Maven, production runtime, replay, report, storage, export, or expanded k6 scenario implementation in this PR.

## Tool-Specific Boundaries

### k6 Future Boundary

- one optional local-lab k6 smoke script skeleton only;
- no automatic Maven or CI execution;
- no load/stress/benchmark claims;
- no throughput/p95/p99 evidence claims until separately implemented and validated in a future lane;
- future k6 work must remain local and controlled first.

### Bruno Future Boundary

- no current Bruno collection;
- no external/live API claims;
- future Bruno checks must target local/lab endpoints first.

### Toxiproxy Future Boundary

- no current Toxiproxy config;
- no network damage claims yet;
- future Toxiproxy work must remain local/lab-only first.

### Docker/Docker Compose Future Boundary

- no current Compose file;
- no container orchestration yet;
- future Compose work must be separately scoped and not treated as production deployment.

## Stop Conditions

Stop before merge if:

- a PR adds expanded k6, Bruno, or Toxiproxy files without separate approval;
- the optional k6 smoke skeleton stops defaulting to loopback/local targets;
- the optional k6 smoke skeleton becomes CI-gated or Dockerized;
- a PR adds Docker Compose before the Docker boundary plan;
- a PR changes `src/main/java`;
- a PR adds production endpoint wiring;
- a PR changes Maven dependencies;
- a PR changes CI;
- a PR claims production validation, certification, live-cloud validation, or real-tenant validation;
- a PR claims load/stress/benchmark/p95/p99/throughput evidence without a separately validated test lane.

## Reviewer Checklist

- Is this limited to the one optional k6 smoke skeleton plus docs/tests?
- Are Bruno/Toxiproxy still future-only?
- Is expanded k6 work still separately scoped?
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
- Docker/Bruno/Toxiproxy implementation;
- Docker/Bruno/Toxiproxy execution;
- expanded k6 scenario implementation;
- automatic k6 execution;
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
