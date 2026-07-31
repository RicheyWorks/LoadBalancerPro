# Goal Mode Long-Run Protocol

This protocol explains how to use `/goal` for long-running Codex work with one durable objective. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, external targets, secrets, or broader production claims.

README.md remains the constitutional layer and public trust surface. It is not the only runtime context for long sessions. AGENTS.md is the always-read operating rules file, BUILD_CONTRACT.md is the focused execution contract, and the docs/agent files are the operational scaffolding that keep multi-hour work bounded, resumable, and honest.

## File Roles

- [`../../README.md`](../../README.md) is the Advanced README, constitutional layer, public trust surface, reviewer starting point, trust-boundary summary, and high-level claim contract.
- [`../../AGENTS.md`](../../AGENTS.md) is the always-read operating rules file for Codex and other repository agents.
- [`../../BUILD_CONTRACT.md`](../../BUILD_CONTRACT.md) is the focused execution contract for the current task.
- [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md) is the startup path that ties the files together for normal Codex sessions.
- [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md) is the navigation layer for multi-PR campaign control docs, merge/counting rules, and closeout flow.
- [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md) defines how multi-PR goal campaigns run one scoped PR at a time with concise current state, verification, remote checks, and stop conditions.
- [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md) defines the minimal campaign-state fields.
- [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md) defines the campaign PR opening, merge, post-merge, scope, and stop-condition checklist.
- [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md) defines focused checks, relevant selector bundles, full checks, remote checks, post-merge checks, and merge readiness.
- [`SESSION_MANAGER.md`](SESSION_MANAGER.md) tracks only the active slot, branch/PR, exact-head source, completed gates, genuine blocker, and next action.
- [`FAILURE_LOG.md`](FAILURE_LOG.md) records unresolved material blockers and reusable lessons.

## When To Use Goal Mode

Use `/plan` before `/goal` when the work needs shaping, sequencing, or scope negotiation. Use `/goal` when the task already has one durable objective and enough boundaries to keep the session moving without reinterpreting the mission every few minutes.

Goal text should be short and durable. Detailed instructions belong in BUILD_CONTRACT.md and the docs/agent files. Do not put the entire implementation plan, verification matrix, or safety policy into the `/goal` text.

Use `/goal` to inspect the active goal and decide whether the work should continue, pause, resume, or clear.

## Goal Mode Commands

- `/goal`: inspect the active goal, current checkpoint, current branch, current head SHA, changed files, checks already run, next verification step, blockers, and whether the goal should continue, pause, or clear.
- `/goal pause`: pause when checks fail, scope is unclear, safety is uncertain, remote checks are blocked, or a human decision is needed.
- `/goal resume`: resume only after the blocker is resolved and the next step is still inside BUILD_CONTRACT.md scope.
- `/goal clear`: clear when the objective is done, explicitly abandoned, or replaced.

Do not use `/goal` as permission to ignore scope. Do not use `/goal` as permission to skip verification. Do not use `/goal` as permission to keep going through unsafe changes.

## Long-Run Checkpoints

Keep SESSION_MANAGER.md as a concise current-state snapshot. Replace stale state when the active slot, branch/PR, completed gate set, genuine blocker, or next action changes. Do not append command history, polling, repeated test totals, or head-by-head narration.

Use FAILURE_LOG.md only for product/security defects, invalidated evidence, persistent-state risk, non-obvious recovery, mandatory-gate blocks, or reusable technical lessons. Ordinary syntax errors, failed searches, optional local-tool absence, and polling are not repository records.

## Verification Rules

Use focused checks while editing. Use full verification before merge. Remote checks must be green before claiming green main. Do not claim green main while remote checks are pending. Do not accept failed, cancelled, stale, or pending required checks.

Use VERIFICATION_PROTOCOL.md for the normal escalation path:

- focused failing test or focused documentation guard;
- relevant focused selector bundle;
- `mvn -q test`;
- package checks;
- diff checks;
- enterprise lab package smoke when requested;
- current-head remote PR checks;
- merge-commit main CI/CodeQL checks after merge.

Human review is still required before merge. Goal mode may keep work organized, but it does not create approval to merge unsafe scope, failed checks, stale checks, or unsupported claims.

## Preserve Boundaries

Preserve README / AGENTS / BUILD_CONTRACT boundaries. Preserve not-proven boundaries. Keep no production readiness/certification, no live-cloud or real-tenant validation, no runtime enforcement, no load/stress/benchmarking or throughput/p95/p99 evidence, and no replay/evidence/report/storage/export proof unless actually implemented and verified in the scoped task.

## Goal-mode starter prompt

```text
/goal Work through BUILD_CONTRACT.md for this branch. Follow README.md, AGENTS.md, docs/agent/VERIFICATION_PROTOCOL.md, and docs/agent/GOAL_MODE_LONG_RUN_PROTOCOL.md. Keep scope bounded, keep docs/agent/SESSION_MANAGER.md concise, record only material failures in docs/agent/FAILURE_LOG.md, use focused checks while editing, and do not claim completion until required local verification and remote checks are green.
```

## Goal-mode status prompt

```text
/goal
```

Then ask Codex:

```text
Report current checkpoint, current branch, current head SHA, changed files, checks already run, next verification step, blockers, and whether the goal should continue, pause, or clear.
```

## Pause Prompt

```text
/goal pause
```

Pause because the run is materially blocked or scope is unsafe. Put the genuine blocker and next action in docs/agent/SESSION_MANAGER.md; add docs/agent/FAILURE_LOG.md only when the failure meets its materiality rule.

## Resume Prompt

```text
/goal resume
```

Resume only after confirming the blocker is resolved and the next step is still inside BUILD_CONTRACT.md scope.

## Clear Prompt

```text
/goal clear
```

Clear only after the PR is merged or the goal is explicitly abandoned.

## How Long Can This Run?

Goal mode may run for many hours if the task has clear scope, checkpoints, and verification.

Longer runs require concise current state and strict stop conditions. Independent human review is advisory when an active goal explicitly authorizes exact-head green automatic merge. The goal should pause rather than improvise when scope, safety, or verification is unclear.

## Stop Conditions

Pause or stop instead of continuing when:

- scope expands beyond BUILD_CONTRACT.md;
- README, AGENTS.md, or BUILD_CONTRACT.md boundaries would be weakened;
- production behavior, endpoint behavior, Compose behavior, CI/Maven wiring, runtime resources, scripts, secrets, external/cloud/tenant targets, or automation appear outside explicit scope;
- focused checks fail and the cause is unclear;
- full verification fails;
- remote checks are failed, cancelled, stale, or pending;
- a human decision is needed to choose between safety and scope.
