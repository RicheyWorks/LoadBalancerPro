package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import org.junit.jupiter.api.Test;

class ProxyDnsConfigurationTest {
    @Test
    void acceptsExplicitAddressAuthorityWithoutResolvingTheConfiguredName() {
        ReverseProxyProperties properties = properties(discovered("service.example"));
        properties.getPrivateNetworkValidation().setEnabled(true);

        List<ReverseProxyRoutePlanner.ConfiguredRoute> routes =
                ReverseProxyRoutePlanner.buildEnabledRoutes(
                        properties, RoutingStrategyRegistry.defaultRegistry());

        assertEquals(1, routes.size());
        assertEquals("dns:service.example:8080", routes.get(0).targets().get(0).getDiscovery());
        assertEquals("address", routes.get(0).targets().get(0).getDiscoveryAuthority());
    }

    @Test
    void rejectsDiscoveryWithoutExplicitAddressAuthority() {
        ReverseProxyProperties.Upstream target = discovered("service.example");
        target.setDiscoveryAuthority("");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ReverseProxyRoutePlanner.buildEnabledRoutes(
                        properties(target), RoutingStrategyRegistry.defaultRegistry()));

        assertTrue(exception.getMessage().contains("discovery-authority"));
    }

    @Test
    void rejectsDiscoveryAuthorityOnAStaticTarget() {
        ReverseProxyProperties.Upstream target = new ReverseProxyProperties.Upstream();
        target.setId("backend");
        target.setUrl("http://127.0.0.1:8080");
        target.setDiscoveryAuthority("address");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ReverseProxyRoutePlanner.buildEnabledRoutes(
                        properties(target), RoutingStrategyRegistry.defaultRegistry()));

        assertTrue(exception.getMessage().contains("must be blank"));
    }

    @Test
    void addressAuthorityDoesNotEnableAGlobalRestrictedHeaderOverride() throws Exception {
        try (Stream<Path> sources = Files.walk(Path.of("src/main/java"))) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(source, StandardCharsets.UTF_8);
                assertFalse(text.contains("jdk.httpclient.allowRestrictedHeaders"), source.toString());
            }
        }
    }

    @Test
    void validatesFiniteGlobalDiscoveryBounds() {
        ReverseProxyProperties.DnsDiscovery defaults = new ReverseProxyProperties.DnsDiscovery();
        ProxyDnsDiscoverySettings compiled = ProxyDnsDiscoverySettings.compile(defaults);
        assertEquals(Duration.ofSeconds(30), compiled.ttlFloor());
        assertEquals(Duration.ofMinutes(5), compiled.staleAfter());
        assertEquals(Duration.ofSeconds(2), compiled.resolutionTimeout());
        assertEquals(4, compiled.lookupThreads());

        defaults.setTtlFloor(Duration.ofMillis(999));
        assertThrows(IllegalStateException.class, () -> ProxyDnsDiscoverySettings.compile(defaults));
        defaults.setTtlFloor(Duration.ofSeconds(30));
        defaults.setStaleAfter(Duration.ofSeconds(29));
        assertThrows(IllegalStateException.class, () -> ProxyDnsDiscoverySettings.compile(defaults));
        defaults.setStaleAfter(Duration.ofMinutes(5));
        defaults.setResolutionTimeout(Duration.ofSeconds(31));
        assertThrows(IllegalStateException.class, () -> ProxyDnsDiscoverySettings.compile(defaults));
        defaults.setResolutionTimeout(Duration.ofSeconds(2));
        defaults.setLookupThreads(17);
        assertThrows(IllegalStateException.class, () -> ProxyDnsDiscoverySettings.compile(defaults));
    }

    private static ReverseProxyProperties properties(ReverseProxyProperties.Upstream target) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setUpstreams(List.of(target));
        return properties;
    }

    private static ReverseProxyProperties.Upstream discovered(String name) {
        ReverseProxyProperties.Upstream target = new ReverseProxyProperties.Upstream();
        target.setId("backend");
        target.setUrl("http://" + name + ":8080/base");
        target.setDiscovery("dns:" + name + ":8080");
        target.setDiscoveryAuthority("address");
        return target;
    }
}
