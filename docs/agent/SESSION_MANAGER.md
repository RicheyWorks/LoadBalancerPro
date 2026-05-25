# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Timestamp: 2026-05-24T20:42-07:00

Goal name: LoadBalancerPro Goal Mode 10-PR Trial

Current PR slot: 5

Checkpoint: Slot 5 full local verification passed before pre-PR commit

Started from main SHA: 13fad31cd6cbc34efdf58c0a75ec5fa0f66d478e

Current branch: codex/goal-campaign-failure-log-recovery-examples

PR URL: pending

Head SHA: pending pre-PR commit

Changed files:

- docs/agent/GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md
- docs/agent/GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md
- docs/agent/GOAL_CAMPAIGN_CONTRACT.md
- docs/agent/GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/FAILURE_LOG.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignFailureRecoveryExamplesDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignSessionCheckpointExamplesDocumentationTest.java

Checks run:

- Slot 4 merged and main green.
- PR #309 merged at `13fad31cd6cbc34efdf58c0a75ec5fa0f66d478e`.
- Main pulled with `--ff-only` after the merge.
- Main CI and CodeQL for `13fad31cd6cbc34efdf58c0a75ec5fa0f66d478e` completed successfully.
- Slot 5 branch created from clean main.
- `mvn test "-Dtest=AgentGoalCampaignFailureRecoveryExamplesDocumentationTest"` initially failed because the examples used the combined phrase "production readiness/certification" instead of preserving "production certification" explicitly.
- FAILURE_LOG.md records the focused guard failure and recovery.
- `mvn test "-Dtest=AgentGoalCampaignFailureRecoveryExamplesDocumentationTest"` rerun passed.
- Focused campaign/agent selector bundle passed.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn -q test` passed.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed.
- `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.

Remote status: main green after slot 4; slot 5 PR not opened yet.

Blocker: none.

Next action: commit the slot 5 edit batch, then rerun final-head verification before opening PR.

Decision: continue

## Current Branch

Name: codex/goal-campaign-failure-log-recovery-examples

## Current PR

URL: pending

## Current Goal

Short goal: Add FAILURE_LOG campaign recovery examples for the 10-PR goal campaign, recording slot 4 as merged/main-green and slot 5 as the active scoped PR.

## Current Head SHA

SHA: pending pre-PR commit

## What Changed

- Files changed:
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md
- docs/agent/GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md
- docs/agent/GOAL_CAMPAIGN_CONTRACT.md
- docs/agent/GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md
- docs/agent/GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/FAILURE_LOG.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignFailureRecoveryExamplesDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignSessionCheckpointExamplesDocumentationTest.java
- Behavioral surface:
- None; docs/test-only.
- Documentation surface:
- Adds reusable FAILURE_LOG campaign recovery examples for the live 10-PR trial, links them from campaign docs, advances the board/session checkpoint to slot 5, and makes the slot 4 checkpoint examples guard durable for later active slots.

## Checks Run

- Focused checks:
- AgentGoalCampaignFailureRecoveryExamplesDocumentationTest passed after one logged wording fix.
- Focused campaign/agent selector bundle passed.
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
- Main is green after slot 4; slot 5 PR not opened yet.

## Blockers

- Current blocker:
- None.
- Owner or next decision:
- Codex continues inside campaign scope.

## Next Action

One concrete next step: commit the slot 5 edit batch, then rerun final-head verification before opening PR.

## Recovery Notes

- How to resume:
- Confirm the branch is `codex/goal-campaign-failure-log-recovery-examples`, inspect `git status`, then run the slot 5 focused guard and full verification before opening a PR.
- Commands already run:
- `git checkout main`; `git pull --ff-only origin main`; `gh run watch 26381593332 --exit-status`; `gh run watch 26381593327 --exit-status`; `git checkout -b codex/goal-campaign-failure-log-recovery-examples`.
- Safety boundaries to re-check:
- Docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed:
- PR current-head checks after PR creation; main CI/CodeQL after merge.
