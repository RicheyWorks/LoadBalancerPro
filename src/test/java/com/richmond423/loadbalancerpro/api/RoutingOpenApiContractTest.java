package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RoutingOpenApiContractTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> DELETED_RESULT_FIELDS = List.of(
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
    private static final List<String> DELETED_SCHEMA_FRAGMENTS = List.of(
            "BoundarySummary",
            "FieldInventory",
            "NullSafetySummary",
            "StatusRollup",
            "LaneNavigationSummary",
            "LaneDependencyMap",
            "LaneReferenceIndex",
            "LaneDependencySummary",
            "LaneConsistencySummary",
            "ReviewerSnapshot",
            "ReviewerGuidance",
            "ReviewerHandoffSummary",
            "ReviewerClosureSummary",
            "ReviewerClosureRollup",
            "ReviewerClosureChecklist");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocsExposeRoutingComparePostOperation() throws Exception {
        JsonNode docs = openApi();
        JsonNode operation = docs.at("/paths/~1api~1routing~1compare/post");

        assertTrue(operation.isObject());
        assertTrue(operation.path("requestBody").isObject());
        assertTrue(operation.at("/responses/200").isObject());
        assertEquals(
                "#/components/schemas/RoutingComparisonRequest",
                operation.at("/requestBody/content/application~1json/schema/$ref").asText());
    }

    @Test
    void openApiKeepsCoreExplanationSchemasAndRemovesMetadataSchemas() throws Exception {
        JsonNode docs = openApi();
        JsonNode responseProperties =
                docs.at("/components/schemas/RoutingComparisonResponse/properties");
        JsonNode resultProperties =
                docs.at("/components/schemas/RoutingComparisonResultResponse/properties");
        JsonNode schemas = docs.at("/components/schemas");

        assertTrue(responseProperties.has("requestedStrategies"));
        assertTrue(responseProperties.has("candidateCount"));
        assertTrue(responseProperties.has("timestamp"));
        assertTrue(responseProperties.has("results"));
        assertFalse(responseProperties.has("decisionReplayEvidenceReviewerClosureRollup"));
        assertFalse(responseProperties.has("decisionReplayEvidenceReviewerClosureChecklist"));

        for (String field : List.of(
                "strategyId",
                "status",
                "chosenServerId",
                "reason",
                "candidateServersConsidered",
                "scores",
                "decisionVector",
                "dominantFactorAnalysis",
                "decisionDeltaAnalysis",
                "decisionReplaySnapshot",
                "decisionReplayReconstructionTrace",
                "decisionReplayCapsule",
                "decisionReplayReadinessChecklist",
                "decisionReplayEvidenceSourceMap")) {
            assertTrue(resultProperties.has(field), field);
        }
        DELETED_RESULT_FIELDS.forEach(field -> assertFalse(resultProperties.has(field), field));

        assertTrue(schemas.has("RoutingDecisionVectorResponse"));
        assertTrue(schemas.has("RoutingDecisionReplayEvidenceSourceMapResponse"));
        schemas.fieldNames().forEachRemaining(schemaName ->
                DELETED_SCHEMA_FRAGMENTS.forEach(fragment ->
                        assertFalse(
                                schemaName.startsWith("RoutingDecisionReplayEvidence")
                                        && schemaName.contains(fragment),
                                schemaName)));
    }

    private JsonNode openApi() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return OBJECT_MAPPER.readTree(content);
    }
}
