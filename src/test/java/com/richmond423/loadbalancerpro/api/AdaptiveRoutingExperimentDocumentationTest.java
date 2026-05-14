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

class AdaptiveRoutingExperimentDocumentationTest {
    private static final Path SCRIPT = Path.of("scripts/smoke/adaptive-routing-experiment.ps1");

    @Test
    void scriptWritesOnlyIgnoredTargetEvidenceAndAvoidsPublishCommands() throws Exception {
        String script = read(SCRIPT);
        String normalized = script.toLowerCase(Locale.ROOT);

        assertTrue(script.contains("target/adaptive-routing-experiments"));
        assertTrue(script.contains("--adaptive-routing-experiment=all"));
        assertTrue(script.contains("Assert-OutputUnderTarget"));
        assertTrue(script.contains("Assert-NoSecretValues"));
        for (String prohibited : List.of(
                "git clean",
                "git tag",
                "git push",
                "gh release create",
                "gh release upload",
                "docker push",
                "cosign sign")) {
            assertFalse(normalized.contains(prohibited), "script must not include " + prohibited);
        }
    }

    @Test
    void reviewerDocsMentionExperimentHarnessSafetyAndEvidencePath() throws Exception {
        String docs = read(Path.of("docs/SRE_DEMO_HIGHLIGHTS.md"))
                + read(Path.of("docs/DEMO_WALKTHROUGH.md"))
                + read(Path.of("docs/REVIEWER_TRUST_MAP.md"))
                + read(Path.of("docs/OPERATIONS_RUNBOOK.md"));

        for (String expected : List.of(
                "adaptive-routing experiment harness",
                "baseline vs shadow vs active-experiment",
                "default behavior unchanged",
                "feature flag",
                "target/adaptive-routing-experiments/",
                "--adaptive-routing-experiment=all",
                "no live cloud mutation")) {
            assertTrue(docs.contains(expected), "docs should mention " + expected);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
