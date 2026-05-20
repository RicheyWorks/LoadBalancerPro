package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class RoutingDecisionReplayEvidenceReviewerClosureRollupServiceTest {
    private final RoutingDecisionReplayEvidenceReviewerClosureRollupService service =
            new RoutingDecisionReplayEvidenceReviewerClosureRollupService();

    @Test
    void normalCompareResponseIncludesDeterministicTopLevelReviewerClosureRollup() {
        RoutingComparisonResponse response = healthyCompareResponse();
        RoutingDecisionReplayEvidenceReviewerClosureRollupResponse first =
                service.rollup(response.results());
        RoutingDecisionReplayEvidenceReviewerClosureRollupResponse second =
                service.rollup(response.results());

        assertEquals(first, second);
        assertEquals(response.decisionReplayEvidenceReviewerClosureRollup(), first);
        assertEquals("COMPLETE", first.status());
        assertEquals("REVIEW_COMPLETE_WITH_LIMITATIONS", first.disposition());
        assertEquals(1, first.resultCount());
        assertEquals(1, first.resultsWithClosureSummary());
        assertEquals(0, first.resultsMissingClosureSummary());
        assertEquals(1, first.completeWithLimitationsCount());
        assertEquals(0, first.unknownCount());
        assertTrue(first.reviewerReady());
        assertEquals(List.of(
                "not replay proof",
                "not scoring proof",
                "not correctness validation",
                "not production readiness",
                "not production certification",
                "not guaranteed replay",
                "not production validation"), first.notProvenBoundaries());
        assertTrue(first.summary().contains("1 of 1 results include closure summaries"));
        assertTrue(first.summary().contains("reviewerReady=true"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.notProvenBoundaries().add("other boundary"));
    }

    @Test
    void noHealthyComparePathReturnsSafeUnknownRollupWithoutInventedProof() {
        RoutingComparisonResponse response = new RoutingComparisonService().compare(new RoutingComparisonRequest(
                List.of("TAIL_LATENCY_POWER_OF_TWO"),
                List.of(new RoutingServerStateInput(
                        "green",
                        false,
                        1,
                        null,
                        null,
                        null,
                        10.0,
                        20.0,
                        30.0,
                        0.0,
                        null,
                        null))));

        RoutingDecisionReplayEvidenceReviewerClosureRollupResponse rollup =
                response.decisionReplayEvidenceReviewerClosureRollup();

        assertEquals("UNKNOWN", rollup.status());
        assertEquals("UNKNOWN", rollup.disposition());
        assertEquals(1, rollup.resultCount());
        assertEquals(1, rollup.resultsWithClosureSummary());
        assertEquals(0, rollup.resultsMissingClosureSummary());
        assertEquals(0, rollup.completeWithLimitationsCount());
        assertEquals(1, rollup.unknownCount());
        assertFalse(rollup.reviewerReady());
        assertTrue(rollup.notProvenBoundaries().contains("not replay proof"));
        assertTrue(rollup.notProvenBoundaries().contains("not production validation"));
        assertFalse(rollup.toString().contains("reviewerClosureRollupFingerprint"));
        assertFalse(rollup.toString().contains("production certification is proven"));
        assertFalse(rollup.toString().contains("guaranteed replay is proven"));
    }

    @Test
    void strippedOrMissingClosureSummaryReturnsUnknownWithoutInventingMetadata() {
        RoutingComparisonResultResponse result = healthyCompareResponse().results().get(0);
        RoutingDecisionReplayEvidenceReviewerClosureRollupResponse rollup =
                service.rollup(List.of(withoutReviewerClosureSummary(result)));

        assertEquals("UNKNOWN", rollup.status());
        assertEquals("UNKNOWN", rollup.disposition());
        assertEquals(1, rollup.resultCount());
        assertEquals(0, rollup.resultsWithClosureSummary());
        assertEquals(1, rollup.resultsMissingClosureSummary());
        assertEquals(0, rollup.completeWithLimitationsCount());
        assertEquals(1, rollup.unknownCount());
        assertFalse(rollup.reviewerReady());
        assertTrue(rollup.summary().contains("0 of 1 results include closure summaries"));
    }

    @Test
    void reviewerClosureRollupServiceDoesNotUseUnsafeCapabilities() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayEvidenceReviewerClosureRollupService.java"),
                StandardCharsets.UTF_8);
        String normalized = source.toLowerCase(Locale.ROOT);

        assertFalse(source.contains("ServerScoreCalculator"));
        assertFalse(source.contains("ServerStateVector"));
        assertFalse(source.contains("MessageDigest"));
        assertFalse(source.contains("SHA-256"));
        assertFalse(normalized.contains("sha256"));
        assertFalse(normalized.contains("instant.now"));
        assertFalse(normalized.contains("clock."));
        assertFalse(normalized.contains("system.getenv"));
        assertFalse(normalized.contains("system.getproperty"));
        assertFalse(normalized.contains("randomuuid"));
        assertFalse(normalized.contains("files.write"));
        assertFalse(normalized.contains("@postmapping"));
        assertFalse(normalized.contains("@getmapping"));
        assertFalse(normalized.contains("zipoutputstream"));
        assertFalse(normalized.contains("processbuilder"));
        assertFalse(normalized.contains("runtime.getruntime"));
        assertFalse(source.contains(".getDeclared"));
        assertFalse(source.contains(".getFields"));
        assertFalse(source.contains(".getMethods"));
        assertFalse(normalized.contains("executed replay"));
        assertFalse(normalized.contains("what-if mutation is performed"));
        assertFalse(normalized.contains("reviewerclosurerollupfingerprint"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("correctness validation is proven"));
        assertTrue(source.contains("decisionReplayEvidenceReviewerClosureSummary"));
        assertTrue(source.contains("not replay proof"));
        assertTrue(source.contains("not scoring proof"));
        assertTrue(source.contains("not production validation"));
    }

    private static RoutingComparisonResponse healthyCompareResponse() {
        return new RoutingComparisonService().compare(new RoutingComparisonRequest(
                List.of("TAIL_LATENCY_POWER_OF_TWO"),
                List.of(
                        new RoutingServerStateInput(
                                "edge-alpha",
                                true,
                                5,
                                100.0,
                                100.0,
                                2.0,
                                20.0,
                                40.0,
                                80.0,
                                0.01,
                                1,
                                new NetworkAwarenessInput(
                                        0.0,
                                        0.0,
                                        0.0,
                                        4.0,
                                        false,
                                        0,
                                        120)),
                        new RoutingServerStateInput(
                                "edge-beta",
                                true,
                                28,
                                100.0,
                                100.0,
                                4.0,
                                24.0,
                                52.0,
                                96.0,
                                0.02,
                                3,
                                new NetworkAwarenessInput(
                                        0.01,
                                        0.01,
                                        0.0,
                                        7.0,
                                        false,
                                        1,
                                        120)),
                        new RoutingServerStateInput(
                                "edge-drain",
                                false,
                                1,
                                100.0,
                                100.0,
                                5.0,
                                12.0,
                                20.0,
                                40.0,
                                0.0,
                                0,
                                null))));
    }

    private static RoutingComparisonResultResponse withoutReviewerClosureSummary(
            RoutingComparisonResultResponse result) {
        return new RoutingComparisonResultResponse(
                result.strategyId(),
                result.status(),
                result.chosenServerId(),
                result.reason(),
                result.candidateServersConsidered(),
                result.scores(),
                result.decisionVector(),
                result.dominantFactorAnalysis(),
                result.decisionDeltaAnalysis(),
                result.decisionReplaySnapshot(),
                result.decisionReplayReconstructionTrace(),
                result.decisionReplayCapsule(),
                result.decisionReplayReadinessChecklist(),
                result.decisionReplayEvidenceSourceMap(),
                result.decisionReplayEvidenceBoundarySummary(),
                result.decisionReplayEvidenceFieldInventory(),
                result.decisionReplayEvidenceNullSafetySummary(),
                result.decisionReplayEvidenceStatusRollup(),
                result.decisionReplayEvidenceLaneNavigationSummary(),
                result.decisionReplayEvidenceLaneDependencyMap(),
                result.decisionReplayEvidenceLaneReferenceIndex(),
                result.decisionReplayEvidenceLaneDependencySummary(),
                result.decisionReplayEvidenceLaneConsistencySummary(),
                result.decisionReplayEvidenceReviewerSnapshot(),
                result.decisionReplayEvidenceReviewerGuidance(),
                result.decisionReplayEvidenceReviewerHandoffSummary(),
                null);
    }
}
