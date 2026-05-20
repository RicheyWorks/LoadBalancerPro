package com.richmond423.loadbalancerpro.api;

import java.util.List;
import java.util.Map;

public record RoutingComparisonResultResponse(
        String strategyId,
        String status,
        String chosenServerId,
        String reason,
        List<String> candidateServersConsidered,
        Map<String, Double> scores,
        RoutingDecisionVectorResponse decisionVector,
        DominantFactorAnalysisResponse dominantFactorAnalysis,
        RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
        RoutingDecisionReplaySnapshotResponse decisionReplaySnapshot,
        RoutingDecisionReplayReconstructionTraceResponse decisionReplayReconstructionTrace,
        RoutingDecisionReplayCapsuleResponse decisionReplayCapsule,
        RoutingDecisionReplayReadinessChecklistResponse decisionReplayReadinessChecklist,
        RoutingDecisionReplayEvidenceSourceMapResponse decisionReplayEvidenceSourceMap,
        RoutingDecisionReplayEvidenceBoundarySummaryResponse decisionReplayEvidenceBoundarySummary,
        RoutingDecisionReplayEvidenceFieldInventoryResponse decisionReplayEvidenceFieldInventory,
        RoutingDecisionReplayEvidenceNullSafetySummaryResponse decisionReplayEvidenceNullSafetySummary,
        RoutingDecisionReplayEvidenceStatusRollupResponse decisionReplayEvidenceStatusRollup,
        RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse decisionReplayEvidenceLaneNavigationSummary,
        RoutingDecisionReplayEvidenceLaneDependencyMapResponse decisionReplayEvidenceLaneDependencyMap,
        RoutingDecisionReplayEvidenceLaneReferenceIndexResponse decisionReplayEvidenceLaneReferenceIndex,
        RoutingDecisionReplayEvidenceLaneDependencySummaryResponse decisionReplayEvidenceLaneDependencySummary,
        RoutingDecisionReplayEvidenceLaneConsistencySummaryResponse decisionReplayEvidenceLaneConsistencySummary,
        RoutingDecisionReplayEvidenceReviewerSnapshotResponse decisionReplayEvidenceReviewerSnapshot,
        RoutingDecisionReplayEvidenceReviewerGuidanceResponse decisionReplayEvidenceReviewerGuidance,
        RoutingDecisionReplayEvidenceReviewerHandoffSummaryResponse decisionReplayEvidenceReviewerHandoffSummary,
        RoutingDecisionReplayEvidenceReviewerClosureSummaryResponse decisionReplayEvidenceReviewerClosureSummary) {
}
