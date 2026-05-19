# Enterprise Lab Decision Evidence Status Rollup

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

Decision Evidence Status Rollup is a read-only lab status metadata lane for `POST /api/routing/compare`.

Each routing comparison result can expose additive `results[].decisionReplayEvidenceStatusRollup` data. The rollup is
derived only from already-built compare response DTOs:

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

It compactly summarizes existing routing compare lab evidence status, selected-candidate presence, candidate count, and
boundary state. Status items are informational, deterministic, metadata-only, and read-only. The rollup does not inspect raw server input. It does not inspect raw request payloads. It does not use reflection. It does not generate a new fingerprint. It does not execute replay and does not perform what-if mutation. It does not persist status rollups or audit logs server-side. It
does not export, download, or share status rollups. It does not change routing behavior. It does not recompute scores.
It does not infer hidden scoring and does not retune weights.

## Response Shape

The response schema version is `decision-replay-evidence-status-rollup/v1`.

The top-level rollup includes:

- `readOnly`
- `statusRollupSchemaVersion`
- `source`
- `status`
- `strategyId`
- `selectedCandidateId`
- `candidateCount`
- `availableLaneCount`
- `partialLaneCount`
- `unknownLaneCount`
- `statusItems`
- `explanation`
- `boundaryNote`
- `productionNotProvenBoundary`

Each `statusItems[]` entry includes:

- `laneId`
- `label`
- `status`
- `sourceFieldPath`
- `readOnly`
- `selectedCandidatePresent`
- `candidateCount`
- `boundaryPresent`
- `evidenceSummary`
- `boundaryNote`

## Deterministic Status Item Order

Status items are emitted in this stable order:

1. `decision-vector-status`
2. `dominant-factor-analysis-status`
3. `decision-delta-analysis-status`
4. `replay-snapshot-status`
5. `reconstruction-trace-status`
6. `replay-capsule-status`
7. `readiness-checklist-status`
8. `evidence-source-map-status`
9. `evidence-boundary-summary-status`
10. `evidence-field-inventory-status`
11. `evidence-null-safety-status`
12. `read-only-boundary-status`
13. `production-not-proven-status`

Statuses are normalized to `AVAILABLE`, `PARTIAL`, or `UNKNOWN`.

## Status Boundary

The status rollup uses explicit DTO accessor calls, non-null objects, non-empty lists/maps, nonblank strings, explicit
safe statuses, candidate counts, and already-built status/count fields. Missing, null, blank, empty, or non-finite
evidence is unavailable rather than invented.

The status rollup does not use raw request payloads, raw server fields, routing strategy internals, `ServerStateVector`,
or `ServerScoreCalculator`.

The status rollup summarizes only existing fingerprint-bearing lane status. It does not generate a status-rollup
fingerprint and does not claim cryptographic production proof.

## Missing And No-Healthy Evidence

If selected candidate, candidate set, lane statuses, or boundary evidence is missing, the response returns `UNKNOWN` or
`PARTIAL` status instead of inventing evidence.

Failure/no-healthy-server results remain safe: no selected candidate, candidate set, alternative candidate, score gap,
largest delta factor, fingerprint, replay claim, guaranteed replay claim, production certification claim, quality-ranking
claim, or explanation evidence is fabricated. Empty candidate sets and null selected candidates remain explicit safe-null
evidence for that path.

## Safety Boundaries

This lane does not:

- execute replay
- perform what-if mutation
- persist status rollup data or audit logs
- add telemetry
- add external calls
- add upload/share/download behavior
- add server-side export/PDF/ZIP generation
- generate a new fingerprint
- use reflection for rollup generation
- use `MessageDigest`, SHA-256, timestamps, random IDs, hostnames, environment variables, file paths, secrets, local
  usernames, machine-specific data, or network-specific data for status-rollup output
- recompute scores
- infer hidden scoring
- retune weights
- change routing behavior, scoring behavior, strategy weights, server selection, proxy behavior, dependencies, or app
  configuration
- alter `ServerScoreCalculator`
- add approval, certification, remediation, enforcement, production-readiness, scorecard, production-proof,
  quality-ranking, or guarantee language

This is lab explainability/reviewer-trust status metadata only. It is not production certification, not live-cloud proof,
not real-tenant proof, not SLA/SLO proof, not registry publication proof, not signing proof, not governance application
proof, not exact production scoring proof, not cryptographic production proof, not guaranteed replay, and not production traffic validation.

It is not an approval, remediation, enforcement, production-readiness decision, scorecard, quality-ranking, or guarantee.
It is not guaranteed replay.
