# ADR-0002 LASE Integration Model

## Status

Proposed / planning-only.

Decision type: architecture planning.

Implementation status: not implemented.

## Date

2026-05-21.

## Context

The uploaded architecture report recommends a LASE integration model as a Phase 0 architecture decision. It frames LASE as the future shadow evaluation and experimentation core for adaptive routing experiments, while live allocation remains the runtime route selection path.

[`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) names ADR-0002 as the LASE integration model ADR. [`ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) defines the proposed future layered architecture boundary that separates allocation, LASE, evidence, infrastructure, API, config, and docs/tests concerns.

LoadBalancerPro already has current LASE, replay, scenario, evidence, reviewer metadata, and policy-gate concepts. This ADR documents how those concepts should relate in future architecture work without changing runtime behavior.

This ADR is planning-only. This does not implement runtime LASE enforcement. No runtime LASE enforcement is introduced. This does not add `LaseObservationPort`. No `LaseObservationPort` interface is added. This does not change live allocation. No live allocation behavior changes are introduced. This does not change routing, scoring, strategy, proxy, or API behavior. No routing/scoring/strategy/proxy/API behavior changes are introduced. This does not add replay execution. This does not add evidence packet implementation. This does not add ExternalSignalPort implementation. This does not add WorkloadProfile implementation. This does not claim production readiness or certification.

## Decision

If a future implementation sprint is separately approved, LASE should integrate with LoadBalancerPro as a shadow/evaluation and reviewer-evidence path that can observe, compare, explain, and produce evidence without directly controlling live routing.

This proposed decision is not accepted implementation. It records a target integration model for reviewers:

- live allocation remains the runtime route selection path;
- LASE remains shadow/evaluation/reviewer evidence unless future policy gates explicitly authorize a separately reviewed decision path;
- LASE must not become hidden production routing authority;
- future observation and signal context must begin as read-only metadata;
- future promotion of LASE output into live behavior requires separate ADR review, implementation review, deterministic tests, and explicit rollback boundaries.

## Conceptual LASE Role

Conceptually, LASE is the future shadow/evaluation path for adaptive routing experiments.

LASE may observe, compare, explain, and produce evidence in future approved implementations. LASE must remain separated from live allocation mutation. LASE must not become hidden production routing authority. LASE output must be reviewer/evidence metadata unless future policy gates explicitly promote a decision path.

LASE should support deterministic comparison and replay semantics when implemented later. Determinism means reviewers can understand which inputs were observed, which strategy outputs were compared, and which evidence labels were produced without relying on unstable timestamps, random identifiers, hidden external calls, or live infrastructure mutation.

This ADR does not add new LASE behavior. It gives future LASE implementation work a vocabulary for integration surfaces and safety boundaries.

## Integration Surfaces, Future-Only

Future-only integration surfaces:

- read-only observation snapshots from allocation;
- strategy comparison inputs;
- WorkloadProfile context, if implemented later;
- ExternalSignalPort context, if implemented later;
- policy gate decisions;
- replay/evidence artifacts;
- reviewer metadata summaries;
- audit/review docs.

These surfaces are not created or changed by this ADR. They are future-only architecture concepts. Any future Java interface, API response, report artifact, replay command, policy gate expansion, or package-boundary enforcement must be separately reviewed.

## Live Allocation Relationship

Live allocation remains the runtime route selection path.

Future LASE integration must preserve these rules:

- LASE must not directly mutate live routing state;
- LASE must not directly select production routes;
- LASE must not directly alter proxy behavior;
- LASE must not bypass policy gates;
- LASE must not directly call allocation mutation APIs;
- any future promotion from LASE to live behavior requires separate ADR and implementation PR.

This ADR does not change live allocation behavior, routing behavior, scoring behavior, strategy behavior, proxy behavior, API behavior, or production behavior.

## Shadow Evaluation Relationship

LASE may compare strategies in shadow-only contexts if a future implementation is separately approved.

Future shadow evaluation expectations:

- LASE may compare strategies in shadow-only contexts;
- LASE may produce recommendation metadata;
- LASE findings are review signals, not runtime authority;
- shadow evaluation must be deterministic where practical;
- shadow evaluation must preserve not-proven boundaries;
- shadow outputs must distinguish observed, estimated, synthetic, unavailable, unknown, shadow-only, and gated-lab evidence labels where applicable.

Shadow evaluation does not certify runtime safety, correctness, production readiness, or production certification.

## Replay And Comparison Relationship

Replay support is future-only unless already existing in current docs and current behavior. This ADR does not add replay execution.

Future replay and comparison expectations:

- replay evidence is not replay proof;
- comparison output is not correctness proof;
- replay/comparison must not mutate live routing state;
- replay/comparison must not require external/live infrastructure mutation;
- replay/comparison must not call CloudManager or future grid/facility/GPU/power control paths;
- replay/comparison must keep input assumptions visible to reviewers.

Replay and comparison output may help reviewers inspect a scenario. They do not prove that production runtime behavior is safe, certified, or validated in live cloud or real-tenant environments.

## Evidence And Reviewer Metadata Relationship

LASE outputs may feed future evidence/reviewer metadata if separately implemented.

Future evidence and reviewer metadata expectations:

- evidence metadata must state limitations;
- evidence metadata must not claim production certification;
- evidence metadata must not claim correctness validation;
- reviewer metadata must remain separate from production routing authority;
- reviewer metadata must not hide live route selection or policy-gate decisions;
- evidence summaries must preserve not-proven boundaries.

This ADR does not add EvidencePacket implementation, report generation, JSON/YAML/TOML output, telemetry, storage, persistence, upload/share/download/export behavior, or PR comment/artifact behavior.

## Policy Gate Relationship

Future policy gates define whether LASE outputs are visible, shadow-only, or eligible for promotion.

Policy gate expectations:

- no promotion behavior is implemented in this sprint;
- future policy gate changes require separate scoped PR;
- policy gate docs must not hide runtime authority;
- future promotion must be explicit, deterministic, auditable, and reversible;
- future promotion must not be combined with production-readiness or production-certification claims.

This ADR does not change the current controlled policy gate behavior. It only records how future LASE integration should be reviewed.

## Future Observation Port Relationship

`LaseObservationPort` is a future read-only concept only.

Observation port expectations:

- no observation port interface is added in this sprint;
- future observation snapshots must be read-only;
- future observation snapshots must not expose secrets;
- future observation snapshots must not include unstable IDs unless separately approved;
- future observation snapshots must not expose mutation handles;
- future observation snapshots must not grant LASE direct allocation, proxy, CloudManager, facility, grid, GPU, power, or production control.

This ADR does not add `LaseObservationPort`, package skeletons, Java ports, runtime interfaces, API fields, source scanning, ArchUnit rules, or package-boundary enforcement.

## Future External Signal Relationship

ExternalSignalPort is future-only and read-only.

Future external signal expectations:

- LASE must not directly control GPU/grid/facility/power systems;
- future signal context must not grant mutation authority;
- missing/stale external signals must degrade safely if implemented later;
- future signal context must distinguish observed, estimated, synthetic, unavailable, unknown, and stale values;
- future signal context must not add external clients, HTTP calls, telemetry, storage, persistence, secrets, environment variables, credentials, config, or properties in this sprint.

This ADR does not add ExternalSignalPort implementation, WorkloadProfile implementation, runtime signal ingestion, GPU orchestration, power/grid control, carbon-aware routing implementation, facility automation, or production behavior.

## Determinism And Replayability Expectations

Future LASE integration should be deterministic and replayable where practical.

Future expectations:

- inputs should be explicit and reviewable;
- ordering should be stable when reports or evidence are produced;
- unavailable or stale context should be labeled rather than guessed;
- comparison semantics should be explainable to reviewers;
- replayable evidence should separate reconstruction, comparison, and proof claims;
- deterministic evidence should avoid unstable timestamps, random IDs, UUIDs, hashes, environment values, system-property behavior, private network details, secrets, and local machine paths unless separately approved.

These are future implementation expectations only. This ADR adds no replay execution, no report generation, no JSON/YAML/TOML output, no MessageDigest/SHA/UUID/random/time/env/system-property behavior, and no persistence.

## Safety And Non-Goals

This ADR is documentation and documentation guard tests only.

Non-goals:

- no production Java runtime behavior;
- no records/classes/interfaces/enums under `src/main/java`;
- no `LaseObservationPort`;
- no class renames;
- no package moves or refactors;
- no source scanning logic;
- no ArchUnit or any new dependency;
- no Maven build changes;
- no allowlist files;
- no JSON/YAML/TOML output;
- no dry-run command or report generation;
- no CI workflow changes;
- no PR comment/report artifact behavior;
- no runtime naming enforcement;
- no package-boundary enforcement;
- no ExternalSignalPort implementation;
- no WorkloadProfile implementation;
- no EvidencePacket implementation;
- no ScenarioGenerator implementation;
- no observability, telemetry, storage, or persistence;
- no external API clients;
- no HTTP calls;
- no secrets, tokens, environment variables, credentials, config, or properties;
- no MessageDigest, SHA, UUID, random, time, environment, or system-property behavior;
- no replay execution;
- no what-if mutation;
- no upload/share/download/export/PDF/ZIP behavior;
- no Docker, CI, release, signing, registry, or governance changes;
- no proxy behavior change;
- no strategy behavior change;
- no core routing behavior change;
- no scoring-internals behavior change;
- no API behavior change;
- no production behavior change.

This ADR does not claim ADR approval, LASE integration implementation, runtime LASE enforcement, `LaseObservationPort` implementation, ExternalSignalPort implementation, WorkloadProfile implementation, ScenarioGenerator implementation, EvidencePacket implementation, package-boundary enforcement, source-name guard implementation, production readiness, production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, or facility automation.

## Consequences

Positive consequences:

- reviewers get one proposed ADR for how LASE should relate to live allocation, shadow evaluation, replay, evidence, policy gates, observation ports, and external signals;
- future LASE work can be reviewed without confusing shadow evidence with production routing authority;
- future observation port work can stay read-only by default;
- future external signal work can stay context-only before any scoring/routing influence;
- replay and comparison claims remain separate from proof, correctness, and certification claims.

Costs and risks:

- this ADR adds documentation to maintain;
- LASE integration terminology may still be overread as implementation unless not-proven boundaries remain explicit;
- future implementation will still require focused tests, package review, policy-gate review, and rollback review;
- existing LASE behavior remains unchanged until separately scoped work is approved.

## Relationship To Existing Docs

Related docs:

- [`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) names ADR-0002 as a proposed Phase 0 ADR.
- [`ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) defines the proposed future layer boundary that separates allocation, LASE, evidence, infrastructure, API, config, and docs/tests.
- [`ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md`](ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md) drafts the proposed future evidence architecture model as planning-only guidance for reviewer trust, auditability, explainability, deterministic comparison, and future EvidencePacket boundaries.
- [`ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md`](ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md) drafts the proposed future workload realism and scenario modeling model as planning-only guidance for WorkloadProfile, ScenarioGenerator, workload categories, trace-safety, and deterministic scenario boundaries.
- [`ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md`](ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md) drafts the proposed future safety boundaries and guardrails model as planning-only guidance for observe-only, recommendation, shadow, active-experiment, and manual-promotion boundaries.
- [`ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md`](ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md) drafts the proposed future evidence packet and replay boundary model as planning-only guidance for LASE evidence summaries, replay-facing evidence, `EvidencePacket`, `EvidenceAssembler`, and no-hidden-routing-authority boundaries.
- [`ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md`](ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md) drafts the proposed future reviewer evidence and trust model as planning-only guidance for LASE reviewer explanations, policy gate evidence, safety-mode evidence, and non-certifying trust boundaries.
- [`ADR-0008_RUNTIME_ENFORCEMENT_AND_PACKAGE_BOUNDARY_PLAN.md`](ADR-0008_RUNTIME_ENFORCEMENT_AND_PACKAGE_BOUNDARY_PLAN.md) drafts the proposed future runtime enforcement and package-boundary plan as planning-only guidance for keeping LASE/adaptive decision logic separate from live routing, evidence, external signals, reviewer explanation, and production traffic mutation.
- [`../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md) maps the architecture report's LASE phase to current docs and future-only boundaries.
- [`../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md) defines the future LASE/live allocation boundary contract.
- [`../LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md`](../LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md) maps current classes into future LASE boundary buckets.
- [`../LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md`](../LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md) stages future package-boundary enforcement.
- [`../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md`](../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md) defines the future read-only ExternalSignalPort contract.
- [`../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md`](../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md) defines future WorkloadProfile signal metadata.
- [`../REVIEWER_TRUST_MAP.md`](../REVIEWER_TRUST_MAP.md) provides reviewer navigation and evidence boundaries.
- [`../ENTERPRISE_READINESS_AUDIT.md`](../ENTERPRISE_READINESS_AUDIT.md) records readiness posture and not-proven boundaries.

This ADR does not supersede those docs. It provides a proposed decision frame for future LASE integration work.

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
- LASE boundary not runtime-enforced yet;
- LASE package boundary not enforced yet;
- ArchUnit/package-boundary tooling not added yet;
- Source-name guard not implemented yet;
- Architecture report alignment index is not implementation;
- Phase 0 ADR index is not implementation;
- ADR-0001 is proposed/planning-only;
- ADR-0002 is proposed/planning-only.

## Reviewer-Facing Value

ADR-0002 gives reviewers a single place to inspect the intended future LASE integration model before any observation port, package move, runtime enforcement, policy-gate expansion, or behavior change exists.

Reviewer value:

- LASE's conceptual role is explicit;
- live allocation stays separate from shadow evaluation;
- replay and comparison are not overstated as proof;
- evidence and reviewer metadata remain separate from production routing authority;
- future policy gates, observation ports, and external signals have clear read-only safety expectations;
- deterministic/replayable expectations are documented without implementing replay execution.

Reviewers should treat this ADR as proposed/planning-only. It is not implementation proof, not runtime LASE enforcement, not `LaseObservationPort` implementation, not package-boundary enforcement, not production-readiness proof, and not production-certification proof.
