# Agent Workflow Quickstart

This quickstart explains how to use the repository trust and agent contract files together during Codex sessions. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, external targets, secrets, or broader production claims.

## File Roles

In short: README.md is the Advanced README / public trust surface, AGENTS.md is the Codex/agent operating rules file, BUILD_CONTRACT.md is the current task contract template, GOAL_MODE_LONG_RUN_PROTOCOL.md defines `/goal` long-running session behavior, CAMPAIGN_SYSTEM_ARCHITECTURE.md defines multi-PR campaign execution, CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md defines changed-file, forbidden-scope, claim, and stop-condition auditing, CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md defines factual handoff reports for pauses, resumes, and checkpoints, CAMPAIGN_MERGE_GATE.md defines the final merge decision gate, CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md defines failure recovery and pause/resume rules, VERIFICATION_PROTOCOL.md defines focused-vs-full verification, SESSION_MANAGER.md tracks long-running session state, and FAILURE_LOG.md tracks failures and recovery.

- [`README.md`](../../README.md) is the Advanced README / public trust surface. Use it as the human front door, reviewer starting point, trust-boundary summary, high-level claim contract, and agent-visible context surface.
- [`AGENTS.md`](../../AGENTS.md) is the Codex/agent operating rules file. Use it for scope discipline, guardrail preservation, and honest reporting expectations.
- [`BUILD_CONTRACT.md`](../../BUILD_CONTRACT.md) is the current task contract template. Use it to state the goal, constraints, deliverables, verification requirements, stop conditions, scope boundaries, not-proven boundaries, and final report format.
- [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) explains how `/goal` uses README.md, AGENTS.md, BUILD_CONTRACT.md, and docs/agent files together for multi-hour Codex sessions.
- [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md) explains how a multi-PR goal campaign runs one scoped PR at a time with checkpoints, failure logging, full verification, remote checks, and stop conditions.
- [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md) defines the checkpoint fields for campaign PR count, branch, PR URL, head SHA, checks, blockers, and next action.
- [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](CAMPAIGN_PR_READINESS_CHECKLIST.md) defines the per-PR opening, merge, post-merge, scope, and stop-condition gate for campaign slices.
- [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md) defines changed-file, forbidden-scope, claim, guard-test, and stop-condition auditing for campaign slices.
- [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md) defines the factual handoff format for campaign pauses, resumes, checkpoints, and human review.
- [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](CAMPAIGN_REMOTE_CHECK_AUDIT.md) defines remote PR check and main merge-commit check auditing before a campaign PR can merge or count.
- [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md) defines the final current-head, local verification, remote check, scope, merge method, and post-merge main gate.
- [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) defines how to log, recover from, pause, and resume after local, remote, scope, or tooling failures.
- [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md) defines focused-vs-full verification. Use focused checks while editing and full checks before merge.
- [`SESSION_MANAGER.md`](SESSION_MANAGER.md) tracks long-running session state. Use it for current branch, PR, goal, head SHA, changed files, checks run, blockers, next action, and recovery notes.
- [`FAILURE_LOG.md`](FAILURE_LOG.md) tracks failures and recovery. Use it for local test failures, remote check failures, suspected causes, fixes attempted, results, and follow-up actions.

## Codex Session Flow

1. Read [`README.md`](../../README.md) first to understand the public trust surface and not-proven boundaries.
2. Read [`AGENTS.md`](../../AGENTS.md) for Codex and agent operating rules.
3. Read [`BUILD_CONTRACT.md`](../../BUILD_CONTRACT.md) or the user-provided task contract before editing.
4. Read [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) before starting or resuming long-running `/goal` work.
5. Read [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md) before starting a multi-PR goal campaign.
6. Read [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md) before choosing checks.
7. Use [`SESSION_MANAGER.md`](SESSION_MANAGER.md) for long sessions, interruptions, resumes, or handoffs.
8. Use [`FAILURE_LOG.md`](FAILURE_LOG.md) when a local check, remote check, or scope audit fails.
9. Keep evidence honest: record what changed, what passed, what failed, what remained pending, and what was not verified.

## Verification Rules

- Use focused checks while editing.
- Use relevant focused selector bundles when adjacent docs or guard tests are touched.
- Use full checks before merge.
- Do not claim green main while remote checks are pending.
- Do not accept failed, cancelled, stale required checks, or pending required checks as green.
- Refresh remote PR checks for the current head SHA before merge decisions.
- Refresh main CI/CodeQL for the merge commit before claiming fully green main.

## Claim Boundaries

Preserve not-proven boundaries unless a later separately scoped implementation and verification result explicitly changes them.

Do not overclaim production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, or replay/evidence/report/storage/export proof.

Do not claim replay/evidence/report/storage/export proof unless that behavior is actually implemented and verified in the scoped task.

## Startup Prompt Template

Use or adapt this prompt when starting a Codex session:

```text
Read README.md as the Advanced README / public trust surface.
Read AGENTS.md for Codex/agent operating rules.
Read BUILD_CONTRACT.md or the task-specific contract for scope, deliverables, stop conditions, and final report format.
Read docs/agent/VERIFICATION_PROTOCOL.md before selecting checks.
Follow the requested scope exactly.
Use focused checks while editing and full checks before merge.
Update evidence honestly: report what changed, what passed, what failed, what remains pending, and what was not verified.
Stop if blocked, unsafe, or if the requested work would weaken not-proven boundaries.
```

## Stop And Escalate

Stop and report instead of pushing or merging if:

- scope expands beyond the task contract;
- a change would weaken README trust-boundary wording;
- production behavior, endpoint behavior, Compose behavior, CI/Maven wiring, runtime resources, scripts, secrets, or external/cloud/tenant targets appear outside explicit scope;
- required local checks fail;
- required remote checks fail, are cancelled, are stale, or remain pending;
- the requested wording implies production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation without implementation and verification.
