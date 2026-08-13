package com.richmond423.loadbalancerpro.core;

import java.util.List;
import java.util.Objects;

/**
 * Optional routing-strategy form that retains an incrementally updated server view.
 *
 * <p>Hosts provide the complete currently eligible snapshot through {@link #onServerStates(List)}
 * before the no-list selection methods. LoadBalancerPro serializes that update/selection cycle
 * on the strategy instance. Implementations used by other hosts must account for their concurrency
 * model. Implementations whose indexes retain membership should override the snapshot method to
 * reconcile absent servers.</p>
 */
public interface StatefulRoutingStrategy extends RoutingStrategy {
    void onServerState(ServerStateVector updated);

    RoutingDecision choose();

    /**
     * Applies a complete current snapshot. The default supports stable-membership strategies by
     * forwarding each vector as an incremental update.
     */
    default void onServerStates(List<ServerStateVector> currentServers) {
        List<ServerStateVector> snapshot = List.copyOf(
                Objects.requireNonNull(currentServers, "currentServers cannot be null"));
        snapshot.forEach(this::onServerState);
    }

    /** No-list keyed selection hook; strategies that do not use keys retain {@link #choose()}. */
    default RoutingDecision chooseForKey(String key) {
        return choose();
    }

    /** No-list deterministic comparison hook; deterministic strategies retain {@link #choose()}. */
    default RoutingDecision chooseForComparison(long deterministicSeed) {
        return choose();
    }

    @Override
    default RoutingDecision choose(List<ServerStateVector> servers) {
        synchronized (this) {
            onServerStates(servers);
            return choose();
        }
    }

    @Override
    default RoutingDecision chooseForKey(List<ServerStateVector> servers, String key) {
        synchronized (this) {
            onServerStates(servers);
            return chooseForKey(key);
        }
    }

    @Override
    default RoutingDecision chooseForComparison(
            List<ServerStateVector> servers,
            long deterministicSeed) {
        synchronized (this) {
            onServerStates(servers);
            return chooseForComparison(deterministicSeed);
        }
    }
}
