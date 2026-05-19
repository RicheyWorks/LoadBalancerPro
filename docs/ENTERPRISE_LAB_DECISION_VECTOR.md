# Enterprise Lab Decision Vector Contract

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

The Enterprise Lab Decision Vector is the structured explanation object for one controlled lab routing decision. It turns a final selected-backend outcome into replayable, explainable, and testable local lab evidence without claiming production telemetry, production monitoring, exact production scoring, or production certification.

## Why the Lab Needs It

The cockpit already explains visible outcomes: selected strategy, selected backend/server, candidate signals, known versus unknown signals, and selected-vs-alternative notes. A Decision Vector gives those explanations a contract so the current read-only dominant-factor lane, selected-vs-closest-alternative decision delta lane, Decision Replay Snapshot lane, Decision Replay Reconstruction Trace lane, Decision Replay Capsule lane, Decision Replay Readiness Checklist lane, Decision Replay Evidence Source Map lane, Decision Replay Evidence Boundary Summary lane, Decision Replay Evidence Field Inventory lane, Decision Evidence Null-Safety Summary lane, Decision Evidence Status Rollup lane, and later separately scoped lab planning work can build without inventing hidden scoring.

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
- Dominant factor analysis derived from returned factor contributions.
- Decision delta analysis comparing the selected candidate with the closest scored alternative.
- Decision Replay Snapshot metadata and deterministic local fingerprint for already-returned lab evidence.
- Decision Replay Reconstruction Trace steps and deterministic local trace fingerprint for already-returned lab evidence.
- Decision Replay Capsule canonical evidence packaging and deterministic local capsule fingerprint for already-returned lab evidence.
- Decision Replay Readiness Checklist status over already-built evidence lanes.
- Decision Replay Evidence Source Map relationships over already-built evidence lanes.
- Decision Replay Evidence Boundary Summary metadata over already-built boundary fields and statuses.
- Decision Replay Evidence Field Inventory metadata over already-built evidence field groups.
- Decision Evidence Null-Safety Summary metadata over already-built null, missing, unavailable, and no-healthy/failure-path evidence.
- Decision Evidence Status Rollup metadata over already-built evidence lane statuses, selected-candidate presence, candidate count, and boundary state.
- Replay readiness and later separately scoped replay planning gaps.
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
| `dominantFactorAnalysis` | Additive read-only summary of largest support, penalty/risk, and absolute-impact factors derived only from returned contribution data. |
| `decisionDeltaAnalysis` | Additive read-only selected-vs-closest-alternative score gap and factor contribution delta summary derived only from returned scores and contribution data. |
| `decisionReplaySnapshot` | Additive read-only snapshot of stable compare evidence and deterministic local fingerprint derived only from already-built response fields. |
| `decisionReplayReconstructionTrace` | Additive read-only reconstruction evidence steps and deterministic local trace fingerprint derived only from already-built response fields. |
| `decisionReplayCapsule` | Additive read-only canonical evidence package and deterministic local capsule fingerprint derived only from already-built response fields and prior lab analysis objects. |
| `decisionReplayReadinessChecklist` | Additive read-only checklist of lab replay-readiness evidence statuses derived only from already-built Decision Vector, dominant factor, delta, snapshot, trace, and capsule fields. |
| `decisionReplayEvidenceSourceMap` | Additive read-only source map showing which already-built compare evidence fields support replay/readiness artifacts; it links existing fingerprints only when already available and does not generate a new fingerprint. |
| `decisionReplayEvidenceBoundarySummary` | Additive read-only boundary summary showing existing lab-only, read-only, and not-proven boundary fields across already-built compare evidence; it does not generate a new fingerprint. |
| `decisionReplayEvidenceFieldInventory` | Additive read-only field inventory showing which already-built compare evidence field groups are observed or unavailable; it uses explicit DTO accessors and does not generate a new fingerprint. |
| `decisionReplayEvidenceNullSafetySummary` | Additive read-only null-safety summary showing null, missing, unavailable, and no-healthy/failure-path safety across already-built compare evidence; it uses explicit DTO accessors, does not use reflection, and does not generate a new fingerprint. |
| `decisionReplayEvidenceStatusRollup` | Additive read-only status rollup showing existing evidence lane status, selected-candidate presence, candidate count, and boundary state; it is metadata-only, does not use reflection, and does not generate a new fingerprint. |
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

## Dominant Factor Analysis

Dominant Factor Analysis is the first read-only interpretation layer on top of returned Decision Vector
contribution data. It does not duplicate scoring logic or recompute scores from raw server fields.
Instead, it consumes existing `ScoreFactorContributionResponse` entries and identifies, per candidate:

- The largest support-direction contributor when present.
- The largest penalty/risk contributor when present.
- The factor with the largest absolute numeric impact when present.
- A short deterministic explanation based only on returned factor names, directions, and contribution values.

The selected-decision summary is based only on the selected candidate's contribution list. It does not
borrow factors from non-selected candidates, infer hidden routing internals, or claim production proof.
When contributions are absent or empty, the analysis returns an unknown state rather than inventing data.
Ties are resolved by stable factor-name ordering.

Reviewers can use this lane to see which returned factor most influenced each candidate's local lab score
and why one candidate looked better or worse than another under the visible calculator contribution data.
It does not change routing behavior, scoring math, strategy weights, server selection logic, proxy behavior,
or existing API response fields. See [`ENTERPRISE_LAB_DOMINANT_FACTOR_ANALYSIS.md`](ENTERPRISE_LAB_DOMINANT_FACTOR_ANALYSIS.md)
for the focused reviewer contract and safety boundaries.

## Selected-vs-Closest-Alternative Decision Delta Analysis

Decision Delta Analysis is the next read-only interpretation layer on top of returned Decision Vector
contribution data and result score data. It does not duplicate scoring logic or recompute scores from
raw server fields. Instead, it consumes existing final scores and `ScoreFactorContributionResponse`
entries and identifies, per routing comparison result:

- the selected candidate;
- the closest scored non-selected alternative by smallest absolute final score gap;
- stable candidate/server id tie handling when alternatives have equal score gaps;
- shared finite factor contribution deltas between the selected candidate and closest alternative;
- the largest absolute factor delta when present;
- the selected-minus-alternative final score gap when present.

The comparison uses only the selected candidate and the closest alternative candidate. It does not borrow
factor data from farther alternatives, infer hidden routing internals, fill missing factors from raw server
fields, or claim production proof. When scores or candidate vectors are unavailable, it returns `UNKNOWN`.
When factor data is empty, missing on one side, null, partial, or non-finite, it returns `PARTIAL` and omits
unsafe deltas instead of inventing zero values.

Reviewers can use this lane to inspect which returned factor contribution differences separated the selected
candidate from the closest scored alternative under the visible lab response. Score sign semantics follow the
existing score output and are not reinterpreted. It does not change routing behavior, scoring math, strategy
weights, server selection logic, proxy behavior, or existing API response fields. See
[`ENTERPRISE_LAB_DECISION_DELTA_ANALYSIS.md`](ENTERPRISE_LAB_DECISION_DELTA_ANALYSIS.md) for the focused
reviewer contract and safety boundaries.

## Decision Replay Snapshot

Decision Replay Snapshot is the read-only replay-readiness layer on top of the already-built routing
comparison evidence. It does not execute replay, perform what-if mutation, persist audit logs, or rerun
scoring. Instead, it summarizes stable fields already returned by the compare response and analysis lanes:

- selected candidate id when available;
- deterministically ordered candidate ids considered;
- candidate count;
- strategy id;
- Decision Vector, Dominant Factor Analysis, and Decision Delta Analysis statuses;
- closest alternative id, final score gap, and largest delta factor when returned by Decision Delta Analysis;
- deterministic local snapshot fingerprint over stable fields only.

The fingerprint must not include timestamps, random ids, hostnames, environment variables, file paths,
secrets, local usernames, or machine-specific data. It is a deterministic local comparison aid, not a
cryptographic proof of production behavior, signing proof, registry publication proof, or audit-log
persistence feature.

When selected candidate evidence, candidate ids, or Decision Vector evidence is missing, the snapshot
returns `UNKNOWN`. When optional analysis data is absent, partial, or non-finite, the snapshot returns
`PARTIAL` and keeps missing values unknown instead of inventing them. See
[`ENTERPRISE_LAB_DECISION_REPLAY_SNAPSHOT.md`](ENTERPRISE_LAB_DECISION_REPLAY_SNAPSHOT.md) for the focused
reviewer contract and safety boundaries.

## Decision Replay Reconstruction Trace

Decision Replay Reconstruction Trace is the read-only reconstruction evidence layer on top of the already-built
routing comparison evidence. It does not execute replay, perform what-if mutation, persist traces or audit logs,
or rerun scoring. Instead, it lists deterministic reconstruction steps and field paths that show whether the
current compare result contains enough stable lab evidence to reconstruct the decision explanation later:

- candidate set observed from returned candidate ids, scores, Decision Vector, or replay snapshot fields;
- selected candidate observed from returned selected fields;
- candidate final scores observed only when already returned as finite values;
- Decision Vector evidence and candidate factor contribution evidence;
- Dominant Factor Analysis and Decision Delta Analysis status;
- closest alternative id, final score gap, and largest delta factor when already returned by Decision Delta Analysis;
- linked Decision Replay Snapshot status and snapshot fingerprint;
- deterministic local trace fingerprint over stable trace fields only.

The trace fingerprint must not include timestamps, random ids, hostnames, environment variables, file paths,
secrets, local usernames, machine-specific data, or network-specific data. It is a deterministic local comparison
aid, not a cryptographic proof of production behavior, signing proof, registry publication proof, audit-log
persistence feature, guaranteed replay proof, or production traffic validation.

When selected candidate evidence, candidate ids, and Decision Vector evidence are missing, the trace returns
`UNKNOWN`. When useful evidence is present but some reconstruction steps are missing, partial, or non-finite,
the trace returns `PARTIAL` and keeps missing values unknown instead of inventing them. See
[`ENTERPRISE_LAB_DECISION_REPLAY_RECONSTRUCTION_TRACE.md`](ENTERPRISE_LAB_DECISION_REPLAY_RECONSTRUCTION_TRACE.md)
for the focused reviewer contract and safety boundaries.

## Decision Replay Capsule

Decision Replay Capsule is the read-only canonical evidence packaging layer on top of the already-built routing
comparison evidence. It packages stable candidate evidence, factor evidence, linked replay snapshot and
reconstruction trace fingerprints, and a deterministic capsule fingerprint from already-built compare evidence only.
It does not execute replay, perform what-if mutation, persist capsules or audit logs, export/download/share capsules,
rerun routing, recompute scores, or retune weights.

The capsule includes:

- selected candidate id and deterministically ordered candidate ids when already returned;
- candidate final scores only when already returned as finite values;
- compact candidate evidence with observed factor names, contribution counts, and dominant factor names when already available;
- compact factor evidence for selected-vs-closest-alternative contribution differences when already available;
- linked Decision Replay Snapshot and Decision Replay Reconstruction Trace fingerprints;
- deterministic local capsule fingerprint over stable capsule fields only.

The capsule fingerprint must not include timestamps, random ids, hostnames, environment variables, file paths,
secrets, local usernames, machine-specific data, or network-specific data. It is a deterministic local comparison
aid, not a cryptographic proof of production behavior, signing proof, registry publication proof, audit-log
persistence feature, guaranteed replay proof, exact production scoring proof, or production traffic validation.

When selected candidate evidence, candidate ids, and Decision Vector evidence are missing, the capsule returns
`UNKNOWN`. When useful evidence is present but candidate evidence, factor evidence, linked fingerprints, score gaps,
or prior analysis statuses are incomplete, partial, or non-finite, the capsule returns `PARTIAL` and keeps missing
values unknown instead of inventing them. See
[`ENTERPRISE_LAB_DECISION_REPLAY_CAPSULE.md`](ENTERPRISE_LAB_DECISION_REPLAY_CAPSULE.md) for the focused reviewer
contract and safety boundaries.

## Decision Replay Readiness Checklist

Decision Replay Readiness Checklist is the read-only lab evidence readiness layer on top of the already-built
routing comparison evidence. It summarizes stable checklist items across Decision Vector, Dominant Factor Analysis,
Decision Delta Analysis, Decision Replay Snapshot, Decision Replay Reconstruction Trace, and Decision Replay Capsule
fields. It does not execute replay, perform what-if mutation, persist checklist state or audit logs, export/download/share
checklist data, rerun routing, recompute scores, or retune weights.

The checklist includes:

- deterministic item ids for Decision Vector, dominant factor, decision delta, replay snapshot, reconstruction trace,
  replay capsule, candidate, factor, and read-only boundary evidence;
- source evidence statuses copied from already-built lanes;
- linked snapshot, trace, and capsule fingerprints only when already available;
- available, partial, and unknown item counts;
- missing evidence reasons that keep absent evidence unknown instead of inventing values.

When selected candidate evidence, candidate ids, and Decision Vector evidence are missing, the checklist returns
`UNKNOWN`. When useful evidence is present but item status, final scores, factor contributions, or linked fingerprints
are incomplete, partial, or unavailable, the checklist returns `PARTIAL`. See
[`ENTERPRISE_LAB_DECISION_REPLAY_READINESS_CHECKLIST.md`](ENTERPRISE_LAB_DECISION_REPLAY_READINESS_CHECKLIST.md)
for the focused reviewer contract and safety boundaries.

## Decision Replay Evidence Source Map

Decision Replay Evidence Source Map is the read-only lab evidence relationship layer on top of the already-built
routing comparison evidence. It explains which source fields in Decision Vector, Dominant Factor Analysis,
Decision Delta Analysis, Decision Replay Snapshot, Decision Replay Reconstruction Trace, Decision Replay Capsule, and
Decision Replay Readiness Checklist support downstream replay/readiness artifacts. It does not execute replay,
perform what-if mutation, persist source-map data or audit logs, export/download/share source-map data, generate a
new fingerprint, rerun routing, recompute scores, or retune weights.

The source map includes:

- deterministic source ids for Decision Vector, dominant factor, decision delta, replay snapshot, reconstruction trace,
  replay capsule, replay readiness checklist, and read-only boundary evidence;
- source field paths and downstream evidence field paths for already-built compare response objects;
- normalized source statuses copied from already-built lanes;
- linked snapshot, trace, and capsule fingerprints only when already available and the source status is not `UNKNOWN`;
- missing evidence handling that keeps absent evidence unknown instead of inventing values.

When selected candidate evidence, candidate ids, and source evidence lane statuses are missing, the source map returns
`UNKNOWN`. When useful evidence exists but one or more source entries or linked fingerprints are incomplete, partial,
or unavailable, the source map returns `PARTIAL`. See
[`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_SOURCE_MAP.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_SOURCE_MAP.md)
for the focused reviewer contract and safety boundaries.

## Decision Replay Evidence Boundary Summary

Decision Replay Evidence Boundary Summary is the read-only lab boundary metadata layer on top of the already-built
routing comparison evidence. It summarizes which existing boundary fields and statuses state that the compare evidence
is lab-only, read-only, not replay execution, not what-if mutation, not persisted, not exported or shared, not a routing
behavior change, not score recomputation, not new fingerprint generation, and not production proof.

The boundary summary includes:

- deterministic boundary ids for lab-only, read-only, no replay execution, no what-if mutation, no persistence,
  no export/share/download, no routing behavior change, no score recomputation, no new fingerprint, and
  production not-proven boundaries;
- source field paths and supporting evidence field paths for already-built compare response DTOs;
- normalized source statuses copied from already-built lanes;
- safe `AVAILABLE`, `PARTIAL`, or `UNKNOWN` status handling;
- missing evidence handling that keeps absent selected candidates, candidate sets, alternatives, score gaps, and
  factor values unknown instead of inventing values.

When selected candidate evidence, candidate ids, and source evidence lane statuses are missing, the boundary summary
returns `UNKNOWN`. It does not execute replay, perform what-if mutation, persist boundary-summary data or audit logs,
export/download/share boundary-summary data, generate a new fingerprint, rerun routing, recompute scores, infer hidden
scoring, or retune weights. See
[`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md)
for the focused reviewer contract and safety boundaries.

## Decision Replay Evidence Field Inventory

Decision Replay Evidence Field Inventory is the read-only lab field inventory layer on top of the already-built
routing comparison evidence. It inventories which existing evidence field groups are observed or unavailable across
Decision Vector, Dominant Factor Analysis, Decision Delta Analysis, Decision Replay Snapshot, Decision Replay
Reconstruction Trace, Decision Replay Capsule, Decision Replay Readiness Checklist, Decision Replay Evidence Source
Map, and Decision Replay Evidence Boundary Summary. It does not use reflection, derive from raw server input, execute
replay, perform what-if mutation, persist field-inventory data or audit logs, export/download/share field-inventory
data, generate a new fingerprint, rerun routing, recompute scores, infer hidden scoring, or retune weights.

The field inventory includes:

- deterministic inventory ids for evidence field groups, linked fingerprint fields, read-only boundary fields, and
  production not-proven boundary fields;
- observed field paths and missing or unavailable field paths for already-built compare response DTOs;
- normalized source statuses copied from already-built lanes;
- available, partial, and unknown inventory group counts;
- missing evidence handling that keeps absent selected candidates, candidate sets, alternatives, score gaps, largest
  delta factors, fingerprints, and factor values unknown instead of inventing values.

When selected candidate evidence, candidate ids, and source evidence lane statuses are missing, the field inventory
returns `UNKNOWN`. It does not execute replay, perform what-if mutation, persist field-inventory data or audit logs,
export/download/share field-inventory data, generate a new fingerprint, rerun routing, recompute scores, infer hidden
scoring, or retune weights. See
[`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY.md)
for the focused reviewer contract and safety boundaries.

## Decision Evidence Null-Safety Summary

Decision Evidence Null-Safety Summary is the read-only lab null-safety metadata layer on top of the already-built
routing comparison evidence. It summarizes null, missing, unavailable, and no-healthy/failure-path safety across
Decision Vector, Dominant Factor Analysis, Decision Delta Analysis, Decision Replay Snapshot, Decision Replay
Reconstruction Trace, Decision Replay Capsule, Decision Replay Readiness Checklist, Decision Replay Evidence Source
Map, Decision Replay Evidence Boundary Summary, and Decision Replay Evidence Field Inventory. It does not use
reflection, inspect raw request payloads, inspect raw server input, execute replay, perform what-if mutation, persist
null-safety data or audit logs, export/download/share null-safety data, generate a new fingerprint, rerun routing,
recompute scores, infer hidden scoring, or retune weights.

The null-safety summary includes:

- deterministic null-safety ids for selected-candidate, candidate-set, score-gap, closest-alternative,
  largest-delta-factor, linked-fingerprint, candidate-evidence, factor-evidence, field-inventory, no-healthy-path,
  boundary-text, and production-not-proven checks;
- checked field paths and unavailable field paths from already-built compare response DTOs;
- normalized source statuses copied from already-built lanes;
- available, partial, and unknown null-safety item counts;
- missing evidence handling that keeps absent selected candidates, candidate sets, alternatives, score gaps, largest
  delta factors, fingerprints, replay claims, certification claims, guaranteed replay claims, and factor values
  unknown instead of inventing values.

When selected candidate evidence, candidate ids, source evidence lane statuses, and field groups are missing, the
null-safety summary returns `UNKNOWN` or `PARTIAL`. It does not execute replay, perform what-if mutation, persist
null-safety data or audit logs, export/download/share null-safety data, generate a new fingerprint, rerun routing,
recompute scores, infer hidden scoring, retune weights, or claim production behavior; it is not production
certification and not guaranteed replay. See
[`ENTERPRISE_LAB_DECISION_EVIDENCE_NULL_SAFETY_SUMMARY.md`](ENTERPRISE_LAB_DECISION_EVIDENCE_NULL_SAFETY_SUMMARY.md)
for the focused reviewer contract and safety boundaries.

## Decision Evidence Status Rollup

Decision Evidence Status Rollup is the read-only lab status metadata layer on top of the already-built routing
comparison evidence. It summarizes evidence lane statuses, selected-candidate presence, candidate count, and boundary
state across Decision Vector, Dominant Factor Analysis, Decision Delta Analysis, Decision Replay Snapshot, Decision
Replay Reconstruction Trace, Decision Replay Capsule, Decision Replay Readiness Checklist, Decision Replay Evidence
Source Map, Decision Replay Evidence Boundary Summary, Decision Replay Evidence Field Inventory, and Decision Evidence
Null-Safety Summary. It does not use reflection, inspect raw request payloads, inspect raw server input, execute replay,
perform what-if mutation, persist status-rollup data or audit logs, export/download/share status-rollup data, generate a
new fingerprint, rerun routing, recompute scores, infer hidden scoring, or retune weights.

The status rollup includes:

- deterministic status ids for the existing evidence lanes plus read-only and production-not-proven boundary status;
- selected-candidate presence and candidate count copied only from already-built compare response DTOs;
- normalized `AVAILABLE`, `PARTIAL`, and `UNKNOWN` status counts;
- boundary presence based only on already-built read-only and not-proven boundary text;
- missing evidence handling that keeps absent selected candidates, candidate sets, fingerprints, replay claims,
  certification claims, quality-ranking claims, and guaranteed replay claims unknown instead of inventing values.

When selected candidate evidence, candidate ids, source evidence lane statuses, and boundary evidence are missing, the
status rollup returns `UNKNOWN` or `PARTIAL`. It does not execute replay, perform what-if mutation, persist status-rollup
data or audit logs, export/download/share status-rollup data, generate a new fingerprint, rerun routing, recompute
scores, infer hidden scoring, retune weights, or claim production behavior; it is not production certification and not
guaranteed replay. See
[`ENTERPRISE_LAB_DECISION_EVIDENCE_STATUS_ROLLUP.md`](ENTERPRISE_LAB_DECISION_EVIDENCE_STATUS_ROLLUP.md)
for the focused reviewer contract and safety boundaries.

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
- Result-level `dominantFactorAnalysis` derived from those returned contribution entries.
- Result-level `decisionDeltaAnalysis` derived from returned final scores and shared finite contribution entries.
- Result-level `decisionReplaySnapshot` derived from already-built compare evidence and stable analysis statuses.
- Result-level `decisionReplayReconstructionTrace` derived from already-built compare evidence, stable analysis statuses, and reconstruction steps.
- Result-level `decisionReplayCapsule` derived from already-built compare evidence and already-built analysis objects.
- Result-level `decisionReplayReadinessChecklist` derived from already-built evidence lane statuses and linked fingerprints.
- Result-level `decisionReplayEvidenceSourceMap` derived from already-built evidence lane statuses and source field relationships.
- Result-level `decisionReplayEvidenceBoundarySummary` derived from already-built boundary fields and source statuses.
- Result-level `decisionReplayEvidenceFieldInventory` derived from already-built evidence field groups and source statuses.
- Result-level `decisionReplayEvidenceNullSafetySummary` derived from already-built null, missing, unavailable, and no-healthy/failure-path evidence.
- Result-level `decisionReplayEvidenceStatusRollup` derived from already-built evidence lane statuses and boundary state.
- Exactness, lab proof, and production not-proven boundaries.
- Replay, what-if, and structured logging readiness marked future/not implemented.

The dominant factor field is exposed as `results[].dominantFactorAnalysis` and is derived after
`results[].decisionVector` exists.

The decision delta field is exposed as `results[].decisionDeltaAnalysis` and is derived after
`results[].decisionVector` and existing result score data are available.

The replay snapshot field is exposed as `results[].decisionReplaySnapshot` and is derived after
`results[].decisionVector`, `results[].dominantFactorAnalysis`, and `results[].decisionDeltaAnalysis`
are available.

The reconstruction trace field is exposed as `results[].decisionReplayReconstructionTrace` and is derived after
`results[].decisionVector`, `results[].dominantFactorAnalysis`, `results[].decisionDeltaAnalysis`, and
`results[].decisionReplaySnapshot` are available.

The replay capsule field is exposed as `results[].decisionReplayCapsule` and is derived after
`results[].decisionVector`, `results[].dominantFactorAnalysis`, `results[].decisionDeltaAnalysis`,
`results[].decisionReplaySnapshot`, and `results[].decisionReplayReconstructionTrace` are available.

The replay readiness checklist field is exposed as `results[].decisionReplayReadinessChecklist` and is derived after
`results[].decisionVector`, `results[].dominantFactorAnalysis`, `results[].decisionDeltaAnalysis`,
`results[].decisionReplaySnapshot`, `results[].decisionReplayReconstructionTrace`, and
`results[].decisionReplayCapsule` are available.

The evidence source map field is exposed as `results[].decisionReplayEvidenceSourceMap` and is derived after
`results[].decisionVector`, `results[].dominantFactorAnalysis`, `results[].decisionDeltaAnalysis`,
`results[].decisionReplaySnapshot`, `results[].decisionReplayReconstructionTrace`,
`results[].decisionReplayCapsule`, and `results[].decisionReplayReadinessChecklist` are available.

The evidence boundary summary field is exposed as `results[].decisionReplayEvidenceBoundarySummary` and is derived
after `results[].decisionReplayEvidenceSourceMap` and the already-built upstream evidence lanes are available.

The evidence field inventory field is exposed as `results[].decisionReplayEvidenceFieldInventory` and is derived
after `results[].decisionReplayEvidenceBoundarySummary` and the already-built upstream evidence lanes are available.

The evidence null-safety summary field is exposed as `results[].decisionReplayEvidenceNullSafetySummary` and is
derived after `results[].decisionReplayEvidenceFieldInventory` and the already-built upstream evidence lanes are
available.

The evidence status rollup field is exposed as `results[].decisionReplayEvidenceStatusRollup` and is derived after
`results[].decisionReplayEvidenceNullSafetySummary` and the already-built upstream evidence lanes are available.

The exposure is additive controlled lab explainability only. It does not change routing selection,
score calculation, strategy weights, route/proxy behavior, or existing API response fields.
It does not claim production telemetry, production monitoring, production certification, exact production scoring,
replay execution, or completed what-if experiments.

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
      },
      "dominantFactorAnalysis": {
        "readOnly": true,
        "source": "/api/routing/compare results[].decisionVector candidate factorContributions",
        "status": "AVAILABLE",
        "selectedDecisionAnalysis": {
          "candidateId": "edge-alpha",
          "selected": true,
          "available": true,
          "largestPenaltyContributor": {
            "factorName": "p99LatencyMillis",
            "direction": "WEAKENS_SELECTION",
            "contributionValue": 28.0,
            "absoluteImpact": 28.0
          },
          "largestAbsoluteImpact": {
            "factorName": "p99LatencyMillis",
            "direction": "WEAKENS_SELECTION",
            "contributionValue": 28.0,
            "absoluteImpact": 28.0
          },
          "explanation": "Candidate edge-alpha is selected; dominant factors are derived only from returned contribution data."
        },
        "boundaryNote": "Read-only lab explainability derived only from returned contribution data; routing behavior is unchanged."
      },
      "decisionDeltaAnalysis": {
        "readOnly": true,
        "source": "/api/routing/compare results[].scores and results[].decisionVector candidate factorContributions",
        "status": "PARTIAL",
        "comparison": {
          "selectedCandidateId": "edge-alpha",
          "closestAlternativeCandidateId": "edge-beta",
          "finalScoreGap": -20.0,
          "absoluteFinalScoreGap": 20.0,
          "comparedFactorCount": 15,
          "omittedFactorNames": ["hiddenRoutingInternals"]
        },
        "largestAbsoluteFactorDelta": {
          "factorName": "p99LatencyMillis",
          "selectedCandidateContribution": 28.0,
          "alternativeCandidateContribution": 33.6,
          "contributionDelta": -5.6,
          "absoluteDelta": 5.6
        },
        "boundaryNote": "Read-only lab explainability derived only from returned score and contribution data; routing behavior is unchanged."
      },
      "decisionReplaySnapshot": {
        "readOnly": true,
        "snapshotSchemaVersion": "decision-replay-snapshot/v1",
        "status": "PARTIAL",
        "snapshotFingerprint": "deterministic-local-hash",
        "selectedCandidateId": "edge-alpha",
        "candidateIdsConsidered": ["edge-alpha", "edge-beta", "edge-drain"],
        "candidateCount": 3,
        "strategyId": "TAIL_LATENCY_POWER_OF_TWO",
        "decisionVectorStatus": "AVAILABLE",
        "dominantFactorAnalysisStatus": "AVAILABLE",
        "decisionDeltaAnalysisStatus": "PARTIAL",
        "closestAlternativeCandidateId": "edge-beta",
        "finalScoreGap": -20.0,
        "largestDeltaFactorName": "p99LatencyMillis",
        "boundaryNote": "Read-only lab evidence derived only from already-built compare response data; no replay execution or what-if mutation is performed."
      },
      "decisionReplayReconstructionTrace": {
        "readOnly": true,
        "traceSchemaVersion": "decision-replay-reconstruction-trace/v1",
        "status": "PARTIAL",
        "traceFingerprint": "deterministic-local-trace-hash",
        "snapshotFingerprint": "deterministic-local-hash",
        "selectedCandidateId": "edge-alpha",
        "candidateIdsConsidered": ["edge-alpha", "edge-beta", "edge-drain"],
        "candidateFinalScores": {
          "edge-alpha": 50.0,
          "edge-beta": 70.0
        },
        "decisionVectorStatus": "AVAILABLE",
        "factorContributionStatus": "AVAILABLE",
        "dominantFactorAnalysisStatus": "AVAILABLE",
        "decisionDeltaAnalysisStatus": "PARTIAL",
        "decisionReplaySnapshotStatus": "PARTIAL",
        "closestAlternativeCandidateId": "edge-beta",
        "finalScoreGap": -20.0,
        "largestDeltaFactorName": "p99LatencyMillis",
        "reconstructionSteps": [
          {
            "stepId": "candidate-set-observed",
            "status": "AVAILABLE",
            "evidenceSourceFieldPath": "candidateServersConsidered, scores, decisionVector.candidateSummaries, decisionReplaySnapshot",
            "missingEvidenceReason": null
          },
          {
            "stepId": "replay-snapshot-fingerprint-observed",
            "status": "AVAILABLE",
            "evidenceSourceFieldPath": "decisionReplaySnapshot.snapshotFingerprint",
            "missingEvidenceReason": null
          }
        ],
        "boundaryNote": "Read-only lab evidence derived only from already-built compare response data; no replay execution, what-if mutation, or trace persistence is performed."
      },
      "decisionReplayCapsule": {
        "readOnly": true,
        "capsuleSchemaVersion": "decision-replay-capsule/v1",
        "status": "PARTIAL",
        "capsuleFingerprint": "deterministic-local-capsule-hash",
        "linkedReplaySnapshotFingerprint": "deterministic-local-hash",
        "linkedReconstructionTraceFingerprint": "deterministic-local-trace-hash",
        "selectedCandidateId": "edge-alpha",
        "candidateIdsConsidered": ["edge-alpha", "edge-beta", "edge-drain"],
        "candidateCount": 3,
        "closestAlternativeCandidateId": "edge-beta",
        "finalScoreGap": -20.0,
        "largestDeltaFactorName": "p99LatencyMillis",
        "reconstructionStepIds": ["candidate-set-observed", "replay-snapshot-fingerprint-observed"],
        "candidateEvidence": [
          {
            "candidateId": "edge-alpha",
            "selected": true,
            "finalScore": 50.0,
            "factorNames": ["p95LatencyMillis", "p99LatencyMillis"],
            "contributionCount": 2,
            "dominantFactorNames": ["p99LatencyMillis"],
            "status": "PARTIAL"
          }
        ],
        "factorEvidence": [
          {
            "factorName": "p99LatencyMillis",
            "appearedInSelectedCandidate": true,
            "appearedInClosestAlternative": true,
            "selectedCandidateContribution": 28.0,
            "closestAlternativeContribution": 33.6,
            "contributionDelta": -5.6,
            "status": "AVAILABLE"
          }
        ],
        "boundaryNote": "Read-only canonical lab evidence packaging; no replay execution, what-if mutation, capsule persistence, upload/share/download, or server-side export/PDF/ZIP generation is performed."
      },
      "decisionReplayReadinessChecklist": {
        "readOnly": true,
        "checklistSchemaVersion": "decision-replay-readiness-checklist/v1",
        "status": "PARTIAL",
        "selectedCandidateId": "backend-a",
        "candidateCount": 2,
        "linkedReplaySnapshotFingerprint": "deterministic-local-hash",
        "linkedReconstructionTraceFingerprint": "deterministic-local-trace-hash",
        "linkedReplayCapsuleFingerprint": "deterministic-local-capsule-hash",
        "decisionVectorStatus": "AVAILABLE",
        "decisionReplayCapsuleStatus": "PARTIAL",
        "availableItemCount": 8,
        "partialItemCount": 2,
        "unknownItemCount": 0,
        "checklistItems": [
          {
            "itemId": "decision-vector-evidence",
            "status": "AVAILABLE",
            "evidenceSourceFieldPath": "decisionVector",
            "missingEvidenceReason": null
          },
          {
            "itemId": "read-only-boundary-evidence",
            "status": "AVAILABLE",
            "evidenceSourceFieldPath": "decisionReplayReadinessChecklist.boundaryNote",
            "missingEvidenceReason": null
          }
        ],
        "boundaryNote": "Read-only lab evidence readiness only; no replay execution, what-if mutation, checklist persistence, upload/share/download, or server-side export/PDF/ZIP generation is performed."
      },
      "decisionReplayEvidenceSourceMap": {
        "readOnly": true,
        "sourceMapSchemaVersion": "decision-replay-evidence-source-map/v1",
        "status": "PARTIAL",
        "selectedCandidateId": "backend-a",
        "candidateCount": 2,
        "linkedReplaySnapshotFingerprint": "deterministic-local-hash",
        "linkedReconstructionTraceFingerprint": "deterministic-local-trace-hash",
        "linkedReplayCapsuleFingerprint": "deterministic-local-capsule-hash",
        "decisionVectorStatus": "AVAILABLE",
        "decisionReplayReadinessChecklistStatus": "PARTIAL",
        "sourceMapEntries": [
          {
            "sourceId": "decision-vector-source",
            "status": "AVAILABLE",
            "sourceFieldPath": "decisionVector",
            "downstreamEvidenceFieldPaths": [
              "decisionReplaySnapshot.decisionVectorStatus",
              "decisionReplayCapsule.decisionVectorStatus"
            ],
            "linkedFingerprint": null
          },
          {
            "sourceId": "linked-fingerprint-source",
            "status": "AVAILABLE",
            "sourceFieldPath": "decisionReplaySnapshot.snapshotFingerprint, decisionReplayReconstructionTrace.traceFingerprint, decisionReplayCapsule.capsuleFingerprint",
            "downstreamEvidenceFieldPaths": [
              "decisionReplayEvidenceSourceMap.linkedReplaySnapshotFingerprint",
              "decisionReplayEvidenceSourceMap.linkedReconstructionTraceFingerprint",
              "decisionReplayEvidenceSourceMap.linkedReplayCapsuleFingerprint"
            ],
            "linkedFingerprint": "deterministic-local-hash"
          },
          {
            "sourceId": "read-only-boundary-source",
            "status": "AVAILABLE",
            "sourceFieldPath": "decisionReplayEvidenceSourceMap.boundaryNote",
            "downstreamEvidenceFieldPaths": [],
            "linkedFingerprint": null
          }
        ],
        "boundaryNote": "Read-only lab source mapping only; no replay execution, what-if mutation, source-map persistence, upload/share/download, new fingerprint generation, or server-side export/PDF/ZIP generation is performed."
      },
      "decisionReplayEvidenceBoundarySummary": {
        "readOnly": true,
        "boundarySummarySchemaVersion": "decision-replay-evidence-boundary-summary/v1",
        "status": "AVAILABLE",
        "selectedCandidateId": "backend-a",
        "candidateCount": 2,
        "decisionVectorStatus": "AVAILABLE",
        "decisionReplayEvidenceSourceMapStatus": "PARTIAL",
        "boundaryItems": [
          {
            "boundaryId": "lab-only-boundary",
            "status": "AVAILABLE",
            "sourceFieldPath": "decisionVector.labProofBoundary, decisionReplayEvidenceSourceMap.boundaryNote",
            "supportingEvidenceFieldPaths": [
              "decisionVector.labProofBoundary",
              "decisionReplayEvidenceSourceMap.boundaryNote"
            ]
          },
          {
            "boundaryId": "production-not-proven-boundary",
            "status": "AVAILABLE",
            "sourceFieldPath": "productionNotProvenBoundary fields",
            "supportingEvidenceFieldPaths": [
              "decisionVector.productionNotProvenBoundary",
              "decisionReplayEvidenceSourceMap.productionNotProvenBoundary"
            ]
          }
        ],
        "boundaryNote": "Read-only lab boundary metadata only; no replay execution, what-if mutation, boundary-summary persistence, upload/share/download, new fingerprint generation, or server-side export/PDF/ZIP generation is performed."
      },
      "decisionReplayEvidenceFieldInventory": {
        "readOnly": true,
        "fieldInventorySchemaVersion": "decision-replay-evidence-field-inventory/v1",
        "status": "AVAILABLE",
        "selectedCandidateId": "backend-a",
        "candidateCount": 2,
        "decisionVectorStatus": "AVAILABLE",
        "decisionReplayEvidenceBoundarySummaryStatus": "AVAILABLE",
        "availableInventoryGroupCount": 12,
        "partialInventoryGroupCount": 0,
        "unknownInventoryGroupCount": 0,
        "inventoryEntries": [
          {
            "inventoryId": "decision-vector-fields",
            "status": "AVAILABLE",
            "sourceFieldPath": "decisionVector",
            "observedFieldPaths": [
              "decisionVector.readOnly",
              "decisionVector.selectedStrategy",
              "decisionVector.candidateCount"
            ],
            "missingOrUnavailableFieldPaths": []
          },
          {
            "inventoryId": "production-not-proven-boundary-fields",
            "status": "AVAILABLE",
            "sourceFieldPath": "productionNotProvenBoundary fields",
            "observedFieldPaths": [
              "decisionVector.productionNotProvenBoundary",
              "decisionReplayEvidenceBoundarySummary.productionNotProvenBoundary"
            ],
            "missingOrUnavailableFieldPaths": []
          }
        ],
        "boundaryNote": "Read-only lab field inventory only; no replay execution, what-if mutation, field-inventory persistence, upload/share/download, new fingerprint generation, or server-side export/PDF/ZIP generation is performed."
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
- Which dominant factors were derived from returned contribution entries.
- Which deterministic replay snapshot fields were available for later review.
- Which evidence field groups were observed or unavailable for reviewer inspection.
- Which explanation gaps remain investigation items.

If a candidate appears weakened by unhealthy state, higher visible latency, higher visible load/connection pressure, or lower capacity/weight where those fields are exposed, the vector can record that visible signal comparison. If the local lab response does not expose enough information, the vector must say that the candidate reason is unknown from visible data.

## Replay, What-If, Logging, and Plugin Roadmap

The Decision Vector is a foundation for current read-only dominant-factor explainability and future work. These roadmap items remain bounded unless a later sprint adds and verifies them:

- Dominant factor analysis: implemented as additive read-only interpretation of returned contribution data only.
- Decision delta analysis: implemented as additive read-only selected-vs-closest-alternative interpretation of returned score and contribution data only.
- Decision replay snapshot: implemented as additive read-only snapshot evidence and deterministic local fingerprint only.
- Decision replay reconstruction trace: implemented as additive read-only reconstruction evidence steps and deterministic local trace fingerprint only.
- Decision replay capsule: implemented as additive read-only canonical evidence packaging and deterministic local capsule fingerprint only.
- Decision replay readiness checklist: implemented as additive read-only lab evidence readiness status over already-built evidence lanes only.
- Decision replay evidence source map: implemented as additive read-only lab evidence source mapping over already-built evidence lanes only.
- Decision replay evidence boundary summary: implemented as additive read-only lab boundary metadata over already-built boundary fields and statuses only.
- Decision replay evidence field inventory: implemented as additive read-only lab field inventory over already-built evidence field groups only.
- Decision evidence null-safety summary: implemented as additive read-only lab null-safety metadata over already-built evidence lanes only.
- Decision evidence status rollup: implemented as additive read-only lab status metadata over already-built evidence lanes only.
- Broader factor modeling beyond current returned calculator contribution data: future/not implemented.
- Replay execution: future/not implemented.
- What-if experiments: future/not implemented.
- Structured decision logging: future/not implemented.
- Strategy plugin explainability: future/not implemented.
- Rack, zone, and topology modeling: future/not implemented.
- Correlated failure modeling: future/not implemented.
- Live interrogation mode: future/not implemented.

Decision Vector data should make those paths safer by keeping known signals, unknown signals, exact scoring availability, and production proof boundaries explicit. The dedicated [`ENTERPRISE_LAB_DECISION_REPLAY_WHAT_IF_PLAN.md`](ENTERPRISE_LAB_DECISION_REPLAY_WHAT_IF_PLAN.md) planning lane uses Decision Vectors as the prerequisite evidence layer for future controlled-lab replay and one-signal what-if analysis, but it does not implement replay execution, what-if execution, a live replay endpoint, production traffic replay, external storage, external telemetry, or real backend mutation.

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

It does not prove production traffic behavior, live-cloud behavior, real-tenant behavior, SLA/SLO achievement, registry publication, container signing, governance application, exact production scoring, broader factor modeling beyond returned calculator contribution data, replay execution, completed what-if experiments, completed strategy plugin explainability, or production readiness.
