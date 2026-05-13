package com.richmond423.loadbalancerpro.api.proxy;

import java.net.URI;
import java.util.Locale;

final class PrivateNetworkLiveValidationRequestPathValidator {
    private PrivateNetworkLiveValidationRequestPathValidator() {
    }

    static String invalidReason(String pathAndQuery) {
        if (pathAndQuery == null || pathAndQuery.isBlank()) {
            return "validation request path must not be blank";
        }
        if (containsControlCharacter(pathAndQuery)) {
            return "validation request path must not contain control characters";
        }
        if (pathAndQuery.contains("\\")) {
            return "validation request path must not contain backslash characters";
        }
        URI uri;
        try {
            uri = URI.create(pathAndQuery);
        } catch (IllegalArgumentException exception) {
            return "validation request path must be a valid URI path";
        }
        if (uri.isAbsolute() || uri.getRawAuthority() != null) {
            return "validation request path must be relative";
        }
        if (uri.getRawFragment() != null) {
            return "validation request path must not include a fragment";
        }
        if (uri.getRawQuery() != null) {
            return "validation request path must not include a query string";
        }
        String rawPath = uri.getRawPath();
        String decodedPath = uri.getPath();
        if (rawPath == null || !rawPath.startsWith("/")) {
            return "validation request path must start with /";
        }
        if (rawPath.startsWith("//")) {
            return "validation request path must not start with //";
        }
        if (rawPath.contains("\\") || decodedPath.contains("\\")) {
            return "validation request path must not contain backslash characters";
        }
        if (containsControlCharacter(decodedPath)) {
            return "validation request path must not contain encoded control characters";
        }
        if (containsEncodedTraversalToken(rawPath)
                || containsTraversalSegment(rawPath)
                || containsTraversalSegment(decodedPath)) {
            return "validation request path must not contain traversal segments";
        }
        return "";
    }

    static String safePathOrEmpty(String pathAndQuery) {
        String invalidReason = invalidReason(pathAndQuery);
        if (!invalidReason.isBlank()) {
            return "";
        }
        return URI.create(pathAndQuery).getRawPath();
    }

    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(character -> character < 0x20 || character == 0x7f);
    }

    private static boolean containsEncodedTraversalToken(String rawPath) {
        String normalized = rawPath.toLowerCase(Locale.ROOT);
        return normalized.contains("%2e") || normalized.contains("%2f") || normalized.contains("%5c");
    }

    private static boolean containsTraversalSegment(String path) {
        for (String segment : path.split("/", -1)) {
            if (".".equals(segment) || "..".equals(segment)) {
                return true;
            }
        }
        return false;
    }
}
