package com.richmond423.loadbalancerpro.api.proxy;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
                .andExpect(jsonPath("$.message", is("traffic execution is not wired in this release")))
                .andExpect(jsonPath("$.requestPath", is("/health")))
                .andExpect(jsonPath("$.evidenceRequested", is(true)))
                .andExpect(jsonPath("$.evidenceWritten", is(false)))
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
        assertTrue(response.reasonCodes().contains("LIVE_VALIDATION_DISABLED"));
        assertTrue(response.message().contains("traffic execution is not wired in this release"));
        assertTrue(response.gate().gateStatus().equals("NOT_ENABLED"));
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
                .andExpect(jsonPath("$.requestPath", is("")))
                .andExpect(jsonPath("$.evidenceWritten", is(false)))
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
                .andExpect(jsonPath("$.requestPath", is("")))
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
