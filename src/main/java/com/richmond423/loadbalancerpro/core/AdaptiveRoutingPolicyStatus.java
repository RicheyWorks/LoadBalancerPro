package com.richmond423.loadbalancerpro.core;

import java.util.List;

public record AdaptiveRoutingPolicyStatus(
        String configuredMode,
        String currentMode,
        boolean activeExperimentEnabled,
        List<String> allowedModes,
        int retainedAuditEventCount,
        int maxRetainedAuditEvents,
        String lastGuardrailReason,
        String warning) {
}
