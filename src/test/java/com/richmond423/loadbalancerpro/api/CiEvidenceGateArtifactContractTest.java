package com.richmond423.loadbalancerpro.api;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CiEvidenceGateArtifactContractTest {
    private static final Path CONTRACT = Path.of("docs/CI_EVIDENCE_GATE_ARTIFACT_CONTRACT.md");
    private static final Path TEMPLATE = Path.of("docs/examples/ci-evidence-gate-summary.template.json");
    private static final Path PAGE = Path.of("src/main/resources/static/ci-evidence-gate.html");
    private static final Path CONTROLLER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/EnterpriseLabCiEvidenceGateSummaryController.java");
    private static final Path INDEX = Path.of("src/main/resources/static/index.html");
    private static final Path REVIEWER = Path.of("src/main/resources/static/enterprise-lab-reviewer.html");
    private static final Path OPERATOR = Path.of("src/main/resources/static/operator-evidence-dashboard.html");
    private static final Path TIMELINE = Path.of("src/main/resources/static/evidence-timeline.html");
    private static final Path EXPORT_PACKET = Path.of("src/main/resources/static/evidence-export-packet.html");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path READINESS_AUDIT = Path.of("docs/ENTERPRISE_READINESS_AUDIT.md");
    private static final Path READINESS_LANE = Path.of("docs/CI_EVIDENCE_GATE_READINESS_LANE.md");
    private static final Path PERFORMANCE_AUTH_LANE =
            Path.of("docs/MEASURED_PERFORMANCE_BASELINE_AND_AUTH_PROOF_LANE.md");

    private static final String CONTRACT_PATH = "docs/CI_EVIDENCE_GATE_ARTIFACT_CONTRACT.md";
    private static final String TEMPLATE_PATH = "docs/examples/ci-evidence-gate-summary.template.json";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void contractDocumentDefinesTheLocalArtifactShapeAndBoundaries() throws Exception {
        String contract = read(CONTRACT);

        for (String required : List.of(
                "artifactVersion",
                "artifactKind",
                "generatedBy",
                "generatedAtPolicy",
                "mode",
                "decision",
                "enforcementStatus",
                "evidenceInputs",
                "readinessChecks",
                "manualReviewSteps",
                "safetyBoundaries",
                "notProvenBoundaries",
                "recommendedNextSteps",
                "ci-evidence-gate-artifact/v1",
                "TEMPLATE_ONLY",
                "READY_FOR_LOCAL_REVIEW",
                "NEEDS_MANUAL_REVIEW",
                "BLOCKED_FOR_LOCAL_REVIEW",
                "NOT_ENFORCED",
                "DOCUMENTATION_ONLY",
                "LOCAL_REVIEW_ONLY",
                "PASS_STYLE",
                "WARN_STYLE",
                "FAIL_STYLE_BLOCKER",
                "target/",
                "reviewer handoff",
                "not a generated run result")) {
            assertTrue(contract.contains(required), "contract should document " + required);
        }

        assertSafeNoFakeProofClaims(contract);
    }

    @Test
    void templateJsonIsValidClearlyTemplateOnlyAndFreeOfFakeEvidence() throws Exception {
        JsonNode template = objectMapper.readTree(read(TEMPLATE));

        assertEquals("TEMPLATE_ONLY_NOT_A_RUN_RESULT", template.path("templateNotice").asText());
        assertEquals("ci-evidence-gate-artifact/v1", template.path("artifactVersion").asText());
        assertEquals("ci-evidence-gate-summary-template", template.path("artifactKind").asText());
        assertEquals("TEMPLATE_ONLY_NO_TIMESTAMP", template.path("generatedAtPolicy").asText());
        assertEquals("prototype/local-review", template.path("mode").asText());
        assertEquals("TEMPLATE_ONLY", template.path("decision").asText());
        assertEquals("NOT_ENFORCED", template.path("enforcementStatus").asText());
        assertEquals(CONTRACT_PATH, template.path("artifactContract").asText());
        assertEquals(TEMPLATE_PATH, template.path("artifactTemplatePath").asText());

        JsonNode evidenceInputs = template.path("evidenceInputs");
        assertTrue(evidenceInputs.isArray(), "template evidenceInputs should be an array");
        assertTrue(evidenceInputs.size() >= 6, "template should list local evidence inputs");
        for (JsonNode input : evidenceInputs) {
            assertTrue(input.path("localEvidencePath").asText().startsWith("target/"),
                    "template local evidence paths must stay under target/");
            assertTrue(input.path("templateValuePolicy").asText().contains("placeholder"),
                    "template entries should be labeled as placeholder values");
        }

        String combined = String.join("\n", allTextValues(template)).toLowerCase(Locale.ROOT);
        assertFalse(combined.contains("2026-"), "template should not include fake evidence timestamps");
        assertFalse(combined.contains("tenant-id"), "template should not include fake tenant identifiers");
        assertFalse(combined.contains("tenant_id"), "template should not include fake tenant identifiers");
        assertFalse(combined.contains("github check id"), "template should not include fake GitHub check ids");
        assertFalse(combined.contains("sha256:"), "template should not include fake registry digests");
        assertFalse(combined.contains("benchmark result:"), "template should not include fake benchmark results");
        assertFalse(combined.contains("p95 latency"), "template should not include fake performance numbers");
        assertFalse(combined.contains("p99 latency"), "template should not include fake performance numbers");
        assertFalse(combined.contains("requests per second"), "template should not include fake performance numbers");
        assertNoSecretLikeValues(read(TEMPLATE));
        assertSafeNoFakeProofClaims(read(TEMPLATE));
    }

    @Test
    void endpointExposesContractMetadataWithoutChangingDeterminism() throws Exception {
        String first = mockMvc.perform(get("/api/enterprise-lab/ci-evidence-gate-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gateName", is("CI Evidence Gate Prototype")))
                .andExpect(jsonPath("$.artifactVersion", is("ci-evidence-gate-artifact/v1")))
                .andExpect(jsonPath("$.artifactKind", is("ci-evidence-gate-summary")))
                .andExpect(jsonPath("$.artifactContract", is(CONTRACT_PATH)))
                .andExpect(jsonPath("$.artifactTemplatePath", is(TEMPLATE_PATH)))
                .andExpect(jsonPath("$.mode", is("prototype/local-review")))
                .andExpect(jsonPath("$.decision", is("READY_FOR_LOCAL_REVIEW")))
                .andExpect(jsonPath("$.enforcementStatus", is("NOT_ENFORCED")))
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(get("/api/enterprise-lab/ci-evidence-gate-summary"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(first, second, "contract metadata should stay deterministic");
    }

    @Test
    void pageDocsAndReviewerHandoffFlowExposeContractWithoutExternalCalls() throws Exception {
        String page = read(PAGE);
        assertTrue(page.contains("fetch(\"/api/enterprise-lab/ci-evidence-gate-summary\""),
                "page should keep same-origin fetch only");
        assertTrue(page.contains(CONTRACT_PATH), "page should reference the artifact contract path");
        assertTrue(page.contains(TEMPLATE_PATH), "page should reference the template path");
        assertTrue(page.contains("Reviewer dashboard -> CI Evidence Gate page -> artifact contract"),
                "page should make the reviewer handoff flow discoverable");
        assertFalse(page.contains("http://"), "page should not add external HTTP calls");
        assertFalse(page.contains("https://"), "page should not add external HTTPS calls");
        assertFalse(page.contains("XMLHttpRequest"), "page should not add alternate external call mechanisms");
        assertFalse(page.contains("localStorage"), "page should not persist artifact data in browser storage");
        assertFalse(page.contains("sessionStorage"), "page should not persist artifact data in browser storage");

        for (Path doc : List.of(README, TRUST_MAP, READINESS_AUDIT, READINESS_LANE, PERFORMANCE_AUTH_LANE)) {
            String content = read(doc);
            assertTrue(content.contains("CI_EVIDENCE_GATE_ARTIFACT_CONTRACT.md"),
                    doc + " should link or reference the contract");
            assertTrue(content.contains("ci-evidence-gate-summary.template.json"),
                    doc + " should link or reference the template");
            assertTrue(content.contains("/ci-evidence-gate.html"),
                    doc + " should keep the gate page discoverable");
        }

        assertTrue(read(INDEX).contains("artifact contract handoff"),
                "index should describe the contract handoff surface");
        assertTrue(read(REVIEWER).contains("artifact contract/template paths"),
                "reviewer page should route reviewers toward the artifact handoff");
        assertTrue(read(OPERATOR).contains("artifact contract metadata"),
                "operator page should mention contract metadata");
        assertTrue(read(TIMELINE).contains("artifact contract/template handoff"),
                "timeline page should mention contract/template handoff");
        assertTrue(read(EXPORT_PACKET).contains(CONTRACT_PATH), "export packet should include contract path");
        assertTrue(read(EXPORT_PACKET).contains(TEMPLATE_PATH), "export packet should include template path");
    }

    @Test
    void endpointAndContractBundleStayInsideSafetyBoundaries() throws Exception {
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
                    "endpoint must stay static/read-only and avoid " + prohibited);
        }

        String combined = read(CONTRACT) + "\n" + read(TEMPLATE) + "\n" + read(PAGE) + "\n" + controller;
        assertNoSecretLikeValues(combined);
        assertSafeNoFakeProofClaims(combined);

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
                "mvn deploy")) {
            assertFalse(combined.toLowerCase(Locale.ROOT).contains(prohibited),
                    "contract bundle should not include unsafe command: " + prohibited);
        }
    }

    private static void assertSafeNoFakeProofClaims(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String prohibited : List.of(
                "blocks merges today",
                "ci evidence gate is enforced",
                "live enforcement is enabled",
                "branch protection has been changed",
                "required checks have been changed",
                "github settings have been changed",
                "production certification complete",
                "production certified gateway",
                "production performance is proven",
                "production slo proof is complete",
                "production sla proof is complete",
                "live cloud validated",
                "live-cloud validated",
                "real tenant validation complete",
                "real enterprise idp validation complete",
                "signed container published",
                "registry publish complete",
                "github governance settings applied",
                "governance-applied proof complete",
                "artifact generated for this run",
                "generated evidence for this commit",
                "real run result")) {
            assertFalse(lower.contains(prohibited), "unsafe affirmative claim found: " + prohibited);
        }
    }

    private static void assertNoSecretLikeValues(String text) {
        for (String prohibited : List.of(
                "ghp_",
                "github_pat_",
                "akia",
                "-----begin",
                "xoxb-",
                "xoxp-",
                "client_secret=",
                "client_secret:",
                "password=",
                "password:",
                "bearer ")) {
            assertFalse(text.toLowerCase(Locale.ROOT).contains(prohibited),
                    "secret-like value found: " + prohibited);
        }
    }

    private static List<String> allTextValues(JsonNode node) {
        List<String> values = new ArrayList<>();
        collectTextValues(node, values);
        return values;
    }

    private static void collectTextValues(JsonNode node, List<String> values) {
        if (node.isTextual()) {
            values.add(node.asText());
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectTextValues(child, values);
            }
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                collectTextValues(fields.next().getValue(), values);
            }
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
