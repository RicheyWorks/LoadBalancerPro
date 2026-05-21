# Source-Name Guard Report Sample Plan

This document gives reviewers static documentation examples of what a future source-name guard dry-run report could look like. It is a sample plan only, no report generation, and it does not add source scanning, JSON output files, CI workflow changes, PR comments, artifacts, source-name guard enforcement, runtime naming enforcement, or package-boundary enforcement.

This is docs/test only. The examples in this document are static documentation examples, not generated output. No source scanning is added in this sprint. No JSON output files are generated. No CI workflow change is added. No PR comment or artifact behavior is added. No runtime naming guard is active. No source-name guard is implemented. No dry-run report generation is added. No report file is written. No dry-run command is added. No runtime naming enforcement is added. No source-name guard enforcement is active. No classes are renamed in this sprint. No package moves are made in this sprint. No ArchUnit or package-boundary tool is added. No Maven build files are changed. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

## Executive Summary

PR #227 added [`SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md), which describes future report-only dry-run modes before enforcement. PR #228 added [`SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md), which defines future report field vocabulary before any generator exists. PR #229 added [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md), which defines how reviewers should evaluate a future dry-run report before implementation.

This sample plan is the next preparation layer. It shows static, documentation-only examples of how future report content might be read by reviewers before any source-name guard report generator, JSON output, CI integration, PR comment, artifact upload, or enforcement exists.

The core rule is:

Sample findings are review triggers, not proof of unsafe runtime behavior.

## Current Status: Sample Plan Only, No Report Generation

Current status:

- sample plan only;
- documentation and documentation guard tests only;
- examples are static documentation examples;
- examples are not generated output;
- no report generation;
- no JSON output files;
- no JSON output;
- no source scanning;
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

This sample plan does not claim report generation exists. This sample plan does not claim JSON output exists. This sample plan does not claim source-name guard enforcement is active. This sample plan does not claim a runtime-enforced LASE boundary. This sample plan does not claim package-boundary enforcement is active.

## Relationship To Source-Name Guard Report Schema Plan

The report schema plan defines the future report vocabulary: top-level fields, finding fields, summary fields, severity model, reviewer guidance, not-proven boundaries, deterministic output rules, privacy/secret-safety rules, and versioning.

This sample plan depends on [`SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md). The examples below use the schema vocabulary as documentation-only pseudocode. They do not create JSON files, report artifacts, report commands, or generated output.

## Relationship To Source-Name Guard Report Review Checklist

The report review checklist defines how reviewers should inspect future dry-run reports before implementation.

This sample plan depends on [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md). Reviewers can use these static examples to rehearse report interpretation before any report generator, JSON output, CI integration, PR comment, artifact upload, or enforcement exists.

## Why Sample Reports Come Before Implementation

Sample reports come before implementation because report examples can expose ambiguity before any code exists:

- reviewers can see how severity, reason, reviewer action, and not-proof statements should read;
- report language can be checked for overclaims before it appears in generated output;
- false-positive handling can be discussed before source scanning exists;
- privacy and deterministic-output expectations can be shown without collecting files;
- future implementation can avoid examples that look like production proof, certification, or enforcement;
- future report designers can keep examples static, narrow, and reviewer-readable.

These examples are not machine-generated. They are documentation-only mock examples.

## Sample Report Reading Guide

Use these examples as reading aids:

1. Confirm the sample is marked as report-only or static example content.
2. Confirm findings include a severity, category, name, reason, reviewer action, and not-proof statement.
3. Confirm clean samples still state that a clean report is not proof of production safety.
4. Confirm `BLOCKER_CANDIDATE` means human review is required before any future blocking enforcement.
5. Confirm allowlisted examples remain reviewer metadata, not safety certification.
6. Confirm false-positive notes avoid timestamps, reviewer names, personal data, absolute paths, secrets, tokens, environment variable values, and private network details.

## Sample Clean Report

The following block is a static documentation example, not generated output:

```yaml
schemaVersion: example-v1
sourceNameGuardMode: report-only example
scannedScopeSummary: example scope only
findings: none
summary:
  totalFindings: 0
  reportOnlyMode: true
reviewerGuidance:
  nextReviewStep: no action required except normal review
notProofStatement: clean sample is not proof of production safety
```

Reviewer interpretation:

- a clean sample means no example findings are shown;
- a clean report is not proof of production safety;
- a clean report is not production readiness, production certification, source-name guard enforcement, package-boundary enforcement, or runtime LASE boundary enforcement.

## Sample INFO Finding Report

The following block is a static documentation example, not generated output:

```yaml
schemaVersion: example-v1
sourceNameGuardMode: report-only example
findings:
  - severity: INFO
    category: naming-clarity
    name: ExampleReviewerSummary
    reason: review suggested but likely safe
    reviewerAction: confirm naming remains reviewer metadata only
    notProofStatement: finding is a review trigger only
summary:
  totalFindings: 1
  infoCount: 1
```

Reviewer interpretation:

- `INFO` means review suggested, likely safe;
- the reviewer should confirm the name remains reviewer metadata only;
- the finding is a review trigger only, not proof of unsafe runtime behavior.

## Sample WARN Finding Report

The following block is a static documentation example, not generated output:

```yaml
schemaVersion: example-v1
sourceNameGuardMode: report-only example
findings:
  - severity: WARN
    category: possible-overclaim
    name: ExampleReplayValidator
    reason: name could imply stronger proof than intended
    reviewerAction: consider rename or documentation clarification
    notProofStatement: not proof of unsafe runtime behavior
summary:
  totalFindings: 1
  warnCount: 1
```

Reviewer interpretation:

- `WARN` means naming may imply unsafe authority or overclaim;
- the reviewer may request later rename work or documentation clarification;
- the finding does not prove replay proof, scoring proof, correctness validation, production readiness, or unsafe runtime behavior.

## Sample BLOCKER_CANDIDATE Finding Report

The following block is a static documentation example, not generated output:

```yaml
schemaVersion: example-v1
sourceNameGuardMode: report-only example
findings:
  - severity: BLOCKER_CANDIDATE
    category: production-authority-overclaim
    name: ExampleProductionCertifiedRouter
    reason: name appears to imply production certification or production routing authority
    reviewerAction: human review required before any future enforcement
    notProofStatement: still not automatic build failure in report-only mode and not proof of unsafe runtime behavior
summary:
  totalFindings: 1
  blockerCandidateCount: 1
```

Reviewer interpretation:

- `BLOCKER_CANDIDATE` is not automatic build failure in report-only mode;
- `BLOCKER_CANDIDATE` is not proof of unsafe runtime behavior;
- human review is required before any future blocking enforcement;
- the example name is a name to avoid, not an implemented class.

## Sample Allowlisted Finding Report

The following block is a static documentation example, not generated output:

```yaml
schemaVersion: example-v1
sourceNameGuardMode: report-only example
findings:
  - severity: INFO
    category: reviewed-example
    name: ExampleReviewerSummary
    allowlistStatus: reviewed-example-only
    suppressionReviewRequired: true
    reviewerAction: confirm allowlist rationale remains valid
    notProofStatement: allowlist does not certify production safety
summary:
  totalFindings: 1
  allowlistedCount: 1
```

Reviewer interpretation:

- allowlist status is reviewer metadata only;
- allowlist does not certify production safety;
- suppression review remains required if future implementation proposes to hide the finding.

## Sample False-Positive Review Note

The following block is a static documentation example, not generated output:

```yaml
schemaVersion: example-v1
sourceNameGuardMode: report-only example
findingReview:
  name: ExampleProductionCertifiedRouter
  falsePositiveNotes: documented risky example used to explain names to avoid
  reviewDisposition: downgrade or dismiss as documentation-only example
  reviewerAction: keep example clearly marked as not an implemented class
  notProofStatement: false-positive disposition is not proof of runtime safety
```

Reviewer interpretation:

- false-positive notes should explain why the finding is safe or intentionally documented;
- do not invent reviewer names, initials, personal data, timestamps, UUIDs, hashes, or absolute local paths;
- false-positive disposition should improve future guard rules, not create production safety claims.

## Sample Reviewer Decision Outcomes

Possible reviewer decision outcomes for future reports:

- accept as safe for report-only context;
- request rename in a later scoped PR;
- request allowlist entry with rationale;
- request suppression review;
- request guard rule change before implementation;
- reject guard proposal as too broad or noisy;
- defer until package-boundary enforcement exists.

These outcomes are review decisions only. They do not prove source-name guard enforcement, runtime naming enforcement, package-boundary enforcement, runtime LASE boundary enforcement, production readiness, or production certification.

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

The static samples must not include:

- real timestamps;
- UUIDs;
- hashes;
- absolute local machine paths;
- secrets;
- tokens;
- environment variable values;
- private network details;
- real reviewer names;
- personal data.

This sample plan does not claim report generation exists. This sample plan does not claim JSON output exists. This sample plan does not claim source-name guard enforcement is active. This sample plan does not claim a runtime-enforced LASE boundary. This sample plan does not claim package-boundary enforcement is active.

## Reviewer-Facing Value

Reviewers can use this plan to understand how future source-name guard dry-run report examples should be read before any report generator exists. The plan makes clean reports, `INFO`, `WARN`, `BLOCKER_CANDIDATE`, allowlisted findings, false-positive notes, reviewer decisions, and non-proof statements concrete without adding source scanning, JSON output files, report generation, CI integration, PR comments, artifact upload, or enforcement.

The value is reviewer interpretation, not generated output. Source-name guard report sample is not generated output, source-name guard report review checklist is not enforcement, source-name guard report schema not implemented yet, source-name guard dry run not implemented yet, source-name guard not implemented yet, LASE naming guard is not runtime-enforced yet, and LASE package boundary is not enforced yet.
