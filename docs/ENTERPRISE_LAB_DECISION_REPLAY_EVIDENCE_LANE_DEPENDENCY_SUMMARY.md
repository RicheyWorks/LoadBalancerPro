# Enterprise Lab Decision Replay Evidence Lane Dependency Summary

Decision Replay Evidence Lane Dependency Summary is an additive read-only lab metadata lane for
`POST /api/routing/compare`. It summarizes the already-built
`results[].decisionReplayEvidenceLaneReferenceIndex` so reviewers can understand dependency shape without manually
counting every lane.

The response field is `results[].decisionReplayEvidenceLaneDependencySummary`, with schema version
`decision-replay-evidence-lane-dependency-summary/v1`.

## Source

The dependency summary is derived only from `results[].decisionReplayEvidenceLaneReferenceIndex`.
It uses existing reference item statuses, dependency counts, downstream counts, selected-candidate metadata, candidate
count metadata, and available/partial/unknown lane counts. It does not inspect raw server input or raw request payload
data. It does not use reflection.

## Summary Fields

The summary includes:

- normalized status: `AVAILABLE`, `PARTIAL`, or `UNKNOWN`;
- total lane count;
- available, partial, and unknown lane counts copied from the reference index;
- root lane count for lanes with zero dependencies;
- terminal lane count for lanes with zero downstream references;
- max dependency count and max downstream count;
- densest dependency lane ids from the existing reference items;
- widest downstream lane ids from the existing reference items;
- deterministic reviewer summary text;
- short limitations stating that the summary is read-only metadata, not replay execution, not scoring proof, not correctness validation, and not production readiness.

## Boundaries

The dependency summary:

- does not execute replay;
- does not perform what-if mutation;
- does not persist lane dependency summaries or audit logs server-side;
- does not export, download, upload, or share lane dependency summaries;
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

When the reference index is present but no selected candidate is available and candidate count is zero, the dependency
summary stays present as safe lab metadata and mirrors the reference index status. It does not invent a selected
candidate, candidate set, closest alternative, score gap, largest delta factor, fingerprint, replay claim,
certification claim, guaranteed replay claim, quality-ranking claim, approval claim, correctness-validation claim, or
fake explanation.
