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

class AgentCampaignMergeGateDocumentationTest {
    private static final Path MERGE_GATE = Path.of("docs/agent/CAMPAIGN_MERGE_GATE.md");
    private static final Path CAMPAIGN_ARCHITECTURE = Path.of("docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md");
    private static final Path CHECKPOINT_LEDGER = Path.of("docs/agent/CAMPAIGN_CHECKPOINT_LEDGER.md");
    private static final Path PR_READINESS = Path.of("docs/agent/CAMPAIGN_PR_READINESS_CHECKLIST.md");
    private static final Path REMOTE_AUDIT = Path.of("docs/agent/CAMPAIGN_REMOTE_CHECK_AUDIT.md");
    private static final Path QUICKSTART = Path.of("docs/agent/AGENT_WORKFLOW_QUICKSTART.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignMergeGateDocumentationTest.java");

    @Test
    void mergeGateExistsAndDefinesPreMergeDecisionRules() throws Exception {
        String normalized = read(MERGE_GATE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "campaign merge gate",
                "merge decision gate",
                "current pr head sha matches the reviewed head sha",
                "branch head sha is recorded in session_manager.md",
                "diff remains inside the current pr contract",
                "focused local checks passed",
                "relevant focused selector bundle passed",
                "mvn -q test",
                "mvn -q \"-dskiptests\" package",
                "mvn -b package",
                "git diff --check",
                "enterprise lab package smoke")) {
            assertTrue(normalized.contains(expected), "merge gate should include rule " + expected);
        }
    }

    @Test
    void mergeGateDefinesCurrentHeadRemoteChecksAndMergeMethod() throws Exception {
        String normalized = read(MERGE_GATE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "current pr head sha",
                "build/test/package/smoke",
                "analyze java / codeql",
                "dependency review",
                "failed",
                "cancelled",
                "stale",
                "queued",
                "in-progress",
                "pending",
                "skipped-only",
                "duplicate-only",
                "normal github pr merge commit",
                "do not squash",
                "do not rebase",
                "do not delete the branch")) {
            assertTrue(normalized.contains(expected), "merge gate should define remote or merge rule " + expected);
        }
    }

    @Test
    void mergeGateDefinesPostMergeMainCountingRule() throws Exception {
        String normalized = read(MERGE_GATE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "post-merge main gate",
                "does not count until",
                "local main fast-forwards",
                "merged pr head is contained in main",
                "post-merge focused guard checks pass",
                "post-merge full local verification passes",
                "main build/test/package/smoke is successful",
                "main analyze java / codeql is successful",
                "record the merge commit",
                "new main head")) {
            assertTrue(normalized.contains(expected), "merge gate should define post-merge rule " + expected);
        }
    }

    @Test
    void mergeGatePreservesScopeStopConditionsAndNotProvenBoundaries() throws Exception {
        String normalized = read(MERGE_GATE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no production code",
                "maven config",
                "ci/workflow",
                "dockerfile",
                "compose behavior",
                "runtime behavior",
                "endpoint behavior",
                "secrets",
                "external/cloud/tenant targets",
                "automation",
                "pause the campaign",
                "failure_log.md",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "merge gate should preserve " + expected);
        }
    }

    @Test
    void relatedAgentDocsCrossLinkMergeGate() throws Exception {
        for (Path path : List.of(
                CAMPAIGN_ARCHITECTURE,
                CHECKPOINT_LEDGER,
                PR_READINESS,
                REMOTE_AUDIT,
                QUICKSTART,
                SESSION_MANAGER)) {
            assertTrue(read(path).contains("CAMPAIGN_MERGE_GATE.md"),
                    path + " should link to the campaign merge gate");
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
