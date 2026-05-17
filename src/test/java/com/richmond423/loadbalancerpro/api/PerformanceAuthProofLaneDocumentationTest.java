package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PerformanceAuthProofLaneDocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Path ROADMAP = Path.of("docs/ENTERPRISE_LAB_ROADMAP.md");
    private static final Path CHARTER = Path.of("docs/ENTERPRISE_LAB_PRODUCT_CHARTER.md");
    private static final Path API_SECURITY = Path.of("docs/API_SECURITY.md");
    private static final Path API_CONTRACTS = Path.of("docs/API_CONTRACTS.md");
    private static final Path OBSERVABILITY = Path.of("docs/OBSERVABILITY.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SRE = Path.of("docs/SRE_DEMO_HIGHLIGHTS.md");
    private static final Path DEMO = Path.of("docs/DEMO_WALKTHROUGH.md");
    private static final Path PRODUCTION_SUMMARY = Path.of("docs/PRODUCTION_READINESS_SUMMARY.md");
    private static final Path TEST_EVIDENCE = Path.of("evidence/TEST_EVIDENCE.md");
    private static final Path COMBINED_LANE = Path.of("docs/MEASURED_PERFORMANCE_BASELINE_AND_AUTH_PROOF_LANE.md");
    private static final Path PERFORMANCE_BASELINE = Path.of("evidence/PERFORMANCE_BASELINE.md");
    private static final Path AUTH_PROOF = Path.of("docs/ENTERPRISE_AUTH_PROOF_LANE.md");
    private static final Path PAGE = Path.of("src/main/resources/static/enterprise-lab.html");
    private static final Path PERFORMANCE_SCRIPT = Path.of("scripts/smoke/performance-baseline.ps1");
    private static final Path AUTH_SCRIPT = Path.of("scripts/smoke/enterprise-auth-proof.ps1");

    @Test
    void docsExposePerformanceBaselineAndAuthProofLanesHonestly() throws Exception {
        String combined = read(README) + read(ROADMAP) + read(CHARTER) + read(API_SECURITY)
                + read(API_CONTRACTS) + read(OBSERVABILITY) + read(RUNBOOK) + read(TRUST_MAP)
                + read(SRE) + read(DEMO) + read(PRODUCTION_SUMMARY) + read(TEST_EVIDENCE)
                + read(COMBINED_LANE) + read(PERFORMANCE_BASELINE) + read(AUTH_PROOF) + read(PAGE);

        for (String expected : List.of(
                "MEASURED_PERFORMANCE_BASELINE_AND_AUTH_PROOF_LANE.md",
                "scripts/smoke/performance-baseline.ps1",
                "target/performance-baseline/",
                "performance-dashboard.json",
                "performance-threshold-results.json",
                "warning-only",
                "local loopback",
                "not production SLO",
                "scripts/smoke/enterprise-auth-proof.ps1",
                "target/enterprise-auth-proof/",
                "ENTERPRISE_AUTH_PROOF_LANE.md",
                "mock IdP/JWKS",
                "scope-only denial",
                "token lifetime",
                "key-rotation",
                "no real enterprise IdP tenant validation",
                "local flight simulator and black-box recorder")) {
            assertTrue(combined.contains(expected), "docs should mention " + expected);
        }

        String normalized = combined.toLowerCase(Locale.ROOT);
        assertFalse(normalized.contains("production performance certification is complete"));
        assertFalse(normalized.contains("real enterprise tenant proof is complete"));
        assertFalse(normalized.contains("scope/scp values become application roles"));
    }

    @Test
    void combinedLaneConnectsExistingProofsWithoutNewProductionClaims() throws Exception {
        String lane = read(COMBINED_LANE);
        String normalized = lane.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "local evidence lane ready",
                "LoadBalancerPro is not trying to out-Envoy Envoy",
                "evidence, governance, and explainability layer for adaptive routing",
                "local flight simulator and black-box recorder",
                "scripts\\smoke\\performance-baseline.ps1 -Package",
                "scripts\\smoke\\enterprise-auth-proof.ps1 -Package",
                "PerformanceBaselineFixtureCatalogTest,EnterpriseAuthProofLaneTest,OAuth2AuthorizationTest,PerformanceAuthProofLaneDocumentationTest",
                "target/performance-baseline/",
                "target/enterprise-auth-proof/",
                "../evidence/PERFORMANCE_BASELINE.md",
                "ENTERPRISE_AUTH_PROOF_LANE.md",
                "warning-only performance thresholds",
                "synthetic mock IdP/JWKS fixtures",
                "mocked-resource-server tests",
                "should not be committed")) {
            assertTrue(lane.contains(expected), "combined lane should mention " + expected);
        }

        for (String expected : List.of(
                "not production certification",
                "not live cloud proof",
                "not real tenant proof",
                "not slo/sla proof",
                "real enterprise idp validation",
                "no real secrets",
                "no real enterprise idp tenant validation",
                "does not use real secrets, tokens, tenant ids, customer data, private endpoints, cloud resources")) {
            assertTrue(normalized.contains(expected), "combined lane should keep boundary: " + expected);
        }

        String linkedDocs = read(README) + read(TRUST_MAP) + read(Path.of("docs/ENTERPRISE_READINESS_AUDIT.md"));
        assertTrue(linkedDocs.contains("MEASURED_PERFORMANCE_BASELINE_AND_AUTH_PROOF_LANE.md"),
                "current reviewer entry points should link the combined lane");
        assertFalse(normalized.contains("production performance certification is complete"));
        assertFalse(normalized.contains("production slo proof is complete"));
        assertFalse(normalized.contains("real enterprise tenant proof is complete"));
        assertFalse(normalized.contains("real enterprise idp validation is complete"));
    }

    @Test
    void proofLaneScriptsAvoidReleaseAndContainerPublishingCommands() throws Exception {
        String combined = read(PERFORMANCE_SCRIPT) + "\n" + read(AUTH_SCRIPT);
        String normalized = combined.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "Assert-OutputUnderTarget",
                "Assert-NoSecretValues",
                "target/performance-baseline",
                "target/enterprise-auth-proof",
                "127.0.0.1",
                "local/package mode only",
                "EnterpriseAuthProofLaneTest,OAuth2AuthorizationTest")) {
            assertTrue(combined.contains(expected), "scripts should mention " + expected);
        }

        for (String prohibited : List.of(
                "git clean",
                "git tag",
                "git push",
                "git push --tags",
                "gh release create",
                "gh release upload",
                "gh release delete",
                "docker push",
                "cosign sign",
                "native-image",
                "jpackage",
                "launch4j")) {
            assertFalse(normalized.contains(prohibited), "proof scripts must not include " + prohibited);
        }
    }

    @Test
    void docsAndProofArtifactsDoNotEmbedSecretLookingValues() throws Exception {
        String combined = read(PERFORMANCE_BASELINE) + read(AUTH_PROOF) + read(PERFORMANCE_SCRIPT)
                + read(AUTH_SCRIPT) + read(Path.of("src/test/resources/auth-proof/mock-idp-claims.json"));
        String placeholdersRemoved = combined
                .replace("CHANGE_ME_LOCAL_API_KEY", "")
                .replace("WRONG_CHANGE_ME_LOCAL_API_KEY", "")
                .replace("TEST_PROD_API_KEY", "")
                .replace("<REDACTED>", "")
                .replace("<API_KEY>", "");
        Pattern secretPattern = Pattern.compile(
                "(?i)(x-api-key|api[-_ ]?key|bearer|password|secret|credential|token)\\s*[:=]\\s*"
                        + "(?!(loadbalancerpro|replace-with))[a-z0-9._~+/-]{12,}");
        assertFalse(secretPattern.matcher(placeholdersRemoved).find(),
                "performance/auth proof docs and scripts must not embed secret-looking values");
        assertFalse(read(Path.of("src/test/resources/auth-proof/mock-idp-claims.json")).contains("-----BEGIN"));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
