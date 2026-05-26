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

class AgentDecisionExplorerAgentSchemaDocumentationTest {
    private static final Path SCHEMA_CONTRACT =
            Path.of("docs/agent/DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md");
    private static final Path DATA_CONTRACT = Path.of("docs/agent/DECISION_EXPLORER_DATA_CONTRACT.md");
    private static final Path ADR =
            Path.of("docs/adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerAgentSchemaDocumentationTest.java");

    @Test
    void schemaContractExistsAndStatesPlanningOnlyScope() throws IOException {
        String schema = read(SCHEMA_CONTRACT);

        for (String expected : List.of(
                "# Decision Explorer Agent Schema Contract",
                "Status: planned / docs-test-only.",
                "Classification: WARN / decision-explorer-bootstrap.",
                "Campaign slot: DX-G05.",
                "DECISION_EXPLORER_DATA_CONTRACT.md",
                "ADR-0010-Interactive-Decision-Explorer-Architecture.md",
                "DECISION_EXPLORER_CAMPAIGN_BOARD.md",
                "planned",
                "read-only",
                "simulation-only",
                "docs/test-only",
                "no runtime endpoint/UI/storage/export/replay/evidence-packet implementation claim")) {
            assertTrue(schema.contains(expected), "schema contract should state scope item " + expected);
        }
    }

    @Test
    void schemaContractDefinesAgentIdentityAndRequiredFields() throws IOException {
        String schema = read(SCHEMA_CONTRACT);

        for (String expected : List.of(
                "AgentStructuredOutputV1",
                "decision-explorer-agent-schema/v1",
                "AI_AGENT_STRUCTURED_UNDERSTANDING",
                "`schemaVersion`",
                "`schemaPurpose`",
                "`schemaStatus`",
                "`payloadContractVersion`",
                "`payloadObject`",
                "`agentObject`",
                "`boundaryFlags`",
                "`parseSafetyNote`",
                "`decisionReadout`",
                "`selectedCandidateId`",
                "`candidateReadouts`",
                "`factorContributions`",
                "`policyGateReadouts`",
                "`decisionDiffReadouts`",
                "`evidencePacketReadouts`",
                "`sourceReferences`",
                "`unknowns`",
                "`notProvenBoundaries`",
                "`validationNotes`")) {
            assertTrue(schema.contains(expected), "schema contract should define field " + expected);
        }
    }

    @Test
    void schemaContractDefinesEnumsUnknownHandlingAndParseSafety() throws IOException {
        String schema = read(SCHEMA_CONTRACT);

        for (String expected : List.of(
                "Enum-Like Values",
                "Schema Status Values",
                "Candidate Status Values",
                "Policy Gate Outcome Values",
                "Evidence Status Values",
                "Boundary Flag Names",
                "UNKNOWN",
                "UNAVAILABLE",
                "NOT_APPLICABLE",
                "NOT_IMPLEMENTED",
                "Unknown And Null Handling",
                "Agent Parse Safety Rules",
                "Do not treat explanation as authority",
                "Do not choose live production routes from this schema",
                "Do not infer hidden scoring",
                "Do not treat policy gate visualization as branch protection",
                "Prefer `UNKNOWN`, `UNAVAILABLE`, `NOT_APPLICABLE`, or `NOT_IMPLEMENTED`")) {
            assertTrue(schema.contains(expected), "schema contract should define parse safety item " + expected);
        }
    }

    @Test
    void schemaContractAlignsWithDxG04ObjectVocabulary() throws IOException {
        String schema = read(SCHEMA_CONTRACT);
        String dataContract = read(DATA_CONTRACT);

        for (String expected : List.of(
                "`DecisionExplorerPayloadV1`",
                "`DecisionReadoutV1`",
                "`CandidateReadoutV1`",
                "`FactorContributionV1`",
                "`PolicyGateReadoutV1`",
                "`DecisionDiffReadoutV1`",
                "`EvidencePacketReadoutV1`",
                "`AgentStructuredOutputV1`")) {
            assertTrue(schema.contains(expected), "schema contract should align to object " + expected);
            assertTrue(dataContract.contains(expected), "data contract should still define object " + expected);
        }

        for (String expected : List.of(
                "DX-G05 should define a more explicit AI-agent-readable schema contract",
                "decision-explorer-snapshot/v1",
                "Agent Explanation")) {
            assertTrue(dataContract.contains(expected), "data contract should align with schema item " + expected);
        }
    }

    @Test
    void schemaContractIncludesStaticExampleAndBoundaryFlags() throws IOException {
        String schema = read(SCHEMA_CONTRACT);

        for (String expected : List.of(
                "Static Example Agent Output",
                "\"schemaVersion\": \"decision-explorer-agent-schema/v1\"",
                "\"schemaPurpose\": \"AI_AGENT_STRUCTURED_UNDERSTANDING\"",
                "\"schemaStatus\": [\"PLANNED\", \"DOCS_TEST_ONLY\", \"READ_ONLY\", \"SIMULATION_ONLY\"]",
                "\"payloadContractVersion\": \"decision-explorer-snapshot/v1\"",
                "\"payloadObject\": \"DecisionExplorerPayloadV1\"",
                "\"agentObject\": \"AgentStructuredOutputV1\"",
                "\"object\": \"DecisionReadoutV1\"",
                "\"object\": \"CandidateReadoutV1\"",
                "\"object\": \"FactorContributionV1\"",
                "\"object\": \"PolicyGateReadoutV1\"",
                "\"object\": \"DecisionDiffReadoutV1\"",
                "\"object\": \"EvidencePacketReadoutV1\"",
                "\"notRuntimeImplementation\": true",
                "\"notBroaderAutomation\": true",
                "AI agents must not infer hidden scoring")) {
            assertTrue(schema.contains(expected), "schema contract should include example or flag " + expected);
        }
    }

    @Test
    void schemaContractPreservesNotProvenAndNonImplementationBoundaries() throws IOException {
        String normalized = read(SCHEMA_CONTRACT).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not add endpoints",
                "does not add endpoints, runtime behavior",
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
                "broader automation",
                "not a json schema file",
                "not an implemented endpoint response")) {
            assertTrue(normalized.contains(expected), "schema contract should preserve boundary " + expected);
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
            assertFalse(normalized.contains(forbidden), "schema contract must not overclaim " + forbidden);
        }
    }

    @Test
    void schemaContractAlignsWithAdrAndCampaignBoard() throws IOException {
        String schema = read(SCHEMA_CONTRACT);
        String adr = read(ADR);
        String board = read(BOARD);

        for (String expected : List.of(
                "DX-G05 should define the future AI-agent-readable schema contract",
                "AI agents should get structured JSON direction with stable field names",
                "reason codes",
                "policy-gate outcomes",
                "not-proven boundary flags")) {
            assertTrue(adr.contains(expected), "ADR should align with DX-G05 schema item " + expected);
        }

        for (String expected : List.of(
                "DX-G05 - agent-readable schema contract",
                "codex/dx-g05-agent-readable-schema-contract",
                "AgentDecisionExplorerAgentSchemaDocumentationTest",
                "read-only",
                "simulation-only")) {
            assertTrue(board.contains(expected), "board should align with DX-G05 schema item " + expected);
        }

        for (String expected : List.of(
                "DX-G06 should define evidence lanes and source cards",
                "DX-G07 should define Phase 0 verification guardrails",
                "DX-G08 should define implementation slices",
                "DX-G09 should provide a reviewer walkthrough",
                "DX-G10 should close the bootstrap")) {
            assertTrue(schema.contains(expected), "schema contract should preserve future slot relationship " + expected);
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
