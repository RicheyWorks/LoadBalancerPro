package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class DecisionExplorerCounterfactualAnalysisServiceTest {
    @Test
    void strongSelectedAdvantageClassifiesAsStableWithoutPolicyMutation() {
        DecisionExplorerCounterfactualAnalysisV1 analysis =
                DecisionExplorerCounterfactualFixtureCatalog.stableSelectedAdvantage().analysis();

        assertTrue(analysis.readOnly());
        assertTrue(analysis.simulationOnly());
        assertTrue(analysis.localOnly());
        assertEquals("DecisionExplorerCounterfactualAnalysisV1", analysis.analysisObject());
        assertEquals("v1", analysis.contractVersion());
        assertEquals("edge-a", analysis.selectedCandidateId());
        assertEquals("STABLE", analysis.counterfactualLabel());
        assertEquals("LOW", analysis.sensitivityBand());
        assertEquals("STRONG", analysis.confidenceStatus());
        assertEquals("ACCEPTABLE", analysis.decisionQualityLabel());
        assertEquals("SELECTED_ADVANTAGE", analysis.tradeoffCategory());
        assertEquals("REPLAY_STYLE_READY", analysis.evidenceSufficiencyLevel());
        assertEquals("READY", analysis.replayReadinessStatus());
        assertEquals("RETURNED_EVIDENCE_WEIGHTS", analysis.baselinePolicyProfile());
        assertEquals(3, analysis.policyWeightScenarioCount());
        assertEquals(2, analysis.candidateOutcomeCount());
        assertEquals(1, analysis.factorDeltaCount());
        assertEquals(1, analysis.factorWeightDeltaCount());
        assertEquals(List.of(
                        "BASELINE_RETURNED_EVIDENCE",
                        "SELECTED_SUPPORT_PLUS_10",
                        "ALTERNATIVE_SUPPORT_PLUS_10"),
                analysis.policyWeightScenarios().stream()
                        .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::scenarioId)
                        .toList());
        assertEquals(List.of("STABLE", "STABLE", "STABLE"),
                analysis.policyWeightScenarios().stream()
                        .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::sensitivityLabel)
                        .toList());
        assertEquals("LOCAL_ALTERNATIVE_SUPPORT_PLUS_10",
                analysis.policyWeightScenarios().get(2).policyWeightProfile());
        assertFalse(analysis.policyWeightScenarios().get(2).alternativeBecomesCloseOrPreferable());
        assertTrue(analysis.policyWeightScenarios().get(0).summaryText()
                .contains("does not change production scoring or routing"));
        assertEquals(2, analysis.counterfactualCandidateOutcomeCount());
        assertEquals(List.of("SELECTED_STABLE", "ALTERNATIVE_TRAILING"),
                analysis.counterfactualCandidateOutcomes().stream()
                        .map(DecisionExplorerCounterfactualCandidateOutcomeV1::counterfactualOutcomeLabel)
                        .toList());
        assertEquals(List.of("STABILIZING"), analysis.factorWeightDeltas().stream()
                .map(DecisionExplorerCounterfactualFactorWeightDeltaV1::factorWeightDeltaClassification)
                .toList());
        assertTrue(analysis.factorWeightDeltas().get(0).selectedSupportStabilizesDecision());
        assertTrue(analysis.stableSignals().contains("confidence status is STRONG"));
        assertTrue(analysis.stableSignals()
                .contains("selected candidate has returned-evidence tradeoff advantage"));
        assertTrue(analysis.summaryText().contains("Policy scenarios show baseline BASELINE_RETURNED_EVIDENCE/STABLE"));
        assertTrue(analysis.summaryText().contains("Candidate outcomes show selected edge-a as SELECTED_STABLE"));
        assertTrue(analysis.summaryText().contains("Factor-weight deltas show STABILIZING=1"));
        assertTrue(analysis.summaryText().contains("no production routing, scoring, proxying"));
        assertTrue(analysis.reasonCodes().contains("COUNTERFACTUAL_ANALYSIS_STABLE"));
        assertTrue(analysis.diagnosticFingerprint().startsWith("counterfactual-analysis|v1|"));
        assertEquals("counterfactual:v1:STABLE:edge-a:SELECTED_ADVANTAGE:quality=ACCEPTABLE:"
                + "sufficiency=REPLAY_STYLE_READY:replay=READY:scenarios=3:outcomes=2:factorWeightDeltas=1",
                analysis.reproducibilityKey());
    }

    @Test
    void closeAlternativeClassifiesAsCloseCallFromComputedTradeoff() {
        DecisionExplorerCounterfactualAnalysisV1 analysis =
                DecisionExplorerCounterfactualFixtureCatalog.closeAlternative().analysis();

        assertEquals("CLOSE_CALL", analysis.counterfactualLabel());
        assertEquals("MEDIUM", analysis.sensitivityBand());
        assertEquals("CLOSE_ALTERNATIVE", analysis.tradeoffCategory());
        assertTrue(analysis.sensitivitySignals()
                .contains("route tradeoff category is CLOSE_ALTERNATIVE"));
        assertTrue(analysis.sensitivitySignals()
                .contains("closest alternative score delta is within local close-call band"));
        assertEquals(3, analysis.policyWeightScenarioCount());
        assertTrue(analysis.policyWeightScenarios().get(2).alternativeBecomesCloseOrPreferable());
        assertEquals("CLOSE_CALL", analysis.policyWeightScenarios().get(2).sensitivityLabel());
        assertEquals(List.of("SELECTED_SENSITIVE", "ALTERNATIVE_CLOSE_CALL"),
                analysis.counterfactualCandidateOutcomes().stream()
                        .map(DecisionExplorerCounterfactualCandidateOutcomeV1::counterfactualOutcomeLabel)
                        .toList());
        assertEquals(List.of("NEUTRAL"), analysis.factorWeightDeltas().stream()
                .map(DecisionExplorerCounterfactualFactorWeightDeltaV1::factorWeightDeltaClassification)
                .toList());
        assertTrue(analysis.reasonCodes().contains("COUNTERFACTUAL_ANALYSIS_CLOSE_CALL"));
    }

    @Test
    void partialTradeoffClassifiesAsSensitiveWithSafeLimitations() {
        DecisionExplorerCounterfactualAnalysisV1 analysis =
                DecisionExplorerCounterfactualFixtureCatalog.sensitivePartialEvidence().analysis();

        assertEquals("SENSITIVE", analysis.counterfactualLabel());
        assertEquals("MEDIUM", analysis.sensitivityBand());
        assertEquals("PARTIAL", analysis.confidenceStatus());
        assertEquals("PARTIAL_TRADEOFF", analysis.tradeoffCategory());
        assertTrue(analysis.sensitivitySignals().contains("confidence status is PARTIAL"));
        assertEquals(3, analysis.policyWeightScenarioCount());
        assertEquals(List.of("SENSITIVE", "SENSITIVE", "SENSITIVE"),
                analysis.policyWeightScenarios().stream()
                        .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::sensitivityLabel)
                        .toList());
        assertEquals(List.of("SELECTED_SENSITIVE", "ALTERNATIVE_UNKNOWN"),
                analysis.counterfactualCandidateOutcomes().stream()
                        .map(DecisionExplorerCounterfactualCandidateOutcomeV1::counterfactualOutcomeLabel)
                        .toList());
        assertEquals(List.of("UNKNOWN"), analysis.factorWeightDeltas().stream()
                .map(DecisionExplorerCounterfactualFactorWeightDeltaV1::factorWeightDeltaClassification)
                .toList());
        assertTrue(analysis.limitationSignals().contains("score-comparable alternative evidence was not returned"));
        assertTrue(analysis.unknowns().contains("score delta from selected candidate"));
    }

    @Test
    void degradedEvidenceClassifiesAsDegradedBeforeSensitivity() {
        DecisionExplorerCounterfactualAnalysisV1 analysis =
                DecisionExplorerCounterfactualFixtureCatalog.degradedSelected().analysis();

        assertEquals("DEGRADED", analysis.counterfactualLabel());
        assertEquals("HIGH", analysis.sensitivityBand());
        assertEquals("DEGRADED", analysis.confidenceStatus());
        assertEquals("DEGRADED_DECISION", analysis.decisionQualityLabel());
        assertEquals("DEGRADED", analysis.tradeoffCategory());
        assertTrue(analysis.limitationSignals().contains("confidence status is DEGRADED"));
        assertEquals(3, analysis.policyWeightScenarioCount());
        assertEquals(List.of("DEGRADED", "DEGRADED", "DEGRADED"),
                analysis.policyWeightScenarios().stream()
                        .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::sensitivityLabel)
                        .toList());
        assertEquals(List.of("SELECTED_DEGRADED"),
                analysis.counterfactualCandidateOutcomes().stream()
                        .map(DecisionExplorerCounterfactualCandidateOutcomeV1::counterfactualOutcomeLabel)
                        .toList());
        assertEquals(0, analysis.factorWeightDeltaCount());
        assertTrue(analysis.reasonCodes().contains("COUNTERFACTUAL_ANALYSIS_DEGRADED"));
    }

    @Test
    void insufficientShadowQualityClassifiesAsInsufficientEvidence() {
        DecisionExplorerCounterfactualAnalysisV1 analysis =
                DecisionExplorerCounterfactualFixtureCatalog.insufficientEvidence().analysis();

        assertEquals("INSUFFICIENT_EVIDENCE", analysis.counterfactualLabel());
        assertEquals("INSUFFICIENT", analysis.sensitivityBand());
        assertEquals("INSUFFICIENT_EVIDENCE", analysis.decisionQualityLabel());
        assertTrue(analysis.limitationSignals()
                .contains("shadow decision quality reports INSUFFICIENT_EVIDENCE"));
        assertEquals(1, analysis.policyWeightScenarioCount());
        assertEquals("INSUFFICIENT_EVIDENCE", analysis.policyWeightScenarios().get(0).sensitivityLabel());
        assertEquals(0, analysis.counterfactualCandidateOutcomeCount());
        assertEquals(0, analysis.factorWeightDeltaCount());
        assertTrue(analysis.reasonCodes().contains("COUNTERFACTUAL_ANALYSIS_INSUFFICIENT_EVIDENCE"));
    }

    @Test
    void nullInputsReturnUnknownWithoutInventingCounterfactualEvidence() {
        DecisionExplorerCounterfactualAnalysisV1 analysis =
                DecisionExplorerCounterfactualFixtureCatalog.unknownEmptyEvidence().analysis();

        assertEquals("UNKNOWN", analysis.counterfactualLabel());
        assertEquals("UNKNOWN", analysis.sensitivityBand());
        assertEquals("UNKNOWN", analysis.selectedCandidateId());
        assertEquals(0, analysis.policyWeightScenarioCount());
        assertEquals(0, analysis.counterfactualCandidateOutcomeCount());
        assertEquals(0, analysis.factorWeightDeltaCount());
        assertEquals(0, analysis.candidateOutcomeCount());
        assertEquals(0, analysis.factorDeltaCount());
        assertTrue(analysis.limitationSignals()
                .contains("computed Decision Explorer evidence was unavailable"));
        assertEquals("counterfactual:v1:UNKNOWN:UNKNOWN:UNKNOWN:quality=UNKNOWN:"
                + "sufficiency=INSUFFICIENT:replay=UNKNOWN:scenarios=0:outcomes=0:factorWeightDeltas=0",
                analysis.reproducibilityKey());
    }

    @Test
    void fingerprintsAreStableAndChangeWhenComputedEvidenceChanges() {
        DecisionExplorerCounterfactualAnalysisV1 first =
                DecisionExplorerCounterfactualFixtureCatalog.stableSelectedAdvantage().analysis();
        DecisionExplorerCounterfactualAnalysisV1 second =
                DecisionExplorerCounterfactualFixtureCatalog.stableSelectedAdvantage().analysis();
        DecisionExplorerCounterfactualAnalysisV1 close =
                DecisionExplorerCounterfactualFixtureCatalog.closeAlternative().analysis();

        assertEquals(first.diagnosticFingerprint(), second.diagnosticFingerprint());
        assertEquals(first.reproducibilityKey(), second.reproducibilityKey());
        assertEquals(first.fingerprintInputs(), second.fingerprintInputs());
        assertNotEquals(first.diagnosticFingerprint(), close.diagnosticFingerprint());
        assertNotEquals(first.reproducibilityKey(), close.reproducibilityKey());
    }

    @Test
    void counterfactualFoundationDoesNotUseProductionRoutingMutationOrExternalServices() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualAnalysisService.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualLabelEvaluator.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualPolicyWeightScenarioBuilder.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualPolicyWeightScenarioV1.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualCandidateOutcomeEvaluator.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualCandidateOutcomeV1.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualExplanationBuilder.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualFingerprintBuilder.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualFactorWeightDeltaEvaluator.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualFactorWeightDeltaV1.java"),
                        StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerCounterfactualAnalysisV1.java"), StandardCharsets.UTF_8);
        String normalized = source.toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "serverscorecalculator",
                "serverstatevector",
                "instant.now",
                "system.getenv",
                "system.getproperty",
                "randomuuid",
                "httpclient",
                "urlconnection",
                "socket",
                "files.write",
                "distributeload",
                "addserver",
                "removeserver",
                "proxyclient",
                "trafficshifter")) {
            assertFalse(normalized.contains(forbidden), "counterfactual source must not contain " + forbidden);
        }
    }

}
