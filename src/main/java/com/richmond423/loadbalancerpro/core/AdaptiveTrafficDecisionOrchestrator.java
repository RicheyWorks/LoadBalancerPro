package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class AdaptiveTrafficDecisionOrchestrator {
    public static final String DECISION_SCHEMA_VERSION = "adaptive-traffic-decision/v1";

    private final ServerScoreCalculator scoreCalculator;
    private final AdaptiveRoutingPolicyEngine policyEngine;

    public AdaptiveTrafficDecisionOrchestrator() {
        this(new ServerScoreCalculator(), new AdaptiveRoutingPolicyEngine());
    }

    AdaptiveTrafficDecisionOrchestrator(
            ServerScoreCalculator scoreCalculator,
            AdaptiveRoutingPolicyEngine policyEngine) {
        this.scoreCalculator = Objects.requireNonNull(scoreCalculator, "scoreCalculator cannot be null");
        this.policyEngine = Objects.requireNonNull(policyEngine, "policyEngine cannot be null");
    }

    public AdaptiveTrafficDecisionRecord decide(
            AdaptiveTrafficDecisionRequest request,
            AdaptiveTrafficDecisionPolicy policy) {
        Objects.requireNonNull(request, "request cannot be null");
        Objects.requireNonNull(policy, "policy cannot be null");
        if (request.candidates().size() > policy.maximumCandidateCount()) {
            throw new IllegalArgumentException("candidate count exceeds configured maximumCandidateCount");
        }

        Map<String, List<ServerObservation>> observations = new TreeMap<>();
        Map<String, ServerRollingSignalState> rollingStates = new TreeMap<>();
        Map<String, ServerStateVector> stateVectors = new TreeMap<>();
        Map<String, ServerScoreBreakdown> scoreBreakdowns = new TreeMap<>();
        for (AdaptiveTrafficDecisionCandidate candidate : request.candidates()) {
            if (candidate.observations().size() > policy.observationWindowPolicy().maxSampleCount()) {
                throw new IllegalArgumentException(
                        "candidate observation count exceeds configured maxSampleCount");
            }
            ServerObservationWindow window = ServerObservationWindow
                    .create(candidate.serverId(), policy.observationWindowPolicy())
                    .appendAll(candidate.observations(), request.evaluatedAt());
            ServerRollingSignalState rollingState = window.snapshot(request.evaluatedAt());
            ServerStateVector stateVector = ServerStateVector.fromObservationState(
                    candidate.toServer(),
                    rollingState,
                    candidate.inFlightRequestCount(),
                    candidate.estimatedConcurrencyLimit(),
                    candidate.queueDepth());
            ServerScoreBreakdown scoreBreakdown = scoreCalculator.recommendationScoreBreakdown(
                    stateVector, rollingState, policy.scorePolicy());
            observations.put(candidate.serverId(), candidate.observations());
            rollingStates.put(candidate.serverId(), rollingState);
            stateVectors.put(candidate.serverId(), stateVector);
            scoreBreakdowns.put(candidate.serverId(), scoreBreakdown);
        }

        TrafficAllocationRecommendation allocationRecommendation = LoadDistributionPlanner.recommendTrafficShares(
                List.copyOf(stateVectors.values()),
                scoreBreakdowns,
                request.baselineAllocations(),
                policy.allocationPolicy());
        boolean signalsFresh = rollingStates.values().stream()
                .noneMatch(state -> state.stale() || state.missing());
        boolean evidenceSufficient = rollingStates.values().stream()
                .allMatch(ServerRollingSignalState::sufficientEvidence);
        TrafficAllocationGuardrailDecision guardrailDecision = policyEngine.evaluateAllocation(
                new TrafficAllocationGuardrailInput(
                        request.contextId(),
                        request.mode(),
                        request.baselineAllocations(),
                        allocationRecommendation,
                        signalsFresh,
                        evidenceSufficient,
                        request.conflictingSignals(),
                        request.cooldownActive(),
                        request.explicitExperimentContext(),
                        request.operatorStopRequested()),
                policy.guardrailPolicy());

        List<String> reasons = reasons(rollingStates, allocationRecommendation, guardrailDecision);
        return new AdaptiveTrafficDecisionRecord(
                DECISION_SCHEMA_VERSION,
                request.decisionId(),
                request.contextId(),
                request.mode(),
                request.evaluatedAt(),
                request,
                policy,
                observations,
                rollingStates,
                stateVectors,
                scoreBreakdowns,
                allocationRecommendation,
                guardrailDecision,
                reasons);
    }

    private static List<String> reasons(
            Map<String, ServerRollingSignalState> rollingStates,
            TrafficAllocationRecommendation allocationRecommendation,
            TrafficAllocationGuardrailDecision guardrailDecision) {
        List<String> reasons = new ArrayList<>();
        reasons.add("composed bounded evidence for " + rollingStates.size() + " candidates");
        rollingStates.forEach((serverId, state) -> state.reasons().forEach(
                reason -> reasons.add("state " + serverId + ": " + reason)));
        allocationRecommendation.reasons().forEach(reason -> reasons.add("allocation: " + reason));
        guardrailDecision.reasons().forEach(reason -> reasons.add("guardrail: " + reason));
        reasons.add("decision record only; no traffic action performed");
        return List.copyOf(reasons);
    }
}
