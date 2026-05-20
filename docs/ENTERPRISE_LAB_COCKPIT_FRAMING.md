# Enterprise Lab Cockpit, Not a Demo

LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo.

The Enterprise Lab Cockpit provides controlled lab evidence, local reproducibility, and reviewer/operator explanations. It does not claim production certification, live-cloud proof, real-tenant proof, SLA/SLO proof, registry publication, container signing, governance application, production telemetry, or production monitoring proof.

## Product Identity

LoadBalancerPro should be presented as an Enterprise Lab Cockpit for controlled pre-production routing validation. The cockpit helps reviewers and operators inspect routing decisions, compare controlled lab scenarios, explain visible input signals, and follow evidence pages without changing production routing behavior.

This identity is intentionally strict. The project is not a casual demo, toy, mockup, playground, or sample-only page. It is a local lab validation and reviewer handoff surface with clear not-proven boundaries.

## What the Cockpit Monitors

- Active lab scenario.
- Routing comparison request state.
- Selected strategy.
- Selected backend/server.
- Backend health.
- Visible latency, load, active connection, capacity, and weight-style signals where exposed by the local lab response.
- Degradation, fallback, and recovery state.
- Scenario-to-scenario delta.
- Evidence association path.
- Reviewer handoff readiness.
- Local lab proof boundary.
- Production proof gaps.

## What Questions It Answers

- How was the selected backend chosen from the visible controlled lab response?
- How did the selected strategy influence backend selection?
- How did input signals influence the outcome?
- How should unhealthy, degraded, or recovery states be interpreted?
- How did the current lab scenario differ from the previous scenario?
- How do routing proof, scenario comparison, evidence navigation, timeline, and export packet relate?
- How can the reviewer reproduce and explain the same local lab proof?
- What is proven in the lab?
- What remains not proven for production?

## Controlled Lab Proof Chain

The proof chain starts with a controlled lab scenario, runs the same-origin local routing comparison request, displays selected strategy/backend details, summarizes visible signals, records scenario deltas, and points to reviewer evidence pages. Browser-local copy actions produce handoff text from visible page state only.

This chain proves local repeatability, controlled scenario interpretation, and reviewer/operator explanation quality for the current codebase. It does not prove production traffic behavior, production telemetry, live cloud execution, real tenant behavior, SLA/SLO achievement, registry publication, container signing, governance application, or production certification.

## Monitored Proof Chain

The Enterprise Lab Cockpit monitors the controlled lab scenario, routing comparison request state, selected strategy, selected backend/server, candidate backend health states, visible latency signal, visible load/connection pressure signal, capacity/weight signal when exposed, degradation/fallback/recovery state, scenario-to-scenario delta, evidence association path, reviewer handoff readiness, local lab proof boundary, and production proof gaps.

The monitored proof chain should read as:

controlled lab scenario -> visible input signals -> selected strategy -> selected backend/server -> comparison delta -> evidence association -> reviewer handoff.

This monitoring depth is reviewer-facing interpretation for controlled pre-production routing validation. It does not add production telemetry, production monitoring, live-cloud validation, real-tenant validation, registry operations, signing, or server-side export behavior.

## Decision Trace Depth

Decision trace depth explains why the selected backend appears to have won using only visible controlled lab response data: selected strategy, selected backend/server, returned reason text, candidate health, latency, load/connection pressure, capacity, weight, and scenario delta fields when those fields are exposed.

The cockpit may explain why alternatives were not selected only when visible signal comparison supports that interpretation. If the local lab response does not expose enough data, the correct reviewer note is that non-selected candidate reasons are unknown or only partially explainable. The cockpit must not invent hidden scoring, hidden production weights, or exact production scoring.

Known signals are the fields visible in the local lab request and same-origin routing comparison response. Unknown signals include missing local fields, unavailable API responses, exact scoring not exposed by the API, hidden production weighting, production telemetry, production monitoring, live-cloud behavior, and real-tenant behavior.

The investigation playbook should guide reviewers to inspect the Signal Interpretation Guide, Decision Chain Trace, Scenario Comparison, Evidence Associations, and Export Packet when a backend changes unexpectedly, does not change despite signal changes, changes under the same strategy, stays the same after a strategy change, or all candidates are unhealthy/degraded.

Decision trace depth remains controlled lab evidence for pre-production routing validation. It does not prove production traffic behavior, production telemetry, production monitoring, production certification, SLA/SLO achievement, live-cloud execution, real-tenant behavior, registry publication, container signing, or governance application.

## Alternative Candidate Evidence

Alternative candidate evidence compares the selected backend against non-selected candidates using visible controlled lab response data only. The cockpit can show selected vs non-selected candidate names, visible health state, visible latency signal, visible load/connection pressure, visible capacity/weight signal when exposed, and a reviewer-facing note about why a candidate appears favored or weakened.

Known candidate signals are fields present in the local lab request payload or same-origin routing comparison response. Unknown or unexposed candidate signals include missing candidate fields, unavailable local API responses, exact production scoring not exposed by the API, hidden routing internals, hidden production weighting, production telemetry, production monitoring, live-cloud behavior, and real-tenant behavior.

The cockpit must not invent hidden scoring. If visible signal comparison does not explain why a non-selected candidate lost, reviewers should record that the candidate reason is unknown or only partially explainable and treat the unknown as an investigation item. The correct next step is to inspect Decision Trace, Scenario Comparison, Evidence Associations, the Signal Interpretation Guide, and the Export Packet before making any production-facing claim.

Alternative candidate evidence remains controlled lab evidence for pre-production routing validation. It does not prove production traffic behavior, production telemetry proof, production monitoring proof, production certification, exact production scoring, live-cloud execution, real-tenant behavior, SLA/SLO achievement, registry publication, container signing, or governance application.

## Decision Vector Contract

[`ENTERPRISE_LAB_DECISION_VECTOR.md`](ENTERPRISE_LAB_DECISION_VECTOR.md) defines the structured explanation contract for one controlled lab routing decision. It turns final selected-backend outcomes into structured decision evidence: selected strategy, selected backend/server, candidate backend list, selected candidate vector, non-selected candidate vectors, visible candidate signals, known signals, unknown or unexposed signals, degradation/fallback indicators, reason categories, selected-vs-alternative notes, exact scoring availability or absence, factor contribution availability or absence, dominant factor analysis derived from returned contribution data, decision delta analysis derived from returned score and contribution data, Decision Replay Snapshot evidence derived from already-built compare response fields, Decision Replay Reconstruction Trace evidence steps derived from already-built compare response fields, Decision Replay Capsule canonical evidence packaging derived from already-built compare response fields, Decision Replay Readiness Checklist status derived from already-built evidence lane statuses and linked fingerprints, Decision Replay Evidence Source Map relationships derived from already-built evidence lane statuses and source field paths, Decision Replay Evidence Boundary Summary metadata derived from already-built boundary fields and statuses, Decision Replay Evidence Field Inventory metadata derived from already-built evidence field groups, Decision Evidence Null-Safety Summary metadata derived from already-built null/missing/unavailable and no-healthy/failure-path evidence, Decision Evidence Status Rollup metadata derived from already-built evidence lane statuses and boundary state, Decision Replay Evidence Lane Navigation Summary metadata derived from already-built response field paths, UI section labels, docs reference labels, and statuses, Decision Replay Evidence Lane Dependency Map metadata derived from already-built evidence lane dependency and downstream relationships, Decision Replay Evidence Lane Reference Index metadata derived from already-built evidence lane references, navigation metadata, and dependency counts, Decision Replay Evidence Lane Dependency Summary metadata derived only from the lane reference index, Decision Replay Evidence Lane Consistency Summary metadata derived only from existing status rollup, dependency map, reference index, and dependency summary surfaces, Decision Replay Evidence Reviewer Snapshot metadata derived only from existing status rollup, dependency map, reference index, dependency summary, and consistency summary surfaces, Decision Replay Evidence Reviewer Guidance metadata derived only from existing reviewer evidence metadata surfaces, Decision Replay Evidence Reviewer Handoff Summary metadata derived only from existing reviewer snapshot and guidance surfaces, Decision Replay Evidence Reviewer Closure Summary metadata derived only from existing reviewer snapshot, guidance, handoff, and status surfaces, and top-level Decision Replay Evidence Closure Rollup metadata derived only from returned per-result reviewer closure summaries.

The Candidate Decision Vector describes one visible backend/server with candidate id/name, selected true/false state, health state, latency signal, load or active connection pressure signal, capacity or weight signal when exposed, degradation warning, visible support signals, visible caution signals, unknown signals, why selected or why not selected when explainable, and fallback text when the candidate cannot be explained from visible data.

Decision Replay Evidence Reviewer Handoff Summary metadata derives only from existing reviewer snapshot and reviewer
guidance metadata surfaces. It derives deterministic reviewer handoff metadata from existing reviewer snapshot and guidance metadata. The read-only `/api/routing/compare` response can expose
`results[].decisionReplayEvidenceReviewerHandoffSummary` as additive reviewer handoff metadata without reflection,
new fingerprint generation, not production validation, not replay proof, not scoring proof, not approval,
not enforcement, not remediation, not correctness validation, not production readiness, not production certification,
and not guaranteed replay.

Decision Replay Evidence Reviewer Closure Summary metadata derives only from existing status rollup, dependency map,
lane reference index, dependency summary, consistency summary, reviewer snapshot, reviewer guidance, and reviewer
handoff response surfaces. The read-only `/api/routing/compare` response can expose
`results[].decisionReplayEvidenceReviewerClosureSummary` as additive reviewer closure metadata without reflection,
without new fingerprint generation, without production validation, without replay proof, without scoring proof, without
approval, enforcement, or remediation, without correctness validation, without production readiness, without
production certification, and without guaranteed replay.

Decision Replay Evidence Closure Rollup metadata derives only from already-returned
`results[].decisionReplayEvidenceReviewerClosureSummary` fields. The read-only `/api/routing/compare` response can
expose top-level `decisionReplayEvidenceReviewerClosureRollup` as additive reviewer metadata without reflection,
without new fingerprint generation, without replay execution, without what-if mutation, without scoring changes,
without routing behavior changes, without proxy behavior changes, without persistence, without telemetry, without
upload/share/download/export/PDF/ZIP behavior, without production validation, without correctness validation, without
production readiness, without production certification, and without guaranteed replay claims.

Decision Replay Evidence Closure Checklist metadata derives only from already-returned
`results[].decisionReplayEvidenceReviewerClosureSummary` fields and the top-level
`decisionReplayEvidenceReviewerClosureRollup`. The read-only `/api/routing/compare` response can expose top-level
`decisionReplayEvidenceReviewerClosureChecklist` as additive reviewer checklist metadata without reflection, without
new fingerprint generation, without replay execution, without what-if mutation, without scoring changes, without
routing behavior changes, without proxy behavior changes, without persistence, without telemetry, without
upload/share/download/export/PDF/ZIP behavior, without production validation, without correctness validation, without
production readiness, without production certification, and without guaranteed replay claims.

The Decision Vector contract keeps unknowns explicit. Exact production scoring is not claimed unless exposed by the API, hidden scoring must not be inferred, ServerScoreCalculator factor contribution extraction is additive for current local calculator components, and candidate factor contribution summaries can attach those calculator explanations to selected and non-selected candidate vectors for controlled lab review. [`ENTERPRISE_LAB_DOMINANT_FACTOR_ANALYSIS.md`](ENTERPRISE_LAB_DOMINANT_FACTOR_ANALYSIS.md) documents the additive read-only lane that identifies largest support, penalty/risk, and absolute-impact contributors from those returned entries only. [`ENTERPRISE_LAB_DECISION_DELTA_ANALYSIS.md`](ENTERPRISE_LAB_DECISION_DELTA_ANALYSIS.md) documents the additive read-only lane that compares the selected candidate with the closest scored non-selected alternative using returned final score gaps and shared finite contribution deltas only. [`ENTERPRISE_LAB_DECISION_REPLAY_SNAPSHOT.md`](ENTERPRISE_LAB_DECISION_REPLAY_SNAPSHOT.md) documents the additive read-only lane that records stable snapshot metadata and a deterministic local fingerprint from already-built compare evidence only. [`ENTERPRISE_LAB_DECISION_REPLAY_RECONSTRUCTION_TRACE.md`](ENTERPRISE_LAB_DECISION_REPLAY_RECONSTRUCTION_TRACE.md) documents the additive read-only lane that records deterministic reconstruction evidence steps and a deterministic local trace fingerprint from already-built compare evidence only. [`ENTERPRISE_LAB_DECISION_REPLAY_CAPSULE.md`](ENTERPRISE_LAB_DECISION_REPLAY_CAPSULE.md) documents the additive read-only lane that records canonical evidence packaging and a deterministic local capsule fingerprint from already-built compare evidence only. [`ENTERPRISE_LAB_DECISION_REPLAY_READINESS_CHECKLIST.md`](ENTERPRISE_LAB_DECISION_REPLAY_READINESS_CHECKLIST.md) documents the additive read-only lane that records lab replay-readiness checklist status from already-built evidence lane statuses and linked fingerprints only. [`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_SOURCE_MAP.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_SOURCE_MAP.md) documents the additive read-only lane that maps already-built evidence source fields to replay/readiness artifacts without generating a new fingerprint. [`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md) documents the additive read-only lane that summarizes existing lab-only, read-only, and not-proven boundary fields without generating a fingerprint. [`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY.md) documents the additive read-only lane that inventories already-built evidence field groups without reflection or new fingerprint generation. [`ENTERPRISE_LAB_DECISION_EVIDENCE_NULL_SAFETY_SUMMARY.md`](ENTERPRISE_LAB_DECISION_EVIDENCE_NULL_SAFETY_SUMMARY.md) documents the additive read-only lane that summarizes null, missing, unavailable, and no-healthy/failure-path safety without reflection or new fingerprint generation. [`ENTERPRISE_LAB_DECISION_EVIDENCE_STATUS_ROLLUP.md`](ENTERPRISE_LAB_DECISION_EVIDENCE_STATUS_ROLLUP.md) documents the additive read-only lane that summarizes already-built evidence lane statuses, selected-candidate presence, candidate counts, and boundary state without reflection, scorecards, quality ranking, or new fingerprint generation. [`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_NAVIGATION_SUMMARY.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_NAVIGATION_SUMMARY.md) documents the additive read-only lane that maps already-built evidence lanes to response field paths, UI section labels, and docs reference labels without reflection or new fingerprint generation. [`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_MAP.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_MAP.md) documents the additive read-only lane that maps already-built evidence lane dependencies and downstream relationships without reflection or new fingerprint generation. [`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_REFERENCE_INDEX.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_REFERENCE_INDEX.md) documents the additive read-only lane that indexes already-built evidence lanes by response field path, UI section label, docs reference label, dependency count, and downstream count without reflection or new fingerprint generation. [`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_SUMMARY.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_SUMMARY.md) documents the additive read-only lane that summarizes the existing lane reference index into dependency-shape counts without reflection or new fingerprint generation. [`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_CONSISTENCY_SUMMARY.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_CONSISTENCY_SUMMARY.md) documents the additive read-only lane that cross-checks existing status rollup, dependency map, reference index, and dependency summary surfaces without reflection or new fingerprint generation. [`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_SNAPSHOT.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_SNAPSHOT.md) documents the additive read-only lane that summarizes existing status rollup, dependency map, reference index, dependency summary, and consistency summary surfaces into reviewer highlights and warnings without reflection or new fingerprint generation. [`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_GUIDANCE.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_GUIDANCE.md) documents the additive read-only lane that derives deterministic reviewer guidance from existing reviewer metadata without reflection or new fingerprint generation. The read-only `/api/routing/compare` response can expose `results[].decisionVector`, `results[].dominantFactorAnalysis`, `results[].decisionDeltaAnalysis`, `results[].decisionReplaySnapshot`, `results[].decisionReplayReconstructionTrace`, `results[].decisionReplayCapsule`, `results[].decisionReplayReadinessChecklist`, `results[].decisionReplayEvidenceSourceMap`, `results[].decisionReplayEvidenceBoundarySummary`, `results[].decisionReplayEvidenceFieldInventory`, `results[].decisionReplayEvidenceNullSafetySummary`, `results[].decisionReplayEvidenceStatusRollup`, `results[].decisionReplayEvidenceLaneNavigationSummary`, `results[].decisionReplayEvidenceLaneDependencyMap`, `results[].decisionReplayEvidenceLaneReferenceIndex`, `results[].decisionReplayEvidenceLaneDependencySummary`, `results[].decisionReplayEvidenceLaneConsistencySummary`, `results[].decisionReplayEvidenceReviewerSnapshot`, and `results[].decisionReplayEvidenceReviewerGuidance` as additive controlled lab evidence while preserving existing fields, routing behavior, scoring behavior, and strategy weights. Replay execution and what-if execution remain future/not implemented, and structured decision logging remains future/not implemented.

[`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_CLOSURE_SUMMARY.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_CLOSURE_SUMMARY.md)
documents the additive read-only reviewer closure metadata lane and derives deterministic closure metadata from existing reviewer metadata. [`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_CLOSURE_ROLLUP.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_CLOSURE_ROLLUP.md) documents the additive read-only response-level closure rollup over already-returned reviewer closure summaries. [`ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_CLOSURE_CHECKLIST.md`](ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_CLOSURE_CHECKLIST.md) documents the additive read-only response-level closure checklist over already-returned reviewer closure summaries and the closure rollup. It derives only from existing status rollup,
dependency map, lane reference index, dependency summary, consistency summary, reviewer snapshot, reviewer guidance,
and reviewer handoff surfaces. It adds `results[].decisionReplayEvidenceReviewerClosureSummary` and top-level
`decisionReplayEvidenceReviewerClosureRollup` and `decisionReplayEvidenceReviewerClosureChecklist` without changing
routing behavior, recomputing scores, executing replay, performing what-if mutation, persisting closure data, exporting
closure data, generating a new fingerprint, or making production-validation, certification, guaranteed-replay,
approval, enforcement, remediation, scorecard, quality-ranking, correctness-validation, or production-readiness claims.

## How to Investigate Surprising Lab Decisions

- If the backend changed unexpectedly, inspect the selected strategy, candidate health, visible latency/load/connection pressure, capacity/weight fields, scenario comparison delta, and returned reason text.
- If the backend did not change despite signal changes, compare close candidates and reason text before treating the lab decision as equivalent.
- If all candidates are unhealthy, treat the result as a degradation/fallback boundary, not production proof.
- If the local API is unavailable, static reviewer guidance and evidence links remain available, but the monitored decision chain is incomplete.
- If copied handoff text is unavailable, use the visible lab explanation as the fallback; no server-side sharing or export is implied.

## Evidence Association Model

The cockpit associates:

- Selected scenario to routing decision.
- Routing decision to selected strategy.
- Routing decision to selected backend/server.
- Selected backend/server to key input signals.
- Scenario comparison to what changed.
- Routing proof summary to supporting reviewer pages.
- Evidence pages to export packet and reviewer handoff.

Evidence pages are reviewer navigation aids and local handoff surfaces. They are not server-generated certification artifacts.

## Reviewer Handoff Flow

1. Open the Enterprise Lab routing cockpit at the legacy route `/routing-demo.html`.
2. Load or review a controlled lab scenario.
3. Run routing comparison against the same-origin local API.
4. Inspect the routing proof summary and selected strategy/backend.
5. Compare scenario deltas and edge-case guidance.
6. Follow evidence association links to reviewer dashboard, operator evidence dashboard, evidence timeline, and evidence export packet.
7. Copy proof, walkthrough, association, and lab monitor summaries from the browser.
8. Use the evidence export packet for local browser copy/download/print handoff.
9. Record both what the lab proof supports and what it does not prove for production.

## Production Not-Proven Boundaries

The Enterprise Lab Cockpit does not claim:

- No production certification.
- No production traffic proof.
- No production telemetry proof.
- No production monitoring proof.
- No live-cloud proof.
- No real-tenant proof.
- No SLA/SLO proof.
- No registry publication.
- No container signing.
- No governance application.
- No public exposure readiness.
- No managed load-balancer replacement.

## Naming Guidance

Route names may be legacy. `/routing-demo.html` remains a compatible route/file name, and legacy Postman folders or historical docs may retain their existing names. The visible product identity should still be Enterprise Lab Cockpit, controlled lab evidence, local lab proof, reviewer/operator cockpit, and controlled pre-production routing validation.
