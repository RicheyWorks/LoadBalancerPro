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

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md) as future-only app-service boundary design. It does not add an app service, new Compose services, Compose behavior changes, Docker packaging changes, CI-gating, Maven wiring, production runtime behavior, automated execution, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md) as a readiness gate for future local-lab Compose changes. It does not add Compose behavior changes, an app service, new Compose services, k6 runner services, Bruno runner services, CI-gating, Maven wiring, production Docker packaging, production runtime behavior, automated execution, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md) as the exact preflight checklist before any future app-service Compose PR. It does not add an app service, Compose behavior changes, Docker packaging changes, CI-gating, Maven wiring, production runtime behavior, automated execution, new services, k6 runner services, Bruno runner services, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

This docs/test-only end-of-day Compose handoff update summarizes today's merged local-lab tooling and Compose guardrail chain only. It does not change Compose behavior, add an app service, add new Compose services, add k6 runner services, add Bruno runner services, change Dockerfiles, add production Docker packaging, add CI/Maven wiring, change production runtime behavior, add automated execution, or add replay/evidence/report/storage/export behavior.

This sprint applies the Compose readiness gate and app-service preflight checklist, then adds one gated optional local-lab app-service skeleton documented in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md). The app service uses a public JRE image, mounts the existing local `target/` directory read-only, requires manual package first, and publishes only `127.0.0.1:8080:8080`. It adds no Dockerfile, no production Docker packaging, no CI/Maven wiring, no k6 runner service, no Bruno runner service, no automated execution, and no production runtime behavior.

## End-of-Day Compose Handoff Guidance

Today's merged sequence is PR #270 k6 / Bruno / Toxiproxy design specs, PR #272 optional local-lab k6 smoke script skeleton, PR #273 optional local-lab Bruno collection skeleton, PR #274 optional local-lab Toxiproxy config skeleton, PR #275 manual tooling index/checklist, PR #276 manual tooling runbook, PR #277 Docker Compose boundary design, PR #278 optional local-lab Docker Compose skeleton, PR #279 Docker Compose manual runbook, PR #280 Compose app-service boundary design, PR #281 Compose readiness gate, and PR #282 Compose app-service preflight checklist.

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

Guardrails now in place: [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md), [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md), [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md), [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md), and [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md).

Recommended next sprint: future app-service expansion may be considered only after reusing the preflight checklist and readiness gate. A future app-service expansion PR must be separately scoped. That next step must be separately scoped and must not be treated as proof of production readiness, runtime enforcement, performance, replay, evidence, report, storage, export, or broader automation.

Stop conditions for the next app-service expansion PR:

- stop if `src/main/java` changes appear;
- stop if production API/routing behavior changes appear;
- stop if Dockerfile or production Compose changes appear unexpectedly;
- stop if CI/Maven wiring appears;
- stop if external/cloud/tenant/private-network targets appear;
- stop if secrets/credentials appear;
- stop if `0.0.0.0` default exposure appears;
- stop if performance/readiness/certification/runtime-enforcement claims appear;
- stop if replay/evidence/report/storage/export behavior appears.

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
35. Local-lab Docker Compose app service boundary design.
36. Local-lab Docker Compose readiness gate.
37. Local-lab Docker Compose app service preflight checklist.
38. End-of-day Compose handoff update.

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
- the local-lab Docker Compose app service boundary design can describe app-service prerequisites and stop conditions without adding Docker packaging, wiring CI or Maven, or changing production runtime behavior;
- the local-lab Docker Compose readiness gate can describe the checklist future PRs must satisfy before Compose file changes, app services, k6 runner services, Bruno automation, Toxiproxy expansion, CI wiring, or Maven wiring without changing Compose behavior;
- the local-lab Docker Compose app service preflight checklist can describe the exact proof reviewers require before app-service Compose changes without changing Docker packaging, wiring CI or Maven, or changing production runtime behavior;
- the end-of-day Compose handoff update can summarize today's merged local-lab tooling and Compose guardrail chain without changing Compose behavior, adding services, adding CI/Maven wiring, changing production Docker packaging, changing production runtime behavior, or adding evidence behavior;
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
- not facility automation;
- not broader automation.

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
- Lane A7c1: docs-only future app-service boundary design in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md).
- Lane A7c2: docs-only Compose readiness gate in [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).
- Lane A7c3: docs-only app-service preflight checklist in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md).
- Lane A7c4: docs-only end-of-day Compose handoff update in [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and this next-steps boundary.
- Lane A7d: first gated app-service Compose skeleton in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md), using a public JRE image, read-only local `target/` mount, manual package-first operation, and loopback-only published ports.
- Lane A7e: app-service manual smoke checklist docs in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md), with no Compose behavior change.
- Lane A7f: future app-service expansion only after the preflight checklist and readiness gate are reapplied in a separately scoped implementation PR.
- Lane A7g: future broader Docker Compose orchestration only after a separate implementation PR.
- Lane B: test-scope bounded request burst smoke test, still loopback-only.
- Lane C: test-scope fault-style fixture expansion, no Toxiproxy execution yet.
- Lane D: docs-only Docker Compose design boundary, no compose file yet.
- Lane E: future actual Docker/k6/Bruno PR only after a separate boundary plan.

Lane A is represented by the current k6/Bruno/Toxiproxy implementation boundary plan, first optional local-lab k6 smoke script skeleton, first optional local-lab Bruno collection skeleton, first optional local-lab Toxiproxy config skeleton, and first optional local-lab Docker Compose skeleton. It is not expanded k6 implementation, expanded Bruno implementation, expanded Toxiproxy fault execution, production Docker packaging, app container orchestration, k6 runner services, Bruno runner services, load testing, stress testing, benchmarking, production traffic, replay execution, evidence/report generation, storage, export, throughput evidence, or p95/p99 evidence.

Lane B is represented by the current bounded burst smoke tests and bounded burst reviewer checklist mapping only as fixed-count local/test-scope context. It is not load testing, stress testing, performance benchmarking, throughput evidence, latency measurement, p95/p99 evidence, production traffic, or production proof.

Each lane must be separately scoped and reviewed. A future lane should stop before merge if it needs production code, production endpoint wiring, Maven dependencies, fixed ports, non-loopback defaults, Docker/Toxiproxy execution, expanded Toxiproxy fault execution, expanded Bruno implementation, expanded k6 implementation, replay execution, evidence/report generation, file writing, storage, export, load/stress/benchmark claims, throughput or p95/p99 claims, or production-validation language.

A future app-service expansion PR must additionally stop before merge if `src/main/java` changes appear, production API/routing behavior changes appear, Dockerfile or production Compose changes appear unexpectedly, CI/Maven wiring appears, external/cloud/tenant/private-network targets appear, secrets/credentials appear, `0.0.0.0` default exposure appears, performance/readiness/certification/runtime-enforcement claims appear, or replay/evidence/report/storage/export behavior appears.

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
- stop if the optional local-lab Compose skeleton publishes non-loopback ports, contains `0.0.0.0`, contains production-looking domains, introduces secrets, adds a k6 runner service, adds a Bruno runner service, makes the app service non-local or automatic, becomes CI-gated, becomes wired into Maven, changes production Docker packaging, or changes production runtime behavior;
- stop if load/stress/benchmark/p95/p99/throughput evidence is claimed without a separately validated test lane;
- stop if docs start claiming production validation.

## Handoff Boundary

Passing the current local-lab checks means the test-scope local-lab evidence chain is coherent enough for separately scoped next steps. It does not mean production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, production traffic behavior, replay execution, evidence/report generation, storage/export behavior, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, or facility automation.

## Post-App-Service Compose Handoff Update

PR #284 is now the current local-lab Compose baseline. This next-step update is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, or production runtime behavior.

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

Any future lane remains separately scoped and must continue through [LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md), [LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md), and [LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md).

## App-Service Manual Smoke Checklist Update

The app-service manual smoke checklist is now the next safe lane: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md). This is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, app behavior, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, or production runtime behavior.

The checklist states that `app-under-test` already exists in local-lab Compose, remains optional/manual/local-lab-only, uses the local `target/` mount read-only, requires manual package first, publishes `127.0.0.1:8080:8080`, keeps Toxiproxy present, keeps k6 manual and separate, keeps Bruno manual and separate, has no k6 runner service, has no Bruno runner service, has no CI-gating, has no Maven wiring, has no Dockerfile change, has no production Docker packaging, has no production Compose change, and has no production runtime behavior change.

It does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims. Next safe lanes remain app-service health/readiness documentation only and future k6/Bruno runner design docs only; no runner services until separate gates are created.

## App-Service Health/Readiness Documentation Update

The app-service health/readiness documentation lane is now available at [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md). It is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, app behavior, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, production runtime behavior, or application endpoints. It adds no health endpoint and no readiness endpoint.

The lane documents inspection-only review and optional manual local-only observations for the existing `app-under-test` service. It preserves optional/manual/local-lab-only scope, manual package-first operation, read-only `target/` mounting, `127.0.0.1:8080:8080`, Toxiproxy presence, k6 and Bruno as manual separate tools, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no production runtime behavior change.

The lane does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims. Next safe lanes remain Compose manual runbook refinements and future k6/Bruno runner design docs only; no runner services until separate gates are created.

## App-Service Runbook Update

The app-service runbook is now available at [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md). It is documentation only and acts as a reviewer runbook/refinement that ties together the app-service skeleton, the manual smoke checklist, and the health/readiness lane.

The runbook adds no Compose behavior changes, no app behavior changes, no endpoint changes, no health endpoint, no readiness endpoint, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no production runtime behavior change.

It preserves optional/manual/local-lab-only scope, manual package-first operation, read-only `target/` mounting, `127.0.0.1:8080:8080`, Toxiproxy presence, k6 and Bruno as manual separate tools, no k6 runner service, no Bruno runner service, and manual/local-only health/readiness observations. It does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims. Runner services remain behind future gates.

## Runner-Service Gate Update

The runner-service gate is now available at [`LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md). It is documentation only and a future gate for runner services. It adds no k6 runner service, no Bruno runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, and no production Compose change.

k6 remains manual and separate. Bruno remains manual and separate. The app-service runbook remains the reviewer path. Any future k6 runner PR must be separately scoped. Any future Bruno runner PR must be separately scoped. Future runner services must stay local-lab-only and loopback/local-targeted, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.

## k6 Runner Service Design Gate Update

The k6 runner service design gate is now available at [`LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md). It is documentation only and a future design gate for a k6 Compose runner service. It adds no k6 runner service, no Bruno runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no automated execution.

k6 remains manual and separate. Bruno remains manual and separate. The app-service runbook remains reviewer path. Any future k6 runner PR must be separately scoped, must stay local-lab-only, must target only loopback/local services, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.
