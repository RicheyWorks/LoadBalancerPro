package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class DependencyCiHygieneWorkflowTest {
    private static final Path CI_WORKFLOW = Path.of(".github/workflows/ci.yml");
    private static final Path RELEASE_WORKFLOW = Path.of(".github/workflows/release-artifacts.yml");
    private static final Path DEFAULT_PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path API_SECURITY = Path.of("src/main/java/com/richmond423/loadbalancerpro/api/config/ApiSecurityConfiguration.java");

    @Test
    void ciWorkflowUsesNode24DependencyReviewActionAndKeepsArtifactNames() throws Exception {
        String workflow = read(CI_WORKFLOW);

        assertTrue(workflow.contains("actions/dependency-review-action@v5.0.0"));
        assertTrue(workflow.contains("actions/dependency-review-action@a1d282b36b6f3519aa1f3fc636f609c47dddb294"));
        assertFalse(workflow.contains("actions/dependency-review-action@v4"));
        assertFalse(workflow.contains("2031cfc080254a8a887f58cffee85186f0e49e48"));
        assertTrue(workflow.contains("name: jacoco-coverage-report"));
        assertTrue(workflow.contains("name: packaged-artifact-smoke"));
        assertTrue(workflow.contains("name: loadbalancerpro-sbom"));
    }

    @Test
    void ciWorkflowDoesNotAddReleasePublishingOrReleaseDownloadMutation() throws Exception {
        String workflow = read(CI_WORKFLOW).toLowerCase(Locale.ROOT);

        assertFalse(workflow.contains("gh release"), "CI workflow must not publish GitHub releases");
        assertFalse(workflow.contains("git tag"), "CI workflow must not create tags");
        assertFalse(workflow.contains("release-downloads"), "CI workflow must not mutate release-downloads");
        assertFalse(workflow.contains("softprops/action-gh-release"), "CI workflow must not upload release assets");
        assertFalse(workflow.contains("actions/create-release"), "CI workflow must not create releases");
        assertFalse(workflow.contains("upload-release-asset"), "CI workflow must not upload release assets");
        assertFalse(workflow.contains("new cloudmanager"), "CI workflow must not construct CloudManager");
        assertFalse(workflow.contains("cloudmanager("), "CI workflow must not construct CloudManager");
    }

    @Test
    void releaseWorkflowPublishingRemainsTagOrExplicitDispatchGated() throws Exception {
        String workflow = read(RELEASE_WORKFLOW);

        assertTrue(workflow.contains("tags:"));
        assertTrue(workflow.contains("'v*.*.*'"));
        assertTrue(workflow.contains("workflow_dispatch:"));
        assertTrue(workflow.contains("RELEASE_PUBLISH=false"));
        assertTrue(workflow.contains("if: steps.resolve-release.outputs.release_publish == 'true'"));
        assertFalse(workflow.contains("release-downloads"), "release workflow must not mutate manual release-downloads evidence");
    }

    @Test
    void proxyDefaultsAndReloadApiKeyBoundaryRemainVisible() throws Exception {
        String defaults = read(DEFAULT_PROPERTIES);
        String security = read(API_SECURITY);

        assertTrue(defaults.contains("loadbalancerpro.proxy.enabled=false"));
        assertFalse(defaults.contains("loadbalancerpro.proxy.enabled=true"));
        assertTrue(security.contains("HttpMethod.POST, \"/api/proxy/reload\""));
        assertTrue(security.contains("hasRole(allocationRole)"));
    }

    @Test
    void ciWorkflowAvoidsInflatedClaims() throws Exception {
        String workflow = read(CI_WORKFLOW).toLowerCase(Locale.ROOT);

        assertFalse(workflow.contains("production-grade"));
        assertFalse(workflow.contains("benchmark result"));
        assertFalse(workflow.contains("certification proof"));
        assertFalse(workflow.contains("certified gateway"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
