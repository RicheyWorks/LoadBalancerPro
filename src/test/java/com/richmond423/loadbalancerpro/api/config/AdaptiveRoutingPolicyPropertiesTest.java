package com.richmond423.loadbalancerpro.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import org.junit.jupiter.api.Test;

class AdaptiveRoutingPolicyPropertiesTest {

    @Test
    void defaultsAreSafeAndInvalidModeFailsClosedToOff() {
        AdaptiveRoutingPolicyProperties properties = new AdaptiveRoutingPolicyProperties();

        assertEquals(AdaptiveRoutingPolicyMode.OFF, properties.configuredMode());
        assertEquals(AdaptiveRoutingPolicyMode.OFF, properties.resolvedMode());

        properties.setMode("live");
        assertEquals(AdaptiveRoutingPolicyMode.OFF, properties.configuredMode());
        assertEquals(AdaptiveRoutingPolicyMode.OFF, properties.resolvedMode());
    }

    @Test
    void activeExperimentRequiresSeparateEnableFlag() {
        AdaptiveRoutingPolicyProperties properties = new AdaptiveRoutingPolicyProperties();
        properties.setMode("active-experiment");

        assertEquals(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, properties.configuredMode());
        assertEquals(AdaptiveRoutingPolicyMode.OFF, properties.resolvedMode());

        properties.setActiveExperimentEnabled(true);
        assertEquals(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT, properties.resolvedMode());
    }
}
