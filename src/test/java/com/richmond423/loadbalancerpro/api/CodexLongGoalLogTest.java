package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class CodexLongGoalLogTest {
    private static final Path LOG = Path.of("docs/CODEX_LONG_GOAL_LOG.md");

    @Test
    void longGoalLogRecordsMergedPhasesAndSafetyBoundaries() throws Exception {
        String log = Files.readString(LOG, StandardCharsets.UTF_8);
        String normalized = log.toLowerCase(Locale.ROOT);

        assertTrue(log.contains("# Codex Long Goal Log"));
        assertTrue(log.contains("PR #117 merged"));
        assertTrue(log.contains("PR #118 merged"));
        assertTrue(log.contains("PR #119 merged"));
        assertTrue(log.contains("80f498c27df117bd10949683cf3c1c3a611235a6"));
        assertTrue(log.contains("ef9fe65f6dac9a50c5c8a79c2b448e2a08e6980d"));
        assertTrue(log.contains("62e1b1ef149e6fe5d4cc182046b2494503703293"));
        assertTrue(normalized.contains("runtime command remains non-executing"));
        assertTrue(normalized.contains("executor remains unwired from runtime traffic"));
        assertTrue(normalized.contains("no private-lan/public traffic"));
        assertTrue(normalized.contains("no dns/discovery/scanning/redirect-following"));
        assertTrue(normalized.contains("dry-run-only postman/smoke defaults"));
        assertTrue(normalized.contains("release-downloads/"));
        assertTrue(normalized.contains("generated evidence remains under ignored `target/` output"));
    }
}
