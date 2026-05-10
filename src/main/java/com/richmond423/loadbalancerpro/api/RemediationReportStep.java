package com.richmond423.loadbalancerpro.api;

public record RemediationReportStep(
        String stepId,
        String type,
        String status,
        double acceptedLoad,
        double rejectedLoad,
        double unallocatedLoad,
        int recommendedAdditionalServers,
        String selectedServerId,
        String loadSheddingAction,
        String reason) {
}
