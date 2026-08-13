package com.richmond423.loadbalancerpro.api.proxy;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class ReverseProxyWebSocketPlan implements AutoCloseable {
    private enum Lifecycle {
        PLANNED,
        CLAIMED,
        COMPLETED
    }

    @FunctionalInterface
    interface Completion {
        void complete(
                boolean connected,
                boolean successful,
                boolean upstreamFailure,
                ReverseProxyMetrics.TerminalOutcome outcome,
                Duration elapsed);
    }

    private final String routeName;
    private final String upstreamId;
    private final String strategyName;
    private final URI targetUri;
    private final HttpClient httpClient;
    private final Map<String, List<String>> headers;
    private final Duration connectTimeout;
    private final Duration sendTimeout;
    private final int sendBufferBytes;
    private final Completion completion;
    private final long startedAtNanos = System.nanoTime();
    private final AtomicBoolean connected = new AtomicBoolean();
    private final AtomicReference<Lifecycle> lifecycle = new AtomicReference<>(Lifecycle.PLANNED);

    ReverseProxyWebSocketPlan(
            String routeName,
            String upstreamId,
            String strategyName,
            URI targetUri,
            HttpClient httpClient,
            Map<String, List<String>> headers,
            Duration connectTimeout,
            Duration sendTimeout,
            int sendBufferBytes,
            Completion completion) {
        this.routeName = Objects.requireNonNull(routeName, "routeName cannot be null");
        this.upstreamId = Objects.requireNonNull(upstreamId, "upstreamId cannot be null");
        this.strategyName = Objects.requireNonNull(strategyName, "strategyName cannot be null");
        this.targetUri = Objects.requireNonNull(targetUri, "targetUri cannot be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.headers = Map.copyOf(Objects.requireNonNull(headers, "headers cannot be null"));
        this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout cannot be null");
        this.sendTimeout = Objects.requireNonNull(sendTimeout, "sendTimeout cannot be null");
        this.sendBufferBytes = sendBufferBytes;
        this.completion = Objects.requireNonNull(completion, "completion cannot be null");
        CompletableFuture.delayedExecutor(
                connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .execute(this::completeIfUnclaimed);
    }

    String routeName() {
        return routeName;
    }

    String upstreamId() {
        return upstreamId;
    }

    String strategyName() {
        return strategyName;
    }

    URI targetUri() {
        return targetUri;
    }

    HttpClient httpClient() {
        return httpClient;
    }

    Map<String, List<String>> headers() {
        return headers;
    }

    Duration connectTimeout() {
        return connectTimeout;
    }

    Duration sendTimeout() {
        return sendTimeout;
    }

    int sendBufferBytes() {
        return sendBufferBytes;
    }

    void markConnected() {
        connected.set(true);
    }

    boolean claim() {
        return lifecycle.compareAndSet(Lifecycle.PLANNED, Lifecycle.CLAIMED);
    }

    void complete(
            boolean successful,
            boolean upstreamFailure,
            ReverseProxyMetrics.TerminalOutcome outcome) {
        Lifecycle current;
        do {
            current = lifecycle.get();
            if (current == Lifecycle.COMPLETED) {
                return;
            }
        } while (!lifecycle.compareAndSet(current, Lifecycle.COMPLETED));
        notifyCompletion(successful, upstreamFailure, outcome);
    }

    private void completeIfUnclaimed() {
        if (lifecycle.compareAndSet(Lifecycle.PLANNED, Lifecycle.COMPLETED)) {
            notifyCompletion(false, false, ReverseProxyMetrics.TerminalOutcome.INTERNAL_ERROR);
        }
    }

    private void notifyCompletion(
            boolean successful,
            boolean upstreamFailure,
            ReverseProxyMetrics.TerminalOutcome outcome) {
        completion.complete(
                connected.get(),
                successful,
                upstreamFailure,
                Objects.requireNonNull(outcome, "outcome cannot be null"),
                Duration.ofNanos(Math.max(0, System.nanoTime() - startedAtNanos)));
    }

    @Override
    public void close() {
        complete(false, false, ReverseProxyMetrics.TerminalOutcome.INTERNAL_ERROR);
    }
}
