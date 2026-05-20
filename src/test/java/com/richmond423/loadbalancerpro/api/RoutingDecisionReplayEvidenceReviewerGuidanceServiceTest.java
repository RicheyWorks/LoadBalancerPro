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

class RoutingDecisionReplayEvidenceReviewerGuidanceServiceTest {
    private final RoutingDecisionReplayEvidenceReviewerGuidanceService service =
            new RoutingDecisionReplayEvidenceReviewerGuidanceService();

    @Test
    void healthyReviewerGuidanceIsDeterministicAndDerivedFromExistingReviewerMetadata() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceReviewerGuidanceResponse first = service.reviewerGuidance(
                result.decisionReplayEvidenceStatusRollup(),
                result.decisionReplayEvidenceLaneDependencyMap(),
                result.decisionReplayEvidenceLaneReferenceIndex(),
                result.decisionReplayEvidenceLaneDependencySummary(),
                result.decisionReplayEvidenceLaneConsistencySummary(),
                result.decisionReplayEvidenceReviewerSnapshot());
        RoutingDecisionReplayEvidenceReviewerGuidanceResponse second = service.reviewerGuidance(
                result.decisionReplayEvidenceStatusRollup(),
                result.decisionReplayEvidenceLaneDependencyMap(),
                result.decisionReplayEvidenceLaneReferenceIndex(),
                result.decisionReplayEvidenceLaneDependencySummary(),
                result.decisionReplayEvidenceLaneConsistencySummary(),
                result.decisionReplayEvidenceReviewerSnapshot());

        assertEquals(first, second);
        assertEquals(result.decisionReplayEvidenceReviewerGuidance(), first);
        assertTrue(first.readOnly());
        assertEquals("decision-replay-evidence-reviewer-guidance/v1",
                first.reviewerGuidanceSchemaVersion());
        assertEquals("PARTIAL", first.status());
        assertEquals("REVIEW", first.reviewerPriority());
        assertEquals("TAIL_LATENCY_POWER_OF_TWO", first.strategyId());
        assertEquals("edge-alpha", first.selectedCandidateId());
        assertEquals(3, first.candidateCount());
        assertEquals(14, first.totalLaneCount());
        assertEquals(4, first.availableLaneCount());
        assertEquals(10, first.partialLaneCount());
        assertEquals(0, first.unknownLaneCount());
        assertEquals(6, first.checkedSurfaceCount());
        assertEquals(0, first.missingSurfaceCount());
        assertTrue(first.missingSurfaces().isEmpty());
        assertEquals("Review partial or unknown evidence lanes before citing the lab explanation.",
                first.primaryReviewerFocus());
        assertEquals(List.of(
                "Inspect Decision Replay Evidence Reviewer Snapshot warnings and highlights.",
                "Compare lane consistency, dependency summary, and reference index counts.",
                "Review partial or unknown lane statuses before citing reviewer evidence.",
                "Keep limitations with any reviewer-facing explanation."), first.suggestedReviewSteps());
        assertEquals(List.of(
                "decisionReplayEvidenceStatusRollup",
                "decisionReplayEvidenceLaneDependencyMap",
                "decisionReplayEvidenceLaneReferenceIndex",
                "decisionReplayEvidenceLaneDependencySummary",
                "decisionReplayEvidenceLaneConsistencySummary",
                "decisionReplayEvidenceReviewerSnapshot"), first.evidenceSurfacesToInspect());
        assertTrue(first.cautionNotes().contains("Reviewer snapshot status is PARTIAL."));
        assertTrue(first.cautionNotes().contains("10 evidence lanes remain PARTIAL."));
        assertTrue(first.cautionNotes().contains(
                "Guidance is read-only reviewer metadata, not replay proof or scoring proof."));
        assertEquals("Reviewer guidance is PARTIAL with REVIEW priority, 6 checked surfaces, "
                + "0 missing surfaces, and reviewer snapshot status PARTIAL.", first.summaryText());
        assertTrue(first.limitations().contains(
                "Read-only reviewer guidance derived only from existing decision replay reviewer metadata."));
        assertTrue(first.limitations().contains("Not replay proof and does not execute replay."));
        assertTrue(first.limitations().contains("Not scoring proof and does not recompute scores."));
        assertTrue(first.limitations().contains(
                "Not correctness validation, not production readiness, not production certification, and not guaranteed replay."));
        assertTrue(first.boundaryNote().contains("does not inspect raw server input"));
        assertTrue(first.boundaryNote().contains("does not inspect raw request payloads"));
        assertTrue(first.boundaryNote().contains("does not execute replay"));
        assertTrue(first.boundaryNote().contains("does not perform what-if mutation"));
        assertTrue(first.boundaryNote().contains("does not change routing behavior"));
        assertTrue(first.boundaryNote().contains("does not recompute scores"));
        assertFalse(first.toString().contains("reviewerGuidanceFingerprint"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.suggestedReviewSteps().add("other step"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.limitations().add("other limitation"));
    }

    @Test
    void missingSourceSurfacesReturnSafeUnknownWithoutInventedDetails() {
        RoutingDecisionReplayEvidenceReviewerGuidanceResponse guidance =
                service.reviewerGuidance(null, null, null, null, null, null);

        assertEquals("UNKNOWN", guidance.status());
        assertEquals("UNKNOWN", guidance.reviewerPriority());
        assertEquals("UNKNOWN", guidance.strategyId());
        assertNull(guidance.selectedCandidateId());
        assertEquals(0, guidance.candidateCount());
        assertEquals(0, guidance.totalLaneCount());
        assertEquals(0, guidance.availableLaneCount());
        assertEquals(0, guidance.partialLaneCount());
        assertEquals(0, guidance.unknownLaneCount());
        assertEquals(0, guidance.checkedSurfaceCount());
        assertEquals(6, guidance.missingSurfaceCount());
        assertEquals(List.of(
                "decisionReplayEvidenceStatusRollup",
                "decisionReplayEvidenceLaneDependencyMap",
                "decisionReplayEvidenceLaneReferenceIndex",
                "decisionReplayEvidenceLaneDependencySummary",
                "decisionReplayEvidenceLaneConsistencySummary",
                "decisionReplayEvidenceReviewerSnapshot"), guidance.missingSurfaces());
        assertTrue(guidance.evidenceSurfacesToInspect().isEmpty());
        assertEquals(List.of("Run routing comparison before using reviewer guidance."),
                guidance.suggestedReviewSteps());
        assertTrue(guidance.cautionNotes().contains(
                "Missing surface: decisionReplayEvidenceReviewerSnapshot"));
        assertEquals("Decision replay evidence reviewer guidance is UNKNOWN because required reviewer metadata "
                + "is missing or no selected candidate evidence is available.", guidance.summaryText());
        assertFalse(guidance.toString().contains("reviewerGuidanceFingerprint"));
    }

    @Test
    void missingSupportingSurfaceReturnsDeterministicPartialGuidance() {
        RoutingComparisonResultResponse result = healthyCompareResult();

        RoutingDecisionReplayEvidenceReviewerGuidanceResponse guidance = service.reviewerGuidance(
                null,
                result.decisionReplayEvidenceLaneDependencyMap(),
                result.decisionReplayEvidenceLaneReferenceIndex(),
                result.decisionReplayEvidenceLaneDependencySummary(),
                result.decisionReplayEvidenceLaneConsistencySummary(),
                result.decisionReplayEvidenceReviewerSnapshot());

        assertEquals("PARTIAL", guidance.status());
        assertEquals("REVIEW", guidance.reviewerPriority());
        assertEquals(5, guidance.checkedSurfaceCount());
        assertEquals(1, guidance.missingSurfaceCount());
        assertEquals(List.of("decisionReplayEvidenceStatusRollup"), guidance.missingSurfaces());
        assertTrue(guidance.cautionNotes().contains("Missing surface: decisionReplayEvidenceStatusRollup"));
        assertFalse(guidance.evidenceSurfacesToInspect().contains("decisionReplayEvidenceStatusRollup"));
        assertEquals(14, guidance.totalLaneCount());
    }

    @Test
    void noHealthyComparePathKeepsUnknownReviewerGuidanceWithoutInventedDecisionEvidence() {
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

        RoutingDecisionReplayEvidenceReviewerGuidanceResponse guidance =
                result.decisionReplayEvidenceReviewerGuidance();

        assertEquals("UNKNOWN", guidance.status());
        assertEquals("UNKNOWN", guidance.reviewerPriority());
        assertNull(guidance.selectedCandidateId());
        assertEquals(0, guidance.candidateCount());
        assertEquals(0, guidance.totalLaneCount());
        assertEquals(0, guidance.availableLaneCount());
        assertEquals(0, guidance.partialLaneCount());
        assertEquals(0, guidance.unknownLaneCount());
        assertEquals(6, guidance.checkedSurfaceCount());
        assertEquals(0, guidance.missingSurfaceCount());
        assertTrue(guidance.missingSurfaces().isEmpty());
        assertTrue(guidance.cautionNotes().contains(
                "No selected candidate evidence is available for reviewer guidance."));
        assertTrue(result.candidateServersConsidered().isEmpty());
        assertTrue(result.scores().isEmpty());
        String text = guidance.toString().toLowerCase(Locale.ROOT);
        assertFalse(text.contains("reviewerguidancefingerprint"));
        assertFalse(text.contains("candidate set is invented"));
        assertFalse(text.contains("score gap is invented"));
        assertFalse(text.contains("largest delta factor is invented"));
        assertFalse(text.contains("quality ranking is proven"));
        assertFalse(text.contains("approval is granted"));
        assertFalse(text.contains("correctness validation is proven"));
    }

    @Test
    void reviewerGuidanceServiceDoesNotUseRoutingInternalsOrUnsafeCapabilities() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "RoutingDecisionReplayEvidenceReviewerGuidanceService.java"),
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
        assertFalse(normalized.contains("reviewerguidancefingerprint"));
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
