# Goal Campaign Session Checkpoint Examples

These examples show how to update SESSION_MANAGER.md during a bounded multi-PR `/goal` campaign. They are documentation only; they do not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use these examples with [`SESSION_MANAGER.md`](SESSION_MANAGER.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`FAILURE_LOG.md`](FAILURE_LOG.md), and [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md).

## Purpose

SESSION_MANAGER.md should hold the active checkpoint for the current PR slot. It is not a permanent slot history table; durable slot history belongs in GOAL_CAMPAIGN_BOARD.md. The active checkpoint should move forward as each slot moves from branch creation, to edit batch, to local verification, to PR opening, to remote checks, to merge, to post-merge main checks.

## Branch Created Checkpoint

```text
Timestamp: 2026-05-24T20:06-07:00
Goal name: LoadBalancerPro Goal Mode 10-PR Trial
Current PR slot: 4
Checkpoint: Slot 3 merged and main green; slot 4 branch created
Started from main SHA: <main-sha>
Current branch: codex/goal-campaign-session-checkpoint-examples
PR URL: pending
Head SHA: pending slot 4 commit
Changed files:
- docs/agent/GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
Checks run:
- Prior slot merged.
- Main pulled with --ff-only.
- Main CI and CodeQL green for the prior merge commit.
- Branch created from clean main.
Remote status: prior slot post-merge main green; new PR not opened yet.
Blocker: none.
Next action: complete the edit batch and run the focused guard.
Decision: continue
```

## Edit Batch Completed Checkpoint

```text
Checkpoint: Slot 4 edit batch completed; focused verification pending
Changed files:
- docs/agent/GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md
- docs/agent/SESSION_MANAGER.md
- docs/agent/GOAL_CAMPAIGN_BOARD.md
- src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalCampaignSessionCheckpointExamplesDocumentationTest.java
Checks run:
- Scope audit confirmed docs/test-only.
- No production code, Maven config, CI/workflow, Dockerfile, Compose behavior, runtime behavior, endpoints, k6/Bruno/Toxiproxy behavior, scripts, secrets, external/cloud/tenant targets, automation, or unsupported claims.
Remote status: PR not opened yet.
Blocker: none.
Next action: run the focused guard.
Decision: continue
```

## Focused Verification Checkpoint

```text
Checkpoint: Slot 4 focused verification completed
Checks run:
- mvn test "-Dtest=AgentGoalCampaignSessionCheckpointExamplesDocumentationTest" passed.
- Relevant focused selector bundle passed.
Remote status: PR not opened yet.
Blocker: none.
Next action: run full local verification.
Decision: continue
```

## Full Local Verification Checkpoint

```text
Checkpoint: Slot 4 full local verification completed
Checks run:
- mvn -B dependency:tree "-Dincludes=org.apache.tomcat.embed" passed.
- mvn -q test passed.
- mvn -q "-DskipTests" package passed.
- mvn -B package passed.
- git diff --check passed.
- git diff --check origin/main...HEAD passed.
- git diff --cached --check passed.
- .\scripts\smoke\enterprise-lab-workflow.ps1 -Package passed.
Remote status: PR not opened yet.
Blocker: none.
Next action: push the branch and open the PR.
Decision: continue
```

## PR Opened Checkpoint

```text
Checkpoint: Slot 4 PR opened; final-head verification pending
PR URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/<number>
Head SHA: <head-before-pr-opened-checkpoint-commit>
Checks run:
- PR opened.
- Earlier local verification passed.
Remote status: remote PR checks pending for the branch head.
Blocker: none.
Next action: commit and push this PR-opened checkpoint, then rerun final-head local verification.
Decision: continue
```

## Remote Checks Green Checkpoint

```text
Checkpoint: Slot 4 remote checks green
Checks run:
- Build/Test/Package/Smoke passed for the current PR head.
- Analyze Java / CodeQL passed for the current PR head.
- Dependency Review passed where applicable.
Remote status: latest required checks are green.
Blocker: none.
Next action: merge with the normal GitHub merge commit.
Decision: merge
```

## Post-Merge Main Green Checkpoint

```text
Checkpoint: Slot 4 merged and main green
Checks run:
- PR merged with a normal merge commit.
- Main pulled with --ff-only.
- PR head is contained in main.
- Main CI and CodeQL passed for the merge commit.
Remote status: main green.
Blocker: none.
Next action: start the next slot from clean main.
Decision: continue
```

## Pause Checkpoint

```text
Checkpoint: Goal paused
Checks run:
- Last successful focused/local/remote check listed here.
Remote status: explain pending, failed, cancelled, stale, or ambiguous check state.
Blocker: explain the unsafe scope, failed check, red main, human decision, or uncertainty.
Next action: wait for human decision or fix within scope.
Decision: pause
```

## Failure Logging Rule

If any focused check, selector bundle, full verification command, package check, diff check, smoke check, remote check, GitHub operation, scope audit, or merge decision fails, update FAILURE_LOG.md before continuing. The SESSION_MANAGER.md checkpoint should reference the failure and recovery status.

## Remote Check Rules

Do not claim green main while remote checks are pending. Do not accept failed, cancelled, stale, pending, or duplicate-only required checks as green. Merge only when latest required checks are green for the current head SHA.

## Not-Proven Boundaries

These checkpoint examples do not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof unless implemented and verified, or broader automation.
