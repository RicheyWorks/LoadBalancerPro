# Campaign System Index

This index is the navigation layer for the LoadBalancerPro multi-PR Codex `/goal` campaign system. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this index when starting, resuming, handing off, auditing, or closing a bounded campaign. It points to the authoritative rule files; it does not replace README.md, AGENTS.md, BUILD_CONTRACT.md, VERIFICATION_PROTOCOL.md, SESSION_MANAGER.md, FAILURE_LOG.md, or the campaign gate documents.

## Core Trust And Execution Files

- README.md is the constitutional layer, Advanced README, public trust surface, reviewer starting point, and high-level claim contract.
- AGENTS.md is the Codex and agent operating rules file.
- BUILD_CONTRACT.md is the focused execution contract for each scoped task.
- AGENT_WORKFLOW_QUICKSTART.md is the startup path for using README.md, AGENTS.md, BUILD_CONTRACT.md, and docs/agent files together.
- GOAL_MODE_LONG_RUN_PROTOCOL.md defines `/goal`, `/plan`, `/goal pause`, `/goal resume`, and `/goal clear` behavior.
- VERIFICATION_PROTOCOL.md defines focused checks, relevant selector bundles, full local verification, remote PR checks, and post-merge main checks.
- SESSION_MANAGER.md records current campaign branch, PR, head SHA, changed files, checks run, blockers, and next action.
- FAILURE_LOG.md records failures, suspected causes, fixes attempted, results, and follow-up action.

## Campaign Control Documents

- CAMPAIGN_SYSTEM_ARCHITECTURE.md defines the ten-PR campaign model, execution loop, checkpoints, stop conditions, and not-proven boundaries.
- CAMPAIGN_CHECKPOINT_LEDGER.md defines required checkpoint fields and counting rules.
- CAMPAIGN_PR_READINESS_CHECKLIST.md defines the per-PR opening, merge, post-merge, scope, and stop-condition checklist.
- CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md defines changed-file, forbidden-scope, claim, guard-test, and stop-condition audits.
- CAMPAIGN_REMOTE_CHECK_AUDIT.md defines current-head PR remote check and merge-commit main check auditing.
- CAMPAIGN_MERGE_GATE.md defines the final pre-merge and post-merge gate before a PR can count.
- CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md defines failure logging, safe recovery, pause, and resume rules.
- CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md defines factual handoff reporting for pauses, resumes, checkpoints, and human review.
- CAMPAIGN_CLOSEOUT_PROTOCOL.md defines final campaign count, verification, merged PR summary, and closeout report rules.
- DECISION_EXPLORER_CAMPAIGN_BOARD.md defines the Decision Explorer Architecture Bootstrap Campaign, DX-G01 through DX-G10, read-only/simulation-only boundaries, PR tracking columns, checkpoint rules, verification expectations, and closeout criteria.

## Campaign Loop

Run one scoped PR at a time:

1. Start from clean main and confirm main remote checks are green.
2. Choose one small docs/test-only slice unless a task explicitly scopes a different surface.
3. Create a `codex/` branch and update SESSION_MANAGER.md.
4. Make the scoped edit and keep the diff inside the contract.
5. Run focused checks while editing.
6. Run the relevant selector bundle and full local verification before merge.
7. Open a PR with scope, verification, safety audit, and remaining not-proven boundaries.
8. Audit current-head remote PR checks and merge only when required checks are complete and successful.
9. Fast-forward local main, run post-merge local checks, and confirm main CI/CodeQL for the merge commit.
10. Count the PR only after the post-merge main gate is green.

The current campaign target is ten successful merged PRs. An opened PR, locally green branch, pending remote check, skipped-only check, duplicate-only check, or unverified main merge commit does not count.

## Required Verification Path

Use focused verification while editing and full verification before merge:

- focused failing test or focused documentation guard;
- relevant focused selector bundle;
- `mvn -q test`;
- `mvn -q "-DskipTests" package`;
- `mvn -B package`;
- `git diff --check`;
- `git diff --check origin/main...HEAD`;
- `git diff --cached --check`;
- enterprise lab package smoke when requested;
- current-head remote PR checks;
- merge-commit main CI/CodeQL checks after merge.

Do not claim green main while remote checks are pending. Do not accept failed, cancelled, stale, pending, skipped-only, or duplicate-only required checks as green.

## Scope And Safety Boundary

For a docs/test-only campaign slice, the scope audit must confirm:

- no production code changes;
- no Maven config changes;
- no CI/workflow changes;
- no Dockerfile changes;
- no Compose behavior changes;
- no runtime behavior changes;
- no endpoint behavior changes;
- no k6, Bruno, or Toxiproxy behavior changes;
- no scripts, secrets, external/cloud/tenant targets, or automation;
- no unsupported claims.

Pause instead of improvising if scope becomes unsafe, checks fail, main is red, or human approval is needed.

## Closeout Use

Use CAMPAIGN_CLOSEOUT_PROTOCOL.md for the final report after the tenth PR merges and main post-merge checks are green. The closeout report must name the merged PRs, branches, PR head SHAs, merge commits, verification commands, remote check states, failures logged, scope audit, and remaining not-proven boundaries.

If the final PR cannot merge safely, use CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md and report the campaign as paused rather than complete.

## Not-Proven Boundaries

This index and the campaign system do not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation.
