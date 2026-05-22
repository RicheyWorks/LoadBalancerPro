package com.richmond423.loadbalancerpro.lab;

enum LocalLabFakeBackendBehaviorProfile {
    HEALTHY_FAST(
            "healthy fast baseline",
            "low p50/p95/p99 latency label",
            "no expected application errors",
            "low load and no queue pressure",
            "healthy"),
    SLOW_TAIL_LATENCY(
            "slow tail latency",
            "elevated p95/p99 latency label",
            "mostly successful responses with possible timeout risk",
            "normal load with latency pressure",
            "degraded but still responding"),
    PARTIAL_DEGRADATION(
            "partial degradation",
            "mixed latency label",
            "intermittent soft failures or degraded responses",
            "moderate load with unstable capacity",
            "partially degraded"),
    ERROR_PRONE(
            "error prone",
            "normal-to-mixed latency label",
            "intermittent 500-style error behavior",
            "load may be normal while errors increase",
            "unreliable"),
    OVERLOADED_QUEUE_PRESSURE(
            "overloaded queue pressure",
            "high latency under queue pressure label",
            "timeouts or rejected work may appear under pressure",
            "high load and elevated queue depth",
            "overloaded"),
    ALL_UNHEALTHY_OR_NO_GOOD_CHOICE(
            "all unhealthy or no good choice",
            "unacceptable latency or unavailable label",
            "all candidates are failing, unavailable, or too degraded",
            "load and queue pressure may be unsafe across candidates",
            "no healthy choice"),
    RECOVERY(
            "recovery transition",
            "improving latency label",
            "error rate trending down after degradation",
            "load and queue pressure returning toward baseline",
            "recovering");

    private final String behaviorLabel;
    private final String expectedLatencyBand;
    private final String expectedErrorBehavior;
    private final String expectedLoadQueueBehavior;
    private final String expectedHealthPosture;

    LocalLabFakeBackendBehaviorProfile(
            String behaviorLabel,
            String expectedLatencyBand,
            String expectedErrorBehavior,
            String expectedLoadQueueBehavior,
            String expectedHealthPosture) {
        this.behaviorLabel = behaviorLabel;
        this.expectedLatencyBand = expectedLatencyBand;
        this.expectedErrorBehavior = expectedErrorBehavior;
        this.expectedLoadQueueBehavior = expectedLoadQueueBehavior;
        this.expectedHealthPosture = expectedHealthPosture;
    }

    String behaviorLabel() {
        return behaviorLabel;
    }

    String expectedLatencyBand() {
        return expectedLatencyBand;
    }

    String expectedErrorBehavior() {
        return expectedErrorBehavior;
    }

    String expectedLoadQueueBehavior() {
        return expectedLoadQueueBehavior;
    }

    String expectedHealthPosture() {
        return expectedHealthPosture;
    }
}
