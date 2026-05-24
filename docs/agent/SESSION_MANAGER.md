# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), and [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Campaign: 10-PR LoadBalancerPro goal campaign

Checkpoint: PR 4 opened after local verification

Started from main SHA: f87c24537c48ead4bc60288633106223a4a05d74

Current campaign branch: codex/goal-campaign-remote-check-audit

Current campaign PR: https://github.com/RicheyWorks/LoadBalancerPro/pull/299

Completed campaign PRs: 3 / 10

Current blocker: none

Checks completed: PR 1, PR 2, and PR 3 local, PR, post-merge, and main remote checks passed; AgentCampaignRemoteCheckAuditDocumentationTest; AgentCampaignRemoteCheckAuditDocumentationTest, AgentCampaignPrReadinessChecklistDocumentationTest, AgentCampaignCheckpointLedgerDocumentationTest, AgentCampaignSystemArchitectureDocumentationTest, AgentWorkflowQuickstartDocumentationTest; dependency tree for org.apache.tomcat.embed; mvn -q test; mvn -q -DskipTests package; mvn -B package; git diff --check; git diff --cached --check; enterprise lab package smoke.

Next campaign action: rerun final local verification on the PR 4 head, then audit remote checks.

## Current Branch

Name: codex/goal-campaign-remote-check-audit

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/299

## Current Goal

Short goal: Add campaign remote check audit docs and guard test.

## Current Head SHA

SHA: 14d04564547ef42bc5bfeae5d870865ebea3f2f7

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
