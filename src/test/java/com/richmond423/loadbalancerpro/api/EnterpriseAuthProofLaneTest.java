package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.profiles.active=prod",
        "loadbalancerpro.auth.mode=oauth2",
        "loadbalancerpro.auth.oauth2.issuer-uri=https://idp.example.test/tenant",
        "loadbalancerpro.auth.oauth2.jwk-set-uri=https://idp.example.test/tenant/.well-known/jwks.json"
})
@AutoConfigureMockMvc
@Import(EnterpriseAuthProofLaneTest.MockIdpJwtDecoderConfiguration.class)
class EnterpriseAuthProofLaneTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path MOCK_IDP_FIXTURE = Path.of("src/test/resources/auth-proof/mock-idp-claims.json");
    private static final Path SCRIPT = Path.of("scripts/smoke/enterprise-auth-proof.ps1");
    private static final String ALLOCATION_REQUEST = """
            {
              "requestedLoad": 10.0,
              "servers": [
                {
                  "id": "api-1",
                  "cpuUsage": 10.0,
                  "memoryUsage": 20.0,
                  "diskUsage": 30.0,
                  "capacity": 100.0,
                  "weight": 1.0,
                  "healthy": true
                }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mockIdpFixtureIsSyntheticAndDocumentsKeyRotation() throws Exception {
        JsonNode fixture = OBJECT_MAPPER.readTree(read(MOCK_IDP_FIXTURE));
        assertEqualsText("enterprise-auth-proof-lane-v1", fixture.path("fixtureVersion").asText());
        assertEqualsText("mocked-test-only", fixture.path("jwksMode").asText());
        assertTrue(fixture.path("keys").isArray());
        assertTrue(read(MOCK_IDP_FIXTURE).contains("test-key-a"));
        assertTrue(read(MOCK_IDP_FIXTURE).contains("test-key-b"));
        assertFalse(read(MOCK_IDP_FIXTURE).contains("-----BEGIN"));
        assertFalse(read(MOCK_IDP_FIXTURE).toLowerCase(Locale.ROOT).contains("client_secret"));
    }

    @Test
    void dedicatedRoleClaimGrantsOperatorRoute() throws Exception {
        mockMvc.perform(allocationRequest("proof-role-operator-key-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations.api-1", closeTo(10.0, 0.01)));
    }

    @Test
    void rotatedKeyFixtureWithRealmAccessRoleGrantsOperatorRoute() throws Exception {
        mockMvc.perform(allocationRequest("proof-role-operator-key-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations.api-1", closeTo(10.0, 0.01)));
    }

    @Test
    void scopeOnlyTokenDoesNotGrantApplicationRole() throws Exception {
        mockMvc.perform(allocationRequest("proof-scope-only-operator"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));
    }

    @Test
    void missingRoleClaimFailsClosed() throws Exception {
        mockMvc.perform(allocationRequest("proof-missing-role"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));
    }

    @Test
    void ambiguousScopeAuthoritiesFailClosed() throws Exception {
        mockMvc.perform(allocationRequest("proof-ambiguous-scope-authorities"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));
    }

    @Test
    void expiredIssuerAndAudienceFailuresAreUnauthorized() throws Exception {
        for (String token : List.of("proof-expired-token", "proof-wrong-issuer", "proof-wrong-audience")) {
            mockMvc.perform(allocationRequest(token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));
        }
    }

    @Test
    void authProofScriptWritesIgnoredTargetEvidenceAndAvoidsPublishCommands() throws Exception {
        String script = read(SCRIPT);
        String normalized = script.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "target/enterprise-auth-proof",
                "mock-idp-claims.json",
                "EnterpriseAuthProofLaneTest,OAuth2AuthorizationTest",
                "enterprise-auth-proof-results.json",
                "mock-idp-jwks-fixture-summary.json",
                "enterprise-auth-proof-summary.md",
                "Assert-OutputUnderTarget",
                "Assert-NoSecretValues",
                "no real IdP")) {
            assertTrue(script.contains(expected), "auth proof script should mention " + expected);
        }

        for (String prohibited : List.of(
                "git clean",
                "git tag",
                "git push",
                "gh release create",
                "gh release upload",
                "gh release delete",
                "docker push",
                "cosign sign")) {
            assertFalse(normalized.contains(prohibited), "auth proof script must not include " + prohibited);
        }
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            allocationRequest(String token) {
        return post("/api/allocate/capacity-aware")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ALLOCATION_REQUEST);
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void assertEqualsText(String expected, String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    @TestConfiguration
    static class MockIdpJwtDecoderConfiguration {
        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> switch (token) {
                case "proof-role-operator-key-a" -> jwt(token, "test-key-a",
                        Map.of("roles", List.of("operator")));
                case "proof-role-operator-key-b" -> jwt(token, "test-key-b",
                        Map.of("realm_access", Map.of("roles", List.of("operator"))));
                case "proof-scope-only-operator" -> jwt(token, "test-key-a",
                        Map.of("scope", "operator observer"));
                case "proof-missing-role" -> jwt(token, "test-key-a",
                        Map.of("aud", List.of("loadbalancerpro-api")));
                case "proof-ambiguous-scope-authorities" -> jwt(token, "test-key-a",
                        Map.of("authorities", List.of("SCOPE_operator"), "scp", List.of("operator")));
                case "proof-expired-token" -> throw new InvalidBearerTokenException("Expired mock token");
                case "proof-wrong-issuer" -> throw new InvalidBearerTokenException("Wrong mock issuer");
                case "proof-wrong-audience" -> throw new InvalidBearerTokenException("Wrong mock audience");
                default -> throw new InvalidBearerTokenException("Invalid mock token");
            };
        }

        private static Jwt jwt(String token, String kid, Map<String, Object> claims) {
            Instant now = Instant.now();
            Jwt.Builder builder = Jwt.withTokenValue(token)
                    .header("alg", "mock")
                    .header("kid", kid)
                    .issuer("https://idp.example.test/tenant")
                    .audience(List.of("loadbalancerpro-api"))
                    .subject(token)
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(300));
            claims.forEach(builder::claim);
            return builder.build();
        }
    }
}
