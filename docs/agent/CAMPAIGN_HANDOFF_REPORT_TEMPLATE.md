# Campaign Handoff Report Template

This template defines the handoff report for long-running Codex `/goal` campaigns. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this template with CAMPAIGN_SYSTEM_ARCHITECTURE.md, CAMPAIGN_CHECKPOINT_LEDGER.md, CAMPAIGN_PR_READINESS_CHECKLIST.md, CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md, CAMPAIGN_REMOTE_CHECK_AUDIT.md, CAMPAIGN_MERGE_GATE.md, CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md, CAMPAIGN_CLOSEOUT_PROTOCOL.md, GOAL_MODE_LONG_RUN_PROTOCOL.md, SESSION_MANAGER.md, FAILURE_LOG.md, and VERIFICATION_PROTOCOL.md whenever a campaign pauses, resumes, is handed off, or reaches a PR checkpoint.

## Purpose

A campaign handoff report gives the next Codex run or human reviewer enough factual state to continue safely without relying on chat memory. It must be short, current, and grounded in checks that actually ran.

The handoff report must identify:

- current campaign count;
- current branch;
- current PR;
- current head SHA;
- current main SHA;
- current checkpoint;
- changed files;
- checks already run;
- remote PR check status;
- main post-merge check status;
- failures logged in FAILURE_LOG.md;
- blockers and human decisions needed;
- next safe action.

## Required Handoff Fields

Use this structure when pausing or transferring campaign work:

```text
Campaign:
Current count:
Current branch:
Current PR:
Current branch head SHA:
Current main SHA:
Current checkpoint:

Changed files:
What changed:
Scope/safety audit:

Focused checks run:
Full local verification:
Package checks:
Diff checks:
Smoke checks:
Remote PR checks:
Main post-merge checks:

Failures logged:
Blockers:
Next safe action:
Stop conditions:
Remaining not-proven boundaries:
```

## Evidence Rules

Keep the report factual:

- report only checks that actually ran;
- include exact test selectors when focused checks ran;
- include the PR URL and head SHA when a PR exists;
- include the merge commit and main SHA after merge;
- distinguish local verification from remote PR checks;
- distinguish remote PR checks from main post-merge checks;
- do not claim green main while remote checks are pending;
- do not accept failed, cancelled, stale, pending, skipped-only, or duplicate-only required checks as green.

## Failure And Recovery Notes

If a failure occurred, the handoff report must point to FAILURE_LOG.md and name:

- failure type;
- failing command, test, run, or job;
- suspected cause;
- fix attempted;
- result;
- follow-up action;
- last known good branch or main SHA.

Do not resume from a handoff until SESSION_MANAGER.md and FAILURE_LOG.md agree on the blocker, last known good state, checks run, and next safe action.

## Scope/Safety Audit

Every handoff must repeat the scope boundary in plain language:

- docs/test-only when that is the active contract;
- no production code changes unless explicitly scoped;
- no Maven config changes unless explicitly scoped;
- no CI/workflow changes unless explicitly scoped;
- no Dockerfile changes unless explicitly scoped;
- no Compose behavior changes unless explicitly scoped;
- no runtime behavior changes unless explicitly scoped;
- no endpoint behavior changes unless explicitly scoped;
- no k6, Bruno, or Toxiproxy behavior changes unless explicitly scoped;
- no scripts, secrets, external/cloud/tenant targets, or automation unless explicitly scoped.

Use CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md when preparing this section.

## Stop Conditions

Pause the campaign instead of handing off as ready when:

- scope is unclear or unsafe;
- checks failed and the fix is not complete;
- remote required checks are failed, cancelled, stale, pending, skipped-only, or duplicate-only;
- main CI/CodeQL is red;
- a human decision is needed;
- README.md, AGENTS.md, BUILD_CONTRACT.md, reviewer trust docs, or not-proven boundaries would be weakened.

## Not-Proven Boundaries

A handoff report does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof, or broader automation.
