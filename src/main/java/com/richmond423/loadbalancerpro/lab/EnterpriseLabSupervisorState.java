package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.RecoveryClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransitionReason;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * One immutable, fingerprinted durable view of the independently installed
 * Enterprise Lab loopback allocation. The state is owned by the supervisor
 * process, not by the application JVM.
 */
public record EnterpriseLabSupervisorState(
        String schemaVersion,
        String supervisorInstanceId,
        long supervisorGeneration,
        String acceptedApplicationInstanceId,
        String acceptedApplicationOwnershipFingerprint,
        long acceptedApplicationGeneration,
        String previousApplicationInstanceId,
        String previousApplicationOwnershipFingerprint,
        long previousApplicationGeneration,
        EnterpriseLabInstalledAllocationSnapshot baselineAllocation,
        EnterpriseLabInstalledAllocationSnapshot installedAllocation,
        Optional<EnterpriseLabInstalledAllocationSnapshot> intendedAllocation,
        String transactionId,
        String lastRequestId,
        String lastRequestFingerprint,
        String previousCommittedFingerprint,
        TransactionPhase transactionPhase,
        Instant lastCommitAt,
        RecoveryClassification lastRecoveryClassification,
        TransitionReason transitionReason,
        long durableStateGeneration,
        String predecessorRecordFingerprint,
        String currentRecordFingerprint) {
    public static final String SCHEMA_VERSION = "enterprise-lab-supervisor-state/v1";
    public static final String GENESIS_FINGERPRINT = "GENESIS";
    public static final String NONE = EnterpriseLabSupervisorProtocol.NONE;
    public static final long HARD_MAX_DURABLE_STATE_GENERATION =
            EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION;

    private static final Pattern CANONICAL_ID =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public EnterpriseLabSupervisorState {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported supervisor state schemaVersion");
        }
        supervisorInstanceId = requireId(supervisorInstanceId, "supervisorInstanceId", false);
        supervisorGeneration = requireGeneration(supervisorGeneration, "supervisorGeneration");
        acceptedApplicationInstanceId = requireId(
                acceptedApplicationInstanceId, "acceptedApplicationInstanceId", true);
        acceptedApplicationOwnershipFingerprint = requireFingerprint(
                acceptedApplicationOwnershipFingerprint,
                "acceptedApplicationOwnershipFingerprint", true);
        if (acceptedApplicationGeneration < 0
                || acceptedApplicationGeneration > EnterpriseLabEvidenceOwnership.MAX_GENERATION) {
            throw new IllegalArgumentException(
                    "acceptedApplicationGeneration is outside hard bounds");
        }
        boolean noApplication = NONE.equals(acceptedApplicationInstanceId);
        if (noApplication != NONE.equals(acceptedApplicationOwnershipFingerprint)
                || noApplication != (acceptedApplicationGeneration == 0L)) {
            throw new IllegalArgumentException(
                    "accepted application identity, fingerprint, and generation must be present together");
        }
        previousApplicationInstanceId = requireId(
                previousApplicationInstanceId, "previousApplicationInstanceId", true);
        previousApplicationOwnershipFingerprint = requireFingerprint(
                previousApplicationOwnershipFingerprint,
                "previousApplicationOwnershipFingerprint", true);
        if (previousApplicationGeneration < 0
                || previousApplicationGeneration > EnterpriseLabEvidenceOwnership.MAX_GENERATION) {
            throw new IllegalArgumentException(
                    "previousApplicationGeneration is outside hard bounds");
        }
        boolean noPreviousApplication = NONE.equals(previousApplicationInstanceId);
        if (noPreviousApplication != NONE.equals(previousApplicationOwnershipFingerprint)
                || noPreviousApplication != (previousApplicationGeneration == 0L)
                || (!noPreviousApplication
                && previousApplicationGeneration >= acceptedApplicationGeneration)) {
            throw new IllegalArgumentException(
                    "previous application evidence must be complete and precede the accepted generation");
        }

        baselineAllocation = Objects.requireNonNull(
                baselineAllocation, "baselineAllocation cannot be null");
        installedAllocation = Objects.requireNonNull(
                installedAllocation, "installedAllocation cannot be null");
        intendedAllocation = Objects.requireNonNull(
                intendedAllocation, "intendedAllocation cannot be null");
        if (!baselineAllocation.safeDefault()
                || baselineAllocation.routingSnapshot().kind()
                != EnterpriseLabLoopbackAllocationSnapshot.Kind.BASELINE) {
            throw new IllegalArgumentException(
                    "baselineAllocation must be the immutable generation-zero safe baseline");
        }
        requireSameBinding(baselineAllocation, installedAllocation, "installedAllocation");
        if (intendedAllocation.isPresent()) {
            requireSameBinding(
                    baselineAllocation,
                    intendedAllocation.orElseThrow(),
                    "intendedAllocation");
        }

        transactionId = requireId(transactionId, "transactionId", true);
        lastRequestId = requireId(lastRequestId, "lastRequestId", true);
        lastRequestFingerprint = requireFingerprint(
                lastRequestFingerprint, "lastRequestFingerprint", true);
        if (NONE.equals(lastRequestId) != NONE.equals(lastRequestFingerprint)) {
            throw new IllegalArgumentException(
                    "last request identity and fingerprint must be present together");
        }
        previousCommittedFingerprint = requireFingerprint(
                previousCommittedFingerprint, "previousCommittedFingerprint", true);
        transactionPhase = Objects.requireNonNull(
                transactionPhase, "transactionPhase cannot be null");
        lastCommitAt = Objects.requireNonNull(lastCommitAt, "lastCommitAt cannot be null");
        lastRecoveryClassification = Objects.requireNonNull(
                lastRecoveryClassification, "lastRecoveryClassification cannot be null");
        transitionReason = Objects.requireNonNull(
                transitionReason, "transitionReason cannot be null");
        if (durableStateGeneration < 1
                || durableStateGeneration > HARD_MAX_DURABLE_STATE_GENERATION) {
            throw new IllegalArgumentException("durableStateGeneration is outside hard bounds");
        }
        predecessorRecordFingerprint = requirePredecessor(predecessorRecordFingerprint);

        boolean incomplete = switch (transactionPhase) {
            case PREPARED, INTENT_PERSISTED, APPLYING, APPLIED, VERIFYING,
                    RESTORE_REQUIRED, RESTORING -> true;
            case COMMITTED, RESTORED, REJECTED, FAILED, QUARANTINED -> false;
        };
        if (incomplete != intendedAllocation.isPresent()) {
            throw new IllegalArgumentException(
                    "incomplete supervisor phases must retain exactly one intended allocation");
        }
        if (incomplete && NONE.equals(transactionId)) {
            throw new IllegalArgumentException(
                    "incomplete supervisor state requires a transactionId");
        }
        if (transactionPhase == TransactionPhase.COMMITTED
                && !installedAllocation.allocationFingerprint()
                .equals(previousCommittedFingerprint)
                && NONE.equals(transactionId)) {
            throw new IllegalArgumentException(
                    "idle committed state must identify its installed fingerprint as the prior commit");
        }

        currentRecordFingerprint = requireFingerprint(
                currentRecordFingerprint, "currentRecordFingerprint", false);
        String expected = EnterpriseLabSupervisorStateCodec.canonicalRecordFingerprint(
                schemaVersion,
                supervisorInstanceId,
                supervisorGeneration,
                acceptedApplicationInstanceId,
                acceptedApplicationOwnershipFingerprint,
                acceptedApplicationGeneration,
                previousApplicationInstanceId,
                previousApplicationOwnershipFingerprint,
                previousApplicationGeneration,
                baselineAllocation,
                installedAllocation,
                intendedAllocation,
                transactionId,
                lastRequestId,
                lastRequestFingerprint,
                previousCommittedFingerprint,
                transactionPhase,
                lastCommitAt,
                lastRecoveryClassification,
                transitionReason,
                durableStateGeneration,
                predecessorRecordFingerprint);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                currentRecordFingerprint.getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException(
                    "currentRecordFingerprint does not match canonical supervisor state");
        }
    }

    public boolean applicationOwnerEstablished() {
        return acceptedApplicationGeneration >= EnterpriseLabEvidenceOwnership.INITIAL_GENERATION;
    }

    public boolean transactionIncomplete() {
        return intendedAllocation.isPresent();
    }

    static EnterpriseLabSupervisorState create(
            String supervisorInstanceId,
            long supervisorGeneration,
            String acceptedApplicationInstanceId,
            String acceptedApplicationOwnershipFingerprint,
            long acceptedApplicationGeneration,
            String previousApplicationInstanceId,
            String previousApplicationOwnershipFingerprint,
            long previousApplicationGeneration,
            EnterpriseLabInstalledAllocationSnapshot baselineAllocation,
            EnterpriseLabInstalledAllocationSnapshot installedAllocation,
            Optional<EnterpriseLabInstalledAllocationSnapshot> intendedAllocation,
            String transactionId,
            String lastRequestId,
            String lastRequestFingerprint,
            String previousCommittedFingerprint,
            TransactionPhase transactionPhase,
            Instant lastCommitAt,
            RecoveryClassification lastRecoveryClassification,
            TransitionReason transitionReason,
            long durableStateGeneration,
            String predecessorRecordFingerprint) {
        String fingerprint = EnterpriseLabSupervisorStateCodec.canonicalRecordFingerprint(
                SCHEMA_VERSION,
                supervisorInstanceId,
                supervisorGeneration,
                acceptedApplicationInstanceId,
                acceptedApplicationOwnershipFingerprint,
                acceptedApplicationGeneration,
                previousApplicationInstanceId,
                previousApplicationOwnershipFingerprint,
                previousApplicationGeneration,
                baselineAllocation,
                installedAllocation,
                intendedAllocation,
                transactionId,
                lastRequestId,
                lastRequestFingerprint,
                previousCommittedFingerprint,
                transactionPhase,
                lastCommitAt,
                lastRecoveryClassification,
                transitionReason,
                durableStateGeneration,
                predecessorRecordFingerprint);
        return new EnterpriseLabSupervisorState(
                SCHEMA_VERSION,
                supervisorInstanceId,
                supervisorGeneration,
                acceptedApplicationInstanceId,
                acceptedApplicationOwnershipFingerprint,
                acceptedApplicationGeneration,
                previousApplicationInstanceId,
                previousApplicationOwnershipFingerprint,
                previousApplicationGeneration,
                baselineAllocation,
                installedAllocation,
                intendedAllocation,
                transactionId,
                lastRequestId,
                lastRequestFingerprint,
                previousCommittedFingerprint,
                transactionPhase,
                lastCommitAt,
                lastRecoveryClassification,
                transitionReason,
                durableStateGeneration,
                predecessorRecordFingerprint,
                fingerprint);
    }

    private static long requireGeneration(long value, String field) {
        if (value < EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                || value > EnterpriseLabEvidenceOwnership.MAX_GENERATION) {
            throw new IllegalArgumentException(field + " is outside hard bounds");
        }
        return value;
    }

    private static String requireId(String value, String field, boolean allowNone) {
        if (allowNone && NONE.equals(value)) {
            return value;
        }
        if (value == null || !CANONICAL_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be a bounded canonical identifier");
        }
        return value;
    }

    private static String requireFingerprint(String value, String field, boolean allowNone) {
        if (allowNone && NONE.equals(value)) {
            return value;
        }
        if (value == null || !SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
        return value;
    }

    private static String requirePredecessor(String value) {
        if (GENESIS_FINGERPRINT.equals(value)
                || (value != null && SHA_256.matcher(value).matches())) {
            return value;
        }
        throw new IllegalArgumentException(
                "predecessorRecordFingerprint must be GENESIS or lowercase SHA-256");
    }

    private static void requireSameBinding(
            EnterpriseLabInstalledAllocationSnapshot baseline,
            EnterpriseLabInstalledAllocationSnapshot value,
            String field) {
        if (!baseline.routingSnapshot().scenarioId()
                .equals(value.routingSnapshot().scenarioId())
                || !baseline.routingSnapshot().allocations().keySet()
                .equals(value.routingSnapshot().allocations().keySet())) {
            throw new IllegalArgumentException(
                    field + " must retain the repository-approved scenario and backend set");
        }
    }
}
