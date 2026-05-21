# Three-Tier Adaptive Routing Strategy Positioning

This document positions LoadBalancerPro as an Enterprise-Grade Adaptive Routing Experimentation & Evidence Platform rooted in Tier 1 L4-L7 application delivery and network load-balancing review. It also records future-oriented architecture hooks for Tier 2 compute/GPU/workload signals and Tier 3 facility, thermal, power, grid, and carbon signals.

This is a strategic architecture reference only. It is documentation for reviewer context, not a runtime implementation, API contract, production claim, or deployment certification.

## Executive Summary

LoadBalancerPro is currently best positioned as a specialized Tier 1 tool for advanced L4-L7 adaptive routing experimentation, LASE/shadow evaluation, explainability, replay/evidence culture, reviewer-safe metadata, and strong safety guardrails.

The strategic opportunity is to make the platform architecture-ready for read-only signal consumption across adjacent tiers:

- Tier 2 influence: workload-aware, compute-aware, and GPU-aware routing metadata as a future-oriented design target.
- Tier 3 influence: power-aware, carbon-aware, thermal-aware, facility-aware, and grid-context metadata as planned extensions.
- Long-term differentiation: modeling the interaction between network routing decisions, compute pressure, and facility/grid context without compromising routing safety or reviewer clarity.

Tier 2 and Tier 3 concepts are not currently implemented. LoadBalancerPro does not currently perform production GPU orchestration, carbon-aware routing, facility control, power-grid integration, production certification, live-cloud validation, or real-tenant validation.

## Three-Tier Model

| Tier | Domain | Current status | Intended role |
| --- | --- | --- | --- |
| Tier 1 | Application delivery / L4-L7 routing | Current primary product focus | Compare routing strategies, explain selected backends, support LASE/shadow evaluation, preserve reviewer evidence, and keep strong safety boundaries. |
| Tier 2 | Compute, GPU, and workload orchestration signals | Future-oriented, not currently implemented | Optional read-only context that could help reviewers understand workload pressure, queue pressure, GPU sensitivity, and compute saturation. |
| Tier 3 | Facility, thermal, power, grid, and carbon signals | Future-oriented, not currently implemented | Optional read-only context that could help reviewers reason about estimated power impact, carbon context, thermal pressure, and facility constraints. |

Tier 1 remains the live center of gravity. Tier 2 and Tier 3 are design targets for future read-only signal metadata, not claims of current orchestration or control.

## Where LoadBalancerPro Fits Today

LoadBalancerPro fits today as a controlled Enterprise Adaptive Routing Lab:

- It evaluates L4-L7 routing choices using visible request and response data.
- It supports LASE and shadow/recommendation thinking inside bounded lab workflows.
- It exposes explainability and reviewer metadata for routing decisions.
- It builds a replay/evidence culture without claiming replay proof or production validation.
- It keeps reviewer-safe metadata additive and read-only.
- It guards against production overclaims by documenting what remains unknown or not proven.

The current product is strongest when reviewers ask how routing decisions are compared, explained, reproduced locally, and bounded.

## Why Tier 1 Remains The Primary Product Focus

Tier 1 remains the primary product focus because it is the product surface already represented by the codebase, tests, docs, static reviewer pages, API contracts, and local smoke workflows.

Keeping Tier 1 primary protects the project from drifting into claims that require very different proof:

- GPU orchestration would require scheduler integration, real workload ownership, capacity controls, and tenant boundaries.
- Facility or grid integration would require power, thermal, safety, compliance, and operational control evidence.
- Carbon-aware routing would require live or trusted carbon context, policy semantics, and validation of tradeoffs.
- Production certification would require environment-specific controls, operational evidence, incident processes, and independent acceptance.

Those are valid future design spaces, but they are not current implementation claims.

## Natural Tier 2 And Tier 3 Extension Opportunities

Natural future opportunities are read-only first:

- Attach workload metadata to routing evaluations so reviewers can distinguish latency-critical, bursty, batch, or GPU-sensitive traffic.
- Attach compute pressure metadata so reviewers can see queue pressure, cluster pressure, or accelerator sensitivity next to routing evidence.
- Attach facility and carbon context as optional reviewer metadata, such as estimated power impact, thermal risk, or carbon intensity bands.
- Keep all future external signals behind trust, freshness, source, and determinism labels.
- Preserve routing behavior unless a separately approved sprint defines and tests explicit strategy semantics.

The preferred posture is read-only signal consumption concept before any runtime influence. No live facility/grid mutation, no write/control methods, and no external control plane should be introduced by architecture positioning alone.

## Proposed Future Phase 13: Multi-Tier Signal Integration Layer

Goal: allow LoadBalancerPro to consume read-only external context signals without compromising core routing safety, evidence determinism, reviewer clarity, or experimentation boundaries.

Potential future components:

- `ExternalSignalPort`
- `ExternalSignalSnapshot`
- `SignalSourceMetadata`
- `SignalFreshnessStatus`
- `SignalTrustLevel`
- `CarbonIntensitySignal`
- `GpuClusterPressureSignal`
- `FacilityThermalPressureSignal`
- `PowerCostSignal`

Non-runtime pseudocode sketch:

```java
interface ExternalSignalPort {
    ExternalSignalSnapshot getCurrentSignalSnapshot();

    SignalSourceMetadata describeSignalSource();
}
```

This sketch is documentation-only pseudocode. It intentionally has no mutation methods, no write methods, no control methods, no credentials, no HTTP calls, no filesystem storage, and no telemetry side effects.

Any future implementation would need separate API contracts, security review, trust/freshness semantics, deterministic test fixtures, and explicit reviewer boundaries before it could influence routing results.

## Expanded Phase 4: LASE Multi-Objective Evaluation

Future LASE work could compare strategies across multiple objectives:

- latency
- throughput
- error rate
- queue pressure
- estimated power impact
- estimated carbon impact
- thermal risk
- reviewer evidence completeness

Power, carbon, and thermal values are future optional metadata only. They are not current production optimization claims, not live grid or facility controls, not carbon-aware routing implementation, and not proof that a selected route is globally optimal.

The safe design target is to show reviewers how a strategy would be evaluated under declared objectives while keeping every objective source visible, bounded, and marked as observed, estimated, synthetic, unavailable, or unknown.

## Expanded Phase 5: AI-Era Workload Modeling

Future workload profile metadata could include optional fields such as:

- `workloadClass`
- `burstProfile`
- `gpuSensitivity`
- `carbonSensitivity`
- `estimatedPowerDrawProfile`
- `thermalBurstPattern`
- `latencyCriticality`
- `batchDeferrable`

These fields are documentation-only concepts in this sprint. They do not change runtime behavior, scoring internals, strategy selection, proxy behavior, replay behavior, or production behavior.

## ExternalSignalPort Design Concept

`ExternalSignalPort` should remain a read-only boundary if implemented later. Its design intent is to describe context, not control infrastructure.

The detailed future design contract is documented in [`EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md`](EXTERNAL_SIGNAL_PORT_DESIGN_CONTRACT.md). That contract is documentation-only and does not implement Java interfaces, adapters, clients, signal ingestion, persistence, telemetry, secrets, environment variables, or production behavior.

Design constraints:

- read-only signal snapshots only;
- explicit source metadata;
- explicit freshness status;
- explicit trust level;
- deterministic fixture support;
- clear unavailable/unknown states;
- no secrets or credentials in reviewer output;
- no live facility/grid mutation;
- no power control;
- no GPU orchestration;
- no process execution;
- no upload/share/download/export/PDF/ZIP behavior.

Future adapters should be opt-in, separately reviewed, and able to return `UNKNOWN` when data is unavailable, stale, untrusted, or out of scope.

## Safety Boundaries And Non-Goals

This sprint is documentation and documentation guard tests only. It does not add production Java runtime behavior.

Explicit non-goals:

- no live power/grid control
- no facility mutation
- no GPU orchestration claim
- no production certification claim
- no live-cloud validation claim
- no real-tenant validation claim
- no carbon-aware routing implementation
- no production readiness claim
- no runtime signal ingestion
- no external API clients
- no HTTP calls
- no secrets, tokens, environment variables, or credentials
- no telemetry, storage, or persistence
- no `MessageDigest`, SHA, hash, UUID, random, time, environment, or system-property behavior
- no replay execution
- no what-if mutation
- no upload/share/download/export/PDF/ZIP behavior
- no Docker, CI, release, signing, registry, governance, proxy behavior, strategy behavior, core routing behavior, or scoring-internals change

Carbon-aware routing is not currently implemented. Production readiness is not proven. Live-cloud validation is not provided. Real-tenant validation is not provided. Facility automation is not implemented.

## Reviewer-Facing Value

The three-tier framing helps reviewers understand why the current Tier 1 product matters while preventing overclaiming:

- It explains the current L4-L7 adaptive-routing evidence focus.
- It separates implemented evidence lanes from future architecture hooks.
- It names how compute and facility context could become reviewer metadata later.
- It keeps safety boundaries visible before broader signal integration is proposed.
- It gives future sprints a vocabulary for read-only external context without implying live orchestration or control.

Reviewers should treat this document as strategic positioning, not as proof of Tier 2 or Tier 3 implementation.

## Future Roadmap Table

| Phase | Concept | Scope | Boundary |
| --- | --- | --- | --- |
| Phase 4 expansion | LASE multi-objective evaluation | Future strategy comparison across latency, throughput, error rate, queue pressure, estimated power impact, estimated carbon impact, thermal risk, and reviewer evidence completeness | Future optional metadata only; no production optimization claim. |
| Phase 5 expansion | AI-era workload modeling | Future optional workload profile fields such as workload class, GPU sensitivity, carbon sensitivity, and batch deferrability | Documentation-only in this sprint; no runtime behavior change. |
| Phase 13 | Multi-Tier Signal Integration Layer | Future read-only external signal snapshots with source metadata, freshness, and trust labels | No live facility/grid mutation, no GPU orchestration, no write/control methods. |
| Later gated implementation | External signal adapters | Potential adapters only after separate approval, security review, deterministic fixtures, and API contracts | No external API clients or HTTP calls are added by this document. |
| Later reviewer evidence | Multi-tier evidence panels | Potential reviewer metadata showing available, stale, unavailable, unknown, or synthetic signals | Not proof of production readiness, production certification, live-cloud validation, real-tenant validation, carbon-aware routing implementation, or facility automation. |
