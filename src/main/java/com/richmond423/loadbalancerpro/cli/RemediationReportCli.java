package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RemediationReportCli {
    private static final String FLAG = "--remediation-report";
    private static final String VERIFY_MANIFEST_FLAG = "--verify-manifest";
    private static final String BUNDLE_FLAG = "--bundle";
    private static final String VERIFY_BUNDLE_FLAG = "--verify-bundle";
    private static final String APP_VERSION = "2.4.2";
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
                .anyMatch(arg -> arg.equals(FLAG) || arg.startsWith(FLAG + "=")
                        || arg.equals(VERIFY_MANIFEST_FLAG) || arg.startsWith(VERIFY_MANIFEST_FLAG + "=")
                        || arg.equals(BUNDLE_FLAG) || arg.startsWith(BUNDLE_FLAG + "=")
                        || arg.equals(VERIFY_BUNDLE_FLAG) || arg.startsWith(VERIFY_BUNDLE_FLAG + "="));
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
            if (options.verifyManifestPath().isPresent()) {
                return verifyManifest(options.verifyManifestPath().get(), out);
            }
            if (options.verifyBundlePath().isPresent()) {
                return verifyBundle(options.verifyBundlePath().get(), out);
            }
            validateOptions(options);
            RemediationReportResponse response = responseFromInput(options);
            String rendered = render(response);
            EvidenceRedactionService.RedactionPlan redactionPlan = redactionPlan(options);
            RedactionContext redaction = redactionContext(options, response, rendered, redactionPlan);
            if (options.bundlePath().isPresent()) {
                return writeBundle(options, response, redaction, out);
            }
            writeOutput(redaction.renderedReport(), options.outputPath(), out);
            writeRedactionSummaryIfRequested(options, redaction);
            writeManifestIfRequested(options, response, redaction);
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
        Path inputPath = options.inputPath()
                .orElseThrow(() -> new IllegalArgumentException("input path is required"));
        JsonNode root = OBJECT_MAPPER.readTree(inputPath.toFile());
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

    private static Result verifyManifest(Path manifestPath, PrintStream out) throws IOException {
        ReportChecksumManifestService service = new ReportChecksumManifestService();
        ReportChecksumManifestService.ManifestVerificationResult result = service.verify(manifestPath);
        out.println((result.verified() ? "Manifest verification passed: " : "Manifest verification failed: ")
                + manifestPath);
        for (ReportChecksumManifestService.ManifestVerificationEntry entry : result.entries()) {
            out.println("- " + entry.status() + " [" + entry.role() + "] " + entry.path()
                    + " expected=" + entry.expectedSha256()
                    + (entry.actualSha256() == null ? "" : " actual=" + entry.actualSha256()));
        }
        return new Result(true, result.verified() ? 0 : 2);
    }

    private static Result verifyBundle(Path bundlePath, PrintStream out) throws IOException {
        IncidentBundleService.BundleVerificationResult result = new IncidentBundleService().verify(bundlePath);
        out.println((result.verified() ? "Incident bundle verification passed: "
                : "Incident bundle verification failed: ") + bundlePath);
        for (String error : result.errors()) {
            out.println("- ERROR " + error);
        }
        for (ReportChecksumManifestService.ManifestVerificationEntry entry : result.entries()) {
            out.println("- " + entry.status() + " [" + entry.role() + "] " + entry.path()
                    + " expected=" + entry.expectedSha256()
                    + (entry.actualSha256() == null ? "" : " actual=" + entry.actualSha256()));
        }
        return new Result(true, result.verified() ? 0 : 2);
    }

    private static Result writeBundle(
            CliOptions options,
            RemediationReportResponse response,
            RedactionContext redaction,
            PrintStream out) throws IOException {
        Path inputPath = options.inputPath()
                .orElseThrow(() -> new IllegalArgumentException("--bundle requires --input <path>"));
        IncidentBundleService.BundleExportResult result = new IncidentBundleService().export(
                new IncidentBundleService.BundleExportRequest(
                        options.bundlePath().get(),
                        inputPath,
                        response.format(),
                        redaction.renderedReport(),
                        redaction.redactedInputJson().orElse(null),
                        redaction.redactionSummaryJson().orElse(null),
                        redaction.payload(),
                        options.generatedBy().orElse(null),
                        options.createdAt().orElse(null),
                        appVersion()));
        writeRedactionSummaryIfRequested(options, redaction);
        out.println("Incident bundle written: " + result.bundlePath());
        out.println("Incident bundle verification passed: " + result.bundlePath());
        return new Result(true, 0);
    }

    private static void writeManifestIfRequested(
            CliOptions options, RemediationReportResponse response, RedactionContext redaction) throws IOException {
        if (options.manifestPath().isEmpty()) {
            return;
        }
        Path outputPath = options.outputPath()
                .orElseThrow(() -> new IllegalArgumentException("--manifest requires --output <path>"));
        Path inputPath = options.inputPath()
                .orElseThrow(() -> new IllegalArgumentException("--manifest requires --input <path>"));
        Path manifestInputPath = inputPath;
        List<Path> manifestExtras = new ArrayList<>(options.manifestExtraPaths());
        if (redaction.enabled()) {
            manifestInputPath = redactedInputPath(outputPath);
            Files.writeString(manifestInputPath, redaction.redactedInputJson().orElseThrow(),
                    StandardCharsets.UTF_8);
            options.redactionSummaryPath().ifPresent(manifestExtras::add);
        }
        ReportChecksumManifestService manifestService = new ReportChecksumManifestService();
        ReportChecksumManifestService.ReportChecksumManifest manifest = manifestService.create(
                new ReportChecksumManifestService.ManifestCreateRequest(
                        options.manifestPath().get(),
                        manifestInputPath,
                        outputPath,
                        manifestExtras,
                        redaction.enabled() ? redaction.payload() : response.json(),
                        options.generatedBy().orElse(null),
                        options.createdAt().orElse(null),
                        appVersion()));
        String manifestJson = manifestService.toJson(manifest);
        if (redaction.enabled()) {
            manifestJson = redaction.plan().redactWithoutCounting(manifestJson);
        }
        Files.writeString(options.manifestPath().get(), manifestJson, StandardCharsets.UTF_8);
    }

    private static void writeRedactionSummaryIfRequested(
            CliOptions options, RedactionContext redaction) throws IOException {
        if (options.redactionSummaryPath().isEmpty()) {
            return;
        }
        Files.writeString(options.redactionSummaryPath().get(), redaction.redactionSummaryJson().orElseThrow(),
                StandardCharsets.UTF_8);
    }

    private static void validateOptions(CliOptions options) {
        if (options.manifestPath().isPresent() && options.outputPath().isEmpty()) {
            throw new IllegalArgumentException("--manifest requires --output <path>");
        }
        if (options.bundlePath().isPresent()
                && (options.outputPath().isPresent() || options.manifestPath().isPresent())) {
            throw new IllegalArgumentException("--bundle writes its own report and manifest; omit --output and --manifest");
        }
        if (options.redactionSummaryPath().isPresent() && !options.redactionRequested()) {
            throw new IllegalArgumentException("--redact-output-summary requires --redact or --redact-file");
        }
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

    private static EvidenceRedactionService.RedactionPlan redactionPlan(CliOptions options) throws IOException {
        EvidenceRedactionService service = new EvidenceRedactionService();
        List<String> values = new ArrayList<>(options.redactionValues());
        for (Path path : options.redactionFilePaths()) {
            values.addAll(service.readRedactionFile(path));
        }
        return service.createPlan(values, options.redactionLabel().orElse(null));
    }

    private static RedactionContext redactionContext(
            CliOptions options,
            RemediationReportResponse response,
            String rendered,
            EvidenceRedactionService.RedactionPlan plan) throws IOException {
        if (!plan.enabled()) {
            return new RedactionContext(
                    rendered,
                    Optional.empty(),
                    response.json(),
                    Optional.empty(),
                    plan,
                    false);
        }
        Path inputPath = options.inputPath()
                .orElseThrow(() -> new IllegalArgumentException("input path is required"));
        EvidenceRedactionService.RedactionSession session = plan.newSession();
        String redactedInput = session.redact(IncidentBundleService.INPUT_ENTRY,
                Files.readString(inputPath, StandardCharsets.UTF_8));
        String reportEntry = response.format() == RemediationReportFormat.JSON
                ? IncidentBundleService.JSON_REPORT_ENTRY
                : IncidentBundleService.MARKDOWN_REPORT_ENTRY;
        String redactedReport = session.redact(reportEntry, rendered);
        RemediationReportPayload redactedPayload = redactedPayload(response.json(), plan);
        String summaryJson = new EvidenceRedactionService().toJson(session.summary());
        return new RedactionContext(
                redactedReport,
                Optional.of(redactedInput),
                redactedPayload,
                Optional.of(summaryJson),
                plan,
                true);
    }

    private static RemediationReportPayload redactedPayload(
            RemediationReportPayload payload, EvidenceRedactionService.RedactionPlan plan) throws IOException {
        JsonNode tree = OBJECT_MAPPER.valueToTree(payload);
        JsonNode redacted = redactJsonNode(tree, plan);
        return OBJECT_MAPPER.treeToValue(redacted, RemediationReportPayload.class);
    }

    private static JsonNode redactJsonNode(JsonNode node, EvidenceRedactionService.RedactionPlan plan) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isTextual()) {
            return TextNode.valueOf(plan.redactWithoutCounting(node.asText()));
        }
        if (node.isArray()) {
            ArrayNode array = OBJECT_MAPPER.createArrayNode();
            for (JsonNode child : node) {
                array.add(redactJsonNode(child, plan));
            }
            return array;
        }
        if (node.isObject()) {
            ObjectNode object = OBJECT_MAPPER.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                object.set(field.getKey(), redactJsonNode(field.getValue(), plan));
            }
            return object;
        }
        return node.deepCopy();
    }

    private static Path redactedInputPath(Path outputPath) {
        Path normalized = outputPath.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        String fileName = normalized.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String stem = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        Path target = Path.of(stem + ".input.redacted.json");
        return parent == null ? target : parent.resolve(target);
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
                + "[--format markdown|json] [--output <path>] [--report-id <id>] [--title <title>] "
                + "[--manifest <path>]");
        err.println("Shorthand: --remediation-report=<path>");
        err.println("Verify: --verify-manifest <manifest.json>");
        err.println("Bundle: --input <saved-evaluation-or-replay.json> [--format markdown|json] "
                + "--bundle <incident-bundle.zip>");
        err.println("Verify bundle: --verify-bundle <incident-bundle.zip>");
        err.println("Redaction: [--redact <literal>] [--redact-file <path>] "
                + "[--redaction-label <label>] [--redact-output-summary <path>]");
        err.println("Safety: offline/read-only report generation; no API server, network access, "
                + "CloudManager calls, or cloud mutation.");
    }

    private static String appVersion() {
        Package packageInfo = RemediationReportCli.class.getPackage();
        String implementationVersion = packageInfo == null ? null : packageInfo.getImplementationVersion();
        return implementationVersion == null || implementationVersion.isBlank()
                ? APP_VERSION
                : implementationVersion.trim();
    }

    public record Result(boolean requested, int exitCode) {
    }

    private record RedactionContext(
            String renderedReport,
            Optional<String> redactedInputJson,
            RemediationReportPayload payload,
            Optional<String> redactionSummaryJson,
            EvidenceRedactionService.RedactionPlan plan,
            boolean enabled) {

        private RedactionContext {
            redactedInputJson = redactedInputJson == null ? Optional.empty() : redactedInputJson;
            redactionSummaryJson = redactionSummaryJson == null ? Optional.empty() : redactionSummaryJson;
        }
    }

    record CliOptions(
            Optional<Path> inputPath,
            RemediationReportFormat format,
            Optional<Path> outputPath,
            Optional<String> reportId,
            Optional<String> title,
            Optional<Path> manifestPath,
            List<Path> manifestExtraPaths,
            Optional<Path> verifyManifestPath,
            Optional<Path> bundlePath,
            Optional<Path> verifyBundlePath,
            Optional<String> generatedBy,
            Optional<String> createdAt,
            List<String> redactionValues,
            List<Path> redactionFilePaths,
            Optional<String> redactionLabel,
            Optional<Path> redactionSummaryPath) {

        CliOptions {
            inputPath = inputPath == null ? Optional.empty() : inputPath;
            outputPath = outputPath == null ? Optional.empty() : outputPath;
            reportId = reportId == null ? Optional.empty() : reportId;
            title = title == null ? Optional.empty() : title;
            manifestPath = manifestPath == null ? Optional.empty() : manifestPath;
            manifestExtraPaths = manifestExtraPaths == null ? List.of() : List.copyOf(manifestExtraPaths);
            verifyManifestPath = verifyManifestPath == null ? Optional.empty() : verifyManifestPath;
            bundlePath = bundlePath == null ? Optional.empty() : bundlePath;
            verifyBundlePath = verifyBundlePath == null ? Optional.empty() : verifyBundlePath;
            generatedBy = generatedBy == null ? Optional.empty() : generatedBy;
            createdAt = createdAt == null ? Optional.empty() : createdAt;
            redactionValues = redactionValues == null ? List.of() : List.copyOf(redactionValues);
            redactionFilePaths = redactionFilePaths == null ? List.of() : List.copyOf(redactionFilePaths);
            redactionLabel = redactionLabel == null ? Optional.empty() : redactionLabel;
            redactionSummaryPath = redactionSummaryPath == null ? Optional.empty() : redactionSummaryPath;
        }

        static CliOptions parse(String[] args) {
            Optional<Path> verifyManifest = optionValue(args, VERIFY_MANIFEST_FLAG).map(Path::of);
            if (verifyManifest.isPresent()) {
                return new CliOptions(Optional.empty(), RemediationReportFormat.MARKDOWN, Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), List.of(), verifyManifest,
                        Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), List.of(), List.of(), Optional.empty(), Optional.empty());
            }
            Optional<Path> verifyBundle = optionValue(args, VERIFY_BUNDLE_FLAG).map(Path::of);
            if (verifyBundle.isPresent()) {
                return new CliOptions(Optional.empty(), RemediationReportFormat.MARKDOWN, Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(),
                        Optional.empty(), verifyBundle,
                        Optional.empty(), Optional.empty(), List.of(), List.of(), Optional.empty(), Optional.empty());
            }
            Path input = remediationReportPath(args)
                    .map(Path::of)
                    .or(() -> optionValue(args, "--input").map(Path::of))
                    .orElseThrow(() -> new IllegalArgumentException("input path is required"));
            RemediationReportFormat format = optionValue(args, "--format")
                    .map(CliOptions::format)
                    .orElse(RemediationReportFormat.MARKDOWN);
            Optional<Path> output = optionValue(args, "--output").map(Path::of);
            Optional<String> reportId = optionValue(args, "--report-id").filter(value -> !value.isBlank());
            Optional<String> title = optionValue(args, "--title").filter(value -> !value.isBlank());
            Optional<Path> manifest = optionValue(args, "--manifest").map(Path::of);
            Optional<Path> bundle = optionValue(args, BUNDLE_FLAG).map(Path::of);
            List<String> redactions = optionValues(args, "--redact").stream()
                    .filter(value -> !value.isBlank())
                    .toList();
            List<Path> redactionFiles = optionValues(args, "--redact-file").stream()
                    .filter(value -> !value.isBlank())
                    .map(Path::of)
                    .toList();
            Optional<String> redactionLabel = optionValue(args, "--redaction-label")
                    .filter(value -> !value.isBlank());
            Optional<Path> redactionSummary = optionValue(args, "--redact-output-summary").map(Path::of);
            List<Path> manifestExtras = optionValues(args, "--manifest-extra").stream()
                    .filter(value -> !value.isBlank())
                    .map(Path::of)
                    .toList();
            Optional<String> generatedBy = optionValue(args, "--generated-by").filter(value -> !value.isBlank());
            Optional<String> createdAt = optionValue(args, "--created-at").filter(value -> !value.isBlank());
            return new CliOptions(Optional.of(input), format, output, reportId, title, manifest, manifestExtras,
                    Optional.empty(), bundle, Optional.empty(), generatedBy, createdAt, redactions, redactionFiles,
                    redactionLabel, redactionSummary);
        }

        private boolean redactionRequested() {
            return !redactionValues.isEmpty() || !redactionFilePaths.isEmpty();
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

        private static List<String> optionValues(String[] args, String option) {
            List<String> values = new ArrayList<>();
            for (int index = 0; index < args.length; index++) {
                String arg = args[index];
                if (arg == null) {
                    continue;
                }
                if (arg.startsWith(option + "=")) {
                    values.add(arg.substring((option + "=").length()).trim());
                    continue;
                }
                if (arg.equals(option)) {
                    if (index + 1 >= args.length || args[index + 1] == null || args[index + 1].startsWith("--")) {
                        throw new IllegalArgumentException(option + " requires a value");
                    }
                    values.add(args[index + 1].trim());
                    index++;
                }
            }
            return values;
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
