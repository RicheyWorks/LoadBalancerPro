# Campaign Checkpoint Policy

Repository files hold current campaign state only. Routine commands, polling, CI narration, test totals, and temporary heads belong in pull-request checks, logs, and artifacts.

Use [`SESSION_MANAGER.md`](../../agent/SESSION_MANAGER.md) for the active checkpoint, [`FAILURE_LOG.md`](../../agent/FAILURE_LOG.md) for unresolved material blockers and reusable lessons, and [`COMBINED_BUILD_PLAN_CAMPAIGN_SLOTS.json`](COMBINED_BUILD_PLAN_CAMPAIGN_SLOTS.json) for slot state.

Record only:

- slot ID and status;
- PR, final head, and merge commit when known;
- a genuine blocker;
- the next action.

Do not create a documentation-only commit to narrate a green head.
