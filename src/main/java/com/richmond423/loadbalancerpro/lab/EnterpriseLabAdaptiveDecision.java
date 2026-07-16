package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveTrafficDecisionRecord;

import java.util.List;
import java.util.Objects;

public record EnterpriseLabAdaptiveDecision(
        String contractVersion,
        String scenarioId,
        String fixtureVersion,
        String observationModel,
        AdaptiveTrafficDecisionRecord decision,
        String contentFingerprint,
        boolean trafficActionPerformed,
        List<String> safetyNotes) {

    public EnterpriseLabAdaptiveDecision {
        contractVersion = requireNonBlank(contractVersion, "contractVersion");
        scenarioId = requireNonBlank(scenarioId, "scenarioId");
        fixtureVersion = requireNonBlank(fixtureVersion, "fixtureVersion");
        observationModel = requireNonBlank(observationModel, "observationModel");
        decision = Objects.requireNonNull(decision, "decision cannot be null");
        contentFingerprint = requireNonBlank(contentFingerprint, "contentFingerprint");
        safetyNotes = List.copyOf(Objects.requireNonNull(safetyNotes, "safetyNotes cannot be null"));
        if (safetyNotes.isEmpty()) {
            throw new IllegalArgumentException("safetyNotes cannot be empty");
        }
        if (!decision.contextId().equals("enterprise-lab:" + scenarioId)) {
            throw new IllegalArgumentException("decision context must match the Enterprise Lab scenario");
        }
        if (!contentFingerprint.equals(decision.contentFingerprint())) {
            throw new IllegalArgumentException("contentFingerprint must match the decision record");
        }
        if (trafficActionPerformed || decision.trafficActionPerformed()) {
            throw new IllegalArgumentException("Enterprise Lab decisions cannot perform traffic actions");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
