# Enterprise Lab Decision Replay Evidence Lane Reference Index

Decision Replay Evidence Lane Reference Index is an additive read-only lab reference lane for
`POST /api/routing/compare`. It provides compact reviewer reference metadata for existing routing compare lab evidence
lanes. It is informational, deterministic, metadata-only, lab-only, and reviewer-reference-only.
LoadBalancerPro remains an Enterprise Lab Cockpit for controlled pre-production routing validation; it is not a demo.

The response field is `results[].decisionReplayEvidenceLaneReferenceIndex`, with schema version
`decision-replay-evidence-lane-reference-index/v1`.

## Source Lanes

The lane reference index is derived only from already-built compare evidence DTOs and existing status, navigation, and
dependency metadata:

- Decision Vector.
- Dominant Factor Analysis.
- Decision Delta Analysis.
- Decision Replay Snapshot.
- Decision Replay Reconstruction Trace.
- Decision Replay Capsule.
- Decision Replay Readiness Checklist.
- Decision Replay Evidence Source Map.
- Decision Replay Evidence Boundary Summary.
- Decision Replay Evidence Field Inventory.
- Decision Evidence Null-Safety Summary.
- Decision Evidence Status Rollup.
- Decision Replay Evidence Lane Navigation Summary.
- Decision Replay Evidence Lane Dependency Map.

It does not inspect raw server input or raw request payload data. It does not use reflection.

## Reference Items

Reference items are returned in this deterministic order:

1. `decision-vector-reference`
2. `dominant-factor-analysis-reference`
3. `decision-delta-analysis-reference`
4. `replay-snapshot-reference`
5. `reconstruction-trace-reference`
6. `replay-capsule-reference`
7. `readiness-checklist-reference`
8. `evidence-source-map-reference`
9. `evidence-boundary-summary-reference`
10. `evidence-field-inventory-reference`
11. `evidence-null-safety-reference`
12. `evidence-status-rollup-reference`
13. `evidence-lane-navigation-reference`
14. `evidence-lane-dependency-map-reference`

Each item includes lane id, label, status, response field path, UI section label, docs reference label, dependency count,
downstream count, read-only flag, boundary-present flag, reference summary, and boundary note.

Statuses are normalized to `AVAILABLE`, `PARTIAL`, or `UNKNOWN`. Missing or unavailable evidence stays unavailable or
safe-null; the lane reference index does not invent selected candidates, candidate sets, closest alternatives, score
gaps, largest delta factors, fingerprints, replay claims, certification claims, guaranteed replay claims, or fake
explanations.

## Boundaries

The lane reference index:

- does not execute replay;
- does not perform what-if mutation;
- does not persist lane reference indexes or audit logs server-side;
- does not export, download, upload, or share lane reference indexes;
- does not change routing behavior;
- does not recompute scores;
- does not retune weights;
- does not infer hidden scoring;
- does not generate a new fingerprint;
- does not use reflection;
- does not add telemetry;
- does not add external calls;
- does not add server-side export/PDF/ZIP/file generation.

It is not production certification, not live-cloud proof, not real-tenant proof, not SLA/SLO proof, not registry
publication proof, not signing proof, not governance application proof, not exact production scoring proof, not guaranteed replay,
not cryptographic production proof, and not production traffic validation.

It is not an approval, remediation, enforcement decision, readiness score, scorecard, quality ranking, correctness
validation, or production decision.

## No-Healthy Path

When no selected candidate is available and candidate count is zero, the lane reference index remains present as safe
lab metadata and returns `UNKNOWN` when all source lane statuses are missing or unknown. It does not invent a selected
candidate, candidate set, closest alternative, score gap, largest delta factor, fingerprint, replay claim,
certification claim, guaranteed replay claim, quality-ranking claim, approval claim, correctness-validation claim, or
fake explanation.
