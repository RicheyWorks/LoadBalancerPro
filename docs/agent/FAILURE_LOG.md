# Failure Log Template

Use this template to record failures during PR health passes, docs guard updates, local verification, and remote CI review.

For the full Codex session startup path, use [`AGENT_WORKFLOW_QUICKSTART.md`](AGENT_WORKFLOW_QUICKSTART.md). For long-running `/goal` work, use [`GOAL_MODE_LONG_RUN_PROTOCOL.md`](GOAL_MODE_LONG_RUN_PROTOCOL.md) and record blockers before pause/resume decisions. For multi-PR goal campaigns, use [`CAMPAIGN_SYSTEM_ARCHITECTURE.md`](CAMPAIGN_SYSTEM_ARCHITECTURE.md) and [`CAMPAIGN_CHECKPOINT_LEDGER.md`](CAMPAIGN_CHECKPOINT_LEDGER.md), then log any local, remote, or scope-audit failure before pausing.

## Entry

Date/time:

Branch/PR:

Failure type:

Failing check:

Suspected cause:

Fix attempted:

Result:

Follow-up action:

## Entry

Date/time: 2026-05-24T15:52-07:00

Branch/PR: codex/goal-campaign-checkpoint-ledger / pending

Failure type: local tooling command

Failing check: gh pr create body quoting attempt

Suspected cause: PowerShell passed part of the multi-line PR body as command flags, causing gh to reject `-q` as an unknown shorthand flag.

Fix attempted: switched PR creation to `gh pr create --body-file -` with stdin body content.

Result: retry succeeded and opened https://github.com/RicheyWorks/LoadBalancerPro/pull/297.

Follow-up action: retry PR creation, then update SESSION_MANAGER.md with the PR URL and final branch head.

## Notes

- Keep entries factual.
- Include exact failing test names or job names when available.
- Distinguish local failures from remote PR failures.
- Do not treat a fixed local failure as remotely green until current remote checks complete successfully.
