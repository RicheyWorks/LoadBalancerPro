package com.richmond423.loadbalancerpro.api.explain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.richmond423.loadbalancerpro.api.proxy.LiveRoutingDecisionRecord;

/** Read-only explanation of one retained, actual proxy forwarding attempt. */
public record LiveRoutingDecisionExplanation(
        boolean readOnly,
        boolean retainedLiveProxyAttempt,
        String retentionScope,
        String contractVersion,
        String decisionId,
        Instant capturedAt,
        long configurationGeneration,
        String routeName,
        String strategyId,
        int attempt,
        String selectionSource,
        String selectedCandidateId,
        List<String> exactCandidateIds,
        List<LiveRoutingDecisionRecord.CandidateState> candidateObservations,
        List<String> consideredCandidateIds,
        int responseStatus,
        double latencyMillis,
        boolean retriable,
        String outcome,
        String selectionEvidenceStatus,
        String strategyReason,
        String scorePreference,
        RoutingExplanation analysis,
        List<DecisionChangeThreshold> decisionChangeThresholds,
        String boundaryNote) {

    public static final String CONTRACT_VERSION = "v1";
    public static final String PROCESS_LOCAL = "process-local";

    public LiveRoutingDecisionExplanation {
        readOnly = true;
        retainedLiveProxyAttempt = true;
        retentionScope = PROCESS_LOCAL;
        contractVersion = CONTRACT_VERSION;
        Objects.requireNonNull(capturedAt, "capturedAt cannot be null");
        exactCandidateIds = List.copyOf(exactCandidateIds);
        candidateObservations = List.copyOf(candidateObservations);
        consideredCandidateIds = List.copyOf(consideredCandidateIds);
        Objects.requireNonNull(analysis, "analysis cannot be null");
        decisionChangeThresholds = List.copyOf(decisionChangeThresholds);
    }

    /**
     * Additive contribution change that reaches a selected-versus-alternative score tie.
     * Crossing the tie in the same direction changes the ordering for additive score models;
     * this row does not rerun stateful, positional, keyed, or affinity selection.
     */
    public record DecisionChangeThreshold(
            String factorName,
            String closestAlternativeCandidateId,
            String scorePreference,
            Double selectedContributionChangeToTie,
            Double alternativeContributionChangeToTie,
            Double sharedFactorWeightShiftPercentToTie,
            String boundaryNote) {
    }
}
