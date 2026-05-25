# Campaign PR Readiness Checklist

This checklist defines the review gate for each small PR in a multi-PR Codex `/goal` campaign. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this checklist with CAMPAIGN_SYSTEM_ARCHITECTURE.md, CAMPAIGN_CHECKPOINT_LEDGER.md, CAMPAIGN_REMOTE_CHECK_AUDIT.md, CAMPAIGN_MERGE_GATE.md, CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md, GOAL_MODE_LONG_RUN_PROTOCOL.md, SESSION_MANAGER.md, FAILURE_LOG.md, and VERIFICATION_PROTOCOL.md before opening or merging a campaign PR.

## Purpose

The checklist keeps each campaign PR independently reviewable:

- one scoped PR at a time;
- clean main before branch creation;
- current main CI/CodeQL green before starting;
- current branch and head SHA recorded;
- scope stays inside the PR contract;
- focused checks run while editing;
- full local verification runs before merge;
- remote PR checks are current-head and green before merge;
- CAMPAIGN_MERGE_GATE.md is satisfied before merge;
- post-merge main checks are green before the campaign count advances.

## Before Opening The PR

Confirm all of the following before opening a campaign PR:

- the branch started from clean main;
- SESSION_MANAGER.md records the branch, goal, current head SHA, changed files, checks run, blocker state, and next action;
- FAILURE_LOG.md records any local failure, remote failure, scope audit failure, or recovered tooling failure;
- the diff is limited to the scoped deliverables;
- no production code, Maven config, CI/workflow, Dockerfile, Compose behavior, runtime behavior, endpoint behavior, k6 behavior, Bruno behavior, Toxiproxy behavior, scripts, secrets, external/cloud/tenant targets, or automation changed outside explicit scope;
- focused documentation guard tests passed;
- the relevant focused selector bundle passed;
- `mvn -q test` passed;
- `mvn -q "-DskipTests" package` passed;
- `mvn -B package` passed;
- `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed when applicable;
- the enterprise lab package smoke passed when requested by the task contract.

## Before Merging The PR

Confirm all of the following before merging:

- the PR is open, non-draft, based on main, and mergeable;
- the branch head SHA matches the reviewed PR head SHA;
- the changed files are still inside scope;
- required remote PR checks are complete and successful for the current head;
- Build/Test/Package/Smoke is successful for the current head;
- Analyze Java / CodeQL is successful for the current head;
- Dependency Review is successful when applicable;
- no required check is failed, cancelled, stale, pending, or duplicate-only;
- no unsupported production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation claim appeared.

## After Merge

The campaign count increases only after:

- the PR is merged with the intended merge method;
- local main fast-forwards to the merge commit;
- the PR head is contained in main;
- post-merge local focused checks pass;
- post-merge full local verification passes;
- main CI/CodeQL is green for the merge commit.

Do not start the next campaign PR while main checks are pending, failed, cancelled, or stale.

## Stop Conditions

Pause the campaign, use CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md, and update FAILURE_LOG.md if:

- scope expands beyond the current PR contract;
- main CI/CodeQL is red;
- local focused or full verification fails and the fix is not obvious and safe;
- remote PR or main checks fail, are cancelled, are stale, or remain pending at a merge decision;
- a human decision is needed;
- unsafe production behavior, endpoint behavior, Compose behavior, CI/Maven wiring, runtime resources, scripts, secrets, external/cloud/tenant targets, or automation appears outside explicit scope;
- the PR would weaken README.md, AGENTS.md, BUILD_CONTRACT.md, or not-proven boundary language.

## Not-Proven Boundaries

Campaign PR readiness does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation.
