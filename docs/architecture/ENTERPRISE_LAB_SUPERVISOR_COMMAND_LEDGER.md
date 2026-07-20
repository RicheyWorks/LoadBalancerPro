# Enterprise Lab Supervisor Command Ledger

This document defines the bounded local-lab command-evidence contract introduced by the supervisor command-ledger
campaign. It describes implemented PR1 model/codec and PR2 application-ledger behavior plus the constrained integration
path for later campaign PRs. It does not claim production readiness, non-repudiation, hostile-administrator resistance,
multi-host coordination, or external traffic validation.

## Boundary

The command ledger is restricted to one local application JVM, one local supervisor JVM, one controlled evidence root,
and authenticated literal `127.0.0.1` IPC. It correlates the existing allocation and ownership evidence; it does not
replace the allocation transaction store, experiment journal, installed-state model, or ownership records.

The two eventual ledgers remain independently readable and verifiable:

- the application ledger records durable intent, dispatch, response observation, retry, application commit, and
  reconciliation evidence;
- the supervisor ledger records authenticated receipt, validation, duplicate classification, mutation, installed-state
  read-back, durable commit, response construction/delivery, and reconciliation evidence.

PR1 adds the shared immutable event and strict canonical codec. PR2 adds the fixed application ledger and an explicit
intent-before-transport dispatcher, but does not wire that dispatcher into the supervisor allocation bridge. Supervisor
persistence and coordinated end-to-end dispatch remain later slots.

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

## Application Ledger

`EnterpriseLabApplicationCommandLedger` owns one fixed JSONL file beneath the existing controlled Enterprise Lab
evidence namespace. Callers supply only the explicit trusted root; they cannot select the ledger directory or filename.
The production writer requires the existing live application ownership gate. It verifies the exact ownership record
fingerprint, application instance, and generation carried by each canonical request, rejects a second process-local
writer, and rechecks the live ownership epoch before append, synchronization, and read-back. The ownership gate's
existing operating-system lock remains the cross-process writer authority; the ledger does not add a second lock or
ownership identity.

The ledger is bounded to 8 MiB and 4,096 events. Creation validates any existing chain before returning. Every append:

1. replays the complete bounded chain;
2. derives the next sequence and predecessor fingerprint inside the writer boundary;
3. binds the event to the exact canonical request and, when supplied, response;
4. rejects correlation reuse, generation regression, missing/duplicate intent, illegal lifecycle transitions, or an
   event after a terminal head;
5. appends one newline-delimited canonical event, forces data and metadata, and replays for exact read-back.

Malformed complete events, noncanonical content, fingerprint or predecessor changes, partial tails, unexpected storage
entries, symlink/type escapes, concurrent file changes, and hard-limit overflow fail closed without repair or truncation.
An uncertain post-write failure makes that writer unusable; a fresh bounded replay determines whether the complete event
is present or the partial tail must remain quarantined from further mutation. Read-only inspection creates no path and
`unresolvedHeads()` reconstructs the latest nonterminal event per correlation from durable evidence alone.

### Storage And Replay Contract

The application ledger uses one canonical event per line. Newline is the frame terminator rather than caller content:
the event codec does not emit embedded line breaks, and replay requires the final byte of a nonempty ledger to be a
newline. A missing terminator is classified as an incomplete tail. The reader never guesses whether the bytes would have
formed a valid event, truncates them, or appends beyond them.

Replay validates these layers in order:

1. the supplied root is absolute, existing, local, non-root, and free of symbolic-link traversal;
2. the fixed namespace and application-ledger directory remain direct, non-symbolic-link children;
3. the ledger directory contains only the one fixed ledger filename;
4. the file is a direct non-symbolic-link regular file and remains within the hard byte bound;
5. every complete frame is strict UTF-8 canonical JSON with a matching content fingerprint;
6. sequence, predecessor, application side, owner-generation, correlation identity, and lifecycle rules hold across the
   complete chain.

No replay result is returned from a partially valid chain. A corrupt later frame therefore cannot leave an apparently
usable earlier prefix. This is deliberate: PR2 preserves the complete bytes for later operator evidence and does not
provide repair, truncation, salvage, or caller-selected alternate storage.

The writable path repeats replay before deriving an event and again after preparing the file. It compares both the event
list and byte count so a file change between those observations is rejected. After append it forces the file with the
data-and-metadata policy, replays again, and returns a receipt only when the new head and byte count match exactly. An
exception after any write begins invalidates that writer even if a later replay proves that the full frame reached disk.

### Application Lifecycle Contract

The ledger applies a small application-side state machine per correlation while keeping one global fingerprint chain:

| New event | Required durable history |
| --- | --- |
| `APPLICATION_INTENT_PERSISTED` | no prior event for this correlation |
| `DISPATCH_ATTEMPTED` | correlation head is intent or bounded retry |
| `RESPONSE_LOST` / `TIMEOUT_OBSERVED` | correlation head is dispatch attempted |
| `RETRY_ISSUED` | correlation head is response lost or timeout |
| `APPLICATION_RESPONSE_RECEIVED` | at least one earlier dispatch with the same exact command identity |
| `RECONCILIATION_COMPLETED` | an existing nonterminal correlation |
| `APPLICATION_COMMITTED` | correlation head is response received or reconciliation completed |
| `COMMAND_FAILED` / `COMMAND_QUARANTINED` | an existing nonterminal correlation with explicit bounded outcomes |

The first event for any correlation must be intent, even if its sequence is not the first global sequence. A repeated
intent is rejected. All later events must retain the first event's request fingerprint, transaction and experiment,
command type, application and supervisor epochs, allocation generation, requested allocation fingerprint, and previous
committed fingerprint. Application commit, command failure, and quarantine are terminal in PR2; later events cannot be
silently attached to those heads.

`unresolvedHeads()` is a reconstruction aid, not a readiness decision. It returns the latest durable event for each
correlation whose head is not terminal. It does not infer that a dispatch was received, a mutation happened, or a retry
is safe. Those determinations require the supervisor ledger and reconciliation rules in later PRs.

### Dispatch Failure Matrix

The PR2 dispatcher has intentionally narrow results:

| Boundary outcome | Durable application evidence | Transport called | Remote result inferred |
| --- | --- | --- | --- |
| intent append rejected | none | no | no |
| intent durable, dispatch append rejected | intent | no | no |
| intent and dispatch durable, transport throws | intent plus dispatch attempt | yes | no |
| transport returns mismatched response | intent plus dispatch attempt | yes | no; response rejected |
| transport returns exact response | intent plus dispatch attempt | yes | exact response returned to caller |

PR2 does not automatically write timeout, loss, response-received, or application-commit events from a transport result.
That work needs stable retry identity and the independently durable supervisor event fingerprint, so it remains in the
coordinator and reconciliation slots. The dispatcher is also not yet installed into
`EnterpriseLabSupervisorAllocationBridge`; its tests prove the boundary directly without changing current runtime
dispatch behavior.

### Ownership Separation

The live application ownership record authorizes mutation of the ledger; it is not replaced by ledger evidence. The
production factory compares each request's application instance, ownership-record fingerprint, and generation with the
gate's current durable record, then rechecks the same ownership epoch around the write and force boundaries. Historical
events remain readable after that ownership generation ends, but a stale request cannot append through a new generation.
Test-only mutation seams remain package-private and do not manufacture a production ownership gate.

The application ledger intentionally does not persist the ownership credential, operating-system lock handle, process
ID, host diagnostic, or raw ownership record. Its canonical event carries only the existing application instance and
generation needed for cross-ledger correlation. The production append boundary validates the request's ownership-record
fingerprint against the live record without copying that fingerprint into unrestricted metadata. This keeps authority in
the existing ownership subsystem and evidence in the command ledger, so replaying or editing ledger bytes cannot grant a
mutation capability. Read-only inspection likewise cannot acquire, renew, release, or take over ownership.

`EnterpriseLabApplicationCommandDispatcher` forces both `APPLICATION_INTENT_PERSISTED` and `DISPATCH_ATTEMPTED` before
calling a supplied supervisor transport. If either append fails, the transport is unreachable. A transport exception
leaves the dispatch attempt unresolved and does not infer whether the supervisor acted. Response observation, retry,
restart reconciliation, and application commit integration remain explicit later campaign work.

## Campaign Integration Order

Later PRs add executable behavior in this order:

1. supervisor append-only persistence and receipt-before-mutation;
2. cross-process coordination and identical-retry idempotency;
3. restart reconstruction and partial-history reconciliation;
4. authenticated sanitized status, safe terminal retention/compaction, and separate-process packaged proofs.

No later step may open readiness from process memory alone, delete the only unresolved evidence, accept conflicting
correlation reuse, weaken ownership/generation fencing, or repeat a mutation whose terminal supervisor evidence verifies
the installed state.

## Not-Proven Boundaries

The command ledger does not prove production readiness or certification, live-cloud or real-tenant behavior, public or
external traffic control, multi-host or network-filesystem correctness, distributed consensus, database/broker
durability, hostile local-administrator resistance, production performance, signer identity, or non-repudiation.
