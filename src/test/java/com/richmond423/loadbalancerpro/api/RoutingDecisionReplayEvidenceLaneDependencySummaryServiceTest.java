package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class RoutingDecisionReplayEvidenceLaneDependencySummaryServiceTest {
    private final RoutingDecisionReplayEvidenceLaneDependencySummaryService service =
            new RoutingDecisionReplayEvidenceLaneDependencySummaryService();

    @Test
    void dependencySummaryIsDeterministicAndMatchesReferenceIndexCounts() {
        RoutingDecisionReplayEvidenceLaneReferenceIndexResponse referenceIndex =
                healthyCompareResult().decisionReplayEvidenceLaneReferenceIndex();

        RoutingDecisionReplayEvidenceLaneDependencySummaryResponse first =
                service.dependencySummary(referenceIndex);
        RoutingDecisionReplayEvidenceLaneDependencySummaryResponse second =
                service.dependencySummary(referenceIndex);

        assertEquals(first, second);
        assertTrue(first.readOnly());
        assertEquals("decision-replay-evidence-lane-dependency-summary/v1",
                first.laneDependencySummarySchemaVersion());
        assertEquals(referenceIndex.status(), first.status());
        assertEquals(referenceIndex.strategyId(), first.strategyId());
        assertEquals(referenceIndex.selectedCandidateId(), first.selectedCandidateId());
        assertEquals(referenceIndex.candidateCount(), first.candidateCount());
        assertEquals(referenceIndex.referenceItems().size(), first.totalLaneCount());
        assertEquals(referenceIndex.availableLaneCount(), first.availableLaneCount());
        assertEquals(referenceIndex.partialLaneCount(), first.partialLaneCount());
        assertEquals(referenceIndex.unknownLaneCount(), first.unknownLaneCount());
        assertEquals(1, first.rootLaneCount());
        assertEquals(2, first.terminalLaneCount());
        assertEquals(13, first.maxDependencyCount());
        assertEquals(12, first.maxDownstreamCount());
        assertEquals(List.of("evidence-lane-dependency-map-reference"), first.densestDependencyLaneIds());
        assertEquals(List.of("decision-vector-reference"), first.widestDownstreamLaneIds());
        assertEquals("Reference index is " + referenceIndex.status()
                + " with 14 lanes, "
                + referenceIndex.availableLaneCount()
                + " available, "
                + referenceIndex.partialLaneCount()
                + " partial, and "
                + referenceIndex.unknownLaneCount()
                + " unknown.", first.summaryText());
        assertTrue(first.limitations().contains("Does not execute replay or perform what-if mutation."));
        assertTrue(first.limitations().contains("Does not recompute scores and is not scoring proof."));
        assertTrue(first.limitations().contains("Not correctness validation and not production readiness."));
        assertTrue(first.boundaryNote().contains("does not execute replay"));
        assertTrue(first.boundaryNote().contains("does not perform what-if mutation"));
        assertTrue(first.boundaryNote().contains("does not change routing behavior"));
        assertTrue(first.boundaryNote().contains("does not recompute scores"));
        assertTrue(first.boundaryNote().contains("does not inspect raw server input"));
        assertTrue(first.boundaryNote().contains("does not inspect raw request payloads"));
        assertFalse(first.toString().contains("dependencySummaryFingerprint"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.densestDependencyLaneIds().add("other-lane"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.limitations().add("other limitation"));
    }

    @Test
    void missingReferenceIndexReturnsSafeUnknownSummary() {
        RoutingDecisionReplayEvidenceLaneDependencySummaryResponse summary =
                service.dependencySummary(null);

        assertEquals("UNKNOWN", summary.status());
        assertEquals("UNKNOWN", summary.strategyId());
        assertNull(summary.selectedCandidateId());
        assertEquals(0, summary.candidateCount());
        assertEquals(0, summary.totalLaneCount());
        assertEquals(0, summary.availableLaneCount());
        assertEquals(0, summary.partialLaneCount());
        assertEquals(0, summary.unknownLaneCount());
        assertEquals(0, summary.rootLaneCount());
        assertEquals(0, summary.terminalLaneCount());
        assertEquals(0, summary.maxDependencyCount());
        assertEquals(0, summary.maxDownstreamCount());
        assertTrue(summary.densestDependencyLaneIds().isEmpty());
        assertTrue(summary.widestDownstreamLaneIds().isEmpty());
        assertEquals("Reference index is UNKNOWN with 0 lanes, 0 available, 0 partial, and 0 unknown.",
                summary.summaryText());
        assertFalse(summary.toString().contains("dependencySummaryFingerprint"));
    }

    @Test
    void noHealthyComparePathKeepsSafeUnknownSummaryWithoutInventedDecisionEvidence() {
        RoutingComparisonResultResponse result = new RoutingComparisonService().compare(new RoutingComparisonRequest(
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
                        null))))
                .results()
                .get(0);

        RoutingDecisionReplayEvidenceLaneDependencySummaryResponse summary =
                result.decisionReplayEvidenceLaneDependencySummary();

        assertEquals("UNKNOWN", summary.status());
        assertNull(summary.selectedCandidateId());
        assertEquals(0, summary.candidateCount());
        assertEquals(result.decisionReplayEvidenceLaneReferenceIndex().referenceItems().size(),
                summary.totalLaneCount());
        assertEquals(result.decisionReplayEvidenceLaneReferenceIndex().availableLaneCount(),
                summary.availableLaneCount());
        assertEquals(result.decisionReplayEvidenceLaneReferenceIndex().partialLaneCount(),
                summary.partialLaneCount());
        assertEquals(result.decisionReplayEvidenceLaneReferenceIndex().unknownLaneCount(),
                summary.unknownLaneCount());
        assertEquals(1, summary.rootLaneCount());
        assertEquals(2, summary.terminalLaneCount());
        assertEquals(List.of("evidence-lane-dependency-map-reference"), summary.densestDependencyLaneIds());
        assertEquals(List.of("decision-vector-reference"), summary.widestDownstreamLaneIds());
        String text = summary.toString().toLowerCase(Locale.ROOT);
        assertFalse(text.contains("dependencysummaryfingerprint"));
        assertFalse(text.contains("candidate set is invented"));
        assertFalse(text.contains("score gap is invented"));
        assertFalse(text.contains("largest delta factor is invented"));
        assertFalse(text.contains("quality ranking is proven"));
        assertFalse(text.contains("approval is granted"));
        assertFalse(text.contains("correctness validation is proven"));
    }

    @Test
    void dependencySummaryServiceDoesNotUseRoutingInternalsOrUnsafeCapabilities() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayEvidenceLaneDependencySummaryService.java"),
                StandardCharsets.UTF_8);
        String normalized = source.toLowerCase(Locale.ROOT);

        assertFalse(source.contains("ServerScoreCalculator"));
        assertFalse(source.contains("ServerStateVector"));
        assertFalse(source.contains("MessageDigest"));
        assertFalse(source.contains("SHA-256"));
        assertFalse(normalized.contains("sha256"));
        assertFalse(normalized.contains("instant.now"));
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
        assertFalse(normalized.contains("dependencysummaryfingerprint"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("quality ranking is proven"));
        assertFalse(normalized.contains("approval is granted"));
        assertFalse(normalized.contains("correctness validation is proven"));
        assertTrue(source.contains("Derived only from results[].decisionReplayEvidenceLaneReferenceIndex"));
        assertTrue(source.contains("does not inspect raw server input"));
        assertTrue(source.contains("not inspect raw request payloads"));
        assertTrue(source.contains("does not recompute scores"));
        assertTrue(source.contains("does not infer hidden scoring"));
        assertTrue(source.contains("does not retune weights"));
        assertTrue(source.contains("does not change routing behavior"));
    }

    private static RoutingComparisonResultResponse healthyCompareResult() {
        return new RoutingComparisonService().compare(new RoutingComparisonRequest(
                List.of("TAIL_LATENCY_POWER_OF_TWO"),
                List.of(
                        new RoutingServerStateInput(
                                "green",
                                true,
                                5,
                                100.0,
                                100.0,
                                null,
                                20.0,
                                40.0,
                                80.0,
                                0.01,
                                1,
                                null),
                        new RoutingServerStateInput(
                                "blue",
                                true,
                                75,
                                100.0,
                                100.0,
                                null,
                                35.0,
                                120.0,
                                220.0,
                                0.15,
                                10,
                                null))))
                .results()
                .get(0);
    }
}
