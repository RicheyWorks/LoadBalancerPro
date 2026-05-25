# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), campaign-specific contracts and boards, [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md), and keep the current PR checkpoint factual.

Historical 10-PR trial references remain available through [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md`](GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md), [`GOAL_CAMPAIGN_AGENT_DISCIPLINE.md`](GOAL_CAMPAIGN_AGENT_DISCIPLINE.md), and [`GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md`](GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md), but they are historical closeout records rather than the active campaign pointer.

## Active Campaign Checkpoint

Timestamp: 2026-05-25T04:29-07:00

Goal name: LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign

Current PR slot: 10

Checkpoint: Slot 10 PR opened after full local verification

Started from main SHA: `6f5d0d88502fb86fdc94f5261c709a2356dee65a`

Current branch: codex/evidence-audit-proxy-demo-fixture

PR URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/325

Head SHA: `859209adc8822f3bfb8060c0b516fb61d9e654d4` at PR creation

Changed files:

- README.md
- docs/REVIEWER_TRUST_MAP.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/EVIDENCE_AUDIT_PROXY_DEMO_FIXTURE_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditProxyDemoFixtureAuditDocumentationTest.java

Checks run:

- Slot 9 PR #324 merged.
- Slot 9 branch created from clean main before PR #324.
- Slot 9 final branch head was `ecc0dbca270ff4f6b96c1f41c4ca7c0037569681`.
- Slot 9 merge SHA is `6f5d0d88502fb86fdc94f5261c709a2356dee65a`.
- Slot 9 post-merge local verification passed: focused campaign/agent selector bundle, `mvn -q test`, `mvn -q "-DskipTests" package`, `mvn -B package`, diff checks, and enterprise lab package smoke.
- Slot 9 post-merge main remote checks passed: CI and CodeQL green for `6f5d0d88502fb86fdc94f5261c709a2356dee65a`.
- Slot 10 branch `codex/evidence-audit-proxy-demo-fixture` was created from clean main.
- Slot 10 campaign state repair started as documentation/test-only before proxy demo fixture audit content.
- Slot 10 proxy demo fixture audit doc and read-only documentation guard were added.
- README, Reviewer Trust Map, and repository evidence map now link to the Slot 10 audit.
- Initial Slot 10 focused guard run failed on exact documentation wording and was logged in FAILURE_LOG.md before continuing.
- The Slot 10 audit wording was repaired without behavior changes, and `mvn test "-Dtest=AgentEvidenceAuditProxyDemoFixtureAuditDocumentationTest"` passed on rerun.
- Slot 10 focused selector bundle passed.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn -q test` passed.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed with 2,443 tests, 0 failures, 0 errors, and 0 skipped.
- `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote only target-local lab evidence.
- PR #325 was opened with head `859209adc8822f3bfb8060c0b516fb61d9e654d4`.

Remote status: main CI and CodeQL are green for `6f5d0d88502fb86fdc94f5261c709a2356dee65a`; Slot 10 PR #325 checks are pending for the current head.

Blocker: none.

Next action: commit and push this PR-created checkpoint, rerun focused guards, and wait for current-head PR checks.

Decision: continue

## Current Branch

Name: codex/evidence-audit-proxy-demo-fixture

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/325

## Current Goal

Short goal: Audit proxy demo fixture and demo profiles without changing proxy fixture code, scripts, runtime resources, endpoints, or behavior.

## Current Head SHA

SHA: `859209adc8822f3bfb8060c0b516fb61d9e654d4` at PR creation

## What Changed

- README.md
- docs/REVIEWER_TRUST_MAP.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/EVIDENCE_AUDIT_PROXY_DEMO_FIXTURE_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditProxyDemoFixtureAuditDocumentationTest.java
- Behavioral surface: none; docs/test-only.
- Documentation surface: records slot 9 as merged/main green, advances the active campaign pointer to Slot 10, and adds the proxy demo fixture audit.

## Checks Run

- Focused checks: Slot 9 post-merge focused selector bundle passed before Slot 10 branch creation.
- Slot 10 focused check: first run of `mvn test "-Dtest=AgentEvidenceAuditProxyDemoFixtureAuditDocumentationTest"` failed on exact wording; failure logged; rerun passed.
- Focused selector bundle: passed.
- Dependency checks: `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- Full checks: `mvn -q test` passed.
- Package checks: `mvn -q "-DskipTests" package` and `mvn -B package` passed; verbose package reported 2,443 tests, 0 failures, 0 errors, and 0 skipped.
- Diff checks: `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed.
- Smoke checks: `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- Remote checks: main CI and CodeQL green for Slot 9 merge SHA `6f5d0d88502fb86fdc94f5261c709a2356dee65a`; Slot 10 PR #325 checks pending for current head.

## Blockers

- Current blocker: none.
- Owner or next decision: Codex continues inside docs/test-only campaign scope.

## Next Action

One concrete next step: commit and push this PR-created checkpoint, rerun focused guards, and wait for current-head PR checks.

## Recovery Notes

- How to resume: confirm branch `codex/evidence-audit-proxy-demo-fixture`, inspect `git status`, verify Slot 9 remains recorded as merged/main green, then continue the Slot 10 proxy demo fixture audit.
- Commands already run for Slot 10 start: `git fetch origin`, `git pull --ff-only origin main`, `git checkout -b codex/evidence-audit-proxy-demo-fixture`, `git status --short`, `git rev-parse --abbrev-ref HEAD`, `git rev-parse HEAD`, board/session reads, proxy fixture/profile reads, and source/doc searches for proxy demo surfaces.
- Safety boundaries to re-check: docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed: Slot 10 PR current-head checks after PR creation; main CI/CodeQL after Slot 10 merge.

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
