package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AdaptiveRoutingStrategyComparisonMatrixBuilder {
    public static final String MATRIX_NAME = "Adaptive Routing Strategy Comparison Matrix";
    public static final String MATRIX_VERSION = "adaptive-routing-strategy-comparison-matrix/v1";
    public static final String API_PATH = "/api/enterprise-lab/adaptive-routing-strategy-comparison-matrix";
    public static final String LOCAL_MATRIX_PATH =
            "target/adaptive-routing-scenarios/adaptive-routing-strategy-comparison-matrix.json";

    private final AdaptiveRoutingScenarioRunner runner;

    public AdaptiveRoutingStrategyComparisonMatrixBuilder() {
        this(new AdaptiveRoutingScenarioRunner());
    }

    public AdaptiveRoutingStrategyComparisonMatrixBuilder(AdaptiveRoutingScenarioRunner runner) {
        this.runner = Objects.requireNonNull(runner, "runner cannot be null");
    }

    public AdaptiveRoutingStrategyComparisonMatrix build() {
        AdaptiveRoutingScenarioSummary summary = runner.runSummary();
        AdaptiveRoutingScenarioDrilldown drilldown = runner.runDrilldown();
        List<AdaptiveRoutingStrategyComparisonRow> rows = rows(summary, drilldown);

        return new AdaptiveRoutingStrategyComparisonMatrix(
                MATRIX_NAME,
                MATRIX_VERSION,
                summary.mode(),
                summary.deterministic() && drilldown.deterministic(),
                summary.generatedAt(),
                summary.generatedAtPolicy(),
                summary.dashboardPath(),
                API_PATH,
                List.of(
                        summary.apiPath(),
                        drilldown.detailApiPath(),
                        API_PATH),
                summary.scenarioCount(),
                summary.strategiesCompared().size(),
                summary.totalDecisions(),
                rows,
                insights(summary, rows),
                warnings(),
                reviewerActions(),
                localEvidencePaths(summary),
                summary.notProvenBoundaries(),
                summary.safetyBoundaries());
    }

    private List<AdaptiveRoutingStrategyComparisonRow> rows(
            AdaptiveRoutingScenarioSummary summary,
            AdaptiveRoutingScenarioDrilldown drilldown) {
        List<AdaptiveRoutingStrategyComparisonRow> rows = new ArrayList<>();
        for (String strategy : summary.strategiesCompared()) {
            List<AdaptiveRoutingStrategyScenarioCell> cells = drilldown.scenarios().stream()
                    .map(scenario -> cell(strategy, scenario))
                    .toList();
            int rowTotalDecisions = cells.stream()
                    .mapToInt(AdaptiveRoutingStrategyScenarioCell::decisionCount)
                    .sum();
            rows.add(new AdaptiveRoutingStrategyComparisonRow(
                    strategy,
                    cells.size(),
                    rowTotalDecisions,
                    cells,
                    rowWarnings(strategy, cells),
                    List.of(
                            "Compare " + strategy + " distributions across scenarios before changing strategy behavior.",
                            "Treat matrix signals as local synthetic output-distribution evidence, not production performance proof.")));
        }
        return rows;
    }

    private AdaptiveRoutingStrategyScenarioCell cell(
            String strategy,
            AdaptiveRoutingScenarioDetail scenario) {
        AdaptiveRoutingStrategyExplanation explanation = scenario.strategyExplanations().stream()
                .filter(candidate -> strategy.equals(candidate.strategyName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing strategy explanation for " + strategy + " in " + scenario.scenarioName()));
        Map<String, Integer> distribution = explanation.selectedServerCounts();
        int decisionCount = distribution.values().stream().mapToInt(Integer::intValue).sum();
        int distinctSelectedServers = (int) distribution.values().stream()
                .filter(count -> count > 0)
                .count();
        String dominantServer = dominantSelectedServer(distribution);

        return new AdaptiveRoutingStrategyScenarioCell(
                scenario.scenarioName(),
                strategy,
                decisionCount,
                distribution,
                dominantServer,
                distinctSelectedServers,
                diversitySignal(distribution, decisionCount),
                consistencySignal(distribution, decisionCount),
                explanationNotes(explanation),
                warnings(scenario, explanation),
                List.of(
                        "Review " + strategy + " in " + scenario.scenarioName()
                                + " against selected-server distribution and explanation notes.",
                        "Read this cell as inferred from output distribution and visible synthetic inputs, not hidden causality."),
                scenario.notProvenBoundaries());
    }

    private List<String> explanationNotes(AdaptiveRoutingStrategyExplanation explanation) {
        List<String> notes = new ArrayList<>();
        notes.add("Matrix signal is inferred from selected-server output distribution for local synthetic review.");
        explanation.explanationNotes().stream()
                .limit(3)
                .forEach(notes::add);
        notes.add("Do not read the diversity/consistency label as production benchmark data or guaranteed hidden causality.");
        return List.copyOf(notes);
    }

    private List<String> warnings(
            AdaptiveRoutingScenarioDetail scenario,
            AdaptiveRoutingStrategyExplanation explanation) {
        Set<String> warnings = new LinkedHashSet<>();
        warnings.addAll(scenario.warnings());
        warnings.addAll(explanation.cautionNotes());
        warnings.add("Comparison cell is local synthetic reviewer evidence only.");
        return List.copyOf(warnings);
    }

    private List<String> rowWarnings(
            String strategy,
            List<AdaptiveRoutingStrategyScenarioCell> cells) {
        Set<String> warnings = new LinkedHashSet<>();
        warnings.add("Strategy row for " + strategy + " is computed in-memory and does not write evidence files.");
        warnings.add("Diversity/consistency labels are inferred from output distribution only.");
        for (AdaptiveRoutingStrategyScenarioCell cell : cells) {
            warnings.addAll(cell.warnings());
        }
        return List.copyOf(warnings);
    }

    private List<AdaptiveRoutingStrategyComparisonInsight> insights(
            AdaptiveRoutingScenarioSummary summary,
            List<AdaptiveRoutingStrategyComparisonRow> rows) {
        return List.of(
                new AdaptiveRoutingStrategyComparisonInsight(
                        "matrix-coverage",
                        "Matrix covers every local scenario and strategy",
                        API_PATH,
                        "The matrix contains " + summary.scenarioCount() + " scenarios, "
                                + summary.strategiesCompared().size() + " strategies, and "
                                + summary.totalDecisions() + " selected decisions.",
                        "Use this as cross-strategy review evidence only after confirming local/prototype boundaries."),
                new AdaptiveRoutingStrategyComparisonInsight(
                        "distribution-signals",
                        "Diversity and consistency signals are review labels",
                        API_PATH,
                        "Each cell labels the selected-server distribution as single-server, dominant, distributed, "
                                + "or no-selection based on deterministic output counts.",
                        "Do not turn those labels into throughput, latency, SLO/SLA, or live traffic claims."),
                new AdaptiveRoutingStrategyComparisonInsight(
                        "row-review",
                        "Strategy rows show where reviewer attention should go",
                        API_PATH,
                        rows.stream().map(AdaptiveRoutingStrategyComparisonRow::strategyName)
                                .reduce((left, right) -> left + ", " + right)
                                .orElse("No strategies") + " can be reviewed across the same synthetic scenarios.",
                        "Compare row-level changes before accepting adaptive-routing behavior changes."));
    }

    private List<String> warnings() {
        return List.of(
                "Comparison matrix is deterministic in-memory endpoint output only; it does not write files.",
                "Diversity and consistency signals are inferred from selected-server output distribution, not live traffic.",
                "The matrix is local synthetic evidence and not a production benchmark or active CI enforcement result.");
    }

    private List<String> reviewerActions() {
        return List.of(
                "Compare each strategy row across all three local synthetic scenarios.",
                "Inspect cells where a strategy has distributed or dominant-with-secondary selected-server behavior.",
                "Use explanation notes as local review guidance, not guaranteed hidden strategy causality.",
                "Keep not-proven boundaries attached before citing the matrix in reviewer or future CI-gate notes.");
    }

    private List<String> localEvidencePaths(AdaptiveRoutingScenarioSummary summary) {
        Set<String> paths = new LinkedHashSet<>(summary.localEvidencePaths());
        paths.add(LOCAL_MATRIX_PATH);
        return List.copyOf(paths);
    }

    private String dominantSelectedServer(Map<String, Integer> selectedCounts) {
        return selectedCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .max(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse("none");
    }

    private String diversitySignal(Map<String, Integer> selectedCounts, int decisionCount) {
        if (decisionCount == 0) {
            return "NO_SELECTION";
        }
        long distinct = selectedCounts.values().stream()
                .filter(count -> count > 0)
                .count();
        if (distinct <= 1) {
            return "SINGLE_SERVER_CONSISTENT";
        }
        int dominantCount = selectedCounts.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        if (dominantCount * 4 >= decisionCount * 3) {
            return "DOMINANT_WITH_SECONDARY_SELECTIONS";
        }
        return "DISTRIBUTED_SELECTIONS";
    }

    private String consistencySignal(Map<String, Integer> selectedCounts, int decisionCount) {
        if (decisionCount == 0) {
            return "no selected-server output in this local synthetic cell";
        }
        int dominantCount = selectedCounts.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        if (dominantCount == decisionCount) {
            return "high consistency: the same server was selected for every local synthetic decision";
        }
        if (dominantCount * 2 >= decisionCount) {
            return "mixed consistency: one server dominated but secondary selections appeared";
        }
        return "distributed consistency: selections were spread across multiple healthy candidates";
    }
}
