# Decision Explorer Evidence Lane

Status: planned / docs-test-only.

Classification: WARN / decision-explorer-bootstrap.

Campaign slot: DX-G06.

Related data contract: [`DECISION_EXPLORER_DATA_CONTRACT.md`](DECISION_EXPLORER_DATA_CONTRACT.md).

Related agent schema contract: [`DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md`](DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md).

Related architecture record: [`../adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md`](../adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md).

Related campaign board: [`DECISION_EXPLORER_CAMPAIGN_BOARD.md`](DECISION_EXPLORER_CAMPAIGN_BOARD.md).

## Purpose

This document defines the planned Decision Explorer evidence lane and source-card intake contract. It describes how a
future Interactive Decision Explorer could connect one read-only, simulation-only decision explanation to
source-visible documentation, ADRs, guard tests, fixtures, workflow references, and future evidence packet references.

The evidence lane is documentation only. It does not add endpoints, runtime behavior, UI implementation, storage
behavior, export behavior, evidence-packet implementation, replay execution, automation, deployment behavior, cloud
behavior, tenant behavior, production traffic-control behavior, Maven changes, Docker changes, Compose changes, CI
changes, scripts, secrets, credentials, or external targets.

## Scope

DX-G06 plans two related surfaces:

- `DecisionExplorerEvidenceLaneV1`: a planned lane that groups evidence sources for a simulated decision explanation.
- `DecisionExplorerSourceCardV1`: a planned source-card shape that records source type, path, availability, freshness,
  trust posture, and not-proven boundaries for one source-visible item.

These names are planning vocabulary only. They do not create Java classes, JSON Schema files, endpoint responses, UI
models, persisted records, exports, replay execution, generated evidence packets, CI artifacts, or broader automation.

The lane should populate future `evidenceReferences`, `sourceReferences`, and `evidencePacketReadouts` fields without
claiming that runtime evidence packet generation, storage, export, replay, or UI exists.

## Evidence Lane Goals

The planned evidence lane should help human reviewers and AI agents:

- see which source-visible artifacts support a simulated Decision Explorer explanation;
- distinguish available source documents from planned, unavailable, stale, or not-applicable sources;
- identify source-card status without scraping prose;
- preserve freshness, trust, limitation, and boundary notes beside every source reference;
- avoid treating research notes, local-lab references, or planning documents as production certification;
- keep evidence intake read-only, simulation-only, and planning-only until separately scoped implementation exists.

Evidence lane goals do not create production proof. They do not prove production readiness, production certification,
live-cloud validation, real-tenant validation, benchmark/load/stress evidence, throughput/p95/p99 evidence,
replay/export behavior, storage behavior, runtime endpoint/UI/storage/evidence-packet implementation, autonomous
production action, or broader automation.

## Source Card Fields

Each planned `DecisionExplorerSourceCardV1` should use stable, low-ambiguity fields:

| Field | Meaning |
| --- | --- |
| `sourceCardId` | Stable card id inside the future evidence lane. |
| `sourceType` | Planned values: `DOC`, `ADR`, `GUARD_TEST`, `FIXTURE`, `WORKFLOW`, `LOCAL_LAB_REFERENCE`, `FUTURE_EVIDENCE_PACKET`, or `UNKNOWN`. |
| `sourcePath` | Repository-relative path when source-visible, otherwise an explicit unavailable marker. |
| `sourceTitle` | Human-readable title for the source. |
| `availability` | Planned values: `AVAILABLE`, `PLANNED`, `UNAVAILABLE`, `NOT_APPLICABLE`, `STALE`, or `UNKNOWN`. |
| `freshnessStatus` | Planned values: `CURRENT`, `STALE`, `PLANNED`, `UNVERIFIED`, `UNKNOWN`, or `NOT_APPLICABLE`. |
| `trustLabel` | Planned values: `SOURCE_VISIBLE`, `LOCAL_LAB_ONLY`, `PLANNING_ONLY`, `GUARD_TESTED`, or `UNKNOWN`. |
| `consumerUse` | Human and AI-agent use note. |
| `evidenceReferenceId` | Stable id suitable for `evidenceReferences`. |
| `sourceReferenceId` | Stable id suitable for `sourceReferences`. |
| `evidencePacketReadoutId` | Planned id for `evidencePacketReadouts`; no packet generation claim. |
| `boundaryNote` | Read-only, simulation-only, planning-only, and not-proven boundary statement. |
| `missingReason` | Required when a planned source is unavailable or not implemented. |

Source cards must not contain secrets, credentials, external targets, tenant targets, production-looking defaults,
write paths, upload/share/download handles, deployment instructions, replay execution handles, export handles,
approval commands, or mutation handles.

## Evidence Source Types

The initial source-card catalog should remain source-visible and repository-local:

- `DOC`: reviewer documentation such as DX contracts, README links, or trust-map entries;
- `ADR`: architecture decision records such as ADR-0010;
- `GUARD_TEST`: documentation guard tests that verify safety wording remains present;
- `FIXTURE`: checked-in example or test fixture references, if a later scoped PR explicitly adds them;
- `WORKFLOW`: workflow source references, not runtime check results unless separately scoped;
- `LOCAL_LAB_REFERENCE`: local-lab-only evidence references with explicit no-production-proof boundaries;
- `FUTURE_EVIDENCE_PACKET`: planned packet references only, with `NOT_IMPLEMENTED` packet generation;
- `UNKNOWN`: fallback for unclassified sources that must not be treated as proof.

Evidence source types describe provenance. They are not authorization, certification, deployment approval, runtime
enforcement, or production traffic-control authority.

## Intake Rules

Future evidence intake should follow these rules before any implementation:

- accept only source-visible, repository-relative paths or explicit unavailable markers;
- mark planned or unavailable evidence with `PLANNED`, `UNAVAILABLE`, `NOT_APPLICABLE`, `NOT_IMPLEMENTED`, or `UNKNOWN`;
- keep local-lab evidence separate from production validation claims;
- preserve not-proven boundaries on every source card;
- keep source-card vocabulary aligned with `DecisionExplorerPayloadV1`, `EvidencePacketReadoutV1`, and
  `AgentStructuredOutputV1`;
- avoid hidden scoring inference, hidden production telemetry inference, hidden live-cloud inference, or hidden tenant
  inference;
- reject source-card wording that implies runtime endpoint/UI/storage/export/replay behavior, evidence-packet
  implementation, autonomous production action, live mutation, hidden side effects, or broader automation.

## Source Card Intake Checklist

Before a future source card becomes part of a Decision Explorer explanation, reviewers should be able to answer:

- Is the source repository-relative and source-visible?
- Is the source type one of the planned values?
- Is availability explicit?
- Is freshness explicit?
- Is the trust label explicit?
- Does the card explain how humans and AI agents may use the source?
- Does the card preserve read-only and simulation-only boundaries?
- Does the card avoid production readiness, certification, live-cloud, real-tenant, benchmark, throughput, replay/export,
  runtime implementation, and automation claims?
- Does any missing source include a `missingReason`?

The checklist is planning only. It does not scan files, generate reports, create artifacts, comment on PRs, enforce
rules, mutate runtime behavior, or change CI.

## Static Example Source Card

This example is static documentation. It is not generated output, an endpoint response, a stored record, an exported
report, a replay artifact, or an evidence packet.

```json
{
  "laneVersion": "decision-explorer-evidence-lane/v1",
  "laneStatus": ["PLANNED", "DOCS_TEST_ONLY", "READ_ONLY", "SIMULATION_ONLY"],
  "sourceCards": [
    {
      "object": "DecisionExplorerSourceCardV1",
      "sourceCardId": "source-card-adr-0010",
      "sourceType": "ADR",
      "sourcePath": "docs/adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md",
      "sourceTitle": "ADR-0010 Interactive Decision Explorer Architecture",
      "availability": "AVAILABLE",
      "freshnessStatus": "CURRENT",
      "trustLabel": "PLANNING_ONLY",
      "consumerUse": "Human reviewers and AI agents may use this as architecture planning context only.",
      "evidenceReferenceId": "adr-0010",
      "sourceReferenceId": "source-adr-0010",
      "evidencePacketReadoutId": "NOT_IMPLEMENTED",
      "boundaryNote": "Read-only, simulation-only, planning-only source card; no production proof.",
      "missingReason": "NOT_APPLICABLE"
    },
    {
      "object": "DecisionExplorerSourceCardV1",
      "sourceCardId": "source-card-future-packet",
      "sourceType": "FUTURE_EVIDENCE_PACKET",
      "sourcePath": "UNAVAILABLE",
      "sourceTitle": "Future evidence packet reference",
      "availability": "PLANNED",
      "freshnessStatus": "PLANNED",
      "trustLabel": "PLANNING_ONLY",
      "consumerUse": "Agents must report that packet generation is not implemented.",
      "evidenceReferenceId": "future-evidence-packet",
      "sourceReferenceId": "source-future-packet",
      "evidencePacketReadoutId": "EvidencePacketReadoutV1",
      "boundaryNote": "No evidence packet generation, storage, export, replay execution, or proof claim.",
      "missingReason": "Evidence packet implementation is not part of DX-G06."
    }
  ],
  "boundaryFlags": {
    "planned": true,
    "readOnly": true,
    "simulationOnly": true,
    "notProductionProof": true,
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
  ]
}
```

## Relationship To Existing Contracts

- DX-G04 defines `evidenceReferences` and `EvidencePacketReadoutV1`.
- DX-G05 defines `sourceReferences`, `evidencePacketReadouts`, and agent parse-safety rules.
- DX-G06 defines this planned evidence lane and source-card intake contract.
- DX-G07 should define Phase 0 verification guardrails before implementation.
- DX-G08 should define implementation slices without implementing them in the bootstrap campaign.
- DX-G09 should provide a reviewer walkthrough for human and AI-agent consumption.
- DX-G10 should close the bootstrap and preserve not-proven boundaries.

## Validation Expectations

Future guard tests or schema work should verify that:

- `DecisionExplorerEvidenceLaneV1` and `DecisionExplorerSourceCardV1` remain planning vocabulary only;
- source-card fields remain stable and low-ambiguity;
- source types, availability, freshness, and trust labels stay explicit;
- source paths remain repository-relative or explicitly unavailable;
- evidence packet references remain `PLANNED` or `NOT_IMPLEMENTED` until separately implemented;
- read-only, simulation-only, planning-only, no live mutation, no hidden side effects, and no autonomous production
  action boundaries remain present;
- not-proven boundaries remain attached to every future lane and source-card example;
- no runtime endpoint/UI/storage/export/replay implementation claim, evidence-packet implementation claim, production
  proof claim, cloud proof claim, tenant proof claim, benchmark proof claim, throughput proof claim, or broader
  automation claim is introduced.

## Not-Proven Boundaries

This evidence lane does not prove:

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
