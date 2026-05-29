package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerDiagnosticFingerprintSupportTest {
    @Test
    void inputCanonicalizesKeysValuesCollectionsAndNonFiniteDoubles() {
        assertEquals("risk/key=value with spaces",
                DecisionExplorerDiagnosticFingerprintSupport.input(" risk|key ", " value\r\nwith   spaces "));
        assertEquals("signals=alpha;beta/nil;null;null",
                DecisionExplorerDiagnosticFingerprintSupport.input(
                        "signals",
                        Arrays.asList(" beta|nil ", null, "alpha", Double.POSITIVE_INFINITY)));
    }

    @Test
    void diagnosticFingerprintUsesFallbackNamespaceForEmptyCanonicalInputs() {
        assertEquals("diagnostic|v1|inputs=none",
                DecisionExplorerDiagnosticFingerprintSupport.diagnosticFingerprint(null,
                        Arrays.asList("", null, "   ")));
    }

    @Test
    void diagnosticFingerprintPreservesInputOrderWhileSanitizingValues() {
        String fingerprint = DecisionExplorerDiagnosticFingerprintSupport.diagnosticFingerprint(
                " route-tradeoff|v1 \n",
                Arrays.asList(" alpha | beta ", null, "gamma\r\ndelta", "omega"));

        assertEquals("route-tradeoff|v1|alpha / beta|gamma delta|omega", fingerprint);
        assertFalse(fingerprint.contains("\n"));
        assertFalse(fingerprint.contains("\r"));
    }

    @Test
    void canonicalInputsFiltersBlankValuesAndKeepsSanitizedOrder() {
        assertEquals(
                List.of("b / c", "a", "d e"),
                DecisionExplorerDiagnosticFingerprintSupport.canonicalInputs(
                        Arrays.asList(" b | c ", "", null, "a", "d\n e")));
    }
}
