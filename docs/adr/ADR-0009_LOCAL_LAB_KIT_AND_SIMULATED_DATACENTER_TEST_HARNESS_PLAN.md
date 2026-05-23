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

This ADR is planning-only. This does not implement Docker Compose files, scripts, fake nodes, k6 tests, Bruno collections, telemetry ingestion, replay, report generation, storage, export behavior, or runtime routing behavior yet.

This ADR does not add Docker Compose implementation. This ADR does not add scripts implementation. This ADR does not add fake backend node implementation. This ADR does not add k6 scenario implementation. This ADR does not add Bruno collection implementation. This ADR does not add Prometheus/Grafana implementation. This ADR does not add Toxiproxy implementation. This ADR does not change API behavior. This ADR does not change routing behavior. This ADR does not change scoring behavior. This ADR does not change strategy behavior. This ADR does not change proxy behavior. This ADR does not add config/resource/runtime changes. This ADR does not add Maven dependency changes. This ADR does not add CI changes.

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
| Optional Toxiproxy network degradation layer | Future latency, timeout, connection reset, and network damage scenarios. | No Toxiproxy configuration in this ADR. |
| Optional k6 traffic generator | Future smoke, load, stress, spike, and tail-latency tests. | No k6 scenario files in this ADR. |
| Optional Bruno API checks | Future API collection checks for local lab workflows. | No Bruno collection in this ADR. |
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

These are future/planned roles, not implemented files in this sprint. ADR-0009 does not add Docker Compose files, scripts, fake backend nodes, k6 files, Bruno collections, WireMock fixtures, Toxiproxy configuration, Prometheus/Grafana dashboards, Maven dependencies, CI jobs, or runtime configuration.

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
- no Bruno collections;
- no Prometheus/Grafana dashboards;
- no Toxiproxy configuration;
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
- Bruno collections;
- Prometheus/Grafana dashboards;
- Toxiproxy configuration;
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

Reviewers should treat ADR-0009 as proposed/planning-only. It is not Docker Compose implementation, not scripts implementation, not fake backend node implementation, not k6 scenario implementation, not Bruno collection implementation, not Toxiproxy implementation, not Prometheus/Grafana implementation, not runtime LASE enforcement, not package-boundary enforcement, not routing/scoring/strategy/proxy/API behavior change, not replay execution, not evidence/report generation, not storage/persistence, not filesystem-writing/export behavior, not production-readiness proof, and not production-certification proof.
