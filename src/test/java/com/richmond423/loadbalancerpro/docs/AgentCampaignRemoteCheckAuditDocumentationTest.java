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

class AgentCampaignRemoteCheckAuditDocumentationTest {
    private static final Path AUDIT = Path.of("docs/agent/CAMPAIGN_REMOTE_CHECK_AUDIT.md");
    private static final Path CAMPAIGN_ARCHITECTURE = Path.of("docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md");
    private static final Path CHECKPOINT_LEDGER = Path.of("docs/agent/CAMPAIGN_CHECKPOINT_LEDGER.md");
    private static final Path PR_READINESS = Path.of("docs/agent/CAMPAIGN_PR_READINESS_CHECKLIST.md");
    private static final Path QUICKSTART = Path.of("docs/agent/AGENT_WORKFLOW_QUICKSTART.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignRemoteCheckAuditDocumentationTest.java");

    @Test
    void remoteCheckAuditExistsAndDefinesPrHeadAuditFields() throws Exception {
        String normalized = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "campaign remote check audit",
                "expected pr head sha",
                "current pr head sha",
                "base branch",
                "draft state",
                "mergeability",
                "required check names",
                "latest check run status",
                "latest check run conclusion",
                "run trigger and branch")) {
            assertTrue(normalized.contains(expected), "remote check audit should include field " + expected);
        }
    }

    @Test
    void remoteCheckAuditDefinesRequiredPrAndMainChecks() throws Exception {
        String normalized = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "build/test/package/smoke",
                "analyze java / codeql",
                "dependency review",
                "current-head checks",
                "main codeql is successful",
                "main build/test/package/smoke is successful",
                "merge commit",
                "do not count the campaign pr until main remote checks are green")) {
            assertTrue(normalized.contains(expected), "remote check audit should define required check " + expected);
        }
    }

    @Test
    void remoteCheckAuditRejectsUnacceptableCheckStates() throws Exception {
        String normalized = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "failed",
                "cancelled",
                "stale",
                "skipped-only",
                "duplicate-only",
                "queued",
                "in-progress",
                "pending",
                "do not merge",
                "failure_log.md before pausing")) {
            assertTrue(normalized.contains(expected), "remote check audit should reject " + expected);
        }
    }

    @Test
    void remoteCheckAuditPreservesNotProvenBoundaries() throws Exception {
        String normalized = read(AUDIT).toLowerCase(Locale.ROOT);

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
            assertTrue(normalized.contains(expected), "remote check audit should preserve " + expected);
        }
    }

    @Test
    void relatedAgentDocsCrossLinkRemoteCheckAudit() throws Exception {
        for (Path path : List.of(
                CAMPAIGN_ARCHITECTURE,
                CHECKPOINT_LEDGER,
                PR_READINESS,
                QUICKSTART,
                SESSION_MANAGER)) {
            assertTrue(read(path).contains("CAMPAIGN_REMOTE_CHECK_AUDIT.md"),
                    path + " should link to the campaign remote check audit");
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
