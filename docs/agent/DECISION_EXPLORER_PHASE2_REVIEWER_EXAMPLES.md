# Decision Explorer Phase 2 Reviewer Examples

Status: active / phase2-docs-examples.

Classification: WARN / decision-explorer-phase2-examples.

Campaign slot: DX-P2-G10.

Related Phase 2 board: [`DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md`](DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md).

Related Phase 2 scope: [`DECISION_EXPLORER_PHASE2_ARCHITECTURE_SCOPE.md`](DECISION_EXPLORER_PHASE2_ARCHITECTURE_SCOPE.md).

Related API contracts: [`../API_CONTRACTS.md`](../API_CONTRACTS.md).

Related local page: `/decision-explorer.html`.

## Purpose

This document gives reviewer-facing examples for the implemented Decision Explorer Phase 2 surfaces. The examples are
grounded in source-visible DTO, service, controller, API-contract, and static-page tests that cover the scenario
catalog, factor drill-down readouts, candidate comparison rows, reviewer badges, and additive API hardening.

DX-P2-G10 is documentation and guard-test only. It does not change Java production behavior, routing behavior, strategy
scoring, proxy behavior, Maven, CI, Docker, Compose, scripts, endpoints, runtime resources, deployment, storage,
secrets, external targets, cloud targets, tenant targets, rulesets, or required checks.

The examples are review aids for local, same-origin, read-only, simulation-only inspection. They do not create
production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement,
benchmark/load/stress evidence, throughput/p95/p99 evidence, replay execution, export behavior, storage behavior,
evidence-packet generation, autonomous production action, traffic shifting, or broader automation.

## Grounding Sources

These examples are partial fragments. Reviewers should treat the running app and source-visible tests as the owned
truth for full payload shape and ordering:

- `DecisionExplorerScenarioCatalogServiceTest` proves deterministic scenario catalog metadata.
- `DecisionExplorerScenarioCatalogV1Test` proves catalog DTO defaults, null handling, sorting, and boundary wording.
- `RoutingControllerTest` proves the same-origin `GET /api/routing/decision-explorer/scenarios` route.
- `RoutingOpenApiContractTest` proves the scenario catalog and additive Phase 2 DTO schemas remain in OpenAPI.
- `DecisionExplorerPayloadServiceTest` proves factor drill-down and candidate comparison rows are derived from returned
  routing comparison evidence.
- `DecisionExplorerApiContractHardeningTest` proves Phase 1 fields remain present while Phase 2 arrays remain additive,
  deterministic, and present for partial or unknown evidence.
- `DecisionExplorerStaticPageTest` and `DecisionExplorerReviewerNavigationTest` prove the static reviewer page exposes
  scenario selection, filtering, candidate comparison, factor drill-down, reviewer badges, and not-proven boundaries.

## Scenario Catalog Example

Reviewers can request local synthetic scenario metadata:

```http
GET /api/routing/decision-explorer/scenarios
```

Representative catalog excerpt:

```json
{
  "readOnly": true,
  "simulationOnly": true,
  "payloadObject": "DecisionExplorerScenarioCatalogV1",
  "contractVersion": "v1",
  "scenarios": [
    {
      "scenarioObject": "DecisionExplorerScenarioV1",
      "scenarioId": "normal-balanced-load",
      "scenarioCategory": "HEALTHY_BASELINE",
      "evidenceStatus": "AVAILABLE",
      "displayOrder": 10,
      "sourceReferenceIds": [
        "AdaptiveRoutingExperimentFixtureCatalog:normal-balanced-load",
        "DecisionExplorerPayloadV1",
        "POST /api/routing/decision-explorer"
      ]
    },
    {
      "scenarioId": "stale-signal",
      "scenarioCategory": "PARTIAL_EVIDENCE",
      "evidenceStatus": "PARTIAL"
    },
    {
      "scenarioId": "all-unhealthy-degradation",
      "scenarioCategory": "NO_HEALTHY_SERVER",
      "evidenceStatus": "UNKNOWN"
    }
  ]
}
```

The catalog is orientation metadata. Scenario selection on the page does not run routing by itself, shift traffic,
persist state, execute replay, export evidence, generate evidence packets, or call external systems.

## Decision Explorer Payload Example

Reviewers can request the existing read-only Decision Explorer payload with the same request shape used by routing
comparison:

```http
POST /api/routing/decision-explorer
```

Representative payload excerpt:

```json
[
  {
    "readOnly": true,
    "simulationOnly": true,
    "payloadObject": "DecisionExplorerPayloadV1",
    "contractVersion": "v1",
    "decisionReadout": {
      "selectedStrategy": "TAIL_LATENCY_POWER_OF_TWO",
      "selectedCandidateId": "green"
    },
    "candidateComparisons": [
      {
        "candidateId": "green",
        "selected": true,
        "displayOrder": 1,
        "comparisonStatus": "SELECTED",
        "visibleSignals": ["healthState=healthy"],
        "unknownSignals": ["hidden routing internals"]
      }
    ],
    "factorDrilldowns": [
      {
        "factorName": "healthState",
        "candidateId": "green",
        "observedValueOrStatus": "healthy",
        "influenceCategory": "SUPPORTS_SELECTION",
        "evidenceStatus": "AVAILABLE"
      }
    ],
    "notProvenBoundaries": [
      "no production readiness",
      "no live-cloud validation",
      "no real-tenant validation",
      "no benchmark/load/stress proof",
      "no throughput/p95/p99 proof",
      "no replay/export proof",
      "no storage proof",
      "no evidence-packet generation"
    ]
  }
]
```

This fragment is intentionally partial. Missing, null, empty, unknown, unavailable, not applicable, or not implemented
fields must stay visible instead of being inferred into hidden routing internals or production proof.

## Factor Drill-Down Example

Factor drill-down rows are derived from already-returned `ScoreFactorContributionResponse` evidence. A reviewer can
use a row to ask:

- Which factor name is visible?
- Which candidate does the factor apply to?
- What observed value or status was returned?
- Does the returned influence category support, weaken, or remain unknown for the selected route?
- Which warnings, unknowns, and source reference IDs should stay attached?

Factor drill-down rows do not recompute scores, retune weights, change routing behavior, mutate proxy behavior, persist
storage, export evidence, execute replay, generate evidence packets, call external systems, or prove production
readiness.

## Candidate Comparison Example

Candidate comparison rows are derived from already-built `CandidateReadoutV1` evidence. The selected candidate remains
first, followed by deterministic non-selected candidates. A reviewer can compare:

- selected versus non-selected candidate identity;
- visible signals and unknown signals;
- reason codes and policy gate IDs;
- score delta when both scores are visible;
- evidence reference IDs, warnings, unknowns, and boundary text.

Candidate comparison rows do not create exact hidden scoring, benchmark proof, throughput proof, traffic shifting,
runtime enforcement, storage, replay, export, or evidence-packet behavior.

## Reviewer UI Workflow Example

1. Start the app in the local/default review mode.
2. Open `http://localhost:8080/decision-explorer.html`.
3. Load the same-origin scenario catalog and filter by `PARTIAL_EVIDENCE`, `NO_HEALTHY_SERVER`, `AVAILABLE`,
   `PARTIAL`, or `UNKNOWN`.
4. Select a scenario as reviewer orientation only.
5. Run the Decision Explorer request from the page.
6. Inspect reviewer badges, selected candidate, candidate set, candidate comparison rows, factor contributions, factor
   drill-down rows, warnings, unknowns, not-proven boundaries, and raw JSON payload.
7. Cross-check [`../API_CONTRACTS.md`](../API_CONTRACTS.md) before treating the output as contract evidence.

The page keeps the optional API key in browser memory only. It uses same-origin app routes only and does not use
external services, cloud targets, tenant targets, persistent browser storage, server-side storage, export handles,
replay execution, evidence-packet generation, or production approval controls.

## Human Reviewer Questions

Human reviewers can use Phase 2 examples to ask:

- Which scenario category and evidence status frame the local synthetic review?
- Which selected route and non-selected candidates are visible?
- Which factor drill-down rows are available, partial, warning, or unknown?
- Which comparison rows preserve selected-first deterministic ordering?
- Which reviewer badges restate selected route, warning, unknown, partial evidence, deterministic evidence, and
  not-proven boundary states?
- Which not-proven boundaries must stay attached to the review?

These questions support understanding. They do not authorize production action, prove production readiness, or imply
runtime enforcement.

## AI-Agent Structured Questions

AI agents can parse Phase 2 fields to answer bounded questions:

- What are `payloadObject` and `contractVersion`?
- Which `DecisionExplorerScenarioV1.scenarioId`, `scenarioCategory`, and `evidenceStatus` are selected?
- Which `DecisionExplorerCandidateComparisonRowV1` is selected and what is its `displayOrder`?
- Which `DecisionFactorDrilldownV1` rows expose warning or unknown states?
- Which `warnings[]`, `unknowns[]`, and `notProvenBoundaries[]` should be repeated in a summary?

Agent use is for structured understanding only. It does not enable autonomous production action, live mutation, hidden
network calls, hidden writes, hidden approvals, traffic shifting, or broader automation.

## What These Examples Prove

These examples prove only that the repository has source-visible reviewer examples and guard tests for the implemented
Phase 2 Decision Explorer surfaces. They support local reviewer orientation around:

- deterministic scenario catalog metadata;
- additive same-origin scenario catalog API behavior;
- factor drill-down vocabulary;
- candidate comparison vocabulary;
- reviewer badge language;
- additive API compatibility and null/unknown handling;
- static same-origin page workflow;
- explicit unsupported-claim boundaries.

## What These Examples Do Not Prove

These examples do not prove:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime enforcement;
- benchmark/load/stress evidence;
- throughput/p95/p99 evidence;
- replay/export behavior;
- storage behavior;
- evidence-packet generation;
- autonomous production action;
- traffic shifting;
- carbon-aware routing;
- GPU orchestration;
- power/grid control;
- facility automation;
- broader automation.

## Maintenance Expectations

When the scenario catalog, payload fields, static page, or API contract changes, reviewers should update these examples
only with source-visible behavior covered by tests. Example fragments should remain partial unless a test owns the full
payload. Unsupported behavior must remain named as unknown, unavailable, not applicable, not implemented, or not proven
instead of being inferred.
