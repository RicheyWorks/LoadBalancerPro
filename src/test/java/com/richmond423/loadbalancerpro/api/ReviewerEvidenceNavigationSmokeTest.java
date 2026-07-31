package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ReviewerEvidenceNavigationSmokeTest {
    private static final Path STATIC_ROOT = Path.of("src/main/resources/static");
    private static final Path VIEWER = STATIC_ROOT.resolve("evidence-viewer.html");
    private static final Path VIEWER_LIBRARY =
            STATIC_ROOT.resolve("evidence-viewer-lib.js");
    private static final Pattern HREF = Pattern.compile("href=[\"']([^\"'#?]+)");

    private static final List<String> REVIEWER_EVIDENCE_PATHS = List.of(
            "/enterprise-lab-reviewer.html",
            "/operator-evidence-dashboard.html",
            "/ci-evidence-gate.html",
            "/evidence-timeline.html",
            "/evidence-export-packet.html");

    private static final List<String> VIEW_KEYS = List.of(
            "reviewer",
            "operator",
            "gate",
            "timeline",
            "packet");

    private static final List<String> REVIEWER_ENTRY_PATHS = List.of(
            "/index.html",
            "/routing-demo.html");

    private static final List<String> REQUIRED_ENTRY_LINKS = List.of(
            "/enterprise-lab-reviewer.html",
            "/operator-evidence-dashboard.html",
            "/evidence-timeline.html",
            "/evidence-export-packet.html");

    @Test
    void reviewerEvidencePagesArePackagedStaticResources() {
        assertTrue(Files.exists(VIEWER), "shared evidence viewer should remain source-controlled");
        assertTrue(Files.exists(VIEWER_LIBRARY),
                "shared evidence viewer library should remain source-controlled");
        assertTrue(new ClassPathResource("static/evidence-viewer.html").exists(),
                "shared evidence viewer should be packaged as a static resource");
        assertTrue(new ClassPathResource("static/evidence-viewer-lib.js").exists(),
                "shared evidence viewer library should be packaged as a static resource");
        for (String page : REVIEWER_EVIDENCE_PATHS) {
            Path sourcePath = sourcePath(page);
            assertTrue(Files.exists(sourcePath), page + " should remain source-controlled");
            assertTrue(new ClassPathResource("static" + page).exists(),
                    page + " should be packaged as a static resource");
        }
    }

    @Test
    void sharedViewerBuildsTheSameOriginNavigationMesh() throws Exception {
        String library = read(VIEWER_LIBRARY);
        assertTrue(library.contains("Object.entries(VIEW_DEFINITIONS)"));
        assertTrue(library.contains(
                "link.href = `/evidence-viewer.html?view=${encodeURIComponent(key)}`"));

        for (String viewKey : VIEW_KEYS) {
            assertTrue(library.contains(viewKey + ": Object.freeze({"),
                    "shared viewer should define " + viewKey);
        }
    }

    @Test
    void reviewerEntryPagesLinkIntoEvidenceNavigationPath() throws Exception {
        for (String page : REVIEWER_ENTRY_PATHS) {
            Set<String> links = localHtmlLinks(read(sourcePath(page)));

            for (String expected : REQUIRED_ENTRY_LINKS) {
                assertTrue(links.contains(expected), page + " should link to " + expected);
            }
        }
    }

    @Test
    void reviewerEvidenceNavigationStaysLocalAndReadOnly() throws Exception {
        List<Path> viewerAssets = new java.util.ArrayList<>();
        viewerAssets.add(VIEWER);
        viewerAssets.add(VIEWER_LIBRARY);
        REVIEWER_EVIDENCE_PATHS.forEach(page -> viewerAssets.add(sourcePath(page)));

        for (Path asset : viewerAssets) {
            String content = read(asset);
            String normalized = content.toLowerCase(Locale.ROOT);
            Set<String> links = localHtmlLinks(content);

            assertFalse(links.stream().anyMatch(link -> link.startsWith("http://") || link.startsWith("https://")
                    || link.startsWith("//")), asset + " should not add external navigation");

            for (String unsafe : List.of(
                    "docker push",
                    "docker login",
                    "cosign sign",
                    "cosign attest",
                    "gh release",
                    "git tag",
                    "production certified gateway",
                    "enterprise production ready",
                    "live cloud validated",
                    "real tenant proof complete",
                    "signed container published",
                    "registry publish complete",
                    "container signing complete",
                    "governance settings applied: true",
                    "governance settings applied: yes",
                    "governance-applied proof complete")) {
                assertFalse(normalized.contains(unsafe), asset + " must not include " + unsafe);
            }
        }
    }

    private static Set<String> localHtmlLinks(String html) {
        Matcher matcher = HREF.matcher(html);
        Set<String> links = new LinkedHashSet<>();
        while (matcher.find()) {
            links.add(matcher.group(1));
        }
        return links;
    }

    private static Path sourcePath(String page) {
        String fileName = page.substring(1);
        return STATIC_ROOT.resolve(fileName);
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
