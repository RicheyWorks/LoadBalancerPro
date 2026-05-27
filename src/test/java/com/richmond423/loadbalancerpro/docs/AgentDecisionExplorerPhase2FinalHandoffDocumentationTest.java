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

class AgentDecisionExplorerPhase2FinalHandoffDocumentationTest {
    private static final Path HANDOFF =
            Path.of("docs/agent/DECISION_EXPLORER_PHASE2_FINAL_HANDOFF.md");
    private static final Path BOARD =
            Path.of("docs/agent/DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerPhase2FinalHandoffDocumentationTest.java");

    @Test
    void finalHandoffExistsAndListsPhaseTwoMergeEvidence() throws IOException {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "# Decision Explorer Phase 2 Final Handoff",
                "Status: active / phase2-final-handoff.",
                "Classification: WARN / decision-explorer-phase2-handoff.",
                "Campaign slot: DX-P2-G12.",
                "Candidate closeout status: `WARN / pending DX-P2-G12 merge-health gate`",
                "final operator response records the actual DX-P2-G12 merge SHA",
                "Current main before DX-P2-G12 local edits: `4fc154801b4b81c08bdc0b23ff832f5d0d819be0`")) {
            assertTrue(handoff.contains(expected), "handoff should include " + expected);
        }

        for (String expected : List.of(
                "#369",
                "codex/decision-explorer-phase2-campaign-board",
                "1e75b7326b09cd7c179909aec00f0c42e34da9c1",
                "#370",
                "codex/decision-explorer-phase2-scenario-catalog",
                "1fb16a50d4181d1411abfe6c038815a68f79e7b5",
                "#371",
                "codex/decision-explorer-phase2-scenario-api",
                "186b28db1d261858a42db2ed75531fb3e4930f44",
                "#372",
                "codex/decision-explorer-phase2-factor-drilldown",
                "b2f5017e4c7484e34d0da6a1ffde3954442a9103",
                "#373",
                "codex/decision-explorer-phase2-candidate-comparison",
                "64394f1380708a63d70ad9e5ec1a2ad3589a9780",
                "#374",
                "codex/decision-explorer-phase2-ui-scenarios",
                "e8fcd4f74f3f50c2f973b78d7999c18104aee9bb",
                "#375",
                "codex/decision-explorer-phase2-ui-drilldown-comparison",
                "673af4f8328e9f882cb44ddd1d2b9837dd0fe7e4",
                "#376",
                "codex/decision-explorer-phase2-reviewer-badges",
                "e92bf92f3f60d54bca23b033856af3632a431c87",
                "#377",
                "codex/decision-explorer-phase2-api-hardening",
                "8a0455ee03a80ae2170c6b977a2e761407ad6d90",
                "#378",
                "codex/decision-explorer-phase2-docs-examples",
                "567cf77643a0d56a683cea86104972715b97fa40",
                "#379",
                "codex/decision-explorer-phase2-final-polish",
                "4fc154801b4b81c08bdc0b23ff832f5d0d819be0",
                "DX-P2-G12",
                "codex/decision-explorer-phase2-final-handoff")) {
            assertTrue(handoff.contains(expected), "handoff should list slot evidence " + expected);
        }
    }

    @Test
    void finalHandoffSummarizesImplementedBehaviorAndFilesChangedByArea() throws IOException {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "DecisionExplorerScenarioCatalogV1",
                "DecisionExplorerScenarioV1",
                "DecisionExplorerScenarioCatalogService",
                "DecisionFactorDrilldownV1",
                "DecisionExplorerCandidateComparisonRowV1",
                "DecisionExplorerPayloadV1",
                "DecisionExplorerPayloadService",
                "RoutingController",
                "GET /api/routing/decision-explorer/scenarios",
                "POST /api/routing/decision-explorer",
                "/decision-explorer.html",
                "src/main/resources/static/decision-explorer.html",
                "DecisionExplorerScenarioCatalogServiceTest",
                "DecisionExplorerPayloadServiceTest",
                "DecisionExplorerApiContractHardeningTest",
                "DecisionExplorerStaticPageTest",
                "RoutingOpenApiContractTest",
                "AgentDecisionExplorerPhase2FinalHandoffDocumentationTest")) {
            assertTrue(handoff.contains(expected), "handoff should summarize implemented artifact " + expected);
        }

        for (String expected : List.of(
                "exposes deterministic scenario catalog metadata",
                "keeps scenario catalog entries sorted",
                "adds factor drill-down readouts",
                "adds candidate comparison rows",
                "displays scenario filtering controls",
                "preserves Phase 1 payload compatibility",
                "read-only",
                "same-origin/local-app-only",
                "simulation-only")) {
            assertTrue(handoff.contains(expected), "handoff should summarize behavior " + expected);
        }
    }

    @Test
    void finalHandoffRecordsDeterministicExplanationAndConfidenceBoundary() throws IOException {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "Deterministic Explanation And Confidence Boundary",
                "DecisionReadoutV1.summary",
                "returned strategy id and selected candidate only",
                "DecisionFactorDrilldownV1.explanation",
                "preserves returned factor explanation text",
                "candidate comparison rows derive status, warnings, unknowns, score deltas, and evidence references",
                "static page derives reviewer badges from returned payload fields",
                "does not add a separately versioned confidence-summary DTO",
                "existing payload statuses, warnings, unknowns, partial-evidence states, and reviewer badges",
                "tested `STRONG`, `PARTIAL`, `UNKNOWN`, or `DEGRADED` confidence/status summary")) {
            assertTrue(handoff.contains(expected), "handoff should record explanation boundary " + expected);
        }
    }

    @Test
    void finalHandoffRecordsVerificationAndMergeGate() throws IOException {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "Focused tests for the active slice",
                "Relevant Decision Explorer selector bundle",
                "mvn -q test",
                "mvn -q \"-DskipTests\" package",
                "mvn -B package",
                "git diff --check",
                "git diff --cached --check",
                "git diff --check origin/main...HEAD",
                ".\\scripts\\smoke\\enterprise-lab-workflow.ps1 -Package",
                "Build/Test/Package/Smoke",
                "Analyze Java",
                "CodeQL",
                "Dependency Review",
                "post-merge main CI and CodeQL",
                "https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26544582671",
                "https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26544582636",
                "https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26544831070",
                "https://github.com/RicheyWorks/LoadBalancerPro/actions/runs/26544831012")) {
            assertTrue(handoff.contains(expected), "handoff should preserve verification item " + expected);
        }
    }

    @Test
    void finalHandoffIsLinkedFromReviewerSurfacesAndCampaignState() throws IOException {
        String readme = read(README);
        String trustMap = read(TRUST_MAP);
        String board = read(BOARD);
        String session = read(SESSION);

        for (String expected : List.of(
                "DECISION_EXPLORER_PHASE2_FINAL_HANDOFF.md",
                "Decision Explorer Phase 2 final handoff")) {
            assertTrue(readme.contains(expected), "README should link final handoff item " + expected);
            assertTrue(trustMap.contains(expected), "Reviewer Trust Map should link final handoff item " + expected);
        }

        for (String expected : List.of(
                "Status: active / phase2-final-handoff.",
                "Current PR slot: DX-P2-G12.",
                "Completed Phase 2 PRs: 11 / 12 planned.",
                "merged-main-green / PR #379",
                "4fc154801b4b81c08bdc0b23ff832f5d0d819be0",
                "codex/decision-explorer-phase2-final-handoff",
                "active-branch / handoff in progress")) {
            assertTrue(board.contains(expected), "board should track final handoff item " + expected);
        }

        for (String expected : List.of(
                "Current PR slot: DX-P2-G12",
                "DX-P2-G11 merged as",
                "DX-P2-G12 branch `codex/decision-explorer-phase2-final-handoff`",
                "DECISION_EXPLORER_PHASE2_FINAL_HANDOFF.md",
                "AgentDecisionExplorerPhase2FinalHandoffDocumentationTest")) {
            assertTrue(session.contains(expected), "session should track final handoff item " + expected);
        }
    }

    @Test
    void finalHandoffRecommendsPhaseThreeMeatModeWithoutUnsafeClaims() throws IOException {
        String normalized = read(HANDOFF).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        for (String expected : List.of(
                "decision explorer implementation phase 3 - deeper lase and routing-intelligence behavior",
                "deterministic confidence/status summary",
                "deeper lase evidence interpretation",
                "routing-intelligence service logic",
                "reads existing evidence rather than mutating production routing")) {
            assertTrue(normalized.contains(expected), "handoff should recommend next meat-mode item " + expected);
        }
    }

    @Test
    void finalHandoffPreservesSafetyAndNotProvenBoundaries() throws IOException {
        String combined = (read(HANDOFF) + "\n" + read(README) + "\n" + read(TRUST_MAP))
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        for (String expected : List.of(
                "additive",
                "read-only",
                "same-origin",
                "simulation-only",
                "does not shift traffic",
                "mutate routing",
                "call cloud or tenant systems",
                "persist storage",
                "execute replay",
                "export files",
                "generate evidence packets",
                "no production readiness",
                "live-cloud validation",
                "real-tenant validation",
                "benchmark/load/stress evidence",
                "throughput/p95/p99 evidence",
                "replay/export/storage proof",
                "evidence-packet generation",
                "autonomous production action",
                "broader automation",
                "no hidden side effects")) {
            assertTrue(combined.contains(expected), "handoff should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "production readiness is proven",
                "certified production",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "autonomous production action is enabled",
                "traffic shifting enabled",
                "replay export is implemented",
                "evidence packet generated")) {
            assertFalse(combined.contains(forbidden), "handoff must not overclaim " + forbidden);
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
