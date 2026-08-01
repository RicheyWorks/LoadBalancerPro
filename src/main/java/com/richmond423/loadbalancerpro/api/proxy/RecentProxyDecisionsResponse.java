package com.richmond423.loadbalancerpro.api.proxy;

import java.util.List;
import java.util.Objects;

/** Read-only snapshot of the bounded process-local live decision buffer. */
public record RecentProxyDecisionsResponse(
        boolean proxyEnabled,
        String retentionScope,
        int maxRetained,
        int retainedCount,
        long totalCaptured,
        long totalDropped,
        List<LiveRoutingDecisionRecord> decisions) {

    public static final String PROCESS_LOCAL = "process-local";

    public RecentProxyDecisionsResponse {
        retentionScope = Objects.requireNonNull(retentionScope, "retentionScope cannot be null");
        decisions = List.copyOf(Objects.requireNonNull(decisions, "decisions cannot be null"));
    }

    static RecentProxyDecisionsResponse empty(boolean proxyEnabled, int maxRetained) {
        return new RecentProxyDecisionsResponse(
                proxyEnabled, PROCESS_LOCAL, maxRetained, 0, 0, 0, List.of());
    }
}
