# Campaign Merge Gate

This document defines the merge decision gate for each multi-PR Codex `/goal` campaign slice. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this gate with CAMPAIGN_SYSTEM_ARCHITECTURE.md, CAMPAIGN_CHECKPOINT_LEDGER.md, CAMPAIGN_PR_READINESS_CHECKLIST.md, CAMPAIGN_REMOTE_CHECK_AUDIT.md, GOAL_MODE_LONG_RUN_PROTOCOL.md, SESSION_MANAGER.md, FAILURE_LOG.md, and VERIFICATION_PROTOCOL.md before merging any campaign PR.

## Gate Purpose

The merge gate turns the campaign rules into one final decision point. A campaign PR may merge only when the reviewed branch head, local verification, remote PR checks, scope audit, and human decision state all agree that the PR is safe to advance.

The gate does not replace review. It records what must be true before merge and what must happen after merge before the campaign count advances.

## Required Pre-Merge Confirmations

Before merging a campaign PR, confirm:

- the PR is open, non-draft, based on main, and mergeable;
- the current PR head SHA matches the reviewed head SHA;
- the branch head SHA is recorded in SESSION_MANAGER.md;
- the diff remains inside the current PR contract;
- no production code, Maven config, CI/workflow, Dockerfile, Compose behavior, runtime behavior, endpoint behavior, k6 behavior, Bruno behavior, Toxiproxy behavior, scripts, secrets, external/cloud/tenant targets, or automation changed outside explicit scope;
- focused local checks passed for the final PR head;
- the relevant focused selector bundle passed for the final PR head;
- `mvn -q test` passed for the final PR head;
- `mvn -q "-DskipTests" package` passed for the final PR head;
- `mvn -B package` passed for the final PR head;
- `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed when applicable;
- the enterprise lab package smoke passed when the task contract requires it;
- the PR body or final report states scope, verification, safety audit, and remaining not-proven boundaries.

## Required Remote PR Checks

Before merge, refresh GitHub state for the current PR head SHA. Required checks must be complete and successful for that exact head.

Required successful checks are:

- Build/Test/Package/Smoke;
- Analyze Java / CodeQL;
- Dependency Review when applicable.

Do not merge if any required check is failed, cancelled, stale, queued, in-progress, pending, skipped-only, or duplicate-only. If duplicate check names appear, identify the current-head successful required run and confirm no active duplicate remains unresolved in the PR rollup.

## Merge Method

Use the repository's normal GitHub PR merge commit unless the task contract explicitly says otherwise.

Do not squash. Do not rebase. Do not delete the branch, create a release or tag, or mutate GitHub settings, rulesets, secrets, environments, or required checks as part of this gate.

## Post-Merge Main Gate

After merge, the campaign PR does not count until:

- local main fast-forwards to the merge commit;
- the merged PR head is contained in main;
- post-merge focused guard checks pass on main;
- post-merge full local verification passes on main;
- main Build/Test/Package/Smoke is successful for the merge commit;
- main Analyze Java / CodeQL is successful for the merge commit;
- no main required check is failed, cancelled, stale, queued, in-progress, or pending.

Record the merge commit, new main head, post-merge checks, and next action in SESSION_MANAGER.md.

## Stop Conditions

Pause the campaign and update FAILURE_LOG.md if:

- the current PR head SHA changed unexpectedly and the new diff has not been reviewed;
- scope expands beyond the current PR contract;
- a local focused check, full check, package check, diff check, or smoke check fails and the fix is not obvious and safe;
- a remote PR check or main merge-commit check fails, is cancelled, is stale, or remains pending at a merge decision;
- main CI/CodeQL is red;
- a human decision is needed;
- README.md, AGENTS.md, BUILD_CONTRACT.md, reviewer trust boundaries, or not-proven boundaries would be weakened.

## Not-Proven Boundaries

Passing the campaign merge gate does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation.
