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
    private static final String AUDIT_LOG_FLAG = "--audit-log";
    private static final String VERIFY_AUDIT_LOG_FLAG = "--verify-audit-log";
    private static final String INVENTORY_FLAG = "--inventory";
    private static final String DIFF_INVENTORY_FLAG = "--diff-inventory";
    private static final String POLICY_FLAG = "--policy";
    private static final String POLICY_TEMPLATE_FLAG = "--policy-template";
    private static final String LIST_POLICY_TEMPLATES_FLAG = "--list-policy-templates";
    private static final String EXPORT_POLICY_TEMPLATE_FLAG = "--export-policy-template";
    private static final String VALIDATE_POLICY_FLAG = "--validate-policy";
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
                        || arg.equals(VERIFY_BUNDLE_FLAG) || arg.startsWith(VERIFY_BUNDLE_FLAG + "=")
                        || arg.equals(VERIFY_AUDIT_LOG_FLAG) || arg.startsWith(VERIFY_AUDIT_LOG_FLAG + "=")
                        || arg.equals(INVENTORY_FLAG) || arg.startsWith(INVENTORY_FLAG + "=")
                        || arg.equals(DIFF_INVENTORY_FLAG) || arg.startsWith(DIFF_INVENTORY_FLAG + "=")
                        || arg.equals(LIST_POLICY_TEMPLATES_FLAG)
                        || arg.equals(EXPORT_POLICY_TEMPLATE_FLAG) || arg.startsWith(EXPORT_POLICY_TEMPLATE_FLAG + "=")
                        || arg.equals(VALIDATE_POLICY_FLAG) || arg.startsWith(VALIDATE_POLICY_FLAG + "="));
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
            if (policyTemplateCommandRequested(args)) {
                return runPolicyTemplateCommand(args, out);
            }
            if (catalogDiffRequested(args)) {
                return writeCatalogDiff(CatalogDiffOptions.parse(args), out);
            }
            CliOptions options = CliOptions.parse(args);
            if (options.inventoryPath().isPresent()) {
                return writeInventory(options, out);
            }
            if (options.verifyAuditLogPath().isPresent()) {
                return verifyAuditLog(options.verifyAuditLogPath().get(), out);
            }
            if (options.verifyManifestPath().isPresent()) {
                Result result = verifyManifest(options.verifyManifestPath().get(), out);
                appendAuditIfSuccessful(options, result, "MANIFEST_VERIFIED",
                        null, null, options.verifyManifestPath().get(), null, false,
                        List.of(auditFile("MANIFEST", options.verifyManifestPath().get())));
                return result;
            }
            if (options.verifyBundlePath().isPresent()) {
                Result result = verifyBundle(options.verifyBundlePath().get(), out);
                appendAuditIfSuccessful(options, result, "BUNDLE_VERIFIED",
                        null, null, null, options.verifyBundlePath().get(), false,
                        List.of(auditFile("BUNDLE", options.verifyBundlePath().get())));
                return result;
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
            Optional<ManifestWriteResult> manifestWrite = writeManifestIfRequested(options, response, redaction);
            appendReportAudit(options, response, redaction);
            if (manifestWrite.isPresent()) {
                appendManifestAudit(options, manifestWrite.get(), redaction);
            }
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

    private static Result verifyAuditLog(Path auditLogPath, PrintStream out) throws IOException {
        OfflineCliAuditLogService.AuditVerificationResult result =
                new OfflineCliAuditLogService().verify(auditLogPath);
        out.println((result.verified() ? "Audit log verification passed: "
                : "Audit log verification failed: ") + auditLogPath);
        out.println("- entries=" + result.entryCount() + " latestEntryHash=" + result.latestEntryHash());
        for (String error : result.errors()) {
            out.println("- ERROR " + error);
        }
        return new Result(true, result.verified() ? 0 : 2);
    }

    private static Result writeInventory(CliOptions options, PrintStream out) throws IOException {
        EvidenceInventoryService service = new EvidenceInventoryService();
        EvidenceInventoryService.EvidenceCatalog catalog = service.inventory(
                new EvidenceInventoryService.InventoryRequest(
                        options.inventoryPath().get(),
                        options.verifyInventory(),
                        options.includeInventoryHashes()));
        String rendered = options.inventoryFormat() == EvidenceInventoryService.InventoryFormat.JSON
                ? service.renderJson(catalog)
                : service.renderMarkdown(catalog);
        writeOutput(rendered, options.inventoryOutputPath(), out);
        int exitCode = options.failOnInvalid() && catalog.summary().failureCount() > 0 ? 2 : 0;
        return new Result(true, exitCode);
    }

    private static Result writeCatalogDiff(CatalogDiffOptions options, PrintStream out) throws IOException {
        EvidenceCatalogDiffService service = new EvidenceCatalogDiffService();
        EvidenceCatalogDiffService.EvidenceCatalogDiff diff = service.diff(
                new EvidenceCatalogDiffService.DiffRequest(
                        options.beforeCatalogPath(),
                        options.afterCatalogPath(),
                        options.includeUnchanged()));
        if (options.policyPath().isPresent()) {
            EvidenceHandoffPolicyService policyService = new EvidenceHandoffPolicyService();
            EvidenceHandoffPolicyService.HandoffPolicy policy = policyService.readPolicy(options.policyPath().get());
            return writePolicyReport(options, out, diff, policyService, policy);
        }
        if (options.policyTemplateName().isPresent()) {
            EvidenceHandoffPolicyService policyService = new EvidenceHandoffPolicyService();
            EvidencePolicyTemplateService templateService = new EvidencePolicyTemplateService();
            String templateName = options.policyTemplateName().get();
            EvidenceHandoffPolicyService.HandoffPolicy policy = policyService.readPolicyJson(
                    "template:" + templateName,
                    templateService.templateJson(templateName));
            return writePolicyReport(options, out, diff, policyService, policy);
        }
        String rendered = options.diffFormat() == EvidenceCatalogDiffService.DiffFormat.JSON
                ? service.renderJson(diff)
                : service.renderMarkdown(diff);
        writeOutput(rendered, options.outputPath(), out);
        int exitCode = options.failOnDrift() && diff.hasDrift() ? 2 : 0;
        return new Result(true, exitCode);
    }

    private static Result writePolicyReport(
            CatalogDiffOptions options,
            PrintStream out,
            EvidenceCatalogDiffService.EvidenceCatalogDiff diff,
            EvidenceHandoffPolicyService policyService,
            EvidenceHandoffPolicyService.HandoffPolicy policy) throws IOException {
            EvidenceHandoffPolicyService.PolicyEvaluation evaluation = policyService.evaluate(diff, policy);
            String rendered = options.policyReportFormat() == EvidenceHandoffPolicyService.PolicyReportFormat.JSON
                    ? policyService.renderJson(evaluation)
                    : policyService.renderMarkdown(evaluation);
            writeOutput(rendered, options.policyOutputPath(), out);
            int exitCode = options.failOnPolicyFail()
                    && evaluation.decision() == EvidenceHandoffPolicyService.PolicyDecision.FAIL ? 2 : 0;
            return new Result(true, exitCode);
    }

    private static Result runPolicyTemplateCommand(String[] args, PrintStream out) throws IOException {
        EvidencePolicyTemplateService service = new EvidencePolicyTemplateService();
        if (CatalogDiffOptions.hasFlag(args, LIST_POLICY_TEMPLATES_FLAG)) {
            out.print(service.renderTemplateList());
            return new Result(true, 0);
        }
        Optional<String> exportTemplate = CatalogDiffOptions.optionValue(args, EXPORT_POLICY_TEMPLATE_FLAG)
                .filter(value -> !value.isBlank());
        if (exportTemplate.isPresent()) {
            String templateJson = service.templateJson(exportTemplate.get());
            Optional<Path> outputPath = CatalogDiffOptions.optionValue(args, "--policy-output").map(Path::of);
            writeOutput(templateJson, outputPath, out);
            return new Result(true, 0);
        }
        Optional<Path> validatePolicy = CatalogDiffOptions.optionValue(args, VALIDATE_POLICY_FLAG).map(Path::of);
        if (validatePolicy.isPresent()) {
            EvidenceHandoffPolicyService.HandoffPolicy policy = service.validatePolicy(validatePolicy.get());
            out.println("Policy validation passed: " + validatePolicy.get());
            out.println("- mode=" + policy.mode());
            out.println("- defaultSeverity=" + policy.defaultSeverity());
            out.println("- rules=" + policy.rules().size());
            return new Result(true, 0);
        }
        throw new IllegalArgumentException("policy template command is incomplete");
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
        appendAuditIfRequested(options, "BUNDLE_GENERATED",
                inputPath, null, null, options.bundlePath().get(), redaction.enabled(),
                List.of(auditFile("BUNDLE", options.bundlePath().get())));
        return new Result(true, 0);
    }

    private static Optional<ManifestWriteResult> writeManifestIfRequested(
            CliOptions options, RemediationReportResponse response, RedactionContext redaction) throws IOException {
        if (options.manifestPath().isEmpty()) {
            return Optional.empty();
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
        return Optional.of(new ManifestWriteResult(
                options.manifestPath().get(),
                manifestInputPath,
                outputPath,
                List.copyOf(manifestExtras)));
    }

    private static void writeRedactionSummaryIfRequested(
            CliOptions options, RedactionContext redaction) throws IOException {
        if (options.redactionSummaryPath().isEmpty()) {
            return;
        }
        Files.writeString(options.redactionSummaryPath().get(), redaction.redactionSummaryJson().orElseThrow(),
                StandardCharsets.UTF_8);
    }

    private static void appendReportAudit(
            CliOptions options,
            RemediationReportResponse response,
            RedactionContext redaction) throws IOException {
        String action = response.format() == RemediationReportFormat.JSON
                ? "JSON_REPORT_GENERATED"
                : "MARKDOWN_REPORT_GENERATED";
        List<OfflineCliAuditLogService.AuditFileSource> files = new ArrayList<>();
        options.inputPath().ifPresent(path -> files.add(auditFile("INPUT", path)));
        options.outputPath().ifPresent(path -> files.add(auditFile("REPORT", path)));
        options.redactionSummaryPath().ifPresent(path -> files.add(auditFile("REDACTION_SUMMARY", path)));
        appendAuditIfRequested(options, action,
                options.inputPath().orElse(null),
                options.outputPath().orElse(null),
                null,
                null,
                redaction.enabled(),
                files);
    }

    private static void appendManifestAudit(
            CliOptions options,
            ManifestWriteResult manifestWrite,
            RedactionContext redaction) throws IOException {
        List<OfflineCliAuditLogService.AuditFileSource> files = new ArrayList<>();
        files.add(auditFile("INPUT", manifestWrite.inputPath()));
        files.add(auditFile("REPORT", manifestWrite.reportPath()));
        files.add(auditFile("MANIFEST", manifestWrite.manifestPath()));
        for (Path extra : manifestWrite.extraPaths()) {
            files.add(auditFile("EXTRA", extra));
        }
        appendAuditIfRequested(options, "MANIFEST_GENERATED",
                manifestWrite.inputPath(),
                manifestWrite.reportPath(),
                manifestWrite.manifestPath(),
                null,
                redaction.enabled(),
                files);
    }

    private static void appendAuditIfSuccessful(
            CliOptions options,
            Result result,
            String action,
            Path inputPath,
            Path outputPath,
            Path manifestPath,
            Path bundlePath,
            boolean redactionApplied,
            List<OfflineCliAuditLogService.AuditFileSource> files) throws IOException {
        if (result.exitCode() == 0) {
            appendAuditIfRequested(options, action, inputPath, outputPath, manifestPath, bundlePath,
                    redactionApplied, files);
        }
    }

    private static void appendAuditIfRequested(
            CliOptions options,
            String action,
            Path inputPath,
            Path outputPath,
            Path manifestPath,
            Path bundlePath,
            boolean redactionApplied,
            List<OfflineCliAuditLogService.AuditFileSource> files) throws IOException {
        if (options.auditLogPath().isEmpty()) {
            return;
        }
        new OfflineCliAuditLogService().append(new OfflineCliAuditLogService.AuditAppendRequest(
                options.auditLogPath().get(),
                action,
                options.auditActionId().orElse(null),
                options.auditActor().orElse(null),
                options.auditNote().orElse(null),
                inputPath,
                outputPath,
                manifestPath,
                bundlePath,
                OfflineCliAuditLogService.SUCCESS,
                redactionApplied,
                files));
    }

    private static OfflineCliAuditLogService.AuditFileSource auditFile(String role, Path path) {
        return new OfflineCliAuditLogService.AuditFileSource(role, path);
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
        if (options.auditLogPath().isEmpty()
                && (options.auditActor().isPresent()
                || options.auditActionId().isPresent()
                || options.auditNote().isPresent())) {
            throw new IllegalArgumentException("--audit-actor, --audit-action-id, and --audit-note require --audit-log");
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
        err.println("Audit: [--audit-log <audit.jsonl>] [--audit-actor <label>] "
                + "[--audit-action-id <id>] [--audit-note <note>]");
        err.println("Verify audit log: --verify-audit-log <audit.jsonl>");
        err.println("Inventory: --inventory <directory> [--inventory-format markdown|json] "
                + "[--inventory-output <path>] [--verify-inventory] [--include-hashes] [--fail-on-invalid]");
        err.println("Diff inventory: --diff-inventory <before.json> <after.json> "
                + "[--diff-format markdown|json] [--diff-output <path>] [--fail-on-drift] [--include-unchanged]");
        err.println("Handoff policy: --diff-inventory <before.json> <after.json> "
                + "[--policy <policy.json> | --policy-template <name>] "
                + "[--policy-report-format markdown|json] [--policy-output <path>] [--fail-on-policy-fail]");
        err.println("Policy templates: --list-policy-templates | --export-policy-template <name> "
                + "[--policy-output <path>] | --validate-policy <policy.json>");
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

    private static boolean catalogDiffRequested(String[] args) {
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .anyMatch(arg -> arg.equals(DIFF_INVENTORY_FLAG) || arg.startsWith(DIFF_INVENTORY_FLAG + "="));
    }

    private static boolean policyTemplateCommandRequested(String[] args) {
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .anyMatch(arg -> arg.equals(LIST_POLICY_TEMPLATES_FLAG)
                        || arg.equals(EXPORT_POLICY_TEMPLATE_FLAG)
                        || arg.startsWith(EXPORT_POLICY_TEMPLATE_FLAG + "=")
                        || arg.equals(VALIDATE_POLICY_FLAG)
                        || arg.startsWith(VALIDATE_POLICY_FLAG + "="));
    }

    public record Result(boolean requested, int exitCode) {
    }

    private record CatalogDiffOptions(
            Path beforeCatalogPath,
            Path afterCatalogPath,
            EvidenceCatalogDiffService.DiffFormat diffFormat,
            Optional<Path> outputPath,
            boolean failOnDrift,
            boolean includeUnchanged,
            Optional<Path> policyPath,
            Optional<String> policyTemplateName,
            EvidenceHandoffPolicyService.PolicyReportFormat policyReportFormat,
            Optional<Path> policyOutputPath,
            boolean failOnPolicyFail) {

        private CatalogDiffOptions {
            Objects.requireNonNull(beforeCatalogPath, "before catalog path cannot be null");
            Objects.requireNonNull(afterCatalogPath, "after catalog path cannot be null");
            diffFormat = diffFormat == null ? EvidenceCatalogDiffService.DiffFormat.MARKDOWN : diffFormat;
            outputPath = outputPath == null ? Optional.empty() : outputPath;
            policyPath = policyPath == null ? Optional.empty() : policyPath;
            policyTemplateName = policyTemplateName == null ? Optional.empty() : policyTemplateName;
            if (policyPath.isPresent() && policyTemplateName.isPresent()) {
                throw new IllegalArgumentException("--policy and --policy-template cannot be used together");
            }
            policyReportFormat = policyReportFormat == null
                    ? EvidenceHandoffPolicyService.PolicyReportFormat.MARKDOWN
                    : policyReportFormat;
            policyOutputPath = policyOutputPath == null ? Optional.empty() : policyOutputPath;
        }

        private static CatalogDiffOptions parse(String[] args) {
            List<Path> catalogs = diffInventoryPaths(args);
            EvidenceCatalogDiffService.DiffFormat format = optionValue(args, "--diff-format")
                    .map(EvidenceCatalogDiffService.DiffFormat::parse)
                    .orElse(EvidenceCatalogDiffService.DiffFormat.MARKDOWN);
            Optional<Path> output = optionValue(args, "--diff-output").map(Path::of);
            Optional<Path> policy = optionValue(args, POLICY_FLAG).map(Path::of);
            Optional<String> policyTemplate = optionValue(args, POLICY_TEMPLATE_FLAG)
                    .filter(value -> !value.isBlank());
            EvidenceHandoffPolicyService.PolicyReportFormat policyFormat =
                    optionValue(args, "--policy-report-format")
                            .map(EvidenceHandoffPolicyService.PolicyReportFormat::parse)
                            .orElse(EvidenceHandoffPolicyService.PolicyReportFormat.MARKDOWN);
            Optional<Path> policyOutput = optionValue(args, "--policy-output").map(Path::of);
            return new CatalogDiffOptions(
                    catalogs.get(0),
                    catalogs.get(1),
                    format,
                    output,
                    hasFlag(args, "--fail-on-drift"),
                    hasFlag(args, "--include-unchanged"),
                    policy,
                    policyTemplate,
                    policyFormat,
                    policyOutput,
                    hasFlag(args, "--fail-on-policy-fail"));
        }

        private static List<Path> diffInventoryPaths(String[] args) {
            for (int index = 0; index < args.length; index++) {
                String arg = args[index];
                if (arg == null) {
                    continue;
                }
                if (arg.startsWith(DIFF_INVENTORY_FLAG + "=")) {
                    String raw = arg.substring((DIFF_INVENTORY_FLAG + "=").length()).trim();
                    String[] values = raw.split(",", -1);
                    if (values.length != 2 || values[0].isBlank() || values[1].isBlank()) {
                        throw new IllegalArgumentException(
                                "--diff-inventory requires before and after catalog paths");
                    }
                    return List.of(Path.of(values[0].trim()), Path.of(values[1].trim()));
                }
                if (arg.equals(DIFF_INVENTORY_FLAG)) {
                    if (index + 2 >= args.length
                            || args[index + 1] == null
                            || args[index + 2] == null
                            || args[index + 1].startsWith("--")
                            || args[index + 2].startsWith("--")) {
                        throw new IllegalArgumentException(
                                "--diff-inventory requires before and after catalog paths");
                    }
                    return List.of(Path.of(args[index + 1].trim()), Path.of(args[index + 2].trim()));
                }
            }
            throw new IllegalArgumentException("--diff-inventory requires before and after catalog paths");
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

        private static boolean hasFlag(String[] args, String option) {
            return Arrays.stream(args)
                    .filter(Objects::nonNull)
                    .anyMatch(arg -> arg.equals(option));
        }
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

    private record ManifestWriteResult(
            Path manifestPath,
            Path inputPath,
            Path reportPath,
            List<Path> extraPaths) {

        private ManifestWriteResult {
            extraPaths = extraPaths == null ? List.of() : List.copyOf(extraPaths);
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
            Optional<Path> redactionSummaryPath,
            Optional<Path> auditLogPath,
            Optional<Path> verifyAuditLogPath,
            Optional<String> auditActor,
            Optional<String> auditActionId,
            Optional<String> auditNote,
            Optional<Path> inventoryPath,
            EvidenceInventoryService.InventoryFormat inventoryFormat,
            Optional<Path> inventoryOutputPath,
            boolean verifyInventory,
            boolean includeInventoryHashes,
            boolean failOnInvalid) {

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
            auditLogPath = auditLogPath == null ? Optional.empty() : auditLogPath;
            verifyAuditLogPath = verifyAuditLogPath == null ? Optional.empty() : verifyAuditLogPath;
            auditActor = auditActor == null ? Optional.empty() : auditActor;
            auditActionId = auditActionId == null ? Optional.empty() : auditActionId;
            auditNote = auditNote == null ? Optional.empty() : auditNote;
            inventoryPath = inventoryPath == null ? Optional.empty() : inventoryPath;
            inventoryFormat = inventoryFormat == null
                    ? EvidenceInventoryService.InventoryFormat.MARKDOWN
                    : inventoryFormat;
            inventoryOutputPath = inventoryOutputPath == null ? Optional.empty() : inventoryOutputPath;
        }

        static CliOptions parse(String[] args) {
            Optional<Path> auditLog = optionValue(args, AUDIT_LOG_FLAG).map(Path::of);
            Optional<Path> verifyAuditLog = optionValue(args, VERIFY_AUDIT_LOG_FLAG).map(Path::of);
            Optional<String> auditActor = optionValue(args, "--audit-actor").filter(value -> !value.isBlank());
            Optional<String> auditActionId = optionValue(args, "--audit-action-id").filter(value -> !value.isBlank());
            Optional<String> auditNote = optionValue(args, "--audit-note").filter(value -> !value.isBlank());
            Optional<Path> inventory = optionValue(args, INVENTORY_FLAG).map(Path::of);
            if (inventory.isPresent()) {
                EvidenceInventoryService.InventoryFormat inventoryFormat = optionValue(args, "--inventory-format")
                        .map(EvidenceInventoryService.InventoryFormat::parse)
                        .orElse(EvidenceInventoryService.InventoryFormat.MARKDOWN);
                Optional<Path> inventoryOutput = optionValue(args, "--inventory-output").map(Path::of);
                return new CliOptions(Optional.empty(), RemediationReportFormat.MARKDOWN, Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        List.of(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        inventory, inventoryFormat, inventoryOutput,
                        hasFlag(args, "--verify-inventory"),
                        hasFlag(args, "--include-hashes"),
                        hasFlag(args, "--fail-on-invalid"));
            }
            if (verifyAuditLog.isPresent()) {
                return new CliOptions(Optional.empty(), RemediationReportFormat.MARKDOWN, Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        List.of(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(),
                        verifyAuditLog, Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), null, Optional.empty(), false, false, false);
            }
            Optional<Path> verifyManifest = optionValue(args, VERIFY_MANIFEST_FLAG).map(Path::of);
            if (verifyManifest.isPresent()) {
                return new CliOptions(Optional.empty(), RemediationReportFormat.MARKDOWN, Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), List.of(), verifyManifest,
                        Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), List.of(), List.of(), Optional.empty(), Optional.empty(),
                        auditLog, Optional.empty(), auditActor, auditActionId, auditNote,
                        Optional.empty(), null, Optional.empty(), false, false, false);
            }
            Optional<Path> verifyBundle = optionValue(args, VERIFY_BUNDLE_FLAG).map(Path::of);
            if (verifyBundle.isPresent()) {
                return new CliOptions(Optional.empty(), RemediationReportFormat.MARKDOWN, Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(),
                        Optional.empty(), verifyBundle,
                        Optional.empty(), Optional.empty(), List.of(), List.of(), Optional.empty(), Optional.empty(),
                        auditLog, Optional.empty(), auditActor, auditActionId, auditNote,
                        Optional.empty(), null, Optional.empty(), false, false, false);
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
                    redactionLabel, redactionSummary, auditLog, Optional.empty(),
                    auditActor, auditActionId, auditNote,
                    Optional.empty(), null, Optional.empty(), false, false, false);
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

        private static boolean hasFlag(String[] args, String option) {
            return Arrays.stream(args)
                    .filter(Objects::nonNull)
                    .anyMatch(arg -> arg.equals(option));
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
