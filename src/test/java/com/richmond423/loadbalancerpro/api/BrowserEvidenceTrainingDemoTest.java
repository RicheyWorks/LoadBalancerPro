package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import java.util.Locale;

@SpringBootTest
@AutoConfigureMockMvc
class BrowserEvidenceTrainingDemoTest {
    private static final Path BROWSER_DEMO_PAGE =
            Path.of("src/main/resources/static/evidence-training-demo.html");
    private static final Path PERFECT_FIXTURE =
            Path.of("src/test/resources/evidence-training-demo/perfect-scorecard-answers.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void staticBrowserDemoPageExistsAndIsServed() throws Exception {
        assertTrue(Files.exists(BROWSER_DEMO_PAGE), "browser demo page should be source-controlled");

        mockMvc.perform(get("/evidence-training-demo.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("LoadBalancerPro Evidence Training Demo")))
                .andExpect(content().string(containsString("data-action=\"health\"")))
                .andExpect(content().string(containsString("data-action=\"grade-perfect\"")));
    }

    @Test
    void staticBrowserDemoPageContainsExpectedEndpointPathsAndSamples() throws Exception {
        String page = Files.readString(BROWSER_DEMO_PAGE, StandardCharsets.UTF_8);

        assertTrue(page.contains("/api/health"));
        assertTrue(page.contains("/actuator/health/readiness"));
        assertTrue(page.contains("/api/evidence-training/onboarding"));
        assertTrue(page.contains("/api/evidence-training/templates"));
        assertTrue(page.contains("/api/evidence-training/examples"));
        assertTrue(page.contains("/api/evidence-training/scorecards"));
        assertTrue(page.contains("/api/evidence-training/scorecards/grade"));
        assertTrue(page.contains("strict-zero-drift-pass"));
        assertTrue(page.contains("operator-perfect-demo"));
        assertTrue(page.contains("operator-partial-demo"));
        assertTrue(page.contains("operator-failing-demo"));
        assertTrue(page.contains("Grade perfect sample"));
        assertTrue(page.contains("Grade partial sample"));
        assertTrue(page.contains("Grade failing sample"));
    }

    @Test
    void staticBrowserDemoPageContainsSafetyLimitations() throws Exception {
        String page = Files.readString(BROWSER_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("local/operator training aid only"));
        assertTrue(normalized.contains("not certification"));
        assertTrue(normalized.contains("not legal compliance proof"));
        assertTrue(normalized.contains("not identity proof"));
        assertTrue(normalized.contains("no cloud mutation"));
        assertTrue(normalized.contains("no cloudmanager required for training/onboarding demo"));
        assertTrue(normalized.contains("api server is required for browser/postman demo but not for offline cli workflows"));
        assertTrue(normalized.contains("no external scripts, styles, fonts, cdns, services, secrets, or admin controls"));
    }

    @Test
    void staticBrowserDemoPageHasNoExternalScriptsCdnsOrAuthPlaceholders() throws Exception {
        String page = Files.readString(BROWSER_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertFalse(normalized.contains("<script src="));
        assertFalse(normalized.contains("<link rel=\"stylesheet\""));
        assertFalse(normalized.contains("cdn."));
        assertFalse(normalized.contains("fonts.googleapis"));
        assertFalse(normalized.contains("fonts.gstatic"));
        assertFalse(normalized.contains("x-api-key"));
        assertFalse(normalized.contains("bearer"));
        assertFalse(normalized.contains("authorization"));
        assertFalse(normalized.contains("password"));
        assertFalse(normalized.contains("access key"));
        assertFalse(normalized.contains("secret key"));
        assertFalse(normalized.contains("release-artifacts"));
        assertFalse(normalized.contains("/rulesets"));
        assertFalse(normalized.contains("/repos/"));
        assertFalse(normalized.contains("cloud.livemode"));
        assertFalse(normalized.contains("admin/"));
    }

    @Test
    void staticBrowserDemoPageHasNoAdminReleaseRulesetOrCloudMutationControls() throws Exception {
        String page = Files.readString(BROWSER_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertFalse(normalized.contains("data-action=\"admin"));
        assertFalse(normalized.contains("data-action=\"release"));
        assertFalse(normalized.contains("data-action=\"ruleset"));
        assertFalse(normalized.contains("data-action=\"cloud"));
        assertFalse(normalized.contains("delete-branch"));
        assertFalse(normalized.contains("create release"));
        assertFalse(normalized.contains("create tag"));
        assertFalse(normalized.contains("mutate cloud"));
    }

    @Test
    void browserDemoAndOnboardingPathsDoNotConstructCloudManagerOrMutateCloud() throws Exception {
        try (MockedConstruction<CloudManager> mockedCloudManager =
                Mockito.mockConstruction(CloudManager.class)) {
            mockMvc.perform(get("/evidence-training-demo.html"))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/evidence-training/onboarding"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.readOnly", is(true)))
                    .andExpect(jsonPath("$.cloudMutation", is(false)))
                    .andExpect(jsonPath("$.cloudManagerRequired", is(false)));
            mockMvc.perform(post("/api/evidence-training/scorecards/grade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Files.readString(PERFECT_FIXTURE, StandardCharsets.UTF_8)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalScore", is(70)))
                    .andExpect(jsonPath("$.percent", is(100.0)));

            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "browser demo, onboarding, and grading paths must not construct CloudManager");
        }
    }
}
