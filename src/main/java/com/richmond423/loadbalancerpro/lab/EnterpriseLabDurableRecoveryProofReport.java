package com.richmond423.loadbalancerpro.lab;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public record EnterpriseLabDurableRecoveryProofReport(
        String schemaVersion,
        Instant generatedAt,
        int actualLoopbackRequests,
        EnterpriseLabExperimentState interruptedFinalState,
        EnterpriseLabExperimentState completedFinalState,
        EnterpriseLabExperimentState normalRollbackFinalState,
        boolean firstRecoveryAdmitted,
        boolean secondRecoveryIdempotent,
        boolean completedRestartPreserved,
        boolean normalRollbackRestartPreserved,
        boolean middleCorruptionQuarantined,
        boolean partialTailQuarantined,
        boolean unresolvedEvidenceRetained,
        boolean activeCompactionRejected,
        boolean terminalCompactionVerified,
        String terminalManifestFingerprint,
        boolean allPassed,
        List<String> scopeBoundaries,
        String contentFingerprint) {
    public static final String SCHEMA_VERSION = "enterprise-lab-durable-recovery-proof/v1";

    public EnterpriseLabDurableRecoveryProofReport {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported durable recovery proof schemaVersion");
        }
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt cannot be null");
        interruptedFinalState = Objects.requireNonNull(
                interruptedFinalState, "interruptedFinalState cannot be null");
        completedFinalState = Objects.requireNonNull(completedFinalState, "completedFinalState cannot be null");
        normalRollbackFinalState = Objects.requireNonNull(
                normalRollbackFinalState, "normalRollbackFinalState cannot be null");
        scopeBoundaries = List.copyOf(Objects.requireNonNull(scopeBoundaries, "scopeBoundaries cannot be null"));
        if (actualLoopbackRequests < 1 || scopeBoundaries.isEmpty()
                || terminalManifestFingerprint == null
                || !terminalManifestFingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("durable proof evidence is outside bounded requirements");
        }
        boolean expectedPass = interruptedFinalState == EnterpriseLabExperimentState.ROLLED_BACK
                && completedFinalState == EnterpriseLabExperimentState.COMPLETED
                && normalRollbackFinalState == EnterpriseLabExperimentState.ROLLED_BACK
                && firstRecoveryAdmitted && secondRecoveryIdempotent
                && completedRestartPreserved && normalRollbackRestartPreserved
                && middleCorruptionQuarantined && partialTailQuarantined
                && unresolvedEvidenceRetained && activeCompactionRejected
                && terminalCompactionVerified;
        if (allPassed != expectedPass) {
            throw new IllegalArgumentException("durable proof pass summary is inconsistent");
        }
        String expectedFingerprint = fingerprint(
                generatedAt, actualLoopbackRequests, interruptedFinalState, completedFinalState,
                normalRollbackFinalState, firstRecoveryAdmitted, secondRecoveryIdempotent,
                completedRestartPreserved, normalRollbackRestartPreserved,
                middleCorruptionQuarantined, partialTailQuarantined, unresolvedEvidenceRetained,
                activeCompactionRejected, terminalCompactionVerified, terminalManifestFingerprint,
                allPassed, scopeBoundaries);
        if (!expectedFingerprint.equals(contentFingerprint)) {
            throw new IllegalArgumentException("durable proof contentFingerprint does not match evidence");
        }
    }

    static EnterpriseLabDurableRecoveryProofReport create(
            Instant generatedAt,
            int actualLoopbackRequests,
            EnterpriseLabExperimentState interruptedFinalState,
            EnterpriseLabExperimentState completedFinalState,
            EnterpriseLabExperimentState normalRollbackFinalState,
            boolean firstRecoveryAdmitted,
            boolean secondRecoveryIdempotent,
            boolean completedRestartPreserved,
            boolean normalRollbackRestartPreserved,
            boolean middleCorruptionQuarantined,
            boolean partialTailQuarantined,
            boolean unresolvedEvidenceRetained,
            boolean activeCompactionRejected,
            boolean terminalCompactionVerified,
            String terminalManifestFingerprint) {
        List<String> boundaries = List.of(
                "foreground bounded proof with literal 127.0.0.1 ephemeral backends only",
                "process interruption is simulated at the closed-writer boundary; no operating-system crash is claimed",
                "data remains beneath ignored target output; no arbitrary API path or raw journal read is enabled",
                "single-process local evidence only; no external target, cloud, tenant, signing, or production routing proof");
        boolean passed = interruptedFinalState == EnterpriseLabExperimentState.ROLLED_BACK
                && completedFinalState == EnterpriseLabExperimentState.COMPLETED
                && normalRollbackFinalState == EnterpriseLabExperimentState.ROLLED_BACK
                && firstRecoveryAdmitted && secondRecoveryIdempotent
                && completedRestartPreserved && normalRollbackRestartPreserved
                && middleCorruptionQuarantined && partialTailQuarantined
                && unresolvedEvidenceRetained && activeCompactionRejected
                && terminalCompactionVerified;
        return new EnterpriseLabDurableRecoveryProofReport(
                SCHEMA_VERSION, generatedAt, actualLoopbackRequests,
                interruptedFinalState, completedFinalState, normalRollbackFinalState,
                firstRecoveryAdmitted, secondRecoveryIdempotent, completedRestartPreserved,
                normalRollbackRestartPreserved, middleCorruptionQuarantined,
                partialTailQuarantined, unresolvedEvidenceRetained, activeCompactionRejected,
                terminalCompactionVerified, terminalManifestFingerprint, passed, boundaries,
                fingerprint(
                        generatedAt, actualLoopbackRequests, interruptedFinalState, completedFinalState,
                        normalRollbackFinalState, firstRecoveryAdmitted, secondRecoveryIdempotent,
                        completedRestartPreserved, normalRollbackRestartPreserved,
                        middleCorruptionQuarantined, partialTailQuarantined,
                        unresolvedEvidenceRetained, activeCompactionRejected,
                        terminalCompactionVerified, terminalManifestFingerprint, passed, boundaries));
    }

    private static String fingerprint(
            Instant generatedAt, int actualLoopbackRequests,
            EnterpriseLabExperimentState interruptedFinalState,
            EnterpriseLabExperimentState completedFinalState,
            EnterpriseLabExperimentState normalRollbackFinalState,
            boolean firstRecoveryAdmitted, boolean secondRecoveryIdempotent,
            boolean completedRestartPreserved, boolean normalRollbackRestartPreserved,
            boolean middleCorruptionQuarantined, boolean partialTailQuarantined,
            boolean unresolvedEvidenceRetained, boolean activeCompactionRejected,
            boolean terminalCompactionVerified, String terminalManifestFingerprint,
            boolean allPassed, List<String> scopeBoundaries) {
        MessageDigest digest = sha256();
        update(digest, SCHEMA_VERSION);
        update(digest, generatedAt.toString());
        update(digest, Integer.toString(actualLoopbackRequests));
        update(digest, interruptedFinalState.name());
        update(digest, completedFinalState.name());
        update(digest, normalRollbackFinalState.name());
        update(digest, Boolean.toString(firstRecoveryAdmitted));
        update(digest, Boolean.toString(secondRecoveryIdempotent));
        update(digest, Boolean.toString(completedRestartPreserved));
        update(digest, Boolean.toString(normalRollbackRestartPreserved));
        update(digest, Boolean.toString(middleCorruptionQuarantined));
        update(digest, Boolean.toString(partialTailQuarantined));
        update(digest, Boolean.toString(unresolvedEvidenceRetained));
        update(digest, Boolean.toString(activeCompactionRejected));
        update(digest, Boolean.toString(terminalCompactionVerified));
        update(digest, terminalManifestFingerprint);
        update(digest, Boolean.toString(allPassed));
        scopeBoundaries.forEach(value -> update(digest, value));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
