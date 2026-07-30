package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class LoadSheddingPolicy {
    public LoadSheddingDecision decide(RequestPriority priority,
                                       LoadSheddingSignal signal,
                                       LoadSheddingConfig config) {
        Objects.requireNonNull(priority, "priority cannot be null");
        Objects.requireNonNull(signal, "signal cannot be null");
        Objects.requireNonNull(config, "config cannot be null");

        Pressure pressure = pressure(signal, config);
        LoadSheddingDecision.Action action = actionFor(priority, pressure, config);
        String reason = reasonFor(priority, pressure, action, config);

        return new LoadSheddingDecision(priority, action, reason, signal.timestamp(),
                signal.targetId(), signal.currentInFlightRequestCount(), signal.concurrencyLimit(),
                signal.queueDepth(), signal.utilization(), signal.observedP95LatencyMillis(),
                signal.observedErrorRate());
    }

    private Pressure pressure(LoadSheddingSignal signal, LoadSheddingConfig config) {
        PressureClassifier.Observation observation = PressureClassifier.from(signal);
        PressureClassifier.Assessment hardPressure = PressureClassifier.assess(
                observation, PressureClassifier.forHardLoadShedding(config));
        List<String> hardPressureReasons = new ArrayList<>();
        if (hardPressure.has(PressureClassifier.Dimension.UTILIZATION)) {
            hardPressureReasons.add("hard utilization " + format(signal.utilization())
                    + " reached threshold " + format(config.hardUtilizationThreshold()));
        }
        if (hardPressure.has(PressureClassifier.Dimension.QUEUE)) {
            hardPressureReasons.add("queue depth " + signal.queueDepth()
                    + " exceeded threshold " + config.maxQueueDepth());
        }
        if (hardPressure.has(PressureClassifier.Dimension.P95_LATENCY)) {
            hardPressureReasons.add("p95 latency " + format(signal.observedP95LatencyMillis())
                    + "ms exceeded threshold " + format(config.maxP95LatencyMillis()) + "ms");
        }
        if (hardPressure.has(PressureClassifier.Dimension.ERROR_RATE)) {
            hardPressureReasons.add("error rate " + format(signal.observedErrorRate())
                    + " exceeded threshold " + format(config.maxErrorRate()));
        }
        if (!hardPressureReasons.isEmpty()) {
            return new Pressure(Level.HARD, "overload pressure: " + String.join("; ", hardPressureReasons));
        }
        PressureClassifier.Assessment softPressure = PressureClassifier.assess(
                observation, PressureClassifier.forSoftLoadShedding(config));
        if (softPressure.has(PressureClassifier.Dimension.UTILIZATION)) {
            return new Pressure(Level.SOFT, "soft utilization " + format(signal.utilization())
                    + " reached threshold " + format(config.softUtilizationThreshold()));
        }
        return new Pressure(Level.NORMAL, "normal pressure");
    }

    private LoadSheddingDecision.Action actionFor(RequestPriority priority,
                                                  Pressure pressure,
                                                  LoadSheddingConfig config) {
        if (pressure.level() == Level.NORMAL) {
            return LoadSheddingDecision.Action.ALLOW;
        }
        if (priority == RequestPriority.CRITICAL && config.criticalBypassEnabled()) {
            return LoadSheddingDecision.Action.ALLOW;
        }
        if (pressure.level() == Level.SOFT) {
            return priority == RequestPriority.PREFETCH
                    ? LoadSheddingDecision.Action.SHED
                    : LoadSheddingDecision.Action.ALLOW;
        }
        return switch (priority) {
            case PREFETCH, BACKGROUND -> LoadSheddingDecision.Action.SHED;
            case USER, CRITICAL -> config.shedUserOnHardPressure()
                    ? LoadSheddingDecision.Action.SHED
                    : LoadSheddingDecision.Action.ALLOW;
        };
    }

    private String reasonFor(RequestPriority priority,
                             Pressure pressure,
                             LoadSheddingDecision.Action action,
                             LoadSheddingConfig config) {
        if (pressure.level() == Level.NORMAL) {
            return "Allowing " + priority + " request because pressure is normal.";
        }
        if (priority == RequestPriority.CRITICAL && config.criticalBypassEnabled()) {
            return "Allowing CRITICAL request because critical bypass is enabled during "
                    + pressure.description() + ".";
        }
        if (pressure.level() == Level.SOFT) {
            if (action == LoadSheddingDecision.Action.SHED) {
                return "Shedding " + priority + " request due to " + pressure.description()
                        + "; PREFETCH is lowest priority.";
            }
            return "Allowing " + priority + " request during " + pressure.description()
                    + " to preserve higher-priority traffic.";
        }
        if (priority == RequestPriority.USER && action == LoadSheddingDecision.Action.ALLOW) {
            return "Allowing USER request during " + pressure.description()
                    + " because configured policy protects USER traffic under hard pressure.";
        }
        if (priority == RequestPriority.CRITICAL && action == LoadSheddingDecision.Action.ALLOW) {
            return "Allowing CRITICAL request during " + pressure.description()
                    + " because higher-priority traffic cannot be shed while USER traffic is protected.";
        }
        return (action == LoadSheddingDecision.Action.SHED ? "Shedding " : "Allowing ")
                + priority + " request due to " + pressure.description() + ".";
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private enum Level {
        NORMAL,
        SOFT,
        HARD
    }

    private record Pressure(Level level, String description) {
    }
}
