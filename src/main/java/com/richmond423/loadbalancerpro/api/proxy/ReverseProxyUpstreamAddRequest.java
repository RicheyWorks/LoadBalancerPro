package com.richmond423.loadbalancerpro.api.proxy;

public record ReverseProxyUpstreamAddRequest(
        Long expectedGeneration,
        String route,
        String id,
        String url,
        Boolean healthy,
        Double weight,
        Integer maxInFlight) {
}
