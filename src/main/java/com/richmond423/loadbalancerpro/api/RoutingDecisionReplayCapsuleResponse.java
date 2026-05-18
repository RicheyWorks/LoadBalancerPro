package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RoutingDecisionReplayCapsuleResponse(
        boolean readOnly,
        String capsuleSchemaVersion,
        String source,
        String status,
        String capsuleFingerprint,
        String fingerprintAlgorithm,
        String selectedCandidateId,
        List<String> candidateIdsConsidered,
        int candidateCount,
        String closestAlternativeCandidateId,
        Double finalScoreGap,
        String largestDeltaFactorName,
        String linkedReplaySnapshotFingerprint,
        String linkedReconstructionTraceFingerprint,
        String strategyId,
        String decisionVectorStatus,
        String factorContributionStatus,
        String dominantFactorAnalysisStatus,
        String decisionDeltaAnalysisStatus,
        String decisionReplaySnapshotStatus,
        String decisionReplayReconstructionTraceStatus,
        List<String> reconstructionStepIds,
        List<DecisionReplayCapsuleCandidateEvidenceResponse> candidateEvidence,
        List<DecisionReplayCapsuleFactorEvidenceResponse> factorEvidence,
        String explanation,
        String boundaryNote,
        String productionNotProvenBoundary) {

    public RoutingDecisionReplayCapsuleResponse {
        candidateIdsConsidered = candidateIdsConsidered == null ? List.of() : List.copyOf(candidateIdsConsidered);
        reconstructionStepIds = reconstructionStepIds == null ? List.of() : List.copyOf(reconstructionStepIds);
        candidateEvidence = candidateEvidence == null ? List.of() : List.copyOf(candidateEvidence);
        factorEvidence = factorEvidence == null ? List.of() : List.copyOf(factorEvidence);
    }
}
