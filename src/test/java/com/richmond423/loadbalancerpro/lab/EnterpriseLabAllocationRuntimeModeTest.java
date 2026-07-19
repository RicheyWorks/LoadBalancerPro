package com.richmond423.loadbalancerpro.lab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnterpriseLabAllocationRuntimeModeTest {
    @Test
    void parsesOnlyExplicitUnambiguousWireValues() {
        assertEquals(
                EnterpriseLabAllocationRuntimeMode.IN_PROCESS,
                EnterpriseLabAllocationRuntimeMode.parse("in-process"));
        assertEquals(
                EnterpriseLabAllocationRuntimeMode.EXTERNAL_SUPERVISOR_REQUIRED,
                EnterpriseLabAllocationRuntimeMode.parse(
                        "external-supervisor-required"));
        assertEquals(
                EnterpriseLabAllocationRuntimeMode.DISABLED,
                EnterpriseLabAllocationRuntimeMode.parse("disabled"));
        assertThrows(
                IllegalArgumentException.class,
                () -> EnterpriseLabAllocationRuntimeMode.parse("IN_PROCESS"));
        assertThrows(
                IllegalArgumentException.class,
                () -> EnterpriseLabAllocationRuntimeMode.parse("fallback"));
        assertThrows(
                IllegalArgumentException.class,
                () -> EnterpriseLabAllocationRuntimeMode.parse(" "));
    }

    @Test
    void disabledModeDeniesArmWithoutConstructingAHiddenInProcessPath() {
        try (EnterpriseLabExperimentOperatorService service =
                     new EnterpriseLabExperimentOperatorService(
                             EnterpriseLabExperimentTargetCatalog.empty(),
                             EnterpriseLabAllocationRuntimeMode.DISABLED)) {
            var receipt = service.arm(
                    new EnterpriseLabExperimentOperatorService.ArmRequest(
                            "disabled-request",
                            "disabled-experiment",
                            "tail-latency-pressure",
                            10,
                            java.time.Duration.ofSeconds(30),
                            2,
                            1,
                            java.time.Duration.ofSeconds(60)),
                    true);
            assertEquals("ALLOCATION_MODE_DISABLED", receipt.reasonCode());
        }
    }
}
