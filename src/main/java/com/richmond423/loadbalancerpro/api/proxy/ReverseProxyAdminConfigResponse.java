package com.richmond423.loadbalancerpro.api.proxy;

import java.util.List;

public record ReverseProxyAdminConfigResponse(
        long generation,
        int routeCount,
        int backendTargetCount,
        int effectiveBackendTargetCount,
        List<RouteConfig> routes,
        List<ReverseProxyStatusResponse.DnsDiscoveryStatus> dnsDiscovery,
        List<String> drainingUpstreamIds) {

    public record RouteConfig(
            String name,
            String pathPrefix,
            String hostMatch,
            List<String> headerMatchNames,
            List<SplitConfig> splits,
            String strategy,
            List<UpstreamConfig> upstreams) {

        public RouteConfig(
                String name,
                String strategy,
                List<UpstreamConfig> upstreams) {
            this(name, "/", null, List.of(), List.of(), strategy, upstreams);
        }
    }

    public record SplitConfig(
            String name,
            int percentage,
            List<String> targetIds) {
    }

    public record UpstreamConfig(
            String id,
            boolean healthy,
            double weight,
            int maxInFlight,
            boolean draining,
            String discovery,
            String discoveryAuthority,
            List<String> effectiveMemberIds) {
    }
}
