package com.richmond423.loadbalancerpro.api.proxy;

import java.util.List;

public record ReverseProxyReloadResponse(
        boolean success,
        String status,
        long activeConfigGeneration,
        int activeRouteCount,
        int activeBackendTargetCount,
        List<String> validationErrors,
        ReverseProxyStatusResponse.ReloadStatus reload) {
}
