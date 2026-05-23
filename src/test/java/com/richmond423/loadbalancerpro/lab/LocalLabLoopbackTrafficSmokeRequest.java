package com.richmond423.loadbalancerpro.lab;

import java.net.URI;
import java.util.List;

record LocalLabLoopbackTrafficSmokeRequest(
        String scenarioId,
        String backendId,
        URI loopbackUri,
        String requestMethodLabel,
        String requestPathLabel,
        int expectedStatusCode,
        String expectedLatencyLabel,
        String expectedErrorLoadLabel,
        String expectedEvidenceLabel,
        String safetyBoundaryLabel,
        String notProvenBoundaryLabel) {
    private static final List<Integer> COMMON_FIXED_PORTS = List.of(0, 80, 443, 8080, 8081, 9090);

    LocalLabLoopbackTrafficSmokeRequest {
        requireText("scenarioId", scenarioId);
        requireText("backendId", backendId);
        if (loopbackUri == null) {
            throw new IllegalArgumentException("loopbackUri is required");
        }
        validateLoopbackUri(loopbackUri);
        requireText("requestMethodLabel", requestMethodLabel);
        requireText("requestPathLabel", requestPathLabel);
        if (expectedStatusCode < 100 || expectedStatusCode > 599) {
            throw new IllegalArgumentException("expectedStatusCode must be an HTTP-style status code");
        }
        requireText("expectedLatencyLabel", expectedLatencyLabel);
        requireText("expectedErrorLoadLabel", expectedErrorLoadLabel);
        requireText("expectedEvidenceLabel", expectedEvidenceLabel);
        requireText("safetyBoundaryLabel", safetyBoundaryLabel);
        requireText("notProvenBoundaryLabel", notProvenBoundaryLabel);
    }

    String deterministicText() {
        return String.join(" ",
                scenarioId,
                backendId,
                loopbackUri.toString(),
                requestMethodLabel,
                requestPathLabel,
                Integer.toString(expectedStatusCode),
                expectedLatencyLabel,
                expectedErrorLoadLabel,
                expectedEvidenceLabel,
                safetyBoundaryLabel,
                notProvenBoundaryLabel);
    }

    private static void validateLoopbackUri(URI uri) {
        if (!"http".equals(uri.getScheme())) {
            throw new IllegalArgumentException("loopbackUri must use the test-scope http scheme");
        }
        if (!"127.0.0.1".equals(uri.getHost())) {
            throw new IllegalArgumentException("loopbackUri must target 127.0.0.1 only");
        }
        if (uri.getPort() <= 0) {
            throw new IllegalArgumentException("loopbackUri must use an assigned positive ephemeral port");
        }
        if (COMMON_FIXED_PORTS.contains(uri.getPort())) {
            throw new IllegalArgumentException("loopbackUri must not use common fixed ports");
        }
        if (!LocalLabLoopbackFakeBackendServer.ENDPOINT_PATH.equals(uri.getPath())) {
            throw new IllegalArgumentException("loopbackUri must target the test-scope fake backend endpoint");
        }
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
