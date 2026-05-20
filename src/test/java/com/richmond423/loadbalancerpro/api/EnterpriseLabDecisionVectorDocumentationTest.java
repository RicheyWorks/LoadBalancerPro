package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class EnterpriseLabDecisionVectorDocumentationTest {
    private static final Path DECISION_VECTOR = Path.of("docs/ENTERPRISE_LAB_DECISION_VECTOR.md");
    private static final Path DOMINANT_FACTOR_ANALYSIS =
            Path.of("docs/ENTERPRISE_LAB_DOMINANT_FACTOR_ANALYSIS.md");
    private static final Path DECISION_DELTA_ANALYSIS =
            Path.of("docs/ENTERPRISE_LAB_DECISION_DELTA_ANALYSIS.md");
    private static final Path DECISION_REPLAY_SNAPSHOT =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_SNAPSHOT.md");
    private static final Path DECISION_REPLAY_RECONSTRUCTION_TRACE =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_RECONSTRUCTION_TRACE.md");
    private static final Path DECISION_REPLAY_CAPSULE =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_CAPSULE.md");
    private static final Path DECISION_REPLAY_READINESS_CHECKLIST =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_READINESS_CHECKLIST.md");
    private static final Path DECISION_REPLAY_EVIDENCE_SOURCE_MAP =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_SOURCE_MAP.md");
    private static final Path DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md");
    private static final Path DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY.md");
    private static final Path DECISION_EVIDENCE_NULL_SAFETY_SUMMARY =
            Path.of("docs/ENTERPRISE_LAB_DECISION_EVIDENCE_NULL_SAFETY_SUMMARY.md");
    private static final Path DECISION_EVIDENCE_STATUS_ROLLUP =
            Path.of("docs/ENTERPRISE_LAB_DECISION_EVIDENCE_STATUS_ROLLUP.md");
    private static final Path DECISION_REPLAY_EVIDENCE_LANE_NAVIGATION_SUMMARY =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_NAVIGATION_SUMMARY.md");
    private static final Path DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_MAP =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_MAP.md");
    private static final Path DECISION_REPLAY_EVIDENCE_LANE_REFERENCE_INDEX =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_REFERENCE_INDEX.md");
    private static final Path DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_SUMMARY =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_SUMMARY.md");
    private static final Path DECISION_REPLAY_EVIDENCE_LANE_CONSISTENCY_SUMMARY =
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_CONSISTENCY_SUMMARY.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path FRAMING = Path.of("docs/ENTERPRISE_LAB_COCKPIT_FRAMING.md");
    private static final Path ROUTING_COCKPIT = Path.of("src/main/resources/static/routing-demo.html");

    @Test
    void decisionVectorDocDefinesControlledLabContract() throws Exception {
        String doc = read(DECISION_VECTOR);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Vector Contract"));
        assertTrue(doc.contains(
                "LoadBalancerPro is an Enterprise Lab Cockpit for controlled pre-production routing validation. It is not a demo."));
        assertTrue(doc.contains(
                "The Enterprise Lab Decision Vector is the structured explanation object for one controlled lab routing decision."));
        assertTrue(doc.contains("## Decision Vector Fields"));
        assertTrue(doc.contains("`selectedStrategy`"));
        assertTrue(doc.contains("`selectedBackend`"));
        assertTrue(doc.contains("`candidateBackends`"));
        assertTrue(doc.contains("`selectedCandidateVector`"));
        assertTrue(doc.contains("`nonSelectedCandidateVectors`"));
        assertTrue(doc.contains("`visibleCandidateSignals`"));
        assertTrue(doc.contains("`knownSignals`"));
        assertTrue(doc.contains("`unknownSignals`"));
        assertTrue(doc.contains("`exactScoringAvailability`"));
        assertTrue(doc.contains("`factorContributionAvailability`"));
        assertTrue(doc.contains("`dominantFactorAnalysis`"));
        assertTrue(doc.contains("`decisionDeltaAnalysis`"));
        assertTrue(doc.contains("`decisionReplaySnapshot`"));
        assertTrue(doc.contains("`decisionReplayReconstructionTrace`"));
        assertTrue(doc.contains("`decisionReplayCapsule`"));
        assertTrue(doc.contains("`decisionReplayReadinessChecklist`"));
        assertTrue(doc.contains("`decisionReplayEvidenceSourceMap`"));
        assertTrue(doc.contains("`decisionReplayEvidenceBoundarySummary`"));
        assertTrue(doc.contains("`decisionReplayEvidenceFieldInventory`"));
        assertTrue(doc.contains("`decisionReplayEvidenceNullSafetySummary`"));
        assertTrue(doc.contains("`decisionReplayEvidenceStatusRollup`"));
        assertTrue(doc.contains("`decisionReplayEvidenceLaneNavigationSummary`"));
        assertTrue(doc.contains("`decisionReplayEvidenceLaneDependencyMap`"));
        assertTrue(doc.contains("`decisionReplayEvidenceLaneReferenceIndex`"));
        assertTrue(doc.contains("`decisionReplayEvidenceLaneDependencySummary`"));
        assertTrue(doc.contains("`replayReadiness`"));
        assertTrue(normalized.contains("how it answers why this backend"));
        assertTrue(normalized.contains("selected-vs-alternative"));
        assertTrue(normalized.contains("known visible signals from unknown or unexposed signals"));
    }

    @Test
    void candidateDecisionVectorAndFactorPlaceholderAreBounded() throws Exception {
        String doc = read(DECISION_VECTOR);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("## Candidate Decision Vector"));
        assertTrue(doc.contains("`candidateId` or `candidateName`"));
        assertTrue(doc.contains("`selected`"));
        assertTrue(doc.contains("`healthState`"));
        assertTrue(doc.contains("`latencySignal`"));
        assertTrue(doc.contains("`loadOrConnectionPressureSignal`"));
        assertTrue(doc.contains("`capacityOrWeightSignal`"));
        assertTrue(doc.contains("`visibleSupportSignals`"));
        assertTrue(doc.contains("`visibleCautionSignals`"));
        assertTrue(doc.contains("`selectionExplanation`"));
        assertTrue(doc.contains("`fallbackExplanation`"));
        assertTrue(doc.contains("Candidate reason is unknown from visible data"));
        assertTrue(doc.contains("## Factor Contribution Placeholder Contract"));
        assertTrue(doc.contains("`factorName`"));
        assertTrue(doc.contains("`rawValue`"));
        assertTrue(doc.contains("`normalizedValue`"));
        assertTrue(doc.contains("`direction`"));
        assertTrue(doc.contains("`contributionValue`"));
        assertTrue(doc.contains("`weight`"));
        assertTrue(normalized.contains("future extension unless the api explicitly exposes contribution data"));
        assertTrue(normalized.contains("contributionvalue"));
        assertTrue(normalized.contains("future/not implemented"));
        assertTrue(normalized.contains("exact production scoring is not claimed"));
        assertTrue(normalized.contains("hidden scoring must not be inferred"));
        assertTrue(doc.contains("## From Decision Vector to Factor Contributions"));
        assertTrue(doc.contains("ServerScoreCalculator"));
        assertTrue(doc.contains("It explains current calculator components; it does not retune weights."));
        assertTrue(doc.contains("It preserves existing score values and routing selection behavior."));
        assertTrue(doc.contains("## Candidate Factor Contribution Integration"));
        assertTrue(doc.contains("CandidateFactorContributionSummary"));
        assertTrue(doc.contains("selected and non-selected candidates carry the same contribution summary shape"));
        assertTrue(doc.contains("The read-only `/api/routing/compare` response can expose candidate contribution summaries through"));
        assertTrue(doc.contains("`results[].decisionVector`"));
        assertTrue(doc.contains("\"factorContributionSummary\""));
        assertTrue(doc.contains("It does not implement decision replay, what-if experiments, or structured decision logging."));
        assertTrue(doc.contains("\"factorContributions\""));
        assertTrue(doc.contains("## Dominant Factor Analysis"));
        assertTrue(doc.contains("ScoreFactorContributionResponse"));
        assertTrue(doc.contains("largest support-direction contributor"));
        assertTrue(doc.contains("largest penalty/risk contributor"));
        assertTrue(doc.contains("largest absolute numeric impact"));
        assertTrue(doc.contains("Ties are resolved by stable factor-name ordering."));
        assertTrue(doc.contains("ENTERPRISE_LAB_DOMINANT_FACTOR_ANALYSIS.md"));
        assertTrue(doc.contains("## Selected-vs-Closest-Alternative Decision Delta Analysis"));
        assertTrue(doc.contains("closest scored non-selected alternative"));
        assertTrue(doc.contains("shared finite factor contribution deltas"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_DELTA_ANALYSIS.md"));
        assertTrue(doc.contains("## Decision Replay Snapshot"));
        assertTrue(doc.contains("deterministic local snapshot fingerprint"));
        assertTrue(doc.contains("does not execute replay, perform what-if mutation, persist audit logs, or rerun"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_SNAPSHOT.md"));
        assertTrue(doc.contains("## Decision Replay Reconstruction Trace"));
        assertTrue(doc.contains("deterministic reconstruction steps"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_RECONSTRUCTION_TRACE.md"));
        assertTrue(doc.contains("## Decision Replay Capsule"));
        assertTrue(doc.contains("deterministic capsule fingerprint"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_CAPSULE.md"));
        assertTrue(doc.contains("## Decision Replay Readiness Checklist"));
        assertTrue(doc.contains("Decision Replay Readiness Checklist is the read-only lab evidence readiness layer"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_READINESS_CHECKLIST.md"));
        assertTrue(doc.contains("## Decision Replay Evidence Source Map"));
        assertTrue(doc.contains("Decision Replay Evidence Source Map is the read-only lab evidence relationship layer"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_SOURCE_MAP.md"));
        assertTrue(doc.contains("## Decision Replay Evidence Boundary Summary"));
        assertTrue(doc.contains("Decision Replay Evidence Boundary Summary is the read-only lab boundary metadata layer"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md"));
        assertTrue(doc.contains("## Decision Replay Evidence Field Inventory"));
        assertTrue(doc.contains("Decision Replay Evidence Field Inventory is the read-only lab field inventory layer"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY.md"));
        assertTrue(doc.contains("## Decision Evidence Null-Safety Summary"));
        assertTrue(doc.contains("Decision Evidence Null-Safety Summary is the read-only lab null-safety metadata layer"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_EVIDENCE_NULL_SAFETY_SUMMARY.md"));
        assertTrue(doc.contains("## Decision Evidence Status Rollup"));
        assertTrue(doc.contains("Decision Evidence Status Rollup is the read-only lab status metadata layer"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_EVIDENCE_STATUS_ROLLUP.md"));
        assertTrue(doc.contains("## Decision Replay Evidence Lane Navigation Summary"));
        assertTrue(doc.contains(
                "Decision Replay Evidence Lane Navigation Summary is the read-only lab reviewer-navigation metadata layer"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_NAVIGATION_SUMMARY.md"));
        assertTrue(doc.contains("## Decision Replay Evidence Lane Dependency Map"));
        assertTrue(doc.contains(
                "Decision Replay Evidence Lane Dependency Map is the read-only lab reviewer-navigation and provenance metadata layer"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_MAP.md"));
        assertTrue(doc.contains("## Decision Replay Evidence Lane Reference Index"));
        assertTrue(doc.contains(
                "Decision Replay Evidence Lane Reference Index is the read-only lab reviewer-reference metadata layer"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_REFERENCE_INDEX.md"));
        assertTrue(doc.contains("## Decision Replay Evidence Lane Dependency Summary"));
        assertTrue(doc.contains(
                "Decision Replay Evidence Lane Dependency Summary is the read-only lab reviewer metadata layer"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_SUMMARY.md"));
        assertTrue(doc.contains("## Decision Replay Evidence Lane Consistency Summary"));
        assertTrue(doc.contains(
                "Decision Replay Evidence Lane Consistency Summary is the read-only lab reviewer metadata layer"));
        assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_CONSISTENCY_SUMMARY.md"));
        assertTrue(doc.contains("\"EXACT_FROM_CALCULATOR\""));
        assertTrue(doc.contains("\"NOT_EXPOSED\""));
        assertTrue(doc.contains("not production scoring proof"));
    }

    @Test
    void decisionVectorRoadmapIsFutureAndNotImplemented() throws Exception {
        String doc = read(DECISION_VECTOR);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("## Replay, What-If, Logging, and Plugin Roadmap"));
        for (String futureItem : List.of(
                "Dominant factor analysis: implemented as additive read-only interpretation of returned contribution data only.",
                "Decision delta analysis: implemented as additive read-only selected-vs-closest-alternative interpretation of returned score and contribution data only.",
                "Decision replay snapshot: implemented as additive read-only snapshot evidence and deterministic local fingerprint only.",
                "Decision replay reconstruction trace: implemented as additive read-only reconstruction evidence steps and deterministic local trace fingerprint only.",
                "Decision replay capsule: implemented as additive read-only canonical evidence packaging and deterministic local capsule fingerprint only.",
                "Decision replay readiness checklist: implemented as additive read-only lab evidence readiness status over already-built evidence lanes only.",
                "Decision replay evidence source map: implemented as additive read-only lab evidence source mapping over already-built evidence lanes only.",
                "Decision replay evidence boundary summary: implemented as additive read-only lab boundary metadata over already-built boundary fields and statuses only.",
                "Decision replay evidence field inventory: implemented as additive read-only lab field inventory over already-built evidence field groups only.",
                "Decision evidence null-safety summary: implemented as additive read-only lab null-safety metadata over already-built evidence lanes only.",
                "Decision evidence status rollup: implemented as additive read-only lab status metadata over already-built evidence lanes only.",
                "Decision replay evidence lane navigation summary: implemented as additive read-only reviewer-navigation metadata over already-built evidence lanes only.",
                "Decision replay evidence lane dependency map: implemented as additive read-only reviewer-navigation/provenance metadata over already-built evidence lanes only.",
                "Decision replay evidence lane reference index: implemented as additive read-only reviewer-reference metadata over already-built evidence lanes only.",
                "Decision replay evidence lane dependency summary: implemented as additive read-only reviewer dependency-shape metadata over the already-built lane reference index only.",
                "Decision replay evidence lane consistency summary: implemented as additive read-only reviewer consistency metadata over already-built lane status/dependency/reference/summary surfaces only.",
                "Broader factor modeling beyond current returned calculator contribution data: future/not implemented.",
                "Replay execution: future/not implemented.",
                "What-if experiments: future/not implemented.",
                "Structured decision logging: future/not implemented.",
                "Strategy plugin explainability: future/not implemented.",
                "Rack, zone, and topology modeling: future/not implemented.",
                "Correlated failure modeling: future/not implemented.",
                "Live interrogation mode: future/not implemented.")) {
            assertTrue(doc.contains(futureItem), "roadmap item should stay explicitly future: " + futureItem);
        }

        assertTrue(doc.contains("## Static Example Decision Vector Payload"));
        assertTrue(doc.contains("\"factorContributionAvailability\": \"exposedForCurrentCalculatorComponentsWhenReturnedByApi\""));
        assertTrue(doc.contains("\"replayReadiness\": \"plannedFutureContract; replay execution is not implemented\""));
        assertTrue(doc.contains("\"whatIfReadiness\": \"plannedFutureContract; what-if execution is not implemented\""));
        assertTrue(doc.contains("\"structuredDecisionLoggingReadiness\": \"plannedFutureContract; structured logging is not implemented\""));
        assertTrue(doc.contains("## Read-only Decision Vector Exposure"));
        assertTrue(doc.contains("`POST /api/routing/compare` exposes the Decision Vector through the additive"));
        assertTrue(doc.contains("`results[].decisionVector` field"));
        assertTrue(doc.contains("`results[].dominantFactorAnalysis`"));
        assertTrue(doc.contains("`results[].decisionDeltaAnalysis`"));
        assertTrue(doc.contains("`results[].decisionReplaySnapshot`"));
        assertTrue(doc.contains("`results[].decisionReplayReconstructionTrace`"));
        assertTrue(doc.contains("`results[].decisionReplayCapsule`"));
        assertTrue(doc.contains("`results[].decisionReplayReadinessChecklist`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceSourceMap`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceBoundarySummary`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceFieldInventory`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceNullSafetySummary`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceStatusRollup`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceLaneNavigationSummary`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceLaneDependencyMap`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceLaneReferenceIndex`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceLaneDependencySummary`"));
        assertTrue(doc.contains("preserves existing"));
        assertTrue(doc.contains("`requestedStrategies`, `candidateCount`, `timestamp`, result status"));
        assertTrue(doc.contains("\"localLabResponsePath\": \"/api/routing/compare\""));
        assertFalse(normalized.contains("factor contribution analysis is implemented"));
        assertFalse(normalized.contains("replay execution is implemented"));
        assertFalse(normalized.contains("what-if experiments are implemented"));
        assertFalse(normalized.contains("strategy plugin explainability is implemented"));
    }

    @Test
    void decisionEvidenceNullSafetySummaryDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_EVIDENCE_NULL_SAFETY_SUMMARY);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Evidence Null-Safety Summary"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceNullSafetySummary`"));
        assertTrue(doc.contains("decision-replay-evidence-null-safety-summary/v1"));
        assertTrue(doc.contains("selected-candidate-null-safety"));
        assertTrue(doc.contains("candidate-set-null-safety"));
        assertTrue(doc.contains("score-gap-null-safety"));
        assertTrue(doc.contains("closest-alternative-null-safety"));
        assertTrue(doc.contains("largest-delta-factor-null-safety"));
        assertTrue(doc.contains("linked-fingerprint-null-safety"));
        assertTrue(doc.contains("candidate-evidence-null-safety"));
        assertTrue(doc.contains("factor-evidence-null-safety"));
        assertTrue(doc.contains("field-inventory-null-safety"));
        assertTrue(doc.contains("no-healthy-path-null-safety"));
        assertTrue(doc.contains("boundary-text-null-safety"));
        assertTrue(doc.contains("production-not-proven-null-safety"));
        assertTrue(doc.contains("does not inspect raw server input"));
        assertTrue(doc.contains("does not use reflection"));
        assertTrue(doc.contains("does not generate a new fingerprint"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(doc.contains("does not change routing behavior"));
        assertTrue(doc.contains("does not export, download, or share null-safety summaries"));
        assertTrue(doc.contains("no selected candidate, candidate set, alternative candidate, score gap"));
        assertTrue(normalized.contains("not production certification"));
        assertTrue(normalized.contains("not guaranteed replay"));
        assertTrue(normalized.contains("not cryptographic production proof"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionEvidenceStatusRollupDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_EVIDENCE_STATUS_ROLLUP);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Evidence Status Rollup"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceStatusRollup`"));
        assertTrue(doc.contains("decision-replay-evidence-status-rollup/v1"));
        assertTrue(doc.contains("decision-vector-status"));
        assertTrue(doc.contains("dominant-factor-analysis-status"));
        assertTrue(doc.contains("decision-delta-analysis-status"));
        assertTrue(doc.contains("replay-snapshot-status"));
        assertTrue(doc.contains("reconstruction-trace-status"));
        assertTrue(doc.contains("replay-capsule-status"));
        assertTrue(doc.contains("readiness-checklist-status"));
        assertTrue(doc.contains("evidence-source-map-status"));
        assertTrue(doc.contains("evidence-boundary-summary-status"));
        assertTrue(doc.contains("evidence-field-inventory-status"));
        assertTrue(doc.contains("evidence-null-safety-status"));
        assertTrue(doc.contains("read-only-boundary-status"));
        assertTrue(doc.contains("production-not-proven-status"));
        assertTrue(doc.contains("does not inspect raw server input"));
        assertTrue(doc.contains("does not use reflection"));
        assertTrue(doc.contains("does not generate a new fingerprint"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist status rollups or audit logs server-side"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(doc.contains("does not change routing behavior"));
        assertTrue(doc.contains("does not export, download, or share status rollups"));
        assertTrue(doc.contains("no selected candidate, candidate set, alternative candidate, score gap"));
        assertTrue(normalized.contains("not production certification"));
        assertTrue(normalized.contains("not guaranteed replay"));
        assertTrue(normalized.contains("not cryptographic production proof"));
        assertTrue(normalized.contains("not production traffic validation"));
        assertTrue(normalized.contains("not an approval"));
        assertTrue(normalized.contains("quality-ranking"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("quality ranking is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionReplayEvidenceLaneNavigationSummaryDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_REPLAY_EVIDENCE_LANE_NAVIGATION_SUMMARY);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Evidence Lane Navigation Summary"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceLaneNavigationSummary`"));
        assertTrue(doc.contains("decision-replay-evidence-lane-navigation-summary/v1"));
        assertTrue(doc.contains("decision-vector-navigation"));
        assertTrue(doc.contains("dominant-factor-analysis-navigation"));
        assertTrue(doc.contains("decision-delta-analysis-navigation"));
        assertTrue(doc.contains("replay-snapshot-navigation"));
        assertTrue(doc.contains("reconstruction-trace-navigation"));
        assertTrue(doc.contains("replay-capsule-navigation"));
        assertTrue(doc.contains("readiness-checklist-navigation"));
        assertTrue(doc.contains("evidence-source-map-navigation"));
        assertTrue(doc.contains("evidence-boundary-summary-navigation"));
        assertTrue(doc.contains("evidence-field-inventory-navigation"));
        assertTrue(doc.contains("evidence-null-safety-navigation"));
        assertTrue(doc.contains("evidence-status-rollup-navigation"));
        assertTrue(doc.contains("does not inspect raw server input"));
        assertTrue(doc.contains("does not inspect raw request payloads"));
        assertTrue(doc.contains("does not use reflection"));
        assertTrue(doc.contains("does not generate a lane-navigation fingerprint"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(normalized.contains("persist lane"));
        assertTrue(normalized.contains("navigation summaries or audit logs server-side"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not infer hidden scoring"));
        assertTrue(doc.contains("does not retune"));
        assertTrue(doc.contains("does not change routing behavior"));
        assertTrue(doc.contains("does not export, download, or share lane navigation summaries"));
        assertTrue(doc.contains("no selected candidate, candidate set, alternative candidate, score gap"));
        assertTrue(normalized.contains("not production certification"));
        assertTrue(normalized.contains("not live-cloud proof"));
        assertTrue(normalized.contains("not real-tenant proof"));
        assertTrue(normalized.contains("not sla/slo proof"));
        assertTrue(normalized.contains("not registry publication proof"));
        assertTrue(normalized.contains("not signing proof"));
        assertTrue(normalized.contains("not governance application proof"));
        assertTrue(normalized.contains("not exact production scoring proof"));
        assertTrue(normalized.contains("not cryptographic production proof"));
        assertTrue(normalized.contains("not guaranteed replay"));
        assertTrue(normalized.contains("not production traffic validation"));
        assertTrue(normalized.contains("not an approval"));
        assertTrue(normalized.contains("scorecard"));
        assertTrue(normalized.contains("quality-ranking"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("quality ranking is proven"));
        assertFalse(normalized.contains("approval is granted"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionReplayEvidenceLaneDependencyMapDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_MAP);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Evidence Lane Dependency Map"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceLaneDependencyMap`"));
        assertTrue(doc.contains("decision-replay-evidence-lane-dependency-map/v1"));
        assertTrue(doc.contains("decision-vector-dependency"));
        assertTrue(doc.contains("dominant-factor-analysis-dependency"));
        assertTrue(doc.contains("decision-delta-analysis-dependency"));
        assertTrue(doc.contains("replay-snapshot-dependency"));
        assertTrue(doc.contains("reconstruction-trace-dependency"));
        assertTrue(doc.contains("replay-capsule-dependency"));
        assertTrue(doc.contains("readiness-checklist-dependency"));
        assertTrue(doc.contains("evidence-source-map-dependency"));
        assertTrue(doc.contains("evidence-boundary-summary-dependency"));
        assertTrue(doc.contains("evidence-field-inventory-dependency"));
        assertTrue(doc.contains("evidence-null-safety-dependency"));
        assertTrue(doc.contains("evidence-status-rollup-dependency"));
        assertTrue(doc.contains("evidence-lane-navigation-dependency"));
        assertTrue(doc.contains("dependsOnLaneIds"));
        assertTrue(doc.contains("downstreamLaneIds"));
        assertTrue(doc.contains("dependencyCount"));
        assertTrue(doc.contains("downstreamCount"));
        assertTrue(doc.contains("does not inspect raw server input"));
        assertTrue(doc.contains("inspect raw request payloads"));
        assertTrue(doc.contains("does not use reflection"));
        assertTrue(normalized.contains("lane-dependency fingerprint"));
        assertTrue(normalized.contains("does not execute"));
        assertTrue(normalized.contains("does not perform what-if mutation"));
        assertTrue(normalized.contains("persist lane dependency maps or audit logs server-side"));
        assertTrue(normalized.contains("does not recompute"));
        assertTrue(doc.contains("does not infer hidden scoring"));
        assertTrue(doc.contains("does not retune"));
        assertTrue(doc.contains("does not change routing behavior"));
        assertTrue(doc.contains("does not export, download, or share lane dependency maps"));
        assertTrue(doc.contains("no selected candidate, candidate set, alternative candidate, score gap"));
        assertTrue(normalized.contains("not production certification"));
        assertTrue(normalized.contains("not live-cloud proof"));
        assertTrue(normalized.contains("not real-tenant proof"));
        assertTrue(normalized.contains("not sla/slo proof"));
        assertTrue(normalized.contains("not registry publication proof"));
        assertTrue(normalized.contains("not signing proof"));
        assertTrue(normalized.contains("not governance application proof"));
        assertTrue(normalized.contains("not exact production scoring proof"));
        assertTrue(normalized.contains("not cryptographic production proof"));
        assertTrue(normalized.contains("not guaranteed replay"));
        assertTrue(normalized.contains("not production traffic validation"));
        assertTrue(normalized.contains("not an approval"));
        assertTrue(normalized.contains("scorecard"));
        assertTrue(normalized.contains("quality-ranking"));
        assertTrue(normalized.contains("correctness validation"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("quality ranking is proven"));
        assertFalse(normalized.contains("approval is granted"));
        assertFalse(normalized.contains("correctness validation is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionReplayEvidenceLaneReferenceIndexDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_REPLAY_EVIDENCE_LANE_REFERENCE_INDEX);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Evidence Lane Reference Index"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceLaneReferenceIndex`"));
        assertTrue(doc.contains("decision-replay-evidence-lane-reference-index/v1"));
        assertTrue(doc.contains("Decision Vector."));
        assertTrue(doc.contains("Dominant Factor Analysis."));
        assertTrue(doc.contains("Decision Delta Analysis."));
        assertTrue(doc.contains("Decision Replay Snapshot."));
        assertTrue(doc.contains("Decision Replay Reconstruction Trace."));
        assertTrue(doc.contains("Decision Replay Capsule."));
        assertTrue(doc.contains("Decision Replay Readiness Checklist."));
        assertTrue(doc.contains("Decision Replay Evidence Source Map."));
        assertTrue(doc.contains("Decision Replay Evidence Boundary Summary."));
        assertTrue(doc.contains("Decision Replay Evidence Field Inventory."));
        assertTrue(doc.contains("Decision Evidence Null-Safety Summary."));
        assertTrue(doc.contains("Decision Evidence Status Rollup."));
        assertTrue(doc.contains("Decision Replay Evidence Lane Navigation Summary."));
        assertTrue(doc.contains("Decision Replay Evidence Lane Dependency Map."));
        assertTrue(doc.contains("decision-vector-reference"));
        assertTrue(doc.contains("dominant-factor-analysis-reference"));
        assertTrue(doc.contains("decision-delta-analysis-reference"));
        assertTrue(doc.contains("replay-snapshot-reference"));
        assertTrue(doc.contains("reconstruction-trace-reference"));
        assertTrue(doc.contains("replay-capsule-reference"));
        assertTrue(doc.contains("readiness-checklist-reference"));
        assertTrue(doc.contains("evidence-source-map-reference"));
        assertTrue(doc.contains("evidence-boundary-summary-reference"));
        assertTrue(doc.contains("evidence-field-inventory-reference"));
        assertTrue(doc.contains("evidence-null-safety-reference"));
        assertTrue(doc.contains("evidence-status-rollup-reference"));
        assertTrue(doc.contains("evidence-lane-navigation-reference"));
        assertTrue(doc.contains("evidence-lane-dependency-map-reference"));
        assertTrue(doc.contains("dependency count"));
        assertTrue(doc.contains("downstream count"));
        assertTrue(doc.contains("does not inspect raw server input"));
        assertTrue(normalized.contains("raw request payload"));
        assertTrue(doc.contains("does not use reflection"));
        assertTrue(doc.contains("does not generate a new fingerprint"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist lane reference indexes or audit logs server-side"));
        assertTrue(doc.contains("does not export, download, upload, or share lane reference indexes"));
        assertTrue(doc.contains("does not change routing behavior"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(doc.contains("does not infer hidden scoring"));
        assertTrue(normalized.contains("not production certification"));
        assertTrue(normalized.contains("not live-cloud proof"));
        assertTrue(normalized.contains("not real-tenant proof"));
        assertTrue(normalized.contains("not sla/slo proof"));
        assertTrue(normalized.contains("not registry"));
        assertTrue(normalized.contains("not signing proof"));
        assertTrue(normalized.contains("not governance application proof"));
        assertTrue(normalized.contains("not exact production scoring proof"));
        assertTrue(normalized.contains("not guaranteed replay"));
        assertTrue(normalized.contains("not cryptographic production proof"));
        assertTrue(normalized.contains("not production traffic validation"));
        assertTrue(normalized.contains("not an approval"));
        assertTrue(normalized.contains("remediation"));
        assertTrue(normalized.contains("enforcement"));
        assertTrue(normalized.contains("readiness score"));
        assertTrue(normalized.contains("scorecard"));
        assertTrue(normalized.contains("quality ranking"));
        assertTrue(normalized.contains("correctness"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("quality ranking is proven"));
        assertFalse(normalized.contains("approval is granted"));
        assertFalse(normalized.contains("correctness validation is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("download endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionReplayEvidenceLaneDependencySummaryDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_SUMMARY);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Evidence Lane Dependency Summary"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceLaneDependencySummary`"));
        assertTrue(doc.contains("decision-replay-evidence-lane-dependency-summary/v1"));
        assertTrue(doc.contains("derived only from `results[].decisionReplayEvidenceLaneReferenceIndex`"));
        assertTrue(doc.contains("root lane count"));
        assertTrue(doc.contains("terminal lane count"));
        assertTrue(doc.contains("max dependency count"));
        assertTrue(doc.contains("max downstream count"));
        assertTrue(doc.contains("densest dependency lane ids"));
        assertTrue(doc.contains("widest downstream lane ids"));
        assertTrue(doc.contains("not replay execution"));
        assertTrue(doc.contains("not scoring proof"));
        assertTrue(doc.contains("not correctness validation"));
        assertTrue(doc.contains("not production readiness"));
        assertTrue(doc.contains("does not inspect raw server input"));
        assertTrue(normalized.contains("raw request payload"));
        assertTrue(doc.contains("does not use reflection"));
        assertTrue(doc.contains("does not generate a new fingerprint"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist lane dependency summaries or audit logs server-side"));
        assertTrue(doc.contains("does not export, download, upload, or share lane dependency summaries"));
        assertTrue(doc.contains("does not change routing behavior"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(doc.contains("does not infer hidden scoring"));
        assertTrue(normalized.contains("not production certification"));
        assertTrue(normalized.contains("not guaranteed replay"));
        assertTrue(normalized.contains("not cryptographic production proof"));
        assertTrue(normalized.contains("not production traffic validation"));
        assertTrue(normalized.contains("not an approval"));
        assertTrue(normalized.contains("remediation"));
        assertTrue(normalized.contains("enforcement"));
        assertTrue(normalized.contains("scorecard"));
        assertTrue(normalized.contains("quality ranking"));
        assertTrue(normalized.contains("correctness"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("quality ranking is proven"));
        assertFalse(normalized.contains("approval is granted"));
        assertFalse(normalized.contains("correctness validation is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("download endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionReplayEvidenceLaneConsistencySummaryDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_REPLAY_EVIDENCE_LANE_CONSISTENCY_SUMMARY);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Evidence Lane Consistency Summary"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceLaneConsistencySummary`"));
        assertTrue(doc.contains("decision-replay-evidence-lane-consistency-summary/v1"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceStatusRollup`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceLaneDependencyMap`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceLaneReferenceIndex`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceLaneDependencySummary`"));
        assertTrue(doc.contains("normalized consistency status"));
        assertTrue(doc.contains("mismatched count field names"));
        assertTrue(doc.contains("missing surface names"));
        assertTrue(doc.contains("deterministic consistency check objects"));
        assertTrue(doc.contains("not replay execution"));
        assertTrue(doc.contains("not scoring proof"));
        assertTrue(doc.contains("not correctness validation"));
        assertTrue(doc.contains("not production readiness"));
        assertTrue(doc.contains("not guaranteed replay"));
        assertTrue(doc.contains("does not inspect raw server input"));
        assertTrue(normalized.contains("raw request payload"));
        assertTrue(doc.contains("does not use reflection"));
        assertTrue(doc.contains("does not generate a new fingerprint"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist lane consistency summaries or audit logs server-side"));
        assertTrue(doc.contains("does not export, download, upload, or share lane consistency summaries"));
        assertTrue(doc.contains("does not change routing behavior"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(doc.contains("does not infer hidden scoring"));
        assertTrue(normalized.contains("not production certification"));
        assertTrue(normalized.contains("not guaranteed replay"));
        assertTrue(normalized.contains("not cryptographic production proof"));
        assertTrue(normalized.contains("not production traffic validation"));
        assertTrue(normalized.contains("not an approval"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void dominantFactorAnalysisDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DOMINANT_FACTOR_ANALYSIS);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Dominant Factor Analysis"));
        assertTrue(doc.contains("read-only interpretation layer"));
        assertTrue(doc.contains("ScoreFactorContributionResponse"));
        assertTrue(doc.contains("does not recompute scores from raw server fields"));
        assertTrue(doc.contains("does not change routing behavior"));
        assertTrue(doc.contains("largest support-direction contributor"));
        assertTrue(doc.contains("largest penalty/risk contributor"));
        assertTrue(doc.contains("largest absolute numeric contribution"));
        assertTrue(doc.contains("Tie handling is deterministic"));
        assertTrue(doc.contains("returns an unknown/empty state"));
        assertTrue(normalized.contains("not proof of production readiness"));
        assertTrue(normalized.contains("not proof of production readiness"));
        assertTrue(normalized.contains("production certification"));
        assertTrue(normalized.contains("live-cloud behavior"));
        assertTrue(normalized.contains("real-tenant behavior"));
        assertTrue(normalized.contains("sla/slo"));
        assertTrue(normalized.contains("registry publication"));
        assertTrue(normalized.contains("signing status"));
        assertTrue(normalized.contains("governance application"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("live-cloud behavior is proven"));
        assertFalse(normalized.contains("real-tenant behavior is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionDeltaAnalysisDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_DELTA_ANALYSIS);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Delta Analysis"));
        assertTrue(doc.contains("Selected-vs-Closest-Alternative Decision Delta Analysis"));
        assertTrue(doc.contains("final score values already returned in `results[].scores`"));
        assertTrue(doc.contains("ScoreFactorContributionResponse"));
        assertTrue(doc.contains("does not recompute scores from raw server fields"));
        assertTrue(doc.contains("does not duplicate `ServerScoreCalculator`"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(doc.contains("closest non-selected alternative"));
        assertTrue(doc.contains("stable candidate/server id ordering"));
        assertTrue(doc.contains("contribution delta"));
        assertTrue(doc.contains("absolute delta"));
        assertTrue(doc.contains("If the selected candidate, closest alternative, final scores, or Decision Vector data are unavailable"));
        assertTrue(doc.contains("returns `UNKNOWN`"));
        assertTrue(doc.contains("returns `PARTIAL`"));
        assertTrue(doc.contains("instead of inventing zero values"));
        assertTrue(normalized.contains("lab explainability only"));
        assertTrue(normalized.contains("production certification"));
        assertTrue(normalized.contains("live-cloud behavior"));
        assertTrue(normalized.contains("real-tenant behavior"));
        assertTrue(normalized.contains("sla/slo"));
        assertTrue(normalized.contains("registry publication"));
        assertTrue(normalized.contains("signing status"));
        assertTrue(normalized.contains("governance application"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("live-cloud behavior is proven"));
        assertFalse(normalized.contains("real-tenant behavior is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionReplaySnapshotDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_REPLAY_SNAPSHOT);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Snapshot"));
        assertTrue(doc.contains("read-only evidence lane"));
        assertTrue(doc.contains("`POST /api/routing/compare`"));
        assertTrue(doc.contains("not replay execution"));
        assertTrue(doc.contains("not what-if mutation"));
        assertTrue(doc.contains("does not persist data"));
        assertTrue(doc.contains("does not recompute scores from raw server fields"));
        assertTrue(doc.contains("does not duplicate `ServerScoreCalculator`"));
        assertTrue(doc.contains("deterministic local hash"));
        assertTrue(doc.contains("must not include timestamps, random ids, hostnames"));
        assertTrue(doc.contains("environment variables, file paths, secrets, local usernames"));
        assertTrue(doc.contains("returns `UNKNOWN`"));
        assertTrue(doc.contains("returns `PARTIAL`"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist audit logs"));
        assertTrue(normalized.contains("lab explainability only"));
        assertTrue(normalized.contains("production certification"));
        assertTrue(normalized.contains("live-cloud behavior"));
        assertTrue(normalized.contains("real-tenant behavior"));
        assertTrue(normalized.contains("sla/slo"));
        assertTrue(normalized.contains("registry publication"));
        assertTrue(normalized.contains("signing status"));
        assertTrue(normalized.contains("governance application"));
        assertTrue(normalized.contains("exact production scoring"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("live-cloud behavior is proven"));
        assertFalse(normalized.contains("real-tenant behavior is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("download endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionReplayReconstructionTraceDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_REPLAY_RECONSTRUCTION_TRACE);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Reconstruction Trace"));
        assertTrue(doc.contains("read-only reconstruction evidence lane"));
        assertTrue(doc.contains("`POST /api/routing/compare`"));
        assertTrue(doc.contains("derived from existing routing compare lab evidence"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist traces or audit logs server-side"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(doc.contains("deterministic local trace fingerprint"));
        assertTrue(doc.contains("not cryptographic proof of production behavior"));
        assertTrue(doc.contains("must not include timestamps, random ids, hostnames"));
        assertTrue(doc.contains("environment variables, file paths, secrets, local usernames"));
        assertTrue(doc.contains("returns `UNKNOWN`"));
        assertTrue(doc.contains("returns `PARTIAL`"));
        assertTrue(doc.contains("reconstruction steps"));
        assertTrue(normalized.contains("lab explainability/replay-readiness only"));
        assertTrue(normalized.contains("production certification"));
        assertTrue(normalized.contains("live-cloud behavior"));
        assertTrue(normalized.contains("real-tenant behavior"));
        assertTrue(normalized.contains("sla/slo"));
        assertTrue(normalized.contains("registry publication"));
        assertTrue(normalized.contains("signing status"));
        assertTrue(normalized.contains("governance application"));
        assertTrue(normalized.contains("exact production scoring"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("download endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionReplayCapsuleDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_REPLAY_CAPSULE);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Capsule"));
        assertTrue(doc.contains("read-only canonical evidence packaging lane"));
        assertTrue(doc.contains("`POST /api/routing/compare`"));
        assertTrue(doc.contains("derived from already-built decision vector, dominant factor, decision delta"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist capsules or audit logs server-side"));
        assertTrue(doc.contains("does not export, download, or share capsules"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(doc.contains("deterministic local capsule fingerprint"));
        assertTrue(doc.contains("not cryptographic proof of production behavior"));
        assertTrue(doc.contains("must not include timestamps, random ids, hostnames"));
        assertTrue(doc.contains("environment variables, file paths, secrets, local usernames"));
        assertTrue(doc.contains("returns `UNKNOWN`"));
        assertTrue(doc.contains("returns `PARTIAL`"));
        assertTrue(doc.contains("candidate evidence"));
        assertTrue(doc.contains("factor evidence"));
        assertTrue(normalized.contains("lab explainability/replay-readiness only"));
        assertTrue(normalized.contains("production certification"));
        assertTrue(normalized.contains("live-cloud behavior"));
        assertTrue(normalized.contains("real-tenant behavior"));
        assertTrue(normalized.contains("sla/slo"));
        assertTrue(normalized.contains("registry publication"));
        assertTrue(normalized.contains("signing status"));
        assertTrue(normalized.contains("governance application"));
        assertTrue(normalized.contains("exact production scoring"));
        assertTrue(normalized.contains("guaranteed replay"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("download endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionReplayReadinessChecklistDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_REPLAY_READINESS_CHECKLIST);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Readiness Checklist"));
        assertTrue(doc.contains("read-only lab evidence readiness lane"));
        assertTrue(doc.contains("`POST /api/routing/compare`"));
        assertTrue(doc.contains("derived only from already-built Decision Vector"));
        assertTrue(doc.contains("`results[].decisionReplayReadinessChecklist`"));
        assertTrue(doc.contains("`decision-replay-readiness-checklist/v1`"));
        assertTrue(doc.contains("`decision-vector-evidence`"));
        assertTrue(doc.contains("`read-only-boundary-evidence`"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist checklist state or audit logs"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(doc.contains("does not export, download, or share checklist data"));
        assertTrue(doc.contains("linked replay snapshot, reconstruction trace, and replay capsule fingerprints only when the source evidence is already available"));
        assertTrue(doc.contains("returns `UNKNOWN`"));
        assertTrue(doc.contains("returns `PARTIAL`"));
        assertTrue(normalized.contains("lab explainability/replay-readiness only"));
        assertTrue(normalized.contains("production certification"));
        assertTrue(normalized.contains("live-cloud behavior"));
        assertTrue(normalized.contains("real-tenant behavior"));
        assertTrue(normalized.contains("sla/slo"));
        assertTrue(normalized.contains("registry publication"));
        assertTrue(normalized.contains("signing status"));
        assertTrue(normalized.contains("governance application"));
        assertTrue(normalized.contains("exact production scoring"));
        assertTrue(normalized.contains("guaranteed replay"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("download endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionReplayEvidenceSourceMapDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_REPLAY_EVIDENCE_SOURCE_MAP);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Evidence Source Map"));
        assertTrue(doc.contains("read-only lab source-mapping lane"));
        assertTrue(doc.contains("`POST /api/routing/compare`"));
        assertTrue(doc.contains("derived only from already-built `decisionVector`"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceSourceMap`"));
        assertTrue(doc.contains("`decision-replay-evidence-source-map/v1`"));
        assertTrue(doc.contains("`decision-vector-source`"));
        assertTrue(doc.contains("`read-only-boundary-source`"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist source-map data or audit logs"));
        assertTrue(doc.contains("does not generate a new fingerprint"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(doc.contains("does not export, download, or share source-map data"));
        assertTrue(doc.contains("linked replay snapshot, reconstruction trace, and replay capsule fingerprints only when already available"));
        assertTrue(doc.contains("does not use"));
        assertTrue(doc.contains("MessageDigest"));
        assertTrue(doc.contains("returns `UNKNOWN`"));
        assertTrue(doc.contains("returns `PARTIAL`"));
        assertTrue(normalized.contains("lab explainability/replay-readiness only"));
        assertTrue(normalized.contains("production certification"));
        assertTrue(normalized.contains("live-cloud behavior"));
        assertTrue(normalized.contains("real-tenant behavior"));
        assertTrue(normalized.contains("sla/slo"));
        assertTrue(normalized.contains("registry publication"));
        assertTrue(normalized.contains("signing status"));
        assertTrue(normalized.contains("governance application"));
        assertTrue(normalized.contains("exact production scoring"));
        assertTrue(normalized.contains("guaranteed replay"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("download endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionReplayEvidenceBoundarySummaryDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Evidence Boundary Summary"));
        assertTrue(doc.contains("read-only lab boundary-metadata lane"));
        assertTrue(doc.contains("`POST /api/routing/compare`"));
        assertTrue(doc.contains("derived only from already-built"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceBoundarySummary`"));
        assertTrue(doc.contains("`decision-replay-evidence-boundary-summary/v1`"));
        assertTrue(doc.contains("`lab-only-boundary`"));
        assertTrue(doc.contains("`production-not-proven-boundary`"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist boundary-summary data or audit logs"));
        assertTrue(doc.contains("does not generate a new fingerprint"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(doc.contains("add external calls"));
        assertTrue(doc.contains("add upload/share/download behavior"));
        assertTrue(doc.contains("MessageDigest"));
        assertTrue(doc.contains("returns `UNKNOWN`"));
        assertTrue(doc.contains("`PARTIAL`"));
        assertTrue(normalized.contains("lab explainability/reviewer-trust boundary metadata only"));
        assertTrue(normalized.contains("live-cloud proof"));
        assertTrue(normalized.contains("real-tenant proof"));
        assertTrue(normalized.contains("sla/slo"));
        assertTrue(normalized.contains("registry publication"));
        assertTrue(normalized.contains("container signing"));
        assertTrue(normalized.contains("governance application"));
        assertTrue(normalized.contains("exact production scoring"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("download endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionReplayEvidenceFieldInventoryDocDefinesReadOnlyBoundaries() throws Exception {
        String doc = read(DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("# Enterprise Lab Decision Replay Evidence Field Inventory"));
        assertTrue(doc.contains("read-only lab field-inventory lane"));
        assertTrue(doc.contains("`POST /api/routing/compare`"));
        assertTrue(doc.contains("derived only from already-built compare"));
        assertTrue(doc.contains("`results[].decisionReplayEvidenceFieldInventory`"));
        assertTrue(doc.contains("`decision-replay-evidence-field-inventory/v1`"));
        assertTrue(doc.contains("`decision-vector-fields`"));
        assertTrue(doc.contains("`linked-fingerprint-fields`"));
        assertTrue(doc.contains("`production-not-proven-boundary-fields`"));
        assertTrue(doc.contains("does not use reflection"));
        assertTrue(doc.contains("does not generate a new fingerprint"));
        assertTrue(doc.contains("does not execute replay"));
        assertTrue(doc.contains("does not perform what-if mutation"));
        assertTrue(doc.contains("does not persist field inventories or audit logs server-side"));
        assertTrue(doc.contains("does not recompute scores"));
        assertTrue(doc.contains("does not retune weights"));
        assertTrue(doc.contains("does not export, download, or share field inventories"));
        assertTrue(doc.contains("returns `UNKNOWN`"));
        assertTrue(doc.contains("`PARTIAL`"));
        assertTrue(normalized.contains("lab explainability/reviewer-trust field inventory only"));
        assertTrue(normalized.contains("not production certification"));
        assertTrue(normalized.contains("not live-cloud proof"));
        assertTrue(normalized.contains("not real-tenant proof"));
        assertTrue(normalized.contains("not sla/slo proof"));
        assertTrue(normalized.contains("not registry publication proof"));
        assertTrue(normalized.contains("not signing proof"));
        assertTrue(normalized.contains("not governance application proof"));
        assertTrue(normalized.contains("not exact production scoring proof"));
        assertTrue(normalized.contains("not cryptographic production proof"));
        assertTrue(normalized.contains("not guaranteed replay"));
        assertTrue(normalized.contains("not production traffic validation"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("upload endpoint"));
        assertFalse(normalized.contains("download endpoint"));
        assertFalse(normalized.contains("server-side export endpoint"));
    }

    @Test
    void decisionVectorDocIsLinkedFromReviewerDocs() throws Exception {
        String readme = read(README);
        String trustMap = read(TRUST_MAP);
        String framing = read(FRAMING);

        for (String doc : List.of(readme, trustMap, framing)) {
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_VECTOR.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DOMINANT_FACTOR_ANALYSIS.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_DELTA_ANALYSIS.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_SNAPSHOT.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_RECONSTRUCTION_TRACE.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_CAPSULE.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_READINESS_CHECKLIST.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_SOURCE_MAP.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_EVIDENCE_NULL_SAFETY_SUMMARY.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_EVIDENCE_STATUS_ROLLUP.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_NAVIGATION_SUMMARY.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_MAP.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_REFERENCE_INDEX.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_SUMMARY.md"));
            assertTrue(doc.contains("ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_CONSISTENCY_SUMMARY.md"));
            assertTrue(doc.contains("Decision Vector"));
        }

        assertTrue(readme.contains("read-only Decision Replay Evidence Lane Navigation Summary lane: [`docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_NAVIGATION_SUMMARY.md`](docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_NAVIGATION_SUMMARY.md); read-only Decision Replay Evidence Lane Dependency Map lane: [`docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_MAP.md`](docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_MAP.md); read-only Decision Replay Evidence Lane Reference Index lane: [`docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_REFERENCE_INDEX.md`](docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_REFERENCE_INDEX.md); read-only Decision Replay Evidence Lane Dependency Summary lane: [`docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_SUMMARY.md`](docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_SUMMARY.md); read-only Decision Replay Evidence Lane Consistency Summary lane: [`docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_CONSISTENCY_SUMMARY.md`](docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_CONSISTENCY_SUMMARY.md)."));
        assertTrue(trustMap.contains("### Decision Vector Contract"));
        assertTrue(framing.contains("## Decision Vector Contract"));
    }

    @Test
    void routingCockpitIncludesDecisionVectorFoundation() throws Exception {
        String page = read(ROUTING_COCKPIT);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("id=\"decision-vector-foundation-panel\""));
        assertTrue(page.contains("Decision Vector Foundation"));
        assertTrue(page.contains("Structured Decision Evidence"));
        assertTrue(page.contains("data-copy-target=\"decision-vector-summary-output\""));
        assertTrue(page.contains("A structured explanation object for one controlled lab routing decision"));
        assertTrue(page.contains("Candidate Decision Vector"));
        assertTrue(page.contains("Known vs unknown separation"));
        assertTrue(page.contains("ServerScoreCalculator factor contribution contract extraction has begun"));
        assertTrue(page.contains("read-only comparison response can expose those summaries for selected and non-selected candidate vectors"));
        assertTrue(page.contains("no score weights are retuned"));
        assertTrue(page.contains("The same-origin <code>/api/routing/compare</code> response can return additive"));
        assertTrue(page.contains("Decision Vector data is unavailable in this response; static lab guidance remains available."));
        assertTrue(page.contains("Copy structured decision vector summary"));
        assertTrue(page.contains("Decision replay, what-if experiments, and structured decision logging should build on this contract later"));
        assertTrue(page.contains("id=\"decision-vector-selected-strategy\""));
        assertTrue(page.contains("id=\"decision-vector-selected-backend\""));
        assertTrue(page.contains("id=\"decision-vector-candidate-count\""));
        assertTrue(page.contains("id=\"decision-vector-known-signals\""));
        assertTrue(page.contains("id=\"decision-vector-unknown-signals\""));
        assertTrue(page.contains("id=\"decision-vector-scoring-availability\""));
        assertTrue(page.contains("id=\"decision-vector-factor-contribution\""));
        assertTrue(page.contains("id=\"decision-vector-readonly-exposure\""));
        assertTrue(page.contains("id=\"decision-vector-replay-readiness\""));
        assertTrue(page.contains("Most Influential Signals"));
        assertTrue(page.contains("Dominant factor analysis"));
        assertTrue(page.contains("id=\"decision-vector-dominant-selected\""));
        assertTrue(page.contains("id=\"decision-vector-dominant-candidates\""));
        assertTrue(page.contains("id=\"decision-vector-dominant-boundary\""));
        assertTrue(page.contains("Selected vs Closest Alternative"));
        assertTrue(page.contains("Decision Delta"));
        assertTrue(page.contains("id=\"decision-vector-delta-comparison\""));
        assertTrue(page.contains("id=\"decision-vector-delta-largest\""));
        assertTrue(page.contains("id=\"decision-vector-delta-boundary\""));
        assertTrue(page.contains("Decision Evidence Snapshot"));
        assertTrue(page.contains("id=\"decision-vector-replay-snapshot\""));
        assertTrue(page.contains("id=\"decision-vector-replay-fingerprint\""));
        assertTrue(page.contains("id=\"decision-vector-replay-boundary\""));
        assertTrue(page.contains("Replay Reconstruction Trace"));
        assertTrue(page.contains("id=\"decision-vector-reconstruction-trace\""));
        assertTrue(page.contains("id=\"decision-vector-reconstruction-fingerprint\""));
        assertTrue(page.contains("id=\"decision-vector-reconstruction-boundary\""));
        assertTrue(page.contains("Replay Capsule"));
        assertTrue(page.contains("id=\"decision-vector-replay-capsule\""));
        assertTrue(page.contains("id=\"decision-vector-capsule-fingerprint\""));
        assertTrue(page.contains("id=\"decision-vector-capsule-boundary\""));
        assertTrue(page.contains("Replay Readiness Checklist"));
        assertTrue(page.contains("id=\"decision-vector-readiness-checklist\""));
        assertTrue(page.contains("id=\"decision-vector-readiness-fingerprints\""));
        assertTrue(page.contains("id=\"decision-vector-readiness-boundary\""));
        assertTrue(page.contains("Decision Replay Evidence Source Map"));
        assertTrue(page.contains("id=\"decision-vector-source-map\""));
        assertTrue(page.contains("id=\"decision-vector-source-map-fingerprints\""));
        assertTrue(page.contains("id=\"decision-vector-source-map-boundary\""));
        assertTrue(page.contains("Decision Replay Evidence Boundary Summary"));
        assertTrue(page.contains("id=\"decision-vector-boundary-summary\""));
        assertTrue(page.contains("id=\"decision-vector-boundary-items\""));
        assertTrue(page.contains("id=\"decision-vector-boundary-summary-boundary\""));
        assertTrue(page.contains("Decision Replay Evidence Field Inventory"));
        assertTrue(page.contains("id=\"decision-vector-field-inventory-status\""));
        assertTrue(page.contains("id=\"decision-vector-field-inventory-entries\""));
        assertTrue(page.contains("id=\"decision-vector-field-inventory-boundary\""));
        assertTrue(page.contains("Decision Evidence Status Rollup"));
        assertTrue(page.contains("id=\"decision-vector-status-rollup-status\""));
        assertTrue(page.contains("id=\"decision-vector-status-rollup-items\""));
        assertTrue(page.contains("id=\"decision-vector-status-rollup-boundary\""));
        assertTrue(page.contains("Decision Evidence Lane Navigation"));
        assertTrue(page.contains("id=\"decision-vector-lane-navigation-status\""));
        assertTrue(page.contains("id=\"decision-vector-lane-navigation-items\""));
        assertTrue(page.contains("id=\"decision-vector-lane-navigation-response-paths\""));
        assertTrue(page.contains("id=\"decision-vector-lane-navigation-ui-sections\""));
        assertTrue(page.contains("id=\"decision-vector-lane-navigation-docs\""));
        assertTrue(page.contains("id=\"decision-vector-lane-navigation-boundary\""));
        assertTrue(page.contains("Decision Evidence Lane Dependency Map"));
        assertTrue(page.contains("id=\"decision-vector-lane-dependency-status\""));
        assertTrue(page.contains("id=\"decision-vector-lane-dependency-items\""));
        assertTrue(page.contains("id=\"decision-vector-lane-dependency-depends-on\""));
        assertTrue(page.contains("id=\"decision-vector-lane-dependency-downstream\""));
        assertTrue(page.contains("id=\"decision-vector-lane-dependency-boundary\""));
        assertTrue(page.contains("Decision Evidence Lane Reference Index"));
        assertTrue(page.contains("id=\"decision-vector-lane-reference-index-status\""));
        assertTrue(page.contains("id=\"decision-vector-lane-reference-index-items\""));
        assertTrue(page.contains("id=\"decision-vector-lane-reference-index-response-paths\""));
        assertTrue(page.contains("id=\"decision-vector-lane-reference-index-ui-sections\""));
        assertTrue(page.contains("id=\"decision-vector-lane-reference-index-docs\""));
        assertTrue(page.contains("id=\"decision-vector-lane-reference-index-dependency-count\""));
        assertTrue(page.contains("id=\"decision-vector-lane-reference-index-downstream-count\""));
        assertTrue(page.contains("id=\"decision-vector-lane-reference-index-boundary\""));
        assertTrue(page.contains("# Decision Vector Foundation"));
        assertTrue(page.contains("decisionIdOrLabRunId: "));
        assertTrue(page.contains("candidateVectors: "));
        assertTrue(page.contains("exactScoringAvailability: "));
        assertTrue(page.contains("factorContributionAvailability: "));
        assertTrue(page.contains("candidateFactorContributionSummary: "));
        assertTrue(page.contains("dominantFactorAnalysis: "));
        assertTrue(page.contains("candidateDominantFactors: "));
        assertTrue(page.contains("decisionDeltaAnalysis: "));
        assertTrue(page.contains("decisionReplaySnapshot: "));
        assertTrue(page.contains("decisionReplayReconstructionTrace: "));
        assertTrue(page.contains("decisionReplayCapsule: "));
        assertTrue(page.contains("decisionReplayReadinessChecklist: "));
        assertTrue(page.contains("decisionReplayEvidenceSourceMap: "));
        assertTrue(page.contains("decisionReplayEvidenceBoundarySummary: "));
        assertTrue(page.contains("decisionReplayEvidenceFieldInventory: "));
        assertTrue(page.contains("decisionReplayEvidenceStatusRollup: "));
        assertTrue(page.contains("decisionReplayEvidenceLaneNavigationSummary: "));
        assertTrue(page.contains("decisionReplayEvidenceLaneDependencyMap: "));
        assertTrue(page.contains("decisionReplayEvidenceLaneReferenceIndex: "));
        assertTrue(page.contains("readOnlyExposure: "));
        assertTrue(page.contains("internal calculator contribution and candidate summary contracts started"));
        assertTrue(page.contains("replayReadiness: "));
        assertTrue(page.contains("whatIfReadiness: planned future contract; what-if execution is not implemented"));
        assertTrue(page.contains("structuredDecisionLoggingReadiness: planned future contract; structured decision logging is not implemented"));
        assertTrue(page.contains("copyBoundary: browser-local copy action only; no upload/share/download route; no server-side export/PDF/ZIP generation; no external calls; no telemetry"));
        assertTrue(normalized.contains("hidden scoring is not invented"));
        assertTrue(normalized.contains("exact production scoring is not claimed unless exposed by the api"));
    }

    @Test
    void decisionVectorDocsAndUiAvoidUnsafeClaims() throws Exception {
        for (Path path : List.of(DECISION_VECTOR, DOMINANT_FACTOR_ANALYSIS, DECISION_DELTA_ANALYSIS,
                DECISION_REPLAY_SNAPSHOT, DECISION_REPLAY_RECONSTRUCTION_TRACE, DECISION_REPLAY_CAPSULE,
                DECISION_REPLAY_READINESS_CHECKLIST, DECISION_REPLAY_EVIDENCE_SOURCE_MAP,
                DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY, DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY,
                DECISION_EVIDENCE_NULL_SAFETY_SUMMARY, DECISION_EVIDENCE_STATUS_ROLLUP,
                DECISION_REPLAY_EVIDENCE_LANE_NAVIGATION_SUMMARY,
                DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_MAP,
                DECISION_REPLAY_EVIDENCE_LANE_REFERENCE_INDEX,
                README, TRUST_MAP, FRAMING, ROUTING_COCKPIT)) {
            String content = read(path);
            String normalized = content.toLowerCase(Locale.ROOT);

            assertTrue(content.contains("Enterprise Lab Cockpit"), path + " should preserve Enterprise Lab framing");
            assertTrue(normalized.contains("not a demo"), path + " should preserve not-a-demo framing");
            assertFalse(normalized.contains("hidden scoring is available"), path + " must not expose hidden scoring");
            assertFalse(normalized.contains("hidden scoring is inferred"), path + " must not infer hidden scoring");
            assertFalse(normalized.contains("exact production scoring is claimed"), path + " must not claim exact scoring");
            assertFalse(normalized.contains("completed factor contribution analysis is implemented"),
                    path + " must not claim completed factor contributions");
            assertFalse(normalized.contains("completed replay is implemented"), path + " must not claim completed replay");
            assertFalse(normalized.contains("completed what-if experiments are implemented"),
                    path + " must not claim completed what-if work");
            assertFalse(normalized.contains("production telemetry is available"), path + " must not claim production telemetry");
            assertFalse(normalized.contains("production monitoring is available"), path + " must not claim production monitoring");
            assertFalse(normalized.contains("upload endpoint"), path + " must not add upload endpoints");
            assertFalse(normalized.contains("server-side decision vector export"), path + " must not add server-side export behavior");
            assertFalse(normalized.contains("fetch(\"https://"), path + " must not add external calls");
            assertFalse(normalized.contains("fetch('https://"), path + " must not add external calls");
            assertFalse(normalized.contains("sendbeacon"), path + " must not add telemetry");
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
