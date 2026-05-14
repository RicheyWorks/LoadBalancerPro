package com.richmond423.loadbalancerpro.api;

import com.richmond423.loadbalancerpro.cli.AdaptiveRoutingExperimentCommand;
import com.richmond423.loadbalancerpro.cli.EnterpriseLabWorkflowCommand;
import com.richmond423.loadbalancerpro.cli.LaseDemoCommand;
import com.richmond423.loadbalancerpro.cli.LaseReplayCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoadBalancerApiApplication {
    private static final String FALLBACK_VERSION = "2.5.0";

    public static void main(String[] args) {
        if (isVersionRequested(args)) {
            System.out.println("LoadBalancerPro version " + version());
            return;
        }
        if (!shouldStartApi(args)) {
            AdaptiveRoutingExperimentCommand.Result experimentResult =
                    AdaptiveRoutingExperimentCommand.runIfRequested(args, System.out, System.err);
            if (experimentResult.requested()) {
                if (experimentResult.exitCode() != 0) {
                    System.exit(experimentResult.exitCode());
                }
                return;
            }
            EnterpriseLabWorkflowCommand.Result labWorkflowResult =
                    EnterpriseLabWorkflowCommand.runIfRequested(args, System.out, System.err);
            if (labWorkflowResult.requested()) {
                if (labWorkflowResult.exitCode() != 0) {
                    System.exit(labWorkflowResult.exitCode());
                }
                return;
            }
            LaseReplayCommand.Result replayResult = LaseReplayCommand.runIfRequested(args, System.out, System.err);
            if (replayResult.requested()) {
                if (replayResult.exitCode() != 0) {
                    System.exit(replayResult.exitCode());
                }
                return;
            }
            LaseDemoCommand.Result demoResult = LaseDemoCommand.runIfRequested(args, System.out, System.err);
            if (demoResult.exitCode() != 0) {
                System.exit(demoResult.exitCode());
            }
            return;
        }
        SpringApplication.run(LoadBalancerApiApplication.class, args);
    }

    static boolean shouldStartApi(String[] args) {
        return !isVersionRequested(args)
                && !AdaptiveRoutingExperimentCommand.isRequested(args)
                && !EnterpriseLabWorkflowCommand.isRequested(args)
                && !LaseDemoCommand.isRequested(args)
                && !LaseReplayCommand.isRequested(args);
    }

    static boolean isVersionRequested(String[] args) {
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

    static String version() {
        String implementationVersion = LoadBalancerApiApplication.class.getPackage().getImplementationVersion();
        return implementationVersion == null || implementationVersion.isBlank()
                ? FALLBACK_VERSION
                : implementationVersion;
    }
}
