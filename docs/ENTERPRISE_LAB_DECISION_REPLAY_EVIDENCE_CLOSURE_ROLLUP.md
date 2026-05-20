# Enterprise Lab Decision Replay Evidence Closure Rollup

Decision Replay Evidence Closure Rollup is an additive read-only response-level metadata lane for
`POST /api/routing/compare`. It summarizes already-returned
`results[].decisionReplayEvidenceReviewerClosureSummary` fields across the full compare response so reviewers can
quickly see whether every result includes reviewer closure metadata and whether the response still carries explicit
not-proven boundaries.

The response field is `decisionReplayEvidenceReviewerClosureRollup`.

## Source

The closure rollup is derived only from existing response data:

- `results[].decisionReplayEvidenceReviewerClosureSummary.status`;
- `results[].decisionReplayEvidenceReviewerClosureSummary.closureDisposition`;
- `results[].decisionReplayEvidenceReviewerClosureSummary.unresolvedBoundaries`.

It does not inspect raw server input, raw request payload data, routing internals, scoring internals, strategy
internals, proxy state, environment variables, system properties, time, random values, or filesystem/process APIs.
It does not use reflection.

## Summary Fields

The closure rollup includes:

- response-level status: `COMPLETE`, `PARTIAL`, or `UNKNOWN`;
- response-level disposition: `REVIEW_COMPLETE_WITH_LIMITATIONS`, `REVIEW_INCOMPLETE`, or `UNKNOWN`;
- result count;
- count of results with reviewer closure summaries;
- count of results missing reviewer closure summaries;
- count of results whose closure disposition is `REVIEW_COMPLETE_WITH_LIMITATIONS`;
- count of results that are `UNKNOWN` or not available;
- reviewer-ready boolean;
- deterministic summary text;
- explicit not-proven boundaries:
  - `not replay proof`;
  - `not scoring proof`;
  - `not correctness validation`;
  - `not production readiness`;
  - `not production certification`;
  - `not guaranteed replay`;
  - `not production validation`.

## Boundaries

The closure rollup:

- does not execute replay;
- does not perform what-if mutation;
- does not persist reviewer closure rollup data or audit logs server-side;
- does not export, download, upload, or share reviewer closure rollup data;
- does not change routing behavior;
- does not change strategy selection;
- does not recompute scores;
- does not retune weights;
- does not infer hidden scoring;
- does not generate a new fingerprint, hash, SHA, or UUID;
- does not use reflection;
- does not add telemetry or storage;
- does not add external calls, scripts, or CDNs;
- does not add server-side export/PDF/ZIP/file generation.

It is not replay proof, not scoring proof, not correctness validation, not production readiness, not production
certification, not guaranteed replay, and not production validation.

It is not an approval, remediation, enforcement decision, readiness score, scorecard, quality ranking, correctness
validation, production decision, release gate, registry publication proof, signing proof, or governance proof.

## No-Healthy And Stripped Replay Paths

When the normal compare response has no selected candidate, the per-result reviewer closure summary can remain present
as safe `UNKNOWN` metadata. The response-level rollup then reports the available closure-summary count and `UNKNOWN`
or incomplete counts without inventing a selected candidate, candidate set, replay execution, score gap, fingerprint,
or production claim.

Scenario replay keeps its stripped routing-result posture: reviewer metadata fields remain null in embedded replay
results, and this response-level compare rollup is not promoted into scenario replay output as replay proof.
