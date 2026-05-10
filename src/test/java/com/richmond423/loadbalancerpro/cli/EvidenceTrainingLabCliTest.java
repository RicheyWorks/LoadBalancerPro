package com.richmond423.loadbalancerpro.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.core.CloudManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

class EvidenceTrainingLabCliTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void trainingLabRunsAllPackagedExamplesWithExpectedCounts() throws Exception {
        CapturedRun run = runCli("--run-policy-training-lab", "--training-lab-format", "json");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.error().isBlank());
        JsonNode transcript = OBJECT_MAPPER.readTree(run.output());
        JsonNode summary = transcript.path("summary");
        assertEquals("PASS", summary.path("finalStatus").asText());
        assertEquals(7, summary.path("totalExamples").asInt());
        assertEquals(2, summary.path("expectedPassCount").asInt());
        assertEquals(3, summary.path("expectedWarnCount").asInt());
        assertEquals(2, summary.path("expectedFailCount").asInt());
        assertEquals(7, summary.path("matchedCount").asInt());
        assertEquals(0, summary.path("mismatchCount").asInt());
    }

    @Test
    void jsonTranscriptIsDeterministic() {
        CapturedRun first = runCli("--run-policy-training-lab", "--training-lab-format", "json");
        CapturedRun second = runCli("--run-policy-training-lab", "--training-lab-format", "json");

        assertEquals(0, first.result().exitCode());
        assertEquals(0, second.result().exitCode());
        assertEquals(first.output(), second.output());
        assertTrue(first.output().contains("\"exampleName\" : \"strict-zero-drift-pass\""));
    }

    @Test
    void markdownTranscriptIsDeterministic() {
        CapturedRun first = runCli("--run-policy-training-lab");
        CapturedRun second = runCli("--run-policy-training-lab");

        assertEquals(0, first.result().exitCode());
        assertEquals(first.output(), second.output());
        assertTrue(first.output().contains("# LoadBalancerPro Evidence Policy Training Lab"));
        assertTrue(first.output().contains("- Final status: PASS"));
        assertTrue(first.output().contains("| receiver-redaction-warn | receiver-redaction | WARN | WARN | MATCH |"));
        assertTrue(first.output().contains("not a legal chain-of-custody system"));
    }

    @Test
    void includeTrainingDetailsAddsPerChangeData() throws Exception {
        CapturedRun run = runCli("--run-policy-training-lab",
                "--training-lab-format", "json",
                "--include-training-details");

        assertEquals(0, run.result().exitCode());
        JsonNode examples = OBJECT_MAPPER.readTree(run.output()).path("examples");
        JsonNode receiver = null;
        for (JsonNode example : examples) {
            if ("receiver-redaction-warn".equals(example.path("exampleName").asText())) {
                receiver = example;
                break;
            }
        }
        assertTrue(receiver != null, "receiver-redaction-warn should be included");
        assertTrue(receiver.path("changes").isArray());
        assertEquals("incident-bundle.zip", receiver.path("changes").get(0).path("path").asText());
    }

    @Test
    void exportDirectoryWritesPackagedExamplesDeterministically() throws Exception {
        Path exportDirectory = tempDir.resolve("training-examples");
        CapturedRun run = runCli("--run-policy-training-lab",
                "--training-lab-export-dir", exportDirectory.toString(),
                "--training-lab-format", "json");
        EvidencePolicyExampleService exampleService = new EvidencePolicyExampleService();

        assertEquals(0, run.result().exitCode());
        for (EvidencePolicyExampleService.PolicyExample example : exampleService.listExamples()) {
            Path exampleDirectory = exportDirectory.resolve(example.name());
            assertEquals(exampleService.exampleFileText(example.name(), EvidencePolicyExampleService.BEFORE_FILE),
                    Files.readString(exampleDirectory.resolve(EvidencePolicyExampleService.BEFORE_FILE)));
            assertEquals(exampleService.exampleFileText(example.name(), EvidencePolicyExampleService.AFTER_FILE),
                    Files.readString(exampleDirectory.resolve(EvidencePolicyExampleService.AFTER_FILE)));
            assertEquals(exampleService.exampleFileText(example.name(), EvidencePolicyExampleService.EXPECTED_DECISION_FILE),
                    Files.readString(exampleDirectory.resolve(EvidencePolicyExampleService.EXPECTED_DECISION_FILE)));
        }
    }

    @Test
    void existingExportFilesRequireForce() {
        Path exportDirectory = tempDir.resolve("training-examples");
        CapturedRun first = runCli("--run-policy-training-lab",
                "--training-lab-export-dir", exportDirectory.toString());
        CapturedRun conflict = runCli("--run-policy-training-lab",
                "--training-lab-export-dir", exportDirectory.toString());
        CapturedRun forced = runCli("--run-policy-training-lab",
                "--training-lab-export-dir", exportDirectory.toString(),
                "--force");

        assertEquals(0, first.result().exitCode());
        assertEquals(2, conflict.result().exitCode());
        assertTrue(conflict.error().contains("target file already exists"));
        assertEquals(0, forced.result().exitCode());
    }

    @Test
    void mismatchDetectionAndFailOnMismatchBehaviorAreControlled() throws Exception {
        EvidencePolicyTrainingLabService service = new EvidencePolicyTrainingLabService();
        EvidencePolicyTrainingLabService.TrainingLabResult result = service.run(
                new EvidencePolicyTrainingLabService.TrainingLabRequest(
                        java.util.Optional.empty(),
                        false,
                        false),
                Map.of("receiver-redaction-warn", "PASS"));

        assertEquals(EvidencePolicyTrainingLabService.TrainingLabStatus.FAIL, result.summary().finalStatus());
        assertEquals(1, result.summary().mismatchCount());
        assertEquals(2, service.exitCode(result, true));
        assertEquals(0, service.exitCode(result, false));
    }

    @Test
    void trainingLabOutputCanBeWrittenToFile() throws Exception {
        Path output = tempDir.resolve("training-lab.json");
        CapturedRun run = runCli("--run-policy-training-lab",
                "--training-lab-format", "json",
                "--training-lab-output", output.toString());

        assertEquals(0, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertEquals("PASS", OBJECT_MAPPER.readTree(output.toFile())
                .path("summary")
                .path("finalStatus")
                .asText());
    }

    @Test
    void trainingLabDoesNotConstructCloudManager() {
        try (MockedConstruction<CloudManager> mocked = Mockito.mockConstruction(CloudManager.class)) {
            CapturedRun run = runCli("--run-policy-training-lab");

            assertEquals(0, run.result().exitCode());
            assertTrue(mocked.constructed().isEmpty());
        }
    }

    @Test
    void trainingLabFlagIsRecognized() {
        assertTrue(RemediationReportCli.isRequested(new String[]{"--run-policy-training-lab"}));
    }

    private CapturedRun runCli(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        RemediationReportCli.Result result = RemediationReportCli.run(args,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(error, true, StandardCharsets.UTF_8));
        return new CapturedRun(result, output.toString(StandardCharsets.UTF_8),
                error.toString(StandardCharsets.UTF_8));
    }

    private record CapturedRun(RemediationReportCli.Result result, String output, String error) {
    }
}
