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

class ReleaseV250AlignmentDocumentationTest {
    private static final Path POM = Path.of("pom.xml");
    private static final Path APP_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path API_APP = Path.of("src/main/java/com/richmond423/loadbalancerpro/api/LoadBalancerApiApplication.java");
    private static final Path CLI = Path.of("src/main/java/com/richmond423/loadbalancerpro/cli/LoadBalancerCLI.java");
    private static final Path RELEASE_INTENT = Path.of("docs/RELEASE_INTENT_REVIEW.md");
    private static final Path RELEASE_NOTES = Path.of("docs/RELEASE_NOTES_v2.5.0.md");
    private static final Path AUTH_CHECKLIST = Path.of("docs/V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md");
    private static final Path DECISION_SUMMARY = Path.of("docs/RELEASE_READINESS_DECISION_SUMMARY.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path READINESS_SUMMARY = Path.of("docs/PRODUCTION_READINESS_SUMMARY.md");
    private static final Path SCRIPT = Path.of("scripts/smoke/release-intent-review.ps1");

    @Test
    void projectRuntimeAndReleaseIntentVersionsAgreeOnV250() throws Exception {
        assertTrue(read(POM).contains("<version>2.5.0</version>"));
        assertTrue(read(APP_PROPERTIES).contains("loadbalancerpro.app.version=2.5.0"));
        assertTrue(read(APP_PROPERTIES).contains("info.app.version=2.5.0"));
        assertTrue(read(APP_PROPERTIES).contains("service.version]=2.5.0"));
        assertTrue(read(API_APP).contains("FALLBACK_VERSION = \"2.5.0\""));
        assertTrue(read(CLI).contains("VERSION = \"2.5.0\""));

        String intent = read(RELEASE_INTENT);
        assertTrue(intent.contains("Current Maven project version: `2.5.0`"));
        assertTrue(intent.contains("Latest immutable release tag observed for this line: `v2.4.2`"));
        assertTrue(intent.contains("Recommended exact next release version: `v2.5.0`"));
        assertTrue(intent.contains("release-intent-review.ps1 -DryRun -RecommendedVersion 2.5.0"));
    }

    @Test
    void releaseNotesAndAuthorizationChecklistExistAndAreLinked() throws Exception {
        assertTrue(Files.exists(RELEASE_NOTES), "v2.5.0 release notes should exist");
        assertTrue(Files.exists(AUTH_CHECKLIST), "v2.5.0 release authorization checklist should exist");

        for (Path doc : List.of(README, RELEASE_INTENT, DECISION_SUMMARY, TRUST_MAP, READINESS_SUMMARY)) {
            String content = read(doc);
            assertTrue(content.contains("RELEASE_NOTES_v2.5.0.md"),
                    doc + " should link v2.5.0 release notes");
            assertTrue(content.contains("V2_5_0_RELEASE_AUTHORIZATION_CHECKLIST.md"),
                    doc + " should link v2.5.0 authorization checklist");
        }
    }

    @Test
    void releaseNotesCoverEnterpriseReadinessScopeAndLimits() throws Exception {
        String notes = read(RELEASE_NOTES);

        for (String expected : List.of(
                "protected `prod` profile",
                "deny-by-default for non-`OPTIONS` `/api/**`",
                "dedicated role claims",
                "reject omitted JSON",
                "Release-candidate dry-run packet",
                "Production-candidate evidence gates",
                "IdP claim mapping examples",
                "Dependency/SAST risk workflow",
                "Container registry/signing rollout",
                "JAR/docs-first",
                "No production deployment certification is claimed",
                "Container publication, container signing",
                "release-downloads/")) {
            assertTrue(notes.contains(expected), "release notes should cover " + expected);
        }
    }

    @Test
    void authorizationChecklistKeepsFutureReleaseActionExplicitAndReviewOnly() throws Exception {
        String checklist = read(AUTH_CHECKLIST);

        for (String expected : List.of(
                "Release tag | `v2.5.0`",
                "Maven project version | `2.5.0`",
                "Exact commit",
                "checks are green",
                "Release notes were reviewed",
                "Dry-run packet and release-intent packet",
                "SBOM JSON/XML and SHA-256 checksum evidence",
                "GitHub artifact attestation expectation",
                "No container publication",
                "Rollback or withdrawal plan",
                "DO NOT RUN In This PR",
                "This release-prep sprint did not create tags",
                "This release-prep sprint did not create GitHub Releases",
                "This release-prep sprint did not upload or mutate release assets")) {
            assertTrue(checklist.contains(expected), "authorization checklist should cover " + expected);
        }
    }

    @Test
    void releaseIntentScriptCanRecommendAlignedProjectVersionWithoutPublishing() throws Exception {
        String script = read(SCRIPT);
        String normalized = script.toLowerCase(Locale.ROOT);

        assertTrue(script.contains("ConvertFrom-SemanticRef"));
        assertTrue(script.contains("$projectVersion -ne $latestVersion"));
        assertTrue(script.contains("recommendedVersion = $resolvedRecommendation"));
        assertTrue(script.contains("releaseTag = \"v$resolvedRecommendation\""));

        for (String forbidden : List.of(
                "git clean",
                "git tag",
                "git push",
                "gh release create",
                "gh release upload",
                "docker push",
                "cosign sign")) {
            assertFalse(normalized.contains(forbidden), "release-intent script should not contain " + forbidden);
        }
    }

    @Test
    void releasePrepDocsDoNotEmbedSecretValues() throws Exception {
        String docs = (read(RELEASE_NOTES) + "\n" + read(AUTH_CHECKLIST)).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "change_me_local_api_key",
                "ghp_",
                "github_pat_",
                "client_secret",
                "-----begin")) {
            assertFalse(docs.contains(forbidden), "release-prep docs should not contain " + forbidden);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
