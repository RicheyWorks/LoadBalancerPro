package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class ProductionCandidateEvidenceGateDocumentationTest {
    private static final Path GATE = Path.of("docs/PRODUCTION_CANDIDATE_EVIDENCE_GATE.md");
    private static final Path SUPPLY_CHAIN = Path.of("evidence/SUPPLY_CHAIN_EVIDENCE.md");
    private static final Path TEST_EVIDENCE = Path.of("evidence/TEST_EVIDENCE.md");
    private static final Path SECURITY_POSTURE = Path.of("evidence/SECURITY_POSTURE.md");
    private static final Path HARDENING_GUIDE = Path.of("docs/DEPLOYMENT_HARDENING_GUIDE.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path EXECUTIVE_SUMMARY = Path.of("docs/EXECUTIVE_SUMMARY.md");
    private static final Path RELEASE_WORKFLOW = Path.of(".github/workflows/release-artifacts.yml");
    private static final Path CI_WORKFLOW = Path.of(".github/workflows/ci.yml");
    private static final Path CODEQL_WORKFLOW = Path.of(".github/workflows/codeql.yml");
    private static final Path DOCKERFILE = Path.of("Dockerfile");

    @Test
    void productionCandidateGateListsRequiredEvidenceControls() throws Exception {
        String gate = read(GATE);

        for (String expected : List.of(
                "mvn -q clean test",
                "mvn -q verify",
                "mvn -q -DskipTests package",
                "Required PR checks",
                "CodeQL",
                "Dependency Review",
                "Trivy",
                "CycloneDX",
                "SHA-256",
                "GitHub artifact attestations",
                "GitHub Release asset",
                "Docker base image digest",
                "No secrets",
                "No `release-downloads/` mutation",
                "No native or vendored binary artifacts")) {
            assertTrue(gate.contains(expected), "gate should require " + expected);
        }
    }

    @Test
    void productionCandidateGateSeparatesAutomationFromManualVerification() throws Exception {
        String gate = read(GATE);
        String normalized = gate.toLowerCase(Locale.ROOT);

        assertTrue(gate.contains("## Automated Evidence Today"));
        assertTrue(gate.contains("## Manual Or Operator Verification"));
        assertTrue(gate.contains("## Production-Candidate Checklist"));
        assertTrue(gate.contains("## Release-Ready Additions"));
        assertTrue(gate.contains("Completing it does not create tags, GitHub Releases, release assets"));
        assertTrue(gate.contains("semantic-tag Release Artifacts workflow"));
        assertFalse(normalized.contains("certified production"));
        assertFalse(normalized.contains("guarantees production"));
    }

    @Test
    void gateIsLinkedFromReviewerAndEvidenceDocs() throws Exception {
        for (Path path : List.of(SUPPLY_CHAIN, TEST_EVIDENCE, SECURITY_POSTURE, HARDENING_GUIDE, TRUST_MAP,
                EXECUTIVE_SUMMARY)) {
            assertTrue(read(path).contains("PRODUCTION_CANDIDATE_EVIDENCE_GATE.md"),
                    path + " should link to the production-candidate evidence gate");
        }
    }

    @Test
    void gateMatchesCurrentWorkflowAndContainerEvidenceSources() throws Exception {
        String gate = read(GATE);
        String ci = read(CI_WORKFLOW);
        String codeql = read(CODEQL_WORKFLOW);
        String releaseWorkflow = read(RELEASE_WORKFLOW);
        String dockerfile = read(DOCKERFILE);

        assertTrue(ci.contains("cyclonedx-maven-plugin:2.9.1:makeAggregateBom"));
        assertTrue(ci.contains("aquasecurity/trivy-action@0.36.0"));
        assertTrue(ci.contains("actions/dependency-review-action@v5.0.0"));
        assertTrue(codeql.contains("name: CodeQL"));
        assertTrue(releaseWorkflow.contains("sha256sum -c"));
        assertTrue(releaseWorkflow.contains("actions/attest@v4.1.0"));
        assertTrue(releaseWorkflow.contains("Verify GitHub Release assets"));
        assertTrue(dockerfile.contains("@sha256:"));
        assertTrue(gate.contains("digest-pinned base images"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
