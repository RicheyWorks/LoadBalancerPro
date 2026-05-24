# Local Lab Docker Compose App Service Preflight Checklist

This docs/test-only page is a preflight checklist for a future app-service PR in the optional local-lab Docker Compose lane. It defines the exact preflight proof reviewers should require before any future PR adds an `app-under-test` service to [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml).

This PR does not add an app service. No app service is added. It does not change Compose. It makes no Compose behavior changes. It does not change Docker packaging. It makes no Docker packaging changes. It does not add CI-gating. It does not add Maven wiring. It does not wire Compose into CI or Maven. It does not change production runtime behavior.

It adds no throughput evidence, no p95/p99 evidence, and no load/stress/benchmark evidence. It adds no replay execution, evidence/report generation, storage, or export behavior.

The current Compose skeleton remains Toxiproxy-only. The k6 smoke script remains manual and separate. The Bruno collection remains manual and separate. Toxiproxy remains manual/local-only. This preflight checklist complements [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md), [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md), [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md), and [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).

The end-of-day Compose handoff in [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md) summarizes the current guardrail chain through this checklist. It does not add an app service, does not change Compose behavior, does not change Docker packaging, does not add CI-gating, and does not add Maven wiring.

## Purpose

- Provide a preflight checklist for a future app-service Compose PR.
- Keep this sprint documentation-only and test-only.
- Keep the current Compose skeleton behavior unchanged.
- Keep app service, k6 runner service, Bruno runner service, and new Compose services out of this PR.
- Keep Dockerfiles, production Docker packaging, CI-gating, Maven wiring, automated execution, registry login/push, production runtime behavior, and production Compose profiles out of this PR.

## Required Proof Before Future App Service

A future app-service PR must prove:

- future app-service PR must be separately scoped;
- separately scoped PR;
- local-lab-only service name;
- local-only build/run story;
- no production Dockerfile mutation unless separately reviewed;
- no production Compose profile;
- no registry push/pull requirement beyond public base images already accepted by repo policy;
- no Docker image publishing, registry login, or registry push;
- no secrets or credentials;
- no secrets/credentials;
- no external/cloud/tenant/private-network targets;
- no production/cloud/tenant/private-network/external targets;
- loopback-only ports for any published ports;
- loopback-only published ports if any;
- no `0.0.0.0` default exposure;
- no 0.0.0.0 default exposure;
- no CI execution unless separately scoped;
- no CI-gating unless separately scoped and reviewed;
- no Maven wiring unless separately scoped;
- documented startup command;
- documented health/readiness expectation;
- documented shutdown path;
- documented relationship to the current Toxiproxy-only Compose skeleton;
- documented stop conditions.

## Required Local-Lab Service Boundaries

Before a future app service can be reviewed, the PR must preserve these boundaries:

- app service must not imply production deployment;
- app service must not imply production Docker packaging;
- app service must not change production API behavior;
- app service must not alter routing/scoring/proxy behavior;
- app service must not add replay/report/storage/export behavior;
- app service must not create evidence artifacts;
- app service must not claim runtime enforcement;
- app service must not add k6 runner service behavior or Bruno runner service behavior;
- app service must not make Toxiproxy non-local or automatic.

## Required Review Questions

Reviewers should ask:

- Does this add only local-lab behavior?
- Are all ports loopback-only?
- Is there any `0.0.0.0` exposure?
- Are there any secrets?
- Are there any secrets/credentials?
- Are there any production-looking domains?
- Are there any production/cloud/tenant/private-network/external targets?
- Is CI/Maven untouched?
- Is production runtime untouched?
- Are k6 and Bruno still separate/manual?
- Is Toxiproxy still local-only?
- Are performance/readiness claims avoided?
- Are production readiness/certification claims avoided?
- Are runtime enforcement claims avoided?
- Are replay/evidence/report/storage/export claims avoided?

## Stop Conditions

A future app-service PR must stop before merge if:

- `src/main/java` changes appear;
- production API/routing behavior changes appear;
- production Dockerfile or Compose behavior changes unexpectedly;
- CI/Maven wiring appears without explicit scope;
- external/cloud/tenant/private-network targets appear;
- production/cloud/tenant/private-network/external targets appear;
- secrets/credentials appear;
- `0.0.0.0` default exposure appears;
- performance/readiness/certification/runtime-enforcement claims appear;
- throughput evidence or p95/p99 evidence is claimed;
- load/stress/benchmark evidence is claimed;
- replay/evidence/report/storage/export behavior appears.

## Current Status

- Current Compose skeleton remains Toxiproxy-only: [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml).
- No app service exists yet.
- k6 remains manual and separate: [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md).
- Bruno remains manual and separate: [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md).
- Toxiproxy remains manual/local-only: [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md).
- The Compose readiness gate remains the broader future-change gate: [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).
- The app-service boundary design remains the future-only design layer: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md).
- Manual tooling docs and readiness gate remain reviewer entry points: [`LOCAL_LAB_MANUAL_TOOLING_INDEX.md`](LOCAL_LAB_MANUAL_TOOLING_INDEX.md), [`LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md`](LOCAL_LAB_MANUAL_TOOLING_RUNBOOK.md), and [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md).
- End-of-day handoff docs summarize today's merged local-lab tooling and Compose guardrail chain without changing behavior: [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md).

## Remaining Not-Proven Boundaries

The following remain not proven:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime enforcement;
- Docker/k6/Bruno/Toxiproxy platform implementation beyond optional local-lab skeletons;
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
