package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

    @Test
    void externalStrategyIdentifiersAreCanonicalAndFactoryBacked() {
        RoutingStrategyIdentifier externalId = RoutingStrategyIdentifier.of("daedalus-topology");
        RoutingStrategyRegistry registry = RoutingStrategyRegistry.fromFactories(Map.of(
                externalId,
                (RoutingStrategyRegistry.RoutingStrategyFactory) () -> externalStrategy(externalId)));

        RoutingStrategy first = registry.require(externalId);
        RoutingStrategy second = registry.require(
                registry.findIdentifier("DAEDALUS_TOPOLOGY").orElseThrow());

        assertEquals("DAEDALUS_TOPOLOGY", externalId.externalName());
        assertTrue(externalId.sameIdentifierAs(first.id()));
        assertNotSame(first, second);
        assertEquals(List.of(externalId), registry.registeredIdentifiers());
        assertTrue(registry.registeredIds().isEmpty(),
                "external ids must not be misreported as built-in enum values");
    }

    @Test
    void externalStrategyIdentifiersRejectUnsafeOrEmptyNames() {
        assertThrows(NullPointerException.class, () -> RoutingStrategyIdentifier.of(null));
        assertThrows(IllegalArgumentException.class, () -> RoutingStrategyIdentifier.of(" "));
        assertThrows(IllegalArgumentException.class, () -> RoutingStrategyIdentifier.of("vendor/strategy"));
    }

    @Test
    void externalFactoryCanExtendBuiltInsWithoutReplacingThem() {
        RoutingStrategyIdentifier externalId = RoutingStrategyIdentifier.of("daedalus-topology");

        RoutingStrategyRegistry registry = RoutingStrategyRegistry.defaultRegistry().withFactory(
                externalId, () -> externalStrategy(externalId));

        assertTrue(registry.find(RoutingStrategyId.ROUND_ROBIN).isPresent());
        assertTrue(registry.find(externalId).isPresent());
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.withFactory(
                        RoutingStrategyIdentifier.of("round-robin"),
                        () -> externalStrategy(externalId)));
    }

    private static RoutingStrategy externalStrategy(RoutingStrategyIdentifier id) {
        return new RoutingStrategy() {
            @Override
            public RoutingStrategyIdentifier id() {
                return id;
            }

            @Override
            public RoutingDecision choose(List<ServerStateVector> servers) {
                throw new UnsupportedOperationException("not used by registry contract test");
            }
        };
    }
}
