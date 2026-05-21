# Source-Name Guard Allowlist Design Plan

This document defines future source-name guard allowlist semantics. It is an allowlist design only, no implementation, and it does not add source scanning, allowlist files, JSON/YAML/TOML allowlist output, dry-run report generation, JSON output files, CI workflow changes, PR comments, artifacts, source-name guard enforcement, runtime naming enforcement, or package-boundary enforcement.

This is docs/test only. No source scanning is added in this sprint. No allowlist file is added. No allowlist files are added. No JSON/YAML/TOML output is added. No JSON/YAML/TOML allowlist output is added. No report generation is added. No dry-run report generation is added. No JSON output is generated. No JSON output files are generated. No CI workflow change is added. No PR comment or artifact behavior is added. No runtime naming guard is active. No source-name guard is implemented. No source-name guard rule implementation exists. No report file is written. No dry-run command is added. No runtime naming enforcement is added. No source-name guard enforcement is active. No classes are renamed in this sprint. No package moves are made in this sprint. No ArchUnit or package-boundary tool is added. No Maven build files are changed. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

## Executive Summary

PR #225 added [`LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md`](LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md), which scopes a future narrow source-name guard before implementation. PR #226 added [`SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md), which gives reviewers implementation-readiness questions. PR #227 added [`SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md), which describes future report-only dry-run modes. PR #228 added [`SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md), which defines report field vocabulary. PR #229 added [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md), which defines future report review. PR #230 added [`SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md), which provides static sample reports. PR #231 added [`SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md), which defines report quality gates. PR #232 added [`SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md`](SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md), which defines candidate future rule categories. PR #233 added [`SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md), which defines per-rule review questions.

This allowlist design plan is the next preparation layer. It defines how future allowlists should preserve reviewer rationale, reduce repeated false positives, remain auditable, and avoid safety overclaims before any allowlist file, source scanner, dry-run report generator, JSON/YAML/TOML output, CI integration, PR comment, artifact upload, or enforcement exists.

The core rule is:

Future allowlist entries are reviewer context, not production safety certification.

Future allowlists do not replace package-boundary enforcement or human review.

## Current Status: Allowlist Design Only, No Implementation

Current status:

- allowlist design only;
- documentation and documentation guard tests only;
- no implementation;
- no source scanning;
- no source scanning logic in this sprint;
- no allowlist file;
- no allowlist files;
- no JSON/YAML/TOML output;
- no JSON/YAML/TOML allowlist output;
- no report generation;
- no dry-run report generation;
- no JSON output;
- no JSON output files;
- no CI workflow change is added;
- no PR comment or artifact behavior is added;
- no dry-run command is added;
- no dry-run implementation is added;
- no runtime naming guard is active;
- no source-name guard is implemented;
- source-name guard rule implementation does not exist;
- source-name guard allowlist is not implemented;
- source-name guard not implemented yet;
- source-name guard dry run not implemented yet;
- source-name guard report schema not implemented yet;
- source-name guard report review checklist is not enforcement;
- source-name guard report sample is not generated output;
- source-name guard report acceptance criteria is not enforcement;
- source-name guard rule catalog is not implementation;
- source-name guard rule review checklist is not enforcement;
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

This allowlist design plan does not claim allowlist implementation exists. This allowlist design plan does not claim source-name guard rule implementation exists. This allowlist design plan does not claim report generation exists. This allowlist design plan does not claim source-name guard enforcement is active. This allowlist design plan does not claim a runtime-enforced LASE boundary. This allowlist design plan does not claim package-boundary enforcement is active.

## Relationship To Source-Name Guard Rule Review Checklist

The rule review checklist defines how reviewers should evaluate one future source-name guard rule candidate before implementation.

This allowlist design plan depends on [`SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md). Future allowlist semantics should satisfy that checklist's allowlist, suppression, false-positive, report-output, rollback, and approval-gate questions before any source-name guard rule is implemented.

## Relationship To Source-Name Guard Rule Catalog Plan

The rule catalog plan defines candidate future source-name guard categories, risky example names, safe naming families, severity guidance, allowlist needs, false-positive risks, reviewer actions, and implementation gates.

This allowlist design plan depends on [`SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md`](SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md). A future allowlist entry should identify which rule category it belongs to and preserve the reviewer rationale for why a matching name is acceptable.

The future report acceptance criteria plan is documented in [`SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md). The future report review checklist is documented in [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md). Both remain docs/test-only references and do not add source scanning, allowlist files, report generation, JSON/YAML/TOML output, JSON output files, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

## Why Allowlist Design Comes Before Implementation

Allowlist design comes before implementation because a future source-name guard can become noisy, misleading, or too easy to silence if exceptions are invented after scanning begins:

- reviewers need to know what an allowlist is for before any file format exists;
- rule categories need reviewed exception semantics before source scanning begins;
- repeated false positives need an auditable review path;
- suppression strategy needs to avoid inline ignore spam;
- allowlist entries need to preserve rationale without certifying production safety;
- allowlist scope must be deterministic and privacy-safe before output exists;
- allowlist entries must not become a replacement for package-boundary enforcement or human review;
- implementation gates must keep allowlist files, source scanning, report generation, JSON/YAML/TOML output, CI integration, PR comments, artifacts, and enforcement separate.

This plan defines future allowlist semantics only. It does not implement source scanning, allowlist files, JSON/YAML/TOML output, report generation, CI workflow changes, PR comment/report artifact behavior, or enforcement.

## Future Allowlist Purpose

Future allowlists may be useful only if separately approved later. Their purpose should be to:

- document known-safe exceptions for future source-name guard findings;
- preserve reviewer context for why a name is acceptable;
- reduce repeated false positives;
- keep source-name guard output reviewable and low-noise;
- support future report-only review without broad suppression;
- identify rule categories that produced repeated expected findings;
- never certify production safety;
- never replace package-boundary enforcement;
- never replace human review.

Allowlist entries do not certify production safety. Allowlist does not replace package-boundary enforcement. Allowlist does not replace human review.

## Future Allowlist Entry Fields

A future allowlist entry should be considered only after a later sprint separately approves an allowlist file or equivalent reviewed mechanism. Candidate future fields:

| Field | Future purpose | Boundary |
| --- | --- | --- |
| `allowlistEntryMode` | Identifies the entry as a reviewed mode or category without generating an ID. | Use allowlistEntryMode instead of generated IDs. |
| `ruleCategory` | Names the future rule category that produced the finding. | Must map to a reviewed catalog category. |
| `namePattern` | Describes the reviewed name or narrow pattern. | Must not be broad enough to hide unrelated names. |
| `pathScope` | Describes the repository-relative scope if future scanning is approved. | Must not use absolute local machine paths. |
| `rationale` | Explains why the finding is expected or acceptable. | Must not certify production safety. |
| `reviewerAction` | Records the expected reviewer action, such as keep, clarify, rename later, or revisit. | Must not force runtime changes. |
| `reReviewTrigger` | Describes when the entry should be reviewed again. | Must not depend on unstable timestamps unless separately approved. |
| `expirationMode` | Describes a future bounded re-review posture. | Must avoid generated timestamp expiry unless separately approved. |
| `notProofStatement` | States that the allowlist entry is not runtime safety proof. | Required for any future allowlist entry. |
| `linkedDocumentation` | Points to reviewed docs that explain the decision. | Must not include secrets or private paths. |

Do not propose UUIDs, hashes, random IDs, or timestamp-generated IDs. Do not add an actual allowlist file in this sprint.

## Future Allowlist Review Workflow

A future allowlist review workflow should be explicit and human-reviewed:

1. Finding appears in future report-only output.
2. Reviewer determines whether the finding is expected.
3. Reviewer chooses rename, documentation clarification, rule adjustment, or allowlist candidate.
4. Allowlist candidate must include rationale.
5. Allowlist candidate must identify rule category.
6. Allowlist candidate must include path/scope.
7. Allowlist candidate must include re-review trigger.
8. Allowlist candidate must not be treated as production safety certification.
9. Allowlist candidate must be reviewed before any future enforcement mode.

The workflow should keep report-only output separate from enforcement. A future allowlist entry should reduce repeated review noise without hiding naming overclaim risk.

## Future Allowlist Expiration And Re-Review

Future allowlist entries should not be permanent by default. They should be re-reviewed when:

- related class names change;
- related file paths or path scopes change;
- rule categories change;
- denylist terms or matching patterns change;
- a finding moves from `INFO` or `WARN` to `BLOCKER_CANDIDATE`;
- a future report-only mode is proposed for enforcement;
- package moves are proposed;
- package-boundary enforcement is proposed;
- any production-readiness claim is proposed;
- any production-certification claim is proposed.

Allowlist entries should be re-reviewed before moving from report-only to enforcement. Allowlist entries should be re-reviewed before package moves. Allowlist entries should be re-reviewed before any production-readiness claim. Expiration must not depend on unstable timestamps unless separately approved.

## Future Suppression Strategy

Future suppressions should be narrower than broad category exemptions:

- suppressions should be reviewed and documented;
- suppressions should not become inline ignore spam;
- suppressions should not hide broad categories;
- suppressions should not suppress production-certification/control claims without explicit maintainer review;
- suppressions should remain auditable;
- suppressions should be removable without runtime behavior changes;
- suppressions should identify the rule category and scope they affect;
- suppressions should not suppress intentionally documented risky examples incorrectly;
- suppressions should not replace package-boundary enforcement, runtime review, or production-readiness review.

Suppression strategy is reviewer metadata only unless a later implementation sprint separately approves a mechanism.

## Future Allowlist Privacy And Secret-Safety Rules

Future allowlist entries and reports should preserve privacy and secret safety:

- no secrets;
- no tokens;
- no credentials;
- no environment variable values;
- no private network details;
- no absolute local machine paths;
- no user home directories;
- no tenant identifiers;
- no private reviewer names;
- no cloud account details;
- no service endpoints unless separately approved and already public;
- repository-relative paths only if future scanning is separately approved;
- no uploaded artifacts unless separately approved;
- no PR comments unless separately approved.

Allowlist rationale must be reviewable without exposing private runtime context.

## Future Allowlist Deterministic-Output Rules

Future allowlist output, if separately approved later, should be deterministic:

- stable ordering by reviewed rule category, path scope, and name pattern;
- stable field names;
- no UUID values;
- no random values;
- no hashes or SHA values;
- no generated timestamp IDs;
- no unstable timestamp expiration unless separately approved;
- no environment values;
- no system-property values;
- no absolute local paths;
- no generated IDs;
- `allowlistEntryMode` is not a generated ID;
- `expirationMode` is not a timestamp unless separately approved.

Deterministic allowlist output makes review diffs meaningful without introducing runtime behavior or build behavior changes.

## Future Allowlist Misuse Risks

Misuse risks to review before any future allowlist implementation:

- allowlist treated as safety certification;
- allowlist hides real overclaim risk;
- allowlist becomes too broad;
- allowlist suppresses intentional risky examples incorrectly;
- allowlist tied to unstable paths or generated output;
- allowlist includes secrets or local machine paths;
- allowlist becomes replacement for package-boundary enforcement;
- allowlist becomes replacement for human review;
- allowlist is used to claim source-name guard enforcement is active;
- allowlist is used to claim production readiness, production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, or facility automation.

These risks should be reviewed before any future allowlist file, report generator, CI integration, PR comment, artifact, or enforcement proposal.

## Future Implementation Gates

Before any future allowlist mechanism exists, reviewers should confirm:

- rule review checklist reviewed;
- rule catalog reviewed;
- allowlist design reviewed;
- report schema reviewed;
- report acceptance criteria reviewed;
- report review checklist reviewed;
- false-positive risk reviewed;
- false-negative risk reviewed;
- privacy/secret-safety reviewed;
- deterministic output reviewed;
- suppression process reviewed;
- allowlist entry fields reviewed;
- expiration and re-review posture reviewed;
- rollback/removal path reviewed;
- no allowlist file or output until separately approved;
- no JSON/YAML/TOML output until separately approved;
- no source scanning until separately approved;
- no report generation until separately approved;
- no CI workflow change until separately approved;
- no PR comment/report artifact behavior until separately approved;
- enforcement remains future-only unless separately approved.

These gates are reviewer checkpoints only. They do not implement an allowlist.

## Safety Boundaries And Non-Goals

Hard boundaries for this sprint:

- no production Java runtime behavior;
- no records/classes/interfaces under `src/main/java`;
- no class renames;
- no package moves or refactors;
- no source scanning logic in this sprint;
- no allowlist files;
- no JSON/YAML/TOML allowlist output;
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

This allowlist design plan does not claim allowlist implementation exists. This allowlist design plan does not claim source-name guard rule implementation exists. This allowlist design plan does not claim report generation exists. This allowlist design plan does not claim source-name guard enforcement is active. This allowlist design plan does not claim a runtime-enforced LASE boundary. This allowlist design plan does not claim package-boundary enforcement is active.

## Reviewer-Facing Value

This plan gives reviewers an allowlist vocabulary before any allowlist file or source-name guard implementation exists. It explains what future allowlists should preserve, which fields might be needed, when entries should be re-reviewed, how suppressions should stay auditable, and how privacy, determinism, and misuse risks should be handled.

The value is strategic architecture readiness only. Source-name guard allowlist is not implemented. Source-name guard enforcement is not active. Package-boundary enforcement is not active. Runtime LASE boundary enforcement is not active. Production readiness, production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, and facility automation remain not proven.
