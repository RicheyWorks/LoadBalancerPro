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

class AgentDecisionExplorerPhase0VerificationGateDocumentationTest {
    private static final Path GATE = Path.of("docs/agent/DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md");
    private static final Path ADR =
            Path.of("docs/adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md");
    private static final Path DATA_CONTRACT = Path.of("docs/agent/DECISION_EXPLORER_DATA_CONTRACT.md");
    private static final Path AGENT_SCHEMA =
            Path.of("docs/agent/DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md");
    private static final Path EVIDENCE_LANE = Path.of("docs/agent/DECISION_EXPLORER_EVIDENCE_LANE.md");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerPhase0VerificationGateDocumentationTest.java");

    @Test
    void phase0GateExistsAndStatesDocsOnlyScope() throws IOException {
        String gate = read(GATE);

        for (String expected : List.of(
                "# Decision Explorer Phase 0 Verification Gate",
                "Status: planned / docs-test-only.",
                "Classification: WARN / decision-explorer-bootstrap.",
                "Campaign slot: DX-G07.",
                "ADR-0010-Interactive-Decision-Explorer-Architecture.md",
                "DECISION_EXPLORER_DATA_CONTRACT.md",
                "DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md",
                "DECISION_EXPLORER_EVIDENCE_LANE.md",
                "DECISION_EXPLORER_CAMPAIGN_BOARD.md",
                "planned",
                "read-only",
                "simulation-only",
                "docs-test-only")) {
            assertTrue(gate.contains(expected), "phase 0 gate should state scope item " + expected);
        }
    }

    @Test
    void phase0GateDefinesInputsOutcomesAndExitCriteria() throws IOException {
        String gate = read(GATE);

        for (String expected : List.of(
                "Phase 0 Gate Goals",
                "Gate Inputs",
                "Gate Outcomes",
                "`PASS`",
                "`WARN`",
                "`BLOCK`",
                "Minimum Phase 0 Exit Criteria",
                "Verification Matrix",
                "Agent Review Workflow",
                "Failure Handling",
                "Future Path")) {
            assertTrue(gate.contains(expected), "phase 0 gate should define review item " + expected);
        }
    }

    @Test
    void phase0GateCoversPriorContractsAndEvidenceLaneControls() throws IOException {
        String gate = read(GATE);

        for (String expected : List.of(
                "DecisionExplorerPayloadV1",
                "DecisionReadoutV1",
                "CandidateReadoutV1",
                "agent consumption goals",
                "stable identifiers",
                "JSON field naming rules",
                "null/unknown handling",
                "enum stability",
                "parseability",
                "source-card template",
                "research intake rules",
                "stale-information retirement policy",
                "repo bloat prevention",
                "compacting policy",
                "no raw research dumps",
                "evidence packet future path")) {
            assertTrue(gate.contains(expected), "phase 0 gate should cover control " + expected);
        }
    }

    @Test
    void phase0GateAlignsWithExistingCampaignArtifacts() throws IOException {
        String adr = read(ADR);
        String dataContract = read(DATA_CONTRACT);
        String agentSchema = read(AGENT_SCHEMA);
        String evidenceLane = read(EVIDENCE_LANE);
        String board = read(BOARD);

        assertTrue(adr.contains("DX-G07 should define Phase 0 verification guardrails"),
                "ADR should name the DX-G07 verification gate");
        assertTrue(dataContract.contains("DX-G07 should define Phase 0 verification guardrails"),
                "data contract should name the DX-G07 verification gate");
        assertTrue(agentSchema.contains("DX-G07 should define Phase 0 verification guardrails"),
                "agent schema should name the DX-G07 verification gate");
        assertTrue(evidenceLane.contains("DX-G07 should define Phase 0 verification guardrails"),
                "evidence lane should name the DX-G07 verification gate");

        for (String expected : List.of(
                "DX-G07 | Phase 0 verification guardrails",
                "codex/dx-g07-phase0-verification-guardrails",
                "AgentDecisionExplorerPhase0VerificationGateDocumentationTest",
                "Pre-implementation gate")) {
            assertTrue(board.contains(expected), "campaign board should align with phase 0 gate item " + expected);
        }
    }

    @Test
    void phase0GatePreservesMergeHealthAndRemoteCheckRules() throws IOException {
        String gate = read(GATE);

        for (String expected : List.of(
                "current-head green",
                "post-merge main CI and CodeQL",
                "stale, failed, cancelled, pending, or duplicate-only required checks",
                "changed files stay within the active campaign slot",
                "required-check weakening",
                "branch deletion",
                "ruleset changes")) {
            assertTrue(gate.contains(expected), "phase 0 gate should preserve merge rule " + expected);
        }
    }

    @Test
    void phase0GatePreservesNotProvenAndNonImplementationBoundaries() throws IOException {
        String normalized = read(GATE).toLowerCase(Locale.ROOT);

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
                "evidence packet implementation",
                "autonomous production action",
                "live mutation",
                "hidden side effects",
                "broader automation",
                "not ci configuration",
                "not runtime enforcement states")) {
            assertTrue(normalized.contains(expected), "phase 0 gate should preserve boundary " + expected);
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
            assertFalse(normalized.contains(forbidden), "phase 0 gate must not overclaim " + forbidden);
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
