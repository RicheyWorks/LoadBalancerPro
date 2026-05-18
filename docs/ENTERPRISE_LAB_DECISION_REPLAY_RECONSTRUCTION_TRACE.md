# Enterprise Lab Decision Replay Reconstruction Trace

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

Decision Replay Reconstruction Trace is a read-only reconstruction evidence lane for `POST /api/routing/compare`. It shows which already-returned lab evidence fields are available to reconstruct the decision explanation later. It is derived from existing routing compare lab evidence; it is not replay execution, not what-if mutation, and not audit-log persistence.

The trace is derived from already-built compare response evidence:

- selected candidate id when returned;
- deterministically ordered candidate ids considered;
- candidate final scores already returned in `results[].scores` when finite;
- `results[].decisionVector` availability and candidate factor contribution availability;
- `results[].dominantFactorAnalysis` status;
- `results[].decisionDeltaAnalysis` status, closest alternative id, final score gap, and largest delta factor when available;
- `results[].decisionReplaySnapshot` status and snapshot fingerprint when returned.

It does not persist traces or audit logs server-side, execute replay, execute a stored decision, mutate candidate/server inputs, perform what-if mutation, rerun routing, does not recompute scores from raw server fields, does not duplicate `ServerScoreCalculator`, does not retune weights, change routing behavior, change server selection, mutate proxy behavior, add telemetry, or add upload/share/download/export/PDF/ZIP functionality.

## What It Adds

Each routing comparison result can expose an additive `results[].decisionReplayReconstructionTrace` object with:

- `traceSchemaVersion`;
- `status` such as `AVAILABLE`, `PARTIAL`, or `UNKNOWN`;
- deterministic `traceFingerprint`;
- linked `snapshotFingerprint` when the replay snapshot returned one;
- selected candidate id when returned;
- deterministically ordered candidate ids considered;
- finite candidate final scores already present in the compare result;
- strategy id;
- Decision Vector, factor contribution, Dominant Factor Analysis, Decision Delta Analysis, and Decision Replay Snapshot statuses;
- closest alternative id when already returned by Decision Delta Analysis or the snapshot and present in candidate evidence;
- final score gap when already returned as a finite value;
- largest delta factor name when already returned;
- deterministic reconstruction steps;
- careful explanation and boundary text.

Reconstruction steps are deterministic and use stable ids such as:

- `candidate-set-observed`;
- `selected-candidate-observed`;
- `candidate-final-scores-observed`;
- `decision-vector-observed`;
- `candidate-factor-contributions-observed`;
- `dominant-factors-observed`;
- `closest-alternative-observed`;
- `selected-vs-alternative-delta-observed`;
- `replay-snapshot-fingerprint-observed`.

Each step includes a step id, status, evidence source field path, deterministic explanation, and missing evidence reason when applicable.

## Fingerprint Boundary

The trace fingerprint is a deterministic local trace fingerprint over stable reconstruction fields and step status data. It helps reviewers compare repeated equivalent lab responses and detect whether the reconstruction evidence changed.

The fingerprint must not include timestamps, random ids, hostnames, environment variables, file paths, secrets, local usernames, machine-specific data, or network-specific data.

The fingerprint is not cryptographic proof of production behavior, not a signing feature, not a registry publication proof, not an audit-log commitment, not production traffic validation, and not evidence that production traffic followed the same path.

## Empty, Partial, Or Missing Data

If selected candidate evidence, candidate ids, and Decision Vector evidence are unavailable, the trace returns `UNKNOWN` and does not invent a selected candidate, closest alternative, score gap, largest delta factor, factor values, or explanation evidence.

If useful evidence is present but candidate scores, factor contributions, analysis statuses, final score gap, closest alternative, or snapshot fingerprint evidence is missing, empty, partial, null, or non-finite, the trace returns `PARTIAL` and keeps unavailable values null or unknown instead of inventing zero values.

Candidate ids and reconstruction steps are ordered deterministically. Candidate final scores are included only when they were already returned as finite values.

## Reviewer Use

Reviewers can use the trace to answer narrow lab-readiness questions:

- Which evidence fields are available to reconstruct the decision explanation later?
- Are candidate ids and finite scores present in a deterministic order?
- Did the response include factor contribution evidence for candidate vectors?
- Was closest-alternative delta evidence available?
- Was a Decision Replay Snapshot fingerprint available to link the trace to the snapshot?
- Did repeated equivalent compare responses produce the same local trace fingerprint?

This gives future replay and one-signal what-if work a clearer evidence checklist without implementing those workflows.

## Safety Boundaries

Decision Replay Reconstruction Trace is read-only lab explainability/replay-readiness only. It does not change routing behavior, scoring math, strategy weights, server selection logic, proxy behavior, existing API fields, app configuration, dependencies, or `ServerScoreCalculator` behavior.

It does not execute replay, does not perform what-if mutation, does not persist traces or audit logs server-side, does not add upload/share/download routes, does not add server-side PDF/ZIP/file export generation, does not add live telemetry, does not add external calls, and does not add CDNs.

It does not prove production certification, live-cloud behavior, real-tenant behavior, SLA/SLO achievement, registry publication, signing status, governance application, exact production scoring, production traffic validation, guaranteed replay, cryptographic production proof, or production readiness.
