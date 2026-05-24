# Campaign Checkpoint Ledger

This document defines the checkpoint ledger for a multi-PR Codex `/goal` campaign. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this ledger with CAMPAIGN_SYSTEM_ARCHITECTURE.md, GOAL_MODE_LONG_RUN_PROTOCOL.md, SESSION_MANAGER.md, FAILURE_LOG.md, and VERIFICATION_PROTOCOL.md. The ledger explains what must be recorded at each campaign checkpoint so the current state is reviewable without relying on memory or chat context.

## Ledger Purpose

The ledger keeps each campaign PR auditable:

- current campaign objective;
- current PR number within the campaign;
- current branch;
- current PR URL when available;
- current head SHA;
- changed files;
- focused checks;
- full local verification;
- remote PR checks;
- post-merge main checks;
- blockers;
- next action;
- remaining not-proven boundaries.

## Required Checkpoint Fields

Each checkpoint should record:

- checkpoint name;
- timestamp or relative session point when useful;
- branch and PR;
- head SHA;
- changed files and scope;
- checks run and result;
- remote check state when available;
- blocker state;
- next action;
- whether the goal should continue, pause, resume, or clear.

## Ten-PR Campaign Count

The campaign count increases only after a PR has merged, local main has fast-forwarded to the merge commit, post-merge local checks have passed, and main CI/CodeQL is green for the merge commit.

An opened PR, locally green branch, or pending main check does not count as a successful merged campaign PR.

## Failure Handling

If a focused check, full check, remote check, scope audit, merge gate, or main post-merge check fails, record the failure in FAILURE_LOG.md before pausing. The ledger should point to the failure log entry and name the last known good SHA.

Do not continue to the next PR while the current PR, main branch, or failure recovery state is unresolved.

## Stop Conditions

Pause instead of continuing when:

- scope expands beyond the current PR contract;
- required checks fail, are cancelled, are stale, or remain pending at merge time;
- main CI/CodeQL is red;
- a human decision is needed;
- unsafe production behavior, endpoint behavior, Compose behavior, CI/Maven wiring, runtime resources, scripts, secrets, external/cloud/tenant targets, or automation appear outside explicit scope.

## Not-Proven Boundaries

Checkpoint records must preserve that the campaign does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation.

## Minimal Checkpoint Example

```text
Checkpoint: PR 2 focused checks completed
Branch: codex/example-branch
PR: https://github.com/RicheyWorks/LoadBalancerPro/pull/000
Head SHA: abc123
Changed files: docs/agent/example.md, related guard test
Checks: focused guard passed
Remote checks: not opened yet
Blocker: none
Next action: run full local verification
Goal state: continue
```
