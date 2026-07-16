package com.richmond423.loadbalancerpro.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record TrafficAllocationRecommendation(
        Map<String, Double> rawAllocations,
        Map<String, Double> allocations,
        double unallocatedShare,
        boolean fallbackApplied,
        boolean rateLimited,
        List<String> reasons) {
    private static final double EPSILON = 0.000000001;

    public TrafficAllocationRecommendation {
        rawAllocations = immutableSortedShares(rawAllocations, "rawAllocations");
        allocations = immutableSortedShares(allocations, "allocations");
        requireUnitInterval(unallocatedShare, "unallocatedShare");
        Objects.requireNonNull(reasons, "reasons cannot be null");
        reasons = List.copyOf(reasons);
        if (reasons.isEmpty()) {
            throw new IllegalArgumentException("reasons cannot be empty");
        }
        if (!rawAllocations.isEmpty()) {
            requireTotal(rawAllocations, 1.0, "rawAllocations");
        }
        requireTotalWithUnallocatedShare(allocations, unallocatedShare);
        if (fallbackApplied && !allocations.isEmpty()) {
            throw new IllegalArgumentException("fallback recommendations cannot contain allocations");
        }
        if (rateLimited && allocations.isEmpty()) {
            throw new IllegalArgumentException("rateLimited requires a completed allocation");
        }
    }

    static TrafficAllocationRecommendation noAllocation(
            Map<String, Double> rawAllocations,
            List<String> reasons) {
        return new TrafficAllocationRecommendation(
                rawAllocations,
                Map.of(),
                1.0,
                true,
                false,
                reasons);
    }

    private static Map<String, Double> immutableSortedShares(Map<String, Double> shares, String fieldName) {
        Objects.requireNonNull(shares, fieldName + " cannot be null");
        Map<String, Double> sorted = new TreeMap<>();
        for (Map.Entry<String, Double> entry : shares.entrySet()) {
            String serverId = requireNonBlank(entry.getKey(), fieldName + " serverId");
            Double share = Objects.requireNonNull(entry.getValue(), fieldName + " cannot contain null shares");
            requireUnitInterval(share, fieldName + " share");
            sorted.put(serverId, share);
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    private static void requireTotal(Map<String, Double> shares, double expected, String fieldName) {
        double total = shares.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(total - expected) > EPSILON) {
            throw new IllegalArgumentException(fieldName + " must sum to " + expected);
        }
    }

    private static void requireTotalWithUnallocatedShare(Map<String, Double> shares, double unallocatedShare) {
        double allocated = shares.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(allocated + unallocatedShare - 1.0) > EPSILON) {
            throw new IllegalArgumentException("allocations plus unallocatedShare must sum to 1.0");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " must not have surrounding whitespace");
        }
        return value;
    }

    private static void requireUnitInterval(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and between 0.0 and 1.0");
        }
    }
}
