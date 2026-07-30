# Source-Name Guard Allowlist Sample Plan

This document provides static examples for future source-name guard allowlist entries. It is a sample plan only, no allowlist implementation, and it does not add source scanning, allowlist files, JSON/YAML/TOML allowlist output, dry-run report generation, JSON output files, CI workflow changes, PR comments, artifacts, source-name guard enforcement, runtime naming enforcement, or package-boundary enforcement.

This is docs/test only. The examples in this document are static documentation examples, not generated output. The examples are static documentation examples. These examples are not machine-generated. They are documentation-only mock examples. No source scanning is added in this sprint. No allowlist file is added. No allowlist files are added. No JSON/YAML/TOML output is added. No JSON/YAML/TOML allowlist output is added. No report generation is added. No dry-run report generation is added. No JSON output is generated. No JSON output files are generated. No CI workflow change is added. No PR comment or artifact behavior is added. No runtime naming guard is active. No source-name guard is implemented. No source-name guard rule implementation exists. No report file is written. No dry-run command is added. No runtime naming enforcement is added. No source-name guard enforcement is active. No classes are renamed in this sprint. No package moves are made in this sprint. No ArchUnit or package-boundary tool is added. No Maven build files are changed. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

## Executive Summary

PR #234 added [`SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md), which defines future allowlist semantics before implementation. PR #235 added [`SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md), which defines reviewer criteria for future allowlist candidates.

This allowlist sample plan is the next preparation layer. It gives reviewers static documentation-only examples of future allowlist entry shapes, rationale quality, scope/path boundaries, re-review triggers, suppression posture, stale-entry handling, invalid entries, and misuse risks before any allowlist file, source scanner, dry-run report generator, JSON/YAML/TOML output, CI integration, PR comment, artifact upload, or enforcement exists.

The core rules are:

Allowlist examples do not certify production safety.

Allowlist examples do not replace package-boundary enforcement.

Allowlist examples do not replace human review.

## Current Status: Sample Plan Only, No Allowlist Implementation

Current status:

- sample plan only;
- sample plan only, no allowlist implementation;
- documentation and documentation guard tests only;
- examples are static documentation examples;
- examples are not generated output;
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
- source-name guard allowlist review checklist is not enforcement;
- source-name guard allowlist sample is not generated output;
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

This allowlist sample plan does not claim allowlist implementation exists. This allowlist sample plan does not claim source-name guard rule implementation exists. This allowlist sample plan does not claim report generation exists. This allowlist sample plan does not claim source-name guard enforcement is active. This allowlist sample plan does not claim a runtime-enforced LASE boundary. This allowlist sample plan does not claim package-boundary enforcement is active.

## Relationship To Source-Name Guard Allowlist Design Plan

The allowlist design plan defines future allowlist purpose, entry fields, review workflow, expiration and re-review, suppression strategy, privacy/secret-safety rules, deterministic-output rules, misuse risks, and implementation gates.

This sample plan depends on [`SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md). Reviewers should use the design plan to understand future allowlist semantics, then use this document to see static documentation-only examples of acceptable, questionable, stale, suppression-sensitive, and invalid future allowlist candidates.

## Relationship To Source-Name Guard Allowlist Review Checklist

The allowlist review checklist defines how reviewers should evaluate future allowlist candidates before any allowlist file or source-name guard implementation exists.

This sample plan depends on [`SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md). Reviewers should compare each static sample to the checklist's candidate review, rationale review, scope/path review, rule-category review, re-review trigger, suppression review, privacy/secret-safety, misuse-risk, and approval-gate questions.

The future source-name guard rule review checklist is documented in [`SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md). The future source-name guard rule catalog plan is documented in [`SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md`](SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md). Both remain docs/test-only references and do not add allowlist files, source scanning, report generation, JSON/YAML/TOML output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

The future source-name guard allowlist lifecycle plan is documented in [`SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md). That lifecycle plan remains docs/test-only and defines future allowlist entry states, creation, re-review, expiration, retirement, migration, audit, stale-entry risk handling, privacy/secret-safety, and implementation gates before any allowlist file, source scanning, report generation, JSON/YAML/TOML output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

The future source-name guard allowlist exit criteria plan is documented in [`SOURCE_NAME_GUARD_ALLOWLIST_EXIT_CRITERIA_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_EXIT_CRITERIA_PLAN.md). That exit criteria plan remains docs/test-only and defines when the allowlist planning lane is complete enough to consider a separately approved implementation sprint later before any allowlist file, source scanning, report generation, JSON/YAML/TOML output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

## Why Sample Entries Come Before Implementation

Sample entries come before implementation because future allowlists can become noisy or misleading if reviewers do not first agree on what a good entry looks like:

- reviewers need concrete examples of narrow path/scope boundaries before any file format exists;
- rationale quality should be visible before allowlist entries can suppress future findings;
- rename-preferred and documentation-clarification-preferred outcomes should stay available before an allowlist becomes the default answer;
- stale entries and invalid entries need review language before enforcement is discussed;
- suppression posture must remain auditable and separate from inline ignore spam;
- static samples help reviewers spot misuse risks without creating generated output;
- examples must not imply that allowlist entries certify production safety, replace package-boundary enforcement, or replace human review.

This plan defines static examples only. It does not implement source scanning, allowlist files, JSON/YAML/TOML output, dry-run report generation, CI workflow changes, PR comment/report artifact behavior, or enforcement.

## Sample Allowlist Entry Reading Guide

The examples below are static documentation examples, not generated output. They use fenced markdown blocks with YAML-like pseudocode to make field intent easy to read. They are not JSON/YAML/TOML files, not allowlist artifacts, not scanner output, not report generator output, not CI output, and not PR comment examples from a bot.

Reviewers should read each example as a future discussion aid:

- `allowlistEntryMode` describes the example posture without creating a generated ID;
- `ruleCategory` points to a reviewed future rule category;
- `namePattern` is a narrow example name or pattern;
- `pathScope` should be repository-relative or documentation-only in examples;
- `rationale` explains why the name may be acceptable;
- `reviewerAction` preserves the next review step;
- `reReviewTrigger` explains when the candidate should be revisited;
- `notProofStatement` prevents safety overclaims.

The sample entries are review triggers and reviewer training aids only. They do not prove runtime safety.

## Sample Narrow Allowlist Entry

This sample shows a narrow entry shape for a reviewer-metadata ambiguity example. It is not an allowlist file and it is not generated output.

```text
allowlistEntryMode: documentation-example
ruleCategory: reviewer-metadata-ambiguity
namePattern: ExampleReviewerMetadata
pathScope: docs-example-only
rationale: name is reviewer metadata in this example and does not imply runtime authority
reviewerAction: keep under review
reReviewTrigger: re-review if name, rule category, or package boundary changes
notProofStatement: allowlist entry does not certify production safety
```

This example is acceptable only as documentation guidance. It does not create a future allowlist entry, and it does not replace human review.

## Sample Documentation-Clarification-Preferred Outcome

This sample shows a candidate that should be resolved by documentation clarification rather than allowlist.

```text
allowlistEntryMode: documentation-example
ruleCategory: reviewer-metadata-ambiguity
namePattern: ExampleReviewerSummary
pathScope: docs-example-only
outcome: documentation-clarification-preferred
reason: name is acceptable but reviewer meaning needs clarification
reviewerAction: clarify documentation in a separate scoped PR
notProofStatement: clarification does not prove runtime safety
```

This outcome reminds reviewers that a future allowlist is not the only possible response to naming ambiguity.

## Sample Rename-Preferred Outcome

This sample shows a candidate that should be resolved by a future rename rather than allowlist.

```text
allowlistEntryMode: documentation-example
ruleCategory: production-certification-overclaim
namePattern: ExampleProductionCertifiedRouter
pathScope: docs-example-only
outcome: rename-preferred
reason: name implies proof/control/certification too strongly
reviewerAction: propose rename in separate scoped PR
notProofStatement: rename recommendation is not proof of unsafe runtime behavior
```

The example name is a name to avoid, not an implemented class. Rename preference is a review outcome, not proof of unsafe runtime behavior.

## Sample Suppression-Review-Required Outcome

This sample shows a candidate that requires explicit suppression review.

```text
allowlistEntryMode: documentation-example
ruleCategory: live-grid-control-claim
namePattern: ExampleLiveGridControlTerm
pathScope: docs-example-only
outcome: suppression-review-required
reason: suppression would hide a high-risk category
reviewerAction: maintainer review required before any future suppression
notProofStatement: suppression does not certify production safety
```

Suppression review must remain explicit, narrow, auditable, and separate from runtime behavior changes.

## Sample Stale Allowlist Re-Review Outcome

This sample shows a candidate that must be re-reviewed because a rule category or path scope changed.

```text
allowlistEntryMode: documentation-example
ruleCategory: reviewer-metadata-ambiguity
namePattern: ExampleReviewerMetadata
pathScope: docs-example-only
outcome: stale-allowlist-re-review
reason: rule category or path scope changed since the rationale was reviewed
reviewerAction: re-check rationale before any future enforcement
notProofStatement: stale allowlist is not production safety proof
```

Stale entries should be re-reviewed before any future enforcement mode, package move, production-readiness claim, or source scanner use.

## Sample Invalid Allowlist Entry

This sample shows an invalid entry that reviewers should reject.

```text
allowlistEntryMode: documentation-example
ruleCategory: production-certification-overclaim
namePattern: ExampleProductionCertifiedRouter
pathScope: entire-repository-and-generated-output
outcome: invalid-allowlist-entry
problems: missing rationale; overbroad path scope; claims production safety; references generated output or local machine path
reviewerAction: reject candidate
notProofStatement: invalid entry must not suppress future findings
```

Invalid candidate markers:

- missing rationale;
- overbroad path scope;
- claims production safety;
- references generated output or local machine path;
- reviewerAction: reject candidate;
- notProofStatement: invalid entry must not suppress future findings.

Invalid entries should not be rewritten into broad suppressions. They should be rejected or narrowed in a later scoped review.

## Privacy And Secret-Safety Constraints

The static samples must not include:

- real timestamps;
- UUIDs;
- hashes;
- absolute local machine paths;
- secrets;
- tokens;
- environment variable values;
- env var values;
- private network details;
- real reviewer names;
- personal data;
- tenant identifiers;
- cloud account identifiers;
- user home directories;
- private service endpoints.

Static examples should not invent reviewer names, initials, personal data, timestamps, UUIDs, hashes, or absolute local paths. They should not include external URLs other than existing repository documentation links when needed.

## Misuse Risks And Reviewer Cautions

Reviewers should treat these as misuse risks:

- allowlist examples used as certification;
- allowlist examples used to hide unsafe overclaims;
- allowlist examples treated as source-name guard enforcement;
- allowlist examples treated as package-boundary enforcement;
- allowlist examples treated as replacement for human review;
- allowlist examples treated as production readiness or production certification;
- allowlist entries made too broad;
- stale allowlist rationale kept after rule-category changes;
- suppressions hiding high-risk categories without explicit review;
- examples tied to generated output, absolute local paths, secrets, tokens, environment values, or private network details.

Allowlist examples do not certify production safety. Allowlist examples do not replace package-boundary enforcement. Allowlist examples do not replace human review.

## Safety Boundaries And Non-Goals

Hard boundaries for this sprint:

- no production Java runtime behavior;
- no records/classes/interfaces under `src/main/java`;
- no class renames;
- no package moves or refactors;
- no source scanning logic in this sprint;
- no allowlist files;
- no JSON/YAML/TOML allowlist output;
- no JSON/YAML/TOML output;
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

This allowlist sample plan does not claim allowlist implementation exists. This allowlist sample plan does not claim source-name guard rule implementation exists. This allowlist sample plan does not claim report generation exists. This allowlist sample plan does not claim source-name guard enforcement is active. This allowlist sample plan does not claim a runtime-enforced LASE boundary. This allowlist sample plan does not claim package-boundary enforcement is active.

## Reviewer-Facing Value

This plan gives reviewers concrete static examples for future allowlist entry shape and review outcomes before any allowlist file or source-name guard implementation exists. It helps reviewers distinguish narrow allowlist candidates from documentation clarification, rename preference, suppression review, stale re-review, and invalid entries.

The value is strategic architecture readiness only. Source-name guard allowlist is not implemented. Source-name guard enforcement is not active. Package-boundary enforcement is not active. Runtime LASE boundary enforcement is not active. Production readiness, production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, and facility automation remain not proven.
