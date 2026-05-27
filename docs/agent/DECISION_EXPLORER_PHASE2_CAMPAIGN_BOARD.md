# Decision Explorer Phase 2 Campaign Board

Status: active / phase2-ui-scenarios.

Classification: WARN / decision-explorer-phase2-campaign.

Started from main SHA: `28c8bc10e1aa553a3c53aac70883c04431d55cc2`.

Current PR slot: DX-P2-G06.

Completed Phase 2 PRs: 5 / 12 planned.

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
| DX-P2-G01 | `codex/decision-explorer-phase2-campaign-board` | Phase 2 campaign board and scope contract | `DECISION_EXPLORER_PHASE2_ARCHITECTURE_SCOPE.md`, `DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md`, documentation guard, `SESSION_MANAGER.md` | merged-main-green / PR #369 / `1e75b7326b09cd7c179909aec00f0c42e34da9c1` |
| DX-P2-G02 | `codex/decision-explorer-phase2-scenario-catalog` | Scenario catalog model | Additive scenario catalog DTO/model support and unit tests | merged-main-green / PR #370 / `1fb16a50d4181d1411abfe6c038815a68f79e7b5` |
| DX-P2-G03 | `codex/decision-explorer-phase2-scenario-api` | Scenario catalog service/API | Bounded same-origin catalog API or additive companion data, controller tests, and docs | merged-main-green / PR #371 / `186b28db1d261858a42db2ed75531fb3e4930f44` |
| DX-P2-G04 | `codex/decision-explorer-phase2-factor-drilldown` | Decision factor drill-down | Deterministic factor-level summaries and tests | merged-main-green / PR #372 / `b2f5017e4c7484e34d0da6a1ffde3954442a9103` |
| DX-P2-G05 | `codex/decision-explorer-phase2-candidate-comparison` | Candidate comparison table | Additive candidate comparison rows and tests for ordering, empty, and partial candidates | merged-main-green / PR #373 / `64394f1380708a63d70ad9e5ec1a2ad3589a9780` |
| DX-P2-G06 | `codex/decision-explorer-phase2-ui-scenarios` | UI scenario selector and filtering | Static page controls using same-origin data only | active-pr / PR #374 / current-head checks pending |
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

Current branch: `codex/decision-explorer-phase2-ui-scenarios`.

Current PR: https://github.com/RicheyWorks/LoadBalancerPro/pull/374.

Current head SHA before PR-created checkpoint: `c13b56cb38518160cfc1a754a50e9c0eeeefea28`.

Current Phase 2 focus: add static Decision Explorer scenario selector and filtering controls that read the bounded
same-origin `GET /api/routing/decision-explorer/scenarios` metadata surface. The controls keep scenario selection as
reviewer orientation only and do not run routing, mutate decisions, persist storage, export data, execute replay,
generate evidence packets, or call external systems.

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

DX-P2-G01 PR #369 opened from the current branch after local verification and merged as
`1e75b7326b09cd7c179909aec00f0c42e34da9c1`.

DX-P2-G01 PR URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/369.

DX-P2-G01 post-merge main verification passed:

- `mvn -q test` passed;
- `mvn -q "-DskipTests" package` passed;
- `mvn -B package` passed with 2,682 tests, 0 failures, 0 errors, and 0 skipped;
- `git diff --check` passed;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote ignored target-local evidence only;
- main CI passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26505311136;
- main CodeQL passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26505311135.

DX-P2-G02 starts from clean main at `1e75b7326b09cd7c179909aec00f0c42e34da9c1`.

DX-P2-G02 adds `DecisionExplorerScenarioCatalogV1`, `DecisionExplorerScenarioV1`, and focused unit tests. The slice
adds model support only; it does not expose a new endpoint, change static UI behavior, persist storage, export data,
execute replay, generate evidence packets, call external systems, or change routing/scoring/proxy behavior.

DX-P2-G02 local verification passed before PR creation:

- focused scenario catalog and Phase 2 guard selector passed with 20 tests;
- relevant Decision Explorer selector passed with 132 tests;
- `mvn -q test` passed;
- `mvn -q "-DskipTests" package` passed;
- `mvn -B package` passed with 2,689 tests, 0 failures, 0 errors, and 0 skipped;
- `git diff --check` passed with line-ending warnings only;
- `git diff --cached --check` passed;
- `git diff --check origin/main...HEAD` passed;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote ignored target-local evidence only.

DX-P2-G02 PR #370 opened from the current branch after local verification and merged as
`1fb16a50d4181d1411abfe6c038815a68f79e7b5`.

DX-P2-G02 PR URL: https://github.com/RicheyWorks/LoadBalancerPro/pull/370.

DX-P2-G02 post-merge main verification passed:

- `mvn -q test` passed;
- `mvn -q "-DskipTests" package` passed;
- `mvn -B package` passed with 2,689 tests, 0 failures, 0 errors, and 0 skipped;
- `git diff --check` passed;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote ignored target-local evidence only;
- main CI passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26506855450;
- main CodeQL passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26506855449.

DX-P2-G03 starts from clean main at `1fb16a50d4181d1411abfe6c038815a68f79e7b5`.

DX-P2-G03 adds `DecisionExplorerScenarioCatalogService`, `GET /api/routing/decision-explorer/scenarios`,
controller coverage, OpenAPI coverage, and API contract documentation. The slice exposes deterministic local synthetic
scenario metadata only; it does not run routing, change scoring, mutate proxy behavior, persist storage, export data,
execute replay, generate evidence packets, or call external systems.

DX-P2-G03 focused verification passed with 33 tests:

- `DecisionExplorerScenarioCatalogServiceTest`;
- `DecisionExplorerScenarioCatalogV1Test`;
- `RoutingControllerTest`;
- `RoutingOpenApiContractTest`.

DX-P2-G03 focused verification plus the Phase 2 documentation guard passed with 41 tests.

DX-P2-G03 relevant Decision Explorer selector passed with 158 tests.

DX-P2-G03 full local verification passed before PR creation:

- `mvn -q test` passed;
- `mvn -q "-DskipTests" package` passed;
- `mvn -B package` passed with 2,695 tests, 0 failures, 0 errors, and 0 skipped;
- `git diff --check` passed with line-ending warnings only;
- `git diff --cached --check` passed;
- `git diff --check origin/main...HEAD` passed;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote ignored target-local evidence only.

DX-P2-G03 committed as `eb6098337fc83b44f5b2c657652f8fd522eaf104`.

DX-P2-G03 PR #371 opened from the current branch after local verification:
https://github.com/RicheyWorks/LoadBalancerPro/pull/371.

DX-P2-G03 current-head PR checks passed:

- PR CI passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26507801146;
- PR CodeQL passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26507801273;
- Dependency Review passed in the PR CI run.

DX-P2-G03 merged as `186b28db1d261858a42db2ed75531fb3e4930f44`.

DX-P2-G03 post-merge main verification passed:

- `mvn -q test` passed;
- `mvn -q "-DskipTests" package` passed;
- `mvn -B package` passed with 2,695 tests, 0 failures, 0 errors, and 0 skipped;
- `git diff --check` passed;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote ignored target-local evidence only;
- main CI passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26508078565;
- main CodeQL passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26508078451.

DX-P2-G04 starts from clean main at `186b28db1d261858a42db2ed75531fb3e4930f44`.

DX-P2-G04 is adding additive `DecisionFactorDrilldownV1` readouts and builder tests. The slice derives reviewer
drill-down fields from already-returned `ScoreFactorContributionResponse` factor contribution evidence only; it does
not recompute scores, change routing/scoring/proxy behavior, persist storage, export data, execute replay, generate
evidence packets, or call
external systems.

DX-P2-G04 focused selector initially failed on a test expectation mismatch for returned factor direction. The failure
was logged in `FAILURE_LOG.md`, repaired by preserving the returned influence category, and rerun.

DX-P2-G04 focused verification passed with 23 tests:

- `DecisionFactorDrilldownV1Test`;
- `DecisionExplorerPayloadV1Test`;
- `DecisionExplorerPayloadServiceTest`;
- `RoutingOpenApiContractTest`;
- `AgentDecisionExplorerPhase2ArchitectureScopeDocumentationTest`.

DX-P2-G04 relevant Decision Explorer selector first hit a tooling timeout. The failure was logged in
`FAILURE_LOG.md`, process inspection found no lingering Maven/Java process, and the explicit selector rerun passed
with 140 tests.

DX-P2-G04 full local verification passed before PR creation:

- `mvn -q test` passed;
- `mvn -q "-DskipTests" package` passed;
- `mvn -B package` passed with 2,696 tests, 0 failures, 0 errors, and 0 skipped;
- `git diff --check` passed with line-ending warnings only;
- `git diff --cached --check` passed;
- `git diff --check origin/main...HEAD` passed;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote ignored target-local evidence only.

DX-P2-G04 committed as `9b3ed5d6f677505375a80e09e8c38c1d3ec31f14`.

DX-P2-G04 PR #372 opened from the current branch after local verification:
https://github.com/RicheyWorks/LoadBalancerPro/pull/372.

DX-P2-G04 current-head PR checks passed after the failure-log checkpoint:

- PR CI passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26534430186;
- PR CodeQL passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26534430242;
- Dependency Review passed in the PR CI run.

DX-P2-G04 merged as `b2f5017e4c7484e34d0da6a1ffde3954442a9103`.

DX-P2-G04 post-merge main verification passed:

- `mvn -q test` passed;
- `mvn -q "-DskipTests" package` passed;
- `mvn -B package` passed with 2,696 tests, 0 failures, 0 errors, and 0 skipped;
- `git diff --check` passed;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote ignored target-local evidence only;
- main CI passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26534775988;
- main CodeQL passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26534776086.

DX-P2-G05 starts from clean main at `b2f5017e4c7484e34d0da6a1ffde3954442a9103`.

DX-P2-G05 is adding additive `DecisionExplorerCandidateComparisonRowV1` readouts and builder tests. The slice derives
candidate comparison rows from already-built `CandidateReadoutV1` evidence only; it does not recompute scores, change
routing/scoring/proxy behavior, persist storage, export data, execute replay, generate evidence packets, or call
external systems.

DX-P2-G05 focused verification passed with 23 tests:

- `DecisionExplorerCandidateComparisonRowV1Test`;
- `DecisionExplorerPayloadV1Test`;
- `DecisionExplorerPayloadServiceTest`;
- `RoutingOpenApiContractTest`;
- `AgentDecisionExplorerPhase2ArchitectureScopeDocumentationTest`.

DX-P2-G05 relevant Decision Explorer selector passed with 159 tests.

DX-P2-G05 full local verification passed before PR creation:

- `mvn -q test` passed;
- `mvn -q "-DskipTests" package` passed;
- `mvn -B package` passed with 2,697 tests, 0 failures, 0 errors, and 0 skipped;
- `git diff --check` passed with line-ending warnings only;
- `git diff --cached --check` passed;
- `git diff --check origin/main...HEAD` passed;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote ignored target-local evidence only.

DX-P2-G05 committed as `dfa9baa73695cfc7ce4a2264617ce193077bc482`.

DX-P2-G05 PR #373 opened from the current branch after local verification:
https://github.com/RicheyWorks/LoadBalancerPro/pull/373.

DX-P2-G05 current-head PR checks passed:

- PR CI and Dependency Review passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26536161903;
- duplicate PR CI run passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26536158642;
- PR CodeQL passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26536160452.

DX-P2-G05 merged as `64394f1380708a63d70ad9e5ec1a2ad3589a9780`.

DX-P2-G05 post-merge main verification passed:

- `mvn -q test` passed;
- `mvn -q "-DskipTests" package` passed;
- `mvn -B package` passed with 2,697 tests, 0 failures, 0 errors, and 0 skipped;
- `git diff --check` passed;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote ignored target-local evidence only;
- main CI passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26536519136;
- main CodeQL passed: https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26536519094.

DX-P2-G06 starts from clean main at `64394f1380708a63d70ad9e5ec1a2ad3589a9780`.

DX-P2-G06 is adding same-origin static page scenario selector and filtering controls. The slice reads
`DecisionExplorerScenarioCatalogV1` metadata, filters locally by scenario category and evidence status, and displays
the selected scenario as reviewer orientation only. It does not change routing/scoring/proxy behavior, persist storage,
export data, execute replay, generate evidence packets, or call external systems.

DX-P2-G06 focused verification passed with 38 tests:

- `DecisionExplorerStaticPageTest`;
- `DecisionExplorerReviewerNavigationTest`;
- `RoutingControllerTest`;
- `AgentDecisionExplorerPhase2ArchitectureScopeDocumentationTest`.

DX-P2-G06 relevant Decision Explorer selector passed with 160 tests.

DX-P2-G06 full local verification passed before PR creation:

- `mvn -q test` passed;
- `mvn -q "-DskipTests" package` passed;
- `mvn -B package` passed with 2,698 tests, 0 failures, 0 errors, and 0 skipped;
- `git diff --check` passed with line-ending warnings only;
- `git diff --cached --check` passed;
- `git diff --check origin/main...HEAD` passed;
- `.\scripts\smoke\enterprise-lab-workflow.ps1 -Package` passed and wrote ignored target-local evidence only.

DX-P2-G06 local rendered-page verification passed on `http://127.0.0.1:18080/decision-explorer.html`: the packaged
app served the static page, the same-origin Scenario Catalog loaded, `PARTIAL_EVIDENCE` category filtering narrowed the
visible catalog rows to the partial-evidence scenario, and browser console errors were empty. The local process was
stopped after verification.

DX-P2-G06 committed as `c13b56cb38518160cfc1a754a50e9c0eeeefea28`.

DX-P2-G06 PR #374 opened from the current branch after local verification:
https://github.com/RicheyWorks/LoadBalancerPro/pull/374.

Next action: push the PR-created checkpoint update, wait for PR #374 current-head checks, and merge only after
Build/Test/Package/Smoke, Analyze Java / CodeQL, and Dependency Review are current-head green.

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
