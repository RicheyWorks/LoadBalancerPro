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

class AgentDecisionExplorerPhase1ReviewerExamplesDocumentationTest {
    private static final Path EXAMPLES =
            Path.of("docs/agent/DECISION_EXPLORER_PHASE1_REVIEWER_EXAMPLES.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path API_CONTRACTS = Path.of("docs/API_CONTRACTS.md");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_PHASE1_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path DECISION_EXPLORER_PAGE =
            Path.of("src/main/resources/static/decision-explorer.html");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerPhase1ReviewerExamplesDocumentationTest.java");

    @Test
    void reviewerExamplesExistAndStateDocsOnlyScope() throws IOException {
        String examples = read(EXAMPLES);

        for (String expected : List.of(
                "# Decision Explorer Phase 1 Reviewer Examples",
                "Status: active / phase1-docs-examples.",
                "Classification: WARN / decision-explorer-phase1-examples.",
                "Campaign slot: DX-P1-G07.",
                "DECISION_EXPLORER_PHASE1_CAMPAIGN_BOARD.md",
                "DECISION_EXPLORER_PHASE1_ARCHITECTURE_SCOPE.md",
                "../API_CONTRACTS.md",
                "../REVIEWER_TRUST_MAP.md",
                "/decision-explorer.html",
                "documentation and guard-test only",
                "does not change Java production behavior",
                "read-only",
                "simulation-only")) {
            assertTrue(examples.contains(expected), "reviewer examples should contain " + expected);
        }
    }

    @Test
    void reviewerExamplesGroundCurrentApiPageAndPayloadVocabulary() throws IOException {
        String examples = read(EXAMPLES);
        String page = read(DECISION_EXPLORER_PAGE);

        for (String expected : List.of(
                "POST /api/routing/decision-explorer",
                "RoutingComparisonRequest",
                "DecisionExplorerPayloadV1",
                "AgentStructuredOutputV1",
                "decision summary",
                "selected candidate",
                "candidate set",
                "factor contributions",
                "policy gates",
                "decision diffs",
                "evidence packet readouts",
                "warnings",
                "unknowns",
                "not-proven boundaries",
                "raw JSON payload",
                "green",
                "blue",
                "TAIL_LATENCY_POWER_OF_TWO",
                "averageLatencyMillis",
                "p95LatencyMillis",
                "queueDepth")) {
            assertTrue(examples.contains(expected), "reviewer examples should ground " + expected);
        }

        for (String expected : List.of(
                "Run Decision Explorer",
                "DecisionExplorerPayloadV1",
                "Factor Contributions",
                "Policy Gates",
                "Not-Proven Boundaries")) {
            assertTrue(page.contains(expected), "current page should still expose " + expected);
        }
    }

    @Test
    void reviewerExamplesDefineHumanAndAgentQuestionPaths() throws IOException {
        String examples = read(EXAMPLES);

        for (String expected : List.of(
                "Example Reviewer Path",
                "Example Request Fragment",
                "Example Response Fragment",
                "Example Human Review Questions",
                "Example AI-Agent Questions",
                "What These Examples Prove",
                "What These Examples Do Not Prove",
                "Maintenance Expectations",
                "Which candidate was selected",
                "Which `candidateSet[]` item is selected?",
                "Which `warnings[]`, `unknowns[]`, and `notProvenBoundaries[]` should be repeated in a summary?")) {
            assertTrue(examples.contains(expected), "reviewer examples should define " + expected);
        }
    }

    @Test
    void reviewerExamplesAreLinkedFromReviewerSurfaces() throws IOException {
        String readme = read(README);
        String trustMap = read(TRUST_MAP);
        String apiContracts = read(API_CONTRACTS);
        String board = read(BOARD);
        String session = read(SESSION);

        for (String expected : List.of(
                "DECISION_EXPLORER_PHASE1_REVIEWER_EXAMPLES.md",
                "Decision Explorer Phase 1 reviewer examples")) {
            assertTrue(readme.contains(expected), "README should link examples item " + expected);
            assertTrue(trustMap.contains(expected), "Reviewer Trust Map should link examples item " + expected);
            assertTrue(apiContracts.contains(expected), "API contracts should link examples item " + expected);
        }

        for (String expected : List.of(
                "DX-P1-G07",
                "codex/decision-explorer-phase1-docs-examples",
                "AgentDecisionExplorerPhase1ReviewerExamplesDocumentationTest",
                "phase1-docs-examples",
                "PR #365",
                "66242b7911c123b1f20f2820249b7173a3ef575a")) {
            assertTrue(board.contains(expected), "campaign board should track examples item " + expected);
            assertTrue(session.contains(expected), "session manager should track examples item " + expected);
        }
    }

    @Test
    void reviewerExamplesPreserveSafetyAndNotProvenBoundaries() throws IOException {
        String normalized = read(EXAMPLES).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        for (String expected : List.of(
                "does not shift traffic",
                "mutate routing",
                "call cloud or tenant systems",
                "persist storage",
                "execute replay",
                "export files",
                "generate evidence packets",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "benchmark/load/stress evidence",
                "throughput/p95/p99 evidence",
                "replay/export behavior",
                "storage behavior",
                "evidence-packet generation",
                "autonomous production action",
                "broader automation",
                "hidden network calls",
                "hidden writes",
                "hidden approvals")) {
            assertTrue(normalized.contains(expected), "reviewer examples should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "production readiness is proven",
                "production certification is proven",
                "certified production",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "autonomous production action is enabled",
                "traffic shifting enabled",
                "evidence packet generated")) {
            assertFalse(normalized.contains(forbidden), "reviewer examples must not overclaim " + forbidden);
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
