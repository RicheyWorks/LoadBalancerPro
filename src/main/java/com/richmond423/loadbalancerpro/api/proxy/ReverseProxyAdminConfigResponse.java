package com.richmond423.loadbalancerpro.api.proxy;

import java.util.List;

public record ReverseProxyAdminConfigResponse(
        long generation,
        int routeCount,
        int backendTargetCount,
        List<RouteConfig> routes,
        List<String> drainingUpstreamIds) {

    public record RouteConfig(
            String name,
            String strategy,
            List<UpstreamConfig> upstreams) {
    }

    public record UpstreamConfig(
            String id,
            boolean healthy,
            double weight,
            int maxInFlight,
            boolean draining) {
    }
}
