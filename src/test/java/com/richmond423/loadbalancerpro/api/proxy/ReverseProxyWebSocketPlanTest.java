package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ReverseProxyWebSocketPlanTest {
    @Test
    void unclaimedHandshakeLeaseExpiresButClaimedPlanCompletesOnlyAtTunnelClose() throws Exception {
        AtomicInteger abandonedCompletions = new AtomicInteger();
        CountDownLatch abandoned = new CountDownLatch(1);
        newPlan(Duration.ofMillis(20), () -> {
            abandonedCompletions.incrementAndGet();
            abandoned.countDown();
        });

        assertTrue(abandoned.await(1, TimeUnit.SECONDS));
        assertEquals(1, abandonedCompletions.get());

        AtomicInteger claimedCompletions = new AtomicInteger();
        CountDownLatch claimed = new CountDownLatch(1);
        ReverseProxyWebSocketPlan plan = newPlan(Duration.ofMillis(20), () -> {
            claimedCompletions.incrementAndGet();
            claimed.countDown();
        });
        assertTrue(plan.claim());
        assertFalse(claimed.await(100, TimeUnit.MILLISECONDS));

        plan.complete(true, false, ReverseProxyMetrics.TerminalOutcome.SUCCESS);
        plan.close();
        assertTrue(claimed.await(1, TimeUnit.SECONDS));
        assertEquals(1, claimedCompletions.get());
    }

    private static ReverseProxyWebSocketPlan newPlan(Duration connectTimeout, Runnable completion) {
        return new ReverseProxyWebSocketPlan(
                "route",
                "upstream",
                "ROUND_ROBIN",
                URI.create("ws://127.0.0.1/socket"),
                HttpClient.newHttpClient(),
                Map.of(),
                connectTimeout,
                Duration.ofSeconds(1),
                1_024,
                (connected, successful, upstreamFailure, outcome, elapsed) -> completion.run());
    }
}
