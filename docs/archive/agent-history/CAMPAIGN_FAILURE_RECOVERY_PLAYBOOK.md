# Campaign Failure Recovery Playbook

This playbook defines how to handle failures during a multi-PR Codex `/goal` campaign. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this playbook with CAMPAIGN_SYSTEM_INDEX.md, CAMPAIGN_SYSTEM_ARCHITECTURE.md, CAMPAIGN_CHECKPOINT_LEDGER.md, CAMPAIGN_PR_READINESS_CHECKLIST.md, CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md, CAMPAIGN_REMOTE_CHECK_AUDIT.md, CAMPAIGN_MERGE_GATE.md, CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md, CAMPAIGN_CLOSEOUT_PROTOCOL.md, GOAL_MODE_LONG_RUN_PROTOCOL.md, SESSION_MANAGER.md, FAILURE_LOG.md, and VERIFICATION_PROTOCOL.md.

## Purpose

Material failures are not hidden or hand-waved during a campaign. The campaign either recovers inside the current task contract with explicit evidence, or it pauses when the required recovery needs new authority.

The playbook keeps recovery factual:

- identify the failure type;
- preserve the current branch and head SHA;
- log only a material blocker or reusable lesson in FAILURE_LOG.md;
- replace SESSION_MANAGER.md with the genuine blocker and next action;
- decide whether the fix is obvious, safe, and inside scope;
- rerun focused checks first after a fix;
- rerun full local verification before merge;
- refresh remote PR checks before merge;
- refresh main checks after merge before counting the PR.

## Failure Types

Create a repository failure record only when the event:

- exposes a product or security defect;
- invalidates claimed evidence;
- corrupts or risks persistent state;
- requires a non-obvious recovery;
- blocks a mandatory gate; or
- provides a reusable technical lesson.

Ordinary syntax errors, failed searches, unavailable optional local tools, polling, and corrections that changed no persistent state are not failure-log entries.

## Immediate Response

When a failure appears:

1. Stop the campaign loop at the current step.
2. Keep the current branch and working tree intact.
3. If the failure meets the materiality rule, record the blocker or reusable lesson concisely in FAILURE_LOG.md.
4. Replace SESSION_MANAGER.md with the current blocker and next safe action.
5. Do not merge while the failure is unresolved.
6. Do not continue to the next campaign PR while the current PR or main branch is unresolved.

Do not use destructive git commands such as hard reset or branch deletion as a recovery shortcut unless a human explicitly asks for that operation.

## Safe Recovery Path

Recover inside the current branch only when the fix is obvious, minimal, and inside the current PR contract.

After a safe fix:

- rerun the failing focused check first;
- rerun the relevant focused selector bundle;
- update FAILURE_LOG.md only when the recovery adds a reusable lesson or leaves a blocker;
- replace SESSION_MANAGER.md with the recovered current state;
- continue to full local verification only after focused recovery passes.

If the fix requires production code, Maven config, CI/workflow, Dockerfile, Compose behavior, runtime behavior, endpoint behavior, k6 behavior, Bruno behavior, Toxiproxy behavior, scripts, secrets, external/cloud/tenant targets, or automation outside explicit scope, pause instead of continuing.

## Remote Failure Path

If a remote PR check fails, is cancelled, is stale, or remains pending at a merge decision:

- keep run/job detail in the PR check surface; add a concise FAILURE_LOG.md entry only when the gate remains materially blocked or prior evidence is invalid;
- refresh the PR rollup for the current head SHA;
- do not treat a duplicate, skipped-only, stale, queued, in-progress, or pending check as green;
- pause unless the cause is obvious, safe, and recoverable inside the current PR contract.

If main CI/CodeQL is red after merge, preserve the remote run as evidence, put the blocker in current state, and add a concise failure entry because a mandatory gate is blocked.

## Resume Criteria

Resume a paused campaign only when:

- the blocker is resolved;
- the branch or main head SHA is known;
- SESSION_MANAGER.md names the next safe action;
- FAILURE_LOG.md records the result when the event met the materiality rule;
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
