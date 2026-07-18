# Enterprise Lab Ownership Renewal and Verification Gate Contract

Status: PR3 executable renewal/verification contract; takeover and mutation integration remain later gates.

## One authoritative gate

Each live ownership lease creates exactly one `EnterpriseLabEvidenceOwnershipGate`. The gate retains the live lease; its
constructor is not public, and it cannot create a lock, owner identity, generation, or record. Calling
`ownershipGate()` repeatedly returns that same gate.

Its bounded operations verify without mutation, run one explicitly invoked renewal cycle, or require current ownership
and throw a classified exception when mutation admission is closed.

Later mutation integration must use this gate at meaningful commit boundaries. A cached owner record, timestamp,
generation, process ID, or informal boolean is not mutation authority.

PR3 adds no scheduler. The later lifecycle cadence uses policy; the gate creates no thread, executor, queue, or loop.

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

Tests prove stable gate identity, current-owner verification, generation stability, durable renewal, idempotent same-time
renewal, bounded retry success/exhaustion, write failure, post-install recovery, deadline closure with a still-contending
OS lock, clock regression, owner/generation replacement, missing/corrupt records, directory replacement, unexpected lock
channel closure, clean release after renewal, and permanent gate closure after release or uncertainty.

PR3 does not schedule renewal, acquire at startup, classify stale owners, increment generation, take over evidence,
reconcile journals, or wire mutation paths. It does not prove separate-process, malicious-process, network-filesystem,
multi-host, production-ownership, production-traffic, or production-readiness behavior.
