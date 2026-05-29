package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerCounterfactualAnalysisV1(
        boolean readOnly,
        boolean simulationOnly,
        boolean localOnly,
        String analysisObject,
        String contractVersion,
        String selectedCandidateId,
        String counterfactualLabel,
        String sensitivityBand,
        String confidenceStatus,
        String decisionQualityLabel,
        String tradeoffCategory,
        String evidenceSufficiencyLevel,
        String replayReadinessStatus,
        String baselinePolicyProfile,
        List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> policyWeightScenarios,
        int policyWeightScenarioCount,
        List<DecisionExplorerCounterfactualCandidateOutcomeV1> counterfactualCandidateOutcomes,
        int counterfactualCandidateOutcomeCount,
        int candidateOutcomeCount,
        int factorDeltaCount,
        String summaryText,
        List<String> stableSignals,
        List<String> sensitivitySignals,
        List<String> limitationSignals,
        List<String> reasonCodes,
        List<String> warnings,
        List<String> unknowns,
        List<String> sourceReferenceIds,
        String fingerprintAlgorithm,
        String diagnosticFingerprint,
        String reproducibilityKey,
        List<String> fingerprintInputs,
        String boundaryNote) {
    public static final String ANALYSIS_OBJECT = "DecisionExplorerCounterfactualAnalysisV1";
    public static final String CONTRACT_VERSION = "v1";
    public static final String FINGERPRINT_NAMESPACE = "counterfactual-analysis|v1";

    public static final String LABEL_STABLE = "STABLE";
    public static final String LABEL_SENSITIVE = "SENSITIVE";
    public static final String LABEL_CLOSE_CALL = "CLOSE_CALL";
    public static final String LABEL_DEGRADED = "DEGRADED";
    public static final String LABEL_INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";
    public static final String LABEL_UNKNOWN = "UNKNOWN";

    public static final String BAND_LOW = "LOW";
    public static final String BAND_MEDIUM = "MEDIUM";
    public static final String BAND_HIGH = "HIGH";
    public static final String BAND_INSUFFICIENT = "INSUFFICIENT";
    public static final String BAND_UNKNOWN = "UNKNOWN";

    public DecisionExplorerCounterfactualAnalysisV1 {
        readOnly = true;
        simulationOnly = true;
        localOnly = true;
        analysisObject = DecisionExplorerDtoSupport.valueOrDefault(analysisObject, ANALYSIS_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateId);
        counterfactualLabel = normalizeLabel(counterfactualLabel);
        sensitivityBand = normalizeBand(sensitivityBand, counterfactualLabel);
        confidenceStatus = normalizeConfidenceStatus(confidenceStatus);
        decisionQualityLabel = normalizeDecisionQualityLabel(decisionQualityLabel);
        tradeoffCategory = DecisionExplorerDtoSupport.valueOrUnknown(tradeoffCategory);
        evidenceSufficiencyLevel = normalizeSufficiencyLevel(evidenceSufficiencyLevel);
        replayReadinessStatus = normalizeReplayReadinessStatus(replayReadinessStatus);
        baselinePolicyProfile = DecisionExplorerDtoSupport.valueOrDefault(
                baselinePolicyProfile,
                "RETURNED_EVIDENCE_WEIGHTS");
        policyWeightScenarios = DecisionExplorerDiagnosticListSupport.copyNonNull(policyWeightScenarios);
        policyWeightScenarioCount = policyWeightScenarios.size();
        counterfactualCandidateOutcomes =
                DecisionExplorerDiagnosticListSupport.copyNonNull(counterfactualCandidateOutcomes);
        counterfactualCandidateOutcomeCount = counterfactualCandidateOutcomes.size();
        candidateOutcomeCount = Math.max(0, candidateOutcomeCount);
        factorDeltaCount = Math.max(0, factorDeltaCount);
        summaryText = DecisionExplorerDtoSupport.valueOrUnknown(summaryText);
        stableSignals = DecisionExplorerDtoSupport.copyOrEmpty(stableSignals);
        sensitivitySignals = DecisionExplorerDtoSupport.copyOrEmpty(sensitivitySignals);
        limitationSignals = DecisionExplorerDtoSupport.copyOrEmpty(limitationSignals);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        warnings = DecisionExplorerDtoSupport.copyOrEmpty(warnings);
        unknowns = DecisionExplorerDtoSupport.copyOrEmpty(unknowns);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        fingerprintAlgorithm = DecisionExplorerDtoSupport.valueOrDefault(
                fingerprintAlgorithm,
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM);
        diagnosticFingerprint = DecisionExplorerDtoSupport.valueOrUnknown(diagnosticFingerprint);
        reproducibilityKey = DecisionExplorerDtoSupport.valueOrUnknown(reproducibilityKey);
        fingerprintInputs = DecisionExplorerDtoSupport.copyOrEmpty(fingerprintInputs);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    public static DecisionExplorerCounterfactualAnalysisV1 unknown(String boundaryNote) {
        List<String> fingerprintInputs = List.of(
                "analysisObject=" + ANALYSIS_OBJECT,
                "contractVersion=" + CONTRACT_VERSION,
                "counterfactualLabel=UNKNOWN",
                "selectedCandidateId=UNKNOWN",
                "confidenceStatus=UNKNOWN",
                "decisionQualityLabel=UNKNOWN",
                "tradeoffCategory=UNKNOWN",
                "evidenceSufficiencyLevel=INSUFFICIENT",
                "replayReadinessStatus=UNKNOWN",
                "policyWeightScenarios=[]",
                "policyWeightScenarioCount=0",
                "counterfactualCandidateOutcomes=[]",
                "counterfactualCandidateOutcomeCount=0",
                "candidateOutcomeCount=0",
                "factorDeltaCount=0");
        return new DecisionExplorerCounterfactualAnalysisV1(
                true,
                true,
                true,
                ANALYSIS_OBJECT,
                CONTRACT_VERSION,
                "UNKNOWN",
                LABEL_UNKNOWN,
                BAND_UNKNOWN,
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_UNKNOWN,
                "UNKNOWN",
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT,
                DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN,
                "RETURNED_EVIDENCE_WEIGHTS",
                List.of(),
                0,
                List.of(),
                0,
                0,
                0,
                "Counterfactual analysis is UNKNOWN because computed Decision Explorer evidence was unavailable; "
                        + "no production routing or scoring behavior is changed.",
                List.of(),
                List.of(),
                List.of("computed Decision Explorer evidence was unavailable"),
                List.of("COUNTERFACTUAL_ANALYSIS_UNKNOWN"),
                List.of(),
                List.of("counterfactual analysis input evidence was unavailable"),
                List.of(),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                FINGERPRINT_NAMESPACE + "|" + String.join("|", fingerprintInputs),
                "counterfactual:v1:UNKNOWN:UNKNOWN:UNKNOWN:quality=UNKNOWN:sufficiency=INSUFFICIENT:"
                        + "replay=UNKNOWN:scenarios=0:outcomes=0",
                fingerprintInputs,
                boundaryNote);
    }

    static String bandFor(String counterfactualLabel) {
        return switch (normalizeLabel(counterfactualLabel)) {
            case LABEL_STABLE -> BAND_LOW;
            case LABEL_SENSITIVE, LABEL_CLOSE_CALL -> BAND_MEDIUM;
            case LABEL_DEGRADED -> BAND_HIGH;
            case LABEL_INSUFFICIENT_EVIDENCE -> BAND_INSUFFICIENT;
            default -> BAND_UNKNOWN;
        };
    }

    private static String normalizeLabel(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case LABEL_STABLE,
                    LABEL_SENSITIVE,
                    LABEL_CLOSE_CALL,
                    LABEL_DEGRADED,
                    LABEL_INSUFFICIENT_EVIDENCE,
                    LABEL_UNKNOWN -> normalized;
            default -> LABEL_UNKNOWN;
        };
    }

    private static String normalizeBand(String value, String counterfactualLabel) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case BAND_LOW, BAND_MEDIUM, BAND_HIGH, BAND_INSUFFICIENT, BAND_UNKNOWN -> normalized;
            default -> bandFor(counterfactualLabel);
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

    private static String normalizeDecisionQualityLabel(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_ACCEPTABLE,
                    DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_REVIEW_RECOMMENDED,
                    DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_INSUFFICIENT_EVIDENCE,
                    DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_DEGRADED_DECISION,
                    DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_UNKNOWN -> normalized;
            default -> DecisionExplorerShadowDecisionQualityEvaluationV1.LABEL_UNKNOWN;
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
