package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerCounterfactualCandidateOutcomeV1(
        boolean readOnly,
        boolean simulationOnly,
        boolean localOnly,
        String outcomeObject,
        String contractVersion,
        String candidateId,
        String candidateLabel,
        boolean selected,
        int displayOrder,
        String baselineOutcomeLabel,
        String counterfactualOutcomeLabel,
        String policyScenarioInfluence,
        String strongestScenarioId,
        String tradeoffCategory,
        String riskBenefitClassification,
        String scoreGapCategory,
        Double finalScore,
        Double scoreDeltaFromSelected,
        List<String> supportingScenarioIds,
        List<String> challengingScenarioIds,
        String summaryText,
        List<String> benefitSignals,
        List<String> riskSignals,
        List<String> unknownSignals,
        List<String> degradedSignals,
        List<String> reasonCodes,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String OUTCOME_OBJECT = "DecisionExplorerCounterfactualCandidateOutcomeV1";
    public static final String CONTRACT_VERSION = "v1";

    public static final String OUTCOME_SELECTED_STABLE = "SELECTED_STABLE";
    public static final String OUTCOME_SELECTED_SENSITIVE = "SELECTED_SENSITIVE";
    public static final String OUTCOME_SELECTED_DEGRADED = "SELECTED_DEGRADED";
    public static final String OUTCOME_ALTERNATIVE_TRAILING = "ALTERNATIVE_TRAILING";
    public static final String OUTCOME_ALTERNATIVE_CLOSE_CALL = "ALTERNATIVE_CLOSE_CALL";
    public static final String OUTCOME_ALTERNATIVE_CHALLENGES_SELECTED = "ALTERNATIVE_CHALLENGES_SELECTED";
    public static final String OUTCOME_ALTERNATIVE_UNKNOWN = "ALTERNATIVE_UNKNOWN";
    public static final String OUTCOME_INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";
    public static final String OUTCOME_UNKNOWN = "UNKNOWN";

    public static final String INFLUENCE_STABILIZING = "STABILIZING";
    public static final String INFLUENCE_SENSITIVE = "SENSITIVE";
    public static final String INFLUENCE_CHALLENGING = "CHALLENGING";
    public static final String INFLUENCE_DEGRADED = "DEGRADED";
    public static final String INFLUENCE_INSUFFICIENT = "INSUFFICIENT_EVIDENCE";
    public static final String INFLUENCE_UNKNOWN = "UNKNOWN";

    public DecisionExplorerCounterfactualCandidateOutcomeV1 {
        readOnly = true;
        simulationOnly = true;
        localOnly = true;
        outcomeObject = DecisionExplorerDtoSupport.valueOrDefault(outcomeObject, OUTCOME_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        candidateId = DecisionExplorerDtoSupport.valueOrUnknown(candidateId);
        candidateLabel = DecisionExplorerDtoSupport.valueOrDefault(candidateLabel, candidateId);
        displayOrder = Math.max(0, displayOrder);
        baselineOutcomeLabel = DecisionExplorerDtoSupport.valueOrUnknown(baselineOutcomeLabel);
        counterfactualOutcomeLabel = normalizeOutcome(counterfactualOutcomeLabel);
        policyScenarioInfluence = normalizeInfluence(policyScenarioInfluence);
        strongestScenarioId = DecisionExplorerDtoSupport.valueOrUnknown(strongestScenarioId);
        tradeoffCategory = DecisionExplorerDtoSupport.valueOrUnknown(tradeoffCategory);
        riskBenefitClassification = DecisionExplorerDtoSupport.valueOrUnknown(riskBenefitClassification);
        scoreGapCategory = DecisionExplorerDtoSupport.valueOrUnknown(scoreGapCategory);
        finalScore = finiteOrNull(finalScore);
        scoreDeltaFromSelected = finiteOrNull(scoreDeltaFromSelected);
        supportingScenarioIds = DecisionExplorerDtoSupport.copyOrEmpty(supportingScenarioIds);
        challengingScenarioIds = DecisionExplorerDtoSupport.copyOrEmpty(challengingScenarioIds);
        summaryText = DecisionExplorerDtoSupport.valueOrUnknown(summaryText);
        benefitSignals = DecisionExplorerDtoSupport.copyOrEmpty(benefitSignals);
        riskSignals = DecisionExplorerDtoSupport.copyOrEmpty(riskSignals);
        unknownSignals = DecisionExplorerDtoSupport.copyOrEmpty(unknownSignals);
        degradedSignals = DecisionExplorerDtoSupport.copyOrEmpty(degradedSignals);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    String fingerprintInput() {
        return candidateId
                + ":selected=" + selected
                + ":outcome=" + counterfactualOutcomeLabel
                + ":influence=" + policyScenarioInfluence
                + ":scenario=" + strongestScenarioId
                + ":tradeoff=" + tradeoffCategory
                + ":gap=" + scoreGapCategory
                + ":delta=" + (scoreDeltaFromSelected == null ? "null" : scoreDeltaFromSelected);
    }

    private static String normalizeOutcome(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case OUTCOME_SELECTED_STABLE,
                    OUTCOME_SELECTED_SENSITIVE,
                    OUTCOME_SELECTED_DEGRADED,
                    OUTCOME_ALTERNATIVE_TRAILING,
                    OUTCOME_ALTERNATIVE_CLOSE_CALL,
                    OUTCOME_ALTERNATIVE_CHALLENGES_SELECTED,
                    OUTCOME_ALTERNATIVE_UNKNOWN,
                    OUTCOME_INSUFFICIENT_EVIDENCE,
                    OUTCOME_UNKNOWN -> normalized;
            default -> OUTCOME_UNKNOWN;
        };
    }

    private static String normalizeInfluence(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case INFLUENCE_STABILIZING,
                    INFLUENCE_SENSITIVE,
                    INFLUENCE_CHALLENGING,
                    INFLUENCE_DEGRADED,
                    INFLUENCE_INSUFFICIENT,
                    INFLUENCE_UNKNOWN -> normalized;
            default -> INFLUENCE_UNKNOWN;
        };
    }

    private static Double finiteOrNull(Double value) {
        return value == null || !Double.isFinite(value) ? null : value;
    }
}
