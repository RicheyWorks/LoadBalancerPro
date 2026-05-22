# WorkloadProfile Signal Metadata Design Contract

This document defines a future architecture contract for `WorkloadProfile` signal metadata. It describes how LoadBalancerPro could eventually represent workload shape, sensitivity, and reviewer evidence metadata for AI-era workload modeling, LASE multi-objective evaluation, and future read-only Tier 2/Tier 3 signal context.

This is a design contract only. It is not implemented in this sprint, not an API contract, not a Java runtime record/class, not a workload model feature, not a signal ingestion feature, and not proof of production readiness.

## Executive Summary

`WorkloadProfile` is a future metadata concept for describing the request or workload shape around a routing comparison. The current product remains focused on Tier 1 L4-L7 adaptive routing experimentation and evidence. Future WorkloadProfile metadata would be optional reviewer context unless a later approved implementation explicitly defines, tests, and documents runtime semantics.

The contract exists so future workload metadata starts from a safe shape:

- documentation-only vocabulary;
- deterministic fixture support;
- explicit unknown/unavailable states;
- clear separation from `ExternalSignalPort`;
- reviewer-facing evidence notes;
- no routing behavior change;
- no scoring behavior change;
- no strategy behavior change;
- no proxy behavior change;
- no production readiness claim.

## Current Status: Design Contract Only, Not Implemented

Current status:

- design contract only;
- documentation-only pseudocode;
- no Java source files added for this concept;
- no runtime record or class;
- no API field;
- no workload model code;
- no scoring influence;
- no strategy influence;
- no routing influence;
- no proxy behavior change;
- no signal ingestion;
- no persistence;
- no telemetry;
- no secrets, tokens, environment variables, or credentials;
- no production behavior change.

Future implementation requires a separate approved sprint with API contracts, deterministic fixtures, reviewer documentation, security review, and explicit safety gates.

The Phase 0 architecture ADR index is documented in [`PHASE_0_ARCHITECTURE_ADR_INDEX.md`](PHASE_0_ARCHITECTURE_ADR_INDEX.md). That index names workload realism and scenario modeling as a planning-only ADR topic. The proposed ADR-0004 workload realism and scenario modeling draft is documented in [`adr/ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md`](adr/ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md). It does not implement `WorkloadProfile`, ScenarioGenerator, workload generators, trace import, replay execution, runtime workload models, API fields, scoring influence, routing influence, signal ingestion, telemetry, secrets, or production behavior. The proposed ADR-0005 safety boundaries and guardrails draft is documented in [`adr/ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md`](adr/ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md). It keeps future workload/scenario context behind observe-only, recommendation, shadow, active-experiment, and manual-promotion guardrails without adding runtime enforcement or active traffic shifting. The proposed ADR-0006 evidence packet and replay boundary model is documented in [`adr/ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md`](adr/ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md). It keeps future workload/scenario evidence inside proposed `EvidencePacket`, `EvidenceAssembler`, replay-facing evidence, privacy, filesystem/artifact, and not-proven boundaries without adding EvidencePacket implementation, EvidenceAssembler implementation, replay execution, evidence/report generation, storage/persistence, filesystem-writing behavior, export/upload/download/PDF/ZIP behavior, or production claims. The proposed ADR-0007 reviewer evidence and trust model is documented in [`adr/ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md`](adr/ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md). It keeps future workload/scenario evidence reviewer-readable, non-certifying, and separated from runtime authority without adding reviewer portal/dashboard/API implementation, evidence/report generation, replay execution, storage/persistence, filesystem-writing behavior, export/upload/download/PDF/ZIP behavior, or production claims.

## Why WorkloadProfile Signal Metadata Matters

Adaptive-routing review may eventually need to distinguish workload shapes that look similar at the network layer but behave differently under load. A small metadata vocabulary can help reviewers reason about burstiness, tail latency pressure, think-time patterns, GPU sensitivity, carbon sensitivity, estimated power draw, thermal burst shape, batch deferrability, and latency criticality.

The purpose is not to optimize production traffic today. The purpose is to describe how future lab evidence could explain workload assumptions before those assumptions influence any runtime decision.

The future metadata should answer:

- What workload class was assumed?
- Was the workload bursty, steady, interactive, batch, or unknown?
- Were sensitivity labels observed, estimated, synthetic, unavailable, or unknown?
- Which fields were fixture-derived?
- Which fields were reviewer notes?
- Did the metadata remain read-only?
- Did missing metadata degrade to `UNKNOWN` rather than hidden inference?

## Relationship To LASE / Shadow Evaluation

Future LASE or shadow evaluation could use WorkloadProfile metadata as declared reviewer context for comparing strategies across multiple objectives. That context must begin as metadata shown to reviewers, not hidden scoring behavior.

The future LASE/live allocation boundary is documented in [`LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md). That contract is documentation-only and does not implement runtime boundary enforcement, package refactors, ArchUnit rules, Java ports, API fields, routing behavior, scoring behavior, strategy behavior, proxy behavior, signal ingestion, persistence, telemetry, secrets, environment variables, or production behavior.

The proposed LASE integration ADR is drafted in [`adr/ADR-0002_LASE_INTEGRATION_MODEL.md`](adr/ADR-0002_LASE_INTEGRATION_MODEL.md). That ADR is planning-only and keeps future WorkloadProfile context reviewer-facing/read-only for LASE without adding runtime LASE enforcement, `LaseObservationPort`, WorkloadProfile implementation, replay execution, routing/scoring/strategy/proxy/API behavior changes, or production-readiness claims.

Safe future uses:

- show declared workload assumptions beside a strategy comparison;
- compare strategies under labeled lab assumptions;
- mark missing workload fields as `UNKNOWN`;
- distinguish fixture-derived, synthetic, estimated, and reviewer-supplied metadata;
- keep scoring influence opt-in, separately gated, and test-covered.
- keep WorkloadProfile metadata from changing live routing behavior without explicit future policy gates.

Unsafe current claims:

- LASE currently optimizes GPU sensitivity, carbon sensitivity, thermal burst shape, or power draw;
- current strategy selection change from WorkloadProfile metadata;
- current routing behavior change from WorkloadProfile metadata;
- current production correctness proof from WorkloadProfile metadata;
- current production readiness proof from WorkloadProfile metadata.

Any future LASE influence must require a separate approved sprint, explicit strategy semantics, deterministic tests, and reviewer documentation.

## Relationship To ExternalSignalPort

`WorkloadProfile` is separate from the future `ExternalSignalPort` concept documented in [`EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md`](EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md).

Potential future relationship:

- `WorkloadProfile` could describe the workload shape and sensitivity labels.
- `ExternalSignalSnapshot` could describe external context around that workload.
- Reviewer evidence could show both side by side.
- Missing, stale, or unavailable external signals must not silently mutate WorkloadProfile metadata.
- WorkloadProfile metadata must not become a back door for external control, signal ingestion, or scoring changes.

This sprint does not implement either concept.

## Future WorkloadProfile Vocabulary

Potential future names:

- `WorkloadProfile`
- `BurstProfile`
- `TailLatencyProfile`
- `ThinkTimeProfile`
- `WorkloadSegment`
- `WorkloadClass`
- `WorkloadSignalMetadata`
- `GpuSensitivity`
- `CarbonSensitivity`
- `ThermalBurstPattern`
- `EstimatedPowerDrawProfile`
- `BatchDeferrable`
- `LatencyCriticality`

Suggested workload class vocabulary:

- `INTERACTIVE`
- `BATCH`
- `STREAMING`
- `AI_INFERENCE`
- `AI_TRAINING`
- `CONTROL_PLANE`
- `SYNTHETIC`
- `UNKNOWN`

Suggested sensitivity vocabulary:

- `HIGH`
- `MEDIUM`
- `LOW`
- `NOT_APPLICABLE`
- `UNKNOWN`

The vocabulary is intentionally descriptive. It should not imply current implementation.

## Future Optional Metadata Fields

Potential future fields:

- `workloadClass`
- `burstProfile`
- `tailLatencyProfile`
- `thinkTimeProfile`
- `gpuSensitivity`
- `carbonSensitivity`
- `estimatedPowerDrawProfile`
- `thermalBurstPattern`
- `latencyCriticality`
- `batchDeferrable`
- `deterministicSeed`
- `metadataVersion`
- `evidenceNotes`

Field boundaries:

| Field | Future purpose | Required boundary |
| --- | --- | --- |
| `workloadClass` | Label broad workload family. | Defaults to `UNKNOWN`; does not select a strategy by itself. |
| `burstProfile` | Describe burst shape or pressure band. | Fixture or metadata only; no hidden live inference. |
| `tailLatencyProfile` | Describe tail-latency sensitivity. | Reviewer metadata only unless separately implemented. |
| `thinkTimeProfile` | Describe interactive pacing assumptions. | No session tracking or user profiling in this sprint. |
| `gpuSensitivity` | Mark whether a workload may be accelerator-sensitive. | No GPU orchestration or placement control. |
| `carbonSensitivity` | Mark optional carbon-context sensitivity. | No carbon-aware routing implementation. |
| `estimatedPowerDrawProfile` | Label optional estimated power draw shape. | No live power/grid control. |
| `thermalBurstPattern` | Label optional thermal risk shape. | No facility automation or mutation. |
| `latencyCriticality` | Label latency importance. | Does not alter scoring unless a future sprint explicitly adds that behavior. |
| `batchDeferrable` | Mark whether lab assumptions allow deferred work. | No scheduler control. |
| `deterministicSeed` | Support reproducible fixture examples. | No runtime generation behavior in this sprint. |
| `metadataVersion` | Version future metadata shape. | Not an API contract in this sprint. |
| `evidenceNotes` | Explain reviewer assumptions. | No secrets, credentials, or tenant data. |

## Future Pseudocode Sketch

```java
// Documentation-only pseudocode. Not implemented in this sprint.
record WorkloadProfile(
    String id,
    String name,
    WorkloadClass workloadClass,
    Optional<BurstProfile> burstProfile,
    Optional<TailLatencyProfile> tailLatencyProfile,
    Optional<ThinkTimeProfile> thinkTimeProfile,
    Optional<GpuSensitivity> gpuSensitivity,
    Optional<CarbonSensitivity> carbonSensitivity,
    Optional<EstimatedPowerDrawProfile> estimatedPowerDrawProfile,
    Optional<ThermalBurstPattern> thermalBurstPattern,
    LatencyCriticality latencyCriticality,
    boolean batchDeferrable,
    long deterministicSeed,
    String metadataVersion,
    List<String> evidenceNotes
) {}
```

The sketch intentionally has:

- no routing methods;
- no scoring methods;
- no strategy-selection methods;
- no proxy methods;
- no mutation methods;
- no external signal calls;
- no HTTP client contract;
- no filesystem contract;
- no persistence contract;
- no telemetry contract;
- no credential contract.

## Determinism And Evidence Requirements

Any future implementation must preserve deterministic evidence review.

Minimum requirements:

- Metadata must carry an explicit metadata version.
- Missing data must be represented explicitly as `UNKNOWN`, `UNAVAILABLE`, or equivalent.
- Synthetic fixture data must be labeled as synthetic.
- Estimated data must be labeled as estimated.
- Reviewer-supplied notes must be visibly separated from measured or fixture-derived fields.
- Deterministic examples must use stable fixture values.
- Tests must cover missing, unknown, synthetic, estimated, stale-context, and normal metadata states.
- Any future scoring influence must be separately gated, tested, documented, and opt-in.
- Evidence must not claim production readiness, production certification, live-cloud validation, real-tenant validation, or correctness validation.

The future contract should prefer explicit fixture metadata over hidden inference.

## Safe Degradation Rules

Safe degradation rules for any future implementation:

- Missing workload profile returns `UNKNOWN` status.
- Missing optional fields remain absent or `UNKNOWN`.
- Stale external context does not rewrite workload metadata.
- Untrusted external context does not become workload metadata.
- Synthetic data is labeled as synthetic.
- Estimated data is labeled as estimated.
- Reviewer notes are text evidence only and do not mutate routing behavior.
- Invalid metadata fails closed to reviewer warnings, not silent scoring influence.

No future implementation should infer production readiness from complete metadata.

## Reviewer-Facing Value

This design contract helps reviewers see how future workload modeling could fit without overstating current capability.

Reviewer value:

- clear names for future workload context;
- clear separation from external signals;
- deterministic evidence expectations;
- explicit unknown/unavailable handling;
- visible distinction between workload assumptions and routing decisions;
- future gates that prevent accidental scoring, routing, orchestration, or production claims.

Reviewers should treat this document as architecture planning only.

## Safety Boundaries And Non-Goals

This sprint does not implement `WorkloadProfile`.

Explicit non-goals:

- no Java runtime record or class
- no API behavior change
- no API field
- no workload model code
- no routing behavior change
- no scoring behavior change
- no strategy behavior change
- no proxy behavior change
- no signal ingestion
- no external API clients
- no HTTP calls
- no secrets, tokens, environment variables, or credentials
- no telemetry, storage, or persistence
- no MessageDigest, SHA, hash, UUID, random, time, environment, or system-property behavior
- no replay execution
- no what-if mutation
- no upload/share/download/export/PDF/ZIP behavior
- no Docker, CI, release, signing, registry, or governance changes
- no GPU orchestration claim
- no live power/grid control
- no facility mutation
- no carbon-aware routing implementation
- no production readiness claim
- no production certification claim
- no live-cloud validation claim
- no real-tenant validation claim

The future profile must not become hidden scoring input by accident. It must not mutate routing behavior, select strategies, control schedulers, orchestrate GPU workloads, control facilities, publish telemetry, or make external calls unless a future approved implementation explicitly defines and tests a safer boundary.

## Future Implementation Gates

Before any runtime `WorkloadProfile` implementation, a future sprint must define and pass gates for:

- approved scope and owner intent;
- production-source model design;
- API contract impact review;
- deterministic fixture set;
- metadata versioning plan;
- unknown/unavailable state handling;
- synthetic and estimated label handling;
- reviewer notes redaction and safety review;
- relationship to `ExternalSignalPort`;
- relationship to LASE and shadow evaluation;
- tests for missing, unknown, synthetic, estimated, stale-context, and normal metadata states;
- explicit proof that default routing, scoring, strategy selection, and proxy behavior remain unchanged;
- docs explaining reviewer metadata only;
- explicit no production-readiness, no production-certification, no live-cloud-validation, and no real-tenant-validation boundaries.

No future implementation should be merged only because this design contract exists.
