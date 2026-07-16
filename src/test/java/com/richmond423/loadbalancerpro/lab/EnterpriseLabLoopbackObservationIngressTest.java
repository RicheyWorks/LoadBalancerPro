package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.ServerObservation;
import com.richmond423.loadbalancerpro.core.ServerObservationOutcome;
import com.richmond423.loadbalancerpro.core.ServerObservationSource;
import com.richmond423.loadbalancerpro.core.ServerObservationWindowPolicy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackObservationIngress.BeginResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackObservationIngress.ObservationReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackObservationIngress.ReceiptStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabLoopbackObservationIngressTest {
    private static final Instant NOW = Instant.parse("2026-07-16T18:00:00Z");
    private static final ServerObservationWindowPolicy POLICY = new ServerObservationWindowPolicy(
            3,
            Duration.ofSeconds(30),
            2,
            2,
            0.25,
            0.75,
            2,
            2,
            0.5);

    @Test
    void classifiesAndMeasuresActualCompletionKinds() {
        AtomicLong nanos = new AtomicLong();
        EnterpriseLabLoopbackObservationIngress ingress = ingress(nanos, 4, Duration.ofMillis(100));

        BeginResult success = ingress.begin("request-1", "backend-a");
        nanos.addAndGet(Duration.ofMillis(7).toNanos());
        ObservationReceipt successReceipt = ingress.completeHttp(success.attempt().orElseThrow(), 204);
        assertRecorded(successReceipt, ServerObservationOutcome.SUCCESS, 7.0);

        BeginResult httpFailure = ingress.begin("request-2", "backend-a");
        nanos.addAndGet(Duration.ofMillis(11).toNanos());
        ObservationReceipt httpFailureReceipt = ingress.completeHttp(httpFailure.attempt().orElseThrow(), 503);
        assertRecorded(httpFailureReceipt, ServerObservationOutcome.FAILURE, 11.0);

        BeginResult timeout = ingress.begin("request-3", "backend-a");
        nanos.addAndGet(Duration.ofSeconds(2).toNanos());
        ObservationReceipt timeoutReceipt = ingress.completeTimeout(timeout.attempt().orElseThrow());
        assertRecorded(timeoutReceipt, ServerObservationOutcome.TIMEOUT, 100.0);

        BeginResult connectionFailure = ingress.begin("request-4", "backend-a");
        ObservationReceipt connectionReceipt = ingress.completeConnectionFailure(
                connectionFailure.attempt().orElseThrow(), "connection refused");
        assertEquals(ReceiptStatus.RECORDED, connectionReceipt.status());
        ServerObservation connectionObservation = connectionReceipt.observation().orElseThrow();
        assertEquals(ServerObservationOutcome.CONNECTION_FAILURE, connectionObservation.outcome());
        assertTrue(connectionObservation.latencyMillis().isEmpty());

        assertEquals(3, ingress.observations("backend-a").size(), "the immutable window must remain bounded");
        assertEquals(0, ingress.inFlightCount());
    }

    @Test
    void rejectsUnknownDuplicateAndExcessInFlightRequests() {
        AtomicLong nanos = new AtomicLong();
        EnterpriseLabLoopbackObservationIngress ingress = ingress(nanos, 2, Duration.ofSeconds(1));

        assertFalse(ingress.begin("unknown-request", "backend-unknown").accepted());

        BeginResult first = ingress.begin("request-a", "backend-a");
        BeginResult second = ingress.begin("request-b", "backend-a");
        assertTrue(first.accepted());
        assertTrue(second.accepted());
        assertFalse(ingress.begin("request-c", "backend-a").accepted());
        assertFalse(ingress.begin("request-a", "backend-a").accepted());

        ingress.completeHttp(first.attempt().orElseThrow(), 200);
        ObservationReceipt duplicate = ingress.completeHttp(first.attempt().orElseThrow(), 200);
        assertEquals(ReceiptStatus.DUPLICATE_IGNORED, duplicate.status());
        assertFalse(ingress.begin("request-a", "backend-a").accepted());

        ingress.completeHttp(second.attempt().orElseThrow(), 200);
        assertEquals(0, ingress.inFlightCount());
    }

    @Test
    void malformedCompletionIsRejectedAndReleasesCapacity() {
        AtomicLong nanos = new AtomicLong();
        EnterpriseLabLoopbackObservationIngress ingress = ingress(nanos, 1, Duration.ofSeconds(1));
        BeginResult started = ingress.begin("request-a", "backend-a");

        ObservationReceipt receipt = ingress.completeHttp(started.attempt().orElseThrow(), 99);

        assertEquals(ReceiptStatus.REJECTED, receipt.status());
        assertEquals(0, ingress.observations("backend-a").size());
        assertEquals(0, ingress.inFlightCount());
        assertTrue(ingress.begin("request-b", "backend-a").accepted());
    }

    @Test
    void foreignCompletionTokenCannotCorruptEitherIngress() {
        AtomicLong nanos = new AtomicLong();
        EnterpriseLabLoopbackObservationIngress first = ingress(nanos, 1, Duration.ofSeconds(1));
        EnterpriseLabLoopbackObservationIngress second = ingress(nanos, 1, Duration.ofSeconds(1));
        BeginResult started = first.begin("request-a", "backend-a");

        ObservationReceipt rejected = second.completeHttp(started.attempt().orElseThrow(), 200);

        assertEquals(ReceiptStatus.REJECTED, rejected.status());
        assertEquals(1, first.inFlightCount());
        assertEquals(0, second.inFlightCount());
        assertEquals(ReceiptStatus.RECORDED,
                first.completeHttp(started.attempt().orElseThrow(), 200).status());
    }

    @Test
    void recordingFailureIsStructuredAndNeverEscapesToRequestPath() {
        AtomicInteger calls = new AtomicInteger();
        EnterpriseLabLoopbackObservationIngress ingress = new EnterpriseLabLoopbackObservationIngress(
                Set.of("backend-a"),
                POLICY,
                1,
                Duration.ofSeconds(1),
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> {
                    if (calls.getAndIncrement() == 0) {
                        return 0L;
                    }
                    throw new IllegalStateException("measurement source unavailable");
                });
        BeginResult started = ingress.begin("request-a", "backend-a");

        ObservationReceipt receipt = ingress.completeHttp(started.attempt().orElseThrow(), 200);

        assertEquals(ReceiptStatus.RECORDING_FAILED, receipt.status());
        assertTrue(receipt.reason().contains("IllegalStateException"));
        assertEquals(0, ingress.inFlightCount());
        assertTrue(ingress.observations("backend-a").isEmpty());
    }

    @Test
    void requestStartFailureAndOversizedIdentityAreRejectedWithoutLeakingCapacity() {
        EnterpriseLabLoopbackObservationIngress ingress = new EnterpriseLabLoopbackObservationIngress(
                Set.of("backend-a"),
                POLICY,
                1,
                Duration.ofSeconds(1),
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> {
                    throw new IllegalStateException("measurement source unavailable");
                });

        BeginResult measurementFailure = ingress.begin("request-a", "backend-a");

        assertFalse(measurementFailure.accepted());
        assertTrue(measurementFailure.reason().contains("IllegalStateException"));
        assertEquals(0, ingress.inFlightCount());
        assertFalse(ingress.begin("x".repeat(129), "backend-a").accepted());
        assertFalse(ingress.begin("request with spaces", "backend-a").accepted());
    }

    @Test
    void approvedBackendRegistryHasAHardBound() {
        Set<String> tooManyBackendIds = IntStream
                .rangeClosed(1, EnterpriseLabLoopbackObservationIngress.MAX_APPROVED_BACKENDS + 1)
                .mapToObj(index -> "backend-" + index)
                .collect(java.util.stream.Collectors.toSet());

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new EnterpriseLabLoopbackObservationIngress(tooManyBackendIds));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new EnterpriseLabLoopbackObservationIngress(
                        Set.of("backend-a"),
                        POLICY,
                        257,
                        Duration.ofSeconds(1),
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        () -> 0L));
    }

    @Test
    void rollingSnapshotUsesRecordedLoopbackEvidence() {
        AtomicLong nanos = new AtomicLong();
        EnterpriseLabLoopbackObservationIngress ingress = ingress(nanos, 4, Duration.ofSeconds(1));
        for (int index = 1; index <= 3; index++) {
            BeginResult started = ingress.begin("request-" + index, "backend-a");
            nanos.addAndGet(Duration.ofMillis(index).toNanos());
            ingress.completeHttp(started.attempt().orElseThrow(), 200);
        }

        var state = ingress.snapshot("backend-a", NOW);

        assertEquals(3, state.sampleCount());
        assertTrue(state.sufficientEvidence());
        assertEquals(3, state.successCount());
        assertEquals(java.util.List.of(ServerObservationSource.ENTERPRISE_LAB_LOOPBACK), state.sources());
    }

    private static EnterpriseLabLoopbackObservationIngress ingress(
            AtomicLong nanos,
            int maxInFlight,
            Duration maxLatency) {
        return new EnterpriseLabLoopbackObservationIngress(
                Set.of("backend-a"),
                POLICY,
                maxInFlight,
                maxLatency,
                Clock.fixed(NOW, ZoneOffset.UTC),
                nanos::get);
    }

    private static void assertRecorded(
            ObservationReceipt receipt,
            ServerObservationOutcome expectedOutcome,
            double expectedLatencyMillis) {
        assertEquals(ReceiptStatus.RECORDED, receipt.status());
        ServerObservation observation = receipt.observation().orElseThrow();
        assertEquals(expectedOutcome, observation.outcome());
        assertEquals(ServerObservationSource.ENTERPRISE_LAB_LOOPBACK, observation.source());
        assertEquals(expectedLatencyMillis, observation.latencyMillis().orElseThrow(), 0.000_001);
    }
}
