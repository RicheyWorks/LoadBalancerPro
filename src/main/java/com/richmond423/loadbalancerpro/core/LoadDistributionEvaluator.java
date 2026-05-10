package com.richmond423.loadbalancerpro.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure, stateless load distribution evaluator for read-only API previews.
 */
public final class LoadDistributionEvaluator {
    private static final double DEFAULT_PREDICTIVE_LOAD_FACTOR = 1.1;

    public LoadDistributionResult capacityAware(List<Server> servers, double totalData) {
        return LoadDistributionPlanner.capacityAwareResult(healthyServers(servers), requireValidLoad(totalData));
    }

    public LoadDistributionResult predictive(List<Server> servers, double totalData) {
        List<Server> healthyServers = healthyServers(servers);
        Map<String, Double> predictedLoads = healthyServers.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Server::getServerId,
                        server -> server.getLoadScore() * DEFAULT_PREDICTIVE_LOAD_FACTOR));
        return LoadDistributionPlanner.predictiveResult(healthyServers, requireValidLoad(totalData), predictedLoads);
    }

    private static List<Server> healthyServers(List<Server> servers) {
        Objects.requireNonNull(servers, "servers cannot be null");
        return servers.stream()
                .filter(Objects::nonNull)
                .filter(Server::isHealthy)
                .toList();
    }

    private static double requireValidLoad(double totalData) {
        if (!Double.isFinite(totalData) || totalData < 0.0) {
            throw new IllegalArgumentException("totalData must be a finite non-negative number");
        }
        return totalData;
    }
}
