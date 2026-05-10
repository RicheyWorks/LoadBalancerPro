package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class EvidenceCatalogDiffService {
    private static final String DIFF_VERSION = "1";
    private static final List<String> ADVISORY_NOTES = List.of(
            "Local inventory diff only; no identity proof or cryptographic signature is provided.",
            "This diff is not a legal chain-of-custody system.",
            "The diff can only compare what each saved inventory catalog recorded.");
    private static final ObjectMapper DIFF_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    EvidenceCatalogDiff diff(DiffRequest request) throws IOException {
        Objects.requireNonNull(request, "diff request cannot be null");
        EvidenceInventoryService.EvidenceCatalog before = readCatalog(request.beforeCatalogPath());
        EvidenceInventoryService.EvidenceCatalog after = readCatalog(request.afterCatalogPath());

        Map<String, EvidenceInventoryService.EvidenceItem> beforeItems = index(before, "before");
        Map<String, EvidenceInventoryService.EvidenceItem> afterItems = index(after, "after");
        Set<String> paths = new LinkedHashSet<>();
        paths.addAll(beforeItems.keySet());
        paths.addAll(afterItems.keySet());

        List<EvidenceCatalogChange> includedChanges = new ArrayList<>();
        int addedCount = 0;
        int removedCount = 0;
        int changedCount = 0;
        int statusChangedCount = 0;
        int auditAnchorChangedCount = 0;
        int unchangedCount = 0;
        int warningCount = 0;
        int failureCount = 0;

        for (String path : paths.stream()
                .sorted(Comparator.comparing(value -> value.toLowerCase(Locale.ROOT)))
                .toList()) {
            EvidenceInventoryService.EvidenceItem beforeItem = beforeItems.get(path);
            EvidenceInventoryService.EvidenceItem afterItem = afterItems.get(path);
            EvidenceCatalogChange change = change(path, beforeItem, afterItem);
            if (change.added()) {
                addedCount++;
            }
            if (change.removed()) {
                removedCount++;
            }
            if (change.checksumChanged() || change.typeChanged()) {
                changedCount++;
            }
            if (change.statusChanged()) {
                statusChangedCount++;
            }
            if (change.auditAnchorChanged()) {
                auditAnchorChangedCount++;
            }
            if (change.changeType() == ChangeType.UNCHANGED) {
                unchangedCount++;
            }
            if (!change.warnings().isEmpty()) {
                warningCount++;
            }
            if (change.drifted()) {
                failureCount++;
            }
            if (request.includeUnchanged() || change.changeType() != ChangeType.UNCHANGED) {
                includedChanges.add(change);
            }
        }

        EvidenceCatalogDiffSummary summary = new EvidenceCatalogDiffSummary(
                addedCount,
                removedCount,
                changedCount,
                statusChangedCount,
                auditAnchorChangedCount,
                unchangedCount,
                warningCount,
                failureCount);
        return new EvidenceCatalogDiff(
                DIFF_VERSION,
                catalogRef(request.beforeCatalogPath(), before),
                catalogRef(request.afterCatalogPath(), after),
                summary,
                List.copyOf(includedChanges),
                ADVISORY_NOTES);
    }

    String renderJson(EvidenceCatalogDiff diff) throws IOException {
        return DIFF_MAPPER.writeValueAsString(diff) + System.lineSeparator();
    }

    String renderMarkdown(EvidenceCatalogDiff diff) {
        StringBuilder builder = new StringBuilder();
        builder.append("# LoadBalancerPro Evidence Catalog Diff")
                .append(System.lineSeparator())
                .append(System.lineSeparator());
        builder.append("- Diff version: ").append(diff.diffVersion()).append(System.lineSeparator());
        builder.append("- Before catalog: ").append(diff.beforeCatalog().path()).append(System.lineSeparator());
        builder.append("- After catalog: ").append(diff.afterCatalog().path()).append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("## Summary").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- Added: ").append(diff.summary().addedCount()).append(System.lineSeparator());
        builder.append("- Removed: ").append(diff.summary().removedCount()).append(System.lineSeparator());
        builder.append("- Changed: ").append(diff.summary().changedCount()).append(System.lineSeparator());
        builder.append("- Status changed: ").append(diff.summary().statusChangedCount()).append(System.lineSeparator());
        builder.append("- Audit anchor changed: ").append(diff.summary().auditAnchorChangedCount())
                .append(System.lineSeparator());
        builder.append("- Unchanged: ").append(diff.summary().unchangedCount()).append(System.lineSeparator());
        builder.append("- Warnings: ").append(diff.summary().warningCount()).append(System.lineSeparator());
        builder.append("- Failures: ").append(diff.summary().failureCount()).append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("## Changes").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("| Path | Change | Type | Status | SHA-256 | Audit Anchor | Details |")
                .append(System.lineSeparator());
        builder.append("| --- | --- | --- | --- | --- | --- | --- |").append(System.lineSeparator());
        for (EvidenceCatalogChange change : diff.changes()) {
            builder.append("| ")
                    .append(escapeMarkdown(change.path()))
                    .append(" | ")
                    .append(change.changeType())
                    .append(" | ")
                    .append(escapeMarkdown(beforeAfter(change.beforeType(), change.afterType())))
                    .append(" | ")
                    .append(escapeMarkdown(beforeAfter(change.beforeVerificationStatus(),
                            change.afterVerificationStatus())))
                    .append(" | ")
                    .append(escapeMarkdown(beforeAfter(change.beforeSha256(), change.afterSha256())))
                    .append(" | ")
                    .append(escapeMarkdown(auditDetails(change)))
                    .append(" | ")
                    .append(escapeMarkdown(String.join("; ", detailMessages(change))))
                    .append(" |")
                    .append(System.lineSeparator());
        }

        builder.append(System.lineSeparator()).append("## Advisory Notes").append(System.lineSeparator())
                .append(System.lineSeparator());
        for (String note : diff.advisoryNotes()) {
            builder.append("- ").append(note).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private EvidenceInventoryService.EvidenceCatalog readCatalog(Path path) throws IOException {
        try {
            JsonNode root = DIFF_MAPPER.readTree(path.toFile());
            JsonNode summary = root.path("summary");
            List<EvidenceInventoryService.EvidenceItem> items = new ArrayList<>();
            JsonNode itemNodes = root.path("items");
            if (!itemNodes.isArray()) {
                throw new IllegalArgumentException("catalog items must be an array");
            }
            for (JsonNode item : itemNodes) {
                items.add(new EvidenceInventoryService.EvidenceItem(
                        requiredText(item, "path"),
                        requiredText(item, "type"),
                        optionalText(item, "sha256"),
                        requiredText(item, "verificationStatus"),
                        stringList(item.path("warnings")),
                        stringList(item.path("failures")),
                        optionalText(item, "reportId"),
                        optionalText(item, "latestAuditEntryHash"),
                        optionalInt(item, "entryCount")));
            }
            return new EvidenceInventoryService.EvidenceCatalog(
                    requiredText(root, "catalogVersion"),
                    requiredText(root, "rootDirectory"),
                    root.path("verificationEnabled").asBoolean(false),
                    root.path("includeHashes").asBoolean(false),
                    new EvidenceInventoryService.EvidenceSummary(
                            summary.path("bundleCount").asInt(0),
                            summary.path("manifestCount").asInt(0),
                            summary.path("auditLogCount").asInt(0),
                            summary.path("redactionSummaryCount").asInt(0),
                            summary.path("reportCount").asInt(0),
                            summary.path("validCount").asInt(0),
                            summary.path("warningCount").asInt(0),
                            summary.path("failureCount").asInt(0)),
                    items,
                    stringList(root.path("advisoryNotes")));
        } catch (IOException | IllegalArgumentException e) {
            throw new IOException("failed to read evidence catalog " + path + ": " + safeMessage(e), e);
        }
    }

    private static String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null) {
            throw new IllegalArgumentException("catalog " + field + " is required");
        }
        return value;
    }

    private static String optionalText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    private static Integer optionalInt(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.canConvertToInt() ? value.asInt() : null;
    }

    private static List<String> stringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            if (value.isTextual() && !value.asText().isBlank()) {
                values.add(value.asText());
            }
        }
        return List.copyOf(values);
    }

    private static Map<String, EvidenceInventoryService.EvidenceItem> index(
            EvidenceInventoryService.EvidenceCatalog catalog,
            String label) {
        if (catalog == null) {
            throw new IllegalArgumentException(label + " catalog is required");
        }
        Map<String, EvidenceInventoryService.EvidenceItem> items = new LinkedHashMap<>();
        for (EvidenceInventoryService.EvidenceItem item : catalog.items()) {
            if (item.path() == null || item.path().isBlank()) {
                throw new IllegalArgumentException(label + " catalog contains an item without a path");
            }
            EvidenceInventoryService.EvidenceItem previous = items.put(item.path(), item);
            if (previous != null) {
                throw new IllegalArgumentException(label + " catalog contains duplicate path: " + item.path());
            }
        }
        return items;
    }

    private static EvidenceCatalogChange change(
            String path,
            EvidenceInventoryService.EvidenceItem before,
            EvidenceInventoryService.EvidenceItem after) {
        List<String> warnings = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        if (before == null) {
            failures.add("evidence was added after the baseline inventory");
            if ("REDACTION_SUMMARY".equals(after.type())) {
                warnings.add("redaction summary presence changed");
            }
            return new EvidenceCatalogChange(
                    path,
                    ChangeType.ADDED,
                    null,
                    after.type(),
                    null,
                    after.sha256(),
                    null,
                    after.verificationStatus(),
                    null,
                    after.latestAuditEntryHash(),
                    null,
                    after.entryCount(),
                    false,
                    false,
                    false,
                    false,
                    true,
                    false,
                    List.copyOf(warnings),
                    List.copyOf(failures));
        }
        if (after == null) {
            failures.add("evidence was removed after the baseline inventory");
            if ("REDACTION_SUMMARY".equals(before.type())) {
                warnings.add("redaction summary presence changed");
            }
            return new EvidenceCatalogChange(
                    path,
                    ChangeType.REMOVED,
                    before.type(),
                    null,
                    before.sha256(),
                    null,
                    before.verificationStatus(),
                    null,
                    before.latestAuditEntryHash(),
                    null,
                    before.entryCount(),
                    null,
                    false,
                    false,
                    false,
                    false,
                    false,
                    true,
                    List.copyOf(warnings),
                    List.copyOf(failures));
        }

        boolean typeChanged = different(before.type(), after.type());
        boolean checksumChanged = different(before.sha256(), after.sha256());
        boolean statusChanged = different(before.verificationStatus(), after.verificationStatus());
        boolean auditAnchorChanged = different(before.latestAuditEntryHash(), after.latestAuditEntryHash())
                || different(before.entryCount(), after.entryCount());
        if (typeChanged) {
            failures.add("evidence type changed");
        }
        if (checksumChanged) {
            failures.add("SHA-256 checksum changed");
        }
        if (statusChanged) {
            failures.add("verification status changed");
        }
        if (auditAnchorChanged) {
            failures.add("audit anchor hash or entry count changed");
        }

        ChangeType changeType;
        if (checksumChanged || typeChanged) {
            changeType = ChangeType.CHANGED;
        } else if (statusChanged) {
            changeType = ChangeType.STATUS_CHANGED;
        } else if (auditAnchorChanged) {
            changeType = ChangeType.AUDIT_ANCHOR_CHANGED;
        } else {
            changeType = ChangeType.UNCHANGED;
        }
        return new EvidenceCatalogChange(
                path,
                changeType,
                before.type(),
                after.type(),
                before.sha256(),
                after.sha256(),
                before.verificationStatus(),
                after.verificationStatus(),
                before.latestAuditEntryHash(),
                after.latestAuditEntryHash(),
                before.entryCount(),
                after.entryCount(),
                typeChanged,
                checksumChanged,
                statusChanged,
                auditAnchorChanged,
                false,
                false,
                List.copyOf(warnings),
                List.copyOf(failures));
    }

    private static EvidenceCatalogRef catalogRef(Path path, EvidenceInventoryService.EvidenceCatalog catalog) {
        return new EvidenceCatalogRef(
                displayPath(path),
                catalog.catalogVersion(),
                catalog.rootDirectory(),
                catalog.items().size());
    }

    private static List<String> detailMessages(EvidenceCatalogChange change) {
        List<String> details = new ArrayList<>();
        details.addAll(change.warnings());
        details.addAll(change.failures());
        return details;
    }

    private static String auditDetails(EvidenceCatalogChange change) {
        String before = auditAnchor(change.beforeLatestAuditEntryHash(), change.beforeAuditEntryCount());
        String after = auditAnchor(change.afterLatestAuditEntryHash(), change.afterAuditEntryCount());
        return beforeAfter(before, after);
    }

    private static String auditAnchor(String latestHash, Integer entryCount) {
        if (latestHash == null && entryCount == null) {
            return null;
        }
        return "entries=" + (entryCount == null ? "" : entryCount)
                + ", latest=" + (latestHash == null ? "" : latestHash);
    }

    private static String beforeAfter(Object before, Object after) {
        String left = before == null ? "" : before.toString();
        String right = after == null ? "" : after.toString();
        if (left.equals(right)) {
            return left;
        }
        return left + " -> " + right;
    }

    private static boolean different(Object before, Object after) {
        return !Objects.equals(blankToNull(before), blankToNull(after));
    }

    private static Object blankToNull(Object value) {
        if (value instanceof String text && text.isBlank()) {
            return null;
        }
        return value;
    }

    private static String displayPath(Path path) {
        Path fileName = path.toAbsolutePath().normalize().getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }

    private static String escapeMarkdown(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace(System.lineSeparator(), " ");
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    enum DiffFormat {
        MARKDOWN,
        JSON;

        static DiffFormat parse(String value) {
            String normalized = Objects.requireNonNull(value, "diff format is required")
                    .trim()
                    .toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "markdown", "md" -> MARKDOWN;
                case "json" -> JSON;
                default -> throw new IllegalArgumentException("diff format must be markdown or json");
            };
        }
    }

    enum ChangeType {
        ADDED,
        REMOVED,
        CHANGED,
        STATUS_CHANGED,
        AUDIT_ANCHOR_CHANGED,
        UNCHANGED
    }

    record DiffRequest(
            Path beforeCatalogPath,
            Path afterCatalogPath,
            boolean includeUnchanged) {

        DiffRequest {
            Objects.requireNonNull(beforeCatalogPath, "before catalog path cannot be null");
            Objects.requireNonNull(afterCatalogPath, "after catalog path cannot be null");
        }
    }

    record EvidenceCatalogDiff(
            String diffVersion,
            EvidenceCatalogRef beforeCatalog,
            EvidenceCatalogRef afterCatalog,
            EvidenceCatalogDiffSummary summary,
            List<EvidenceCatalogChange> changes,
            List<String> advisoryNotes) {

        EvidenceCatalogDiff {
            changes = changes == null ? List.of() : List.copyOf(changes);
            advisoryNotes = advisoryNotes == null ? List.of() : List.copyOf(advisoryNotes);
        }

        boolean hasDrift() {
            return summary.addedCount() > 0
                    || summary.removedCount() > 0
                    || summary.changedCount() > 0
                    || summary.statusChangedCount() > 0
                    || summary.auditAnchorChangedCount() > 0;
        }
    }

    record EvidenceCatalogRef(
            String path,
            String catalogVersion,
            String rootDirectory,
            int itemCount) {
    }

    record EvidenceCatalogDiffSummary(
            int addedCount,
            int removedCount,
            int changedCount,
            int statusChangedCount,
            int auditAnchorChangedCount,
            int unchangedCount,
            int warningCount,
            int failureCount) {
    }

    record EvidenceCatalogChange(
            String path,
            ChangeType changeType,
            String beforeType,
            String afterType,
            String beforeSha256,
            String afterSha256,
            String beforeVerificationStatus,
            String afterVerificationStatus,
            String beforeLatestAuditEntryHash,
            String afterLatestAuditEntryHash,
            Integer beforeAuditEntryCount,
            Integer afterAuditEntryCount,
            boolean typeChanged,
            boolean checksumChanged,
            boolean statusChanged,
            boolean auditAnchorChanged,
            boolean added,
            boolean removed,
            List<String> warnings,
            List<String> failures) {

        EvidenceCatalogChange {
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
            failures = failures == null ? List.of() : List.copyOf(failures);
        }

        boolean drifted() {
            return changeType != ChangeType.UNCHANGED;
        }
    }
}
