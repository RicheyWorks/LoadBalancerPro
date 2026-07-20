package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentProofExporter;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabIndependentSupervisorProofReport;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabIndependentSupervisorProofRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/** Foreground packaged entry point for the independent-supervisor proof matrix. */
public final class EnterpriseLabIndependentSupervisorProofCommand {
    private static final String FLAG = "--enterprise-lab-independent-supervisor-proof";
    private static final String OUTPUT_FLAG = FLAG + "-output=";
    private static final String CHILD_FLAG = FLAG + "-child=";
    private static final String RUN_FLAG = FLAG + "-run=";
    private static final String CASE_FLAG = FLAG + "-case=";
    private static final String FAILURE_FLAG = FLAG + "-failure=";
    private static final Path DEFAULT_OUTPUT =
            Path.of("target", "enterprise-lab-independent-supervisor-proof");

    private EnterpriseLabIndependentSupervisorProofCommand() {
    }

    public static boolean isRequested(String[] args) {
        return args != null && Arrays.stream(args)
                .filter(Objects::nonNull)
                .anyMatch(value -> FLAG.equals(value) || value.startsWith(CHILD_FLAG));
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
            Path output = EnterpriseLabExperimentProofExporter.validateOutputDirectory(
                    selected(args, OUTPUT_FLAG).map(Path::of).orElse(DEFAULT_OUTPUT));
            Optional<String> child = selected(args, CHILD_FLAG);
            EnterpriseLabIndependentSupervisorProofRunner runner =
                    new EnterpriseLabIndependentSupervisorProofRunner();
            if (child.isPresent()) {
                int exit = runner.runChild(
                        output,
                        required(args, RUN_FLAG, "proof child run token is required"),
                        required(args, CASE_FLAG, "proof child case is required"),
                        child.orElseThrow(),
                        selected(args, FAILURE_FLAG),
                        out,
                        err);
                return new Result(true, exit);
            }

            Files.createDirectories(output);
            EnterpriseLabIndependentSupervisorProofReport report = runner.run(output);
            if (!report.allPassed()) {
                throw new IllegalStateException("independent supervisor proof checks failed: "
                        + String.join(",", report.failedChecks()));
            }
            Path json = output.resolve("enterprise-lab-independent-supervisor-proof.json");
            Path summary = output.resolve(
                    "enterprise-lab-independent-supervisor-proof-summary.md");
            ObjectMapper mapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .findAndRegisterModules();
            Files.writeString(json, mapper.writeValueAsString(report)
                    + System.lineSeparator(), StandardCharsets.UTF_8);
            Files.writeString(summary, markdown(report), StandardCharsets.UTF_8);
            out.println("=== LoadBalancerPro Independent Enterprise Lab Supervisor Proof ===");
            out.println("Separate application processes: "
                    + report.separateApplicationProcessCount());
            out.println("Separate supervisor processes: "
                    + report.separateSupervisorProcessCount());
            out.println("Supervisor crash windows: "
                    + report.supervisorCrashWindows().size());
            out.println("IPC boundary checks: " + report.ipcBoundaryChecks().size());
            out.println("All checks passed: " + report.allPassed());
            out.println("Report fingerprint: " + report.contentFingerprint());
            out.println("Proof JSON: " + json);
            out.println("Safety: bounded foreground separate-local-JVM and literal-loopback proof; "
                    + "no API server, arbitrary path, force unlock, external target, cloud, tenant, "
                    + "multi-host, network-filesystem, or production action.");
            return new Result(true, 0);
        } catch (IOException | RuntimeException exception) {
            err.println("Enterprise Lab independent supervisor proof failed safely: "
                    + safeMessage(exception));
            return new Result(true, 1);
        }
    }

    private static String required(String[] args, String prefix, String message) {
        return selected(args, prefix).orElseThrow(() -> new IllegalArgumentException(message));
    }

    private static Optional<String> selected(String[] args, String prefix) {
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(value -> value.startsWith(prefix))
                .findFirst()
                .map(value -> value.substring(prefix.length()));
    }

    private static String markdown(EnterpriseLabIndependentSupervisorProofReport report) {
        String line = System.lineSeparator();
        StringBuilder text = new StringBuilder()
                .append("# Independent Enterprise Lab Supervisor Proof").append(line).append(line)
                .append("- Independent installed state survived application crash: `")
                .append(report.independentInstalledStateSurvivedApplicationCrash())
                .append("`").append(line)
                .append("- Stale application rejected: `")
                .append(report.staleApplicationRejected()).append("`").append(line)
                .append("- Supervisor restart reconciled: `")
                .append(report.supervisorRestartReconciled()).append("`").append(line)
                .append("- Application crash after supervisor apply reconciled: `")
                .append(report.applicationCrashAfterSupervisorApplyReconciled())
                .append("`").append(line)
                .append("- Competing supervisor single winner: `")
                .append(report.competingSupervisorSingleWinner()).append("`").append(line)
                .append("- All checks passed: `").append(report.allPassed()).append("`")
                .append(line)
                .append("- Report fingerprint: `").append(report.contentFingerprint())
                .append("`").append(line).append(line)
                .append("## Supervisor crash windows").append(line).append(line);
        report.supervisorCrashWindows().forEach((name, passed) -> text
                .append("- ").append(name).append(": `").append(passed).append("`")
                .append(line));
        text.append(line).append("## IPC boundaries").append(line).append(line);
        report.ipcBoundaryChecks().forEach((name, passed) -> text
                .append("- ").append(name).append(": `").append(passed).append("`")
                .append(line));
        text.append(line).append("## Boundaries").append(line).append(line);
        report.scopeBoundaries().forEach(boundary -> text.append("- ")
                .append(boundary).append(line));
        return text.toString();
    }

    private static String safeMessage(Exception exception) {
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message == null || message.isBlank()
                ? root.getClass().getSimpleName() : message;
    }

    public record Result(boolean requested, int exitCode) {
    }
}
