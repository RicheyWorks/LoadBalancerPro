package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

final class OfflineCliAuditLogService {
    static final String SCHEMA_VERSION = "1";
    static final String ZERO_PREVIOUS_HASH = "0".repeat(64);
    static final String SUCCESS = "SUCCESS";

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "schemaVersion",
            "sequence",
            "action",
            "actionId",
            "actor",
            "note",
            "inputPath",
            "outputPath",
            "manifestPath",
            "bundlePath",
            "result",
            "redactionApplied",
            "fileHashes",
            "previousEntryHash",
            "entryHash",
            "advisoryOnly",
            "cloudMutation");

    private static final ObjectMapper AUDIT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    AuditEntry append(AuditAppendRequest request) throws IOException {
        Objects.requireNonNull(request, "audit append request cannot be null");
        Path auditLogPath = normalize(request.auditLogPath());
        Path parent = auditLogPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        AuditVerificationResult current = Files.exists(auditLogPath)
                ? verify(auditLogPath)
                : new AuditVerificationResult(true, 0, ZERO_PREVIOUS_HASH, List.of(), List.of());
        if (!current.verified()) {
            throw new IOException("audit log chain is not valid; refusing to append: "
                    + String.join("; ", current.errors()));
        }

        AuditEntry unsigned = new AuditEntry(
                SCHEMA_VERSION,
                current.entryCount() + 1,
                requireText(request.action(), "audit action is required"),
                blankToNull(request.actionId()),
                blankToNull(request.actor()),
                blankToNull(request.note()),
                displayPath(auditLogPath, request.inputPath()),
                displayPath(auditLogPath, request.outputPath()),
                displayPath(auditLogPath, request.manifestPath()),
                displayPath(auditLogPath, request.bundlePath()),
                blankToNull(request.result()) == null ? SUCCESS : request.result(),
                request.redactionApplied(),
                fileHashes(auditLogPath, request.fileSources()),
                current.latestEntryHash(),
                null,
                true,
                false);
        String entryHash = hash(unsigned);
        AuditEntry signed = unsigned.withEntryHash(entryHash);
        Files.writeString(auditLogPath, toJsonLine(signed) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE);
        return signed;
    }

    AuditVerificationResult verify(Path auditLogPath) throws IOException {
        Path normalized = normalize(auditLogPath);
        if (!Files.isRegularFile(normalized)) {
            throw new IOException("audit log does not exist: " + normalized);
        }
        List<String> lines = Files.readAllLines(normalized, StandardCharsets.UTF_8);
        List<String> errors = new ArrayList<>();
        List<AuditEntry> entries = new ArrayList<>();
        String expectedPreviousHash = ZERO_PREVIOUS_HASH;
        int expectedSequence = 1;

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            int lineNumber = index + 1;
            if (line == null || line.isBlank()) {
                errors.add("line " + lineNumber + " is blank");
                continue;
            }

            AuditEntry entry;
            try {
                entry = readEntry(line);
            } catch (IllegalArgumentException e) {
                errors.add("line " + lineNumber + " is malformed: " + e.getMessage());
                continue;
            }

            entries.add(entry);
            if (entry.sequence() != expectedSequence) {
                errors.add("line " + lineNumber + " sequence expected " + expectedSequence
                        + " but was " + entry.sequence());
            }
            if (!expectedPreviousHash.equals(entry.previousEntryHash())) {
                errors.add("line " + lineNumber + " previousEntryHash expected " + expectedPreviousHash
                        + " but was " + entry.previousEntryHash());
            }
            if (!isSha256(entry.entryHash())) {
                errors.add("line " + lineNumber + " entryHash is not a SHA-256 digest");
            } else {
                String recomputed = hash(entry.withEntryHash(null));
                if (!recomputed.equals(entry.entryHash())) {
                    errors.add("line " + lineNumber + " entryHash mismatch expected " + entry.entryHash()
                            + " but recomputed " + recomputed);
                }
            }
            expectedPreviousHash = entry.entryHash();
            expectedSequence++;
        }

        String latestHash = entries.isEmpty() ? ZERO_PREVIOUS_HASH : entries.get(entries.size() - 1).entryHash();
        return new AuditVerificationResult(errors.isEmpty(), entries.size(), latestHash,
                List.copyOf(errors), List.copyOf(entries));
    }

    private static List<AuditFileHash> fileHashes(Path auditLogPath, List<AuditFileSource> sources)
            throws IOException {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        List<AuditFileHash> hashes = new ArrayList<>();
        for (AuditFileSource source : sources) {
            if (source == null || source.path() == null) {
                continue;
            }
            Path normalized = normalize(source.path());
            if (!Files.isRegularFile(normalized)) {
                throw new IOException("audit file is missing: " + normalized);
            }
            hashes.add(new AuditFileHash(
                    requireText(source.role(), "audit file role is required"),
                    displayPath(auditLogPath, normalized),
                    ReportChecksumManifestService.sha256(normalized)));
        }
        return List.copyOf(hashes);
    }

    private static AuditEntry readEntry(String line) {
        try {
            JsonNode root = AUDIT_MAPPER.readTree(line);
            if (!root.isObject()) {
                throw new IllegalArgumentException("audit entry must be a JSON object");
            }
            Iterator<String> fields = root.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (!ALLOWED_FIELDS.contains(field)) {
                    throw new IllegalArgumentException("unsupported audit entry field: " + field);
                }
            }
            List<AuditFileHash> fileHashes = new ArrayList<>();
            JsonNode fileHashNode = root.path("fileHashes");
            if (fileHashNode.isArray()) {
                for (JsonNode file : fileHashNode) {
                    fileHashes.add(new AuditFileHash(
                            requiredText(file, "role"),
                            requiredText(file, "path"),
                            requiredSha256(file, "sha256")));
                }
            } else if (!fileHashNode.isMissingNode() && !fileHashNode.isNull()) {
                throw new IllegalArgumentException("fileHashes must be an array");
            }
            AuditEntry entry = new AuditEntry(
                    requiredText(root, "schemaVersion"),
                    requiredInt(root, "sequence"),
                    requiredText(root, "action"),
                    optionalText(root, "actionId"),
                    optionalText(root, "actor"),
                    optionalText(root, "note"),
                    optionalText(root, "inputPath"),
                    optionalText(root, "outputPath"),
                    optionalText(root, "manifestPath"),
                    optionalText(root, "bundlePath"),
                    requiredText(root, "result"),
                    root.path("redactionApplied").asBoolean(false),
                    List.copyOf(fileHashes),
                    requiredSha256(root, "previousEntryHash"),
                    requiredSha256(root, "entryHash"),
                    root.path("advisoryOnly").asBoolean(false),
                    root.path("cloudMutation").asBoolean(true));
            if (!SCHEMA_VERSION.equals(entry.schemaVersion())) {
                throw new IllegalArgumentException("unsupported audit schema version: " + entry.schemaVersion());
            }
            return entry;
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private static String hash(AuditEntry entry) {
        try {
            return ReportChecksumManifestService.sha256(canonicalBytes(entry, false));
        } catch (IOException e) {
            throw new IllegalStateException("failed to serialize audit entry for hashing", e);
        }
    }

    private static String toJsonLine(AuditEntry entry) throws IOException {
        return new String(canonicalBytes(entry, true), StandardCharsets.UTF_8);
    }

    private static byte[] canonicalBytes(AuditEntry entry, boolean includeEntryHash) throws IOException {
        return AUDIT_MAPPER.writeValueAsBytes(canonicalMap(entry, includeEntryHash));
    }

    private static LinkedHashMap<String, Object> canonicalMap(AuditEntry entry, boolean includeEntryHash) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        put(map, "schemaVersion", entry.schemaVersion());
        put(map, "sequence", entry.sequence());
        put(map, "action", entry.action());
        put(map, "actionId", entry.actionId());
        put(map, "actor", entry.actor());
        put(map, "note", entry.note());
        put(map, "inputPath", entry.inputPath());
        put(map, "outputPath", entry.outputPath());
        put(map, "manifestPath", entry.manifestPath());
        put(map, "bundlePath", entry.bundlePath());
        put(map, "result", entry.result());
        put(map, "redactionApplied", entry.redactionApplied());
        put(map, "fileHashes", fileHashMaps(entry.fileHashes()));
        put(map, "previousEntryHash", entry.previousEntryHash());
        if (includeEntryHash) {
            put(map, "entryHash", entry.entryHash());
        }
        put(map, "advisoryOnly", entry.advisoryOnly());
        put(map, "cloudMutation", entry.cloudMutation());
        return map;
    }

    private static List<LinkedHashMap<String, Object>> fileHashMaps(List<AuditFileHash> hashes) {
        if (hashes == null || hashes.isEmpty()) {
            return List.of();
        }
        List<LinkedHashMap<String, Object>> values = new ArrayList<>();
        for (AuditFileHash hash : hashes) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            put(map, "role", hash.role());
            put(map, "path", hash.path());
            put(map, "sha256", hash.sha256());
            values.add(map);
        }
        return values;
    }

    private static void put(LinkedHashMap<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private static String displayPath(Path auditLogPath, Path filePath) {
        if (filePath == null) {
            return null;
        }
        Path normalizedFile = normalize(filePath);
        Path auditParent = normalize(auditLogPath).getParent();
        if (auditParent != null && normalizedFile.startsWith(auditParent)) {
            return normalizeSeparators(auditParent.relativize(normalizedFile).toString());
        }
        return normalizeSeparators(normalizedFile.getFileName().toString());
    }

    private static String normalizeSeparators(String path) {
        return path.replace('\\', '/');
    }

    private static String requireText(String value, String message) {
        String sanitized = blankToNull(value);
        if (sanitized == null) {
            throw new IllegalArgumentException(message);
        }
        return sanitized;
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.asText();
    }

    private static String optionalText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    private static String requiredSha256(JsonNode node, String field) {
        String value = requiredText(node, field).toLowerCase(Locale.ROOT);
        if (!isSha256(value)) {
            throw new IllegalArgumentException(field + " must be a SHA-256 digest");
        }
        return value;
    }

    private static int requiredInt(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.canConvertToInt()) {
            throw new IllegalArgumentException(field + " must be an integer");
        }
        return value.asInt();
    }

    private static boolean isSha256(String value) {
        return value != null && value.matches("(?i)[0-9a-f]{64}");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Path normalize(Path path) {
        return Objects.requireNonNull(path, "path cannot be null").toAbsolutePath().normalize();
    }

    record AuditAppendRequest(
            Path auditLogPath,
            String action,
            String actionId,
            String actor,
            String note,
            Path inputPath,
            Path outputPath,
            Path manifestPath,
            Path bundlePath,
            String result,
            boolean redactionApplied,
            List<AuditFileSource> fileSources) {

        AuditAppendRequest {
            fileSources = fileSources == null ? List.of() : List.copyOf(fileSources);
        }
    }

    record AuditFileSource(String role, Path path) {
    }

    record AuditEntry(
            String schemaVersion,
            int sequence,
            String action,
            String actionId,
            String actor,
            String note,
            String inputPath,
            String outputPath,
            String manifestPath,
            String bundlePath,
            String result,
            boolean redactionApplied,
            List<AuditFileHash> fileHashes,
            String previousEntryHash,
            String entryHash,
            boolean advisoryOnly,
            boolean cloudMutation) {

        AuditEntry {
            fileHashes = fileHashes == null ? List.of() : List.copyOf(fileHashes);
        }

        AuditEntry withEntryHash(String hash) {
            return new AuditEntry(
                    schemaVersion,
                    sequence,
                    action,
                    actionId,
                    actor,
                    note,
                    inputPath,
                    outputPath,
                    manifestPath,
                    bundlePath,
                    result,
                    redactionApplied,
                    fileHashes,
                    previousEntryHash,
                    hash,
                    advisoryOnly,
                    cloudMutation);
        }
    }

    record AuditFileHash(String role, String path, String sha256) {
    }

    record AuditVerificationResult(
            boolean verified,
            int entryCount,
            String latestEntryHash,
            List<String> errors,
            List<AuditEntry> entries) {

        AuditVerificationResult {
            errors = errors == null ? List.of() : List.copyOf(errors);
            entries = entries == null ? List.of() : List.copyOf(entries);
        }
    }
}
