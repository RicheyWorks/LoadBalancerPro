package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerDiagnosticListSupportTest {
    @Test
    void copyNonNullReturnsEmptyForNullAndFiltersNullElements() {
        assertEquals(List.of(), DecisionExplorerDiagnosticListSupport.copyNonNull(null));
        assertEquals(List.of("alpha", "beta"),
                DecisionExplorerDiagnosticListSupport.copyNonNull(Arrays.asList("alpha", null, "beta")));
    }

    @Test
    void distinctSortedTrimsDropsBlankValuesAndSortsStableValues() {
        assertEquals(
                List.of("alpha", "beta", "gamma  delta", "gamma delta"),
                DecisionExplorerDiagnosticListSupport.distinctSorted(Arrays.asList(
                        " beta ",
                        null,
                        "",
                        "alpha",
                        "gamma  delta",
                        "gamma delta",
                        "beta")));
    }

    @Test
    void distinctSortedNormalizedWhitespacePreservesEvidenceSufficiencyCanonicalization() {
        assertEquals(
                List.of("alpha", "beta", "gamma delta"),
                DecisionExplorerDiagnosticListSupport.distinctSortedNormalizedWhitespace(Arrays.asList(
                        " beta ",
                        null,
                        "",
                        "alpha",
                        "gamma\r\n  delta",
                        "gamma delta")));
    }
}
