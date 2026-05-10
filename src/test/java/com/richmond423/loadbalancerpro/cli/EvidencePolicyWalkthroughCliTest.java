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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class EvidencePolicyWalkthroughCliTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<String, String> EXPECTED_DECISIONS = expectedDecisions();

    @TempDir
    Path tempDir;

    @Test
    void listPolicyExamplesIsDeterministic() {
        CapturedRun first = runCli("--list-policy-examples");
        CapturedRun second = runCli("--list-policy-examples");

        assertEquals(0, first.result().exitCode());
        assertEquals(first.output(), second.output());
        assertTrue(first.error().isBlank());
        assertTrue(first.output().startsWith("Available evidence policy examples:"));
        for (String exampleName : EXPECTED_DECISIONS.keySet()) {
            assertTrue(first.output().contains("- " + exampleName + ":"),
                    "list should include " + exampleName);
        }
    }

    @Test
    void exportsEachPackagedExampleDeterministically() throws Exception {
        EvidencePolicyExampleService service = new EvidencePolicyExampleService();

        for (String exampleName : EXPECTED_DECISIONS.keySet()) {
            Path outputDirectory = tempDir.resolve(exampleName);
            CapturedRun run = runCli("--export-policy-example", exampleName,
                    "--example-output-dir", outputDirectory.toString());

            assertEquals(0, run.result().exitCode(), exampleName);
            assertTrue(run.error().isBlank(), exampleName);
            assertTrue(run.output().contains("Evidence policy example exported: " + exampleName));
            assertEquals(service.exampleFileText(exampleName, EvidencePolicyExampleService.BEFORE_FILE),
                    Files.readString(outputDirectory.resolve(EvidencePolicyExampleService.BEFORE_FILE)));
            assertEquals(service.exampleFileText(exampleName, EvidencePolicyExampleService.AFTER_FILE),
                    Files.readString(outputDirectory.resolve(EvidencePolicyExampleService.AFTER_FILE)));
            assertEquals(service.exampleFileText(exampleName, EvidencePolicyExampleService.EXPECTED_DECISION_FILE),
                    Files.readString(outputDirectory.resolve(EvidencePolicyExampleService.EXPECTED_DECISION_FILE)));
        }
    }

    @Test
    void exportedExamplesEvaluateExpectedPassWarnAndFailDecisions() throws Exception {
        for (Map.Entry<String, String> expected : EXPECTED_DECISIONS.entrySet()) {
            Path outputDirectory = exportExample(expected.getKey());
            EvidenceHandoffPolicyService.PolicyEvaluation evaluation = evaluateExported(outputDirectory);
            JsonNode descriptor = OBJECT_MAPPER.readTree(
                    outputDirectory.resolve(EvidencePolicyExampleService.EXPECTED_DECISION_FILE).toFile());

            assertEquals(expected.getValue(), evaluation.decision().name(), expected.getKey());
            assertEquals(descriptor.path("expectedFailCount").asInt(), evaluation.summary().failCount());
            assertEquals(descriptor.path("expectedWarnCount").asInt(), evaluation.summary().warnCount());
            assertEquals(descriptor.path("expectedInfoCount").asInt(), evaluation.summary().infoCount());
            assertEquals(descriptor.path("expectedIgnoredCount").asInt(), evaluation.summary().ignoredCount());
            assertEquals(descriptor.path("expectedUnclassifiedCount").asInt(),
                    evaluation.summary().unclassifiedCount());
        }
    }

    @Test
    void printPolicyExampleShowsCompactSummary() {
        CapturedRun run = runCli("--print-policy-example", "receiver-redaction-warn");

        assertEquals(0, run.result().exitCode());
        assertTrue(run.error().isBlank());
        assertTrue(run.output().contains("Evidence policy example: receiver-redaction-warn"));
        assertTrue(run.output().contains("template: receiver-redaction"));
        assertTrue(run.output().contains("expectedDecision: WARN"));
    }

    @Test
    void walkthroughCommandProducesDeterministicJsonForPassWarnAndFailExamples() throws Exception {
        for (String exampleName : List.of(
                "strict-zero-drift-pass",
                "receiver-redaction-warn",
                "regulated-handoff-fail")) {
            CapturedRun first = runCli("--walkthrough-policy-example", exampleName,
                    "--example-output-dir", tempDir.resolve(exampleName + "-first").toString(),
                    "--policy-report-format", "json");
            CapturedRun second = runCli("--walkthrough-policy-example", exampleName,
                    "--example-output-dir", tempDir.resolve(exampleName + "-second").toString(),
                    "--policy-report-format", "json");

            assertEquals(0, first.result().exitCode(), exampleName);
            assertEquals(first.output(), second.output(), exampleName);
            JsonNode walkthrough = OBJECT_MAPPER.readTree(first.output());
            assertEquals(exampleName, walkthrough.path("exampleName").asText());
            assertEquals(EXPECTED_DECISIONS.get(exampleName), walkthrough.path("actualDecision").asText());
            assertTrue(walkthrough.path("decisionMatchesExpectation").asBoolean());
            assertEquals(EvidencePolicyExampleService.BEFORE_FILE,
                    walkthrough.path("exportedFiles").path("before").asText());
        }
    }

    @Test
    void walkthroughCommandProducesDeterministicMarkdown() {
        CapturedRun first = runCli("--walkthrough-policy-example", "audit-append-warn",
                "--example-output-dir", tempDir.resolve("audit-first").toString());
        CapturedRun second = runCli("--walkthrough-policy-example", "audit-append-warn",
                "--example-output-dir", tempDir.resolve("audit-second").toString());

        assertEquals(0, first.result().exitCode());
        assertEquals(first.output(), second.output());
        assertTrue(first.output().contains("# LoadBalancerPro Evidence Policy Walkthrough"));
        assertTrue(first.output().contains("- Actual decision: WARN"));
        assertTrue(first.output().contains("Local checksum policy walkthrough only"));
    }

    @Test
    void walkthroughFailOnPolicyFailReturnsControlledNonZero() {
        CapturedRun run = runCli("--walkthrough-policy-example", "regulated-handoff-fail",
                "--example-output-dir", tempDir.resolve("regulated-fail").toString(),
                "--fail-on-policy-fail");

        assertEquals(2, run.result().exitCode());
        assertTrue(run.error().isBlank());
        assertTrue(run.output().contains("- Actual decision: FAIL"));
    }

    @Test
    void existingFileConflictRequiresForce() throws Exception {
        Path outputDirectory = exportExample("strict-zero-drift-pass");

        CapturedRun conflict = runCli("--export-policy-example", "strict-zero-drift-pass",
                "--example-output-dir", outputDirectory.toString());
        CapturedRun forced = runCli("--export-policy-example", "strict-zero-drift-pass",
                "--example-output-dir", outputDirectory.toString(), "--force");

        assertEquals(2, conflict.result().exitCode());
        assertTrue(conflict.output().isBlank());
        assertTrue(conflict.error().contains("target file already exists"));
        assertEquals(0, forced.result().exitCode());
    }

    @Test
    void walkthroughCommandsDoNotConstructCloudManager() throws Exception {
        try (MockedConstruction<CloudManager> mocked = Mockito.mockConstruction(CloudManager.class)) {
            CapturedRun list = runCli("--list-policy-examples");
            CapturedRun export = runCli("--export-policy-example", "strict-zero-drift-pass",
                    "--example-output-dir", tempDir.resolve("strict-export").toString());
            CapturedRun walkthrough = runCli("--walkthrough-policy-example", "receiver-redaction-warn",
                    "--example-output-dir", tempDir.resolve("receiver-walkthrough").toString());

            assertEquals(0, list.result().exitCode());
            assertEquals(0, export.result().exitCode());
            assertEquals(0, walkthrough.result().exitCode());
            assertTrue(mocked.constructed().isEmpty());
        }
    }

    @Test
    void policyExampleFlagsAreRecognized() {
        assertTrue(RemediationReportCli.isRequested(new String[]{"--list-policy-examples"}));
        assertTrue(RemediationReportCli.isRequested(
                new String[]{"--export-policy-example", "strict-zero-drift-pass"}));
        assertTrue(RemediationReportCli.isRequested(
                new String[]{"--walkthrough-policy-example=receiver-redaction-warn"}));
    }

    private Path exportExample(String exampleName) {
        Path outputDirectory = tempDir.resolve(exampleName + "-" + System.nanoTime());
        CapturedRun run = runCli("--export-policy-example", exampleName,
                "--example-output-dir", outputDirectory.toString());
        assertEquals(0, run.result().exitCode(), exampleName);
        return outputDirectory;
    }

    private EvidenceHandoffPolicyService.PolicyEvaluation evaluateExported(Path outputDirectory) throws Exception {
        JsonNode expected = OBJECT_MAPPER.readTree(
                outputDirectory.resolve(EvidencePolicyExampleService.EXPECTED_DECISION_FILE).toFile());
        EvidenceCatalogDiffService diffService = new EvidenceCatalogDiffService();
        EvidenceHandoffPolicyService policyService = new EvidenceHandoffPolicyService();
        EvidencePolicyTemplateService templateService = new EvidencePolicyTemplateService();
        EvidenceCatalogDiffService.EvidenceCatalogDiff diff = diffService.diff(
                new EvidenceCatalogDiffService.DiffRequest(
                        outputDirectory.resolve(expected.path("before").asText()),
                        outputDirectory.resolve(expected.path("after").asText()),
                        false));
        EvidenceHandoffPolicyService.HandoffPolicy policy = policyService.readPolicyJson(
                "template:" + expected.path("template").asText(),
                templateService.templateJson(expected.path("template").asText()));
        return policyService.evaluate(diff, policy);
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

    private static Map<String, String> expectedDecisions() {
        Map<String, String> decisions = new LinkedHashMap<>();
        decisions.put("strict-zero-drift-pass", "PASS");
        decisions.put("strict-zero-drift-fail", "FAIL");
        decisions.put("receiver-redaction-warn", "WARN");
        decisions.put("audit-append-warn", "WARN");
        decisions.put("regulated-handoff-pass", "PASS");
        decisions.put("regulated-handoff-fail", "FAIL");
        decisions.put("investigation-working-copy-warn", "WARN");
        return java.util.Collections.unmodifiableMap(decisions);
    }

    private record CapturedRun(RemediationReportCli.Result result, String output, String error) {
    }
}
