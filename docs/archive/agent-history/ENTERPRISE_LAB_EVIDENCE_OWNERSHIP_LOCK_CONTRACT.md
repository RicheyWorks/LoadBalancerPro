# Enterprise Lab Single-Host Lock and Owner-Record Contract

Status: PR2 executable acquisition/release contract. Renewal, takeover, startup integration, and mutation fencing are
separate later gates.

## Authority and sequence

The operating-system-backed exclusive JDK `FileLock` is the single-host exclusion authority. The owner record is bounded
audit and recovery evidence; it never substitutes for the live lock.

Initial acquisition uses this order:

1. Validate the explicit absolute local evidence root and fixed ownership paths.
2. Reserve the directory identity in the current JVM to reject duplicate local acquisition.
3. Open, but never delete, the fixed `owner.lock` regular file with symbolic-link following disabled.
4. Attempt a non-shared `tryLock` no more than the configured bounded attempt count.
5. Prove that reopening the controlled lock path overlaps the held lock; a newly lockable path means replacement.
6. Derive the lock-file identity only while the exclusive lock remains valid.
7. Refuse any prior owner record until the separately scoped takeover path evaluates its generation and history.
8. Build generation 1 with an internally generated owner and application-instance identity.
9. Write the canonical bytes to the fixed temporary record, force data and metadata, and atomically install the record.
10. Force the installed record; on POSIX storage, also force the ownership-directory metadata.
11. Re-read and exactly verify schema, fields, fingerprint, directory identity, and canonical bytes.
12. Publish a live ownership resource only while both the lock object and its channel remain valid.

The public acquisition method accepts a trusted root, bounded policy, and injectable clock. It does not accept an owner
ID, application-instance ID, process ID, hostname, generation, filename, lock mode, or takeover instruction. Test-only
package seams inject deterministic identities, failures, and lock-provider behavior without creating an API surface.

## Resource lifetime

The returned final `AutoCloseable` resource retains the private `FileLock` and `FileChannel`. Neither handle is exposed.
Successful acquisition evidence cannot be returned without that resource, and the resource cannot be constructed without
an open channel plus a valid exclusive lock.

The process-local reservation remains held for the same lifetime. A second request in the JVM receives a structured
duplicate-acquisition refusal; a different JVM holding the OS lock causes bounded `tryLock` attempts to return a live-
owner refusal. No lock call waits indefinitely and no unbounded executor, queue, retry, or sleep is introduced.

## Durable publication decisions

| Condition | Decision |
|---|---|
| Lock attempt returns `null` through the bounded final attempt | Refuse as a live competing owner; preserve the lock file. |
| `OverlappingFileLockException` or active local reservation | Refuse as a duplicate acquisition in this JVM. |
| Exclusive locking or required open options are unsupported | Fail with `LOCK_UNSUPPORTED`; publish no owner. |
| Channel closes or lock becomes invalid before publication | Fail with `LOCK_LOST`; publish no live resource. |
| Controlled lock path no longer resolves to the locked file | Fail with `LOCK_IDENTITY_MISMATCH`. |
| Prior canonical owner record exists | Release the newly acquired lock and require later takeover evaluation. |
| Prior record is malformed, unsupported, or fingerprint-invalid | Preserve it and return its exact structured failure class. |
| Temporary record already exists | Preserve it as interrupted-publication evidence and fail closed. |
| Atomic installation is unavailable | Preserve source evidence and fail; do not use a non-atomic replacement fallback. |
| Failure after an owner record was installed | Release resources and leave the record as abrupt-owner evidence. |

PR2 never deletes the lock file to influence ownership. A file's absence or timestamp is not used to infer owner death.
Process ID and the hashed directory-derived host diagnostic are context only, never exclusion proof.

## Release ordering

Normal release stops at one synchronized resource boundary:

1. Confirm the lock and channel are still valid and the directory identity is unchanged.
2. Re-read the current record and require its exact fingerprint.
3. Create the canonical `RELEASED` record without changing generation or prior-owner evidence.
4. Force the temporary record, atomically replace the owner record, force installed evidence, and verify it exactly.
5. Release the `FileLock`, close the channel, and remove the process-local reservation.

Repeated release returns the same structured result. If release publication fails, the method still performs its final
bounded OS resource close and reports that the record remains active-looking; the next owner must use stale/takeover
classification. If the durable release record installed but in-memory status publication failed, an exact read-back can
prove installation before the lock is released.

## Evidence boundary

Tests cover simultaneous threads, duplicate acquisition, direct-channel contention, bounded live-owner refusal,
unsupported locking, unexpected channel closure, lock-file replacement, owner-record corruption/version mismatch,
failures before lock and across record force/install boundaries, clean release, failed release, post-install recovery,
idempotent release, fixed lock-file preservation, and symbolic-link rejection.

PR2 does not yet wire ownership before Spring startup reconciliation or any journal, allocation, experiment, quarantine,
compaction, retention, or export mutation. It does not provide renewal, stale-owner classification, generation increase,
takeover, separate-process packaged proof, force unlock, malicious-process resistance, network-filesystem correctness,
multi-host coordination, production traffic control, production ownership safety, or production readiness.
