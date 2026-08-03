package com.richmond423.loadbalancerpro.api.proxy;

public record ReverseProxyUpstreamPatchRequest(
        Long expectedGeneration,
        Double weight,
        Boolean healthy,
        Boolean drain) {
}
