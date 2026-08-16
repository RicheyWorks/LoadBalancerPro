package com.richmond423.loadbalancerpro.api.config;

/** Optional observer boundary for rate-limit events. */
@FunctionalInterface
public interface ApiRateLimitObserver {
    void recordRateLimited(String surface);

    static ApiRateLimitObserver disabled() {
        return ignored -> { };
    }
}
