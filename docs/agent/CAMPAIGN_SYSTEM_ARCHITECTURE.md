# Campaign System Architecture

This document defines the docs/test-only campaign system for long-running Codex `/goal` work that produces a sequence of small, independently reviewed PRs. It is operational scaffolding only; it does not add automation, CI/Maven wiring, runtime behavior, external targets, secrets, Docker/Compose behavior, or production claims.

README.md remains the constitutional layer and public trust surface. AGENTS.md remains the operating rules file. BUILD_CONTRACT.md remains the focused execution contract. GOAL_MODE_LONG_RUN_PROTOCOL.md governs `/goal` lifecycle, pause/resume/clear behavior, checkpoint expectations, and verification discipline.

## Campaign Objective

A campaign is a bounded sequence of separately scoped PRs that share one durable objective. The current campaign target is ten successful merged PRs, one scoped PR at a time, from clean main. The campaign stops after ten successful merged PRs or pauses if scope becomes unsafe, checks fail, main is red, or human approval is needed.

## Campaign Roles

- README.md: public claim boundary, reviewer starting point, and not-proven boundary source.
- AGENTS.md: Codex and agent operating rules.
- BUILD_CONTRACT.md: per-PR task contract, deliverables, stop conditions, and final report format.
- GOAL_MODE_LONG_RUN_PROTOCOL.md: `/goal`, `/plan`, `/goal pause`, `/goal resume`, and `/goal clear` behavior.
- VERIFICATION_PROTOCOL.md: focused checks, full checks, remote PR checks, and post-merge main checks.
- SESSION_MANAGER.md: checkpoint ledger for current branch, PR, head SHA, checks run, blockers, and next action.
- FAILURE_LOG.md: factual record for local failures, remote failures, suspected causes, fixes attempted, results, and follow-up action.
- CAMPAIGN_SYSTEM_INDEX.md: navigation layer for the campaign control docs, execution loop, verification path, scope audit, and closeout flow.
- CAMPAIGN_CHECKPOINT_LEDGER.md: required checkpoint fields and counting rules for multi-PR goal campaigns.
- CAMPAIGN_PR_READINESS_CHECKLIST.md: per-PR opening, merge, post-merge, scope, and stop-condition checklist.
- CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md: changed-file, forbidden-scope, claim, guard-test, and stop-condition scope audit.
- CAMPAIGN_REMOTE_CHECK_AUDIT.md: remote PR and main merge-commit check audit rules.
- CAMPAIGN_MERGE_GATE.md: final pre-merge and post-merge decision gate before a PR can merge or count.
- CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md: failure logging, safe recovery, pause, and resume rules for campaign interruptions.
- CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md: factual handoff report format for pauses, resumes, checkpoints, and human review.
- CAMPAIGN_CLOSEOUT_PROTOCOL.md: final campaign count, verification, report, and pause rules before claiming the campaign complete.

## Ten-PR Execution Loop

Repeat this loop for each campaign PR:

1. Start from clean main and confirm current main CI/CodeQL is green for the main head.
2. Choose one small scoped slice and write the per-PR contract in the PR body or task notes.
3. Create a new `codex/` branch.
4. Update SESSION_MANAGER.md at branch creation, after edits, before full verification, after PR creation, after remote checks, and after merge.
5. Keep changes inside the scoped slice.
6. Run focused verification while editing.
7. Run full local verification before merge.
8. Use CAMPAIGN_PR_READINESS_CHECKLIST.md and CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md, then open a PR with scope, verification, safety audit, and not-proven boundaries.
9. Use CAMPAIGN_REMOTE_CHECK_AUDIT.md and CAMPAIGN_MERGE_GATE.md, then merge only after current-head remote required checks are complete and successful.
10. Return to main, fast-forward, run post-merge checks, confirm main remote checks for the merge commit, and then start the next PR.

## Required Checkpoints

Update SESSION_MANAGER.md at these minimum checkpoints:

- campaign start;
- branch created;
- edit batch completed;
- focused checks completed;
- full local verification completed;
- PR opened;
- remote PR checks completed;
- PR merged;
- post-merge main checks completed;
- campaign pause or closeout.

If any check fails or scope becomes unsafe, use CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md and update FAILURE_LOG.md before pausing.

## Merge Gate

Do not merge a campaign PR unless all of these are true:

- the branch head SHA is the reviewed head SHA;
- the diff is still inside scope;
- focused local checks passed;
- full local verification passed;
- required remote PR checks passed for the current head;
- no required remote check is failed, cancelled, stale, pending, or duplicate-only;
- main was green before starting the PR;
- no human decision remains open.

## Stop Conditions

Pause the campaign instead of improvising when:

- scope expands beyond BUILD_CONTRACT.md or the active PR contract;
- README, AGENTS.md, BUILD_CONTRACT.md, or reviewer trust boundaries would be weakened;
- production behavior, endpoint behavior, Compose behavior, CI/Maven wiring, runtime resources, scripts, secrets, external/cloud/tenant targets, or automation appear outside explicit scope;
- local verification fails and the fix is not obvious and safe;
- remote checks fail, are cancelled, are stale, or remain pending at a merge decision;
- main CI/CodeQL is red;
- human approval is needed.

## Reporting Rules

Every campaign PR report must include branch, PR URL, head SHA, changed files, what changed, local verification, remote checks, scope/safety audit, remaining not-proven boundaries, and next recommended action.

Use CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md for pauses, resumes, and checkpoint handoffs. Use CAMPAIGN_CLOSEOUT_PROTOCOL.md for the final count, merge commits, verification summary, failures or pauses, and any remaining work.

## Not-Proven Boundaries

The campaign does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation unless a separately scoped implementation actually adds and verifies that behavior.
