# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), and [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Campaign: 10-PR LoadBalancerPro goal campaign

Checkpoint: PR 3 opened after local verification

Started from main SHA: a405c3aeb54d309a69228a5dee2ea663faac0322

Current campaign branch: codex/goal-campaign-pr-readiness-checklist

Current campaign PR: https://github.com/RicheyWorks/LoadBalancerPro/pull/298

Completed campaign PRs: 2 / 10

Current blocker: none

Checks completed: PR 1 and PR 2 local, PR, post-merge, and main remote checks passed; AgentCampaignPrReadinessChecklistDocumentationTest; AgentCampaignPrReadinessChecklistDocumentationTest, AgentCampaignCheckpointLedgerDocumentationTest, AgentCampaignSystemArchitectureDocumentationTest, AgentGoalModeLongRunProtocolDocumentationTest, AgentWorkflowQuickstartDocumentationTest; dependency tree for org.apache.tomcat.embed; mvn -q test; mvn -q -DskipTests package; mvn -B package; git diff --check; git diff --cached --check; enterprise lab package smoke.

Next campaign action: rerun final local verification on the PR 3 head, then audit remote checks.

## Current Branch

Name: codex/goal-campaign-pr-readiness-checklist

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/298

## Current Goal

Short goal: Add campaign PR readiness checklist docs and guard test.

## Current Head SHA

SHA: d90bc5d46403efddc166ef0aed6ee00365ea6876

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
