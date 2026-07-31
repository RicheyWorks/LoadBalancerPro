# Enterprise Lab Single-Host Ownership Schema

Status: PR1 ownership-domain contract. Lock acquisition and mutation authority are not implemented by this document.

## Controlled storage

Ownership infrastructure is fixed beneath the existing journal namespace:

```text
<trusted-root>/enterprise-lab-experiment-journals-v1/ownership-v1/
  directory-identity-v1
  owner.lock
  owner-record-v1.json
  owner-record-v1.tmp
  history/
```

Only `directory-identity-v1` and the two controlled directories are created by PR1. Lock and owner-record files are
reserved for later ownership acquisition. No controller or API accepts a path, filename, owner ID, or generation.

The trusted root must be an explicit absolute local path to an existing non-symbolic-link directory. Root paths, UNC
paths, traversal through links, substituted ownership directories, and caller-selected ownership files fail closed.

`directory-identity-v1` contains a bounded random non-secret marker. Creation is forced to the filesystem channel. The
live path capability hashes that marker with filesystem identity evidence and rejects missing, malformed, or changed
markers. This detects replacement during a live ownership lifetime; it is not an identity anchor outside the evidence
root and does not prove network-filesystem behavior.

## Owner record v1

Schema: `enterprise-lab-evidence-owner-record/v1`

Canonical fields:

- controlled directory and lock-file identity fingerprints;
- bounded owner ID and application-instance ID;
- process ID, or zero when unavailable;
- hashed host diagnostic identity, never a raw hostname;
- monotonic owner generation;
- explicit ownership state;
- acquisition, renewal, and lease-expiration instants;
- prior owner-record fingerprint or `GENESIS`;
- takeover reason and bounded takeover sequence;
- reconciliation and release status;
- SHA-256 fingerprint of every preceding canonical field.

Encoding is one bounded canonical JSON object. Unknown or duplicate fields, trailing content, malformed UTF-8/types,
unsupported versions, invalid timestamps/enums, oversized input, and fingerprint mismatch are rejected deliberately.
The hard encoded-record maximum is 16,384 bytes.

Generation 1 requires `GENESIS`, takeover sequence zero, and `INITIAL_ACQUISITION`. Later generations require the exact
prior record fingerprint and a positive bounded takeover sequence. Generation is never derived from wall-clock time or
accepted from an API caller; overflow fails closed.

## Compatibility and failure decisions

The v1 reader and path capability apply the following deliberate decisions:

| Evidence encountered | Required decision |
|---|---|
| No directory marker in a valid trusted root | Create one bounded marker through the controlled path, then force it before use; concurrent readers use at most eight one-millisecond retries. |
| Exact canonical v1 owner record | Validate every field, invariant, and fingerprint before returning the record. |
| Unknown schema, field, enum, or trailing content | Reject as unsupported or malformed; do not guess a compatible meaning. |
| Duplicate field or changed canonical content | Reject before any ownership conclusion. |
| Initial generation without `GENESIS` | Reject as inconsistent ownership history. |
| Later generation without a prior fingerprint and positive takeover sequence | Reject as inconsistent ownership history. |
| Generation or takeover sequence at its bounded ceiling | Refuse rollover; do not wrap or reset history. |
| Missing, changed, or malformed directory marker during a live capability lifetime | Reject as directory identity mismatch. |
| Symbolic-link substitution in the trusted or ownership path | Reject before any lock or record operation. |
| Expired timestamps without a newly acquired exclusive OS lock | Treat as diagnostic evidence only; do not authorize takeover. |

Future readers must add an explicit versioned compatibility decision before accepting a later schema. They must not
silently reinterpret a later record as v1, discard an unknown field, repair a fingerprint, reset a generation, or use
record time as a substitute for operating-system exclusion.

## States and results

The model distinguishes acquisition, owned, renewal, release-pending/released, competing-owner, stale-candidate,
takeover-pending/completed, and failed states. Reconciliation and release have separate explicit status values.

Acquisition, renewal, verification, release, stale-owner, and takeover results carry bounded reason codes and structured
failure classifications. A successful result cannot omit its record or carry a failure. Successful verification must
state that the OS lock is valid; successful release must state that the OS lock was released; stale candidacy cannot be
claimed without exclusive lock acquisition.

## Evidence and not-proven boundary

The owner record is audit and generation evidence. It is not the exclusion primitive. PR2 must hold an exclusive
OS-backed `FileLock` for the complete ownership lifetime before publishing a record. Timestamps alone never authorize
takeover. Fingerprints detect canonical-content modification but do not authenticate an owner, prove non-repudiation,
or resist a malicious process with direct filesystem access.

PR1 does not yet provide cross-process exclusion, renewal, stale takeover, reconciliation gating, mutation fencing,
operator endpoints, or packaged subprocess proof. Multi-host and network-filesystem coordination remain out of scope.
