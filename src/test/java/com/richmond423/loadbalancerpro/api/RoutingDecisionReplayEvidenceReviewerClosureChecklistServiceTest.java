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

class RoutingDecisionReplayEvidenceReviewerClosureChecklistServiceTest {
    private final RoutingDecisionReplayEvidenceReviewerClosureChecklistService service =
            new RoutingDecisionReplayEvidenceReviewerClosureChecklistService();

    @Test
    void normalCompareResponseIncludesDeterministicReviewerClosureChecklist() {
        RoutingComparisonResponse response = healthyCompareResponse();
        RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse first =
                service.checklist(response.results(), response.decisionReplayEvidenceReviewerClosureRollup());
        RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse second =
                service.checklist(response.results(), response.decisionReplayEvidenceReviewerClosureRollup());

        assertEquals(first, second);
        assertEquals(response.decisionReplayEvidenceReviewerClosureChecklist(), first);
        assertEquals("COMPLETE", first.status());
        assertTrue(first.reviewerReady());
        assertEquals(5, first.items().size());
        assertChecklistItem(first, "closureSummaryPresent", "PASS");
        assertChecklistItem(first, "closureRollupPresent", "PASS");
        assertChecklistItem(first, "countsMatchResultMetadata", "PASS");
        assertChecklistItem(first, "scenarioReplayStripped", "PASS");
        assertChecklistItem(first, "notProvenBoundariesPresent", "PASS");
        assertEquals(List.of(
                "not replay proof",
                "not scoring proof",
                "not correctness validation",
                "not production readiness",
                "not production certification",
                "not guaranteed replay",
                "not production validation"), first.notProvenBoundaries());
        assertTrue(first.summary().contains("closureSummaryPresent=PASS"));
        assertTrue(first.summary().contains("reviewerReady=true"));
        assertTrue(first.summary().contains("not replay proof"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.items().add(new RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse(
                        "other", "PASS", "other")));
        assertThrows(UnsupportedOperationException.class,
                () -> first.notProvenBoundaries().add("other boundary"));
    }

    @Test
    void noHealthyComparePathReturnsUnknownChecklistWithoutInventedProof() {
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

        RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse checklist =
                response.decisionReplayEvidenceReviewerClosureChecklist();

        assertEquals("UNKNOWN", checklist.status());
        assertFalse(checklist.reviewerReady());
        assertChecklistItem(checklist, "closureSummaryPresent", "PASS");
        assertChecklistItem(checklist, "closureRollupPresent", "PASS");
        assertChecklistItem(checklist, "countsMatchResultMetadata", "PASS");
        assertChecklistItem(checklist, "scenarioReplayStripped", "PASS");
        assertChecklistItem(checklist, "notProvenBoundariesPresent", "PASS");
        assertTrue(checklist.notProvenBoundaries().contains("not replay proof"));
        assertTrue(checklist.notProvenBoundaries().contains("not production validation"));
        assertFalse(checklist.toString().contains("reviewerClosureChecklistFingerprint"));
        assertFalse(checklist.toString().contains("production certification is proven"));
        assertFalse(checklist.toString().contains("guaranteed replay is proven"));
        assertFalse(checklist.toString().contains("correctness validation is proven"));
    }

    @Test
    void strippedOrMissingClosureSummaryWarnsWhenRollupCountsNoLongerMatch() {
        RoutingComparisonResponse response = healthyCompareResponse();
        RoutingComparisonResultResponse result = response.results().get(0);
        RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse checklist =
                service.checklist(
                        List.of(withoutReviewerClosureSummary(result)),
                        response.decisionReplayEvidenceReviewerClosureRollup());

        assertEquals("PARTIAL", checklist.status());
        assertFalse(checklist.reviewerReady());
        assertChecklistItem(checklist, "closureSummaryPresent", "WARN");
        assertChecklistItem(checklist, "closureRollupPresent", "PASS");
        assertChecklistItem(checklist, "countsMatchResultMetadata", "WARN");
        assertChecklistItem(checklist, "scenarioReplayStripped", "PASS");
        assertChecklistItem(checklist, "notProvenBoundariesPresent", "PASS");
        assertTrue(checklist.summary().contains("countsMatchResultMetadata=WARN"));
    }

    @Test
    void reviewerClosureChecklistServiceDoesNotUseUnsafeCapabilities() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayEvidenceReviewerClosureChecklistService.java"),
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
        assertFalse(normalized.contains("reviewerclosurechecklistfingerprint"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("correctness validation is proven"));
        assertTrue(source.contains("decisionReplayEvidenceReviewerClosureSummary"));
        assertTrue(source.contains("decisionReplayEvidenceReviewerClosureRollup"));
        assertTrue(source.contains("not replay proof"));
        assertTrue(source.contains("not scoring proof"));
        assertTrue(source.contains("not production validation"));
    }

    private static void assertChecklistItem(
            RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse checklist,
            String name,
            String status) {
        RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse item = checklist.items().stream()
                .filter(candidate -> name.equals(candidate.name()))
                .findFirst()
                .orElseThrow();
        assertEquals(status, item.status());
        assertFalse(item.description().isBlank());
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
