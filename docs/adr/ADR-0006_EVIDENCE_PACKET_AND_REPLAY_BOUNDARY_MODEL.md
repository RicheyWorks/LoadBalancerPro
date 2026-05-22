# ADR-0006 Evidence Packet And Replay Boundary Model

## Status

Proposed / planning-only.

Decision type: architecture planning.

Implementation status: not implemented.

## Date

2026-05-22.

## Context

The architecture report frames LoadBalancerPro as an adaptive routing experimentation and evidence platform. It describes evidence packets, replay-facing evidence, deterministic comparison, auditability, and reviewer/operator explanations as core future architecture concerns.

[`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) now names ADR-0006 as the evidence packet and replay boundary model ADR. [`ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) defines the proposed future layered architecture boundary. [`ADR-0002_LASE_INTEGRATION_MODEL.md`](ADR-0002_LASE_INTEGRATION_MODEL.md) defines the proposed future LASE integration model. [`ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md`](ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md) defines evidence as a proposed future first-class architecture artifact. [`ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md`](ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md) defines proposed future workload realism and scenario modeling boundaries. [`ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md`](ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md) defines future safety modes and guardrails. [`ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md`](ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md) defines the proposed future reviewer evidence and trust model.

LoadBalancerPro already has reviewer evidence surfaces, decision/replay documentation, bounded lab evidence, and not-proven boundary language. This ADR records the future boundary model for `EvidencePacket`, `EvidenceAssembler`, and replay-facing evidence without implementing new runtime behavior.

This ADR is planning-only. This does not implement `EvidencePacket`. This does not implement `EvidenceAssembler`. This does not add replay execution. This does not add evidence generation or report generation. This does not add JSON output. This does not add storage, persistence, telemetry, or audit log implementation. This does not add filesystem-writing behavior. This does not add export, upload, download, PDF, ZIP, PR comment, or report artifact behavior. This does not change routing, scoring, strategy, proxy, API, config, Docker, CI, release, signing, registry, governance, or production behavior. This does not claim production readiness, production certification, live-cloud validation, or real-tenant validation.

## Decision

If a future implementation sprint is separately approved, evidence packet and replay-facing evidence work should be designed as a bounded reviewer/operator evidence path. Evidence may explain decisions, comparisons, signals, safety modes, uncertainty, and not-proven boundaries, but it must not become hidden production routing authority or production proof.

This proposed decision is not accepted implementation. It records a target boundary model for future evidence work:

- `EvidencePacket` is a future architecture concept, not a current runtime class introduced by this ADR;
- `EvidenceAssembler` is a future assembly boundary, not a current runtime component introduced by this ADR;
- replay-facing evidence should remain deterministic, bounded, and reviewer-readable where practical;
- replay evidence is not replay proof;
- comparison evidence is not correctness validation;
- evidence must distinguish observed facts from inferred or hypothetical outcomes;
- evidence must preserve privacy, source provenance, safety mode, and not-proven boundaries;
- evidence must not mutate live traffic, cloud resources, proxy behavior, routing state, GitHub settings, release assets, registries, or production systems.

## Conceptual Boundary Role

ADR-0006 defines the future line between evidence concepts and runtime behavior.

The boundary exists so future evidence packets and replay-facing summaries can help reviewers answer "what happened, why, what else was considered, what signals were used, and what was not proven" without turning evidence into an execution path.

Future evidence should be useful for explainability and auditability. It should help avoid black-box adaptive routing by preserving selected options, rejected options, policy checks, signal provenance, safety mode, uncertainty, and operator-facing explanation.

This ADR does not add a packet schema, assembler class, replay engine, report generator, persistence layer, export path, or filesystem writer.

## EvidencePacket Purpose

Future `EvidencePacket` concepts should capture decision context in a stable reviewer-readable shape.

Future `EvidencePacket` purpose:

- capture decision context;
- capture signals used;
- capture selected option;
- capture rejected options;
- capture policy checks;
- capture safety mode;
- capture uncertainty and not-proven boundaries;
- support reviewer/operator understanding;
- avoid black-box adaptive routing.

Future evidence packet content must remain explicit about limitations. It must not imply production readiness, production certification, live-cloud validation, real-tenant validation, correctness validation, or replay proof.

This ADR does not implement `EvidencePacket`, packet schemas, packet IDs, packet storage, JSON output, report files, or API fields.

## EvidenceAssembler Boundary

`EvidenceAssembler` is a future component boundary only.

Future `EvidenceAssembler` expectations:

- deterministic assembly expectations should be documented before implementation;
- assembly ordering should be stable where practical;
- source sections should identify observed, inferred, synthetic, unavailable, stale, and hypothetical context;
- no secret/env leakage;
- no tokens, credentials, private network details, or absolute local machine paths;
- no unsafe external calls;
- no hidden mutation;
- no filesystem writes unless separately designed later;
- no production traffic mutation;
- no automatic promotion from evidence to traffic-changing behavior.

Future `EvidenceAssembler` must not reach across boundaries to mutate live allocation, proxy routing, cloud resources, external systems, GitHub settings, release assets, registries, or production behavior.

This ADR does not add an `EvidenceAssembler` class, service, interface, package, API endpoint, config property, storage layer, or file writer.

## Replay Boundary Model

Replay-facing evidence is future architecture context. Replay must be bounded before it is implemented.

Future replay boundary expectations:

- replay should be deterministic and bounded;
- replay evidence should not be treated as production proof;
- replay should not mutate live systems;
- replay should not mutate routing state, scoring state, strategy state, proxy behavior, cloud resources, or production systems;
- replay should not import unsafe traces without privacy review;
- replay should distinguish observed facts from inferred/hypothetical outcomes;
- replay should label unavailable, synthetic, estimated, stale, or unknown context;
- replay should not require live-cloud or real-tenant access;
- replay should not publish artifacts, write files, upload outputs, or create downloads unless separately designed later.

Replay evidence is not replay proof. Comparison evidence is not correctness validation. Clean replay summaries are not production safety proof.

This ADR does not add replay execution, trace import, replay storage, replay reports, replay exports, replay downloads, replay PDFs, replay ZIPs, or replay filesystem-writing behavior.

## Evidence Categories

Future evidence categories should stay explicit and reviewable:

- routing decision evidence;
- policy gate evidence;
- signal provenance evidence;
- workload/scenario evidence;
- safety boundary evidence;
- rejected-option evidence;
- operator-review evidence;
- not-proven boundary evidence.

These categories are future planning vocabulary. They do not create runtime fields, files, JSON output, report output, API behavior, or persistence.

## Routing Decision Evidence

Future routing decision evidence should explain the selected option, rejected options, decision context, and policy/safety mode behind a decision or comparison.

Future routing evidence expectations:

- selected option should be visible;
- rejected options should be visible where applicable;
- candidate context should distinguish observed facts from derived summaries;
- scoring or ranking context should not reveal secrets or private infrastructure details;
- evidence must not mutate live routing state;
- evidence must not become hidden production route selection.

This ADR does not change routing, scoring, strategy, proxy, or API behavior.

## Policy Gate Evidence

Future policy gate evidence should show whether a decision or comparison was observe-only, recommendation, shadow, or active-experiment eligible.

Future policy evidence expectations:

- policy checks passed/failed should be visible;
- failed checks should remain reviewer-readable;
- safety mode should be explicit;
- traffic-change authorization must not be inferred from a clean packet;
- policy evidence must not bypass operator approval.

This ADR does not add policy gate implementation, config flags, approval workflows, API commands, runtime enforcement, or active traffic shifting.

## Signal Provenance Evidence

Future signal provenance evidence should explain what signals were used, where they came from, and what trust label applies.

Future provenance expectations:

- evidence should identify source-name, workload, scenario, LASE, policy, and external-signal context where available;
- signal context should be labeled as observed, synthetic, estimated, stale, unavailable, unknown, inferred, or hypothetical where applicable;
- signal provenance must not include secrets, tokens, credentials, env var values, private network details, or absolute local machine paths;
- future external signal context must remain read-only unless separately approved;
- missing or stale signal context must not grant mutation authority.

This ADR does not implement source-name guard behavior, external signal ingestion, `ExternalSignalPort`, `WorkloadProfile`, telemetry, external clients, or HTTP calls.

## Workload And Scenario Evidence

Future workload/scenario evidence should explain scenario assumptions and workload shape without claiming production validation.

Future workload/scenario evidence expectations:

- scenario labels should be stable and reviewer-readable;
- workload context should identify synthetic/lab/shadow/replay boundaries;
- trace-derived context requires privacy review before implementation;
- workload/scenario evidence must not prove live-cloud validation;
- workload/scenario evidence must not prove real-tenant validation;
- clean scenario output is not production safety proof.

This ADR does not implement `WorkloadProfile`, `ScenarioGenerator`, workload generators, trace import, telemetry import, replay execution, or scenario report generation.

## Safety Boundary Evidence

Future safety boundary evidence should align with ADR-0005.

Future safety evidence expectations:

- safety mode should be visible;
- rollback/stop condition context should be visible where applicable;
- blast-radius context should be visible where applicable;
- operator/reviewer action should be visible where applicable;
- what was not proven should be included;
- evidence must not certify production safety.

This ADR does not add runtime safety enforcement, active-experiment behavior, runtime LASE enforcement, or package-boundary enforcement.

## Rejected-Option Evidence

Future rejected-option evidence should explain why alternatives were not selected without overclaiming correctness.

Future rejected-option expectations:

- rejected options should be listed where useful;
- rejection reasons should be reviewer-readable;
- unavailable or incomplete evidence should be labeled;
- rejection evidence should not prove correctness validation;
- rejection evidence should not hide uncertainty.

This ADR does not add comparison output, report generation, scoring changes, or strategy behavior changes.

## Operator-Review Evidence

Future operator-review evidence should support human review before promotion.

Future operator-review expectations:

- operator-facing explanation should be present;
- manual review and approval should remain separate from packet assembly;
- evidence should not replace human review;
- evidence should not become hidden production routing authority;
- evidence should not write durable audit records unless a future storage/retention ADR and implementation PR approve that path.

This ADR does not add approval UI, durable audit log implementation, persistence, workflow automation, PR comments, artifacts, exports, or downloads.

## Not-Proven Boundary Evidence

Future evidence packets must make not-proven boundaries visible.

Future not-proven boundary evidence should identify that evidence does not prove:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime LASE enforcement;
- package-boundary enforcement;
- replay proof;
- correctness validation;
- safety certification;
- autonomous production traffic shifting.

This ADR does not add proof semantics or certification semantics.

## Determinism And Ordering Expectations

Future evidence assembly should be deterministic where practical.

Future deterministic expectations:

- evidence ordering should be stable;
- section names should be stable;
- omitted values should be explicit rather than fabricated;
- generated IDs, UUIDs, random values, unstable timestamps, hashes, MessageDigest, SHA, environment, and system-property behavior should be avoided unless separately approved;
- deterministic output must not be confused with correctness proof.

This ADR does not add deterministic output generation, file generation, JSON output, or report generation.

## Privacy And Trace-Safety Expectations

Future evidence and replay-facing summaries must preserve privacy and trace-safety boundaries.

Future privacy expectations:

- no secrets;
- no tokens;
- no credentials;
- no env var values;
- no private network details;
- no absolute local machine paths;
- no real reviewer names unless separately approved;
- future traces must be anonymized or synthetic unless separately approved;
- future trace import requires separate ADR/PR;
- future retention/deletion requires separate review.

This ADR does not add trace import, telemetry import, storage, persistence, audit logs, retention policy, deletion policy, export behavior, upload behavior, download behavior, PDF generation, or ZIP generation.

## Filesystem And Artifact Boundaries

Future evidence packets may require output design later, but this ADR does not add any output behavior.

Future filesystem/artifact expectations:

- no filesystem writes unless separately designed later;
- output paths must be reviewed before implementation;
- generated artifacts must not be confused with source-controlled docs;
- export/upload/download/PDF/ZIP behavior requires separate approval;
- PR comments and workflow artifacts require separate approval;
- storage and retention require separate approval.

This ADR does not add filesystem-writing implementation, report output, JSON output, export behavior, upload behavior, download behavior, PDF generation, ZIP generation, PR comment behavior, CI artifact behavior, or storage/persistence.

## Relationship To Prior ADRs

ADR-0006 depends on and narrows the evidence implications of prior ADRs:

- ADR-0001 architecture boundary: future evidence belongs in an evidence/reviewer boundary and must not blur domain, allocation, LASE, infrastructure, API, or config responsibilities.
- ADR-0002 LASE boundary: future LASE outputs may feed evidence, but LASE evidence must not mutate live allocation, select production routes, alter proxy behavior, or bypass policy gates.
- ADR-0003 evidence and external signal/source boundary: ADR-0003 defines evidence as a first-class artifact, while this ADR narrows future `EvidencePacket`, `EvidenceAssembler`, source provenance, and replay-facing evidence boundaries.
- ADR-0004 workload realism/scenario modeling: future workload and scenario context may enrich evidence, but clean scenario results remain lab/shadow/reviewer context, not production proof.
- ADR-0005 safety boundaries and guardrails: future evidence packets must expose safety mode, policy checks, uncertainty, traffic-change reasons, operator explanations, and not-proven boundaries.
- ADR-0007 reviewer evidence and trust model: future evidence packets may support reviewer trust, but they must remain non-certifying, explainable, and separate from reviewer portal/dashboard/API implementation unless separately approved.

These relationships are planning guidance only. They do not approve implementation.

## Relationship To LASE And Shadow Evaluation

Future LASE outputs may feed evidence packet summaries if separately implemented.

Future LASE/evidence expectations:

- LASE evidence remains reviewer/evidence metadata unless future policy gates separately promote behavior;
- LASE evidence must not directly mutate live allocation state;
- LASE evidence must not directly select production routes;
- LASE evidence must not directly alter proxy behavior;
- LASE evidence must not call CloudManager, GPU, grid, facility, power, carbon, release, registry, signing, or governance control paths;
- LASE evidence must not bypass policy/operator gates.

This ADR does not add runtime LASE enforcement, `LaseObservationPort`, replay execution, or package-boundary enforcement.

## Relationship To External Signals

Future evidence packets may include external signal context only as read-only provenance metadata if that context is separately implemented.

Future external signal expectations:

- external signal context must be read-only unless separately approved;
- unavailable, stale, synthetic, estimated, unknown, inferred, or hypothetical signals must be labeled;
- external signal context must not include secrets or private infrastructure details;
- external signal context must not grant mutation authority;
- missing or stale external signals must degrade safely if implemented later.

This ADR does not implement `ExternalSignalPort`, signal ingestion, external clients, HTTP calls, telemetry, GPU orchestration, power/grid control, carbon-aware routing, or facility automation.

## Safety And Non-Goals

This ADR is documentation and documentation guard tests only.

Non-goals:

- no production Java runtime behavior;
- no records/classes/interfaces/enums under `src/main/java`;
- no `EvidencePacket`;
- no `EvidenceAssembler`;
- no evidence generation;
- no report generation;
- no JSON output;
- no replay execution;
- no trace import;
- no workload generation;
- no `WorkloadProfile`;
- no `ScenarioGenerator`;
- no `ExternalSignalPort`;
- no `LaseObservationPort`;
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

This ADR does not claim ADR approval, `EvidencePacket` implementation, `EvidenceAssembler` implementation, replay execution, evidence/report generation, JSON output, storage/persistence, export/upload/download/PDF/ZIP behavior, filesystem-writing behavior, runtime LASE enforcement, package-boundary enforcement, ArchUnit enforcement, source-name guard implementation, routing/scoring/strategy/proxy/API behavior changes, production readiness, production certification, live-cloud validation, or real-tenant validation.

## Consequences

Positive consequences:

- reviewers get one proposed ADR for future evidence packet and replay-facing evidence boundaries;
- future `EvidencePacket` and `EvidenceAssembler` proposals can be evaluated before classes, interfaces, files, or runtime behavior exist;
- replay evidence is explicitly separated from replay proof and production proof;
- privacy, trace-safety, filesystem, artifact, and no-hidden-mutation boundaries are visible before implementation;
- ADR-0001 through ADR-0005 remain linked into a coherent evidence boundary model.

Costs and risks:

- this ADR adds documentation to maintain;
- future implementers may overread evidence vocabulary as current implementation unless planning-only language remains explicit;
- existing runtime/local evidence surfaces may be confused with architecture-report-level `EvidencePacket` unless future docs keep the distinction clear;
- future replay work still requires separate scoped PR review, deterministic tests, privacy review, behavior-impact review, and storage/artifact review.

## Relationship To Existing Docs

Related docs:

- [`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) names ADR-0006 as a proposed Phase 0 ADR.
- [`ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) defines the proposed future layer boundary that separates allocation, LASE, evidence, infrastructure, API, config, and docs/tests.
- [`ADR-0002_LASE_INTEGRATION_MODEL.md`](ADR-0002_LASE_INTEGRATION_MODEL.md) defines the proposed future LASE integration model.
- [`ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md`](ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md) defines the proposed future evidence architecture model.
- [`ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md`](ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md) defines the proposed future workload realism and scenario modeling model.
- [`ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md`](ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md) defines the proposed future safety boundary and guardrail model.
- [`ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md`](ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md) defines the proposed future reviewer evidence and trust model.
- [`../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md) maps evidence, replay, auditability, explainability, and future-only boundaries to current docs.
- [`../REVIEWER_TRUST_MAP.md`](../REVIEWER_TRUST_MAP.md) provides reviewer navigation and evidence boundaries.
- [`../ENTERPRISE_READINESS_AUDIT.md`](../ENTERPRISE_READINESS_AUDIT.md) records readiness posture and not-proven boundaries.
- [`../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md) defines the future LASE/live allocation boundary contract.
- [`../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md`](../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md) defines future WorkloadProfile signal metadata.
- [`../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md`](../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md) defines the future read-only ExternalSignalPort contract.
- [`../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md`](../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md) documents current decision replay evidence boundaries.
- [`../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY.md`](../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY.md) documents current decision replay evidence fields.

This ADR does not supersede those docs. It provides a proposed decision frame for future `EvidencePacket`, `EvidenceAssembler`, and replay boundary work.

## Not-Proven Boundaries

Remaining not-proven boundaries:

- not production-ready;
- not production-certified;
- not live-cloud validated;
- not real-tenant validated;
- not autonomous production traffic shifting;
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
- ADR-0006 is proposed/planning-only.
- ADR-0007 is proposed/planning-only.

## Reviewer-Facing Value

ADR-0006 gives reviewers a single place to inspect the intended future evidence packet and replay-facing evidence boundary before any `EvidencePacket`, `EvidenceAssembler`, runtime replay, report generation, storage, persistence, export, upload, download, PDF, ZIP, filesystem-writing behavior, or behavior change exists.

Reviewer value:

- future evidence packet purpose is explicit;
- future EvidenceAssembler boundaries are explicit;
- replay evidence is separated from replay proof;
- observed facts are separated from inferred or hypothetical outcomes;
- evidence categories are named before implementation;
- privacy, trace-safety, filesystem, artifact, and no-hidden-mutation boundaries are visible;
- relationships to ADR-0001 through ADR-0005 are recorded;
- not-proven boundaries remain part of the review model.

Reviewers should treat this ADR as proposed/planning-only. It is not ADR approval, not `EvidencePacket` implementation, not `EvidenceAssembler` implementation, not replay execution, not evidence/report generation, not storage/persistence, not export/upload/download/PDF/ZIP behavior, not filesystem-writing behavior, not runtime safety enforcement, not runtime LASE enforcement, not package-boundary enforcement, not ArchUnit enforcement, not autonomous production traffic shifting, not production-readiness proof, and not production-certification proof.
