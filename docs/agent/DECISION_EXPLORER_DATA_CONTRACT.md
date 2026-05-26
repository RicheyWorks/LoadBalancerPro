# Decision Explorer Data Contract

Status: planned / docs-test-only.

Classification: WARN / decision-explorer-bootstrap.

Campaign slot: DX-G04.

Related architecture record: [`../adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md`](../adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md).

Related campaign board: [`DECISION_EXPLORER_CAMPAIGN_BOARD.md`](DECISION_EXPLORER_CAMPAIGN_BOARD.md).

## Purpose

This document defines the planned Decision Explorer data contract for a future Interactive Decision Explorer. It gives
reviewers, operators, and AI agents a shared vocabulary for one read-only, simulation-only routing decision explanation.

The contract is schema-like documentation, not a JSON Schema runtime dependency. It does not add endpoints, runtime
behavior, UI implementation, storage behavior, export behavior, evidence-packet implementation, replay execution,
automation, deployment behavior, cloud behavior, tenant behavior, production traffic-control behavior, Maven changes,
Docker changes, Compose changes, CI changes, scripts, secrets, credentials, or external targets.

## Scope

The planned contract describes a future payload family named `decision-explorer-snapshot/v1`. A future payload should
help a reviewer or AI agent understand:

- which simulated decision is being explained;
- which selected route or backend is being described;
- which candidate routes or backends were visible;
- which signals, policy gates, reason codes, and evidence references were visible;
- which what-if and counterfactual notes are simulation-only;
- which evidence packet references are planned or unavailable;
- which not-proven boundaries remain attached to the explanation.

The data contract is planning only until a later scoped implementation PR defines DTOs, JSON schemas, endpoint behavior,
UI behavior, storage behavior, export behavior, or evidence-packet behavior.

This planning-only language is intentional: DX-G04 records names, field families, and reviewer expectations so later
work can evolve from a stable vocabulary. It makes no runtime endpoint/UI/storage/export/replay implementation claim.

## Contract Principles

- Planned: the data contract is a future-facing planning surface.
- Read-only: the payload must not include mutation handles, production credentials, command fields, write methods, or
  approval authority.
- Simulation-only: what-if and counterfactual fields must describe simulated explanation context only.
- Human-readable: reviewers should be able to read concise labels, summaries, warnings, and links.
- AI-agent readable: agents should be able to parse stable field names, enum-like values, reason codes, policy-gate
  outcomes, evidence references, and explicit boundary flags without scraping prose.
- Source-visible: contract meaning should remain visible in repository documentation and guard tests.
- Boundary-preserving: every future payload should carry not-proven boundaries beside its explanation.

## Required Top-Level Fields

| Field | Required | Meaning |
| --- | --- | --- |
| `contractVersion` | yes | Version string. Planned value: `decision-explorer-snapshot/v1`. |
| `contractStatus` | yes | Planning status. Planned values: `PLANNED`, `SIMULATION_ONLY`, or `READ_ONLY_REVIEW`. |
| `decisionExplorerMode` | yes | Scope marker. Planned values: `PLANNED`, `READ_ONLY`, and `SIMULATION_ONLY`. |
| `decisionId` | yes | Stable decision identifier when available; otherwise an explicit not-available marker. |
| `scenarioId` | yes | Simulated scenario or reviewer context label. |
| `selectedCandidate` | yes | Selected route/backend summary for the simulated decision. |
| `candidateSet` | yes | Visible selected and rejected candidates. |
| `signalSummary` | yes | Visible routing signals and unavailable signal markers. |
| `policyGateSummary` | yes | Gate outcomes such as allowed, warned, blocked, or requires-review. |
| `reasonCodes` | yes | Stable machine-readable reason codes for the explanation. |
| `humanExplanation` | yes | Reviewer-facing summary, cautions, and source links. |
| `agentExplanation` | yes | Structured fields for AI-agent understanding. |
| `evidenceReferences` | yes | Source-visible docs, tests, ADRs, fixtures, or future evidence lane references. |
| `whatIfPreview` | yes | Simulation-only counterfactual notes; no replay execution or mutation. |
| `notProvenBoundaries` | yes | Explicit proof boundaries that must travel with the explanation. |

## Planned V1 Objects

The future data contract should use explicit V1 object names before any implementation creates DTOs, schemas, endpoint
responses, UI state, storage records, exports, replay records, or evidence packets.

| Object | Purpose |
| --- | --- |
| `DecisionExplorerPayloadV1` | Top-level planned payload wrapper for one read-only, simulation-only explanation. |
| `DecisionReadoutV1` | Human and machine-readable readout for the selected simulated decision. |
| `CandidateReadoutV1` | Candidate route/backend readout for selected, rejected, unavailable, or unknown candidates. |
| `FactorContributionV1` | Visible factor contribution readout when factor data is returned or explicitly unavailable. |
| `PolicyGateReadoutV1` | Policy-gate display readout with outcome, reason codes, and authorization boundary. |
| `DecisionDiffReadoutV1` | Simulation-only selected-vs-alternative or what-if/counterfactual readout. |
| `EvidencePacketReadoutV1` | Planned evidence packet reference shape; no packet generation, storage, export, or proof claim. |
| `AgentStructuredOutputV1` | AI-agent structured output view with stable field names, reason codes, boundary flags, and parse notes. |

These object names are contract vocabulary only. They do not create Java classes, JSON Schema files, runtime resources,
endpoint responses, UI models, persisted records, exports, replay execution, evidence packets, or broader automation.

## Versioning Rules

- V1 payloads should use `contractVersion: decision-explorer-snapshot/v1`.
- V1 object names should remain additive and parse-stable for future guard tests.
- New optional fields may be added only when they preserve read-only and simulation-only meaning.
- Existing field names should not be renamed without a new version and migration note.
- Enum-like values should prefer explicit known values plus `UNKNOWN`, `UNAVAILABLE`, or `NOT_APPLICABLE`.
- Version bumps must preserve not-proven boundaries and must not imply implementation, endpoint, UI, storage, export,
  replay, evidence-packet, production, cloud, tenant, benchmark, throughput, or automation proof.

## Unknown And Null Handling

Unknown and null handling should keep absence explicit:

- use `UNKNOWN` when visible data exists but the explanation cannot classify it;
- use `UNAVAILABLE` when a field is intentionally not exposed;
- use `NOT_APPLICABLE` when the field does not apply to the scenario;
- use `null` only for explicitly nullable scalar values documented by a future schema;
- include a companion reason field such as `missingReason`, `unknownSignalNote`, or `unavailableReason` when absence
  affects reviewer or agent interpretation;
- never infer hidden scoring, hidden routing internals, production telemetry, production monitoring, live-cloud state,
  tenant state, or benchmark evidence from null or missing fields.

## Schema Stability Expectations

Future schema work should make `AgentStructuredOutputV1` stable enough for AI-agent structured understanding:

- stable top-level object names;
- stable field names for selected decision, candidates, factors, gates, diffs, evidence references, and boundaries;
- stable reason code and policy gate outcome vocabularies;
- source-visible field descriptions;
- explicit nullable and unavailable semantics;
- deterministic ordering where future arrays need reviewer comparison;
- no hidden command fields, mutation handles, credentials, export handles, replay execution handles, or production
  authorization handles.

## Candidate Object

Each `candidateSet` item should use stable fields:

| Field | Meaning |
| --- | --- |
| `candidateId` | Stable candidate identifier from simulated input or returned evidence. |
| `candidateLabel` | Human-readable route/backend label. |
| `selected` | Boolean marker for the selected candidate. |
| `candidateStatus` | Planned values: `SELECTED`, `REJECTED`, `UNAVAILABLE`, `UNKNOWN`, or `NOT_APPLICABLE`. |
| `visibleSignals` | Array of signal references that are visible in the simulated context. |
| `reasonCodes` | Stable reason codes attached to this candidate. |
| `policyGateOutcomes` | Policy gate outcomes attached to this candidate. |
| `evidenceReferenceIds` | References to source-visible evidence items. |
| `unknownSignalNote` | Required when visible data cannot explain selection or rejection. |
| `boundaryNote` | Candidate-specific read-only and simulation-only boundary. |

Candidate objects must not infer hidden routing internals. If visible evidence does not explain a candidate outcome, the
contract should mark that reason as unknown from visible data.

## Signal Summary

The `signalSummary` object should keep visible and unavailable data separate:

| Field | Meaning |
| --- | --- |
| `visibleSignalCount` | Count of visible signal entries. |
| `unavailableSignalCount` | Count of unavailable, missing, or intentionally unexposed signals. |
| `signals` | Array of visible signal entries. |
| `unavailableSignals` | Array of unavailable signal descriptors. |
| `hiddenInternalsPolicy` | Must state that hidden routing internals are not inferred. |
| `scoringAvailability` | Planned values: `RETURNED`, `PARTIAL`, `NOT_EXPOSED`, or `UNKNOWN`. |

Signal entries may describe health, latency, load pressure, queue pressure, capacity, weight, error rate,
network-awareness risk, policy warnings, or future read-only external signal context only when those values are visible.

## Policy Gate Summary

The `policyGateSummary` object should make gate display inspectable without turning display into authorization.

| Field | Meaning |
| --- | --- |
| `gateId` | Stable gate identifier. |
| `gateLabel` | Human-readable label. |
| `outcome` | Planned values: `ALLOWED`, `WARNED`, `BLOCKED`, `REQUIRES_REVIEW`, `NOT_EVALUATED`, or `UNKNOWN`. |
| `reasonCodes` | Machine-readable reason codes for the gate outcome. |
| `sourceReferenceIds` | Links to evidence references or docs that explain the gate. |
| `authorizationBoundary` | Must state that visualization does not approve production action. |

Policy gate fields are reviewer explanation data only. They are not branch protection, required-check governance,
deployment approval, traffic shifting approval, or production authorization.

## Human Explanation

The `humanExplanation` object should include:

- `summary`: concise reviewer-facing explanation;
- `selectedCandidateLabel`: selected route/backend label;
- `rejectedCandidateLabels`: visible rejected candidates;
- `keySignals`: compact signal list;
- `cautionNotes`: uncertainty, unavailable fields, and boundary reminders;
- `sourceLinks`: repository docs, ADRs, tests, or future evidence lane references.

This section should help humans understand the simulated decision without implying production validation.

## Agent Explanation

The `agentExplanation` object should include:

- `schemaPurpose`: `AI_AGENT_STRUCTURED_UNDERSTANDING`;
- `stableFieldNames`: explicit list of parse-stable field names;
- `reasonCodes`: machine-readable reason code list;
- `policyGateOutcomes`: machine-readable gate outcome list;
- `evidenceReferenceIds`: machine-readable source reference list;
- `boundaryFlags`: booleans such as `planned`, `readOnly`, `simulationOnly`, and `notProductionProof`;
- `parseSafetyNote`: reminder that agents must not infer hidden scoring, production readiness, or authorization.

AI-agent fields should support structured understanding, not autonomous production action.

## Evidence References

The `evidenceReferences` array should keep source-visible provenance explicit:

| Field | Meaning |
| --- | --- |
| `referenceId` | Stable reference id inside the payload. |
| `referenceType` | Planned values: `DOC`, `ADR`, `GUARD_TEST`, `FIXTURE`, `WORKFLOW`, `FUTURE_EVIDENCE_LANE`, or `UNKNOWN`. |
| `path` | Repository-relative path when source-visible. |
| `status` | Planned values: `AVAILABLE`, `PLANNED`, `UNAVAILABLE`, or `NOT_APPLICABLE`. |
| `freshnessNote` | Reviewer note about current, planned, or unavailable evidence. |
| `boundaryNote` | Boundary attached to the evidence reference. |

Evidence references do not prove production readiness, production certification, live-cloud validation, real-tenant
validation, benchmark/load/stress evidence, throughput/p95/p99 evidence, replay/export proof, storage proof, or broader
automation.

## What-If Preview

The `whatIfPreview` object is reserved for future simulation-only counterfactual explanation. It may describe possible
changes to visible signals, policy gates, or candidate ordering, but it must not execute replay, mutate live systems,
write storage, export artifacts, or approve production action.

Planned fields:

- `status`: `PLANNED`, `UNAVAILABLE`, or `SIMULATION_ONLY`;
- `counterfactualSummary`: reviewer-facing description;
- `changedSignalRefs`: visible signal ids that the preview discusses;
- `expectedExplanationChange`: explanation text only;
- `mutationBoundary`: required no-mutation statement.

## Static Example Payload

This example is static documentation. It is not an implemented endpoint response, stored record, exported report, or
evidence packet.

```json
{
  "contractVersion": "decision-explorer-snapshot/v1",
  "contractStatus": "PLANNED",
  "decisionExplorerMode": ["PLANNED", "READ_ONLY", "SIMULATION_ONLY"],
  "decisionId": "planned-simulated-decision-001",
  "scenarioId": "local-lab-simulation-example",
  "selectedCandidate": {
    "candidateId": "edge-alpha",
    "candidateLabel": "edge-alpha",
    "selected": true,
    "candidateStatus": "SELECTED",
    "visibleSignals": ["health-state", "p95-latency", "queue-depth"],
    "reasonCodes": ["HEALTHY_CANDIDATE", "LOWER_VISIBLE_LATENCY"],
    "policyGateOutcomes": ["ALLOWED"],
    "evidenceReferenceIds": ["adr-0010", "campaign-board"],
    "unknownSignalNote": "Hidden routing internals are not inferred.",
    "boundaryNote": "Read-only simulation explanation only; no production action is approved."
  },
  "candidateSet": [
    {
      "candidateId": "edge-alpha",
      "candidateLabel": "edge-alpha",
      "selected": true,
      "candidateStatus": "SELECTED",
      "visibleSignals": ["health-state", "p95-latency", "queue-depth"],
      "reasonCodes": ["HEALTHY_CANDIDATE", "LOWER_VISIBLE_LATENCY"],
      "policyGateOutcomes": ["ALLOWED"],
      "evidenceReferenceIds": ["adr-0010"],
      "unknownSignalNote": "Hidden routing internals are not inferred.",
      "boundaryNote": "Read-only simulation explanation only."
    }
  ],
  "signalSummary": {
    "visibleSignalCount": 3,
    "unavailableSignalCount": 1,
    "signals": ["health-state", "p95-latency", "queue-depth"],
    "unavailableSignals": ["exact-production-scoring"],
    "hiddenInternalsPolicy": "Do not infer hidden routing internals.",
    "scoringAvailability": "NOT_EXPOSED"
  },
  "policyGateSummary": [
    {
      "gateId": "reviewer-boundary-gate",
      "gateLabel": "Reviewer boundary gate",
      "outcome": "REQUIRES_REVIEW",
      "reasonCodes": ["NOT_PRODUCTION_PROOF"],
      "sourceReferenceIds": ["data-contract"],
      "authorizationBoundary": "Visualization does not approve production action."
    }
  ],
  "humanExplanation": {
    "summary": "Visible simulated signals explain why edge-alpha is preferred in this planned example.",
    "selectedCandidateLabel": "edge-alpha",
    "rejectedCandidateLabels": [],
    "keySignals": ["healthy state", "lower visible p95 latency", "lower visible queue depth"],
    "cautionNotes": ["No live-cloud validation", "No real-tenant validation", "No production readiness proof"],
    "sourceLinks": ["docs/agent/DECISION_EXPLORER_DATA_CONTRACT.md"]
  },
  "agentExplanation": {
    "schemaPurpose": "AI_AGENT_STRUCTURED_UNDERSTANDING",
    "stableFieldNames": ["contractVersion", "decisionId", "candidateSet", "reasonCodes"],
    "reasonCodes": ["HEALTHY_CANDIDATE", "LOWER_VISIBLE_LATENCY", "NOT_PRODUCTION_PROOF"],
    "policyGateOutcomes": ["ALLOWED", "REQUIRES_REVIEW"],
    "evidenceReferenceIds": ["adr-0010", "campaign-board", "data-contract"],
    "boundaryFlags": {
      "planned": true,
      "readOnly": true,
      "simulationOnly": true,
      "notProductionProof": true
    },
    "parseSafetyNote": "AI agents must not infer hidden scoring, production readiness, or authorization."
  },
  "plannedObjects": {
    "payload": "DecisionExplorerPayloadV1",
    "decisionReadout": "DecisionReadoutV1",
    "candidateReadout": "CandidateReadoutV1",
    "factorContribution": "FactorContributionV1",
    "policyGateReadout": "PolicyGateReadoutV1",
    "decisionDiffReadout": "DecisionDiffReadoutV1",
    "evidencePacketReadout": "EvidencePacketReadoutV1",
    "agentStructuredOutput": "AgentStructuredOutputV1"
  },
  "evidenceReferences": [
    {
      "referenceId": "adr-0010",
      "referenceType": "ADR",
      "path": "docs/adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md",
      "status": "AVAILABLE",
      "freshnessNote": "Architecture planning reference.",
      "boundaryNote": "Planning-only ADR; no runtime implementation."
    }
  ],
  "whatIfPreview": {
    "status": "SIMULATION_ONLY",
    "counterfactualSummary": "A future view may explain how visible signal changes could alter the explanation.",
    "changedSignalRefs": ["p95-latency"],
    "expectedExplanationChange": "Explanation text only; no replay execution.",
    "mutationBoundary": "No live mutation, storage write, export, or production action."
  },
  "notProvenBoundaries": [
    "production readiness",
    "production certification",
    "live-cloud validation",
    "real-tenant validation",
    "benchmark/load/stress evidence",
    "throughput/p95/p99 evidence",
    "replay/export/storage proof",
    "runtime endpoint/UI/storage/evidence-packet implementation",
    "broader automation"
  ]
}
```

## Validation Expectations

Future guard tests or schema work should verify that:

- `contractVersion` stays explicit;
- planned, read-only, and simulation-only boundaries are present;
- human and AI-agent consumption paths are both represented;
- selected and rejected candidate fields remain source-visible;
- reason codes and policy gate outcomes remain machine-readable;
- evidence references remain repository-relative or explicitly unavailable;
- what-if fields remain simulation-only and no-mutation;
- not-proven boundaries remain attached to the payload;
- `DecisionExplorerPayloadV1`, `DecisionReadoutV1`, `CandidateReadoutV1`, `FactorContributionV1`,
  `PolicyGateReadoutV1`, `DecisionDiffReadoutV1`, `EvidencePacketReadoutV1`, and `AgentStructuredOutputV1` remain
  planning-only vocabulary until separately implemented;
- versioning rules, unknown/null handling, and schema stability expectations remain explicit;
- no hidden production readiness, live-cloud validation, real-tenant validation, benchmark, throughput, replay/export,
  endpoint/UI/storage/evidence-packet implementation, or broader automation claim is introduced.

## Relationship To Future DX Slots

- DX-G04 defines this planned data contract.
- DX-G05 should define a more explicit AI-agent-readable schema contract.
- DX-G06 should define evidence lanes and source cards that can populate `evidenceReferences`.
- DX-G07 should define Phase 0 verification guardrails before implementation.
- DX-G08 should define implementation slices without implementing them in the bootstrap campaign.
- DX-G09 should provide a reviewer walkthrough for human and AI-agent consumption.
- DX-G10 should close the bootstrap and preserve not-proven boundaries.

## Not-Proven Boundaries

This data contract does not prove:

- production readiness;
- production certification;
- live-cloud validation;
- real-tenant validation;
- benchmark/load/stress evidence;
- throughput/p95/p99 evidence;
- replay/export behavior;
- storage behavior;
- runtime endpoint/UI/storage/evidence-packet implementation;
- autonomous production action;
- broader automation.
