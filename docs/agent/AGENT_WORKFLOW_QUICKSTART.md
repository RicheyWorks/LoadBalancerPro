# Agent Workflow Quickstart

This quickstart explains how to use the repository trust and agent contract files together during Codex sessions. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, external targets, secrets, or broader production claims.

## File Roles

In short: README.md is the operator-facing public trust surface, AGENTS.md is the Codex/agent operating rules file, BUILD_CONTRACT.md is the current task contract template, GOAL_MODE_LONG_RUN_PROTOCOL.md defines `/goal` behavior, VERIFICATION_PROTOCOL.md defines focused-vs-full verification, SESSION_MANAGER.md holds only current execution state, and FAILURE_LOG.md holds only material blockers and reusable lessons.

- [`README.md`](../../README.md) is the Advanced README / public trust surface. Use it as the human front door, reviewer starting point, trust-boundary summary, high-level claim contract, and agent-visible context surface.
- [`AGENTS.md`](../../AGENTS.md) is the Codex/agent operating rules file. Use it for scope discipline, guardrail preservation, and honest reporting expectations.
- [`BUILD_CONTRACT.md`](../../BUILD_CONTRACT.md) is the current task contract template. Use it to state the goal, constraints, deliverables, verification requirements, stop conditions, scope boundaries, not-proven boundaries, and final report format.
- [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) explains how `/goal` uses README.md, AGENTS.md, BUILD_CONTRACT.md, and docs/agent files together for multi-hour Codex sessions.
- [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md) is the navigation layer for multi-PR campaign control docs and closeout flow.
- [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md) explains how a multi-PR goal campaign runs one scoped PR at a time with current-state checkpoints, full verification, remote checks, and stop conditions.
- [`CAMPAIGN_CHECKPOINT_LEDGER.md`](../archive/agent-history/CAMPAIGN_CHECKPOINT_LEDGER.md) defines the minimal current-state fields.
- [`CAMPAIGN_PR_READINESS_CHECKLIST.md`](../archive/agent-history/CAMPAIGN_PR_READINESS_CHECKLIST.md) defines the per-PR opening, merge, post-merge, scope, and stop-condition gate for campaign slices.
- [`CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md`](../archive/agent-history/CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md) defines changed-file, forbidden-scope, claim, guard-test, and stop-condition auditing for campaign slices.
- [`CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md`](../archive/agent-history/CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md) defines the factual handoff format for a material pause, resume, or closeout.
- [`CAMPAIGN_CLOSEOUT_PROTOCOL.md`](../archive/agent-history/CAMPAIGN_CLOSEOUT_PROTOCOL.md) defines the final count, verification, reporting, and pause rules before a campaign may be called complete.
- [`CAMPAIGN_REMOTE_CHECK_AUDIT.md`](../archive/agent-history/CAMPAIGN_REMOTE_CHECK_AUDIT.md) defines remote PR check and main merge-commit check auditing before a campaign PR can merge or count.
- [`CAMPAIGN_MERGE_GATE.md`](../archive/agent-history/CAMPAIGN_MERGE_GATE.md) defines the final current-head, local verification, remote check, scope, merge method, and post-merge main gate.
- [`CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md`](../archive/agent-history/CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md) defines how to log, recover from, pause, and resume after local, remote, scope, or tooling failures.
- [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md) defines focused-vs-full verification. Use focused checks while editing and full checks before merge.
- [`SESSION_MANAGER.md`](SESSION_MANAGER.md) tracks only the active slot, branch/PR, exact-head source, completed gates, genuine blocker, and next action.
- [`FAILURE_LOG.md`](FAILURE_LOG.md) tracks unresolved material blockers and reusable technical lessons, not routine command or polling history.

## Codex Session Flow

1. Read [`README.md`](../../README.md) first to understand the public trust surface and not-proven boundaries.
2. Read [`AGENTS.md`](../../AGENTS.md) for Codex and agent operating rules.
3. Read [`BUILD_CONTRACT.md`](../../BUILD_CONTRACT.md) or the user-provided task contract before editing.
4. Read [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) before starting or resuming long-running `/goal` work.
5. Read [`CAMPAIGN_SYSTEM_INDEX.md`](CAMPAIGN_SYSTEM_INDEX.md) before starting, resuming, auditing, or closing a multi-PR goal campaign.
6. Read [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md) before executing campaign PR slices.
7. Read [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md) before choosing checks.
8. Use [`SESSION_MANAGER.md`](SESSION_MANAGER.md) for concise current state during long sessions, interruptions, resumes, or handoffs.
9. Use [`FAILURE_LOG.md`](FAILURE_LOG.md) only when a failure exposes a product/security defect, invalidates evidence, risks persistent state, requires non-obvious recovery, blocks a mandatory gate, or yields a reusable lesson.
10. Keep evidence honest: record what changed, what passed, what failed, what remained pending, and what was not verified.

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
