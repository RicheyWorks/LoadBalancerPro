package com.richmond423.loadbalancerpro.api;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.richmond423.loadbalancerpro.api.config.AdaptiveRoutingPolicyProperties;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyAuditLog;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyDecision;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyEngine;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyInput;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.core.DomainMetrics;
import com.richmond423.loadbalancerpro.core.LaseEvaluationReport;
import com.richmond423.loadbalancerpro.core.LoadDistributionEvaluator;
import com.richmond423.loadbalancerpro.core.LoadBalancer;
import com.richmond423.loadbalancerpro.core.LoadDistributionResult;
import com.richmond423.loadbalancerpro.core.LoadSheddingConfig;
import com.richmond423.loadbalancerpro.core.LoadSheddingDecision;
import com.richmond423.loadbalancerpro.core.LoadSheddingPolicy;
import com.richmond423.loadbalancerpro.core.LoadSheddingSignal;
import com.richmond423.loadbalancerpro.core.LaseShadowAdvisor;
import com.richmond423.loadbalancerpro.core.LaseShadowEventLog;
import com.richmond423.loadbalancerpro.core.LaseShadowObservabilitySnapshot;
import com.richmond423.loadbalancerpro.core.RequestPriority;
import com.richmond423.loadbalancerpro.core.ScalingRecommendation;
import com.richmond423.loadbalancerpro.core.Server;
import com.richmond423.loadbalancerpro.core.ServerType;

@Service
public class AllocatorService {
    private static final String LASE_SHADOW_PROPERTY = "loadbalancerpro.lase.shadow.enabled";
    private static final String LASE_SHADOW_ENVIRONMENT_VARIABLE = "LOADBALANCERPRO_LASE_SHADOW_ENABLED";
    private static final String STRATEGY_CAPACITY_AWARE = "CAPACITY_AWARE";
    private static final String STRATEGY_PREDICTIVE = "PREDICTIVE";
    private static final String EVALUATION_TARGET_ID = "allocation-evaluation";
    private static final LoadSheddingConfig DEFAULT_LOAD_SHEDDING_CONFIG =
            new LoadSheddingConfig(0.75, 0.90, 20, 250.0, 0.10, true, true);
    private static final List<String> EVALUATION_METRIC_NAMES = List.of(
            DomainMetrics.ALLOCATION_REQUESTS,
            DomainMetrics.ALLOCATION_ACCEPTED_LOAD,
            DomainMetrics.ALLOCATION_REJECTED_LOAD,
            DomainMetrics.ALLOCATION_UNALLOCATED_LOAD,
            DomainMetrics.ALLOCATION_SERVER_COUNT,
            DomainMetrics.ALLOCATION_SCALING_RECOMMENDED_SERVERS);

    private final boolean laseShadowEnabled;
    private final LaseShadowEventLog laseShadowEventLog = new LaseShadowEventLog();
    private final LoadDistributionEvaluator loadDistributionEvaluator = new LoadDistributionEvaluator();
    private final LoadSheddingPolicy loadSheddingPolicy = new LoadSheddingPolicy();
    private final OperatorRemediationPlanner remediationPlanner = new OperatorRemediationPlanner();
    private final AdaptiveRoutingPolicyProperties policyProperties;
    private final AdaptiveRoutingPolicyAuditLog policyAuditLog;
    private final AdaptiveRoutingPolicyEngine policyEngine = new AdaptiveRoutingPolicyEngine();

    public AllocatorService(Environment environment) {
        this(environment, new AdaptiveRoutingPolicyProperties(), new AdaptiveRoutingPolicyAuditLog());
    }

    @Autowired
    public AllocatorService(Environment environment,
                            AdaptiveRoutingPolicyProperties policyProperties,
                            AdaptiveRoutingPolicyAuditLog policyAuditLog) {
        this.laseShadowEnabled = resolveLaseShadowEnabled(environment);
        this.policyProperties = policyProperties == null ? new AdaptiveRoutingPolicyProperties() : policyProperties;
        this.policyAuditLog = policyAuditLog == null ? new AdaptiveRoutingPolicyAuditLog() : policyAuditLog;
    }

    public AllocationResponse capacityAware(AllocationRequest request) {
        return allocate(request, true);
    }

    public AllocationResponse predictive(AllocationRequest request) {
        return allocate(request, false);
    }

    public AllocationEvaluationResponse evaluate(AllocationEvaluationRequest request) {
        validateEvaluationRequest(request);
        String strategy = resolveStrategy(request.strategy());
        List<Server> servers = request.servers().stream()
                .map(AllocatorService::toServer)
                .toList();
        LoadDistributionResult result = STRATEGY_PREDICTIVE.equals(strategy)
                ? loadDistributionEvaluator.predictive(servers, request.requestedLoad())
                : loadDistributionEvaluator.capacityAware(servers, request.requestedLoad());
        double acceptedLoad = totalAllocated(result.allocations());
        double rejectedLoad = result.unallocatedLoad();
        ScalingRecommendation recommendation = ScalingRecommendation.forUnallocatedLoad(
                rejectedLoad, averageHealthyCapacity(request.servers()));
        ScalingSimulationResult simulation = simulateScaling(rejectedLoad, recommendation);
        LoadSheddingDecision loadSheddingDecision = loadSheddingPolicy.decide(
                resolvePriority(request.priority()),
                signalFor(request, acceptedLoad, rejectedLoad),
                DEFAULT_LOAD_SHEDDING_CONFIG);
        LoadSheddingEvaluation loadShedding = toLoadSheddingEvaluation(loadSheddingDecision);
        AdaptiveRoutingPolicyMode policyMode = policyProperties.resolvedMode();
        LaseObservation laseObservation = observeLase(strategy, servers, request.requestedLoad(), result,
                laseShadowEnabled || policyMode != AdaptiveRoutingPolicyMode.OFF);
        LaseAllocationShadowSummary laseShadow = laseObservation.toSummary(laseShadowEnabled
                || policyMode != AdaptiveRoutingPolicyMode.OFF);
        AdaptiveRoutingPolicyDecision lasePolicy = policyEngine.decide(new AdaptiveRoutingPolicyInput(
                "allocation-evaluation",
                policyMode,
                servers,
                request.requestedLoad(),
                selectedBackend(result.allocations()),
                laseObservation.recommendedServerId(),
                laseObservation.recommendedAction(),
                laseObservation.report().isPresent(),
                true,
                false,
                policyMode == AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT
                        && policyProperties.isActiveExperimentEnabled(),
                laseObservation.summary(),
                laseObservation.failureReason()));
        policyAuditLog.record(lasePolicy);
        AllocationEvaluationMetricsPreview metricsPreview = new AllocationEvaluationMetricsPreview(
                strategy,
                healthyServerCount(request.servers()),
                acceptedLoad,
                rejectedLoad,
                rejectedLoad,
                recommendation.additionalServers(),
                EVALUATION_METRIC_NAMES,
                false);
        return new AllocationEvaluationResponse(
                strategy,
                result.allocations(),
                acceptedLoad,
                rejectedLoad,
                rejectedLoad,
                recommendation.additionalServers(),
                simulation,
                loadShedding,
                metricsPreview,
                laseShadow,
                lasePolicy,
                true,
                remediationPlanner.planForEvaluation(
                        acceptedLoad,
                        rejectedLoad,
                        rejectedLoad,
                        recommendation.additionalServers(),
                        loadShedding,
                        request.servers()),
                decisionReason(acceptedLoad, rejectedLoad, loadSheddingDecision));
    }

    public LaseShadowObservabilitySnapshot laseShadowObservability() {
        return laseShadowEventLog.snapshot();
    }

    private AllocationResponse allocate(AllocationRequest request, boolean capacityAware) {
        validateRequest(request);
        LoadBalancer balancer = createLoadBalancer();
        try {
            String strategy = capacityAware ? "CAPACITY_AWARE" : "PREDICTIVE";
            for (ServerInput input : request.servers()) {
                balancer.addServer(toServer(input));
            }
            LoadDistributionResult result = capacityAware
                    ? balancer.capacityAwareWithResult(request.requestedLoad())
                    : balancer.predictiveLoadBalancingWithResult(request.requestedLoad());
            ScalingRecommendation recommendation = balancer.recommendScaling(
                    result.unallocatedLoad(), averageHealthyCapacity(request.servers()));
            DomainMetrics.recordAllocationScalingRecommendation(strategy, recommendation.additionalServers());
            ScalingSimulationResult simulation = simulateScaling(result.unallocatedLoad(), recommendation);
            return new AllocationResponse(
                    result.allocations(),
                    result.unallocatedLoad(),
                    recommendation.additionalServers(),
                    simulation);
        } finally {
            balancer.shutdown();
        }
    }

    static boolean resolveLaseShadowEnabled(Environment environment) {
        if (environment == null) {
            return false;
        }
        String configured = environment.getProperty(LASE_SHADOW_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = environment.getProperty(LASE_SHADOW_ENVIRONMENT_VARIABLE);
        }
        return Boolean.parseBoolean(configured);
    }

    boolean isLaseShadowEnabledForTesting() {
        return laseShadowEnabled;
    }

    private LoadBalancer createLoadBalancer() {
        return new LoadBalancer(laseShadowEnabled, laseShadowEventLog);
    }

    private LaseObservation observeLase(
            String strategy, List<Server> servers, double requestedLoad, LoadDistributionResult result,
            boolean enabled) {
        if (!enabled) {
            return LaseObservation.disabled();
        }
        try {
            return LaseObservation.observed(new LaseShadowAdvisor(true, laseShadowEventLog)
                    .observe(strategy, servers, requestedLoad, result));
        } catch (RuntimeException exception) {
            return LaseObservation.failed("LASE shadow observation failed safely.");
        }
    }

    private static ScalingSimulationResult simulateScaling(
            double unallocatedLoad, ScalingRecommendation recommendation) {
        String reason;
        if (recommendation.additionalServers() > 0) {
            reason = "Unallocated load exceeds available capacity; simulated scale-up recommended.";
        } else if (unallocatedLoad > 0.0) {
            reason = "Unallocated load exists, but target capacity is unavailable; no scale-up count simulated.";
        } else {
            reason = "No unallocated load requiring scale-up.";
        }
        return new ScalingSimulationResult(recommendation.additionalServers(), reason, true);
    }

    private static void validateRequest(AllocationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.requestedLoad() == null || request.requestedLoad() < 0
                || !Double.isFinite(request.requestedLoad())) {
            throw new IllegalArgumentException("requestedLoad must be a finite non-negative number");
        }
        if (request.servers() == null || request.servers().isEmpty()) {
            throw new IllegalArgumentException("servers must contain at least one server");
        }
    }

    private static void validateEvaluationRequest(AllocationEvaluationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.requestedLoad() == null || request.requestedLoad() < 0
                || !Double.isFinite(request.requestedLoad())) {
            throw new IllegalArgumentException("requestedLoad must be a finite non-negative number");
        }
        if (request.servers() == null || request.servers().isEmpty()) {
            throw new IllegalArgumentException("servers must contain at least one server");
        }
    }

    private static Server toServer(ServerInput input) {
        if (input == null) {
            throw new IllegalArgumentException("server input cannot be null");
        }
        Server server = new Server(
                input.id(), input.cpuUsage(), input.memoryUsage(), input.diskUsage(), ServerType.ONSITE);
        server.setCapacity(input.capacity());
        server.setWeight(input.weight());
        server.setHealthy(input.healthy());
        return server;
    }

    private static double averageHealthyCapacity(List<ServerInput> servers) {
        return servers.stream()
                .filter(AllocatorService::isHealthy)
                .mapToDouble(server -> server.capacity())
                .average()
                .orElse(0.0);
    }

    private static double totalAllocated(Map<String, Double> allocations) {
        return allocations.values().stream()
                .mapToDouble(value -> Math.max(0.0, value))
                .sum();
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

    private static int healthyServerCount(List<ServerInput> servers) {
        return (int) servers.stream()
                .filter(AllocatorService::isHealthy)
                .count();
    }

    private static double totalHealthyCapacity(List<ServerInput> servers) {
        return servers.stream()
                .filter(AllocatorService::isHealthy)
                .mapToDouble(server -> server.capacity())
                .sum();
    }

    private static boolean isHealthy(ServerInput server) {
        return Boolean.TRUE.equals(server.healthy());
    }

    private static String resolveStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return STRATEGY_CAPACITY_AWARE;
        }
        String normalized = strategy.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case STRATEGY_CAPACITY_AWARE, STRATEGY_PREDICTIVE -> normalized;
            default -> throw new IllegalArgumentException(
                    "strategy must be one of CAPACITY_AWARE or PREDICTIVE");
        };
    }

    private static RequestPriority resolvePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return RequestPriority.USER;
        }
        String normalized = priority.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return RequestPriority.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "priority must be one of CRITICAL, USER, BACKGROUND, or PREFETCH");
        }
    }

    private static LoadSheddingSignal signalFor(
            AllocationEvaluationRequest request, double acceptedLoad, double rejectedLoad) {
        int defaultConcurrencyLimit = Math.max(1, safeCeilingToInt(totalHealthyCapacity(request.servers())));
        int concurrencyLimit = request.concurrencyLimit() != null
                ? request.concurrencyLimit()
                : defaultConcurrencyLimit;
        int currentInFlightRequestCount = request.currentInFlightRequestCount() != null
                ? request.currentInFlightRequestCount()
                : Math.min(concurrencyLimit, safeCeilingToInt(acceptedLoad + rejectedLoad));
        int queueDepth = request.queueDepth() != null
                ? request.queueDepth()
                : safeCeilingToInt(rejectedLoad);
        double observedP95LatencyMillis = request.observedP95LatencyMillis() != null
                ? request.observedP95LatencyMillis()
                : 0.0;
        double observedErrorRate = request.observedErrorRate() != null
                ? request.observedErrorRate()
                : 0.0;
        return new LoadSheddingSignal(EVALUATION_TARGET_ID, currentInFlightRequestCount, concurrencyLimit,
                queueDepth, observedP95LatencyMillis, observedErrorRate, Instant.EPOCH);
    }

    private static int safeCeilingToInt(double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            return 0;
        }
        double ceiling = Math.ceil(value);
        return ceiling > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) ceiling;
    }

    private static LoadSheddingEvaluation toLoadSheddingEvaluation(LoadSheddingDecision decision) {
        return new LoadSheddingEvaluation(
                decision.priority().name(),
                decision.action().name(),
                decision.reason(),
                decision.targetId(),
                decision.currentInFlightRequestCount(),
                decision.concurrencyLimit(),
                decision.queueDepth(),
                decision.utilization(),
                decision.observedP95LatencyMillis(),
                decision.observedErrorRate());
    }

    private static String decisionReason(
            double acceptedLoad, double rejectedLoad, LoadSheddingDecision loadSheddingDecision) {
        if (rejectedLoad > 0.0) {
            return "Read-only evaluation accepted %.3f load and left %.3f load unallocated; %s"
                    .formatted(acceptedLoad, rejectedLoad, loadSheddingDecision.reason());
        }
        return "Read-only evaluation accepted the requested load; " + loadSheddingDecision.reason();
    }

    private record LaseObservation(Optional<LaseEvaluationReport> report, String failureReason) {
        private static LaseObservation disabled() {
            return new LaseObservation(Optional.empty(), null);
        }

        private static LaseObservation observed(Optional<LaseEvaluationReport> report) {
            return new LaseObservation(report == null ? Optional.empty() : report, null);
        }

        private static LaseObservation failed(String reason) {
            return new LaseObservation(Optional.empty(), reason);
        }

        private LaseAllocationShadowSummary toSummary(boolean enabled) {
            if (!enabled) {
                return LaseAllocationShadowSummary.disabled();
            }
            if (failureReason != null) {
                return LaseAllocationShadowSummary.failSafe(failureReason);
            }
            return report.map(LaseAllocationShadowSummary::observed)
                    .orElseGet(() -> LaseAllocationShadowSummary.failSafe(
                            "LASE shadow observation did not produce a report."));
        }

        private String recommendedServerId() {
            return report.flatMap(value -> value.routingDecision().chosenServer()
                    .map(server -> server.serverId())
                    .or(() -> value.routingDecision().explanation().chosenServerId()))
                    .orElse(null);
        }

        private String recommendedAction() {
            return report.map(value -> value.autoscalingRecommendation().action().name())
                    .orElse(null);
        }

        private String summary() {
            return report.map(LaseEvaluationReport::summary).orElse(null);
        }
    }
}
