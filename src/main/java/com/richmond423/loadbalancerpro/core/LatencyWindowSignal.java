package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

public record LatencyWindowSignal(
        int sampleCount,
        OptionalDouble ewmaLatencyMillis,
        OptionalDouble rollingAverageLatencyMillis,
        OptionalDouble rollingP95LatencyMillis,
        OptionalDouble rollingP99LatencyMillis) {
    private static final int DEFAULT_MAX_WINDOW_SIZE = 64;
    private static final double MINIMUM_LATENCY_BASIS_MILLIS = 1.0;
    private static final LatencyWindowSignal EMPTY = new LatencyWindowSignal(
            0,
            OptionalDouble.empty(),
            OptionalDouble.empty(),
            OptionalDouble.empty(),
            OptionalDouble.empty());

    public LatencyWindowSignal {
        Objects.requireNonNull(ewmaLatencyMillis, "ewmaLatencyMillis cannot be null");
        Objects.requireNonNull(rollingAverageLatencyMillis, "rollingAverageLatencyMillis cannot be null");
        Objects.requireNonNull(rollingP95LatencyMillis, "rollingP95LatencyMillis cannot be null");
        Objects.requireNonNull(rollingP99LatencyMillis, "rollingP99LatencyMillis cannot be null");
        requireNonNegative(sampleCount, "sampleCount");
        ewmaLatencyMillis.ifPresent(value -> requireNonNegative(value, "ewmaLatencyMillis"));
        rollingAverageLatencyMillis.ifPresent(value -> requireNonNegative(value, "rollingAverageLatencyMillis"));
        rollingP95LatencyMillis.ifPresent(value -> requireNonNegative(value, "rollingP95LatencyMillis"));
        rollingP99LatencyMillis.ifPresent(value -> requireNonNegative(value, "rollingP99LatencyMillis"));
        if (sampleCount == 0 && hasAnyValue(ewmaLatencyMillis, rollingAverageLatencyMillis,
                rollingP95LatencyMillis, rollingP99LatencyMillis)) {
            throw new IllegalArgumentException("sampleCount must be positive when latency window values are present");
        }
    }

    public static LatencyWindowSignal empty() {
        return EMPTY;
    }

    public static LatencyWindowSignal fromSamples(List<Double> latencySamplesMillis, double ewmaAlpha) {
        return fromSamples(latencySamplesMillis, ewmaAlpha, DEFAULT_MAX_WINDOW_SIZE);
    }

    public static LatencyWindowSignal fromSamples(List<Double> latencySamplesMillis,
                                                  double ewmaAlpha,
                                                  int maxWindowSize) {
        Objects.requireNonNull(latencySamplesMillis, "latencySamplesMillis cannot be null");
        requireAlpha(ewmaAlpha);
        if (maxWindowSize < 1) {
            throw new IllegalArgumentException("maxWindowSize must be greater than zero");
        }
        if (latencySamplesMillis.isEmpty()) {
            return empty();
        }

        int startIndex = Math.max(0, latencySamplesMillis.size() - maxWindowSize);
        List<Double> boundedSamples = new ArrayList<>(latencySamplesMillis.size() - startIndex);
        for (int index = startIndex; index < latencySamplesMillis.size(); index++) {
            double sample = latencySamplesMillis.get(index);
            requireNonNegative(sample, "latencySamplesMillis");
            boundedSamples.add(sample);
        }

        double rollingAverage = boundedSamples.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double ewma = ewma(boundedSamples, ewmaAlpha);
        List<Double> sortedSamples = boundedSamples.stream().sorted(Comparator.naturalOrder()).toList();

        return new LatencyWindowSignal(
                boundedSamples.size(),
                OptionalDouble.of(ewma),
                OptionalDouble.of(rollingAverage),
                OptionalDouble.of(percentile(sortedSamples, 95)),
                OptionalDouble.of(percentile(sortedSamples, 99)));
    }

    public boolean hasLatencyWindowValues() {
        return sampleCount > 0 && hasAnyValue(ewmaLatencyMillis, rollingAverageLatencyMillis,
                rollingP95LatencyMillis, rollingP99LatencyMillis);
    }

    public boolean hasTailLatencyWindowValues() {
        return sampleCount > 0 && rollingP95LatencyMillis.isPresent() && rollingP99LatencyMillis.isPresent();
    }

    public double effectiveAverageLatencyMillis(double fallbackAverageLatencyMillis) {
        requireNonNegative(fallbackAverageLatencyMillis, "fallbackAverageLatencyMillis");
        if (rollingAverageLatencyMillis.isPresent()) {
            return rollingAverageLatencyMillis.getAsDouble();
        }
        if (ewmaLatencyMillis.isPresent()) {
            return ewmaLatencyMillis.getAsDouble();
        }
        return fallbackAverageLatencyMillis;
    }

    public double effectiveP95LatencyMillis(double fallbackP95LatencyMillis) {
        requireNonNegative(fallbackP95LatencyMillis, "fallbackP95LatencyMillis");
        return rollingP95LatencyMillis.orElse(fallbackP95LatencyMillis);
    }

    public double effectiveP99LatencyMillis(double fallbackP99LatencyMillis) {
        requireNonNegative(fallbackP99LatencyMillis, "fallbackP99LatencyMillis");
        return rollingP99LatencyMillis.orElse(fallbackP99LatencyMillis);
    }

    public double effectiveTailLatencySpreadMillis(double fallbackP95LatencyMillis, double fallbackP99LatencyMillis) {
        double effectiveP95 = effectiveP95LatencyMillis(fallbackP95LatencyMillis);
        double effectiveP99 = effectiveP99LatencyMillis(fallbackP99LatencyMillis);
        return Math.max(0.0, effectiveP99 - effectiveP95);
    }

    public double effectiveTailLatencyPressure(double fallbackP95LatencyMillis, double fallbackP99LatencyMillis) {
        double effectiveP95 = effectiveP95LatencyMillis(fallbackP95LatencyMillis);
        double spread = effectiveTailLatencySpreadMillis(fallbackP95LatencyMillis, fallbackP99LatencyMillis);
        return boundedRatio(spread, Math.max(MINIMUM_LATENCY_BASIS_MILLIS, effectiveP95));
    }

    private static double ewma(List<Double> samples, double alpha) {
        double value = samples.get(0);
        for (int index = 1; index < samples.size(); index++) {
            value = (alpha * samples.get(index)) + ((1.0 - alpha) * value);
        }
        return value;
    }

    private static double percentile(List<Double> sortedSamples, int percentile) {
        if (sortedSamples.isEmpty()) {
            return 0.0;
        }
        double rank = Math.ceil((percentile / 100.0) * sortedSamples.size());
        int index = (int) Math.max(0, Math.min(sortedSamples.size() - 1, rank - 1));
        return sortedSamples.get(index);
    }

    private static boolean hasAnyValue(OptionalDouble... values) {
        for (OptionalDouble value : values) {
            if (value.isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static void requireAlpha(double value) {
        if (!Double.isFinite(value) || value <= 0.0 || value > 1.0) {
            throw new IllegalArgumentException("ewmaAlpha must be finite and between 0.0 exclusive and 1.0 inclusive");
        }
    }

    private static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }

    private static void requireNonNegative(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
    }

    private static double boundedRatio(double numerator, double denominator) {
        if (denominator <= 0.0) {
            return numerator > 0.0 ? 1.0 : 0.0;
        }
        return Math.max(0.0, Math.min(1.0, numerator / denominator));
    }
}
