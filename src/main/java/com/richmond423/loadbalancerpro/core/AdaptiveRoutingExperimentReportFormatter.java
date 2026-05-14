package com.richmond423.loadbalancerpro.core;

import java.util.Map;
import java.util.stream.Collectors;

public final class AdaptiveRoutingExperimentReportFormatter {

    public String format(AdaptiveRoutingExperimentReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Adaptive Routing Experiment Report").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- Mode: `").append(report.mode()).append('`').append(System.lineSeparator());
        builder.append("- Active LASE influence enabled: `").append(report.activeInfluenceEnabled()).append('`')
                .append(System.lineSeparator());
        builder.append("- Scenario count: `").append(report.results().size()).append('`').append(System.lineSeparator());
        for (String note : report.safetyNotes()) {
            builder.append("- Safety: ").append(note).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator());
        builder.append("| Scenario | Baseline backend | Shadow recommendation | Policy mode | Final backend | Changed | Guardrail |")
                .append(System.lineSeparator());
        builder.append("| --- | --- | --- | --- | --- | --- | --- |").append(System.lineSeparator());
        for (AdaptiveRoutingExperimentResult result : report.results()) {
            builder.append("| ")
                    .append(result.scenarioName())
                    .append(" | ")
                    .append(display(result.baselineSelectedBackend()))
                    .append(" | ")
                    .append(display(result.shadowRecommendedBackend()))
                    .append(" / ")
                    .append(display(result.shadowRecommendedAction()))
                    .append(" | ")
                    .append(result.policyDecision().mode())
                    .append(" | ")
                    .append(display(result.influencedSelectedBackend()))
                    .append(" | ")
                    .append(result.resultChanged())
                    .append(" | ")
                    .append(escape(result.guardrailReason()))
                    .append(" |")
                    .append(System.lineSeparator());
        }
        builder.append(System.lineSeparator());
        for (AdaptiveRoutingExperimentResult result : report.results()) {
            appendScenario(builder, result);
        }
        return builder.toString();
    }

    private void appendScenario(StringBuilder builder, AdaptiveRoutingExperimentResult result) {
        builder.append("## ").append(result.scenarioName()).append(System.lineSeparator()).append(System.lineSeparator());
        builder.append(result.description()).append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- Expected pressure: ").append(result.expectedPressure()).append(System.lineSeparator());
        builder.append("- Replay event order: ").append(String.join(", ", result.replayEventOrder()))
                .append(System.lineSeparator());
        builder.append("- Baseline allocations: ").append(formatAllocations(result.baselineAllocations()))
                .append(System.lineSeparator());
        builder.append("- Baseline unallocated load: ").append(formatDouble(result.baselineUnallocatedLoad()))
                .append(System.lineSeparator());
        builder.append("- LASE signals considered: ").append(String.join(", ", result.laseSignalsConsidered()))
                .append(System.lineSeparator());
        builder.append("- Shadow observation recorded: ").append(result.shadowObservationRecorded())
                .append(System.lineSeparator());
        builder.append("- Shadow recommendation: ").append(display(result.shadowRecommendedBackend()))
                .append(" / ").append(display(result.shadowRecommendedAction())).append(System.lineSeparator());
        builder.append("- Influenced allocations: ").append(formatAllocations(result.influencedAllocations()))
                .append(System.lineSeparator());
        builder.append("- Influenced unallocated load: ").append(formatDouble(result.influencedUnallocatedLoad()))
                .append(System.lineSeparator());
        builder.append("- Explanation: ").append(result.explanation()).append(System.lineSeparator());
        builder.append("- Guardrail: ").append(result.guardrailReason()).append(System.lineSeparator());
        builder.append("- Rollback reason: ").append(result.rollbackReason()).append(System.lineSeparator());
        builder.append("- Policy final decision: ").append(display(result.policyDecision().finalDecision()))
                .append(System.lineSeparator());
        builder.append("- Shadow summary: ").append(result.shadowSummary()).append(System.lineSeparator())
                .append(System.lineSeparator());
    }

    private static String formatAllocations(Map<String, Double> allocations) {
        if (allocations.isEmpty()) {
            return "{}";
        }
        return allocations.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + formatDouble(entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static String display(String value) {
        return value == null || value.isBlank() ? "none" : escape(value);
    }

    private static String escape(String value) {
        return value.replace("|", "\\|").replace(System.lineSeparator(), " ");
    }

    private static String formatDouble(double value) {
        return "%.3f".formatted(value);
    }
}
