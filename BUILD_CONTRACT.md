# Build Contract Template

Use this template when opening or executing a scoped repository task. It is a contract for scope, evidence, and stop conditions, not a production-readiness claim.

For the Codex startup flow that explains how this contract fits with README, AGENTS.md, and docs/agent templates, use [`docs/agent/AGENT_WORKFLOW_QUICKSTART.md`](docs/agent/AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` sessions, use [`docs/agent/GOAL_MODE_LONG_RUN_PROTOCOL.md`](docs/agent/GOAL_MODE_LONG_RUN_PROTOCOL.md) and keep this file as the focused execution contract. For multi-PR goal campaigns, start with [`docs/agent/CAMPAIGN_SYSTEM_INDEX.md`](docs/agent/CAMPAIGN_SYSTEM_INDEX.md), use [`docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md`](docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md), and use [`docs/agent/GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](docs/agent/GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md) as the filled 10-PR campaign example.

## Goal

State the concrete outcome expected from this task.

## Context And Constraints

- Current branch or expected base.
- Related PRs or documents.
- Files or subsystems that are explicitly in scope.
- Files or subsystems that are explicitly out of scope.
- Safety boundaries that must remain intact.

## Deliverables

- Files to add or update.
- Tests or guard checks to add or update.
- Documentation links or cross-links to preserve.
- Checkpoint or handoff files to update during long-running `/goal` work.
- Campaign checkpoints to update when the task belongs to a multi-PR goal campaign.

## Verification Requirements

- Focused failing test or focused documentation guard.
- Relevant focused selector bundle.
- `mvn -q test`.
- `mvn -q "-DskipTests" package`.
- `mvn -B package`.
- `git diff --check`.
- `git diff --check origin/main...HEAD`.
- `git diff --cached --check`.
- Enterprise lab package smoke when requested.
- Remote PR checks when a PR is opened.
- Main post-merge checks when a PR is merged.

## Evidence And Reporting

Report exact checks run, pass/fail state, head SHA, PR URL, remote check state, scope audit, and remaining not-proven boundaries.

## Stop Conditions

Stop before merge or push if:

- required checks fail, are cancelled, remain pending, or are stale;
- scope expands beyond the task contract;
- production behavior changes unexpectedly;
- safety wording is removed or weakened;
- secrets, external targets, cloud/tenant targets, or production-looking defaults appear;
- unreviewed CI/Maven/Docker/Compose/runtime behavior appears.

## Scope Boundaries

Name any forbidden areas, such as `src/main/java`, Maven config, CI/workflow files, Dockerfile, Docker Compose, scripts, runtime resources, production API/routing/scoring/proxy behavior, replay/report/storage/export behavior, local-lab behavior, k6, Bruno, or Toxiproxy.

## Not-Proven Boundaries

Unless explicitly implemented and verified in a separate scoped task, do not claim production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmark evidence, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation.

## Final Report Format

- Overall Classification: PASS / WARN / FAIL
- Branch
- PR URL
- Head SHA
- Changed files
- What changed
- Verification results
- Remote check status if available
- Scope/safety audit
- Remaining not-proven boundaries
- Next recommended action
