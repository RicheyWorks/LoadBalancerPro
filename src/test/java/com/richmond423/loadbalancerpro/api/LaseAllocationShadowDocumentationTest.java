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

class LaseAllocationShadowDocumentationTest {
    private static final Path README = Path.of("README.md");
    private static final Path API_CONTRACTS = Path.of("docs/API_CONTRACTS.md");
    private static final Path DEMO_WALKTHROUGH = Path.of("docs/DEMO_WALKTHROUGH.md");
    private static final Path READINESS_SUMMARY = Path.of("docs/PRODUCTION_READINESS_SUMMARY.md");

    @Test
    void docsDescribeShadowOnlyAllocationEvaluationSummary() throws Exception {
        String combined = read(README) + "\n" + read(API_CONTRACTS) + "\n"
                + read(DEMO_WALKTHROUGH) + "\n" + read(READINESS_SUMMARY);
        String normalized = combined.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "loadbalancerpro.lase.shadow.enabled=true",
                "POST /api/allocate/evaluate",
                "`laseShadow`",
                "shadow-only",
                "does not alter live allocation",
                "signals considered",
                "tail latency",
                "queue depth",
                "adaptive concurrency")) {
            assertTrue(combined.contains(expected), "docs should mention " + expected);
        }

        assertFalse(normalized.contains("lase active routing is enabled by default"));
        assertFalse(normalized.contains("lase changes live allocation by default"));
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
