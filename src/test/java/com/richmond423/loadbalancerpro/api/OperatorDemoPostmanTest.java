package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richmond423.loadbalancerpro.core.CloudManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@SpringBootTest
@AutoConfigureMockMvc
class OperatorDemoPostmanTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path PERFECT_FIXTURE =
            Path.of("src/test/resources/evidence-training-demo/perfect-scorecard-answers.json");
    private static final Path PARTIAL_FIXTURE =
            Path.of("src/test/resources/evidence-training-demo/partial-scorecard-answers.json");
    private static final Path FAILING_FIXTURE =
            Path.of("src/test/resources/evidence-training-demo/failing-scorecard-answers.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void operatorDemoGuideExistsAndMentionsSafetyLimitations() throws Exception {
        String guide = Files.readString(Path.of("docs/OPERATOR_DEMO_WALKTHROUGH.md"), StandardCharsets.UTF_8);
        String normalized = guide.toLowerCase(Locale.ROOT);

        assertTrue(guide.contains("Operator Evidence Training Demo Walkthrough"));
        assertTrue(guide.contains("curl -fsS http://127.0.0.1:8080/api/health"));
        assertTrue(guide.contains("curl -fsS http://127.0.0.1:8080/actuator/health/readiness"));
        assertTrue(guide.contains("/api/evidence-training/onboarding"));
        assertTrue(guide.contains("postman/LoadBalancerPro.postman_collection.json"));
        assertTrue(guide.contains("http://localhost:8080/evidence-training-demo.html"));
        assertTrue(guide.contains("Evidence Training Demo Walkthrough"));
        assertTrue(guide.contains("src/test/resources/evidence-training-demo/perfect-scorecard-answers.json"));
        assertTrue(guide.contains("src/test/resources/evidence-training-demo/partial-scorecard-answers.json"));
        assertTrue(guide.contains("src/test/resources/evidence-training-demo/failing-scorecard-answers.json"));
        assertTrue(normalized.contains("local/operator training aid only"));
        assertTrue(normalized.contains("not certification"));
        assertTrue(normalized.contains("not legal compliance proof"));
        assertTrue(normalized.contains("not identity proof"));
        assertTrue(normalized.contains("no cloud mutation"));
        assertTrue(normalized.contains("no `cloudmanager` required"));
        assertTrue(normalized.contains("api server is required for browser/postman demo but not for offline cli workflows"));
    }

    @Test
    void demoFixturesAreValidJsonAndUsePackagedScorecards() throws Exception {
        JsonNode perfect = readJson(PERFECT_FIXTURE);
        JsonNode partial = readJson(PARTIAL_FIXTURE);
        JsonNode failing = readJson(FAILING_FIXTURE);

        assertEquals("operator-perfect-demo", perfect.path("operator").asText());
        assertEquals(7, perfect.path("answers").size());
        assertEquals("audit-append-warn", perfect.at("/answers/0/exerciseName").asText());
        assertEquals("strict-zero-drift-pass", perfect.at("/answers/6/exerciseName").asText());

        assertEquals("operator-partial-demo", partial.path("operator").asText());
        assertEquals(1, partial.path("answers").size());
        assertEquals("audit-append-warn", partial.at("/answers/0/exerciseName").asText());

        assertEquals("operator-failing-demo", failing.path("operator").asText());
        assertEquals(1, failing.path("answers").size());
        assertEquals("strict-zero-drift-pass", failing.at("/answers/0/exerciseName").asText());
    }

    @Test
    void perfectPartialAndFailingFixturesGradeDeterministicallyWithoutCloudManager() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(get("/api/evidence-training/onboarding"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cloudMutation", is(false)))
                    .andExpect(jsonPath("$.cloudManagerRequired", is(false)));

            assertDeterministicGrade(PERFECT_FIXTURE, 70, 70, 100.0, true, "operator-perfect-demo");
            assertDeterministicGrade(PARTIAL_FIXTURE, 7, 70, 10.0, false, "operator-partial-demo");
            assertDeterministicGrade(FAILING_FIXTURE, 0, 70, 0.0, false, "operator-failing-demo");

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "operator demo onboarding and grading paths must not construct CloudManager");
        }
    }

    @Test
    void postmanCollectionJsonIsValidAndContainsDemoWalkthroughFolder() throws Exception {
        JsonNode collection = readJson(Path.of("postman/LoadBalancerPro.postman_collection.json"));
        assertEquals("LoadBalancerPro", collection.at("/info/name").asText());
        assertEquals("http://localhost:8080", collection.at("/variable/0/value").asText());

        JsonNode folder = findFolder(collection, "Evidence Training Demo Walkthrough");
        assertNotNull(folder, "Postman collection should include the operator demo walkthrough folder");
        assertEquals(11, folder.path("item").size());

        List<String> expectedNames = List.of(
                "GET Health Check",
                "GET Readiness Check",
                "GET Evidence Training Onboarding",
                "GET Demo Policy Templates",
                "GET Demo Policy Examples",
                "GET Demo Training Scorecards",
                "GET Strict Zero Drift Pass Scorecard",
                "GET Strict Zero Drift Pass Answer Template",
                "POST Grade Perfect Demo Sample",
                "POST Grade Partial Demo Sample",
                "POST Grade Failing Demo Sample");
        for (int i = 0; i < expectedNames.size(); i++) {
            assertEquals(expectedNames.get(i), folder.at("/item/" + i + "/name").asText());
        }

        assertEquals("{{baseUrl}}/actuator/health/readiness",
                folder.at("/item/1/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/evidence-training/scorecards/strict-zero-drift-pass",
                folder.at("/item/6/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/evidence-training/scorecards/strict-zero-drift-pass/answer-template",
                folder.at("/item/7/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/evidence-training/scorecards/grade",
                folder.at("/item/8/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/evidence-training/scorecards/grade",
                folder.at("/item/9/request/url/raw").asText());
        assertEquals("{{baseUrl}}/api/evidence-training/scorecards/grade",
                folder.at("/item/10/request/url/raw").asText());
        assertTrue(folder.at("/item/8/request/body/raw").asText().contains("operator-perfect-demo"));
        assertTrue(folder.at("/item/9/request/body/raw").asText().contains("operator-partial-demo"));
        assertTrue(folder.at("/item/10/request/body/raw").asText().contains("operator-failing-demo"));
    }

    @Test
    void demoAddsBrowserPageWithoutGeneratedRuntimeReports() {
        assertTrue(Files.exists(Path.of("src/main/resources/static/evidence-training-demo.html")),
                "operator demos should include the no-dependency browser page");
        assertFalse(Files.exists(Path.of("docs/examples/evidence-training-demo/scorecard-report.md")));
        assertFalse(Files.exists(Path.of("docs/examples/evidence-training-demo/scorecard-report.json")));
    }

    private void assertDeterministicGrade(
            Path fixture,
            int totalScore,
            int maxScore,
            double percent,
            boolean passed,
            String operator) throws Exception {
        String body = Files.readString(fixture, StandardCharsets.UTF_8);
        String first = mockMvc.perform(post("/api/evidence-training/scorecards/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operator", is(operator)))
                .andExpect(jsonPath("$.totalExercises", is(7)))
                .andExpect(jsonPath("$.totalScore", is(totalScore)))
                .andExpect(jsonPath("$.maxScore", is(maxScore)))
                .andExpect(jsonPath("$.percent", is(percent)))
                .andExpect(jsonPath("$.passed", is(passed)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String second = mockMvc.perform(post("/api/evidence-training/scorecards/grade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(first, second, "demo fixture grading should be deterministic");
    }

    private static JsonNode readJson(Path path) throws Exception {
        return OBJECT_MAPPER.readTree(Files.readString(path, StandardCharsets.UTF_8));
    }

    private static JsonNode findFolder(JsonNode collection, String folderName) {
        for (JsonNode item : collection.path("item")) {
            if (folderName.equals(item.path("name").asText())) {
                return item;
            }
        }
        return null;
    }
}
