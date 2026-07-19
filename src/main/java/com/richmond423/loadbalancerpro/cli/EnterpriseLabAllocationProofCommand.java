package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationProofReport;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationProofRunner;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationProofStateHolder;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentProofExporter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/** Foreground packaged entrypoint for bounded allocation crash-window proofs. */
public final class EnterpriseLabAllocationProofCommand {
    private static final String FLAG = "--enterprise-lab-allocation-proof";
    private static final String HOLDER_FLAG = "--enterprise-lab-allocation-proof-holder";
    private static final String OUTPUT_FLAG = "--enterprise-lab-allocation-proof-output=";
    private static final String RUN_FLAG = "--enterprise-lab-allocation-proof-run=";
    private static final Path DEFAULT_OUTPUT =
            Path.of("target", "enterprise-lab-allocation-proof");

    private EnterpriseLabAllocationProofCommand() {
    }

    public static boolean isRequested(String[] args) {
        return args != null && Arrays.stream(args)
                .filter(Objects::nonNull)
                .anyMatch(value -> FLAG.equals(value) || HOLDER_FLAG.equals(value));
    }

    public static Result runIfRequested(String[] args, PrintStream out, PrintStream err) {
        return isRequested(args) ? run(args, out, err) : new Result(false, 0);
    }

    public static Result run(String[] args, PrintStream out, PrintStream err) {
        Objects.requireNonNull(args, "args cannot be null");
        Objects.requireNonNull(out, "out cannot be null");
        Objects.requireNonNull(err, "err cannot be null");
        try {
            Path output = EnterpriseLabExperimentProofExporter.validateOutputDirectory(
                    selected(args, OUTPUT_FLAG).map(Path::of).orElse(DEFAULT_OUTPUT));
            if (Arrays.asList(args).contains(HOLDER_FLAG)) {
                int exit = EnterpriseLabAllocationProofStateHolder.runChild(
                        output,
                        selected(args, RUN_FLAG).orElseThrow(() ->
                                new IllegalArgumentException(
                                        "allocation proof holder run token is required")),
                        out,
                        err);
                return new Result(true, exit);
            }
            Files.createDirectories(output);
            EnterpriseLabAllocationProofReport report =
                    new EnterpriseLabAllocationProofRunner().run(output);
            if (!report.allPassed()) {
                throw new IllegalStateException(
                        "allocation proof checks failed: "
                                + String.join(",", report.failedChecks()));
            }
            Path json = output.resolve("enterprise-lab-allocation-proof.json");
            Path summary = output.resolve(
                    "enterprise-lab-allocation-proof-summary.md");
            ObjectMapper mapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .findAndRegisterModules();
            Files.writeString(
                    json,
                    mapper.writeValueAsString(report) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            Files.writeString(summary, markdown(report), StandardCharsets.UTF_8);
            out.println("=== LoadBalancerPro Enterprise Lab Allocation Proof ===");
            out.println("Normal transaction: " + report.normalTransactionPassed());
            out.println("Crash before apply: " + report.crashBeforeApplyPassed());
            out.println("Crash after apply: " + report.crashAfterApplyPassed());
            out.println("Crash after commit: " + report.crashAfterCommitPassed());
            out.println("Stale-owner takeover: " + report.staleOwnerTakeoverPassed());
            out.println("Controlled drift cases: " + report.driftClassifications().size());
            out.println("Restoration failure closed admission: "
                    + report.restorationFailureClosedAdmission());
            out.println("Repeated reconciliation stable: "
                    + report.repeatedReconciliationStable());
            out.println("Separate-process holder: "
                    + report.externalHolderSeparateProcess());
            out.println("All checks passed: " + report.allPassed());
            out.println("Report fingerprint: " + report.contentFingerprint());
            out.println("Proof JSON: " + json);
            out.println("Safety: proof-only bounded foreground literal-loopback execution; "
                    + "no API server, arbitrary path, allocation override, force commit, "
                    + "external target, cloud, tenant, native command, or production action.");
            return new Result(true, 0);
        } catch (IOException | RuntimeException exception) {
            err.println("Enterprise Lab allocation proof failed safely: "
                    + safeMessage(exception));
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

    private static String markdown(EnterpriseLabAllocationProofReport report) {
        String line = System.lineSeparator();
        StringBuilder value = new StringBuilder()
                .append("# Enterprise Lab Allocation Crash-Window Proof").append(line).append(line)
                .append("- Normal transaction: `").append(report.normalTransactionPassed()).append("`").append(line)
                .append("- Crash before apply: `").append(report.crashBeforeApplyPassed()).append("`").append(line)
                .append("- Crash after apply: `").append(report.crashAfterApplyPassed()).append("`").append(line)
                .append("- Crash after commit: `").append(report.crashAfterCommitPassed()).append("`").append(line)
                .append("- Stale-owner takeover: `").append(report.staleOwnerTakeoverPassed()).append("`").append(line)
                .append("- Stale mutation denied: `").append(report.competingStaleMutationDenied()).append("`").append(line)
                .append("- Restoration failure closed admission: `")
                .append(report.restorationFailureClosedAdmission()).append("`").append(line)
                .append("- Repeated reconciliation stable: `")
                .append(report.repeatedReconciliationStable()).append("`").append(line)
                .append("- Separate-process holder: `")
                .append(report.externalHolderSeparateProcess()).append("`").append(line)
                .append("- All checks passed: `").append(report.allPassed()).append("`").append(line)
                .append("- Report fingerprint: `").append(report.contentFingerprint()).append("`")
                .append(line).append(line)
                .append("## Controlled drift").append(line).append(line);
        report.driftClassifications().forEach((name, classification) -> value
                .append("- ").append(name).append(": `").append(classification)
                .append("`").append(line));
        value.append(line).append("## Boundaries").append(line).append(line);
        report.scopeBoundaries().forEach(boundary -> value
                .append("- ").append(boundary).append(line));
        return value.toString();
    }

    private static String safeMessage(Exception exception) {
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message == null || message.isBlank()
                ? root.getClass().getSimpleName()
                : message.replace('\r', ' ').replace('\n', ' ');
    }

    public record Result(boolean requested, int exitCode) {
    }
}
