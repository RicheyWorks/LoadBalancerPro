package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DecisionExplorerPayloadServiceTest {
    private final DecisionExplorerPayloadService service = new DecisionExplorerPayloadService();

    @Test
    void buildPayloadsOrdersResultsCandidatesAndFactorsDeterministically() {
        RoutingComparisonResponse first = comparison(
                result("WEIGHTED_LEAST_CONNECTIONS", "selected-b", "SUCCESS",
                        vector("WEIGHTED_LEAST_CONNECTIONS",
                                candidate("selected-b", true, contribution("queueDepth", -3.0)),
                                candidate("alternative-a", false, contribution("latency", 2.0))),
                        scores("alternative-a", 8.0, "selected-b", 4.0),
                        delta("selected-b", "alternative-a", 4.0),
                        readiness("readiness-z", "Replay readiness", "PARTIAL"),
                        sourceMap("source-z", "PARTIAL")),
                result("TAIL_LATENCY_POWER_OF_TWO", "edge-a", "SUCCESS",
                        vector("TAIL_LATENCY_POWER_OF_TWO",
                                candidate("edge-z", false, contribution("p95LatencyMillis", 12.0)),
                                candidate("edge-a", true, contribution("healthState", -8.0))),
                        scores("edge-z", 20.0, "edge-a", 10.0),
                        delta("edge-a", "edge-z", 10.0),
                        readiness("readiness-a", "Snapshot readiness", "AVAILABLE"),
                        sourceMap("source-a", "AVAILABLE")));

        RoutingComparisonResponse second = comparison(
                result("TAIL_LATENCY_POWER_OF_TWO", "edge-a", "SUCCESS",
                        vector("TAIL_LATENCY_POWER_OF_TWO",
                                candidate("edge-a", true, contribution("healthState", -8.0)),
                                candidate("edge-z", false, contribution("p95LatencyMillis", 12.0))),
                        scores("edge-a", 10.0, "edge-z", 20.0),
                        delta("edge-a", "edge-z", 10.0),
                        readiness("readiness-a", "Snapshot readiness", "AVAILABLE"),
                        sourceMap("source-a", "AVAILABLE")),
                result("WEIGHTED_LEAST_CONNECTIONS", "selected-b", "SUCCESS",
                        vector("WEIGHTED_LEAST_CONNECTIONS",
                                candidate("alternative-a", false, contribution("latency", 2.0)),
                                candidate("selected-b", true, contribution("queueDepth", -3.0))),
                        scores("selected-b", 4.0, "alternative-a", 8.0),
                        delta("selected-b", "alternative-a", 4.0),
                        readiness("readiness-z", "Replay readiness", "PARTIAL"),
                        sourceMap("source-z", "PARTIAL")));

        List<DecisionExplorerPayloadV1> firstPayloads = service.buildPayloads(first);
        List<DecisionExplorerPayloadV1> secondPayloads = service.buildPayloads(second);

        assertEquals(firstPayloads, secondPayloads);
        assertEquals(2, firstPayloads.size());
        assertEquals("routing-compare/tail-latency-power-of-two/edge-a", firstPayloads.get(0).decisionId());
        assertEquals(List.of("edge-a", "edge-z"),
                firstPayloads.get(0).candidateSet().stream().map(CandidateReadoutV1::candidateId).toList());
        assertEquals(List.of("1:edge-a:SELECTED:0.0", "2:edge-z:COMPARED_TO_SELECTED:10.0"),
                firstPayloads.get(0).candidateComparisons().stream()
                        .map(row -> row.displayOrder() + ":" + row.candidateId() + ":"
                                + row.comparisonStatus() + ":" + row.scoreDeltaFromSelected())
                        .toList());
        assertEquals("STRONG", firstPayloads.get(0).confidenceSummary().status());
        assertEquals("COMPLETE", firstPayloads.get(0).confidenceSummary().evidenceQuality());
        assertEquals("edge-a", firstPayloads.get(0).confidenceSummary().selectedCandidateId());
        assertEquals(List.of("CANDIDATE_COMPARISONS_AVAILABLE", "FACTOR_EVIDENCE_AVAILABLE",
                        "NO_STATUS_WARNINGS", "SELECTED_CANDIDATE_CONFIRMED"),
                firstPayloads.get(0).confidenceSummary().statusReasons());
        assertEquals(List.of("1:edge-a:STRONG:HEALTHY", "2:edge-z:STRONG:HEALTHY"),
                firstPayloads.get(0).confidenceSummary().candidateConfidenceDetails().stream()
                        .map(detail -> detail.displayOrder() + ":" + detail.candidateId() + ":"
                                + detail.confidenceStatus() + ":" + detail.healthEvidenceState())
                        .toList());
        assertEquals(List.of("1:edge-a:healthState:STRONG", "2:edge-z:p95LatencyMillis:STRONG"),
                firstPayloads.get(0).confidenceSummary().factorStatusDetails().stream()
                        .map(detail -> detail.displayOrder() + ":" + detail.candidateId() + ":"
                                + detail.factorName() + ":" + detail.factorStatus())
                        .toList());
        assertEquals("STRONG", firstPayloads.get(0).confidenceSummary().statusExplanation().status());
        assertEquals("STRONG",
                firstPayloads.get(0).confidenceSummary().statusExplanation().selectedCandidateConfidenceStatus());
        assertEquals("STRONG", firstPayloads.get(0).confidenceSummary().statusExplanation().factorStatusRollup());
        assertEquals("STRONG", firstPayloads.get(0).routingDiagnostics().overallStatus());
        assertEquals("COMPLETE", firstPayloads.get(0).routingDiagnostics().evidenceQuality());
        assertTrue(firstPayloads.get(0).routingDiagnostics().explanationText()
                .contains("selected candidate edge-a as STRONG"));
        assertEquals(List.of(
                        "CANDIDATE:candidate-comparisons:PRESENT",
                        "CANDIDATE:candidate-confidence:PRESENT",
                        "CANDIDATE:selected-candidate:PRESENT",
                        "CAUTION:status-unknowns:UNKNOWN",
                        "CAUTION:status-warnings:PARTIAL",
                        "DECISION:decision-status:PRESENT",
                        "FACTOR:factor-evidence:PRESENT",
                        "FACTOR:factor-status:PRESENT",
                        "SOURCE:source-references:PRESENT"),
                firstPayloads.get(0).routingDiagnostics().evidenceDiagnostics().stream()
                        .map(diagnostic -> diagnostic.category() + ":" + diagnostic.diagnosticId()
                                + ":" + diagnostic.status())
                        .toList());
        assertEquals("edge-a",
                firstPayloads.get(0).routingDiagnostics().selectedCandidateDiagnostic().candidateId());
        assertEquals(List.of("edge-z"),
                firstPayloads.get(0).routingDiagnostics().alternativeCandidateDiagnostics().stream()
                        .map(DecisionExplorerCandidateDiagnosticV1::candidateId)
                        .toList());
        assertEquals(List.of("edge-a:healthState:SUPPORTING", "edge-z:p95LatencyMillis:WARNING"),
                firstPayloads.get(0).routingDiagnostics().factorDiagnostics().stream()
                        .map(diagnostic -> diagnostic.candidateId() + ":" + diagnostic.factorName()
                                + ":" + diagnostic.contribution())
                        .toList());
        assertEquals("DecisionExplorerRouteTradeoffAnalysisV1",
                firstPayloads.get(0).routeTradeoffAnalysis().analysisObject());
        assertEquals("STRONG", firstPayloads.get(0).routeTradeoffAnalysis().overallStatus());
        assertEquals("SELECTED_ADVANTAGE", firstPayloads.get(0).routeTradeoffAnalysis().tradeoffCategory());
        assertEquals("edge-a", firstPayloads.get(0).routeTradeoffAnalysis().selectedCandidateId());
        assertEquals(2, firstPayloads.get(0).routeTradeoffAnalysis().candidateTradeoffCount());
        assertEquals(1, firstPayloads.get(0).routeTradeoffAnalysis().comparedAlternativeCount());
        assertEquals("edge-z", firstPayloads.get(0).routeTradeoffAnalysis().closestAlternativeCandidateId());
        assertEquals("TRADEOFF_READY",
                firstPayloads.get(0).routeTradeoffAnalysis().evidenceSufficiency().sufficiencyLevel());
        assertTrue(firstPayloads.get(0).routeTradeoffAnalysis().explanationText()
                .contains("selected candidate edge-a is STRONG with category SELECTED_ADVANTAGE"));
        assertTrue(firstPayloads.get(0).routeTradeoffAnalysis().explanationText()
                .contains("replay readiness PARTIAL with replay execution unavailable"));
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                firstPayloads.get(0).routeTradeoffAnalysis().fingerprintAlgorithm());
        assertTrue(firstPayloads.get(0).routeTradeoffAnalysis().diagnosticFingerprint()
                .startsWith("route-tradeoff|v1|"));
        assertTrue(firstPayloads.get(0).routeTradeoffAnalysis().fingerprintInputs()
                .contains("selectedCandidateId=edge-a"));
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                firstPayloads.get(0).routeTradeoffAnalysis().evidenceSufficiency().fingerprintAlgorithm());
        assertTrue(firstPayloads.get(0).routeTradeoffAnalysis().evidenceSufficiency()
                .diagnosticFingerprint().startsWith("evidence-sufficiency|v1|"));
        assertEquals("AVAILABLE",
                firstPayloads.get(0).routeTradeoffAnalysis()
                        .replayReadinessDiagnostic().fingerprintEvidenceStatus());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                firstPayloads.get(0).routeTradeoffAnalysis()
                        .replayReadinessDiagnostic().fingerprintAlgorithm());
        assertTrue(firstPayloads.get(0).routeTradeoffAnalysis()
                .replayReadinessDiagnostic().diagnosticFingerprint().startsWith("replay-readiness|v1|"));
        assertFalse(firstPayloads.get(0).routeTradeoffAnalysis()
                .replayReadinessDiagnostic().replayExecutionAvailable());
        assertFalse(firstPayloads.get(0).routeTradeoffAnalysis()
                .replayReadinessDiagnostic().missingEvidenceSignals()
                .contains("diagnostic fingerprint evidence has not been computed yet"));
        assertEquals("DecisionExplorerShadowDecisionQualityEvaluationV1",
                firstPayloads.get(0).shadowDecisionQualityEvaluation().evaluationObject());
        assertEquals("REVIEW_RECOMMENDED", firstPayloads.get(0).shadowDecisionQualityEvaluation().qualityLabel());
        assertEquals("MEDIUM", firstPayloads.get(0).shadowDecisionQualityEvaluation().qualityBand());
        assertEquals("edge-a", firstPayloads.get(0).shadowDecisionQualityEvaluation().selectedCandidateId());
        assertEquals("STRONG", firstPayloads.get(0).shadowDecisionQualityEvaluation().confidenceStatus());
        assertEquals("SELECTED_ADVANTAGE",
                firstPayloads.get(0).shadowDecisionQualityEvaluation().tradeoffCategory());
        assertEquals("TRADEOFF_READY",
                firstPayloads.get(0).shadowDecisionQualityEvaluation().evidenceSufficiencyLevel());
        assertEquals("PARTIAL", firstPayloads.get(0).shadowDecisionQualityEvaluation().replayReadinessStatus());
        assertEquals(List.of("SELECTED_BASELINE", "ACCEPTABLE_ALTERNATIVE"),
                firstPayloads.get(0).shadowDecisionQualityEvaluation().candidateOutcomeComparisons().stream()
                        .map(DecisionExplorerShadowCandidateOutcomeV1::outcomeLabel)
                        .toList());
        assertEquals("MEDIUM",
                firstPayloads.get(0).shadowDecisionQualityEvaluation()
                        .policySensitivityDiagnostic().sensitivityLevel());
        assertEquals("PARTIAL_INPUT",
                firstPayloads.get(0).shadowDecisionQualityEvaluation()
                        .scenarioInputQuality().inputQualityLabel());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                firstPayloads.get(0).shadowDecisionQualityEvaluation().fingerprintAlgorithm());
        assertTrue(firstPayloads.get(0).shadowDecisionQualityEvaluation().diagnosticFingerprint()
                .startsWith("shadow-decision-quality|v1|"));
        assertTrue(firstPayloads.get(0).shadowDecisionQualityEvaluation().reproducibilityKey()
                .startsWith("shadow-decision-quality:v1:REVIEW_RECOMMENDED:edge-a:SELECTED_ADVANTAGE"));
        assertTrue(firstPayloads.get(0).shadowDecisionQualityEvaluation().fingerprintInputs()
                .contains("selectedCandidateId=edge-a"));
        assertTrue(firstPayloads.get(0).shadowDecisionQualityEvaluation().fingerprintInputs()
                .contains("candidateOutcomeCount=2"));
        assertTrue(firstPayloads.get(0).shadowDecisionQualityEvaluation().explanationText()
                .contains("Shadow decision-quality explanation is REVIEW_RECOMMENDED"));
        assertTrue(firstPayloads.get(0).shadowDecisionQualityEvaluation().explanationText()
                .contains(firstPayloads.get(0).shadowDecisionQualityEvaluation().reproducibilityKey()));
        assertTrue(firstPayloads.get(0).shadowDecisionQualityEvaluation().qualityReasons()
                .contains("SHADOW_DECISION_QUALITY_REVIEW_RECOMMENDED"));
        assertTrue(firstPayloads.get(0).shadowDecisionQualityEvaluation().evidenceBasisSummary()
                .contains("no production routing decision is changed"));
        assertEquals(List.of("edge-a:healthState", "edge-z:p95LatencyMillis"),
                firstPayloads.get(0).factorContributions().stream()
                        .map(factor -> factor.candidateId() + ":" + factor.factorName())
                        .toList());
        assertEquals(List.of("edge-a:healthState:SUPPORTS_SELECTION:AVAILABLE",
                        "edge-z:p95LatencyMillis:WEAKENS_SELECTION:AVAILABLE"),
                firstPayloads.get(0).factorDrilldowns().stream()
                        .map(factor -> factor.candidateId() + ":" + factor.factorName() + ":"
                                + factor.influenceCategory() + ":" + factor.evidenceStatus())
                        .toList());
        assertTrue(firstPayloads.get(0).policyGateReadouts().stream()
                .anyMatch(gate -> gate.gateId().equals("readiness-readiness-a")
                        && gate.outcome().equals("PASS")));
    }

    @Test
    void partialResultBuildsSafeUnknownReadoutsWithoutInventingEvidence() {
        RoutingComparisonResultResponse partial = result(
                "TAIL_LATENCY_POWER_OF_TWO",
                null,
                "FAILED",
                null,
                null,
                null,
                null,
                null);

        DecisionExplorerPayloadV1 payload = service.buildPayload(partial);

        assertTrue(payload.readOnly());
        assertTrue(payload.simulationOnly());
        assertEquals("routing-compare/tail-latency-power-of-two/unknown", payload.decisionId());
        assertEquals("UNKNOWN", payload.decisionReadout().selectedCandidateId());
        assertEquals("UNKNOWN", payload.selectedCandidate().candidateId());
        assertTrue(payload.candidateSet().isEmpty());
        assertTrue(payload.candidateComparisons().isEmpty());
        assertTrue(payload.factorContributions().isEmpty());
        assertTrue(payload.factorDrilldowns().isEmpty());
        assertEquals("DEGRADED", payload.confidenceSummary().status());
        assertEquals(List.of("DECISION_STATUS_FAILED"), payload.confidenceSummary().statusReasons());
        assertTrue(payload.confidenceSummary().candidateConfidenceDetails().isEmpty());
        assertTrue(payload.confidenceSummary().factorStatusDetails().isEmpty());
        assertEquals("DEGRADED", payload.confidenceSummary().statusExplanation().status());
        assertTrue(payload.confidenceSummary().statusExplanation().summaryText().contains("DECISION_STATUS_FAILED"));
        assertEquals("DEGRADED", payload.routingDiagnostics().overallStatus());
        assertEquals("DEGRADED", payload.routingDiagnostics().evidenceQuality());
        assertTrue(payload.routingDiagnostics().explanationText()
                .contains("selected candidate UNKNOWN as DEGRADED"));
        assertTrue(payload.routingDiagnostics().diagnosticReasons().contains("DECISION_STATUS_FAILED"));
        assertEquals("UNKNOWN", payload.routingDiagnostics().selectedCandidateDiagnostic().candidateId());
        assertEquals("DEGRADED", payload.routeTradeoffAnalysis().overallStatus());
        assertEquals("DEGRADED", payload.routeTradeoffAnalysis().tradeoffCategory());
        assertTrue(payload.routeTradeoffAnalysis().explanationText()
                .contains("selected candidate UNKNOWN is DEGRADED with category DEGRADED"));
        assertTrue(payload.routeTradeoffAnalysis().candidateTradeoffs().isEmpty());
        assertEquals("DEGRADED", payload.routeTradeoffAnalysis().evidenceSufficiency().sufficiencyLevel());
        assertFalse(payload.routeTradeoffAnalysis().replayReadinessDiagnostic().replayExecutionAvailable());
        assertEquals("DEGRADED_DECISION", payload.shadowDecisionQualityEvaluation().qualityLabel());
        assertEquals("LOW", payload.shadowDecisionQualityEvaluation().qualityBand());
        assertEquals("DEGRADED", payload.shadowDecisionQualityEvaluation().confidenceStatus());
        assertEquals("DEGRADED", payload.shadowDecisionQualityEvaluation().evidenceSufficiencyLevel());
        assertEquals("MISSING_CANDIDATE_INPUT",
                payload.shadowDecisionQualityEvaluation().scenarioInputQuality().inputQualityLabel());
        assertTrue(payload.shadowDecisionQualityEvaluation().qualityReasons()
                .contains("SHADOW_DECISION_QUALITY_DEGRADED_DECISION"));
        assertTrue(payload.decisionDiffReadouts().isEmpty());
        assertEquals("future-evidence-packet", payload.evidencePacketReadouts().get(0).referenceId());
        assertTrue(payload.warnings().contains("Selected candidate was not returned."));
        assertTrue(payload.unknowns().contains("hidden routing internals"));
        assertTrue(payload.notProvenBoundaries().contains("no replay/export proof"));
        assertFalse(payload.toString().toLowerCase(Locale.ROOT).contains("production readiness proven"));
    }

    @Test
    void nullComparisonBuildsOneUnknownPayloadWithBoundaryLanguage() {
        List<DecisionExplorerPayloadV1> payloads = service.buildPayloads(null);

        assertEquals(1, payloads.size());
        DecisionExplorerPayloadV1 payload = payloads.get(0);
        assertEquals("routing-compare/unknown/unknown", payload.decisionId());
        assertEquals("UNKNOWN", payload.decisionReadout().status());
        assertEquals("UNKNOWN", payload.confidenceSummary().status());
        assertEquals(List.of("NO_ROUTING_EVIDENCE_RETURNED"), payload.confidenceSummary().statusReasons());
        assertTrue(payload.confidenceSummary().factorStatusDetails().isEmpty());
        assertEquals("UNKNOWN", payload.confidenceSummary().statusExplanation().status());
        assertTrue(payload.confidenceSummary().statusExplanation().summaryText()
                .contains("NO_ROUTING_EVIDENCE_RETURNED"));
        assertEquals("UNKNOWN", payload.routingDiagnostics().overallStatus());
        assertTrue(payload.routingDiagnostics().explanationText()
                .contains("NO_CONFIDENCE_SUMMARY_RETURNED"));
        assertEquals(List.of("NO_CONFIDENCE_SUMMARY_RETURNED"), payload.routingDiagnostics().diagnosticReasons());
        assertTrue(payload.routingDiagnostics().unknowns().contains("confidence summary evidence was unavailable"));
        assertEquals("UNKNOWN", payload.routeTradeoffAnalysis().overallStatus());
        assertEquals("UNKNOWN", payload.routeTradeoffAnalysis().tradeoffCategory());
        assertTrue(payload.routeTradeoffAnalysis().explanationText()
                .contains("Route tradeoff explanation is UNKNOWN"));
        assertEquals("INSUFFICIENT", payload.routeTradeoffAnalysis().evidenceSufficiency().sufficiencyLevel());
        assertEquals("UNKNOWN",
                payload.routeTradeoffAnalysis().replayReadinessDiagnostic().readinessStatus());
        assertEquals("UNKNOWN", payload.shadowDecisionQualityEvaluation().qualityLabel());
        assertEquals("UNKNOWN", payload.shadowDecisionQualityEvaluation().qualityBand());
        assertTrue(payload.shadowDecisionQualityEvaluation().candidateOutcomeComparisons().isEmpty());
        assertTrue(payload.shadowDecisionQualityEvaluation().unknowns()
                .contains("shadow decision-quality input evidence was unavailable"));
        assertTrue(payload.warnings().get(0).contains("did not include result evidence"));
        assertTrue(payload.policyGateReadouts().stream()
                .anyMatch(gate -> gate.gateId().equals("boundary-read-only") && gate.outcome().equals("PASS")));
        assertTrue(payload.evidencePacketReadouts().get(0).unavailableReasons().get(0)
                .contains("does not generate, persist, export, or replay evidence packets"));
    }

    @Test
    void contributionAndDiffReadoutsPreserveReturnedEvidenceOnly() {
        DecisionExplorerPayloadV1 payload = service.buildPayload(result(
                "WEIGHTED_LEAST_CONNECTIONS",
                "selected-b",
                "SUCCESS",
                vector("WEIGHTED_LEAST_CONNECTIONS",
                        candidate("selected-b", true, contribution("queueDepth", -3.0)),
                        candidate("alternative-a", false, contribution("latency", Double.NaN))),
                scores("selected-b", 4.0, "alternative-a", Double.NaN),
                delta("selected-b", "alternative-a", Double.NaN),
                readiness("readiness-z", "Replay readiness", "PARTIAL"),
                sourceMap("source-z", "PARTIAL")));

        CandidateReadoutV1 alternative = payload.candidateSet().get(1);
        assertEquals("alternative-a", alternative.candidateId());
        assertNull(alternative.finalScore());
        DecisionExplorerCandidateComparisonRowV1 alternativeComparison = payload.candidateComparisons().get(1);
        assertEquals("alternative-a", alternativeComparison.candidateId());
        assertEquals("PARTIAL_EVIDENCE", alternativeComparison.comparisonStatus());
        assertNull(alternativeComparison.finalScore());
        assertNull(alternativeComparison.scoreDeltaFromSelected());
        assertTrue(alternativeComparison.warnings().contains("candidate final score was not returned"));
        assertTrue(alternativeComparison.unknowns().contains("score delta from selected candidate"));
        assertEquals("latency", payload.factorContributions().get(0).factorName());
        assertNull(payload.factorContributions().get(0).contributionValue());
        DecisionFactorDrilldownV1 partialDrilldown = payload.factorDrilldowns().get(0);
        assertEquals("alternative-a", partialDrilldown.candidateId());
        assertEquals("latency", partialDrilldown.factorName());
        assertEquals("SUPPORTS_SELECTION", partialDrilldown.influenceCategory());
        assertEquals("PARTIAL", partialDrilldown.evidenceStatus());
        assertTrue(partialDrilldown.warnings().contains("finite contribution value was not returned"));
        assertTrue(partialDrilldown.unknowns().contains("numeric contribution value"));
        assertTrue(partialDrilldown.sourceReferenceIds()
                .contains("factor-contribution:alternative-a:latency"));
        assertEquals("PARTIAL", payload.confidenceSummary().status());
        assertTrue(payload.confidenceSummary().statusReasons().contains("PARTIAL_CANDIDATE_COMPARISON_EVIDENCE"));
        assertTrue(payload.confidenceSummary().statusReasons().contains("PARTIAL_FACTOR_EVIDENCE"));
        DecisionExplorerCandidateConfidenceV1 alternativeConfidence =
                payload.confidenceSummary().candidateConfidenceDetails().get(1);
        assertEquals("alternative-a", alternativeConfidence.candidateId());
        assertEquals("PARTIAL", alternativeConfidence.confidenceStatus());
        assertTrue(alternativeConfidence.confidenceReasons().contains("FINAL_SCORE_UNKNOWN"));
        DecisionExplorerFactorStatusV1 alternativeFactorStatus =
                payload.confidenceSummary().factorStatusDetails().get(0);
        assertEquals("alternative-a", alternativeFactorStatus.candidateId());
        assertEquals("latency", alternativeFactorStatus.factorName());
        assertEquals("PARTIAL", alternativeFactorStatus.factorStatus());
        assertTrue(alternativeFactorStatus.statusReasons().contains("FACTOR_EVIDENCE_PARTIAL"));
        assertTrue(payload.confidenceSummary().statusReasons().contains("FACTOR_STATUS_PARTIAL"));
        assertEquals("PARTIAL", payload.confidenceSummary().statusExplanation().status());
        assertEquals("PARTIAL", payload.confidenceSummary().statusExplanation().factorStatusRollup());
        assertEquals("PARTIAL", payload.routingDiagnostics().overallStatus());
        assertTrue(payload.routingDiagnostics().explanationText()
                .contains("selected candidate selected-b as PARTIAL"));
        assertTrue(payload.routingDiagnostics().partialEvidenceReasons().stream()
                .anyMatch(reason -> reason.contains("alternative-a:latency")));
        assertTrue(payload.routingDiagnostics().unknowns().contains("score delta from selected candidate"));
        assertEquals("PARTIAL", payload.routeTradeoffAnalysis().overallStatus());
        assertEquals("PARTIAL_TRADEOFF", payload.routeTradeoffAnalysis().tradeoffCategory());
        assertTrue(payload.routeTradeoffAnalysis().explanationText()
                .contains("selected candidate selected-b is PARTIAL with category PARTIAL_TRADEOFF"));
        assertEquals("BASIC_DIAGNOSTICS_ONLY",
                payload.routeTradeoffAnalysis().evidenceSufficiency().sufficiencyLevel());
        assertEquals("PARTIAL",
                payload.routeTradeoffAnalysis().replayReadinessDiagnostic().scoreEvidenceStatus());
        assertEquals("REVIEW_RECOMMENDED", payload.shadowDecisionQualityEvaluation().qualityLabel());
        assertEquals("PARTIAL_TRADEOFF", payload.shadowDecisionQualityEvaluation().tradeoffCategory());
        assertEquals("BASIC_DIAGNOSTICS_ONLY",
                payload.shadowDecisionQualityEvaluation().evidenceSufficiencyLevel());
        assertEquals("UNKNOWN_ALTERNATIVE",
                payload.shadowDecisionQualityEvaluation().candidateOutcomeComparisons().get(1).outcomeLabel());
        assertEquals("PARTIAL_INPUT",
                payload.shadowDecisionQualityEvaluation().scenarioInputQuality().inputQualityLabel());
        assertNull(payload.decisionDiffReadouts().get(0).finalScoreGap());
        assertEquals(List.of("latency", "queueDepth"), payload.decisionDiffReadouts().get(0).comparedFactorNames());
        assertTrue(payload.evidencePacketReadouts().get(0).unavailableReasons()
                .contains("source map entry status is PARTIAL"));
    }

    @Test
    void sourceDoesNotUseRoutingCalculatorEnvironmentOrExternalSideEffects() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                + "DecisionExplorerPayloadService.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/com/richmond423/loadbalancerpro/api/"
                        + "DecisionExplorerConfidenceSummaryService.java"), StandardCharsets.UTF_8);
        String normalized = source.toLowerCase(Locale.ROOT);

        assertFalse(source.contains("ServerScoreCalculator"));
        assertFalse(source.contains("ServerStateVector"));
        assertFalse(normalized.contains("instant.now"));
        assertFalse(normalized.contains("system.getenv"));
        assertFalse(normalized.contains("system.getproperty"));
        assertFalse(normalized.contains("randomuuid"));
        assertFalse(normalized.contains("httpclient"));
        assertFalse(normalized.contains("urlconnection"));
        assertFalse(normalized.contains("socket"));
        assertFalse(normalized.contains("files.write"));
    }

    @Test
    void boundaryLanguageDoesNotOverclaimDecisionExplorerEvidence() {
        String normalized = service.buildPayload(result(
                        "TAIL_LATENCY_POWER_OF_TWO",
                        "edge-a",
                        "SUCCESS",
                        vector("TAIL_LATENCY_POWER_OF_TWO",
                                candidate("edge-a", true, contribution("healthState", -8.0))),
                        scores("edge-a", 10.0),
                        delta("edge-a", null, null),
                        null,
                        null))
                .toString()
                .toLowerCase(Locale.ROOT);

        for (String expected : List.of(
                "read-only",
                "simulation-only",
                "does not change routing behavior",
                "no production readiness",
                "no live-cloud validation",
                "no real-tenant validation",
                "no benchmark/load/stress proof",
                "no throughput/p95/p99 proof",
                "no replay/export proof")) {
            assertTrue(normalized.contains(expected), "payload should preserve boundary " + expected);
        }

        for (String forbidden : List.of(
                "production readiness proven",
                "certification complete",
                "live-cloud validation complete",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "autonomous production action enabled")) {
            assertFalse(normalized.contains(forbidden), "payload must not overclaim " + forbidden);
        }
    }

    @Test
    void notProvenBoundariesDoNotDenyCurrentEndpointOrStaticPage() {
        DecisionExplorerPayloadV1 payload = service.buildPayload(result(
                "TAIL_LATENCY_POWER_OF_TWO",
                "edge-a",
                "SUCCESS",
                vector("TAIL_LATENCY_POWER_OF_TWO",
                        candidate("edge-a", true, contribution("healthState", -8.0))),
                scores("edge-a", 10.0),
                delta("edge-a", null, null),
                null,
                null));

        String normalized = String.join(" | ", payload.notProvenBoundaries()).toLowerCase(Locale.ROOT);

        assertTrue(payload.notProvenBoundaries().contains("no storage proof"));
        assertTrue(payload.notProvenBoundaries().contains("no evidence-packet generation"));
        assertTrue(payload.notProvenBoundaries().contains("no autonomous production action"));
        assertTrue(payload.agentStructuredOutput().notProvenBoundaries().contains("no storage proof"));
        assertFalse(normalized.contains("no decision explorer endpoint"));
        assertFalse(normalized.contains("no decision explorer ui"));
    }

    private static RoutingComparisonResponse comparison(RoutingComparisonResultResponse... results) {
        return new RoutingComparisonResponse(
                List.of("TAIL_LATENCY_POWER_OF_TWO", "WEIGHTED_LEAST_CONNECTIONS"),
                2,
                java.time.Instant.parse("2026-05-27T00:00:00Z"),
                null,
                null,
                List.of(results));
    }

    private static RoutingComparisonResultResponse result(
            String strategyId,
            String chosenServerId,
            String status,
            RoutingDecisionVectorResponse decisionVector,
            Map<String, Double> scores,
            RoutingDecisionDeltaAnalysisResponse delta,
            RoutingDecisionReplayReadinessChecklistResponse readiness,
            RoutingDecisionReplayEvidenceSourceMapResponse sourceMap) {
        return new RoutingComparisonResultResponse(
                strategyId,
                status,
                chosenServerId,
                "test reason",
                candidateServerIds(decisionVector, scores),
                scores,
                decisionVector,
                null,
                delta,
                null,
                null,
                null,
                readiness,
                sourceMap,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static List<String> candidateServerIds(
            RoutingDecisionVectorResponse decisionVector,
            Map<String, Double> scores) {
        if (decisionVector != null) {
            return decisionVector.candidateSummaries().stream()
                    .map(CandidateDecisionVectorResponse::candidateId)
                    .toList();
        }
        return scores == null ? List.of() : List.copyOf(scores.keySet());
    }

    private static RoutingDecisionVectorResponse vector(
            String strategyId,
            CandidateDecisionVectorResponse... candidates) {
        List<CandidateDecisionVectorResponse> candidateList = List.of(candidates);
        CandidateDecisionVectorResponse selected = candidateList.stream()
                .filter(CandidateDecisionVectorResponse::selected)
                .findFirst()
                .orElse(null);
        List<CandidateDecisionVectorResponse> nonSelected = candidateList.stream()
                .filter(candidate -> !candidate.selected())
                .toList();
        return new RoutingDecisionVectorResponse(
                true,
                "/api/routing/compare",
                "not exposed",
                strategyId,
                selected == null ? null : selected.candidateId(),
                candidateList.size(),
                candidateList,
                selected,
                nonSelected,
                List.of("healthState=true", "queueDepth=visible"),
                List.of("hidden routing internals not exposed"),
                "current calculator components only",
                List.of("selected-vs-alternative note"),
                "controlled lab evidence only",
                "no production certification",
                "exposed for current calculator components",
                "future/not implemented",
                "future/not implemented",
                "future/not implemented");
    }

    private static CandidateDecisionVectorResponse candidate(
            String candidateId,
            boolean selected,
            ScoreFactorContributionResponse... contributions) {
        return new CandidateDecisionVectorResponse(
                candidateId,
                selected,
                List.of("healthState=true", "queueDepth=visible"),
                List.of("hidden routing internals not exposed"),
                List.of(contributions),
                "candidate explanation",
                "current calculator components only",
                "controlled lab evidence only",
                "no production certification");
    }

    private static ScoreFactorContributionResponse contribution(String factorName, Double contributionValue) {
        return new ScoreFactorContributionResponse(
                factorName,
                "raw value",
                "weight",
                contributionValue == null || contributionValue > 0.0 ? "WEAKENS_SELECTION" : "SUPPORTS_SELECTION",
                "contribution",
                contributionValue,
                "EXACT_FROM_RETURNED_EVIDENCE",
                "factor explanation",
                "factor readout is not benchmark/load/stress evidence");
    }

    private static RoutingDecisionDeltaAnalysisResponse delta(
            String selectedCandidateId,
            String alternativeCandidateId,
            Double finalScoreGap) {
        return new RoutingDecisionDeltaAnalysisResponse(
                true,
                "delta source",
                "AVAILABLE",
                new CandidateDecisionDeltaResponse(
                        selectedCandidateId,
                        alternativeCandidateId,
                        4.0,
                        finalScoreGap == null ? null : 4.0 + finalScoreGap,
                        finalScoreGap,
                        finalScoreGap == null || !Double.isFinite(finalScoreGap) ? null : Math.abs(finalScoreGap),
                        1,
                        1,
                        2,
                        List.of("queueDepth", "latency"),
                        List.of(),
                        "selected-vs-alternative comparison"),
                List.of(),
                null,
                "delta explanation",
                "delta boundary",
                "no production certification");
    }

    private static RoutingDecisionReplayReadinessChecklistResponse readiness(
            String itemId,
            String label,
            String status) {
        return new RoutingDecisionReplayReadinessChecklistResponse(
                true,
                "checklist/v1",
                "readiness source",
                status,
                "TAIL_LATENCY_POWER_OF_TWO",
                "edge-a",
                2,
                null,
                null,
                null,
                "AVAILABLE",
                "AVAILABLE",
                "AVAILABLE",
                "AVAILABLE",
                "AVAILABLE",
                "AVAILABLE",
                1,
                0,
                0,
                List.of(new DecisionReplayReadinessChecklistItemResponse(
                        itemId,
                        label,
                        status,
                        "decisionReplayReadinessChecklist.checklistItems[]",
                        "readiness explanation",
                        null)),
                "readiness summary",
                "readiness boundary",
                "no production certification");
    }

    private static RoutingDecisionReplayEvidenceSourceMapResponse sourceMap(String sourceId, String status) {
        return new RoutingDecisionReplayEvidenceSourceMapResponse(
                true,
                "source-map/v1",
                "source map",
                status,
                "TAIL_LATENCY_POWER_OF_TWO",
                "edge-a",
                2,
                null,
                null,
                null,
                "AVAILABLE",
                "AVAILABLE",
                "AVAILABLE",
                "AVAILABLE",
                "AVAILABLE",
                "AVAILABLE",
                "AVAILABLE",
                List.of(new DecisionReplayEvidenceSourceMapEntryResponse(
                        sourceId,
                        "Source " + sourceId,
                        status,
                        "decisionVector",
                        List.of("decisionExplorerPayload"),
                        null,
                        "source map summary",
                        "source map boundary")),
                "source map explanation",
                "source map boundary",
                "no production certification");
    }

    private static Map<String, Double> scores(Object... alternatingKeysAndValues) {
        Map<String, Double> values = new LinkedHashMap<>();
        for (int index = 0; index < alternatingKeysAndValues.length; index += 2) {
            values.put((String) alternatingKeysAndValues[index], (Double) alternatingKeysAndValues[index + 1]);
        }
        return values;
    }
}
