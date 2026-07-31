package com.richmond423.loadbalancerpro.cli;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * Compatibility launcher for the retained offline evidence/report commands.
 *
 * <p>The former synthetic interactive menu was retired. The executable Spring Boot JAR dispatches
 * {@link RemediationReportCli} directly through its primary application entry point; this class remains only for
 * callers that explicitly selected the historical CLI main class.</p>
 */
public final class LoadBalancerCLI {
    private static final String VERSION = "2.5.0";
    static final String RETIRED_MESSAGE =
            "The synthetic interactive LoadBalancerCLI menu has been retired. "
                    + "Use the Spring Boot JAR for API/proxy operation or pass a documented offline "
                    + "evidence/report command such as --remediation-report.";

    private LoadBalancerCLI() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        Objects.requireNonNull(args, "args cannot be null");
        Objects.requireNonNull(out, "out cannot be null");
        Objects.requireNonNull(err, "err cannot be null");

        if (Arrays.stream(args).anyMatch("--version"::equalsIgnoreCase)) {
            out.println("LoadBalancerCLI version " + VERSION);
            return 0;
        }

        RemediationReportCli.Result reportResult = RemediationReportCli.runIfRequested(args, out, err);
        if (reportResult.requested()) {
            return reportResult.exitCode();
        }

        err.println(RETIRED_MESSAGE);
        return 2;
    }
}
