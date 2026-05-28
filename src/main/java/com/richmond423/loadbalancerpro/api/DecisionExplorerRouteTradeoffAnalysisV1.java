package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerRouteTradeoffAnalysisV1(
        boolean readOnly,
        boolean simulationOnly,
        String analysisObject,
        String contractVersion,
        String overallStatus,
        String evidenceQuality,
        String selectedCandidateId,
        String tradeoffCategory,
        String selectedCandidateSummary,
        String alternativeCandidateSummary,
        int candidateTradeoffCount,
        int alternativeCount,
        int comparedAlternativeCount,
        String closestAlternativeCandidateId,
        Double closestAlternativeScoreDelta,
        List<DecisionExplorerRouteTradeoffRowV1> candidateTradeoffs,
        List<DecisionExplorerCandidateTradeoffScoringExplanationV1> candidateScoringExplanations,
        List<String> tradeoffReasons,
        List<String> warnings,
        List<String> unknowns,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String ANALYSIS_OBJECT = "DecisionExplorerRouteTradeoffAnalysisV1";
    public static final String CONTRACT_VERSION = "v1";

    public DecisionExplorerRouteTradeoffAnalysisV1 {
        readOnly = true;
        simulationOnly = true;
        analysisObject = DecisionExplorerDtoSupport.valueOrDefault(analysisObject, ANALYSIS_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        overallStatus = normalizeStatus(overallStatus);
        evidenceQuality = normalizeEvidenceQuality(evidenceQuality, overallStatus);
        selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateId);
        tradeoffCategory = normalizeTradeoffCategory(tradeoffCategory);
        selectedCandidateSummary = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateSummary);
        alternativeCandidateSummary = DecisionExplorerDtoSupport.valueOrUnknown(alternativeCandidateSummary);
        candidateTradeoffCount = Math.max(0, candidateTradeoffCount);
        alternativeCount = Math.max(0, alternativeCount);
        comparedAlternativeCount = Math.max(0, comparedAlternativeCount);
        closestAlternativeCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(closestAlternativeCandidateId);
        closestAlternativeScoreDelta = finiteOrNull(closestAlternativeScoreDelta);
        candidateTradeoffs = DecisionExplorerDtoSupport.copyOrEmpty(candidateTradeoffs);
        candidateScoringExplanations = DecisionExplorerDtoSupport.copyOrEmpty(candidateScoringExplanations);
        tradeoffReasons = DecisionExplorerDtoSupport.copyOrEmpty(tradeoffReasons);
        warnings = DecisionExplorerDtoSupport.copyOrEmpty(warnings);
        unknowns = DecisionExplorerDtoSupport.copyOrEmpty(unknowns);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    public static DecisionExplorerRouteTradeoffAnalysisV1 unknown(String boundaryNote) {
        return new DecisionExplorerRouteTradeoffAnalysisV1(
                true,
                true,
                ANALYSIS_OBJECT,
                CONTRACT_VERSION,
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_UNKNOWN,
                "UNKNOWN",
                DecisionExplorerRouteTradeoffRowV1.TRADEOFF_UNKNOWN,
                "Selected candidate tradeoff analysis could not be computed because routing diagnostics "
                        + "were unavailable.",
                "Alternative candidate tradeoff analysis could not be computed because routing diagnostics "
                        + "were unavailable.",
                0,
                0,
                0,
                "UNKNOWN",
                null,
                List.of(),
                List.of(),
                List.of("ROUTING_DIAGNOSTICS_UNAVAILABLE"),
                List.of(),
                List.of("route tradeoff diagnostics were unavailable"),
                List.of(),
                boundaryNote);
    }

    private static String normalizeStatus(String status) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(status);
        return switch (normalized) {
            case DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                    DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                    DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                    DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED -> normalized;
            default -> DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        };
    }

    private static String normalizeEvidenceQuality(String evidenceQuality, String status) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(evidenceQuality);
        return switch (normalized) {
            case DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_COMPLETE,
                    DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_PARTIAL,
                    DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_UNKNOWN,
                    DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_DEGRADED -> normalized;
            default -> DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status);
        };
    }

    private static String normalizeTradeoffCategory(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case "SELECTED_ADVANTAGE",
                    "SELECTED_CHALLENGED",
                    "CLOSE_ALTERNATIVE",
                    "PARTIAL_TRADEOFF",
                    "NO_ALTERNATIVE",
                    "DEGRADED",
                    "UNKNOWN" -> normalized;
            default -> "UNKNOWN";
        };
    }

    private static Double finiteOrNull(Double value) {
        return value == null || !Double.isFinite(value) ? null : value;
    }
}
