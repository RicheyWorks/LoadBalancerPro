# ADR-0003 Evidence As First-Class Artifact

## Status

Proposed / planning-only.

Decision type: architecture planning.

Implementation status: not implemented.

## Date

2026-05-21.

## Context

The uploaded architecture report recommends "Evidence as First-Class Artifact" as a Phase 0 architecture decision. It frames LoadBalancerPro as a flight simulator, black box recorder, and evidence generator for adaptive routing decisions, while keeping production claims bounded by explicit not-proven limits.

[`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) names ADR-0003 as the evidence-as-architecture-artifact ADR. [`ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) defines the proposed future layered architecture boundary, and [`ADR-0002_LASE_INTEGRATION_MODEL.md`](ADR-0002_LASE_INTEGRATION_MODEL.md) defines the proposed future LASE integration model.

LoadBalancerPro already has current reviewer evidence, decision replay metadata, local lab evidence lanes, and reviewer handoff concepts. This ADR documents the future architecture role for evidence without adding new runtime behavior.

This ADR is planning-only. This does not implement `EvidencePacket`. No EvidencePacket implementation is introduced. This does not implement `EvidenceAssembler`. No EvidenceAssembler implementation is introduced. This does not add report generation. No report generation is introduced. This does not add JSON output. No JSON output is introduced. This does not add storage, persistence, telemetry, or audit log implementation. No storage/persistence/telemetry/audit log implementation is introduced. This does not add replay execution. No replay execution is introduced. This does not change routing, scoring, strategy, proxy, or API behavior. No routing/scoring/strategy/proxy/API behavior changes are introduced. This does not claim correctness validation, replay proof, production readiness, or production certification.

## Decision

If a future implementation sprint is separately approved, evidence should be treated as a first-class architecture concern that explains adaptive routing decisions, comparisons, limitations, and reviewer context without becoming hidden production routing authority.

This proposed decision is not accepted implementation. It records a target evidence model for reviewers:

- evidence should explain what decision or comparison was made;
- evidence should explain why it happened and what inputs were considered;
- evidence should state limitations and not-proven boundaries;
- evidence should support deterministic review where practical;
- evidence should remain reviewer metadata unless future policy gates separately approve runtime behavior;
- evidence must not certify correctness, production safety, production readiness, or production certification.

## Conceptual Evidence Role

Evidence is a future first-class architecture concern.

Future evidence should explain what decision or comparison was made, why it happened, what inputs were considered, and what limitations remain. Evidence should be reviewer-readable and machine-readable if implemented later. Evidence should preserve not-proven boundaries. Evidence should support deterministic review where practical.

Evidence should never become hidden production routing authority. Evidence should not be treated as correctness proof by default. Evidence can help a reviewer understand behavior, but it must not silently promote a shadow recommendation, replay summary, or lab artifact into production control.

This ADR does not add new evidence generation. It gives future evidence work a vocabulary for artifacts, boundaries, determinism, privacy, and storage review.

## Future Evidence Artifact Types

Future evidence artifact types may include:

- reviewer summary;
- decision explanation;
- strategy comparison summary;
- LASE shadow evaluation summary;
- replay/comparison summary;
- workload/scenario context summary;
- policy gate summary;
- not-proven boundary summary;
- future EvidencePacket concept.

These artifact types are future architecture concepts only. This ADR does not create report files, JSON output, API fields, storage tables, audit logs, or export/download/share behavior.

## Future Evidence Packet Boundaries

`EvidencePacket` is future-only and not implemented in this sprint.

Future EvidencePacket boundaries:

- Future EvidencePacket must not include secrets.
- Future EvidencePacket must not include env var values.
- Future EvidencePacket must not include tokens, credentials, private network details, or absolute local machine paths.
- Future EvidencePacket IDs/timestamps/hashes must be deterministic or separately approved.
- Future EvidencePacket must include `notProvenBoundaries`.
- Future EvidencePacket must distinguish report-only/shadow/replay/lab outputs from production proof.
- Future EvidencePacket must not claim production certification.
- Future EvidencePacket must not replace human review.
- Future EvidencePacket must not replace package-boundary enforcement.

This ADR does not implement `EvidencePacket`, `EvidenceAssembler`, report generation, JSON output, storage, persistence, telemetry, audit log implementation, replay execution, upload/share/download/export behavior, or PR comment/artifact behavior.

## Reviewer Trust Relationship

Evidence should help reviewers understand behavior.

Future reviewer trust expectations:

- evidence should make limitations explicit;
- evidence should help compare strategies and LASE recommendations;
- evidence should support safe review before any future autonomy;
- evidence must not replace human review;
- evidence must not certify production safety;
- evidence must not hide the difference between current behavior, shadow output, replay/comparison output, report-only output, and future-only architecture targets.

Evidence is useful because it can make a decision understandable. It is unsafe if it is read as proof that production behavior is certified, complete, or correct.

## Auditability And Explainability Relationship

future evidence should be auditable.

Future auditability and explainability expectations:

- future evidence should explain dominant factors and decision context where available;
- future evidence should distinguish observed facts from derived summaries;
- future evidence should distinguish deterministic outputs from future contextual metadata;
- future evidence should avoid unstable generated IDs unless separately approved;
- future evidence should identify unavailable, unknown, stale, synthetic, or estimated inputs where applicable;
- future evidence should preserve enough context for reviewers to understand what was evaluated without exposing secrets.

Auditability does not imply durable audit log implementation in this sprint. Explainability does not imply correctness validation.

## LASE And Shadow Evaluation Relationship

Future LASE outputs may feed evidence summaries if separately implemented.

LASE evidence expectations:

- LASE evidence remains shadow/evaluation metadata unless a future policy gate and implementation path separately promote behavior;
- LASE evidence must not directly mutate live allocation;
- LASE evidence must not become production route authority;
- LASE evidence must preserve policy-gate context and not-proven boundaries;
- LASE evidence must keep reviewer metadata separate from production routing authority.

This ADR does not change the LASE integration model from ADR-0002. It only records that future evidence should be able to summarize LASE output without granting it hidden runtime control.

## Replay And Comparison Relationship

Replay/comparison evidence is future architecture context.

Future replay and comparison expectations:

- replay evidence is not replay proof;
- comparison evidence is not correctness validation;
- replay/comparison output must not mutate live routing state;
- replay/comparison output must not require live infrastructure mutation;
- replay/comparison output must not call CloudManager or future grid/facility/GPU/power control paths;
- replay/comparison output must keep assumptions, inputs, and limitations visible to reviewers.

This ADR does not add replay execution, what-if mutation, report generation, JSON output, storage, persistence, telemetry, or audit log implementation.

## Determinism Expectations

Future evidence should be deterministic where practical.

Future determinism expectations:

- evidence should avoid unstable timestamps unless separately approved;
- evidence should avoid UUID/random/hash identifiers unless separately approved;
- evidence ordering should be stable if implemented later;
- evidence summaries should be reproducible where practical;
- evidence should include source/context descriptions without exposing secrets;
- evidence should distinguish deterministic source fields from future contextual metadata;
- evidence should label unknown, unavailable, stale, synthetic, or estimated data rather than guessing.

These are future implementation expectations only. This ADR adds no MessageDigest/SHA, UUID, random, time, environment, system-property behavior, report generation, JSON output, replay execution, or persistence.

## Integrity And Privacy Expectations

Future evidence integrity should make tampering, provenance, and scope reviewable without overclaiming proof.

Future integrity and privacy expectations:

- evidence should not include secrets, tokens, credentials, environment variable values, private network details, or absolute local machine paths;
- evidence should avoid real reviewer names unless separately approved;
- evidence should use deterministic identifiers only if separately reviewed;
- evidence should distinguish tamper-evidence concepts from correctness proof;
- evidence should include privacy and secret-safety review before any future storage, export, download, share, PR comment, artifact, or audit log implementation;
- evidence should avoid leaking external system details through future signal context.

This ADR does not add integrity checks, checksums, hashes, signatures, storage, telemetry, or audit log behavior.

## Future Storage And Retention Boundaries

No storage or persistence is implemented in this sprint.

Future storage and retention expectations:

- future retention requires separate ADR/PR;
- future storage must address privacy, secrets, scope, lifecycle, and deletion/retention policy;
- future audit log implementation must be separately approved;
- future export/download/share behavior must be separately approved;
- future evidence retention must not imply production certification;
- future stored evidence must not become hidden production routing authority.

This ADR does not add database tables, files, generated artifacts, upload endpoints, download endpoints, share endpoints, PDF/ZIP generation, PR comments, or GitHub artifact behavior.

## Safety And Non-Goals

This ADR is documentation and documentation guard tests only.

Non-goals:

- no production Java runtime behavior;
- no records/classes/interfaces/enums under `src/main/java`;
- no `EvidencePacket`;
- no `EvidenceAssembler`;
- no report generation;
- no JSON output;
- no storage, persistence, telemetry, or audit log implementation;
- no replay execution;
- no `LaseObservationPort`;
- no class renames;
- no package moves or refactors;
- no source scanning logic;
- no ArchUnit or any new dependency;
- no Maven build changes;
- no allowlist files;
- no JSON/YAML/TOML output;
- no dry-run command;
- no CI workflow changes;
- no PR comment/report artifact behavior;
- no runtime naming enforcement;
- no package-boundary enforcement;
- no ExternalSignalPort implementation;
- no WorkloadProfile implementation;
- no ScenarioGenerator implementation;
- no observability, telemetry, storage, or persistence;
- no external API clients;
- no HTTP calls;
- no secrets, tokens, environment variables, credentials, config, or properties;
- no MessageDigest, SHA, UUID, random, time, environment, or system-property behavior;
- no what-if mutation;
- no upload/share/download/export/PDF/ZIP behavior;
- no Docker, CI, release, signing, registry, or governance changes;
- no proxy behavior change;
- no strategy behavior change;
- no core routing behavior change;
- no scoring-internals behavior change;
- no API behavior change;
- no production behavior change.

This ADR does not claim ADR approval, EvidencePacket implementation, EvidenceAssembler implementation, report generation implementation, JSON output, storage/persistence, replay execution, LASE integration implementation, `LaseObservationPort` implementation, ExternalSignalPort implementation, WorkloadProfile implementation, runtime LASE enforcement, package-boundary enforcement, source-name guard implementation, routing/scoring/strategy/proxy/API behavior changes, production readiness, production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, or facility automation.

## Consequences

Positive consequences:

- reviewers get one proposed ADR for how evidence should relate to reviewer trust, auditability, explainability, deterministic comparison, LASE output, replay/comparison output, and future EvidencePacket boundaries;
- future evidence work can be reviewed without confusing evidence metadata with production route authority;
- future EvidencePacket design can begin with privacy, determinism, and not-proven boundaries;
- future storage, retention, export, and audit log work stays separate from evidence vocabulary;
- replay and comparison claims remain separate from proof, correctness validation, and certification claims.

Costs and risks:

- this ADR adds documentation to maintain;
- evidence terminology may be overread as implementation unless not-proven boundaries remain explicit;
- future implementation will still require focused tests, privacy review, deterministic-output review, rollback review, and storage/retention review;
- existing evidence behavior remains unchanged until separately scoped work is approved.

## Relationship To Existing Docs

Related docs:

- [`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) names ADR-0003 as a proposed Phase 0 ADR.
- [`ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) defines the proposed future layer boundary that separates allocation, LASE, evidence, infrastructure, API, config, and docs/tests.
- [`ADR-0002_LASE_INTEGRATION_MODEL.md`](ADR-0002_LASE_INTEGRATION_MODEL.md) defines the proposed future LASE integration model.
- [`ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md`](ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md) drafts the proposed future workload realism and scenario modeling model as planning-only guidance for WorkloadProfile, ScenarioGenerator, workload categories, trace-safety, and deterministic scenario boundaries.
- [`ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md`](ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md) drafts the proposed future safety boundaries and guardrails model as planning-only guidance for decision safety evidence, safety modes, rollback/stop conditions, and guardrails before autonomy.
- [`ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md`](ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md) narrows the proposed future EvidencePacket, EvidenceAssembler, replay-facing evidence, filesystem/artifact, and not-proven boundary model before any implementation exists.
- [`../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md) maps the architecture report's evidence phase to current docs and future-only boundaries.
- [`../REVIEWER_TRUST_MAP.md`](../REVIEWER_TRUST_MAP.md) provides reviewer navigation and evidence boundaries.
- [`../ENTERPRISE_READINESS_AUDIT.md`](../ENTERPRISE_READINESS_AUDIT.md) records readiness posture and not-proven boundaries.
- [`../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md`](../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md) summarizes current decision replay evidence boundaries.
- [`../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_GUIDANCE.md`](../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_GUIDANCE.md) gives current reviewer guidance for decision replay evidence.
- [`../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_HANDOFF_SUMMARY.md`](../ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_HANDOFF_SUMMARY.md) describes current reviewer handoff evidence summaries.
- [`../SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md`](../SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md) documents future report schema vocabulary for source-name guard planning.

This ADR does not supersede those docs. It provides a proposed decision frame for future evidence-as-architecture work.

## Not-Proven Boundaries

Remaining not-proven boundaries:

- not production-ready;
- not production-certified;
- not live-cloud validated;
- not real-tenant validated;
- not GPU orchestration;
- not power/grid control;
- not carbon-aware routing implementation;
- not facility automation;
- ExternalSignalPort not implemented;
- WorkloadProfile signal metadata not implemented;
- WorkloadProfile implementation not added;
- ScenarioGenerator implementation not added;
- EvidencePacket implementation not added;
- EvidenceAssembler implementation not added;
- LaseObservationPort not added;
- replay execution not added;
- evidence/report generation not added;
- storage/persistence not added;
- LASE boundary not runtime-enforced yet;
- LASE package boundary not enforced yet;
- ArchUnit/package-boundary tooling not added yet;
- Source-name guard not implemented yet;
- Architecture report alignment index is not implementation;
- Phase 0 ADR index is not implementation;
- ADR-0001 is proposed/planning-only;
- ADR-0002 is proposed/planning-only;
- ADR-0003 is proposed/planning-only.

## Reviewer-Facing Value

ADR-0003 gives reviewers a single place to inspect the intended future evidence architecture before any EvidencePacket, EvidenceAssembler, report generation, JSON output, replay execution, storage, telemetry, persistence, audit log, or behavior change exists.

Reviewer value:

- the conceptual evidence role is explicit;
- future evidence artifact types are named without creating artifacts;
- future EvidencePacket boundaries are privacy-safe and not-proving;
- reviewer trust, auditability, and explainability expectations are separated from proof claims;
- LASE, shadow evaluation, replay, and comparison evidence stay separate from live routing authority;
- determinism, integrity, privacy, storage, and retention expectations are documented before implementation pressure appears.

Reviewers should treat this ADR as proposed/planning-only. It is not ADR approval, not EvidencePacket implementation, not EvidenceAssembler implementation, not report generation, not JSON output, not storage/persistence/telemetry/audit log implementation, not replay execution, not runtime LASE enforcement, not package-boundary enforcement, not production-readiness proof, and not production-certification proof.
