package com.richmond423.loadbalancerpro.api;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class DecisionExplorerDiagnosticListSupport {
    private DecisionExplorerDiagnosticListSupport() {
    }

    static <T> List<T> copyNonNull(List<T> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    static List<String> distinctSorted(Collection<String> values) {
        if (values == null) {
            return List.of();
        }
        Set<String> distinct = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return distinct.stream()
                .sorted()
                .toList();
    }

    static List<String> distinctSortedNormalizedWhitespace(Collection<String> values) {
        if (values == null) {
            return List.of();
        }
        Set<String> distinct = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(DecisionExplorerDiagnosticListSupport::normalizedWhitespace)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return distinct.stream()
                .sorted()
                .toList();
    }

    private static String normalizedWhitespace(String value) {
        return value.trim()
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ");
    }
}
