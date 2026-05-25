# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Timestamp: 2026-05-24T21:14-07:00

Goal name: LoadBalancerPro Goal Mode 10-PR Trial

Current PR slot: 6

Checkpoint: Slot 6 full local verification passed; PR open pending

Started from main SHA: 702070aa6b0db90743986176bb96d1bf9208381b

Current branch: codex/goal-campaign-verification-protocol-refinement

PR URL: pending

Head SHA: pending until slot 6 commit

Changed files:

- docs/agent/GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md
- docs/agent/GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md
- docs/agent/GOAL_CAMPAIGN_CONTRACT.md
- docs/agent/VERIFICATION_PROTOCOL.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignVerificationProtocolRefinementDocumentationTest.java

Checks run:

- Slot 5 merged and main green.
- PR #310 merged at `702070aa6b0db90743986176bb96d1bf9208381b`.
- Main pulled with `--ff-only` after the merge.
- Main CI and CodeQL for `702070aa6b0db90743986176bb96d1bf9208381b` completed successfully.
- Slot 6 branch created from clean main.
- Slot 6 documentation/test edit batch completed.
- `mvn test "-Dtest=AgentGoalCampaignVerificationProtocolRefinementDocumentationTest"` passed.
- Focused campaign/agent selector bundle passed.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn -q test` passed.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed.
- `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.

Remote status: no PR opened yet for slot 6.

Blocker: none.

Next action: commit, push, and open PR slot 6.

Decision: continue

## Current Branch

Name: codex/goal-campaign-verification-protocol-refinement

## Current PR

URL: pending

## Current Goal

Short goal: Add VERIFICATION_PROTOCOL campaign mode refinement for the 10-PR goal campaign, recording slot 5 as merged/main-green and slot 6 as the active scoped PR.

## Current Head SHA

SHA: pending until slot 6 commit

## What Changed

- Files changed:
- docs/agent/GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md
- docs/agent/GOAL_CAMPAIGN_CONTRACT.md
- docs/agent/GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md
- docs/agent/VERIFICATION_PROTOCOL.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignVerificationProtocolRefinementDocumentationTest.java
- Behavioral surface:
- None; docs/test-only.
- Documentation surface:
- Adds reusable campaign verification protocol refinement for the live 10-PR trial, links it from verification/campaign docs, and advances the board/session checkpoint to slot 6 after slot 5 merged green.

## Checks Run

- Focused checks:
- AgentGoalCampaignVerificationProtocolRefinementDocumentationTest passed.
- Focused campaign/agent selector bundle passed.
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
- No slot 6 PR opened yet.

## Blockers

- Current blocker:
- None.
- Owner or next decision:
- Codex continues inside campaign scope.

## Next Action

One concrete next step: commit, push, and open the slot 6 PR.

## Recovery Notes

- How to resume:
- Confirm the branch is `codex/goal-campaign-verification-protocol-refinement`, inspect `git status`, then run the slot 6 focused guard and full verification before opening a PR.
- Commands already run:
- `git checkout main`; `git pull --ff-only origin main`; watched PR #310 and main CI/CodeQL checks to green; `git checkout -b codex/goal-campaign-verification-protocol-refinement`.
- Safety boundaries to re-check:
- Docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed:
- PR current-head checks after PR creation; main CI/CodeQL after merge.
