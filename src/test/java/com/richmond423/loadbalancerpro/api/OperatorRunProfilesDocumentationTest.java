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

class OperatorRunProfilesDocumentationTest {
    private static final Path RUN_PROFILES = Path.of("docs/OPERATOR_RUN_PROFILES.md");
    private static final Path README = Path.of("README.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path INSTALL_MATRIX = Path.of("docs/OPERATOR_INSTALL_RUN_MATRIX.md");
    private static final Path PACKAGING = Path.of("docs/OPERATOR_PACKAGING.md");
    private static final Path API_SECURITY = Path.of("docs/API_SECURITY.md");
    private static final Path DEPLOYMENT_HARDENING = Path.of("docs/DEPLOYMENT_HARDENING_GUIDE.md");
    private static final Path REVERSE_PROXY_MODE = Path.of("docs/REVERSE_PROXY_MODE.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path DEFAULT_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path LOCAL_EXAMPLE =
            Path.of("docs/examples/operator-run-profiles/local-demo.properties");
    private static final Path PROD_API_KEY_EXAMPLE =
            Path.of("docs/examples/operator-run-profiles/prod-api-key.properties");
    private static final Path CLOUD_SANDBOX_EXAMPLE =
            Path.of("docs/examples/operator-run-profiles/cloud-sandbox-api-key.properties");
    private static final Path PROXY_LOOPBACK_EXAMPLE =
            Path.of("docs/examples/operator-run-profiles/proxy-loopback.properties");
    private static final List<Path> POINTER_DOCS = List.of(
            README,
            RUNBOOK,
            INSTALL_MATRIX,
            PACKAGING,
            API_SECURITY,
            DEPLOYMENT_HARDENING,
            REVERSE_PROXY_MODE,
            REVIEWER_TRUST_MAP);
    private static final List<Path> PROFILE_EXAMPLES = List.of(
            LOCAL_EXAMPLE,
            PROD_API_KEY_EXAMPLE,
            CLOUD_SANDBOX_EXAMPLE,
            PROXY_LOOPBACK_EXAMPLE);
    private static final Pattern PUBLIC_EXTERNAL_URL =
            Pattern.compile("https?://(?!127\\.0\\.0\\.1(?::|/|$)|localhost(?::|/|$))[^\\s\"'`]+");
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+" + "CloudManager\\s*\\(|" + "CloudManager\\s*\\(");
    private static final Pattern RELEASE_COMMAND =
            Pattern.compile("(?im)^\\s*(gh\\s+release|git\\s+tag)\\b");

    @Test
    void runProfilesGuideExistsAndKeyDocsLinkToIt() throws Exception {
        assertTrue(Files.exists(RUN_PROFILES), "operator run profiles guide should exist");

        for (Path doc : POINTER_DOCS) {
            assertTrue(read(doc).contains("OPERATOR_RUN_PROFILES.md"),
                    doc + " should link to the operator run profiles guide");
        }
    }

    @Test
    void profileMatrixCoversSupportedRunModes() throws Exception {
        String guide = read(RUN_PROFILES);

        assertTrue(guide.contains("## Profile Matrix"));
        assertTrue(guide.contains("| local demo |"));
        assertTrue(guide.contains("| packaged jar local |"));
        assertTrue(guide.contains("| prod API-key boundary |"));
        assertTrue(guide.contains("| cloud-sandbox API-key boundary |"));
        assertTrue(guide.contains("| OAuth2 mode |"));
        assertTrue(guide.contains("| proxy-enabled loopback validation |"));
        assertTrue(guide.contains("| container run |"));
        assertTrue(guide.contains("Proxy mode is lightweight and optional"));
        assertTrue(guide.contains("loadbalancerpro.proxy.enabled=false"));
    }

    @Test
    void recipesIncludeProtectedProxyStatusFailureAndApiKeySuccessExamples() throws Exception {
        String guide = read(RUN_PROFILES);

        assertTrue(guide.contains("curl -i http://127.0.0.1:8080/api/proxy/status"));
        assertTrue(guide.contains("curl -i -H \"X-API-Key: $LOADBALANCERPRO_API_KEY\" "
                + "http://127.0.0.1:8080/api/proxy/status"));
        assertTrue(guide.contains("curl.exe -i -H \"X-API-Key: $env:LOADBALANCERPRO_API_KEY\" "
                + "http://127.0.0.1:8080/api/proxy/status"));
        assertTrue(guide.contains("unauthenticated status/proxy calls return HTTP 401"));
        assertTrue(guide.contains("authenticated `/proxy/demo` call still returns HTTP 404 unless proxy mode is "
                + "explicitly enabled"));
        assertTrue(guide.contains("CHANGE_ME_LOCAL_API_KEY"));
    }

    @Test
    void examplesUsePlaceholdersLoopbackOnlyAndExpectedProxyDefaults() throws Exception {
        assertTrue(read(DEFAULT_PROPERTIES).contains("loadbalancerpro.proxy.enabled=false"));
        assertTrue(read(LOCAL_EXAMPLE).contains("loadbalancerpro.proxy.enabled=false"));
        assertTrue(read(PROD_API_KEY_EXAMPLE).contains("loadbalancerpro.proxy.enabled=false"));
        assertTrue(read(CLOUD_SANDBOX_EXAMPLE).contains("loadbalancerpro.proxy.enabled=false"));
        assertTrue(read(PROXY_LOOPBACK_EXAMPLE).contains("loadbalancerpro.proxy.enabled=true"));
        assertTrue(read(PROXY_LOOPBACK_EXAMPLE).contains("http://127.0.0.1:18081"));
        assertTrue(read(PROXY_LOOPBACK_EXAMPLE).contains("http://127.0.0.1:18082"));
        assertTrue(read(PROD_API_KEY_EXAMPLE).contains("CHANGE_ME_LOCAL_API_KEY"));
        assertTrue(read(CLOUD_SANDBOX_EXAMPLE).contains("CHANGE_ME_LOCAL_API_KEY"));
        assertTrue(read(CLOUD_SANDBOX_EXAMPLE).contains("cloud.allowLiveMutation=false"));

        for (Path example : PROFILE_EXAMPLES) {
            String content = read(example);
            assertFalse(PUBLIC_EXTERNAL_URL.matcher(content).find(),
                    example + " should use localhost or 127.0.0.1 URLs only");
            assertFalse(content.contains("real-secret"), example + " should not contain real secrets");
            assertFalse(content.contains("BEGIN PRIVATE KEY"), example + " should not contain private keys");
        }
    }

    @Test
    void tlsAndOperatorCautionsAreDocumentedWithoutFakeClaims() throws Exception {
        String guide = read(RUN_PROFILES);
        String normalized = guide.toLowerCase(Locale.ROOT);

        assertTrue(guide.contains("TLS termination is expected at a trusted reverse proxy, ingress, managed "
                + "load balancer, platform edge, or service mesh"));
        assertTrue(guide.contains("Demo mode is not a security boundary"));
        assertTrue(guide.contains("API-key and OAuth2 modes are access boundaries for controlled validation"));
        assertTrue(guide.contains("This guide does not create tags, GitHub Releases, release assets, "
                + "`release-downloads/` evidence"));
        assertFalse(normalized.contains("production-grade gateway"));
        assertFalse(normalized.contains("production-grade security"));
        assertFalse(normalized.contains("enterprise security certification"));
        assertFalse(normalized.contains("certified gateway"));
        assertFalse(normalized.contains("benchmark result:"));
        assertFalse(normalized.contains("tls is implemented"));
        assertFalse(normalized.contains("end-to-end encryption is implemented"));
    }

    @Test
    void sprintDocsAndExamplesAvoidCloudManagerConstructionReleaseCommandsAndPublicUrls() throws Exception {
        for (Path path : concat(List.of(RUN_PROFILES), PROFILE_EXAMPLES)) {
            String content = read(path);
            assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(content).find(),
                    path + " must not construct CloudManager");
            assertFalse(RELEASE_COMMAND.matcher(content).find(),
                    path + " must not instruct release or tag creation");
            assertFalse(PUBLIC_EXTERNAL_URL.matcher(content).find(),
                    path + " should not add public external URLs");
        }
    }

    @Test
    void existingAuthAndProxyTestsRemainPresent() {
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/ProdApiKeyProtectionTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/OAuth2AuthorizationTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/ReverseProxyDisabledTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/OperatorAuthTlsBoundaryDocumentationTest.java")));
    }

    private static List<Path> concat(List<Path> first, List<Path> second) {
        return java.util.stream.Stream.concat(first.stream(), second.stream()).toList();
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
