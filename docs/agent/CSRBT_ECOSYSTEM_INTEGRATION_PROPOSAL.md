# CSRBT Ecosystem Integration Proposal (WARN-classified planning surface)

This document is a WARN-classified planning surface only. It is a proposal ledger for
scoped, lab-mode-only integration of RicheyWorks CSRBT-ecosystem libraries into
LoadBalancerPro. It is not implementation permission, not a merged capability, not
production behavior, and not evidence. No lane below is complete until its own PR is
merged under the campaign rules and main checks are green.

This proposal does not relax the README trust contract. It does not authorize Maven config
changes, CI/workflow changes, Dockerfile changes, Compose behavior changes, runtime
behavior changes, endpoint changes, secrets, external/cloud/tenant targets, or
production-looking defaults outside an explicitly scoped, separately approved PR per lane.

## What is being proposed, at claim level

The CSRBT ecosystem is a twelve-engine, zero-runtime-dependency, Java 17 library family by
the same author ([map](https://github.com/RicheyWorks/SuperBeefSort/blob/main/docs/ECOSYSTEM.md)):
an adaptive ordered index with exact O(log n) order statistics
([CSRBT](https://github.com/RicheyWorks/CSRBT)), a log-structured record store whose
append-only CRC'd log is the only truth
([SmokeHouse](https://github.com/RicheyWorks/SmokeHouse)), and derived engines (views,
time travel, crash-atomic batches, cold archives) that are all rebuildable caches of that
log. Every engine ships seeded oracle tests and composed integration tests
([WholeHog](https://github.com/RicheyWorks/WholeHog)).

The claim-level fit: LoadBalancerPro's trust posture is built on controlled lab evidence,
local reproducibility, and explicit proof boundaries. SmokeHouse's design doctrine — the
log is the only truth; every index is a rebuildable cache; a crash can never lose evidence
that was durably written — is the same posture expressed as a storage engine. CSRBT's
order statistics give exact (not estimated) sliding-window percentiles, which is the
substrate the LASE Core Expansion ledger's tail-latency-aware scoring goals need.

## Dependency posture (a named precondition, not a lane)

The ecosystem artifacts (`io.github.richeyworks:*:0.1.0`) currently install via
`publishToMavenLocal` only. **Precondition P0:** no lane that adds a Maven dependency may
merge until either (a) the artifacts are published to Maven Central (the ecosystem's own
open Phase 9 item), or (b) a reviewer-approved local-lab-only resolution posture is
documented in the lane's PR with the same explicitness as the Compose readiness gate.
Local-lab docs must state that mavenLocal resolution is manual, local, and not CI-proof.
The dependency direction is one-way: LoadBalancerPro consumes the libraries; it does not
join the ecosystem's composite build, and the ecosystem takes no dependency on
LoadBalancerPro.

## Proposed lanes (each PR-sized, each separately scoped and approved)

### Lane E1 — Lab-mode allocation-evidence store (SmokeHouse, embedded)

Append every allocation decision (request descriptor, candidate readouts, decision vector,
chosen target, evaluation metrics) as a record in an embedded SmokeHouse store, behind a
lab-only configuration flag that is off by default. Deliverables: the evidence-record
codec, the append path in the allocation facade's lab seam (no endpoint changes), seeded
oracle tests (TreeMap reference, in the ecosystem's house style and this repo's
tested-invariant style), and reopen/replay tests proving the evidence trail survives a
crash by construction. Claim boundary: this is lab evidence capture, not
replay/evidence/report/storage/export proof in the README's sense until a PR says so.

### Lane E2 — Exact tail-latency scoring substrate (CSRBT order statistics)

A lab-mode per-backend sliding-window latency tracker backed by CSRBT's windowed ordered
set: `percentileKey(95)`/`percentileKey(99)` are exact order-statistics walks, not sketch
estimates. Exposed to the load-distribution planner/evaluator as an optional scoring input
behind configuration, off by default. Deliverables: the tracker, property tests comparing
against brute-force percentile computation on the same window, and a scenario-evidence lab
run in the existing local-lab manner. Claim boundary: this enables tail-latency-aware
scoring experiments in lab mode; it is not throughput/p95/p99 production evidence.

### Lane E3 — Reviewer-facing decision-history views (Renderer over E1)

Materialized counts and rankings (decisions per backend, per strategy, per outcome class)
folded live off the E1 store's tail, for reviewer/operator surfaces. Depends on E1.
Deliverables: view definitions, fold-vs-brute-force oracle tests, and wiring into an
existing reviewer surface only if that surface's own scope allows it in the same PR.

### Lane E4 — Evidence retention and archival (DryAge + Jerky over E1) — ledger-only

Preserve evidence-store generations at scenario boundaries; cure them into CRC-verified
archives for the evidence directory. Recorded here for completeness; not proposed for
implementation until E1 has merged and a reviewer names a retention requirement.

### Lane E5 — Anti-thrash strategy-promotion gates (MorphPolicy pattern) — ledger-only

CSRBT's promotion gates (cooldown, minimum improvement, stability wins) are the shape the
adaptive routing policy would need to avoid strategy thrash under regime shifts. Recorded
as a design pointer for the LASE ledger; any implementation is its own scoped campaign.

## Verification expectations

Each lane follows VERIFICATION_PROTOCOL.md: focused checks while editing, full local
verification before merge, current-head remote checks before merge, post-merge main checks
before the lane counts. Each lane's PR states what was actually verified and nothing more.
Failures land in FAILURE_LOG.md.

## What this proposal does not claim

It does not prove production readiness, production certification, live-cloud validation,
real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99
production evidence, replay/evidence/report/storage/export proof, or broader automation.
It does not make the CSRBT ecosystem a supported production dependency of LoadBalancerPro;
it proposes lab-mode library use behind flags that default off, one bounded PR at a time.
