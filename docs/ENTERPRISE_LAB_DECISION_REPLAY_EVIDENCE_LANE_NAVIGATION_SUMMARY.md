# Enterprise Lab Decision Replay Evidence Lane Navigation Summary

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

Decision Replay Evidence Lane Navigation Summary is a read-only lab reviewer-navigation metadata lane for
`POST /api/routing/compare`.

Each routing comparison result can expose additive `results[].decisionReplayEvidenceLaneNavigationSummary` data. The
navigation summary is derived only from already-built compare response DTOs:

- `decisionVector`
- `dominantFactorAnalysis`
- `decisionDeltaAnalysis`
- `decisionReplaySnapshot`
- `decisionReplayReconstructionTrace`
- `decisionReplayCapsule`
- `decisionReplayReadinessChecklist`
- `decisionReplayEvidenceSourceMap`
- `decisionReplayEvidenceBoundarySummary`
- `decisionReplayEvidenceFieldInventory`
- `decisionReplayEvidenceNullSafetySummary`
- `decisionReplayEvidenceStatusRollup`

It provides deterministic reviewer navigation metadata for existing routing compare lab evidence lanes: lane id, label,
response field path, current status, UI section label, docs reference label, read-only state, and boundary presence.
Navigation items are informational, deterministic, metadata-only, reviewer-navigation-only, and read-only. The summary
does not inspect raw server input. It does not inspect raw request payloads. It does not use reflection. It does not
generate a new fingerprint. It does not execute replay and does not perform what-if mutation. It does not persist lane
navigation summaries or audit logs server-side. It does not export, download, or share lane navigation summaries. It
does not change routing behavior. It does not recompute scores. It does not infer hidden scoring and does not retune
weights.

## Response Shape

The response schema version is `decision-replay-evidence-lane-navigation-summary/v1`.

The top-level summary includes:

- `readOnly`
- `laneNavigationSchemaVersion`
- `source`
- `status`
- `strategyId`
- `selectedCandidateId`
- `candidateCount`
- `availableLaneCount`
- `partialLaneCount`
- `unknownLaneCount`
- `navigationItems`
- `explanation`
- `boundaryNote`
- `productionNotProvenBoundary`

Each `navigationItems[]` entry includes:

- `laneId`
- `label`
- `status`
- `responseFieldPath`
- `uiSectionLabel`
- `docsReferenceLabel`
- `readOnly`
- `boundaryPresent`
- `navigationSummary`
- `boundaryNote`

## Deterministic Navigation Item Order

Navigation items are emitted in this stable order:

1. `decision-vector-navigation`
2. `dominant-factor-analysis-navigation`
3. `decision-delta-analysis-navigation`
4. `replay-snapshot-navigation`
5. `reconstruction-trace-navigation`
6. `replay-capsule-navigation`
7. `readiness-checklist-navigation`
8. `evidence-source-map-navigation`
9. `evidence-boundary-summary-navigation`
10. `evidence-field-inventory-navigation`
11. `evidence-null-safety-navigation`
12. `evidence-status-rollup-navigation`

Statuses are normalized to `AVAILABLE`, `PARTIAL`, or `UNKNOWN`.

## Navigation Boundary

The lane navigation summary uses explicit DTO accessor calls, non-null objects, non-empty lists/maps, nonblank strings,
explicit safe statuses, candidate counts, and already-built status/count fields. Missing, null, blank, empty, or
non-finite evidence is unavailable rather than invented.

The lane navigation summary does not use raw request payloads, raw server fields, routing strategy internals,
`ServerStateVector`, or `ServerScoreCalculator`.

The lane navigation summary points to existing response fields, UI section labels, and docs reference labels only. It
does not generate a lane-navigation fingerprint and does not claim cryptographic production proof.

## Missing And No-Healthy Evidence

If selected candidate, candidate set, lane statuses, or navigation evidence is missing, the response returns `UNKNOWN`
or `PARTIAL` status instead of inventing evidence.

Failure/no-healthy-server results remain safe: no selected candidate, candidate set, alternative candidate, score gap,
largest delta factor, fingerprint, replay claim, guaranteed replay claim, production certification claim,
quality-ranking claim, approval claim, or explanation evidence is fabricated. Empty candidate sets and null selected
candidates remain explicit safe-null evidence for that path.

## Safety Boundaries

This lane does not:

- execute replay
- perform what-if mutation
- persist lane navigation summary data or audit logs
- add telemetry
- add external calls
- add upload/share/download behavior
- add server-side export/PDF/ZIP generation
- generate a new fingerprint
- use reflection for navigation summary generation
- use `MessageDigest`, SHA-256, timestamps, random IDs, hostnames, environment variables, file paths, secrets, local
  usernames, machine-specific data, or network-specific data for lane-navigation output
- recompute scores
- infer hidden scoring
- retune weights
- change routing behavior, scoring behavior, strategy weights, server selection, proxy behavior, dependencies, or app
  configuration
- alter `ServerScoreCalculator`
- add approval, certification, remediation, enforcement, production-readiness, scorecard, production-proof,
  quality-ranking, correctness-validation, or guarantee language

This is lab explainability/reviewer-trust navigation metadata only:

- not production certification
- not live-cloud proof
- not real-tenant proof
- not SLA/SLO proof
- not registry publication proof
- not signing proof
- not governance application proof
- not exact production scoring proof
- not cryptographic production proof
- not guaranteed replay
- not production traffic validation

It is not an approval, remediation, enforcement, production-readiness decision, readiness score, scorecard,
quality-ranking, correctness validation, or guarantee.
