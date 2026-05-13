package com.richmond423.loadbalancerpro.api.proxy;

import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.AMBIGUOUS_HOST_REJECTED;
import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.INVALID_REJECTED;
import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.LOOPBACK_ALLOWED;
import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.PRIVATE_NETWORK_ALLOWED;
import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.PUBLIC_NETWORK_REJECTED;
import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.UNSUPPORTED_SCHEME_REJECTED;
import static com.richmond423.loadbalancerpro.api.proxy.ProxyBackendUrlClassifier.Status.USERINFO_REJECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ProxyBackendUrlClassifierTest {
    private static final Path CLASSIFIER_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/ProxyBackendUrlClassifier.java");
    private static final Path ROUTE_PLANNER_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyRoutePlanner.java");
    private static final Path LIVE_GATE_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/PrivateNetworkLiveValidationGate.java");
    private static final Path LIVE_STATUS_REPORT_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/"
                    + "PrivateNetworkLiveValidationStatusResponse.java");

    @Test
    void loopbackHostsAndAddressesAreAllowed() {
        assertStatus("http://localhost:9001", LOOPBACK_ALLOWED);
        assertStatus("https://LOCALHOST/api", LOOPBACK_ALLOWED);
        assertStatus("http://127.0.0.1:8080", LOOPBACK_ALLOWED);
        assertStatus("http://127.42.0.1/health", LOOPBACK_ALLOWED);
        assertStatus("https://[::1]:8443/health", LOOPBACK_ALLOWED);
    }

    @Test
    void rfc1918AndUniqueLocalPrivateAddressesAreAllowed() {
        assertStatus("http://10.1.2.3:9001", PRIVATE_NETWORK_ALLOWED);
        assertStatus("http://172.16.0.10", PRIVATE_NETWORK_ALLOWED);
        assertStatus("http://172.31.255.254", PRIVATE_NETWORK_ALLOWED);
        assertStatus("https://192.168.1.10/api", PRIVATE_NETWORK_ALLOWED);
        assertStatus("http://[fc00::1]:9001", PRIVATE_NETWORK_ALLOWED);
        assertStatus("http://[fd12:3456:789a::1]", PRIVATE_NETWORK_ALLOWED);
    }

    @Test
    void publicIpAddressesAreRejected() {
        assertStatus("http://8.8.8.8", PUBLIC_NETWORK_REJECTED);
        assertStatus("http://1.1.1.1:8080", PUBLIC_NETWORK_REJECTED);
        assertStatus("http://172.15.255.255", PUBLIC_NETWORK_REJECTED);
        assertStatus("http://172.32.0.1", PUBLIC_NETWORK_REJECTED);
        assertStatus("http://192.169.0.1", PUBLIC_NETWORK_REJECTED);
        assertStatus("http://[2001:4860:4860::8888]", PUBLIC_NETWORK_REJECTED);
    }

    @Test
    void domainsAreRejectedAsAmbiguousWithoutDnsResolution() {
        assertStatus("http://example.com", AMBIGUOUS_HOST_REJECTED);
        assertStatus("http://internal.service.local", AMBIGUOUS_HOST_REJECTED);
        assertStatus("http://localhost.example.com", AMBIGUOUS_HOST_REJECTED);
        assertStatus("http://service-name", AMBIGUOUS_HOST_REJECTED);
    }

    @Test
    void unsupportedSchemesAreRejected() {
        assertStatus("file:///tmp/backend", UNSUPPORTED_SCHEME_REJECTED);
        assertStatus("ftp://127.0.0.1/resource", UNSUPPORTED_SCHEME_REJECTED);
        assertStatus("jar:http://127.0.0.1/backend.jar!/", UNSUPPORTED_SCHEME_REJECTED);
        assertStatus("data:text/plain,hello", UNSUPPORTED_SCHEME_REJECTED);
        assertStatus("javascript:alert(1)", UNSUPPORTED_SCHEME_REJECTED);
    }

    @Test
    void userInfoIsRejectedWithoutEchoingSecretInNormalizedUrl() {
        ProxyBackendUrlClassifier.Classification result =
                ProxyBackendUrlClassifier.classify("http://user:pass@127.0.0.1:8080");

        assertEquals(USERINFO_REJECTED, result.status());
        assertFalse(result.allowed());
        assertEquals("", result.normalizedUrl());
        assertFalse(result.reason().contains("pass"));
    }

    @Test
    void malformedOrMissingHostsAreRejected() {
        assertStatus("", INVALID_REJECTED);
        assertStatus("http://", INVALID_REJECTED);
        assertStatus("http:///backend", INVALID_REJECTED);
        assertStatus("http://:8080", INVALID_REJECTED);
        assertStatus("://127.0.0.1", INVALID_REJECTED);
        assertStatus("http://[::1", INVALID_REJECTED);
        assertStatus("http://999.1.1.1", INVALID_REJECTED);
        assertStatus("http://127.0.0.1?target=other", INVALID_REJECTED);
        assertStatus("http://127.0.0.1#fragment", INVALID_REJECTED);
    }

    @Test
    void wildcardBroadAndAmbiguousHostPatternsAreRejected() {
        assertStatus("http://*.example.internal", AMBIGUOUS_HOST_REJECTED);
        assertStatus("http://10.0.0.*", AMBIGUOUS_HOST_REJECTED);
        assertStatus("http://0.0.0.0:8080", AMBIGUOUS_HOST_REJECTED);
        assertStatus("http://[::]:8080", AMBIGUOUS_HOST_REJECTED);
        assertStatus("http://010.000.000.001", AMBIGUOUS_HOST_REJECTED);
        assertStatus("http://2130706433", AMBIGUOUS_HOST_REJECTED);
    }

    @Test
    void classifierSourceStaysOfflineAndDoesNotResolveOrProbe() throws Exception {
        String source = read(CLASSIFIER_SOURCE) + "\n" + read(ROUTE_PLANNER_SOURCE)
                + "\n" + read(LIVE_GATE_SOURCE);

        for (String forbidden : new String[] {
                "InetAddress",
                "getByName",
                "HttpClient",
                "URLConnection",
                "new Socket",
                "DatagramSocket",
                ".connect("}) {
            assertFalse(source.contains(forbidden), "classifier must stay offline; found " + forbidden);
        }
    }

    @Test
    void helperIsWiredOnlyIntoPrivateNetworkConfigurationValidation() throws Exception {
        Set<Path> allowedSources = Set.of(CLASSIFIER_SOURCE, ROUTE_PLANNER_SOURCE, LIVE_GATE_SOURCE,
                LIVE_STATUS_REPORT_SOURCE);

        try (Stream<Path> sources = Files.walk(Path.of("src/main/java"))) {
            for (Path source : sources
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList()) {
                boolean containsClassifier = read(source).contains("ProxyBackendUrlClassifier");
                assertEquals(allowedSources.contains(source), containsClassifier,
                        source + " must only reference classifier from the offline helper, config validation, "
                                + "offline live gate skeleton, or report-only live gate status DTO");
            }
        }
    }

    private static void assertStatus(String url, ProxyBackendUrlClassifier.Status expected) {
        ProxyBackendUrlClassifier.Classification result = ProxyBackendUrlClassifier.classify(url);

        assertEquals(expected, result.status(), url);
        assertEquals(expected == LOOPBACK_ALLOWED || expected == PRIVATE_NETWORK_ALLOWED, result.allowed(), url);
        if (result.allowed()) {
            assertFalse(result.normalizedUrl().isBlank(), url + " should have a normalized URL");
        } else {
            assertTrue(result.normalizedUrl().isBlank(), url + " should not expose a normalized rejected URL");
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
