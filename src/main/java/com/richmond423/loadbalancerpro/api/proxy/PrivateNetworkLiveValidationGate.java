package com.richmond423.loadbalancerpro.api.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Offline approval gate for a future private-network live validator.
 *
 * <p>This class only evaluates configuration. It does not open sockets, resolve DNS,
 * probe backends, or send HTTP traffic.</p>
 */
public final class PrivateNetworkLiveValidationGate {
    public static final String LIVE_ENABLED_FLAG =
            "loadbalancerpro.proxy.private-network-live-validation.enabled";
    public static final String OPERATOR_APPROVED_FLAG =
            "loadbalancerpro.proxy.private-network-live-validation.operator-approved";
    public static final String CONFIG_VALIDATION_FLAG =
            "loadbalancerpro.proxy.private-network-validation.enabled";

    public Result evaluate(ReverseProxyProperties properties) {
        if (properties == null) {
            return Result.blocked(List.of("loadbalancerpro.proxy configuration is required"));
        }
        ReverseProxyProperties.PrivateNetworkLiveValidation liveValidation =
                properties.getPrivateNetworkLiveValidation();
        if (!liveValidation.isEnabled()) {
            return Result.notEnabled(LIVE_ENABLED_FLAG + " is false");
        }

        List<String> reasons = new ArrayList<>();
        if (!liveValidation.isOperatorApproved()) {
            reasons.add(OPERATOR_APPROVED_FLAG + " must be true");
        }
        if (!properties.getPrivateNetworkValidation().isEnabled()) {
            reasons.add(CONFIG_VALIDATION_FLAG + " must be true");
        }
        if (!properties.isEnabled()) {
            reasons.add("loadbalancerpro.proxy.enabled must be true");
        }

        List<TargetReference> targets = targetReferences(properties);
        if (targets.isEmpty()) {
            reasons.add("at least one operator-provided backend URL is required");
        }

        List<BackendDecision> backendDecisions = targets.stream()
                .map(this::classify)
                .toList();
        backendDecisions.stream()
                .filter(decision -> !decision.allowed())
                .forEach(decision -> reasons.add(decision.label() + " blocked by classifier status="
                        + decision.status() + "; reason=" + decision.reason()));

        if (!reasons.isEmpty()) {
            return Result.blocked(reasons, backendDecisions);
        }
        return Result.allowed(backendDecisions);
    }

    private BackendDecision classify(TargetReference target) {
        ProxyBackendUrlClassifier.Classification classification =
                ProxyBackendUrlClassifier.classify(target.url());
        return new BackendDecision(
                target.label(),
                classification.status(),
                classification.reason(),
                classification.allowed());
    }

    private static List<TargetReference> targetReferences(ReverseProxyProperties properties) {
        if (!properties.getRoutes().isEmpty()) {
            List<TargetReference> targets = new ArrayList<>();
            for (Map.Entry<String, ReverseProxyProperties.Route> entry : properties.getRoutes().entrySet()) {
                String routeName = entry.getKey();
                ReverseProxyProperties.Route route = entry.getValue();
                List<ReverseProxyProperties.Upstream> routeTargets = route == null
                        ? List.of()
                        : route.getTargets();
                for (int index = 0; index < routeTargets.size(); index++) {
                    targets.add(targetReference(routeTargets.get(index),
                            "routes." + routeName + ".targets[" + index + "]"));
                }
            }
            return targets;
        }

        List<ReverseProxyProperties.Upstream> upstreams = properties.getUpstreams();
        List<TargetReference> targets = new ArrayList<>();
        for (int index = 0; index < upstreams.size(); index++) {
            targets.add(targetReference(upstreams.get(index), "upstreams[" + index + "]"));
        }
        return targets;
    }

    private static TargetReference targetReference(ReverseProxyProperties.Upstream upstream, String fallbackLabel) {
        if (upstream == null) {
            return new TargetReference(fallbackLabel, "");
        }
        String id = upstream.getId();
        String label = id == null || id.isBlank() ? fallbackLabel : fallbackLabel + "." + id.trim();
        return new TargetReference(label, upstream.getUrl());
    }

    public record Result(Status status, List<String> reasons, List<BackendDecision> backendDecisions) {
        public Result {
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
            backendDecisions = backendDecisions == null ? List.of() : List.copyOf(backendDecisions);
        }

        public boolean allowed() {
            return status == Status.ALLOWED;
        }

        static Result notEnabled(String reason) {
            return new Result(Status.NOT_ENABLED, List.of(Objects.requireNonNull(reason)),
                    List.of());
        }

        static Result blocked(List<String> reasons) {
            return blocked(reasons, List.of());
        }

        static Result blocked(List<String> reasons, List<BackendDecision> backendDecisions) {
            return new Result(Status.BLOCKED, reasons, backendDecisions);
        }

        static Result allowed(List<BackendDecision> backendDecisions) {
            return new Result(Status.ALLOWED, List.of(), backendDecisions);
        }
    }

    public enum Status {
        ALLOWED,
        NOT_ENABLED,
        BLOCKED
    }

    public record BackendDecision(
            String label,
            ProxyBackendUrlClassifier.Status status,
            String reason,
            boolean allowed) {
    }

    private record TargetReference(String label, String url) {
    }
}
