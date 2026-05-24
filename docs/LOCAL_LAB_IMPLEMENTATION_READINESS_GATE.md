# Local Lab Implementation Readiness Gate

This document describes the test-scope readiness gate for the passive Local Lab Kit foundation described by [`adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md`](adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md) and [`LOCAL_LAB_SCENARIO_MATRIX.md`](LOCAL_LAB_SCENARIO_MATRIX.md).

This PR adds a test-scope implementation readiness gate only. The readiness gate evaluates passive planning/test artifacts in memory. It does not implement fake backend servers. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It does not start listeners, open ports, call localhost, generate traffic, or run tools. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling. Fake backend execution remains future tooling only. Passing the readiness gate only means ready for a separately scoped implementation PR, not production proof.

This PR adds a test-scope fake backend handler only. The handler maps simulated request labels to existing response fixtures in memory. It does not implement fake backend servers. It does not start listeners. It does not open ports. It does not call localhost. It does not generate traffic. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It does not run tools. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling. Passing handler tests is not production proof.

This PR adds a test-scope loopback fake backend server harness only. The harness lives under `src/test/java`. It binds to `127.0.0.1` only and uses OS-assigned ephemeral ports. It does not add production endpoints. It does not change production routing, proxy, scoring, strategy, or API behavior. It does not add Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing loopback tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven.

This PR adds test-scope lifecycle hardening for the loopback fake backend server only. The harness remains `src/test/java`-only. It binds to `127.0.0.1` only. It uses ephemeral ports. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not add Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing lifecycle tests is not production proof.

This PR adds a test-scope multi-backend loopback harness only. The harness remains `src/test/java`-only. It starts multiple loopback-only fake backend servers using OS-assigned ephemeral ports. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not add Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing multi-backend loopback tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven.

This PR adds test-scope multi-backend transcript alignment tests only. The alignment proves existing passive transcript expectations match loopback harness responses. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not add Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing alignment tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven.

This PR adds a test-scope deterministic loopback traffic smoke client and in-memory summary only. The client calls only the `src/test/java` multi-backend loopback harness on `127.0.0.1` using harness-assigned ephemeral ports. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not add Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing smoke client tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven.

This PR adds a test-scope traffic smoke reviewer checklist mapper and docs-only progress handoff only. The mapper turns existing in-memory loopback traffic smoke summaries into reviewer checklist entries. It does not call endpoints. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped. Passing smoke checklist tests is not production proof. See [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md).

This PR adds deterministic test-scope traffic matrix tests and in-memory summaries only. The matrix uses the existing `src/test/java` multi-backend loopback harness. The matrix calls only `127.0.0.1` harness URLs with ephemeral ports. It is not k6, Bruno, Docker, Toxiproxy, load testing, stress testing, or production traffic. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing matrix tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven. See [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md).

This PR adds a test-scope traffic matrix reviewer checklist mapper and handoff update only. The mapper turns existing in-memory traffic matrix summaries into reviewer checklist entries. It does not call endpoints. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It is not load testing or stress testing. Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped. Passing matrix checklist tests is not production proof. See [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md).

This PR adds docs/test-only end-of-day handoff and next-step boundary cleanup only. It updates reviewer-facing handoff docs and documentation guard tests. It does not add local-lab harness functionality, client functionality, server functionality, production endpoints, production listeners, Docker/k6/Bruno/Toxiproxy implementation, replay execution, evidence/report generation, file writing, storage, export, or runtime behavior. See [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

This PR adds deterministic test-scope bounded request burst smoke tests and in-memory summaries only. The burst uses the existing `src/test/java` multi-backend loopback harness. The burst calls only `127.0.0.1` harness URLs with ephemeral ports. The burst uses fixed small request counts only. It is not k6, Bruno, Docker, Toxiproxy, load testing, stress testing, benchmarking, or production traffic. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing bounded burst tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven. See [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

This PR adds a test-scope bounded burst reviewer checklist mapper and handoff update only. The mapper turns existing in-memory bounded request burst summaries into reviewer checklist entries. It does not call endpoints. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It is not load testing, stress testing, benchmarking, throughput evidence, or p95/p99 evidence. Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped. Passing bounded burst checklist tests is not production proof. See [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

This PR adds a docs/test-only k6/Bruno/Toxiproxy implementation boundary plan only. The plan prepares future local-lab tooling lanes and reviewer stop conditions without adding tool files. It adds no k6 scripts, no Bruno collections, no Toxiproxy config, no Docker Compose files, no scripts, no CI jobs, no Maven dependencies, no production endpoints, no production listeners, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence. k6, Bruno, Toxiproxy, and Docker/Docker Compose remain future-only unless separately scoped. See [`LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md`](LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md).

PR #270 added docs/test-only future tool scenario design specs. See [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). Future tool work must be separately scoped and must target local/lab endpoints first. The specs added no k6 scripts, no Bruno collections, no Toxiproxy config, no Docker Compose files, no scripts, no CI jobs, no Maven dependencies, no production endpoints, no production listeners, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence.

This sprint adds one optional local-lab k6 smoke script skeleton at [`../lab/k6/local-lab-smoke.js`](../lab/k6/local-lab-smoke.js). See [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md), [`LOCAL_LAB_K6_SCENARIO_DESIGN.md`](LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). It is not CI-gated, not Dockerized, and must target local/lab-owned loopback endpoints by default. It does not add Bruno execution, Toxiproxy execution, Docker Compose files, automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This sprint adds one optional local-lab Bruno collection skeleton at [`../lab/bruno/local-lab-smoke/`](../lab/bruno/local-lab-smoke/). See [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md), [`LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md), and [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). It is optional and manual, not CI-gated, not Dockerized, not Toxiproxy integration, not k6 execution, and must target local/lab-owned loopback endpoints by default. It does not add Toxiproxy execution, Docker Compose files, automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This sprint adds one optional local-lab Toxiproxy config skeleton at [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json). See [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md), [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md), [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md), and [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md). It is optional, manual-only, not CI-gated, not Dockerized, not Docker Compose orchestration, not wired into the application, not wired into Maven, not wired into k6 execution, not wired into Bruno execution, and must target local/lab-owned loopback endpoints by default. It does not start Toxiproxy, does not start the application, and does not add automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md) as the unified manual tooling reviewer index for the optional k6, Bruno, and Toxiproxy skeletons. It does not run tools, start servers, call endpoints, write files, change readiness behavior, or add production runtime behavior.

This docs/test-only follow-up adds [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md) as the companion manual reviewer/operator runbook for inspection-only review and optional local-only manual use. It does not run tools, start servers, call endpoints, write files, change readiness behavior, or add production runtime behavior.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md) as future-only Docker Compose boundary design. It does not add Compose files, Dockerfiles, compose profiles, CI automation, Maven wiring, production runtime behavior, tool execution, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

This sprint adds one optional local-lab Docker Compose skeleton at [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml). See [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md), [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md), [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md), [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md), and [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md). It is optional, manual-only, local-lab-only, loopback-bound, not CI-gated, not wired into Maven, not production Docker packaging, not production runtime behavior, not wired into k6 execution, not wired into Bruno execution, and not wired into automated execution. It does not add app container orchestration, k6 runner services, Bruno runner services, automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md) as the Compose-specific manual runbook/checklist. It adds no Compose behavior, no new services, no app service, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no production Docker packaging, no production runtime behavior, no automated execution, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md) as future-only app-service boundary design. It adds no app service, no new Compose services, no Compose behavior changes, no Docker packaging changes, no CI-gating, no Maven wiring, no production runtime behavior, no automated execution, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md) as a readiness gate for future local-lab Compose changes. It adds no Compose behavior changes, no app service, no new Compose services, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no production Docker packaging, no production runtime behavior, no automated execution, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence.

This docs/test-only follow-up adds [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md) as the preflight proof checklist for any future app-service Compose PR. It adds no app service, no Compose behavior changes, no Docker packaging changes, no CI-gating, no Maven wiring, no production runtime behavior, no automated execution, no new services, no k6 runner service, no Bruno runner service, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence.

This gated app-service follow-up uses the Compose readiness gate and app-service preflight checklist before adding [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md). It records that the current Compose skeleton contains Toxiproxy plus one optional/manual/local-only app-under-test service, k6 remains manual and separate, Bruno remains manual and separate, Toxiproxy remains manual/local-only, Compose is not CI-gated, Compose is not Maven-wired, no Dockerfile changed, and no production Docker packaging has been added. Future app-service expansion must remain separately scoped. The app-service manual smoke checklist is documented in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md). The end-of-day Compose handoff remains summarized in [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

expanded Toxiproxy fault execution remains separately scoped and is not proven by this readiness gate.

## Purpose

The readiness gate gives reviewers and operators one deterministic answer to a narrow question: is the passive planning/test evidence chain coherent enough to plan a future, separately scoped implementation PR for an actual fake backend server?

The answer is intentionally bounded. A passing readiness gate means the passive catalogs line up across scenario model, response fixtures, transcript fixtures, transcript summaries, and reviewer checklist mappings. It does not approve runtime work inside the current PR, and it does not prove production behavior.

## Passive Criteria

The gate checks that:

- every scenario has a behavior profile;
- every scenario has a response fixture;
- every scenario has a passive transcript;
- every passive transcript has a summary;
- every summary has a reviewer checklist;
- every checklist includes evidence expectations;
- every checklist includes safety boundaries;
- every checklist includes not-proven boundaries;
- every checklist includes a local-simulation-is-not-production-proof warning;
- Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling;
- fake backend execution remains future tooling only;
- passive artifacts avoid production, live-cloud, and real-tenant validation claims;
- passive artifacts avoid replay execution, storage, export, and runtime behavior claims.

## Assessment Result

The in-memory assessment may report `PASSIVE_FOUNDATION_READY_FOR_SEPARATE_IMPLEMENTATION_PR` when all passive criteria pass. That status means only that the next safe implementation candidate can be separately scoped and reviewed.

The intended next safe implementation candidate is a future separately scoped fake backend server implementation PR with explicit networking, tool, runtime, and not-proven boundaries.

## Non-Goals

This readiness gate does not add:

- production Java behavior;
- routing/scoring/strategy/proxy/API behavior changes;
- Maven dependency or configuration changes;
- Docker Compose implementation;
- scripts implementation;
- fake backend server implementation;
- networking, server, listener, port, or localhost behavior;
- k6, Bruno, Toxiproxy, Prometheus, or Grafana implementation;
- CI changes;
- EvidencePacket or EvidenceAssembler implementation;
- replay execution;
- evidence or report generation;
- file writing;
- storage or persistence;
- export/download/upload/PDF/ZIP behavior;
- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation.

## Review Boundary

Reviewers should treat this gate as implementation-prep evidence only. It helps decide whether the passive chain is ready for a small future implementation PR, but it does not make any server, tool, replay, report, export, storage, or runtime behavior exist.

## Post-App-Service Compose Handoff Update

PR #284 is now the current local-lab Compose baseline. This readiness-gate update is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, or production runtime behavior.

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

Use the Compose-specific [LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md), [LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md), and [LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md) before considering any later Compose change.

## App-Service Manual Smoke Checklist Update

The app-service manual smoke checklist is now documented in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md). It is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, app behavior, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, or production runtime behavior.

It states that `app-under-test` already exists in local-lab Compose, remains optional/manual/local-lab-only, uses the local `target/` mount read-only, requires a manual package step before optional Compose use, publishes `127.0.0.1:8080:8080`, keeps Toxiproxy present, keeps k6 manual and separate, keeps Bruno manual and separate, has no k6 runner service, has no Bruno runner service, has no CI-gating, has no Maven wiring, has no Dockerfile change, has no production Docker packaging, has no production Compose change, and has no production runtime behavior change.

The checklist does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.

## App-Service Health/Readiness Documentation Update

The app-service health/readiness documentation lane is now available at [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md). It is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, app behavior, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, production runtime behavior, or application endpoints. It adds no health endpoint and no readiness endpoint.

It records manual/local-only health/readiness observations for the existing `app-under-test` service while preserving the manual package step, read-only `target/` mount, `127.0.0.1:8080:8080`, Toxiproxy presence, k6 and Bruno as manual separate tools, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no production runtime behavior change.

The lane does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.

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
