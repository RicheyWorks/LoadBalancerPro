# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), campaign-specific contracts and boards, [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md), and keep the current PR checkpoint factual.

Historical 10-PR trial references remain available through [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md`](GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md), [`GOAL_CAMPAIGN_AGENT_DISCIPLINE.md`](GOAL_CAMPAIGN_AGENT_DISCIPLINE.md), and [`GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md`](GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md), but they are historical closeout records rather than the active campaign pointer.

## Active Campaign Checkpoint

Timestamp: 2026-05-24T23:18-07:00

Goal name: LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign

Current PR slot: 1

Checkpoint: Slot 1 PR opened; final-head verification pending

Started from main SHA: `c27dc5a8da365f9b64ab13e671d9dad07f0f2f01`

Current branch: codex/evidence-audit-closeout-repair

PR URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/316

Head SHA: `001bc2056c348fba26b8cad38b9855bf92e33866` at PR creation; final checkpoint head pending verification

Changed files:

- README.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_CONTRACT.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_CHECKPOINT_TEMPLATE.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_FINAL_REPORT_TEMPLATE.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditCampaignCloseoutRepairDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignFinalHandoffReportDocumentationTest.java

Checks run:

- Read README.md, AGENTS.md, BUILD_CONTRACT.md, AGENT_WORKFLOW_QUICKSTART.md, GOAL_MODE_LONG_RUN_PROTOCOL.md, VERIFICATION_PROTOCOL.md, SESSION_MANAGER.md, and FAILURE_LOG.md.
- Fetched origin and confirmed local main was clean.
- Verified PR #315 is merged.
- Verified PR #315 title is "Add goal campaign final handoff report."
- Verified PR #315 final head SHA is `99934cd6f511f535cc70e316a5c8f306fd643745`.
- Verified PR #315 merge commit is `c27dc5a8da365f9b64ab13e671d9dad07f0f2f01`.
- Verified main CI and CodeQL were green for `c27dc5a8da365f9b64ab13e671d9dad07f0f2f01`.
- Verified PR #291 is open, conflicting, and unrelated to slot 1; no changes were made to it.
- Created slot 1 branch from clean main.
- Completed slot 1 documentation/test repair batch.
- `mvn test "-Dtest=AgentEvidenceAuditCampaignCloseoutRepairDocumentationTest"` passed.
- Focused campaign/agent selector bundle passed after logged wording-drift repairs.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn -q test` passed with 2,387 tests.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed with 2,387 tests.
- `git diff --check` passed with line-ending warnings only.
- `git diff --check origin/main...HEAD` passed.
- `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- Slot 1 commit `001bc2056c348fba26b8cad38b9855bf92e33866` pushed.
- PR #316 opened.

Remote status: PR #316 checks pending or not yet audited; main CI and CodeQL were green for the starting main SHA.

Blocker: none.

Next action: run final-head verification for the PR-created checkpoint, push the checkpoint commit, then audit PR #316 current-head remote checks.

Decision: continue

## Current Branch

Name: codex/evidence-audit-closeout-repair

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/316

## Current Goal

Short goal: Repair stale post-merge state for the completed 10-PR trial and initialize the 20-PR evidence audit campaign controls.

## Current Head SHA

SHA: `001bc2056c348fba26b8cad38b9855bf92e33866` at PR creation; final checkpoint head pending verification

## What Changed

- Files changed:
- README.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_CONTRACT.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_CHECKPOINT_TEMPLATE.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_FINAL_REPORT_TEMPLATE.md
- docs/agent/FAILURE_LOG.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- docs/agent/GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditCampaignCloseoutRepairDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignFinalHandoffReportDocumentationTest.java
- Behavioral surface: none; docs/test-only.
- Documentation surface: repairs the completed 10-PR campaign state and starts the new 20-slot audit campaign.

## Checks Run

- Focused checks:
- `mvn test "-Dtest=AgentEvidenceAuditCampaignCloseoutRepairDocumentationTest"` passed.
- Focused selector bundle:
- Campaign/agent selector bundle passed after logged wording-drift repairs.
- Dependency checks:
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- Full checks:
- `mvn -q test` passed with 2,387 tests.
- Package checks:
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed with 2,387 tests.
- Diff checks:
- `git diff --check` passed with line-ending warnings only.
- `git diff --check origin/main...HEAD` passed.
- `git diff --cached --check` passed.
- Smoke checks:
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- Remote checks: PR #316 checks pending or not yet audited.

## Blockers

- Current blocker: none.
- Owner or next decision: Codex continues inside docs/test-only campaign scope.

## Next Action

One concrete next step: rerun final-head verification for the PR-created checkpoint.

## Recovery Notes

- How to resume: confirm branch `codex/evidence-audit-closeout-repair`, inspect `git status`, then run the slot 1 focused guard and final-head verification.
- Commands already run: read required source files, `git fetch origin`, `gh pr view 315`, `gh pr view 291`, `gh run list --branch main`, `git checkout main`, `git pull --ff-only origin main`, and `git checkout -b codex/evidence-audit-closeout-repair`.
- Safety boundaries to re-check: docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed: PR current-head checks after PR creation; main CI/CodeQL after merge.

## Historical Closeout: LoadBalancerPro Goal Mode 10-PR Trial

- Goal name: LoadBalancerPro Goal Mode 10-PR Trial.
- Current PR slot: completed.
- Result: 10 / 10 PRs merged.
- Final PR: [#315](https://github.com/RicheyWorks/LoadBalancerPro/pull/315).
- Final branch: `codex/goal-campaign-final-handoff-report`.
- Final head SHA: `99934cd6f511f535cc70e316a5c8f306fd643745`.
- Final merge SHA: `c27dc5a8da365f9b64ab13e671d9dad07f0f2f01`.
- Slot 9 merged and main green before slot 10 started at `b045b4669ab736cfc0c707fae058ad2e73d7cd20`.
- Slot 10 merged and main green after PR #315.
- Final remote status: main CI and CodeQL green for `c27dc5a8da365f9b64ab13e671d9dad07f0f2f01`.
- Decision: completed; no PR #315 pending state remains active.
