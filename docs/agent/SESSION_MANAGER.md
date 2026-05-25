# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md`](GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md), [`GOAL_CAMPAIGN_AGENT_DISCIPLINE.md`](GOAL_CAMPAIGN_AGENT_DISCIPLINE.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Timestamp: 2026-05-24T22:25-07:00

Goal name: LoadBalancerPro Goal Mode 10-PR Trial

Current PR slot: 9

Checkpoint: Slot 9 full local verification passed; PR opening pending

Started from main SHA: f2e3c784b5858b61e664844502ce66d3cc31d352

Current branch: codex/goal-campaign-agents-discipline

PR URL: pending

Head SHA: pending until slot 9 commit

Changed files:

- AGENTS.md
- docs/agent/GOAL_CAMPAIGN_AGENT_DISCIPLINE.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignAgentsDisciplineDocumentationTest.java

Checks run:

- Slot 8 merged and main green.
- PR #313 merged at `f2e3c784b5858b61e664844502ce66d3cc31d352`.
- Main pulled with `--ff-only` after the merge.
- Main CI and CodeQL for `f2e3c784b5858b61e664844502ce66d3cc31d352` completed successfully.
- Slot 8 post-merge local verification passed: dependency tree, campaign focused selector, `mvn -q test`, package checks, diff checks, and enterprise lab package smoke.
- Slot 9 branch created from clean main.
- Slot 9 documentation/test edit batch completed.
- `mvn test "-Dtest=AgentGoalCampaignAgentsDisciplineDocumentationTest"` initially failed due exact wording drift and a self-referential forbidden API name list, then passed after the failure was logged and fixed.
- `FAILURE_LOG.md` records the focused guard failure and recovery.
- `mvn test "-Dtest=AgentGoalCampaignAgentsDisciplineDocumentationTest"` passed.
- Focused campaign/agent selector bundle passed.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn -q test` passed.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed.
- `git diff --check` passed with line-ending warnings only.
- `git diff --check origin/main...HEAD` passed.
- `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.

Remote status: no slot 9 PR opened yet.

Blocker: none.

Next action: commit the slot 9 docs/test-only changes, push the branch, and open the PR.

Decision: continue

## Current Branch

Name: codex/goal-campaign-agents-discipline

## Current PR

URL: pending

## Current Goal

Short goal: Add AGENTS.md campaign discipline guidance, recording slot 8 as merged/main-green and slot 9 as the active scoped PR.

## Current Head SHA

SHA: pending until slot 9 commit

## What Changed

- Files changed:
- AGENTS.md
- docs/agent/GOAL_CAMPAIGN_AGENT_DISCIPLINE.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignAgentsDisciplineDocumentationTest.java
- Behavioral surface:
- None; docs/test-only.
- Documentation surface:
- Adds campaign discipline guidance to AGENTS.md and a dedicated docs/agent page, then advances the board/session checkpoint to slot 9 after slot 8 merged green.

## Checks Run

- Focused checks:
- AgentGoalCampaignAgentsDisciplineDocumentationTest passed after one logged exact-wording/self-check recovery.
- Focused campaign/agent selector bundle passed.
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
- No slot 9 PR opened yet.

## Blockers

- Current blocker:
- None.
- Owner or next decision:
- Codex continues inside campaign scope.

## Next Action

One concrete next step: commit the slot 9 docs/test-only changes and open the pull request.

## Recovery Notes

- How to resume:
- Confirm the branch is `codex/goal-campaign-agents-discipline`, inspect `git status`, and run slot 9 focused verification.
- Commands already run:
- `git checkout main`; `git pull --ff-only origin main`; watched PR #313 and main CI/CodeQL checks to green; `git checkout -b codex/goal-campaign-agents-discipline`.
- Safety boundaries to re-check:
- Docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed:
- PR current-head checks after PR creation; main CI/CodeQL after merge.
