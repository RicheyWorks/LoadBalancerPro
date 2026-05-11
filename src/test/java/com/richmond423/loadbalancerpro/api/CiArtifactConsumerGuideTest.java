package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class CiArtifactConsumerGuideTest {
    private static final Path GUIDE = Path.of("docs/CI_ARTIFACT_CONSUMER_GUIDE.md");
    private static final Path README = Path.of("README.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path LOCAL_ARTIFACT_DOC = Path.of("docs/LOCAL_ARTIFACT_VERIFICATION.md");
    private static final Path DISTRIBUTION_SMOKE_DOC = Path.of("docs/OPERATOR_DISTRIBUTION_SMOKE_KIT.md");
    private static final Path OPERATOR_PACKAGING_DOC = Path.of("docs/OPERATOR_PACKAGING.md");
    private static final Path TESTING_COVERAGE_DOC = Path.of("docs/TESTING_COVERAGE.md");
    private static final Path API_SECURITY_DOC = Path.of("docs/API_SECURITY.md");
    private static final Path CI_WORKFLOW = Path.of(".github/workflows/ci.yml");

    @Test
    void guideDocumentsCurrentWorkflowArtifactsAndSmokeFiles() throws Exception {
        String guide = read(GUIDE);

        assertTrue(guide.contains("# CI Artifact Consumer Guide"));
        assertTrue(guide.contains("jacoco-coverage-report"));
        assertTrue(guide.contains("packaged-artifact-smoke"));
        assertTrue(guide.contains("artifact-smoke-summary.txt"));
        assertTrue(guide.contains("artifact-sha256.txt"));
        assertTrue(guide.contains("jar-resource-list.txt"));
        assertTrue(guide.contains("loadbalancerpro-sbom"));
        assertTrue(guide.contains("bom.json"));
        assertTrue(guide.contains("bom.xml"));
        assertTrue(guide.contains("GitHub Actions workflow artifacts"));
        assertTrue(guide.contains("They are not GitHub Release assets."));
    }

    @Test
    void guideExplainsChecksumComparisonAndReleaseCandidateChecklist() throws Exception {
        String guide = read(GUIDE);

        assertTrue(guide.contains("Compare Local And CI SHA-256"));
        assertTrue(guide.contains("Get-FileHash -Algorithm SHA256"));
        assertTrue(guide.contains("sha256sum target/LoadBalancerPro-2.4.2.jar"));
        assertTrue(guide.contains("shasum -a 256 target/LoadBalancerPro-2.4.2.jar"));
        assertTrue(guide.contains("artifact-sha256.txt"));
        assertTrue(guide.contains("A local rebuild may differ"));
        assertTrue(guide.contains("Release Candidate Checklist"));
        assertTrue(guide.contains("CI reports zero skipped tests"));
        assertTrue(guide.contains("No tags, GitHub releases, release assets, `release-downloads/` changes"));
    }

    @Test
    void guideReleaseBoundaryAvoidsPublishingInstructionsAndGeneratedEvidenceClaims() throws Exception {
        String guide = read(GUIDE);
        String normalized = guide.toLowerCase(Locale.ROOT);

        assertTrue(guide.contains("no tags"));
        assertTrue(guide.contains("no GitHub releases"));
        assertTrue(guide.contains("no release assets"));
        assertTrue(guide.contains("no `release-downloads/` changes"));
        assertFalse(normalized.contains("gh release"), "guide must not add release publishing commands");
        assertFalse(normalized.contains("git tag"), "guide must not add tag creation commands");
        assertFalse(normalized.contains("softprops/action-gh-release"), "guide must not use release asset actions");
        assertFalse(normalized.contains("actions/create-release"), "guide must not use release creation actions");
        assertFalse(normalized.contains("upload-release-asset"), "guide must not use release asset actions");
        assertFalse(normalized.contains("new cloudmanager"), "guide must not construct CloudManager");
        assertFalse(normalized.contains("cloudmanager("), "guide must not construct CloudManager");
        assertFalse(normalized.contains("production-grade"), "guide must not add production-grade claims");
        assertFalse(normalized.contains("certification proof"), "guide must not add certification claims");
        assertFalse(normalized.contains("identity proof"), "guide must not add identity claims");
    }

    @Test
    void sbomArtifactClaimMatchesCurrentCiWorkflow() throws Exception {
        String guide = read(GUIDE);
        String workflow = read(CI_WORKFLOW);

        assertTrue(workflow.contains("name: loadbalancerpro-sbom"));
        assertTrue(workflow.contains("target/bom.json"));
        assertTrue(workflow.contains("target/bom.xml"));
        assertTrue(workflow.contains("org.cyclonedx:cyclonedx-maven-plugin"));
        assertTrue(guide.contains("loadbalancerpro-sbom"));
    }

    @Test
    void existingOperatorDocsLinkToCiArtifactConsumerGuide() throws Exception {
        assertTrue(read(README).contains("CI_ARTIFACT_CONSUMER_GUIDE.md"));
        assertTrue(read(RUNBOOK).contains("CI_ARTIFACT_CONSUMER_GUIDE.md"));
        assertTrue(read(LOCAL_ARTIFACT_DOC).contains("CI_ARTIFACT_CONSUMER_GUIDE.md"));
        assertTrue(read(DISTRIBUTION_SMOKE_DOC).contains("CI_ARTIFACT_CONSUMER_GUIDE.md"));
        assertTrue(read(OPERATOR_PACKAGING_DOC).contains("CI_ARTIFACT_CONSUMER_GUIDE.md"));
        assertTrue(read(TESTING_COVERAGE_DOC).contains("CI_ARTIFACT_CONSUMER_GUIDE.md"));
        assertTrue(read(API_SECURITY_DOC).contains("CI_ARTIFACT_CONSUMER_GUIDE.md"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
