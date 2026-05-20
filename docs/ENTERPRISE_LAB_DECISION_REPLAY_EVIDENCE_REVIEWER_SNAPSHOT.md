# Enterprise Lab Decision Replay Evidence Reviewer Snapshot

Decision Replay Evidence Reviewer Snapshot is an additive read-only lab metadata lane for
`POST /api/routing/compare`. It gives reviewers a compact one-glance summary of already-exposed decision replay
evidence surfaces. It is a convenience/readability layer only.

The response field is `results[].decisionReplayEvidenceReviewerSnapshot`, with schema version
`decision-replay-evidence-reviewer-snapshot/v1`.

## Source

The reviewer snapshot is derived only from existing response-surface metadata:

- `results[].decisionReplayEvidenceStatusRollup`;
- `results[].decisionReplayEvidenceLaneDependencyMap`;
- `results[].decisionReplayEvidenceLaneReferenceIndex`;
- `results[].decisionReplayEvidenceLaneDependencySummary`;
- `results[].decisionReplayEvidenceLaneConsistencySummary`.

It does not inspect raw server input or raw request payload data. It does not use reflection.

## Summary Fields

The snapshot includes:

- normalized snapshot status: `AVAILABLE`, `PARTIAL`, or `UNKNOWN`;
- copied or derived consistency, reference-index, dependency-summary, status-rollup, and dependency-map statuses;
- total, available, partial, and unknown lane counts from the already-exposed consistency/dependency/reference
  surfaces;
- checked and missing reviewer surface counts;
- deterministic missing surface names, if any;
- deterministic reviewer highlights;
- deterministic reviewer warnings;
- deterministic reviewer summary text;
- short limitations stating that the snapshot is read-only metadata, not replay execution, not scoring proof,
  not correctness validation, not production readiness, not production certification, and not guaranteed replay.

## Boundaries

The reviewer snapshot:

- does not execute replay;
- does not perform what-if mutation;
- does not persist reviewer snapshots or audit logs server-side;
- does not export, download, upload, or share reviewer snapshots;
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

When no selected candidate is available and candidate count is zero, the reviewer snapshot stays present as safe lab
metadata and reports `UNKNOWN` with zero lane counts. It does not invent a selected candidate, candidate set, closest
alternative, score gap, largest delta factor, fingerprint, replay claim, certification claim, guaranteed replay claim,
quality-ranking claim, approval claim, correctness-validation claim, or fake explanation.
