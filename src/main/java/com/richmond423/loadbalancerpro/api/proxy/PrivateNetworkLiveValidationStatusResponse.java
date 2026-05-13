package com.richmond423.loadbalancerpro.api.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record PrivateNetworkLiveValidationStatusResponse(
        boolean liveValidationEnabled,
        boolean operatorApproved,
        boolean configValidationEnabled,
        boolean proxyEnabled,
        String gateStatus,
        boolean allowedByGate,
        boolean trafficExecuted,
        String trafficExecution,
        String note,
        List<String> reasonCodes,
        List<String> reasons,
        List<BackendStatus> backends) {

    private static final String NOT_EXECUTED = "traffic not executed by this report";

    public PrivateNetworkLiveValidationStatusResponse {
        gateStatus = gateStatus == null ? "" : gateStatus;
        trafficExecution = trafficExecution == null || trafficExecution.isBlank() ? NOT_EXECUTED : trafficExecution;
        note = note == null || note.isBlank()
                ? "Status/report only; no live validation request was sent."
                : note;
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        backends = backends == null ? List.of() : List.copyOf(backends);
    }

    static PrivateNetworkLiveValidationStatusResponse from(ReverseProxyProperties properties) {
        PrivateNetworkLiveValidationGate.Result result = new PrivateNetworkLiveValidationGate().evaluate(properties);
        ReverseProxyProperties safeProperties = properties == null ? new ReverseProxyProperties() : properties;
        List<BackendStatus> backends = backendStatuses(result, safeProperties);
        return new PrivateNetworkLiveValidationStatusResponse(
                safeProperties.getPrivateNetworkLiveValidation().isEnabled(),
                safeProperties.getPrivateNetworkLiveValidation().isOperatorApproved(),
                safeProperties.getPrivateNetworkValidation().isEnabled(),
                safeProperties.isEnabled(),
                result.status().name(),
                result.allowed(),
                false,
                NOT_EXECUTED,
                "This report evaluates the offline gate only; it does not call the live executor or send traffic.",
                reasonCodes(result),
                result.reasons(),
                backends);
    }

    private static List<BackendStatus> backendStatuses(PrivateNetworkLiveValidationGate.Result result,
                                                       ReverseProxyProperties properties) {
        if (!result.backendDecisions().isEmpty()) {
            return result.backendDecisions().stream()
                    .map(PrivateNetworkLiveValidationStatusResponse::backendStatus)
                    .toList();
        }
        return targetReferences(properties).stream()
                .map(PrivateNetworkLiveValidationStatusResponse::backendStatus)
                .toList();
    }

    private static BackendStatus backendStatus(PrivateNetworkLiveValidationGate.BackendDecision decision) {
        return new BackendStatus(
                decision.label(),
                decision.status().name(),
                decision.allowed(),
                decision.reason(),
                decision.allowed() ? decision.normalizedUrl() : "");
    }

    private static BackendStatus backendStatus(TargetReference target) {
        ProxyBackendUrlClassifier.Classification classification = ProxyBackendUrlClassifier.classify(target.url());
        return new BackendStatus(
                target.label(),
                classification.status().name(),
                classification.allowed(),
                classification.reason(),
                classification.allowed() ? classification.normalizedUrl() : "");
    }

    private static List<TargetReference> targetReferences(ReverseProxyProperties properties) {
        if (!properties.getRoutes().isEmpty()) {
            List<TargetReference> targets = new ArrayList<>();
            for (Map.Entry<String, ReverseProxyProperties.Route> entry : properties.getRoutes().entrySet()) {
                ReverseProxyProperties.Route route = entry.getValue();
                List<ReverseProxyProperties.Upstream> routeTargets = route == null
                        ? List.of()
                        : route.getTargets();
                for (int index = 0; index < routeTargets.size(); index++) {
                    targets.add(targetReference(routeTargets.get(index),
                            "routes." + entry.getKey() + ".targets[" + index + "]"));
                }
            }
            return targets;
        }

        List<TargetReference> targets = new ArrayList<>();
        List<ReverseProxyProperties.Upstream> upstreams = properties.getUpstreams();
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

    private static List<String> reasonCodes(PrivateNetworkLiveValidationGate.Result result) {
        if (result.allowed()) {
            return List.of("ALLOWED_BY_GATE");
        }
        return result.reasons().stream()
                .map(PrivateNetworkLiveValidationStatusResponse::reasonCode)
                .distinct()
                .toList();
    }

    private static String reasonCode(String reason) {
        String normalized = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
        if (normalized.contains(PrivateNetworkLiveValidationGate.LIVE_ENABLED_FLAG)) {
            return "LIVE_VALIDATION_DISABLED";
        }
        if (normalized.contains(PrivateNetworkLiveValidationGate.OPERATOR_APPROVED_FLAG)) {
            return "OPERATOR_APPROVAL_REQUIRED";
        }
        if (normalized.contains(PrivateNetworkLiveValidationGate.CONFIG_VALIDATION_FLAG)) {
            return "CONFIG_VALIDATION_REQUIRED";
        }
        if (normalized.contains("loadbalancerpro.proxy.enabled")) {
            return "PROXY_ENABLED_REQUIRED";
        }
        if (normalized.contains("operator-provided backend url")) {
            return "BACKEND_URL_REQUIRED";
        }
        if (normalized.contains("blocked by classifier")) {
            return "BACKEND_CLASSIFIER_REJECTED";
        }
        if (normalized.contains("configuration is required")) {
            return "PROXY_CONFIGURATION_REQUIRED";
        }
        return "GATE_BLOCKED";
    }

    public record BackendStatus(
            String label,
            String classifierStatus,
            boolean classifierApproved,
            String reason,
            String normalizedUrl) {
        public BackendStatus {
            label = label == null ? "" : label;
            classifierStatus = classifierStatus == null ? "" : classifierStatus;
            reason = reason == null ? "" : reason;
            normalizedUrl = normalizedUrl == null ? "" : normalizedUrl;
        }
    }

    private record TargetReference(String label, String url) {
    }
}
