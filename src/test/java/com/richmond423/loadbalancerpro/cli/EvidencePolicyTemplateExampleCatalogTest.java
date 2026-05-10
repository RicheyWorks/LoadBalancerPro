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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class EvidencePolicyTemplateExampleCatalogTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String EXAMPLE_ROOT = "evidence-policy-examples/";
    private static final List<ExampleDescriptor> EXAMPLES = List.of(
            new ExampleDescriptor("strict-zero-drift", "expected-decision.json"),
            new ExampleDescriptor("strict-zero-drift", "expected-fail.json"),
            new ExampleDescriptor("receiver-redaction", "expected-decision.json"),
            new ExampleDescriptor("audit-append", "expected-decision.json"),
            new ExampleDescriptor("regulated-handoff", "expected-decision.json"),
            new ExampleDescriptor("regulated-handoff", "expected-fail.json"),
            new ExampleDescriptor("investigation-working-copy", "expected-decision.json"));

    @TempDir
    Path tempDir;

    @Test
    void examplesMatchExpectedPolicyDecisions() throws Exception {
        for (ExampleDescriptor descriptor : EXAMPLES) {
            ExampleRun run = evaluate(descriptor);
            JsonNode expected = run.expected();
            EvidenceHandoffPolicyService.PolicyEvaluation evaluation = run.evaluation();

            assertEquals(expected.path("expectedDecision").asText(), evaluation.decision().name(),
                    descriptor + " decision");
            assertEquals(expected.path("expectedFailCount").asInt(), evaluation.summary().failCount(),
                    descriptor + " fail count");
            assertEquals(expected.path("expectedWarnCount").asInt(), evaluation.summary().warnCount(),
                    descriptor + " warn count");
            assertEquals(expected.path("expectedInfoCount").asInt(), evaluation.summary().infoCount(),
                    descriptor + " info count");
            assertEquals(expected.path("expectedIgnoredCount").asInt(), evaluation.summary().ignoredCount(),
                    descriptor + " ignored count");
            assertEquals(expected.path("expectedUnclassifiedCount").asInt(),
                    evaluation.summary().unclassifiedCount(), descriptor + " unclassified count");
            assertEquals(textArray(expected.path("expectedChangedPaths")),
                    evaluation.evaluatedChanges().stream()
                            .map(EvidenceHandoffPolicyService.EvaluatedPolicyChange::path)
                            .toList(),
                    descriptor + " changed paths");
            assertEquals(textArray(expected.path("expectedChangeTypes")),
                    evaluation.evaluatedChanges().stream()
                            .map(change -> change.changeType().name())
                            .toList(),
                    descriptor + " change types");
        }
    }

    @Test
    void examplesExercisePassWarnAndFailDecisions() throws Exception {
        List<String> decisions = EXAMPLES.stream()
                .map(descriptor -> {
                    try {
                        return evaluate(descriptor).evaluation().decision().name();
                    } catch (Exception e) {
                        throw new AssertionError("failed to evaluate " + descriptor, e);
                    }
                })
                .distinct()
                .sorted()
                .toList();

        assertEquals(List.of("FAIL", "PASS", "WARN"), decisions);
    }

    @Test
    void policyReportsAreDeterministicForExampleCatalogs() throws Exception {
        ExampleRun run = evaluate(new ExampleDescriptor("receiver-redaction", "expected-decision.json"));
        EvidenceHandoffPolicyService policyService = new EvidenceHandoffPolicyService();

        String firstJson = policyService.renderJson(run.evaluation());
        String secondJson = policyService.renderJson(run.evaluation());
        String firstMarkdown = policyService.renderMarkdown(run.evaluation());
        String secondMarkdown = policyService.renderMarkdown(run.evaluation());

        assertEquals(firstJson, secondJson);
        assertEquals(firstMarkdown, secondMarkdown);
        assertTrue(firstJson.contains("\"decision\" : \"WARN\""));
        assertTrue(firstMarkdown.contains("# LoadBalancerPro Evidence Handoff Policy Report"));
        assertTrue(firstMarkdown.contains("receiver-redaction-before.json"));
    }

    @Test
    void examplesUseRealServicesWithoutCloudManagerConstruction() throws Exception {
        try (MockedConstruction<CloudManager> mocked = Mockito.mockConstruction(CloudManager.class)) {
            for (ExampleDescriptor descriptor : EXAMPLES) {
                evaluate(descriptor);
            }

            assertTrue(mocked.constructed().isEmpty());
        }
    }

    private ExampleRun evaluate(ExampleDescriptor descriptor) throws Exception {
        JsonNode expected = readJson(resourcePath(descriptor.directory(), descriptor.expectedFile()));
        Path before = copyResource(resourcePath(descriptor.directory(), expected.path("before").asText()),
                descriptor.directory() + "-" + expected.path("before").asText());
        Path after = copyResource(resourcePath(descriptor.directory(), expected.path("after").asText()),
                descriptor.directory() + "-" + expected.path("after").asText());

        EvidenceCatalogDiffService diffService = new EvidenceCatalogDiffService();
        EvidenceHandoffPolicyService policyService = new EvidenceHandoffPolicyService();
        EvidencePolicyTemplateService templateService = new EvidencePolicyTemplateService();
        EvidenceCatalogDiffService.EvidenceCatalogDiff diff = diffService.diff(
                new EvidenceCatalogDiffService.DiffRequest(before, after, false));
        EvidenceHandoffPolicyService.HandoffPolicy policy = policyService.readPolicyJson(
                "template:" + expected.path("template").asText(),
                templateService.templateJson(expected.path("template").asText()));
        return new ExampleRun(expected, diff, policyService.evaluate(diff, policy));
    }

    private JsonNode readJson(String resourcePath) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream input = resource.getInputStream()) {
            return OBJECT_MAPPER.readTree(input);
        }
    }

    private Path copyResource(String resourcePath, String fileName) throws Exception {
        Path target = tempDir.resolve(fileName.replace('/', '-'));
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream input = resource.getInputStream()) {
            Files.write(target, input.readAllBytes());
        }
        return target;
    }

    private static String resourcePath(String directory, String fileName) {
        return EXAMPLE_ROOT + directory + "/" + fileName;
    }

    private static List<String> textArray(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        return OBJECT_MAPPER.convertValue(node,
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    private record ExampleDescriptor(String directory, String expectedFile) {
    }

    private record ExampleRun(
            JsonNode expected,
            EvidenceCatalogDiffService.EvidenceCatalogDiff diff,
            EvidenceHandoffPolicyService.PolicyEvaluation evaluation) {
    }
}
