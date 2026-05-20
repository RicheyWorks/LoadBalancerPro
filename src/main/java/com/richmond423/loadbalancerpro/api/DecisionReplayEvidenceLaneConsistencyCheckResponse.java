package com.richmond423.loadbalancerpro.api;

public record DecisionReplayEvidenceLaneConsistencyCheckResponse(
        String name,
        String status,
        String expected,
        String actual,
        String detail) {
}
