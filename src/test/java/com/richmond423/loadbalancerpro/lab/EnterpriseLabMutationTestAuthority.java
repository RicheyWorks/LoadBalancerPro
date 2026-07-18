package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceMutationAuthority.MutationAuthorization;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;

import java.nio.file.Path;

/** Explicit test-only ownership seam; production factories never create it. */
final class EnterpriseLabMutationTestAuthority
        implements EnterpriseLabEvidenceMutationAuthority {
    private final Path trustedRoot;
    private String ownerId = "test-owner";
    private long generation = EnterpriseLabEvidenceOwnership.INITIAL_GENERATION;
    private FailureClassification failure;

    EnterpriseLabMutationTestAuthority(Path trustedRoot) {
        this.trustedRoot = trustedRoot.toAbsolutePath().normalize();
    }

    static EnterpriseLabExperimentJournalDirectory ownedDirectory(Path trustedRoot) {
        return EnterpriseLabExperimentJournalDirectory.createOwned(
                trustedRoot, new EnterpriseLabMutationTestAuthority(trustedRoot));
    }

    static EnterpriseLabExperimentJournalDirectory ownedDirectory(
            Path trustedRoot,
            long maxJournalBytes,
            int maxJournalEntries) {
        return EnterpriseLabExperimentJournalDirectory.createForTesting(
                trustedRoot,
                maxJournalBytes,
                maxJournalEntries,
                new EnterpriseLabMutationTestAuthority(trustedRoot));
    }

    void replaceOwner(String replacementOwnerId, long replacementGeneration) {
        ownerId = replacementOwnerId;
        generation = replacementGeneration;
    }

    void fail(FailureClassification classification) {
        failure = classification;
    }

    void clearFailure() {
        failure = null;
    }

    @Override
    public MutationAuthorization requireMutationAuthorization() {
        if (failure != null) {
            throw new EnterpriseLabEvidenceOwnershipException(
                    failure, "injected test ownership failure");
        }
        return new MutationAuthorization(trustedRoot, ownerId, generation);
    }
}
