package com.richmond423.loadbalancerpro.core;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    public static final String DETAIL_API_PATH = "/api/enterprise-lab/adaptive-routing-scenario-detail";
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

    public AdaptiveRoutingScenarioDrilldown runDrilldown() {
        List<AdaptiveRoutingScenarioDetail> details = scenarios().stream()
                .map(this::runScenarioDetail)
                .toList();
        int totalDecisions = details.stream()
                .mapToInt(AdaptiveRoutingScenarioDetail::totalDecisions)
                .sum();
        return new AdaptiveRoutingScenarioDrilldown(
                RUNNER_NAME,
                RUNNER_VERSION,
                MODE,
                true,
                GENERATED_AT,
                "fixed synthetic clock; generatedAt is not wall-clock evidence",
                DASHBOARD_PATH,
                API_PATH,
                DETAIL_API_PATH,
                details.size(),
                totalDecisions,
                details,
                LOCAL_EVIDENCE_PATHS,
                List.of(
                        "Drilldown explains deterministic local synthetic inputs and observed output distribution.",
                        "Explanation notes do not infer hidden strategy internals beyond exposed decision reasons.",
                        "The endpoint returns in-memory review data only and does not create files."),
                NOT_PROVEN_BOUNDARIES,
                SAFETY_BOUNDARIES);
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

    private AdaptiveRoutingScenarioDetail runScenarioDetail(AdaptiveRoutingScenario scenario) {
        AdaptiveRoutingScenarioResult result = runScenario(scenario);
        Map<String, RoutingDecisionExplanation> firstDecisionExplanations = firstDecisionExplanations(scenario);
        List<AdaptiveRoutingCandidateSignal> candidateSignals = scenario.candidates().stream()
                .map(this::candidateSignal)
                .toList();
        List<AdaptiveRoutingStrategyExplanation> strategyExplanations = STRATEGIES.stream()
                .map(strategy -> strategyExplanation(scenario, result, strategy, firstDecisionExplanations))
                .toList();
        return new AdaptiveRoutingScenarioDetail(
                scenario.scenarioName(),
                scenario.description(),
                scenario.mode(),
                true,
                scenario.iterations(),
                candidateSignals,
                result.totalDecisions(),
                result.selectedServerCounts(),
                strategyExplanations,
                result.warnings(),
                "Explanations are based on deterministic synthetic inputs, exposed strategy decision reasons, "
                        + "first-decision score snapshots when available, and observed selected-server distribution.",
                NOT_PROVEN_BOUNDARIES);
    }

    private Map<String, RoutingDecisionExplanation> firstDecisionExplanations(AdaptiveRoutingScenario scenario) {
        RoutingComparisonEngine engine = new RoutingComparisonEngine(deterministicRegistry(), FIXED_CLOCK);
        RoutingComparisonReport report = engine.compare(scenario.candidates(), STRATEGIES);
        Map<String, RoutingDecisionExplanation> explanations = new LinkedHashMap<>();
        for (RoutingComparisonResult result : report.results()) {
            if (result.decision().isPresent()) {
                explanations.put(result.strategyId().externalName(), result.decision().get().explanation());
            }
        }
        return explanations;
    }

    private AdaptiveRoutingStrategyExplanation strategyExplanation(AdaptiveRoutingScenario scenario,
                                                                  AdaptiveRoutingScenarioResult result,
                                                                  RoutingStrategyId strategy,
                                                                  Map<String, RoutingDecisionExplanation> firstDecisionExplanations) {
        String strategyName = strategy.externalName();
        Map<String, Integer> selectedCounts = result.selectedServerCounts()
                .getOrDefault(strategyName, Map.of());
        int totalSelections = selectedCounts.values().stream().mapToInt(Integer::intValue).sum();
        String dominantServer = dominantSelectedServer(selectedCounts);
        RoutingDecisionExplanation firstDecision = firstDecisionExplanations.get(strategyName);

        return new AdaptiveRoutingStrategyExplanation(
                scenario.scenarioName(),
                strategyName,
                totalSelections,
                selectedCounts,
                dominantServer,
                firstDecision == null ? healthyCandidateIds(scenario) : firstDecision.candidateServersConsidered(),
                firstDecision == null ? Map.of() : firstDecision.scores(),
                observedInputSignals(scenario, strategy, dominantServer),
                explanationNotes(scenario, strategy, selectedCounts, dominantServer, firstDecision),
                cautionNotes(strategy),
                NOT_PROVEN_BOUNDARIES);
    }

    private AdaptiveRoutingCandidateSignal candidateSignal(ServerStateVector state) {
        return new AdaptiveRoutingCandidateSignal(
                state.serverId(),
                state.healthy(),
                state.inFlightRequestCount(),
                state.configuredCapacity().orElse(0.0),
                state.estimatedConcurrencyLimit().orElse(0.0),
                state.weight(),
                state.averageLatencyMillis(),
                state.p95LatencyMillis(),
                state.p99LatencyMillis(),
                state.recentErrorRate(),
                state.queueDepth().orElse(0),
                List.of(
                        state.healthy() ? "healthy candidate" : "unhealthy candidate excluded by healthy-only strategies",
                        "synthetic local input only",
                        "not production telemetry or capacity proof"));
    }

    private List<String> observedInputSignals(AdaptiveRoutingScenario scenario,
                                              RoutingStrategyId strategy,
                                              String dominantServer) {
        ServerStateVector dominant = scenario.candidates().stream()
                .filter(candidate -> candidate.serverId().equals(dominantServer))
                .findFirst()
                .orElse(null);
        List<String> signals = new ArrayList<>();
        signals.add("Healthy candidates in this scenario: " + String.join(", ", healthyCandidateIds(scenario)) + ".");
        if (dominant != null) {
            signals.add("Dominant selected server " + dominant.serverId()
                    + " visible inputs: healthy=" + dominant.healthy()
                    + ", inFlight=" + dominant.inFlightRequestCount()
                    + ", weight=" + format(dominant.weight())
                    + ", averageLatencyMillis=" + format(dominant.averageLatencyMillis())
                    + ", p95LatencyMillis=" + format(dominant.p95LatencyMillis())
                    + ", p99LatencyMillis=" + format(dominant.p99LatencyMillis())
                    + ", recentErrorRate=" + format(dominant.recentErrorRate())
                    + ", queueDepth=" + dominant.queueDepth().orElse(0) + ".");
        }
        signals.add(switch (strategy) {
            case TAIL_LATENCY_POWER_OF_TWO ->
                    "Tail-latency power-of-two uses seeded candidate sampling plus visible score inputs; "
                            + "lower exposed score favors selection for the sampled pair.";
            case WEIGHTED_LEAST_LOAD ->
                    "Weighted least-load uses visible load, queue, latency, tail, error, capacity, and weight inputs; "
                            + "lower weighted pressure favors selection.";
            case WEIGHTED_LEAST_CONNECTIONS ->
                    "Weighted least-connections uses visible in-flight count divided by effective weight; "
                            + "lower weighted connection pressure favors selection.";
            case WEIGHTED_ROUND_ROBIN ->
                    "Weighted round-robin uses visible effective routing weights across healthy candidates; "
                            + "it does not claim latency or load causality.";
            case ROUND_ROBIN ->
                    "Round-robin uses healthy candidate order and cursor position; it does not claim load, latency, "
                            + "or weight causality.";
        });
        return signals;
    }

    private List<String> explanationNotes(AdaptiveRoutingScenario scenario,
                                          RoutingStrategyId strategy,
                                          Map<String, Integer> selectedCounts,
                                          String dominantServer,
                                          RoutingDecisionExplanation firstDecision) {
        int totalSelections = selectedCounts.values().stream().mapToInt(Integer::intValue).sum();
        List<String> notes = new ArrayList<>();
        notes.add("Observed distribution across " + scenario.iterations() + " deterministic iterations: "
                + distributionText(selectedCounts) + ".");
        notes.add("Dominant selected server: " + dominantServer + " with "
                + selectedCounts.getOrDefault(dominantServer, 0) + " of " + totalSelections + " selections.");
        if (firstDecision != null) {
            notes.add("First deterministic decision reason: " + firstDecision.reason());
            if (!firstDecision.scores().isEmpty()) {
                notes.add("First deterministic decision score snapshot: "
                        + scoreText(firstDecision.scores()) + ".");
            }
        }
        notes.add("Explanation is based on synthetic inputs and observed output distribution, not guaranteed hidden "
                + "internal causality.");
        notes.add(switch (strategy) {
            case TAIL_LATENCY_POWER_OF_TWO ->
                    "Seeded power-of-two sampling can favor different healthy candidates across iterations while still "
                            + "using exposed lower-score comparison for each sampled pair.";
            case WEIGHTED_LEAST_LOAD ->
                    "Weighted least-load output should be read against visible weighted pressure inputs.";
            case WEIGHTED_LEAST_CONNECTIONS ->
                    "Weighted least-connections output should be read against visible in-flight and weight inputs.";
            case WEIGHTED_ROUND_ROBIN ->
                    "Weighted round-robin output should be read as smooth distribution by routing weight.";
            case ROUND_ROBIN ->
                    "Round-robin output should be read as deterministic rotation through healthy candidates.";
        });
        return notes;
    }

    private List<String> cautionNotes(RoutingStrategyId strategy) {
        List<String> notes = new ArrayList<>();
        notes.add("Local synthetic explanation only; not production benchmark or live traffic validation.");
        notes.add("No tenant, IdP, cloud, registry, signing, release, or GitHub governance proof is implied.");
        if (strategy == RoutingStrategyId.TAIL_LATENCY_POWER_OF_TWO) {
            notes.add("Power-of-two candidate sampling is deterministic here because the runner uses a fixed seed.");
        }
        return notes;
    }

    private String dominantSelectedServer(Map<String, Integer> selectedCounts) {
        return selectedCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .max(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse("none");
    }

    private List<String> healthyCandidateIds(AdaptiveRoutingScenario scenario) {
        return scenario.candidates().stream()
                .filter(ServerStateVector::healthy)
                .map(ServerStateVector::serverId)
                .toList();
    }

    private String distributionText(Map<String, Integer> selectedCounts) {
        if (selectedCounts.isEmpty()) {
            return "no selected servers";
        }
        return selectedCounts.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("no selected servers");
    }

    private String scoreText(Map<String, Double> scores) {
        return scores.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + format(entry.getValue()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
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
