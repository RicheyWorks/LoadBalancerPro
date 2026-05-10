package com.richmond423.loadbalancerpro.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.api.AllocationEvaluationResponse;
import com.richmond423.loadbalancerpro.api.RemediationReportFormat;
import com.richmond423.loadbalancerpro.api.RemediationReportRequest;
import com.richmond423.loadbalancerpro.api.RemediationReportService;
import com.richmond423.loadbalancerpro.core.CloudManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class RemediationReportCliTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String EVALUATION_FIXTURE =
            "remediation-reports/evaluation-overload-response.json";
    private static final String REPLAY_FIXTURE =
            "remediation-reports/replay-mixed-incident-response.json";

    @TempDir
    Path tempDir;

    @Test
    void savedEvaluationJsonEmitsMarkdownReport() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");

        CapturedRun run = runReport("--remediation-report", "--input", input.toString(),
                "--format", "markdown", "--report-id", "incident-cli", "--title", "CLI Overload Report");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.error().isBlank());
        assertTrue(run.output().contains("# CLI Overload Report"));
        assertTrue(run.output().contains("Report ID: incident-cli"));
        assertTrue(run.output().contains("Source: EVALUATION"));
        assertTrue(run.output().contains("Unallocated load: 50.000"));
        assertTrue(run.output().contains("SCALE_UP"));
        assertTrue(run.output().contains("SHED_LOAD"));
        assertTrue(run.output().contains("Cloud mutation: false"));
    }

    @Test
    void savedReplayJsonEmitsStructuredJsonReport() throws Exception {
        Path input = copyResource(REPLAY_FIXTURE, "replay.json");

        CapturedRun run = runReport("--remediation-report=" + input, "--format=json", "--report-id=incident-replay");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.error().isBlank());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("incident-replay", report.path("reportId").asText());
        assertEquals("SCENARIO_REPLAY", report.path("sourceType").asText());
        assertEquals("OVERLOADED", report.path("status").asText());
        assertEquals("route-before-incident", report.path("steps").get(0).path("stepId").asText());
        assertEquals("overload-spike", report.path("steps").get(1).path("stepId").asText());
        assertEquals("SCALE_UP", report.path("remediationPlan").path("recommendations").get(0).path("action").asText());
        assertFalse(report.path("cloudMutation").asBoolean());
    }

    @Test
    void outputFileReceivesReportWhenRequested() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path output = tempDir.resolve("incident.md");

        CapturedRun run = runReport("--remediation-report", "--input", input.toString(),
                "--format", "markdown", "--output", output.toString(), "--report-id", "file-report");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().isBlank());
        String written = Files.readString(output);
        assertTrue(written.contains("Report ID: file-report"));
        assertTrue(written.contains("## Safety Guarantees"));
    }

    @Test
    void repeatedRunsProduceDeterministicOutput() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");

        CapturedRun first = runReport("--remediation-report", "--input", input.toString(),
                "--format", "markdown", "--report-id", "stable");
        CapturedRun second = runReport("--remediation-report", "--input", input.toString(),
                "--format", "markdown", "--report-id", "stable");

        assertEquals(0, first.result().exitCode());
        assertEquals(first.output(), second.output());
        assertEquals(first.error(), second.error());
    }

    @Test
    void cliMarkdownMatchesReportServiceForSameEvaluationRequest() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        AllocationEvaluationResponse evaluation = OBJECT_MAPPER.readValue(
                input.toFile(), AllocationEvaluationResponse.class);
        String expected = new RemediationReportService().export(new RemediationReportRequest(
                "parity", "LoadBalancerPro Remediation Report", RemediationReportFormat.MARKDOWN,
                evaluation, null)).report();

        CapturedRun run = runReport("--remediation-report", "--input", input.toString(),
                "--format", "markdown", "--report-id", "parity");

        assertEquals(0, run.result().exitCode());
        assertEquals(expected, run.output());
    }

    @Test
    void savedReportResponseJsonCanBeRenderedAsMarkdown() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        AllocationEvaluationResponse evaluation = OBJECT_MAPPER.readValue(
                input.toFile(), AllocationEvaluationResponse.class);
        Path reportResponse = tempDir.resolve("saved-report-response.json");
        Files.writeString(reportResponse, OBJECT_MAPPER.writeValueAsString(new RemediationReportService().export(
                new RemediationReportRequest("saved-response", "Saved Response", RemediationReportFormat.JSON,
                        evaluation, null))));

        CapturedRun run = runReport("--remediation-report", "--input", reportResponse.toString(),
                "--format", "markdown");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.output().contains("# Saved Response"));
        assertTrue(run.output().contains("Report ID: saved-response"));
        assertTrue(run.output().contains("SCALE_UP"));
    }

    @Test
    void invalidInputFailsSafelyWithUsage() throws Exception {
        Path invalid = tempDir.resolve("invalid.json");
        Files.writeString(invalid, "{}", StandardCharsets.UTF_8);

        CapturedRun run = runReport("--remediation-report", "--input", invalid.toString());

        assertEquals(2, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().contains("Remediation report export failed safely"));
        assertTrue(run.error().contains("Input JSON must be"));
        assertTrue(run.error().contains("Usage: --remediation-report"));
        assertFalse(run.error().contains("\tat "));
    }

    @Test
    void reportCliDoesNotConstructCloudManager() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");

        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            CapturedRun run = runReport("--remediation-report", "--input", input.toString(),
                    "--format", "markdown");

            assertEquals(0, run.result().exitCode());
            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Offline report CLI must not construct CloudManager or enter cloud paths.");
        }
    }

    @Test
    void outputAvoidsSpringStartupMarkersAndCredentialTerms() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");

        CapturedRun run = runReport("--remediation-report", "--input", input.toString());
        String combined = run.output() + run.error();
        String lower = combined.toLowerCase();

        assertFalse(combined.contains("Started LoadBalancerApiApplication"));
        assertFalse(combined.contains("Tomcat started"));
        assertFalse(combined.contains("SpringApplication"));
        assertFalse(lower.contains("password"));
        assertFalse(lower.contains("secret"));
        assertFalse(lower.contains("credential"));
        assertFalse(lower.contains("accesskey"));
        assertFalse(lower.contains("token"));
    }

    @Test
    void requestDetectionOnlyMatchesReportFlag() {
        assertTrue(RemediationReportCli.isRequested(new String[]{"--remediation-report", "--input", "report.json"}));
        assertTrue(RemediationReportCli.isRequested(new String[]{"--remediation-report=report.json"}));
        assertFalse(RemediationReportCli.isRequested(new String[]{"--lase-replay=events.jsonl"}));
        assertFalse(RemediationReportCli.isRequested(new String[]{"--server.port=18080"}));
    }

    private Path copyResource(String resourcePath, String fileName) throws Exception {
        Path target = tempDir.resolve(fileName);
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream input = resource.getInputStream()) {
            Files.write(target, input.readAllBytes());
        }
        return target;
    }

    private CapturedRun runReport(String... args) {
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
