package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

final class EvidencePolicyTrainingLabService {
    private static final String TRAINING_LAB_VERSION = "1";
    private static final ObjectMapper TRAINING_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final List<String> LIMITATIONS = List.of(
            "Local packaged examples only; no identity proof or cryptographic signature is provided.",
            "This training transcript is not a legal chain-of-custody system.",
            "The lab validates synthetic catalog policy outcomes and is not a substitute for real incident review.");

    TrainingLabResult run(TrainingLabRequest request) throws IOException {
        return run(request, Map.of());
    }

    TrainingLabResult run(
            TrainingLabRequest request,
            Map<String, String> expectedDecisionOverrides) throws IOException {
        Objects.requireNonNull(request, "training lab request cannot be null");
        Map<String, String> overrides = expectedDecisionOverrides == null ? Map.of() : expectedDecisionOverrides;
        EvidencePolicyExampleService exampleService = new EvidencePolicyExampleService();
        boolean temporaryExportRoot = request.exportDirectory().isEmpty();
        Path exportRoot = temporaryExportRoot
                ? Files.createTempDirectory("lbp-policy-training-lab-")
                : request.exportDirectory().get().toAbsolutePath().normalize();
        if (!temporaryExportRoot) {
            Files.createDirectories(exportRoot);
        }

        try {
            List<TrainingLabExampleResult> examples = new ArrayList<>();
            for (EvidencePolicyExampleService.PolicyExample example : exampleService.listExamples()) {
                Path exampleDirectory = exportRoot.resolve(example.name()).normalize();
                if (!exampleDirectory.getParent().equals(exportRoot)) {
                    throw new IllegalArgumentException("example export path cannot escape training lab directory");
                }
                EvidencePolicyExampleService.ExportedPolicyExample exported = exampleService.exportExample(
                        example.name(),
                        exampleDirectory,
                        request.force());
                examples.add(evaluateExample(exported, request.includeDetails(), overrides));
            }
            return buildResult(examples);
        } finally {
            if (temporaryExportRoot) {
                deleteRecursively(exportRoot);
            }
        }
    }

    String renderJson(TrainingLabResult result) throws IOException {
        return TRAINING_MAPPER.writeValueAsString(result) + System.lineSeparator();
    }

    String renderMarkdown(TrainingLabResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("# LoadBalancerPro Evidence Policy Training Lab")
                .append(System.lineSeparator())
                .append(System.lineSeparator());
        builder.append("- Training lab version: ").append(result.trainingLabVersion()).append(System.lineSeparator());
        builder.append("- Final status: ").append(result.summary().finalStatus()).append(System.lineSeparator());
        builder.append("- Total examples: ").append(result.summary().totalExamples()).append(System.lineSeparator());
        builder.append("- Expected PASS: ").append(result.summary().expectedPassCount()).append(System.lineSeparator());
        builder.append("- Expected WARN: ").append(result.summary().expectedWarnCount()).append(System.lineSeparator());
        builder.append("- Expected FAIL: ").append(result.summary().expectedFailCount()).append(System.lineSeparator());
        builder.append("- Matched: ").append(result.summary().matchedCount()).append(System.lineSeparator());
        builder.append("- Mismatched: ").append(result.summary().mismatchCount()).append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("## Example Results").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("| Example | Template | Expected | Actual | Result | Policy Counts |")
                .append(System.lineSeparator());
        builder.append("| --- | --- | --- | --- | --- | --- |").append(System.lineSeparator());
        for (TrainingLabExampleResult example : result.examples()) {
            builder.append("| ")
                    .append(escapeMarkdown(example.exampleName()))
                    .append(" | ")
                    .append(escapeMarkdown(example.templateName()))
                    .append(" | ")
                    .append(example.expectedDecision())
                    .append(" | ")
                    .append(example.actualDecision())
                    .append(" | ")
                    .append(example.result())
                    .append(" | ")
                    .append("fail=")
                    .append(example.failCount())
                    .append(", warn=")
                    .append(example.warnCount())
                    .append(", info=")
                    .append(example.infoCount())
                    .append(", ignored=")
                    .append(example.ignoredCount())
                    .append(", unclassified=")
                    .append(example.unclassifiedCount())
                    .append(" |")
                    .append(System.lineSeparator());
        }

        builder.append(System.lineSeparator()).append("## Limitations").append(System.lineSeparator())
                .append(System.lineSeparator());
        for (String limitation : result.limitations()) {
            builder.append("- ").append(limitation).append(System.lineSeparator());
        }
        return builder.toString();
    }

    int exitCode(TrainingLabResult result, boolean failOnMismatch) {
        Objects.requireNonNull(result, "training lab result cannot be null");
        return failOnMismatch && result.summary().mismatchCount() > 0 ? 2 : 0;
    }

    private TrainingLabExampleResult evaluateExample(
            EvidencePolicyExampleService.ExportedPolicyExample exported,
            boolean includeDetails,
            Map<String, String> expectedDecisionOverrides) throws IOException {
        EvidenceCatalogDiffService diffService = new EvidenceCatalogDiffService();
        EvidenceHandoffPolicyService policyService = new EvidenceHandoffPolicyService();
        EvidencePolicyTemplateService templateService = new EvidencePolicyTemplateService();
        JsonNode expected = TRAINING_MAPPER.readTree(exported.expectedDecisionPath().toFile());
        String templateName = text(expected, "template", exported.example().templateName());
        EvidenceCatalogDiffService.EvidenceCatalogDiff diff = diffService.diff(
                new EvidenceCatalogDiffService.DiffRequest(
                        exported.beforePath(),
                        exported.afterPath(),
                        false));
        EvidenceHandoffPolicyService.HandoffPolicy policy = policyService.readPolicyJson(
                "template:" + templateName,
                templateService.templateJson(templateName));
        EvidenceHandoffPolicyService.PolicyEvaluation evaluation = policyService.evaluate(diff, policy);
        String expectedDecision = expectedDecisionOverrides.getOrDefault(
                exported.example().name(),
                text(expected, "expectedDecision", exported.example().expectedDecision()));
        String actualDecision = evaluation.decision().name();
        List<TrainingLabChange> changes = includeDetails
                ? evaluation.evaluatedChanges().stream()
                        .map(change -> new TrainingLabChange(
                                change.path(),
                                change.changeType().name(),
                                change.severity().name(),
                                change.reason()))
                        .toList()
                : null;
        return new TrainingLabExampleResult(
                exported.example().name(),
                templateName,
                expectedDecision,
                actualDecision,
                expectedDecision.equals(actualDecision) ? TrainingLabExampleStatus.MATCH
                        : TrainingLabExampleStatus.MISMATCH,
                evaluation.summary().failCount(),
                evaluation.summary().warnCount(),
                evaluation.summary().infoCount(),
                evaluation.summary().ignoredCount(),
                evaluation.summary().unclassifiedCount(),
                changes);
    }

    private static TrainingLabResult buildResult(List<TrainingLabExampleResult> examples) {
        int expectedPassCount = 0;
        int expectedWarnCount = 0;
        int expectedFailCount = 0;
        int matchedCount = 0;
        int mismatchCount = 0;
        for (TrainingLabExampleResult example : examples) {
            switch (normalizeDecision(example.expectedDecision())) {
                case "PASS" -> expectedPassCount++;
                case "WARN" -> expectedWarnCount++;
                case "FAIL" -> expectedFailCount++;
                default -> throw new IllegalArgumentException(
                        "expected decision must be PASS, WARN, or FAIL for " + example.exampleName());
            }
            if (example.result() == TrainingLabExampleStatus.MATCH) {
                matchedCount++;
            } else {
                mismatchCount++;
            }
        }
        TrainingLabSummary summary = new TrainingLabSummary(
                examples.size(),
                expectedPassCount,
                expectedWarnCount,
                expectedFailCount,
                matchedCount,
                mismatchCount,
                mismatchCount == 0 ? TrainingLabStatus.PASS : TrainingLabStatus.FAIL);
        return new TrainingLabResult(
                TRAINING_LAB_VERSION,
                summary,
                List.copyOf(examples),
                LIMITATIONS);
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : fallback;
    }

    private static String normalizeDecision(String decision) {
        return Objects.requireNonNull(decision, "expected decision cannot be null")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static String escapeMarkdown(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace(System.lineSeparator(), " ");
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    enum TrainingLabFormat {
        MARKDOWN,
        JSON;

        static TrainingLabFormat parse(String value) {
            String normalized = Objects.requireNonNull(value, "training lab format is required")
                    .trim()
                    .toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "markdown", "md" -> MARKDOWN;
                case "json" -> JSON;
                default -> throw new IllegalArgumentException("training lab format must be markdown or json");
            };
        }
    }

    enum TrainingLabStatus {
        PASS,
        FAIL
    }

    enum TrainingLabExampleStatus {
        MATCH,
        MISMATCH
    }

    record TrainingLabRequest(
            Optional<Path> exportDirectory,
            boolean force,
            boolean includeDetails) {

        TrainingLabRequest {
            exportDirectory = exportDirectory == null ? Optional.empty() : exportDirectory;
        }
    }

    record TrainingLabResult(
            String trainingLabVersion,
            TrainingLabSummary summary,
            List<TrainingLabExampleResult> examples,
            List<String> limitations) {

        TrainingLabResult {
            examples = examples == null ? List.of() : List.copyOf(examples);
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
        }
    }

    record TrainingLabSummary(
            int totalExamples,
            int expectedPassCount,
            int expectedWarnCount,
            int expectedFailCount,
            int matchedCount,
            int mismatchCount,
            TrainingLabStatus finalStatus) {
    }

    record TrainingLabExampleResult(
            String exampleName,
            String templateName,
            String expectedDecision,
            String actualDecision,
            TrainingLabExampleStatus result,
            int failCount,
            int warnCount,
            int infoCount,
            int ignoredCount,
            int unclassifiedCount,
            List<TrainingLabChange> changes) {

        TrainingLabExampleResult {
            changes = changes == null ? null : List.copyOf(changes);
        }
    }

    record TrainingLabChange(
            String path,
            String changeType,
            String severity,
            String reason) {
    }
}
