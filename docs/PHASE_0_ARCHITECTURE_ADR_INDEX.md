# Phase 0 Architecture ADR Index

This document identifies the initial architecture decision record set recommended by the uploaded architecture report and maps those decision areas to the current LoadBalancerPro documentation set. It is an ADR index only, not implementation, and it does not add runtime architecture changes, package moves, source scanning, ArchUnit, dependencies, Maven build changes, API behavior, routing behavior, scoring behavior, strategy behavior, proxy behavior, config changes, Docker changes, CI changes, release changes, registry changes, governance changes, or production behavior.

This is docs/test only. Proposed ADRs are planning only until separately written/approved in future scoped PRs. No package moves are introduced. No runtime LASE enforcement is introduced. No EvidencePacket implementation is introduced. No WorkloadProfile implementation is introduced. No ScenarioGenerator implementation is introduced. No ExternalSignalPort implementation is introduced. No production readiness claim is made. No production certification claim is made.

## Executive Summary

Phase 0 of the architecture report emphasizes discovery, north-star definition, current-vs-target mapping, and an initial ADR set before implementation work. This index names that initial ADR set and points reviewers to existing repository documentation that already frames the relevant decision areas.

The index is deliberately conservative:

- it names decisions that should be written later;
- it maps those decisions to current docs;
- it separates current behavior from documented-only, planning-only, and future-only work;
- it keeps future implementation behind separate scoped PRs;
- it does not claim roadmap completion.

## Current Status: ADR Index Only, Not Implementation

Current status:

- ADR index only;
- documentation and documentation guard tests only;
- no ADR text is approved by this document;
- no runtime architecture changes;
- no production Java runtime behavior;
- no records/classes/interfaces/enums under `src/main/java`;
- no class renames;
- no package moves or refactors;
- no source scanning logic;
- no ArchUnit or package-boundary tooling;
- no new dependencies;
- no Maven build changes;
- no API behavior changes;
- no routing, scoring, strategy, proxy, config, Docker, CI, release, signing, registry, governance, or production behavior changes;
- no runtime LASE boundary implementation;
- no runtime package-boundary enforcement;
- no ExternalSignalPort implementation;
- no WorkloadProfile implementation;
- no EvidencePacket implementation;
- no ScenarioGenerator implementation;
- no observability, telemetry, storage, or persistence implementation;
- no external API clients or HTTP calls;
- no secrets, tokens, environment variables, credentials, config, or properties;
- no replay execution;
- no what-if mutation;
- no upload/share/download/export/PDF/ZIP behavior.

The proposed ADRs are planning markers. Future ADR text, future architecture changes, and future implementation work each require separate scoped PR review.

## Relationship To Architecture Report Alignment Index

The architecture report alignment index is documented in [`ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md). That index maps the architecture report's Phase 0 through Phase 12 themes to current repository docs and future-only boundaries.

This Phase 0 ADR index is the next documentation slice recommended by that alignment index. It narrows Phase 0 into an initial ADR backlog:

- layered architecture;
- LASE integration;
- evidence as an architecture artifact;
- workload realism;
- safety guardrails;
- live allocation and shadow evaluation separation;
- reviewer evidence and trust;
- future external signal context boundaries.

This document does not implement those ADRs. It only identifies what future ADRs should cover and how reviewers can inspect existing related documentation today.

## Why ADR Indexing Comes Before Implementation

ADR indexing comes before implementation because the architecture report describes a broad target architecture. Without a small ADR map first, future PRs could accidentally mix package moves, runtime behavior, evidence changes, external-signal semantics, and production claims into one hard-to-review change.

The index gives reviewers a stable preparation layer:

- it makes decision boundaries visible before code moves;
- it identifies existing documents that already frame the decision;
- it separates "current behavior" from "documented only" and "future-only";
- it records implementation boundaries before implementation pressure appears;
- it helps keep later ADR-writing sprints narrow and auditable.

## Proposed ADR Set

The proposed Phase 0 ADR set is:

| ADR | Decision area | Current status | Safe next documentation slice |
| --- | --- | --- | --- |
| ADR-0001 | layered architecture boundary | proposed / planning-only draft in [`adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) | Keep ADR-0001 docs/test-only until separately reviewed; no package moves, ArchUnit, package-boundary enforcement, or runtime behavior changes |
| ADR-0002 | LASE integration model | proposed / planning-only draft in [`adr/ADR-0002_LASE_INTEGRATION_MODEL.md`](adr/ADR-0002_LASE_INTEGRATION_MODEL.md) | Keep ADR-0002 docs/test-only until separately reviewed; no runtime LASE enforcement, `LaseObservationPort`, package-boundary enforcement, replay execution, or behavior changes |
| ADR-0003 | evidence as first-class artifact | planning only | Write ADR-0003 as docs/test-only evidence decision text |
| ADR-0004 | workload realism and scenario modeling | planning only | Write ADR-0004 as docs/test-only workload realism decision text |
| ADR-0005 | safety boundaries and guardrails | planning only | Write ADR-0005 as docs/test-only safety boundary decision text |
| ADR-0006 | live allocation vs shadow evaluation separation | planning only | Write ADR-0006 as docs/test-only separation decision text |
| ADR-0007 | reviewer evidence and trust model | planning only | Write ADR-0007 as docs/test-only reviewer trust decision text |
| ADR-0008 | future external signal context boundaries | planning only | Write ADR-0008 as docs/test-only external signal context decision text |

These are proposed ADRs, not accepted ADRs. They do not prove implementation, package enforcement, runtime enforcement, production readiness, or production certification.

## ADR-0001 Layered Architecture Boundary

- Proposed ADR draft: [`adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md).
- Decision area: ADR-0001 layered architecture boundary.
- Why it matters: The architecture report recommends clearer future domain, application, infrastructure, and evidence boundaries. A future ADR should define those layers before any package move or enforcement sprint.
- Existing related docs: [`ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md), [`LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md), [`LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md`](LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md), [`LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md`](LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md).
- Current status: proposed / planning-only. Existing docs and the ADR-0001 draft map future boundaries, but package-boundary enforcement is not active.
- Implementation boundary: must not move packages in this sprint; must not rename classes; must not add ArchUnit; must not claim package-boundary enforcement.
- Safe next documentation slice: review the docs/test-only ADR-0001 draft before any package plan or enforcement proposal.
- Explicit non-claims: no package moves, no package-boundary enforcement, no runtime architecture implementation, no production readiness claim, and no production certification claim.

## ADR-0002 LASE Integration Model

- Proposed ADR draft: [`adr/ADR-0002_LASE_INTEGRATION_MODEL.md`](adr/ADR-0002_LASE_INTEGRATION_MODEL.md).
- Decision area: ADR-0002 LASE integration model.
- Why it matters: LASE needs a clear architecture decision for how shadow/evidence paths relate to live allocation without becoming hidden routing authority.
- Existing related docs: [`LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md), [`LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md`](LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md), [`LASE_BOUNDARY_NAMING_GUARD_PLAN.md`](LASE_BOUNDARY_NAMING_GUARD_PLAN.md), [`LASE_NAMING_GUARD_INVENTORY.md`](LASE_NAMING_GUARD_INVENTORY.md).
- Current status: proposed / planning-only. LASE boundary concepts and the ADR-0002 draft are documented, but runtime LASE boundary enforcement is not introduced.
- Implementation boundary: must not implement runtime LASE enforcement; must not add package-boundary enforcement; must not change routing, scoring, strategy, or proxy behavior.
- Safe next documentation slice: review the docs/test-only ADR-0002 draft before any runtime LASE enforcement, observation port, replay execution, policy-gate expansion, or package-boundary enforcement proposal.
- Explicit non-claims: no runtime LASE enforcement, no package-boundary enforcement, no ArchUnit tooling, no production routing authority, and no production certification claim.

## ADR-0003 Evidence As First-Class Artifact

- Decision area: ADR-0003 evidence as first-class artifact.
- Why it matters: The architecture report treats evidence packets, reviewer evidence, audit trail, and explainability as architecture concerns. A future ADR should define those terms before new evidence artifacts or report generation exist.
- Existing related docs: [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md), [`ENTERPRISE_READINESS_AUDIT.md`](ENTERPRISE_READINESS_AUDIT.md), [`ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md), [`SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md), [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md), [`SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md).
- Current status: current behavior plus planning only. Current reviewer evidence exists, but the architecture-report-level EvidencePacket remains future-only.
- Implementation boundary: must not implement EvidencePacket; must not add report generation; must not add JSON/YAML/TOML output; must not add upload/share/download/export/PDF/ZIP behavior.
- Safe next documentation slice: a docs/test-only ADR-0003 that defines evidence artifact terms, retention boundaries, deterministic expectations, and reviewer value.
- Explicit non-claims: no EvidencePacket implementation, no report generation, no audit-trail persistence, no replay execution, no correctness validation, and no production certification claim.

## ADR-0004 Workload Realism And Scenario Modeling

- Decision area: ADR-0004 workload realism and scenario modeling.
- Why it matters: The architecture report recommends richer workload profiles and scenario generation. A future ADR should define what workload realism means before adding runtime models.
- Existing related docs: [`WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md`](WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md), [`THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md`](THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md), [`ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md), [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md).
- Current status: documented only and future-only. WorkloadProfile signal metadata is documented as a future design contract, while ScenarioGenerator implementation is not added.
- Implementation boundary: must not implement WorkloadProfile; must not implement ScenarioGenerator; must not add runtime workload model code; must not change routing, scoring, strategy, or proxy behavior.
- Safe next documentation slice: a docs/test-only ADR-0004 that defines workload realism vocabulary, fixture boundaries, scenario quality, and not-proven limits.
- Explicit non-claims: no WorkloadProfile implementation, no ScenarioGenerator implementation, no production workload modeling proof, no GPU orchestration, no power/grid control, and no carbon-aware routing implementation.

## ADR-0005 Safety Boundaries And Guardrails

- Decision area: ADR-0005 safety boundaries and guardrails.
- Why it matters: The architecture report depends on strong guardrails, especially around CloudManager, infrastructure mutation, source-name claims, production language, and external control paths.
- Existing related docs: [`ENTERPRISE_READINESS_AUDIT.md`](ENTERPRISE_READINESS_AUDIT.md), [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md), [`SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md`](SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md), [`SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md), [`SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md).
- Current status: current behavior plus planning only. Safety posture is documented and tested in bounded areas, while future source-name guard and allowlist implementation remain future-only.
- Implementation boundary: must not mutate cloud resources, GitHub settings, rulesets, secrets, environments, registries, releases, signing, or production behavior.
- Safe next documentation slice: a docs/test-only ADR-0005 that records safety vocabulary, mutation boundaries, escalation gates, and no-overclaim language.
- Explicit non-claims: no cloud mutation, no GitHub settings mutation, no registry publication, no release creation, no container signing, no production readiness claim, and no production certification claim.

## ADR-0006 Live Allocation Vs Shadow Evaluation Separation

- Decision area: ADR-0006 live allocation vs shadow evaluation separation.
- Why it matters: The architecture report repeatedly separates production decision paths from shadow/evidence evaluation paths. A future ADR should define that separation before any future policy or package enforcement work.
- Existing related docs: [`LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md), [`ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md), [`THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md`](THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md), [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md).
- Current status: current behavior plus planning only. Current controlled lab behavior and docs preserve separation language, while future runtime enforcement remains not implemented.
- Implementation boundary: must not change routing behavior, scoring behavior, strategy behavior, proxy behavior, API behavior, or production behavior.
- Safe next documentation slice: a docs/test-only ADR-0006 that defines live allocation, shadow evaluation, recommendation, reviewer evidence, and future policy-gate meanings.
- Explicit non-claims: no routing behavior change, no scoring-internals change, no strategy behavior change, no proxy behavior change, no runtime LASE enforcement, and no production traffic-control claim.

## ADR-0007 Reviewer Evidence And Trust Model

- Decision area: ADR-0007 reviewer evidence and trust model.
- Why it matters: The architecture report emphasizes evidence, explainability, auditability, and reviewer trust. A future ADR should define the trust model before adding new dashboards, packets, or closure artifacts.
- Existing related docs: [`REVIEWER_TRUST_MAP.md`](REVIEWER_TRUST_MAP.md), [`ENTERPRISE_READINESS_AUDIT.md`](ENTERPRISE_READINESS_AUDIT.md), [`ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md), [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md).
- Current status: current behavior plus planning only. Reviewer trust documentation exists, but it does not certify correctness, production readiness, or production safety.
- Implementation boundary: must not claim production certification, correctness validation, live-cloud validation, real-tenant validation, SLA/SLO proof, or production monitoring proof.
- Safe next documentation slice: a docs/test-only ADR-0007 that defines reviewer trust inputs, proof boundaries, confidence language, and residual-risk language.
- Explicit non-claims: no production certification, no correctness validation, no live-cloud validation, no real-tenant validation, no production safety proof, and no production readiness claim.

## ADR-0008 Future External Signal Context Boundaries

- Decision area: ADR-0008 future external signal context boundaries.
- Why it matters: The architecture report points toward external context such as workload, compute, GPU, grid, facility, carbon, and power signals. A future ADR should define read-only context boundaries before any external integration exists.
- Existing related docs: [`EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md`](EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md), [`WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md`](WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md), [`THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md`](THREE_TIER_ADAPTIVE_ROUTING_STRATEGY.md), [`ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md).
- Current status: documented only and future-only. ExternalSignalPort and WorkloadProfile signal metadata are design contracts, not implementation.
- Implementation boundary: must not implement external clients, HTTP calls, signal ingestion, telemetry/storage/persistence, GPU orchestration, power/grid control, facility automation, or carbon-aware routing.
- Safe next documentation slice: a docs/test-only ADR-0008 that defines read-only signal context, freshness, trust labels, unavailable states, and control-plane prohibitions.
- Explicit non-claims: no ExternalSignalPort implementation, no WorkloadProfile implementation, no runtime signal ingestion, no GPU orchestration, no power/grid control, no carbon-aware routing implementation, and no facility automation.

## ADR Readiness Checklist

Before any proposed ADR is written or accepted, reviewers should confirm:

- decision area is clearly named;
- related existing docs are linked;
- current status is labeled as current behavior, documented only, planning only, or future-only;
- implementation boundary is explicit;
- safe next documentation slice is identified;
- explicit non-claims are present;
- production readiness and production certification are not claimed;
- live-cloud validation and real-tenant validation are not claimed;
- package moves and runtime behavior changes are not bundled with ADR text;
- future enforcement remains separate unless explicitly approved later.

## ADR Sequencing Guidance

Suggested sequencing:

1. Review [`adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md) before any package plan or enforcement tool proposal.
2. Review [`adr/ADR-0002_LASE_INTEGRATION_MODEL.md`](adr/ADR-0002_LASE_INTEGRATION_MODEL.md) before any runtime LASE boundary enforcement, observation port, policy-gate expansion, or replay execution proposal.
3. Write ADR-0003 evidence as first-class artifact before any EvidencePacket or report generation proposal.
4. Write ADR-0004 workload realism and scenario modeling before any WorkloadProfile or ScenarioGenerator implementation proposal.
5. Write ADR-0005 safety boundaries and guardrails before any implementation that could affect cloud, release, registry, governance, or external-control boundaries.
6. Write ADR-0006 live allocation vs shadow evaluation separation before any policy-gate expansion or LASE influence proposal.
7. Write ADR-0007 reviewer evidence and trust model before any new reviewer proof, dashboard, or certification-adjacent claim.
8. Write ADR-0008 future external signal context boundaries before any external signal ingestion, external client, or Tier 2/Tier 3 context implementation.

The sequence is advisory. It does not authorize implementation.

## Explicit Future-Only Implementation Boundaries

Future-only implementation boundaries:

- proposed ADRs are planning only until separately written/approved;
- future architecture implementation would require separate scoped PR review;
- no package moves are introduced;
- no package-boundary enforcement is active;
- no runtime LASE enforcement is introduced;
- no ExternalSignalPort implementation exists;
- no WorkloadProfile implementation exists;
- no EvidencePacket implementation exists;
- no ScenarioGenerator implementation exists;
- no source-name guard implementation exists;
- no allowlist implementation exists;
- no source scanning is added;
- no report generation is added;
- no JSON/YAML/TOML output is added;
- no telemetry/storage/persistence implementation is added;
- no external client or HTTP call is added;
- no GPU orchestration, power/grid control, carbon-aware routing implementation, or facility automation is added;
- no production readiness claim, production certification claim, live-cloud validation claim, or real-tenant validation claim is made.

## Safety Boundaries And Non-Goals

Hard boundaries for this sprint:

- documentation/test only;
- no production Java runtime behavior;
- no records/classes/interfaces/enums under `src/main/java`;
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
- no production readiness claim;
- no production certification claim;
- no live-cloud validation claim;
- no real-tenant validation claim;
- no GPU orchestration claim;
- no power/grid control claim;
- no carbon-aware routing implementation claim;
- no facility automation claim.

## Reviewer-Facing Value

This index helps reviewers inspect the first architecture decisions recommended by the report without treating the report as implementation proof. It provides a compact map from future ADR topics to current docs, current status, implementation boundaries, and safe next documentation slices.

The value is reviewer navigation and decision scoping only. Phase 0 ADR index is not implementation. Architecture report alignment index is not implementation. Production readiness, production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, facility automation, ExternalSignalPort implementation, WorkloadProfile implementation, EvidencePacket implementation, ScenarioGenerator implementation, runtime LASE boundary enforcement, package-boundary enforcement, ArchUnit tooling, and source-name guard implementation remain not proven.
