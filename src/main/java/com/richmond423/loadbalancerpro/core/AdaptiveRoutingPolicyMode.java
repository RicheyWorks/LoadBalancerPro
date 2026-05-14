package com.richmond423.loadbalancerpro.core;

import java.util.List;
import java.util.Locale;

public enum AdaptiveRoutingPolicyMode {
    OFF("off", false),
    SHADOW("shadow", false),
    RECOMMEND("recommend", false),
    ACTIVE_EXPERIMENT("active-experiment", true);

    private static final List<String> WIRE_VALUES = List.of(
            OFF.wireValue,
            SHADOW.wireValue,
            RECOMMEND.wireValue,
            ACTIVE_EXPERIMENT.wireValue);

    private final String wireValue;
    private final boolean activeInfluenceAllowed;

    AdaptiveRoutingPolicyMode(String wireValue, boolean activeInfluenceAllowed) {
        this.wireValue = wireValue;
        this.activeInfluenceAllowed = activeInfluenceAllowed;
    }

    public String wireValue() {
        return wireValue;
    }

    public boolean activeInfluenceAllowed() {
        return activeInfluenceAllowed;
    }

    public static List<String> wireValues() {
        return WIRE_VALUES;
    }

    public static AdaptiveRoutingPolicyMode from(String value) {
        if (value == null || value.isBlank()) {
            return OFF;
        }
        String normalized = normalize(value);
        return switch (normalized) {
            case "off", "disabled" -> OFF;
            case "shadow", "shadow-only" -> SHADOW;
            case "recommend", "recommendation" -> RECOMMEND;
            case "active-experiment", "active", "influence", "all" -> ACTIVE_EXPERIMENT;
            default -> throw new IllegalArgumentException("Unsupported adaptive routing policy mode: " + value
                    + ". Valid values: off, shadow, recommend, active-experiment");
        };
    }

    public static AdaptiveRoutingPolicyMode fromOrOff(String value) {
        try {
            return from(value);
        } catch (IllegalArgumentException exception) {
            return OFF;
        }
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
