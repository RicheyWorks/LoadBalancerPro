package com.richmond423.loadbalancerpro.lab;

import java.util.Locale;

public enum EnterpriseLabMode {
    SHADOW("shadow", false),
    INFLUENCE("influence", true),
    ALL("all", true);

    private final String wireValue;
    private final boolean activeInfluenceEnabled;

    EnterpriseLabMode(String wireValue, boolean activeInfluenceEnabled) {
        this.wireValue = wireValue;
        this.activeInfluenceEnabled = activeInfluenceEnabled;
    }

    public String wireValue() {
        return wireValue;
    }

    public boolean activeInfluenceEnabled() {
        return activeInfluenceEnabled;
    }

    public static EnterpriseLabMode from(String value) {
        if (value == null || value.isBlank()) {
            return SHADOW;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (EnterpriseLabMode mode : values()) {
            if (mode.wireValue.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported lab mode: " + value + ". Valid values: shadow, influence, all");
    }
}

