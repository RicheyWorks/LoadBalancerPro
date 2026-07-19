package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.Policy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabEvidenceOwnershipRenewerTest {
    private static final Policy POLICY = new Policy(
            Duration.ofMillis(400), Duration.ofMillis(40), 1, 1, Duration.ZERO);

    @TempDir
    Path temporaryDirectory;

    @Test
    void ownedServiceCannotStartTheRenewerBeforeReconciliationIsReady() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-17T17:50:00Z"));
        var acquisition = EnterpriseLabEvidenceOwnershipManager.acquire(
                temporaryDirectory, POLICY, clock);
        EnterpriseLabEvidenceOwnershipLease lease = acquisition.ownership().orElseThrow();
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(
                        temporaryDirectory, lease.ownershipGate());
        EnterpriseLabExperimentRecoveryGate recoveryGate =
                EnterpriseLabExperimentRecoveryGate.pending();
        EnterpriseLabExperimentDurableEvidenceRepository repository =
                new EnterpriseLabExperimentDurableEvidenceRepository(
                        directory, recoveryGate, clock);
        try {
            IllegalStateException failure = assertThrows(
                    IllegalStateException.class,
                    () -> new EnterpriseLabExperimentOperatorService(
                            EnterpriseLabExperimentTargetCatalog.empty(),
                            recoveryGate,
                            repository,
                            lease));

            assertTrue(failure.getMessage().contains("completed startup reconciliation"));
            assertTrue(lease.operatingSystemLockValid());
            assertFalse(recoveryGate.admissionAllowed());
        } finally {
            repository.close();
            assertEquals(OperationStatus.SUCCEEDED, lease.release().status());
        }
    }

    @Test
    void boundedPeriodicTaskRenewsTheSameOwnerGenerationAndStopsCleanly() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-17T18:00:00Z"));
        var acquisition = EnterpriseLabEvidenceOwnershipManager.acquire(
                temporaryDirectory, POLICY, clock);
        EnterpriseLabEvidenceOwnershipLease lease = acquisition.ownership().orElseThrow();
        EnterpriseLabExperimentRecoveryGate recoveryGate =
                EnterpriseLabExperimentRecoveryGate.inMemoryOnly();
        try (EnterpriseLabEvidenceOwnershipRenewer renewer =
                new EnterpriseLabEvidenceOwnershipRenewer(
                        lease.ownershipGate(), recoveryGate, POLICY.renewalInterval())) {
            clock.advance(Duration.ofMillis(80));

            assertTrue(await(() -> renewer.lastResult().isPresent()));
            var renewed = renewer.lastResult().orElseThrow();
            assertEquals(OperationStatus.SUCCEEDED, renewed.status());
            assertEquals(EnterpriseLabEvidenceOwnership.INITIAL_GENERATION,
                    renewed.record().orElseThrow().generation());
            assertEquals(clock.instant(), renewed.record().orElseThrow().lastRenewedAt());
            assertTrue(recoveryGate.admissionAllowed());
        } finally {
            assertEquals(OperationStatus.SUCCEEDED, lease.release().status());
        }
    }

    @Test
    void renewalDeadlineFailureClosesAdmissionAndThePeriodicTask() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-17T18:10:00Z"));
        var acquisition = EnterpriseLabEvidenceOwnershipManager.acquire(
                temporaryDirectory, POLICY, clock);
        EnterpriseLabEvidenceOwnershipLease lease = acquisition.ownership().orElseThrow();
        EnterpriseLabExperimentRecoveryGate recoveryGate =
                EnterpriseLabExperimentRecoveryGate.inMemoryOnly();
        EnterpriseLabAllocationReconciliationGate allocationGate =
                EnterpriseLabAllocationReconciliationGate.pending();
        try (EnterpriseLabEvidenceOwnershipRenewer renewer =
                new EnterpriseLabEvidenceOwnershipRenewer(
                        lease.ownershipGate(), recoveryGate,
                        Optional.of(allocationGate), POLICY.renewalInterval())) {
            clock.advance(Duration.ofSeconds(1));

            assertTrue(await(() -> !recoveryGate.admissionAllowed()));
            var failed = renewer.lastResult().orElseThrow();
            assertEquals(OperationStatus.FAILED, failed.status());
            assertEquals(FailureClassification.RENEWAL_DEADLINE_EXCEEDED,
                    failed.failure());
            assertEquals("OWNERSHIP_RENEWAL_FAILED", recoveryGate.reasonCode());
            assertFalse(recoveryGate.admissionAllowed());
            assertEquals("OWNERSHIP_RENEWAL_FAILED",
                    allocationGate.admissionStatus().reasonCode());
            assertFalse(allocationGate.admissionAllowed());
        } finally {
            assertTrue(lease.release().operatingSystemLockReleased());
        }
    }

    @Test
    void boundedSupervisorVerifierClosesOnlyAllocationAdmissionAndKeepsOwnershipRenewalAlive() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-17T18:20:00Z"));
        var acquisition = EnterpriseLabEvidenceOwnershipManager.acquire(
                temporaryDirectory, POLICY, clock);
        EnterpriseLabEvidenceOwnershipLease lease = acquisition.ownership().orElseThrow();
        EnterpriseLabExperimentRecoveryGate recoveryGate =
                EnterpriseLabExperimentRecoveryGate.inMemoryOnly();
        EnterpriseLabAllocationReconciliationGate allocationGate =
                EnterpriseLabAllocationReconciliationGate.pending();
        AtomicInteger checks = new AtomicInteger();
        try (EnterpriseLabEvidenceOwnershipRenewer renewer =
                new EnterpriseLabEvidenceOwnershipRenewer(
                        lease.ownershipGate(),
                        recoveryGate,
                        Optional.of(allocationGate),
                        Optional.of(() -> {
                            checks.incrementAndGet();
                            throw new IllegalStateException("supervisor epoch changed");
                        }),
                        POLICY.renewalInterval())) {
            clock.advance(Duration.ofMillis(80));

            assertTrue(await(() -> checks.get() >= 1));
            int firstCount = checks.get();
            assertTrue(await(() -> checks.get() > firstCount));
            assertEquals(OperationStatus.SUCCEEDED,
                    renewer.lastResult().orElseThrow().status());
            assertTrue(recoveryGate.admissionAllowed());
            assertEquals(
                    "SUPERVISOR_SESSION_UNAVAILABLE",
                    allocationGate.admissionStatus().reasonCode());
            assertFalse(allocationGate.admissionAllowed());
        } finally {
            assertEquals(OperationStatus.SUCCEEDED, lease.release().status());
        }
    }

    private static boolean await(BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            LockSupport.parkNanos(Duration.ofMillis(5).toNanos());
        }
        return condition.getAsBoolean();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone)) {
                throw new IllegalArgumentException("test clock is UTC only");
            }
            return this;
        }

        @Override
        public synchronized Instant instant() {
            return instant;
        }
    }
}
