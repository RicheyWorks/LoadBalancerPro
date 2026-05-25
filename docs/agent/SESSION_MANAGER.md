# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), campaign-specific contracts and boards, [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md), and keep the current PR checkpoint factual.

Historical 10-PR trial references remain available through [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md`](GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md), [`GOAL_CAMPAIGN_AGENT_DISCIPLINE.md`](GOAL_CAMPAIGN_AGENT_DISCIPLINE.md), and [`GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md`](GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md), but they are historical closeout records rather than the active campaign pointer.

## Active Campaign Checkpoint

Timestamp: 2026-05-25T02:00-07:00

Goal name: LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign

Current PR slot: 5

Checkpoint: Slot 5 PR #320 opened after local verification

Started from main SHA: `bc62bef7fb5843e2ab143a47a65f81dd6fc46f8f`

Current branch: codex/evidence-audit-codeql-dependency-review

PR URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/320

Head SHA: `d1a4e5463c2acd4c440dc61f30563cd864179747` at initial PR creation; checkpoint commit pending

Changed files:

- docs/agent/EVIDENCE_AUDIT_CODEQL_DEPENDENCY_REVIEW_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- docs/agent/EVIDENCE_AUDIT_CI_WORKFLOW_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/FAILURE_LOG.md
- README.md
- docs/REVIEWER_TRUST_MAP.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditCodeqlDependencyReviewAuditDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditCiWorkflowAuditDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditOpenPrHygieneDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditRepositoryEvidenceMapDocumentationTest.java

Checks run:

- Slot 4 PR #319 merged as `bc62bef7fb5843e2ab143a47a65f81dd6fc46f8f`.
- Slot 4 post-merge local verification passed: focused campaign/agent selector bundle, dependency tree, `mvn -q test`, `mvn -q "-DskipTests" package`, `mvn -B package`, diff checks, and enterprise lab package smoke.
- Slot 4 post-merge main remote checks passed: CI and CodeQL green for `bc62bef7fb5843e2ab143a47a65f81dd6fc46f8f`.
- Slot 5 branch created from clean main.
- Slot 5 CodeQL and dependency-review posture audit completed as documentation/test-only.
- Focused guard `mvn test "-Dtest=AgentEvidenceAuditCodeqlDependencyReviewAuditDocumentationTest"` failed once because the audit doc did not include exact `fail-on-severity: high` wording; logged in FAILURE_LOG.md, fixed in docs, and rerun passed.
- Focused selector bundle failed once because older slot 2 and slot 3 guards depended on moving active-checkpoint wording; logged in FAILURE_LOG.md, fixed by making those guards assert durable merged-slot history, and rerun passed.
- Dependency tree passed: `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"`.
- Full tests passed: `mvn -q test`.
- Skip-tests package passed: `mvn -q "-DskipTests" package`.
- Full package passed: `mvn -B package` with 2,410 tests, 0 failures, 0 errors, and 0 skipped.
- Diff checks passed: `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check`.
- Enterprise lab package smoke passed: `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package`.
- Slot 5 PR opened: [#320](https://github.com/RicheyWorks/LoadBalancerPro/pull/320).

Remote status: main CI and CodeQL were green for the slot 5 starting main SHA; PR #320 remote checks are pending for the current PR head.

Blocker: none.

Next action: commit and push this PR-creation checkpoint, rerun focused guards, then wait for current-head remote checks.

Decision: continue

## Current Branch

Name: codex/evidence-audit-codeql-dependency-review

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/320

## Current Goal

Short goal: Audit CodeQL and Dependency Review posture without changing workflow behavior.

## Current Head SHA

SHA: `d1a4e5463c2acd4c440dc61f30563cd864179747` at initial PR creation; checkpoint commit pending

## What Changed

- Files changed:
- docs/agent/EVIDENCE_AUDIT_CODEQL_DEPENDENCY_REVIEW_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- docs/agent/EVIDENCE_AUDIT_CI_WORKFLOW_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/FAILURE_LOG.md
- README.md
- docs/REVIEWER_TRUST_MAP.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditCodeqlDependencyReviewAuditDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditCiWorkflowAuditDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditOpenPrHygieneDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditRepositoryEvidenceMapDocumentationTest.java
- Behavioral surface: none; docs/test-only.
- Documentation surface: records slot 4 as merged/main green and adds the CodeQL/dependency-review audit.

## Checks Run

- Focused checks: `mvn test "-Dtest=AgentEvidenceAuditCodeqlDependencyReviewAuditDocumentationTest"` failed once on exact dependency-review wording, then passed after the logged repair.
- Focused selector bundle: `mvn test "-Dtest=AgentEvidenceAuditCodeqlDependencyReviewAuditDocumentationTest,AgentEvidenceAuditCiWorkflowAuditDocumentationTest,AgentEvidenceAuditRepositoryEvidenceMapDocumentationTest,AgentEvidenceAuditOpenPrHygieneDocumentationTest,AgentEvidenceAuditCampaignCloseoutRepairDocumentationTest,AgentGoalCampaignFinalHandoffReportDocumentationTest,AgentGoalCampaignBoardInitializationDocumentationTest,AgentGoalCampaignTemplateArchitectureDocumentationTest,AgentGoalModeLongRunProtocolDocumentationTest,AgentWorkflowQuickstartDocumentationTest,AdvancedReadmeAgentContractDocumentationTest"` failed once on moving slot-history guard wording, then passed after the logged guard repair.
- Dependency checks: `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- Full checks: `mvn -q test` passed.
- Package checks: `mvn -q "-DskipTests" package` passed; `mvn -B package` passed with 2,410 tests, 0 failures, 0 errors, and 0 skipped.
- Diff checks: `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed.
- Smoke checks: `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- Remote checks: main CI and CodeQL green for slot 5 starting SHA; PR #320 remote checks pending for the current PR head.

## Blockers

- Current blocker: none.
- Owner or next decision: Codex continues inside docs/test-only campaign scope.

## Next Action

One concrete next step: commit and push this PR-creation checkpoint, rerun focused guards, and wait for current-head remote checks.

## Recovery Notes

- How to resume: confirm branch `codex/evidence-audit-codeql-dependency-review`, inspect `git status`, push the PR-creation checkpoint if it is still local, then inspect PR #320 current-head checks.
- Commands already run for slot 5: `git checkout -b codex/evidence-audit-codeql-dependency-review`, `Get-Content .github/workflows/codeql.yml`, `Get-Content .github/workflows/ci.yml`, main remote status checks confirming slot 4 main was green, the focused guard and selector bundle after repairs, dependency tree, `mvn -q test`, package checks, diff checks, enterprise lab package smoke, `git commit -m "Add evidence audit CodeQL dependency review audit"`, `git push origin codex/evidence-audit-codeql-dependency-review`, and `gh pr create`.
- Safety boundaries to re-check: docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed: slot 5 PR current-head checks after PR creation; main CI/CodeQL after merge.

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
