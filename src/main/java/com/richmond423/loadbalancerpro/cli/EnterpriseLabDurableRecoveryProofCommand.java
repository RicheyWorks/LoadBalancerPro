package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabDurableRecoveryProofReport;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabDurableRecoveryProofRunner;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentProofExporter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class EnterpriseLabDurableRecoveryProofCommand {
    private static final String FLAG = "--enterprise-lab-durable-recovery-proof";
    private static final String OUTPUT_FLAG = "--enterprise-lab-durable-recovery-output=";
    private static final Path DEFAULT_OUTPUT = Path.of("target", "enterprise-lab-durable-recovery-proof");

    private EnterpriseLabDurableRecoveryProofCommand() {
    }

    public static boolean isRequested(String[] args) {
        return args != null && Arrays.stream(args)
                .filter(Objects::nonNull)
                .anyMatch(FLAG::equals);
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
                    selectedOutput(args).orElse(DEFAULT_OUTPUT));
            Files.createDirectories(output);
            EnterpriseLabDurableRecoveryProofReport report =
                    new EnterpriseLabDurableRecoveryProofRunner().run(output);
            if (!report.allPassed()) {
                throw new IllegalStateException("one or more durable recovery proof checks failed: "
                        + report);
            }
            Path json = output.resolve("enterprise-lab-durable-recovery-proof.json");
            Path summary = output.resolve("enterprise-lab-durable-recovery-proof-summary.md");
            ObjectMapper mapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .findAndRegisterModules();
            Files.writeString(json, mapper.writeValueAsString(report) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            Files.writeString(summary, markdown(report), StandardCharsets.UTF_8);
            out.println("=== LoadBalancerPro Enterprise Lab Durable Recovery Proof ===");
            out.println("Actual loopback requests: " + report.actualLoopbackRequests());
            out.println("Interrupted final state: " + report.interruptedFinalState());
            out.println("Middle corruption quarantined: " + report.middleCorruptionQuarantined());
            out.println("Terminal compaction verified: " + report.terminalCompactionVerified());
            out.println("All checks passed: " + report.allPassed());
            out.println("Report fingerprint: " + report.contentFingerprint());
            out.println("Proof JSON: " + json);
            out.println("Safety: bounded foreground literal-loopback and target-only local-filesystem proof; "
                    + "no API server, external target, operating-system crash claim, cloud, tenant, or production action.");
            return new Result(true, 0);
        } catch (IOException | RuntimeException exception) {
            err.println("Enterprise Lab durable recovery proof failed safely: " + safeMessage(exception));
            return new Result(true, 1);
        }
    }

    private static Optional<Path> selectedOutput(String[] args) {
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(arg -> arg.startsWith(OUTPUT_FLAG))
                .findFirst()
                .map(arg -> Path.of(arg.substring(OUTPUT_FLAG.length())));
    }

    private static String markdown(EnterpriseLabDurableRecoveryProofReport report) {
        String line = System.lineSeparator();
        return "# Enterprise Lab Durable Recovery Proof" + line + line
                + "- Actual loopback requests: `" + report.actualLoopbackRequests() + "`" + line
                + "- Interrupted final state: `" + report.interruptedFinalState() + "`" + line
                + "- Completed restart preserved: `" + report.completedRestartPreserved() + "`" + line
                + "- Normal rollback restart preserved: `" + report.normalRollbackRestartPreserved() + "`" + line
                + "- Middle corruption quarantined: `" + report.middleCorruptionQuarantined() + "`" + line
                + "- Partial tail quarantined: `" + report.partialTailQuarantined() + "`" + line
                + "- Terminal compaction verified: `" + report.terminalCompactionVerified() + "`" + line
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
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public record Result(boolean requested, int exitCode) {
    }
}
