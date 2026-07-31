package com.richmond423.loadbalancerpro.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadBalancerApiApplicationTest {
    @Test
    void productionCommandsSkipApiServerStartup() {
        assertFalse(LoadBalancerApiApplication.shouldStartApi(new String[]{"--lase-demo"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(new String[]{"--lase-demo=healthy"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(new String[]{"--lase-replay=shadow-events.jsonl"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(new String[]{"--adaptive-routing-experiment"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(new String[]{"--adaptive-routing-experiment=all"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--enterprise-lab-supervisor"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(
                new String[]{
                        "--enterprise-lab-storage-repair",
                        "--enterprise-lab-storage-repair-data-root=C:\\bounded-root",
                        "--enterprise-lab-storage-repair-store=application-ledger"
                }));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--remediation-report", "--input", "saved-evaluation.json"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--bundle", "incident-bundle.zip", "--input", "saved-evaluation.json"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--inventory", "incident-evidence"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--list-policy-templates"}));
        assertFalse(LoadBalancerApiApplication.shouldStartApi(new String[]{"--version"}));
        assertTrue(LoadBalancerApiApplication.shouldStartApi(new String[]{"--server.port=18080"}));
        assertTrue(LoadBalancerApiApplication.shouldStartApi(new String[]{}));
    }

    @Test
    void proofToolFlagsAreNotProductionCliDispatch() {
        assertTrue(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--enterprise-lab-experiment-proof=completion"}));
        assertTrue(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--enterprise-lab-durable-recovery-proof"}));
        assertTrue(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--enterprise-lab-ownership-proof"}));
        assertTrue(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--enterprise-lab-allocation-proof"}));
        assertTrue(LoadBalancerApiApplication.shouldStartApi(
                new String[]{"--enterprise-lab-independent-supervisor-proof"}));
    }

    @Test
    void versionFallsBackWhenPackageMetadataIsUnavailable() {
        assertTrue(LoadBalancerApiApplication.isVersionRequested(new String[]{"--version"}));
        assertFalse(LoadBalancerApiApplication.isVersionRequested(new String[]{"--server.port=18080"}));
        assertEquals("2.5.0", LoadBalancerApiApplication.version());
    }
}
