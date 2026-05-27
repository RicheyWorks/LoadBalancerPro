# Decision Explorer Implementation Plan

Status: planned / docs-test-only.

Classification: WARN / decision-explorer-bootstrap.

Campaign slot: DX-G08.

Related Phase 0 gate: [`DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md`](DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md).

Related architecture record: [`../adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md`](../adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md).

Related data contract: [`DECISION_EXPLORER_DATA_CONTRACT.md`](DECISION_EXPLORER_DATA_CONTRACT.md).

Related agent schema contract: [`DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md`](DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md).

Related evidence lane: [`DECISION_EXPLORER_EVIDENCE_LANE.md`](DECISION_EXPLORER_EVIDENCE_LANE.md).

Related campaign board: [`DECISION_EXPLORER_CAMPAIGN_BOARD.md`](DECISION_EXPLORER_CAMPAIGN_BOARD.md).

Related bootstrap closeout: [`DECISION_EXPLORER_BOOTSTRAP_CLOSEOUT.md`](DECISION_EXPLORER_BOOTSTRAP_CLOSEOUT.md).

## Purpose

This document defines a planned implementation path for the future Interactive Decision Explorer. It translates the
bootstrap architecture, data contract, agent schema contract, evidence lane, and Phase 0 verification gate into future
implementation slices that a later separately scoped campaign could consider.

This plan is documentation only. It does not add Java classes, endpoints, runtime behavior, UI implementation, storage
behavior, export behavior, replay execution, evidence-packet implementation, automation, deployment behavior, cloud
behavior, tenant behavior, production traffic-control behavior, Maven changes, Docker changes, Compose changes, CI
changes, scripts, secrets, credentials, or external targets.

## Implementation Plan Goals

The planned implementation path should help reviewers and AI agents understand:

- which documentation must exist before code;
- which future implementation slices should remain read-only and simulation-only;
- which contracts each slice must preserve;
- which stop conditions block Java, backend, UI, storage, export, replay, endpoint, or evidence-packet work;
- which verification expectations must be green before a future implementation PR can merge;
- which not-proven boundaries must stay attached to every future slice.

The implementation is future work. DX-G08 provides planning-only language and makes no runtime
endpoint/UI/storage/export/replay implementation claim.

The plan does not approve implementation in this bootstrap campaign. It does not prove production readiness, production
certification, live-cloud validation, real-tenant validation, benchmark/load/stress evidence, throughput/p95/p99
evidence, replay/export behavior, storage behavior, runtime endpoint/UI/storage/evidence-packet implementation,
autonomous production action, or broader automation.

## Required Docs Before Code

Before any future Java/backend/UI/runtime work is proposed, reviewers should verify that these source-visible documents
and guards exist and still pass:

| Required source | Required signal |
| --- | --- |
| README and Reviewer Trust Map links | Decision Explorer remains discoverable as planned, read-only, simulation-only, and not production proof. |
| ADR-0010 | Architecture constraints, alternatives, consequences, and not-proven boundaries are source-visible. |
| Data contract | `DecisionExplorerPayloadV1`, readout objects, versioning, null/unknown handling, and schema stability remain planned. |
| Agent schema contract | `AgentStructuredOutputV1`, stable identifiers, JSON field naming rules, enum stability, parseability, and no autonomous production action remain explicit. |
| Evidence lane | Source-card template and source-card template fields, research intake rules, stale-information retirement, repo bloat prevention, compacting policy, no raw research dumps, and evidence packet future path remain explicit. |
| Phase 0 verification gate | Required docs, boundaries, stop conditions, and current-head green check rules remain source-visible. |

If any required source is missing, stale, unguarded, overclaiming, or no longer current-head green after merge, future
implementation work should stop before code.

This means future implementation work should stop before code whenever a required planning source is not current,
guarded, and bounded.

## Planned Implementation Slices

The slices below are a future planning sequence only. They are not implemented by DX-G08.

| Slice | Future intent | Required boundary |
| --- | --- | --- |
| Slice 0 - Contract freeze | Reconfirm ADR, data contract, agent schema, evidence lane, and Phase 0 gate before code. | Documentation and guard review only; no runtime behavior. |
| Slice 1 - Readout vocabulary | Create future DTO or schema vocabulary for `DecisionExplorerPayloadV1`, `DecisionReadoutV1`, `CandidateReadoutV1`, `FactorContributionV1`, `PolicyGateReadoutV1`, `DecisionDiffReadoutV1`, `EvidencePacketReadoutV1`, and `AgentStructuredOutputV1`. | Read-only, simulation-only objects; no endpoint, storage, export, replay, or evidence packet generation. |
| Slice 2 - Snapshot assembly | Map visible simulated routing explanation data into the planned payload vocabulary. | No hidden scoring inference, no live mutation, no production traffic control, no cloud or tenant calls. |
| Slice 3 - Human readout | Add future reviewer-facing summaries, caution notes, source links, and policy gate visualization. | Display only; not authorization, deployment approval, or branch protection. |
| Slice 4 - Agent structured output | Add future parse-stable `AgentStructuredOutputV1` fields and boundary flags. | Supports structured understanding only; no autonomous production action or hidden side effects. |
| Slice 5 - Evidence lane binding | Link future source cards to `evidenceReferences`, `sourceReferences`, and planned `evidencePacketReadouts`. | Repository-relative references only; no raw research dumps, generated packets, storage, export, or replay. |
| Slice 6 - What-if preview | Add simulation-only decision diffs and counterfactual readouts. | Explanation text only; no live replay, mutation, traffic shifting, storage writes, or production action. |
| Slice 7 - Reviewer walkthrough | Provide human and AI-agent walkthrough material after implementation planning remains bounded. | Walkthrough only; not production proof or runtime certification. |

Every future slice should be independently scoped, reviewed, tested, and merged only after current-head PR checks and
post-merge main CI/CodeQL are green.

## Future Slice Families

The future backend model slices would define read-only DTOs or schema objects for `DecisionExplorerPayloadV1`,
`DecisionReadoutV1`, `CandidateReadoutV1`, `FactorContributionV1`, `PolicyGateReadoutV1`,
`DecisionDiffReadoutV1`, `EvidencePacketReadoutV1`, and `AgentStructuredOutputV1`. These slices would not create
mutation paths, production routing authority, storage writes, export handles, replay execution, endpoint behavior, or
evidence packet generation.

The future endpoint slices would be separately scoped only after the model slices and guards are proven. Any future
endpoint discussion must remain read-only and simulation-only until a later PR defines and verifies actual behavior.
DX-G08 does not add controllers, routes, API resources, OpenAPI changes, runtime configuration, or network behavior.

The future static UI slices would describe reviewer-facing display of a simulated decision explanation, candidate
readouts, visible factors, policy gate visualization, source cards, and boundary notes. DX-G08 does not add static UI
assets, templates, frontend code, browser behavior, runtime resources, storage, export, or replay behavior.

The future what-if/counterfactual slices would remain simulation-only. They may explain how visible signals could alter
a planned explanation, but they must not execute replay, mutate routing, shift traffic, write storage, call cloud or
tenant targets, export packets, or approve production action.

The future policy gate visualization slices would display gate outcomes such as allowed, warned, blocked, requires
review, not evaluated, or unknown. Visualization is not authorization, branch protection, required-check governance,
deployment approval, production traffic-control approval, or runtime enforcement.

The future evidence packet renderer slices would be planned only until a separate scoped campaign proves them. A future
renderer may format `EvidencePacketReadoutV1` references for reviewers, but DX-G08 does not implement packet generation,
storage, export, download, upload, replay execution, persistence, certification, or production proof.

## Test Strategy

The test strategy for a later implementation campaign should be incremental:

- guard tests should continue to prove docs-before-code, read-only, simulation-only, and not-proven boundaries;
- model tests should verify DTO or schema vocabulary without adding endpoint, storage, export, replay, or production
  behavior;
- endpoint tests, if later scoped, should prove read-only behavior, no mutation handles, no production-looking defaults,
  and explicit not-implemented states for unavailable behavior;
- static UI tests, if later scoped, should verify source-visible display text, boundary flags, and accessibility without
  claiming runtime production proof;
- what-if and policy gate tests should prove simulation-only display and no authorization semantics;
- evidence packet renderer tests should prove planned or not-implemented states until a future implementation provides
  separately verified behavior.

The test strategy does not add tests in DX-G08 beyond this documentation guard, and it does not change Maven or CI.

## Verification Strategy

The verification strategy for future implementation slices should require:

- focused guard verification before broader suites;
- relevant Decision Explorer documentation selector bundles;
- full Maven tests and package verification;
- enterprise lab smoke package only as local-lab evidence, not production proof;
- current-head PR CI, CodeQL, and Dependency Review before merge;
- post-merge main CI and CodeQL before a later slot opens;
- exact failure reporting when checks fail, including test class, job, step, and run link.

The verification strategy is planning only. It does not add workflows, required checks, rulesets, scripts, release
automation, deployment automation, or runtime enforcement.

## Branch/PR Sequence

The branch/PR sequence for a later implementation campaign should stay one scoped PR at a time:

1. Open a branch from a main commit whose CI and CodeQL are green.
2. Keep changed files inside the active slice.
3. Run focused local verification before broad verification.
4. Open a PR only after local verification is current.
5. Merge only after current-head PR CI, CodeQL, and Dependency Review are green.
6. Fast-forward local main after merge.
7. Verify post-merge main CI and CodeQL before opening the next PR.

The branch/PR sequence does not delete branches, weaken rulesets, weaken required checks, or authorize stacked
implementation PRs in this bootstrap campaign.

## Stop Conditions Before Java, Backend, Or UI Work

Future implementation work should stop before Java/backend/UI/runtime changes when:

- required docs before code are absent, stale, unguarded, or inconsistent;
- README or Reviewer Trust Map wording loses planned, read-only, simulation-only, or not-proven boundaries;
- ADR verification, data contract verification, agent schema verification, evidence lane verification, or Phase 0 gate
  verification fails;
- branch diffs include unscoped production code, Maven, Docker, Compose, scripts, endpoints, runtime resources,
  deployment, secrets, credentials, external targets, cloud targets, tenant targets, or automation;
- required PR checks are stale, failed, cancelled, pending, or duplicate-only;
- post-merge main CI and CodeQL are not completed successfully for the previous slot;
- wording implies endpoint/UI/runtime/storage/export/replay/evidence-packet implementation already exists;
- wording claims production readiness, production certification, live-cloud validation, real-tenant validation,
  benchmark proof, throughput proof, replay/export proof, storage proof, or broader automation.

Stop conditions are review gates only. They do not enforce runtime policy, mutate repositories, update rulesets, change
branch protection, or block traffic.

## Slice Verification Expectations

Future implementation slices should use a verification ladder before merge:

1. Focused guard for the active Decision Explorer slice.
2. Relevant Decision Explorer docs selector bundle.
3. Full Maven tests.
4. Package verification.
5. Enterprise lab smoke package where applicable.
6. Whitespace checks for the working tree, staged diff, and `origin/main...HEAD`.
7. Current-head PR CI, CodeQL, and Dependency Review.
8. Post-merge main CI and CodeQL before a later slot opens.

This verification ladder does not add CI workflows, Maven configuration, Docker behavior, Compose behavior, scripts,
rulesets, required checks, or automation in DX-G08.

## Future Non-Goals

Future implementation planning must keep these non-goals visible until separately scoped proof exists:

- no live mutation;
- no autonomous production action;
- no production traffic shifting;
- no live-cloud or real-tenant validation claim;
- no benchmark/load/stress or throughput/p95/p99 proof claim;
- no replay/export/storage proof claim;
- no runtime endpoint/UI/storage/export/replay implementation claim;
- no runtime endpoint/UI/storage/evidence-packet implementation claim in this bootstrap campaign;
- no evidence packet generation, persistence, download, upload, or certification claim;
- no hidden network calls, hidden writes, hidden approvals, or hidden side effects;
- no hidden side effects.

## Review Checklist

Before any future implementation PR is opened, reviewers and agents should confirm:

- the active branch starts from a main commit whose CI and CodeQL are green;
- the branch contains only the scoped files for that future implementation slice;
- read-only and simulation-only boundaries are attached to new DTOs, schemas, services, UI plans, or examples;
- `UNKNOWN`, `UNAVAILABLE`, `NOT_APPLICABLE`, and `NOT_IMPLEMENTED` remain distinct where absence matters;
- source references remain repository-relative or explicitly unavailable;
- policy gate visualization remains display-only and not authorization;
- what-if and counterfactual behavior remains simulation-only;
- evidence packet references remain planned or not implemented until a separate PR proves otherwise;
- not-proven boundaries are repeated in docs, tests, PR body, and final report.

The checklist is planning only. It does not create endpoint behavior, runtime UI, storage, export, replay, evidence
packet generation, deployment behavior, cloud behavior, tenant behavior, automation, or production proof.

## Relationship To Future DX Slots

- DX-G08 defines this planned implementation path without implementing it in the bootstrap campaign.
- DX-G09 should provide a reviewer walkthrough for human and AI-agent consumption.
- DX-G10 should close the bootstrap and preserve not-proven boundaries.
- DX-G10 should use [`DECISION_EXPLORER_BOOTSTRAP_CLOSEOUT.md`](DECISION_EXPLORER_BOOTSTRAP_CLOSEOUT.md) to record
  the final merge-health gate and hand off only to a separately scoped Decision Explorer Implementation Phase 1.

Future implementation work should be a separate Decision Explorer Implementation Phase 1 campaign with explicit scope,
current-head green checks, post-merge main verification, and new proof before any claims change.

## Not-Proven Boundaries

This implementation plan does not prove:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- benchmark/load/stress evidence;
- throughput/p95/p99 evidence;
- replay/export behavior;
- storage behavior;
- runtime endpoint/UI/storage/evidence-packet implementation;
- evidence packet implementation;
- autonomous production action;
- broader automation.
