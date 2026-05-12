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

class DeploymentSmokeKitDocumentationTest {
    private static final Path SMOKE_DOC = Path.of("docs/DEPLOYMENT_SMOKE_KIT.md");
    private static final Path SMOKE_SCRIPT = Path.of("scripts/smoke/operator-run-profiles-smoke.ps1");
    private static final Path README = Path.of("README.md");
    private static final Path RUN_PROFILES = Path.of("docs/OPERATOR_RUN_PROFILES.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path PACKAGING = Path.of("docs/OPERATOR_PACKAGING.md");
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
    private static final Pattern PUBLIC_EXTERNAL_URL =
            Pattern.compile("https?://(?!127\\.0\\.0\\.1(?::|/|$)|localhost(?::|/|$))[^\\s\"'`]+");
    private static final Pattern RELEASE_COMMAND =
            Pattern.compile("(?im)^\\s*(gh\\s+release|git\\s+tag)\\b");
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+" + "CloudManager\\s*\\(|" + "CloudManager\\s*\\(");

    @Test
    void deploymentSmokeKitDocExistsAndKeyDocsLinkToIt() throws Exception {
        assertTrue(Files.exists(SMOKE_DOC), "deployment smoke kit doc should exist");

        for (Path doc : List.of(README, RUN_PROFILES, RUNBOOK, PACKAGING, REVIEWER_TRUST_MAP)) {
            assertTrue(read(doc).contains("DEPLOYMENT_SMOKE_KIT.md"),
                    doc + " should link to the deployment smoke kit");
        }
    }

    @Test
    void smokeScriptExistsAndCoversRunProfileChecks() throws Exception {
        String script = read(SMOKE_SCRIPT);

        assertTrue(script.contains("[switch]$DryRun"));
        assertTrue(script.contains("[switch]$Package"));
        assertTrue(script.contains("mvn"));
        assertTrue(script.contains("java"));
        assertTrue(script.contains("Start-SmokeApp"));
        assertTrue(script.contains("api/health"));
        assertTrue(script.contains("api/proxy/status"));
        assertTrue(script.contains("X-API-Key"));
        assertTrue(script.contains("CHANGE_ME_LOCAL_API_KEY"));
        assertTrue(script.contains("proxy/api/smoke?step=1"));
        assertTrue(script.contains("docs/examples/operator-run-profiles/proxy-loopback.properties"));
        assertTrue(script.contains("PASS:"));
        assertTrue(script.contains("WARN:"));
        assertTrue(script.contains("FAIL:"));
        assertTrue(script.contains("exit $exitCode"));
        assertTrue(script.contains("finally"));
    }

    @Test
    void smokeScriptIsLocalOnlyAndAvoidsReleaseOrSecretMutation() throws Exception {
        String script = read(SMOKE_SCRIPT);

        assertTrue(script.contains("127.0.0.1"));
        assertFalse(PUBLIC_EXTERNAL_URL.matcher(script).find(), "script should not call public URLs");
        assertFalse(RELEASE_COMMAND.matcher(script).find(), "script should not create releases or tags");
        assertFalse(script.contains("release-downloads"), "script should not mutate release-downloads");
        assertFalse(script.contains("BEGIN PRIVATE KEY"), "script should not include private keys");
        assertFalse(script.contains("real-secret"), "script should not include real secrets");
        assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(script).find(), "script must not construct CloudManager");
    }

    @Test
    void smokeDocsStateLocalOnlyScopeAndTroubleshootingWithoutOverclaiming() throws Exception {
        String doc = read(SMOKE_DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(doc.contains("local-only"));
        assertTrue(doc.contains("without external services"));
        assertTrue(doc.contains("does not require cloud credentials"));
        assertTrue(doc.contains("does not require cloud credentials, call cloud APIs, or mutate cloud state"));
        assertTrue(doc.contains("PKIX"));
        assertTrue(doc.contains("HTTP 401 without X-API-Key"));
        assertTrue(doc.contains("HTTP 200 with X-API-Key"));
        assertTrue(doc.contains("No tag, release, or asset creation"));
        assertTrue(doc.contains("No generated artifacts committed"));
        assertTrue(normalized.contains("not production certification"));
        assertTrue(normalized.contains("not") && normalized.contains("benchmark"));
        assertTrue(normalized.contains("not") && normalized.contains("certification"));
        assertFalse(normalized.contains("production-grade gateway"));
        assertFalse(normalized.contains("enterprise security certification"));
        assertFalse(normalized.contains("benchmark results"));
        assertFalse(normalized.contains("tls is implemented"));
    }

    @Test
    void proxyDefaultsAndProfileExamplesRemainSafe() throws Exception {
        assertTrue(read(DEFAULT_PROPERTIES).contains("loadbalancerpro.proxy.enabled=false"));
        assertTrue(read(LOCAL_EXAMPLE).contains("loadbalancerpro.proxy.enabled=false"));
        assertTrue(read(PROD_API_KEY_EXAMPLE).contains("loadbalancerpro.proxy.enabled=false"));
        assertTrue(read(CLOUD_SANDBOX_EXAMPLE).contains("loadbalancerpro.proxy.enabled=false"));
        assertTrue(read(PROXY_LOOPBACK_EXAMPLE).contains("loadbalancerpro.proxy.enabled=true"));
        assertTrue(read(PROXY_LOOPBACK_EXAMPLE).contains("http://127.0.0.1:18081"));
        assertTrue(read(PROXY_LOOPBACK_EXAMPLE).contains("http://127.0.0.1:18082"));
    }

    @Test
    void sprintDocsAndScriptAvoidFakeEvidencePublicUrlsAndCloudManagerConstruction() throws Exception {
        for (Path path : List.of(SMOKE_DOC, SMOKE_SCRIPT)) {
            String content = read(path);
            assertFalse(PUBLIC_EXTERNAL_URL.matcher(content).find(),
                    path + " should not add public external URLs");
            assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(content).find(),
                    path + " must not construct CloudManager");
            assertFalse(content.contains("BEGIN PRIVATE KEY"),
                    path + " must not contain private keys");
            assertFalse(content.contains("SHA256="),
                    path + " must not add fake hashes");
        }
    }

    @Test
    void existingApiKeyOauthProxyAndRunProfileTestsRemainPresent() {
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/ProdApiKeyProtectionTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/OAuth2AuthorizationTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/ReverseProxyDisabledTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/OperatorRunProfilesDocumentationTest.java")));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
