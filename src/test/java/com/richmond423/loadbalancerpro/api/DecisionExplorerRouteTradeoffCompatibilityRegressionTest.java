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

class DecisionExplorerRouteTradeoffCompatibilityRegressionTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final DecisionExplorerConfidenceSummaryService summaryService =
            new DecisionExplorerConfidenceSummaryService();
    private final DecisionExplorerRoutingDiagnosticsService diagnosticsService =
            new DecisionExplorerRoutingDiagnosticsService();
    private final DecisionExplorerRouteTradeoffService tradeoffService =
            new DecisionExplorerRouteTradeoffService();

    @Test
    void routeTradeoffJsonKeepsAdditiveFieldOrder() {
        JsonNode json = OBJECT_MAPPER.valueToTree(tradeoffSnapshots().get(0).analysis());

        assertEquals(List.of(
                "readOnly",
                "simulationOnly",
                "analysisObject",
                "contractVersion",
                "overallStatus",
                "evidenceQuality",
                "selectedCandidateId",
                "tradeoffCategory",
                "selectedCandidateSummary",
                "alternativeCandidateSummary",
                "candidateTradeoffCount",
                "alternativeCount",
                "comparedAlternativeCount",
                "closestAlternativeCandidateId",
                "closestAlternativeScoreDelta",
                "candidateTradeoffs",
                "candidateScoringExplanations",
                "factorTradeoffDeltas",
                "evidenceSufficiency",
                "replayReadinessDiagnostic",
                "fingerprintAlgorithm",
                "diagnosticFingerprint",
                "reproducibilityKey",
                "explanationText",
                "fingerprintInputs",
                "tradeoffReasons",
                "warnings",
                "unknowns",
                "sourceReferenceIds",
                "boundaryNote"), fieldNames(json));
        assertEquals("DecisionExplorerRouteTradeoffAnalysisV1", json.path("analysisObject").asText());
        assertEquals(DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                json.path("fingerprintAlgorithm").asText());
        assertTrue(json.path("diagnosticFingerprint").asText().startsWith("route-tradeoff|v1|"));
        assertTrue(json.path("explanationText").asText().contains("Route tradeoff explanation"));
        assertTrue(json.path("candidateTradeoffs").isArray());
        assertTrue(json.path("candidateScoringExplanations").isArray());
        assertTrue(json.path("factorTradeoffDeltas").isArray());
        assertEquals("DecisionExplorerEvidenceSufficiencyV1",
                json.at("/evidenceSufficiency/diagnosticObject").asText());
        assertEquals("DecisionExplorerReplayReadinessDiagnosticV1",
                json.at("/replayReadinessDiagnostic/diagnosticObject").asText());
        assertFalse(json.at("/replayReadinessDiagnostic/replayExecutionAvailable").asBoolean());
        assertFalse(json.at("/replayReadinessDiagnostic/replayStorageAvailable").asBoolean());
        assertFalse(json.at("/replayReadinessDiagnostic/replayExportAvailable").asBoolean());
    }

    @Test
    void fixtureTradeoffsSerializeDeterministicallyAcrossBuilds() {
        List<String> firstFingerprints = compatibilityFingerprints();
        List<String> secondFingerprints = compatibilityFingerprints();

        assertEquals(firstFingerprints, secondFingerprints);
        assertEquals(List.of(
                "strong-confirmed-selection|STRONG|COMPLETE|edge-a|SELECTED_ADVANTAGE|2/1/1",
                "partial-candidate-and-factor-evidence|PARTIAL|PARTIAL|edge-a|SELECTED_ADVANTAGE|2/1/1",
                "unknown-no-routing-evidence|UNKNOWN|UNKNOWN|UNKNOWN|UNKNOWN|0/0/0",
                "degraded-selected-health-evidence|DEGRADED|DEGRADED|edge-a|DEGRADED|1/0/0"),
                firstFingerprints.stream()
                        .map(fingerprint -> fingerprint.substring(0, fingerprint.indexOf("|sufficiency=")))
                        .toList());
        assertTrue(firstFingerprints.get(0).contains("sufficiency=TRADEOFF_READY:"));
        assertTrue(firstFingerprints.get(0).contains("replay=PARTIAL"));
        assertTrue(firstFingerprints.get(1).contains("replay=PARTIAL"));
        assertTrue(firstFingerprints.get(2).contains("sufficiency="));
        assertTrue(firstFingerprints.get(3).contains("replay=DEGRADED"));
    }

    @Test
    void dtoConstructorsReconcileCountsWithMaterializedRowsAndSignals() {
        DecisionExplorerRouteTradeoffRowV1 selected = row(
                "edge-a",
                true,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE,
                DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE,
                10.0,
                0.0);
        DecisionExplorerRouteTradeoffRowV1 alternative = row(
                "edge-b",
                false,
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_CLOSE,
                DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_TRADEOFF,
                11.0,
                1.0);
        DecisionExplorerEvidenceSufficiencyV1 sufficiency = new DecisionExplorerEvidenceSufficiencyV1(
                false,
                false,
                null,
                null,
                "REPLAY_STYLE_READY",
                500,
                true,
                true,
                true,
                -4,
                -3,
                -2,
                99,
                98,
                97,
                96,
                95,
                List.of("candidate evidence present", "score evidence present"),
                List.of("factor evidence partial"),
                List.of("alternative evidence missing"),
                List.of("selected health degraded"),
                List.of("hidden score unknown"),
                List.of("reason"),
                null,
                null,
                null,
                null,
                List.of("decision-vector"),
                "compatibility boundary");

        DecisionExplorerRouteTradeoffAnalysisV1 analysis = new DecisionExplorerRouteTradeoffAnalysisV1(
                false,
                false,
                null,
                null,
                "STRONG",
                "COMPLETE",
                "edge-a",
                "SELECTED_ADVANTAGE",
                "selected summary",
                "alternative summary",
                99,
                88,
                77,
                "edge-b",
                1.0,
                List.of(selected, alternative),
                List.of(),
                List.of(),
                sufficiency,
                DecisionExplorerReplayReadinessDiagnosticV1.unknown("compatibility boundary"),
                null,
                null,
                null,
                null,
                null,
                List.of("reason"),
                List.of(),
                List.of(),
                List.of("decision-vector"),
                "compatibility boundary");

        assertEquals(2, analysis.candidateTradeoffCount());
        assertEquals(1, analysis.alternativeCount());
        assertEquals(1, analysis.comparedAlternativeCount());
        assertEquals(100, analysis.evidenceSufficiency().readinessScore());
        assertEquals(2, analysis.evidenceSufficiency().presentEvidenceCount());
        assertEquals(1, analysis.evidenceSufficiency().partialEvidenceCount());
        assertEquals(1, analysis.evidenceSufficiency().missingEvidenceCount());
        assertEquals(1, analysis.evidenceSufficiency().degradedEvidenceCount());
        assertEquals(1, analysis.evidenceSufficiency().unknownEvidenceCount());
        assertTrue(analysis.readOnly());
        assertTrue(analysis.simulationOnly());
    }

    @Test
    void routeTradeoffCompatibilityPayloadDoesNotOverclaimOrMutateRoutingBoundaries() {
        String normalized = tradeoffSnapshots().toString().toLowerCase(Locale.ROOT);

        assertTrue(normalized.contains("read-only"));
        assertTrue(normalized.contains("no production routing behavior changes"));
        assertTrue(normalized.contains("replay execution"));
        for (String forbidden : List.of(
                "production readiness proven",
                "certification complete",
                "live-cloud validation complete",
                "real tenant validated",
                "benchmark proven",
                "throughput proven",
                "replay export is implemented",
                "autonomous production action enabled")) {
            assertFalse(normalized.contains(forbidden), "route tradeoff diagnostics must not overclaim " + forbidden);
        }
    }

    private List<TradeoffSnapshot> tradeoffSnapshots() {
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
                    DecisionExplorerRouteTradeoffAnalysisV1 analysis = tradeoffService.buildTradeoffs(
                            summary,
                            diagnostics,
                            DecisionExplorerConfidenceSummaryFixtureCatalog.BOUNDARY_NOTE);
                    return new TradeoffSnapshot(fixture.fixtureId(), analysis);
                })
                .toList();
    }

    private List<String> compatibilityFingerprints() {
        return tradeoffSnapshots().stream()
                .map(snapshot -> compatibilityFingerprint(snapshot.fixtureId(), snapshot.analysis()))
                .toList();
    }

    private static String compatibilityFingerprint(String fixtureId, DecisionExplorerRouteTradeoffAnalysisV1 analysis) {
        return fixtureId + "|"
                + analysis.overallStatus() + "|"
                + analysis.evidenceQuality() + "|"
                + analysis.selectedCandidateId() + "|"
                + analysis.tradeoffCategory() + "|"
                + analysis.candidateTradeoffCount() + "/"
                + analysis.alternativeCount() + "/"
                + analysis.comparedAlternativeCount()
                + "|sufficiency=" + analysis.evidenceSufficiency().sufficiencyLevel() + ":"
                + analysis.evidenceSufficiency().readinessScore()
                + "|replay=" + analysis.replayReadinessDiagnostic().readinessStatus()
                + "|candidates=" + analysis.candidateTradeoffs().stream()
                        .map(row -> row.displayOrder() + ":" + row.candidateId() + ":" + row.tradeoffCategory()
                                + ":" + row.riskBenefitClassification() + ":" + row.scoreGapCategory())
                        .collect(Collectors.joining(","))
                + "|scoring=" + analysis.candidateScoringExplanations().stream()
                        .map(row -> row.displayOrder() + ":" + row.candidateId() + ":" + row.explanationStatus()
                                + ":" + row.scoreEvidenceState() + ":" + row.factorStatusRollup())
                        .collect(Collectors.joining(","))
                + "|factors=" + analysis.factorTradeoffDeltas().stream()
                        .map(delta -> delta.alternativeCandidateId() + ":" + delta.factorName() + ":"
                                + delta.deltaClassification() + ":" + delta.selectedContribution() + ":"
                                + delta.alternativeContribution())
                        .collect(Collectors.joining(","));
    }

    private static DecisionExplorerRouteTradeoffRowV1 row(
            String candidateId,
            boolean selected,
            String tradeoffCategory,
            String classification,
            Double finalScore,
            Double scoreDelta) {
        return new DecisionExplorerRouteTradeoffRowV1(
                candidateId,
                candidateId,
                selected,
                selected ? 1 : 2,
                tradeoffCategory,
                classification,
                "STRONG",
                selected ? "LOW" : "REVIEW",
                "HEALTHY",
                finalScore,
                scoreDelta,
                selected ? "BASELINE" : "CLOSE",
                "scoring explanation",
                "evidence summary",
                List.of("benefit"),
                List.of("risk"),
                List.of(),
                List.of(),
                List.of("reason"),
                List.of("decision-vector:" + candidateId),
                "compatibility boundary");
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private record TradeoffSnapshot(String fixtureId, DecisionExplorerRouteTradeoffAnalysisV1 analysis) {
    }
}
