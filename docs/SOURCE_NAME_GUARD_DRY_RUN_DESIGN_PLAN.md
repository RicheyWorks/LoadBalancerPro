# Source-Name Guard Dry-Run Design Plan

This document defines a future report-only source-name guard dry-run design. It is a dry-run design only, no implementation, and it does not add source scanning, report generation, CI workflow changes, PR comments, artifacts, source-name guard enforcement, runtime naming enforcement, or package-boundary enforcement.

This is docs/test only. No source scanning is added in this sprint. No CI workflow change is added. No report generation is added. No PR comment bot or artifact upload is added. No runtime naming guard is active. No source-name guard is implemented. No dry-run command is added. No runtime naming enforcement is added. No source-name guard enforcement is active. No classes are renamed in this sprint. No package moves are made in this sprint. No ArchUnit or package-boundary tool is added. No Maven build files are changed. No runtime interface, API field, routing behavior, scoring behavior, strategy behavior, proxy behavior, configuration behavior, CI behavior, Docker behavior, release behavior, registry behavior, governance behavior, or production behavior is added.

## Executive Summary

PR #223 added [`LASE_BOUNDARY_NAMING_GUARD_PLAN.md`](LASE_BOUNDARY_NAMING_GUARD_PLAN.md), which defines future naming guard vocabulary. PR #224 added [`LASE_NAMING_GUARD_INVENTORY.md`](LASE_NAMING_GUARD_INVENTORY.md), which inventories current names before enforcement. PR #225 added [`LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md`](LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md), which scopes a future narrow source-name guard. PR #226 added [`SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md), which gives reviewers a checklist before implementation.

This dry-run design plan is the next preparation layer. It describes how a future source-name guard could run in report-only mode before any enforcement exists.

The core rule is:

Future dry-run findings are review triggers, not runtime safety proof.

The future report schema plan is documented in [`SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md`](SOURCE_NAME_GUARD_REPORT_SCHEMA_PLAN.md). That schema plan remains docs/test-only and does not add source scanning, dry-run report generation, JSON output, CI workflow changes, PR comment/report artifact behavior, runtime naming enforcement, source-name guard enforcement, package-boundary enforcement, class renames, package moves, ArchUnit tooling, Maven build changes, or behavior changes.

## Current Status: Dry-Run Design Only, No Implementation

Current status:

- dry-run design only;
- documentation and documentation guard tests only;
- no implementation;
- no source scanning;
- no CI workflow change is added;
- no report generation is added;
- no PR comment bot or artifact upload is added;
- no dry-run command is added;
- no runtime naming guard is active;
- no source-name guard is implemented;
- source-name guard not implemented yet;
- source-name guard dry run not implemented yet;
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

This dry-run design plan does not claim dry-run implementation is active. This dry-run design plan does not claim source-name guard enforcement is active. This dry-run design plan does not claim a runtime-enforced LASE boundary. This dry-run design plan does not claim package-boundary enforcement is active.

## Relationship To Source-Name Guard Review Checklist

The review checklist defines the questions reviewers should answer before any future source-name guard implementation. This dry-run design plan uses that checklist as a gate for report-only mode.

A future dry-run proposal should complete [`SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md) before it adds source scanning, report output, CI report-only integration, PR comments, artifacts, or enforcement behavior.

## Relationship To Source-Name Guard Feasibility Plan

The feasibility plan defines the safe shape of a future narrow source-name guard: small scope, deterministic behavior, explicit allowlist and denylist review, clear failure messages, and non-proving semantics.

This dry-run design plan depends on [`LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md`](LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md). Dry-run/report-only mode should be treated as an implementation staging concept only. It does not make source-name guard enforcement active, and it does not prove unsafe runtime behavior.

## Why Dry-Run/Report-Only Comes Before Enforcement

Dry-run/report-only mode should come before enforcement because naming checks are weak signals:

- reviewers need to see what would be scanned before builds are blocked;
- risky documentation examples and negative boundary text can create false positives;
- broad scans can produce noisy output and hide useful findings;
- allowlist and suppression workflow should be reviewed before enforcement;
- future report contents should be readable before any CI integration;
- report-only findings should not be mistaken for proof of unsafe runtime behavior;
- enforcement mode must remain future-only and separately approved.

Dry-run mode is useful only if it keeps the future guard non-blocking, deterministic, reviewable, and easy to disable.

## Proposed Future Dry-Run Modes

Future dry-run modes, if separately approved later:

| Mode | Purpose | Boundary |
| --- | --- | --- |
| Local docs-only dry run | Exercise report wording against documented example names and negative boundary text. | Documentation-only; does not scan source names as enforcement. |
| Local source-name dry run | Scan only approved source-name paths and print a report for local review. | Report-only; does not fail builds unless separately approved later. |
| CI report-only dry run | Run in CI as an informational report. | Non-blocking; separate from required enforcement. |
| PR comment/report artifact dry run | Publish reviewer-readable findings in a PR comment or report artifact. | Future-only; must avoid secrets and must not upload artifacts in this sprint. |
| Enforcement mode | Fail builds or block merges for approved naming violations. | Future-only and must require separate approval. |

These are future concepts only. This sprint does not add a dry-run command, CI workflow, artifact upload, report file generation, PR comment bot, or source scanning logic.

## Proposed Future Report Contents

Future report contents should include:

- scanned scope summary;
- files/classes considered;
- allowlisted names;
- flagged names;
- severity;
- reason;
- recommended reviewer action;
- false-positive notes;
- suppression review notes;
- not-proven boundaries;
- statement that findings are review triggers, not runtime safety proof.

A future report must make non-proving semantics visible. It should never claim source-name guard implementation proves runtime safety, package-boundary enforcement, runtime LASE boundary enforcement, production readiness, or production certification.

## Proposed Future Review Workflow

A future review workflow should require:

1. Confirm a dry-run implementation sprint is separately approved.
2. Re-read [`SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md`](SOURCE_NAME_GUARD_REVIEW_CHECKLIST.md).
3. Re-read [`LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md`](LASE_SOURCE_NAME_GUARD_FEASIBILITY_PLAN.md).
4. Confirm dry-run/report-only output is non-blocking.
5. Confirm scanned scope is narrow and deterministic.
6. Confirm report contents include not-proven boundaries.
7. Confirm allowlist and suppression handling is reviewed.
8. Confirm false-positive and false-negative handling is documented.
9. Confirm no CI workflow change is mixed with enforcement mode.
10. Confirm enforcement mode is future-only and separately approved.

Future dry-run findings are review triggers, not runtime safety proof.

## Proposed Future Severity Levels

Future severity levels:

| Severity | Meaning | Required reviewer posture |
| --- | --- | --- |
| `INFO` | Naming should be reviewed but likely safe. | Confirm context and keep as informational unless a pattern repeats. |
| `WARN` | Name may imply unsafe authority or overclaim. | Review scope, owner, and safer wording before accepting. |
| `BLOCKER-CANDIDATE` | Name appears to imply production certification/control/proof, but still requires human review before blocking. | Treat as a candidate for blocking only after human review and policy approval. |

Important severity boundary:

A future dry-run must not automatically prove unsafe behavior. It only identifies review candidates.

## Proposed Future Allowlist/Suppression Workflow

Future allowlist and suppression workflow:

- start from the naming inventory and review checklist;
- keep allowlists small and reviewer-readable;
- require a reason for each allowlisted name;
- require a reason for each suppression;
- require suppression review outside inline ignore comments;
- document whether a suppression is temporary, permanent, or pending rename;
- keep risky documentation examples marked as examples to avoid, not implemented classes;
- ensure suppressions do not hide package-boundary or runtime behavior risks.

Suppressions are review records, not proof of safe runtime behavior.

## False-Positive Handling

Future dry-run false-positive handling should require:

- a plain-language explanation of why the finding is safe or intentionally documented;
- a reviewed allowlist or suppression entry if the finding should remain quiet;
- a review of whether the denylist term is too broad;
- a review of whether the scanned scope is too broad;
- no runtime behavior change just to silence a naming false positive.

False positives should improve guard design, not create churn.

## False-Negative Handling

Future dry-run false-negative handling should acknowledge:

- safe names can still hide unsafe behavior;
- source-name dry runs do not prove package boundaries;
- source-name dry runs do not prove dependency direction;
- source-name dry runs do not prove routing, scoring, strategy, proxy, or production behavior safety;
- package-boundary enforcement and behavior review remain future separate work.

False negatives are why dry-run reports must remain review aids only.

## Future CI Integration Gates

Future CI integration gates:

- no CI integration in this sprint;
- future CI report-only mode must be separate from required enforcement;
- future CI report-only output must not fail builds unless separately approved;
- future enforcement mode must be a separate sprint;
- future PR comments or artifacts must not include secrets;
- future output must avoid noisy broad scans;
- future output must be deterministic;
- future report-only mode must not change Maven build files unless separately approved;
- future report-only mode must not be used to claim production readiness or production certification.

CI report-only mode should make findings visible while preserving review control.

## Rollback And Disable Strategy

A future dry-run implementation should include a rollback and disable strategy:

- disable report-only output without changing runtime behavior;
- remove report generation without changing routing, scoring, strategy, proxy, API, config, Docker, release, registry, governance, or production behavior;
- remove CI report-only integration without touching enforcement mode;
- remove PR comment/report artifact behavior without changing source scanning rules;
- keep rollback independent from class renames and package moves;
- document the fallback to checklist-only review.

This sprint does not implement disable flags, environment variables, system properties, config properties, CI switches, or report files.

## Safety Boundaries And Non-Goals

Explicit non-goals:

- no production Java runtime behavior;
- no records/classes/interfaces under `src/main/java`;
- no class renames;
- no package moves or refactors;
- no source scanning logic in this sprint;
- no dry-run command;
- no report generation;
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

This dry-run design plan does not claim dry-run implementation is active. This dry-run design plan does not claim source-name guard enforcement is active. This dry-run design plan does not claim a runtime-enforced LASE boundary. This dry-run design plan does not claim package-boundary enforcement is active.

## Reviewer-Facing Value

Reviewers can use this plan to understand how a future source-name guard could be introduced as report-only before enforcement. The plan makes dry-run modes, report contents, severity, review workflow, allowlist/suppression handling, false-positive handling, false-negative handling, CI gates, and rollback visible before any implementation exists.

The value is staged review, not enforcement. Source-name guard dry run not implemented yet, source-name guard not implemented yet, source-name guard checklist is not enforcement, LASE naming guard is not runtime-enforced yet, and LASE package boundary is not enforced yet.
