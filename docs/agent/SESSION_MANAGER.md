# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md) and [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Campaign: 10-PR LoadBalancerPro goal campaign

Checkpoint: PR 2 opened after local verification

Started from main SHA: 66221c046fa5c510ab537d5f00ea0fb0a9ff7a9d

Current campaign branch: codex/goal-campaign-checkpoint-ledger

Current campaign PR: https://github.com/RicheyWorks/LoadBalancerPro/pull/297

Completed campaign PRs: 1 / 10

Current blocker: none

Checks completed: PR 1 local, PR, post-merge, and main remote checks passed; AgentCampaignCheckpointLedgerDocumentationTest; AgentCampaignCheckpointLedgerDocumentationTest, AgentCampaignSystemArchitectureDocumentationTest, AgentGoalModeLongRunProtocolDocumentationTest, AgentWorkflowQuickstartDocumentationTest; dependency tree for org.apache.tomcat.embed; mvn -q test; mvn -q -DskipTests package; mvn -B package; git diff --check; git diff --cached --check; enterprise lab package smoke.

Next campaign action: rerun final local verification on the PR 2 head, then audit remote checks.

## Current Branch

Name: codex/goal-campaign-checkpoint-ledger

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/297

## Current Goal

Short goal: Add campaign checkpoint ledger docs and guard test.

## Current Head SHA

SHA: 5c197445b4e3809cc00abc659f9065232e09cd7b

## What Changed

- Files changed:
- Behavioral surface:
- Documentation surface:

## Checks Run

- Focused checks:
- Full checks:
- Package checks:
- Diff checks:
- Smoke checks:
- Remote checks:

## Blockers

- Current blocker:
- Owner or next decision:

## Next Action

One concrete next step:

## Recovery Notes

- How to resume:
- Commands already run:
- Safety boundaries to re-check:
- Remote checks that must be refreshed:
