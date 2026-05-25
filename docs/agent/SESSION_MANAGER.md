# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md`](GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Timestamp: 2026-05-24T22:00-07:00

Goal name: LoadBalancerPro Goal Mode 10-PR Trial

Current PR slot: 8

Checkpoint: Slot 8 PR opened; final checkpoint verification pending

Started from main SHA: ca16382638dbbc118aeab7070a4b8bbf585ae827

Current branch: codex/goal-campaign-reviewer-trust-navigation

PR URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/313

Head SHA: `dd50971bdf3bf88e780200b11135826b2b0f5d8e` at PR creation; final pushed checkpoint head pending remote audit

Changed files:

- docs/REVIEWER_TRUST_MAP.md
- docs/agent/GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignReviewerTrustNavigationDocumentationTest.java

Checks run:

- Slot 7 merged and main green.
- PR #312 merged at `ca16382638dbbc118aeab7070a4b8bbf585ae827`.
- Main pulled with `--ff-only` after the merge.
- Main CI and CodeQL for `ca16382638dbbc118aeab7070a4b8bbf585ae827` completed successfully.
- Slot 8 branch created from clean main.
- Slot 8 documentation/test edit batch completed.
- `mvn test "-Dtest=AgentGoalCampaignReviewerTrustNavigationDocumentationTest"` initially failed twice due exact wording drift and then passed after the wording was made explicit.
- `FAILURE_LOG.md` records both focused guard failures and recoveries.
- `mvn test "-Dtest=AgentGoalCampaignReviewerTrustNavigationDocumentationTest"` passed.
- Focused reviewer-trust/campaign/agent selector bundle passed.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn -q test` passed.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed.
- `git diff --check` passed with line-ending warnings only.
- `git diff --check origin/main...HEAD` passed.
- `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- Slot 8 commit `dd50971bdf3bf88e780200b11135826b2b0f5d8e` pushed.
- PR #313 opened and was mergeable at PR creation.
- Initial PR #313 remote checks were in progress for the first pushed head.

Remote status: PR #313 checks in progress.

Blocker: none.

Next action: push the PR checkpoint update, rerun focused and final-head verification, then wait for current-head remote checks.

Decision: continue

## Current Branch

Name: codex/goal-campaign-reviewer-trust-navigation

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/313

## Current Goal

Short goal: Add reviewer-facing goal campaign navigation to the Reviewer Trust Map, recording slot 7 as merged/main-green and slot 8 as the active scoped PR.

## Current Head SHA

SHA: `dd50971bdf3bf88e780200b11135826b2b0f5d8e` at PR creation; final pushed checkpoint head pending remote audit

## What Changed

- Files changed:
- docs/REVIEWER_TRUST_MAP.md
- docs/agent/GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignReviewerTrustNavigationDocumentationTest.java
- Behavioral surface:
- None; docs/test-only.
- Documentation surface:
- Adds reviewer-facing campaign navigation, links it from the Reviewer Trust Map, and advances the board/session checkpoint to slot 8 after slot 7 merged green.

## Checks Run

- Focused checks:
- AgentGoalCampaignReviewerTrustNavigationDocumentationTest passed after two logged exact-wording recoveries.
- Focused reviewer-trust/campaign/agent selector bundle passed.
- Dependency checks:
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- Full checks:
- `mvn -q test` passed.
- Package checks:
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed.
- Diff checks:
- `git diff --check` passed with line-ending warnings only.
- `git diff --check origin/main...HEAD` passed.
- `git diff --cached --check` passed.
- Smoke checks:
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- Remote checks:
- PR #313 opened; current-head remote checks pending.

## Blockers

- Current blocker:
- None.
- Owner or next decision:
- Codex continues inside campaign scope.

## Next Action

One concrete next step: push the PR checkpoint update, rerun focused and final-head verification, then wait for current-head remote checks.

## Recovery Notes

- How to resume:
- Confirm the branch is `codex/goal-campaign-reviewer-trust-navigation`, inspect `git status`, push the PR checkpoint update if needed, then rerun final-head verification before merge consideration.
- Commands already run:
- `git checkout main`; `git pull --ff-only origin main`; watched PR #312 and main CI/CodeQL checks to green; `git checkout -b codex/goal-campaign-reviewer-trust-navigation`; opened PR #313.
- Safety boundaries to re-check:
- Docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed:
- PR current-head checks after PR creation; main CI/CodeQL after merge.
