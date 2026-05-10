package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.api.AllocationEvaluationResponse;
import com.richmond423.loadbalancerpro.api.RemediationReportFormat;
import com.richmond423.loadbalancerpro.api.RemediationReportPayload;
import com.richmond423.loadbalancerpro.api.RemediationReportRequest;
import com.richmond423.loadbalancerpro.api.RemediationReportResponse;
import com.richmond423.loadbalancerpro.api.RemediationReportService;
import com.richmond423.loadbalancerpro.api.ScenarioReplayResponse;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class RemediationReportCli {
    private static final String FLAG = "--remediation-report";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RemediationReportCli() {
    }

    public static void main(String[] args) {
        Result result = run(args, System.out, System.err);
        if (result.exitCode() != 0) {
            System.exit(result.exitCode());
        }
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
            CliOptions options = CliOptions.parse(args);
            RemediationReportResponse response = responseFromInput(options);
            String rendered = render(response);
            writeOutput(rendered, options.outputPath(), out);
            return new Result(true, 0);
        } catch (IllegalArgumentException e) {
            err.println("Remediation report export failed safely: " + safeMessage(e));
            printUsage(err);
            return new Result(true, 2);
        } catch (IOException e) {
            err.println("Remediation report export failed safely: " + safeMessage(e));
            return new Result(true, 2);
        } catch (RuntimeException e) {
            err.println("Remediation report export failed safely: " + safeMessage(e));
            return new Result(true, 1);
        }
    }

    static RemediationReportResponse responseFromInput(CliOptions options) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(options.inputPath().toFile());
        RemediationReportService service = new RemediationReportService();
        if (root.has("json") && root.path("json").has("sourceType")) {
            return responseForPayload(options.format(),
                    OBJECT_MAPPER.treeToValue(root.path("json"), RemediationReportPayload.class),
                    service);
        }
        if (root.has("sourceType") && root.has("remediationPlan")) {
            return responseForPayload(options.format(),
                    OBJECT_MAPPER.treeToValue(root, RemediationReportPayload.class),
                    service);
        }
        return service.export(requestFromJson(root, options));
    }

    private static RemediationReportRequest requestFromJson(JsonNode root, CliOptions options) throws IOException {
        RemediationReportFormat format = options.format();
        String reportId = options.reportId().orElse(null);
        String title = options.title().orElse(null);

        if (root.has("evaluation") || root.has("replay")) {
            RemediationReportRequest request = OBJECT_MAPPER.treeToValue(root, RemediationReportRequest.class);
            return new RemediationReportRequest(
                    firstNonBlank(reportId, request.reportId()),
                    firstNonBlank(title, request.title()),
                    format,
                    request.evaluation(),
                    request.replay());
        }
        if (root.has("scenarioId") && root.has("steps")) {
            ScenarioReplayResponse replay = OBJECT_MAPPER.treeToValue(root, ScenarioReplayResponse.class);
            return new RemediationReportRequest(reportId, title, format, null, replay);
        }
        if (root.has("acceptedLoad") && root.has("remediationPlan")) {
            AllocationEvaluationResponse evaluation = OBJECT_MAPPER.treeToValue(
                    root, AllocationEvaluationResponse.class);
            return new RemediationReportRequest(reportId, title, format, evaluation, null);
        }

        throw new IllegalArgumentException(
                "Input JSON must be an allocation evaluation response, scenario replay response, or report request");
    }

    private static RemediationReportResponse responseForPayload(
            RemediationReportFormat format, RemediationReportPayload payload, RemediationReportService service) {
        String report = format == RemediationReportFormat.MARKDOWN ? service.renderMarkdown(payload) : null;
        return new RemediationReportResponse(
                format,
                format == RemediationReportFormat.MARKDOWN ? "text/markdown" : "application/json",
                report,
                payload,
                payload.readOnly(),
                payload.advisoryOnly(),
                payload.cloudMutation());
    }

    private static String render(RemediationReportResponse response) throws IOException {
        if (response.format() == RemediationReportFormat.MARKDOWN) {
            return response.report();
        }
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(response.json())
                + System.lineSeparator();
    }

    private static void writeOutput(String rendered, Optional<Path> outputPath, PrintStream out) throws IOException {
        if (outputPath.isPresent()) {
            Files.writeString(outputPath.get(), rendered, StandardCharsets.UTF_8);
            return;
        }
        out.print(rendered);
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private static void printUsage(PrintStream err) {
        err.println("Usage: --remediation-report --input <saved-evaluation-or-replay.json> "
                + "[--format markdown|json] [--output <path>] [--report-id <id>] [--title <title>]");
        err.println("Shorthand: --remediation-report=<path>");
        err.println("Safety: offline/read-only report generation; no API server, network access, "
                + "CloudManager calls, or cloud mutation.");
    }

    public record Result(boolean requested, int exitCode) {
    }

    record CliOptions(
            Path inputPath,
            RemediationReportFormat format,
            Optional<Path> outputPath,
            Optional<String> reportId,
            Optional<String> title) {

        static CliOptions parse(String[] args) {
            Path input = remediationReportPath(args)
                    .or(() -> optionValue(args, "--input").map(Path::of))
                    .orElseThrow(() -> new IllegalArgumentException("input path is required"));
            RemediationReportFormat format = optionValue(args, "--format")
                    .map(CliOptions::format)
                    .orElse(RemediationReportFormat.MARKDOWN);
            Optional<Path> output = optionValue(args, "--output").map(Path::of);
            Optional<String> reportId = optionValue(args, "--report-id").filter(value -> !value.isBlank());
            Optional<String> title = optionValue(args, "--title").filter(value -> !value.isBlank());
            return new CliOptions(input, format, output, reportId, title);
        }

        private static Optional<String> remediationReportPath(String[] args) {
            return Arrays.stream(args)
                    .filter(Objects::nonNull)
                    .filter(arg -> arg.startsWith(FLAG + "="))
                    .findFirst()
                    .map(arg -> arg.substring((FLAG + "=").length()).trim())
                    .filter(value -> !value.isBlank());
        }

        private static Optional<String> optionValue(String[] args, String option) {
            for (int index = 0; index < args.length; index++) {
                String arg = args[index];
                if (arg == null) {
                    continue;
                }
                if (arg.startsWith(option + "=")) {
                    return Optional.of(arg.substring((option + "=").length()).trim());
                }
                if (arg.equals(option)) {
                    if (index + 1 >= args.length || args[index + 1] == null || args[index + 1].startsWith("--")) {
                        throw new IllegalArgumentException(option + " requires a value");
                    }
                    return Optional.of(args[index + 1].trim());
                }
            }
            return Optional.empty();
        }

        private static RemediationReportFormat format(String value) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "markdown", "md" -> RemediationReportFormat.MARKDOWN;
                case "json" -> RemediationReportFormat.JSON;
                default -> throw new IllegalArgumentException("format must be markdown or json");
            };
        }
    }
}
