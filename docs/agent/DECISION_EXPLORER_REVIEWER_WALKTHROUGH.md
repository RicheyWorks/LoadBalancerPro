# Decision Explorer Reviewer/Operator Walkthrough

Status: current / docs-and-guard normalization.

Classification: WARN / local reviewer evidence.

Historical campaign slot: DX-G09.

Normalization campaign slot: LASE-P6-PR5.

Related current API contract: [`../API_CONTRACTS.md`](../API_CONTRACTS.md).

Related reviewer trust map: [`../REVIEWER_TRUST_MAP.md`](../REVIEWER_TRUST_MAP.md).

Related Phase 6 naming anchor:
[`LASE_ROUTING_INTELLIGENCE_PHASE6_REVIEWER_EVIDENCE_NORMALIZATION.md`](LASE_ROUTING_INTELLIGENCE_PHASE6_REVIEWER_EVIDENCE_NORMALIZATION.md).

Historical bootstrap context remains in
[`DECISION_EXPLORER_IMPLEMENTATION_PLAN.md`](DECISION_EXPLORER_IMPLEMENTATION_PLAN.md) and
[`DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md`](DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md).

## Purpose

This walkthrough explains how a human reviewer, operator reviewer, or AI agent can inspect the current local Decision
Explorer surface without confusing reviewer evidence with production proof. The current source-controlled surface is:

- `/decision-explorer.html`, a same-origin static reviewer page;
- `GET /api/routing/decision-explorer/scenarios`, a read-only synthetic scenario catalog; and
- `POST /api/routing/decision-explorer`, a read-only and simulation-only payload surface grounded in routing
  comparison evidence already computed for the request.

The page and APIs are implemented repository surfaces, but this walkthrough is documentation and guard-test only. It
does not add or change Java classes, endpoints, API schemas, runtime behavior, UI behavior, routing, scoring, proxying,
allocation, traffic shifting, replay execution, storage, export, evidence-packet generation, Maven, CI, Docker,
Compose, scripts, secrets, credentials, cloud targets, tenant targets, private-network targets, external targets, or
automation.

## Pre-Walkthrough Gate

Before using this walkthrough, confirm:

- the application under review is a local instance built from the intended revision;
- `/decision-explorer.html` and the two same-origin API paths above match [`../API_CONTRACTS.md`](../API_CONTRACTS.md);
- `readOnly`, `simulationOnly`, warnings, unknowns, and `notProvenBoundaries` remain visible in returned evidence;
- any optional API key remains in browser memory only and is not persisted by the page;
- the active change, if any, has current local and remote verification; and
- required checks are complete and successful before a merge decision.

This gate is reviewer procedure, not runtime enforcement, branch protection, authorization, deployment approval,
traffic-control approval, production certification, or production-readiness evidence.

## Walkthrough Roles

| Role | Walkthrough focus | Boundary |
| --- | --- | --- |
| Human reviewer | Understand why a simulated routing comparison selected a candidate. | Review aid only; not production approval. |
| Operator reviewer | Inspect visible inputs, diagnostics, policy-gate display, warnings, and stop conditions. | No live mutation, traffic shifting, or runtime enforcement. |
| AI agent | Parse stable field names and boundary flags into a low-ambiguity summary. | Structured understanding only; no autonomous production action. |

The same returned evidence supports narrative review and agent-readable parsing without hidden writes, hidden approvals,
or hidden network calls from this walkthrough.

## Walkthrough Steps

### Step 1 - Confirm The Current Local Surface

Open `/decision-explorer.html` from the same local application origin. Confirm that the page identifies the payload as
`DecisionExplorerPayloadV1` and retains the `read-only`, `simulation-only`, and `Not-Proven Boundaries` cues.

The page is a local reviewer surface. Its presence does not prove a public deployment, production operation,
authorization boundary, or certification.

### Step 2 - Load The Scenario Catalog

Load the `Scenario Catalog` through `GET /api/routing/decision-explorer/scenarios`. Review scenario identifiers,
categories, evidence status, and warnings before selecting a sample.

The catalog contains deterministic local synthetic metadata. Loading or selecting a scenario does not by itself run
routing, mutate state, contact a cloud or tenant system, or shift traffic.

### Step 3 - Run The Read-Only Sample

Run a sample through `POST /api/routing/decision-explorer`. Confirm the response reports `readOnly: true`,
`simulationOnly: true`, the selected candidate, candidate set, warnings, unknowns, and `notProvenBoundaries`.

Treat the returned decision summary as an explanation aid. It is not routing authority, production traffic-control
authority, deployment approval, runtime enforcement, or certification.

### Step 4 - Map Panels To Normalized Evidence Groups

Use the Phase 6 vocabulary consistently:

| Static-page panel | Current API field or surface | Reviewer use |
| --- | --- | --- |
| Scenario Catalog | `GET /api/routing/decision-explorer/scenarios` | Orient the review with deterministic local scenario metadata. |
| Routing Intelligence Status | `confidenceSummary` | Review status, confidence details, warnings, unknowns, and explanation context. |
| Routing Diagnostics | `routingDiagnostics` | Inspect candidate, factor, evidence, degradation, partial-evidence, and unknown-evidence diagnostics. |
| Route Tradeoff Intelligence | `routeTradeoffAnalysis` | Compare the selected candidate with alternatives and inspect score-gap and factor-delta context. |
| Evidence Sufficiency | `routeTradeoffAnalysis.evidenceSufficiency` | Inspect whether returned evidence is sufficient for the bounded explanation. |
| Replay Readiness | `routeTradeoffAnalysis.replayReadinessDiagnostic` | Inspect readiness diagnostics without treating them as replay execution. |
| Shadow Decision Quality | `shadowDecisionQualityEvaluation` | Review local shadow-quality labels, outcomes, inputs, fingerprints, and explanations. |
| Counterfactual Analysis | `counterfactualAnalysis` | Review returned local policy-weight and factor-weight sensitivity rows. |
| Not-Proven Boundaries | `notProvenBoundaries` | Keep unsupported claims attached to every review. |

These fields organize already-returned reviewer evidence. They do not change the routing decision, recompute
production scores, retune live weights, or enforce policy.

### Step 5 - Compare Candidates, Factors, And Policy Gates

Use `decisionReadout`, `selectedCandidate`, `candidateSet`, and the available factor rows to compare selected and
non-selected candidates, visible factor contributions, known signals, and unknown or unavailable signals. Treat policy
gate states as display-only reviewer cues.

Do not infer hidden scoring, invent missing metrics, claim exact production scoring, or treat a displayed policy gate
as authorization, branch protection, required-check governance, deployment approval, or runtime enforcement.

### Step 6 - Inspect Tradeoffs, Replay Readiness, And Counterfactuals

Use `routeTradeoffAnalysis`, `shadowDecisionQualityEvaluation`, and `counterfactualAnalysis` only for bounded local
interpretation of returned evidence. Preserve fingerprints, reproducibility keys, warnings, explanations, partial
states, and unknown states when summarizing them.

Replay readiness is distinct from replay execution. Counterfactual rows do not retune live weights, recompute
production scores, mutate routing, write storage, export packets, call cloud or tenant targets, approve production
action, or prove benchmark/load/stress behavior.

### Step 7 - Validate Evidence And Structured Output

Review source references, warnings, unknowns, `agentStructuredOutput`, and any evidence-packet readout fields after the
human-readable summary. Preserve stable identifiers, JSON field names, enum values, null and unknown handling,
parseability, and low-ambiguity boundary flags.

An evidence-packet readout is not evidence-packet generation, storage, export, upload, download, PDF, or ZIP behavior.
Source references are reviewer context only; they do not prove live-cloud validation, real-tenant validation,
production readiness, replay/export proof, or storage proof.

### Step 8 - Record Review Questions And Stop Conditions

Record questions without converting the walkthrough into implementation proof. Stop and report when:

- a required source is missing, stale, unguarded, or overclaiming;
- current-head checks are stale, failed, cancelled, pending, skipped-only, or duplicate-only;
- returned evidence is missing its read-only, simulation-only, warning, unknown, or not-proven cues;
- wording implies replay execution, storage/export, evidence-packet generation, runtime enforcement, or production
  action that the scoped evidence does not prove;
- source material contains secrets, external targets, production-looking defaults, or mutation handles; or
- wording claims production readiness, production certification, live-cloud validation, real-tenant validation,
  benchmark/load/stress proof, throughput/p95/p99 proof, or broader automation.

Stop conditions are review signals only. They do not mutate repositories, change rulesets, delete branches, block
traffic, or approve deployment.

## AI-Agent Walkthrough Questions

An AI agent should be able to answer these questions from the current returned evidence without inventing facts:

- What payload version and scenario are being reviewed?
- Which candidate was selected, and which candidates were alternatives?
- What do `confidenceSummary` and `routingDiagnostics` say about evidence quality and unknowns?
- What tradeoffs, evidence-sufficiency state, and replay-readiness state are returned?
- What shadow-quality and counterfactual rows are present, partial, unavailable, or unknown?
- Which policy gates are display-only?
- Which source references, fingerprints, and reproducibility keys support the explanation?
- Which `notProvenBoundaries` apply?
- Which stop condition should block a merge or stronger claim?

These questions support structured understanding only. They do not create autonomous action, production approval,
runtime mutation, storage writes, export handles, replay behavior, or evidence-packet generation.

## Reviewer Checklist

Before treating the local readout as reviewable, confirm:

- the page and API paths are same-origin;
- the payload is read-only and simulation-only;
- optional API-key handling is memory-only;
- the panel labels map to the current normalized field names;
- warnings, unknowns, partial states, and not-proven boundaries remain visible;
- policy-gate visualization remains display-only;
- replay readiness is not described as replay execution;
- counterfactual evidence is not described as live retuning or production score recomputation;
- evidence-packet readouts are not described as generation, storage, or export; and
- unsupported behavior remains named as unknown, unavailable, not applicable, not implemented, or not proven.

## Historical Relationship

DX-G09 created the first source-visible reviewer walkthrough while Decision Explorer was still a planned bootstrap.
The historical contract stated that DX-G10 should close the bootstrap and preserve not-proven boundaries. DX-G10
subsequently closed that bootstrap and handed off to separately scoped implementation phases, which added the bounded
local page and API surfaces. LASE-P6-PR5 updates this walkthrough to describe that current repository evidence and the
Phase 6 normalized vocabulary; it does not retroactively turn the historical planning documents into runtime proof.

## Not-Proven Boundaries

This reviewer/operator walkthrough and the current bounded local surface do not prove:

- production readiness or production certification;
- live-cloud validation or real-tenant validation;
- runtime enforcement or production authorization;
- benchmark/load/stress evidence or throughput/p95/p99 evidence;
- production routing, scoring, proxy, or allocation behavior;
- replay execution or replay/export behavior;
- storage behavior or evidence-packet generation;
- traffic shifting or autonomous production action; or
- broader automation.
