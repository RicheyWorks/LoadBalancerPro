package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class EvidenceInventoryService {
    private static final String CATALOG_VERSION = "1";
    private static final List<String> ADVISORY_NOTES = List.of(
            "Local checksum inventory only; no identity proof or cryptographic signature is provided.",
            "This catalog is not a legal chain-of-custody system.",
            "Verification reuses offline bundle, manifest, and audit-log checksum checks.");
    private static final ObjectMapper INVENTORY_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    EvidenceCatalog inventory(InventoryRequest request) throws IOException {
        Objects.requireNonNull(request, "inventory request cannot be null");
        Path root = request.rootDirectory().toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IOException("inventory path is not a directory: " + root);
        }

        List<EvidenceItem> items = new ArrayList<>();
        try (var walk = Files.walk(root)) {
            List<Path> paths = walk
                    .filter(path -> !path.equals(root))
                    .sorted(Comparator.comparing(path -> relativePath(root, path).toLowerCase(Locale.ROOT)))
                    .toList();
            for (Path path : paths) {
                if (Files.isDirectory(path)) {
                    continue;
                }
                items.add(inventoryItem(root, path, request));
            }
        }

        EvidenceSummary summary = summary(items);
        return new EvidenceCatalog(
                CATALOG_VERSION,
                displayRoot(root),
                request.verifyInventory(),
                request.includeHashes(),
                summary,
                List.copyOf(items),
                ADVISORY_NOTES);
    }

    String renderJson(EvidenceCatalog catalog) throws IOException {
        return INVENTORY_MAPPER.writeValueAsString(catalog) + System.lineSeparator();
    }

    String renderMarkdown(EvidenceCatalog catalog) {
        StringBuilder builder = new StringBuilder();
        builder.append("# LoadBalancerPro Evidence Inventory").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- Catalog version: ").append(catalog.catalogVersion()).append(System.lineSeparator());
        builder.append("- Root directory: ").append(catalog.rootDirectory()).append(System.lineSeparator());
        builder.append("- Verification: ").append(catalog.verificationEnabled() ? "enabled" : "disabled")
                .append(System.lineSeparator());
        builder.append("- Include hashes: ").append(catalog.includeHashes()).append(System.lineSeparator())
                .append(System.lineSeparator());
        builder.append("## Summary").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- Bundles: ").append(catalog.summary().bundleCount()).append(System.lineSeparator());
        builder.append("- Manifests: ").append(catalog.summary().manifestCount()).append(System.lineSeparator());
        builder.append("- Audit logs: ").append(catalog.summary().auditLogCount()).append(System.lineSeparator());
        builder.append("- Redaction summaries: ").append(catalog.summary().redactionSummaryCount())
                .append(System.lineSeparator());
        builder.append("- Reports: ").append(catalog.summary().reportCount()).append(System.lineSeparator());
        builder.append("- Valid: ").append(catalog.summary().validCount()).append(System.lineSeparator());
        builder.append("- Warnings: ").append(catalog.summary().warningCount()).append(System.lineSeparator());
        builder.append("- Failures: ").append(catalog.summary().failureCount()).append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("## Items").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("| Path | Type | Status | SHA-256 | Details |").append(System.lineSeparator());
        builder.append("| --- | --- | --- | --- | --- |").append(System.lineSeparator());
        for (EvidenceItem item : catalog.items()) {
            builder.append("| ")
                    .append(escapeMarkdown(item.path()))
                    .append(" | ")
                    .append(item.type())
                    .append(" | ")
                    .append(item.verificationStatus())
                    .append(" | ")
                    .append(item.sha256() == null ? "" : item.sha256())
                    .append(" | ")
                    .append(escapeMarkdown(details(item)))
                    .append(" |")
                    .append(System.lineSeparator());
        }

        builder.append(System.lineSeparator()).append("## Advisory Notes").append(System.lineSeparator())
                .append(System.lineSeparator());
        for (String note : catalog.advisoryNotes()) {
            builder.append("- ").append(note).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private EvidenceItem inventoryItem(Path root, Path path, InventoryRequest request) {
        ItemBuilder builder = new ItemBuilder(relativePath(root, path), classify(path));
        if (Files.isSymbolicLink(path)) {
            builder.status(VerificationStatus.WARNING);
            builder.warning("symbolic links are not followed or verified");
            return builder.build();
        }
        if (request.includeHashes()) {
            try {
                builder.sha256(ReportChecksumManifestService.sha256(path));
            } catch (IOException e) {
                builder.warning("failed to hash file: " + safeMessage(e));
            }
        }
        if (!request.verifyInventory()) {
            builder.status(VerificationStatus.NOT_VERIFIED);
            addLightweightMetadata(path, builder);
            return builder.build();
        }

        switch (builder.type) {
            case BUNDLE -> verifyBundle(path, builder);
            case MANIFEST -> verifyManifest(path, builder);
            case AUDIT_LOG -> verifyAuditLog(path, builder);
            case REDACTION_SUMMARY -> {
                builder.status(VerificationStatus.WARNING);
                builder.warning("redaction summary detected; original redacted values are not inspected");
            }
            case REPORT, INPUT, VERIFICATION_SUMMARY, UNKNOWN -> builder.status(VerificationStatus.NOT_VERIFIED);
            default -> builder.status(VerificationStatus.NOT_VERIFIED);
        }
        addLightweightMetadata(path, builder);
        return builder.build();
    }

    private static void verifyBundle(Path path, ItemBuilder builder) {
        try {
            IncidentBundleService.BundleVerificationResult result = new IncidentBundleService().verify(path);
            builder.status(result.verified() ? VerificationStatus.VALID : VerificationStatus.FAILED);
            result.errors().forEach(builder::failure);
            for (ReportChecksumManifestService.ManifestVerificationEntry entry : result.entries()) {
                if (!entry.verified()) {
                    builder.failure("bundle " + entry.status() + " [" + entry.role() + "] " + entry.path());
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            builder.status(VerificationStatus.FAILED);
            builder.failure("bundle verification failed safely: " + safeMessage(e));
        }
    }

    private static void verifyManifest(Path path, ItemBuilder builder) {
        try {
            ReportChecksumManifestService.ManifestVerificationResult result =
                    new ReportChecksumManifestService().verify(path);
            builder.status(result.verified() ? VerificationStatus.VALID : VerificationStatus.FAILED);
            builder.reportId(result.manifest().reportId());
            for (ReportChecksumManifestService.ManifestVerificationEntry entry : result.entries()) {
                if (!entry.verified()) {
                    builder.failure("manifest " + entry.status() + " [" + entry.role() + "] " + entry.path());
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            builder.status(VerificationStatus.FAILED);
            builder.failure("manifest verification failed safely: " + safeMessage(e));
        }
    }

    private static void verifyAuditLog(Path path, ItemBuilder builder) {
        try {
            OfflineCliAuditLogService.AuditVerificationResult result =
                    new OfflineCliAuditLogService().verify(path);
            builder.status(result.verified() ? VerificationStatus.VALID : VerificationStatus.FAILED);
            builder.entryCount(result.entryCount());
            builder.latestAuditEntryHash(result.latestEntryHash());
            result.errors().forEach(builder::failure);
        } catch (IOException | IllegalArgumentException e) {
            builder.status(VerificationStatus.FAILED);
            builder.failure("audit log verification failed safely: " + safeMessage(e));
        }
    }

    private static void addLightweightMetadata(Path path, ItemBuilder builder) {
        if (builder.reportId != null) {
            return;
        }
        try {
            if (builder.type == EvidenceType.MANIFEST) {
                ReportChecksumManifestService.ReportChecksumManifest manifest =
                        new ReportChecksumManifestService().readManifest(Files.readAllBytes(path));
                builder.reportId(manifest.reportId());
            } else if (builder.type == EvidenceType.REPORT && path.getFileName().toString().endsWith(".json")) {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                var report = INVENTORY_MAPPER.readTree(content);
                if (report.has("reportId") && report.path("reportId").isTextual()) {
                    builder.reportId(report.path("reportId").asText());
                }
            }
        } catch (IOException | IllegalArgumentException ignored) {
            // Metadata extraction is best-effort; verification handles malformed evidence when requested.
        }
    }

    private static EvidenceType classify(Path path) {
        String fileName = path.getFileName().toString();
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".zip")) {
            return EvidenceType.BUNDLE;
        }
        if (lower.equals("manifest.json") || lower.endsWith(".manifest.json")) {
            return EvidenceType.MANIFEST;
        }
        if (lower.endsWith(".jsonl")) {
            return EvidenceType.AUDIT_LOG;
        }
        if (lower.equals("redaction-summary.json") || lower.endsWith(".redaction-summary.json")) {
            return EvidenceType.REDACTION_SUMMARY;
        }
        if (lower.equals("report.md") || lower.equals("report.json")
                || (lower.contains("report") && (lower.endsWith(".md") || lower.endsWith(".json")))) {
            return EvidenceType.REPORT;
        }
        if (lower.equals("input.json") || lower.endsWith(".input.redacted.json")
                || (lower.endsWith(".json") && (lower.contains("evaluation") || lower.contains("replay")))) {
            return EvidenceType.INPUT;
        }
        if (lower.equals("verification-summary.json")) {
            return EvidenceType.VERIFICATION_SUMMARY;
        }
        return EvidenceType.UNKNOWN;
    }

    private static EvidenceSummary summary(List<EvidenceItem> items) {
        int bundleCount = 0;
        int manifestCount = 0;
        int auditLogCount = 0;
        int redactionSummaryCount = 0;
        int reportCount = 0;
        int validCount = 0;
        int warningCount = 0;
        int failureCount = 0;

        for (EvidenceItem item : items) {
            EvidenceType type = EvidenceType.valueOf(item.type());
            switch (type) {
                case BUNDLE -> bundleCount++;
                case MANIFEST -> manifestCount++;
                case AUDIT_LOG -> auditLogCount++;
                case REDACTION_SUMMARY -> redactionSummaryCount++;
                case REPORT -> reportCount++;
                default -> {
                }
            }
            VerificationStatus status = VerificationStatus.valueOf(item.verificationStatus());
            if (status == VerificationStatus.VALID) {
                validCount++;
            }
            if (status == VerificationStatus.WARNING || !item.warnings().isEmpty()) {
                warningCount++;
            }
            if (status == VerificationStatus.FAILED || !item.failures().isEmpty()) {
                failureCount++;
            }
        }

        return new EvidenceSummary(
                bundleCount,
                manifestCount,
                auditLogCount,
                redactionSummaryCount,
                reportCount,
                validCount,
                warningCount,
                failureCount);
    }

    private static String details(EvidenceItem item) {
        List<String> details = new ArrayList<>();
        if (item.reportId() != null) {
            details.add("reportId=" + item.reportId());
        }
        if (item.entryCount() != null) {
            details.add("entries=" + item.entryCount());
        }
        if (item.latestAuditEntryHash() != null) {
            details.add("latestAuditEntryHash=" + item.latestAuditEntryHash());
        }
        details.addAll(item.warnings());
        details.addAll(item.failures());
        return String.join("; ", details);
    }

    private static String displayRoot(Path root) {
        Path fileName = root.getFileName();
        return fileName == null ? "." : fileName.toString();
    }

    private static String relativePath(Path root, Path path) {
        return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static String escapeMarkdown(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace(System.lineSeparator(), " ");
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    enum InventoryFormat {
        MARKDOWN,
        JSON;

        static InventoryFormat parse(String value) {
            String normalized = Objects.requireNonNull(value, "inventory format is required")
                    .trim()
                    .toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "markdown", "md" -> MARKDOWN;
                case "json" -> JSON;
                default -> throw new IllegalArgumentException("inventory format must be markdown or json");
            };
        }
    }

    private enum EvidenceType {
        BUNDLE,
        MANIFEST,
        AUDIT_LOG,
        REDACTION_SUMMARY,
        REPORT,
        INPUT,
        VERIFICATION_SUMMARY,
        UNKNOWN
    }

    private enum VerificationStatus {
        VALID,
        WARNING,
        FAILED,
        NOT_VERIFIED
    }

    record InventoryRequest(
            Path rootDirectory,
            boolean verifyInventory,
            boolean includeHashes) {

        InventoryRequest {
            Objects.requireNonNull(rootDirectory, "inventory root directory cannot be null");
        }
    }

    record EvidenceCatalog(
            String catalogVersion,
            String rootDirectory,
            boolean verificationEnabled,
            boolean includeHashes,
            EvidenceSummary summary,
            List<EvidenceItem> items,
            List<String> advisoryNotes) {

        EvidenceCatalog {
            items = items == null ? List.of() : List.copyOf(items);
            advisoryNotes = advisoryNotes == null ? List.of() : List.copyOf(advisoryNotes);
        }
    }

    record EvidenceSummary(
            int bundleCount,
            int manifestCount,
            int auditLogCount,
            int redactionSummaryCount,
            int reportCount,
            int validCount,
            int warningCount,
            int failureCount) {
    }

    record EvidenceItem(
            String path,
            String type,
            String sha256,
            String verificationStatus,
            List<String> warnings,
            List<String> failures,
            String reportId,
            String latestAuditEntryHash,
            Integer entryCount) {

        EvidenceItem {
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
            failures = failures == null ? List.of() : List.copyOf(failures);
        }
    }

    private static final class ItemBuilder {
        private final String path;
        private final EvidenceType type;
        private String sha256;
        private VerificationStatus status = VerificationStatus.NOT_VERIFIED;
        private final List<String> warnings = new ArrayList<>();
        private final List<String> failures = new ArrayList<>();
        private String reportId;
        private String latestAuditEntryHash;
        private Integer entryCount;

        private ItemBuilder(String path, EvidenceType type) {
            this.path = path;
            this.type = type;
        }

        private void sha256(String value) {
            this.sha256 = value;
        }

        private void status(VerificationStatus value) {
            this.status = value;
        }

        private void warning(String value) {
            this.warnings.add(value);
        }

        private void failure(String value) {
            this.failures.add(value);
        }

        private void reportId(String value) {
            this.reportId = value == null || value.isBlank() ? null : value;
        }

        private void latestAuditEntryHash(String value) {
            this.latestAuditEntryHash = value;
        }

        private void entryCount(int value) {
            this.entryCount = value;
        }

        private EvidenceItem build() {
            return new EvidenceItem(
                    path,
                    type.name(),
                    sha256,
                    status.name(),
                    warnings,
                    failures,
                    reportId,
                    latestAuditEntryHash,
                    entryCount);
        }
    }
}
