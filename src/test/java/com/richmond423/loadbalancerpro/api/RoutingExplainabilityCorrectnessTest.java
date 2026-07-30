package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.richmond423.loadbalancerpro.core.RoutingDecisionExplanation;
import com.richmond423.loadbalancerpro.core.RoutingDecisionFingerprint;
import org.junit.jupiter.api.Test;

class RoutingExplainabilityCorrectnessTest {
    private static final List<String> ALL_STRATEGIES = List.of(
            "TAIL_LATENCY_POWER_OF_TWO",
            "WEIGHTED_LEAST_LOAD",
            "WEIGHTED_LEAST_CONNECTIONS",
            "WEIGHTED_ROUND_ROBIN",
            "ROUND_ROBIN");

    private final RoutingComparisonService service = new RoutingComparisonService();

    @Test
    void eachStrategyReturnsItsOwnReconciledSelectionEvidence() {
        Map<String, RoutingComparisonResultResponse> results = byStrategy(service.compare(request()).results());

        RoutingComparisonResultResponse weightedRoundRobin = results.get("WEIGHTED_ROUND_ROBIN");
        assertEquals("edge-b", weightedRoundRobin.chosenServerId());
        assertEquals(
                weightedRoundRobin.scores().entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .orElseThrow()
                        .getKey(),
                weightedRoundRobin.chosenServerId());
        assertEquals(
                List.of("effectiveRoutingWeight"),
                weightedRoundRobin.decisionVector().selectedCandidateVector().factorContributions().stream()
                        .map(ScoreFactorContributionResponse::factorName)
                        .toList());
        assertTrue(weightedRoundRobin.decisionVector().candidateSummaries().stream()
                .flatMap(candidate -> candidate.factorContributions().stream())
                .allMatch(factor -> factor.exactness().equals("EXACT_FROM_STRATEGY_MODEL")));

        for (String strategy : List.of(
                "TAIL_LATENCY_POWER_OF_TWO",
                "WEIGHTED_LEAST_LOAD",
                "WEIGHTED_LEAST_CONNECTIONS",
                "WEIGHTED_ROUND_ROBIN")) {
            assertFactorsReconcile(results.get(strategy));
        }

        RoutingComparisonResultResponse tail = results.get("TAIL_LATENCY_POWER_OF_TWO");
        assertEquals(2, tail.decisionVector().candidateCount());
        assertEquals(
                tail.candidateServersConsidered(),
                tail.decisionVector().candidateSummaries().stream()
                        .map(CandidateDecisionVectorResponse::candidateId)
                        .toList());

        RoutingComparisonResultResponse roundRobin = results.get("ROUND_ROBIN");
        assertEquals("edge-a", roundRobin.chosenServerId());
        assertTrue(roundRobin.scores().isEmpty());
        assertTrue(roundRobin.decisionVector().candidateSummaries().stream()
                .allMatch(candidate -> candidate.factorContributions().isEmpty()));
        assertTrue(roundRobin.decisionVector().exactnessBoundary()
                .contains("no additive score model"));
    }

    @Test
    void identicalCompareRequestsReturnIdenticalSelectionsAndFingerprints() {
        Map<String, String> expected = selectionAndFingerprint(service.compare(request()).results());

        for (int iteration = 0; iteration < 20; iteration++) {
            Map<String, String> actual = selectionAndFingerprint(service.compare(request()).results());
            assertEquals(expected, actual, "iteration " + iteration);
        }
        assertTrue(expected.values().stream()
                .allMatch(value -> value.matches(".+\\|sha256:v1:[0-9a-f]{64}")));
    }

    @Test
    void fingerprintUsesFramedCandidateFieldsAndExcludesTimestamp() {
        RoutingDecisionExplanation commaIdentifier = explanation(
                List.of("a,b"), Optional.empty(), Instant.parse("2026-07-29T00:00:00Z"));
        RoutingDecisionExplanation separateIdentifiers = explanation(
                List.of("a", "b"), Optional.empty(), Instant.parse("2026-07-29T00:00:00Z"));
        assertNotEquals(
                RoutingDecisionFingerprint.from(commaIdentifier),
                RoutingDecisionFingerprint.from(separateIdentifiers));

        RoutingDecisionExplanation laterTimestamp = explanation(
                List.of("a,b"), Optional.empty(), Instant.parse("2026-07-30T00:00:00Z"));
        assertEquals(
                RoutingDecisionFingerprint.from(commaIdentifier),
                RoutingDecisionFingerprint.from(laterTimestamp));
    }

    private static void assertFactorsReconcile(RoutingComparisonResultResponse result) {
        assertNotNull(result);
        assertEquals("SUCCESS", result.status());
        assertNotNull(result.decisionVector());
        for (CandidateDecisionVectorResponse candidate : result.decisionVector().candidateSummaries()) {
            double contributionTotal = candidate.factorContributions().stream()
                    .map(ScoreFactorContributionResponse::contributionValue)
                    .filter(value -> value != null)
                    .mapToDouble(Double::doubleValue)
                    .sum();
            assertEquals(result.scores().get(candidate.candidateId()), contributionTotal, 0.000000001,
                    result.strategyId() + " " + candidate.candidateId());
        }
    }

    private static Map<String, RoutingComparisonResultResponse> byStrategy(
            List<RoutingComparisonResultResponse> results) {
        Map<String, RoutingComparisonResultResponse> byStrategy = new LinkedHashMap<>();
        for (RoutingComparisonResultResponse result : results) {
            byStrategy.put(result.strategyId(), result);
        }
        return byStrategy;
    }

    private static Map<String, String> selectionAndFingerprint(
            List<RoutingComparisonResultResponse> results) {
        Map<String, String> decisions = new LinkedHashMap<>();
        for (RoutingComparisonResultResponse result : results) {
            decisions.put(
                    result.strategyId(),
                    result.chosenServerId() + "|" + result.decisionFingerprint());
        }
        return decisions;
    }

    private static RoutingDecisionExplanation explanation(
            List<String> candidates, Optional<String> chosen, Instant timestamp) {
        return new RoutingDecisionExplanation(
                "ROUND_ROBIN",
                candidates,
                chosen,
                Map.of(),
                "test fixture",
                timestamp);
    }

    private static RoutingComparisonRequest request() {
        return new RoutingComparisonRequest(ALL_STRATEGIES, List.of(
                server("edge-a", 40, 1.0, 25.0, 50.0, 90.0, 0.02, 4),
                server("edge-b", 10, 4.0, 30.0, 55.0, 100.0, 0.01, 2),
                server("edge-c", 20, 2.0, 40.0, 80.0, 140.0, 0.03, 6),
                server("edge-d", 5, 3.0, 18.0, 35.0, 70.0, 0.005, 1)));
    }

    private static RoutingServerStateInput server(
            String serverId,
            int inFlight,
            double weight,
            double averageLatency,
            double p95Latency,
            double p99Latency,
            double errorRate,
            int queueDepth) {
        return new RoutingServerStateInput(
                serverId,
                true,
                inFlight,
                100.0,
                100.0,
                weight,
                averageLatency,
                p95Latency,
                p99Latency,
                errorRate,
                queueDepth,
                new NetworkAwarenessInput(0.01, 0.02, 0.005, 2.0, false, 1, 100));
    }
}
