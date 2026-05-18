package com.richmond423.loadbalancerpro.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public record RoutingDecisionReplayReconstructionTraceResponse(
        boolean readOnly,
        String traceSchemaVersion,
        String source,
        String status,
        String traceFingerprint,
        String fingerprintAlgorithm,
        String snapshotFingerprint,
        String selectedCandidateId,
        List<String> candidateIdsConsidered,
        int candidateCount,
        Map<String, Double> candidateFinalScores,
        String strategyId,
        String decisionVectorStatus,
        String factorContributionStatus,
        String dominantFactorAnalysisStatus,
        String decisionDeltaAnalysisStatus,
        String decisionReplaySnapshotStatus,
        String closestAlternativeCandidateId,
        Double finalScoreGap,
        String largestDeltaFactorName,
        List<DecisionReplayReconstructionStepResponse> reconstructionSteps,
        String explanation,
        String boundaryNote,
        String productionNotProvenBoundary) {

    public RoutingDecisionReplayReconstructionTraceResponse {
        candidateIdsConsidered = List.copyOf(candidateIdsConsidered);
        candidateFinalScores = Collections.unmodifiableMap(new LinkedHashMap<>(candidateFinalScores));
        reconstructionSteps = List.copyOf(reconstructionSteps);
    }
}
