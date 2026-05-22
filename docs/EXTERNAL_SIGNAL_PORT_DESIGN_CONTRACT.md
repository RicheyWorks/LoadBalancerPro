# External Signal Port Design Contract

This document defines a future architecture contract for a read-only `ExternalSignalPort` concept. It describes how LoadBalancerPro could eventually consume Tier 2 compute/GPU/workload signals and Tier 3 facility/power/grid/carbon signals without compromising Tier 1 L4-L7 adaptive routing experimentation, evidence determinism, reviewer clarity, or safety boundaries.

This is a design contract only. It is not implemented in this sprint, not an API contract, not a Java runtime interface, not a signal ingestion feature, and not proof of production readiness.

## Executive Summary

`ExternalSignalPort` is a future read-only boundary for describing external context that may help reviewers understand routing decisions in richer environments. The current product remains focused on Tier 1 L4-L7 adaptive routing experimentation and evidence. Tier 2 and Tier 3 signal concepts remain future-oriented.

The contract exists so any future implementation starts from a safe shape:

- read-only snapshots;
- explicit source metadata;
- explicit freshness and trust labels;
- deterministic fixture support;
- visible unknown/unavailable states;
- no write/control methods;
- no live facility/grid mutation;
- no GPU orchestration claim;
- no carbon-aware routing implementation claim;
- no production certification claim.

## Why ExternalSignalPort Exists

Future adaptive-routing review may need context beyond request-level network and application delivery signals. Reviewers may ask whether a decision considered workload pressure, GPU cluster pressure, power cost, carbon intensity, thermal pressure, or signal freshness.

`ExternalSignalPort` provides a vocabulary for that future without adding behavior today. Its purpose is to make future signal consumption auditable before any implementation exists.

The port should answer:

- What external context was available?
- Where did it come from?
- How fresh was it?
- How trusted was it?
- Was it observed, estimated, synthetic, unavailable, stale, or unknown?
- Did it remain read-only?
- Did it avoid control-plane or mutation behavior?

## Current Status: Design Contract Only, Not Implemented

Current status:

- design contract only;
- documentation-only pseudocode;
- no Java source files added for this concept;
- no runtime interface;
- no adapters;
- no HTTP clients;
- no signal ingestion;
- no persistence;
- no telemetry;
- no secrets, tokens, environment variables, or credentials;
- no production behavior change.

Future implementation requires a separate approved sprint with API contracts, test fixtures, security review, reviewer documentation, and explicit safety gates.

The Phase 0 architecture ADR index is documented in [`PHASE_0_ARCHITECTURE_ADR_INDEX.md`](PHASE_0_ARCHITECTURE_ADR_INDEX.md). That index names future external signal context boundaries as a planning-only ADR topic. It does not implement `ExternalSignalPort`, adapters, external clients, HTTP calls, signal ingestion, telemetry, secrets, GPU orchestration, power/grid control, carbon-aware routing, facility automation, or production behavior.

## Read-Only Signal Boundary

The future port must be read-only. It may describe external context, but it must not control external systems.

Allowed future shape:

- read-only signal snapshots;
- source metadata;
- freshness status;
- trust level;
- deterministic fixture mode;
- unavailable/unknown states;
- reviewer-facing explanations.

Disallowed shape:

- mutation methods;
- write methods;
- control methods;
- deployment actions;
- external orchestration commands;
- facility control;
- power/grid control;
- GPU orchestration;
- production traffic control;
- secret or credential handling inside reviewer metadata.

The safest default for unavailable, stale, untrusted, or out-of-scope signals is `UNKNOWN`.

## Future Signal Categories

| Category | Future examples | Required boundary |
| --- | --- | --- |
| Workload / compute pressure | queue pressure, cluster pressure, batch deferrability, latency criticality | Read-only metadata only; no scheduler control. |
| GPU cluster pressure | accelerator saturation, GPU-sensitive workload flags, capacity pressure bands | No GPU orchestration claim and no workload placement control. |
| Power cost | estimated power cost band, local price band, cost pressure status | No power market execution and no facility/grid mutation. |
| Carbon intensity | carbon intensity band, region-level estimate, source freshness | Carbon-aware routing is not currently implemented. |
| Thermal/facility pressure | thermal pressure band, cooling pressure, facility risk indicator | No facility automation or control. |
| Signal freshness / confidence | freshness status, confidence band, trust level, source type | Missing or stale data remains `UNKNOWN`. |

## Proposed Future Domain Vocabulary

Potential future names:

- `ExternalSignalPort`
- `ExternalSignalSnapshot`
- `SignalSourceMetadata`
- `SignalFreshnessStatus`
- `SignalTrustLevel`
- `CarbonIntensitySignal`
- `GpuClusterPressureSignal`
- `FacilityThermalPressureSignal`
- `PowerCostSignal`

Suggested status vocabulary:

- `FRESH`
- `STALE`
- `UNAVAILABLE`
- `UNKNOWN`

Suggested trust vocabulary:

- `TRUSTED`
- `ESTIMATED`
- `SYNTHETIC`
- `UNTRUSTED`
- `UNKNOWN`

The vocabulary is intentionally descriptive. It should not imply current implementation.

## Future Pseudocode Sketch

```java
// Documentation-only pseudocode. Not implemented in this sprint.
interface ExternalSignalPort {
    ExternalSignalSnapshot getCurrentSignalSnapshot();

    SignalSourceMetadata describeSignalSource();
}
```

The sketch intentionally has:

- no mutation methods;
- no write methods;
- no control methods;
- no async polling contract;
- no background scheduling contract;
- no HTTP client contract;
- no filesystem contract;
- no persistence contract;
- no telemetry contract;
- no credential contract.

## Determinism And Evidence Requirements

Any future implementation must preserve deterministic evidence review.

Minimum requirements:

- Signal snapshots must carry source metadata.
- Signal snapshots must carry freshness status.
- Signal snapshots must carry trust level.
- Missing data must be represented explicitly as `UNKNOWN`, `UNAVAILABLE`, or equivalent.
- Synthetic fixture data must be labeled as synthetic.
- Estimated data must be labeled as estimated.
- Reviewer output must distinguish observed, estimated, synthetic, stale, unavailable, and unknown values.
- Tests must cover stale, missing, untrusted, synthetic, and normal signal states.
- Future LASE or routing influence must be separately gated, tested, documented, and opt-in.
- Evidence must not claim production readiness, production certification, live-cloud validation, real-tenant validation, or correctness validation.

The future contract should prefer deterministic snapshots over hidden live lookups.

## Safety And Non-Goals

This sprint does not implement `ExternalSignalPort`.

Explicit non-goals:

- no Java runtime interface
- no adapters
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
- no proxy behavior changes
- no strategy behavior changes
- no core routing behavior changes
- no scoring-internals changes
- no GPU orchestration claim
- no live power/grid control
- no facility mutation
- no carbon-aware routing implementation
- no production readiness claim
- no production certification claim
- no live-cloud validation claim
- no real-tenant validation claim

The future port must not become a control plane by accident. It must not mutate infrastructure, operate schedulers, write facility controls, publish telemetry, or make external calls unless a future approved implementation explicitly defines and tests a safer boundary.

## Relationship To LASE / Shadow Evaluation

The future port could eventually provide optional context to LASE or shadow evaluation. That context must begin as reviewer metadata, not hidden scoring behavior.

The future LASE/live allocation boundary is documented in [`LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md). That contract is documentation-only and does not implement runtime boundary enforcement, package refactors, ArchUnit rules, Java ports, API fields, routing behavior, scoring behavior, strategy behavior, proxy behavior, signal ingestion, persistence, telemetry, secrets, environment variables, or production behavior.

The proposed LASE integration ADR is drafted in [`adr/ADR-0002_LASE_INTEGRATION_MODEL.md`](adr/ADR-0002_LASE_INTEGRATION_MODEL.md). That ADR is planning-only and keeps future ExternalSignalPort context read-only for LASE without adding runtime LASE enforcement, `LaseObservationPort`, ExternalSignalPort implementation, signal ingestion, replay execution, routing/scoring/strategy/proxy/API behavior changes, or production-readiness claims.

The proposed safety boundaries ADR is drafted in [`adr/ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md`](adr/ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md). That ADR is planning-only and keeps future external signal context behind policy/operator gates, provenance expectations, privacy constraints, and no-hidden-mutation boundaries without adding ExternalSignalPort implementation, signal ingestion, external clients, HTTP calls, runtime enforcement, active traffic shifting, GPU orchestration, power/grid control, carbon-aware routing, or facility automation.

The proposed evidence packet and replay boundary model ADR is drafted in [`adr/ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md`](adr/ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md). That ADR is planning-only and keeps future external signal/source provenance evidence read-only, privacy-safe, and not-proving without adding EvidencePacket implementation, EvidenceAssembler implementation, replay execution, evidence/report generation, storage/persistence, filesystem-writing behavior, export/upload/download/PDF/ZIP behavior, external signal ingestion, external clients, HTTP calls, or production claims.

Safe future uses:

- show optional external context alongside a routing comparison;
- mark unavailable or stale context explicitly;
- compare strategies under declared multi-objective assumptions in a lab setting;
- help reviewers ask better follow-up questions.
- keep ExternalSignalPort inputs read-only so they never grant LASE direct facility, grid, GPU, cloud, proxy, routing, or production control.

Unsafe current claims:

- LASE currently optimizes power, carbon, thermal, or GPU pressure;
- current carbon-aware routing implementation;
- current GPU orchestration capability;
- current facility or grid control capability;
- external signals prove production correctness.

Any future scoring influence must require a separate approved sprint, explicit strategy semantics, deterministic tests, and reviewer documentation.

## Relationship To WorkloadProfile

`ExternalSignalPort` is separate from a future `WorkloadProfile`.

The future `WorkloadProfile` metadata design contract is documented in [`WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md`](WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md). That contract is documentation-only and does not implement Java records/classes, API fields, workload model code, routing behavior, scoring behavior, strategy behavior, proxy behavior, signal ingestion, persistence, telemetry, secrets, environment variables, or production behavior. The proposed ADR-0004 workload realism and scenario modeling draft is documented in [`adr/ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md`](adr/ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md). ADR-0004 is documentation-only and does not implement workload generators, trace import, replay execution, or production behavior.

Potential future relationship:

- `WorkloadProfile` could describe the request or workload shape.
- `ExternalSignalSnapshot` could describe external context around that workload.
- Reviewer evidence could show both without mutating routing behavior.

Future workload fields such as `workloadClass`, `burstProfile`, `gpuSensitivity`, `carbonSensitivity`, `estimatedPowerDrawProfile`, `thermalBurstPattern`, `latencyCriticality`, and `batchDeferrable` remain documentation concepts unless separately implemented.

## Reviewer-Facing Value

This design contract helps reviewers see where future signal work would fit without overstating current capability.

Reviewer value:

- clear names for future external context;
- clear safety boundaries before implementation;
- deterministic evidence expectations;
- explicit unknown/unavailable handling;
- visible separation between Tier 1 current behavior and Tier 2/Tier 3 future context;
- future gates that prevent accidental production, orchestration, or control claims.

Reviewers should treat this document as architecture planning only.

## Future Implementation Gates

Before any runtime `ExternalSignalPort` implementation, a future sprint must define and pass gates for:

- approved scope and owner intent;
- production-source interface design;
- API contract impact review;
- deterministic fixture set;
- no-live-call default behavior;
- source metadata model;
- freshness and trust model;
- unavailable/unknown state handling;
- security review for secrets and network boundaries;
- tests for stale, missing, untrusted, synthetic, and normal signals;
- docs explaining reviewer metadata only;
- explicit no-control-plane proof;
- explicit no production-readiness, no production-certification, no live-cloud-validation, and no real-tenant-validation boundaries.

No future implementation should be merged only because this design contract exists.
