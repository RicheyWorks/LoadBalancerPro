package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
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
    private static final Path V1_9_1_EVIDENCE_PLAN = Path.of("docs/V1_9_1_RELEASE_EVIDENCE_DOCS_PLAN.md");
    private static final Path RELEASE_ARTIFACT_EVIDENCE = Path.of("evidence/RELEASE_ARTIFACT_EVIDENCE.md");
    private static final Path THIS_TEST = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/api/ReviewerTrustMapDocumentationTest.java");

    private static final List<Path> REVIEWER_NAV_DOCS = List.of(
            TRUST_MAP,
            README,
            RUNBOOK,
            OPERATOR_PACKAGING,
            INSTALL_MATRIX,
            RELEASE_DRY_RUN,
            REAL_BACKEND_EXAMPLES,
            TESTING_COVERAGE);

    private static final List<Path> RELEASE_FREE_DOCS = List.of(
            TRUST_MAP,
            OPERATOR_PACKAGING,
            INSTALL_MATRIX,
            RELEASE_DRY_RUN,
            REAL_BACKEND_EXAMPLES,
            TESTING_COVERAGE);

    private static final Pattern FAKE_HASH =
            Pattern.compile("\\b[0-9a-fA-F]{40}\\b|\\b[0-9a-fA-F]{64}\\b");
    private static final Pattern MARKDOWN_LINK =
            Pattern.compile("\\[[^\\]]+\\]\\(([^)\\s]+\\.md(?:#[^)\\s]+)?)\\)");
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+CloudManager\\s*\\(|CloudManager\\s*\\(");

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
                "LocalOnlyRealBackendProxyValidationTest",
                "LocalProxyEvidenceExportTest",
                "target/proxy-evidence/local-proxy-evidence.md",
                "target/proxy-evidence/local-proxy-evidence.json",
                "PRIVATE_NETWORK_PROXY_PROFILE_PLAN.md",
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
    void localProxyEvidenceExportRecipeIsConciseAndSafetyBounded() throws Exception {
        String trustMap = read(TRUST_MAP);
        String readme = read(README);
        String runbook = read(RUNBOOK);
        String recipe = section(trustMap, "### Local Proxy Evidence Export", "### Release-Readiness Review");
        String normalized = recipe.toLowerCase(Locale.ROOT);

        assertTrue(recipe.contains("mvn -Dtest=LocalProxyEvidenceExportTest test"));
        assertTrue(recipe.contains("target/proxy-evidence/local-proxy-evidence.md"));
        assertTrue(recipe.contains("target/proxy-evidence/local-proxy-evidence.json"));
        assertTrue(normalized.contains("markdown file is the human review path"));
        assertTrue(normalized.contains("json file is the structured evidence path"));
        assertTrue(normalized.contains("loopback/local-only jdk `httpserver`"));
        assertTrue(recipe.contains("/proxy/**"));
        assertTrue(normalized.contains("backend receipt"));
        assertTrue(normalized.contains("forwarded status/body/header proof"));
        assertTrue(normalized.contains("prod api-key `401`/`200` boundary"));
        assertTrue(normalized.contains("ignored `target/` output"));
        assertTrue(normalized.contains("not tracked docs"));
        assertTrue(normalized.contains("do not write api keys or secrets"));
        assertTrue(normalized.contains("do not add external network behavior"));
        assertTrue(recipe.contains("apiKeyRedacted=\"<REDACTED>\""));
        assertTrue(readme.contains("REVIEWER_TRUST_MAP.md#local-proxy-evidence-export"));
        assertTrue(runbook.contains("REVIEWER_TRUST_MAP.md#local-proxy-evidence-export"));
    }

    @Test
    void readmeAndRunbookLinksResolveToReviewerTrustMap() throws Exception {
        assertLocalMarkdownLinkResolvesTo(README, "docs/REVIEWER_TRUST_MAP.md", TRUST_MAP);
        assertLocalMarkdownLinkResolvesTo(RUNBOOK, "REVIEWER_TRUST_MAP.md", TRUST_MAP);
    }

    @Test
    void reviewerTrustMapMarkdownReferencesResolve() throws Exception {
        for (String link : markdownLinks(read(TRUST_MAP))) {
            Path resolved = resolveMarkdownLink(TRUST_MAP, link);
            assertTrue(Files.exists(resolved), "trust map link should resolve: " + link + " -> " + resolved);
        }
    }

    @Test
    void releaseEvidencePlanUsesResolvableEvidenceLink() throws Exception {
        String plan = read(V1_9_1_EVIDENCE_PLAN);

        assertTrue(plan.contains("[`RELEASE_ARTIFACT_EVIDENCE.md`](../evidence/RELEASE_ARTIFACT_EVIDENCE.md)"));
        assertFalse(plan.contains("](evidence/RELEASE_ARTIFACT_EVIDENCE.md)"),
                "docs-local markdown links must not point to a nonexistent docs/evidence path");
        assertEquals(RELEASE_ARTIFACT_EVIDENCE,
                resolveMarkdownLink(V1_9_1_EVIDENCE_PLAN, "../evidence/RELEASE_ARTIFACT_EVIDENCE.md"));
        assertTrue(Files.exists(RELEASE_ARTIFACT_EVIDENCE), "release artifact evidence should exist");
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
        assertTrue(trustMap.contains("explicit operator-provided backend URLs only"));
        assertTrue(trustMap.contains("no private-network live execution until separately approved"));
    }

    @Test
    void reviewerTrustMapAvoidsFakeEvidenceAndUnsafeClaims() throws Exception {
        String trustMap = read(TRUST_MAP);
        String normalized = trustMap.toLowerCase(Locale.ROOT);

        assertFalse(FAKE_HASH.matcher(trustMap).find(), "trust map must not include fake hashes");
        assertFalse(normalized.contains("fake evidence"), "trust map must not include fake evidence");
        assertFalse(normalized.contains("gh release"), "trust map must not add release commands");
        assertFalse(normalized.contains("git tag"), "trust map must not add tag commands");
        assertNoCloudManagerConstruction(TRUST_MAP, trustMap);
        assertFalse(normalized.contains("production-grade"), "trust map must not add production-grade claims");
        assertFalse(normalized.contains("benchmark proof"), "trust map must not add benchmark proof claims");
        assertFalse(normalized.contains("certification proof"), "trust map must not add certification proof claims");
    }

    @Test
    void reviewerNavigationDocsAvoidPositiveProductionBenchmarkAndCertificationClaims() throws Exception {
        for (Path doc : REVIEWER_NAV_DOCS) {
            String normalized = read(doc).toLowerCase(Locale.ROOT);

            assertFalse(normalized.contains("production-grade gateway"), doc + " must not add production-grade claims");
            assertFalse(normalized.contains("production-ready gateway"), doc + " must not add production-ready claims");
            assertFalse(normalized.contains("is a production benchmark"), doc + " must not add benchmark claims");
            assertFalse(normalized.contains("benchmark result"), doc + " must not add benchmark result claims");
            assertFalse(normalized.contains("certification proof"), doc + " must not add certification proof claims");
            assertFalse(normalized.contains("certified gateway"), doc + " must not add certification claims");
        }
    }

    @Test
    void releaseFreeDocsAvoidTagReleaseAndAssetCreationCommands() throws Exception {
        for (Path doc : RELEASE_FREE_DOCS) {
            String normalized = read(doc).toLowerCase(Locale.ROOT);

            assertFalse(normalized.contains("gh release create"), doc + " must not create releases");
            assertFalse(normalized.contains("gh release upload"), doc + " must not upload release assets");
            assertFalse(normalized.contains("gh release edit"), doc + " must not edit releases");
            assertFalse(normalized.contains("git tag -"), doc + " must not create tags");
        }
    }

    @Test
    void docsAndStaticReferenceTestAvoidCloudManagerConstruction() throws Exception {
        for (Path doc : REVIEWER_NAV_DOCS) {
            assertNoCloudManagerConstruction(doc, read(doc));
        }
        assertNoCloudManagerConstruction(V1_9_1_EVIDENCE_PLAN, read(V1_9_1_EVIDENCE_PLAN));
        assertNoCloudManagerConstruction(THIS_TEST, read(THIS_TEST));
    }

    private static void assertLocalMarkdownLinkResolvesTo(Path source, String link, Path expected) throws IOException {
        assertTrue(read(source).contains("(" + link + ")"), source + " should link to " + link);
        assertEquals(expected, resolveMarkdownLink(source, link));
        assertTrue(Files.exists(expected), expected + " should exist");
    }

    private static List<String> markdownLinks(String markdown) {
        Matcher matcher = MARKDOWN_LINK.matcher(markdown);
        List<String> links = new ArrayList<>();
        while (matcher.find()) {
            links.add(matcher.group(1));
        }
        return links;
    }

    private static Path resolveMarkdownLink(Path source, String link) {
        String linkWithoutFragment = link.split("#", 2)[0];
        Path parent = source.getParent();
        Path base = parent == null ? Path.of("") : parent;
        return base.resolve(linkWithoutFragment).normalize();
    }

    private static void assertNoCloudManagerConstruction(Path source, String text) {
        assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(text).find(), source + " must not construct CloudManager");
    }

    private static String section(String text, String startHeading, String endHeading) {
        int start = text.indexOf(startHeading);
        assertTrue(start >= 0, "missing section start: " + startHeading);
        int end = text.indexOf(endHeading, start + startHeading.length());
        assertTrue(end > start, "missing section end: " + endHeading);
        return text.substring(start, end);
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
