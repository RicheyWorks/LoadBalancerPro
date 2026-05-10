package com.richmond423.loadbalancerpro.api;

public enum RemediationReason {
    HEALTHY,
    CAPACITY_DEFICIT,
    LOW_PRIORITY_LOAD_SHED,
    NO_HEALTHY_CAPACITY,
    UNHEALTHY_SERVERS_PRESENT,
    ROUTING_DEGRADED
}
