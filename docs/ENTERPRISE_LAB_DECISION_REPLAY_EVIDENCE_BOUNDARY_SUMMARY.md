# Enterprise Lab Decision Replay Evidence Boundary Summary

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

Decision Replay Evidence Boundary Summary is a read-only lab boundary-metadata lane for
`POST /api/routing/compare`.

Each routing comparison result can expose additive
`results[].decisionReplayEvidenceBoundarySummary` data. The summary is derived only from already-built
compare response DTOs:

- `decisionVector`
- `dominantFactorAnalysis`
- `decisionDeltaAnalysis`
- `decisionReplaySnapshot`
- `decisionReplayReconstructionTrace`
- `decisionReplayCapsule`
- `decisionReplayReadinessChecklist`
- `decisionReplayEvidenceSourceMap`

It summarizes existing lab-only, read-only, and not-proven boundary fields and statuses. It does not derive from raw
server input except through those already-built DTOs.
It does not generate a new fingerprint.
It does not recompute scores and does not retune weights.
It does not persist boundary-summary data or audit logs.
It does not execute replay and does not perform what-if mutation.

## Response Shape

The response schema version is `decision-replay-evidence-boundary-summary/v1`.

The top-level summary includes:

- `readOnly`
- `boundarySummarySchemaVersion`
- `source`
- `status`
- `strategyId`
- `selectedCandidateId`
- `candidateCount`
- `decisionVectorStatus`
- `dominantFactorAnalysisStatus`
- `decisionDeltaAnalysisStatus`
- `decisionReplaySnapshotStatus`
- `decisionReplayReconstructionTraceStatus`
- `decisionReplayCapsuleStatus`
- `decisionReplayReadinessChecklistStatus`
- `decisionReplayEvidenceSourceMapStatus`
- `boundaryItems`
- `explanation`
- `boundaryNote`
- `productionNotProvenBoundary`

Each `boundaryItems[]` entry includes:

- `boundaryId`
- `label`
- `status`
- `sourceFieldPath`
- `supportingEvidenceFieldPaths`
- `evidenceSummary`
- `boundaryNote`

## Deterministic Boundary Item Order

Boundary items are emitted in this stable order:

1. `lab-only-boundary`
2. `read-only-boundary`
3. `no-replay-execution-boundary`
4. `no-what-if-mutation-boundary`
5. `no-persistence-storage-boundary`
6. `no-export-share-download-boundary`
7. `no-routing-behavior-change-boundary`
8. `no-score-recomputation-boundary`
9. `fingerprint-boundary`
10. `production-not-proven-boundary`

Statuses are normalized to `AVAILABLE`, `PARTIAL`, or `UNKNOWN`.

## Safety Boundaries

This lane does not:

- execute replay
- perform what-if mutation
- persist boundary-summary data or audit logs
- add telemetry
- add external calls
- add upload/share/download behavior
- add server-side export/PDF/ZIP generation
- generate a new fingerprint
- use `MessageDigest`, SHA-256, timestamps, random IDs, hostnames, environment variables, file paths, secrets,
  local usernames, machine-specific data, or network-specific data for summary output
- recompute scores
- infer hidden scoring
- retune weights
- change routing behavior, scoring behavior, strategy weights, server selection, proxy behavior, dependencies, or app
  configuration
- alter `ServerScoreCalculator`

If selected candidate, candidate set, source statuses, or boundary evidence is missing, the response returns `UNKNOWN`
or `PARTIAL` status instead of inventing evidence.

This is lab explainability/reviewer-trust boundary metadata only. It is not live-cloud proof, real-tenant proof,
SLA/SLO proof, registry publication proof, container signing proof, governance application proof, production traffic
validation, exact production scoring proof, cryptographic production proof, or replay execution proof.
