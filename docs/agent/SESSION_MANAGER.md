# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Timestamp: 2026-05-24T19:39-07:00

Goal name: LoadBalancerPro Goal Mode 10-PR Trial

Current PR slot: 3

Checkpoint: Slot 2 merged and main green; slot 3 branch created

Started from main SHA: a4e2a9780de53857280748b51e097364a9872b45

Current branch: codex/goal-campaign-build-contract-example

PR URL: pending

Head SHA: pending slot 3 commit

Changed files:

- BUILD_CONTRACT.md
- docs/agent/GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignBuildContractExampleDocumentationTest.java

Checks run:

- PR #307 merged at `a4e2a9780de53857280748b51e097364a9872b45`.
- Main pulled with `--ff-only` after the merge.
- Main CI and CodeQL for `a4e2a9780de53857280748b51e097364a9872b45` completed successfully.
- Slot 3 branch created from clean main.

Remote status: slot 2 post-merge main green; slot 3 PR not opened yet.

Blocker: none.

Next action: add the filled BUILD_CONTRACT campaign example, run focused verification, then run full local verification before opening PR slot 3.

Decision: continue

## Current Branch

Name: codex/goal-campaign-build-contract-example

## Current PR

URL: pending

## Current Goal

Short goal: Add a filled BUILD_CONTRACT example for the 10-PR goal campaign, recording slot 2 as merged/main-green and slot 3 as the active scoped PR.

## Current Head SHA

SHA: pending slot 3 commit

## What Changed

- Files changed:
- BUILD_CONTRACT.md
- docs/agent/GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignBuildContractExampleDocumentationTest.java
- Behavioral surface:
- None; docs/test-only.
- Documentation surface:
- Adds a filled BUILD_CONTRACT example for the live 10-PR trial and advances the board/session checkpoint to slot 3.

## Checks Run

- Focused checks:
- Pending for slot 3.
- Dependency checks:
- Pending for slot 3.
- Full checks:
- Pending for slot 3.
- Package checks:
- Pending for slot 3.
- Diff checks:
- Pending for slot 3.
- Smoke checks:
- Pending for slot 3.
- Remote checks:
- Slot 2 main CI/CodeQL green; slot 3 PR not opened yet.

## Blockers

- Current blocker:
- None.
- Owner or next decision:
- Codex continues inside campaign scope.

## Next Action

One concrete next step: run the slot 3 focused guard after the BUILD_CONTRACT example edit batch.

## Recovery Notes

- How to resume:
- Confirm the branch is `codex/goal-campaign-build-contract-example`, inspect `git status`, then run the slot 3 focused guard and full verification before opening the PR.
- Commands already run:
- `git checkout main`; `git pull --ff-only origin main`; `gh run list --branch main --limit 5`; `git checkout -b codex/goal-campaign-build-contract-example`.
- Safety boundaries to re-check:
- Docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed:
- PR current-head checks after PR creation; main CI/CodeQL after merge.
