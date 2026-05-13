package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class OperatorRemediationPlanner {
    private static final double POSITIVE_LOAD_EPSILON = 0.000001;

    public RemediationPlan planForEvaluation(
            double acceptedLoad,
            double rejectedLoad,
            double unallocatedLoad,
            int recommendedAdditionalServers,
            LoadSheddingEvaluation loadShedding,
            List<ServerInput> servers) {
        int healthyServerCount = healthyServerCount(servers);
        int unhealthyServerCount = unhealthyServerCount(servers);
        List<RemediationRecommendation> recommendations = new ArrayList<>();
        EnumSet<RemediationAction> emitted = EnumSet.noneOf(RemediationAction.class);

        if (healthyServerCount == 0 && hasPositiveLoad(acceptedLoad + rejectedLoad + unallocatedLoad)) {
            add(recommendations, emitted, RemediationAction.RESTORE_CAPACITY, RemediationPriority.HIGH,
                    RemediationReason.NO_HEALTHY_CAPACITY, null, unallocatedLoad,
                    "Restore or replace healthy serving capacity before retrying allocation.");
            add(recommendations, emitted, RemediationAction.RETRY_WHEN_HEALTHY, RemediationPriority.MEDIUM,
                    RemediationReason.NO_HEALTHY_CAPACITY, null, unallocatedLoad,
                    "Retry the workload after at least one server is healthy.");
        } else if (hasPositiveLoad(unallocatedLoad) || hasPositiveLoad(rejectedLoad)) {
            if (recommendedAdditionalServers > 0) {
                add(recommendations, emitted, RemediationAction.SCALE_UP, RemediationPriority.HIGH,
                        RemediationReason.CAPACITY_DEFICIT, recommendedAdditionalServers,
                        Math.max(unallocatedLoad, rejectedLoad),
                        "Scale up by the simulated additional server count to cover unallocated load.");
            }
            if (isLoadShed(loadShedding)) {
                add(recommendations, emitted, RemediationAction.SHED_LOAD, RemediationPriority.MEDIUM,
                        RemediationReason.LOW_PRIORITY_LOAD_SHED, null, Math.max(unallocatedLoad, rejectedLoad),
                        "Shed, defer, or queue low-priority work until capacity pressure clears.");
            }
        }

        if (unhealthyServerCount > 0 && healthyServerCount > 0) {
            add(recommendations, emitted, RemediationAction.INVESTIGATE_UNHEALTHY, RemediationPriority.MEDIUM,
                    RemediationReason.UNHEALTHY_SERVERS_PRESENT, unhealthyServerCount, null,
                    "Investigate unhealthy servers while healthy capacity continues serving traffic.");
        }

        if (recommendations.isEmpty()) {
            add(recommendations, emitted, RemediationAction.NO_ACTION, RemediationPriority.LOW,
                    RemediationReason.HEALTHY, null, 0.0,
                    "No operator action is recommended for the current read-only evaluation.");
        }

        return new RemediationPlan(statusFor(healthyServerCount, unhealthyServerCount, unallocatedLoad, rejectedLoad),
                "allocation-evaluation", true, true, false, recommendations);
    }

    public RemediationPlan planForReplay(List<ScenarioReplayStepResponse> steps) {
        List<ScenarioReplayStepResponse> safeSteps = steps == null ? List.of() : steps;
        ReplaySignals signals = replaySignals(safeSteps);
        List<RemediationRecommendation> recommendations = new ArrayList<>();
        EnumSet<RemediationAction> emitted = EnumSet.noneOf(RemediationAction.class);

        if (signals.everAllUnhealthyWithLoad()) {
            add(recommendations, emitted, RemediationAction.RESTORE_CAPACITY, RemediationPriority.HIGH,
                    RemediationReason.NO_HEALTHY_CAPACITY, signals.maxUnhealthyServerCount(),
                    signals.maxUnallocatedLoad(),
                    "Restore healthy capacity; replay observed no healthy servers during load pressure.");
            add(recommendations, emitted, RemediationAction.RETRY_WHEN_HEALTHY, RemediationPriority.MEDIUM,
                    RemediationReason.NO_HEALTHY_CAPACITY, null, signals.maxUnallocatedLoad(),
                    "Retry allocation or routing after health checks report available capacity.");
        } else {
            if (signals.maxRecommendedAdditionalServers() > 0) {
                add(recommendations, emitted, RemediationAction.SCALE_UP, RemediationPriority.HIGH,
                        RemediationReason.CAPACITY_DEFICIT, signals.maxRecommendedAdditionalServers(),
                        signals.maxUnallocatedLoad(),
                        "Scale up by the largest simulated additional server count seen in replay.");
            }
            if (signals.anyLoadShed()) {
                add(recommendations, emitted, RemediationAction.SHED_LOAD, RemediationPriority.MEDIUM,
                        RemediationReason.LOW_PRIORITY_LOAD_SHED, null, signals.maxRejectedLoad(),
                        "Shed or defer low-priority work observed during replay overload.");
            }
            if (signals.maxUnhealthyServerCount() > 0) {
                add(recommendations, emitted, RemediationAction.INVESTIGATE_UNHEALTHY, RemediationPriority.MEDIUM,
                        RemediationReason.UNHEALTHY_SERVERS_PRESENT, signals.maxUnhealthyServerCount(), null,
                        "Investigate servers that became unhealthy during the replay scenario.");
            }
            if (signals.anyRoutingMiss()) {
                add(recommendations, emitted, RemediationAction.REVIEW_ROUTING, RemediationPriority.LOW,
                        RemediationReason.ROUTING_DEGRADED, null, null,
                        "Review routing strategy inputs because replay produced no selected server.");
            }
        }

        if (recommendations.isEmpty()) {
            add(recommendations, emitted, RemediationAction.NO_ACTION, RemediationPriority.LOW,
                    RemediationReason.HEALTHY, null, 0.0,
                    "No operator action is recommended for this replay.");
        }

        return new RemediationPlan(replayStatus(signals), "scenario-replay", true, true, false, recommendations);
    }

    private static ReplaySignals replaySignals(List<ScenarioReplayStepResponse> steps) {
        double maxUnallocatedLoad = 0.0;
        double maxRejectedLoad = 0.0;
        int maxRecommendedAdditionalServers = 0;
        int maxUnhealthyServerCount = 0;
        boolean anyLoadShed = false;
        boolean anyRoutingMiss = false;
        boolean everAllUnhealthyWithLoad = false;

        for (ScenarioReplayStepResponse step : steps) {
            maxUnallocatedLoad = Math.max(maxUnallocatedLoad, step.unallocatedLoad());
            maxRejectedLoad = Math.max(maxRejectedLoad, step.rejectedLoad());
            maxRecommendedAdditionalServers = Math.max(
                    maxRecommendedAdditionalServers, step.recommendedAdditionalServers());
            anyLoadShed = anyLoadShed || isLoadShed(step.loadShedding());
            anyRoutingMiss = anyRoutingMiss || ("ROUTE".equals(step.type()) && step.selectedServerId() == null);

            int healthyCount = 0;
            int unhealthyCount = 0;
            for (ScenarioServerState serverState : step.serverStates()) {
                if (serverState.healthy()) {
                    healthyCount++;
                } else {
                    unhealthyCount++;
                }
            }
            maxUnhealthyServerCount = Math.max(maxUnhealthyServerCount, unhealthyCount);
            boolean routingMiss = "ROUTE".equals(step.type()) && step.selectedServerId() == null;
            if (healthyCount == 0 && unhealthyCount > 0
                    && (hasPositiveLoad(step.acceptedLoad() + step.rejectedLoad() + step.unallocatedLoad())
                    || routingMiss)) {
                everAllUnhealthyWithLoad = true;
            }
        }

        return new ReplaySignals(maxUnallocatedLoad, maxRejectedLoad, maxRecommendedAdditionalServers,
                maxUnhealthyServerCount, anyLoadShed, anyRoutingMiss, everAllUnhealthyWithLoad);
    }

    private static int healthyServerCount(List<ServerInput> servers) {
        return (int) servers.stream().filter(OperatorRemediationPlanner::isHealthy).count();
    }

    private static int unhealthyServerCount(List<ServerInput> servers) {
        return (int) servers.stream().filter(server -> !isHealthy(server)).count();
    }

    private static boolean isHealthy(ServerInput server) {
        return Boolean.TRUE.equals(server.healthy());
    }

    private static boolean isLoadShed(LoadSheddingEvaluation loadShedding) {
        return loadShedding != null && "SHED".equals(loadShedding.action());
    }

    private static boolean hasPositiveLoad(double load) {
        return Double.isFinite(load) && load > POSITIVE_LOAD_EPSILON;
    }

    private static void add(
            List<RemediationRecommendation> recommendations,
            EnumSet<RemediationAction> emitted,
            RemediationAction action,
            RemediationPriority priority,
            RemediationReason reason,
            Integer serverCount,
            Double loadAmount,
            String message) {
        if (!emitted.add(action)) {
            return;
        }
        recommendations.add(new RemediationRecommendation(
                recommendations.size() + 1,
                action,
                priority,
                reason,
                serverCount,
                loadAmount,
                false,
                message));
    }

    private static String statusFor(
            int healthyServerCount, int unhealthyServerCount, double unallocatedLoad, double rejectedLoad) {
        if (healthyServerCount == 0 && (hasPositiveLoad(unallocatedLoad) || hasPositiveLoad(rejectedLoad))) {
            return "NO_HEALTHY_CAPACITY";
        }
        if (hasPositiveLoad(unallocatedLoad) || hasPositiveLoad(rejectedLoad)) {
            return "OVERLOADED";
        }
        if (unhealthyServerCount > 0) {
            return "DEGRADED";
        }
        return "HEALTHY";
    }

    private static String replayStatus(ReplaySignals signals) {
        if (signals.everAllUnhealthyWithLoad()) {
            return "NO_HEALTHY_CAPACITY";
        }
        if (hasPositiveLoad(signals.maxUnallocatedLoad()) || hasPositiveLoad(signals.maxRejectedLoad())) {
            return "OVERLOADED";
        }
        if (signals.maxUnhealthyServerCount() > 0 || signals.anyRoutingMiss()) {
            return "DEGRADED";
        }
        return "HEALTHY";
    }

    private record ReplaySignals(
            double maxUnallocatedLoad,
            double maxRejectedLoad,
            int maxRecommendedAdditionalServers,
            int maxUnhealthyServerCount,
            boolean anyLoadShed,
            boolean anyRoutingMiss,
            boolean everAllUnhealthyWithLoad) {
    }
}
