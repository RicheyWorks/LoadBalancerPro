# Decision Explorer Phase 1 Campaign Board

Status: active / phase1-dto-skeleton.

Classification: WARN / decision-explorer-phase1-campaign.

Started from main SHA: `755ed394adfa18e462f89312c5289fd3154075f2`.

Current PR slot: DX-P1-G02.

Completed Phase 1 PRs: 1 / 9 planned.

Related architecture scope: [`DECISION_EXPLORER_PHASE1_ARCHITECTURE_SCOPE.md`](DECISION_EXPLORER_PHASE1_ARCHITECTURE_SCOPE.md).

Related bootstrap closeout: [`DECISION_EXPLORER_BOOTSTRAP_CLOSEOUT.md`](DECISION_EXPLORER_BOOTSTRAP_CLOSEOUT.md).

## Purpose

This board tracks the Decision Explorer Implementation Phase 1 campaign. Phase 1 is additive, read-only, and
simulation-only. It should turn existing LoadBalancerPro routing comparison and decision evidence into a bounded
reviewer-facing Decision Explorer surface without changing production routing, scoring, proxy, cloud, tenant, storage,
export, replay, or deployment behavior.

The board is a campaign checkpoint surface only. It does not create runtime behavior, endpoints, static UI assets,
storage, exports, replay execution, evidence-packet generation, automation, releases, tags, package publication,
container pushes, secrets, external targets, cloud targets, tenant targets, rulesets, or production claims.

## Counting Rules

A Phase 1 PR counts only after:

- the branch starts from clean, current `origin/main`;
- the branch diff stays inside the scoped slice;
- focused and full local verification pass;
- current-head PR Build/Test/Package/Smoke passes;
- current-head Analyze Java / CodeQL passes;
- current-head Dependency Review passes when present;
- the PR is merged by the normal repository PR merge flow;
- local main is fast-forwarded to the merge commit;
- post-merge local verification passes;
- main CI and CodeQL are green for the merge commit.

Pending, failed, cancelled, stale, skipped-only, duplicate-only, or wrong-head checks do not count.

## Planned Slots

| Slot | Branch | Scope | Expected files | State |
| --- | --- | --- | --- | --- |
| DX-P1-G01 | `codex/decision-explorer-phase1-architecture` | Phase 1 architecture and scope contract | `DECISION_EXPLORER_PHASE1_ARCHITECTURE_SCOPE.md`, `DECISION_EXPLORER_PHASE1_CAMPAIGN_BOARD.md`, documentation guard, `SESSION_MANAGER.md` | merged-main-green as PR #360, merge `0fe9331a757973d93820bbae46b05ae53f8ba64a` |
| DX-P1-G02 | `codex/decision-explorer-phase1-dto-skeleton` | Backend DTO skeleton | Additive Decision Explorer DTO/record classes and unit tests | active-local |
| DX-P1-G03 | `codex/decision-explorer-phase1-builder` | Deterministic builder/service | Builder/service and tests for ordering, null safety, partial evidence, and deterministic output | planned |
| DX-P1-G04 | `codex/decision-explorer-phase1-api` | Read-only API surface | Controller/API docs/tests or additive response field with bounded behavior | planned |
| DX-P1-G05 | `codex/decision-explorer-phase1-ui-first-pass` | UI first pass | Static page or cockpit integration and resource tests | planned |
| DX-P1-G06 | `codex/decision-explorer-phase1-ui-navigation` | UI polish and reviewer navigation | Labels, empty states, ordering, README/trust-map/cockpit links, and tests | planned |
| DX-P1-G07 | `codex/decision-explorer-phase1-docs-examples` | Docs and examples | Reviewer examples grounded in tests and guard tests against overclaims | planned |
| DX-P1-G08 | `codex/decision-explorer-phase1-hardening` | Hardening | Edge-case coverage, null-safety checks, contract tests, and cross-link cleanup | planned |
| DX-P1-G09 | `codex/decision-explorer-phase1-final-handoff` | Final handoff | Handoff doc with PRs, merge SHAs, behavior, tests, safety audit, and Phase 2 recommendation | planned |

## Current Checkpoint

DX-P1-G01 merged-main-green as PR #360 at merge commit `0fe9331a757973d93820bbae46b05ae53f8ba64a`.

DX-P1-G01 PR CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26491188794.

DX-P1-G01 PR CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26491188818.

DX-P1-G01 main CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26491392315.

DX-P1-G01 main CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26491392313.

DX-P1-G02 starts from clean main at `0fe9331a757973d93820bbae46b05ae53f8ba64a`.

Current branch: `codex/decision-explorer-phase1-dto-skeleton`.

Current PR: https://github.com/RicheyWorks/LoadBalancerPro/pull/361.

Current head SHA: tracked by PR #361 current head after PR-created checkpoint and current-head guard repair commits.

Next action: commit and push the PR-created checkpoint, rerun current-head local verification as needed, wait for
current-head PR checks, merge only if green, verify post-merge main, and then continue to DX-P1-G03.

Decision: continue.

## Scope And Safety Audit

Phase 1 must stay additive and bounded. It must not remove existing evidence fields, mutate production routing or
scoring, mutate proxy behavior, add external services, add live-cloud dependencies, add tenant assumptions, add secrets,
create releases, create tags, publish packages, push containers, delete branches, weaken rulesets, weaken required
checks, or change GitHub settings.

Phase 1 does not prove production readiness, production certification, live-cloud validation, real-tenant validation,
runtime enforcement, benchmark/load/stress evidence, throughput/p95/p99 evidence, replay/export behavior, storage
behavior, evidence packet implementation, autonomous production action, or broader automation.
