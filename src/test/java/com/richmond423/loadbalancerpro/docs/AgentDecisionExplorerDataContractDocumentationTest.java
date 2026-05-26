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

class AgentDecisionExplorerDataContractDocumentationTest {
    private static final Path DATA_CONTRACT = Path.of("docs/agent/DECISION_EXPLORER_DATA_CONTRACT.md");
    private static final Path ADR =
            Path.of("docs/adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerDataContractDocumentationTest.java");

    @Test
    void dataContractExistsAndStatesPlanningOnlyScope() throws IOException {
        String contract = read(DATA_CONTRACT);

        for (String expected : List.of(
                "# Decision Explorer Data Contract",
                "Status: planned / docs-test-only.",
                "Classification: WARN / decision-explorer-bootstrap.",
                "Campaign slot: DX-G04.",
                "ADR-0010-Interactive-Decision-Explorer-Architecture.md",
                "DECISION_EXPLORER_CAMPAIGN_BOARD.md",
                "planned",
                "read-only",
                "simulation-only",
                "docs-test-only")) {
            assertTrue(contract.contains(expected), "data contract should state scope item " + expected);
        }
    }

    @Test
    void dataContractDefinesStableTopLevelPayloadFields() throws IOException {
        String contract = read(DATA_CONTRACT);

        for (String expected : List.of(
                "decision-explorer-snapshot/v1",
                "`contractVersion`",
                "`contractStatus`",
                "`decisionExplorerMode`",
                "`decisionId`",
                "`scenarioId`",
                "`selectedCandidate`",
                "`candidateSet`",
                "`signalSummary`",
                "`policyGateSummary`",
                "`reasonCodes`",
                "`humanExplanation`",
                "`agentExplanation`",
                "`evidenceReferences`",
                "`whatIfPreview`",
                "`notProvenBoundaries`")) {
            assertTrue(contract.contains(expected), "data contract should define field " + expected);
        }
    }

    @Test
    void dataContractDefinesCandidateSignalPolicyHumanAndAgentShapes() throws IOException {
        String contract = read(DATA_CONTRACT);

        for (String expected : List.of(
                "Candidate Object",
                "`candidateId`",
                "`candidateLabel`",
                "`candidateStatus`",
                "`visibleSignals`",
                "`unknownSignalNote`",
                "Signal Summary",
                "`visibleSignalCount`",
                "`hiddenInternalsPolicy`",
                "`scoringAvailability`",
                "Policy Gate Summary",
                "`gateId`",
                "`outcome`",
                "`authorizationBoundary`",
                "Human Explanation",
                "Agent Explanation",
                "AI_AGENT_STRUCTURED_UNDERSTANDING",
                "`boundaryFlags`",
                "`parseSafetyNote`")) {
            assertTrue(contract.contains(expected), "data contract should define shape item " + expected);
        }
    }

    @Test
    void dataContractPreservesJsonEvidenceAndWhatIfPlanningBoundaries() throws IOException {
        String contract = read(DATA_CONTRACT);

        for (String expected : List.of(
                "Static Example Payload",
                "\"contractVersion\": \"decision-explorer-snapshot/v1\"",
                "\"decisionExplorerMode\": [\"PLANNED\", \"READ_ONLY\", \"SIMULATION_ONLY\"]",
                "\"schemaPurpose\": \"AI_AGENT_STRUCTURED_UNDERSTANDING\"",
                "\"planned\": true",
                "\"readOnly\": true",
                "\"simulationOnly\": true",
                "\"notProductionProof\": true",
                "Evidence References",
                "What-If Preview",
                "SIMULATION_ONLY",
                "No live mutation, storage write, export, or production action")) {
            assertTrue(contract.contains(expected), "data contract should preserve JSON or boundary item " + expected);
        }
    }

    @Test
    void dataContractRejectsImplementationAndProductionOverclaims() throws IOException {
        String normalized = read(DATA_CONTRACT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not add endpoints",
                "does not add endpoints, runtime",
                "does not prove",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "benchmark/load/stress evidence",
                "throughput/p95/p99 evidence",
                "replay/export behavior",
                "storage behavior",
                "runtime endpoint/ui/storage/evidence-packet implementation",
                "autonomous production action",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "data contract should preserve not-proven boundary " + expected);
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
            assertFalse(normalized.contains(forbidden), "data contract must not overclaim " + forbidden);
        }
    }

    @Test
    void dataContractAlignsWithAdrAndCampaignBoard() throws IOException {
        String contract = read(DATA_CONTRACT);
        String adr = read(ADR);
        String board = read(BOARD);

        for (String expected : List.of(
                "DX-G04 should define the future Decision Explorer data contract",
                "planned",
                "read-only",
                "simulation-only")) {
            assertTrue(adr.contains(expected), "ADR should align with DX-G04 contract item " + expected);
        }

        for (String expected : List.of(
                "DX-G04 - Decision Explorer data contract",
                "codex/dx-g04-decision-explorer-data-contract",
                "AgentDecisionExplorerDataContractDocumentationTest",
                "read-only",
                "simulation-only")) {
            assertTrue(board.contains(expected), "board should align with DX-G04 contract item " + expected);
        }

        for (String expected : List.of(
                "DX-G05 should define a more explicit AI-agent-readable schema contract",
                "DX-G06 should define evidence lanes and source cards",
                "DX-G07 should define Phase 0 verification guardrails",
                "DX-G08 should define implementation slices",
                "DX-G09 should provide a reviewer walkthrough",
                "DX-G10 should close the bootstrap")) {
            assertTrue(contract.contains(expected), "data contract should preserve future slot relationship " + expected);
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
