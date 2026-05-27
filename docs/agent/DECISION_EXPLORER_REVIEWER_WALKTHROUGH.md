# Decision Explorer Reviewer/Operator Walkthrough

Status: planned / docs-test-only.

Classification: WARN / decision-explorer-bootstrap.

Campaign slot: DX-G09.

Related implementation plan: [`DECISION_EXPLORER_IMPLEMENTATION_PLAN.md`](DECISION_EXPLORER_IMPLEMENTATION_PLAN.md).

Related Phase 0 gate: [`DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md`](DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md).

Related data contract: [`DECISION_EXPLORER_DATA_CONTRACT.md`](DECISION_EXPLORER_DATA_CONTRACT.md).

Related agent schema contract: [`DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md`](DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md).

Related evidence lane: [`DECISION_EXPLORER_EVIDENCE_LANE.md`](DECISION_EXPLORER_EVIDENCE_LANE.md).

Related campaign board: [`DECISION_EXPLORER_CAMPAIGN_BOARD.md`](DECISION_EXPLORER_CAMPAIGN_BOARD.md).

## Purpose

This walkthrough describes how a future reviewer, operator, or AI agent should inspect a planned Interactive Decision
Explorer readout. It is a source-visible review script for human reviewer understanding and AI-agent structured
understanding.

This walkthrough is documentation only. It does not add Java classes, endpoints, runtime behavior, UI implementation,
storage behavior, export behavior, replay execution, evidence-packet implementation, automation, deployment behavior,
cloud behavior, tenant behavior, production traffic-control behavior, Maven changes, Docker changes, Compose changes,
CI changes, scripts, secrets, credentials, or external targets.

The Decision Explorer remains planned, read-only, simulation-only, and docs-test-only. DX-G09 provides walkthrough
language only and makes no runtime endpoint/UI/storage/export/replay implementation claim.

## Pre-Walkthrough Gate

Before using this walkthrough, reviewers and agents should confirm:

- current `main` CI and CodeQL were green before the branch started;
- the active branch is scoped to the walkthrough document and guard test;
- the Decision Explorer implementation plan, Phase 0 gate, data contract, agent schema contract, evidence lane, and
  campaign board remain source-visible;
- the planned readout still uses read-only and simulation-only language;
- no endpoint, UI, runtime storage, export, replay, or evidence packet behavior is claimed as implemented;
- current-head PR checks are complete and successful before any merge decision.

This gate is not runtime enforcement, branch protection, authorization, deployment approval, traffic-control approval,
or production readiness evidence.

## Walkthrough Roles

The planned walkthrough supports three reader roles:

| Role | Walkthrough focus | Boundary |
| --- | --- | --- |
| Human reviewer | Understand why a simulated decision appears in a planned readout. | Review aid only; not production approval. |
| Operator reviewer | Check visible inputs, policy gate display, source cards, and stop conditions. | No live mutation, no traffic shifting, no runtime enforcement. |
| AI agent | Parse stable structured fields and boundary flags for low-ambiguity summaries. | Structured understanding only; no autonomous production action. |

The same source-visible readout should support both narrative review and agent-readable parsing without hidden side
effects.

## Walkthrough Steps

### Step 1 - Confirm Source-Visible Scope

Start with the source-visible planning documents. The reviewer should confirm that the active readout is described as
planned, read-only, simulation-only, and docs-test-only.

Expected source cues:

- `Decision Explorer Reviewer/Operator Walkthrough`;
- `DecisionExplorerPayloadV1`;
- `AgentStructuredOutputV1`;
- `DECISION_EXPLORER_IMPLEMENTATION_PLAN.md`;
- `DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md`;
- `DECISION_EXPLORER_EVIDENCE_LANE.md`.

Stop if the wording claims runtime endpoint/UI/storage/export/replay implementation, evidence packet implementation,
production readiness, live-cloud validation, real-tenant validation, benchmark proof, throughput proof, or broader
automation.

### Step 2 - Read The Planned Decision Summary

Inspect the planned `DecisionReadoutV1` section first. A future readout should identify the simulated decision, selected
candidate, visible strategy name, known visible signals, unknown or unavailable signals, and boundary flags.

The reviewer should treat the decision summary as an explanation aid. It must not be interpreted as routing authority,
production traffic-control authority, deployment approval, runtime enforcement, or certification.

### Step 3 - Compare Candidate Readouts

Review each planned `CandidateReadoutV1` and `FactorContributionV1` item. The walkthrough should help readers compare:

- selected and non-selected candidates;
- visible factor contribution names;
- visible factor contribution values;
- known visible signals;
- unknown or unexposed signals;
- exactness boundaries and not-proven boundaries.

Candidate comparison must not infer hidden scoring, invent missing metrics, mutate routing state, shift live traffic, or
claim exact production scoring.

### Step 4 - Review Policy Gate Visualization

Inspect the planned `PolicyGateReadoutV1` area next. The walkthrough should make policy gate visualization display-only.
Future gate states may include `ALLOWED`, `WARNED`, `BLOCKED`, `REQUIRES_REVIEW`, `NOT_EVALUATED`, and `UNKNOWN`.

Policy gate visualization is not authorization, branch protection, required-check governance, deployment approval,
production traffic-control approval, and not runtime enforcement.

### Step 5 - Inspect What-If And Counterfactual Readouts

Review the planned `DecisionDiffReadoutV1` section only as a simulation-only explanation path. What-if and
counterfactual readouts may help explain how visible inputs could change a planned decision summary.

They must not execute replay, mutate routing, write storage, export packets, call cloud or tenant targets, approve
production action, or claim benchmark/load/stress proof.

### Step 6 - Validate Evidence And Source Cards

Use the evidence lane to inspect source cards and planned evidence references. The walkthrough should preserve:

- source-card template fields;
- research intake rules;
- stale-information retirement policy;
- repo bloat prevention;
- compacting policy;
- no raw research dumps;
- evidence packet future path.

Evidence and source cards are reviewer context only. They do not prove live-cloud validation, real-tenant validation,
production readiness, replay/export proof, storage proof, or evidence-packet implementation.

### Step 7 - Read Agent Structured Output

Inspect planned `AgentStructuredOutputV1` fields after the human summary. Agent output should preserve stable
identifiers, including exact stable identifiers wording, JSON field naming rules, enum stability expectations, null and
unknown handling, including exact null and unknown handling wording, parseability, and low-ambiguity boundary flags.

Agent structured output supports AI-agent structured understanding. It must not authorize autonomous production action,
live mutation, no hidden writes, no hidden approvals, no hidden network calls, or broader automation.

### Step 8 - Record Review Questions And Stop Conditions

The reviewer should record questions without converting the walkthrough into implementation proof. Stop before
Java/backend/UI/runtime work; exact stop before Java/backend/UI/runtime work wording applies when:

- a required planning source is missing, stale, unguarded, or overclaiming;
- current-head checks are stale, failed, cancelled, pending, skipped-only, or duplicate-only;
- wording implies endpoint/UI/runtime/storage/export/replay/evidence-packet implementation exists;
- source cards contain raw research dumps, secrets, external targets, production-looking defaults, or mutation handles;
- wording claims production readiness, production certification, live-cloud validation, real-tenant validation,
  benchmark proof, throughput/p95/p99 proof, replay/export proof, storage proof, or broader automation.

Stop conditions are review signals only. They do not enforce runtime policy, mutate repositories, change rulesets,
delete branches, block traffic, or approve deployment.

## AI-Agent Walkthrough Questions

An AI agent should be able to answer these planned questions from a future readout without inventing facts:

- What source-visible payload version is being reviewed?
- Which candidate was selected, and which candidates were not selected?
- Which visible factors contributed to the explanation?
- Which policy gate display state is shown?
- Which fields are unknown, unavailable, not applicable, or not implemented?
- Which source cards support the explanation?
- Which not-proven boundaries apply?
- Which stop condition should block implementation or merge work?

These questions support structured understanding only. They do not create autonomous action, production approval,
runtime mutation, storage writes, export handles, replay behavior, or evidence packet generation.

## Reviewer Checklist

Before treating a planned readout as reviewable, confirm:

- the readout states planned, read-only, simulation-only, and docs-test-only scope;
- `DecisionExplorerPayloadV1`, `DecisionReadoutV1`, `CandidateReadoutV1`, `FactorContributionV1`,
  `PolicyGateReadoutV1`, `DecisionDiffReadoutV1`, `EvidencePacketReadoutV1`, and `AgentStructuredOutputV1` remain
  planning vocabulary;
- source references are repository-relative or explicitly unavailable;
- source cards are compact and do not include raw research dumps;
- policy gate visualization remains display-only;
- what-if and counterfactual language remains simulation-only;
- evidence packet references remain a future path;
- no runtime endpoint/UI/storage/export/replay implementation claim is present;
- not-proven boundaries remain visible.

## Relationship To Future DX Slots

- DX-G09 provides this reviewer/operator walkthrough for human and AI-agent consumption.
- DX-G10 should close the bootstrap and preserve not-proven boundaries.
- Future implementation work should be a separate Decision Explorer Implementation Phase 1 campaign with explicit
  scope, current-head green checks, post-merge main verification, and new proof before any claims change.

## Not-Proven Boundaries

This reviewer/operator walkthrough does not prove:

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
