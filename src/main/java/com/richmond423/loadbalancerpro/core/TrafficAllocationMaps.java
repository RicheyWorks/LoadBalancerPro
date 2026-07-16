package com.richmond423.loadbalancerpro.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

final class TrafficAllocationMaps {
    static final double EPSILON = 0.000000001;

    private TrafficAllocationMaps() {
    }

    static Map<String, Double> immutableNormalized(
            Map<String, Double> shares,
            String fieldName,
            boolean allowEmpty) {
        Objects.requireNonNull(shares, fieldName + " cannot be null");
        if (!allowEmpty && shares.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        Map<String, Double> sorted = new TreeMap<>();
        for (Map.Entry<String, Double> entry : shares.entrySet()) {
            String serverId = requireCanonicalId(entry.getKey(), fieldName + " serverId");
            Double share = Objects.requireNonNull(entry.getValue(), fieldName + " cannot contain null shares");
            if (!Double.isFinite(share) || share < 0.0 || share > 1.0) {
                throw new IllegalArgumentException(fieldName + " shares must be finite and between 0.0 and 1.0");
            }
            sorted.put(serverId, share);
        }
        if (!sorted.isEmpty()) {
            double total = sorted.values().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(total - 1.0) > EPSILON) {
                throw new IllegalArgumentException(fieldName + " must sum to 1.0");
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    static double totalVariation(Map<String, Double> first, Map<String, Double> second) {
        Set<String> serverIds = union(first, second);
        double absoluteMovement = serverIds.stream()
                .mapToDouble(serverId -> Math.abs(
                        first.getOrDefault(serverId, 0.0) - second.getOrDefault(serverId, 0.0)))
                .sum();
        return absoluteMovement / 2.0;
    }

    static boolean same(Map<String, Double> first, Map<String, Double> second) {
        return totalVariation(first, second) <= EPSILON;
    }

    static boolean hasPositiveShareMissingFrom(Map<String, Double> baseline, Set<String> requestedServerIds) {
        return baseline.entrySet().stream()
                .anyMatch(entry -> entry.getValue() > EPSILON && !requestedServerIds.contains(entry.getKey()));
    }

    static Map<String, Double> interpolate(
            Map<String, Double> baseline,
            Map<String, Double> target,
            double factor) {
        if (!Double.isFinite(factor) || factor < 0.0 || factor > 1.0) {
            throw new IllegalArgumentException("interpolation factor must be between 0.0 and 1.0");
        }
        Map<String, Double> result = new TreeMap<>();
        for (String serverId : union(baseline, target)) {
            double initial = baseline.getOrDefault(serverId, 0.0);
            double requested = target.getOrDefault(serverId, 0.0);
            result.put(serverId, initial + (factor * (requested - initial)));
        }
        correctResidual(result);
        return immutableNormalized(result, "interpolatedAllocations", false);
    }

    private static Set<String> union(Map<String, Double> first, Map<String, Double> second) {
        Set<String> serverIds = new TreeSet<>(first.keySet());
        serverIds.addAll(second.keySet());
        return serverIds;
    }

    private static void correctResidual(Map<String, Double> shares) {
        double residual = 1.0 - shares.values().stream().mapToDouble(Double::doubleValue).sum();
        if (residual == 0.0) {
            return;
        }
        for (String serverId : shares.keySet()) {
            double adjusted = shares.get(serverId) + residual;
            if (adjusted >= -EPSILON && adjusted <= 1.0 + EPSILON) {
                shares.put(serverId, Math.max(0.0, Math.min(1.0, adjusted)));
                return;
            }
        }
        throw new IllegalStateException("unable to normalize interpolated allocation");
    }

    private static String requireCanonicalId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not have surrounding whitespace");
        }
        return value;
    }
}
