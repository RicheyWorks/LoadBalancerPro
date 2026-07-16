package com.richmond423.loadbalancerpro.core;

public enum ServerDegradationState {
    UNKNOWN,
    HEALTHY,
    PARTIALLY_DEGRADED,
    RECOVERING,
    FAILED
}
