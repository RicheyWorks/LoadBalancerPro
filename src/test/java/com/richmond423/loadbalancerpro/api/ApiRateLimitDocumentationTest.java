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

class ApiRateLimitDocumentationTest {
    private static final Path API_SECURITY = Path.of("docs/API_SECURITY.md");
    private static final Path RUNBOOK = Path.of("docs/OPERATIONS_RUNBOOK.md");
    private static final Path READINESS_SUMMARY = Path.of("docs/PRODUCTION_READINESS_SUMMARY.md");
    private static final Path README = Path.of("README.md");
    private static final Path RATE_LIMIT_FILTER = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/config/ApiRateLimitFilter.java");

    @Test
    void docsDescribeOptionalProcessLocalRateLimiterAndEdgeLimitRequirement() throws Exception {
        String apiSecurity = read(API_SECURITY);
        String normalized = apiSecurity.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "loadbalancerpro.api.rate-limit.enabled=false",
                "loadbalancerpro.api.rate-limit.capacity",
                "loadbalancerpro.api.rate-limit.refill-tokens",
                "loadbalancerpro.api.rate-limit.refill-period",
                "loadbalancerpro.api.rate-limit.trust-forwarded-for",
                "disabled by default",
                "local/demo workflows stay convenient",
                "process-local",
                "not a distributed quota system",
                "Apply rate limiting at the edge",
                "allocation",
                "routing",
                "scenario replay",
                "remediation",
                "proxy control/status",
                "LASE shadow observability",
                "`/proxy/**`",
                "`GET /api/health`",
                "`OPTIONS`",
                "`429 rate_limited`",
                "`Retry-After`")) {
            assertTrue(apiSecurity.contains(expected), "API security docs should mention " + expected);
        }

        assertFalse(normalized.contains("api key value"), "docs must not embed API key values");
        assertFalse(normalized.contains("bearer ey"), "docs must not embed bearer tokens");
    }

    @Test
    void reviewerDocsKeepRateLimitClaimsHonest() throws Exception {
        String runbook = read(RUNBOOK);
        String summary = read(READINESS_SUMMARY);
        String readme = read(README);

        assertTrue(runbook.contains("loadbalancerpro.api.rate-limit.enabled=true"));
        assertTrue(runbook.contains("not a replacement for distributed edge quotas"));
        assertTrue(summary.contains("optional process-local rate limiter"));
        assertTrue(summary.contains("Distributed edge rate limiting is still required"));
        assertTrue(readme.contains("optional process-local rate limiter"));
        assertTrue(readme.contains("not a distributed quota system"));
    }

    @Test
    void limiterImplementationAvoidsPublishAndSecretPersistenceCommands() throws Exception {
        String filter = read(RATE_LIMIT_FILTER).toLowerCase(Locale.ROOT);

        for (String prohibited : List.of(
                "git clean",
                "git tag",
                "gh release",
                "docker push",
                "cosign sign",
                "secret",
                "x-api-key",
                "authorization")) {
            assertFalse(filter.contains(prohibited), "rate-limit filter must not contain " + prohibited);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
