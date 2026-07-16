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
    private static final Path API_CONTRACTS = Path.of("docs/API_CONTRACTS.md");
    private static final Path REVIEWER_TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path PHASE6_ANCHOR = Path.of(
            "docs/agent/LASE_ROUTING_INTELLIGENCE_PHASE6_REVIEWER_EVIDENCE_NORMALIZATION.md");
    private static final Path STATIC_PAGE =
            Path.of("src/main/resources/static/decision-explorer.html");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentDecisionExplorerReviewerWalkthroughDocumentationTest.java");

    @Test
    void reviewerWalkthroughStatesCurrentBoundedScope() throws IOException {
        String walkthrough = read(WALKTHROUGH);

        for (String expected : List.of(
                "# Decision Explorer Reviewer/Operator Walkthrough",
                "Status: current / docs-and-guard normalization.",
                "Classification: WARN / local reviewer evidence.",
                "Historical campaign slot: DX-G09.",
                "Normalization campaign slot: LASE-P6-PR5.",
                "/decision-explorer.html",
                "GET /api/routing/decision-explorer/scenarios",
                "POST /api/routing/decision-explorer",
                "same-origin static reviewer page",
                "read-only and simulation-only payload surface",
                "documentation and guard-test only",
                "does not add or change Java classes")) {
            assertTrue(walkthrough.contains(expected), "walkthrough should state scope item " + expected);
        }

        for (String stale : List.of(
                "Status: planned / docs-test-only.",
                "The Decision Explorer remains planned",
                "a future reviewer, operator, or AI agent should inspect",
                "no runtime endpoint/UI/storage/export/replay implementation claim")) {
            assertFalse(walkthrough.contains(stale), "walkthrough must retire stale bootstrap claim " + stale);
        }
    }

    @Test
    void reviewerWalkthroughDefinesCurrentWorkflowRolesAndQuestions() throws IOException {
        String walkthrough = read(WALKTHROUGH);

        for (String expected : List.of(
                "Pre-Walkthrough Gate",
                "Walkthrough Roles",
                "Human reviewer",
                "Operator reviewer",
                "AI agent",
                "Step 1 - Confirm The Current Local Surface",
                "Step 2 - Load The Scenario Catalog",
                "Step 3 - Run The Read-Only Sample",
                "Step 4 - Map Panels To Normalized Evidence Groups",
                "Step 5 - Compare Candidates, Factors, And Policy Gates",
                "Step 6 - Inspect Tradeoffs, Replay Readiness, And Counterfactuals",
                "Step 7 - Validate Evidence And Structured Output",
                "Step 8 - Record Review Questions And Stop Conditions",
                "AI-Agent Walkthrough Questions",
                "Reviewer Checklist",
                "Historical Relationship")) {
            assertTrue(walkthrough.contains(expected), "walkthrough should define review item " + expected);
        }
    }

    @Test
    void reviewerWalkthroughUsesPhase6EvidenceGroupsAcrossCurrentSources() throws IOException {
        String walkthrough = read(WALKTHROUGH);
        String apiContracts = read(API_CONTRACTS);
        String reviewerTrustMap = read(REVIEWER_TRUST_MAP);
        String phase6Anchor = read(PHASE6_ANCHOR);
        String staticPage = read(STATIC_PAGE);

        for (String field : List.of(
                "confidenceSummary",
                "routingDiagnostics",
                "routeTradeoffAnalysis",
                "shadowDecisionQualityEvaluation",
                "counterfactualAnalysis")) {
            assertTrue(walkthrough.contains(field), "walkthrough should name current field " + field);
            assertTrue(apiContracts.contains(field), "API contract should name current field " + field);
            assertTrue(reviewerTrustMap.contains(field), "trust map should name current field " + field);
            assertTrue(phase6Anchor.contains(field), "Phase 6 anchor should name current field " + field);
            assertTrue(staticPage.contains(field), "static page should consume current field " + field);
        }

        for (String currentSource : List.of(walkthrough, phase6Anchor, staticPage)) {
            assertTrue(currentSource.contains("notProvenBoundaries"),
                    "current reviewer source should name notProvenBoundaries");
        }
        assertTrue(reviewerTrustMap.contains("not-proven boundaries"),
                "trust map should preserve the human-facing not-proven boundary phrase");

        for (String panel : List.of(
                "Scenario Catalog",
                "Routing Intelligence Status",
                "Routing Diagnostics",
                "Route Tradeoff Intelligence",
                "Evidence Sufficiency",
                "Replay Readiness",
                "Shadow Decision Quality",
                "Counterfactual Analysis")) {
            assertTrue(walkthrough.contains(panel), "walkthrough should name current panel " + panel);
            assertTrue(phase6Anchor.contains(panel), "Phase 6 anchor should name current panel " + panel);
            assertTrue(staticPage.contains(panel), "static page should render current panel " + panel);
        }
    }

    @Test
    void reviewerWalkthroughPreservesStructuredReviewSemantics() throws IOException {
        String walkthrough = read(WALKTHROUGH);

        for (String expected : List.of(
                "DecisionExplorerPayloadV1",
                "decisionReadout",
                "selectedCandidate",
                "candidateSet",
                "agentStructuredOutput",
                "stable identifiers",
                "JSON field names",
                "enum values",
                "null and unknown handling",
                "parseability",
                "low-ambiguity boundary flags",
                "evidence-packet readout is not evidence-packet generation",
                "Replay readiness is distinct from replay execution")) {
            assertTrue(walkthrough.contains(expected), "walkthrough should preserve semantic item " + expected);
        }
    }

    @Test
    void reviewerWalkthroughPreservesStopConditionsAndNotProvenBoundaries() throws IOException {
        String normalized = read(WALKTHROUGH).toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "not runtime enforcement",
                "production readiness or production certification",
                "live-cloud validation or real-tenant validation",
                "benchmark/load/stress evidence or throughput/p95/p99 evidence",
                "production routing, scoring, proxy, or allocation behavior",
                "replay execution or replay/export behavior",
                "storage behavior or evidence-packet generation",
                "traffic shifting or autonomous production action",
                "broader automation",
                "no live mutation",
                "hidden writes",
                "hidden approvals",
                "hidden network calls",
                "current-head checks are stale, failed, cancelled, pending, skipped-only, or duplicate-only")) {
            assertTrue(normalized.contains(expected), "walkthrough should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "production readiness is proven",
                "production certification is proven",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "replay execution is implemented",
                "evidence packet generation is implemented",
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
