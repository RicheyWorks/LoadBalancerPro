package com.richmond423.loadbalancerpro.api;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciler.DriftClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationReconciliationGate.InitializationState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationSupervisor.FingerprintComparison;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationSupervisor.SupervisionStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.AllocationSupervisionRestoration;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.ChangeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EnterpriseLabAllocationSupervisionControllerTest {
    private EnterpriseLabExperimentOperatorService operatorService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        operatorService = mock(EnterpriseLabExperimentOperatorService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new EnterpriseLabAllocationSupervisionController(operatorService))
                .build();
    }

    @Test
    void statusAndActionsReturnOnlyBoundedSanitizedSupervisionEvidence() throws Exception {
        SupervisionStatus status = statusFixture();
        when(operatorService.allocationSupervisionStatus())
                .thenReturn(Optional.of(status));
        when(operatorService.verifyAllocationSupervision())
                .thenReturn(Optional.of(status));
        when(operatorService.restoreSupervisedSafeBaseline())
                .thenReturn(Optional.of(new AllocationSupervisionRestoration(
                        ChangeStatus.NO_CHANGE,
                        false,
                        "ALLOCATION_RECONCILIATION_READY: durable baseline remained exact",
                        status)));

        mockMvc.perform(get("/api/lab/allocation-supervision"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured", is(true)))
                .andExpect(jsonPath("$.ready", is(true)))
                .andExpect(jsonPath("$.history.length()", is(0)))
                .andExpect(content().string(not(containsString("127.0.0.1"))))
                .andExpect(content().string(not(containsString("baselineAllocation"))))
                .andExpect(content().string(not(containsString("dataDirectory"))));

        mockMvc.perform(post("/api/lab/allocation-supervision/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready", is(true)));

        mockMvc.perform(post("/api/lab/allocation-supervision/restore-safe-baseline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restorationStatus", is("NO_CHANGE")))
                .andExpect(jsonPath("$.trafficActionPerformed", is(false)))
                .andExpect(jsonPath("$.status.ready", is(true)))
                .andExpect(content().string(not(containsString("allocations"))));
    }

    @Test
    void mutationInputsAreRejectedBeforeCallingSupervisionService() throws Exception {
        String body = """
                {
                  "allocation":{"blue":1.0},
                  "ownerGeneration":99,
                  "force":true,
                  "path":"outside"
                }
                """;

        mockMvc.perform(post("/api/lab/allocation-supervision/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reasonCode",
                        is("ALLOCATION_SUPERVISION_INPUT_REJECTED")));

        mockMvc.perform(post("/api/lab/allocation-supervision/restore-safe-baseline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accepted", is(false)));

        verifyNoInteractions(operatorService);
    }

    private static SupervisionStatus statusFixture() {
        String none = EnterpriseLabAllocationState.NO_FINGERPRINT;
        return new SupervisionStatus(
                "enterprise-lab-allocation-supervision-status/v1",
                true,
                true,
                InitializationState.READY,
                "ALLOCATION_RECONCILIATION_READY",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0L,
                0L,
                0L,
                new FingerprintComparison(none, none, none, true, false, false),
                DriftClassification.NO_PRIOR_ALLOCATION_EVIDENCE,
                Optional.empty(),
                List.of(),
                0,
                0,
                "sanitized fixed-target summaries only");
    }
}
