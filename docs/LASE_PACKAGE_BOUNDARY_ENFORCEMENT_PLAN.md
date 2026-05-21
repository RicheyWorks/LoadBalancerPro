# LASE Package Boundary Enforcement Plan

This document explains how LoadBalancerPro should eventually move from the LASE boundary architecture contract and current-tree inventory to actual package-boundary enforcement.

This is planning only, not enforcement. It is docs/test only. No ArchUnit or package-boundary tool is added in this sprint. No classes are moved in this sprint. No packages are refactored. No Maven build files are changed. No runtime interfaces, API fields, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, or production behavior are added.

The future naming preparation layer is documented in [`LASE_BOUNDARY_NAMING_GUARD_PLAN.md`](LASE_BOUNDARY_NAMING_GUARD_PLAN.md). That plan is docs/test-only naming guard preparation, not runtime naming enforcement, package-boundary enforcement, ArchUnit tooling, package moves, runtime interfaces, Maven build changes, or behavior change.

The current naming inventory is documented in [`LASE_NAMING_GUARD_INVENTORY.md`](LASE_NAMING_GUARD_INVENTORY.md). That inventory maps current class/file naming before any source-name guard test, package move, ArchUnit rule, runtime naming enforcement, package-boundary enforcement, or behavior change.

The source-name guard feasibility plan is documented in [`LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md`](LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md). That plan keeps any future source-name guard narrow and review-trigger-only before source scanning, package moves, ArchUnit rules, runtime naming enforcement, package-boundary enforcement, or behavior change.

## Executive Summary

PR #220 added [`LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md`](LASE_BOUNDARY_ARCHITECTURE_CONTRACT.md), which defines the desired future boundary between live allocation and LASE shadow/evidence paths. PR #221 added [`LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md`](LASE_BOUNDARY_ENFORCEMENT_INVENTORY.md), which maps the current tree into future boundary buckets.

This plan describes the staged path from that inventory to future package-boundary enforcement. It intentionally keeps enforcement work separate from package moves, runtime behavior changes, and production claims.

The central rule remains:

LASE may observe, compare, explain, and generate reviewer evidence, but LASE must not directly mutate live routing state, directly select production routes, directly alter proxy behavior, directly call `CloudManager` or future grid/facility/GPU control paths, write production configuration, or become hidden production routing authority.

## Current Status: Planning Only, Not Enforcement

Current status:

- planning only;
- documentation and documentation guard tests only;
- no runtime enforcement;
- no package-boundary enforcement;
- LASE package boundary is not currently enforced;
- no ArchUnit or package-boundary tool is added in this sprint;
- no new dependency is added in this sprint;
- no Maven build files are changed in this sprint;
- no classes are moved in this sprint;
- no packages are renamed or refactored in this sprint;
- no production Java runtime behavior is added;
- no records/classes/interfaces are added under `src/main/java`;
- no runtime LASE boundary implementation is added;
- no runtime package-boundary enforcement is added;
- no API fields are added;
- no routing, scoring, strategy, proxy, config, CI, Docker, release, signing, registry, governance, or production behavior changes are made.

This document is not enforcement proof, production-readiness proof, production-certification proof, live-cloud validation proof, real-tenant validation proof, GPU orchestration proof, power/grid control proof, carbon-aware routing implementation proof, or facility automation proof.

## Relationship To LASE Boundary Architecture Contract

The LASE boundary architecture contract defines the target shape:

- live allocation remains the explicit live decision path;
- LASE remains shadow/evaluation/evidence by default;
- replay/evidence remains reviewer controlled;
- reviewer metadata remains read-only;
- future ExternalSignalPort concepts remain read-only context;
- future WorkloadProfile metadata remains optional and deterministic;
- missing or stale inputs degrade safely;
- any future LASE influence requires explicit policy gates and separate approval.

This plan does not replace the contract. It translates the contract into future enforcement phases that can be reviewed before any package move or enforcement tool appears.

## Relationship To LASE Boundary Enforcement Inventory

The current-tree inventory maps today's code into future buckets: live allocation, LASE shadow/evaluation, replay/evidence, reviewer metadata, domain model candidates, infrastructure/cloud/future integration, API/view, and configuration.

This plan depends on that inventory because package-boundary enforcement should not start from guesses. Future maintainers should first confirm the inventory, identify high-risk classes, and preserve behavior before moving or enforcing anything.

The inventory remains a map, not enforcement. This plan remains a plan, not enforcement.

## Why Package-Boundary Enforcement Must Be Staged

Package-boundary enforcement can improve maintainability and reviewer confidence, but doing it too early can create risk:

- package moves can hide accidental behavior changes;
- enforcement tooling can create build and dependency churn;
- runtime port introductions can blur API and behavior ownership;
- LASE and allocation are currently close enough that a naive move could alter routing semantics;
- `CloudManager`, proxy behavior, and future facility/grid/GPU boundaries need explicit dependency-direction review;
- reviewers need to know whether a sprint changed architecture documentation, package layout, runtime behavior, or production claims.

Staging keeps each risk small and reviewable.

Important warning:

Never combine package moves, enforcement tooling, behavior changes, and production claims in the same sprint.

## Proposed Future Package Model

The future package model is conceptual only in this sprint:

| Future package | Intended responsibility | Required boundary |
| --- | --- | --- |
| `domain` | Pure models, facts, events, observations, decisions, signals, and reviewer-safe evidence vocabulary. | No infrastructure dependencies and no mutation side effects. |
| `allocation` | Live routing/allocation decision path, health-aware selection, policy-gated allocation behavior, and live decision orchestration. | Does not grant LASE direct mutation handles or hidden production authority. |
| `lase` | Shadow evaluation, comparison, replay support, evidence generation, explanation metadata, and reviewer findings. | Shadow/evidence by default; no direct live routing mutation or production route selection. |
| `infrastructure` | Guarded external/cloud/future integration boundaries, proxy adapters, file/tooling boundaries, and future facility/grid/GPU adapters if separately approved. | Not called directly from LASE except through approved read-only ports. |
| `api` | Controllers, DTOs, reviewer/API response surfaces, static UI, and view shaping. | Exposes reviewer-readable state without changing route selection or field semantics. |
| `config` | Application configuration, security filters, rate limits, telemetry guards, and web configuration. | No hidden activation of LASE production authority. |

This plan does not introduce these packages. It documents where future enforcement could land.

## Proposed Enforcement Phases

Future enforcement should proceed in small, reversible phases:

| Phase | Name | Future scope | Required boundary |
| --- | --- | --- | --- |
| E0 | Current inventory and docs guard tests | Keep the architecture contract and current-tree inventory current. | No enforcement claim. |
| E1 | Naming and docs-only boundary guard tests | Add non-invasive documentation and naming tests that describe intended ownership. | No package moves and no behavior changes. |
| E2 | Introduce package skeletons only, no class moves | Add empty or placeholder package directories only if separately approved and meaningful for navigation. | No runtime interfaces, classes, or behavior. |
| E3 | Move pure domain models only, no behavior changes | Move side-effect-free models first after compatibility tests are in place. | No scoring, routing, strategy, proxy, or API semantic changes. |
| E4 | Introduce read-only observation port only, no behavior changes | Add a `LaseObservationPort`-style boundary only after explicit approval. | Observation-only, no mutation methods, no production route selection. |
| E5 | Move LASE-only classes only, no behavior changes | Move LASE shadow/evidence classes after domain and observation boundaries are stable. | LASE remains shadow/evidence unless future policy gates approve otherwise. |
| E6 | Add package-boundary enforcement tool such as ArchUnit only after dependency/build impact review | Introduce enforcement tooling only after dependency, CI, and build effects are reviewed. | Enforcement tooling must not be bundled with behavior changes or production claims. |
| E7 | Enforce no LASE-to-mutation-path dependency | Prevent LASE packages from depending on allocation mutation services or live route selection APIs. | LASE must not mutate live routing state or become production route selector. |
| E8 | Enforce no LASE-to-cloud/facility/grid/GPU control dependency | Prevent LASE packages from calling `CloudManager` or future cloud/facility/grid/GPU control paths directly. | External context must use approved read-only ports only. |
| E9 | Add reviewer-visible boundary evidence only after enforcement is real | Add reviewer metadata that describes enforcement status only after tests enforce it. | Reviewer evidence must not claim more than the enforcement actually proves. |

Each phase should be its own separately scoped sprint unless an explicit approval says otherwise.

## Candidate Rules For Future Enforcement

Candidate future enforcement rules:

- `lase` must not depend on allocation mutation services.
- `lase` must not directly mutate live routing state.
- `lase` must not become production route selector.
- `lase` must not directly call proxy mutation paths.
- `lase` must not directly call `CloudManager` or future grid/facility/GPU control paths.
- `lase` must not write production configuration.
- `allocation` may publish read-only observation snapshots.
- `reviewer metadata` may summarize LASE outputs without granting routing authority.
- `domain` must remain free of infrastructure dependencies.
- `infrastructure` must not be called from LASE without an approved read-only port.
- Package-boundary enforcement must not be used to claim production readiness.
- Package-boundary enforcement must not be used to claim production certification.
- Package-boundary enforcement must not be used to claim live-cloud validation or real-tenant validation.

These are candidate rules only. This sprint does not implement ArchUnit, package-boundary tooling, dependency-direction checks, or runtime enforcement.

## Migration Risks And Mitigations

| Risk | Why it matters | Mitigation |
| --- | --- | --- |
| Accidental routing behavior change | Moving allocation-adjacent classes can alter imports, visibility, or constructor wiring. | Move only one category at a time and run full routing/API tests. |
| Hidden scoring change | Domain/scoring models can be tightly coupled to explanation and comparison output. | Keep scoring migrations separate from scoring algorithm changes. |
| LASE becomes hidden authority | Shadow recommendations can look like live control if naming and dependency direction are unclear. | Enforce no LASE-to-mutation-path dependency before adding reviewer-visible enforcement claims. |
| Cloud/proxy boundary confusion | `CloudManager` and proxy classes are live-capable boundaries. | Keep LASE away from direct cloud/proxy/facility/grid/GPU control dependencies. |
| Build churn from enforcement tooling | ArchUnit or similar tools can add dependency and CI impact. | Review dependency/build impact before adding enforcement tooling. |
| Reviewer overclaim | A package-boundary test can be misread as production certification. | Keep not-proven boundaries in docs and reviewer metadata. |

## What Must Not Be Combined In One Sprint

Do not combine these in one sprint:

- package moves and behavior changes;
- package moves and scoring changes;
- package moves and proxy behavior changes;
- package moves and API field changes;
- package moves and production claims;
- enforcement tooling and production-readiness claims;
- ArchUnit or equivalent dependency additions and runtime behavior changes;
- LASE boundary enforcement and cloud/facility/grid/GPU control work;
- reviewer-visible enforcement evidence and not-yet-enforced boundaries;
- release/signing/registry/governance work and package-boundary enforcement.

Never combine package moves, enforcement tooling, behavior changes, and production claims in the same sprint.

## Required Approval Gates Before Enforcement

Before future enforcement starts, reviewers should require:

- explicit sprint approval for enforcement work;
- a current inventory review;
- an owner-approved package model;
- dependency/build impact review for any enforcement tool;
- CI impact review;
- API contract impact review;
- tests proving routing behavior is unchanged;
- tests proving scoring behavior is unchanged;
- tests proving strategy behavior is unchanged;
- tests proving proxy behavior is unchanged;
- tests proving no direct LASE call to allocation mutation paths;
- tests proving no direct LASE call to `CloudManager` or future cloud/facility/grid/GPU control paths;
- documentation that distinguishes enforcement from production readiness;
- rollback instructions for reverting package moves or enforcement tooling.

## Testing Strategy For Future Enforcement

Future testing should progress in layers:

1. Documentation guard tests for intended boundaries.
2. Naming tests that keep future package labels and links discoverable.
3. Compatibility tests around moved domain models.
4. API contract tests proving response semantics remain stable.
5. Routing and scoring regression tests proving behavior remains unchanged.
6. Shadow/evidence tests proving LASE remains observation/comparison/evidence by default.
7. Package-boundary tests after an approved enforcement tool is added.
8. Negative dependency tests that block LASE-to-mutation-path and LASE-to-control-path dependencies.

Package-boundary tests should prove only what they enforce. They should not claim replay proof, scoring proof, correctness validation, production readiness, production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, or facility automation.

## Rollback Strategy For Future Enforcement

Future enforcement should be rollback-friendly:

- keep package moves separate from behavior changes;
- use small PRs with clear before/after file lists;
- preserve old public API semantics;
- avoid mixing enforcement tooling with dependency upgrades;
- make any package-boundary tool removable without deleting runtime code;
- keep reviewer-visible enforcement claims out until enforcement is real;
- if enforcement causes build friction, revert the enforcement phase before touching live allocation, LASE, proxy, or cloud behavior.

Rollback should restore the previous package layout or enforcement configuration without changing routing behavior.

## Safety Boundaries And Non-Goals

Explicit non-goals for this sprint:

- no production Java runtime behavior;
- no records/classes/interfaces under `src/main/java`;
- no package moves or refactors;
- no ArchUnit or any new dependency;
- no Maven build changes;
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

This plan does not claim LASE boundary is runtime-enforced. This plan does not claim LASE package boundary is enforced. This plan does not claim ArchUnit or package-boundary tooling exists. This plan does not claim current production readiness or production certification.

## Reviewer-Facing Value

This plan helps reviewers see the intended route from architecture documents to future enforcement:

- the architecture contract defines the boundary;
- the inventory maps the current tree;
- this plan stages future enforcement;
- future package moves are separated from behavior changes;
- future enforcement tooling is separated from production claims;
- candidate rules are visible before implementation;
- rollback and approval gates are explicit;
- not-proven boundaries remain intact.

Reviewers should treat this plan as enforcement preparation only. It is not runtime enforcement, not package-boundary enforcement, not ArchUnit enforcement, not production-readiness proof, and not production-certification proof.
