package com.richmond423.loadbalancerpro.lab;

import java.nio.ByteBuffer;
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

/** Bounded aggregate evidence from the packaged independent-supervisor proof. */
public record EnterpriseLabIndependentSupervisorProofReport(
        String schemaVersion,
        Instant executedAt,
        boolean independentInstalledStateSurvivedApplicationCrash,
        boolean staleApplicationRejected,
        boolean supervisorRestartReconciled,
        boolean applicationCrashAfterSupervisorApplyReconciled,
        boolean supervisorCrashWindowsReconciled,
        boolean competingSupervisorSingleWinner,
        boolean ipcBoundariesEnforced,
        Map<String, Boolean> supervisorCrashWindows,
        Map<String, Boolean> ipcBoundaryChecks,
        int separateApplicationProcessCount,
        int separateSupervisorProcessCount,
        long firstApplicationGeneration,
        long recoveredApplicationGeneration,
        long firstSupervisorGeneration,
        long recoveredSupervisorGeneration,
        String candidateFingerprint,
        String finalBaselineFingerprint,
        List<String> scopeBoundaries,
        String contentFingerprint) {
    public static final String SCHEMA_VERSION =
            "enterprise-lab-independent-supervisor-proof-report/v1";
    public static final int REQUIRED_CRASH_WINDOWS = 8;
    public static final int REQUIRED_IPC_BOUNDARY_CHECKS = 18;

    public EnterpriseLabIndependentSupervisorProofReport {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException(
                    "unsupported independent supervisor proof schemaVersion");
        }
        executedAt = Objects.requireNonNull(executedAt, "executedAt cannot be null");
        supervisorCrashWindows = immutableSorted(
                supervisorCrashWindows, "supervisorCrashWindows");
        ipcBoundaryChecks = immutableSorted(
                ipcBoundaryChecks, "ipcBoundaryChecks");
        if (supervisorCrashWindows.size() != REQUIRED_CRASH_WINDOWS
                || ipcBoundaryChecks.size() != REQUIRED_IPC_BOUNDARY_CHECKS
                || separateApplicationProcessCount < 3
                || separateApplicationProcessCount > 64
                || separateSupervisorProcessCount < 3
                || separateSupervisorProcessCount > 64
                || firstApplicationGeneration < 1L
                || recoveredApplicationGeneration <= firstApplicationGeneration
                || firstSupervisorGeneration < 1L
                || recoveredSupervisorGeneration <= firstSupervisorGeneration) {
            throw new IllegalArgumentException(
                    "independent supervisor proof counts or generations are inconsistent");
        }
        candidateFingerprint = requireFingerprint(
                candidateFingerprint, "candidateFingerprint");
        finalBaselineFingerprint = requireFingerprint(
                finalBaselineFingerprint, "finalBaselineFingerprint");
        if (candidateFingerprint.equals(finalBaselineFingerprint)) {
            throw new IllegalArgumentException(
                    "candidate and final baseline fingerprints must differ");
        }
        scopeBoundaries = List.copyOf(Objects.requireNonNull(
                scopeBoundaries, "scopeBoundaries cannot be null"));
        if (scopeBoundaries.isEmpty()
                || scopeBoundaries.size() > 16
                || scopeBoundaries.stream().anyMatch(value -> value == null
                        || value.isBlank()
                        || value.length() > 256)) {
            throw new IllegalArgumentException(
                    "independent supervisor proof boundaries are outside hard bounds");
        }
        String expected = fingerprint(
                schemaVersion,
                executedAt.toString(),
                Boolean.toString(independentInstalledStateSurvivedApplicationCrash),
                Boolean.toString(staleApplicationRejected),
                Boolean.toString(supervisorRestartReconciled),
                Boolean.toString(applicationCrashAfterSupervisorApplyReconciled),
                Boolean.toString(supervisorCrashWindowsReconciled),
                Boolean.toString(competingSupervisorSingleWinner),
                Boolean.toString(ipcBoundariesEnforced),
                supervisorCrashWindows.toString(),
                ipcBoundaryChecks.toString(),
                Integer.toString(separateApplicationProcessCount),
                Integer.toString(separateSupervisorProcessCount),
                Long.toString(firstApplicationGeneration),
                Long.toString(recoveredApplicationGeneration),
                Long.toString(firstSupervisorGeneration),
                Long.toString(recoveredSupervisorGeneration),
                candidateFingerprint,
                finalBaselineFingerprint,
                scopeBoundaries.toString());
        if (!expected.equals(contentFingerprint)) {
            throw new IllegalArgumentException(
                    "independent supervisor proof content fingerprint does not match");
        }
    }

    public boolean allPassed() {
        return failedChecks().isEmpty();
    }

    public List<String> failedChecks() {
        List<String> failed = new ArrayList<>();
        add(failed, independentInstalledStateSurvivedApplicationCrash,
                "independent-installed-state-survival");
        add(failed, staleApplicationRejected, "stale-application-rejection");
        add(failed, supervisorRestartReconciled, "supervisor-restart");
        add(failed, applicationCrashAfterSupervisorApplyReconciled,
                "application-crash-after-supervisor-apply");
        add(failed, supervisorCrashWindowsReconciled, "supervisor-crash-windows");
        add(failed, competingSupervisorSingleWinner, "competing-supervisors");
        add(failed, ipcBoundariesEnforced, "ipc-boundaries");
        supervisorCrashWindows.forEach((name, passed) ->
                add(failed, Boolean.TRUE.equals(passed), "supervisor-crash-" + name));
        ipcBoundaryChecks.forEach((name, passed) ->
                add(failed, Boolean.TRUE.equals(passed), "ipc-boundary-" + name));
        return List.copyOf(failed);
    }

    public static EnterpriseLabIndependentSupervisorProofReport create(
            Instant executedAt,
            boolean independentInstalledStateSurvivedApplicationCrash,
            boolean staleApplicationRejected,
            boolean supervisorRestartReconciled,
            boolean applicationCrashAfterSupervisorApplyReconciled,
            Map<String, Boolean> supervisorCrashWindows,
            boolean competingSupervisorSingleWinner,
            Map<String, Boolean> ipcBoundaryChecks,
            int separateApplicationProcessCount,
            int separateSupervisorProcessCount,
            long firstApplicationGeneration,
            long recoveredApplicationGeneration,
            long firstSupervisorGeneration,
            long recoveredSupervisorGeneration,
            String candidateFingerprint,
            String finalBaselineFingerprint,
            List<String> scopeBoundaries) {
        Map<String, Boolean> crash = new TreeMap<>(supervisorCrashWindows);
        Map<String, Boolean> ipc = new TreeMap<>(ipcBoundaryChecks);
        List<String> boundaries = List.copyOf(scopeBoundaries);
        boolean allCrash = crash.size() == REQUIRED_CRASH_WINDOWS
                && crash.values().stream().allMatch(Boolean.TRUE::equals);
        boolean allIpc = ipc.size() == REQUIRED_IPC_BOUNDARY_CHECKS
                && ipc.values().stream().allMatch(Boolean.TRUE::equals);
        String content = fingerprint(
                SCHEMA_VERSION,
                executedAt.toString(),
                Boolean.toString(independentInstalledStateSurvivedApplicationCrash),
                Boolean.toString(staleApplicationRejected),
                Boolean.toString(supervisorRestartReconciled),
                Boolean.toString(applicationCrashAfterSupervisorApplyReconciled),
                Boolean.toString(allCrash),
                Boolean.toString(competingSupervisorSingleWinner),
                Boolean.toString(allIpc),
                crash.toString(),
                ipc.toString(),
                Integer.toString(separateApplicationProcessCount),
                Integer.toString(separateSupervisorProcessCount),
                Long.toString(firstApplicationGeneration),
                Long.toString(recoveredApplicationGeneration),
                Long.toString(firstSupervisorGeneration),
                Long.toString(recoveredSupervisorGeneration),
                candidateFingerprint,
                finalBaselineFingerprint,
                boundaries.toString());
        return new EnterpriseLabIndependentSupervisorProofReport(
                SCHEMA_VERSION,
                executedAt,
                independentInstalledStateSurvivedApplicationCrash,
                staleApplicationRejected,
                supervisorRestartReconciled,
                applicationCrashAfterSupervisorApplyReconciled,
                allCrash,
                competingSupervisorSingleWinner,
                allIpc,
                crash,
                ipc,
                separateApplicationProcessCount,
                separateSupervisorProcessCount,
                firstApplicationGeneration,
                recoveredApplicationGeneration,
                firstSupervisorGeneration,
                recoveredSupervisorGeneration,
                candidateFingerprint,
                finalBaselineFingerprint,
                boundaries,
                content);
    }

    private static Map<String, Boolean> immutableSorted(
            Map<String, Boolean> source, String field) {
        TreeMap<String, Boolean> sorted = new TreeMap<>(Objects.requireNonNull(
                source, field + " cannot be null"));
        if (sorted.keySet().stream().anyMatch(value -> value == null
                || !value.matches("[a-z0-9][a-z0-9-]{0,63}"))
                || sorted.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(field + " entries are invalid");
        }
        return Collections.unmodifiableMap(sorted);
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
                byte[] bytes = Objects.requireNonNull(
                        part, "fingerprint part cannot be null")
                        .getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES)
                        .putInt(bytes.length).array());
                digest.update(bytes);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
