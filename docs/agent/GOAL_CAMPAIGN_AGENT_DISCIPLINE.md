# Goal Campaign Agent Discipline

This page is documentation only. It adds operating discipline for Codex agents running the LoadBalancerPro Goal Mode 10-PR Trial; it does not add automation, CI/Maven wiring, Dockerfile changes, Compose behavior, runtime behavior, endpoints, runner services, secrets, external targets, or production claims.

Use this page with [`AGENTS.md`](../../AGENTS.md), [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BOARD.md`](GOAL_CAMPAIGN_BOARD.md), [`SESSION_MANAGER.md`](SESSION_MANAGER.md), [`FAILURE_LOG.md`](FAILURE_LOG.md), [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md), [`GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md`](GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md), and [`CAMPAIGN_MERGE_GATE.md`](CAMPAIGN_MERGE_GATE.md).

## Purpose

Goal campaigns stretch Codex across multiple PRs, so the agent must act like a careful release assistant rather than a broad refactoring engine. The campaign discipline is:

- one scoped PR at a time;
- docs/test-only by default unless the user explicitly scopes otherwise;
- current-head local verification before PR creation;
- current-head remote verification before merge;
- post-merge main verification before counting the slot;
- SESSION_MANAGER.md updates after every checkpoint;
- FAILURE_LOG.md entries before continuing after any failure.

## Required Agent Behavior

- Read README.md, AGENTS.md, BUILD_CONTRACT.md, the goal-mode protocol, the campaign contract, the board, the verification protocol, SESSION_MANAGER.md, and FAILURE_LOG.md before changing a campaign slot.
- Keep the active branch, PR URL, head SHA, changed files, checks run, remote status, blocker, next action, and continue/pause decision factual in SESSION_MANAGER.md.
- Log local command failures, documentation guard failures, diff hygiene failures, remote check failures, ambiguous check states, scope conflicts, and unsafe requests in FAILURE_LOG.md before continuing.
- Use focused checks while editing, then run the full required verification ladder before opening or merging a PR.
- Merge only when the latest active required checks are green for the current head SHA.
- Do not accept pending, failed, cancelled, stale, or duplicate-only required checks.
- Do not claim green main until the merge commit has completed main CI and CodeQL successfully.

## Stop Conditions

Pause the goal instead of improvising when:

- the branch diff moves outside the slot scope;
- production code, Maven config, CI/workflow, Dockerfile, Compose behavior, runtime behavior, endpoints, scripts, runner services, secrets, or external/cloud/tenant targets are needed but not explicitly scoped;
- local verification fails and the fix is not obviously safe;
- remote checks fail, are cancelled, are stale, or are ambiguous;
- main is red or still pending after a merge;
- the user has to choose between safety, scope, or wording tradeoffs and a human decision is needed.

## Not-Proven Boundaries

The campaign discipline preserves the README and AGENTS.md not-proven boundaries. It does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof unless actually implemented and verified, or broader automation.

## Slot 9 Application

For slot 9, this page makes AGENTS.md the durable place for campaign agent discipline. It does not change application behavior, documentation claims, or verification commands; it only clarifies how an agent should behave while executing the remaining campaign slots.
