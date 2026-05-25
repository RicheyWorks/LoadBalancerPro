# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Timestamp: 2026-05-24T18:58-07:00

Goal name: LoadBalancerPro Goal Mode 10-PR Trial

Current PR slot: 1

Checkpoint: Slot 1 PR opened; final-head verification pending

Started from main SHA: 3ff40933e6dc486ea5dad37bf0113e27996a97b9

Current branch: codex/goal-campaign-template-architecture

PR URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/306

Head SHA: 9ac75a295bda318e0cf60da4e12de46865b1c847 before the PR-opened checkpoint commit

Changed files:

- docs/agent/GOAL_CAMPAIGN_CONTRACT.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/GOAL_CAMPAIGN_PR_TEMPLATE.md
- docs/agent/GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md
- docs/agent/GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignTemplateArchitectureDocumentationTest.java

Checks run:

- Required source files read.
- PR #295 confirmed merged.
- Main checked out, pulled with `--ff-only`, and confirmed clean.
- Main CI and CodeQL checked green for the current main lineage before slot 1 edits.
- Campaign template docs missing check completed and found the five requested template docs absent.
- Slot 1 branch created from clean main.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn test "-Dtest=AgentGoalCampaignTemplateArchitectureDocumentationTest"` passed.
- Campaign focused selector bundle passed.
- `mvn -q test` passed.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed.
- `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- PR #306 opened from head 9ac75a295bda318e0cf60da4e12de46865b1c847.

Remote status: PR #306 opened; remote checks pending for the branch head.

Blocker: none.

Next action: commit and push this PR-opened checkpoint, then rerun final-head local verification.

Decision: continue

## Current Branch

Name: codex/goal-campaign-template-architecture

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/306

## Current Goal

Short goal: Add the goal campaign template architecture so this 10-PR trial has a reusable contract, board, PR template, checkpoint template, final report template, and guard test.

## Current Head SHA

SHA: 9ac75a295bda318e0cf60da4e12de46865b1c847 before the PR-opened checkpoint commit

## What Changed

- Files changed:
- docs/agent/GOAL_CAMPAIGN_CONTRACT.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/GOAL_CAMPAIGN_PR_TEMPLATE.md
- docs/agent/GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md
- docs/agent/GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignTemplateArchitectureDocumentationTest.java
- Behavioral surface:
- None; docs/test-only.
- Documentation surface:
- Adds reusable campaign templates for one scoped PR at a time, 10 PR slots, PR status tracking, checkpoint records, final reporting, verification levels, remote check rules, merge rules, failure logging, stop conditions, and not-proven boundaries.

## Checks Run

- Focused checks:
- AgentGoalCampaignTemplateArchitectureDocumentationTest passed.
- Campaign focused selector bundle passed.
- Dependency checks:
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
- PR #306 opened; remote checks pending for the branch head.

## Blockers

- Current blocker:
- None.
- Owner or next decision:
- Codex continues inside campaign scope.

## Next Action

One concrete next step: commit and push this PR-opened checkpoint, then rerun final-head local verification.

## Recovery Notes

- How to resume:
- Confirm the branch is `codex/goal-campaign-template-architecture`, inspect `git status`, then run the slot 1 focused guard and full verification before opening the PR.
- Commands already run:
- `git fetch origin`; `git checkout main`; `git pull --ff-only origin main`; `git checkout -b codex/goal-campaign-template-architecture`.
- Safety boundaries to re-check:
- Docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed:
- PR current-head checks after PR creation; main CI/CodeQL after merge.
