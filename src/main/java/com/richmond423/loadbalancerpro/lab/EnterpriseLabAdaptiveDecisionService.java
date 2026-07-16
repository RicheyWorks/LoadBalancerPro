package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentScenario;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentServer;
import com.richmond423.loadbalancerpro.core.AdaptiveTrafficDecisionCandidate;
import com.richmond423.loadbalancerpro.core.AdaptiveTrafficDecisionOrchestrator;
import com.richmond423.loadbalancerpro.core.AdaptiveTrafficDecisionPolicy;
import com.richmond423.loadbalancerpro.core.AdaptiveTrafficDecisionRecord;
import com.richmond423.loadbalancerpro.core.AdaptiveTrafficDecisionRequest;
import com.richmond423.loadbalancerpro.core.ServerObservation;
import com.richmond423.loadbalancerpro.core.ServerObservationSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.TreeMap;

public final class EnterpriseLabAdaptiveDecisionService {
    public static final String CONTRACT_VERSION = "enterprise-lab-adaptive-decision/v1";
    private static final Clock DEFAULT_CLOCK = Clock.fixed(
            Instant.parse("2026-05-14T00:00:00Z"), ZoneOffset.UTC);
    private static final double[] LATENCY_MULTIPLIERS = {0.80, 0.90, 1.00, 1.10, 1.50};
    private static final List<String> SAFETY_NOTES = List.of(
            "Decision inputs come only from bounded deterministic Enterprise Lab fixtures.",
            "Off and observe modes expose signal state and scores without an allocation recommendation.",
            "Shadow and recommend modes retain the baseline allocation.",
            "Active-experiment output requires explicit opt-in and remains decision data only.",
            "No proxy, CloudManager, external network, telemetry export, or traffic action is invoked.");

    private final EnterpriseLabScenarioCatalogService scenarioCatalog;
    private final AdaptiveTrafficDecisionOrchestrator orchestrator;
    private final AdaptiveTrafficDecisionPolicy policy;
    private final Clock clock;

    public EnterpriseLabAdaptiveDecisionService() {
        this(
                new EnterpriseLabScenarioCatalogService(),
                new AdaptiveTrafficDecisionOrchestrator(),
                AdaptiveTrafficDecisionPolicy.localLabDefaults(),
                DEFAULT_CLOCK);
    }

    EnterpriseLabAdaptiveDecisionService(
            EnterpriseLabScenarioCatalogService scenarioCatalog,
            AdaptiveTrafficDecisionOrchestrator orchestrator,
            AdaptiveTrafficDecisionPolicy policy,
            Clock clock) {
        this.scenarioCatalog = Objects.requireNonNull(scenarioCatalog, "scenarioCatalog cannot be null");
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator cannot be null");
        this.policy = Objects.requireNonNull(policy, "policy cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    public EnterpriseLabAdaptiveDecision decide(
            String scenarioId,
            String modeValue,
            boolean explicitExperimentContext,
            boolean cooldownActive,
            boolean operatorStopRequested) {
        AdaptiveRoutingExperimentScenario scenario = scenarioCatalog.findScenario(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown enterprise lab scenario: " + normalizeScenarioId(scenarioId)));
        EnterpriseLabMode mode = EnterpriseLabMode.from(modeValue);
        Instant evaluatedAt = clock.instant();
        String recoveringServerId = recoveringServerId(scenario);
        List<AdaptiveTrafficDecisionCandidate> candidates = scenario.servers().stream()
                .map(server -> candidate(scenario, server, recoveringServerId, evaluatedAt))
                .toList();
        AdaptiveTrafficDecisionRequest request = new AdaptiveTrafficDecisionRequest(
                "lab-decision:" + scenario.name() + ":" + mode.wireValue(),
                "enterprise-lab:" + scenario.name(),
                mode.policyMode(),
                candidates,
                baselineAllocations(scenario.servers()),
                evaluatedAt,
                scenario.expectedPressure().toLowerCase(Locale.ROOT).contains("conflicting"),
                cooldownActive,
                explicitExperimentContext,
                operatorStopRequested);
        AdaptiveTrafficDecisionRecord decision = orchestrator.decide(request, policy);
        return new EnterpriseLabAdaptiveDecision(
                CONTRACT_VERSION,
                scenario.name(),
                EnterpriseLabScenarioCatalogService.FIXTURE_VERSION,
                "five synthetic local samples per backend derived from fixed fixture pressure and health",
                decision,
                decision.contentFingerprint(),
                decision.trafficActionPerformed(),
                SAFETY_NOTES);
    }

    private static AdaptiveTrafficDecisionCandidate candidate(
            AdaptiveRoutingExperimentScenario scenario,
            AdaptiveRoutingExperimentServer server,
            String recoveringServerId,
            Instant evaluatedAt) {
        long inFlight = Math.round(server.capacity() * server.cpuUsage() / 100.0);
        int boundedInFlight = (int) Math.min(Integer.MAX_VALUE, inFlight);
        int queueDepth = (int) Math.round(server.memoryUsage() / 5.0);
        OptionalDouble concurrencyLimit = server.capacity() > 0.0
                ? OptionalDouble.of(server.capacity())
                : OptionalDouble.empty();
        return new AdaptiveTrafficDecisionCandidate(
                server.id(),
                server.healthy(),
                server.capacity(),
                server.weight(),
                boundedInFlight,
                concurrencyLimit,
                OptionalInt.of(queueDepth),
                observations(scenario, server, recoveringServerId, evaluatedAt));
    }

    private static List<ServerObservation> observations(
            AdaptiveRoutingExperimentScenario scenario,
            AdaptiveRoutingExperimentServer server,
            String recoveringServerId,
            Instant evaluatedAt) {
        List<ServerObservation> observations = new ArrayList<>();
        boolean stale = !scenario.signalsFresh();
        boolean recovering = server.id().equals(recoveringServerId);
        double baseLatencyMillis = 10.0 + (3.0 * maximumPressure(server));
        for (int index = 0; index < LATENCY_MULTIPLIERS.length; index++) {
            long ageSeconds = stale ? 60L - index : 5L - index;
            Instant observedAt = evaluatedAt.minusSeconds(ageSeconds);
            String observationId = scenario.name() + ":" + server.id() + ":" + (index + 1);
            if (!server.healthy() || (recovering && index < 3)) {
                observations.add(ServerObservation.failure(
                        observationId, server.id(), ServerObservationSource.ENTERPRISE_LAB, observedAt));
            } else {
                observations.add(ServerObservation.success(
                        observationId,
                        server.id(),
                        ServerObservationSource.ENTERPRISE_LAB,
                        baseLatencyMillis * LATENCY_MULTIPLIERS[index],
                        observedAt));
            }
        }
        return List.copyOf(observations);
    }

    private static Map<String, Double> baselineAllocations(List<AdaptiveRoutingExperimentServer> servers) {
        Map<String, Double> weights = new TreeMap<>();
        double totalWeight = 0.0;
        for (AdaptiveRoutingExperimentServer server : servers) {
            double weight = server.healthy() ? server.capacity() * server.weight() : 0.0;
            weights.put(server.id(), weight);
            totalWeight += weight;
        }
        if (totalWeight == 0.0) {
            double uniform = 1.0 / servers.size();
            weights.replaceAll((serverId, weight) -> uniform);
        } else {
            double divisor = totalWeight;
            weights.replaceAll((serverId, weight) -> weight / divisor);
        }
        correctResidual(weights);
        return Collections.unmodifiableMap(new LinkedHashMap<>(weights));
    }

    private static void correctResidual(Map<String, Double> allocations) {
        double residual = 1.0 - allocations.values().stream().mapToDouble(Double::doubleValue).sum();
        if (residual == 0.0) {
            return;
        }
        String serverId = allocations.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.0)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
        allocations.put(serverId, allocations.get(serverId) + residual);
    }

    private static String recoveringServerId(AdaptiveRoutingExperimentScenario scenario) {
        if (!scenario.expectedPressure().toLowerCase(Locale.ROOT).contains("recovery")) {
            return "";
        }
        return scenario.servers().stream()
                .filter(AdaptiveRoutingExperimentServer::healthy)
                .min(Comparator.comparingDouble(EnterpriseLabAdaptiveDecisionService::maximumPressure)
                        .thenComparing(AdaptiveRoutingExperimentServer::id))
                .map(AdaptiveRoutingExperimentServer::id)
                .orElse("");
    }

    private static double maximumPressure(AdaptiveRoutingExperimentServer server) {
        return Math.max(server.cpuUsage(), Math.max(server.memoryUsage(), server.diskUsage()));
    }

    private static String normalizeScenarioId(String scenarioId) {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId cannot be null or blank");
        }
        return scenarioId.trim().toLowerCase(Locale.ROOT);
    }
}
