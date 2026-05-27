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

class AgentDecisionExplorerImplementationPlanDocumentationTest {
    private static final Path PLAN = Path.of("docs/agent/DECISION_EXPLORER_IMPLEMENTATION_PLAN.md");
    private static final Path README = Path.of("README.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path ADR =
            Path.of("docs/adr/ADR-0010-Interactive-Decision-Explorer-Architecture.md");
    private static final Path DATA_CONTRACT = Path.of("docs/agent/DECISION_EXPLORER_DATA_CONTRACT.md");
    private static final Path AGENT_SCHEMA =
            Path.of("docs/agent/DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md");
    private static final Path EVIDENCE_LANE = Path.of("docs/agent/DECISION_EXPLORER_EVIDENCE_LANE.md");
    private static final Path PHASE0_GATE =
            Path.of("docs/agent/DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerImplementationPlanDocumentationTest.java");

    @Test
    void implementationPlanExistsAndStatesPlanningOnlyScope() throws IOException {
        String plan = read(PLAN);

        for (String expected : List.of(
                "# Decision Explorer Implementation Plan",
                "Status: planned / docs-test-only.",
                "Classification: WARN / decision-explorer-bootstrap.",
                "Campaign slot: DX-G08.",
                "DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md",
                "ADR-0010-Interactive-Decision-Explorer-Architecture.md",
                "DECISION_EXPLORER_DATA_CONTRACT.md",
                "DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md",
                "DECISION_EXPLORER_EVIDENCE_LANE.md",
                "DECISION_EXPLORER_CAMPAIGN_BOARD.md",
                "planned",
                "planning-only language",
                "no runtime endpoint/UI/storage/export/replay implementation claim",
                "read-only",
                "simulation-only",
                "docs-test-only")) {
            assertTrue(plan.contains(expected), "implementation plan should state scope item " + expected);
        }
    }

    @Test
    void implementationPlanRequiresDocsBeforeCode() throws IOException {
        String plan = read(PLAN);

        for (String expected : List.of(
                "Required Docs Before Code",
                "README and Reviewer Trust Map links",
                "ADR-0010",
                "Data contract",
                "Agent schema contract",
                "Evidence lane",
                "Phase 0 verification gate",
                "required source is missing, stale, unguarded, overclaiming",
                "future implementation work should stop before code")) {
            assertTrue(plan.contains(expected), "implementation plan should require docs-before-code item " + expected);
        }
    }

    @Test
    void implementationPlanDefinesFutureSlicesWithoutImplementingThem() throws IOException {
        String plan = read(PLAN);

        for (String expected : List.of(
                "Planned Implementation Slices",
                "The slices below are a future planning sequence only. They are not implemented by DX-G08.",
                "Slice 0 - Contract freeze",
                "Slice 1 - Readout vocabulary",
                "Slice 2 - Snapshot assembly",
                "Slice 3 - Human readout",
                "Slice 4 - Agent structured output",
                "Slice 5 - Evidence lane binding",
                "Slice 6 - What-if preview",
                "Slice 7 - Reviewer walkthrough",
                "Future Slice Families",
                "future backend model slices",
                "future endpoint slices",
                "future static UI slices",
                "future what-if/counterfactual slices",
                "future policy gate visualization slices",
                "future evidence packet renderer slices",
                "Test Strategy",
                "test strategy",
                "Verification Strategy",
                "verification strategy",
                "Branch/PR Sequence",
                "branch/PR sequence",
                "implementation is future work",
                "Future Non-Goals",
                "Review Checklist")) {
            assertTrue(plan.contains(expected), "implementation plan should define future slice item " + expected);
        }
    }

    @Test
    void implementationPlanCoversContractsEvidenceAndAgentSchemaObjects() throws IOException {
        String plan = read(PLAN);

        for (String expected : List.of(
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
                "parseability",
                "source-card template",
                "research intake rules",
                "stale-information retirement",
                "repo bloat prevention",
                "compacting policy",
                "no raw research dumps",
                "evidence packet future path")) {
            assertTrue(plan.contains(expected), "implementation plan should preserve contract item " + expected);
        }
    }

    @Test
    void implementationPlanDefinesStopConditionsAndVerificationLadder() throws IOException {
        String plan = read(PLAN);

        for (String expected : List.of(
                "Stop Conditions Before Java, Backend, Or UI Work",
                "required PR checks are stale, failed, cancelled, pending, or duplicate-only",
                "post-merge main CI and CodeQL",
                "Slice Verification Expectations",
                "Focused guard",
                "Relevant Decision Explorer docs selector bundle",
                "Full Maven tests",
                "Package verification",
                "Enterprise lab smoke package",
                "Current-head PR CI, CodeQL, and Dependency Review",
                "does not add CI workflows, Maven configuration, Docker behavior")) {
            assertTrue(plan.contains(expected), "implementation plan should preserve verification item " + expected);
        }
    }

    @Test
    void implementationPlanAlignsWithPriorDecisionExplorerArtifacts() throws IOException {
        String readme = read(README);
        String trustMap = read(TRUST_MAP);
        String adr = read(ADR);
        String dataContract = read(DATA_CONTRACT);
        String agentSchema = read(AGENT_SCHEMA);
        String evidenceLane = read(EVIDENCE_LANE);
        String phase0Gate = read(PHASE0_GATE);
        String board = read(BOARD);

        for (String expected : List.of(
                "Decision Explorer Architecture Bootstrap Campaign",
                "read-only",
                "simulation-only")) {
            assertTrue(readme.contains(expected), "README should preserve Decision Explorer discovery item " + expected);
            assertTrue(trustMap.contains(expected),
                    "Reviewer Trust Map should preserve Decision Explorer discovery item " + expected);
        }

        for (String expected : List.of(
                "DX-G08 should define implementation slices",
                "planned",
                "read-only",
                "simulation-only")) {
            assertTrue(adr.contains(expected), "ADR should align with implementation plan item " + expected);
            assertTrue(dataContract.contains(expected),
                    "data contract should align with implementation plan item " + expected);
            assertTrue(agentSchema.contains(expected),
                    "agent schema should align with implementation plan item " + expected);
            assertTrue(evidenceLane.contains(expected),
                    "evidence lane should align with implementation plan item " + expected);
        }

        assertTrue(phase0Gate.contains("DX-G08 should define implementation slices without implementing them"),
                "phase 0 gate should point to DX-G08 implementation slices");

        for (String expected : List.of(
                "DX-G08 | Decision Explorer implementation plan",
                "codex/dx-g08-decision-explorer-implementation-plan",
                "AgentDecisionExplorerImplementationPlanDocumentationTest",
                "Future implementation slices")) {
            assertTrue(board.contains(expected), "campaign board should align with implementation plan item " + expected);
        }
    }

    @Test
    void implementationPlanPreservesNotProvenAndNonImplementationBoundaries() throws IOException {
        String normalized = read(PLAN).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not add java classes",
                "does not approve implementation in this bootstrap campaign",
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
                "broader automation",
                "no live mutation",
                "no hidden side effects",
                "no runtime endpoint/ui/storage/evidence-packet implementation claim")) {
            assertTrue(normalized.contains(expected), "implementation plan should preserve boundary " + expected);
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
            assertFalse(normalized.contains(forbidden), "implementation plan must not overclaim " + forbidden);
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
