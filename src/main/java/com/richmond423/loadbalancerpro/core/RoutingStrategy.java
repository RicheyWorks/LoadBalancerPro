package com.richmond423.loadbalancerpro.core;

import java.util.List;

public interface RoutingStrategy {
    RoutingStrategyId id();

    RoutingDecision choose(List<ServerStateVector> servers);

    /**
     * Request-key-aware entry point. Strategies that do not use affinity keys keep their
     * existing behavior through this default implementation.
     */
    default RoutingDecision chooseForKey(List<ServerStateVector> servers, String key) {
        return choose(servers);
    }

    /**
     * Comparison-only entry point. Stateful production strategies retain their normal
     * {@link #choose(List)} behavior; strategies with random comparison sampling can use the
     * canonical request-derived seed without changing their live routing behavior.
     */
    default RoutingDecision chooseForComparison(List<ServerStateVector> servers, long deterministicSeed) {
        return choose(servers);
    }
}
