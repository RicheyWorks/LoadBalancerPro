package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record RemediationPlan(
        String status,
        String generatedFrom,
        boolean advisoryOnly,
        boolean readOnly,
        boolean cloudMutation,
        List<RemediationRecommendation> recommendations) {

    public RemediationPlan {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
