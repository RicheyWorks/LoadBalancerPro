package com.richmond423.loadbalancerpro.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabStorageRepairCommandTest {

    @Test
    void absentFlagDoesNotClaimTheProcess() {
        var result = EnterpriseLabStorageRepairCommand.runIfRequested(
                new String[]{"--server.port=0"},
                System.out,
                System.err);

        assertFalse(result.requested());
        assertEquals(0, result.exitCode());
    }

    @Test
    void missingBoundedTargetFailsSafelyWithoutStartingRepair() {
        ByteArrayOutputStream standard = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        var result = EnterpriseLabStorageRepairCommand.runIfRequested(
                new String[]{"--enterprise-lab-storage-repair"},
                new PrintStream(standard, true, StandardCharsets.UTF_8),
                new PrintStream(errors, true, StandardCharsets.UTF_8));

        assertTrue(result.requested());
        assertEquals(1, result.exitCode());
        assertEquals("", standard.toString(StandardCharsets.UTF_8));
        assertTrue(errors.toString(StandardCharsets.UTF_8)
                .contains("failed safely"));
    }
}
