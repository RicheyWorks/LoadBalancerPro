# ADR-0005 Safety Boundaries And Guardrails

## Status

Proposed / planning-only.

Decision type: architecture planning.

Implementation status: not implemented.

## Date

2026-05-21.

## Context

The uploaded architecture report recommends safety boundaries and guardrails as a core Phase 0 architecture decision. It frames LoadBalancerPro as a future adaptive routing experimentation and evidence platform that must avoid reckless, opaque, or unsafe traffic-control behavior as it moves from observe-only toward recommendation, shadow, and active-experiment modes.

[`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) names ADR-0005 as the safety boundaries and guardrails ADR. [`ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) defines the proposed future layered architecture boundary. [`ADR-0002_LASE_INTEGRATION_MODEL.md`](ADR-0002_LASE_INTEGRATION_MODEL.md) defines the proposed future LASE integration model. [`ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md`](ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md) defines the proposed future evidence architecture model. [`ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md`](ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md) defines the proposed future workload realism and scenario modeling model. [`ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md`](ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md) defines the proposed future evidence packet and replay boundary model.

LoadBalancerPro already has bounded lab modes, policy-gate language, reviewer evidence, safety documentation, and not-proven boundaries. This ADR documents the future safety boundary model without changing runtime behavior.

This ADR is planning-only. This does not implement runtime enforcement. No runtime enforcement is introduced. This does not implement runtime LASE enforcement. This does not add package-boundary enforcement. This does not add ArchUnit enforcement. This does not add replay execution. This does not add evidence/report generation. This does not add storage or persistence. This does not add workload generation. This does not add trace import. This does not add external signal ingestion. This does not change routing, scoring, strategy, proxy, API, config, Docker, CI, release, signing, registry, governance, or production behavior. This does not claim production readiness, production certification, live-cloud validation, or real-tenant validation.

## Decision

If a future implementation sprint is separately approved, adaptive traffic-control behavior should progress through explicit safety modes and guardrails before it can influence live traffic.

This proposed decision is not accepted implementation. It records a target safety model for future LASE and adaptive routing work:

- no automatic jump from observation to traffic-changing behavior;
- evidence and reviewer explanations are required before any future promotion;
- manual promotion only for movement toward traffic-changing behavior;
- policy gates and operator approval gates must be explicit;
- rollback, stop conditions, blast-radius limits, and confidence thresholds must be reviewable;
- safety modes must preserve not-proven boundaries;
- future active-experiment mode must not be represented as production certification.

## Conceptual Safety Boundary Role

Safety boundaries are a future architecture concern for keeping adaptive routing transparent, reversible, and reviewer-auditable.

The safety model exists to prevent future adaptive traffic control from becoming reckless, opaque, or unsafe. It should make it clear when LoadBalancerPro is only observing, when it is making a recommendation, when it is comparing in shadow, and when a tightly bounded active experiment may be considered.

Safety boundaries should support the LoadBalancerPro north-star vision for future datacenter adaptive traffic control while keeping guardrails before autonomy. The model should help reviewers reason about partial degradation, tail latency under pressure, safer adaptive routing, explainability, auditability, and the avoidance of black-box routing decisions.

This ADR does not make current routing safer by itself. It records the future safety vocabulary and review gates that must exist before any separately approved implementation can move closer to live traffic influence.

## Safety Mode Progression

The proposed future mode progression is:

1. observe-only;
2. recommendation;
3. shadow;
4. active-experiment;
5. manual promotion only.

No automatic jump from observation to traffic-changing behavior is allowed by this ADR. Future promotion between modes must require separate policy/operator gates, deterministic evidence review, and scoped implementation approval.

Mode progression expectations:

- observe-only may collect or expose review context but must not influence routing;
- recommendation may suggest actions but must not apply them automatically;
- shadow may evaluate hypothetical decisions beside real routing but must not mutate live traffic;
- active-experiment may allow tightly bounded experiments only after explicit policy and operator gates;
- manual promotion only means a reviewer/operator decision, not a hidden autonomous transition;
- rollback and stop conditions must be known before active-experiment behavior is eligible;
- mode labels must be visible in future evidence and reviewer metadata;
- mode labels must not imply production readiness, production certification, live-cloud validation, or real-tenant validation.

This ADR does not add modes, policy fields, API fields, config flags, persistence, report generation, runtime gates, or routing behavior.

## Guardrail Categories

Future guardrail categories should include:

- policy gates;
- operator approval / review gates;
- deterministic evidence requirements;
- scenario/replay evidence requirements;
- risk boundaries;
- rollback/stop conditions;
- blast-radius limits;
- confidence thresholds;
- source-name / signal provenance expectations;
- privacy and trace-safety constraints;
- no secret/env leakage;
- no unsafe external calls;
- no hidden autonomous mutation.

These categories are future planning vocabulary only. This ADR does not add policy gate implementation, operator approval workflow, report generation, replay execution, storage, persistence, external clients, HTTP calls, source scanning, or runtime mutation controls.

## Policy Gates

Future policy gates should define whether adaptive outputs are visible, recommendation-only, shadow-only, or eligible for tightly bounded active-experiment review.

Future policy gate expectations:

- policy gates must be explicit and reviewable;
- policy gates must distinguish observe-only, recommendation, shadow, and active-experiment modes;
- failed policy checks must be visible in future decision safety evidence;
- policy gates must not be hidden inside scoring, strategy, proxy, or API behavior;
- policy gates must not bypass operator approval where traffic-changing behavior is possible;
- policy gate changes require separate scoped PR review.

No policy-gate expansion is implemented by this ADR.

## Operator Approval And Review Gates

Future traffic-changing behavior should require explicit operator approval and reviewer-visible justification.

Future review gate expectations:

- reviewer/operator approval must be explicit before active-experiment behavior;
- approval must identify the safety mode, scope, reason, rollback plan, stop conditions, and blast-radius limit;
- approval must not be inferred from a clean report, passing test, or scenario comparison;
- approval must not be hidden inside config defaults;
- approval must not certify production safety;
- approval evidence must not include secrets, tokens, env var values, credentials, private network details, or absolute local machine paths.

This ADR does not add approval UI, approval storage, config properties, API commands, workflow changes, comments, artifacts, or external integrations.

## Deterministic Evidence Requirements

Future adaptive decisions should expose deterministic evidence where practical before they can be reviewed for promotion.

Future deterministic evidence expectations:

- evidence ordering should be stable;
- evidence summaries should be reproducible where practical;
- evidence should avoid UUID, random, time, environment, system-property, MessageDigest, SHA, or hash behavior unless separately approved;
- evidence should distinguish observed facts from derived summaries;
- evidence should distinguish deterministic lab/shadow context from production proof;
- deterministic evidence must not be confused with correctness validation.

This ADR does not add deterministic evidence generation, report generation, JSON output, replay execution, storage, persistence, or audit logs.

## Scenario And Replay Evidence Requirements

Future safety review may use scenario and replay evidence, but those outputs must remain bounded.

Future scenario/replay expectations:

- scenario evidence may support reviewer understanding;
- replay/comparison evidence may support deterministic review if implemented later;
- replay evidence is not replay proof;
- comparison output is not correctness validation;
- clean scenario results are not production safety proof;
- scenario and replay evidence must not mutate live routing state;
- scenario and replay evidence must not require live-cloud or real-tenant access;
- scenario and replay evidence must not publish artifacts unless separately approved.

This ADR does not add workload generation, trace import, replay execution, evidence/report generation, JSON/YAML/TOML output, upload/download/export/PDF/ZIP behavior, or storage.

## Risk Boundaries

Future adaptive routing safety review should define risk boundaries before traffic can be changed.

Future risk boundary expectations:

- risk boundaries should identify affected routes, strategy choices, backends, scenarios, and operator assumptions where available;
- risk boundaries should label unknown, stale, unavailable, synthetic, or estimated context;
- risk boundaries should identify what was not proven;
- risk boundaries should prevent source-name, workload, external signal, or evidence metadata from becoming hidden production routing authority;
- risk boundaries should fail closed when safety context is missing or ambiguous;
- risk boundaries should not claim live-cloud validation or real-tenant validation unless separately proven.

This ADR does not change risk handling in runtime code.

## Rollback And Stop Conditions

Future active-experiment behavior must have rollback and stop conditions before it can be considered.

Future rollback/stop expectations:

- rollback conditions must be documented before an active experiment is eligible;
- stop conditions must be documented before an active experiment is eligible;
- rollback must not require package moves or behavior refactors in the same sprint;
- stop conditions must be visible to reviewers/operators;
- stop conditions should include confidence loss, policy failure, stale signals, excessive uncertainty, unsafe source provenance, unexpected route impact, or operator cancellation where applicable;
- rollback and stop evidence must not be represented as production certification.

This ADR does not add rollback execution, stop-condition automation, runtime gates, storage, persistence, or config.

## Blast-Radius Limits

Future active experiments must have limited blast radius.

Future blast-radius expectations:

- scope must be narrow and explicit;
- traffic-changing behavior must not be repository-wide, cluster-wide, tenant-wide, or environment-wide by default;
- active-experiment scope should be bounded by route, scenario, tenant surrogate, lab profile, percentage, duration, or another separately approved stable limiter if implemented later;
- blast-radius limits must not depend on unstable timestamps or generated IDs unless separately approved;
- blast-radius limits must be reviewable before promotion;
- blast-radius limits must not imply production readiness.

This ADR does not add experiment execution, traffic shifting, runtime percentage control, timers, persistence, or production control.

## Confidence Thresholds

Future confidence thresholds should be reviewer-readable and conservative.

Future confidence expectations:

- thresholds must be explicit;
- thresholds must not hide uncertainty;
- thresholds must identify what signals were available, missing, stale, or estimated;
- thresholds must not treat clean scenario results as production safety proof;
- thresholds must not treat source-name or external signal labels as runtime proof;
- thresholds must require separate review before active-experiment use.

This ADR does not add scoring changes, threshold code, policy engine changes, API behavior, config, or telemetry.

## Source-Name And Signal Provenance Expectations

Future adaptive safety review must account for source-name and signal provenance.

Future provenance expectations:

- source-name claims must remain reviewable and must not certify safety;
- future source-name guard output is not enforcement unless separately implemented;
- signal provenance should identify whether context is observed, synthetic, estimated, stale, unavailable, or unknown;
- future external signals must remain read-only unless separately approved;
- missing or stale signal context must degrade safely if implemented later;
- signal provenance must not include secrets, tokens, credentials, env var values, private network details, or absolute local machine paths.

This ADR does not add source scanning, source-name guard implementation, allowlist files, external signal ingestion, ExternalSignalPort implementation, WorkloadProfile implementation, telemetry, persistence, or HTTP calls.

## Privacy And Trace-Safety Constraints

Future safety evidence must preserve privacy and trace-safety boundaries.

Future privacy expectations:

- future evidence must not include secrets;
- future evidence must not include tokens;
- future evidence must not include credentials;
- future evidence must not include env var values;
- future evidence must not include private network details;
- future evidence must not include absolute local machine paths;
- future traces must be anonymized or synthetic unless separately approved;
- future trace import requires separate ADR/PR;
- future retention/deletion requires separate review.

This ADR does not add trace import, telemetry import, storage, persistence, audit log implementation, secret handling behavior, retention policy, deletion policy, export behavior, upload behavior, download behavior, PDF generation, or ZIP generation.

## External Call And Mutation Boundaries

Future adaptive safety review must forbid unsafe external calls and hidden autonomous mutation.

Future external-call boundaries:

- no unsafe external calls;
- no hidden autonomous mutation;
- no direct LASE control of GPU, grid, facility, power, carbon, or cloud systems;
- no direct LASE mutation of live routing state;
- no direct LASE alteration of proxy behavior;
- no direct LASE selection of production routes;
- no GitHub settings, ruleset, secret, environment, release, registry, signing, or governance mutation without separate explicit approval;
- no cloud mutation without separate explicit approval.

This ADR does not add external clients, HTTP calls, cloud mutation, GitHub mutation, registry publication, release creation, signing, facility automation, GPU orchestration, power/grid control, or carbon-aware routing.

## Decision Safety Evidence

Future adaptive decisions should expose decision safety evidence before reviewers consider promotion.

Future decision safety evidence should include:

- selected option;
- rejected options;
- signals used;
- policy checks passed/failed;
- known uncertainty;
- what was not proven;
- safety mode;
- reason traffic was or was not allowed to change;
- operator-facing explanation.

Future decision safety evidence must preserve limitations. It must not certify production safety, prove correctness, prove live-cloud validation, prove real-tenant validation, or replace human review.

This ADR does not add evidence generation, report generation, JSON output, API fields, storage, persistence, export behavior, upload behavior, download behavior, PDF generation, ZIP generation, or PR comment/artifact behavior.

## Observe-Only Boundaries

Observe-only mode may observe, compare, and record evidence if separately implemented.

Allowed in future observe-only mode:

- observe local/reviewer-safe context;
- compare candidate outputs for review;
- record bounded evidence if separately approved;
- surface uncertainty and not-proven boundaries;
- support reviewer/operator explanations.

Forbidden in observe-only mode:

- must not influence routing;
- must not select production routes;
- must not mutate live traffic;
- must not mutate proxy behavior;
- must not mutate cloud, facility, GPU, grid, power, carbon, GitHub, release, signing, registry, governance, or production systems;
- must not be represented as production certification.

This ADR does not add observe-only runtime behavior.

## Recommendation Boundaries

Recommendation mode may produce suggested actions if separately implemented.

Allowed in future recommendation mode:

- produce suggested actions for reviewers/operators;
- explain selected and rejected options;
- identify policy checks, signals used, uncertainty, and not-proven boundaries;
- require manual review before any action is applied.

Forbidden in recommendation mode:

- must not apply suggested actions automatically;
- must not mutate live traffic;
- must not alter proxy behavior;
- must not bypass policy/operator gates;
- must not become hidden production routing authority;
- must not be represented as production certification.

This ADR does not add recommendation runtime behavior.

## Shadow Boundaries

Shadow mode may evaluate hypothetical decisions beside real routing if separately implemented.

Allowed in future shadow mode:

- evaluate hypothetical decisions beside real routing;
- compare shadow decisions with live allocation decisions;
- produce reviewer metadata;
- preserve deterministic evidence where practical;
- keep policy failures and uncertainty visible.

Forbidden in shadow mode:

- must not mutate live traffic;
- must not directly influence routing;
- must not directly select production routes;
- must not directly alter proxy behavior;
- must not bypass policy/operator gates;
- must not be represented as production certification.

This ADR does not add shadow runtime behavior, replay execution, or report generation.

## Active-Experiment Boundaries

Active-experiment mode may allow tightly bounded experiments only after explicit policy/operator gates if separately implemented.

Allowed in future active-experiment mode:

- traffic-changing behavior only inside a narrow approved scope;
- explicit policy gate approval;
- explicit operator/reviewer approval;
- deterministic safety evidence where practical;
- rollback and stop conditions;
- limited blast radius;
- confidence thresholds and uncertainty labels;
- visible operator-facing explanations.

Forbidden in active-experiment mode:

- must not be automatic from observe-only, recommendation, or shadow mode;
- must not run without explicit policy and operator gates;
- must not lack rollback/stop conditions;
- must not lack blast-radius limits;
- must not hide autonomous mutation;
- must not mutate cloud, facility, GPU, grid, power, carbon, GitHub, release, signing, registry, governance, or production systems without separately approved scope;
- must not be represented as production certification.

Active-experiment mode does not mean production-ready. Active-experiment mode does not mean production-certified. Active-experiment mode does not prove live-cloud validation or real-tenant validation.

This ADR does not add active-experiment runtime behavior.

## Manual Promotion Only

Manual promotion only means future movement toward traffic-changing behavior must require explicit human review and approval.

Manual promotion expectations:

- no automatic jump from observation to traffic-changing behavior;
- no self-promotion from recommendation to active experiment;
- no self-promotion from shadow to active experiment;
- no hidden background promotion;
- no promotion based only on a clean report, passing test, scenario comparison, or confidence score;
- no promotion without rollback/stop conditions and blast-radius limits;
- no promotion without explicit not-proven boundaries.

This ADR does not add promotion workflows, workflow automation, bot comments, PR artifacts, config flags, runtime gates, or API commands.

## Relationship To The North-Star Vision

ADR-0005 supports the LoadBalancerPro north-star vision by putting guardrails before autonomy.

Relationship to the future platform vision:

- future datacenter adaptive traffic control needs explainable safety modes;
- partial degradation and tail latency under pressure require visible uncertainty and risk boundaries;
- safer adaptive routing requires policy/operator gates before traffic can change;
- explainability and auditability require decision safety evidence;
- guardrails before autonomy reduce black-box routing decisions;
- active experimentation must stay bounded, reversible, and reviewable;
- future adaptive behavior must preserve explicit not-proven boundaries.

This ADR is a planning document, not proof that the north-star architecture is implemented.

## Relationship To LASE And Adaptive Routing

ADR-0005 complements ADR-0002 by defining safety mode and guardrail vocabulary for future LASE and adaptive routing promotion.

Future LASE/adaptive routing expectations:

- LASE outputs must remain observe-only, recommendation, shadow, or active-experiment according to explicit mode labels;
- LASE must not directly mutate live allocation state;
- LASE must not directly select production routes;
- LASE must not directly alter proxy behavior;
- LASE must not bypass policy/operator gates;
- adaptive strategy outputs must not become hidden production routing authority;
- any future promotion from LASE to live behavior requires separate ADR and implementation PR.

This ADR does not change live allocation behavior, routing behavior, scoring behavior, strategy behavior, proxy behavior, API behavior, or production behavior.

## Relationship To Evidence And Reviewer Metadata

ADR-0005 complements ADR-0003 by defining what future decision safety evidence should explain.

Future evidence expectations:

- evidence should identify selected option and rejected options;
- evidence should identify signals used and policy checks passed/failed;
- evidence should identify known uncertainty and what was not proven;
- evidence should identify safety mode and traffic-change reason;
- evidence should include operator-facing explanation;
- evidence must not replace human review;
- evidence must not certify production safety.

This ADR does not implement EvidencePacket, EvidenceAssembler, report generation, JSON output, storage, persistence, telemetry, audit logs, export, upload, download, PDF, ZIP, or PR comment/artifact behavior.

## Relationship To Workload And Scenario Modeling

ADR-0005 complements ADR-0004 by defining how future scenario evidence may support safety review without becoming proof.

Future workload/scenario expectations:

- workload and scenario context may inform safety review if separately implemented;
- clean scenario results are not production safety proof;
- comparison output is not correctness validation;
- replay evidence is not replay proof;
- scenario labels must remain reviewer-readable and stable;
- scenario context must not grant LASE live routing authority;
- trace import and workload generation remain future-only.

This ADR does not implement WorkloadProfile, ScenarioGenerator, workload generators, trace import, replay execution, or runtime workload behavior.

## Relationship To External Signals

ADR-0005 complements the future `ExternalSignalPort` and `WorkloadProfile` contracts by requiring read-only, provenance-aware, privacy-safe signal context.

Future external signal expectations:

- external signal context should be read-only unless separately approved;
- stale, missing, unknown, unavailable, synthetic, or estimated signals should be labeled;
- external signal context must not grant mutation authority;
- future signals must not control GPU, grid, facility, power, carbon, cloud, proxy, routing, scoring, strategy, or production systems directly;
- missing or stale external signals must degrade safely if implemented later.

This ADR does not add ExternalSignalPort implementation, WorkloadProfile implementation, runtime signal ingestion, telemetry, external clients, HTTP calls, GPU orchestration, power/grid control, carbon-aware routing, or facility automation.

## Safety And Non-Goals

This ADR is documentation and documentation guard tests only.

Non-goals:

- no production Java runtime behavior;
- no records/classes/interfaces/enums under `src/main/java`;
- no runtime enforcement;
- no runtime LASE enforcement;
- no package-boundary enforcement;
- no ArchUnit enforcement;
- no source-name guard implementation;
- no source scanning;
- no WorkloadProfile;
- no ScenarioGenerator;
- no workload generators;
- no trace import;
- no replay execution;
- no EvidencePacket;
- no EvidenceAssembler;
- no report generation;
- no JSON output;
- no storage or persistence;
- no telemetry or audit log implementation;
- no export, upload, download, PDF, or ZIP behavior;
- no filesystem-writing implementation;
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

This ADR does not claim ADR approval, runtime safety enforcement, runtime LASE enforcement, package-boundary enforcement, ArchUnit enforcement, replay execution, evidence/report generation, storage/persistence, workload generation, trace import, external signal ingestion, carbon-aware routing, GPU orchestration, power/grid control, facility automation, routing/scoring/strategy/proxy/API behavior changes, production readiness, production certification, live-cloud validation, or real-tenant validation.

## Consequences

Positive consequences:

- reviewers get one proposed ADR for future safety modes and guardrail vocabulary;
- future adaptive behavior proposals can be evaluated against observe-only, recommendation, shadow, active-experiment, and manual-promotion boundaries;
- future decision safety evidence has a clear target shape;
- guardrails are recorded before autonomy;
- LASE, evidence, workload/scenario, and external-signal docs share one safety frame;
- not-proven boundaries stay visible before implementation pressure appears.

Costs and risks:

- this ADR adds documentation to maintain;
- future implementers may overread safety vocabulary as current enforcement unless planning-only language remains explicit;
- active-experiment terminology may be misread as production certification unless boundaries remain explicit;
- future safety enforcement still requires separate scoped PR review, deterministic tests, rollback review, privacy review, and behavior-impact review.

## Relationship To Existing Docs

Related docs:

- [`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) names ADR-0005 as a proposed Phase 0 ADR.
- [`ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) defines the proposed future layer boundary that separates allocation, LASE, evidence, infrastructure, API, config, and docs/tests.
- [`ADR-0002_LASE_INTEGRATION_MODEL.md`](ADR-0002_LASE_INTEGRATION_MODEL.md) defines the proposed future LASE integration model.
- [`ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md`](ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md) defines the proposed future evidence architecture model.
- [`ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md`](ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md) defines the proposed future workload realism and scenario modeling model.
- [`ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md`](ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md) drafts the proposed future evidence packet and replay boundary model as planning-only guidance for decision safety evidence, policy checks, safety modes, operator explanations, replay-facing evidence, and not-proven boundary evidence.
- [`ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md`](ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md) drafts the proposed future reviewer evidence and trust model as planning-only guidance for policy gate evidence, safety-mode evidence, manual/operator approval expectations, and non-certifying trust boundaries.
- [`../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md) maps safety and guardrail phases to current docs and future-only boundaries.
- [`../REVIEWER_TRUST_MAP.md`](../REVIEWER_TRUST_MAP.md) provides reviewer navigation and evidence boundaries.
- [`../ENTERPRISE_READINESS_AUDIT.md`](../ENTERPRISE_READINESS_AUDIT.md) records readiness posture and not-proven boundaries.
- [`../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md) defines the future LASE/live allocation boundary contract.
- [`../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md`](../WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md) defines future WorkloadProfile signal metadata.
- [`../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md`](../EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md) defines the future read-only ExternalSignalPort contract.
- [`../THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md`](../THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md) frames current Tier 1 routing focus and future Tier 2/Tier 3 signal concepts.
- [`../SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md`](../SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md) documents future source-name guard rule categories.
- [`../SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md`](../SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md) documents future allowlist lifecycle planning.

This ADR does not supersede those docs. It provides a proposed decision frame for future safety boundaries and guardrail work.

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
- EvidencePacket implementation not added;
- EvidenceAssembler implementation not added;
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

## Reviewer-Facing Value

ADR-0005 gives reviewers a single place to inspect the intended future safety boundary model before any runtime enforcement, active experimentation, traffic shifting, replay execution, evidence/report generation, storage, persistence, workload generation, trace import, external signal ingestion, or behavior change exists.

Reviewer value:

- safety mode progression is explicit;
- observe-only, recommendation, shadow, active-experiment, and manual-promotion boundaries are separated;
- guardrail categories are visible before implementation;
- future decision safety evidence has a reviewer-readable target shape;
- policy/operator gates, rollback/stop conditions, blast-radius limits, confidence thresholds, provenance, privacy, and trace-safety expectations are documented;
- guardrails are tied to the north-star vision without claiming the north-star architecture is implemented;
- black-box routing decisions are explicitly discouraged;
- not-proven boundaries remain part of the review model.

Reviewers should treat this ADR as proposed/planning-only. It is not ADR approval, not runtime safety enforcement, not runtime LASE enforcement, not package-boundary enforcement, not ArchUnit enforcement, not replay execution, not evidence/report generation, not storage/persistence, not workload generation, not trace import, not external signal ingestion, not autonomous production traffic shifting, not production-readiness proof, and not production-certification proof.
