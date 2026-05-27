# Decision Explorer Phase 1 Architecture And Scope

Status: active / phase1-scope.

Classification: WARN / decision-explorer-phase1-scope.

Campaign slot: DX-P1-G01.

Related Phase 1 campaign board: [`DECISION_EXPLORER_PHASE1_CAMPAIGN_BOARD.md`](DECISION_EXPLORER_PHASE1_CAMPAIGN_BOARD.md).

Related bootstrap closeout: [`DECISION_EXPLORER_BOOTSTRAP_CLOSEOUT.md`](DECISION_EXPLORER_BOOTSTRAP_CLOSEOUT.md).

Related architecture record: [`../adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md`](../adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md).

Related data contract: [`DECISION_EXPLORER_DATA_CONTRACT.md`](DECISION_EXPLORER_DATA_CONTRACT.md).

Related agent schema contract: [`DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md`](DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md).

Related evidence lane: [`DECISION_EXPLORER_EVIDENCE_LANE.md`](DECISION_EXPLORER_EVIDENCE_LANE.md).

Related Phase 0 gate: [`DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md`](DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md).

Related implementation plan: [`DECISION_EXPLORER_IMPLEMENTATION_PLAN.md`](DECISION_EXPLORER_IMPLEMENTATION_PLAN.md).

## Purpose

This document starts Decision Explorer Implementation Phase 1 with a source-visible architecture and scope contract.
It converts the bootstrap handoff into a bounded implementation campaign without changing runtime behavior in this
first slice.

DX-P1-G01 is documentation and guard-test only. It does not add Java runtime classes, controllers, routes, static UI
assets, storage behavior, export behavior, replay execution, evidence-packet generation, Maven configuration, CI
workflow behavior, Docker behavior, Compose behavior, scripts, deployment behavior, secrets, credentials, external
targets, cloud targets, tenant targets, rulesets, branch protection, or broader automation.

Decision Explorer Phase 1 remains additive, read-only, and simulation-only. Later scoped Phase 1 PRs may add Java DTOs,
builder logic, a bounded read-only API surface, and a reviewer UI, but each later PR must prove its own behavior with
focused tests, full local verification, current-head PR checks, and post-merge main checks before it counts.

## Starting Point

Phase 1 starts after the Decision Explorer Architecture Bootstrap Campaign completed on main at
`755ed394adfa18e462f89312c5289fd3154075f2`.

The bootstrap artifacts are treated as guardrails:

- ADR-0010 keeps the architecture read-only and simulation-only.
- The data contract defines `DecisionExplorerPayloadV1`, `DecisionReadoutV1`, `CandidateReadoutV1`,
  `FactorContributionV1`, `PolicyGateReadoutV1`, `DecisionDiffReadoutV1`, `EvidencePacketReadoutV1`, and
  `AgentStructuredOutputV1`.
- The agent schema contract preserves stable identifiers, JSON field naming rules, enum stability, null and unknown
  handling, parseability, and no autonomous production action.
- Exact Phase 1 schema wording: null and unknown handling must remain source-visible.
- The evidence lane preserves source-card templates, research intake rules, stale-information retirement policy,
  repo bloat prevention, compacting policy, no raw research dumps, and evidence packet future path.
- The Phase 0 gate preserves docs-before-code, stop conditions, current-head green PR checks, and post-merge main
  checks.

## Phase 1 Objective

Decision Explorer Phase 1 should create an additive reviewer-facing product surface for exploring routing decisions
and decision evidence already produced by LoadBalancerPro.

The implementation target is:

- deterministic summary objects that explain an already computed simulated routing decision;
- a service or builder that maps existing routing comparison and evidence responses into explorer-friendly sections;
- a bounded read-only API surface or additive response surface for explorer payloads;
- a static reviewer UI or cockpit integration that displays the explorer payload without mutation handles;
- documentation, reviewer navigation, and guard tests that preserve not-proven boundaries;
- a final handoff that lists PRs, merge commits, implemented behavior, verification, and remaining limits.

## Data Source Boundary

Phase 1 must reuse existing source-visible LoadBalancerPro decision evidence. It may read or transform already built
objects such as `RoutingComparisonResponse`, `RoutingComparisonResultResponse`, `RoutingDecisionVectorResponse`,
`CandidateDecisionVectorResponse`, `ScoreFactorContributionResponse`, `DominantFactorAnalysisResponse`,
`RoutingDecisionDeltaAnalysisResponse`, and existing Decision Replay evidence summaries where those objects are already
available.

Phase 1 must not invent hidden scoring signals, call cloud services, call tenant systems, open external network
connections, write storage, persist audit logs, execute replay, export packets, mutate routing state, mutate proxy
state, mutate cloud resources, or change production routing/scoring behavior.

Unknown, unavailable, partial, not-applicable, and not-implemented states must remain distinct. Absence must be visible
to both human reviewers and AI-agent structured consumers.

## API Boundary

The preferred Phase 1 API shape is a bounded read-only Decision Explorer payload derived from caller-provided simulated
routing comparison input or from an existing comparison response path. A later endpoint PR may choose a small additive
endpoint such as `POST /api/routing/decision-explorer` or an additive field on the existing routing comparison response,
but the chosen surface must be documented and tested in that PR.

Any Phase 1 API change must preserve these limits:

- read-only request handling;
- no allocation mutation;
- no proxy forwarding;
- no cloud, tenant, or external target call;
- no storage write;
- no replay execution;
- no export, upload, download, PDF, ZIP, or evidence-packet generation;
- explicit `notProvenBoundaries`;
- deterministic ordering for candidates, factors, warnings, source references, and boundary flags.

## UI Boundary

A later Phase 1 UI slice may add a static Decision Explorer page or an existing cockpit integration. The UI must consume
only the bounded API or static sample data created by a scoped PR. It must display:

- decision summary;
- selected candidate;
- candidate set;
- factor contributions;
- policy gate display-only status;
- warnings and unknowns;
- source or evidence references where available;
- fingerprints only when already present in existing evidence responses;
- not-proven boundaries.

The UI must not create server-side exports, uploads, downloads, PDFs, ZIPs, storage writes, replay execution, live
traffic shifting, production approval controls, branch-protection controls, required-check controls, cloud controls, or
autonomous production action.

## Phase 1 PR Sequence

The campaign should remain one small PR at a time:

| Slot | Slice | Expected boundary |
| --- | --- | --- |
| DX-P1-G01 | Architecture and scope contract | Documentation and guard only; no runtime behavior. |
| DX-P1-G02 | Backend DTO skeleton | Additive DTOs or records only; no endpoint or storage behavior. |
| DX-P1-G03 | Deterministic builder/service | Convert existing evidence into explorer sections without changing routing behavior. |
| DX-P1-G04 | Read-only API surface | Add bounded endpoint or response field with controller tests and API docs. |
| DX-P1-G05 | UI first pass | Add static reviewer UI or cockpit section without mutation or export handles. |
| DX-P1-G06 | UI polish and navigation | Improve labels, empty states, ordering, and reviewer links. |
| DX-P1-G07 | Docs and examples | Add grounded examples and guard unsupported claims. |
| DX-P1-G08 | Hardening | Add edge-case, null-safety, contract, and cross-link coverage. |
| DX-P1-G09 | Final handoff | Summarize merged PRs, behavior, verification, safety audit, and Phase 2 recommendation. |

The sequence may split further if a slice becomes too large. It must not stack PRs unless a blocker makes a non-stacked
flow impossible and the blocker is reported.

## Verification Expectations

Each Phase 1 PR must run the strongest practical local verification for its scope before opening a PR:

1. Focused tests for the active slice.
2. Relevant Decision Explorer selector bundle.
3. `mvn -q test`.
4. `mvn -q "-DskipTests" package`.
5. `mvn -B package`.
6. `git diff --check`.
7. `git diff --cached --check` when staged changes exist.
8. `git diff --check origin/main...HEAD`.
9. `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` when available and applicable.

Remote PR checks must be current-head green before merge: Build, Test, Package, Smoke; Analyze Java / CodeQL; and
Dependency Review when present. Exact Phase 1 merge-gate wording: Current-head PR CI, CodeQL, and Dependency Review
must be green when those checks are present. After merge, local main must be fast-forwarded, local verification must
pass, and main CI plus CodeQL must be green for the merge commit before the next PR starts.

## Scope And Safety Audit

Phase 1 allows additive implementation only inside explicitly scoped PRs. Any PR that changes production routing,
scoring, proxy behavior, Maven configuration, CI/workflow files, Dockerfile, Compose behavior, scripts, deployment,
secrets, external targets, cloud targets, tenant targets, rulesets, releases, tags, package publication, container
pushes, or GitHub settings must stop unless the user explicitly scopes that change.

Phase 1 must preserve no hidden side effects.

Phase 1 must preserve no autonomous production action.

Phase 1 must preserve no live mutation.

Phase 1 must preserve read-only and simulation-only boundaries for every Decision Explorer surface.

## Remaining Not-Proven Boundaries

This architecture and scope contract does not prove:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- runtime enforcement;
- benchmark/load/stress evidence;
- throughput/p95/p99 evidence;
- replay/export behavior;
- storage behavior;
- evidence packet implementation;
- runtime endpoint/UI/storage/evidence-packet implementation in this first slice;
- autonomous production action;
- broader automation.
