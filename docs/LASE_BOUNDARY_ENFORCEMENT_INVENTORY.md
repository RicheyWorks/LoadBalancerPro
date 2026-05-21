# LASE Boundary Enforcement Inventory

This document maps the current LoadBalancerPro source tree into the future LASE boundary architecture described by [`LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md).

This is an inventory only, not enforcement. It is docs/test only, no runtime enforcement, no package move, no Java runtime interface, no ArchUnit rule set, no production behavior, and no proof of production readiness.

## Executive Summary

The current codebase already contains live allocation, routing strategy, LASE shadow evaluation, replay, scenario, evidence, reviewer metadata, API/view, configuration, and cloud/infrastructure concepts. Most of those concepts are currently co-located under `com.richmond423.loadbalancerpro.core` or `com.richmond423.loadbalancerpro.api`.

The inventory records where important classes appear today and how they could map to future boundary buckets:

- live allocation / routing path;
- LASE shadow/evaluation path;
- replay / evidence path;
- reviewer metadata path;
- domain model candidates;
- infrastructure / cloud / future integration boundary;
- API/view layer;
- configuration layer.

No classes were moved. No packages were refactored. No runtime interfaces were added. No ArchUnit dependency or enforcement rule was added. This document is preparation for future review only.

## Current Status: Inventory Only, No Runtime Enforcement

Current status:

- inventory only;
- documentation and documentation guard tests only;
- no classes were moved;
- no packages were renamed;
- no production Java runtime behavior was added;
- no runtime interfaces were added;
- no ports or adapters were added;
- no ArchUnit dependency or package enforcement was added;
- no Maven build files were changed by this inventory;
- no API fields were added;
- no routing behavior changed;
- no scoring behavior changed;
- no strategy behavior changed;
- no proxy behavior changed;
- no CloudManager behavior changed;
- no runtime signal ingestion was added;
- no runtime workload model was added;
- no telemetry, storage, or persistence was added;
- no external clients or HTTP calls were added;
- no secrets, environment variables, config, credentials, or properties were added.

This inventory does not claim the LASE boundary is runtime-enforced. It does not claim production readiness, production certification, live-cloud validation, or real-tenant validation.

## Why This Inventory Exists

The LASE boundary contract defines the desired future architecture: live allocation should stay separate from LASE shadow evaluation, replay/evidence, reviewer metadata, and external control boundaries. The current tree is older than that contract and still mixes several responsibilities in existing packages.

This inventory gives future maintainers a low-risk map before any package move, runtime interface, ArchUnit rule, or enforcement sprint is proposed.

The staged path from this inventory to future package-boundary enforcement is documented in [`LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md`](LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md). That plan is planning only, not enforcement. It adds no ArchUnit or package-boundary tool, moves no classes, and changes no runtime behavior.

The future naming preparation layer is documented in [`LASE_BOUNDARY_NAMING_GUARD_PLAN.md`](LASE_BOUNDARY_NAMING_GUARD_PLAN.md). That plan is docs/test-only naming guard preparation, not runtime naming enforcement, package-boundary enforcement, ArchUnit tooling, package moves, or behavior change.

Reviewer questions this document helps answer:

- Which current classes appear to be live allocation or routing behavior?
- Which current classes appear to be LASE shadow/evaluation behavior?
- Which current classes appear to be replay/evidence or reviewer metadata?
- Which current classes should stay far away from cloud, facility, grid, GPU, or proxy control paths?
- What can be migrated safely later without changing behavior?

## Current Class/Package Observations

Observed current layout:

| Current location | Current observation | Likely future package/category | Migration risk | Runtime/evidence/API status | Safety notes |
| --- | --- | --- | --- | --- | --- |
| `com.richmond423.loadbalancerpro.core` | Mixed core package with routing models, live allocation, LASE, replay, scenario, scoring, cloud, metrics, and strategy code. | Split candidates: `domain`, `allocation`, `lase`, `infrastructure`. | HIGH | Runtime behavior. | Any future split must preserve behavior and test coverage. Do not combine package moves with scoring/routing changes. |
| `com.richmond423.loadbalancerpro.api` | Spring API controllers, services, request/response records, reviewer metadata shaping, scenario replay API, and comparison API. | `api` plus possible future application/service layer. | MEDIUM | API/view and runtime orchestration. | Response/view shaping should stay read-only where documented. |
| `com.richmond423.loadbalancerpro.api.config` | Spring security, auth, rate limit, request-size, telemetry, policy, and web configuration. | `config`. | LOW to MEDIUM | Runtime configuration. | Future boundary enforcement must not hide production activation or control behavior in config. |
| `com.richmond423.loadbalancerpro.api.proxy` | Lightweight reverse proxy, route planning, status, metrics, private-network validation gate, and loopback/private-network boundary helpers. | `infrastructure` or `api.proxy` boundary. | HIGH | Runtime proxy behavior. | LASE must not directly alter proxy behavior or private-network validation behavior. |
| `com.richmond423.loadbalancerpro.cli` | CLI commands and local evidence/audit/report helpers. | `api`, `tooling`, or `infrastructure` depending on responsibility. | MEDIUM | CLI runtime and evidence tooling. | Future moves must not create release/export/share/download behavior accidentally. |
| `com.richmond423.loadbalancerpro.lab` | Enterprise Lab run, evidence export, scenario catalog, and scorecard support. | `lase`, `evidence`, or `api` depending on future design. | MEDIUM | Lab evidence runtime. | Lab evidence must remain bounded and not become production authority. |
| `com.richmond423.loadbalancerpro.gui` | JavaFX desktop UI and command abstractions. | `api/view` or legacy UI boundary. | MEDIUM | Optional UI runtime. | Keep optional UI separate from API/proxy runtime assumptions. |
| `com.richmond423.loadbalancerpro.demo` | Local proxy fixture launcher. | `tooling` or `infrastructure.demo`. | LOW | Local demo/runtime fixture. | Loopback/local demo only; not production validation. |
| `com.richmond423.loadbalancerpro.util` | File parsing, import/export, hashing, and utility helpers. | `infrastructure` or `tooling`. | MEDIUM | Runtime utility behavior. | Do not expand filesystem/export behavior during boundary work. |
| `src/main/resources/static` | Static reviewer/operator pages: `routing-demo.html`, `load-balancing-cockpit.html`, `enterprise-lab-reviewer.html`, `operator-evidence-dashboard.html`, and related local pages. | `api/view` surface. | LOW | Static API/view-only surface. | Static UI must not invent unsafe claims or external scripts/CDNs. |

There is no standalone `com.richmond423.loadbalancerpro.config` package in the observed tree. Configuration classes currently appear under `com.richmond423.loadbalancerpro.api.config`, with additional CLI/GUI configuration classes in their respective packages.

## Future Boundary Buckets

Future boundary buckets are conceptual only:

| Future bucket | Intended responsibility | Current candidates | Enforcement status |
| --- | --- | --- | --- |
| `domain` | Pure models, facts, decisions, signals, explanations, and immutable evidence vocabulary. | `Server`, `ServerStateVector`, `RoutingDecision`, `RoutingDecisionExplanation`, `ScoreFactorContribution`, `NetworkAwarenessSignal`, policy and LASE report records. | Not enforced. |
| `allocation` | Live allocation and configured routing behavior. | `LoadBalancer`, `LoadDistributionEngine`, `RoutingComparisonEngine`, routing strategies, `AllocatorService`, `AdaptiveRoutingPolicyEngine`. | Not enforced. |
| `lase` | Shadow evaluation, policy comparison, scenario evaluation, replay metrics, and evidence generation. | `LaseShadowAdvisor`, `LaseEvaluationEngine`, LASE records, adaptive scenario classes, replay classes. | Not enforced. |
| `evidence` | Reviewer evidence packets, replay summaries, closure metadata, and local evidence helpers. | Routing decision replay evidence services/responses, `AdaptiveRoutingScenarioEvidencePacket`, lab evidence exporter, CLI evidence helpers. | Not enforced. |
| `infrastructure` | Cloud, proxy, filesystem, external integration, and future guarded adapters. | `CloudManager`, `CloudAwsClients`, `CloudConfig`, proxy classes, `Utils`, local fixture launcher. | Not enforced. |
| `api` | HTTP request/response shaping, controllers, static UI, and view DTOs. | `RoutingController`, `RoutingComparisonService`, `AllocatorController`, response records, static pages. | Not enforced. |
| `config` | Explicit runtime configuration and security filters. | `api.config` classes. | Not enforced. |

This sprint does not introduce these packages. They are future categories for review.

## Live Allocation Path Candidates

| Current class or package | Current location | Likely future package/category | Migration risk | Why it belongs here | Safety notes | Status |
| --- | --- | --- | --- | --- | --- | --- |
| `LoadBalancer` | `src/main/java/com/richmond423/loadbalancerpro/core/LoadBalancer.java` | `allocation` | HIGH | It owns server registry coordination, allocation methods, health handling, scaling recommendation entry points, and optional cloud initialization hooks. | Must not be moved with behavior changes. LASE hooks currently exist here and need careful future separation. | Runtime behavior. |
| `AllocatorService` | `src/main/java/com/richmond423/loadbalancerpro/api/AllocatorService.java` | `allocation` service or application service | HIGH | It orchestrates allocation/evaluation requests, policies, load distribution, load shedding, LASE observation summary, and response shaping. | Future split should avoid changing allocation semantics or policy gates. | Runtime API behavior. |
| `LoadDistributionEngine`, `LoadDistributionPlanner`, `LoadDistributionEvaluator`, `LoadDistributionResult` | `core` | `allocation` | HIGH | They compute capacity-aware, predictive, weighted, and allocation result details. | Do not combine migration with scoring or route-selection changes. | Runtime behavior. |
| `RoutingComparisonEngine`, `RoutingStrategy`, `RoutingStrategyRegistry`, routing strategy classes | `core` | `allocation` or `domain` depending on future design | HIGH | They compare registered routing strategies and select candidate decisions. | Strategy semantics must remain unchanged unless separately approved. | Runtime behavior. |
| `RoundRobinRoutingStrategy`, `WeightedRoundRobinRoutingStrategy`, `WeightedLeastConnectionsRoutingStrategy`, `WeightedLeastLoadStrategy`, `TailLatencyPowerOfTwoStrategy` | `core` | `allocation` strategy implementations | HIGH | They select candidates for routing comparison and LASE evaluation. | No strategy behavior change in boundary preparation work. | Runtime behavior. |
| `Server`, `ServerRegistry`, `ServerMonitor`, `ServerHealthCoordinator` | `core` | `allocation` plus `domain` split candidates | HIGH | They model and manage server state, health, and monitored data. | Moving mutable server state needs strong compatibility tests. | Runtime behavior. |
| `ServerScoreCalculator` | `core` | `allocation` or `domain.scoring` candidate | HIGH | It computes visible score factors used by routing comparison/explanation. | Scoring internals must not change in inventory or package-boundary preparation. | Runtime scoring behavior. |
| `AdaptiveRoutingPolicyEngine`, `AdaptiveRoutingPolicyInput`, `AdaptiveRoutingPolicyDecision`, `AdaptiveRoutingPolicyMode`, `AdaptiveRoutingPolicyStatus`, `AdaptiveRoutingPolicyAuditLog`, `AdaptiveRoutingPolicyAuditEvent` | `core` | `allocation.policy` or gated policy boundary | HIGH | They govern shadow/recommend/active-experiment decisions and guardrails. | Policy semantics must stay explicit and not become hidden LASE production authority. | Runtime policy behavior. |
| `LoadSheddingPolicy`, `LoadSheddingConfig`, `LoadSheddingSignal`, `LoadSheddingDecision` | `core` | `allocation` or `domain` | MEDIUM | They evaluate load-shedding actions used by allocation and LASE evaluation. | Future split must distinguish live decisions from shadow evaluation use. | Runtime behavior. |
| `AdaptiveConcurrencyLimiter`, `ConcurrencyFeedback`, `ConcurrencyLimitDecision` | `core` | `allocation` or `lase` depending on use | MEDIUM | They support adaptive concurrency decisions used in LASE evaluation. | Clarify live-vs-shadow use before moving. | Runtime behavior. |

## LASE Shadow/Evaluation Path Candidates

| Current class or package | Current location | Likely future package/category | Migration risk | Why it belongs here | Safety notes | Status |
| --- | --- | --- | --- | --- | --- | --- |
| `LaseShadowAdvisor` | `core` | `lase` | HIGH | It observes allocation/distribution results, builds LASE input, evaluates shadow reports, and records shadow events. | Must not receive live mutation handles in future boundary work. | Runtime shadow behavior. |
| `LaseEvaluationEngine` | `core` | `lase` | MEDIUM | It runs LASE evaluation over routing, concurrency, load shedding, autoscaling, and failure scenario dimensions. | Keep shadow/evidence semantics distinct from live allocation. | Runtime shadow behavior. |
| `LaseEvaluationConfig`, `LaseEvaluationInput`, `LaseEvaluationReport`, `LaseEvaluationReportFormatter` | `core` | `lase` plus possible `domain` models | MEDIUM | They define inputs, configuration, reports, and formatting for LASE evaluation. | Treat reports as evidence/reviewer outputs unless future policy gates say otherwise. | Runtime/evidence behavior. |
| `LaseShadowEvent`, `LaseShadowEventLog`, `LaseShadowSummary`, `LaseShadowObservabilitySnapshot`, `LaseShadowNetworkSummary` | `core` | `lase` or `evidence` | MEDIUM | They capture observed shadow events and summaries. | Future boundary should keep event logging bounded and reviewer-safe. | Runtime/evidence behavior. |
| `ShadowAutoscaler`, `ShadowAutoscalerConfig`, `AutoscalingSignal`, `AutoscalingRecommendation`, `AutoscalingAction` | `core` | `lase` | MEDIUM | They provide shadow autoscaling recommendations in LASE contexts. | Must not become live cloud/facility/GPU control. | Runtime shadow behavior. |
| `FailureScenarioRunner`, failure scenario classes, `MitigationAction` | `core` | `lase` or `domain` | MEDIUM | They evaluate controlled failure scenario signals for LASE/explanation. | Keep synthetic/scenario scope explicit. | Runtime shadow behavior. |
| `LaseAllocationShadowSummary` | `api` | `api` view of `lase` evidence | LOW | It shapes LASE shadow status for allocation evaluation responses. | View-only summary; should not mutate allocation. | API/view-only metadata. |
| `LaseDemoScenario`, `LaseDemoScenarioFactory`, `LaseDemoScenarioType` | `core` | `lase.demo` or `evidence.fixture` | LOW | They support local/demo scenario review. | Local fixture/demo only, not production proof. | Runtime fixture behavior. |

## Replay/Evidence Path Candidates

| Current class or package | Current location | Likely future package/category | Migration risk | Why it belongs here | Safety notes | Status |
| --- | --- | --- | --- | --- | --- | --- |
| `LaseShadowReplayEngine`, `LaseShadowReplayReader`, `LaseShadowReplayRecord`, `LaseShadowReplayMetrics`, `LaseShadowReplayReport`, `LaseShadowReplayReportFormatter`, `LaseShadowReplayScoreSummary`, `LaseShadowReplayException` | `core` | `lase.replay` or `evidence.replay` | MEDIUM | They evaluate stored shadow events and produce replay metrics/reports. | Replay evidence must not imply production replay proof or live replay execution. | Runtime evidence behavior. |
| `AdaptiveRoutingScenario`, `AdaptiveRoutingScenarioRunner`, `AdaptiveRoutingScenarioResult`, `AdaptiveRoutingScenarioDetail`, `AdaptiveRoutingScenarioDrilldown`, `AdaptiveRoutingScenarioSummary` | `core` | `evidence.scenario` or `lase.scenario` | MEDIUM | They run deterministic local synthetic scenario comparisons and expose reviewer details. | Scenario output is local evidence, not production validation. | Runtime evidence behavior. |
| `AdaptiveRoutingScenarioEvidencePacket`, `AdaptiveRoutingScenarioEvidencePacketBuilder`, `AdaptiveRoutingScenarioEvidenceSection`, `AdaptiveRoutingScenarioGateEvaluator`, `AdaptiveRoutingScenarioGateEvaluation`, `AdaptiveRoutingScenarioGateFinding` | `core` | `evidence` | MEDIUM | They produce scenario evidence packets and reviewer findings. | Evidence packets must stay reviewer metadata unless future gates approve more. | Runtime evidence behavior. |
| `AdaptiveRoutingExperiment*` classes | `core` | `lase.experiment` or `evidence.experiment` | MEDIUM | They support adaptive routing experiment fixtures, reports, and services. | Keep offline/shadow experiment semantics distinct from live production routing. | Runtime evidence behavior. |
| `EnterpriseLabEvidenceExporter`, `EnterpriseLabRunService`, `EnterpriseLabScenarioCatalogService`, lab records | `lab` | `evidence.lab` | MEDIUM | They generate and expose bounded Enterprise Lab evidence. | Evidence output remains ignored/local unless separately approved. | Runtime evidence behavior. |
| CLI evidence helpers such as `EvidenceInventoryService`, `EvidenceCatalogDiffService`, `EvidenceHandoffPolicyService`, `EvidencePolicyTemplateService`, `EvidenceTrainingScorecardService`, `ReportChecksumManifestService` | `cli` | `tooling.evidence` | MEDIUM | They support local evidence inspection and review workflows. | Do not turn tooling into upload/share/download/export production behavior during boundary work. | CLI evidence behavior. |

## Reviewer Metadata Path Candidates

| Current class or package | Current location | Likely future package/category | Migration risk | Why it belongs here | Safety notes | Status |
| --- | --- | --- | --- | --- | --- | --- |
| `RoutingComparisonResponse`, `RoutingComparisonResultResponse`, `RoutingComparisonService`, `RoutingController`, request/response records | `api` | `api` | HIGH | They shape `/api/routing/compare` and expose reviewer-readable routing metadata. | API semantics and field meanings must not change during inventory or package moves. | API/view runtime behavior. |
| `RoutingDecisionVectorResponse`, `CandidateDecisionVectorResponse`, `RoutingDecisionDeltaAnalysis*`, `DominantFactorAnalysis*`, `ScoreFactor*Response` | `api` | `api` plus `evidence` services | MEDIUM | They expose decision-vector, dominant-factor, and selected-vs-alternative evidence. | Must not claim exact production scoring or hidden scoring proof. | API/view metadata. |
| `RoutingDecisionReplaySnapshot*`, `RoutingDecisionReplayReconstructionTrace*`, `RoutingDecisionReplayCapsule*`, `RoutingDecisionReplayReadinessChecklist*` | `api` | `api.evidence` | MEDIUM | They expose read-only replay evidence lanes. | Must not execute replay, what-if mutation, persistence, or export/share/download behavior. | API/view metadata. |
| `RoutingDecisionReplayEvidence*` services/responses/items | `api` | `api.evidence` or `evidence` | MEDIUM | They expose source maps, boundary summaries, field inventories, null-safety summaries, rollups, navigation, dependency, and closure metadata. | Preserve explicit not-proven boundaries and unknown states. | API/view metadata. |
| `EnterpriseLab*Controller`, `EvidenceTrainingController`, reviewer/operator summary controllers | `api` | `api` | LOW to MEDIUM | They expose local reviewer, operator, scenario, training, and evidence views. | Must stay local/reviewer-facing and not become production certification proof. | API/view runtime behavior. |
| Static UI files such as `routing-demo.html`, `load-balancing-cockpit.html`, `enterprise-lab-reviewer.html`, `operator-evidence-dashboard.html`, `adaptive-routing-scenarios.html`, `ci-evidence-gate.html`, `evidence-timeline.html`, and `evidence-export-packet.html` | `src/main/resources/static` | `api/view` | LOW | They render reviewer/operator metadata from existing APIs. | No external scripts/CDNs and no unsafe claims. | Static API/view-only surface. |

## Domain Model Candidates

| Current class or package | Current location | Likely future package/category | Migration risk | Why it belongs here | Safety notes | Status |
| --- | --- | --- | --- | --- | --- | --- |
| `Server`, `ServerType`, `IServerType` | `core` | `domain` plus allocation-owned mutable state review | HIGH | They model server identity, type, capacity, health, and metrics. | `Server` appears mutable; move only after tests pin behavior. | Runtime domain/allocation behavior. |
| `ServerStateVector` | `core` | `domain` | MEDIUM | It is a snapshot-style candidate state for routing comparison and LASE. | Good future snapshot vocabulary candidate. | Runtime domain model. |
| `RoutingDecision`, `RoutingDecisionExplanation`, `RoutingComparisonReport`, `RoutingComparisonResult`, `RoutingStrategyId` | `core` | `domain` | MEDIUM | They describe routing decisions and comparison reports. | Do not change serialized API semantics during model moves. | Runtime domain model. |
| `CandidateFactorContributionSummary`, `ScoreFactorContribution`, `ScoreFactorDirection`, `ScoreFactorExactness` | `core` | `domain.evidence` or `domain.scoring` | MEDIUM | They represent score contribution metadata. | Avoid hidden scoring changes. | Runtime domain/scoring metadata. |
| `NetworkAwarenessSignal`, `LoadSheddingSignal`, `AutoscalingSignal`, `FailureScenarioSignal` | `core` | `domain.signal` | MEDIUM | They model local signal inputs for routing/LASE contexts. | Future ExternalSignalPort concepts must remain read-only and separate. | Runtime domain model. |
| LASE report/event/summary records | `core` | `domain.lase` or `lase` | MEDIUM | They represent LASE facts and evidence. | Keep shadow-only semantics explicit. | Runtime evidence/domain model. |

## Infrastructure / Cloud / External Integration Candidates

| Current class or package | Current location | Likely future package/category | Migration risk | Why it belongs here | Safety notes | Status |
| --- | --- | --- | --- | --- | --- | --- |
| `CloudManager` | `core` | `infrastructure.cloud` | HIGH | It owns AWS Auto Scaling, CloudWatch, EC2 client interaction, guarded live/dry-run behavior, file logging, and background cloud jobs. | LASE must not directly call `CloudManager` or future cloud/facility/grid/GPU control paths. | Runtime infrastructure behavior. |
| `CloudAwsClients`, `CloudConfig`, `CloudMetricsCoordinator`, `CloudMutationSource` | `core` | `infrastructure.cloud` | HIGH | They configure and coordinate cloud clients/metrics/mutation boundaries. | Keep live mutation guardrails separate and explicit. | Runtime infrastructure behavior. |
| `com.richmond423.loadbalancerpro.api.proxy` classes | `api.proxy` | `infrastructure.proxy` or `api.proxy` | HIGH | They implement optional reverse proxy behavior, route planning, status, metrics, and private-network validation boundaries. | LASE must not directly alter proxy behavior. | Runtime proxy behavior. |
| `ProxyDemoFixtureLauncher` | `demo` | `tooling.demo` | LOW | It provides local fixture servers for proxy demos. | Local loopback demo only, not production validation. | Local runtime fixture. |
| `Utils`, `CsvServerLogParser`, `JsonServerLogParser` | `util` | `infrastructure.file` or `tooling` | MEDIUM | They parse/import/export server logs and reports. | Do not expand filesystem/process/export behavior in boundary inventory work. | Runtime utility behavior. |

## API And Static UI Surface Candidates

| Current class or package | Current location | Likely future package/category | Migration risk | Why it belongs here | Safety notes | Status |
| --- | --- | --- | --- | --- | --- | --- |
| `LoadBalancerApiApplication` | `api` | `api` bootstrap | LOW | Spring Boot API entry point. | Not part of LASE enforcement. | Runtime API behavior. |
| `AllocatorController`, `RoutingController`, `ScenarioReplayController`, `RemediationReportController` | `api` | `api` | MEDIUM | They expose allocation, routing, scenario replay, and remediation endpoints. | Do not change API behavior during inventory work. | Runtime API behavior. |
| `EnterpriseLab*Controller`, `EvidenceTrainingController` | `api` | `api.view` or `api.lab` | LOW to MEDIUM | They expose Enterprise Lab and training review surfaces. | Keep reviewer/lab claims bounded. | Runtime API/view behavior. |
| API request/response records | `api` | `api.dto` | MEDIUM | They define the external API response shapes and view models. | Field semantics must stay stable unless separately approved. | API/view behavior. |
| Static reviewer pages | `src/main/resources/static` | `api/view` | LOW | They render local reviewer/operator workflows. | No external scripts/CDNs; no production certification claims. | Static UI behavior. |

## Configuration Layer Candidates

| Current class or package | Current location | Likely future package/category | Migration risk | Why it belongs here | Safety notes | Status |
| --- | --- | --- | --- | --- | --- | --- |
| `AdaptiveRoutingPolicyConfiguration`, `AdaptiveRoutingPolicyProperties` | `api.config` | `config` | MEDIUM | They expose controlled policy mode configuration. | Config must not hide production activation of LASE authority. | Runtime config. |
| `ApiSecurityConfiguration`, `AuthModeConfiguration`, `AuthProperties`, `ProdApiKeyFilter`, `SecurityHeadersFilter` | `api.config` | `config.security` | MEDIUM | They configure API auth and security boundaries. | Keep auth behavior separate from architecture inventory. | Runtime config/security. |
| `ApiRateLimitFilter`, `RequestSizeLimitFilter` | `api.config` | `config.security` | MEDIUM | They enforce request safety limits. | Do not change request behavior during inventory work. | Runtime config/security. |
| `TelemetryConfiguration`, `TelemetryProperties`, `TelemetryStartupGuard` | `api.config` | `config.telemetry` | MEDIUM | They guard telemetry configuration. | No new telemetry/storage/persistence in this sprint. | Runtime config. |
| `WebConfig` | `api.config` | `config.web` | LOW | It configures web behavior. | No web behavior changes in inventory work. | Runtime config. |
| `CliConfig`, `GuiConfig`, `ConfigLoader` | `cli` and `gui` | CLI/UI config or legacy boundary | LOW to MEDIUM | They configure CLI and GUI behavior. | Future moves should keep UI/CLI defaults intact. | Runtime config. |

## Boundary Risks Observed In Current Layout

The current layout is workable but mixed. Observed future migration risks:

- `core` contains live allocation, LASE, replay, evidence, scenario, cloud, scoring, and domain models together.
- `LoadBalancer` contains live allocation state and a `LaseShadowAdvisor` hook, so future separation needs careful observation-only boundaries.
- `AllocatorService` currently orchestrates allocation/evaluation, LASE observation, policy decisions, remediation planning, and API response shaping.
- `AdaptiveRoutingPolicyEngine` can choose final decisions under guarded active-experiment mode, so future package naming must distinguish policy-gated lab influence from ordinary shadow evidence.
- `LaseEvaluationEngine` uses routing, load shedding, concurrency, autoscaling, and failure scenario components, so future moves must prevent shadow recommendations from looking like live production control.
- Replay/evidence classes live in both `core` and `api`, so future package moves should happen only after stable DTO and documentation tests are in place.
- `CloudManager` currently lives in `core`, near LASE and allocation classes. Future boundaries should keep LASE from direct cloud/facility/grid/GPU control calls.
- Proxy classes live under `api.proxy`; future LASE work must not directly mutate proxy routes, status, private-network validation, or runtime forwarding behavior.
- File/export utility classes exist under `util`; future evidence work must not accidentally introduce new upload/share/download/export/PDF/ZIP behavior.
- Static pages are broad reviewer surfaces; future architecture docs must avoid implying runtime enforcement before it exists.

These are migration-readiness observations, not defects and not proof of unsafe behavior.

## Safe Future Migration Sequence

A safe future migration sequence should be:

1. Inventory current classes and responsibilities.
2. Add package-boundary documentation.
3. Add non-invasive tests for docs and naming.
4. Introduce domain package only when separately approved.
5. Introduce LaseObservationPort only when separately approved.
6. Move LASE classes only when separately approved.
7. Add ArchUnit or equivalent package-boundary enforcement only when separately approved and dependency/build impact is reviewed.
8. Never combine package moves with behavior changes.
9. Never combine boundary enforcement with production claims.

Package moves must not be combined with behavior changes. ArchUnit or package enforcement is future-only unless separately approved.

## Future Enforcement Options

Future enforcement options, each requiring separate approval:

- documentation-only package boundary contract updates;
- naming guard tests that confirm intended packages and links;
- the staged package-boundary enforcement plan in [`LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md`](LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md);
- a future `domain` package for pure models;
- a future read-only `LaseObservationPort`;
- a future `lase` package for shadow evaluation and replay/evidence;
- a future `infrastructure.cloud` boundary for `CloudManager` and AWS client code;
- future ArchUnit or equivalent package-boundary enforcement after build/dependency impact review;
- future dependency-direction rules that prevent LASE from calling proxy, cloud, facility, grid, GPU, or allocation mutation APIs.

None of these are implemented by this inventory.

## Non-Goals And Safety Boundaries

Explicit non-goals:

- no production Java runtime behavior;
- no records/classes/interfaces under `src/main/java`;
- no package moves or refactors;
- no ArchUnit or new dependency;
- no Maven build changes;
- no Docker, CI, release, signing, registry, or governance changes;
- no proxy behavior change;
- no strategy behavior change;
- no core routing behavior change;
- no scoring-internals behavior change;
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
- no live-cloud validation claim;
- no real-tenant validation claim;
- no GPU orchestration claim;
- no power/grid control claim;
- no carbon-aware routing implementation claim;
- no facility automation claim;
- no production readiness claim;
- no production certification claim.

This inventory does not claim LASE boundary is runtime-enforced. This inventory does not claim LASE package boundary is enforced. It does not claim ArchUnit enforcement exists. It does not claim current production readiness or production certification.

## Reviewer-Facing Value

This inventory helps reviewers see the current tree before any enforcement work:

- current live allocation candidates are named;
- current LASE shadow/evaluation candidates are named;
- current replay/evidence candidates are named;
- current reviewer metadata/API/view candidates are named;
- current domain model candidates are named;
- current cloud/proxy/infrastructure candidates are named;
- current configuration candidates are named;
- future risks are visible before refactors;
- future migration steps are intentionally separated from behavior changes and production claims.

Reviewers should treat this as a migration-readiness map only. It is not implementation proof, not package enforcement, not runtime enforcement, and not production certification.
