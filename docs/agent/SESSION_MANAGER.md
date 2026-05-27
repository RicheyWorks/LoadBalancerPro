# Session Manager Template

Use this template during long Codex sessions, handoffs, resumes, and interrupted work.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and update this file at checkpoints. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md), [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md), campaign-specific contracts and boards, [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md), [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md), [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md), [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md), [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md), [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](CAMPAIGN_CLOSEOUT_PROTOCOL.md), and [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md), and keep the current PR checkpoint factual.

Historical 10-PR trial references remain available through [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md`](GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md`](GOAL_CAMPAIGN_REVIEWER_TRUST_NAVIGATION.md), [`GOAL_CAMPAIGN_AGENT_DISCIPLINE.md`](GOAL_CAMPAIGN_AGENT_DISCIPLINE.md), and [`GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md`](GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md), but they are historical closeout records rather than the active campaign pointer.

## Active Campaign Checkpoint

Timestamp: 2026-05-26T22:50-07:00

Goal name: Decision Explorer Implementation Phase 1

Current PR slot: DX-P1-G03

Checkpoint: DX-P1-G03 PR opened after local verification; PR-created metadata checkpoint in progress

Started from main SHA: `755ed394adfa18e462f89312c5289fd3154075f2`

Current branch: codex/decision-explorer-phase1-builder

PR URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/362

Head SHA: `d32cc14b9af4edc1dc2ae420231051946f9f1292` at PR opening; final PR-created checkpoint commit is pending

Changed files planned for this slice:

- src/main/java/com/richmond423/loadbalancerpro/api/DecisionExplorerPayloadService.java
- src/test/java/com/richmond423/loadbalancerpro/api/DecisionExplorerPayloadServiceTest.java
- docs/agent/DECISION_EXPLORER_PHASE1_CAMPAIGN_BOARD.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/FAILURE_LOG.md if repair logging is needed

Checks run:

- PR #360 merged as `0fe9331a757973d93820bbae46b05ae53f8ba64a`; DX-P1-G01 is merged-main-green.
- PR #360 current-head CI passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26491188794.
- PR #360 current-head CodeQL passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26491188818.
- Main CI passed for `0fe9331a757973d93820bbae46b05ae53f8ba64a`:
  https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26491392315.
- Main CodeQL passed for `0fe9331a757973d93820bbae46b05ae53f8ba64a`:
  https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26491392313.
- Branch `codex/decision-explorer-phase1-dto-skeleton` was created from clean main at
  `0fe9331a757973d93820bbae46b05ae53f8ba64a`.
- DX-P1-G02 added the additive `DecisionExplorerPayloadV1`, `DecisionReadoutV1`,
  `CandidateReadoutV1`, `FactorContributionV1`, `PolicyGateReadoutV1`,
  `DecisionDiffReadoutV1`, `EvidencePacketReadoutV1`, `AgentStructuredOutputV1`,
  and `DecisionExplorerDtoSupport` DTO skeletons plus `DecisionExplorerPayloadV1Test`.
- DX-P1-G02 local verification passed before PR #361: focused DTO/phase guard selector, `mvn -q test`,
  `mvn -q "-DskipTests" package`, `mvn -B package` with 2,643 tests and 0 failures, diff checks,
  and `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package`.
- PR #361 was opened: https://github.com/RicheyWorks/LoadBalancerPro/pull/361.
- PR #361 current-head CI passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26492455493.
- PR #361 current-head CodeQL passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26492455473.
- PR #361 Dependency Review passed in the PR CI run.
- PR #361 merged as `fca765b897937cd20ee9955bfb7f9ba7a665a9be`; DX-P1-G02 is merged-main-green.
- Local main was fast-forwarded to `fca765b897937cd20ee9955bfb7f9ba7a665a9be`.
- DX-P1-G02 post-merge local verification on main passed: `mvn -q test`, `mvn -q "-DskipTests" package`,
  `mvn -B package` with 2,643 tests and 0 failures, `git diff --check`, and
  `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package`.
- Main CI passed for `fca765b897937cd20ee9955bfb7f9ba7a665a9be`:
  https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26492690702.
- Main CodeQL passed for `fca765b897937cd20ee9955bfb7f9ba7a665a9be`:
  https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26492690719.
- Branch `codex/decision-explorer-phase1-builder` was created from clean main at
  `fca765b897937cd20ee9955bfb7f9ba7a665a9be`.
- DX-P1-G03 added `DecisionExplorerPayloadService`, a read-only/simulation-only builder that reshapes
  already-built `RoutingComparisonResponse` and `RoutingComparisonResultResponse` evidence into
  `DecisionExplorerPayloadV1` objects without changing routing behavior.
- DX-P1-G03 added `DecisionExplorerPayloadServiceTest` for deterministic result/candidate/factor ordering,
  null and partial evidence handling, returned-evidence-only score/diff treatment, no side effects, and no
  unsupported claim language.
- Focused DX-P1-G03 test passed: `mvn test "-Dtest=DecisionExplorerPayloadServiceTest,DecisionExplorerPayloadV1Test"`
  with 11 tests, 0 failures, 0 errors, and 0 skipped.
- Focused DX-P1-G03 plus phase guard selector initially failed on exact session-manager wording for `PR #360
  merged as`; the failure was logged in `FAILURE_LOG.md`, repaired without changing runtime behavior, and rerun.
- Focused DX-P1-G03 plus phase guard selector rerun passed with 19 tests, 0 failures, 0 errors, and 0 skipped.
- Relevant Decision Explorer selector passed with 82 tests, 0 failures, 0 errors, and 0 skipped.
- `mvn -q test` passed.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed with 2,649 tests, 0 failures, 0 errors, and 0 skipped.
- `git diff --check` passed with line-ending warnings only.
- `git diff --cached --check` passed.
- `git diff --check origin/main...HEAD` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote ignored target-local evidence only.
- Commit `d32cc14b9af4edc1dc2ae420231051946f9f1292` was created for the DX-P1-G03 builder/service slice.
- Branch `codex/decision-explorer-phase1-builder` was pushed to origin.
- PR #362 was opened: https://github.com/RicheyWorks/LoadBalancerPro/pull/362.

Remote status: main CI and CodeQL green for `fca765b897937cd20ee9955bfb7f9ba7a665a9be`; PR #362 checks are pending after PR creation.

Blocker: none.

Next action: commit and push this PR-created checkpoint, rerun current-head local verification as needed, wait for
current-head PR checks, merge only if green, verify post-merge main, then continue to DX-P1-G04.

Decision: continue.

## Historical Evidence Audit Checkpoint

Timestamp: 2026-05-25T11:57-07:00

Goal name: LoadBalancerPro 20-PR Evidence Audit and Closeout Repair Campaign

Current PR slot: 11

Checkpoint: Slot 11 PR #326 opened for review after clean-process local verification recovery; paused before merge or slot advancement

Started from main SHA: `d4a07057c7e0475e012e610a551733184d26791d`

Current branch: codex/evidence-audit-cli-app-startup

PR URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/326

Head SHA: `1634973d761594cb491a42a6a4fb6891ac84cde1` at PR opening; this checkpoint records the PR-opened metadata update before the final metadata commit is pushed

Changed files:

- README.md
- docs/REVIEWER_TRUST_MAP.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/EVIDENCE_AUDIT_CLI_APP_STARTUP_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- docs/agent/FAILURE_LOG.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditCliAppStartupAuditDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditProxyDemoFixtureAuditDocumentationTest.java

Checks run:

- Slot 9 PR #324 merged.
- Slot 9 branch created from clean main before PR #324.
- Slot 9 final branch head was `ecc0dbca270ff4f6b96c1f41c4ca7c0037569681`.
- Slot 9 merge SHA is `6f5d0d88502fb86fdc94f5261c709a2356dee65a`.
- Slot 9 post-merge local verification passed: focused campaign/agent selector bundle, `mvn -q test`, `mvn -q "-DskipTests" package`, `mvn -B package`, diff checks, and enterprise lab package smoke.
- Slot 9 post-merge main remote checks passed: CI and CodeQL green for `6f5d0d88502fb86fdc94f5261c709a2356dee65a`.
- Slot 10 branch `codex/evidence-audit-proxy-demo-fixture` was created from clean main.
- Slot 10 proxy demo fixture audit doc and read-only documentation guard were added.
- Slot 10 final branch head was `4bad0291be2a36ed7695bb47fa3b9a3e63d4dbb0`.
- Slot 10 PR #325 merged as `d4a07057c7e0475e012e610a551733184d26791d`.
- Slot 10 post-merge local verification passed: focused campaign/agent selector bundle, `mvn -q test`, `mvn -q "-DskipTests" package`, `mvn -B package`, diff checks, and enterprise lab package smoke.
- Slot 10 post-merge main remote checks passed: CI and CodeQL green for `d4a07057c7e0475e012e610a551733184d26791d`.
- Slot 11 branch `codex/evidence-audit-cli-app-startup` was created from clean main.
- Slot 11 campaign state repair started as documentation/test-only before CLI mode and app startup audit content.
- Slot 11 CLI app startup audit doc and read-only documentation guard were added.
- README, Reviewer Trust Map, and repository evidence map now link to the Slot 11 audit.
- Initial Slot 11 focused guard runs failed on exact wording and factual coverage assertions and were logged in FAILURE_LOG.md before continuing.
- The Slot 11 audit wording and guard expectations were repaired without changing app code, startup behavior, endpoints, scripts, or runtime resources.
- `mvn test "-Dtest=AgentEvidenceAuditCliAppStartupAuditDocumentationTest"` passed on rerun.
- Slot 11 focused selector bundle passed, including current slot guard, prior audit guards, campaign/agent guards, `LoadBalancerApiApplicationTest`, and CLI command tests.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn -q test` passed.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed with 2,451 tests, 0 failures, 0 errors, and 0 skipped.
- `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote only target-local lab evidence.
- After updating this checkpoint, a final working-tree rerun of `mvn -q test` timed out at the tool boundary and was logged in FAILURE_LOG.md.
- A recovery rerun of `mvn -B test` also timed out at the tool boundary and was logged in FAILURE_LOG.md.
- Stale Maven/Surefire processes from the recovery rerun were stopped; a follow-up process check found no remaining Maven/Java test processes.
- Recovery orientation found branch `codex/evidence-audit-cli-app-startup`, `HEAD`, `main`, and `origin/main` all at `d4a07057c7e0475e012e610a551733184d26791d`.
- Recovery orientation confirmed Slot 11 changes are uncommitted workspace changes and no open PR exists for the branch.
- Recovery clean-process check found no Maven, Surefire, or Java test processes; `jps -lv` reported only the `jps` process itself.
- Recovery focused guard `mvn test "-Dtest=AgentEvidenceAuditCliAppStartupAuditDocumentationTest"` passed.
- Recovery durability guard `mvn test "-Dtest=AgentEvidenceAuditProxyDemoFixtureAuditDocumentationTest"` passed.
- Recovery selector bundle failed once because `AgentGoalCampaignBoardInitializationDocumentationTest` requires the session manager to preserve a `decision: continue` marker while the active Slot 11 checkpoint was still marked `Decision: pause`; the failure is logged in FAILURE_LOG.md.
- Recovery selector bundle rerun passed after recording that Slot 11 is continuing recovery verification only.
- `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- `mvn -q test` passed from the clean process state after the stale process cleanup and recovery checkpoint update.
- `mvn -q "-DskipTests" package` passed.
- `mvn -B package` passed with 2,451 tests, 0 failures, 0 errors, and 0 skipped.
- `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed.
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- Previous timeout failures remain logged as historical failures; this checkpoint records the later clean-process recovery result.
- Commit `1634973d761594cb491a42a6a4fb6891ac84cde1` was created for the recovered Slot 11 docs/test-only work.
- Branch `codex/evidence-audit-cli-app-startup` was pushed to origin.
- PR #326 was opened for review at https://github.com/RicheyWorks/LoadBalancerPro/pull/326.
- Slot 11 is not merged, advanced, or complete.

Remote status: main CI and CodeQL are green for `d4a07057c7e0475e012e610a551733184d26791d`; PR #326 remote checks started after PR creation and were still in progress at this checkpoint.

Blocker: none for local verification after clean-process recovery; slot advancement remains intentionally blocked until PR review, required remote checks, merge, and post-merge main verification.

Next action: wait for PR #326 required remote checks on the final branch head; merge only if fully green, then verify post-merge main before advancing Slot 11.

Decision: pause before merge or Slot 11 advancement.

## Current Branch

Name: codex/evidence-audit-cli-app-startup

## Current PR

URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/326

## Current Goal

Short goal: Audit CLI mode and app startup dispatch without changing CLI code, app startup behavior, runtime resources, endpoints, scripts, or behavior.

## Current Head SHA

SHA: `1634973d761594cb491a42a6a4fb6891ac84cde1` at PR opening; final metadata-only checkpoint commit is pending push

## What Changed

- README.md
- docs/REVIEWER_TRUST_MAP.md
- docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md
- docs/agent/EVIDENCE_AUDIT_CLI_APP_STARTUP_AUDIT.md
- docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md
- docs/agent/FAILURE_LOG.md
- docs/agent/SESSION_MANAGER.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditCliAppStartupAuditDocumentationTest.java
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentEvidenceAuditProxyDemoFixtureAuditDocumentationTest.java
- Behavioral surface: none; docs/test-only.
- Documentation surface: records slot 10 as merged/main green, advances the active campaign pointer to Slot 11, and adds the CLI mode and app startup audit.

## Checks Run

- Slot 10 post-merge focused selector bundle passed before Slot 11 branch creation.
- Slot 10 post-merge `mvn -q test`, `mvn -q "-DskipTests" package`, `mvn -B package`, diff checks, and enterprise lab package smoke passed.
- Remote checks: main CI and CodeQL green for Slot 10 merge SHA `d4a07057c7e0475e012e610a551733184d26791d`.
- Slot 11 focused guard passed after logged wording repairs.
- Slot 11 focused selector bundle passed.
- Dependency checks: `mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed"` passed.
- Preliminary full checks before the final session update: `mvn -q test` passed.
- Preliminary package checks before the final session update: `mvn -q "-DskipTests" package` and `mvn -B package` passed; verbose package reported 2,451 tests, 0 failures, 0 errors, and 0 skipped.
- Preliminary diff checks before the final session update: `git diff --check`, `git diff --check origin/main...HEAD`, and `git diff --cached --check` passed.
- Preliminary smoke checks before the final session update: `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed.
- Final working-tree rerun of `mvn -q test` timed out and was logged.
- Recovery rerun of `mvn -B test` timed out and was logged.
- Recovery process inspection found no Maven/Surefire/Java test processes before rerun.
- Recovery focused Slot 11 guard passed.
- Recovery durability-updated Slot 10 guard passed.
- Recovery selector bundle failed once on stale active-decision wording and was logged.
- Recovery selector bundle rerun passed.
- Recovery full local verification passed once after the clean process state: dependency tree, `mvn -q test`, `mvn -q "-DskipTests" package`, `mvn -B package`, diff checks, and enterprise lab package smoke.
- Final post-update verification passed after the recovery-result checkpoint update: dependency tree, `mvn -q test`, `mvn -q "-DskipTests" package`, `mvn -B package`, diff checks, and enterprise lab package smoke.
- PR #326 was opened after commit `1634973d761594cb491a42a6a4fb6891ac84cde1`; required PR checks were in progress at checkpoint time.

## Blockers

- Current blocker: PR #326 required remote checks are not yet complete; Slot 11 must not be marked merged or complete before PR review, required remote checks, merge, and post-merge main verification.
- Owner or next decision: wait for current-head PR checks, then run a final health pass and merge only if fully green.

## Next Action

One concrete next step: wait for PR #326 required remote checks on the final branch head; do not merge or advance Slot 11 in this turn.

## Recovery Notes

- How to resume: confirm branch `codex/evidence-audit-cli-app-startup`, inspect `git status`, verify no Maven/Java test processes remain, then rerun full local verification before any commit or PR.
- Commands already run for Slot 11 start: `git fetch origin`, `git pull --ff-only origin main`, `git checkout -b codex/evidence-audit-cli-app-startup`, `git status --short`, `git rev-parse --abbrev-ref HEAD`, `git rev-parse HEAD`, board/session reads, CLI command source reads, and source/doc searches for CLI startup surfaces.
- Safety boundaries to re-check: docs/test-only, no production code, no Maven config, no CI/workflow, no Dockerfile, no Compose behavior, no runtime behavior, no endpoints, no k6/Bruno/Toxiproxy behavior, no scripts, no secrets, no external/cloud/tenant targets, no automation, no unsupported claims.
- Remote checks that must be refreshed: none yet because Slot 11 PR was not opened; if resumed, Slot 11 PR current-head checks after PR creation and main CI/CodeQL after Slot 11 merge.

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
