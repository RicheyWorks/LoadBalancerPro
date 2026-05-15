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
class OperatorEvidenceDashboardTest {
    private static final Path DASHBOARD =
            Path.of("src/main/resources/static/operator-evidence-dashboard.html");
    private static final Path REVIEWER_DASHBOARD =
            Path.of("src/main/resources/static/enterprise-lab-reviewer.html");
    private static final Path INDEX = Path.of("src/main/resources/static/index.html");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void operatorEvidenceDashboardPageExistsOnClasspathAndIsServed() throws Exception {
        assertTrue(Files.exists(DASHBOARD), "operator evidence dashboard should be source-controlled");
        assertTrue(new ClassPathResource("static/operator-evidence-dashboard.html").exists(),
                "operator evidence dashboard should be packaged as a static resource");

        mockMvc.perform(get("/operator-evidence-dashboard.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Operator Evidence Dashboard")))
                .andExpect(content().string(containsString("Local/CI evidence only")))
                .andExpect(content().string(containsString("Not production certified")))
                .andExpect(content().string(containsString("Not enterprise-production ready")));
    }

    @Test
    void operatorEvidenceDashboardOrganizesEvidencePathsAndBoundaries() throws Exception {
        String page = read(DASHBOARD);

        for (String expected : List.of(
                "/api/enterprise-lab/operator-evidence-summary",
                "target/enterprise-lab-runs/",
                "container-dry-run-evidence-no-publish-no-sign",
                "target/container-dry-run-evidence/",
                "/enterprise-lab-reviewer.html",
                "/api/enterprise-lab/reviewer-summary",
                ".github/CODEOWNERS",
                "docs/MANUAL_GITHUB_GOVERNANCE_HARDENING.md",
                "mvn -q test",
                "mvn -q -DskipTests package",
                "mvn -B package",
                "git diff --check",
                "scripts/smoke/enterprise-lab-workflow.ps1 -Package",
                "not production certified",
                "not enterprise-production ready",
                "no registry publish",
                "no container signing")) {
            assertTrue(page.contains(expected), "operator dashboard should mention " + expected);
        }
    }

    @Test
    void operatorEvidenceSummaryApiReturnsDeterministicLocalEvidenceMetadata() throws Exception {
        String response = mockMvc.perform(get("/api/enterprise-lab/operator-evidence-summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dashboardPath", is("/operator-evidence-dashboard.html")))
                .andExpect(jsonPath("$.reviewerDashboardPath", is("/enterprise-lab-reviewer.html")))
                .andExpect(jsonPath("$.reviewerSummaryApi", is("/api/enterprise-lab/reviewer-summary")))
                .andExpect(jsonPath("$.evidencePaths.enterpriseLabRuns", is("target/enterprise-lab-runs/")))
                .andExpect(jsonPath("$.evidencePaths.containerDryRunEvidence",
                        is("target/container-dry-run-evidence/")))
                .andExpect(jsonPath("$.ciArtifact.name",
                        is("container-dry-run-evidence-no-publish-no-sign")))
                .andExpect(jsonPath("$.ciArtifact.evidenceDirectory",
                        is("target/container-dry-run-evidence/")))
                .andExpect(jsonPath("$.commands[0]", is("mvn -q test")))
                .andExpect(jsonPath("$.commands[1]", is("mvn -q -DskipTests package")))
                .andExpect(jsonPath("$.commands[2]", is("mvn -B package")))
                .andExpect(jsonPath("$.commands[3]", is("git diff --check")))
                .andExpect(jsonPath("$.commands[4]",
                        is("powershell -NoProfile -ExecutionPolicy Bypass -File .\\scripts\\smoke\\enterprise-lab-workflow.ps1 -Package")))
                .andExpect(jsonPath("$.proves[0]",
                        is("local tests, package build, and Enterprise Lab smoke can pass")))
                .andExpect(jsonPath("$.doesNotProve[0]", is("production certification")))
                .andExpect(jsonPath("$.safetyBoundaries[0]",
                        is("generated evidence under target/ remains ignored output")))
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
            assertFalse(response.contains(unsafe), "operator evidence API must not include " + unsafe);
        }
    }

    @Test
    void operatorEvidenceDashboardAvoidsUnsafeCommandsAndExternalDependencies() throws Exception {
        String normalized = read(DASHBOARD).toLowerCase(Locale.ROOT);

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
            assertFalse(normalized.contains(prohibited), "operator dashboard must not include " + prohibited);
        }

        assertTrue(normalized.contains("fetch(\"/api/enterprise-lab/operator-evidence-summary\""),
                "operator dashboard should fetch the same-origin local summary endpoint");
    }

    @Test
    void operatorEvidenceDashboardIsLinkedFromRootReviewerReadmeAndTrustMap() throws Exception {
        assertTrue(read(INDEX).contains("/operator-evidence-dashboard.html"));
        assertTrue(read(REVIEWER_DASHBOARD).contains("/operator-evidence-dashboard.html"));
        assertTrue(read(README).contains("http://localhost:8080/operator-evidence-dashboard.html"));
        assertTrue(read(TRUST_MAP).contains("/operator-evidence-dashboard.html"));
        assertTrue(read(TRUST_MAP).contains("/api/enterprise-lab/operator-evidence-summary"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
