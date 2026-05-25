# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Campaign: 10-PR LoadBalancerPro goal campaign

Checkpoint: PR 6 opened

Started from main SHA: 39e1a6a013b2be884b67f8ed840aaada737f514a

Current campaign branch: codex/goal-campaign-failure-recovery-playbook

Current campaign PR: https://github.com/RicheyWorks/LoadBalancerPro/pull/301

Completed campaign PRs: 5 / 10

Current blocker: none

Checks completed: PR 1, PR 2, PR 3, PR 4, and PR 5 local, PR, post-merge, and main remote checks passed. Main CI and CodeQL are green for 39e1a6a013b2be884b67f8ed840aaada737f514a. AgentCampaignFailureRecoveryPlaybookDocumentationTest passed. AgentCampaignFailureRecoveryPlaybookDocumentationTest, AgentCampaignMergeGateDocumentationTest, AgentCampaignRemoteCheckAuditDocumentationTest, AgentCampaignPrReadinessChecklistDocumentationTest, AgentCampaignCheckpointLedgerDocumentationTest, AgentCampaignSystemArchitectureDocumentationTest, and AgentWorkflowQuickstartDocumentationTest passed. Dependency tree for org.apache.tomcat.embed passed. mvn -q test passed. mvn -q -DskipTests package passed. mvn -B package passed. git diff --check, git diff --check origin/main...HEAD, and git diff --cached --check passed. Enterprise lab package smoke passed. PR #301 opened.

Next campaign action: commit and push this PR-opened checkpoint, then rerun final local verification on the final PR head.

## Current Branch

Name: codex/goal-campaign-failure-recovery-playbook

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/301

## Current Goal

Short goal: Add campaign failure recovery playbook docs and guard test.

## Current Head SHA

SHA: d9ecc4913ac6097d2ce30eb39924136b11a54bed

## What Changed

- Files changed:
- docs/agent/CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md
- docs/agent/AGENT_WORKFLOW_QUICKSTART.md
- docs/agent/CAMPAIGN_CHECKPOINT_LEDGER.md
- docs/agent/CAMPAIGN_PR_READINESS_CHECKLIST.md
- docs/agent/CAMPAIGN_REMOTE_CHECK_AUDIT.md
- docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md
- docs/agent/FAILURE_LOG.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignFailureRecoveryPlaybookDocumentationTest.java
- Behavioral surface:
- None; docs/test-only.
- Documentation surface:
- Adds campaign failure recovery playbook and cross-links it into the campaign control docs.

## Checks Run

- Focused checks:
- AgentCampaignFailureRecoveryPlaybookDocumentationTest; campaign focused bundle.
- Full checks:
- mvn -q test; mvn -B package.
- Package checks:
- mvn -q -DskipTests package; mvn -B package.
- Diff checks:
- git diff --check; git diff --check origin/main...HEAD; git diff --cached --check.
- Smoke checks:
- enterprise-lab-workflow.ps1 -Package.
- Remote checks:
- Not opened yet for PR 6.

## Blockers

- Current blocker:
- None.
- Owner or next decision:
- Codex continues inside campaign scope.

## Next Action

One concrete next step: commit and push the PR-opened checkpoint, then rerun the PR 6 local verification gate on the final head.

## Recovery Notes

- How to resume:
- Commands already run:
- Safety boundaries to re-check:
- Remote checks that must be refreshed:
