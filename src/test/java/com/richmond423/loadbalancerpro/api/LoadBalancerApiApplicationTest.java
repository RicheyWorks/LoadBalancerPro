package com.richmond423.loadbalancerpro.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadBalancerApiApplicationTest {
    @Test
    void versionFallsBackWhenPackageMetadataIsUnavailable() {
        assertTrue(LoadBalancerApiApplication.isVersionRequested(new String[]{"--version"}));
        assertFalse(LoadBalancerApiApplication.isVersionRequested(new String[]{"--server.port=18080"}));
        assertEquals("2.5.0", LoadBalancerApiApplication.version());
    }
}
