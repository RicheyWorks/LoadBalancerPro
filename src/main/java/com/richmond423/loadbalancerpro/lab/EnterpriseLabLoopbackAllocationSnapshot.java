package com.richmond423.loadbalancerpro.lab;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Immutable allocation used only by the bounded Enterprise Lab loopback router.
 */
public record EnterpriseLabLoopbackAllocationSnapshot(
        String schemaVersion,
        String scenarioId,
        long revision,
        String sourceDecisionId,
        Kind kind,
        Map<String, Double> allocations) {
    public static final String SCHEMA_VERSION = "enterprise-lab-loopback-allocation/v1";
    static final int HARD_MAX_BACKENDS = 64;
    private static final int MAX_ID_LENGTH = 128;
    private static final double EPSILON = 0.000000001;

    public EnterpriseLabLoopbackAllocationSnapshot {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported loopback allocation schemaVersion");
        }
        scenarioId = requireCanonicalId(scenarioId, "scenarioId");
        if (revision < 0) {
            throw new IllegalArgumentException("revision cannot be negative");
        }
        sourceDecisionId = requireCanonicalId(sourceDecisionId, "sourceDecisionId");
        kind = Objects.requireNonNull(kind, "kind cannot be null");
        allocations = immutableExactAllocation(allocations);
    }

    static EnterpriseLabLoopbackAllocationSnapshot normalized(
            String scenarioId,
            long revision,
            String sourceDecisionId,
            Kind kind,
            Collection<String> approvedBackendIds,
            Map<String, Double> requestedAllocations) {
        return new EnterpriseLabLoopbackAllocationSnapshot(
                SCHEMA_VERSION,
                scenarioId,
                revision,
                sourceDecisionId,
                kind,
                normalizedAllocations(approvedBackendIds, requestedAllocations, false));
    }

    /**
     * Reuses the router's allocation normalization for durable records while
     * requiring every approved backend to be represented explicitly.
     */
    static Map<String, Double> exactNormalizedAllocations(
            Collection<String> approvedBackendIds,
            Map<String, Double> requestedAllocations) {
        return normalizedAllocations(approvedBackendIds, requestedAllocations, true);
    }

    private static Map<String, Double> normalizedAllocations(
            Collection<String> approvedBackendIds,
            Map<String, Double> requestedAllocations,
            boolean requireExactBackendSet) {
        Objects.requireNonNull(approvedBackendIds, "approvedBackendIds cannot be null");
        Objects.requireNonNull(requestedAllocations, "requestedAllocations cannot be null");
        TreeSet<String> approved = new TreeSet<>();
        for (String backendId : approvedBackendIds) {
            String safeBackendId = requireCanonicalId(backendId, "approved backendId");
            if (!approved.add(safeBackendId)) {
                throw new IllegalArgumentException("approved backendIds must be unique");
            }
        }
        if (approved.isEmpty() || approved.size() > HARD_MAX_BACKENDS) {
            throw new IllegalArgumentException("approved backend count must be between 1 and 64");
        }
        if (requestedAllocations.isEmpty()) {
            throw new IllegalArgumentException("requestedAllocations cannot be empty");
        }
        if (!approved.containsAll(requestedAllocations.keySet())) {
            throw new IllegalArgumentException("allocation contains a backend outside the approved scenario catalog");
        }
        if (requireExactBackendSet && !approved.equals(new TreeSet<>(requestedAllocations.keySet()))) {
            throw new IllegalArgumentException("durable allocation must contain every approved backend exactly once");
        }

        Map<String, Double> complete = new TreeMap<>();
        for (String backendId : approved) {
            complete.put(backendId, requestedAllocations.getOrDefault(backendId, 0.0));
        }
        return immutableExactAllocation(complete);
    }

    public List<String> eligibleBackendIds() {
        return allocations.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.0)
                .map(Map.Entry::getKey)
                .toList();
    }

    public List<String> excludedBackendIds() {
        return allocations.entrySet().stream()
                .filter(entry -> entry.getValue() == 0.0)
                .map(Map.Entry::getKey)
                .toList();
    }

    public String selectBackend(long selectionOrdinal) {
        if (selectionOrdinal < 0) {
            throw new IllegalArgumentException("selectionOrdinal cannot be negative");
        }
        double routingPoint = deterministicUnitInterval(selectionOrdinal);
        double cumulative = 0.0;
        String lastEligible = "";
        for (Map.Entry<String, Double> entry : allocations.entrySet()) {
            if (entry.getValue() <= 0.0) {
                continue;
            }
            lastEligible = entry.getKey();
            cumulative += entry.getValue();
            if (routingPoint < cumulative) {
                return entry.getKey();
            }
        }
        if (lastEligible.isEmpty()) {
            throw new IllegalStateException("allocation has no eligible backend");
        }
        return lastEligible;
    }

    boolean sameAllocations(EnterpriseLabLoopbackAllocationSnapshot other) {
        return other != null && sameAllocations(allocations, other.allocations);
    }

    static boolean sameAllocations(Map<String, Double> first, Map<String, Double> second) {
        if (first == null || second == null || !first.keySet().equals(second.keySet())) {
            return false;
        }
        return first.keySet().stream()
                .allMatch(backendId -> Math.abs(first.get(backendId) - second.get(backendId)) <= EPSILON);
    }

    private static Map<String, Double> immutableExactAllocation(Map<String, Double> values) {
        Objects.requireNonNull(values, "allocations cannot be null");
        if (values.isEmpty() || values.size() > HARD_MAX_BACKENDS) {
            throw new IllegalArgumentException("allocation backend count must be between 1 and 64");
        }
        Map<String, Double> sorted = new TreeMap<>();
        double total = 0.0;
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            String backendId = requireCanonicalId(entry.getKey(), "allocation backendId");
            Double share = Objects.requireNonNull(entry.getValue(), "allocation shares cannot be null");
            if (!Double.isFinite(share) || share < 0.0 || share > 1.0) {
                throw new IllegalArgumentException("allocation shares must be finite and between 0.0 and 1.0");
            }
            if (share == 0.0d) {
                share = 0.0d;
            }
            if (sorted.putIfAbsent(backendId, share) != null) {
                throw new IllegalArgumentException("allocation backendIds must be unique");
            }
            total += share;
        }
        if (Math.abs(total - 1.0) > EPSILON) {
            throw new IllegalArgumentException("allocation shares must sum to 1.0");
        }

        double residual = 1.0 - total;
        if (residual != 0.0) {
            List<String> candidates = new ArrayList<>(sorted.keySet());
            for (String backendId : candidates) {
                double adjusted = sorted.get(backendId) + residual;
                if (adjusted >= 0.0 && adjusted <= 1.0) {
                    sorted.put(backendId, adjusted);
                    break;
                }
            }
        }
        double exactTotal = sorted.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(exactTotal - 1.0) > EPSILON) {
            throw new IllegalStateException("unable to normalize loopback allocation");
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    private static double deterministicUnitInterval(long selectionOrdinal) {
        long value = selectionOrdinal + 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }

    private static String requireCanonicalId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        if (!value.equals(value.trim()) || value.length() > MAX_ID_LENGTH
                || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(fieldName + " must be a bounded canonical identifier");
        }
        return value;
    }

    public enum Kind {
        BASELINE,
        CANDIDATE,
        RESTORED_BASELINE
    }
}
