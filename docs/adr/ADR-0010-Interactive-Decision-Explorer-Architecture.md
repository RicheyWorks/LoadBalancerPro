# ADR-0010 Interactive Decision Explorer Architecture

## Status

Proposed / planning-only.

Decision type: architecture planning.

Implementation status: not implemented.

## Date

2026-05-26.

## Context

The Decision Explorer Architecture Bootstrap Campaign is tracked in
[`../agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md`](../agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md).
DX-G03 records the proposed architecture decision for the future Interactive Decision Explorer before any
data contract, schema, evidence lane, UI, endpoint, storage, or implementation plan is added.

The next-level architecture skeleton frames LoadBalancerPro as an enterprise adaptive routing lab with a future
evidence and replay cockpit, decision traces, what-if analysis, and reviewer proof paths. The Interactive Decision
Explorer should become the explanation surface for routing decisions, but during this bootstrap campaign it remains
planned, read-only, simulation-only, and docs/test-only.

This ADR does not add endpoints. This ADR does not add UI implementation. This ADR does not add runtime behavior.
This ADR does not add storage behavior. This ADR does not add evidence-packet implementation. This ADR does not add replay execution.
This ADR does not add export behavior. This ADR does not add automation, deployment behavior, cloud behavior, tenant
behavior, production traffic-control behavior, or broader automation.

This ADR does not prove production readiness or production certification. This ADR does not prove live-cloud or real-tenant validation.
This ADR does not prove benchmark/load/stress, throughput/p95/p99, replay/export,
runtime endpoint/UI/storage/evidence-packet implementation, or broader automation.

## Decision

If a future implementation campaign is separately approved, the Interactive Decision Explorer should be designed as a
read-only explanation architecture that supports human reviewer understanding and AI-agent structured understanding of
routing decisions.

The proposed architecture separates explanation from authority:

- the Decision Explorer may describe why a routing decision would be selected in a planned or simulated context;
- it must not mutate routing, scoring, strategy, proxy, cloud, tenant, deployment, or production traffic state;
- it must keep evidence and source references visible without turning evidence into runtime authority;
- it must expose not-proven boundaries beside any explanation;
- it must remain compatible with future data contract, schema, evidence lane, verification guardrail, implementation
  plan, reviewer walkthrough, and closeout slots.

This decision is not accepted implementation. It is a planning contract for future DX-G04 through DX-G10 work.

## Constraints

The Decision Explorer architecture is constrained by the repository trust boundary:

- planned only until separately scoped implementation work exists;
- read-only until a later implementation campaign proves otherwise;
- simulation-only during this bootstrap campaign;
- no runtime endpoint, UI, storage, evidence-packet, export, or automation implementation in this ADR;
- no production traffic mutation or autonomous production action;
- no cloud, tenant, deployment, secret, external-target, Docker, Compose, Maven, CI, or script changes;
- no production readiness, production certification, live-cloud validation, real-tenant validation,
  benchmark/load/stress proof, throughput/p95/p99 proof, replay/export proof, or broader automation claim.

## Conceptual Architecture

### Decision Intake Snapshot

The future Decision Explorer should explain a read-only snapshot of a routing decision candidate set. A future snapshot
may describe selected candidate, rejected candidates, routing signals, policy gates, safety mode, scoring inputs,
scenario label, and not-proven boundaries. The snapshot is a planned data contract only until DX-G04 or later scoped
work defines it.

### Explanation Model

The future explanation model should translate routing evidence into a human-readable narrative that helps reviewers and
operators answer why one route was preferred, why other candidates were rejected, which signals mattered, and which
guardrails limited the decision.

The explanation model must not become decision authority. It may explain decisions, compare simulated alternatives, and
surface uncertainty, but it must not choose live production routes or approve autonomous production action.

### Structured Agent View

The future structured agent view should provide stable, machine-readable fields for AI-agent structured understanding.
It should let an agent inspect decision reason codes, source references, confidence labels, policy-gate outcomes,
scenario context, and not-proven boundaries without scraping prose.

The structured view is planning-only in this ADR. It does not add JSON schemas, API responses, storage records,
downloadable artifacts, exported reports, or evidence packets.

## Human And AI-Agent Dual-Consumption Model

The future Decision Explorer should serve two consumers without letting either become runtime authority:

- human reviewers and operators should get compact explanations, visual summaries, uncertainty labels, and source links;
- AI agents should get structured JSON direction with stable field names, reason codes, policy-gate outcomes, evidence
  references, and not-proven boundary flags;
- both consumers should see the same read-only and simulation-only boundary language;
- neither consumer should receive mutation handles, production credentials, hidden approval paths, or autonomous action
  authority.

## Future Presentation Directions

The future presentation layer should be designed in separately scoped work. This ADR only records direction:

- structured JSON direction: future contracts should define machine-readable decision snapshots for AI-agent structured
  understanding without adding API responses in this ADR;
- visual UI direction: future reviewer surfaces may show selected routes, rejected candidates, signal weights, confidence
  labels, policy-gate outcomes, and not-proven boundaries without implementing UI in this ADR;
- what-if/counterfactual future path: future simulation-only views may compare how a decision could change if a signal,
  policy, or scenario changed, without executing replay or mutating live systems;
- policy gate visualization future path: future views may show which gates allowed, warned, blocked, or required operator
  review, without turning gate display into authorization;
- evidence packet future path: future work may connect explanations to evidence packet concepts, source cards, guard
  tests, and scenario fixtures, without implementing evidence packets, storage, export, or report artifacts in this ADR.

### Evidence And Source Lane

The future evidence and source lane should connect a decision explanation to source-visible controls, guard tests,
scenario fixtures, ADRs, and reviewer-facing docs. It should make the evidence path inspectable while keeping research,
local lab, and simulation evidence separate from production validation claims.

This lane must preserve provenance and freshness labels. Research notes, local lab results, and simulation outputs are
not production certification, live-cloud validation, real-tenant validation, benchmark/load/stress evidence, or
throughput/p95/p99 evidence unless a later scoped PR actually implements and verifies that evidence.

### Boundary And Authority Guard

The Decision Explorer should stay behind an authority guard:

- read-only explanation is allowed as a future design goal;
- simulation-only what-if comparison is allowed as a future design goal;
- live mutation is out of scope;
- runtime endpoint/UI/storage/evidence-packet implementation is out of scope for this ADR;
- production traffic-control behavior is out of scope;
- broader automation is out of scope.

## Relationship To Campaign Slots

- DX-G03 records this proposed architecture ADR.
- DX-G04 should define the future Decision Explorer data contract.
- DX-G05 should define the future AI-agent-readable schema contract.
- DX-G06 should define the future evidence lane and source cards.
- DX-G07 should define Phase 0 verification guardrails before implementation work.
- DX-G08 should define implementation slices without implementing them in this bootstrap campaign.
- DX-G09 should provide a reviewer walkthrough.
- DX-G10 should close the bootstrap and preserve not-proven boundaries.

## Consequences

- Reviewers get a single proposed architecture reference before later contract/schema work.
- Future agents get vocabulary for decision intake, explanation, structured agent view, evidence lane, and authority
  guard without inventing runtime behavior.
- Later implementation work must still be separately scoped and verified.
- This ADR keeps Decision Explorer claims bounded to planned, read-only, simulation-only architecture.

## Alternatives Considered

- Implement an endpoint or UI immediately: rejected for this campaign because DX-G03 is architecture planning only.
- Store decision explanations immediately: rejected because storage/export behavior must wait for a separate scoped
  contract and implementation plan.
- Treat explanation as runtime authority: rejected because the Decision Explorer must explain decisions without
  approving or mutating production traffic.
- Fold the ADR into the campaign board only: rejected because ADR-0010 gives later DX slots a stable architecture
  reference while the board remains campaign tracking.

## Not-Proven Boundaries

ADR-0010 does not prove:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- benchmark/load/stress evidence;
- throughput/p95/p99 evidence;
- replay/export behavior;
- runtime endpoint/UI/storage/evidence-packet implementation;
- autonomous production action;
- broader automation.

## Verification

DX-G03 is guarded by `Adr0010DecisionExplorerArchitectureDocumentationTest`.

Before a future DX-G03 PR can merge, required checks must emit and pass for the current head. After merge, main CI and
CodeQL must pass before DX-G03 is counted complete or a later DX slot is opened.
