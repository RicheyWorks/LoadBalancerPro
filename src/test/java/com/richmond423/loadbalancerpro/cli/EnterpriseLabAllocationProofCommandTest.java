package com.richmond423.loadbalancerpro.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    void testScopeLauncherAndSmokeScriptOwnProofDispatch() throws Exception {
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        assertEquals(2, EnterpriseLabProofToolsApplication.run(
                new String[]{"--server.port=18080"},
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(errors)));
        assertTrue(errors.toString(StandardCharsets.UTF_8)
                .contains("No Enterprise Lab proof tool command"));

        String script = Files.readString(Path.of(
                "scripts", "smoke", "enterprise-lab-allocation-proof.ps1"));
        assertTrue(script.contains("EnterpriseLabProofToolsApplication"));
        assertTrue(script.contains("--enterprise-lab-allocation-proof"));
        assertFalse(script.contains("java -jar"));
    }
}
