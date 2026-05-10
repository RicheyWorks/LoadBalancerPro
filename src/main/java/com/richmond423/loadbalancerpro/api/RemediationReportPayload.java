package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RemediationReportPayload(
        String reportId,
        String title,
        String sourceType,
        String status,
        String summary,
        double acceptedLoad,
        double rejectedLoad,
        double unallocatedLoad,
        int recommendedAdditionalServers,
        ScalingSimulationResult scalingSimulation,
        LoadSheddingEvaluation loadShedding,
        RemediationPlan remediationPlan,
        boolean readOnly,
        boolean advisoryOnly,
        boolean cloudMutation,
        List<String> warnings,
        List<String> limitations,
        List<RemediationReportStep> steps) {

    public RemediationReportPayload {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
