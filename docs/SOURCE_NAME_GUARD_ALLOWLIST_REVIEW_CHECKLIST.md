# Source-Name Guard Allowlist Review Checklist

This document gives reviewers a checklist for evaluating future source-name guard allowlist candidates. It is an allowlist review checklist only, no implementation, and it does not add source scanning, allowlist files, JSON/YAML/TOML allowlist output, dry-run report generation, JSON output files, CI workflow changes, PR comments, artifacts, source-name guard enforcement, runtime naming enforcement, or package-boundary enforcement.

This is docs/test only. No source scanning is added in this sprint. No allowlist file is added. No allowlist files are added. No JSON/YAML/TOML output is added. No JSON/YAML/TOML allowlist output is added. No report generation is added. No dry-run report generation is added. No JSON output is generated. No JSON output files are generated. No CI workflow change is added. No PR comment or artifact behavior is added. No runtime naming guard is active. No source-name guard is implemented. No source-name guard rule implementation exists. No report file is written. No dry-run command is added. No runtime naming enforcement is added. No source-name guard enforcement is active. No classes are renamed in this sprint. No package moves are made in this sprint. No ArchUnit or package-boundary tool is added. No Maven build files are changed. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

## Executive Summary

PR #232 added [`SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md`](SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md), which defines candidate future source-name guard rule categories. PR #233 added [`SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md), which defines per-rule review questions. PR #234 added [`SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md), which defines future allowlist semantics before implementation.

This allowlist review checklist is the next preparation layer. It defines how reviewers should evaluate future allowlist candidates before any allowlist file, source scanner, dry-run report generator, JSON/YAML/TOML output, CI integration, PR comment, artifact upload, or enforcement exists.

The core rule is:

Allowlist candidates do not certify production safety.

Allowlist does not replace package-boundary enforcement. Allowlist does not replace human review.

## Current Status: Allowlist Review Checklist Only, No Implementation

Current status:

- allowlist review checklist only;
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
- source-name guard allowlist review checklist is not enforcement;
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
- no records/classes/interfaces under `src/main/java`;
- no ArchUnit or package-boundary tool is added;
- no new dependency is added;
- no Maven build files are changed;
- no API fields are added;
- no routing, scoring, strategy, proxy, config, CI, Docker, release, signing, registry, governance, or production behavior changes are made.

This allowlist review checklist does not claim allowlist implementation exists. This allowlist review checklist does not claim source-name guard rule implementation exists. This allowlist review checklist does not claim report generation exists. This allowlist review checklist does not claim source-name guard enforcement is active. This allowlist review checklist does not claim a runtime-enforced LASE boundary. This allowlist review checklist does not claim package-boundary enforcement is active.

## Relationship To Source-Name Guard Allowlist Design Plan

The allowlist design plan defines future allowlist purpose, entry fields, review workflow, expiration and re-review, suppression strategy, privacy/secret-safety rules, deterministic-output rules, misuse risks, and implementation gates.

This checklist depends on [`SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_DESIGN_PLAN.md). Reviewers should use the design plan to understand what a future allowlist is for, then use this checklist to decide whether a candidate exception is narrow, justified, auditable, privacy-safe, and non-proving.

## Relationship To Source-Name Guard Rule Review Checklist

The rule review checklist defines how reviewers should evaluate each future source-name guard rule before implementation.

This checklist depends on [`SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_RULE_REVIEW_CHECKLIST.md). A future allowlist candidate should be reviewed against the rule category, pattern specificity, severity, false-positive risk, suppression, report-output, and rollback expectations from that checklist.

The future source-name guard rule catalog plan is documented in [`SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md`](SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md). The future source-name guard report review checklist is documented in [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md). Both remain docs/test-only references and do not add allowlist files, source scanning, report generation, JSON/YAML/TOML output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

The future source-name guard allowlist sample plan is documented in [`SOURCE_NAME_GUARD_ALLOWLIST_SAMPLE_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_SAMPLE_PLAN.md). That sample plan remains docs/test-only and provides static examples for future allowlist entry shapes, rationale quality, scope/path boundaries, re-review triggers, suppression posture, stale entries, invalid entries, privacy constraints, and misuse risks before any allowlist file, source scanning, report generation, JSON/YAML/TOML output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

The future source-name guard allowlist lifecycle plan is documented in [`SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md`](SOURCE_NAME_GUARD_ALLOWLIST_LIFECYCLE_PLAN.md). That lifecycle plan remains docs/test-only and defines future allowlist entry states, creation, re-review, expiration, retirement, migration, audit, stale-entry risk handling, privacy/secret-safety, and implementation gates before any allowlist file, source scanning, report generation, JSON/YAML/TOML output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

## When This Checklist Should Be Used

Use this checklist when a future report-only source-name guard output, rule review, or reviewer discussion proposes adding a name, pattern, path, or scope to a future allowlist.

Use it before any future allowlist file is introduced. Use it before any future source scanner reads allowlist entries. Use it before any future dry-run report generator, JSON/YAML/TOML output, CI report-only integration, PR comment, artifact upload, or enforcement mode exists.

Do not use this checklist as a replacement for source scanning approval, package-boundary enforcement, LASE runtime boundary review, production-readiness review, security review, dependency-direction review, or human judgment.

## Allowlist Candidate Review Checklist

- [ ] Candidate has a clear rule category.
- [ ] Candidate has a clear name or naming pattern.
- [ ] Candidate has a clear path/scope boundary.
- [ ] Candidate has a clear rationale.
- [ ] Candidate identifies why rename or documentation clarification is not preferred.
- [ ] Candidate identifies reviewer action.
- [ ] Candidate includes notProofStatement.
- [ ] Candidate has a re-review trigger.
- [ ] Candidate does not certify production safety.
- [ ] Candidate does not replace package-boundary enforcement.
- [ ] Candidate does not replace human review.
- [ ] Candidate stays independent from package moves.
- [ ] Candidate stays independent from runtime behavior changes.
- [ ] Candidate remains removable without changing runtime behavior.

## Rationale Review Checklist

- [ ] Rationale is specific.
- [ ] Rationale references the rule category.
- [ ] Rationale explains why the name is acceptable.
- [ ] Rationale explains why a future allowlist candidate is more appropriate than immediate rename or documentation clarification.
- [ ] Rationale avoids production-readiness claims.
- [ ] Rationale avoids certification claims.
- [ ] Rationale avoids live-cloud or real-tenant validation claims.
- [ ] Rationale avoids GPU/grid/facility/carbon-control claims.
- [ ] Rationale avoids claiming that the candidate proves runtime safety.
- [ ] Rationale is understandable to a reviewer.

## Scope/Path Review Checklist

- [ ] Scope is narrow.
- [ ] Scope avoids generated files.
- [ ] Scope avoids build output.
- [ ] Scope avoids release assets.
- [ ] Scope avoids broad repository-wide suppression unless separately approved.
- [ ] Scope avoids absolute local machine paths.
- [ ] Scope avoids secrets or private network details.
- [ ] Scope avoids `target/`, release bundles, build artifacts, and generated report output unless separately approved.
- [ ] Scope can be audited without source scanning false positives expanding silently.

## Rule-Category Review Checklist

- [ ] Candidate maps to a reviewed source-name guard rule category.
- [ ] Candidate identifies whether it relates to production-certification overclaim, replay/scoring proof overclaim, live-control authority, GPU/grid/facility/control implication, carbon-aware production routing, hidden production-routing authority, reviewer metadata ambiguity, or shadow-vs-live allocation ambiguity.
- [ ] Candidate does not invent an ad hoc category only to hide a finding.
- [ ] Candidate includes the expected severity posture for the matching rule category.
- [ ] Candidate includes the expected reviewer action for the matching rule category.
- [ ] Candidate can be re-reviewed if the rule category changes.

## Re-Review Trigger Checklist

- [ ] Re-review on class/name changes.
- [ ] Re-review on rule category changes.
- [ ] Re-review before enforcement mode.
- [ ] Re-review before package moves.
- [ ] Re-review before production-readiness claims.
- [ ] Re-review if false-positive rate changes.
- [ ] Re-review if suppression becomes too broad.
- [ ] Re-review before any future allowlist candidate is used by a source scanner.
- [ ] Re-review before any future allowlist candidate appears in CI report-only output.

## Suppression Review Checklist

- [ ] Suppression is reviewed.
- [ ] Suppression is documented.
- [ ] Suppression is not inline ignore spam.
- [ ] Suppression does not hide whole categories without explicit review.
- [ ] Suppression does not suppress production-certification/control/proof terms without maintainer review.
- [ ] Suppression remains auditable.
- [ ] Suppression can be removed without runtime behavior changes.
- [ ] Suppression does not replace rename review, documentation clarification, rule adjustment, or human review.

## Privacy And Secret-Safety Checklist

- [ ] Candidate contains no secrets.
- [ ] Candidate contains no tokens.
- [ ] Candidate contains no credentials.
- [ ] Candidate contains no environment variable values.
- [ ] Candidate contains no private network details.
- [ ] Candidate contains no absolute local machine paths.
- [ ] Candidate contains no user home directories.
- [ ] Candidate contains no tenant identifiers, cloud account identifiers, service endpoints, reviewer names, or personal data.
- [ ] Candidate can be reviewed without uploading artifacts, posting PR comments, or exposing local machine context unless separately approved.

## Misuse-Risk Checklist

Reviewers should reject or rework an allowlist candidate if any of these risks apply:

- allowlist treated as certification;
- allowlist used to hide unsafe overclaims;
- allowlist too broad;
- allowlist stale after rule/category changes;
- allowlist hides intentionally risky examples incorrectly;
- allowlist tied to generated paths;
- allowlist includes secrets, env values, tokens, private network details, or local machine paths;
- allowlist used instead of package-boundary enforcement;
- allowlist treated as proof of production readiness;
- allowlist treated as proof of runtime LASE boundary enforcement;
- allowlist treated as proof of source-name guard enforcement.

## Approval Gates Before Allowlist Implementation

Before any future allowlist implementation is proposed, reviewers should confirm:

- [ ] Allowlist design reviewed.
- [ ] Allowlist review checklist completed.
- [ ] Rule catalog reviewed.
- [ ] Rule review checklist reviewed.
- [ ] Candidate rationale reviewed.
- [ ] Scope/path reviewed.
- [ ] Rule category reviewed.
- [ ] Re-review trigger reviewed.
- [ ] Suppression review completed.
- [ ] Privacy/secret-safety reviewed.
- [ ] Misuse risks reviewed.
- [ ] False-positive risk reviewed.
- [ ] False-negative risk reviewed.
- [ ] Deterministic output reviewed.
- [ ] Rollback/removal reviewed.
- [ ] No allowlist file is added unless separately approved.
- [ ] No JSON/YAML/TOML allowlist output is added unless separately approved.
- [ ] No source scanning logic is added unless separately approved.
- [ ] Enforcement remains future-only unless separately approved.

## Safety Boundaries And Non-Goals

This sprint is documentation and documentation guard tests only. It keeps these non-goals:

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

This allowlist review checklist does not claim allowlist implementation exists. This allowlist review checklist does not claim source-name guard rule implementation exists. This allowlist review checklist does not claim report generation exists. This allowlist review checklist does not claim source-name guard enforcement is active. This allowlist review checklist does not claim a runtime-enforced LASE boundary. This allowlist review checklist does not claim package-boundary enforcement is active.

## Reviewer-Facing Value

This checklist gives reviewers a stable way to decide whether a future allowlist candidate is narrow, specific, rationale-backed, privacy-safe, reviewable, non-proving, and reversible before any allowlist file or source-name guard implementation exists.

It also keeps the source-name guard sequence conservative: rule catalog, rule review, allowlist design, allowlist candidate review, then only later a separately approved implementation path.
