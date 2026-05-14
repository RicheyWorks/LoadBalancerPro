package com.richmond423.loadbalancerpro.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class EnterpriseLabWorkflowCommandTest {

    @Test
    void commandWritesEnterpriseLabEvidenceWithoutStartingApi() {
        Path output = Path.of("target", "enterprise-lab-command-test");
        CapturedRun run = runCommand("--enterprise-lab-workflow=all",
                "--enterprise-lab-output=" + output);

        assertEquals(0, run.result().exitCode());
        assertTrue(run.output().contains("LoadBalancerPro Enterprise Lab Workflow"));
        assertTrue(run.output().contains("Mode: active-experiment"));
        assertTrue(run.output().contains("Scenario catalog JSON"));
        assertTrue(run.output().contains("Safety: ignored target/ evidence only"));
        assertTrue(Files.exists(output.resolve("enterprise-lab-scenario-catalog.json")));
        assertTrue(Files.exists(output.resolve("enterprise-lab-run.json")));
        assertTrue(Files.exists(output.resolve("enterprise-lab-run-summary.md")));
        assertFalse(run.output().contains("Started LoadBalancerApiApplication"));
        assertTrue(run.error().isBlank());
    }

    @Test
    void invalidOutputFailsSafely() {
        CapturedRun run = runCommand("--enterprise-lab-workflow=all",
                "--enterprise-lab-output=release-downloads/enterprise-lab");

        assertEquals(1, run.result().exitCode());
        assertTrue(run.error().contains("must stay under target"));
    }

    @Test
    void requestDetectionOnlyMatchesEnterpriseLabFlag() {
        assertTrue(EnterpriseLabWorkflowCommand.isRequested(new String[]{"--enterprise-lab-workflow"}));
        assertTrue(EnterpriseLabWorkflowCommand.isRequested(new String[]{"--enterprise-lab-workflow=all"}));
        assertFalse(EnterpriseLabWorkflowCommand.isRequested(new String[]{"--adaptive-routing-experiment=all"}));
        assertFalse(EnterpriseLabWorkflowCommand.isRequested(new String[]{"--server.port=18080"}));
    }

    @Test
    void bareWorkflowFlagDefaultsToShadowMode() {
        Path output = Path.of("target", "enterprise-lab-command-default-test");
        CapturedRun run = runCommand("--enterprise-lab-workflow",
                "--enterprise-lab-output=" + output);

        assertEquals(0, run.result().exitCode());
        assertTrue(run.output().contains("Mode: shadow"));
        assertFalse(run.output().contains("Mode: active-experiment"));
    }

    private CapturedRun runCommand(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        EnterpriseLabWorkflowCommand.Result result = EnterpriseLabWorkflowCommand.run(args,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(error, true, StandardCharsets.UTF_8));
        return new CapturedRun(result, output.toString(StandardCharsets.UTF_8),
                error.toString(StandardCharsets.UTF_8));
    }

    private record CapturedRun(EnterpriseLabWorkflowCommand.Result result, String output, String error) {
    }
}

