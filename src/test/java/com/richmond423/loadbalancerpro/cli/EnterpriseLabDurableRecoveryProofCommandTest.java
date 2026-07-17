package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabDurableRecoveryProofCommandTest {
    @Test
    void commandRunsCrashBoundaryCorruptionAndCompactionWithoutStartingTheApi() throws Exception {
        Path output = Path.of(
                "target", "enterprise-lab-durable-recovery-command-test-" + System.nanoTime());
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        var result = EnterpriseLabDurableRecoveryProofCommand.run(
                new String[]{
                        "--enterprise-lab-durable-recovery-proof",
                        "--enterprise-lab-durable-recovery-output=" + output},
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));

        assertEquals(0, result.exitCode(), () -> stderr.toString(StandardCharsets.UTF_8));
        assertTrue(stdout.toString(StandardCharsets.UTF_8)
                .contains("Enterprise Lab Durable Recovery Proof"));
        assertFalse(stdout.toString(StandardCharsets.UTF_8)
                .contains("Started LoadBalancerApiApplication"));
        var report = new ObjectMapper().findAndRegisterModules().readTree(
                output.resolve("enterprise-lab-durable-recovery-proof.json").toFile());
        assertTrue(report.path("allPassed").asBoolean());
        assertEquals("ROLLED_BACK", report.path("interruptedFinalState").asText());
        assertEquals("COMPLETED", report.path("completedFinalState").asText());
        assertTrue(report.path("middleCorruptionQuarantined").asBoolean());
        assertTrue(report.path("partialTailQuarantined").asBoolean());
        assertTrue(report.path("terminalCompactionVerified").asBoolean());
        assertTrue(Files.exists(output.resolve("enterprise-lab-durable-recovery-proof-summary.md")));
    }

    @Test
    void commandDetectionAndOutputBoundaryAreFailClosed() throws Exception {
        assertTrue(EnterpriseLabDurableRecoveryProofCommand.isRequested(
                new String[]{"--enterprise-lab-durable-recovery-proof"}));
        assertFalse(EnterpriseLabDurableRecoveryProofCommand.isRequested(
                new String[]{"--enterprise-lab-experiment-proof=all"}));
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        var result = EnterpriseLabDurableRecoveryProofCommand.run(
                new String[]{
                        "--enterprise-lab-durable-recovery-proof",
                        "--enterprise-lab-durable-recovery-output=outside-target/proof"},
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(error));
        assertEquals(1, result.exitCode());
        assertTrue(error.toString().contains("must stay under target"));
        String script = Files.readString(Path.of(
                "scripts", "smoke", "enterprise-lab-durable-recovery-proof.ps1")).toLowerCase();
        assertTrue(script.contains("--enterprise-lab-durable-recovery-proof"));
        assertTrue(script.contains("assert-outputundertarget"));
        assertTrue(script.contains("middlecorruptionquarantined"));
        assertTrue(script.contains("terminalcompactionverified"));
        assertFalse(script.contains("invoke-webrequest"));
        assertFalse(script.contains("invoke-restmethod"));
    }
}
