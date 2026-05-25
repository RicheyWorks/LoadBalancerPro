# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), campaign-specific contracts and boards, [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md), and keep the current PR checkpoint factual.

Historical 10-PR trial references remain available through [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md`](GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md), [`GOAL_CAMPAIGN_AGENT_DISCIPLINE.md`](GOAL_CAMPAIGN_AGENT_DISCIPLINE.md), and [`GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md`](GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md), but they are historical closeout records rather than the active campaign pointer.

## Active Campaign Checkpoint

Timestamp: 2026-05-25T02:30-07:00

Goal name: LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign

Current PR slot: 6

Checkpoint: Slot 6 local verification passed after two logged documentation guard repairs and post-checkpoint reruns

Started from main SHA: `a58d61511d84b8d9013d5a2652dc696fb555e83c`

Current branch: codex/evidence-audit-maven-dependency-posture

PR URL: pending

Head SHA: pending until the local checkpoint commit is created

Changed files:

- docs/agent/EVIDENCE_AUDIT_MAVEN_DEPENDENCY_POSTURE_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/FAILURE_LOG.md
- README.md
- docs/REVIEWER_TRUST_MAP.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditMavenDependencyPostureAuditDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditCodeqlDependencyReviewAuditDocumentationTest.java

Checks run:

- Slot 5 PR #320 merged as `a58d61511d84b8d9013d5a2652dc696fb555e83c`.
- Slot 5 post-merge local verification passed: focused campaign/agent selector bundle, dependency tree, `mvn -q test`, `mvn -q "-DskipTests" package`, `mvn -B package`, diff checks, and enterprise lab package smoke.
- Slot 5 post-merge main remote checks passed: CI and CodeQL green for `a58d61511d84b8d9013d5a2652dc696fb555e83c`.
- Slot 6 branch created from clean main.
- Slot 6 Maven/dependency posture audit started as documentation/test-only.
- `mvn test "-Dtest=AgentEvidenceAuditMavenDependencyPostureAuditDocumentationTest"` failed, the failure was logged in FAILURE_LOG.md, the exact-wording/main-class assertion issue was fixed, and the focused guard rerun passed.
- `mvn test "-Dtest=AgentEvidenceAuditMavenDependencyPostureAuditDocumentationTest,AgentEvidenceAuditCodeqlDependencyReviewAuditDocumentationTest,AgentEvidenceAuditCiWorkflowAuditDocumentationTest,AgentEvidenceAuditRepositoryEvidenceMapDocumentationTest,AgentEvidenceAuditOpenPrHygieneDocumentationTest,AgentEvidenceAuditCampaignCloseoutRepairDocumentationTest,AgentGoalCampaignFinalHandoffReportDocumentationTest,AgentGoalCampaignBoardInitializationDocumentationTest,AgentGoalCampaignTemplateArchitectureDocumentationTest,AgentGoalModeLongRunProtocolDocumentationTest,AgentWorkflowQuickstartDocumentationTest,AdvancedReadmeAgentContractDocumentationTest"` failed, the failure was logged in FAILURE_LOG.md, the slot 5 guard was made durable against slot 6 board movement, and the selector bundle rerun passed.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn -q test` passed.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed with 2,416 tests, 0 failures, 0 errors, and 0 skipped.
- `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote only target-local lab evidence.
- After this checkpoint was updated, the slot 6 focused selector bundle passed again, `mvn -q test` passed again, `mvn -q "-DskipTests" package` passed again, `mvn -B package` passed again with 2,416 tests and no failures/errors/skips, diff checks passed again, and enterprise lab package smoke passed again.

Remote status: main CI and CodeQL were green for the slot 6 starting main SHA; slot 6 PR not opened yet.

Blocker: none.

Next action: commit, push, and open the slot 6 PR.

Decision: continue

## Current Branch

Name: codex/evidence-audit-maven-dependency-posture

## Current PR

URL: pending

## Current Goal

Short goal: Audit Maven/dependency posture without changing Maven configuration or dependencies.

## Current Head SHA

SHA: pending until the local checkpoint commit is created

## What Changed

- Files changed:
- docs/agent/EVIDENCE_AUDIT_MAVEN_DEPENDENCY_POSTURE_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/FAILURE_LOG.md
- README.md
- docs/REVIEWER_TRUST_MAP.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditMavenDependencyPostureAuditDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditCodeqlDependencyReviewAuditDocumentationTest.java
- Behavioral surface: none; docs/test-only.
- Documentation surface: records slot 5 as merged/main green and adds the Maven/dependency posture audit.

## Checks Run

- Focused checks: slot 6 guard passed after one logged repair.
- Focused selector bundle: passed after one logged durable-guard repair and passed again after the checkpoint update.
- Dependency checks: `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- Full checks: `mvn -q test` passed, including the post-checkpoint rerun.
- Package checks: `mvn -q "-DskipTests" package` and `mvn -B package` passed, including the post-checkpoint reruns.
- Diff checks: `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed, including the post-checkpoint reruns.
- Smoke checks: `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed, including the post-checkpoint rerun.
- Remote checks: main CI and CodeQL green for slot 6 starting SHA; slot 6 PR not opened yet.

## Blockers

- Current blocker: none.
- Owner or next decision: Codex continues inside docs/test-only campaign scope.

## Next Action

One concrete next step: commit the slot 6 docs/test-only audit and open PR.

## Recovery Notes

- How to resume: confirm branch `codex/evidence-audit-maven-dependency-posture`, inspect `git status`, rerun the slot 6 focused guard and selector bundle, then commit and open PR if the branch remains docs/test-only.
- Commands already run for slot 6: `git checkout -b codex/evidence-audit-maven-dependency-posture`, `Get-Content pom.xml`, the slot 6 focused guard, the relevant selector bundle, dependency tree, `mvn -q test`, package checks, diff checks, enterprise lab package smoke, and main remote status checks confirming slot 5 main was green.
- Safety boundaries to re-check: docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed: slot 6 PR current-head checks after PR creation; main CI/CodeQL after merge.

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
