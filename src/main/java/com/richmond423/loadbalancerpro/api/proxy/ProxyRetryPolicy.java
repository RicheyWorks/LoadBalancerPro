package com.richmond423.loadbalancerpro.api.proxy;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;

final class ProxyRetryPolicy {
    private static final int CREDIT_SCALE = 100;
    private static final Duration MAXIMUM_BACKOFF = Duration.ofSeconds(60);

    private final int budgetPercent;
    private final Duration backoffBase;
    private final Duration backoffMax;
    private final DoubleSupplier jitter;
    private final Sleeper sleeper;

    private int availableCredits;
    private long primaryRequests;
    private long grantedRetries;
    private long rejectedRetries;

    private ProxyRetryPolicy(
            int budgetPercent,
            Duration backoffBase,
            Duration backoffMax,
            DoubleSupplier jitter,
            Sleeper sleeper) {
        this.budgetPercent = budgetPercent;
        this.backoffBase = backoffBase;
        this.backoffMax = backoffMax;
        this.jitter = Objects.requireNonNull(jitter, "jitter cannot be null");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper cannot be null");
    }

    static ProxyRetryPolicy compile(ReverseProxyProperties.Retry retry) {
        Objects.requireNonNull(retry, "retry cannot be null");
        int budgetPercent = retry.getBudgetPercent();
        if (budgetPercent < 0 || budgetPercent > 100) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.retry.budget-percent must be between 0 and 100");
        }
        ReverseProxyProperties.Backoff backoff = retry.getBackoff();
        Duration base = requireBoundedDuration(
                backoff.getBase(), "loadbalancerpro.proxy.retry.backoff.base");
        Duration max = requireBoundedDuration(
                backoff.getMax(), "loadbalancerpro.proxy.retry.backoff.max");
        if (max.compareTo(base) < 0) {
            throw new IllegalStateException(
                    "loadbalancerpro.proxy.retry.backoff.max must be greater than or equal to backoff.base");
        }
        return new ProxyRetryPolicy(
                budgetPercent,
                base,
                max,
                () -> ThreadLocalRandom.current().nextDouble(),
                duration -> TimeUnit.NANOSECONDS.sleep(duration.toNanos()));
    }

    static ProxyRetryPolicy forTest(
            ReverseProxyProperties.Retry retry, DoubleSupplier jitter, Sleeper sleeper) {
        ProxyRetryPolicy compiled = compile(retry);
        return new ProxyRetryPolicy(
                compiled.budgetPercent, compiled.backoffBase, compiled.backoffMax, jitter, sleeper);
    }

    synchronized void recordPrimaryRequest() {
        primaryRequests++;
        availableCredits = Math.min(CREDIT_SCALE, availableCredits + budgetPercent);
    }

    synchronized boolean tryAcquireRetry() {
        if (availableCredits < CREDIT_SCALE) {
            rejectedRetries++;
            return false;
        }
        availableCredits -= CREDIT_SCALE;
        grantedRetries++;
        return true;
    }

    Duration pauseBeforeRetry(int retryNumber) throws InterruptedException {
        Duration ceiling = backoffCeiling(retryNumber);
        if (ceiling.isZero()) {
            return Duration.ZERO;
        }
        double sample = jitter.getAsDouble();
        double boundedSample = Double.isFinite(sample)
                ? Math.max(0.0, Math.min(Math.nextDown(1.0), sample))
                : 0.0;
        long delayNanos = (long) Math.floor(ceiling.toNanos() * boundedSample);
        Duration delay = Duration.ofNanos(delayNanos);
        if (!delay.isZero()) {
            sleeper.sleep(delay);
        }
        return delay;
    }

    Duration backoffCeiling(int retryNumber) {
        int safeRetryNumber = Math.max(1, retryNumber);
        long ceiling = backoffBase.toNanos();
        long maximum = backoffMax.toNanos();
        for (int step = 1; step < safeRetryNumber && ceiling < maximum; step++) {
            ceiling = ceiling > maximum / 2 ? maximum : Math.min(maximum, ceiling * 2);
        }
        return Duration.ofNanos(Math.min(ceiling, maximum));
    }

    synchronized Snapshot snapshot() {
        return new Snapshot(
                budgetPercent,
                backoffBase.toMillis(),
                backoffMax.toMillis(),
                primaryRequests,
                grantedRetries,
                rejectedRetries,
                availableCredits);
    }

    private static Duration requireBoundedDuration(Duration value, String fieldName) {
        if (value == null || value.isNegative() || value.compareTo(MAXIMUM_BACKOFF) > 0) {
            throw new IllegalStateException(
                    fieldName + " must be between 0ms and " + MAXIMUM_BACKOFF.toSeconds() + "s");
        }
        return value;
    }

    record Snapshot(
            int budgetPercent,
            long backoffBaseMillis,
            long backoffMaxMillis,
            long primaryRequests,
            long grantedRetries,
            long rejectedRetries,
            int availableCreditsPercent) {
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }
}
