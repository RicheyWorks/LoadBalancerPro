package com.richmond423.loadbalancerpro.lab;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Sanitized aggregate from the bounded separate-process ownership proof. */
public record EnterpriseLabEvidenceOwnershipProofReport(
        String schemaVersion,
        Instant generatedAt,
        long initialGeneration,
        long cleanTakeoverGeneration,
        long abruptTakeoverGeneration,
        long repeatedRestartGeneration,
        boolean liveOwnerDenied,
        boolean ownerAppendAndReconciliationVerified,
        boolean nonOwnerAppendDenied,
        boolean nonOwnerCompactionDenied,
        boolean nonOwnerRetentionDenied,
        boolean nonOwnerExperimentStartDenied,
        boolean nonOwnerAllocationChangeDenied,
        boolean renewalSucceeded,
        boolean cleanReleaseRecorded,
        boolean repeatedReleaseIdempotent,
        boolean cleanTakeoverClassified,
        boolean restartedPriorOwnerDenied,
        boolean abruptStaleOwnerClassified,
        boolean journalsVerifiedAndReplayed,
        boolean interruptedExperimentRolledBack,
        boolean baselineRestorationVerified,
        boolean takeoverRecoveryRecorded,
        boolean repeatedRestartIdempotent,
        boolean simultaneousAcquisitionSingleWinner,
        boolean competingTakeoverSingleWinner,
        boolean allPassed,
        String contentFingerprint,
        List<String> scopeBoundaries) {
    public static final String SCHEMA_VERSION = "enterprise-lab-evidence-ownership-proof/v1";

    public EnterpriseLabEvidenceOwnershipProofReport {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported ownership proof schemaVersion");
        }
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt cannot be null");
        contentFingerprint = requireFingerprint(contentFingerprint);
        scopeBoundaries = List.copyOf(Objects.requireNonNull(
                scopeBoundaries, "scopeBoundaries cannot be null"));
        if (initialGeneration < EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                || cleanTakeoverGeneration <= initialGeneration
                || abruptTakeoverGeneration <= cleanTakeoverGeneration
                || repeatedRestartGeneration <= abruptTakeoverGeneration
                || scopeBoundaries.isEmpty() || scopeBoundaries.size() > 12) {
            throw new IllegalArgumentException("ownership proof generation or boundary evidence is inconsistent");
        }
        boolean expected = liveOwnerDenied
                && ownerAppendAndReconciliationVerified
                && nonOwnerAppendDenied
                && nonOwnerCompactionDenied
                && nonOwnerRetentionDenied
                && nonOwnerExperimentStartDenied
                && nonOwnerAllocationChangeDenied
                && renewalSucceeded
                && cleanReleaseRecorded
                && repeatedReleaseIdempotent
                && cleanTakeoverClassified
                && restartedPriorOwnerDenied
                && abruptStaleOwnerClassified
                && journalsVerifiedAndReplayed
                && interruptedExperimentRolledBack
                && baselineRestorationVerified
                && takeoverRecoveryRecorded
                && repeatedRestartIdempotent
                && simultaneousAcquisitionSingleWinner
                && competingTakeoverSingleWinner;
        if (allPassed != expected) {
            throw new IllegalArgumentException("allPassed must reflect every ownership proof check");
        }
    }

    static EnterpriseLabEvidenceOwnershipProofReport create(
            Instant generatedAt,
            long initialGeneration,
            long cleanTakeoverGeneration,
            long abruptTakeoverGeneration,
            long repeatedRestartGeneration,
            boolean liveOwnerDenied,
            boolean ownerAppendAndReconciliationVerified,
            boolean nonOwnerAppendDenied,
            boolean nonOwnerCompactionDenied,
            boolean nonOwnerRetentionDenied,
            boolean nonOwnerExperimentStartDenied,
            boolean nonOwnerAllocationChangeDenied,
            boolean renewalSucceeded,
            boolean cleanReleaseRecorded,
            boolean repeatedReleaseIdempotent,
            boolean cleanTakeoverClassified,
            boolean restartedPriorOwnerDenied,
            boolean abruptStaleOwnerClassified,
            boolean journalsVerifiedAndReplayed,
            boolean interruptedExperimentRolledBack,
            boolean baselineRestorationVerified,
            boolean takeoverRecoveryRecorded,
            boolean repeatedRestartIdempotent,
            boolean simultaneousAcquisitionSingleWinner,
            boolean competingTakeoverSingleWinner) {
        boolean allPassed = liveOwnerDenied
                && ownerAppendAndReconciliationVerified
                && nonOwnerAppendDenied
                && nonOwnerCompactionDenied
                && nonOwnerRetentionDenied
                && nonOwnerExperimentStartDenied
                && nonOwnerAllocationChangeDenied
                && renewalSucceeded
                && cleanReleaseRecorded
                && repeatedReleaseIdempotent
                && cleanTakeoverClassified
                && restartedPriorOwnerDenied
                && abruptStaleOwnerClassified
                && journalsVerifiedAndReplayed
                && interruptedExperimentRolledBack
                && baselineRestorationVerified
                && takeoverRecoveryRecorded
                && repeatedRestartIdempotent
                && simultaneousAcquisitionSingleWinner
                && competingTakeoverSingleWinner;
        List<String> boundaries = List.of(
                "separate local JVM processes and one controlled target-directory tree only",
                "JDK operating-system file locks are the single-host exclusion authority",
                "literal loopback targets only; no public, tenant, cloud, or production traffic",
                "no arbitrary path, owner, generation, release, force-unlock, or takeover API",
                "no multi-host, network-filesystem, distributed-consensus, or malicious-process claim",
                "restart allocation proof is bounded to the existing process-local baseline model");
        String fingerprint = fingerprint(
                generatedAt, initialGeneration, cleanTakeoverGeneration,
                abruptTakeoverGeneration, repeatedRestartGeneration,
                liveOwnerDenied, ownerAppendAndReconciliationVerified,
                nonOwnerAppendDenied, nonOwnerCompactionDenied, nonOwnerRetentionDenied,
                nonOwnerExperimentStartDenied, nonOwnerAllocationChangeDenied,
                renewalSucceeded, cleanReleaseRecorded, repeatedReleaseIdempotent,
                cleanTakeoverClassified, restartedPriorOwnerDenied,
                abruptStaleOwnerClassified, journalsVerifiedAndReplayed,
                interruptedExperimentRolledBack, baselineRestorationVerified,
                takeoverRecoveryRecorded, repeatedRestartIdempotent,
                simultaneousAcquisitionSingleWinner, competingTakeoverSingleWinner,
                allPassed, boundaries);
        return new EnterpriseLabEvidenceOwnershipProofReport(
                SCHEMA_VERSION, generatedAt, initialGeneration, cleanTakeoverGeneration,
                abruptTakeoverGeneration, repeatedRestartGeneration, liveOwnerDenied,
                ownerAppendAndReconciliationVerified, nonOwnerAppendDenied,
                nonOwnerCompactionDenied, nonOwnerRetentionDenied,
                nonOwnerExperimentStartDenied, nonOwnerAllocationChangeDenied,
                renewalSucceeded, cleanReleaseRecorded, repeatedReleaseIdempotent,
                cleanTakeoverClassified, restartedPriorOwnerDenied,
                abruptStaleOwnerClassified, journalsVerifiedAndReplayed,
                interruptedExperimentRolledBack, baselineRestorationVerified,
                takeoverRecoveryRecorded, repeatedRestartIdempotent,
                simultaneousAcquisitionSingleWinner, competingTakeoverSingleWinner,
                allPassed, fingerprint, boundaries);
    }

    private static String fingerprint(Object... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Object value : values) {
                digest.update(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String requireFingerprint(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("contentFingerprint must be lowercase SHA-256");
        }
        return value;
    }
}
