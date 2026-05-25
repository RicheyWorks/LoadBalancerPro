# Agent Operating Rules

This file gives Codex and other repository agents explicit operating rules. It does not replace the README public trust surface; it keeps task/session procedure out of the README while preserving the same safety boundaries.

For the session startup path that ties README, this file, the build contract, and the docs/agent templates together, use [`docs/agent/AGENT_WORKFLOW_QUICKSTART.md`](docs/agent/AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` sessions, use [`docs/agent/GOAL_MODE_LONG_RUN_PROTOCOL.md`](docs/agent/GOAL_MODE_LONG_RUN_PROTOCOL.md). For multi-PR goal campaigns, start with [`docs/agent/CAMPAIGN_SYSTEM_INDEX.md`](docs/agent/CAMPAIGN_SYSTEM_INDEX.md) and use [`docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md`](docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md).

## Core Rules

- Preserve safety boundaries and not-proven boundaries.
- Do not overclaim production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmark evidence, throughput/p95/p99 evidence, or broader automation.
- Do not weaken guardrails or remove boundary language just to make prose shorter.
- Do not change production behavior unless the task explicitly scopes that behavior change.
- Respect docs/test-only scope when requested.
- Keep local-lab claims bounded to optional/manual/local-lab-only behavior unless a later scoped PR proves otherwise.
- Do not add CI/Maven/Docker/Compose/runtime behavior unless explicitly scoped.
- Do not introduce secrets, external targets, cloud/tenant targets, private-network targets, or production-looking defaults.
- Report honestly what was verified, what was not verified, and which checks remain pending.

## Verification Posture

- Use focused verification first while editing.
- Use relevant selector bundles when a change touches shared documentation or guard tests.
- Use full verification before merge decisions.
- Do not accept stale, failed, cancelled, or pending required checks as green.
- Do not claim green main while main remote checks are pending.

## Scope Discipline

- Read the user request and the current branch diff before editing.
- For long-running `/goal` work, keep the active objective inside the task contract and update the session manager at checkpoints.
- Keep edits close to the requested files and behavior surface.
- Treat README and reviewer docs as claim contracts, not cosmetic copy.
- Preserve reviewer trust wording when refactoring or reorganizing documentation.
- Stop and report if required safety wording conflicts with a requested wording change.

## Reporting

Every final report should name:

- branch and PR;
- head SHA;
- changed files;
- verification run;
- remote check status when available;
- scope/safety audit;
- remaining not-proven boundaries;
- next recommended action.
