# Enterprise Lab Durable Allocation Supervision

## Scope

This contract covers the optional single-host durable allocation supervisor for one repository-bound Enterprise Lab
loopback scenario. It does not make the load balancer production-ready and does not add cloud, tenant, private-network,
multi-host, distributed-lock, throughput, latency, load, stress, or benchmark evidence.

Allocation supervision is configured only when the existing controlled experiment journal directory, live OS-backed
ownership, and exactly one fixed loopback target scenario are present. With no bound scenario, the existing experiment
API remains unable to arm work and the supervision endpoint reports `ALLOCATION_SUPERVISION_NOT_CONFIGURED`. Multiple
durable scenarios are rejected rather than sharing one allocation chain ambiguously.

## Startup and mutation ordering

Durable startup retains this ordering:

1. Resolve the configured absolute controlled data root.
2. Acquire or safely take over live ownership.
3. verify and reconcile experiment journals.
4. Open and replay the fixed allocation state store.
5. Read the installed router snapshot.
6. Classify drift and restore the durable baseline when required.
7. Verify the final installed fingerprint and owner generation.
8. Publish allocation readiness.
9. Admit experiment mutation and start bounded ownership renewal.

Each armed lifecycle attaches the exact session router to the supervisor and runs another synchronous pre-admission
reconciliation. Candidate start then uses the durable transaction coordinator: persist intent, reverify ownership,
apply, read back, compare fingerprint and generations, and persist commit. Completion, rollback, cancellation, shutdown,
and durable-journal failure restore through the supervisor so restoration evidence is part of the allocation chain.

There is no periodic allocation scheduler. Request selection continues to read one immutable installed snapshot without
a global request-path lock.

## Authenticated operator surface

The existing `/api/lab/**` API-key and OAuth2 operator-role policy protects:

- `GET /api/lab/allocation-supervision`
- `POST /api/lab/allocation-supervision/verify`
- `POST /api/lab/allocation-supervision/restore-safe-baseline`

Status is bounded to sanitized scenario/backend identifiers, eligible and excluded identifier lists, fingerprints,
transaction phases, owner/router/allocation generations, readiness, last reconciliation, sixteen recent transaction
summaries, and unresolved/quarantine counts. It excludes target addresses, filesystem paths, raw allocations, raw
journal bytes, headers, credentials, and stack traces.

The two POST actions accept no body. Any body or transfer-encoded input is rejected before reaching the supervisor, so
callers cannot choose allocations, transaction phases, owner or router generations, filesystem paths, force behavior,
commit behavior, read-back skipping, restoration skipping, or drift bypass. Safe restoration always uses the verified
durable baseline.

## Packaged crash-window proof

After packaging, run:

```powershell
./scripts/smoke/enterprise-lab-allocation-proof.ps1
```

The foreground CLI writes ignored evidence below `target/enterprise-lab-allocation-proof-smoke/`. It proves normal
apply/commit, crash before apply, crash after apply, crash after commit, higher-generation takeover, stale mutation
denial, restoration failure, repeated reconciliation, and ten controlled drift cases.

Crash-after-apply, crash-after-commit, and takeover cases use a child JVM that holds installed state independently of
the coordinator. Control binds only to `127.0.0.1`, requires an unguessable per-run key, validates canonical installed
snapshot bytes, performs value-based compare-and-set, caps requests and body sizes, stops after a bounded lifetime, and
is forcibly terminated if clean shutdown does not finish in time. The holder is proof-only; normal and production
router construction never supplies it.

The proof does not edit durable records to manufacture crash state. The coordinator's real failure checkpoints leave
the intent/commit boundary, while the independent holder preserves the router observation across coordinator
reconstruction.

The takeover result also runs the existing separate-process ownership proof. Its real JDK OS lock, stale-owner timeout,
higher-generation acquisition, restarted-prior-owner denial, and non-owner allocation denial must all pass together
with the external-holder allocation restoration. This is a bounded composition proof, not a claim that the proof-only
holder is a production ownership authority.

## Fail-closed behavior

- Unapplied durable intent is rejected without candidate installation.
- A router candidate without commit evidence is partial and returns to the verified baseline.
- A committed candidate after restart is recognized without duplicate commit, then follows the safe lifecycle policy
  back to baseline.
- Unknown, malformed, missing-backend, unexpected-backend, zero-total, normalization, or fingerprint evidence remains
  unavailable/unsafe; it is not silently normalized into readiness.
- Ownership generation changes reject stale mutation and prevent a stale commit.
- Failed or unverifiable restoration leaves readiness closed and preserves existing evidence.
- Repeated safe reconciliation performs no duplicate mutation, commit, generation change, or fingerprint change.

## Not proven

This local packaged proof does not prove production deployment, external traffic, real tenants, arbitrary private
networks, durable external routers, cross-host state, network-filesystem correctness, distributed consensus, performance,
availability, throughput, p95/p99, or unattended production recovery. The separate holder demonstrates only the bounded
local crash-window contract and is not available through the operator API.
