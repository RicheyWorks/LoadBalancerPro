package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

final class DecisionExplorerShadowQualityFingerprintBuilder {
    List<String> fingerprintInputs(
            String qualityLabel,
            int qualityScore,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRoutingDiagnosticsV1 diagnostics,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity,
            DecisionExplorerShadowScenarioInputQualityV1 scenarioInputQuality,
            List<String> evidenceBasis,
            List<String> selectedCandidateBasis,
            List<String> qualityReasons,
            List<String> warnings,
            List<String> unknowns,
            List<String> sourceReferenceIds) {
        List<String> inputs = new ArrayList<>();
        inputs.add(input("evaluationObject", DecisionExplorerShadowDecisionQualityEvaluationV1.EVALUATION_OBJECT));
        inputs.add(input("contractVersion", DecisionExplorerShadowDecisionQualityEvaluationV1.CONTRACT_VERSION));
        inputs.add(input("qualityLabel", qualityLabel));
        inputs.add(input("qualityBand", DecisionExplorerShadowDecisionQualityEvaluationV1.bandFor(qualityLabel)));
        inputs.add(input("qualityScore", qualityScore));
        inputs.add(input("selectedCandidateId", summary.selectedCandidateId()));
        inputs.add(input("confidenceStatus", summary.status()));
        inputs.add(input("evidenceQuality", summary.evidenceQuality()));
        inputs.add(input("diagnosticsStatus", diagnostics.overallStatus()));
        inputs.add(input("tradeoffCategory", tradeoff.tradeoffCategory()));
        inputs.add(input("evidenceSufficiencyLevel", sufficiency.sufficiencyLevel()));
        inputs.add(input("replayReadinessStatus", replayReadiness.readinessStatus()));
        inputs.add(input("candidateOutcomeCount", copyNonNull(candidateOutcomes).size()));
        copyNonNull(candidateOutcomes)
                .forEach(row -> inputs.add(input("candidateOutcome", candidateOutcomeFingerprint(row))));
        inputs.add(input("policySensitivity", policySensitivityFingerprint(policySensitivity)));
        inputs.add(input("scenarioInputQuality", scenarioInputQualityFingerprint(scenarioInputQuality)));
        inputs.add(input("evidenceBasis", evidenceBasis));
        inputs.add(input("selectedCandidateBasis", selectedCandidateBasis));
        inputs.add(input("qualityReasons", qualityReasons));
        inputs.add(input("warnings", warnings));
        inputs.add(input("unknowns", unknowns));
        inputs.add(input("sourceReferenceIds", sourceReferenceIds));
        inputs.add(input("routeTradeoffFingerprint", tradeoff.diagnosticFingerprint()));
        inputs.add(input("evidenceSufficiencyFingerprint", sufficiency.diagnosticFingerprint()));
        inputs.add(input("replayReadinessFingerprint", replayReadiness.diagnosticFingerprint()));
        return canonicalInputs(inputs);
    }

    String reproducibilityKey(
            String qualityLabel,
            DecisionExplorerConfidenceSummaryV1 summary,
            DecisionExplorerRouteTradeoffAnalysisV1 tradeoff,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadiness,
            List<DecisionExplorerShadowCandidateOutcomeV1> candidateOutcomes,
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity,
            DecisionExplorerShadowScenarioInputQualityV1 scenarioInputQuality) {
        return String.join(":",
                "shadow-decision-quality",
                DecisionExplorerShadowDecisionQualityEvaluationV1.CONTRACT_VERSION,
                fingerprintValue(qualityLabel),
                fingerprintValue(summary.selectedCandidateId()),
                fingerprintValue(tradeoff.tradeoffCategory()),
                "outcomes=" + copyNonNull(candidateOutcomes).size(),
                "policy=" + fingerprintValue(policySensitivity.sensitivityLevel()),
                "scenario=" + fingerprintValue(scenarioInputQuality.inputQualityLabel()),
                "sufficiency=" + fingerprintValue(sufficiency.sufficiencyLevel()),
                "replay=" + fingerprintValue(replayReadiness.readinessStatus()));
    }

    String diagnosticFingerprint(String namespace, List<String> inputs) {
        List<String> canonicalInputs = canonicalInputs(inputs);
        String safeNamespace = namespace == null || namespace.isBlank()
                ? "diagnostic|v1"
                : namespace.trim().replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ");
        if (canonicalInputs.isEmpty()) {
            return safeNamespace + "|inputs=none";
        }
        return safeNamespace + "|" + String.join("|", canonicalInputs);
    }

    private static String candidateOutcomeFingerprint(DecisionExplorerShadowCandidateOutcomeV1 row) {
        return String.join(",",
                "candidate=" + fingerprintValue(row.candidateId()),
                "selected=" + row.selected(),
                "order=" + row.displayOrder(),
                "outcome=" + fingerprintValue(row.outcomeLabel()),
                "impact=" + fingerprintValue(row.qualityImpact()),
                "tradeoff=" + fingerprintValue(row.tradeoffCategory()),
                "riskBenefit=" + fingerprintValue(row.riskBenefitClassification()),
                "status=" + fingerprintValue(row.diagnosticStatus()),
                "risk=" + fingerprintValue(row.riskLevel()),
                "gap=" + fingerprintValue(row.scoreGapCategory()),
                "finalScore=" + fingerprintValue(row.finalScore()),
                "delta=" + fingerprintValue(row.scoreDeltaFromSelected()),
                "reasons=" + fingerprintValue(row.reasonCodes()));
    }

    private static String policySensitivityFingerprint(
            DecisionExplorerShadowPolicySensitivityDiagnosticV1 policySensitivity) {
        return String.join(",",
                "level=" + fingerprintValue(policySensitivity.sensitivityLevel()),
                "category=" + fingerprintValue(policySensitivity.sensitivityCategory()),
                "score=" + policySensitivity.sensitivityScore(),
                "candidateOutcomes=" + policySensitivity.candidateOutcomeCount(),
                "stable=" + fingerprintValue(policySensitivity.stableSignals()),
                "review=" + fingerprintValue(policySensitivity.reviewSignals()),
                "missing=" + fingerprintValue(policySensitivity.missingEvidenceSignals()),
                "degraded=" + fingerprintValue(policySensitivity.degradedSignals()),
                "reasons=" + fingerprintValue(policySensitivity.reasonCodes()));
    }

    private static String scenarioInputQualityFingerprint(
            DecisionExplorerShadowScenarioInputQualityV1 scenarioInputQuality) {
        return String.join(",",
                "label=" + fingerprintValue(scenarioInputQuality.inputQualityLabel()),
                "band=" + fingerprintValue(scenarioInputQuality.supportBand()),
                "score=" + scenarioInputQuality.inputQualityScore(),
                "candidates=" + scenarioInputQuality.candidateEvidenceCount(),
                "factors=" + scenarioInputQuality.factorEvidenceCount(),
                "partial=" + scenarioInputQuality.partialSignalCount(),
                "missing=" + scenarioInputQuality.missingSignalCount(),
                "degraded=" + scenarioInputQuality.degradedSignalCount(),
                "candidateSignals=" + fingerprintValue(scenarioInputQuality.candidateInputSignals()),
                "factorSignals=" + fingerprintValue(scenarioInputQuality.factorInputSignals()),
                "partialSignals=" + fingerprintValue(scenarioInputQuality.partialInputSignals()),
                "missingSignals=" + fingerprintValue(scenarioInputQuality.missingInputSignals()),
                "degradedSignals=" + fingerprintValue(scenarioInputQuality.degradedInputSignals()),
                "reasons=" + fingerprintValue(scenarioInputQuality.reasonCodes()));
    }

    private static String input(String key, Object value) {
        return fingerprintValue(key) + "=" + fingerprintValue(value);
    }

    private static List<String> canonicalInputs(Collection<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(DecisionExplorerShadowQualityFingerprintBuilder::fingerprintValue)
                .toList();
    }

    private static String fingerprintValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                return "[]";
            }
            return collection.stream()
                    .map(DecisionExplorerShadowQualityFingerprintBuilder::fingerprintValue)
                    .sorted()
                    .collect(Collectors.joining(";"));
        }
        if (value instanceof Double number && !Double.isFinite(number)) {
            return "null";
        }
        return String.valueOf(value)
                .trim()
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('|', '/')
                .replaceAll("\\s+", " ");
    }

    private static <T> List<T> copyNonNull(List<T> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }
}
