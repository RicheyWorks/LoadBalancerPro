package com.richmond423.loadbalancerpro.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class EnterpriseLabEvidenceExporter {
    private static final String CATALOG_FILE = "enterprise-lab-scenario-catalog.json";
    private static final String RUN_FILE = "enterprise-lab-run.json";
    private static final String SUMMARY_FILE = "enterprise-lab-run-summary.md";
    private static final String METADATA_FILE = "enterprise-lab-evidence-metadata.json";

    private final ObjectMapper objectMapper;

    public EnterpriseLabEvidenceExporter() {
        this(new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                .findAndRegisterModules());
    }

    EnterpriseLabEvidenceExporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EnterpriseLabEvidenceManifest export(Path outputDirectory,
                                                List<EnterpriseLabScenarioMetadata> scenarios,
                                                EnterpriseLabRun run,
                                                String gitCommit) throws IOException {
        Path safeOutputDirectory = requireTargetPath(outputDirectory);
        Files.createDirectories(safeOutputDirectory);

        Path catalogPath = safeOutputDirectory.resolve(CATALOG_FILE);
        Path runPath = safeOutputDirectory.resolve(RUN_FILE);
        Path summaryPath = safeOutputDirectory.resolve(SUMMARY_FILE);
        Path metadataPath = safeOutputDirectory.resolve(METADATA_FILE);

        writeJson(catalogPath, Map.of(
                "generatedBy", "EnterpriseLabEvidenceExporter",
                "scenarioCount", scenarios.size(),
                "scenarios", scenarios));
        writeJson(runPath, run);
        Files.writeString(summaryPath, markdownSummary(scenarios, run), StandardCharsets.UTF_8);
        writeJson(metadataPath, Map.of(
                "generatedBy", "EnterpriseLabEvidenceExporter",
                "gitCommit", gitCommit == null || gitCommit.isBlank() ? "unknown" : gitCommit.trim(),
                "runId", run.runId(),
                "mode", run.mode(),
                "policyAuditEventCount", run.policyAuditEvents().size(),
                "outputDirectory", safeOutputDirectory.toString(),
                "safety", "ignored target/ evidence only; no secrets, release actions, container publication, live cloud, or private-network execution"));

        return new EnterpriseLabEvidenceManifest(
                safeOutputDirectory.toString(),
                catalogPath.toString(),
                runPath.toString(),
                summaryPath.toString(),
                metadataPath.toString());
    }

    private void writeJson(Path path, Object value) throws IOException {
        String json = objectMapper.writeValueAsString(value);
        assertNoSecretLikeText(json);
        Files.writeString(path, json + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static String markdownSummary(List<EnterpriseLabScenarioMetadata> scenarios, EnterpriseLabRun run) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Enterprise Adaptive Routing Lab Run").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- Run id: `").append(run.runId()).append('`').append(System.lineSeparator());
        builder.append("- Created at: `").append(run.createdAt()).append('`').append(System.lineSeparator());
        builder.append("- Mode: `").append(run.mode()).append('`').append(System.lineSeparator());
        builder.append("- Scenario count: `").append(run.scorecard().totalScenarios()).append('`').append(System.lineSeparator());
        builder.append("- Storage: ").append(run.storageMode()).append(System.lineSeparator());
        builder.append("- Policy audit events: `").append(run.policyAuditEvents().size()).append('`')
                .append(System.lineSeparator());
        builder.append("- Final recommendation: ").append(run.scorecard().finalRecommendation()).append(System.lineSeparator());
        builder.append("- Safety: lab-grade evidence, not production gateway activation").append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("## Scorecard").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("| Metric | Value |").append(System.lineSeparator());
        builder.append("| --- | --- |").append(System.lineSeparator());
        appendScore(builder, "Total scenarios", run.scorecard().totalScenarios());
        appendScore(builder, "Baseline vs shadow differences", run.scorecard().baselineVsShadowDifferences());
        appendScore(builder, "Baseline vs influence differences", run.scorecard().baselineVsInfluenceDifferences());
        appendScore(builder, "Guardrail-blocked influence count", run.scorecard().guardrailBlockedInfluenceCount());
        appendScore(builder, "Unsafe/all-unhealthy blocked count", run.scorecard().unsafeAllUnhealthyBlockedCount());
        appendScore(builder, "Stale/conflicting signal blocked count", run.scorecard().staleConflictingSignalBlockedCount());
        builder.append("| Explanation coverage | ").append(run.scorecard().explanationCoverage()).append(" |")
                .append(System.lineSeparator());
        builder.append("| Mode used | ").append(run.scorecard().modeUsed()).append(" |")
                .append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("## Scenario Matrix").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("| Scenario | Category | Baseline | Recommendation | Final | Mode | Changed | Guardrail | Rollback |")
                .append(System.lineSeparator());
        builder.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- |").append(System.lineSeparator());
        Map<String, EnterpriseLabScenarioMetadata> metadataById = scenarios.stream()
                .collect(Collectors.toMap(EnterpriseLabScenarioMetadata::scenarioId, metadata -> metadata));
        run.results().forEach(result -> {
            EnterpriseLabScenarioMetadata metadata = metadataById.get(result.scenarioName());
            builder.append("| ")
                    .append(result.scenarioName())
                    .append(" | ")
                    .append(metadata == null ? "unknown" : metadata.category())
                    .append(" | ")
                    .append(display(result.baselineSelectedBackend()))
                    .append(" | ")
                    .append(display(result.shadowRecommendedBackend()))
                    .append(" / ")
                    .append(display(result.shadowRecommendedAction()))
                    .append(" | ")
                    .append(display(result.policyDecision().finalDecision()))
                    .append(" | ")
                    .append(result.policyDecision().mode())
                    .append(" | ")
                    .append(result.resultChanged())
                    .append(" | ")
                    .append(escape(result.guardrailReason()))
                    .append(" | ")
                    .append(escape(result.rollbackReason()))
                    .append(" |")
                    .append(System.lineSeparator());
        });
        builder.append(System.lineSeparator());
        builder.append("## Policy Audit Events").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("| Event | Mode | Baseline | Recommendation | Final | Changed | Guardrails | Rollback |")
                .append(System.lineSeparator());
        builder.append("| --- | --- | --- | --- | --- | --- | --- | --- |").append(System.lineSeparator());
        run.policyAuditEvents().forEach(event -> builder.append("| ")
                .append(event.eventId())
                .append(" | ")
                .append(event.mode())
                .append(" | ")
                .append(display(event.baselineDecision()))
                .append(" | ")
                .append(display(event.recommendation()))
                .append(" | ")
                .append(display(event.finalDecision()))
                .append(" | ")
                .append(event.changed())
                .append(" | ")
                .append(escape(String.join("; ", event.guardrailReasons())))
                .append(" | ")
                .append(escape(event.rollbackReason()))
                .append(" |")
                .append(System.lineSeparator()));
        builder.append(System.lineSeparator());
        builder.append("## Safety Notes").append(System.lineSeparator()).append(System.lineSeparator());
        run.safetyNotes().forEach(note -> builder.append("- ").append(note).append(System.lineSeparator()));
        return builder.toString();
    }

    private static void appendScore(StringBuilder builder, String label, int value) {
        builder.append("| ").append(label).append(" | ").append(value).append(" |").append(System.lineSeparator());
    }

    private static Path requireTargetPath(Path outputDirectory) {
        Path targetRoot = Path.of("target").toAbsolutePath().normalize();
        Path resolvedOutput = outputDirectory.toAbsolutePath().normalize();
        if (!resolvedOutput.startsWith(targetRoot)) {
            throw new IllegalArgumentException("Enterprise Lab evidence output must stay under target/: "
                    + outputDirectory);
        }
        return resolvedOutput;
    }

    private static void assertNoSecretLikeText(String text) {
        String normalized = text.toLowerCase();
        if (normalized.contains("bearer ")
                || normalized.contains("x-api-key")
                || normalized.contains("change_me_local_api_key")
                || normalized.matches("(?s).*(password|secret|credential|token)\\s*[:=]\\s*[a-z0-9._~+/-]{8,}.*")) {
            throw new IllegalArgumentException("Refusing to write Enterprise Lab evidence that looks secret-bearing");
        }
    }

    private static String display(String value) {
        return value == null || value.isBlank() ? "none" : escape(value);
    }

    private static String escape(String value) {
        return value.replace("|", "\\|").replace(System.lineSeparator(), " ");
    }

    public record EnterpriseLabEvidenceManifest(
            String outputDirectory,
            String scenarioCatalogJson,
            String labRunJson,
            String markdownSummary,
            String metadataJson) {
    }
}

