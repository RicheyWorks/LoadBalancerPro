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

class ProductionReadinessSummaryDocumentationTest {
    private static final Path SUMMARY = Path.of("docs/PRODUCTION_READINESS_SUMMARY.md");
    private static final Path README = Path.of("README.md");
    private static final Path EXECUTIVE_SUMMARY = Path.of("docs/EXECUTIVE_SUMMARY.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path HARDENING_GUIDE = Path.of("docs/DEPLOYMENT_HARDENING_GUIDE.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path PRODUCTION_GATE = Path.of("docs/PRODUCTION_CANDIDATE_EVIDENCE_GATE.md");
    private static final Path SECURITY_POSTURE = Path.of("evidence/SECURITY_POSTURE.md");
    private static final Path SUPPLY_CHAIN = Path.of("evidence/SUPPLY_CHAIN_EVIDENCE.md");

    @Test
    void summaryDocumentsProductionCandidateStatusAndAuthBoundary() throws Exception {
        String summary = read(SUMMARY);

        for (String expected : List.of(
                "production-candidate for controlled enterprise demo/reviewer usage",
                "Local/default mode is intentionally permissive",
                "Container/default deployment uses the `prod` profile",
                "deny-by-default for non-`OPTIONS` `/api/**`",
                "`GET /api/health` as the explicit public API exception",
                "`/proxy/**`",
                "`/api/proxy/status`",
                "`/v3/api-docs`",
                "Swagger UI")) {
            assertTrue(summary.contains(expected), "summary should document " + expected);
        }
    }

    @Test
    void summaryDocumentsOauthDtoContainerAndSupplyChainStatus() throws Exception {
        String summary = read(SUMMARY);

        for (String expected : List.of(
                "`roles`, `role`, `authorities`, or `realm_access.roles`",
                "Standard `scope` and `scp` claims do not grant `ROLE_operator` or `ROLE_admin`",
                "reject omitted JSON values",
                "Dockerfile defaults to `SPRING_PROFILES_ACTIVE=prod`",
                "`LOADBALANCERPRO_API_KEY`",
                "Dependency Review",
                "Trivy",
                "CycloneDX SBOM",
                "CodeQL",
                "GitHub artifact attestations",
                "DEPENDENCY_SAST_RISK_WORKFLOW.md",
                "PRODUCTION_CANDIDATE_EVIDENCE_GATE.md")) {
            assertTrue(summary.contains(expected), "summary should document " + expected);
        }
    }

    @Test
    void summaryDocumentsValidationCountAndRemainingLimits() throws Exception {
        String summary = read(SUMMARY);
        String normalized = summary.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "0 failures, 0 errors, and 0 skips",
                "Surefire reports for the exact test count",
                "mvn -q verify",
                "mvn -q -DskipTests package",
                "Operator run-profile and Postman enterprise lab dry-runs",
                "No container image is published to a registry",
                "No container signing",
                "No PGP-style release artifact signing",
                "No real enterprise IdP tenant configuration",
                "No live AWS sandbox validation",
                "`release-downloads/` remains manual")) {
            assertTrue(summary.contains(expected), "summary should document " + expected);
        }

        assertFalse(normalized.contains("guarantees production"));
        assertFalse(normalized.contains("cosign signature exists"));
        assertFalse(normalized.contains("published registry image"));
    }

    @Test
    void summaryIsLinkedFromReviewerEntryPoints() throws Exception {
        for (Path path : List.of(README, EXECUTIVE_SUMMARY, TRUST_MAP, HARDENING_GUIDE, RUNBOOK, PRODUCTION_GATE,
                SECURITY_POSTURE, SUPPLY_CHAIN)) {
            assertTrue(read(path).contains("PRODUCTION_READINESS_SUMMARY.md"),
                    path + " should link the production readiness summary");
        }
    }

    @Test
    void securityPostureNoLongerDocumentsOldMutationOnlyApiKeyBoundary() throws Exception {
        String securityPosture = read(SECURITY_POSTURE);

        assertTrue(securityPosture.contains("require `X-API-Key` for non-`OPTIONS` `/api/**` requests by default"));
        assertFalse(securityPosture.contains("protect `POST`/`PUT`/`PATCH` requests under `/api/**`"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
