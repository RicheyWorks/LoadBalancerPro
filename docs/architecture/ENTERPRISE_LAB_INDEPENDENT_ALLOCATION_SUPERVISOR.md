# Enterprise Lab Independent Allocation Supervisor

This contract defines the bounded single-host process boundary between the Enterprise Lab application and the local
independent allocation supervisor. PR2 implements the supervisor side; later slots integrate the application client and
reconciliation. It is not a production traffic-controller design, a distributed fencing claim, or a public service
contract.

## Responsibility boundary

The application remains authoritative for experiment lifecycle, observations, scoring, guardrails, durable allocation
intent, experiment evidence, operator authorization, and reconciliation policy.

The supervisor is authoritative for its own OS-backed ownership, its installed loopback allocation, durable
installed-state recovery, application-generation acceptance, atomic application, and independent read-back.

Both processes reuse the existing loopback allocation snapshots, installed-state snapshots, target catalog, allocation
fingerprints, transaction identifiers, owner-generation limits, and sanitized evidence rules. There is no second
allocation representation.

## PR1 protocol boundary

Schema `enterprise-lab-allocation-supervisor-ipc/v1` defines canonical business messages for:

- health and readiness observation;
- installed-allocation and bounded-status read-back;
- initial safe-baseline establishment;
- approved allocation application;
- safe-baseline restoration;
- exact allocation verification;
- application-ownership handoff;
- supervisor-generation observation; and
- authorized clean shutdown.

Unknown schemas, commands, enum values, fields, missing fields, duplicate JSON fields, non-canonical JSON, malformed
UTF-8, excessive bytes, invalid target bindings, fingerprint mismatches, and command/payload mismatches fail closed.

PR1 adds no listener, process mode, credential file, storage path, operator endpoint, routing activation, or application
fallback behavior.

## PR2 process and durable-holder boundary

`--enterprise-lab-supervisor` runs the holder in a separate JVM without starting Spring or the API server. It acquires
the fixed `enterprise-lab-supervisor-v1/supervisor.lock` beneath the explicit local data root, reconstructs or creates
canonical supervisor state, binds a `ServerSocket` only through the byte-constructed literal address `127.0.0.1`, and
publishes completed readiness metadata by same-directory atomic move. No hostname, wildcard address, external fallback,
HTTP server framework, environment proxy, production backend, or application in-memory allocation is involved.

The runtime transport is a one-request-per-connection binary envelope around the PR1 canonical JSON business message.
It has fixed magic and version fields, one 256-bit per-process credential encoded as 64 lowercase hexadecimal bytes,
and explicit credential/request lengths. The credential is stored separately from readiness and business evidence,
compared in constant time, never placed in URLs, request fingerprints, state records, reasons, or command output, and
rotated whenever a new supervisor process starts. Clean shutdown removes readiness and credential metadata; abrupt-exit
metadata remains generation-fenced and is replaced before the next readiness publication. This server-side credential
boundary does not yet claim an application client; that is PR3.

The holder's current file is `supervisor-state-v1.json`. It contains the supervisor instance and generation, current and
previous accepted application ownership evidence, immutable generation-zero safe baseline, installed allocation,
optional in-flight intended allocation, transaction/request fingerprints, prior committed allocation fingerprint,
transaction phase, last commit time, recovery classification and reason, durable generation, and predecessor/current
record fingerprints. Canonical encoding reuses the installed-allocation representation and target binding. The complete
state is fsynced to a sibling temporary file, atomically replaces the current file, and is decoded and compared exactly
before success is returned. Fingerprints detect content changes; they do not authenticate an author.

Allocation mutation is a three-publication sequence: durable intent with the prior installed state, atomic installed
replacement retained as an incomplete applied phase, then independently read-back committed state. A crash before the
commit leaves an intended allocation and therefore cannot be mistaken for success. On restart, the new supervisor
generation preserves a completed installed allocation exactly; any incomplete phase restores and verifies the immutable
safe baseline before readiness. One interrupted state temporary is atomically preserved under a fixed name as bounded
forensic evidence. A second unresolved interrupted temporary fails startup closed rather than growing storage.

The production ownership verifier reads only the repository-controlled application ownership record. Handoff and every
mutation require exact application instance, record fingerprint, generation, active lease, completed reconciliation,
non-released state, and the expected current supervisor identity/generation. Initial handoff may adopt the exact current
repository record; subsequent handoff advances one generation. Current and previous accepted application epochs remain
in durable state. PR2 does not wire the application to this verifier or change its router dependency graph.

## PR3 authenticated client boundary

`EnterpriseLabSupervisorClient` is the synchronous application-side transport for one pinned supervisor process epoch.
It accepts only an explicit trusted local data root and the repository target catalog. Discovery reads the fixed
`supervisor-ready-v1.json` file, then the separate fixed `supervisor-credential-v1`, then readiness again. All paths
remain beneath `enterprise-lab-supervisor-v1`; symbolic links, non-regular files, changed file identities, incomplete
reads, excessive bytes, unsafe POSIX credential permissions, and a changed readiness document fail closed.

Readiness schema `enterprise-lab-supervisor-readiness/v1` is canonical UTF-8 JSON with one LF terminator and a 4,096-byte
maximum. It permits only literal address `127.0.0.1`, a process-published port from 1,024 through 65,535, canonical
supervisor identity, bounded supervisor and durable-state generations, lowercase state SHA-256, and a canonical
publication time. Unknown, duplicate, missing, malformed, or non-canonical content is rejected. The 65-byte credential
file is exactly 64 lowercase hexadecimal bytes plus one LF. The client never accepts a hostname, URL, proxy, redirect,
caller-selected address or port, environment fallback, or external target.

Every connection uses a byte-constructed IPv4 loopback `InetAddress` and a raw JDK `Socket` created with
`Proxy.NO_PROXY`; therefore DNS, HTTP content negotiation, redirect behavior, and HTTP proxy selection are absent. The
binary transport instead requires the exact PR2 magic, frame version, transport status, credential length, and bounded
canonical business response. Connect time is at most two seconds, response idle time is at most three seconds,
monotonic absolute response lifetime is at most ten seconds, response content is at most 65,536 bytes, and transport
attempts are limited to two. One client serializes its requests and creates no thread or queue.

Construction authenticates a request-correlated health exchange before returning a usable client. Every later command
must carry the pinned supervisor identity and generation, rereads both fixed control files, verifies freshness, and
rejects metadata change, credential change, supervisor restart, generation regression, unexpected response identity,
and request/response mismatch. A readiness/credential epoch older than eight hours fails closed; reconnecting does not
extend it, and starting a new supervisor rotates the 256-bit credential and advances the supervisor generation.

Credential bytes are transport-only. Runtime generation, publication, validation, comparison, and cleanup avoid
immutable credential strings; transient entropy and copies are zeroed, comparison is constant-time where secrets are
compared, and retained server/client arrays are zeroed on close. POSIX group/other access is rejected; Windows remains
bounded by the repository-controlled local directory and inherited ACL behavior. This does not claim resistance to a
malicious local administrator or a same-authority process that can replace controlled files.

PR3 does not select this client from application configuration, mutate the normal router, perform application ownership
handoff, change startup admission, or silently fall back to in-process state. Those integration and reconciliation
responsibilities begin in PR4 and PR5.

## Request evidence

Every request carries:

- a canonical request ID and request fingerprint;
- the command and application instance identity;
- the application ownership-record fingerprint and owner generation;
- the expected supervisor identity and generation;
- transaction and optional experiment correlation;
- an existing allocation purpose and, only where required, an existing loopback allocation snapshot;
- canonical allocation and previous-commit fingerprints;
- a canonical UTC request time; and
- bounded sanitized metadata.

Transport authentication is intentionally outside this canonical business payload. A credential must never become part
of request fingerprints, allocation journals, command history, failure reasons, logs, or operator responses.

Read-only bootstrap observations use explicit `NONE` sentinels and cannot carry transaction or allocation-mutation
evidence. Mutation, verification, handoff, and shutdown commands require current application ownership, an expected
supervisor identity/generation, and a transaction ID. Optional identity/generation fences must be present or absent as
a pair, while required request, application, experiment, supervisor, and transaction identities cannot use `NONE` as
their identity. Allocation apply and baseline restore require an exact prior committed allocation fingerprint; initial
baseline establishment and non-mutating verification do not accept one.

## Response evidence

Every response carries:

- the exact request ID, request fingerprint, and command;
- the supervisor instance identity and generation;
- the application generation observed by the supervisor;
- a command-derived classification and accepted, rejected, or failed status;
- a truthful action-performed indicator;
- optional installed-state read-back with matching allocation fingerprint and router generation;
- the durable state generation and verification result;
- a bounded structured reason and canonical UTC response time; and
- a canonical response fingerprint.

An accepted allocation operation or verification requires an installed snapshot and `MATCHED` verification. Rejected
or failed responses cannot claim an action. Observation commands cannot claim allocation mutation. Application clients
must use request-bound response issuance and decoding: correlate all three of request ID, request fingerprint, and
command, then reject an accepted result unless its supplied supervisor fence, observed application generation, and
requested allocation fingerprint match the exact request. An installed-allocation read requires installed state but
does not claim that an independently supplied target allocation was verified.

## Allocation and target rules

Allocation-bearing messages reuse `EnterpriseLabLoopbackAllocationSnapshot`. Installed read-back reuses
`EnterpriseLabInstalledAllocationSnapshot` and its strict codec. The protocol codec verifies the repository-controlled
target catalog, backend set, normalized shares, allocation fingerprint, installed fingerprint, and router generation.

No business message accepts a filesystem path, URL, host, port, executable, shell command, transaction phase, caller-
selected router generation, or caller-selected supervisor generation. The request's expected supervisor generation is a
comparison fence; it is not authority to create or advance a supervisor generation.

## Initial lock and mutation order

The campaign will preserve this order unless executable deadlock testing proves a narrower safe refinement:

1. application evidence ownership;
2. application allocation transaction coordination;
3. bounded supervisor request;
4. supervisor ownership and application-handoff validation;
5. supervisor installed-state mutation;
6. supervisor durable commit and read-back;
7. application durable commit.

The application must not hold unrelated broad locks while waiting on IPC or filesystem work. The normal loopback backend
selection path must not acquire either process's broad mutation lock.

## Hard protocol limits

PR1 fixes request and response messages at no more than 65,536 bytes each, metadata at no more than 16 entries, metadata
keys at 64 characters, metadata values at 256 characters, canonical identifiers at 128 characters, and allocations at
the existing maximum of 64 approved backends. Later process PRs must add stricter transport, concurrency, queue,
duration, storage, history, retry, startup, idle, shutdown, log, and proof-runtime limits without weakening these bounds.

PR2 fixes the process limits as follows:

- four worker connections and eight queued connections, with rejection beyond the queue;
- 4,096 one-shot connections per process and one request per connection;
- three-second connection idle I/O timeout, ten-second monotonic absolute connection lifetime, and five-second
  command/shutdown bounds;
- 30-second maximum request age and five-second future clock-skew allowance;
- 65,536-byte canonical request and response limits inherited from PR1;
- 192 KiB per durable state record, one retained current transaction, one interrupted temporary, and 768 KiB across
  those durable state paths;
- the existing 64-backend maximum and repository-owned exact backend set;
- ephemeral port zero by default or one explicitly configured numeric port from 1,024 through 65,535, avoiding a
  privileged-port requirement; and
- no unbounded thread creation, retry loop, process output, request metadata, or evidence filename.

Start locally with an explicit controlled directory when the default ignored target directory is not desired:

```text
java -jar target/LoadBalancerPro-2.5.0.jar --enterprise-lab-supervisor --enterprise-lab-supervisor-data-directory=target/enterprise-lab-supervisor --enterprise-lab-supervisor-port=0
```

Readiness is distinct from process health. The listener can answer health while mutation readiness remains closed until
verified application ownership is accepted and no transaction is incomplete. An authorized clean-shutdown command is
required for clean metadata removal; ordinary operating-system termination intentionally exercises abrupt recovery.

## Process sequence

The campaign sequence is:

1. PR2 adds the separately executable literal-`127.0.0.1` supervisor, OS lock, bounded transport server, and durable
   installed-state reconstruction described above;
2. PR3 adds high-entropy authenticated client transport with no DNS, proxy, redirect, or external fallback;
3. integrate supervisor-required mode without silent in-process fallback;
4. reconcile application restart, supervisor restart, dual restart, stale application, and crash windows; and
5. expose sanitized operator status and packaged multi-process proofs.

The current proof-only allocation holder remains proof-only until those production Enterprise Lab boundaries are
implemented and verified. Its raw in-memory CAS and run-token protocol are not the runtime supervisor contract.

## Not-proven boundaries

This contract does not prove production readiness, production routing, external targets, public listeners, multi-host or
distributed coordination, network-filesystem correctness, malicious-local-administrator resistance, automatic failover,
load/stress capacity, throughput, latency, p95/p99, SLOs, cloud operation, or deployment certification.
