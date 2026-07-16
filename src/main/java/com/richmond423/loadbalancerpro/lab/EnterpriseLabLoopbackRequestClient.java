package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.ServerObservationOutcome;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackObservationIngress.BeginResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackObservationIngress.ObservationReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackObservationIngress.RequestAttempt;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;

/**
 * Synchronous bounded client for approved Enterprise Lab loopback targets only.
 */
public final class EnterpriseLabLoopbackRequestClient {
    public static final Duration DEFAULT_MAX_REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration HARD_MAX_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final Map<String, EnterpriseLabLoopbackTarget> targets;
    private final EnterpriseLabLoopbackObservationIngress ingress;
    private final Transport transport;
    private final Duration maxRequestTimeout;

    public EnterpriseLabLoopbackRequestClient(
            Collection<EnterpriseLabLoopbackTarget> targets,
            EnterpriseLabLoopbackObservationIngress ingress) {
        this(targets, ingress, new JdkTransport(), DEFAULT_MAX_REQUEST_TIMEOUT);
    }

    EnterpriseLabLoopbackRequestClient(
            Collection<EnterpriseLabLoopbackTarget> targets,
            EnterpriseLabLoopbackObservationIngress ingress,
            Transport transport,
            Duration maxRequestTimeout) {
        this.ingress = Objects.requireNonNull(ingress, "ingress cannot be null");
        this.transport = Objects.requireNonNull(transport, "transport cannot be null");
        Objects.requireNonNull(maxRequestTimeout, "maxRequestTimeout cannot be null");
        if (maxRequestTimeout.isZero() || maxRequestTimeout.isNegative()
                || maxRequestTimeout.compareTo(HARD_MAX_REQUEST_TIMEOUT) > 0) {
            throw new IllegalArgumentException("maxRequestTimeout must be positive and no greater than 30 seconds");
        }
        this.maxRequestTimeout = maxRequestTimeout;
        this.targets = createTargetMap(targets, ingress);
    }

    public Execution get(String requestId, String backendId, Duration timeout) {
        EnterpriseLabLoopbackTarget target = targets.get(backendId);
        if (target == null) {
            return Execution.rejected(requestId, backendId, "backend identity is not an approved loopback target");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative() || timeout.compareTo(maxRequestTimeout) > 0) {
            return Execution.rejected(requestId, backendId, "request timeout is outside the bounded policy");
        }

        BeginResult begin = ingress.begin(requestId, backendId);
        if (!begin.accepted()) {
            return Execution.rejected(requestId, backendId, begin.reason());
        }
        RequestAttempt attempt = begin.attempt().orElseThrow();
        try {
            int statusCode = transport.get(target.requestUri(), timeout);
            ObservationReceipt receipt = ingress.completeHttp(attempt, statusCode);
            ServerObservationOutcome outcome = statusCode >= 200 && statusCode <= 299
                    ? ServerObservationOutcome.SUCCESS
                    : ServerObservationOutcome.FAILURE;
            return Execution.completed(
                    requestId, target, outcome, OptionalInt.of(statusCode), receipt,
                    "approved loopback request completed with HTTP " + statusCode);
        } catch (HttpTimeoutException exception) {
            ObservationReceipt receipt = ingress.completeTimeout(attempt);
            return Execution.completed(
                    requestId, target, ServerObservationOutcome.TIMEOUT, OptionalInt.empty(), receipt,
                    "approved loopback request reached its timeout");
        } catch (ConnectException exception) {
            ObservationReceipt receipt = ingress.completeConnectionFailure(attempt, "loopback connection failed");
            return Execution.completed(
                    requestId, target, ServerObservationOutcome.CONNECTION_FAILURE, OptionalInt.empty(), receipt,
                    "approved loopback connection failed");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            ObservationReceipt receipt = ingress.completeFailure(attempt, "loopback request interrupted");
            return Execution.completed(
                    requestId, target, ServerObservationOutcome.FAILURE, OptionalInt.empty(), receipt,
                    "approved loopback request was interrupted");
        } catch (IOException exception) {
            ObservationReceipt receipt = ingress.completeConnectionFailure(
                    attempt, "loopback I/O failure: " + exception.getClass().getSimpleName());
            return Execution.completed(
                    requestId, target, ServerObservationOutcome.CONNECTION_FAILURE, OptionalInt.empty(), receipt,
                    "approved loopback request failed before an HTTP response");
        } catch (RuntimeException exception) {
            ObservationReceipt receipt = ingress.completeFailure(
                    attempt, "loopback transport failure: " + exception.getClass().getSimpleName());
            return Execution.completed(
                    requestId, target, ServerObservationOutcome.FAILURE, OptionalInt.empty(), receipt,
                    "approved loopback request failed safely");
        }
    }

    public ListView targets() {
        return new ListView(targets.size(), List.copyOf(targets.keySet()),
                "repository-approved literal loopback targets only");
    }

    private static Map<String, EnterpriseLabLoopbackTarget> createTargetMap(
            Collection<EnterpriseLabLoopbackTarget> targets,
            EnterpriseLabLoopbackObservationIngress ingress) {
        Objects.requireNonNull(targets, "targets cannot be null");
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("targets cannot be empty");
        }
        Map<String, EnterpriseLabLoopbackTarget> created = new TreeMap<>();
        for (EnterpriseLabLoopbackTarget target : targets) {
            EnterpriseLabLoopbackTarget safeTarget = Objects.requireNonNull(target, "targets cannot contain null");
            if (!ingress.approvedBackendIds().contains(safeTarget.backendId())) {
                throw new IllegalArgumentException("target backend identity is not approved by the observation ingress");
            }
            if (created.putIfAbsent(safeTarget.backendId(), safeTarget) != null) {
                throw new IllegalArgumentException("target backend identities must be unique");
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(created));
    }

    @FunctionalInterface
    interface Transport {
        int get(URI uri, Duration timeout) throws IOException, InterruptedException;
    }

    private static final class JdkTransport implements Transport {
        private final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_MAX_REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        @Override
        public int get(URI uri, Duration timeout) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .GET()
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        }
    }

    public record Execution(
            String requestId,
            String scenarioId,
            String backendId,
            boolean requestSent,
            Optional<ServerObservationOutcome> outcome,
            OptionalInt responseStatusCode,
            ObservationReceipt observationReceipt,
            String targetScope,
            String reason) {

        public Execution {
            requestId = requestId == null ? "" : requestId;
            scenarioId = scenarioId == null ? "" : scenarioId;
            backendId = backendId == null ? "" : backendId;
            outcome = Objects.requireNonNull(outcome, "outcome cannot be null");
            responseStatusCode = Objects.requireNonNull(responseStatusCode, "responseStatusCode cannot be null");
            observationReceipt = Objects.requireNonNull(observationReceipt, "observationReceipt cannot be null");
            targetScope = requireNonBlank(targetScope, "targetScope");
            reason = requireNonBlank(reason, "reason");
            if (requestSent != outcome.isPresent()) {
                throw new IllegalArgumentException("requestSent must match outcome presence");
            }
            if (!requestSent && responseStatusCode.isPresent()) {
                throw new IllegalArgumentException("rejected executions cannot have an HTTP status");
            }
        }

        private static Execution completed(
                String requestId,
                EnterpriseLabLoopbackTarget target,
                ServerObservationOutcome outcome,
                OptionalInt responseStatusCode,
                ObservationReceipt receipt,
                String reason) {
            return new Execution(
                    requestId,
                    target.scenarioId(),
                    target.backendId(),
                    true,
                    Optional.of(outcome),
                    responseStatusCode,
                    receipt,
                    "approved Enterprise Lab loopback target",
                    reason);
        }

        private static Execution rejected(String requestId, String backendId, String reason) {
            return new Execution(
                    requestId,
                    "",
                    backendId,
                    false,
                    Optional.empty(),
                    OptionalInt.empty(),
                    ObservationReceipt.rejected(reason),
                    "no target contacted",
                    reason);
        }
    }

    public record ListView(int count, java.util.List<String> backendIds, String boundary) {
        public ListView {
            if (count < 0) {
                throw new IllegalArgumentException("count cannot be negative");
            }
            backendIds = java.util.List.copyOf(Objects.requireNonNull(backendIds, "backendIds cannot be null"));
            boundary = requireNonBlank(boundary, "boundary");
            if (count != backendIds.size()) {
                throw new IllegalArgumentException("count must match backendIds size");
            }
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
