# Decision Explorer Phase 1 Reviewer Examples

Status: active / phase1-docs-examples.

Classification: WARN / decision-explorer-phase1-examples.

Campaign slot: DX-P1-G07.

Related Phase 1 board: [`DECISION_EXPLORER_PHASE1_CAMPAIGN_BOARD.md`](DECISION_EXPLORER_PHASE1_CAMPAIGN_BOARD.md).

Related Phase 1 scope: [`DECISION_EXPLORER_PHASE1_ARCHITECTURE_SCOPE.md`](DECISION_EXPLORER_PHASE1_ARCHITECTURE_SCOPE.md).

Related API contracts: [`../API_CONTRACTS.md`](../API_CONTRACTS.md).

Related reviewer trust map: [`../REVIEWER_TRUST_MAP.md`](../REVIEWER_TRUST_MAP.md).

Related local page: `/decision-explorer.html`.

## Purpose

This document gives reviewer-facing examples for the current Decision Explorer Phase 1 local surface. The examples are
grounded in the implemented same-origin static page, the bounded read-only `POST /api/routing/decision-explorer` data
surface, and source-visible tests for `DecisionExplorerPayloadV1`.

DX-P1-G07 is documentation and guard-test only. It does not change Java production behavior, routing behavior, strategy
scoring, proxy behavior, Maven, CI, Docker, Compose, scripts, endpoints, runtime resources, deployment, storage,
secrets, external targets, cloud targets, tenant targets, rulesets, or required checks.

The examples are review aids. They do not create benchmark evidence, production certification, runtime enforcement,
traffic-shifting authority, replay execution, export behavior, storage behavior, evidence-packet generation, autonomous
production action, or broader automation.

## What Exists Now

Decision Explorer Phase 1 currently provides these bounded inspection surfaces:

- `/decision-explorer.html`, a same-origin static reviewer page for local inspection.
- `POST /api/routing/decision-explorer`, a read-only data surface that accepts the same `RoutingComparisonRequest`
  shape used by `POST /api/routing/compare`.
- `DecisionExplorerPayloadV1` readouts derived from already-built routing comparison evidence.
- Display sections for decision summary, selected candidate, candidate set, factor contributions, policy gates,
  decision diffs, evidence packet readouts, agent structured output, warnings, unknowns, not-proven boundaries, and raw
  payload.

The surface remains additive, read-only, and simulation-only. It does not shift traffic, mutate routing, call cloud or tenant systems, persist storage, execute replay, export files, generate evidence packets, or approve production use.

## Example Reviewer Path

1. Start the app in the usual local/default review mode.
2. Open `http://localhost:8080/decision-explorer.html`.
3. Leave the optional API key blank in local/default mode, or enter it only when a protected profile is intentionally
   configured for review. The page keeps the optional API key in browser page memory only.
4. Use `Run Decision Explorer` to send the deterministic synthetic `green` and `blue` candidate request.
5. Inspect the selected candidate, candidate ordering, factor contribution rows, policy gate rows, warnings, unknowns,
   not-proven boundaries, and raw JSON payload.
6. Cross-check the bounded route description in [`../API_CONTRACTS.md`](../API_CONTRACTS.md) before treating the output
   as reviewer evidence.

This path is local reviewer inspection only. It is not a production workflow, deployment approval, traffic-control
approval, certification gate, benchmark, load test, stress test, throughput proof, p95/p99 proof, export workflow, or
replay workflow.

## Example Request Fragment

The static page uses deterministic synthetic telemetry shaped like this fragment:

```json
{
  "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
  "servers": [
    {
      "serverId": "green",
      "healthy": true,
      "averageLatencyMillis": 20.0,
      "p95LatencyMillis": 40.0,
      "p99LatencyMillis": 80.0,
      "recentErrorRate": 0.01,
      "queueDepth": 1
    },
    {
      "serverId": "blue",
      "healthy": true,
      "averageLatencyMillis": 35.0,
      "p95LatencyMillis": 120.0,
      "p99LatencyMillis": 220.0,
      "recentErrorRate": 0.15,
      "queueDepth": 10
    }
  ]
}
```

The request is caller-provided synthetic routing telemetry. It is not real tenant telemetry, live-cloud telemetry,
production traffic, or benchmark/load/stress input.

## Example Response Fragment

A reviewer should expect a `DecisionExplorerPayloadV1` array. The exact full payload should come from the running app or
from source-visible tests, but the important review cues are:

```json
[
  {
    "readOnly": true,
    "simulationOnly": true,
    "payloadObject": "DecisionExplorerPayloadV1",
    "contractVersion": "1",
    "decisionReadout": {
      "selectedStrategy": "TAIL_LATENCY_POWER_OF_TWO",
      "selectedCandidateId": "green"
    },
    "notProvenBoundaries": [
      "no production readiness",
      "no live-cloud validation",
      "no real-tenant validation",
      "no benchmark/load/stress proof"
    ]
  }
]
```

This fragment is intentionally partial. Reviewers should not infer hidden routing internals, exact production scoring,
missing factors, storage behavior, export behavior, replay behavior, or evidence-packet generation from fields that are
absent, null, unknown, unavailable, not applicable, or not implemented.

## Example Human Review Questions

Human reviewers can use the current surface to ask:

- Which candidate was selected by the returned routing comparison evidence?
- Which candidates were visible, and is the selected candidate listed first in the stable display order?
- Which factor contributions were returned, and which signals remain unknown or unexposed?
- Which policy gates are display-only reviewer cues, such as the read-only boundary and simulation-only boundary?
- Which decision diff and evidence packet readouts are present, partial, unavailable, or not implemented?
- Which not-proven boundaries should remain attached to the review?

These questions support understanding. They do not authorize production action, prove production readiness, or imply
runtime enforcement.

## Example AI-Agent Questions

AI agents can parse the structured output to answer bounded questions:

- What is the `payloadObject` and `contractVersion`?
- What is the `decisionReadout.selectedCandidateId`?
- Which `candidateSet[]` item is selected?
- Which `factorContributions[]` names and exactness values are returned?
- Which `policyGateReadouts[]` outcomes are visible?
- Which `warnings[]`, `unknowns[]`, and `notProvenBoundaries[]` should be repeated in a summary?

Agent use is for structured understanding only. It does not enable autonomous production action, live mutation, hidden
network calls, hidden writes, hidden approvals, or broader automation.

## What These Examples Prove

These examples prove only that the repository has source-visible reviewer examples for the bounded Phase 1 Decision
Explorer surface and that guard tests require the examples to preserve current safety wording.

They support local reviewer orientation around:

- the current same-origin page path;
- the current read-only data surface path;
- `DecisionExplorerPayloadV1` and `AgentStructuredOutputV1` vocabulary;
- deterministic candidate ordering and explicit unknown/unavailable handling;
- not-proven boundary visibility.

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
- traffic shifting;
- cloud or tenant mutation;
- autonomous production action;
- broader automation.

## Maintenance Expectations

When the Decision Explorer payload, local page, or API contract changes, reviewers should update these examples only
with source-visible behavior that is covered by tests. Example fragments must stay partial unless a test owns the full
payload. Unsupported behavior must remain named as unknown, unavailable, not applicable, not implemented, or not proven
instead of being inferred.
