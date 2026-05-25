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

class AgentCampaignCloseoutProtocolDocumentationTest {
    private static final Path CLOSEOUT = Path.of("docs/agent/CAMPAIGN_CLOSEOUT_PROTOCOL.md");
    private static final Path CAMPAIGN_ARCHITECTURE = Path.of("docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md");
    private static final Path CHECKPOINT_LEDGER = Path.of("docs/agent/CAMPAIGN_CHECKPOINT_LEDGER.md");
    private static final Path PR_READINESS = Path.of("docs/agent/CAMPAIGN_PR_READINESS_CHECKLIST.md");
    private static final Path SCOPE_AUDIT = Path.of("docs/agent/CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md");
    private static final Path REMOTE_AUDIT = Path.of("docs/agent/CAMPAIGN_REMOTE_CHECK_AUDIT.md");
    private static final Path MERGE_GATE = Path.of("docs/agent/CAMPAIGN_MERGE_GATE.md");
    private static final Path FAILURE_PLAYBOOK = Path.of("docs/agent/CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md");
    private static final Path HANDOFF = Path.of("docs/agent/CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md");
    private static final Path QUICKSTART = Path.of("docs/agent/AGENT_WORKFLOW_QUICKSTART.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignCloseoutProtocolDocumentationTest.java");

    @Test
    void closeoutProtocolExistsAndDefinesCompletion() throws Exception {
        String normalized = read(CLOSEOUT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "campaign closeout protocol",
                "long-running codex `/goal` campaign",
                "documentation only",
                "target count is ten successful merged prs",
                "normal merge commit",
                "local main was fast-forwarded",
                "main ci and codeql passed for the merge commit",
                "do not count a pr")) {
            assertTrue(normalized.contains(expected), "closeout protocol should define " + expected);
        }
    }

    @Test
    void closeoutProtocolRequiresFinalReportAndCountingFields() throws Exception {
        String normalized = read(CLOSEOUT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "target count",
                "completed count",
                "final main sha",
                "final pr",
                "final merge commit",
                "merged prs",
                "pr head sha",
                "remote pr checks",
                "main post-merge checks",
                "failures logged",
                "remaining not-proven boundaries",
                "next recommended action")) {
            assertTrue(normalized.contains(expected), "closeout protocol should require " + expected);
        }
    }

    @Test
    void closeoutProtocolRequiresFinalVerificationGate() throws Exception {
        String normalized = read(CLOSEOUT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "final focused guard selector",
                "campaign guard selector bundle",
                "mvn -q test",
                "mvn -q \"-dskiptests\" package",
                "mvn -b package",
                "git diff --check",
                "git diff --cached --check",
                "enterprise-lab-workflow.ps1 -package",
                "do not claim green main while remote checks are pending",
                "failed, cancelled, stale, pending, skipped-only, or duplicate-only")) {
            assertTrue(normalized.contains(expected), "closeout protocol should require final gate item " + expected);
        }
    }

    @Test
    void closeoutProtocolPreservesScopeAndNotProvenBoundaries() throws Exception {
        String normalized = read(CLOSEOUT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "docs/test-only",
                "no production code changes",
                "no maven config changes",
                "no ci/workflow changes",
                "no dockerfile changes",
                "no compose behavior changes",
                "no runtime behavior changes",
                "no endpoint behavior changes",
                "no k6, bruno, or toxiproxy behavior changes",
                "secrets, external/cloud/tenant targets, or automation",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "closeout protocol should preserve " + expected);
        }
    }

    @Test
    void campaignDocsCrossLinkCloseoutProtocol() throws Exception {
        for (Path path : List.of(
                CAMPAIGN_ARCHITECTURE,
                CHECKPOINT_LEDGER,
                PR_READINESS,
                SCOPE_AUDIT,
                REMOTE_AUDIT,
                MERGE_GATE,
                FAILURE_PLAYBOOK,
                HANDOFF,
                QUICKSTART,
                SESSION_MANAGER)) {
            assertTrue(read(path).contains("CAMPAIGN_CLOSEOUT_PROTOCOL.md"),
                    path + " should link to the campaign closeout protocol");
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
