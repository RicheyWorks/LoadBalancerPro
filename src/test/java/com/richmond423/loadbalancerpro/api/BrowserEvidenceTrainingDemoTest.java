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
                .andExpect(content().string(containsString("LoadBalancerPro Evidence Training Cockpit")))
                .andExpect(content().string(containsString("data-action=\"health\"")))
                .andExpect(content().string(containsString("data-action=\"grade-perfect\"")))
                .andExpect(content().string(containsString("data-action=\"run-sequence\"")));
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
        assertTrue(page.contains("sequenceSteps"));
        assertTrue(page.contains("Evidence Training Browser Demo"));
        assertTrue(page.contains("local browser same-origin demo"));
    }

    @Test
    void staticBrowserDemoPageContainsRunSequenceControlsAndSummaryPreview() throws Exception {
        String page = Files.readString(BROWSER_DEMO_PAGE, StandardCharsets.UTF_8);

        assertTrue(page.contains("Run demo sequence"));
        assertTrue(page.contains("Stop demo"));
        assertTrue(page.contains("Reset demo"));
        assertTrue(page.contains("Copy demo summary"));
        assertTrue(page.contains("data-action=\"run-sequence\""));
        assertTrue(page.contains("data-action=\"stop-sequence\""));
        assertTrue(page.contains("data-action=\"reset-sequence\""));
        assertTrue(page.contains("data-copy-target=\"demo-summary-output\""));
        assertTrue(page.contains("Demo summary / transcript preview"));
        assertTrue(page.contains("id=\"demo-summary-output\""));
        assertTrue(page.contains("Client-side preview only. Nothing is written as a runtime report."));
        assertTrue(page.contains("runDemoSequence"));
        assertTrue(page.contains("stopDemoSequence"));
        assertTrue(page.contains("resetDemoSequence"));
    }

    @Test
    void staticBrowserDemoPageContainsGuidedChecklistAndSummaryCounters() throws Exception {
        String page = Files.readString(BROWSER_DEMO_PAGE, StandardCharsets.UTF_8);

        assertTrue(page.contains("Guided checklist"));
        assertTrue(page.contains("Health check"));
        assertTrue(page.contains("Readiness check"));
        assertTrue(page.contains("Load onboarding"));
        assertTrue(page.contains("List templates"));
        assertTrue(page.contains("List examples"));
        assertTrue(page.contains("List scorecards"));
        assertTrue(page.contains("Load scorecard detail"));
        assertTrue(page.contains("Load answer template"));
        assertTrue(page.contains("Grade perfect sample"));
        assertTrue(page.contains("Grade partial sample"));
        assertTrue(page.contains("Grade failing sample"));
        assertTrue(page.contains("data-step-status=\"health\""));
        assertTrue(page.contains("data-step-status=\"grade-failing\""));
        assertTrue(page.contains(">Not run<"));
        assertTrue(page.contains("NOT_RUN"));
        assertTrue(page.contains("PASSED"));
        assertTrue(page.contains("WARNING"));
        assertTrue(page.contains("FAILED"));
        assertTrue(page.contains("id=\"summary-total\""));
        assertTrue(page.contains("id=\"summary-passed\""));
        assertTrue(page.contains("id=\"summary-warning\""));
        assertTrue(page.contains("id=\"summary-failed\""));
        assertTrue(page.contains("id=\"summary-not-run\""));
    }

    @Test
    void staticBrowserDemoPageContainsCurlSnippetsAndPostmanParity() throws Exception {
        String page = Files.readString(BROWSER_DEMO_PAGE, StandardCharsets.UTF_8);

        assertTrue(page.contains("curl and Postman parity"));
        assertTrue(page.contains("data-copy-target=\"curl-health\""));
        assertTrue(page.contains("data-copy-target=\"curl-readiness\""));
        assertTrue(page.contains("data-copy-target=\"curl-onboarding\""));
        assertTrue(page.contains("data-copy-target=\"curl-templates\""));
        assertTrue(page.contains("data-copy-target=\"curl-examples\""));
        assertTrue(page.contains("data-copy-target=\"curl-scorecards\""));
        assertTrue(page.contains("data-copy-target=\"curl-scorecard-detail\""));
        assertTrue(page.contains("data-copy-target=\"curl-answer-template\""));
        assertTrue(page.contains("data-copy-target=\"curl-grading\""));
        assertTrue(page.contains("data-curl-path=\"/api/health\""));
        assertTrue(page.contains("data-curl-path=\"/actuator/health/readiness\""));
        assertTrue(page.contains("data-curl-path=\"/api/evidence-training/onboarding\""));
        assertTrue(page.contains("data-curl-path=\"/api/evidence-training/templates\""));
        assertTrue(page.contains("data-curl-path=\"/api/evidence-training/examples\""));
        assertTrue(page.contains("data-curl-path=\"/api/evidence-training/scorecards\""));
        assertTrue(page.contains("data-dynamic-curl=\"scorecard-detail\""));
        assertTrue(page.contains("data-dynamic-curl=\"answer-template\""));
        assertTrue(page.contains("data-curl-method=\"POST\""));
        assertTrue(page.contains("Postman parity: Evidence Training Demo Walkthrough"));
    }

    @Test
    void staticBrowserDemoPageContainsCopyablePayloadsAndGradingSummary() throws Exception {
        String page = Files.readString(BROWSER_DEMO_PAGE, StandardCharsets.UTF_8);

        assertTrue(page.contains("Grading visual summary"));
        assertTrue(page.contains("Visual result"));
        assertTrue(page.contains("Score percent"));
        assertTrue(page.contains("Passed boolean"));
        assertTrue(page.contains("Exercise count"));
        assertTrue(page.contains("id=\"grade-decisions\""));
        assertTrue(page.contains("Copy perfect payload"));
        assertTrue(page.contains("Copy partial payload"));
        assertTrue(page.contains("Copy failing payload"));
        assertTrue(page.contains("Copy health response"));
        assertTrue(page.contains("Copy onboarding response"));
        assertTrue(page.contains("Copy templates response"));
        assertTrue(page.contains("Copy examples response"));
        assertTrue(page.contains("Copy scorecards response"));
        assertTrue(page.contains("Copy scorecard response"));
        assertTrue(page.contains("Copy answer template response"));
        assertTrue(page.contains("Copy grading response"));
        assertTrue(page.contains("Export perfect payload"));
        assertTrue(page.contains("Export partial payload"));
        assertTrue(page.contains("Export failing payload"));
        assertTrue(page.contains("operator-perfect-demo"));
        assertTrue(page.contains("operator-partial-demo"));
        assertTrue(page.contains("operator-failing-demo"));
        assertTrue(page.contains("perfect-scorecard-answers.json"));
        assertTrue(page.contains("partial-scorecard-answers.json"));
        assertTrue(page.contains("failing-scorecard-answers.json"));
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
        assertTrue(normalized.contains("no external scripts/cdns"));
        assertTrue(normalized.contains("no external styles, fonts, images, services, or dependencies"));
        assertTrue(normalized.contains("no runtime report is written"));
        assertTrue(normalized.contains("no browser storage is used"));
        assertTrue(normalized.contains("no secrets, auth fields, admin controls, release controls, ruleset controls, or cloud mutation controls"));
    }

    @Test
    void staticBrowserDemoPageHasNoExternalScriptsCdnsFontsImagesStorageOrAuthPlaceholders() throws Exception {
        String page = Files.readString(BROWSER_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertFalse(normalized.contains("<script src="));
        assertFalse(normalized.contains("<link rel=\"stylesheet\""));
        assertFalse(normalized.contains("<img"));
        assertFalse(normalized.contains("background-image"));
        assertFalse(normalized.contains("cdn."));
        assertFalse(normalized.contains("fonts.googleapis"));
        assertFalse(normalized.contains("fonts.gstatic"));
        assertFalse(normalized.contains("localstorage"));
        assertFalse(normalized.contains("sessionstorage"));
        assertFalse(normalized.contains("x-api-key"));
        assertFalse(normalized.contains("bearer"));
        assertFalse(normalized.contains("authorization"));
        assertFalse(normalized.contains("type=\"password\""));
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
    void staticBrowserDemoPageHasDeterministicClientSideSummaryOnly() throws Exception {
        String page = Files.readString(BROWSER_DEMO_PAGE, StandardCharsets.UTF_8);
        String normalized = page.toLowerCase(Locale.ROOT);

        assertTrue(page.contains("demoName: Evidence Training Browser Demo"));
        assertTrue(page.contains("mode: local browser same-origin demo"));
        assertTrue(page.contains("no runtime report written"));
        assertTrue(page.contains("recordTranscriptStep"));
        assertTrue(page.contains("recordGradingTranscript"));
        assertFalse(normalized.contains("date.now"));
        assertFalse(normalized.contains("new date"));
        assertFalse(normalized.contains("math.random"));
        assertFalse(normalized.contains("randomuuid"));
        assertFalse(normalized.contains("crypto.randomuuid"));
        assertFalse(normalized.contains("timestamp"));
        assertFalse(normalized.contains("generated runtime reports"));
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
        assertFalse(normalized.contains("new cloudmanager"));
        assertFalse(normalized.contains("construct cloudmanager"));
        assertFalse(normalized.contains("certified operator"));
        assertFalse(normalized.contains("legal training compliance"));
        assertFalse(normalized.contains("identity verified"));
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
