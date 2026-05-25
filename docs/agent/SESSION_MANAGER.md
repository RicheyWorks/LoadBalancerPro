# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) and keep the current PR checkpoint factual.

## Active Campaign Checkpoint

Timestamp: 2026-05-24T19:47-07:00

Goal name: LoadBalancerPro Goal Mode 10-PR Trial

Current PR slot: 3

Checkpoint: Slot 3 PR opened; final-head verification pending

Started from main SHA: a4e2a9780de53857280748b51e097364a9872b45

Current branch: codex/goal-campaign-build-contract-example

PR URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/308

Head SHA: 01b133bf15ae4e061d43701f91dd9667372a6f0c before the PR-opened checkpoint commit

Changed files:

- BUILD_CONTRACT.md
- docs/agent/FAILURE_LOG.md
- docs/agent/GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/GOAL_CAMPAIGN_CONTRACT.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignBoardInitializationDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignBuildContractExampleDocumentationTest.java

Checks run:

- Slot 2 merged and main green.
- PR #307 merged at `a4e2a9780de53857280748b51e097364a9872b45`.
- Main pulled with `--ff-only` after the merge.
- Main CI and CodeQL for `a4e2a9780de53857280748b51e097364a9872b45` completed successfully.
- Slot 3 branch created from clean main.
- `mvn test "-Dtest=AgentGoalCampaignBuildContractExampleDocumentationTest"` passed.
- Focused campaign/agent selector bundle passed.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn -q test` passed.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed.
- `git diff --check` and `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- PR creation initially failed because PowerShell parsed `-q` from the intended PR body as a gh flag; FAILURE_LOG.md records the failure and recovery path.
- PR #308 opened from head `01b133bf15ae4e061d43701f91dd9667372a6f0c`.

Remote status: PR #308 opened; remote checks pending for the branch head.

Blocker: none.

Next action: commit and push this PR-opened checkpoint, then rerun final-head local verification.

Decision: continue

## Current Branch

Name: codex/goal-campaign-build-contract-example

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/308

## Current Goal

Short goal: Add a filled BUILD_CONTRACT example for the 10-PR goal campaign, recording slot 2 as merged/main-green and slot 3 as the active scoped PR.

## Current Head SHA

SHA: 01b133bf15ae4e061d43701f91dd9667372a6f0c before the PR-opened checkpoint commit

## What Changed

- Files changed:
- BUILD_CONTRACT.md
- docs/agent/FAILURE_LOG.md
- docs/agent/GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/GOAL_CAMPAIGN_CONTRACT.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignBoardInitializationDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignBuildContractExampleDocumentationTest.java
- Behavioral surface:
- None; docs/test-only.
- Documentation surface:
- Adds a filled BUILD_CONTRACT example for the live 10-PR trial, advances the board/session checkpoint to slot 3, and records the recovered PR body quoting failure.

## Checks Run

- Focused checks:
- AgentGoalCampaignBuildContractExampleDocumentationTest passed.
- Focused campaign/agent selector bundle passed.
- Dependency checks:
- Dependency tree for org.apache.tomcat.embed passed.
- Full checks:
- mvn -q test passed.
- Package checks:
- mvn -q -DskipTests package passed; mvn -B package passed.
- Diff checks:
- git diff --check and git diff --cached --check passed.
- Smoke checks:
- enterprise-lab-workflow.ps1 -Package passed.
- Remote checks:
- PR #308 opened; remote checks pending for the branch head.

## Blockers

- Current blocker:
- None.
- Owner or next decision:
- Codex continues inside campaign scope.

## Next Action

One concrete next step: commit and push this PR-opened checkpoint, then rerun final-head local verification.

## Recovery Notes

- How to resume:
- Confirm the branch is `codex/goal-campaign-build-contract-example`, inspect `git status`, then run the slot 3 focused guard and full verification before merging PR #308.
- Commands already run:
- `git checkout main`; `git pull --ff-only origin main`; `gh run list --branch main --limit 5`; `git checkout -b codex/goal-campaign-build-contract-example`; `gh pr create --body-file -`.
- Safety boundaries to re-check:
- Docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed:
- PR current-head checks after PR creation; main CI/CodeQL after merge.
