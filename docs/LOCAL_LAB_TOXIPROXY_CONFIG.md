# Local Lab Toxiproxy Config

This document describes the first separately scoped optional local-lab Toxiproxy config skeleton: [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json).

The config is local-lab-only tooling. It is optional. It is manual-only. It is not CI-gated. It is not Dockerized. It is not Docker Compose orchestration. It is not wired into the application. It is not wired into Maven. It is not wired into k6 execution. It is not wired into Bruno execution. It does not start Toxiproxy. It does not start the application. It does not prove runtime enforcement. It is not a benchmark. It is not a stress test. It is not a load test. It does not prove throughput, p95, p99, production readiness, production certification, live-cloud validation, or real-tenant validation.

The config defaults to loopback-only listen and upstream addresses. It uses `127.0.0.1` for every host value and must not bind to `0.0.0.0`. Any host or port value must stay local/lab-owned and non-production. Non-loopback targets, real cloud/provider endpoints, private customer networks, and production-looking domains require a separate review and must not be treated as default behavior.

## Relationship To Existing Tooling

This Toxiproxy skeleton is the first narrow config step after the docs/test-only design phase, the optional k6 smoke script, and the optional Bruno collection:

- [`LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md`](LOCAL_LAB_TOXIPROXY_FAULT_MODEL_DESIGN.md) defines future Toxiproxy fault-model categories and now points at this first optional local-lab Toxiproxy config skeleton.
- [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md) describes the separate optional k6 smoke script. The Toxiproxy config does not run k6, and the k6 script does not run Toxiproxy.
- [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md) describes the separate optional Bruno collection. The Toxiproxy config does not run Bruno, and the Bruno collection does not run Toxiproxy.
- [`LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md`](LOCAL_LAB_K6_BRUNO_TOXIPROXY_BOUNDARY_PLAN.md) tracks the local-lab tool lane stop conditions.
- [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md) is the unified docs/test-only reviewer index for the current k6, Bruno, and Toxiproxy manual tool skeletons.
- [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md) is the companion manual reviewer/operator runbook for inspection-only review and optional local-only manual use.
- [`LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_BOUNDARY_DESIGN.md) is future-only Docker Compose boundary design. It does not add Docker Compose, Dockerfiles, CI automation, Maven wiring, runtime behavior, Toxiproxy startup, or automatic Toxiproxy execution.
- [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md) documents the separate optional local-lab Compose skeleton that may mount this config read-only when a reviewer manually runs Docker Compose. The config itself remains not Dockerized, not Docker Compose orchestration, not wired into Maven, not wired into the application, and not automatic Toxiproxy execution.
- [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md) gives the Compose-specific inspection checklist and optional manual local-only commands. It does not change this config, add Toxiproxy execution, add new services, wire Compose into CI or Maven, or broaden targets beyond loopback/local use.

This sprint does not add Docker, Docker Compose, automatic execution, CI workflow changes, Maven dependency changes, production endpoints, production listeners, production routing/scoring/strategy/proxy/API behavior, replay execution, evidence/report generation, storage, export, upload, download, PDF, or ZIP behavior.

## Manual Use Boundary

The file is a static JSON skeleton only. It does not launch a Toxiproxy process, create proxies, enable faults, start the application, or call endpoints. If a reviewer later uses it manually, Toxiproxy and the local app or local backend must already be started by the reviewer outside Maven and CI.

The config includes tiny loopback-only proxy placeholders:

- `local-lab-app-loopback`: listens on `127.0.0.1:18080` and points at a locally owned app port `127.0.0.1:8080`.
- `local-lab-backend-loopback`: listens on `127.0.0.1:18081` and points at a locally owned backend placeholder port `127.0.0.1:18082`.

The `toxics` arrays are empty. Any latency, timeout, reset, bandwidth, or other fault entry must be separately reviewed before it becomes part of a future local-lab lane. This skeleton is not throughput evidence, not p95 evidence, not p99 evidence, not production proof, not live-cloud proof, and not real-tenant proof.

## Reviewer Stop Conditions

Stop before merge or use if:

- a listen or upstream host is changed away from `127.0.0.1` or `localhost`;
- the config binds to `0.0.0.0`;
- an external host, production-looking domain, cloud host, private customer host, or real tenant target appears;
- Toxiproxy startup, app startup, k6 execution, Bruno execution, Maven execution, CI execution, Docker, or Docker Compose is added;
- production endpoint wiring, production listeners, or production runtime behavior changes are required;
- documentation starts claiming runtime enforcement, benchmark, stress, load, throughput, p95, p99, production readiness, production certification, live-cloud validation, or real-tenant validation evidence.

## Remaining Not-Proven Boundaries

The following remain not proven: production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, Docker/k6/Bruno/Toxiproxy platform implementation, Docker Compose implementation, Toxiproxy execution, expanded Toxiproxy fault execution, expanded k6 scenario implementation, expanded Bruno collection implementation, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, p95 evidence, p99 evidence, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, and facility automation.
