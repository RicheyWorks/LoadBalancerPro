# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Campaign: 10-PR LoadBalancerPro goal campaign

Checkpoint: PR 10 opened; final-head verification pending

Started from main SHA: fccc80eb0d299d18f5e3ee59f09cb75a5c311c65

Current campaign branch: codex/goal-campaign-system-index

Current campaign PR: https://github.com/RicheyWorks/LoadBalancerPro/pull/305

Completed campaign PRs: 9 / 10

Current blocker: none

Checks completed: PR 1 through PR 9 local, PR, post-merge, and main remote checks passed. Main CI and CodeQL are green for fccc80eb0d299d18f5e3ee59f09cb75a5c311c65. PR 10 branch was created from clean main. The PR 10 edit batch added the campaign system index, cross-links, a guard test, and this checkpoint update. AgentCampaignSystemIndexDocumentationTest passed. Dependency tree for org.apache.tomcat.embed passed. The campaign focused selector bundle passed. mvn -q test passed. mvn -q -DskipTests package passed. mvn -B package passed. git diff --check, git diff --check origin/main...HEAD, and git diff --cached --check passed. Enterprise lab package smoke passed. PR #305 opened from head 518b52d9171c9ca7b6674307fd247d4437a36282.

Next campaign action: commit and push this PR-opened checkpoint, then rerun final-head verification on the updated branch head.

## Current Branch

Name: codex/goal-campaign-system-index

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/305

## Current Goal

Short goal: Add the final campaign system index and documentation guard so the ten-PR campaign has a single navigation and closeout path.

## Current Head SHA

SHA: 518b52d9171c9ca7b6674307fd247d4437a36282 before the PR-opened checkpoint commit

## What Changed

- Files changed:
- README.md
- AGENTS.md
- BUILD_CONTRACT.md
- docs/REVIEWER_TRUST_MAP.md
- docs/agent/AGENT_WORKFLOW_QUICKSTART.md
- docs/agent/GOAL_MODE_LONG_RUN_PROTOCOL.md
- docs/agent/CAMPAIGN_CHECKPOINT_LEDGER.md
- docs/agent/CAMPAIGN_CLOSEOUT_PROTOCOL.md
- docs/agent/CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md
- docs/agent/CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md
- docs/agent/CAMPAIGN_MERGE_GATE.md
- docs/agent/CAMPAIGN_PR_READINESS_CHECKLIST.md
- docs/agent/CAMPAIGN_REMOTE_CHECK_AUDIT.md
- docs/agent/CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md
- docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md
- docs/agent/CAMPAIGN_SYSTEM_INDEX.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignSystemIndexDocumentationTest.java
- Behavioral surface:
- None; docs/test-only.
- Documentation surface:
- Adds the campaign system index and links it from the README, agent contract files, reviewer trust map, goal protocol, session manager, and campaign control docs.

## Checks Run

- Focused checks:
- AgentCampaignSystemIndexDocumentationTest passed.
- Campaign focused selector bundle passed.
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
- PR #305 opened; remote checks pending for the branch head.

## Blockers

- Current blocker:
- None.
- Owner or next decision:
- Codex continues inside campaign scope.

## Next Action

One concrete next step: commit and push this PR-opened checkpoint, then rerun final-head verification.

## Recovery Notes

- How to resume:
- Confirm the branch is `codex/goal-campaign-system-index`, rerun the PR 10 focused guard, then follow the normal campaign verification and PR creation path.
- Commands already run:
- `git fetch origin`; `git checkout main`; `git pull --ff-only origin main`; `git checkout -b codex/goal-campaign-system-index`.
- Safety boundaries to re-check:
- Docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed:
- PR current-head checks after PR creation; main CI/CodeQL after merge.
