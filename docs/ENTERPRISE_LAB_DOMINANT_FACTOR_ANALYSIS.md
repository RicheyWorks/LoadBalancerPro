# Enterprise Lab Dominant Factor Analysis

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

LoadBalancerPro's Dominant Factor Analysis lane is a read-only interpretation layer for controlled lab routing decisions. It summarizes the returned `ScoreFactorContributionResponse` entries so reviewers can quickly see which visible contribution most supported a candidate, which contribution carried the largest penalty or risk, and which returned factor had the largest absolute impact.

It is derived from the existing Decision Vector contribution data returned by `POST /api/routing/compare`. It does not recompute scores from raw server fields, does not change routing behavior, does not retune weights, does not change strategy behavior, does not change server selection, does not mutate proxy behavior, and does not replace the existing reason strings and score fields.

## What It Adds

For each candidate with returned contribution data, the analysis reports:

- The largest support-direction contributor when one is present.
- The largest penalty/risk contributor when one is present.
- The largest absolute numeric contribution when one is present.
- A deterministic explanation assembled only from returned factor names, directions, and contribution values.

For the selected decision, the analysis uses only the selected candidate's contribution list. It does not borrow a larger factor from a non-selected candidate, and it does not infer hidden routing internals.

Tie handling is deterministic. When multiple returned factors have the same impact, stable factor-name ordering decides the winner.

## Empty Or Missing Data

If factor contribution data is absent, empty, non-numeric, or not exposed, the analysis returns an unknown/empty state. It does not invent support factors, penalties, absolute impacts, confidence, production telemetry, or hidden scoring explanations.

## Reviewer Use

Reviewers can use the lane to answer narrow lab questions:

- Which returned factor most influenced this candidate's additive score?
- Was the selected candidate mostly affected by latency, pressure, error, health, or network-risk contributions?
- Which candidate carried a large penalty/risk contributor compared with the selected candidate?
- Which explanation gaps remain unknown because the API did not expose supporting contribution data?

This is useful for comparing candidates, but it is still local lab explainability. Lower additive score favors selection in the current calculator, so positive additive risk contributors usually weaken selection rather than certify that a server is better.

## Safety Boundaries

Dominant Factor Analysis is not proof of production readiness, production certification, live-cloud behavior, real-tenant behavior, SLA/SLO achievement, registry publication, signing status, or governance application.

It does not add live telemetry, external calls, upload/share endpoints, server-side PDF/ZIP/file export generation, private-network validation, release publication, registry login, signing, or GitHub settings mutation.

It remains an additive read-only API/UI layer over controlled lab response data.
