# Enterprise Lab Durable Evidence Contract

This contract describes the implemented single-process, local-filesystem experiment journal. It is a bounded Enterprise
Lab recovery mechanism, not a production audit service, signer, distributed log, database, or disaster-recovery guarantee.

## Enablement and ownership

Durable evidence is opt-in through:

`loadbalancer.enterprise-lab.experiment-journal-data-directory`

The value must identify an existing absolute local directory. Startup rejects relative paths, filesystem roots, UNC paths,
symbolic-link traversal, and surrounding whitespace. API callers never provide a path or journal filename. Blank
configuration retains the legacy process-local service and makes durable endpoints report
`DURABLE_EVIDENCE_NOT_CONFIGURED`.

Installed-allocation authority is selected once at startup through
`loadbalancer.enterprise-lab.allocation-supervisor-mode`. The exact supported values are `in-process` (the default
local/test-compatible holder), `external-supervisor-required`, and `disabled`. External-required mode also requires this
durable evidence root, one repository-controlled loopback scenario, and an already ready supervisor beneath the same
trusted root. Startup fails closed if any of those requirements or the authenticated ownership handoff cannot be
verified; it never falls back to the in-process holder. Disabled mode keeps allocation mutation and experiment admission
closed. These modes affect only the bounded Enterprise Lab loopback allocation holder; they do not enable production
routing, external targets, public supervisor listeners, multi-host coordination, or production-readiness claims.

The configured directory contains one versioned namespace with controlled `journals`, `quarantine`, and `compacted`
children. Experiment IDs are bounded canonical identifiers and are SHA-256-hashed before filename construction. One
process-local writer owns an active journal; a second writer, reader, verifier, or compactor cannot claim it concurrently.
This is not a cross-process lease.

## Live append boundary

The durable repository projects already accepted facts from `EnterpriseLabExperimentOperatorService`; it does not own
traffic and does not implement a second lifecycle. Arm, candidate application, start, observation progress, hold evaluation,
completion, cancellation, rollback, restoration, rejection, failure, and recovery facts use the existing canonical event,
codec, verifier, replay payload, lifecycle states, allocation snapshots, and fingerprint rules.

The default writer opens with `FORCE_DATA_AND_METADATA`. Each complete canonical JSON frame is appended, checked against
the current writer-owned chain, and synchronized with `FileChannel.force(true)` before the operator action returns. The
implementation therefore proves that the Java append and force calls succeeded on the exercised local filesystem. It does
not prove drive firmware behavior, power-loss survival on every filesystem, remote-filesystem semantics, or a full
operating-system crash matrix.

If an append, entry bound, journal bound, or synchronization check fails, the recovery gate becomes failed. An active
process-local candidate allocation is returned through the existing baseline restoration and rollback path before the
service throws a bounded fail-closed error. No persistence retry loop, background queue, watcher, or automatic resume is
introduced.

## Verification, replay, and restart

Startup discovery is capped at 256 controlled journal entries. Identity is trusted only when a strict canonical genesis
frame hashes to the controlled filename. Exact verification runs before replay or allocation inspection. Replay is pure,
bounded, deterministic, idempotent, and sends no traffic.

An interrupted running or holding experiment never resumes candidate traffic. Startup reconstructs the existing lifecycle,
inspects the repository-approved loopback allocation adapter, returns to the recorded baseline, appends recovery evidence,
and terminalizes the experiment. Armed work is safely cancelled. Interrupted rollback and completion are continued only
through their existing verified restoration paths. A second startup preserves the resulting terminal record without a
second traffic action.

Malformed, unsupported, truncated, identity-mismatched, or corrupted evidence is never skipped. The original bytes are
atomically moved to the controlled quarantine namespace when possible, exposed only as bounded opaque metadata, and kept as
unresolved evidence. Admission remains failed when recovery cannot prove a safe result.

## Terminal compaction and retention

Only an exactly valid journal whose deterministic replay contains a terminal record may be compacted. Active, running,
holding, completing, rolling-back, unavailable, corrupted, or semantically rejected journals are preserved and rejected.

The terminal manifest is strict canonical JSON with schema
`enterprise-lab-terminal-manifest/v1`. It records:

- controlled journal and experiment identity;
- scenario, configuration, decision, baseline, candidate, and applied-allocation fingerprints;
- terminal lifecycle, rollback, and restoration outcome;
- source entry count and byte count;
- source terminal sequence and fingerprint;
- reconstructed-state fingerprint and terminal time;
- compaction time, reason code, and manifest fingerprint.

Compaction writes a bounded temporary manifest, restricts permissions where supported, force-synchronizes the file, and
requires an atomic move. If atomic installation is unavailable, compaction fails and preserves the source. The installed
manifest is strictly decoded, canonical-byte compared, fingerprint verified, and identity compared before the exact
controlled source file is removed. A crash between installation and source cleanup leaves both pieces and is safe to retry.
Repeated compaction reuses the verified manifest.

Retention accepts only a maximum terminal-journal count from 0 through 256. Dry-run reports the oldest terminal candidates
without mutation. Apply compacts only the excess exactly valid terminal journals. Active journals are not counted as
terminal candidates, and invalid or unresolved evidence is reported and retained. Quarantine is never deleted by retention.

## Operator evidence API

The existing API-key and OAuth2 operator-role rules for `/api/lab/**` protect all durable endpoints:

- `GET /api/lab/experiments/durable`
- `GET /api/lab/experiments/durable/recovery`
- `GET|POST /api/lab/experiments/durable/{experimentId}/verification|verify`
- `GET /api/lab/experiments/durable/{experimentId}/export`
- `POST /api/lab/experiments/durable/{experimentId}/compact`
- `GET /api/lab/experiments/durable/compacted`
- `GET /api/lab/experiments/durable/quarantine`
- `POST /api/lab/experiments/durable/retention`

Responses are bounded structured summaries, verification findings, reconstructed evidence, manifests, recovery status, or
opaque quarantine metadata. The API accepts bounded experiment IDs and retention values only. It never accepts or returns a
filesystem path, arbitrary journal name, raw corrupt content, source bytes, secret, credential, stack trace, deletion
request, or unrestricted history mutation.

## Packaged proof

Run:

```powershell
.\scripts\smoke\enterprise-lab-durable-recovery-proof.ps1 -Package
```

The shipped-JAR command is `--enterprise-lab-durable-recovery-proof`, with an optional target-only
`--enterprise-lab-durable-recovery-output=...`. It starts three literal `127.0.0.1` ephemeral backends, sends bounded real
HTTP requests, records live events, simulates process interruption at the closed-writer boundary, reconciles twice, proves
normal completion and rollback across restart, quarantines middle corruption and a partial tail, retains unresolved
quarantine evidence, rejects active compaction, and verifies terminal compaction. JSON and Markdown evidence remain beneath
ignored `target/` output.

The proof does not simulate an operating-system kill, power loss, disk firmware loss, multi-process writer race, network
filesystem, cloud store, tenant traffic, production traffic, signing identity, non-repudiation, tamper-proof storage,
throughput, p95/p99 production performance, load/stress capacity, production readiness, or deployment certification.

## Operator run examples

The configured root must already exist. Keep it outside the source tree and grant it only to the application identity. A
local PowerShell launch can use:

```powershell
$journalRoot = (Resolve-Path -LiteralPath 'C:\local-lab\experiment-evidence').Path
java -jar target\LoadBalancerPro-2.5.0.jar `
  "--loadbalancer.enterprise-lab.experiment-journal-data-directory=$journalRoot"
```

This is a process configuration value, not an API request field. The startup reconciler completes before the service bean
is returned. An unsafe or unresolved report keeps experiment admission closed even when authentication succeeds.

With the repository's API-key mode, a bounded inventory request is:

```powershell
Invoke-RestMethod `
  -Uri 'http://127.0.0.1:8080/api/lab/experiments/durable' `
  -Headers @{ 'X-API-Key' = '<API_KEY>' }
```

The response contains a count, a maximum count, an evidence-boundary statement, and sanitized journal summaries. It never
contains the configured root. Use the existing OAuth2 bearer flow instead when OAuth2 mode is configured; viewer-only roles
remain forbidden.

Verification is read-only:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri 'http://127.0.0.1:8080/api/lab/experiments/durable/<EXPERIMENT_ID>/verify' `
  -Headers @{ 'X-API-Key' = '<API_KEY>' }
```

Substitute only a bounded canonical experiment ID; do not substitute a filename or path. The response reports outcome,
classification, complete/tail/total byte
counts, verified event count, and bounded findings without returning source frames.

Retention should normally be inspected first:

```powershell
$body = @{ maximumTerminalJournals = 64; dryRun = $true } | ConvertTo-Json
Invoke-RestMethod `
  -Method Post `
  -Uri 'http://127.0.0.1:8080/api/lab/experiments/durable/retention' `
  -ContentType 'application/json' `
  -Headers @{ 'X-API-Key' = '<API_KEY>' } `
  -Body $body
```

After reviewing the returned action list, send the same request with `dryRun=false` to compact only the reported excess
terminal journals. A maximum of zero means compact every eligible terminal journal; it does not remove active, corrupt, or
quarantined evidence. There is no retention API that deletes a caller-selected file.

One terminal experiment can be compacted explicitly:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri 'http://127.0.0.1:8080/api/lab/experiments/durable/<EXPERIMENT_ID>/compact' `
  -Headers @{ 'X-API-Key' = '<API_KEY>' }
```

A successful result includes `COMPACTED` or `COMPLETED_IDEMPOTENTLY`, the verified manifest, and a true source-removal
indicator. An active writer, non-terminal replay, invalid source, unsupported source, identity mismatch, or unavailable
atomic installation returns a bounded rejection and leaves required source evidence in place.

## Failure and action matrix

| Observed boundary | Mutation permitted | Admission result | Evidence result |
| --- | --- | --- | --- |
| Valid terminal journal, allocation at baseline | None during startup | Ready | Terminal record preserved |
| Valid terminal journal, allocation drifted | Verified loopback baseline restoration | Ready only after verification | Recovery action appended |
| Armed journal, baseline current | No traffic; terminal cancellation | Ready | Cancellation appended |
| Running or holding journal | Baseline restoration or verified no-op | Ready only after terminal rollback | Recovery rollback appended |
| Completing journal | Continue baseline restoration only | Ready only after terminal completion | Recovery completion appended |
| Rolling-back journal | Idempotent baseline restoration only | Ready only after terminal rollback | Recovery result appended |
| Truncated final frame | Quarantine move only | Failed closed | Original bytes retained |
| Middle corruption or fingerprint mismatch | Quarantine move only | Failed closed | Original bytes retained |
| Unsupported schema or malformed canonical JSON | Quarantine move only | Failed closed | Original bytes retained |
| Restoration cannot be verified | Bounded restoration attempt | Failed closed | Failure remains in journal |
| Active writer during verification or compaction | None | Existing gate state retained | `UNAVAILABLE` or action rejection |
| Append or sync failure after candidate application | Process-local baseline restoration | Failed closed | Original append failure retained |
| Atomic manifest move unavailable | None | Existing gate state retained | Source and synchronized temporary evidence preserved |
| Retention sees unresolved evidence | None for that evidence | Existing gate state retained | Unresolved count reported |

## Verification checklist

For a local change affecting this boundary, verify in this order:

1. Run focused codec, local journal, verifier, replay, startup reconciler, durable repository, operator, controller, API-key,
   OAuth2, and proof-command tests.
2. Run `mvn -q test` and confirm Surefire reports zero failures, errors, and skips.
3. Run `mvn -q "-DskipTests" package`, then `mvn -B package` at the exact candidate head.
4. Run the existing completion/rollback proof and the durable recovery proof against the packaged JAR.
5. Verify embedded Tomcat resolution, JaCoCo output, CycloneDX SBOM generation, the executable JAR, and ignored target-only
   proof output.
6. Inspect the diff for paths, non-loopback URLs, secret-like values, generated journal evidence, unbounded collections,
   unbounded retries, background executors, and dependency changes.
7. Require exact-head CI, CodeQL, dependency review, container runtime smoke, and Trivy results before merge.
8. Merge normally, preserve the source branch, and require exact-merge main CI and CodeQL before declaring completion.

The packaged proof report should show `ROLLED_BACK` for the interrupted case, `COMPLETED` for normal completion,
`ROLLED_BACK` for normal cancellation, and true values for both recovery passes, corruption quarantine, partial-tail
quarantine, unresolved retention, active-compaction rejection, and terminal-compaction verification. Generated files belong
under ignored `target/`; they are verification output and must not be committed.

## Operational interpretation

`VALID` and `RECONSTRUCTED` mean the bounded bytes and supported semantics passed the implemented checks. They do not prove
who wrote the bytes. A SHA-256 content or chain fingerprint detects covered content changes; it is not a signature.

`READY` means the current startup reconciliation found no unresolved local experiment that prevents new Enterprise Lab
admission. It does not mean a production service, cloud target, real tenant, or multi-process owner is ready.

`COMPACTED` means a verified terminal source was represented by an independently verified terminal manifest before source
cleanup. It does not mean every historical event remains available as a raw frame after retention.

`QUARANTINED` means the original controlled bytes were preserved outside the active journal namespace. It is deliberately a
failed-closed result until an operator investigates; retention cannot convert it into a success or delete it.
