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

class AgentLaseRoutingIntelligencePhase5CloseoutDocumentationTest {
    private static final Path CLOSEOUT =
            Path.of("docs/agent/LASE_ROUTING_INTELLIGENCE_PHASE5_CLOSEOUT.md");
    private static final Path SOURCE = Path.of(
            "src/test/java/com/richmond423/loadbalancerpro/docs/"
                    + "AgentLaseRoutingIntelligencePhase5CloseoutDocumentationTest.java");

    @Test
    void closeoutListsMergedPhase5PrsAndMainGate() throws IOException {
        String closeout = read(CLOSEOUT);

        for (String expected : List.of(
                "# LASE Routing Intelligence Phase 5 Closeout",
                "LASE Routing Intelligence Phase 5 - Local Counterfactual Decision Analysis",
                "Classification: WARN / pending LASE-P5-PR10 merge-health gate.",
                "Final closeout branch: `codex/lase-phase5-closeout-report`.",
                "Implementation-complete main SHA before this closeout PR: "
                        + "`f1b9d33c2469b4fcea32dc12d243e9d4b2f41665`",
                "#428",
                "codex/lase-phase5-counterfactual-foundation",
                "048d0c829e35b65a6657f11f7d0e57f271b01991",
                "b401e28351613e17f496e2ed074eea76dbe1def5",
                "#429",
                "codex/lase-phase5-policy-weight-sensitivity",
                "df935446d2d04fb78e5f56a9e4de59fcc6115dc7",
                "be2d748a54b9bf9cdd27701be42f45419b744bfc",
                "#430",
                "codex/lase-phase5-counterfactual-candidate-outcomes",
                "3fc36f8c7f3ab8841420dda0472150729fe85d09",
                "7b11212a53c839ef473a8d3d7f47e926ce22869f",
                "#431",
                "codex/lase-phase5-factor-weight-deltas",
                "d58a603b5f7ac941d49447018ce5b98f97fe7bdf",
                "cd40d786841aa9a16797ad4d836def987eafa5cd",
                "#432",
                "codex/lase-phase5-counterfactual-explanations",
                "00b1ee4086b8ade269d22a41ee2c1f22106420fc",
                "6d4094f7d23adb7925e0ddfd4358221a6651d558",
                "#433",
                "codex/lase-phase5-counterfactual-fingerprints",
                "8f6f22ff34d5117d2abb1bed84ad28db815bbbab",
                "d90b80e2c07d1299bc49a5b37ed08e070d1bb582",
                "#434",
                "codex/lase-phase5-counterfactual-fixtures",
                "3973e7e19bade4d4b3c6cb4d326dd1c3bc259d33",
                "5f6a184486f0960e61cf5cc7e670b2c1c1a6efbb",
                "#435",
                "codex/lase-phase5-counterfactual-payload-exposure",
                "8127c4cd5e55b04e216b0ccfba5c6768b5f9c1c7",
                "3bcc0529ad085866f0aec2ac49635e6126fa34f5",
                "#436",
                "codex/lase-phase5-counterfactual-ui-panel",
                "d702f4a0e271fbfe53c8d400bc4ff3a13e395fe4",
                "f1b9d33c2469b4fcea32dc12d243e9d4b2f41665")) {
            assertTrue(closeout.contains(expected), "closeout should include " + expected);
        }
    }

    @Test
    void closeoutNamesImplementedCounterfactualArtifacts() throws IOException {
        String closeout = read(CLOSEOUT);

        for (String expected : List.of(
                "DecisionExplorerCounterfactualAnalysisV1",
                "DecisionExplorerCounterfactualAnalysisService",
                "DecisionExplorerCounterfactualPolicyWeightScenarioV1",
                "DecisionExplorerCounterfactualCandidateOutcomeV1",
                "DecisionExplorerCounterfactualFactorWeightDeltaV1",
                "DecisionExplorerCounterfactualExplanationBuilder",
                "DecisionExplorerCounterfactualFingerprintBuilder",
                "DecisionExplorerCounterfactualFixtureCatalog",
                "DecisionExplorerPayloadV1.counterfactualAnalysis",
                "/decision-explorer.html",
                "docs/API_CONTRACTS.md",
                "STABLE",
                "SENSITIVE",
                "CLOSE_CALL",
                "DEGRADED",
                "INSUFFICIENT_EVIDENCE",
                "UNKNOWN",
                "BASELINE_RETURNED_EVIDENCE",
                "SELECTED_SUPPORT_PLUS_10",
                "ALTERNATIVE_SUPPORT_PLUS_10",
                "STABILIZING",
                "DESTABILIZING",
                "NEUTRAL")) {
            assertTrue(closeout.contains(expected), "closeout should name implemented artifact " + expected);
        }

        for (Path path : List.of(
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualAnalysisV1.java"),
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualAnalysisService.java"),
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualPolicyWeightScenarioV1.java"),
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualCandidateOutcomeV1.java"),
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualFactorWeightDeltaV1.java"),
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualExplanationBuilder.java"),
                Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualFingerprintBuilder.java"),
                Path.of("src/main/resources/static/decision-explorer.html"),
                Path.of("docs/API_CONTRACTS.md"),
                Path.of("src/test/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualFixtureCatalogTest.java"))) {
            assertTrue(Files.exists(path), path + " should back the Phase 5 closeout");
        }
    }

    @Test
    void closeoutRecordsVerificationAndBoundaries() throws IOException {
        String normalized = read(CLOSEOUT).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        for (String expected : List.of(
                "decisionexplorercounterfactualanalysisservicetest",
                "decisionexplorercounterfactualcandidateoutcomeevaluatortest",
                "decisionexplorercounterfactualfactorweightdeltaevaluatortest",
                "decisionexplorercounterfactualexplanationbuildertest",
                "decisionexplorercounterfactualfingerprintbuildertest",
                "decisionexplorercounterfactualfixturecatalogtest",
                "decisionexplorerpayloadservicetest",
                "decisionexplorerpayloadv1test",
                "decisionexplorerapicontracthardeningtest",
                "routingopenapicontracttest",
                "decisionexplorerstaticpagetest",
                "decisionexplorerreviewernavigationtest",
                "agentlaseroutingintelligencephase5closeoutdocumentationtest",
                "mvn -q test",
                "mvn -q \"-dskiptests\" package",
                "mvn -b package",
                "git diff --check",
                ".\\scripts\\smoke\\enterprise-lab-workflow.ps1 -package",
                "main ci run `26671006920`",
                "main codeql run `26671006915`",
                "build/test/package/smoke",
                "dependency review")) {
            assertTrue(normalized.contains(expected), "closeout should record " + expected);
        }

        for (String expected : List.of(
                "additive",
                "local-only",
                "read-only",
                "simulation-only",
                "did not change production routing",
                "no-production-mutation boundaries",
                "external targets",
                "does not prove",
                "production readiness",
                "production certification",
                "live-cloud validation",
                "real-tenant validation",
                "runtime enforcement",
                "benchmark/load/stress behavior",
                "throughput or p95/p99 behavior",
                "replay execution",
                "replay storage or export",
                "evidence-packet generation",
                "autonomous production action",
                "traffic shifting",
                "broader automation")) {
            assertTrue(normalized.contains(expected), "closeout should preserve boundary " + expected);
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
            assertFalse(normalized.contains(forbidden), "closeout must not overclaim " + forbidden);
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
