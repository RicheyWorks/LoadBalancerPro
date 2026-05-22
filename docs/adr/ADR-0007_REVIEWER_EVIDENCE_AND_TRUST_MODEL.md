# ADR-0007 Reviewer Evidence And Trust Model

## Status

Proposed / planning-only.

Decision type: architecture planning.

Implementation status: not implemented.

## Date

2026-05-22.

## Context

The architecture report frames LoadBalancerPro as an adaptive routing experimentation and evidence platform. It emphasizes reviewer trust, explainability, auditability, guardrails before autonomy, deterministic comparison, and a clear separation between current lab evidence and future production-facing authority.

[`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) now names ADR-0007 as the reviewer evidence and trust model ADR. [`ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) defines the proposed future layered architecture boundary. [`ADR-0002_LASE_INTEGRATION_MODEL.md`](ADR-0002_LASE_INTEGRATION_MODEL.md) defines the proposed future LASE integration model. [`ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md`](ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md) defines evidence as a proposed future first-class architecture artifact. [`ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md`](ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md) defines proposed future workload realism and scenario modeling boundaries. [`ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md`](ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md) defines future safety modes and guardrails. [`ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md`](ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md) defines proposed future evidence packet and replay-facing evidence boundaries.

LoadBalancerPro already has reviewer evidence surfaces, local lab dashboards, bounded evidence packet shapes, trust maps, and readiness audit language. ADR-0007 records the future reviewer evidence and trust model without implementing new reviewer portals, dashboards, APIs, evidence generation, replay execution, storage, export, or runtime enforcement.

This ADR is planning-only. This does not implement reviewer portals. This does not implement reviewer dashboards. This does not implement reviewer APIs. This does not add evidence generation or report generation. This does not add replay execution. This does not add storage, persistence, telemetry, or audit log implementation. This does not add filesystem-writing behavior. This does not add export, upload, download, PDF, ZIP, PR comment, or report artifact behavior. This does not add runtime safety enforcement or runtime LASE enforcement. This does not change routing, scoring, strategy, proxy, API, config, Docker, CI, release, signing, registry, governance, or production behavior. This does not claim production readiness, production certification, live-cloud validation, or real-tenant validation.

## Decision

If a future implementation sprint is separately approved, reviewer-facing evidence should be designed around explicit trust boundaries. Evidence should help reviewers, operators, and future enterprise evaluators understand adaptive-routing behavior without treating documentation, local lab output, or future evidence packets as production proof.

This proposed decision is not accepted implementation. It records a target trust model for future reviewer evidence work:

- reviewer evidence should explain what decision was considered, what option was selected, what options were rejected, what signals were used, and what was not proven;
- observed facts must be separated from inferred, hypothetical, synthetic, stale, unavailable, or unknown context;
- reviewer evidence must preserve safety mode, policy gate, signal provenance, uncertainty, manual/operator approval, and not-proven boundaries;
- reviewer evidence must not become hidden production routing authority;
- reviewer evidence must not replace human review;
- reviewer evidence must not imply runtime enforcement where none exists;
- future reviewer evidence changes require separately scoped PR review before dashboards, APIs, generated reports, persisted artifacts, exports, or runtime behavior are added.

## Reviewer Evidence Purpose

Reviewer evidence exists to help humans understand adaptive-routing behavior without black-box trust.

Future reviewer evidence should:

- help reviewers understand adaptive-routing decisions;
- avoid black-box trust;
- separate observed facts from inferred/hypothetical claims;
- show what was proven and what was not proven;
- support operator review before autonomy;
- support future enterprise evaluation without overclaiming production readiness;
- make safety boundaries visible before traffic-changing authority expands;
- keep reviewer understanding separate from production routing authority.

This ADR does not add reviewer evidence generation, reviewer dashboards, reviewer portals, reviewer APIs, or report output.

## Reviewer Trust Model

A reviewer should evaluate future evidence through a bounded trust model.

Reviewer trust inputs:

- decision evidence;
- policy gate evidence;
- signal provenance evidence;
- rejected-option evidence;
- safety-mode evidence;
- replay/scenario evidence;
- uncertainty and not-proven boundaries;
- manual/operator approval requirements;
- deterministic evidence expectations.

Reviewer trust does not mean production trust. A clean reviewer evidence packet, dashboard, or local lab result should be treated as review support unless a separate future implementation and validation path proves more.

## Decision Evidence

Future decision evidence should make the considered adaptive-routing decision understandable.

Decision evidence should show:

- what decision was considered;
- which option was selected;
- which options were rejected;
- what decision context was available;
- whether the decision was observe-only, recommendation, shadow, or active-experiment context;
- what was not proven by the decision evidence.

Decision evidence must not become production route selection, hidden scoring authority, or correctness proof.

## Policy Gate Evidence

Future policy gate evidence should explain why traffic was or was not allowed to change.

Policy gate evidence should show:

- which policy checks passed/failed;
- current safety mode;
- whether operator approval was required;
- whether manual promotion was allowed or denied;
- whether rollback or stop conditions were required;
- why traffic was or was not allowed to change.

Policy gate evidence must not bypass policy gates, operator approval, runtime enforcement boundaries, or separate implementation review.

## Signal Provenance Evidence

Future signal provenance evidence should explain which signals were used and what trust label applies.

Signal provenance evidence should show:

- which signals were used;
- signal source/provenance;
- whether signals were observed, synthetic, inferred, hypothetical, stale, unavailable, estimated, or unknown;
- whether signal context came from workload/scenario evidence, LASE evidence, policy evidence, or future external signal context;
- which signal limitations remain.

Signal provenance evidence must not include secrets, tokens, credentials, environment variable values, private network details, or absolute local machine paths. It must not add external signal ingestion, external clients, HTTP calls, or mutation authority.

## Rejected-Option Evidence

Future rejected-option evidence should help reviewers understand tradeoffs without overclaiming correctness.

Rejected-option evidence should show:

- which options were rejected;
- why each rejected option was not selected;
- what assumptions influenced the rejection;
- what uncertainty remains;
- whether a rejected option would require additional review before promotion.

Rejected-option evidence is not correctness validation and must not hide unsafe assumptions.

## Safety-Mode Evidence

Future safety-mode evidence should align with ADR-0005.

Safety-mode evidence should show:

- current safety mode;
- observe-only, recommendation, shadow, or active-experiment posture;
- manual/operator approval requirements;
- rollback/stop conditions where applicable;
- blast-radius limits where applicable;
- confidence threshold context where applicable;
- reason traffic was or was not allowed to change.

Safety-mode evidence must not imply autonomous production traffic shifting, production certification, or runtime safety enforcement unless those are separately implemented and validated.

## Replay And Scenario Evidence

Future replay/scenario evidence should support reviewer understanding without becoming proof.

Replay/scenario evidence should show:

- whether context is synthetic, lab, shadow, replay-facing, inferred, or observed;
- what workload or scenario assumptions apply;
- what comparison was made;
- what was not proven;
- whether replay execution exists or remains future-only.

Replay evidence is not replay proof. Scenario evidence is not live-cloud validation. Scenario evidence is not real-tenant validation. Clean scenario results are not production safety proof.

This ADR does not add replay execution, trace import, scenario generation, report generation, storage, persistence, or export behavior.

## Evidence Explanation Expectations

Future reviewer-facing evidence should explain:

- what decision was considered;
- which option was selected;
- which options were rejected;
- which signals were used;
- signal source/provenance;
- which policy checks passed/failed;
- current safety mode;
- why traffic was or was not allowed to change;
- known uncertainty;
- explicit not-proven boundaries;
- operator-facing explanation;
- manual/operator approval requirements where applicable.

Explanation quality matters because reviewer trust depends on visible reasoning, not just a pass/fail label.

## Manual And Operator Approval Expectations

Future reviewer evidence should keep manual/operator approval separate from evidence assembly.

Manual/operator approval expectations:

- evidence may support operator review;
- evidence may recommend follow-up review;
- evidence may explain why manual promotion is blocked or allowed in a future policy model;
- evidence must not apply suggested actions automatically;
- evidence must not replace human review;
- evidence must not imply approval if no operator approval exists.

No approval workflow, dashboard, portal, API command, PR comment, or runtime promotion behavior is implemented by this ADR.

## Deterministic Evidence Expectations

Future reviewer-facing evidence should be deterministic where practical.

Deterministic expectations:

- evidence section names should be stable;
- evidence ordering should be stable;
- unavailable values should be explicit rather than fabricated;
- deterministic evidence expectations should be documented before implementation;
- generated IDs, UUIDs, random values, unstable timestamps, hashes, MessageDigest, SHA, environment, and system-property behavior should be avoided unless separately approved;
- deterministic output must not be confused with correctness proof.

This ADR does not add deterministic output generation, JSON output, report files, persisted artifacts, or filesystem-writing behavior.

## Trust Boundaries

Reviewer evidence has strict trust boundaries.

Reviewer evidence:

- is not production certification;
- is not live-cloud validation;
- is not real-tenant validation;
- is not proof of correctness;
- is not proof of replay accuracy unless replay is separately implemented and validated;
- is not proof of safe autonomous production traffic shifting;
- must not hide unsafe assumptions;
- must not imply runtime enforcement where none exists;
- must not replace human review;
- must not become hidden production routing authority.

These boundaries apply to existing local lab evidence and to any future reviewer evidence proposal.

## Relationship To Prior ADRs

ADR-0007 depends on and narrows the reviewer-facing trust implications of prior ADRs:

- ADR-0001 architecture boundary: reviewer evidence belongs in evidence/reviewer surfaces and must not blur domain, allocation, LASE, infrastructure, API, or config responsibilities.
- ADR-0002 LASE boundary: LASE evidence may inform reviewer trust, but it must not mutate live allocation, select production routes, alter proxy behavior, or bypass policy/operator gates.
- ADR-0003 external signal/source boundary: ADR-0003 defines evidence as a first-class artifact and frames signal/source limitations; reviewer trust must keep source provenance and not-proven boundaries visible.
- ADR-0004 workload realism/scenario modeling: workload and scenario evidence may support reviewer understanding, but clean lab/scenario results remain non-proving and do not imply live-cloud or real-tenant validation.
- ADR-0005 safety boundaries and guardrails: reviewer evidence must expose safety mode, policy checks, rollback/stop context, manual promotion requirements, and traffic-change reasons.
- ADR-0006 evidence packet and replay boundary model: future `EvidencePacket`, `EvidenceAssembler`, and replay-facing evidence may support reviewer trust, but they remain future boundaries until separately implemented.

These relationships are planning guidance only. They do not approve implementation.

## Relationship To North-Star Vision

ADR-0007 supports the north-star vision by keeping adaptive routing understandable before it becomes more autonomous.

North-star relationship:

- future datacenter adaptive traffic control should remain explainable to reviewers and operators;
- partial degradation and recovery behavior should be reviewable before promotion;
- tail latency under pressure should be explained with visible assumptions and not-proven boundaries;
- safer adaptive routing requires guardrails before autonomy;
- explainability and auditability should come before hidden traffic-changing authority;
- trusted adaptive routing should avoid black-box routing decisions.

This ADR ties reviewer trust to future datacenter adaptive traffic control without claiming production readiness, production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing, or facility automation.

## Safety And Non-Goals

This ADR is documentation and documentation guard tests only.

Non-goals:

- no production Java runtime behavior;
- no records/classes/interfaces/enums under `src/main/java`;
- no reviewer portal implementation;
- no reviewer dashboard implementation;
- no reviewer API implementation;
- no EvidencePacket implementation;
- no EvidenceAssembler implementation;
- no evidence generation;
- no report generation;
- no JSON output;
- no replay execution;
- no trace import;
- no workload generation;
- no WorkloadProfile implementation;
- no ScenarioGenerator implementation;
- no ExternalSignalPort implementation;
- no LaseObservationPort implementation;
- no storage or persistence;
- no telemetry or audit log implementation;
- no filesystem-writing implementation;
- no export, upload, download, PDF, or ZIP behavior;
- no PR comment/report artifact behavior;
- no source scanning;
- no source-name guard implementation;
- no runtime enforcement;
- no runtime LASE enforcement;
- no package-boundary enforcement;
- no ArchUnit enforcement;
- no external signal ingestion;
- no external API clients;
- no HTTP calls;
- no secrets, tokens, environment variables, credentials, config, or properties;
- no routing behavior change;
- no scoring behavior change;
- no strategy behavior change;
- no proxy behavior change;
- no API behavior change;
- no config/resource/runtime changes;
- no Maven dependency changes;
- no Docker, CI, release, signing, registry, or governance changes;
- no autonomous production traffic shifting;
- no carbon-aware routing;
- no GPU orchestration;
- no power/grid control;
- no facility automation.

This ADR does not claim ADR approval, reviewer portal implementation, reviewer dashboard implementation, reviewer API implementation, `EvidencePacket` implementation, `EvidenceAssembler` implementation, replay execution, evidence/report generation, JSON output, storage/persistence, export/upload/download/PDF/ZIP behavior, filesystem-writing behavior, runtime LASE enforcement, package-boundary enforcement, ArchUnit enforcement, source-name guard implementation, routing/scoring/strategy/proxy/API behavior changes, production readiness, production certification, live-cloud validation, or real-tenant validation.

## Consequences

Positive consequences:

- reviewers get one proposed ADR for future reviewer evidence and trust boundaries;
- future evidence UI, portal, dashboard, packet, report, or API proposals can be evaluated against a stable trust model before implementation;
- observed facts, inferred claims, hypothetical outcomes, and not-proven boundaries are separated;
- safety mode, policy gates, signal provenance, rejected options, uncertainty, and manual/operator approval expectations are visible before runtime expansion;
- the north-star vision gains an explicit anti-black-box trust frame.

Costs and risks:

- this ADR adds documentation to maintain;
- future implementers may overread reviewer trust vocabulary as current implementation unless planning-only language remains explicit;
- existing local reviewer pages may be confused with future reviewer trust model implementation unless docs keep boundaries clear;
- future reviewer portal/dashboard/API work still requires separate scoped PR review, deterministic tests, privacy review, behavior-impact review, and storage/artifact review.

## Relationship To Existing Docs

Related docs:

- [`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) names ADR-0007 as a proposed Phase 0 ADR.
- [`ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) defines the proposed future layer boundary that separates allocation, LASE, evidence, infrastructure, API, config, and docs/tests.
- [`ADR-0002_LASE_INTEGRATION_MODEL.md`](ADR-0002_LASE_INTEGRATION_MODEL.md) defines the proposed future LASE integration model.
- [`ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md`](ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md) defines the proposed future evidence architecture model.
- [`ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md`](ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md) defines the proposed future workload realism and scenario modeling model.
- [`ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md`](ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md) defines the proposed future safety boundary and guardrail model.
- [`ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md`](ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md) defines the proposed future evidence packet and replay boundary model.
- [`../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md) maps evidence, explainability, auditability, observability, cockpit, and future-only boundaries to current docs.
- [`../REVIEWER_TRUST_MAP.md`](../REVIEWER_TRUST_MAP.md) provides reviewer navigation and evidence boundaries.
- [`../ENTERPRISE_READINESS_AUDIT.md`](../ENTERPRISE_READINESS_AUDIT.md) records readiness posture and not-proven boundaries.
- [`../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md) defines the future LASE/live allocation boundary contract.
- [`../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md`](../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md) defines future WorkloadProfile signal metadata.
- [`../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md`](../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md) defines the future read-only ExternalSignalPort contract.
- [`../ENTERPRISE_LAB_DECISION_VECTOR.md`](../ENTERPRISE_LAB_DECISION_VECTOR.md) documents current controlled-lab decision explanation.
- [`../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md`](../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md) documents current decision replay evidence boundaries.
- [`../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY.md`](../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY.md) documents current decision replay evidence fields.

This ADR does not supersede those docs. It provides a proposed decision frame for future reviewer evidence and trust model work.

## Not-Proven Boundaries

Remaining not-proven boundaries:

- not production-ready;
- not production-certified;
- not live-cloud validated;
- not real-tenant validated;
- not autonomous production traffic shifting;
- not reviewer portal implementation;
- not reviewer dashboard implementation;
- not reviewer API implementation;
- not runtime safety enforcement;
- not runtime LASE enforcement;
- not package-boundary enforcement;
- not ArchUnit enforcement;
- replay execution not added;
- evidence/report generation not added;
- EvidencePacket implementation not added;
- EvidenceAssembler implementation not added;
- filesystem-writing implementation not added;
- export/upload/download/PDF/ZIP behavior not added;
- storage/persistence not added;
- workload generation not added;
- trace import not added;
- external signal ingestion not added;
- carbon-aware routing not implemented;
- GPU orchestration not implemented;
- power/grid control not implemented;
- facility automation not implemented;
- ExternalSignalPort not implemented;
- WorkloadProfile signal metadata not implemented;
- WorkloadProfile implementation not added;
- ScenarioGenerator implementation not added;
- LaseObservationPort not added;
- source-name guard not implemented;
- LASE boundary not runtime-enforced yet;
- LASE package boundary not enforced yet;
- Architecture report alignment index is not implementation;
- Phase 0 ADR index is not implementation;
- ADR-0001 is proposed/planning-only;
- ADR-0002 is proposed/planning-only;
- ADR-0003 is proposed/planning-only;
- ADR-0004 is proposed/planning-only;
- ADR-0005 is proposed/planning-only;
- ADR-0006 is proposed/planning-only;
- ADR-0007 is proposed/planning-only.

## Reviewer-Facing Value

ADR-0007 gives reviewers a single place to inspect the intended future reviewer evidence and trust model before any new reviewer portal, dashboard, API, evidence generation, replay execution, report generation, storage, persistence, export, upload, download, PDF, ZIP, filesystem-writing behavior, or behavior change exists.

Reviewer value:

- reviewer evidence purpose is explicit;
- reviewer trust inputs are named before implementation;
- evidence explanation expectations are visible;
- observed facts are separated from inferred and hypothetical claims;
- policy gates, signal provenance, rejected options, safety mode, uncertainty, and manual/operator approval expectations are reviewable;
- trust boundaries are explicit and non-certifying;
- relationships to ADR-0001 through ADR-0006 are recorded;
- the north-star adaptive-routing vision is tied to explainability and auditability before autonomy;
- not-proven boundaries remain part of the review model.

Reviewers should treat this ADR as proposed/planning-only. It is not ADR approval, not reviewer portal implementation, not reviewer dashboard implementation, not reviewer API implementation, not `EvidencePacket` implementation, not `EvidenceAssembler` implementation, not replay execution, not evidence/report generation, not storage/persistence, not export/upload/download/PDF/ZIP behavior, not filesystem-writing behavior, not runtime safety enforcement, not runtime LASE enforcement, not package-boundary enforcement, not ArchUnit enforcement, not autonomous production traffic shifting, not production-readiness proof, and not production-certification proof.
