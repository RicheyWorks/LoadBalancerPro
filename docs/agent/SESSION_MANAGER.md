# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), campaign-specific contracts and boards, [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md), and keep the current PR checkpoint factual.

Historical 10-PR trial references remain available through [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md`](GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md), [`GOAL_CAMPAIGN_AGENT_DISCIPLINE.md`](GOAL_CAMPAIGN_AGENT_DISCIPLINE.md), and [`GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md`](GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md), but they are historical closeout records rather than the active campaign pointer.

## Active Campaign Checkpoint

Timestamp: 2026-05-25T00:43-07:00

Goal name: LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign

Current PR slot: 3

Checkpoint: Slot 3 final-head local verification passed after logged EOF whitespace repair; checkpoint commit and remote checks pending

Started from main SHA: `7dd64becaefd589ff94ed2fea93b017397b4a747`

Current branch: codex/evidence-audit-repository-map

PR URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/318

Head SHA: `b8eaa5919a7b34b39e760dcab326caa24c034416` at PR creation; final checkpoint commit pending push

Changed files:

- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- docs/agent/EVIDENCE_AUDIT_OPEN_PR_HYGIENE.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- README.md
- docs/REVIEWER_TRUST_MAP.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditRepositoryEvidenceMapDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditOpenPrHygieneDocumentationTest.java

Checks run:

- Slot 1 PR #316 merged as `4622d788569fc68de1fab212cdad388d2cf10dc8`.
- Post-merge main verification for slot 1 passed locally.
- Main CI and CodeQL were green for `4622d788569fc68de1fab212cdad388d2cf10dc8`.
- Slot 2 branch created from clean main.
- Created slot 2 branch from clean main.
- Audited open PRs with `gh pr list --state open --json number,title,headRefName,headRefOid,baseRefName,isDraft,mergeable,mergeStateStatus,updatedAt,url,author`.
- Verified PR #291 is open, non-draft, `DIRTY` / `CONFLICTING`, and unchanged by this audit.
- Completed slot 2 documentation/test audit batch.
- `mvn test "-Dtest=AgentEvidenceAuditOpenPrHygieneDocumentationTest"` failed once for missing exact `documentation/test-only` wording, was logged in FAILURE_LOG.md, and passed after the wording fix.
- Relevant campaign/agent selector bundle failed once because a slot 1 guard froze the active board to slot 1, was logged in FAILURE_LOG.md, and passed after the guard was made durable.
- `mvn test "-Dtest=AgentEvidenceAuditOpenPrHygieneDocumentationTest,AgentEvidenceAuditCampaignCloseoutRepairDocumentationTest,AgentGoalCampaignFinalHandoffReportDocumentationTest,AgentGoalCampaignBoardInitializationDocumentationTest,AgentGoalCampaignTemplateArchitectureDocumentationTest,AgentGoalModeLongRunProtocolDocumentationTest,AgentWorkflowQuickstartDocumentationTest,AdvancedReadmeAgentContractDocumentationTest"` passed.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn -q test` passed with 2,392 tests.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed with 2,392 tests.
- `git diff --check` passed with line-ending warnings only.
- `git diff --check origin/main...HEAD` passed.
- `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- Slot 2 commit `c53ad0a799fb8d14ebe92df2f6b40fb2bcd1d71c` pushed.
- PR #317 opened.
- Slot 2 final checkpoint commit `08e3320e6b5413d372249b7886876341af1529e6` pushed.
- PR #317 current-head remote checks passed: Build/Test/Package/Smoke, Analyze Java / CodeQL, and Dependency Review where applicable.
- PR #317 merged as `7dd64becaefd589ff94ed2fea93b017397b4a747`.
- Slot 2 post-merge local verification passed: dependency tree, focused guard, campaign/agent selector bundle, `mvn -q test`, `mvn -q "-DskipTests" package`, `mvn -B package`, diff checks, and enterprise lab package smoke.
- Slot 2 post-merge main remote checks passed: CI and CodeQL green for `7dd64becaefd589ff94ed2fea93b017397b4a747`.
- Slot 3 branch created from clean main.
- Slot 3 repository evidence map added as documentation/test-only.
- `mvn test "-Dtest=AgentEvidenceAuditRepositoryEvidenceMapDocumentationTest"` passed.
- `mvn test "-Dtest=AgentEvidenceAuditRepositoryEvidenceMapDocumentationTest,AgentEvidenceAuditOpenPrHygieneDocumentationTest,AgentEvidenceAuditCampaignCloseoutRepairDocumentationTest,AgentGoalCampaignFinalHandoffReportDocumentationTest,AgentGoalCampaignBoardInitializationDocumentationTest,AgentGoalCampaignTemplateArchitectureDocumentationTest,AgentGoalModeLongRunProtocolDocumentationTest,AgentWorkflowQuickstartDocumentationTest,AdvancedReadmeAgentContractDocumentationTest"` passed.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn -q test` passed with 2,398 tests.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed with 2,398 tests.
- `git diff --check` passed with line-ending warnings only.
- `git diff --check origin/main...HEAD` passed.
- `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- Slot 3 commit `b8eaa5919a7b34b39e760dcab326caa24c034416` pushed.
- PR #318 opened.
- `git diff --check origin/main...HEAD` reported an extra blank line at EOF in the slot 3 evidence map doc and guard test because the repair was not yet committed; this was logged in FAILURE_LOG.md before continuing.
- EOF whitespace repair applied to `docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md` and `src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditRepositoryEvidenceMapDocumentationTest.java`.
- Final-head focused guard rerun passed.
- Final-head campaign/agent selector bundle rerun passed.
- Final-head `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- Final-head `mvn -q test` passed.
- Final-head `mvn -q "-DskipTests" package` passed.
- Final-head `mvn -B package` passed.
- Final-head `git diff --check` passed.
- Final-head `git diff --cached --check` passed.
- Final-head `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- Final-head `git diff --check origin/main...HEAD` must be rerun after the checkpoint commit includes the EOF repair.

Remote status: PR #318 checks pending final checkpoint push; main CI and CodeQL were green for the starting main SHA.

Blocker: none.

Next action: push the final checkpoint commit, rerun `git diff --check origin/main...HEAD` for the committed repair, then audit PR #318 current-head remote checks.

Decision: continue

## Current Branch

Name: codex/evidence-audit-repository-map

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/318

## Current Goal

Short goal: Map repository evidence surfaces across README, trust docs, CI, CodeQL, Docker, Compose, smoke scripts, runtime config, local-lab, and campaign docs.

## Current Head SHA

SHA: `b8eaa5919a7b34b39e760dcab326caa24c034416` at PR creation; final checkpoint commit pending push

## What Changed

- Files changed:
- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- docs/agent/EVIDENCE_AUDIT_OPEN_PR_HYGIENE.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- README.md
- docs/REVIEWER_TRUST_MAP.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditRepositoryEvidenceMapDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditOpenPrHygieneDocumentationTest.java
- Behavioral surface: none; docs/test-only.
- Documentation surface: records slot 2 as merged/main green and adds the repository evidence map.

## Checks Run

- Focused checks:
- `mvn test "-Dtest=AgentEvidenceAuditOpenPrHygieneDocumentationTest"` passed after one logged wording repair.
- Focused selector bundle:
- Relevant campaign/agent selector bundle passed after one logged guard durability repair.
- Dependency checks:
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- Full checks:
- `mvn -q test` passed with 2,392 tests.
- Package checks:
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed with 2,392 tests.
- Diff checks:
- `git diff --check` passed with line-ending warnings only.
- `git diff --check origin/main...HEAD` passed.
- `git diff --cached --check` passed.
- Smoke checks:
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- Remote checks: PR #318 checks pending final checkpoint push.

## Blockers

- Current blocker: none.
- Owner or next decision: Codex continues inside docs/test-only campaign scope.

## Next Action

One concrete next step: push the final checkpoint commit and audit PR #318 current-head remote checks.

## Recovery Notes

- How to resume: confirm branch `codex/evidence-audit-repository-map`, inspect `git status`, push the final slot 3 checkpoint commit if not yet pushed, and audit PR #318 current-head remote checks.
- Commands already run for slot 3: `git status`, `gh run list --branch main`, `git checkout -b codex/evidence-audit-repository-map`, focused guard, relevant selector bundle, dependency tree, full tests, package checks, diff checks, enterprise lab package smoke, `git push origin codex/evidence-audit-repository-map`, and `gh pr create`.
- Safety boundaries to re-check: docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed: slot 3 PR current-head checks after PR creation; main CI/CodeQL after merge.

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
