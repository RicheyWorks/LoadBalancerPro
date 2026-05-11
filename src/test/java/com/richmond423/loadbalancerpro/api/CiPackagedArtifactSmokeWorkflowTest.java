package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class CiPackagedArtifactSmokeWorkflowTest {
    private static final Path CI_WORKFLOW = Path.of(".github/workflows/ci.yml");
    private static final Path LOCAL_ARTIFACT_DOC = Path.of("docs/LOCAL_ARTIFACT_VERIFICATION.md");
    private static final Path DISTRIBUTION_SMOKE_DOC = Path.of("docs/OPERATOR_DISTRIBUTION_SMOKE_KIT.md");
    private static final Path TESTING_COVERAGE_DOC = Path.of("docs/TESTING_COVERAGE.md");
    private static final Path WINDOWS_HELPER = Path.of("scripts/local-artifact-verify.ps1");
    private static final Path UNIX_HELPER = Path.of("scripts/local-artifact-verify.sh");
    private static final Path DEFAULT_PROPERTIES = Path.of("src/main/resources/application.properties");

    @Test
    void ciWorkflowVerifiesPackagedJarResourcesAndUploadsWorkflowArtifactOnly() throws Exception {
        String workflow = read(CI_WORKFLOW);

        assertTrue(workflow.contains("Verify packaged artifact resources"));
        assertTrue(workflow.contains("Upload packaged artifact smoke output"));
        assertTrue(workflow.contains("packaged-artifact-smoke"));
        assertTrue(workflow.contains("target/artifact-smoke"));
        assertTrue(workflow.contains("artifact-smoke-summary.txt"));
        assertTrue(workflow.contains("artifact-sha256.txt"));
        assertTrue(workflow.contains("jar-resource-list.txt"));
        assertTrue(workflow.contains("sha256sum \"$JAR\""));
        assertTrue(workflow.contains("jar tf \"$JAR\" | sort > \"$RESOURCE_LIST\""));
        assertTrue(workflow.contains("actions/upload-artifact"));
        assertTrue(workflow.contains("BOOT-INF/classes/static/proxy-status.html"));
        assertTrue(workflow.contains("BOOT-INF/classes/static/load-balancing-cockpit.html"));
        assertTrue(workflow.contains("BOOT-INF/classes/application-proxy-demo-round-robin.properties"));
        assertTrue(workflow.contains("BOOT-INF/classes/application-proxy-demo-weighted-round-robin.properties"));
        assertTrue(workflow.contains("BOOT-INF/classes/application-proxy-demo-failover.properties"));
        assertTrue(workflow.contains("BOOT-INF/classes/com/richmond423/loadbalancerpro/demo/ProxyDemoFixtureLauncher.class"));
    }

    @Test
    void ciWorkflowAvoidsReleaseTagAndReleaseDownloadCommands() throws Exception {
        String workflow = read(CI_WORKFLOW).toLowerCase(Locale.ROOT);

        assertFalse(workflow.contains("gh release"), "CI smoke workflow must not create or upload releases");
        assertFalse(workflow.contains("git tag"), "CI smoke workflow must not create tags");
        assertFalse(workflow.contains("actions/create-release"), "CI smoke workflow must not create releases");
        assertFalse(workflow.contains("softprops/action-gh-release"), "CI smoke workflow must not upload release assets");
        assertFalse(workflow.contains("upload-release-asset"), "CI smoke workflow must not upload release assets");
        assertFalse(workflow.contains("release-downloads"), "CI smoke workflow must not touch release-downloads");
    }

    @Test
    void docsPointReviewersToPackagedArtifactSmokeWorkflowArtifact() throws Exception {
        String localArtifactDoc = read(LOCAL_ARTIFACT_DOC);
        String distributionSmokeDoc = read(DISTRIBUTION_SMOKE_DOC);
        String testingCoverageDoc = read(TESTING_COVERAGE_DOC);
        String combined = localArtifactDoc + "\n" + distributionSmokeDoc + "\n" + testingCoverageDoc;

        assertTrue(combined.contains("packaged-artifact-smoke"));
        assertTrue(combined.contains("artifact-smoke-summary.txt"));
        assertTrue(combined.contains("artifact-sha256.txt"));
        assertTrue(combined.contains("jar-resource-list.txt"));
        assertTrue(combined.contains("GitHub Actions artifact"));
        assertTrue(combined.contains("no tags"));
        assertTrue(combined.contains("GitHub releases"));
        assertTrue(combined.contains("release assets"));
        assertTrue(combined.contains("release-downloads/"));
    }

    @Test
    void localHelpersStayAlignedWithCiArtifactNamesAndAvoidReleaseCommands() throws Exception {
        String powershell = read(WINDOWS_HELPER);
        String unix = read(UNIX_HELPER);
        String combined = powershell + "\n" + unix;
        String normalized = combined.toLowerCase(Locale.ROOT);

        assertTrue(combined.contains("packaged-artifact-smoke"));
        assertTrue(combined.contains("artifact-smoke-summary.txt"));
        assertTrue(combined.contains("artifact-sha256.txt"));
        assertTrue(combined.contains("jar-resource-list.txt"));
        assertFalse(normalized.contains("gh release"), "local helpers must not create or upload releases");
        assertFalse(normalized.contains("git tag"), "local helpers must not create tags");
        assertFalse(normalized.contains("softprops/action-gh-release"), "local helpers must not call release actions");
        assertFalse(normalized.contains("release-downloads"), "local helpers must not touch release-downloads");
    }

    @Test
    void defaultProxyConfigurationStillRequiresExplicitOptIn() throws Exception {
        String defaults = read(DEFAULT_PROPERTIES);

        assertTrue(defaults.contains("loadbalancerpro.proxy.enabled=false"));
        assertFalse(defaults.contains("loadbalancerpro.proxy.enabled=true"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
