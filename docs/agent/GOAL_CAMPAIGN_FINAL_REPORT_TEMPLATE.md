# Goal Campaign Final Report Template

This template defines the final report for a bounded Codex `/goal` campaign. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this template with GOAL_CAMPAIGN_CONTRACT.md, GOAL_CAMPAIGN_BOARD.md, GOAL_CAMPAIGN_PR_TEMPLATE.md, GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md, SESSION_MANAGER.md, FAILURE_LOG.md, and VERIFICATION_PROTOCOL.md.

## Final Report Template

```text
Overall Classification: PASS / WARN / FAIL

Campaign Goal:
Elapsed Time:
PRs Attempted:
PRs Merged:
Open PRs:
Blocked PRs:
Current Main HEAD:
Main CI/CodeQL:
Session Manager Updated:
Failures Logged:
Verification Summary:
Scope/Safety Audit:
Remaining Not-Proven Boundaries:
Recommended Next Goal:
```

## Completion Rules

Report PASS only when all intended PR slots have merged, post-merge main checks have passed, and main CI/CodeQL are green for the final merge commit.

Report WARN when work is safe but incomplete, remote checks are still pending, or human review is needed.

Report FAIL when a required check failed, scope became unsafe, main is red, or the campaign cannot continue safely.

## Required Evidence

The final report must include:

- total PR slots attempted;
- total PR slots merged;
- branch and PR URL for each merged slot;
- head SHA and merge SHA for each merged slot;
- checks run;
- remote status;
- failure log references;
- SESSION_MANAGER.md checkpoint status;
- scope/safety audit;
- remaining not-proven boundaries.

## Remote Check Rules

Merge only when latest required checks are green. Failed/cancelled/stale/pending required checks are not acceptable. Duplicate-only checks do not prove a required check passed.

## Not-Proven Boundaries

The final report must preserve not-proven boundaries. The campaign does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof unless implemented and verified, or broader automation.
