# LASE Routing Intelligence Phase 6 Reviewer Evidence Normalization

Date: 2026-05-29

Campaign: LASE Routing Intelligence Phase 6 - Reviewer Evidence Normalization.

Status: PR4 panel vocabulary guard.

Classification: WARN / docs-test-only normalization campaign.

Started from main SHA: `9d135fa9e2d451cc35379e003da7aa35d15e1f45`.

Phase 5 closed with PR #437 at merge SHA `9d135fa9e2d451cc35379e003da7aa35d15e1f45`.

## Purpose

This anchor defines how reviewer-facing LASE and Decision Explorer evidence names should be grouped, cross-linked, and
bounded during Phase 6. It is documentation and guard-test only. It does not add endpoints, DTOs, runtime behavior,
storage/export behavior, evidence-packet generation, replay execution, routing/scoring/proxy/allocation behavior,
traffic shifting, runtime enforcement, Maven configuration, CI workflow behavior, Docker, Compose, secrets, cloud
targets, tenant targets, private-network targets, or external targets.

Phase 6 should improve reviewer evidence normalization and trust-map clarity without changing the current additive API
surface. Any future PR that touches `docs/API_CONTRACTS.md`, reviewer trust navigation, or agent handoff docs should
use this document as the naming and boundary anchor.

## PR Discipline

- Target: 10 PRs maximum.
- Work one separately scoped PR at a time.
- Do not start the next PR until the current PR is merged and main CI/CodeQL are green.
- Prefer docs/test-only changes.
- Keep `SESSION_MANAGER.md` current and concise.
- Log local, remote, scope, or tooling failures in `FAILURE_LOG.md` before continuing.
- Preserve branches; do not delete campaign branches.
- Keep all claims bounded to reviewer evidence, documentation, normalization, and local/test proof.

## Audit Findings For PR1

The PR1 audit found these reviewer-normalization gaps:

- Phase 5 was merged and main-green through PR #437, but `LASE_ROUTING_INTELLIGENCE_PHASE5_CLOSEOUT.md` still used
  candidate/pending closeout wording.
- `docs/API_CONTRACTS.md` documents the additive Decision Explorer evidence fields, but it does not yet point to a
  single naming map for reviewer-facing evidence groups.
- `docs/REVIEWER_TRUST_MAP.md` points reviewers to Decision Explorer Phase 1 and Phase 2 paths, but it does not yet
  name the Phase 6 normalization anchor for current LASE routing-intelligence evidence terms.
- Repeated safety wording is mostly correct, but the normalized vocabulary should make unsupported claims easier to
  spot: reviewer evidence is not production readiness, runtime enforcement, benchmark proof, replay proof, export
  proof, or autonomous production action.

## Normalized Evidence Groups

Use these group names when documenting or cross-linking reviewer-facing LASE evidence:

| Group | API or UI field family | Reviewer meaning | Boundary |
| --- | --- | --- | --- |
| Decision Explorer payload | `DecisionExplorerPayloadV1` | Top-level same-origin local reviewer readout for routing comparison evidence. | Additive, read-only, simulation-only. |
| Confidence summary | `confidenceSummary` | Status, candidate confidence, factor status, warnings, unknowns, and explanation context. | Derived from returned evidence only. |
| Routing diagnostics | `routingDiagnostics` | Candidate, factor, evidence, degradation, partial-evidence, unknown-evidence, and explanation diagnostics. | Does not change routing decisions. |
| Route tradeoff analysis | `routeTradeoffAnalysis` | Selected-vs-alternative tradeoff rows, score-gap context, evidence sufficiency, replay readiness, factor deltas, fingerprints, and explanations. | Does not execute replay or mutate scores. |
| Shadow decision quality | `shadowDecisionQualityEvaluation` | Local shadow quality labels, candidate outcomes, policy sensitivity, scenario-input quality, fingerprints, and explanations. | Does not change final allocation. |
| Counterfactual analysis | `counterfactualAnalysis` | Local policy-weight sensitivity rows, candidate outcome rows, factor-weight delta rows, fingerprints, reproducibility keys, and explanations. | Does not retune live weights or recompute production scores. |
| Static reviewer page | `/decision-explorer.html` | Browser-local panels, reviewer badges, tables, copy summaries, warnings, unknowns, raw payload, and not-proven boundaries. | Same-origin, memory-only, no runtime files. |
| Scenario catalog | `GET /api/routing/decision-explorer/scenarios` | Deterministic local synthetic scenario metadata used for reviewer orientation. | Scenario selection alone does not run routing. |

## Naming Rules

- Use JSON field names exactly when describing API contracts: `confidenceSummary`, `routingDiagnostics`,
  `routeTradeoffAnalysis`, `shadowDecisionQualityEvaluation`, and `counterfactualAnalysis`.
- Use title-case panel names when describing the static page: `Routing Intelligence Status`, `Routing Diagnostics`,
  `Route Tradeoff Intelligence`, `Evidence Sufficiency`, `Replay Readiness`, `Shadow Decision Quality`, and
  `Counterfactual Analysis`.
- Use `not-proven boundaries` for reviewer-facing safety limits. Avoid swapping in stronger wording such as
  production proof, certification proof, or approval unless an implementation PR proves that exact behavior.
- Use `local-only`, `read-only`, `simulation-only`, `additive`, `same-origin`, and `memory-only` only when those
  boundaries are true for the specific surface being described.
- Keep `counterfactual` language tied to returned local evidence. It may describe bounded local interpretation rows; it
  must not imply live weight retuning, live score recomputation, or production traffic changes.
- Keep `replay readiness` distinct from `replay execution`. Readiness diagnostics and checklists do not execute replay.
- Keep `evidence packet readouts` distinct from evidence-packet generation, storage, export, upload, download, PDF, or
  ZIP behavior.

## Static-Page Panel Vocabulary Guard

PR4 locks the current `/decision-explorer.html` title-case panel labels to the normalized API field vocabulary. This is
a documentation/guard-test alignment pass only. It does not rename panels, does not change static-page behavior, does
not add fields, does not add endpoints, does not change schemas, does not execute replay, does not export or store
evidence, does not generate evidence packets, does not shift traffic, does not enforce runtime decisions, and does not
prove production readiness or create production action automation.

| Static-page panel label | Normalized reviewer-evidence group | Current API field or surface |
| --- | --- | --- |
| Scenario Catalog | Scenario catalog | `GET /api/routing/decision-explorer/scenarios` |
| Routing Intelligence Status | Confidence summary | `confidenceSummary` |
| Routing Diagnostics | Routing diagnostics | `routingDiagnostics` |
| Route Tradeoff Intelligence | Route tradeoff analysis | `routeTradeoffAnalysis` |
| Evidence Sufficiency | Route tradeoff analysis | `routeTradeoffAnalysis.evidenceSufficiency` |
| Replay Readiness | Route tradeoff analysis | `routeTradeoffAnalysis.replayReadinessDiagnostic` |
| Shadow Decision Quality | Shadow decision quality | `shadowDecisionQualityEvaluation` |
| Counterfactual Analysis | Counterfactual analysis | `counterfactualAnalysis` |
| Not-Proven Boundaries | Decision Explorer payload | `notProvenBoundaries` |

The guard should compare this table with the actual static page and reviewer documents. If a later PR intentionally
renames a panel or moves evidence between fields, update the page, `docs/API_CONTRACTS.md`,
`docs/REVIEWER_TRUST_MAP.md`, this anchor, and the guard in the same scoped change.

## Required Boundary Phrase Family

Phase 6 docs should preserve this boundary family when discussing reviewer-facing LASE evidence:

- no production routing change;
- no production scoring change;
- no proxy behavior change;
- no allocation behavior change;
- no replay execution;
- no storage or export proof;
- no evidence-packet generation proof;
- no traffic shifting;
- no runtime enforcement claim;
- no Maven, CI, Docker, or Compose behavior change;
- no secrets, cloud targets, tenant targets, private-network targets, or external targets;
- no production readiness;
- no production certification;
- no live-cloud validation;
- no real-tenant validation;
- no benchmark/load/stress proof;
- no throughput, p95, or p99 proof;
- no autonomous production action;
- no broader automation.

## Cross-Link Expectations

- `docs/API_CONTRACTS.md` should point readers here when contract prose needs the normalized Decision Explorer evidence
  vocabulary.
- `docs/REVIEWER_TRUST_MAP.md` should point readers here when reviewer navigation needs a current LASE naming anchor.
- Phase closeout docs should record whether their closeout gate is candidate/pending or completed/main-green, and stale
  candidate wording should be corrected after merge-health is proven.
- Guard tests should prefer source-visible string checks for exact headings, field names, merge SHAs, and not-proven
  boundary wording.

## Out Of Scope For PR1

PR1 does not rewrite all reviewer docs, rename fields, alter OpenAPI generation, change the static page, change runtime
API behavior, or introduce new evidence types. Later Phase 6 PRs may make narrow docs/test-only updates to trust-map
rows, API contract grouping, reviewer walkthroughs, examples, or stale wording guards after the current PR is merged
and main CI/CodeQL are green.

## Recommended PR5

After PR4 merges and main is green, PR5 should audit reviewer walkthrough or example docs for the same Phase 6
vocabulary and only add a narrow docs/test-only update if a concrete stale reviewer path remains.
