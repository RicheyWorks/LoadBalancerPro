package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentResult;

import java.time.Instant;
import java.util.List;

public record EnterpriseLabRun(
        String runId,
        Instant createdAt,
        String mode,
        boolean activeInfluenceEnabled,
        List<String> selectedScenarioIds,
        List<AdaptiveRoutingExperimentResult> results,
        EnterpriseLabScorecard scorecard,
        List<String> safetyNotes,
        String storageMode,
        int maxRetainedRuns,
        int maxScenariosPerRun) {

    public EnterpriseLabRun {
        runId = requireNonBlank(runId, "runId");
        createdAt = createdAt == null ? Instant.EPOCH : createdAt;
        mode = requireNonBlank(mode, "mode");
        selectedScenarioIds = List.copyOf(selectedScenarioIds == null ? List.of() : selectedScenarioIds);
        results = List.copyOf(results == null ? List.of() : results);
        safetyNotes = List.copyOf(safetyNotes == null ? List.of() : safetyNotes);
        storageMode = requireNonBlank(storageMode, "storageMode");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}

