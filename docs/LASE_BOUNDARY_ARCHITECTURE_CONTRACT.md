# LASE Boundary Architecture Contract

This document defines a future architecture contract for the boundary between the live allocation path and the LASE shadow/experimentation path. It explains how LASE may observe, compare, explain, and generate reviewer evidence without becoming hidden production routing authority.

This is a boundary contract only. It is not implemented in this sprint, not an API contract, not a Java runtime interface, not a package refactor, not an ArchUnit rule set, not a production routing feature, and not proof of production readiness.

## Executive Summary

LoadBalancerPro already has substantial LASE, evidence, replay, scenario, and reviewer metadata infrastructure. The long-term architecture needs a clear safety rule:

LASE may observe, compare, explain, and produce evidence, but it must not directly mutate live allocation state, directly select production routes, directly alter proxy behavior, directly call external control systems, directly call `CloudManager` or any future cloud/facility/grid/GPU control path, or become hidden production routing authority.

The contract exists so future implementation work starts from a safe shape:

- live allocation remains the explicit live decision path;
- LASE remains shadow/evaluation/evidence unless separately promoted through future policy gates;
- replay/evidence remains reviewer controlled;
- reviewer metadata remains read-only;
- external signal concepts remain read-only inputs;
- WorkloadProfile concepts remain optional metadata;
- missing or stale inputs degrade safely;
- no production readiness claim;
- no production certification claim.

## Current Status: Boundary Contract Only, Not Implementation

Current status:

- design contract only;
- documentation and documentation guard tests only;
- documentation-only pseudocode;
- no Java source files added for this concept;
- no runtime boundary enforcement;
- no package moves or refactors;
- no ArchUnit dependency or rule set;
- no runtime interface;
- no ports or adapters;
- no API fields;
- no routing behavior change;
- no scoring behavior change;
- no strategy behavior change;
- no proxy behavior change;
- no telemetry;
- no persistence;
- no external API clients;
- no HTTP calls;
- no secrets, tokens, environment variables, credentials, config, or properties;
- no production behavior change.

This document does not claim the LASE boundary is already enforced by package refactor or ArchUnit. Future enforcement requires a separate approved sprint.

The current-tree inventory for this future boundary is documented in [`LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md`](LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md). That inventory is docs/test-only preparation. It maps current classes into future boundary buckets without moving classes, adding runtime interfaces, adding ArchUnit rules, enforcing packages, or changing behavior.

The staged package-boundary enforcement plan is documented in [`LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md`](LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md). That plan is docs/test-only preparation for future enforcement. It does not add ArchUnit, package-boundary tooling, package moves, runtime interfaces, or behavior changes.

The LASE boundary naming guard plan is documented in [`LASE_BOUNDARY_NAMING_GUARD_PLAN.md`](LASE_BOUNDARY_NAMING_GUARD_PLAN.md). That plan is docs/test-only naming preparation for future boundary vocabulary. It does not add runtime naming enforcement, ArchUnit, package-boundary tooling, package moves, runtime interfaces, or behavior changes.

The Phase 0 architecture ADR index is documented in [`PHASE_0_ARCHITECTURE_ADR_INDEX.md`](PHASE_0_ARCHITECTURE_ADR_INDEX.md). That index names the future LASE integration ADR and layered architecture ADR as planning-only decision topics. It does not implement runtime LASE enforcement, package moves, package-boundary enforcement, ArchUnit tooling, or behavior changes.

ADR-0001 is drafted in [`adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md`](adr/ADR-0001_LAYERED_ARCHITECTURE_BOUNDARY.md). That ADR is proposed/planning-only layered architecture guidance. It maps future domain, allocation, LASE, evidence, infrastructure, API, config, and docs/tests boundaries without moving packages, adding ArchUnit, enforcing package boundaries, changing routing/scoring/strategy/proxy behavior, or claiming production readiness.

ADR-0002 is drafted in [`adr/ADR-0002_LASE_INTEGRATION_MODEL.md`](adr/ADR-0002_LASE_INTEGRATION_MODEL.md). That ADR is proposed/planning-only LASE integration guidance. It maps future LASE relationships to live allocation, shadow evaluation, replay/comparison, evidence/reviewer metadata, policy gates, read-only observation ports, and future external signals without adding runtime LASE enforcement, `LaseObservationPort`, replay execution, package-boundary enforcement, routing/scoring/strategy/proxy/API behavior changes, or production-readiness claims.

ADR-0005 is drafted in [`adr/ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md`](adr/ADR-0005_SAFETY_BOUNDARIES_AND_GUARDRAILS.md). That ADR is proposed/planning-only safety boundary guidance. It maps future observe-only, recommendation, shadow, active-experiment, and manual-promotion guardrails without adding runtime enforcement, active traffic shifting, replay execution, evidence/report generation, storage/persistence, workload generation, trace import, external signal ingestion, or behavior changes.

ADR-0006 is drafted in [`adr/ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md`](adr/ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md). That ADR is proposed/planning-only evidence packet and replay boundary guidance. It maps future `EvidencePacket`, `EvidenceAssembler`, replay-facing evidence, deterministic assembly, privacy, filesystem/artifact, and not-proven boundaries without adding EvidencePacket implementation, EvidenceAssembler implementation, replay execution, evidence/report generation, storage/persistence, filesystem-writing behavior, export/upload/download/PDF/ZIP behavior, or behavior changes.

ADR-0007 is drafted in [`adr/ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md`](adr/ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md). That ADR is proposed/planning-only reviewer evidence and trust guidance. It maps future reviewer evidence purpose, trust inputs, explanation expectations, policy gate evidence, signal provenance, rejected-option evidence, safety-mode evidence, replay/scenario evidence, manual/operator approval, and not-proven boundaries without adding reviewer portal/dashboard/API implementation, evidence/report generation, replay execution, storage/persistence, filesystem-writing behavior, export/upload/download/PDF/ZIP behavior, runtime enforcement, or behavior changes.

ADR-0008 is drafted in [`adr/ADR-0008_RUNTIME_ENFORCEMENT_AND_PACKAGE_BOUNDARY_PLAN.md`](adr/ADR-0008_RUNTIME_ENFORCEMENT_AND_PACKAGE_BOUNDARY_PLAN.md). That ADR is proposed/planning-only runtime enforcement and package-boundary guidance. It maps future dependency direction, package-boundary tests, ArchUnit-style options, source-name guard checks, explicit ports, adapter boundaries, mutation-authority checks, safety-mode tests, and forbidden dependency checks without adding runtime enforcement, package-boundary enforcement, ArchUnit dependency/enforcement, package moves, source-name guard implementation, routing/scoring/strategy/proxy/API behavior changes, or production-readiness claims.

ADR-0009 is drafted in [`adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md`](adr/ADR-0009_LOCAL_LAB_KIT_AND_SIMULATED_DATACENTER_TEST_HARNESS_PLAN.md). That ADR is proposed/planning-only Local Lab Kit and simulated datacenter guidance. It maps future local lab scenarios, fake backend roles, degradation tooling, evidence expectations, and hardware expansion boundaries without adding Docker Compose files, scripts, fake backend nodes, k6 scenarios, Bruno collections, Toxiproxy configuration, Prometheus/Grafana dashboards, routing/scoring/strategy/proxy/API behavior changes, replay/storage/export behavior, or production-readiness claims.

## Why The LASE/Live Allocation Boundary Matters

LASE is useful because it can compare strategies, explain tradeoffs, evaluate scenarios, and generate evidence while the live allocation path remains understandable and bounded. That value weakens if shadow evaluation can silently mutate live state or become an unreviewed routing authority.

The boundary matters because reviewers need to answer:

- Which code path selected the live backend?
- Which path only observed or compared the decision?
- Which evidence came from deterministic lab or reviewer contexts?
- Which outputs are explanations rather than production control actions?
- Which policy gate, if any, allowed a shadow recommendation to influence a lab decision?
- Did missing, stale, or unknown metadata degrade safely?

The safest default is separation: live allocation chooses through explicit allocation behavior, while LASE observes and explains unless a future policy gate explicitly authorizes a bounded mode.

## Current Conceptual Areas

### Live Allocation Path

The live allocation path is the explicit decision path that selects backends or servers for allocation. Its responsibilities are to apply configured routing behavior, health information, strategy semantics, and guardrails that are already part of the implementation.

Boundary:

- live allocation may publish read-only observations in a future design;
- live allocation must not depend on hidden LASE authority;
- live allocation must not allow LASE to mutate state through private back channels;
- any future LASE influence must be visible, gated, audited, and separately tested.

### LASE Shadow Evaluation Path

The LASE shadow evaluation path may compare, recommend, score alternatives in controlled contexts, and explain outcomes. It is an experimentation and evidence path unless explicitly promoted through future policy gates.

Boundary:

- LASE may consume observations;
- LASE may run shadow comparisons;
- LASE may produce recommendations or findings for reviewers;
- LASE must not directly select production routes;
- LASE must not directly mutate allocation or proxy state.

### Replay/Evidence Path

The replay/evidence path supports deterministic reviewer understanding. It may summarize facts, limitations, comparison outputs, evidence packets, and not-proven boundaries.

Boundary:

- replay/evidence may report reviewer metadata;
- replay/evidence must not execute hidden production replay;
- replay/evidence must not become what-if mutation;
- replay/evidence must not claim replay proof, correctness validation, production readiness, or production certification unless a future approved implementation proves and documents those claims.

### Reviewer Metadata Path

The reviewer metadata path is read-only explanatory output. It helps humans inspect what happened, what was compared, and what remains unknown.

Boundary:

- reviewer metadata may summarize status, limitations, and evidence completeness;
- reviewer metadata must not change route selection;
- reviewer metadata must not persist hidden routing authority;
- reviewer metadata must preserve not-proven boundaries.

## Future Layered Architecture Concept

A future implementation could separate concerns into explicit layers:

| Layer | Future purpose | Required boundary |
| --- | --- | --- |
| `domain` | Pure models, facts, observations, and events. | No infrastructure calls or mutation side effects. |
| `allocation` | Live decision path and configured allocation behavior. | Does not grant LASE direct mutation access. |
| `lase` | Shadow evaluation, policy comparison, replay, explanation, and evidence. | Shadow/evidence by default; no production routing authority. |
| `infrastructure` | Guarded external/cloud/future integrations. | No direct LASE control over cloud, facility, grid, GPU, or proxy mutation paths. |
| `api` | Response and view layer. | Exposes reviewer-readable state without changing semantics. |
| `config` | Configuration. | No hidden activation of LASE production control. |

This is a proposed future package boundary model, not a package move in this sprint.

## Proposed Future Package Boundary Model

Future package design could use naming that makes the boundary visible:

- `domain`: immutable or side-effect-free models and events;
- `allocation`: live allocation decision flow;
- `lase`: shadow evaluation, policy comparison, replay, evidence, and reviewer explanation;
- `infrastructure`: guarded adapters for cloud or future external integrations;
- `api`: HTTP response/view shaping;
- `config`: explicit configuration.

Required future rules:

- `allocation` may publish observation snapshots.
- `lase` may consume observations through a read-only port.
- `lase` must not call allocation mutation methods.
- `lase` must not call proxy mutation paths.
- `lase` must not call `CloudManager` or any future facility/grid/GPU control path.
- `lase` output must be evidence/reviewer metadata unless explicitly promoted through future policy gates.

This document does not claim these packages already exist or are already enforced.

## Proposed Future LaseObservationPort Concept

The future `LaseObservationPort` concept would make observation flow explicit. Allocation could publish snapshots; LASE could consume those snapshots; LASE would not receive mutation handles.

```java
// Documentation-only pseudocode. Not implemented in this sprint.
interface LaseObservationPort {
    LaseObservationSnapshot currentObservationSnapshot();

    LaseObservationSource describeObservationSource();
}

// Documentation-only pseudocode. Not implemented in this sprint.
interface LaseShadowEvaluationBoundary {
    LaseEvaluationResult evaluateShadowOnly(LaseObservationSnapshot snapshot);
}
```

The sketch intentionally has:

- no allocation mutation methods;
- no routing selection methods;
- no proxy mutation methods;
- no `CloudManager` calls;
- no cloud/facility/grid/GPU control methods;
- no external API client contract;
- no HTTP client contract;
- no persistence contract;
- no telemetry contract;
- no credential contract;
- no production activation method.

## Allowed LASE Responsibilities

Allowed LASE responsibilities:

- observe snapshots;
- run shadow comparisons;
- compare strategies offline or in shadow mode;
- generate reviewer evidence;
- generate explanation metadata;
- support replay evidence in controlled reviewer contexts;
- report limitations and not-proven boundaries;
- degrade safely when required inputs are missing or stale.

Allowed LASE output should remain explicit about whether it is observed, estimated, synthetic, unavailable, unknown, shadow-only, or gated lab evidence.

## Forbidden LASE Responsibilities

Forbidden LASE responsibilities:

- directly mutate live routing state;
- directly select production routes;
- directly alter proxy behavior;
- directly call external control systems;
- directly call `CloudManager` or future cloud/facility/grid/GPU control paths;
- write production configuration;
- persist hidden routing authority;
- bypass policy gates;
- claim replay proof, scoring proof, correctness validation, production readiness, or production certification.

If a future approved sprint promotes any LASE output into a controlled decision path, that sprint must make the policy gate explicit, deterministic, auditable, and separately documented.

## Determinism And Evidence Requirements

Any future implementation must preserve deterministic evidence review.

Minimum requirements:

- Observation snapshots must be immutable or treated as immutable by LASE.
- Observation snapshots must identify their source.
- Missing data must be represented explicitly as `UNKNOWN`, `UNAVAILABLE`, or equivalent.
- Stale data must remain visible to reviewers.
- Synthetic fixture data must be labeled as synthetic.
- Estimated data must be labeled as estimated.
- LASE findings must distinguish shadow comparison, reviewer metadata, and gated lab influence.
- Tests must cover missing, stale, synthetic, estimated, unknown, and normal observation states.
- Any future live influence must be separately gated, tested, documented, and opt-in.
- Evidence must not claim replay proof, scoring proof, correctness validation, production readiness, production certification, live-cloud validation, or real-tenant validation.

The future contract should prefer explicit snapshots over hidden live lookups.

## Safety Boundaries And Non-Goals

This sprint does not implement runtime LASE boundary enforcement.

Explicit non-goals:

- no production Java runtime behavior
- no Java runtime interface
- no ports or adapters
- no package moves or refactors
- no ArchUnit dependency or build changes
- no runtime LASE boundary implementation
- no API fields
- no routing behavior change
- no scoring behavior change
- no strategy behavior change
- no proxy behavior change
- no runtime workload model implementation
- no runtime signal ingestion
- no external API clients
- no HTTP calls
- no secrets, tokens, environment variables, credentials, config, or properties
- no telemetry, storage, or persistence
- no MessageDigest, SHA, hash, UUID, random, time, environment, or system-property behavior
- no replay execution
- no what-if mutation
- no upload/share/download/export/PDF/ZIP behavior
- no Docker, CI, release, signing, registry, or governance changes
- no live-cloud validation claim
- no real-tenant validation claim
- no GPU orchestration claim
- no live power/grid control
- no facility mutation
- no carbon-aware routing implementation
- no production readiness claim
- no production certification claim

The future boundary must not become a control plane by accident. It must not mutate routing, proxy, cloud, facility, grid, GPU, scheduler, telemetry, storage, or production configuration unless a future approved implementation explicitly defines and tests a safer boundary.

## Relationship To ExternalSignalPort

The future `ExternalSignalPort` design contract is documented in [`EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md`](EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md). ExternalSignalPort remains a documentation-only read-only signal concept.

If implemented later, future ExternalSignalPort signals must be read-only inputs. They must not grant LASE direct control over facility, grid, GPU, cloud, proxy, routing, or production systems.

Safe future relationship:

- ExternalSignalPort may provide optional context to reviewer evidence.
- LASE may compare strategies under declared signal assumptions in shadow mode.
- Missing, stale, unavailable, or untrusted external signals must degrade to explicit `UNKNOWN` or warnings.
- External signals must not become hidden production routing authority.

This sprint does not implement ExternalSignalPort, external signal ingestion, or runtime LASE integration.

## Relationship To WorkloadProfile Signal Metadata

The future WorkloadProfile signal metadata design contract is documented in [`WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md`](WORKLOAD_PROFILE_SIGNAL_METADATA_DESIGN_CONTRACT.md). The proposed ADR-0004 workload realism and scenario modeling draft is documented in [`adr/ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md`](adr/ADR-0004_WORKLOAD_REALISM_AND_SCENARIO_MODELING.md). WorkloadProfile and ScenarioGenerator remain documentation-only metadata and scenario concepts.

Future WorkloadProfile metadata may help LASE generate deterministic shadow comparisons and evidence. It must not by itself change live routing behavior without explicit future policy gates.

Safe future relationship:

- WorkloadProfile may describe workload assumptions for reviewers.
- LASE may use metadata in deterministic shadow comparisons.
- Missing or stale metadata must degrade safely.
- WorkloadProfile metadata must not become hidden scoring, routing, strategy, proxy, or production behavior.

This sprint does not implement WorkloadProfile records/classes, ScenarioGenerator classes, workload generators, trace import, replay execution, API fields, workload model code, or runtime LASE integration.

## Future Implementation Gates

Before any runtime boundary enforcement, a future sprint must define and pass gates for:

- review of the current-tree inventory in [`LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md`](LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md);
- review of the staged enforcement plan in [`LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md`](LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md);
- approved scope and owner intent;
- explicit package boundary design;
- dependency direction review;
- API contract impact review;
- deterministic observation fixtures;
- unknown/unavailable state handling;
- stale-context handling;
- evidence semantics for shadow-only output;
- policy-gate semantics for any future active influence;
- security review for CloudManager, proxy, external signal, facility, grid, GPU, and cloud boundaries;
- tests proving default allocation, routing, scoring, strategy selection, and proxy behavior remain unchanged;
- docs explaining reviewer metadata only;
- explicit proof that LASE cannot directly mutate allocation or proxy state;
- explicit proof that LASE cannot directly call CloudManager or future control paths;
- explicit no production-readiness, no production-certification, no live-cloud-validation, and no real-tenant-validation boundaries.

No future implementation should be merged only because this design contract exists.

## Reviewer-Facing Value

This design contract helps reviewers understand how LoadBalancerPro can keep LASE useful without letting experimentation blur into hidden production control.

Reviewer value:

- clear separation between live allocation and shadow evaluation;
- explicit allowed and forbidden LASE responsibilities;
- future package boundary vocabulary;
- future read-only observation port concept;
- deterministic evidence expectations;
- clear relationship to ExternalSignalPort and WorkloadProfile;
- explicit unknown/stale degradation rules;
- preserved not-proven boundaries.

Reviewers should treat this document as architecture planning only, not implementation proof.
