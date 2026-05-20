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

class RoutingDecisionReplayEvidenceLaneConsistencySummaryServiceTest {
    private final RoutingDecisionReplayEvidenceLaneConsistencySummaryService service =
            new RoutingDecisionReplayEvidenceLaneConsistencySummaryService();

    @Test
    void healthyConsistencySummaryIsDeterministicAndMatchesExistingLaneCounts() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse first =
                service.consistencySummary(
                        result.decisionReplayEvidenceStatusRollup(),
                        result.decisionReplayEvidenceLaneDependencyMap(),
                        result.decisionReplayEvidenceLaneReferenceIndex(),
                        result.decisionReplayEvidenceLaneDependencySummary());
        RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse second =
                service.consistencySummary(
                        result.decisionReplayEvidenceStatusRollup(),
                        result.decisionReplayEvidenceLaneDependencyMap(),
                        result.decisionReplayEvidenceLaneReferenceIndex(),
                        result.decisionReplayEvidenceLaneDependencySummary());

        assertEquals(first, second);
        assertEquals(result.decisionReplayEvidenceLaneConsistencySummary(), first);
        assertTrue(first.readOnly());
        assertEquals("decision-replay-evidence-lane-consistency-summary/v1",
                first.laneConsistencySummarySchemaVersion());
        assertEquals("CONSISTENT", first.status());
        assertEquals("PARTIAL", first.referenceIndexStatus());
        assertEquals("PARTIAL", first.dependencySummaryStatus());
        assertEquals("PARTIAL", first.statusRollupStatus());
        assertEquals("PARTIAL", first.dependencyMapStatus());
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", first.strategyId());
        assertEquals("green", first.selectedCandidateId());
        assertEquals(2, first.candidateCount());
        assertEquals(14, first.totalLaneCount());
        assertEquals(result.decisionReplayEvidenceLaneReferenceIndex().availableLaneCount(),
                first.availableLaneCount());
        assertEquals(result.decisionReplayEvidenceLaneReferenceIndex().partialLaneCount(),
                first.partialLaneCount());
        assertEquals(0, first.unknownLaneCount());
        assertEquals(13, first.dependencyMapLaneCount());
        assertEquals(14, first.referenceIndexLaneCount());
        assertEquals(14, first.dependencySummaryLaneCount());
        assertTrue(first.mismatchedCountFields().isEmpty());
        assertTrue(first.missingSurfaces().isEmpty());
        assertEquals(List.of(
                "status-rollup-present",
                "dependency-map-present",
                "reference-index-present",
                "dependency-summary-present",
                "lane-count-alignment",
                "available-count-alignment",
                "partial-count-alignment",
                "unknown-count-alignment",
                "lane-status-alignment",
                "dependency-map-context-count"),
                first.consistencyChecks().stream()
                        .map(DecisionReplayEvidenceLaneConsistencyCheckResponse::name)
                        .toList());
        assertTrue(first.consistencyChecks().stream()
                .allMatch(check -> "PASS".equals(check.status())));
        assertEquals("Lane evidence surfaces are consistent: reference index and dependency summary both report "
                + "14 lanes with " + first.availableLaneCount() + " available, "
                + first.partialLaneCount() + " partial, and 0 unknown.", first.summaryText());
        assertTrue(first.limitations().contains(
                "Read-only reviewer consistency metadata derived only from existing lane surfaces."));
        assertTrue(first.limitations().contains("Does not execute replay or perform what-if mutation."));
        assertTrue(first.limitations().contains("Does not recompute scores and is not scoring proof."));
        assertTrue(first.limitations().contains(
                "Not correctness validation, not production readiness, and not guaranteed replay."));
        assertTrue(first.boundaryNote().contains("does not inspect raw server input"));
        assertTrue(first.boundaryNote().contains("does not inspect raw request payloads"));
        assertTrue(first.boundaryNote().contains("does not execute replay"));
        assertTrue(first.boundaryNote().contains("does not perform what-if mutation"));
        assertTrue(first.boundaryNote().contains("does not change routing behavior"));
        assertTrue(first.boundaryNote().contains("does not recompute scores"));
        assertFalse(first.toString().contains("dependencyConsistencyFingerprint"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.mismatchedCountFields().add("other-count"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.consistencyChecks().add(new DecisionReplayEvidenceLaneConsistencyCheckResponse(
                        "other",
                        "PASS",
                        "expected",
                        "actual",
                        "detail")));
    }

    @Test
    void missingReferenceAndDependencySummaryReturnSafeUnknownWithoutInventedDetails() {
        RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse summary =
                service.consistencySummary(null, null, null, null);

        assertEquals("UNKNOWN", summary.status());
        assertEquals("UNKNOWN", summary.referenceIndexStatus());
        assertEquals("UNKNOWN", summary.dependencySummaryStatus());
        assertEquals("UNKNOWN", summary.statusRollupStatus());
        assertEquals("UNKNOWN", summary.dependencyMapStatus());
        assertEquals("UNKNOWN", summary.strategyId());
        assertNull(summary.selectedCandidateId());
        assertEquals(0, summary.candidateCount());
        assertEquals(0, summary.totalLaneCount());
        assertEquals(0, summary.availableLaneCount());
        assertEquals(0, summary.partialLaneCount());
        assertEquals(0, summary.unknownLaneCount());
        assertEquals(0, summary.dependencyMapLaneCount());
        assertEquals(0, summary.referenceIndexLaneCount());
        assertEquals(0, summary.dependencySummaryLaneCount());
        assertEquals(List.of(
                "decisionReplayEvidenceStatusRollup",
                "decisionReplayEvidenceLaneDependencyMap",
                "decisionReplayEvidenceLaneReferenceIndex",
                "decisionReplayEvidenceLaneDependencySummary"), summary.missingSurfaces());
        assertTrue(summary.mismatchedCountFields().isEmpty());
        assertEquals("Lane evidence consistency summary is UNKNOWN because required lane metadata is missing or "
                + "unavailable.", summary.summaryText());
        assertFalse(summary.toString().contains("dependencyConsistencyFingerprint"));
    }

    @Test
    void missingSupportingSurfaceReturnsDeterministicPartialStatus() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse summary =
                service.consistencySummary(
                        result.decisionReplayEvidenceStatusRollup(),
                        null,
                        result.decisionReplayEvidenceLaneReferenceIndex(),
                        result.decisionReplayEvidenceLaneDependencySummary());

        assertEquals("PARTIAL", summary.status());
        assertEquals(List.of("decisionReplayEvidenceLaneDependencyMap"), summary.missingSurfaces());
        assertEquals(0, summary.dependencyMapLaneCount());
        assertTrue(summary.mismatchedCountFields().isEmpty());
        assertEquals("UNKNOWN", summary.dependencyMapStatus());
        assertEquals("dependency-map-present", summary.consistencyChecks().get(1).name());
        assertEquals("UNKNOWN", summary.consistencyChecks().get(1).status());
    }

    @Test
    void mismatchedCountSurfaceReturnsDeterministicPartialStatus() {
        RoutingComparisonResultResponse result = healthyCompareResult();
        RoutingDecisionReplayEvidenceLaneDependencySummaryResponse original =
                result.decisionReplayEvidenceLaneDependencySummary();
        RoutingDecisionReplayEvidenceLaneDependencySummaryResponse altered =
                new RoutingDecisionReplayEvidenceLaneDependencySummaryResponse(
                        original.readOnly(),
                        original.laneDependencySummarySchemaVersion(),
                        original.source(),
                        original.status(),
                        original.strategyId(),
                        original.selectedCandidateId(),
                        original.candidateCount(),
                        original.totalLaneCount(),
                        original.availableLaneCount() + 1,
                        original.partialLaneCount(),
                        original.unknownLaneCount(),
                        original.rootLaneCount(),
                        original.terminalLaneCount(),
                        original.maxDependencyCount(),
                        original.maxDownstreamCount(),
                        original.densestDependencyLaneIds(),
                        original.widestDownstreamLaneIds(),
                        original.summaryText(),
                        original.limitations(),
                        original.boundaryNote());

        RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse summary =
                service.consistencySummary(
                        result.decisionReplayEvidenceStatusRollup(),
                        result.decisionReplayEvidenceLaneDependencyMap(),
                        result.decisionReplayEvidenceLaneReferenceIndex(),
                        altered);

        assertEquals("PARTIAL", summary.status());
        assertEquals(List.of("availableLaneCount"), summary.mismatchedCountFields());
        assertEquals("available-count-alignment", summary.consistencyChecks().get(5).name());
        assertEquals("WARN", summary.consistencyChecks().get(5).status());
        assertTrue(summary.summaryText().contains("partially aligned"));
    }

    @Test
    void noHealthyComparePathKeepsUnknownConsistencyWithoutInventedDecisionEvidence() {
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

        RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse summary =
                result.decisionReplayEvidenceLaneConsistencySummary();

        assertEquals("UNKNOWN", summary.status());
        assertEquals("UNKNOWN", summary.referenceIndexStatus());
        assertEquals("UNKNOWN", summary.dependencySummaryStatus());
        assertNull(summary.selectedCandidateId());
        assertEquals(0, summary.candidateCount());
        assertEquals(result.decisionReplayEvidenceLaneReferenceIndex().referenceItems().size(),
                summary.referenceIndexLaneCount());
        assertEquals(result.decisionReplayEvidenceLaneDependencySummary().totalLaneCount(),
                summary.dependencySummaryLaneCount());
        assertEquals(result.decisionReplayEvidenceLaneDependencyMap().dependencyItems().size(),
                summary.dependencyMapLaneCount());
        assertTrue(summary.missingSurfaces().isEmpty());
        assertTrue(summary.mismatchedCountFields().isEmpty());
        assertEquals("Lane evidence consistency summary is UNKNOWN because required lane metadata is missing or "
                + "unavailable.", summary.summaryText());
        assertTrue(result.candidateServersConsidered().isEmpty());
        assertTrue(result.scores().isEmpty());
        String text = summary.toString().toLowerCase(Locale.ROOT);
        assertFalse(text.contains("dependencyconsistencyfingerprint"));
        assertFalse(text.contains("candidate set is invented"));
        assertFalse(text.contains("score gap is invented"));
        assertFalse(text.contains("largest delta factor is invented"));
        assertFalse(text.contains("quality ranking is proven"));
        assertFalse(text.contains("approval is granted"));
        assertFalse(text.contains("correctness validation is proven"));
    }

    @Test
    void consistencySummaryServiceDoesNotUseRoutingInternalsOrUnsafeCapabilities() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayEvidenceLaneConsistencySummaryService.java"),
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
        assertFalse(normalized.contains("dependencyconsistencyfingerprint"));
        assertFalse(normalized.contains("production certification is proven"));
        assertFalse(normalized.contains("guaranteed replay is proven"));
        assertFalse(normalized.contains("quality ranking is proven"));
        assertFalse(normalized.contains("approval is granted"));
        assertFalse(normalized.contains("correctness validation is proven"));
        assertTrue(source.contains("Derived only from already-built results[]"));
        assertTrue(source.contains("does not inspect raw server input"));
        assertTrue(source.contains("does not inspect raw request"));
        assertTrue(source.contains("payloads"));
        assertTrue(source.contains("does not recompute scores"));
        assertTrue(source.contains("does not infer hidden"));
        assertTrue(source.contains("scoring"));
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
