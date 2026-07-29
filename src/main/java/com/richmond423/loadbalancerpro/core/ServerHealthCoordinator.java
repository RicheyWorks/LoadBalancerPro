package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.logging.log4j.Logger;

final class ServerHealthCoordinator {
    static final int DEFAULT_BAD_CYCLES_BEFORE_EVICTION = 3;
    static final int DEFAULT_GOOD_CYCLES_BEFORE_READMISSION = 2;

    private final ServerRegistry serverRegistry;
    private final LoadDistributionEngine loadDistributionEngine;
    private final ConsistentHashRing consistentHashRing;
    private final double maxUsageThreshold;
    private final Logger logger;
    private final Supplier<List<Server>> healthyServers;
    private final Function<Double, Map<String, Double>> leastLoadedDistributor;
    private final Function<ServerType, List<Server>> serversByType;
    private final Supplier<CloudManager> cloudManagerSupplier;
    private final int badCyclesBeforeEviction;
    private final int goodCyclesBeforeReadmission;

    ServerHealthCoordinator(ServerRegistry serverRegistry,
                            LoadDistributionEngine loadDistributionEngine,
                            ConsistentHashRing consistentHashRing,
                            double maxUsageThreshold,
                            Logger logger,
                            Supplier<List<Server>> healthyServers,
                            Function<Double, Map<String, Double>> leastLoadedDistributor,
                            Function<ServerType, List<Server>> serversByType,
                            Supplier<CloudManager> cloudManagerSupplier) {
        this(serverRegistry,
                loadDistributionEngine,
                consistentHashRing,
                maxUsageThreshold,
                logger,
                healthyServers,
                leastLoadedDistributor,
                serversByType,
                cloudManagerSupplier,
                DEFAULT_BAD_CYCLES_BEFORE_EVICTION,
                DEFAULT_GOOD_CYCLES_BEFORE_READMISSION);
    }

    ServerHealthCoordinator(ServerRegistry serverRegistry,
                            LoadDistributionEngine loadDistributionEngine,
                            ConsistentHashRing consistentHashRing,
                            double maxUsageThreshold,
                            Logger logger,
                            Supplier<List<Server>> healthyServers,
                            Function<Double, Map<String, Double>> leastLoadedDistributor,
                            Function<ServerType, List<Server>> serversByType,
                            Supplier<CloudManager> cloudManagerSupplier,
                            int badCyclesBeforeEviction,
                            int goodCyclesBeforeReadmission) {
        if (badCyclesBeforeEviction <= 0 || goodCyclesBeforeReadmission <= 0) {
            throw new IllegalArgumentException("Health-cycle thresholds must be positive.");
        }
        this.serverRegistry = serverRegistry;
        this.loadDistributionEngine = loadDistributionEngine;
        this.consistentHashRing = consistentHashRing;
        this.maxUsageThreshold = maxUsageThreshold;
        this.logger = logger;
        this.healthyServers = healthyServers;
        this.leastLoadedDistributor = leastLoadedDistributor;
        this.serversByType = serversByType;
        this.cloudManagerSupplier = cloudManagerSupplier;
        this.badCyclesBeforeEviction = badCyclesBeforeEviction;
        this.goodCyclesBeforeReadmission = goodCyclesBeforeReadmission;
    }

    List<Server> detectFailedServers() {
        List<Server> newlyEvictedServers = new ArrayList<>();
        for (Server server : serverRegistry.snapshot()) {
            boolean thresholdBreached = server.getCpuUsage() >= maxUsageThreshold
                    || server.getMemoryUsage() >= maxUsageThreshold
                    || server.getDiskUsage() >= maxUsageThreshold;
            Server.HealthTransition transition = server.recordHealthObservation(
                    thresholdBreached,
                    badCyclesBeforeEviction,
                    goodCyclesBeforeReadmission);
            switch (transition) {
                case DEGRADED -> logger.warn(
                        "Server {} ({}) entered DEGRADED after a threshold breach.",
                        server.getServerId(),
                        server.getServerType());
                case EVICTED -> {
                    newlyEvictedServers.add(server);
                    logger.warn(
                            "Server {} ({}) entered EVICTED after {} consecutive bad health cycles.",
                            server.getServerId(),
                            server.getServerType(),
                            badCyclesBeforeEviction);
                }
                case RECOVERING -> logger.info(
                        "Server {} ({}) entered RECOVERING; awaiting {} consecutive good health cycles.",
                        server.getServerId(),
                        server.getServerType(),
                        goodCyclesBeforeReadmission);
                case READMITTED -> logger.info(
                        "Server {} ({}) returned to HEALTHY rotation.",
                        server.getServerId(),
                        server.getServerType());
                case NONE -> {
                    // No lifecycle transition to report.
                }
            }
        }
        return newlyEvictedServers;
    }

    void evictServersAndRecover(List<Server> newlyEvictedServers) {
        double redistributedData = 0;
        List<Server> evictedCloudServers = new ArrayList<>();
        for (Server evicted : newlyEvictedServers) {
            redistributedData += evictServerAllocation(evicted);
            if (evicted.getServerType() == ServerType.CLOUD) {
                evictedCloudServers.add(evicted);
            }
        }
        redistributeLoad(redistributedData);
        replaceFailedCloudCapacity(evictedCloudServers);
    }

    double removeRegisteredServer(Server server) {
        double removedData = loadDistributionEngine.removeServerAllocation(server.getServerId());
        serverRegistry.remove(server);
        consistentHashRing.removeServer(server);
        return removedData;
    }

    private double evictServerAllocation(Server evicted) {
        return loadDistributionEngine.removeServerAllocation(evicted.getServerId());
    }

    private void redistributeLoad(double redistributedData) {
        if (redistributedData > 0) {
            List<Server> healthy = healthyServers.get();
            if (healthy.isEmpty()) {
                logger.error("No healthy servers available to redistribute {}GB.", redistributedData);
                return;
            }
            Map<String, Double> newDist = leastLoadedDistributor.apply(redistributedData);
            loadDistributionEngine.putAllAllocations(newDist);
            logger.info("Redistributed {}GB: {}", redistributedData, newDist);
        }
    }

    private void replaceFailedCloudCapacity(List<Server> failedCloudServers) {
        CloudManager cloudManager = cloudManagerSupplier.get();
        if (cloudManager != null && !failedCloudServers.isEmpty()) {
            int minServers = cloudManager.getMinServers();
            int activeCloudServers = (int) serversByType.apply(ServerType.CLOUD).stream()
                    .filter(Server::isHealthy)
                    .count();
            int desiredCapacity = Math.max(minServers, activeCloudServers + failedCloudServers.size());
            cloudManager.scaleServers(desiredCapacity);
            logger.info("Scaled cloud to {} servers after failover of {} cloud servers.",
                    desiredCapacity, failedCloudServers.size());
        }
    }
}
