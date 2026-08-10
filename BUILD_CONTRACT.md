# Optional Build Contract

A user prompt or pull-request description may serve as the task contract. Do not commit a filled copy of this template
unless the user explicitly requests it or the contract has lasting operator value.

## Outcome

State the concrete result to deliver.

## Scope

- In scope: files, behavior, and operational surfaces the task may change.
- Out of scope: adjacent systems that must remain unchanged.

## Safety Constraints

Name the task-specific security, data-integrity, external-target, destructive-action, and compatibility boundaries.
Repository-wide invariants remain in [`AGENTS.md`](AGENTS.md).

## Acceptance Criteria

List observable behavior and deterministic evidence that will show the outcome is complete.

## Verification

Choose checks according to risk: focused tests during development; affected integration/contract tests before review;
full tests for shared runtime, security, build, or broad test/governance changes; packaging for merge candidates or
packaging changes; and required exact-head GitHub checks before merge.

## Stop Conditions

List only conditions that genuinely require new authority, a user decision, an external-state change, or a safety/data
integrity resolution.
