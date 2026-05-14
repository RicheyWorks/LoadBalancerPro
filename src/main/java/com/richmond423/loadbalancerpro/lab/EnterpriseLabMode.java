package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;

import java.util.List;
import java.util.Locale;

public enum EnterpriseLabMode {
    OFF("off", AdaptiveRoutingPolicyMode.OFF),
    SHADOW("shadow", AdaptiveRoutingPolicyMode.SHADOW),
    RECOMMEND("recommend", AdaptiveRoutingPolicyMode.RECOMMEND),
    ACTIVE_EXPERIMENT("active-experiment", AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT);

    private final String wireValue;
    private final AdaptiveRoutingPolicyMode policyMode;

    EnterpriseLabMode(String wireValue, AdaptiveRoutingPolicyMode policyMode) {
        this.wireValue = wireValue;
        this.policyMode = policyMode;
    }

    public String wireValue() {
        return wireValue;
    }

    public boolean activeInfluenceEnabled() {
        return policyMode.activeInfluenceAllowed();
    }

    public AdaptiveRoutingPolicyMode policyMode() {
        return policyMode;
    }

    public static List<String> wireValues() {
        return List.of(OFF.wireValue, SHADOW.wireValue, RECOMMEND.wireValue, ACTIVE_EXPERIMENT.wireValue);
    }

    public static EnterpriseLabMode from(String value) {
        if (value == null || value.isBlank()) {
            return SHADOW;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if ("influence".equals(normalized) || "all".equals(normalized) || "active".equals(normalized)) {
            return ACTIVE_EXPERIMENT;
        }
        for (EnterpriseLabMode mode : values()) {
            if (mode.wireValue.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported lab mode: " + value
                + ". Valid values: off, shadow, recommend, active-experiment");
    }
}

