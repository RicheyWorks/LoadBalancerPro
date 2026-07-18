package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Internal, non-detachable bridge from the live ownership resource to one
 * mutation boundary. Production callers receive it only through the live
 * ownership gate or the manager-held takeover reconciliation path.
 */
interface EnterpriseLabEvidenceMutationAuthority {
    MutationAuthorization requireMutationAuthorization();

    record MutationAuthorization(
            Path trustedRoot,
            String ownerId,
            long generation) {
        public MutationAuthorization {
            trustedRoot = Objects.requireNonNull(trustedRoot, "trustedRoot cannot be null")
                    .toAbsolutePath().normalize();
            ownerId = EnterpriseLabEvidenceOwnership.requireId(ownerId, "ownerId");
            if (generation < EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                    || generation > EnterpriseLabEvidenceOwnership.MAX_GENERATION) {
                throw new IllegalArgumentException("ownership generation is outside hard bounds");
            }
        }

        static MutationAuthorization from(Path trustedRoot, OwnershipRecord record) {
            OwnershipRecord safeRecord = Objects.requireNonNull(record, "record cannot be null");
            return new MutationAuthorization(
                    trustedRoot,
                    safeRecord.owner().ownerId(),
                    safeRecord.generation());
        }

        void requireSameEpoch(MutationAuthorization current) {
            MutationAuthorization safeCurrent = Objects.requireNonNull(
                    current, "current authorization cannot be null");
            if (!trustedRoot.equals(safeCurrent.trustedRoot)
                    || !ownerId.equals(safeCurrent.ownerId)
                    || generation != safeCurrent.generation) {
                throw new EnterpriseLabEvidenceOwnershipException(
                        EnterpriseLabEvidenceOwnership.FailureClassification.RECORD_REPLACED,
                        "ownership generation changed before mutation commit");
            }
        }
    }
}
