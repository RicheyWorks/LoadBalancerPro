package com.richmond423.loadbalancerpro.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabSupervisorCommandTest {
    @Test
    void recognizesOnlyExplicitSupervisorMode() {
        assertTrue(EnterpriseLabSupervisorCommand.isRequested(
                new String[]{"--enterprise-lab-supervisor"}));
        assertFalse(EnterpriseLabSupervisorCommand.isRequested(
                new String[]{"--enterprise-lab-supervisor-port=0"}));
        assertFalse(EnterpriseLabSupervisorCommand.isRequested(null));
    }

    @Test
    void rejectsInvalidPortBeforeStartingAProcessOrListener() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        EnterpriseLabSupervisorCommand.Result result =
                EnterpriseLabSupervisorCommand.runIfRequested(
                        new String[]{
                                "--enterprise-lab-supervisor",
                                "--enterprise-lab-supervisor-port=70000"},
                        new PrintStream(output, true, StandardCharsets.UTF_8),
                        new PrintStream(error, true, StandardCharsets.UTF_8));

        assertTrue(result.requested());
        assertEquals(2, result.exitCode());
        assertEquals("", output.toString(StandardCharsets.UTF_8));
        assertEquals(
                "enterprise-lab supervisor failed safely" + System.lineSeparator(),
                error.toString(StandardCharsets.UTF_8));
    }

    @Test
    void ignoresUnrelatedCommands() {
        EnterpriseLabSupervisorCommand.Result result =
                EnterpriseLabSupervisorCommand.runIfRequested(
                        new String[]{"--enterprise-lab-workflow"},
                        System.out,
                        System.err);
        assertFalse(result.requested());
        assertEquals(0, result.exitCode());
    }
}
