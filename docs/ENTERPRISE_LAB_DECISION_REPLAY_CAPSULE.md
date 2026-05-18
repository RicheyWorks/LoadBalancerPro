# Enterprise Lab Decision Replay Capsule

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

The Decision Replay Capsule is a read-only canonical evidence packaging lane for `POST /api/routing/compare`.
It packages stable lab evidence already present in the compare result so reviewers can understand what would be
available for later reconstruction. It is not replay execution, not what-if mutation, not persistence, and not an
export/download/share feature.

The capsule is derived from already-built decision vector, dominant factor, decision delta, replay snapshot, and
reconstruction trace data. It does not recompute scores, does not retune weights, does not duplicate
`ServerScoreCalculator`, does not rerun routing, does not mutate server or candidate inputs, and does not change
routing, scoring, strategy, proxy, or API behavior outside the additive response field.

Each routing comparison result can expose an additive `results[].decisionReplayCapsule` object with:

- `capsuleSchemaVersion`, currently `decision-replay-capsule/v1`.
- `status`: `AVAILABLE`, `PARTIAL`, or `UNKNOWN`.
- `selectedCandidateId`, when already returned by compare evidence.
- `candidateIdsConsidered` in deterministic order.
- `candidateCount`.
- `closestAlternativeCandidateId`, `finalScoreGap`, and `largestDeltaFactorName` when already returned as finite or known analysis evidence.
- `linkedReplaySnapshotFingerprint` and `linkedReconstructionTraceFingerprint`, when returned by prior read-only lanes.
- `capsuleFingerprint`, a deterministic local capsule fingerprint derived only from stable capsule fields.
- compact candidate evidence entries with candidate id, finite final score when already available, observed factor names, contribution count, dominant factor names when already available, and safe status.
- compact factor evidence entries with factor name, selected/closest-alternative presence, finite selected and closest-alternative contributions when already available, finite delta when already available, and safe status.
- careful explanation, boundary note, and production not-proven boundary text.

## Fingerprint Boundary

The capsule fingerprint is a deterministic local hash over stable capsule fields. It must not include timestamps, random ids, hostnames, environment variables, file paths, secrets, local usernames, machine-specific data, or network-specific data. It is not cryptographic proof of production behavior, not signing proof, not registry publication proof, and not an audit-log proof.

Repeated equivalent compare evidence should produce the same capsule fingerprint. Different evidence can produce a
different fingerprint. The fingerprint helps reviewers compare lab evidence packages; it does not prove that
production traffic behaved the same way.

## Missing Evidence

If the selected candidate, candidate set, closest alternative, score gap, factor contributions, snapshot fingerprint,
trace fingerprint, candidate evidence, or factor evidence is missing, unavailable, null, empty, partial, or non-finite,
the capsule returns `UNKNOWN` or `PARTIAL` rather than inventing values. It must not invent selected candidates,
candidate sets, closest alternatives, score gaps, largest delta factors, factor values, replay claims, or explanations.
It returns `PARTIAL` when some useful capsule evidence is present but linked evidence, candidate evidence, factor
evidence, score gaps, or fingerprints are incomplete.

Failure/no-healthy-server results remain safe: no selected candidate, closest alternative, factor delta, score gap, or
capsule evidence is fabricated.

## Reviewer Use

Reviewers can use the capsule to see the stable lab evidence package behind one routing comparison result:

- Which candidate ids were considered.
- Which candidate was selected when already returned.
- Which closest alternative and score gap were returned by decision delta analysis.
- Which snapshot and reconstruction trace fingerprints are linked.
- Which candidate evidence and factor evidence were available for later explanation reconstruction.

This is useful for replay-readiness review, but it does not execute replay and does not perform what-if mutation.

## Safety Boundaries

The Decision Replay Capsule is lab explainability/replay-readiness only. It is not production certification, not
live-cloud behavior proof, not real-tenant behavior proof, not SLA/SLO proof, not registry publication proof, not
signing status proof, not governance application proof, not exact production scoring proof, not guaranteed replay,
not production traffic validation, and not production readiness proof.

It does not persist capsules or audit logs server-side. It does not export, download, or share capsules. It adds no
upload/share/download route, telemetry, external calls, CDNs, or server-side export/PDF/ZIP/file generation.
