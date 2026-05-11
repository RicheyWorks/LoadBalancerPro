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

class CockpitDiscoverabilityDocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Path INDEX_PAGE = Path.of("src/main/resources/static/index.html");
    private static final Path COCKPIT_PAGE = Path.of("src/main/resources/static/load-balancing-cockpit.html");
    private static final Path ROUTING_DEMO_PAGE = Path.of("src/main/resources/static/routing-demo.html");
    private static final Path THIS_TEST = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/api/CockpitDiscoverabilityDocumentationTest.java");

    private static final List<Path> CLOUD_MANAGER_SCAN_FILES = List.of(README, INDEX_PAGE, THIS_TEST);
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+" + "CloudManager\\s*\\(|" + "CloudManager\\s*\\(");

    @Test
    void rootIndexPageExistsAndLinksToCockpitAndRuntimeReviewPaths() throws Exception {
        assertTrue(Files.exists(INDEX_PAGE), "root static landing page should exist");
        assertTrue(Files.exists(COCKPIT_PAGE), "existing cockpit page should remain present");

        String index = read(INDEX_PAGE);
        assertTrue(index.contains("/load-balancing-cockpit.html"), "index should link to cockpit");
        assertTrue(index.contains("/api/health"), "index should link to API health");
        assertTrue(index.contains("/actuator/health/readiness"), "index should link to readiness");

        if (Files.exists(ROUTING_DEMO_PAGE)) {
            assertTrue(index.contains("/routing-demo.html"), "index should link to routing demo when present");
        }
    }

    @Test
    void readmeMakesCockpitAndReviewerMapEasyToFind() throws Exception {
        String readme = read(README);

        assertTrue(readme.contains("## Try the Web Cockpit"));
        assertTrue(readme.contains("mvn spring-boot:run"));
        assertTrue(readme.contains("http://localhost:8080/"));
        assertTrue(readme.contains("http://localhost:8080/load-balancing-cockpit.html"));
        assertTrue(readme.contains("docs/REVIEWER_TRUST_MAP.md"));

        if (Files.exists(ROUTING_DEMO_PAGE)) {
            assertTrue(readme.contains("http://localhost:8080/routing-demo.html"));
        }
    }

    @Test
    void discoverabilityTextKeepsScopeHonestWithoutUnsafeClaims() throws Exception {
        for (String text : List.of(readTryWebCockpitSection(), read(INDEX_PAGE))) {
            String normalized = text.toLowerCase(Locale.ROOT);

            assertFalse(normalized.contains("production-grade gateway"), "must not add gateway claims");
            assertFalse(normalized.contains("production-ready gateway"), "must not add gateway claims");
            assertFalse(normalized.contains("benchmark proof"), "must not add benchmark claims");
            assertFalse(normalized.contains("benchmark result"), "must not add benchmark claims");
            assertFalse(normalized.contains("certification proof"), "must not add certification claims");
            assertFalse(normalized.contains("certified gateway"), "must not add certification claims");
            assertFalse(normalized.contains("gh release create"), "must not instruct release creation");
            assertFalse(normalized.contains("gh release upload"), "must not instruct asset uploads");
            assertFalse(normalized.contains("git tag -"), "must not instruct tag creation");
            assertFalse(normalized.contains("create release"), "must not instruct release creation");
            assertFalse(normalized.contains("create tag"), "must not instruct tag creation");
        }
    }

    @Test
    void discoverabilitySprintAddsNoCloudManagerConstruction() throws Exception {
        for (Path file : CLOUD_MANAGER_SCAN_FILES) {
            assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(read(file)).find(),
                    file + " must not construct CloudManager");
        }
    }

    private static String readTryWebCockpitSection() throws IOException {
        String readme = read(README);
        int start = readme.indexOf("## Try the Web Cockpit");
        int end = readme.indexOf("## What This Project Demonstrates");
        assertTrue(start >= 0, "README should contain Try the Web Cockpit section");
        assertTrue(end > start, "README should keep cockpit section before project details");
        return readme.substring(start, end);
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
