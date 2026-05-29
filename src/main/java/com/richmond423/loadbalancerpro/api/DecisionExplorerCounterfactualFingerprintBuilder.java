package com.richmond423.loadbalancerpro.api;

import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticFingerprintSupport.input;

import java.util.List;

final class DecisionExplorerCounterfactualFingerprintBuilder {
    FingerprintResult build(
            String counterfactualLabel,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> policyWeightScenarios,
            List<DecisionExplorerCounterfactualCandidateOutcomeV1> counterfactualCandidateOutcomes,
            List<DecisionExplorerCounterfactualFactorWeightDeltaV1> factorWeightDeltas,
            List<String> stableSignals,
            List<String> sensitivitySignals,
            List<String> limitationSignals,
            List<String> reasonCodes,
            List<String> warnings,
            List<String> unknowns,
            List<String> sourceReferenceIds) {
        List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> scenarios =
                DecisionExplorerDiagnosticListSupport.copyNonNull(policyWeightScenarios);
        List<DecisionExplorerCounterfactualCandidateOutcomeV1> outcomes =
                DecisionExplorerDiagnosticListSupport.copyNonNull(counterfactualCandidateOutcomes);
        List<DecisionExplorerCounterfactualFactorWeightDeltaV1> factorDeltas =
                DecisionExplorerDiagnosticListSupport.copyNonNull(factorWeightDeltas);
        List<String> fingerprintInputs = fingerprintInputs(
                counterfactualLabel,
                summary,
                tradeoff,
                quality,
                sufficiency,
                replayReadiness,
                scenarios,
                outcomes,
                factorDeltas,
                stableSignals,
                sensitivitySignals,
                limitationSignals,
                reasonCodes,
                warnings,
                unknowns,
                sourceReferenceIds);
        return new FingerprintResult(
                fingerprintInputs,
                DecisionExplorerDiagnosticFingerprintSupport.diagnosticFingerprint(
                        DecisionExplorerCounterfactualAnalysisV1.FINGERPRINT_NAMESPACE,
                        fingerprintInputs),
                reproducibilityKey(
                        counterfactualLabel,
                        summary,
                        tradeoff,
                        quality,
                        sufficiency,
                        replayReadiness,
                        scenarios,
                        outcomes,
                        factorDeltas));
    }

    private static List<String> fingerprintInputs(
            String counterfactualLabel,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> policyWeightScenarios,
            List<DecisionExplorerCounterfactualCandidateOutcomeV1> counterfactualCandidateOutcomes,
            List<DecisionExplorerCounterfactualFactorWeightDeltaV1> factorWeightDeltas,
            List<String> stableSignals,
            List<String> sensitivitySignals,
            List<String> limitationSignals,
            List<String> reasonCodes,
            List<String> warnings,
            List<String> unknowns,
            List<String> sourceReferenceIds) {
        return List.of(
                input("analysisObject", DecisionExplorerCounterfactualAnalysisV1.ANALYSIS_OBJECT),
                input("contractVersion", DecisionExplorerCounterfactualAnalysisV1.CONTRACT_VERSION),
                input("counterfactualLabel", counterfactualLabel),
                input("sensitivityBand", DecisionExplorerCounterfactualAnalysisV1.bandFor(counterfactualLabel)),
                input("selectedCandidateId", summary.selectedCandidateId()),
                input("confidenceStatus", summary.status()),
                input("decisionQualityLabel", quality.qualityLabel()),
                input("tradeoffCategory", tradeoff.tradeoffCategory()),
                input("evidenceSufficiencyLevel", sufficiency.sufficiencyLevel()),
                input("replayReadinessStatus", replayReadiness.readinessStatus()),
                input("policyWeightScenarioCount", policyWeightScenarios.size()),
                input("policyWeightScenarios", policyWeightScenarios.stream()
                        .map(DecisionExplorerCounterfactualPolicyWeightScenarioV1::fingerprintInput)
                        .toList()),
                input("counterfactualCandidateOutcomeCount", counterfactualCandidateOutcomes.size()),
                input("counterfactualCandidateOutcomes", counterfactualCandidateOutcomes.stream()
                        .map(DecisionExplorerCounterfactualCandidateOutcomeV1::fingerprintInput)
                        .toList()),
                input("factorWeightDeltaCount", factorWeightDeltas.size()),
                input("factorWeightDeltas", factorWeightDeltas.stream()
                        .map(DecisionExplorerCounterfactualFactorWeightDeltaV1::fingerprintInput)
                        .toList()),
                input("candidateOutcomeCount", quality.candidateOutcomeCount()),
                input("factorDeltaCount", tradeoff.factorTradeoffDeltas().size()),
                input("stableSignals", stableSignals),
                input("sensitivitySignals", sensitivitySignals),
                input("limitationSignals", limitationSignals),
                input("reasonCodes", reasonCodes),
                input("warnings", warnings),
                input("unknowns", unknowns),
                input("sourceReferenceIds", sourceReferenceIds));
    }

    private static String reproducibilityKey(
            String counterfactualLabel,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerShadowDecisionQualityEvaluationV1 quality,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerCounterfactualPolicyWeightScenarioV1> policyWeightScenarios,
            List<DecisionExplorerCounterfactualCandidateOutcomeV1> counterfactualCandidateOutcomes,
            List<DecisionExplorerCounterfactualFactorWeightDeltaV1> factorWeightDeltas) {
        return "counterfactual:v1:" + counterfactualLabel + ":" + summary.selectedCandidateId()
                + ":" + tradeoff.tradeoffCategory()
                + ":quality=" + quality.qualityLabel()
                + ":sufficiency=" + sufficiency.sufficiencyLevel()
                + ":replay=" + replayReadiness.readinessStatus()
                + ":scenarios=" + policyWeightScenarios.size()
                + ":outcomes=" + counterfactualCandidateOutcomes.size()
                + ":factorWeightDeltas=" + factorWeightDeltas.size();
    }

    record FingerprintResult(
            List<String> fingerprintInputs,
            String diagnosticFingerprint,
            String reproducibilityKey) {
        FingerprintResult {
            fingerprintInputs = DecisionExplorerDtoSupport.copyOrEmpty(fingerprintInputs);
            diagnosticFingerprint = DecisionExplorerDtoSupport.valueOrUnknown(diagnosticFingerprint);
            reproducibilityKey = DecisionExplorerDtoSupport.valueOrUnknown(reproducibilityKey);
        }
    }
}
