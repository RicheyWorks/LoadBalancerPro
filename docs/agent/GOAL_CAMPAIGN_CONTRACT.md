# Goal Campaign Contract

This contract defines the reusable structure for a bounded Codex `/goal` campaign. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this contract with README.md, AGENTS.md, BUILD_CONTRACT.md, VERIFICATION_PROTOCOL.md, GOAL_MODE_LONG_RUN_PROTOCOL.md, SESSION_MANAGER.md, FAILURE_LOG.md, GOAL_CAMPAIGN_BOARD.md, GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md, GOAL_CAMPAIGN_PR_TEMPLATE.md, GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md, and GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md.

## Campaign Contract

A goal campaign is one durable objective split into small, separately scoped PRs. The default trial shape is 10 PR slots, one scoped PR at a time, starting each slot from clean main after the previous slot has merged and main has gone green.

The campaign contract must state:

- campaign name;
- purpose;
- current main SHA at start;
- allowed files and forbidden files;
- number of PR slots;
- per-PR deliverables;
- required SESSION_MANAGER.md updates;
- required FAILURE_LOG.md entries for failures;
- verification levels;
- remote check rules;
- merge rules;
- stop conditions;
- remaining not-proven boundaries.

## Ten PR Slots

The default campaign has 10 PR slots. Each slot gets one branch, one PR, one focused scope, one verification record, one remote check audit, one merge decision, and one post-merge main check before the next slot begins.

An opened PR, locally green branch, pending remote check, stale check, duplicate-only check, or unverified main merge commit does not count as a successful merged slot.

## Verification Levels

Each slot should run the required levels from VERIFICATION_PROTOCOL.md:

- focused failing test or focused documentation guard;
- relevant focused selector bundle;
- `mvn -q test`;
- `mvn -q "-DskipTests" package`;
- `mvn -B package`;
- `git diff --check`;
- `git diff --check origin/main...HEAD`;
- `git diff --cached --check`;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package`;
- remote PR checks;
- post-merge main checks.

Use focused checks while editing. Use full verification before merge.

## Remote Check Rules

Merge only when latest required checks are green for the current PR head. Build/Test/Package/Smoke must pass, Analyze Java / CodeQL must pass, and Dependency Review must pass where applicable.

Failed/cancelled/stale/pending required checks are not acceptable. Duplicate-only checks are not acceptable evidence. Do not claim green main while main remote checks are pending.

## Merge Rules

Merge one scoped PR at a time. Use the normal GitHub merge commit unless the task explicitly says otherwise. Do not squash, do not rebase, do not delete branches, do not create releases or tags, and do not mutate GitHub settings, rulesets, secrets, environments, or required checks.

After merge, pull main, confirm the PR head is contained in main, rerun the requested post-merge checks, inspect main CI/CodeQL for the merge commit, and only then count the slot.

## Checkpoint Rules

Update SESSION_MANAGER.md after every checkpoint:

- branch created;
- edit batch completed;
- focused verification completed;
- full local verification completed;
- PR opened;
- remote checks completed;
- merge decision completed;
- post-merge main checks completed;
- pause or final report.

Log failures in FAILURE_LOG.md with timestamp, branch/PR, failing check, failure type, suspected cause, fix attempted, result, recovery status, and next action.

## Stop Conditions

Pause the goal if:

- main becomes red;
- a required check fails and cannot be fixed safely;
- scope requires production behavior changes;
- scope requires secrets or external targets;
- scope requires CI/Maven/Docker/Compose/runtime changes not explicitly allowed;
- GitHub check state is ambiguous;
- Codex is uncertain whether to continue;
- human approval is needed;
- 10 PRs are merged.

## Not-Proven Boundaries

Preserve not-proven boundaries. This contract does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof unless implemented and verified, or broader automation.
