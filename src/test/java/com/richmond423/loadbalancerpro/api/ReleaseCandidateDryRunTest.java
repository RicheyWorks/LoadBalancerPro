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

class ReleaseCandidateDryRunTest {
    private static final Path DRY_RUN_GUIDE = Path.of("docs/RELEASE_CANDIDATE_DRY_RUN.md");
    private static final Path PACKET_TEMPLATE = Path.of("docs/RELEASE_CANDIDATE_REVIEW_PACKET_TEMPLATE.md");
    private static final Path README = Path.of("README.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path CI_ARTIFACT_GUIDE = Path.of("docs/CI_ARTIFACT_CONSUMER_GUIDE.md");
    private static final Path LOCAL_ARTIFACT_DOC = Path.of("docs/LOCAL_ARTIFACT_VERIFICATION.md");
    private static final Path DISTRIBUTION_SMOKE_DOC = Path.of("docs/OPERATOR_DISTRIBUTION_SMOKE_KIT.md");
    private static final Path OPERATOR_PACKAGING_DOC = Path.of("docs/OPERATOR_PACKAGING.md");
    private static final Path TESTING_COVERAGE_DOC = Path.of("docs/TESTING_COVERAGE.md");
    private static final Path API_SECURITY_DOC = Path.of("docs/API_SECURITY.md");
    private static final Path CI_WORKFLOW = Path.of(".github/workflows/ci.yml");
    private static final Pattern FORTY_HEX = Pattern.compile("(?i)\\b[a-f0-9]{40}\\b");
    private static final Pattern SHA256_HEX = Pattern.compile("(?i)\\b[a-f0-9]{64}\\b");

    @Test
    void dryRunGuideMapsReleaseCandidateEvidenceWithoutPublishing() throws Exception {
        String guide = read(DRY_RUN_GUIDE);

        assertTrue(guide.contains("# Release Candidate Dry Run"));
        assertTrue(guide.contains("This dry run does not publish a release."));
        assertTrue(guide.contains("No tags."));
        assertTrue(guide.contains("No GitHub Releases."));
        assertTrue(guide.contains("No release assets."));
        assertTrue(guide.contains("No `release-downloads/` evidence."));
        assertTrue(guide.contains("jacoco-coverage-report"));
        assertTrue(guide.contains("packaged-artifact-smoke"));
        assertTrue(guide.contains("loadbalancerpro-sbom"));
        assertTrue(guide.contains("artifact-smoke-summary.txt"));
        assertTrue(guide.contains("artifact-sha256.txt"));
        assertTrue(guide.contains("jar-resource-list.txt"));
        assertTrue(guide.contains("proxy-status.html"));
        assertTrue(guide.contains("load-balancing-cockpit.html"));
        assertTrue(guide.contains("ProxyDemoFixtureLauncher.class"));
        assertTrue(guide.contains("CI reported zero skipped tests"));
    }

    @Test
    void dryRunGuideIncludesStructuredGoNoGoChecklistAndPacketTemplateLink() throws Exception {
        String guide = read(DRY_RUN_GUIDE);

        assertTrue(guide.contains("## Go/No-Go Checklist"));
        assertTrue(guide.contains("| Evidence item | Source | Expected result | Status | Notes |"));
        assertTrue(guide.contains("Final dry-run result"));
        assertTrue(guide.contains("Go or no-go recorded"));
        assertTrue(guide.contains("RELEASE_CANDIDATE_REVIEW_PACKET_TEMPLATE.md"));
        assertTrue(guide.contains("Do not add sample hashes, sample checksums, or invented evidence."));
    }

    @Test
    void reviewerPacketTemplateUsesPlaceholdersOnly() throws Exception {
        String template = read(PACKET_TEMPLATE);

        assertTrue(template.contains("# Release Candidate Review Packet Template"));
        assertTrue(template.contains("<commit-hash>"));
        assertTrue(template.contains("<ci-run-url>"));
        assertTrue(template.contains("<count-from-ci>"));
        assertTrue(template.contains("<local-sha256-or-not-run>"));
        assertTrue(template.contains("<ci-sha256-or-not-reviewed>"));
        assertTrue(template.contains("<go|no-go|defer>"));
        assertFalse(FORTY_HEX.matcher(template).find(), "template must not include fake commit hashes");
        assertFalse(SHA256_HEX.matcher(template).find(), "template must not include fake checksums");
    }

    @Test
    void sbomArtifactReferenceIsBackedByCurrentWorkflow() throws Exception {
        String guide = read(DRY_RUN_GUIDE);
        String workflow = read(CI_WORKFLOW);

        assertTrue(guide.contains("loadbalancerpro-sbom"));
        assertTrue(workflow.contains("name: loadbalancerpro-sbom"));
        assertTrue(workflow.contains("target/bom.json"));
        assertTrue(workflow.contains("target/bom.xml"));
    }

    @Test
    void existingOperatorDocsLinkToReleaseCandidateDryRunGuide() throws Exception {
        assertTrue(read(README).contains("RELEASE_CANDIDATE_DRY_RUN.md"));
        assertTrue(read(RUNBOOK).contains("RELEASE_CANDIDATE_DRY_RUN.md"));
        assertTrue(read(CI_ARTIFACT_GUIDE).contains("RELEASE_CANDIDATE_DRY_RUN.md"));
        assertTrue(read(LOCAL_ARTIFACT_DOC).contains("RELEASE_CANDIDATE_DRY_RUN.md"));
        assertTrue(read(DISTRIBUTION_SMOKE_DOC).contains("RELEASE_CANDIDATE_DRY_RUN.md"));
        assertTrue(read(OPERATOR_PACKAGING_DOC).contains("RELEASE_CANDIDATE_DRY_RUN.md"));
        assertTrue(read(TESTING_COVERAGE_DOC).contains("RELEASE_CANDIDATE_DRY_RUN.md"));
        assertTrue(read(API_SECURITY_DOC).contains("RELEASE_CANDIDATE_DRY_RUN.md"));
    }

    @Test
    void dryRunDocsAvoidReleaseCommandsAndUnsafeClaims() throws Exception {
        String combined = read(DRY_RUN_GUIDE) + "\n" + read(PACKET_TEMPLATE);
        String normalized = combined.toLowerCase(Locale.ROOT);

        assertFalse(normalized.contains("gh release"), "dry-run docs must not add release publishing commands");
        assertFalse(normalized.contains("git tag"), "dry-run docs must not add tag creation commands");
        assertFalse(normalized.contains("softprops/action-gh-release"), "dry-run docs must not use release asset actions");
        assertFalse(normalized.contains("actions/create-release"), "dry-run docs must not use release creation actions");
        assertFalse(normalized.contains("upload-release-asset"), "dry-run docs must not use release asset actions");
        assertFalse(normalized.contains("new cloudmanager"), "dry-run docs must not construct CloudManager");
        assertFalse(normalized.contains("cloudmanager("), "dry-run docs must not construct CloudManager");
        assertFalse(normalized.contains("production-grade"), "dry-run docs must not add production-grade claims");
        assertFalse(normalized.contains("benchmark proof"), "dry-run docs must not add benchmark claims");
        assertFalse(normalized.contains("certification proof"), "dry-run docs must not add certification claims");
        assertFalse(normalized.contains("identity proof"), "dry-run docs must not add identity claims");
        assertFalse(normalized.contains("fake"), "dry-run docs should use placeholder language instead of fake evidence language");
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
