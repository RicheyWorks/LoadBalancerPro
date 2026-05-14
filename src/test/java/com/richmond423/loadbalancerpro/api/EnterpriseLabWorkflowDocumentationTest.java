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

class EnterpriseLabWorkflowDocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Path API_CONTRACTS = Path.of("docs/API_CONTRACTS.md");
    private static final Path CHARTER = Path.of("docs/ENTERPRISE_LAB_PRODUCT_CHARTER.md");
    private static final Path ROADMAP = Path.of("docs/ENTERPRISE_LAB_ROADMAP.md");
    private static final Path POLICY_GATE = Path.of("docs/CONTROLLED_ACTIVE_LASE_POLICY_GATE.md");
    private static final Path DEMO = Path.of("docs/DEMO_WALKTHROUGH.md");
    private static final Path SRE = Path.of("docs/SRE_DEMO_HIGHLIGHTS.md");
    private static final Path TRUST = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path TEST_EVIDENCE = Path.of("evidence/TEST_EVIDENCE.md");
    private static final Path PAGE = Path.of("src/main/resources/static/enterprise-lab.html");
    private static final Path INDEX = Path.of("src/main/resources/static/index.html");
    private static final Path SCRIPT = Path.of("scripts/smoke/enterprise-lab-workflow.ps1");
    private static final Path POLICY_SCRIPT = Path.of("scripts/smoke/controlled-adaptive-routing-policy.ps1");
    private static final Path POSTMAN_SMOKE = Path.of("scripts/smoke/postman-enterprise-lab-safe-smoke.ps1");

    @Test
    void labWorkflowDocsExposeScenarioRunScorecardEvidenceAndBoundaries() throws Exception {
        String docs = read(README) + read(API_CONTRACTS) + read(CHARTER) + read(ROADMAP) + read(POLICY_GATE)
                + read(DEMO) + read(SRE) + read(TRUST) + read(RUNBOOK) + read(TEST_EVIDENCE);

        for (String expected : List.of(
                "GET /api/lab/scenarios",
                "POST /api/lab/runs",
                "GET /api/lab/runs/{runId}",
                "GET /api/lab/policy",
                "GET /api/lab/audit-events",
                "/enterprise-lab.html",
                "target/enterprise-lab-runs/",
                "target/controlled-adaptive-routing/",
                "scorecard",
                "baseline",
                "shadow",
                "recommend",
                "active-experiment",
                "guardrail",
                "rollback",
                "audit events",
                "loadbalancerpro.lase.policy.mode",
                "loadbalancerpro.lase.policy.active-experiment-enabled",
                "process-local",
                "bounded",
                "in-memory",
                "lab evidence only / not production activation",
                "prod/cloud-sandbox",
                "X-API-Key")) {
            assertTrue(docs.contains(expected), "Enterprise Lab docs should mention " + expected);
        }

        String normalized = docs.toLowerCase(Locale.ROOT);
        assertFalse(normalized.contains("enterprise lab is production deployment certified"));
        assertFalse(normalized.contains("active production adaptive routing by default"));
    }

    @Test
    void browserPageIsSourceVisibleLocalOnlyAndDoesNotPersistSecrets() throws Exception {
        String page = read(PAGE);
        String index = read(INDEX);
        String normalized = page.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "LoadBalancerPro Enterprise Lab",
                "/api/lab/scenarios",
                "/api/lab/runs",
                "/api/lab/policy",
                "/api/lab/audit-events",
                "X-API-Key",
                "&lt;API_KEY&gt;",
                "lab evidence only",
                "not production activation",
                "scorecard",
                "active-experiment",
                "Policy Status",
                "Audit Events",
                "target/enterprise-lab-runs/")) {
            assertTrue(page.contains(expected), "browser page should mention " + expected);
        }
        assertTrue(index.contains("enterprise-lab.html"));

        for (String prohibited : List.of(
                "localstorage",
                "sessionstorage",
                "https://",
                "cdn.",
                "docker push",
                "cosign sign",
                "gh release")) {
            assertFalse(normalized.contains(prohibited), "browser page must not include " + prohibited);
        }
    }

    @Test
    void controlledPolicySmokeScriptWritesOnlyIgnoredTargetEvidenceAndAvoidsPublishCommands() throws Exception {
        String script = read(POLICY_SCRIPT);
        String normalized = script.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "target/controlled-adaptive-routing",
                "--enterprise-lab-workflow=$mode",
                "off",
                "shadow",
                "recommend",
                "active-experiment",
                "Assert-OutputUnderTarget",
                "Assert-NoSecretValues",
                "controlled-adaptive-routing-policy-summary.md",
                "controlled-adaptive-routing-policy-metadata.json")) {
            assertTrue(script.contains(expected), "policy script should mention " + expected);
        }

        for (String prohibited : List.of(
                "git clean",
                "git tag",
                "git push",
                "gh release create",
                "gh release upload",
                "gh release delete",
                "docker push",
                "cosign sign",
                "release-downloads")) {
            assertFalse(normalized.contains(prohibited), "policy script must not include " + prohibited);
        }
    }

    @Test
    void enterpriseLabSmokeScriptWritesOnlyIgnoredTargetEvidenceAndAvoidsPublishCommands() throws Exception {
        String script = read(SCRIPT);
        String normalized = script.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "target/enterprise-lab-runs",
                "--enterprise-lab-workflow=$Mode",
                "--enterprise-lab-output=$OutputDir",
                "Assert-OutputUnderTarget",
                "Assert-NoSecretValues",
                "enterprise-lab-scenario-catalog.json",
                "enterprise-lab-run.json",
                "enterprise-lab-run-summary.md",
                "enterprise-lab-evidence-metadata.json")) {
            assertTrue(script.contains(expected), "script should mention " + expected);
        }

        for (String prohibited : List.of(
                "git clean",
                "git tag",
                "git push",
                "gh release create",
                "gh release upload",
                "gh release delete",
                "docker push",
                "cosign sign",
                "release-downloads")) {
            assertFalse(normalized.contains(prohibited), "script must not include " + prohibited);
        }
    }

    @Test
    void postmanSmokeMentionsEnterpriseLabChecksAndKeepsSecretsPlaceholderOnly() throws Exception {
        String script = read(POSTMAN_SMOKE);

        for (String expected : List.of(
                "/api/lab/scenarios",
                "/api/lab/runs",
                "/api/lab/policy",
                "/api/lab/audit-events",
                "Enterprise Lab scenarios missing key gated",
                "Enterprise Lab scenarios correct key allowed",
                "Enterprise Lab run missing key gated",
                "Enterprise Lab run correct key allowed",
                "Enterprise Lab policy missing key gated",
                "Enterprise Lab audit correct key allowed",
                "<REDACTED>")) {
            assertTrue(script.contains(expected), "Postman smoke should mention " + expected);
        }

        assertFalse(looksLikeEmbeddedSecret(script), "Postman smoke must not embed real secret-looking values");
    }

    @Test
    void docsAndScriptsDoNotEmbedRealSecretLookingValues() throws Exception {
        String combined = read(README) + read(API_CONTRACTS) + read(POLICY_GATE) + read(DEMO) + read(SRE)
                + read(TRUST) + read(RUNBOOK) + read(SCRIPT) + read(POLICY_SCRIPT) + read(PAGE);

        assertFalse(looksLikeEmbeddedSecret(combined),
                "Enterprise Lab docs/scripts must use placeholders instead of secret-looking values");
    }

    private static boolean looksLikeEmbeddedSecret(String text) {
        String placeholdersRemoved = text
                .replace("CHANGE_ME_LOCAL_API_KEY", "")
                .replace("WRONG_CHANGE_ME_LOCAL_API_KEY", "")
                .replace("TEST_PROD_API_KEY", "")
                .replace("<API_KEY>", "")
                .replace("<REDACTED>", "");
        Pattern secretPattern = Pattern.compile(
                "(?i)(x-api-key|api[-_ ]?key|bearer|password|secret|token)\\s*[:=]\\s*"
                        + "(?!(loadbalancerpro|replace-with))[a-z0-9._~+/-]{12,}");
        return secretPattern.matcher(placeholdersRemoved).find();
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
