# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Campaign: 10-PR LoadBalancerPro goal campaign

Checkpoint: PR 8 opened and final-head verification pending

Started from main SHA: ee65436193f8ca855b5dab571eb7b154cddc3158

Current campaign branch: codex/goal-campaign-handoff-report-template

Current campaign PR: https://github.com/RicheyWorks/LoadBalancerPro/pull/303

Completed campaign PRs: 7 / 10

Current blocker: none

Checks completed: PR 1, PR 2, PR 3, PR 4, PR 5, PR 6, and PR 7 local, PR, post-merge, and main remote checks passed. Main CI and CodeQL are green for ee65436193f8ca855b5dab571eb7b154cddc3158. PR 8 branch was created from clean main. AgentCampaignHandoffReportTemplateDocumentationTest passed. The campaign focused selector bundle passed. Dependency tree for org.apache.tomcat.embed passed. mvn -q test passed. mvn -q -DskipTests package passed. mvn -B package passed. git diff --check, git diff --check origin/main...HEAD, and git diff --cached --check passed. Enterprise lab package smoke passed. PR #303 opened.

Next campaign action: commit and push this PR-opened checkpoint, then rerun final local verification on the final PR head.

## Current Branch

Name: codex/goal-campaign-handoff-report-template

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/303

## Current Goal

Short goal: Add campaign handoff report template docs and guard test.

## Current Head SHA

SHA: ddff9d6cdc3fb8458c679dfd415abf08d2bb9776

## What Changed

- Files changed:
- docs/agent/CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md
- docs/agent/CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md
- docs/agent/AGENT_WORKFLOW_QUICKSTART.md
- docs/agent/CAMPAIGN_CHECKPOINT_LEDGER.md
- docs/agent/CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md
- docs/agent/CAMPAIGN_MERGE_GATE.md
- docs/agent/CAMPAIGN_PR_READINESS_CHECKLIST.md
- docs/agent/CAMPAIGN_REMOTE_CHECK_AUDIT.md
- docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md
- docs/agent/FAILURE_LOG.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignHandoffReportTemplateDocumentationTest.java
- Behavioral surface:
- None; docs/test-only.
- Documentation surface:
- Adds campaign handoff report template and cross-links it into the campaign control docs.

## Checks Run

- Focused checks:
- AgentCampaignHandoffReportTemplateDocumentationTest passed.
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
- PR #303 opened; remote checks pending for the branch head.

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
