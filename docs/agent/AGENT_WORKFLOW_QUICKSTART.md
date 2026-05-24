# Agent Workflow Quickstart

This quickstart explains how to use the repository trust and agent contract files together during Codex sessions. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, external targets, secrets, or broader production claims.

## File Roles

In short: README.md is the Advanced README / public trust surface, AGENTS.md is the Codex/agent operating rules file, BUILD_CONTRACT.md is the current task contract template, VERIFICATION_PROTOCOL.md defines focused-vs-full verification, SESSION_MANAGER.md tracks long-running session state, and FAILURE_LOG.md tracks failures and recovery.

- [`README.md`](../../README.md) is the Advanced README / public trust surface. Use it as the human front door, reviewer starting point, trust-boundary summary, high-level claim contract, and agent-visible context surface.
- [`AGENTS.md`](../../AGENTS.md) is the Codex/agent operating rules file. Use it for scope discipline, guardrail preservation, and honest reporting expectations.
- [`BUILD_CONTRACT.md`](../../BUILD_CONTRACT.md) is the current task contract template. Use it to state the goal, constraints, deliverables, verification requirements, stop conditions, scope boundaries, not-proven boundaries, and final report format.
- [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md) defines focused-vs-full verification. Use focused checks while editing and full checks before merge.
- [`SESSION_MANAGER.md`](SESSION_MANAGER.md) tracks long-running session state. Use it for current branch, PR, goal, head SHA, changed files, checks run, blockers, next action, and recovery notes.
- [`FAILURE_LOG.md`](FAILURE_LOG.md) tracks failures and recovery. Use it for local test failures, remote check failures, suspected causes, fixes attempted, results, and follow-up actions.

## Codex Session Flow

1. Read [`README.md`](../../README.md) first to understand the public trust surface and not-proven boundaries.
2. Read [`AGENTS.md`](../../AGENTS.md) for Codex and agent operating rules.
3. Read [`BUILD_CONTRACT.md`](../../BUILD_CONTRACT.md) or the user-provided task contract before editing.
4. Read [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md) before choosing checks.
5. Use [`SESSION_MANAGER.md`](SESSION_MANAGER.md) for long sessions, interruptions, resumes, or handoffs.
6. Use [`FAILURE_LOG.md`](FAILURE_LOG.md) when a local check, remote check, or scope audit fails.
7. Keep evidence honest: record what changed, what passed, what failed, what remained pending, and what was not verified.

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
