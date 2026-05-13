package com.richmond423.loadbalancerpro.api.proxy;

import static com.richmond423.loadbalancerpro.api.proxy.PrivateNetworkLiveValidationGate.Status.ALLOWED;
import static com.richmond423.loadbalancerpro.api.proxy.PrivateNetworkLiveValidationGate.Status.BLOCKED;
import static com.richmond423.loadbalancerpro.api.proxy.PrivateNetworkLiveValidationGate.Status.NOT_ENABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class PrivateNetworkLiveValidationGateTest {
    private static final Path GATE_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/PrivateNetworkLiveValidationGate.java");
    private static final Path APPLICATION_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path SMOKE_SCRIPTS = Path.of("scripts/smoke");
    private static final Path POSTMAN_DOCS = Path.of("docs/postman");

    private final PrivateNetworkLiveValidationGate gate = new PrivateNetworkLiveValidationGate();

    @Test
    void liveValidationIsDefaultDisabled() throws Exception {
        ReverseProxyProperties properties = propertiesWithTarget("local", "http://127.0.0.1:18081");

        PrivateNetworkLiveValidationGate.Result result = gate.evaluate(properties);

        assertEquals(NOT_ENABLED, result.status());
        assertFalse(result.allowed());
        assertTrue(result.reasons().get(0).contains(
                "loadbalancerpro.proxy.private-network-live-validation.enabled is false"));
        String defaults = read(APPLICATION_PROPERTIES);
        assertTrue(defaults.contains("loadbalancerpro.proxy.private-network-live-validation.enabled=false"));
        assertTrue(defaults.contains("loadbalancerpro.proxy.private-network-live-validation.operator-approved=false"));
    }

    @Test
    void enabledWithoutOperatorApprovalIsBlocked() {
        ReverseProxyProperties properties = propertiesWithTarget("local", "http://127.0.0.1:18081");
        properties.getPrivateNetworkLiveValidation().setEnabled(true);
        properties.getPrivateNetworkValidation().setEnabled(true);

        PrivateNetworkLiveValidationGate.Result result = gate.evaluate(properties);

        assertEquals(BLOCKED, result.status());
        assertTrue(result.reasons().contains(
                "loadbalancerpro.proxy.private-network-live-validation.operator-approved must be true"));
    }

    @Test
    void operatorApprovalWithoutLiveFlagDoesNotEnableValidation() {
        ReverseProxyProperties properties = propertiesWithTarget("local", "http://127.0.0.1:18081");
        properties.getPrivateNetworkLiveValidation().setOperatorApproved(true);
        properties.getPrivateNetworkValidation().setEnabled(true);

        PrivateNetworkLiveValidationGate.Result result = gate.evaluate(properties);

        assertEquals(NOT_ENABLED, result.status());
        assertFalse(result.allowed());
    }

    @Test
    void configValidationDisabledBlocksLiveValidation() {
        ReverseProxyProperties properties = propertiesWithTarget("local", "http://127.0.0.1:18081");
        properties.getPrivateNetworkLiveValidation().setEnabled(true);
        properties.getPrivateNetworkLiveValidation().setOperatorApproved(true);

        PrivateNetworkLiveValidationGate.Result result = gate.evaluate(properties);

        assertEquals(BLOCKED, result.status());
        assertTrue(result.reasons().contains(
                "loadbalancerpro.proxy.private-network-validation.enabled must be true"));
    }

    @Test
    void proxyDisabledBlocksLiveValidation() {
        ReverseProxyProperties properties = propertiesWithTarget("local", "http://127.0.0.1:18081");
        properties.setEnabled(false);
        enableAllLiveGateFlags(properties);

        PrivateNetworkLiveValidationGate.Result result = gate.evaluate(properties);

        assertEquals(BLOCKED, result.status());
        assertTrue(result.reasons().contains("loadbalancerpro.proxy.enabled must be true"));
    }

    @Test
    void publicDomainUserinfoUnsupportedAndMalformedTargetsAreBlocked() {
        for (String url : List.of(
                "http://8.8.8.8:18081",
                "http://example.com:18081",
                "http://user:pass@127.0.0.1:18081",
                "ftp://127.0.0.1:18081",
                "http://[bad")) {
            ReverseProxyProperties properties = propertiesWithTarget("candidate", url);
            enableAllLiveGateFlags(properties);

            PrivateNetworkLiveValidationGate.Result result = gate.evaluate(properties);

            assertEquals(BLOCKED, result.status(), url);
            assertFalse(result.allowed(), url);
            assertTrue(result.backendDecisions().stream().anyMatch(decision -> !decision.allowed()), url);
        }
    }

    @Test
    void loopbackAndPrivateLiteralTargetsPassTheOfflineGate() {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setUpstreams(List.of(
                upstream("loopback", "http://127.0.0.1:18081"),
                upstream("private", "http://10.1.2.3:18082"),
                upstream("unique-local", "http://[fd12:3456:789a::1]:18083")));
        enableAllLiveGateFlags(properties);

        PrivateNetworkLiveValidationGate.Result result = gate.evaluate(properties);

        assertEquals(ALLOWED, result.status());
        assertTrue(result.allowed());
        assertEquals(3, result.backendDecisions().size());
        assertTrue(result.backendDecisions().stream().allMatch(PrivateNetworkLiveValidationGate.BackendDecision::allowed));
    }

    @Test
    void gateDoesNotSendNetworkTrafficWhenNoBackendIsRunning() {
        ReverseProxyProperties properties = propertiesWithTarget("not-running", "http://127.0.0.1:9");
        enableAllLiveGateFlags(properties);

        PrivateNetworkLiveValidationGate.Result result = gate.evaluate(properties);

        assertEquals(ALLOWED, result.status());
        assertTrue(result.allowed());
    }

    @Test
    void gateSourceStaysOfflineAndDoesNotResolveProbeOrScan() throws Exception {
        String source = read(GATE_SOURCE);

        for (String forbidden : List.of(
                "InetAddress",
                "getByName",
                "HttpClient",
                "URLConnection",
                "new Socket",
                "DatagramSocket",
                ".connect(",
                "isReachable")) {
            assertFalse(source.contains(forbidden), "live validation gate must stay offline; found " + forbidden);
        }
    }

    @Test
    void smokeAndPostmanPathsDoNotRunPrivateNetworkLiveValidation() throws Exception {
        String combined = readTree(SMOKE_SCRIPTS) + "\n" + readTree(POSTMAN_DOCS);

        assertFalse(combined.contains("private-network-live-validation"),
                "smoke/Postman paths must not add live private-network validation execution");
        assertFalse(combined.contains("PrivateNetworkLiveValidationGate"),
                "smoke/Postman paths must not invoke the live gate");
    }

    private static ReverseProxyProperties propertiesWithTarget(String id, String url) {
        ReverseProxyProperties properties = new ReverseProxyProperties();
        properties.setEnabled(true);
        properties.setUpstreams(List.of(upstream(id, url)));
        return properties;
    }

    private static ReverseProxyProperties.Upstream upstream(String id, String url) {
        ReverseProxyProperties.Upstream upstream = new ReverseProxyProperties.Upstream();
        upstream.setId(id);
        upstream.setUrl(url);
        upstream.setWeight(1.0);
        return upstream;
    }

    private static void enableAllLiveGateFlags(ReverseProxyProperties properties) {
        properties.getPrivateNetworkLiveValidation().setEnabled(true);
        properties.getPrivateNetworkLiveValidation().setOperatorApproved(true);
        properties.getPrivateNetworkValidation().setEnabled(true);
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String readTree(Path root) throws IOException {
        assertTrue(Files.exists(root), root + " should exist");
        StringBuilder content = new StringBuilder();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                content.append(read(path)).append('\n');
            }
        }
        return content.toString();
    }
}
