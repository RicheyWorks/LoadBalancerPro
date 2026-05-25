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

class AgentEvidenceAuditOpenPrHygieneDocumentationTest {
    private static final Path HYGIENE = Path.of("docs/agent/EVIDENCE_AUDIT_OPEN_PR_HYGIENE.md");
    private static final Path BOARD = Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentEvidenceAuditOpenPrHygieneDocumentationTest.java");

    @Test
    void openPrHygieneDocRecordsPr291WithoutTakingAction() throws IOException {
        String hygiene = read(HYGIENE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "slot 2",
                "open pr hygiene",
                "#291",
                "richeyworks-patch-2",
                "8342f39dedeb29f97b55be7874e3ee5e3ca9a057",
                "dirty",
                "conflicting",
                "stale or superseded",
                "do not close or modify",
                "human review",
                "fresh, separately scoped readme wording pr")) {
            assertTrue(hygiene.contains(expected), "Missing PR #291 hygiene finding: " + expected);
        }
    }

    @Test
    void openPrInventoryCoversEveryAuditedOpenPr() throws IOException {
        String hygiene = read(HYGIENE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "#291",
                "#271",
                "#238",
                "#216",
                "#182",
                "#179",
                "#178",
                "#168",
                "#167",
                "dependabot",
                "dependency pr",
                "workflow action versions",
                "docker base image digest",
                "older cockpit polish pr")) {
            assertTrue(hygiene.contains(expected), "Missing open PR inventory detail: " + expected);
        }
    }

    @Test
    void slotBoardAndSessionPreserveSlotTwoHistory() throws IOException {
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "codex/evidence-audit-open-pr-hygiene",
                "pr #316 merged",
                "4622d788569fc68de1fab212cdad388d2cf10dc8",
                "#317",
                "08e3320e6b5413d372249b7886876341af1529e6",
                "7dd64becaefd589ff94ed2fea93b017397b4a747",
                "open pr hygiene audit",
                "post-merge main ci and codeql green")) {
            assertTrue(board.contains(expected) || session.contains(expected),
                    "Missing slot 2 campaign state: " + expected);
        }
    }

    @Test
    void hygieneDocPreservesScopeAndNotProvenBoundaries() throws IOException {
        String hygiene = read(HYGIENE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "documentation/test-only",
                "does not close",
                "do not merge",
                "pending, failed, cancelled, stale, duplicate-only, missing, or ambiguous required checks",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "broader automation")) {
            assertTrue(hygiene.contains(expected), "Missing hygiene boundary: " + expected);
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
