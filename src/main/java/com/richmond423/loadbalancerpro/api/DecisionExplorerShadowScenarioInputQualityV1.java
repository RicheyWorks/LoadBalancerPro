package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerShadowScenarioInputQualityV1(
        boolean readOnly,
        boolean simulationOnly,
        String evaluationObject,
        String contractVersion,
        String inputQualityLabel,
        String supportBand,
        int inputQualityScore,
        String selectedCandidateId,
        String confidenceStatus,
        String evidenceSufficiencyLevel,
        String replayReadinessStatus,
        int candidateEvidenceCount,
        int factorEvidenceCount,
        int candidateOutcomeCount,
        int partialSignalCount,
        int missingSignalCount,
        int degradedSignalCount,
        String summaryText,
        List<String> candidateInputSignals,
        List<String> factorInputSignals,
        List<String> partialInputSignals,
        List<String> missingInputSignals,
        List<String> degradedInputSignals,
        List<String> reasonCodes,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String EVALUATION_OBJECT = "DecisionExplorerShadowScenarioInputQualityV1";
    public static final String CONTRACT_VERSION = "v1";

    public static final String LABEL_EVALUABLE = "EVALUABLE";
    public static final String LABEL_PARTIAL_INPUT = "PARTIAL_INPUT";
    public static final String LABEL_MISSING_CANDIDATE_INPUT = "MISSING_CANDIDATE_INPUT";
    public static final String LABEL_MISSING_FACTOR_INPUT = "MISSING_FACTOR_INPUT";
    public static final String LABEL_DEGRADED_INPUT = "DEGRADED_INPUT";
    public static final String LABEL_UNKNOWN = "UNKNOWN";

    public static final String BAND_HIGH = "HIGH";
    public static final String BAND_MEDIUM = "MEDIUM";
    public static final String BAND_LOW = "LOW";
    public static final String BAND_INSUFFICIENT = "INSUFFICIENT";
    public static final String BAND_UNKNOWN = "UNKNOWN";

    public DecisionExplorerShadowScenarioInputQualityV1 {
        readOnly = true;
        simulationOnly = true;
        evaluationObject = DecisionExplorerDtoSupport.valueOrDefault(evaluationObject, EVALUATION_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        inputQualityLabel = normalizeInputQualityLabel(inputQualityLabel);
        supportBand = normalizeSupportBand(supportBand, inputQualityLabel);
        inputQualityScore = Math.max(0, Math.min(100, inputQualityScore));
        selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateId);
        confidenceStatus = normalizeConfidenceStatus(confidenceStatus);
        evidenceSufficiencyLevel = normalizeSufficiencyLevel(evidenceSufficiencyLevel);
        replayReadinessStatus = normalizeReplayReadinessStatus(replayReadinessStatus);
        candidateEvidenceCount = Math.max(0, candidateEvidenceCount);
        factorEvidenceCount = Math.max(0, factorEvidenceCount);
        candidateOutcomeCount = Math.max(0, candidateOutcomeCount);
        candidateInputSignals = DecisionExplorerDtoSupport.copyOrEmpty(candidateInputSignals);
        factorInputSignals = DecisionExplorerDtoSupport.copyOrEmpty(factorInputSignals);
        partialInputSignals = DecisionExplorerDtoSupport.copyOrEmpty(partialInputSignals);
        missingInputSignals = DecisionExplorerDtoSupport.copyOrEmpty(missingInputSignals);
        degradedInputSignals = DecisionExplorerDtoSupport.copyOrEmpty(degradedInputSignals);
        partialSignalCount = partialInputSignals.size();
        missingSignalCount = missingInputSignals.size();
        degradedSignalCount = degradedInputSignals.size();
        summaryText = DecisionExplorerDtoSupport.valueOrUnknown(summaryText);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    public static DecisionExplorerShadowScenarioInputQualityV1 unknown(String boundaryNote) {
        return new DecisionExplorerShadowScenarioInputQualityV1(
                true,
                true,
                EVALUATION_OBJECT,
                CONTRACT_VERSION,
                LABEL_UNKNOWN,
                BAND_UNKNOWN,
                0,
                "UNKNOWN",
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT,
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN,
                0,
                0,
                0,
                0,
                1,
                0,
                "Scenario-input quality is UNKNOWN because computed local shadow evaluator inputs were unavailable.",
                List.of(),
                List.of(),
                List.of(),
                List.of("computed scenario-input evidence was unavailable"),
                List.of(),
                List.of("SHADOW_SCENARIO_INPUT_QUALITY_UNKNOWN"),
                List.of(),
                boundaryNote);
    }

    public static String supportBandFor(String inputQualityLabel) {
        return switch (normalizeInputQualityLabel(inputQualityLabel)) {
            case LABEL_EVALUABLE -> BAND_HIGH;
            case LABEL_PARTIAL_INPUT -> BAND_MEDIUM;
            case LABEL_DEGRADED_INPUT -> BAND_LOW;
            case LABEL_MISSING_CANDIDATE_INPUT, LABEL_MISSING_FACTOR_INPUT -> BAND_INSUFFICIENT;
            default -> BAND_UNKNOWN;
        };
    }

    private static String normalizeInputQualityLabel(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case LABEL_EVALUABLE,
                    LABEL_PARTIAL_INPUT,
                    LABEL_MISSING_CANDIDATE_INPUT,
                    LABEL_MISSING_FACTOR_INPUT,
                    LABEL_DEGRADED_INPUT,
                    LABEL_UNKNOWN -> normalized;
            default -> LABEL_UNKNOWN;
        };
    }

    private static String normalizeSupportBand(String value, String inputQualityLabel) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case BAND_HIGH, BAND_MEDIUM, BAND_LOW, BAND_INSUFFICIENT, BAND_UNKNOWN -> normalized;
            default -> supportBandFor(inputQualityLabel);
        };
    }

    private static String normalizeConfidenceStatus(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                    DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                    DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                    DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED -> normalized;
            default -> DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
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
