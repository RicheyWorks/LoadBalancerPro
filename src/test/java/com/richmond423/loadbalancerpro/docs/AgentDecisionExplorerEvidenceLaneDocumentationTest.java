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

class AgentDecisionExplorerEvidenceLaneDocumentationTest {
    private static final Path EVIDENCE_LANE = Path.of("docs/agent/DECISION_EXPLORER_EVIDENCE_LANE.md");
    private static final Path DATA_CONTRACT = Path.of("docs/agent/DECISION_EXPLORER_DATA_CONTRACT.md");
    private static final Path AGENT_SCHEMA =
            Path.of("docs/agent/DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md");
    private static final Path ADR =
            Path.of("docs/adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerEvidenceLaneDocumentationTest.java");

    @Test
    void evidenceLaneExistsAndStatesPlanningOnlyScope() throws IOException {
        String lane = read(EVIDENCE_LANE);

        for (String expected : List.of(
                "# Decision Explorer Evidence Lane",
                "Status: planned / docs-test-only.",
                "Classification: WARN / decision-explorer-bootstrap.",
                "Campaign slot: DX-G06.",
                "DECISION_EXPLORER_DATA_CONTRACT.md",
                "DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md",
                "ADR-0010-Interactive-Decision-Explorer-Architecture.md",
                "DECISION_EXPLORER_CAMPAIGN_BOARD.md",
                "planned",
                "read-only",
                "simulation-only",
                "docs-test-only")) {
            assertTrue(lane.contains(expected), "evidence lane should state scope item " + expected);
        }
    }

    @Test
    void evidenceLaneDefinesLaneAndSourceCardObjects() throws IOException {
        String lane = read(EVIDENCE_LANE);

        for (String expected : List.of(
                "DecisionExplorerEvidenceLaneV1",
                "DecisionExplorerSourceCardV1",
                "evidenceReferences",
                "sourceReferences",
                "evidencePacketReadouts",
                "Evidence Lane Goals",
                "Source Card Fields",
                "`sourceCardId`",
                "`sourceType`",
                "`sourcePath`",
                "`availability`",
                "`freshnessStatus`",
                "`trustLabel`",
                "`consumerUse`",
                "`evidenceReferenceId`",
                "`sourceReferenceId`",
                "`evidencePacketReadoutId`",
                "`boundaryNote`",
                "`missingReason`")) {
            assertTrue(lane.contains(expected), "evidence lane should define source-card item " + expected);
        }
    }

    @Test
    void evidenceLaneDefinesSourceTypesAndIntakeChecklist() throws IOException {
        String lane = read(EVIDENCE_LANE);

        for (String expected : List.of(
                "Evidence Source Types",
                "`DOC`",
                "`ADR`",
                "`GUARD_TEST`",
                "`FIXTURE`",
                "`WORKFLOW`",
                "`LOCAL_LAB_REFERENCE`",
                "`FUTURE_EVIDENCE_PACKET`",
                "`UNKNOWN`",
                "Intake Rules",
                "Source Card Intake Checklist",
                "repository-relative",
                "source-visible",
                "availability explicit",
                "freshness explicit",
                "trust label explicit")) {
            assertTrue(lane.contains(expected), "evidence lane should define intake/source type item " + expected);
        }
    }

    @Test
    void evidenceLaneIncludesStaticExampleAndBoundaryFlags() throws IOException {
        String lane = read(EVIDENCE_LANE);

        for (String expected : List.of(
                "Static Example Source Card",
                "\"laneVersion\": \"decision-explorer-evidence-lane/v1\"",
                "\"laneStatus\": [\"PLANNED\", \"DOCS_TEST_ONLY\", \"READ_ONLY\", \"SIMULATION_ONLY\"]",
                "\"object\": \"DecisionExplorerSourceCardV1\"",
                "\"sourceCardId\": \"source-card-adr-0010\"",
                "\"sourceType\": \"ADR\"",
                "\"sourcePath\": \"docs/adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md\"",
                "\"evidencePacketReadoutId\": \"EvidencePacketReadoutV1\"",
                "\"notRuntimeImplementation\": true",
                "\"notBroaderAutomation\": true",
                "No evidence packet generation, storage, export, replay execution, or proof claim")) {
            assertTrue(lane.contains(expected), "evidence lane should include example or boundary " + expected);
        }
    }

    @Test
    void evidenceLaneAlignsWithPriorContractsAndCampaignBoard() throws IOException {
        String lane = read(EVIDENCE_LANE);
        String dataContract = read(DATA_CONTRACT);
        String agentSchema = read(AGENT_SCHEMA);
        String adr = read(ADR);
        String board = read(BOARD);

        for (String expected : List.of(
                "DX-G06 should define evidence lanes and source cards that can populate `evidenceReferences`",
                "`evidenceReferences`",
                "`EvidencePacketReadoutV1`")) {
            assertTrue(dataContract.contains(expected), "data contract should align with evidence item " + expected);
        }

        for (String expected : List.of(
                "DX-G06 should define evidence lanes and source cards that can populate `sourceReferences`",
                "`sourceReferences`",
                "`evidencePacketReadouts`")) {
            assertTrue(agentSchema.contains(expected), "agent schema should align with evidence item " + expected);
        }

        for (String expected : List.of(
                "DX-G06 should define the future evidence lane and source cards",
                "evidence packet future path",
                "source cards")) {
            assertTrue(adr.contains(expected), "ADR should align with evidence lane item " + expected);
        }

        for (String expected : List.of(
                "DX-G06 | Evidence lane and source cards",
                "codex/dx-g06-evidence-lane-and-source-cards",
                "AgentDecisionExplorerEvidenceLaneDocumentationTest",
                "read-only",
                "simulation-only")) {
            assertTrue(board.contains(expected), "campaign board should align with evidence lane item " + expected);
        }

        for (String expected : List.of(
                "DX-G07 should define Phase 0 verification guardrails",
                "DX-G08 should define implementation slices",
                "DX-G09 should provide a reviewer walkthrough",
                "DX-G10 should close the bootstrap")) {
            assertTrue(lane.contains(expected), "evidence lane should preserve future slot relationship " + expected);
        }
    }

    @Test
    void evidenceLanePreservesNotProvenAndNonImplementationBoundaries() throws IOException {
        String normalized = read(EVIDENCE_LANE).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not add endpoints",
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
                "live mutation",
                "hidden side effects",
                "broader automation",
                "not generated output",
                "not runtime check results")) {
            assertTrue(normalized.contains(expected), "evidence lane should preserve boundary " + expected);
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
            assertFalse(normalized.contains(forbidden), "evidence lane must not overclaim " + forbidden);
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
