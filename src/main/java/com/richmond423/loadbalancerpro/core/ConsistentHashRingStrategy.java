package com.richmond423.loadbalancerpro.core;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ConsistentHashRingStrategy implements RoutingStrategy {
    public static final String STRATEGY_NAME = "CONSISTENT_HASH";
    static final int DEFAULT_HASH_REPLICAS = 128;

    private static final String DEFAULT_KEY = "loadbalancerpro-default-consistent-hash-key";
    private static final Logger LOGGER = LogManager.getLogger(ConsistentHashRingStrategy.class);

    private final Clock clock;
    private final int hashReplicas;
    private final Set<String> configuredServerIds;
    private final ConsistentHashRing.IdRing configuredRing;

    public ConsistentHashRingStrategy() {
        this(Clock.systemUTC(), DEFAULT_HASH_REPLICAS, Set.of());
    }

    ConsistentHashRingStrategy(Clock clock, int hashReplicas, Collection<String> configuredServerIds) {
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.hashReplicas = Math.max(1, hashReplicas);
        LinkedHashSet<String> ids = normalizedIds(configuredServerIds);
        this.configuredServerIds = Set.copyOf(ids);
        this.configuredRing = ids.isEmpty()
                ? null
                : new ConsistentHashRing.IdRing(this.hashReplicas, LOGGER, ids);
    }

    public ConsistentHashRingStrategy configuredFor(Collection<String> serverIds) {
        return new ConsistentHashRingStrategy(clock, hashReplicas, serverIds);
    }

    @Override
    public RoutingStrategyId id() {
        return RoutingStrategyId.CONSISTENT_HASH;
    }

    @Override
    public RoutingDecision choose(List<ServerStateVector> servers) {
        return chooseForKey(servers, DEFAULT_KEY);
    }

    @Override
    public RoutingDecision chooseForComparison(List<ServerStateVector> servers, long deterministicSeed) {
        return chooseForKey(servers, Long.toUnsignedString(deterministicSeed));
    }

    @Override
    public RoutingDecision chooseForKey(List<ServerStateVector> servers, String key) {
        Objects.requireNonNull(servers, "servers cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        Map<String, ServerStateVector> candidates = eligibleCandidates(servers);
        if (candidates.isEmpty()) {
            return noCandidateDecision("No healthy eligible servers with positive routing weight were available.");
        }

        ConsistentHashRing.IdRing ring = configuredRing == null
                ? new ConsistentHashRing.IdRing(hashReplicas, LOGGER, candidates.keySet())
                : configuredRing;
        String selectedId = ring.select(key, candidates::containsKey);
        if (selectedId == null) {
            return noCandidateDecision("No configured ring member was currently healthy and eligible.");
        }
        ServerStateVector chosen = candidates.get(selectedId);
        List<String> considered = candidates.keySet().stream().toList();
        RoutingDecisionExplanation explanation = new RoutingDecisionExplanation(
                STRATEGY_NAME,
                considered,
                Optional.of(selectedId),
                Map.of(),
                "Chose " + selectedId + " from the consistent-hash ring; the routing key is not exposed.",
                Instant.now(clock));
        return new RoutingDecision(Optional.of(chosen), explanation);
    }

    private Map<String, ServerStateVector> eligibleCandidates(List<ServerStateVector> servers) {
        Map<String, ServerStateVector> candidates = new LinkedHashMap<>();
        for (ServerStateVector candidate : servers) {
            if (candidate != null && candidate.healthy() && candidate.weight() > 0.0
                    && (configuredServerIds.isEmpty() || configuredServerIds.contains(candidate.serverId()))) {
                candidates.putIfAbsent(candidate.serverId(), candidate);
            }
        }
        return candidates;
    }

    private RoutingDecision noCandidateDecision(String reason) {
        RoutingDecisionExplanation explanation = new RoutingDecisionExplanation(
                STRATEGY_NAME, List.of(), Optional.empty(), Map.of(), reason, Instant.now(clock));
        return new RoutingDecision(Optional.empty(), explanation);
    }

    private static LinkedHashSet<String> normalizedIds(Collection<String> serverIds) {
        Objects.requireNonNull(serverIds, "serverIds cannot be null");
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String serverId : serverIds) {
            if (serverId == null || serverId.isBlank()) {
                throw new IllegalArgumentException("serverIds cannot contain null or blank values");
            }
            if (!ids.add(serverId.trim())) {
                throw new IllegalArgumentException("serverIds cannot contain duplicates: " + serverId.trim());
            }
        }
        return ids;
    }
}
