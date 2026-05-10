package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record AllocationEvaluationMetricsPreview(
        String strategy,
        int evaluatedHealthyServerCount,
        double acceptedLoad,
        double rejectedLoad,
        double unallocatedLoad,
        int recommendedAdditionalServers,
        List<String> metricNames,
        boolean emitted) {
}
