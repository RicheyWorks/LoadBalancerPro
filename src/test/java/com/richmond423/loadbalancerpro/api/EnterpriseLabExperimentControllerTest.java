package com.richmond423.loadbalancerpro.api;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentRecoveryGate.InitializationState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentTargetCatalog;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager;
import com.richmond423.loadbalancerpro.api.config.AdaptiveRoutingPolicyProperties;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @TempDir
    Path journalRoot;

    @Test
    void configuredAbsoluteJournalRootCompletesRecoveryBeforeServiceAdmission() {
        var configuration = new EnterpriseLabExperimentController.EnterpriseLabExperimentConfiguration();
        try (EnterpriseLabExperimentOperatorService service =
                configuration.enterpriseLabExperimentOperatorService(
                        EnterpriseLabExperimentTargetCatalog.empty(), journalRoot.toString())) {
            var status = service.recoveryStatus();

            assertEquals(InitializationState.READY, status.state());
            assertTrue(status.admissionAllowed());
            assertTrue(status.recoveryReport().isPresent());
            assertTrue(Files.isDirectory(
                    journalRoot.resolve("enterprise-lab-experiment-journals-v1/journals")));
            EnterpriseLabExperimentController controller = new EnterpriseLabExperimentController(
                    service, new AdaptiveRoutingPolicyProperties());
            var journals = (EnterpriseLabExperimentController.DurableJournalListResponse)
                    controller.durableJournals().getBody();
            assertEquals(0, journals.count());
            assertEquals(0, ((com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalDirectory.RetentionReport)
                    controller.enforceDurableRetention(
                            new EnterpriseLabExperimentController.RetentionRequest(0, true)).getBody())
                    .actions().size());

            IllegalStateException competing = assertThrows(
                    IllegalStateException.class,
                    () -> configuration.enterpriseLabExperimentOperatorService(
                            EnterpriseLabExperimentTargetCatalog.empty(), journalRoot.toString()));
            assertTrue(competing.getMessage().contains("DUPLICATE_ACQUISITION"));
        }
        try (EnterpriseLabExperimentOperatorService restarted =
                configuration.enterpriseLabExperimentOperatorService(
                        EnterpriseLabExperimentTargetCatalog.empty(), journalRoot.toString())) {
            assertEquals(InitializationState.READY, restarted.recoveryStatus().state());
            assertTrue(restarted.recoveryStatus().admissionAllowed());
        }
        assertThrows(IllegalArgumentException.class,
                () -> configuration.enterpriseLabExperimentOperatorService(
                        EnterpriseLabExperimentTargetCatalog.empty(), "target/relative-journal-root"));
    }

    @Test
    void releasedOwnerBeforeJournalInitializationCanRestartThroughTakeover() {
        var firstAcquisition = EnterpriseLabEvidenceOwnershipManager.acquire(
                journalRoot,
                EnterpriseLabEvidenceOwnership.Policy.safetyFirstDefaults(),
                java.time.Clock.systemUTC());
        assertTrue(firstAcquisition.ownership().isPresent());
        assertTrue(firstAcquisition.ownership().orElseThrow().release()
                .operatingSystemLockReleased());
        assertTrue(Files.notExists(journalRoot.resolve(
                "enterprise-lab-experiment-journals-v1/journals")));

        var configuration = new EnterpriseLabExperimentController.EnterpriseLabExperimentConfiguration();
        try (EnterpriseLabExperimentOperatorService restarted =
                configuration.enterpriseLabExperimentOperatorService(
                        EnterpriseLabExperimentTargetCatalog.empty(), journalRoot.toString())) {
            assertEquals(InitializationState.READY, restarted.recoveryStatus().state());
            assertTrue(restarted.recoveryStatus().admissionAllowed());
            assertTrue(Files.isDirectory(
                    journalRoot.resolve("enterprise-lab-experiment-journals-v1/journals")));
        }
    }

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

        mockMvc.perform(get("/api/lab/experiments/durable"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.configured", is(false)))
                .andExpect(jsonPath("$.reasonCode", is("DURABLE_EVIDENCE_NOT_CONFIGURED")))
                .andExpect(content().string(not(containsString(journalRoot.toString()))));

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
