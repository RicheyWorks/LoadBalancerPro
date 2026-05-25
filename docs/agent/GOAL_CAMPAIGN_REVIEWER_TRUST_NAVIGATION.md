# Goal Campaign Reviewer Trust Navigation

This document is the reviewer-facing navigation lane for the **LoadBalancerPro Goal Mode 10-PR Trial**. It is docs/test-only. It does not add automation, CI/Maven wiring, Dockerfile changes, Compose behavior changes, runtime behavior changes, endpoint changes, k6/Bruno/Toxiproxy behavior changes, runner services, secrets, external/cloud/tenant targets, or production claims.

## Purpose

Use this lane when a reviewer wants to understand how the campaign evidence is organized without reading every agent scaffold first. The lane keeps the README as the public trust surface, uses the Reviewer Trust Map as the reviewer entry point, and points into the campaign docs that record scope, verification, merge gates, checkpoints, and final reporting.

This lane does not replace the campaign contract. It helps reviewers find the contract and confirm that the campaign remains bounded.

## Reviewer Path

1. Start with `README.md` for the Advanced README / public trust surface.
2. Use `docs/REVIEWER_TRUST_MAP.md` for reviewer navigation and evidence boundaries.
3. Use `docs/agent/GOAL_CAMPAIGN_CONTRACT.md` for the 10-PR campaign contract.
4. Use `docs/agent/GOAL_CAMPAIGN_BOARD.md` for the current slot status, PR history, and merge status.
5. Use `docs/agent/GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md` and `docs/agent/VERIFICATION_PROTOCOL.md` for focused checks, full checks, remote checks, and main post-merge checks.
6. Use `docs/agent/SESSION_MANAGER.md` for the active checkpoint.
7. Use `docs/agent/FAILURE_LOG.md` for failures and recovery.
8. Use `docs/agent/GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md` when the campaign reaches 10 merged PRs or pauses.

## Campaign Trust Rules

- Work one scoped PR at a time.
- Prefer docs/test-only scope for every campaign slot.
- Update `SESSION_MANAGER.md` after every checkpoint.
- SESSION_MANAGER.md after every checkpoint is the durable campaign checkpoint rule.
- Log failures in `FAILURE_LOG.md` before continuing.
- FAILURE_LOG.md before continuing is the durable campaign failure logging rule.
- Run focused checks while editing.
- Run full local verification before opening or merging a PR.
- Merge only when latest/current-head required checks are green.
- Failed, cancelled, stale, pending, or duplicate-only required checks are not acceptable.
- Do not count a PR slot until post-merge main CI and CodeQL are green.
- Preserve the not-proven boundaries in README, AGENTS.md, BUILD_CONTRACT.md, the campaign contract, and this trust navigation lane.

## Reviewer Questions

| Reviewer question | Start here | What it proves | What it does not prove |
| --- | --- | --- | --- |
| What is the campaign objective? | `GOAL_CAMPAIGN_CONTRACT.md` | The bounded 10-PR goal, allowed scope, verification expectations, and stop conditions. | Production readiness, production certification, live-cloud validation, real-tenant validation, or runtime enforcement. |
| Which PR slot is active? | `GOAL_CAMPAIGN_BOARD.md` and `SESSION_MANAGER.md` | Current branch, PR URL when opened, head tracking, checks run, blockers, and next action. | That a pending PR is mergeable or green before remote checks finish. |
| What should be checked before merge? | `GOAL_CAMPAIGN_VERIFICATION_PROTOCOL_REFINEMENT.md` and `CAMPAIGN_MERGE_GATE.md` | Focused verification, full local verification, remote PR checks, and post-merge main checks required by the campaign. | Permission to accept stale, failed, cancelled, pending, or duplicate-only checks. |
| What happened when a check failed? | `FAILURE_LOG.md` and `GOAL_CAMPAIGN_FAILURE_RECOVERY_EXAMPLES.md` | Failure type, suspected cause, attempted fix, result, recovery status, and next action. | Permission to keep going through unsafe scope or ambiguous check state. |
| How is the final campaign reported? | `GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md` | Final status fields for PRs attempted, PRs merged, main state, verification, scope, failures, and remaining boundaries. | Production proof or automation beyond the docs/test-only campaign scaffold. |

## Scope Boundary

The campaign navigation lane does not authorize production code changes, Maven config changes, CI/workflow changes, Dockerfile changes, Compose behavior changes, runtime behavior changes, endpoint changes, k6 behavior changes, Bruno behavior changes, Toxiproxy behavior changes, scripts changes, runtime resource changes, runner services, automation, secrets, or external/cloud/tenant targets.

Any slot that needs those changes must pause or become a separately approved task outside this docs/test-only campaign lane.

## Not-Proven Boundaries

This reviewer trust navigation lane does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof unless implemented and verified, or broader automation.
