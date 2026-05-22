# ADR-0001 Layered Architecture Boundary

## Status

Proposed / planning-only.

Decision type: architecture planning.

Implementation status: not implemented.

## Date

2026-05-21.

## Context

The uploaded architecture report recommends Phase 0 discovery, north-star definition, current-vs-target mapping, and initial architecture decision records before package moves or runtime architecture work. [`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) names ADR-0001 as the layered architecture boundary ADR.

LoadBalancerPro currently has meaningful architecture concepts already visible in code and documentation: live allocation, routing strategy, scoring, LASE shadow evaluation, replay, scenario evidence, reviewer metadata, guarded cloud/infrastructure code, API surfaces, static UI pages, and runtime configuration. Those concepts are not yet separated into enforced `domain`, `allocation`, `lase`, `evidence`, `infrastructure`, `api`, and `config` packages.

This ADR draft documents the intended future layer model and maps current areas to future boundary vocabulary. It is planning-only. This does not move packages. No package moves are introduced. This does not add ArchUnit. This does not enforce package boundaries. No ArchUnit or package-boundary enforcement is introduced. This does not change runtime behavior. No runtime behavior changes are introduced. This does not change routing/scoring/strategy/proxy behavior. This does not implement domain/application/infrastructure packages. This does not claim production readiness or certification.

## Decision

If a future implementation sprint is separately approved, LoadBalancerPro should evolve toward explicit architecture layers with clear dependency direction and strict separation between live allocation, LASE shadow evaluation, reviewer evidence, infrastructure mutation boundaries, API/view surfaces, and configuration.

This proposed decision is not accepted implementation. It records a target architecture boundary for reviewers:

- keep current packages stable in this sprint;
- keep all dependency rules future-only until separately implemented;
- define future package moves only after ADR review, migration review, and behavior regression coverage;
- never combine package moves with behavior changes;
- never combine enforcement with production-readiness claims.

## Intended Future Layer Model

The intended future layer model is documentation-only in this ADR:

| Future layer | Intended responsibility | Current implementation status |
| --- | --- | --- |
| `domain` | Pure models, value objects, domain events, facts, observations, decisions, signals, and immutable or side-effect-free evidence vocabulary. | Not implemented as a package boundary. |
| `allocation` | Live routing/allocation decision path, allocation policy gates, routing strategy orchestration, scoring inputs, and live decision coordination. | Current behavior exists, but package boundary is not enforced. |
| `lase` | LASE shadow/evaluation layer for shadow evaluation, comparison, replay support, explanation metadata, and evidence generation. | Current LASE behavior exists, but runtime LASE boundary enforcement is not introduced. |
| `evidence` | Evidence/reviewer layer for reviewer evidence, audit artifact assembly concepts, scenario summaries, closure metadata, and not-proven boundary language. | Current evidence surfaces exist, but EvidencePacket architecture implementation is not added. |
| `infrastructure` | Infrastructure/cloud/future integration layer for guarded external/cloud/proxy/file/future facility/grid/GPU adapter boundaries. | Current infrastructure behavior exists in mixed packages; future integration boundaries are not enforced. |
| `api` | API layer for controllers, DTOs, reviewer/API response surfaces, static UI, and view shaping. | Current API behavior exists; no API behavior changes are introduced by this ADR. |
| `config` | Config layer for application configuration, security filters, rate limits, telemetry guards, web configuration, and safe runtime settings. | Current config behavior exists; no config behavior changes are introduced by this ADR. |
| `docs/tests` | Documentation and guard tests that describe, validate, and constrain future claims. | Current documentation guard tests exist; they are not runtime enforcement. |

The layer names are future targets. This ADR does not create package skeletons, move classes, introduce runtime interfaces, or enforce dependency direction.

The allocation/live routing layer remains the future home for live routing/allocation decision path concepts. The LASE shadow/evaluation layer remains the future home for shadow comparison and evidence generation concepts. The evidence/reviewer layer remains reviewer-facing and must not become production routing authority. The infrastructure/cloud/future integration layer remains guarded and explicit. The api layer and config layer keep response/configuration responsibilities separate from hidden control authority.

## Current Package/Class Observations

Current package observations:

| Current area | Current observations | Possible future boundary |
| --- | --- | --- |
| `com.richmond423.loadbalancerpro.core` | Contains live allocation, domain-like models, scoring, routing strategy, LASE, replay, scenario, evidence, and cloud classes. | Split candidates: `domain`, `allocation`, `lase`, `evidence`, and `infrastructure`. |
| `com.richmond423.loadbalancerpro.api` | Contains Spring API entry point, controllers, services, DTOs, reviewer metadata shaping, and evidence response surfaces. | `api` plus possible future application-service boundary. |
| `com.richmond423.loadbalancerpro.api.config` | Contains security, auth, rate limit, request size, telemetry, policy, and web configuration classes. | `config`. |
| `com.richmond423.loadbalancerpro.api.proxy` | Contains optional proxy behavior, route planning, status, metrics, and private-network validation boundaries. | `infrastructure` or guarded `api.proxy` boundary. |
| `com.richmond423.loadbalancerpro.lab` | Contains Enterprise Lab run, scenario catalog, scorecard, and evidence exporter concepts. | `evidence` or `lase` depending on future ADRs. |
| `com.richmond423.loadbalancerpro.cli` and `com.richmond423.loadbalancerpro.gui` | Contain CLI/GUI commands, local evidence helpers, and optional UI behavior. | `api/view`, `tooling`, or legacy UI/CLI boundary. |
| `src/main/resources/static` | Contains static UI pages and reviewer dashboards. | `api` / view surface. |

Current class observations include `LoadBalancer`, `AllocatorService`, `Server`, `ServerStateVector`, `ServerScoreCalculator`, `AdaptiveRoutingPolicyEngine`, `LaseShadowAdvisor`, `LaseEvaluationEngine`, `LaseShadowReplayEngine`, `AdaptiveRoutingScenario` classes, evidence/reviewer metadata classes, `CloudManager` and cloud/infrastructure classes, static UI pages, and reviewer dashboards.

These observations are mapping notes, not defects and not proof of unsafe runtime behavior.

## Boundary Rules, Future-Only

Future-only boundary rules:

- domain must not depend on infrastructure;
- LASE must not directly mutate live allocation state;
- LASE must not directly call proxy mutation paths;
- LASE must not directly call CloudManager or future grid/facility/GPU control paths;
- evidence/reviewer metadata must not become production routing authority;
- api may expose reviewer metadata but must not bypass policy gates;
- infrastructure integrations must remain guarded and explicit;
- config must not hide production authority or certification claims;
- package-boundary enforcement is future-only and separately approved.

These rules are not active enforcement. They are proposed direction for future ADRs, package plans, and enforcement tooling if separately approved.

## Allowed Dependencies, Future-Only

Future-only allowed dependencies:

- api may depend on application services and response DTOs;
- allocation may depend on domain models and policy gates;
- LASE may consume read-only observation snapshots;
- evidence may summarize domain/allocation/LASE outputs;
- infrastructure may implement guarded adapters;
- docs/tests may reference all areas for documentation and validation.

These allowed dependencies are planning guidance only. They do not create runtime interfaces, package skeletons, ArchUnit rules, or build-time checks.

## Forbidden Dependencies, Future-Only

Future-only forbidden dependencies:

- domain to infrastructure;
- LASE to allocation mutation APIs;
- LASE to proxy mutation APIs;
- LASE to CloudManager or future grid/facility/GPU control APIs;
- evidence to hidden production route selection;
- api to bypass safety policy gates;
- docs/tests to claim runtime enforcement that does not exist.

These forbidden dependencies are not enforced in this sprint. Future enforcement must be separately reviewed and must not be bundled with package moves, behavior changes, or production-readiness claims.

## Migration Strategy, Future-Only

Future migration strategy:

1. Keep this ADR proposed until separately reviewed.
2. Keep current packages stable.
3. Continue docs/test-only inventories where needed.
4. Draft ADR-0002 through ADR-0008 separately.
5. Add package skeletons only in a separately approved sprint.
6. Move pure domain models only in a separately approved sprint.
7. Introduce observation ports only in a separately approved sprint.
8. Add ArchUnit or equivalent enforcement only after dependency/build impact review.
9. Never combine package moves with behavior changes.
10. Never combine enforcement with production-readiness claims.

Package moves must not be combined with behavior changes. Enforcement must not be combined with production-readiness claims.

## Consequences

Positive consequences:

- reviewers get a stable vocabulary for future architecture slices;
- package moves can be reviewed separately from behavior changes;
- LASE shadow/evidence boundaries stay visible before enforcement exists;
- infrastructure and future external control boundaries remain explicit;
- future ArchUnit or equivalent tooling can be evaluated after dependency/build impact review.

Costs and risks:

- this ADR adds documentation to maintain;
- future package moves still require careful compatibility tests;
- current packages remain mixed until a separate approved sprint changes them;
- planning language can be overread as implementation unless not-proven boundaries stay explicit.

## Relationship To Existing Docs

Related docs:

- [`../PHASE_0_ARCHITECTURE_ADR_INDEX.md`](../PHASE_0_ARCHITECTURE_ADR_INDEX.md) names ADR-0001 as a proposed Phase 0 ADR.
- [`../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md`](../ARCHITECTURE_REPORT_ALIGNMENT_INDEX.md) maps architecture report phases to current docs and future-only boundaries.
- [`../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](../LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md) defines the future LASE/live allocation boundary contract.
- [`../LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md`](../LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md) maps current classes into future boundary buckets.
- [`../LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md`](../LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md) stages future package-boundary enforcement.
- [`../REVIEWER_TRUST_MAP.md`](../REVIEWER_TRUST_MAP.md) provides reviewer navigation and evidence boundaries.
- [`../ENTERPRISE_READINESS_AUDIT.md`](../ENTERPRISE_READINESS_AUDIT.md) records readiness posture and not-proven boundaries.

This ADR does not supersede those docs. It provides a proposed decision frame for future layered architecture work.

## Safety Boundaries And Non-Goals

This ADR is documentation and documentation guard tests only.

Non-goals:

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
- no production behavior change.

This ADR does not claim architecture report implementation, ADR approval, runtime architecture implementation, ExternalSignalPort implementation, WorkloadProfile implementation, ScenarioGenerator implementation, EvidencePacket implementation, runtime LASE enforcement, package-boundary enforcement, source-name guard implementation, production readiness, production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, or facility automation.

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
- LASE boundary not runtime-enforced yet;
- LASE package boundary not enforced yet;
- ArchUnit/package-boundary tooling not added yet;
- Source-name guard not implemented yet;
- Architecture report alignment index is not implementation;
- Phase 0 ADR index is not implementation;
- ADR-0001 is proposed/planning-only.

## Reviewer-Facing Value

ADR-0001 gives reviewers a single place to inspect the intended future layered architecture boundary before any package move, runtime interface, enforcement tool, or behavior change exists.

Reviewer value:

- future layer vocabulary is explicit;
- current package/class observations are mapped without moving code;
- live allocation and LASE shadow/evidence separation remains visible;
- infrastructure/cloud/future control boundaries remain guarded;
- allowed and forbidden dependencies are documented as future-only;
- migration sequencing keeps package moves separate from behavior changes and production claims.

Reviewers should treat this ADR as proposed/planning-only. It is not implementation proof, not package-boundary enforcement, not runtime LASE enforcement, not production-readiness proof, and not production-certification proof.
