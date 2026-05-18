# Enterprise Lab Decision Delta Analysis

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

Selected-vs-Closest-Alternative Decision Delta Analysis is a read-only interpretation layer for controlled lab routing decisions. It compares the selected candidate with the closest scored non-selected candidate so reviewers can inspect which returned factor contribution differences separated those two candidates in the local lab response.

The lane is derived from existing `POST /api/routing/compare` response data:

- final score values already returned in `results[].scores`;
- candidate vectors already returned in `results[].decisionVector`;
- factor contribution entries already returned as `ScoreFactorContributionResponse` values.

It does not recompute scores from raw server fields, does not duplicate `ServerScoreCalculator`, does not retune weights, does not change strategy behavior, does not change server selection, does not mutate proxy behavior, and does not replace existing reason strings, score fields, Decision Vector data, or Dominant Factor Analysis.

## What It Adds

For each routing comparison result, the analysis:

- identifies the selected candidate from the returned Decision Vector;
- identifies the closest non-selected alternative using finite returned final score values;
- resolves equal score-gap ties by stable candidate/server id ordering;
- compares only shared factor names where both selected and alternative candidates returned finite contribution values;
- reports factor name, selected candidate contribution, alternative candidate contribution, contribution delta, and absolute delta;
- identifies the largest absolute factor delta when available;
- reports the final score gap as selected score minus alternative score.

The lane uses neutral score language. It reports contribution differences and final score gaps; it does not infer hidden advantage, penalty, production causality, or hidden score semantics.

## Empty, Partial, Or Missing Data

If the selected candidate, closest alternative, final scores, or Decision Vector data are unavailable, the analysis returns `UNKNOWN`.

If final scores are available but factor contribution data is empty, partial, missing on one side, null, or non-finite, the analysis returns `PARTIAL` and omits unsafe factor deltas instead of inventing zero values.

The lane never borrows factor data from a non-closest alternative and never fills missing selected or alternative factor values from raw server fields.

## Reviewer Use

Reviewers can use this lane to answer narrow lab questions:

- Which non-selected candidate was closest to the selected candidate by returned final score?
- What was the selected-minus-alternative final score gap?
- Which shared returned factor contribution had the largest absolute difference?
- Which factor differences are unknown because one side did not return finite contribution data?

This can help explain why two visible candidates looked close or far apart under controlled lab response data. It is not proof of production routing behavior.

## Safety Boundaries

Decision Delta Analysis is read-only lab explainability only. It does not change routing behavior, scoring math, strategy weights, server selection logic, proxy behavior, existing API fields, or production deployment posture.

It does not prove production certification, live-cloud behavior, real-tenant behavior, SLA/SLO achievement, registry publication, signing status, governance application, production traffic validation, or exact production scoring.

It does not add live telemetry, external calls, upload/share endpoints, server-side PDF/ZIP/file export generation, private-network validation, release publication, registry login, signing, or GitHub settings mutation.
