package com.richmond423.loadbalancerpro.api;

public record ScenarioServerState(
        String id,
        double cpuUsage,
        double memoryUsage,
        double diskUsage,
        double capacity,
        double weight,
        boolean healthy) {

    static ScenarioServerState from(ServerInput input) {
        return new ScenarioServerState(input.id(), input.cpuUsage(), input.memoryUsage(), input.diskUsage(),
                input.capacity(), input.weight(), input.healthy());
    }
}
