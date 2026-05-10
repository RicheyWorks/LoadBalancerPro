package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.richmond423.loadbalancerpro.core.CloudManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RemediationReportControllerTest {
    private static final String NORMAL_EVALUATION_REQUEST = """
            {
              "requestedLoad": 40.0,
              "strategy": "CAPACITY_AWARE",
              "priority": "USER",
              "servers": [
                {
                  "id": "green",
                  "cpuUsage": 20.0,
                  "memoryUsage": 20.0,
                  "diskUsage": 20.0,
                  "capacity": 100.0,
                  "weight": 1.0,
                  "healthy": true
                }
              ]
            }
            """;

    private static final String OVERLOAD_EVALUATION_REQUEST = """
            {
              "requestedLoad": 150.0,
              "strategy": "CAPACITY_AWARE",
              "priority": "BACKGROUND",
              "currentInFlightRequestCount": 95,
              "concurrencyLimit": 100,
              "queueDepth": 25,
              "observedP95LatencyMillis": 300.0,
              "observedErrorRate": 0.20,
              "servers": [
                {
                  "id": "primary",
                  "cpuUsage": 30.0,
                  "memoryUsage": 30.0,
                  "diskUsage": 30.0,
                  "capacity": 100.0,
                  "weight": 1.0,
                  "healthy": true
                },
                {
                  "id": "fallback",
                  "cpuUsage": 70.0,
                  "memoryUsage": 70.0,
                  "diskUsage": 70.0,
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
    void markdownReportForNormalEvaluationIsDeterministicAndAdvisory() throws Exception {
        String evaluation = evaluate(NORMAL_EVALUATION_REQUEST);
        String reportRequest = reportRequest("MARKDOWN", "incident-normal", "Normal Evaluation", "evaluation",
                evaluation);

        String first = mockMvc.perform(report(reportRequest))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.format", is("MARKDOWN")))
                .andExpect(jsonPath("$.contentType", is("text/markdown")))
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.advisoryOnly", is(true)))
                .andExpect(jsonPath("$.cloudMutation", is(false)))
                .andExpect(jsonPath("$.json.sourceType", is("EVALUATION")))
                .andExpect(jsonPath("$.json.status", is("HEALTHY")))
                .andExpect(jsonPath("$.json.acceptedLoad", closeTo(40.0, 0.01)))
                .andExpect(jsonPath("$.json.unallocatedLoad", closeTo(0.0, 0.01)))
                .andExpect(jsonPath("$.json.remediationPlan.recommendations[0].action", is("NO_ACTION")))
                .andExpect(jsonPath("$.report", containsString("# Normal Evaluation")))
                .andExpect(jsonPath("$.report", containsString("## Ranked Remediation Actions")))
                .andExpect(jsonPath("$.report", containsString("NO_ACTION")))
                .andExpect(jsonPath("$.report", containsString("Cloud mutation: false")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String second = mockMvc.perform(report(reportRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(first, second, "same report input should produce identical output");
    }

    @Test
    void markdownReportForOverloadIncludesScaleUpAndLoadSheddingActions() throws Exception {
        String evaluation = evaluate(OVERLOAD_EVALUATION_REQUEST);

        mockMvc.perform(report(reportRequest("MARKDOWN", "incident-overload", "Overload Review", "evaluation",
                        evaluation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.json.status", is("OVERLOADED")))
                .andExpect(jsonPath("$.json.rejectedLoad", closeTo(50.0, 0.01)))
                .andExpect(jsonPath("$.json.unallocatedLoad", closeTo(50.0, 0.01)))
                .andExpect(jsonPath("$.json.recommendedAdditionalServers", is(1)))
                .andExpect(jsonPath("$.json.loadShedding.action", is("SHED")))
                .andExpect(jsonPath("$.json.remediationPlan.recommendations[0].action", is("SCALE_UP")))
                .andExpect(jsonPath("$.json.remediationPlan.recommendations[1].action", is("SHED_LOAD")))
                .andExpect(jsonPath("$.report", containsString("Additional servers: 1")))
                .andExpect(jsonPath("$.report", containsString("Unallocated load: 50.000")))
                .andExpect(jsonPath("$.report", containsString("SCALE_UP")))
                .andExpect(jsonPath("$.report", containsString("SHED_LOAD")));
    }

    @Test
    void jsonReportForOverloadIsAutomationFriendlyWithoutMarkdownBody() throws Exception {
        String evaluation = evaluate(OVERLOAD_EVALUATION_REQUEST);

        mockMvc.perform(report(reportRequest("JSON", "incident-json", "JSON Export", "evaluation", evaluation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format", is("JSON")))
                .andExpect(jsonPath("$.contentType", is("application/json")))
                .andExpect(jsonPath("$.report", nullValue()))
                .andExpect(jsonPath("$.json.reportId", is("incident-json")))
                .andExpect(jsonPath("$.json.sourceType", is("EVALUATION")))
                .andExpect(jsonPath("$.json.summary", containsString("left 50.000 load unallocated")))
                .andExpect(jsonPath("$.json.warnings[0]", containsString("Unallocated or rejected load")))
                .andExpect(jsonPath("$.json.limitations[0]", containsString("advisory")))
                .andExpect(jsonPath("$.json.cloudMutation", is(false)));
    }

    @Test
    void replayReportForMixedIncidentSummarizesOrderedStepsAndRecommendations() throws Exception {
        String replay = replay(resourceText("scenarios/replay/mixed-incident-replay.json"));

        mockMvc.perform(report(reportRequest("MARKDOWN", "incident-mixed", "Mixed Replay", "replay", replay)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.json.sourceType", is("SCENARIO_REPLAY")))
                .andExpect(jsonPath("$.json.steps[0].stepId", is("route-before-incident")))
                .andExpect(jsonPath("$.json.steps[1].stepId", is("overload-spike")))
                .andExpect(jsonPath("$.json.steps[1].unallocatedLoad", closeTo(20.0, 0.01)))
                .andExpect(jsonPath("$.json.steps[1].loadSheddingAction", is("SHED")))
                .andExpect(jsonPath("$.json.remediationPlan.recommendations[0].action", is("SCALE_UP")))
                .andExpect(jsonPath("$.json.remediationPlan.recommendations[1].action", is("SHED_LOAD")))
                .andExpect(jsonPath("$.json.remediationPlan.recommendations[2].action",
                        is("INVESTIGATE_UNHEALTHY")))
                .andExpect(jsonPath("$.report", containsString("## Replay Steps")))
                .andExpect(jsonPath("$.report", containsString("route-before-incident [ROUTE]")))
                .andExpect(jsonPath("$.report", containsString("overload-spike [OVERLOAD]")));
    }

    @Test
    void allUnhealthyReplayReportIncludesRestoreAndRetryWarnings() throws Exception {
        String replay = replay(resourceText("scenarios/replay/all-unhealthy-degradation.json"));

        mockMvc.perform(report(reportRequest("JSON", "incident-outage", "Outage Report", "replay", replay)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.json.status", is("NO_HEALTHY_CAPACITY")))
                .andExpect(jsonPath("$.json.unallocatedLoad", closeTo(80.0, 0.01)))
                .andExpect(jsonPath("$.json.remediationPlan.recommendations[0].action", is("RESTORE_CAPACITY")))
                .andExpect(jsonPath("$.json.remediationPlan.recommendations[1].action", is("RETRY_WHEN_HEALTHY")))
                .andExpect(jsonPath("$.json.warnings[1]", containsString("No healthy capacity")))
                .andExpect(jsonPath("$.readOnly", is(true)))
                .andExpect(jsonPath("$.cloudMutation", is(false)));
    }

    @Test
    void invalidReportRequestReturnsControlledBadRequest() throws Exception {
        mockMvc.perform(report("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", containsString(
                        "Exactly one of evaluation or replay report source is required")))
                .andExpect(jsonPath("$.path", is("/api/remediation/report")));
    }

    @Test
    void reportExportDoesNotConstructCloudManager() throws Exception {
        String evaluation = evaluate(OVERLOAD_EVALUATION_REQUEST);
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(report(reportRequest("MARKDOWN", "incident-safe", "Safe Report", "evaluation",
                            evaluation)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cloudMutation", is(false)));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Remediation report export must not construct CloudManager or execute remediation.");
        }
    }

    private String evaluate(String request) throws Exception {
        return mockMvc.perform(post("/api/allocate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String replay(String request) throws Exception {
        return mockMvc.perform(post("/api/scenarios/replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder report(String request) {
        return post("/api/remediation/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request);
    }

    private String reportRequest(String format, String reportId, String title, String fieldName, String sourceJson) {
        return """
                {
                  "format": "%s",
                  "reportId": "%s",
                  "title": "%s",
                  "%s": %s
                }
                """.formatted(format, reportId, title, fieldName, sourceJson);
    }

    private String resourceText(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
