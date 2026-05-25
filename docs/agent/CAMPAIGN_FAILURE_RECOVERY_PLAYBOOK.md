# Campaign Failure Recovery Playbook

This playbook defines how to handle failures during a multi-PR Codex `/goal` campaign. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this playbook with CAMPAIGN_SYSTEM_ARCHITECTURE.md, CAMPAIGN_CHECKPOINT_LEDGER.md, CAMPAIGN_PR_READINESS_CHECKLIST.md, CAMPAIGN_REMOTE_CHECK_AUDIT.md, CAMPAIGN_MERGE_GATE.md, GOAL_MODE_LONG_RUN_PROTOCOL.md, SESSION_MANAGER.md, FAILURE_LOG.md, and VERIFICATION_PROTOCOL.md.

## Purpose

Failures are not hidden or hand-waved during a campaign. A failure can be local, remote, procedural, or scope-related. The campaign either recovers inside the current task contract with explicit evidence, or it pauses for a human decision.

The playbook keeps recovery factual:

- identify the failure type;
- preserve the current branch and head SHA;
- log the failure in FAILURE_LOG.md;
- update SESSION_MANAGER.md with blocker, last known good state, checks run, and next action;
- decide whether the fix is obvious, safe, and inside scope;
- rerun focused checks first after a fix;
- rerun full local verification before merge;
- refresh remote PR checks before merge;
- refresh main checks after merge before counting the PR.

## Failure Types

Record the specific failure type:

- focused documentation guard failure;
- relevant focused selector bundle failure;
- `mvn -q test` failure;
- package check failure;
- diff or whitespace check failure;
- scope audit failure;
- enterprise lab package smoke failure;
- remote PR check failure;
- remote PR check cancellation;
- stale remote check;
- pending remote check at merge decision;
- post-merge main CI/CodeQL failure;
- tooling command failure;
- mergeability or branch state failure;
- unsafe scope expansion;
- human decision needed.

## Immediate Response

When a failure appears:

1. Stop the campaign loop at the current step.
2. Keep the current branch and working tree intact.
3. Record the branch, PR, head SHA, failing command or job, suspected cause, fix attempted, result, and follow-up action in FAILURE_LOG.md.
4. Update SESSION_MANAGER.md with the current blocker, last known good SHA, checks already run, and the next safe action.
5. Do not merge while the failure is unresolved.
6. Do not continue to the next campaign PR while the current PR or main branch is unresolved.

Do not use destructive git commands such as hard reset or branch deletion as a recovery shortcut unless a human explicitly asks for that operation.

## Safe Recovery Path

Recover inside the current branch only when the fix is obvious, minimal, and inside the current PR contract.

After a safe fix:

- rerun the failing focused check first;
- rerun the relevant focused selector bundle;
- update FAILURE_LOG.md with the result;
- update SESSION_MANAGER.md with the recovery checkpoint;
- continue to full local verification only after focused recovery passes.

If the fix requires production code, Maven config, CI/workflow, Dockerfile, Compose behavior, runtime behavior, endpoint behavior, k6 behavior, Bruno behavior, Toxiproxy behavior, scripts, secrets, external/cloud/tenant targets, or automation outside explicit scope, pause instead of continuing.

## Remote Failure Path

If a remote PR check fails, is cancelled, is stale, or remains pending at a merge decision:

- record the run id, job name, status, conclusion, branch, PR head SHA, and suspected cause in FAILURE_LOG.md;
- refresh the PR rollup for the current head SHA;
- do not treat a duplicate, skipped-only, stale, queued, in-progress, or pending check as green;
- pause unless the cause is obvious, safe, and recoverable inside the current PR contract.

If main CI/CodeQL is red after merge, record the merge commit, failing run, failing job, local main state, and last known good main SHA before pausing.

## Resume Criteria

Resume a paused campaign only when:

- the blocker is resolved;
- the branch or main head SHA is known;
- SESSION_MANAGER.md names the next safe action;
- FAILURE_LOG.md records the failure result;
- scope still matches BUILD_CONTRACT.md and the current PR contract;
- focused checks are ready to rerun;
- main is green if the next step starts a new PR.

Use `/goal resume` only after those conditions are true.

## Stop Conditions

Pause instead of improvising when:

- scope is unclear or expands beyond the active PR contract;
- README.md, AGENTS.md, BUILD_CONTRACT.md, reviewer trust boundaries, or not-proven boundaries would be weakened;
- required checks fail and the fix is not obvious and safe;
- required checks are cancelled, stale, queued, in-progress, pending, skipped-only, or duplicate-only at a merge decision;
- main CI/CodeQL is red;
- a human decision is needed.

## Not-Proven Boundaries

Recovering from a campaign failure does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation.
