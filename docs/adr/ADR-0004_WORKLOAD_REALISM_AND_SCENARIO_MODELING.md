# ADR-0004 Workload Realism And Scenario Modeling

## Status

Proposed / planning-only.

Decision type: architecture planning.

Implementation status: not implemented.

## Date

2026-05-21.

## Context

The uploaded architecture report recommends workload realism and scenario modeling as a major architecture concern. It calls out AI-era workloads, bursty and tail-heavy request patterns, inference-like scenarios, strategy comparison, LASE evaluation, and evidence generation as future review topics.

[`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) names ADR-0004 as the workload realism and scenario modeling ADR. [`ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) defines the proposed future layered architecture boundary. [`ADR-0002_LASE_INTEGRATION_MODEL.md`](ADR-0002_LASE_INTEGRATION_MODEL.md) defines the proposed future LASE integration model. [`ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md`](ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md) defines the proposed future evidence architecture model.

LoadBalancerPro already has bounded local scenarios, strategy comparison, LASE/shadow concepts, and reviewer evidence documentation. This ADR documents how future workload realism and scenario modeling should be reviewed without adding runtime behavior.

This ADR is planning-only. This does not implement WorkloadProfile. No WorkloadProfile implementation is introduced. This does not implement ScenarioGenerator. No ScenarioGenerator implementation is introduced. This does not add workload generators. No workload generator implementation is introduced. This does not add trace import. No trace import is introduced. This does not add replay execution. No replay execution is introduced. This does not add telemetry, storage, or persistence. No telemetry/storage/persistence implementation is introduced. This does not add EvidencePacket or report generation. No EvidencePacket/report generation implementation is introduced. This does not add JSON output. No JSON output is introduced. This does not change routing, scoring, strategy, proxy, or API behavior. No routing/scoring/strategy/proxy/API behavior changes are introduced. This does not claim production readiness or production certification.

## Decision

If a future implementation sprint is separately approved, workload realism and scenario modeling should be treated as first-class architecture concerns for evaluating adaptive routing strategies under meaningful, reviewer-understandable conditions.

This proposed decision is not accepted implementation. It records a target review model for future workload and scenario work:

- workload realism should improve strategy comparison and reviewer evidence quality if implemented later;
- scenario modeling should keep steady, bursty, degraded, tail-heavy, recovery, and inference-like conditions explicit;
- workload context should remain lab, shadow, and reviewer metadata unless future policy gates separately promote behavior;
- workload modeling must not become hidden production traffic control;
- clean scenario results must not be read as production safety proof;
- not-proven boundaries must remain visible.

## Conceptual Workload Realism Role

Workload realism is a future architecture concern for evaluating routing strategies under meaningful conditions.

Scenario modeling should help reviewers understand how strategies and LASE behave under steady, bursty, degraded, tail-heavy, recovery, and inference-like conditions. Workload realism should improve strategy comparison and reviewer evidence quality if implemented later.

Workload modeling must not become production traffic control by itself. Scenario output must remain lab/shadow/reviewer context unless separately approved future implementation promotes behavior through policy gates. Workload realism must preserve not-proven boundaries.

Scenario modeling does not prove live-cloud validation. Scenario modeling does not prove real-tenant validation. Clean scenario results are not production safety proof.

This ADR does not add workload model code, workload generator code, trace import, replay execution, evidence generation, report generation, API fields, telemetry, storage, persistence, routing behavior, scoring behavior, strategy behavior, proxy behavior, or production behavior.

## Future WorkloadProfile Boundaries

`WorkloadProfile` is future-only and not implemented in this sprint.

Future WorkloadProfile boundaries:

- Future WorkloadProfile should describe workload shape/context, not mutate routing state.
- Future WorkloadProfile may include workload labels, traffic shape, burst hints, tail-latency sensitivity, degradation context, and future signal metadata if separately approved.
- Future WorkloadProfile must not include secrets, credentials, tokens, env var values, private network details, or absolute local machine paths.
- Future WorkloadProfile identifiers/timestamps/hashes must be deterministic or separately approved.
- Future WorkloadProfile must not claim production validation or real-tenant validation.
- Future WorkloadProfile must not become hidden scoring, routing, strategy, proxy, or production behavior.
- Future WorkloadProfile must degrade to explicit unknown, unavailable, stale, synthetic, or estimated labels where appropriate.

This ADR does not add WorkloadProfile records, classes, interfaces, enums, API fields, config, properties, schema files, or fixture generation.

## Future ScenarioGenerator Boundaries

`ScenarioGenerator` is future-only and not implemented in this sprint.

Future ScenarioGenerator boundaries:

- Future ScenarioGenerator may create deterministic lab scenarios for review if separately approved.
- Future ScenarioGenerator must not generate live traffic.
- Future ScenarioGenerator must not mutate cloud, production, proxy, routing, scoring, or infrastructure state.
- Future ScenarioGenerator must not require live-cloud or real-tenant access.
- Future ScenarioGenerator must not claim correctness validation.
- Future ScenarioGenerator must not publish artifacts, upload reports, create PR comments, or write generated output unless separately approved.
- Future ScenarioGenerator must not use UUID, random, time, environment, system-property, MessageDigest, SHA, or hash behavior unless separately approved.

This ADR does not add ScenarioGenerator classes, workload generators, trace import, replay execution, JSON/YAML/TOML output, storage, telemetry, persistence, or generated scenario artifacts.

## Future Workload/Scenario Categories

Future workload and scenario categories may include:

- steady baseline;
- bursty load;
- tail-heavy latency;
- overload pressure;
- all-unhealthy degradation;
- recovery;
- mixed steady-plus-bursty;
- inference-like request spikes;
- external signal context, future-only;
- reviewer evidence scenario, future-only.

These categories are future architecture vocabulary only. They do not create a generator, trace importer, replay command, report schema, JSON output, API response, or runtime workload model.

## Relationship To LASE And Shadow Evaluation

Future workload profiles may provide context for LASE shadow evaluation.

Future LASE and workload expectations:

- LASE may compare behavior across scenarios if implemented later.
- LASE outputs based on workload profiles remain reviewer/evidence metadata unless future policy gates separately promote behavior.
- Workload context must not grant LASE live routing authority.
- Missing or stale workload context must degrade safely if implemented later.
- LASE must not infer production certification, live-cloud validation, real-tenant validation, or correctness proof from scenario labels.
- Workload context must not grant LASE GPU, grid, facility, power, carbon-aware routing, proxy mutation, or CloudManager control.

This ADR does not change ADR-0002. It only records how future workload realism should stay separated from live allocation and runtime LASE enforcement.

## Relationship To Strategy Comparison

Future workload/scenario modeling may support side-by-side strategy comparison.

Future strategy comparison expectations:

- comparison output is not correctness validation;
- workload-driven comparisons must not mutate live allocation;
- strategy comparison must remain deterministic where practical;
- scenario labels must be reviewer-readable and stable;
- any production use requires separate ADR and implementation PR;
- workload-driven comparison must not hide routing, scoring, strategy, or proxy authority.

This ADR does not change current strategy comparison behavior, scoring internals, strategy selection, proxy behavior, API behavior, or production behavior.

## Relationship To Evidence And Reviewer Metadata

Future workload context may appear in evidence summaries.

Future evidence and reviewer metadata expectations:

- evidence must state workload/scenario limitations;
- evidence must distinguish synthetic/lab/shadow context from production validation;
- evidence must not claim real-tenant validation unless separately proven;
- clean scenario results are not production safety proof;
- reviewer metadata must not become production routing authority;
- evidence must distinguish workload assumptions from observed facts and derived summaries.

This ADR does not add EvidencePacket implementation, EvidenceAssembler implementation, report generation, JSON output, storage, persistence, telemetry, audit log implementation, upload/share/download/export behavior, or PR comment/artifact behavior.

## Relationship To Replay And Deterministic Validation

Replay support is future-only unless already existing in current repo docs.

Future replay and deterministic validation expectations:

- replay evidence is not replay proof;
- deterministic validation is future architecture guidance, not current implementation;
- replay/comparison output must not mutate live routing state;
- trace import/replay must avoid secrets and private data if implemented later;
- replay/comparison output must not require live infrastructure mutation;
- replay/comparison output must not claim live-cloud validation, real-tenant validation, correctness validation, production readiness, or production certification.

This ADR does not add replay execution, trace import, what-if mutation, generated reports, JSON output, storage, persistence, telemetry, or audit logs.

## Determinism Expectations

Future scenario generation should be deterministic where practical.

Future determinism expectations:

- future scenario ordering should be stable;
- future workload labels should be stable and reviewer-readable;
- avoid UUID/random/time/env/system-property behavior unless separately approved;
- generated scenario metadata should be reproducible where practical;
- deterministic output must not be confused with correctness proof;
- unavailable, unknown, stale, synthetic, or estimated context should be labeled rather than guessed.

These are future implementation expectations only. This ADR adds no MessageDigest/SHA, UUID, random, time, environment, system-property behavior, trace import, replay execution, workload generation, report generation, JSON output, or persistence.

## Privacy And Trace-Safety Expectations

Future trace import requires separate ADR/PR.

Future privacy and trace-safety expectations:

- future trace import must avoid real secrets;
- future trace import must avoid env values, tokens, credentials, private network details, and local machine paths;
- future traces must be anonymized or synthetic unless separately approved;
- future trace retention/deletion requires separate review;
- trace import must not imply real-tenant validation unless separately proven;
- workload profiles and scenario metadata must avoid real reviewer names unless separately approved;
- workload profiles and scenario metadata must not expose customer, tenant, private network, or local machine details.

This ADR does not add trace import, trace storage, telemetry import, persistence, retention policy, deletion policy, external clients, HTTP calls, or secret handling behavior.

## Future Import/Replay Boundaries

No import tooling is added in this sprint. No replay execution is added in this sprint.

Future import/replay boundaries:

- future import/replay must be lab-safe;
- future import/replay must not mutate production infrastructure;
- future import/replay must not publish artifacts unless separately approved;
- future import/replay must not certify production safety;
- future import/replay must not require live-cloud or real-tenant access;
- future import/replay must not become hidden production routing authority;
- future import/replay must not bypass policy gates.

Any future import/replay implementation requires separate scoped ADR/PR review, deterministic tests, privacy review, rollback review, and explicit no-overclaim documentation.

## Safety And Non-Goals

This ADR is documentation and documentation guard tests only.

Non-goals:

- no production Java runtime behavior;
- no records/classes/interfaces/enums under `src/main/java`;
- no WorkloadProfile;
- no ScenarioGenerator;
- no workload generators;
- no trace import;
- no replay execution;
- no EvidencePacket;
- no EvidenceAssembler;
- no report generation;
- no JSON output;
- no storage, persistence, telemetry, or audit log implementation;
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

This ADR does not claim ADR approval, WorkloadProfile implementation, ScenarioGenerator implementation, workload generator implementation, trace import, replay execution, EvidencePacket implementation, EvidenceAssembler implementation, report generation implementation, JSON output, storage/persistence, LASE integration implementation, `LaseObservationPort` implementation, ExternalSignalPort implementation, runtime LASE enforcement, package-boundary enforcement, source-name guard implementation, routing/scoring/strategy/proxy/API behavior changes, production readiness, production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, or facility automation.

## Consequences

Positive consequences:

- reviewers get one proposed ADR for how workload realism should relate to future WorkloadProfile and ScenarioGenerator concepts;
- future scenario work can be reviewed without confusing lab/shadow context with production traffic control;
- AI-like, bursty, tail-heavy, degraded, recovery, and inference-like workload language is available before runtime implementation pressure appears;
- future workload context can be tied to LASE, strategy comparison, evidence, replay, determinism, privacy, and trace-safety boundaries;
- clean scenario results remain separate from production proof, live-cloud validation, real-tenant validation, and correctness validation.

Costs and risks:

- this ADR adds documentation to maintain;
- workload realism terminology may be overread as implementation unless not-proven boundaries remain explicit;
- future WorkloadProfile and ScenarioGenerator implementation still require separate tests, privacy review, deterministic-output review, rollback review, and API/behavior impact review;
- existing workload, scenario, strategy, LASE, and evidence behavior remains unchanged until separately scoped work is approved.

## Relationship To Existing Docs

Related docs:

- [`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) names ADR-0004 as a proposed Phase 0 ADR.
- [`ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) defines the proposed future layer boundary that separates allocation, LASE, evidence, infrastructure, API, config, and docs/tests.
- [`ADR-0002_LASE_INTEGRATION_MODEL.md`](ADR-0002_LASE_INTEGRATION_MODEL.md) defines the proposed future LASE integration model.
- [`ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md`](ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md) drafts the proposed future evidence architecture model.
- [`ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md`](ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md) drafts the proposed future safety boundaries and guardrails model as planning-only guidance for how workload/scenario evidence supports safety review without becoming production proof.
- [`ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md`](ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md) drafts the proposed future evidence packet and replay boundary model as planning-only guidance for how workload/scenario evidence may be represented without implementing replay execution, report generation, storage, persistence, filesystem-writing, or export/upload/download/PDF/ZIP behavior.
- [`ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md`](ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md) drafts the proposed future reviewer evidence and trust model as planning-only guidance for how workload/scenario evidence should remain explainable, non-certifying, and separated from runtime authority.
- [`ADR-0008_RUNTIME_ENFORCEMENT_AND_PACKAGE_BOUNDARY_PLAN.md`](ADR-0008_RUNTIME_ENFORCEMENT_AND_PACKAGE_BOUNDARY_PLAN.md) drafts the proposed future runtime enforcement and package-boundary plan as planning-only guidance for keeping future WorkloadProfile, ScenarioGenerator, replay/scenario evaluation, and traffic mutation authority separated until separately implemented.
- [`ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md`](ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md) drafts the proposed future Local Lab Kit and simulated datacenter test harness plan for partial degradation, p95/p99 tail latency, overload, error-prone backend, recovery, and local hardware expansion scenarios before workload generators or trace import exist.
- [`../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md) maps the architecture report's workload phase to current docs and future-only boundaries.
- [`../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md`](../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md) defines future WorkloadProfile signal metadata.
- [`../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md`](../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md) defines the future read-only ExternalSignalPort contract.
- [`../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md) defines the future LASE/live allocation boundary contract.
- [`../REVIEWER_TRUST_MAP.md`](../REVIEWER_TRUST_MAP.md) provides reviewer navigation and evidence boundaries.
- [`../ENTERPRISE_READINESS_AUDIT.md`](../ENTERPRISE_READINESS_AUDIT.md) records readiness posture and not-proven boundaries.
- [`../THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md`](../THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md) frames current Tier 1 routing focus and future Tier 2/Tier 3 signal concepts.
- [`../ENTERPRISE_LAB_DECISION_REPLAY_READINESS_CHECKLIST.md`](../ENTERPRISE_LAB_DECISION_REPLAY_READINESS_CHECKLIST.md) documents read-only replay-readiness checklist status without replay execution.

This ADR does not supersede those docs. It provides a proposed decision frame for future workload realism and scenario modeling work.

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
- workload generator implementation not added;
- trace import not added;
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
- ADR-0003 is proposed/planning-only;
- ADR-0004 is proposed/planning-only.

## Reviewer-Facing Value

ADR-0004 gives reviewers a single place to inspect the intended future workload realism and scenario modeling architecture before any WorkloadProfile, ScenarioGenerator, workload generator, trace import, replay execution, EvidencePacket, report generation, JSON output, telemetry, storage, persistence, or behavior change exists.

Reviewer value:

- workload realism is framed as reviewer/lab context, not production traffic control;
- future WorkloadProfile and ScenarioGenerator boundaries are explicit;
- steady, bursty, tail-heavy, degraded, recovery, and inference-like scenario vocabulary is available before implementation;
- LASE, strategy comparison, evidence, replay, determinism, privacy, and trace-safety relationships are documented;
- clean scenario results, replay evidence, and comparison output remain separate from proof and certification claims;
- future implementation pressure starts from explicit no-overclaim boundaries.

Reviewers should treat this ADR as proposed/planning-only. It is not ADR approval, not WorkloadProfile implementation, not ScenarioGenerator implementation, not workload generator implementation, not trace import, not replay execution, not EvidencePacket implementation, not EvidenceAssembler implementation, not report generation, not JSON output, not storage/persistence/telemetry/audit log implementation, not runtime LASE enforcement, not package-boundary enforcement, not production-readiness proof, and not production-certification proof.
