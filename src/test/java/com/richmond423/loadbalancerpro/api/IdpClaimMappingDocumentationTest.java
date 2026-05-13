package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class IdpClaimMappingDocumentationTest {
    private static final Path GUIDE = Path.of("docs/IDP_CLAIM_MAPPING_EXAMPLES.md");
    private static final Path API_SECURITY = Path.of("docs/API_SECURITY.md");
    private static final Path AUTH_PLAN = Path.of("docs/ENTERPRISE_COCKPIT_AUTH_PLAN.md");
    private static final Path API_CONTRACTS = Path.of("docs/API_CONTRACTS.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SECURITY_CONFIG =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/config/ApiSecurityConfiguration.java");
    private static final Path OAUTH2_TEST =
            Path.of("src/test/java/com/richmond423/loadbalancerpro/api/OAuth2AuthorizationTest.java");

    @Test
    void idpGuideDocumentsAcceptedDedicatedRoleClaims() throws Exception {
        String guide = read(GUIDE);

        for (String expected : List.of(
                "`roles`",
                "`role`",
                "`authorities`",
                "`realm_access.roles`",
                "\"roles\": [\"operator\"]",
                "\"roles\": [\"admin\"]",
                "\"realm_access\"",
                "operator",
                "admin")) {
            assertTrue(guide.contains(expected), "guide should document " + expected);
        }
    }

    @Test
    void idpGuideDocumentsScopeOnlyDenialAndFailClosedBehavior() throws Exception {
        String guide = read(GUIDE);

        for (String expected : List.of(
                "Scope-Only Tokens Do Not Grant App Roles",
                "`scope=operator` does not create `ROLE_operator`",
                "`scp=[\"operator\"]` does not create `ROLE_operator`",
                "`scope=admin` does not create `ROLE_admin`",
                "Missing dedicated role claims fail closed")) {
            assertTrue(guide.contains(expected), "guide should document " + expected);
        }
    }

    @Test
    void idpGuideDocumentsLocalMockValidationWithoutRealSecrets() throws Exception {
        String guide = read(GUIDE);
        String normalized = guide.toLowerCase(Locale.ROOT);

        assertTrue(guide.contains("mvn -q \"-Dtest=OAuth2AuthorizationTest\" test"));
        assertTrue(guide.contains("roles-operator-token"));
        assertTrue(guide.contains("scope-operator-token"));
        assertTrue(guide.contains("roles-admin-token"));
        assertTrue(guide.contains("without a real IdP"));
        assertTrue(guide.contains("Do not paste real tenant IDs"));
        assertFalse(normalized.contains("client_secret"));
        assertFalse(normalized.contains("login.microsoftonline.com"));
        assertFalse(normalized.contains("okta.com/oauth2"));
    }

    @Test
    void reviewerAndSecurityDocsLinkClaimMappingGuide() throws Exception {
        for (Path path : List.of(API_SECURITY, AUTH_PLAN, API_CONTRACTS, RUNBOOK, TRUST_MAP)) {
            assertTrue(read(path).contains("IDP_CLAIM_MAPPING_EXAMPLES.md"),
                    path + " should link the IdP claim mapping examples");
        }
    }

    @Test
    void implementationAndTestsStillRejectScopeOnlyRoles() throws Exception {
        String config = read(SECURITY_CONFIG);
        String tests = read(OAUTH2_TEST);

        assertTrue(config.contains("OAuth2 scope/scp claims are intentionally not application roles."));
        assertTrue(config.contains("jwt.getClaim(\"roles\")"));
        assertTrue(config.contains("jwt.getClaim(\"role\")"));
        assertTrue(config.contains("jwt.getClaim(\"authorities\")"));
        assertTrue(config.contains("realm_access"));
        assertFalse(config.contains("jwt.getClaim(\"scope\")"));
        assertFalse(config.contains("jwt.getClaim(\"scp\")"));
        assertTrue(tests.contains("oauth2ModeRejectsOperatorScopeStringAsApplicationRole"));
        assertTrue(tests.contains("oauth2ModeRejectsOperatorScpArrayAsApplicationRole"));
        assertTrue(tests.contains("adminScopeDoesNotGrantConfiguredAdminRole"));
        assertTrue(tests.contains("roles-admin-token"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
