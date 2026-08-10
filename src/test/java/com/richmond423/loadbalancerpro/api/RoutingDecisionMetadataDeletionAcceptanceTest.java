package com.richmond423.loadbalancerpro.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoutingDecisionMetadataDeletionAcceptanceTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path FIXTURE =
            Path.of("src/test/resources/routing-demo/compare-strategies-sample.json");
    private static final Path API_SOURCE =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api");
    private static final int PRE_DELETION_FIXTURE_BYTES = 1_364_797;
    private static final List<String> TOP_LEVEL_METADATA_FIELDS = List.of(
            "decisionReplayEvidenceReviewerClosureRollup",
            "decisionReplayEvidenceReviewerClosureChecklist");
    private static final List<String> RESULT_METADATA_FIELDS = List.of(
            "decisionReplayEvidenceBoundarySummary",
            "decisionReplayEvidenceFieldInventory",
            "decisionReplayEvidenceNullSafetySummary",
            "decisionReplayEvidenceStatusRollup",
            "decisionReplayEvidenceLaneNavigationSummary",
            "decisionReplayEvidenceLaneDependencyMap",
            "decisionReplayEvidenceLaneReferenceIndex",
            "decisionReplayEvidenceLaneDependencySummary",
            "decisionReplayEvidenceLaneConsistencySummary",
            "decisionReplayEvidenceReviewerSnapshot",
            "decisionReplayEvidenceReviewerGuidance",
            "decisionReplayEvidenceReviewerHandoffSummary",
            "decisionReplayEvidenceReviewerClosureSummary");
    private static final List<String> RETIRED_REPLAY_FIELDS = List.of(
            "decisionReplaySnapshot",
            "decisionReplayReconstructionTrace",
            "decisionReplayCapsule",
            "decisionReplayReadinessChecklist",
            "decisionReplayEvidenceSourceMap");
    private static final List<String> RETIRED_SERVICE_TYPES = List.of(
            "RoutingDecisionReplayEvidenceBoundarySummaryService",
            "RoutingDecisionReplayEvidenceFieldInventoryService",
            "RoutingDecisionReplayEvidenceNullSafetySummaryService",
            "RoutingDecisionReplayEvidenceStatusRollupService",
            "RoutingDecisionReplayEvidenceLaneNavigationSummaryService",
            "RoutingDecisionReplayEvidenceLaneDependencyMapService",
            "RoutingDecisionReplayEvidenceLaneReferenceIndexService",
            "RoutingDecisionReplayEvidenceLaneDependencySummaryService",
            "RoutingDecisionReplayEvidenceLaneConsistencySummaryService",
            "RoutingDecisionReplayEvidenceReviewerSnapshotService",
            "RoutingDecisionReplayEvidenceReviewerGuidanceService",
            "RoutingDecisionReplayEvidenceReviewerHandoffSummaryService",
            "RoutingDecisionReplayEvidenceReviewerClosureSummaryService",
            "RoutingDecisionReplayEvidenceReviewerClosureRollupService",
            "RoutingDecisionReplayEvidenceReviewerClosureChecklistService");
    private static final List<String> RETIRED_DTO_TYPES = List.of(
            "DecisionReplayEvidenceBoundarySummaryItemResponse",
            "DecisionReplayEvidenceFieldInventoryEntryResponse",
            "DecisionReplayEvidenceLaneReferenceIndexItemResponse",
            "DecisionReplayEvidenceStatusRollupItemResponse",
            "RoutingDecisionReplayEvidenceBoundarySummaryResponse",
            "RoutingDecisionReplayEvidenceFieldInventoryResponse",
            "RoutingDecisionReplayEvidenceNullSafetySummaryResponse",
            "RoutingDecisionReplayEvidenceStatusRollupResponse",
            "RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse",
            "RoutingDecisionReplayEvidenceLaneDependencyMapResponse",
            "RoutingDecisionReplayEvidenceLaneReferenceIndexResponse",
            "RoutingDecisionReplayEvidenceLaneDependencySummaryResponse",
            "RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse",
            "RoutingDecisionReplayEvidenceReviewerSnapshotResponse",
            "RoutingDecisionReplayEvidenceReviewerGuidanceResponse",
            "RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse",
            "RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse",
            "RoutingDecisionReplayEvidenceReviewerClosureRollupResponse",
            "RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse",
            "RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void compareDeletesMetadataAndShrinksRepresentativePayloadByAtLeastHalf()
            throws Exception {
        String payload = Files.readString(FIXTURE, StandardCharsets.UTF_8);
        String compareContent = mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode response = OBJECT_MAPPER.readTree(compareContent);
        int responseBytes = compareContent.getBytes(StandardCharsets.UTF_8).length;

        assertAll(
                () -> TOP_LEVEL_METADATA_FIELDS.forEach(
                        field -> assertFalse(response.has(field), field + " must be deleted")),
                () -> response.path("results").forEach(result ->
                        RESULT_METADATA_FIELDS.forEach(field ->
                                assertFalse(result.has(field), field + " must be deleted"))),
                () -> response.path("results").forEach(result ->
                        RETIRED_REPLAY_FIELDS.forEach(field ->
                                assertFalse(result.has(field), field + " must be deleted"))),
                () -> assertTrue(response.at("/results/0/decisionVector").isObject()),
                () -> assertTrue(response.at("/results/0/dominantFactorAnalysis").isObject()),
                () -> assertTrue(response.at("/results/0/decisionDeltaAnalysis").isObject()),
                () -> assertTrue(
                        responseBytes <= PRE_DELETION_FIXTURE_BYTES / 2,
                        "representative compare payload must shrink by at least half: "
                                + responseBytes + " bytes"));
    }

    @Test
    void retiredServicesAndDtosHaveNoProductionSourceOrReference() throws Exception {
        List<Path> productionSources;
        try (var paths = Files.walk(API_SOURCE)) {
            productionSources = paths
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList();
        }
        String combinedSource = productionSources.stream()
                .map(RoutingDecisionMetadataDeletionAcceptanceTest::read)
                .reduce("", (left, right) -> left + '\n' + right);

        assertAll(
                () -> RETIRED_SERVICE_TYPES.forEach(type -> assertAll(
                        () -> assertFalse(Files.exists(API_SOURCE.resolve(type + ".java"))),
                        () -> assertFalse(combinedSource.contains(type),
                                type + " must have no production reference"))),
                () -> RETIRED_DTO_TYPES.forEach(type -> assertAll(
                        () -> assertFalse(Files.exists(API_SOURCE.resolve(type + ".java"))),
                        () -> assertFalse(combinedSource.contains(type),
                                type + " must have no production reference"))));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("could not read " + path, exception);
        }
    }
}
