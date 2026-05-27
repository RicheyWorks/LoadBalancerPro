package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerCandidateComparisonRowV1Test {

    @Test
    void normalizesNullsAndCopiesCollectionsForPartialCandidateEvidence() {
        List<String> warnings = new ArrayList<>();
        warnings.add("candidate final score was not returned");

        DecisionExplorerCandidateComparisonRowV1 row = new DecisionExplorerCandidateComparisonRowV1(
                null,
                null,
                false,
                2,
                null,
                null,
                null,
                List.of("healthState=true"),
                null,
                null,
                null,
                List.of("decision-vector:candidate-a"),
                warnings,
                null,
                null);

        warnings.add("late mutation");

        assertEquals("UNKNOWN", row.candidateId());
        assertEquals("UNKNOWN", row.candidateLabel());
        assertEquals(2, row.displayOrder());
        assertEquals("UNKNOWN", row.comparisonStatus());
        assertEquals(List.of("healthState=true"), row.visibleSignals());
        assertEquals(List.of("candidate final score was not returned"), row.warnings());
        assertEquals(List.of("decision-vector:candidate-a"), row.evidenceReferenceIds());
        assertEquals("UNKNOWN", row.boundaryNote());
        assertThrows(UnsupportedOperationException.class, () -> row.warnings().add("mutated"));
    }
}
