package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class FailureScenarioRunner {
    private static final double SEVERE_ERROR_RATE = 0.50;

    public FailureScenarioResult evaluate(FailureScenarioSignal signal, FailureScenarioConfig config) {
        Objects.requireNonNull(signal, "signal cannot be null");
        Objects.requireNonNull(config, "config cannot be null");

        if (signal.sampleSize() < config.minSampleSize()) {
            return result(signal, FailureSeverity.LOW, List.of(MitigationAction.HOLD),
                    "Holding scenario evaluation because insufficient sample size " + signal.sampleSize()
                            + " is below minimum " + config.minSampleSize() + ".");
        }

        PressureClassifier.Assessment pressure = PressureClassifier.assess(
                PressureClassifier.from(signal), PressureClassifier.forFailureScenario(config));
        return switch (signal.scenarioType()) {
            case TRAFFIC_SPIKE -> evaluateTrafficSpike(signal, config, pressure);
            case SLOW_SERVER -> evaluateSlowServer(signal, config, pressure);
            case QUEUE_BACKLOG -> evaluateQueueBacklog(signal, config, pressure);
            case ERROR_STORM -> evaluateErrorStorm(signal, config, pressure);
            case FLAPPING_SERVER -> evaluateFlappingServer(signal, config, pressure);
            case PARTIAL_OUTAGE -> evaluatePartialOutage(signal, config, pressure);
            case CAPACITY_SATURATION -> evaluateCapacitySaturation(signal, config, pressure);
        };
    }

    private FailureScenarioResult evaluateTrafficSpike(FailureScenarioSignal signal,
                                                       FailureScenarioConfig config,
                                                       PressureClassifier.Assessment pressure) {
        List<String> pressures = pressureSignals(signal, config, pressure);
        boolean utilizationPressure = pressure.has(PressureClassifier.Dimension.UTILIZATION);
        boolean queuePressure = pressure.has(PressureClassifier.Dimension.QUEUE);
        if (utilizationPressure || queuePressure) {
            List<MitigationAction> actions = new ArrayList<>();
            actions.add(MitigationAction.SCALE_UP_SHADOW);
            if (queuePressure) {
                actions.add(MitigationAction.SHED_LOW_PRIORITY);
            }
            return result(signal, FailureSeverity.HIGH, actions,
                    "Traffic spike pressure detected: " + String.join("; ", pressures) + ".");
        }
        return lowPressure(signal);
    }

    private FailureScenarioResult evaluateSlowServer(FailureScenarioSignal signal,
                                                     FailureScenarioConfig config,
                                                     PressureClassifier.Assessment pressure) {
        List<String> pressures = pressureSignals(signal, config, pressure);
        if (pressure.has(PressureClassifier.Dimension.P95_LATENCY)
                || pressure.has(PressureClassifier.Dimension.P99_LATENCY)) {
            FailureSeverity severity = signal.observedP99LatencyMillis() > config.highP99LatencyMillis()
                    ? FailureSeverity.HIGH : FailureSeverity.MEDIUM;
            return result(signal, severity,
                    List.of(MitigationAction.REDUCE_CONCURRENCY, MitigationAction.ROUTE_AROUND),
                    "Slow server pressure detected: " + String.join("; ", pressures) + ".");
        }
        return lowPressure(signal);
    }

    private FailureScenarioResult evaluateQueueBacklog(FailureScenarioSignal signal,
                                                       FailureScenarioConfig config,
                                                       PressureClassifier.Assessment pressure) {
        List<String> pressures = pressureSignals(signal, config, pressure);
        if (pressure.has(PressureClassifier.Dimension.QUEUE)) {
            return result(signal, FailureSeverity.HIGH,
                    List.of(MitigationAction.SHED_LOW_PRIORITY, MitigationAction.SCALE_UP_SHADOW),
                    "Queue backlog pressure detected: " + String.join("; ", pressures) + ".");
        }
        return lowPressure(signal);
    }

    private FailureScenarioResult evaluateErrorStorm(FailureScenarioSignal signal,
                                                     FailureScenarioConfig config,
                                                     PressureClassifier.Assessment pressure) {
        if (pressure.has(PressureClassifier.Dimension.ERROR_RATE)) {
            if (signal.observedErrorRate() >= SEVERE_ERROR_RATE) {
                return result(signal, FailureSeverity.CRITICAL,
                        List.of(MitigationAction.INVESTIGATE, MitigationAction.FAIL_CLOSED),
                        "severe error rate detected: "
                                + String.join("; ", pressureSignals(signal, config, pressure)) + ".");
            }
            return result(signal, FailureSeverity.HIGH, List.of(MitigationAction.INVESTIGATE),
                    "Error storm pressure detected: "
                            + String.join("; ", pressureSignals(signal, config, pressure)) + ".");
        }
        return lowPressure(signal);
    }

    private FailureScenarioResult evaluateFlappingServer(FailureScenarioSignal signal,
                                                         FailureScenarioConfig config,
                                                         PressureClassifier.Assessment pressure) {
        if (pressure.has(PressureClassifier.Dimension.HEALTH)
                || pressure.has(PressureClassifier.Dimension.ERROR_RATE)) {
            return result(signal, FailureSeverity.HIGH,
                    List.of(MitigationAction.ROUTE_AROUND, MitigationAction.INVESTIGATE),
                    "Flapping server pressure detected: "
                            + String.join("; ", pressureSignals(signal, config, pressure)) + ".");
        }
        return lowPressure(signal);
    }

    private FailureScenarioResult evaluatePartialOutage(FailureScenarioSignal signal,
                                                        FailureScenarioConfig config,
                                                        PressureClassifier.Assessment pressure) {
        if (pressure.has(PressureClassifier.Dimension.HEALTH)) {
            return result(signal, FailureSeverity.CRITICAL,
                    List.of(MitigationAction.ROUTE_AROUND, MitigationAction.SHED_LOW_PRIORITY,
                            MitigationAction.INVESTIGATE),
                    "Partial outage pressure detected: "
                            + String.join("; ", pressureSignals(signal, config, pressure)) + ".");
        }
        return lowPressure(signal);
    }

    private FailureScenarioResult evaluateCapacitySaturation(FailureScenarioSignal signal,
                                                             FailureScenarioConfig config,
                                                             PressureClassifier.Assessment pressure) {
        if (pressure.has(PressureClassifier.Dimension.UTILIZATION)) {
            return result(signal, FailureSeverity.HIGH,
                    List.of(MitigationAction.SCALE_UP_SHADOW, MitigationAction.REDUCE_CONCURRENCY),
                    "Capacity saturation detected: "
                            + String.join("; ", pressureSignals(signal, config, pressure)) + ".");
        }
        return lowPressure(signal);
    }

    private FailureScenarioResult lowPressure(FailureScenarioSignal signal) {
        return result(signal, FailureSeverity.LOW, List.of(MitigationAction.HOLD),
                "Holding because scenario signals show low pressure.");
    }

    private List<String> pressureSignals(FailureScenarioSignal signal,
                                         FailureScenarioConfig config,
                                         PressureClassifier.Assessment pressure) {
        List<String> pressures = new ArrayList<>();
        if (pressure.has(PressureClassifier.Dimension.HEALTH)) {
            pressures.add("healthy ratio " + format(signal.healthyRatio())
                    + " is below threshold " + format(config.partialOutageHealthyRatioThreshold()));
        }
        if (pressure.has(PressureClassifier.Dimension.UTILIZATION)) {
            pressures.add("utilization " + format(signal.utilization())
                    + " reached threshold " + format(config.saturationUtilizationThreshold()));
        }
        if (pressure.has(PressureClassifier.Dimension.QUEUE)) {
            pressures.add("queue depth " + signal.queueDepth()
                    + " exceeded threshold " + config.highQueueDepthThreshold());
        }
        if (pressure.has(PressureClassifier.Dimension.P95_LATENCY)) {
            pressures.add("p95 latency " + format(signal.observedP95LatencyMillis())
                    + "ms exceeded threshold " + format(config.highP95LatencyMillis()) + "ms");
        }
        if (pressure.has(PressureClassifier.Dimension.P99_LATENCY)) {
            pressures.add("p99 latency " + format(signal.observedP99LatencyMillis())
                    + "ms exceeded threshold " + format(config.highP99LatencyMillis()) + "ms");
        }
        if (pressure.has(PressureClassifier.Dimension.ERROR_RATE)) {
            pressures.add("error rate " + format(signal.observedErrorRate())
                    + " exceeded threshold " + format(config.highErrorRate()));
        }
        if (pressures.isEmpty()) {
            pressures.add("no configured pressure thresholds exceeded");
        }
        return pressures;
    }

    private FailureScenarioResult result(FailureScenarioSignal signal,
                                         FailureSeverity severity,
                                         List<MitigationAction> recommendations,
                                         String reason) {
        return new FailureScenarioResult(signal.scenarioId(), signal.scenarioType(), signal.targetId(),
                severity, recommendations, reason, signal.timestamp(), signal.healthyRatio(),
                signal.utilization(), signal.queueDepth(), signal.observedP95LatencyMillis(),
                signal.observedP99LatencyMillis(), signal.observedErrorRate(), signal.sampleSize());
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
