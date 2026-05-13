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

class PostmanLocalSmokeHarnessTest {
    private static final Path SCRIPT = Path.of("scripts/smoke/postman-enterprise-lab-safe-smoke.ps1");
    private static final Path POSTMAN_DOC = Path.of("docs/POSTMAN_COLLECTION.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path API_SECURITY = Path.of("docs/API_SECURITY.md");
    private static final Path README = Path.of("README.md");
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "AKIA[0-9A-Z]{16}|(?i)aws_secret_access_key\\s*[:=]|(?i)client_secret\\s*[:=]|"
                    + "(?i)ghp_[A-Za-z0-9_]{20,}|(?i)xox[baprs]-[A-Za-z0-9-]{20,}|"
                    + "-----BEGIN [A-Z ]*PRIVATE KEY-----");
    private static final Pattern PUBLIC_EXTERNAL_URL =
            Pattern.compile("https?://(?!127\\.0\\.0\\.1(?::|/|$)|localhost(?::|/|$))[^\\s\"'`]+");
    private static final Pattern CLOUD_MANAGER_CONSTRUCTION =
            Pattern.compile("new\\s+" + "CloudManager\\s*\\(|" + "CloudManager\\s*\\(");

    @Test
    void smokeHarnessExistsAndSupportsExpectedModes() throws Exception {
        String script = read(SCRIPT);

        assertTrue(script.contains("[switch]$DryRun"));
        assertTrue(script.contains("[switch]$Package"));
        assertTrue(script.contains("[int]$LocalPort"));
        assertTrue(script.contains("[int]$ProdPort"));
        assertTrue(script.contains("[string]$ApiKey"));
        assertTrue(script.contains("[string]$EvidenceDir"));
        assertTrue(script.contains("if ($DryRun -or -not $Package)"));
        assertTrue(script.contains("Find-ExecutableJar"));
        assertTrue(script.contains("Start-OrReuseApp"));
        assertTrue(script.contains("Write-SmokeEvidence"));
        assertTrue(script.contains("docs/postman/LoadBalancerPro Enterprise Lab.postman_collection.json"));
        assertTrue(script.contains("docs/postman/LoadBalancerPro Local.postman_environment.json"));
        assertTrue(script.contains("mvn"));
        assertTrue(script.contains("java"));
    }

    @Test
    void smokeHarnessChecksPostmanEnterpriseLabBoundaries() throws Exception {
        String script = read(SCRIPT);

        assertTrue(script.contains("/api/health"));
        assertTrue(script.contains("/actuator/health"));
        assertTrue(script.contains("/v3/api-docs"));
        assertTrue(script.contains("/swagger-ui/index.html"));
        assertTrue(script.contains("/api/routing/compare"));
        assertTrue(script.contains("/api/allocate/evaluate"));
        assertTrue(script.contains("/actuator/metrics"));
        assertTrue(script.contains("/actuator/prometheus"));
        assertTrue(script.contains("OpenAPI missing key gated"));
        assertTrue(script.contains("OpenAPI wrong key gated"));
        assertTrue(script.contains("OpenAPI correct key allowed"));
        assertTrue(script.contains("Swagger missing key gated"));
        assertTrue(script.contains("routing missing key gated"));
        assertTrue(script.contains("routing wrong key gated"));
        assertTrue(script.contains("routing correct key allowed"));
        assertTrue(script.contains("evaluation missing key gated"));
        assertTrue(script.contains("evaluation wrong key gated"));
        assertTrue(script.contains("evaluation correct key allowed"));
        assertTrue(script.contains("actuator metrics not public"));
        assertTrue(script.contains("actuator Prometheus not public"));
    }

    @Test
    void smokeHarnessRedactsApiKeysAndAvoidsUnsafeContent() throws Exception {
        String script = read(SCRIPT);
        String normalized = script.toLowerCase(Locale.ROOT);

        assertTrue(script.contains("Redact-Text"));
        assertTrue(script.contains("<REDACTED>"));
        assertTrue(script.contains("LOADBALANCERPRO_API_KEY"));
        assertTrue(script.contains("CHANGE_ME_LOCAL_API_KEY"));
        assertTrue(script.contains("WRONG_CHANGE_ME_LOCAL_API_KEY"));
        assertFalse(script.contains("--loadbalancerpro.api.key=$ApiKey"));
        assertFalse(normalized.contains("authorization"));
        assertFalse(script.contains("Bearer "));
        assertFalse(normalized.contains("localstorage"));
        assertFalse(normalized.contains("sessionstorage"));
        assertFalse(script.contains("Cookie:"));
        assertFalse(script.contains("\"Cookie\""));
        assertFalse(normalized.contains("newman run"));
        assertFalse(normalized.contains("npm install"));
        assertFalse(script.contains("release-downloads"));
        assertFalse(script.contains("delete-branch"));
        assertFalse(script.contains("/rulesets"));
        assertFalse(SECRET_PATTERN.matcher(script).find());
        assertFalse(PUBLIC_EXTERNAL_URL.matcher(script).find(), "script should be localhost/loopback only");
        assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(script).find(), "script must not construct CloudManager");
    }

    @Test
    void smokeHarnessExportsSanitizedMarkdownAndJsonEvidence() throws Exception {
        String script = read(SCRIPT);

        assertTrue(script.contains("postman-enterprise-lab-smoke.json"));
        assertTrue(script.contains("postman-enterprise-lab-smoke.md"));
        assertTrue(script.contains("apiKey = \"<REDACTED>\""));
        assertTrue(script.contains("No secrets, bearer tokens, cookies, credentials"));
        assertTrue(script.contains("profile = $Profile"));
        assertTrue(script.contains("expectedStatus = $Expected"));
        assertTrue(script.contains("actualStatus = $Actual"));
        assertTrue(script.contains("passed = $Passed"));
    }

    @Test
    void docsMentionLocalOnlySmokeEvidenceAndLimitations() throws Exception {
        String combined = read(POSTMAN_DOC) + "\n" + read(RUNBOOK) + "\n" + read(API_SECURITY) + "\n" + read(README);
        String normalized = combined.toLowerCase(Locale.ROOT);

        assertTrue(combined.contains("postman-enterprise-lab-safe-smoke.ps1"));
        assertTrue(combined.contains("-DryRun"));
        assertTrue(combined.contains("-Package"));
        assertTrue(combined.contains("-EvidenceDir"));
        assertTrue(combined.contains("sanitized Markdown"));
        assertTrue(combined.contains("sanitized JSON"));
        assertTrue(combined.contains("X-API-Key"));
        assertTrue(combined.contains("<REDACTED>"));
        assertTrue(normalized.contains("local-only"));
        assertTrue(normalized.contains("no real secrets"));
        assertTrue(normalized.contains("cloud mutation"));
        assertTrue(normalized.contains("oauth2"));
        assertTrue(normalized.contains("not production iam"));
    }

    @Test
    void committedPostmanSmokeFilesAvoidSecretsAndGeneratedEvidence() throws Exception {
        for (Path path : List.of(SCRIPT, POSTMAN_DOC, RUNBOOK, API_SECURITY, README)) {
            String content = read(path);
            assertFalse(SECRET_PATTERN.matcher(content).find(), path + " must not contain secret-like values");
            assertFalse(CLOUD_MANAGER_CONSTRUCTION.matcher(content).find(), path + " must not construct CloudManager");
        }

        assertFalse(Files.exists(Path.of("postman-enterprise-lab-smoke.json")),
                "generated smoke evidence should not be committed at the repo root");
        assertFalse(Files.exists(Path.of("postman-enterprise-lab-smoke.md")),
                "generated smoke evidence should not be committed at the repo root");
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
