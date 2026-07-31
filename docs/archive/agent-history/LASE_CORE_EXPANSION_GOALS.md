# LASE Core Expansion Goals

## Campaign

- Campaign name: LASE Core Expansion Campaign.
- Classification: WARN.
- Current purpose: deepen Load-Aware Synthetic Evaluation and adaptive routing intelligence through reviewable, evidence-backed, PR-sized changes.
- Current campaign posture: goal-driven campaign planning and narrow implementation slices only.

This ledger is documentation/test-only. It does not add production code, Maven config, CI/workflow behavior, Dockerfile behavior, Compose behavior, scripts, runtime resources, endpoints, secrets, external targets, runner services, automation, or production-looking defaults.

## Purpose

The campaign exists to make LASE work durable across many small PRs. It should improve tail-latency-aware routing, adaptive load-aware scoring, degradation/recovery behavior, lab-mode concurrency or shedding experiments, explainable decisions, and scenario evidence without turning any single PR into a large architecture rewrite.

The ledger is not a completion claim. It is a planning and tracking surface for future scoped work. Each future PR must carry its own implementation, verification, and not-proven boundaries.

## Rules

- One goal means one small PR by default.
- Base each PR on current healthy `origin/main` unless a deliberate stacked-branch exception is documented.
- Do not open later implementation branches on top of an unmerged PR by default.
- Do not claim production readiness without proof.
- Do not claim production certification without proof.
- Do not claim real-world latency improvements without benchmark/load evidence.
- Do not claim live-cloud validation without actual live-cloud validation.
- Do not claim real-tenant validation without actual real-tenant validation.
- Preserve prior failures and recovery evidence; do not hide or rewrite them.
- Each PR must list exact verification evidence.
- Each PR must state not-proven boundaries.
- Failed, cancelled, stale, pending, missing, or duplicate-only required checks are not acceptable as green.
- Do not claim green main while required remote checks are pending.

## Goal States

- planned: scoped but not started.
- active: current branch or local work is being prepared.
- PR-opened: pull request exists and is awaiting required checks, review, or merge decision.
- merged-awaiting-main-green: PR merged, but merge-commit main checks are not yet proven green.
- merged/main-green: PR merged and post-merge main CI/CodeQL are green for the merge commit.
- paused/WARN: blocked by scope, verification, remote status, or human decision.
- closed/superseded: explicitly replaced by a different scoped goal.

## Common Not-Proven Boundaries For Every Goal

Unless a later separately scoped implementation and verification result explicitly changes a boundary, every goal preserves:

- no production readiness;
- no production certification;
- no live-cloud validation;
- no real-tenant validation;
- no runtime enforcement claim beyond implemented and verified code paths;
- no load/stress/benchmarking evidence unless those checks are actually run and reported;
- no throughput/p95/p99 production evidence;
- no replay/evidence/report/storage/export proof unless implemented and verified in that PR;
- no broader automation claim.

## Current First Slice Status

LASE-G01 corresponds to Slot 12 / PR #327: [Expand LASE tail-latency scoring and explainability](https://github.com/RicheyWorks/LoadBalancerPro/pull/327).

Observed during Slot 13 orientation and merge-health handling:

- PR #327 state: merged.
- PR #327 branch: `codex/lase-core-expansion-tail-latency-scoring`.
- PR #327 head SHA: `7a6779b96d97e5701a3654d94c1a1d60c04018cb`.
- PR #327 merge SHA: `38c30312640e2c7a5920a518d147cf93cdcd4d80`.
- PR #327 base: `main`.
- PR #327 mergeability: mergeable/clean when inspected.
- PR #327 remote checks observed during Slot 13: Build/Test/Package/Smoke passed, Analyze Java / CodeQL passed, Dependency Review passed.
- PR #327 post-merge main checks completed successfully for CI and CodeQL when this ledger was updated, so LASE-G01 is recorded as merged/main-green.

## Goals

### LASE-G01 - Tail-latency score breakdown and explanation

- Proposed PR title: Expand LASE tail-latency scoring and explainability.
- Primary scope: structured score breakdown and clearer power-of-two routing explanations.
- Expected changed areas: `ServerScoreBreakdown`, `ServerScoreCalculator`, `TailLatencyPowerOfTwoStrategy`, focused core tests.
- Success criteria: current score semantics remain stable; explanations name material penalty factors; tests cover tail latency, saturation, recovery/network risk, and score-breakdown stability.
- Verification expectations: focused scoring/routing tests, relevant LASE/routing selector bundle, full Maven tests, package checks, diff checks, enterprise lab package smoke, remote PR checks, and post-merge main checks before counting the goal.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: current healthy main.
- Initial status: merged/main-green as PR #327.

### LASE-G02 - Goal ledger and campaign guard

- Proposed PR title: Add LASE core expansion goal ledger.
- Primary scope: durable campaign ledger and documentation guard.
- Expected changed areas: `docs/agent/LASE_CORE_EXPANSION_GOALS.md`, README/reviewer trust links if useful, read-only documentation guard.
- Success criteria: at least 20 PR-sized goals are defined with statuses, scope, success criteria, verification, dependencies, and boundaries.
- Verification expectations: focused ledger guard, relevant agent/campaign documentation selector bundle, full Maven tests, package checks, diff checks, enterprise lab package smoke, and remote PR checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: PR #327 status must be represented factually without merging it.
- Initial status: active for this Slot 13 PR.

### LASE-G03 - ServerStateVector signal expansion

- Proposed PR title: Expand LASE server state vector signals.
- Primary scope: add or refine state-vector fields for recent tail latency, active concurrency, error pressure, and recovery state.
- Expected changed areas: `ServerStateVector`, score/routing tests, small scenario tests.
- Success criteria: new signals are immutable, deterministic, validation-safe, and covered by focused tests.
- Verification expectations: state-vector tests, scoring tests, routing selector bundle, full Maven/package/diff/smoke checks, and current-head remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G01 or equivalent score-explainability path merged/main-green.
- Initial status: merged/main-green as Slot 14 / PR #329: [Expand ServerStateVector signal modeling](https://github.com/RicheyWorks/LoadBalancerPro/pull/329).
- PR #329 final head SHA: `f24e973ddad0d631edb103b0a8f30d15474ef923`.
- PR #329 merge SHA: `efd1a09c32f99663a4f7612cd7fe8535716b9eae`.
- PR #329 post-merge main checks completed successfully for CI and CodeQL when this ledger was updated, so LASE-G03 is recorded as merged/main-green.
- This checkpoint keeps future LASE goals planned; it does not start LASE-G04.

### LASE-G04 - EWMA or rolling-window latency signal support

- Proposed PR title: Add bounded LASE latency-window signal support.
- Primary scope: introduce bounded EWMA or rolling-window latency signal handling if existing telemetry supports it.
- Expected changed areas: telemetry/state helpers and tests.
- Success criteria: signal handling is deterministic, bounded, and does not claim real-world p95/p99 proof.
- Verification expectations: focused signal tests, scoring/routing selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G03 or equivalent signal model.
- Initial status: merged/main-green as PR #331: [LASE-G04: Add latency window signal support](https://github.com/RicheyWorks/LoadBalancerPro/pull/331).
- PR #331 final head SHA: `e3644a34160ff37d68cff2a5302afcd1e55c48a1`.
- PR #331 merge SHA: `1f1cc1da6bb0bcb3430d660a5c579e008f109c40`.
- PR #331 post-merge main checks completed successfully for CI and CodeQL when this ledger was updated, so LASE-G04 is recorded as merged/main-green.

### LASE-G05 - Tail-latency hysteresis and anti-flapping

- Proposed PR title: Add LASE tail-latency anti-flapping guard.
- Primary scope: reduce unstable route switching when candidate scores are nearly tied.
- Expected changed areas: scoring/routing decision helper and focused tests.
- Success criteria: tests show stable behavior under small metric oscillations without hiding material degradation.
- Verification expectations: routing tests, anti-flapping scenario tests, selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G01 and any score-breakdown API needed for explainability.
- Initial status: active for branch `codex/lase-g05-tail-latency-hysteresis`; not merged/main-green.

### LASE-G06 - Recovery dampening refinement

- Proposed PR title: Refine LASE recovery dampening.
- Primary scope: improve how recovering servers re-enter candidate preference after degradation.
- Expected changed areas: score/state model and focused recovery tests.
- Success criteria: tests show bounded recovery penalty, no instant over-selection after one good signal, and eventual improvement as recovery evidence accumulates.
- Verification expectations: recovery-focused tests, LASE/routing selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G03 or equivalent recovery signal.
- Initial status: planned.

### LASE-G07 - Saturation and active-concurrency pressure model

- Proposed PR title: Strengthen LASE saturation scoring.
- Primary scope: strengthen active request, concurrency, and saturation pressure scoring.
- Expected changed areas: score calculator/state model and focused tests.
- Success criteria: saturated candidates receive explainable penalties while neutral healthy candidates retain stable behavior.
- Verification expectations: saturation tests, scoring/routing selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G01 and any state-vector signal work needed.
- Initial status: planned.

### LASE-G08 - Lab-mode adaptive concurrency limiter

- Proposed PR title: Add lab-mode adaptive concurrency limiter slice.
- Primary scope: introduce or refine a lab-mode limiter or limiter simulation.
- Expected changed areas: lab-mode limiter classes/tests only unless a narrower existing seam exists.
- Success criteria: limiter behavior is bounded, deterministic, opt-in, and does not alter production defaults broadly.
- Verification expectations: limiter tests, relevant routing/lab selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: current limiter design review and healthy main.
- Initial status: planned.

### LASE-G09 - Lab-mode adaptive load shedding

- Proposed PR title: Add lab-mode LASE load-shedding decision model.
- Primary scope: add a conservative lab/scenario-mode load-shedding decision model.
- Expected changed areas: lab/scenario model, tests, and bounded docs if needed.
- Success criteria: decisions are explainable, opt-in, and do not imply production enforcement.
- Verification expectations: load-shedding model tests, routing/lab selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G07 or equivalent saturation signal.
- Initial status: planned.

### LASE-G10 - Degradation scenario: tail-latency spike

- Proposed PR title: Add LASE tail-latency spike scenario.
- Primary scope: scenario coverage where one candidate develops high tail latency.
- Expected changed areas: scenario fixtures/tests and bounded evidence docs if needed.
- Success criteria: local tests show routing decisions shift away from the degraded candidate and explanations identify tail factors.
- Verification expectations: scenario test, LASE/routing selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G01.
- Initial status: planned.

### LASE-G11 - Degradation scenario: saturation and concurrency pressure

- Proposed PR title: Add LASE saturation degradation scenario.
- Primary scope: scenario coverage where active-concurrency pressure affects routing.
- Expected changed areas: scenario fixtures/tests and bounded evidence docs if needed.
- Success criteria: local tests show score and explanation responses to saturation pressure.
- Verification expectations: scenario test, saturation tests, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G07 or current pressure-scoring equivalent.
- Initial status: planned.

### LASE-G12 - Recovery scenario: brownout then return

- Proposed PR title: Add LASE brownout recovery scenario.
- Primary scope: scenario showing degraded server recovery without instant over-selection.
- Expected changed areas: scenario fixtures/tests and bounded evidence docs if needed.
- Success criteria: tests cover brownout, dampened recovery, and eventual candidate re-entry where supported.
- Verification expectations: recovery scenario tests, LASE/routing selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G06.
- Initial status: planned.

### LASE-G13 - Decision trace model

- Proposed PR title: Add LASE routing decision trace model.
- Primary scope: add a small immutable routing decision trace model for reviewer-friendly evidence.
- Expected changed areas: trace model classes/tests and docs if needed.
- Success criteria: trace captures selected candidate, considered candidates, score factors, decision reason, and explicit unknowns without leaking secrets.
- Verification expectations: trace model tests, score/routing selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G01 and current routing decision model.
- Initial status: planned.

### LASE-G14 - Decision replay fixture

- Proposed PR title: Add LASE decision replay fixture.
- Primary scope: add lab/test-only replay of recorded or synthetic routing decisions.
- Expected changed areas: test fixtures, replay fixture helpers, and bounded docs.
- Success criteria: replay fixture is deterministic, local/test-only, and does not claim production replay/report/storage/export proof.
- Verification expectations: replay fixture tests, LASE selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G13 or equivalent trace data.
- Initial status: planned.

### LASE-G15 - Scenario report artifact

- Proposed PR title: Add deterministic LASE scenario report artifact.
- Primary scope: produce a simple deterministic evidence artifact for LASE scenarios.
- Expected changed areas: test fixture/report code, ignored target output if needed, and documentation guard.
- Success criteria: artifact is deterministic, local, redacted where needed, and does not imply broad storage/export behavior.
- Verification expectations: artifact tests, diff checks proving no tracked generated output unless intentionally added, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G10 through LASE-G13 as needed.
- Initial status: planned.

### LASE-G16 - Strategy comparison harness

- Proposed PR title: Add LASE strategy comparison harness.
- Primary scope: compare baseline Power-of-Two and LASE-aware scoring in synthetic scenarios.
- Expected changed areas: comparison harness/tests and bounded docs.
- Success criteria: comparison is deterministic and labels local synthetic evidence only.
- Verification expectations: comparison harness tests, LASE/routing selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G10 and LASE-G11.
- Initial status: planned.

### LASE-G17 - Candidate sampling fairness and tie-break stability

- Proposed PR title: Harden LASE candidate sampling tie-break behavior.
- Primary scope: ensure deterministic tie-breaking or fairness behavior under equivalent candidates.
- Expected changed areas: routing strategy tests and narrowly scoped implementation if needed.
- Success criteria: equivalent candidates behave predictably and no accidental bias/regression is introduced.
- Verification expectations: tie-break tests, routing selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: current routing strategy behavior review.
- Initial status: planned.

### LASE-G18 - Explanation renderer for reviewer trust

- Proposed PR title: Add LASE explanation renderer.
- Primary scope: render score/decision breakdowns into stable human-readable explanations.
- Expected changed areas: renderer/helper tests and reviewer docs if needed.
- Success criteria: renderer is stable, readable, and avoids brittle exact wording while preserving material factors.
- Verification expectations: renderer tests, documentation guard if docs change, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G01 and LASE-G13.
- Initial status: planned.

### LASE-G19 - LASE evidence map update

- Proposed PR title: Update LASE evidence map.
- Primary scope: connect new LASE features to tests and reviewer paths.
- Expected changed areas: README, Reviewer Trust Map, repository evidence map, and documentation guard.
- Success criteria: reviewers can find implemented LASE evidence and not-proven boundaries without reading implementation first.
- Verification expectations: documentation guard, relevant agent/docs selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: a small set of merged/main-green implementation goals.
- Initial status: planned.

### LASE-G20 - Failure-injection fixtures for routing labs

- Proposed PR title: Add LASE failure-injection fixtures.
- Primary scope: add local deterministic fixtures for latency spike, network risk, error pressure, saturation, and recovery.
- Expected changed areas: test fixtures and scenario tests.
- Success criteria: fixtures are local, deterministic, non-networked unless explicitly test-scope loopback, and do not add automation.
- Verification expectations: fixture tests, scenario selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G10 through LASE-G12 or equivalent scenario needs.
- Initial status: planned.

### LASE-G21 - Adaptive threshold calibration tests

- Proposed PR title: Add LASE adaptive threshold calibration tests.
- Primary scope: test thresholds that decide when penalties materially affect routing.
- Expected changed areas: focused tests and small constant documentation if needed.
- Success criteria: thresholds have reviewer-readable rationale and regression tests without magic unreviewed behavior.
- Verification expectations: calibration tests, scoring/routing selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: LASE-G05 through LASE-G07.
- Initial status: planned.

### LASE-G22 - LASE CLI or lab scenario entrypoint

- Proposed PR title: Add LASE lab scenario entrypoint.
- Primary scope: add or refine a lab-only CLI/scenario entrypoint if existing CLI architecture supports it.
- Expected changed areas: CLI/lab entrypoint, tests, and bounded docs.
- Success criteria: entrypoint is explicit, local/lab-only, deterministic, and avoids production automation claims.
- Verification expectations: CLI tests, LASE/lab selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: scenario/report artifacts from earlier goals.
- Initial status: planned.

### LASE-G23 - Evidence audit guard for LASE claims

- Proposed PR title: Add LASE claim-boundary guard.
- Primary scope: add read-only documentation guard preventing unsupported LASE readiness/performance claims.
- Expected changed areas: documentation guard test and docs if necessary.
- Success criteria: guard catches unsupported production readiness, production certification, live-cloud, real-tenant, runtime enforcement, and benchmark/throughput/p95/p99 claims without blocking factual local evidence.
- Verification expectations: focused documentation guard, relevant docs selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: current README and reviewer trust wording.
- Initial status: planned.

### LASE-G24 - Main-green consolidation checkpoint

- Proposed PR title: Add LASE core expansion consolidation checkpoint.
- Primary scope: documentation checkpoint after a set of merged/main-green LASE PRs.
- Expected changed areas: goal ledger, reviewer map, campaign summary docs, and documentation guard.
- Success criteria: checkpoint lists only actually merged/main-green goals and preserves open not-proven boundaries.
- Verification expectations: documentation guard, relevant agent/docs selector bundle, full Maven/package/diff/smoke checks, and remote checks.
- Not-proven boundaries: common campaign not-proven boundaries apply.
- Dependencies: several earlier LASE goals merged/main-green.
- Initial status: planned.

## Suggested Next-Goal Order

1. Finish main-branch health confirmation for LASE-G01 before treating it as merged/main-green.
2. Merge LASE-G02 after current-head checks pass and review accepts the ledger.
3. Prefer LASE-G03 next only after LASE-G01 and LASE-G02 reach merged/main-green: ServerStateVector signal expansion.
4. Continue through LASE-G04, LASE-G05, LASE-G06, and LASE-G07 before adding broader lab-mode limiter or shedding behavior.
5. Add scenarios and evidence mapping only after the underlying deterministic model exists.

Goal order may change based on main health, conflicts, reviewer feedback, and safer seams discovered in code. Future PRs must not inherit unsupported claims from earlier PRs.
