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
                "/api/enterprise-lab/reviewer-summary",
                "Live Local Summary",
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
    void reviewerSummaryApiReturnsDeterministicLocalBoundaryMetadata() throws Exception {
        String response = mockMvc.perform(get("/api/enterprise-lab/reviewer-summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posture.enterpriseLabReady", is(true)))
                .andExpect(jsonPath("$.posture.reviewerReadyEnterpriseLab", is(true)))
                .andExpect(jsonPath("$.posture.productionCertified", is(false)))
                .andExpect(jsonPath("$.posture.enterpriseProductionReady", is(false)))
                .andExpect(jsonPath("$.boundaries.noRegistryPublishClaim", is(true)))
                .andExpect(jsonPath("$.boundaries.noContainerSigningClaim", is(true)))
                .andExpect(jsonPath("$.boundaries.noLiveCloudValidationClaim", is(true)))
                .andExpect(jsonPath("$.boundaries.noRealTenantProofClaim", is(true)))
                .andExpect(jsonPath("$.boundaries.noProductionSloSlaProof", is(true)))
                .andExpect(jsonPath("$.boundaries.governancePreparedRepoSideOnly", is(true)))
                .andExpect(jsonPath("$.evidence.readinessAuditPath",
                        is("docs/ENTERPRISE_READINESS_AUDIT.md")))
                .andExpect(jsonPath("$.evidence.reviewerTrustMapPath",
                        is("docs/REVIEWER_TRUST_MAP.md")))
                .andExpect(jsonPath("$.evidence.productionReadinessSummaryPath",
                        is("docs/PRODUCTION_READINESS_SUMMARY.md")))
                .andExpect(jsonPath("$.evidence.securityPosturePath",
                        is("evidence/SECURITY_POSTURE.md")))
                .andExpect(jsonPath("$.evidence.governanceHardeningPath",
                        is("docs/MANUAL_GITHUB_GOVERNANCE_HARDENING.md")))
                .andExpect(jsonPath("$.evidence.containerDistributionSigningLanePath",
                        is("docs/CONTAINER_DISTRIBUTION_SIGNING_EVIDENCE_LANE.md")))
                .andExpect(jsonPath("$.evidence.containerSigningDryRunLanePath",
                        is("docs/CONTAINER_SIGNING_DRY_RUN_VERIFICATION_LANE.md")))
                .andExpect(jsonPath("$.ciArtifact.name",
                        is("container-dry-run-evidence-no-publish-no-sign")))
                .andExpect(jsonPath("$.ciArtifact.evidenceDirectory",
                        is("target/container-dry-run-evidence/")))
                .andExpect(jsonPath("$.ciArtifact.proves[0]", is("local-only image identity evidence")))
                .andExpect(jsonPath("$.ciArtifact.doesNotProve[0]", is("registry publication")))
                .andExpect(jsonPath("$.verificationCommands[0]", is("mvn -q test")))
                .andExpect(jsonPath("$.verificationCommands[1]", is("mvn -q -DskipTests package")))
                .andExpect(jsonPath("$.verificationCommands[2]", is("mvn -B package")))
                .andExpect(jsonPath("$.verificationCommands[3]", is("git diff --check")))
                .andExpect(jsonPath("$.dashboard.pagePath", is("/enterprise-lab-reviewer.html")))
                .andExpect(jsonPath("$.dashboard.summaryVersion", is("local-reviewer-summary-v1")))
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
            assertFalse(response.contains(unsafe), "reviewer summary API must not include " + unsafe);
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
