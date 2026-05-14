package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class OperatorInstallRunMatrixTest {
    private static final Path MATRIX = Path.of("docs/OPERATOR_INSTALL_RUN_MATRIX.md");
    private static final Path RELEASE_INTENT = Path.of("docs/RELEASE_INTENT_CHECKLIST.md");
    private static final Path README = Path.of("README.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path DRY_RUN = Path.of("docs/RELEASE_CANDIDATE_DRY_RUN.md");
    private static final Path CI_ARTIFACT_GUIDE = Path.of("docs/CI_ARTIFACT_CONSUMER_GUIDE.md");
    private static final Path LOCAL_ARTIFACT_DOC = Path.of("docs/LOCAL_ARTIFACT_VERIFICATION.md");
    private static final Path DISTRIBUTION_SMOKE_DOC = Path.of("docs/OPERATOR_DISTRIBUTION_SMOKE_KIT.md");
    private static final Path OPERATOR_PACKAGING_DOC = Path.of("docs/OPERATOR_PACKAGING.md");
    private static final Path TESTING_COVERAGE_DOC = Path.of("docs/TESTING_COVERAGE.md");
    private static final Path API_SECURITY_DOC = Path.of("docs/API_SECURITY.md");
    private static final Pattern FORTY_HEX = Pattern.compile("(?i)\\b[a-f0-9]{40}\\b");
    private static final Pattern SHA256_HEX = Pattern.compile("(?i)\\b[a-f0-9]{64}\\b");

    @Test
    void installRunMatrixDocumentsOperatorCommandPaths() throws Exception {
        String matrix = read(MATRIX);

        assertTrue(matrix.contains("# Operator Install Run Matrix"));
        assertTrue(matrix.contains("Windows command"));
        assertTrue(matrix.contains("Unix command"));
        assertTrue(matrix.contains("java -jar target/LoadBalancerPro-2.5.0.jar"));
        assertTrue(matrix.contains("exec:java"));
        assertTrue(matrix.contains("spring-boot:run"));
        assertTrue(matrix.contains("ProxyDemoFixtureLauncher"));
        assertTrue(matrix.contains("scripts/proxy-demo.ps1"));
        assertTrue(matrix.contains("scripts/proxy-demo.sh"));
        assertTrue(matrix.contains("application-proxy-real-backend-example.properties"));
        assertTrue(matrix.contains("proxy-status.html"));
        assertTrue(matrix.contains("load-balancing-cockpit.html"));
        assertTrue(matrix.contains("/api/proxy/status"));
    }

    @Test
    void installRunMatrixMapsCiArtifactsAndLocalVerificationPaths() throws Exception {
        String matrix = read(MATRIX);

        assertTrue(matrix.contains("jacoco-coverage-report"));
        assertTrue(matrix.contains("packaged-artifact-smoke"));
        assertTrue(matrix.contains("loadbalancerpro-sbom"));
        assertTrue(matrix.contains("scripts/local-artifact-verify.ps1"));
        assertTrue(matrix.contains("scripts/local-artifact-verify.sh"));
        assertTrue(matrix.contains("scripts/operator-distribution-smoke.ps1"));
        assertTrue(matrix.contains("scripts/operator-distribution-smoke.sh"));
        assertTrue(matrix.contains("Workflow artifacts are not GitHub Release assets."));
        assertTrue(matrix.contains("Every matrix row is release-free."));
    }

    @Test
    void releaseIntentChecklistAddsExplicitHardStopsBeforeRealRelease() throws Exception {
        String checklist = read(RELEASE_INTENT);

        assertTrue(checklist.contains("# Release Intent Checklist"));
        assertTrue(checklist.contains("This checklist does not create a release."));
        assertTrue(checklist.contains("Latest `main` CI is successful."));
        assertTrue(checklist.contains("Latest `main` CodeQL is successful."));
        assertTrue(checklist.contains("CI reported zero skipped tests."));
        assertTrue(checklist.contains("jacoco-coverage-report"));
        assertTrue(checklist.contains("packaged-artifact-smoke"));
        assertTrue(checklist.contains("loadbalancerpro-sbom"));
        assertTrue(checklist.contains("Local artifact verification"));
        assertTrue(checklist.contains("Do not run `gh release` commands unless the user explicitly approves a real release in a separate prompt."));
        assertTrue(checklist.contains("Do not run `git tag` commands unless the user explicitly approves a real release in a separate prompt."));
        assertTrue(checklist.contains("Do not modify `release-downloads/` unless the user explicitly approves that evidence task."));
        assertTrue(checklist.contains("Do not change the default branch."));
        assertTrue(checklist.contains("Do not change repository rulesets."));
    }

    @Test
    void existingOperatorDocsLinkToMatrixAndReleaseIntentChecklist() throws Exception {
        assertLinks(README);
        assertLinks(RUNBOOK);
        assertLinks(DRY_RUN);
        assertLinks(CI_ARTIFACT_GUIDE);
        assertLinks(LOCAL_ARTIFACT_DOC);
        assertLinks(DISTRIBUTION_SMOKE_DOC);
        assertLinks(OPERATOR_PACKAGING_DOC);
        assertLinks(TESTING_COVERAGE_DOC);
        assertLinks(API_SECURITY_DOC);
    }

    @Test
    void newDocsAvoidFakeEvidenceAndUnsafeClaims() throws Exception {
        String combined = read(MATRIX) + "\n" + read(RELEASE_INTENT);
        String normalized = combined.toLowerCase(Locale.ROOT);

        assertFalse(FORTY_HEX.matcher(combined).find(), "docs must not include fake commit hashes");
        assertFalse(SHA256_HEX.matcher(combined).find(), "docs must not include fake checksums");
        assertFalse(normalized.contains("new cloudmanager"), "docs must not construct CloudManager");
        assertFalse(normalized.contains("cloudmanager("), "docs must not construct CloudManager");
        assertFalse(normalized.contains("production-grade"), "docs must not add production-grade claims");
        assertFalse(normalized.contains("certification proof"), "docs must not add certification claims");
        assertFalse(normalized.contains("identity proof"), "docs must not add identity claims");
        assertFalse(normalized.contains("release-downloads/ evidence"), "docs must not ask for release-downloads evidence");
        assertFalse(normalized.contains("sample hash"), "docs must not add fake sample hashes");
        assertFalse(normalized.contains("sample checksum"), "docs must not add fake sample checksums");
    }

    @Test
    void releaseCommandsAppearOnlyAsHardStopText() throws Exception {
        String checklist = read(RELEASE_INTENT);
        String matrix = read(MATRIX);

        assertFalse(matrix.toLowerCase(Locale.ROOT).contains("gh release"), "matrix must not include release commands");
        assertFalse(matrix.toLowerCase(Locale.ROOT).contains("git tag"), "matrix must not include tag commands");
        assertTrue(checklist.contains("Do not run `gh release` commands"));
        assertTrue(checklist.contains("Do not run `git tag` commands"));
    }

    private static void assertLinks(Path path) throws IOException {
        String content = read(path);
        assertTrue(content.contains("OPERATOR_INSTALL_RUN_MATRIX.md"), path + " should link to install/run matrix");
        assertTrue(content.contains("RELEASE_INTENT_CHECKLIST.md"), path + " should link to release intent checklist");
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
