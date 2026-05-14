package com.richmond423.loadbalancerpro.cli;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceExporter;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabMode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabRun;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabRunService;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class EnterpriseLabWorkflowCommand {
    private static final String FLAG = "--enterprise-lab-workflow";
    private static final String OUTPUT_FLAG = "--enterprise-lab-output=";
    private static final Path DEFAULT_OUTPUT = Path.of("target", "enterprise-lab-runs");

    private EnterpriseLabWorkflowCommand() {
    }

    public static boolean isRequested(String[] args) {
        if (args == null) {
            return false;
        }
        return Arrays.stream(args)
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
            EnterpriseLabMode mode = EnterpriseLabMode.from(selectedMode(args).orElse("shadow"));
            Path outputDirectory = selectedOutput(args).orElse(DEFAULT_OUTPUT);
            EnterpriseLabRunService runService = new EnterpriseLabRunService();
            EnterpriseLabRun run = runService.run(null, mode.wireValue(), "summary");
            EnterpriseLabEvidenceExporter.EnterpriseLabEvidenceManifest manifest =
                    new EnterpriseLabEvidenceExporter().export(
                            outputDirectory,
                            runService.listScenarioMetadata(),
                            run,
                            gitCommit());

            out.println("=== LoadBalancerPro Enterprise Lab Workflow ===");
            out.println("Mode: " + mode.wireValue());
            out.println("Run id: " + run.runId());
            out.println("Scenario count: " + run.scorecard().totalScenarios());
            out.println("Scorecard recommendation: " + run.scorecard().finalRecommendation());
            out.println("Evidence directory: " + manifest.outputDirectory());
            out.println("Scenario catalog JSON: " + manifest.scenarioCatalogJson());
            out.println("Lab run JSON: " + manifest.labRunJson());
            out.println("Markdown summary: " + manifest.markdownSummary());
            out.println("Safety: ignored target/ evidence only; no API server, live cloud, external network, release, tag, asset, container, or registry action.");
            return new Result(true, 0);
        } catch (RuntimeException | java.io.IOException exception) {
            err.println("Enterprise Lab workflow failed safely: " + safeMessage(exception));
            return new Result(true, 1);
        }
    }

    private static Optional<String> selectedMode(String[] args) {
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(arg -> arg.equals(FLAG) || arg.startsWith(FLAG + "="))
                .findFirst()
                .map(arg -> arg.equals(FLAG) ? "shadow" : arg.substring((FLAG + "=").length()));
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

