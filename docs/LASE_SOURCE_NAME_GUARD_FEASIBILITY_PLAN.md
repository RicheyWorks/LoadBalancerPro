# LASE Source-Name Guard Feasibility Plan

This document evaluates whether a future narrow source-name guard could safely help reviewers spot names that imply unsafe LASE authority or production proof. It is a feasibility plan only, not source scanning.

This is docs/test only. No source scanning is added in this sprint. No runtime naming guard is active. No runtime naming enforcement is added. No classes are renamed in this sprint. No package moves are made in this sprint. No source-name guard enforcement is added. No ArchUnit or package-boundary tool is added. No Maven build files are changed. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

## Executive Summary

PR #223 added [`LASE_BOUNDARY_NAMING_GUARD_PLAN.md`](LASE_BOUNDARY_NAMING_GUARD_PLAN.md), which defines future naming categories, candidate naming rules, allowed examples, risky examples to avoid, and weak early-signal guard ideas. PR #224 added [`LASE_NAMING_GUARD_INVENTORY.md`](LASE_NAMING_GUARD_INVENTORY.md), which maps current class/file naming against that plan before any naming guard enforcement exists.

This feasibility plan is the next preparation layer. It decides what a future source-name guard could safely check, what it must not check, and how reviewers should avoid broad false positives before any source-name scanning test is implemented.

The reviewer checklist for evaluating any future source-name guard proposal is documented in [`SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md). That checklist remains docs/test-only and does not add source scanning, runtime naming enforcement, source-name guard enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

The main feasibility finding is:

A future source-name guard may be useful only if it stays narrow, deterministic, reviewable, and explicitly non-proving. It should treat failures as review triggers, not proof of unsafe runtime behavior.

## Current Status: Feasibility Only, No Source Scanning

Current status:

- feasibility only;
- documentation and documentation guard tests only;
- no source scanning;
- no runtime naming guard is active;
- no runtime naming enforcement is added;
- no source-name guard enforcement is active;
- source-name guard not implemented yet;
- no classes are renamed in this sprint;
- no package moves are made in this sprint;
- no production Java runtime behavior is added;
- no records/classes/interfaces are added under `src/main/java`;
- no ArchUnit or package-boundary tool is added;
- no new dependency is added;
- no Maven build files are changed;
- no API fields are added;
- no routing, scoring, strategy, proxy, config, CI, Docker, release, signing, registry, governance, or production behavior changes are made.

This document is not source-name guard enforcement proof, runtime naming enforcement proof, runtime LASE boundary proof, package-boundary enforcement proof, ArchUnit tooling proof, production-readiness proof, production-certification proof, live-cloud validation proof, real-tenant validation proof, GPU orchestration proof, power/grid control proof, carbon-aware routing implementation proof, or facility automation proof.

## Relationship To LASE Boundary Naming Guard Plan

The naming guard plan defines the future vocabulary and warns that naming guards are weak early signals only. This feasibility plan narrows the next possible step: whether a future source-name guard should scan production source file names at all, and what constraints would be required before that happens.

This plan does not replace the naming guard plan. It does not activate the naming guard. It does not scan source names. It does not claim runtime enforcement.

## Relationship To LASE Naming Guard Inventory

The naming inventory maps current names into live allocation, LASE shadow/evaluation, replay/evidence, reviewer metadata, domain model, infrastructure/cloud/future integration, API/view, and configuration naming buckets.

This feasibility plan depends on that inventory because source-name guard design should not start from guesses. The inventory identifies current names that should be understood before any future denylist, allowlist, source scan, class rename, package move, or enforcement rule is approved.

The inventory remains inventory only. This feasibility plan remains feasibility only.

## Why Source-Name Guards Must Be Narrow

Source-name guards must be narrow because naming is contextual:

- documentation may intentionally include risky examples as names to avoid;
- negative boundary text may contain production, grid, GPU, carbon, facility, proof, certification, and readiness terms for safety reasons;
- existing live allocation names can be correct runtime names even when they imply authority;
- LASE, replay, evidence, and reviewer names can be safe when they include explicit shadow or metadata wording;
- broad scans can block honest documentation and create noisy false positives;
- broad scans can be mistaken for package-boundary or runtime enforcement;
- broad scans can encourage inline suppression spam instead of real review.

A future guard should be narrow enough that reviewers can understand every failure without reverse-engineering a brittle naming policy.

## Candidate Future Source-Name Guard Scope

Candidate future source-name guard scope, if separately approved later:

- only scan stable production source file names;
- do not scan generated output, ignored artifacts, `target/`, release bundles, or dependency directories;
- start with very narrow denylist terms that imply unsafe production claims;
- consider docs/test-only guard coverage before any source guard;
- prefer explicit allowlists for known safe class names and safe naming families;
- require clear failure messages that explain the unsafe implication;
- require a documented review process before adding new denylist terms;
- require easy suppression only through reviewed documentation, not inline ignore spam;
- treat guard failures as review triggers, not proof of unsafe runtime behavior;
- keep source-name guard enforcement separate from package moves, behavior changes, runtime interfaces, and production claims.

This sprint does not implement this scope.

## Out-Of-Scope Patterns

Out-of-scope patterns for any near-term future guard:

- broad source-content scanning;
- scanning documentation examples as if they were implemented classes;
- scanning negative boundary text as unsafe claims;
- scanning test names before production source names are understood;
- scanning generated files or ignored evidence output;
- scanning API response field names without an API contract review;
- scanning Maven, Docker, CI, release, governance, registry, or signing files;
- blocking existing live allocation names simply because they imply live authority;
- blocking LASE names that clearly include shadow, evaluation, replay, evidence, reviewer, metadata, snapshot, summary, or plan wording;
- adding ArchUnit, package-boundary tooling, or dependency changes as part of source-name guard feasibility;
- claiming runtime enforcement from naming checks.

## False-Positive Risks

False-positive risks:

| Risk | Why it matters | Mitigation |
| --- | --- | --- |
| Risky examples in docs | Names such as `CertifiedRouter` may appear as examples to avoid, not implemented classes. | Do not scan docs as source-name enforcement, or allowlist documented risky-example sections. |
| Negative boundary language | Docs must say no production certification, no live grid control, and similar limits. | Do not treat negative boundary language as implementation claims. |
| Existing live allocation names | `LoadBalancer` and allocation names correctly imply runtime authority. | Scope denylist to unsafe proof/control claims, not ordinary allocation vocabulary. |
| LASE evidence names | Replay/evidence names can be safe when they are bounded to reviewer metadata. | Require exact denylist themes and human review. |
| Future docs/test names | Documentation tests may intentionally name the risky concept they are guarding. | Start with production source file names only if approved. |

False positives should be treated as guard design failures, not as evidence that runtime behavior is unsafe.

## False-Negative Risks

False-negative risks:

| Risk | Why it matters | Mitigation |
| --- | --- | --- |
| Unsafe behavior with safe names | A class can have a safe name and still call a mutation path. | Use package-boundary and behavior tests later; naming is not enforcement. |
| Split wording | Risky claims can be hidden across multiple words or abbreviations. | Keep reviewer review in the loop; avoid overfitting string rules. |
| Existing broad names | Names can stay broad while documentation clarifies boundaries. | Pair naming inventory with future dependency review. |
| API/view ambiguity | Response/view names can summarize authority without owning it. | Keep API contract review separate from naming checks. |
| Infrastructure coupling | Names may not reveal direct calls to `CloudManager` or proxy mutation paths. | Future package-boundary enforcement is still required. |

False negatives are why naming guards must never replace architecture review, tests, dependency-direction checks, or runtime audits.

## Allowlist And Denylist Strategy

Future allowlist and denylist strategy:

- start from the naming inventory before adding denylist terms;
- keep the denylist very small and focused on unsafe implication themes;
- include allowlists for known safe existing names and safe naming families;
- require a plain-language reason for every denylist term;
- require a reviewer-approved update when a denylist term changes;
- require allowlist entries to describe why the name is safe or intentionally bounded;
- keep risky documentation examples allowlisted only as examples to avoid;
- prefer exact or tightly bounded pattern matches over broad substring matching;
- make suppressions documentation-reviewed rather than scattered inline ignores;
- document every guard limitation in the failure message.

Potential future denylist themes:

- production certification implication;
- replay proof implication;
- scoring proof implication;
- live grid control implication;
- facility automation implication;
- GPU orchestration implication;
- carbon-aware production routing implication;
- hidden production routing authority implication.

## Future Review Workflow

A future review workflow should require:

1. Confirm the source-name guard sprint is separately approved.
2. Re-read [`LASE_BOUNDARY_NAMING_GUARD_PLAN.md`](LASE_BOUNDARY_NAMING_GUARD_PLAN.md).
3. Re-read [`LASE_NAMING_GUARD_INVENTORY.md`](LASE_NAMING_GUARD_INVENTORY.md).
4. Complete [`SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md).
5. Confirm the guard scans only approved paths.
6. Confirm denylist terms are narrow and documented.
7. Confirm allowlist entries are reviewed and explain why they are safe.
8. Confirm failures produce actionable review messages.
9. Confirm source-name guard enforcement is not combined with package moves or behavior changes.
10. Confirm the guard is not used to claim production readiness, production certification, package-boundary enforcement, or runtime enforcement.

Guard failures are review triggers, not proof of unsafe runtime behavior.

## Future Implementation Gates

Before any future source-name guard is implemented, reviewers should require:

- explicit sprint approval for source-name scanning;
- current naming inventory review;
- approved source path scope;
- approved denylist themes;
- approved allowlist strategy;
- deterministic test behavior;
- clear failure messages;
- no generated-output scanning;
- no broad source-content scanning;
- no Maven build changes unless separately approved;
- no ArchUnit or package-boundary tooling unless separately approved;
- no class renames in the same sprint;
- no package moves in the same sprint;
- no routing, scoring, strategy, proxy, API, config, CI, Docker, release, governance, or production behavior changes;
- no production-readiness or production-certification claims.

## Example Risky Future Source-Name Patterns

Risky examples are names to avoid, not implemented classes:

| Risky future source-name pattern | Unsafe implication |
| --- | --- |
| `CertifiedRouter` | Implies certification. |
| `ProductionCertifiedBalancer` | Implies production certification. |
| `ReplayProofValidator` | Implies replay proof. |
| `ScoringProofEngine` | Implies scoring proof. |
| `LiveGridController` | Implies live power/grid control. |
| `FacilityAutomationManager` | Implies facility automation. |
| `GpuOrchestrator` | Implies GPU orchestration. |
| `CarbonAwareProductionRouter` | Implies carbon-aware production routing implementation. |
| `AutonomousProductionRouter` | Implies hidden production routing authority. |

These examples must not be read as existing source classes, API fields, runtime capabilities, or implementation proof.

## Example Safe Existing/Future Naming Families

Safe existing or future naming families:

| Naming family | Why it is safer |
| --- | --- |
| `LaseShadow` | Signals LASE shadow scope. |
| `LaseEvaluation` | Signals evaluation rather than production route selection. |
| `LaseEvidence` | Signals evidence/reviewer context. |
| `ReviewerMetadata` | Signals metadata/view responsibility. |
| `ReviewerSummary` | Signals reviewer-readable summary responsibility. |
| `ExternalSignalSnapshot` | Signals read-only snapshot context. |
| `WorkloadProfileSignalMetadata` | Signals optional workload metadata. |
| `BoundaryInventory` | Signals inventory, not enforcement. |
| `BoundaryPlan` | Signals planning, not implementation. |

Safe naming families do not prove safe runtime behavior. They only reduce misleading naming risk.

## Safety Boundaries And Non-Goals

Explicit non-goals:

- no production Java runtime behavior;
- no records/classes/interfaces under `src/main/java`;
- no class renames;
- no package moves or refactors;
- no source scanning logic in this sprint;
- no runtime naming enforcement;
- no source-name guard enforcement;
- no package-boundary enforcement;
- no runtime LASE boundary implementation;
- no runtime workload model implementation;
- no runtime signal ingestion;
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
- no live-cloud validation claim;
- no real-tenant validation claim;
- no GPU orchestration claim;
- no power/grid control claim;
- no carbon-aware routing implementation claim;
- no facility automation claim;
- no production readiness claim;
- no production certification claim.

Source-name guard enforcement must not be combined with package moves or behavior changes. This feasibility plan does not claim source-name guard enforcement is active. This feasibility plan does not claim a runtime-enforced LASE boundary. This feasibility plan does not claim package-boundary enforcement is active. This feasibility plan does not claim current production readiness or production certification.

## Reviewer-Facing Value

This feasibility plan helps reviewers decide whether a future source-name guard is worth adding:

- future source-name guard scope is constrained before implementation;
- out-of-scope patterns are explicit;
- false-positive and false-negative risks are documented;
- allowlist and denylist strategy is reviewable;
- risky example names are visibly examples to avoid;
- safe naming families are bounded as naming help, not proof;
- review workflow and implementation gates are explicit;
- source-name guard limitations are visible before tests exist;
- not-proven boundaries remain intact.

Reviewers should treat this plan as feasibility preparation only. It is not source-name scanning, not runtime naming enforcement, not source-name guard enforcement, not package-boundary enforcement, not ArchUnit enforcement, not production-readiness proof, and not production-certification proof.
