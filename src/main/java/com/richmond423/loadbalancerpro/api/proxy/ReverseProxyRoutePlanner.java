package com.richmond423.loadbalancerpro.api.proxy;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.richmond423.loadbalancerpro.core.RoutingStrategy;
import com.richmond423.loadbalancerpro.core.RoutingStrategyId;
import com.richmond423.loadbalancerpro.core.RoutingStrategyRegistry;

final class ReverseProxyRoutePlanner {
    static final String LEGACY_ROUTE_NAME = "legacy-upstreams";

    private static final Pattern ROUTE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    private static final String PRIVATE_NETWORK_VALIDATION_FLAG =
            "loadbalancerpro.proxy.private-network-validation.enabled";

    private ReverseProxyRoutePlanner() {
    }

    static List<ConfiguredRoute> buildEnabledRoutes(ReverseProxyProperties properties,
                                                    RoutingStrategyRegistry registry) {
        Objects.requireNonNull(properties, "properties cannot be null");
        Objects.requireNonNull(registry, "registry cannot be null");
        if (!properties.isEnabled()) {
            return List.of();
        }

        boolean privateNetworkValidationEnabled = properties.getPrivateNetworkValidation().isEnabled();
        if (!properties.getRoutes().isEmpty()) {
            List<ConfiguredRoute> routes = new ArrayList<>();
            for (Map.Entry<String, ReverseProxyProperties.Route> entry : properties.getRoutes().entrySet()) {
                String routeName = validateRouteName(entry.getKey());
                ReverseProxyProperties.Route route = Objects.requireNonNullElseGet(
                        entry.getValue(), ReverseProxyProperties.Route::new);
                String pathPrefix = normalizedPathPrefix(route.getPathPrefix(),
                        "loadbalancerpro.proxy.routes." + routeName + ".path-prefix");
                String strategyName = route.getStrategy() == null || route.getStrategy().isBlank()
                        ? properties.getStrategy()
                        : route.getStrategy();
                RoutingStrategyId strategyId = strategyId(strategyName,
                        "loadbalancerpro.proxy.routes." + routeName + ".strategy");
                RoutingStrategy strategy = strategy(registry, strategyId);
                List<ReverseProxyProperties.Upstream> targets = route.getTargets();
                if (targets.isEmpty()) {
                    throw new IllegalStateException(
                            "loadbalancerpro.proxy.routes." + routeName + ".targets must contain at least one target");
                }
                validateTargets(targets, "loadbalancerpro.proxy.routes." + routeName + ".targets",
                        privateNetworkValidationEnabled);
                routes.add(new ConfiguredRoute(routeName, pathPrefix, strategyId, strategy, List.copyOf(targets)));
            }
            return List.copyOf(routes);
        }

        List<ReverseProxyProperties.Upstream> upstreams = properties.getUpstreams();
        if (upstreams.isEmpty()) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.enabled=true requires at least one configured route or upstream target");
        }
        RoutingStrategyId strategyId = strategyId(properties.getStrategy(), "loadbalancerpro.proxy.strategy");
        RoutingStrategy strategy = strategy(registry, strategyId);
        validateTargets(upstreams, "loadbalancerpro.proxy.upstreams", privateNetworkValidationEnabled);
        return List.of(new ConfiguredRoute(
                LEGACY_ROUTE_NAME, "/", strategyId, strategy, List.copyOf(upstreams)));
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

    private static String validateRouteName(String routeName) {
        if (routeName == null || !ROUTE_NAME.matcher(routeName).matches()) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.routes route names must match " + ROUTE_NAME.pattern());
        }
        return routeName;
    }

    private static RoutingStrategyId strategyId(String value, String fieldName) {
        String strategyName = value == null || value.isBlank() ? "" : value.trim();
        return RoutingStrategyId.fromName(strategyName)
                .orElseThrow(() -> new IllegalStateException(fieldName
                        + " must be a supported strategy id; received: " + strategyName));
    }

    private static RoutingStrategy strategy(RoutingStrategyRegistry registry, RoutingStrategyId strategyId) {
        return registry.find(strategyId)
                .orElseThrow(() -> new IllegalStateException(
                        "Proxy routing strategy is not registered: " + strategyId.externalName()));
    }

    private static void validateTargets(List<ReverseProxyProperties.Upstream> targets,
                                        String fieldPrefix,
                                        boolean privateNetworkValidationEnabled) {
        Set<String> ids = new LinkedHashSet<>();
        for (int index = 0; index < targets.size(); index++) {
            ReverseProxyProperties.Upstream target = targets.get(index);
            String targetPrefix = fieldPrefix + "[" + index + "]";
            if (target == null) {
                throw new IllegalStateException(targetPrefix + " must not be null");
            }
            String id = requireNonBlank(target.getId(), targetPrefix + ".id");
            if (!ids.add(id)) {
                throw new IllegalStateException(fieldPrefix + " contains duplicate target id: " + id);
            }
            validateTargetUrl(target.getUrl(), targetPrefix + ".url", privateNetworkValidationEnabled);
            if (!Double.isFinite(target.getWeight()) || target.getWeight() <= 0.0) {
                throw new IllegalStateException(targetPrefix + ".weight must be finite and greater than 0");
            }
        }
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
            RoutingStrategyId strategyId,
            RoutingStrategy strategy,
            List<ReverseProxyProperties.Upstream> targets) {
    }
}
