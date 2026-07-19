# Enterprise Lab Independent Allocation Supervisor

This contract defines the bounded single-host process boundary between the Enterprise Lab application and the future
independent allocation supervisor. It is not a production traffic-controller design, a distributed fencing claim, or a
public service contract.

## Responsibility boundary

The application remains authoritative for experiment lifecycle, observations, scoring, guardrails, durable allocation
intent, experiment evidence, operator authorization, and reconciliation policy.

The supervisor will be authoritative for its own OS-backed ownership, its installed loopback allocation, durable
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

## Process sequence

Later campaign slices will:

1. add a separately executable literal-`127.0.0.1` supervisor with its own OS lock and durable installed state;
2. add high-entropy authenticated client transport with no DNS, proxy, redirect, or external fallback;
3. integrate supervisor-required mode without silent in-process fallback;
4. reconcile application restart, supervisor restart, dual restart, stale application, and crash windows; and
5. expose sanitized operator status and packaged multi-process proofs.

The current proof-only allocation holder remains proof-only until those production Enterprise Lab boundaries are
implemented and verified. Its raw in-memory CAS and run-token protocol are not the runtime supervisor contract.

## Not-proven boundaries

This contract does not prove production readiness, production routing, external targets, public listeners, multi-host or
distributed coordination, network-filesystem correctness, malicious-local-administrator resistance, automatic failover,
load/stress capacity, throughput, latency, p95/p99, SLOs, cloud operation, or deployment certification.
