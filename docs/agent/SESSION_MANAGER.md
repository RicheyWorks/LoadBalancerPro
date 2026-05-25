# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Campaign: 10-PR LoadBalancerPro goal campaign

Checkpoint: PR 9 opened and final-head verification pending

Started from main SHA: a8a659e04aa01836785359d3a8e94bc5b29b5d7a

Current campaign branch: codex/goal-campaign-closeout-protocol

Current campaign PR: https://github.com/RicheyWorks/LoadBalancerPro/pull/304

Completed campaign PRs: 8 / 10

Current blocker: none

Checks completed: PR 1, PR 2, PR 3, PR 4, PR 5, PR 6, PR 7, and PR 8 local, PR, post-merge, and main remote checks passed. Main CI and CodeQL are green for a8a659e04aa01836785359d3a8e94bc5b29b5d7a. PR 9 branch was created from clean main. AgentCampaignCloseoutProtocolDocumentationTest passed. The campaign focused selector bundle passed. Dependency tree for org.apache.tomcat.embed passed. mvn -q test passed. mvn -q -DskipTests package passed. mvn -B package passed. git diff --check, git diff --check origin/main...HEAD, and git diff --cached --check passed. Enterprise lab package smoke passed. PR #304 opened.

Next campaign action: commit and push this PR-opened checkpoint, then rerun final local verification on the final PR head.

## Current Branch

Name: codex/goal-campaign-closeout-protocol

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/304

## Current Goal

Short goal: Add campaign closeout protocol docs and guard test.

## Current Head SHA

SHA: 6d34ffa1edc92633fc7b2bc4d4549e71c56148b1

## What Changed

- Files changed:
- docs/agent/AGENT_WORKFLOW_QUICKSTART.md
- docs/agent/CAMPAIGN_CHECKPOINT_LEDGER.md
- docs/agent/CAMPAIGN_CLOSEOUT_PROTOCOL.md
- docs/agent/CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md
- docs/agent/CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md
- docs/agent/CAMPAIGN_MERGE_GATE.md
- docs/agent/CAMPAIGN_PR_READINESS_CHECKLIST.md
- docs/agent/CAMPAIGN_REMOTE_CHECK_AUDIT.md
- docs/agent/CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md
- docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignCloseoutProtocolDocumentationTest.java
- Behavioral surface:
- None; docs/test-only.
- Documentation surface:
- Adds campaign closeout protocol and cross-links it into the campaign control docs.

## Checks Run

- Focused checks:
- AgentCampaignCloseoutProtocolDocumentationTest passed.
- Campaign focused selector bundle passed.
- Full checks:
- mvn -q test passed.
- Package checks:
- mvn -q -DskipTests package passed; mvn -B package passed.
- Diff checks:
- git diff --check, git diff --check origin/main...HEAD, and git diff --cached --check passed.
- Smoke checks:
- enterprise-lab-workflow.ps1 -Package passed.
- Remote checks:
- PR #304 opened; remote checks pending for the branch head.

## Blockers

- Current blocker:
- None.
- Owner or next decision:
- Codex continues inside campaign scope.

## Next Action

One concrete next step: commit and push this PR-opened checkpoint, then rerun final local verification on the final PR head.

## Recovery Notes

- How to resume:
- Commands already run:
- Safety boundaries to re-check:
- Remote checks that must be refreshed:
