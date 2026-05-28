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

class AgentLaseRoutingIntelligencePhase1FinalHandoffDocumentationTest {
    private static final Path HANDOFF =
            Path.of("docs/agent/LASE_ROUTING_INTELLIGENCE_PHASE1_FINAL_HANDOFF.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentLaseRoutingIntelligencePhase1FinalHandoffDocumentationTest.java");

    @Test
    void finalHandoffListsMergedImplementationPrsAndMainGate() throws IOException {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "# LASE Routing Intelligence Infrastructure Phase 1 Final Handoff",
                "Implementation main before closeout: `4a82c0297f7df46a62ec5755117f8695eafaefd4`",
                "Final closeout branch: `codex/lase-routing-intelligence-final-closeout`",
                "final operator response",
                "#381",
                "codex/lase-routing-intelligence-confidence-summary",
                "235265c3119346b192a2fd66532e5a3d67153576",
                "#382",
                "codex/lase-routing-intelligence-candidate-confidence",
                "11232ed7520c203417f2fcbaae265dc9bd581bc2",
                "#383",
                "codex/lase-routing-intelligence-factor-status",
                "526045b70cc459a513f0607d351da69f8b1280e2",
                "#384",
                "codex/lase-routing-intelligence-status-explanation",
                "d8bb28d57319585eee3ac1416a2ce7597ccba010",
                "#385",
                "codex/lase-routing-intelligence-ui-status-summary",
                "6e6c22ddc3826bfb3f8ef5248970ac8631a79484",
                "#386",
                "codex/lase-routing-intelligence-status-fixtures",
                "4a82c0297f7df46a62ec5755117f8695eafaefd4")) {
            assertTrue(handoff.contains(expected), "handoff should include " + expected);
        }
    }

    @Test
    void finalHandoffNamesImplementedRoutingIntelligenceArtifacts() throws IOException {
        String handoff = read(HANDOFF);

        for (String expected : List.of(
                "DecisionExplorerConfidenceSummaryV1",
                "DecisionExplorerConfidenceSummaryService",
                "DecisionExplorerCandidateConfidenceV1",
                "DecisionExplorerFactorStatusV1",
                "DecisionExplorerStatusExplanationV1",
                "DecisionExplorerPayloadV1",
                "DecisionExplorerPayloadService",
                "/decision-explorer.html",
                "DecisionExplorerConfidenceSummaryFixtureCatalog",
                "STRONG",
                "PARTIAL",
                "UNKNOWN",
                "DEGRADED",
                "confidenceSummary")) {
            assertTrue(handoff.contains(expected), "handoff should name implemented artifact " + expected);
        }

        for (Path path : List.of(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerConfidenceSummaryV1.java"),
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerConfidenceSummaryService.java"),
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCandidateConfidenceV1.java"),
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerFactorStatusV1.java"),
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerStatusExplanationV1.java"),
                Path.of("src/main/resources/static/decision-explorer.html"),
                Path.of("src/test/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerConfidenceSummaryFixtureCatalogTest.java"))) {
            assertTrue(Files.exists(path), path + " should back the final handoff");
        }
    }

    @Test
    void finalHandoffRecordsTestsVerificationAndBoundaries() throws IOException {
        String normalized = read(HANDOFF).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        for (String expected : List.of(
                "decisionexplorerconfidencesummaryservicetest",
                "decisionexplorerpayloadservicetest",
                "decisionexplorerapicontracthardeningtest",
                "decisionexplorerstaticpagetest",
                "decisionexplorerconfidencesummaryfixturecatalogtest",
                "mvn -q test",
                "mvn -q \"-dskiptests\" package",
                "mvn -b package",
                "git diff --check",
                ".\\scripts\\smoke\\enterprise-lab-workflow.ps1 -package",
                "main ci run `26551418998`",
                "main codeql run `26551418999`",
                "build/test/package/smoke",
                "dependency review")) {
            assertTrue(normalized.contains(expected), "handoff should record " + expected);
        }

        for (String expected : List.of(
                "additive",
                "read-only",
                "did not change production routing",
                "no external targets",
                "does not prove production readiness",
                "live-cloud validation",
                "real-tenant validation",
                "benchmark/load/stress results",
                "throughput",
                "p95/p99",
                "replay/export/storage proof",
                "evidence-packet generation",
                "autonomous production action",
                "traffic shifting",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "handoff should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "production readiness is proven",
                "certified production",
                "live-cloud validated",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "traffic shifting enabled",
                "autonomous production action is enabled")) {
            assertFalse(normalized.contains(forbidden), "handoff must not overclaim " + forbidden);
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
