package com.richmond423.loadbalancerpro.lab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabEvidenceOwnershipStatusTest {
    @TempDir
    Path root;

    @Test
    void explicitVerificationPublishesSanitizedFailureAndClosesMutationAdmission() throws Exception {
        Clock clock = Clock.systemUTC();
        EnterpriseLabExperimentRecoveryGate recoveryGate =
                EnterpriseLabExperimentRecoveryGate.pending();
        EnterpriseLabEvidenceOwnershipLease lease = EnterpriseLabEvidenceOwnershipManager.acquire(
                root, EnterpriseLabEvidenceOwnership.Policy.safetyFirstDefaults(), clock)
                .ownership().orElseThrow();
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(root, lease.ownershipGate());
        new EnterpriseLabExperimentStartupReconciler(
                directory,
                new EnterpriseLabProcessLocalAllocationRecovery(
                        EnterpriseLabExperimentTargetCatalog.empty()),
                recoveryGate,
                clock).initialize();
        EnterpriseLabExperimentDurableEvidenceRepository repository =
                new EnterpriseLabExperimentDurableEvidenceRepository(
                        directory, recoveryGate, clock);
        EnterpriseLabExperimentOperatorService service =
                new EnterpriseLabExperimentOperatorService(
                        EnterpriseLabExperimentTargetCatalog.empty(),
                        recoveryGate,
                        repository,
                        lease);

        EnterpriseLabEvidenceOwnershipStatus before = service.ownershipStatus().orElseThrow();
        assertTrue(before.operatingSystemLockValid());
        assertTrue(before.mutationAdmissionAllowed());
        assertTrue(before.verificationStatus().isEmpty());

        lockChannel(lease).close();
        EnterpriseLabEvidenceOwnershipStatus failed = service.verifyOwnership().orElseThrow();

        assertEquals(EnterpriseLabEvidenceOwnership.OperationStatus.FAILED,
                failed.verificationStatus().orElseThrow());
        assertEquals(EnterpriseLabEvidenceOwnership.FailureClassification.LOCK_LOST,
                failed.verificationFailure().orElseThrow());
        assertEquals("OWNERSHIP_LOCK_LOST", failed.verificationReasonCode().orElseThrow());
        assertFalse(failed.operatingSystemLockValid());
        assertFalse(failed.mutationAdmissionAllowed());
        assertEquals(EnterpriseLabExperimentRecoveryGate.InitializationState.FAILED,
                failed.admissionState());
        assertEquals("OWNERSHIP_VERIFICATION_FAILED", failed.admissionReasonCode());

        var arm = service.arm(new EnterpriseLabExperimentOperatorService.ArmRequest(
                "status-arm", "status-experiment", "tail-latency-pressure", 4,
                java.time.Duration.ofSeconds(10), 1, 1,
                java.time.Duration.ofSeconds(20)), true);
        assertEquals(EnterpriseLabExperimentOperatorService.OperatorStatus.DENIED, arm.status());
        assertEquals("RECOVERY_NOT_READY", arm.reasonCode());
        assertThrows(EnterpriseLabEvidenceOwnershipException.class, service::close);
    }

    private static FileChannel lockChannel(EnterpriseLabEvidenceOwnershipLease lease)
            throws ReflectiveOperationException {
        Field field = EnterpriseLabEvidenceOwnershipLease.class.getDeclaredField("lockChannel");
        field.setAccessible(true);
        return (FileChannel) field.get(lease);
    }
}
