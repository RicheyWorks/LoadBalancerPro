package com.richmond423.loadbalancerpro.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionExplorerRouteTradeoffFingerprintBuilderTest {
    private static final String BOUNDARY_NOTE = "read-only route tradeoff diagnostics";

    private final DecisionExplorerRouteTradeoffFingerprintBuilder builder =
            new DecisionExplorerRouteTradeoffFingerprintBuilder();

    @Test
    void completeInputsBuildStableRouteFingerprintAndReproducibilityKey() {
        DecisionExplorerRouteTradeoffRowV1 selected = row("edge-a", true, 1, 10.0, 0.0);
        DecisionExplorerRouteTradeoffRowV1 alternative = row("edge-b", false, 2, 9.5, 5.0);

        DecisionExplorerRouteTradeoffFingerprintBuilder.Result first = builder.build(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "edge-a"),
                List.of(selected, alternative),
                List.of(alternative),
                alternative,
                "SELECTED_ADVANTAGE",
                sufficiency(DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY, 100),
                replay(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY),
                List.of("ROUTE_TRADEOFF_CATEGORY_SELECTED_ADVANTAGE"),
                List.of("warning-two", "warning-one"),
                List.of("hidden routing internals"),
                List.of("source-b", "source-a"));
        DecisionExplorerRouteTradeoffFingerprintBuilder.Result second = builder.build(
                summary(DecisionExplorerConfidenceSummaryV1.STATUS_STRONG, "edge-a"),
                List.of(selected, alternative),
                List.of(alternative),
                alternative,
                "SELECTED_ADVANTAGE",
                sufficiency(DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY, 100),
                replay(DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY),
                List.of("ROUTE_TRADEOFF_CATEGORY_SELECTED_ADVANTAGE"),
                List.of("warning-two", "warning-one"),
                List.of("hidden routing internals"),
                List.of("source-b", "source-a"));

        assertEquals("route-tradeoff:v1:STRONG:edge-a:SELECTED_ADVANTAGE:rows=2:alternatives=1:"
                + "sufficiency=REPLAY_STYLE_READY:replay=READY", first.reproducibilityKey());
        assertTrue(first.diagnosticFingerprint().startsWith("route-tradeoff|v1|"));
        assertTrue(first.fingerprintInputs().contains("overallStatus=STRONG"));
        assertTrue(first.fingerprintInputs().contains("selectedCandidateId=edge-a"));
        assertTrue(first.fingerprintInputs().contains("candidateTradeoffCount=2"));
        assertTrue(first.fingerprintInputs().contains("alternativeCount=1"));
        assertTrue(first.fingerprintInputs().contains("comparedAlternativeCount=1"));
        assertTrue(first.fingerprintInputs().contains("closestAlternativeCandidateId=edge-b"));
        assertTrue(first.fingerprintInputs().contains("closestAlternativeScoreDelta=5.0"));
        assertTrue(first.fingerprintInputs().contains("evidenceSufficiency.sufficiencyLevel=REPLAY_STYLE_READY"));
        assertTrue(first.fingerprintInputs().contains("replayReadiness.readinessStatus=READY"));
        assertTrue(first.fingerprintInputs().stream()
                .anyMatch(input -> input.startsWith("candidateTradeoff=candidate=edge-a,selected=true")));
        assertTrue(first.fingerprintInputs().contains("warnings=warning-one;warning-two"));
        assertTrue(first.fingerprintInputs().contains("sourceReferenceIds=source-a;source-b"));
        assertEquals(first.diagnosticFingerprint(), second.diagnosticFingerprint());
        assertEquals(first.fingerprintInputs(), second.fingerprintInputs());
    }

    @Test
    void missingInputsProduceStableUnknownSafeFingerprintValues() {
        DecisionExplorerRouteTradeoffFingerprintBuilder.Result result =
                builder.build(null, null, null, null, null, null, null, null, null, null, null);

        assertEquals("route-tradeoff:v1:null:null:null:rows=0:alternatives=0:sufficiency=null:replay=null",
                result.reproducibilityKey());
        assertTrue(result.diagnosticFingerprint().startsWith("route-tradeoff|v1|"));
        assertTrue(result.fingerprintInputs().contains("overallStatus=null"));
        assertTrue(result.fingerprintInputs().contains("selectedCandidateId=null"));
        assertTrue(result.fingerprintInputs().contains("candidateTradeoffCount=0"));
        assertTrue(result.fingerprintInputs().contains("closestAlternativeCandidateId=UNKNOWN"));
        assertTrue(result.fingerprintInputs().contains("tradeoffReasons=null"));
    }

    private static DecisionExplorerConfidenceSummaryV1 summary(String status, String selectedCandidateId) {
        return new DecisionExplorerConfidenceSummaryV1(
                true,
                true,
                DecisionExplorerConfidenceSummaryV1.SUMMARY_OBJECT,
                DecisionExplorerConfidenceSummaryV1.CONTRACT_VERSION,
                status,
                DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status),
                selectedCandidateId,
                2,
                1,
                1,
                0,
                0,
                0,
                0,
                1,
                List.of("summary evidence"),
                List.of("SUMMARY_STATUS_" + status),
                List.of(),
                List.of(),
                List.of("summary-source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerEvidenceSufficiencyV1 sufficiency(String level, int readinessScore) {
        return new DecisionExplorerEvidenceSufficiencyV1(
                true,
                true,
                DecisionExplorerEvidenceSufficiencyV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerEvidenceSufficiencyV1.CONTRACT_VERSION,
                level,
                readinessScore,
                true,
                true,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY.equals(level),
                2,
                1,
                1,
                1,
                0,
                0,
                0,
                0,
                List.of("candidate evidence present"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("EVIDENCE_SUFFICIENCY_LEVEL_" + level),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "evidence-sufficiency|v1|" + level,
                "evidence-sufficiency:v1:" + level,
                List.of("sufficiencyLevel=" + level, "readinessScore=" + readinessScore),
                List.of("sufficiency-source"),
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerReplayReadinessDiagnosticV1 replay(String readinessStatus) {
        return new DecisionExplorerReplayReadinessDiagnosticV1(
                true,
                true,
                DecisionExplorerReplayReadinessDiagnosticV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerReplayReadinessDiagnosticV1.CONTRACT_VERSION,
                readinessStatus,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY,
                100,
                false,
                false,
                false,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE,
                List.of("candidate evidence present"),
                List.of(),
                List.of(),
                List.of(),
                List.of("replay execution is intentionally unavailable"),
                List.of("candidate evidence: AVAILABLE"),
                List.of("replay execution is intentionally unavailable"),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "replay-readiness|v1|" + readinessStatus,
                "replay-readiness:v1:" + readinessStatus,
                List.of("readinessStatus=" + readinessStatus),
                List.of("replay-source"),
                "replay readiness summary",
                BOUNDARY_NOTE);
    }

    private static DecisionExplorerRouteTradeoffRowV1 row(
            String candidateId,
            boolean selected,
            int displayOrder,
            Double finalScore,
            Double scoreDelta) {
        return new DecisionExplorerRouteTradeoffRowV1(
                candidateId,
                candidateId,
                selected,
                displayOrder,
                selected ? DecisionExplorerRouteTradeoffRowV1.TRADEOFF_SELECTED_BASELINE
                        : DecisionExplorerRouteTradeoffRowV1.TRADEOFF_ALTERNATIVE_TRAILS_SELECTED,
                selected ? DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_BASELINE
                        : DecisionExplorerRouteTradeoffRowV1.CLASSIFICATION_TRADEOFF,
                DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                DecisionExplorerCandidateDiagnosticV1.RISK_LOW,
                DecisionExplorerCandidateConfidenceV1.HEALTHY,
                finalScore,
                scoreDelta,
                DecisionExplorerRouteTradeoffRowV1.scoreGapCategoryFor(selected, scoreDelta),
                "scoring explanation",
                "evidence summary",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("ROW_" + candidateId),
                List.of("row-source"),
                BOUNDARY_NOTE);
    }
}
