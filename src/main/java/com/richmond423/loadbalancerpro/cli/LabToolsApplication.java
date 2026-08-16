package com.richmond423.loadbalancerpro.cli;

import com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication;

/**
 * Opt-in entry point for simulation, replay, evidence, and Enterprise Lab tooling.
 * Packaged only by the Maven {@code lab} profile.
 */
public final class LabToolsApplication {
    private LabToolsApplication() {
    }

    public static void main(String[] args) {
        if (isVersionRequested(args)) {
            LoadBalancerApiApplication.main(args);
            return;
        }

        RemediationReportCli.Result reportResult =
                RemediationReportCli.runIfRequested(args, System.out, System.err);
        if (exitIfRequested(reportResult.requested(), reportResult.exitCode())) {
            return;
        }

        EnterpriseLabStorageRepairCommand.Result repairResult =
                EnterpriseLabStorageRepairCommand.runIfRequested(args, System.out, System.err);
        if (exitIfRequested(repairResult.requested(), repairResult.exitCode())) {
            return;
        }

        EnterpriseLabSupervisorCommand.Result supervisorResult =
                EnterpriseLabSupervisorCommand.runIfRequested(args, System.out, System.err);
        if (exitIfRequested(supervisorResult.requested(), supervisorResult.exitCode())) {
            return;
        }

        AdaptiveRoutingExperimentCommand.Result experimentResult =
                AdaptiveRoutingExperimentCommand.runIfRequested(args, System.out, System.err);
        if (exitIfRequested(experimentResult.requested(), experimentResult.exitCode())) {
            return;
        }

        EnterpriseLabWorkflowCommand.Result workflowResult =
                EnterpriseLabWorkflowCommand.runIfRequested(args, System.out, System.err);
        if (exitIfRequested(workflowResult.requested(), workflowResult.exitCode())) {
            return;
        }

        LaseReplayCommand.Result replayResult =
                LaseReplayCommand.runIfRequested(args, System.out, System.err);
        if (exitIfRequested(replayResult.requested(), replayResult.exitCode())) {
            return;
        }

        if (LaseDemoCommand.isRequested(args)) {
            LaseDemoCommand.Result demoResult = LaseDemoCommand.runIfRequested(args, System.out, System.err);
            exitIfRequested(true, demoResult.exitCode());
            return;
        }

        LoadBalancerApiApplication.main(args);
    }

    private static boolean exitIfRequested(boolean requested, int exitCode) {
        if (!requested) {
            return false;
        }
        if (exitCode != 0) {
            System.exit(exitCode);
        }
        return true;
    }

    private static boolean isVersionRequested(String[] args) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if ("--version".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }
}
