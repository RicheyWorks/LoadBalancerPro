package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DecisionExplorerShadowDecisionQualityCompatibilityRegressionTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final DecisionExplorerConfidenceSummaryService summaryService =
            new DecisionExplorerConfidenceSummaryService();
    private final DecisionExplorerRoutingDiagnosticsService diagnosticsService =
            new DecisionExplorerRoutingDiagnosticsService();
    private final DecisionExplorerRouteTradeoffService tradeoffService =
            new DecisionExplorerRouteTradeoffService();
    private final DecisionExplorerShadowDecisionQualityService shadowQualityService =
            new DecisionExplorerShadowDecisionQualityService();

    @Test
    void shadowDecisionQualityJsonKeepsAdditiveFieldOrder() {
        JsonNode json = OBJECT_MAPPER.valueToTree(shadowQualitySnapshots().get(0).evaluation());

        assertEquals(List.of(
                "readOnly",
                "simulationOnly",
                "evaluationObject",
                "contractVersion",
                "qualityLabel",
                "qualityBand",
                "qualityScore",
                "selectedCandidateId",
                "confidenceStatus",
                "evidenceQuality",
                "tradeoffCategory",
                "evidenceSufficiencyLevel",
                "replayReadinessStatus",
                "candidateOutcomeCount",
                "candidateOutcomeComparisons",
                "policySensitivityDiagnostic",
                "scenarioInputQuality",
                "evidenceBasisCount",
                "selectedCandidateBasisCount",
                "evidenceBasisSummary",
                "selectedCandidateBasisSummary",
                "evidenceBasis",
                "selectedCandidateBasis",
                "qualityReasons",
                "warnings",
                "unknowns",
                "sourceReferenceIds",
                "explanationText",
                "fingerprintAlgorithm",
                "diagnosticFingerprint",
                "reproducibilityKey",
                "fingerprintInputs",
                "boundaryNote"), fieldNames(json));
        assertEquals("DecisionExplorerShadowDecisionQualityEvaluationV1",
                json.path("evaluationObject").asText());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                json.path("fingerprintAlgorithm").asText());
        assertTrue(json.path("diagnosticFingerprint").asText().startsWith("shadow-decision-quality|v1|"));
        assertTrue(json.path("reproducibilityKey").asText().startsWith("shadow-decision-quality:v1:"));
        assertTrue(json.path("explanationText").asText().contains("Shadow decision-quality explanation"));
        assertTrue(json.path("explanationText").asText().contains("no production routing decision is changed"));
        assertEquals("DecisionExplorerShadowPolicySensitivityDiagnosticV1",
                json.at("/policySensitivityDiagnostic/diagnosticObject").asText());
        assertEquals("DecisionExplorerShadowScenarioInputQualityV1",
                json.at("/scenarioInputQuality/evaluationObject").asText());
        assertTrue(json.path("candidateOutcomeComparisons").isArray());
        assertTrue(json.path("fingerprintInputs").isArray());
    }

    @Test
    void fixtureShadowQualitySerializesDeterministicallyAcrossBuilds() {
        List<String> firstFingerprints = compatibilityFingerprints();
        List<String> secondFingerprints = compatibilityFingerprints();

        assertEquals(firstFingerprints, secondFingerprints);
        assertEquals(List.of(
                "strong-confirmed-selection|REVIEW_RECOMMENDED|MEDIUM|75|edge-a|STRONG|SELECTED_ADVANTAGE",
                "partial-candidate-and-factor-evidence|REVIEW_RECOMMENDED|MEDIUM|75|edge-a|PARTIAL|"
                        + "SELECTED_ADVANTAGE",
                "unknown-no-routing-evidence|UNKNOWN|UNKNOWN|0|UNKNOWN|UNKNOWN|UNKNOWN",
                "degraded-selected-health-evidence|DEGRADED_DECISION|LOW|40|edge-a|DEGRADED|DEGRADED"),
                firstFingerprints.stream()
                        .map(fingerprint -> fingerprint.substring(0, fingerprint.indexOf("|sufficiency=")))
                        .toList());
        assertTrue(firstFingerprints.get(0).contains("sufficiency=TRADEOFF_READY|"));
        assertTrue(firstFingerprints.get(0).contains("replay=PARTIAL"));
        assertTrue(firstFingerprints.get(2).contains("sufficiency=BASIC_DIAGNOSTICS_ONLY|"));
        assertTrue(firstFingerprints.get(2).contains("replay=PARTIAL"));
        assertTrue(firstFingerprints.get(2).contains("scenario=MISSING_CANDIDATE_INPUT/INSUFFICIENT"));
        assertTrue(firstFingerprints.get(3).contains("policy=HIGH/DEGRADED_EVIDENCE"));
    }

    @Test
    void unknownFallbackKeepsSafeAdditiveArraysAndFingerprintFields() {
        JsonNode json = OBJECT_MAPPER.valueToTree(DecisionExplorerShadowDecisionQualityEvaluationV1.unknown(null));

        assertEquals("UNKNOWN", json.path("qualityLabel").asText());
        assertEquals("UNKNOWN", json.path("qualityBand").asText());
        assertEquals("UNKNOWN", json.path("selectedCandidateId").asText());
        assertEquals("INSUFFICIENT", json.path("evidenceSufficiencyLevel").asText());
        assertEquals("UNKNOWN", json.path("replayReadinessStatus").asText());
        assertTrue(json.path("candidateOutcomeComparisons").isArray());
        assertEquals(0, json.path("candidateOutcomeComparisons").size());
        assertTrue(json.path("evidenceBasis").isArray());
        assertTrue(json.path("selectedCandidateBasis").isArray());
        assertTrue(json.path("qualityReasons").isArray());
        assertTrue(json.path("unknowns").isArray());
        assertEquals("UNKNOWN", json.at("/policySensitivityDiagnostic/sensitivityLevel").asText());
        assertEquals("UNKNOWN", json.at("/scenarioInputQuality/inputQualityLabel").asText());
        assertTrue(json.path("fingerprintInputs").isArray());
        assertTrue(json.path("diagnosticFingerprint").asText().startsWith("shadow-decision-quality|v1|"));
        assertTrue(json.path("explanationText").asText().contains("computed Decision Explorer evidence was unavailable"));
    }

    @Test
    void shadowDecisionQualityCompatibilityPayloadDoesNotOverclaimOrMutateRoutingBoundaries() {
        String normalized = shadowQualitySnapshots().toString().toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("read-only"));
        assertTrue(normalized.contains("no production routing decision is changed"));
        assertTrue(normalized.contains("replayexecutionavailable=false"));
        for (String forbidden : List.of(
                "production readiness proven",
                "certification complete",
                "live-cloud validation complete",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "replay execution available=true",
                "replay storage available=true",
                "replay export available=true",
                "evidence packet generated",
                "traffic shifting enabled",
                "production routing decision changed",
                "autonomous production action enabled")) {
            assertFalse(normalized.contains(forbidden),
                    "shadow decision-quality diagnostics must not overclaim " + forbidden);
        }
    }

    private List<ShadowQualitySnapshot> shadowQualitySnapshots() {
        return DecisionExplorerConfidenceSummaryFixtureCatalog.fixtures().stream()
                .map(fixture -> {
                    DecisionExplorerConfidenceSummaryV1 summary = fixture.build(summaryService);
                    DecisionExplorerRoutingDiagnosticsV1 diagnostics = diagnosticsService.buildDiagnostics(
                            summary,
                            fixture.candidateSet(),
                            fixture.candidateComparisons(),
                            fixture.factorDrilldowns(),
                            fixture.warnings(),
                            fixture.unknowns(),
                            DecisionExplorerConfidenceSummaryFixtureCatalog.BOUNDARY_NOTE);
                    DecisionExplorerRouteTradeoffAnalysisV1 tradeoff = tradeoffService.buildTradeoffs(
                            summary,
                            diagnostics,
                            DecisionExplorerConfidenceSummaryFixtureCatalog.BOUNDARY_NOTE);
                    DecisionExplorerShadowDecisionQualityEvaluationV1 evaluation =
                            shadowQualityService.buildEvaluation(
                                    summary,
                                    diagnostics,
                                    tradeoff,
                                    DecisionExplorerConfidenceSummaryFixtureCatalog.BOUNDARY_NOTE);
                    return new ShadowQualitySnapshot(fixture.fixtureId(), evaluation);
                })
                .toList();
    }

    private List<String> compatibilityFingerprints() {
        return shadowQualitySnapshots().stream()
                .map(snapshot -> compatibilityFingerprint(snapshot.fixtureId(), snapshot.evaluation()))
                .toList();
    }

    private static String compatibilityFingerprint(
            String fixtureId,
            DecisionExplorerShadowDecisionQualityEvaluationV1 evaluation) {
        return fixtureId + "|"
                + evaluation.qualityLabel() + "|"
                + evaluation.qualityBand() + "|"
                + evaluation.qualityScore() + "|"
                + evaluation.selectedCandidateId() + "|"
                + evaluation.confidenceStatus() + "|"
                + evaluation.tradeoffCategory()
                + "|sufficiency=" + evaluation.evidenceSufficiencyLevel()
                + "|replay=" + evaluation.replayReadinessStatus()
                + "|policy=" + evaluation.policySensitivityDiagnostic().sensitivityLevel()
                + "/" + evaluation.policySensitivityDiagnostic().sensitivityCategory()
                + "|scenario=" + evaluation.scenarioInputQuality().inputQualityLabel()
                + "/" + evaluation.scenarioInputQuality().supportBand()
                + "|outcomes=" + evaluation.candidateOutcomeComparisons().stream()
                        .map(row -> row.displayOrder() + ":" + row.candidateId() + ":" + row.outcomeLabel()
                                + ":" + row.qualityImpact() + ":" + row.scoreGapCategory())
                        .collect(Collectors.joining(","))
                + "|fingerprintInputCount=" + evaluation.fingerprintInputs().size();
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private record ShadowQualitySnapshot(
            String fixtureId,
            DecisionExplorerShadowDecisionQualityEvaluationV1 evaluation) {
    }
}
