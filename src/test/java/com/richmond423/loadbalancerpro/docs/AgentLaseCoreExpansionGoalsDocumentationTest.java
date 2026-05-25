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

class AgentLaseCoreExpansionGoalsDocumentationTest {
    private static final Path LEDGER = Path.of("docs/agent/LASE_CORE_EXPANSION_GOALS.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentLaseCoreExpansionGoalsDocumentationTest.java");

    @Test
    void laseCoreExpansionGoalLedgerExistsAndDefinesCampaign() throws IOException {
        String normalized = read(LEDGER).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "lase core expansion campaign",
                "classification: warn",
                "load-aware synthetic evaluation",
                "adaptive routing intelligence",
                "one goal means one small pr by default",
                "goal states",
                "planned",
                "pr-opened",
                "merged/main-green",
                "paused/warn")) {
            assertTrue(normalized.contains(expected), "ledger should define " + expected);
        }
    }

    @Test
    void goalLedgerContainsAtLeastTwentyGoalIdsAndRequiredFields() throws IOException {
        String content = read(LEDGER);
        Matcher matcher = Pattern.compile("LASE-G\\d{2}").matcher(content);
        Set<String> goalIds = new java.util.TreeSet<>();
        while (matcher.find()) {
            goalIds.add(matcher.group());
        }

        assertTrue(goalIds.size() >= 20, "ledger should contain at least 20 unique LASE-Gxx goals");

        String normalized = content.toLowerCase(Locale.ROOT);
        for (String expected : List.of(
                "proposed pr title",
                "primary scope",
                "expected changed areas",
                "success criteria",
                "verification expectations",
                "not-proven boundaries",
                "dependencies",
                "initial status")) {
            assertTrue(normalized.contains(expected), "ledger should contain goal field " + expected);
        }
    }

    @Test
    void goalLedgerRecordsPr327AsOpenFirstImplementationSlice() throws IOException {
        String normalized = read(LEDGER).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "pr #327",
                "https://github.com/richeyworks/loadbalancerpro/pull/327",
                "slot 12",
                "7a6779b96d97e5701a3654d94c1a1d60c04018cb",
                "pr #327 state: open",
                "pr #327 is not marked merged",
                "tail-latency score breakdown and explanation")) {
            assertTrue(normalized.contains(expected), "ledger should preserve PR #327 fact: " + expected);
        }
    }

    @Test
    void goalLedgerPreservesVerificationAndRemoteCheckRules() throws IOException {
        String normalized = read(LEDGER).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "focused",
                "selector bundle",
                "full maven tests",
                "package checks",
                "diff checks",
                "enterprise lab package smoke",
                "remote pr checks",
                "post-merge main checks",
                "failed, cancelled, stale, pending, missing, or duplicate-only required checks are not acceptable",
                "do not claim green main while required remote checks are pending")) {
            assertTrue(normalized.contains(expected), "ledger should preserve verification rule: " + expected);
        }
    }

    @Test
    void goalLedgerPreservesNotProvenBoundariesAndAvoidsOverclaims() throws IOException {
        String normalized = read(LEDGER).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no production readiness",
                "no production certification",
                "no live-cloud validation",
                "no real-tenant validation",
                "no runtime enforcement",
                "no load/stress/benchmarking evidence",
                "no throughput/p95/p99 production evidence",
                "no replay/evidence/report/storage/export proof",
                "no broader automation")) {
            assertTrue(normalized.contains(expected), "ledger should preserve boundary: " + expected);
        }

        for (String forbidden : List.of(
                "production ready",
                "certified",
                "guaranteed p99",
                "real tenant validated",
                "live-cloud validated",
                "benchmark proven")) {
            assertFalse(normalized.contains(forbidden), "ledger must not overclaim: " + forbidden);
        }
    }

    @Test
    void readmeAndReviewerTrustMapLinkToLaseCoreExpansionGoals() throws IOException {
        assertTrue(read(README).contains("docs/agent/LASE_CORE_EXPANSION_GOALS.md"),
                "README should link to the LASE core expansion goal ledger");
        assertTrue(read(TRUST_MAP).contains("agent/LASE_CORE_EXPANSION_GOALS.md"),
                "Reviewer Trust Map should link to the LASE core expansion goal ledger");
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
