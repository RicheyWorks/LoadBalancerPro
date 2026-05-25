# Goal Campaign Failure Recovery Examples

This file provides reusable FAILURE_LOG.md examples for the LoadBalancerPro Goal Mode 10-PR Trial. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use these examples with [`FAILURE_LOG.md`](FAILURE_LOG.md), [`SESSION_MANAGER.md`](SESSION_MANAGER.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md`](GOAL_CAMPAIGN_SESSION_CHECKPOINT_EXAMPLES.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md), and [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md).

## Purpose

The campaign needs a factual recovery record whenever a local check, remote check, scope audit, GitHub operation, or merge decision fails. FAILURE_LOG.md is not a blame file; it is the durable record that lets a later Codex session or reviewer understand what broke, what changed, what passed after the fix, and whether the campaign may continue.

Every failure entry should preserve:

- timestamp;
- branch/PR;
- current head SHA or last known good SHA when known;
- failure type;
- failing check, command, run, or job;
- suspected cause;
- fix attempted;
- result;
- recovery status;
- next action.

## When To Log

Update FAILURE_LOG.md before continuing when any of these fail:

- focused documentation guard test;
- relevant focused selector bundle;
- dependency tree check;
- `mvn -q test`;
- `mvn -q "-DskipTests" package`;
- `mvn -B package`;
- `git diff --check`;
- `git diff --check origin/main...HEAD`;
- `git diff --cached --check`;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package`;
- remote PR Build/Test/Package/Smoke;
- remote PR Analyze Java / CodeQL;
- Dependency Review when applicable;
- main post-merge CI or CodeQL;
- scope audit;
- GitHub operation;
- merge decision.

If scope becomes unsafe, GitHub check state is ambiguous, a required check is failed, cancelled, stale, or pending at merge decision time, or a human decision is needed, log the blocker and pause instead of improvising.

## Focused Guard Failure Example

```text
Date/time: 2026-05-24T00:00-07:00

Branch/PR: codex/example-slot / pending

Head SHA: <current branch head>

Failure type: focused documentation guard

Failing check: mvn test "-Dtest=ExampleDocumentationTest"

Suspected cause: The guard expected a durable phrase that the docs did not yet preserve.

Fix attempted: Updated the documentation wording without weakening scope, safety, verification, or not-proven boundaries.

Result: Focused guard rerun passed.

Recovery status: recovered locally; continue to the focused selector bundle.

Follow-up action: Update SESSION_MANAGER.md with the recovered checkpoint and continue verification.
```

## Focused Selector Bundle Failure Example

```text
Date/time: 2026-05-24T00:00-07:00

Branch/PR: codex/example-slot / pending

Head SHA: <current branch head>

Failure type: focused selector bundle

Failing check: mvn test "-Dtest=ExampleDocumentationTest,RelatedDocumentationTest"

Suspected cause: A related guard froze a moving active checkpoint instead of durable slot history.

Fix attempted: Updated the related guard to verify durable history and generic active checkpoint shape.

Result: Focused selector bundle rerun passed.

Recovery status: recovered locally; continue full local verification.

Follow-up action: Keep the failure entry and reference it from SESSION_MANAGER.md.
```

## Full Local Verification Failure Example

```text
Date/time: 2026-05-24T00:00-07:00

Branch/PR: codex/example-slot / pending

Head SHA: <current branch head>

Failure type: full local verification

Failing check: mvn -q test

Suspected cause: A documentation guard expected a cross-link that was not updated.

Fix attempted: Added the missing cross-link and reran the focused guard first.

Result: Focused guard and mvn -q test passed.

Recovery status: recovered locally; continue package checks and diff checks.

Follow-up action: Update SESSION_MANAGER.md with the recovery and continue only inside the current scope.
```

## Diff Or Smoke Failure Example

```text
Date/time: 2026-05-24T00:00-07:00

Branch/PR: codex/example-slot / pending

Head SHA: <current branch head>

Failure type: diff check or enterprise lab package smoke

Failing check: git diff --check origin/main...HEAD

Suspected cause: Trailing whitespace or a formatting artifact in a docs-only edit.

Fix attempted: Removed the whitespace artifact without changing production code or runtime behavior.

Result: Diff check rerun passed.

Recovery status: recovered locally; continue final local verification.

Follow-up action: Keep FAILURE_LOG.md factual and do not claim remote green until current remote checks complete.
```

## Remote PR Check Failure Example

```text
Date/time: 2026-05-24T00:00-07:00

Branch/PR: codex/example-slot / https://github.com/RicheyWorks/LoadBalancerPro/pull/<number>

Head SHA: <current PR head>

Failure type: remote PR check

Failing check: Build/Test/Package/Smoke

Suspected cause: The remote job failed on the current PR head.

Fix attempted: Inspect the run log, make only scoped fixes, rerun focused checks, rerun full local verification, push a new head, and wait for current-head remote checks.

Result: Pending until the new current-head remote checks complete successfully.

Recovery status: not recovered until Build/Test/Package/Smoke, Analyze Java / CodeQL, and Dependency Review where applicable are green.

Follow-up action: Pause if the failure cannot be fixed inside scope or if GitHub check state stays ambiguous.
```

## Main Post-Merge Failure Example

```text
Date/time: 2026-05-24T00:00-07:00

Branch/PR: main after PR #<number>

Head SHA: <merge commit>

Failure type: main post-merge check

Failing check: Analyze Java / CodeQL

Suspected cause: Main is red after merge and the campaign cannot start the next slot.

Fix attempted: Do not open the next campaign PR. Inspect the failing main run and identify the smallest safe recovery path.

Result: Pending human-safe recovery.

Recovery status: paused.

Follow-up action: Update SESSION_MANAGER.md with the blocker and continue only after main CI/CodeQL are green.
```

## Scope Audit Or Merge Decision Failure Example

```text
Date/time: 2026-05-24T00:00-07:00

Branch/PR: codex/example-slot / https://github.com/RicheyWorks/LoadBalancerPro/pull/<number>

Head SHA: <current PR head>

Failure type: scope audit or merge decision

Failing check: scope audit before merge

Suspected cause: A changed file appears outside the allowed docs/test-only scope.

Fix attempted: Stop and inspect whether the unexpected change is required. Do not revert user work without explicit instruction.

Result: Not recovered until scope is safe or the campaign pauses for a human decision.

Recovery status: paused if safe scope cannot be restored inside the current task.

Follow-up action: Update SESSION_MANAGER.md and request human review if the scope boundary is unclear.
```

## Remote Check Rules

Merge only when the latest required checks are green for the current head SHA. Failed, cancelled, stale, pending, or duplicate-only required checks are not acceptable. Do not claim green main while remote checks are pending.

## Stop Conditions

Pause the campaign and log the blocker when:

- main becomes red;
- scope requires production behavior changes;
- scope requires secrets or external/cloud/tenant targets;
- CI/Maven/Docker/Compose/runtime changes appear without explicit scope;
- a required check fails and cannot be fixed safely;
- GitHub check state is ambiguous;
- human approval is needed;
- 10 PRs are merged.

## Not-Proven Boundaries

Failure recovery entries must preserve not-proven boundaries: no production readiness, no production certification, no live-cloud/real-tenant validation, no runtime enforcement, no load/stress/benchmarking or throughput/p95/p99 evidence, no replay/evidence/report/storage/export proof unless implemented and verified, and no broader automation.
