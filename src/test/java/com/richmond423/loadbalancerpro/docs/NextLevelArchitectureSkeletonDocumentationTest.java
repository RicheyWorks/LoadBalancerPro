package com.richmond423.loadbalancerpro.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class NextLevelArchitectureSkeletonDocumentationTest {
    private static final Path ARCHITECTURE = Path.of(
            "docs/architecture/LOADBALANCERPRO_NEXT_LEVEL_ARCHITECTURE.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "NextLevelArchitectureSkeletonDocumentationTest.java");

    @Test
    void architectureSkeletonExistsAndDefinesControlPlanePurpose() throws IOException {
        String content = read(ARCHITECTURE);
        String normalized = content.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "loadbalancerpro next-level architecture campaign skeleton",
                "living skeleton / architecture control plane",
                "repository target",
                "richeyworks/loadbalancerpro",
                "one scoped pr at a time",
                "architecture north star",
                "target architecture layers")) {
            assertTrue(normalized.contains(expected), "architecture skeleton should contain " + expected);
        }
    }

    @Test
    void architectureSkeletonPreservesWarnAndNotProvenBoundaries() throws IOException {
        String normalized = read(ARCHITECTURE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "warn",
                "prove or authorize claims",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmark evidence",
                "throughput, p95, or p99 production evidence",
                "replay/evidence/report/storage/export proof",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "architecture skeleton should preserve boundary " + expected);
        }
    }

    @Test
    void architectureSkeletonDefinesFiveHundredSlotCampaignModel() throws IOException {
        String content = read(ARCHITECTURE);
        String normalized = content.toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("500 pr-sized slots"), "architecture skeleton should define 500 slots");
        assertTrue(normalized.contains("50 ten-pr campaigns"), "architecture skeleton should define 50 campaigns");
        assertTrue(normalized.contains("campaign model: 500 pr-sized slots"),
                "architecture skeleton should include the campaign model section");

        Matcher matcher = Pattern.compile("\\| C\\d{2} \\|").matcher(content);
        Set<String> campaignIds = new java.util.TreeSet<>();
        while (matcher.find()) {
            campaignIds.add(matcher.group());
        }

        assertTrue(campaignIds.size() >= 50, "architecture skeleton should list at least 50 campaigns");
    }

    @Test
    void architectureSkeletonIncludesDailyUpdateResearchAndAdrTemplates() throws IOException {
        String normalized = read(ARCHITECTURE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "daily multi-agent update protocol",
                "daily update - yyyy-mm-dd",
                "research source card template",
                "src-yyyymmdd-###",
                "architecture decision record template",
                "adr-###",
                "decision: candidate / accepted / rejected / superseded / archived",
                "verification needed before claim")) {
            assertTrue(normalized.contains(expected), "architecture skeleton should contain template item " + expected);
        }
    }

    @Test
    void architectureSkeletonKeepsFutureSlotsPlanningOnly() throws IOException {
        String normalized = read(ARCHITECTURE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "slot ids are planning ids",
                "research is not proof",
                "this document is the architecture map, not the evidence itself",
                "implementation and verification are the only claim upgrade path",
                "merged/main-green")) {
            assertTrue(normalized.contains(expected), "architecture skeleton should keep planning boundary " + expected);
        }
    }

    @Test
    void architectureSkeletonDoesNotUseForbiddenOverclaimPhrases() throws IOException {
        String normalized = read(ARCHITECTURE).toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "production ready",
                "production readiness is proven",
                "certified production",
                "guaranteed p99",
                "real tenant validated",
                "live-cloud validated",
                "benchmark proven",
                "load test proven",
                "stress test proven",
                "broader automation is implemented")) {
            assertFalse(normalized.contains(forbidden), "architecture skeleton must not overclaim " + forbidden);
        }
    }

    @Test
    void guardTestOnlyReadsTrackedFiles() throws IOException {
        String source = read(SOURCE);

        for (String forbidden : List.of(
                "Files." + "write",
                "Files." + "create",
                "Files." + "delete",
                "Process" + "Builder",
                "Runtime." + "getRuntime",
                ".ex" + "ec(",
                "Http" + "Client",
                "URL" + "Connection",
                "Socket" + "(")) {
            assertFalse(source.contains(forbidden), "guard test must not use " + forbidden);
        }
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), path + " should exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
