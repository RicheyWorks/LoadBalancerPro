package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CheckedInSecurityDefaultsTest {
    private static final Path DEFAULT_PROPERTIES =
            Path.of("src/main/resources/application.properties");
    private static final Path API_SECURITY = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/config/ApiSecurityConfiguration.java");

    @Test
    void proxyAndApiClassificationRemainFailClosedByDefault() throws IOException {
        String defaults = Files.readString(DEFAULT_PROPERTIES, StandardCharsets.UTF_8);
        String security = Files.readString(API_SECURITY, StandardCharsets.UTF_8);

        assertTrue(defaults.contains("loadbalancerpro.auth.mode=api-key"));
        assertTrue(defaults.contains("loadbalancerpro.proxy.enabled=false"));
        assertTrue(defaults.contains("loadbalancerpro.proxy.websocket.enabled=false"));
        assertFalse(defaults.contains("loadbalancerpro.proxy.enabled=true"));
        assertTrue(security.contains("requestMatchers(\"/api/proxy/**\").hasRole(allocationRole)"));
        assertTrue(security.contains("requestMatchers(\"/api/**\").denyAll()"));
    }
}
