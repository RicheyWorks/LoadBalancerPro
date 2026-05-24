# Local Lab Next Steps Boundary

This document is an end-of-day checkpoint for the local-lab work. It is reviewer handoff text only. It does not add runtime behavior, harness behavior, clients, servers, listeners, tools, storage, export, replay, or report generation.

This PR adds deterministic test-scope bounded request burst smoke tests and in-memory summaries only. The burst uses the existing `src/test/java` multi-backend loopback harness. The burst calls only `127.0.0.1` harness URLs with ephemeral ports. The burst uses fixed small request counts only. It is not k6, Bruno, Docker, Toxiproxy, load testing, stress testing, benchmarking, or production traffic. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing bounded burst tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven.

This PR adds a test-scope bounded burst reviewer checklist mapper and handoff update only. The mapper turns existing in-memory bounded request burst summaries into reviewer checklist entries. It does not call endpoints. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It is not load testing, stress testing, benchmarking, throughput evidence, or p95/p99 evidence. Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped. Passing bounded burst checklist tests is not production proof.

This PR adds a docs/test-only k6/Bruno/Toxiproxy implementation boundary plan only. See [`LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md`](LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md). It adds no k6 scripts, no Bruno collections, no Toxiproxy config, no Docker Compose files, no scripts, no CI jobs, no Maven dependencies, no production endpoints, no production listeners, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence. k6, Bruno, Toxiproxy, and Docker/Docker Compose remain future-only unless separately scoped.

PR #270 added docs/test-only future tool scenario design specs only: [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). Future tool work must be separately scoped and must target local/lab endpoints first. Those specs added no k6 scripts, no Bruno collections, no Toxiproxy config, no Docker Compose files, no scripts, no CI jobs, no Maven dependencies, no production endpoints, no production listeners, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence.

This sprint adds one optional local-lab k6 smoke script skeleton at [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js). See [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md), [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). It is not CI-gated, not Dockerized, and must target local/lab-owned loopback endpoints by default. It does not add Bruno execution, Toxiproxy execution, Docker Compose files, automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This sprint adds one optional local-lab Bruno collection skeleton at [`../lab/bruno/local-lab-smoke/`](../lab/bruno/local-lab-smoke/). See [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). It is optional and manual, not CI-gated, not Dockerized, not Toxiproxy integration, not k6 execution, and must target local/lab-owned loopback endpoints by default. It does not add Toxiproxy execution, Docker Compose files, automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This sprint adds one optional local-lab Toxiproxy config skeleton at [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json). See [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md), [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md), [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md), and [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md). It is optional, manual-only, not CI-gated, not Dockerized, not Docker Compose orchestration, not wired into the application, not wired into Maven, not wired into k6 execution, not wired into Bruno execution, and must target local/lab-owned loopback endpoints by default. It does not start Toxiproxy, does not start the application, and does not add automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md) as the unified manual tooling reviewer index and checklist for the optional k6 smoke script skeleton, optional Bruno collection skeleton, and optional Toxiproxy config skeleton. It does not add automated execution, Docker/Compose orchestration, CI-gating, Maven wiring, production runtime wiring, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md) as the companion manual reviewer/operator runbook for the existing optional k6, Bruno, and Toxiproxy skeletons. It does not add automated execution, Docker/Compose orchestration, CI-gating, Maven wiring, production runtime wiring, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md) as a future-only Docker Compose boundary design. It does not add Docker Compose files, Dockerfiles, compose profiles, CI-gating, Maven wiring, production runtime behavior, tool execution, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

This sprint adds one optional local-lab Docker Compose skeleton at [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml). See [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md), [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md), [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md), [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md), and [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md). It is optional, manual-only, local-lab-only, loopback-bound, not CI-gated, not wired into Maven, not production Docker packaging, not production runtime behavior, not wired into k6 execution, not wired into Bruno execution, and not wired into automated execution. It does not add app container orchestration, k6 runner services, Bruno runner services, automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md) as the Compose-specific manual runbook/checklist. It does not add new Compose services, app service behavior, k6 runner behavior, Bruno runner behavior, CI-gating, Maven wiring, production Docker packaging, production runtime behavior, automated execution, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

## Current Local-Lab Stack

The current local-lab stack is:

1. ADR-0009 local lab plan.
2. Local lab scenario matrix.
3. Passive fake backend scenario model/catalog.
4. Passive response fixtures.
5. Passive request/response transcript fixtures.
6. Passive transcript summary renderer.
7. Passive reviewer checklist mapping.
8. Implementation readiness gate.
9. Test-scope in-memory fake backend handler.
10. Handler/transcript consistency tests.
11. Test-scope loopback fake backend server harness.
12. Loopback lifecycle hardening.
13. Test-scope multi-backend loopback harness.
14. Multi-backend transcript alignment tests.
15. Deterministic loopback traffic smoke client.
16. In-memory traffic smoke summary renderer.
17. Traffic smoke reviewer checklist mapping.
18. Deterministic traffic matrix tests.
19. Traffic matrix summary renderer.
20. Traffic matrix reviewer checklist mapping.
21. Bounded request burst smoke tests.
22. In-memory bounded request burst summary renderer.
23. Bounded burst reviewer checklist mapping.
24. Local-lab progress handoff docs.
25. k6/Bruno/Toxiproxy implementation boundary plan.
26. k6/Bruno/Toxiproxy scenario design specs.
27. Optional local-lab k6 smoke script skeleton.
28. Optional local-lab Bruno collection skeleton.
29. Optional local-lab Toxiproxy config skeleton.
30. Local-lab manual tooling index and reviewer checklist.
31. Local-lab manual tooling runbook.
32. Local-lab Docker Compose boundary design.
33. Optional local-lab Docker Compose skeleton.
34. Local-lab Docker Compose manual runbook/checklist.

## What Is Actually Proven Today

- deterministic test-scope local-lab models exist;
- loopback-only fake backend servers can run in tests;
- multi-backend loopback harness can run with OS-assigned ephemeral ports;
- smoke client can call only `127.0.0.1` loopback harness URLs;
- traffic matrix can cover the required local-lab profiles;
- bounded request burst smoke tests can issue fixed small loopback-only repetitions across the existing matrix;
- bounded burst reviewer checklist mapping can turn in-memory burst summaries into bounded reviewer questions;
- the k6/Bruno/Toxiproxy boundary plan can describe future tool lanes and stop conditions before any tool files exist;
- the k6/Bruno/Toxiproxy scenario design specs can describe future scenario, collection, and fault-model shapes before any tool files exist;
- the optional local-lab k6 smoke script skeleton can describe and perform a tiny manual loopback-only walkthrough against an already-running local app endpoint without CI, Docker, production, benchmark, throughput, p95, or p99 claims;
- the optional local-lab Bruno collection skeleton can describe tiny manual loopback-only Bruno requests without CI, Docker, Toxiproxy integration, k6 execution, production, benchmark, throughput, p95, or p99 claims;
- the optional local-lab Toxiproxy config skeleton can describe tiny manual loopback-only proxy placeholders without CI, Docker, Docker Compose orchestration, application wiring, Maven wiring, k6 execution, Bruno execution, runtime enforcement, benchmark, throughput, p95, or p99 claims;
- the local-lab Docker Compose boundary design can describe future orchestration stop conditions before any Compose file, Dockerfile, CI automation, Maven wiring, runtime behavior, or tool execution exists;
- the optional local-lab Docker Compose skeleton can describe one manual loopback-bound Toxiproxy service mount without CI, Maven wiring, production Docker packaging, production runtime behavior, app container orchestration, k6 runner services, Bruno runner services, runtime enforcement, benchmark, throughput, p95, or p99 claims;
- the local-lab Docker Compose manual runbook can describe inspection-only review and optional manual local-only Compose commands without adding services, CI, Maven wiring, production Docker packaging, production runtime behavior, automated execution, runtime enforcement, benchmark, throughput, p95, or p99 claims;
- reviewer checklist and handoff docs explain evidence boundaries;
- all current evidence is local/test-scope only.

## What Remains Not Proven

- not production readiness;
- not production certification;
- not live-cloud validation;
- not real-tenant validation;
- not runtime enforcement;
- not production traffic behavior;
- not Docker/Bruno/Toxiproxy execution;
- not automatic k6 execution;
- not automatic Bruno execution;
- not automatic Toxiproxy execution;
- not expanded k6 scenario implementation;
- not expanded Bruno collection implementation;
- not expanded Toxiproxy fault execution;
- not replay execution;
- not evidence/report generation;
- not storage/export behavior;
- not load/stress/benchmark testing;
- not throughput evidence;
- not p95/p99 evidence;
- not autonomous production traffic shifting;
- not carbon-aware routing;
- not GPU orchestration;
- not power/grid control;
- not facility automation.

## Next Safe Implementation Lanes

- Lane A: docs-only k6/Bruno/Toxiproxy implementation plan.
- Lane A1: docs-only k6 scenario design.
- Lane A2: docs-only Bruno collection design.
- Lane A3: docs-only Toxiproxy fault model design.
- Lane A4a: first optional local-lab k6 smoke script skeleton.
- Lane A4b: future expanded k6 scenario files only after separate review.
- Lane A5a: first optional local-lab Bruno collection skeleton.
- Lane A5b: future expanded Bruno collection files only after separate review.
- Lane A6a: first optional local-lab Toxiproxy config skeleton.
- Lane A6b: future expanded Toxiproxy fault execution only after separate review.
- Lane A7a: docs-only Docker Compose design boundary in [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md), no compose file yet.
- Lane A7b: first optional local-lab Docker Compose skeleton in [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md).
- Lane A7c: docs-only Compose manual runbook/checklist in [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).
- Lane A7d: future broader Docker Compose orchestration only after a separate implementation PR.
- Lane B: test-scope bounded request burst smoke test, still loopback-only.
- Lane C: test-scope fault-style fixture expansion, no Toxiproxy execution yet.
- Lane D: docs-only Docker Compose design boundary, no compose file yet.
- Lane E: future actual Docker/k6/Bruno PR only after a separate boundary plan.

Lane A is represented by the current k6/Bruno/Toxiproxy implementation boundary plan, first optional local-lab k6 smoke script skeleton, first optional local-lab Bruno collection skeleton, first optional local-lab Toxiproxy config skeleton, and first optional local-lab Docker Compose skeleton. It is not expanded k6 implementation, expanded Bruno implementation, expanded Toxiproxy fault execution, production Docker packaging, app container orchestration, k6 runner services, Bruno runner services, load testing, stress testing, benchmarking, production traffic, replay execution, evidence/report generation, storage, export, throughput evidence, or p95/p99 evidence.

Lane B is represented by the current bounded burst smoke tests and bounded burst reviewer checklist mapping only as fixed-count local/test-scope context. It is not load testing, stress testing, performance benchmarking, throughput evidence, latency measurement, p95/p99 evidence, production traffic, or production proof.

Each lane must be separately scoped and reviewed. A future lane should stop before merge if it needs production code, production endpoint wiring, Maven dependencies, fixed ports, non-loopback defaults, Docker/Toxiproxy execution, expanded Toxiproxy fault execution, expanded Bruno implementation, expanded k6 implementation, replay execution, evidence/report generation, file writing, storage, export, load/stress/benchmark claims, throughput or p95/p99 claims, or production-validation language.

## Stop Conditions / Safety Gates

- stop if a non-loopback host appears;
- stop if a fixed port appears;
- stop if `src/main/java` changes are needed;
- stop if production endpoint wiring appears;
- stop if Maven dependencies are required;
- stop if CI changes are required;
- stop if expanded k6, expanded Bruno, or expanded Toxiproxy fault files appear without separate approval;
- stop if Docker Compose appears before the Docker boundary plan;
- stop if Docker/Toxiproxy platform implementation sneaks in;
- stop if expanded k6 implementation sneaks in;
- stop if the optional k6 smoke skeleton becomes CI-gated, Dockerized, or non-loopback by default;
- stop if the optional Bruno collection skeleton becomes CI-gated, Dockerized, Toxiproxy integration, k6 execution, or non-loopback by default;
- stop if the optional Toxiproxy config skeleton becomes CI-gated, Dockerized, Docker Compose orchestration, wired into the application, wired into Maven, wired into k6 execution, wired into Bruno execution, starts Toxiproxy, starts the application, binds to `0.0.0.0`, or becomes non-loopback by default;
- stop if the optional local-lab Compose skeleton publishes non-loopback ports, contains `0.0.0.0`, contains production-looking domains, introduces secrets, adds an app service, adds a k6 runner service, adds a Bruno runner service, becomes CI-gated, becomes wired into Maven, changes production Docker packaging, or changes production runtime behavior;
- stop if load/stress/benchmark/p95/p99/throughput evidence is claimed without a separately validated test lane;
- stop if docs start claiming production validation.

## Handoff Boundary

Passing the current local-lab checks means the test-scope local-lab evidence chain is coherent enough for separately scoped next steps. It does not mean production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, production traffic behavior, replay execution, evidence/report generation, storage/export behavior, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, or facility automation.
