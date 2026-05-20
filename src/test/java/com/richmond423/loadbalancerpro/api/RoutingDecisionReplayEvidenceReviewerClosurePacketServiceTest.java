package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RoutingDecisionReplayEvidenceReviewerClosurePacketServiceTest {
    private final RoutingDecisionReplayEvidenceReviewerClosureChecklistService checklistService =
            new RoutingDecisionReplayEvidenceReviewerClosureChecklistService();
    private final RoutingDecisionReplayEvidenceReviewerClosurePacketService service =
            new RoutingDecisionReplayEvidenceReviewerClosurePacketService();

    @Test
    void normalCompareResponseIncludesDeterministicReviewerClosurePacket() {
        RoutingComparisonResponse response = healthyCompareResponse();
        RoutingDecisionReplayEvidenceReviewerClosurePacketResponse first = service.packet(
                response.results(),
                response.decisionReplayEvidenceReviewerClosureRollup(),
                response.decisionReplayEvidenceReviewerClosureChecklist());
        RoutingDecisionReplayEvidenceReviewerClosurePacketResponse second = service.packet(
                response.results(),
                response.decisionReplayEvidenceReviewerClosureRollup(),
                response.decisionReplayEvidenceReviewerClosureChecklist());

        assertEquals(first, second);
        assertEquals(response.decisionReplayEvidenceReviewerClosurePacket(), first);
        assertEquals("COMPLETE", first.status());
        assertTrue(first.reviewerReady());
        assertEquals("v1", first.packetVersion());
        assertEquals(5, first.sections().size());
        assertSection(first, "closureSummary", "PASS");
        assertSection(first, "closureRollup", "PASS");
        assertSection(first, "closureChecklist", "PASS");
        assertSection(first, "scenarioReplayBoundary", "PASS");
        assertSection(first, "notProvenBoundaries", "PASS");
        assertEquals(List.of(
                "not replay proof",
                "not scoring proof",
                "not correctness validation",
                "not production readiness",
                "not production certification",
                "not guaranteed replay",
                "not production validation"), first.notProvenBoundaries());
        assertTrue(first.summary().contains("closureSummary=PASS"));
        assertTrue(first.summary().contains("not an export/share/download packet"));
        assertTrue(first.summary().contains("not replay proof"));
        assertTrue(first.reviewerGuidance().get(0).contains("in-response reviewer index"));
        assertTrue(first.reviewerGuidance().get(2).contains("not production validation"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.sections().add(new RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse(
                        "other", "PASS", "other")));
        assertThrows(UnsupportedOperationException.class,
                () -> first.reviewerGuidance().add("other guidance"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.notProvenBoundaries().add("other boundary"));
    }

    @Test
    void noHealthyComparePathReturnsUnknownPacketWithoutInventedProof() {
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

        RoutingDecisionReplayEvidenceReviewerClosurePacketResponse packet =
                response.decisionReplayEvidenceReviewerClosurePacket();

        assertEquals("UNKNOWN", packet.status());
        assertFalse(packet.reviewerReady());
        assertEquals("v1", packet.packetVersion());
        assertSection(packet, "closureSummary", "PASS");
        assertSection(packet, "closureRollup", "UNKNOWN");
        assertSection(packet, "closureChecklist", "UNKNOWN");
        assertSection(packet, "scenarioReplayBoundary", "PASS");
        assertSection(packet, "notProvenBoundaries", "PASS");
        assertTrue(packet.notProvenBoundaries().contains("not replay proof"));
        assertTrue(packet.notProvenBoundaries().contains("not production validation"));
        assertFalse(packet.toString().contains("reviewerClosurePacketFingerprint"));
        assertFalse(packet.toString().contains("production certification is proven"));
        assertFalse(packet.toString().contains("guaranteed replay is proven"));
        assertFalse(packet.toString().contains("correctness validation is proven"));
    }

    @Test
    void strippedOrMissingClosureSummaryWarnsWhenPacketSourcesNoLongerAlign() {
        RoutingComparisonResponse response = healthyCompareResponse();
        RoutingComparisonResultResponse strippedResult = withoutReviewerClosureSummary(response.results().get(0));
        RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse checklist = checklistService.checklist(
                List.of(strippedResult),
                response.decisionReplayEvidenceReviewerClosureRollup());

        RoutingDecisionReplayEvidenceReviewerClosurePacketResponse packet = service.packet(
                List.of(strippedResult),
                response.decisionReplayEvidenceReviewerClosureRollup(),
                checklist);

        assertEquals("PARTIAL", packet.status());
        assertFalse(packet.reviewerReady());
        assertSection(packet, "closureSummary", "WARN");
        assertSection(packet, "closureRollup", "PASS");
        assertSection(packet, "closureChecklist", "WARN");
        assertSection(packet, "scenarioReplayBoundary", "PASS");
        assertSection(packet, "notProvenBoundaries", "PASS");
        assertTrue(packet.summary().contains("closureSummary=WARN"));
    }

    @Test
    void reviewerClosurePacketResponseDoesNotExposeUnsafeOrProofClaims() throws Exception {
        String json = new ObjectMapper().writeValueAsString(healthyCompareResponse()
                .decisionReplayEvidenceReviewerClosurePacket());
        String normalized = json.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("decision replay evidence reviewer closure packet"));
        assertTrue(json.contains("not an export/share/download packet"));
        assertTrue(json.contains("not replay proof"));
        assertTrue(json.contains("not scoring proof"));
        assertTrue(json.contains("not production validation"));
        assertFalse(json.contains("reviewerClosurePacketFingerprint"));
        assertFalse(json.contains("reviewerClosurePacketHash"));
        assertFalse(json.contains("production certification is proven"));
        assertFalse(json.contains("guaranteed replay is proven"));
        assertFalse(json.contains("correctness validation is proven"));
    }

    private static void assertSection(
            RoutingDecisionReplayEvidenceReviewerClosurePacketResponse packet,
            String name,
            String status) {
        RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse section = packet.sections().stream()
                .filter(candidate -> name.equals(candidate.name()))
                .findFirst()
                .orElseThrow();
        assertEquals(status, section.status());
        assertFalse(section.description().isBlank());
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
