package com.richmond423.loadbalancerpro.api;

public enum RemediationAction {
    SCALE_UP,
    INVESTIGATE_UNHEALTHY,
    RESTORE_CAPACITY,
    SHED_LOAD,
    RETRY_WHEN_HEALTHY,
    NO_ACTION,
    REVIEW_ROUTING
}
