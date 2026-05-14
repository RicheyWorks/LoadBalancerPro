package com.richmond423.loadbalancerpro.cli;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentReportFormatter;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentService;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class AdaptiveRoutingExperimentCommand {
    private static final String FLAG = "--adaptive-routing-experiment";
    private static final List<String> VALID_MODES = List.of(
            "off", "shadow", "recommend", "active-experiment", "influence", "all");

    private AdaptiveRoutingExperimentCommand() {
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

        String mode = selectedMode(args).orElse("shadow");
        if (!VALID_MODES.contains(mode)) {
            err.println("Invalid adaptive routing experiment mode: " + mode);
            err.println("Valid values: off, shadow, recommend, active-experiment, all");
            return new Result(true, 2);
        }

        try {
            out.println("=== LoadBalancerPro Adaptive Routing Experiment ===");
            out.println("Mode: deterministic local experiment harness.");
            out.println("Safety: no API server, no live cloud mutation, no external network, no release action.");
            out.println("Default posture: off/shadow-first; active-experiment LASE influence is explicit opt-in for experiment output.");
            AdaptiveRoutingExperimentService service = new AdaptiveRoutingExperimentService();
            AdaptiveRoutingExperimentReportFormatter formatter = new AdaptiveRoutingExperimentReportFormatter();
            if ("all".equals(mode)) {
                out.println();
                out.println(formatter.format(service.runCatalog(AdaptiveRoutingPolicyMode.OFF)));
                out.println();
                out.println(formatter.format(service.runCatalog(AdaptiveRoutingPolicyMode.SHADOW)));
                out.println();
                out.println(formatter.format(service.runCatalog(AdaptiveRoutingPolicyMode.RECOMMEND)));
                out.println();
                out.println(formatter.format(service.runCatalog(AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT)));
            } else {
                out.println();
                out.println(formatter.format(service.runCatalog(AdaptiveRoutingPolicyMode.from(mode))));
            }
            return new Result(true, 0);
        } catch (RuntimeException e) {
            err.println("Adaptive routing experiment failed safely: " + safeMessage(e));
            return new Result(true, 1);
        }
    }

    private static Optional<String> selectedMode(String[] args) {
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(arg -> arg.equals(FLAG) || arg.startsWith(FLAG + "="))
                .findFirst()
                .map(arg -> arg.equals(FLAG) ? "shadow" : normalize(arg.substring((FLAG + "=").length())));
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String safeMessage(RuntimeException e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    public record Result(boolean requested, int exitCode) {
    }
}
