# Campaign Scope Audit Checklist

This checklist defines the scope audit for every small PR in a multi-PR Codex `/goal` campaign. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this checklist with CAMPAIGN_SYSTEM_ARCHITECTURE.md, CAMPAIGN_CHECKPOINT_LEDGER.md, CAMPAIGN_PR_READINESS_CHECKLIST.md, CAMPAIGN_REMOTE_CHECK_AUDIT.md, CAMPAIGN_MERGE_GATE.md, CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md, CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md, GOAL_MODE_LONG_RUN_PROTOCOL.md, SESSION_MANAGER.md, FAILURE_LOG.md, and VERIFICATION_PROTOCOL.md before opening, merging, or counting a campaign PR.

## Purpose

The scope audit keeps a campaign slice small enough to review and safe enough to continue:

- confirm the PR started from clean main;
- confirm the current branch and head SHA are recorded;
- confirm the changed files match the active PR contract;
- confirm SESSION_MANAGER.md names the current checkpoint, checks run, blocker state, and next action;
- confirm FAILURE_LOG.md records any local, remote, tooling, or scope-audit failure;
- confirm unsupported claims and unsafe behavior did not appear;
- confirm the campaign must pause instead of improvising when scope becomes unclear or unsafe.

## Changed-File Audit

Before opening and before merging a campaign PR, inspect `git diff --name-status origin/main...HEAD` and `git diff --stat origin/main...HEAD`.

For a docs/test-only campaign slice, the changed files must remain limited to:

- Markdown documentation;
- documentation guard tests under `src/test/java`;
- SESSION_MANAGER.md checkpoint updates;
- FAILURE_LOG.md entries for actual failures.

Stop and use CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md if any unexpected file appears.

## Forbidden Out-Of-Scope Changes

Unless the active PR contract explicitly scopes them, the campaign PR must not change:

- `src/main/java`;
- Maven config or dependencies;
- CI/workflow files;
- Dockerfile;
- production Compose files;
- local Compose behavior;
- runtime resources;
- scripts;
- app behavior;
- endpoint behavior;
- production API/routing/scoring/proxy behavior;
- reviewer portal/API behavior;
- EvidencePacket/EvidenceAssembler behavior;
- replay/report/storage/export behavior;
- k6 behavior;
- Bruno behavior;
- Toxiproxy behavior.

The PR must not add:

- automation;
- CI/Maven wiring;
- Docker/Compose services;
- runner services;
- secrets or credentials;
- external/cloud/tenant targets;
- production-looking defaults.

## Claim Audit

Confirm the diff does not add or imply unsupported claims:

- no production readiness claim;
- no production certification claim;
- no live-cloud validation claim;
- no real-tenant validation claim;
- no runtime enforcement claim;
- no load/stress/benchmarking claim;
- no throughput/p95/p99 evidence claim;
- no replay/evidence/report/storage/export proof claim;
- no broader automation claim.

If a claim is ambiguous, tighten the wording or pause for human review before opening or merging the PR.

## Review Questions

Use these questions at each scope checkpoint:

- Does the diff still match the active BUILD_CONTRACT.md and PR contract?
- Are README.md, AGENTS.md, BUILD_CONTRACT.md, reviewer trust docs, and not-proven boundaries preserved?
- Are all changed docs linked from the relevant campaign control docs?
- Do the guard tests only read tracked files?
- Did any guard test start servers, run Docker, run Compose, run tools, call network endpoints, execute processes, write files, or depend on environment?
- Did any production code, build config, CI, Docker, Compose, runtime, endpoint, script, secret, external target, or automation change appear unexpectedly?
- Did any production readiness, certification, live-cloud, real-tenant, runtime enforcement, performance, replay, evidence, report, storage, export, or broader automation claim appear?

## Stop Conditions

Pause the campaign, update SESSION_MANAGER.md, and log the issue in FAILURE_LOG.md when:

- the changed-file audit finds an unexpected path;
- the diff expands beyond the active PR contract;
- safety boundary language is weakened;
- README.md, AGENTS.md, BUILD_CONTRACT.md, or reviewer trust boundaries are weakened;
- a guard test must be loosened to pass;
- local verification fails and the fix is not obvious and inside scope;
- remote checks are failed, cancelled, stale, queued, in-progress, pending, skipped-only, or duplicate-only at a merge decision;
- main CI/CodeQL is red;
- a human decision is needed.

## Not-Proven Boundaries

Passing this scope audit does not prove production readiness. It does not prove production certification. It does not prove live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation.
