# Source-Name Guard Review Checklist

This document gives reviewers a checklist for evaluating any future source-name guard proposal before implementation. It is a checklist only, no source scanning, and it does not add source-name guard enforcement.

This is docs/test only. No source scanning is added in this sprint. No runtime naming guard is active. No source-name guard is implemented. No runtime naming enforcement is added. No source-name guard enforcement is active. No classes are renamed in this sprint. No package moves are made in this sprint. No source-name guard tests that scan source names are added in this sprint. No ArchUnit or package-boundary tool is added. No Maven build files are changed. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

## Executive Summary

PR #223 added [`LASE_BOUNDARY_NAMING_GUARD_PLAN.md`](LASE_BOUNDARY_NAMING_GUARD_PLAN.md), which defines future naming guard vocabulary. PR #224 added [`LASE_NAMING_GUARD_INVENTORY.md`](LASE_NAMING_GUARD_INVENTORY.md), which inventories current names before enforcement. PR #225 added [`LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md`](LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md), which scopes what a future narrow source-name guard could safely check.

This checklist is the next preparation layer. It gives reviewers a structured way to decide whether a proposed future guard is narrow, deterministic, reviewable, low false-positive risk, easy to roll back, and separate from package moves, behavior changes, and production claims.

The core rule is:

Guard failures are review triggers, not proof of unsafe runtime behavior.

## Current Status: Checklist Only, No Source Scanning

Current status:

- checklist only;
- documentation and documentation guard tests only;
- no source scanning;
- no runtime naming guard is active;
- no source-name guard is implemented;
- source-name guard not implemented yet;
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

This checklist does not claim source-name guard enforcement is active. This checklist does not claim a runtime-enforced LASE boundary. This checklist does not claim package-boundary enforcement is active. Source-name guard checklist is not enforcement.

## Relationship To LASE Source-Name Guard Feasibility Plan

The feasibility plan defines the safe outline for a future source-name guard: narrow scope, deterministic behavior, explicit allowlist and denylist review, clear failure messages, and no proof claims.

This checklist turns that feasibility plan into review questions. It should be used before any future sprint adds source scanning, source-name guard tests, source-name guard enforcement, runtime naming enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

## Relationship To LASE Naming Guard Inventory

The naming inventory maps the current naming surface before enforcement. This checklist depends on that inventory so reviewers can evaluate proposed denylist terms against real current names instead of guesses.

The inventory remains inventory only. This checklist remains checklist only. Neither document proves runtime safety, package-boundary enforcement, source-name guard enforcement, production readiness, or production certification.

## When This Checklist Should Be Used

Use this checklist when a future sprint proposes any source-name guard, denylist term, allowlist strategy, suppression process, source-name scan, or source-name guard failure message.

Do not use this checklist as a replacement for package-boundary enforcement, dependency-direction review, runtime behavior review, API contract review, or production-readiness review.

## Required Review Questions

Reviewers should answer these questions before approving a future guard:

- Is the proposed guard narrow and deterministic?
- Does it avoid broad source scanning?
- Does it avoid scanning generated files, target directories, build output, release assets, or documentation examples unless separately approved?
- Does it avoid flagging intentionally documented risky examples?
- Are denylist terms specific enough to avoid normal engineering language?
- Is there a reviewed allowlist for known safe existing names?
- Is there a documented suppression process that does not encourage inline ignore spam?
- Are failure messages clear and actionable?
- Does the guard treat failures as review triggers, not proof of unsafe runtime behavior?
- Is the guard separate from package moves?
- Is the guard separate from behavior changes?
- Is the guard separate from production-readiness claims?
- Is rollback simple?
- Can the guard be removed without changing runtime behavior?
- Has the false-positive risk been reviewed?
- Has the false-negative risk been reviewed?

## Proposed Guard Scope Checklist

- [ ] The guard scans only paths approved in a separate implementation sprint.
- [ ] The guard avoids broad source scanning.
- [ ] The guard does not scan generated files, `target/`, build output, release assets, dependency directories, or ignored evidence output.
- [ ] The guard does not scan documentation examples unless separately approved.
- [ ] The guard does not flag intentionally documented risky examples as implemented classes.
- [ ] The guard starts with stable production source file names only if that scope is separately approved.
- [ ] The guard does not scan Maven, Docker, CI, release, governance, registry, signing, or configuration files unless separately approved.
- [ ] The guard is deterministic across platforms and does not depend on time, environment variables, system properties, random values, UUIDs, filesystem order, or network state.
- [ ] The guard produces repeatable output from the same checked tree.

## Denylist Term Checklist

- [ ] Each denylist term has a plain-language unsafe implication.
- [ ] Denylist terms are specific enough to avoid normal engineering language.
- [ ] Denylist terms focus on unsafe implication themes such as production certification implication, replay proof implication, scoring proof implication, live grid control implication, facility automation implication, GPU orchestration implication, carbon-aware production routing implication, or hidden production routing authority implication.
- [ ] Denylist terms do not block ordinary live allocation names such as routing, allocation, server, policy, or score names by default.
- [ ] Denylist terms do not block safe LASE naming families such as `LaseShadow`, `LaseEvaluation`, `LaseEvidence`, `ReviewerMetadata`, `ReviewerSummary`, `ExternalSignalSnapshot`, `WorkloadProfileSignalMetadata`, `BoundaryInventory`, or `BoundaryPlan`.
- [ ] Denylist terms are reviewed before they are added.
- [ ] Denylist terms are documented with examples and limits.

## Allowlist And Suppression Checklist

- [ ] Known safe existing names have a reviewed allowlist entry.
- [ ] Allowlist entries explain why the name is safe or intentionally bounded.
- [ ] Risky documentation examples are excluded or allowlisted only as names to avoid, not as implemented classes.
- [ ] Suppressions require reviewed documentation.
- [ ] Suppressions do not encourage inline ignore spam.
- [ ] Suppression review includes the owning category, false-positive reason, and future cleanup path if any.
- [ ] The allowlist is small enough that reviewers can understand it.

## False-Positive Review Checklist

- [ ] The proposal identifies intentionally documented risky examples that must not fail the guard.
- [ ] The proposal identifies negative boundary language that must not be treated as an implementation claim.
- [ ] The proposal explains how safe current live allocation names are handled.
- [ ] The proposal explains how safe LASE evidence/reviewer names are handled.
- [ ] The proposal explains how generated files, build output, and release assets stay out of scope.
- [ ] The proposal explains how reviewers can diagnose and fix a false positive without changing runtime behavior.

False positives should be treated as guard design failures, not as evidence that runtime behavior is unsafe.

## False-Negative Review Checklist

- [ ] The proposal acknowledges that safe names can still hide unsafe behavior.
- [ ] The proposal acknowledges that naming checks do not prove package boundaries.
- [ ] The proposal acknowledges that naming checks do not prove dependency direction.
- [ ] The proposal acknowledges that naming checks do not prove routing, scoring, strategy, proxy, or production behavior safety.
- [ ] The proposal explains what future package-boundary or behavior review would still be required.

False negatives are why naming guards must remain weak early signals only.

## Failure-Message Checklist

- [ ] Failure messages name the risky file or class name.
- [ ] Failure messages explain the unsafe implication in plain language.
- [ ] Failure messages identify the review checklist section that applies.
- [ ] Failure messages say failures are review triggers, not proof of unsafe runtime behavior.
- [ ] Failure messages do not claim source-name guard enforcement proves runtime safety.
- [ ] Failure messages point to the allowlist/suppression review process.
- [ ] Failure messages avoid production-readiness or production-certification claims.

## Rollback And Removal Checklist

- [ ] Rollback is simple.
- [ ] The guard can be removed without changing runtime behavior.
- [ ] The guard can be removed without changing routing, scoring, strategy, proxy, API, config, CI, Docker, release, registry, governance, or production behavior.
- [ ] The guard can be removed without moving packages or renaming classes.
- [ ] The guard can be removed without changing Maven dependencies or build plugins.
- [ ] Removal steps are documented in the future implementation PR.

## Approval Gates Before Implementation

Before any future source-name guard implementation, reviewers should require:

1. Separate sprint approval for source-name scanning.
2. Re-read [`LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md`](LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md).
3. Re-read [`LASE_NAMING_GUARD_INVENTORY.md`](LASE_NAMING_GUARD_INVENTORY.md).
4. Complete this checklist.
5. Confirm source-name guard implementation must not be combined with package moves.
6. Confirm source-name guard implementation must not be combined with behavior changes.
7. Confirm source-name guard implementation must not be combined with production claims.
8. Confirm the guard is not used to claim runtime naming enforcement, package-boundary enforcement, runtime LASE boundary enforcement, production readiness, or production certification.

Source-name guard implementation must not be combined with package moves. Source-name guard implementation must not be combined with behavior changes. Source-name guard implementation must not be combined with production claims.

## Safety Boundaries And Non-Goals

Explicit non-goals:

- no source scanning in this sprint;
- no runtime naming enforcement;
- no source-name guard implementation;
- no source-name guard enforcement;
- no package-boundary enforcement;
- no ArchUnit/tooling implementation;
- no production Java runtime behavior;
- no records/classes/interfaces under `src/main/java`;
- no class renames;
- no package moves or refactors;
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

This checklist does not claim source-name guard enforcement is active. This checklist does not claim a runtime-enforced LASE boundary. This checklist does not claim package-boundary enforcement is active.

## Reviewer-Facing Value

Reviewers can use this checklist to evaluate a future source-name guard proposal before any code starts scanning names. It makes scope, denylist terms, allowlist handling, suppression review, false-positive risk, false-negative risk, failure messages, rollback, and approval gates visible.

The value is review clarity, not enforcement. Source-name guard checklist is not enforcement, source-name guard not implemented yet, LASE naming guard is not runtime-enforced yet, and LASE package boundary is not enforced yet.
