package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentProofExporter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabExperimentProofCommandTest {
    private static final Path SCRIPT = Path.of("scripts", "smoke", "enterprise-lab-experiment-proof.ps1");

    @Test
    void completionCommandWritesImmutableProofWithoutStartingApi() throws Exception {
        Path output = Path.of("target", "enterprise-lab-experiment-proof-command-test");
        CapturedRun run = runCommand(
                "--enterprise-lab-experiment-proof=completion",
                "--enterprise-lab-experiment-output=" + output);

        assertEquals(0, run.result.exitCode());
        assertTrue(run.output.contains("Enterprise Lab Experiment Proof"));
        assertTrue(run.output.contains("Suite: completion"));
        assertTrue(run.output.contains("Scenario count: 3"));
        assertTrue(run.output.contains("All checks passed: true"));
        assertTrue(run.output.contains("Safety: foreground bounded literal-loopback proof"));
        assertFalse(run.output.contains("Started LoadBalancerApiApplication"));
        assertTrue(run.error.isBlank());

        Path reportPath = output.resolve("enterprise-lab-experiment-proof.json");
        assertTrue(Files.exists(reportPath));
        assertTrue(Files.exists(output.resolve("enterprise-lab-experiment-proof-summary.md")));
        assertTrue(Files.exists(output.resolve("enterprise-lab-experiment-proof-metadata.json")));
        JsonNode report = new ObjectMapper().findAndRegisterModules().readTree(reportPath.toFile());
        assertTrue(report.path("allPassed").asBoolean());
        assertEquals(3, report.path("scenarios").size());
        assertTrue(report.path("scenarios").valueStream()
                .allMatch(value -> "COMPLETED".equals(value.path("finalRecord").path("lifecycle").path("state").asText())));
    }

    @Test
    void invalidOutputAndSuiteFailBeforeUnsafeAction() {
        CapturedRun invalidOutput = runCommand(
                "--enterprise-lab-experiment-proof=completion",
                "--enterprise-lab-experiment-output=release-downloads/proof");
        assertEquals(1, invalidOutput.result.exitCode());
        assertTrue(invalidOutput.error.contains("must stay under target"));

        CapturedRun invalidSuite = runCommand(
                "--enterprise-lab-experiment-proof=external",
                "--enterprise-lab-experiment-output=target/invalid-suite-proof");
        assertEquals(1, invalidSuite.result.exitCode());
        assertTrue(invalidSuite.error.contains("completion, rollback, or all"));
        assertThrows(IllegalArgumentException.class,
                () -> EnterpriseLabExperimentProofExporter.validateOutputDirectory(Path.of("outside-target")));
    }

    @Test
    void commandDetectionAndPackagedSmokeContractStayExplicitAndBounded() throws Exception {
        assertTrue(EnterpriseLabExperimentProofCommand.isRequested(
                new String[]{"--enterprise-lab-experiment-proof"}));
        assertTrue(EnterpriseLabExperimentProofCommand.isRequested(
                new String[]{"--enterprise-lab-experiment-proof=rollback"}));
        assertFalse(EnterpriseLabExperimentProofCommand.isRequested(
                new String[]{"--enterprise-lab-workflow=all"}));
        assertFalse(EnterpriseLabExperimentProofCommand.isRequested(new String[]{"--server.port=18080"}));

        String script = Files.readString(SCRIPT).toLowerCase();
        assertTrue(script.contains("--enterprise-lab-experiment-proof=$suite"));
        assertTrue(script.contains("--enterprise-lab-experiment-output=$outputdir"));
        assertTrue(script.contains("assert-outputundertarget"));
        assertTrue(script.contains("literal-loopback"));
        assertTrue(script.contains("convertfrom-json"));
        assertTrue(script.contains("baselineRestored".toLowerCase()));
        assertFalse(script.contains("invoke-webrequest"));
        assertFalse(script.contains("invoke-restmethod"));
    }

    private static CapturedRun runCommand(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        EnterpriseLabExperimentProofCommand.Result result = EnterpriseLabExperimentProofCommand.run(
                args,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(error, true, StandardCharsets.UTF_8));
        return new CapturedRun(
                result,
                output.toString(StandardCharsets.UTF_8),
                error.toString(StandardCharsets.UTF_8));
    }

    private record CapturedRun(
            EnterpriseLabExperimentProofCommand.Result result,
            String output,
            String error) {
    }
}
