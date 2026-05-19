# Enterprise Lab Decision Replay Evidence Field Inventory

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

Decision Replay Evidence Field Inventory is a read-only lab field-inventory lane for
`POST /api/routing/compare`.

Each routing comparison result can expose additive
`results[].decisionReplayEvidenceFieldInventory` data. The inventory is derived only from already-built compare
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

It inventories existing routing compare lab evidence field groups. It does not derive from raw server input except
through those already-built DTOs. It does not use reflection. It does not generate a new fingerprint. It does not recompute scores and does not retune weights. It does not persist field inventories or audit logs server-side. It does not execute replay and does not perform what-if mutation. It does not change routing behavior. It does not export, download, or share field inventories.

## Response Shape

The response schema version is `decision-replay-evidence-field-inventory/v1`.

The top-level inventory includes:

- `readOnly`
- `fieldInventorySchemaVersion`
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
- `availableInventoryGroupCount`
- `partialInventoryGroupCount`
- `unknownInventoryGroupCount`
- `inventoryEntries`
- `explanation`
- `boundaryNote`
- `productionNotProvenBoundary`

Each `inventoryEntries[]` entry includes:

- `inventoryId`
- `label`
- `status`
- `sourceFieldPath`
- `observedFieldPaths`
- `missingOrUnavailableFieldPaths`
- `observedFieldCount`
- `missingOrUnavailableFieldCount`
- `evidenceSummary`
- `boundaryNote`

## Deterministic Inventory Entry Order

Inventory entries are emitted in this stable order:

1. `decision-vector-fields`
2. `dominant-factor-analysis-fields`
3. `decision-delta-analysis-fields`
4. `replay-snapshot-fields`
5. `reconstruction-trace-fields`
6. `replay-capsule-fields`
7. `readiness-checklist-fields`
8. `evidence-source-map-fields`
9. `evidence-boundary-summary-fields`
10. `linked-fingerprint-fields`
11. `read-only-boundary-fields`
12. `production-not-proven-boundary-fields`

Statuses are normalized to `AVAILABLE`, `PARTIAL`, or `UNKNOWN`.

## Field Presence Boundary

The field inventory uses explicit DTO accessor calls, non-null objects, non-empty lists/maps, nonblank strings, and
safe statuses. Missing, null, empty, or non-finite evidence is unavailable rather than invented. The service does not
use reflection, raw request payloads, raw server fields, routing strategy internals, `ServerStateVector`, or
`ServerScoreCalculator`.

The linked fingerprint entry inventories only already-present snapshot, reconstruction trace, replay capsule,
readiness checklist, and evidence source map fingerprint fields. It does not generate a field-inventory fingerprint
and does not claim cryptographic production proof.

## Missing Evidence

If selected candidate, candidate set, source statuses, field groups, or boundary evidence is missing, the response
returns `UNKNOWN` or `PARTIAL` status instead of inventing evidence.

Failure/no-healthy-server results remain safe: no selected candidate, candidate set, alternative candidate, score gap,
largest delta factor, fingerprint, replay claim, guaranteed replay claim, production certification claim, or
explanation evidence is fabricated.

## Safety Boundaries

This lane does not:

- execute replay
- perform what-if mutation
- persist field-inventory data or audit logs
- add telemetry
- add external calls
- add upload/share/download behavior
- add server-side export/PDF/ZIP generation
- generate a new fingerprint
- use reflection for inventory generation
- use `MessageDigest`, SHA-256, timestamps, random IDs, hostnames, environment variables, file paths, secrets,
  local usernames, machine-specific data, or network-specific data for inventory output
- recompute scores
- infer hidden scoring
- retune weights
- change routing behavior, scoring behavior, strategy weights, server selection, proxy behavior, dependencies, or app
  configuration
- alter `ServerScoreCalculator`

This is lab explainability/reviewer-trust field inventory only. It is not production certification, not live-cloud proof, not real-tenant proof, not SLA/SLO proof, not registry publication proof, not signing proof, not governance application proof, not exact production scoring proof, not cryptographic production proof, not guaranteed replay, and not production traffic validation.
