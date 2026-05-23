# Local Lab Next Steps Boundary

This document is an end-of-day checkpoint for the local-lab work. It is reviewer handoff text only. It does not add runtime behavior, harness behavior, clients, servers, listeners, tools, storage, export, replay, or report generation.

This PR adds deterministic test-scope bounded request burst smoke tests and in-memory summaries only. The burst uses the existing `src/test/java` multi-backend loopback harness. The burst calls only `127.0.0.1` harness URLs with ephemeral ports. The burst uses fixed small request counts only. It is not k6, Bruno, Docker, Toxiproxy, load testing, stress testing, benchmarking, or production traffic. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing bounded burst tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven.

This PR adds a test-scope bounded burst reviewer checklist mapper and handoff update only. The mapper turns existing in-memory bounded request burst summaries into reviewer checklist entries. It does not call endpoints. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It is not load testing, stress testing, benchmarking, throughput evidence, or p95/p99 evidence. Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped. Passing bounded burst checklist tests is not production proof.

This PR adds a docs/test-only k6/Bruno/Toxiproxy implementation boundary plan only. See [`LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md`](LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md). It adds no k6 scripts, no Bruno collections, no Toxiproxy config, no Docker Compose files, no scripts, no CI jobs, no Maven dependencies, no production endpoints, no production listeners, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence. k6, Bruno, Toxiproxy, and Docker/Docker Compose remain future-only unless separately scoped.

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

## What Is Actually Proven Today

- deterministic test-scope local-lab models exist;
- loopback-only fake backend servers can run in tests;
- multi-backend loopback harness can run with OS-assigned ephemeral ports;
- smoke client can call only `127.0.0.1` loopback harness URLs;
- traffic matrix can cover the required local-lab profiles;
- bounded request burst smoke tests can issue fixed small loopback-only repetitions across the existing matrix;
- bounded burst reviewer checklist mapping can turn in-memory burst summaries into bounded reviewer questions;
- the k6/Bruno/Toxiproxy boundary plan can describe future tool lanes and stop conditions before any tool files exist;
- reviewer checklist and handoff docs explain evidence boundaries;
- all current evidence is local/test-scope only.

## What Remains Not Proven

- not production readiness;
- not production certification;
- not live-cloud validation;
- not real-tenant validation;
- not runtime enforcement;
- not production traffic behavior;
- not Docker/k6/Bruno/Toxiproxy execution;
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
- Lane A4: future actual k6 files only after boundary approval.
- Lane A5: future actual Bruno collection only after boundary approval.
- Lane A6: future actual Toxiproxy config only after boundary approval.
- Lane A7: future Docker Compose only after a separate Docker boundary plan.
- Lane B: test-scope bounded request burst smoke test, still loopback-only.
- Lane C: test-scope fault-style fixture expansion, no Toxiproxy yet.
- Lane D: docs-only Docker Compose design boundary, no compose file yet.
- Lane E: future actual Docker/k6/Bruno PR only after a separate boundary plan.

Lane A is represented by the current docs-only k6/Bruno/Toxiproxy implementation boundary plan. It is not k6 implementation, Bruno implementation, Toxiproxy implementation, Docker/Docker Compose implementation, load testing, stress testing, benchmarking, production traffic, replay execution, evidence/report generation, storage, export, throughput evidence, or p95/p99 evidence.

Lane B is represented by the current bounded burst smoke tests and bounded burst reviewer checklist mapping only as fixed-count local/test-scope context. It is not load testing, stress testing, performance benchmarking, throughput evidence, latency measurement, p95/p99 evidence, production traffic, or production proof.

Each lane must be separately scoped and reviewed. A future lane should stop before merge if it needs production code, production endpoint wiring, Maven dependencies, fixed ports, non-loopback hosts, Docker/k6/Bruno/Toxiproxy implementation, replay execution, evidence/report generation, file writing, storage, export, load/stress/benchmark claims, throughput or p95/p99 claims, or production-validation language.

## Stop Conditions / Safety Gates

- stop if a non-loopback host appears;
- stop if a fixed port appears;
- stop if `src/main/java` changes are needed;
- stop if production endpoint wiring appears;
- stop if Maven dependencies are required;
- stop if CI changes are required;
- stop if actual k6/Bruno/Toxiproxy files appear without separate approval;
- stop if Docker Compose appears before the Docker boundary plan;
- stop if Docker/k6/Bruno/Toxiproxy implementation sneaks in;
- stop if load/stress/benchmark/p95/p99/throughput evidence is claimed without a separately validated test lane;
- stop if docs start claiming production validation.

## Handoff Boundary

Passing the current local-lab checks means the test-scope local-lab evidence chain is coherent enough for separately scoped next steps. It does not mean production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, production traffic behavior, replay execution, evidence/report generation, storage/export behavior, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, or facility automation.
