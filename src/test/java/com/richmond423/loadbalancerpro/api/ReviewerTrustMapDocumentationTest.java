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

class ReviewerTrustMapDocumentationTest {
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path README = Path.of("README.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path OPERATOR_PACKAGING = Path.of("docs/OPERATOR_PACKAGING.md");
    private static final Path INSTALL_MATRIX = Path.of("docs/OPERATOR_INSTALL_RUN_MATRIX.md");
    private static final Path RELEASE_DRY_RUN = Path.of("docs/RELEASE_CANDIDATE_DRY_RUN.md");
    private static final Path REAL_BACKEND_EXAMPLES = Path.of("docs/REAL_BACKEND_PROXY_EXAMPLES.md");
    private static final Path TESTING_COVERAGE = Path.of("docs/TESTING_COVERAGE.md");

    private static final Pattern FAKE_HASH =
            Pattern.compile("\\b[0-9a-fA-F]{40}\\b|\\b[0-9a-fA-F]{64}\\b");

    @Test
    void reviewerTrustMapExistsAndProvidesTopLevelNavigation() throws Exception {
        String trustMap = read(TRUST_MAP);

        assertTrue(trustMap.contains("# Reviewer Trust Map"));
        assertTrue(trustMap.contains("## Start Here"));
        assertTrue(trustMap.contains("## Evidence Matrix"));
        assertTrue(trustMap.contains("## Recommended Reviewer Flows"));
        assertTrue(trustMap.contains("## Safety Boundaries"));
        assertTrue(trustMap.contains("## Current Limitations"));
        assertTrue(trustMap.contains("10-Minute Quick Review"));
        assertTrue(trustMap.contains("Proxy-Focused Review"));
        assertTrue(trustMap.contains("Release-Readiness Review"));
        assertTrue(trustMap.contains("Operator Install/Run Review"));
    }

    @Test
    void reviewerTrustMapCoversPrimaryEvidencePaths() throws Exception {
        String trustMap = read(TRUST_MAP);

        for (String expected : List.of(
                "TESTING_COVERAGE.md",
                "REVERSE_PROXY_MODE.md",
                "REVERSE_PROXY_HEALTH_AND_METRICS.md",
                "REVERSE_PROXY_RESILIENCE.md",
                "PROXY_OPERATOR_STATUS_UI.md",
                "PROXY_STRATEGY_DEMO_LAB.md",
                "PROXY_DEMO_STACK.md",
                "PROXY_DEMO_FIXTURE_LAUNCHER.md",
                "REAL_BACKEND_PROXY_EXAMPLES.md",
                "OPERATOR_INSTALL_RUN_MATRIX.md",
                "OPERATOR_PACKAGING.md",
                "LOCAL_ARTIFACT_VERIFICATION.md",
                "CI_ARTIFACT_CONSUMER_GUIDE.md",
                "RELEASE_CANDIDATE_DRY_RUN.md",
                "RELEASE_INTENT_CHECKLIST.md",
                "JAVAFX_OPTIONAL_UI.md",
                "PACKAGE_NAMING.md",
                "jacoco-coverage-report",
                "packaged-artifact-smoke",
                "loadbalancerpro-sbom",
                "zero skipped tests")) {
            assertTrue(trustMap.contains(expected), "trust map should mention " + expected);
        }
    }

    @Test
    void keyDocsLinkBackToReviewerTrustMap() throws Exception {
        for (Path doc : List.of(README, RUNBOOK, OPERATOR_PACKAGING, INSTALL_MATRIX,
                RELEASE_DRY_RUN, REAL_BACKEND_EXAMPLES, TESTING_COVERAGE)) {
            assertTrue(read(doc).contains("REVIEWER_TRUST_MAP.md"), doc + " should link to trust map");
        }
    }

    @Test
    void reviewerTrustMapDocumentsSafetyBoundariesWithoutReleaseActions() throws Exception {
        String trustMap = read(TRUST_MAP);

        assertTrue(trustMap.contains("Proxy is disabled by default."));
        assertTrue(trustMap.contains("JavaFX is optional"));
        assertTrue(trustMap.contains("Release-free docs do not create tags, GitHub Releases, or release assets."));
        assertTrue(trustMap.contains("`release-downloads/` remains manual and explicit only."));
        assertTrue(trustMap.contains("Workflow artifacts are not GitHub Release assets."));
        assertTrue(trustMap.contains("do not construct or mutate `CloudManager`"));
    }

    @Test
    void reviewerTrustMapAvoidsFakeEvidenceAndUnsafeClaims() throws Exception {
        String trustMap = read(TRUST_MAP);
        String normalized = trustMap.toLowerCase(Locale.ROOT);

        assertFalse(FAKE_HASH.matcher(trustMap).find(), "trust map must not include fake hashes");
        assertFalse(normalized.contains("fake evidence"), "trust map must not include fake evidence");
        assertFalse(normalized.contains("gh release"), "trust map must not add release commands");
        assertFalse(normalized.contains("git tag"), "trust map must not add tag commands");
        assertFalse(normalized.contains("new cloudmanager"), "trust map must not construct CloudManager");
        assertFalse(normalized.contains("cloudmanager("), "trust map must not construct CloudManager");
        assertFalse(normalized.contains("production-grade"), "trust map must not add production-grade claims");
        assertFalse(normalized.contains("benchmark proof"), "trust map must not add benchmark proof claims");
        assertFalse(normalized.contains("certification proof"), "trust map must not add certification proof claims");
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
