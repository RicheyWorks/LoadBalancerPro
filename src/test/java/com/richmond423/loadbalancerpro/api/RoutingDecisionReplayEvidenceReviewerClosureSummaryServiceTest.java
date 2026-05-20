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

class RoutingDecisionReplayEvidenceReviewerClosureSummaryServiceTest {
    private final RoutingDecisionReplayEvidenceReviewerClosureSummaryService service =
            new RoutingDecisionReplayEvidenceReviewerClosureSummaryService();

    @Test
    void healthyReviewerClosureSummaryIsDeterministicAndDerivedFromExistingReviewerMetadata() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse first = service.reviewerClosureSummary(
                result.decisionReplayEvidenceStatusRollup(),
                result.decisionReplayEvidenceLaneDependencyMap(),
                result.decisionReplayEvidenceLaneReferenceIndex(),
                result.decisionReplayEvidenceLaneDependencySummary(),
                result.decisionReplayEvidenceLaneConsistencySummary(),
                result.decisionReplayEvidenceReviewerSnapshot(),
                result.decisionReplayEvidenceReviewerGuidance(),
                result.decisionReplayEvidenceReviewerHandoffSummary());
        RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse second = service.reviewerClosureSummary(
                result.decisionReplayEvidenceStatusRollup(),
                result.decisionReplayEvidenceLaneDependencyMap(),
                result.decisionReplayEvidenceLaneReferenceIndex(),
                result.decisionReplayEvidenceLaneDependencySummary(),
                result.decisionReplayEvidenceLaneConsistencySummary(),
                result.decisionReplayEvidenceReviewerSnapshot(),
                result.decisionReplayEvidenceReviewerGuidance(),
                result.decisionReplayEvidenceReviewerHandoffSummary());

        assertEquals(first, second);
        assertEquals(result.decisionReplayEvidenceReviewerClosureSummary(), first);
        assertTrue(first.readOnly());
        assertEquals("decision-replay-evidence-reviewer-closure-summary/v1",
                first.reviewerClosureSummarySchemaVersion());
        assertEquals("PARTIAL", first.status());
        assertEquals("REVIEW_COMPLETE_WITH_LIMITATIONS", first.closureDisposition());
        assertEquals("PARTIAL", first.reviewerSnapshotStatus());
        assertEquals("PARTIAL", first.reviewerGuidanceStatus());
        assertEquals("PARTIAL", first.reviewerHandoffStatus());
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
                "Reviewer handoff is PARTIAL.",
                "Consistency summary is CONSISTENT.",
                "14 evidence lanes summarized: 4 available, 10 partial, 0 unknown.",
                "8 closure surfaces referenced."), first.closureBullets());
        assertEquals(List.of(
                "Reviewer metadata was generated deterministically from exposed compare surfaces.",
                "Consistency summary reported CONSISTENT.",
                "Evidence lane counts were stable across equivalent requests.",
                "Reviewer closure remains read-only and lab-only."), first.safeConclusions());
        assertEquals(List.of(
                "Not replay proof.",
                "Not scoring proof.",
                "Not correctness validation.",
                "Not production readiness.",
                "Not production certification.",
                "Not guaranteed replay.",
                "Not production validation."), first.unresolvedBoundaries());
        assertEquals(List.of(
                "decisionReplayEvidenceStatusRollup",
                "decisionReplayEvidenceLaneDependencyMap",
                "decisionReplayEvidenceLaneReferenceIndex",
                "decisionReplayEvidenceLaneDependencySummary",
                "decisionReplayEvidenceLaneConsistencySummary",
                "decisionReplayEvidenceReviewerSnapshot",
                "decisionReplayEvidenceReviewerGuidance",
                "decisionReplayEvidenceReviewerHandoffSummary"), first.evidenceSurfacesReferenced());
        assertEquals("Reviewer closure summary is PARTIAL with REVIEW_COMPLETE_WITH_LIMITATIONS disposition, "
                + "snapshot status PARTIAL, guidance status PARTIAL, handoff status PARTIAL, "
                + "consistency status CONSISTENT, and 14 lanes (4 available, 10 partial, 0 unknown).",
                first.summaryText());
        assertTrue(first.limitations().contains(
                "Read-only reviewer closure metadata derived only from existing decision replay reviewer metadata."));
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
        assertFalse(first.toString().contains("reviewerClosureFingerprint"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.closureBullets().add("other bullet"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.safeConclusions().add("other conclusion"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.unresolvedBoundaries().add("other boundary"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.limitations().add("other limitation"));
    }

    @Test
    void missingSourceSurfacesReturnSafeUnknownWithoutInventedDetails() {
        RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse closure =
                service.reviewerClosureSummary(null, null, null, null, null, null, null, null);

        assertEquals("UNKNOWN", closure.status());
        assertEquals("UNKNOWN", closure.closureDisposition());
        assertEquals("UNKNOWN", closure.reviewerSnapshotStatus());
        assertEquals("UNKNOWN", closure.reviewerGuidanceStatus());
        assertEquals("UNKNOWN", closure.reviewerHandoffStatus());
        assertEquals("UNKNOWN", closure.consistencyStatus());
        assertEquals("UNKNOWN", closure.strategyId());
        assertNull(closure.selectedCandidateId());
        assertEquals(0, closure.candidateCount());
        assertEquals(0, closure.totalLaneCount());
        assertEquals(0, closure.availableLaneCount());
        assertEquals(0, closure.partialLaneCount());
        assertEquals(0, closure.unknownLaneCount());
        assertTrue(closure.safeConclusions().isEmpty());
        assertTrue(closure.evidenceSurfacesReferenced().isEmpty());
        assertEquals(List.of("0 reviewer closure surfaces checked"), closure.closureBullets());
        assertEquals("Decision replay evidence reviewer closure summary is UNKNOWN because required reviewer "
                + "metadata is missing or no selected candidate evidence is available.", closure.summaryText());
        assertFalse(closure.toString().contains("reviewerClosureFingerprint"));
    }

    @Test
    void missingSupportingSurfaceReturnsDeterministicPartialClosureSummary() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse closure = service.reviewerClosureSummary(
                null,
                result.decisionReplayEvidenceLaneDependencyMap(),
                result.decisionReplayEvidenceLaneReferenceIndex(),
                result.decisionReplayEvidenceLaneDependencySummary(),
                result.decisionReplayEvidenceLaneConsistencySummary(),
                result.decisionReplayEvidenceReviewerSnapshot(),
                result.decisionReplayEvidenceReviewerGuidance(),
                result.decisionReplayEvidenceReviewerHandoffSummary());

        assertEquals("PARTIAL", closure.status());
        assertEquals("REVIEW_INCOMPLETE", closure.closureDisposition());
        assertFalse(closure.evidenceSurfacesReferenced().contains("decisionReplayEvidenceStatusRollup"));
        assertEquals(14, closure.totalLaneCount());
        assertEquals(4, closure.availableLaneCount());
        assertEquals(10, closure.partialLaneCount());
    }

    @Test
    void noHealthyComparePathKeepsUnknownReviewerClosureWithoutInventedDecisionEvidence() {
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

        RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse closure =
                result.decisionReplayEvidenceReviewerClosureSummary();

        assertEquals("UNKNOWN", closure.status());
        assertEquals("UNKNOWN", closure.closureDisposition());
        assertEquals("UNKNOWN", closure.reviewerSnapshotStatus());
        assertEquals("UNKNOWN", closure.reviewerGuidanceStatus());
        assertEquals("UNKNOWN", closure.reviewerHandoffStatus());
        assertEquals("UNKNOWN", closure.consistencyStatus());
        assertNull(closure.selectedCandidateId());
        assertEquals(0, closure.candidateCount());
        assertEquals(0, closure.totalLaneCount());
        assertEquals(0, closure.availableLaneCount());
        assertEquals(0, closure.partialLaneCount());
        assertEquals(0, closure.unknownLaneCount());
        assertEquals(List.of(
                "8 reviewer closure surfaces checked",
                "Reviewer closure summary status is UNKNOWN"), closure.closureBullets());
        assertTrue(closure.safeConclusions().isEmpty());
        assertTrue(closure.unresolvedBoundaries().contains("Not replay proof."));
        assertTrue(result.candidateServersConsidered().isEmpty());
        assertTrue(result.scores().isEmpty());
        String text = closure.toString().toLowerCase(Locale.ROOT);
        assertFalse(text.contains("reviewerclosurefingerprint"));
        assertFalse(text.contains("candidate set is invented"));
        assertFalse(text.contains("score gap is invented"));
        assertFalse(text.contains("largest delta factor is invented"));
        assertFalse(text.contains("quality ranking is proven"));
        assertFalse(text.contains("approval is granted"));
        assertFalse(text.contains("correctness validation is proven"));
    }

    @Test
    void reviewerClosureSummaryServiceDoesNotUseRoutingInternalsOrUnsafeCapabilities() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayEvidenceReviewerClosureSummaryService.java"),
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
        assertFalse(normalized.contains("reviewerclosurefingerprint"));
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
