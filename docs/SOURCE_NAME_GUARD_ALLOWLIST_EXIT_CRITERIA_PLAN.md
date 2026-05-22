# Source-Name Guard Allowlist Exit Criteria Plan

This document defines exit criteria for deciding when the source-name guard allowlist planning lane is complete enough to consider a separately approved implementation sprint later. It is exit criteria only, no allowlist implementation, and it does not add source scanning, allowlist files, JSON/YAML/TOML allowlist output, dry-run report generation, JSON output files, CI workflow changes, PR comments, artifacts, source-name guard enforcement, runtime naming enforcement, or package-boundary enforcement.

This is docs/test only. No source scanning is added in this sprint. No allowlist file is added. No allowlist files are added. No JSON/YAML/TOML output is added. No JSON/YAML/TOML allowlist output is added. No report generation is added. No dry-run report generation is added. No JSON output is generated. No JSON output files are generated. No CI workflow change is added. No PR comment or artifact behavior is added. No runtime naming guard is active. No source-name guard is implemented. No source-name guard rule implementation exists. No report file is written. No dry-run command is added. No allowlist implementation is added. No JSON/YAML/TOML allowlist file is added. No runtime naming enforcement is added. No source-name guard enforcement is active. No classes are renamed in this sprint. No package moves are made in this sprint. No ArchUnit or package-boundary tool is added. No Maven build files are changed. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

## Executive Summary

PR #234 added [`SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md), which defines future allowlist semantics before implementation. PR #235 added [`SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md), which defines reviewer criteria for future allowlist candidates. PR #236 added [`SOURCE_NAME_GUARD_ALLOWLIST_SAMPLE_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_SAMPLE_PLAN.md), which provides static documentation-only examples for future allowlist entries. PR #237 added [`SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md), which defines future allowlist lifecycle states and workflows.

This allowlist exit criteria plan is the closure layer for the allowlist planning lane. It defines the documentation completeness, review-readiness, privacy/secret-safety, determinism, allowlist-quality, misuse-risk, implementation-readiness, and non-exit criteria reviewers should satisfy before considering a separately approved implementation sprint later.

The core rules are:

Allowlist exit criteria do not certify production safety.

Allowlist exit criteria do not replace package-boundary enforcement.

Allowlist exit criteria do not replace human review.

## Current Status: Exit Criteria Only, No Allowlist Implementation

Current status:

- exit criteria only;
- exit criteria only, no allowlist implementation;
- documentation and documentation guard tests only;
- docs/test only;
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
- source-name guard allowlist lifecycle is not implemented;
- source-name guard allowlist exit criteria is not enforcement;
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
- no records/classes/interfaces/enums under `src/main/java`;
- no ArchUnit or package-boundary tool is added;
- no new dependency is added;
- no Maven build files are changed;
- no API fields are added;
- no routing, scoring, strategy, proxy, config, CI, Docker, release, signing, registry, governance, or production behavior changes are made.

This allowlist exit criteria plan does not claim allowlist implementation exists. This allowlist exit criteria plan does not claim source-name guard rule implementation exists. This allowlist exit criteria plan does not claim report generation exists. This allowlist exit criteria plan does not claim source-name guard enforcement is active. This allowlist exit criteria plan does not claim a runtime-enforced LASE boundary. This allowlist exit criteria plan does not claim package-boundary enforcement is active.

## Relationship To Source-Name Guard Allowlist Design Plan

The allowlist design plan defines future allowlist purpose, entry fields, review workflow, expiration and re-review, suppression strategy, privacy/secret-safety rules, deterministic-output rules, misuse risks, and implementation gates.

This exit criteria plan depends on [`SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md). Reviewers should use the design plan to understand the intended future semantics, then use this document to decide whether the design lane is complete enough to consider a separately approved implementation sprint later.

## Relationship To Source-Name Guard Allowlist Review Checklist

The allowlist review checklist defines how reviewers should evaluate future allowlist candidates before any allowlist file or source-name guard implementation exists.

This exit criteria plan depends on [`SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_ALLOWLIST_REVIEW_CHECKLIST.md). Reviewers should use that checklist to evaluate candidate quality, then use this document to decide whether reviewer expectations are complete, non-proving, privacy-safe, deterministic, and implementation-ready.

## Relationship To Source-Name Guard Allowlist Sample Plan

The allowlist sample plan provides static documentation-only examples for future allowlist entry shapes, documentation-clarification-preferred outcomes, rename-preferred outcomes, suppression-review-required outcomes, stale re-review outcomes, and invalid entries.

This exit criteria plan depends on [`SOURCE_NAME_GUARD_ALLOWLIST_SAMPLE_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_SAMPLE_PLAN.md). Reviewers should use the sample plan to rehearse concrete entries, then use this document to decide whether those examples make allowlist quality, stale-entry handling, and rejection conditions clear enough before implementation is discussed.

## Relationship To Source-Name Guard Allowlist Lifecycle Plan

The allowlist lifecycle plan defines future lifecycle states, creation workflow, re-review workflow, expiration workflow, retirement workflow, migration workflow, audit workflow, stale-entry risk handling, privacy/secret-safety requirements, and implementation gates.

This exit criteria plan depends on [`SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md). Reviewers should use the lifecycle plan to understand how entries would move through future states, then use this document to decide whether the planning lane has enough lifecycle, audit, stale-entry, rollback, and non-exit language to pause before a separately approved implementation sprint.

The future source-name guard rule review checklist is documented in [`SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md). The future source-name guard rule catalog plan is documented in [`SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md`](SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md). Both remain docs/test-only references and do not add allowlist files, source scanning, report generation, JSON/YAML/TOML output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

## Why Exit Criteria Come Before Implementation

Exit criteria come before implementation because allowlist planning can otherwise drift into vague permission to scan, suppress, or enforce names without enough reviewer agreement:

- reviewers need a clear definition of planning completeness before any allowlist file exists;
- allowlist quality criteria need to be visible before future entries can reduce repeated findings;
- privacy and secret-safety rules need to be explicit before any future output includes paths or names;
- deterministic-output rules need to exist before future reports or allowlist entries are generated;
- misuse risks need to be reviewed before suppressions become normal;
- non-exit conditions need to block an implementation proposal if docs are incomplete, vague, stale, too broad, or overclaiming;
- implementation-readiness gates need to keep allowlist files, source scanning, report generation, JSON/YAML/TOML output, CI integration, PR comments, artifacts, and enforcement separate.

This plan defines exit criteria only. It does not implement source scanning, allowlist files, JSON/YAML/TOML output, report generation, CI workflow changes, PR comment/report artifact behavior, or enforcement.

## Required Documentation Completeness Criteria

The allowlist planning lane should not be considered ready for a separately approved implementation sprint unless documentation completeness is clear:

- allowlist design plan exists;
- allowlist review checklist exists;
- allowlist sample plan exists;
- allowlist lifecycle plan exists;
- rule catalog exists;
- rule review checklist exists;
- report schema plan exists;
- report review checklist exists;
- report acceptance criteria exists;
- all docs clearly say planning only;
- all docs clearly say no enforcement;
- all docs clearly say no production-readiness claim.

Documentation completeness means reviewers can trace the allowlist lane back to report quality, rule categories, and non-proving source-name guard boundaries.

## Required Review-Readiness Criteria

The allowlist planning lane should be review-ready only when:

- reviewer can understand why allowlists may be needed;
- reviewer can evaluate an allowlist candidate;
- reviewer can evaluate rationale quality;
- reviewer can evaluate scope/path boundaries;
- reviewer can identify overbroad suppressions;
- reviewer can identify stale entries;
- reviewer can identify invalid entries;
- reviewer can identify when rename or documentation clarification is preferred;
- reviewer can confirm an allowlist does not certify production safety.

Review readiness is about human judgment. It does not replace human review, source scanning approval, package-boundary enforcement, runtime safety controls, or production-readiness review.

## Required Privacy And Secret-Safety Criteria

Future allowlist planning should preserve privacy and secret safety before any output exists:

- future allowlist entries must not include secrets;
- future allowlist entries must not include tokens;
- future allowlist entries must not include environment variable values;
- future allowlist entries must not include private network details;
- future allowlist entries must not include absolute local machine paths;
- future allowlist entries must avoid real reviewer names;
- future allowlist entries must avoid generated unstable IDs unless separately approved.

Privacy and secret-safety criteria are exit criteria for planning only. They do not add scanners, filters, validators, config, environment reads, system-property reads, artifact uploads, PR comments, or runtime behavior.

## Required Determinism Criteria

Future allowlist planning should define deterministic behavior before any implementation exists:

- future allowlist review should avoid unstable timestamps unless separately approved;
- future allowlist entries should avoid UUID/random/hash identifiers unless separately approved;
- future allowlist matching must be deterministic if implemented later;
- future allowlist report output must be deterministic if implemented later;
- future allowlist ordering must be stable if implemented later;
- future allowlist lifecycle states must be reviewable and stable.

Determinism criteria do not add MessageDigest, SHA, UUID, random, time, environment, system-property, generated-ID, or report-generation behavior in this sprint.

## Required Allowlist-Quality Criteria

Future allowlist candidates should not be considered acceptable unless each candidate has clear reviewer context:

- each candidate has a rule category;
- each candidate has a name or naming pattern;
- each candidate has a path/scope boundary;
- each candidate has a rationale;
- each candidate has reviewer action;
- each candidate has re-review trigger;
- each candidate has notProofStatement;
- each candidate avoids production-readiness claims;
- each candidate avoids certification claims;
- each candidate avoids replacing human review;
- each candidate avoids replacing package-boundary enforcement.

Allowlist quality criteria do not certify production safety. They make future candidate review more specific if a later implementation sprint is separately approved.

## Required Misuse-Risk Criteria

The allowlist planning lane should explicitly guard against misuse:

- allowlist must not become production safety certification;
- allowlist must not hide real overclaim risk;
- allowlist must not be too broad;
- allowlist must not become stale after rule changes;
- allowlist must not hide intentionally risky examples incorrectly;
- allowlist must not target generated/build output unless separately approved;
- allowlist must not become a substitute for package-boundary enforcement;
- allowlist must not become a substitute for runtime safety controls.

Misuse-risk criteria are non-proving review criteria. They do not imply that a clean future allowlist output proves production safety.

## Required Implementation-Readiness Gates

Before any future allowlist implementation is proposed, reviewers should confirm:

- design plan reviewed;
- review checklist reviewed;
- sample plan reviewed;
- lifecycle plan reviewed;
- exit criteria reviewed;
- rule catalog reviewed;
- rule review checklist reviewed;
- report schema reviewed;
- report review checklist reviewed;
- acceptance criteria reviewed;
- privacy/secret-safety reviewed;
- deterministic-output strategy reviewed;
- false-positive risk reviewed;
- false-negative risk reviewed;
- rollback/removal plan reviewed;
- CI impact reviewed;
- enforcement remains future-only unless separately approved.

These implementation-readiness gates are reviewer checkpoints only. They do not implement an allowlist, source scanner, report generator, JSON/YAML/TOML output, CI workflow, PR comment, artifact, runtime naming guard, source-name guard enforcement, or package-boundary enforcement.

## Explicit Non-Exit Conditions

The allowlist planning lane should not be treated as complete if any of these conditions remain:

- missing not-proven boundaries;
- vague allowlist rationale rules;
- no stale-entry handling;
- no privacy/secret-safety criteria;
- no deterministic-output criteria;
- no rollback/removal path;
- no false-positive review path;
- no false-negative review path;
- allowlist described as production safety certification;
- allowlist described as package-boundary enforcement;
- allowlist described as runtime enforcement;
- source-name guard implementation bundled with unrelated behavior changes.

Any non-exit condition should keep implementation out of scope until a later scoped sprint resolves it.

## Safety Boundaries And Non-Goals

Hard boundaries for this sprint:

- no production Java runtime behavior;
- no records/classes/interfaces/enums under `src/main/java`;
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

This allowlist exit criteria plan does not claim allowlist implementation exists. This allowlist exit criteria plan does not claim source-name guard rule implementation exists. This allowlist exit criteria plan does not claim report generation exists. This allowlist exit criteria plan does not claim source-name guard enforcement is active. This allowlist exit criteria plan does not claim a runtime-enforced LASE boundary. This allowlist exit criteria plan does not claim package-boundary enforcement is active.

## Reviewer-Facing Value

This plan gives reviewers a stopping rule for the allowlist planning lane before any allowlist file or source-name guard implementation exists. It explains what documentation, review, privacy, determinism, quality, misuse-risk, implementation-gate, and non-exit criteria should be satisfied before a separate implementation sprint is even considered.

The value is strategic architecture readiness only. Source-name guard allowlist is not implemented. Source-name guard enforcement is not active. Package-boundary enforcement is not active. Runtime LASE boundary enforcement is not active. Production readiness, production certification, live-cloud validation, real-tenant validation, GPU orchestration, power/grid control, carbon-aware routing implementation, and facility automation remain not proven.
