package com.richmond423.loadbalancerpro.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
    void markdownReportCanEmitChecksumManifest() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path output = tempDir.resolve("incident-report.md");
        Path manifest = tempDir.resolve("incident-report.manifest.json");

        CapturedRun run = runReport("--remediation-report", "--input", input.toString(),
                "--format", "markdown", "--output", output.toString(), "--manifest", manifest.toString(),
                "--report-id", "manifest-md");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().isBlank());
        assertTrue(Files.readString(output).contains("Report ID: manifest-md"));

        JsonNode manifestJson = OBJECT_MAPPER.readTree(manifest.toFile());
        assertEquals("1", manifestJson.path("manifestVersion").asText());
        assertEquals("SHA-256", manifestJson.path("algorithm").asText());
        assertEquals("manifest-md", manifestJson.path("reportId").asText());
        assertEquals("EVALUATION", manifestJson.path("sourceType").asText());
        assertEquals("LoadBalancerPro offline remediation report CLI", manifestJson.path("generatedBy").asText());
        assertFalse(manifestJson.has("createdAt"));
        assertFalse(manifestJson.path("cloudMutation").asBoolean());
        assertManifestFile(manifestJson, "INPUT", "evaluation.json", input);
        assertManifestFile(manifestJson, "REPORT", "incident-report.md", output);
    }

    @Test
    void jsonReportCanEmitChecksumManifest() throws Exception {
        Path input = copyResource(REPLAY_FIXTURE, "replay.json");
        Path output = tempDir.resolve("incident-report.json");
        Path manifest = tempDir.resolve("incident-report.manifest.json");

        CapturedRun run = runReport("--remediation-report=" + input, "--format=json",
                "--output", output.toString(), "--manifest", manifest.toString(),
                "--report-id=manifest-json", "--created-at", "2026-05-10T00:00:00Z");

        assertEquals(0, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(output.toFile());
        JsonNode manifestJson = OBJECT_MAPPER.readTree(manifest.toFile());
        assertEquals("manifest-json", report.path("reportId").asText());
        assertEquals("SCENARIO_REPLAY", manifestJson.path("sourceType").asText());
        assertEquals("2026-05-10T00:00:00Z", manifestJson.path("createdAt").asText());
        assertManifestFile(manifestJson, "INPUT", "replay.json", input);
        assertManifestFile(manifestJson, "REPORT", "incident-report.json", output);
    }

    @Test
    void repeatedManifestGenerationIsDeterministic() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path firstOutput = tempDir.resolve("first.md");
        Path firstManifest = tempDir.resolve("first.manifest.json");
        Path secondOutput = tempDir.resolve("second.md");
        Path secondManifest = tempDir.resolve("second.manifest.json");

        CapturedRun first = runReport("--remediation-report", "--input", input.toString(),
                "--output", firstOutput.toString(), "--manifest", firstManifest.toString(),
                "--report-id", "stable-manifest");
        CapturedRun second = runReport("--remediation-report", "--input", input.toString(),
                "--output", secondOutput.toString(), "--manifest", secondManifest.toString(),
                "--report-id", "stable-manifest");

        assertEquals(0, first.result().exitCode());
        assertEquals(0, second.result().exitCode());
        JsonNode firstJson = OBJECT_MAPPER.readTree(firstManifest.toFile());
        JsonNode secondJson = OBJECT_MAPPER.readTree(secondManifest.toFile());
        assertEquals(firstJson.path("files").get(0).path("sha256").asText(),
                secondJson.path("files").get(0).path("sha256").asText());
        assertEquals(firstJson.path("files").get(1).path("sha256").asText(),
                secondJson.path("files").get(1).path("sha256").asText());
        assertEquals(Files.readString(firstOutput), Files.readString(secondOutput));
        assertFalse(firstJson.has("createdAt"));
        assertFalse(secondJson.has("createdAt"));
    }

    @Test
    void verifyManifestSucceedsForUnchangedBundle() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path output = tempDir.resolve("incident-report.md");
        Path manifest = tempDir.resolve("incident-report.manifest.json");
        runReport("--remediation-report", "--input", input.toString(),
                "--output", output.toString(), "--manifest", manifest.toString());

        CapturedRun verify = runReport("--verify-manifest", manifest.toString());

        assertEquals(0, verify.result().exitCode());
        assertTrue(verify.error().isBlank());
        assertTrue(verify.output().contains("Manifest verification passed"));
        assertTrue(verify.output().contains("OK [INPUT]"));
        assertTrue(verify.output().contains("OK [REPORT]"));
    }

    @Test
    void verifyManifestDetectsTamperedReport() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path output = tempDir.resolve("incident-report.md");
        Path manifest = tempDir.resolve("incident-report.manifest.json");
        runReport("--remediation-report", "--input", input.toString(),
                "--output", output.toString(), "--manifest", manifest.toString());
        Files.writeString(output, "\nunauthorized edit\n", StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);

        CapturedRun verify = runReport("--verify-manifest", manifest.toString());

        assertEquals(2, verify.result().exitCode());
        assertTrue(verify.error().isBlank());
        assertTrue(verify.output().contains("Manifest verification failed"));
        assertTrue(verify.output().contains("MISMATCH [REPORT]"));
    }

    @Test
    void verifyManifestDetectsMissingReport() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path output = tempDir.resolve("incident-report.md");
        Path manifest = tempDir.resolve("incident-report.manifest.json");
        runReport("--remediation-report", "--input", input.toString(),
                "--output", output.toString(), "--manifest", manifest.toString());
        Files.delete(output);

        CapturedRun verify = runReport("--verify-manifest=" + manifest);

        assertEquals(2, verify.result().exitCode());
        assertTrue(verify.error().isBlank());
        assertTrue(verify.output().contains("MISSING [REPORT]"));
    }

    @Test
    void markdownIncidentBundleIncludesReportManifestAndVerificationSummary() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path bundle = tempDir.resolve("incident-bundle.zip");

        CapturedRun run = runReport("--input", input.toString(),
                "--format", "markdown", "--bundle", bundle.toString(), "--report-id", "bundle-md");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.error().isBlank());
        assertTrue(run.output().contains("Incident bundle written"));
        assertTrue(run.output().contains("Incident bundle verification passed"));
        Map<String, byte[]> entries = zipEntries(bundle);
        assertTrue(entries.containsKey("input.json"));
        assertTrue(entries.containsKey("report.md"));
        assertTrue(entries.containsKey("manifest.json"));
        assertTrue(entries.containsKey("verification-summary.json"));
        assertTrue(entries.containsKey("README.md"));
        assertEquals(Files.readString(input), text(entries, "input.json"));
        assertTrue(text(entries, "report.md").contains("Report ID: bundle-md"));
        assertTrue(text(entries, "verification-summary.json").contains("\"verificationStatus\" : \"PASS\""));
        assertTrue(text(entries, "README.md").contains("not a cryptographic signature"));

        JsonNode manifestJson = OBJECT_MAPPER.readTree(entries.get("manifest.json"));
        assertManifestFile(manifestJson, "INPUT", "input.json", entries.get("input.json"));
        assertManifestFile(manifestJson, "REPORT", "report.md", entries.get("report.md"));
        assertManifestFile(manifestJson, "EXTRA", "README.md", entries.get("README.md"));
        assertManifestFile(manifestJson, "EXTRA", "verification-summary.json",
                entries.get("verification-summary.json"));
    }

    @Test
    void jsonIncidentBundleIncludesStructuredReportAndManifest() throws Exception {
        Path input = copyResource(REPLAY_FIXTURE, "replay.json");
        Path bundle = tempDir.resolve("incident-bundle.zip");

        CapturedRun run = runReport("--input", input.toString(),
                "--format=json", "--bundle", bundle.toString(), "--report-id=bundle-json");

        assertEquals(0, run.result().exitCode());
        Map<String, byte[]> entries = zipEntries(bundle);
        assertTrue(entries.containsKey("report.json"));
        assertFalse(entries.containsKey("report.md"));
        JsonNode report = OBJECT_MAPPER.readTree(entries.get("report.json"));
        JsonNode manifest = OBJECT_MAPPER.readTree(entries.get("manifest.json"));
        assertEquals("bundle-json", report.path("reportId").asText());
        assertEquals("SCENARIO_REPLAY", report.path("sourceType").asText());
        assertEquals("bundle-json", manifest.path("reportId").asText());
        assertManifestFile(manifest, "REPORT", "report.json", entries.get("report.json"));
    }

    @Test
    void markdownReportRedactsLiteralValuesAndWritesSafeSummary() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path summary = tempDir.resolve("redaction-summary.json");
        String sensitiveReportId = "host-a.internal";

        CapturedRun run = runReport("--remediation-report", "--input", input.toString(),
                "--format", "markdown", "--report-id", sensitiveReportId,
                "--redact", sensitiveReportId, "--redact", "primary",
                "--redact-output-summary", summary.toString());

        assertEquals(0, run.result().exitCode());
        assertTrue(run.error().isBlank());
        assertTrue(run.output().contains("[REDACTED]"));
        assertFalse(run.output().contains(sensitiveReportId));

        String summaryText = Files.readString(summary);
        JsonNode summaryJson = OBJECT_MAPPER.readTree(summaryText);
        assertTrue(summaryJson.path("redactionApplied").asBoolean());
        assertEquals(2, summaryJson.path("configuredTokenCount").asInt());
        assertTrue(summaryJson.path("totalReplacementCount").asInt() >= 2);
        assertTrue(summaryText.contains(ReportChecksumManifestService.sha256(
                sensitiveReportId.getBytes(StandardCharsets.UTF_8))));
        assertTrue(summaryText.contains(ReportChecksumManifestService.sha256(
                "primary".getBytes(StandardCharsets.UTF_8))));
        assertFalse(summaryText.contains(sensitiveReportId));
        assertFalse(summaryText.contains("primary"));
    }

    @Test
    void jsonReportRedactsSavedReplayValues() throws Exception {
        Path input = copyResource(REPLAY_FIXTURE, "replay.json");

        CapturedRun run = runReport("--remediation-report=" + input, "--format=json",
                "--redact", "green", "--redaction-label", "[MASKED]");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.error().isBlank());
        assertFalse(run.output().contains("green"));
        assertTrue(run.output().contains("[MASKED]"));
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("[MASKED]", report.path("steps").get(0).path("selectedServerId").asText());
    }

    @Test
    void redactedBundleContainsRedactedInputReportSummaryAndVerifies() throws Exception {
        Path input = copyResource(REPLAY_FIXTURE, "replay.json");
        Path bundle = tempDir.resolve("incident-bundle.zip");

        CapturedRun run = runReport("--input", input.toString(), "--format", "markdown",
                "--bundle", bundle.toString(),
                "--redact", "mixed-incident-replay", "--redact", "green");

        assertEquals(0, run.result().exitCode());
        Map<String, byte[]> entries = zipEntries(bundle);
        assertTrue(entries.containsKey("redaction-summary.json"));
        assertFalse(text(entries, "input.json").contains("mixed-incident-replay"));
        assertFalse(text(entries, "input.json").contains("green"));
        assertFalse(text(entries, "report.md").contains("mixed-incident-replay"));
        assertFalse(text(entries, "report.md").contains("green"));
        assertFalse(text(entries, "redaction-summary.json").contains("mixed-incident-replay"));
        assertFalse(text(entries, "redaction-summary.json").contains("green"));
        assertTrue(text(entries, "redaction-summary.json").contains(ReportChecksumManifestService.sha256(
                "mixed-incident-replay".getBytes(StandardCharsets.UTF_8))));
        assertTrue(text(entries, "README.md").contains("Redaction applied: true"));

        JsonNode manifest = OBJECT_MAPPER.readTree(entries.get("manifest.json"));
        assertManifestFile(manifest, "INPUT", "input.json", entries.get("input.json"));
        assertManifestFile(manifest, "REPORT", "report.md", entries.get("report.md"));
        assertManifestFile(manifest, "EXTRA", "redaction-summary.json", entries.get("redaction-summary.json"));

        CapturedRun verify = runReport("--verify-bundle", bundle.toString());
        assertEquals(0, verify.result().exitCode());
        assertTrue(verify.output().contains("Incident bundle verification passed"));
    }

    @Test
    void redactionFileAppliesMultipleValuesDeterministically() throws Exception {
        Path input = copyResource(REPLAY_FIXTURE, "replay.json");
        Path redactionFile = tempDir.resolve("redactions.json");
        Path firstBundle = tempDir.resolve("first.zip");
        Path secondBundle = tempDir.resolve("second.zip");
        Files.writeString(redactionFile, "[\"green\",\"blue\"]", StandardCharsets.UTF_8);

        CapturedRun first = runReport("--input", input.toString(), "--format=json",
                "--bundle", firstBundle.toString(), "--redact-file", redactionFile.toString());
        CapturedRun second = runReport("--input", input.toString(), "--format=json",
                "--bundle", secondBundle.toString(), "--redact-file", redactionFile.toString());

        assertEquals(0, first.result().exitCode());
        assertEquals(0, second.result().exitCode());
        assertArrayEquals(Files.readAllBytes(firstBundle), Files.readAllBytes(secondBundle));
        String report = text(zipEntries(firstBundle), "report.json");
        assertFalse(report.contains("green"));
        assertFalse(report.contains("blue"));
        assertTrue(report.contains("[REDACTED]"));
    }

    @Test
    void redactedManifestHashesRedactedInputAndReportFiles() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path output = tempDir.resolve("incident-report.md");
        Path manifest = tempDir.resolve("incident-report.manifest.json");
        Path summary = tempDir.resolve("incident-report.redaction-summary.json");

        CapturedRun run = runReport("--remediation-report", "--input", input.toString(),
                "--format", "markdown", "--output", output.toString(), "--manifest", manifest.toString(),
                "--redact", "primary", "--redact-output-summary", summary.toString());

        assertEquals(0, run.result().exitCode());
        Path redactedInput = tempDir.resolve("incident-report.input.redacted.json");
        assertTrue(Files.exists(redactedInput));
        assertFalse(Files.readString(redactedInput).contains("primary"));
        assertFalse(Files.readString(output).contains("primary"));

        JsonNode manifestJson = OBJECT_MAPPER.readTree(manifest.toFile());
        assertManifestFile(manifestJson, "INPUT", "incident-report.input.redacted.json", redactedInput);
        assertManifestFile(manifestJson, "REPORT", "incident-report.md", output);
        assertManifestFile(manifestJson, "EXTRA", "incident-report.redaction-summary.json", summary);

        CapturedRun verify = runReport("--verify-manifest", manifest.toString());
        assertEquals(0, verify.result().exitCode());
    }

    @Test
    void noRedactionOptionsPreserveExistingOutputBehavior() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");

        CapturedRun baseline = runReport("--remediation-report", "--input", input.toString(),
                "--format", "markdown", "--report-id", "stable");
        CapturedRun withUnusedLabel = runReport("--remediation-report", "--input", input.toString(),
                "--format", "markdown", "--report-id", "stable", "--redaction-label", "[MASKED]");

        assertEquals(0, baseline.result().exitCode());
        assertEquals(0, withUnusedLabel.result().exitCode());
        assertEquals(baseline.output(), withUnusedLabel.output());
    }

    @Test
    void invalidRedactionFileFailsSafely() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path redactionFile = tempDir.resolve("invalid-redactions.json");
        Files.writeString(redactionFile, "[1]", StandardCharsets.UTF_8);

        CapturedRun run = runReport("--remediation-report", "--input", input.toString(),
                "--redact-file", redactionFile.toString());

        assertEquals(2, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().contains("redaction JSON file must contain only strings"));
        assertFalse(run.error().contains("\tat "));
    }

    @Test
    void verifyBundleSucceedsForUnchangedBundle() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path bundle = tempDir.resolve("incident-bundle.zip");
        runReport("--input", input.toString(), "--bundle", bundle.toString());

        CapturedRun verify = runReport("--verify-bundle", bundle.toString());

        assertEquals(0, verify.result().exitCode());
        assertTrue(verify.error().isBlank());
        assertTrue(verify.output().contains("Incident bundle verification passed"));
        assertTrue(verify.output().contains("OK [INPUT]"));
        assertTrue(verify.output().contains("OK [REPORT]"));
    }

    @Test
    void verifyBundleDetectsTamperedReport() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path bundle = tempDir.resolve("incident-bundle.zip");
        runReport("--input", input.toString(), "--bundle", bundle.toString());
        Map<String, byte[]> entries = zipEntries(bundle);
        entries.put("report.md", (text(entries, "report.md") + "\nunauthorized edit\n")
                .getBytes(StandardCharsets.UTF_8));
        writeZip(bundle, entries);

        CapturedRun verify = runReport("--verify-bundle", bundle.toString());

        assertEquals(2, verify.result().exitCode());
        assertTrue(verify.error().isBlank());
        assertTrue(verify.output().contains("Incident bundle verification failed"));
        assertTrue(verify.output().contains("MISMATCH [REPORT]"));
    }

    @Test
    void verifyBundleDetectsMissingManifestAndMissingFiles() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path bundle = tempDir.resolve("incident-bundle.zip");
        runReport("--input", input.toString(), "--bundle", bundle.toString());
        Map<String, byte[]> entries = zipEntries(bundle);

        Map<String, byte[]> withoutManifest = new LinkedHashMap<>(entries);
        withoutManifest.remove("manifest.json");
        Path missingManifestBundle = tempDir.resolve("missing-manifest.zip");
        writeZip(missingManifestBundle, withoutManifest);

        CapturedRun missingManifest = runReport("--verify-bundle", missingManifestBundle.toString());
        assertEquals(2, missingManifest.result().exitCode());
        assertTrue(missingManifest.output().contains("missing manifest.json"));

        Map<String, byte[]> withoutReport = new LinkedHashMap<>(entries);
        withoutReport.remove("report.md");
        Path missingReportBundle = tempDir.resolve("missing-report.zip");
        writeZip(missingReportBundle, withoutReport);

        CapturedRun missingReport = runReport("--verify-bundle", missingReportBundle.toString());
        assertEquals(2, missingReport.result().exitCode());
        assertTrue(missingReport.output().contains("MISSING [REPORT]"));
    }

    @Test
    void verifyBundleRejectsUnsafeZipEntryNames() throws Exception {
        Path unsafeBundle = tempDir.resolve("unsafe.zip");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("../evil.txt", "tamper".getBytes(StandardCharsets.UTF_8));
        writeZip(unsafeBundle, entries);

        CapturedRun verify = runReport("--verify-bundle", unsafeBundle.toString());

        assertEquals(2, verify.result().exitCode());
        assertTrue(verify.output().isBlank());
        assertTrue(verify.error().contains("unsafe bundle entry path"));
    }

    @Test
    void repeatedBundleGenerationIsByteStable() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path firstBundle = tempDir.resolve("first.zip");
        Path secondBundle = tempDir.resolve("second.zip");

        CapturedRun first = runReport("--input", input.toString(), "--bundle", firstBundle.toString(),
                "--report-id", "stable-bundle");
        CapturedRun second = runReport("--input", input.toString(), "--bundle", secondBundle.toString(),
                "--report-id", "stable-bundle");

        assertEquals(0, first.result().exitCode());
        assertEquals(0, second.result().exitCode());
        assertArrayEquals(Files.readAllBytes(firstBundle), Files.readAllBytes(secondBundle));
    }

    @Test
    void bundleInvalidInputFailsSafely() throws Exception {
        Path invalid = tempDir.resolve("invalid.json");
        Path bundle = tempDir.resolve("invalid-bundle.zip");
        Files.writeString(invalid, "{}", StandardCharsets.UTF_8);

        CapturedRun run = runReport("--input", invalid.toString(), "--bundle", bundle.toString());

        assertEquals(2, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().contains("Input JSON must be"));
        assertFalse(Files.exists(bundle));
    }

    @Test
    void bundleGenerationAndVerificationDoNotConstructCloudManager() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path bundle = tempDir.resolve("incident-bundle.zip");

        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            CapturedRun generate = runReport("--input", input.toString(), "--bundle", bundle.toString());
            CapturedRun verify = runReport("--verify-bundle", bundle.toString());

            assertEquals(0, generate.result().exitCode());
            assertEquals(0, verify.result().exitCode());
            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Incident bundle export and verification must not construct CloudManager.");
        }
    }

    @Test
    void invalidManifestFailsSafely() throws Exception {
        Path manifest = tempDir.resolve("invalid.manifest.json");
        Files.writeString(manifest, "{}", StandardCharsets.UTF_8);

        CapturedRun verify = runReport("--verify-manifest", manifest.toString());

        assertEquals(2, verify.result().exitCode());
        assertTrue(verify.output().isBlank());
        assertTrue(verify.error().contains("Remediation report export failed safely"));
        assertTrue(verify.error().contains("unsupported manifest version"));
        assertFalse(verify.error().contains("\tat "));
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
    void manifestGenerationAndVerificationDoNotConstructCloudManager() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path output = tempDir.resolve("incident-report.md");
        Path manifest = tempDir.resolve("incident-report.manifest.json");

        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            CapturedRun generate = runReport("--remediation-report", "--input", input.toString(),
                    "--output", output.toString(), "--manifest", manifest.toString());
            CapturedRun verify = runReport("--verify-manifest", manifest.toString());

            assertEquals(0, generate.result().exitCode());
            assertEquals(0, verify.result().exitCode());
            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Manifest generation and verification must not construct CloudManager.");
        }
    }

    @Test
    void reportGenerationAppendsChecksumChainedAuditEntry() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path output = tempDir.resolve("incident-report.md");
        Path auditLog = tempDir.resolve("offline-cli-audit.jsonl");

        CapturedRun run = runReport("--remediation-report", "--input", input.toString(),
                "--format", "markdown", "--output", output.toString(),
                "--audit-log", auditLog.toString(), "--audit-actor", "operator-a",
                "--audit-action-id", "ticket-123", "--audit-note", "initial report");

        assertEquals(0, run.result().exitCode());
        List<JsonNode> entries = auditEntries(auditLog);
        assertEquals(1, entries.size());
        JsonNode entry = entries.get(0);
        assertEquals(1, entry.path("sequence").asInt());
        assertEquals("MARKDOWN_REPORT_GENERATED", entry.path("action").asText());
        assertEquals("ticket-123", entry.path("actionId").asText());
        assertEquals("operator-a", entry.path("actor").asText());
        assertEquals(OfflineCliAuditLogService.ZERO_PREVIOUS_HASH, entry.path("previousEntryHash").asText());
        assertTrue(entry.path("entryHash").asText().matches("(?i)[0-9a-f]{64}"));
        assertFalse(entry.path("cloudMutation").asBoolean());

        CapturedRun verify = runReport("--verify-audit-log", auditLog.toString());
        assertEquals(0, verify.result().exitCode());
        assertTrue(verify.output().contains("Audit log verification passed"));
        assertTrue(verify.output().contains("entries=1"));
    }

    @Test
    void jsonReportGenerationAppendsAuditEntry() throws Exception {
        Path input = copyResource(REPLAY_FIXTURE, "replay.json");
        Path output = tempDir.resolve("incident-report.json");
        Path auditLog = tempDir.resolve("offline-cli-audit.jsonl");

        CapturedRun run = runReport("--remediation-report", "--input", input.toString(),
                "--format", "json", "--output", output.toString(), "--audit-log", auditLog.toString());

        assertEquals(0, run.result().exitCode());
        List<JsonNode> entries = auditEntries(auditLog);
        assertEquals(1, entries.size());
        assertEquals("JSON_REPORT_GENERATED", entries.get(0).path("action").asText());
        assertEquals("SUCCESS", entries.get(0).path("result").asText());
        assertAuditFile(entries.get(0), "REPORT", "incident-report.json", output);
    }

    @Test
    void manifestGenerationAndVerificationAppendAuditEntries() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path output = tempDir.resolve("incident-report.md");
        Path manifest = tempDir.resolve("incident-report.manifest.json");
        Path auditLog = tempDir.resolve("offline-cli-audit.jsonl");

        CapturedRun generate = runReport("--remediation-report", "--input", input.toString(),
                "--output", output.toString(), "--manifest", manifest.toString(),
                "--audit-log", auditLog.toString());
        CapturedRun verifyManifest = runReport("--verify-manifest", manifest.toString(),
                "--audit-log", auditLog.toString());

        assertEquals(0, generate.result().exitCode());
        assertEquals(0, verifyManifest.result().exitCode());
        List<JsonNode> entries = auditEntries(auditLog);
        assertEquals(3, entries.size());
        assertEquals("MARKDOWN_REPORT_GENERATED", entries.get(0).path("action").asText());
        assertEquals("MANIFEST_GENERATED", entries.get(1).path("action").asText());
        assertEquals("MANIFEST_VERIFIED", entries.get(2).path("action").asText());
        assertEquals(entries.get(0).path("entryHash").asText(), entries.get(1).path("previousEntryHash").asText());
        assertEquals(entries.get(1).path("entryHash").asText(), entries.get(2).path("previousEntryHash").asText());

        CapturedRun verifyAudit = runReport("--verify-audit-log", auditLog.toString());
        assertEquals(0, verifyAudit.result().exitCode());
    }

    @Test
    void bundleGenerationAndVerificationAppendAuditEntriesIncludingRedactionState() throws Exception {
        Path input = copyResource(REPLAY_FIXTURE, "replay.json");
        Path bundle = tempDir.resolve("incident-bundle.zip");
        Path auditLog = tempDir.resolve("offline-cli-audit.jsonl");

        CapturedRun generate = runReport("--input", input.toString(), "--format=json",
                "--bundle", bundle.toString(), "--redact", "green",
                "--audit-log", auditLog.toString());
        CapturedRun verifyBundle = runReport("--verify-bundle", bundle.toString(),
                "--audit-log", auditLog.toString());

        assertEquals(0, generate.result().exitCode());
        assertEquals(0, verifyBundle.result().exitCode());
        List<JsonNode> entries = auditEntries(auditLog);
        assertEquals(2, entries.size());
        assertEquals("BUNDLE_GENERATED", entries.get(0).path("action").asText());
        assertTrue(entries.get(0).path("redactionApplied").asBoolean());
        assertEquals("BUNDLE_VERIFIED", entries.get(1).path("action").asText());
        assertFalse(entries.get(1).path("redactionApplied").asBoolean());

        CapturedRun verifyAudit = runReport("--verify-audit-log", auditLog.toString());
        assertEquals(0, verifyAudit.result().exitCode());
    }

    @Test
    void auditLogVerificationDetectsTamperedEntry() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path output = tempDir.resolve("incident-report.md");
        Path auditLog = tempDir.resolve("offline-cli-audit.jsonl");
        runReport("--remediation-report", "--input", input.toString(),
                "--output", output.toString(), "--audit-log", auditLog.toString());

        String tampered = Files.readString(auditLog).replace("MARKDOWN_REPORT_GENERATED", "JSON_REPORT_GENERATED");
        Files.writeString(auditLog, tampered, StandardCharsets.UTF_8);

        CapturedRun verify = runReport("--verify-audit-log", auditLog.toString());
        assertEquals(2, verify.result().exitCode());
        assertTrue(verify.output().contains("Audit log verification failed"));
        assertTrue(verify.output().contains("entryHash mismatch"));
    }

    @Test
    void auditLogVerificationDetectsDeletedOrReorderedEntries() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path output = tempDir.resolve("incident-report.md");
        Path manifest = tempDir.resolve("incident-report.manifest.json");
        Path auditLog = tempDir.resolve("offline-cli-audit.jsonl");
        runReport("--remediation-report", "--input", input.toString(),
                "--output", output.toString(), "--manifest", manifest.toString(),
                "--audit-log", auditLog.toString());
        runReport("--verify-manifest", manifest.toString(), "--audit-log", auditLog.toString());
        List<String> originalLines = Files.readAllLines(auditLog, StandardCharsets.UTF_8);

        Path deletedMiddle = tempDir.resolve("deleted-middle-audit.jsonl");
        Files.write(deletedMiddle, List.of(originalLines.get(0), originalLines.get(2)), StandardCharsets.UTF_8);
        CapturedRun deletedVerify = runReport("--verify-audit-log", deletedMiddle.toString());
        assertEquals(2, deletedVerify.result().exitCode());
        assertTrue(deletedVerify.output().contains("sequence expected 2"));
        assertTrue(deletedVerify.output().contains("previousEntryHash expected"));

        Path reordered = tempDir.resolve("reordered-audit.jsonl");
        Files.write(reordered, List.of(originalLines.get(1), originalLines.get(0), originalLines.get(2)),
                StandardCharsets.UTF_8);
        CapturedRun reorderedVerify = runReport("--verify-audit-log", reordered.toString());
        assertEquals(2, reorderedVerify.result().exitCode());
        assertTrue(reorderedVerify.output().contains("Audit log verification failed"));
    }

    @Test
    void auditLogVerificationDetectsBadPreviousHashAndMalformedLine() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path output = tempDir.resolve("incident-report.md");
        Path manifest = tempDir.resolve("incident-report.manifest.json");
        Path auditLog = tempDir.resolve("offline-cli-audit.jsonl");
        runReport("--remediation-report", "--input", input.toString(),
                "--output", output.toString(), "--manifest", manifest.toString(),
                "--audit-log", auditLog.toString());
        List<String> lines = Files.readAllLines(auditLog, StandardCharsets.UTF_8);

        Path badPrevious = tempDir.resolve("bad-previous-audit.jsonl");
        String replacementHash = "f".repeat(64);
        String changedPrevious = lines.get(1).replace(lines.get(0).substring(
                lines.get(0).indexOf("\"entryHash\":\"") + 13,
                lines.get(0).indexOf("\"", lines.get(0).indexOf("\"entryHash\":\"") + 13)), replacementHash);
        Files.write(badPrevious, List.of(lines.get(0), changedPrevious), StandardCharsets.UTF_8);
        CapturedRun badPreviousVerify = runReport("--verify-audit-log", badPrevious.toString());
        assertEquals(2, badPreviousVerify.result().exitCode());
        assertTrue(badPreviousVerify.output().contains("previousEntryHash expected"));

        Path malformed = tempDir.resolve("malformed-audit.jsonl");
        Files.writeString(malformed, "{\"schemaVersion\":\"1\"", StandardCharsets.UTF_8);
        CapturedRun malformedVerify = runReport("--verify-audit-log", malformed.toString());
        assertEquals(2, malformedVerify.result().exitCode());
        assertTrue(malformedVerify.output().contains("malformed"));
    }

    @Test
    void auditLoggingDoesNotConstructCloudManager() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path output = tempDir.resolve("incident-report.md");
        Path auditLog = tempDir.resolve("offline-cli-audit.jsonl");

        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            CapturedRun generate = runReport("--remediation-report", "--input", input.toString(),
                    "--output", output.toString(), "--audit-log", auditLog.toString());
            CapturedRun verify = runReport("--verify-audit-log", auditLog.toString());

            assertEquals(0, generate.result().exitCode());
            assertEquals(0, verify.result().exitCode());
            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Offline audit log generation and verification must not construct CloudManager.");
        }
    }

    @Test
    void manifestRequiresOutputFileForReportChecksum() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path manifest = tempDir.resolve("incident-report.manifest.json");

        CapturedRun run = runReport("--remediation-report", "--input", input.toString(),
                "--manifest", manifest.toString());

        assertEquals(2, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().contains("--manifest requires --output <path>"));
        assertFalse(Files.exists(manifest));
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
        assertTrue(RemediationReportCli.isRequested(new String[]{"--verify-manifest", "report.manifest.json"}));
        assertTrue(RemediationReportCli.isRequested(new String[]{"--input", "report.json", "--bundle", "bundle.zip"}));
        assertTrue(RemediationReportCli.isRequested(new String[]{"--verify-bundle", "bundle.zip"}));
        assertTrue(RemediationReportCli.isRequested(new String[]{"--verify-audit-log", "offline-cli-audit.jsonl"}));
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

    private List<JsonNode> auditEntries(Path auditLog) throws Exception {
        List<JsonNode> entries = new java.util.ArrayList<>();
        for (String line : Files.readAllLines(auditLog, StandardCharsets.UTF_8)) {
            entries.add(OBJECT_MAPPER.readTree(line));
        }
        return entries;
    }

    private void assertManifestFile(JsonNode manifest, String role, String path, Path actualFile) throws Exception {
        JsonNode matchingFile = null;
        for (JsonNode file : manifest.path("files")) {
            if (role.equals(file.path("role").asText())) {
                matchingFile = file;
                break;
            }
        }
        assertTrue(matchingFile != null, "Manifest should include role " + role);
        assertEquals(path, matchingFile.path("path").asText());
        assertEquals(ReportChecksumManifestService.sha256(actualFile), matchingFile.path("sha256").asText());
    }

    private void assertManifestFile(JsonNode manifest, String role, String path, byte[] actualContent) {
        JsonNode matchingFile = null;
        for (JsonNode file : manifest.path("files")) {
            if (role.equals(file.path("role").asText()) && path.equals(file.path("path").asText())) {
                matchingFile = file;
                break;
            }
        }
        assertTrue(matchingFile != null, "Manifest should include role " + role + " at " + path);
        assertEquals(ReportChecksumManifestService.sha256(actualContent), matchingFile.path("sha256").asText());
    }

    private void assertAuditFile(JsonNode auditEntry, String role, String path, Path actualFile) throws Exception {
        JsonNode matchingFile = null;
        for (JsonNode file : auditEntry.path("fileHashes")) {
            if (role.equals(file.path("role").asText()) && path.equals(file.path("path").asText())) {
                matchingFile = file;
                break;
            }
        }
        assertTrue(matchingFile != null, "Audit entry should include role " + role + " at " + path);
        assertEquals(ReportChecksumManifestService.sha256(actualFile), matchingFile.path("sha256").asText());
    }

    private Map<String, byte[]> zipEntries(Path zipPath) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                ByteArrayOutputStream content = new ByteArrayOutputStream();
                zip.transferTo(content);
                entries.put(entry.getName(), content.toByteArray());
                zip.closeEntry();
            }
        }
        return entries;
    }

    private void writeZip(Path zipPath, Map<String, byte[]> entries) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setTime(315532800000L);
                zip.putNextEntry(zipEntry);
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
    }

    private String text(Map<String, byte[]> entries, String name) {
        return new String(entries.get(name), StandardCharsets.UTF_8);
    }

    private record CapturedRun(RemediationReportCli.Result result, String output, String error) {
    }
}
