package com.richmond423.loadbalancerpro.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadBalancerApiApplicationTest {
    @Test
    void laseDemoFlagSkipsApiServerStartup() {
        assertFalse(LoadBalancerApiApplication.shouldStartApi(new String[]{"--lase-demo"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(new String[]{"--lase-demo=healthy"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(new String[]{"--lase-replay=shadow-events.jsonl"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(new String[]{"--adaptive-routing-experiment"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(new String[]{"--adaptive-routing-experiment=all"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--enterprise-lab-experiment-proof=completion"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--enterprise-lab-durable-recovery-proof"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--enterprise-lab-ownership-proof"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--enterprise-lab-ownership-proof-child=contend"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--enterprise-lab-allocation-proof"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--enterprise-lab-allocation-proof-holder"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--enterprise-lab-supervisor"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(new String[]{"--version"}));
        assertTrue(LoadBalancerApiApplication.shouldStartApi(new String[]{"--server.port=18080"}));
        assertTrue(LoadBalancerApiApplication.shouldStartApi(new String[]{}));
    }

    @Test
    void versionFallsBackWhenPackageMetadataIsUnavailable() {
        assertTrue(LoadBalancerApiApplication.isVersionRequested(new String[]{"--version"}));
        assertFalse(LoadBalancerApiApplication.isVersionRequested(new String[]{"--server.port=18080"}));
        assertEquals("2.5.0", LoadBalancerApiApplication.version());
    }
}
