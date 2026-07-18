package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipProofReport;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipProofRunner;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentProofExporter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/** Foreground packaged entrypoint for the bounded separate-process ownership proof. */
public final class EnterpriseLabEvidenceOwnershipProofCommand {
    private static final String FLAG = "--enterprise-lab-ownership-proof";
    private static final String OUTPUT_FLAG = "--enterprise-lab-ownership-proof-output=";
    private static final String CHILD_FLAG = "--enterprise-lab-ownership-proof-child=";
    private static final String RUN_FLAG = "--enterprise-lab-ownership-proof-run=";
    private static final String CASE_FLAG = "--enterprise-lab-ownership-proof-case=";
    private static final Path DEFAULT_OUTPUT =
            Path.of("target", "enterprise-lab-ownership-proof");

    private EnterpriseLabEvidenceOwnershipProofCommand() {
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
            Optional<String> childAction = selected(args, CHILD_FLAG);
            if (childAction.isPresent()) {
                int exit = new EnterpriseLabEvidenceOwnershipProofRunner().runChild(
                        output,
                        selected(args, RUN_FLAG).orElseThrow(
                                () -> new IllegalArgumentException("ownership proof child run token is required")),
                        selected(args, CASE_FLAG).orElseThrow(
                                () -> new IllegalArgumentException("ownership proof child case is required")),
                        childAction.orElseThrow(),
                        out,
                        err);
                return new Result(true, exit);
            }
            Files.createDirectories(output);
            EnterpriseLabEvidenceOwnershipProofReport report =
                    new EnterpriseLabEvidenceOwnershipProofRunner().run(output);
            if (!report.allPassed()) {
                throw new IllegalStateException("one or more ownership proof checks failed");
            }
            Path json = output.resolve("enterprise-lab-ownership-proof.json");
            Path summary = output.resolve("enterprise-lab-ownership-proof-summary.md");
            ObjectMapper mapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .findAndRegisterModules();
            Files.writeString(json, mapper.writeValueAsString(report) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            Files.writeString(summary, markdown(report), StandardCharsets.UTF_8);
            out.println("=== LoadBalancerPro Enterprise Lab Ownership Proof ===");
            out.println("Initial generation: " + report.initialGeneration());
            out.println("Clean takeover generation: " + report.cleanTakeoverGeneration());
            out.println("Abrupt takeover generation: " + report.abruptTakeoverGeneration());
            out.println("Separate-process live-owner denial: " + report.liveOwnerDenied());
            out.println("Competing takeover single winner: "
                    + report.competingTakeoverSingleWinner());
            out.println("Interrupted experiment rolled back: "
                    + report.interruptedExperimentRolledBack());
            out.println("All checks passed: " + report.allPassed());
            out.println("Report fingerprint: " + report.contentFingerprint());
            out.println("Proof JSON: " + json);
            out.println("Safety: bounded foreground separate-local-JVM and literal-loopback proof; "
                    + "no API server, arbitrary path, force unlock, external target, cloud, tenant, "
                    + "multi-host, network-filesystem, or production action.");
            return new Result(true, 0);
        } catch (IOException | RuntimeException exception) {
            err.println("Enterprise Lab ownership proof failed safely: " + safeMessage(exception));
            return new Result(true, 1);
        }
    }

    private static Optional<String> selected(String[] args, String prefix) {
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(value -> value.startsWith(prefix))
                .findFirst()
                .map(value -> value.substring(prefix.length()));
    }

    private static String markdown(EnterpriseLabEvidenceOwnershipProofReport report) {
        String line = System.lineSeparator();
        return "# Enterprise Lab Separate-Process Ownership Proof" + line + line
                + "- Initial generation: `" + report.initialGeneration() + "`" + line
                + "- Clean takeover generation: `" + report.cleanTakeoverGeneration() + "`" + line
                + "- Abrupt takeover generation: `" + report.abruptTakeoverGeneration() + "`" + line
                + "- Live owner denied: `" + report.liveOwnerDenied() + "`" + line
                + "- Simultaneous acquisition single winner: `"
                + report.simultaneousAcquisitionSingleWinner() + "`" + line
                + "- Competing takeover single winner: `"
                + report.competingTakeoverSingleWinner() + "`" + line
                + "- Interrupted experiment rolled back: `"
                + report.interruptedExperimentRolledBack() + "`" + line
                + "- Baseline restoration verified: `"
                + report.baselineRestorationVerified() + "`" + line
                + "- Repeated restart idempotent: `"
                + report.repeatedRestartIdempotent() + "`" + line
                + "- All checks passed: `" + report.allPassed() + "`" + line
                + "- Report fingerprint: `" + report.contentFingerprint() + "`" + line + line
                + "## Boundaries" + line + line
                + report.scopeBoundaries().stream().map(value -> "- " + value + line)
                        .reduce("", String::concat);
    }

    private static String safeMessage(Exception exception) {
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = exception.getMessage();
        if (root != exception && root.getMessage() != null && !root.getMessage().isBlank()) {
            message = message + "; cause: " + root.getMessage();
        }
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName() : message;
    }

    public record Result(boolean requested, int exitCode) {
    }
}
