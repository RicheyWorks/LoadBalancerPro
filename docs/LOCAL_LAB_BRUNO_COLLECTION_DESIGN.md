# Local Lab Bruno Collection Design

This document started as a docs/test-only future Bruno API check collection design spec. It converts the Bruno lane from the local-lab boundary plan into concrete collection shapes and now points at the first separately scoped optional local-lab Bruno collection skeleton.

The first Bruno follow-up adds one optional local-lab Bruno collection skeleton at [`../lab/bruno/local-lab-smoke/`](../lab/bruno/local-lab-smoke/). See [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md) and [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md). It is not CI-gated, not Dockerized, not Toxiproxy integration, not k6 execution, and must target local/lab-owned loopback endpoints by default.

No live API or production API checks are added. No external calls are added. No Docker Compose files are added in this PR. No actual tool execution occurs in this PR.

Future Bruno collection work must target local/lab endpoints first. Future Bruno work must be separately scoped. Future tool work must be separately scoped. Future tool work must target local/lab endpoints first.

## Relationship To Current Local Lab

The current local lab already has deterministic loopback harnesses, smoke and matrix summaries, bounded burst summaries, reviewer checklist mappings, and boundary docs. This design only names future Bruno collection areas that could be considered after a separate implementation review.

This design does not add expanded Bruno collections, k6 scripts, Toxiproxy config, Docker/Docker Compose files, scripts, CI jobs, Maven dependencies, production endpoints, production listeners, external calls, live API checks, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, or p95/p99 evidence.

## Future Collection Areas

| Future collection area | Design-only shape | Boundary expectation |
| --- | --- | --- |
| local health/readiness checks if applicable | requests against future local/lab health or readiness surfaces | local/lab endpoints first, no production API checks |
| local routing compare checks if applicable | local comparison requests that mirror reviewer expectations | no production routing or proxy behavior changes |
| local lab harness check patterns | checks around loopback harness descriptors and expected labels | no external calls and no non-loopback targets |
| future operator/reviewer API checks | reviewer-facing checks only after a future local API lane exists | no reviewer portal, dashboard, or production API behavior in this PR |
| negative/boundary checks | expected boundary responses for unknown labels or unsupported paths | deterministic, local/test-scope wording only |
| collection naming and folder structure as design only | proposed names and grouping for later Bruno files | design-only for future expansion; no expanded Bruno collection files are added |
| first optional local-lab smoke collection skeleton | tiny read-only requests for `{{baseUrl}}/api/health`, `{{baseUrl}}/actuator/health`, and `{{baseUrl}}/api/lab/scenarios` | optional/manual only, not CI-gated, not Dockerized, not Toxiproxy integration, not k6 execution, local/lab-owned loopback endpoints only |

## Future Bruno Boundaries

- one optional local-lab Bruno collection skeleton is added at `lab/bruno/local-lab-smoke`;
- no expanded Bruno collection files are added in this PR;
- no live API or production API checks are added;
- no external calls are added;
- the skeleton must stay optional/manual, not CI-gated, not Dockerized, not Toxiproxy integration, and not k6 execution;
- future Bruno collection work must target local/lab endpoints first;
- future Bruno work must be separately scoped;
- future Bruno work must stop if it needs production endpoint wiring, non-loopback targets, fixed ports, Maven dependency changes, scripts outside the approved lane, CI changes, replay execution, evidence/report generation, storage, export, or production-validation language.

## Shared Reviewer Boundaries

These collection shapes are design-only. They provide no actual tool execution, no production proof, no live-cloud proof, no real-tenant proof, no runtime enforcement proof, no replay execution, no evidence/report generation, no storage/export behavior, no production traffic, and no autonomous traffic shifting.

The following remain not proven: production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, Docker/k6/Bruno/Toxiproxy implementation, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, p95/p99 evidence, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, and facility automation.
