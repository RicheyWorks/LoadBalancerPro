package com.richmond423.loadbalancerpro.api;

import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticFingerprintSupport.canonicalInputs;
import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticFingerprintSupport.diagnosticFingerprint;
import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticFingerprintSupport.fingerprintValue;
import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticFingerprintSupport.input;
import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticListSupport.copyNonNull;

import java.util.ArrayList;
import java.util.List;

final class DecisionExplorerRouteTradeoffFingerprintBuilder {
    Result build(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerRouteTradeoffRowV1> alternatives,
            DecisionExplorerRouteTradeoffRowV1 closestAlternative,
            String tradeoffCategory,
            DecisionExplorerEvidenceSufficiencyV1 evidenceSufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadinessDiagnostic,
            List<String> tradeoffReasons,
            List<String> warnings,
            List<String> unknowns,
            List<String> sourceReferenceIds) {
        List<String> fingerprintInputs = routeTradeoffFingerprintInputs(
                confidenceSummary,
                rows,
                alternatives,
                closestAlternative,
                tradeoffCategory,
                evidenceSufficiency,
                replayReadinessDiagnostic,
                tradeoffReasons,
                warnings,
                unknowns,
                sourceReferenceIds);
        return new Result(
                fingerprintInputs,
                diagnosticFingerprint("route-tradeoff|v1", fingerprintInputs),
                routeTradeoffReproducibilityKey(
                        confidenceSummary,
                        rows,
                        alternatives,
                        tradeoffCategory,
                        evidenceSufficiency,
                        replayReadinessDiagnostic));
    }

    private static List<String> routeTradeoffFingerprintInputs(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerRouteTradeoffRowV1> alternatives,
            DecisionExplorerRouteTradeoffRowV1 closestAlternative,
            String tradeoffCategory,
            DecisionExplorerEvidenceSufficiencyV1 evidenceSufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadinessDiagnostic,
            List<String> tradeoffReasons,
            List<String> warnings,
            List<String> unknowns,
            List<String> sourceReferenceIds) {
        List<String> inputs = new ArrayList<>();
        inputs.add(input("analysisObject", DecisionExplorerRouteTradeoffAnalysisV1.ANALYSIS_OBJECT));
        inputs.add(input("contractVersion", DecisionExplorerRouteTradeoffAnalysisV1.CONTRACT_VERSION));
        inputs.add(input("overallStatus", confidenceSummary == null ? null : confidenceSummary.status()));
        inputs.add(input("evidenceQuality", confidenceSummary == null ? null : confidenceSummary.evidenceQuality()));
        inputs.add(input("selectedCandidateId",
                confidenceSummary == null ? null : confidenceSummary.selectedCandidateId()));
        inputs.add(input("tradeoffCategory", tradeoffCategory));
        inputs.add(input("candidateTradeoffCount", copyNonNull(rows).size()));
        inputs.add(input("alternativeCount", copyNonNull(alternatives).size()));
        inputs.add(input("comparedAlternativeCount", comparedAlternativeCount(copyNonNull(alternatives))));
        inputs.add(input("closestAlternativeCandidateId",
                closestAlternative == null ? "UNKNOWN" : closestAlternative.candidateId()));
        inputs.add(input("closestAlternativeScoreDelta",
                closestAlternative == null ? null : closestAlternative.scoreDeltaFromSelected()));
        copyNonNull(rows).forEach(row -> inputs.add(input("candidateTradeoff", tradeoffRowFingerprint(row))));
        copyNonNull(evidenceSufficiency == null ? null : evidenceSufficiency.fingerprintInputs()).stream()
                .map(value -> "evidenceSufficiency." + value)
                .forEach(inputs::add);
        copyNonNull(replayReadinessDiagnostic == null ? null : replayReadinessDiagnostic.fingerprintInputs())
                .stream()
                .map(value -> "replayReadiness." + value)
                .forEach(inputs::add);
        inputs.add(input("tradeoffReasons", tradeoffReasons));
        inputs.add(input("warnings", warnings));
        inputs.add(input("unknowns", unknowns));
        inputs.add(input("sourceReferenceIds", sourceReferenceIds));
        return canonicalInputs(inputs);
    }

    private static int comparedAlternativeCount(List<DecisionExplorerRouteTradeoffRowV1> alternatives) {
        return (int) alternatives.stream()
                .filter(row -> row.scoreDeltaFromSelected() != null)
                .count();
    }

    private static String routeTradeoffReproducibilityKey(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerRouteTradeoffRowV1> alternatives,
            String tradeoffCategory,
            DecisionExplorerEvidenceSufficiencyV1 evidenceSufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadinessDiagnostic) {
        return String.join(":",
                "route-tradeoff",
                DecisionExplorerRouteTradeoffAnalysisV1.CONTRACT_VERSION,
                fingerprintValue(confidenceSummary == null ? null : confidenceSummary.status()),
                fingerprintValue(confidenceSummary == null ? null : confidenceSummary.selectedCandidateId()),
                fingerprintValue(tradeoffCategory),
                "rows=" + copyNonNull(rows).size(),
                "alternatives=" + copyNonNull(alternatives).size(),
                "sufficiency=" + fingerprintValue(
                        evidenceSufficiency == null ? null : evidenceSufficiency.sufficiencyLevel()),
                "replay=" + fingerprintValue(
                        replayReadinessDiagnostic == null ? null : replayReadinessDiagnostic.readinessStatus()));
    }

    private static String tradeoffRowFingerprint(DecisionExplorerRouteTradeoffRowV1 row) {
        return String.join(",",
                "candidate=" + fingerprintValue(row.candidateId()),
                "selected=" + row.selected(),
                "category=" + fingerprintValue(row.tradeoffCategory()),
                "classification=" + fingerprintValue(row.riskBenefitClassification()),
                "status=" + fingerprintValue(row.diagnosticStatus()),
                "risk=" + fingerprintValue(row.riskLevel()),
                "health=" + fingerprintValue(row.healthEvidenceState()),
                "finalScore=" + fingerprintValue(row.finalScore()),
                "delta=" + fingerprintValue(row.scoreDeltaFromSelected()),
                "gap=" + fingerprintValue(row.scoreGapCategory()),
                "reasons=" + fingerprintValue(row.reasonCodes()));
    }

    record Result(
            List<String> fingerprintInputs,
            String diagnosticFingerprint,
            String reproducibilityKey) {
        Result {
            fingerprintInputs = DecisionExplorerDtoSupport.copyOrEmpty(fingerprintInputs);
            diagnosticFingerprint = DecisionExplorerDtoSupport.valueOrUnknown(diagnosticFingerprint);
            reproducibilityKey = DecisionExplorerDtoSupport.valueOrUnknown(reproducibilityKey);
        }
    }
}
