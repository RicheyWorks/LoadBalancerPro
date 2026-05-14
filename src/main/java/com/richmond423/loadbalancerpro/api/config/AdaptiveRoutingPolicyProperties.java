package com.richmond423.loadbalancerpro.api.config;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loadbalancerpro.lase.policy")
public class AdaptiveRoutingPolicyProperties {
    private String mode = AdaptiveRoutingPolicyMode.OFF.wireValue();
    private boolean activeExperimentEnabled = false;
    private int maxAuditEvents = 100;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode == null ? AdaptiveRoutingPolicyMode.OFF.wireValue() : mode;
    }

    public boolean isActiveExperimentEnabled() {
        return activeExperimentEnabled;
    }

    public void setActiveExperimentEnabled(boolean activeExperimentEnabled) {
        this.activeExperimentEnabled = activeExperimentEnabled;
    }

    public int getMaxAuditEvents() {
        return maxAuditEvents;
    }

    public void setMaxAuditEvents(int maxAuditEvents) {
        this.maxAuditEvents = Math.max(1, maxAuditEvents);
    }

    public AdaptiveRoutingPolicyMode configuredMode() {
        return AdaptiveRoutingPolicyMode.fromOrOff(mode);
    }

    public AdaptiveRoutingPolicyMode resolvedMode() {
        AdaptiveRoutingPolicyMode configured = configuredMode();
        if (configured == AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT && !activeExperimentEnabled) {
            return AdaptiveRoutingPolicyMode.OFF;
        }
        return configured;
    }
}
