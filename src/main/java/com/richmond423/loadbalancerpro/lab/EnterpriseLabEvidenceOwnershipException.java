package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;

/** Structured fail-closed ownership boundary error without backing path disclosure. */
public final class EnterpriseLabEvidenceOwnershipException extends RuntimeException {
    private final FailureClassification classification;

    EnterpriseLabEvidenceOwnershipException(
            FailureClassification classification,
            String message) {
        super(message);
        this.classification = requireFailure(classification);
    }

    EnterpriseLabEvidenceOwnershipException(
            FailureClassification classification,
            String message,
            Throwable cause) {
        super(message, cause);
        this.classification = requireFailure(classification);
    }

    public FailureClassification classification() {
        return classification;
    }

    private static FailureClassification requireFailure(FailureClassification value) {
        if (value == null || value == FailureClassification.NONE) {
            throw new IllegalArgumentException("ownership exception requires a failure classification");
        }
        return value;
    }
}
