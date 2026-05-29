package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    private static String diagnosticFingerprint(String namespace, List<String> inputs) {
        List<String> canonicalInputs = canonicalInputs(inputs);
        String safeNamespace = namespace == null || namespace.isBlank()
                ? "diagnostic|v1"
                : namespace.trim().replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ");
        if (canonicalInputs.isEmpty()) {
            return safeNamespace + "|inputs=none";
        }
        return safeNamespace + "|" + String.join("|", canonicalInputs);
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

    private static String input(String key, Object value) {
        return fingerprintValue(key) + "=" + fingerprintValue(value);
    }

    private static List<String> canonicalInputs(Collection<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(DecisionExplorerRouteTradeoffFingerprintBuilder::fingerprintValue)
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
                    .map(DecisionExplorerRouteTradeoffFingerprintBuilder::fingerprintValue)
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
