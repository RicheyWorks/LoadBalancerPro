# Enterprise Lab Decision Replay Evidence Reviewer Closure Summary

Decision Replay Evidence Reviewer Closure Summary is an additive read-only lab metadata lane for
`POST /api/routing/compare`. It gives reviewers a compact closure-ready summary of what can be safely concluded
after inspecting already-exposed decision replay reviewer evidence and what remains not proven. It is reviewer closure
metadata only, not production validation or replay proof.

The response field is `results[].decisionReplayEvidenceReviewerClosureSummary`, with schema version
`decision-replay-evidence-reviewer-closure-summary/v1`.

## Source

Reviewer closure summary is derived only from existing response-surface metadata:

- `results[].decisionReplayEvidenceStatusRollup`;
- `results[].decisionReplayEvidenceLaneDependencyMap`;
- `results[].decisionReplayEvidenceLaneReferenceIndex`;
- `results[].decisionReplayEvidenceLaneDependencySummary`;
- `results[].decisionReplayEvidenceLaneConsistencySummary`;
- `results[].decisionReplayEvidenceReviewerSnapshot`;
- `results[].decisionReplayEvidenceReviewerGuidance`;
- `results[].decisionReplayEvidenceReviewerHandoffSummary`.

It does not inspect raw server input or raw request payload data. It does not use reflection.

## Summary Fields

The closure summary includes:

- normalized closure status: `AVAILABLE`, `PARTIAL`, or `UNKNOWN`;
- closure disposition: `REVIEW_COMPLETE_WITH_LIMITATIONS`, `REVIEW_INCOMPLETE`, or `UNKNOWN`;
- reviewer snapshot status, reviewer guidance status, reviewer handoff status, and consistency status from existing
  metadata;
- selected strategy, selected candidate, and candidate count when already present in reviewer metadata;
- total, available, partial, and unknown lane counts from already-exposed reviewer metadata;
- deterministic closure bullets;
- deterministic safe conclusions;
- deterministic unresolved boundaries;
- deterministic evidence surfaces referenced;
- deterministic reviewer closure summary text;
- short limitations stating that the closure summary is read-only metadata, not replay proof, not scoring proof,
  not correctness validation, not production readiness, not production certification, not guaranteed replay, and not
  production validation.

## Boundaries

The reviewer closure summary:

- does not execute replay;
- does not perform what-if mutation;
- does not persist reviewer closure summary data or audit logs server-side;
- does not export, download, upload, or share reviewer closure summary data;
- does not change routing behavior;
- does not recompute scores;
- does not retune weights;
- does not infer hidden scoring;
- does not generate a new fingerprint;
- does not use reflection;
- does not add telemetry;
- does not add external calls;
- does not add server-side export/PDF/ZIP/file generation;
- is not production validation.

It is not production certification, not live-cloud proof, not real-tenant proof, not SLA/SLO proof, not registry
publication proof, not signing proof, not governance application proof, not exact production scoring proof, not
guaranteed replay, not cryptographic production proof, and not production traffic validation.

It is not an approval, remediation, enforcement decision, readiness score, scorecard, quality ranking, correctness
validation, or production decision.

## No-Healthy Path

When no selected candidate is available and candidate count is zero, reviewer closure summary stays present as safe lab
metadata and reports `UNKNOWN` with zero lane counts. It does not invent a selected candidate, candidate set, closest
alternative, score gap, largest delta factor, fingerprint, replay claim, certification claim, guaranteed replay claim,
quality-ranking claim, approval claim, correctness-validation claim, remediation claim, enforcement claim, or fake
explanation.
