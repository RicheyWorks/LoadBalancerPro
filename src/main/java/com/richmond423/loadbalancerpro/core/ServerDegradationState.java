package com.richmond423.loadbalancerpro.core;

public enum ServerDegradationState {
    UNKNOWN,
    HEALTHY,
    PARTIALLY_DEGRADED,
    RECOVERING,
    FAILED,

    /**
     * Operational health lifecycle state: metrics breached the health threshold but the
     * consecutive-bad-cycle eviction threshold has not been reached.
     */
    DEGRADED,

    /**
     * Operational health lifecycle state: an operator explicitly removed the server from
     * allocation rotation without deleting its registry identity.
     */
    DRAINING,

    /**
     * Operational health lifecycle state: consecutive bad health cycles removed the server
     * from allocation rotation without deleting its registry identity.
     */
    EVICTED
}
