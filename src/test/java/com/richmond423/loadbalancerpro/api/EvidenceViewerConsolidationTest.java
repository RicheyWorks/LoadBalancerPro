package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EvidenceViewerConsolidationTest {
    private static final Path STATIC_ROOT = Path.of("src/main/resources/static");
    private static final Path VIEWER = STATIC_ROOT.resolve("evidence-viewer.html");
    private static final Path LIBRARY = STATIC_ROOT.resolve("evidence-viewer-lib.js");
    private static final Path STYLES = STATIC_ROOT.resolve("evidence-viewer.css");
    private static final Map<String, String> LEGACY_VIEWS = legacyViews();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void consolidatedViewerAssetsArePackagedAndServed() throws Exception {
        mockMvc.perform(get("/evidence-viewer.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString(
                        "Content-Security-Policy")))
                .andExpect(content().string(containsString(
                        "src=\"/evidence-viewer-lib.js\"")))
                .andExpect(content().string(containsString(
                        "href=\"/evidence-viewer.css\"")))
                .andExpect(content().string(containsString("id=\"summary-grid\"")))
                .andExpect(content().string(containsString("id=\"detail-sections\"")));

        mockMvc.perform(get("/evidence-viewer-lib.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.valueOf("text/javascript")));

        mockMvc.perform(get("/evidence-viewer.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.valueOf("text/css")));
    }

    @Test
    void legacyUrlsRemainSmallPackagedCompatibilityRoutes() throws Exception {
        int totalLines = 0;
        for (Map.Entry<String, String> entry : LEGACY_VIEWS.entrySet()) {
            Path source = STATIC_ROOT.resolve(entry.getKey().substring(1));
            String page = read(source);
            totalLines += Files.readAllLines(source, StandardCharsets.UTF_8).size();

            assertTrue(page.contains("/evidence-viewer.html?view=" + entry.getValue()));
            assertFalse(page.contains("fetch("));
            assertFalse(page.contains("<style"));
            assertFalse(page.contains("<script"));

            mockMvc.perform(get(entry.getKey()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(content().string(containsString(
                            "/evidence-viewer.html?view=" + entry.getValue())));
        }
        assertTrue(totalLines <= 100,
                "legacy routes should remain compatibility shims, not duplicated viewers");
    }

    @Test
    void libraryAllowlistsFiveSameOriginReadOnlyEndpoints() throws Exception {
        String library = read(LIBRARY);
        List<String> endpoints = List.of(
                "/api/enterprise-lab/reviewer-summary",
                "/api/enterprise-lab/operator-evidence-summary",
                "/api/enterprise-lab/ci-evidence-gate-summary",
                "/api/enterprise-lab/evidence-timeline",
                "/api/enterprise-lab/evidence-export-packet");

        for (String endpoint : endpoints) {
            assertEquals(1, occurrences(library, "endpoint: \"" + endpoint + "\""),
                    "each fixed endpoint should have one view definition");
        }
        assertTrue(library.contains("hasOwn(VIEW_DEFINITIONS, requested)"));
        assertTrue(library.contains("credentials: \"same-origin\""));
        assertTrue(library.contains("method: \"GET\""));
        assertTrue(library.contains("cache: \"no-store\""));
    }

    @Test
    void rendererTreatsApiValuesAsTextAndExportsOnlyInBrowser() throws Exception {
        String viewer = read(VIEWER);
        String styles = read(STYLES);
        String library = read(LIBRARY);
        String normalized = (viewer + styles + library).toLowerCase();

        assertTrue(library.contains("element.textContent = text"));
        assertTrue(library.contains("JSON.stringify(payload, null, 2)"));
        assertTrue(library.contains("new Blob("));
        assertTrue(library.contains("URL.createObjectURL"));
        assertTrue(library.contains("URL.revokeObjectURL"));
        assertTrue(library.contains("window.print()"));
        assertTrue(library.contains("navigator.clipboard"));
        assertTrue(styles.contains("@media print"));

        for (String prohibited : List.of(
                "innerhtml",
                "insertadjacenthtml",
                "eval(",
                "new function",
                "localstorage",
                "sessionstorage",
                "sendbeacon",
                "navigator.share",
                "http://",
                "https://",
                "websocket",
                "eventsource")) {
            assertFalse(normalized.contains(prohibited),
                    "viewer assets must not include unsafe capability: " + prohibited);
        }
    }

    private static Map<String, String> legacyViews() {
        Map<String, String> views = new LinkedHashMap<>();
        views.put("/enterprise-lab-reviewer.html", "reviewer");
        views.put("/operator-evidence-dashboard.html", "operator");
        views.put("/ci-evidence-gate.html", "gate");
        views.put("/evidence-timeline.html", "timeline");
        views.put("/evidence-export-packet.html", "packet");
        return Map.copyOf(views);
    }

    private static int occurrences(String source, String token) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
