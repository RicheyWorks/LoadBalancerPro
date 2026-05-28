package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerShadowPolicySensitivityDiagnosticV1(
        boolean readOnly,
        boolean simulationOnly,
        String diagnosticObject,
        String contractVersion,
        String sensitivityLevel,
        String sensitivityCategory,
        int sensitivityScore,
        String selectedCandidateId,
        String tradeoffCategory,
        String evidenceSufficiencyLevel,
        String replayReadinessStatus,
        int candidateOutcomeCount,
        String summaryText,
        List<String> stableSignals,
        List<String> reviewSignals,
        List<String> missingEvidenceSignals,
        List<String> degradedSignals,
        List<String> reasonCodes,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String DIAGNOSTIC_OBJECT = "DecisionExplorerShadowPolicySensitivityDiagnosticV1";
    public static final String CONTRACT_VERSION = "v1";

    public static final String LEVEL_LOW = "LOW";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String LEVEL_HIGH = "HIGH";
    public static final String LEVEL_UNKNOWN = "UNKNOWN";

    public static final String CATEGORY_STABLE = "STABLE";
    public static final String CATEGORY_CLOSE_ALTERNATIVE = "CLOSE_ALTERNATIVE";
    public static final String CATEGORY_MISSING_EVIDENCE = "MISSING_EVIDENCE";
    public static final String CATEGORY_DEGRADED_EVIDENCE = "DEGRADED_EVIDENCE";
    public static final String CATEGORY_UNKNOWN = "UNKNOWN";

    public DecisionExplorerShadowPolicySensitivityDiagnosticV1 {
        readOnly = true;
        simulationOnly = true;
        diagnosticObject = DecisionExplorerDtoSupport.valueOrDefault(diagnosticObject, DIAGNOSTIC_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        sensitivityLevel = normalizeSensitivityLevel(sensitivityLevel);
        sensitivityCategory = normalizeSensitivityCategory(sensitivityCategory);
        sensitivityScore = Math.max(0, Math.min(100, sensitivityScore));
        selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateId);
        tradeoffCategory = DecisionExplorerDtoSupport.valueOrUnknown(tradeoffCategory);
        evidenceSufficiencyLevel = DecisionExplorerDtoSupport.valueOrUnknown(evidenceSufficiencyLevel);
        replayReadinessStatus = DecisionExplorerDtoSupport.valueOrUnknown(replayReadinessStatus);
        candidateOutcomeCount = Math.max(0, candidateOutcomeCount);
        summaryText = DecisionExplorerDtoSupport.valueOrUnknown(summaryText);
        stableSignals = DecisionExplorerDtoSupport.copyOrEmpty(stableSignals);
        reviewSignals = DecisionExplorerDtoSupport.copyOrEmpty(reviewSignals);
        missingEvidenceSignals = DecisionExplorerDtoSupport.copyOrEmpty(missingEvidenceSignals);
        degradedSignals = DecisionExplorerDtoSupport.copyOrEmpty(degradedSignals);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    public static DecisionExplorerShadowPolicySensitivityDiagnosticV1 unknown(String boundaryNote) {
        return new DecisionExplorerShadowPolicySensitivityDiagnosticV1(
                true,
                true,
                DIAGNOSTIC_OBJECT,
                CONTRACT_VERSION,
                LEVEL_UNKNOWN,
                CATEGORY_UNKNOWN,
                0,
                "UNKNOWN",
                "UNKNOWN",
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT,
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN,
                0,
                "Policy-sensitivity diagnostics could not classify the local shadow decision because computed "
                        + "evidence was unavailable; no routing policy or scoring behavior is changed.",
                List.of(),
                List.of(),
                List.of("computed shadow decision-quality evidence was unavailable"),
                List.of(),
                List.of("SHADOW_POLICY_SENSITIVITY_UNKNOWN"),
                List.of(),
                boundaryNote);
    }

    private static String normalizeSensitivityLevel(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case LEVEL_LOW, LEVEL_MEDIUM, LEVEL_HIGH, LEVEL_UNKNOWN -> normalized;
            default -> LEVEL_UNKNOWN;
        };
    }

    private static String normalizeSensitivityCategory(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case CATEGORY_STABLE,
                    CATEGORY_CLOSE_ALTERNATIVE,
                    CATEGORY_MISSING_EVIDENCE,
                    CATEGORY_DEGRADED_EVIDENCE,
                    CATEGORY_UNKNOWN -> normalized;
            default -> CATEGORY_UNKNOWN;
        };
    }
}
