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

class AgentCampaignPrReadinessChecklistDocumentationTest {
    private static final Path CHECKLIST = Path.of("docs/agent/CAMPAIGN_PR_READINESS_CHECKLIST.md");
    private static final Path CAMPAIGN_ARCHITECTURE = Path.of("docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md");
    private static final Path CHECKPOINT_LEDGER = Path.of("docs/agent/CAMPAIGN_CHECKPOINT_LEDGER.md");
    private static final Path QUICKSTART = Path.of("docs/agent/AGENT_WORKFLOW_QUICKSTART.md");
    private static final Path GOAL_PROTOCOL = Path.of("docs/agent/GOAL_MODE_LONG_RUN_PROTOCOL.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignPrReadinessChecklistDocumentationTest.java");

    @Test
    void prReadinessChecklistExistsAndDefinesOpeningGate() throws Exception {
        String normalized = read(CHECKLIST).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "campaign pr readiness checklist",
                "before opening the pr",
                "one scoped pr at a time",
                "clean main before branch creation",
                "current main ci/codeql green before starting",
                "session_manager.md records",
                "failure_log.md records",
                "focused documentation guard tests passed",
                "relevant focused selector bundle passed",
                "mvn -q test",
                "mvn -q \"-dskiptests\" package",
                "mvn -b package",
                "git diff --check",
                "enterprise lab package smoke")) {
            assertTrue(normalized.contains(expected), "readiness checklist should include opening gate " + expected);
        }
    }

    @Test
    void prReadinessChecklistDefinesMergeGateAndCampaignCountingRule() throws Exception {
        String normalized = read(CHECKLIST).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "before merging the pr",
                "head sha matches the reviewed pr head sha",
                "required remote pr checks are complete and successful",
                "build/test/package/smoke is successful",
                "analyze java / codeql is successful",
                "dependency review is successful",
                "failed, cancelled, stale, pending, or duplicate-only",
                "after merge",
                "local main fast-forwards",
                "pr head is contained in main",
                "main ci/codeql is green for the merge commit")) {
            assertTrue(normalized.contains(expected), "readiness checklist should include merge gate " + expected);
        }
    }

    @Test
    void prReadinessChecklistPreservesScopeAndStopConditions() throws Exception {
        String normalized = read(CHECKLIST).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no production code",
                "maven config",
                "ci/workflow",
                "dockerfile",
                "compose behavior",
                "runtime behavior",
                "endpoint behavior",
                "scripts",
                "secrets",
                "external/cloud/tenant targets",
                "automation",
                "pause the campaign",
                "a human decision is needed")) {
            assertTrue(normalized.contains(expected), "readiness checklist should preserve scope or stop rule " + expected);
        }
    }

    @Test
    void prReadinessChecklistPreservesNotProvenBoundaries() throws Exception {
        String normalized = read(CHECKLIST).toLowerCase(Locale.ROOT);

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
            assertTrue(normalized.contains(expected), "readiness checklist should preserve " + expected);
        }
    }

    @Test
    void relatedAgentDocsCrossLinkPrReadinessChecklist() throws Exception {
        for (Path path : List.of(
                CAMPAIGN_ARCHITECTURE,
                CHECKPOINT_LEDGER,
                QUICKSTART,
                GOAL_PROTOCOL,
                SESSION_MANAGER)) {
            assertTrue(read(path).contains("CAMPAIGN_PR_READINESS_CHECKLIST.md"),
                    path + " should link to the campaign PR readiness checklist");
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
