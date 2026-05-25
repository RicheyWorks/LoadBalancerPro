# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Timestamp: 2026-05-24T20:06-07:00

Goal name: LoadBalancerPro Goal Mode 10-PR Trial

Current PR slot: 4

Checkpoint: Slot 3 merged and main green; slot 4 branch created

Started from main SHA: 0a855c2579b02d238d043f1152572985dce5bf82

Current branch: codex/goal-campaign-session-checkpoint-examples

PR URL: pending

Head SHA: pending slot 4 commit

Changed files:

- docs/agent/GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignSessionCheckpointExamplesDocumentationTest.java

Checks run:

- Slot 3 merged and main green.
- PR #308 merged at `0a855c2579b02d238d043f1152572985dce5bf82`.
- Main pulled with `--ff-only` after the merge.
- Main CI and CodeQL for `0a855c2579b02d238d043f1152572985dce5bf82` completed successfully.
- Slot 4 branch created from clean main.

Remote status: slot 3 post-merge main green; slot 4 PR not opened yet.

Blocker: none.

Next action: add SESSION_MANAGER campaign checkpoint examples and run the slot 4 focused guard.

Decision: continue

## Current Branch

Name: codex/goal-campaign-session-checkpoint-examples

## Current PR

URL: pending

## Current Goal

Short goal: Add SESSION_MANAGER campaign checkpoint examples for the 10-PR goal campaign, recording slot 3 as merged/main-green and slot 4 as the active scoped PR.

## Current Head SHA

SHA: pending slot 4 commit

## What Changed

- Files changed:
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md
- docs/agent/GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignSessionCheckpointExamplesDocumentationTest.java
- Behavioral surface:
- None; docs/test-only.
- Documentation surface:
- Adds reusable SESSION_MANAGER checkpoint examples for the live 10-PR trial and advances the board/session checkpoint to slot 4.

## Checks Run

- Focused checks:
- Pending for slot 4.
- Dependency checks:
- Pending for slot 4.
- Full checks:
- Pending for slot 4.
- Package checks:
- Pending for slot 4.
- Diff checks:
- Pending for slot 4.
- Smoke checks:
- Pending for slot 4.
- Remote checks:
- Slot 3 main CI/CodeQL green; slot 4 PR not opened yet.

## Blockers

- Current blocker:
- None.
- Owner or next decision:
- Codex continues inside campaign scope.

## Next Action

One concrete next step: run the slot 4 focused guard after the checkpoint examples edit batch.

## Recovery Notes

- How to resume:
- Confirm the branch is `codex/goal-campaign-session-checkpoint-examples`, inspect `git status`, then run the slot 4 focused guard and full verification before opening the PR.
- Commands already run:
- `git checkout main`; `git pull --ff-only origin main`; `gh run list --branch main --limit 5`; `git checkout -b codex/goal-campaign-build-contract-example`; `gh pr create --body-file -`.
- Safety boundaries to re-check:
- Docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed:
- PR current-head checks after PR creation; main CI/CodeQL after merge.
