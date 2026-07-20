# Enterprise Lab Supervisor Command Ledger

This document defines the bounded local-lab command-evidence contract introduced by the supervisor command-ledger
campaign. It describes implemented PR1 model/codec behavior and the constrained integration path for later campaign
PRs. It does not claim production readiness, non-repudiation, hostile-administrator resistance, multi-host coordination,
or external traffic validation.

## Boundary

The command ledger is restricted to one local application JVM, one local supervisor JVM, one controlled evidence root,
and authenticated literal `127.0.0.1` IPC. It correlates the existing allocation and ownership evidence; it does not
replace the allocation transaction store, experiment journal, installed-state model, or ownership records.

The two eventual ledgers remain independently readable and verifiable:

- the application ledger records durable intent, dispatch, response observation, retry, application commit, and
  reconciliation evidence;
- the supervisor ledger records authenticated receipt, validation, duplicate classification, mutation, installed-state
  read-back, durable commit, response construction/delivery, and reconciliation evidence.

PR1 adds the shared immutable event and strict canonical codec. It does not yet create a ledger file or change command
dispatch.

## Identity Reuse

The ledger reuses existing canonical identities:

- `correlationId` is the supervisor protocol `requestId` and must remain stable for an identical retry;
- `requestFingerprint` is the existing canonical protocol request fingerprint;
- `transactionId` is the exact transaction identity carried by the supervisor request;
- `experimentId` is the existing optional experiment identity;
- application instance and owner generation come from durable application ownership;
- supervisor instance and generation come from the pinned supervisor epoch;
- allocation generation comes from the existing application transaction boundary;
- requested, previous committed, and installed fingerprints reuse existing canonical allocation fingerprints;
- `responseFingerprint` is the existing canonical supervisor response fingerprint.

No second correlation, allocation, experiment, ownership, or installed-state identity is introduced. Later integration
must stabilize the existing request ID before dispatch rather than wrap each retry in a new identity.

## Canonical Event

`EnterpriseLabCommandLedgerEvent` is versioned as
`enterprise-lab-supervisor-command-ledger-event/v1`. Each event binds:

- application or supervisor ledger side;
- monotonic bounded sequence and predecessor fingerprint;
- event type, correlation ID, request fingerprint, transaction ID, experiment ID, and command type;
- application and supervisor process epochs;
- allocation generation and allocation fingerprints;
- router generations before and after the event;
- authentication, validation, duplicate, mutation, response, and application-commit classifications;
- bounded retry attempt, reason code, UTC timestamp, and safe metadata;
- optional canonical response and observed supervisor-event fingerprints;
- codec-controlled current fingerprint.

Application-only and supervisor-only events cannot be written to the other side. Shared failure, quarantine, and
reconciliation events remain legal on either side.

## Canonical Encoding

`EnterpriseLabCommandLedgerEventCodec` emits one strict UTF-8 JSON object with fixed field order and lexically ordered
metadata. The current event fingerprint is lowercase SHA-256 over the canonical event without its fingerprint field.

Decode rejects:

- unsupported schema versions;
- missing, unknown, duplicated, or unknown-enum fields;
- malformed or noncanonical JSON and UTC timestamps;
- invalid UTF-8;
- fingerprint mismatch;
- input beyond the hard event-byte limit;
- event-side, sequence, predecessor, generation, transaction, allocation, or outcome invariant violations.

Fingerprints detect content changes. They do not authenticate an author and do not provide cryptographic signer identity
or non-repudiation. Existing authenticated IPC and ownership resources remain the mutation authorities.

## Safety And Bounds

The model permits at most 32 KiB per event, 16 metadata entries, eight retry attempts, and a bounded sequence. IDs,
reason codes, metadata keys, and metadata values have hard length and syntax limits.

Events contain no authentication credential, header, raw request/response bytes, raw allocation payload, arbitrary path,
backend address, URL, host, port, command text, executable name, stack trace, or caller-selected control. Secret-like,
location-like, and forbidden control metadata is rejected before encoding.

## Campaign Integration Order

Later PRs add executable behavior in this order:

1. application append-only persistence and intent-before-dispatch;
2. supervisor append-only persistence and receipt-before-mutation;
3. cross-process coordination and identical-retry idempotency;
4. restart reconstruction and partial-history reconciliation;
5. authenticated sanitized status, safe terminal retention/compaction, and separate-process packaged proofs.

No later step may open readiness from process memory alone, delete the only unresolved evidence, accept conflicting
correlation reuse, weaken ownership/generation fencing, or repeat a mutation whose terminal supervisor evidence verifies
the installed state.

## Not-Proven Boundaries

The command ledger does not prove production readiness or certification, live-cloud or real-tenant behavior, public or
external traffic control, multi-host or network-filesystem correctness, distributed consensus, database/broker
durability, hostile local-administrator resistance, production performance, signer identity, or non-repudiation.
