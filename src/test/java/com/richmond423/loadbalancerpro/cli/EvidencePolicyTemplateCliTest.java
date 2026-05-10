package com.richmond423.loadbalancerpro.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.List;

class EvidencePolicyTemplateCliTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BASELINE_CATALOG = "evidence-catalogs/baseline.json";

    @TempDir
    Path tempDir;

    @Test
    void allTemplatesParseAndUseSupportedSchema() throws Exception {
        EvidencePolicyTemplateService templateService = new EvidencePolicyTemplateService();
        EvidenceHandoffPolicyService policyService = new EvidenceHandoffPolicyService();

        List<EvidencePolicyTemplateService.PolicyTemplate> templates = templateService.listTemplates();

        assertEquals(List.of(
                "audit-append",
                "investigation-working-copy",
                "receiver-redaction",
                "regulated-handoff",
                "strict-zero-drift"),
                templates.stream().map(EvidencePolicyTemplateService.PolicyTemplate::name).toList());
        for (EvidencePolicyTemplateService.PolicyTemplate template : templates) {
            EvidenceHandoffPolicyService.HandoffPolicy policy = policyService.readPolicyJson(
                    "template:" + template.name(),
                    templateService.templateJson(template.name()));
            assertEquals("1", policy.policyVersion());
            assertFalse(policy.rules().stream()
                    .anyMatch(rule -> rule.changeType() == EvidenceCatalogDiffService.ChangeType.UNCHANGED));
        }
    }

    @Test
    void strictTemplateFailsDrift() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path drifted = copyResource("evidence-catalogs/drifted.json", "drifted.json");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), drifted.toString(),
                "--policy-template", "strict-zero-drift", "--policy-report-format", "json",
                "--fail-on-policy-fail");

        assertEquals(2, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("FAIL", report.path("decision").asText());
        assertEquals(5, report.path("summary").path("failCount").asInt());
    }

    @Test
    void receiverRedactionTemplateAllowsExpectedRedactionSummary() throws Exception {
        Path before = catalogWithout("redaction-summary.json", "before-without-redaction.json");
        Path after = copyResource(BASELINE_CATALOG, "after-with-redaction.json");

        CapturedRun run = runCli("--diff-inventory", before.toString(), after.toString(),
                "--policy-template", "receiver-redaction", "--policy-report-format", "json");

        assertEquals(0, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("PASS", report.path("decision").asText());
        assertEquals(1, report.path("summary").path("infoCount").asInt());
        assertEquals("INFO", report.path("evaluatedChanges").get(0).path("severity").asText());
    }

    @Test
    void auditAppendTemplateWarnsForAuditAnchorDrift() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path after = catalogWithAuditAnchorDrift("audit-anchor-drift.json");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), after.toString(),
                "--policy-template", "audit-append", "--policy-report-format", "json",
                "--fail-on-policy-fail");

        assertEquals(0, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("WARN", report.path("decision").asText());
        assertEquals(1, report.path("summary").path("warnCount").asInt());
        assertEquals("AUDIT_ANCHOR_CHANGED",
                report.path("evaluatedChanges").get(0).path("changeType").asText());
    }

    @Test
    void regulatedTemplateFailsRemovedBundle() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path after = catalogWithout("incident-bundle.zip", "missing-bundle.json");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), after.toString(),
                "--policy-template", "regulated-handoff", "--policy-report-format", "json");

        assertEquals(0, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("FAIL", report.path("decision").asText());
        assertEquals(1, report.path("summary").path("failCount").asInt());
        assertEquals("REMOVED", report.path("evaluatedChanges").get(0).path("changeType").asText());
    }

    @Test
    void investigationWorkingCopyTemplateWarnsForReportChanges() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path after = catalogWithReportChecksumDrift("report-drift.json");

        CapturedRun run = runCli("--diff-inventory", baseline.toString(), after.toString(),
                "--policy-template", "investigation-working-copy", "--policy-report-format", "json");

        assertEquals(0, run.result().exitCode());
        JsonNode report = OBJECT_MAPPER.readTree(run.output());
        assertEquals("WARN", report.path("decision").asText());
        assertEquals(1, report.path("summary").path("warnCount").asInt());
        assertEquals("report.md", report.path("evaluatedChanges").get(0).path("path").asText());
    }

    @Test
    void listExportAndValidatePolicyTemplatesAreDeterministic() throws Exception {
        CapturedRun listRun = runCli("--list-policy-templates");
        Path exported = tempDir.resolve("regulated-handoff.json");

        CapturedRun exportRun = runCli("--export-policy-template", "regulated-handoff",
                "--policy-output", exported.toString());
        CapturedRun validateRun = runCli("--validate-policy", exported.toString());

        assertEquals(0, listRun.result().exitCode());
        assertTrue(listRun.output().contains("- strict-zero-drift:"));
        assertTrue(listRun.output().contains("- receiver-redaction:"));
        assertTrue(listRun.output().contains("- audit-append:"));
        assertTrue(listRun.output().contains("- regulated-handoff:"));
        assertTrue(listRun.output().contains("- investigation-working-copy:"));
        assertEquals(0, exportRun.result().exitCode());
        assertTrue(exportRun.output().isBlank());
        assertEquals(new EvidencePolicyTemplateService().templateJson("regulated-handoff"),
                Files.readString(exported));
        assertEquals(0, validateRun.result().exitCode());
        assertTrue(validateRun.output().contains("Policy validation passed"));
        assertTrue(validateRun.output().contains("mode=ALLOWLIST"));
    }

    @Test
    void malformedPolicyValidationReturnsControlledError() throws Exception {
        Path malformed = tempDir.resolve("malformed-policy.json");
        Files.writeString(malformed, "{ not-json", StandardCharsets.UTF_8);

        CapturedRun run = runCli("--validate-policy", malformed.toString());

        assertEquals(2, run.result().exitCode());
        assertTrue(run.output().isBlank());
        assertTrue(run.error().contains("failed to read evidence handoff policy"));
    }

    @Test
    void templateDiffDoesNotConstructCloudManager() throws Exception {
        Path baseline = copyResource(BASELINE_CATALOG, "baseline.json");
        Path after = catalogWithReportChecksumDrift("report-drift.json");

        try (MockedConstruction<CloudManager> mocked = Mockito.mockConstruction(CloudManager.class)) {
            CapturedRun run = runCli("--diff-inventory", baseline.toString(), after.toString(),
                    "--policy-template", "investigation-working-copy",
                    "--policy-report-format", "markdown");

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

    private Path writeCatalog(String fileName, JsonNode catalog) throws Exception {
        Path target = tempDir.resolve(fileName);
        Files.writeString(target, OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(catalog),
                StandardCharsets.UTF_8);
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

    private record CapturedRun(RemediationReportCli.Result result, String output, String error) {
    }
}
