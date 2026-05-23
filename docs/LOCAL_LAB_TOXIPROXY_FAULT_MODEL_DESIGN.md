# Local Lab Toxiproxy Fault Model Design

This document started as a docs/test-only future Toxiproxy fault model design spec. It converts the Toxiproxy lane from the local-lab boundary plan into concrete future fault categories and now points at the first separately scoped optional local-lab Toxiproxy config skeleton.

The first Toxiproxy follow-up adds one optional local-lab Toxiproxy config skeleton at [`../lab/toxiproxy/local-lab-toxiproxy.json`](../lab/toxiproxy/local-lab-toxiproxy.json). See [`LOCAL_LAB_TOXIPROXY_CONFIG.md`](LOCAL_LAB_TOXIPROXY_CONFIG.md), [`LOCAL_LAB_K6_SMOKE_SCRIPT.md`](LOCAL_LAB_K6_SMOKE_SCRIPT.md), and [`LOCAL_LAB_BRUNO_COLLECTION.md`](LOCAL_LAB_BRUNO_COLLECTION.md). It is manual-only, not CI-gated, not Dockerized, not Docker Compose orchestration, not wired into the application, not wired into Maven, not wired into k6 execution, not wired into Bruno execution, and must use local/lab-owned loopback endpoints by default. It does not start Toxiproxy. It does not start the application.

No Docker Compose is added in this PR. No Docker Compose files are added in this PR. No network damage is actually executed. No live network, LAN, or production network is touched. No actual tool execution occurs in this PR.

Future Toxiproxy work must be local/lab-only first. Future Toxiproxy work must be separately scoped. Future tool work must be separately scoped. Future tool work must target local/lab endpoints first.

## Relationship To Current Local Lab

The current local lab already has deterministic fixture labels for latency, degradation, error, overload, unhealthy, recovery, and unknown-label boundaries. This design names future local fault model categories and cross-links the first optional loopback-only config skeleton that could support later manual local review after separate approval.

This design does not add expanded Toxiproxy fault execution, Docker/Docker Compose files, k6 scripts, Bruno collections, scripts, CI jobs, Maven dependencies, production endpoints, production listeners, live network behavior, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, or p95/p99 evidence.

## Future Fault Model Categories

| Future fault category | Local-lab source concept | Design-only expectation |
| --- | --- | --- |
| latency injection | `SLOW_TAIL_LATENCY` fixture and matrix labels | would model local fault delay labels without benchmarking claims |
| timeout behavior | future fault-style fixture expansion | would remain local/lab-only and separately scoped |
| intermittent connection failure | `ERROR_PRONE` and partial degradation labels | would avoid live network, LAN, and production network effects |
| bandwidth/throughput constraint as a future concept only | queue-pressure labels | design-only; no throughput evidence is produced |
| reset/close behavior | future fault-style fixture expansion | design-only; no network damage is actually executed |
| recovery after fault removal | `RECOVERY` fixture labels | would preserve deterministic local recovery expectations |
| partial degradation patterns | `PARTIAL_DEGRADATION` fixture and matrix case | would keep degradation modeling local/test-scope only |

## Future Toxiproxy Boundaries

- one optional local-lab Toxiproxy config skeleton is added at `lab/toxiproxy/local-lab-toxiproxy.json`;
- no expanded Toxiproxy fault execution is added in this PR;
- no Docker Compose is added in this PR;
- no network damage is actually executed;
- no live network, LAN, or production network is touched;
- the skeleton must stay manual-only, not CI-gated, not Dockerized, not Docker Compose orchestration, not wired into the application, not wired into Maven, not wired into k6 execution, and not wired into Bruno execution;
- the skeleton does not start Toxiproxy and does not start the application;
- future Toxiproxy work must be local/lab-only first;
- future Toxiproxy work must be separately scoped;
- future Toxiproxy work must stop if it needs production endpoint wiring, non-loopback targets, `0.0.0.0` binding, fixed production targets, Maven dependency changes, scripts outside the approved lane, CI changes, replay execution, evidence/report generation, storage, export, or production-validation language.

## Shared Reviewer Boundaries

These fault categories are design-only. They provide no actual tool execution, no production proof, no live-cloud proof, no real-tenant proof, no runtime enforcement proof, no replay execution, no evidence/report generation, no storage/export behavior, no production traffic, and no autonomous traffic shifting.

The following remain not proven: production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, Docker/k6/Bruno/Toxiproxy implementation, replay execution, evidence/report generation, storage/export behavior, load testing, stress testing, benchmarking, throughput evidence, p95/p99 evidence, autonomous production traffic shifting, carbon-aware routing, GPU orchestration, power/grid control, and facility automation.
