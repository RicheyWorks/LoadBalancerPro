package com.richmond423.loadbalancerpro.api.proxy;

import java.time.Duration;
import java.util.Objects;

/** Validated finite global bounds for the DNS discovery runtime. */
record ProxyDnsDiscoverySettings(
        Duration ttlFloor,
        Duration staleAfter,
        Duration resolutionTimeout,
        int lookupThreads) {
    static final Duration MIN_TTL_FLOOR = Duration.ofSeconds(1);
    static final Duration MAX_TTL_FLOOR = Duration.ofHours(1);
    static final Duration MIN_STALE_AFTER = Duration.ofSeconds(1);
    static final Duration MAX_STALE_AFTER = Duration.ofHours(24);
    static final Duration MIN_RESOLUTION_TIMEOUT = Duration.ofMillis(100);
    static final Duration MAX_RESOLUTION_TIMEOUT = Duration.ofSeconds(30);
    static final int MAX_LOOKUP_THREADS = 16;

    ProxyDnsDiscoverySettings {
        ttlFloor = bounded(ttlFloor, MIN_TTL_FLOOR, MAX_TTL_FLOOR,
                "loadbalancerpro.proxy.dns-discovery.ttl-floor");
        staleAfter = bounded(staleAfter, MIN_STALE_AFTER, MAX_STALE_AFTER,
                "loadbalancerpro.proxy.dns-discovery.stale-after");
        resolutionTimeout = bounded(resolutionTimeout, MIN_RESOLUTION_TIMEOUT, MAX_RESOLUTION_TIMEOUT,
                "loadbalancerpro.proxy.dns-discovery.resolution-timeout");
        if (staleAfter.compareTo(ttlFloor) < 0) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.dns-discovery.stale-after must not be shorter than ttl-floor");
        }
        if (lookupThreads < 1 || lookupThreads > MAX_LOOKUP_THREADS) {
            throw new IllegalStateException("loadbalancerpro.proxy.dns-discovery.lookup-threads must be from 1 through "
                    + MAX_LOOKUP_THREADS);
        }
    }

    static ProxyDnsDiscoverySettings compile(ReverseProxyProperties.DnsDiscovery properties) {
        ReverseProxyProperties.DnsDiscovery candidate = Objects.requireNonNullElseGet(
                properties, ReverseProxyProperties.DnsDiscovery::new);
        return new ProxyDnsDiscoverySettings(
                candidate.getTtlFloor(),
                candidate.getStaleAfter(),
                candidate.getResolutionTimeout(),
                candidate.getLookupThreads());
    }

    private static Duration bounded(Duration value, Duration minimum, Duration maximum, String fieldName) {
        if (value == null || value.isNegative() || value.isZero()
                || value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalStateException(fieldName + " must be from " + minimum + " through " + maximum);
        }
        return value;
    }
}
