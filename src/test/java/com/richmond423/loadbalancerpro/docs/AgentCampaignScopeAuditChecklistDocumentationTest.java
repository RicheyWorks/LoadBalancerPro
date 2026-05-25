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

class AgentCampaignScopeAuditChecklistDocumentationTest {
    private static final Path CHECKLIST = Path.of("docs/agent/CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md");
    private static final Path CAMPAIGN_ARCHITECTURE = Path.of("docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md");
    private static final Path CHECKPOINT_LEDGER = Path.of("docs/agent/CAMPAIGN_CHECKPOINT_LEDGER.md");
    private static final Path PR_READINESS = Path.of("docs/agent/CAMPAIGN_PR_READINESS_CHECKLIST.md");
    private static final Path REMOTE_AUDIT = Path.of("docs/agent/CAMPAIGN_REMOTE_CHECK_AUDIT.md");
    private static final Path MERGE_GATE = Path.of("docs/agent/CAMPAIGN_MERGE_GATE.md");
    private static final Path FAILURE_PLAYBOOK = Path.of("docs/agent/CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md");
    private static final Path QUICKSTART = Path.of("docs/agent/AGENT_WORKFLOW_QUICKSTART.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path FAILURE_LOG = Path.of("docs/agent/FAILURE_LOG.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignScopeAuditChecklistDocumentationTest.java");

    @Test
    void scopeAuditChecklistExistsAndDefinesCampaignScopeAudit() throws Exception {
        String normalized = read(CHECKLIST).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "campaign scope audit checklist",
                "multi-pr codex `/goal` campaign",
                "started from clean main",
                "current branch and head sha",
                "changed files match the active pr contract",
                "session_manager.md",
                "failure_log.md",
                "pause instead of improvising")) {
            assertTrue(normalized.contains(expected), "scope audit should define " + expected);
        }
    }

    @Test
    void scopeAuditChecklistDefinesChangedFileAndForbiddenScopeRules() throws Exception {
        String normalized = read(CHECKLIST).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "git diff --name-status origin/main...head",
                "git diff --stat origin/main...head",
                "markdown documentation",
                "documentation guard tests under `src/test/java`",
                "src/main/java",
                "maven config",
                "ci/workflow",
                "dockerfile",
                "local compose behavior",
                "runtime resources",
                "endpoint behavior",
                "k6 behavior",
                "bruno behavior",
                "toxiproxy behavior",
                "secrets or credentials",
                "external/cloud/tenant targets")) {
            assertTrue(normalized.contains(expected), "scope audit should name scope rule " + expected);
        }
    }

    @Test
    void scopeAuditChecklistPreservesClaimAuditAndNotProvenBoundaries() throws Exception {
        String normalized = read(CHECKLIST).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no production readiness claim",
                "no production certification claim",
                "no live-cloud validation claim",
                "no real-tenant validation claim",
                "no runtime enforcement claim",
                "no load/stress/benchmarking claim",
                "no throughput/p95/p99 evidence claim",
                "no replay/evidence/report/storage/export proof claim",
                "no broader automation claim",
                "does not prove production readiness",
                "does not prove production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "scope audit should preserve " + expected);
        }
    }

    @Test
    void scopeAuditChecklistDefinesStopConditionsAndGuardTestBoundaries() throws Exception {
        String normalized = read(CHECKLIST).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "the changed-file audit finds an unexpected path",
                "the diff expands beyond the active pr contract",
                "safety boundary language is weakened",
                "a guard test must be loosened to pass",
                "remote checks are failed, cancelled, stale, queued, in-progress, pending, skipped-only, or duplicate-only",
                "main ci/codeql is red",
                "do the guard tests only read tracked files",
                "start servers",
                "run docker",
                "run compose",
                "call network endpoints",
                "execute processes",
                "write files")) {
            assertTrue(normalized.contains(expected), "scope audit should define stop or guard boundary " + expected);
        }
    }

    @Test
    void relatedAgentDocsCrossLinkScopeAuditChecklist() throws Exception {
        for (Path path : List.of(
                CAMPAIGN_ARCHITECTURE,
                CHECKPOINT_LEDGER,
                PR_READINESS,
                REMOTE_AUDIT,
                MERGE_GATE,
                FAILURE_PLAYBOOK,
                QUICKSTART,
                SESSION_MANAGER,
                FAILURE_LOG)) {
            assertTrue(read(path).contains("CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md"),
                    path + " should link to the campaign scope audit checklist");
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
