package com.richmond423.loadbalancerpro.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

class EvidenceInventoryCliTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String EVALUATION_FIXTURE =
            "remediation-reports/evaluation-overload-response.json";
    private static final String REPLAY_FIXTURE =
            "remediation-reports/replay-mixed-incident-response.json";

    @TempDir
    Path tempDir;

    @Test
    void inventoryDetectsAndVerifiesValidBundle() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path bundle = tempDir.resolve("incident-bundle.zip");
        assertEquals(0, runCli("--input", input.toString(), "--bundle", bundle.toString(),
                "--report-id", "inventory-bundle").result().exitCode());

        CapturedRun run = runCli("--inventory", tempDir.toString(), "--inventory-format", "json",
                "--verify-inventory", "--include-hashes");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.error().isBlank());
        JsonNode catalog = OBJECT_MAPPER.readTree(run.output());
        assertEquals(1, catalog.path("summary").path("bundleCount").asInt());
        JsonNode bundleItem = item(catalog, "incident-bundle.zip");
        assertEquals("BUNDLE", bundleItem.path("type").asText());
        assertEquals("VALID", bundleItem.path("verificationStatus").asText());
        assertEquals(64, bundleItem.path("sha256").asText().length());
    }

    @Test
    void inventoryDetectsManifestReportAndInputFiles() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path report = tempDir.resolve("incident-report.md");
        Path manifest = tempDir.resolve("incident-report.manifest.json");
        assertEquals(0, runCli("--input", input.toString(), "--output", report.toString(),
                "--manifest", manifest.toString(), "--report-id", "inventory-manifest").result().exitCode());

        CapturedRun run = runCli("--inventory", tempDir.toString(), "--inventory-format", "json",
                "--verify-inventory", "--include-hashes");

        assertEquals(0, run.result().exitCode());
        JsonNode catalog = OBJECT_MAPPER.readTree(run.output());
        assertEquals(1, catalog.path("summary").path("manifestCount").asInt());
        assertEquals(1, catalog.path("summary").path("reportCount").asInt());
        assertEquals("VALID", item(catalog, "incident-report.manifest.json")
                .path("verificationStatus").asText());
        assertEquals("inventory-manifest", item(catalog, "incident-report.manifest.json")
                .path("reportId").asText());
        assertEquals("INPUT", item(catalog, "evaluation.json").path("type").asText());
        assertEquals("REPORT", item(catalog, "incident-report.md").path("type").asText());
    }

    @Test
    void inventoryDetectsAuditLogAndLatestAnchor() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path report = tempDir.resolve("incident-report.md");
        Path auditLog = tempDir.resolve("offline-cli-audit.jsonl");
        assertEquals(0, runCli("--input", input.toString(), "--output", report.toString(),
                "--audit-log", auditLog.toString(), "--audit-action-id", "audit-inventory").result().exitCode());

        CapturedRun run = runCli("--inventory", tempDir.toString(), "--inventory-format", "json",
                "--verify-inventory");

        assertEquals(0, run.result().exitCode());
        JsonNode auditItem = item(OBJECT_MAPPER.readTree(run.output()), "offline-cli-audit.jsonl");
        assertEquals("AUDIT_LOG", auditItem.path("type").asText());
        assertEquals("VALID", auditItem.path("verificationStatus").asText());
        assertEquals(1, auditItem.path("entryCount").asInt());
        assertTrue(auditItem.path("latestAuditEntryHash").asText().matches("(?i)[0-9a-f]{64}"));
    }

    @Test
    void inventoryDetectsRedactionSummaryWithoutLeakingOriginals() throws Exception {
        Path input = copyResource(REPLAY_FIXTURE, "replay.json");
        Path report = tempDir.resolve("incident-report.md");
        Path summary = tempDir.resolve("incident-report.redaction-summary.json");
        assertEquals(0, runCli("--input", input.toString(), "--output", report.toString(),
                "--redact", "mixed-incident-replay", "--redact", "green",
                "--redact-output-summary", summary.toString()).result().exitCode());

        CapturedRun run = runCli("--inventory", tempDir.toString(), "--inventory-format", "json",
                "--verify-inventory");

        assertEquals(0, run.result().exitCode());
        JsonNode catalog = OBJECT_MAPPER.readTree(run.output());
        assertEquals(1, catalog.path("summary").path("redactionSummaryCount").asInt());
        assertEquals("REDACTION_SUMMARY", item(catalog, "incident-report.redaction-summary.json")
                .path("type").asText());
        assertFalse(run.output().contains("mixed-incident-replay"));
        assertFalse(run.output().contains("green"));
    }

    @Test
    void markdownInventoryOutputIsDeterministicAndCanBeWrittenToFile() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path report = tempDir.resolve("incident-report.md");
        assertEquals(0, runCli("--input", input.toString(), "--output", report.toString()).result().exitCode());
        Path firstCatalog = tempDir.getParent().resolve("first-inventory.md");
        Path secondCatalog = tempDir.getParent().resolve("second-inventory.md");

        CapturedRun first = runCli("--inventory", tempDir.toString(), "--inventory-output", firstCatalog.toString());
        CapturedRun second = runCli("--inventory", tempDir.toString(), "--inventory-output", secondCatalog.toString());

        assertEquals(0, first.result().exitCode());
        assertEquals(0, second.result().exitCode());
        assertTrue(first.output().isBlank());
        assertEquals(Files.readString(firstCatalog), Files.readString(secondCatalog));
        assertTrue(Files.readString(firstCatalog).contains("# LoadBalancerPro Evidence Inventory"));
    }

    @Test
    void verificationCatchesTamperedBundleAndManifestWithFailOnInvalid() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path bundle = tempDir.resolve("incident-bundle.zip");
        Path report = tempDir.resolve("incident-report.md");
        Path manifest = tempDir.resolve("incident-report.manifest.json");
        assertEquals(0, runCli("--input", input.toString(), "--bundle", bundle.toString()).result().exitCode());
        assertEquals(0, runCli("--input", input.toString(), "--output", report.toString(),
                "--manifest", manifest.toString()).result().exitCode());

        Map<String, byte[]> entries = zipEntries(bundle);
        entries.put("report.md", "tampered report".getBytes(StandardCharsets.UTF_8));
        writeZip(bundle, entries);
        Files.writeString(report, "tampered report", StandardCharsets.UTF_8);

        CapturedRun run = runCli("--inventory", tempDir.toString(), "--inventory-format", "json",
                "--verify-inventory", "--fail-on-invalid");

        assertEquals(2, run.result().exitCode());
        JsonNode catalog = OBJECT_MAPPER.readTree(run.output());
        assertTrue(catalog.path("summary").path("failureCount").asInt() >= 2);
        assertEquals("FAILED", item(catalog, "incident-bundle.zip").path("verificationStatus").asText());
        assertEquals("FAILED", item(catalog, "incident-report.manifest.json").path("verificationStatus").asText());
    }

    @Test
    void verificationCatchesTamperedAuditLog() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path report = tempDir.resolve("incident-report.md");
        Path auditLog = tempDir.resolve("offline-cli-audit.jsonl");
        assertEquals(0, runCli("--input", input.toString(), "--output", report.toString(),
                "--audit-log", auditLog.toString()).result().exitCode());
        String tampered = Files.readString(auditLog).replace("MARKDOWN_REPORT_GENERATED", "JSON_REPORT_GENERATED");
        Files.writeString(auditLog, tampered, StandardCharsets.UTF_8);

        CapturedRun run = runCli("--inventory", tempDir.toString(), "--inventory-format", "json",
                "--verify-inventory", "--fail-on-invalid");

        assertEquals(2, run.result().exitCode());
        JsonNode auditItem = item(OBJECT_MAPPER.readTree(run.output()), "offline-cli-audit.jsonl");
        assertEquals("FAILED", auditItem.path("verificationStatus").asText());
        assertTrue(auditItem.path("failures").toString().contains("entryHash mismatch"));
    }

    @Test
    void malformedFilesProduceControlledInventoryFailures() throws Exception {
        Files.writeString(tempDir.resolve("broken.manifest.json"), "{ not-json", StandardCharsets.UTF_8);

        CapturedRun run = runCli("--inventory", tempDir.toString(), "--inventory-format", "json",
                "--verify-inventory", "--fail-on-invalid");

        assertEquals(2, run.result().exitCode());
        assertTrue(run.error().isBlank());
        JsonNode item = item(OBJECT_MAPPER.readTree(run.output()), "broken.manifest.json");
        assertEquals("FAILED", item.path("verificationStatus").asText());
        assertTrue(item.path("failures").toString().contains("manifest verification failed safely"));
    }

    @Test
    void inventoryDoesNotConstructCloudManager() throws Exception {
        Path input = copyResource(EVALUATION_FIXTURE, "evaluation.json");
        Path bundle = tempDir.resolve("incident-bundle.zip");
        assertEquals(0, runCli("--input", input.toString(), "--bundle", bundle.toString()).result().exitCode());

        try (MockedConstruction<CloudManager> mocked = Mockito.mockConstruction(CloudManager.class)) {
            CapturedRun run = runCli("--inventory", tempDir.toString(), "--verify-inventory");

            assertEquals(0, run.result().exitCode());
            assertTrue(mocked.constructed().isEmpty());
            assertTrue(run.output().contains("Local checksum inventory only"));
        }
    }

    @Test
    void inventoryModeIsRecognizedWithoutStartingInteractiveCli() {
        assertTrue(RemediationReportCli.isRequested(new String[]{"--inventory", "evidence"}));
        assertTrue(RemediationReportCli.isRequested(new String[]{"--inventory=evidence"}));
    }

    private Path copyResource(String resourcePath, String fileName) throws Exception {
        Path target = tempDir.resolve(fileName);
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream input = resource.getInputStream()) {
            Files.write(target, input.readAllBytes());
        }
        return target;
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

    private JsonNode item(JsonNode catalog, String path) {
        for (JsonNode item : catalog.path("items")) {
            if (path.equals(item.path("path").asText())) {
                return item;
            }
        }
        throw new AssertionError("catalog should include " + path);
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

    private record CapturedRun(RemediationReportCli.Result result, String output, String error) {
    }
}
