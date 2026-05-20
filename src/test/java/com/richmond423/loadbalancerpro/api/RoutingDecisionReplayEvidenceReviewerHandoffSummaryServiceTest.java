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

class RoutingDecisionReplayEvidenceReviewerHandoffSummaryServiceTest {
    private final RoutingDecisionReplayEvidenceReviewerHandoffSummaryService service =
            new RoutingDecisionReplayEvidenceReviewerHandoffSummaryService();

    @Test
    void healthyReviewerHandoffSummaryIsDeterministicAndDerivedFromExistingReviewerMetadata() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse first = service.reviewerHandoffSummary(
                result.decisionReplayEvidenceStatusRollup(),
                result.decisionReplayEvidenceLaneDependencyMap(),
                result.decisionReplayEvidenceLaneReferenceIndex(),
                result.decisionReplayEvidenceLaneDependencySummary(),
                result.decisionReplayEvidenceLaneConsistencySummary(),
                result.decisionReplayEvidenceReviewerSnapshot(),
                result.decisionReplayEvidenceReviewerGuidance());
        RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse second = service.reviewerHandoffSummary(
                result.decisionReplayEvidenceStatusRollup(),
                result.decisionReplayEvidenceLaneDependencyMap(),
                result.decisionReplayEvidenceLaneReferenceIndex(),
                result.decisionReplayEvidenceLaneDependencySummary(),
                result.decisionReplayEvidenceLaneConsistencySummary(),
                result.decisionReplayEvidenceReviewerSnapshot(),
                result.decisionReplayEvidenceReviewerGuidance());

        assertEquals(first, second);
        assertEquals(result.decisionReplayEvidenceReviewerHandoffSummary(), first);
        assertTrue(first.readOnly());
        assertEquals("decision-replay-evidence-reviewer-handoff-summary/v1",
                first.reviewerHandoffSummarySchemaVersion());
        assertEquals("PARTIAL", first.status());
        assertEquals("REVIEW", first.handoffPriority());
        assertEquals("PARTIAL", first.reviewerSnapshotStatus());
        assertEquals("PARTIAL", first.reviewerGuidanceStatus());
        assertEquals("CONSISTENT", first.consistencyStatus());
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", first.strategyId());
        assertEquals("edge-alpha", first.selectedCandidateId());
        assertEquals(3, first.candidateCount());
        assertEquals(14, first.totalLaneCount());
        assertEquals(4, first.availableLaneCount());
        assertEquals(10, first.partialLaneCount());
        assertEquals(0, first.unknownLaneCount());
        assertEquals(List.of(
                "Reviewer snapshot is PARTIAL.",
                "Reviewer guidance is PARTIAL.",
                "Consistency summary is CONSISTENT.",
                "14 evidence lanes summarized: 4 available, 10 partial, 0 unknown.",
                "7 handoff surfaces referenced."), first.handoffBullets());
        assertEquals(List.of(
                "Review partial or unknown evidence lanes before operator handoff.",
                "Keep lab-only limitations attached to the handoff."), first.operatorFollowUpItems());
        assertEquals(List.of(
                "decisionReplayEvidenceStatusRollup",
                "decisionReplayEvidenceLaneDependencyMap",
                "decisionReplayEvidenceLaneReferenceIndex",
                "decisionReplayEvidenceLaneDependencySummary",
                "decisionReplayEvidenceLaneConsistencySummary",
                "decisionReplayEvidenceReviewerSnapshot",
                "decisionReplayEvidenceReviewerGuidance"), first.evidenceSurfacesReferenced());
        assertTrue(first.cautionNotes().contains("Reviewer snapshot status is PARTIAL."));
        assertTrue(first.cautionNotes().contains("Reviewer guidance status is PARTIAL."));
        assertTrue(first.cautionNotes().contains("10 evidence lanes remain PARTIAL."));
        assertTrue(first.cautionNotes().contains(
                "Handoff summary is read-only reviewer metadata, not replay proof, scoring proof, "
                        + "correctness validation, production readiness, or production validation."));
        assertEquals("Reviewer handoff summary is PARTIAL with REVIEW priority, snapshot status PARTIAL, "
                + "guidance status PARTIAL, consistency status CONSISTENT, and 14 lanes (4 available, "
                + "10 partial, 0 unknown).", first.summaryText());
        assertTrue(first.limitations().contains(
                "Read-only reviewer handoff metadata derived only from existing decision replay reviewer metadata."));
        assertTrue(first.limitations().contains("Not replay proof and does not execute replay."));
        assertTrue(first.limitations().contains("Not scoring proof and does not recompute scores."));
        assertTrue(first.limitations().contains(
                "Not correctness validation, not production readiness, not production certification, "
                        + "not guaranteed replay, and not production validation."));
        assertTrue(first.boundaryNote().contains("does not inspect raw server input"));
        assertTrue(first.boundaryNote().contains("does not inspect raw request payloads"));
        assertTrue(first.boundaryNote().contains("does not execute replay"));
        assertTrue(first.boundaryNote().contains("does not perform what-if mutation"));
        assertTrue(first.boundaryNote().contains("does not change routing behavior"));
        assertTrue(first.boundaryNote().contains("does not recompute scores"));
        assertFalse(first.toString().contains("reviewerHandoffFingerprint"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.handoffBullets().add("other bullet"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.limitations().add("other limitation"));
    }

    @Test
    void missingSourceSurfacesReturnSafeUnknownWithoutInventedDetails() {
        RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse handoff =
                service.reviewerHandoffSummary(null, null, null, null, null, null, null);

        assertEquals("UNKNOWN", handoff.status());
        assertEquals("UNKNOWN", handoff.handoffPriority());
        assertEquals("UNKNOWN", handoff.reviewerSnapshotStatus());
        assertEquals("UNKNOWN", handoff.reviewerGuidanceStatus());
        assertEquals("UNKNOWN", handoff.consistencyStatus());
        assertEquals("UNKNOWN", handoff.strategyId());
        assertNull(handoff.selectedCandidateId());
        assertEquals(0, handoff.candidateCount());
        assertEquals(0, handoff.totalLaneCount());
        assertEquals(0, handoff.availableLaneCount());
        assertEquals(0, handoff.partialLaneCount());
        assertEquals(0, handoff.unknownLaneCount());
        assertEquals(List.of(
                "decisionReplayEvidenceStatusRollup",
                "decisionReplayEvidenceLaneDependencyMap",
                "decisionReplayEvidenceLaneReferenceIndex",
                "decisionReplayEvidenceLaneDependencySummary",
                "decisionReplayEvidenceLaneConsistencySummary",
                "decisionReplayEvidenceReviewerSnapshot",
                "decisionReplayEvidenceReviewerGuidance"), handoff.cautionNotes().stream()
                        .filter(note -> note.startsWith("Missing surface: "))
                        .map(note -> note.substring("Missing surface: ".length()))
                        .toList());
        assertTrue(handoff.evidenceSurfacesReferenced().isEmpty());
        assertEquals(List.of("Run routing comparison before using reviewer handoff metadata."),
                handoff.operatorFollowUpItems());
        assertEquals("Decision replay evidence reviewer handoff summary is UNKNOWN because required reviewer "
                + "metadata is missing or no selected candidate evidence is available.", handoff.summaryText());
        assertFalse(handoff.toString().contains("reviewerHandoffFingerprint"));
    }

    @Test
    void missingSupportingSurfaceReturnsDeterministicPartialHandoffSummary() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse handoff = service.reviewerHandoffSummary(
                null,
                result.decisionReplayEvidenceLaneDependencyMap(),
                result.decisionReplayEvidenceLaneReferenceIndex(),
                result.decisionReplayEvidenceLaneDependencySummary(),
                result.decisionReplayEvidenceLaneConsistencySummary(),
                result.decisionReplayEvidenceReviewerSnapshot(),
                result.decisionReplayEvidenceReviewerGuidance());

        assertEquals("PARTIAL", handoff.status());
        assertEquals("REVIEW", handoff.handoffPriority());
        assertTrue(handoff.cautionNotes().contains("Missing surface: decisionReplayEvidenceStatusRollup"));
        assertFalse(handoff.evidenceSurfacesReferenced().contains("decisionReplayEvidenceStatusRollup"));
        assertEquals(14, handoff.totalLaneCount());
        assertEquals(4, handoff.availableLaneCount());
        assertEquals(10, handoff.partialLaneCount());
    }

    @Test
    void noHealthyComparePathKeepsUnknownReviewerHandoffWithoutInventedDecisionEvidence() {
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

        RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse handoff =
                result.decisionReplayEvidenceReviewerHandoffSummary();

        assertEquals("UNKNOWN", handoff.status());
        assertEquals("UNKNOWN", handoff.handoffPriority());
        assertEquals("UNKNOWN", handoff.reviewerSnapshotStatus());
        assertEquals("UNKNOWN", handoff.reviewerGuidanceStatus());
        assertEquals("UNKNOWN", handoff.consistencyStatus());
        assertNull(handoff.selectedCandidateId());
        assertEquals(0, handoff.candidateCount());
        assertEquals(0, handoff.totalLaneCount());
        assertEquals(0, handoff.availableLaneCount());
        assertEquals(0, handoff.partialLaneCount());
        assertEquals(0, handoff.unknownLaneCount());
        assertEquals(List.of(
                "7 reviewer handoff surfaces checked",
                "Reviewer handoff summary status is UNKNOWN"), handoff.handoffBullets());
        assertTrue(handoff.cautionNotes().contains("No selected candidate evidence is available for reviewer handoff."));
        assertTrue(result.candidateServersConsidered().isEmpty());
        assertTrue(result.scores().isEmpty());
        String text = handoff.toString().toLowerCase(Locale.ROOT);
        assertFalse(text.contains("reviewerhandofffingerprint"));
        assertFalse(text.contains("candidate set is invented"));
        assertFalse(text.contains("score gap is invented"));
        assertFalse(text.contains("largest delta factor is invented"));
        assertFalse(text.contains("quality ranking is proven"));
        assertFalse(text.contains("approval is granted"));
        assertFalse(text.contains("correctness validation is proven"));
    }

    @Test
    void reviewerHandoffSummaryServiceDoesNotUseRoutingInternalsOrUnsafeCapabilities() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayEvidenceReviewerHandoffSummaryService.java"),
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
        assertFalse(normalized.contains("reviewerhandofffingerprint"));
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
