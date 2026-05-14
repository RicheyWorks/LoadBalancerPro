package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AdaptiveRoutingExperimentFixtureCatalogTest {

    @Test
    void fixtureCatalogCoversDeterministicExperimentEdgesInStableOrder() {
        List<AdaptiveRoutingExperimentScenario> scenarios =
                new AdaptiveRoutingExperimentFixtureCatalog().createAll();

        assertEquals(List.of(
                "normal-balanced-load",
                "tail-latency-pressure",
                "overload-pressure",
                "stale-signal",
                "conflicting-signal",
                "all-unhealthy-degradation",
                "recovery-transition",
                "capacity-skew",
                "repeated-replay-event-order",
                "zero-edge-metric-values"), scenarios.stream().map(AdaptiveRoutingExperimentScenario::name).toList());
        assertTrue(scenarios.stream().allMatch(scenario -> !scenario.servers().isEmpty()));
        assertTrue(scenarios.stream().allMatch(scenario -> !scenario.replayEventOrder().isEmpty()));
    }

    @Test
    void repeatedCatalogConstructionIsStable() {
        AdaptiveRoutingExperimentFixtureCatalog catalog = new AdaptiveRoutingExperimentFixtureCatalog();

        assertEquals(catalog.createAll(), catalog.createAll());
    }
}
