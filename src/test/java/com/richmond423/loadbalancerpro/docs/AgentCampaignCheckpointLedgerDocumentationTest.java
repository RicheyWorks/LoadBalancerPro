package com.richmond423.loadbalancerpro.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class AgentCampaignCheckpointLedgerDocumentationTest {
    private static final Path LEDGER = Path.of("docs/agent/CAMPAIGN_CHECKPOINT_LEDGER.md");
    private static final Path CAMPAIGN_ARCHITECTURE = Path.of("docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md");
    private static final Path QUICKSTART = Path.of("docs/agent/AGENT_WORKFLOW_QUICKSTART.md");
    private static final Path GOAL_PROTOCOL = Path.of("docs/agent/GOAL_MODE_LONG_RUN_PROTOCOL.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path FAILURE_LOG = Path.of("docs/agent/FAILURE_LOG.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignCheckpointLedgerDocumentationTest.java");

    @Test
    void checkpointLedgerExistsAndDefinesCampaignStateFields() throws Exception {
        String ledger = read(LEDGER);

        for (String expected : List.of(
                "Campaign Checkpoint Ledger",
                "current campaign objective",
                "current PR number",
                "current branch",
                "current PR URL",
                "current head SHA",
                "changed files",
                "focused checks",
                "full local verification",
                "remote PR checks",
                "post-merge main checks",
                "blockers",
                "next action")) {
            assertTrue(ledger.contains(expected), "checkpoint ledger should contain " + expected);
        }
    }

    @Test
    void checkpointLedgerDefinesTenPrCountingRule() throws Exception {
        String normalized = read(LEDGER).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "count increases only after a pr has merged",
                "local main has fast-forwarded",
                "post-merge local checks have passed",
                "main ci/codeql is green",
                "does not count as a successful merged campaign pr")) {
            assertTrue(normalized.contains(expected), "checkpoint ledger should define counting rule " + expected);
        }
    }

    @Test
    void checkpointLedgerPreservesFailureAndStopRules() throws Exception {
        String normalized = read(LEDGER).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "record the failure in failure_log.md before pausing",
                "last known good sha",
                "required checks fail, are cancelled, are stale, or remain pending",
                "main ci/codeql is red",
                "a human decision is needed",
                "pause instead of continuing")) {
            assertTrue(normalized.contains(expected), "checkpoint ledger should preserve stop rule " + expected);
        }
    }

    @Test
    void checkpointLedgerPreservesNotProvenBoundaries() throws Exception {
        String normalized = read(LEDGER).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "checkpoint ledger should preserve " + expected);
        }
    }

    @Test
    void relatedAgentDocsCrossLinkCheckpointLedger() throws Exception {
        for (Path path : List.of(
                CAMPAIGN_ARCHITECTURE,
                QUICKSTART,
                GOAL_PROTOCOL,
                SESSION_MANAGER,
                FAILURE_LOG)) {
            assertTrue(read(path).contains("CAMPAIGN_CHECKPOINT_LEDGER.md"),
                    path + " should link to the campaign checkpoint ledger");
        }
    }

    @Test
    void guardTestOnlyReadsTrackedFiles() throws Exception {
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
