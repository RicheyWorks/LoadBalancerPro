package com.richmond423.loadbalancerpro.api;

public record RemediationRecommendation(
        int rank,
        RemediationAction action,
        RemediationPriority priority,
        RemediationReason reason,
        Integer serverCount,
        Double loadAmount,
        boolean executable,
        String message) {
}
