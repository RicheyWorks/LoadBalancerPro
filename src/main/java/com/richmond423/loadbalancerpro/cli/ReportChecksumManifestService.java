package com.richmond423.loadbalancerpro.cli;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.richmond423.loadbalancerpro.api.RemediationReportPayload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class ReportChecksumManifestService {
    static final String MANIFEST_VERSION = "1";
    static final String ALGORITHM = "SHA-256";
    static final String GENERATED_BY = "LoadBalancerPro offline remediation report CLI";

    private static final ObjectMapper MANIFEST_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    ReportChecksumManifest create(ManifestCreateRequest request) throws IOException {
        Objects.requireNonNull(request, "manifest request cannot be null");
        RemediationReportPayload payload = Objects.requireNonNull(
                request.payload(), "remediation report payload cannot be null");
        Path manifestPath = normalize(request.manifestPath());
        Path inputPath = normalize(request.inputPath());
        Path reportPath = normalize(request.reportPath());
        validateDistinctBundlePaths(inputPath, reportPath, manifestPath);

        List<ReportChecksumFile> files = new ArrayList<>();
        files.add(fileEntry(manifestPath, inputPath, "INPUT"));
        files.add(fileEntry(manifestPath, reportPath, "REPORT"));
        List<Path> extraFiles = request.extraFiles().stream()
                .filter(Objects::nonNull)
                .map(ReportChecksumManifestService::normalize)
                .sorted(Comparator.comparing(path -> path.toString().toLowerCase(Locale.ROOT)))
                .toList();
        for (Path path : extraFiles) {
            files.add(fileEntry(manifestPath, path, "EXTRA"));
        }

        return new ReportChecksumManifest(
                MANIFEST_VERSION,
                ALGORITHM,
                payload.reportId(),
                payload.sourceType(),
                request.appVersion(),
                List.copyOf(files),
                payload.advisoryOnly(),
                payload.cloudMutation(),
                defaultGeneratedBy(request.generatedBy()),
                blankToNull(request.createdAt()));
    }

    void write(Path manifestPath, ReportChecksumManifest manifest) throws IOException {
        Objects.requireNonNull(manifestPath, "manifest path cannot be null");
        Objects.requireNonNull(manifest, "manifest cannot be null");
        Files.writeString(manifestPath, MANIFEST_MAPPER.writeValueAsString(manifest) + System.lineSeparator(),
                StandardCharsets.UTF_8);
    }

    ManifestVerificationResult verify(Path manifestPath) throws IOException {
        Path normalizedManifest = normalize(manifestPath);
        ReportChecksumManifest manifest = readManifest(Files.readAllBytes(normalizedManifest));

        Path manifestDirectory = normalizedManifest.getParent();
        Path baseDirectory = manifestDirectory == null
                ? Path.of(".").toAbsolutePath().normalize()
                : manifestDirectory;

        return verify(manifest, path -> {
            Path resolved = resolveManifestFile(baseDirectory, path);
            return Files.isRegularFile(resolved) ? Files.readAllBytes(resolved) : null;
        });
    }

    ReportChecksumManifest readManifest(byte[] manifestBytes) throws IOException {
        ReportChecksumManifest manifest = MANIFEST_MAPPER.readValue(manifestBytes, ReportChecksumManifest.class);
        validateManifest(manifest);
        return manifest;
    }

    ManifestVerificationResult verify(ReportChecksumManifest manifest, ManifestContentResolver resolver)
            throws IOException {
        validateManifest(manifest);
        Objects.requireNonNull(resolver, "manifest content resolver cannot be null");
        List<ManifestVerificationEntry> entries = new ArrayList<>();
        for (ReportChecksumFile file : manifest.files()) {
            byte[] content = resolver.resolve(file.path());
            if (content == null) {
                entries.add(new ManifestVerificationEntry(file.path(), file.role(), file.sha256(), null, false,
                        "MISSING"));
                continue;
            }
            String actual = sha256(content);
            boolean match = actual.equalsIgnoreCase(file.sha256());
            entries.add(new ManifestVerificationEntry(file.path(), file.role(), file.sha256(), actual, match,
                    match ? "OK" : "MISMATCH"));
        }
        boolean verified = entries.stream().allMatch(ManifestVerificationEntry::verified);
        return new ManifestVerificationResult(verified, manifest, List.copyOf(entries));
    }

    String toJson(ReportChecksumManifest manifest) throws IOException {
        return MANIFEST_MAPPER.writeValueAsString(manifest) + System.lineSeparator();
    }

    static String sha256(Path path) throws IOException {
        MessageDigest digest = sha256Digest();
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static String sha256(byte[] content) {
        MessageDigest digest = sha256Digest();
        digest.update(content);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static ReportChecksumFile fileEntry(Path manifestPath, Path filePath, String role) throws IOException {
        if (!Files.isRegularFile(filePath)) {
            throw new IOException("manifest bundle file is missing: " + filePath);
        }
        return new ReportChecksumFile(displayPath(manifestPath, filePath), sha256(filePath), role);
    }

    private static void validateManifest(ReportChecksumManifest manifest) {
        if (manifest == null) {
            throw new IllegalArgumentException("manifest is required");
        }
        if (!MANIFEST_VERSION.equals(manifest.manifestVersion())) {
            throw new IllegalArgumentException("unsupported manifest version: " + manifest.manifestVersion());
        }
        if (!ALGORITHM.equalsIgnoreCase(manifest.algorithm())) {
            throw new IllegalArgumentException("unsupported manifest algorithm: " + manifest.algorithm());
        }
        if (manifest.files() == null || manifest.files().isEmpty()) {
            throw new IllegalArgumentException("manifest must contain at least one file");
        }
        for (ReportChecksumFile file : manifest.files()) {
            if (file.path() == null || file.path().isBlank()) {
                throw new IllegalArgumentException("manifest file path is required");
            }
            if (file.sha256() == null || !file.sha256().matches("(?i)[0-9a-f]{64}")) {
                throw new IllegalArgumentException("manifest file sha256 must be a 64-character hexadecimal digest");
            }
            if (file.role() == null || file.role().isBlank()) {
                throw new IllegalArgumentException("manifest file role is required");
            }
        }
    }

    private static Path resolveManifestFile(Path baseDirectory, String manifestPath) {
        Path path = Path.of(manifestPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return baseDirectory.resolve(path).normalize();
    }

    private static String displayPath(Path manifestPath, Path filePath) {
        Path manifestDirectory = manifestPath.getParent();
        if (manifestDirectory != null && filePath.startsWith(manifestDirectory)) {
            return normalizeSeparators(manifestDirectory.relativize(filePath).toString());
        }
        return normalizeSeparators(filePath.toString());
    }

    private static String normalizeSeparators(String path) {
        return path.replace('\\', '/');
    }

    private static Path normalize(Path path) {
        return Objects.requireNonNull(path, "path cannot be null").toAbsolutePath().normalize();
    }

    private static void validateDistinctBundlePaths(Path inputPath, Path reportPath, Path manifestPath) {
        if (inputPath.equals(reportPath)) {
            throw new IllegalArgumentException("input and report output must be different files");
        }
        if (manifestPath.equals(inputPath) || manifestPath.equals(reportPath)) {
            throw new IllegalArgumentException("manifest path must be different from input and report output files");
        }
    }

    private static String defaultGeneratedBy(String generatedBy) {
        String sanitized = blankToNull(generatedBy);
        return sanitized == null ? GENERATED_BY : sanitized;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest algorithm is unavailable", e);
        }
    }

    record ManifestCreateRequest(
            Path manifestPath,
            Path inputPath,
            Path reportPath,
            List<Path> extraFiles,
            RemediationReportPayload payload,
            String generatedBy,
            String createdAt,
            String appVersion) {

        ManifestCreateRequest {
            extraFiles = extraFiles == null ? List.of() : List.copyOf(extraFiles);
        }
    }

    public record ReportChecksumManifest(
            String manifestVersion,
            String algorithm,
            String reportId,
            String sourceType,
            String appVersion,
            List<ReportChecksumFile> files,
            boolean advisoryOnly,
            boolean cloudMutation,
            String generatedBy,
            String createdAt) {

        public ReportChecksumManifest {
            files = files == null ? List.of() : List.copyOf(files);
        }
    }

    public record ReportChecksumFile(
            String path,
            String sha256,
            String role) {
    }

    record ManifestVerificationResult(
            boolean verified,
            ReportChecksumManifest manifest,
            List<ManifestVerificationEntry> entries) {

        ManifestVerificationResult {
            entries = entries == null ? List.of() : List.copyOf(entries);
        }
    }

    record ManifestVerificationEntry(
            String path,
            String role,
            String expectedSha256,
            String actualSha256,
            boolean verified,
            String status) {
    }

    @FunctionalInterface
    interface ManifestContentResolver {
        byte[] resolve(String path) throws IOException;
    }
}
