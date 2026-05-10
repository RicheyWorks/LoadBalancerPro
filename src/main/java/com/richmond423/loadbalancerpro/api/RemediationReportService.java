package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;

@Service
public class RemediationReportService {
    private static final String SOURCE_EVALUATION = "EVALUATION";
    private static final String SOURCE_SCENARIO_REPLAY = "SCENARIO_REPLAY";
    private static final String DEFAULT_REPORT_ID = "not-supplied";
    private static final String DEFAULT_TITLE = "LoadBalancerPro Remediation Report";

    public RemediationReportResponse export(RemediationReportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        RemediationReportPayload payload = payload(request);
        RemediationReportFormat format = request.format() == null
                ? RemediationReportFormat.MARKDOWN
                : request.format();
        String report = format == RemediationReportFormat.MARKDOWN ? renderMarkdown(payload) : null;
        return new RemediationReportResponse(
                format,
                format == RemediationReportFormat.MARKDOWN ? "text/markdown" : "application/json",
                report,
                payload,
                payload.readOnly(),
                payload.advisoryOnly(),
                payload.cloudMutation());
    }

    private RemediationReportPayload payload(RemediationReportRequest request) {
        boolean hasEvaluation = request.evaluation() != null;
        boolean hasReplay = request.replay() != null;
        if (hasEvaluation == hasReplay) {
            throw new IllegalArgumentException("Exactly one of evaluation or replay report source is required");
        }
        String reportId = sanitize(request.reportId(), DEFAULT_REPORT_ID);
        String title = sanitize(request.title(), DEFAULT_TITLE);
        return hasEvaluation
                ? evaluationPayload(reportId, title, request.evaluation())
                : replayPayload(reportId, title, request.replay());
    }

    private RemediationReportPayload evaluationPayload(
            String reportId, String title, AllocationEvaluationResponse evaluation) {
        RemediationPlan plan = requirePlan(evaluation.remediationPlan());
        double acceptedLoad = finiteOrZero(evaluation.acceptedLoad());
        double rejectedLoad = finiteOrZero(evaluation.rejectedLoad());
        double unallocatedLoad = finiteOrZero(evaluation.unallocatedLoad());
        boolean cloudMutation = plan.cloudMutation();
        return new RemediationReportPayload(
                reportId,
                title,
                SOURCE_EVALUATION,
                safeText(plan.status(), "UNKNOWN"),
                evaluationSummary(acceptedLoad, rejectedLoad, unallocatedLoad),
                acceptedLoad,
                rejectedLoad,
                unallocatedLoad,
                Math.max(0, evaluation.recommendedAdditionalServers()),
                evaluation.scalingSimulation(),
                evaluation.loadShedding(),
                plan,
                evaluation.readOnly() && plan.readOnly(),
                plan.advisoryOnly(),
                cloudMutation,
                warnings(plan.status(), rejectedLoad, unallocatedLoad, evaluation.readOnly(), cloudMutation),
                limitations(),
                List.of());
    }

    private RemediationReportPayload replayPayload(String reportId, String title, ScenarioReplayResponse replay) {
        RemediationPlan plan = requirePlan(replay.remediationPlan());
        List<ScenarioReplayStepResponse> steps = replay.steps() == null ? List.of() : replay.steps();
        double acceptedLoad = steps.stream().mapToDouble(step -> finiteOrZero(step.acceptedLoad())).sum();
        double rejectedLoad = steps.stream().mapToDouble(step -> finiteOrZero(step.rejectedLoad())).sum();
        double unallocatedLoad = steps.stream().mapToDouble(step -> finiteOrZero(step.unallocatedLoad())).sum();
        int recommendedAdditionalServers = steps.stream()
                .mapToInt(step -> Math.max(0, step.recommendedAdditionalServers()))
                .max()
                .orElse(0);
        boolean cloudMutation = replay.cloudMutation() || plan.cloudMutation();
        return new RemediationReportPayload(
                reportId,
                title,
                SOURCE_SCENARIO_REPLAY,
                safeText(plan.status(), "UNKNOWN"),
                replaySummary(replay.scenarioId(), acceptedLoad, unallocatedLoad, steps.size()),
                acceptedLoad,
                rejectedLoad,
                unallocatedLoad,
                recommendedAdditionalServers,
                firstScalingSimulation(steps),
                firstLoadShedding(steps),
                plan,
                replay.readOnly() && plan.readOnly(),
                plan.advisoryOnly(),
                cloudMutation,
                warnings(plan.status(), rejectedLoad, unallocatedLoad, replay.readOnly(), cloudMutation),
                limitations(),
                reportSteps(steps));
    }

    private String renderMarkdown(RemediationReportPayload payload) {
        StringBuilder report = new StringBuilder();
        report.append("# ").append(payload.title()).append("\n\n");
        report.append("Report ID: ").append(payload.reportId()).append("\n");
        report.append("Source: ").append(payload.sourceType()).append("\n");
        report.append("Status: ").append(payload.status()).append("\n\n");

        report.append("## Summary\n");
        report.append(payload.summary()).append("\n\n");

        report.append("## Load Impact\n");
        report.append("- Accepted load: ").append(formatLoad(payload.acceptedLoad())).append("\n");
        report.append("- Rejected load: ").append(formatLoad(payload.rejectedLoad())).append("\n");
        report.append("- Unallocated load: ").append(formatLoad(payload.unallocatedLoad())).append("\n\n");

        report.append("## Scaling Recommendation\n");
        report.append("- Additional servers: ").append(payload.recommendedAdditionalServers()).append("\n");
        report.append("- Simulated only: ").append(simulatedOnly(payload.scalingSimulation())).append("\n");
        report.append("- Reason: ").append(scalingReason(payload.scalingSimulation())).append("\n\n");

        report.append("## Load-Shedding Decision\n");
        if (payload.loadShedding() == null) {
            report.append("- Decision: not supplied\n\n");
        } else {
            report.append("- Priority: ").append(safeText(payload.loadShedding().priority(), "UNKNOWN")).append("\n");
            report.append("- Action: ").append(safeText(payload.loadShedding().action(), "UNKNOWN")).append("\n");
            report.append("- Reason: ").append(safeText(payload.loadShedding().reason(), "not supplied"))
                    .append("\n\n");
        }

        appendSteps(report, payload.steps());
        appendRecommendations(report, payload.remediationPlan());
        appendSafety(report, payload);
        appendNotes(report, payload);
        return report.toString();
    }

    private void appendSteps(StringBuilder report, List<RemediationReportStep> steps) {
        if (steps.isEmpty()) {
            return;
        }
        report.append("## Replay Steps\n");
        for (RemediationReportStep step : steps) {
            report.append("- ").append(step.stepId()).append(" [").append(step.type()).append("]");
            report.append(": accepted=").append(formatLoad(step.acceptedLoad()));
            report.append(", rejected=").append(formatLoad(step.rejectedLoad()));
            report.append(", unallocated=").append(formatLoad(step.unallocatedLoad()));
            if (step.selectedServerId() != null) {
                report.append(", selected=").append(step.selectedServerId());
            }
            if (step.loadSheddingAction() != null) {
                report.append(", loadShedding=").append(step.loadSheddingAction());
            }
            report.append("\n");
        }
        report.append("\n");
    }

    private void appendRecommendations(StringBuilder report, RemediationPlan plan) {
        report.append("## Ranked Remediation Actions\n");
        List<RemediationRecommendation> recommendations = sortedRecommendations(plan);
        if (recommendations.isEmpty()) {
            report.append("No remediation recommendations were supplied.\n\n");
            return;
        }
        for (RemediationRecommendation recommendation : recommendations) {
            report.append(recommendation.rank()).append(". [").append(recommendation.priority()).append("] ");
            report.append(recommendation.action()).append(" (").append(recommendation.reason()).append(")");
            report.append(" - executable=").append(recommendation.executable());
            if (recommendation.serverCount() != null) {
                report.append(", serverCount=").append(recommendation.serverCount());
            }
            if (recommendation.loadAmount() != null) {
                report.append(", loadAmount=").append(formatLoad(recommendation.loadAmount()));
            }
            report.append("\n");
            report.append("   ").append(safeText(recommendation.message(), "No operator message supplied."))
                    .append("\n");
        }
        report.append("\n");
    }

    private void appendSafety(StringBuilder report, RemediationReportPayload payload) {
        report.append("## Safety Guarantees\n");
        report.append("- Read only: ").append(payload.readOnly()).append("\n");
        report.append("- Advisory only: ").append(payload.advisoryOnly()).append("\n");
        report.append("- Cloud mutation: ").append(payload.cloudMutation()).append("\n");
        report.append("- Executable remediation: false\n\n");
    }

    private void appendNotes(StringBuilder report, RemediationReportPayload payload) {
        report.append("## Notes / Limitations\n");
        for (String warning : payload.warnings()) {
            report.append("- Warning: ").append(warning).append("\n");
        }
        for (String limitation : payload.limitations()) {
            report.append("- ").append(limitation).append("\n");
        }
    }

    private static RemediationPlan requirePlan(RemediationPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("remediationPlan is required for report export");
        }
        return plan;
    }

    private static List<RemediationReportStep> reportSteps(List<ScenarioReplayStepResponse> steps) {
        return steps.stream()
                .map(step -> new RemediationReportStep(
                        safeText(step.stepId(), "step"),
                        safeText(step.type(), "UNKNOWN"),
                        safeText(step.status(), "UNKNOWN"),
                        finiteOrZero(step.acceptedLoad()),
                        finiteOrZero(step.rejectedLoad()),
                        finiteOrZero(step.unallocatedLoad()),
                        Math.max(0, step.recommendedAdditionalServers()),
                        blankToNull(step.selectedServerId()),
                        step.loadShedding() == null ? null : blankToNull(step.loadShedding().action()),
                        blankToNull(step.reason())))
                .toList();
    }

    private static ScalingSimulationResult firstScalingSimulation(List<ScenarioReplayStepResponse> steps) {
        return steps.stream()
                .map(ScenarioReplayStepResponse::scalingSimulation)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(ScalingSimulationResult::recommendedAdditionalServers))
                .orElse(null);
    }

    private static LoadSheddingEvaluation firstLoadShedding(List<ScenarioReplayStepResponse> steps) {
        return steps.stream()
                .map(ScenarioReplayStepResponse::loadShedding)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static List<RemediationRecommendation> sortedRecommendations(RemediationPlan plan) {
        if (plan.recommendations() == null) {
            return List.of();
        }
        return plan.recommendations().stream()
                .sorted(Comparator.comparingInt(RemediationRecommendation::rank))
                .toList();
    }

    private static String evaluationSummary(double acceptedLoad, double rejectedLoad, double unallocatedLoad) {
        if (unallocatedLoad > 0.0 || rejectedLoad > 0.0) {
            return "Read-only evaluation accepted %s load and left %s load unallocated."
                    .formatted(formatLoad(acceptedLoad), formatLoad(unallocatedLoad));
        }
        return "Read-only evaluation accepted %s load with no unallocated load."
                .formatted(formatLoad(acceptedLoad));
    }

    private static String replaySummary(String scenarioId, double acceptedLoad, double unallocatedLoad, int stepCount) {
        return "Read-only replay '%s' produced %d ordered steps, accepted %s load, and left %s load unallocated."
                .formatted(safeText(scenarioId, "adhoc-replay"), stepCount, formatLoad(acceptedLoad),
                        formatLoad(unallocatedLoad));
    }

    private static List<String> warnings(
            String status, double rejectedLoad, double unallocatedLoad, boolean readOnly, boolean cloudMutation) {
        List<String> warnings = new ArrayList<>();
        if (unallocatedLoad > 0.0 || rejectedLoad > 0.0) {
            warnings.add("Unallocated or rejected load is present; review scale and load-shedding recommendations.");
        }
        if ("NO_HEALTHY_CAPACITY".equals(status)) {
            warnings.add("No healthy capacity was available; restore capacity before retrying traffic.");
        }
        if (!readOnly) {
            warnings.add("Input source was not marked read-only; verify the upstream result before acting.");
        }
        if (cloudMutation) {
            warnings.add("Input source indicated cloud mutation; do not treat this as a dry-run report.");
        }
        return warnings;
    }

    private static List<String> limitations() {
        return List.of(
                "This report is advisory and does not execute remediation actions.",
                "No timestamp or random report identifier is generated unless supplied by the caller.",
                "Verify live deployment state, health checks, and release version before acting.");
    }

    private static boolean simulatedOnly(ScalingSimulationResult scalingSimulation) {
        return scalingSimulation == null || scalingSimulation.simulatedOnly();
    }

    private static String scalingReason(ScalingSimulationResult scalingSimulation) {
        return scalingSimulation == null
                ? "No scaling simulation supplied."
                : safeText(scalingSimulation.reason(), "No scaling reason supplied.");
    }

    private static String sanitize(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        String singleLine = text.replace('\r', ' ').replace('\n', ' ').trim();
        return singleLine.isBlank() ? fallback : singleLine;
    }

    private static String safeText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text.trim();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private static String formatLoad(double value) {
        return String.format(Locale.ROOT, "%.3f", finiteOrZero(value));
    }
}
