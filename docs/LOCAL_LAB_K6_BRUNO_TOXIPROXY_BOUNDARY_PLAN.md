# Local Lab k6 Bruno Toxiproxy Boundary Plan

This document is a docs/test-only k6/Bruno/Toxiproxy implementation boundary plan for future k6, Bruno, Toxiproxy, and Docker/Docker Compose local-lab tooling. It prepares reviewer expectations before any tool files exist.

This PR adds a docs/test-only k6/Bruno/Toxiproxy implementation boundary plan only. It adds no k6 scripts, no Bruno collections, no Toxiproxy config, no Docker Compose files, no scripts, no CI jobs, no Maven dependencies, no production endpoints, no production listeners, no runtime behavior, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence.

k6, Bruno, and Toxiproxy are future-only tooling in this plan. Docker/Docker Compose is future-only unless separately scoped. Passing this documentation guard only means the implementation boundary is reviewer-readable; it is not production proof.

## Purpose

Future local-lab tooling should be introduced only after reviewers can see why the tool exists and where it must stop.

- k6 is a future local-lab tool candidate for controlled traffic scenarios.
- Bruno is a future local-lab tool candidate for API check collections.
- Toxiproxy is a future local-lab tool candidate for network degradation simulation.
- Docker/Docker Compose is a future local service orchestration candidate only after separate boundary review.

This plan does not implement those tools. It defines the next safe documentation boundary for considering them later.

This PR also adds docs/test-only scenario design specs for future tool lanes: [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). Future tool work must be separately scoped and must target local/lab endpoints first. The specs add no k6 scripts, no Bruno collections, no Toxiproxy config, no Docker Compose files, no scripts, no CI jobs, no Maven dependencies, no production endpoints, no production listeners, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence.

## Relationship To Current Local Lab

The current local-lab stack already includes:

- test-scope loopback fake backend servers;
- test-scope multi-backend loopback harness;
- deterministic loopback traffic smoke client;
- deterministic traffic matrix tests;
- bounded request burst smoke tests;
- in-memory smoke, matrix, and burst summaries;
- reviewer checklist mappings for passive, smoke, matrix, and bounded burst summaries;
- progress handoff and next-step boundary docs.

The current local lab still does not use k6, Bruno, Toxiproxy, Docker, Docker Compose, scripts, CI jobs, production infrastructure, production traffic, replay execution, evidence/report generation, storage, or export behavior.

## Future Implementation Sequence

Each future lane must be separately scoped and reviewed.

- Lane A1: docs-only k6 scenario design.
- Lane A2: docs-only Bruno collection design.
- Lane A3: docs-only Toxiproxy fault model design.
- Lane A4: future actual k6 files only after boundary approval.
- Lane A5: future actual Bruno collection only after boundary approval.
- Lane A6: future actual Toxiproxy config only after boundary approval.
- Lane A7: future Docker Compose only after a separate Docker boundary plan.

No lane here authorizes implementation in this PR.

## Tool-Specific Boundaries

### k6 Future Boundary

- no current k6 scripts;
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

- a PR adds actual k6/Bruno/Toxiproxy files without separate approval;
- a PR adds Docker Compose before the Docker boundary plan;
- a PR changes `src/main/java`;
- a PR adds production endpoint wiring;
- a PR changes Maven dependencies;
- a PR changes CI;
- a PR claims production validation, certification, live-cloud validation, or real-tenant validation;
- a PR claims load/stress/benchmark/p95/p99/throughput evidence without a separately validated test lane.

## Reviewer Checklist

- Is this docs-only?
- Are k6/Bruno/Toxiproxy still future-only?
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
- Docker/k6/Bruno/Toxiproxy implementation;
- Docker/k6/Bruno/Toxiproxy execution;
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
