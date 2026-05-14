package com.richmond423.loadbalancerpro.core;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public final class AdaptiveRoutingExperimentService {
    public static final String MODE_SHADOW = "shadow";
    public static final String MODE_INFLUENCE = "active-experiment";
    private static final Clock EXPERIMENT_CLOCK = Clock.fixed(
            Instant.parse("2026-05-14T00:00:00Z"), ZoneOffset.UTC);
    private static final long ROUTING_SEED = 23L;
    private static final List<String> SIGNALS_CONSIDERED = List.of(
            "tail latency",
            "queue depth",
            "error rate",
            "adaptive concurrency",
            "load shedding",
            "shadow autoscaling",
            "failure scenario");
    private static final List<String> SAFETY_NOTES = List.of(
            "Experiment harness is deterministic, local, and read-only.",
            "Default mode is shadow-only and does not alter live allocation behavior.",
            "Influence mode is explicit opt-in and only changes experiment output.",
            "No CloudManager, cloud credentials, external network, release, tag, asset, or container action is used.");

    private final AdaptiveRoutingExperimentFixtureCatalog fixtureCatalog;
    private final LoadDistributionEvaluator loadDistributionEvaluator;
    private final AdaptiveRoutingPolicyEngine policyEngine;

    public AdaptiveRoutingExperimentService() {
        this(new AdaptiveRoutingExperimentFixtureCatalog(), new LoadDistributionEvaluator(),
                new AdaptiveRoutingPolicyEngine());
    }

    AdaptiveRoutingExperimentService(
            AdaptiveRoutingExperimentFixtureCatalog fixtureCatalog,
            LoadDistributionEvaluator loadDistributionEvaluator,
            AdaptiveRoutingPolicyEngine policyEngine) {
        this.fixtureCatalog = Objects.requireNonNull(fixtureCatalog, "fixtureCatalog cannot be null");
        this.loadDistributionEvaluator = Objects.requireNonNull(loadDistributionEvaluator,
                "loadDistributionEvaluator cannot be null");
        this.policyEngine = Objects.requireNonNull(policyEngine, "policyEngine cannot be null");
    }

    public AdaptiveRoutingExperimentReport runCatalog(boolean activeInfluenceEnabled) {
        AdaptiveRoutingPolicyMode mode = activeInfluenceEnabled
                ? AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT
                : AdaptiveRoutingPolicyMode.SHADOW;
        return runCatalog(mode);
    }

    public AdaptiveRoutingExperimentReport runCatalog(AdaptiveRoutingPolicyMode mode) {
        List<AdaptiveRoutingExperimentResult> results = fixtureCatalog.createAll().stream()
                .map(scenario -> evaluate(scenario, mode))
                .toList();
        return new AdaptiveRoutingExperimentReport(
                mode.wireValue(),
                mode.activeInfluenceAllowed(),
                results,
                SAFETY_NOTES);
    }

    public AdaptiveRoutingExperimentResult evaluate(
            AdaptiveRoutingExperimentScenario scenario,
            boolean activeInfluenceEnabled) {
        return evaluate(scenario, activeInfluenceEnabled
                ? AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT
                : AdaptiveRoutingPolicyMode.SHADOW);
    }

    public AdaptiveRoutingExperimentResult evaluate(
            AdaptiveRoutingExperimentScenario scenario,
            AdaptiveRoutingPolicyMode mode) {
        Objects.requireNonNull(scenario, "scenario cannot be null");
        AdaptiveRoutingPolicyMode safeMode = mode == null ? AdaptiveRoutingPolicyMode.OFF : mode;
        List<Server> servers = scenario.servers().stream()
                .map(AdaptiveRoutingExperimentService::toServer)
                .toList();
        LoadDistributionResult baseline = baseline(scenario, servers);
        Optional<LaseEvaluationReport> shadowReport = deterministicShadowAdvisor()
                .observe(scenario.strategy(), servers, scenario.requestedLoad(), baseline);
        String recommendedBackend = shadowReport.flatMap(this::recommendedServerId).orElse(null);
        String recommendedAction = shadowReport
                .map(report -> report.autoscalingRecommendation().action().name())
                .orElse("NO_RECOMMENDATION");
        String shadowSummary = shadowReport
                .map(LaseEvaluationReport::summary)
                .orElse("LASE shadow did not produce a recommendation for this scenario.");
        AdaptiveRoutingPolicyDecision policyDecision = policyEngine.decide(new AdaptiveRoutingPolicyInput(
                scenario.name(),
                safeMode,
                servers,
                scenario.requestedLoad(),
                selectedBackend(baseline.allocations()),
                recommendedBackend,
                recommendedAction,
                shadowReport.isPresent(),
                scenario.signalsFresh(),
                isConflictingSignal(scenario),
                safeMode == AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT,
                shadowSummary,
                null));
        InfluenceResult influence = influencedResult(scenario, servers, baseline, policyDecision);

        return new AdaptiveRoutingExperimentResult(
                scenario.name(),
                scenario.description(),
                scenario.expectedPressure(),
                scenario.replayEventOrder(),
                selectedBackend(baseline.allocations()),
                baseline.allocations(),
                baseline.unallocatedLoad(),
                SIGNALS_CONSIDERED,
                shadowReport.isPresent(),
                recommendedBackend,
                recommendedAction,
                shadowSummary,
                safeMode.activeInfluenceAllowed(),
                selectedBackend(influence.allocations()),
                influence.allocations(),
                influence.unallocatedLoad(),
                influence.changed(),
                influence.explanation(),
                influence.guardrailReason(),
                policyDecision.rollbackReason(),
                policyDecision);
    }

    private LoadDistributionResult baseline(AdaptiveRoutingExperimentScenario scenario, List<Server> servers) {
        if ("PREDICTIVE".equals(scenario.strategy())) {
            return loadDistributionEvaluator.predictive(servers, scenario.requestedLoad());
        }
        return loadDistributionEvaluator.capacityAware(servers, scenario.requestedLoad());
    }

    private InfluenceResult influencedResult(
            AdaptiveRoutingExperimentScenario scenario,
            List<Server> servers,
            LoadDistributionResult baseline,
            AdaptiveRoutingPolicyDecision policyDecision) {
        if (!policyDecision.changed() || !policyDecision.influenceAllowed()) {
            return unchanged(baseline, policyDecision.explanationSummary(),
                    String.join("; ", policyDecision.guardrailReasons()));
        }
        String recommendedServerId = policyDecision.recommendedDecision();
        Optional<Server> recommendedServer = servers.stream()
                .filter(Server::isHealthy)
                .filter(server -> recommendedServerId.equals(server.getServerId()))
                .findFirst();
        if (recommendedServer.isEmpty()) {
            return unchanged(baseline,
                    "Active experiment failed closed after policy allowed influence but backend lookup failed.",
                    "recommended backend unavailable");
        }

        LoadDistributionResult influenced = preferRecommendedBackend(
                servers, scenario.requestedLoad(), recommendedServer.get());
        boolean changed = !baseline.allocations().equals(influenced.allocations())
                || Double.compare(baseline.unallocatedLoad(), influenced.unallocatedLoad()) != 0;
        String explanation = changed
                ? "Opt-in influence preferred LASE-recommended backend %s for experiment output only."
                        .formatted(recommendedServerId)
                : "Opt-in influence produced the same allocation as baseline.";
        String guardrail = changed
                ? "active-experiment policy gates passed; lab evidence only"
                : "influence matched baseline";
        return new InfluenceResult(influenced.allocations(), influenced.unallocatedLoad(), changed, explanation, guardrail);
    }

    private InfluenceResult unchanged(LoadDistributionResult baseline, String explanation, String guardrailReason) {
        return new InfluenceResult(baseline.allocations(), baseline.unallocatedLoad(), false, explanation, guardrailReason);
    }

    private LoadDistributionResult preferRecommendedBackend(
            List<Server> servers,
            double requestedLoad,
            Server recommendedServer) {
        Map<String, Double> allocations = new LinkedHashMap<>();
        servers.forEach(server -> allocations.put(server.getServerId(), 0.0));
        double remaining = requestedLoad;
        double recommendedAllocation = Math.min(remaining, Math.max(0.0, recommendedServer.getCapacity()));
        allocations.put(recommendedServer.getServerId(), recommendedAllocation);
        remaining -= recommendedAllocation;

        List<Server> otherHealthyServers = servers.stream()
                .filter(Server::isHealthy)
                .filter(server -> !recommendedServer.getServerId().equals(server.getServerId()))
                .sorted(Comparator.comparing(Server::getServerId))
                .toList();
        for (Server server : otherHealthyServers) {
            if (remaining <= 0.0) {
                break;
            }
            double allocation = Math.min(remaining, Math.max(0.0, server.getCapacity()));
            allocations.put(server.getServerId(), allocation);
            remaining -= allocation;
        }
        return new LoadDistributionResult(allocations, Math.max(0.0, remaining));
    }

    private Optional<String> recommendedServerId(LaseEvaluationReport report) {
        return report.routingDecision().chosenServer()
                .map(ServerStateVector::serverId)
                .or(() -> report.routingDecision().explanation().chosenServerId());
    }

    private static boolean isConflictingSignal(AdaptiveRoutingExperimentScenario scenario) {
        return scenario.expectedPressure().toLowerCase(Locale.ROOT).contains("conflicting");
    }

    private static String selectedBackend(Map<String, Double> allocations) {
        return allocations.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0.0)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private static LaseShadowAdvisor deterministicShadowAdvisor() {
        return new LaseShadowAdvisor(true, new LaseEvaluationEngine(
                new TailLatencyPowerOfTwoStrategy(new ServerScoreCalculator(), new Random(ROUTING_SEED),
                        EXPERIMENT_CLOCK),
                new LoadSheddingPolicy(),
                new ShadowAutoscaler(),
                new FailureScenarioRunner(),
                EXPERIMENT_CLOCK), EXPERIMENT_CLOCK);
    }

    private static Server toServer(AdaptiveRoutingExperimentServer input) {
        Server server = new Server(input.id(), input.cpuUsage(), input.memoryUsage(), input.diskUsage(),
                ServerType.ONSITE);
        server.setCapacity(input.capacity());
        server.setWeight(input.weight());
        server.setHealthy(input.healthy());
        return server;
    }

    private record InfluenceResult(
            Map<String, Double> allocations,
            double unallocatedLoad,
            boolean changed,
            String explanation,
            String guardrailReason) {
    }
}
