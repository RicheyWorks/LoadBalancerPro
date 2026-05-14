package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class EnterpriseLabWorkflowDocumentationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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
    private static final Path OBSERVABILITY_SCRIPT = Path.of("scripts/smoke/enterprise-lab-observability-pack.ps1");
    private static final Path GRAFANA_DASHBOARD =
            Path.of("docs/observability/grafana-enterprise-lab-dashboard.json");
    private static final Path ALERTS = Path.of("docs/observability/enterprise-lab-alerts.yml");
    private static final Path SLO_TEMPLATES = Path.of("docs/observability/SLO_TEMPLATES.md");

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
                "GET /api/lab/metrics",
                "GET /api/lab/metrics/prometheus",
                "/enterprise-lab.html",
                "target/enterprise-lab-runs/",
                "target/controlled-adaptive-routing/",
                "target/enterprise-lab-observability/",
                "grafana-enterprise-lab-dashboard.json",
                "enterprise-lab-alerts.yml",
                "SLO_TEMPLATES.md",
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
                "/api/lab/metrics",
                "/api/lab/metrics/prometheus",
                "X-API-Key",
                "&lt;API_KEY&gt;",
                "lab evidence only",
                "not production activation",
                "scorecard",
                "active-experiment",
                "Policy Status",
                "Audit Events",
                "Observability",
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
                "Enterprise Lab metrics missing key gated",
                "Enterprise Lab metrics correct key allowed",
                "Enterprise Lab Prometheus metrics missing key gated",
                "Enterprise Lab Prometheus metrics correct key allowed",
                "<REDACTED>")) {
            assertTrue(script.contains(expected), "Postman smoke should mention " + expected);
        }

        assertFalse(looksLikeEmbeddedSecret(script), "Postman smoke must not embed real secret-looking values");
    }

    @Test
    void observabilityAssetsExistAndStayLabGrade() throws Exception {
        String dashboard = read(GRAFANA_DASHBOARD);
        String alerts = read(ALERTS);
        String slos = read(SLO_TEMPLATES);
        JsonNode dashboardJson = OBJECT_MAPPER.readTree(dashboard);
        assertTrue(dashboardJson.path("panels").isArray(), "dashboard JSON should expose panels");

        for (String expected : List.of(
                "Lab Runs Created",
                "Scenarios Executed",
                "Policy Decisions By Mode",
                "Recommendations Vs Active Changes",
                "Guardrail Blocks By Reason",
                "Rollback And Fail-Closed Events",
                "Audit Event Retention And Drops",
                "Rate Limit Events",
                "API Health And Prod Boundary Status",
                "Lab-Grade Evidence Note")) {
            assertTrue(dashboard.contains(expected), "dashboard should mention " + expected);
        }

        for (String expected : List.of(
                "LoadBalancerProActiveExperimentUnexpectedlyEnabled",
                "LoadBalancerProHighGuardrailBlockRate",
                "LoadBalancerProRollbackFailClosedEvents",
                "LoadBalancerProAuditEventDrops",
                "LoadBalancerProRateLimitEvents",
                "LoadBalancerProMissingLabMetrics",
                "LoadBalancerProStaleEvidenceGeneration",
                "lab-grade-template")) {
            assertTrue(alerts.contains(expected), "alerts should mention " + expected);
        }

        for (String expected : List.of(
                "Lab Workflow SLO Templates",
                "Controlled Adaptive-Routing SLO Templates",
                "Future Production Gateway Candidate SLO Templates",
                "current metrics are lab-grade/process-local",
                "No production SLO certification is claimed")) {
            assertTrue(slos.contains(expected), "SLO templates should mention " + expected);
        }

        String normalized = (dashboard + alerts + slos).toLowerCase(Locale.ROOT);
        assertFalse(normalized.contains("production slo certification is complete"));
        assertFalse(normalized.contains("published registry image"));
        assertFalse(normalized.contains("client_secret"));
    }

    @Test
    void observabilitySmokeScriptWritesOnlyIgnoredTargetEvidenceAndAvoidsPublishCommands() throws Exception {
        String script = read(OBSERVABILITY_SCRIPT);
        String normalized = script.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "target/enterprise-lab-observability",
                "/api/lab/metrics",
                "/api/lab/metrics/prometheus",
                "lab-metrics.json",
                "lab-metrics.prom",
                "observability-summary.md",
                "observability-evidence-manifest.json",
                "Assert-OutputUnderTarget",
                "Assert-NoSecretValues")) {
            assertTrue(script.contains(expected), "observability script should mention " + expected);
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
            assertFalse(normalized.contains(prohibited), "observability script must not include " + prohibited);
        }
    }

    @Test
    void docsAndScriptsDoNotEmbedRealSecretLookingValues() throws Exception {
        String combined = read(README) + read(API_CONTRACTS) + read(POLICY_GATE) + read(DEMO) + read(SRE)
                + read(TRUST) + read(RUNBOOK) + read(SCRIPT) + read(POLICY_SCRIPT) + read(OBSERVABILITY_SCRIPT)
                + read(PAGE);

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
