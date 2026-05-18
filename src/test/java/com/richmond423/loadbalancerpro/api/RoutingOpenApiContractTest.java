package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocsExposeRoutingComparePostOperation() throws Exception {
        JsonNode docs = openApiDocs();
        JsonNode operation = required(docs, "/paths/~1api~1routing~1compare/post");

        JsonNode requestContent = required(operation, "/requestBody/content");
        assertFalse(requestContent.path("application/json").isMissingNode(),
                "routing comparison should declare an application/json request body");
        assertRef(requestContent.at("/application~1json/schema"), "#/components/schemas/RoutingComparisonRequest");

        JsonNode response = required(operation, "/responses/200");
        assertFalse(response.isMissingNode(), "routing comparison should declare a 200 response");
    }

    @Test
    void openApiDocsCharacterizeInferredRoutingCompareSchemas() throws Exception {
        JsonNode docs = openApiDocs();

        JsonNode requestProperties = required(docs,
                "/components/schemas/RoutingComparisonRequest/properties");
        assertEquals("array", required(requestProperties, "/servers/type").asText());
        assertRef(required(requestProperties, "/servers/items"), "#/components/schemas/RoutingServerStateInput");
        assertEquals("array", required(requestProperties, "/strategies/type").asText());
        assertEquals("string", required(requestProperties, "/strategies/items/type").asText());
        assertTrue(requestProperties.at("/strategies/items/enum").isMissingNode(),
                "strategies is currently inferred as strings, not a curated OpenAPI enum");

        JsonNode responseProperties = required(docs,
                "/components/schemas/RoutingComparisonResponse/properties");
        assertEquals("array", required(responseProperties, "/requestedStrategies/type").asText());
        assertEquals("string", required(responseProperties, "/requestedStrategies/items/type").asText());
        assertEquals("integer", required(responseProperties, "/candidateCount/type").asText());
        assertEquals("array", required(responseProperties, "/results/type").asText());
        assertRef(required(responseProperties, "/results/items"),
                "#/components/schemas/RoutingComparisonResultResponse");

        JsonNode resultProperties = required(docs,
                "/components/schemas/RoutingComparisonResultResponse/properties");
        assertEquals("string", required(resultProperties, "/strategyId/type").asText());
        assertEquals("string", required(resultProperties, "/status/type").asText());
        assertEquals("string", required(resultProperties, "/chosenServerId/type").asText());
        assertEquals("string", required(resultProperties, "/reason/type").asText());
        assertEquals("array", required(resultProperties, "/candidateServersConsidered/type").asText());
        assertEquals("object", required(resultProperties, "/scores/type").asText());
        assertRef(required(resultProperties, "/decisionVector"), "#/components/schemas/RoutingDecisionVectorResponse");
        assertRef(required(resultProperties, "/dominantFactorAnalysis"),
                "#/components/schemas/DominantFactorAnalysisResponse");
        assertRef(required(resultProperties, "/decisionDeltaAnalysis"),
                "#/components/schemas/RoutingDecisionDeltaAnalysisResponse");

        JsonNode vectorProperties = required(docs,
                "/components/schemas/RoutingDecisionVectorResponse/properties");
        assertEquals("boolean", required(vectorProperties, "/readOnly/type").asText());
        assertEquals("string", required(vectorProperties, "/localLabResponsePath/type").asText());
        assertEquals("string", required(vectorProperties, "/selectedStrategy/type").asText());
        assertEquals("string", required(vectorProperties, "/selectedBackend/type").asText());
        assertEquals("array", required(vectorProperties, "/candidateSummaries/type").asText());
        assertRef(required(vectorProperties, "/candidateSummaries/items"),
                "#/components/schemas/CandidateDecisionVectorResponse");
        assertRef(required(vectorProperties, "/selectedCandidateVector"),
                "#/components/schemas/CandidateDecisionVectorResponse");

        JsonNode candidateProperties = required(docs,
                "/components/schemas/CandidateDecisionVectorResponse/properties");
        assertEquals("string", required(candidateProperties, "/candidateId/type").asText());
        assertEquals("boolean", required(candidateProperties, "/selected/type").asText());
        assertEquals("array", required(candidateProperties, "/factorContributions/type").asText());
        assertRef(required(candidateProperties, "/factorContributions/items"),
                "#/components/schemas/ScoreFactorContributionResponse");

        JsonNode dominantProperties = required(docs,
                "/components/schemas/DominantFactorAnalysisResponse/properties");
        assertEquals("boolean", required(dominantProperties, "/readOnly/type").asText());
        assertEquals("array", required(dominantProperties, "/candidateAnalyses/type").asText());
        assertRef(required(dominantProperties, "/candidateAnalyses/items"),
                "#/components/schemas/CandidateDominantFactorResponse");
        assertRef(required(dominantProperties, "/selectedDecisionAnalysis"),
                "#/components/schemas/CandidateDominantFactorResponse");

        JsonNode candidateDominantProperties = required(docs,
                "/components/schemas/CandidateDominantFactorResponse/properties");
        assertEquals("string", required(candidateDominantProperties, "/candidateId/type").asText());
        assertEquals("boolean", required(candidateDominantProperties, "/available/type").asText());
        assertRef(required(candidateDominantProperties, "/largestAbsoluteImpact"),
                "#/components/schemas/DominantFactorResponse");

        JsonNode factorProperties = required(docs,
                "/components/schemas/DominantFactorResponse/properties");
        assertEquals("string", required(factorProperties, "/factorName/type").asText());
        assertEquals("number", required(factorProperties, "/absoluteImpact/type").asText());

        JsonNode deltaProperties = required(docs,
                "/components/schemas/RoutingDecisionDeltaAnalysisResponse/properties");
        assertEquals("boolean", required(deltaProperties, "/readOnly/type").asText());
        assertRef(required(deltaProperties, "/comparison"),
                "#/components/schemas/CandidateDecisionDeltaResponse");
        assertEquals("array", required(deltaProperties, "/factorDeltas/type").asText());
        assertRef(required(deltaProperties, "/factorDeltas/items"),
                "#/components/schemas/ScoreFactorDeltaResponse");
        assertRef(required(deltaProperties, "/largestAbsoluteFactorDelta"),
                "#/components/schemas/ScoreFactorDeltaResponse");

        JsonNode candidateDeltaProperties = required(docs,
                "/components/schemas/CandidateDecisionDeltaResponse/properties");
        assertEquals("string", required(candidateDeltaProperties, "/selectedCandidateId/type").asText());
        assertEquals("string", required(candidateDeltaProperties, "/closestAlternativeCandidateId/type").asText());
        assertEquals("number", required(candidateDeltaProperties, "/finalScoreGap/type").asText());
        assertEquals("array", required(candidateDeltaProperties, "/comparedFactorNames/type").asText());

        JsonNode factorDeltaProperties = required(docs,
                "/components/schemas/ScoreFactorDeltaResponse/properties");
        assertEquals("string", required(factorDeltaProperties, "/factorName/type").asText());
        assertEquals("number", required(factorDeltaProperties, "/contributionDelta/type").asText());
        assertEquals("number", required(factorDeltaProperties, "/absoluteDelta/type").asText());
    }

    private JsonNode openApiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return OBJECT_MAPPER.readTree(body);
    }

    private static JsonNode required(JsonNode node, String pointer) {
        JsonNode value = node.at(pointer);
        assertFalse(value.isMissingNode(), () -> "Expected OpenAPI node at " + pointer);
        return value;
    }

    private static void assertRef(JsonNode schema, String expectedRef) {
        assertEquals(expectedRef, required(schema, "/$ref").asText());
    }
}
