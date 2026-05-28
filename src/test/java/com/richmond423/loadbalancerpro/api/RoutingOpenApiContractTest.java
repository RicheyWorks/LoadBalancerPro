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

        JsonNode explorerOperation = required(docs, "/paths/~1api~1routing~1decision-explorer/post");
        JsonNode explorerRequestContent = required(explorerOperation, "/requestBody/content");
        assertFalse(explorerRequestContent.path("application/json").isMissingNode(),
                "Decision Explorer should declare an application/json request body");
        assertRef(explorerRequestContent.at("/application~1json/schema"),
                "#/components/schemas/RoutingComparisonRequest");

        JsonNode explorerResponseSchema = required(explorerOperation,
                "/responses/200/content/*~1*/schema");
        assertEquals("array", required(explorerResponseSchema, "/type").asText());
        assertRef(required(explorerResponseSchema, "/items"), "#/components/schemas/DecisionExplorerPayloadV1");

        JsonNode scenarioCatalogOperation = required(docs,
                "/paths/~1api~1routing~1decision-explorer~1scenarios/get");
        JsonNode scenarioCatalogResponseSchema = required(scenarioCatalogOperation,
                "/responses/200/content/*~1*/schema");
        assertRef(scenarioCatalogResponseSchema, "#/components/schemas/DecisionExplorerScenarioCatalogV1");
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
        assertRef(required(responseProperties, "/decisionReplayEvidenceReviewerClosureRollup"),
                "#/components/schemas/RoutingDecisionReplayEvidenceReviewerClosureRollupResponse");
        assertRef(required(responseProperties, "/decisionReplayEvidenceReviewerClosureChecklist"),
                "#/components/schemas/RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse");
        assertEquals("array", required(responseProperties, "/results/type").asText());
        assertRef(required(responseProperties, "/results/items"),
                "#/components/schemas/RoutingComparisonResultResponse");

        JsonNode explorerProperties = required(docs,
                "/components/schemas/DecisionExplorerPayloadV1/properties");
        assertEquals("boolean", required(explorerProperties, "/readOnly/type").asText());
        assertEquals("boolean", required(explorerProperties, "/simulationOnly/type").asText());
        assertEquals("string", required(explorerProperties, "/payloadObject/type").asText());
        assertEquals("string", required(explorerProperties, "/contractVersion/type").asText());
        assertEquals("string", required(explorerProperties, "/source/type").asText());
        assertEquals("string", required(explorerProperties, "/decisionId/type").asText());
        assertRef(required(explorerProperties, "/decisionReadout"), "#/components/schemas/DecisionReadoutV1");
        assertRef(required(explorerProperties, "/selectedCandidate"), "#/components/schemas/CandidateReadoutV1");
        assertArrayRef(required(explorerProperties, "/candidateSet"), "#/components/schemas/CandidateReadoutV1");
        assertArrayRef(required(explorerProperties, "/candidateComparisons"),
                "#/components/schemas/DecisionExplorerCandidateComparisonRowV1");
        assertRef(required(explorerProperties, "/confidenceSummary"),
                "#/components/schemas/DecisionExplorerConfidenceSummaryV1");
        assertArrayRef(required(explorerProperties, "/factorContributions"),
                "#/components/schemas/FactorContributionV1");
        assertArrayRef(required(explorerProperties, "/factorDrilldowns"),
                "#/components/schemas/DecisionFactorDrilldownV1");
        assertArrayRef(required(explorerProperties, "/policyGateReadouts"),
                "#/components/schemas/PolicyGateReadoutV1");
        assertArrayRef(required(explorerProperties, "/decisionDiffReadouts"),
                "#/components/schemas/DecisionDiffReadoutV1");
        assertArrayRef(required(explorerProperties, "/evidencePacketReadouts"),
                "#/components/schemas/EvidencePacketReadoutV1");
        assertRef(required(explorerProperties, "/agentStructuredOutput"),
                "#/components/schemas/AgentStructuredOutputV1");
        assertEquals("array", required(explorerProperties, "/warnings/type").asText());
        assertEquals("string", required(explorerProperties, "/warnings/items/type").asText());
        assertEquals("array", required(explorerProperties, "/unknowns/type").asText());
        assertEquals("string", required(explorerProperties, "/unknowns/items/type").asText());
        assertEquals("array", required(explorerProperties, "/notProvenBoundaries/type").asText());
        assertEquals("string", required(explorerProperties, "/notProvenBoundaries/items/type").asText());
        assertEquals("string", required(explorerProperties, "/boundaryNote/type").asText());

        JsonNode scenarioCatalogProperties = required(docs,
                "/components/schemas/DecisionExplorerScenarioCatalogV1/properties");
        assertEquals("boolean", required(scenarioCatalogProperties, "/readOnly/type").asText());
        assertEquals("boolean", required(scenarioCatalogProperties, "/simulationOnly/type").asText());
        assertEquals("string", required(scenarioCatalogProperties, "/payloadObject/type").asText());
        assertEquals("string", required(scenarioCatalogProperties, "/contractVersion/type").asText());
        assertEquals("string", required(scenarioCatalogProperties, "/source/type").asText());
        assertArrayRef(required(scenarioCatalogProperties, "/scenarios"),
                "#/components/schemas/DecisionExplorerScenarioV1");
        assertEquals("array", required(scenarioCatalogProperties, "/warnings/type").asText());
        assertEquals("array", required(scenarioCatalogProperties, "/unknowns/type").asText());
        assertEquals("array", required(scenarioCatalogProperties, "/notProvenBoundaries/type").asText());
        assertEquals("string", required(scenarioCatalogProperties, "/boundaryNote/type").asText());

        JsonNode scenarioProperties = required(docs,
                "/components/schemas/DecisionExplorerScenarioV1/properties");
        assertEquals("string", required(scenarioProperties, "/scenarioObject/type").asText());
        assertEquals("string", required(scenarioProperties, "/scenarioId/type").asText());
        assertEquals("string", required(scenarioProperties, "/scenarioLabel/type").asText());
        assertEquals("string", required(scenarioProperties, "/scenarioCategory/type").asText());
        assertEquals("string", required(scenarioProperties, "/evidenceStatus/type").asText());
        assertEquals("integer", required(scenarioProperties, "/displayOrder/type").asText());
        assertEquals("string", required(scenarioProperties, "/description/type").asText());
        assertEquals("string", required(scenarioProperties, "/requestPresetId/type").asText());
        assertEquals("array", required(scenarioProperties, "/sourceReferenceIds/type").asText());
        assertEquals("array", required(scenarioProperties, "/expectedReviewerQuestions/type").asText());
        assertEquals("array", required(scenarioProperties, "/tags/type").asText());
        assertEquals("array", required(scenarioProperties, "/warnings/type").asText());
        assertEquals("array", required(scenarioProperties, "/unknowns/type").asText());
        assertEquals("array", required(scenarioProperties, "/notProvenBoundaries/type").asText());
        assertEquals("string", required(scenarioProperties, "/boundaryNote/type").asText());

        JsonNode decisionReadoutProperties = required(docs,
                "/components/schemas/DecisionReadoutV1/properties");
        assertEquals("string", required(decisionReadoutProperties, "/decisionId/type").asText());
        assertEquals("string", required(decisionReadoutProperties, "/status/type").asText());
        assertEquals("string", required(decisionReadoutProperties, "/selectedCandidateId/type").asText());
        assertEquals("string", required(decisionReadoutProperties, "/selectedStrategy/type").asText());
        assertEquals("string", required(decisionReadoutProperties, "/summary/type").asText());
        assertEquals("array", required(decisionReadoutProperties, "/reasonCodes/type").asText());
        assertEquals("array", required(decisionReadoutProperties, "/sourceReferenceIds/type").asText());

        JsonNode candidateReadoutProperties = required(docs,
                "/components/schemas/CandidateReadoutV1/properties");
        assertEquals("string", required(candidateReadoutProperties, "/candidateId/type").asText());
        assertEquals("string", required(candidateReadoutProperties, "/candidateLabel/type").asText());
        assertEquals("boolean", required(candidateReadoutProperties, "/selected/type").asText());
        assertEquals("string", required(candidateReadoutProperties, "/candidateStatus/type").asText());
        assertEquals("number", required(candidateReadoutProperties, "/finalScore/type").asText());
        assertEquals("array", required(candidateReadoutProperties, "/visibleSignals/type").asText());
        assertEquals("array", required(candidateReadoutProperties, "/unknownSignals/type").asText());
        assertEquals("array", required(candidateReadoutProperties, "/policyGateIds/type").asText());
        assertEquals("array", required(candidateReadoutProperties, "/evidenceReferenceIds/type").asText());

        JsonNode candidateComparisonProperties = required(docs,
                "/components/schemas/DecisionExplorerCandidateComparisonRowV1/properties");
        assertEquals("string", required(candidateComparisonProperties, "/candidateId/type").asText());
        assertEquals("string", required(candidateComparisonProperties, "/candidateLabel/type").asText());
        assertEquals("boolean", required(candidateComparisonProperties, "/selected/type").asText());
        assertEquals("integer", required(candidateComparisonProperties, "/displayOrder/type").asText());
        assertEquals("string", required(candidateComparisonProperties, "/comparisonStatus/type").asText());
        assertEquals("number", required(candidateComparisonProperties, "/finalScore/type").asText());
        assertEquals("number", required(candidateComparisonProperties, "/scoreDeltaFromSelected/type").asText());
        assertEquals("array", required(candidateComparisonProperties, "/visibleSignals/type").asText());
        assertEquals("array", required(candidateComparisonProperties, "/unknownSignals/type").asText());
        assertEquals("array", required(candidateComparisonProperties, "/reasonCodes/type").asText());
        assertEquals("array", required(candidateComparisonProperties, "/policyGateIds/type").asText());
        assertEquals("array", required(candidateComparisonProperties, "/evidenceReferenceIds/type").asText());
        assertEquals("array", required(candidateComparisonProperties, "/warnings/type").asText());
        assertEquals("array", required(candidateComparisonProperties, "/unknowns/type").asText());
        assertEquals("string", required(candidateComparisonProperties, "/boundaryNote/type").asText());

        JsonNode confidenceSummaryProperties = required(docs,
                "/components/schemas/DecisionExplorerConfidenceSummaryV1/properties");
        assertEquals("boolean", required(confidenceSummaryProperties, "/readOnly/type").asText());
        assertEquals("boolean", required(confidenceSummaryProperties, "/simulationOnly/type").asText());
        assertEquals("string", required(confidenceSummaryProperties, "/summaryObject/type").asText());
        assertEquals("string", required(confidenceSummaryProperties, "/contractVersion/type").asText());
        assertEquals("string", required(confidenceSummaryProperties, "/status/type").asText());
        assertEquals("string", required(confidenceSummaryProperties, "/evidenceQuality/type").asText());
        assertEquals("string", required(confidenceSummaryProperties, "/selectedCandidateId/type").asText());
        assertEquals("integer", required(confidenceSummaryProperties, "/candidateCount/type").asText());
        assertEquals("integer", required(confidenceSummaryProperties, "/candidateComparisonCount/type").asText());
        assertEquals("integer", required(confidenceSummaryProperties, "/availableFactorCount/type").asText());
        assertEquals("integer", required(confidenceSummaryProperties, "/partialFactorCount/type").asText());
        assertEquals("integer", required(confidenceSummaryProperties, "/unknownFactorCount/type").asText());
        assertEquals("integer", required(confidenceSummaryProperties, "/warningCount/type").asText());
        assertEquals("integer", required(confidenceSummaryProperties, "/unknownCount/type").asText());
        assertEquals("integer", required(confidenceSummaryProperties, "/sourceReferenceCount/type").asText());
        assertArrayRef(required(confidenceSummaryProperties, "/candidateConfidenceDetails"),
                "#/components/schemas/DecisionExplorerCandidateConfidenceV1");
        assertEquals("array", required(confidenceSummaryProperties, "/evidenceSignals/type").asText());
        assertEquals("string", required(confidenceSummaryProperties, "/evidenceSignals/items/type").asText());
        assertEquals("array", required(confidenceSummaryProperties, "/statusReasons/type").asText());
        assertEquals("string", required(confidenceSummaryProperties, "/statusReasons/items/type").asText());
        assertEquals("array", required(confidenceSummaryProperties, "/warnings/type").asText());
        assertEquals("string", required(confidenceSummaryProperties, "/warnings/items/type").asText());
        assertEquals("array", required(confidenceSummaryProperties, "/unknowns/type").asText());
        assertEquals("string", required(confidenceSummaryProperties, "/unknowns/items/type").asText());
        assertEquals("array", required(confidenceSummaryProperties, "/sourceReferenceIds/type").asText());
        assertEquals("string", required(confidenceSummaryProperties, "/sourceReferenceIds/items/type").asText());
        assertEquals("string", required(confidenceSummaryProperties, "/boundaryNote/type").asText());

        JsonNode candidateConfidenceProperties = required(docs,
                "/components/schemas/DecisionExplorerCandidateConfidenceV1/properties");
        assertEquals("string", required(candidateConfidenceProperties, "/candidateId/type").asText());
        assertEquals("string", required(candidateConfidenceProperties, "/candidateLabel/type").asText());
        assertEquals("boolean", required(candidateConfidenceProperties, "/selected/type").asText());
        assertEquals("integer", required(candidateConfidenceProperties, "/displayOrder/type").asText());
        assertEquals("string", required(candidateConfidenceProperties, "/confidenceStatus/type").asText());
        assertEquals("string", required(candidateConfidenceProperties, "/healthEvidenceState/type").asText());
        assertEquals("string", required(candidateConfidenceProperties, "/comparisonStatus/type").asText());
        assertEquals("number", required(candidateConfidenceProperties, "/finalScore/type").asText());
        assertEquals("number", required(candidateConfidenceProperties, "/scoreDeltaFromSelected/type").asText());
        assertEquals("integer", required(candidateConfidenceProperties, "/visibleSignalCount/type").asText());
        assertEquals("integer", required(candidateConfidenceProperties, "/unknownSignalCount/type").asText());
        assertEquals("integer", required(candidateConfidenceProperties, "/availableFactorCount/type").asText());
        assertEquals("integer", required(candidateConfidenceProperties, "/partialFactorCount/type").asText());
        assertEquals("integer", required(candidateConfidenceProperties, "/unknownFactorCount/type").asText());
        assertEquals("array", required(candidateConfidenceProperties, "/confidenceReasons/type").asText());
        assertEquals("string", required(candidateConfidenceProperties, "/confidenceReasons/items/type").asText());
        assertEquals("array", required(candidateConfidenceProperties, "/warnings/type").asText());
        assertEquals("string", required(candidateConfidenceProperties, "/warnings/items/type").asText());
        assertEquals("array", required(candidateConfidenceProperties, "/unknowns/type").asText());
        assertEquals("string", required(candidateConfidenceProperties, "/unknowns/items/type").asText());
        assertEquals("array", required(candidateConfidenceProperties, "/sourceReferenceIds/type").asText());
        assertEquals("string", required(candidateConfidenceProperties, "/sourceReferenceIds/items/type").asText());
        assertEquals("string", required(candidateConfidenceProperties, "/boundaryNote/type").asText());

        JsonNode factorReadoutProperties = required(docs,
                "/components/schemas/FactorContributionV1/properties");
        assertEquals("string", required(factorReadoutProperties, "/factorName/type").asText());
        assertEquals("string", required(factorReadoutProperties, "/candidateId/type").asText());
        assertEquals("string", required(factorReadoutProperties, "/direction/type").asText());
        assertEquals("number", required(factorReadoutProperties, "/contributionValue/type").asText());
        assertEquals("string", required(factorReadoutProperties, "/exactness/type").asText());
        assertEquals("string", required(factorReadoutProperties, "/explanation/type").asText());

        JsonNode factorDrilldownProperties = required(docs,
                "/components/schemas/DecisionFactorDrilldownV1/properties");
        assertEquals("string", required(factorDrilldownProperties, "/factorName/type").asText());
        assertEquals("string", required(factorDrilldownProperties, "/candidateId/type").asText());
        assertEquals("string", required(factorDrilldownProperties, "/observedValueOrStatus/type").asText());
        assertEquals("string", required(factorDrilldownProperties, "/influenceCategory/type").asText());
        assertEquals("string", required(factorDrilldownProperties, "/evidenceStatus/type").asText());
        assertEquals("string", required(factorDrilldownProperties, "/explanation/type").asText());
        assertEquals("array", required(factorDrilldownProperties, "/warnings/type").asText());
        assertEquals("array", required(factorDrilldownProperties, "/unknowns/type").asText());
        assertEquals("array", required(factorDrilldownProperties, "/sourceReferenceIds/type").asText());
        assertEquals("string", required(factorDrilldownProperties, "/boundaryNote/type").asText());

        JsonNode policyGateProperties = required(docs,
                "/components/schemas/PolicyGateReadoutV1/properties");
        assertEquals("string", required(policyGateProperties, "/gateId/type").asText());
        assertEquals("string", required(policyGateProperties, "/gateName/type").asText());
        assertEquals("string", required(policyGateProperties, "/gateStatus/type").asText());
        assertEquals("string", required(policyGateProperties, "/outcome/type").asText());

        JsonNode diffReadoutProperties = required(docs,
                "/components/schemas/DecisionDiffReadoutV1/properties");
        assertEquals("string", required(diffReadoutProperties, "/baselineCandidateId/type").asText());
        assertEquals("string", required(diffReadoutProperties, "/comparisonCandidateId/type").asText());
        assertEquals("string", required(diffReadoutProperties, "/diffStatus/type").asText());
        assertEquals("number", required(diffReadoutProperties, "/finalScoreGap/type").asText());
        assertEquals("array", required(diffReadoutProperties, "/comparedFactorNames/type").asText());
        assertEquals("array", required(diffReadoutProperties, "/omittedFactorNames/type").asText());

        JsonNode evidencePacketProperties = required(docs,
                "/components/schemas/EvidencePacketReadoutV1/properties");
        assertEquals("string", required(evidencePacketProperties, "/referenceId/type").asText());
        assertEquals("string", required(evidencePacketProperties, "/packetStatus/type").asText());
        assertEquals("string", required(evidencePacketProperties, "/sourceReferenceId/type").asText());
        assertEquals("string", required(evidencePacketProperties, "/freshnessStatus/type").asText());
        assertEquals("array", required(evidencePacketProperties, "/unavailableReasons/type").asText());

        JsonNode agentOutputProperties = required(docs,
                "/components/schemas/AgentStructuredOutputV1/properties");
        assertEquals("string", required(agentOutputProperties, "/schemaName/type").asText());
        assertEquals("string", required(agentOutputProperties, "/schemaVersion/type").asText());
        assertEquals("array", required(agentOutputProperties, "/stableFieldNames/type").asText());
        assertEquals("array", required(agentOutputProperties, "/parseabilityRules/type").asText());
        assertEquals("array", required(agentOutputProperties, "/supportedQuestions/type").asText());
        assertEquals("array", required(agentOutputProperties, "/unsupportedActions/type").asText());

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
        assertRef(required(resultProperties, "/decisionReplaySnapshot"),
                "#/components/schemas/RoutingDecisionReplaySnapshotResponse");
        assertRef(required(resultProperties, "/decisionReplayReconstructionTrace"),
                "#/components/schemas/RoutingDecisionReplayReconstructionTraceResponse");
        assertRef(required(resultProperties, "/decisionReplayCapsule"),
                "#/components/schemas/RoutingDecisionReplayCapsuleResponse");
        assertRef(required(resultProperties, "/decisionReplayReadinessChecklist"),
                "#/components/schemas/RoutingDecisionReplayReadinessChecklistResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceSourceMap"),
                "#/components/schemas/RoutingDecisionReplayEvidenceSourceMapResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceBoundarySummary"),
                "#/components/schemas/RoutingDecisionReplayEvidenceBoundarySummaryResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceFieldInventory"),
                "#/components/schemas/RoutingDecisionReplayEvidenceFieldInventoryResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceNullSafetySummary"),
                "#/components/schemas/RoutingDecisionReplayEvidenceNullSafetySummaryResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceStatusRollup"),
                "#/components/schemas/RoutingDecisionReplayEvidenceStatusRollupResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceLaneNavigationSummary"),
                "#/components/schemas/RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceLaneDependencyMap"),
                "#/components/schemas/RoutingDecisionReplayEvidenceLaneDependencyMapResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceLaneReferenceIndex"),
                "#/components/schemas/RoutingDecisionReplayEvidenceLaneReferenceIndexResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceLaneDependencySummary"),
                "#/components/schemas/RoutingDecisionReplayEvidenceLaneDependencySummaryResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceLaneConsistencySummary"),
                "#/components/schemas/RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceReviewerSnapshot"),
                "#/components/schemas/RoutingDecisionReplayEvidenceReviewerSnapshotResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceReviewerGuidance"),
                "#/components/schemas/RoutingDecisionReplayEvidenceReviewerGuidanceResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceReviewerHandoffSummary"),
                "#/components/schemas/RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse");
        assertRef(required(resultProperties, "/decisionReplayEvidenceReviewerClosureSummary"),
                "#/components/schemas/RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse");

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

        JsonNode snapshotProperties = required(docs,
                "/components/schemas/RoutingDecisionReplaySnapshotResponse/properties");
        assertEquals("boolean", required(snapshotProperties, "/readOnly/type").asText());
        assertEquals("string", required(snapshotProperties, "/snapshotSchemaVersion/type").asText());
        assertEquals("string", required(snapshotProperties, "/snapshotFingerprint/type").asText());
        assertEquals("array", required(snapshotProperties, "/candidateIdsConsidered/type").asText());
        assertEquals("integer", required(snapshotProperties, "/candidateCount/type").asText());
        assertEquals("string", required(snapshotProperties, "/decisionVectorStatus/type").asText());
        assertEquals("string", required(snapshotProperties, "/decisionDeltaAnalysisStatus/type").asText());
        assertEquals("number", required(snapshotProperties, "/finalScoreGap/type").asText());

        JsonNode traceProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayReconstructionTraceResponse/properties");
        assertEquals("boolean", required(traceProperties, "/readOnly/type").asText());
        assertEquals("string", required(traceProperties, "/traceSchemaVersion/type").asText());
        assertEquals("string", required(traceProperties, "/traceFingerprint/type").asText());
        assertEquals("string", required(traceProperties, "/snapshotFingerprint/type").asText());
        assertEquals("array", required(traceProperties, "/candidateIdsConsidered/type").asText());
        assertEquals("integer", required(traceProperties, "/candidateCount/type").asText());
        assertEquals("object", required(traceProperties, "/candidateFinalScores/type").asText());
        assertEquals("string", required(traceProperties, "/factorContributionStatus/type").asText());
        assertEquals("string", required(traceProperties, "/decisionReplaySnapshotStatus/type").asText());
        assertEquals("array", required(traceProperties, "/reconstructionSteps/type").asText());
        assertRef(required(traceProperties, "/reconstructionSteps/items"),
                "#/components/schemas/DecisionReplayReconstructionStepResponse");

        JsonNode traceStepProperties = required(docs,
                "/components/schemas/DecisionReplayReconstructionStepResponse/properties");
        assertEquals("string", required(traceStepProperties, "/stepId/type").asText());
        assertEquals("string", required(traceStepProperties, "/status/type").asText());
        assertEquals("string", required(traceStepProperties, "/evidenceSourceFieldPath/type").asText());

        JsonNode capsuleProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayCapsuleResponse/properties");
        assertEquals("boolean", required(capsuleProperties, "/readOnly/type").asText());
        assertEquals("string", required(capsuleProperties, "/capsuleSchemaVersion/type").asText());
        assertEquals("string", required(capsuleProperties, "/capsuleFingerprint/type").asText());
        assertEquals("string", required(capsuleProperties, "/linkedReplaySnapshotFingerprint/type").asText());
        assertEquals("string", required(capsuleProperties, "/linkedReconstructionTraceFingerprint/type").asText());
        assertEquals("array", required(capsuleProperties, "/candidateIdsConsidered/type").asText());
        assertEquals("array", required(capsuleProperties, "/reconstructionStepIds/type").asText());
        assertEquals("array", required(capsuleProperties, "/candidateEvidence/type").asText());
        assertRef(required(capsuleProperties, "/candidateEvidence/items"),
                "#/components/schemas/DecisionReplayCapsuleCandidateEvidenceResponse");
        assertEquals("array", required(capsuleProperties, "/factorEvidence/type").asText());
        assertRef(required(capsuleProperties, "/factorEvidence/items"),
                "#/components/schemas/DecisionReplayCapsuleFactorEvidenceResponse");

        JsonNode capsuleCandidateProperties = required(docs,
                "/components/schemas/DecisionReplayCapsuleCandidateEvidenceResponse/properties");
        assertEquals("string", required(capsuleCandidateProperties, "/candidateId/type").asText());
        assertEquals("boolean", required(capsuleCandidateProperties, "/selected/type").asText());
        assertEquals("array", required(capsuleCandidateProperties, "/factorNames/type").asText());
        assertEquals("integer", required(capsuleCandidateProperties, "/contributionCount/type").asText());

        JsonNode capsuleFactorProperties = required(docs,
                "/components/schemas/DecisionReplayCapsuleFactorEvidenceResponse/properties");
        assertEquals("string", required(capsuleFactorProperties, "/factorName/type").asText());
        assertEquals("boolean", required(capsuleFactorProperties, "/appearedInSelectedCandidate/type").asText());
        assertEquals("boolean", required(capsuleFactorProperties, "/appearedInClosestAlternative/type").asText());
        assertEquals("number", required(capsuleFactorProperties, "/contributionDelta/type").asText());

        JsonNode checklistProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayReadinessChecklistResponse/properties");
        assertEquals("boolean", required(checklistProperties, "/readOnly/type").asText());
        assertEquals("string", required(checklistProperties, "/checklistSchemaVersion/type").asText());
        assertEquals("string", required(checklistProperties, "/status/type").asText());
        assertEquals("string", required(checklistProperties, "/linkedReplaySnapshotFingerprint/type").asText());
        assertEquals("string", required(checklistProperties, "/linkedReconstructionTraceFingerprint/type").asText());
        assertEquals("string", required(checklistProperties, "/linkedReplayCapsuleFingerprint/type").asText());
        assertEquals("integer", required(checklistProperties, "/availableItemCount/type").asText());
        assertEquals("array", required(checklistProperties, "/checklistItems/type").asText());
        assertRef(required(checklistProperties, "/checklistItems/items"),
                "#/components/schemas/DecisionReplayReadinessChecklistItemResponse");

        JsonNode checklistItemProperties = required(docs,
                "/components/schemas/DecisionReplayReadinessChecklistItemResponse/properties");
        assertEquals("string", required(checklistItemProperties, "/itemId/type").asText());
        assertEquals("string", required(checklistItemProperties, "/status/type").asText());
        assertEquals("string", required(checklistItemProperties, "/evidenceSourceFieldPath/type").asText());

        JsonNode sourceMapProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceSourceMapResponse/properties");
        assertEquals("boolean", required(sourceMapProperties, "/readOnly/type").asText());
        assertEquals("string", required(sourceMapProperties, "/sourceMapSchemaVersion/type").asText());
        assertEquals("string", required(sourceMapProperties, "/status/type").asText());
        assertEquals("string", required(sourceMapProperties, "/linkedReplaySnapshotFingerprint/type").asText());
        assertEquals("string", required(sourceMapProperties, "/linkedReconstructionTraceFingerprint/type").asText());
        assertEquals("string", required(sourceMapProperties, "/linkedReplayCapsuleFingerprint/type").asText());
        assertEquals("string", required(sourceMapProperties, "/decisionReplayReadinessChecklistStatus/type")
                .asText());
        assertEquals("array", required(sourceMapProperties, "/sourceMapEntries/type").asText());
        assertRef(required(sourceMapProperties, "/sourceMapEntries/items"),
                "#/components/schemas/DecisionReplayEvidenceSourceMapEntryResponse");

        JsonNode sourceMapEntryProperties = required(docs,
                "/components/schemas/DecisionReplayEvidenceSourceMapEntryResponse/properties");
        assertEquals("string", required(sourceMapEntryProperties, "/sourceId/type").asText());
        assertEquals("string", required(sourceMapEntryProperties, "/status/type").asText());
        assertEquals("string", required(sourceMapEntryProperties, "/sourceFieldPath/type").asText());
        assertEquals("array", required(sourceMapEntryProperties, "/downstreamEvidenceFieldPaths/type").asText());

        JsonNode boundarySummaryProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceBoundarySummaryResponse/properties");
        assertEquals("boolean", required(boundarySummaryProperties, "/readOnly/type").asText());
        assertEquals("string", required(boundarySummaryProperties, "/boundarySummarySchemaVersion/type").asText());
        assertEquals("string", required(boundarySummaryProperties, "/status/type").asText());
        assertEquals("string", required(boundarySummaryProperties, "/decisionReplayEvidenceSourceMapStatus/type")
                .asText());
        assertEquals("array", required(boundarySummaryProperties, "/boundaryItems/type").asText());
        assertRef(required(boundarySummaryProperties, "/boundaryItems/items"),
                "#/components/schemas/DecisionReplayEvidenceBoundarySummaryItemResponse");

        JsonNode boundaryItemProperties = required(docs,
                "/components/schemas/DecisionReplayEvidenceBoundarySummaryItemResponse/properties");
        assertEquals("string", required(boundaryItemProperties, "/boundaryId/type").asText());
        assertEquals("string", required(boundaryItemProperties, "/status/type").asText());
        assertEquals("string", required(boundaryItemProperties, "/sourceFieldPath/type").asText());
        assertEquals("array", required(boundaryItemProperties, "/supportingEvidenceFieldPaths/type").asText());

        JsonNode fieldInventoryProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceFieldInventoryResponse/properties");
        assertEquals("boolean", required(fieldInventoryProperties, "/readOnly/type").asText());
        assertEquals("string", required(fieldInventoryProperties, "/fieldInventorySchemaVersion/type").asText());
        assertEquals("string", required(fieldInventoryProperties, "/status/type").asText());
        assertEquals("string", required(fieldInventoryProperties, "/decisionReplayEvidenceBoundarySummaryStatus/type")
                .asText());
        assertEquals("integer", required(fieldInventoryProperties, "/availableInventoryGroupCount/type").asText());
        assertEquals("integer", required(fieldInventoryProperties, "/partialInventoryGroupCount/type").asText());
        assertEquals("integer", required(fieldInventoryProperties, "/unknownInventoryGroupCount/type").asText());
        assertEquals("array", required(fieldInventoryProperties, "/inventoryEntries/type").asText());
        assertRef(required(fieldInventoryProperties, "/inventoryEntries/items"),
                "#/components/schemas/DecisionReplayEvidenceFieldInventoryEntryResponse");

        JsonNode inventoryEntryProperties = required(docs,
                "/components/schemas/DecisionReplayEvidenceFieldInventoryEntryResponse/properties");
        assertEquals("string", required(inventoryEntryProperties, "/inventoryId/type").asText());
        assertEquals("string", required(inventoryEntryProperties, "/status/type").asText());
        assertEquals("string", required(inventoryEntryProperties, "/sourceFieldPath/type").asText());
        assertEquals("array", required(inventoryEntryProperties, "/observedFieldPaths/type").asText());
        assertEquals("array", required(inventoryEntryProperties, "/missingOrUnavailableFieldPaths/type").asText());
        assertEquals("integer", required(inventoryEntryProperties, "/observedFieldCount/type").asText());
        assertEquals("integer", required(inventoryEntryProperties, "/missingOrUnavailableFieldCount/type").asText());

        JsonNode nullSafetyProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceNullSafetySummaryResponse/properties");
        assertEquals("boolean", required(nullSafetyProperties, "/readOnly/type").asText());
        assertEquals("string", required(nullSafetyProperties, "/nullSafetySchemaVersion/type").asText());
        assertEquals("string", required(nullSafetyProperties, "/status/type").asText());
        assertEquals("string", required(nullSafetyProperties, "/decisionReplayEvidenceFieldInventoryStatus/type")
                .asText());
        assertEquals("integer", required(nullSafetyProperties, "/availableNullSafetyItemCount/type").asText());
        assertEquals("integer", required(nullSafetyProperties, "/partialNullSafetyItemCount/type").asText());
        assertEquals("integer", required(nullSafetyProperties, "/unknownNullSafetyItemCount/type").asText());
        assertEquals("array", required(nullSafetyProperties, "/nullSafetyItems/type").asText());
        assertRef(required(nullSafetyProperties, "/nullSafetyItems/items"),
                "#/components/schemas/DecisionReplayEvidenceNullSafetyItemResponse");

        JsonNode nullSafetyItemProperties = required(docs,
                "/components/schemas/DecisionReplayEvidenceNullSafetyItemResponse/properties");
        assertEquals("string", required(nullSafetyItemProperties, "/nullSafetyId/type").asText());
        assertEquals("string", required(nullSafetyItemProperties, "/status/type").asText());
        assertEquals("string", required(nullSafetyItemProperties, "/sourceFieldPath/type").asText());
        assertEquals("array", required(nullSafetyItemProperties, "/checkedFieldPaths/type").asText());
        assertEquals("array", required(nullSafetyItemProperties, "/unavailableFieldPaths/type").asText());
        assertEquals("integer", required(nullSafetyItemProperties, "/checkedFieldCount/type").asText());
        assertEquals("integer", required(nullSafetyItemProperties, "/unavailableFieldCount/type").asText());

        JsonNode statusRollupProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceStatusRollupResponse/properties");
        assertEquals("boolean", required(statusRollupProperties, "/readOnly/type").asText());
        assertEquals("string", required(statusRollupProperties, "/statusRollupSchemaVersion/type").asText());
        assertEquals("string", required(statusRollupProperties, "/status/type").asText());
        assertEquals("integer", required(statusRollupProperties, "/availableLaneCount/type").asText());
        assertEquals("integer", required(statusRollupProperties, "/partialLaneCount/type").asText());
        assertEquals("integer", required(statusRollupProperties, "/unknownLaneCount/type").asText());
        assertEquals("array", required(statusRollupProperties, "/statusItems/type").asText());
        assertRef(required(statusRollupProperties, "/statusItems/items"),
                "#/components/schemas/DecisionReplayEvidenceStatusRollupItemResponse");

        JsonNode statusRollupItemProperties = required(docs,
                "/components/schemas/DecisionReplayEvidenceStatusRollupItemResponse/properties");
        assertEquals("string", required(statusRollupItemProperties, "/laneId/type").asText());
        assertEquals("string", required(statusRollupItemProperties, "/status/type").asText());
        assertEquals("string", required(statusRollupItemProperties, "/sourceFieldPath/type").asText());
        assertEquals("boolean", required(statusRollupItemProperties, "/readOnly/type").asText());
        assertEquals("boolean", required(statusRollupItemProperties, "/selectedCandidatePresent/type").asText());
        assertEquals("integer", required(statusRollupItemProperties, "/candidateCount/type").asText());
        assertEquals("boolean", required(statusRollupItemProperties, "/boundaryPresent/type").asText());

        JsonNode laneNavigationProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse/properties");
        assertEquals("boolean", required(laneNavigationProperties, "/readOnly/type").asText());
        assertEquals("string", required(laneNavigationProperties, "/laneNavigationSchemaVersion/type").asText());
        assertEquals("string", required(laneNavigationProperties, "/status/type").asText());
        assertEquals("integer", required(laneNavigationProperties, "/availableLaneCount/type").asText());
        assertEquals("integer", required(laneNavigationProperties, "/partialLaneCount/type").asText());
        assertEquals("integer", required(laneNavigationProperties, "/unknownLaneCount/type").asText());
        assertEquals("array", required(laneNavigationProperties, "/navigationItems/type").asText());
        assertRef(required(laneNavigationProperties, "/navigationItems/items"),
                "#/components/schemas/DecisionReplayEvidenceLaneNavigationItemResponse");

        JsonNode laneNavigationItemProperties = required(docs,
                "/components/schemas/DecisionReplayEvidenceLaneNavigationItemResponse/properties");
        assertEquals("string", required(laneNavigationItemProperties, "/laneId/type").asText());
        assertEquals("string", required(laneNavigationItemProperties, "/status/type").asText());
        assertEquals("string", required(laneNavigationItemProperties, "/responseFieldPath/type").asText());
        assertEquals("string", required(laneNavigationItemProperties, "/uiSectionLabel/type").asText());
        assertEquals("string", required(laneNavigationItemProperties, "/docsReferenceLabel/type").asText());
        assertEquals("boolean", required(laneNavigationItemProperties, "/readOnly/type").asText());
        assertEquals("boolean", required(laneNavigationItemProperties, "/boundaryPresent/type").asText());

        JsonNode laneDependencyMapProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceLaneDependencyMapResponse/properties");
        assertEquals("boolean", required(laneDependencyMapProperties, "/readOnly/type").asText());
        assertEquals("string", required(laneDependencyMapProperties, "/laneDependencyMapSchemaVersion/type")
                .asText());
        assertEquals("string", required(laneDependencyMapProperties, "/status/type").asText());
        assertEquals("integer", required(laneDependencyMapProperties, "/availableLaneCount/type").asText());
        assertEquals("integer", required(laneDependencyMapProperties, "/partialLaneCount/type").asText());
        assertEquals("integer", required(laneDependencyMapProperties, "/unknownLaneCount/type").asText());
        assertEquals("array", required(laneDependencyMapProperties, "/dependencyItems/type").asText());
        assertRef(required(laneDependencyMapProperties, "/dependencyItems/items"),
                "#/components/schemas/DecisionReplayEvidenceLaneDependencyItemResponse");

        JsonNode laneDependencyItemProperties = required(docs,
                "/components/schemas/DecisionReplayEvidenceLaneDependencyItemResponse/properties");
        assertEquals("string", required(laneDependencyItemProperties, "/laneId/type").asText());
        assertEquals("string", required(laneDependencyItemProperties, "/status/type").asText());
        assertEquals("string", required(laneDependencyItemProperties, "/responseFieldPath/type").asText());
        assertEquals("array", required(laneDependencyItemProperties, "/dependsOnLaneIds/type").asText());
        assertEquals("array", required(laneDependencyItemProperties, "/downstreamLaneIds/type").asText());
        assertEquals("integer", required(laneDependencyItemProperties, "/dependencyCount/type").asText());
        assertEquals("integer", required(laneDependencyItemProperties, "/downstreamCount/type").asText());
        assertEquals("boolean", required(laneDependencyItemProperties, "/readOnly/type").asText());
        assertEquals("boolean", required(laneDependencyItemProperties, "/boundaryPresent/type").asText());

        JsonNode laneReferenceIndexProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceLaneReferenceIndexResponse/properties");
        assertEquals("boolean", required(laneReferenceIndexProperties, "/readOnly/type").asText());
        assertEquals("string", required(laneReferenceIndexProperties, "/laneReferenceIndexSchemaVersion/type")
                .asText());
        assertEquals("string", required(laneReferenceIndexProperties, "/status/type").asText());
        assertEquals("integer", required(laneReferenceIndexProperties, "/availableLaneCount/type").asText());
        assertEquals("integer", required(laneReferenceIndexProperties, "/partialLaneCount/type").asText());
        assertEquals("integer", required(laneReferenceIndexProperties, "/unknownLaneCount/type").asText());
        assertEquals("array", required(laneReferenceIndexProperties, "/referenceItems/type").asText());
        assertRef(required(laneReferenceIndexProperties, "/referenceItems/items"),
                "#/components/schemas/DecisionReplayEvidenceLaneReferenceIndexItemResponse");

        JsonNode laneReferenceItemProperties = required(docs,
                "/components/schemas/DecisionReplayEvidenceLaneReferenceIndexItemResponse/properties");
        assertEquals("string", required(laneReferenceItemProperties, "/laneId/type").asText());
        assertEquals("string", required(laneReferenceItemProperties, "/status/type").asText());
        assertEquals("string", required(laneReferenceItemProperties, "/responseFieldPath/type").asText());
        assertEquals("string", required(laneReferenceItemProperties, "/uiSectionLabel/type").asText());
        assertEquals("string", required(laneReferenceItemProperties, "/docsReferenceLabel/type").asText());
        assertEquals("integer", required(laneReferenceItemProperties, "/dependencyCount/type").asText());
        assertEquals("integer", required(laneReferenceItemProperties, "/downstreamCount/type").asText());
        assertEquals("boolean", required(laneReferenceItemProperties, "/readOnly/type").asText());
        assertEquals("boolean", required(laneReferenceItemProperties, "/boundaryPresent/type").asText());

        JsonNode laneDependencySummaryProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceLaneDependencySummaryResponse/properties");
        assertEquals("boolean", required(laneDependencySummaryProperties, "/readOnly/type").asText());
        assertEquals("string", required(laneDependencySummaryProperties, "/laneDependencySummarySchemaVersion/type")
                .asText());
        assertEquals("string", required(laneDependencySummaryProperties, "/status/type").asText());
        assertEquals("integer", required(laneDependencySummaryProperties, "/totalLaneCount/type").asText());
        assertEquals("integer", required(laneDependencySummaryProperties, "/availableLaneCount/type").asText());
        assertEquals("integer", required(laneDependencySummaryProperties, "/partialLaneCount/type").asText());
        assertEquals("integer", required(laneDependencySummaryProperties, "/unknownLaneCount/type").asText());
        assertEquals("integer", required(laneDependencySummaryProperties, "/rootLaneCount/type").asText());
        assertEquals("integer", required(laneDependencySummaryProperties, "/terminalLaneCount/type").asText());
        assertEquals("integer", required(laneDependencySummaryProperties, "/maxDependencyCount/type").asText());
        assertEquals("integer", required(laneDependencySummaryProperties, "/maxDownstreamCount/type").asText());
        assertEquals("array", required(laneDependencySummaryProperties, "/densestDependencyLaneIds/type").asText());
        assertEquals("string", required(laneDependencySummaryProperties,
                "/densestDependencyLaneIds/items/type").asText());
        assertEquals("array", required(laneDependencySummaryProperties, "/widestDownstreamLaneIds/type").asText());
        assertEquals("string", required(laneDependencySummaryProperties,
                "/widestDownstreamLaneIds/items/type").asText());
        assertEquals("array", required(laneDependencySummaryProperties, "/limitations/type").asText());
        assertEquals("string", required(laneDependencySummaryProperties, "/limitations/items/type").asText());

        JsonNode laneConsistencySummaryProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse/properties");
        assertEquals("boolean", required(laneConsistencySummaryProperties, "/readOnly/type").asText());
        assertEquals("string", required(laneConsistencySummaryProperties,
                "/laneConsistencySummarySchemaVersion/type").asText());
        assertEquals("string", required(laneConsistencySummaryProperties, "/status/type").asText());
        assertEquals("string", required(laneConsistencySummaryProperties, "/referenceIndexStatus/type").asText());
        assertEquals("string", required(laneConsistencySummaryProperties, "/dependencySummaryStatus/type").asText());
        assertEquals("string", required(laneConsistencySummaryProperties, "/statusRollupStatus/type").asText());
        assertEquals("string", required(laneConsistencySummaryProperties, "/dependencyMapStatus/type").asText());
        assertEquals("integer", required(laneConsistencySummaryProperties, "/totalLaneCount/type").asText());
        assertEquals("integer", required(laneConsistencySummaryProperties, "/availableLaneCount/type").asText());
        assertEquals("integer", required(laneConsistencySummaryProperties, "/partialLaneCount/type").asText());
        assertEquals("integer", required(laneConsistencySummaryProperties, "/unknownLaneCount/type").asText());
        assertEquals("integer", required(laneConsistencySummaryProperties, "/dependencyMapLaneCount/type").asText());
        assertEquals("integer", required(laneConsistencySummaryProperties, "/referenceIndexLaneCount/type").asText());
        assertEquals("integer", required(laneConsistencySummaryProperties, "/dependencySummaryLaneCount/type")
                .asText());
        assertEquals("array", required(laneConsistencySummaryProperties, "/mismatchedCountFields/type").asText());
        assertEquals("string", required(laneConsistencySummaryProperties,
                "/mismatchedCountFields/items/type").asText());
        assertEquals("array", required(laneConsistencySummaryProperties, "/missingSurfaces/type").asText());
        assertEquals("string", required(laneConsistencySummaryProperties, "/missingSurfaces/items/type").asText());
        assertEquals("array", required(laneConsistencySummaryProperties, "/consistencyChecks/type").asText());
        assertRef(required(laneConsistencySummaryProperties, "/consistencyChecks/items"),
                "#/components/schemas/DecisionReplayEvidenceLaneConsistencyCheckResponse");
        assertEquals("array", required(laneConsistencySummaryProperties, "/limitations/type").asText());
        assertEquals("string", required(laneConsistencySummaryProperties, "/limitations/items/type").asText());

        JsonNode laneConsistencyCheckProperties = required(docs,
                "/components/schemas/DecisionReplayEvidenceLaneConsistencyCheckResponse/properties");
        assertEquals("string", required(laneConsistencyCheckProperties, "/name/type").asText());
        assertEquals("string", required(laneConsistencyCheckProperties, "/status/type").asText());
        assertEquals("string", required(laneConsistencyCheckProperties, "/expected/type").asText());
        assertEquals("string", required(laneConsistencyCheckProperties, "/actual/type").asText());
        assertEquals("string", required(laneConsistencyCheckProperties, "/detail/type").asText());

        JsonNode reviewerSnapshotProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceReviewerSnapshotResponse/properties");
        assertEquals("boolean", required(reviewerSnapshotProperties, "/readOnly/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/reviewerSnapshotSchemaVersion/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/source/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/status/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/consistencyStatus/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/referenceIndexStatus/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/dependencySummaryStatus/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/statusRollupStatus/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/dependencyMapStatus/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/strategyId/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/selectedCandidateId/type").asText());
        assertEquals("integer", required(reviewerSnapshotProperties, "/candidateCount/type").asText());
        assertEquals("integer", required(reviewerSnapshotProperties, "/totalLaneCount/type").asText());
        assertEquals("integer", required(reviewerSnapshotProperties, "/availableLaneCount/type").asText());
        assertEquals("integer", required(reviewerSnapshotProperties, "/partialLaneCount/type").asText());
        assertEquals("integer", required(reviewerSnapshotProperties, "/unknownLaneCount/type").asText());
        assertEquals("integer", required(reviewerSnapshotProperties, "/checkedSurfaceCount/type").asText());
        assertEquals("integer", required(reviewerSnapshotProperties, "/missingSurfaceCount/type").asText());
        assertEquals("array", required(reviewerSnapshotProperties, "/missingSurfaces/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/missingSurfaces/items/type").asText());
        assertEquals("array", required(reviewerSnapshotProperties, "/reviewerHighlights/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/reviewerHighlights/items/type").asText());
        assertEquals("array", required(reviewerSnapshotProperties, "/reviewerWarnings/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/reviewerWarnings/items/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/summaryText/type").asText());
        assertEquals("array", required(reviewerSnapshotProperties, "/limitations/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/limitations/items/type").asText());
        assertEquals("string", required(reviewerSnapshotProperties, "/boundaryNote/type").asText());

        JsonNode reviewerGuidanceProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceReviewerGuidanceResponse/properties");
        assertEquals("boolean", required(reviewerGuidanceProperties, "/readOnly/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties, "/reviewerGuidanceSchemaVersion/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties, "/source/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties, "/status/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties, "/reviewerPriority/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties, "/strategyId/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties, "/selectedCandidateId/type").asText());
        assertEquals("integer", required(reviewerGuidanceProperties, "/candidateCount/type").asText());
        assertEquals("integer", required(reviewerGuidanceProperties, "/totalLaneCount/type").asText());
        assertEquals("integer", required(reviewerGuidanceProperties, "/availableLaneCount/type").asText());
        assertEquals("integer", required(reviewerGuidanceProperties, "/partialLaneCount/type").asText());
        assertEquals("integer", required(reviewerGuidanceProperties, "/unknownLaneCount/type").asText());
        assertEquals("integer", required(reviewerGuidanceProperties, "/checkedSurfaceCount/type").asText());
        assertEquals("integer", required(reviewerGuidanceProperties, "/missingSurfaceCount/type").asText());
        assertEquals("array", required(reviewerGuidanceProperties, "/missingSurfaces/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties, "/missingSurfaces/items/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties, "/primaryReviewerFocus/type").asText());
        assertEquals("array", required(reviewerGuidanceProperties, "/suggestedReviewSteps/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties, "/suggestedReviewSteps/items/type").asText());
        assertEquals("array", required(reviewerGuidanceProperties, "/evidenceSurfacesToInspect/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties,
                "/evidenceSurfacesToInspect/items/type").asText());
        assertEquals("array", required(reviewerGuidanceProperties, "/cautionNotes/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties, "/cautionNotes/items/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties, "/summaryText/type").asText());
        assertEquals("array", required(reviewerGuidanceProperties, "/limitations/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties, "/limitations/items/type").asText());
        assertEquals("string", required(reviewerGuidanceProperties, "/boundaryNote/type").asText());

        JsonNode reviewerHandoffProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse/properties");
        assertEquals("boolean", required(reviewerHandoffProperties, "/readOnly/type").asText());
        assertEquals("string", required(reviewerHandoffProperties,
                "/reviewerHandoffSummarySchemaVersion/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/source/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/status/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/handoffPriority/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/reviewerSnapshotStatus/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/reviewerGuidanceStatus/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/consistencyStatus/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/strategyId/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/selectedCandidateId/type").asText());
        assertEquals("integer", required(reviewerHandoffProperties, "/candidateCount/type").asText());
        assertEquals("integer", required(reviewerHandoffProperties, "/totalLaneCount/type").asText());
        assertEquals("integer", required(reviewerHandoffProperties, "/availableLaneCount/type").asText());
        assertEquals("integer", required(reviewerHandoffProperties, "/partialLaneCount/type").asText());
        assertEquals("integer", required(reviewerHandoffProperties, "/unknownLaneCount/type").asText());
        assertEquals("array", required(reviewerHandoffProperties, "/handoffBullets/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/handoffBullets/items/type").asText());
        assertEquals("array", required(reviewerHandoffProperties, "/operatorFollowUpItems/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/operatorFollowUpItems/items/type").asText());
        assertEquals("array", required(reviewerHandoffProperties, "/evidenceSurfacesReferenced/type").asText());
        assertEquals("string", required(reviewerHandoffProperties,
                "/evidenceSurfacesReferenced/items/type").asText());
        assertEquals("array", required(reviewerHandoffProperties, "/cautionNotes/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/cautionNotes/items/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/summaryText/type").asText());
        assertEquals("array", required(reviewerHandoffProperties, "/limitations/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/limitations/items/type").asText());
        assertEquals("string", required(reviewerHandoffProperties, "/boundaryNote/type").asText());

        JsonNode reviewerClosureProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse/properties");
        assertEquals("boolean", required(reviewerClosureProperties, "/readOnly/type").asText());
        assertEquals("string", required(reviewerClosureProperties,
                "/reviewerClosureSummarySchemaVersion/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/source/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/status/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/closureDisposition/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/reviewerSnapshotStatus/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/reviewerGuidanceStatus/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/reviewerHandoffStatus/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/consistencyStatus/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/strategyId/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/selectedCandidateId/type").asText());
        assertEquals("integer", required(reviewerClosureProperties, "/candidateCount/type").asText());
        assertEquals("integer", required(reviewerClosureProperties, "/totalLaneCount/type").asText());
        assertEquals("integer", required(reviewerClosureProperties, "/availableLaneCount/type").asText());
        assertEquals("integer", required(reviewerClosureProperties, "/partialLaneCount/type").asText());
        assertEquals("integer", required(reviewerClosureProperties, "/unknownLaneCount/type").asText());
        assertEquals("array", required(reviewerClosureProperties, "/closureBullets/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/closureBullets/items/type").asText());
        assertEquals("array", required(reviewerClosureProperties, "/safeConclusions/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/safeConclusions/items/type").asText());
        assertEquals("array", required(reviewerClosureProperties, "/unresolvedBoundaries/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/unresolvedBoundaries/items/type").asText());
        assertEquals("array", required(reviewerClosureProperties, "/evidenceSurfacesReferenced/type").asText());
        assertEquals("string", required(reviewerClosureProperties,
                "/evidenceSurfacesReferenced/items/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/summaryText/type").asText());
        assertEquals("array", required(reviewerClosureProperties, "/limitations/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/limitations/items/type").asText());
        assertEquals("string", required(reviewerClosureProperties, "/boundaryNote/type").asText());

        JsonNode reviewerClosureRollupProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceReviewerClosureRollupResponse/properties");
        assertEquals("string", required(reviewerClosureRollupProperties, "/status/type").asText());
        assertEquals("string", required(reviewerClosureRollupProperties, "/disposition/type").asText());
        assertEquals("integer", required(reviewerClosureRollupProperties, "/resultCount/type").asText());
        assertEquals("integer",
                required(reviewerClosureRollupProperties, "/resultsWithClosureSummary/type").asText());
        assertEquals("integer",
                required(reviewerClosureRollupProperties, "/resultsMissingClosureSummary/type").asText());
        assertEquals("integer",
                required(reviewerClosureRollupProperties, "/completeWithLimitationsCount/type").asText());
        assertEquals("integer", required(reviewerClosureRollupProperties, "/unknownCount/type").asText());
        assertEquals("boolean", required(reviewerClosureRollupProperties, "/reviewerReady/type").asText());
        assertEquals("string", required(reviewerClosureRollupProperties, "/summary/type").asText());
        assertEquals("array", required(reviewerClosureRollupProperties, "/notProvenBoundaries/type").asText());
        assertEquals("string",
                required(reviewerClosureRollupProperties, "/notProvenBoundaries/items/type").asText());

        JsonNode reviewerClosureChecklistProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse/properties");
        assertEquals("string", required(reviewerClosureChecklistProperties, "/status/type").asText());
        assertEquals("boolean", required(reviewerClosureChecklistProperties, "/reviewerReady/type").asText());
        assertEquals("array", required(reviewerClosureChecklistProperties, "/items/type").asText());
        assertRef(required(reviewerClosureChecklistProperties, "/items/items"),
                "#/components/schemas/RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse");
        assertEquals("string", required(reviewerClosureChecklistProperties, "/summary/type").asText());
        assertEquals("array", required(reviewerClosureChecklistProperties, "/notProvenBoundaries/type").asText());
        assertEquals("string",
                required(reviewerClosureChecklistProperties, "/notProvenBoundaries/items/type").asText());

        JsonNode reviewerClosureChecklistItemProperties = required(docs,
                "/components/schemas/RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse/properties");
        assertEquals("string", required(reviewerClosureChecklistItemProperties, "/name/type").asText());
        assertEquals("string", required(reviewerClosureChecklistItemProperties, "/status/type").asText());
        assertEquals("string", required(reviewerClosureChecklistItemProperties, "/description/type").asText());
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

    private static void assertArrayRef(JsonNode schema, String expectedItemRef) {
        assertEquals("array", required(schema, "/type").asText());
        assertRef(required(schema, "/items"), expectedItemRef);
    }
}
