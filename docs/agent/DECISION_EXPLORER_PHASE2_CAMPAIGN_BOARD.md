# Decision Explorer Phase 2 Campaign Board

Status: active / phase2-scope.

Classification: WARN / decision-explorer-phase2-campaign.

Started from main SHA: `28c8bc10e1aa553a3c53aac70883c04431d55cc2`.

Current PR slot: DX-P2-G01.

Completed Phase 2 PRs: 0 / 12 planned.

Related architecture scope: [`DECISION_EXPLORER_PHASE2_ARCHITECTURE_SCOPE.md`](DECISION_EXPLORER_PHASE2_ARCHITECTURE_SCOPE.md).

Related Phase 1 handoff: [`DECISION_EXPLORER_PHASE1_FINAL_HANDOFF.md`](DECISION_EXPLORER_PHASE1_FINAL_HANDOFF.md).

## Purpose

This board tracks the Decision Explorer Implementation Phase 2 campaign. Phase 2 is additive, read-only,
same-origin/local-app-only, and simulation-only. It should enrich the Phase 1 Decision Explorer with deterministic
scenario catalog views, factor drill-down, candidate comparison, filtering, explanation badges, API hardening, UI
hardening, reviewer examples, and final handoff evidence without changing production routing, scoring, proxy, cloud,
tenant, storage, export, replay, or deployment behavior.

The board is a campaign checkpoint surface only. It does not create runtime behavior, endpoints, static UI behavior,
storage, exports, replay execution, evidence-packet generation, automation, releases, tags, package publication,
container pushes, registry logins, secrets, external targets, cloud targets, tenant targets, rulesets, GitHub settings,
or production claims.

## Counting Rules

A Phase 2 PR counts only after:

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
| DX-P2-G01 | `codex/decision-explorer-phase2-campaign-board` | Phase 2 campaign board and scope contract | `DECISION_EXPLORER_PHASE2_ARCHITECTURE_SCOPE.md`, `DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md`, documentation guard, `SESSION_MANAGER.md` | active-local / local verification passed |
| DX-P2-G02 | `codex/decision-explorer-phase2-scenario-catalog` | Scenario catalog model | Additive scenario catalog DTO/model support and unit tests | pending |
| DX-P2-G03 | `codex/decision-explorer-phase2-scenario-api` | Scenario catalog service/API | Bounded same-origin catalog API or additive companion data, controller tests, and docs | pending |
| DX-P2-G04 | `codex/decision-explorer-phase2-factor-drilldown` | Decision factor drill-down | Deterministic factor-level summaries and tests | pending |
| DX-P2-G05 | `codex/decision-explorer-phase2-candidate-comparison` | Candidate comparison table | Additive candidate comparison rows and tests for ordering, empty, and partial candidates | pending |
| DX-P2-G06 | `codex/decision-explorer-phase2-ui-scenarios` | UI scenario selector and filtering | Static page controls using same-origin data only | pending |
| DX-P2-G07 | `codex/decision-explorer-phase2-ui-drilldown-comparison` | UI factor drill-down and candidate comparison | Static page display for drill-down and comparison states | pending |
| DX-P2-G08 | `codex/decision-explorer-phase2-reviewer-badges` | Explanation badges and reviewer language | Reviewer-facing badges, docs language, and no-overclaim guard coverage | pending |
| DX-P2-G09 | `codex/decision-explorer-phase2-api-hardening` | API contract hardening | Compatibility, null-safety, ordering tests, and API docs updates | pending |
| DX-P2-G10 | `codex/decision-explorer-phase2-docs-examples` | Docs and examples | Grounded Phase 2 examples and unsupported-claim guard tests | pending |
| DX-P2-G11 | `codex/decision-explorer-phase2-final-polish` | Final hardening and navigation polish | Reviewer navigation cleanup, page labels, and edge-case cleanup | pending |
| DX-P2-G12 | `codex/decision-explorer-phase2-final-handoff` | Final handoff | Handoff doc with PRs, merge SHAs, behavior, tests, safety audit, and Phase 3 recommendation | pending |

## Current Checkpoint

Decision Explorer Implementation Phase 1 completed at main SHA `28c8bc10e1aa553a3c53aac70883c04431d55cc2`.

Phase 1 final handoff PR #368 merged as `28c8bc10e1aa553a3c53aac70883c04431d55cc2`.

DX-P2-G01 starts from clean main at `28c8bc10e1aa553a3c53aac70883c04431d55cc2`.

Current branch: `codex/decision-explorer-phase2-campaign-board`.

Current PR: pending.

Current head SHA before local edits: `28c8bc10e1aa553a3c53aac70883c04431d55cc2`.

Current Phase 2 focus: define the campaign board, source-visible architecture/scope contract, counting rules, planned
slot sequence, safety boundaries, and documentation guard tests before any runtime implementation.

DX-P2-G01 local verification passed before PR creation:

- focused Phase 2/Phase 1 scope selector passed with 22 tests;
- relevant Decision Explorer selector passed with 125 tests;
- `mvn -q test` passed;
- `mvn -q "-DskipTests" package` passed;
- `mvn -B package` passed with 2,682 tests, 0 failures, 0 errors, and 0 skipped;
- `git diff --check` passed with line-ending warnings only;
- `git diff --cached --check` passed;
- `git diff --check origin/main...HEAD` passed;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote ignored target-local evidence only.

Next action: commit, push, open the DX-P2-G01 PR, wait for current-head checks, and merge only if green.

Decision: continue.

## Scope And Safety Audit

Phase 2 must stay additive and bounded. It must not remove existing evidence fields, mutate production routing or
scoring, mutate proxy behavior, add external services, add live-cloud dependencies, add tenant assumptions, add secrets,
create releases, create tags, publish packages, push containers, delete branches, weaken rulesets, weaken required
checks, change GitHub settings, or add registry work.

Phase 2 does not prove production readiness, production certification, live-cloud validation, real-tenant validation,
runtime enforcement, benchmark/load/stress evidence, throughput/p95/p99 evidence, replay execution, export behavior,
storage behavior, evidence packet generation, autonomous production action, traffic shifting, carbon-aware routing,
GPU orchestration, power/grid control, facility automation, or broader automation.
