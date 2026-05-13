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

class DependencySastRiskWorkflowDocumentationTest {
    private static final Path WORKFLOW = Path.of("docs/DEPENDENCY_SAST_RISK_WORKFLOW.md");
    private static final Path CODEOWNERS = Path.of(".github/CODEOWNERS");
    private static final Path SUPPLY_CHAIN = Path.of("evidence/SUPPLY_CHAIN_EVIDENCE.md");
    private static final Path SECURITY_POSTURE = Path.of("evidence/SECURITY_POSTURE.md");
    private static final Path RESIDUAL_RISKS = Path.of("evidence/RESIDUAL_RISKS.md");
    private static final Path HARDENING_GUIDE = Path.of("docs/DEPLOYMENT_HARDENING_GUIDE.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path README = Path.of("README.md");
    private static final Path PRODUCTION_GATE = Path.of("docs/PRODUCTION_CANDIDATE_EVIDENCE_GATE.md");

    @Test
    void workflowDocumentsOwnersAndTools() throws Exception {
        String workflow = read(WORKFLOW);
        String codeowners = read(CODEOWNERS);

        for (String expected : List.of(
                "CodeQL/SAST owner",
                "Dependency Review owner",
                "Trivy/container owner",
                "`@RicheyWorks`",
                ".github/CODEOWNERS",
                "pom.xml",
                "Dockerfile",
                "SBOM")) {
            assertTrue(workflow.contains(expected), "workflow should document " + expected);
        }
        assertTrue(codeowners.contains("* @RicheyWorks"));
    }

    @Test
    void workflowDocumentsSeverityHandlingAndNoSilentDismissal() throws Exception {
        String workflow = read(WORKFLOW);
        String normalized = workflow.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "Critical",
                "High",
                "Medium",
                "Unknown severity",
                "Do not silently dismiss, ignore, or allowlist high or critical findings",
                "owner",
                "severity",
                "expiry or review date",
                "follow-up")) {
            assertTrue(workflow.contains(expected), "workflow should document " + expected);
        }
        assertFalse(normalized.contains("silently dismiss high"));
        assertFalse(normalized.contains("silently ignore high"));
    }

    @Test
    void workflowDocumentsAcceptedRiskFalsePositiveAndRemediationTargets() throws Exception {
        String workflow = read(WORKFLOW);

        for (String expected : List.of(
                "Accepted-Risk Rationale Template",
                "False-Positive Rationale Template",
                "Reason for temporary acceptance",
                "Why the finding does not apply",
                "Reopen condition",
                "Critical reachable runtime finding",
                "High reachable runtime finding",
                "Medium reachable runtime finding",
                "License/policy blocker from Dependency Review")) {
            assertTrue(workflow.contains(expected), "workflow should document " + expected);
        }
    }

    @Test
    void workflowDocumentsEvidenceLocationsAndSafeBoundaries() throws Exception {
        String workflow = read(WORKFLOW);
        String normalized = workflow.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "CI, Dependency Review, CodeQL, and Trivy check output",
                "`loadbalancerpro-sbom`",
                "docs/PRODUCTION_CANDIDATE_EVIDENCE_GATE.md",
                "evidence/SUPPLY_CHAIN_EVIDENCE.md",
                "evidence/SECURITY_POSTURE.md",
                "evidence/RESIDUAL_RISKS.md",
                "does not dismiss GitHub alerts",
                "does not change rulesets",
                "does not mutate `release-downloads/`")) {
            assertTrue(workflow.contains(expected), "workflow should document " + expected);
        }
        assertFalse(normalized.contains("gh release create"));
        assertFalse(normalized.contains("gh release upload"));
        assertFalse(normalized.contains("git tag -"));
    }

    @Test
    void workflowIsLinkedFromReviewerAndEvidenceDocs() throws Exception {
        for (Path path : List.of(SUPPLY_CHAIN, SECURITY_POSTURE, RESIDUAL_RISKS, HARDENING_GUIDE, TRUST_MAP,
                RUNBOOK, README, PRODUCTION_GATE)) {
            assertTrue(read(path).contains("DEPENDENCY_SAST_RISK_WORKFLOW.md"),
                    path + " should link the dependency/SAST risk workflow");
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
