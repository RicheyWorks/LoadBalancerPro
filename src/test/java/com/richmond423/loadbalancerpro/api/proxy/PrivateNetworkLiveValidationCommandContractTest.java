package com.richmond423.loadbalancerpro.api.proxy;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "loadbalancerpro.proxy.enabled=true",
        "loadbalancerpro.proxy.private-network-validation.enabled=true",
        "loadbalancerpro.proxy.private-network-live-validation.enabled=true",
        "loadbalancerpro.proxy.private-network-live-validation.operator-approved=true",
        "loadbalancerpro.proxy.health-check.enabled=false",
        "loadbalancerpro.proxy.upstreams[0].id=command-loopback",
        "loadbalancerpro.proxy.upstreams[0].url=http://127.0.0.1:18081",
        "loadbalancerpro.proxy.upstreams[0].healthy=true"
})
@AutoConfigureMockMvc
class PrivateNetworkLiveValidationCommandContractTest {
    private static final String COMMAND_ENDPOINT = "/api/proxy/private-network-live-validation";
    private static final Path COMMAND_REQUEST_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/"
                    + "PrivateNetworkLiveValidationCommandRequest.java");
    private static final Path COMMAND_RESPONSE_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/"
                    + "PrivateNetworkLiveValidationCommandResponse.java");
    private static final Path REQUEST_VALIDATOR_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/"
                    + "PrivateNetworkLiveValidationRequestPathValidator.java");
    private static final Path STATUS_CONTROLLER_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyStatusController.java");
    private static final Path REVERSE_PROXY_SERVICE_SOURCE = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/proxy/ReverseProxyService.java");
    private static final Path SMOKE_SCRIPTS = Path.of("scripts/smoke");
    private static final Path POSTMAN_DOCS = Path.of("docs/postman");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void commandReportsAllowedGateButNotImplementedAndDoesNotExecuteTraffic() throws Exception {
        mockMvc.perform(post(COMMAND_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "requestPath", "/health",
                                "evidenceRequested", true,
                                "operatorAcknowledged", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted", is(false)))
                .andExpect(jsonPath("$.executable", is(false)))
                .andExpect(jsonPath("$.trafficExecuted", is(false)))
                .andExpect(jsonPath("$.status", is("NOT_IMPLEMENTED")))
                .andExpect(jsonPath("$.gateStatus", is("ALLOWED")))
                .andExpect(jsonPath("$.allowedByGate", is(true)))
                .andExpect(jsonPath("$.message", is("traffic execution is not wired in this release")))
                .andExpect(jsonPath("$.requestPath", is("/health")))
                .andExpect(jsonPath("$.evidenceRequested", is(true)))
                .andExpect(jsonPath("$.evidenceWritten", is(false)))
                .andExpect(jsonPath("$.evidenceEligible", is(true)))
                .andExpect(jsonPath("$.plannedEvidenceDirectory", is("target/proxy-evidence/")))
                .andExpect(jsonPath("$.plannedEvidenceMarkdown", is("private-network-live-validation.md")))
                .andExpect(jsonPath("$.plannedEvidenceJson", is("private-network-live-validation.json")))
                .andExpect(jsonPath("$.redactionRequired", is(true)))
                .andExpect(jsonPath("$.trafficExecution", is("traffic execution is not wired in this release")))
                .andExpect(jsonPath("$.auditTrail.auditTrailEligible", is(true)))
                .andExpect(jsonPath("$.auditTrail.auditTrailWritten", is(false)))
                .andExpect(jsonPath("$.auditTrail.plannedAuditTrail",
                        is("target/proxy-evidence/private-network-live-validation-audit.jsonl")))
                .andExpect(jsonPath("$.auditTrail.plannedFields[0]", is("requestPath")))
                .andExpect(jsonPath("$.auditTrail.plannedFields[3]", is("trafficExecuted")))
                .andExpect(jsonPath("$.operatorAcknowledged", is(true)))
                .andExpect(jsonPath("$.gate.allowedByGate", is(true)))
                .andExpect(jsonPath("$.gate.gateStatus", is("ALLOWED")))
                .andExpect(jsonPath("$.gate.trafficExecuted", is(false)))
                .andExpect(jsonPath("$.gate.trafficExecution", is("traffic not executed by this report")))
                .andExpect(jsonPath("$.gate.backends[0].classifierStatus", is("LOOPBACK_ALLOWED")))
                .andExpect(jsonPath("$.reasonCodes[0]", is("LIVE_VALIDATION_EXECUTION_NOT_WIRED")))
                .andExpect(content().string(not(containsString("PrivateNetworkLiveValidationExecutor"))))
                .andExpect(content().string(not(containsString("Authorization"))))
                .andExpect(content().string(not(containsString("X-API-Key"))))
                .andExpect(content().string(not(containsString("Bearer"))))
                .andExpect(content().string(not(containsString("Cookie"))));
    }

    @Test
    void commandFactoryReportsDefaultGateBlockedWithoutExecutingTraffic() {
        PrivateNetworkLiveValidationCommandResponse response =
                PrivateNetworkLiveValidationCommandResponse.from(
                        new ReverseProxyProperties(),
                        new PrivateNetworkLiveValidationCommandRequest("/health", false, false, null));

        assertFalse(response.accepted());
        assertFalse(response.executable());
        assertFalse(response.trafficExecuted());
        assertFalse(response.evidenceWritten());
        assertFalse(response.evidenceEligible());
        assertEquals("BLOCKED_BY_GATE", response.status());
        assertEquals(response.gate().gateStatus(), response.gateStatus());
        assertEquals(response.gate().allowedByGate(), response.allowedByGate());
        assertEquals("NOT_ENABLED", response.gateStatus());
        assertTrue(response.plannedEvidenceDirectory().equals("target/proxy-evidence/"));
        assertTrue(response.plannedEvidenceMarkdown().equals("private-network-live-validation.md"));
        assertTrue(response.plannedEvidenceJson().equals("private-network-live-validation.json"));
        assertTrue(response.redactionRequired());
        assertTrue(response.trafficExecution().equals("traffic execution is not wired in this release"));
        assertFalse(response.auditTrail().auditTrailEligible());
        assertFalse(response.auditTrail().auditTrailWritten());
        assertTrue(response.auditTrail().plannedAuditTrail()
                .equals("target/proxy-evidence/private-network-live-validation-audit.jsonl"));
        assertTrue(response.reasonCodes().contains("LIVE_VALIDATION_DISABLED"));
        assertTrue(response.message().contains("traffic execution is not wired in this release"));
        assertTrue(response.gate().gateStatus().equals("NOT_ENABLED"));
    }

    @Test
    void commandFactoryKeepsGateSummaryConsistentForBlockedAndAllowedStates() {
        ReverseProxyProperties blocked = propertiesWithTarget("blocked", "http://127.0.0.1:18081");
        blocked.getPrivateNetworkLiveValidation().setEnabled(true);
        blocked.getPrivateNetworkValidation().setEnabled(true);

        PrivateNetworkLiveValidationCommandResponse blockedResponse =
                PrivateNetworkLiveValidationCommandResponse.from(
                        blocked,
                        new PrivateNetworkLiveValidationCommandRequest("/health", true, false, null));

        assertEquals("BLOCKED_BY_GATE", blockedResponse.status());
        assertEquals(blockedResponse.gate().gateStatus(), blockedResponse.gateStatus());
        assertEquals(blockedResponse.gate().allowedByGate(), blockedResponse.allowedByGate());
        assertEquals("BLOCKED", blockedResponse.gateStatus());
        assertFalse(blockedResponse.allowedByGate());
        assertFalse(blockedResponse.trafficExecuted());
        assertFalse(blockedResponse.evidenceWritten());
        assertFalse(blockedResponse.auditTrail().auditTrailWritten());
        assertTrue(blockedResponse.reasonCodes().contains("OPERATOR_APPROVAL_REQUIRED"));

        ReverseProxyProperties allowed = propertiesWithTarget("allowed", "http://127.0.0.1:18081");
        enableAllLiveGateFlags(allowed);
        PrivateNetworkLiveValidationCommandResponse allowedResponse =
                PrivateNetworkLiveValidationCommandResponse.from(
                        allowed,
                        new PrivateNetworkLiveValidationCommandRequest("/health", true, true, null));

        assertEquals("NOT_IMPLEMENTED", allowedResponse.status());
        assertEquals(allowedResponse.gate().gateStatus(), allowedResponse.gateStatus());
        assertEquals(allowedResponse.gate().allowedByGate(), allowedResponse.allowedByGate());
        assertEquals("ALLOWED", allowedResponse.gateStatus());
        assertTrue(allowedResponse.allowedByGate());
        assertEquals(List.of("LIVE_VALIDATION_EXECUTION_NOT_WIRED"), allowedResponse.reasonCodes());
        assertEquals(List.of("ALLOWED_BY_GATE"), allowedResponse.gate().reasonCodes());
        assertFalse(allowedResponse.trafficExecuted());
        assertFalse(allowedResponse.evidenceWritten());
        assertFalse(allowedResponse.auditTrail().auditTrailWritten());
    }

    @Test
    void invalidRequestFailsClosedWhileStillReportingOfflineGateSummary() {
        ReverseProxyProperties allowed = propertiesWithTarget("allowed", "http://127.0.0.1:18081");
        enableAllLiveGateFlags(allowed);

        PrivateNetworkLiveValidationCommandResponse response =
                PrivateNetworkLiveValidationCommandResponse.from(
                        allowed,
                        new PrivateNetworkLiveValidationCommandRequest(
                                "/health?token=SHOULD_NOT_ECHO", true, true, null));

        assertEquals("INVALID_REQUEST", response.status());
        assertEquals("ALLOWED", response.gateStatus());
        assertTrue(response.allowedByGate());
        assertEquals("", response.requestPath());
        assertFalse(response.trafficExecuted());
        assertFalse(response.evidenceWritten());
        assertFalse(response.evidenceEligible());
        assertFalse(response.auditTrail().auditTrailWritten());
        assertTrue(response.reasonCodes().contains("INVALID_REQUEST_PATH"));
        assertFalse(response.toString().contains("SHOULD_NOT_ECHO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "http://127.0.0.1/health?secret=SHOULD_NOT_ECHO",
            "//public.example/path",
            "/safe?token=SHOULD_NOT_ECHO",
            "/safe#fragment",
            "/../secret",
            "/%2e%2e/secret",
            "/control\u0001",
            "/back\\slash"
    })
    void unsafeRequestPathsFailClosedWithoutEchoingUnsafeInput(String requestPath) throws Exception {
        mockMvc.perform(post(COMMAND_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "requestPath", requestPath,
                                "evidenceRequested", true,
                                "operatorAcknowledgement", "I understand this is not wired"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accepted", is(false)))
                .andExpect(jsonPath("$.executable", is(false)))
                .andExpect(jsonPath("$.trafficExecuted", is(false)))
                .andExpect(jsonPath("$.status", is("INVALID_REQUEST")))
                .andExpect(jsonPath("$.gateStatus", is("ALLOWED")))
                .andExpect(jsonPath("$.allowedByGate", is(true)))
                .andExpect(jsonPath("$.requestPath", is("")))
                .andExpect(jsonPath("$.evidenceWritten", is(false)))
                .andExpect(jsonPath("$.evidenceEligible", is(false)))
                .andExpect(jsonPath("$.plannedEvidenceDirectory", is("target/proxy-evidence/")))
                .andExpect(jsonPath("$.plannedEvidenceMarkdown", is("private-network-live-validation.md")))
                .andExpect(jsonPath("$.plannedEvidenceJson", is("private-network-live-validation.json")))
                .andExpect(jsonPath("$.redactionRequired", is(true)))
                .andExpect(jsonPath("$.trafficExecution", is("traffic execution is not wired in this release")))
                .andExpect(jsonPath("$.auditTrail.auditTrailEligible", is(false)))
                .andExpect(jsonPath("$.auditTrail.auditTrailWritten", is(false)))
                .andExpect(jsonPath("$.operatorAcknowledged", is(true)))
                .andExpect(jsonPath("$.reasonCodes[0]", is("INVALID_REQUEST_PATH")))
                .andExpect(content().string(not(containsString("SHOULD_NOT_ECHO"))))
                .andExpect(content().string(not(containsString("PrivateNetworkLiveValidationExecutor"))));
    }

    @Test
    void missingRequestBodyFailsClosedWithoutTraffic() throws Exception {
        mockMvc.perform(post(COMMAND_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accepted", is(false)))
                .andExpect(jsonPath("$.executable", is(false)))
                .andExpect(jsonPath("$.trafficExecuted", is(false)))
                .andExpect(jsonPath("$.status", is("INVALID_REQUEST")))
                .andExpect(jsonPath("$.gateStatus", is("ALLOWED")))
                .andExpect(jsonPath("$.allowedByGate", is(true)))
                .andExpect(jsonPath("$.requestPath", is("")))
                .andExpect(jsonPath("$.evidenceEligible", is(false)))
                .andExpect(jsonPath("$.evidenceWritten", is(false)))
                .andExpect(jsonPath("$.auditTrail.auditTrailWritten", is(false)))
                .andExpect(jsonPath("$.reasonCodes[0]", is("INVALID_REQUEST")));
    }

    @Test
    void commandSourcesDoNotInvokeExecutorOrNetworkDiscoveryApis() throws Exception {
        String commandSources = read(COMMAND_REQUEST_SOURCE)
                + "\n" + read(COMMAND_RESPONSE_SOURCE)
                + "\n" + read(REQUEST_VALIDATOR_SOURCE)
                + "\n" + read(STATUS_CONTROLLER_SOURCE);
        String serviceSource = read(REVERSE_PROXY_SERVICE_SOURCE);

        assertTrue(commandSources.contains("traffic execution is not wired in this release"));
        assertTrue(commandSources.contains("trafficExecuted"));
        assertTrue(commandSources.contains("gateStatus"));
        assertTrue(commandSources.contains("allowedByGate"));
        assertTrue(commandSources.contains("evidenceEligible"));
        assertTrue(commandSources.contains("plannedEvidenceDirectory"));
        assertTrue(commandSources.contains("plannedEvidenceMarkdown"));
        assertTrue(commandSources.contains("plannedEvidenceJson"));
        assertTrue(commandSources.contains("redactionRequired"));
        assertTrue(commandSources.contains("AuditTrailContract"));
        assertTrue(commandSources.contains("target/proxy-evidence/"));
        assertTrue(commandSources.contains("private-network-live-validation-audit.jsonl"));
        assertTrue(commandSources.contains("@PostMapping(\"/private-network-live-validation\")"));
        assertTrue(serviceSource.contains("privateNetworkLiveValidationCommand"));
        assertFalse(commandSources.contains("PrivateNetworkLiveValidationExecutor"),
                "command/report contract must not invoke the live executor");
        assertFalse(serviceSource.contains("PrivateNetworkLiveValidationExecutor"),
                "proxy runtime service must not invoke the live executor");

        for (String forbidden : List.of(
                "InetAddress",
                "getByName",
                "HttpClient",
                "URLConnection",
                "new Socket",
                "DatagramSocket",
                ".connect(",
                "isReachable",
                "port scan",
                "discovery")) {
            assertFalse(commandSources.contains(forbidden),
                    "command contract must stay offline; found " + forbidden);
        }
    }

    @Test
    void smokeAndPostmanPathsDoNotGainPrivateNetworkLiveCommandExecution() throws Exception {
        String combined = readTree(SMOKE_SCRIPTS) + "\n" + readTree(POSTMAN_DOCS);

        assertFalse(combined.contains("private-network-live-validation"),
                "smoke/Postman paths must not add private-network live command execution");
        assertFalse(combined.contains("PrivateNetworkLiveValidationExecutor"),
                "smoke/Postman paths must not invoke the live executor");
        assertFalse(combined.contains("PrivateNetworkLiveValidationCommand"),
                "smoke/Postman paths must not invoke the command contract");
    }

    private String json(Map<String, Object> payload) throws IOException {
        return objectMapper.writeValueAsString(payload);
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
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                content.append(read(path)).append('\n');
            }
        }
        return content.toString();
    }
}
