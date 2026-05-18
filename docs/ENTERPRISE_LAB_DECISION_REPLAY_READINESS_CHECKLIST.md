# Enterprise Lab Decision Replay Readiness Checklist

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

The Decision Replay Readiness Checklist is a read-only lab evidence readiness lane for `POST /api/routing/compare`.
It summarizes whether already-built compare evidence is present for later reviewer analysis. It does not execute
replay, does not perform what-if mutation, does not persist checklist state or audit logs, and does not export,
download, or share checklist data.

The checklist is derived only from already-built Decision Vector, Dominant Factor Analysis, Decision Delta Analysis,
Decision Replay Snapshot, Decision Replay Reconstruction Trace, and Decision Replay Capsule data. It does not
recompute scores, does not retune weights, does not duplicate `ServerScoreCalculator`, does not rerun routing,
does not mutate server or candidate inputs, and does not change routing, scoring, strategy, proxy, or API behavior
outside the additive response field.

It does not recompute scores and does not retune weights. It does not export, download, or share checklist data.

Each routing comparison result can expose an additive `results[].decisionReplayReadinessChecklist` object with:

- `checklistSchemaVersion`, currently `decision-replay-readiness-checklist/v1`.
- `status`: `AVAILABLE`, `PARTIAL`, or `UNKNOWN`.
- selected candidate id and candidate count only when already available from prior compare evidence.
- linked replay snapshot, reconstruction trace, and replay capsule fingerprints only when the source evidence is already available.
- source evidence statuses for Decision Vector, Dominant Factor Analysis, Decision Delta Analysis, Replay Snapshot,
  Reconstruction Trace, and Replay Capsule.
- deterministic checklist item counts for available, partial, and unknown items.
- deterministic checklist items with item id, label, status, source field path, explanation, and missing evidence reason.
- careful explanation, boundary note, and production not-proven boundary text.

## Checklist Items

The checklist uses stable item ordering:

1. `decision-vector-evidence`
2. `dominant-factor-evidence`
3. `decision-delta-evidence`
4. `replay-snapshot-evidence`
5. `reconstruction-trace-evidence`
6. `replay-capsule-evidence`
7. `candidate-evidence`
8. `factor-evidence`
9. `read-only-boundary-evidence`

Items return `UNKNOWN` when source evidence is absent, `PARTIAL` when useful but incomplete evidence exists, and
`AVAILABLE` only when the relevant already-built evidence is present. Missing selected candidates, candidate sets,
candidate final scores, factor contribution status, analysis status, or fingerprints stay missing instead of being
invented.

The checklist returns `PARTIAL` when useful evidence exists but one or more checklist items remain incomplete.

## Fingerprint Boundary

The readiness checklist does not create a new proof fingerprint. It links existing deterministic local snapshot,
trace, and capsule fingerprints only when those source lanes are already available or partial. It does not include
timestamps, random ids, hostnames, environment variables, file paths, secrets, local usernames, machine-specific
data, or network-specific data.

Linked fingerprints are local lab comparison aids. They are not cryptographic proof of production behavior, not
signing proof, not registry publication proof, not production traffic validation, and not audit-log proof.

## Missing Evidence

If selected candidate, candidate set, Decision Vector, final scores, factor contribution evidence, analysis status,
snapshot fingerprint, trace fingerprint, or capsule fingerprint is missing, unavailable, null, empty, partial, or
non-finite, the checklist returns `UNKNOWN` or `PARTIAL` rather than inventing values.

Failure/no-healthy-server results remain safe: no selected candidate, candidate set, alternative candidate, score
gap, largest delta factor, replay claim, readiness claim, or explanation evidence is fabricated.

## Reviewer Use

Reviewers can use the checklist to see whether one routing comparison result has the lab evidence needed for later
review of:

- Decision Vector presence.
- Selected candidate and candidate set presence.
- Candidate final score presence when already returned.
- Factor contribution status.
- Dominant factor and decision delta analysis status.
- Linked snapshot, trace, and capsule fingerprints.

This is useful for lab replay-readiness review, but it does not execute replay and does not perform what-if mutation.

## Safety Boundaries

The Decision Replay Readiness Checklist is lab explainability/replay-readiness only. It is not production
certification, not live-cloud behavior proof, not real-tenant behavior proof, not SLA/SLO proof, not registry
publication proof, not signing status proof, not governance application proof, not exact production scoring proof,
not guaranteed replay, not production traffic validation, and not production readiness proof.

No production certification, live-cloud behavior, real-tenant behavior, SLA/SLO, registry publication, signing status,
governance application, exact production scoring, or guaranteed replay is claimed.

It does not persist checklist state or audit logs server-side. It does not export, download, or share checklist
data. It adds no upload/share/download route, telemetry, external calls, CDNs, or server-side export/PDF/ZIP/file
generation.
