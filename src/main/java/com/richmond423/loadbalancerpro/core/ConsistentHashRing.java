package com.richmond423.loadbalancerpro.core;

import com.richmond423.loadbalancerpro.util.Utils;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.apache.logging.log4j.Logger;

final class ConsistentHashRing {
    private final IdRing ring;
    private final Map<String, Server> serversById = new ConcurrentHashMap<>();

    ConsistentHashRing(int hashReplicas, Logger logger) {
        this.ring = new IdRing(hashReplicas, logger);
    }

    void addServer(Server server) {
        Objects.requireNonNull(server, "server cannot be null");
        serversById.put(server.getServerId(), server);
        ring.add(server.getServerId());
    }

    void removeServer(Server server) {
        Objects.requireNonNull(server, "server cannot be null");
        serversById.remove(server.getServerId());
        ring.remove(server.getServerId());
    }

    boolean isEmpty() {
        return ring.isEmpty();
    }

    Server selectHealthyServer(String key) {
        String serverId = ring.select(key, id -> {
            Server server = serversById.get(id);
            return server != null && server.isHealthy();
        });
        return serverId == null ? null : serversById.get(serverId);
    }

    /** Shared virtual-node ring used by both the legacy facade and request-level strategy. */
    static final class IdRing {
        private final ConcurrentNavigableMap<Long, String> entries = new ConcurrentSkipListMap<>();
        private final int hashReplicas;
        private final Logger logger;

        IdRing(int hashReplicas, Logger logger) {
            this.hashReplicas = Math.max(1, hashReplicas);
            this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        }

        IdRing(int hashReplicas, Logger logger, Collection<String> serverIds) {
            this(hashReplicas, logger);
            Objects.requireNonNull(serverIds, "serverIds cannot be null").forEach(this::add);
        }

        void add(String serverId) {
            String id = requireId(serverId);
            for (int replica = 0; replica < hashReplicas; replica++) {
                long hash = replicaHash(id, replica);
                entries.put(hash, id);
            }
        }

        void remove(String serverId) {
            String id = requireId(serverId);
            for (int replica = 0; replica < hashReplicas; replica++) {
                entries.remove(replicaHash(id, replica), id);
            }
        }

        boolean isEmpty() {
            return entries.isEmpty();
        }

        String select(String key, Predicate<String> eligible) {
            Objects.requireNonNull(key, "key cannot be null");
            Objects.requireNonNull(eligible, "eligible cannot be null");
            if (entries.isEmpty()) {
                return null;
            }
            Map.Entry<Long, String> entry = entries.ceilingEntry(Utils.hash(key));
            if (entry == null) {
                entry = entries.firstEntry();
            }
            int attempts = 0;
            while (entry != null && attempts < entries.size()) {
                if (eligible.test(entry.getValue())) {
                    return entry.getValue();
                }
                entry = entries.higherEntry(entry.getKey());
                if (entry == null) {
                    entry = entries.firstEntry();
                }
                attempts++;
            }
            return null;
        }

        private long replicaHash(String serverId, int replica) {
            long hash = Utils.hash(serverId + "-" + replica);
            if (hash == Long.MIN_VALUE) {
                logger.warn("Invalid hash for server {} replica {}; using fallback.", serverId, replica);
                return replica;
            }
            return hash;
        }

        private static String requireId(String serverId) {
            if (serverId == null || serverId.isBlank()) {
                throw new IllegalArgumentException("serverId cannot be null or blank");
            }
            return serverId.trim();
        }
    }
}
