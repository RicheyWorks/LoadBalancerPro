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

class AgentDecisionExplorerPhase2ReviewerExamplesDocumentationTest {
    private static final Path EXAMPLES =
            Path.of("docs/agent/DECISION_EXPLORER_PHASE2_REVIEWER_EXAMPLES.md");
    private static final Path API_CONTRACTS = Path.of("docs/API_CONTRACTS.md");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path DECISION_EXPLORER_PAGE =
            Path.of("src/main/resources/static/decision-explorer.html");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerPhase2ReviewerExamplesDocumentationTest.java");

    @Test
    void phaseTwoExamplesExistAndStateDocsOnlyScope() throws IOException {
        String examples = read(EXAMPLES);

        for (String expected : List.of(
                "# Decision Explorer Phase 2 Reviewer Examples",
                "Status: active / phase2-docs-examples.",
                "Classification: WARN / decision-explorer-phase2-examples.",
                "Campaign slot: DX-P2-G10.",
                "DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md",
                "DECISION_EXPLORER_PHASE2_ARCHITECTURE_SCOPE.md",
                "../API_CONTRACTS.md",
                "/decision-explorer.html",
                "documentation and guard-test only",
                "does not change Java production behavior",
                "same-origin",
                "read-only",
                "simulation-only")) {
            assertTrue(examples.contains(expected), "Phase 2 examples should contain " + expected);
        }
    }

    @Test
    void phaseTwoExamplesGroundImplementedSurfacesAndCurrentTests() throws IOException {
        String examples = read(EXAMPLES);
        String page = read(DECISION_EXPLORER_PAGE);

        for (String expected : List.of(
                "DecisionExplorerScenarioCatalogServiceTest",
                "DecisionExplorerScenarioCatalogV1Test",
                "RoutingControllerTest",
                "RoutingOpenApiContractTest",
                "DecisionExplorerPayloadServiceTest",
                "DecisionExplorerApiContractHardeningTest",
                "DecisionExplorerStaticPageTest",
                "DecisionExplorerReviewerNavigationTest",
                "GET /api/routing/decision-explorer/scenarios",
                "POST /api/routing/decision-explorer",
                "DecisionExplorerScenarioCatalogV1",
                "DecisionExplorerScenarioV1",
                "DecisionExplorerPayloadV1",
                "DecisionExplorerCandidateComparisonRowV1",
                "DecisionFactorDrilldownV1",
                "candidate comparison",
                "factor drill-down",
                "reviewer badges")) {
            assertTrue(examples.contains(expected), "Phase 2 examples should ground " + expected);
        }

        for (String expected : List.of(
                "/api/routing/decision-explorer/scenarios",
                "Scenario Catalog",
                "Candidate Comparison",
                "Factor Drill-Down",
                "Reviewer Explanation Badges",
                "Not-Proven Boundaries")) {
            assertTrue(page.contains(expected), "current page should still expose " + expected);
        }
    }

    @Test
    void phaseTwoExamplesIncludeRepresentativeFragmentsAndWorkflow() throws IOException {
        String examples = read(EXAMPLES);

        for (String expected : List.of(
                "\"payloadObject\": \"DecisionExplorerScenarioCatalogV1\"",
                "\"scenarioObject\": \"DecisionExplorerScenarioV1\"",
                "\"scenarioId\": \"normal-balanced-load\"",
                "\"scenarioCategory\": \"HEALTHY_BASELINE\"",
                "\"evidenceStatus\": \"AVAILABLE\"",
                "\"scenarioId\": \"stale-signal\"",
                "\"scenarioCategory\": \"PARTIAL_EVIDENCE\"",
                "\"scenarioId\": \"all-unhealthy-degradation\"",
                "\"scenarioCategory\": \"NO_HEALTHY_SERVER\"",
                "\"candidateComparisons\"",
                "\"factorDrilldowns\"",
                "\"comparisonStatus\": \"SELECTED\"",
                "\"influenceCategory\": \"SUPPORTS_SELECTION\"",
                "Reviewer UI Workflow Example",
                "Human Reviewer Questions",
                "AI-Agent Structured Questions",
                "What These Examples Do Not Prove")) {
            assertTrue(examples.contains(expected), "Phase 2 examples should include " + expected);
        }
    }

    @Test
    void phaseTwoExamplesAreLinkedFromApiContractsAndTrackedByCampaignSurfaces() throws IOException {
        String apiContracts = read(API_CONTRACTS);
        String board = read(BOARD);
        String session = read(SESSION);

        for (String expected : List.of(
                "DECISION_EXPLORER_PHASE2_REVIEWER_EXAMPLES.md",
                "Decision Explorer Phase 2 reviewer examples",
                "scenario catalog",
                "factor drill-down rows",
                "candidate comparison rows",
                "reviewer badges")) {
            assertTrue(apiContracts.contains(expected), "API contracts should link examples item " + expected);
        }

        for (String expected : List.of(
                "DX-P2-G10",
                "codex/decision-explorer-phase2-docs-examples",
                "DECISION_EXPLORER_PHASE2_REVIEWER_EXAMPLES.md",
                "AgentDecisionExplorerPhase2ReviewerExamplesDocumentationTest",
                "phase2-docs-examples",
                "PR #378",
                "ee5e2c4e8836d33ceead8ccc22371cc2daf77c1b",
                "8a0455ee03a80ae2170c6b977a2e761407ad6d90")) {
            assertTrue(board.contains(expected), "campaign board should track examples item " + expected);
            assertTrue(session.contains(expected), "session manager should track examples item " + expected);
        }
    }

    @Test
    void phaseTwoExamplesPreserveSafetyAndNotProvenBoundaries() throws IOException {
        String normalized = read(EXAMPLES).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        for (String expected : List.of(
                "same-origin",
                "read-only",
                "simulation-only",
                "does not run routing by itself",
                "shift traffic",
                "persist state",
                "execute replay",
                "export evidence",
                "generate evidence packets",
                "call external systems",
                "hidden routing internals",
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
                "traffic shifting",
                "broader automation",
                "hidden network calls",
                "hidden writes",
                "hidden approvals")) {
            assertTrue(normalized.contains(expected), "Phase 2 examples should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "production readiness is proven",
                "production certification is proven",
                "certified production",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "replay export is implemented",
                "evidence packet generated",
                "autonomous production action is enabled",
                "traffic shifting enabled")) {
            assertFalse(normalized.contains(forbidden), "Phase 2 examples must not overclaim " + forbidden);
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
