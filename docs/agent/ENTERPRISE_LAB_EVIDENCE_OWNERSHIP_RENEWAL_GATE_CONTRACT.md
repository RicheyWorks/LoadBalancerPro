# Enterprise Lab Ownership Renewal and Verification Gate Contract

Status: PR6 executable renewal, takeover-reconciliation, mutation-fencing, operator-status, and proof contract.

## One authoritative gate

Each live ownership lease creates exactly one `EnterpriseLabEvidenceOwnershipGate`. The gate retains the live lease; its
constructor is not public, and it cannot create a lock, owner identity, generation, or record. Calling
`ownershipGate()` repeatedly returns that same gate.

Its bounded operations verify without mutation, run one explicitly invoked renewal cycle, or require current ownership
and throw a classified exception when mutation admission is closed.

Mutation-capable repository objects derive a narrow internal authorization from this gate. The authorization binds the
controlled root, owner ID, and generation, and is revalidated at meaningful commit boundaries. A cached owner record,
timestamp, generation, process ID, or informal boolean is not mutation authority.

After startup reconciliation succeeds, the owned operator service starts one daemon scheduled task at the policy's
bounded renewal interval. It has one periodic task, no user queue, no unbounded retry, and no work before ownership is
published. Renewal failure closes experiment admission. Shutdown closes durable writers, stops this task, publishes the
release record, and only then releases the OS lock and channel.

## Mutation boundaries

Fresh application startup acquires ownership before preparing the journal namespace. A prior record enters the bounded
takeover path; while the manager holds the exclusive lock and the exact `TAKEOVER_PENDING` record, only a manager-confined
authorization can run the existing reconciler. The live gate is published only after successful reconciliation.

Journal creation and every append verify the same owner generation before write chunks and synchronization. Startup
reconciliation verifies before admission, recovery append, quarantine, and baseline restoration. Terminal manifest
installation, source deletion, and retention apply verify again at their commit points. The read-only directory factory
cannot create a writer; verification, replay, discovery, dry-run retention, and bounded evidence reads remain non-mutating.

The durable operator service and loopback router verify the same authorization before arm/start/evaluate/cancel state
changes, candidate allocation, baseline restoration, and each loopback request dispatch. Existing journal event bytes and
fingerprint chains are unchanged; owner generation is carried by the non-detachable mutation capability rather than a
journal schema rewrite.

## Verification order

A successful verification proves within one synchronized lease boundary that release is incomplete; the private channel
and non-shared OS `FileLock` remain valid; controlled directory and lock-path identities remain unchanged; and the fixed
record is present, bounded, canonical, and fingerprint-valid. It also requires the durable record to exactly match the
owner, instance, generation, lock, timing, and state in memory, with no clock regression or exceeded deadline.

The record store performs a fresh fixed-path read for every verification. Verification never accepts caller-provided
paths, owner IDs, generation values, record bytes, timestamps, or reason codes.

## Deterministic renewal

Renewal first performs the same authoritative verification. Preflight-only I/O failures may be retried no more than the
policy's hard-bounded renewal attempt count, with only the hard-bounded policy retry delay. Lock loss, path replacement,
record replacement, fingerprint failure, generation change, clock regression, and deadline failure are not retried.

After preflight, renewal preserves owner, instance, generation, acquisition, prior-owner, takeover, reconciliation, and
release evidence. It advances renewal/expiration from the verified clock, fingerprints the canonical record, forces a
fixed temporary file, atomically replaces and forces installed evidence and supported POSIX directory metadata, exactly
re-reads it, then repeats authoritative verification before success.

A renewal invoked at the already durable instant is an idempotent success and performs no rewrite. A clock value before
the durable instant fails closed as `CLOCK_REGRESSION`. A renewal at the exact expiration instant may extend the lease;
a time after expiration fails as `RENEWAL_DEADLINE_EXCEEDED`.

Durable-write failures are not blindly retried because the fixed temporary file is preserved as interrupted-operation
evidence. If atomic installation completed before status publication failed, exact read-back of the intended canonical
record may recover success.

## Fail-closed lifetime

The first unverifiable condition latches one classified terminal failure for that lease's gate. Later verification,
renewal, and required-admission calls return or throw that same failure even if external bytes or the clock appear to be
restored. This prevents transient repair from silently reopening mutation admission.

The gate does not release or delete the lock when it closes admission. A valid OS lock continues excluding another local
process even after the record's timestamp expires. Normal release may still publish a durable `RELEASED` record when the
expected record and directory remain authoritative. If they do not, release closes the OS resources and reports failure
without overwriting conflicting evidence.

Valid evidence succeeds, and same-instant renewal is idempotent. Exhausted preflight I/O latches `IO_FAILURE`; lock loss
latches `LOCK_LOST`; identity, record, generation, canonical-read, clock, and deadline failures retain their exact class.
Deadline failure retains a valid lock. A completed install is recovered only by exact intended-record read-back, while a
pre-install write failure preserves temporary evidence and closes the gate.

## Evidence and scope boundary

Tests prove stable gate identity, current-owner verification, generation stability, durable periodic renewal, bounded
retry success/exhaustion, deadline closure, startup live-owner denial, clean-release takeover, read-only writer denial,
append generation fencing, reconciliation preflight, router fencing, and preservation during denied compaction and
retention apply. Existing journal compatibility tests remain green.

PR6 adds two operations to the existing authenticated Enterprise Lab operator boundary:

- `GET /api/lab/experiments/durable/ownership` returns a bounded immutable snapshot of the held owner reference,
  generation, state, acquisition/renewal/expiration times, lock validity, takeover/reconciliation/release summary,
  admission state, last scheduled-renewal result when present, and record fingerprints.
- `POST /api/lab/experiments/durable/ownership/verify` performs the existing authoritative verification and returns the
  same snapshot with its structured result. A failure closes mutation admission.

Both routes use the existing API-key and OAuth2 operator-role rules. They expose no path, PID, host fingerprint,
directory or lock-file identity, file handle, raw record/lock bytes, release action, forced unlock, owner/generation
override, or takeover action. An unconfigured durable repository returns the existing bounded conflict response.

`scripts/smoke/enterprise-lab-ownership-proof.ps1` runs the packaged
`--enterprise-lab-ownership-proof` command. The foreground parent derives fresh roots only beneath `target/` and starts
truly separate child JVMs. The executable proof covers live-owner refusal, owner append/reconciliation, non-owner
append/compaction/retention/experiment/allocation refusal, bounded renewal, clean release and higher-generation handoff,
forced process loss, expired stale classification, higher-generation reconciled takeover, journal verification/replay,
interrupted rollback, process-local baseline verification, repeated restart idempotency, simultaneous acquisition, and
a competing takeover race with one winner. Child actions and case names are fixed; callers cannot supply an evidence
root, owner identity, generation, unlock, release, or takeover decision.

The proof is single-host, local-filesystem, literal-loopback, target-only evidence. It does not prove malicious-process
resistance, network-filesystem correctness, multi-host or distributed fencing, durable external allocation state,
production ownership, production traffic, or production readiness. Abrupt termination can prove journal recovery and
that the restarted process-local router is baseline-safe; it does not claim an external allocation supervisor exists.
