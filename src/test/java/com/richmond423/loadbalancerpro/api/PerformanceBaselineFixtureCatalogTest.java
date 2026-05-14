package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PerformanceBaselineFixtureCatalogTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path FIXTURES = Path.of("docs/performance/performance-fixtures.json");
    private static final Path THRESHOLDS = Path.of("docs/performance/performance-thresholds.example.json");
    private static final Path SCRIPT = Path.of("scripts/smoke/performance-baseline.ps1");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fixtureCatalogHasStableIdsAndLoopbackOnlySafety() throws Exception {
        JsonNode catalog = readJson(FIXTURES);
        List<String> ids = StreamSupport.stream(catalog.path("fixtures").spliterator(), false)
                .map(fixture -> fixture.path("id").asText())
                .toList();

        assertEquals(List.of(
                "health-status",
                "allocation-evaluation",
                "routing-comparison",
                "lab-scenarios-list",
                "lab-run-small-shadow",
                "controlled-policy-status",
                "lab-metrics",
                "enterprise-lab-page"), ids);
        assertEquals(ids.size(), Set.copyOf(ids).size(), "performance fixture ids must be unique");
        assertEquals("local", catalog.path("profile").asText());
        assertTrue(catalog.path("safety").asText().contains("Loopback-only"));
        assertFalse(read(FIXTURES).contains("https://"));
    }

    @Test
    void deterministicFixturesReturnExpectedStatusInLocalProfile() throws Exception {
        JsonNode catalog = readJson(FIXTURES);
        for (JsonNode fixture : catalog.path("fixtures")) {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(
                    HttpMethod.valueOf(fixture.path("method").asText()), fixture.path("path").asText());
            if (fixture.has("body")) {
                request.contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsBytes(fixture.path("body")));
            }
            mockMvc.perform(request)
                    .andExpect(status().is(fixture.path("expectedStatus").asInt()));
        }
    }

    @Test
    void thresholdConfigIsWarningOnlyAndReferencesKnownFixtures() throws Exception {
        JsonNode catalog = readJson(FIXTURES);
        Set<String> fixtureIds = StreamSupport.stream(catalog.path("fixtures").spliterator(), false)
                .map(fixture -> fixture.path("id").asText())
                .collect(Collectors.toSet());
        JsonNode thresholds = readJson(THRESHOLDS);

        assertEquals("warning-only", thresholds.path("mode").asText());
        assertTrue(thresholds.path("safety").asText().contains("not production SLOs"));
        assertTrue(thresholds.path("defaults").path("p95WarningMillis").asDouble() > 0.0);
        for (JsonNode fixtureThreshold : thresholds.path("fixtures")) {
            assertTrue(fixtureIds.contains(fixtureThreshold.path("id").asText()),
                    "threshold fixture id should exist in catalog");
        }
    }

    @Test
    void performanceScriptWritesIgnoredTargetEvidenceAndAvoidsPublishCommands() throws Exception {
        String script = read(SCRIPT);
        String normalized = script.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "target/performance-baseline",
                "performance-report.json",
                "performance-dashboard.json",
                "performance-threshold-results.json",
                "performance-summary.md",
                "Assert-OutputUnderTarget",
                "Assert-NoSecretValues",
                "127.0.0.1",
                "local/package mode only")) {
            assertTrue(script.contains(expected), "performance script should mention " + expected);
        }

        for (String prohibited : List.of(
                "git clean",
                "git tag",
                "git push",
                "gh release create",
                "gh release upload",
                "gh release delete",
                "docker push",
                "cosign sign")) {
            assertFalse(normalized.contains(prohibited), "performance script must not include " + prohibited);
        }
    }

    private static JsonNode readJson(Path path) throws Exception {
        return OBJECT_MAPPER.readTree(read(path));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
