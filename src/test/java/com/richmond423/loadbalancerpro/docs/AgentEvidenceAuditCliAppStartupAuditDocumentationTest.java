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

class AgentEvidenceAuditCliAppStartupAuditDocumentationTest {
    private static final Path AUDIT = Path.of("docs/agent/EVIDENCE_AUDIT_CLI_APP_STARTUP_AUDIT.md");
    private static final Path API_APP = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/api/LoadBalancerApiApplication.java");
    private static final Path ADAPTIVE_COMMAND = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/cli/AdaptiveRoutingExperimentCommand.java");
    private static final Path ENTERPRISE_COMMAND = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/cli/EnterpriseLabWorkflowCommand.java");
    private static final Path REPLAY_COMMAND = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/cli/LaseReplayCommand.java");
    private static final Path DEMO_COMMAND = Path.of(
            "src/main/java/com/richmond423/loadbalancerpro/cli/LaseDemoCommand.java");
    private static final Path API_APP_TEST = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/api/LoadBalancerApiApplicationTest.java");
    private static final Path ADAPTIVE_COMMAND_TEST = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/cli/AdaptiveRoutingExperimentCommandTest.java");
    private static final Path ENTERPRISE_COMMAND_TEST = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/cli/EnterpriseLabWorkflowCommandTest.java");
    private static final Path REPLAY_COMMAND_TEST = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/cli/LaseReplayCommandTest.java");
    private static final Path DEMO_COMMAND_TEST = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/cli/LaseDemoCommandTest.java");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path EVIDENCE_MAP = Path.of("docs/agent/EVIDENCE_AUDIT_REPOSITORY_EVIDENCE_MAP.md");
    private static final Path BOARD = Path.of("docs/agent/EVIDENCE_AUDIT_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentEvidenceAuditCliAppStartupAuditDocumentationTest.java");

    @Test
    void auditExistsAndNamesSlotElevenScope() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "slot 11",
                "cli mode and app startup audit",
                "documentation/test-only",
                "loadbalancerapiapplication.java",
                "adaptiveroutingexperimentcommand.java",
                "enterpriselabworkflowcommand.java",
                "lasereplaycommand.java",
                "lasedemocommand.java",
                "codex/evidence-audit-cli-app-startup",
                "d4a07057c7e0475e012e610a551733184d26791d",
                "4bad0291be2a36ed7695bb47fa3b9a3e63d4dbb0",
                "does not run cli commands",
                "does not start spring boot",
                "does not change app startup behavior")) {
            assertTrue(audit.contains(expected), "Missing slot 11 audit scope: " + expected);
        }
    }

    @Test
    void auditCoversRequiredCliDispatchModes() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "--version",
                "prints `loadbalancerpro version",
                "shouldstartapi(args)",
                "springapplication.run",
                "--adaptive-routing-experiment",
                "--enterprise-lab-workflow",
                "--lase-replay=<path>",
                "--lase-demo",
                "spring boot startup is the default app path",
                "smoke coverage expectations",
                "started loadbalancerapiapplication",
                "target/enterprise-lab-runs",
                "offline/read-only",
                "synthetic recommendation-only",
                "reviewer questions",
                "remaining limits")) {
            assertTrue(audit.contains(expected), "Missing CLI audit wording: " + expected);
        }
    }

    @Test
    void apiApplicationSourcePreservesDispatchOrderAndStartupBoundary() throws IOException {
        String source = read(API_APP);
        String normalized = source.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "if (isversionrequested(args))",
                "system.out.println(\"loadbalancerpro version \" + version())",
                "if (!shouldstartapi(args))",
                "adaptiveroutingexperimentcommand.runifrequested",
                "enterpriselabworkflowcommand.runifrequested",
                "lasereplaycommand.runifrequested",
                "lasedemocommand.runifrequested",
                "springapplication.run(loadbalancerapiapplication.class, args)",
                "\"--version\".equalsignorecase(arg)",
                "private static final string fallback_version = \"2.5.0\"")) {
            assertTrue(normalized.contains(expected), "Missing API dispatch source boundary: " + expected);
        }

        int versionIndex = normalized.indexOf("if (isversionrequested(args))");
        int springIndex = normalized.indexOf("springapplication.run(loadbalancerapiapplication.class, args)");
        assertTrue(versionIndex >= 0 && springIndex > versionIndex,
                "--version should be checked before SpringApplication.run");
    }

    @Test
    void commandSourcesPreserveLocalAndOfflineSafetyWording() throws IOException {
        String adaptive = read(ADAPTIVE_COMMAND).toLowerCase(Locale.ROOT);
        String enterprise = read(ENTERPRISE_COMMAND).toLowerCase(Locale.ROOT);
        String replay = read(REPLAY_COMMAND).toLowerCase(Locale.ROOT);
        String demo = read(DEMO_COMMAND).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "no api server",
                "no live cloud mutation",
                "no external network",
                "no release action")) {
            assertTrue(adaptive.contains(expected), "Missing adaptive command safety wording: " + expected);
        }

        for (String expected : List.of(
                "target\", \"enterprise-lab-runs",
                "no api server, live cloud, external network, release, tag, asset, container, or registry action")) {
            assertTrue(enterprise.contains(expected), "Missing enterprise command safety wording: " + expected);
        }

        for (String expected : List.of(
                "offline/read-only replay",
                "no api server, network access, cloudmanager calls, or cloud mutation",
                "--lase-replay=<path-to-shadow-events.jsonl>")) {
            assertTrue(replay.contains(expected), "Missing replay command safety wording: " + expected);
        }

        for (String expected : List.of(
                "synthetic demo, recommendation-only evaluation",
                "no live aws resources touched",
                "no real routing mutation",
                "no cloudmanager calls",
                "no aws keys, network access, or api server required")) {
            assertTrue(demo.contains(expected), "Missing LASE demo command safety wording: " + expected);
        }
    }

    @Test
    void existingTestsCoverCliStartupSplit() throws IOException {
        String apiTest = read(API_APP_TEST).toLowerCase(Locale.ROOT);
        String adaptiveTest = read(ADAPTIVE_COMMAND_TEST).toLowerCase(Locale.ROOT);
        String enterpriseTest = read(ENTERPRISE_COMMAND_TEST).toLowerCase(Locale.ROOT);
        String replayTest = read(REPLAY_COMMAND_TEST).toLowerCase(Locale.ROOT);
        String demoTest = read(DEMO_COMMAND_TEST).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "assertfalse(loadbalancerapiapplication.shouldstartapi(new string[]{\"--lase-demo\"}))",
                "assertfalse(loadbalancerapiapplication.shouldstartapi(new string[]{\"--lase-replay=shadow-events.jsonl\"}))",
                "assertfalse(loadbalancerapiapplication.shouldstartapi(new string[]{\"--adaptive-routing-experiment\"}))",
                "assertfalse(loadbalancerapiapplication.shouldstartapi(new string[]{\"--version\"}))",
                "asserttrue(loadbalancerapiapplication.shouldstartapi(new string[]{\"--server.port=18080\"}))",
                "assertequals(\"2.5.0\", loadbalancerapiapplication.version())")) {
            assertTrue(apiTest.contains(expected), "Missing API startup test expectation: " + expected);
        }

        assertTrue(adaptiveTest.contains("started loadbalancerapiapplication"));
        assertTrue(enterpriseTest.contains("--enterprise-lab-workflow"));
        assertTrue(enterpriseTest.contains("started loadbalancerapiapplication"));
        assertTrue(replayTest.contains("started loadbalancerapiapplication"));
        assertTrue(demoTest.contains("--lase-demo"));
    }

    @Test
    void navigationAndCampaignStateReferenceCliStartupAudit() throws IOException {
        String readme = read(README).toLowerCase(Locale.ROOT);
        String trustMap = read(TRUST_MAP).toLowerCase(Locale.ROOT);
        String evidenceMap = read(EVIDENCE_MAP).toLowerCase(Locale.ROOT);
        String board = read(BOARD).toLowerCase(Locale.ROOT);
        String session = read(SESSION).toLowerCase(Locale.ROOT);

        assertTrue(readme.contains("docs/agent/evidence_audit_cli_app_startup_audit.md"),
                "README should link to the CLI app startup audit");
        assertTrue(trustMap.contains("agent/evidence_audit_cli_app_startup_audit.md"),
                "Reviewer Trust Map should link to the CLI app startup audit");
        assertTrue(evidenceMap.contains("evidence_audit_cli_app_startup_audit.md"),
                "repository evidence map should link to the CLI app startup audit");

        for (String expected : List.of(
                "completed campaign prs: 10 / 20",
                "current pr slot: 11",
                "cli mode and app startup audit",
                "codex/evidence-audit-cli-app-startup",
                "slot 10 result",
                "#325",
                "4bad0291be2a36ed7695bb47fa3b9a3e63d4dbb0",
                "d4a07057c7e0475e012e610a551733184d26791d",
                "post-merge main ci and codeql were green")) {
            assertTrue(board.contains(expected) || session.contains(expected),
                    "Missing slot 11 campaign checkpoint: " + expected);
        }
    }

    @Test
    void auditPreservesScopeAndNotProvenBoundaries() throws IOException {
        String audit = read(AUDIT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not prove production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "load/stress/benchmarking",
                "throughput/p95/p99 evidence",
                "replay/evidence/report/storage/export proof",
                "production gateway readiness",
                "registry publication",
                "container signing",
                "production telemetry",
                "production monitoring",
                "broader automation")) {
            assertTrue(audit.contains(expected), "Missing CLI startup audit boundary: " + expected);
        }

        for (String forbidden : List.of(
                "proves production readiness",
                "production certification proof",
                "live-cloud proof",
                "real-tenant proof",
                "runtime enforcement proof",
                "benchmark proof",
                "throughput proof")) {
            assertFalse(audit.contains(forbidden), "CLI startup audit must not overclaim: " + forbidden);
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
