package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

@Service
public class ScenarioReplayService {
    private static final String DEFAULT_SCENARIO_ID = "adhoc-replay";
    private static final String DEFAULT_STRATEGY = "CAPACITY_AWARE";
    private static final String STATUS_OK = "OK";

    private final AllocatorService allocatorService;

    public ScenarioReplayService(AllocatorService allocatorService) {
        this.allocatorService = allocatorService;
    }

    public ScenarioReplayResponse replay(ScenarioReplayRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        Map<String, ServerInput> serverState = initialServerState(request.servers());
        List<ScenarioReplayStepResponse> results = new ArrayList<>();
        int index = 1;
        for (ScenarioReplayStepRequest step : request.steps()) {
            if (step == null) {
                throw new IllegalArgumentException("scenario steps cannot contain null values");
            }
            results.add(replayStep(serverState, step, index));
            index++;
        }
        return new ScenarioReplayResponse(scenarioId(request.scenarioId()), true, false, List.copyOf(results));
    }

    private ScenarioReplayStepResponse replayStep(
            Map<String, ServerInput> serverState, ScenarioReplayStepRequest step, int index) {
        ScenarioReplayEventType type = ScenarioReplayEventType.from(step.type());
        return switch (type) {
            case ALLOCATE, EVALUATE, OVERLOAD -> allocationPreview(serverState, step, index, type);
            case ROUTE -> routingPreview(serverState, step, index, type);
            case MARK_UNHEALTHY -> markHealth(serverState, step, index, type, false);
            case MARK_HEALTHY -> markHealth(serverState, step, index, type, true);
        };
    }

    private ScenarioReplayStepResponse allocationPreview(
            Map<String, ServerInput> serverState,
            ScenarioReplayStepRequest step,
            int index,
            ScenarioReplayEventType type) {
        double requestedLoad = requireLoad(step.requestedLoad());
        AllocationEvaluationRequest evaluationRequest = new AllocationEvaluationRequest(
                requestedLoad,
                currentServers(serverState),
                strategyOrDefault(step.strategy()),
                step.priority(),
                step.currentInFlightRequestCount(),
                step.concurrencyLimit(),
                step.queueDepth(),
                step.observedP95LatencyMillis(),
                step.observedErrorRate());
        AllocationEvaluationResponse evaluation = allocatorService.evaluate(evaluationRequest);
        return new ScenarioReplayStepResponse(
                stepId(step, index),
                type.name(),
                STATUS_OK,
                evaluation.strategy(),
                evaluation.allocations(),
                evaluation.acceptedLoad(),
                evaluation.rejectedLoad(),
                evaluation.unallocatedLoad(),
                evaluation.recommendedAdditionalServers(),
                evaluation.scalingSimulation(),
                evaluation.loadShedding(),
                evaluation.metricsPreview(),
                null,
                List.of(),
                snapshot(serverState),
                reasonFor(type, evaluation));
    }

    private ScenarioReplayStepResponse routingPreview(
            Map<String, ServerInput> serverState,
            ScenarioReplayStepRequest step,
            int index,
            ScenarioReplayEventType type) {
        RoutingComparisonResponse routing = new RoutingComparisonService()
                .compare(new RoutingComparisonRequest(step.routingStrategies(), routingInputs(serverState, step)));
        String selectedServerId = routing.results().stream()
                .map(RoutingComparisonResultResponse::chosenServerId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return new ScenarioReplayStepResponse(
                stepId(step, index),
                type.name(),
                STATUS_OK,
                null,
                Map.of(),
                0.0,
                0.0,
                0.0,
                0,
                null,
                null,
                null,
                selectedServerId,
                routing.results(),
                snapshot(serverState),
                selectedServerId == null
                        ? "Routing preview found no healthy eligible server."
                        : "Routing preview selected " + selectedServerId + ".");
    }

    private ScenarioReplayStepResponse markHealth(
            Map<String, ServerInput> serverState,
            ScenarioReplayStepRequest step,
            int index,
            ScenarioReplayEventType type,
            boolean healthy) {
        String serverId = requireServerId(step.serverId());
        ServerInput existing = requireServer(serverState, serverId);
        serverState.put(serverId, new ServerInput(existing.id(), existing.cpuUsage(), existing.memoryUsage(),
                existing.diskUsage(), existing.capacity(), existing.weight(), healthy));
        return new ScenarioReplayStepResponse(
                stepId(step, index),
                type.name(),
                STATUS_OK,
                null,
                Map.of(),
                0.0,
                0.0,
                0.0,
                0,
                null,
                null,
                null,
                null,
                List.of(),
                snapshot(serverState),
                "Server " + serverId + " marked " + (healthy ? "healthy" : "unhealthy") + " for this replay only.");
    }

    private Map<String, ServerInput> initialServerState(List<ServerInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("servers must contain at least one server");
        }
        Map<String, ServerInput> servers = new LinkedHashMap<>();
        for (ServerInput input : inputs) {
            if (input == null) {
                throw new IllegalArgumentException("server input cannot be null");
            }
            String id = requireServerId(input.id());
            if (servers.putIfAbsent(id, input) != null) {
                throw new IllegalArgumentException("server id must be unique: " + id);
            }
        }
        return servers;
    }

    private List<ServerInput> currentServers(Map<String, ServerInput> serverState) {
        return List.copyOf(serverState.values());
    }

    private List<ScenarioServerState> snapshot(Map<String, ServerInput> serverState) {
        return serverState.values().stream()
                .map(ScenarioServerState::from)
                .toList();
    }

    private List<RoutingServerStateInput> routingInputs(
            Map<String, ServerInput> serverState, ScenarioReplayStepRequest step) {
        int queueDepth = step.queueDepth() != null ? step.queueDepth() : 0;
        return serverState.values().stream()
                .map(server -> routingInput(server, queueDepth))
                .toList();
    }

    private RoutingServerStateInput routingInput(ServerInput server, int queueDepth) {
        double averageLatencyMillis = 10.0 + (server.cpuUsage() / 10.0);
        double p95LatencyMillis = averageLatencyMillis + 20.0;
        double p99LatencyMillis = p95LatencyMillis + 40.0;
        int inFlightRequestCount = safeCeilingToInt(Math.max(0.0, server.cpuUsage()));
        double capacity = Math.max(1.0, server.capacity());
        double weight = server.weight() > 0.0 ? server.weight() : 1.0;
        return new RoutingServerStateInput(
                server.id(),
                server.healthy(),
                inFlightRequestCount,
                capacity,
                capacity,
                weight,
                averageLatencyMillis,
                p95LatencyMillis,
                p99LatencyMillis,
                server.healthy() ? 0.0 : 0.20,
                queueDepth,
                null);
    }

    private ServerInput requireServer(Map<String, ServerInput> serverState, String serverId) {
        ServerInput server = serverState.get(serverId);
        if (server == null) {
            throw new IllegalArgumentException("serverId not found in scenario state: " + serverId);
        }
        return server;
    }

    private String scenarioId(String scenarioId) {
        return scenarioId == null || scenarioId.isBlank() ? DEFAULT_SCENARIO_ID : scenarioId.trim();
    }

    private String stepId(ScenarioReplayStepRequest step, int index) {
        return step.stepId() == null || step.stepId().isBlank() ? "step-" + index : step.stepId().trim();
    }

    private String strategyOrDefault(String strategy) {
        return strategy == null || strategy.isBlank() ? DEFAULT_STRATEGY : strategy;
    }

    private String requireServerId(String serverId) {
        if (serverId == null || serverId.isBlank()) {
            throw new IllegalArgumentException("serverId is required");
        }
        return serverId.trim();
    }

    private double requireLoad(Double requestedLoad) {
        if (requestedLoad == null || !Double.isFinite(requestedLoad) || requestedLoad < 0.0) {
            throw new IllegalArgumentException("requestedLoad must be a finite non-negative number");
        }
        return requestedLoad;
    }

    private int safeCeilingToInt(double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            return 0;
        }
        double ceiling = Math.ceil(value);
        return ceiling > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) ceiling;
    }

    private String reasonFor(ScenarioReplayEventType type, AllocationEvaluationResponse evaluation) {
        return switch (type) {
            case ALLOCATE -> "Simulated allocation preview; no allocation state or cloud state was mutated.";
            case OVERLOAD -> "Simulated overload preview; " + evaluation.decisionReason();
            case EVALUATE -> evaluation.decisionReason();
            default -> "Scenario replay step completed.";
        };
    }
}
