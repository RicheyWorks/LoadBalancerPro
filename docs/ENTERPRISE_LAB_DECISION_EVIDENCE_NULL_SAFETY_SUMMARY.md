# Enterprise Lab Decision Evidence Null-Safety Summary

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

Decision Evidence Null-Safety Summary is a read-only lab null-safety lane for
`POST /api/routing/compare`.

Each routing comparison result can expose additive
`results[].decisionReplayEvidenceNullSafetySummary` data. The summary is derived only from already-built compare
response DTOs:

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

It summarizes existing routing compare lab null, missing, unavailable, and no-healthy/failure-path safety. It does not inspect raw server input or raw request payloads. It does not use reflection. It does not generate a new fingerprint.
It does not recompute scores and does not retune weights. It does not persist null-safety summaries or audit logs
server-side. It does not execute replay and does not perform what-if mutation. It does not change routing behavior. It
does not export, download, or share null-safety summaries.

## Response Shape

The response schema version is `decision-replay-evidence-null-safety-summary/v1`.

The top-level summary includes:

- `readOnly`
- `nullSafetySchemaVersion`
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
- `decisionReplayEvidenceBoundarySummaryStatus`
- `decisionReplayEvidenceFieldInventoryStatus`
- `availableNullSafetyItemCount`
- `partialNullSafetyItemCount`
- `unknownNullSafetyItemCount`
- `nullSafetyItems`
- `explanation`
- `boundaryNote`
- `productionNotProvenBoundary`

Each `nullSafetyItems[]` entry includes:

- `nullSafetyId`
- `label`
- `status`
- `sourceFieldPath`
- `checkedFieldPaths`
- `unavailableFieldPaths`
- `checkedFieldCount`
- `unavailableFieldCount`
- `safetySummary`
- `boundaryNote`

## Deterministic Null-Safety Item Order

Null-safety items are emitted in this stable order:

1. `selected-candidate-null-safety`
2. `candidate-set-null-safety`
3. `score-gap-null-safety`
4. `closest-alternative-null-safety`
5. `largest-delta-factor-null-safety`
6. `linked-fingerprint-null-safety`
7. `candidate-evidence-null-safety`
8. `factor-evidence-null-safety`
9. `field-inventory-null-safety`
10. `no-healthy-path-null-safety`
11. `boundary-text-null-safety`
12. `production-not-proven-null-safety`

Statuses are normalized to `AVAILABLE`, `PARTIAL`, or `UNKNOWN`.

## Null-Safety Boundary

The null-safety summary uses explicit DTO accessor calls, non-null objects, non-empty lists/maps, nonblank strings,
safe statuses, candidate counts, and already-built field inventory observed/missing counts. Missing, null, blank,
empty, or non-finite evidence is unavailable rather than invented. The service does not use reflection, raw request
payloads, raw server fields, routing strategy internals, `ServerStateVector`, or `ServerScoreCalculator`.

The linked fingerprint item summarizes only already-present snapshot, reconstruction trace, replay capsule, readiness
checklist, and evidence source map fingerprint fields. It does not generate a null-safety fingerprint and does not
claim cryptographic production proof.

## Missing And No-Healthy Evidence

If selected candidate, candidate set, closest alternative, score gap, largest delta factor, fingerprint, source
statuses, field groups, or boundary evidence is missing, the response returns `UNKNOWN` or `PARTIAL` status instead of
inventing evidence.

Failure/no-healthy-server results remain safe: no selected candidate, candidate set, alternative candidate, score gap,
largest delta factor, fingerprint, replay claim, guaranteed replay claim, production certification claim, or
explanation evidence is fabricated. Empty candidate sets and null selected candidates remain explicit safe-null
evidence for that path.

## Safety Boundaries

This lane does not:

- execute replay
- perform what-if mutation
- persist null-safety summary data or audit logs
- add telemetry
- add external calls
- add upload/share/download behavior
- add server-side export/PDF/ZIP generation
- generate a new fingerprint
- use reflection for summary generation
- use `MessageDigest`, SHA-256, timestamps, random IDs, hostnames, environment variables, file paths, secrets,
  local usernames, machine-specific data, or network-specific data for null-safety output
- recompute scores
- infer hidden scoring
- retune weights
- change routing behavior, scoring behavior, strategy weights, server selection, proxy behavior, dependencies, or app
  configuration
- alter `ServerScoreCalculator`
- add approval, certification, remediation, enforcement, production-readiness, production-proof, quality ranking, or
  guarantee language

This is lab explainability/reviewer-trust null-safety metadata only. It is not production certification, not
live-cloud proof, not real-tenant proof, not SLA/SLO proof, not registry publication proof, not signing proof, not
governance application proof, not exact production scoring proof, not cryptographic production proof, not guaranteed
replay, and not production traffic validation.

It is not guaranteed replay.
