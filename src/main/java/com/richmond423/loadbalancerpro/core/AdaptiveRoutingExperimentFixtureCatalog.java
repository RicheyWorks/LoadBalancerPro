package com.richmond423.loadbalancerpro.core;

import java.util.List;

public final class AdaptiveRoutingExperimentFixtureCatalog {

    public List<AdaptiveRoutingExperimentScenario> createAll() {
        return List.of(
                normalBalancedLoad(),
                tailLatencyPressure(),
                overloadPressure(),
                staleSignal(),
                conflictingSignal(),
                allUnhealthyDegradation(),
                recoveryTransition(),
                capacitySkew(),
                repeatedReplayEventOrder(),
                zeroEdgeMetricValues());
    }

    private AdaptiveRoutingExperimentScenario normalBalancedLoad() {
        return scenario(
                "normal-balanced-load",
                "Balanced healthy servers with enough capacity; LASE should observe without forcing change.",
                80.0,
                true,
                List.of("request", "allocation", "shadow-observation"),
                "normal",
                server("blue", 25, 30, 20, 100, true),
                server("green", 28, 25, 22, 100, true),
                server("orange", 35, 35, 30, 100, true));
    }

    private AdaptiveRoutingExperimentScenario tailLatencyPressure() {
        return scenario(
                "tail-latency-pressure",
                "One server has high resource pressure that drives higher synthetic tail latency.",
                90.0,
                true,
                List.of("request", "latency-signal", "shadow-observation"),
                "tail latency",
                server("blue", 88, 82, 78, 100, true),
                server("green", 30, 28, 24, 100, true),
                server("orange", 45, 40, 35, 100, true));
    }

    private AdaptiveRoutingExperimentScenario overloadPressure() {
        return scenario(
                "overload-pressure",
                "Requested load exceeds available healthy capacity and creates unallocated pressure.",
                260.0,
                true,
                List.of("request", "capacity-pressure", "shadow-observation"),
                "overload",
                server("blue", 55, 60, 50, 80, true),
                server("green", 50, 55, 50, 80, true),
                server("orange", 90, 88, 90, 80, false));
    }

    private AdaptiveRoutingExperimentScenario staleSignal() {
        return scenario(
                "stale-signal",
                "Good-looking routing signal is marked stale; influence must refuse to change allocation.",
                70.0,
                false,
                List.of("request", "stale-shadow-event", "shadow-observation"),
                "stale signal",
                server("blue", 80, 70, 65, 100, true),
                server("green", 25, 20, 20, 100, true),
                server("orange", 35, 40, 30, 100, true));
    }

    private AdaptiveRoutingExperimentScenario conflictingSignal() {
        return scenario(
                "conflicting-signal",
                "Capacity and load signals disagree; shadow evidence should explain the chosen recommendation.",
                120.0,
                true,
                List.of("request", "conflicting-metric", "shadow-observation"),
                "conflicting signal",
                server("blue", 20, 20, 20, 40, true),
                server("green", 68, 72, 70, 140, true),
                server("orange", 45, 50, 45, 80, true));
    }

    private AdaptiveRoutingExperimentScenario allUnhealthyDegradation() {
        return scenario(
                "all-unhealthy-degradation",
                "Every backend is unhealthy; baseline and LASE influence must fail closed without cloud mutation.",
                60.0,
                true,
                List.of("request", "health-collapse", "shadow-observation"),
                "all unhealthy",
                server("blue", 95, 95, 95, 100, false),
                server("green", 92, 90, 94, 100, false),
                server("orange", 88, 90, 91, 100, false));
    }

    private AdaptiveRoutingExperimentScenario recoveryTransition() {
        return scenario(
                "recovery-transition",
                "A restored backend has low pressure after an incident while other nodes remain warm.",
                100.0,
                true,
                List.of("incident", "restore-backend", "allocation", "shadow-observation"),
                "recovery transition",
                server("blue", 72, 68, 65, 100, true),
                server("green", 20, 18, 16, 100, true),
                server("orange", 75, 70, 72, 100, true));
    }

    private AdaptiveRoutingExperimentScenario capacitySkew() {
        return scenario(
                "capacity-skew",
                "Healthy servers have skewed capacity so baseline may favor capacity while LASE checks pressure.",
                160.0,
                true,
                List.of("request", "capacity-skew", "shadow-observation"),
                "capacity skew",
                server("blue", 35, 35, 35, 200, true),
                server("green", 20, 20, 20, 50, true),
                server("orange", 30, 30, 30, 50, true));
    }

    private AdaptiveRoutingExperimentScenario repeatedReplayEventOrder() {
        return scenario(
                "repeated-replay-event-order",
                "Repeated replay-style events keep deterministic order and stable comparison output.",
                75.0,
                true,
                List.of("request-1", "request-1", "allocation-1", "shadow-observation-1"),
                "repeated replay event order",
                server("blue", 45, 40, 35, 100, true),
                server("green", 44, 39, 34, 100, true),
                server("orange", 46, 41, 36, 100, true));
    }

    private AdaptiveRoutingExperimentScenario zeroEdgeMetricValues() {
        return scenario(
                "zero-edge-metric-values",
                "Zero resource metrics and zero requested load remain deterministic and no-op friendly.",
                0.0,
                true,
                List.of("request", "zero-metric", "shadow-observation"),
                "zero edge metric",
                server("blue", 0, 0, 0, 100, true),
                server("green", 0, 0, 0, 100, true),
                server("orange", 0, 0, 0, 100, true));
    }

    private AdaptiveRoutingExperimentScenario scenario(
            String name,
            String description,
            double requestedLoad,
            boolean signalsFresh,
            List<String> replayEventOrder,
            String expectedPressure,
            AdaptiveRoutingExperimentServer... servers) {
        return new AdaptiveRoutingExperimentScenario(
                name,
                description,
                "CAPACITY_AWARE",
                requestedLoad,
                List.of(servers),
                signalsFresh,
                replayEventOrder,
                expectedPressure);
    }

    private AdaptiveRoutingExperimentServer server(
            String id,
            double cpuUsage,
            double memoryUsage,
            double diskUsage,
            double capacity,
            boolean healthy) {
        return new AdaptiveRoutingExperimentServer(id, cpuUsage, memoryUsage, diskUsage, capacity, 1.0, healthy);
    }
}
