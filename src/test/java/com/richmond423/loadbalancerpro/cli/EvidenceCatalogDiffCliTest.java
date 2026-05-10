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
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class EvidenceCatalogDiffCliTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BASELINE_CATALOG = "evidence-catalogs/baseline.json";
    private static final String DRIFTED_CATALOG = "evidence-catalogs/drifted.json";

    @TempDir
    Path tempDir;

    @Test
    void identicalCatalogsHaveNoDrift() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), baseline.toString(),
                "--diff-format", "json");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.error().isBlank());
        JsonNode diff = OBJECT_MAPPER.readTree(run.output());
        assertEquals(0, diff.path("summary").path("addedCount").asInt());
        assertEquals(0, diff.path("summary").path("removedCount").asInt());
        assertEquals(0, diff.path("summary").path("changedCount").asInt());
        assertEquals(0, diff.path("summary").path("statusChangedCount").asInt());
        assertEquals(0, diff.path("summary").path("auditAnchorChangedCount").asInt());
        assertEquals(6, diff.path("summary").path("unchangedCount").asInt());
        assertEquals(0, diff.path("changes").size());
    }

    @Test
    void includeUnchangedShowsStableItems() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), baseline.toString(),
                "--diff-format", "json", "--include-unchanged");

        assertEquals(0, run.result().exitCode());
        JsonNode diff = OBJECT_MAPPER.readTree(run.output());
        assertEquals(6, diff.path("changes").size());
        assertEquals("UNCHANGED", change(diff, "incident-bundle.zip").path("changeType").asText());
    }

    @Test
    void mixedDriftSummaryDetectsAddedRemovedChangedStatusAndAuditAnchor() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path drifted = copyResource(DRIFTED_CATALOG, "drifted.json");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), drifted.toString(),
                "--diff-format", "json");

        assertEquals(0, run.result().exitCode());
        JsonNode diff = OBJECT_MAPPER.readTree(run.output());
        assertEquals(1, diff.path("summary").path("addedCount").asInt());
        assertEquals(1, diff.path("summary").path("removedCount").asInt());
        assertEquals(1, diff.path("summary").path("changedCount").asInt());
        assertEquals(1, diff.path("summary").path("statusChangedCount").asInt());
        assertEquals(1, diff.path("summary").path("auditAnchorChangedCount").asInt());
        assertEquals(2, diff.path("summary").path("unchangedCount").asInt());
        assertEquals(5, diff.path("summary").path("failureCount").asInt());
    }

    @Test
    void addedAndRemovedEvidenceAreReportedByPath() throws Exception {
        JsonNode diff = driftedDiff();

        JsonNode added = change(diff, "verification-summary.json");
        assertEquals("ADDED", added.path("changeType").asText());
        assertEquals("VERIFICATION_SUMMARY", added.path("afterType").asText());
        assertTrue(added.path("failures").toString().contains("added"));

        JsonNode removed = change(diff, "input.json");
        assertEquals("REMOVED", removed.path("changeType").asText());
        assertEquals("INPUT", removed.path("beforeType").asText());
        assertTrue(removed.path("failures").toString().contains("removed"));
    }

    @Test
    void changedChecksumIsReported() throws Exception {
        JsonNode report = change(driftedDiff(), "report.md");

        assertEquals("CHANGED", report.path("changeType").asText());
        assertTrue(report.path("checksumChanged").asBoolean());
        assertEquals("7777777777777777777777777777777777777777777777777777777777777777",
                report.path("beforeSha256").asText());
        assertEquals("9999999999999999999999999999999999999999999999999999999999999999",
                report.path("afterSha256").asText());
    }

    @Test
    void verificationStatusDriftIsReported() throws Exception {
        JsonNode manifest = change(driftedDiff(), "manifest.json");

        assertEquals("STATUS_CHANGED", manifest.path("changeType").asText());
        assertTrue(manifest.path("statusChanged").asBoolean());
        assertEquals("VALID", manifest.path("beforeVerificationStatus").asText());
        assertEquals("FAILED", manifest.path("afterVerificationStatus").asText());
    }

    @Test
    void auditAnchorDriftIsReported() throws Exception {
        JsonNode audit = change(driftedDiff(), "offline-cli-audit.jsonl");

        assertEquals("AUDIT_ANCHOR_CHANGED", audit.path("changeType").asText());
        assertTrue(audit.path("auditAnchorChanged").asBoolean());
        assertEquals(2, audit.path("beforeAuditEntryCount").asInt());
        assertEquals(3, audit.path("afterAuditEntryCount").asInt());
        assertTrue(audit.path("afterLatestAuditEntryHash").asText().endsWith("ef"));
    }

    @Test
    void markdownAndJsonOutputAreDeterministic() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path drifted = copyResource(DRIFTED_CATALOG, "drifted.json");

        CapturedRun firstJson = runCli("--diff-inventory", baseline.toString(), drifted.toString(),
                "--diff-format", "json");
        CapturedRun secondJson = runCli("--diff-inventory", baseline.toString(), drifted.toString(),
                "--diff-format", "json");
        CapturedRun firstMarkdown = runCli("--diff-inventory", baseline.toString(), drifted.toString(),
                "--diff-format", "markdown");
        CapturedRun secondMarkdown = runCli("--diff-inventory", baseline.toString(), drifted.toString(),
                "--diff-format", "markdown");

        assertEquals(firstJson.output(), secondJson.output());
        assertEquals(firstMarkdown.output(), secondMarkdown.output());
        assertTrue(firstMarkdown.output().contains("# LoadBalancerPro Evidence Catalog Diff"));
        assertTrue(firstMarkdown.output().contains("AUDIT_ANCHOR_CHANGED"));
    }

    @Test
    void diffCanWriteOutputFile() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path drifted = copyResource(DRIFTED_CATALOG, "drifted.json");
        Path output = tempDir.resolve("handoff-delta.md");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), drifted.toString(),
                "--diff-output", output.toString());

        assertEquals(0, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(Files.readString(output).contains("Evidence Catalog Diff"));
    }

    @Test
    void failOnDriftReturnsControlledNonZero() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path drifted = copyResource(DRIFTED_CATALOG, "drifted.json");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), drifted.toString(),
                "--diff-format", "json", "--fail-on-drift");

        assertEquals(2, run.result().exitCode());
        assertTrue(run.error().isBlank());
        assertEquals(5, OBJECT_MAPPER.readTree(run.output()).path("summary").path("failureCount").asInt());
    }

    @Test
    void malformedCatalogFailsSafely() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path malformed = tempDir.resolve("malformed.json");
        Files.writeString(malformed, "{ not-json", StandardCharsets.UTF_8);

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), malformed.toString(),
                "--diff-format", "json");

        assertEquals(2, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().contains("failed to read evidence catalog"));
    }

    @Test
    void diffDoesNotConstructCloudManager() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path drifted = copyResource(DRIFTED_CATALOG, "drifted.json");

        try (MockedConstruction<CloudManager> mocked = Mockito.mockConstruction(CloudManager.class)) {
            CapturedRun run = runCli("--diff-inventory", baseline.toString(), drifted.toString(),
                    "--diff-format", "markdown");

            assertEquals(0, run.result().exitCode());
            assertTrue(mocked.constructed().isEmpty());
            assertTrue(run.output().contains("Local inventory diff only"));
        }
    }

    @Test
    void diffModeIsRecognizedWithoutStartingInteractiveCli() {
        assertTrue(RemediationReportCli.isRequested(
                new String[]{"--diff-inventory", "before.json", "after.json"}));
        assertTrue(RemediationReportCli.isRequested(
                new String[]{"--diff-inventory=before.json,after.json"}));
    }

    private JsonNode driftedDiff() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path drifted = copyResource(DRIFTED_CATALOG, "drifted.json");
        CapturedRun run = runCli("--diff-inventory", baseline.toString(), drifted.toString(),
                "--diff-format", "json");
        assertEquals(0, run.result().exitCode());
        return OBJECT_MAPPER.readTree(run.output());
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

    private JsonNode change(JsonNode diff, String path) {
        for (JsonNode change : diff.path("changes")) {
            if (path.equals(change.path("path").asText())) {
                return change;
            }
        }
        throw new AssertionError("diff should include " + path);
    }

    private record CapturedRun(RemediationReportCli.Result result, String output, String error) {
    }
}
