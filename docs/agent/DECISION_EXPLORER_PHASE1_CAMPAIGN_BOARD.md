# Decision Explorer Phase 1 Campaign Board

Status: active / phase1-hardening.

Classification: WARN / decision-explorer-phase1-campaign.

Started from main SHA: `755ed394adfa18e462f89312c5289fd3154075f2`.

Current PR slot: DX-P1-G08.

Completed Phase 1 PRs: 7 / 9 planned.

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
| DX-P1-G02 | `codex/decision-explorer-phase1-dto-skeleton` | Backend DTO skeleton | Additive Decision Explorer DTO/record classes and unit tests | merged-main-green as PR #361, merge `fca765b897937cd20ee9955bfb7f9ba7a665a9be` |
| DX-P1-G03 | `codex/decision-explorer-phase1-builder` | Deterministic builder/service | Builder/service and tests for ordering, null safety, partial evidence, and deterministic output | merged-main-green as PR #362, merge `af351b043fbc3ff0ffff50d9c0f17a667f84b7af` |
| DX-P1-G04 | `codex/decision-explorer-phase1-api` | Read-only API surface | Controller/API docs/tests or additive response field with bounded behavior | merged-main-green as PR #363, merge `20b9080d5c24ef3807e15a3ef8367a8ef1ae4915` |
| DX-P1-G05 | `codex/decision-explorer-phase1-ui-first-pass` | UI first pass | Static page or cockpit integration and resource tests | merged-main-green as PR #364, merge `818540b424dc92df0ec59de68e456d0ce080adbf` |
| DX-P1-G06 | `codex/decision-explorer-phase1-ui-navigation` | UI polish and reviewer navigation | Labels, empty states, ordering, README/trust-map/cockpit links, and tests | merged-main-green as PR #365, merge `66242b7911c123b1f20f2820249b7173a3ef575a` |
| DX-P1-G07 | `codex/decision-explorer-phase1-docs-examples` | Docs and examples | Reviewer examples grounded in tests and guard tests against overclaims | merged-main-green as PR #366, merge `3d85730efc979373c2838e414c78c16df43656a9` |
| DX-P1-G08 | `codex/decision-explorer-phase1-hardening` | Hardening | Edge-case coverage, null-safety checks, contract tests, and cross-link cleanup | active-local |
| DX-P1-G09 | `codex/decision-explorer-phase1-final-handoff` | Final handoff | Handoff doc with PRs, merge SHAs, behavior, tests, safety audit, and Phase 2 recommendation | planned |

## Current Checkpoint

DX-P1-G01 merged-main-green as PR #360 at merge commit `0fe9331a757973d93820bbae46b05ae53f8ba64a`.

DX-P1-G01 PR CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26491188794.

DX-P1-G01 PR CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26491188818.

DX-P1-G01 main CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26491392315.

DX-P1-G01 main CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26491392313.

DX-P1-G02 merged-main-green as PR #361 at merge commit `fca765b897937cd20ee9955bfb7f9ba7a665a9be`.

DX-P1-G02 PR CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26492455493.

DX-P1-G02 PR CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26492455473.

DX-P1-G02 main CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26492690702.

DX-P1-G02 main CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26492690719.

DX-P1-G03 merged-main-green as PR #362 at merge commit `af351b043fbc3ff0ffff50d9c0f17a667f84b7af`.

DX-P1-G03 PR CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26493443317.

DX-P1-G03 PR CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26493443321.

DX-P1-G03 main CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26493648007.

DX-P1-G03 main CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26493648025.

DX-P1-G04 merged-main-green as PR #363 at merge commit `20b9080d5c24ef3807e15a3ef8367a8ef1ae4915`.

DX-P1-G04 PR CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26494750282.

DX-P1-G04 PR CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26494750255.

DX-P1-G04 main CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26495000093.

DX-P1-G04 main CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26495000098.

DX-P1-G05 merged-main-green as PR #364 at merge commit `818540b424dc92df0ec59de68e456d0ce080adbf`.

DX-P1-G05 PR CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26496094906.

DX-P1-G05 PR CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26496094904.

DX-P1-G05 main CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26496379571.

DX-P1-G05 main CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26496379570.

DX-P1-G06 merged-main-green as PR #365 at merge commit `66242b7911c123b1f20f2820249b7173a3ef575a`.

DX-P1-G06 PR CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26498224080.

DX-P1-G06 PR CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26498224028.

DX-P1-G06 main CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26498546830.

DX-P1-G06 main CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26498546610.

DX-P1-G07 merged-main-green as PR #366 at merge commit `3d85730efc979373c2838e414c78c16df43656a9`.

DX-P1-G07 guard: `AgentDecisionExplorerPhase1ReviewerExamplesDocumentationTest`.

DX-P1-G07 PR CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26499810472.

DX-P1-G07 PR CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26499810546.

DX-P1-G07 main CI: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26500117530.

DX-P1-G07 main CodeQL: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26500117476.

DX-P1-G08 starts from clean main at `3d85730efc979373c2838e414c78c16df43656a9`.

Current branch: `codex/decision-explorer-phase1-hardening`.

Current PR: pending.

Current head SHA before local edits: `3d85730efc979373c2838e414c78c16df43656a9`.

Current hardening focus: align Decision Explorer `notProvenBoundaries` with the now-implemented bounded endpoint and
static page without weakening the remaining storage/export/replay/evidence-packet and production-proof boundaries.

DX-P1-G08 local verification passed: focused service/API/static-page/docs selector, relevant Decision Explorer selector,
`mvn -q test`, `mvn -q "-DskipTests" package`, `mvn -B package` with 2,668 tests, diff checks, and
`.\scripts\smoke\enterprise-lab-workflow.ps1 -Package`.

Next action: commit and push the G08 hardening slice, open the PR, wait for current-head checks, and merge only if
green.

Decision: continue.

## Scope And Safety Audit

Phase 1 must stay additive and bounded. It must not remove existing evidence fields, mutate production routing or
scoring, mutate proxy behavior, add external services, add live-cloud dependencies, add tenant assumptions, add secrets,
create releases, create tags, publish packages, push containers, delete branches, weaken rulesets, weaken required
checks, or change GitHub settings.

Phase 1 does not prove production readiness, production certification, live-cloud validation, real-tenant validation,
runtime enforcement, benchmark/load/stress evidence, throughput/p95/p99 evidence, replay/export behavior, storage
behavior, evidence packet implementation, autonomous production action, or broader automation.
