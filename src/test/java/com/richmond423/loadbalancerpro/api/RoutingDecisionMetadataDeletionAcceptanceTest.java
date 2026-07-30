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
    private static final List<Path> ACTIVE_PRESENTATION_SURFACES = List.of(
            Path.of("README.md"),
            Path.of("docs/API_CONTRACTS.md"),
            Path.of("docs/ENTERPRISE_LAB_COCKPIT_FRAMING.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_VECTOR.md"),
            Path.of("docs/REVIEWER_TRUST_MAP.md"),
            Path.of("docs/adr/ADR-0003_EVIDENCE_AS_FIRST_CLASS_ARTIFACT.md"),
            Path.of("docs/adr/ADR-0006_EVIDENCE_PACKET_AND_REPLAY_BOUNDARY_MODEL.md"),
            Path.of("docs/adr/ADR-0007_REVIEWER_EVIDENCE_AND_TRUST_MODEL.md"),
            Path.of("src/main/resources/static/load-balancing-cockpit.html"),
            Path.of("src/main/resources/static/routing-demo.html"));
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
    private static final List<Path> RETIRED_DOCS = List.of(
            Path.of("docs/ENTERPRISE_LAB_DECISION_EVIDENCE_NULL_SAFETY_SUMMARY.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_EVIDENCE_STATUS_ROLLUP.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_BOUNDARY_SUMMARY.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_CLOSURE_CHECKLIST.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_CLOSURE_ROLLUP.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_FIELD_INVENTORY.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_CONSISTENCY_SUMMARY.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_MAP.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_DEPENDENCY_SUMMARY.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_NAVIGATION_SUMMARY.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_LANE_REFERENCE_INDEX.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_CLOSURE_SUMMARY.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_GUIDANCE.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_HANDOFF_SUMMARY.md"),
            Path.of("docs/ENTERPRISE_LAB_DECISION_REPLAY_EVIDENCE_REVIEWER_SNAPSHOT.md"));
    private static final List<String> RETIRED_SURFACE_TITLES = List.of(
            "Decision Replay Evidence Boundary Summary",
            "Decision Replay Evidence Field Inventory",
            "Decision Evidence Null-Safety Summary",
            "Decision Evidence Status Rollup",
            "Decision Replay Evidence Lane Navigation Summary",
            "Decision Replay Evidence Lane Dependency Map",
            "Decision Replay Evidence Lane Reference Index",
            "Decision Replay Evidence Lane Dependency Summary",
            "Decision Replay Evidence Lane Consistency Summary",
            "Decision Replay Evidence Reviewer Snapshot",
            "Decision Replay Evidence Reviewer Guidance",
            "Decision Replay Evidence Reviewer Handoff Summary",
            "Decision Replay Evidence Reviewer Closure Summary",
            "Decision Replay Evidence Closure Rollup",
            "Decision Replay Evidence Closure Checklist");

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
                () -> assertTrue(response.at("/results/0/decisionVector").isObject()),
                () -> assertTrue(response.at("/results/0/dominantFactorAnalysis").isObject()),
                () -> assertTrue(response.at("/results/0/decisionDeltaAnalysis").isObject()),
                () -> assertTrue(response.at("/results/0/decisionReplayEvidenceSourceMap").isObject()),
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

    @Test
    void retiredDocsAndPresentationReferencesAreAbsent() {
        assertAll(
                () -> RETIRED_DOCS.forEach(path ->
                        assertFalse(Files.exists(path), path + " must be deleted")),
                () -> ACTIVE_PRESENTATION_SURFACES.forEach(path -> {
                    String content = read(path);
                    RESULT_METADATA_FIELDS.forEach(field ->
                            assertFalse(content.contains(field), path + ": " + field));
                    TOP_LEVEL_METADATA_FIELDS.forEach(field ->
                            assertFalse(content.contains(field), path + ": " + field));
                    RETIRED_SURFACE_TITLES.forEach(title ->
                            assertFalse(content.contains(title), path + ": " + title));
                }));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("could not read " + path, exception);
        }
    }
}
