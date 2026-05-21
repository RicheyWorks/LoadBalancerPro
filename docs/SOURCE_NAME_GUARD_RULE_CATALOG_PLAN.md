# Source-Name Guard Rule Catalog Plan

This document defines candidate future source-name guard rule categories. It is a rule catalog only, no implementation, and it does not add source scanning, dry-run report generation, JSON output files, CI workflow changes, PR comments, artifacts, source-name guard enforcement, runtime naming enforcement, or package-boundary enforcement.

This is docs/test only. No source scanning is added in this sprint. No report generation is added. No JSON output is generated. No JSON output files are generated. No CI workflow change is added. No PR comment or artifact behavior is added. No runtime naming guard is active. No source-name guard is implemented. No source-name guard rule implementation exists. No dry-run report generation is added. No report file is written. No dry-run command is added. No runtime naming enforcement is added. No source-name guard enforcement is active. No classes are renamed in this sprint. No package moves are made in this sprint. No ArchUnit or package-boundary tool is added. No Maven build files are changed. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

## Executive Summary

PR #225 added [`LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md`](LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md), which scopes a future narrow source-name guard before implementation. PR #226 added [`SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md), which gives reviewers implementation-readiness questions. PR #227 added [`SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md), which describes future report-only dry-run modes. PR #228 added [`SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md), which defines report field vocabulary. PR #229 added [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md), which defines future report review. PR #230 added [`SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md), which provides static sample reports. PR #231 added [`SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md), which defines report quality gates.

This rule catalog plan is the next preparation layer. It defines candidate future rule categories, risky example names, safe naming families, severity guidance, allowlist needs, false-positive risks, reviewer actions, and implementation gates before any source-name guard implementation exists.

The core rule is:

Future source-name guard rule hits are review triggers, not proof of unsafe runtime behavior.

## Current Status: Rule Catalog Only, No Implementation

Current status:

- rule catalog only;
- documentation and documentation guard tests only;
- no implementation;
- no source scanning;
- no report generation;
- no dry-run report generation;
- no JSON output;
- no JSON output files;
- no CI workflow change is added;
- no PR comment or artifact behavior is added;
- no dry-run command is added;
- no runtime naming guard is active;
- no source-name guard is implemented;
- source-name guard rule implementation does not exist;
- source-name guard not implemented yet;
- source-name guard dry run not implemented yet;
- source-name guard report schema not implemented yet;
- source-name guard report review checklist is not enforcement;
- source-name guard report sample is not generated output;
- source-name guard report acceptance criteria is not enforcement;
- source-name guard rule catalog is not implementation;
- no runtime naming enforcement is added;
- no source-name guard enforcement is active;
- no package-boundary enforcement is active;
- no classes are renamed in this sprint;
- no package moves are made in this sprint;
- no production Java runtime behavior is added;
- no records/classes/interfaces are added under `src/main/java`;
- no ArchUnit or package-boundary tool is added;
- no new dependency is added;
- no Maven build files are changed;
- no API fields are added;
- no routing, scoring, strategy, proxy, config, CI, Docker, release, signing, registry, governance, or production behavior changes are made.

This rule catalog plan does not claim source-name guard rule implementation exists. This rule catalog plan does not claim report generation exists. This rule catalog plan does not claim source-name guard enforcement is active. This rule catalog plan does not claim a runtime-enforced LASE boundary. This rule catalog plan does not claim package-boundary enforcement is active.

## Relationship To Source-Name Guard Report Acceptance Criteria Plan

The report acceptance criteria plan defines future report quality, finding quality, severity quality, privacy/secret-safety, determinism, rejection criteria, and approval gates.

This rule catalog depends on [`SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md). A future source-name guard rule proposal should satisfy those acceptance criteria before it can be considered for report generation, CI report-only mode, PR artifact behavior, or enforcement.

## Relationship To Source-Name Guard Report Schema Plan

The report schema plan defines future top-level report fields, finding fields, summary fields, severity model, reviewer guidance, not-proven boundaries, deterministic output rules, privacy and secret-safety rules, and versioning.

This rule catalog depends on [`SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md). Any future rule category should map cleanly to schema fields such as `category`, `severity`, `reason`, `reviewerAction`, `allowlistStatus`, `falsePositiveNotes`, and `notProofStatement`.

The future report review checklist is documented in [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md). The future dry-run design plan is documented in [`SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md). Both remain docs/test-only references and do not add source scanning, report generation, JSON output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

The future source-name guard rule review checklist is documented in [`SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md). That checklist remains docs/test-only and defines per-rule review questions for intent, scope, pattern specificity, severity, false-positive risk, false-negative risk, allowlists, suppressions, report output, rollback, and approval gates without adding source scanning, report generation, JSON output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

The future source-name guard allowlist design plan is documented in [`SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md). That plan remains docs/test-only and defines future allowlist purpose, entry fields, review workflow, expiration and re-review, suppression strategy, privacy/secret-safety, deterministic output, misuse risks, and implementation gates without adding allowlist files, source scanning, report generation, JSON/YAML/TOML output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

The future source-name guard allowlist review checklist is documented in [`SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md). That checklist remains docs/test-only and defines reviewer criteria for future allowlist candidates before any allowlist file, source scanning, report generation, JSON/YAML/TOML output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

## Why Rule Catalog Comes Before Implementation

Rule catalog comes before implementation because naming rules can become noisy or misleading if categories are invented after scanning begins:

- reviewers need to know which unsafe implication a rule is trying to detect;
- future denylist terms need category-specific false-positive review;
- future allowlist entries need to identify the rule category they suppress;
- severity labels should reflect the category without becoming automatic enforcement in report-only mode;
- risky documentation examples must stay examples to avoid, not implemented class claims;
- safe naming families should be visible before any scanner is written;
- rule hits must remain review triggers, not proof of unsafe runtime behavior;
- implementation gates keep source scanning, report generation, JSON output, CI integration, PR comments, artifacts, and enforcement separate.

This plan defines candidate rules only. It does not implement source scanning, report generation, JSON output, CI workflow changes, PR comment/report artifact behavior, or enforcement.

## Candidate Future Rule Categories

Candidate future rule categories:

| Category | Intended future review target | Default severity guidance | Boundary |
| --- | --- | --- | --- |
| `production-certification-overclaim` | Names that imply production certification, production readiness, or formally certified routing. | `BLOCKER_CANDIDATE` when explicit certification language appears. | Review trigger only, not proof of unsafe runtime behavior. |
| `replay-proof-overclaim` | Names that imply replay output proves correctness or safety. | `WARN` or `BLOCKER_CANDIDATE` depending on proof wording. | Replay evidence remains evidence, not proof. |
| `scoring-proof-overclaim` | Names that imply scoring math proves correctness or certification. | `WARN` or `BLOCKER_CANDIDATE` depending on proof wording. | Scoring evidence remains reviewer context, not production certification. |
| `live-grid-control-claim` | Names that imply live grid or power control. | `BLOCKER_CANDIDATE`. | No power/grid control implementation is claimed. |
| `facility-automation-claim` | Names that imply automated facility control. | `BLOCKER_CANDIDATE`. | No facility automation implementation is claimed. |
| `gpu-orchestration-claim` | Names that imply GPU orchestration. | `BLOCKER_CANDIDATE`. | No GPU orchestration implementation is claimed. |
| `carbon-aware-production-routing-claim` | Names that imply carbon-aware production routing. | `BLOCKER_CANDIDATE`. | Carbon-aware routing implementation is not claimed. |
| `hidden-production-routing-authority` | Names that imply hidden route selection authority outside the approved allocation path. | `WARN` or `BLOCKER_CANDIDATE`. | Naming does not grant routing authority. |
| `reviewer-metadata-ambiguity` | Names that blur reviewer metadata/reporting with runtime decisions. | `INFO` or `WARN`. | Reviewer metadata remains non-authoritative. |
| `shadow-vs-live-allocation-ambiguity` | Names that blur LASE shadow evaluation with live allocation or mutation. | `WARN`. | LASE boundary is not runtime-enforced yet. |

These categories are future candidates only. They are not implemented rules.

Example safe naming families to preserve or prefer where appropriate:

- `LaseShadow`;
- `LaseEvaluation`;
- `LaseEvidence`;
- `ReviewerMetadata`;
- `ReviewerSummary`;
- `BoundaryInventory`;
- `BoundaryPlan`;
- `ExternalSignalSnapshot`;
- `WorkloadProfileSignalMetadata`;
- `ReportOnlyFinding`.

Safe naming families are naming guidance only. They do not prove runtime safety, package-boundary enforcement, source-name guard enforcement, or production readiness.

## Production Certification Overclaim Rule Category

The `production-certification-overclaim` category would review names that imply production readiness, production certification, or formal safety certification. Example risky names to avoid include `CertifiedRouter` and `ProductionCertifiedBalancer`.

Why this category matters:

- certification language can be mistaken for evidence stronger than the project currently proves;
- clean local tests are not production certification;
- source-name guard findings are review triggers, not proof of unsafe runtime behavior;
- a future allowlist entry must not certify production safety.

Recommended future reviewer action: request rename in a later scoped PR, request documentation clarification, or reject the rule proposal if the term is too broad.

## Replay/Scoring Proof Overclaim Rule Category

The `replay-proof-overclaim` and `scoring-proof-overclaim` categories would review names that imply replay or scoring evidence proves runtime correctness. Example risky names to avoid include `ReplayProofValidator`, `ScoringProofEngine`, and `ReviewerProofEngine`.

Why these categories matter:

- replay and evidence docs can explain decisions without proving production safety;
- scoring explanations can help reviewers without proving global optimality or certification;
- proof wording can overstate what a source-name report can establish;
- reviewer metadata should not become hidden production authority.

Recommended future reviewer action: clarify the name, move proof language into bounded documentation, or require human review before any future blocking enforcement.

## Live-Control Authority Rule Category

The `live-grid-control-claim` and `hidden-production-routing-authority` categories would review names that imply live control authority or hidden production route selection. Example risky names to avoid include `LiveGridController`, `AutonomousProductionRouter`, and `HiddenProductionRouter`.

Why this category matters:

- live allocation and mutation paths must remain separate from LASE shadow/evidence paths;
- names that imply live control can obscure whether a component is report-only, reviewer metadata, or runtime behavior;
- hidden routing authority must not be introduced by naming, documentation, or future guard reports.

Recommended future reviewer action: confirm the name is not implying production route selection, request rename later, or defer until package-boundary enforcement exists.

## GPU/Grid/Facility/Control Rule Category

The `gpu-orchestration-claim`, `live-grid-control-claim`, and `facility-automation-claim` categories would review names that imply GPU orchestration, grid/power control, or facility automation. Example risky names to avoid include `GpuOrchestrator`, `LiveGridController`, and `FacilityAutomationManager`.

Why this category matters:

- GPU orchestration is not implemented;
- power/grid control is not implemented;
- facility automation is not implemented;
- infrastructure and control naming can be misread as integration proof.

Recommended future reviewer action: treat explicit control names as `BLOCKER_CANDIDATE` in report-only mode and require human review before any enforcement.

## Carbon-Aware Production Routing Rule Category

The `carbon-aware-production-routing-claim` category would review names that imply carbon-aware production routing. Example risky names to avoid include `CarbonAwareProductionRouter`.

Why this category matters:

- future Tier 2/Tier 3 signal concepts are read-only planning references unless separately implemented;
- source names must not imply carbon-aware production routing implementation;
- current documentation does not prove live-cloud validation, real-tenant validation, power/grid control, or facility automation.

Recommended future reviewer action: request safer wording, clarify docs-only context, or defer until a separate approved implementation exists.

## Hidden Production-Routing Authority Rule Category

The `hidden-production-routing-authority` category would review names that imply a component selects production routes, controls allocation, or overrides approved routing policy outside the live allocation path. Example risky names to avoid include `AutonomousProductionRouter` and `HiddenProductionRouter`.

Why this category matters:

- LASE may observe, compare, explain, and produce evidence, but should not become hidden production routing authority;
- report-only source-name findings must not be treated as dependency-direction proof;
- package-boundary enforcement remains future-only.

Recommended future reviewer action: inspect whether naming clarity, docs clarification, or later package-boundary enforcement is the right response.

## Reviewer Metadata Ambiguity Rule Category

The `reviewer-metadata-ambiguity` category would review names that blur reviewer metadata with proof, certification, or runtime authority. Example risky name to avoid: `ReviewerProofEngine`.

Safe naming families include `ReviewerMetadata`, `ReviewerSummary`, `LaseEvidence`, `BoundaryInventory`, `BoundaryPlan`, and `ReportOnlyFinding`.

Why this category matters:

- reviewer-facing outputs can summarize evidence without controlling routing;
- names should make metadata/reporting responsibilities clear;
- ambiguous reviewer names should usually be `INFO` or `WARN`, not automatic blockers in report-only mode.

Recommended future reviewer action: confirm the name stays reviewer metadata only, request docs clarification, or request a later scoped rename.

## Future Allowlist Strategy By Rule Category

Future allowlist strategy:

- allowlist entries must have a rationale;
- allowlist entries must identify the rule category;
- allowlist entries must be reviewed;
- allowlist entries must not certify production safety;
- allowlist entries must be easy to audit;
- suppressions should not be inline ignore spam;
- allowlist does not replace package-boundary enforcement;
- allowlist entries should distinguish safe existing names, intentionally documented risky examples, and temporary rename deferrals;
- allowlist reviews should include false-positive and false-negative notes when appropriate.

Allowlists are reviewer records only. They do not prove runtime safety.

## Future False-Positive Risk By Rule Category

Future false-positive risks:

| Category family | False-positive risk | Mitigation |
| --- | --- | --- |
| Production certification | Documentation may mention certification as a non-goal. | Exclude negative boundary text and examples to avoid. |
| Replay/scoring proof | Tests or docs may use proof language to deny proof claims. | Require reason text and not-proof statements. |
| Live-control authority | Historical docs may discuss future control paths as non-goals. | Scan only approved source names if separately approved later. |
| GPU/grid/facility/control | Risky examples may be intentionally documented. | Treat documented risky examples as examples to avoid, not implemented classes. |
| Carbon-aware production routing | Future strategy docs may discuss carbon-aware routing as not implemented. | Keep report scope narrow and reviewer-approved. |
| Reviewer metadata ambiguity | Reviewer docs may intentionally use reviewer/proof language in explanations. | Prefer `INFO`/`WARN` and allowlist with rationale. |
| Shadow-vs-live allocation ambiguity | Existing class names may predate the future package model. | Use findings as migration-review prompts, not enforcement proof. |

False positives should improve future guard design. They should not force runtime behavior changes or package moves.

## Future Severity Guidance By Rule Category

Future severity guidance:

- `INFO`: name should be reviewed for clarity but is likely safe.
- `WARN`: name may imply unsafe authority, runtime control, proof, or overclaim.
- `BLOCKER_CANDIDATE`: name appears to imply production certification, production routing authority, live control, proof, or facility/grid/GPU control and requires human review before any future blocking enforcement.

Important severity boundary:

`BLOCKER_CANDIDATE` is not automatic build failure in report-only mode and is not proof of unsafe runtime behavior.

`BLOCKER_CANDIDATE` is not proof of unsafe runtime behavior.

Severity labels are reviewer metadata only until a separate enforcement sprint is approved.

## Future Reviewer Actions By Rule Category

Future reviewer actions:

- accept as safe with no change;
- request documentation clarification;
- request rename in a later scoped PR;
- request allowlist entry with rationale and rule category;
- request suppression review without inline ignore spam;
- request rule category refinement before implementation;
- downgrade severity if the rule is too broad;
- reject guard proposal as too noisy;
- defer until package-boundary enforcement exists;
- require separate approval before any blocking enforcement.

Reviewer actions should stay separate from package moves, behavior changes, report generation, CI workflow changes, PR comments, artifacts, and production claims.

## Future Implementation Gates

Before a future source-name guard rule implementation, reviewers should require:

1. Rule catalog reviewed.
2. Report schema reviewed.
3. Sample report reviewed.
4. Report review checklist reviewed.
5. Acceptance criteria reviewed.
6. False-positive risk reviewed.
7. False-negative risk reviewed.
8. Allowlist strategy reviewed.
9. Deterministic output reviewed.
10. Privacy/secret-safety reviewed.
11. Source scanning scope reviewed separately.
12. JSON output reviewed separately.
13. CI report-only behavior reviewed separately.
14. PR comment/report artifact behavior reviewed separately.
15. Enforcement remains future-only unless separately approved.

These gates are planning criteria only. They do not implement source scanning, report generation, JSON output, CI integration, PR comment/report artifact behavior, or enforcement.

## Safety Boundaries And Non-Goals

Explicit non-goals:

- no production Java runtime behavior;
- no records/classes/interfaces under `src/main/java`;
- no class renames;
- no package moves or refactors;
- no source scanning logic in this sprint;
- no dry-run command;
- no dry-run implementation;
- no report generation;
- no JSON output files;
- no JSON output;
- no CI workflow changes;
- no PR comment/report artifact behavior;
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
- no production readiness claim;
- no production certification claim;
- no live-cloud validation claim;
- no real-tenant validation claim;
- no GPU orchestration claim;
- no power/grid control claim;
- no carbon-aware routing implementation claim;
- no facility automation claim.

This rule catalog plan does not claim source-name guard rule implementation exists. This rule catalog plan does not claim report generation exists. This rule catalog plan does not claim source-name guard enforcement is active. This rule catalog plan does not claim a runtime-enforced LASE boundary. This rule catalog plan does not claim package-boundary enforcement is active.

## Reviewer-Facing Value

Reviewers can use this plan to understand what future source-name guard rules might review before any scanner, report generator, JSON output, CI integration, PR comment, artifact upload, or enforcement exists. The plan makes rule categories, risky example names, safe naming families, severity guidance, allowlist expectations, false-positive risks, reviewer actions, and implementation gates visible before implementation.

The value is rule clarity, not implementation. Source-name guard rule catalog is not implementation, source-name guard report acceptance criteria is not enforcement, source-name guard report sample is not generated output, source-name guard report review checklist is not enforcement, source-name guard report schema not implemented yet, source-name guard dry run not implemented yet, source-name guard not implemented yet, LASE naming guard is not runtime-enforced yet, and LASE package boundary is not enforced yet.
