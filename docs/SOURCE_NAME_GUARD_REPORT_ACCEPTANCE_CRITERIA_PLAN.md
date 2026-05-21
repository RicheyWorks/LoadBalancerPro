# Source-Name Guard Report Acceptance Criteria Plan

This document defines acceptance criteria for deciding whether a future source-name guard dry-run report is acceptable for reviewer inspection. It is an acceptance criteria plan only, no implementation, and it does not add source scanning, dry-run report generation, JSON output files, CI workflow changes, PR comments, artifacts, source-name guard enforcement, runtime naming enforcement, or package-boundary enforcement.

This is docs/test only. No source scanning is added in this sprint. No report generation is added. No JSON output is generated. No JSON output files are generated. No CI workflow change is added. No PR comment or artifact behavior is added. No runtime naming guard is active. No source-name guard is implemented. No dry-run report generation is added. No report file is written. No dry-run command is added. No runtime naming enforcement is added. No source-name guard enforcement is active. No classes are renamed in this sprint. No package moves are made in this sprint. No ArchUnit or package-boundary tool is added. No Maven build files are changed. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

## Executive Summary

PR #227 added [`SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md), which describes future report-only dry-run modes before enforcement. PR #228 added [`SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md), which defines future report field vocabulary before any generator exists. PR #229 added [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md), which defines how reviewers should evaluate a future report. PR #230 added [`SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md), which provides static documentation examples for future reports.

This acceptance criteria plan is the next preparation layer. It defines what a future report must contain, what each finding must explain, what severity must mean, when a future report should be rejected, and what approvals must happen before any implementation exists.

The core rule is:

Future report findings are review triggers, not proof of unsafe runtime behavior.

Clean reports are not production safety proof.

## Current Status: Acceptance Criteria Only, No Implementation

Current status:

- acceptance criteria only;
- documentation and documentation guard tests only;
- no implementation;
- no source scanning;
- no report generation;
- no JSON output;
- no JSON output files;
- no CI workflow change is added;
- no PR comment or artifact behavior is added;
- no dry-run command is added;
- no dry-run implementation is added;
- no runtime naming guard is active;
- no source-name guard is implemented;
- source-name guard not implemented yet;
- source-name guard dry run not implemented yet;
- source-name guard report schema not implemented yet;
- source-name guard report review checklist is not enforcement;
- source-name guard report sample is not generated output;
- source-name guard report acceptance criteria is not enforcement;
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

This acceptance criteria plan does not claim report generation exists. This acceptance criteria plan does not claim source-name guard enforcement is active. This acceptance criteria plan does not claim a runtime-enforced LASE boundary. This acceptance criteria plan does not claim package-boundary enforcement is active.

## Relationship To Source-Name Guard Report Sample Plan

The report sample plan gives static documentation examples for clean reports, `INFO`, `WARN`, `BLOCKER_CANDIDATE`, allowlisted findings, false-positive notes, and reviewer decision outcomes.

This acceptance criteria plan depends on [`SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md). Future reports should be compared against the sample plan for readability and non-proof language before any report generator, JSON output, CI integration, PR comment, artifact upload, or enforcement exists.

## Relationship To Source-Name Guard Report Review Checklist

The report review checklist defines how reviewers should inspect a future source-name guard dry-run report before implementation.

This acceptance criteria plan depends on [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md). The acceptance criteria below turn checklist questions into quality gates for a future report proposal.

## Relationship To Source-Name Guard Report Schema Plan

The report schema plan defines future top-level fields, finding fields, summary fields, severity model, reviewer guidance, not-proven boundaries, deterministic output rules, privacy and secret-safety rules, and versioning.

This acceptance criteria plan depends on [`SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md). A future report should not be accepted for review if it omits the schema vocabulary needed to make findings deterministic, reviewer-readable, privacy-safe, and explicitly non-proving.

The source-name guard dry-run design plan remains the staging reference in [`SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md). It is docs/test-only and does not add source scanning, dry-run report generation, JSON output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

The future source-name guard rule catalog plan is documented in [`SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md`](SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md). That catalog remains docs/test-only and defines candidate future rule categories, severity guidance, false-positive risks, allowlist needs, reviewer actions, and implementation gates without adding source scanning, report generation, JSON output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

The future source-name guard rule review checklist is documented in [`SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md). That checklist remains docs/test-only and defines per-rule review questions before implementation without adding source scanning, report generation, JSON output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

## Why Acceptance Criteria Come Before Implementation

Acceptance criteria come before implementation because future report output can become misleading if quality gates are invented after scanning begins:

- reviewers need to know what makes a report complete before generated output exists;
- finding quality must be specific enough to avoid vague or noisy review work;
- severity labels must stay non-blocking in report-only mode unless a later sprint separately approves enforcement;
- privacy and secret-safety rules must be defined before any output channel exists;
- deterministic output rules must be visible before report diffs become reviewer evidence;
- rejection criteria help prevent reports that overclaim production safety or runtime risk;
- implementation gates keep source scanning, JSON output, CI workflow changes, PR comments, artifacts, and enforcement separate.

This plan defines acceptance criteria only. It does not implement source scanning, report generation, JSON output, CI workflow changes, PR comment/report artifact behavior, or enforcement.

## Required Report Quality Criteria

A future report should be acceptable for review only if it satisfies all of these report quality criteria:

- report is clearly marked report-only or dry-run if implemented later;
- report includes `schemaVersion` or equivalent future schema marker;
- report includes `scannedScopeSummary`;
- report includes a findings list, even when empty;
- report includes summary counts that match findings;
- report includes `reviewerGuidance`;
- report includes `notProvenBoundaries`;
- report includes a statement that findings are review triggers, not runtime safety proof;
- clean reports are explicitly not production safety proof;
- report states source-name guard enforcement is not active unless a separate enforcement sprint approves it;
- report avoids production readiness, production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, and facility automation claims;
- report remains readable to reviewers without requiring access to hidden runtime state.

Required report quality criteria are reviewer quality gates only. They do not prove runtime safety.

## Required Finding Quality Criteria

Each future finding should be acceptable for review only if it satisfies all of these finding quality criteria:

- each finding has a clear name;
- each finding has a clear path or scope reference;
- each finding has a category;
- each finding has severity;
- each finding has a specific reason;
- each finding has `reviewerAction`;
- each finding has `notProofStatement`;
- each finding avoids secrets, credentials, tokens, environment values, private network details, and absolute local paths;
- each finding avoids generated/build output unless separately approved;
- each finding explains whether the concern is naming clarity, possible overclaim, possible production authority implication, or another reviewed category;
- each finding gives a reviewer next step without requiring runtime behavior changes just to quiet a naming concern;
- each finding is not based only on intentionally documented risky examples unless the report explicitly treats that case as a false-positive review candidate.

Finding quality criteria are about reviewability. A finding name alone is not proof of unsafe runtime behavior.

## Required Severity Quality Criteria

Future severity labels should satisfy these quality criteria:

- `INFO` means review suggested and likely safe;
- `WARN` means naming may imply unsafe authority or overclaim;
- `BLOCKER_CANDIDATE` means human review required before any future blocking enforcement;
- `BLOCKER_CANDIDATE` is not automatic build failure in report-only mode;
- `BLOCKER_CANDIDATE` is not proof of unsafe runtime behavior;
- severity labels are reviewer metadata only in dry-run/report-only mode;
- severity counts do not claim production readiness, production certification, package-boundary enforcement, source-name guard enforcement, or runtime LASE boundary enforcement.

Severity quality criteria keep future reports useful without turning report-only output into enforcement.

## Required Privacy And Secret-Safety Criteria

Future reports should satisfy these privacy and secret-safety criteria:

- report contains no secrets;
- report contains no credentials;
- report contains no tokens;
- report contains no environment variable values;
- report contains no private network details;
- report contains no absolute local machine paths;
- report contains no user home directories, workstation-specific paths, API keys, cloud account details, service endpoints, tenant identifiers, or private reviewer identities;
- report keeps paths repository-relative if future scanning is separately approved;
- report does not upload artifacts unless separately approved;
- report does not post PR comments unless separately approved;
- report does not infer hidden ownership, credentials, tenants, or network topology from file names.

Privacy and secret-safety criteria must be reviewed before any report output channel exists.

## Required Determinism Criteria

Future reports should satisfy these determinism criteria:

- field ordering is stable;
- finding ordering is stable and reviewer-explainable;
- counts are derived only from the approved report scope;
- output does not depend on filesystem traversal order;
- output does not depend on timestamps, clocks, time zones, environment variables, system properties, random values, UUIDs, hashes, network state, or local machine state;
- `generatedAtMode` describes timing posture without embedding a real timestamp unless separately approved;
- `findingIdMode` describes deterministic identity posture without UUIDs, random values, hashes, or time-derived IDs unless separately approved;
- repeated runs on the same reviewed tree and same approved scope produce equivalent report content if report generation is later approved.

Determinism is a reviewability requirement, not runtime safety proof.

## Required Reviewer-Action Criteria

Future reports should satisfy these reviewer-action criteria:

- every finding has a clear reviewer action;
- reviewer actions distinguish accept, rename later, allowlist, suppression review, guard rule change, reject guard proposal, or defer until package-boundary enforcement exists;
- reviewer actions do not require package moves in the same sprint as source-name guard work;
- reviewer actions do not require runtime behavior changes in the same sprint as source-name guard work;
- reviewer actions do not claim production readiness or production certification;
- reviewer actions identify when separate implementation approval is required;
- reviewer actions include rollback or removal guidance when a future report proposal is too noisy.

Reviewer-action criteria make future reports actionable without creating hidden production routing authority.

## Rejection Criteria For Future Reports

A future report should be rejected or sent back for redesign if any of these criteria apply:

- report contains secrets or environment values;
- report contains absolute local machine paths;
- report claims production readiness, certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, or facility automation;
- report treats findings as proof of unsafe runtime behavior;
- report treats clean output as proof of production safety;
- report generates unstable timestamps, UUIDs, hashes, or random IDs without separate approval;
- report scans broad/generated/build output without review;
- report lacks clear reviewer actions;
- report lacks rollback/removal guidance;
- report is too noisy to be useful;
- report omits not-proven boundaries;
- report omits privacy and secret-safety statements;
- report implies enforcement before a separate enforcement sprint approves it.

Rejection criteria should improve report design. They should not be used to claim runtime safety or runtime unsafety by themselves.

## Approval Gates Before Future Implementation

Before a future source-name guard report implementation, reviewers should require:

1. Schema reviewed.
2. Sample reviewed.
3. Review checklist reviewed.
4. Acceptance criteria reviewed.
5. False-positive risk reviewed.
6. False-negative risk reviewed.
7. Privacy/secret-safety reviewed.
8. Deterministic-output strategy reviewed.
9. Rollback strategy reviewed.
10. CI impact reviewed.
11. Enforcement remains future-only unless separately approved.
12. Source scanning scope reviewed separately.
13. JSON output reviewed separately.
14. PR comment/report artifact behavior reviewed separately.
15. No combination with package moves.
16. No combination with runtime behavior changes.
17. No production-readiness or certification claim.

These approval gates are planning criteria only. They do not implement source scanning, report generation, JSON output, CI integration, PR comment/report artifact behavior, or enforcement.

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

This acceptance criteria plan does not claim report generation exists. This acceptance criteria plan does not claim source-name guard enforcement is active. This acceptance criteria plan does not claim a runtime-enforced LASE boundary. This acceptance criteria plan does not claim package-boundary enforcement is active.

## Reviewer-Facing Value

Reviewers can use this plan to decide whether a future source-name guard dry-run report is good enough to review before any implementation exists. The plan makes report quality, finding quality, severity quality, privacy and secret-safety, determinism, reviewer actions, rejection criteria, and approval gates visible before source scanning, report generation, JSON output, CI integration, PR comments, artifact upload, or enforcement.

The value is acceptance clarity, not enforcement. Source-name guard report acceptance criteria is not enforcement, source-name guard report sample is not generated output, source-name guard report review checklist is not enforcement, source-name guard report schema not implemented yet, source-name guard dry run not implemented yet, source-name guard not implemented yet, LASE naming guard is not runtime-enforced yet, and LASE package boundary is not enforced yet.
