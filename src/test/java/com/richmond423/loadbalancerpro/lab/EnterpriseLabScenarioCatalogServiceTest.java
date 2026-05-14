package com.richmond423.loadbalancerpro.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

class EnterpriseLabScenarioCatalogServiceTest {
    private final EnterpriseLabScenarioCatalogService catalog = new EnterpriseLabScenarioCatalogService();

    @Test
    void catalogMetadataIsStableOrderedAndUnique() {
        List<EnterpriseLabScenarioMetadata> scenarios = catalog.listScenarioMetadata();

        assertEquals(10, scenarios.size());
        assertEquals("normal-balanced-load", scenarios.get(0).scenarioId());
        assertEquals("zero-edge-metric-values", scenarios.get(9).scenarioId());
        assertEquals(scenarios.size(), new HashSet<>(scenarios.stream()
                .map(EnterpriseLabScenarioMetadata::scenarioId)
                .toList()).size());
        assertTrue(scenarios.stream().allMatch(scenario ->
                scenario.supportedModes().equals(List.of("off", "shadow", "recommend", "active-experiment"))));
        assertTrue(scenarios.stream().allMatch(scenario ->
                EnterpriseLabScenarioCatalogService.FIXTURE_VERSION.equals(scenario.deterministicFixtureVersion())));
    }

    @Test
    void scenarioMetadataIncludesSignalsGuardrailsAndInfluenceSafety() {
        EnterpriseLabScenarioMetadata stale = catalog.findScenarioMetadata("stale-signal").orElseThrow();
        EnterpriseLabScenarioMetadata allUnhealthy = catalog.findScenarioMetadata("all-unhealthy-degradation")
                .orElseThrow();
        EnterpriseLabScenarioMetadata normal = catalog.findScenarioMetadata("normal-balanced-load").orElseThrow();

        assertEquals("signal-quality", stale.category());
        assertFalse(stale.safeForInfluenceExperiment());
        assertTrue(stale.expectedGuardrails().contains("stale signals block active influence"));
        assertEquals("resilience", allUnhealthy.category());
        assertFalse(allUnhealthy.safeForInfluenceExperiment());
        assertTrue(normal.safeForInfluenceExperiment());
        assertTrue(normal.signalsInvolved().contains("shadow"));
    }

    @Test
    void resolveScenariosRejectsUnknownAndTooManySelections() {
        assertEquals(1, catalog.resolveScenarios(List.of("normal-balanced-load"), 10).size());
        assertThrows(IllegalArgumentException.class,
                () -> catalog.resolveScenarios(List.of("missing-scenario"), 10));
        assertThrows(IllegalArgumentException.class,
                () -> catalog.resolveScenarios(List.of("normal-balanced-load", "tail-latency-pressure"), 1));
    }
}

