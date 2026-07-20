package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class ApiContractTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String CAPACITY_AWARE_REQUEST = """
            {
              "requestedLoad": 75.0,
              "servers": [
                {
                  "id": "api-1",
                  "cpuUsage": 90.0,
                  "memoryUsage": 90.0,
                  "diskUsage": 90.0,
                  "capacity": 100.0,
                  "weight": 1.0,
                  "healthy": true
                },
                {
                  "id": "worker-1",
                  "cpuUsage": 80.0,
                  "memoryUsage": 80.0,
                  "diskUsage": 80.0,
                  "capacity": 100.0,
                  "weight": 1.0,
                  "healthy": true
                }
              ]
            }
            """;

    private static final String EVALUATION_REQUEST = """
            {
              "requestedLoad": 150.0,
              "strategy": "CAPACITY_AWARE",
              "priority": "BACKGROUND",
              "currentInFlightRequestCount": 95,
              "concurrencyLimit": 100,
              "queueDepth": 25,
              "observedP95LatencyMillis": 300.0,
              "observedErrorRate": 0.20,
              "servers": [
                {
                  "id": "primary",
                  "cpuUsage": 30.0,
                  "memoryUsage": 30.0,
                  "diskUsage": 30.0,
                  "capacity": 100.0,
                  "weight": 1.0,
                  "healthy": true
                },
                {
                  "id": "fallback",
                  "cpuUsage": 70.0,
                  "memoryUsage": 70.0,
                  "diskUsage": 70.0,
                  "capacity": 100.0,
                  "weight": 1.0,
                  "healthy": true
                },
                {
                  "id": "failed",
                  "cpuUsage": 0.0,
                  "memoryUsage": 0.0,
                  "diskUsage": 0.0,
                  "capacity": 500.0,
                  "weight": 10.0,
                  "healthy": false
                }
              ]
            }
            """;

    private static final String ROUTING_REQUEST = """
            {
              "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
              "servers": [
                {
                  "serverId": "green",
                  "healthy": true,
                  "inFlightRequestCount": 5,
                  "configuredCapacity": 100.0,
                  "estimatedConcurrencyLimit": 100.0,
                  "averageLatencyMillis": 20.0,
                  "p95LatencyMillis": 40.0,
                  "p99LatencyMillis": 80.0,
                  "recentErrorRate": 0.01,
                  "queueDepth": 1
                },
                {
                  "serverId": "blue",
                  "healthy": true,
                  "inFlightRequestCount": 75,
                  "configuredCapacity": 100.0,
                  "estimatedConcurrencyLimit": 100.0,
                  "averageLatencyMillis": 35.0,
                  "p95LatencyMillis": 120.0,
                  "p99LatencyMillis": 220.0,
                  "recentErrorRate": 0.15,
                  "queueDepth": 10
                }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocumentExposesCoreApiPathsAndSchemas() throws Exception {
        JsonNode docs = openApiDocs();

        assertPathRequestSchemaAndOkResponse(docs, "/api/allocate/capacity-aware", "AllocationRequest");
        assertPathRequestSchemaAndOkResponse(docs, "/api/allocate/predictive", "AllocationRequest");
        assertPathRequestSchemaAndOkResponse(docs, "/api/allocate/evaluate", "AllocationEvaluationRequest");
        assertPathRequestSchemaAndOkResponse(docs, "/api/routing/compare", "RoutingComparisonRequest");
        assertPathRequestSchemaAndOkResponse(docs, "/api/scenarios/replay", "ScenarioReplayRequest");
        assertPathRequestSchemaAndOkResponse(docs, "/api/remediation/report", "RemediationReportRequest");

        assertSchemaProperties(docs, "AllocationRequest", "requestedLoad", "servers");
        assertSchemaRequired(docs, "AllocationRequest", "requestedLoad", "servers");
        assertSchemaProperties(docs, "AllocationResponse", "allocations", "unallocatedLoad",
                "recommendedAdditionalServers", "scalingSimulation");
        assertSchemaProperties(docs, "AllocationEvaluationRequest", "requestedLoad", "servers",
                "strategy", "priority", "currentInFlightRequestCount", "concurrencyLimit",
                "queueDepth", "observedP95LatencyMillis", "observedErrorRate");
        assertSchemaRequired(docs, "AllocationEvaluationRequest", "requestedLoad", "servers");
        assertSchemaRequired(docs, "ServerInput", "id", "cpuUsage", "memoryUsage", "diskUsage",
                "capacity", "weight", "healthy");
        assertSchemaProperties(docs, "AllocationEvaluationResponse", "strategy", "allocations",
                "acceptedLoad", "rejectedLoad", "unallocatedLoad", "recommendedAdditionalServers",
                "scalingSimulation", "loadShedding", "metricsPreview", "lasePolicy", "readOnly", "remediationPlan",
                "decisionReason");
        assertSchemaProperties(docs, "RemediationPlan", "status", "generatedFrom", "advisoryOnly",
                "readOnly", "cloudMutation", "recommendations");
        assertSchemaProperties(docs, "RemediationRecommendation", "rank", "action", "priority",
                "reason", "serverCount", "loadAmount", "executable", "message");
        assertSchemaProperties(docs, "LoadSheddingEvaluation", "priority", "action", "reason",
                "targetId", "currentInFlightRequestCount", "concurrencyLimit", "queueDepth",
                "utilization", "observedP95LatencyMillis", "observedErrorRate");
        assertSchemaProperties(docs, "AllocationEvaluationMetricsPreview", "strategy",
                "evaluatedHealthyServerCount", "acceptedLoad", "rejectedLoad", "unallocatedLoad",
                "recommendedAdditionalServers", "metricNames", "emitted");
        assertSchemaProperties(docs, "RoutingComparisonRequest", "strategies", "servers");
        assertSchemaProperties(docs, "RoutingComparisonResponse", "requestedStrategies", "candidateCount",
                "timestamp", "decisionReplayEvidenceReviewerClosureRollup",
                "decisionReplayEvidenceReviewerClosureChecklist", "decisionReplayEvidenceReviewerClosurePacket",
                "results");
        assertSchemaProperties(docs, "RoutingComparisonResultResponse", "strategyId", "status",
                "chosenServerId", "reason", "candidateServersConsidered", "scores", "decisionVector",
                "dominantFactorAnalysis", "decisionDeltaAnalysis", "decisionReplaySnapshot",
                "decisionReplayReconstructionTrace", "decisionReplayCapsule",
                "decisionReplayReadinessChecklist", "decisionReplayEvidenceSourceMap",
                "decisionReplayEvidenceBoundarySummary", "decisionReplayEvidenceFieldInventory",
                "decisionReplayEvidenceNullSafetySummary", "decisionReplayEvidenceStatusRollup",
                "decisionReplayEvidenceLaneNavigationSummary", "decisionReplayEvidenceLaneDependencyMap",
                "decisionReplayEvidenceLaneReferenceIndex", "decisionReplayEvidenceLaneDependencySummary",
                "decisionReplayEvidenceLaneConsistencySummary", "decisionReplayEvidenceReviewerSnapshot",
                "decisionReplayEvidenceReviewerGuidance", "decisionReplayEvidenceReviewerHandoffSummary",
                "decisionReplayEvidenceReviewerClosureSummary");
        assertSchemaProperties(docs, "RoutingDecisionVectorResponse", "readOnly", "localLabResponsePath",
                "decisionIdOrLabRunId", "selectedStrategy", "selectedBackend", "candidateCount",
                "candidateSummaries", "selectedCandidateVector", "nonSelectedCandidateVectors",
                "knownVisibleSignals", "unknownOrUnexposedSignals", "exactnessBoundary",
                "selectedVsAlternativeExplanationNotes", "labProofBoundary", "productionNotProvenBoundary",
                "factorContributionAvailability", "replayReadiness", "whatIfReadiness",
                "structuredDecisionLoggingReadiness");
        assertSchemaProperties(docs, "CandidateDecisionVectorResponse", "candidateId", "selected",
                "knownVisibleSignals", "unknownOrUnexposedSignals", "factorContributions",
                "selectedVsAlternativeExplanationNote", "exactnessBoundary", "labProofBoundary",
                "productionNotProvenBoundary");
        assertSchemaProperties(docs, "ScoreFactorContributionResponse", "factorName", "rawValueDescription",
                "weightDescription", "direction", "contributionDescription", "contributionValue",
                "exactness", "explanationText", "boundaryNote");
        assertSchemaProperties(docs, "DominantFactorAnalysisResponse", "readOnly", "source", "status",
                "candidateAnalyses", "selectedDecisionAnalysis", "selectedDecisionExplanation",
                "boundaryNote", "productionNotProvenBoundary");
        assertSchemaProperties(docs, "CandidateDominantFactorResponse", "candidateId", "selected", "available",
                "sourceContributionCount", "sourceFactorNames", "largestPositiveContributor",
                "largestPenaltyContributor", "largestAbsoluteImpact", "explanation", "boundaryNote");
        assertSchemaProperties(docs, "DominantFactorResponse", "factorName", "direction", "contributionValue",
                "absoluteImpact", "contributionDescription", "explanationText", "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionDeltaAnalysisResponse", "readOnly", "source", "status",
                "comparison", "factorDeltas", "largestAbsoluteFactorDelta", "explanation", "boundaryNote",
                "productionNotProvenBoundary");
        assertSchemaProperties(docs, "CandidateDecisionDeltaResponse", "selectedCandidateId",
                "closestAlternativeCandidateId", "selectedFinalScore", "alternativeFinalScore", "finalScoreGap",
                "absoluteFinalScoreGap", "selectedContributionCount", "alternativeContributionCount",
                "comparedFactorCount", "comparedFactorNames", "omittedFactorNames", "explanation");
        assertSchemaProperties(docs, "ScoreFactorDeltaResponse", "factorName", "selectedCandidateContribution",
                "alternativeCandidateContribution", "contributionDelta", "absoluteDelta",
                "selectedCandidateDirection", "alternativeCandidateDirection", "explanation");
        assertSchemaProperties(docs, "RoutingDecisionReplaySnapshotResponse", "readOnly", "snapshotSchemaVersion",
                "source", "status", "snapshotFingerprint", "fingerprintAlgorithm", "selectedCandidateId",
                "candidateIdsConsidered", "candidateCount", "strategyId", "decisionVectorStatus",
                "dominantFactorAnalysisStatus", "decisionDeltaAnalysisStatus", "closestAlternativeCandidateId",
                "finalScoreGap", "largestDeltaFactorName", "explanation", "boundaryNote",
                "productionNotProvenBoundary");
        assertSchemaProperties(docs, "RoutingDecisionReplayReconstructionTraceResponse", "readOnly",
                "traceSchemaVersion", "source", "status", "traceFingerprint", "fingerprintAlgorithm",
                "snapshotFingerprint", "selectedCandidateId", "candidateIdsConsidered", "candidateCount",
                "candidateFinalScores", "strategyId", "decisionVectorStatus", "factorContributionStatus",
                "dominantFactorAnalysisStatus", "decisionDeltaAnalysisStatus", "decisionReplaySnapshotStatus",
                "closestAlternativeCandidateId", "finalScoreGap", "largestDeltaFactorName",
                "reconstructionSteps", "explanation", "boundaryNote", "productionNotProvenBoundary");
        assertSchemaProperties(docs, "DecisionReplayReconstructionStepResponse", "stepId", "status",
                "evidenceSourceFieldPath", "explanation", "missingEvidenceReason");
        assertSchemaProperties(docs, "RoutingDecisionReplayCapsuleResponse", "readOnly", "capsuleSchemaVersion",
                "source", "status", "capsuleFingerprint", "fingerprintAlgorithm", "selectedCandidateId",
                "candidateIdsConsidered", "candidateCount", "closestAlternativeCandidateId", "finalScoreGap",
                "largestDeltaFactorName", "linkedReplaySnapshotFingerprint", "linkedReconstructionTraceFingerprint",
                "strategyId", "decisionVectorStatus", "factorContributionStatus", "dominantFactorAnalysisStatus",
                "decisionDeltaAnalysisStatus", "decisionReplaySnapshotStatus",
                "decisionReplayReconstructionTraceStatus", "reconstructionStepIds", "candidateEvidence",
                "factorEvidence", "explanation", "boundaryNote", "productionNotProvenBoundary");
        assertSchemaProperties(docs, "DecisionReplayCapsuleCandidateEvidenceResponse", "candidateId", "selected",
                "finalScore", "factorNames", "contributionCount", "dominantFactorNames", "status");
        assertSchemaProperties(docs, "DecisionReplayCapsuleFactorEvidenceResponse", "factorName",
                "appearedInSelectedCandidate", "appearedInClosestAlternative", "selectedCandidateContribution",
                "closestAlternativeContribution", "contributionDelta", "status");
        assertSchemaProperties(docs, "RoutingDecisionReplayReadinessChecklistResponse", "readOnly",
                "checklistSchemaVersion", "source", "status", "strategyId", "selectedCandidateId",
                "candidateCount", "linkedReplaySnapshotFingerprint", "linkedReconstructionTraceFingerprint",
                "linkedReplayCapsuleFingerprint", "decisionVectorStatus", "dominantFactorAnalysisStatus",
                "decisionDeltaAnalysisStatus", "decisionReplaySnapshotStatus",
                "decisionReplayReconstructionTraceStatus", "decisionReplayCapsuleStatus",
                "availableItemCount", "partialItemCount", "unknownItemCount", "checklistItems",
                "explanation", "boundaryNote", "productionNotProvenBoundary");
        assertSchemaProperties(docs, "DecisionReplayReadinessChecklistItemResponse", "itemId", "label", "status",
                "evidenceSourceFieldPath", "explanation", "missingEvidenceReason");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceSourceMapResponse", "readOnly",
                "sourceMapSchemaVersion", "source", "status", "strategyId", "selectedCandidateId",
                "candidateCount", "linkedReplaySnapshotFingerprint", "linkedReconstructionTraceFingerprint",
                "linkedReplayCapsuleFingerprint", "decisionVectorStatus", "dominantFactorAnalysisStatus",
                "decisionDeltaAnalysisStatus", "decisionReplaySnapshotStatus",
                "decisionReplayReconstructionTraceStatus", "decisionReplayCapsuleStatus",
                "decisionReplayReadinessChecklistStatus", "sourceMapEntries", "explanation", "boundaryNote",
                "productionNotProvenBoundary");
        assertSchemaProperties(docs, "DecisionReplayEvidenceSourceMapEntryResponse", "sourceId", "label",
                "status", "sourceFieldPath", "downstreamEvidenceFieldPaths", "linkedFingerprint",
                "evidenceSummary", "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceBoundarySummaryResponse", "readOnly",
                "boundarySummarySchemaVersion", "source", "status", "strategyId", "selectedCandidateId",
                "candidateCount", "decisionVectorStatus", "dominantFactorAnalysisStatus",
                "decisionDeltaAnalysisStatus", "decisionReplaySnapshotStatus",
                "decisionReplayReconstructionTraceStatus", "decisionReplayCapsuleStatus",
                "decisionReplayReadinessChecklistStatus", "decisionReplayEvidenceSourceMapStatus",
                "boundaryItems", "explanation", "boundaryNote", "productionNotProvenBoundary");
        assertSchemaProperties(docs, "DecisionReplayEvidenceBoundarySummaryItemResponse", "boundaryId", "label",
                "status", "sourceFieldPath", "supportingEvidenceFieldPaths", "evidenceSummary", "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceFieldInventoryResponse", "readOnly",
                "fieldInventorySchemaVersion", "source", "status", "strategyId", "selectedCandidateId",
                "candidateCount", "decisionVectorStatus", "dominantFactorAnalysisStatus",
                "decisionDeltaAnalysisStatus", "decisionReplaySnapshotStatus",
                "decisionReplayReconstructionTraceStatus", "decisionReplayCapsuleStatus",
                "decisionReplayReadinessChecklistStatus", "decisionReplayEvidenceSourceMapStatus",
                "decisionReplayEvidenceBoundarySummaryStatus", "availableInventoryGroupCount",
                "partialInventoryGroupCount", "unknownInventoryGroupCount", "inventoryEntries",
                "explanation", "boundaryNote", "productionNotProvenBoundary");
        assertSchemaProperties(docs, "DecisionReplayEvidenceFieldInventoryEntryResponse", "inventoryId", "label",
                "status", "sourceFieldPath", "observedFieldPaths", "missingOrUnavailableFieldPaths",
                "observedFieldCount", "missingOrUnavailableFieldCount", "evidenceSummary", "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceNullSafetySummaryResponse", "readOnly",
                "nullSafetySchemaVersion", "source", "status", "strategyId", "selectedCandidateId",
                "candidateCount", "decisionVectorStatus", "dominantFactorAnalysisStatus",
                "decisionDeltaAnalysisStatus", "decisionReplaySnapshotStatus",
                "decisionReplayReconstructionTraceStatus", "decisionReplayCapsuleStatus",
                "decisionReplayReadinessChecklistStatus", "decisionReplayEvidenceSourceMapStatus",
                "decisionReplayEvidenceBoundarySummaryStatus", "decisionReplayEvidenceFieldInventoryStatus",
                "availableNullSafetyItemCount", "partialNullSafetyItemCount", "unknownNullSafetyItemCount",
                "nullSafetyItems", "explanation", "boundaryNote", "productionNotProvenBoundary");
        assertSchemaProperties(docs, "DecisionReplayEvidenceNullSafetyItemResponse", "nullSafetyId", "label",
                "status", "sourceFieldPath", "checkedFieldPaths", "unavailableFieldPaths",
                "checkedFieldCount", "unavailableFieldCount", "safetySummary", "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceStatusRollupResponse", "readOnly",
                "statusRollupSchemaVersion", "source", "status", "strategyId", "selectedCandidateId",
                "candidateCount", "availableLaneCount", "partialLaneCount", "unknownLaneCount",
                "statusItems", "explanation", "boundaryNote", "productionNotProvenBoundary");
        assertSchemaProperties(docs, "DecisionReplayEvidenceStatusRollupItemResponse", "laneId", "label",
                "status", "sourceFieldPath", "readOnly", "selectedCandidatePresent", "candidateCount",
                "boundaryPresent", "evidenceSummary", "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse", "readOnly",
                "laneNavigationSchemaVersion", "source", "status", "strategyId", "selectedCandidateId",
                "candidateCount", "availableLaneCount", "partialLaneCount", "unknownLaneCount",
                "navigationItems", "explanation", "boundaryNote", "productionNotProvenBoundary");
        assertSchemaProperties(docs, "DecisionReplayEvidenceLaneNavigationItemResponse", "laneId", "label",
                "status", "responseFieldPath", "uiSectionLabel", "docsReferenceLabel", "readOnly",
                "boundaryPresent", "navigationSummary", "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceLaneDependencyMapResponse", "readOnly",
                "laneDependencyMapSchemaVersion", "source", "status", "strategyId", "selectedCandidateId",
                "candidateCount", "availableLaneCount", "partialLaneCount", "unknownLaneCount",
                "dependencyItems", "explanation", "boundaryNote", "productionNotProvenBoundary");
        assertSchemaProperties(docs, "DecisionReplayEvidenceLaneDependencyItemResponse", "laneId", "label",
                "status", "responseFieldPath", "dependsOnLaneIds", "downstreamLaneIds", "dependencyCount",
                "downstreamCount", "readOnly", "boundaryPresent", "dependencySummary", "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceLaneReferenceIndexResponse", "readOnly",
                "laneReferenceIndexSchemaVersion", "source", "status", "strategyId", "selectedCandidateId",
                "candidateCount", "availableLaneCount", "partialLaneCount", "unknownLaneCount",
                "referenceItems", "explanation", "boundaryNote", "productionNotProvenBoundary");
        assertSchemaProperties(docs, "DecisionReplayEvidenceLaneReferenceIndexItemResponse", "laneId", "label",
                "status", "responseFieldPath", "uiSectionLabel", "docsReferenceLabel", "dependencyCount",
                "downstreamCount", "readOnly", "boundaryPresent", "referenceSummary", "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceLaneDependencySummaryResponse", "readOnly",
                "laneDependencySummarySchemaVersion", "source", "status", "strategyId", "selectedCandidateId",
                "candidateCount", "totalLaneCount", "availableLaneCount", "partialLaneCount",
                "unknownLaneCount", "rootLaneCount", "terminalLaneCount", "maxDependencyCount",
                "maxDownstreamCount", "densestDependencyLaneIds", "widestDownstreamLaneIds", "summaryText",
                "limitations", "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse", "readOnly",
                "laneConsistencySummarySchemaVersion", "source", "status", "referenceIndexStatus",
                "dependencySummaryStatus", "statusRollupStatus", "dependencyMapStatus", "strategyId",
                "selectedCandidateId", "candidateCount", "totalLaneCount", "availableLaneCount",
                "partialLaneCount", "unknownLaneCount", "dependencyMapLaneCount", "referenceIndexLaneCount",
                "dependencySummaryLaneCount", "mismatchedCountFields", "missingSurfaces", "consistencyChecks",
                "summaryText", "limitations", "boundaryNote");
        assertSchemaProperties(docs, "DecisionReplayEvidenceLaneConsistencyCheckResponse", "name", "status",
                "expected", "actual", "detail");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceReviewerSnapshotResponse", "readOnly",
                "reviewerSnapshotSchemaVersion", "source", "status", "consistencyStatus", "referenceIndexStatus",
                "dependencySummaryStatus", "statusRollupStatus", "dependencyMapStatus", "strategyId",
                "selectedCandidateId", "candidateCount", "totalLaneCount", "availableLaneCount",
                "partialLaneCount", "unknownLaneCount", "checkedSurfaceCount", "missingSurfaceCount",
                "missingSurfaces", "reviewerHighlights", "reviewerWarnings", "summaryText", "limitations",
                "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceReviewerGuidanceResponse", "readOnly",
                "reviewerGuidanceSchemaVersion", "source", "status", "reviewerPriority", "strategyId",
                "selectedCandidateId", "candidateCount", "totalLaneCount", "availableLaneCount",
                "partialLaneCount", "unknownLaneCount", "checkedSurfaceCount", "missingSurfaceCount",
                "missingSurfaces", "primaryReviewerFocus", "suggestedReviewSteps",
                "evidenceSurfacesToInspect", "cautionNotes", "summaryText", "limitations", "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse", "readOnly",
                "reviewerHandoffSummarySchemaVersion", "source", "status", "handoffPriority",
                "reviewerSnapshotStatus", "reviewerGuidanceStatus", "consistencyStatus", "strategyId",
                "selectedCandidateId", "candidateCount", "totalLaneCount", "availableLaneCount",
                "partialLaneCount", "unknownLaneCount", "handoffBullets", "operatorFollowUpItems",
                "evidenceSurfacesReferenced", "cautionNotes", "summaryText", "limitations", "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse", "readOnly",
                "reviewerClosureSummarySchemaVersion", "source", "status", "closureDisposition",
                "reviewerSnapshotStatus", "reviewerGuidanceStatus", "reviewerHandoffStatus",
                "consistencyStatus", "strategyId", "selectedCandidateId", "candidateCount",
                "totalLaneCount", "availableLaneCount", "partialLaneCount", "unknownLaneCount",
                "closureBullets", "safeConclusions", "unresolvedBoundaries", "evidenceSurfacesReferenced",
                "summaryText", "limitations", "boundaryNote");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceReviewerClosureRollupResponse",
                "status", "disposition", "resultCount", "resultsWithClosureSummary",
                "resultsMissingClosureSummary", "completeWithLimitationsCount", "unknownCount",
                "reviewerReady", "summary", "notProvenBoundaries");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceReviewerClosureChecklistResponse",
                "status", "reviewerReady", "items", "summary", "notProvenBoundaries");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceReviewerClosureChecklistItemResponse",
                "name", "status", "description");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceReviewerClosurePacketResponse",
                "status", "reviewerReady", "packetVersion", "sections", "summary", "reviewerGuidance",
                "notProvenBoundaries");
        assertSchemaProperties(docs, "RoutingDecisionReplayEvidenceReviewerClosurePacketSectionResponse",
                "name", "status", "description");
        assertSchemaProperties(docs, "ScenarioReplayRequest", "scenarioId", "servers", "steps");
        assertSchemaProperties(docs, "ScenarioReplayStepRequest", "stepId", "type", "requestedLoad",
                "strategy", "priority", "serverId", "routingStrategies", "currentInFlightRequestCount",
                "concurrencyLimit", "queueDepth", "observedP95LatencyMillis", "observedErrorRate");
        assertSchemaProperties(docs, "ScenarioReplayResponse", "scenarioId", "readOnly", "cloudMutation",
                "remediationPlan", "steps");
        assertSchemaProperties(docs, "ScenarioReplayStepResponse", "stepId", "type", "status", "strategy",
                "allocations", "acceptedLoad", "rejectedLoad", "unallocatedLoad", "recommendedAdditionalServers",
                "scalingSimulation", "loadShedding", "metricsPreview", "selectedServerId", "routingResults",
                "serverStates", "reason");
        assertSchemaProperties(docs, "ScenarioServerState", "id", "cpuUsage", "memoryUsage", "diskUsage",
                "capacity", "weight", "healthy");
        assertSchemaProperties(docs, "RemediationReportRequest", "reportId", "title", "format", "evaluation",
                "replay");
        assertSchemaProperties(docs, "RemediationReportResponse", "format", "contentType", "report", "json",
                "readOnly", "advisoryOnly", "cloudMutation");
        assertSchemaProperties(docs, "RemediationReportPayload", "reportId", "title", "sourceType", "status",
                "summary", "acceptedLoad", "rejectedLoad", "unallocatedLoad", "recommendedAdditionalServers",
                "scalingSimulation", "loadShedding", "remediationPlan", "readOnly", "advisoryOnly",
                "cloudMutation", "warnings", "limitations", "steps");
        assertSchemaProperties(docs, "RemediationReportStep", "stepId", "type", "status", "acceptedLoad",
                "rejectedLoad", "unallocatedLoad", "recommendedAdditionalServers", "selectedServerId",
                "loadSheddingAction", "reason");
    }

    @Test
    void capacityAwareAllocationResponseShapeIsStable() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CAPACITY_AWARE_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.allocations").isMap())
                .andExpect(jsonPath("$.allocations['api-1']", closeTo(10.0, 0.01)))
                .andExpect(jsonPath("$.allocations['worker-1']", closeTo(20.0, 0.01)))
                .andExpect(jsonPath("$.unallocatedLoad", closeTo(45.0, 0.01)))
                .andExpect(jsonPath("$.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation").isMap())
                .andExpect(jsonPath("$.scalingSimulation.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation.reason", containsString("simulated scale-up")))
                .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void evaluationResponseShapeIsStableAndReadOnly() throws Exception {
        String first = mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVALUATION_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.strategy", is("CAPACITY_AWARE")))
                .andExpect(jsonPath("$.allocations").isMap())
                .andExpect(jsonPath("$.allocations.primary", closeTo(70.0, 0.01)))
                .andExpect(jsonPath("$.allocations.fallback", closeTo(30.0, 0.01)))
                .andExpect(jsonPath("$.allocations.failed").doesNotExist())
                .andExpect(jsonPath("$.acceptedLoad", closeTo(100.0, 0.01)))
                .andExpect(jsonPath("$.rejectedLoad", closeTo(50.0, 0.01)))
                .andExpect(jsonPath("$.unallocatedLoad", closeTo(50.0, 0.01)))
                .andExpect(jsonPath("$.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.scalingSimulation.simulatedOnly", is(true)))
                .andExpect(jsonPath("$.loadShedding.priority", is("BACKGROUND")))
                .andExpect(jsonPath("$.loadShedding.action", is("SHED")))
                .andExpect(jsonPath("$.loadShedding.reason", containsString("overload pressure")))
                .andExpect(jsonPath("$.loadShedding.utilization", closeTo(0.95, 0.01)))
                .andExpect(jsonPath("$.metricsPreview.emitted", is(false)))
                .andExpect(jsonPath("$.metricsPreview.metricNames").isArray())
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.remediationPlan.status", is("OVERLOADED")))
                .andExpect(jsonPath("$.remediationPlan.advisoryOnly", is(true)))
                .andExpect(jsonPath("$.remediationPlan.cloudMutation", is(false)))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].action", is("SCALE_UP")))
                .andExpect(jsonPath("$.remediationPlan.recommendations[0].priority", is("HIGH")))
                .andExpect(jsonPath("$.remediationPlan.recommendations[1].action", is("SHED_LOAD")))
                .andExpect(jsonPath("$.remediationPlan.recommendations[2].action", is("INVESTIGATE_UNHEALTHY")))
                .andExpect(jsonPath("$.decisionReason", containsString("Read-only evaluation")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String second = mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVALUATION_REQUEST))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(first, second, "evaluation responses must stay deterministic for generated clients");
    }

    @Test
    void routingComparisonResponseShapeIsStable() throws Exception {
        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROUTING_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requestedStrategies[0]", is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.candidateCount", is(2)))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.status", is("COMPLETE")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.disposition",
                        is("REVIEW_COMPLETE_WITH_LIMITATIONS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.resultCount", is(1)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.resultsWithClosureSummary",
                        is(1)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.resultsMissingClosureSummary",
                        is(0)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup"
                        + ".completeWithLimitationsCount", is(1)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.unknownCount", is(0)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.reviewerReady", is(true)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.summary",
                        containsString("1 of 1 results include closure summaries")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.notProvenBoundaries[0]",
                        is("not replay proof")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.notProvenBoundaries[6]",
                        is("not production validation")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.status", is("COMPLETE")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.reviewerReady", is(true)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[0].name",
                        is("closureSummaryPresent")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[0].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[1].name",
                        is("closureRollupPresent")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[1].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[2].name",
                        is("countsMatchResultMetadata")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[2].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[3].name",
                        is("scenarioReplayStripped")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[3].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[4].name",
                        is("notProvenBoundariesPresent")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[4].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.summary",
                        containsString("closureSummaryPresent=PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.notProvenBoundaries[0]",
                        is("not replay proof")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.notProvenBoundaries[6]",
                        is("not production validation")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.status", is("COMPLETE")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.reviewerReady", is(true)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.packetVersion", is("v1")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[0].name",
                        is("closureSummary")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[0].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[1].name",
                        is("closureRollup")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[1].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[2].name",
                        is("closureChecklist")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[2].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[3].name",
                        is("scenarioReplayBoundary")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[3].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[4].name",
                        is("notProvenBoundaries")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[4].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.summary",
                        containsString("not an export/share/download packet")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.reviewerGuidance[0]",
                        containsString("in-response reviewer index")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.notProvenBoundaries[0]",
                        is("not replay proof")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.notProvenBoundaries[6]",
                        is("not production validation")))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].strategyId", is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$.results[0].chosenServerId", is("green")))
                .andExpect(jsonPath("$.results[0].reason", containsString("Chose green")))
                .andExpect(jsonPath("$.results[0].candidateServersConsidered[0]", is("green")))
                .andExpect(jsonPath("$.results[0].candidateServersConsidered[1]", is("blue")))
                .andExpect(jsonPath("$.results[0].scores.green").isNumber())
                .andExpect(jsonPath("$.results[0].scores.blue").isNumber())
                .andExpect(jsonPath("$.results[0].decisionVector.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionVector.localLabResponsePath", is("/api/routing/compare")))
                .andExpect(jsonPath("$.results[0].decisionVector.selectedStrategy", is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.results[0].decisionVector.selectedBackend", is("green")))
                .andExpect(jsonPath("$.results[0].decisionVector.candidateCount", is(2)))
                .andExpect(jsonPath("$.results[0].decisionVector.selectedCandidateVector.candidateId", is("green")))
                .andExpect(jsonPath("$.results[0].decisionVector.nonSelectedCandidateVectors[0].candidateId", is("blue")))
                .andExpect(jsonPath("$.results[0].dominantFactorAnalysis.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].dominantFactorAnalysis.status", is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].dominantFactorAnalysis.selectedDecisionAnalysis.candidateId",
                        is("green")))
                .andExpect(jsonPath("$.results[0].dominantFactorAnalysis.selectedDecisionAnalysis"
                        + ".largestPenaltyContributor.factorName").isString())
                .andExpect(jsonPath("$.results[0].decisionDeltaAnalysis.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionDeltaAnalysis.status", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionDeltaAnalysis.comparison.selectedCandidateId",
                        is("green")))
                .andExpect(jsonPath("$.results[0].decisionDeltaAnalysis.comparison.closestAlternativeCandidateId",
                        is("blue")))
                .andExpect(jsonPath("$.results[0].decisionDeltaAnalysis.comparison.finalScoreGap").isNumber())
                .andExpect(jsonPath("$.results[0].decisionDeltaAnalysis.largestAbsoluteFactorDelta.factorName")
                        .isString())
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.snapshotSchemaVersion",
                        is("decision-replay-snapshot/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.status", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.selectedCandidateId", is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.closestAlternativeCandidateId", is("blue")))
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.candidateIdsConsidered[0]", is("blue")))
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.candidateIdsConsidered[1]", is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.finalScoreGap").isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.snapshotFingerprint").isString())
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.traceSchemaVersion",
                        is("decision-replay-reconstruction-trace/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.status", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.selectedCandidateId",
                        is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.closestAlternativeCandidateId",
                        is("blue")))
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.candidateIdsConsidered[0]",
                        is("blue")))
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.candidateIdsConsidered[1]",
                        is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.candidateFinalScores.green")
                        .isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.finalScoreGap").isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.traceFingerprint").isString())
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.reconstructionSteps[0].stepId",
                        is("candidate-set-observed")))
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.reconstructionSteps[8].stepId",
                        is("replay-snapshot-fingerprint-observed")))
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.capsuleSchemaVersion",
                        is("decision-replay-capsule/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.status", is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.selectedCandidateId", is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.closestAlternativeCandidateId", is("blue")))
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.candidateIdsConsidered[0]", is("blue")))
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.candidateIdsConsidered[1]", is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.finalScoreGap").isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.linkedReplaySnapshotFingerprint").isString())
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.linkedReconstructionTraceFingerprint")
                        .isString())
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.capsuleFingerprint").isString())
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.candidateEvidence[0].candidateId",
                        is("blue")))
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.factorEvidence").isArray())
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.checklistSchemaVersion",
                        is("decision-replay-readiness-checklist/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.status", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.decisionVectorStatus",
                        is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.decisionReplayCapsuleStatus",
                        is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.selectedCandidateId",
                        is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.candidateCount",
                        is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.linkedReplaySnapshotFingerprint")
                        .isString())
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.linkedReconstructionTraceFingerprint")
                        .isString())
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.linkedReplayCapsuleFingerprint")
                        .isString())
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.checklistItems[0].itemId",
                        is("decision-vector-evidence")))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.checklistItems[8].itemId",
                        is("read-only-boundary-evidence")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.sourceMapSchemaVersion",
                        is("decision-replay-evidence-source-map/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.status", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.decisionVectorStatus",
                        is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.decisionReplayCapsuleStatus",
                        is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap"
                        + ".decisionReplayReadinessChecklistStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.selectedCandidateId",
                        is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.candidateCount",
                        is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.linkedReplaySnapshotFingerprint")
                        .isString())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap"
                        + ".linkedReconstructionTraceFingerprint").isString())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.linkedReplayCapsuleFingerprint")
                        .isString())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.sourceMapEntries[0].sourceId",
                        is("decision-vector-source")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.sourceMapEntries[7].sourceId",
                        is("linked-fingerprint-source")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.sourceMapEntries[8].sourceId",
                        is("read-only-boundary-source")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary"
                        + ".boundarySummarySchemaVersion",
                        is("decision-replay-evidence-boundary-summary/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.status",
                        is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.decisionVectorStatus",
                        is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary"
                        + ".decisionReplayEvidenceSourceMapStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.selectedCandidateId",
                        is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.candidateCount",
                        is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.boundaryItems[0]"
                        + ".boundaryId", is("lab-only-boundary")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.boundaryItems[8]"
                        + ".boundaryId", is("fingerprint-boundary")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.boundaryItems[9]"
                        + ".boundaryId", is("production-not-proven-boundary")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory"
                        + ".fieldInventorySchemaVersion",
                        is("decision-replay-evidence-field-inventory/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.status",
                        is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.decisionVectorStatus",
                        is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory"
                        + ".decisionReplayEvidenceBoundarySummaryStatus", is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.selectedCandidateId",
                        is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.candidateCount",
                        is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory"
                        + ".availableInventoryGroupCount", is(12)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.inventoryEntries[0]"
                        + ".inventoryId", is("decision-vector-fields")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.inventoryEntries[9]"
                        + ".inventoryId", is("linked-fingerprint-fields")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.inventoryEntries[11]"
                        + ".inventoryId", is("production-not-proven-boundary-fields")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary"
                        + ".nullSafetySchemaVersion",
                        is("decision-replay-evidence-null-safety-summary/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.status").isString())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.decisionVectorStatus",
                        is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary"
                        + ".decisionReplayEvidenceFieldInventoryStatus", is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.selectedCandidateId",
                        is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.candidateCount",
                        is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.nullSafetyItems[0]"
                        + ".nullSafetyId", is("selected-candidate-null-safety")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.nullSafetyItems[5]"
                        + ".nullSafetyId", is("linked-fingerprint-null-safety")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.nullSafetyItems[9]"
                        + ".nullSafetyId", is("no-healthy-path-null-safety")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.nullSafetyItems[11]"
                        + ".nullSafetyId", is("production-not-proven-null-safety")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup"
                        + ".statusRollupSchemaVersion",
                        is("decision-replay-evidence-status-rollup/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.status").isString())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.selectedCandidateId",
                        is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.candidateCount",
                        is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.availableLaneCount")
                        .isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.partialLaneCount")
                        .isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.unknownLaneCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.statusItems[0]"
                        + ".laneId", is("decision-vector-status")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.statusItems[10]"
                        + ".laneId", is("evidence-null-safety-status")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.statusItems[11]"
                        + ".laneId", is("read-only-boundary-status")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.statusItems[12]"
                        + ".laneId", is("production-not-proven-status")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary"
                        + ".laneNavigationSchemaVersion",
                        is("decision-replay-evidence-lane-navigation-summary/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary.status").isString())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary.selectedCandidateId",
                        is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary.candidateCount",
                        is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary.availableLaneCount")
                        .isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary.partialLaneCount")
                        .isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary.unknownLaneCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary"
                        + ".navigationItems[0].laneId", is("decision-vector-navigation")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary"
                        + ".navigationItems[0].responseFieldPath", is("results[].decisionVector")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary"
                        + ".navigationItems[0].uiSectionLabel", is("Decision Vector")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary"
                        + ".navigationItems[0].docsReferenceLabel", is("Enterprise Lab Decision Vector")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary"
                        + ".navigationItems[11].laneId", is("evidence-status-rollup-navigation")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".laneDependencyMapSchemaVersion",
                        is("decision-replay-evidence-lane-dependency-map/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap.status").isString())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap.selectedCandidateId",
                        is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap.candidateCount",
                        is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap.availableLaneCount")
                        .isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap.partialLaneCount")
                        .isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap.unknownLaneCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[0].laneId", is("decision-vector-dependency")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[0].responseFieldPath", is("results[].decisionVector")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[0].dependencyCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[0].downstreamCount", is(12)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[0].downstreamLaneIds[0]", is("dominant-factor-analysis")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[12].laneId", is("evidence-lane-navigation-dependency")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[12].dependencyCount", is(12)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[12].downstreamCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex.readOnly", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".laneReferenceIndexSchemaVersion",
                        is("decision-replay-evidence-lane-reference-index/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex.status").isString())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex.selectedCandidateId",
                        is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex.candidateCount",
                        is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex.availableLaneCount")
                        .isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex.partialLaneCount")
                        .isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex.unknownLaneCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[0].laneId", is("decision-vector-reference")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[0].responseFieldPath", is("results[].decisionVector")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[0].uiSectionLabel", is("Decision Vector")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[0].docsReferenceLabel", is("Enterprise Lab Decision Vector")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[0].dependencyCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[0].downstreamCount", is(12)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[13].laneId", is("evidence-lane-dependency-map-reference")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[13].dependencyCount", is(13)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[13].downstreamCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.readOnly",
                        is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary"
                        + ".laneDependencySummarySchemaVersion",
                        is("decision-replay-evidence-lane-dependency-summary/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.status",
                        is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.selectedCandidateId",
                        is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.candidateCount",
                        is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.totalLaneCount",
                        is(14)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.availableLaneCount")
                        .isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.partialLaneCount")
                        .isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.unknownLaneCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.rootLaneCount",
                        is(1)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.terminalLaneCount",
                        is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.maxDependencyCount",
                        is(13)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.maxDownstreamCount",
                        is(12)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary"
                        + ".densestDependencyLaneIds[0]", is("evidence-lane-dependency-map-reference")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary"
                        + ".widestDownstreamLaneIds[0]", is("decision-vector-reference")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.summaryText",
                        containsString("Reference index is PARTIAL with 14 lanes")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.limitations[0]")
                        .isString())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary.readOnly",
                        is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".laneConsistencySummarySchemaVersion",
                        is("decision-replay-evidence-lane-consistency-summary/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary.status",
                        is("CONSISTENT")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".referenceIndexStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".dependencySummaryStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".statusRollupStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".dependencyMapStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".selectedCandidateId", is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".candidateCount", is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".totalLaneCount", is(14)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".availableLaneCount").isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".partialLaneCount").isNumber())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".unknownLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".dependencyMapLaneCount", is(13)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".referenceIndexLaneCount", is(14)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".dependencySummaryLaneCount", is(14)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".mismatchedCountFields").isEmpty())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".missingSurfaces").isEmpty())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".consistencyChecks[0].name", is("status-rollup-present")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".consistencyChecks[4].name", is("lane-count-alignment")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary.summaryText",
                        containsString("Lane evidence surfaces are consistent")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary.limitations[0]")
                        .isString())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot.readOnly",
                        is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".reviewerSnapshotSchemaVersion",
                        is("decision-replay-evidence-reviewer-snapshot/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot.status",
                        is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".consistencyStatus", is("CONSISTENT")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".referenceIndexStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".dependencySummaryStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".statusRollupStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".dependencyMapStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".selectedCandidateId", is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".candidateCount", is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".totalLaneCount", is(14)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".unknownLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".checkedSurfaceCount", is(5)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".missingSurfaceCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".missingSurfaces").isEmpty())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".reviewerHighlights[0]", is("14 evidence lanes summarized")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".reviewerHighlights[5]", is("Consistency summary reports CONSISTENT")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot.summaryText",
                        containsString("Reviewer snapshot is PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot.limitations[0]",
                        containsString("Read-only reviewer snapshot metadata")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot.boundaryNote",
                        containsString("does not execute replay")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance.readOnly",
                        is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".reviewerGuidanceSchemaVersion",
                        is("decision-replay-evidence-reviewer-guidance/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance.status",
                        is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".reviewerPriority", is("REVIEW")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".selectedCandidateId", is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".candidateCount", is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".totalLaneCount", is(14)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".availableLaneCount", is(5)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".partialLaneCount", is(9)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".unknownLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".checkedSurfaceCount", is(6)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".missingSurfaceCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".evidenceSurfacesToInspect[5]", is("decisionReplayEvidenceReviewerSnapshot")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".suggestedReviewSteps[0]",
                        is("Inspect Decision Replay Evidence Reviewer Snapshot warnings and highlights.")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance.summaryText",
                        containsString("Reviewer guidance is PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance.limitations[0]",
                        containsString("Read-only reviewer guidance")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance.boundaryNote",
                        containsString("does not execute replay")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary.readOnly",
                        is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".reviewerHandoffSummarySchemaVersion",
                        is("decision-replay-evidence-reviewer-handoff-summary/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary.status",
                        is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".handoffPriority", is("REVIEW")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".reviewerSnapshotStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".reviewerGuidanceStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".consistencyStatus", is("CONSISTENT")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".selectedCandidateId", is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".candidateCount", is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".totalLaneCount", is(14)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".availableLaneCount", is(5)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".partialLaneCount", is(9)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".unknownLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".handoffBullets[0]", is("Reviewer snapshot is PARTIAL.")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".operatorFollowUpItems[0]",
                        is("Review partial or unknown evidence lanes before operator handoff.")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".evidenceSurfacesReferenced[6]", is("decisionReplayEvidenceReviewerGuidance")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary.summaryText",
                        containsString("Reviewer handoff summary is PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary.limitations[0]",
                        containsString("Read-only reviewer handoff metadata")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary.boundaryNote",
                        containsString("does not execute replay")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary.readOnly",
                        is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".reviewerClosureSummarySchemaVersion",
                        is("decision-replay-evidence-reviewer-closure-summary/v1")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary.status",
                        is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".closureDisposition", is("REVIEW_COMPLETE_WITH_LIMITATIONS")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".reviewerSnapshotStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".reviewerGuidanceStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".reviewerHandoffStatus", is("PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".consistencyStatus", is("CONSISTENT")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".selectedCandidateId", is("green")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".candidateCount", is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".totalLaneCount", is(14)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".availableLaneCount", is(5)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".partialLaneCount", is(9)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".unknownLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".closureBullets[0]", is("Reviewer snapshot is PARTIAL.")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".safeConclusions[0]",
                        is("Reviewer metadata was generated deterministically from exposed compare surfaces.")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".unresolvedBoundaries[0]", is("Not replay proof.")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".evidenceSurfacesReferenced[7]",
                        is("decisionReplayEvidenceReviewerHandoffSummary")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary.summaryText",
                        containsString("Reviewer closure summary is PARTIAL")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary.limitations[0]",
                        containsString("Read-only reviewer closure metadata")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary.boundaryNote",
                        containsString("does not execute replay")))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void routingAllUnhealthyResponseShapeIsStable() throws Exception {
        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategies": ["TAIL_LATENCY_POWER_OF_TWO"],
                                  "servers": [
                                    {
                                      "serverId": "green",
                                      "healthy": false,
                                      "inFlightRequestCount": 1,
                                      "averageLatencyMillis": 10.0,
                                      "p95LatencyMillis": 20.0,
                                      "p99LatencyMillis": 30.0,
                                      "recentErrorRate": 0.0
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount", is(1)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.disposition", is("UNKNOWN")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.resultCount", is(1)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.resultsWithClosureSummary",
                        is(1)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.resultsMissingClosureSummary",
                        is(0)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup"
                        + ".completeWithLimitationsCount", is(0)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.unknownCount", is(1)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.reviewerReady", is(false)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.notProvenBoundaries[0]",
                        is("not replay proof")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureRollup.notProvenBoundaries[6]",
                        is("not production validation")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.reviewerReady", is(false)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[0].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[2].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[3].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.items[4].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.notProvenBoundaries[0]",
                        is("not replay proof")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosureChecklist.notProvenBoundaries[6]",
                        is("not production validation")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.reviewerReady", is(false)))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.packetVersion", is("v1")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[0].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[1].status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[2].status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[3].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.sections[4].status",
                        is("PASS")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.notProvenBoundaries[0]",
                        is("not replay proof")))
                .andExpect(jsonPath("$.decisionReplayEvidenceReviewerClosurePacket.notProvenBoundaries[6]",
                        is("not production validation")))
                .andExpect(jsonPath("$.results[0].strategyId", is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$.results[0].chosenServerId", nullValue()))
                .andExpect(jsonPath("$.results[0].candidateServersConsidered").isEmpty())
                .andExpect(jsonPath("$.results[0].scores").isEmpty())
                .andExpect(jsonPath("$.results[0].decisionVector", nullValue()))
                .andExpect(jsonPath("$.results[0].dominantFactorAnalysis.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionDeltaAnalysis.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionDeltaAnalysis.factorDeltas").isEmpty())
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.selectedCandidateId", nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.candidateIdsConsidered").isEmpty())
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.closestAlternativeCandidateId", nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplaySnapshot.finalScoreGap", nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.selectedCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.candidateIdsConsidered")
                        .isEmpty())
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.candidateFinalScores")
                        .isEmpty())
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.closestAlternativeCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayReconstructionTrace.finalScoreGap", nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.selectedCandidateId", nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.candidateIdsConsidered").isEmpty())
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.candidateEvidence").isEmpty())
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.factorEvidence").isEmpty())
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.closestAlternativeCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayCapsule.finalScoreGap", nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.decisionVectorStatus",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.decisionReplayCapsuleStatus",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.selectedCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.candidateCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.linkedReplaySnapshotFingerprint",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.linkedReconstructionTraceFingerprint",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.linkedReplayCapsuleFingerprint",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayReadinessChecklist.checklistItems[6].status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.decisionVectorStatus",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.decisionReplayCapsuleStatus",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap"
                        + ".decisionReplayReadinessChecklistStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.selectedCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.candidateCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.linkedReplaySnapshotFingerprint",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap"
                        + ".linkedReconstructionTraceFingerprint", nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.linkedReplayCapsuleFingerprint",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.sourceMapEntries[0].status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.sourceMapEntries[7].status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceSourceMap.sourceMapEntries[8].status",
                        is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.decisionVectorStatus",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary"
                        + ".decisionReplayEvidenceSourceMapStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.selectedCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.candidateCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.boundaryItems[0]"
                        + ".boundaryId", is("lab-only-boundary")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.boundaryItems[8]"
                        + ".status", is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceBoundarySummary.boundaryItems[9]"
                        + ".status", is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.decisionVectorStatus",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory"
                        + ".decisionReplayEvidenceBoundarySummaryStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.selectedCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.candidateCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory"
                        + ".unknownInventoryGroupCount", is(10)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.inventoryEntries[0]"
                        + ".status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.inventoryEntries[9]"
                        + ".status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceFieldInventory.inventoryEntries[11]"
                        + ".inventoryId", is("production-not-proven-boundary-fields")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.decisionVectorStatus",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary"
                        + ".decisionReplayEvidenceFieldInventoryStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.selectedCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.candidateCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.nullSafetyItems[0]"
                        + ".status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.nullSafetyItems[1]"
                        + ".status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.nullSafetyItems[9]"
                        + ".nullSafetyId", is("no-healthy-path-null-safety")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.nullSafetyItems[9]"
                        + ".status", is("AVAILABLE")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceNullSafetySummary.nullSafetyItems[11]"
                        + ".nullSafetyId", is("production-not-proven-null-safety")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.selectedCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.candidateCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.statusItems[0]"
                        + ".laneId", is("decision-vector-status")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.statusItems[0]"
                        + ".status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.statusItems[10]"
                        + ".laneId", is("evidence-null-safety-status")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.statusItems[10]"
                        + ".status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.statusItems[12]"
                        + ".laneId", is("production-not-proven-status")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceStatusRollup.statusItems[12]"
                        + ".boundaryPresent", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary.status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary.selectedCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary.candidateCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary"
                        + ".navigationItems[0].laneId", is("decision-vector-navigation")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary"
                        + ".navigationItems[0].status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary"
                        + ".navigationItems[11].laneId", is("evidence-status-rollup-navigation")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary"
                        + ".navigationItems[11].status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneNavigationSummary"
                        + ".navigationItems[11].boundaryPresent", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap.status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap.selectedCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap.candidateCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[0].laneId", is("decision-vector-dependency")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[0].status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[0].dependencyCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[0].downstreamCount", is(12)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[12].laneId", is("evidence-lane-navigation-dependency")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[12].status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[12].dependencyCount", is(12)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[12].downstreamCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencyMap"
                        + ".dependencyItems[12].boundaryPresent", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex.status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex.selectedCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex.candidateCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[0].laneId", is("decision-vector-reference")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[0].status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[0].dependencyCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[0].downstreamCount", is(12)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[13].laneId", is("evidence-lane-dependency-map-reference")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[13].status", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[13].dependencyCount", is(13)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[13].downstreamCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneReferenceIndex"
                        + ".referenceItems[13].boundaryPresent", is(true)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.selectedCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.candidateCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.totalLaneCount",
                        is(14)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.rootLaneCount",
                        is(1)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary.terminalLaneCount",
                        is(2)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary"
                        + ".densestDependencyLaneIds[0]", is("evidence-lane-dependency-map-reference")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneDependencySummary"
                        + ".widestDownstreamLaneIds[0]", is("decision-vector-reference")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary.status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary.selectedCandidateId",
                        nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary.candidateCount",
                        is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".referenceIndexStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".dependencySummaryStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".missingSurfaces").isEmpty())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary"
                        + ".mismatchedCountFields").isEmpty())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceLaneConsistencySummary.summaryText",
                        containsString("UNKNOWN because required lane metadata is missing or unavailable")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot.status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".consistencyStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".selectedCandidateId", nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".candidateCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".totalLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".availableLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".partialLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".unknownLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".checkedSurfaceCount", is(5)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".missingSurfaceCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot"
                        + ".reviewerWarnings[0]",
                        is("No selected candidate evidence is available for reviewer snapshot.")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerSnapshot.summaryText",
                        containsString("UNKNOWN because required reviewer evidence surfaces are missing")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance.status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".reviewerPriority", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".selectedCandidateId", nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".candidateCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".totalLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".availableLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".partialLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".unknownLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".checkedSurfaceCount", is(6)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".missingSurfaceCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance"
                        + ".cautionNotes[0]",
                        is("No selected candidate evidence is available for reviewer guidance.")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerGuidance.summaryText",
                        containsString("UNKNOWN because required reviewer metadata is missing")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary.status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".handoffPriority", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".reviewerSnapshotStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".reviewerGuidanceStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".consistencyStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".selectedCandidateId", nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".candidateCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".totalLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".availableLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".partialLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".unknownLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary"
                        + ".cautionNotes[0]",
                        is("No selected candidate evidence is available for reviewer handoff.")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerHandoffSummary.summaryText",
                        containsString("UNKNOWN because required reviewer metadata is missing")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary.status",
                        is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".closureDisposition", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".reviewerSnapshotStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".reviewerGuidanceStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".reviewerHandoffStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".consistencyStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".selectedCandidateId", nullValue()))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".candidateCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".totalLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".availableLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".partialLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".unknownLaneCount", is(0)))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".closureBullets[1]", is("Reviewer closure summary status is UNKNOWN")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".safeConclusions").isEmpty())
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary"
                        + ".unresolvedBoundaries[0]", is("Not replay proof.")))
                .andExpect(jsonPath("$.results[0].decisionReplayEvidenceReviewerClosureSummary.summaryText",
                        containsString("UNKNOWN because required reviewer metadata is missing")))
                .andExpect(jsonPath("$.results[0].reason", containsString("No healthy eligible servers")));
    }

    @Test
    void invalidRequestErrorShapeIsStableAcrossApiContracts() throws Exception {
        mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedLoad": 10.0,
                                  "strategy": "CAPACITY_AWARE",
                                  "priority": "gold",
                                  "servers": [
                                    {
                                      "id": "api-1",
                                      "cpuUsage": 10.0,
                                      "memoryUsage": 20.0,
                                      "diskUsage": 20.0,
                                      "capacity": 100.0,
                                      "weight": 1.0,
                                      "healthy": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("priority must be one of")))
                .andExpect(jsonPath("$.path", is("/api/allocate/evaluate")))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
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

    private static void assertPathRequestSchemaAndOkResponse(JsonNode docs, String path, String requestSchema) {
        String encodedPath = path.replace("/", "~1");
        JsonNode operation = required(docs, "/paths/" + encodedPath + "/post");
        JsonNode requestContent = required(operation, "/requestBody/content/application~1json/schema");
        assertRef(requestContent, "#/components/schemas/" + requestSchema);

        required(operation, "/responses/200");
    }

    private static void assertSchemaProperties(JsonNode docs, String schemaName, String... properties) {
        JsonNode schemaProperties = required(docs, "/components/schemas/" + schemaName + "/properties");
        for (String property : properties) {
            assertFalse(schemaProperties.path(property).isMissingNode(),
                    () -> schemaName + " should expose property " + property);
        }
    }

    private static void assertSchemaRequired(JsonNode docs, String schemaName, String... properties) {
        JsonNode requiredProperties = required(docs, "/components/schemas/" + schemaName + "/required");
        for (String property : properties) {
            boolean present = false;
            for (JsonNode requiredProperty : requiredProperties) {
                if (property.equals(requiredProperty.asText())) {
                    present = true;
                    break;
                }
            }
            assertTrue(present, () -> schemaName + " should require property " + property);
        }
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
