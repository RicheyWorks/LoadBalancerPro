package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabEvidenceOwnershipProofCommandTest {
    @Test
    void commandProvesSeparateProcessExclusionTakeoverAndRestartRecovery() throws Exception {
        Path output = Path.of(
                "target", "enterprise-lab-ownership-command-test-" + System.nanoTime());
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        var result = EnterpriseLabEvidenceOwnershipProofCommand.run(
                new String[]{
                        "--enterprise-lab-ownership-proof",
                        "--enterprise-lab-ownership-proof-output=" + output},
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));

        assertEquals(0, result.exitCode(), () -> stderr.toString(StandardCharsets.UTF_8));
        String console = stdout.toString(StandardCharsets.UTF_8);
        assertTrue(console.contains("Enterprise Lab Ownership Proof"));
        assertTrue(console.contains("Separate-process live-owner denial: true"));
        assertFalse(console.contains("Started LoadBalancerApiApplication"));
        var report = new ObjectMapper().findAndRegisterModules().readTree(
                output.resolve("enterprise-lab-ownership-proof.json").toFile());
        assertTrue(report.path("allPassed").asBoolean());
        assertTrue(report.path("liveOwnerDenied").asBoolean());
        assertTrue(report.path("ownerAppendAndReconciliationVerified").asBoolean());
        assertTrue(report.path("nonOwnerAppendDenied").asBoolean());
        assertTrue(report.path("nonOwnerCompactionDenied").asBoolean());
        assertTrue(report.path("nonOwnerRetentionDenied").asBoolean());
        assertTrue(report.path("nonOwnerExperimentStartDenied").asBoolean());
        assertTrue(report.path("nonOwnerAllocationChangeDenied").asBoolean());
        assertTrue(report.path("renewalSucceeded").asBoolean());
        assertTrue(report.path("cleanReleaseRecorded").asBoolean());
        assertTrue(report.path("repeatedReleaseIdempotent").asBoolean());
        assertTrue(report.path("cleanTakeoverClassified").asBoolean());
        assertTrue(report.path("restartedPriorOwnerDenied").asBoolean());
        assertTrue(report.path("abruptStaleOwnerClassified").asBoolean());
        assertTrue(report.path("journalsVerifiedAndReplayed").asBoolean());
        assertTrue(report.path("interruptedExperimentRolledBack").asBoolean());
        assertTrue(report.path("baselineRestorationVerified").asBoolean());
        assertTrue(report.path("takeoverRecoveryRecorded").asBoolean());
        assertTrue(report.path("repeatedRestartIdempotent").asBoolean());
        assertTrue(report.path("simultaneousAcquisitionSingleWinner").asBoolean());
        assertTrue(report.path("competingTakeoverSingleWinner").asBoolean());
        assertTrue(report.path("cleanTakeoverGeneration").asLong()
                > report.path("initialGeneration").asLong());
        assertTrue(report.path("abruptTakeoverGeneration").asLong()
                > report.path("cleanTakeoverGeneration").asLong());
        assertTrue(Files.exists(output.resolve("enterprise-lab-ownership-proof-summary.md")));
    }

    @Test
    void commandAndChildBoundariesRejectOutsideTargetOrCallerDirectedState() throws Exception {
        assertTrue(EnterpriseLabEvidenceOwnershipProofCommand.isRequested(
                new String[]{"--enterprise-lab-ownership-proof"}));
        assertTrue(EnterpriseLabEvidenceOwnershipProofCommand.isRequested(
                new String[]{"--enterprise-lab-ownership-proof-child=contend"}));
        assertFalse(EnterpriseLabEvidenceOwnershipProofCommand.isRequested(
                new String[]{"--enterprise-lab-durable-recovery-proof"}));

        ByteArrayOutputStream outsideError = new ByteArrayOutputStream();
        var outside = EnterpriseLabEvidenceOwnershipProofCommand.run(
                new String[]{
                        "--enterprise-lab-ownership-proof",
                        "--enterprise-lab-ownership-proof-output=outside-target/ownership"},
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(outsideError));
        assertEquals(1, outside.exitCode());
        assertTrue(outsideError.toString().contains("must stay under target"));

        Path output = Path.of("target", "ownership-child-boundary-" + System.nanoTime());
        ByteArrayOutputStream childError = new ByteArrayOutputStream();
        var child = EnterpriseLabEvidenceOwnershipProofCommand.run(
                new String[]{
                        "--enterprise-lab-ownership-proof-child=force-unlock",
                        "--enterprise-lab-ownership-proof-output=" + output,
                        "--enterprise-lab-ownership-proof-run=caller-selected-owner",
                        "--enterprise-lab-ownership-proof-case=lifecycle"},
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(childError));
        assertEquals(1, child.exitCode());
        assertTrue(childError.toString().contains("run token"));

        String script = Files.readString(Path.of(
                "scripts", "smoke", "enterprise-lab-ownership-proof.ps1")).toLowerCase();
        assertTrue(script.contains("--enterprise-lab-ownership-proof"));
        assertTrue(script.contains("assert-outputundertarget"));
        assertTrue(script.contains("competingtakeoversinglewinner"));
        assertTrue(script.contains("abruptstaleownerclassified"));
        assertFalse(script.contains("invoke-webrequest"));
        assertFalse(script.contains("invoke-restmethod"));
        assertFalse(script.contains("force-unlock"));
    }

    @Test
    void commandRejectsTargetSymlinkBeforeCreatingProofStateOutsideTarget() throws Exception {
        Path outside = Files.createTempDirectory("ownership-proof-symlink-outside-");
        Path link = Path.of("target", "ownership-proof-symlink-" + System.nanoTime());
        try {
            try {
                Files.createSymbolicLink(link, outside.toAbsolutePath());
            } catch (IOException | UnsupportedOperationException | SecurityException exception) {
                Files.writeString(link, "non-directory-output-boundary", StandardCharsets.UTF_8);
            }

            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            var result = EnterpriseLabEvidenceOwnershipProofCommand.run(
                    new String[]{
                            "--enterprise-lab-ownership-proof",
                            "--enterprise-lab-ownership-proof-output=" + link},
                    new PrintStream(new ByteArrayOutputStream()),
                    new PrintStream(stderr));

            assertEquals(1, result.exitCode());
            assertTrue(stderr.toString(StandardCharsets.UTF_8)
                    .contains("symbolic links or non-directories"));
            try (var entries = Files.list(outside)) {
                assertTrue(entries.findAny().isEmpty());
            }
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(outside);
        }
    }
}
