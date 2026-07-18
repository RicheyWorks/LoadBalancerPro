package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.RenewalResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.VerificationResult;

/**
 * The single authoritative admission gate for ownership-bound local mutation.
 * It remains tied to the live lease and cannot manufacture ownership evidence.
 */
public final class EnterpriseLabEvidenceOwnershipGate
        implements EnterpriseLabEvidenceMutationAuthority {
    private final EnterpriseLabEvidenceOwnershipLease lease;

    EnterpriseLabEvidenceOwnershipGate(EnterpriseLabEvidenceOwnershipLease lease) {
        this.lease = java.util.Objects.requireNonNull(lease, "lease cannot be null");
    }

    public VerificationResult verifyCurrentOwnership() {
        return lease.verifyCurrentOwnership();
    }

    public RenewalResult renew() {
        return lease.renewCurrentOwnership();
    }

    public OwnershipRecord requireCurrentOwnership() {
        VerificationResult result = verifyCurrentOwnership();
        if (result.status() != EnterpriseLabEvidenceOwnership.OperationStatus.SUCCEEDED) {
            throw new EnterpriseLabEvidenceOwnershipException(
                    result.failure(), result.reasonCode());
        }
        return result.record().orElseThrow();
    }

    @Override
    public MutationAuthorization requireMutationAuthorization() {
        return MutationAuthorization.from(lease.trustedRoot(), requireCurrentOwnership());
    }
}
