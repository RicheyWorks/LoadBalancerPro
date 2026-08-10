package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import org.junit.jupiter.api.Test;

class ProxyDnsEffectiveConfigTest {
    @Test
    void leavesStaticUpstreamsUnchanged() {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        ReverseProxyProperties.Upstream staticTarget = target("static", "http://127.0.0.1:9000/base");
        staticTarget.setWeight(7.0);
        staticTarget.setMaxInFlight(11);
        properties.setUpstreams(List.of(staticTarget));

        ProxyDnsEffectiveConfig.Expansion expansion = ProxyDnsEffectiveConfig.expand(
                properties, ProxyDnsDiscoveryRuntime.Snapshot.empty());

        ReverseProxyProperties.Upstream effective = expansion.effectiveProperties().getUpstreams().get(0);
        assertEquals("static", effective.getId());
        assertEquals("http://127.0.0.1:9000/base", effective.getUrl());
        assertEquals(7.0, effective.getWeight());
        assertEquals(11, effective.getMaxInFlight());
        assertTrue(expansion.allowedEmptyRoutes().isEmpty());
    }

    @Test
    void emptyDiscoveredRouteAndSplitFailClosedWithoutWeakeningStaticValidation() {
        ReverseProxyProperties properties = splitProperties();
        ProxyDnsEffectiveConfig.Expansion expansion = ProxyDnsEffectiveConfig.expand(
                properties, snapshot(Map.of("discovered", List.of())));

        ReverseProxyRoutePlanner.ConfiguredRoute route = ReverseProxyRoutePlanner.buildEnabledRoutes(
                expansion.effectiveProperties(), RoutingStrategyRegistry.defaultRegistry(), List.of(), expansion)
                .get(0);

        assertEquals(List.of("static"), route.targets().stream()
                .map(ReverseProxyProperties.Upstream::getId).toList());
        assertTrue(route.splits().stream()
                .filter(split -> split.name().equals("canary"))
                .findFirst().orElseThrow().targetIds().isEmpty());

        ReverseProxyProperties invalidStatic = new ReverseProxyProperties();
        invalidStatic.setEnabled(true);
        assertThrows(IllegalStateException.class, () -> ReverseProxyRoutePlanner.buildEnabledRoutes(
                invalidStatic, RoutingStrategyRegistry.defaultRegistry()));
    }

    @Test
    void dividesLogicalWeightCapacityAndLimitsAcrossDeterministicMembers() throws Exception {
        ReverseProxyProperties properties = splitProperties();
        ReverseProxyProperties.Upstream logical = properties.getRoutes().get("route").getTargets().get(0);
        logical.setWeight(10.0);
        logical.setConfiguredCapacity(100.0);
        logical.setEstimatedConcurrencyLimit(50.0);
        logical.setMaxInFlight(5);
        logical.setInFlightRequestCount(3);
        logical.setQueueDepth(3);
        List<ProxyDnsDiscovery.Member> members = members(logical, List.of(
                literal(127, 0, 0, 2), literal(127, 0, 0, 1)));

        ProxyDnsEffectiveConfig.Expansion expansion = ProxyDnsEffectiveConfig.expand(
                properties, snapshot(Map.of("discovered", members)));
        List<ReverseProxyProperties.Upstream> discovered = expansion.effectiveProperties()
                .getRoutes().get("route").getTargets().stream()
                .filter(target -> !target.getId().equals("static"))
                .toList();

        assertEquals(2, discovered.size());
        assertEquals(List.of("http://127.0.0.1:8080/base", "http://127.0.0.2:8080/base"),
                discovered.stream().map(ReverseProxyProperties.Upstream::getUrl).toList());
        assertEquals(List.of(5.0, 5.0), discovered.stream()
                .map(ReverseProxyProperties.Upstream::getWeight).toList());
        assertEquals(List.of(50.0, 50.0), discovered.stream()
                .map(ReverseProxyProperties.Upstream::getConfiguredCapacity).toList());
        assertEquals(List.of(25.0, 25.0), discovered.stream()
                .map(ReverseProxyProperties.Upstream::getEstimatedConcurrencyLimit).toList());
        assertEquals(List.of(3, 2), discovered.stream()
                .map(ReverseProxyProperties.Upstream::getMaxInFlight).toList());
        assertEquals(List.of(2, 1), discovered.stream()
                .map(ReverseProxyProperties.Upstream::getInFlightRequestCount).toList());
        assertEquals(List.of(2, 1), discovered.stream()
                .map(ReverseProxyProperties.Upstream::getQueueDepth).toList());

        ReverseProxyRoutePlanner.ConfiguredRoute planned = ReverseProxyRoutePlanner.buildEnabledRoutes(
                expansion.effectiveProperties(), RoutingStrategyRegistry.defaultRegistry(), List.of(), expansion)
                .get(0);
        assertEquals(2, planned.splits().stream()
                .filter(split -> split.name().equals("canary"))
                .findFirst().orElseThrow().targetIds().size());
        assertEquals(1, planned.splits().stream()
                .filter(split -> split.name().equals("stable"))
                .findFirst().orElseThrow().targetIds().size());
    }

    @Test
    void rejectsPerRouteAndAggregateEffectiveMemberOverflowAtomically() throws Exception {
        ReverseProxyProperties routeOverflow = routesWithDiscoveredTargets(1, 3);
        Map<String, List<ProxyDnsDiscovery.Member>> routeMembers = memberMap(routeOverflow, 32);
        assertThrows(IllegalStateException.class, () -> ProxyDnsEffectiveConfig.expand(
                routeOverflow, snapshot(routeMembers)));

        ReverseProxyProperties totalOverflow = routesWithDiscoveredTargets(5, 2);
        Map<String, List<ProxyDnsDiscovery.Member>> totalMembers = memberMap(totalOverflow, 32);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ProxyDnsEffectiveConfig.expand(totalOverflow, snapshot(totalMembers)));
        assertTrue(exception.getMessage().contains("256 effective members"));
    }

    private static ReverseProxyProperties splitProperties() {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        ReverseProxyProperties.Route route = new ReverseProxyProperties.Route();
        route.setTargets(List.of(discovered("discovered", "service.example"),
                target("static", "http://127.0.0.1:9000")));
        ReverseProxyProperties.SplitGroup canary = new ReverseProxyProperties.SplitGroup();
        canary.setPercentage(20);
        canary.setTargetIds(List.of("discovered"));
        ReverseProxyProperties.SplitGroup stable = new ReverseProxyProperties.SplitGroup();
        stable.setPercentage(80);
        stable.setTargetIds(List.of("static"));
        route.setSplit(Map.of("canary", canary, "stable", stable));
        properties.setRoutes(Map.of("route", route));
        return properties;
    }

    private static ReverseProxyProperties routesWithDiscoveredTargets(int routeCount, int targetsPerRoute) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        Map<String, ReverseProxyProperties.Route> routes = new LinkedHashMap<>();
        for (int routeIndex = 0; routeIndex < routeCount; routeIndex++) {
            ReverseProxyProperties.Route route = new ReverseProxyProperties.Route();
            List<ReverseProxyProperties.Upstream> targets = new ArrayList<>();
            for (int targetIndex = 0; targetIndex < targetsPerRoute; targetIndex++) {
                String id = "r" + routeIndex + "t" + targetIndex;
                targets.add(discovered(id, id + ".example"));
            }
            route.setTargets(targets);
            routes.put("route" + routeIndex, route);
        }
        properties.setRoutes(routes);
        return properties;
    }

    private static Map<String, List<ProxyDnsDiscovery.Member>> memberMap(
            ReverseProxyProperties properties, int count) throws Exception {
        Map<String, List<ProxyDnsDiscovery.Member>> result = new LinkedHashMap<>();
        for (ReverseProxyProperties.Route route : properties.getRoutes().values()) {
            for (ReverseProxyProperties.Upstream logical : route.getTargets()) {
                List<InetAddress> answers = new ArrayList<>();
                int discriminator = result.size() + 1;
                for (int index = 1; index <= count; index++) {
                    answers.add(literal(10, discriminator, 0, index));
                }
                result.put(logical.getId(), members(logical, answers));
            }
        }
        return result;
    }

    private static List<ProxyDnsDiscovery.Member> members(
            ReverseProxyProperties.Upstream logical, List<InetAddress> answers) {
        ProxyDnsDiscovery.Spec spec = ProxyDnsDiscovery.compile(
                logical.getDiscovery(), logical.getUrl(), logical.getDiscoveryAuthority(), "upstream.discovery");
        return ProxyDnsDiscovery.members(spec, logical.getId(), answers, true);
    }

    private static ProxyDnsDiscoveryRuntime.Snapshot snapshot(
            Map<String, List<ProxyDnsDiscovery.Member>> members) {
        return new ProxyDnsDiscoveryRuntime.Snapshot(1, members, Map.of());
    }

    private static ReverseProxyProperties.Upstream discovered(String id, String name) {
        ReverseProxyProperties.Upstream target = target(id, "http://" + name + ":8080/base");
        target.setDiscovery("dns:" + name + ":8080");
        target.setDiscoveryAuthority("address");
        return target;
    }

    private static ReverseProxyProperties.Upstream target(String id, String url) {
        ReverseProxyProperties.Upstream target = new ReverseProxyProperties.Upstream();
        target.setId(id);
        target.setUrl(url);
        return target;
    }

    private static InetAddress literal(int... octets) throws Exception {
        byte[] bytes = new byte[octets.length];
        for (int index = 0; index < octets.length; index++) {
            bytes[index] = (byte) octets[index];
        }
        return InetAddress.getByAddress(bytes);
    }
}
