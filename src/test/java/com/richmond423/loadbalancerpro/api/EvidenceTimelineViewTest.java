package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EvidenceTimelineViewTest {
    private static final Path TIMELINE =
            Path.of("src/main/resources/static/evidence-timeline.html");
    private static final Path OPERATOR_DASHBOARD =
            Path.of("src/main/resources/static/operator-evidence-dashboard.html");
    private static final Path REVIEWER_DASHBOARD =
            Path.of("src/main/resources/static/enterprise-lab-reviewer.html");
    private static final Path INDEX = Path.of("src/main/resources/static/index.html");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void evidenceTimelinePageExistsOnClasspathAndIsServed() throws Exception {
        assertTrue(Files.exists(TIMELINE), "evidence timeline should be source-controlled");
        assertTrue(new ClassPathResource("static/evidence-timeline.html").exists(),
                "evidence timeline should be packaged as a static resource");

        mockMvc.perform(get("/evidence-timeline.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Evidence Timeline / History View")))
                .andExpect(content().string(containsString("Local/CI evidence timeline")))
                .andExpect(content().string(containsString("Not production certified")))
                .andExpect(content().string(containsString("Not enterprise-production ready")));
    }

    @Test
    void evidenceTimelineDocumentsStagesPathsTemplateAndBoundaries() throws Exception {
        String page = read(TIMELINE);

        for (String expected : List.of(
                "Evidence Timeline / History View",
                "/api/enterprise-lab/evidence-timeline",
                "target/enterprise-lab-runs/",
                "target/container-dry-run-evidence/",
                "container-dry-run-evidence-no-publish-no-sign",
                "/operator-evidence-dashboard.html",
                "/enterprise-lab-reviewer.html",
                "/api/enterprise-lab/operator-evidence-summary",
                "/api/enterprise-lab/reviewer-summary",
                "docs/REVIEWER_TRUST_MAP.md",
                "docs/ENTERPRISE_READINESS_AUDIT.md",
                "run label",
                "commit SHA",
                "not production certified",
                "not enterprise-production ready",
                "no registry publish",
                "no container signing",
                "generated evidence should not be committed")) {
            assertTrue(page.contains(expected), "evidence timeline should mention " + expected);
        }
    }

    @Test
    void evidenceTimelineApiReturnsDeterministicLocalTimelineMetadata() throws Exception {
        String response = mockMvc.perform(get("/api/enterprise-lab/evidence-timeline"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dashboardPath", is("/evidence-timeline.html")))
                .andExpect(jsonPath("$.operatorDashboardPath", is("/operator-evidence-dashboard.html")))
                .andExpect(jsonPath("$.reviewerDashboardPath", is("/enterprise-lab-reviewer.html")))
                .andExpect(jsonPath("$.operatorEvidenceSummaryApi",
                        is("/api/enterprise-lab/operator-evidence-summary")))
                .andExpect(jsonPath("$.reviewerSummaryApi", is("/api/enterprise-lab/reviewer-summary")))
                .andExpect(jsonPath("$.evidenceStages[0].label", is("Source readiness docs")))
                .andExpect(jsonPath("$.evidenceStages[7].label",
                        is("Future gated publish/signing evidence")))
                .andExpect(jsonPath("$.evidencePaths.enterpriseLabRuns", is("target/enterprise-lab-runs/")))
                .andExpect(jsonPath("$.evidencePaths.containerDryRunEvidence",
                        is("target/container-dry-run-evidence/")))
                .andExpect(jsonPath("$.ciArtifact.name",
                        is("container-dry-run-evidence-no-publish-no-sign")))
                .andExpect(jsonPath("$.runTemplateFields[0]", is("run label")))
                .andExpect(jsonPath("$.runTemplateFields[1]", is("commit SHA")))
                .andExpect(jsonPath("$.proves[0]", is("evidence categories are organized")))
                .andExpect(jsonPath("$.doesNotProve[0]", is("production certification")))
                .andExpect(jsonPath("$.safetyBoundaries[0]",
                        is("local app does not claim real-time GitHub status")))
                .andReturn().getResponse().getContentAsString().toLowerCase(Locale.ROOT);

        for (String unsafe : List.of(
                "production certified gateway",
                "enterprise production ready",
                "live cloud validated",
                "real tenant proof complete",
                "signed container published",
                "registry publish complete",
                "container signing complete",
                "governance settings applied",
                "docker push",
                "docker login",
                "cosign sign",
                "cosign attest",
                "gh release",
                "git tag")) {
            assertFalse(response.contains(unsafe), "evidence timeline API must not include " + unsafe);
        }
    }

    @Test
    void evidenceTimelineAvoidsUnsafeCommandsAndExternalDependencies() throws Exception {
        String normalized = read(TIMELINE).toLowerCase(Locale.ROOT);

        for (String prohibited : List.of(
                "docker push",
                "docker login",
                "cosign sign",
                "cosign attest",
                "gh release",
                "git tag",
                "aws ",
                "az ",
                "gcloud",
                "kubectl",
                "terraform",
                "pulumi",
                "helm",
                "aws_access_key_id",
                "aws_secret_access_key",
                "github_token",
                "cosign_private_key",
                "localstorage",
                "sessionstorage",
                "cdn.",
                "http://",
                "https://")) {
            assertFalse(normalized.contains(prohibited), "evidence timeline must not include " + prohibited);
        }

        assertTrue(normalized.contains("fetch(\"/api/enterprise-lab/evidence-timeline\""),
                "evidence timeline should fetch the same-origin local summary endpoint");
    }

    @Test
    void evidenceTimelineIsLinkedFromDashboardsReadmeAndTrustMap() throws Exception {
        assertTrue(read(INDEX).contains("/evidence-timeline.html"));
        assertTrue(read(OPERATOR_DASHBOARD).contains("/evidence-timeline.html"));
        assertTrue(read(REVIEWER_DASHBOARD).contains("/evidence-timeline.html"));
        assertTrue(read(README).contains("http://localhost:8080/evidence-timeline.html"));
        assertTrue(read(TRUST_MAP).contains("/evidence-timeline.html"));
        assertTrue(read(TRUST_MAP).contains("/api/enterprise-lab/evidence-timeline"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
