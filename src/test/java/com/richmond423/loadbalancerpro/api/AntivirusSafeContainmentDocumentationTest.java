package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class AntivirusSafeContainmentDocumentationTest {
    private static final Path ANTIVIRUS_DOC = Path.of("docs/ANTIVIRUS_SAFE_DEVELOPMENT.md");
    private static final Path LIVE_PROXY_DOC = Path.of("docs/LIVE_PROXY_CONTAINMENT.md");
    private static final Path README = Path.of("README.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path CONTAINER_DOC = Path.of("docs/CONTAINER_DEPLOYMENT.md");
    private static final Path POSTMAN_DOC = Path.of("docs/POSTMAN_COLLECTION.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path GITIGNORE = Path.of(".gitignore");
    private static final Path DOCKERIGNORE = Path.of(".dockerignore");
    private static final Path SCRATCH_SMOKE_FILE = Path.of("scripts", "smoke", "tmp-test" + "-file.txt");
    private static final Pattern UNKNOWN_BINARY_INSTRUCTION = Pattern.compile(
            "(?i)\\b(native-image|launch4j|jpackage|self-extracting|unknown binaries?|"
                    + "generated executables?|native wrappers?)\\b");
    private static final Pattern DOWNLOADED_NATIVE_BINARY =
            Pattern.compile("(?i)\\b(download|fetch|curl|invoke-webrequest)\\b[^\\n]*(\\.(exe|msi|dll)\\b)");
    private static final Pattern EXE_REFERENCE = Pattern.compile("(?i)\\.exe\\b");

    @Test
    void containmentDocsExistAndAreLinkedFromReviewerEntryPoints() throws Exception {
        assertTrue(Files.exists(ANTIVIRUS_DOC), "antivirus-safe development doc should exist");
        assertTrue(Files.exists(LIVE_PROXY_DOC), "live proxy containment doc should exist");

        for (Path doc : List.of(README, RUNBOOK, CONTAINER_DOC, POSTMAN_DOC, TRUST_MAP)) {
            String content = read(doc);
            assertTrue(content.contains("ANTIVIRUS_SAFE_DEVELOPMENT.md"),
                    doc + " should link to antivirus-safe development guidance");
            assertTrue(content.contains("LIVE_PROXY_CONTAINMENT.md"),
                    doc + " should link to live proxy containment guidance");
        }
    }

    @Test
    void antivirusSafeDocDefinesSafeAndAvoidedArtifactTypes() throws Exception {
        String doc = read(ANTIVIRUS_DOC);

        for (String expected : List.of(
                "Java source",
                "Maven tests",
                "Spring Boot JARs",
                "PowerShell scripts",
                "Postman JSON collections and environments",
                "Markdown docs",
                "Dockerfile and Docker documentation",
                "GitHub Actions workflow definitions",
                "`.exe` files",
                "`native-image` outputs",
                "`launch4j` wrappers",
                "`jpackage` installers",
                "Installers",
                "Packers",
                "Self-extracting archives",
                "Vendored third-party binaries")) {
            assertTrue(doc.contains(expected), "antivirus doc should mention " + expected);
        }

        assertTrue(doc.contains("source-visible artifact types"));
        assertTrue(doc.contains("Quarantine unknown detections"));
        assertTrue(doc.contains("Do not whitelist unknown files"));
        assertTrue(doc.contains("Do not restore unknown quarantined files"));
        assertTrue(doc.contains("second-opinion scan"));
        assertTrue(doc.contains("Do not copy borrowed-drive artifacts"));
        assertTrue(doc.contains("Do not commit generated binaries"));
        assertTrue(doc.contains("Do not commit secrets"));
        assertTrue(doc.contains("Do not mutate `release-downloads/`"));
    }

    @Test
    void liveProxyContainmentDocDefinesRequiredContainmentBoundaries() throws Exception {
        String doc = read(LIVE_PROXY_DOC);
        String normalized = doc.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("live/proxy mode is opt-in"));
        assertTrue(normalized.contains("localhost or private-network backends"));
        assertTrue(normalized.contains("no port scanning"));
        assertTrue(normalized.contains("no persistence mechanisms"));
        assertTrue(normalized.contains("no scheduled tasks"));
        assertTrue(normalized.contains("no service installation"));
        assertTrue(normalized.contains("no credential storage in the repository"));
        assertTrue(normalized.contains("no browser `localstorage`"));
        assertTrue(normalized.contains("no suspicious downloads"));
        assertTrue(normalized.contains("no hidden background agents"));
        assertTrue(normalized.contains("deterministic, documented, and low-risk"));
        assertTrue(normalized.contains("production/cloud behavior must remain explicit and security-gated"));
    }

    @Test
    void readmeLinksBothContainmentDocs() throws Exception {
        String readme = read(README);

        assertTrue(readme.contains("[`ANTIVIRUS_SAFE_DEVELOPMENT.md`](docs/ANTIVIRUS_SAFE_DEVELOPMENT.md)"));
        assertTrue(readme.contains("[`LIVE_PROXY_CONTAINMENT.md`](docs/LIVE_PROXY_CONTAINMENT.md)"));
        assertTrue(readme.contains("source-visible local-only smoke harness that is dry-run safe by default"));
    }

    @Test
    void postmanSmokeHarnessIsDocumentedAsSourceVisibleAndDryRunSafeByDefault() throws Exception {
        String combined = read(README) + "\n" + read(RUNBOOK) + "\n" + read(POSTMAN_DOC) + "\n" + read(TRUST_MAP);
        String normalized = combined.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("source-visible"));
        assertTrue(normalized.contains("dry-run safe"));
        assertTrue(combined.contains("postman-enterprise-lab-safe-smoke.ps1"));
        assertTrue(combined.contains("-Package"));
    }

    @Test
    void postmanAndOperatorDocsAvoidUnknownBinaryAndNativeWrapperInstructions() throws Exception {
        for (Path doc : List.of(POSTMAN_DOC, RUNBOOK, CONTAINER_DOC,
                Path.of("docs/OPERATOR_RUN_PROFILES.md"),
                Path.of("docs/OPERATOR_INSTALL_RUN_MATRIX.md"),
                Path.of("docs/OPERATOR_PACKAGING.md"))) {
            String content = read(doc);
            String withoutKnownCurl = content.replace("curl.exe", "curl");

            assertFalse(UNKNOWN_BINARY_INSTRUCTION.matcher(withoutKnownCurl).find(),
                    doc + " must not instruct users to run native wrappers or unknown/generated executables");
            assertFalse(DOWNLOADED_NATIVE_BINARY.matcher(withoutKnownCurl).find(),
                    doc + " must not instruct users to download native binaries");
            assertFalse(EXE_REFERENCE.matcher(withoutKnownCurl).find(),
                    doc + " should not reference .exe files except the documented Windows curl client");
        }
    }

    @Test
    void releaseDownloadsRemainExcludedAndManualOnly() throws Exception {
        assertTrue(read(GITIGNORE).contains("release-downloads/"),
                ".gitignore should keep local release-downloads evidence untracked");
        assertTrue(read(DOCKERIGNORE).contains("release-downloads/"),
                ".dockerignore should keep release-downloads out of Docker build context");

        String combined = read(ANTIVIRUS_DOC) + "\n" + read(LIVE_PROXY_DOC) + "\n" + read(TRUST_MAP);
        assertTrue(combined.contains("release-downloads/"));
        assertTrue(combined.toLowerCase(Locale.ROOT).contains("explicit"));
        assertFalse(combined.toLowerCase(Locale.ROOT).contains("copy generated files into `release-downloads/`"));
    }

    @Test
    void scratchSmokeTempFilesAreAbsent() {
        assertFalse(Files.exists(SCRATCH_SMOKE_FILE), SCRATCH_SMOKE_FILE + " must not be committed or present");
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
