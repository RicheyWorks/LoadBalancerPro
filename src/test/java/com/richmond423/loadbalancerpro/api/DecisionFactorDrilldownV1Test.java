package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionFactorDrilldownV1Test {

    @Test
    void normalizesNullsAndCopiesCollectionsForPartialEvidence() {
        List<String> warnings = new ArrayList<>();
        warnings.add("partial evidence");

        DecisionFactorDrilldownV1 drilldown = new DecisionFactorDrilldownV1(
                null,
                null,
                null,
                null,
                null,
                null,
                warnings,
                null,
                List.of("decision-vector:candidate-a"),
                null);

        warnings.add("late mutation");

        assertEquals("UNKNOWN", drilldown.factorName());
        assertEquals("UNKNOWN", drilldown.candidateId());
        assertEquals("UNKNOWN", drilldown.observedValueOrStatus());
        assertEquals("UNKNOWN", drilldown.influenceCategory());
        assertEquals("UNKNOWN", drilldown.evidenceStatus());
        assertEquals("UNKNOWN", drilldown.explanation());
        assertEquals(List.of("partial evidence"), drilldown.warnings());
        assertTrue(drilldown.unknowns().isEmpty());
        assertEquals(List.of("decision-vector:candidate-a"), drilldown.sourceReferenceIds());
        assertEquals("UNKNOWN", drilldown.boundaryNote());
        assertThrows(UnsupportedOperationException.class, () -> drilldown.warnings().add("mutated"));
    }
}
