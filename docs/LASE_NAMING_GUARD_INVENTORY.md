# LASE Naming Guard Inventory

This document inventories current LoadBalancerPro class, file, and responsibility names against the future naming guard vocabulary in [`LASE_BOUNDARY_NAMING_GUARD_PLAN.md`](LASE_BOUNDARY_NAMING_GUARD_PLAN.md).

This is a naming inventory only, not enforcement. It is docs/test only. No runtime naming guard is active. No classes are renamed in this sprint. No package moves are made in this sprint. No source-name guard tests are added in this sprint. No ArchUnit or package-boundary tool is added. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

The next source-name guard feasibility step is documented in [`LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md`](LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md). That feasibility plan remains docs/test-only and does not add source scanning, runtime naming enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

The source-name guard review checklist is documented in [`SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md). That checklist remains docs/test-only and helps reviewers evaluate future guard scope, denylist terms, allowlists, suppressions, false-positive risk, false-negative risk, failure messages, rollback, and approval gates before any source-name guard implementation exists.

## Executive Summary

PR #220 added the LASE boundary architecture contract. PR #221 mapped current classes into future boundary buckets. PR #222 staged future package-boundary enforcement. PR #223 defined naming guard vocabulary and examples. This inventory is the next preparation layer: it maps current names into the naming categories before any source-name guard, package move, ArchUnit rule, or runtime enforcement exists.

The current naming surface is mostly understandable, but several names deserve careful handling before future enforcement:

- broad live names such as `LoadBalancer`, `AllocatorService`, and `AdaptiveRoutingPolicyEngine` imply runtime authority and need explicit separation from LASE shadow/evidence concepts;
- LASE names such as `LaseShadowAdvisor`, `LaseEvaluationEngine`, and `LaseShadowReplayEngine` are useful because they signal shadow, evaluation, and replay responsibilities;
- replay and evidence names should keep evidence/reviewer meaning visible and must not imply proof, production certification, or live replay authority;
- cloud/proxy/infrastructure names such as `CloudManager` and reverse-proxy classes should remain visibly separate from LASE naming;
- static UI and API response names should remain view/metadata names and not suggest routing authority.

This inventory is not a defect list. It is a migration-readiness map for future naming guard review.

## Current Status: Naming Inventory Only, Not Enforcement

Current status:

- naming inventory only;
- documentation and documentation guard tests only;
- no runtime naming guard is active;
- no runtime naming enforcement is added;
- no package-boundary enforcement is added;
- no source-name guard tests are added in this sprint;
- no classes are renamed in this sprint;
- no package moves are made in this sprint;
- no production Java runtime behavior is added;
- no records/classes/interfaces are added under `src/main/java`;
- no ArchUnit or package-boundary tool is added;
- no new dependency is added;
- no Maven build files are changed;
- no API fields are added;
- no routing, scoring, strategy, proxy, config, CI, Docker, release, signing, registry, governance, or production behavior changes are made.

Naming inventory does not equal runtime enforcement. LASE naming inventory is not enforcement. This document does not claim the LASE boundary is runtime-enforced, the LASE package boundary is enforced, or the LASE naming guard is runtime-enforced.

## Relationship To LASE Boundary Naming Guard Plan

The naming guard plan defines future naming categories, candidate naming rules, allowed examples, risky examples to avoid, and future guard strategy. This inventory uses those categories to inspect current names before any naming guard scans source names.

The plan remains a plan. This inventory remains an inventory. Neither document enforces naming, package boundaries, dependency direction, runtime mutation safety, replay proof, scoring proof, production readiness, or production certification.

## Relationship To LASE Boundary Enforcement Inventory

[`LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md`](LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md) maps current classes into future architecture buckets: live allocation, LASE shadow/evaluation, replay/evidence, reviewer metadata, domain model candidates, infrastructure/cloud/future integration, API/view, and configuration.

This naming inventory reuses those buckets but focuses on names rather than package ownership. A name can be acceptable while still requiring future package-boundary review. A name can be risky without proving unsafe runtime behavior.

## Why Naming Inventory Comes Before Naming Guard Enforcement

Naming inventory comes before naming guard enforcement because a guard should not start from guesses:

- current names need a reviewer-readable baseline before source-name scanning exists;
- broad false-positive scans can make honest negative boundary text noisy;
- source-name guards can mistake risky examples in documentation for implemented classes unless allowlists are designed carefully;
- package moves and behavior changes must remain separate from naming guard enforcement;
- naming guard results must never be used to claim production readiness.

This sprint documents the naming surface only. Source-name guard tests are future-only unless separately approved.

## Current Naming Categories Observed

| Naming category | Current naming families observed | General read | Future action |
| --- | --- | --- | --- |
| Live allocation naming | `LoadBalancer`, `AllocatorService`, routing strategies, server state, policy, and score names. | Runtime authority names are broad and should stay clearly separate from shadow/evidence names. | Keep for now; review before enforcement. |
| LASE shadow/evaluation naming | `LaseShadowAdvisor`, `LaseEvaluationEngine`, LASE reports, shadow events, replay names. | LASE/shadow/evaluation wording is helpful. | Keep and clarify shadow-only limits in docs. |
| Replay/evidence naming | `LaseShadowReplay*`, `AdaptiveRoutingScenario*`, `*EvidencePacket`, `RoutingDecisionReplayEvidence*`. | Evidence/replay intent is visible, but proof wording must stay out. | Keep; guard against proof/certification implications. |
| Reviewer metadata naming | `RoutingComparisonResponse`, `RoutingDecisionReplayEvidenceReviewer*`, checklist, rollup, and closure names. | Reviewer/view naming is mostly clear. | Keep; ensure metadata names do not imply route authority. |
| Domain model naming | `Server`, `ServerStateVector`, `RoutingDecision`, `ScoreFactorContribution`, signal names. | Some names are pure-model candidates; some are mutable runtime models. | Review before package moves. |
| Infrastructure/cloud/future integration naming | `CloudManager`, `CloudAwsClients`, proxy classes, utility parsers. | Infrastructure/control names are explicit and high-risk for future LASE dependency direction. | Keep separate from LASE; review before enforcement. |
| API/view naming | controllers, services, response records, static UI pages. | API/view names are generally discoverable. | Keep; avoid implying production proof. |
| Configuration naming | `*Configuration`, `*Properties`, filters, policy config, telemetry config. | Configuration names are explicit but runtime-sensitive. | Keep; ensure config does not hide LASE authority. |

These are observed naming categories only. They are not new packages, rules, or runtime behavior.

## Live Allocation Naming Observations

| Current name or family | Current location if known | Likely future naming bucket | Risk level | Naming read | Recommended future action |
| --- | --- | --- | --- | --- | --- |
| `LoadBalancer` | `src/main/java/com/richmond423/loadbalancerpro/core/LoadBalancer.java` | Live allocation naming | HIGH | Broad name implies runtime allocation authority and server coordination. It is acceptable as a live-runtime name, but it should not be mistaken for shadow-only behavior. | Keep; review before enforcement and clarify live allocation ownership. |
| `AllocatorService` | `src/main/java/com/richmond423/loadbalancerpro/api/AllocatorService.java` | Live allocation / API orchestration naming | HIGH | The service name implies allocation orchestration and can sit close to API response shaping. | Keep; review before package enforcement. |
| `Server`, `ServerStateVector` | `core` | Live allocation and domain model naming | MEDIUM | `Server` can imply mutable runtime state; `ServerStateVector` reads more like a snapshot candidate. | Keep; distinguish mutable server state from read-only snapshot vocabulary later. |
| `ServerScoreCalculator` | `core` | Scoring/allocation naming | HIGH | Clearly signals score calculation and therefore scoring authority. | Keep; review before enforcement so scoring internals are not changed accidentally. |
| `AdaptiveRoutingPolicyEngine` and `AdaptiveRoutingPolicy*` | `core`, `api.config` | Live allocation / policy naming | HIGH | Policy and engine names imply runtime policy authority, especially near active-experiment vocabulary. | Keep; clarify policy gates and avoid LASE production-authority wording. |
| Routing strategy classes | `core` | Live allocation strategy naming | HIGH | Names such as round-robin, weighted, and tail-latency strategies imply route-selection behavior. | Keep; do not combine naming work with strategy behavior changes. |

These names appear to be runtime behavior names, not reviewer metadata names. Naming guards should not rename them without a separately approved refactor.

## LASE Shadow/Evaluation Naming Observations

| Current name or family | Current location if known | Likely future naming bucket | Risk level | Naming read | Recommended future action |
| --- | --- | --- | --- | --- | --- |
| `LaseShadowAdvisor` | `core` | LASE shadow/evaluation naming | LOW | Good shadow vocabulary. The name signals advisory behavior rather than live route authority. | Keep; ensure future dependencies stay observation-only. |
| `LaseEvaluationEngine` | `core` | LASE shadow/evaluation naming | MEDIUM | Evaluation wording is appropriate, but engine can sound authoritative if used outside shadow/evidence context. | Keep; clarify shadow/evaluation scope in docs. |
| `LaseShadowReplayEngine` | `core` | LASE replay/evidence naming | MEDIUM | Shadow replay wording is helpful, but replay must not imply replay proof or live replay execution. | Keep; guard against proof wording. |
| `LaseEvaluationReport`, `LaseShadowSummary`, `LaseShadowEvent`, `LaseShadowObservabilitySnapshot` | `core` | LASE evidence/domain naming | LOW | Report, summary, event, and snapshot names signal read/review surfaces. | Keep; use as examples of clearer LASE evidence vocabulary. |
| `LaseAllocationShadowSummary` | `api` | API/view naming for LASE evidence | LOW | Clearly reads as API/view summary metadata. | Keep; avoid granting routing authority through response wording. |

These names mostly align with the naming guard plan because they include `Lase`, `Shadow`, `Evaluation`, `Replay`, or summary/report wording.

## Replay/Evidence Naming Observations

| Current name or family | Current location if known | Likely future naming bucket | Risk level | Naming read | Recommended future action |
| --- | --- | --- | --- | --- | --- |
| `AdaptiveRoutingScenario` and `AdaptiveRoutingScenario*` | `core` | Replay/evidence or LASE scenario naming | MEDIUM | Scenario names are useful for controlled lab runs but can be broad without explicit evidence limits. | Keep; clarify local scenario/evidence scope. |
| `AdaptiveRoutingScenarioEvidencePacket` and packet builder/section/gate classes | `core` | Evidence naming | LOW | Evidence packet naming is reviewer-friendly and bounded when not paired with production-proof language. | Keep; avoid export/share/download overclaims. |
| `RoutingDecisionReplayEvidence*` services and responses | `api` | Reviewer metadata / evidence naming | LOW to MEDIUM | Replay evidence naming is clear, but must continue to avoid replay proof, replay execution, and mutation claims. | Keep; preserve explicit not-proven boundaries. |
| `RoutingDecisionReplaySnapshot*`, `RoutingDecisionReplayCapsule*`, `RoutingDecisionReplayReadinessChecklist*` | `api` | Reviewer metadata / evidence naming | MEDIUM | Snapshot, capsule, and checklist are useful view names; capsule and readiness can overclaim if not bounded. | Keep; continue negative boundary language. |
| `EnterpriseLabEvidenceExporter` and related lab evidence services | `lab` | Evidence/lab tooling naming | MEDIUM | Existing exporter wording describes bounded local evidence tooling, but future docs must not add new export/share/download behavior. | Keep; review before any new artifact behavior. |

Evidence and replay names should not imply production route authority, replay proof, scoring proof, correctness validation, production readiness, or production certification.
They should keep evidence-only behavior and reviewer metadata limits visible when a class or file does not own live routing behavior.

## Reviewer Metadata Naming Observations

| Current name or family | Current location if known | Likely future naming bucket | Risk level | Naming read | Recommended future action |
| --- | --- | --- | --- | --- | --- |
| `RoutingComparisonResponse`, `RoutingComparisonResultResponse` | `api` | API/view naming | MEDIUM | Response names are clear API surface names, but they summarize live comparison data. | Keep; guard field semantics and reviewer-only language. |
| `RoutingComparisonService` | `api` | API/view and orchestration naming | HIGH | Service wording can blur view shaping and runtime comparison orchestration. | Keep; review before package enforcement. |
| `RoutingDecisionReplayEvidenceReviewer*` | `api` | Reviewer metadata naming | LOW | Reviewer wording clearly indicates metadata/reporting responsibility. | Keep; use as safer naming pattern. |
| closure summary, closure rollup, and closure checklist names | `api` and docs | Reviewer metadata naming | LOW | Summary, rollup, checklist, and closure wording signal read-only reviewer review layers. | Keep; do not imply replay/scoring/correctness proof. |
| Static reviewer labels in `routing-demo.html` and `load-balancing-cockpit.html` | `src/main/resources/static` | API/view naming | LOW | Static UI names are reviewer-facing display labels. | Keep; avoid unsafe production/certification claims. |

Reviewer metadata naming should clearly signal metadata, view, report, checklist, summary, rollup, handoff, closure, or guidance responsibility.

## Domain Model Candidate Naming Observations

| Current name or family | Current location if known | Likely future naming bucket | Risk level | Naming read | Recommended future action |
| --- | --- | --- | --- | --- | --- |
| `Server` | `core` | Domain model / live allocation naming | HIGH | Short and central, but likely mutable and runtime-adjacent. | Keep; review before any domain package move. |
| `ServerStateVector` | `core` | Domain snapshot naming | MEDIUM | Vector wording suggests a structured state snapshot. | Keep; good future snapshot vocabulary candidate. |
| `RoutingDecision`, `RoutingComparisonReport`, `RoutingComparisonResult` | `core` | Domain decision/report naming | MEDIUM | Decision/report naming is appropriate but touches route-selection concepts. | Keep; preserve API semantics if moved later. |
| `ScoreFactorContribution`, `CandidateFactorContributionSummary`, score factor names | `core` | Domain scoring/evidence naming | HIGH | Score naming is necessary but high-risk for accidental scoring-internal changes. | Keep; separate any rename from scoring changes. |
| signal names such as `NetworkAwarenessSignal`, `LoadSheddingSignal`, `AutoscalingSignal`, `FailureScenarioSignal` | `core` | Domain signal naming | MEDIUM | Signal names are useful, but future external signals must remain read-only context unless approved. | Keep; align future names with ExternalSignalPort boundaries. |

Domain candidate names should remain free of infrastructure/control implications when future package work is approved.

## Infrastructure/Cloud/Future Integration Naming Observations

| Current name or family | Current location if known | Likely future naming bucket | Risk level | Naming read | Recommended future action |
| --- | --- | --- | --- | --- | --- |
| `CloudManager` | `core` | Infrastructure/cloud naming | HIGH | Explicit cloud/control-style name. Its clarity is useful, but it must stay out of direct LASE control paths. | Keep; review before enforcement and keep LASE dependency direction explicit. |
| `CloudAwsClients`, `CloudConfig`, `CloudMetricsCoordinator`, `CloudMutationSource` | `core` | Infrastructure/cloud naming | HIGH | Cloud, client, metrics, and mutation words make infrastructure/control boundaries visible. | Keep; never treat as LASE shadow authority. |
| `ReverseProxyService`, `ReverseProxyController`, `ReverseProxyConfiguration`, `ReverseProxyProperties`, proxy helpers | `api.proxy` | Proxy/infrastructure naming | HIGH | Proxy names clearly imply runtime forwarding/control surfaces. | Keep; LASE must not directly alter proxy behavior. |
| `Utils`, `CsvServerLogParser`, `JsonServerLogParser` | `util` | Infrastructure/tooling naming | MEDIUM | Parser names are clear; `Utils` is generic and less helpful for future enforcement. | Keep for now; consider later clarification only in a separate refactor. |
| `ProxyDemoFixtureLauncher` | `demo` | Demo/tooling naming | LOW | Fixture/demo wording makes local-only role visible. | Keep; avoid production validation claims. |

Infrastructure and cloud names must not imply live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, or facility automation.

## API/View Naming Observations

| Current name or family | Current location if known | Likely future naming bucket | Risk level | Naming read | Recommended future action |
| --- | --- | --- | --- | --- | --- |
| `AllocatorController`, `RoutingController`, `ScenarioReplayController`, `RemediationReportController` | `api` | API/view naming | MEDIUM | Controller names make HTTP/API ownership clear. `ScenarioReplayController` should remain bounded to safe scenario/review semantics. | Keep; avoid API behavior changes. |
| `EnterpriseLabController`, `EnterpriseLabReviewerSummaryController`, `EnterpriseLabOperatorEvidenceSummaryController`, `EnterpriseLabEvidenceTimelineController`, `EnterpriseLabEvidenceExportPacketController` | `api` | API/view and reviewer metadata naming | LOW to MEDIUM | Enterprise Lab and evidence labels are reviewer-facing. Existing export packet wording must stay bounded to current behavior. | Keep; do not add upload/share/download/export behavior. |
| `EvidenceTrainingController`, `EvidenceTrainingOnboardingService` | `api` | API/view training naming | LOW | Training/onboarding names are reviewer support surfaces. | Keep; avoid production certification wording. |
| `routing-demo.html`, `load-balancing-cockpit.html`, `enterprise-lab-reviewer.html`, `operator-evidence-dashboard.html`, `adaptive-routing-scenarios.html`, `ci-evidence-gate.html`, `evidence-timeline.html`, `evidence-export-packet.html` | `src/main/resources/static` | Static UI/view naming | LOW | Static pages make reviewer/operator views visible. | Keep; no external scripts/CDNs and no unsafe claims. |

API and static UI names should distinguish response/view presentation from route selection authority.

## Configuration Naming Observations

| Current name or family | Current location if known | Likely future naming bucket | Risk level | Naming read | Recommended future action |
| --- | --- | --- | --- | --- | --- |
| `AdaptiveRoutingPolicyConfiguration`, `AdaptiveRoutingPolicyProperties` | `api.config` | Configuration naming | MEDIUM | Policy config names are clear but connected to guarded runtime policy modes. | Keep; avoid hidden LASE authority. |
| `ApiSecurityConfiguration`, `AuthModeConfiguration`, `AuthProperties`, security filters | `api.config` | Security/config naming | MEDIUM | Security and auth names make runtime boundaries visible. | Keep; do not change security behavior. |
| `TelemetryConfiguration`, `TelemetryProperties`, `TelemetryStartupGuard` | `api.config` | Telemetry/config naming | MEDIUM | Telemetry names are explicit. | Keep; no telemetry/storage/persistence additions. |
| `ReverseProxyConfiguration`, `ReverseProxyProperties` | `api.proxy` | Proxy/config naming | HIGH | Proxy config names control runtime proxy behavior. | Keep; do not change proxy behavior. |
| `CliConfig`, `GuiConfig`, `ConfigLoader` | `cli`, `gui` | CLI/UI configuration naming | LOW to MEDIUM | CLI and GUI config names are understandable. | Keep; avoid package moves in naming work. |

Configuration naming should not hide production activation, LASE authority, external clients, secrets, credentials, or new properties.

## Naming Risks Observed

Observed naming risks:

- `LoadBalancer` and `AllocatorService` are broad live-runtime names. They are acceptable today, but future naming guards should not mistake them for shadow-only components.
- `AdaptiveRoutingPolicyEngine` and active-experiment policy vocabulary can blur LASE/shadow influence and live policy authority unless future docs keep gates explicit.
- `LaseEvaluationEngine` uses engine wording; future docs should keep it in shadow/evaluation/evidence context.
- `LaseShadowReplayEngine` and replay evidence names must not imply replay proof, guaranteed replay, correctness validation, or live replay execution.
- `RoutingComparisonService` can blur API view shaping and routing comparison orchestration.
- `CloudManager` and proxy names imply live-capable infrastructure/control boundaries and should remain separated from LASE naming.
- `Utils` is generic and may be unclear for future package-boundary enforcement.
- evidence packet/export labels can imply export/share/download behavior if not bounded by docs and tests.
- risky example names from the naming plan remain examples to avoid, not implemented classes.

These risks are naming observations only. They do not prove unsafe runtime behavior.

## Safe Future Naming Guard Sequence

A safe future naming guard sequence is:

1. Inventory current names.
2. Document risky naming patterns.
3. Add narrow docs-only guard tests.
4. Add source-name guard tests only in a later approved sprint.
5. Keep source-name guard tests narrow and deterministic.
6. Avoid broad false-positive scans.
7. Never combine naming guard enforcement with package moves.
8. Never combine naming guard enforcement with behavior changes.
9. Never use naming guard results to claim production readiness.

Source-name guard tests are future-only unless separately approved. Naming guard enforcement must not be combined with package moves or behavior changes. Naming guards do not equal runtime enforcement. Naming guards do not equal package-boundary enforcement.

## Non-Goals And Safety Boundaries

Explicit non-goals:

- no production Java runtime behavior;
- no records/classes/interfaces under `src/main/java`;
- no class renames;
- no package moves or refactors;
- no ArchUnit or any new dependency;
- no Maven build changes;
- no broad source scanning;
- no source-name guard tests in this sprint;
- no runtime naming enforcement;
- no package-boundary enforcement;
- no runtime LASE boundary implementation;
- no runtime workload model implementation;
- no runtime signal ingestion;
- no external API clients;
- no HTTP calls;
- no secrets, tokens, environment variables, credentials, config, or properties;
- no telemetry, storage, or persistence;
- no MessageDigest, SHA, hash, UUID, random, time, environment, or system-property behavior;
- no replay execution;
- no what-if mutation;
- no upload/share/download/export/PDF/ZIP behavior;
- no Docker, CI, release, signing, registry, or governance changes;
- no proxy behavior change;
- no strategy behavior change;
- no core routing behavior change;
- no scoring-internals behavior change;
- no live-cloud validation claim;
- no real-tenant validation claim;
- no GPU orchestration claim;
- no power/grid control claim;
- no carbon-aware routing implementation claim;
- no facility automation claim;
- no production readiness claim;
- no production certification claim.

This inventory does not claim a runtime-enforced LASE boundary. This inventory does not claim package-boundary enforcement is active. This inventory does not claim naming guard enforcement is active. This inventory does not claim current production readiness or production certification.

## Reviewer-Facing Value

This inventory helps reviewers inspect naming intent before any enforcement exists:

- live allocation names are identified as runtime-authority names;
- LASE shadow/evaluation names are identified as shadow/evidence names;
- replay/evidence names are separated from proof and production-readiness claims;
- reviewer metadata names are identified as read-only API/view surfaces;
- infrastructure/cloud/proxy names are identified as high-risk future control boundaries;
- API/static UI and configuration names are cataloged without behavior changes;
- future source-name guard work has a baseline;
- not-proven boundaries remain explicit.

Reviewers should treat this inventory as naming preparation only. It is not runtime enforcement, not package-boundary enforcement, not naming guard enforcement, not ArchUnit enforcement, not source-name scanning, not production-readiness proof, and not production-certification proof.
