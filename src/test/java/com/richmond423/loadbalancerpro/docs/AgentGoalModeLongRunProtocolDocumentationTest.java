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

class AgentGoalModeLongRunProtocolDocumentationTest {
    private static final Path GOAL_PROTOCOL = Path.of("docs/agent/GOAL_MODE_LONG_RUN_PROTOCOL.md");
    private static final Path README = Path.of("README.md");
    private static final Path AGENTS = Path.of("AGENTS.md");
    private static final Path BUILD_CONTRACT = Path.of("BUILD_CONTRACT.md");
    private static final Path QUICKSTART = Path.of("docs/agent/AGENT_WORKFLOW_QUICKSTART.md");
    private static final Path VERIFICATION_PROTOCOL = Path.of("docs/agent/VERIFICATION_PROTOCOL.md");
    private static final Path SESSION_MANAGER = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path FAILURE_LOG = Path.of("docs/agent/FAILURE_LOG.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/AgentGoalModeLongRunProtocolDocumentationTest.java");

    @Test
    void goalModeProtocolExistsAndNamesCommandLifecycle() throws Exception {
        String protocol = read(GOAL_PROTOCOL);

        for (String expected : List.of(
                "/goal",
                "/plan",
                "/goal pause",
                "/goal resume",
                "/goal clear",
                "Goal-mode starter prompt",
                "Goal-mode status prompt",
                "Pause Prompt",
                "Resume Prompt",
                "Clear Prompt",
                "How Long Can This Run?")) {
            assertTrue(protocol.contains(expected), "goal-mode protocol should contain " + expected);
        }
    }

    @Test
    void goalModeProtocolExplainsFileRolesAndRuntimeContext() throws Exception {
        String protocol = read(GOAL_PROTOCOL);
        String normalized = protocol.toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "README.md",
                "AGENTS.md",
                "BUILD_CONTRACT.md",
                "VERIFICATION_PROTOCOL.md",
                "SESSION_MANAGER.md",
                "FAILURE_LOG.md",
                "README.md remains the constitutional layer",
                "public trust surface",
                "always-read operating rules file",
                "BUILD_CONTRACT.md is the focused execution contract",
                "focused execution contract",
                "one durable objective",
                "short and durable")) {
            assertTrue(protocol.contains(expected), "goal-mode protocol should explain " + expected);
        }

        assertTrue(normalized.contains("not the only runtime context"),
                "README should not be treated as the only runtime context");
    }

    @Test
    void goalModeProtocolPreservesVerificationRules() throws Exception {
        String normalized = read(GOAL_PROTOCOL).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "focused checks while editing",
                "full verification before merge",
                "remote checks must be green before claiming green main",
                "do not claim green main while remote checks are pending",
                "failed, cancelled, stale, or pending required checks",
                "update session_manager.md at checkpoints",
                "log failures in docs/agent/failure_log.md",
                "human review is still required before merge")) {
            assertTrue(normalized.contains(expected), "goal-mode protocol should preserve " + expected);
        }
    }

    @Test
    void goalModeProtocolPreservesBoundariesAndStopConditions() throws Exception {
        String normalized = read(GOAL_PROTOCOL).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "preserve not-proven boundaries",
                "no production readiness/certification",
                "no live-cloud or real-tenant validation",
                "no runtime enforcement",
                "no load/stress/benchmarking or throughput/p95/p99 evidence",
                "no replay/evidence/report/storage/export proof",
                "do not use `/goal` as permission to ignore scope",
                "do not use `/goal` as permission to skip verification",
                "do not use `/goal` as permission to keep going through unsafe changes",
                "pause rather than improvise when scope, safety, or verification is unclear")) {
            assertTrue(normalized.contains(expected), "goal-mode protocol should preserve boundary " + expected);
        }
    }

    @Test
    void requestedDocsCrossLinkGoalModeProtocol() throws Exception {
        for (Path path : List.of(
                README,
                AGENTS,
                BUILD_CONTRACT,
                QUICKSTART,
                VERIFICATION_PROTOCOL,
                SESSION_MANAGER,
                FAILURE_LOG,
                REVIEWER_TRUST_MAP)) {
            assertTrue(read(path).contains("GOAL_MODE_LONG_RUN_PROTOCOL.md"),
                    path + " should link to the goal-mode long-run protocol");
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
