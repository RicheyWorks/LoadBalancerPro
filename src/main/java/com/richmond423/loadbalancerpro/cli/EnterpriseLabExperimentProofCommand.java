package com.richmond423.loadbalancerpro.cli;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentProofExporter;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentProofReport;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentProofRunner;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class EnterpriseLabExperimentProofCommand {
    private static final String FLAG = "--enterprise-lab-experiment-proof";
    private static final String OUTPUT_FLAG = "--enterprise-lab-experiment-output=";
    private static final Path DEFAULT_OUTPUT = Path.of("target", "enterprise-lab-experiment-proof");

    private EnterpriseLabExperimentProofCommand() {
    }

    public static boolean isRequested(String[] args) {
        return args != null && Arrays.stream(args)
                .filter(Objects::nonNull)
                .anyMatch(arg -> arg.equals(FLAG) || arg.startsWith(FLAG + "="));
    }

    public static Result runIfRequested(String[] args, PrintStream out, PrintStream err) {
        if (!isRequested(args)) {
            return new Result(false, 0);
        }
        return run(args, out, err);
    }

    public static Result run(String[] args, PrintStream out, PrintStream err) {
        Objects.requireNonNull(args, "args cannot be null");
        Objects.requireNonNull(out, "out cannot be null");
        Objects.requireNonNull(err, "err cannot be null");
        try {
            String suite = selectedSuite(args).orElse("all");
            Path outputDirectory = selectedOutput(args).orElse(DEFAULT_OUTPUT);
            EnterpriseLabExperimentProofExporter.validateOutputDirectory(outputDirectory);
            EnterpriseLabExperimentProofReport report = new EnterpriseLabExperimentProofRunner().run(suite);
            EnterpriseLabExperimentProofExporter.Manifest manifest =
                    new EnterpriseLabExperimentProofExporter().export(outputDirectory, report, gitCommit());
            if (!report.allPassed()) {
                throw new IllegalStateException("one or more bounded loopback proof scenarios failed");
            }
            out.println("=== LoadBalancerPro Enterprise Lab Experiment Proof ===");
            out.println("Suite: " + report.requestedSuite());
            out.println("Scenario count: " + report.scenarios().size());
            out.println("Actual loopback requests: " + report.totalActualRequests());
            out.println("All checks passed: " + report.allPassed());
            out.println("Report fingerprint: " + report.contentFingerprint());
            out.println("Evidence directory: " + manifest.outputDirectory());
            out.println("Proof JSON: " + manifest.reportJson());
            out.println("Markdown summary: " + manifest.markdownSummary());
            out.println("Safety: foreground bounded literal-loopback proof with ignored target-only evidence; "
                    + "no API server, external target, cloud, tenant, production routing, release, or registry action.");
            return new Result(true, 0);
        } catch (RuntimeException | java.io.IOException exception) {
            err.println("Enterprise Lab experiment proof failed safely: " + safeMessage(exception));
            return new Result(true, 1);
        }
    }

    private static Optional<String> selectedSuite(String[] args) {
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(arg -> arg.equals(FLAG) || arg.startsWith(FLAG + "="))
                .findFirst()
                .map(arg -> arg.equals(FLAG) ? "all" : arg.substring((FLAG + "=").length()));
    }

    private static Optional<Path> selectedOutput(String[] args) {
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(arg -> arg.startsWith(OUTPUT_FLAG))
                .findFirst()
                .map(arg -> Path.of(arg.substring(OUTPUT_FLAG.length())));
    }

    private static String gitCommit() {
        String value = System.getenv("GITHUB_SHA");
        return value == null || value.isBlank() ? "local" : value.trim();
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public record Result(boolean requested, int exitCode) {
    }
}
