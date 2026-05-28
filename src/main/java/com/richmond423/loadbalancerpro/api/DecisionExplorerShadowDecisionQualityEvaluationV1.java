package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerShadowDecisionQualityEvaluationV1(
        boolean readOnly,
        boolean simulationOnly,
        String evaluationObject,
        String contractVersion,
        String qualityLabel,
        String qualityBand,
        int qualityScore,
        String selectedCandidateId,
        String confidenceStatus,
        String evidenceQuality,
        String tradeoffCategory,
        String evidenceSufficiencyLevel,
        String replayReadinessStatus,
        int candidateOutcomeCount,
        List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomeComparisons,
        int evidenceBasisCount,
        int selectedCandidateBasisCount,
        String evidenceBasisSummary,
        String selectedCandidateBasisSummary,
        List<String> evidenceBasis,
        List<String> selectedCandidateBasis,
        List<String> qualityReasons,
        List<String> warnings,
        List<String> unknowns,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String EVALUATION_OBJECT = "DecisionExplorerShadowDecisionQualityEvaluationV1";
    public static final String CONTRACT_VERSION = "v1";
    public static final String LABEL_ACCEPTABLE = "ACCEPTABLE";
    public static final String LABEL_REVIEW_RECOMMENDED = "REVIEW_RECOMMENDED";
    public static final String LABEL_INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";
    public static final String LABEL_DEGRADED_DECISION = "DEGRADED_DECISION";
    public static final String LABEL_UNKNOWN = "UNKNOWN";
    public static final String BAND_HIGH = "HIGH";
    public static final String BAND_MEDIUM = "MEDIUM";
    public static final String BAND_LOW = "LOW";
    public static final String BAND_INSUFFICIENT = "INSUFFICIENT";
    public static final String BAND_UNKNOWN = "UNKNOWN";

    public DecisionExplorerShadowDecisionQualityEvaluationV1 {
        readOnly = true;
        simulationOnly = true;
        evaluationObject = DecisionExplorerDtoSupport.valueOrDefault(evaluationObject, EVALUATION_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        qualityLabel = normalizeQualityLabel(qualityLabel);
        qualityBand = normalizeQualityBand(qualityBand, qualityLabel);
        qualityScore = Math.max(0, Math.min(100, qualityScore));
        selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateId);
        confidenceStatus = normalizeConfidenceStatus(confidenceStatus);
        evidenceQuality = normalizeEvidenceQuality(evidenceQuality, confidenceStatus);
        tradeoffCategory = DecisionExplorerDtoSupport.valueOrUnknown(tradeoffCategory);
        evidenceSufficiencyLevel = normalizeSufficiencyLevel(evidenceSufficiencyLevel);
        replayReadinessStatus = normalizeReplayReadinessStatus(replayReadinessStatus);
        candidateOutcomeComparisons = DecisionExplorerDtoSupport.copyOrEmpty(candidateOutcomeComparisons);
        candidateOutcomeCount = candidateOutcomeComparisons.isEmpty()
                ? Math.max(0, candidateOutcomeCount)
                : candidateOutcomeComparisons.size();
        evidenceBasis = DecisionExplorerDtoSupport.copyOrEmpty(evidenceBasis);
        selectedCandidateBasis = DecisionExplorerDtoSupport.copyOrEmpty(selectedCandidateBasis);
        evidenceBasisCount = evidenceBasis.size();
        selectedCandidateBasisCount = selectedCandidateBasis.size();
        evidenceBasisSummary = DecisionExplorerDtoSupport.valueOrUnknown(evidenceBasisSummary);
        selectedCandidateBasisSummary = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateBasisSummary);
        qualityReasons = DecisionExplorerDtoSupport.copyOrEmpty(qualityReasons);
        warnings = DecisionExplorerDtoSupport.copyOrEmpty(warnings);
        unknowns = DecisionExplorerDtoSupport.copyOrEmpty(unknowns);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    public DecisionExplorerShadowDecisionQualityEvaluationV1(
            boolean readOnly,
            boolean simulationOnly,
            String evaluationObject,
            String contractVersion,
            String qualityLabel,
            String qualityBand,
            int qualityScore,
            String selectedCandidateId,
            String confidenceStatus,
            String evidenceQuality,
            String tradeoffCategory,
            String evidenceSufficiencyLevel,
            String replayReadinessStatus,
            int candidateOutcomeCount,
            int evidenceBasisCount,
            int selectedCandidateBasisCount,
            String evidenceBasisSummary,
            String selectedCandidateBasisSummary,
            List<String> evidenceBasis,
            List<String> selectedCandidateBasis,
            List<String> qualityReasons,
            List<String> warnings,
            List<String> unknowns,
            List<String> sourceReferenceIds,
            String boundaryNote) {
        this(
                readOnly,
                simulationOnly,
                evaluationObject,
                contractVersion,
                qualityLabel,
                qualityBand,
                qualityScore,
                selectedCandidateId,
                confidenceStatus,
                evidenceQuality,
                tradeoffCategory,
                evidenceSufficiencyLevel,
                replayReadinessStatus,
                candidateOutcomeCount,
                List.of(),
                evidenceBasisCount,
                selectedCandidateBasisCount,
                evidenceBasisSummary,
                selectedCandidateBasisSummary,
                evidenceBasis,
                selectedCandidateBasis,
                qualityReasons,
                warnings,
                unknowns,
                sourceReferenceIds,
                boundaryNote);
    }

    public static DecisionExplorerShadowDecisionQualityEvaluationV1 unknown(String boundaryNote) {
        return new DecisionExplorerShadowDecisionQualityEvaluationV1(
                true,
                true,
                EVALUATION_OBJECT,
                CONTRACT_VERSION,
                LABEL_UNKNOWN,
                BAND_UNKNOWN,
                0,
                "UNKNOWN",
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_UNKNOWN,
                "UNKNOWN",
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT,
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN,
                0,
                0,
                0,
                "Shadow decision-quality evaluation could not classify decision quality because computed "
                        + "Decision Explorer evidence was unavailable.",
                "Selected candidate quality basis was unavailable.",
                List.of(),
                List.of(),
                List.of("SHADOW_DECISION_QUALITY_UNKNOWN"),
                List.of(),
                List.of("shadow decision-quality input evidence was unavailable"),
                List.of(),
                boundaryNote);
    }

    private static String normalizeQualityLabel(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case LABEL_ACCEPTABLE,
                    LABEL_REVIEW_RECOMMENDED,
                    LABEL_INSUFFICIENT_EVIDENCE,
                    LABEL_DEGRADED_DECISION,
                    LABEL_UNKNOWN -> normalized;
            default -> LABEL_UNKNOWN;
        };
    }

    private static String normalizeQualityBand(String value, String qualityLabel) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case BAND_HIGH, BAND_MEDIUM, BAND_LOW, BAND_INSUFFICIENT, BAND_UNKNOWN -> normalized;
            default -> bandFor(qualityLabel);
        };
    }

    static String bandFor(String qualityLabel) {
        return switch (normalizeQualityLabel(qualityLabel)) {
            case LABEL_ACCEPTABLE -> BAND_HIGH;
            case LABEL_REVIEW_RECOMMENDED -> BAND_MEDIUM;
            case LABEL_DEGRADED_DECISION -> BAND_LOW;
            case LABEL_INSUFFICIENT_EVIDENCE -> BAND_INSUFFICIENT;
            default -> BAND_UNKNOWN;
        };
    }

    private static String normalizeConfidenceStatus(String status) {
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

    private static String normalizeSufficiencyLevel(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY,
                    DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY,
                    DecisionExplorerEvidenceSufficiencyV1.LEVEL_BASIC_DIAGNOSTICS_ONLY,
                    DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT,
                    DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED -> normalized;
            default -> DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT;
        };
    }

    private static String normalizeReplayReadinessStatus(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY,
                    DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL,
                    DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN,
                    DecisionExplorerReplayReadinessDiagnosticV1.STATUS_DEGRADED -> normalized;
            default -> DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN;
        };
    }
}
