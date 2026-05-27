# Decision Explorer Phase 2 Architecture And Scope

Status: active / phase2-scope.

Classification: WARN / decision-explorer-phase2-scope.

Campaign slot: DX-P2-G01.

Related Phase 2 campaign board: [`DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md`](DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md).

Related Phase 1 handoff: [`DECISION_EXPLORER_PHASE1_FINAL_HANDOFF.md`](DECISION_EXPLORER_PHASE1_FINAL_HANDOFF.md).

Related Phase 1 scope: [`DECISION_EXPLORER_PHASE1_ARCHITECTURE_SCOPE.md`](DECISION_EXPLORER_PHASE1_ARCHITECTURE_SCOPE.md).

Related API contracts: [`../API_CONTRACTS.md`](../API_CONTRACTS.md).

Related reviewer trust map: [`../REVIEWER_TRUST_MAP.md`](../REVIEWER_TRUST_MAP.md).

Related local reviewer page: `/decision-explorer.html`.

## Purpose

This document starts Decision Explorer Implementation Phase 2 with a source-visible architecture and scope contract.
Phase 2 builds on the completed Phase 1 surface at main SHA `28c8bc10e1aa553a3c53aac70883c04431d55cc2`.

DX-P2-G01 is documentation and guard-test only. It does not add Java runtime classes, controllers, routes, static UI
behavior, storage behavior, export behavior, replay execution, evidence-packet generation, Maven configuration, CI
workflow behavior, Docker behavior, Compose behavior, scripts, deployment behavior, secrets, credentials, external
targets, cloud targets, tenant targets, rulesets, branch protection, releases, tags, package publication, container
pushes, registry logins, GitHub settings changes, or broader automation.

Phase 2 remains additive, same-origin, read-only, simulation-only, and grounded in already computed routing comparison evidence.
Later scoped Phase 2 PRs may add deterministic scenario catalog support, richer explanation DTOs, companion
API data, static UI controls, reviewer docs, and guard tests. Each later PR must prove its own behavior with focused
tests, full local verification, current-head PR checks, and post-merge main checks before it counts.

## Starting Point

Phase 2 starts after Decision Explorer Implementation Phase 1 completed and main was green at
`28c8bc10e1aa553a3c53aac70883c04431d55cc2`.

Phase 1 is treated as a compatibility contract:

- `DecisionExplorerPayloadV1` and related readouts remain the stable Phase 1 payload vocabulary.
- `DecisionExplorerPayloadService` remains read-only and only reshapes already-built routing comparison evidence.
- `POST /api/routing/decision-explorer` remains a same-origin, read-only data surface.
- `/decision-explorer.html` remains a static reviewer page with no persistent browser storage and no mutation handles.
- Existing `warnings`, `unknowns`, `notProvenBoundaries`, and `boundaryNote` fields remain source-visible.
- Existing reviewer docs and guard tests remain claim contracts, not cosmetic copy.

Phase 2 must preserve Phase 1 API consumers. New fields or companion DTOs must be additive, deterministic, null-safe,
and compatible with the existing response shape unless a later PR explicitly proves a safer additive companion endpoint.

## Phase 2 Objective

Decision Explorer Phase 2 should make the read-only reviewer experience more useful without upgrading safety claims.
The implementation target is:

- deterministic scenario catalog views for safe local synthetic review situations;
- scenario catalog metadata for healthy baseline, partial evidence, unknown or no-healthy-server fixture, and any
  existing safe local/demo scenarios already represented in the app;
- factor-level drill-down summaries with observed value or status, influence category, explanation, warning or unknown
  state, and source evidence reference where available;
- reviewer-friendly candidate comparison rows with deterministic selected-first ordering, visible signal summaries,
  unknown signal summaries, reason codes, policy gate references, and source evidence references where available;
- static UI scenario selection and filtering controls that call only same-origin app APIs;
- static UI drill-down and comparison sections with clear empty, partial, warning, unknown, deterministic evidence, and
  not-proven boundary states;
- API and documentation hardening that proves Phase 1 compatibility and preserves unsupported-claim boundaries;
- a final handoff that lists Phase 2 PRs, merge commits, implemented behavior, verification, safety audit, and
  remaining limits.

## Data Source Boundary

Phase 2 must reuse existing source-visible LoadBalancerPro decision evidence. It may read or transform already built
objects such as `RoutingComparisonResponse`, `RoutingComparisonResultResponse`, `DecisionExplorerPayloadV1`,
`DecisionReadoutV1`, `CandidateReadoutV1`, `FactorContributionV1`, `PolicyGateReadoutV1`, `DecisionDiffReadoutV1`,
`EvidencePacketReadoutV1`, `RoutingDecisionVectorResponse`, `CandidateDecisionVectorResponse`,
`ScoreFactorContributionResponse`, `DominantFactorAnalysisResponse`, and `RoutingDecisionDeltaAnalysisResponse`.
It remains grounded in already computed routing comparison evidence and must not invent hidden scoring signals.

Scenario catalog fixtures must be deterministic, local-app safe, and synthetic. They may describe request presets or
sample inputs for the existing Decision Explorer path, but they must not call cloud services, tenant systems, external
network targets, non-loopback required targets, storage, replay execution, export behavior, or traffic-shifting
controls.

Unknown, unavailable, partial, not-applicable, and not-implemented states must remain distinct. Absence must stay
visible to both human reviewers and AI-agent structured consumers.

## API Boundary

Phase 2 may add additive DTOs or companion readouts for:

- `DecisionExplorerScenarioCatalogV1`;
- `DecisionExplorerScenarioV1`;
- `DecisionExplorerFactorDrilldownV1`;
- `DecisionExplorerCandidateComparisonRowV1`;
- `DecisionExplorerReviewerBadgeV1`;
- additive payload fields or a bounded companion response if a later PR proves that is the safer compatibility path.

Any Phase 2 API change must preserve these limits:

- read-only request handling;
- same-origin local app usage;
- no allocation mutation;
- no proxy forwarding;
- no cloud, tenant, or external target call;
- no storage write;
- no replay execution;
- no export, upload, download, PDF, ZIP, or evidence-packet generation;
- explicit `notProvenBoundaries`;
- deterministic ordering for scenarios, candidates, factors, warnings, unknowns, source references, reviewer badges,
  and boundary flags;
- Phase 1 response compatibility for existing clients.

## UI Boundary

Phase 2 may update the static `/decision-explorer.html` page with richer reviewer controls. The UI may add:

- scenario selector;
- scenario category or evidence-status filtering;
- selected route badge;
- warning badge;
- unknown badge;
- partial evidence badge;
- deterministic evidence badge;
- factor drill-down section;
- candidate comparison table;
- empty, partial, warning, unknown, and not-proven boundary states;
- reviewer links back to Phase 2 docs and API contracts.

The UI must consume only same-origin app data from scoped Phase 2 API surfaces or static source-controlled sample
metadata. It must not create server-side exports, uploads, downloads, PDFs, ZIPs, storage writes, replay execution,
live traffic shifting, production approval controls, branch-protection controls, required-check controls, cloud
controls, tenant controls, or autonomous production action.

## Phase 2 PR Sequence

The campaign should remain one small PR at a time:

| Slot | Slice | Expected boundary |
| --- | --- | --- |
| DX-P2-G01 | Campaign board and scope | Documentation and guard only; no runtime behavior. |
| DX-P2-G02 | Scenario catalog model | Additive scenario catalog DTO/model support and unit tests; no endpoint behavior. |
| DX-P2-G03 | Scenario catalog service/API | Bounded same-origin scenario catalog API or additive companion data with controller tests and docs. |
| DX-P2-G04 | Decision factor drill-down | Deterministic factor-level summaries and tests; no routing behavior changes. |
| DX-P2-G05 | Candidate comparison table | Additive comparison rows and tests for ordering, empty, and partial candidates. |
| DX-P2-G06 | UI scenario selector and filtering | Static page controls using same-origin data only. |
| DX-P2-G07 | UI factor drill-down and candidate comparison | Static page display for drill-down and comparison states. |
| DX-P2-G08 | Explanation badges and reviewer language | Reviewer-facing badges, docs language, and no-overclaim guard coverage. |
| DX-P2-G09 | API contract hardening | Compatibility, null-safety, ordering tests, and API docs updates. |
| DX-P2-G10 | Docs and examples | Grounded Phase 2 examples and unsupported-claim guard tests. |
| DX-P2-G11 | Final hardening and navigation polish | Reviewer navigation cleanup, page labels, and edge-case cleanup. |
| DX-P2-G12 | Final handoff | Summarize merged PRs, final main SHA, behavior, verification, safety audit, and Phase 3 recommendation. |

The sequence may split further if a slice becomes too large. It must not stack PRs unless a blocker makes a non-stacked
flow impossible and the blocker is reported.

## Verification Expectations

Each Phase 2 PR must run the strongest practical local verification for its scope before opening a PR:

1. Focused tests for the active slice.
2. Relevant Decision Explorer selector bundle.
3. `mvn -q test`.
4. `mvn -q "-DskipTests" package`.
5. `mvn -B package`.
6. `git diff --check`.
7. `git diff --cached --check` when staged changes exist.
8. `git diff --check origin/main...HEAD`.
9. `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` when available and applicable.

Remote PR checks must be current-head green before merge: Build, Test, Package, Smoke; Analyze Java / CodeQL; and
Dependency Review when present. Pending, failed, cancelled, stale, skipped-only, duplicate-only, or wrong-head checks
do not count. After merge, local main must be fast-forwarded, local verification must pass, and main CI plus CodeQL
must be green for the merge commit before the next PR starts.

## Scope And Safety Audit

Phase 2 allows additive implementation only inside explicitly scoped PRs. Any PR that changes production routing,
scoring, proxy behavior, Maven configuration, CI/workflow files, Dockerfile, Compose behavior, scripts, deployment,
secrets, external targets, cloud targets, tenant targets, rulesets, releases, tags, package publication, container
pushes, registry logins, or GitHub settings must stop unless the user explicitly scopes that change.

Phase 2 must preserve no hidden side effects.

Phase 2 must preserve no autonomous production action.

Phase 2 must preserve no live mutation.

Phase 2 must preserve read-only and simulation-only boundaries for every Decision Explorer surface.

Phase 2 must preserve same-origin/local-app-only behavior for the Decision Explorer UI and API.

## Remaining Not-Proven Boundaries

This architecture and scope contract does not prove:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime enforcement;
- benchmark/load/stress evidence;
- throughput/p95/p99 evidence;
- replay execution;
- export behavior;
- storage behavior;
- evidence packet generation;
- runtime scenario catalog, factor drill-down, candidate comparison, filtering, badge, or Phase 2 UI behavior in this
  first slice;
- autonomous production action;
- traffic shifting;
- carbon-aware routing;
- GPU orchestration;
- power/grid control;
- facility automation;
- broader automation.
