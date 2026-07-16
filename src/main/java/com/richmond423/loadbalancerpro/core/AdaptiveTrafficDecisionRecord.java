package com.richmond423.loadbalancerpro.core;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record AdaptiveTrafficDecisionRecord(
        String schemaVersion,
        String decisionId,
        String contextId,
        AdaptiveRoutingPolicyMode mode,
        Instant evaluatedAt,
        AdaptiveTrafficDecisionRequest request,
        AdaptiveTrafficDecisionPolicy policy,
        Map<String, List<ServerObservation>> observations,
        Map<String, ServerRollingSignalState> rollingStates,
        Map<String, ServerStateVector> stateVectors,
        Map<String, ServerScoreBreakdown> scoreBreakdowns,
        TrafficAllocationRecommendation allocationRecommendation,
        TrafficAllocationGuardrailDecision guardrailDecision,
        List<String> reasons) {

    public AdaptiveTrafficDecisionRecord {
        schemaVersion = requireNonBlank(schemaVersion, "schemaVersion");
        decisionId = requireNonBlank(decisionId, "decisionId");
        contextId = requireNonBlank(contextId, "contextId");
        mode = Objects.requireNonNull(mode, "mode cannot be null");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt cannot be null");
        request = Objects.requireNonNull(request, "request cannot be null");
        policy = Objects.requireNonNull(policy, "policy cannot be null");
        observations = immutableSortedObservationLists(observations);
        rollingStates = immutableSorted(rollingStates, "rollingStates");
        stateVectors = immutableSorted(stateVectors, "stateVectors");
        scoreBreakdowns = immutableSorted(scoreBreakdowns, "scoreBreakdowns");
        allocationRecommendation = Objects.requireNonNull(
                allocationRecommendation, "allocationRecommendation cannot be null");
        guardrailDecision = Objects.requireNonNull(guardrailDecision, "guardrailDecision cannot be null");
        reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons cannot be null"));
        if (reasons.isEmpty()) {
            throw new IllegalArgumentException("reasons cannot be empty");
        }
        if (!observations.keySet().equals(rollingStates.keySet())
                || !rollingStates.keySet().equals(stateVectors.keySet())
                || !rollingStates.keySet().equals(scoreBreakdowns.keySet())) {
            throw new IllegalArgumentException("observation, state, vector, and score serverIds must match");
        }
        for (String serverId : rollingStates.keySet()) {
            if (observations.get(serverId).stream()
                    .anyMatch(observation -> !serverId.equals(observation.serverId()))) {
                throw new IllegalArgumentException("decision observations must match map serverIds");
            }
            if (!serverId.equals(rollingStates.get(serverId).serverId())
                    || !serverId.equals(stateVectors.get(serverId).serverId())
                    || !serverId.equals(scoreBreakdowns.get(serverId).serverId())) {
                throw new IllegalArgumentException("decision maps must match nested serverIds");
            }
            if (!evaluatedAt.equals(rollingStates.get(serverId).evaluatedAt())
                    || !evaluatedAt.equals(stateVectors.get(serverId).timestamp())) {
                throw new IllegalArgumentException("decision evidence timestamps must match evaluatedAt");
            }
        }
        if (!contextId.equals(guardrailDecision.contextId()) || mode != guardrailDecision.mode()) {
            throw new IllegalArgumentException("guardrail decision must match decision context and mode");
        }
        if (!decisionId.equals(request.decisionId())
                || !contextId.equals(request.contextId())
                || mode != request.mode()
                || !evaluatedAt.equals(request.evaluatedAt())) {
            throw new IllegalArgumentException("request identity must match decision identity");
        }
        Map<String, List<ServerObservation>> requestObservations = new TreeMap<>();
        request.candidates().forEach(candidate ->
                requestObservations.put(candidate.serverId(), candidate.observations()));
        if (!observations.equals(requestObservations)) {
            throw new IllegalArgumentException("recorded observations must match request candidates");
        }
        if (!request.baselineAllocations().equals(guardrailDecision.baselineAllocations())) {
            throw new IllegalArgumentException("guardrail baseline must match request baseline");
        }
        if (!allocationRecommendation.allocations().equals(guardrailDecision.requestedAllocations())) {
            throw new IllegalArgumentException("guardrail request must match allocation recommendation");
        }
    }

    public Map<String, Double> effectiveAllocations() {
        return guardrailDecision.effectiveAllocations();
    }

    public boolean trafficActionPerformed() {
        return false;
    }

    public String contentFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateDigest(digest, schemaVersion);
            updateDigest(digest, decisionId);
            updateDigest(digest, contextId);
            updateDigest(digest, mode.wireValue());
            updateDigest(digest, evaluatedAt.toString());
            updateDigest(digest, request.toString());
            updateDigest(digest, policy.toString());
            updateDigest(digest, observations.toString());
            updateDigest(digest, rollingStates.toString());
            updateDigest(digest, stateVectors.toString());
            updateDigest(digest, scoreBreakdowns.toString());
            updateDigest(digest, allocationRecommendation.toString());
            updateDigest(digest, guardrailDecision.toString());
            updateDigest(digest, reasons.toString());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void updateDigest(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static <T> Map<String, T> immutableSorted(Map<String, T> values, String fieldName) {
        Objects.requireNonNull(values, fieldName + " cannot be null");
        if (values.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        Map<String, T> sorted = new TreeMap<>();
        for (Map.Entry<String, T> entry : values.entrySet()) {
            String serverId = requireCanonicalId(entry.getKey(), fieldName + " serverId");
            T value = Objects.requireNonNull(entry.getValue(), fieldName + " cannot contain null values");
            sorted.put(serverId, value);
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    private static Map<String, List<ServerObservation>> immutableSortedObservationLists(
            Map<String, List<ServerObservation>> values) {
        Objects.requireNonNull(values, "observations cannot be null");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("observations cannot be empty");
        }
        Map<String, List<ServerObservation>> sorted = new TreeMap<>();
        for (Map.Entry<String, List<ServerObservation>> entry : values.entrySet()) {
            String serverId = requireCanonicalId(entry.getKey(), "observations serverId");
            List<ServerObservation> serverObservations = List.copyOf(
                    Objects.requireNonNull(entry.getValue(), "observations cannot contain null lists"));
            sorted.put(serverId, serverObservations);
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static String requireCanonicalId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not have surrounding whitespace");
        }
        return value;
    }
}
