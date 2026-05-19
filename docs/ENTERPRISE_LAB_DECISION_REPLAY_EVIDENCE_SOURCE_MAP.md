# Enterprise Lab Decision Replay Evidence Source Map

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

The Decision Replay Evidence Source Map is a read-only lab source-mapping lane for `POST /api/routing/compare`.
It explains which already-built compare evidence fields support the current replay/readiness artifacts. It does not execute replay, does not perform what-if mutation, does not persist source-map data or audit logs, does not generate a new fingerprint, and does not export, download, or share source-map data.

The source map is derived only from already-built `decisionVector`, `dominantFactorAnalysis`,
`decisionDeltaAnalysis`, `decisionReplaySnapshot`, `decisionReplayReconstructionTrace`, `decisionReplayCapsule`,
and `decisionReplayReadinessChecklist` response objects. It does not derive from raw server input, does not recompute scores, does not infer hidden scoring, does not retune weights, does not duplicate `ServerScoreCalculator`, does not rerun routing, does not mutate candidate inputs, and does not change routing, scoring, strategy, proxy, dependency, app config, or API behavior outside the additive response field.

Each routing comparison result can expose an additive `results[].decisionReplayEvidenceSourceMap` object with:

- `sourceMapSchemaVersion`, currently `decision-replay-evidence-source-map/v1`.
- `status`: `AVAILABLE`, `PARTIAL`, or `UNKNOWN`.
- selected candidate id and candidate count only when already available from prior compare evidence.
- linked replay snapshot, reconstruction trace, and replay capsule fingerprints only when already available and the linked source status is not `UNKNOWN`.
- normalized source statuses for Decision Vector, Dominant Factor Analysis, Decision Delta Analysis, Replay Snapshot,
  Reconstruction Trace, Replay Capsule, and Replay Readiness Checklist.
- deterministic source-map entries with source id, label, status, source field path, downstream evidence field paths,
  optional linked fingerprint, evidence summary, and boundary note.
- careful explanation, boundary note, and production not-proven boundary text.

## Source Map Entries

The source map uses stable entry ordering:

1. `decision-vector-source`
2. `dominant-factor-analysis-source`
3. `decision-delta-analysis-source`
4. `replay-snapshot-source`
5. `reconstruction-trace-source`
6. `replay-capsule-source`
7. `readiness-checklist-source`
8. `linked-fingerprint-source`
9. `read-only-boundary-source`

Entries return `UNKNOWN` when source evidence is absent, `PARTIAL` when useful but incomplete evidence exists, and
`AVAILABLE` only when the relevant already-built evidence is present. Missing selected candidates, candidate sets,
alternative candidates, score gaps, largest delta factors, factor values, analysis status, or fingerprints stay missing
instead of being invented.

The source map returns `PARTIAL` when useful evidence exists but one or more source entries remain incomplete.

## Fingerprint Boundary

The evidence source map does not create a new proof fingerprint. It links existing deterministic local snapshot,
trace, and capsule fingerprints only when those source lanes are already available or partial. It does not use
SHA-256, `MessageDigest`, timestamps, random ids, hostnames, environment variables, file paths, secrets, local
usernames, machine-specific data, or network-specific data.

Linked fingerprints are local lab comparison aids. They are not cryptographic proof of production behavior, not
signing proof, not registry publication proof, not production traffic validation, and not audit-log proof.

## Missing Evidence

If selected candidate, candidate set, Decision Vector, analysis status, snapshot fingerprint, trace fingerprint,
capsule fingerprint, or readiness checklist status is missing, unavailable, null, empty, partial, or non-finite, the
source map returns `UNKNOWN` or `PARTIAL` rather than inventing values.

Failure/no-healthy-server results remain safe: no selected candidate, candidate set, alternative candidate, score
gap, largest delta factor, factor values, replay claim, guaranteed replay claim, production certification claim, or
explanation evidence is fabricated.

## Reviewer Use

Reviewers can use the source map to see how one routing comparison result connects:

- Decision Vector evidence to downstream dominant factor, decision delta, snapshot, trace, capsule, and readiness data.
- Dominant factor and decision delta statuses to replay/readiness artifacts.
- Replay snapshot and reconstruction trace fingerprints to downstream trace, capsule, and readiness fields.
- Replay capsule status and fingerprint to readiness fields.
- Replay Readiness Checklist status to the source-map status summary.

This is useful for lab explainability and reviewer trust, but it does not execute replay and does not perform what-if
mutation.

## Safety Boundaries

The Decision Replay Evidence Source Map is lab explainability/replay-readiness only. It is not production
certification, not live-cloud behavior proof, not real-tenant behavior proof, not SLA/SLO proof, not registry
publication proof, not signing status proof, not governance application proof, not exact production scoring proof,
not cryptographic production proof, not guaranteed replay, not production traffic validation, and not production
readiness proof.

No production certification, live-cloud behavior, real-tenant behavior, SLA/SLO, registry publication, signing status,
governance application, exact production scoring, cryptographic production proof, or guaranteed replay is claimed.

It does not persist source-map data or audit logs server-side. It does not export, download, or share source-map data. It adds no upload/share/download route, telemetry, external calls, CDNs, or server-side export/PDF/ZIP/file generation.
