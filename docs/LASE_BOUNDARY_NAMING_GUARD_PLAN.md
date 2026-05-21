# LASE Boundary Naming Guard Plan

This document prepares future lightweight naming guards for the LASE boundary architecture. It explains how future documentation and test checks could keep LASE boundary, package, class, and responsibility names honest before any package move, ArchUnit tooling, runtime interface, or enforcement implementation exists.

This is a naming plan only, not enforcement. It is docs/test only. No ArchUnit or package-boundary tool is added in this sprint. No classes are moved in this sprint. No packages are refactored. No Maven build files are changed. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

## Executive Summary

PR #220 added [`LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md), which defines the desired future boundary between live allocation and LASE shadow/evidence paths. PR #221 added [`LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md`](LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md), which maps the current tree into future boundary buckets. PR #222 added [`LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md`](LASE_PACKAGE_BOUNDARY_ENFORCEMENT_PLAN.md), which stages future package-boundary enforcement.

This naming guard plan is the next preparation layer. It defines naming categories, candidate naming rules, allowed examples, risky examples to avoid, and future documentation/test-only guard ideas. The plan is intentionally weaker than package-boundary enforcement and does not claim runtime protection.

The central naming principle is:

Names should make ownership and limits visible. LASE, shadow, replay, evidence, reviewer metadata, live allocation, infrastructure, API, and configuration names should not imply production authority, certification, live-cloud validation, GPU orchestration, power/grid control, carbon-aware routing implementation, or facility automation unless a future separately approved implementation proves and documents that scope.

## Current Status: Naming Plan Only, Not Enforcement

Current status:

- naming plan only;
- documentation and documentation guard tests only;
- no runtime enforcement;
- no package-boundary enforcement;
- LASE naming guard is not runtime-enforced;
- LASE package boundary is not currently enforced;
- no ArchUnit or package-boundary tool is added in this sprint;
- no new dependency is added in this sprint;
- no Maven build files are changed in this sprint;
- no classes are moved in this sprint;
- no packages are renamed or refactored in this sprint;
- no production Java runtime behavior is added;
- no records/classes/interfaces are added under `src/main/java`;
- no runtime interface or behavior is added;
- no runtime LASE boundary implementation is added;
- no runtime package-boundary enforcement is added;
- no runtime naming enforcement beyond documentation guard tests is added;
- no API fields are added;
- no routing, scoring, strategy, proxy, config, CI, Docker, release, signing, registry, governance, or production behavior changes are made.

This document is not runtime enforcement proof, package-boundary enforcement proof, ArchUnit tooling proof, production-readiness proof, production-certification proof, live-cloud validation proof, real-tenant validation proof, GPU orchestration proof, power/grid control proof, carbon-aware routing implementation proof, or facility automation proof.

## Relationship To LASE Boundary Architecture Contract

The LASE boundary architecture contract defines the intended future separation:

- live allocation remains the explicit live decision path;
- LASE remains shadow/evaluation/evidence by default;
- replay/evidence remains reviewer controlled;
- reviewer metadata remains read-only;
- future ExternalSignalPort concepts remain read-only context;
- future WorkloadProfile metadata remains optional and deterministic;
- missing or stale inputs degrade safely;
- future active influence requires explicit policy gates and separate approval.

Naming guards can support that contract by making names less ambiguous. They do not enforce the contract. They do not prove dependency direction, runtime call boundaries, package ownership, or mutation safety.

## Relationship To LASE Boundary Enforcement Inventory

The current-tree inventory maps today's classes into future boundary buckets: live allocation, LASE shadow/evaluation, replay/evidence, reviewer metadata, domain model candidates, infrastructure/cloud/future integration, API/view, and configuration.

This naming plan uses those buckets as future naming categories. It does not change the inventory. It does not move classes. It does not relabel current classes as defects. It gives future maintainers a low-risk vocabulary before package moves or enforcement tooling are introduced.

## Relationship To LASE Package Boundary Enforcement Plan

The package-boundary enforcement plan stages future enforcement from documentation and naming preparation toward possible future package moves and ArchUnit-style dependency checks.

This naming plan fits the E1-style preparation lane from that plan: naming and docs-only boundary guard tests. It is intentionally earlier and weaker than package-boundary enforcement. Naming guard tests do not replace package-boundary enforcement and must not be used to claim package-boundary enforcement is active.

The current naming inventory is documented in [`LASE_NAMING_GUARD_INVENTORY.md`](LASE_NAMING_GUARD_INVENTORY.md). That inventory maps current class/file naming against this plan before any source-name guard test, runtime naming enforcement, package move, ArchUnit rule, or behavior change is introduced.

## Why Naming Guards Come Before Package Moves

Naming guards come before package moves because names are cheap to review and low-risk to discuss:

- unclear names can make shadow/evidence code look like live production authority;
- live allocation names should not imply they are only shadow evaluators;
- evidence and replay names should not imply proof beyond the evidence they actually provide;
- reviewer metadata names should make view/reporting scope visible;
- infrastructure names should keep cloud, proxy, facility, grid, GPU, and future control paths explicit;
- risky names can be documented before any runtime refactor touches imports, wiring, routing, scoring, proxy behavior, or API semantics.

Naming guards are an early warning, not a safety boundary. They can help reviewers spot misleading responsibility names, but they cannot prove call graphs, runtime behavior, package boundaries, or mutation safety.

## Future Naming Categories

Future naming categories:

| Category | Naming purpose | Safety note |
| --- | --- | --- |
| Live allocation naming | Names for live routing, allocation, selection, server state, policy, and route decision code. | Should avoid names that imply shadow-only behavior unless the class is explicitly observation-only. |
| LASE shadow/evaluation naming | Names for LASE shadow comparisons, evaluation reports, recommendations, and controlled experiment evidence. | Should signal shadow/evaluation/evidence by default, not production route authority. |
| Replay/evidence naming | Names for replay summaries, scenario evidence, reconstruction traces, reviewer packets, and evidence completeness metadata. | Should not imply replay proof, correctness proof, production readiness, or production certification. |
| Reviewer metadata naming | Names for read-only response, report, checklist, rollup, handoff, closure, and guidance surfaces. | Should clearly signal metadata, view, reporting, or reviewer responsibility. |
| Domain model naming | Names for pure models, facts, observations, snapshots, signals, decisions, and immutable vocabulary. | Should not imply infrastructure calls or live mutation. |
| Infrastructure/cloud/future integration naming | Names for cloud, proxy, future external signal, facility, grid, GPU, and adapter boundaries. | Should keep control paths explicit and avoid implying LASE direct control. |
| API/view naming | Names for controllers, DTOs, static UI, and response/view shaping. | Should distinguish response/view presentation from route selection authority. |
| Configuration naming | Names for properties, policy modes, security filters, telemetry guards, and web configuration. | Should not hide production activation or LASE authority in ambiguous names. |

These categories are future planning categories only. They are not package names introduced by this sprint.

## Candidate Naming Rules

Candidate future naming rules:

- LASE-only classes should use clear `Lase`, `Shadow`, `Evaluation`, `Replay`, `Evidence`, or `ReviewerEvidence` naming where appropriate.
- Live allocation classes should avoid names that imply shadow-only behavior unless they are explicitly observation-only.
- Reviewer metadata classes should clearly signal metadata/view/reporting responsibilities.
- Evidence and replay classes should not imply production route authority.
- Future ports should include read-only wording when they are not mutation/control paths.
- Future observation objects should use snapshot, observation, source, status, or metadata wording when they are read-only.
- Future infrastructure names should make cloud, proxy, facility, grid, GPU, or external-control boundaries explicit.
- Names must not imply production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, or facility automation.
- Naming guard tests must not claim runtime enforcement.
- Naming guard tests must not replace package-boundary enforcement.
- Naming guard tests must be treated as a weak early signal only.

These are candidate rules only. This sprint does not implement broad source scanning, ArchUnit rules, dependency-direction checks, runtime enforcement, or package-boundary enforcement.

## Allowed Naming Examples

Allowed examples for future naming review:

| Example | Why it is acceptable as a name concept |
| --- | --- |
| `LaseShadowEvaluation` | Signals LASE and shadow/evaluation scope. |
| `LaseObservationSnapshot` | Signals read-only observation data. |
| `LaseEvidenceSummary` | Signals evidence summary, not live control. |
| `RoutingDecisionReviewerMetadata` | Signals reviewer-facing metadata. |
| `ExternalSignalSnapshot` | Signals a read-only external context snapshot. |
| `WorkloadProfileSignalMetadata` | Signals optional workload metadata rather than live behavior. |

These examples are naming examples only. They are not implemented by this sprint.

## Risky Naming Examples

Risky examples are examples of names to avoid, not implemented classes:

| Risky example | Why future naming guards should warn |
| --- | --- |
| `ProductionLaseRouter` | Implies LASE is production routing authority. |
| `CertifiedAdaptiveRouter` | Implies certification. |
| `LiveGridControlSignal` | Implies live power/grid control. |
| `GpuOrchestrator` | Implies GPU orchestration. |
| `CarbonAwareProductionRouter` | Implies carbon-aware production routing implementation. |
| `AutoFacilityController` | Implies facility automation/control. |
| `ReplayProofValidator` | Implies replay proof. |
| `ScoringProofEngine` | Implies scoring proof. |

These risky examples must not be read as existing source classes, API fields, runtime capabilities, or implementation proof.

## Future Docs/Test-Only Guard Strategy

A future docs/test-only naming guard may:

- verify that architecture docs link the current naming plan;
- verify that docs describe naming guards as weak early signals;
- verify that docs do not claim naming guards equal runtime enforcement;
- verify that docs do not claim naming guards equal package-boundary enforcement;
- verify that risky examples remain documented as examples to avoid;
- verify that reviewer-facing docs keep production, cloud, grid, GPU, carbon, facility, and certification claims negative or explicitly future-only;
- scan documentation-only naming examples for unsafe implication patterns if that scan is narrow, deterministic, and low-noise.

This sprint adds only documentation guard tests for this plan itself. It does not add broad source scanning that could create flaky or broad false-positive behavior. It does not add runtime naming enforcement. It does not add ArchUnit or package-boundary tooling.

## Future Implementation Gates

Before any future naming guard becomes broader than documentation assertions, reviewers should require:

- explicit sprint approval for naming guard implementation;
- a current inventory review, starting with [`LASE_NAMING_GUARD_INVENTORY.md`](LASE_NAMING_GUARD_INVENTORY.md);
- an agreed list of unsafe implication patterns;
- a small allowlist for documentation-only risky examples;
- deterministic tests with clear failure messages;
- proof that tests do not scan generated output or ignored artifacts;
- proof that tests do not block honest negative boundary text;
- proof that tests do not claim runtime or package-boundary enforcement;
- review that the guard does not create broad false positives across docs or source;
- separate approval before any package move, ArchUnit dependency, runtime interface, or enforcement tool is introduced.

## Safety Boundaries And Non-Goals

Explicit non-goals for this sprint:

- no production Java runtime behavior;
- no records/classes/interfaces under `src/main/java`;
- no package moves or refactors;
- no ArchUnit or any new dependency;
- no Maven build changes;
- no source scanning beyond documentation guard tests for this plan;
- no external API clients;
- no HTTP calls;
- no secrets, tokens, environment variables, credentials, config, or properties;
- no telemetry, storage, or persistence;
- no MessageDigest, SHA, hash, UUID, random, time, environment, or system-property behavior;
- no replay execution;
- no what-if mutation;
- no upload/share/download/export/PDF/ZIP behavior;
- no Docker, CI, release, signing, registry, or governance changes;
- no proxy behavior change;
- no strategy behavior change;
- no core routing behavior change;
- no scoring-internals behavior change;
- no runtime LASE boundary implementation;
- no runtime package-boundary enforcement;
- no runtime naming enforcement beyond documentation guard tests;
- no runtime workload model implementation;
- no runtime signal ingestion;
- no production behavior change;
- no live-cloud validation claim;
- no real-tenant validation claim;
- no GPU orchestration claim;
- no power/grid control claim;
- no carbon-aware routing implementation claim;
- no facility automation claim;
- no production readiness claim;
- no production certification claim.

This plan does not claim the LASE boundary is runtime-enforced. This plan does not claim the LASE package boundary is enforced. This plan does not claim ArchUnit or package-boundary tooling exists. This plan does not claim a runtime LASE naming guard exists. This plan does not claim current production readiness or production certification.

## Reviewer-Facing Value

This plan helps reviewers inspect naming intent before enforcement exists:

- future naming categories are visible;
- candidate naming rules are reviewable before package moves;
- allowed examples show safer vocabulary;
- risky examples show unsafe implications to avoid;
- naming guard limits are explicit;
- naming guard tests are described as weak early signals only;
- package-boundary enforcement remains separate;
- runtime enforcement remains separate;
- not-proven boundaries remain intact.

Reviewers should treat this plan as naming preparation only. It is not runtime enforcement, not package-boundary enforcement, not ArchUnit enforcement, not runtime naming enforcement, not production-readiness proof, and not production-certification proof.
