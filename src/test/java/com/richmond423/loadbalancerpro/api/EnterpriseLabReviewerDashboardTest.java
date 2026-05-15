package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class EnterpriseLabReviewerDashboardTest {
    private static final Path DASHBOARD =
            Path.of("src/main/resources/static/enterprise-lab-reviewer.html");
    private static final Path INDEX = Path.of("src/main/resources/static/index.html");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reviewerDashboardPageExistsOnClasspathAndIsServed() throws Exception {
        assertTrue(Files.exists(DASHBOARD), "reviewer dashboard should be source-controlled");
        assertTrue(new ClassPathResource("static/enterprise-lab-reviewer.html").exists(),
                "reviewer dashboard should be packaged as a static resource");

        mockMvc.perform(get("/enterprise-lab-reviewer.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Enterprise Lab Reviewer Dashboard")))
                .andExpect(content().string(containsString("Enterprise Lab ready")))
                .andExpect(content().string(containsString("Not production certified")))
                .andExpect(content().string(containsString("Not enterprise-production ready")));
    }

    @Test
    void reviewerDashboardSummarizesEvidenceGovernanceAndArtifactBoundaries() throws Exception {
        String page = read(DASHBOARD);

        for (String expected : List.of(
                "container-dry-run-evidence-no-publish-no-sign",
                "target/container-dry-run-evidence/",
                "docs/ENTERPRISE_READINESS_AUDIT.md",
                "docs/REVIEWER_TRUST_MAP.md",
                "docs/PRODUCTION_READINESS_SUMMARY.md",
                "evidence/SECURITY_POSTURE.md",
                "docs/MANUAL_GITHUB_GOVERNANCE_HARDENING.md",
                "docs/CONTAINER_SIGNING_DRY_RUN_VERIFICATION_LANE.md",
                "docs/CONTAINER_DISTRIBUTION_SIGNING_EVIDENCE_LANE.md",
                "Maven tests pass in CI",
                "CI and CodeQL are required and passing on main",
                "Docker build/runtime smoke and Trivy run in CI",
                "CODEOWNERS file exists",
                "repo files alone do not apply them",
                "mvn -q test",
                "mvn -q -DskipTests package",
                "mvn -B package",
                "git diff --check")) {
            assertTrue(page.contains(expected), "reviewer dashboard should mention " + expected);
        }
    }

    @Test
    void reviewerDashboardKeepsNotProvenBoundariesExplicit() throws Exception {
        String page = read(DASHBOARD);

        for (String expected : List.of(
                "No production certification",
                "No enterprise-production readiness",
                "No registry publication",
                "No container signing",
                "No live AWS/cloud validation",
                "No private-network production validation",
                "No real tenant/IdP proof",
                "No production SLO/SLA proof",
                "no registry publish",
                "no container signing")) {
            assertTrue(page.contains(expected), "reviewer dashboard should preserve boundary " + expected);
        }
    }

    @Test
    void reviewerDashboardAvoidsPublishSignReleaseCloudAndSecretPatterns() throws Exception {
        String normalized = read(DASHBOARD).toLowerCase(Locale.ROOT);

        for (String prohibited : List.of(
                "docker push",
                "docker login",
                "cosign sign",
                "cosign attest",
                "gh release",
                "git tag",
                "aws_access_key_id",
                "aws_secret_access_key",
                "github_token",
                "cosign_private_key",
                "localstorage",
                "sessionstorage",
                "cdn.")) {
            assertFalse(normalized.contains(prohibited), "reviewer dashboard must not include " + prohibited);
        }

        for (String unsafeClaim : List.of(
                "production certified gateway",
                "enterprise production ready",
                "live cloud validated",
                "real tenant proof complete",
                "signed container published",
                "registry publish complete",
                "container signing complete",
                "governance settings applied")) {
            assertFalse(normalized.contains(unsafeClaim), "reviewer dashboard must not overclaim " + unsafeClaim);
        }
    }

    @Test
    void reviewerDashboardIsLinkedFromRootReadmeAndTrustMap() throws Exception {
        String index = read(INDEX);
        String readme = read(README);
        String trustMap = read(TRUST_MAP);

        assertTrue(index.contains("/enterprise-lab-reviewer.html"));
        assertTrue(readme.contains("http://localhost:8080/enterprise-lab-reviewer.html"));
        assertTrue(trustMap.contains("/enterprise-lab-reviewer.html"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
