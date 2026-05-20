# Enterprise Lab Decision Replay Evidence Reviewer Handoff Summary

Decision Replay Evidence Reviewer Handoff Summary is an additive read-only lab metadata lane for
`POST /api/routing/compare`. It gives reviewers compact handoff-ready notes derived from already-exposed decision
replay reviewer metadata. It is a reviewer convenience layer only, not a production validation or replay proof lane.

The response field is `results[].decisionReplayEvidenceReviewerHandoffSummary`, with schema version
`decision-replay-evidence-reviewer-handoff-summary/v1`.

## Source

Reviewer handoff summary is derived only from existing response-surface metadata:

- `results[].decisionReplayEvidenceStatusRollup`;
- `results[].decisionReplayEvidenceLaneDependencyMap`;
- `results[].decisionReplayEvidenceLaneReferenceIndex`;
- `results[].decisionReplayEvidenceLaneDependencySummary`;
- `results[].decisionReplayEvidenceLaneConsistencySummary`;
- `results[].decisionReplayEvidenceReviewerSnapshot`;
- `results[].decisionReplayEvidenceReviewerGuidance`.

It does not inspect raw server input or raw request payload data. It does not use reflection.

## Summary Fields

The handoff summary includes:

- normalized handoff status: `AVAILABLE`, `PARTIAL`, or `UNKNOWN`;
- handoff priority: `REVIEW`, `INFORMATIONAL`, or `UNKNOWN`;
- reviewer snapshot status, reviewer guidance status, and consistency status from existing metadata;
- selected strategy, selected candidate, and candidate count when already present in reviewer metadata;
- total, available, partial, and unknown lane counts from already-exposed reviewer metadata;
- deterministic handoff bullets;
- deterministic operator follow-up items;
- deterministic evidence surfaces referenced;
- deterministic caution notes;
- deterministic reviewer handoff summary text;
- short limitations stating that the handoff summary is read-only metadata, not replay proof, not scoring proof,
  not correctness validation, not production readiness, not production certification, not guaranteed replay, and not
  production validation.

## Boundaries

The reviewer handoff summary:

- does not execute replay;
- does not perform what-if mutation;
- does not persist reviewer handoff summary data or audit logs server-side;
- does not export, download, upload, or share reviewer handoff summary data;
- does not change routing behavior;
- does not recompute scores;
- does not retune weights;
- does not infer hidden scoring;
- does not generate a new fingerprint;
- does not use reflection;
- does not add telemetry;
- does not add external calls;
- does not add server-side export/PDF/ZIP/file generation.
- is not production validation.

It is not production certification, not live-cloud proof, not real-tenant proof, not SLA/SLO proof, not registry
publication proof, not signing proof, not governance application proof, not exact production scoring proof, not
guaranteed replay, not cryptographic production proof, and not production traffic validation.

It is not an approval, remediation, enforcement decision, readiness score, scorecard, quality ranking, correctness
validation, or production decision.

## No-Healthy Path

When no selected candidate is available and candidate count is zero, reviewer handoff summary stays present as safe lab
metadata and reports `UNKNOWN` with zero lane counts. It does not invent a selected candidate, candidate set, closest
alternative, score gap, largest delta factor, fingerprint, replay claim, certification claim, guaranteed replay claim,
quality-ranking claim, approval claim, correctness-validation claim, remediation claim, enforcement claim, or fake
explanation.
