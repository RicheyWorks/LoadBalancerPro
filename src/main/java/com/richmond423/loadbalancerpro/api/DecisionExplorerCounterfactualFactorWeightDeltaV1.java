package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerCounterfactualFactorWeightDeltaV1(
        boolean readOnly,
        boolean simulationOnly,
        boolean localOnly,
        String deltaObject,
        String contractVersion,
        String factorName,
        String selectedCandidateId,
        String alternativeCandidateId,
        int displayOrder,
        String factorWeightDeltaClassification,
        String baselineDeltaClassification,
        String selectedContribution,
        String alternativeContribution,
        String selectedFactorStatus,
        String alternativeFactorStatus,
        String baselinePolicyProfile,
        String selectedSupportScenarioLabel,
        String alternativeSupportScenarioLabel,
        String strongestScenarioId,
        int localAssumptionWeightShiftPercent,
        boolean selectedSupportStabilizesDecision,
        boolean alternativeSupportCanChallengeSelection,
        String scoreGapCategory,
        Double alternativeScoreDeltaFromSelected,
        String summaryText,
        List<String> stabilizingSignals,
        List<String> destabilizingSignals,
        List<String> limitationSignals,
        List<String> reasonCodes,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String DELTA_OBJECT = "DecisionExplorerCounterfactualFactorWeightDeltaV1";
    public static final String CONTRACT_VERSION = "v1";

    public static final String CLASSIFICATION_STABILIZING = "STABILIZING";
    public static final String CLASSIFICATION_DESTABILIZING = "DESTABILIZING";
    public static final String CLASSIFICATION_NEUTRAL = "NEUTRAL";
    public static final String CLASSIFICATION_DEGRADED = "DEGRADED";
    public static final String CLASSIFICATION_UNKNOWN = "UNKNOWN";

    public DecisionExplorerCounterfactualFactorWeightDeltaV1 {
        readOnly = true;
        simulationOnly = true;
        localOnly = true;
        deltaObject = DecisionExplorerDtoSupport.valueOrDefault(deltaObject, DELTA_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        factorName = DecisionExplorerDtoSupport.valueOrUnknown(factorName);
        selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateId);
        alternativeCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(alternativeCandidateId);
        displayOrder = Math.max(0, displayOrder);
        factorWeightDeltaClassification = normalizeClassification(factorWeightDeltaClassification);
        baselineDeltaClassification = normalizeBaselineDelta(baselineDeltaClassification);
        selectedContribution = DecisionExplorerDtoSupport.valueOrUnknown(selectedContribution);
        alternativeContribution = DecisionExplorerDtoSupport.valueOrUnknown(alternativeContribution);
        selectedFactorStatus = DecisionExplorerDtoSupport.valueOrUnknown(selectedFactorStatus);
        alternativeFactorStatus = DecisionExplorerDtoSupport.valueOrUnknown(alternativeFactorStatus);
        baselinePolicyProfile = DecisionExplorerDtoSupport.valueOrDefault(
                baselinePolicyProfile,
                DecisionExplorerCounterfactualPolicyWeightScenarioV1.PROFILE_RETURNED_EVIDENCE_WEIGHTS);
        selectedSupportScenarioLabel = normalizeCounterfactualLabel(selectedSupportScenarioLabel);
        alternativeSupportScenarioLabel = normalizeCounterfactualLabel(alternativeSupportScenarioLabel);
        strongestScenarioId = DecisionExplorerDtoSupport.valueOrUnknown(strongestScenarioId);
        localAssumptionWeightShiftPercent = Math.max(-10, Math.min(10, localAssumptionWeightShiftPercent));
        scoreGapCategory = DecisionExplorerDtoSupport.valueOrUnknown(scoreGapCategory);
        alternativeScoreDeltaFromSelected = finiteOrNull(alternativeScoreDeltaFromSelected);
        summaryText = DecisionExplorerDtoSupport.valueOrUnknown(summaryText);
        stabilizingSignals = DecisionExplorerDtoSupport.copyOrEmpty(stabilizingSignals);
        destabilizingSignals = DecisionExplorerDtoSupport.copyOrEmpty(destabilizingSignals);
        limitationSignals = DecisionExplorerDtoSupport.copyOrEmpty(limitationSignals);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    String fingerprintInput() {
        return factorName
                + ":classification=" + factorWeightDeltaClassification
                + ":baseline=" + baselineDeltaClassification
                + ":selected=" + selectedCandidateId
                + ":alternative=" + alternativeCandidateId
                + ":scenario=" + strongestScenarioId
                + ":shift=" + localAssumptionWeightShiftPercent
                + ":selectedStabilizes=" + selectedSupportStabilizesDecision
                + ":alternativeChallenges=" + alternativeSupportCanChallengeSelection
                + ":gap=" + scoreGapCategory
                + ":delta=" + (alternativeScoreDeltaFromSelected == null
                        ? "null"
                        : alternativeScoreDeltaFromSelected);
    }

    private static String normalizeClassification(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case CLASSIFICATION_STABILIZING,
                    CLASSIFICATION_DESTABILIZING,
                    CLASSIFICATION_NEUTRAL,
                    CLASSIFICATION_DEGRADED,
                    CLASSIFICATION_UNKNOWN -> normalized;
            default -> CLASSIFICATION_UNKNOWN;
        };
    }

    private static String normalizeBaselineDelta(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case DecisionExplorerFactorTradeoffDeltaV1.DELTA_ADVANTAGE,
                    DecisionExplorerFactorTradeoffDeltaV1.DELTA_DISADVANTAGE,
                    DecisionExplorerFactorTradeoffDeltaV1.DELTA_NEUTRAL,
                    DecisionExplorerFactorTradeoffDeltaV1.DELTA_DEGRADED,
                    DecisionExplorerFactorTradeoffDeltaV1.DELTA_UNKNOWN -> normalized;
            default -> DecisionExplorerFactorTradeoffDeltaV1.DELTA_UNKNOWN;
        };
    }

    private static String normalizeCounterfactualLabel(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case DecisionExplorerCounterfactualAnalysisV1.LABEL_STABLE,
                    DecisionExplorerCounterfactualAnalysisV1.LABEL_SENSITIVE,
                    DecisionExplorerCounterfactualAnalysisV1.LABEL_CLOSE_CALL,
                    DecisionExplorerCounterfactualAnalysisV1.LABEL_DEGRADED,
                    DecisionExplorerCounterfactualAnalysisV1.LABEL_INSUFFICIENT_EVIDENCE,
                    DecisionExplorerCounterfactualAnalysisV1.LABEL_UNKNOWN -> normalized;
            default -> DecisionExplorerCounterfactualAnalysisV1.LABEL_UNKNOWN;
        };
    }

    private static Double finiteOrNull(Double value) {
        return value == null || !Double.isFinite(value) ? null : value;
    }
}
