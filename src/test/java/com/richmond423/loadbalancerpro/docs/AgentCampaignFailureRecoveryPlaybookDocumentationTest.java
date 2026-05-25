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

class AgentCampaignFailureRecoveryPlaybookDocumentationTest {
    private static final Path PLAYBOOK = Path.of("docs/agent/CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md");
    private static final Path CAMPAIGN_ARCHITECTURE = Path.of("docs/agent/CAMPAIGN_SYSTEM_ARCHITECTURE.md");
    private static final Path CHECKPOINT_LEDGER = Path.of("docs/agent/CAMPAIGN_CHECKPOINT_LEDGER.md");
    private static final Path PR_READINESS = Path.of("docs/agent/CAMPAIGN_PR_READINESS_CHECKLIST.md");
    private static final Path REMOTE_AUDIT = Path.of("docs/agent/CAMPAIGN_REMOTE_CHECK_AUDIT.md");
    private static final Path MERGE_GATE = Path.of("docs/agent/CAMPAIGN_MERGE_GATE.md");
    private static final Path QUICKSTART = Path.of("docs/agent/AGENT_WORKFLOW_QUICKSTART.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path FAILURE_LOG = Path.of("docs/agent/FAILURE_LOG.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentCampaignFailureRecoveryPlaybookDocumentationTest.java");

    @Test
    void failureRecoveryPlaybookExistsAndDefinesFailureTypes() throws Exception {
        String normalized = read(PLAYBOOK).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "campaign failure recovery playbook",
                "focused documentation guard failure",
                "relevant focused selector bundle failure",
                "mvn -q test",
                "package check failure",
                "diff or whitespace check failure",
                "scope audit failure",
                "enterprise lab package smoke failure",
                "remote pr check failure",
                "post-merge main ci/codeql failure",
                "tooling command failure",
                "human decision needed")) {
            assertTrue(normalized.contains(expected), "playbook should define failure type " + expected);
        }
    }

    @Test
    void failureRecoveryPlaybookRequiresFailureLogAndSessionManagerUpdates() throws Exception {
        String normalized = read(PLAYBOOK).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "log the failure in failure_log.md",
                "update session_manager.md",
                "branch and head sha",
                "last known good state",
                "checks already run",
                "next safe action",
                "do not merge while the failure is unresolved",
                "do not continue to the next campaign pr")) {
            assertTrue(normalized.contains(expected), "playbook should require recovery record " + expected);
        }
    }

    @Test
    void failureRecoveryPlaybookDefinesSafeRecoveryAndResumeCriteria() throws Exception {
        String normalized = read(PLAYBOOK).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "fix is obvious, minimal, and inside the current pr contract",
                "rerun the failing focused check first",
                "rerun the relevant focused selector bundle",
                "continue to full local verification only after focused recovery passes",
                "resume a paused campaign only when",
                "blocker is resolved",
                "scope still matches build_contract.md",
                "/goal resume")) {
            assertTrue(normalized.contains(expected), "playbook should define safe recovery or resume rule " + expected);
        }
    }

    @Test
    void failureRecoveryPlaybookPreservesScopeStopConditionsAndNotProvenBoundaries() throws Exception {
        String normalized = read(PLAYBOOK).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "production code",
                "maven config",
                "ci/workflow",
                "dockerfile",
                "compose behavior",
                "runtime behavior",
                "endpoint behavior",
                "secrets",
                "external/cloud/tenant targets",
                "automation",
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
            assertTrue(normalized.contains(expected), "playbook should preserve " + expected);
        }
    }

    @Test
    void relatedAgentDocsCrossLinkFailureRecoveryPlaybook() throws Exception {
        for (Path path : List.of(
                CAMPAIGN_ARCHITECTURE,
                CHECKPOINT_LEDGER,
                PR_READINESS,
                REMOTE_AUDIT,
                MERGE_GATE,
                QUICKSTART,
                SESSION_MANAGER,
                FAILURE_LOG)) {
            assertTrue(read(path).contains("CAMPAIGN_FAILURE_RECOVERY_PLAYBOOK.md"),
                    path + " should link to the campaign failure recovery playbook");
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
