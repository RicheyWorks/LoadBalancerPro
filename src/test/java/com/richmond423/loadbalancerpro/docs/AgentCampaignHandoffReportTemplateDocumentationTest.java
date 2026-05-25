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

class AgentCampaignHandoffReportTemplateDocumentationTest {
    private static final Path TEMPLATE = Path.of("docs/agent/CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md");
    private static final Path CAMPAIGN_ARCHITECTURE = Path.of("docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md");
    private static final Path CHECKPOINT_LEDGER = Path.of("docs/agent/CAMPAIGN_CHECKPOINT_LEDGER.md");
    private static final Path PR_READINESS = Path.of("docs/agent/CAMPAIGN_PR_READINESS_CHECKLIST.md");
    private static final Path SCOPE_AUDIT = Path.of("docs/agent/CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md");
    private static final Path REMOTE_AUDIT = Path.of("docs/agent/CAMPAIGN_REMOTE_CHECK_AUDIT.md");
    private static final Path MERGE_GATE = Path.of("docs/agent/CAMPAIGN_MERGE_GATE.md");
    private static final Path FAILURE_PLAYBOOK = Path.of("docs/agent/CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md");
    private static final Path QUICKSTART = Path.of("docs/agent/AGENT_WORKFLOW_QUICKSTART.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path FAILURE_LOG = Path.of("docs/agent/FAILURE_LOG.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignHandoffReportTemplateDocumentationTest.java");

    @Test
    void handoffTemplateExistsAndDefinesPurpose() throws Exception {
        String normalized = read(TEMPLATE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "campaign handoff report template",
                "long-running codex `/goal` campaigns",
                "documentation only",
                "without relying on chat memory",
                "current campaign count",
                "current branch",
                "current pr",
                "current head sha",
                "current main sha",
                "next safe action")) {
            assertTrue(normalized.contains(expected), "template should define " + expected);
        }
    }

    @Test
    void handoffTemplateRequiresVerificationAndRemoteStatusFields() throws Exception {
        String normalized = read(TEMPLATE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "focused checks run",
                "full local verification",
                "package checks",
                "diff checks",
                "smoke checks",
                "remote pr checks",
                "main post-merge checks",
                "report only checks that actually ran",
                "do not claim green main while remote checks are pending",
                "failed, cancelled, stale, pending, skipped-only, or duplicate-only")) {
            assertTrue(normalized.contains(expected), "template should require verification field " + expected);
        }
    }

    @Test
    void handoffTemplateRequiresFailureLogAndSessionManagerAlignment() throws Exception {
        String normalized = read(TEMPLATE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "failure_log.md",
                "failure type",
                "failing command, test, run, or job",
                "suspected cause",
                "fix attempted",
                "follow-up action",
                "last known good branch or main sha",
                "session_manager.md",
                "blocker",
                "checks run")) {
            assertTrue(normalized.contains(expected), "template should require failure or session field " + expected);
        }
    }

    @Test
    void handoffTemplatePreservesScopeSafetyAndNotProvenBoundaries() throws Exception {
        String normalized = read(TEMPLATE).toLowerCase(Locale.ROOT);

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
            assertTrue(normalized.contains(expected), "template should preserve " + expected);
        }
    }

    @Test
    void relatedAgentDocsCrossLinkHandoffReportTemplate() throws Exception {
        for (Path path : List.of(
                CAMPAIGN_ARCHITECTURE,
                CHECKPOINT_LEDGER,
                PR_READINESS,
                SCOPE_AUDIT,
                REMOTE_AUDIT,
                MERGE_GATE,
                FAILURE_PLAYBOOK,
                QUICKSTART,
                SESSION_MANAGER,
                FAILURE_LOG)) {
            assertTrue(read(path).contains("CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md"),
                    path + " should link to the campaign handoff report template");
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
