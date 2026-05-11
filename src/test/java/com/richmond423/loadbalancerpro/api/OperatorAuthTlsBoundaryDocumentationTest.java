package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class OperatorAuthTlsBoundaryDocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Path API_SECURITY = Path.of("docs/API_SECURITY.md");
    private static final Path DEPLOYMENT_HARDENING = Path.of("docs/DEPLOYMENT_HARDENING_GUIDE.md");
    private static final Path REVERSE_PROXY_MODE = Path.of("docs/REVERSE_PROXY_MODE.md");
    private static final Path REAL_BACKEND_EXAMPLES = Path.of("docs/REAL_BACKEND_PROXY_EXAMPLES.md");
    private static final Path OPERATIONS_RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path PROD_API_KEY_FILTER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/config/ProdApiKeyFilter.java");
    private static final Path THIS_TEST = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/api/OperatorAuthTlsBoundaryDocumentationTest.java");

    private static final List<Path> BOUNDARY_DOCS = List.of(
            API_SECURITY,
            DEPLOYMENT_HARDENING,
            REVERSE_PROXY_MODE,
            REAL_BACKEND_EXAMPLES,
            OPERATIONS_RUNBOOK,
            REVIEWER_TRUST_MAP);
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+" + "CloudManager\\s*\\(|" + "CloudManager\\s*\\(");
    private static final Pattern RELEASE_COMMAND =
            Pattern.compile("(?im)^\\s*(gh\\s+release|git\\s+tag)\\b");

    @Test
    void docsDocumentProxyAuthBoundaryForCurrentModes() throws Exception {
        String readme = read(README);
        String apiSecurity = read(API_SECURITY);
        String reverseProxyMode = read(REVERSE_PROXY_MODE);

        assertTrue(readme.contains(
                "protects API mutation/allocation endpoints, `/proxy/**`, and `GET /api/proxy/status` "
                        + "with the `X-API-Key` header"));
        assertTrue(readme.contains(
                "requires the `operator` role for allocation endpoints, `/proxy/**`, and "
                        + "`GET /api/proxy/status`"));

        assertTrue(apiSecurity.contains(
                "In prod and cloud-sandbox profiles it also protects `/proxy/**` and "
                        + "`GET /api/proxy/status`"));
        assertTrue(reverseProxyMode.contains("## Auth And TLS Boundary"));
        assertTrue(reverseProxyMode.contains("Local/default API-key mode stays demo-friendly"));
        assertTrue(reverseProxyMode.contains(
                "In prod or cloud-sandbox API-key mode, `/proxy/**` and `GET /api/proxy/status` "
                        + "require the configured `X-API-Key`"));
        assertTrue(reverseProxyMode.contains(
                "In OAuth2 mode, the same proxy surfaces require the configured allocation role"));
    }

    @Test
    void docsDocumentTlsTerminationAsDeploymentBoundary() throws Exception {
        String reverseProxyMode = read(REVERSE_PROXY_MODE);
        String realBackendExamples = read(REAL_BACKEND_EXAMPLES);
        String operationsRunbook = read(OPERATIONS_RUNBOOK);
        String deploymentHardening = read(DEPLOYMENT_HARDENING);

        assertTrue(reverseProxyMode.contains("LoadBalancerPro does not terminate TLS for proxy traffic"));
        assertTrue(reverseProxyMode.contains(
                "Terminate TLS at a trusted reverse proxy, ingress, managed load balancer, platform edge, "
                        + "or service mesh"));
        assertTrue(realBackendExamples.contains(
                "TLS termination, public exposure, authentication, ingress, rate limits, and service "
                        + "ownership checks belong to the deployment environment"));
        assertTrue(operationsRunbook.contains(
                "TLS termination remains a deployment responsibility at a trusted reverse proxy, ingress, "
                        + "managed load balancer, platform edge, or service mesh"));
        assertTrue(deploymentHardening.contains(
                "TLS termination and ingress policy still belong at the deployment edge"));
    }

    @Test
    void docsPreserveDemoDefaultsAndProxyDisabledBoundary() throws Exception {
        String reverseProxyMode = read(REVERSE_PROXY_MODE);
        String realBackendExamples = read(REAL_BACKEND_EXAMPLES);
        String reviewerTrustMap = read(REVIEWER_TRUST_MAP);

        assertTrue(reverseProxyMode.contains("Disabled by default: `loadbalancerpro.proxy.enabled=false`"));
        assertTrue(realBackendExamples.contains("Proxy mode remains disabled by default."));
        assertTrue(reviewerTrustMap.contains("- Proxy is disabled by default."));
        assertTrue(reviewerTrustMap.contains("Local/default proxy demos are not a security boundary"));
    }

    @Test
    void boundaryDocsAvoidFakeSecurityClaimsAndReleaseCommands() throws Exception {
        for (Path doc : BOUNDARY_DOCS) {
            String content = read(doc);
            String normalized = content.toLowerCase(Locale.ROOT);

            assertFalse(normalized.contains("production-grade gateway"),
                    doc + " must not claim a production-grade gateway");
            assertFalse(normalized.contains("production-grade security"),
                    doc + " must not claim production-grade security");
            assertFalse(normalized.contains("enterprise security certification"),
                    doc + " must not claim enterprise security certification");
            assertFalse(normalized.contains("certified security"),
                    doc + " must not claim certified security");
            assertFalse(normalized.contains("benchmark result"),
                    doc + " must not claim benchmark results");
            assertFalse(normalized.contains("security benchmark"),
                    doc + " must not claim security benchmarks");
            assertFalse(normalized.contains("tls is implemented"),
                    doc + " must not claim TLS is implemented");
            assertFalse(normalized.contains("implements tls termination"),
                    doc + " must not claim in-app TLS termination");
            assertFalse(normalized.contains("end-to-end encryption is implemented"),
                    doc + " must not claim end-to-end encryption");
            assertFalse(RELEASE_COMMAND.matcher(content).find(),
                    doc + " must not instruct release or tag creation");
        }
    }

    @Test
    void boundarySprintAddsNoCloudManagerConstruction() throws Exception {
        for (Path path : List.of(
                PROD_API_KEY_FILTER,
                API_SECURITY,
                DEPLOYMENT_HARDENING,
                REVERSE_PROXY_MODE,
                REAL_BACKEND_EXAMPLES,
                OPERATIONS_RUNBOOK,
                REVIEWER_TRUST_MAP,
                THIS_TEST)) {
            assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(read(path)).find(),
                    path + " must not construct CloudManager");
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
