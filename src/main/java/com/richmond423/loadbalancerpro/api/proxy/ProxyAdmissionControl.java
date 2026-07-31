package com.richmond423.loadbalancerpro.api.proxy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.richmond423.loadbalancerpro.core.AdaptiveConcurrencyConfig;
import com.richmond423.loadbalancerpro.core.AdaptiveConcurrencyLimiter;
import com.richmond423.loadbalancerpro.core.ConcurrencyFeedback;
import com.richmond423.loadbalancerpro.core.ConcurrencyLimitDecision;
import com.richmond423.loadbalancerpro.core.LoadSheddingConfig;
import com.richmond423.loadbalancerpro.core.LoadSheddingDecision;
import com.richmond423.loadbalancerpro.core.LoadSheddingPolicy;
import com.richmond423.loadbalancerpro.core.LoadSheddingSignal;
import com.richmond423.loadbalancerpro.core.RequestPriority;
import jakarta.servlet.http.HttpServletRequest;

final class ProxyAdmissionControl {
    private static final String GLOBAL_TARGET_ID = "proxy-global";
    private static final int ADAPTIVE_MIN_SAMPLE_SIZE = 20;
    private static final int ADAPTIVE_EVALUATION_INTERVAL = 20;
    private static final Pattern HEADER_NAME = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]+");

    private ProxyAdmissionControl() {
    }

    static Policy compile(ReverseProxyProperties properties, Clock clock) {
        Objects.requireNonNull(properties, "properties cannot be null");
        Objects.requireNonNull(clock, "clock cannot be null");
        ReverseProxyProperties.Limits limits = properties.getLimits();
        ReverseProxyProperties.Shedding shedding = properties.getShedding();
        int maxInFlight = limits.getMaxInFlight();
        if (maxInFlight < 0) {
            throw new IllegalStateException("loadbalancerpro.proxy.limits.max-in-flight must be non-negative");
        }
        if (limits.isAdaptive() && maxInFlight == 0) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.limits.adaptive=true requires limits.max-in-flight greater than zero");
        }
        if (shedding.isEnabled() && maxInFlight == 0) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.shedding.enabled=true requires limits.max-in-flight greater than zero");
        }
        LoadSheddingConfig sheddingConfig;
        try {
            sheddingConfig = new LoadSheddingConfig(
                    shedding.getSoftUtilizationThreshold(),
                    shedding.getHardUtilizationThreshold(),
                    shedding.getMaxQueueDepth(),
                    shedding.getMaxP95LatencyMillis(),
                    shedding.getMaxErrorRate(),
                    shedding.isCriticalBypassEnabled(),
                    shedding.isShedUserOnHardPressure());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("loadbalancerpro.proxy.shedding thresholds are invalid: "
                    + exception.getMessage(), exception);
        }
        String priorityHeader = validatedPriorityHeader(shedding.getPriorityHeader());
        int retryAfterSeconds = retryAfterSeconds(shedding.getRetryAfter());
        AdaptiveState adaptiveState = limits.isAdaptive()
                ? AdaptiveState.create(maxInFlight, sheddingConfig, clock)
                : null;
        return new Policy(
                maxInFlight,
                limits.isAdaptive(),
                shedding.isEnabled(),
                priorityHeader,
                retryAfterSeconds,
                sheddingConfig,
                new LoadSheddingPolicy(),
                adaptiveState,
                clock);
    }

    private static String validatedPriorityHeader(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String header = value.trim();
        if (!HEADER_NAME.matcher(header).matches() || ReverseProxyService.isHopByHopHeader(header)) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.shedding.priority-header must be a non-hop-by-hop HTTP header name");
        }
        return header;
    }

    static int retryAfterSeconds(Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException("loadbalancerpro.proxy.shedding.retry-after must be greater than zero");
        }
        long millis;
        try {
            millis = value.toMillis();
        } catch (ArithmeticException exception) {
            return Integer.MAX_VALUE;
        }
        long seconds = Math.max(1, millis / 1_000 + (millis % 1_000 == 0 ? 0 : 1));
        return (int) Math.min(Integer.MAX_VALUE, seconds);
    }

    static final class Policy {
        private final int configuredMaxInFlight;
        private final boolean adaptiveEnabled;
        private final boolean sheddingEnabled;
        private final String priorityHeader;
        private final int retryAfterSeconds;
        private final LoadSheddingConfig sheddingConfig;
        private final LoadSheddingPolicy sheddingPolicy;
        private final AdaptiveState adaptiveState;
        private final Clock clock;

        private Policy(int configuredMaxInFlight,
                       boolean adaptiveEnabled,
                       boolean sheddingEnabled,
                       String priorityHeader,
                       int retryAfterSeconds,
                       LoadSheddingConfig sheddingConfig,
                       LoadSheddingPolicy sheddingPolicy,
                       AdaptiveState adaptiveState,
                       Clock clock) {
            this.configuredMaxInFlight = configuredMaxInFlight;
            this.adaptiveEnabled = adaptiveEnabled;
            this.sheddingEnabled = sheddingEnabled;
            this.priorityHeader = priorityHeader;
            this.retryAfterSeconds = retryAfterSeconds;
            this.sheddingConfig = sheddingConfig;
            this.sheddingPolicy = sheddingPolicy;
            this.adaptiveState = adaptiveState;
            this.clock = clock;
        }

        Admission tryAcquire(HttpServletRequest request, UpstreamRuntimeStats globalStats) {
            Objects.requireNonNull(request, "request cannot be null");
            Objects.requireNonNull(globalStats, "globalStats cannot be null");
            int effectiveLimit = effectiveMaxInFlight();
            RequestPriority priority = requestPriority(request);
            UpstreamRuntimeStats.Snapshot snapshot = sheddingEnabled ? globalStats.snapshot() : null;
            AtomicReference<LoadSheddingDecision> sheddingDecision = new AtomicReference<>();
            boolean acquired = globalStats.tryRequestStarted(effectiveLimit, prospectiveInFlight -> {
                if (!sheddingEnabled) {
                    return true;
                }
                LoadSheddingDecision decision = sheddingPolicy.decide(
                        priority,
                        new LoadSheddingSignal(
                                GLOBAL_TARGET_ID,
                                prospectiveInFlight,
                                effectiveLimit,
                                0,
                                snapshot == null ? 0.0 : snapshot.p95LatencyMillis(),
                                snapshot == null ? 0.0 : snapshot.recentErrorRate(),
                                Instant.now(clock)),
                        sheddingConfig);
                sheddingDecision.set(decision);
                return decision.action() == LoadSheddingDecision.Action.ALLOW;
            });
            if (acquired) {
                return Admission.acquired(priority, retryAfterSeconds);
            }
            LoadSheddingDecision decision = sheddingDecision.get();
            if (decision != null && decision.action() == LoadSheddingDecision.Action.SHED) {
                return Admission.rejected(
                        priority,
                        "proxy_load_shed",
                        "Proxy load-shedding policy rejected this " + priority + " request.",
                        decision.reason(),
                        retryAfterSeconds);
            }
            return Admission.rejected(
                    priority,
                    "proxy_concurrency_limit",
                    "Proxy global in-flight limit is reached.",
                    "global in-flight limit " + effectiveLimit + " is reached",
                    retryAfterSeconds);
        }

        void requestCompleted(UpstreamRuntimeStats globalStats) {
            if (adaptiveState != null) {
                adaptiveState.observe(globalStats);
            }
        }

        Status status(UpstreamRuntimeStats globalStats) {
            UpstreamRuntimeStats.Snapshot snapshot = globalStats.snapshot();
            ConcurrencyLimitDecision decision = adaptiveState == null ? null : adaptiveState.lastDecision();
            return new Status(
                    configuredMaxInFlight,
                    effectiveMaxInFlight(),
                    snapshot.inFlightRequestCount(),
                    adaptiveEnabled,
                    decision == null ? null : decision.action().name(),
                    decision == null ? null : decision.reason(),
                    decision == null ? null : decision.timestamp(),
                    sheddingEnabled,
                    priorityHeader,
                    retryAfterSeconds,
                    sheddingConfig);
        }

        private int effectiveMaxInFlight() {
            return adaptiveState == null ? configuredMaxInFlight : adaptiveState.currentLimit();
        }

        private RequestPriority requestPriority(HttpServletRequest request) {
            if (!sheddingEnabled || priorityHeader.isEmpty()) {
                return RequestPriority.USER;
            }
            String value = request.getHeader(priorityHeader);
            if (value == null || value.isBlank()) {
                return RequestPriority.USER;
            }
            String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            try {
                return RequestPriority.valueOf(normalized);
            } catch (IllegalArgumentException exception) {
                return RequestPriority.USER;
            }
        }
    }

    record Admission(
            boolean acquired,
            RequestPriority priority,
            String errorCode,
            String message,
            String reason,
            int retryAfterSeconds) {
        static Admission acquired(RequestPriority priority, int retryAfterSeconds) {
            return new Admission(true, priority, null, null, "admitted", retryAfterSeconds);
        }

        static Admission rejected(RequestPriority priority,
                                  String errorCode,
                                  String message,
                                  String reason,
                                  int retryAfterSeconds) {
            return new Admission(false, priority, errorCode, message, reason, retryAfterSeconds);
        }
    }

    record Status(
            int configuredMaxInFlight,
            int effectiveMaxInFlight,
            int currentInFlight,
            boolean adaptiveEnabled,
            String lastAdaptiveAction,
            String lastAdaptiveReason,
            Instant lastAdaptiveUpdate,
            boolean sheddingEnabled,
            String priorityHeader,
            int retryAfterSeconds,
            LoadSheddingConfig sheddingConfig) {
    }

    private static final class AdaptiveState {
        private final AtomicInteger currentLimit;
        private final AtomicLong lastEvaluatedCompletedCount = new AtomicLong();
        private final AtomicReference<ConcurrencyLimitDecision> lastDecision = new AtomicReference<>();
        private final AdaptiveConcurrencyLimiter limiter;

        private AdaptiveState(int initialLimit, AdaptiveConcurrencyLimiter limiter) {
            this.currentLimit = new AtomicInteger(initialLimit);
            this.limiter = limiter;
        }

        static AdaptiveState create(int maxInFlight, LoadSheddingConfig sheddingConfig, Clock clock) {
            AdaptiveConcurrencyConfig config = new AdaptiveConcurrencyConfig(
                    1,
                    maxInFlight,
                    1,
                    0.8,
                    sheddingConfig.maxP95LatencyMillis(),
                    sheddingConfig.maxErrorRate(),
                    ADAPTIVE_MIN_SAMPLE_SIZE);
            return new AdaptiveState(maxInFlight, new AdaptiveConcurrencyLimiter(config, clock));
        }

        int currentLimit() {
            return currentLimit.get();
        }

        ConcurrencyLimitDecision lastDecision() {
            return lastDecision.get();
        }

        void observe(UpstreamRuntimeStats stats) {
            long completed = stats.completedRequestCount();
            long previous = lastEvaluatedCompletedCount.get();
            if (completed - previous < ADAPTIVE_EVALUATION_INTERVAL
                    || !lastEvaluatedCompletedCount.compareAndSet(previous, completed)) {
                return;
            }
            UpstreamRuntimeStats.Snapshot snapshot = stats.snapshot();
            ConcurrencyFeedback feedback = new ConcurrencyFeedback(
                    GLOBAL_TARGET_ID,
                    snapshot.inFlightRequestCount(),
                    snapshot.ewmaLatencyMillis(),
                    snapshot.p95LatencyMillis(),
                    snapshot.p99LatencyMillis(),
                    snapshot.recentErrorRate(),
                    snapshot.latencySampleCount(),
                    snapshot.lastUpdatedAt() == null ? Instant.EPOCH : snapshot.lastUpdatedAt());
            ConcurrencyLimitDecision decision = limiter.calculateNextLimit(currentLimit.get(), feedback);
            currentLimit.set(decision.nextLimit());
            lastDecision.set(decision);
        }
    }
}
