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

class AgentDecisionExplorerPhase2ArchitectureScopeDocumentationTest {
    private static final Path SCOPE =
            Path.of("docs/agent/DECISION_EXPLORER_PHASE2_ARCHITECTURE_SCOPE.md");
    private static final Path BOARD =
            Path.of("docs/agent/DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md");
    private static final Path PHASE1_HANDOFF =
            Path.of("docs/agent/DECISION_EXPLORER_PHASE1_FINAL_HANDOFF.md");
    private static final Path PHASE1_SCOPE =
            Path.of("docs/agent/DECISION_EXPLORER_PHASE1_ARCHITECTURE_SCOPE.md");
    private static final Path API_CONTRACTS = Path.of("docs/API_CONTRACTS.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerPhase2ArchitectureScopeDocumentationTest.java");

    @Test
    void phase2ArchitectureScopeExistsAndStatesCurrentSliceBoundary() throws IOException {
        String scope = read(SCOPE);

        for (String expected : List.of(
                "# Decision Explorer Phase 2 Architecture And Scope",
                "Status: active / phase2-scope.",
                "Classification: WARN / decision-explorer-phase2-scope.",
                "Campaign slot: DX-P2-G01.",
                "DECISION_EXPLORER_PHASE2_CAMPAIGN_BOARD.md",
                "DECISION_EXPLORER_PHASE1_FINAL_HANDOFF.md",
                "DX-P2-G01 is documentation and guard-test only",
                "28c8bc10e1aa553a3c53aac70883c04431d55cc2")) {
            assertTrue(scope.contains(expected), "phase 2 scope should state " + expected);
        }
    }

    @Test
    void phase2ScopePreservesPhase1CompatibilityContract() throws IOException {
        String scope = read(SCOPE);

        for (String expected : List.of(
                "Phase 1 is treated as a compatibility contract",
                "DecisionExplorerPayloadV1",
                "DecisionReadoutV1",
                "CandidateReadoutV1",
                "FactorContributionV1",
                "PolicyGateReadoutV1",
                "DecisionDiffReadoutV1",
                "EvidencePacketReadoutV1",
                "DecisionExplorerPayloadService",
                "POST /api/routing/decision-explorer",
                "/decision-explorer.html",
                "warnings",
                "unknowns",
                "notProvenBoundaries",
                "boundaryNote",
                "Phase 1 response compatibility")) {
            assertTrue(scope.contains(expected), "phase 2 scope should preserve compatibility item " + expected);
        }
    }

    @Test
    void phase2ScopeDefinesScenarioDrilldownComparisonUiAndApiBoundaries() throws IOException {
        String scope = read(SCOPE);

        for (String expected : List.of(
                "deterministic scenario catalog views",
                "healthy baseline",
                "partial evidence",
                "unknown or no-healthy-server fixture",
                "factor-level drill-down summaries",
                "observed value or status",
                "influence category",
                "source evidence reference",
                "candidate comparison rows",
                "selected-first ordering",
                "scenario selector",
                "scenario category or evidence-status filtering",
                "selected route badge",
                "warning badge",
                "unknown badge",
                "partial evidence badge",
                "deterministic evidence badge",
                "API Boundary",
                "UI Boundary",
                "Verification Expectations")) {
            assertTrue(scope.contains(expected), "phase 2 scope should define boundary item " + expected);
        }
    }

    @Test
    void phase2CampaignBoardTracksSlotsAndCountingRules() throws IOException {
        String board = read(BOARD);

        for (String expected : List.of(
                "# Decision Explorer Phase 2 Campaign Board",
                "Status: active / phase2-api-hardening.",
                "Current PR slot: DX-P2-G09.",
                "Completed Phase 2 PRs: 8 / 12 planned.",
                "DX-P2-G01",
                "codex/decision-explorer-phase2-campaign-board",
                "merged-main-green",
                "https://github.com/RicheyWorks/LoadBalancerPro/pull/369",
                "1e75b7326b09cd7c179909aec00f0c42e34da9c1",
                "DX-P2-G02",
                "codex/decision-explorer-phase2-scenario-catalog",
                "merged-main-green",
                "https://github.com/RicheyWorks/LoadBalancerPro/pull/370",
                "1fb16a50d4181d1411abfe6c038815a68f79e7b5",
                "DecisionExplorerScenarioCatalogV1",
                "DecisionExplorerScenarioV1",
                "DX-P2-G03",
                "codex/decision-explorer-phase2-scenario-api",
                "merged-main-green",
                "PR #371",
                "186b28db1d261858a42db2ed75531fb3e4930f44",
                "focused verification passed",
                "full local verification passed",
                "DecisionExplorerScenarioCatalogService",
                "GET /api/routing/decision-explorer/scenarios",
                "https://github.com/RicheyWorks/LoadBalancerPro/pull/371",
                "DX-P2-G04",
                "codex/decision-explorer-phase2-factor-drilldown",
                "merged-main-green",
                "PR #372",
                "b2f5017e4c7484e34d0da6a1ffde3954442a9103",
                "DecisionFactorDrilldownV1",
                "ScoreFactorContributionResponse",
                "https://github.com/RicheyWorks/LoadBalancerPro/pull/372",
                "DX-P2-G05",
                "codex/decision-explorer-phase2-candidate-comparison",
                "merged-main-green",
                "PR #373",
                "64394f1380708a63d70ad9e5ec1a2ad3589a9780",
                "DecisionExplorerCandidateComparisonRowV1",
                "CandidateReadoutV1",
                "https://github.com/RicheyWorks/LoadBalancerPro/pull/373",
                "dfa9baa73695cfc7ce4a2264617ce193077bc482",
                "DX-P2-G06",
                "codex/decision-explorer-phase2-ui-scenarios",
                "merged-main-green",
                "PR #374",
                "e8fcd4f74f3f50c2f973b78d7999c18104aee9bb",
                "DecisionExplorerScenarioCatalogV1",
                "scenario category and evidence status",
                "https://github.com/RicheyWorks/LoadBalancerPro/pull/374",
                "DX-P2-G07",
                "codex/decision-explorer-phase2-ui-drilldown-comparison",
                "merged-main-green",
                "PR #375",
                "673af4f8328e9f882cb44ddd1d2b9837dd0fe7e4",
                "https://github.com/RicheyWorks/LoadBalancerPro/pull/375",
                "factor drill-down",
                "candidate comparison rows",
                "DX-P2-G08",
                "codex/decision-explorer-phase2-reviewer-badges",
                "merged-main-green",
                "PR #376",
                "e92bf92f3f60d54bca23b033856af3632a431c87",
                "https://github.com/RicheyWorks/LoadBalancerPro/pull/376",
                "PR creation commit",
                "1091470d88da5196e3e5ef27f763f4cbed34803f",
                "must re-read the latest PR head from GitHub before merge",
                "selected route",
                "warning",
                "unknown",
                "partial evidence",
                "deterministic evidence",
                "not-proven boundary",
                "DX-P2-G09",
                "codex/decision-explorer-phase2-api-hardening",
                "active-pr",
                "PR #377",
                "https://github.com/RicheyWorks/LoadBalancerPro/pull/377",
                "PR creation commit",
                "a7b790636bbd8042bc06c48db5fc6390c334215e",
                "must re-read the latest PR head from GitHub before merge",
                "DecisionExplorerPayloadV1 field presence",
                "legacy constructor compatibility",
                "null/unknown evidence array presence",
                "deterministic selected-first comparison ordering",
                "DX-P2-G10",
                "codex/decision-explorer-phase2-docs-examples",
                "DX-P2-G11",
                "codex/decision-explorer-phase2-final-polish",
                "DX-P2-G12",
                "codex/decision-explorer-phase2-final-handoff",
                "Pending, failed, cancelled, stale, skipped-only, duplicate-only, or wrong-head checks do not count",
                "Decision: continue")) {
            assertTrue(board.contains(expected), "phase 2 board should track " + expected);
        }
    }

    @Test
    void phase2ScopeAlignsWithPhase1HandoffAndApiSources() throws IOException {
        String phase1Handoff = read(PHASE1_HANDOFF);
        String phase1Scope = read(PHASE1_SCOPE);
        String apiContracts = read(API_CONTRACTS);

        for (String expected : List.of(
                "Decision Explorer Implementation Phase 2",
                "recommended next campaign",
                "read-only",
                "simulation-only")) {
            assertTrue(phase1Handoff.contains(expected), "phase 1 handoff should preserve " + expected);
        }

        for (String expected : List.of(
                "DecisionExplorerPayloadV1",
                "POST /api/routing/decision-explorer",
                "GET /api/routing/decision-explorer/scenarios",
                "DecisionExplorerScenarioCatalogV1",
                "DecisionExplorerPayloadV1.factorDrilldowns",
                "DecisionFactorDrilldownV1",
                "DecisionExplorerPayloadV1.candidateComparisons",
                "DecisionExplorerCandidateComparisonRowV1",
                "reviewer explanation badges",
                "Decision Explorer Phase 2 API contract hardening",
                "legacy constructor paths",
                "present empty arrays",
                "read-only",
                "simulation-only")) {
            if (expected.startsWith("GET ")
                    || expected.equals("DecisionExplorerScenarioCatalogV1")
                    || expected.startsWith("DecisionExplorerPayloadV1.")
                    || expected.equals("DecisionFactorDrilldownV1")
                    || expected.equals("DecisionExplorerCandidateComparisonRowV1")
                    || expected.equals("reviewer explanation badges")
                    || expected.equals("Decision Explorer Phase 2 API contract hardening")
                    || expected.equals("legacy constructor paths")
                    || expected.equals("present empty arrays")) {
                assertTrue(apiContracts.contains(expected), "API contracts should preserve " + expected);
            } else {
                assertTrue(phase1Scope.contains(expected), "phase 1 scope should preserve " + expected);
                assertTrue(apiContracts.contains(expected), "API contracts should preserve " + expected);
            }
        }
    }

    @Test
    void sessionManagerRecordsPhase2BranchCreationCheckpoint() throws IOException {
        String session = read(SESSION).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "decision explorer implementation phase 2",
                "current pr slot: dx-p2-g09",
                "codex/decision-explorer-phase2-api-hardening",
                "decision_explorer_phase2_campaign_board.md",
                "api_contracts.md",
                "decisionexplorerapicontracthardeningtest.java",
                "agentdecisionexplorerphase2architecturescopedocumentationtest.java",
                "64394f1380708a63d70ad9e5ec1a2ad3589a9780",
                "https://github.com/richeyworks/loadbalancerpro/pull/369",
                "https://github.com/richeyworks/loadbalancerpro/pull/370",
                "https://github.com/richeyworks/loadbalancerpro/pull/371",
                "https://github.com/richeyworks/loadbalancerpro/pull/372",
                "https://github.com/richeyworks/loadbalancerpro/pull/373",
                "https://github.com/richeyworks/loadbalancerpro/pull/374",
                "https://github.com/richeyworks/loadbalancerpro/pull/375",
                "https://github.com/richeyworks/loadbalancerpro/pull/376",
                "https://github.com/richeyworks/loadbalancerpro/pull/377",
                "merged as `1fb16a50d4181d1411abfe6c038815a68f79e7b5`",
                "merged as `186b28db1d261858a42db2ed75531fb3e4930f44`",
                "merged as `b2f5017e4c7484e34d0da6a1ffde3954442a9103`",
                "merged as `64394f1380708a63d70ad9e5ec1a2ad3589a9780`",
                "merged as `e8fcd4f74f3f50c2f973b78d7999c18104aee9bb`",
                "merged as `673af4f8328e9f882cb44ddd1d2b9837dd0fe7e4`",
                "merged as `e92bf92f3f60d54bca23b033856af3632a431c87`",
                "dfa9baa73695cfc7ce4a2264617ce193077bc482",
                "c13b56cb38518160cfc1a754a50e9c0eeeefea28",
                "fb7e4f87b93645228a57d9bbf69ad51a5833531f",
                "0efd6df064f5c8bad05270044c2a28b6a7333d9a",
                "673af4f8328e9f882cb44ddd1d2b9837dd0fe7e4",
                "e92bf92f3f60d54bca23b033856af3632a431c87",
                "1091470d88da5196e3e5ef27f763f4cbed34803f",
                "37e219d9a616eee28b49cdc87b4a36c2ce3a0921",
                "a7b790636bbd8042bc06c48db5fc6390c334215e",
                "e8fcd4f74f3f50c2f973b78d7999c18104aee9bb",
                "get /api/routing/decision-explorer/scenarios",
                "2,682 tests",
                "2,689 tests",
                "2,695 tests",
                "2,696 tests",
                "2,697 tests",
                "2,698 tests",
                "2,701 tests",
                "decisionexplorerapicontracthardeningtest",
                "focused selector passed",
                "full local verification passed",
                "remote status: pr #377",
                "decision: continue")) {
            assertTrue(session.contains(expected), "session manager should record " + expected);
        }
    }

    @Test
    void phase2ScopeAndBoardPreserveSafetyAndNotProvenBoundaries() throws IOException {
        String combined = (read(SCOPE) + "\n" + read(BOARD)).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "additive",
                "read-only",
                "same-origin",
                "local-app-only",
                "simulation-only",
                "already computed routing comparison evidence",
                "no hidden side effects",
                "no live mutation",
                "no autonomous production action",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "benchmark/load/stress evidence",
                "throughput/p95/p99 evidence",
                "replay execution",
                "export behavior",
                "storage behavior",
                "evidence packet generation",
                "traffic shifting",
                "carbon-aware routing",
                "gpu orchestration",
                "power/grid control",
                "facility automation",
                "broader automation",
                "secrets",
                "external targets",
                "cloud targets",
                "tenant targets",
                "rulesets")) {
            assertTrue(combined.contains(expected), "phase 2 scope should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "phase 2 is implemented",
                "scenario catalog is implemented",
                "factor drill-down is implemented",
                "candidate comparison is implemented",
                "decision explorer storage is implemented",
                "evidence packet is generated",
                "replay export is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "traffic shifting enabled",
                "autonomous production action is enabled")) {
            assertFalse(combined.contains(forbidden), "phase 2 scope must not overclaim " + forbidden);
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
