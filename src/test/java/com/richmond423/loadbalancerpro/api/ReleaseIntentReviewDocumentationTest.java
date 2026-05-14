package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class ReleaseIntentReviewDocumentationTest {
    private static final Path REVIEW = Path.of("docs/RELEASE_INTENT_REVIEW.md");
    private static final Path SCRIPT = Path.of("scripts/smoke/release-intent-review.ps1");
    private static final Path README = Path.of("README.md");
    private static final Path DRY_RUN_PACKET = Path.of("docs/RELEASE_CANDIDATE_DRY_RUN_PACKET.md");
    private static final Path SUMMARY = Path.of("docs/PRODUCTION_READINESS_SUMMARY.md");
    private static final Path GATE = Path.of("docs/PRODUCTION_CANDIDATE_EVIDENCE_GATE.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");

    @Test
    void releaseIntentReviewExistsAndIsLinkedFromReviewerEntryPoints() throws Exception {
        assertTrue(Files.exists(REVIEW), "release-intent review doc should exist");
        assertTrue(Files.exists(SCRIPT), "release-intent review script should exist");

        for (Path doc : List.of(README, DRY_RUN_PACKET, SUMMARY, GATE, TRUST_MAP, RUNBOOK)) {
            assertTrue(read(doc).contains("RELEASE_INTENT_REVIEW.md"),
                    doc + " should link the release-intent review");
        }
    }

    @Test
    void releaseIntentReviewDocumentsExactVersionRecommendationAndReasoning() throws Exception {
        String review = read(REVIEW);

        for (String expected : List.of(
                "Recommended exact next release version: `v2.5.0`",
                "Recommended release type: JAR/docs-first minor release",
                "Recommend `v2.5.0` rather than `v2.4.3`",
                "Recommend a minor release rather than a major release",
                "containers default to the protected `prod` profile",
                "deny-by-default for non-`OPTIONS` `/api/**`",
                "dedicated role claims",
                "reject omitted JSON")) {
            assertTrue(review.contains(expected), "release-intent review should document " + expected);
        }
    }

    @Test
    void releaseIntentReviewDocumentsReviewOnlyBoundaryAndEvidenceExpectations() throws Exception {
        String review = read(REVIEW);

        for (String expected : List.of(
                "does not create a semantic tag",
                "does not create tags",
                "create a GitHub Release",
                "upload release assets",
                "publish containers",
                "sign artifacts",
                "`release-downloads/`",
                "mvn -q clean test",
                "mvn -q verify",
                "mvn -q -DskipTests package",
                "release-candidate-dry-run-packet.ps1 -Package",
                "release-intent-review.ps1 -DryRun",
                "SBOM",
                "SHA-256",
                "GitHub artifact attestations",
                "CodeQL",
                "Dependency Review",
                "Trivy",
                "GitHub Release assets")) {
            assertTrue(review.contains(expected), "release-intent review should cover " + expected);
        }
    }

    @Test
    void releaseIntentScriptWritesOnlyIgnoredTargetEvidence() throws Exception {
        String script = read(SCRIPT);

        assertTrue(script.contains("[string]$OutputDir = \"target/release-intent-review\""));
        assertTrue(script.contains("Assert-SafeOutputDirectory"));
        assertTrue(script.contains("Release-intent evidence output must stay under target/."));
        assertTrue(script.contains("release-intent-review.md"));
        assertTrue(script.contains("release-intent-review.json"));
        assertTrue(script.contains("versionAlignmentRequired"));
        assertTrue(script.contains("releaseDownloadsMutated = $false"));
    }

    @Test
    void releaseIntentScriptAvoidsPublishCommandsAndSecretValues() throws Exception {
        String script = read(SCRIPT).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "git clean",
                "git tag",
                "git push",
                "gh release create",
                "gh release upload",
                "gh release delete",
                "docker push",
                "cosign sign",
                "oras push",
                "helm push",
                "change_me_local_api_key",
                "ghp_",
                "github_pat_",
                "-----begin")) {
            assertFalse(script.contains(forbidden), "release-intent script should not contain " + forbidden);
        }
    }

    @Test
    void releaseIntentDocsAvoidEmbeddedSecretValues() throws Exception {
        String docs = read(REVIEW).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "change_me_local_api_key",
                "ghp_",
                "github_pat_",
                "client_secret",
                "-----begin")) {
            assertFalse(docs.contains(forbidden), "release-intent docs should not contain " + forbidden);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
