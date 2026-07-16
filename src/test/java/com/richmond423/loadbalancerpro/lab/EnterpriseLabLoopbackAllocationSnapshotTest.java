package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabLoopbackAllocationSnapshotTest {

    @Test
    void normalizationIsImmutableExactAndAddsZeroShareForExcludedBackends() {
        var snapshot = snapshot(Map.of("blue", 0.7, "green", 0.3));

        assertEquals(List.of("blue", "green", "orange"), List.copyOf(snapshot.allocations().keySet()));
        assertEquals(1.0, total(snapshot.allocations()), 0.0);
        assertEquals(List.of("blue", "green"), snapshot.eligibleBackendIds());
        assertEquals(List.of("orange"), snapshot.excludedBackendIds());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.allocations().put("orange", 0.1));

        for (long ordinal = 0; ordinal < 20_000; ordinal++) {
            assertTrue(!"orange".equals(snapshot.selectBackend(ordinal)));
        }
    }

    @Test
    void weightedSelectionIsDeterministicStableAndBounded() {
        var snapshot = snapshot(Map.of("blue", 0.5, "green", 0.3, "orange", 0.2));
        Map<String, Integer> first = selectionCounts(snapshot, 50_000);
        Map<String, Integer> second = selectionCounts(snapshot, 50_000);

        assertEquals(first, second);
        assertEquals(0.5, first.get("blue") / 50_000.0, 0.01);
        assertEquals(0.3, first.get("green") / 50_000.0, 0.01);
        assertEquals(0.2, first.get("orange") / 50_000.0, 0.01);
        assertEquals(snapshot.selectBackend(Long.MAX_VALUE), snapshot.selectBackend(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> snapshot.selectBackend(-1));
    }

    @Test
    void normalizationRejectsUnknownInvalidAndOversizedAllocations() {
        assertThrows(IllegalArgumentException.class,
                () -> snapshot(Map.of("blue", 0.5, "rogue", 0.5)));
        assertThrows(IllegalArgumentException.class,
                () -> snapshot(Map.of("blue", 0.4, "green", 0.4)));
        assertThrows(IllegalArgumentException.class,
                () -> snapshot(Map.of("blue", Double.NaN, "green", 1.0)));
        assertThrows(IllegalArgumentException.class,
                () -> EnterpriseLabLoopbackAllocationSnapshot.normalized(
                        "normal-balanced-load", 1, "decision-1", Kind.CANDIDATE,
                        approvedIds(65), Map.of("backend-0", 1.0)));
    }

    private static EnterpriseLabLoopbackAllocationSnapshot snapshot(Map<String, Double> allocation) {
        return EnterpriseLabLoopbackAllocationSnapshot.normalized(
                "normal-balanced-load",
                1,
                "decision-1",
                Kind.CANDIDATE,
                List.of("blue", "green", "orange"),
                allocation);
    }

    private static Map<String, Integer> selectionCounts(
            EnterpriseLabLoopbackAllocationSnapshot snapshot,
            int count) {
        Map<String, Integer> result = new LinkedHashMap<>();
        snapshot.allocations().keySet().forEach(backendId -> result.put(backendId, 0));
        for (long ordinal = 0; ordinal < count; ordinal++) {
            result.computeIfPresent(snapshot.selectBackend(ordinal), (backendId, value) -> value + 1);
        }
        return result;
    }

    private static List<String> approvedIds(int count) {
        List<String> ids = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            ids.add("backend-" + index);
        }
        return ids;
    }

    private static double total(Map<String, Double> allocations) {
        return allocations.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
