package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.richmond423.loadbalancerpro.api.RemediationReportFormat;
import com.richmond423.loadbalancerpro.api.RemediationReportPayload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

final class IncidentBundleService {
    static final String INPUT_ENTRY = "input.json";
    static final String MARKDOWN_REPORT_ENTRY = "report.md";
    static final String JSON_REPORT_ENTRY = "report.json";
    static final String MANIFEST_ENTRY = "manifest.json";
    static final String VERIFICATION_SUMMARY_ENTRY = "verification-summary.json";
    static final String README_ENTRY = "README.md";

    private static final String BUNDLE_FORMAT_VERSION = "1";
    private static final FileTime FIXED_ZIP_TIME = FileTime.from(Instant.parse("1980-01-01T00:00:00Z"));
    private static final ObjectMapper BUNDLE_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    BundleExportResult export(BundleExportRequest request) throws IOException {
        Objects.requireNonNull(request, "bundle export request cannot be null");
        Path bundlePath = normalize(request.bundlePath());
        RemediationReportPayload payload = Objects.requireNonNull(request.payload(),
                "remediation report payload cannot be null");
        byte[] inputJson = Files.readAllBytes(normalize(request.inputPath()));
        String reportEntry = reportEntry(request.format());

        Path staging = Files.createTempDirectory("loadbalancerpro-incident-bundle-");
        try {
            Path stagedInput = staging.resolve(INPUT_ENTRY);
            Path stagedReport = staging.resolve(reportEntry);
            Path stagedReadme = staging.resolve(README_ENTRY);
            Path stagedSummary = staging.resolve(VERIFICATION_SUMMARY_ENTRY);
            Path stagedManifest = staging.resolve(MANIFEST_ENTRY);

            Files.write(stagedInput, inputJson);
            Files.writeString(stagedReport, request.report(), StandardCharsets.UTF_8);
            Files.writeString(stagedReadme, readme(payload, request.format(), reportEntry), StandardCharsets.UTF_8);
            Files.writeString(stagedSummary, verificationSummary(payload, request.format(), reportEntry),
                    StandardCharsets.UTF_8);

            ReportChecksumManifestService manifestService = new ReportChecksumManifestService();
            ReportChecksumManifestService.ReportChecksumManifest manifest = manifestService.create(
                    new ReportChecksumManifestService.ManifestCreateRequest(
                            stagedManifest,
                            stagedInput,
                            stagedReport,
                            List.of(stagedReadme, stagedSummary),
                            payload,
                            request.generatedBy(),
                            request.createdAt(),
                            request.appVersion()));
            manifestService.write(stagedManifest, manifest);
            ReportChecksumManifestService.ManifestVerificationResult verification =
                    manifestService.verify(stagedManifest);
            if (!verification.verified()) {
                throw new IOException("bundle manifest failed verification before ZIP export");
            }

            LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
            entries.put(INPUT_ENTRY, Files.readAllBytes(stagedInput));
            entries.put(reportEntry, Files.readAllBytes(stagedReport));
            entries.put(MANIFEST_ENTRY, Files.readAllBytes(stagedManifest));
            entries.put(VERIFICATION_SUMMARY_ENTRY, Files.readAllBytes(stagedSummary));
            entries.put(README_ENTRY, Files.readAllBytes(stagedReadme));

            Path parent = bundlePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            writeZip(bundlePath, entries);

            BundleVerificationResult bundleVerification = verify(bundlePath);
            if (!bundleVerification.verified()) {
                throw new IOException("exported bundle failed verification: "
                        + String.join("; ", bundleVerification.errors()));
            }
            return new BundleExportResult(bundlePath, manifest, bundleVerification);
        } finally {
            deleteRecursively(staging);
        }
    }

    BundleVerificationResult verify(Path bundlePath) throws IOException {
        Objects.requireNonNull(bundlePath, "bundle path cannot be null");
        LinkedHashMap<String, byte[]> entries = readZip(normalize(bundlePath));
        List<String> errors = new ArrayList<>();
        byte[] manifestBytes = entries.get(MANIFEST_ENTRY);
        if (manifestBytes == null) {
            errors.add("bundle is missing " + MANIFEST_ENTRY);
            return new BundleVerificationResult(false, List.of(), List.copyOf(errors));
        }

        ReportChecksumManifestService manifestService = new ReportChecksumManifestService();
        ReportChecksumManifestService.ReportChecksumManifest manifest = manifestService.readManifest(manifestBytes);
        Set<String> manifestPaths = new LinkedHashSet<>();
        for (ReportChecksumManifestService.ReportChecksumFile file : manifest.files()) {
            manifestPaths.add(safeEntryName(file.path()));
        }
        Set<String> allowedEntries = new HashSet<>(manifestPaths);
        allowedEntries.add(MANIFEST_ENTRY);
        for (String entryName : entries.keySet()) {
            if (!allowedEntries.contains(entryName)) {
                errors.add("bundle contains an entry not covered by the manifest: " + entryName);
            }
        }

        ReportChecksumManifestService.ManifestVerificationResult verification = manifestService.verify(
                manifest,
                manifestPath -> entries.get(safeEntryName(manifestPath)));
        boolean verified = errors.isEmpty() && verification.verified();
        return new BundleVerificationResult(verified, verification.entries(), List.copyOf(errors));
    }

    private static void writeZip(Path bundlePath, LinkedHashMap<String, byte[]> entries) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(
                bundlePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setLastModifiedTime(FIXED_ZIP_TIME);
                zip.putNextEntry(zipEntry);
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
    }

    private static LinkedHashMap<String, byte[]> readZip(Path bundlePath) throws IOException {
        if (!Files.isRegularFile(bundlePath)) {
            throw new IOException("incident bundle does not exist: " + bundlePath);
        }
        LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(bundlePath))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String safeName = safeEntryName(entry.getName());
                if (entry.isDirectory()) {
                    throw new IOException("bundle directory entries are not supported: " + safeName);
                }
                if (entries.containsKey(safeName)) {
                    throw new IOException("bundle contains a duplicate entry: " + safeName);
                }
                ByteArrayOutputStream content = new ByteArrayOutputStream();
                zip.transferTo(content);
                entries.put(safeName, content.toByteArray());
                zip.closeEntry();
            }
        }
        if (entries.isEmpty()) {
            throw new IOException("incident bundle is empty: " + bundlePath);
        }
        return entries;
    }

    private static String safeEntryName(String rawName) throws IOException {
        if (rawName == null || rawName.isBlank()) {
            throw new IOException("bundle entry path is required");
        }
        String name = rawName.replace('\\', '/');
        if (!name.equals(rawName)) {
            throw new IOException("bundle entry path must use forward slashes only: " + rawName);
        }
        if (name.startsWith("/") || name.matches("^[A-Za-z]:.*")) {
            throw new IOException("bundle entry path must be relative: " + rawName);
        }
        List<String> segments = new ArrayList<>();
        for (String segment : name.split("/")) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                throw new IOException("unsafe bundle entry path: " + rawName);
            }
            segments.add(segment);
        }
        return String.join("/", segments);
    }

    private static String reportEntry(RemediationReportFormat format) {
        return format == RemediationReportFormat.JSON ? JSON_REPORT_ENTRY : MARKDOWN_REPORT_ENTRY;
    }

    private static String readme(
            RemediationReportPayload payload,
            RemediationReportFormat format,
            String reportEntry) {
        return "# LoadBalancerPro Incident Bundle\n\n"
                + "This offline bundle contains saved incident input, a deterministic remediation report, "
                + "a SHA-256 checksum manifest, and a verification summary.\n\n"
                + "## Contents\n\n"
                + "- `input.json`: saved evaluation, replay, or report request JSON.\n"
                + "- `" + reportEntry + "`: generated " + format.name().toLowerCase() + " remediation report.\n"
                + "- `manifest.json`: checksum manifest for bundle files.\n"
                + "- `verification-summary.json`: deterministic summary of bundle safety semantics.\n"
                + "- `README.md`: this file.\n\n"
                + "## Safety\n\n"
                + "- Advisory only: " + payload.advisoryOnly() + "\n"
                + "- Read only: " + payload.readOnly() + "\n"
                + "- Cloud mutation: " + payload.cloudMutation() + "\n"
                + "- Signing or key management: false\n\n"
                + "The manifest is checksum-based tamper evidence. It is not a cryptographic signature "
                + "and does not prove operator identity.\n";
    }

    private static String verificationSummary(
            RemediationReportPayload payload,
            RemediationReportFormat format,
            String reportEntry) throws IOException {
        BundleVerificationSummary summary = new BundleVerificationSummary(
                BUNDLE_FORMAT_VERSION,
                "PASS",
                payload.reportId(),
                payload.sourceType(),
                format.name(),
                reportEntry,
                MANIFEST_ENTRY,
                true,
                payload.readOnly(),
                payload.advisoryOnly(),
                payload.cloudMutation(),
                false,
                List.of(
                        "Exporter verifies manifest checksums before reporting success.",
                        "Offline verification must recompute SHA-256 for all manifest-listed files.",
                        "Checksum manifests detect missing or changed files but do not prove identity."));
        return BUNDLE_MAPPER.writeValueAsString(summary) + System.lineSeparator();
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path candidate : paths) {
                Files.deleteIfExists(candidate);
            }
        }
    }

    private static Path normalize(Path path) {
        return Objects.requireNonNull(path, "path cannot be null").toAbsolutePath().normalize();
    }

    record BundleExportRequest(
            Path bundlePath,
            Path inputPath,
            RemediationReportFormat format,
            String report,
            RemediationReportPayload payload,
            String generatedBy,
            String createdAt,
            String appVersion) {
    }

    record BundleExportResult(
            Path bundlePath,
            ReportChecksumManifestService.ReportChecksumManifest manifest,
            BundleVerificationResult verification) {
    }

    record BundleVerificationResult(
            boolean verified,
            List<ReportChecksumManifestService.ManifestVerificationEntry> entries,
            List<String> errors) {

        BundleVerificationResult {
            entries = entries == null ? List.of() : List.copyOf(entries);
            errors = errors == null ? List.of() : List.copyOf(errors);
        }
    }

    private record BundleVerificationSummary(
            String bundleFormatVersion,
            String verificationStatus,
            String reportId,
            String sourceType,
            String reportFormat,
            String reportFile,
            String manifestFile,
            boolean contentStable,
            boolean readOnly,
            boolean advisoryOnly,
            boolean cloudMutation,
            boolean signingKeyManagement,
            List<String> notes) {

        private BundleVerificationSummary {
            notes = notes == null ? List.of() : List.copyOf(notes);
        }
    }
}
