# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), campaign-specific contracts and boards, [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md), and keep the current PR checkpoint factual.

Historical 10-PR trial references remain available through [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md`](GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md), [`GOAL_CAMPAIGN_AGENT_DISCIPLINE.md`](GOAL_CAMPAIGN_AGENT_DISCIPLINE.md), and [`GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md`](GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md), but they are historical closeout records rather than the active campaign pointer.

## Active Campaign Checkpoint

Timestamp: 2026-05-25T02:59-07:00

Goal name: LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign

Current PR slot: 7

Checkpoint: Slot 7 local verification passed and ready for PR creation after one logged selector-guard repair

Started from main SHA: `06d800c478b308ef836b0ab01d8b641d8b1a35f0`

Current branch: codex/evidence-audit-dockerfile-runtime

PR URL: pending

Head SHA: `06d800c478b308ef836b0ab01d8b641d8b1a35f0` at branch creation

Changed files:

- docs/agent/EVIDENCE_AUDIT_DOCKERFILE_RUNTIME_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/FAILURE_LOG.md
- README.md
- docs/REVIEWER_TRUST_MAP.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditDockerfileRuntimeAuditDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditCodeqlDependencyReviewAuditDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditMavenDependencyPostureAuditDocumentationTest.java

Checks run:

- Slot 6 PR #321 merged as `06d800c478b308ef836b0ab01d8b641d8b1a35f0`.
- Slot 6 final branch head was `e2798905b6d5a5633a965dd6c44ede7e553ece88`.
- Slot 6 post-merge local verification passed: focused campaign/agent selector bundle, dependency tree, `mvn -q test`, `mvn -q "-DskipTests" package`, `mvn -B package`, diff checks, and enterprise lab package smoke.
- Slot 6 post-merge main remote checks passed: CI and CodeQL green for `06d800c478b308ef836b0ab01d8b641d8b1a35f0`.
- Slot 7 branch created from clean main.
- Slot 7 Dockerfile runtime audit started as documentation/test-only.
- `mvn test "-Dtest=AgentEvidenceAuditDockerfileRuntimeAuditDocumentationTest"` passed.
- `mvn test "-Dtest=AgentEvidenceAuditDockerfileRuntimeAuditDocumentationTest,AgentEvidenceAuditMavenDependencyPostureAuditDocumentationTest,AgentEvidenceAuditCodeqlDependencyReviewAuditDocumentationTest,AgentEvidenceAuditCiWorkflowAuditDocumentationTest,AgentEvidenceAuditRepositoryEvidenceMapDocumentationTest,AgentEvidenceAuditOpenPrHygieneDocumentationTest,AgentEvidenceAuditCampaignCloseoutRepairDocumentationTest,AgentGoalCampaignFinalHandoffReportDocumentationTest,AgentGoalCampaignBoardInitializationDocumentationTest,AgentGoalCampaignTemplateArchitectureDocumentationTest,AgentGoalModeLongRunProtocolDocumentationTest,AgentWorkflowQuickstartDocumentationTest,AdvancedReadmeAgentContractDocumentationTest"` failed, the failure was logged in FAILURE_LOG.md, the slot 5 and slot 6 guards were made durable against slot 7 board movement, and the selector bundle rerun passed.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn -q test` passed, then passed again after this checkpoint was updated.
- `mvn -q "-DskipTests" package` passed, then passed again after this checkpoint was updated.
- `mvn -B package` passed with 2,422 tests, 0 failures, 0 errors, and 0 skipped, then passed again after this checkpoint was updated with the same result count.
- `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed, then passed again after this checkpoint was updated.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote only target-local lab evidence, then passed again after this checkpoint was updated.

Remote status: main CI and CodeQL were green for the slot 7 starting main SHA; slot 7 PR not opened yet.

Blocker: none.

Next action: commit, push, and open the slot 7 PR.

Decision: continue

## Current Branch

Name: codex/evidence-audit-dockerfile-runtime

## Current PR

URL: pending

## Current Goal

Short goal: Audit Dockerfile runtime posture without changing Dockerfile contents or Docker behavior.

## Current Head SHA

SHA: `06d800c478b308ef836b0ab01d8b641d8b1a35f0` at branch creation

## What Changed

- Files changed:
- docs/agent/EVIDENCE_AUDIT_DOCKERFILE_RUNTIME_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/FAILURE_LOG.md
- README.md
- docs/REVIEWER_TRUST_MAP.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditDockerfileRuntimeAuditDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditCodeqlDependencyReviewAuditDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditMavenDependencyPostureAuditDocumentationTest.java
- Behavioral surface: none; docs/test-only.
- Documentation surface: records slot 6 as merged/main green and adds the Dockerfile runtime audit.

## Checks Run

- Focused checks: slot 7 guard passed.
- Focused selector bundle: passed after one logged durable-guard repair.
- Dependency checks: `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- Full checks: `mvn -q test` passed.
- Package checks: `mvn -q "-DskipTests" package` and `mvn -B package` passed.
- Diff checks: `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed.
- Smoke checks: `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- Remote checks: main CI and CodeQL green for slot 7 starting SHA; slot 7 PR not opened yet.

## Blockers

- Current blocker: none.
- Owner or next decision: Codex continues inside docs/test-only campaign scope.

## Next Action

One concrete next step: commit, push, and open the slot 7 PR.

## Recovery Notes

- How to resume: confirm branch `codex/evidence-audit-dockerfile-runtime`, inspect `git status`, then commit and open PR if the branch remains docs/test-only.
- Commands already run for slot 7: `git checkout -b codex/evidence-audit-dockerfile-runtime`, `Get-Content Dockerfile`, `Get-Content .github/workflows/ci.yml`, the slot 7 focused guard, the relevant selector bundle, dependency tree, `mvn -q test`, package checks, diff checks, enterprise lab package smoke, and main remote status checks confirming slot 6 main was green.
- Safety boundaries to re-check: docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed: slot 7 PR current-head checks after PR creation; main CI/CodeQL after merge.

## Historical Closeout: LoadBalancerPro Goal Mode 10-PR Trial

- Goal name: LoadBalancerPro Goal Mode 10-PR Trial.
- Current PR slot: completed.
- Result: 10 / 10 PRs merged.
- PR #315 is merged.
- Final PR: [#315](https://github.com/RicheyWorks/LoadBalancerPro/pull/315).
- Final branch: `codex/goal-campaign-final-handoff-report`.
- Final head SHA: `99934cd6f511f535cc70e316a5c8f306fd643745`.
- Final merge SHA: `c27dc5a8da365f9b64ab13e671d9dad07f0f2f01`.
- Slot 9 merged and main green before slot 10 started at `b045b4669ab736cfc0c707fae058ad2e73d7cd20`.
- Slot 10 merged and main green after PR #315.
- Final remote status: main CI and CodeQL green for `c27dc5a8da365f9b64ab13e671d9dad07f0f2f01`.
- Decision: completed; no PR #315 pending state remains active.
