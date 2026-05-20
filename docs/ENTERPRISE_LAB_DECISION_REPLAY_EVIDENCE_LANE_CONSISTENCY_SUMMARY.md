# Enterprise Lab Decision Replay Evidence Lane Consistency Summary

Decision Replay Evidence Lane Consistency Summary is an additive read-only lab metadata lane for
`POST /api/routing/compare`. It cross-checks already exposed lane metadata so reviewers can see whether the
lane status and count surfaces agree without manually comparing every field.

The response field is `results[].decisionReplayEvidenceLaneConsistencySummary`, with schema version
`decision-replay-evidence-lane-consistency-summary/v1`.

## Source

The consistency summary is derived only from existing response-surface metadata:

- `results[].decisionReplayEvidenceStatusRollup`;
- `results[].decisionReplayEvidenceLaneDependencyMap`;
- `results[].decisionReplayEvidenceLaneReferenceIndex`;
- `results[].decisionReplayEvidenceLaneDependencySummary`.

It does not inspect raw server input or raw request payload data. It does not use reflection.

## Summary Fields

The summary includes:

- normalized consistency status: `CONSISTENT`, `PARTIAL`, or `UNKNOWN`;
- source surface statuses for the reference index, dependency summary, status rollup, and dependency map;
- total, available, partial, and unknown lane counts from the matching reference-index and dependency-summary
  surfaces;
- dependency-map, reference-index, and dependency-summary lane counts as reviewer context;
- deterministic mismatched count field names, if any;
- deterministic missing surface names, if any;
- deterministic consistency check objects with `name`, `status`, `expected`, `actual`, and `detail`;
- deterministic reviewer summary text;
- short limitations stating that the summary is read-only metadata, not replay execution, not scoring proof,
  not correctness validation, not production readiness, and not guaranteed replay.

## Boundaries

The consistency summary:

- does not execute replay;
- does not perform what-if mutation;
- does not persist lane consistency summaries or audit logs server-side;
- does not export, download, upload, or share lane consistency summaries;
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
publication proof, not signing proof, not governance application proof, not exact production scoring proof, not
guaranteed replay, not cryptographic production proof, and not production traffic validation.

It is not an approval, remediation, enforcement decision, readiness score, scorecard, quality ranking, correctness
validation, or production decision.

## No-Healthy Path

When no selected candidate is available and candidate count is zero, the consistency summary stays present as safe
lab metadata and reports `UNKNOWN`. It does not invent a selected candidate, candidate set, closest alternative,
score gap, largest delta factor, fingerprint, replay claim, certification claim, guaranteed replay claim,
quality-ranking claim, approval claim, correctness-validation claim, or fake explanation.
