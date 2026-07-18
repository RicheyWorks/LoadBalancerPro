package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReconciliationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReleaseStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.RenewalResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentRecoveryGate.AdmissionStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentRecoveryGate.InitializationState;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Sanitized immutable operator view of the locally held ownership capability. */
public record EnterpriseLabEvidenceOwnershipStatus(
        String schemaVersion,
        String ownerReference,
        long generation,
        OwnershipState ownershipState,
        Instant acquiredAt,
        Instant lastRenewedAt,
        Instant leaseExpiresAt,
        boolean operatingSystemLockValid,
        ReconciliationStatus reconciliationStatus,
        ReleaseStatus releaseStatus,
        long takeoverSequence,
        String takeoverReasonCode,
        String previousOwnerFingerprint,
        String recordFingerprint,
        InitializationState admissionState,
        boolean mutationAdmissionAllowed,
        String admissionReasonCode,
        Optional<OperationStatus> renewalStatus,
        Optional<FailureClassification> renewalFailure,
        Optional<String> renewalReasonCode,
        Optional<OperationStatus> verificationStatus,
        Optional<FailureClassification> verificationFailure,
        Optional<String> verificationReasonCode,
        String evidenceBoundary) {
    public static final String SCHEMA_VERSION = "enterprise-lab-evidence-ownership-status/v1";
    public static final String EVIDENCE_BOUNDARY =
            "sanitized single-host status only; no paths, process or host details, handles, raw lock bytes, release, force-unlock, or takeover control";

    public EnterpriseLabEvidenceOwnershipStatus {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported ownership status schemaVersion");
        }
        ownerReference = requireText(ownerReference, "ownerReference", 128);
        ownershipState = Objects.requireNonNull(ownershipState, "ownershipState cannot be null");
        acquiredAt = Objects.requireNonNull(acquiredAt, "acquiredAt cannot be null");
        lastRenewedAt = Objects.requireNonNull(lastRenewedAt, "lastRenewedAt cannot be null");
        leaseExpiresAt = Objects.requireNonNull(leaseExpiresAt, "leaseExpiresAt cannot be null");
        reconciliationStatus = Objects.requireNonNull(
                reconciliationStatus, "reconciliationStatus cannot be null");
        releaseStatus = Objects.requireNonNull(releaseStatus, "releaseStatus cannot be null");
        takeoverReasonCode = requireText(takeoverReasonCode, "takeoverReasonCode", 64);
        previousOwnerFingerprint = requireText(
                previousOwnerFingerprint, "previousOwnerFingerprint", 64);
        recordFingerprint = requireText(recordFingerprint, "recordFingerprint", 64);
        admissionState = Objects.requireNonNull(admissionState, "admissionState cannot be null");
        admissionReasonCode = requireText(admissionReasonCode, "admissionReasonCode", 64);
        renewalStatus = Objects.requireNonNull(renewalStatus, "renewalStatus cannot be null");
        renewalFailure = Objects.requireNonNull(renewalFailure, "renewalFailure cannot be null");
        renewalReasonCode = Objects.requireNonNull(
                renewalReasonCode, "renewalReasonCode cannot be null");
        verificationStatus = Objects.requireNonNull(
                verificationStatus, "verificationStatus cannot be null");
        verificationFailure = Objects.requireNonNull(
                verificationFailure, "verificationFailure cannot be null");
        verificationReasonCode = Objects.requireNonNull(
                verificationReasonCode, "verificationReasonCode cannot be null");
        evidenceBoundary = requireText(evidenceBoundary, "evidenceBoundary", 256);
        if (generation < EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                || takeoverSequence < 0
                || mutationAdmissionAllowed != (admissionState == InitializationState.READY)
                || renewalStatus.isPresent() != renewalFailure.isPresent()
                || renewalStatus.isPresent() != renewalReasonCode.isPresent()
                || verificationStatus.isPresent() != verificationFailure.isPresent()
                || verificationStatus.isPresent() != verificationReasonCode.isPresent()) {
            throw new IllegalArgumentException("ownership status evidence is inconsistent");
        }
    }

    static EnterpriseLabEvidenceOwnershipStatus from(
            EnterpriseLabEvidenceOwnershipLease lease,
            Optional<RenewalResult> renewal,
            AdmissionStatus admission,
            Optional<VerificationResult> verification) {
        EnterpriseLabEvidenceOwnershipLease safeLease = Objects.requireNonNull(
                lease, "lease cannot be null");
        Optional<RenewalResult> safeRenewal = Objects.requireNonNull(
                renewal, "renewal cannot be null");
        AdmissionStatus safeAdmission = Objects.requireNonNull(
                admission, "admission cannot be null");
        Optional<VerificationResult> safeVerification = Objects.requireNonNull(
                verification, "verification cannot be null");
        OwnershipRecord record = safeLease.record();
        return new EnterpriseLabEvidenceOwnershipStatus(
                SCHEMA_VERSION,
                record.owner().ownerId(),
                record.generation(),
                record.state(),
                record.acquiredAt(),
                record.lastRenewedAt(),
                record.leaseExpiresAt(),
                safeLease.operatingSystemLockValid(),
                record.reconciliationStatus(),
                record.releaseStatus(),
                record.takeoverSequence(),
                record.takeoverReasonCode(),
                record.previousOwnerFingerprint(),
                record.recordFingerprint(),
                safeAdmission.state(),
                safeAdmission.admissionAllowed(),
                safeAdmission.reasonCode(),
                safeRenewal.map(RenewalResult::status),
                safeRenewal.map(RenewalResult::failure),
                safeRenewal.map(RenewalResult::reasonCode),
                safeVerification.map(VerificationResult::status),
                safeVerification.map(VerificationResult::failure),
                safeVerification.map(VerificationResult::reasonCode),
                EVIDENCE_BOUNDARY);
    }

    private static String requireText(String value, String field, int maximumLength) {
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > maximumLength) {
            throw new IllegalArgumentException(field + " must be trimmed, non-blank, and bounded");
        }
        return value;
    }
}
