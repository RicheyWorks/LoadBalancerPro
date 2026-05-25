# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), campaign-specific contracts and boards, [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md), and keep the current PR checkpoint factual.

Historical 10-PR trial references remain available through [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md`](GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md), [`GOAL_CAMPAIGN_AGENT_DISCIPLINE.md`](GOAL_CAMPAIGN_AGENT_DISCIPLINE.md), and [`GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md`](GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md), but they are historical closeout records rather than the active campaign pointer.

## Active Campaign Checkpoint

Timestamp: 2026-05-25T03:44-07:00

Goal name: LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign

Current PR slot: 9

Checkpoint: Slot 8 merged and main green; slot 9 branch created

Started from main SHA: `0fc6a5431f400eb4e5f71a70805b3fcb317f1c69`

Current branch: codex/evidence-audit-runtime-config

PR URL: pending

Head SHA: pending

Changed files:

- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/EVIDENCE_AUDIT_RUNTIME_CONFIGURATION_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- README.md
- docs/REVIEWER_TRUST_MAP.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditRuntimeConfigurationAuditDocumentationTest.java

Checks run:

- Slot 8 PR #323 merged as `0fc6a5431f400eb4e5f71a70805b3fcb317f1c69`.
- Slot 8 final branch head was `b1a1c578eca4a11b55a60f2213d45bf48cc28838`.
- Slot 8 post-merge local verification passed: focused campaign/agent selector bundle, `mvn -q test`, `mvn -q "-DskipTests" package`, `mvn -B package`, diff checks, and enterprise lab package smoke.
- Slot 8 post-merge main remote checks passed: CI and CodeQL green for `0fc6a5431f400eb4e5f71a70805b3fcb317f1c69`.
- Slot 9 branch created from clean main.
- Slot 9 runtime configuration audit started as documentation/test-only.

Remote status: main CI and CodeQL were green for the slot 9 starting main SHA.

Blocker: none.

Next action: finish the slot 9 runtime configuration audit, run focused guards, then run full local verification before opening a PR.

Decision: continue

## Current Branch

Name: codex/evidence-audit-runtime-config

## Current PR

URL: pending

## Current Goal

Short goal: Audit runtime configuration without changing runtime resources or behavior.

## Current Head SHA

SHA: pending

## What Changed

- Files changed:
- docs/agent/EVIDENCE_AUDIT_RUNTIME_CONFIGURATION_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- README.md
- docs/REVIEWER_TRUST_MAP.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditRuntimeConfigurationAuditDocumentationTest.java
- Behavioral surface: none; docs/test-only.
- Documentation surface: records slot 8 as merged/main green and adds the runtime configuration audit.

## Checks Run

- Focused checks: pending.
- Focused selector bundle: pending.
- Dependency checks: pending.
- Full checks: pending.
- Package checks: pending.
- Diff checks: pending.
- Smoke checks: pending.
- Remote checks: main CI and CodeQL green for slot 9 starting SHA.

## Blockers

- Current blocker: none.
- Owner or next decision: Codex continues inside docs/test-only campaign scope.

## Next Action

One concrete next step: finish the slot 9 docs/test-only audit and run the focused documentation guard.

## Recovery Notes

- How to resume: confirm branch `codex/evidence-audit-runtime-config`, inspect `git status`, review the runtime configuration audit scope, then run the focused slot 9 guard.
- Commands already run for slot 9: `git checkout -b codex/evidence-audit-runtime-config`, `Get-Content src/main/resources/application.properties`, `Get-Content src/main/resources/application-prod.properties`, and source/doc searches for auth, CORS, telemetry, rate-limit, proxy, LASE, and private-network defaults.
- Safety boundaries to re-check: docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed: slot 9 PR current-head checks after PR creation; main CI/CodeQL after merge.

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
