package com.richmond423.loadbalancerpro.cli;

import java.io.PrintStream;

/**
 * Test-scope launcher for the five bounded Enterprise Lab proof families.
 */
public final class EnterpriseLabProofToolsApplication {
    private EnterpriseLabProofToolsApplication() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        EnterpriseLabSupervisorCommand.Result supervisor =
                EnterpriseLabSupervisorCommand.runIfRequested(args, out, err);
        if (supervisor.requested()) {
            return supervisor.exitCode();
        }

        EnterpriseLabAllocationProofCommand.Result allocation =
                EnterpriseLabAllocationProofCommand.runIfRequested(args, out, err);
        if (allocation.requested()) {
            return allocation.exitCode();
        }

        EnterpriseLabIndependentSupervisorProofCommand.Result independentSupervisor =
                EnterpriseLabIndependentSupervisorProofCommand.runIfRequested(args, out, err);
        if (independentSupervisor.requested()) {
            return independentSupervisor.exitCode();
        }

        EnterpriseLabEvidenceOwnershipProofCommand.Result ownership =
                EnterpriseLabEvidenceOwnershipProofCommand.runIfRequested(args, out, err);
        if (ownership.requested()) {
            return ownership.exitCode();
        }

        EnterpriseLabDurableRecoveryProofCommand.Result durableRecovery =
                EnterpriseLabDurableRecoveryProofCommand.runIfRequested(args, out, err);
        if (durableRecovery.requested()) {
            return durableRecovery.exitCode();
        }

        EnterpriseLabExperimentProofCommand.Result experiment =
                EnterpriseLabExperimentProofCommand.runIfRequested(args, out, err);
        if (experiment.requested()) {
            return experiment.exitCode();
        }

        err.println("No Enterprise Lab proof tool command was requested.");
        return 2;
    }
}
