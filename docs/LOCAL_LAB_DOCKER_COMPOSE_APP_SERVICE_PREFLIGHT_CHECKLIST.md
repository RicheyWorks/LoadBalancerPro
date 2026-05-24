# Local Lab Docker Compose App Service Preflight Checklist

This page is the preflight checklist for app-service PRs in the optional local-lab Docker Compose lane. It defines the exact preflight proof reviewers should require before adding or expanding an `app-under-test` service in [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml).

The gated app-service skeleton PR applies this checklist before adding one local-lab-only `app-under-test` service. It does not change Docker packaging. It makes no Docker packaging changes. It does not add CI-gating. It does not add Maven wiring. It does not wire Compose into CI or Maven. It does not change production runtime behavior.

It adds no throughput evidence, no p95/p99 evidence, and no load/stress/benchmark evidence. It adds no replay execution, evidence/report generation, storage, or export behavior.

The current Compose skeleton contains the existing Toxiproxy service plus the gated app-under-test service. The k6 smoke script remains manual and separate. The Bruno collection remains manual and separate. Toxiproxy remains manual/local-only. This preflight checklist complements [`LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md`](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md), [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_BOUNDARY_DESIGN.md), [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md), [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md), [`LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_SKELETON.md), and [`LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md`](LOCAL_LAB_DOCKER_COMPOSE_MANUAL_RUNBOOK.md).

The end-of-day Compose handoff in [`LOCAL_LAB_PROGRESS_HANDOFF.md`](LOCAL_LAB_PROGRESS_HANDOFF.md) and [`LOCAL_LAB_NEXT_STEPS_BOUNDARY.md`](LOCAL_LAB_NEXT_STEPS_BOUNDARY.md) summarizes the current guardrail chain through this checklist. It does not add an app service, does not change Compose behavior, does not change Docker packaging, does not add CI-gating, and does not add Maven wiring.

## Purpose

- Provide a preflight checklist for app-service Compose PRs.
- Keep the gated app service optional, manual-only, local-lab-only, loopback-bound, and read-only mounted.
- Keep k6 runner service, Bruno runner service, and broader new Compose services out of this PR.
- Keep Dockerfiles, production Docker packaging, CI-gating, Maven wiring, automated execution, registry login/push, production runtime behavior, and production Compose profiles out of this PR.

## Required Proof Before App Service Changes

An app-service PR must prove:

- future app-service expansion PR must be separately scoped;
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
- documented relationship to the current Compose skeleton with Toxiproxy and the gated app service;
- documented stop conditions.

## Required Local-Lab Service Boundaries

Before an app service can be reviewed, the PR must preserve these boundaries:

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

- Current Compose skeleton contains the existing Toxiproxy service and the gated app-under-test service: [`../lab/docker-compose/local-lab-compose.yml`](../lab/docker-compose/local-lab-compose.yml).
- The app-service skeleton remains optional/manual/local-lab-only and is documented in [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md).
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

## Post-App-Service Compose Handoff Update

PR #284 is now the current local-lab Compose baseline. This preflight update is docs/test-only and does not change `lab/docker-compose/local-lab-compose.yml`, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, scripts, production Compose, production Docker packaging, or production runtime behavior.

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

Use this preflight checklist together with [LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md](LOCAL_LAB_DOCKER_COMPOSE_READINESS_GATE.md) and [LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_SKELETON.md) before considering any later Compose change.

## App-Service Manual Smoke Checklist Update

The app-service manual smoke checklist is now a docs/test-only companion checklist: [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_MANUAL_SMOKE_CHECKLIST.md). It does not change `lab/docker-compose/local-lab-compose.yml`, app behavior, Dockerfiles, Maven, CI, runtime resources, k6, Bruno, Toxiproxy behavior, production Compose, production Docker packaging, or production runtime behavior.

It documents optional/manual/local-lab-only inspection and manual smoke steps for the existing `app-under-test` service. It preserves manual package-first operation, the read-only local `target/` mount, the `127.0.0.1:8080:8080` app port, Toxiproxy presence, k6 and Bruno as manual separate tools, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no production runtime behavior change.

The checklist does not create production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.

## App-Service Health/Readiness Documentation Update

The app-service health/readiness documentation lane is now available at [`LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md`](LOCAL_LAB_DOCKER_COMPOSE_APP_SERVICE_HEALTH_READINESS.md). It is docs/test-only and does not change Compose behavior, app behavior, Dockerfiles, Maven, CI, runtime resources, production Compose, production Docker packaging, production runtime behavior, or application endpoints. It adds no health endpoint and no readiness endpoint.

The lane records what reviewers may inspect and what optional manual local-only observations can and cannot show for the existing `app-under-test` service. It preserves the optional/manual/local-lab-only boundary, manual package-first operation, read-only `target/` mount, `127.0.0.1:8080:8080`, Toxiproxy presence, k6 and Bruno as manual separate tools, no k6 runner service, no Bruno runner service, no CI-gating, no Maven wiring, no Dockerfile change, no production Docker packaging, no production Compose change, and no production runtime behavior change.

The lane does not support production readiness/certification claims, live-cloud or real-tenant validation claims, runtime enforcement claims, replay/evidence/report/storage/export behavior claims, load/stress/benchmark claims, or throughput/p95/p99 evidence claims.
