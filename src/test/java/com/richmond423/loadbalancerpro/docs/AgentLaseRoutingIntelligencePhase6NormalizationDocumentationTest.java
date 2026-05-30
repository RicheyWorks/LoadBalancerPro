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

class AgentLaseRoutingIntelligencePhase6NormalizationDocumentationTest {
    private static final Path NORMALIZATION =
            Path.of("docs/agent/LASE_ROUTING_INTELLIGENCE_PHASE6_REVIEWER_EVIDENCE_NORMALIZATION.md");
    private static final Path PHASE5_CLOSEOUT =
            Path.of("docs/agent/LASE_ROUTING_INTELLIGENCE_PHASE5_CLOSEOUT.md");
    private static final Path API_CONTRACTS = Path.of("docs/API_CONTRACTS.md");
    private static final Path TRUST_MAP = Path.of("docs/REVIEWER_TRUST_MAP.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentLaseRoutingIntelligencePhase6NormalizationDocumentationTest.java");

    @Test
    void normalizationAnchorDefinesCampaignAndFieldVocabulary() throws IOException {
        String anchor = read(NORMALIZATION);

        for (String expected : List.of(
                "# LASE Routing Intelligence Phase 6 Reviewer Evidence Normalization",
                "Campaign: LASE Routing Intelligence Phase 6 - Reviewer Evidence Normalization.",
                "Started from main SHA: `9d135fa9e2d451cc35379e003da7aa35d15e1f45`.",
                "Phase 5 closed with PR #437 at merge SHA `9d135fa9e2d451cc35379e003da7aa35d15e1f45`.",
                "Target: 10 PRs maximum.",
                "Do not start the next PR until the current PR is merged and main CI/CodeQL are green.",
                "Decision Explorer payload",
                "Confidence summary",
                "Routing diagnostics",
                "Route tradeoff analysis",
                "Shadow decision quality",
                "Counterfactual analysis",
                "Static reviewer page",
                "Scenario catalog",
                "DecisionExplorerPayloadV1",
                "confidenceSummary",
                "routingDiagnostics",
                "routeTradeoffAnalysis",
                "shadowDecisionQualityEvaluation",
                "counterfactualAnalysis",
                "/decision-explorer.html",
                "GET /api/routing/decision-explorer/scenarios",
                "not-proven boundaries")) {
            assertTrue(anchor.contains(expected), "normalization anchor should contain " + expected);
        }
    }

    @Test
    void normalizationAnchorPreservesSafetyBoundaries() throws IOException {
        String normalized = read(NORMALIZATION).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        for (String expected : List.of(
                "documentation and guard-test only",
                "no production routing change",
                "no production scoring change",
                "no proxy behavior change",
                "no allocation behavior change",
                "no replay execution",
                "no storage or export proof",
                "no evidence-packet generation proof",
                "no traffic shifting",
                "no runtime enforcement claim",
                "no maven, ci, docker, or compose behavior change",
                "no secrets, cloud targets, tenant targets, private-network targets, or external targets",
                "no production readiness",
                "no production certification",
                "no live-cloud validation",
                "no real-tenant validation",
                "no benchmark/load/stress proof",
                "no throughput, p95, or p99 proof",
                "no autonomous production action",
                "no broader automation")) {
            assertTrue(normalized.contains(expected), "normalization anchor should preserve " + expected);
        }

        for (String forbidden : List.of(
                "production readiness is proven",
                "certified production",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "runtime enforcement is enabled",
                "traffic shifting enabled",
                "autonomous production action is enabled")) {
            assertFalse(normalized.contains(forbidden), "normalization anchor must not overclaim " + forbidden);
        }
    }

    @Test
    void apiContractsAndTrustMapPointToNormalizationAnchor() throws IOException {
        String apiContracts = read(API_CONTRACTS);
        String trustMap = read(TRUST_MAP);

        for (String expected : List.of(
                "LASE_ROUTING_INTELLIGENCE_PHASE6_REVIEWER_EVIDENCE_NORMALIZATION.md",
                "reviewer-facing Decision Explorer evidence groups",
                "confidenceSummary",
                "routingDiagnostics",
                "routeTradeoffAnalysis",
                "shadowDecisionQualityEvaluation",
                "counterfactualAnalysis")) {
            assertTrue(apiContracts.contains(expected), "API contracts should point to " + expected);
        }

        for (String expected : List.of(
                "LASE Routing Intelligence Phase 6 reviewer evidence normalization",
                "agent/LASE_ROUTING_INTELLIGENCE_PHASE6_REVIEWER_EVIDENCE_NORMALIZATION.md",
                "canonical naming and boundary anchor",
                "does not add endpoints",
                "does not prove production readiness")) {
            assertTrue(trustMap.contains(expected), "trust map should point to " + expected);
        }
    }

    @Test
    void phase5CloseoutNoLongerContainsStalePendingCloseoutWording() throws IOException {
        String closeout = read(PHASE5_CLOSEOUT);
        String normalized = closeout.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        for (String expected : List.of(
                "Status: final closeout complete.",
                "Classification: PASS / Phase 5 implementation and closeout merged.",
                "Final closeout PR: [#437](https://github.com/RicheyWorks/LoadBalancerPro/pull/437).",
                "Final closeout head SHA: `dac438bb68fe58a97adb748577435467320b4f12`.",
                "Final closeout merge SHA and final main SHA: `9d135fa9e2d451cc35379e003da7aa35d15e1f45`.",
                "main CI run `26671719015`",
                "main CodeQL run `26671719007`")) {
            assertTrue(closeout.contains(expected), "Phase 5 closeout should contain " + expected);
        }

        for (String stale : List.of(
                "status: candidate closeout",
                "pending lase-p5-pr10 merge-health gate",
                "must still pass focused closeout documentation verification",
                "after the lase-p5-pr10 closeout pr merges and main is green")) {
            assertFalse(normalized.contains(stale), "Phase 5 closeout should not keep stale wording " + stale);
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
