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

class SreDemoHighlightsDocumentationTest {
    private static final Path HIGHLIGHTS = Path.of("docs/SRE_DEMO_HIGHLIGHTS.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path READINESS_SUMMARY = Path.of("docs/PRODUCTION_READINESS_SUMMARY.md");
    private static final Path DEMO_WALKTHROUGH = Path.of("docs/DEMO_WALKTHROUGH.md");

    @Test
    void highlightsPageCoversReleaseGuardrailsAdaptiveRoutingAndRisks() throws Exception {
        String highlights = read(HIGHLIGHTS);
        String normalized = highlights.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "# SRE Demo Highlights",
                "`v2.5.0`",
                "4cc03750be5479d9f8f88f8ef8014e05a8dc587a",
                "LoadBalancerPro-2.5.0.jar",
                "LoadBalancerPro-2.5.0-bom.json",
                "LoadBalancerPro-2.5.0-bom.xml",
                "LoadBalancerPro-2.5.0-SHA256SUMS.txt",
                "checksum verification passed",
                "SBOM JSON/XML",
                "GitHub artifact attestation",
                "container publication and container signing are deferred",
                "prod",
                "deny-by-default for non-`OPTIONS` `/api/**`",
                "OAuth2 application roles",
                "dedicated role claims",
                "omitted JSON fails validation",
                "optional process-local rate limiter",
                "distributed edge rate limiting remains required",
                "Local/default mode is intentionally permissive",
                "operator intent flag",
                "sandbox resource-name prefix",
                "ownership confirmation",
                "capacity limits",
                "dry-run default",
                "deterministic LASE/replay tests",
                "`laseShadow`",
                "shadow-only",
                "does not alter live allocation",
                "controlled active LASE policy gate",
                "`active-experiment` explicit, guarded, and not enabled by default",
                "target/controlled-adaptive-routing/",
                "not production deployment certification",
                "No real enterprise IdP tenant proof")) {
            assertTrue(highlights.contains(expected), "highlights page should mention " + expected);
        }

        assertFalse(normalized.contains("production-certified"));
        assertFalse(normalized.contains("container signing is complete"));
        assertFalse(normalized.contains("lase active routing is enabled by default"));
    }

    @Test
    void highlightsPageIsLinkedFromReviewerEntryPoints() throws Exception {
        for (Path path : List.of(README, TRUST_MAP, READINESS_SUMMARY, DEMO_WALKTHROUGH)) {
            assertTrue(read(path).contains("SRE_DEMO_HIGHLIGHTS.md"),
                    path + " should link SRE demo highlights");
        }
    }

    @Test
    void highlightsPageDoesNotAddReleaseOrContainerCommands() throws Exception {
        String normalized = read(HIGHLIGHTS).toLowerCase(Locale.ROOT);

        for (String prohibited : List.of(
                "gh release create",
                "gh release upload",
                "git tag -",
                "docker push",
                "cosign sign",
                "git clean")) {
            assertFalse(normalized.contains(prohibited), "highlights page must not include " + prohibited);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
