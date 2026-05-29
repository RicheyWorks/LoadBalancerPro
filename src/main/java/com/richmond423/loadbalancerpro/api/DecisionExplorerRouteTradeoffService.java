package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DecisionExplorerRouteTradeoffService {
    public static final String FINGERPRINT_ALGORITHM = "stable-field-concat-v1";

    private final DecisionExplorerRouteTradeoffRowBuilder rowBuilder =
            new DecisionExplorerRouteTradeoffRowBuilder();
    private final DecisionExplorerCandidateTradeoffScoringBuilder scoringBuilder =
            new DecisionExplorerCandidateTradeoffScoringBuilder();
    private final DecisionExplorerFactorTradeoffDeltaBuilder factorDeltaBuilder =
            new DecisionExplorerFactorTradeoffDeltaBuilder();
    private final DecisionExplorerEvidenceSufficiencyEvaluator evidenceSufficiencyEvaluator =
            new DecisionExplorerEvidenceSufficiencyEvaluator();
    private final DecisionExplorerReplayReadinessEvaluator replayReadinessEvaluator =
            new DecisionExplorerReplayReadinessEvaluator();

    public DecisionExplorerRouteTradeoffAnalysisV1 buildTradeoffs(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            String boundaryNote) {
        if (confidenceSummary == null || routingDiagnostics == null) {
            return DecisionExplorerRouteTradeoffAnalysisV1.unknown(boundaryNote);
        }

        List<DecisionExplorerRouteTradeoffRowV1> rows = rowBuilder.build(routingDiagnostics, boundaryNote);
        List<DecisionExplorerRouteTradeoffRowV1> alternatives = rows.stream()
                .filter(row -> !row.selected())
                .toList();
        DecisionExplorerRouteTradeoffRowV1 closestAlternative = closestAlternative(alternatives);
        String tradeoffCategory = tradeoffCategory(confidenceSummary, alternatives);
        List<DecisionExplorerCandidateTradeoffScoringExplanationV1> scoringExplanations =
                scoringBuilder.build(rows, routingDiagnostics.factorDiagnostics(), boundaryNote);
        List<DecisionExplorerFactorTradeoffDeltaV1> factorTradeoffDeltas =
                factorDeltaBuilder.build(rows, routingDiagnostics.factorDiagnostics(), boundaryNote);
        DecisionExplorerEvidenceSufficiencyV1 evidenceSufficiency = evidenceSufficiencyEvaluator.build(
                confidenceSummary,
                routingDiagnostics,
                rows,
                scoringExplanations,
                factorTradeoffDeltas,
                boundaryNote);
        DecisionExplorerReplayReadinessDiagnosticV1 replayReadinessDiagnostic = replayReadinessEvaluator.build(
                evidenceSufficiency,
                rows,
                scoringExplanations,
                factorTradeoffDeltas,
                routingDiagnostics,
                boundaryNote);
        List<String> tradeoffReasons = tradeoffReasons(confidenceSummary, routingDiagnostics, rows, tradeoffCategory);
        List<String> warnings = distinctSorted(routingDiagnostics.warnings());
        List<String> unknowns = tradeoffUnknowns(routingDiagnostics, rows);
        List<String> sourceReferenceIds = distinctSorted(routingDiagnostics.sourceReferenceIds());
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
        String diagnosticFingerprint = diagnosticFingerprint("route-tradeoff|v1", fingerprintInputs);
        String reproducibilityKey = routeTradeoffReproducibilityKey(
                confidenceSummary,
                rows,
                alternatives,
                tradeoffCategory,
                evidenceSufficiency,
                replayReadinessDiagnostic);
        String explanationText = routeTradeoffExplanationText(
                confidenceSummary,
                closestAlternative,
                tradeoffCategory,
                evidenceSufficiency,
                replayReadinessDiagnostic,
                tradeoffReasons,
                warnings,
                unknowns,
                reproducibilityKey);

        return new DecisionExplorerRouteTradeoffAnalysisV1(
                true,
                true,
                DecisionExplorerRouteTradeoffAnalysisV1.ANALYSIS_OBJECT,
                DecisionExplorerRouteTradeoffAnalysisV1.CONTRACT_VERSION,
                confidenceSummary.status(),
                confidenceSummary.evidenceQuality(),
                confidenceSummary.selectedCandidateId(),
                tradeoffCategory,
                selectedSummary(routingDiagnostics.selectedCandidateDiagnostic(), rows),
                alternativeSummary(alternatives, closestAlternative, tradeoffCategory),
                rows.size(),
                alternatives.size(),
                comparedAlternativeCount(alternatives),
                closestAlternative == null ? "UNKNOWN" : closestAlternative.candidateId(),
                closestAlternative == null ? null : closestAlternative.scoreDeltaFromSelected(),
                rows,
                scoringExplanations,
                factorTradeoffDeltas,
                evidenceSufficiency,
                replayReadinessDiagnostic,
                FINGERPRINT_ALGORITHM,
                diagnosticFingerprint,
                reproducibilityKey,
                explanationText,
                fingerprintInputs,
                tradeoffReasons,
                warnings,
                unknowns,
                sourceReferenceIds,
                boundaryNote);
    }

    private static String tradeoffCategory(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            List<DecisionExplorerRouteTradeoffRowV1> alternatives) {
        if (DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(confidenceSummary.status())) {
            return "DEGRADED";
        }
        if (DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN.equals(confidenceSummary.status())) {
            return "UNKNOWN";
        }
        if (alternatives.isEmpty()) {
            return "NO_ALTERNATIVE";
        }
        if (alternatives.stream().anyMatch(row -> DecisionExplorerRouteTradeoffRowV1
                .TRADEOFF_ALTERNATIVE_BEATS_SELECTED.equals(row.tradeoffCategory()))) {
            return "SELECTED_CHALLENGED";
        }
        if (alternatives.stream().anyMatch(row -> DecisionExplorerRouteTradeoffRowV1
                .TRADEOFF_ALTERNATIVE_CLOSE.equals(row.tradeoffCategory()))) {
            return "CLOSE_ALTERNATIVE";
        }
        if (alternatives.stream().anyMatch(row -> DecisionExplorerRouteTradeoffRowV1
                .TRADEOFF_ALTERNATIVE_UNKNOWN.equals(row.tradeoffCategory()))) {
            return "PARTIAL_TRADEOFF";
        }
        if (alternatives.stream().allMatch(row -> DecisionExplorerRouteTradeoffRowV1
                .TRADEOFF_ALTERNATIVE_TRAILS_SELECTED.equals(row.tradeoffCategory()))) {
            return "SELECTED_ADVANTAGE";
        }
        return "UNKNOWN";
    }

    private static DecisionExplorerRouteTradeoffRowV1 closestAlternative(
            List<DecisionExplorerRouteTradeoffRowV1> alternatives) {
        return alternatives.stream()
                .filter(row -> row.scoreDeltaFromSelected() != null)
                .min(Comparator
                        .comparingDouble((DecisionExplorerRouteTradeoffRowV1 row) ->
                                Math.abs(row.scoreDeltaFromSelected()))
                        .thenComparingInt(row -> row.displayOrder() > 0 ? row.displayOrder() : Integer.MAX_VALUE)
                        .thenComparing(DecisionExplorerRouteTradeoffRowV1::candidateId))
                .orElse(null);
    }

    private static int comparedAlternativeCount(List<DecisionExplorerRouteTradeoffRowV1> alternatives) {
        return (int) alternatives.stream()
                .filter(row -> row.scoreDeltaFromSelected() != null)
                .count();
    }

    private static String selectedSummary(
            DecisionExplorerCandidateDiagnosticV1 selectedDiagnostic,
            List<DecisionExplorerRouteTradeoffRowV1> rows) {
        DecisionExplorerRouteTradeoffRowV1 selectedRow = rows.stream()
                .filter(DecisionExplorerRouteTradeoffRowV1::selected)
                .findFirst()
                .orElse(null);
        if (selectedRow == null) {
            return "Selected candidate tradeoff baseline was unavailable.";
        }
        String diagnosticSummary = selectedDiagnostic == null
                ? "selected candidate diagnostic summary was unavailable"
                : DecisionExplorerDtoSupport.valueOrUnknown(selectedDiagnostic.summaryText());
        return "Selected candidate " + selectedRow.candidateId() + " is the tradeoff baseline with "
                + selectedRow.diagnosticStatus() + " diagnostics, " + selectedRow.riskLevel()
                + " risk, and score gap category " + selectedRow.scoreGapCategory() + ". "
                + diagnosticSummary;
    }

    private static String alternativeSummary(
            List<DecisionExplorerRouteTradeoffRowV1> alternatives,
            DecisionExplorerRouteTradeoffRowV1 closestAlternative,
            String tradeoffCategory) {
        if (alternatives.isEmpty()) {
            return "No alternative candidate diagnostics were available for route tradeoff analysis.";
        }
        String closest = closestAlternative == null
                ? "no score-comparable closest alternative"
                : "closest alternative " + closestAlternative.candidateId() + " with score delta "
                        + closestAlternative.scoreDeltaFromSelected();
        return alternatives.size() + " alternative candidate(s) were analyzed; " + closest
                + "; route tradeoff category is " + tradeoffCategory + ".";
    }

    private static List<String> tradeoffReasons(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            String tradeoffCategory) {
        List<String> reasons = new ArrayList<>();
        reasons.add("ROUTE_TRADEOFF_CATEGORY_" + tradeoffCategory);
        reasons.add("SELECTED_CANDIDATE_" + confidenceSummary.selectedCandidateId());
        reasons.add("ROUTE_TRADEOFF_ROW_COUNT_" + rows.size());
        reasons.addAll(confidenceSummary.statusReasons());
        reasons.addAll(routingDiagnostics.diagnosticReasons());
        rows.stream()
                .flatMap(row -> row.reasonCodes().stream())
                .forEach(reasons::add);
        return distinctSorted(reasons);
    }

    private static List<String> tradeoffUnknowns(
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            List<DecisionExplorerRouteTradeoffRowV1> rows) {
        List<String> unknowns = new ArrayList<>(routingDiagnostics.unknowns());
        rows.stream()
                .flatMap(row -> row.unknownSignals().stream())
                .forEach(unknowns::add);
        return distinctSorted(unknowns);
    }

    private static String routeTradeoffExplanationText(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRouteTradeoffRowV1 closestAlternative,
            String tradeoffCategory,
            DecisionExplorerEvidenceSufficiencyV1 evidenceSufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadinessDiagnostic,
            List<String> tradeoffReasons,
            List<String> warnings,
            List<String> unknowns,
            String reproducibilityKey) {
        String alternativeText = closestAlternative == null
                ? "no score-comparable alternative was returned"
                : "closest alternative " + closestAlternative.candidateId() + " has score delta "
                        + DecisionExplorerDtoSupport.valueOrUnknown(
                                closestAlternative.scoreDeltaFromSelected() == null
                                        ? null
                                        : closestAlternative.scoreDeltaFromSelected().toString());
        String primaryReason = firstOrUnknown(tradeoffReasons);
        return "Route tradeoff explanation: selected candidate "
                + DecisionExplorerDtoSupport.valueOrUnknown(confidenceSummary.selectedCandidateId())
                + " is " + DecisionExplorerDtoSupport.valueOrUnknown(confidenceSummary.status())
                + " with category " + DecisionExplorerDtoSupport.valueOrUnknown(tradeoffCategory)
                + "; " + alternativeText
                + "; evidence sufficiency " + DecisionExplorerDtoSupport.valueOrUnknown(
                        evidenceSufficiency.sufficiencyLevel())
                + " with readiness score " + evidenceSufficiency.readinessScore()
                + "; replay readiness " + DecisionExplorerDtoSupport.valueOrUnknown(
                        replayReadinessDiagnostic.readinessStatus())
                + " with replay execution "
                + (replayReadinessDiagnostic.replayExecutionAvailable() ? "available" : "unavailable")
                + "; primary reason " + primaryReason
                + "; warnings " + copyNonNull(warnings).size()
                + "; unknowns " + copyNonNull(unknowns).size()
                + "; reproducibility key "
                + DecisionExplorerDtoSupport.valueOrUnknown(reproducibilityKey) + ".";
    }

    private static String firstOrUnknown(List<String> values) {
        return copyNonNull(values).stream()
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("UNKNOWN");
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
        inputs.add(input("overallStatus", confidenceSummary.status()));
        inputs.add(input("evidenceQuality", confidenceSummary.evidenceQuality()));
        inputs.add(input("selectedCandidateId", confidenceSummary.selectedCandidateId()));
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
                fingerprintValue(confidenceSummary.status()),
                fingerprintValue(confidenceSummary.selectedCandidateId()),
                fingerprintValue(tradeoffCategory),
                "rows=" + copyNonNull(rows).size(),
                "alternatives=" + copyNonNull(alternatives).size(),
                "sufficiency=" + fingerprintValue(evidenceSufficiency.sufficiencyLevel()),
                "replay=" + fingerprintValue(replayReadinessDiagnostic.readinessStatus()));
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
                .map(DecisionExplorerRouteTradeoffService::fingerprintValue)
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
                    .map(DecisionExplorerRouteTradeoffService::fingerprintValue)
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

    private static List<String> distinctSorted(Collection<String> values) {
        if (values == null) {
            return List.of();
        }
        Set<String> distinct = new LinkedHashSet<>();
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(distinct::add);
        return distinct.stream().sorted().toList();
    }
}
