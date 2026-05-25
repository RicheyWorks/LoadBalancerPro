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

class AgentCampaignSystemIndexDocumentationTest {
    private static final Path INDEX = Path.of("docs/agent/CAMPAIGN_SYSTEM_INDEX.md");
    private static final Path README = Path.of("README.md");
    private static final Path AGENTS = Path.of("AGENTS.md");
    private static final Path BUILD_CONTRACT = Path.of("BUILD_CONTRACT.md");
    private static final Path QUICKSTART = Path.of("docs/agent/AGENT_WORKFLOW_QUICKSTART.md");
    private static final Path GOAL_PROTOCOL = Path.of("docs/agent/GOAL_MODE_LONG_RUN_PROTOCOL.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path ARCHITECTURE = Path.of("docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md");
    private static final Path CHECKPOINT_LEDGER = Path.of("docs/agent/CAMPAIGN_CHECKPOINT_LEDGER.md");
    private static final Path PR_READINESS = Path.of("docs/agent/CAMPAIGN_PR_READINESS_CHECKLIST.md");
    private static final Path SCOPE_AUDIT = Path.of("docs/agent/CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md");
    private static final Path REMOTE_AUDIT = Path.of("docs/agent/CAMPAIGN_REMOTE_CHECK_AUDIT.md");
    private static final Path MERGE_GATE = Path.of("docs/agent/CAMPAIGN_MERGE_GATE.md");
    private static final Path FAILURE_PLAYBOOK = Path.of("docs/agent/CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md");
    private static final Path HANDOFF = Path.of("docs/agent/CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md");
    private static final Path CLOSEOUT = Path.of("docs/agent/CAMPAIGN_CLOSEOUT_PROTOCOL.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignSystemIndexDocumentationTest.java");

    @Test
    void campaignSystemIndexExistsAndDefinesNavigationRole() throws Exception {
        String normalized = read(INDEX).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "campaign system index",
                "navigation layer",
                "loadbalancerpro multi-pr codex `/goal` campaign system",
                "documentation only",
                "does not add automation",
                "does not replace readme.md",
                "session_manager.md",
                "failure_log.md")) {
            assertTrue(normalized.contains(expected), "index should define " + expected);
        }
    }

    @Test
    void campaignSystemIndexNamesAllControlDocuments() throws Exception {
        String content = read(INDEX);

        for (String expected : List.of(
                "README.md",
                "AGENTS.md",
                "BUILD_CONTRACT.md",
                "AGENT_WORKFLOW_QUICKSTART.md",
                "GOAL_MODE_LONG_RUN_PROTOCOL.md",
                "VERIFICATION_PROTOCOL.md",
                "SESSION_MANAGER.md",
                "FAILURE_LOG.md",
                "CAMPAIGN_SYSTEM_ARCHITECTURE.md",
                "CAMPAIGN_CHECKPOINT_LEDGER.md",
                "CAMPAIGN_PR_READINESS_CHECKLIST.md",
                "CAMPAIGN_SCOPE_AUDIT_CHECKLIST.md",
                "CAMPAIGN_REMOTE_CHECK_AUDIT.md",
                "CAMPAIGN_MERGE_GATE.md",
                "CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md",
                "CAMPAIGN_HANDOFF_REPORT_TEMPLATE.md",
                "CAMPAIGN_CLOSEOUT_PROTOCOL.md")) {
            assertTrue(content.contains(expected), "index should name " + expected);
        }
    }

    @Test
    void campaignSystemIndexPreservesVerificationAndCountingRules() throws Exception {
        String normalized = read(INDEX).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "run one scoped pr at a time",
                "start from clean main",
                "focused checks while editing",
                "full verification before merge",
                "mvn -q test",
                "mvn -q \"-dskiptests\" package",
                "mvn -b package",
                "git diff --check",
                "enterprise lab package smoke",
                "current-head remote pr checks",
                "merge-commit main ci/codeql",
                "ten successful merged prs",
                "does not count",
                "do not claim green main while remote checks are pending",
                "failed, cancelled, stale, pending, skipped-only, or duplicate-only")) {
            assertTrue(normalized.contains(expected), "index should preserve " + expected);
        }
    }

    @Test
    void campaignSystemIndexPreservesScopeAndNotProvenBoundaries() throws Exception {
        String normalized = read(INDEX).toLowerCase(Locale.ROOT);

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
                "no scripts, secrets, external/cloud/tenant targets, or automation",
                "pause instead of improvising",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "index should preserve " + expected);
        }
    }

    @Test
    void publicAndCampaignDocsCrossLinkCampaignSystemIndex() throws Exception {
        for (Path path : List.of(
                README,
                AGENTS,
                BUILD_CONTRACT,
                QUICKSTART,
                GOAL_PROTOCOL,
                SESSION_MANAGER,
                ARCHITECTURE,
                CHECKPOINT_LEDGER,
                PR_READINESS,
                SCOPE_AUDIT,
                REMOTE_AUDIT,
                MERGE_GATE,
                FAILURE_PLAYBOOK,
                HANDOFF,
                CLOSEOUT,
                TRUST_MAP)) {
            assertTrue(read(path).contains("CAMPAIGN_SYSTEM_INDEX.md"),
                    path + " should link to the campaign system index");
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
