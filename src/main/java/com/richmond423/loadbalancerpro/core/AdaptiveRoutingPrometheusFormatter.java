package com.richmond423.loadbalancerpro.core;

import java.util.Map;

public final class AdaptiveRoutingPrometheusFormatter {
    private AdaptiveRoutingPrometheusFormatter() {
    }

    public static String format(AdaptiveRoutingObservabilitySnapshot snapshot) {
        StringBuilder builder = new StringBuilder();
        appendCounter(builder, "loadbalancerpro_lab_runs_total",
                "Enterprise Lab runs created in this process.", snapshot.labRunsCreated());
        appendCounter(builder, "loadbalancerpro_lab_scenarios_executed_total",
                "Enterprise Lab deterministic scenarios executed in this process.", snapshot.labScenariosExecuted());
        appendCounter(builder, "loadbalancerpro_lab_explanations_total",
                "Enterprise Lab result explanations generated in this process.", snapshot.explanationCoverageCount());
        appendCounter(builder, "loadbalancerpro_lase_policy_recommendations_total",
                "Controlled LASE policy recommendations produced in this process.",
                snapshot.recommendationsProduced());
        appendCounter(builder, "loadbalancerpro_lase_policy_active_changes_total",
                "Controlled active-experiment changes allowed in lab/evaluation contexts.",
                snapshot.activeExperimentChangesAllowed());
        appendCounter(builder, "loadbalancerpro_lase_policy_rollback_total",
                "Controlled LASE rollback or fail-closed events in this process.",
                snapshot.rollbackFailClosedEvents());
        appendCounter(builder, "loadbalancerpro_lase_policy_audit_events_retained_total",
                "Controlled LASE audit events retained in bounded process-local memory.",
                snapshot.auditEventsRetained());
        appendCounter(builder, "loadbalancerpro_lase_policy_audit_events_dropped_total",
                "Controlled LASE audit events dropped by retention bounds.",
                snapshot.auditEventsDropped());
        appendLabeledCounter(builder, "loadbalancerpro_lase_policy_decisions_total",
                "Controlled LASE policy decisions by mode.", "mode", snapshot.policyDecisionsByMode());
        appendLabeledCounter(builder, "loadbalancerpro_lase_policy_guardrail_blocks_total",
                "Controlled LASE guardrail blocks by normalized reason.", "reason",
                snapshot.guardrailBlocksByReason());
        appendLabeledCounter(builder, "loadbalancerpro_api_rate_limit_events_total",
                "API rate-limit events by surface.", "surface", snapshot.rateLimitEventsBySurface());
        builder.append("# HELP loadbalancerpro_lab_observability_info Lab-grade process-local observability posture.\n");
        builder.append("# TYPE loadbalancerpro_lab_observability_info gauge\n");
        builder.append("loadbalancerpro_lab_observability_info{scope=\"lab_grade_process_local\"} 1\n");
        return builder.toString();
    }

    private static void appendCounter(StringBuilder builder, String metric, String help, long value) {
        builder.append("# HELP ").append(metric).append(' ').append(help).append('\n');
        builder.append("# TYPE ").append(metric).append(" counter\n");
        builder.append(metric).append(' ').append(value).append('\n');
    }

    private static void appendLabeledCounter(
            StringBuilder builder, String metric, String help, String labelName, Map<String, Long> values) {
        builder.append("# HELP ").append(metric).append(' ').append(help).append('\n');
        builder.append("# TYPE ").append(metric).append(" counter\n");
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> builder.append(metric)
                        .append('{').append(labelName).append("=\"").append(entry.getKey()).append("\"} ")
                        .append(entry.getValue())
                        .append('\n'));
    }
}
