package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(properties = {
        "spring.profiles.active=prod",
        "loadbalancerpro.auth.mode=oauth2",
        "loadbalancerpro.auth.oauth2.jwk-set-uri=https://auth.example.test/.well-known/jwks.json",
        "loadbalancerpro.api.cors.allowed-origins=https://app.example.com",
        "loadbalancerpro.api.max-request-bytes=512"
})
@AutoConfigureMockMvc
class OAuth2AuthorizationTest {
    private static final String REQUEST_BODY = """
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
    private static final String ROUTING_REQUEST_BODY = """
            {"servers":[{"serverId":"green","healthy":true,"inFlightRequestCount":1,"averageLatencyMillis":10.0,"p95LatencyMillis":20.0,"p99LatencyMillis":30.0,"recentErrorRate":0.0}]}
            """;
    private static final String LIVE_VALIDATION_COMMAND_BODY = """
            {"requestPath":"/health","operatorAcknowledged":true}
            """;
    private static final String ENTERPRISE_LAB_RUN_BODY = """
            {"mode":"shadow","scenarioIds":["normal-balanced-load"]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void oauth2ModeRejectsMissingTokenWith401() throws Exception {
        mockMvc.perform(get("/api/lase/shadow"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("unauthorized")))
                .andExpect(jsonPath("$.message", containsString("Bearer token")))
                .andExpect(jsonPath("$.path", is("/api/lase/shadow")));
    }

    @Test
    void oauth2ModeRejectsInvalidTokenWith401() throws Exception {
        mockMvc.perform(get("/api/lase/shadow").header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("unauthorized")))
                .andExpect(jsonPath("$.path", is("/api/lase/shadow")));
    }

    @Test
    void oauth2ModeRejectsAuthenticatedUserWithoutRequiredRoleWith403() throws Exception {
        mockMvc.perform(allocationRequest().header(HttpHeaders.AUTHORIZATION, "Bearer viewer-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("forbidden")))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));
    }

    @Test
    void oauth2ModeAllowsObserverToReadLaseShadowObservability() throws Exception {
        mockMvc.perform(get("/api/lase/shadow").header(HttpHeaders.AUTHORIZATION, "Bearer observer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalEvaluations").isNumber())
                .andExpect(jsonPath("$.recentEvents").isArray());
    }

    @Test
    void oauth2ModeRejectsObserverAllocationRequestsWith403() throws Exception {
        mockMvc.perform(allocationRequest().header(HttpHeaders.AUTHORIZATION, "Bearer observer-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("forbidden")))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));
    }

    @Test
    void oauth2ModeAllowsOperatorAllocationRequests() throws Exception {
        mockMvc.perform(allocationRequest().header(HttpHeaders.AUTHORIZATION, "Bearer roles-operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations.api-1").isNumber())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void oauth2ModeRejectsViewerRoutingCompareRequestsWith403() throws Exception {
        mockMvc.perform(routingCompareRequest().header(HttpHeaders.AUTHORIZATION, "Bearer viewer-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("forbidden")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")));
    }

    @Test
    void oauth2ModeRejectsObserverRoutingCompareRequestsWith403() throws Exception {
        mockMvc.perform(routingCompareRequest().header(HttpHeaders.AUTHORIZATION, "Bearer observer-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("forbidden")))
                .andExpect(jsonPath("$.path", is("/api/routing/compare")));
    }

    @Test
    void oauth2ModeAllowsOperatorRoutingCompareRequests() throws Exception {
        mockMvc.perform(routingCompareRequest().header(HttpHeaders.AUTHORIZATION, "Bearer roles-operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedStrategies[0]", is("TAIL_LATENCY_POWER_OF_TWO")))
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void oauth2ModeRequiresOperatorRoleForProxyStatus() throws Exception {
        mockMvc.perform(get("/api/proxy/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.path", is("/api/proxy/status")));

        mockMvc.perform(get("/api/proxy/status").header(HttpHeaders.AUTHORIZATION, "Bearer observer-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/api/proxy/status")));

        mockMvc.perform(get("/api/proxy/status").header(HttpHeaders.AUTHORIZATION, "Bearer roles-operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proxyEnabled", is(false)))
                .andExpect(jsonPath("$.securityBoundary.authMode", is("oauth2")))
                .andExpect(jsonPath("$.securityBoundary.proxyStatusProtected", is(true)))
                .andExpect(jsonPath("$.securityBoundary.proxyForwardingProtected", is(true)));
    }

    @Test
    void oauth2ModeRequiresOperatorRoleForPrivateNetworkLiveValidationCommand() throws Exception {
        mockMvc.perform(privateNetworkLiveValidationCommandRequest())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.path", is("/api/proxy/private-network-live-validation")));

        mockMvc.perform(privateNetworkLiveValidationCommandRequest()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer observer-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/api/proxy/private-network-live-validation")));

        mockMvc.perform(privateNetworkLiveValidationCommandRequest()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer roles-operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted", is(false)))
                .andExpect(jsonPath("$.executable", is(false)))
                .andExpect(jsonPath("$.trafficExecuted", is(false)))
                .andExpect(jsonPath("$.evidenceWritten", is(false)))
                .andExpect(jsonPath("$.plannedEvidenceDirectory", is("target/proxy-evidence/")))
                .andExpect(jsonPath("$.plannedEvidenceMarkdown", is("private-network-live-validation.md")))
                .andExpect(jsonPath("$.plannedEvidenceJson", is("private-network-live-validation.json")))
                .andExpect(jsonPath("$.auditTrail.auditTrailWritten", is(false)))
                .andExpect(jsonPath("$.status", is("BLOCKED_BY_GATE")))
                .andExpect(jsonPath("$.gateStatus", is("NOT_ENABLED")))
                .andExpect(jsonPath("$.allowedByGate", is(false)))
                .andExpect(jsonPath("$.gate.gateStatus", is("NOT_ENABLED")));
    }

    @Test
    void oauth2ModeAllowsReadRolesForLabScenarioCatalog() throws Exception {
        mockMvc.perform(get("/api/lab/scenarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.path", is("/api/lab/scenarios")));

        mockMvc.perform(get("/api/lab/scenarios").header(HttpHeaders.AUTHORIZATION, "Bearer viewer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(10)))
                .andExpect(jsonPath("$.scenarios[0].scenarioId", is("normal-balanced-load")));

        mockMvc.perform(get("/api/lab/scenarios/stale-signal")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer observer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioId", is("stale-signal")));
    }

    @Test
    void oauth2ModeRequiresOperatorRoleForLabRuns() throws Exception {
        mockMvc.perform(post("/api/lab/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ENTERPRISE_LAB_RUN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.path", is("/api/lab/runs")));

        mockMvc.perform(post("/api/lab/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer viewer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ENTERPRISE_LAB_RUN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/api/lab/runs")));

        mockMvc.perform(post("/api/lab/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer roles-operator-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ENTERPRISE_LAB_RUN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode", is("shadow")))
                .andExpect(jsonPath("$.scorecard.totalScenarios", is(1)));
    }

    @Test
    void oauth2ModeRequiresOperatorRoleForLabPolicyStatusAndAuditEvents() throws Exception {
        mockMvc.perform(get("/api/lab/policy"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.path", is("/api/lab/policy")));

        mockMvc.perform(get("/api/lab/policy")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer viewer-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/api/lab/policy")));

        mockMvc.perform(get("/api/lab/policy")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer roles-operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMode", is("off")));

        mockMvc.perform(get("/api/lab/audit-events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer viewer-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.path", is("/api/lab/audit-events")));

        mockMvc.perform(get("/api/lab/audit-events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer roles-operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageMode", is("process-local bounded audit log")));
    }

    @Test
    void oauth2ModeProtectsProxyForwardingSurface() throws Exception {
        mockMvc.perform(get("/proxy/demo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.path", is("/proxy/demo")));

        mockMvc.perform(get("/proxy/demo").header(HttpHeaders.AUTHORIZATION, "Bearer observer-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/proxy/demo")));

        mockMvc.perform(get("/proxy/demo").header(HttpHeaders.AUTHORIZATION, "Bearer roles-operator-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void oauth2ModeAllowsOperatorFromSingleRoleClaim() throws Exception {
        mockMvc.perform(allocationRequest().header(HttpHeaders.AUTHORIZATION, "Bearer role-operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations.api-1").isNumber())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void oauth2ModeAllowsOperatorFromAuthoritiesClaim() throws Exception {
        mockMvc.perform(allocationRequest().header(HttpHeaders.AUTHORIZATION, "Bearer authorities-operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations.api-1").isNumber())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void oauth2ModeRejectsOperatorScopeStringAsApplicationRole() throws Exception {
        mockMvc.perform(allocationRequest().header(HttpHeaders.AUTHORIZATION, "Bearer scope-operator-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));
    }

    @Test
    void oauth2ModeRejectsOperatorScpArrayAsApplicationRole() throws Exception {
        mockMvc.perform(allocationRequest().header(HttpHeaders.AUTHORIZATION, "Bearer scp-operator-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));
    }

    @Test
    void oauth2ModeRejectsScopePrefixedAuthoritiesAsApplicationRole() throws Exception {
        mockMvc.perform(allocationRequest()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer scope-authorities-operator-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));
    }

    @Test
    void oauth2ModeRejectsMissingRoleClaimsForRoleRequiredRoutes() throws Exception {
        mockMvc.perform(allocationRequest().header(HttpHeaders.AUTHORIZATION, "Bearer no-role-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));
    }

    @Test
    void oauth2ModeAllowsOperatorFromRealmAccessRolesClaim() throws Exception {
        mockMvc.perform(allocationRequest().header(HttpHeaders.AUTHORIZATION, "Bearer realm-operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations.api-1").isNumber())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void oauth2ModeAllowsObserverFromRealmAccessRolesClaimToReadLaseShadow() throws Exception {
        mockMvc.perform(get("/api/lase/shadow").header(HttpHeaders.AUTHORIZATION, "Bearer realm-observer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalEvaluations").isNumber())
                .andExpect(jsonPath("$.recentEvents").isArray());
    }

    @Test
    void oauth2ModeKeepsApiHealthPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")));
    }

    @Test
    void oauth2ModeGatesOpenApiDocsByDefault() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(get("/v3/api-docs").header(HttpHeaders.AUTHORIZATION, "Bearer viewer-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists());
    }

    @Test
    void oauth2ModeGatesSwaggerUiByDefault() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Test
    void oauth2ModeCorsPreflightAllowsAuthorizationHeaderForConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/allocate/capacity-aware")
                        .header(HttpHeaders.ORIGIN, "https://app.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example.com"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")));
    }

    @Test
    void requestSizeFilterStillWorksForAuthenticatedOauth2Mutation() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer roles-operator-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":\"" + "x".repeat(600) + "\"}"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status", is(413)))
                .andExpect(jsonPath("$.error", is("payload_too_large")))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));
    }

    @Test
    void oauth2ModeRejectsUnauthenticatedOversizedMutationWith401BeforeRequestSizeCheck() throws Exception {
        mockMvc.perform(post("/api/allocate/capacity-aware")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":\"" + "x".repeat(600) + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("unauthorized")))
                .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));
    }

    @Nested
    @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
    @SpringBootTest(properties = {
            "spring.profiles.active=prod",
            "loadbalancerpro.auth.mode=oauth2",
            "loadbalancerpro.auth.oauth2.jwk-set-uri=https://auth.example.test/.well-known/jwks.json",
            "loadbalancerpro.auth.docs-public=true"
    })
    @AutoConfigureMockMvc
    @Import(JwtDecoderTestConfiguration.class)
    class DocsPublicOverrideTests {
        @Autowired
        private MockMvc docsPublicMockMvc;

        @Test
        void docsPublicAllowsUnauthenticatedOpenApiDocs() throws Exception {
            docsPublicMockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.openapi").exists());
        }

        @Test
        void docsPublicAllowsUnauthenticatedSwaggerUi() throws Exception {
            docsPublicMockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Swagger UI")));
        }

        @Test
        void docsPublicDoesNotBypassProtectedMutationRoutes() throws Exception {
            docsPublicMockMvc.perform(allocationRequest())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.error", is("unauthorized")))
                    .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));
        }
    }

    @Nested
    @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
    @SpringBootTest(properties = {
            "spring.profiles.active=prod",
            "loadbalancerpro.auth.mode=oauth2",
            "loadbalancerpro.auth.oauth2.jwk-set-uri=https://auth.example.test/.well-known/jwks.json",
            "loadbalancerpro.auth.required-role.lase-shadow=shadow-reader",
            "loadbalancerpro.auth.required-role.allocation=allocator"
    })
    @AutoConfigureMockMvc
    @Import(JwtDecoderTestConfiguration.class)
    class CustomRequiredRoleOverrideTests {
        @Autowired
        private MockMvc customRoleMockMvc;

        @Test
        void customLaseShadowRoleAllowsConfiguredRoleAndRejectsDefaultObserver() throws Exception {
            customRoleMockMvc.perform(get("/api/lase/shadow")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer observer-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.path", is("/api/lase/shadow")));

            customRoleMockMvc.perform(get("/api/lase/shadow")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer shadow-reader-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.summary.totalEvaluations").isNumber())
                    .andExpect(jsonPath("$.recentEvents").isArray());

            customRoleMockMvc.perform(get("/api/lase/shadow")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer allocator-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.summary.totalEvaluations").isNumber())
                    .andExpect(jsonPath("$.recentEvents").isArray());
        }

        @Test
        void customAllocationRoleAllowsConfiguredRoleAndRejectsDefaultOperator() throws Exception {
            customRoleMockMvc.perform(allocationRequest()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer roles-operator-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));

            customRoleMockMvc.perform(allocationRequest()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer shadow-reader-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));

            customRoleMockMvc.perform(allocationRequest()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer allocator-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allocations.api-1").isNumber())
                    .andExpect(jsonPath("$.error").doesNotExist());
        }
    }

    @Nested
    @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
    @SpringBootTest(properties = {
            "spring.profiles.active=prod",
            "loadbalancerpro.auth.mode=oauth2",
            "loadbalancerpro.auth.oauth2.jwk-set-uri=https://auth.example.test/.well-known/jwks.json",
            "loadbalancerpro.auth.required-role.allocation=admin"
    })
    @AutoConfigureMockMvc
    @Import(JwtDecoderTestConfiguration.class)
    class AdminRequiredRoleOverrideTests {
        @Autowired
        private MockMvc adminRoleMockMvc;

        @Test
        void adminScopeDoesNotGrantConfiguredAdminRole() throws Exception {
            adminRoleMockMvc.perform(allocationRequest()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer scope-admin-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.path", is("/api/allocate/capacity-aware")));

            adminRoleMockMvc.perform(allocationRequest()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer roles-admin-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allocations.api-1").isNumber())
                    .andExpect(jsonPath("$.error").doesNotExist());
        }
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder allocationRequest() {
        return post("/api/allocate/capacity-aware")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder routingCompareRequest() {
        return post("/api/routing/compare")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ROUTING_REQUEST_BODY);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            privateNetworkLiveValidationCommandRequest() {
        return post("/api/proxy/private-network-live-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(LIVE_VALIDATION_COMMAND_BODY);
    }

    @TestConfiguration
    static class JwtDecoderTestConfiguration {
        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> switch (token) {
                case "viewer-token" -> jwt(token, "viewer");
                case "observer-token" -> jwt(token, "observer");
                case "roles-operator-token" -> jwt(token, Map.of("roles", List.of("operator")));
                case "shadow-reader-token" -> jwt(token, "shadow-reader");
                case "allocator-token" -> jwt(token, "allocator");
                case "role-operator-token" -> jwt(token, Map.of("role", "operator"));
                case "authorities-operator-token" -> jwt(token, Map.of("authorities", List.of("operator")));
                case "scope-operator-token" -> jwt(token, Map.of("scope", "operator observer"));
                case "scp-operator-token" -> jwt(token, Map.of("scp", List.of("operator", "observer")));
                case "scope-authorities-operator-token" -> jwt(token,
                        Map.of("authorities", List.of("SCOPE_operator")));
                case "no-role-token" -> jwt(token, Map.of("aud", List.of("loadbalancerpro")));
                case "scope-admin-token" -> jwt(token, Map.of("scope", "admin"));
                case "roles-admin-token" -> jwt(token, Map.of("roles", List.of("admin")));
                case "realm-operator-token" -> jwt(token,
                        Map.of("realm_access", Map.of("roles", List.of("operator"))));
                case "realm-observer-token" -> jwt(token,
                        Map.of("realm_access", Map.of("roles", List.of("observer"))));
                default -> throw new InvalidBearerTokenException("Invalid test token");
            };
        }

        private static Jwt jwt(String token, String... roles) {
            return jwt(token, Map.of("roles", List.of(roles)));
        }

        private static Jwt jwt(String token, Map<String, Object> claims) {
            Instant now = Instant.now();
            Jwt.Builder builder = Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .issuer("https://auth.example.test")
                    .subject(token)
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(300));
            claims.forEach(builder::claim);
            return builder.build();
        }
    }
}
