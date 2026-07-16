package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class EnterpriseLabExperimentProofExporter {
    private static final String REPORT_FILE = "enterprise-lab-experiment-proof.json";
    private static final String SUMMARY_FILE = "enterprise-lab-experiment-proof-summary.md";
    private static final String METADATA_FILE = "enterprise-lab-experiment-proof-metadata.json";

    private final ObjectMapper objectMapper;

    public EnterpriseLabExperimentProofExporter() {
        this(new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).findAndRegisterModules());
    }

    EnterpriseLabExperimentProofExporter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    public static Path validateOutputDirectory(Path outputDirectory) {
        return EnterpriseLabEvidenceOutputPolicy.requireTargetPath(outputDirectory);
    }

    public Manifest export(Path outputDirectory, EnterpriseLabExperimentProofReport report, String gitCommit)
            throws IOException {
        Path safeOutput = validateOutputDirectory(outputDirectory);
        EnterpriseLabExperimentProofReport safeReport = Objects.requireNonNull(report, "report cannot be null");
        Files.createDirectories(safeOutput);
        Path reportPath = safeOutput.resolve(REPORT_FILE);
        Path summaryPath = safeOutput.resolve(SUMMARY_FILE);
        Path metadataPath = safeOutput.resolve(METADATA_FILE);
        writeJson(reportPath, safeReport);
        writeText(summaryPath, markdown(safeReport));
        writeJson(metadataPath, Map.of(
                "generatedBy", "EnterpriseLabExperimentProofExporter",
                "gitCommit", gitCommit == null || gitCommit.isBlank() ? "unknown" : gitCommit.trim(),
                "requestedSuite", safeReport.requestedSuite(),
                "scenarioCount", safeReport.scenarios().size(),
                "actualRequestCount", safeReport.totalActualRequests(),
                "reportFingerprint", safeReport.contentFingerprint(),
                "safety", "ignored target-only literal-loopback proof; no external target, production routing, or release action"));
        return new Manifest(safeOutput.toString(), reportPath.toString(), summaryPath.toString(), metadataPath.toString());
    }

    private void writeJson(Path path, Object value) throws IOException {
        writeText(path, objectMapper.writeValueAsString(value) + System.lineSeparator());
    }

    private static void writeText(Path path, String text) throws IOException {
        EnterpriseLabEvidenceOutputPolicy.assertNoSecretLikeText(text);
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }

    private static String markdown(EnterpriseLabExperimentProofReport report) {
        String line = System.lineSeparator();
        StringBuilder value = new StringBuilder();
        value.append("# Enterprise Lab Real-Loopback Experiment Proof").append(line).append(line);
        value.append("- Suite: `").append(report.requestedSuite()).append("`").append(line);
        value.append("- Scenarios: `").append(report.scenarios().size()).append("`").append(line);
        value.append("- Actual loopback requests: `").append(report.totalActualRequests()).append("`").append(line);
        value.append("- All checks passed: `").append(report.allPassed()).append("`").append(line);
        value.append("- Report fingerprint: `").append(report.contentFingerprint()).append("`").append(line).append(line);
        value.append("| Proof | Expected | Final state | Requests | Guardrail | Dispositions | Triggers | Restored | Passed |").append(line);
        value.append("| --- | --- | --- | ---: | --- | --- | --- | --- | --- |").append(line);
        report.scenarios().forEach(scenario -> value.append("| ")
                .append(scenario.proofId()).append(" | ")
                .append(escape(scenario.expectedOutcome())).append(" | ")
                .append(scenario.finalRecord().lifecycle().state()).append(" | ")
                .append(scenario.actualRequestCount()).append(" | ")
                .append(scenario.guardrailAction()).append(" | ")
                .append(escape(scenario.evaluationDispositions().toString())).append(" | ")
                .append(escape(scenario.rollbackTriggers().toString())).append(" | ")
                .append(scenario.baselineRestored()).append(" | ")
                .append(scenario.passed()).append(" |").append(line));
        value.append(line).append("## Boundaries").append(line).append(line);
        report.scopeBoundaries().forEach(boundary -> value.append("- ").append(boundary).append(line));
        return value.toString();
    }

    private static String escape(String value) {
        return value.replace("|", "\\|").replace(System.lineSeparator(), " ");
    }

    public record Manifest(String outputDirectory, String reportJson, String markdownSummary, String metadataJson) {
        public Manifest {
            outputDirectory = require(outputDirectory, "outputDirectory");
            reportJson = require(reportJson, "reportJson");
            markdownSummary = require(markdownSummary, "markdownSummary");
            metadataJson = require(metadataJson, "metadataJson");
        }

        private static String require(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " cannot be null or blank");
            }
            return value;
        }
    }
}
