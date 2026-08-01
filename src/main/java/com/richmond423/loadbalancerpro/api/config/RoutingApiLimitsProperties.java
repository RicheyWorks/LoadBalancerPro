package com.richmond423.loadbalancerpro.api.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "loadbalancerpro.api")
public class RoutingApiLimitsProperties {
    public static final int ABSOLUTE_MAX_CANDIDATES = 32;
    public static final int ABSOLUTE_MAX_STRATEGIES = 6;
    public static final long ABSOLUTE_MAX_DECISION_EXPLORER_RESPONSE_BYTES = 64L * 1024L * 1024L;

    @Min(1)
    @Max(ABSOLUTE_MAX_CANDIDATES)
    private int maxCandidates = ABSOLUTE_MAX_CANDIDATES;

    @Min(1)
    @Max(ABSOLUTE_MAX_STRATEGIES)
    private int maxStrategies = ABSOLUTE_MAX_STRATEGIES;

    @Min(1)
    @Max(ABSOLUTE_MAX_DECISION_EXPLORER_RESPONSE_BYTES)
    private long maxDecisionExplorerResponseBytes = 16L * 1024L * 1024L;

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = maxCandidates;
    }

    public int getMaxStrategies() {
        return maxStrategies;
    }

    public void setMaxStrategies(int maxStrategies) {
        this.maxStrategies = maxStrategies;
    }

    public long getMaxDecisionExplorerResponseBytes() {
        return maxDecisionExplorerResponseBytes;
    }

    public void setMaxDecisionExplorerResponseBytes(long maxDecisionExplorerResponseBytes) {
        this.maxDecisionExplorerResponseBytes = maxDecisionExplorerResponseBytes;
    }
}
