package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerCounterfactualPolicyWeightScenarioV1(
        boolean readOnly,
        boolean simulationOnly,
        boolean localOnly,
        String scenarioObject,
        String contractVersion,
        String scenarioId,
        int displayOrder,
        String policyWeightProfile,
        int localAssumptionWeightShiftPercent,
        String sensitivityLabel,
        String sensitivityBand,
        String selectedCandidateId,
        String referenceAlternativeCandidateId,
        Double closestAlternativeScoreDelta,
        boolean selectedCandidateRemainsSupported,
        boolean alternativeBecomesCloseOrPreferable,
        String tradeoffCategory,
        String evidenceSufficiencyLevel,
        String replayReadinessStatus,
        String summaryText,
        List<String> stabilizingSignals,
        List<String> sensitivitySignals,
        List<String> limitationSignals,
        List<String> reasonCodes,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String SCENARIO_OBJECT = "DecisionExplorerCounterfactualPolicyWeightScenarioV1";
    public static final String CONTRACT_VERSION = "v1";

    public static final String SCENARIO_BASELINE_RETURNED_EVIDENCE = "BASELINE_RETURNED_EVIDENCE";
    public static final String SCENARIO_SELECTED_SUPPORT_PLUS_10 = "SELECTED_SUPPORT_PLUS_10";
    public static final String SCENARIO_ALTERNATIVE_SUPPORT_PLUS_10 = "ALTERNATIVE_SUPPORT_PLUS_10";

    public static final String PROFILE_RETURNED_EVIDENCE_WEIGHTS = "RETURNED_EVIDENCE_WEIGHTS";
    public static final String PROFILE_LOCAL_SELECTED_SUPPORT_PLUS_10 = "LOCAL_SELECTED_SUPPORT_PLUS_10";
    public static final String PROFILE_LOCAL_ALTERNATIVE_SUPPORT_PLUS_10 = "LOCAL_ALTERNATIVE_SUPPORT_PLUS_10";

    public DecisionExplorerCounterfactualPolicyWeightScenarioV1 {
        readOnly = true;
        simulationOnly = true;
        localOnly = true;
        scenarioObject = DecisionExplorerDtoSupport.valueOrDefault(scenarioObject, SCENARIO_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        scenarioId = normalizeScenarioId(scenarioId);
        displayOrder = Math.max(0, displayOrder);
        policyWeightProfile = normalizePolicyWeightProfile(policyWeightProfile);
        localAssumptionWeightShiftPercent = Math.max(-10, Math.min(10, localAssumptionWeightShiftPercent));
        sensitivityLabel = normalizeSensitivityLabel(sensitivityLabel);
        sensitivityBand = DecisionExplorerCounterfactualAnalysisV1.bandFor(sensitivityLabel);
        selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateId);
        referenceAlternativeCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(referenceAlternativeCandidateId);
        closestAlternativeScoreDelta = finiteOrNull(closestAlternativeScoreDelta);
        tradeoffCategory = DecisionExplorerDtoSupport.valueOrUnknown(tradeoffCategory);
        evidenceSufficiencyLevel = DecisionExplorerDtoSupport.valueOrUnknown(evidenceSufficiencyLevel);
        replayReadinessStatus = DecisionExplorerDtoSupport.valueOrUnknown(replayReadinessStatus);
        summaryText = DecisionExplorerDtoSupport.valueOrUnknown(summaryText);
        stabilizingSignals = DecisionExplorerDtoSupport.copyOrEmpty(stabilizingSignals);
        sensitivitySignals = DecisionExplorerDtoSupport.copyOrEmpty(sensitivitySignals);
        limitationSignals = DecisionExplorerDtoSupport.copyOrEmpty(limitationSignals);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    String fingerprintInput() {
        return scenarioId
                + ":profile=" + policyWeightProfile
                + ":shift=" + localAssumptionWeightShiftPercent
                + ":label=" + sensitivityLabel
                + ":band=" + sensitivityBand
                + ":selected=" + selectedCandidateId
                + ":alternative=" + referenceAlternativeCandidateId
                + ":delta=" + (closestAlternativeScoreDelta == null ? "null" : closestAlternativeScoreDelta)
                + ":selectedSupported=" + selectedCandidateRemainsSupported
                + ":alternativeClose=" + alternativeBecomesCloseOrPreferable;
    }

    private static String normalizeScenarioId(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case SCENARIO_BASELINE_RETURNED_EVIDENCE,
                    SCENARIO_SELECTED_SUPPORT_PLUS_10,
                    SCENARIO_ALTERNATIVE_SUPPORT_PLUS_10 -> normalized;
            default -> SCENARIO_BASELINE_RETURNED_EVIDENCE;
        };
    }

    private static String normalizePolicyWeightProfile(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrDefault(value, PROFILE_RETURNED_EVIDENCE_WEIGHTS);
        return switch (normalized) {
            case PROFILE_RETURNED_EVIDENCE_WEIGHTS,
                    PROFILE_LOCAL_SELECTED_SUPPORT_PLUS_10,
                    PROFILE_LOCAL_ALTERNATIVE_SUPPORT_PLUS_10 -> normalized;
            default -> PROFILE_RETURNED_EVIDENCE_WEIGHTS;
        };
    }

    private static String normalizeSensitivityLabel(String value) {
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
