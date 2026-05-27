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

class AgentDecisionExplorerReviewerWalkthroughDocumentationTest {
    private static final Path WALKTHROUGH =
            Path.of("docs/agent/DECISION_EXPLORER_REVIEWER_WALKTHROUGH.md");
    private static final Path IMPLEMENTATION_PLAN =
            Path.of("docs/agent/DECISION_EXPLORER_IMPLEMENTATION_PLAN.md");
    private static final Path PHASE0_GATE =
            Path.of("docs/agent/DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md");
    private static final Path DATA_CONTRACT = Path.of("docs/agent/DECISION_EXPLORER_DATA_CONTRACT.md");
    private static final Path AGENT_SCHEMA =
            Path.of("docs/agent/DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md");
    private static final Path EVIDENCE_LANE = Path.of("docs/agent/DECISION_EXPLORER_EVIDENCE_LANE.md");
    private static final Path BOARD = Path.of("docs/agent/DECISION_EXPLORER_CAMPAIGN_BOARD.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerReviewerWalkthroughDocumentationTest.java");

    @Test
    void reviewerWalkthroughExistsAndStatesDocsOnlyScope() throws IOException {
        String walkthrough = read(WALKTHROUGH);

        for (String expected : List.of(
                "# Decision Explorer Reviewer/Operator Walkthrough",
                "Status: planned / docs-test-only.",
                "Classification: WARN / decision-explorer-bootstrap.",
                "Campaign slot: DX-G09.",
                "DECISION_EXPLORER_IMPLEMENTATION_PLAN.md",
                "DECISION_EXPLORER_PHASE0_VERIFICATION_GATE.md",
                "DECISION_EXPLORER_DATA_CONTRACT.md",
                "DECISION_EXPLORER_AGENT_SCHEMA_CONTRACT.md",
                "DECISION_EXPLORER_EVIDENCE_LANE.md",
                "DECISION_EXPLORER_CAMPAIGN_BOARD.md",
                "planned",
                "read-only",
                "simulation-only",
                "docs-test-only",
                "no runtime endpoint/UI/storage/export/replay implementation claim")) {
            assertTrue(walkthrough.contains(expected), "walkthrough should state scope item " + expected);
        }
    }

    @Test
    void reviewerWalkthroughDefinesGateRolesStepsAndQuestions() throws IOException {
        String walkthrough = read(WALKTHROUGH);

        for (String expected : List.of(
                "Pre-Walkthrough Gate",
                "Walkthrough Roles",
                "Human reviewer",
                "Operator reviewer",
                "AI agent",
                "Walkthrough Steps",
                "Step 1 - Confirm Source-Visible Scope",
                "Step 2 - Read The Planned Decision Summary",
                "Step 3 - Compare Candidate Readouts",
                "Step 4 - Review Policy Gate Visualization",
                "Step 5 - Inspect What-If And Counterfactual Readouts",
                "Step 6 - Validate Evidence And Source Cards",
                "Step 7 - Read Agent Structured Output",
                "Step 8 - Record Review Questions And Stop Conditions",
                "AI-Agent Walkthrough Questions",
                "Reviewer Checklist")) {
            assertTrue(walkthrough.contains(expected), "walkthrough should define review item " + expected);
        }
    }

    @Test
    void reviewerWalkthroughSupportsHumanAndAgentStructuredUnderstanding() throws IOException {
        String walkthrough = read(WALKTHROUGH);

        for (String expected : List.of(
                "human reviewer understanding",
                "AI-agent structured understanding",
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
                "enum stability expectations",
                "null and unknown handling",
                "parseability",
                "low-ambiguity boundary flags")) {
            assertTrue(walkthrough.contains(expected), "walkthrough should support structured item " + expected);
        }
    }

    @Test
    void reviewerWalkthroughPreservesEvidenceLaneAndPolicyGateBoundaries() throws IOException {
        String walkthrough = read(WALKTHROUGH);

        for (String expected : List.of(
                "policy gate visualization display-only",
                "not authorization",
                "not runtime enforcement",
                "source-card template",
                "research intake rules",
                "stale-information retirement policy",
                "repo bloat prevention",
                "compacting policy",
                "no raw research dumps",
                "evidence packet future path",
                "source cards are reviewer context only")) {
            assertTrue(walkthrough.contains(expected), "walkthrough should preserve evidence/gate item " + expected);
        }
    }

    @Test
    void reviewerWalkthroughAlignsWithPriorDecisionExplorerArtifacts() throws IOException {
        String implementationPlan = read(IMPLEMENTATION_PLAN);
        String phase0Gate = read(PHASE0_GATE);
        String dataContract = read(DATA_CONTRACT);
        String agentSchema = read(AGENT_SCHEMA);
        String evidenceLane = read(EVIDENCE_LANE);
        String board = read(BOARD);

        for (String expected : List.of(
                "DX-G09 should provide a reviewer walkthrough",
                "human and AI-agent consumption")) {
            assertTrue(implementationPlan.contains(expected),
                    "implementation plan should align with walkthrough item " + expected);
            assertTrue(phase0Gate.contains(expected), "phase 0 gate should align with walkthrough item " + expected);
            assertTrue(dataContract.contains(expected), "data contract should align with walkthrough item " + expected);
            assertTrue(agentSchema.contains(expected), "agent schema should align with walkthrough item " + expected);
            assertTrue(evidenceLane.contains(expected), "evidence lane should align with walkthrough item " + expected);
        }

        for (String expected : List.of(
                "DX-G09 | Reviewer walkthrough",
                "codex/dx-g09-reviewer-walkthrough",
                "AgentDecisionExplorerReviewerWalkthroughDocumentationTest",
                "Human and agent walkthrough")) {
            assertTrue(board.contains(expected), "campaign board should align with walkthrough item " + expected);
        }
    }

    @Test
    void reviewerWalkthroughPreservesStopConditionsAndNotProvenBoundaries() throws IOException {
        String normalized = read(WALKTHROUGH).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "does not add java classes",
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
                "no hidden writes",
                "no hidden approvals",
                "no hidden network calls",
                "stop before java/backend/ui/runtime work",
                "current-head checks are stale, failed, cancelled, pending, skipped-only, or duplicate-only")) {
            assertTrue(normalized.contains(expected), "walkthrough should preserve boundary " + expected);
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
            assertFalse(normalized.contains(forbidden), "walkthrough must not overclaim " + forbidden);
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
