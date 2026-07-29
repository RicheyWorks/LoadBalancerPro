package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerHealthLifecycleTest {
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
    void oneBadHealthSampleDegradesWithoutRemovingOrDrainingServer() {
        Server server = server("TRANSIENT");
        balancer.addServer(server);

        server.updateMetrics(100.0, 100.0, 100.0);
        balancer.checkServerHealth();

        assertSame(server, balancer.getServer("TRANSIENT"));
        assertEquals(List.of(server), balancer.getServers());
        assertEquals(ServerDegradationState.DEGRADED, server.getDegradationState());
        assertTrue(server.isHealthy(), "A single bad cycle must not evict the server from rotation.");
        assertEquals(Map.of("TRANSIENT", 40.0), balancer.roundRobin(40.0));
    }

    @Test
    void consecutiveBadCyclesEvictAndConsecutiveGoodCyclesReadmitWithoutRegistryDeletion() {
        Server unstable = server("UNSTABLE");
        Server survivor = server("SURVIVOR");
        balancer.addServer(unstable);
        balancer.addServer(survivor);

        unstable.updateMetrics(100.0, 100.0, 100.0);
        balancer.checkServerHealth();
        balancer.checkServerHealth();

        assertSame(unstable, balancer.getServer("UNSTABLE"));
        assertTrue(unstable.isHealthy(), "The default bad-cycle threshold must not evict early.");

        balancer.checkServerHealth();

        assertSame(unstable, balancer.getServer("UNSTABLE"),
                "Health eviction removes a server from rotation, not from the registry.");
        assertEquals(ServerDegradationState.EVICTED, unstable.getDegradationState());
        assertFalse(unstable.isHealthy());
        assertEquals(Map.of("SURVIVOR", 60.0), balancer.roundRobin(60.0));

        unstable.updateMetrics(10.0, 20.0, 30.0);
        balancer.checkServerHealth();

        assertEquals(ServerDegradationState.RECOVERING, unstable.getDegradationState());
        assertFalse(unstable.isHealthy(), "One good cycle must not bypass the recovery threshold.");

        balancer.checkServerHealth();

        assertEquals(ServerDegradationState.HEALTHY, unstable.getDegradationState());
        assertTrue(unstable.isHealthy());
        assertEquals(Map.of("UNSTABLE", 40.0, "SURVIVOR", 40.0), balancer.roundRobin(80.0));
        assertEquals(List.of(unstable, survivor), balancer.getServers());
    }

    @Test
    void manualDrainStaysRegisteredAndOnlyExplicitRemovalDeletesIt() {
        Server drained = server("DRAINED");
        Server active = server("ACTIVE");
        balancer.addServer(drained);
        balancer.addServer(active);

        drained.setHealthy(false);
        balancer.checkServerHealth();
        balancer.checkServerHealth();
        balancer.checkServerHealth();

        assertSame(drained, balancer.getServer("DRAINED"));
        assertEquals(ServerDegradationState.DRAINING, drained.getDegradationState());
        assertEquals(List.of(drained, active), balancer.getServers());
        assertEquals(Map.of("ACTIVE", 50.0), balancer.roundRobin(50.0));

        balancer.removeServer("DRAINED");

        assertNull(balancer.getServer("DRAINED"));
        assertEquals(List.of(active), balancer.getServers());
    }

    private static Server server(String id) {
        Server server = new Server(id, 10.0, 20.0, 30.0);
        server.setCapacity(100.0);
        server.setWeight(1.0);
        return server;
    }
}
