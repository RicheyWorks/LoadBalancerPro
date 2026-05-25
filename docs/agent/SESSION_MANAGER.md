# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Timestamp: 2026-05-24T19:21-07:00

Goal name: LoadBalancerPro Goal Mode 10-PR Trial

Current PR slot: 2

Checkpoint: Slot 2 PR opened; final-head verification pending

Started from main SHA: 9b0efc0dc0d6654c0e8f95294e77e7de72bd7941

Current branch: codex/goal-campaign-board-initialization

PR URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/307

Head SHA: 846e57e725ba27867d7e76a4e97675dfcd05a5ef before the PR-opened checkpoint commit

Changed files:

- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignBoardInitializationDocumentationTest.java

Checks run:

- PR #306 merged at `9b0efc0dc0d6654c0e8f95294e77e7de72bd7941`.
- Main pulled with `--ff-only` after the merge.
- Main CI and CodeQL for `9b0efc0dc0d6654c0e8f95294e77e7de72bd7941` completed successfully.
- Slot 2 branch created from clean main.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn test "-Dtest=AgentGoalCampaignBoardInitializationDocumentationTest"` passed.
- Focused campaign/agent selector bundle initially failed because the slot 1 architecture guard froze the active session checkpoint at slot 1.
- FAILURE_LOG.md records the stale guard failure and recovery.
- Focused campaign/agent selector bundle passed after updating the stale guard.
- `mvn -q test` passed.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed.
- `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- PR #307 opened from head 846e57e725ba27867d7e76a4e97675dfcd05a5ef.

Remote status: PR #307 opened; remote checks pending for the branch head.

Blocker: none.

Next action: commit and push this PR-opened checkpoint, then rerun final-head local verification.

Decision: continue

## Current Branch

Name: codex/goal-campaign-board-initialization

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/307

## Current Goal

Short goal: Initialize the goal campaign board for this trial, recording slot 1 as merged/main-green and slot 2 as the active scoped PR.

## Current Head SHA

SHA: 846e57e725ba27867d7e76a4e97675dfcd05a5ef before the PR-opened checkpoint commit

## What Changed

- Files changed:
- docs/agent/FAILURE_LOG.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignBoardInitializationDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignTemplateArchitectureDocumentationTest.java
- Behavioral surface:
- None; docs/test-only.
- Documentation surface:
- Initializes the live 10-PR trial board with slot 1 merged and main-green, slot 2 active, and remaining slots planned.

## Checks Run

- Focused checks:
- AgentGoalCampaignBoardInitializationDocumentationTest passed.
- Focused campaign/agent selector bundle passed after one logged stale-guard fix.
- Dependency checks:
- Dependency tree for org.apache.tomcat.embed passed.
- Full checks:
- mvn -q test passed.
- Package checks:
- mvn -q -DskipTests package passed; mvn -B package passed.
- Diff checks:
- git diff --check, git diff --check origin/main...HEAD, and git diff --cached --check passed.
- Smoke checks:
- enterprise-lab-workflow.ps1 -Package passed.
- Remote checks:
- PR #307 opened; remote checks pending for the branch head.

## Blockers

- Current blocker:
- None.
- Owner or next decision:
- Codex continues inside campaign scope.

## Next Action

One concrete next step: commit and push this PR-opened checkpoint, then rerun final-head local verification.

## Recovery Notes

- How to resume:
- Confirm the branch is `codex/goal-campaign-board-initialization`, inspect `git status`, then run the slot 2 focused guard and full verification before opening the PR.
- Commands already run:
- `git checkout main`; `git pull --ff-only origin main`; `gh run list --branch main --limit 3`; `git checkout -b codex/goal-campaign-board-initialization`.
- Safety boundaries to re-check:
- Docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed:
- PR current-head checks after PR creation; main CI/CodeQL after merge.
