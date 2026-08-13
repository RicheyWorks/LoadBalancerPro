package com.richmond423.loadbalancerpro.core;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable public identity for a routing strategy.
 *
 * <p>{@link RoutingStrategyId} remains the catalog of built-in strategies. External
 * strategies can use {@link #of(String)} without extending that closed enum.</p>
 */
public interface RoutingStrategyIdentifier {
    String externalName();

    default String canonicalName() {
        return canonicalExternalName(externalName());
    }

    default boolean sameIdentifierAs(RoutingStrategyIdentifier other) {
        return other != null && canonicalName().equals(other.canonicalName());
    }

    static RoutingStrategyIdentifier of(String externalName) {
        return new NamedRoutingStrategyIdentifier(canonicalExternalName(externalName));
    }

    static String canonicalExternalName(String value) {
        Objects.requireNonNull(value, "routing strategy external name cannot be null");
        String canonical = value.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        if (!RoutingStrategyIdentifierNames.EXTERNAL_NAME.matcher(canonical).matches()) {
            throw new IllegalArgumentException(
                    "routing strategy external name must match "
                            + RoutingStrategyIdentifierNames.EXTERNAL_NAME.pattern());
        }
        return canonical;
    }
}

final class RoutingStrategyIdentifierNames {
    static final Pattern EXTERNAL_NAME = Pattern.compile("[A-Z0-9][A-Z0-9_.]{0,127}");

    private RoutingStrategyIdentifierNames() {
    }
}

record NamedRoutingStrategyIdentifier(String externalName) implements RoutingStrategyIdentifier {
    NamedRoutingStrategyIdentifier {
        externalName = RoutingStrategyIdentifier.canonicalExternalName(externalName);
    }

    @Override
    public String toString() {
        return externalName;
    }
}
