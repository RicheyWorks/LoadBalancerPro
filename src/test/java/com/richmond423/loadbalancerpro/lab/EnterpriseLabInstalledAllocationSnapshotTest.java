package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabInstalledAllocationSnapshotCodec.CodecException;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabInstalledAllocationSnapshotCodec.Failure;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabInstalledAllocationSnapshotTest {
    private static final String SCENARIO = "tail-latency-pressure";
    private static final Instant NOW = Instant.parse("2026-07-18T11:00:00Z");
    private static final Map<String, Double> BASELINE =
            Map.of("blue", 0.5, "green", 0.25, "orange", 0.25);

    @TempDir
    Path temporaryDirectory;

    private List<EnterpriseLabLoopbackTarget> targets;
    private EnterpriseLabExperimentTargetCatalog targetCatalog;
    private EnterpriseLabMutationTestAuthority authority;
    private MutableClock clock;
    private EnterpriseLabInstalledAllocationSnapshotCodec codec;

    @BeforeEach
    void setUp() {
        targets = targets();
        targetCatalog = new EnterpriseLabExperimentTargetCatalog(targets);
        authority = new EnterpriseLabMutationTestAuthority(temporaryDirectory);
        clock = new MutableClock(NOW);
        codec = new EnterpriseLabInstalledAllocationSnapshotCodec(targetCatalog);
    }

    @Test
    void safeDefaultReadBackIsCompleteClockedOwnedAndFingerprintEquivalent() {
        EnterpriseLabLoopbackAllocationRouter router = ownedRouter();

        EnterpriseLabInstalledAllocationSnapshot installed = router.installedSnapshot();

        assertSame(installed, router.installedSnapshot());
        assertSame(installed.routingSnapshot(), router.currentSnapshot());
        assertEquals(installed, router.baselineInstalledSnapshot());
        assertTrue(installed.safeDefault());
        assertEquals(0L, installed.routerGeneration());
        assertEquals(1L, installed.ownerGeneration());
        assertEquals(NOW, installed.installedAt());
        assertEquals("SAFE_DEFAULT_INITIALIZED", installed.installationReason());
        assertEquals(List.of("blue", "green", "orange"), installed.eligibleBackendIds());
        assertTrue(installed.excludedBackendIds().isEmpty());
        assertEquals(
                EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                        SCENARIO, BASELINE),
                installed.allocationFingerprint());
        assertEquals(installed, codec.decode(codec.encode(installed)));
    }

    @Test
    void candidateAndRestoreAtomicallyAdvanceRouterAndInstallationProvenance() {
        EnterpriseLabAdaptiveDecision candidate = new EnterpriseLabAdaptiveDecisionService()
                .decide(SCENARIO, "active-experiment", true, false, false);
        EnterpriseLabLoopbackAllocationRouter router = ownedRouter(
                candidate.decision().request().baselineAllocations());
        clock.advanceSeconds(5);

        var applied = router.applyCandidate(candidate, true);
        EnterpriseLabInstalledAllocationSnapshot candidateInstalled = router.installedSnapshot();

        assertEquals(Kind.CANDIDATE, candidateInstalled.routingSnapshot().kind());
        assertSame(candidateInstalled.routingSnapshot(), applied.currentSnapshot());
        assertEquals(1L, candidateInstalled.routerGeneration());
        assertEquals(1L, candidateInstalled.ownerGeneration());
        assertEquals(NOW.plusSeconds(5), candidateInstalled.installedAt());
        assertEquals("APPROVED_CANDIDATE_APPLIED", candidateInstalled.installationReason());
        assertEquals(
                EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                        SCENARIO,
                        EnterpriseLabLoopbackAllocationSnapshot.exactNormalizedAllocations(
                                Set.of("blue", "green", "orange"),
                                candidate.decision().guardrailDecision().effectiveAllocations())),
                candidateInstalled.allocationFingerprint());

        clock.advanceSeconds(7);
        var restored = router.restoreBaseline("focused verification complete");
        EnterpriseLabInstalledAllocationSnapshot baselineInstalled = router.installedSnapshot();
        assertEquals(Kind.RESTORED_BASELINE, baselineInstalled.routingSnapshot().kind());
        assertSame(baselineInstalled.routingSnapshot(), restored.currentSnapshot());
        assertEquals(2L, baselineInstalled.routerGeneration());
        assertEquals(NOW.plusSeconds(12), baselineInstalled.installedAt());
        assertEquals("BASELINE_RESTORED: focused verification complete",
                baselineInstalled.installationReason());
        assertFalse(baselineInstalled.safeDefault());
        assertNotEquals(candidateInstalled.allocationFingerprint(),
                baselineInstalled.allocationFingerprint());
    }

    @Test
    void takeoverGenerationIsRecordedAndRegressionFailsClosed() {
        EnterpriseLabAdaptiveDecision candidate = new EnterpriseLabAdaptiveDecisionService()
                .decide(SCENARIO, "active-experiment", true, false, false);
        EnterpriseLabLoopbackAllocationRouter router = ownedRouter(
                candidate.decision().request().baselineAllocations());
        authority.replaceOwner("takeover-owner", 2);

        router.applyCandidate(candidate, true);
        EnterpriseLabInstalledAllocationSnapshot generationTwo = router.installedSnapshot();
        assertEquals(2L, generationTwo.ownerGeneration());

        authority.replaceOwner("regressed-owner", 1);
        EnterpriseLabEvidenceOwnershipException regression = assertThrows(
                EnterpriseLabEvidenceOwnershipException.class,
                () -> router.restoreBaseline("regression rejected"));
        assertEquals(EnterpriseLabEvidenceOwnership.FailureClassification.RECORD_REPLACED,
                regression.classification());
        assertSame(generationTwo, router.installedSnapshot());

        authority.fail(EnterpriseLabEvidenceOwnership.FailureClassification.LOCK_LOST);
        assertSame(generationTwo, router.installedSnapshot());
        assertThrows(EnterpriseLabEvidenceOwnershipException.class,
                () -> router.applyCandidate(candidate, true));
        assertSame(generationTwo, router.installedSnapshot());
    }

    @Test
    void canonicalEvidenceRoundTripsIndependentOfInputMapOrder() {
        Map<String, Double> reordered = new LinkedHashMap<>();
        reordered.put("orange", 0.25);
        reordered.put("blue", 0.5);
        reordered.put("green", 0.25);
        EnterpriseLabLoopbackAllocationSnapshot firstRouting =
                EnterpriseLabLoopbackAllocationSnapshot.normalized(
                        SCENARIO, 4, "decision-4", Kind.CANDIDATE,
                        Set.of("blue", "green", "orange"), BASELINE);
        EnterpriseLabLoopbackAllocationSnapshot secondRouting =
                EnterpriseLabLoopbackAllocationSnapshot.normalized(
                        SCENARIO, 4, "decision-4", Kind.CANDIDATE,
                        Set.of("orange", "green", "blue"), reordered);
        EnterpriseLabInstalledAllocationSnapshot first =
                EnterpriseLabInstalledAllocationSnapshot.installed(
                        firstRouting, Clock.fixed(NOW, ZoneOffset.UTC),
                        "CANONICAL_EVIDENCE", 3);
        EnterpriseLabInstalledAllocationSnapshot second =
                EnterpriseLabInstalledAllocationSnapshot.installed(
                        secondRouting, Clock.fixed(NOW, ZoneOffset.UTC),
                        "CANONICAL_EVIDENCE", 3);

        assertEquals(first, second);
        assertArrayEquals(codec.encode(first), codec.encode(second));
        String json = new String(codec.encode(first), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"blue\":\"0x1.0p-1\""));
        assertTrue(json.contains("\"installedAt\":\"2026-07-18T11:00:00Z\""));
        assertEquals(first, codec.decode(codec.encode(first)));
    }

    @Test
    void codecRejectsTamperingUnknownFieldsDuplicatesNonCanonicalAndBounds() {
        EnterpriseLabInstalledAllocationSnapshot installed = ownedRouter().installedSnapshot();
        String encoded = new String(codec.encode(installed), StandardCharsets.UTF_8);

        CodecException fingerprint = assertThrows(CodecException.class, () -> codec.decode(bytes(
                encoded.replace(installed.allocationFingerprint(), "0".repeat(64)))));
        assertEquals(Failure.INVALID_SNAPSHOT, fingerprint.failure());

        CodecException unknown = assertThrows(CodecException.class, () -> codec.decode(bytes(
                encoded.replaceFirst("\\{", "{\"unexpected\":true,"))));
        assertEquals(Failure.UNKNOWN_FIELD, unknown.failure());

        String schemaField = "\"schemaVersion\":\""
                + EnterpriseLabInstalledAllocationSnapshot.SCHEMA_VERSION + "\",";
        CodecException duplicate = assertThrows(CodecException.class, () -> codec.decode(bytes(
                encoded.replace(schemaField, schemaField + schemaField))));
        assertEquals(Failure.MALFORMED_SNAPSHOT, duplicate.failure());

        CodecException whitespace = assertThrows(CodecException.class,
                () -> codec.decode(bytes(" " + encoded)));
        assertEquals(Failure.NON_CANONICAL_SNAPSHOT, whitespace.failure());

        byte[] oversized = new byte[
                EnterpriseLabInstalledAllocationSnapshotCodec.HARD_MAX_SNAPSHOT_BYTES + 1];
        Arrays.fill(oversized, (byte) 'x');
        CodecException bounds = assertThrows(CodecException.class, () -> codec.decode(oversized));
        assertEquals(Failure.EXCEEDED_BOUNDS, bounds.failure());
    }

    @Test
    void modelRejectsFingerprintEligibilityGenerationAndSensitiveReasonMismatch() {
        EnterpriseLabInstalledAllocationSnapshot installed = ownedRouter().installedSnapshot();

        assertThrows(IllegalArgumentException.class, () -> new EnterpriseLabInstalledAllocationSnapshot(
                installed.schemaVersion(), installed.routingSnapshot(), installed.routerGeneration(),
                "0".repeat(64), installed.eligibleBackendIds(), installed.excludedBackendIds(),
                installed.installedAt(), installed.installationReason(), installed.ownerGeneration()));
        assertThrows(IllegalArgumentException.class, () -> new EnterpriseLabInstalledAllocationSnapshot(
                installed.schemaVersion(), installed.routingSnapshot(), installed.routerGeneration(),
                installed.allocationFingerprint(), List.of("blue"), installed.excludedBackendIds(),
                installed.installedAt(), installed.installationReason(), installed.ownerGeneration()));
        assertThrows(IllegalArgumentException.class, () -> new EnterpriseLabInstalledAllocationSnapshot(
                installed.schemaVersion(), installed.routingSnapshot(), 1,
                installed.allocationFingerprint(), installed.eligibleBackendIds(),
                installed.excludedBackendIds(), installed.installedAt(),
                installed.installationReason(), installed.ownerGeneration()));
        assertThrows(IllegalArgumentException.class,
                () -> EnterpriseLabInstalledAllocationSnapshot.installed(
                        installed.routingSnapshot(), clock, "password=do-not-record", 1));
    }

    @Test
    void codecRejectsCatalogMismatchAndInvalidUtf8() {
        EnterpriseLabInstalledAllocationSnapshot installed = ownedRouter().installedSnapshot();
        EnterpriseLabInstalledAllocationSnapshotCodec unbound =
                new EnterpriseLabInstalledAllocationSnapshotCodec(
                        EnterpriseLabExperimentTargetCatalog.empty());

        CodecException mismatch = assertThrows(
                CodecException.class, () -> unbound.encode(installed));
        assertEquals(Failure.TARGET_MISMATCH, mismatch.failure());
        assertEquals(Failure.MALFORMED_SNAPSHOT, assertThrows(
                CodecException.class,
                () -> codec.decode(new byte[]{(byte) 0xC3, (byte) 0x28})).failure());
    }

    @Test
    void concurrentReadersObserveOnlyCompleteFingerprintConsistentInstalledStates()
            throws Exception {
        EnterpriseLabAdaptiveDecision candidate = new EnterpriseLabAdaptiveDecisionService()
                .decide(SCENARIO, "active-experiment", true, false, false);
        EnterpriseLabLoopbackAllocationRouter router = ownedRouter(
                candidate.decision().request().baselineAllocations());
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        threads.add(thread("installed-writer", ready, start, failure, () -> {
            for (int index = 0; index < 200; index++) {
                router.applyCandidate(candidate, true);
                router.restoreBaseline("atomic-cycle-" + index);
            }
        }));
        for (int reader = 0; reader < 3; reader++) {
            threads.add(thread("installed-reader-" + reader, ready, start, failure, () -> {
                for (int index = 0; index < 2_000; index++) {
                    EnterpriseLabInstalledAllocationSnapshot installed = router.installedSnapshot();
                    EnterpriseLabLoopbackAllocationSnapshot routing = installed.routingSnapshot();
                    if (installed.routerGeneration() != routing.revision()
                            || !installed.eligibleBackendIds().equals(routing.eligibleBackendIds())
                            || !installed.excludedBackendIds().equals(routing.excludedBackendIds())
                            || !installed.allocationFingerprint().equals(
                            EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                                    routing.scenarioId(), routing.allocations()))) {
                        throw new AssertionError("reader observed inconsistent installed state");
                    }
                }
            }));
        }

        threads.forEach(Thread::start);
        assertTrue(ready.await(2, TimeUnit.SECONDS));
        start.countDown();
        for (Thread thread : threads) {
            thread.join(10_000);
            assertFalse(thread.isAlive(), "bounded installed-state thread must complete");
        }
        assertNull(failure.get(), () -> "installed-state concurrency failure: " + failure.get());
    }

    private EnterpriseLabLoopbackAllocationRouter ownedRouter() {
        return ownedRouter(BASELINE);
    }

    private EnterpriseLabLoopbackAllocationRouter ownedRouter(Map<String, Double> baseline) {
        return new EnterpriseLabLoopbackAllocationRouter(
                targets,
                new EnterpriseLabLoopbackObservationIngress(
                        Set.of("blue", "green", "orange")),
                baseline,
                Optional.of(authority),
                clock);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static List<EnterpriseLabLoopbackTarget> targets() {
        return List.of(
                new EnterpriseLabLoopbackTarget(
                        SCENARIO, "blue", URI.create("http://127.0.0.1:49101/probe")),
                new EnterpriseLabLoopbackTarget(
                        SCENARIO, "green", URI.create("http://127.0.0.1:49102/probe")),
                new EnterpriseLabLoopbackTarget(
                        SCENARIO, "orange", URI.create("http://[::1]:49103/probe")));
    }

    private static Thread thread(
            String name,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicReference<Throwable> failure,
            ThrowingRunnable action) {
        return new Thread(() -> {
            ready.countDown();
            try {
                start.await();
                action.run();
            } catch (Throwable throwable) {
                failure.compareAndSet(null, throwable);
            }
        }, name);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
