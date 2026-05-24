# ADR-0009 Local Lab Kit And Simulated Datacenter Test Harness Plan

## Status

Proposed / planning-only.

Decision type: architecture planning.

Implementation status: not implemented.

## Date

2026-05-22.

## Context

LoadBalancerPro is building toward future datacenter adaptive traffic control that can reason about messy real-world conditions without becoming a black box. The project already has bounded local evidence paths, LASE shadow/evaluation framing, workload realism planning, evidence/reviewer trust planning, safety guardrails, and runtime enforcement/package-boundary planning.

ADR-0009 records the proposed future Local Lab Kit and simulated datacenter test harness plan. The Local Lab Kit should become the practical bridge between today's safe evidence/control-plane work and later controlled experiments with partial degradation, p95/p99 tail latency, overload and queue pressure, error-prone backends, recovery behavior, explainable routing decisions, reviewer/operator evidence, guardrails before autonomy, and trusted adaptive routing instead of black-box routing.

This ADR is planning-only. This does not implement Docker Compose files, scripts, fake nodes, k6 tests beyond the optional local-lab smoke skeleton, expanded Bruno collections beyond the optional local-lab skeleton, expanded Toxiproxy fault execution beyond the optional local-lab config skeleton, telemetry ingestion, replay, report generation, storage, export behavior, or runtime routing behavior yet.

This ADR does not add Docker Compose implementation. This ADR does not add scripts implementation. This ADR does not add fake backend node implementation. This ADR does not add expanded k6 scenario implementation. This ADR does not add expanded Bruno collection implementation. This ADR does not add Prometheus/Grafana implementation. This ADR does not add Toxiproxy implementation. This ADR does not change API behavior. This ADR does not change routing behavior. This ADR does not change scoring behavior. This ADR does not change strategy behavior. This ADR does not change proxy behavior. This ADR does not add config/resource/runtime changes. This ADR does not add Maven dependency changes. This ADR does not add CI changes.

PR #250 adds only a test-scope scenario model/catalog. It does not implement fake backend servers. It does not implement Docker Compose, k6, Bruno, Toxiproxy, Prometheus/Grafana, scripts, networking, or runtime behavior. It is a stepping stone toward future local lab tooling.

This PR adds test-scope response fixtures only. The fixtures describe future fake backend response expectations. It is not fake backend server implementation. It does not implement fake backend servers. It does not start listeners, open ports, call localhost, generate traffic, run Docker, k6, Bruno, Toxiproxy, Prometheus/Grafana, scripts, networking, or runtime behavior. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling, and this is not production proof.

This PR adds test-scope passive transcript fixtures only. Transcripts describe future request/response evidence expectations. Transcripts do not execute HTTP requests. Transcripts do not implement fake backend servers. Transcripts do not start listeners, open ports, call localhost, generate traffic, run replay, write reports, persist storage, or run tools. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling. This is still not production proof.

This PR adds a test-scope passive transcript summary renderer only. The renderer summarizes existing passive transcript fixtures in memory. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It does not implement fake backend servers. It does not start listeners, open ports, call localhost, generate traffic, or run tools. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling. This is still not production proof.

This PR adds a test-scope passive reviewer checklist mapper only. The mapper turns existing passive transcript summaries into in-memory reviewer checklist entries. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It does not implement fake backend servers. It does not start listeners, open ports, call localhost, generate traffic, or run tools. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling. This is still not production proof.

This PR adds a test-scope implementation readiness gate only. The readiness gate evaluates passive planning/test artifacts in memory. It does not implement fake backend servers. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It does not start listeners, open ports, call localhost, generate traffic, or run tools. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling. Fake backend execution remains future tooling only. Passing the readiness gate only means ready for a separately scoped implementation PR, not production proof.

This PR adds a test-scope fake backend handler only. The handler maps simulated request labels to existing response fixtures in memory. It does not implement fake backend servers. It does not start listeners. It does not open ports. It does not call localhost. It does not generate traffic. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It does not run tools. Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana remain future tooling. Passing handler tests is not production proof.

This PR adds a test-scope loopback fake backend server harness only. The harness lives under `src/test/java`. It binds to `127.0.0.1` only and uses OS-assigned ephemeral ports. It does not add production endpoints. It does not change production routing, proxy, scoring, strategy, or API behavior. It does not add Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing loopback tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven.

This PR adds test-scope lifecycle hardening for the loopback fake backend server only. The harness remains `src/test/java`-only. It binds to `127.0.0.1` only. It uses ephemeral ports. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not add Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing lifecycle tests is not production proof.

This PR adds a test-scope multi-backend loopback harness only. The harness remains `src/test/java`-only. It starts multiple loopback-only fake backend servers using OS-assigned ephemeral ports. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not add Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing multi-backend loopback tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven.

This PR adds test-scope multi-backend transcript alignment tests only. The alignment proves existing passive transcript expectations match loopback harness responses. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not add Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing alignment tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven.

This PR adds a test-scope deterministic loopback traffic smoke client and in-memory summary only. The client calls only the `src/test/java` multi-backend loopback harness on `127.0.0.1` using harness-assigned ephemeral ports. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not add Docker/k6/Bruno/Toxiproxy/Prometheus/Grafana. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing smoke client tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven.

This PR adds a test-scope traffic smoke reviewer checklist mapper and docs-only progress handoff only. The mapper turns existing in-memory loopback traffic smoke summaries into reviewer checklist entries. It does not call endpoints. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped. Passing smoke checklist tests is not production proof. See [`../LOCAL_LAB_PROGRESS_HANDOFF.md`](../LOCAL_LAB_PROGRESS_HANDOFF.md).

This PR adds deterministic test-scope traffic matrix tests and in-memory summaries only. The matrix uses the existing `src/test/java` multi-backend loopback harness. The matrix calls only `127.0.0.1` harness URLs with ephemeral ports. It is not k6, Bruno, Docker, Toxiproxy, load testing, stress testing, or production traffic. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing matrix tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven. See [`../LOCAL_LAB_PROGRESS_HANDOFF.md`](../LOCAL_LAB_PROGRESS_HANDOFF.md).

This PR adds a test-scope traffic matrix reviewer checklist mapper and handoff update only. The mapper turns existing in-memory traffic matrix summaries into reviewer checklist entries. It does not call endpoints. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It is not load testing or stress testing. Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped. Passing matrix checklist tests is not production proof. See [`../LOCAL_LAB_PROGRESS_HANDOFF.md`](../LOCAL_LAB_PROGRESS_HANDOFF.md).

This PR adds docs/test-only end-of-day handoff and next-step boundary cleanup only. It updates reviewer-facing handoff docs and documentation guard tests. It does not add local-lab harness functionality, client functionality, server functionality, production endpoints, production listeners, Docker/k6/Bruno/Toxiproxy implementation, replay execution, evidence/report generation, file writing, storage, export, or runtime behavior. See [`../LOCAL_LAB_PROGRESS_HANDOFF.md`](../LOCAL_LAB_PROGRESS_HANDOFF.md) and [`../LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](../LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

This PR adds deterministic test-scope bounded request burst smoke tests and in-memory summaries only. The burst uses the existing `src/test/java` multi-backend loopback harness. The burst calls only `127.0.0.1` harness URLs with ephemeral ports. The burst uses fixed small request counts only. It is not k6, Bruno, Docker, Toxiproxy, load testing, stress testing, benchmarking, or production traffic. It does not add production endpoints. It does not change production routing, proxy, scoring, or API behavior. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. Passing bounded burst tests is not production proof. Live-cloud, real-tenant, production certification, and runtime enforcement remain not proven. See [`../LOCAL_LAB_PROGRESS_HANDOFF.md`](../LOCAL_LAB_PROGRESS_HANDOFF.md) and [`../LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](../LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

This PR adds a test-scope bounded burst reviewer checklist mapper and handoff update only. The mapper turns existing in-memory bounded request burst summaries into reviewer checklist entries. It does not call endpoints. It does not execute replay. It does not generate evidence reports. It does not write files. It does not persist storage. It does not export/download/upload/PDF/ZIP anything. It is not load testing, stress testing, benchmarking, throughput evidence, or p95/p99 evidence. Docker/k6/Bruno/Toxiproxy remain future-only unless separately scoped. Passing bounded burst checklist tests is not production proof. See [`../LOCAL_LAB_PROGRESS_HANDOFF.md`](../LOCAL_LAB_PROGRESS_HANDOFF.md) and [`../LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](../LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

This PR adds a docs/test-only k6/Bruno/Toxiproxy implementation boundary plan only. The plan prepares future local-lab tooling lanes and reviewer stop conditions without adding tool files. It adds no k6 scripts, no Bruno collections, no Toxiproxy config, no Docker Compose files, no scripts, no CI jobs, no Maven dependencies, no production endpoints, no production listeners, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence. k6, Bruno, Toxiproxy, and Docker/Docker Compose remain future-only unless separately scoped. See [`../LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md`](../LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md).

PR #270 added docs/test-only future tool scenario design specs. See [`../LOCAL_LAB_K6_SCENARIO_DESIGN.md`](../LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`../LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](../LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`../LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](../LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). Future tool work must be separately scoped and must target local/lab endpoints first. The specs added no k6 scripts, no Bruno collections, no Toxiproxy config, no Docker Compose files, no scripts, no CI jobs, no Maven dependencies, no production endpoints, no production listeners, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark testing, no throughput evidence, and no p95/p99 evidence.

This sprint adds one optional local-lab k6 smoke script skeleton at [`../../lab/k6/local-lab-smoke.js`](../../lab/k6/local-lab-smoke.js). See [`../LOCAL_LAB_K6_SMOKE_SCRIPT.md`](../LOCAL_LAB_K6_SMOKE_SCRIPT.md), [`../LOCAL_LAB_K6_SCENARIO_DESIGN.md`](../LOCAL_LAB_K6_SCENARIO_DESIGN.md), [`../LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](../LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), and [`../LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](../LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). It is not CI-gated, not Dockerized, and must target local/lab-owned loopback endpoints by default. It does not add Bruno execution, Toxiproxy execution, Docker Compose files, automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This sprint adds one optional local-lab Bruno collection skeleton at [`../../lab/bruno/local-lab-smoke/`](../../lab/bruno/local-lab-smoke/). See [`../LOCAL_LAB_BRUNO_COLLECTION.md`](../LOCAL_LAB_BRUNO_COLLECTION.md), [`../LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md`](../LOCAL_LAB_BRUNO_COLLECTION_DESIGN.md), [`../LOCAL_LAB_K6_SMOKE_SCRIPT.md`](../LOCAL_LAB_K6_SMOKE_SCRIPT.md), and [`../LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](../LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md). It is optional and manual, not CI-gated, not Dockerized, not Toxiproxy integration, not k6 execution, and must target local/lab-owned loopback endpoints by default. It does not add Toxiproxy execution, Docker Compose files, automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This sprint adds one optional local-lab Toxiproxy config skeleton at [`../../lab/toxiproxy/local-lab-toxiproxy.json`](../../lab/toxiproxy/local-lab-toxiproxy.json). See [`../LOCAL_LAB_TOXIPROXY_CONFIG.md`](../LOCAL_LAB_TOXIPROXY_CONFIG.md), [`../LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](../LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md), [`../LOCAL_LAB_K6_SMOKE_SCRIPT.md`](../LOCAL_LAB_K6_SMOKE_SCRIPT.md), and [`../LOCAL_LAB_BRUNO_COLLECTION.md`](../LOCAL_LAB_BRUNO_COLLECTION.md). It is optional, manual-only, not CI-gated, not Dockerized, not Docker Compose orchestration, not wired into the application, not wired into Maven, not wired into k6 execution, not wired into Bruno execution, and must target local/lab-owned loopback endpoints by default. It does not start Toxiproxy, does not start the application, and does not add automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`../LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](../LOCAL_LAB_MANUAL_TOOLING_INDEX.md) as the unified manual tooling reviewer index for the optional k6 smoke script skeleton, optional Bruno collection skeleton, and optional Toxiproxy config skeleton. It adds no tool behavior, automated execution, CI-gating, Docker/Compose orchestration, Maven wiring, production runtime wiring, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`../LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](../LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md) as the companion manual reviewer/operator runbook for inspection-only review and optional local-only manual use of the existing optional k6, Bruno, and Toxiproxy skeletons. It adds no tool behavior, automated execution, CI-gating, Docker/Compose orchestration, Maven wiring, production runtime wiring, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`../LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](../LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md) as future-only Docker Compose boundary design for possible local-lab orchestration. It adds no Compose files, Dockerfiles, compose profiles, automated execution, CI-gating, Maven wiring, production runtime wiring, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark evidence, throughput evidence, or p95/p99 evidence.

This sprint adds one optional local-lab Docker Compose skeleton at [`../../lab/docker-compose/local-lab-compose.yml`](../../lab/docker-compose/local-lab-compose.yml). See [`../LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](../LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md), [`../LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](../LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md), [`../LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](../LOCAL_LAB_MANUAL_TOOLING_INDEX.md), [`../LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](../LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md), and [`../LOCAL_LAB_TOXIPROXY_CONFIG.md`](../LOCAL_LAB_TOXIPROXY_CONFIG.md). It is optional, manual-only, local-lab-only, loopback-bound, not CI-gated, not wired into Maven, not production Docker packaging, not production runtime behavior, not wired into k6 execution, not wired into Bruno execution, and not wired into automated execution. It does not add app container orchestration, k6 runner services, Bruno runner services, automatic execution, CI jobs, Maven dependencies, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage/export behavior, load/stress/benchmark testing, throughput evidence, or p95/p99 evidence.

This docs/test-only follow-up adds [`../LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](../LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md) as the Compose-specific manual reviewer/operator runbook and checklist for the optional local-lab Compose skeleton. It adds no new Compose services, no app service, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no production Docker packaging, no production runtime behavior, no automated execution, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark evidence, no throughput evidence, and no p95/p99 evidence.

This docs/test-only follow-up adds [`../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md) as future-only app-service boundary design for the optional local-lab Compose lane. It adds no app service, no new Compose services, no Compose behavior changes, no Docker packaging changes, no CI-gating, no Maven wiring, no production runtime behavior, no automated execution, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark evidence, no throughput evidence, and no p95/p99 evidence.

This docs/test-only follow-up adds [`../LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](../LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md) as the readiness gate for future local-lab Compose file or service changes. It adds no Compose behavior changes, no app service, no new Compose services, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no production Docker packaging, no production runtime behavior, no automated execution, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark evidence, no throughput evidence, and no p95/p99 evidence.

This docs/test-only follow-up adds [`../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md) as the exact preflight checklist before any future app-service Compose PR. It adds no app service, no Compose behavior changes, no Docker packaging changes, no CI-gating, no Maven wiring, no production runtime behavior, no automated execution, no new Compose services, no k6 runner service, no Bruno runner service, no replay execution, no evidence/report generation, no storage/export behavior, no load/stress/benchmark evidence, no throughput evidence, and no p95/p99 evidence.

This docs/test-only post-app-service Compose handoff update summarizes the merged local-lab tooling and Compose guardrail chain in [`../LOCAL_LAB_PROGRESS_HANDOFF.md`](../LOCAL_LAB_PROGRESS_HANDOFF.md) and [`../LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](../LOCAL_LAB_NEXT_STEPS_BOUNDARY.md). It records that the current Compose skeleton now contains the existing Toxiproxy service plus one optional/manual/local-lab-only `app-under-test` service, and that future Compose changes still have to be separately scoped after the readiness gate and preflight checklist are reused. It adds no CI/Maven wiring, no production runtime behavior, and no replay/evidence/report/storage/export behavior.

This gated app-service follow-up adds [`../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md) after applying the readiness gate and preflight checklist. The current Compose skeleton now contains the existing Toxiproxy service plus one optional/manual/local-only app-under-test service that uses a public JRE image, a read-only local `target/` mount, and `127.0.0.1` published ports only. It adds no Dockerfile changes, no production Compose changes, no CI/Maven wiring, no k6 runner service, no Bruno runner service, no production Docker packaging, no production runtime behavior, and no replay/evidence/report/storage/export behavior.

The app-service manual smoke checklist in [`../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md) documents inspection-only checks and optional manual local-only commands for the existing `app-under-test` service. It is docs/test-only and changes no Compose behavior, app behavior, Dockerfiles, Maven, CI, k6, Bruno, Toxiproxy, production Compose, production Docker packaging, or production runtime behavior.

## Decision

If a future implementation sprint is separately approved, LoadBalancerPro should grow a Local Lab Kit that can simulate a small datacenter on one Windows machine before real server hardware is purchased.

The proposed decision is documentation guidance only:

- the Local Lab Kit should begin as local, repeatable, low-risk simulation;
- the simulated datacenter should model backend differences beyond simple healthy/unhealthy status;
- future scenarios should expose latency, error, load, queue, policy, safety-mode, and decision evidence;
- future evidence should distinguish what was proven from what was not proven;
- future local simulation should help reviewers and operators reason about adaptive routing before any hardware expansion;
- future local and LAN hardware tests should remain controlled lab work unless separately validated;
- guardrails, explainability, policy checks, and not-proven boundaries should come before autonomy.

This ADR does not approve implementation. It defines a planning boundary for future lab work.

## Local Lab Kit Purpose

The future Local Lab Kit should:

- provide a repeatable local testing environment before real server hardware is purchased;
- simulate a small datacenter on one Windows machine;
- support future expansion to LAN server hardware and an Acer AI mini machine;
- test partial degradation, p95/p99 tail latency, overload and queue pressure, error-prone backends, and recovery behavior;
- generate reviewer/operator evidence expectations without claiming production validation;
- support learning, research, and controlled experiments without unsafe autonomous behavior;
- make future routing decisions explainable through selected candidates, rejected candidates, signals, policy gates, safety mode, and not-proven boundaries.

The Local Lab Kit is not production validation. It is a controlled evidence-driven lab plan for safer learning before broader hardware or live-environment work.

## Planned Conceptual Topology

The future simulated datacenter topology is conceptual only:

| Planned component | Future role | Current boundary |
| --- | --- | --- |
| LoadBalancerPro running locally | Local app under review, with future lab scenarios calling local APIs or proxy paths where separately approved. | No runtime behavior change in this ADR. |
| Fake backend node 1: healthy/fast | Stable baseline backend with low latency and low error rate. | No fake backend node implementation in this ADR. |
| Fake backend node 2: slow/tail-latency heavy | Backend with elevated p95/p99 tail latency and occasional slow responses. | No fake backend node implementation in this ADR. |
| Fake backend node 3: partial degradation | Backend with mixed health, intermittent slowness, or degraded but not fully down behavior. | No fake backend node implementation in this ADR. |
| Fake backend node 4: error-prone | Backend with intermittent 500 errors or controlled error bursts. | No fake backend node implementation in this ADR. |
| Fake backend node 5: overloaded/queue-depth simulated | Backend with overload and queue-depth simulation signals. | No fake backend node implementation in this ADR. |
| Optional Toxiproxy network degradation layer | Current tiny optional local-lab Toxiproxy config skeleton plus future latency, timeout, connection reset, and network damage scenarios. | Manual local/lab-owned loopback endpoints only; no expanded Toxiproxy fault execution in this ADR. |
| Optional local-lab Docker Compose skeleton | Current tiny optional manual loopback-bound Compose skeleton for the existing local Toxiproxy config. | Manual local/lab-owned loopback ports only; no app container orchestration, no k6 runner service, no Bruno runner service, no CI execution, no Maven execution, and no production Docker packaging in this ADR. |
| Optional k6 traffic generator | Future smoke, load, stress, spike, and tail-latency tests. | No k6 scenario files in this ADR. |
| Optional Bruno API checks | Current tiny optional local-lab Bruno collection skeleton plus future expanded API collection checks for local lab workflows. | Manual local/lab-owned loopback endpoints only; no expanded Bruno collection in this ADR. |
| Optional Prometheus/Grafana observability later | Future metrics visualization for lab-only signals and local dashboards. | No Prometheus/Grafana implementation in this ADR. |
| Optional future LAN server | Future backend host for controlled private lab experiments. | Not production infrastructure. |
| Optional future Acer AI mini machine | Future local lab, coding, research, local inference, and telemetry experiment support. | Not production infrastructure. |

The Acer AI mini machine is documented only as future local lab/coding/research/local inference/telemetry support, not as production infrastructure.

## Tooling Boundary

The future tooling roles are planned only:

- Docker Desktop and Docker Compose could run local containerized services for LoadBalancerPro-adjacent fake backend nodes, degradation tools, and observability tools.
- k6 could run smoke, load, stress, spike, and tail-latency tests against local endpoints and fake backend topologies.
- Bruno could store local API collection checks for repeatable reviewer/operator exercises.
- WireMock or lightweight fake services could provide controlled backend behavior without needing real servers.
- Toxiproxy could provide latency, timeout, bandwidth, reset, and network damage scenarios.
- Prometheus/Grafana could later visualize local lab metrics and scenario state.
- Java/Maven remains the build and local run path for LoadBalancerPro.
- Windows PowerShell should be the preferred local command shell for examples.

These are future/planned roles, not production implementation files in this sprint. ADR-0009 does not add Docker Compose files, scripts, fake backend nodes, expanded k6 files, expanded Bruno collections beyond the optional local-lab skeleton, expanded Toxiproxy fault execution beyond the optional local-lab config skeleton, WireMock fixtures, Prometheus/Grafana dashboards, Maven dependencies, CI jobs, or runtime configuration.

## Scenario Categories

Future Local Lab Kit scenarios should be grouped into reviewer-readable categories:

- healthy baseline;
- slow backend / tail latency;
- partial degradation;
- intermittent 500 errors;
- overload / queue pressure;
- all-unhealthy or no-good-choice scenario;
- recovery scenario;
- strategy comparison scenario;
- evidence completeness scenario;
- safety-mode boundary scenario;
- local hardware expansion scenario;
- operator-review scenario.

These scenario categories are planning labels only. They do not add workload generation, trace import, replay execution, routing/scoring/strategy/proxy/API behavior changes, or runtime evidence generation.

## Evidence Expectations

Future local lab scenarios should eventually show:

- selected backend;
- rejected backends;
- signals used;
- latency/error/load observations;
- p95/p99 tail latency observations where the future scenario measures or simulates them;
- queue pressure or overload observations where the future scenario models them;
- policy gate status;
- safety mode;
- what changed during the scenario;
- what was proven;
- what was not proven;
- why local simulation is not production proof;
- why evidence completeness matters before autonomy.

Evidence completeness matters because trusted adaptive routing needs more than "the system picked a backend." Reviewers and operators should be able to see the considered options, the rejected options, the signal freshness/trust boundary, the policy gate posture, the safety mode, the risk boundary, and the not-proven limits.

ADR-0009 does not implement `EvidencePacket`, `EvidenceAssembler`, replay execution, evidence/report generation, storage/persistence, filesystem-writing behavior, export/upload/download/PDF/ZIP behavior, reviewer portal/dashboard/API behavior, or telemetry ingestion.

## Hardware Expansion Path

The staged hardware expansion path is:

1. Current Windows PC runs LoadBalancerPro and test tools.
2. Same PC simulates backends in containers.
3. Future LAN server can host backend nodes.
4. Future Acer AI mini machine can support coding, research, local inference, and telemetry experiments.
5. Additional machines can become degraded or overloaded nodes.
6. Real hardware tests still do not equal production certification.
7. Live-cloud and real-tenant validation remain separate future gates.

The hardware path remains controlled local lab work. LAN/server/Acer AI hardware experiments are useful learning evidence, but they are not production certification, not live-cloud validation, not real-tenant validation, and not a production deployment approval.

## Relationship To Prior ADRs

ADR-0009 depends on the prior ADR set:

- ADR-0001 architecture boundary: the Local Lab Kit needs layered vocabulary so lab tooling, fake backends, evidence, LASE/adaptive decision logic, and runtime routing authority stay separable.
- ADR-0002 LASE boundary: local lab scenarios should support future LASE observation and shadow/recommendation evidence without granting hidden live allocation authority.
- ADR-0003 external signal/source boundary: local lab evidence should identify synthetic, observed, stale, unavailable, and externally sourced signals without turning provenance into runtime authority.
- ADR-0004 workload realism/scenario modeling: the Local Lab Kit is a future place to exercise workload realism, partial degradation, tail latency, overload, and recovery scenarios before workload generators or trace import exist.
- ADR-0005 safety boundaries and guardrails: lab scenarios should respect observe-only, recommendation, shadow, active-experiment, manual promotion, rollback, stop-condition, and blast-radius boundaries before autonomy.
- ADR-0006 evidence packet and replay boundary model: local lab evidence should prepare for future EvidencePacket, EvidenceAssembler, and replay-facing evidence without adding replay execution, report generation, storage, or export behavior.
- ADR-0007 reviewer evidence and trust model: the Local Lab Kit should make selected options, rejected options, signals, policy checks, safety modes, uncertainty, and not-proven boundaries reviewer-readable.
- ADR-0008 runtime enforcement and package boundary plan: future lab implementation should remain separate from package moves, ArchUnit enforcement, runtime enforcement, source-name guard implementation, and routing/scoring/strategy/proxy/API behavior changes unless separately approved.

These relationships are planning guidance only. They do not approve implementation.

## Relationship To Existing Boundary Docs

Related docs:

- [`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) names ADR-0009 as a proposed Phase 0/local-lab planning ADR.
- [`../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md) maps architecture report testing, validation, and north-star themes to current docs and future-only boundaries.
- [`../REVIEWER_TRUST_MAP.md`](../REVIEWER_TRUST_MAP.md) provides reviewer navigation and evidence boundaries.
- [`../ENTERPRISE_READINESS_AUDIT.md`](../ENTERPRISE_READINESS_AUDIT.md) records readiness posture and not-proven boundaries.
- [`../LOCAL_LAB_SCENARIO_MATRIX.md`](../LOCAL_LAB_SCENARIO_MATRIX.md) lists future scenario categories as planning-only reviewer/operator scaffolding.
- [`../LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](../LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md) documents the first optional manual local-lab Compose skeleton and its loopback/non-production boundaries.
- [`../LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](../LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md) documents the Compose-specific inspection checklist and optional manual local-only commands without adding services, CI, Maven wiring, production Docker packaging, production runtime behavior, or automated execution.
- [`../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md) documents future app-service prerequisites and stop conditions without adding an app service, new Compose services, CI, Maven wiring, Docker packaging changes, production runtime behavior, or automated execution.
- [`../LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](../LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md) documents the readiness gate future PRs must satisfy before changing the Compose file, adding services, adding CI wiring, or adding Maven wiring.
- [`../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md`](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md) documents the exact preflight proof reviewers require before any future app-service Compose PR without adding an app service, changing Compose behavior, changing Docker packaging, adding CI wiring, or adding Maven wiring.
- [`../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md) documents the first gated optional local-lab app service, manual package-first operation, read-only local `target/` mount, loopback-only published app port, and non-production boundaries.
- [`../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md) documents the optional manual app-service smoke checklist, inspection-only path, manual package prerequisite, local-only Compose commands, and non-production boundaries.
- [`../LOCAL_LAB_PROGRESS_HANDOFF.md`](../LOCAL_LAB_PROGRESS_HANDOFF.md) and [`../LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](../LOCAL_LAB_NEXT_STEPS_BOUNDARY.md) now carry the end-of-day Compose handoff that summarizes the merged guardrail chain through the app-service preflight checklist without adding Compose behavior, services, CI wiring, Maven wiring, production Docker packaging, production runtime behavior, or evidence behavior.
- [`ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) defines proposed future layer boundaries.
- [`ADR-0002_LASE_INTEGRATION_MODEL.md`](ADR-0002_LASE_INTEGRATION_MODEL.md) defines proposed future LASE integration boundaries.
- [`ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md`](ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md) defines evidence as proposed future architecture material.
- [`ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md`](ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md) defines future workload and scenario modeling boundaries.
- [`ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md`](ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md) defines future safety boundaries and guardrails.
- [`ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md`](ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md) defines future evidence packet and replay boundaries.
- [`ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md`](ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md) defines reviewer evidence and trust boundaries.
- [`ADR-0008_RUNTIME_ENFORCEMENT_AND_PACKAGE_BOUNDARY_PLAN.md`](ADR-0008_RUNTIME_ENFORCEMENT_AND_PACKAGE_BOUNDARY_PLAN.md) defines runtime enforcement and package-boundary planning.
- [`../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md) defines the future LASE/live allocation boundary.
- [`../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md`](../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md) defines future WorkloadProfile signal metadata.
- [`../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md`](../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md) defines future read-only external signal context.

ADR-0009 does not supersede those docs. It gives reviewers and operators a single place to inspect the future local lab kit plan before implementation exists.

## Relationship To The North-Star Vision

ADR-0009 ties the Local Lab Kit to the north-star vision:

- future datacenter adaptive traffic control needs lab scenarios that model partial degradation instead of simple up/down health;
- p95/p99 tail latency under pressure should be visible before routing decisions become more adaptive;
- overload and queue pressure should be visible as separate signals, not hidden side effects;
- safer adaptive routing should progress through observe-only, recommendation, shadow, and active-experiment modes before broader authority;
- explainability and auditability should show structured evidence, signals, policy checks, rejected options, risk boundaries, and what was not proven;
- guardrails before autonomy should be testable in local and later LAN lab environments;
- trusted adaptive routing should avoid black-box routing by making local simulation results reviewable;
- building a local evidence-driven lab before real hardware expansion reduces the risk of buying hardware before scenario and evidence expectations are clear.

This north-star relationship is planning guidance. It does not claim production readiness, production certification, live-cloud validation, real-tenant validation, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, or facility automation.

## Safety And Non-Goals

This ADR is documentation and documentation guard tests only.

Non-goals:

- no production Java runtime behavior;
- no records/classes/interfaces/enums under `src/main/java`;
- no Docker Compose lab implementation;
- no Docker Compose files;
- no scripts implementation;
- no fake backend node implementation;
- no k6 scenario files;
- no expanded Bruno collections beyond the optional local-lab skeleton;
- no Prometheus/Grafana dashboards;
- no expanded Toxiproxy fault execution beyond the optional local-lab config skeleton;
- no WireMock fixtures;
- no telemetry ingestion;
- no observability implementation;
- no runtime LASE enforcement;
- no package-boundary enforcement;
- no ArchUnit dependency;
- no ArchUnit enforcement;
- no source-name guard implementation;
- no package moves or renames;
- no config/resource/runtime changes;
- no Maven dependency changes;
- no Docker, CI, release, signing, registry, or governance changes;
- no routing behavior change;
- no scoring behavior change;
- no strategy behavior change;
- no proxy behavior change;
- no API behavior change;
- no reviewer portal implementation;
- no reviewer dashboard implementation;
- no reviewer API implementation;
- no EvidencePacket implementation;
- no EvidenceAssembler implementation;
- no replay execution;
- no evidence generation;
- no report generation;
- no storage or persistence;
- no filesystem-writing implementation;
- no export, upload, download, PDF, or ZIP behavior;
- no workload generation;
- no trace import;
- no external signal ingestion;
- no autonomous production traffic shifting;
- no carbon-aware routing;
- no GPU orchestration;
- no power/grid control;
- no facility automation.

## Not-Proven Boundaries

ADR-0009 does not implement or prove:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- Docker Compose lab implementation;
- fake backend nodes;
- k6 scenario files;
- expanded Bruno collections beyond the optional local-lab skeleton;
- Prometheus/Grafana dashboards;
- expanded Toxiproxy fault execution beyond the optional local-lab config skeleton;
- runtime LASE enforcement;
- package-boundary enforcement;
- ArchUnit enforcement;
- source-name guard implementation;
- routing/scoring/strategy/proxy behavior changes;
- API behavior changes;
- reviewer portal/dashboard/API behavior;
- EvidencePacket implementation;
- EvidenceAssembler implementation;
- replay execution;
- evidence/report generation;
- storage/persistence;
- filesystem-writing/export behavior;
- workload generation;
- trace import;
- external signal ingestion;
- autonomous production traffic shifting;
- carbon-aware routing;
- GPU orchestration;
- power/grid control;
- facility automation;
- local simulation as production proof;
- LAN hardware tests as production proof;
- Acer AI mini machine use as production infrastructure;
- live-cloud or real-tenant validation;
- ADR-0009 approval beyond proposed/planning-only status.

Local simulation is useful evidence. Local simulation is not production certification.

## Consequences

Positive consequences:

- reviewers get a single proposed ADR for the future Local Lab Kit and simulated datacenter test harness;
- local lab planning is tied to prior architecture, safety, evidence, workload, reviewer, and enforcement ADRs;
- future Docker Compose, k6, Bruno, Toxiproxy, fake backend, and observability work stays separately reviewable;
- hardware purchases can be staged behind clearer scenario and evidence expectations;
- the north-star adaptive routing vision gets a practical local evidence path before broader hardware work.

Costs and risks:

- this ADR adds documentation to maintain;
- future implementers may overread the topology as implementation unless planning-only language remains explicit;
- local simulation can create false confidence if reviewers forget that local lab evidence is not production proof;
- future hardware expansion still requires separate safety, networking, observability, and evidence gates.

## Reviewer-Facing Value

ADR-0009 helps reviewers answer:

- what future Local Lab Kit should exist before real hardware is purchased;
- how the simulated datacenter should model healthy, slow, degraded, error-prone, overloaded, and recovering backends;
- which future tools might support local lab experiments;
- which evidence a future scenario should expose;
- why local simulation is useful but not production proof;
- how LAN/server/Acer AI hardware expansion remains controlled lab work;
- how local lab planning supports trusted adaptive routing instead of black-box routing.

Reviewers should treat ADR-0009 as proposed/planning-only. It is not Docker Compose implementation, not scripts implementation, not fake backend node implementation, not expanded k6 scenario implementation, not expanded Bruno collection implementation, not automatic Bruno execution, not automatic Toxiproxy execution, not expanded Toxiproxy fault execution, not Toxiproxy platform implementation, not Prometheus/Grafana implementation, not runtime LASE enforcement, not package-boundary enforcement, not routing/scoring/strategy/proxy/API behavior change, not replay execution, not evidence/report generation, not storage/persistence, not filesystem-writing/export behavior, not production-readiness proof, and not production-certification proof.

## Post-App-Service Compose Handoff Update

PR #284 is now the current local-lab Compose baseline. This ADR handoff update is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, or production runtime behavior.

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

Use [../LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md](../LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md), [../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_PREFLIGHT_CHECKLIST.md), and [../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md) before considering any later Compose change.

## App-Service Manual Smoke Checklist Update

The app-service manual smoke checklist is now available in [`../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md). This is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, app behavior, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, or production runtime behavior.

It records that `app-under-test` already exists in local-lab Compose, remains optional/manual/local-lab-only, uses the local `target/` mount read-only, requires a manual package step before optional Compose use, publishes `127.0.0.1:8080:8080`, keeps Toxiproxy present, keeps k6 manual and separate, keeps Bruno manual and separate, has no k6 runner service, has no Bruno runner service, has no CI-gating, has no Maven wiring, has no Dockerfile change, has no production Docker packaging, has no production Compose change, and has no production runtime behavior change.

The checklist does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.

## App-Service Health/Readiness Documentation Update

The app-service health/readiness documentation lane is now available in [`../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md`](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md). It is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, app behavior, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, production runtime behavior, or application endpoints. It adds no health endpoint and no readiness endpoint.

This ADR continues to treat the lane as local-lab reviewer context only. It preserves optional/manual/local-lab-only scope, manual package-first operation, read-only `target/` mounting, `127.0.0.1:8080:8080`, Toxiproxy presence, k6 and Bruno as manual separate tools, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no production runtime behavior change.

The lane does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.

## App-Service Runbook Update

The app-service runbook is now available in [`../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md`](../LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_RUNBOOK.md). It is documentation only and acts as a reviewer runbook/refinement that ties together the app-service skeleton, the manual smoke checklist, and the health/readiness lane.

The runbook adds no Compose behavior changes, no app behavior changes, no endpoint changes, no health endpoint, no readiness endpoint, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no production runtime behavior change.

This ADR continues to treat the runbook as local-lab reviewer context only. It preserves optional/manual/local-lab-only scope, manual package-first operation, read-only `target/` mounting, `127.0.0.1:8080:8080`, Toxiproxy presence, k6 and Bruno as manual separate tools, no k6 runner service, no Bruno runner service, and manual/local-only health/readiness observations. It does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.

## Runner-Service Gate Update

The runner-service gate is now available in [`../LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md`](../LOCAL_LAB_DOCKER_COMPOSE_RUNNER_SERVICE_GATE.md). It is documentation only and a future gate for runner services. It adds no k6 runner service, no Bruno runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, and no production Compose change.

k6 remains manual and separate. Bruno remains manual and separate. The app-service runbook remains the reviewer path. Any future k6 runner PR must be separately scoped. Any future Bruno runner PR must be separately scoped. Future runner services must stay local-lab-only and loopback/local-targeted, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.

## k6 Runner Service Design Gate Update

The k6 runner service design gate is now available in [`../LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md`](../LOCAL_LAB_DOCKER_COMPOSE_K6_RUNNER_SERVICE_DESIGN_GATE.md). It is documentation only and a future design gate for a k6 Compose runner service. It adds no k6 runner service, no Bruno runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no automated execution.

k6 remains manual and separate. Bruno remains manual and separate. The app-service runbook remains reviewer path. Any future k6 runner PR must be separately scoped, must stay local-lab-only, must target only loopback/local services, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.

## Bruno Runner Service Design Gate Update

The Bruno runner service design gate is now available in [`../LOCAL_LAB_DOCKER_COMPOSE_BRUNO_RUNNER_SERVICE_DESIGN_GATE.md`](../LOCAL_LAB_DOCKER_COMPOSE_BRUNO_RUNNER_SERVICE_DESIGN_GATE.md). It is documentation only and a future design gate for a Bruno Compose runner service. It adds no Bruno runner service, no k6 runner service, no Compose behavior changes, no app behavior changes, no endpoint changes, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no automated execution.

Bruno remains manual and separate. k6 remains manual and separate. The app-service runbook remains reviewer path. Any future Bruno runner PR must be separately scoped, must stay local-lab-only, must target only loopback/local services, must not target production/cloud/tenant/external endpoints, must not introduce secrets/credentials, must not imply production API validation, must not imply load/stress/benchmark evidence, must not claim throughput/p95/p99 evidence, must not claim production readiness/certification, must not claim live-cloud or real-tenant validation, must not claim runtime enforcement, and must not claim replay/evidence/report/storage/export behavior.
