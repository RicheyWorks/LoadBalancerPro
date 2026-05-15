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
class EvidenceExportPacketViewTest {
    private static final Path EXPORT_PACKET =
            Path.of("src/main/resources/static/evidence-export-packet.html");
    private static final Path EXPORT_PACKET_CONTROLLER =
            Path.of("src/main/java/com/richmond423/loadbalancerpro/api/EnterpriseLabEvidenceExportPacketController.java");
    private static final Path TIMELINE =
            Path.of("src/main/resources/static/evidence-timeline.html");
    private static final Path OPERATOR_DASHBOARD =
            Path.of("src/main/resources/static/operator-evidence-dashboard.html");
    private static final Path REVIEWER_DASHBOARD =
            Path.of("src/main/resources/static/enterprise-lab-reviewer.html");
    private static final Path INDEX = Path.of("src/main/resources/static/index.html");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final List<Path> MAJOR_EVIDENCE_PAGES =
            List.of(REVIEWER_DASHBOARD, OPERATOR_DASHBOARD, TIMELINE, EXPORT_PACKET);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void evidenceExportPacketPageExistsOnClasspathAndIsServed() throws Exception {
        assertTrue(Files.exists(EXPORT_PACKET), "evidence export packet should be source-controlled");
        assertTrue(new ClassPathResource("static/evidence-export-packet.html").exists(),
                "evidence export packet should be packaged as a static resource");

        mockMvc.perform(get("/evidence-export-packet.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Evidence Export Packet")))
                .andExpect(content().string(containsString("Reviewer handoff packet")))
                .andExpect(content().string(containsString("Not production certified")))
                .andExpect(content().string(containsString("Not enterprise-production ready")));
    }

    @Test
    void evidenceExportPacketDocumentsTemplatePathsLinksAndBoundaries() throws Exception {
        String page = read(EXPORT_PACKET);

        for (String expected : List.of(
                "Evidence Export Packet",
                "Reviewer handoff packet",
                "/api/enterprise-lab/evidence-export-packet",
                "target/enterprise-lab-runs/",
                "target/container-dry-run-evidence/",
                "container-dry-run-evidence-no-publish-no-sign",
                "/evidence-timeline.html",
                "/operator-evidence-dashboard.html",
                "/enterprise-lab-reviewer.html",
                "/api/enterprise-lab/evidence-timeline",
                "/api/enterprise-lab/operator-evidence-summary",
                "/api/enterprise-lab/reviewer-summary",
                "docs/REVIEWER_TRUST_MAP.md",
                "docs/ENTERPRISE_READINESS_AUDIT.md",
                "docs/MANUAL_GITHUB_GOVERNANCE_HARDENING.md",
                "Packet label",
                "Commit SHA",
                "Dashboard links checked",
                "not production certified",
                "not enterprise-production ready",
                "no registry publish",
                "no container signing",
                "generated evidence should not be committed",
                "do not include secrets/tokens/private keys",
                "Download is generated locally in your browser",
                "The server does not create files",
                "no actual export file generation")) {
            assertTrue(page.contains(expected), "evidence export packet should mention " + expected);
        }
    }

    @Test
    void evidenceExportPacketProvidesBrowserLocalMarkdownAndJsonDownloads() throws Exception {
        String page = read(EXPORT_PACKET);

        for (String expected : List.of(
                "Download Markdown packet",
                "Download JSON packet",
                "Copy Markdown packet",
                "Blob",
                "URL.createObjectURL",
                "loadbalancerpro-evidence-packet.md",
                "loadbalancerpro-evidence-packet.json",
                "generated locally in your browser",
                "server does not create files",
                "do not include secrets/tokens/private keys",
                "buildMarkdownPacket",
                "buildJsonPacket",
                "downloadText")) {
            assertTrue(page.contains(expected), "browser-local packet download should include " + expected);
        }
    }

    @Test
    void evidenceExportPacketProvidesBrowserLocalPrintStyles() throws Exception {
        String page = read(EXPORT_PACKET);

        for (String expected : List.of(
                "@media print",
                "Print / Save as PDF",
                "window.print",
                "handled by your browser",
                "server does not create PDF files",
                "Download Markdown packet",
                "Download JSON packet",
                "Copy Markdown packet",
                "Blob",
                "URL.createObjectURL",
                "not production certified",
                "not enterprise-production ready",
                "no registry publish",
                "no container signing",
                "no live cloud validation",
                "no real tenant/IdP proof")) {
            assertTrue(page.contains(expected), "browser-local print support should include " + expected);
        }
    }

    @Test
    void evidenceExportPacketProvidesReviewerShareChecklistWithoutUploadBehavior() throws Exception {
        String page = read(EXPORT_PACKET);

        for (String expected : List.of(
                "Reviewer Packet Share Checklist",
                "Markdown packet copied or downloaded",
                "JSON packet downloaded",
                "PDF/print copy generated through browser",
                "Evidence timeline link included",
                "Operator evidence dashboard link included",
                "Reviewer dashboard link included",
                "CI dry-run artifact name included",
                "Verification commands included",
                "Not-proven boundaries included",
                "no secrets/tokens/private keys included",
                "do not claim production certification",
                "do not claim registry publish",
                "do not claim container signing",
                "server does not send, upload, or create share artifacts",
                "server does not create PDF files",
                "Generated target evidence should not be committed",
                "/enterprise-lab-reviewer.html",
                "/operator-evidence-dashboard.html",
                "/evidence-timeline.html",
                "/evidence-export-packet.html")) {
            assertTrue(page.contains(expected), "reviewer share checklist should include " + expected);
        }
    }

    @Test
    void majorEvidencePagesExposeConsistentReviewerNavigation() throws Exception {
        List<String> evidencePages = List.of(
                "/enterprise-lab-reviewer.html",
                "/operator-evidence-dashboard.html",
                "/evidence-timeline.html",
                "/evidence-export-packet.html");
        List<String> localApiPaths = List.of(
                "/api/enterprise-lab/reviewer-summary",
                "/api/enterprise-lab/operator-evidence-summary",
                "/api/enterprise-lab/evidence-timeline",
                "/api/enterprise-lab/evidence-export-packet");
        List<String> reviewerPathSteps = List.of(
                "Evidence Navigation",
                "Recommended reviewer path",
                "Start with Reviewer Dashboard",
                "Check Operator Evidence Dashboard",
                "Review Evidence Timeline",
                "Open Evidence Export Packet",
                "Copy/download/print packet",
                "Use Share Checklist before sending",
                "Verify not-proven boundaries");

        for (Path pagePath : MAJOR_EVIDENCE_PAGES) {
            String page = read(pagePath);
            String normalized = page.toLowerCase(Locale.ROOT);
            for (String expected : evidencePages) {
                assertTrue(page.contains(expected), pagePath + " should link " + expected);
            }
            for (String expected : localApiPaths) {
                assertTrue(page.contains(expected), pagePath + " should list local API path " + expected);
            }
            for (String expected : reviewerPathSteps) {
                assertTrue(page.contains(expected), pagePath + " should include reviewer path step " + expected);
            }
            for (String boundary : List.of(
                    "not production certified",
                    "not enterprise-production ready",
                    "no registry publish",
                    "no container signing")) {
                assertTrue(normalized.contains(boundary), pagePath + " should preserve boundary " + boundary);
            }
            for (String prohibited : List.of(
                    "upload endpoint",
                    "share endpoint",
                    "server-side share endpoint",
                    "server-side export complete",
                    "server creates pdf")) {
                assertFalse(normalized.contains(prohibited), pagePath + " must not introduce " + prohibited);
            }
        }

        String index = read(INDEX);
        for (String expected : evidencePages) {
            assertTrue(index.contains(expected), "root page should link " + expected);
        }

        String trustMap = read(TRUST_MAP);
        assertTrue(trustMap.contains("Evidence Page Navigation"));
        assertTrue(trustMap.contains("Recommended reviewer path"));
        assertTrue(trustMap.contains("Use Share Checklist before sending"));
    }

    @Test
    void evidenceExportPacketApiReturnsDeterministicLocalPacketMetadata() throws Exception {
        String response = mockMvc.perform(get("/api/enterprise-lab/evidence-export-packet"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dashboardPath", is("/evidence-export-packet.html")))
                .andExpect(jsonPath("$.timelinePath", is("/evidence-timeline.html")))
                .andExpect(jsonPath("$.operatorDashboardPath", is("/operator-evidence-dashboard.html")))
                .andExpect(jsonPath("$.reviewerDashboardPath", is("/enterprise-lab-reviewer.html")))
                .andExpect(jsonPath("$.packetSections[0]", is("project/version/commit")))
                .andExpect(jsonPath("$.evidencePaths.enterpriseLabRuns", is("target/enterprise-lab-runs/")))
                .andExpect(jsonPath("$.evidencePaths.containerDryRunEvidence",
                        is("target/container-dry-run-evidence/")))
                .andExpect(jsonPath("$.ciArtifact.name",
                        is("container-dry-run-evidence-no-publish-no-sign")))
                .andExpect(jsonPath("$.verificationCommands[0]", is("mvn -q test")))
                .andExpect(jsonPath("$.verificationCommands[1]", is("mvn -q -DskipTests package")))
                .andExpect(jsonPath("$.verificationCommands[2]", is("mvn -B package")))
                .andExpect(jsonPath("$.verificationCommands[3]", is("git diff --check")))
                .andExpect(jsonPath("$.packetTemplateFields[0]", is("Packet label")))
                .andExpect(jsonPath("$.packetTemplateFields[1]", is("Commit SHA")))
                .andExpect(jsonPath("$.proves[0]",
                        is("reviewer packet can be assembled from existing local/CI evidence")))
                .andExpect(jsonPath("$.doesNotProve[0]", is("production certification")))
                .andExpect(jsonPath("$.safetyBoundaries[0]",
                        is("human-readable handoff template only")))
                .andExpect(jsonPath("$.safetyBoundaries[1]",
                        is("no actual export file generation")))
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
                "export file generated",
                "docker push",
                "docker login",
                "cosign sign",
                "cosign attest",
                "gh release",
                "git tag")) {
            assertFalse(response.contains(unsafe), "evidence export packet API must not include " + unsafe);
        }
    }

    @Test
    void evidenceExportPacketAvoidsUnsafeCommandsAndExternalDependencies() throws Exception {
        String normalized = read(EXPORT_PACKET).toLowerCase(Locale.ROOT);

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
                "mailto:",
                "navigator.share",
                "sendbeacon",
                "upload endpoint",
                "share endpoint",
                "server-side share endpoint",
                "http://",
                "https://")) {
            assertFalse(normalized.contains(prohibited), "evidence export packet must not include " + prohibited);
        }

        assertTrue(normalized.contains("fetch(\"/api/enterprise-lab/evidence-export-packet\""),
                "evidence export packet should fetch the same-origin local summary endpoint");
    }

    @Test
    void evidenceExportPacketControllerDoesNotGenerateServerSideFiles() throws Exception {
        String controller = read(EXPORT_PACKET_CONTROLLER);

        for (String prohibited : List.of(
                "PDFBox",
                "iText",
                "PdfWriter",
                "Files.write",
                "FileOutputStream",
                "ZipOutputStream",
                "createFile",
                "writeString",
                "ProcessBuilder",
                "Runtime.getRuntime",
                "System.getenv")) {
            assertFalse(controller.contains(prohibited),
                    "evidence export packet API must not include server-side export behavior: " + prohibited);
        }
    }

    @Test
    void evidenceExportPacketIsLinkedFromDashboardsReadmeAndTrustMap() throws Exception {
        assertTrue(read(INDEX).contains("/evidence-export-packet.html"));
        assertTrue(read(TIMELINE).contains("/evidence-export-packet.html"));
        assertTrue(read(OPERATOR_DASHBOARD).contains("/evidence-export-packet.html"));
        assertTrue(read(REVIEWER_DASHBOARD).contains("/evidence-export-packet.html"));
        assertTrue(read(README).contains("http://localhost:8080/evidence-export-packet.html"));
        assertTrue(read(TRUST_MAP).contains("/evidence-export-packet.html"));
        assertTrue(read(TRUST_MAP).contains("/api/enterprise-lab/evidence-export-packet"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
