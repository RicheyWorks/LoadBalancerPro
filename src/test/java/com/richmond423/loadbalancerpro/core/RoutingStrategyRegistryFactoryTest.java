package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

class RoutingStrategyRegistryFactoryTest {
    @Test
    void defaultRegistryFactoryCreatesIndependentStatefulStrategies() {
        RoutingStrategyRegistry registry = RoutingStrategyRegistry.defaultRegistry();
        RoutingStrategyRegistry.RoutingStrategyFactory factory =
                registry.requireFactory(RoutingStrategyId.WEIGHTED_ROUND_ROBIN);

        RoutingStrategy first = factory.create();
        RoutingStrategy second = factory.create();

        assertEquals(RoutingStrategyId.WEIGHTED_ROUND_ROBIN, first.id());
        assertEquals(RoutingStrategyId.WEIGHTED_ROUND_ROBIN, second.id());
        assertNotSame(first, second);
    }

    @Test
    void factoryResultMustMatchRegisteredStrategyId() {
        RoutingStrategyRegistry registry = new RoutingStrategyRegistry(Map.of(
                RoutingStrategyId.WEIGHTED_ROUND_ROBIN,
                (RoutingStrategyRegistry.RoutingStrategyFactory) RoundRobinRoutingStrategy::new));

        assertThrows(
                IllegalStateException.class,
                () -> registry.requireFactory(RoutingStrategyId.WEIGHTED_ROUND_ROBIN).create());
    }
}
