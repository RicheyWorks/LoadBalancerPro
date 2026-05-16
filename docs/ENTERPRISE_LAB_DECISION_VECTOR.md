# Enterprise Lab Decision Vector Contract

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

The Enterprise Lab Decision Vector is the structured explanation object for one controlled lab routing decision. It turns a final selected-backend outcome into replayable, explainable, and testable local lab evidence without claiming production telemetry, production monitoring, exact production scoring, or production certification.

## Why the Lab Needs It

The cockpit already explains visible outcomes: selected strategy, selected backend/server, candidate signals, known versus unknown signals, and selected-vs-alternative notes. A Decision Vector gives those explanations a contract so future work can add factor contribution analysis, decision replay, what-if experiments, structured decision logging, strategy plugin explainability, and data center signal modeling without inventing hidden scoring.

A Decision Vector differs from a simple reason string because it separates:

- The selected strategy and selected backend/server.
- The full candidate backend list visible to the controlled lab run.
- The selected candidate vector.
- Non-selected candidate vectors.
- Known visible signals from unknown or unexposed signals.
- Degradation, fallback, and recovery indicators.
- Selected-vs-alternative explanation notes.
- Exact scoring availability or absence.
- Factor contribution availability or absence.
- Replay readiness and future replay gaps.
- Lab proof boundaries and production not-proven boundaries.

## Decision Vector Fields

One Decision Vector represents one controlled lab routing decision. The contract should include these fields when available:

| Field | Meaning |
| --- | --- |
| `decisionId` or `labRunId` | Stable decision id or lab run id when exposed; otherwise mark as not exposed. |
| `selectedStrategy` | Strategy returned by the local lab comparison response. |
| `selectedBackend` | Backend/server selected by the controlled lab response. |
| `candidateBackends` | Candidate backend list visible in the local lab request or response. |
| `selectedCandidateVector` | Candidate Decision Vector for the selected backend/server. |
| `nonSelectedCandidateVectors` | Candidate Decision Vectors for visible non-selected backends. |
| `visibleCandidateSignals` | Health, latency, load, connection pressure, capacity, weight, error, queue, and network-awareness fields when exposed. |
| `knownSignals` | Signals visible in the local lab request or same-origin comparison response. |
| `unknownSignals` | Missing, unavailable, or unexposed fields; exact scoring and hidden internals must stay unknown unless the API exposes them. |
| `degradationFallbackIndicators` | Unhealthy, overloaded, empty, unavailable, fallback, or recovery markers visible in the controlled lab evidence. |
| `reasonCategories` | Reviewer-facing categories such as health, latency, load pressure, capacity/weight, degradation, recovery, or unknown. |
| `selectedVsAlternativeNotes` | Notes explaining why the selected backend appears favored and why alternatives appear weakened when visible data supports it. |
| `exactScoringAvailability` | `notExposed` unless the API explicitly returns exact scoring. |
| `factorContributionAvailability` | Exposed only when the local lab response returns current calculator contribution fields; otherwise mark as unavailable. |
| `replayReadiness` | Contract readiness for future replay; replay execution remains future/not implemented until built. |
| `labProofBoundary` | Controlled lab evidence, local reproducibility, same-origin local API responses, and browser-local interpretation. |
| `productionNotProvenBoundary` | No production traffic proof, production telemetry proof, production monitoring proof, production certification, live-cloud proof, real-tenant proof, SLA/SLO proof, registry publication, container signing, governance application, or exact production scoring proof. |

## Candidate Decision Vector

A Candidate Decision Vector represents one backend/server in the controlled lab decision. It should include these fields when visible:

| Field | Meaning |
| --- | --- |
| `candidateId` or `candidateName` | Backend/server id from the controlled lab payload or response. |
| `selected` | `true` for the selected candidate, `false` for non-selected candidates. |
| `healthState` | Visible healthy/unhealthy/degraded state. |
| `latencySignal` | Average, p95, p99, or related latency fields when exposed. |
| `loadOrConnectionPressureSignal` | In-flight request count, active connection count, queue depth, or pressure fields when exposed. |
| `capacityOrWeightSignal` | Configured capacity, estimated concurrency limit, weight, or related fields when exposed. |
| `degradationWarning` | Unhealthy, overloaded, draining, empty, unavailable, or fallback warning when visible. |
| `visibleSupportSignals` | Signals that visibly support the candidate. |
| `visibleCautionSignals` | Signals that visibly weaken or caution against the candidate. |
| `unknownSignals` | Candidate fields not exposed by the controlled lab response. |
| `selectionExplanation` | Why the candidate was selected or why it was not selected when visible data supports that explanation. |
| `fallbackExplanation` | Text such as `Candidate reason is unknown from visible data` when non-selection cannot be explained without hidden scoring. |

Candidate vectors must not infer hidden routing internals. If visible signals do not explain a non-selected candidate, the vector should mark the reason as unknown and send reviewers to the investigation playbook.

## Factor Contribution Placeholder Contract

Factor Contribution is a future extension unless the API explicitly exposes contribution data. The read-only
`/api/routing/compare` response can now expose current `ServerScoreCalculator` contribution entries under
`results[].decisionVector.candidateSummaries[].factorContributions`; fields beyond those current calculator
components remain unavailable unless a later sprint implements them. The contract shape is:

| Field | Meaning |
| --- | --- |
| `factorName` | Health, latency, load pressure, capacity, weight, error, queue, topology, or another exposed factor. |
| `rawValue` | Raw value from the local lab response when available. |
| `normalizedValue` | Normalized value when implemented and exposed. |
| `direction` | `supports`, `weakens`, `neutral`, or `unknown`. |
| `contributionValue` | Future/not implemented unless contribution values are exposed. |
| `weight` | Future/not implemented unless factor weights are exposed. |
| `confidenceNote` | Reviewer-facing confidence note for the factor. |
| `explanationText` | Human-readable local lab interpretation. |

The placeholder must keep unavailable fields unavailable until real fields exist. Exact production scoring is not claimed, and hidden scoring must not be inferred.

## From Decision Vector to Factor Contributions

Factor contributions are the next explainability layer under the Decision Vector. The current
`ServerScoreCalculator` factor contribution contract is additive and explains existing scoring
components such as p95 latency, p99 latency, average latency, in-flight pressure, queue pressure,
recent error rate, network-awareness risk signals, health penalty, and capacity-basis interpretation.

This contract is deliberately narrow:

- It explains current calculator components; it does not retune weights.
- It preserves existing score values and routing selection behavior.
- It marks visible state fields that do not contribute to this calculator, such as server weight, instead of inventing hidden weighting.
- It keeps hidden routing internals and production scoring unavailable unless explicitly exposed.
- It does not claim production scoring proof, production telemetry proof, production monitoring proof, or production certification.
- It does not implement decision replay, what-if experiments, or structured decision logging.
- Future cockpit rendering can consume factor contributions once an API or Decision Vector payload exposes them.

The contribution contract makes future factor analysis safer by tying every explanation entry to
existing calculator logic, an exact local-lab contribution value when available, an exactness marker,
and a boundary note. Unknown factors stay unknown instead of being inferred.

Candidate vectors can later attach contribution entries like this when the Decision Vector payload
exposes them. This is a contract shape and documentation example, not a new runtime API field:

```json
{
  "candidateId": "edge-alpha",
  "selected": true,
  "factorContributions": [
    {
      "factorName": "p95LatencyMillis",
      "rawValueDescription": "p95LatencyMillis=60.000000",
      "weightDescription": "P95_WEIGHT=0.450000",
      "direction": "WEAKENS_SELECTION",
      "contributionValue": 27.0,
      "exactness": "EXACT_FROM_CALCULATOR",
      "explanationText": "p95 latency contribution = p95LatencyMillis * P95_WEIGHT.",
      "boundaryNote": "Exact for the current local calculator component only; not production scoring proof."
    },
    {
      "factorName": "hiddenRoutingInternals",
      "rawValueDescription": "not exposed by this calculator contract",
      "direction": "UNKNOWN",
      "contributionValue": "notExposed",
      "exactness": "NOT_EXPOSED",
      "explanationText": "Hidden routing internals are not inferred.",
      "boundaryNote": "Exact production scoring is not claimed."
    }
  ]
}
```

## Candidate Factor Contribution Integration

Candidate factor contribution integration connects the `ServerScoreCalculator` contribution contract to
Candidate Decision Vectors without changing scoring behavior, strategy weights, or selected backend outcomes.
The additive `CandidateFactorContributionSummary` view can organize:

- Candidate backend/server id.
- Selected true/false state.
- Known visible state-vector signals.
- Unknown or unexposed candidate signals.
- Existing `ScoreFactorContribution` entries from `ServerScoreCalculator`.
- Selected-vs-alternative explanation notes.
- Exactness boundary for current calculator internals.
- Controlled lab proof boundary.
- Production not-proven boundary.

This integration lets selected and non-selected candidates carry the same contribution summary shape.
Selected-vs-alternative reasoning should use only visible/exposed contribution data, returned reason
text, and controlled lab signals. Unknown or unavailable candidate signals remain explicit investigation
items, and hidden scoring must not be invented.

The read-only `/api/routing/compare` response can expose candidate contribution summaries through
`results[].decisionVector` without changing scoring behavior, strategy weights, selected backend outcomes,
or existing response fields. This does not implement decision replay, what-if execution, strategy plugin
explainability, structured decision logging, production telemetry, production monitoring, or production
scoring proof.

Representative candidate vector attachment shape:

```json
{
  "candidateId": "edge-alpha",
  "selected": true,
  "knownVisibleSignals": [
    "healthState=true",
    "p95LatencyMillis=60.000000",
    "inFlightRequestCount=20"
  ],
  "unknownOrUnexposedSignals": [
    "hidden routing internals not exposed",
    "exact production scoring not exposed",
    "production telemetry not exposed"
  ],
  "factorContributionSummary": {
    "factorNames": ["p95LatencyMillis", "recentErrorRate", "healthPenalty"],
    "exactnessBoundary": "current calculator components only; hidden scoring is not inferred",
    "labProofBoundary": "controlled lab evidence only",
    "productionNotProvenBoundary": "no production scoring proof, telemetry proof, monitoring proof, or certification"
  }
}
```

## Read-only Decision Vector Exposure

`POST /api/routing/compare` exposes the Decision Vector through the additive
`results[].decisionVector` field for successful controlled lab routing results. This is the local lab
response path the Enterprise Lab Cockpit can consume; it is read-only and preserves existing
`requestedStrategies`, `candidateCount`, `timestamp`, result status, selected backend, reason, candidate
list, and score fields.

The read-only field includes:

- `selectedStrategy` and `selectedBackend` for the strategy result.
- `candidateSummaries` for selected and non-selected candidates.
- `selectedCandidateVector` and `nonSelectedCandidateVectors`.
- `knownVisibleSignals` and `unknownOrUnexposedSignals`.
- Current calculator `factorContributions` where the contract exposes them.
- Exactness, lab proof, and production not-proven boundaries.
- Replay, what-if, and structured logging readiness marked future/not implemented.

The exposure is additive controlled lab explainability only. It does not change routing selection,
score calculation, strategy weights, route/proxy behavior, or existing API response fields.
It does not claim production telemetry, production monitoring, production certification, exact production scoring,
completed replay, or completed what-if experiments.

Example response snippet:

```json
{
  "results": [
    {
      "strategyId": "TAIL_LATENCY_POWER_OF_TWO",
      "status": "SUCCESS",
      "chosenServerId": "edge-alpha",
      "decisionVector": {
        "readOnly": true,
        "localLabResponsePath": "/api/routing/compare",
        "decisionIdOrLabRunId": "not exposed by this read-only local lab response",
        "selectedStrategy": "TAIL_LATENCY_POWER_OF_TWO",
        "selectedBackend": "edge-alpha",
        "candidateCount": 3,
        "candidateSummaries": [
          {
            "candidateId": "edge-alpha",
            "selected": true,
            "knownVisibleSignals": ["healthState=true", "p95LatencyMillis=40.000000"],
            "unknownOrUnexposedSignals": [
              "hidden routing internals not exposed",
              "exact production scoring not exposed",
              "production telemetry not exposed"
            ],
            "factorContributions": [
              {
                "factorName": "p95LatencyMillis",
                "direction": "WEAKENS_SELECTION",
                "contributionValue": 18.0,
                "exactness": "EXACT_FROM_CALCULATOR",
                "boundaryNote": "Tail latency is an exact current calculator input, not production telemetry proof."
              },
              {
                "factorName": "hiddenRoutingInternals",
                "direction": "UNKNOWN",
                "contributionValue": null,
                "exactness": "NOT_EXPOSED",
                "boundaryNote": "Exact production scoring is not claimed; this contract explains current local calculator components only."
              }
            ]
          }
        ],
        "factorContributionAvailability": "exposed for current ServerScoreCalculator components through read-only controlled lab response data; hidden scoring is not inferred and exact production scoring is not claimed.",
        "replayReadiness": "future/not implemented; read-only Decision Vector exposure does not execute replay.",
        "whatIfReadiness": "future/not implemented; read-only Decision Vector exposure does not execute what-if experiments."
      }
    }
  ]
}
```

## How It Answers Why This Backend

The Decision Vector answers "why this backend?" by showing:

- Which controlled lab scenario was used.
- Which strategy selected the backend.
- Which backend/server was selected.
- Which visible candidate signals support the selected backend.
- Which visible candidate signals caution against non-selected candidates.
- Which fields are unknown or unexposed.
- Which explanation notes are supported by visible data.
- Which explanation gaps remain investigation items.

If a candidate appears weakened by unhealthy state, higher visible latency, higher visible load/connection pressure, or lower capacity/weight where those fields are exposed, the vector can record that visible signal comparison. If the local lab response does not expose enough information, the vector must say that the candidate reason is unknown from visible data.

## Replay, What-If, Logging, and Plugin Roadmap

The Decision Vector is a foundation for future work. These roadmap items are future/not implemented unless a later sprint adds and verifies them:

- Factor contribution analysis: future/not implemented.
- Decision replay: future/not implemented.
- What-if experiments: future/not implemented.
- Structured decision logging: future/not implemented.
- Strategy plugin explainability: future/not implemented.
- Rack, zone, and topology modeling: future/not implemented.
- Correlated failure modeling: future/not implemented.
- Live interrogation mode: future/not implemented.

Decision Vector data should make those paths safer by keeping known signals, unknown signals, exact scoring availability, and production proof boundaries explicit.

## Static Example Decision Vector Payload

This example is static documentation, not an implemented runtime endpoint or server-side export.

```json
{
  "decisionId": "not-exposed-in-current-local-lab-response",
  "labRunId": "browser-local-controlled-lab-run",
  "selectedStrategy": "TAIL_LATENCY_POWER_OF_TWO",
  "selectedBackend": "edge-alpha",
  "candidateBackends": ["edge-alpha", "edge-beta", "edge-drain"],
  "selectedCandidateVector": {
    "candidateId": "edge-alpha",
    "selected": true,
    "healthState": "healthy",
    "latencySignal": {
      "p95LatencyMillis": 42,
      "known": true
    },
    "loadOrConnectionPressureSignal": {
      "inFlightRequestCount": 12,
      "queueDepth": 1,
      "known": true
    },
    "capacityOrWeightSignal": {
      "configuredCapacity": 100,
      "weight": 5,
      "known": true
    },
    "visibleSupportSignals": ["healthy state", "lower visible p95 latency", "lower visible queue depth"],
    "visibleCautionSignals": [],
    "unknownSignals": ["exact scoring", "hidden routing internals", "production telemetry"],
    "selectionExplanation": "Visible controlled lab signals support the selected backend.",
    "fallbackExplanation": "Not needed for selected candidate."
  },
  "nonSelectedCandidateVectors": [
    {
      "candidateId": "edge-beta",
      "selected": false,
      "healthState": "healthy",
      "latencySignal": {
        "p95LatencyMillis": 52,
        "known": true
      },
      "loadOrConnectionPressureSignal": {
        "inFlightRequestCount": 28,
        "queueDepth": 3,
        "known": true
      },
      "capacityOrWeightSignal": {
        "configuredCapacity": 100,
        "weight": 4,
        "known": true
      },
      "visibleSupportSignals": ["healthy state"],
      "visibleCautionSignals": ["higher visible p95 latency", "higher visible load/connection pressure"],
      "unknownSignals": ["exact scoring", "hidden routing internals"],
      "selectionExplanation": "Visible signal comparison suggests the candidate was weaker than the selected backend.",
      "fallbackExplanation": "Use returned reason text if visible signals are insufficient."
    },
    {
      "candidateId": "edge-drain",
      "selected": false,
      "healthState": "unhealthy",
      "latencySignal": {
        "p95LatencyMillis": 20,
        "known": true
      },
      "loadOrConnectionPressureSignal": {
        "inFlightRequestCount": 1,
        "queueDepth": 0,
        "known": true
      },
      "capacityOrWeightSignal": {
        "configuredCapacity": 100,
        "weight": 5,
        "known": true
      },
      "visibleSupportSignals": ["low visible latency"],
      "visibleCautionSignals": ["unhealthy state"],
      "unknownSignals": ["exact scoring", "hidden routing internals"],
      "selectionExplanation": "Visible unhealthy state cautions against selection.",
      "fallbackExplanation": "Candidate reason is unknown from visible data if health state is not enough to explain non-selection."
    }
  ],
  "exactScoringAvailability": "notExposedUnlessReturnedByApi",
  "factorContributionAvailability": "exposedForCurrentCalculatorComponentsWhenReturnedByApi",
  "factorContributionPlaceholder": [
    {
      "factorName": "p95LatencyMillis",
      "rawValue": 42,
      "normalizedValue": "futureNotImplemented",
      "direction": "supports",
      "contributionValue": "futureNotImplemented",
      "weight": "futureNotImplemented",
      "confidenceNote": "Reviewer-facing lab interpretation only.",
      "explanationText": "Lower visible latency supports the selected backend in the controlled lab response."
    }
  ],
  "replayReadiness": "plannedFutureContract; replay execution is not implemented",
  "whatIfReadiness": "plannedFutureContract; what-if execution is not implemented",
  "structuredDecisionLoggingReadiness": "plannedFutureContract; structured logging is not implemented",
  "productionNotProvenBoundary": "No production traffic proof, production telemetry proof, production monitoring proof, production certification, live-cloud proof, real-tenant proof, SLA/SLO proof, registry publication, container signing, governance application, or exact production scoring proof."
}
```

## Production Not-Proven Boundaries

The Decision Vector contract is controlled lab explainability. It does not change live routing behavior, does not add production telemetry, does not add production monitoring, does not generate server-side files, and does not claim production certification.

It does not prove production traffic behavior, live-cloud behavior, real-tenant behavior, SLA/SLO achievement, registry publication, container signing, governance application, exact production scoring, completed factor contribution analysis, completed replay, completed what-if experiments, completed strategy plugin explainability, or production readiness.
