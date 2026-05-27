# Decision Explorer Phase 0 Verification Gate

Status: planned / docs-test-only.

Classification: WARN / decision-explorer-bootstrap.

Campaign slot: DX-G07.

Related architecture record: [`../adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md`](../adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md).

Related data contract: [`DECISION_EXPLORER_DATA_CONTRACT.md`](DECISION_EXPLORER_DATA_CONTRACT.md).

Related agent schema contract: [`DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md`](DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md).

Related evidence lane: [`DECISION_EXPLORER_EVIDENCE_LANE.md`](DECISION_EXPLORER_EVIDENCE_LANE.md).

Related campaign board: [`DECISION_EXPLORER_CAMPAIGN_BOARD.md`](DECISION_EXPLORER_CAMPAIGN_BOARD.md).

## Purpose

This document defines the planned Phase 0 verification gate for the future Interactive Decision Explorer. The gate is a
pre-implementation review contract for human reviewers and AI agents. It describes what must remain source-visible
before a later implementation campaign can be considered.

The gate is documentation only. It does not add endpoints, runtime behavior, UI implementation, storage behavior,
export behavior, replay execution, evidence-packet implementation, automation, deployment behavior, cloud behavior,
tenant behavior, production traffic-control behavior, Maven changes, Docker changes, Compose changes, CI changes,
scripts, secrets, credentials, or external targets.

## Phase 0 Gate Goals

The planned Phase 0 gate should help reviewers and agents confirm that:

- the Decision Explorer remains planned, read-only, simulation-only, and docs-test-only;
- ADR-0010, the data contract, the agent schema contract, and the evidence lane are source-visible;
- the contract language stays consistent enough for future structured JSON and visual UI work;
- source-card intake, stale-information retirement, repo bloat prevention, and compacting rules remain explicit;
- evidence packet references remain a future path and are not claimed as implemented;
- not-proven boundaries remain attached to the campaign before implementation planning continues.

The gate does not prove production readiness, production certification, live-cloud validation, real-tenant validation,
benchmark/load/stress evidence, throughput/p95/p99 evidence, replay/export behavior, storage behavior, runtime
endpoint/UI/storage/evidence-packet implementation, autonomous production action, or broader automation.

## Gate Inputs

Phase 0 gate input should be repository-local and source-visible:

| Input | Required signal |
| --- | --- |
| ADR-0010 | Defines the planned Decision Explorer architecture, constraints, alternatives, consequences, and not-proven boundaries. |
| Data contract | Defines `DecisionExplorerPayloadV1`, readout objects, versioning, null/unknown handling, and planning-only boundaries. |
| Agent schema contract | Defines agent consumption goals, stable identifiers, field naming rules, parseability, and no autonomous action. |
| Evidence lane | Defines source-card template, research intake rules, stale-information retirement policy, repo bloat prevention, compacting policy, and evidence packet future path. |
| Guard tests | Prove that source-visible documentation keeps the required boundary wording. |
| Campaign board | Keeps DX-G01 through DX-G10 scoped as a sequential docs/test-only campaign. |

Gate inputs must not include secrets, credentials, external targets, tenant targets, production-looking defaults,
private-network targets, raw research dumps, generated archives, raw benchmark logs, raw traces, exported packets,
runtime evidence stores, deployment commands, or mutation handles.

## Gate Outcomes

The planned gate has three review outcomes:

- `PASS`: all source-visible planning documents and guards preserve the required scope, boundaries, and not-proven
  language.
- `WARN`: required source-visible planning language exists, but reviewers should record stale wording, ambiguous
  linkage, missing compacting notes, or a verification gap before implementation planning proceeds.
- `BLOCK`: a source-visible control is missing, stale, unguarded, overclaiming, or implies implementation, production
  authority, live mutation, hidden side effects, or broader automation.

`PASS` is not production approval. It is only permission for a later scoped planning PR to discuss implementation slices.
`WARN` and `BLOCK` are not runtime enforcement states.

## Minimum Phase 0 Exit Criteria

Before a future implementation campaign can be proposed, reviewers should be able to verify:

- ADR-0010 exists and still names read-only and simulation-only architecture constraints;
- the data contract exists and still names planning-only versioned readout objects;
- the agent schema contract exists and still names stable identifiers, JSON field naming rules, null/unknown handling,
  enum stability, parseability, and no autonomous production action;
- the evidence lane exists and still names source-card template, research intake rules, stale-information retirement
  policy, repo bloat prevention, compacting policy, no raw research dumps, and evidence packet future path;
- guard tests exist for the Phase 0 gate and the prior Decision Explorer campaign surfaces;
- local verification and required remote checks are current-head green before any merge decision;
- post-merge main CI and CodeQL are verified before a later campaign slot opens its PR.

These criteria are pre-implementation criteria only. They do not create runtime endpoint/UI/storage/export/replay
implementation, evidence packet implementation, release evidence, deployment approval, traffic-control authority, or
production proof.

## Verification Matrix

| Area | Planned verification |
| --- | --- |
| Scope | Guard documentation states planned, docs-test-only, read-only, and simulation-only scope. |
| Architecture | Guard references ADR-0010 and rejects production authority or implementation claims. |
| Data contract | Guard references `DecisionExplorerPayloadV1`, `DecisionReadoutV1`, `CandidateReadoutV1`, and related readouts. |
| Agent schema | Guard references agent consumption goals, stable identifiers, JSON field naming rules, and parseability rules. |
| Evidence lane | Guard references source cards, intake rules, stale-information retirement, repo bloat prevention, compacting, and evidence packet future path. |
| Safety | Guard rejects production readiness, live-cloud, real-tenant, benchmark, throughput, replay/export, storage, runtime implementation, and broader automation claims. |
| Merge health | Gate treats stale, failed, cancelled, pending, or duplicate-only required checks as not green. |

The matrix is not CI configuration. It does not add workflows, required checks, rulesets, scripts, or automation.

## Agent Review Workflow

AI agents using this gate should:

1. Read the current branch diff before editing.
2. Confirm changed files stay within the active campaign slot.
3. Confirm prior Decision Explorer docs and guard tests remain source-visible.
4. Run focused guard verification first, then broader verification if the slot is ready.
5. Report the exact checks run, current head SHA, remote check links, and any still-pending main checks.
6. Preserve not-proven boundaries instead of shortening them away.

The workflow does not authorize autonomous production action, live mutation, hidden side effects, branch deletion,
ruleset changes, required-check weakening, endpoint creation, runtime UI/storage/export/replay implementation, evidence
packet implementation, or broader automation.

## Failure Handling

The Phase 0 gate should report `BLOCK` when:

- required current-head checks are failed, cancelled, pending, stale, or duplicate-only;
- the branch changes files outside the slot scope;
- a source-visible contract is removed or loses required boundary wording;
- source cards contain raw research dumps, secrets, external targets, production-looking defaults, or mutation handles;
- stale information is retained without a stale-information retirement note;
- compacting removes safety boundaries;
- wording claims production readiness, production certification, live-cloud validation, real-tenant validation,
  benchmark proof, throughput proof, replay/export proof, runtime implementation, evidence packet implementation, or
  broader automation.

Failure handling is a documentation review outcome only. It does not enforce runtime policy, block traffic, delete
artifacts, mutate environments, update rulesets, or change branch protection.

## Future Path

DX-G07 closes the pre-implementation guardrail layer for the bootstrap campaign. Later slots should remain bounded:

- DX-G08 should define implementation slices without implementing them in this campaign.
- DX-G09 should provide a reviewer walkthrough for human and AI-agent consumption.
- DX-G10 should close the bootstrap and preserve not-proven boundaries.

Future implementation work would require a separately scoped campaign, new PRs, current-head green checks, post-merge
main verification, and explicit proof before any claims change.

## Not-Proven Boundaries

This Phase 0 gate does not prove:

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
