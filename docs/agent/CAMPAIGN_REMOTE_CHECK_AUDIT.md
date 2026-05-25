# Campaign Remote Check Audit

This document defines the remote-check audit for each multi-PR Codex `/goal` campaign slice. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this audit with CAMPAIGN_SYSTEM_ARCHITECTURE.md, CAMPAIGN_CHECKPOINT_LEDGER.md, CAMPAIGN_PR_READINESS_CHECKLIST.md, CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md, CAMPAIGN_MERGE_GATE.md, CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md, CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md, GOAL_MODE_LONG_RUN_PROTOCOL.md, SESSION_MANAGER.md, FAILURE_LOG.md, and VERIFICATION_PROTOCOL.md before merging a campaign PR and before counting the PR after merge.

## Audit Purpose

Remote checks are the campaign's external confirmation that the branch or merge commit is still safe to advance. They do not replace local verification, scope review, or human judgment.

CAMPAIGN_MERGE_GATE.md consumes this audit as one input to the final merge decision.

The audit records:

- PR number and branch;
- expected PR head SHA;
- current PR head SHA from GitHub;
- base branch;
- draft state;
- mergeability;
- required check names;
- latest check run status;
- latest check run conclusion;
- run trigger and branch;
- whether a skipped or duplicate check is only a duplicate, not the required success;
- main merge commit after merge;
- main CI/CodeQL state for the merge commit.

## Required PR Checks

Before merge, confirm the current PR head has successful current-head checks for:

- Build/Test/Package/Smoke;
- Analyze Java / CodeQL;
- Dependency Review when applicable.

Do not accept failed, cancelled, stale, skipped-only, duplicate-only, queued, in-progress, or pending required checks as green.

If GitHub shows duplicate checks for the same name, identify the current-head successful run and confirm no active duplicate remains pending in the PR rollup at the merge decision.

## Required Main Checks

After merge, confirm:

- local main fast-forwarded to the merge commit;
- the merged PR head is contained in main;
- main CodeQL is successful for the merge commit;
- main Build/Test/Package/Smoke is successful for the merge commit;
- no main required check is failed, cancelled, stale, queued, in-progress, or pending.

Do not count the campaign PR until main remote checks are green for the merge commit.

## Audit Commands

Use commands like these as inspection aids, not automation:

```powershell
gh pr view <pr-number> --json number,title,state,isDraft,mergeable,headRefName,headRefOid,baseRefName,url,statusCheckRollup
gh run list --branch <branch-name> --limit 10
gh run watch <run-id> --exit-status --interval 15
gh run list --branch main --limit 10
```

Command output must be interpreted against the current branch head or merge commit. Do not rely on stale earlier runs.

## Failure Handling

If a remote PR check or main check fails, is cancelled, becomes stale, or remains pending at a merge decision, use CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md and update FAILURE_LOG.md before pausing. Record the failing run or job name, suspected cause, fix attempted, result, follow-up action, and last known good SHA.

Do not merge while any required remote check is unresolved. Do not continue to the next campaign PR while main is red or pending for the merge commit.

## Not-Proven Boundaries

Remote check success in a campaign does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation.
