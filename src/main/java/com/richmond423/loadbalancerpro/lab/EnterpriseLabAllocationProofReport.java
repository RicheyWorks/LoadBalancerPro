package com.richmond423.loadbalancerpro.lab;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Bounded aggregate evidence from the packaged allocation crash-window proof. */
public record EnterpriseLabAllocationProofReport(
        String schemaVersion,
        Instant executedAt,
        boolean normalTransactionPassed,
        boolean crashBeforeApplyPassed,
        boolean crashAfterApplyPassed,
        boolean crashAfterCommitPassed,
        boolean staleOwnerTakeoverPassed,
        boolean competingStaleMutationDenied,
        boolean restorationFailureClosedAdmission,
        boolean repeatedReconciliationStable,
        boolean externalHolderSeparateProcess,
        Map<String, String> driftClassifications,
        int durableRecordCount,
        long finalOwnerGeneration,
        long finalRouterGeneration,
        String baselineFingerprint,
        String finalInstalledFingerprint,
        List<String> scopeBoundaries,
        String contentFingerprint) {
    public static final String SCHEMA_VERSION =
            "enterprise-lab-allocation-proof-report/v1";
    public static final int REQUIRED_DRIFT_CASES = 10;

    public EnterpriseLabAllocationProofReport {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException(
                    "unsupported allocation proof report schemaVersion");
        }
        executedAt = Objects.requireNonNull(executedAt, "executedAt cannot be null");
        driftClassifications = Collections.unmodifiableMap(new TreeMap<>(Objects.requireNonNull(
                driftClassifications, "driftClassifications cannot be null")));
        if (driftClassifications.size() != REQUIRED_DRIFT_CASES) {
            throw new IllegalArgumentException(
                    "allocation proof requires exactly ten controlled drift cases");
        }
        if (durableRecordCount < 1
                || durableRecordCount > EnterpriseLabAllocationStateStore.HARD_MAX_RECORDS
                || finalOwnerGeneration < EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                || finalOwnerGeneration > EnterpriseLabEvidenceOwnership.MAX_GENERATION
                || finalRouterGeneration < 0
                || finalRouterGeneration
                > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION) {
            throw new IllegalArgumentException(
                    "allocation proof counters are outside hard bounds");
        }
        baselineFingerprint = requireFingerprint(
                baselineFingerprint, "baselineFingerprint");
        finalInstalledFingerprint = requireFingerprint(
                finalInstalledFingerprint, "finalInstalledFingerprint");
        scopeBoundaries = List.copyOf(Objects.requireNonNull(
                scopeBoundaries, "scopeBoundaries cannot be null"));
        if (scopeBoundaries.isEmpty() || scopeBoundaries.size() > 16
                || scopeBoundaries.stream().anyMatch(value -> value == null
                        || value.isBlank() || value.length() > 256)) {
            throw new IllegalArgumentException(
                    "allocation proof scope boundaries must be bounded plain text");
        }
        String expected = fingerprint(
                schemaVersion,
                executedAt.toString(),
                Boolean.toString(normalTransactionPassed),
                Boolean.toString(crashBeforeApplyPassed),
                Boolean.toString(crashAfterApplyPassed),
                Boolean.toString(crashAfterCommitPassed),
                Boolean.toString(staleOwnerTakeoverPassed),
                Boolean.toString(competingStaleMutationDenied),
                Boolean.toString(restorationFailureClosedAdmission),
                Boolean.toString(repeatedReconciliationStable),
                Boolean.toString(externalHolderSeparateProcess),
                driftClassifications.toString(),
                Integer.toString(durableRecordCount),
                Long.toString(finalOwnerGeneration),
                Long.toString(finalRouterGeneration),
                baselineFingerprint,
                finalInstalledFingerprint,
                scopeBoundaries.toString());
        if (!expected.equals(contentFingerprint)) {
            throw new IllegalArgumentException(
                    "allocation proof report content fingerprint does not match");
        }
    }

    public boolean allPassed() {
        return failedChecks().isEmpty();
    }

    public List<String> failedChecks() {
        List<String> failed = new ArrayList<>();
        add(failed, normalTransactionPassed, "normal-transaction");
        add(failed, crashBeforeApplyPassed, "crash-before-apply");
        add(failed, crashAfterApplyPassed, "crash-after-apply");
        add(failed, crashAfterCommitPassed, "crash-after-commit");
        add(failed, staleOwnerTakeoverPassed, "stale-owner-takeover");
        add(failed, competingStaleMutationDenied, "competing-stale-mutation");
        add(failed, restorationFailureClosedAdmission, "restoration-failure");
        add(failed, repeatedReconciliationStable, "repeated-reconciliation");
        add(failed, externalHolderSeparateProcess, "separate-process-holder");
        driftClassifications.forEach((name, classification) -> {
            if (classification == null
                    || classification.isBlank()
                    || "SAFE_BASELINE_INSTALLED".equals(classification)) {
                failed.add("drift-" + name);
            }
        });
        return List.copyOf(failed);
    }

    public static EnterpriseLabAllocationProofReport create(
            Instant executedAt,
            boolean normalTransactionPassed,
            boolean crashBeforeApplyPassed,
            boolean crashAfterApplyPassed,
            boolean crashAfterCommitPassed,
            boolean staleOwnerTakeoverPassed,
            boolean competingStaleMutationDenied,
            boolean restorationFailureClosedAdmission,
            boolean repeatedReconciliationStable,
            boolean externalHolderSeparateProcess,
            Map<String, String> driftClassifications,
            int durableRecordCount,
            long finalOwnerGeneration,
            long finalRouterGeneration,
            String baselineFingerprint,
            String finalInstalledFingerprint,
            List<String> scopeBoundaries) {
        Map<String, String> sorted = new TreeMap<>(driftClassifications);
        List<String> boundaries = List.copyOf(scopeBoundaries);
        String content = fingerprint(
                SCHEMA_VERSION,
                executedAt.toString(),
                Boolean.toString(normalTransactionPassed),
                Boolean.toString(crashBeforeApplyPassed),
                Boolean.toString(crashAfterApplyPassed),
                Boolean.toString(crashAfterCommitPassed),
                Boolean.toString(staleOwnerTakeoverPassed),
                Boolean.toString(competingStaleMutationDenied),
                Boolean.toString(restorationFailureClosedAdmission),
                Boolean.toString(repeatedReconciliationStable),
                Boolean.toString(externalHolderSeparateProcess),
                sorted.toString(),
                Integer.toString(durableRecordCount),
                Long.toString(finalOwnerGeneration),
                Long.toString(finalRouterGeneration),
                baselineFingerprint,
                finalInstalledFingerprint,
                boundaries.toString());
        return new EnterpriseLabAllocationProofReport(
                SCHEMA_VERSION,
                executedAt,
                normalTransactionPassed,
                crashBeforeApplyPassed,
                crashAfterApplyPassed,
                crashAfterCommitPassed,
                staleOwnerTakeoverPassed,
                competingStaleMutationDenied,
                restorationFailureClosedAdmission,
                repeatedReconciliationStable,
                externalHolderSeparateProcess,
                sorted,
                durableRecordCount,
                finalOwnerGeneration,
                finalRouterGeneration,
                baselineFingerprint,
                finalInstalledFingerprint,
                boundaries,
                content);
    }

    private static void add(List<String> failed, boolean passed, String name) {
        if (!passed) {
            failed.add(name);
        }
    }

    private static String requireFingerprint(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be canonical SHA-256");
        }
        return value;
    }

    private static String fingerprint(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                byte[] bytes = Objects.requireNonNull(part, "fingerprint part cannot be null")
                        .getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBufferSupport.intBytes(bytes.length));
                digest.update(bytes);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static final class ByteBufferSupport {
        private ByteBufferSupport() {
        }

        private static byte[] intBytes(int value) {
            return new byte[]{
                    (byte) (value >>> 24),
                    (byte) (value >>> 16),
                    (byte) (value >>> 8),
                    (byte) value};
        }
    }
}
