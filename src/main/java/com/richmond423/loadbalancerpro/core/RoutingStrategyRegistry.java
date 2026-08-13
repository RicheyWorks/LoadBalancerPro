package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RoutingStrategyRegistry {
    private final Map<String, RegisteredFactory> factories;

    public RoutingStrategyRegistry() {
        this(defaultFactories());
    }

    public RoutingStrategyRegistry(Collection<? extends RoutingStrategy> strategies) {
        Objects.requireNonNull(strategies, "strategies cannot be null");
        Map<String, RegisteredFactory> registeredFactories = new LinkedHashMap<>();
        for (RoutingStrategy strategy : strategies) {
            RoutingStrategy nonNullStrategy = Objects.requireNonNull(strategy, "strategies cannot contain null");
            RoutingStrategyIdentifier strategyId = Objects.requireNonNull(nonNullStrategy.id(),
                    "strategy id cannot be null");
            String canonicalName = strategyId.canonicalName();
            RegisteredFactory previous = registeredFactories.putIfAbsent(
                    canonicalName,
                    new RegisteredFactory(strategyId, guardedFactory(strategyId, () -> nonNullStrategy)));
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate routing strategy id: " + canonicalName);
            }
        }
        this.factories = Collections.unmodifiableMap(registeredFactories);
    }

    public RoutingStrategyRegistry(Map<RoutingStrategyId, RoutingStrategyFactory> factories) {
        this(factoryRegistrations(factories));
    }

    private RoutingStrategyRegistry(List<FactoryRegistration> registrations) {
        Objects.requireNonNull(registrations, "factory registrations cannot be null");
        Map<String, RegisteredFactory> registeredFactories = new LinkedHashMap<>();
        for (FactoryRegistration registration : registrations) {
            FactoryRegistration nonNullRegistration = Objects.requireNonNull(
                    registration, "factory registrations cannot contain null");
            RoutingStrategyIdentifier strategyId = nonNullRegistration.identifier();
            String canonicalName = strategyId.canonicalName();
            RegisteredFactory previous = registeredFactories.putIfAbsent(
                    canonicalName,
                    new RegisteredFactory(
                            strategyId,
                            guardedFactory(strategyId, nonNullRegistration.factory())));
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate routing strategy id: " + canonicalName);
            }
        }
        this.factories = Collections.unmodifiableMap(registeredFactories);
    }

    public static RoutingStrategyRegistry fromFactories(
            Map<? extends RoutingStrategyIdentifier, RoutingStrategyFactory> factories) {
        return new RoutingStrategyRegistry(factoryRegistrations(factories));
    }

    public static RoutingStrategyRegistry defaultRegistry() {
        return new RoutingStrategyRegistry();
    }

    /** Returns a new registry containing the current registrations and one additional factory. */
    public RoutingStrategyRegistry withFactory(
            RoutingStrategyIdentifier identifier,
            RoutingStrategyFactory factory) {
        List<FactoryRegistration> registrations = new ArrayList<>();
        factories.values().forEach(registered -> registrations.add(
                new FactoryRegistration(registered.identifier(), registered.factory())));
        registrations.add(new FactoryRegistration(identifier, factory));
        return new RoutingStrategyRegistry(registrations);
    }

    public Optional<RoutingStrategyFactory> findFactory(RoutingStrategyIdentifier strategyId) {
        if (strategyId == null) {
            return Optional.empty();
        }
        RegisteredFactory registered = factories.get(strategyId.canonicalName());
        return registered == null ? Optional.empty() : Optional.of(registered.factory());
    }

    public Optional<RoutingStrategyIdentifier> findIdentifier(String strategyName) {
        if (strategyName == null || strategyName.isBlank()) {
            return Optional.empty();
        }
        final String canonicalName;
        try {
            canonicalName = RoutingStrategyIdentifier.canonicalExternalName(strategyName);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        RegisteredFactory registered = factories.get(canonicalName);
        return registered == null ? Optional.empty() : Optional.of(registered.identifier());
    }

    public RoutingStrategyFactory requireFactory(RoutingStrategyIdentifier strategyId) {
        return findFactory(strategyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Routing strategy is not registered: " + strategyId));
    }

    public Optional<RoutingStrategy> find(RoutingStrategyIdentifier strategyId) {
        return findFactory(strategyId).map(RoutingStrategyFactory::create);
    }

    public RoutingStrategy require(RoutingStrategyIdentifier strategyId) {
        return requireFactory(strategyId).create();
    }

    /** Returns registered built-in ids for compatibility with enum-based comparison APIs. */
    public List<RoutingStrategyId> registeredIds() {
        return factories.values().stream()
                .map(RegisteredFactory::identifier)
                .filter(RoutingStrategyId.class::isInstance)
                .map(RoutingStrategyId.class::cast)
                .toList();
    }

    public List<RoutingStrategyIdentifier> registeredIdentifiers() {
        return factories.values().stream().map(RegisteredFactory::identifier).toList();
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

    private static List<FactoryRegistration> factoryRegistrations(
            Map<? extends RoutingStrategyIdentifier, RoutingStrategyFactory> factories) {
        Objects.requireNonNull(factories, "factories cannot be null");
        return factories.entrySet().stream()
                .map(entry -> new FactoryRegistration(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static RoutingStrategyFactory guardedFactory(
            RoutingStrategyIdentifier strategyId, RoutingStrategyFactory factory) {
        return () -> {
            RoutingStrategy strategy = Objects.requireNonNull(factory.create(),
                    "strategy factory returned null for " + strategyId);
            RoutingStrategyIdentifier createdId = Objects.requireNonNull(strategy.id(),
                    "strategy factory returned strategy with null id for " + strategyId);
            if (!strategyId.sameIdentifierAs(createdId)) {
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

    private record FactoryRegistration(
            RoutingStrategyIdentifier identifier,
            RoutingStrategyFactory factory) {
        public FactoryRegistration {
            Objects.requireNonNull(identifier, "factory strategy id cannot be null");
            Objects.requireNonNull(factory, "strategy factory cannot be null");
            identifier.canonicalName();
        }
    }

    private record RegisteredFactory(
            RoutingStrategyIdentifier identifier,
            RoutingStrategyFactory factory) {
    }
}
