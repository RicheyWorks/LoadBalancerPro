package com.richmond423.loadbalancerpro.core;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;

public final class AdaptiveRoutingScenarioRunner {
    public static final String RUNNER_NAME = "Adaptive Routing Scenario Runner";
    public static final String RUNNER_VERSION = "adaptive-routing-scenario-runner/v1";
    public static final String MODE = "local-synthetic";
    public static final String DASHBOARD_PATH = "/adaptive-routing-scenarios.html";
    public static final String API_PATH = "/api/enterprise-lab/adaptive-routing-scenario-summary";
    private static final Instant GENERATED_AT = Instant.parse("2026-05-17T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(GENERATED_AT, ZoneOffset.UTC);
    private static final long ROUTING_SEED = 41L;
    private static final List<RoutingStrategyId> STRATEGIES = List.of(
            RoutingStrategyId.TAIL_LATENCY_POWER_OF_TWO,
            RoutingStrategyId.WEIGHTED_LEAST_LOAD,
            RoutingStrategyId.WEIGHTED_LEAST_CONNECTIONS,
            RoutingStrategyId.WEIGHTED_ROUND_ROBIN,
            RoutingStrategyId.ROUND_ROBIN);
    private static final List<String> LOCAL_EVIDENCE_PATHS = List.of(
            "target/adaptive-routing-scenarios/adaptive-routing-scenario-summary.json");
    private static final List<String> NOT_PROVEN_BOUNDARIES = List.of(
            "No production certification",
            "No production benchmark or machine-speed claim",
            "No production SLO/SLA proof",
            "No live traffic validation",
            "No live-cloud validation",
            "No real tenant validation",
            "No real enterprise IdP validation",
            "No signed-container proof",
            "No registry publish completion",
            "No GitHub governance-applied proof");
    private static final List<String> SAFETY_BOUNDARIES = List.of(
            "in-memory synthetic inputs only",
            "read-only endpoint output",
            "no file reads or writes",
            "no process execution",
            "no environment variable or secret reads",
            "no external network calls",
            "no filesystem mutation",
            "no live CI inspection");

    public AdaptiveRoutingScenarioSummary runSummary() {
        List<AdaptiveRoutingScenarioResult> results = scenarios().stream()
                .map(this::runScenario)
                .toList();
        int totalDecisions = results.stream()
                .mapToInt(AdaptiveRoutingScenarioResult::totalDecisions)
                .sum();
        return new AdaptiveRoutingScenarioSummary(
                RUNNER_NAME,
                RUNNER_VERSION,
                MODE,
                true,
                GENERATED_AT,
                "fixed synthetic clock; generatedAt is not wall-clock evidence",
                DASHBOARD_PATH,
                API_PATH,
                results.size(),
                strategyNames(),
                totalDecisions,
                results,
                LOCAL_EVIDENCE_PATHS,
                List.of(
                        "Results are deterministic local measurements of routing decisions, not throughput or latency benchmarks.",
                        "The endpoint returns the evidence shape only and does not create files.",
                        "Future CI gate consumption should parse an approved artifact contract before enforcement."),
                NOT_PROVEN_BOUNDARIES,
                SAFETY_BOUNDARIES,
                List.of(
                        "Review selected-server distributions before changing adaptive-routing behavior.",
                        "Use the summary as a future CI evidence input after schema and failure policy approval.",
                        "Keep generated exports, if added later, under ignored target/adaptive-routing-scenarios/."));
    }

    public List<AdaptiveRoutingScenario> scenarios() {
        return List.of(
                new AdaptiveRoutingScenario(
                        "balanced-weighted-local-synthetic",
                        "Compares how strategies distribute choices across healthy weighted local candidates.",
                        MODE,
                        8,
                        List.of(
                                state("edge-a", true, 8, 120.0, 100.0, 3.0,
                                        22.0, 38.0, 62.0, 0.01, 2),
                                state("edge-b", true, 12, 120.0, 100.0, 2.0,
                                        24.0, 42.0, 68.0, 0.01, 3),
                                state("edge-c", true, 16, 120.0, 100.0, 1.0,
                                        26.0, 48.0, 80.0, 0.02, 4)),
                        List.of("healthy weighted candidates", "stable local decision distribution")),
                new AdaptiveRoutingScenario(
                        "tail-latency-degradation-local-synthetic",
                        "Keeps one healthy candidate visibly degraded so latency-aware strategies can avoid it.",
                        MODE,
                        6,
                        List.of(
                                state("stable-a", true, 6, 100.0, 90.0, 2.0,
                                        18.0, 34.0, 55.0, 0.01, 1),
                                state("stable-b", true, 9, 100.0, 90.0, 1.5,
                                        20.0, 36.0, 58.0, 0.02, 2),
                                state("tail-risk", true, 5, 100.0, 90.0, 1.0,
                                        48.0, 180.0, 320.0, 0.16, 9)),
                        List.of("tail latency pressure", "error-rate pressure", "local-only synthetic signal")),
                new AdaptiveRoutingScenario(
                        "capacity-pressure-local-synthetic",
                        "Models a local capacity-pressure case with one unhealthy backend and two viable choices.",
                        MODE,
                        6,
                        List.of(
                                state("capacity-a", true, 20, 160.0, 140.0, 2.0,
                                        30.0, 55.0, 85.0, 0.02, 4),
                                state("capacity-b", true, 5, 80.0, 70.0, 1.0,
                                        24.0, 46.0, 76.0, 0.03, 2),
                                state("capacity-c-unhealthy", false, 1, 120.0, 100.0, 1.0,
                                        20.0, 40.0, 70.0, 0.01, 1)),
                        List.of("capacity pressure", "unhealthy candidate excluded", "fail-closed review boundary")));
    }

    private AdaptiveRoutingScenarioResult runScenario(AdaptiveRoutingScenario scenario) {
        RoutingComparisonEngine engine = new RoutingComparisonEngine(deterministicRegistry(), FIXED_CLOCK);
        Map<String, Map<String, Integer>> selectedServerCounts = emptySelectedServerCounts();
        Map<String, Integer> noSelectionCounts = emptyNoSelectionCounts();
        Map<String, String> firstDecisionNotes = new LinkedHashMap<>();
        int selectedDecisionCount = 0;

        for (int iteration = 0; iteration < scenario.iterations(); iteration++) {
            RoutingComparisonReport report = engine.compare(scenario.candidates(), STRATEGIES);
            for (RoutingComparisonResult result : report.results()) {
                String strategy = result.strategyId().externalName();
                firstDecisionNotes.putIfAbsent(strategy, result.reason());
                if (result.successful() && result.decision().isPresent()
                        && result.decision().get().chosenServer().isPresent()) {
                    String selectedServerId = result.decision().get().chosenServer().get().serverId();
                    selectedServerCounts.get(strategy).merge(selectedServerId, 1, Integer::sum);
                    selectedDecisionCount++;
                } else {
                    noSelectionCounts.merge(strategy, 1, Integer::sum);
                }
            }
        }

        int attemptedDecisions = scenario.iterations() * STRATEGIES.size();
        AdaptiveRoutingMeasurementSummary measurementSummary = new AdaptiveRoutingMeasurementSummary(
                RUNNER_VERSION,
                "deterministic selected-server count summary",
                GENERATED_AT,
                "fixed synthetic clock; no wall-clock duration or throughput measurement",
                scenario.iterations(),
                scenario.candidates().size(),
                healthyCandidateCount(scenario),
                STRATEGIES.size(),
                attemptedDecisions,
                selectedDecisionCount,
                attemptedDecisions - selectedDecisionCount,
                List.of(
                        "Counts are produced by repeated in-memory strategy comparison over synthetic inputs.",
                        "No sleeps, timing loops, network calls, or machine-speed measurements are used."));

        List<String> warnings = new ArrayList<>();
        warnings.add("Local synthetic measurement only; not a production benchmark.");
        warnings.add("Selection counts show deterministic strategy behavior, not live traffic validation.");
        if (measurementSummary.noSelectionDecisionCount() > 0) {
            warnings.add("One or more strategies returned no selected server and should be reviewed as fail-closed output.");
        }

        return new AdaptiveRoutingScenarioResult(
                scenario.scenarioName(),
                scenario.mode(),
                true,
                strategyNames(),
                selectedDecisionCount,
                selectedServerCounts,
                noSelectionCounts,
                firstDecisionNotes.entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .toList(),
                warnings,
                "READY_FOR_LOCAL_CI_GATE_REVIEW",
                LOCAL_EVIDENCE_PATHS,
                NOT_PROVEN_BOUNDARIES,
                measurementSummary);
    }

    private static RoutingStrategyRegistry deterministicRegistry() {
        return new RoutingStrategyRegistry(List.of(
                new TailLatencyPowerOfTwoStrategy(new ServerScoreCalculator(), new Random(ROUTING_SEED), FIXED_CLOCK),
                new WeightedLeastLoadStrategy(FIXED_CLOCK),
                new WeightedLeastConnectionsRoutingStrategy(FIXED_CLOCK),
                new WeightedRoundRobinRoutingStrategy(FIXED_CLOCK),
                new RoundRobinRoutingStrategy(FIXED_CLOCK)));
    }

    private static List<String> strategyNames() {
        return STRATEGIES.stream()
                .map(RoutingStrategyId::externalName)
                .toList();
    }

    private static Map<String, Map<String, Integer>> emptySelectedServerCounts() {
        Map<String, Map<String, Integer>> counts = new LinkedHashMap<>();
        for (RoutingStrategyId strategy : STRATEGIES) {
            counts.put(strategy.externalName(), new LinkedHashMap<>());
        }
        return counts;
    }

    private static Map<String, Integer> emptyNoSelectionCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (RoutingStrategyId strategy : STRATEGIES) {
            counts.put(strategy.externalName(), 0);
        }
        return counts;
    }

    private static int healthyCandidateCount(AdaptiveRoutingScenario scenario) {
        return (int) scenario.candidates().stream()
                .filter(ServerStateVector::healthy)
                .count();
    }

    private static ServerStateVector state(String serverId,
                                           boolean healthy,
                                           int inFlightRequestCount,
                                           double configuredCapacity,
                                           double estimatedConcurrencyLimit,
                                           double weight,
                                           double averageLatencyMillis,
                                           double p95LatencyMillis,
                                           double p99LatencyMillis,
                                           double recentErrorRate,
                                           int queueDepth) {
        return new ServerStateVector(
                serverId,
                healthy,
                inFlightRequestCount,
                OptionalDouble.of(configuredCapacity),
                OptionalDouble.of(estimatedConcurrencyLimit),
                weight,
                averageLatencyMillis,
                p95LatencyMillis,
                p99LatencyMillis,
                recentErrorRate,
                OptionalInt.of(queueDepth),
                GENERATED_AT);
    }
}
