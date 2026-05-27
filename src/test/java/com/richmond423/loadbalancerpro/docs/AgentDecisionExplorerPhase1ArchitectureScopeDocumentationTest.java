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

class AgentDecisionExplorerPhase1ArchitectureScopeDocumentationTest {
    private static final Path SCOPE =
            Path.of("docs/agent/DECISION_EXPLORER_PHASE1_ARCHITECTURE_SCOPE.md");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_PHASE1_CAMPAIGN_BOARD.md");
    private static final Path BOOTSTRAP_CLOSEOUT =
            Path.of("docs/agent/DECISION_EXPLORER_BOOTSTRAP_CLOSEOUT.md");
    private static final Path DATA_CONTRACT = Path.of("docs/agent/DECISION_EXPLORER_DATA_CONTRACT.md");
    private static final Path AGENT_SCHEMA =
            Path.of("docs/agent/DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md");
    private static final Path EVIDENCE_LANE = Path.of("docs/agent/DECISION_EXPLORER_EVIDENCE_LANE.md");
    private static final Path PHASE0_GATE =
            Path.of("docs/agent/DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md");
    private static final Path SESSION = Path.of("docs/agent/SESSION_MANAGER.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerPhase1ArchitectureScopeDocumentationTest.java");

    @Test
    void phase1ArchitectureScopeExistsAndStatesCurrentSliceBoundary() throws IOException {
        String scope = read(SCOPE);

        for (String expected : List.of(
                "# Decision Explorer Phase 1 Architecture And Scope",
                "Status: active / phase1-scope.",
                "Classification: WARN / decision-explorer-phase1-scope.",
                "Campaign slot: DX-P1-G01.",
                "DECISION_EXPLORER_PHASE1_CAMPAIGN_BOARD.md",
                "DECISION_EXPLORER_BOOTSTRAP_CLOSEOUT.md",
                "ADR-0010-Interactive-Decision-Explorer-Architecture.md",
                "DECISION_EXPLORER_DATA_CONTRACT.md",
                "DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md",
                "DECISION_EXPLORER_EVIDENCE_LANE.md",
                "DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md",
                "DX-P1-G01 is documentation and guard-test only")) {
            assertTrue(scope.contains(expected), "phase 1 scope should state " + expected);
        }
    }

    @Test
    void phase1ScopePreservesBootstrapContractsAndObjectVocabulary() throws IOException {
        String scope = read(SCOPE);

        for (String expected : List.of(
                "755ed394adfa18e462f89312c5289fd3154075f2",
                "DecisionExplorerPayloadV1",
                "DecisionReadoutV1",
                "CandidateReadoutV1",
                "FactorContributionV1",
                "PolicyGateReadoutV1",
                "DecisionDiffReadoutV1",
                "EvidencePacketReadoutV1",
                "AgentStructuredOutputV1",
                "stable identifiers",
                "JSON field naming rules",
                "enum stability",
                "null and unknown handling",
                "parseability",
                "no autonomous production action",
                "source-card templates",
                "no raw research dumps",
                "docs-before-code")) {
            assertTrue(scope.contains(expected), "phase 1 scope should preserve contract item " + expected);
        }
    }

    @Test
    void phase1ScopeDefinesDataApiUiAndVerificationBoundaries() throws IOException {
        String scope = read(SCOPE);

        for (String expected : List.of(
                "Data Source Boundary",
                "RoutingComparisonResponse",
                "RoutingComparisonResultResponse",
                "RoutingDecisionVectorResponse",
                "CandidateDecisionVectorResponse",
                "ScoreFactorContributionResponse",
                "DominantFactorAnalysisResponse",
                "RoutingDecisionDeltaAnalysisResponse",
                "API Boundary",
                "POST /api/routing/decision-explorer",
                "read-only request handling",
                "UI Boundary",
                "decision summary",
                "selected candidate",
                "candidate set",
                "factor contributions",
                "policy gate display-only status",
                "not-proven boundaries",
                "Verification Expectations",
                "Current-head PR CI, CodeQL, and Dependency Review",
                "main CI plus CodeQL")) {
            assertTrue(scope.contains(expected), "phase 1 scope should define boundary item " + expected);
        }
    }

    @Test
    void phase1CampaignBoardTracksSlotsAndCountingRules() throws IOException {
        String board = read(BOARD);

        for (String expected : List.of(
                "# Decision Explorer Phase 1 Campaign Board",
                "Status: active / phase1-hardening.",
                "Current PR slot: DX-P1-G08.",
                "Completed Phase 1 PRs: 7 / 9 planned.",
                "DX-P1-G01",
                "codex/decision-explorer-phase1-architecture",
                "merged-main-green as PR #360",
                "0fe9331a757973d93820bbae46b05ae53f8ba64a",
                "DX-P1-G02",
                "codex/decision-explorer-phase1-dto-skeleton",
                "merged-main-green as PR #361",
                "fca765b897937cd20ee9955bfb7f9ba7a665a9be",
                "DX-P1-G03",
                "codex/decision-explorer-phase1-builder",
                "merged-main-green as PR #362",
                "af351b043fbc3ff0ffff50d9c0f17a667f84b7af",
                "DX-P1-G04",
                "codex/decision-explorer-phase1-api",
                "merged-main-green as PR #363",
                "20b9080d5c24ef3807e15a3ef8367a8ef1ae4915",
                "DX-P1-G05",
                "codex/decision-explorer-phase1-ui-first-pass",
                "merged-main-green as PR #364",
                "818540b424dc92df0ec59de68e456d0ce080adbf",
                "DX-P1-G06",
                "codex/decision-explorer-phase1-ui-navigation",
                "merged-main-green as PR #365",
                "66242b7911c123b1f20f2820249b7173a3ef575a",
                "DX-P1-G07",
                "codex/decision-explorer-phase1-docs-examples",
                "merged-main-green as PR #366",
                "3d85730efc979373c2838e414c78c16df43656a9",
                "DX-P1-G08",
                "codex/decision-explorer-phase1-hardening",
                "PR #367 open; checks pending",
                "DX-P1-G09",
                "codex/decision-explorer-phase1-final-handoff",
                "Pending, failed, cancelled, stale, skipped-only, duplicate-only, or wrong-head checks do not count",
                "Decision: continue")) {
            assertTrue(board.contains(expected), "phase 1 board should track " + expected);
        }
    }

    @Test
    void phase1ScopeAlignsWithBootstrapHandoffSources() throws IOException {
        String closeout = read(BOOTSTRAP_CLOSEOUT);
        String dataContract = read(DATA_CONTRACT);
        String agentSchema = read(AGENT_SCHEMA);
        String evidenceLane = read(EVIDENCE_LANE);
        String phase0Gate = read(PHASE0_GATE);

        for (String expected : List.of(
                "Decision Explorer Implementation Phase 1",
                "fresh scoped contract",
                "implementation is future work")) {
            assertTrue(closeout.contains(expected), "bootstrap closeout should hand off " + expected);
        }

        for (String expected : List.of("read-only", "simulation-only", "not-proven")) {
            assertTrue(dataContract.contains(expected), "data contract should preserve " + expected);
            assertTrue(agentSchema.contains(expected), "agent schema should preserve " + expected);
            assertTrue(evidenceLane.contains(expected), "evidence lane should preserve " + expected);
            assertTrue(phase0Gate.contains(expected), "phase 0 gate should preserve " + expected);
        }
    }

    @Test
    void sessionManagerRecordsPhase1BranchCreationCheckpoint() throws IOException {
        String session = read(SESSION).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "decision explorer implementation phase 1",
                "current pr slot: dx-p1-g08",
                "pr #360",
                "pr #360 merged as",
                "0fe9331a757973d93820bbae46b05ae53f8ba64a",
                "pr #361",
                "pr #361 merged as",
                "fca765b897937cd20ee9955bfb7f9ba7a665a9be",
                "pr #362",
                "pr #362 merged as",
                "af351b043fbc3ff0ffff50d9c0f17a667f84b7af",
                "pr #363",
                "pr #363 merged as",
                "20b9080d5c24ef3807e15a3ef8367a8ef1ae4915",
                "pr #364",
                "pr #364 merged as",
                "818540b424dc92df0ec59de68e456d0ce080adbf",
                "pr #365",
                "pr #365 merged as",
                "66242b7911c123b1f20f2820249b7173a3ef575a",
                "pr #366",
                "pr #366 merged as",
                "3d85730efc979373c2838e414c78c16df43656a9",
                "pr #367",
                "codex/decision-explorer-phase1-docs-examples",
                "codex/decision-explorer-phase1-hardening",
                "decisionexplorerpayloadservice.java",
                "decisionexplorerpayloadservicetest.java",
                "decision_explorer_phase1_reviewer_examples.md",
                "agentdecisionexplorerphase1reviewerexamplesdocumentationtest.java",
                "post /api/routing/decision-explorer",
                "remote status:",
                "decision: continue")) {
            assertTrue(session.contains(expected), "session manager should record " + expected);
        }
    }

    @Test
    void phase1ScopeAndBoardPreserveSafetyAndNotProvenBoundaries() throws IOException {
        String combined = (read(SCOPE) + "\n" + read(BOARD)).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "additive",
                "read-only",
                "simulation-only",
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
                "replay/export behavior",
                "storage behavior",
                "evidence packet implementation",
                "broader automation",
                "secrets",
                "external targets",
                "cloud targets",
                "tenant targets",
                "rulesets")) {
            assertTrue(combined.contains(expected), "phase 1 scope should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "decision explorer is implemented",
                "decision explorer endpoint is implemented",
                "decision explorer ui is implemented",
                "decision explorer storage is implemented",
                "evidence packet is implemented",
                "replay export is implemented",
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "autonomous production action is enabled")) {
            assertFalse(combined.contains(forbidden), "phase 1 scope must not overclaim " + forbidden);
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
