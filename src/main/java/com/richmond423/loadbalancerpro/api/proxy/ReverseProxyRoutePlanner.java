package com.richmond423.loadbalancerpro.api.proxy;

import java.net.IDN;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.richmond423.loadbalancerpro.core.ConsistentHashRingStrategy;
import com.richmond423.loadbalancerpro.core.RoutingStrategy;
import com.richmond423.loadbalancerpro.core.RoutingStrategyId;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;
import jakarta.servlet.http.HttpServletRequest;

final class ReverseProxyRoutePlanner {
    static final String LEGACY_ROUTE_NAME = "legacy-upstreams";
    static final int MAX_CONFIGURED_ROUTES = 32;
    static final int MAX_TARGETS_PER_ROUTE = 64;
    static final int MAX_CONFIGURED_TARGETS = 256;
    static final int MAX_HEADER_MATCHES_PER_ROUTE = 16;
    static final int MAX_SPLIT_GROUPS_PER_ROUTE = 16;

    private static final Pattern ROUTE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    private static final Pattern UPSTREAM_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    private static final Pattern HEADER_NAME = Pattern.compile("[-!#$%&'*+.^_`|~0-9A-Za-z]+");
    private static final Pattern DNS_LABEL = Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?");
    private static final Set<String> SENSITIVE_MATCH_HEADERS = Set.of(
            "authorization", "proxy-authorization", "cookie", "set-cookie", "x-api-key");
    private static final Set<String> RESERVED_METRIC_TAG_VALUES = Set.of("NONE", "OTHER", "UNMATCHED");
    private static final String PRIVATE_NETWORK_VALIDATION_FLAG =
            "loadbalancerpro.proxy.private-network-validation.enabled";

    private ReverseProxyRoutePlanner() {
    }

    static List<ConfiguredRoute> buildEnabledRoutes(ReverseProxyProperties properties,
                                                    RoutingStrategyRegistry registry) {
        return buildEnabledRoutes(properties, registry, List.of());
    }

    static List<ConfiguredRoute> buildEnabledRoutes(
            ReverseProxyProperties properties,
            RoutingStrategyRegistry registry,
            List<ConfiguredRoute> previousRoutes) {
        return buildEnabledRoutes(properties, registry, previousRoutes, null);
    }

    static List<ConfiguredRoute> buildEnabledRoutes(
            ReverseProxyProperties properties,
            RoutingStrategyRegistry registry,
            List<ConfiguredRoute> previousRoutes,
            ProxyDnsEffectiveConfig.Expansion discoveryExpansion) {
        Objects.requireNonNull(properties, "properties cannot be null");
        Objects.requireNonNull(registry, "registry cannot be null");
        Objects.requireNonNull(previousRoutes, "previousRoutes cannot be null");
        if (!properties.isEnabled()) {
            return List.of();
        }

        Map<String, ConfiguredRoute> previousRoutesByName = new LinkedHashMap<>();
        previousRoutes.forEach(route -> previousRoutesByName.put(route.name(), route));
        boolean privateNetworkValidationEnabled = properties.getPrivateNetworkValidation().isEnabled();
        if (!properties.getRoutes().isEmpty()) {
            if (properties.getRoutes().size() > MAX_CONFIGURED_ROUTES) {
                throw new IllegalStateException("loadbalancerpro.proxy.routes must contain at most "
                        + MAX_CONFIGURED_ROUTES + " routes");
            }
            List<ConfiguredRoute> routes = new ArrayList<>();
            List<RoutingStrategy> ownedStrategies = new ArrayList<>();
            int configuredTargetCount = 0;
            for (Map.Entry<String, ReverseProxyProperties.Route> entry : properties.getRoutes().entrySet()) {
                String routeName = validateRouteName(entry.getKey());
                ReverseProxyProperties.Route route = Objects.requireNonNullElseGet(
                        entry.getValue(), ReverseProxyProperties.Route::new);
                String pathPrefix = normalizedPathPrefix(route.getPathPrefix(),
                        "loadbalancerpro.proxy.routes." + routeName + ".path-prefix");
                String strategyName = route.getStrategy() == null || route.getStrategy().isBlank()
                        ? properties.getStrategy()
                        : route.getStrategy();
                Duration requestTimeout = route.getRequestTimeout() == null
                        ? properties.getRequestTimeout()
                        : route.getRequestTimeout();
                RoutingStrategyId strategyId = strategyId(strategyName,
                        "loadbalancerpro.proxy.routes." + routeName + ".strategy");
                List<ReverseProxyProperties.Upstream> targets = route.getTargets();
                if (targets.isEmpty() && (discoveryExpansion == null
                        || !discoveryExpansion.allowsEmptyRoute(routeName))) {
                    throw new IllegalStateException(
                            "loadbalancerpro.proxy.routes." + routeName + ".targets must contain at least one target");
                }
                configuredTargetCount = boundedTargetCount(configuredTargetCount, targets.size());
                validateTargets(targets, "loadbalancerpro.proxy.routes." + routeName + ".targets",
                        privateNetworkValidationEnabled);
                RoutingStrategy strategy = routeStrategy(
                        registry, strategyId, routeName, targets, previousRoutesByName);
                requireOwnedStrategy(routeName, strategy, ownedStrategies);
                ConfiguredMatch match = compileMatch(
                        route.getMatch(), "loadbalancerpro.proxy.routes." + routeName + ".match");
                List<ConfiguredSplit> splits = compileSplits(
                        routeName, route.getSplit(), targets, strategyId, registry,
                        previousRoutesByName.get(routeName), ownedStrategies, discoveryExpansion);
                ProxyRequestHeaders.HeaderRewrites headerRewrites = ProxyRequestHeaders.compileRewrites(
                        route.getHeaders(), "loadbalancerpro.proxy.routes." + routeName + ".headers");
                ProxyRouteSelectionPolicy selectionPolicy = ProxyRouteSelectionPolicy.compile(
                        routeName, route, "loadbalancerpro.proxy.routes." + routeName);
                routes.add(new ConfiguredRoute(
                        routeName, pathPrefix, match, strategyId, strategy, splits, requestTimeout,
                        headerRewrites, selectionPolicy, List.copyOf(targets)));
            }
            return List.copyOf(routes);
        }

        List<ReverseProxyProperties.Upstream> upstreams = properties.getUpstreams();
        if (upstreams.isEmpty() && (discoveryExpansion == null
                || !discoveryExpansion.allowsEmptyRoute(LEGACY_ROUTE_NAME))) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.enabled=true requires at least one configured route or upstream target");
        }
        boundedTargetCount(0, upstreams.size());
        RoutingStrategyId strategyId = strategyId(properties.getStrategy(), "loadbalancerpro.proxy.strategy");
        validateTargets(upstreams, "loadbalancerpro.proxy.upstreams", privateNetworkValidationEnabled);
        RoutingStrategy strategy = routeStrategy(
                registry, strategyId, LEGACY_ROUTE_NAME, upstreams, previousRoutesByName);
        return List.of(new ConfiguredRoute(
                LEGACY_ROUTE_NAME, "/", ConfiguredMatch.any(), strategyId, strategy, List.of(),
                properties.getRequestTimeout(),
                ProxyRequestHeaders.compileRewrites(
                        new ReverseProxyProperties.Headers(), "loadbalancerpro.proxy.routes.legacy.headers"),
                ProxyRouteSelectionPolicy.legacy(LEGACY_ROUTE_NAME),
                List.copyOf(upstreams)));
    }

    static boolean pathMatches(String pathPrefix, String proxyPathSuffix) {
        if ("/".equals(pathPrefix)) {
            return true;
        }
        return proxyPathSuffix.equals(pathPrefix) || proxyPathSuffix.startsWith(pathPrefix + "/");
    }

    static String normalizedPathPrefix(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(fieldName + " must not be blank");
        }
        String prefix = value.trim();
        if (!prefix.startsWith("/")) {
            throw new IllegalStateException(fieldName + " must start with /");
        }
        if (prefix.length() > 1 && prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        if (prefix.startsWith("//") || prefix.contains("\\") || prefix.contains("?")
                || containsControlCharacter(prefix)) {
            throw new IllegalStateException(fieldName + " must be a simple absolute path prefix");
        }
        return prefix;
    }

    static String safeRouteStrategy(ReverseProxyProperties properties, ReverseProxyProperties.Route route) {
        String strategy = route.getStrategy();
        return strategy == null || strategy.isBlank() ? properties.getStrategy() : strategy.trim();
    }

    static String validateRouteName(String routeName) {
        if (routeName == null || !ROUTE_NAME.matcher(routeName).matches()) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.routes route names must match " + ROUTE_NAME.pattern());
        }
        if (RESERVED_METRIC_TAG_VALUES.contains(routeName)) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.routes route names must not use reserved metric tag values "
                            + RESERVED_METRIC_TAG_VALUES);
        }
        return routeName;
    }

    static String validateUpstreamId(String value, String fieldName) {
        String id = requireNonBlank(value, fieldName);
        if (!UPSTREAM_ID.matcher(id).matches() || RESERVED_METRIC_TAG_VALUES.contains(id)) {
            throw new IllegalStateException(fieldName + " must match " + UPSTREAM_ID.pattern()
                    + " and must not use reserved metric tag values " + RESERVED_METRIC_TAG_VALUES);
        }
        return id;
    }

    private static RoutingStrategyId strategyId(String value, String fieldName) {
        String strategyName = value == null || value.isBlank() ? "" : value.trim();
        return RoutingStrategyId.fromName(strategyName)
                .orElseThrow(() -> new IllegalStateException(fieldName
                        + " must be a supported strategy id; received: " + strategyName));
    }

    private static RoutingStrategy routeStrategy(
            RoutingStrategyRegistry registry,
            RoutingStrategyId strategyId,
            String routeName,
            List<ReverseProxyProperties.Upstream> targets,
            Map<String, ConfiguredRoute> previousRoutesByName) {
        ConfiguredRoute previousRoute = previousRoutesByName.get(routeName);
        if (previousRoute != null
                && previousRoute.strategyId() == strategyId
                && upstreamIds(previousRoute.targets()).equals(upstreamIds(targets))) {
            return previousRoute.strategy();
        }
        RoutingStrategy strategy = registry.findFactory(strategyId)
                .map(RoutingStrategyRegistry.RoutingStrategyFactory::create)
                .orElseThrow(() -> new IllegalStateException(
                        "Proxy routing strategy is not registered: " + strategyId.externalName()));
        if (strategy instanceof ConsistentHashRingStrategy consistentHash) {
            return consistentHash.configuredFor(upstreamIds(targets));
        }
        return strategy;
    }

    private static Set<String> upstreamIds(List<ReverseProxyProperties.Upstream> targets) {
        Set<String> ids = new LinkedHashSet<>();
        targets.forEach(target -> ids.add(target.getId().trim()));
        return ids;
    }

    private static void requireOwnedStrategy(
            String ownerName,
            RoutingStrategy strategy,
            List<RoutingStrategy> ownedStrategies) {
        boolean alreadyOwned = ownedStrategies.stream().anyMatch(owned -> owned == strategy);
        if (alreadyOwned) {
            throw new IllegalStateException(
                    "Proxy routing strategy factory must create an owned instance for " + ownerName);
        }
        ownedStrategies.add(strategy);
    }

    private static ConfiguredMatch compileMatch(ReverseProxyProperties.Match match, String fieldPrefix) {
        ReverseProxyProperties.Match candidate = Objects.requireNonNullElseGet(
                match, ReverseProxyProperties.Match::new);
        String configuredHost = candidate.getHost() == null || candidate.getHost().isBlank()
                ? null
                : normalizedHost(candidate.getHost(), fieldPrefix + ".host");
        Map<String, String> configuredHeaders = candidate.getHeader();
        if (configuredHeaders.size() > MAX_HEADER_MATCHES_PER_ROUTE) {
            throw new IllegalStateException(fieldPrefix + ".header must contain at most "
                    + MAX_HEADER_MATCHES_PER_ROUTE + " entries");
        }
        Map<String, String> normalizedHeaders = new LinkedHashMap<>();
        configuredHeaders.entrySet().stream()
                .sorted(Comparator.comparing(
                        Map.Entry<String, String>::getKey,
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)))
                .forEach(entry -> {
                    String name = normalizedHeaderName(entry.getKey(), fieldPrefix + ".header");
                    String value = normalizedHeaderValue(entry.getValue(), fieldPrefix + ".header." + name);
                    if (normalizedHeaders.putIfAbsent(name, value) != null) {
                        throw new IllegalStateException(fieldPrefix
                                + ".header contains a case-insensitive duplicate: " + name);
                    }
                });
        return new ConfiguredMatch(configuredHost, Map.copyOf(normalizedHeaders));
    }

    private static List<ConfiguredSplit> compileSplits(
            String routeName,
            Map<String, ReverseProxyProperties.SplitGroup> configuredSplits,
            List<ReverseProxyProperties.Upstream> targets,
            RoutingStrategyId strategyId,
            RoutingStrategyRegistry registry,
            ConfiguredRoute previousRoute,
            List<RoutingStrategy> ownedStrategies,
            ProxyDnsEffectiveConfig.Expansion discoveryExpansion) {
        if (configuredSplits.isEmpty()) {
            return List.of();
        }
        String fieldPrefix = "loadbalancerpro.proxy.routes." + routeName + ".split";
        if (configuredSplits.size() > MAX_SPLIT_GROUPS_PER_ROUTE) {
            throw new IllegalStateException(fieldPrefix + " must contain at most "
                    + MAX_SPLIT_GROUPS_PER_ROUTE + " groups");
        }
        Map<String, ReverseProxyProperties.Upstream> targetsById = new LinkedHashMap<>();
        targets.forEach(target -> targetsById.put(target.getId().trim(), target));
        Set<String> assignedTargetIds = new LinkedHashSet<>();
        List<ConfiguredSplit> splits = new ArrayList<>();
        int cumulativePercentage = 0;
        List<Map.Entry<String, ReverseProxyProperties.SplitGroup>> orderedGroups = configuredSplits.entrySet().stream()
                .sorted(Comparator.comparing(
                        Map.Entry<String, ReverseProxyProperties.SplitGroup>::getKey,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        for (Map.Entry<String, ReverseProxyProperties.SplitGroup> entry : orderedGroups) {
            String groupName = validateSplitName(entry.getKey(), fieldPrefix);
            ReverseProxyProperties.SplitGroup group = Objects.requireNonNullElseGet(
                    entry.getValue(), ReverseProxyProperties.SplitGroup::new);
            int percentage = group.getPercentage();
            if (percentage <= 0 || percentage > 100) {
                throw new IllegalStateException(fieldPrefix + "." + groupName
                        + ".percentage must be between 1 and 100");
            }
            cumulativePercentage += percentage;
            if (cumulativePercentage > 100) {
                throw new IllegalStateException(fieldPrefix + " percentages must total exactly 100");
            }
            if (group.getTargetIds().isEmpty() && (discoveryExpansion == null
                    || !discoveryExpansion.allowsEmptySplit(routeName, groupName))) {
                throw new IllegalStateException(fieldPrefix + "." + groupName
                        + ".target-ids must contain at least one target id");
            }
            LinkedHashSet<String> groupTargetIds = new LinkedHashSet<>();
            for (int index = 0; index < group.getTargetIds().size(); index++) {
                String targetId = validateUpstreamId(group.getTargetIds().get(index),
                        fieldPrefix + "." + groupName + ".target-ids[" + index + "]");
                if (!targetsById.containsKey(targetId)) {
                    throw new IllegalStateException(fieldPrefix + "." + groupName
                            + " references unknown target id: " + targetId);
                }
                if (!groupTargetIds.add(targetId) || !assignedTargetIds.add(targetId)) {
                    throw new IllegalStateException(fieldPrefix
                            + " target ids must belong to exactly one group: " + targetId);
                }
            }
            List<ReverseProxyProperties.Upstream> groupTargets = groupTargetIds.stream()
                    .map(targetsById::get)
                    .toList();
            RoutingStrategy groupStrategy = splitStrategy(
                    registry, strategyId, routeName, groupName, groupTargets, previousRoute);
            requireOwnedStrategy(routeName + ".split." + groupName, groupStrategy, ownedStrategies);
            splits.add(new ConfiguredSplit(
                    groupName, percentage, cumulativePercentage, Set.copyOf(groupTargetIds), groupStrategy));
        }
        if (cumulativePercentage != 100) {
            throw new IllegalStateException(fieldPrefix + " percentages must total exactly 100");
        }
        if (!assignedTargetIds.equals(targetsById.keySet())) {
            Set<String> missing = new LinkedHashSet<>(targetsById.keySet());
            missing.removeAll(assignedTargetIds);
            throw new IllegalStateException(fieldPrefix
                    + " groups must assign every route target exactly once; unassigned=" + missing);
        }
        return List.copyOf(splits);
    }

    private static RoutingStrategy splitStrategy(
            RoutingStrategyRegistry registry,
            RoutingStrategyId strategyId,
            String routeName,
            String groupName,
            List<ReverseProxyProperties.Upstream> targets,
            ConfiguredRoute previousRoute) {
        Set<String> targetIds = upstreamIds(targets);
        if (previousRoute != null && previousRoute.strategyId() == strategyId) {
            Optional<ConfiguredSplit> previousSplit = previousRoute.splits().stream()
                    .filter(split -> split.name().equals(groupName))
                    .filter(split -> split.targetIds().equals(targetIds))
                    .findFirst();
            if (previousSplit.isPresent()) {
                return previousSplit.get().strategy();
            }
        }
        RoutingStrategy strategy = registry.findFactory(strategyId)
                .map(RoutingStrategyRegistry.RoutingStrategyFactory::create)
                .orElseThrow(() -> new IllegalStateException(
                        "Proxy routing strategy is not registered: " + strategyId.externalName()));
        if (strategy instanceof ConsistentHashRingStrategy consistentHash) {
            return consistentHash.configuredFor(targetIds);
        }
        return strategy;
    }

    private static String validateSplitName(String value, String fieldPrefix) {
        if (value == null || !ROUTE_NAME.matcher(value).matches()) {
            throw new IllegalStateException(fieldPrefix + " group names must match " + ROUTE_NAME.pattern());
        }
        return value;
    }

    private static String normalizedHeaderName(String value, String fieldPrefix) {
        String name = requireNonBlank(value, fieldPrefix + " name").toLowerCase(Locale.ROOT);
        if (name.length() > 128 || !HEADER_NAME.matcher(name).matches()) {
            throw new IllegalStateException(fieldPrefix + " names must be valid HTTP field names");
        }
        if (SENSITIVE_MATCH_HEADERS.contains(name)
                || ProxyRequestHeaders.isSpoofable(name)
                || ReverseProxyService.isHopByHopHeader(name)) {
            throw new IllegalStateException(fieldPrefix
                    + " cannot use sensitive, forwarding, or hop-by-hop headers");
        }
        return name;
    }

    private static String normalizedHeaderValue(String value, String fieldName) {
        String candidate = requireNonBlank(value, fieldName);
        if (candidate.length() > 512 || containsControlCharacter(candidate)) {
            throw new IllegalStateException(fieldName
                    + " must be at most 512 characters and contain no control characters");
        }
        return candidate;
    }

    static String normalizedHost(String value, String fieldName) {
        String authority = requireNonBlank(value, fieldName);
        if (authority.length() > 320 || authority.chars().anyMatch(Character::isWhitespace)
                || containsControlCharacter(authority) || authority.contains("/")
                || authority.contains("?") || authority.contains("#") || authority.contains("@")) {
            throw new IllegalStateException(fieldName + " must be an exact host with an optional numeric port");
        }
        String host;
        String port = null;
        if (authority.startsWith("[")) {
            int closing = authority.indexOf(']');
            if (closing <= 1) {
                throw new IllegalStateException(fieldName + " contains an invalid bracketed IPv6 host");
            }
            host = authority.substring(1, closing);
            String remainder = authority.substring(closing + 1);
            if (!remainder.isEmpty()) {
                if (!remainder.startsWith(":")) {
                    throw new IllegalStateException(fieldName + " contains an invalid host authority");
                }
                port = remainder.substring(1);
            }
            if (host.contains("%") || !isIpv6Literal(host)) {
                throw new IllegalStateException(fieldName + " contains an invalid IPv6 literal");
            }
            validateOptionalPort(port, fieldName);
            return host.toLowerCase(Locale.ROOT);
        }
        int firstColon = authority.indexOf(':');
        int lastColon = authority.lastIndexOf(':');
        if (firstColon >= 0) {
            if (firstColon != lastColon) {
                throw new IllegalStateException(fieldName + " must bracket IPv6 literals");
            }
            host = authority.substring(0, firstColon);
            port = authority.substring(firstColon + 1);
        } else {
            host = authority;
        }
        validateOptionalPort(port, fieldName);
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        String ascii;
        try {
            ascii = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(fieldName + " contains an invalid DNS host", exception);
        }
        if (ascii.isEmpty() || ascii.length() > 253) {
            throw new IllegalStateException(fieldName + " contains an invalid DNS host");
        }
        for (String label : ascii.split("\\.", -1)) {
            if (!DNS_LABEL.matcher(label).matches()) {
                throw new IllegalStateException(fieldName + " contains an invalid DNS host");
            }
        }
        return ascii;
    }

    private static void validateOptionalPort(String port, String fieldName) {
        if (port == null) {
            return;
        }
        if (!port.matches("[0-9]{1,5}")) {
            throw new IllegalStateException(fieldName + " contains an invalid port");
        }
        int numericPort = Integer.parseInt(port);
        if (numericPort < 1 || numericPort > 65_535) {
            throw new IllegalStateException(fieldName + " contains an invalid port");
        }
    }

    private static boolean isIpv6Literal(String value) {
        try {
            URI parsed = URI.create("http://[" + value + "]");
            return value.contains(":")
                    && parsed.getHost() != null
                    && parsed.getRawAuthority().equals("[" + value + "]")
                    && (parsed.getPath() == null || parsed.getPath().isEmpty());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static void validateTargets(List<ReverseProxyProperties.Upstream> targets,
                                        String fieldPrefix,
                                        boolean privateNetworkValidationEnabled) {
        if (targets.size() > MAX_TARGETS_PER_ROUTE) {
            throw new IllegalStateException(fieldPrefix + " must contain at most "
                    + MAX_TARGETS_PER_ROUTE + " targets");
        }
        Set<String> ids = new LinkedHashSet<>();
        for (int index = 0; index < targets.size(); index++) {
            ReverseProxyProperties.Upstream target = targets.get(index);
            String targetPrefix = fieldPrefix + "[" + index + "]";
            if (target == null) {
                throw new IllegalStateException(targetPrefix + " must not be null");
            }
            String id = validateUpstreamId(target.getId(), targetPrefix + ".id");
            if (!ids.add(id)) {
                throw new IllegalStateException(fieldPrefix + " contains duplicate target id: " + id);
            }
            if (target.getDiscovery() == null || target.getDiscovery().isBlank()) {
                if (target.getDiscoveryAuthority() != null && !target.getDiscoveryAuthority().isBlank()) {
                    throw new IllegalStateException(targetPrefix
                            + ".discovery-authority must be blank when discovery is not configured");
                }
                validateTargetUrl(target.getUrl(), targetPrefix + ".url", privateNetworkValidationEnabled);
            } else {
                ProxyDnsDiscovery.compile(
                        target.getDiscovery(), target.getUrl(), target.getDiscoveryAuthority(),
                        targetPrefix + ".discovery");
            }
            if (!Double.isFinite(target.getWeight()) || target.getWeight() < 0.0) {
                throw new IllegalStateException(targetPrefix + ".weight must be finite and non-negative");
            }
        }
    }

    private static int boundedTargetCount(int current, int additional) {
        if (additional > MAX_TARGETS_PER_ROUTE) {
            throw new IllegalStateException("proxy target lists must contain at most "
                    + MAX_TARGETS_PER_ROUTE + " targets");
        }
        long total = (long) current + additional;
        if (total > MAX_CONFIGURED_TARGETS) {
            throw new IllegalStateException("loadbalancerpro.proxy configuration must contain at most "
                    + MAX_CONFIGURED_TARGETS + " route targets");
        }
        return (int) total;
    }

    private static URI validateTargetUrl(String value, String fieldName, boolean privateNetworkValidationEnabled) {
        String url = requireNonBlank(value, fieldName);
        if (privateNetworkValidationEnabled) {
            ProxyBackendUrlClassifier.Classification classification = ProxyBackendUrlClassifier.classify(url);
            if (!classification.allowed()) {
                throw new IllegalStateException(fieldName
                        + " must be loopback or private-network when " + PRIVATE_NETWORK_VALIDATION_FLAG
                        + "=true; classifier status=" + classification.status()
                        + "; reason=" + classification.reason());
            }
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(fieldName + " must be a valid http/https URI", exception);
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalStateException(fieldName + " must use http or https");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalStateException(fieldName + " must include a host and must not include user info");
        }
        if (uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException(fieldName + " must not include query strings or fragments");
        }
        return uri;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(character -> character < 0x20 || character == 0x7f);
    }

    record ConfiguredRoute(
            String name,
            String pathPrefix,
            ConfiguredMatch match,
            RoutingStrategyId strategyId,
            RoutingStrategy strategy,
            List<ConfiguredSplit> splits,
            Duration requestTimeout,
            ProxyRequestHeaders.HeaderRewrites headerRewrites,
            ProxyRouteSelectionPolicy selectionPolicy,
            List<ReverseProxyProperties.Upstream> targets) {

        Optional<ConfiguredSplit> splitFor(String routingKey) {
            if (splits.isEmpty()) {
                return Optional.empty();
            }
            int bucket = stableBucket(routingKey);
            return splits.stream()
                    .filter(split -> bucket < split.upperExclusivePercentage())
                    .findFirst();
        }
    }

    record ConfiguredMatch(String host, Map<String, String> headers) {
        ConfiguredMatch {
            headers = Map.copyOf(headers);
        }

        static ConfiguredMatch any() {
            return new ConfiguredMatch(null, Map.of());
        }

        boolean matches(HttpServletRequest request) {
            if (host != null) {
                String requestHost = request.getHeader("Host");
                if (requestHost == null) {
                    return false;
                }
                try {
                    if (!host.equals(normalizedHost(requestHost, "request Host"))) {
                        return false;
                    }
                } catch (IllegalStateException exception) {
                    return false;
                }
            }
            return headers.entrySet().stream()
                    .allMatch(entry -> entry.getValue().equals(request.getHeader(entry.getKey())));
        }

        int hostSpecificity() {
            return host == null ? 0 : 1;
        }

        List<String> headerNames() {
            return headers.keySet().stream().sorted().toList();
        }
    }

    record ConfiguredSplit(
            String name,
            int percentage,
            int upperExclusivePercentage,
            Set<String> targetIds,
            RoutingStrategy strategy) {
        ConfiguredSplit {
            targetIds = Set.copyOf(targetIds);
        }
    }

    private static int stableBucket(String routingKey) {
        byte[] bytes = Objects.requireNonNull(routingKey, "routingKey cannot be null")
                .getBytes(StandardCharsets.UTF_8);
        long hash = 0xcbf29ce484222325L;
        for (byte value : bytes) {
            hash ^= value & 0xffL;
            hash *= 0x100000001b3L;
        }
        return (int) Long.remainderUnsigned(hash, 100);
    }
}
