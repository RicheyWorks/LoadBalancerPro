package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class EnterpriseCockpitAuthPlanDocsTest {
    private static final Path PLAN = Path.of("docs/ENTERPRISE_COCKPIT_AUTH_PLAN.md");
    private static final Path README = Path.of("README.md");
    private static final Path API_SECURITY = Path.of("docs/API_SECURITY.md");
    private static final Path API_CONTRACTS = Path.of("docs/API_CONTRACTS.md");
    private static final Path OPERATIONS_RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path RESIDUAL_RISKS = Path.of("evidence/RESIDUAL_RISKS.md");

    private static final Pattern REAL_LOOKING_SECRET = Pattern.compile(
            "(?i)(AKIA[0-9A-Z]{16}|ASIA[0-9A-Z]{16}|ghp_[A-Za-z0-9_]{20,}"
                    + "|github_pat_[A-Za-z0-9_]{20,}|xox[baprs]-[A-Za-z0-9-]{20,}"
                    + "|Bearer\\s+eyJ[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{20,}"
                    + "\\.[A-Za-z0-9_-]{20,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)");
    private static final Pattern POSITIVE_CERTIFICATION_OR_COMPLIANCE_CLAIM = Pattern.compile(
            "(?i)(certified gateway|certified security|certified compliance|certification proof"
                    + "|compliance proof|production iam certified|soc\\s*2|hipaa|pci[- ]dss|fedramp"
                    + "|is compliant with)");

    @Test
    void planExistsAndDocumentsCockpitAuthAndOpenApiGating() throws Exception {
        String plan = read(PLAN);
        String normalized = plan.toLowerCase(Locale.ROOT);

        assertTrue(plan.contains("# Enterprise Cockpit Auth Plan"));
        assertTrue(normalized.contains("cockpit auth"));
        assertTrue(plan.contains("## Swagger/OpenAPI Gating Plan"));
        assertTrue(normalized.contains("swagger/openapi gating"));
        assertTrue(plan.contains("## Endpoint Exposure Matrix"));
        assertTrue(plan.contains("/load-balancing-cockpit.html"));
        assertTrue(plan.contains("/v3/api-docs"));
        assertTrue(plan.contains("/actuator/prometheus"));
    }

    @Test
    void planDocumentsCurrentMismatchModesTestsAndRisks() throws Exception {
        String plan = read(PLAN);

        for (String expected : List.of(
                "## Current Model",
                "## Product Mismatch",
                "prod-api-key mode",
                "oauth2 mode",
                "reverse-proxy auth mode",
                "memory-only",
                "localStorage",
                "## Required Tests",
                "OAuth2 mode missing issuer/JWK fails closed",
                "Actuator metrics and Prometheus are not public in prod-like modes",
                "## Implementation Risks",
                "## Follow-up Sprint Recommendation")) {
            assertTrue(plan.contains(expected), "plan should mention " + expected);
        }
    }

    @Test
    void planAvoidsPositiveCertificationComplianceClaimsAndRealLookingSecrets() throws Exception {
        String plan = read(PLAN);

        assertTrue(plan.contains("No real IAM certification."));
        assertFalse(POSITIVE_CERTIFICATION_OR_COMPLIANCE_CLAIM.matcher(plan).find(),
                "plan must not add positive certification or compliance claims");
        assertFalse(REAL_LOOKING_SECRET.matcher(plan).find(), "plan must not include real-looking secrets");
    }

    @Test
    void docsPointToEnterpriseCockpitAuthPlan() throws Exception {
        for (Path path : List.of(README, API_SECURITY, API_CONTRACTS, OPERATIONS_RUNBOOK)) {
            assertTrue(read(path).contains("ENTERPRISE_COCKPIT_AUTH_PLAN.md"),
                    path + " should point to the enterprise cockpit auth plan");
        }
    }

    @Test
    void docsDocumentImplementedApiKeySwaggerGatingAndMemoryOnlyCockpitToken() throws Exception {
        String combined = read(PLAN) + "\n" + read(API_SECURITY) + "\n" + read(API_CONTRACTS)
                + "\n" + read(OPERATIONS_RUNBOOK) + "\n" + read(README);
        String normalized = combined.toLowerCase(Locale.ROOT);

        assertTrue(combined.contains("/v3/api-docs"));
        assertTrue(combined.contains("/swagger-ui.html"));
        assertTrue(combined.contains("/swagger-ui/**"));
        assertTrue(combined.contains("X-API-Key"));
        assertTrue(normalized.contains("prod/cloud-sandbox api-key mode"));
        assertTrue(normalized.contains("local/default"));
        assertTrue(normalized.contains("memory-only"));
        assertTrue(combined.contains("localStorage"));
        assertTrue(combined.contains("sessionStorage"));
        assertTrue(combined.contains("<API_KEY>"));
        assertTrue(normalized.contains("not logged") || normalized.contains("logs"));
        assertTrue(normalized.contains("not put in urls") || normalized.contains("not placed in urls"));
        assertTrue(normalized.contains("oauth2 login"));
        assertTrue(normalized.contains("scope") && normalized.contains("scp"));
        assertTrue(normalized.contains("not app roles") || normalized.contains("not promoted to app roles"));
        assertTrue(normalized.contains("not production iam certification"));
    }

    @Test
    void docsDocumentDtoOmissionValidation() throws Exception {
        String combined = read(API_CONTRACTS) + "\n" + read(OPERATIONS_RUNBOOK) + "\n" + read(RESIDUAL_RISKS);
        String normalized = combined.toLowerCase(Locale.ROOT);

        for (String expected : List.of("requestedLoad", "cpuUsage", "memoryUsage", "diskUsage",
                "capacity", "weight", "healthy")) {
            assertTrue(combined.contains(expected), "docs should name required DTO field " + expected);
        }
        assertTrue(normalized.contains("omitted required"));
        assertTrue(normalized.contains("instead of default"));
        assertTrue(normalized.contains("0.0") && normalized.contains("false"));
        assertTrue(combined.contains("HTTP 400"));
        assertTrue(combined.contains("Mitigated"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
