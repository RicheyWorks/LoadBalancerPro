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

class ReadmeVisibilityDocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Path EXECUTIVE_SUMMARY = Path.of("docs/EXECUTIVE_SUMMARY.md");
    private static final Path DEMO_WALKTHROUGH = Path.of("docs/DEMO_WALKTHROUGH.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path DEFAULT_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path API_SECURITY =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/config/ApiSecurityConfiguration.java");

    private static final Pattern FAKE_COVERAGE_PERCENT =
            Pattern.compile("(?i)\\b(?:coverage|covered)\\s*(?:is|at|=|:)?\\s*\\d{1,3}(?:\\.\\d+)?%");
    private static final Pattern FAKE_LIVE_DEMO_URL =
            Pattern.compile("(?i)https?://(?!localhost\\b|127\\.0\\.0\\.1\\b)[^\\s)]*(?:loadbalancerpro|demo)");
    private static final Pattern RELEASE_COMMAND =
            Pattern.compile("(?im)^\\s*(gh\\s+release|git\\s+tag)\\b");
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+" + "CloudManager\\s*\\(|" + "CloudManager\\s*\\(");

    @Test
    void readmeHasClearPublicPositioningAndQuickEvaluationPath() throws Exception {
        String readme = read(README);

        assertTrue(readme.contains("## Why This Project Matters"));
        assertTrue(readme.contains("## Start Here For Reviewers"));
        assertTrue(readme.contains("## Evaluate In 5 Minutes"));
        assertTrue(readme.contains("## Enterprise-Style Operator Foundation"));
        assertTrue(readme.contains("## Coverage And Evidence"));
        assertTrue(readme.contains("operator-focused service"));
        assertTrue(readme.toLowerCase(Locale.ROOT).contains("enterprise-style operator foundation"));
        assertTrue(readme.contains("http://localhost:8080/"));
        assertTrue(readme.contains("http://localhost:8080/load-balancing-cockpit.html"));
        assertTrue(readme.contains("docs/REVIEWER_TRUST_MAP.md#reviewer-demo-path"));
    }

    @Test
    void readmeLinksToReviewerAndOperatorEvidenceHubs() throws Exception {
        String readme = read(README);

        for (String expected : List.of(
                "docs/EXECUTIVE_SUMMARY.md",
                "docs/DEMO_WALKTHROUGH.md",
                "docs/REVIEWER_TRUST_MAP.md",
                "docs/REVIEWER_TRUST_MAP.md#reviewer-demo-path",
                "docs/OPERATOR_RUN_PROFILES.md",
                "docs/DEPLOYMENT_SMOKE_KIT.md",
                "docs/CONTAINER_DEPLOYMENT.md",
                "docs/API_SECURITY.md",
                "docs/DEPLOYMENT_HARDENING_GUIDE.md")) {
            assertTrue(readme.contains(expected), "README should link to " + expected);
        }
    }

    @Test
    void readmeReviewerStartPathNamesSafeEvidenceCommandsAndBoundaries() throws Exception {
        String readme = read(README);
        String normalized = readme.toLowerCase(Locale.ROOT);

        assertTrue(readme.contains("mvn -Dtest=LocalProxyEvidenceExportTest test"));
        assertTrue(readme.contains("mvn -Dtest=PrivateNetworkProxyDryRunEvidenceTest test"));
        assertTrue(readme.contains("postman"));
        assertTrue(readme.contains("operator smoke dry-runs"));
        assertTrue(readme.contains("CI/CodeQL evidence"));
        assertTrue(normalized.contains("ignored `target/` output"));
        assertTrue(normalized.contains("secrets are redacted"));
        assertTrue(normalized.contains("live private-network validation remains intentionally unimplemented"));
    }

    @Test
    void readmeDocumentsCoverageArtifactsWithoutInventingCoverageNumbers() throws Exception {
        String readme = read(README);

        assertTrue(readme.contains("jacoco-coverage-report"));
        assertTrue(readme.contains("packaged-artifact-smoke"));
        assertTrue(readme.contains("loadbalancerpro-sbom"));
        assertTrue(readme.contains("does not claim a fixed coverage percentage"));
        assertFalse(FAKE_COVERAGE_PERCENT.matcher(readme).find(),
                "README must not invent a fixed coverage percentage");
    }

    @Test
    void executiveSummaryAndDemoWalkthroughExistAndPointToEvidence() throws Exception {
        assertTrue(Files.exists(EXECUTIVE_SUMMARY));
        assertTrue(Files.exists(DEMO_WALKTHROUGH));

        String executiveSummary = read(EXECUTIVE_SUMMARY);
        String demoWalkthrough = read(DEMO_WALKTHROUGH);
        String trustMap = read(REVIEWER_TRUST_MAP);

        assertTrue(executiveSummary.contains("# Executive Summary"));
        assertTrue(executiveSummary.contains("REVIEWER_TRUST_MAP.md"));
        assertTrue(executiveSummary.contains("DEPLOYMENT_SMOKE_KIT.md"));
        assertTrue(executiveSummary.contains("CONTAINER_DEPLOYMENT.md"));
        assertTrue(executiveSummary.contains("jacoco-coverage-report"));
        assertTrue(demoWalkthrough.contains("# Demo Walkthrough"));
        assertTrue(demoWalkthrough.contains("60 to 90 second"));
        assertTrue(demoWalkthrough.contains("load-balancing-cockpit.html"));
        assertTrue(demoWalkthrough.contains("operator-run-profiles-smoke.ps1"));
        assertTrue(demoWalkthrough.contains("postman-enterprise-lab-safe-smoke.ps1"));
        assertTrue(demoWalkthrough.contains("mvn -Dtest=LocalProxyEvidenceExportTest test"));
        assertTrue(demoWalkthrough.contains("mvn -Dtest=PrivateNetworkProxyDryRunEvidenceTest test"));
        assertTrue(demoWalkthrough.contains("target/proxy-evidence/local-proxy-evidence.md"));
        assertTrue(demoWalkthrough.contains("target/proxy-evidence/local-proxy-evidence.json"));
        assertTrue(demoWalkthrough.contains("target/proxy-evidence/private-network-validation-dry-run.md"));
        assertTrue(demoWalkthrough.contains("target/proxy-evidence/private-network-validation-dry-run.json"));
        assertTrue(demoWalkthrough.toLowerCase(Locale.ROOT)
                .contains("live private-network traffic execution is not implemented yet"));
        assertTrue(demoWalkthrough.toLowerCase(Locale.ROOT).contains("should not contain api keys"));
        assertTrue(demoWalkthrough.toLowerCase(Locale.ROOT).contains("dns resolution"));
        assertTrue(demoWalkthrough.toLowerCase(Locale.ROOT).contains("release-downloads/"));
        assertTrue(trustMap.contains("EXECUTIVE_SUMMARY.md"));
        assertTrue(trustMap.contains("DEMO_WALKTHROUGH.md"));
    }

    @Test
    void publicFacingDocsAvoidFakeLiveDemoCoverageReleaseAndInflatedClaims() throws Exception {
        for (Path doc : List.of(README, EXECUTIVE_SUMMARY, DEMO_WALKTHROUGH, REVIEWER_TRUST_MAP)) {
            String content = read(doc);
            String normalized = content.toLowerCase(Locale.ROOT);

            assertFalse(FAKE_LIVE_DEMO_URL.matcher(content).find(), doc + " must not add fake live demo URLs");
            assertFalse(RELEASE_COMMAND.matcher(content).find(), doc + " must not add release or tag commands");
            assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(content).find(), doc + " must not construct CloudManager");
            assertFalse(normalized.contains("fake stars"), doc + " must not invent popularity evidence");
            assertFalse(normalized.contains("production-grade gateway"), doc + " must not add gateway claims");
            assertFalse(normalized.contains("production-grade security"), doc + " must not add security claims");
            assertFalse(normalized.contains("production-grade observability"), doc + " must not add observability claims");
            assertFalse(normalized.contains("certified gateway"), doc + " must not add certification claims");
            assertFalse(normalized.contains("certification proof"), doc + " must not add certification proof claims");
            assertFalse(normalized.contains("is benchmark proof"), doc + " must not add benchmark proof claims");
            assertFalse(normalized.contains("provides benchmark proof"), doc + " must not add benchmark proof claims");
            assertFalse(normalized.contains("benchmark result:"), doc + " must not add benchmark result claims");
        }
    }

    @Test
    void proxyDefaultsApiKeyAndReloadBoundariesRemainPreserved() throws Exception {
        String defaults = read(DEFAULT_PROPERTIES);
        String security = read(API_SECURITY);

        assertTrue(defaults.contains("loadbalancerpro.proxy.enabled=false"));
        assertFalse(defaults.contains("loadbalancerpro.proxy.enabled=true"));
        assertTrue(security.contains("HttpMethod.GET, \"/api/proxy/status\""));
        assertTrue(security.contains("HttpMethod.POST, \"/api/proxy/reload\""));
        assertTrue(security.contains("hasRole(allocationRole)"));
    }

    @Test
    void existingProxySecurityReloadSmokeAndContainerTestsRemainPresent() {
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/ProdApiKeyProtectionTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/ReverseProxyReloadSecurityTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/DeploymentSmokeKitDocumentationTest.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/richmond423/loadbalancerpro/api/ContainerDeploymentDocumentationTest.java")));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
