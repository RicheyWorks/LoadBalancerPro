package com.richmond423.loadbalancerpro.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RoutingStrategyRegistry {
    private final Map<RoutingStrategyId, RoutingStrategyFactory> factories;

    public RoutingStrategyRegistry() {
        this(defaultFactories());
    }

    public RoutingStrategyRegistry(Collection<? extends RoutingStrategy> strategies) {
        Objects.requireNonNull(strategies, "strategies cannot be null");
        Map<RoutingStrategyId, RoutingStrategyFactory> registeredFactories = new LinkedHashMap<>();
        for (RoutingStrategy strategy : strategies) {
            RoutingStrategy nonNullStrategy = Objects.requireNonNull(strategy, "strategies cannot contain null");
            RoutingStrategyId strategyId = Objects.requireNonNull(nonNullStrategy.id(),
                    "strategy id cannot be null");
            RoutingStrategyFactory previous = registeredFactories.putIfAbsent(
                    strategyId, guardedFactory(strategyId, () -> nonNullStrategy));
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate routing strategy id: " + strategyId);
            }
        }
        this.factories = Collections.unmodifiableMap(registeredFactories);
    }

    public RoutingStrategyRegistry(Map<RoutingStrategyId, RoutingStrategyFactory> factories) {
        Objects.requireNonNull(factories, "factories cannot be null");
        Map<RoutingStrategyId, RoutingStrategyFactory> registeredFactories = new LinkedHashMap<>();
        factories.forEach((strategyId, factory) -> {
            RoutingStrategyId nonNullId = Objects.requireNonNull(strategyId,
                    "factory strategy id cannot be null");
            RoutingStrategyFactory nonNullFactory = Objects.requireNonNull(factory,
                    "strategy factories cannot contain null");
            registeredFactories.put(nonNullId, guardedFactory(nonNullId, nonNullFactory));
        });
        this.factories = Collections.unmodifiableMap(registeredFactories);
    }

    public static RoutingStrategyRegistry defaultRegistry() {
        return new RoutingStrategyRegistry();
    }

    public Optional<RoutingStrategyFactory> findFactory(RoutingStrategyId strategyId) {
        if (strategyId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(factories.get(strategyId));
    }

    public RoutingStrategyFactory requireFactory(RoutingStrategyId strategyId) {
        return findFactory(strategyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Routing strategy is not registered: " + strategyId));
    }

    public Optional<RoutingStrategy> find(RoutingStrategyId strategyId) {
        return findFactory(strategyId).map(RoutingStrategyFactory::create);
    }

    public RoutingStrategy require(RoutingStrategyId strategyId) {
        return requireFactory(strategyId).create();
    }

    public List<RoutingStrategyId> registeredIds() {
        return List.copyOf(factories.keySet());
    }

    private static Map<RoutingStrategyId, RoutingStrategyFactory> defaultFactories() {
        Map<RoutingStrategyId, RoutingStrategyFactory> defaults = new LinkedHashMap<>();
        defaults.put(RoutingStrategyId.TAIL_LATENCY_POWER_OF_TWO, TailLatencyPowerOfTwoStrategy::new);
        defaults.put(RoutingStrategyId.WEIGHTED_LEAST_LOAD, WeightedLeastLoadStrategy::new);
        defaults.put(RoutingStrategyId.WEIGHTED_LEAST_CONNECTIONS,
                WeightedLeastConnectionsRoutingStrategy::new);
        defaults.put(RoutingStrategyId.WEIGHTED_ROUND_ROBIN, WeightedRoundRobinRoutingStrategy::new);
        defaults.put(RoutingStrategyId.ROUND_ROBIN, RoundRobinRoutingStrategy::new);
        defaults.put(RoutingStrategyId.CONSISTENT_HASH, ConsistentHashRingStrategy::new);
        return defaults;
    }

    private static RoutingStrategyFactory guardedFactory(
            RoutingStrategyId strategyId, RoutingStrategyFactory factory) {
        return () -> {
            RoutingStrategy strategy = Objects.requireNonNull(factory.create(),
                    "strategy factory returned null for " + strategyId);
            RoutingStrategyId createdId = Objects.requireNonNull(strategy.id(),
                    "strategy factory returned strategy with null id for " + strategyId);
            if (createdId != strategyId) {
                throw new IllegalStateException(
                        "Routing strategy factory for " + strategyId + " returned " + createdId);
            }
            return strategy;
        };
    }

    @FunctionalInterface
    public interface RoutingStrategyFactory {
        RoutingStrategy create();
    }
}
