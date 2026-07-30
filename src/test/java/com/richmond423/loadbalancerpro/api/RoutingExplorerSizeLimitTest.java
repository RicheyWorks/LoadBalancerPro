package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.api.config.RoutingApiLimitsProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@SpringBootTest(properties = {
        "loadbalancerpro.auth.mode=none",
        "loadbalancerpro.api.max-request-bytes=1048576",
        "loadbalancerpro.api.max-candidates=2",
        "loadbalancerpro.api.max-strategies=2",
        "loadbalancerpro.api.max-decision-explorer-response-bytes=1024"
})
@AutoConfigureMockMvc
class RoutingExplorerSizeLimitTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requestContractRejectsTheHundredCandidateAuditPocBeforeEndpointWork() {
        RoutingComparisonRequest request = new RoutingComparisonRequest(
                fiveStrategies(),
                IntStream.range(0, 100)
                        .mapToObj(index -> candidateInput("candidate-" + index))
                        .toList());

        Set<ConstraintViolation<RoutingComparisonRequest>> violations = validate(request);

        assertTrue(violations.stream().anyMatch(violation ->
                        "servers".equals(violation.getPropertyPath().toString())
                                && violation.getMessage().contains("at most 32")),
                "the DTO contract must reject 100 candidates without constructing comparison payloads");
    }

    @Test
    void hundredCandidateFiveStrategyAuditPocReturnsHttp400BeforePayloadServices() throws Exception {
        RoutingComparisonService comparisonService = mock(RoutingComparisonService.class);
        DecisionExplorerPayloadService payloadService = mock(DecisionExplorerPayloadService.class);
        DecisionExplorerScenarioCatalogService scenarioCatalogService =
                mock(DecisionExplorerScenarioCatalogService.class);
        RoutingApiLimitsProperties limits = new RoutingApiLimitsProperties();
        DecisionExplorerResponseSizeGuard responseSizeGuard =
                new DecisionExplorerResponseSizeGuard(OBJECT_MAPPER, limits);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        try {
            MockMvc validationOnlyMvc = MockMvcBuilders.standaloneSetup(new RoutingController(
                            comparisonService,
                            payloadService,
                            scenarioCatalogService,
                            responseSizeGuard))
                    .setControllerAdvice(new RestExceptionHandler())
                    .setValidator(validator)
                    .build();

            validationOnlyMvc.perform(post("/api/routing/decision-explorer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson(fiveStrategies(), 100)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("validation_failed")))
                    .andExpect(jsonPath("$.details", hasItem(containsString("at most 32 candidates"))));

            verifyNoInteractions(comparisonService, payloadService, scenarioCatalogService);
        } finally {
            validator.close();
        }
    }

    @Test
    void requestContractRejectsMoreThanFiveExplicitStrategies() {
        RoutingComparisonRequest request = new RoutingComparisonRequest(
                List.of("one", "two", "three", "four", "five", "six"),
                List.of(candidateInput("candidate")));

        Set<ConstraintViolation<RoutingComparisonRequest>> violations = validate(request);

        assertTrue(violations.stream().anyMatch(violation ->
                        "strategies".equals(violation.getPropertyPath().toString())
                                && violation.getMessage().contains("at most 5")),
                "the DTO contract must cap explicit strategy fan-out before comparison work");
    }

    @Test
    void configuredCandidateCapRejectsBeforeComparisonPayloadConstruction() throws Exception {
        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(List.of("ROUND_ROBIN"), 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("configured maximum of 2 candidates")));
    }

    @Test
    void configuredStrategyCapRejectsBeforeComparisonPayloadConstruction() throws Exception {
        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(
                                List.of("ROUND_ROBIN", "WEIGHTED_ROUND_ROBIN", "WEIGHTED_LEAST_LOAD"), 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString("configured maximum of 2 strategies")));
    }

    @Test
    void decisionExplorerResponseGuardRejectsOversizedGeneratedPayload() throws Exception {
        mockMvc.perform(post("/api/routing/decision-explorer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(List.of("ROUND_ROBIN"), 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message",
                        containsString("decision explorer response exceeds maximum size of 1024 bytes")));
    }

    @Test
    void configuredCandidateAndStrategyCapsAreInclusive() throws Exception {
        mockMvc.perform(post("/api/routing/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(List.of("ROUND_ROBIN", "WEIGHTED_ROUND_ROBIN"), 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount", is(2)))
                .andExpect(jsonPath("$.requestedStrategies.length()", is(2)));
    }

    private static Set<ConstraintViolation<RoutingComparisonRequest>> validate(RoutingComparisonRequest request) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(request);
        }
    }

    private static String requestJson(List<String> strategies, int candidateCount) throws JsonProcessingException {
        List<Map<String, Object>> servers = IntStream.range(0, candidateCount)
                .mapToObj(index -> candidateJson("candidate-" + index))
                .toList();
        return OBJECT_MAPPER.writeValueAsString(Map.of(
                "strategies", strategies,
                "servers", servers));
    }

    private static List<String> fiveStrategies() {
        return List.of(
                "TAIL_LATENCY_POWER_OF_TWO",
                "WEIGHTED_LEAST_LOAD",
                "WEIGHTED_LEAST_CONNECTIONS",
                "WEIGHTED_ROUND_ROBIN",
                "ROUND_ROBIN");
    }

    private static RoutingServerStateInput candidateInput(String serverId) {
        return new RoutingServerStateInput(
                serverId,
                true,
                1,
                100.0,
                100.0,
                1.0,
                10.0,
                20.0,
                30.0,
                0.0,
                0,
                null);
    }

    private static Map<String, Object> candidateJson(String serverId) {
        return Map.of(
                "serverId", serverId,
                "healthy", true,
                "inFlightRequestCount", 1,
                "averageLatencyMillis", 10.0,
                "p95LatencyMillis", 20.0,
                "p99LatencyMillis", 30.0,
                "recentErrorRate", 0.0);
    }
}
