package com.richmond423.loadbalancerpro.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "loadbalancerpro.lase.policy.mode=active-experiment",
        "loadbalancerpro.lase.policy.active-experiment-enabled=true"
})
@AutoConfigureMockMvc
class EnterpriseLabExperimentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void defaultApplicationTargetCatalogFailsClosedWithoutAddressExposure() throws Exception {
        mockMvc.perform(get("/api/lab/experiments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(0)))
                .andExpect(jsonPath("$.boundScenarioIds", empty()))
                .andExpect(jsonPath("$.activeExperimentEnabled", is(true)))
                .andExpect(jsonPath("$.targetBoundary",
                        is("request bodies cannot supply or reveal backend target addresses")))
                .andExpect(content().string(not(containsString("http://"))))
                .andExpect(content().string(not(containsString("requestUri"))));

        String armBody = """
                {
                  "operatorRequestId":"api-arm-1",
                  "experimentId":"api-experiment-1",
                  "scenarioId":"tail-latency-pressure",
                  "maximumRequestCount":20,
                  "maximumDurationSeconds":60,
                  "minimumEvidenceCount":5,
                  "holdDownCycles":2,
                  "expirationSeconds":120
                }
                """;
        mockMvc.perform(post("/api/lab/experiments/arm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(armBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DENIED")))
                .andExpect(jsonPath("$.reasonCode", is("APPROVED_TARGETS_UNAVAILABLE")))
                .andExpect(jsonPath("$.trafficActionPerformed", is(false)))
                .andExpect(jsonPath("$.experimentRecord").isEmpty())
                .andExpect(content().string(not(containsString("127.0.0.1"))));
    }

    @Test
    void malformedUnknownAndExcessiveOperatorRequestsReturnStructuredReasons() throws Exception {
        mockMvc.perform(post("/api/lab/experiments/arm")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", is("Request body is required")))
                .andExpect(jsonPath("$.path", is("/api/lab/experiments/arm")));

        String excessive = """
                {
                  "operatorRequestId":"api-arm-excessive",
                  "experimentId":"api-experiment-excessive",
                  "scenarioId":"tail-latency-pressure",
                  "maximumRequestCount":65,
                  "maximumDurationSeconds":60,
                  "minimumEvidenceCount":5,
                  "holdDownCycles":2,
                  "expirationSeconds":120
                }
                """;
        mockMvc.perform(post("/api/lab/experiments/arm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(excessive))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("between 1 and 64")))
                .andExpect(content().string(not(containsString("Exception"))));

        String unknown = """
                {
                  "operatorRequestId":"api-arm-unknown",
                  "experimentId":"api-experiment-unknown",
                  "scenarioId":"unknown-scenario"
                }
                """;
        mockMvc.perform(post("/api/lab/experiments/arm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unknown))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DENIED")))
                .andExpect(jsonPath("$.reasonCode", is("UNKNOWN_SCENARIO")))
                .andExpect(jsonPath("$.trafficActionPerformed", is(false)));

        mockMvc.perform(get("/api/lab/experiments/missing-experiment"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("not_found")))
                .andExpect(jsonPath("$.path", is("/api/lab/experiments/missing-experiment")));
    }

    @Test
    void unknownLifecycleCommandsStayStructuredAndPerformNoTrafficAction() throws Exception {
        String command = "{\"operatorRequestId\":\"api-start-missing\"}";
        mockMvc.perform(post("/api/lab/experiments/missing-experiment/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.reasonCode", is("UNKNOWN_EXPERIMENT")))
                .andExpect(jsonPath("$.trafficActionPerformed", is(false)));

        mockMvc.perform(post("/api/lab/experiments/missing-experiment/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operatorRequestId\":\"api-route-missing\",\"count\":1,\"timeoutMillis\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NOT_FOUND")))
                .andExpect(jsonPath("$.sentCount", is(0)))
                .andExpect(jsonPath("$.trafficActionPerformed", is(false)));
    }
}
