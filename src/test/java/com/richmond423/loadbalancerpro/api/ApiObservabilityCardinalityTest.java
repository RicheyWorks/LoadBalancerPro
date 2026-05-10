package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.richmond423.loadbalancerpro.core.DomainMetrics;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiObservabilityCardinalityTest {
    private static final Set<String> ALLOCATION_METRICS = Set.of(
            DomainMetrics.ALLOCATION_REQUESTS,
            DomainMetrics.ALLOCATION_ACCEPTED_LOAD,
            DomainMetrics.ALLOCATION_REJECTED_LOAD,
            DomainMetrics.ALLOCATION_UNALLOCATED_LOAD,
            DomainMetrics.ALLOCATION_SERVER_COUNT,
            DomainMetrics.ALLOCATION_SCALING_RECOMMENDED_SERVERS);
    private static final Set<String> ALLOWED_STRATEGY_TAG_VALUES = Set.of(
            "CAPACITY_AWARE",
            "PREDICTIVE",
            "ROUND_ROBIN",
            "LEAST_LOADED",
            "WEIGHTED",
            "CONSISTENT_HASHING");
    private static final Set<String> ALLOWED_VALIDATION_PATHS = Set.of(
            "/api/allocate/capacity-aware",
            "/api/allocate/predictive",
            "/api/allocate/evaluate");
    private static final Set<String> ALLOWED_VALIDATION_REASONS = Set.of(
            "bad_request",
            "validation_failed");

    @Autowired
    private MockMvc mockMvc;

    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUpMetrics() {
        registry = new SimpleMeterRegistry();
        Metrics.addRegistry(registry);
    }

    @AfterEach
    void tearDownMetrics() {
        Metrics.removeRegistry(registry);
        registry.close();
    }

    @Test
    void allocationMetricsUseBoundedStrategyLabelsOnly() throws Exception {
        postAllocation("/api/allocate/capacity-aware", "edge-host-0001", 125.0)
                .andExpect(status().isOk());
        postAllocation("/api/allocate/capacity-aware", "tenant-a-request-42", 25.0)
                .andExpect(status().isOk());
        postAllocation("/api/allocate/predictive", "uuid-like-550e8400-e29b-41d4-a716-446655440000", 40.0)
                .andExpect(status().isOk());

        for (String metric : ALLOCATION_METRICS) {
            assertEquals(Set.of("strategy"), tagKeys(metric), metric + " should only expose the strategy tag");
            assertTrue(ALLOWED_STRATEGY_TAG_VALUES.containsAll(tagValues(metric, "strategy")),
                    metric + " should use bounded strategy values");
            assertTrue(registry.find(metric).meters().size() <= ALLOWED_STRATEGY_TAG_VALUES.size(),
                    metric + " should not create one series per server or request");
        }
        assertNoRequestSpecificTagValues();
    }

    @Test
    void evaluationRequestsRemainReadOnlyAndDoNotEmitAllocationMetrics() throws Exception {
        Map<String, Double> before = allocationMetricMeasurements();

        postEvaluation("edge-host-0001", 120.0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.metricsPreview.emitted", is(false)));
        postEvaluation("tenant-b-request-99", 75.0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.metricsPreview.emitted", is(false)));

        for (String metric : ALLOCATION_METRICS) {
            assertEquals(before.get(metric), measurementTotal(metric), 0.0001,
                    "Read-only evaluation should preview " + metric + " without emitting it");
        }
        assertNoRequestSpecificTagValues();
    }

    @Test
    void validationAndLoadSheddingFailuresUseBoundedPathAndReasonLabels() throws Exception {
        postMalformedJson("/api/allocate/capacity-aware")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")));
        postInvalidEvaluation("tenant-a-request-42")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("validation_failed")));
        postInvalidEvaluation("tenant-b-request-99")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("validation_failed")));

        assertEquals(Set.of("path", "reason"), tagKeys(DomainMetrics.ALLOCATION_VALIDATION_FAILURES));
        assertTrue(ALLOWED_VALIDATION_PATHS.containsAll(
                        tagValues(DomainMetrics.ALLOCATION_VALIDATION_FAILURES, "path")),
                "Validation failure path labels should stay on known API routes");
        assertTrue(ALLOWED_VALIDATION_REASONS.containsAll(
                        tagValues(DomainMetrics.ALLOCATION_VALIDATION_FAILURES, "reason")),
                "Validation failure reason labels should stay bounded");
        assertTrue(registry.find(DomainMetrics.ALLOCATION_VALIDATION_FAILURES).meters().size()
                        <= ALLOWED_VALIDATION_PATHS.size() * ALLOWED_VALIDATION_REASONS.size(),
                "Validation failures should not create one metric series per request");
        assertNoRequestSpecificTagValues();
    }

    @Test
    void actuatorPrometheusExposesOperatorMetricNamesWithoutExternalPrometheus() throws Exception {
        postAllocation("/api/allocate/capacity-aware", "edge-host-0001", 125.0)
                .andExpect(status().isOk());
        postMalformedJson("/api/allocate/capacity-aware")
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("allocation_requests_count")))
                .andExpect(content().string(containsString("allocation_unallocated_load")))
                .andExpect(content().string(containsString("allocation_scaling_recommended_servers")))
                .andExpect(content().string(containsString("allocation_validation_failures_count")));
    }

    private org.springframework.test.web.servlet.ResultActions postAllocation(
            String path, String serverId, double requestedLoad) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "requestedLoad": %.1f,
                          "servers": [
                            {
                              "id": "%s",
                              "cpuUsage": 20.0,
                              "memoryUsage": 30.0,
                              "diskUsage": 40.0,
                              "capacity": 100.0,
                              "weight": 1.0,
                              "healthy": true
                            }
                          ]
                        }
                        """.formatted(requestedLoad, serverId)));
    }

    private org.springframework.test.web.servlet.ResultActions postEvaluation(
            String serverId, double requestedLoad) throws Exception {
        return mockMvc.perform(post("/api/allocate/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "requestedLoad": %.1f,
                          "strategy": "CAPACITY_AWARE",
                          "priority": "BACKGROUND",
                          "currentInFlightRequestCount": 95,
                          "concurrencyLimit": 100,
                          "queueDepth": 25,
                          "observedP95LatencyMillis": 300.0,
                          "observedErrorRate": 0.20,
                          "servers": [
                            {
                              "id": "%s",
                              "cpuUsage": 20.0,
                              "memoryUsage": 30.0,
                              "diskUsage": 40.0,
                              "capacity": 100.0,
                              "weight": 1.0,
                              "healthy": true
                            }
                          ]
                        }
                        """.formatted(requestedLoad, serverId)));
    }

    private org.springframework.test.web.servlet.ResultActions postInvalidEvaluation(String serverId) throws Exception {
        return mockMvc.perform(post("/api/allocate/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "requestedLoad": -1.0,
                          "strategy": "CAPACITY_AWARE",
                          "servers": [
                            {
                              "id": "%s",
                              "cpuUsage": 20.0,
                              "memoryUsage": 30.0,
                              "diskUsage": 40.0,
                              "capacity": 100.0,
                              "weight": 1.0,
                              "healthy": true
                            }
                          ]
                        }
                        """.formatted(serverId)));
    }

    private org.springframework.test.web.servlet.ResultActions postMalformedJson(String path) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "requestedLoad": 10.0,
                          "servers": [
                        }
                        """));
    }

    private Set<String> tagKeys(String meterName) {
        return registry.find(meterName).meters().stream()
                .map(Meter::getId)
                .map(Meter.Id::getTags)
                .flatMap(Collection::stream)
                .map(Tag::getKey)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<String> tagValues(String meterName, String tagKey) {
        return registry.find(meterName).meters().stream()
                .map(Meter::getId)
                .map(Meter.Id::getTags)
                .flatMap(Collection::stream)
                .filter(tag -> tagKey.equals(tag.getKey()))
                .map(Tag::getValue)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Map<String, Double> allocationMetricMeasurements() {
        Map<String, Double> measurements = new HashMap<>();
        for (String metric : ALLOCATION_METRICS) {
            measurements.put(metric, measurementTotal(metric));
        }
        return measurements;
    }

    private double measurementTotal(String meterName) {
        double total = 0.0;
        for (Meter meter : registry.find(meterName).meters()) {
            for (Measurement measurement : meter.measure()) {
                total += measurement.getValue();
            }
        }
        return total;
    }

    private void assertNoRequestSpecificTagValues() {
        for (Meter meter : registry.getMeters()) {
            for (Tag tag : meter.getId().getTags()) {
                String value = tag.getValue().toLowerCase(Locale.ROOT);
                assertFalse(value.contains("edge-host"), "Metric tags must not include raw server ids: " + tag);
                assertFalse(value.contains("tenant-a"), "Metric tags must not include tenant/request ids: " + tag);
                assertFalse(value.contains("tenant-b"), "Metric tags must not include tenant/request ids: " + tag);
                assertFalse(value.contains("uuid-like"), "Metric tags must not include raw request ids: " + tag);
                assertFalse(value.contains("550e8400"), "Metric tags must not include UUID-like values: " + tag);
            }
        }
    }
}
