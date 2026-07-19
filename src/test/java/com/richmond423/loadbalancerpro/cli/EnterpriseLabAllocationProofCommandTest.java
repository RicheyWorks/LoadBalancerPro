package com.richmond423.loadbalancerpro.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabAllocationProofCommandTest {
    @Test
    void recognizesForegroundAndHolderModesWithoutClaimingApiArguments() {
        assertTrue(EnterpriseLabAllocationProofCommand.isRequested(
                new String[]{"--enterprise-lab-allocation-proof"}));
        assertTrue(EnterpriseLabAllocationProofCommand.isRequested(
                new String[]{"--enterprise-lab-allocation-proof-holder"}));
        assertFalse(EnterpriseLabAllocationProofCommand.isRequested(
                new String[]{"--server.port=18080"}));
        assertFalse(EnterpriseLabAllocationProofCommand.isRequested(null));
    }

    @Test
    void rejectsOutsideTargetOutputAndHolderWithoutRunToken() {
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        var outside = EnterpriseLabAllocationProofCommand.run(
                new String[]{
                        "--enterprise-lab-allocation-proof",
                        "--enterprise-lab-allocation-proof-output=outside-target"
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(errors));
        assertTrue(outside.requested());
        assertEquals(1, outside.exitCode());
        assertTrue(errors.toString(StandardCharsets.UTF_8)
                .contains("failed safely"));

        errors.reset();
        var holder = EnterpriseLabAllocationProofCommand.run(
                new String[]{
                        "--enterprise-lab-allocation-proof-holder",
                        "--enterprise-lab-allocation-proof-output=target/allocation-holder-command-test"
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(errors));
        assertEquals(1, holder.exitCode());
        assertTrue(errors.toString(StandardCharsets.UTF_8)
                .contains("run token is required"));
    }
}
