# Campaign Closeout Protocol

This protocol defines how a long-running Codex `/goal` campaign closes after a bounded sequence of independently reviewed PRs. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this protocol with CAMPAIGN_SYSTEM_ARCHITECTURE.md, CAMPAIGN_CHECKPOINT_LEDGER.md, CAMPAIGN_PR_READINESS_CHECKLIST.md, CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md, CAMPAIGN_REMOTE_CHECK_AUDIT.md, CAMPAIGN_MERGE_GATE.md, CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md, CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md, GOAL_MODE_LONG_RUN_PROTOCOL.md, SESSION_MANAGER.md, FAILURE_LOG.md, and VERIFICATION_PROTOCOL.md when the campaign reaches its final PR, pauses near completion, or needs a final human-readable status report.

## Purpose

The closeout protocol keeps campaign completion factual. A campaign is complete only after the target count is reached, each PR has merged through its merge gate, local post-merge checks have run, and main remote checks are green for the final merge commit.

For the current LoadBalancerPro campaign, the target count is ten successful merged PRs. Do not count a PR until:

- the PR was separately scoped;
- the reviewed PR head SHA is recorded;
- the PR remote checks passed for the current head;
- the PR merged with a normal merge commit;
- local main was fast-forwarded to the merge commit;
- focused post-merge checks passed on main;
- full post-merge verification passed on main;
- package checks passed on main;
- diff checks passed on main;
- enterprise lab package smoke passed on main;
- main CI and CodeQL passed for the merge commit.

## Required Closeout Fields

Use this structure for the final campaign report:

```text
Campaign:
Target count:
Completed count:
Final main SHA:
Final PR:
Final merge commit:

Merged PRs:
- PR:
  Branch:
  PR head SHA:
  Merge commit:
  Scope:
  Local verification:
  Remote PR checks:
  Main post-merge checks:
  Failures logged:

Campaign scope audit:
Verification summary:
Remote check summary:
Failures and recoveries:
Open blockers:
Remaining not-proven boundaries:
Next recommended action:
```

## Counting Rules

Count conservatively:

- count only merged PRs;
- count only PRs whose required current-head remote checks completed successfully;
- count only PRs whose post-merge main checks completed successfully;
- do not count a PR with pending, failed, cancelled, stale, skipped-only, or duplicate-only required checks;
- do not count a PR if main CI or CodeQL is red for its merge commit;
- do not count a PR if the scope expanded beyond its contract without review;
- do not count a PR if SESSION_MANAGER.md and FAILURE_LOG.md disagree about an unresolved blocker.

If the campaign reaches nine PRs but the tenth PR cannot safely merge, pause the campaign and use CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md instead of claiming closeout.

## Final Verification Gate

Before a closeout report can say the campaign is complete, rerun the final verification set on main:

- final focused guard selector for the last PR;
- relevant campaign guard selector bundle;
- `mvn -q test`;
- `mvn -q "-DskipTests" package`;
- `mvn -B package`;
- `git diff --check`;
- `git diff --cached --check`;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package`;
- main branch CI for the final merge commit;
- main branch CodeQL for the final merge commit.

Do not claim green main while remote checks are pending. Do not accept failed, cancelled, stale, pending, skipped-only, or duplicate-only required checks as green.

## Failure And Pause Handling

If closeout is blocked:

- update SESSION_MANAGER.md with the current branch, PR, head SHA, final known green main SHA, checks run, blocker, and next safe action;
- update FAILURE_LOG.md with the failure type, failing command or remote job, suspected cause, fix attempted, result, and follow-up action;
- pause the campaign instead of improvising;
- resume only after the blocker is resolved and the next step still fits BUILD_CONTRACT.md and AGENTS.md.

## Scope/Safety Audit

The closeout report must repeat the campaign scope boundary:

- docs/test-only when that was the active contract;
- no production code changes unless explicitly scoped;
- no Maven config changes unless explicitly scoped;
- no CI/workflow changes unless explicitly scoped;
- no Dockerfile changes unless explicitly scoped;
- no Compose behavior changes unless explicitly scoped;
- no runtime behavior changes unless explicitly scoped;
- no endpoint behavior changes unless explicitly scoped;
- no k6, Bruno, or Toxiproxy behavior changes unless explicitly scoped;
- no scripts, secrets, external/cloud/tenant targets, or automation unless explicitly scoped.

Use CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md before writing the final scope audit.

## Final Report Discipline

The final report must be grounded in actual checks run. It should name the ten PRs, branch names, PR head SHAs, merge commits, verification commands, remote check states, failures logged, and remaining boundaries.

If any evidence is missing, report WARN or PAUSED instead of PASS. If any required check failed, report FAIL and include the failing command, run, or job.

## Not-Proven Boundaries

Campaign closeout does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation.
