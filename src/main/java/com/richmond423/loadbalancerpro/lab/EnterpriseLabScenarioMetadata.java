package com.richmond423.loadbalancerpro.lab;

import java.util.List;

public record EnterpriseLabScenarioMetadata(
        String scenarioId,
        String displayName,
        String category,
        String description,
        List<String> signalsInvolved,
        List<String> expectedGuardrails,
        List<String> supportedModes,
        boolean safeForInfluenceExperiment,
        String deterministicFixtureVersion,
        String strategy,
        double requestedLoad,
        int serverCount,
        int replayEventCount) {

    public EnterpriseLabScenarioMetadata {
        scenarioId = requireNonBlank(scenarioId, "scenarioId");
        displayName = requireNonBlank(displayName, "displayName");
        category = requireNonBlank(category, "category");
        description = requireNonBlank(description, "description");
        signalsInvolved = List.copyOf(signalsInvolved == null ? List.of() : signalsInvolved);
        expectedGuardrails = List.copyOf(expectedGuardrails == null ? List.of() : expectedGuardrails);
        supportedModes = List.copyOf(supportedModes == null ? List.of() : supportedModes);
        deterministicFixtureVersion = requireNonBlank(deterministicFixtureVersion, "deterministicFixtureVersion");
        strategy = requireNonBlank(strategy, "strategy");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}

