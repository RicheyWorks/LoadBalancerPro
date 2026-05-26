package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreLoadBalancerServerLifecycleInvariantTest {
    private LoadBalancer balancer;

    @BeforeEach
    void setUp() {
        balancer = new LoadBalancer();
    }

    @AfterEach
    void tearDown() {
        balancer.shutdown();
    }

    @Test
    void addServerKeepsPublicSnapshotsDetachedAndMapSnapshotReadOnly() {
        Server alpha = server("ALPHA", ServerType.CLOUD);
        balancer.addServer(alpha);

        List<Server> serverSnapshot = balancer.getServers();
        Map<String, Server> mapSnapshot = balancer.getServerMap();
        List<Server> typedSnapshot = balancer.getServersByType(ServerType.CLOUD);

        Server beta = server("BETA", ServerType.ONSITE);
        balancer.addServer(beta);
        serverSnapshot.clear();
        typedSnapshot.clear();

        assertAll("add server snapshot lifecycle contract",
                () -> assertTrue(serverSnapshot.isEmpty()),
                () -> assertTrue(typedSnapshot.isEmpty()),
                () -> assertEquals(List.of(alpha, beta), balancer.getServers()),
                () -> assertEquals(Map.of("ALPHA", alpha), mapSnapshot),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> mapSnapshot.put("BETA", beta)),
                () -> assertEquals(List.of(alpha), balancer.getServersByType(ServerType.CLOUD)),
                () -> assertEquals(List.of(beta), balancer.getServersByType(ServerType.ONSITE)));
    }

    @Test
    void duplicateServerReplacementClearsOldAccumulatedLoadAndUsesReplacement() {
        Server original = server("SAME", ServerType.ONSITE);
        Server peer = server("PEER", ServerType.CLOUD);
        Server replacement = server("SAME", ServerType.CLOUD);
        balancer.addServer(original);
        balancer.roundRobin(90.0);
        balancer.addServer(peer);

        balancer.addServer(replacement);

        Map<String, Double> staleRebalance = balancer.rebalanceExistingLoad();
        Map<String, Double> postReplacementAllocation = balancer.roundRobin(40.0);
        Map<String, Double> postReplacementRebalance = balancer.rebalanceExistingLoad();
        Map<String, Double> hashed = balancer.consistentHashing(50.0, 10);

        assertAll("duplicate replacement lifecycle contract",
                () -> assertSame(replacement, balancer.getServer("SAME")),
                () -> assertFalse(balancer.getServers().contains(original)),
                () -> assertEquals(List.of(peer, replacement), balancer.getServers()),
                () -> assertEquals(Set.of("PEER", "SAME"), balancer.getServerMap().keySet()),
                () -> assertTrue(staleRebalance.isEmpty()),
                () -> assertEquals(Map.of("PEER", 20.0, "SAME", 20.0), postReplacementAllocation),
                () -> assertEquals(Map.of("PEER", 20.0, "SAME", 20.0), postReplacementRebalance),
                () -> assertEquals(Set.of("PEER", "SAME"), hashed.keySet()));
    }

    @Test
    void removeServerReconcilesAccumulatedLoadAndRoutingState() {
        Server removed = server("REMOVE", ServerType.ONSITE);
        Server keep = server("KEEP", ServerType.CLOUD);
        balancer.addServer(removed);
        balancer.addServer(keep);
        balancer.roundRobin(100.0);

        balancer.removeServer("REMOVE");

        Map<String, Double> rebalanced = balancer.rebalanceExistingLoad();
        Map<String, Double> hashed = balancer.consistentHashing(50.0, 10);

        assertAll("remove server lifecycle contract",
                () -> assertNull(balancer.getServer("REMOVE")),
                () -> assertSame(keep, balancer.getServer("KEEP")),
                () -> assertEquals(List.of(keep), balancer.getServers()),
                () -> assertEquals(Map.of("KEEP", keep), balancer.getServerMap()),
                () -> assertEquals(List.of(keep), balancer.getServersByType(ServerType.CLOUD)),
                () -> assertEquals(Map.of("KEEP", 50.0), rebalanced),
                () -> assertEquals(Set.of("KEEP"), hashed.keySet()),
                () -> assertDoesNotThrow(() -> balancer.removeServer("MISSING")),
                () -> assertDoesNotThrow(() -> balancer.removeServer(null)),
                () -> assertEquals(List.of(keep), balancer.getServers()));
    }

    @Test
    void healthTransitionExcludesUnhealthyServerAndReentersRecoveredServer() {
        Server primary = server("PRIMARY", ServerType.CLOUD);
        Server recovering = server("RECOVERING", ServerType.ONSITE);
        balancer.addServer(primary);
        balancer.addServer(recovering);

        recovering.setHealthy(false);
        Map<String, Double> degraded = balancer.roundRobin(80.0);

        recovering.setHealthy(true);
        Map<String, Double> recovered = balancer.roundRobin(80.0);

        assertAll("health transition lifecycle contract",
                () -> assertEquals(Map.of("PRIMARY", 80.0), degraded),
                () -> assertEquals(Map.of("PRIMARY", 40.0, "RECOVERING", 40.0), recovered),
                () -> assertEquals(List.of(primary, recovering), balancer.getServers()),
                () -> assertSame(recovering, balancer.getServer("RECOVERING")));
    }

    @Test
    void healthCheckRemovalRedistributesAccumulatedLoadToSurvivor() {
        Server failing = server("FAILING", ServerType.ONSITE);
        balancer.addServer(failing);
        balancer.roundRobin(120.0);
        Server survivor = server("SURVIVOR", ServerType.CLOUD);
        balancer.addServer(survivor);

        failing.updateMetrics(100.0, 100.0, 100.0);
        balancer.checkServerHealth();

        Map<String, Double> rebalanced = balancer.rebalanceExistingLoad();
        Map<String, Double> hashed = balancer.consistentHashing(60.0, 6);

        assertAll("health-check removal lifecycle contract",
                () -> assertFalse(failing.isHealthy()),
                () -> assertNull(balancer.getServer("FAILING")),
                () -> assertSame(survivor, balancer.getServer("SURVIVOR")),
                () -> assertEquals(List.of(survivor), balancer.getServers()),
                () -> assertEquals(Map.of("SURVIVOR", 120.0), rebalanced),
                () -> assertEquals(Set.of("SURVIVOR"), hashed.keySet()));
    }

    private static Server server(String id, ServerType type) {
        Server server = new Server(id, 10.0, 20.0, 30.0, type);
        server.setCapacity(100.0);
        server.setWeight(1.0);
        return server;
    }
}
