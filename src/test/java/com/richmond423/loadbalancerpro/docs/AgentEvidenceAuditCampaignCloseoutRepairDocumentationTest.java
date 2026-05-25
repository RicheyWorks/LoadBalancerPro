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

class AgentEvidenceAuditCampaignCloseoutRepairDocumentationTest {
    private static final Path CONTRACT = Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_CONTRACT.md");
    private static final Path BOARD = Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md");
    private static final Path CHECKPOINT =
            Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_CHECKPOINT_TEMPLATE.md");
    private static final Path FINAL_TEMPLATE =
            Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_FINAL_REPORT_TEMPLATE.md");
    private static final Path GOAL_BOARD = Path.of("docs/agent/GOAL_CAMPAIGN_BOARD.md");
    private static final Path FINAL_HANDOFF =
            Path.of("docs/agent/GOAL_CAMPAIGN_FINAL_HANDOFF_REPORT.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path README = Path.of("README.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentEvidenceAuditCampaignCloseoutRepairDocumentationTest.java");

    @Test
    void auditCampaignArchitectureExistsAndDefinesTwentySlots() throws IOException {
        String contract = read(CONTRACT).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String checkpoint = read(CHECKPOINT).toLowerCase(Locale.ROOT);
        String finalTemplate = read(FINAL_TEMPLATE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "loadbalancerpro 20-pr evidence audit and closeout repair campaign",
                "audit the current github repository state",
                "one scoped pr at a time",
                "20 merged prs",
                "prior 10-pr closeout repair",
                "open pr hygiene audit",
                "repository evidence map",
                "ci workflow audit",
                "final 20-pr audit closeout",
                "session_manager.md after",
                "failure_log.md before continuing",
                "failed, cancelled, stale, pending, missing, or duplicate-only required checks")) {
            assertTrue(contract.contains(expected) || board.contains(expected) || checkpoint.contains(expected)
                    || finalTemplate.contains(expected), "Missing audit campaign wording: " + expected);
        }

        assertTrue(board.contains("completed campaign prs: 0 / 20"));
        assertTrue(board.contains("current pr slot: 1"));
        assertTrue(board.contains("codex/evidence-audit-closeout-repair"));
    }

    @Test
    void priorTenPrCloseoutIsRepairedWithVerifiedMergeFacts() throws IOException {
        String goalBoard = read(GOAL_BOARD).toLowerCase(Locale.ROOT);
        String handoff = read(FINAL_HANDOFF).toLowerCase(Locale.ROOT);
        String session = read(SESSION).toLowerCase(Locale.ROOT);
        String readme = read(README).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "completed campaign prs: 10 / 10",
                "current pr slot: completed",
                "pr #315 is merged",
                "99934cd6f511f535cc70e316a5c8f306fd643745",
                "c27dc5a8da365f9b64ab13e671d9dad07f0f2f01",
                "main ci/codeql",
                "slot 10 merged and main green",
                "completed loadbalancerpro goal mode 10-pr trial",
                "final post-merge trial closeout")) {
            assertTrue(goalBoard.contains(expected) || handoff.contains(expected) || session.contains(expected)
                    || readme.contains(expected), "Missing repaired closeout fact: " + expected);
        }
    }

    @Test
    void stalePendingLanguageIsRejectedForPriorCloseout() throws IOException {
        List<String> closeoutDocs = List.of(
                read(GOAL_BOARD).toLowerCase(Locale.ROOT),
                read(FINAL_HANDOFF).toLowerCase(Locale.ROOT),
                read(SESSION).toLowerCase(Locale.ROOT),
                read(README).toLowerCase(Locale.ROOT));

        for (String doc : closeoutDocs) {
            for (String stale : List.of(
                    "completed campaign prs: 9 / 10",
                    "final checkpoint head pending remote audit",
                    "pr #315 checks in progress",
                    "current-head remote checks pending",
                    "the current loadbalancerpro goal mode 10-pr trial")) {
                assertFalse(doc.contains(stale), "Stale prior-campaign state should be repaired: " + stale);
            }
        }
    }

    @Test
    void auditCampaignPreservesScopeAndNotProvenBoundaries() throws IOException {
        String combined = String.join("\n",
                read(CONTRACT),
                read(BOARD),
                read(CHECKPOINT),
                read(FINAL_TEMPLATE)).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "docs/test-only",
                "src/main/java",
                "maven config",
                "ci/workflow",
                "dockerfile",
                "compose behavior",
                "runtime behavior",
                "endpoints",
                "runner services",
                "automation",
                "secrets",
                "external/cloud/tenant targets",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "broader automation")) {
            assertTrue(combined.contains(expected), "Missing audit scope or boundary: " + expected);
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
