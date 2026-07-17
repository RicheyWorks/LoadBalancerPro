package com.richmond423.loadbalancerpro.lab;

import java.util.Objects;
import java.util.Optional;

/**
 * Synchronous startup admission gate. Active experiment work is admitted only
 * after durable reconciliation has completed successfully, or when durable
 * journaling was explicitly not configured for the legacy in-memory service.
 */
public final class EnterpriseLabExperimentRecoveryGate {
    private InitializationState state;
    private Optional<EnterpriseLabExperimentStartupReconciler.RecoveryReport> report;
    private String reasonCode;

    private EnterpriseLabExperimentRecoveryGate(
            InitializationState state,
            Optional<EnterpriseLabExperimentStartupReconciler.RecoveryReport> report,
            String reasonCode) {
        this.state = Objects.requireNonNull(state, "state cannot be null");
        this.report = Objects.requireNonNull(report, "report cannot be null");
        this.reasonCode = requireCode(reasonCode);
    }

    public static EnterpriseLabExperimentRecoveryGate pending() {
        return new EnterpriseLabExperimentRecoveryGate(
                InitializationState.PENDING, Optional.empty(), "RECOVERY_PENDING");
    }

    public static EnterpriseLabExperimentRecoveryGate inMemoryOnly() {
        return new EnterpriseLabExperimentRecoveryGate(
                InitializationState.READY, Optional.empty(), "DURABLE_RECOVERY_NOT_CONFIGURED");
    }

    synchronized void begin() {
        if (state == InitializationState.RECONCILING) {
            throw new IllegalStateException("startup reconciliation is already running");
        }
        if (state == InitializationState.READY || state == InitializationState.FAILED) {
            return;
        }
        state = InitializationState.RECONCILING;
        reasonCode = "RECOVERY_RECONCILING";
    }

    synchronized void complete(EnterpriseLabExperimentStartupReconciler.RecoveryReport completed) {
        EnterpriseLabExperimentStartupReconciler.RecoveryReport safe =
                Objects.requireNonNull(completed, "completed report cannot be null");
        report = Optional.of(safe);
        state = safe.admissionAllowed() ? InitializationState.READY : InitializationState.FAILED;
        reasonCode = safe.reasonCode();
    }

    synchronized void fail(String code) {
        state = InitializationState.FAILED;
        reasonCode = requireCode(code);
    }

    public synchronized AdmissionStatus admissionStatus() {
        return new AdmissionStatus(
                state,
                state == InitializationState.READY,
                reasonCode,
                report);
    }

    public synchronized boolean admissionAllowed() {
        return state == InitializationState.READY;
    }

    public synchronized String reasonCode() {
        return reasonCode;
    }

    public enum InitializationState {
        PENDING,
        RECONCILING,
        READY,
        FAILED
    }

    public record AdmissionStatus(
            InitializationState state,
            boolean admissionAllowed,
            String reasonCode,
            Optional<EnterpriseLabExperimentStartupReconciler.RecoveryReport> recoveryReport) {
        public AdmissionStatus {
            state = Objects.requireNonNull(state, "state cannot be null");
            reasonCode = requireCode(reasonCode);
            recoveryReport = Objects.requireNonNull(recoveryReport, "recoveryReport cannot be null");
            if (admissionAllowed != (state == InitializationState.READY)) {
                throw new IllegalArgumentException("admissionAllowed must reflect READY state");
            }
        }
    }

    private static String requireCode(String value) {
        if (value == null || !value.matches("[A-Z0-9][A-Z0-9_.:-]{0,63}")) {
            throw new IllegalArgumentException("reason code must be bounded canonical text");
        }
        return value;
    }
}
