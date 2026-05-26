# Decision Explorer Agent Schema Contract

Status: planned / docs-test-only.

Classification: WARN / decision-explorer-bootstrap.

Campaign slot: DX-G05.

Related data contract: [`DECISION_EXPLORER_DATA_CONTRACT.md`](DECISION_EXPLORER_DATA_CONTRACT.md).

Related architecture record: [`../adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md`](../adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md).

Related campaign board: [`DECISION_EXPLORER_CAMPAIGN_BOARD.md`](DECISION_EXPLORER_CAMPAIGN_BOARD.md).

## Purpose

This document defines the planned AI-agent-readable schema contract for the future Interactive Decision Explorer. It
narrows the `AgentStructuredOutputV1` part of the DX-G04 data contract into stable field names, enum-like values,
parse-safety rules, boundary flags, and validation expectations.

The schema contract is documentation only. It is not a JSON Schema file, Java model, endpoint response, UI state model,
storage record, export format, replay format, evidence packet, generated artifact, CI artifact, automation hook, or
runtime dependency.

This document does not add endpoints, runtime behavior, UI implementation, storage behavior, export behavior,
evidence-packet implementation, replay execution, automation, deployment behavior, cloud behavior, tenant behavior,
production traffic-control behavior, Maven changes, Docker changes, Compose changes, CI changes, scripts, secrets,
credentials, or external targets.

## Scope

DX-G05 plans one agent-readable view named `AgentStructuredOutputV1`. A future implementation may embed this view inside
`DecisionExplorerPayloadV1`, but this document does not implement either object.

The schema contract supports AI-agent structured understanding by making these items parseable without scraping prose:

- the payload schema version and purpose;
- the explained decision, scenario, selected candidate, and candidate set;
- factor contribution readouts when visible data exists;
- policy gate readouts and their authorization boundaries;
- simulation-only decision diffs and what-if summaries;
- evidence packet references when available or explicitly planned;
- source-visible documentation and guard-test references;
- explicit unknown, unavailable, not-applicable, and not-implemented markers;
- not-proven boundaries and parse safety notes.

The contract remains planned, read-only, simulation-only, and docs/test-only. It makes no runtime
endpoint/UI/storage/export/replay/evidence-packet implementation claim, and it also carries this exact guard phrase:
no runtime endpoint/UI/storage/export/replay/evidence-packet implementation claim.

## Agent Schema Principles

- Parse-stable: field names should be durable within `decision-explorer-agent-schema/v1`.
- Additive: future optional fields may be added only when existing meanings remain stable.
- Boundary-first: agent outputs must carry planned, read-only, simulation-only, and not-production-proof flags.
- Evidence-visible: agents should receive repository-relative source references or explicit unavailable markers.
- No hidden inference: agents must not infer hidden scoring, hidden production telemetry, live-cloud state, tenant state,
  benchmark evidence, throughput evidence, replay execution, export proof, or production authorization.
- Human-aligned: structured output should match the human readout rather than create a separate authority path.
- Command-free: schema fields must not contain mutation handles, approval commands, deployment instructions, credentials,
  secrets, write paths, replay execution handles, export handles, or autonomous action hooks.

## Planned Schema Identity

| Field | Required | Planned value or meaning |
| --- | --- | --- |
| `schemaVersion` | yes | Planned value: `decision-explorer-agent-schema/v1`. |
| `schemaPurpose` | yes | Planned value: `AI_AGENT_STRUCTURED_UNDERSTANDING`. |
| `schemaStatus` | yes | Planned values: `PLANNED`, `DOCS_TEST_ONLY`, `READ_ONLY`, or `SIMULATION_ONLY`. |
| `payloadContractVersion` | yes | Link back to `decision-explorer-snapshot/v1`. |
| `payloadObject` | yes | Planned value: `DecisionExplorerPayloadV1`. |
| `agentObject` | yes | Planned value: `AgentStructuredOutputV1`. |
| `boundaryFlags` | yes | Boolean flags for planned, read-only, simulation-only, and not-production-proof state. |
| `parseSafetyNote` | yes | Instruction that agents must not infer authority, proof, hidden scoring, or runtime behavior. |

## Required Agent Fields

The future `AgentStructuredOutputV1` object should expose the following stable field families:

| Field | Meaning |
| --- | --- |
| `decisionReadout` | Structured `DecisionReadoutV1` summary for the explained decision. |
| `selectedCandidateId` | Selected candidate identifier when visible, otherwise an explicit marker. |
| `candidateReadouts` | Array of `CandidateReadoutV1` entries for selected and non-selected candidates. |
| `factorContributions` | Array of `FactorContributionV1` entries when contribution data is visible or unavailable. |
| `policyGateReadouts` | Array of `PolicyGateReadoutV1` entries with outcomes and authorization boundaries. |
| `decisionDiffReadouts` | Array of `DecisionDiffReadoutV1` entries for simulation-only comparisons. |
| `evidencePacketReadouts` | Array of `EvidencePacketReadoutV1` references; no packet generation or export claim. |
| `reasonCodes` | Stable machine-readable reason codes attached to the explanation. |
| `policyGateOutcomes` | Stable machine-readable gate outcomes. |
| `sourceReferences` | Repository-relative docs, ADRs, tests, fixtures, or explicit unavailable references. |
| `unknowns` | Explicit unknown, unavailable, not-applicable, and not-implemented markers. |
| `notProvenBoundaries` | Production, cloud, tenant, benchmark, throughput, replay/export, storage, and automation boundaries. |
| `validationNotes` | Agent-facing notes about parse safety, field freshness, unavailable data, and review limits. |

The required field families are planning vocabulary only. They do not create Java classes, JSON Schema files, endpoint
responses, UI models, persisted records, exports, replay execution, evidence packets, or broader automation.

## Enum-Like Values

Agents should receive explicit values instead of free-form implication where possible.

### Schema Status Values

- `PLANNED`
- `DOCS_TEST_ONLY`
- `READ_ONLY`
- `SIMULATION_ONLY`
- `UNAVAILABLE`
- `UNKNOWN`
- `NOT_APPLICABLE`
- `NOT_IMPLEMENTED`

### Candidate Status Values

- `SELECTED`
- `REJECTED`
- `UNAVAILABLE`
- `UNKNOWN`
- `NOT_APPLICABLE`

### Policy Gate Outcome Values

- `ALLOWED`
- `WARNED`
- `BLOCKED`
- `REQUIRES_REVIEW`
- `NOT_EVALUATED`
- `UNKNOWN`

### Evidence Status Values

- `AVAILABLE`
- `PLANNED`
- `UNAVAILABLE`
- `NOT_APPLICABLE`
- `STALE`
- `UNKNOWN`

### Boundary Flag Names

- `planned`
- `readOnly`
- `simulationOnly`
- `notProductionProof`
- `notProductionCertification`
- `notLiveCloudValidation`
- `notRealTenantValidation`
- `notBenchmarkProof`
- `notThroughputProof`
- `notReplayExportProof`
- `notRuntimeImplementation`
- `notBroaderAutomation`

## Unknown And Null Handling

Unknown and null handling must be explicit enough for agents to avoid invented certainty:

- use `UNKNOWN` when visible data exists but the schema cannot classify it;
- use `UNAVAILABLE` when data is intentionally not exposed;
- use `NOT_APPLICABLE` when a field does not apply to the scenario;
- use `NOT_IMPLEMENTED` for planned fields that do not exist in runtime code;
- use `null` only for scalar values that a later approved schema marks nullable;
- include companion fields such as `missingReason`, `unavailableReason`, `unknownSignalNote`, `notImplementedReason`,
  or `notApplicableReason` when absence affects interpretation;
- never convert a missing value into a production readiness, live-cloud validation, real-tenant validation, benchmark,
  throughput, replay/export, runtime endpoint/UI/storage/evidence-packet implementation, or automation claim.

## Agent Parse Safety Rules

The future schema must make these rules visible to every agent consumer:

- Do not treat explanation as authority.
- Do not choose live production routes from this schema.
- Do not mutate routing, scoring, strategy, proxy, cloud, tenant, deployment, storage, export, or production traffic
  state from this schema.
- Do not infer hidden scoring or hidden routing internals from absent fields.
- Do not infer production telemetry, production monitoring, live-cloud state, real-tenant state, benchmark proof, or
  throughput/p95/p99 proof.
- Do not treat policy gate visualization as branch protection, required-check governance, deployment approval, traffic
  shifting approval, or production authorization.
- Do not treat evidence packet readouts as implemented evidence packets, generated reports, persisted storage, exports,
  downloads, replay execution, or certification.
- Prefer `UNKNOWN`, `UNAVAILABLE`, `NOT_APPLICABLE`, or `NOT_IMPLEMENTED` over fabricated detail.

## Relationship To DX-G04 Objects

`AgentStructuredOutputV1` should remain aligned with the DX-G04 object vocabulary:

| DX-G04 object | Agent schema relationship |
| --- | --- |
| `DecisionExplorerPayloadV1` | Top-level planned payload that may carry the agent schema view. |
| `DecisionReadoutV1` | Source for `decisionReadout`. |
| `CandidateReadoutV1` | Source for `candidateReadouts`. |
| `FactorContributionV1` | Source for `factorContributions`. |
| `PolicyGateReadoutV1` | Source for `policyGateReadouts`. |
| `DecisionDiffReadoutV1` | Source for simulation-only `decisionDiffReadouts`. |
| `EvidencePacketReadoutV1` | Source for planned `evidencePacketReadouts` references only. |
| `AgentStructuredOutputV1` | The future agent-readable schema view described by this document. |

The relationship is source-visible planning only. It does not add runtime endpoint/UI/storage/export/replay behavior or
evidence-packet implementation.

## Static Example Agent Output

This example is static documentation. It is not an implemented endpoint response, generated JSON Schema, stored record,
exported report, replay output, or evidence packet.

```json
{
  "schemaVersion": "decision-explorer-agent-schema/v1",
  "schemaPurpose": "AI_AGENT_STRUCTURED_UNDERSTANDING",
  "schemaStatus": ["PLANNED", "DOCS_TEST_ONLY", "READ_ONLY", "SIMULATION_ONLY"],
  "payloadContractVersion": "decision-explorer-snapshot/v1",
  "payloadObject": "DecisionExplorerPayloadV1",
  "agentObject": "AgentStructuredOutputV1",
  "decisionReadout": {
    "object": "DecisionReadoutV1",
    "decisionId": "planned-simulated-decision-001",
    "scenarioId": "local-lab-simulation-example",
    "selectedCandidateId": "edge-alpha",
    "status": "PLANNED"
  },
  "candidateReadouts": [
    {
      "object": "CandidateReadoutV1",
      "candidateId": "edge-alpha",
      "candidateStatus": "SELECTED",
      "reasonCodes": ["HEALTHY_CANDIDATE", "LOWER_VISIBLE_LATENCY"],
      "unknownSignalNote": "Hidden routing internals are not inferred."
    }
  ],
  "factorContributions": [
    {
      "object": "FactorContributionV1",
      "factorId": "p95-latency",
      "availability": "AVAILABLE",
      "direction": "LOWER_IS_BETTER",
      "notProofNote": "Local simulated explanation only."
    }
  ],
  "policyGateReadouts": [
    {
      "object": "PolicyGateReadoutV1",
      "gateId": "reviewer-boundary-gate",
      "outcome": "REQUIRES_REVIEW",
      "authorizationBoundary": "Visualization does not approve production action."
    }
  ],
  "decisionDiffReadouts": [
    {
      "object": "DecisionDiffReadoutV1",
      "status": "SIMULATION_ONLY",
      "mutationBoundary": "No live mutation, storage write, export, or production action."
    }
  ],
  "evidencePacketReadouts": [
    {
      "object": "EvidencePacketReadoutV1",
      "status": "PLANNED",
      "packetGeneration": "NOT_IMPLEMENTED",
      "exportStatus": "NOT_IMPLEMENTED"
    }
  ],
  "reasonCodes": ["HEALTHY_CANDIDATE", "LOWER_VISIBLE_LATENCY", "NOT_PRODUCTION_PROOF"],
  "policyGateOutcomes": ["REQUIRES_REVIEW"],
  "sourceReferences": [
    "docs/agent/DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md",
    "docs/agent/DECISION_EXPLORER_DATA_CONTRACT.md",
    "docs/adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md"
  ],
  "unknowns": [
    {
      "field": "exactProductionScoring",
      "status": "UNAVAILABLE",
      "unavailableReason": "Production scoring and telemetry are not exposed by this planned schema."
    }
  ],
  "boundaryFlags": {
    "planned": true,
    "readOnly": true,
    "simulationOnly": true,
    "notProductionProof": true,
    "notProductionCertification": true,
    "notLiveCloudValidation": true,
    "notRealTenantValidation": true,
    "notBenchmarkProof": true,
    "notThroughputProof": true,
    "notReplayExportProof": true,
    "notRuntimeImplementation": true,
    "notBroaderAutomation": true
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
  ],
  "parseSafetyNote": "AI agents must not infer hidden scoring, production readiness, production certification, live-cloud validation, real-tenant validation, benchmark proof, throughput proof, replay/export proof, runtime implementation, authorization, or autonomous production action."
}
```

## Versioning And Stability Expectations

Future schema work should verify that:

- `schemaVersion` stays explicit and uses `decision-explorer-agent-schema/v1`;
- `schemaPurpose` stays `AI_AGENT_STRUCTURED_UNDERSTANDING`;
- required field families remain parse-stable for agents;
- enum-like values remain explicit and documented before agents depend on them;
- unknown, unavailable, not-applicable, and not-implemented semantics remain distinct;
- source references remain repository-relative or explicitly unavailable;
- boundary flags remain attached to every future agent output;
- static examples remain documentation only;
- future changes stay additive unless a new schema version and migration note are approved;
- no hidden production readiness, production certification, live-cloud validation, real-tenant validation, benchmark,
  throughput, replay/export, endpoint/UI/storage/evidence-packet implementation, or broader automation claim is added.

## Relationship To Future DX Slots

- DX-G05 defines this planned agent-readable schema contract.
- DX-G06 should define evidence lanes and source cards that can populate `sourceReferences` and `evidencePacketReadouts`.
- DX-G07 should define Phase 0 verification guardrails before implementation.
- DX-G08 should define implementation slices without implementing them in the bootstrap campaign.
- DX-G09 should provide a reviewer walkthrough for human and AI-agent consumption.
- DX-G10 should close the bootstrap and preserve not-proven boundaries.

## Not-Proven Boundaries

This agent schema contract does not prove:

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
