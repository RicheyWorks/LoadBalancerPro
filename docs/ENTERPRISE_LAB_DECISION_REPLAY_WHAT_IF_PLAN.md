# Enterprise Lab Decision Replay / What-If Plan

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. This plan defines a future Decision Replay / What-If lane for reviewer-facing routing explainability. It is a planning contract only: replay execution and what-if execution are planned/not implemented.

## Purpose

Decision Replay / What-If analysis should eventually help reviewers answer:

- Why did the Enterprise Lab choose this backend?
- What visible factor appeared decisive for the selected backend?
- What would have changed if one visible input signal had been different?
- Which alternative backend would have become more plausible under a bounded lab-only change?

The lane is meant to turn an existing read-only Decision Vector into a controlled lab explanation path. It is not production traffic replay, not live cloud validation, not production telemetry, and not a production certification mechanism.

## Current State After Decision Vector Exposure

PR #176 exposed `results[].decisionVector` on the read-only `POST /api/routing/compare` response. That response can now carry selected strategy, selected backend, candidate vectors, known visible signals, unknown or unexposed signals, factor contributions for current `ServerScoreCalculator` components, replay readiness, what-if readiness, lab proof boundaries, and production not-proven boundaries.

This is the prerequisite evidence layer for future replay/what-if work because it separates known visible lab inputs from hidden or unavailable signals. It does not execute replay. It does not execute what-if experiments. It does not persist decisions. It does not create a replay endpoint.

## Planned Replay Input Model

A future replay request should be based on a captured controlled lab decision, not live traffic. The planned input model should include:

| Field | Purpose |
| --- | --- |
| `sourceDecisionVector` | The read-only Decision Vector returned by `/api/routing/compare`. |
| `originalRequestSnapshot` | The original controlled lab request payload used to produce the decision. |
| `selectedStrategy` | The strategy from the original decision. |
| `selectedBackend` | The backend selected by the original decision. |
| `candidateBackends` | Candidate state vectors visible in the original lab request or response. |
| `knownVisibleSignals` | Explicit visible inputs such as health, latency, load, queue, error, capacity, weight, and network-awareness fields. |
| `unknownOrUnexposedSignals` | Hidden routing internals, production telemetry, exact production scoring, and unavailable local fields. |
| `factorContributions` | Current calculator contribution entries when the Decision Vector exposes them. |
| `replayScope` | A lab-only scope marker such as `controlledLabOnly`. |
| `requestedComparison` | The future reviewer question, such as baseline replay or one bounded what-if mutation. |

The planned replay model must preserve the original decision as immutable evidence. A future implementation should build a copied candidate set for comparison rather than mutating the captured original decision.

## Planned What-If Mutation Model

A future what-if request should apply exactly one bounded mutation to a copied controlled lab candidate input. The planned mutation model should include:

| Field | Purpose |
| --- | --- |
| `mutationId` | Reviewer-facing id for the local what-if question. |
| `candidateId` | Candidate backend/server whose visible signal is changed. |
| `signalName` | One allowed visible lab signal, for example health, p95 latency, recent error rate, in-flight request count, queue depth, configured capacity, estimated concurrency limit, weight, or network-awareness signal. |
| `originalValue` | Value from the original controlled lab input. |
| `mutatedValue` | Proposed lab-only value after validation. |
| `validationBoundary` | Field-specific validation, such as finite non-negative latency or rate between 0 and 1. |
| `expectedOutputShape` | Baseline selected backend, what-if selected backend, changed/not changed marker, decisive visible factors, and remaining unknowns. |

The planned model should reject multiple simultaneous mutations until single-factor explanations are verified. It should also reject hidden or unexposed signals. Unknown production telemetry, hidden production weights, private backend state, and exact production scoring must stay unavailable unless a future API explicitly exposes them.

## Safe What-If Questions

Safe future questions are constrained to visible local lab signals:

- If `edge-beta` p95 latency dropped from 120 ms to 45 ms, would the selected backend change?
- If `edge-alpha` became unhealthy, which healthy candidate would the strategy prefer?
- If `edge-beta` recent error rate rose from 0.02 to 0.25, would it become less plausible?
- If `edge-alpha` in-flight request count doubled, would the same strategy still select it?
- If `edge-drain` recovered from unhealthy to healthy, would visible factors make it eligible?
- If queue depth changed for one candidate, which factor contribution would move most?

Unsafe questions remain out of scope:

- Replaying production traffic.
- Mutating real backend state.
- Reading production telemetry or production monitoring streams.
- Inferring hidden scoring or hidden strategy internals.
- Validating live cloud behavior or real tenant behavior.
- Producing SLA/SLO evidence or production certification.

## Exactness Boundaries

Future replay/what-if output must be explicit about exactness:

- Exact only for visible controlled lab request fields and returned read-only Decision Vector fields.
- Exact only for current local calculator contribution fields when those fields are exposed.
- Not exact for production traffic behavior.
- Not exact for hidden routing internals.
- Not exact for production telemetry, production monitoring, live cloud behavior, or real tenant behavior.
- Not exact for future strategy plugins unless a future sprint implements and verifies plugin explainability.

If a future what-if result changes the selected backend, the explanation should say which visible lab mutation and visible factor contribution changed the local comparison. If it does not change, the explanation should say which visible factors kept the same selected backend plausible. In both cases, unknown or unexposed signals must remain explicit boundaries.

## Why Replay Is Not Production Proof

Decision Replay / What-If analysis is controlled lab interpretation. It is not production proof because:

- No production traffic is replayed.
- No live backend is mutated.
- No live cloud environment is validated.
- No real tenant environment is validated.
- No production telemetry or external monitoring stream is read.
- No SLA/SLO proof is produced.
- No registry publication, container signing, release, tag, or GitHub Release is performed.
- No production certification is claimed.

Even a future local replay implementation would prove only that a copied controlled lab input can be compared under documented rules in the current codebase.

## Safety Boundaries

This planning lane keeps these boundaries:

- No live replay endpoint exists.
- No `/api/routing/replay`, `/api/routing/what-if`, or `/api/routing/decision-replay` endpoint is added.
- No production traffic replay is implemented.
- No real backend mutation is implemented.
- No server-side export files are generated.
- No external storage is read or written.
- No external telemetry, analytics, CDN, upload/share endpoint, PDF/ZIP generation, registry login/publish, signing, release/tag/GitHub Release, live cloud validation, private-network validation, or GitHub settings/rulesets/secrets/environment mutation is introduced.
- No routing behavior, scoring behavior, strategy weights, proxy behavior, or external runtime behavior changes.
- No hidden scoring is invented.

## Future Implementation Phases

1. Documentation and guardrails: define this plan, link it from reviewer docs, and test the planned/not-implemented safety boundary.
2. Contract sketch: add design-only DTO names or examples if useful, still without exposing an endpoint or implying completion.
3. Pure local replay service spike: copy a controlled lab input in memory and compare baseline versus one mutation in unit tests only.
4. Read-only local API proposal: only after the pure service is verified, propose a protected local endpoint contract with no storage, no external calls, and no production claims.
5. Cockpit rendering proposal: display future replay/what-if results as read-only local lab interpretation, keeping unknowns and not-proven limits visible.
6. Evidence hardening: add regression fixtures for unchanged selection, changed selection, all-unhealthy cases, invalid mutation rejection, and exactness boundary language.

Each phase requires separate review. This sprint completes phase 1 only.
