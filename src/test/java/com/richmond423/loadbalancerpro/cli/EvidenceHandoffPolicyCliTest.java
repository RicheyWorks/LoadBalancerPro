package com.richmond423.loadbalancerpro.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

class EvidenceHandoffPolicyCliTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BASELINE_CATALOG = "evidence-catalogs/baseline.json";
    private static final String STRICT_POLICY = "evidence-policies/strict-zero-drift.json";
    private static final String ALLOW_REDACTION_POLICY = "evidence-policies/allow-added-redaction-summary.json";
    private static final String WARN_AUDIT_POLICY = "evidence-policies/warn-audit-anchor-drift.json";
    private static final String IGNORE_README_POLICY = "evidence-policies/ignore-expected-readme-change.json";
    private static final String FAIL_REMOVED_BUNDLE_POLICY = "evidence-policies/fail-removed-bundle.json";

    @TempDir
    Path tempDir;

    @Test
    void strictNoDriftPolicyPassesIdenticalCatalogs() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path policy = copyResource(STRICT_POLICY, "strict-zero-drift.json");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), baseline.toString(),
                "--policy", policy.toString(), "--policy-report-format", "json");

        assertEquals(0, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("PASS", report.path("decision").asText());
        assertEquals(0, report.path("summary").path("failCount").asInt());
        assertEquals(0, report.path("evaluatedChanges").size());
    }

    @Test
    void strictPolicyFailsAnyUnallowedDrift() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path drifted = copyResource("evidence-catalogs/drifted.json", "drifted.json");
        Path policy = copyResource(STRICT_POLICY, "strict-zero-drift.json");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), drifted.toString(),
                "--policy", policy.toString(), "--policy-report-format", "json",
                "--fail-on-policy-fail");

        assertEquals(2, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("FAIL", report.path("decision").asText());
        assertEquals(5, report.path("summary").path("failCount").asInt());
        assertEquals(5, report.path("summary").path("unclassifiedCount").asInt());
    }

    @Test
    void allowlistPermitsExpectedAddedFile() throws Exception {
        Path before = catalogWithout("redaction-summary.json", "before-without-redaction.json");
        Path after = copyResource(BASELINE_CATALOG, "after-with-redaction.json");
        Path policy = copyResource(ALLOW_REDACTION_POLICY, "allow-redaction.json");

        CapturedRun run = runCli("--diff-inventory", before.toString(), after.toString(),
                "--policy", policy.toString(), "--policy-report-format", "json");

        assertEquals(0, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("PASS", report.path("decision").asText());
        assertEquals(1, report.path("summary").path("infoCount").asInt());
        assertEquals("INFO", report.path("evaluatedChanges").get(0).path("severity").asText());
    }

    @Test
    void auditAnchorDriftCanBeWarning() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path after = catalogWithAuditAnchorDrift("audit-anchor-drift.json");
        Path policy = copyResource(WARN_AUDIT_POLICY, "warn-audit.json");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), after.toString(),
                "--policy", policy.toString(), "--policy-report-format", "json",
                "--fail-on-policy-fail");

        assertEquals(0, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("WARN", report.path("decision").asText());
        assertEquals(1, report.path("summary").path("warnCount").asInt());
        assertEquals("AUDIT_ANCHOR_CHANGED",
                report.path("evaluatedChanges").get(0).path("changeType").asText());
    }

    @Test
    void removedBundleCanBeFail() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path after = catalogWithout("incident-bundle.zip", "missing-bundle.json");
        Path policy = copyResource(FAIL_REMOVED_BUNDLE_POLICY, "fail-removed-bundle.json");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), after.toString(),
                "--policy", policy.toString(), "--policy-report-format", "json");

        assertEquals(0, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("FAIL", report.path("decision").asText());
        assertEquals(1, report.path("summary").path("failCount").asInt());
        assertEquals("REMOVED", report.path("evaluatedChanges").get(0).path("changeType").asText());
    }

    @Test
    void ignoredPathDoesNotAffectDecision() throws Exception {
        CatalogPair catalogs = catalogsWithReadmeChecksumDrift();
        Path policy = copyResource(IGNORE_README_POLICY, "ignore-readme.json");

        CapturedRun run = runCli("--diff-inventory", catalogs.before().toString(), catalogs.after().toString(),
                "--policy", policy.toString(), "--policy-report-format", "json");

        assertEquals(0, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("PASS", report.path("decision").asText());
        assertEquals(1, report.path("summary").path("ignoredCount").asInt());
        assertEquals("IGNORE", report.path("evaluatedChanges").get(0).path("severity").asText());
    }

    @Test
    void unmatchedChangeUsesDefaultSeverity() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path after = catalogWithReportChecksumDrift("report-drift.json");
        Path policy = writePolicy("default-warn-policy.json", """
                {
                  "policyVersion" : "1",
                  "mode" : "ALLOWLIST",
                  "defaultSeverity" : "WARN",
                  "rules" : [ ]
                }
                """);

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), after.toString(),
                "--policy", policy.toString(), "--policy-report-format", "json");

        assertEquals(0, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("WARN", report.path("decision").asText());
        assertEquals(1, report.path("summary").path("warnCount").asInt());
        assertEquals(1, report.path("summary").path("unclassifiedCount").asInt());
    }

    @Test
    void jsonAndMarkdownPolicyReportsAreDeterministic() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path after = catalogWithAuditAnchorDrift("audit-anchor-drift.json");
        Path policy = copyResource(WARN_AUDIT_POLICY, "warn-audit.json");

        CapturedRun firstJson = runCli("--diff-inventory", baseline.toString(), after.toString(),
                "--policy", policy.toString(), "--policy-report-format", "json");
        CapturedRun secondJson = runCli("--diff-inventory", baseline.toString(), after.toString(),
                "--policy", policy.toString(), "--policy-report-format", "json");
        CapturedRun firstMarkdown = runCli("--diff-inventory", baseline.toString(), after.toString(),
                "--policy", policy.toString(), "--policy-report-format", "markdown");
        CapturedRun secondMarkdown = runCli("--diff-inventory", baseline.toString(), after.toString(),
                "--policy", policy.toString(), "--policy-report-format", "markdown");

        assertEquals(firstJson.output(), secondJson.output());
        assertEquals(firstMarkdown.output(), secondMarkdown.output());
        assertTrue(firstMarkdown.output().contains("# LoadBalancerPro Evidence Handoff Policy Report"));
        assertTrue(firstMarkdown.output().contains("Decision: WARN"));
    }

    @Test
    void policyReportCanWriteOutputFile() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path after = catalogWithReportChecksumDrift("report-drift.json");
        Path policy = writePolicy("default-info-policy.json", """
                {
                  "policyVersion" : "1",
                  "mode" : "ALLOWLIST",
                  "defaultSeverity" : "INFO",
                  "rules" : [ ]
                }
                """);
        Path output = tempDir.resolve("policy-report.md");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), after.toString(),
                "--policy", policy.toString(), "--policy-output", output.toString());

        assertEquals(0, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(Files.readString(output).contains("Evidence Handoff Policy Report"));
    }

    @Test
    void malformedPolicyReturnsControlledError() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path malformed = writePolicy("malformed-policy.json", "{ not-json");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), baseline.toString(),
                "--policy", malformed.toString(), "--policy-report-format", "json");

        assertEquals(2, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().contains("failed to read evidence handoff policy"));
    }

    @Test
    void policyEvaluationDoesNotConstructCloudManager() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path after = catalogWithReportChecksumDrift("report-drift.json");
        Path policy = writePolicy("default-info-policy.json", """
                {
                  "policyVersion" : "1",
                  "mode" : "ALLOWLIST",
                  "defaultSeverity" : "INFO",
                  "rules" : [ ]
                }
                """);

        try (MockedConstruction<CloudManager> mocked = Mockito.mockConstruction(CloudManager.class)) {
            CapturedRun run = runCli("--diff-inventory", baseline.toString(), after.toString(),
                    "--policy", policy.toString(), "--policy-report-format", "markdown");

            assertEquals(0, run.result().exitCode());
            assertTrue(mocked.constructed().isEmpty());
            assertTrue(run.output().contains("Local checksum policy evaluation only"));
        }
    }

    private Path catalogWithout(String path, String fileName) throws Exception {
        ObjectNode catalog = baselineCatalog();
        ArrayNode items = (ArrayNode) catalog.path("items");
        for (int index = items.size() - 1; index >= 0; index--) {
            if (path.equals(items.get(index).path("path").asText())) {
                items.remove(index);
            }
        }
        return writeCatalog(fileName, catalog);
    }

    private Path catalogWithAuditAnchorDrift(String fileName) throws Exception {
        ObjectNode catalog = baselineCatalog();
        ObjectNode audit = item(catalog, "offline-cli-audit.jsonl");
        audit.put("latestAuditEntryHash", "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeef");
        audit.put("entryCount", 3);
        return writeCatalog(fileName, catalog);
    }

    private Path catalogWithReportChecksumDrift(String fileName) throws Exception {
        ObjectNode catalog = baselineCatalog();
        item(catalog, "report.md").put("sha256",
                "9999999999999999999999999999999999999999999999999999999999999999");
        return writeCatalog(fileName, catalog);
    }

    private CatalogPair catalogsWithReadmeChecksumDrift() throws Exception {
        ObjectNode before = baselineCatalog();
        ObjectNode after = baselineCatalog();
        addItem(before, "README.md", "REPORT",
                "1111111111111111111111111111111111111111111111111111111111111111");
        addItem(after, "README.md", "REPORT",
                "2222222222222222222222222222222222222222222222222222222222222222");
        return new CatalogPair(writeCatalog("readme-before.json", before), writeCatalog("readme-after.json", after));
    }

    private ObjectNode baselineCatalog() throws Exception {
        ClassPathResource resource = new ClassPathResource(BASELINE_CATALOG);
        try (InputStream input = resource.getInputStream()) {
            return (ObjectNode) OBJECT_MAPPER.readTree(input);
        }
    }

    private ObjectNode item(ObjectNode catalog, String path) {
        for (JsonNode item : catalog.path("items")) {
            if (path.equals(item.path("path").asText())) {
                return (ObjectNode) item;
            }
        }
        throw new AssertionError("catalog should include " + path);
    }

    private void addItem(ObjectNode catalog, String path, String type, String sha256) {
        ObjectNode item = OBJECT_MAPPER.createObjectNode();
        item.put("path", path);
        item.put("type", type);
        item.put("sha256", sha256);
        item.put("verificationStatus", "NOT_VERIFIED");
        item.putArray("warnings");
        item.putArray("failures");
        ((ArrayNode) catalog.path("items")).add(item);
    }

    private Path writeCatalog(String fileName, JsonNode catalog) throws Exception {
        Path target = tempDir.resolve(fileName);
        Files.writeString(target, OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(catalog),
                StandardCharsets.UTF_8);
        return target;
    }

    private Path writePolicy(String fileName, String policy) throws Exception {
        Path target = tempDir.resolve(fileName);
        Files.writeString(target, policy, StandardCharsets.UTF_8);
        return target;
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

    private record CatalogPair(Path before, Path after) {
    }

    private record CapturedRun(RemediationReportCli.Result result, String output, String error) {
    }
}
