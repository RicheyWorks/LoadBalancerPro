package com.richmond423.loadbalancerpro.api;

import java.util.List;

final class DecisionExplorerDtoSupport {
    private static final String UNKNOWN = "UNKNOWN";

    private DecisionExplorerDtoSupport() {
    }

    static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    static String valueOrUnknown(String value) {
        return valueOrDefault(value, UNKNOWN);
    }

    static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
