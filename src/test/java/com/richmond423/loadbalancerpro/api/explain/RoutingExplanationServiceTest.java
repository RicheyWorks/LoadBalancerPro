package com.richmond423.loadbalancerpro.api.explain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.api.CandidateDecisionVectorResponse;
import com.richmond423.loadbalancerpro.api.DominantFactorAnalysisResponse;
import com.richmond423.loadbalancerpro.api.RoutingComparisonResponse;
import com.richmond423.loadbalancerpro.api.RoutingComparisonResultResponse;
import com.richmond423.loadbalancerpro.api.RoutingDecisionDeltaAnalysisResponse;
import com.richmond423.loadbalancerpro.api.RoutingDecisionDeltaAnalysisService;
import com.richmond423.loadbalancerpro.api.RoutingDecisionVectorResponse;
import com.richmond423.loadbalancerpro.api.RoutingDominantFactorAnalysisService;
import com.richmond423.loadbalancerpro.api.ScoreFactorContributionResponse;
import com.richmond423.loadbalancerpro.api.proxy.LiveRoutingDecisionRecord;
import org.junit.jupiter.api.Test;

class RoutingExplanationServiceTest {
    private static final long LEGACY_DECISION_EXPLORER_BYTES = 7_784_535L;
    private static final String DECISION_FINGERPRINT =
            "sha256:v1:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final Path GOLDEN =
            Path.of("src/test/resources/api/routing-explanation-v2-golden.json");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final RoutingExplanationService service = new RoutingExplanationService();
    private final LiveRoutingExplanationService liveService = new LiveRoutingExplanationService();

    @Test
    void carriesCandidateFactorsAndDominantDeltaAnalysisWithoutParallelRestatements() {
        RoutingExplanation explanation = service.explain(result());

        assertEquals("TAIL_LATENCY_POWER_OF_TWO", explanation.strategyId());
        assertEquals("edge-a", explanation.selectedCandidateId());
        assertEquals(DECISION_FINGERPRINT, explanation.decisionFingerprint());
        assertEquals(List.of("edge-a", "edge-b"),
                explanation.candidates().stream().map(RoutingExplanation.CandidateFactors::candidateId).toList());
        assertEquals(List.of("errors", "latency"),
                explanation.candidates().get(0).factors().stream()
                        .map(RoutingExplanation.FactorContribution::factorName)
                        .toList());
        assertEquals("AVAILABLE", explanation.dominantFactors().status());
        assertEquals("latency",
                explanation.dominantFactors().candidates().get(0).largestAbsoluteImpact().factorName());
        assertEquals("AVAILABLE", explanation.decisionDelta().status());
        assertEquals("edge-b", explanation.decisionDelta().closestAlternativeCandidateId());
        assertEquals(-20.0d, explanation.decisionDelta().finalScoreGap());
        assertEquals(List.of("errors", "latency"),
                explanation.decisionDelta().factors().stream()
                        .map(RoutingExplanation.FactorDelta::factorName)
                        .toList());
    }

    @Test
    void counterfactualWeightScenariosProjectMinusAndPlusTenPercent() {
        List<RoutingExplanation.CounterfactualWeightScenario> scenarios =
                service.explain(result()).counterfactualWeightScenarios();

        assertEquals(List.of(
                        "errors:-10",
                        "errors:10",
                        "latency:-10",
                        "latency:10"),
                scenarios.stream()
                        .map(row -> row.factorName() + ":" + row.weightShiftPercent())
                        .toList());
        RoutingExplanation.CounterfactualWeightScenario latencyMinus = scenarios.get(2);
        assertEquals(9.0d, latencyMinus.adjustedSelectedContribution());
        assertEquals(18.0d, latencyMinus.adjustedAlternativeContribution());
        assertEquals(-9.0d, latencyMinus.adjustedContributionDelta());
        assertEquals(-19.0d, latencyMinus.projectedFinalScoreGap());
        RoutingExplanation.CounterfactualWeightScenario latencyPlus = scenarios.get(3);
        assertEquals(11.0d, latencyPlus.adjustedSelectedContribution());
        assertEquals(22.0d, latencyPlus.adjustedAlternativeContribution());
        assertEquals(-11.0d, latencyPlus.adjustedContributionDelta());
        assertEquals(-21.0d, latencyPlus.projectedFinalScoreGap());
    }

    @Test
    void counterfactualWeightScenariosOmitFactorsWhoseProjectionCannotChangeTheGap() {
        CandidateDecisionVectorResponse selected =
                candidate("edge-a", true, contribution("sameFactor", 3.0));
        CandidateDecisionVectorResponse alternative =
                candidate("edge-b", false, contribution("sameFactor", 3.0));
        RoutingDecisionVectorResponse vector = new RoutingDecisionVectorResponse(
                true, "/api/routing/compare", "fixture", "ROUND_ROBIN", "edge-a", 2,
                List.of(selected, alternative), selected, List.of(alternative), List.of(), List.of(),
                "fixture", List.of(), "local fixture", "no production proof", "available",
                "not implemented", "not implemented", "not implemented");
        RoutingExplanation explanation = service.explain(new RoutingComparisonResultResponse(
                "ROUND_ROBIN", "SUCCESS", "edge-a", "selected", List.of("edge-a", "edge-b"),
                Map.of("edge-a", 1.0, "edge-b", 2.0), DECISION_FINGERPRINT, vector,
                new RoutingDominantFactorAnalysisService().analyze(vector),
                new RoutingDecisionDeltaAnalysisService().analyze(
                        vector, Map.of("edge-a", 1.0, "edge-b", 2.0))));

        assertTrue(explanation.counterfactualWeightScenarios().isEmpty());
    }

    @Test
    void missingEvidenceStaysUnknownAndDoesNotInventCounterfactualRows() {
        RoutingExplanation explanation = service.explain(new RoutingComparisonResultResponse(
                "ROUND_ROBIN", "FAILED", null, "no decision", List.of(), Map.of(),
                null, null, null, null));

        assertEquals("UNKNOWN", explanation.selectedCandidateId());
        assertEquals("UNKNOWN", explanation.decisionFingerprint());
        assertTrue(explanation.candidates().isEmpty());
        assertEquals("UNKNOWN", explanation.dominantFactors().status());
        assertEquals("UNKNOWN", explanation.decisionDelta().status());
        assertNull(explanation.decisionDelta().finalScoreGap());
        assertTrue(explanation.counterfactualWeightScenarios().isEmpty());
        assertTrue(explanation.readOnly());
        assertTrue(explanation.simulationOnly());
    }

    @Test
    void explainsCapturedActualDecisionWithoutRerunningTheStrategy() {
        LiveRoutingDecisionExplanation live = liveService.explain(liveDecision(
                "strategy",
                new LiveRoutingDecisionRecord.SelectionEvidence(
                        "CAPTURED",
                        "Chose edge-a from the captured weighted least-load score model.",
                        "LOWER_WINS",
                        List.of("edge-a", "edge-b"),
                        Map.of("edge-a", 0.2d, "edge-b", 0.5d),
                        Map.of(
                                "edge-a", List.of(
                                        liveFactor("loadPressure", 0.1d),
                                        liveFactor("recentErrorRate", 0.1d)),
                                "edge-b", List.of(
                                        liveFactor("loadPressure", 0.3d),
                                        liveFactor("recentErrorRate", 0.2d))),
                        DECISION_FINGERPRINT)));

        assertTrue(live.readOnly());
        assertTrue(live.retainedLiveProxyAttempt());
        assertEquals("process-local", live.retentionScope());
        assertEquals("proxy-decision-00000042", live.decisionId());
        assertEquals("WEIGHTED_LEAST_LOAD", live.strategyId());
        assertEquals("edge-a", live.selectedCandidateId());
        assertEquals(List.of("edge-a", "edge-b"), live.exactCandidateIds());
        assertEquals(2, live.candidateObservations().size());
        assertEquals(80.0d, live.candidateObservations().get(1).p95LatencyMillis());
        assertEquals(List.of("edge-a", "edge-b"), live.consideredCandidateIds());
        assertFalse(live.analysis().simulationOnly());
        assertEquals("CAPTURED", live.analysis().status());
        assertEquals("AVAILABLE", live.analysis().dominantFactors().status());
        assertEquals("edge-b", live.analysis().decisionDelta().closestAlternativeCandidateId());
        assertEquals(-0.3d, live.analysis().decisionDelta().finalScoreGap(), 0.000000001d);
        assertEquals(4, live.analysis().counterfactualWeightScenarios().size());
        assertEquals(2, live.decisionChangeThresholds().size());
        LiveRoutingDecisionExplanation.DecisionChangeThreshold loadThreshold =
                live.decisionChangeThresholds().stream()
                        .filter(threshold -> threshold.factorName().equals("loadPressure"))
                        .findFirst()
                        .orElseThrow();
        assertEquals(0.3d, loadThreshold.selectedContributionChangeToTie(), 0.000000001d);
        assertEquals(-0.3d, loadThreshold.alternativeContributionChangeToTie(), 0.000000001d);
        assertEquals(-150.0d, loadThreshold.sharedFactorWeightShiftPercentToTie(), 0.000000001d);
    }

    @Test
    void affinityDecisionDoesNotInventAHiddenStrategyScore() {
        LiveRoutingDecisionExplanation live = liveService.explain(liveDecision(
                "affinity",
                new LiveRoutingDecisionRecord.SelectionEvidence(
                        "NOT_APPLICABLE",
                        "Affinity selected the retained upstream; the affinity value was not retained.",
                        "NOT_APPLICABLE",
                        List.of("edge-a", "edge-b"),
                        Map.of(),
                        Map.of(),
                        null)));

        assertEquals("NOT_APPLICABLE", live.selectionEvidenceStatus());
        assertEquals("NOT_APPLICABLE", live.analysis().decisionDelta().status());
        assertTrue(live.analysis().counterfactualWeightScenarios().isEmpty());
        assertTrue(live.decisionChangeThresholds().isEmpty());
        assertEquals("UNKNOWN", live.analysis().decisionFingerprint());
    }

    @Test
    void dominantFactorsHonorTheStrategyDirectionForStatefulWeightedRoundRobinCarry() {
        LiveRoutingDecisionExplanation live = liveService.explain(liveDecision(
                "strategy",
                new LiveRoutingDecisionRecord.SelectionEvidence(
                        "CAPTURED",
                        "Chose edge-a from captured smooth weighted round-robin state.",
                        "HIGHER_WINS",
                        List.of("edge-a", "edge-b"),
                        Map.of("edge-a", -1.0d, "edge-b", -2.0d),
                        Map.of(
                                "edge-a", List.of(
                                        new LiveRoutingDecisionRecord.FactorContribution(
                                                "smoothWeightCarry",
                                                -2.0d,
                                                "WEAKENS_SELECTION",
                                                "EXACT_FROM_STRATEGY_MODEL"),
                                        new LiveRoutingDecisionRecord.FactorContribution(
                                                "effectiveRoutingWeight",
                                                1.0d,
                                                "SUPPORTS_SELECTION",
                                                "EXACT_FROM_STRATEGY_MODEL")),
                                "edge-b", List.of(
                                        new LiveRoutingDecisionRecord.FactorContribution(
                                                "smoothWeightCarry",
                                                -3.0d,
                                                "WEAKENS_SELECTION",
                                                "EXACT_FROM_STRATEGY_MODEL"),
                                        new LiveRoutingDecisionRecord.FactorContribution(
                                                "effectiveRoutingWeight",
                                                1.0d,
                                                "SUPPORTS_SELECTION",
                                                "EXACT_FROM_STRATEGY_MODEL"))),
                        DECISION_FINGERPRINT)));

        RoutingExplanation.CandidateDominantFactor selected =
                live.analysis().dominantFactors().candidates().get(0);
        assertEquals("effectiveRoutingWeight", selected.largestSupport().factorName());
        assertEquals("smoothWeightCarry", selected.largestPenalty().factorName());
        assertEquals("smoothWeightCarry", selected.largestAbsoluteImpact().factorName());
    }

    @Test
    void goldenPayloadDocumentsCompactV2Contract() throws Exception {
        List<RoutingExplanation> payload = service.explain(new RoutingComparisonResponse(
                List.of("TAIL_LATENCY_POWER_OF_TWO"), 2, null, List.of(result())));
        byte[] actualBytes = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
        JsonNode actual = OBJECT_MAPPER.readTree(actualBytes);
        JsonNode expected = OBJECT_MAPPER.readTree(Files.readAllBytes(GOLDEN));

        assertEquals(expected, actual);
        assertTrue(actualBytes.length <= LEGACY_DECISION_EXPLORER_BYTES / 10,
                () -> "compact payload should be at least an order of magnitude smaller; bytes=" + actualBytes.length);
        assertFalse(actual.toString().contains("confidenceSummary"));
        assertFalse(actual.toString().contains("routingDiagnostics"));
        assertFalse(actual.toString().contains("decisionReplaySnapshot"));
    }

    private static RoutingComparisonResultResponse result() {
        RoutingDecisionVectorResponse vector = vector();
        DominantFactorAnalysisResponse dominant = new RoutingDominantFactorAnalysisService().analyze(vector);
        RoutingDecisionDeltaAnalysisResponse delta =
                new RoutingDecisionDeltaAnalysisService().analyze(vector, Map.of("edge-a", 100.0, "edge-b", 120.0));
        return new RoutingComparisonResultResponse(
                "TAIL_LATENCY_POWER_OF_TWO",
                "SUCCESS",
                "edge-a",
                "selected",
                List.of("edge-a", "edge-b"),
                Map.of("edge-a", 100.0, "edge-b", 120.0),
                DECISION_FINGERPRINT,
                vector,
                dominant,
                delta);
    }

    private static RoutingDecisionVectorResponse vector() {
        CandidateDecisionVectorResponse selected = candidate(
                "edge-a",
                true,
                contribution("latency", 10.0),
                contribution("errors", 1.0));
        CandidateDecisionVectorResponse alternative = candidate(
                "edge-b",
                false,
                contribution("latency", 20.0),
                contribution("errors", 5.0));
        return new RoutingDecisionVectorResponse(
                true,
                "/api/routing/compare",
                "fixture",
                "TAIL_LATENCY_POWER_OF_TWO",
                "edge-a",
                2,
                List.of(selected, alternative),
                selected,
                List.of(alternative),
                List.of(),
                List.of(),
                "fixture",
                List.of(),
                "local fixture",
                "no production proof",
                "available",
                "not implemented",
                "not implemented",
                "not implemented");
    }

    private static CandidateDecisionVectorResponse candidate(
            String candidateId,
            boolean selected,
            ScoreFactorContributionResponse... factors) {
        return new CandidateDecisionVectorResponse(
                candidateId,
                selected,
                List.of(),
                List.of(),
                List.of(factors),
                "fixture",
                "fixture",
                "local fixture",
                "no production proof");
    }

    private static ScoreFactorContributionResponse contribution(String name, double value) {
        return new ScoreFactorContributionResponse(
                name,
                "fixture",
                "fixture",
                "WEAKENS_SELECTION",
                "fixture",
                value,
                "EXACT_FROM_RETURNED_EVIDENCE",
                "fixture",
                "local fixture");
    }

    private static LiveRoutingDecisionRecord liveDecision(
            String selectionSource,
            LiveRoutingDecisionRecord.SelectionEvidence evidence) {
        Instant capturedAt = Instant.parse("2026-07-31T18:00:00Z");
        return new LiveRoutingDecisionRecord(
                "proxy-decision-00000042",
                capturedAt,
                3,
                "api",
                "WEIGHTED_LEAST_LOAD",
                1,
                selectionSource,
                "edge-a",
                List.of(
                        liveCandidate("edge-a", 4, 20.0d, 0.01d, capturedAt),
                        liveCandidate("edge-b", 16, 80.0d, 0.20d, capturedAt)),
                evidence,
                200,
                12.5d,
                false,
                "upstream_response");
    }

    private static LiveRoutingDecisionRecord.CandidateState liveCandidate(
            String id,
            int inFlight,
            double p95LatencyMillis,
            double errorRate,
            Instant observedAt) {
        return new LiveRoutingDecisionRecord.CandidateState(
                id,
                true,
                inFlight,
                100.0d,
                50.0d,
                1.0d,
                p95LatencyMillis / 2.0d,
                p95LatencyMillis,
                p95LatencyMillis * 1.5d,
                errorRate,
                inFlight,
                observedAt);
    }

    private static LiveRoutingDecisionRecord.FactorContribution liveFactor(String name, double value) {
        return new LiveRoutingDecisionRecord.FactorContribution(
                name,
                value,
                value > 0.0d ? "WEAKENS_SELECTION" : "NEUTRAL",
                "EXACT_FROM_STRATEGY_MODEL");
    }
}
