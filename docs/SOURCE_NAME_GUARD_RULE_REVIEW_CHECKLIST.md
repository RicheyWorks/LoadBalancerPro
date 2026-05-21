# Source-Name Guard Rule Review Checklist

This document gives reviewers a checklist for evaluating each future source-name guard rule candidate before implementation. It is a rule review checklist only, no implementation, and it does not add source scanning, dry-run report generation, JSON output files, CI workflow changes, PR comments, artifacts, source-name guard enforcement, runtime naming enforcement, or package-boundary enforcement.

This is docs/test only. No source scanning is added in this sprint. No report generation is added. No JSON output is generated. No JSON output files are generated. No CI workflow change is added. No PR comment or artifact behavior is added. No runtime naming guard is active. No source-name guard is implemented. No source-name guard rule implementation exists. No dry-run report generation is added. No report file is written. No dry-run command is added. No runtime naming enforcement is added. No source-name guard enforcement is active. No classes are renamed in this sprint. No package moves are made in this sprint. No ArchUnit or package-boundary tool is added. No Maven build files are changed. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

## Executive Summary

PR #225 added [`LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md`](LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md), which scopes a future narrow source-name guard. PR #226 added [`SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md), which gives reviewers implementation-readiness questions. PR #227 added [`SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md`](SOURCE_NAME_GUARD_DRY_RUN_DESIGN_PLAN.md), which describes future report-only dry-run modes. PR #228 added [`SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md), which defines report vocabulary. PR #229 added [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md), which defines report review. PR #230 added [`SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SAMPLE_PLAN.md), which provides static sample reports. PR #231 added [`SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md), which defines report quality gates. PR #232 added [`SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md`](SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md), which defines candidate future rule categories.

This rule review checklist is the next preparation layer. It defines how reviewers should evaluate each future rule candidate from the catalog before any scanner, report generator, JSON output, CI integration, PR comment, artifact upload, or enforcement exists.

The core rule is:

Future source-name guard rule hits are review triggers, not proof of unsafe runtime behavior.

Clean output is not production safety proof.

## Current Status: Rule Review Checklist Only, No Implementation

Current status:

- rule review checklist only;
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

This rule review checklist does not claim source-name guard rule implementation exists. This rule review checklist does not claim report generation exists. This rule review checklist does not claim source-name guard enforcement is active. This rule review checklist does not claim a runtime-enforced LASE boundary. This rule review checklist does not claim package-boundary enforcement is active.

## Relationship To Source-Name Guard Rule Catalog Plan

The rule catalog plan defines candidate future rule categories, risky example names, safe naming families, severity guidance, allowlist expectations, false-positive risks, reviewer actions, and implementation gates.

This checklist depends on [`SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md`](SOURCE_NAME_GUARD_RULE_CATALOG_PLAN.md). Reviewers should use the catalog to identify which future category a rule belongs to, then use this checklist to decide whether that candidate rule is narrow, deterministic, reviewable, privacy-safe, and non-proving enough to propose later.

## Relationship To Source-Name Guard Report Acceptance Criteria Plan

The report acceptance criteria plan defines future report quality, finding quality, severity quality, privacy/secret-safety, determinism, rejection criteria, and approval gates.

This checklist depends on [`SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_ACCEPTANCE_CRITERIA_PLAN.md). A future rule candidate should not be approved for implementation unless its output can satisfy those report acceptance criteria.

The future report schema plan is documented in [`SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md). The future report review checklist is documented in [`SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REPORT_REVIEW_CHECKLIST.md). Both remain docs/test-only references and do not add source scanning, report generation, JSON output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

## Why Rule Review Comes Before Implementation

Rule review comes before implementation because a future source-name guard can become noisy, misleading, or too broad if individual rules are not reviewed before scanning begins:

- each rule needs a narrow category and intent;
- each rule needs reviewed risky and safe examples;
- each pattern needs specificity checks before it can scan source names;
- each severity assignment needs non-blocking report-only semantics;
- each false-positive risk needs a reviewer strategy;
- each false-negative risk needs a reminder that naming checks do not prove behavior safety;
- each allowlist and suppression path needs an auditable review process;
- each report-output expectation must be non-proving and privacy-safe;
- each rollback path must stay independent from runtime behavior.

This checklist defines review questions only. It does not implement source scanning, dry-run report generation, JSON output, CI workflow changes, PR comment/report artifact behavior, or enforcement.

## Per-Rule Review Checklist

For each future source-name guard rule candidate, reviewers should confirm:

- [ ] Rule category is clearly named.
- [ ] Rule intent is clear.
- [ ] Reviewed risky name examples are documented.
- [ ] Reviewed safe name examples are documented.
- [ ] Severity guidance is documented.
- [ ] False-positive risk is documented.
- [ ] False-negative risk is documented.
- [ ] Allowlist strategy is documented.
- [ ] Suppression strategy is documented.
- [ ] Reviewer action is documented.
- [ ] Report output expectation is documented.
- [ ] Rollback/removal path is documented.
- [ ] Rule does not claim runtime safety proof.
- [ ] Rule does not claim production readiness or certification.
- [ ] Rule does not claim source-name guard enforcement is active.
- [ ] Rule does not claim package-boundary enforcement is active.

Per-rule review is a human review gate only. It does not implement rule execution.

## Rule Intent Checklist

- [ ] Rule detects possible naming overclaim only.
- [ ] Rule does not infer runtime behavior from name alone.
- [ ] Rule does not certify clean output as safe.
- [ ] Rule does not claim package-boundary enforcement.
- [ ] Rule does not claim LASE runtime enforcement.
- [ ] Rule treats hits as review triggers, not proof of unsafe runtime behavior.
- [ ] Rule keeps production-readiness and production-certification language out of findings except as non-goal or risky-example context.
- [ ] Rule avoids granting routing authority to report-only output.

Rule intent should stay about naming implication, not runtime truth.

## Rule Scope Checklist

- [ ] Rule scope is narrow.
- [ ] Rule avoids generated files.
- [ ] Rule avoids build output.
- [ ] Rule avoids release assets.
- [ ] Rule avoids examples unless separately approved.
- [ ] Rule avoids broad ambiguous terms.
- [ ] Rule is deterministic.
- [ ] Rule can be reviewed by humans.
- [ ] Rule scope is documented before source scanning is proposed.
- [ ] Rule scope avoids docs-only risky examples unless a later sprint separately approves that review.

Scope review should prevent broad scans and noisy findings before implementation exists.

## Pattern Specificity Checklist

- [ ] Pattern is specific enough to avoid normal engineering language.
- [ ] Pattern avoids flagging safe reviewer/evidence terms.
- [ ] Pattern avoids flagging intentionally documented risky examples.
- [ ] Pattern has clear allowlist examples.
- [ ] Pattern has clear failure message expectations.
- [ ] Pattern identifies the rule category it belongs to.
- [ ] Pattern has a documented reason for each denylist term.
- [ ] Pattern avoids matching negative boundary language such as non-goals or not-proven statements.
- [ ] Pattern can be explained without private context or hidden runtime assumptions.

Specificity review keeps future rules narrow and reviewer-readable.

## Severity Assignment Checklist

- [ ] `INFO` is for clarity review only.
- [ ] `WARN` is for possible unsafe authority or overclaim.
- [ ] `BLOCKER_CANDIDATE` is for possible production certification/control/proof implications.
- [ ] `BLOCKER_CANDIDATE` is not an automatic build failure in report-only mode.
- [ ] Severity does not prove unsafe runtime behavior.
- [ ] Severity does not certify clean output as safe.
- [ ] Severity is mapped to a reviewer action.
- [ ] Severity can be downgraded if false-positive risk is high.

Severity assignment is reviewer metadata only unless a separate enforcement sprint is approved later.

## False-Positive Review Checklist

- [ ] False-positive risk is documented for the rule category.
- [ ] Intentionally documented risky examples are excluded or reviewed separately.
- [ ] Negative boundary text is not treated as an implementation claim.
- [ ] Safe naming families are documented.
- [ ] Normal engineering language is not swept into the pattern.
- [ ] The rule has a human review path for borderline matches.
- [ ] The rule can be narrowed before implementation if it is noisy.
- [ ] The rule does not require runtime behavior changes just to silence a false positive.

False positives should improve rule design, not create unrelated churn.

## False-Negative Review Checklist

- [ ] False-negative risk is documented for the rule category.
- [ ] Reviewers acknowledge that safe names can still hide unsafe behavior.
- [ ] Reviewers acknowledge that naming checks do not prove package boundaries.
- [ ] Reviewers acknowledge that naming checks do not prove dependency direction.
- [ ] Reviewers acknowledge that naming checks do not prove routing, scoring, strategy, proxy, or production behavior safety.
- [ ] The rule documents what future package-boundary or behavior review would still be required.
- [ ] Clean output is not production safety proof.

False-negative review keeps naming checks honest about what they cannot prove.

## Allowlist Review Checklist

- [ ] Allowlist strategy is documented.
- [ ] Allowlist entries require a rationale.
- [ ] Allowlist entries identify the rule category.
- [ ] Allowlist entries are reviewed by humans.
- [ ] Allowlist entries are easy to audit.
- [ ] Allowlist entries do not certify production safety.
- [ ] Allowlist entries do not replace package-boundary enforcement.
- [ ] Allowlist entries distinguish safe existing names from intentionally documented risky examples.

Allowlist review is reviewer metadata only, not runtime safety proof.

## Suppression Review Checklist

- [ ] Suppression strategy is documented.
- [ ] Suppressions require reviewed documentation.
- [ ] Suppressions avoid inline ignore spam.
- [ ] Suppressions include a reason and future cleanup path when appropriate.
- [ ] Suppressions do not hide package-boundary or runtime behavior risks.
- [ ] Suppressions can be removed without changing runtime behavior.
- [ ] Suppression counts, if reported later, are review metadata only.

Suppressions should be auditable and reversible.

## Report-Output Review Checklist

- [ ] Report output expectation is documented before implementation.
- [ ] Findings include a clear name, category, reason, severity, reviewer action, and not-proof statement.
- [ ] Findings avoid secrets, credentials, tokens, environment values, private network details, and absolute local paths.
- [ ] Findings are review triggers, not proof of unsafe runtime behavior.
- [ ] Clean output is not production safety proof.
- [ ] `BLOCKER_CANDIDATE` is not automatic build failure in report-only mode.
- [ ] Report output does not claim source-name guard enforcement is active.
- [ ] Report output does not claim package-boundary enforcement is active.

Report-output review should keep future findings actionable without turning reports into certification.

## Rollback Review Checklist

- [ ] Rollback/removal path is documented.
- [ ] Rule can be removed without changing runtime behavior.
- [ ] Rule can be disabled without package moves.
- [ ] Rule can be revised without CI workflow changes unless separately approved.
- [ ] Rule can be removed without changing routing, scoring, strategy, proxy, API, config, Docker, release, registry, governance, or production behavior.
- [ ] Rule rollback does not delete historical reviewer context.
- [ ] Rule rollback does not imply the name was safe or unsafe at runtime.

Rollback review keeps future rule changes reversible and low-risk.

## Approval Gates Before Implementation

Before a future source-name guard rule implementation, reviewers should require:

1. Rule catalog reviewed.
2. Rule review checklist completed.
3. Acceptance criteria reviewed.
4. Report schema reviewed.
5. Sample report reviewed.
6. False-positive risk reviewed.
7. False-negative risk reviewed.
8. Allowlist strategy reviewed.
9. Deterministic output reviewed.
10. Privacy/secret-safety reviewed.
11. Rollback strategy reviewed.
12. Source scanning scope reviewed separately.
13. JSON output reviewed separately.
14. CI report-only behavior reviewed separately.
15. PR comment/report artifact behavior reviewed separately.
16. Enforcement remains future-only unless separately approved.

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

This rule review checklist does not claim source-name guard rule implementation exists. This rule review checklist does not claim report generation exists. This rule review checklist does not claim source-name guard enforcement is active. This rule review checklist does not claim a runtime-enforced LASE boundary. This rule review checklist does not claim package-boundary enforcement is active.

## Reviewer-Facing Value

Reviewers can use this checklist to evaluate individual future source-name guard rule candidates before any scanner, report generator, JSON output, CI integration, PR comment, artifact upload, or enforcement exists. It makes rule intent, scope, pattern specificity, severity, false-positive risk, false-negative risk, allowlist strategy, suppression strategy, report output, rollback, and approval gates visible before implementation.

The value is rule review clarity, not enforcement. Source-name guard rule review checklist is not enforcement, source-name guard rule catalog is not implementation, source-name guard report acceptance criteria is not enforcement, source-name guard report sample is not generated output, source-name guard report review checklist is not enforcement, source-name guard report schema not implemented yet, source-name guard dry run not implemented yet, source-name guard not implemented yet, LASE naming guard is not runtime-enforced yet, and LASE package boundary is not enforced yet.
