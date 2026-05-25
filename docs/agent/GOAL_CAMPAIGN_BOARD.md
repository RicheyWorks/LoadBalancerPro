# Goal Campaign Board

This board tracks a bounded 10-PR Codex `/goal` campaign. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this board with GOAL_CAMPAIGN_CONTRACT.md, GOAL_CAMPAIGN_PR_TEMPLATE.md, GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md, GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md, SESSION_MANAGER.md, FAILURE_LOG.md, and VERIFICATION_PROTOCOL.md.

## Board Rules

- Track 10 PR slots.
- Work one scoped PR at a time.
- Update SESSION_MANAGER.md after every checkpoint.
- Log failures in FAILURE_LOG.md.
- Merge only when latest required checks are green.
- Failed/cancelled/stale/pending required checks are not acceptable.
- Do not count a slot until post-merge main checks are green.
- Preserve not-proven boundaries.

## Status Values

Use these status values:

- planned;
- in progress;
- local verification passed;
- PR opened;
- remote checks green;
- merged;
- post-merge main green;
- paused;
- blocked;
- abandoned.

## Trial Board

| Slot | Scope | Branch | PR | Status | Head SHA | Merge SHA | Checkpoint |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Goal campaign template architecture | codex/goal-campaign-template-architecture | pending | in progress | pending | pending | template docs being added |
| 2 | Initialize goal campaign board for this trial | pending | pending | planned | pending | pending | pending |
| 3 | Add filled BUILD_CONTRACT example for 10-PR campaign | pending | pending | planned | pending | pending | pending |
| 4 | Add SESSION_MANAGER campaign checkpoint examples | pending | pending | planned | pending | pending | pending |
| 5 | Add FAILURE_LOG campaign recovery examples | pending | pending | planned | pending | pending | pending |
| 6 | Add VERIFICATION_PROTOCOL campaign mode refinement | pending | pending | planned | pending | pending | pending |
| 7 | Add README goal-mode campaign summary | pending | pending | planned | pending | pending | pending |
| 8 | Add Reviewer Trust Map goal-mode campaign navigation | pending | pending | planned | pending | pending | pending |
| 9 | Add AGENTS.md campaign discipline section | pending | pending | planned | pending | pending | pending |
| 10 | Add goal-mode trial final handoff/report | pending | pending | planned | pending | pending | pending |

## Remote Check Rules

Each slot must record Build/Test/Package/Smoke, Analyze Java / CodeQL, and Dependency Review where applicable. Merge only when latest required checks are green for the current head SHA.

Failed/cancelled/stale/pending required checks are not acceptable, and duplicate-only checks do not count as green.

## Not-Proven Boundaries

The board does not prove production readiness, production certification, live-cloud validation, real-tenant validation, runtime enforcement, load/stress/benchmarking, throughput/p95/p99 evidence, replay/evidence/report/storage/export proof unless implemented and verified, or broader automation.
