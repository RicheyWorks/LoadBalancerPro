# Combined Build-Plan Campaign Contract

This file defines the active ordered implementation campaign derived from
[`../BUILD_PLAN_DEPLOYABLE.md`](../BUILD_PLAN_DEPLOYABLE.md) and
[`../BUILD_PLAN_LAB_SHADOW.md`](../BUILD_PLAN_LAB_SHADOW.md). The board and JSON manifest are operational state, not
evidence that a feature is complete.

## Objective

Complete the remaining proxy and lab/shadow/analysis slots as small engineering pull requests, one active slot at a
time. Production code, deterministic tests, infrastructure, security, deployment capability, benchmarks, and real lab
evidence are the default work surfaces. Routine documentation stays below 10% of substantive campaign effort.

## State

- Human-readable board: [`COMBINED_BUILD_PLAN_CAMPAIGN_BOARD.md`](COMBINED_BUILD_PLAN_CAMPAIGN_BOARD.md)
- Machine-checked manifest: [`COMBINED_BUILD_PLAN_CAMPAIGN_SLOTS.json`](COMBINED_BUILD_PLAN_CAMPAIGN_SLOTS.json)
- Verification guide: [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md)
- Material unresolved defects and reusable lessons: [`FAILURE_LOG.md`](FAILURE_LOG.md)

Update the board and manifest only at slot start, PR opening, merge completion, or a genuine blocker. GitHub owns
transient branch, SHA, check, polling, and review state.

## Execution

1. Start the lowest-ordinal dependency-ready slot from clean, remotely green `main`.
2. Implement the smallest complete acceptance slice and preserve unrelated active work.
3. Run focused checks while developing and risk-appropriate integration/full/package checks before review.
4. Require applicable exact-head CI, CodeQL, dependency review, SBOM, container, benchmark, and image gates.
5. Merge normally only when required checks are green; verify merge-main CI and CodeQL before starting the next slot.

## Safety

Do not weaken required checks or runtime guardrails, introduce secrets or production targets, spend billable cloud
resources, use production-looking defaults, or treat imported roadmap language as proof. Use mocks and loopback fixtures
unless separately authorized. Stop for genuine security, data-integrity, protected-workflow, destructive-action, or
scope/authority conflicts.

Campaign completion means every manifest slot is `MAIN_GREEN` and the final main commit has green required checks. It
does not by itself certify any untested deployment environment.
