package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
class CiEvidenceGatePrototypeTest {
    private static final Path PAGE = Path.of("src/main/resources/static/ci-evidence-gate.html");
    private static final Path CONTROLLER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/EnterpriseLabCiEvidenceGateSummaryController.java");
    private static final Path INDEX = Path.of("src/main/resources/static/index.html");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path READINESS_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path CI_GATE_LANE = Path.of("docs/CI_EVIDENCE_GATE_READINESS_LANE.md");
    private static final Path PERFORMANCE_AUTH_LANE =
            Path.of("docs/MEASURED_PERFORMANCE_BASELINE_AND_AUTH_PROOF_LANE.md");
    private static final List<Path> LINKED_REVIEWER_PAGES = List.of(
            Path.of("src/main/resources/static/enterprise-lab-reviewer.html"),
            Path.of("src/main/resources/static/operator-evidence-dashboard.html"),
            Path.of("src/main/resources/static/evidence-timeline.html"),
            Path.of("src/main/resources/static/evidence-export-packet.html"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ciEvidenceGateSummaryEndpointReturnsDeterministicLocalPrototype() throws Exception {
        String first = mockMvc.perform(get("/api/enterprise-lab/ci-evidence-gate-summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.gateName", is("CI Evidence Gate Prototype")))
                .andExpect(jsonPath("$.mode", is("prototype/local-review")))
                .andExpect(jsonPath("$.decision", is("READY_FOR_LOCAL_REVIEW")))
                .andExpect(jsonPath("$.enforcementStatus", is("NOT_ENFORCED")))
                .andExpect(jsonPath("$.dashboardPath", is("/ci-evidence-gate.html")))
                .andExpect(jsonPath("$.apiPath",
                        is("/api/enterprise-lab/ci-evidence-gate-summary")))
                .andExpect(jsonPath("$.linkedReviewerPages[0]",
                        is("/enterprise-lab-reviewer.html")))
                .andExpect(jsonPath("$.requiredEvidenceInputs[0].localEvidencePath",
                        is("target/enterprise-lab-runs/enterprise-lab-run-summary.md")))
                .andExpect(jsonPath("$.requiredEvidenceInputs[1].localEvidencePath",
                        is("target/controlled-adaptive-routing/controlled-adaptive-routing-policy-summary.md")))
                .andExpect(jsonPath("$.requiredEvidenceInputs[2].localEvidencePath",
                        is("target/enterprise-lab-observability/observability-summary.md")))
                .andExpect(jsonPath("$.requiredEvidenceInputs[3].localEvidencePath",
                        is("target/performance-baseline/performance-summary.md")))
                .andExpect(jsonPath("$.requiredEvidenceInputs[4].localEvidencePath",
                        is("target/enterprise-auth-proof/enterprise-auth-proof-summary.md")))
                .andExpect(jsonPath("$.requiredEvidenceInputs[5].localEvidencePath",
                        is("target/adaptive-routing-experiments/adaptive-routing-experiment.md")))
                .andExpect(jsonPath("$.readinessChecks[0].state", is("PASS_STYLE")))
                .andExpect(jsonPath("$.readinessChecks[2].state", is("WARN_STYLE")))
                .andExpect(jsonPath("$.readinessChecks[4].state", is("FAIL_STYLE_BLOCKER")))
                .andExpect(jsonPath("$.manualReviewSteps[0]",
                        is("Confirm latest CI and CodeQL passed for the exact commit under review.")))
                .andExpect(jsonPath("$.notProvenBoundaries[0]", is("No production certification")))
                .andExpect(jsonPath("$.safetyBoundaries[2]", is("no file reads")))
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(get("/api/enterprise-lab/ci-evidence-gate-summary"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(first, second, "CI evidence gate summary should be deterministic");
    }

    @Test
    void ciEvidenceGatePageExistsIsPackagedServedAndUsesSameOriginSummary() throws Exception {
        assertTrue(Files.exists(PAGE), "CI evidence gate page should be source-controlled");
        assertTrue(new ClassPathResource("static/ci-evidence-gate.html").exists(),
                "CI evidence gate page should be packaged as a static resource");

        mockMvc.perform(get("/ci-evidence-gate.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("CI Evidence Gate Prototype")))
                .andExpect(content().string(containsString("/api/enterprise-lab/ci-evidence-gate-summary")))
                .andExpect(content().string(containsString("READY_FOR_LOCAL_REVIEW")))
                .andExpect(content().string(containsString("NOT_ENFORCED")))
                .andExpect(content().string(containsString("PASS_STYLE")))
                .andExpect(content().string(containsString("WARN_STYLE")))
                .andExpect(content().string(containsString("FAIL_STYLE_BLOCKER")));

        String page = read(PAGE);
        assertTrue(page.contains("fetch(\"/api/enterprise-lab/ci-evidence-gate-summary\""),
                "page should fetch only the same-origin prototype summary endpoint");
        for (String expected : List.of(
                "/enterprise-lab-reviewer.html",
                "/operator-evidence-dashboard.html",
                "/evidence-timeline.html",
                "/evidence-export-packet.html")) {
            assertTrue(page.contains(expected), "page should link " + expected);
        }
    }

    @Test
    void ciEvidenceGatePrototypeIsLinkedFromDocsIndexAndReviewerSurfaces() throws Exception {
        for (Path path : List.of(INDEX, README, TRUST_MAP, READINESS_AUDIT, CI_GATE_LANE, PERFORMANCE_AUTH_LANE)) {
            String content = read(path);
            assertTrue(content.contains("/ci-evidence-gate.html"), path + " should link the page");
            assertTrue(content.contains("/api/enterprise-lab/ci-evidence-gate-summary"),
                    path + " should reference the local summary API");
        }

        for (Path page : LINKED_REVIEWER_PAGES) {
            String content = read(page);
            assertTrue(content.contains("/ci-evidence-gate.html"), page + " should link the prototype page");
            assertTrue(content.contains("/api/enterprise-lab/ci-evidence-gate-summary"),
                    page + " should reference the prototype API");
        }
    }

    @Test
    void ciEvidenceGatePrototypeKeepsLocalEvidencePathsUnderIgnoredTarget() throws Exception {
        String combined = read(PAGE) + "\n" + read(CONTROLLER) + "\n" + read(CI_GATE_LANE);

        for (String expected : List.of(
                "target/enterprise-lab-runs/",
                "target/controlled-adaptive-routing/",
                "target/enterprise-lab-observability/",
                "target/performance-baseline/",
                "target/enterprise-auth-proof/",
                "target/adaptive-routing-experiments/")) {
            assertTrue(combined.contains(expected), "prototype should mention local evidence path " + expected);
        }

        for (String suspiciousPath : List.of(
                "../target",
                "C:\\",
                "/var/",
                "/tmp/",
                ".github/workflows/")) {
            assertFalse(read(PAGE).contains(suspiciousPath),
                    "prototype page should not point reviewers to " + suspiciousPath);
        }
    }

    @Test
    void ciEvidenceGatePrototypeAvoidsUnsafeCallsCommandsAndOverclaims() throws Exception {
        String combined = (read(PAGE) + "\n" + read(CONTROLLER) + "\n" + read(CI_GATE_LANE))
                .toLowerCase(Locale.ROOT);

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
                "https://",
                "branch protection has been changed",
                "required checks have been changed",
                "rulesets have been changed",
                "github settings have been changed",
                "blocks merges today",
                "ci evidence gate is enforced",
                "live enforcement is enabled",
                "production certification complete",
                "production certified gateway",
                "production slo proof is complete",
                "production sla proof is complete",
                "live cloud validated",
                "real tenant validation complete",
                "real enterprise idp validation is complete",
                "signed container published",
                "registry publish complete",
                "github governance settings applied",
                "governance-applied proof complete",
                "benchmark result:",
                "p95 latency is",
                "p99 latency is",
                "requests per second is")) {
            assertFalse(combined.contains(prohibited), "prototype must not include " + prohibited);
        }
    }

    @Test
    void ciEvidenceGateSummaryControllerStaysStaticReadOnlyAndLocal() throws Exception {
        String controller = read(CONTROLLER);

        for (String prohibited : List.of(
                "Files.",
                "Path.of",
                "FileInputStream",
                "FileOutputStream",
                "Files.write",
                "ProcessBuilder",
                "Runtime.getRuntime",
                "System.getenv",
                "System.getProperty",
                "@Value",
                "Environment",
                "RestTemplate",
                "WebClient",
                "HttpClient",
                "URLConnection",
                "new URL",
                "java.io",
                "java.nio.file",
                "Thread",
                "Executor")) {
            assertFalse(controller.contains(prohibited),
                    "CI evidence gate endpoint must not include dynamic side-effect behavior: " + prohibited);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
