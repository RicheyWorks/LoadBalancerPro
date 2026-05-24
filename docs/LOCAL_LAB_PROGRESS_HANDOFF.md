# Local Lab Progress Handoff

This handoff summarizes what the local lab can do now and what remains not proven. It is docs/test-only reviewer context for ADR-0009 and the local-lab test-scope harness. It does not add production endpoints, production routing, production proxy behavior, storage, export, replay execution, evidence/report generation, Docker/Toxiproxy platform implementation, expanded k6/Bruno implementation, or runtime enforcement.

This PR adds a test-scope traffic smoke reviewer checklist mapper and docs-only progress handoff only. The mapper turns existing in-memory loopback traffic smoke summaries into reviewer checklist entries. It does not call endpoints. It does not start listeners. It does not open ports. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped. Passing smoke checklist tests is not production proof.

This PR adds deterministic test-scope traffic matrix tests and in-memory summaries only. The matrix uses the existing `src/test/java` multi-backend loopback harness. The matrix calls only `127.0.0.1` harness URLs with ephemeral ports. It is not k6, Bruno, Docker, Toxiproxy, load testing, stress testing, or production traffic. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing matrix tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven.

This PR adds a test-scope traffic matrix reviewer checklist mapper and handoff update only. The mapper turns existing in-memory traffic matrix summaries into reviewer checklist entries. It does not call endpoints. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It is not load testing or stress testing. Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped. Passing matrix checklist tests is not production proof.

The traffic matrix layer is now present; traffic matrix reviewer checklist mapping is now present. The matrix layer proves deterministic loopback-only coverage over required local-lab profiles, stable fixture/boundary matching, stable in-memory matrix summaries, and reviewer-friendly checklist language. It does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, Docker/k6/Bruno/Toxiproxy execution, replay execution, evidence/report generation, storage/export, load/stress testing, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, or facility automation.

This PR adds docs/test-only end-of-day handoff and next-step boundary cleanup only. It updates reviewer-facing handoff docs and documentation guard tests. It does not add local-lab harness functionality, client functionality, server functionality, production endpoints, production listeners, Docker/k6/Bruno/Toxiproxy implementation, replay execution, evidence/report generation, file writing, storage, export, or runtime behavior. See [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

This PR adds deterministic test-scope bounded request burst smoke tests and in-memory summaries only. The burst uses the existing `src/test/java` multi-backend loopback harness. The burst calls only `127.0.0.1` harness URLs with ephemeral ports. The burst uses fixed small request counts only. It is not k6, Bruno, Docker, Toxiproxy, load testing, stress testing, benchmarking, or production traffic. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing bounded burst tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven.

This PR adds a test-scope bounded burst reviewer checklist mapper and handoff update only. The mapper turns existing in-memory bounded request burst summaries into reviewer checklist entries. It does not call endpoints. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It is not load testing, stress testing, benchmarking, throughput evidence, or p95/p99 evidence. Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped. Passing bounded burst checklist tests is not production proof.

The bounded burst layer is now present; bounded burst reviewer checklist mapping is now present. The bounded burst layer proves fixed small-count deterministic loopback-only request coverage, stable fixture/boundary matching under repeated calls, stable in-memory burst summaries, and reviewer-friendly checklist language. The reviewer boundary is explicit: bounded burst tests are not production proof; bounded burst tests are not load/stress/benchmark tests; bounded burst tests do not provide throughput or p95/p99 evidence. The layer does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, Docker/k6/Bruno/Toxiproxy execution, replay execution, evidence/report generation, storage/export, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, or facility automation.

This PR adds a docs/test-only k6/Bruno/Toxiproxy implementation boundary plan only. See [`LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md`](LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md). It adds no k6 scripts, no Bruno collections, no Toxiproxy config, no Docker Compose files, no scripts, no CI jobs, no Maven dependencies, no production endpoints, no production listeners, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence. k6, Bruno, Toxiproxy, and Docker/Docker Compose remain future-only unless separately scoped.

The k6/Bruno/Toxiproxy boundary plan is now present. It explains future lanes for docs-only k6 scenario design, docs-only Bruno collection design, docs-only Toxiproxy fault model design, later tool files only after boundary approval, and future Docker Compose only after a separate Docker boundary plan. The plan is not tool implementation and does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, replay execution, evidence/report generation, storage/export, load testing, stress testing, benchmarking, throughput evidence, p95/p99 evidence, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, or facility automation.

PR #270 added docs/test-only future tool scenario design specs: [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). Future tool work must be separately scoped and must target local/lab endpoints first. Those specs added no k6 scripts, no Bruno collections, no Toxiproxy config, no Docker Compose files, no scripts, no CI jobs, no Maven dependencies, no production endpoints, no production listeners, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence.

This sprint adds one optional local-lab k6 smoke script skeleton at [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js). See [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md), [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). It is not CI-gated, not Dockerized, and must target local/lab-owned loopback endpoints by default. It does not add Bruno execution, Toxiproxy execution, Docker Compose files, automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This sprint adds one optional local-lab Bruno collection skeleton at [`../lab/bruno/local-lab-smoke/`](../lab/bruno/local-lab-smoke/). See [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). It is optional and manual, not CI-gated, not Dockerized, not Toxiproxy integration, not k6 execution, and must target local/lab-owned loopback endpoints by default. It does not add Toxiproxy execution, Docker Compose files, automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This sprint adds one optional local-lab Toxiproxy config skeleton at [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json). See [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md), [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md), [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md), and [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md). It is optional, manual-only, not CI-gated, not Dockerized, not Docker Compose orchestration, not wired into the application, not wired into Maven, not wired into k6 execution, not wired into Bruno execution, and must target local/lab-owned loopback endpoints by default. It does not start Toxiproxy, does not start the application, and does not add automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md) as the unified manual tooling reviewer index for the optional k6 smoke script skeleton, optional Bruno collection skeleton, and optional Toxiproxy config skeleton. It adds no tool behavior, automated execution, CI-gating, Docker/Compose orchestration, Maven wiring, production runtime wiring, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md) as the companion manual reviewer/operator runbook for the existing optional k6, Bruno, and Toxiproxy skeletons. It adds no tool behavior, automated execution, CI-gating, Docker/Compose orchestration, Maven wiring, production runtime wiring, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md) as future-only Docker Compose boundary design. It does not add Compose files, Dockerfiles, compose profiles, CI automation, Maven wiring, production runtime behavior, tool execution, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

This sprint adds one optional local-lab Docker Compose skeleton at [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml). See [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md), [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md), [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md), [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md), and [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md). It is optional, manual-only, local-lab-only, loopback-bound, not CI-gated, not wired into Maven, not production Docker packaging, not production runtime behavior, not wired into k6 execution, not wired into Bruno execution, and not wired into automated execution. It does not add app container orchestration, k6 runner services, Bruno runner services, automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md) as the Compose-specific manual runbook/checklist for inspecting the optional local-lab Compose skeleton and, if a reviewer chooses, running only manual local-only commands. It adds no new Compose services, no app service, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no production Docker packaging, no production runtime behavior, no automated execution, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark evidence, no throughput evidence, and no p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md) as a future-only app-service boundary design. It adds no app service, no new Compose services, no Compose behavior changes, no Docker packaging changes, no CI-gating, no Maven wiring, no production runtime behavior, no k6 runner service, no Bruno runner service, no automated execution, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark evidence, no throughput evidence, and no p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md) as a readiness gate for future local-lab Compose changes. It adds no Compose behavior changes, no app service, no new Compose services, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no production Docker packaging, no production runtime behavior, no automated execution, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark evidence, no throughput evidence, and no p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md) as the exact preflight checklist before any future app-service Compose PR. It adds no app service, no Compose behavior changes, no Docker packaging changes, no CI-gating, no Maven wiring, no production runtime behavior, no automated execution, no new services, no k6 runner service, no Bruno runner service, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark evidence, no throughput evidence, and no p95/p99 evidence.

This docs/test-only end-of-day Compose handoff update summarizes the merged local-lab tooling and Compose guardrail chain through PR #282. It adds no Compose behavior changes, no app service, no new Compose services, no k6 runner service, no Bruno runner service, no Dockerfile changes, no production Docker packaging, no CI-gating, no Maven wiring, no production runtime behavior, no automated execution, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark evidence, no throughput evidence, and no p95/p99 evidence.

This sprint applies the Compose readiness gate and app-service preflight checklist, then adds the smallest safe optional local-lab app-service skeleton in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md) and [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml). It keeps the existing Toxiproxy service intact, adds one `app-under-test` service using a public JRE image and read-only local `target/` mount, requires manual package first, binds published app ports to `127.0.0.1` only, adds no Dockerfile, adds no production Docker packaging, adds no CI/Maven wiring, adds no k6 runner service, adds no Bruno runner service, and adds no automated execution.

## End-of-Day Compose Handoff Update

Today's merged local-lab chain is now: PR #270 k6 / Bruno / Toxiproxy design specs; PR #272 optional local-lab k6 smoke script skeleton; PR #273 optional local-lab Bruno collection skeleton; PR #274 optional local-lab Toxiproxy config skeleton; PR #275 manual tooling index/checklist; PR #276 manual tooling runbook; PR #277 Docker Compose boundary design; PR #278 optional local-lab Docker Compose skeleton; PR #279 Docker Compose manual runbook; PR #280 Compose app-service boundary design; PR #281 Compose readiness gate; and PR #282 Compose app-service preflight checklist.

Current completed state:

- the k6 skeleton exists and remains optional/manual/local-only;
- the Bruno collection exists and remains optional/manual/local-only;
- the Toxiproxy config exists and remains optional/manual/local-only;
- the Docker Compose skeleton exists and remains optional/manual/local-only;
- Current Compose skeleton now contains Toxiproxy plus the gated app-under-test service;
- the app service skeleton exists and remains optional/manual/local-only;
- k6 remains manual and separate;
- Bruno remains manual and separate;
- Toxiproxy remains manual/local-only;
- Compose is not CI-gated;
- Compose is not Maven-wired;
- no production Docker packaging has been added.

Guardrails now in place:

- Docker Compose boundary design exists: [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md).
- Docker Compose manual runbook exists: [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).
- Compose app-service boundary design exists: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md).
- Compose readiness gate exists: [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).
- Compose app-service preflight checklist exists: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md).
- Compose app-service skeleton exists: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md).

Recommended next sprint: future app-service expansion may be considered only after reusing the app-service preflight checklist and the Compose readiness gate. A future app-service expansion PR must be separately scoped. That next step must be a future separately scoped PR, not current proof.

Stop conditions for a next app-service expansion PR:

- stop if `src/main/java` changes appear;
- stop if production API/routing behavior changes appear;
- stop if Dockerfile or production Compose changes appear unexpectedly;
- stop if CI/Maven wiring appears;
- stop if external/cloud/tenant/private-network targets appear;
- stop if secrets/credentials appear;
- stop if `0.0.0.0` default exposure appears;
- stop if performance/readiness/certification/runtime-enforcement claims appear;
- stop if replay/evidence/report/storage/export behavior appears.

Remaining not-proven boundaries:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime enforcement;
- broader Docker/k6/Bruno/Toxiproxy platform implementation beyond optional local-lab skeletons;
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
- facility automation;
- broader automation.

## End-of-Day Checkpoint

This checkpoint summarizes the completed local-lab stack at the end of the day. All current evidence is local/test-scope only. The loopback layers bind to `127.0.0.1` only and use OS-assigned ephemeral ports in tests. The checkpoint is reviewer handoff context, not production validation.

### Current Local-Lab Stack

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
35. Local-lab Docker Compose app service boundary design.
36. Local-lab Docker Compose readiness gate.
37. Local-lab Docker Compose app service preflight checklist.
38. End-of-day Compose handoff update.

### What Is Actually Proven Today

- deterministic test-scope local-lab models exist;
- loopback-only fake backend servers can run in tests;
- the multi-backend loopback harness can run with ephemeral ports;
- the smoke client can call loopback harness URLs;
- the traffic matrix can cover required local-lab profiles;
- the bounded request burst smoke test can issue fixed small loopback-only repetitions across the existing matrix;
- the bounded burst reviewer checklist can map in-memory burst summaries into reviewer-friendly bounded language;
- the k6/Bruno/Toxiproxy boundary plan can define future tool lanes and stop conditions before any tool files exist;
- the k6/Bruno/Toxiproxy scenario design specs can define future scenario, collection, and fault-model shapes before any tool files exist;
- the optional local-lab k6 smoke script skeleton can document a tiny manual loopback-only k6 walkthrough without CI, Docker, production, benchmark, throughput, p95, or p99 claims;
- the optional local-lab Bruno collection skeleton can document tiny manual loopback-only Bruno requests without CI, Docker, Toxiproxy integration, k6 execution, production, benchmark, throughput, p95, or p99 claims;
- the optional local-lab Toxiproxy config skeleton can document tiny manual loopback-only proxy placeholders without CI, Docker, Docker Compose orchestration, application wiring, Maven wiring, k6 execution, Bruno execution, runtime enforcement, benchmark, throughput, p95, or p99 claims;
- the local-lab manual tooling index can point reviewers to the current optional manual k6, Bruno, and Toxiproxy skeletons and restate their shared boundaries without requiring tool execution;
- the local-lab manual tooling runbook can give reviewers an inspection-only path and optional local-only manual run path without requiring tool execution;
- the local-lab Docker Compose boundary design can document future local-lab orchestration candidates and stop conditions without adding Compose files, Dockerfiles, CI automation, Maven wiring, runtime behavior, or tool execution;
- the optional local-lab Docker Compose skeleton can document one manual loopback-bound Toxiproxy service mount without CI, Maven wiring, production Docker packaging, production runtime behavior, app container orchestration, k6 runner services, Bruno runner services, runtime enforcement, benchmark, throughput, p95, or p99 claims;
- the local-lab Docker Compose manual runbook can document inspection-only review and optional manual local-only Compose commands without adding services, CI, Maven wiring, production Docker packaging, production runtime behavior, automated execution, runtime enforcement, benchmark, throughput, p95, or p99 claims;
- the local-lab Docker Compose app service boundary design can document future app-service prerequisites and stop conditions without adding an app service, changing Compose behavior, adding Docker packaging, wiring CI or Maven, or changing production runtime behavior;
- the local-lab Docker Compose readiness gate can document the required checklist before future Compose file changes, app services, k6 runner services, Bruno automation, Toxiproxy expansion, CI wiring, or Maven wiring without changing Compose behavior;
- the local-lab Docker Compose app service preflight checklist can document the exact proof reviewers require before any future app-service Compose PR without adding an app service, changing Compose behavior, changing Docker packaging, wiring CI or Maven, or changing production runtime behavior;
- the end-of-day Compose handoff update can summarize the merged local-lab tooling and Compose guardrail chain without adding Compose behavior, app services, runner services, CI wiring, Maven wiring, production Docker packaging, production runtime behavior, automated execution, or evidence behavior;
- reviewer checklist and handoff docs explain boundaries;
- all current evidence is local/test-scope only.

### What Remains Not Proven

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
- not autonomous production traffic shifting;
- not carbon-aware routing;
- not GPU orchestration;
- not power/grid control;
- not facility automation.

### Next Safe Implementation Lanes

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
- Lane A7c1: docs-only future app-service boundary design in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md).
- Lane A7c2: docs-only Compose readiness gate in [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).
- Lane A7c3: docs-only app-service preflight checklist in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md).
- Lane A7c4: docs-only end-of-day Compose handoff update in this progress handoff and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).
- Lane A7d: first gated app-service Compose skeleton in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md), using a public JRE image, read-only local `target/` mount, manual package-first operation, and loopback-only published ports.
- Lane A7e: future app-service expansion only after the preflight checklist and readiness gate are reapplied in a separately scoped implementation PR.
- Lane A7f: future broader Docker Compose orchestration only after a separate implementation PR.
- Lane B: test-scope bounded request burst smoke test, still loopback-only.
- Lane C: test-scope fault-style fixture expansion, no Toxiproxy execution yet.
- Lane D: docs-only Docker Compose design boundary, no compose file yet.
- Lane E: future actual Docker/k6/Bruno PR only after a separate boundary plan.

### Stop Conditions / Safety Gates

- stop if a non-loopback host appears;
- stop if a fixed port appears;
- stop if `src/main/java` changes are needed;
- stop if production endpoint wiring appears;
- stop if Maven dependencies are required;
- stop if Docker/Bruno/Toxiproxy implementation sneaks in;
- stop if expanded k6 implementation sneaks in;
- stop if the optional k6 smoke skeleton becomes CI-gated, Dockerized, or non-loopback by default;
- stop if the optional Bruno collection skeleton becomes CI-gated, Dockerized, Toxiproxy integration, k6 execution, or non-loopback by default;
- stop if the optional Toxiproxy config skeleton becomes CI-gated, Dockerized, Docker Compose orchestration, wired into the application, wired into Maven, wired into k6 execution, wired into Bruno execution, starts Toxiproxy, starts the application, binds to `0.0.0.0`, or becomes non-loopback by default;
- stop if the optional local-lab Compose skeleton publishes non-loopback ports, contains `0.0.0.0`, contains production-looking domains, introduces secrets, adds an app service, adds a k6 runner service, adds a Bruno runner service, becomes CI-gated, becomes wired into Maven, changes production Docker packaging, or changes production runtime behavior;
- stop if docs start claiming production validation.

## What Exists Now

- passive scenario catalog;
- passive response fixtures;
- passive transcript fixtures;
- transcript summaries;
- passive reviewer checklist mapping;
- implementation readiness gate;
- in-memory fake backend handler;
- loopback fake backend server;
- lifecycle hardening;
- multi-backend loopback harness;
- transcript alignment;
- deterministic loopback traffic smoke client;
- in-memory smoke summary;
- smoke reviewer checklist mapping;
- deterministic traffic matrix tests;
- in-memory traffic matrix summary;
- traffic matrix reviewer checklist mapping;
- bounded request burst smoke tests;
- in-memory bounded request burst summary renderer.
- bounded burst reviewer checklist mapping.
- k6/Bruno/Toxiproxy implementation boundary plan.
- optional local-lab k6 smoke script skeleton.
- optional local-lab Bruno collection skeleton.
- optional local-lab Toxiproxy config skeleton.
- local-lab manual tooling index and reviewer checklist.
- local-lab manual tooling runbook.
- local-lab Docker Compose boundary design.
- optional local-lab Docker Compose skeleton.
- local-lab Docker Compose manual runbook/checklist.
- local-lab Docker Compose app service boundary design.
- local-lab Docker Compose readiness gate.
- local-lab Docker Compose app service preflight checklist.
- end-of-day Compose handoff update.

## What Each Layer Proves

| Layer | Reviewer value | Still bounded by |
| --- | --- | --- |
| passive scenario catalog | stable scenario/profile names for healthy, slow, degraded, error-prone, overloaded, no-good-choice, and recovery cases | metadata only |
| passive response fixtures | deterministic expected response labels for each scenario | fixture data only |
| passive transcript fixtures | deterministic request/response labels for future scenario evidence | no HTTP execution |
| transcript summaries | in-memory rollup of passive transcript labels | no report files |
| passive reviewer checklist mapping | reviewer questions for passive summaries | checklist data only |
| implementation readiness gate | confirms passive chain coherence for a separately scoped step | not production readiness |
| in-memory fake backend handler | maps simulated request labels to fixtures | no server by itself |
| loopback fake backend server | exposes the test-scope handler on 127.0.0.1 with ephemeral ports inside tests | no production endpoint |
| lifecycle hardening | checks clean start/stop and loopback/ephemeral boundaries | no long-running service |
| multi-backend loopback harness | starts one test-scope loopback server per profile in tests | no Docker or external tooling |
| transcript alignment | checks passive transcript labels against loopback responses | no replay execution |
| deterministic loopback traffic smoke client | calls only 127.0.0.1 harness URLs and returns in-memory observations | not production traffic |
| in-memory smoke summary | summarizes loopback-only coverage, fixture matches, and boundaries | no evidence/report generation |
| smoke reviewer checklist mapping | maps the smoke summary into stable reviewer checklist items | no persistence or export |
| deterministic traffic matrix tests | exercise a small scenario/profile matrix through the existing loopback harness and smoke client | not load/stress testing |
| in-memory traffic matrix summary | summarizes matrix case count, fixture matches, boundary cases, loopback-only use, and ephemeral-port use | no evidence/report generation |
| traffic matrix reviewer checklist mapping | maps the matrix summary into stable reviewer checklist items | no persistence, export, or production validation |
| bounded request burst smoke tests | issue fixed small loopback-only request repetitions across the existing matrix through the harness and smoke client | not load/stress/benchmark testing |
| in-memory bounded request burst summary | summarizes burst case count, total request count, fixed repetition count, fixture matches, boundary responses, loopback-only use, and ephemeral-port use | no evidence/report generation |
| bounded burst reviewer checklist mapping | maps the bounded burst summary into stable reviewer checklist items | no persistence, export, production validation, load testing, stress testing, benchmarking, throughput evidence, or p95/p99 evidence |
| k6/Bruno/Toxiproxy implementation boundary plan | documents future local-lab tool lanes, reviewer checklist language, and stop conditions | no expanded k6 scripts, expanded Bruno collections, expanded Toxiproxy fault execution, Docker Compose files, automatic execution, CI jobs, dependencies, replay, report, storage, export, or production validation |
| optional local-lab k6 smoke script skeleton | gives reviewers a tiny manual k6 walkthrough against an already-running loopback app endpoint | not CI-gated, not Dockerized, local/lab-owned loopback endpoints only, no Bruno execution, no Toxiproxy execution, no Docker Compose, no production behavior, no replay, no report, no storage, no export, no benchmark, no throughput evidence, no p95/p99 evidence |
| optional local-lab Bruno collection skeleton | gives reviewers tiny manual Bruno requests against an already-running loopback app endpoint | not CI-gated, not Dockerized, not Toxiproxy integration, not k6 execution, local/lab-owned loopback endpoints only, no Toxiproxy execution, no Docker Compose, no production behavior, no replay, no report, no storage, no export, no benchmark, no throughput evidence, no p95/p99 evidence |
| optional local-lab Toxiproxy config skeleton | gives reviewers tiny manual loopback-only proxy placeholders for later local review | manual-only, not CI-gated, not Dockerized, not Docker Compose orchestration, not wired into the application, not wired into Maven, not wired into k6 execution, not wired into Bruno execution, does not start Toxiproxy, does not start the application, local/lab-owned loopback endpoints only, no production behavior, no replay, no report, no storage, no export, no runtime enforcement, no benchmark, no throughput evidence, no p95/p99 evidence |
| local-lab Docker Compose boundary design | documents future local-lab orchestration candidates, boundary rules, and stop conditions | docs/test-only future design; no Compose files, Dockerfiles, compose profiles, CI automation, Maven wiring, runtime behavior, tool execution, replay, report, storage, export, benchmark, throughput evidence, or p95/p99 evidence |
| optional local-lab Docker Compose skeleton | gives reviewers one optional manual loopback-bound Compose skeleton for the existing local Toxiproxy config and gated app service | manual-only, local-lab-only, 127.0.0.1 published ports only, not CI-gated, not wired into Maven, not production Docker packaging, not production runtime behavior, no k6 runner, no Bruno runner, no automatic execution, no production endpoints, no replay, no report, no storage, no export, no benchmark, no throughput evidence, no p95/p99 evidence |
| local-lab Docker Compose manual runbook/checklist | gives reviewers a Compose-specific inspection path and optional manual local-only commands | docs/test-only, no k6 runner, no Bruno runner, no CI-gating, no Maven wiring, no production Docker packaging, no production runtime behavior, no automated execution, no benchmark, no throughput evidence, no p95/p99 evidence |
| local-lab Docker Compose app service boundary design | documents prerequisites and stop conditions for app service changes | no Docker packaging changes, no CI-gating, no Maven wiring, no production runtime behavior, no automated execution, no benchmark, no throughput evidence, no p95/p99 evidence |
| local-lab Docker Compose readiness gate | documents the required future-change checklist before Compose file, service, CI, or Maven changes | docs/test-only, no Compose behavior changes, no app service, no new Compose services, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no production runtime behavior, no automated execution, no benchmark, no throughput evidence, no p95/p99 evidence |
| local-lab Docker Compose app service preflight checklist | documents exact proof required before app-service Compose changes | no Docker packaging changes, no CI-gating, no Maven wiring, no production runtime behavior, no automated execution, no benchmark, no throughput evidence, no p95/p99 evidence |
| local-lab Docker Compose app service skeleton | gives reviewers one optional manual local-lab app service using a public JRE image and read-only local `target/` mount | manual package-first, local-lab-only, 127.0.0.1 published app port only, no Dockerfile, no image build, no CI-gating, no Maven wiring, no production Docker packaging, no production runtime behavior, no k6 runner, no Bruno runner, no automated execution, no benchmark, no throughput evidence, no p95/p99 evidence |
| end-of-day Compose handoff update | summarizes the merged local-lab tooling and Compose guardrail chain for reviewer handoff | docs/test-only, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no production Docker packaging, no production runtime behavior, no automated execution, no benchmark, no throughput evidence, no p95/p99 evidence |

## What Each Layer Does Not Prove

Local loopback smoke is not production proof. The local-lab chain does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, replay execution, evidence/report generation, storage/export, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, or facility automation.

The traffic matrix layer does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, Docker/k6/Bruno/Toxiproxy execution, replay execution, evidence/report generation, storage/export, load/stress testing, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, or facility automation.

The bounded request burst layer uses fixed small request counts only. It is not load testing, stress testing, performance benchmarking, throughput evidence, latency measurement, p95/p99 evidence, production traffic, production proof, live-cloud validation, real-tenant validation, runtime enforcement, Docker/k6/Bruno/Toxiproxy execution, replay execution, evidence/report generation, storage/export, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, or facility automation.

The bounded burst reviewer checklist mapping turns the in-memory burst summary into reviewer questions about burst case count, total request count, fixed repetition count, fixture matches, boundary responses, loopback-only use, ephemeral-port use, deterministic output, no-production-proof warnings, no load/stress/benchmark warning, no throughput/p95/p99 evidence warning, no Docker/k6/Bruno/Toxiproxy warning, and no replay/report/storage/export warning. It is checklist data only and does not call endpoints, write files, persist storage, export anything, or change runtime behavior.

The k6/Bruno/Toxiproxy implementation boundary plan started as docs-only. The first k6 follow-up adds one optional local-lab k6 smoke script skeleton only. The first Bruno follow-up adds one optional local-lab Bruno collection skeleton only. The first Toxiproxy follow-up adds one optional local-lab Toxiproxy config skeleton only. The first Compose follow-up adds one optional local-lab Docker Compose skeleton only. The app-service boundary follow-up adds design guidance. The app-service preflight follow-up adds checklist proof requirements. The gated app-service skeleton follow-up adds one optional manual local-lab app service only. These manual tools are separate: Bruno is not k6 execution, k6 is not Bruno execution, the Toxiproxy config does not run k6, the Toxiproxy config does not run Bruno, and the Compose skeleton does not run k6 or Bruno. They do not add automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production Docker packaging, production runtime behavior, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, or p95/p99 evidence.

Docker/Toxiproxy platform execution remains future-only unless separately scoped. Expanded k6 scenarios, expanded Bruno collections, expanded Toxiproxy fault execution, app container orchestration, k6 runner services, Bruno runner services, and production Docker packaging remain future-only unless separately scoped. Prometheus/Grafana dashboards, automatically run scripts, storage, export/download/upload/PDF/ZIP behavior, and production traffic remain outside this handoff.

## Next Safe Steps

1. Review the small deterministic traffic matrix tests and in-memory traffic matrix summary as local-lab test-scope context only.
2. Then consider k6/Bruno planning docs or a docs-only k6/Bruno/Toxiproxy implementation plan that describes future tooling boundaries without adding tool execution.
3. Review the bounded request burst smoke tests as fixed-count local/test-scope context only; they are not load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.
4. Review bounded burst reviewer checklist mapping as local/test-scope reviewer context, then consider test-scope fault-style fixture expansion or docs-only k6/Bruno/Toxiproxy planning.
5. Keep Docker/Toxiproxy execution, expanded k6, expanded Bruno, expanded Toxiproxy fault execution, app container orchestration, k6 runner services, Bruno runner services, and production Docker packaging future-only unless a later sprint separately scopes them.
6. Future app-service expansion may be considered only after reusing [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md) and [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md), and only as a separately scoped implementation PR.
7. Possible next safe lanes remain app-service manual smoke checklist docs, app-service health/readiness documentation only, test-scope fault-style fixture expansion, or a separately scoped broader Compose orchestration plan, but not production traffic.
8. Use the k6/Bruno/Toxiproxy boundary plan to decide whether a future sprint is still docs-only or has crossed into actual tool files.

## Explicit Not-Proven Boundaries

- not production readiness;
- not production certification;
- not live-cloud validation;
- not real-tenant validation;
- not runtime enforcement;
- not Docker/k6/Bruno/Toxiproxy platform implementation;
- not Docker/k6/Bruno/Toxiproxy execution as automated platform behavior;
- not Docker/Toxiproxy execution;
- not broader Docker Compose platform implementation beyond optional local-lab skeletons;
- not automatic Bruno execution;
- not automatic Toxiproxy execution;
- not replay execution;
- not evidence/report generation;
- not storage/export;
- not load testing;
- not stress testing;
- not benchmarking;
- not throughput evidence;
- not p95/p99 evidence;
- not autonomous production traffic shifting;
- not carbon-aware routing;
- not GPU orchestration;
- not power/grid control;
- not facility automation.
- not broader automation.

## App-Service Manual Smoke Checklist Update

The app-service manual smoke checklist is now documented in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md). This update is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, app behavior, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, or production runtime behavior.

Current app-service smoke-checklist state:

- `app-under-test` already exists in local-lab Compose.
- It remains optional/manual/local-lab-only.
- It uses the local `target/` mount read-only and requires a manual package step before optional Compose use.
- The app published port remains `127.0.0.1:8080:8080`.
- Toxiproxy remains present.
- k6 remains manual and separate.
- Bruno remains manual and separate.
- no k6 runner service exists.
- no Bruno runner service exists.
- no CI-gating.
- no Maven wiring.
- no Dockerfile change.
- no production Docker packaging.
- no production Compose change.
- no production runtime behavior change.
- no production readiness/certification claim.
- no live-cloud or real-tenant validation claim.
- no runtime enforcement claim.
- no replay/evidence/report/storage/export behavior claim.
- no load/stress/benchmark claim.
- no throughput/p95/p99 evidence claim.

Next safe lanes remain docs/test-only app-service health/readiness documentation, Compose manual runbook refinements, and future k6/Bruno runner design docs only. Runner services remain blocked until separate gates are created.

## Post-App-Service Compose Handoff Update

PR #284 is now the current local-lab Compose baseline. This handoff update is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, or production runtime behavior.

Current state:

- `app-under-test` service now exists in local-lab Compose.
- It is optional/manual/local-lab-only.
- It uses the local `target/` mount read-only and requires a manual package step before optional use.
- The published app port is loopback-bound at `127.0.0.1:8080:8080`.
- The existing Toxiproxy service remains present and loopback/local.
- k6 remains manual and separate.
- Bruno remains manual and separate.
- no k6 runner service exists.
- no Bruno runner service exists.
- no CI-gating.
- no Maven wiring.
- no Dockerfile change.
- no production Docker packaging.
- no production Compose change.
- no production runtime behavior change.
- no production readiness/certification claim.
- no live-cloud or real-tenant validation claim.
- no runtime enforcement claim.
- no replay/evidence/report/storage/export behavior claim.
- no load/stress/benchmark claim.
- no throughput/p95/p99 evidence claim.

Next safe expansion lanes:

- app-service manual smoke checklist docs;
- Compose manual runbook update for app service;
- app-service health/readiness documentation only;
- future k6/Bruno runner design docs only;
- no runner services until separate gates are created.

Use [LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md), [LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md), and [LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md) before considering any later Compose change.

## App-Service Health/Readiness Documentation Update

The app-service health/readiness documentation lane is now available at [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md). This update is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, app behavior, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, production runtime behavior, or application endpoints. It adds no health endpoint and no readiness endpoint.

Current app-service health/readiness state:

- `app-under-test` already exists in local-lab Compose.
- It remains optional/manual/local-lab-only.
- It uses the local `target/` mount read-only and requires a manual package step before optional Compose use.
- The app published port remains `127.0.0.1:8080:8080`.
- Toxiproxy remains present.
- k6 remains manual and separate.
- Bruno remains manual and separate.
- no k6 runner service exists.
- no Bruno runner service exists.
- no CI-gating.
- no Maven wiring.
- no Dockerfile change.
- no production Docker packaging.
- no production Compose change.
- no production runtime behavior change.
- no production readiness/certification claim.
- no live-cloud or real-tenant validation claim.
- no runtime enforcement claim.
- no replay/evidence/report/storage/export behavior claim.
- no load/stress/benchmark claim.
- no throughput/p95/p99 evidence claim.

Next safe lanes remain Compose manual runbook refinements and future k6/Bruno runner design docs only. Runner services remain blocked until separate gates are created.

## App-Service Runbook Update

The app-service runbook is now available at [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md). It is documentation only and acts as a reviewer runbook/refinement that ties together the app-service skeleton, the manual smoke checklist, and the health/readiness lane.

The runbook adds no Compose behavior changes, no app behavior changes, no endpoint changes, no health endpoint, no readiness endpoint, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no production runtime behavior change.

It preserves optional/manual/local-lab-only scope, manual package-first operation, read-only `target/` mounting, `127.0.0.1:8080:8080`, Toxiproxy presence, k6 and Bruno as manual separate tools, no k6 runner service, no Bruno runner service, and manual/local-only health/readiness observations. It does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.

## Runner-Service Gate Update

The runner-service gate is now available at [`LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md). It is documentation only and a future gate for runner services. It adds no k6 runner service, no Bruno runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, and no production Compose change.

k6 remains manual and separate. Bruno remains manual and separate. The app-service runbook remains the reviewer path. Any future k6 runner PR must be separately scoped. Any future Bruno runner PR must be separately scoped. Future runner services must stay local-lab-only and loopback/local-targeted, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.

## k6 Runner Service Design Gate Update

The k6 runner service design gate is now available at [`LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md). It is documentation only and a future design gate for a k6 Compose runner service. It adds no k6 runner service, no Bruno runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no automated execution.

k6 remains manual and separate. Bruno remains manual and separate. The app-service runbook remains reviewer path. Any future k6 runner PR must be separately scoped, must stay local-lab-only, must target only loopback/local services, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.

## Bruno Runner Service Design Gate Update

The Bruno runner service design gate is now available at [`LOCAL_LAB_DOCKER_COMPOSE_BRUNO_RUNNER_SERVICE_DESIGN_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_BRUNO_RUNNER_SERVICE_DESIGN_GATE.md). It is documentation only and a future design gate for a Bruno Compose runner service. It adds no Bruno runner service, no k6 runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no automated execution.

Bruno remains manual and separate. k6 remains manual and separate. The app-service runbook remains reviewer path. Any future Bruno runner PR must be separately scoped, must stay local-lab-only, must target only loopback/local services, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply production API validation, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.
