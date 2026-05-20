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

class RoutingDecisionReplayEvidenceReviewerSnapshotServiceTest {
    private final RoutingDecisionReplayEvidenceReviewerSnapshotService service =
            new RoutingDecisionReplayEvidenceReviewerSnapshotService();

    @Test
    void healthyReviewerSnapshotIsDeterministicAndSummarizesExistingEvidenceSurfaces() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceReviewerSnapshotResponse first =
                service.reviewerSnapshot(
                        result.decisionReplayEvidenceStatusRollup(),
                        result.decisionReplayEvidenceLaneDependencyMap(),
                        result.decisionReplayEvidenceLaneReferenceIndex(),
                        result.decisionReplayEvidenceLaneDependencySummary(),
                        result.decisionReplayEvidenceLaneConsistencySummary());
        RoutingDecisionReplayEvidenceReviewerSnapshotResponse second =
                service.reviewerSnapshot(
                        result.decisionReplayEvidenceStatusRollup(),
                        result.decisionReplayEvidenceLaneDependencyMap(),
                        result.decisionReplayEvidenceLaneReferenceIndex(),
                        result.decisionReplayEvidenceLaneDependencySummary(),
                        result.decisionReplayEvidenceLaneConsistencySummary());

        assertEquals(first, second);
        assertEquals(result.decisionReplayEvidenceReviewerSnapshot(), first);
        assertTrue(first.readOnly());
        assertEquals("decision-replay-evidence-reviewer-snapshot/v1",
                first.reviewerSnapshotSchemaVersion());
        assertEquals("PARTIAL", first.status());
        assertEquals("CONSISTENT", first.consistencyStatus());
        assertEquals("PARTIAL", first.referenceIndexStatus());
        assertEquals("PARTIAL", first.dependencySummaryStatus());
        assertEquals("PARTIAL", first.statusRollupStatus());
        assertEquals("PARTIAL", first.dependencyMapStatus());
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", first.strategyId());
        assertEquals("edge-alpha", first.selectedCandidateId());
        assertEquals(3, first.candidateCount());
        assertEquals(14, first.totalLaneCount());
        assertEquals(4, first.availableLaneCount());
        assertEquals(10, first.partialLaneCount());
        assertEquals(0, first.unknownLaneCount());
        assertEquals(5, first.checkedSurfaceCount());
        assertEquals(0, first.missingSurfaceCount());
        assertTrue(first.missingSurfaces().isEmpty());
        assertEquals(List.of(
                "14 evidence lanes summarized",
                "4 lanes available",
                "10 lanes partial",
                "0 lanes unknown",
                "5 reviewer evidence surfaces checked",
                "Consistency summary reports CONSISTENT"), first.reviewerHighlights());
        assertEquals(List.of("10 lanes remain PARTIAL."), first.reviewerWarnings());
        assertEquals("Reviewer snapshot is PARTIAL with 14 lanes, 4 available, 10 partial, 0 unknown, "
                + "0 missing surface(s), and consistency status CONSISTENT.", first.summaryText());
        assertTrue(first.limitations().contains(
                "Read-only reviewer snapshot metadata derived only from existing decision replay evidence surfaces."));
        assertTrue(first.limitations().contains("Does not execute replay or perform what-if mutation."));
        assertTrue(first.limitations().contains("Does not recompute scores and is not scoring proof."));
        assertTrue(first.limitations().contains(
                "Not correctness validation, not production readiness, not production certification, and not guaranteed replay."));
        assertTrue(first.boundaryNote().contains("does not inspect raw server input"));
        assertTrue(first.boundaryNote().contains("does not inspect raw request payloads"));
        assertTrue(first.boundaryNote().contains("does not execute replay"));
        assertTrue(first.boundaryNote().contains("does not perform what-if mutation"));
        assertTrue(first.boundaryNote().contains("does not change routing behavior"));
        assertTrue(first.boundaryNote().contains("does not recompute scores"));
        assertFalse(first.toString().contains("reviewerSnapshotFingerprint"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.reviewerHighlights().add("other highlight"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.limitations().add("other limitation"));
    }

    @Test
    void missingSourceSurfacesReturnSafeUnknownWithoutInventedDetails() {
        RoutingDecisionReplayEvidenceReviewerSnapshotResponse snapshot =
                service.reviewerSnapshot(null, null, null, null, null);

        assertEquals("UNKNOWN", snapshot.status());
        assertEquals("UNKNOWN", snapshot.consistencyStatus());
        assertEquals("UNKNOWN", snapshot.referenceIndexStatus());
        assertEquals("UNKNOWN", snapshot.dependencySummaryStatus());
        assertEquals("UNKNOWN", snapshot.statusRollupStatus());
        assertEquals("UNKNOWN", snapshot.dependencyMapStatus());
        assertEquals("UNKNOWN", snapshot.strategyId());
        assertNull(snapshot.selectedCandidateId());
        assertEquals(0, snapshot.candidateCount());
        assertEquals(0, snapshot.totalLaneCount());
        assertEquals(0, snapshot.availableLaneCount());
        assertEquals(0, snapshot.partialLaneCount());
        assertEquals(0, snapshot.unknownLaneCount());
        assertEquals(0, snapshot.checkedSurfaceCount());
        assertEquals(5, snapshot.missingSurfaceCount());
        assertEquals(List.of(
                "decisionReplayEvidenceStatusRollup",
                "decisionReplayEvidenceLaneDependencyMap",
                "decisionReplayEvidenceLaneReferenceIndex",
                "decisionReplayEvidenceLaneDependencySummary",
                "decisionReplayEvidenceLaneConsistencySummary"), snapshot.missingSurfaces());
        assertEquals(List.of("0 reviewer evidence surfaces checked"), snapshot.reviewerHighlights());
        assertTrue(snapshot.reviewerWarnings().contains(
                "No selected candidate evidence is available for reviewer snapshot."));
        assertTrue(snapshot.reviewerWarnings().contains(
                "Missing surface: decisionReplayEvidenceLaneConsistencySummary"));
        assertEquals("Decision replay evidence reviewer snapshot is UNKNOWN because required reviewer evidence "
                + "surfaces are missing or no selected candidate evidence is available.", snapshot.summaryText());
        assertFalse(snapshot.toString().contains("reviewerSnapshotFingerprint"));
    }

    @Test
    void missingSupportingSurfaceReturnsDeterministicPartialStatus() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceReviewerSnapshotResponse snapshot =
                service.reviewerSnapshot(
                        null,
                        result.decisionReplayEvidenceLaneDependencyMap(),
                        result.decisionReplayEvidenceLaneReferenceIndex(),
                        result.decisionReplayEvidenceLaneDependencySummary(),
                        result.decisionReplayEvidenceLaneConsistencySummary());

        assertEquals("PARTIAL", snapshot.status());
        assertEquals(4, snapshot.checkedSurfaceCount());
        assertEquals(1, snapshot.missingSurfaceCount());
        assertEquals(List.of("decisionReplayEvidenceStatusRollup"), snapshot.missingSurfaces());
        assertTrue(snapshot.reviewerWarnings().contains("Missing surface: decisionReplayEvidenceStatusRollup"));
        assertEquals(14, snapshot.totalLaneCount());
        assertEquals("CONSISTENT", snapshot.consistencyStatus());
    }

    @Test
    void noHealthyComparePathKeepsUnknownReviewerSnapshotWithoutInventedDecisionEvidence() {
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

        RoutingDecisionReplayEvidenceReviewerSnapshotResponse snapshot =
                result.decisionReplayEvidenceReviewerSnapshot();

        assertEquals("UNKNOWN", snapshot.status());
        assertEquals("UNKNOWN", snapshot.consistencyStatus());
        assertEquals("UNKNOWN", snapshot.referenceIndexStatus());
        assertEquals("UNKNOWN", snapshot.dependencySummaryStatus());
        assertNull(snapshot.selectedCandidateId());
        assertEquals(0, snapshot.candidateCount());
        assertEquals(0, snapshot.totalLaneCount());
        assertEquals(0, snapshot.availableLaneCount());
        assertEquals(0, snapshot.partialLaneCount());
        assertEquals(0, snapshot.unknownLaneCount());
        assertEquals(5, snapshot.checkedSurfaceCount());
        assertEquals(0, snapshot.missingSurfaceCount());
        assertTrue(snapshot.missingSurfaces().isEmpty());
        assertTrue(snapshot.reviewerWarnings().contains(
                "No selected candidate evidence is available for reviewer snapshot."));
        assertTrue(result.candidateServersConsidered().isEmpty());
        assertTrue(result.scores().isEmpty());
        String text = snapshot.toString().toLowerCase(Locale.ROOT);
        assertFalse(text.contains("reviewersnapshotfingerprint"));
        assertFalse(text.contains("candidate set is invented"));
        assertFalse(text.contains("score gap is invented"));
        assertFalse(text.contains("largest delta factor is invented"));
        assertFalse(text.contains("quality ranking is proven"));
        assertFalse(text.contains("approval is granted"));
        assertFalse(text.contains("correctness validation is proven"));
    }

    @Test
    void reviewerSnapshotServiceDoesNotUseRoutingInternalsOrUnsafeCapabilities() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayEvidenceReviewerSnapshotService.java"),
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
        assertFalse(normalized.contains("reviewersnapshotfingerprint"));
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
                                null))))
                .results()
                .get(0);
    }
}
