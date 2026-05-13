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

class CsrfDispositionSecurityTest {
    private static final Path API_SECURITY_CONFIG = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/config/ApiSecurityConfiguration.java");
    private static final Path DEFAULT_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path API_SECURITY = Path.of("docs/API_SECURITY.md");
    private static final Path DEPLOYMENT_HARDENING = Path.of("docs/DEPLOYMENT_HARDENING_GUIDE.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SECURITY_POSTURE = Path.of("evidence/SECURITY_POSTURE.md");
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+CloudManager\\s*\\(|CloudManager\\s*\\(");
    private static final Pattern RELEASE_COMMAND =
            Pattern.compile("(?im)^\\s*(gh\\s+release|git\\s+tag)\\b");

    @Test
    void securityConfigurationScopesCsrfWithoutGlobalDisable() throws Exception {
        String security = read(API_SECURITY_CONFIG);

        assertFalse(security.contains("csrf(AbstractHttpConfigurer::disable)"),
                "CSRF must not be globally disabled");
        assertFalse(security.contains("csrf.disable"),
                "CSRF must not use the CodeQL-flagged global disable pattern");
        assertTrue(security.contains("csrf -> csrf.ignoringRequestMatchers("));
        assertTrue(security.contains("\"/api/**\""));
        assertTrue(security.contains("\"/proxy\""));
        assertTrue(security.contains("\"/proxy/**\""));
        assertTrue(security.contains("\"/actuator/**\""));
        assertTrue(security.contains("SessionCreationPolicy.STATELESS"));
        assertTrue(security.contains("httpBasic(AbstractHttpConfigurer::disable)"));
        assertTrue(security.contains("formLogin(AbstractHttpConfigurer::disable)"));
        assertTrue(security.contains("logout(AbstractHttpConfigurer::disable)"));
    }

    @Test
    void csrfDispositionDocsExplainStatelessHeaderAuthAndFutureCookieCaveat() throws Exception {
        String apiSecurity = read(API_SECURITY);
        String deploymentHardening = read(DEPLOYMENT_HARDENING);
        String reviewerTrustMap = read(REVIEWER_TRUST_MAP);
        String securityPosture = read(SECURITY_POSTURE);

        assertTrue(apiSecurity.contains("## CSRF Disposition"));
        assertTrue(apiSecurity.contains("stateless sessions"));
        assertTrue(apiSecurity.contains("form login disabled"));
        assertTrue(apiSecurity.contains("HTTP Basic disabled"));
        assertTrue(apiSecurity.contains("logout disabled"));
        assertTrue(apiSecurity.contains("X-API-Key"));
        assertTrue(apiSecurity.contains("Authorization: Bearer"));
        assertTrue(apiSecurity.contains("do not rely on browser ambient cookie credentials"));
        assertTrue(apiSecurity.contains("session-cookie authentication"));
        assertTrue(apiSecurity.contains("must be re-evaluated before that change ships"));
        assertTrue(deploymentHardening.contains("CSRF protection is scoped for the current stateless API model"));
        assertTrue(reviewerTrustMap.contains("API_SECURITY.md#csrf-disposition"));
        assertTrue(securityPosture.contains("CSRF is not globally disabled."));
        assertTrue(securityPosture.contains("earlier disabled-CSRF finding is addressed by scoped CSRF configuration"));
    }

    @Test
    void csrfDispositionPreservesDemoDefaultsAndBoundaryTests() throws Exception {
        String defaults = read(DEFAULT_PROPERTIES);
        String security = read(API_SECURITY_CONFIG);

        assertTrue(defaults.contains("loadbalancerpro.proxy.enabled=false"));
        assertFalse(defaults.contains("loadbalancerpro.proxy.enabled=true"));
        assertTrue(security.contains("authorize.anyRequest().permitAll()"),
                "local/default non-OAuth2 demo mode should remain usable");
        assertTrue(security.contains("HttpMethod.GET, \"/api/proxy/status\""));
        assertTrue(security.contains("HttpMethod.POST, \"/api/proxy/reload\""));
        assertTrue(security.contains("HttpMethod.POST, \"/api/proxy/private-network-live-validation\""));
        assertTrue(security.contains("hasRole(allocationRole)"));

        for (String path : List.of(
                "src/test/java/com/richmond423/loadbalancerpro/api/ProdApiKeyProtectionTest.java",
                "src/test/java/com/richmond423/loadbalancerpro/api/OAuth2AuthorizationTest.java",
                "src/test/java/com/richmond423/loadbalancerpro/api/ReverseProxyReloadSecurityTest.java",
                "src/test/java/com/richmond423/loadbalancerpro/api/DeploymentSmokeKitDocumentationTest.java")) {
            assertTrue(Files.exists(Path.of(path)), path + " should remain present");
        }
    }

    @Test
    void csrfDispositionAddsNoCloudReleaseOrInflatedSecurityClaims() throws Exception {
        for (Path path : List.of(API_SECURITY_CONFIG, API_SECURITY, DEPLOYMENT_HARDENING, REVIEWER_TRUST_MAP,
                SECURITY_POSTURE)) {
            String content = read(path);
            String normalized = content.toLowerCase(Locale.ROOT);

            assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(content).find(), path + " must not construct CloudManager");
            assertFalse(RELEASE_COMMAND.matcher(content).find(), path + " must not add release or tag commands");
            assertFalse(normalized.contains("production-grade security"),
                    path + " must not add production-grade security claims");
            assertFalse(normalized.contains("enterprise security certification"),
                    path + " must not add enterprise security certification claims");
            assertFalse(normalized.contains("certification proof"),
                    path + " must not add certification proof claims");
            assertFalse(normalized.contains("benchmark result"),
                    path + " must not add benchmark result claims");
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
