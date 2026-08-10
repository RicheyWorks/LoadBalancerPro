package com.richmond423.loadbalancerpro.api.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Expands logical discovered upstreams into an atomic bounded effective configuration. */
final class ProxyDnsEffectiveConfig {
    private ProxyDnsEffectiveConfig() {
    }

    static List<ProxyDnsDiscoveryRuntime.Registration> registrations(ReverseProxyProperties properties) {
        Objects.requireNonNull(properties, "properties cannot be null");
        Map<String, ProxyDnsDiscoveryRuntime.Registration> registrations = new LinkedHashMap<>();
        boolean privateOnly = properties.getPrivateNetworkValidation().isEnabled();
        if (properties.getRoutes().isEmpty()) {
            collectRegistrations(properties.getUpstreams(), "loadbalancerpro.proxy.upstreams", privateOnly,
                    registrations);
        } else {
            properties.getRoutes().forEach((routeName, route) -> collectRegistrations(
                    route == null ? List.of() : route.getTargets(),
                    "loadbalancerpro.proxy.routes." + routeName + ".targets",
                    privateOnly,
                    registrations));
        }
        return List.copyOf(registrations.values());
    }

    static Expansion expand(
            ReverseProxyProperties logicalProperties,
            ProxyDnsDiscoveryRuntime.Snapshot snapshot) {
        ReverseProxyProperties effective = ReverseProxyService.copyProperties(
                Objects.requireNonNull(logicalProperties, "logicalProperties cannot be null"));
        ProxyDnsDiscoveryRuntime.Snapshot safeSnapshot = Objects.requireNonNullElseGet(
                snapshot, ProxyDnsDiscoveryRuntime.Snapshot::empty);
        Set<String> emptyRoutes = new LinkedHashSet<>();
        Set<String> emptySplits = new LinkedHashSet<>();
        Map<String, List<String>> logicalToEffective = new LinkedHashMap<>();
        Map<String, String> effectiveToLogical = new LinkedHashMap<>();
        int total = 0;

        if (effective.getRoutes().isEmpty()) {
            ExpandedTargets expanded = expandTargets(
                    effective.getUpstreams(), safeSnapshot, logicalToEffective, effectiveToLogical);
            effective.setUpstreams(expanded.targets());
            total = boundedRouteAndTotal(
                    ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME, expanded.targets().size(), total);
            if (expanded.targets().isEmpty()) {
                emptyRoutes.add(ReverseProxyRoutePlanner.LEGACY_ROUTE_NAME);
            }
        } else {
            for (Map.Entry<String, ReverseProxyProperties.Route> routeEntry : effective.getRoutes().entrySet()) {
                String routeName = routeEntry.getKey();
                ReverseProxyProperties.Route route = routeEntry.getValue();
                if (route == null) {
                    continue;
                }
                ExpandedTargets expanded = expandTargets(
                        route.getTargets(), safeSnapshot, logicalToEffective, effectiveToLogical);
                route.setTargets(expanded.targets());
                total = boundedRouteAndTotal(routeName, expanded.targets().size(), total);
                if (expanded.targets().isEmpty()) {
                    emptyRoutes.add(routeName);
                }
                Map<String, ReverseProxyProperties.SplitGroup> effectiveSplits = new LinkedHashMap<>();
                route.getSplit().forEach((groupName, group) -> {
                    ReverseProxyProperties.SplitGroup copied = new ReverseProxyProperties.SplitGroup();
                    copied.setPercentage(group == null ? 0 : group.getPercentage());
                    List<String> expandedIds = group == null
                            ? List.of()
                            : group.getTargetIds().stream()
                                    .flatMap(id -> logicalToEffective.getOrDefault(id, List.of()).stream())
                                    .toList();
                    copied.setTargetIds(expandedIds);
                    if (expandedIds.isEmpty()) {
                        emptySplits.add(splitKey(routeName, groupName));
                    }
                    effectiveSplits.put(groupName, copied);
                });
                route.setSplit(effectiveSplits);
            }
        }
        return new Expansion(
                effective,
                Set.copyOf(emptyRoutes),
                Set.copyOf(emptySplits),
                immutableLists(logicalToEffective),
                Map.copyOf(effectiveToLogical),
                total);
    }

    static ProxyDnsDiscoveryRuntime.Snapshot carryForward(
            ReverseProxyProperties candidate,
            ReverseProxyProperties previous,
            ProxyDnsDiscoveryRuntime.Snapshot previousSnapshot,
            long generation) {
        Map<String, ProxyDnsDiscoveryRuntime.Registration> candidateRegistrations = byLogicalId(
                registrations(candidate));
        Map<String, ProxyDnsDiscoveryRuntime.Registration> previousRegistrations = byLogicalId(
                registrations(previous));
        Map<String, List<ProxyDnsDiscovery.Member>> members = new LinkedHashMap<>();
        Map<String, ProxyDnsDiscoveryRuntime.Status> statuses = new LinkedHashMap<>();
        candidateRegistrations.forEach((logicalId, registration) -> {
            if (!registration.equals(previousRegistrations.get(logicalId))) {
                return;
            }
            members.put(logicalId, previousSnapshot.membersByLogicalId().getOrDefault(logicalId, List.of()));
            ProxyDnsDiscoveryRuntime.Status status = previousSnapshot.statusByLogicalId().get(logicalId);
            if (status != null) {
                statuses.put(logicalId, status);
            }
        });
        return new ProxyDnsDiscoveryRuntime.Snapshot(generation, members, statuses);
    }

    static String splitKey(String routeName, String groupName) {
        return routeName + "\n" + groupName;
    }

    private static void collectRegistrations(
            List<ReverseProxyProperties.Upstream> targets,
            String fieldPrefix,
            boolean privateOnly,
            Map<String, ProxyDnsDiscoveryRuntime.Registration> registrations) {
        for (int index = 0; index < targets.size(); index++) {
            ReverseProxyProperties.Upstream target = targets.get(index);
            if (target == null || target.getDiscovery() == null || target.getDiscovery().isBlank()) {
                continue;
            }
            String fieldName = fieldPrefix + "[" + index + "].discovery";
            String logicalId = ReverseProxyRoutePlanner.validateUpstreamId(
                    target.getId(), fieldPrefix + "[" + index + "].id");
            ProxyDnsDiscovery.Spec spec = ProxyDnsDiscovery.compile(
                    target.getDiscovery(), target.getUrl(), target.getDiscoveryAuthority(), fieldName);
            ProxyDnsDiscoveryRuntime.Registration registration =
                    new ProxyDnsDiscoveryRuntime.Registration(logicalId, spec, privateOnly);
            ProxyDnsDiscoveryRuntime.Registration previous = registrations.putIfAbsent(logicalId, registration);
            if (previous != null && !previous.equals(registration)) {
                throw new IllegalStateException(
                        "DNS discovery logical id must have one configuration across all routes: " + logicalId);
            }
        }
    }

    private static ExpandedTargets expandTargets(
            List<ReverseProxyProperties.Upstream> logicalTargets,
            ProxyDnsDiscoveryRuntime.Snapshot snapshot,
            Map<String, List<String>> logicalToEffective,
            Map<String, String> effectiveToLogical) {
        List<ReverseProxyProperties.Upstream> effectiveTargets = new ArrayList<>();
        for (ReverseProxyProperties.Upstream logical : logicalTargets) {
            if (logical == null) {
                continue;
            }
            String logicalId = Objects.requireNonNullElse(logical.getId(), "").trim();
            if (logical.getDiscovery() == null || logical.getDiscovery().isBlank()) {
                ReverseProxyProperties.Upstream copied = ReverseProxyService.copyUpstream(logical);
                effectiveTargets.add(copied);
                logicalToEffective.put(logicalId, List.of(logicalId));
                effectiveToLogical.put(logicalId, logicalId);
                continue;
            }
            List<ProxyDnsDiscovery.Member> members = snapshot.membersByLogicalId()
                    .getOrDefault(logicalId, List.of());
            List<String> memberIds = new ArrayList<>(members.size());
            for (int index = 0; index < members.size(); index++) {
                ProxyDnsDiscovery.Member member = members.get(index);
                ReverseProxyProperties.Upstream effective = effectiveMember(logical, member, members.size(), index);
                effectiveTargets.add(effective);
                memberIds.add(member.id());
                String previous = effectiveToLogical.putIfAbsent(member.id(), logicalId);
                if (previous != null && !previous.equals(logicalId)) {
                    throw new IllegalStateException("DNS effective member id collision");
                }
            }
            logicalToEffective.put(logicalId, List.copyOf(memberIds));
        }
        return new ExpandedTargets(List.copyOf(effectiveTargets));
    }

    private static ReverseProxyProperties.Upstream effectiveMember(
            ReverseProxyProperties.Upstream logical,
            ProxyDnsDiscovery.Member member,
            int memberCount,
            int memberIndex) {
        ReverseProxyProperties.Upstream effective = ReverseProxyService.copyUpstream(logical);
        effective.setId(member.id());
        effective.setUrl(member.endpoint().toString());
        effective.setDiscovery("");
        effective.setDiscoveryAuthority("");
        effective.setWeight(logical.getWeight() / memberCount);
        effective.setConfiguredCapacity(divide(logical.getConfiguredCapacity(), memberCount));
        effective.setEstimatedConcurrencyLimit(divide(logical.getEstimatedConcurrencyLimit(), memberCount));
        effective.setMaxInFlight(distribute(logical.getMaxInFlight(), memberCount, memberIndex));
        effective.setInFlightRequestCount(distribute(
                logical.getInFlightRequestCount(), memberCount, memberIndex));
        effective.setQueueDepth(logical.getQueueDepth() == null
                ? null
                : distribute(logical.getQueueDepth(), memberCount, memberIndex));
        return effective;
    }

    private static Double divide(Double value, int count) {
        return value == null ? null : value / count;
    }

    private static int distribute(int value, int count, int index) {
        if (value <= 0) {
            return 0;
        }
        return value / count + (index < value % count ? 1 : 0);
    }

    private static int boundedRouteAndTotal(String routeName, int routeCount, int currentTotal) {
        if (routeCount > ReverseProxyRoutePlanner.MAX_TARGETS_PER_ROUTE) {
            throw new IllegalStateException("DNS refresh would create more than "
                    + ReverseProxyRoutePlanner.MAX_TARGETS_PER_ROUTE
                    + " effective members for route " + routeName);
        }
        long total = (long) currentTotal + routeCount;
        if (total > ReverseProxyRoutePlanner.MAX_CONFIGURED_TARGETS) {
            throw new IllegalStateException("DNS refresh would create more than "
                    + ReverseProxyRoutePlanner.MAX_CONFIGURED_TARGETS + " effective members in total");
        }
        return (int) total;
    }

    private static Map<String, List<String>> immutableLists(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Map.copyOf(copy);
    }

    private static Map<String, ProxyDnsDiscoveryRuntime.Registration> byLogicalId(
            Collection<ProxyDnsDiscoveryRuntime.Registration> registrations) {
        Map<String, ProxyDnsDiscoveryRuntime.Registration> result = new LinkedHashMap<>();
        registrations.forEach(registration -> result.put(registration.logicalId(), registration));
        return result;
    }

    record Expansion(
            ReverseProxyProperties effectiveProperties,
            Set<String> allowedEmptyRoutes,
            Set<String> allowedEmptySplits,
            Map<String, List<String>> effectiveIdsByLogicalId,
            Map<String, String> logicalIdByEffectiveId,
            int effectiveTargetCount) {
        boolean allowsEmptyRoute(String routeName) {
            return allowedEmptyRoutes.contains(routeName);
        }

        boolean allowsEmptySplit(String routeName, String groupName) {
            return allowedEmptySplits.contains(splitKey(routeName, groupName));
        }
    }

    private record ExpandedTargets(List<ReverseProxyProperties.Upstream> targets) {
    }
}
