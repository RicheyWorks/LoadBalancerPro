# Failure Log Template

Use this template to record failures during PR health passes, docs guard updates, local verification, and remote CI review.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and record blockers before pause/resume decisions.

## Entry

Date/time:

Branch/PR:

Failure type:

Failing check:

Suspected cause:

Fix attempted:

Result:

Follow-up action:

## Notes

- Keep entries factual.
- Include exact failing test names or job names when available.
- Distinguish local failures from remote PR failures.
- Do not treat a fixed local failure as remotely green until current remote checks complete successfully.
