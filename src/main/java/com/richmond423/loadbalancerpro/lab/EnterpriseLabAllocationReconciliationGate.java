package com.richmond423.loadbalancerpro.lab;

import java.util.Objects;
import java.util.Optional;

/**
 * Fail-closed admission gate for durable allocation reconciliation. A check
 * closes admission while it runs and readiness is published only from an
 * exact final reconciliation report.
 */
public final class EnterpriseLabAllocationReconciliationGate {
    private InitializationState state;
    private String reasonCode;
    private Optional<EnterpriseLabAllocationReconciler.ReconciliationReport> report;

    private EnterpriseLabAllocationReconciliationGate(
            InitializationState state,
            String reasonCode,
            Optional<EnterpriseLabAllocationReconciler.ReconciliationReport> report) {
        this.state = Objects.requireNonNull(state, "state cannot be null");
        this.reasonCode = requireCode(reasonCode);
        this.report = Objects.requireNonNull(report, "report cannot be null");
    }

    public static EnterpriseLabAllocationReconciliationGate pending() {
        return new EnterpriseLabAllocationReconciliationGate(
                InitializationState.PENDING,
                "ALLOCATION_RECONCILIATION_PENDING",
                Optional.empty());
    }

    synchronized void begin() {
        if (state == InitializationState.RECONCILING) {
            throw new IllegalStateException("allocation reconciliation is already running");
        }
        state = InitializationState.RECONCILING;
        reasonCode = "ALLOCATION_RECONCILING";
    }

    synchronized void complete(
            EnterpriseLabAllocationReconciler.ReconciliationReport completed) {
        EnterpriseLabAllocationReconciler.ReconciliationReport safe =
                Objects.requireNonNull(completed, "completed report cannot be null");
        report = Optional.of(safe);
        state = safe.ready()
                ? InitializationState.READY : InitializationState.FAILED;
        reasonCode = safe.reasonCode();
    }

    public synchronized void fail(String code) {
        state = InitializationState.FAILED;
        reasonCode = requireCode(code);
    }

    public synchronized boolean admissionAllowed() {
        return state == InitializationState.READY;
    }

    public synchronized AdmissionStatus admissionStatus() {
        return new AdmissionStatus(
                state,
                state == InitializationState.READY,
                reasonCode,
                report);
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
            Optional<EnterpriseLabAllocationReconciler.ReconciliationReport> report) {
        public AdmissionStatus {
            state = Objects.requireNonNull(state, "state cannot be null");
            reasonCode = requireCode(reasonCode);
            report = Objects.requireNonNull(report, "report cannot be null");
            if (admissionAllowed != (state == InitializationState.READY)) {
                throw new IllegalArgumentException(
                        "admissionAllowed must reflect READY state");
            }
        }
    }

    private static String requireCode(String value) {
        if (value == null || !value.matches("[A-Z0-9][A-Z0-9_.:-]{0,63}")) {
            throw new IllegalArgumentException(
                    "reason code must be bounded canonical text");
        }
        return value;
    }
}
