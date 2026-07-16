package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.ServerObservation;
import com.richmond423.loadbalancerpro.core.ServerObservationOutcome;
import com.richmond423.loadbalancerpro.core.ServerObservationSource;
import com.richmond423.loadbalancerpro.core.ServerObservationWindow;
import com.richmond423.loadbalancerpro.core.ServerObservationWindowPolicy;
import com.richmond423.loadbalancerpro.core.ServerRollingSignalState;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/**
 * Bounded, concurrency-safe ingress for actual Enterprise Lab loopback outcomes.
 */
public final class EnterpriseLabLoopbackObservationIngress {
    public static final int DEFAULT_MAX_IN_FLIGHT_REQUESTS = 32;
    public static final Duration DEFAULT_MAX_MEASURED_LATENCY = Duration.ofSeconds(10);
    public static final int MAX_APPROVED_BACKENDS = 64;
    private static final int MAX_ID_LENGTH = 128;
    private static final int HARD_MAX_IN_FLIGHT_REQUESTS = 256;
    private static final int HARD_MAX_WINDOW_SAMPLES = 256;
    private static final Duration HARD_MAX_MEASURED_LATENCY = Duration.ofMinutes(1);

    private final Map<String, AtomicReference<ServerObservationWindow>> windows;
    private final ConcurrentMap<String, RequestAttempt> inFlight = new ConcurrentHashMap<>();
    private final AtomicInteger reservedInFlight = new AtomicInteger();
    private final ServerObservationWindowPolicy windowPolicy;
    private final int maxInFlightRequests;
    private final long maxMeasuredLatencyNanos;
    private final Clock clock;
    private final LongSupplier monotonicNanos;

    public EnterpriseLabLoopbackObservationIngress(Collection<String> approvedBackendIds) {
        this(
                approvedBackendIds,
                ServerObservationWindowPolicy.localLabDefaults(),
                DEFAULT_MAX_IN_FLIGHT_REQUESTS,
                DEFAULT_MAX_MEASURED_LATENCY,
                Clock.systemUTC(),
                System::nanoTime);
    }

    EnterpriseLabLoopbackObservationIngress(
            Collection<String> approvedBackendIds,
            ServerObservationWindowPolicy windowPolicy,
            int maxInFlightRequests,
            Duration maxMeasuredLatency,
            Clock clock,
            LongSupplier monotonicNanos) {
        this.windowPolicy = Objects.requireNonNull(windowPolicy, "windowPolicy cannot be null");
        if (maxInFlightRequests < 1 || maxInFlightRequests > HARD_MAX_IN_FLIGHT_REQUESTS) {
            throw new IllegalArgumentException("maxInFlightRequests must be between 1 and 256");
        }
        this.maxInFlightRequests = maxInFlightRequests;
        if (windowPolicy.maxSampleCount() > HARD_MAX_WINDOW_SAMPLES) {
            throw new IllegalArgumentException("observation window cannot retain more than 256 samples per backend");
        }
        Objects.requireNonNull(maxMeasuredLatency, "maxMeasuredLatency cannot be null");
        if (maxMeasuredLatency.isZero() || maxMeasuredLatency.isNegative()
                || maxMeasuredLatency.compareTo(HARD_MAX_MEASURED_LATENCY) > 0) {
            throw new IllegalArgumentException("maxMeasuredLatency must be positive and no greater than one minute");
        }
        this.maxMeasuredLatencyNanos = maxMeasuredLatency.toNanos();
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.monotonicNanos = Objects.requireNonNull(monotonicNanos, "monotonicNanos cannot be null");
        this.windows = createWindows(approvedBackendIds, windowPolicy);
    }

    public BeginResult begin(String requestId, String backendId) {
        String safeRequestId = normalizedId(requestId);
        String safeBackendId = normalizedId(backendId);
        if (safeRequestId == null || safeBackendId == null) {
            return BeginResult.rejected(requestId, backendId, "requestId and backendId must be canonical non-blank IDs");
        }
        AtomicReference<ServerObservationWindow> window = windows.get(safeBackendId);
        if (window == null) {
            return BeginResult.rejected(safeRequestId, safeBackendId, "backend identity is not approved");
        }
        boolean completedId = window.get().observations().stream()
                .anyMatch(observation -> observation.observationId().equals(safeRequestId));
        if (completedId) {
            return BeginResult.rejected(safeRequestId, safeBackendId, "requestId already has a completed observation");
        }
        if (!reserveInFlight()) {
            return BeginResult.rejected(safeRequestId, safeBackendId, "bounded in-flight request limit reached");
        }

        RequestAttempt attempt;
        try {
            attempt = new RequestAttempt(
                    this,
                    safeRequestId,
                    safeBackendId,
                    clock.instant(),
                    monotonicNanos.getAsLong());
        } catch (RuntimeException exception) {
            releaseInFlight();
            return BeginResult.rejected(
                    safeRequestId,
                    safeBackendId,
                    "request measurement could not start: " + exception.getClass().getSimpleName());
        }
        RequestAttempt previous = inFlight.putIfAbsent(safeRequestId, attempt);
        if (previous != null) {
            releaseInFlight();
            return BeginResult.rejected(safeRequestId, safeBackendId, "requestId is already in flight");
        }
        return BeginResult.accepted(attempt);
    }

    public ObservationReceipt completeHttp(RequestAttempt attempt, int statusCode) {
        if (statusCode < 100 || statusCode > 599) {
            return rejectCompletion(attempt, "HTTP status code must be between 100 and 599");
        }
        ServerObservationOutcome outcome = statusCode >= 200 && statusCode <= 299
                ? ServerObservationOutcome.SUCCESS
                : ServerObservationOutcome.FAILURE;
        return complete(attempt, outcome, true, "HTTP " + statusCode);
    }

    public ObservationReceipt completeTimeout(RequestAttempt attempt) {
        return complete(attempt, ServerObservationOutcome.TIMEOUT, true, "request timeout");
    }

    public ObservationReceipt completeConnectionFailure(RequestAttempt attempt, String reason) {
        return complete(attempt, ServerObservationOutcome.CONNECTION_FAILURE, false,
                safeReason(reason, "connection failure"));
    }

    public ObservationReceipt completeFailure(RequestAttempt attempt, String reason) {
        return complete(attempt, ServerObservationOutcome.FAILURE, true,
                safeReason(reason, "request failure"));
    }

    public List<ServerObservation> observations(String backendId) {
        return approvedWindow(backendId).get().observations();
    }

    public ServerRollingSignalState snapshot(String backendId, Instant evaluatedAt) {
        return approvedWindow(backendId).get().snapshot(
                Objects.requireNonNull(evaluatedAt, "evaluatedAt cannot be null"));
    }

    public List<String> approvedBackendIds() {
        return List.copyOf(windows.keySet());
    }

    public int inFlightCount() {
        return reservedInFlight.get();
    }

    public int maxInFlightRequests() {
        return maxInFlightRequests;
    }

    public ServerObservationWindowPolicy observationWindowPolicy() {
        return windowPolicy;
    }

    private ObservationReceipt rejectCompletion(RequestAttempt attempt, String reason) {
        if (alreadyCompleted(attempt)) {
            return duplicateReceipt(attempt);
        }
        if (!owns(attempt)) {
            return ObservationReceipt.rejected(reason);
        }
        if (!attempt.completed.compareAndSet(false, true)) {
            return duplicateReceipt(attempt);
        }
        removeAttempt(attempt);
        return new ObservationReceipt(
                ReceiptStatus.REJECTED,
                attempt.requestId,
                attempt.backendId,
                Optional.empty(),
                observations(attempt.backendId).size(),
                reason);
    }

    private ObservationReceipt complete(
            RequestAttempt attempt,
            ServerObservationOutcome outcome,
            boolean includeElapsed,
            String reason) {
        if (alreadyCompleted(attempt)) {
            return duplicateReceipt(attempt);
        }
        if (!owns(attempt)) {
            return ObservationReceipt.rejected("completion token is unknown or belongs to another ingress");
        }
        if (!attempt.completed.compareAndSet(false, true)) {
            return duplicateReceipt(attempt);
        }
        try {
            Instant observedAt = clock.instant();
            if (observedAt.isBefore(attempt.startedAt)) {
                observedAt = attempt.startedAt;
            }
            double elapsedMillis = measuredLatencyMillis(attempt.startedNanos);
            OptionalDouble latency = includeElapsed ? OptionalDouble.of(elapsedMillis) : OptionalDouble.empty();
            ServerObservation observation = new ServerObservation(
                    attempt.requestId,
                    attempt.backendId,
                    ServerObservationSource.ENTERPRISE_LAB_LOOPBACK,
                    outcome,
                    latency,
                    observedAt);
            int windowSize = append(observation, observedAt);
            return new ObservationReceipt(
                    ReceiptStatus.RECORDED,
                    attempt.requestId,
                    attempt.backendId,
                    Optional.of(observation),
                    windowSize,
                    reason);
        } catch (RuntimeException exception) {
            return new ObservationReceipt(
                    ReceiptStatus.RECORDING_FAILED,
                    attempt.requestId,
                    attempt.backendId,
                    Optional.empty(),
                    observations(attempt.backendId).size(),
                    "observation recording failed: " + exception.getClass().getSimpleName());
        } finally {
            removeAttempt(attempt);
        }
    }

    private int append(ServerObservation observation, Instant receivedAt) {
        AtomicReference<ServerObservationWindow> reference = windows.get(observation.serverId());
        while (true) {
            ServerObservationWindow current = reference.get();
            ServerObservationWindow updated = current.append(observation, receivedAt);
            if (reference.compareAndSet(current, updated)) {
                return updated.size();
            }
        }
    }

    private boolean reserveInFlight() {
        while (true) {
            int current = reservedInFlight.get();
            if (current >= maxInFlightRequests) {
                return false;
            }
            if (reservedInFlight.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    private void releaseInFlight() {
        reservedInFlight.updateAndGet(current -> Math.max(0, current - 1));
    }

    private void removeAttempt(RequestAttempt attempt) {
        if (inFlight.remove(attempt.requestId, attempt)) {
            releaseInFlight();
        }
    }

    private boolean owns(RequestAttempt attempt) {
        return attempt != null && attempt.owner == this && inFlight.get(attempt.requestId) == attempt;
    }

    private boolean alreadyCompleted(RequestAttempt attempt) {
        return attempt != null && attempt.owner == this && attempt.completed.get();
    }

    private ObservationReceipt duplicateReceipt(RequestAttempt attempt) {
        return ObservationReceipt.duplicate(
                attempt.requestId,
                attempt.backendId,
                observations(attempt.backendId).size());
    }

    private double measuredLatencyMillis(long startedNanos) {
        long elapsed;
        try {
            elapsed = Math.subtractExact(monotonicNanos.getAsLong(), startedNanos);
        } catch (ArithmeticException exception) {
            elapsed = maxMeasuredLatencyNanos;
        }
        long bounded = Math.min(maxMeasuredLatencyNanos, Math.max(0L, elapsed));
        return bounded / 1_000_000.0;
    }

    private AtomicReference<ServerObservationWindow> approvedWindow(String backendId) {
        String safeBackendId = normalizedId(backendId);
        AtomicReference<ServerObservationWindow> window = safeBackendId == null ? null : windows.get(safeBackendId);
        if (window == null) {
            throw new IllegalArgumentException("backend identity is not approved");
        }
        return window;
    }

    private static Map<String, AtomicReference<ServerObservationWindow>> createWindows(
            Collection<String> approvedBackendIds,
            ServerObservationWindowPolicy policy) {
        Objects.requireNonNull(approvedBackendIds, "approvedBackendIds cannot be null");
        if (approvedBackendIds.isEmpty()) {
            throw new IllegalArgumentException("approvedBackendIds cannot be empty");
        }
        if (approvedBackendIds.size() > MAX_APPROVED_BACKENDS) {
            throw new IllegalArgumentException("approvedBackendIds exceeds the bounded local-lab maximum");
        }
        Map<String, AtomicReference<ServerObservationWindow>> created = new TreeMap<>();
        for (String backendId : approvedBackendIds) {
            String safeBackendId = normalizedId(backendId);
            if (safeBackendId == null) {
                throw new IllegalArgumentException("approved backend IDs must be canonical and non-blank");
            }
            if (created.putIfAbsent(safeBackendId, new AtomicReference<>(
                    ServerObservationWindow.create(safeBackendId, policy))) != null) {
                throw new IllegalArgumentException("approved backend IDs must be unique");
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(created));
    }

    private static String normalizedId(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > MAX_ID_LENGTH || !value.matches("[A-Za-z0-9._:-]+")) {
            return null;
        }
        return value;
    }

    private static String safeReason(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String singleLine = value.replace('\r', ' ').replace('\n', ' ').trim();
        return singleLine.length() <= 160 ? singleLine : singleLine.substring(0, 160);
    }

    public enum ReceiptStatus {
        RECORDED,
        DUPLICATE_IGNORED,
        REJECTED,
        RECORDING_FAILED
    }

    public record BeginResult(
            String requestId,
            String backendId,
            boolean accepted,
            Optional<RequestAttempt> attempt,
            String reason) {

        public BeginResult {
            requestId = requestId == null ? "" : requestId;
            backendId = backendId == null ? "" : backendId;
            attempt = Objects.requireNonNull(attempt, "attempt cannot be null");
            reason = safeReason(reason, "unspecified begin result");
            if (accepted != attempt.isPresent()) {
                throw new IllegalArgumentException("accepted must match attempt presence");
            }
        }

        private static BeginResult accepted(RequestAttempt attempt) {
            return new BeginResult(attempt.requestId, attempt.backendId, true, Optional.of(attempt),
                    "request measurement started");
        }

        private static BeginResult rejected(String requestId, String backendId, String reason) {
            return new BeginResult(requestId, backendId, false, Optional.empty(), reason);
        }
    }

    public record ObservationReceipt(
            ReceiptStatus status,
            String requestId,
            String backendId,
            Optional<ServerObservation> observation,
            int windowSize,
            String reason) {

        public ObservationReceipt {
            status = Objects.requireNonNull(status, "status cannot be null");
            requestId = requestId == null ? "" : requestId;
            backendId = backendId == null ? "" : backendId;
            observation = Objects.requireNonNull(observation, "observation cannot be null");
            if (windowSize < 0) {
                throw new IllegalArgumentException("windowSize cannot be negative");
            }
            reason = safeReason(reason, "unspecified observation result");
            if ((status == ReceiptStatus.RECORDED) != observation.isPresent()) {
                throw new IllegalArgumentException("only recorded receipts contain an observation");
            }
        }

        private static ObservationReceipt duplicate(String requestId, String backendId, int windowSize) {
            return new ObservationReceipt(ReceiptStatus.DUPLICATE_IGNORED, requestId, backendId,
                    Optional.empty(), windowSize, "completion was already recorded or rejected");
        }

        static ObservationReceipt rejected(String reason) {
            return new ObservationReceipt(ReceiptStatus.REJECTED, "", "", Optional.empty(), 0, reason);
        }

        public boolean recorded() {
            return status == ReceiptStatus.RECORDED;
        }
    }

    public static final class RequestAttempt {
        private final EnterpriseLabLoopbackObservationIngress owner;
        private final String requestId;
        private final String backendId;
        private final Instant startedAt;
        private final long startedNanos;
        private final AtomicBoolean completed = new AtomicBoolean();

        private RequestAttempt(
                EnterpriseLabLoopbackObservationIngress owner,
                String requestId,
                String backendId,
                Instant startedAt,
                long startedNanos) {
            this.owner = owner;
            this.requestId = requestId;
            this.backendId = backendId;
            this.startedAt = startedAt;
            this.startedNanos = startedNanos;
        }

        public String requestId() {
            return requestId;
        }

        public String backendId() {
            return backendId;
        }

        public Instant startedAt() {
            return startedAt;
        }
    }
}
