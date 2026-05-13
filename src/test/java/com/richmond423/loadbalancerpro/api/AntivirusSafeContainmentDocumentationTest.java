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
    private static final Path PRIVATE_NETWORK_PROXY_PLAN = Path.of("docs/PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md");
    private static final Path README = Path.of("README.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path CONTAINER_DOC = Path.of("docs/CONTAINER_DEPLOYMENT.md");
    private static final Path POSTMAN_DOC = Path.of("docs/POSTMAN_COLLECTION.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path LOCAL_REAL_BACKEND_PROXY_TEST =
            Path.of("src/test/java/com/richmond423/loadbalancerpro/api/LocalOnlyRealBackendProxyValidationTest.java");
    private static final Path LOCAL_PROXY_EVIDENCE_EXPORT_TEST =
            Path.of("src/test/java/com/richmond423/loadbalancerpro/api/LocalProxyEvidenceExportTest.java");
    private static final Path PROXY_EVIDENCE_MARKDOWN =
            Path.of("target", "proxy-evidence", "local-proxy-evidence.md");
    private static final Path PROXY_EVIDENCE_JSON =
            Path.of("target", "proxy-evidence", "local-proxy-evidence.json");
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
        assertTrue(doc.contains("LocalOnlyRealBackendProxyValidationTest"));
        assertTrue(doc.contains("LocalProxyEvidenceExportTest"));
        assertTrue(doc.contains("target/proxy-evidence/local-proxy-evidence.md"));
        assertTrue(doc.contains("target/proxy-evidence/local-proxy-evidence.json"));
        assertTrue(doc.contains("PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md"));
        assertTrue(normalized.contains("explicit operator-provided backend urls only"));
        assertTrue(normalized.contains("no discovery or scanning"));
        assertTrue(doc.contains("Java-assigned ephemeral loopback ports"));
        assertTrue(normalized.contains("does not scan ports"));
        assertTrue(normalized.contains("does not"));
        assertTrue(normalized.contains("call external networks"));
    }

    @Test
    void readmeLinksBothContainmentDocs() throws Exception {
        String readme = read(README);

        assertTrue(readme.contains("[`ANTIVIRUS_SAFE_DEVELOPMENT.md`](docs/ANTIVIRUS_SAFE_DEVELOPMENT.md)"));
        assertTrue(readme.contains("[`LIVE_PROXY_CONTAINMENT.md`](docs/LIVE_PROXY_CONTAINMENT.md)"));
        assertTrue(readme.contains("REVIEWER_TRUST_MAP.md#local-proxy-evidence-export"));
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

        String combined = read(ANTIVIRUS_DOC) + "\n" + read(LIVE_PROXY_DOC) + "\n" + read(TRUST_MAP)
                + "\n" + read(PRIVATE_NETWORK_PROXY_PLAN);
        assertTrue(combined.contains("release-downloads/"));
        assertTrue(combined.toLowerCase(Locale.ROOT).contains("explicit"));
        assertFalse(combined.toLowerCase(Locale.ROOT).contains("copy generated files into `release-downloads/`"));
    }

    @Test
    void scratchSmokeTempFilesAreAbsent() {
        assertFalse(Files.exists(SCRATCH_SMOKE_FILE), SCRATCH_SMOKE_FILE + " must not be committed or present");
    }

    @Test
    void localOnlyRealBackendProxyValidationPathIsDocumentedAndSourceVisible() throws Exception {
        String combinedDocs = read(LIVE_PROXY_DOC) + "\n" + read(RUNBOOK) + "\n" + read(TRUST_MAP);
        String testSource = read(LOCAL_REAL_BACKEND_PROXY_TEST);

        assertTrue(combinedDocs.contains("LocalOnlyRealBackendProxyValidationTest"));
        assertTrue(combinedDocs.contains("source-visible"));
        assertTrue(combinedDocs.contains("JDK loopback"));
        assertTrue(combinedDocs.contains("Java-assigned ephemeral"));
        assertTrue(combinedDocs.contains("configured-unavailable backend fails closed"));
        assertTrue(testSource.contains("com.sun.net.httpserver.HttpServer"));
        assertTrue(testSource.contains("InetAddress.getLoopbackAddress()"));
        assertTrue(testSource.contains("new InetSocketAddress(InetAddress.getLoopbackAddress(), 0)"));
        assertTrue(testSource.contains("loadbalancerpro.proxy.enabled"));
        assertTrue(testSource.contains("loadbalancerpro.proxy.routes.unavailable"));
        assertTrue(testSource.contains("X-LoadBalancerPro-Upstream"));
        assertTrue(testSource.contains("X-LoadBalancerPro-Strategy"));
    }

    @Test
    void localProxyEvidenceExportPathIsIgnoredAndSourceVisible() throws Exception {
        String combinedDocs = read(README) + "\n" + read(LIVE_PROXY_DOC) + "\n" + read(RUNBOOK)
                + "\n" + read(TRUST_MAP);
        String normalizedDocs = combinedDocs.toLowerCase(Locale.ROOT);
        String testSource = read(LOCAL_PROXY_EVIDENCE_EXPORT_TEST);

        assertTrue(PROXY_EVIDENCE_MARKDOWN.startsWith(Path.of("target")),
                PROXY_EVIDENCE_MARKDOWN + " should remain under ignored Maven build output");
        assertTrue(PROXY_EVIDENCE_JSON.startsWith(Path.of("target")),
                PROXY_EVIDENCE_JSON + " should remain under ignored Maven build output");
        assertTrue(read(GITIGNORE).contains("target/"),
                ".gitignore should keep generated proxy evidence untracked");
        assertTrue(combinedDocs.contains("LocalProxyEvidenceExportTest"));
        assertTrue(combinedDocs.contains("target/proxy-evidence/local-proxy-evidence.md"));
        assertTrue(combinedDocs.contains("target/proxy-evidence/local-proxy-evidence.json"));
        assertTrue(combinedDocs.contains("redacted"));
        assertTrue(combinedDocs.contains("prod API-key boundary"));
        assertTrue(combinedDocs.contains("mvn -Dtest=LocalProxyEvidenceExportTest test"));
        assertTrue(normalizedDocs.contains("ignored `target/` output"));
        assertTrue(normalizedDocs.contains("do not write api keys or secrets"));
        assertTrue(normalizedDocs.contains("loopback/local-only jdk `httpserver`"));
        assertTrue(normalizedDocs.contains("prod api-key `401`/`200` boundary"));
        assertTrue(normalizedDocs.contains("do not add external network behavior"));
        assertTrue(testSource.contains("Path.of(\"target\", \"proxy-evidence\")"));
        assertTrue(testSource.contains("Files.createDirectories(EVIDENCE_DIR)"));
        assertTrue(testSource.contains("Files.writeString(MARKDOWN_EVIDENCE"));
        assertTrue(testSource.contains("Files.writeString(JSON_EVIDENCE"));
        assertTrue(testSource.contains("\"apiKeyRedacted\": \"<REDACTED>\""));
        assertTrue(testSource.contains("assertFalse(markdown.contains(API_KEY))"));
        assertTrue(testSource.contains("assertFalse(json.contains(API_KEY))"));
    }

    @Test
    void localProxyEvidenceExportUsesLoopbackAndPreservesApiKeyBoundary() throws Exception {
        String testSource = read(LOCAL_PROXY_EVIDENCE_EXPORT_TEST);

        assertTrue(testSource.contains("com.sun.net.httpserver.HttpServer"));
        assertTrue(testSource.contains("InetAddress.getLoopbackAddress()"));
        assertTrue(testSource.contains("new InetSocketAddress(InetAddress.getLoopbackAddress(), 0)"));
        assertTrue(testSource.contains("return \"http://127.0.0.1:\""));
        assertTrue(testSource.contains("spring.profiles.active=prod"));
        assertTrue(testSource.contains("loadbalancerpro.api.key=TEST_PROXY_EVIDENCE_KEY"));
        assertTrue(testSource.contains("X-API-Key"));
        assertTrue(testSource.contains("status().isUnauthorized()"));
        assertTrue(testSource.contains("X-LoadBalancerPro-Upstream"));
        assertTrue(testSource.contains("X-LoadBalancerPro-Strategy"));
        assertTrue(testSource.contains("X-Local-Proxy-Evidence"));
        assertTrue(testSource.contains("portPolicy\": \"java-assigned-ephemeral\""));
    }

    @Test
    void localOnlyRealBackendProxyValidationPathAvoidsUnsafeArtifactsAndInstructions() throws Exception {
        String testSource = read(LOCAL_REAL_BACKEND_PROXY_TEST).toLowerCase(Locale.ROOT);

        assertFalse(Pattern.compile("(?i)\\.(exe|dll|msi|bin)\\b").matcher(testSource).find(),
                LOCAL_REAL_BACKEND_PROXY_TEST + " should not reference native binary file extensions");
        for (String forbidden : List.of(
                "native-image",
                "launch4j",
                "jpackage",
                "installer",
                "self-extracting",
                "release-downloads",
                "scheduled task",
                "service install",
                "localstorage",
                "sessionstorage",
                "downloaded helper",
                "port scan")) {
            assertFalse(testSource.contains(forbidden),
                    LOCAL_REAL_BACKEND_PROXY_TEST + " should not contain unsafe proxy validation pattern: "
                            + forbidden);
        }
    }

    @Test
    void localProxyEvidenceExportAvoidsUnsafeArtifactsAndExternalTargets() throws Exception {
        String testSource = read(LOCAL_PROXY_EVIDENCE_EXPORT_TEST);
        String normalized = testSource.toLowerCase(Locale.ROOT);

        assertFalse(Pattern.compile("(?i)\\.(exe|dll|msi|bin)\\b").matcher(testSource).find(),
                LOCAL_PROXY_EVIDENCE_EXPORT_TEST + " should not reference native binary file extensions");
        assertFalse(Pattern.compile("(?i)https?://(?!127\\.0\\.0\\.1)").matcher(testSource).find(),
                LOCAL_PROXY_EVIDENCE_EXPORT_TEST + " should not introduce external HTTP targets");
        for (String forbidden : List.of(
                "native-image",
                "launch4j",
                "jpackage",
                "installer",
                "self-extracting",
                "release-downloads",
                "localstorage",
                "sessionstorage",
                "port scan")) {
            assertFalse(normalized.contains(forbidden),
                    LOCAL_PROXY_EVIDENCE_EXPORT_TEST + " should not contain unsafe evidence export pattern: "
                            + forbidden);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
