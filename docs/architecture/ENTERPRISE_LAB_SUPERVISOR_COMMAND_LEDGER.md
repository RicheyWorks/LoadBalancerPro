# Enterprise Lab Supervisor Command Ledger

This document defines the bounded local-lab command-evidence contract introduced by the supervisor command-ledger
campaign. It describes the implemented canonical model, both append-only ledgers, the cross-process command coordinator,
and bounded restart reconstruction through campaign PR5. It does not claim production readiness,
non-repudiation, hostile-administrator resistance, multi-host coordination, or external traffic validation.

## Boundary

The command ledger is restricted to one local application JVM, one local supervisor JVM, one controlled evidence root,
and authenticated literal `127.0.0.1` IPC. It correlates the existing allocation and ownership evidence; it does not
replace the allocation transaction store, experiment journal, installed-state model, or ownership records.

The two ledgers remain independently readable and verifiable:

- the application ledger records durable intent, dispatch, response observation, retry, application commit, and
  reconciliation evidence;
- the supervisor ledger records authenticated receipt, validation, duplicate classification, mutation, installed-state
  read-back, durable commit, response construction/delivery, and reconciliation evidence.

PR1 added the shared immutable event and strict canonical codec. PR2 added the fixed application ledger and explicit
intent-before-transport dispatcher. PR3 added the fixed supervisor ledger and made a forced authenticated receipt the
first supervisor service action. PR4 installed those ledgers on the existing application-to-supervisor path, including
duplicate outcomes, mutation stages, exact read-back, supervisor commit, response delivery, and deferred application
commit through the sole allocation coordinator. PR5 adds the startup reconstruction contract below. Authenticated
status, terminal retention/compaction, and packaged command-ledger proofs remain PR6 scope.

## Identity Reuse

The ledger reuses existing canonical identities:

- `correlationId` is the supervisor protocol `requestId` and must remain stable for an identical retry;
- `requestFingerprint` is the existing canonical protocol request fingerprint;
- `transactionId` is the exact transaction identity carried by the supervisor request;
- `experimentId` is the existing optional experiment identity;
- application instance and owner generation come from durable application ownership;
- the event's supervisor instance and generation retain the canonical request's pinned expected epoch; an accepted
  response must match that fence, while a rejected response's current observed epoch is bound by its exact canonical
  `responseFingerprint` without rewriting the request identity;
- allocation generation comes from the existing application transaction boundary when the canonical request carries
  that existing value; a supervisor receipt records zero rather than inventing a generation when an older direct
  supervisor request does not carry it;
- requested, previous committed, and installed fingerprints reuse existing canonical allocation fingerprints;
- `responseFingerprint` is the existing canonical supervisor response fingerprint.

For allocation mutations, the bridge deterministically derives the stable protocol correlation and transaction IDs from
the existing application allocation transaction ID, command, allocation generation, and requested allocation
fingerprint. The protocol transaction ID and allocation-store transaction ID are intentionally distinct encoded forms;
PR5 verifies the exact derivation against the allocation chain head rather than equating or replacing them.

No second correlation, allocation, experiment, ownership, or installed-state identity is introduced. The integrated
dispatcher stabilizes the existing request ID before dispatch rather than wrapping each retry in a new identity.

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
usable earlier prefix. This is deliberate: the ledger preserves the complete bytes for later operator evidence and does
not
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
committed fingerprint. Application commit, command failure, and quarantine are terminal; later events cannot be
silently attached to those heads.

`unresolvedHeads()` is a reconstruction aid, not a readiness decision. It returns the latest durable event for each
correlation whose head is not terminal. It does not infer that a dispatch was received, a mutation happened, or a retry
is safe. PR5 makes those determinations only from the independently replayed supervisor ledger, authoritative installed
state, and the existing allocation transaction chain.

### Dispatch Failure Matrix

The dispatcher has intentionally narrow transport-boundary results:

| Boundary outcome | Durable application evidence | Transport called | Remote result inferred |
| --- | --- | --- | --- |
| intent append rejected | none | no | no |
| intent durable, dispatch append rejected | intent | no | no |
| intent and dispatch durable, transport throws | intent plus dispatch attempt | yes | no |
| transport returns mismatched response | intent plus dispatch attempt | yes | no; response rejected |
| transport returns exact response | intent plus dispatch attempt | yes | exact response returned to caller |

The PR4 coordinator records bounded response loss/timeout, response receipt, and application commit only after matching
the independently readable supervisor event. Allocation mutation application commit remains deferred until the existing
allocation transaction coordinator has forced and exactly read back its `COMMITTED` state. The dispatcher is installed
in `EnterpriseLabSupervisorAllocationBridge`; there is no local mutation fallback and no second allocation coordinator.

### Ownership Separation

The live application ownership record authorizes mutation of the ledger; it is not replaced by ledger evidence. The
production factory compares each request's application instance, ownership-record fingerprint, and generation with the
gate's current durable record, then rechecks the same ownership epoch around the write and force boundaries. Historical
events remain readable after that ownership generation ends, but a stale request cannot append through a new generation.
PR5 permits the current owner to append only reconciliation, application-commit, failure, or quarantine outcomes to an
existing older correlation. Those events retain the original command identity and generation; bounded metadata records
the live reconciling owner generation. This continuation path cannot create an older-generation intent or dispatch and
repeats the live mutation authorization around append, force, and exact read-back. Test-only mutation seams remain
package-private and do not manufacture a production ownership gate.

The application ledger intentionally does not persist the ownership credential, operating-system lock handle, process
ID, host diagnostic, or raw ownership record. Its canonical event carries only the existing application instance and
generation needed for cross-ledger correlation. The production append boundary validates the request's ownership-record
fingerprint against the live record without copying that fingerprint into unrestricted metadata. This keeps authority in
the existing ownership subsystem and evidence in the command ledger, so replaying or editing ledger bytes cannot grant a
mutation capability. Read-only inspection likewise cannot acquire, renew, release, or take over ownership.

`EnterpriseLabApplicationCommandDispatcher` forces both `APPLICATION_INTENT_PERSISTED` and `DISPATCH_ATTEMPTED` before
calling a supplied supervisor transport. If either append fails, the transport is unreachable. A transport exception
leaves the dispatch attempt unresolved and does not infer whether the supervisor acted. PR4 and PR5 resolve later state
only through exact supervisor evidence and authoritative installed-state verification.

## Supervisor Ledger

`EnterpriseLabSupervisorCommandLedger` owns one fixed JSONL file beneath the existing
`enterprise-lab-supervisor-v1` directory. Callers cannot select the directory or filename. The existing
`EnterpriseLabSupervisorOwnership` operating-system lock is the sole cross-process writer capability; the ledger adds no
second ownership record, generation, transaction ID, or file lock. A process-local mutex serializes complete replay and
append operations for the fixed path.

The supervisor ledger has the same 8 MiB and 4,096-event hard limits and the same strict newline-framed canonical replay
posture as the application ledger. Writable creation requires the live supervisor lock and validates the complete prior
chain. Append rechecks that lock before path preparation, write, force, and exact read-back. A lost lock, unexpected
entry, symlink/type escape, malformed or noncanonical frame, truncated tail, concurrent change, hard-limit overflow, or
uncertain post-write result fails closed without repair. Read-only inspection creates no path and cannot acquire the
supervisor lock.

### Authenticated Receipt Boundary

Transport processing remains ordered as follows:

1. `EnterpriseLabSupervisorServer` validates frame bounds;
2. it compares the fixed-size transport credential in constant time;
3. it decodes the strict canonical protocol request;
4. only then does it call `EnterpriseLabSupervisorService.dispatch`;
5. `dispatch` forces and exactly reads back `SUPERVISOR_RECEIPT_PERSISTED` before shutdown, time, supervisor-fence,
   duplicate, ownership, command, or mutation validation can run.

A malformed frame, wrong credential, or undecodable request never reaches the supervisor ledger. The ledger therefore
does not copy credentials, raw frames, or unauthenticated caller material and PR3 does not manufacture an
`AUTHENTICATION_REJECTED` event from bytes that failed the transport boundary. Tests count the durable receipts across a
real literal-`127.0.0.1` server exchange and prove that wrong credentials and malformed frames leave that count
unchanged.

If receipt append, synchronization, or read-back fails, `dispatch` returns the existing bounded failed response and
reloads the durable supervisor state. None of the existing validation or mutation branches is reached. The ledger failure
does not weaken supervisor ownership, application ownership verification, generation fencing, duplicate classification,
transaction recovery, read-back, or quarantine behavior.

### Supervisor Lifecycle Contract

The first supervisor event for a correlation must be `SUPERVISOR_RECEIPT_PERSISTED`. Every authenticated retry starts a
new receipt episode. This includes a retry that reuses the correlation with changed canonical content: the receipt is
preserved before duplicate validation, and later episode events must bind to that latest exact receipt. The application
ledger remains stricter because it owns the stable intent and rejects changed correlation reuse before transport.

The supervisor store validates this order for later event integration:

| New event | Required durable episode head |
| --- | --- |
| `SUPERVISOR_RECEIPT_PERSISTED` | no prior correlation, another receipt-only observation, or a terminal prior episode |
| `VALIDATION_REJECTED` / duplicate classification / `MUTATION_STARTED` | latest exact receipt |
| `ALLOCATION_APPLIED` | mutation started |
| `READ_BACK_VERIFIED` | allocation applied |
| `SUPERVISOR_COMMITTED` | read-back verified |
| `RESPONSE_SENT` | receipt-only observation or a bounded rejection, duplicate, commit, failure, or reconciliation outcome |

For application-side events, an allocation mutation still requires the existing positive application allocation
generation. A supervisor receipt cannot invent a missing identifier, so an authenticated direct request that omits the
optional `applicationAllocationGeneration` metadata records zero while retaining its exact request fingerprint,
transaction, requested allocation fingerprint, and process fences. Noncanonical or out-of-range supplied values remain
rejected.

PR4 wires bounded validation, duplicate, mutation, read-back, commit, and response outcomes into the live service.
Supervisor-only observation histories may exist for authenticated direct health/status clients; application allocation
reconstruction ignores those non-mutating histories. Any supervisor allocation-mutation history without its durable
application intent remains a failed-closed startup condition.

## Restart Reconstruction And Repair

`EnterpriseLabCommandHistoryReconciler` runs synchronously after the external supervisor session has accepted the current
application owner and after the sole allocation coordinator is constructed. The allocation reconciler defers publishing
`READY` until command-history postflight completes; a replay, identity, installed-state, or repair failure closes the
existing gate as `COMMAND_LEDGER_RECONCILIATION_FAILED`.

The preflight replays both complete bounded ledgers, groups at most 128 unresolved application correlations, and requires
every supervisor event in a shared correlation to retain the application's exact request fingerprint, transaction,
experiment, command, process epochs, allocation generation, requested fingerprint, and previous committed fingerprint.
It applies these policies:

- intent or dispatch without supervisor receipt is recorded as reconciled and not executed;
- receipt, validation rejection, stale generation, or conflicting duplicate without mutation becomes a terminal failed
  application command and is not retried under a new identity;
- a terminal accepted observation can complete missing application evidence without allocation mutation;
- an exact `SUPERVISOR_COMMITTED` allocation event must match the allocation chain head and authoritative installed
  allocation, owner, router generation, and fingerprints; the sole coordinator then appends only missing `APPLIED`,
  `VERIFYING`, and `COMMITTED` allocation evidence and never calls the router mutation path;
- mutation-started history without supervisor commit remains pending while the existing allocation reconciler restores
  or retains the verified baseline; postflight terminalizes the application command only after the allocation report is
  ready;
- a matching terminal pair is an idempotent no-op; an intervening reconciliation event is reused after restart rather
  than duplicated;
- conflicting identity or installed-state evidence is quarantined when a safe append remains possible and always keeps
  readiness failed closed; malformed, noncanonical, unsupported, or truncated ledger bytes are preserved unchanged by
  the strict replay failure.

Each repair appends to the global predecessor chain and forces exact read-back. It does not truncate, rewrite, delete,
move, or synthesize the original request. It does not resume an experiment candidate. Baseline restoration remains the
existing allocation recovery policy, so command-history repair adds no parallel traffic authority.

## Campaign Integration Order

The remaining PR6 adds authenticated sanitized status, safe terminal retention/compaction, and separate-process packaged
proofs.

No later step may open readiness from process memory alone, delete the only unresolved evidence, accept conflicting
correlation reuse, weaken ownership/generation fencing, or repeat a mutation whose terminal supervisor evidence verifies
the installed state.

## Not-Proven Boundaries

The command ledger does not prove production readiness or certification, live-cloud or real-tenant behavior, public or
external traffic control, multi-host or network-filesystem correctness, distributed consensus, database/broker
durability, hostile local-administrator resistance, production performance, signer identity, or non-repudiation.
