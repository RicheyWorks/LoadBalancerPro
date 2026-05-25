# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), campaign-specific contracts and boards, [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md), and keep the current PR checkpoint factual.

Historical 10-PR trial references remain available through [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md`](GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md), [`GOAL_CAMPAIGN_AGENT_DISCIPLINE.md`](GOAL_CAMPAIGN_AGENT_DISCIPLINE.md), and [`GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md`](GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md), but they are historical closeout records rather than the active campaign pointer.

## Active Campaign Checkpoint

Timestamp: 2026-05-25T00:04-07:00

Goal name: LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign

Current PR slot: 2

Checkpoint: Slot 2 local verification passed; PR creation pending

Started from main SHA: `4622d788569fc68de1fab212cdad388d2cf10dc8`

Current branch: codex/evidence-audit-open-pr-hygiene

PR URL: pending

Head SHA: working tree before slot 2 commit

Changed files:

- docs/agent/EVIDENCE_AUDIT_OPEN_PR_HYGIENE.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
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

Remote status: main CI and CodeQL green for the starting main SHA; slot 2 PR not opened yet.

Blocker: none.

Next action: commit and push slot 2, open the slot 2 PR, then audit current-head remote checks.

Decision: continue

## Current Branch

Name: codex/evidence-audit-open-pr-hygiene

## Current PR

URL: pending

## Current Goal

Short goal: Audit open PR hygiene, especially PR #291, without closing or modifying unrelated PRs.

## Current Head SHA

SHA: working tree verified before slot 2 commit

## What Changed

- Files changed:
- docs/agent/EVIDENCE_AUDIT_OPEN_PR_HYGIENE.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditOpenPrHygieneDocumentationTest.java
- Behavioral surface: none; docs/test-only.
- Documentation surface: documents open PR hygiene findings and records slot 1 as merged/main green.

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
- Remote checks: slot 2 PR not opened yet.

## Blockers

- Current blocker: none.
- Owner or next decision: Codex continues inside docs/test-only campaign scope.

## Next Action

One concrete next step: commit and push slot 2, then open the slot 2 PR.

## Recovery Notes

- How to resume: confirm branch `codex/evidence-audit-open-pr-hygiene`, inspect `git status`, then run the slot 2 focused guard.
- Commands already run for slot 2: `git status`, `git rev-parse HEAD`, `gh run list --branch main`, `git checkout -b codex/evidence-audit-open-pr-hygiene`, `gh pr list --state open`, `gh pr view 291`, and PR #291 diff inspection.
- Safety boundaries to re-check: docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed: slot 2 PR current-head checks after PR creation; main CI/CodeQL after merge.

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
