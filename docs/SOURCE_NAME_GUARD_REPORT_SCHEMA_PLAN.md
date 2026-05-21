# Source-Name Guard Report Schema Plan

This document defines a future source-name guard dry-run report schema plan. It is a schema plan only, no report generation, and it does not add source scanning, JSON output, CI workflow changes, PR comments, artifacts, source-name guard enforcement, runtime naming enforcement, or package-boundary enforcement.

This is docs/test only. No source scanning is added in this sprint. No JSON output is generated. No CI workflow change is added. No PR comment or artifact behavior is added. No runtime naming guard is active. No source-name guard is implemented. No dry-run report generation is added. No report file is written. No dry-run command is added. No runtime naming enforcement is added. No source-name guard enforcement is active. No classes are renamed in this sprint. No package moves are made in this sprint. No ArchUnit or package-boundary tool is added. No Maven build files are changed. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

## Executive Summary

PR #225 added [`LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md`](LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md), which scopes a future narrow source-name guard. PR #226 added [`SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md), which gives reviewers implementation-readiness questions. PR #227 added [`SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md), which describes future report-only dry-run modes before enforcement.

This report schema plan is the next preparation layer. It defines the shape a future dry-run report should use before any report generator exists.

The core rule is:

Future report findings are review triggers, not proof of unsafe runtime behavior.

## Current Status: Schema Plan Only, No Report Generation

Current status:

- schema plan only;
- documentation and documentation guard tests only;
- no report generation;
- no JSON output;
- no source scanning;
- no CI workflow change is added;
- no PR comment or artifact behavior is added;
- no dry-run command is added;
- no runtime naming guard is active;
- no source-name guard is implemented;
- source-name guard not implemented yet;
- source-name guard dry run not implemented yet;
- source-name guard report schema not implemented yet;
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

This report schema plan does not claim report generation exists. This report schema plan does not claim source-name guard enforcement is active. This report schema plan does not claim a runtime-enforced LASE boundary. This report schema plan does not claim package-boundary enforcement is active.

## Relationship To Source-Name Guard Dry-Run Design Plan

The dry-run design plan defines future report-only modes, review workflow, severity posture, CI gates, and rollback concepts. This schema plan narrows the report surface those future modes could produce.

A future dry-run implementation should read [`SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md) before it adds source scanning, report output, CI report-only integration, PR comments, artifacts, or enforcement behavior.

## Relationship To Source-Name Guard Review Checklist

The review checklist defines the questions reviewers should answer before any future guard implementation. This schema plan gives those reviewers a stable field vocabulary to inspect before report generation exists.

A future implementation proposal should complete [`SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md) and confirm the proposed report schema remains narrow, deterministic, reviewable, and non-proving.

The future report review checklist is documented in [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md). That checklist remains docs/test-only and does not add source scanning, dry-run report generation, JSON output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

The future report sample plan is documented in [`SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md). That sample plan remains docs/test-only and provides static documentation examples only; it does not add source scanning, dry-run report generation, JSON output files, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

## Why Report Schema Comes Before Implementation

Report schema comes before implementation because a source-name dry run can become noisy or misleading if output structure is improvised after scanning begins:

- reviewers need a stable vocabulary before generated output exists;
- field names should avoid unstable timestamps, UUIDs, random values, hashes, and local machine paths;
- report-only output should be easy to diff across runs;
- severity must not be confused with automatic build failure;
- findings must not be mistaken for proof of unsafe runtime behavior;
- privacy and secret-safety rules must be clear before any PR comment or artifact concept is implemented;
- future CI integration should not invent new fields without review.

This plan defines a future report shape only. It does not generate JSON, files, artifacts, PR comments, or CI output.

## Proposed Future Report Top-Level Fields

Future top-level report fields:

| Field | Purpose | Safety note |
| --- | --- | --- |
| `schemaVersion` | Identifies the future report schema revision. | Use an explicit version string reviewed with schema changes. |
| `generatedAtMode` | Describes how generation timing is represented without adding a real timestamp concept. | Use mode text instead of real timestamps unless separately approved. |
| `repositoryContext` | Identifies the reviewed repository/ref context in a bounded way. | Avoid absolute local machine paths, secrets, tokens, and private network details. |
| `scannedScopeSummary` | Summarizes the approved future scan scope. | Keep scope narrow and deterministic. |
| `sourceNameGuardMode` | Names the future mode such as local docs-only, local source-name, CI report-only, or PR report concept. | Must not imply enforcement unless a separate enforcement sprint approves it. |
| `findings` | Contains future finding records. | Findings are review triggers, not runtime safety proof. |
| `summary` | Contains counts and scope notes. | Counts are report metadata only. |
| `notProvenBoundaries` | Repeats explicit non-proof boundaries. | Must remain visible in report-only output. |
| `reviewerGuidance` | Gives next review steps and references. | Must not direct production activation. |

Important timestamp boundary:

Use `generatedAtMode` instead of a real timestamp concept, because future report generation must avoid unstable timestamps unless explicitly approved.

This sprint does not add real JSON generation, report files, or report commands.

## Proposed Future Finding Fields

Future finding fields:

| Field | Purpose | Safety note |
| --- | --- | --- |
| `findingIdMode` | Describes how a finding can be identified without UUIDs, random values, or hashes. | Future IDs must avoid UUID/random/hash unless separately approved. |
| `severity` | Uses the future severity model. | Severity is a review signal only in dry-run mode. |
| `category` | Groups the unsafe implication theme. | Categories should be narrow and documented. |
| `name` | Names the class/file/name candidate being reviewed. | Avoid including secret-derived or environment-derived names. |
| `path` | Gives a bounded repository-relative path if future scanning is approved. | Must not include absolute local machine paths. |
| `reason` | Explains the naming concern in plain language. | Must not claim runtime behavior is unsafe by name alone. |
| `reviewerAction` | Suggests a reviewer next step. | Must not imply automatic production gating. |
| `allowlistStatus` | States whether the name is known, reviewed, suppressed, or unresolved. | Allowlist status is reviewer metadata only. |
| `falsePositiveNotes` | Captures why a finding may be safe or intentionally documented. | False positives should improve guard design. |
| `notProofStatement` | States that the finding is not proof of unsafe runtime behavior. | Required for non-proving semantics. |

Important ID boundary:

Use `findingIdMode` instead of generated UUIDs, hashes, random values, or time-derived IDs. Future IDs must avoid UUID/random/hash unless separately approved.

## Proposed Future Summary Fields

Future summary fields:

| Field | Purpose | Boundary |
| --- | --- | --- |
| `totalNamesConsidered` | Count of approved names considered by a future dry run. | Count only within approved scope. |
| `totalFindings` | Count of findings emitted by a future dry run. | Not a safety score. |
| `infoCount` | Count of `INFO` findings. | Review metadata only. |
| `warnCount` | Count of `WARN` findings. | Review metadata only. |
| `blockerCandidateCount` | Count of `BLOCKER_CANDIDATE` findings. | Not automatic failure in dry-run mode. |
| `allowlistedCount` | Count of allowlisted findings or names. | Requires reviewed allowlist reasons. |
| `suppressedCount` | Count of reviewed suppressions. | Suppressions must stay reviewable. |
| `reviewedScopeDescription` | Human-readable scope description. | Avoid broad or hidden scan scope. |
| `reportOnlyMode` | Boolean-like future field indicating report-only behavior. | Must remain non-blocking unless separately approved. |

Summary fields must not be used to claim production readiness, runtime safety, package-boundary enforcement, or source-name guard enforcement.

## Proposed Future Severity Model

Future severity model:

| Severity | Meaning | Required reviewer posture |
| --- | --- | --- |
| `INFO` | Review suggested, likely safe. | Confirm context and keep as informational unless a pattern repeats. |
| `WARN` | Naming may imply unsafe authority or overclaim. | Review scope, owner, category, and safer wording before accepting. |
| `BLOCKER_CANDIDATE` | Naming appears to imply production certification/control/proof and requires human review before any blocking enforcement. | Treat as a candidate for blocking only after human review and separate policy approval. |

Important severity boundary:

`BLOCKER_CANDIDATE` is not an automatic build failure in dry-run mode and is not proof of unsafe runtime behavior.

## Proposed Future Reviewer Guidance Fields

Future reviewer guidance fields:

| Field | Purpose | Boundary |
| --- | --- | --- |
| `nextReviewStep` | Describes the next human review action. | Must not direct automatic enforcement. |
| `suggestedOwner` | Identifies a future owner category, not a personal secret or credential. | Avoid private identity or environment-derived content. |
| `documentationReferences` | Links to reviewer docs such as checklist, feasibility, dry-run design, and schema plan. | References must stay stable and repository-relative. |
| `suppressionReviewRequired` | Signals whether a reviewed suppression process is needed. | Suppression is not proof of runtime safety. |
| `implementationGateRequired` | Signals whether a separate approved implementation sprint is required. | Must stay true for enforcement, scanning, CI output, or PR artifacts. |

Reviewer guidance should make the report useful without granting routing authority, source-name guard enforcement, or production readiness claims.

## Proposed Future Not-Proven Boundary Fields

Future reports should include `notProvenBoundaries` with explicit statements such as:

- not production-ready;
- not production-certified;
- not live-cloud validated;
- not real-tenant validated;
- not GPU orchestration;
- not power/grid control;
- not carbon-aware routing implementation;
- not facility automation;
- ExternalSignalPort not implemented;
- WorkloadProfile signal metadata not implemented;
- LASE boundary not runtime-enforced yet;
- LASE package boundary not enforced yet;
- ArchUnit/package-boundary tooling not added yet;
- LASE naming guard not runtime-enforced yet;
- LASE naming inventory is not enforcement;
- Source-name guard not implemented yet;
- Source-name guard checklist is not enforcement;
- Source-name guard dry run not implemented yet;
- Source-name guard report schema not implemented yet.

These boundaries keep report-only output from becoming proof language.

## Proposed Future Deterministic Output Rules

Future deterministic output rules:

- field ordering should be stable;
- finding ordering should be stable and reviewer-explainable;
- counts should be derived only from the approved report scope;
- output should not depend on filesystem traversal order;
- output should not depend on timestamps, clocks, time zones, environment variables, system properties, random values, UUIDs, hashes, or network state;
- `generatedAtMode` should describe timing posture without embedding a real timestamp unless separately approved;
- `findingIdMode` should describe deterministic identity posture without UUIDs, random values, or hashes unless separately approved;
- repeated runs on the same reviewed tree and same approved scope should produce equivalent report content.

Determinism is a reviewability requirement, not proof of runtime safety.

## Proposed Future Privacy And Secret-Safety Rules

Future privacy and secret-safety rules:

- report must not include secrets;
- report must not include environment variable values;
- report must not include absolute local machine paths;
- report must not include tokens, credentials, or private network details;
- report must not upload artifacts unless separately approved;
- report must not post PR comments unless separately approved;
- report must not include user home directories, workstation-specific paths, API keys, service endpoints, cloud account details, or private tenant details;
- report must not infer hidden ownership or credentials from file names;
- report must keep all paths repository-relative if future scanning is separately approved.

Secret safety must be designed before any output channel exists.

## Proposed Future Versioning Strategy

Future versioning strategy:

- start with `schemaVersion` such as `source-name-guard-report-schema/v1` if a later sprint implements output;
- require reviewer approval before adding, removing, or reinterpreting fields;
- keep old fields stable during report-only trials unless a separate schema migration is approved;
- record schema changes in docs before enforcement depends on them;
- avoid silent meaning changes for severity, category, allowlist status, or not-proven boundaries;
- never use schema versioning to claim runtime enforcement, package-boundary enforcement, or production readiness.

Versioning should keep future reports reviewable and diffable.

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

This report schema plan does not claim report generation exists. This report schema plan does not claim source-name guard enforcement is active. This report schema plan does not claim a runtime-enforced LASE boundary. This report schema plan does not claim package-boundary enforcement is active.

## Reviewer-Facing Value

Reviewers can use this plan to inspect the intended shape of a future source-name guard dry-run report before any generator exists. The plan makes top-level fields, finding fields, summary fields, severity, reviewer guidance, not-proven boundaries, deterministic output rules, privacy rules, and versioning visible before implementation.

The value is schema clarity, not implementation. Source-name guard report schema not implemented yet, source-name guard dry run not implemented yet, source-name guard not implemented yet, source-name guard checklist is not enforcement, LASE naming guard is not runtime-enforced yet, and LASE package boundary is not enforced yet.
