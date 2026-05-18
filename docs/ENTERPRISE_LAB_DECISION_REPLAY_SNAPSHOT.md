# Enterprise Lab Decision Replay Snapshot

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

Decision Replay Snapshot is a read-only evidence lane for `POST /api/routing/compare`. It captures a deterministic, structured summary of the lab decision evidence that was already produced for one routing comparison result. It is the evidence foundation for future replay and what-if lanes, but it is not replay execution and it is not what-if mutation.

The snapshot is derived from existing compare response evidence:

- result strategy id, selected candidate id, and candidate ids already returned by the comparison result;
- `results[].decisionVector` availability and candidate ids;
- `results[].dominantFactorAnalysis` status;
- `results[].decisionDeltaAnalysis` status, closest alternative id, final score gap, and largest delta factor when available.

It does not persist data, write audit logs, replay traffic, mutate inputs, rerun scoring, does not recompute scores from raw server fields, does not duplicate `ServerScoreCalculator`, retune weights, change routing behavior, change server selection, mutate proxy behavior, or add telemetry.

## What It Adds

Each routing comparison result can expose an additive `results[].decisionReplaySnapshot` object with:

- `snapshotSchemaVersion`;
- `status` such as `AVAILABLE`, `PARTIAL`, or `UNKNOWN`;
- deterministic `snapshotFingerprint`;
- selected candidate id when returned;
- deterministically ordered candidate ids considered;
- candidate count derived from those candidate ids;
- strategy id;
- Decision Vector, Dominant Factor Analysis, and Decision Delta Analysis statuses;
- closest alternative id when returned by Decision Delta Analysis and present in candidate evidence;
- final score gap when returned as a finite value by Decision Delta Analysis;
- largest delta factor name when returned by Decision Delta Analysis;
- careful explanation and boundary text.

The snapshot uses only stable snapshot fields for the fingerprint. It must not include timestamps, random ids, hostnames, environment variables, file paths, secrets, local usernames, or machine-specific data.

## Fingerprint Boundary

The fingerprint is a deterministic local hash over stable snapshot fields. It helps reviewers compare repeated equivalent lab responses and detect whether the captured evidence summary changed.

The fingerprint is not a cryptographic proof of production behavior, not a signing feature, not an audit-log commitment, not a registry publication proof, and not evidence that production traffic followed the same path.

## Empty, Partial, Or Missing Data

If selected candidate evidence, candidate ids, or Decision Vector evidence is unavailable, the snapshot returns `UNKNOWN` and does not invent candidate, alternative, score gap, or factor evidence.

If the selected candidate and candidate ids are available but an analysis lane is missing, unknown, partial, or the final score gap is absent or non-finite, the snapshot returns `PARTIAL` and keeps unavailable values null or unknown.

Candidate ids are ordered deterministically. Closest alternative ids are accepted only when returned by Decision Delta Analysis and present in the candidate evidence.

## Reviewer Use

Reviewers can use the snapshot to answer narrow lab-readiness questions:

- What exact structured lab evidence was available for this comparison result?
- Which candidate ids were summarized in a stable order?
- Which selected-vs-closest-alternative fields were available?
- Did repeated equivalent compare responses produce the same local snapshot fingerprint?
- Which replay-readiness fields are still unknown or partial?

This gives future replay and one-signal what-if work a safer input shape without implementing those workflows yet.

## Safety Boundaries

Decision Replay Snapshot is read-only lab explainability only. It does not change routing behavior, scoring math, strategy weights, server selection logic, proxy behavior, existing API fields, app configuration, or dependencies.

It does not execute replay, does not perform what-if mutation, does not persist audit logs, does not add upload/share/download routes, does not add server-side PDF/ZIP/file export generation, does not add live telemetry, does not add external calls, and does not add CDNs.

It does not prove production certification, live-cloud behavior, real-tenant behavior, SLA/SLO achievement, registry publication, signing status, governance application, production traffic validation, exact production scoring, or production readiness.
