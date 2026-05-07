package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.richmond423.loadbalancerpro.util.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class ConsistentHashRingTest {
    private static final Logger LOGGER = LogManager.getLogger(ConsistentHashRingTest.class);

    @Test
    void emptyRingReturnsNullForAnyKey() {
        ConsistentHashRing ring = new ConsistentHashRing(3, LOGGER);

        assertTrue(ring.isEmpty());
        assertNull(ring.selectHealthyServer("missing-key"));
    }

    @Test
    void addingOneServerMakesItSelectableForArbitraryKeys() {
        ConsistentHashRing ring = new ConsistentHashRing(3, LOGGER);
        Server server = server("ONLY");

        ring.addServer(server);

        assertFalse(ring.isEmpty());
        assertSame(server, ring.selectHealthyServer("alpha"));
        assertSame(server, ring.selectHealthyServer("beta"));
        assertSame(server, ring.selectHealthyServer("gamma"));
    }

    @Test
    void multipleServersReturnDeterministicSelectionForSameKey() {
        ConsistentHashRing ring = new ConsistentHashRing(5, LOGGER);
        ring.addServer(server("S1"));
        ring.addServer(server("S2"));
        ring.addServer(server("S3"));

        Server firstSelection = ring.selectHealthyServer("stable-request-key");

        assertSame(firstSelection, ring.selectHealthyServer("stable-request-key"));
        assertSame(firstSelection, ring.selectHealthyServer("stable-request-key"));
    }

    @Test
    void removingExistingServerRemovesItFromSelection() {
        ConsistentHashRing ring = new ConsistentHashRing(3, LOGGER);
        Server removed = server("REMOVE");
        Server remaining = server("KEEP");
        ring.addServer(removed);
        ring.addServer(remaining);

        ring.removeServer(removed);

        assertFalse(ring.isEmpty());
        assertSame(remaining, ring.selectHealthyServer("alpha"));
        assertSame(remaining, ring.selectHealthyServer("beta"));
        assertSame(remaining, ring.selectHealthyServer("gamma"));
    }

    @Test
    void removingMissingServerIsSafeNoOp() {
        ConsistentHashRing ring = new ConsistentHashRing(3, LOGGER);
        Server existing = server("EXISTING");
        ring.addServer(existing);

        ring.removeServer(server("MISSING"));

        assertFalse(ring.isEmpty());
        assertSame(existing, ring.selectHealthyServer("after-missing-remove"));
    }

    @Test
    void unhealthyServersAreSkippedAndAllUnhealthyReturnsNull() {
        ConsistentHashRing ring = new ConsistentHashRing(3, LOGGER);
        Server unhealthy = server("UNHEALTHY");
        Server healthy = server("HEALTHY");
        unhealthy.setHealthy(false);
        ring.addServer(unhealthy);
        ring.addServer(healthy);

        for (int i = 0; i < 20; i++) {
            assertSame(healthy, ring.selectHealthyServer("request-" + i));
        }

        healthy.setHealthy(false);

        assertNull(ring.selectHealthyServer("request-after-all-unhealthy"));
    }

    @Test
    void selectionWrapsToFirstEntryWhenKeyHashIsBeyondLastEntry() {
        ConsistentHashRing ring = new ConsistentHashRing(1, LOGGER);
        Server server = server("WRAP");
        ring.addServer(server);

        assertSame(server, ring.selectHealthyServer(keyHashingAboveReplica("WRAP")));
    }

    @Test
    void addingSameServerIdAgainReplacesRingEntryForThatId() {
        ConsistentHashRing ring = new ConsistentHashRing(3, LOGGER);
        Server original = server("DUPLICATE");
        Server replacement = server("DUPLICATE");

        ring.addServer(original);
        ring.addServer(replacement);

        assertSame(replacement, ring.selectHealthyServer("alpha"));
        assertSame(replacement, ring.selectHealthyServer("beta"));
    }

    private Server server(String id) {
        return new Server(id, 10.0, 20.0, 30.0);
    }

    private String keyHashingAboveReplica(String serverId) {
        long replicaHash = Utils.hash(serverId + "-0");
        for (int i = 0; i < 10_000; i++) {
            String key = "wrap-key-" + i;
            if (Utils.hash(key) > replicaHash) {
                return key;
            }
        }
        throw new AssertionError("Could not find key hashing above server replica");
    }
}
