package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

final class ServerRegistry {
    private final List<Server> servers = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Server> serverMap = new ConcurrentHashMap<>();
    private final PriorityQueue<Server> loadQueue = new PriorityQueue<>(Comparator.comparingDouble(Server::getLoadScore));

    boolean contains(String serverId) {
        return serverMap.containsKey(serverId);
    }

    void add(Server server) {
        servers.add(server);
        serverMap.put(server.getServerId(), server);
        loadQueue.offer(server);
    }

    void remove(Server server) {
        servers.remove(server);
        serverMap.remove(server.getServerId());
        loadQueue.remove(server);
    }

    Server get(String serverId) {
        return serverMap.get(serverId);
    }

    List<Server> snapshot() {
        return new ArrayList<>(servers);
    }

    Map<String, Server> mapSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(serverMap));
    }

    List<Server> byType(ServerType type) {
        return servers.stream()
                      .filter(server -> server.getServerType() == type)
                      .collect(Collectors.toList());
    }

    List<Server> healthySnapshot() {
        return servers.stream().filter(Server::isHealthy).toList();
    }

    boolean isEmpty() {
        return servers.isEmpty();
    }
}
