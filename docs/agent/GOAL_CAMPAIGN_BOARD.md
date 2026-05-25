# Goal Campaign Board

This board tracks the bounded **LoadBalancerPro Goal Mode 10-PR Trial**. It is documentation only; it does not add automation, CI/Maven wiring, runtime behavior, Docker/Compose behavior, external targets, secrets, or production claims.

Use this board with [`GOAL_CAMPAIGN_CONTRACT.md`](GOAL_CAMPAIGN_CONTRACT.md), [`GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md`](GOAL_CAMPAIGN_BUILD_CONTRACT_EXAMPLE.md), [`GOAL_CAMPAIGN_PR_TEMPLATE.md`](GOAL_CAMPAIGN_PR_TEMPLATE.md), [`GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md`](GOAL_CAMPAIGN_CHECKPOINT_TEMPLATE.md), [`GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md`](GOAL_CAMPAIGN_FINAL_REPORT_TEMPLATE.md), [`SESSION_MANAGER.md`](SESSION_MANAGER.md), [`FAILURE_LOG.md`](FAILURE_LOG.md), and [`VERIFICATION_PROTOCOL.md`](VERIFICATION_PROTOCOL.md).

## Current Trial State

- Campaign goal: LoadBalancerPro Goal Mode 10-PR Trial.
- Total target: 10 merged PRs.
- Completed campaign PRs: 2 / 10.
- Current PR slot: 3.
- Current branch: `codex/goal-campaign-build-contract-example`.
- Current PR: not opened yet.
- Current main HEAD: `a4e2a9780de53857280748b51e097364a9872b45`.
- Slot 1 PR: [#306](https://github.com/RicheyWorks/LoadBalancerPro/pull/306).
- Slot 1 head SHA: `30828f89a41d64e30d1acc668714e5455a6e8a9f`.
- Slot 1 merge SHA: `9b0efc0dc0d6654c0e8f95294e77e7de72bd7941`.
- Slot 1 post-merge main checks: CI and CodeQL green.
- Slot 2 PR: [#307](https://github.com/RicheyWorks/LoadBalancerPro/pull/307).
- Slot 2 head SHA: `3c8edd518b82fd1f182044d339d224b72bf9b75e`.
- Slot 2 merge SHA: `a4e2a9780de53857280748b51e097364a9872b45`.
- Slot 2 post-merge main checks: CI and CodeQL green.

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
| 1 | Goal campaign template architecture | codex/goal-campaign-template-architecture | [#306](https://github.com/RicheyWorks/LoadBalancerPro/pull/306) | post-merge main green | `30828f89a41d64e30d1acc668714e5455a6e8a9f` | `9b0efc0dc0d6654c0e8f95294e77e7de72bd7941` | Template docs merged and main CI/CodeQL green |
| 2 | Initialize goal campaign board for this trial | codex/goal-campaign-board-initialization | [#307](https://github.com/RicheyWorks/LoadBalancerPro/pull/307) | post-merge main green | `3c8edd518b82fd1f182044d339d224b72bf9b75e` | `a4e2a9780de53857280748b51e097364a9872b45` | Board initialized; main CI/CodeQL green |
| 3 | Add filled BUILD_CONTRACT example for 10-PR campaign | codex/goal-campaign-build-contract-example | pending | in progress | pending | pending | Build contract example edit batch in progress |
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
