package com.richmond423.loadbalancerpro.lab;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

/**
 * Repository-controlled target for bounded Enterprise Lab traffic.
 */
public record EnterpriseLabLoopbackTarget(
        String scenarioId,
        String backendId,
        URI requestUri) {
    private static final int MAX_ID_LENGTH = 128;

    public EnterpriseLabLoopbackTarget {
        scenarioId = requireCanonicalId(scenarioId, "scenarioId");
        backendId = requireCanonicalId(backendId, "backendId");
        requestUri = validateLoopbackUri(requestUri);
    }

    private static URI validateLoopbackUri(URI value) {
        URI uri = Objects.requireNonNull(value, "requestUri cannot be null").normalize();
        if (!uri.isAbsolute() || !"http".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("requestUri must use absolute loopback http");
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("requestUri cannot contain user info, query, or fragment data");
        }
        String host = normalizeHost(uri.getHost());
        if (!"127.0.0.1".equals(host) && !"::1".equals(host)) {
            throw new IllegalArgumentException("requestUri must use a literal IPv4 or IPv6 loopback address");
        }
        if (uri.getPort() < 1 || uri.getPort() > 65_535) {
            throw new IllegalArgumentException("requestUri must use an explicit valid port");
        }
        if (uri.getRawPath() == null || uri.getRawPath().isBlank() || !uri.getRawPath().startsWith("/")) {
            throw new IllegalArgumentException("requestUri must use an absolute path");
        }
        return uri;
    }

    private static String normalizeHost(String value) {
        if (value == null) {
            return "";
        }
        String host = value.toLowerCase(Locale.ROOT);
        if (host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static String requireCanonicalId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        if (!value.equals(value.trim()) || value.length() > MAX_ID_LENGTH
                || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(fieldName + " must be a bounded canonical identifier");
        }
        return value;
    }
}
