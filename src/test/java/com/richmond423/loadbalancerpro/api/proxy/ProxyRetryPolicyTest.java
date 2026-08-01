package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProxyRetryPolicyTest {
    @Test
    void twentyPercentBudgetCapsSustainedRetriesWithoutAccumulatingABurst() {
        ReverseProxyProperties.Retry retry = retry(20, Duration.ZERO, Duration.ZERO);
        ProxyRetryPolicy policy = ProxyRetryPolicy.forTest(retry, () -> 0.0, ignored -> { });
        int granted = 0;
        int rejected = 0;

        for (int request = 0; request < 100; request++) {
            policy.recordPrimaryRequest();
            if (policy.tryAcquireRetry()) {
                granted++;
            } else {
                rejected++;
            }
        }

        ProxyRetryPolicy.Snapshot snapshot = policy.snapshot();
        assertEquals(20, granted);
        assertEquals(80, rejected);
        assertEquals(100, snapshot.primaryRequests());
        assertEquals(20, snapshot.grantedRetries());
        assertEquals(80, snapshot.rejectedRetries());
        assertEquals(0, snapshot.availableCreditsPercent());
    }

    @Test
    void healthyTrafficCanBankAtMostOneRetry() {
        ReverseProxyProperties.Retry retry = retry(20, Duration.ZERO, Duration.ZERO);
        ProxyRetryPolicy policy = ProxyRetryPolicy.forTest(retry, () -> 0.0, ignored -> { });

        for (int request = 0; request < 100; request++) {
            policy.recordPrimaryRequest();
        }

        assertTrue(policy.tryAcquireRetry());
        assertFalse(policy.tryAcquireRetry());
        assertEquals(0, policy.snapshot().availableCreditsPercent());
    }

    @Test
    void fullJitterUsesExponentialCeilingsCappedByConfiguredMaximum() throws Exception {
        ReverseProxyProperties.Retry retry = retry(
                100, Duration.ofMillis(100), Duration.ofMillis(500));
        List<Duration> sleeps = new ArrayList<>();
        ProxyRetryPolicy policy = ProxyRetryPolicy.forTest(retry, () -> 0.5, sleeps::add);

        assertEquals(Duration.ofMillis(50), policy.pauseBeforeRetry(1));
        assertEquals(Duration.ofMillis(100), policy.pauseBeforeRetry(2));
        assertEquals(Duration.ofMillis(200), policy.pauseBeforeRetry(3));
        assertEquals(Duration.ofMillis(250), policy.pauseBeforeRetry(4));
        assertEquals(Duration.ofMillis(250), policy.pauseBeforeRetry(20));
        assertEquals(List.of(
                Duration.ofMillis(50),
                Duration.ofMillis(100),
                Duration.ofMillis(200),
                Duration.ofMillis(250),
                Duration.ofMillis(250)), sleeps);
    }

    @Test
    void zeroBudgetRejectsRetriesAndZeroBackoffDoesNotSleep() throws Exception {
        ReverseProxyProperties.Retry retry = retry(0, Duration.ZERO, Duration.ZERO);
        List<Duration> sleeps = new ArrayList<>();
        ProxyRetryPolicy policy = ProxyRetryPolicy.forTest(retry, () -> 0.5, sleeps::add);

        policy.recordPrimaryRequest();

        assertFalse(policy.tryAcquireRetry());
        assertEquals(Duration.ZERO, policy.pauseBeforeRetry(1));
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void backoffDurationsAboveSixtySecondsFailClosed() {
        ReverseProxyProperties.Retry retry = retry(
                20, Duration.ofSeconds(1), Duration.ofSeconds(61));

        assertThrows(IllegalStateException.class, () -> ProxyRetryPolicy.compile(retry));
    }

    private static ReverseProxyProperties.Retry retry(
            int budgetPercent, Duration base, Duration max) {
        ReverseProxyProperties.Retry retry = new ReverseProxyProperties.Retry();
        retry.setBudgetPercent(budgetPercent);
        ReverseProxyProperties.Backoff backoff = new ReverseProxyProperties.Backoff();
        backoff.setBase(base);
        backoff.setMax(max);
        retry.setBackoff(backoff);
        return retry;
    }
}
