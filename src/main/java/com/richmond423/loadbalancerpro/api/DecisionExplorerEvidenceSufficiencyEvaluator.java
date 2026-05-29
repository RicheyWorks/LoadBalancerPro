package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class DecisionExplorerEvidenceSufficiencyEvaluator {
    DecisionExplorerEvidenceSufficiencyV1 build(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerCandidateTradeoffScoringExplanationV1> scoringExplanations,
            List<DecisionExplorerFactorTradeoffDeltaV1> factorTradeoffDeltas,
            String boundaryNote) {
        List<DecisionExplorerRouteTradeoffRowV1> safeRows = copyNonNull(rows);
        List<DecisionExplorerRouteTradeoffRowV1> alternatives = safeRows.stream()
                .filter(row -> !row.selected())
                .toList();
        List<DecisionExplorerCandidateTradeoffScoringExplanationV1> safeScoring =
                copyNonNull(scoringExplanations);
        List<DecisionExplorerFactorTradeoffDeltaV1> safeDeltas = copyNonNull(factorTradeoffDeltas);
        int comparableAlternativeCount = comparedAlternativeCount(alternatives);
        boolean selectedKnown = selectedCandidateKnown(confidenceSummary, safeRows);
        boolean scoreEvidenceComplete = scoreEvidenceComplete(safeScoring);
        boolean factorDeltasPresent = !safeDeltas.isEmpty();
        boolean criticalFactorUnknown = hasUnknownFactorDelta(safeDeltas);
        boolean degradedEvidence = hasDegradedTradeoffEvidence(confidenceSummary, routingDiagnostics, safeRows,
                safeDeltas);

        List<String> presentSignals = presentEvidenceSignals(
                selectedKnown,
                safeRows,
                alternatives,
                comparableAlternativeCount,
                safeScoring,
                factorDeltasPresent,
                routingDiagnostics);
        List<String> partialSignals = partialEvidenceSignals(routingDiagnostics, safeScoring, safeDeltas);
        List<String> missingSignals = missingEvidenceSignals(
                selectedKnown,
                safeRows,
                alternatives,
                comparableAlternativeCount,
                factorDeltasPresent,
                routingDiagnostics);
        List<String> degradedSignals = degradedEvidenceSignals(confidenceSummary, routingDiagnostics, safeRows,
                safeDeltas);
        List<String> unknownSignals = unknownEvidenceSignals(routingDiagnostics, safeRows, safeScoring, safeDeltas);
        boolean replayStyleReady = selectedKnown
                && comparableAlternativeCount > 0
                && scoreEvidenceComplete
                && factorDeltasPresent
                && !criticalFactorUnknown
                && !degradedEvidence;
        boolean tradeoffReady = selectedKnown
                && comparableAlternativeCount > 0
                && scoreEvidenceComplete
                && !degradedEvidence;
        boolean basicReady = selectedKnown || !safeRows.isEmpty() || routingDiagnostics.diagnosticCount() > 0;
        String level = sufficiencyLevel(replayStyleReady, tradeoffReady, basicReady, degradedEvidence);
        List<String> readinessReasons = readinessReasons(
                level,
                selectedKnown,
                comparableAlternativeCount,
                scoreEvidenceComplete,
                safeDeltas.size(),
                degradedEvidence);
        int readinessScore = readinessScore(selectedKnown, safeRows, comparableAlternativeCount,
                scoreEvidenceComplete, factorDeltasPresent, routingDiagnostics, degradedEvidence);
        List<String> sourceReferenceIds = distinctSorted(routingDiagnostics.sourceReferenceIds());
        List<String> fingerprintInputs = fingerprintInputs(
                level,
                readinessScore,
                basicReady,
                tradeoffReady,
                replayStyleReady,
                safeRows.size(),
                comparableAlternativeCount,
                safeDeltas.size(),
                presentSignals,
                partialSignals,
                missingSignals,
                degradedSignals,
                unknownSignals,
                readinessReasons,
                sourceReferenceIds);
        return new DecisionExplorerEvidenceSufficiencyV1(
                true,
                true,
                DecisionExplorerEvidenceSufficiencyV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerEvidenceSufficiencyV1.CONTRACT_VERSION,
                level,
                readinessScore,
                basicReady,
                tradeoffReady,
                replayStyleReady,
                safeRows.size(),
                comparableAlternativeCount,
                safeDeltas.size(),
                presentSignals.size(),
                partialSignals.size(),
                missingSignals.size(),
                degradedSignals.size(),
                unknownSignals.size(),
                presentSignals,
                partialSignals,
                missingSignals,
                degradedSignals,
                unknownSignals,
                readinessReasons,
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                diagnosticFingerprint("evidence-sufficiency|v1", fingerprintInputs),
                reproducibilityKey(level, readinessScore, safeRows.size(), comparableAlternativeCount,
                        safeDeltas.size()),
                fingerprintInputs,
                sourceReferenceIds,
                boundaryNote);
    }

    private static int comparedAlternativeCount(List<DecisionExplorerRouteTradeoffRowV1> alternatives) {
        return (int) alternatives.stream()
                .filter(row -> row.scoreDeltaFromSelected() != null)
                .count();
    }

    private static boolean selectedCandidateKnown(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            List<DecisionExplorerRouteTradeoffRowV1> rows) {
        String selectedCandidateId = confidenceSummary == null ? null : confidenceSummary.selectedCandidateId();
        return selectedCandidateId != null
                && !"UNKNOWN".equals(selectedCandidateId)
                && rows.stream().anyMatch(DecisionExplorerRouteTradeoffRowV1::selected);
    }

    private static boolean scoreEvidenceComplete(
            List<DecisionExplorerCandidateTradeoffScoringExplanationV1> scoringExplanations) {
        return !scoringExplanations.isEmpty()
                && scoringExplanations.stream()
                        .noneMatch(explanation -> explanation.scoreEvidenceState().endsWith("_UNKNOWN"));
    }

    private static boolean hasUnknownFactorDelta(List<DecisionExplorerFactorTradeoffDeltaV1> deltas) {
        return deltas.stream()
                .anyMatch(delta -> DecisionExplorerFactorTradeoffDeltaV1.DELTA_UNKNOWN
                        .equals(delta.deltaClassification()));
    }

    private static boolean hasDegradedTradeoffEvidence(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerFactorTradeoffDeltaV1> deltas) {
        return DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(confidenceSummary.status())
                || routingDiagnostics.degradedEvidenceCount() > 0
                || rows.stream().anyMatch(row -> DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED
                        .equals(row.diagnosticStatus())
                        || !row.degradedSignals().isEmpty())
                || deltas.stream().anyMatch(delta -> DecisionExplorerFactorTradeoffDeltaV1.DELTA_DEGRADED
                        .equals(delta.deltaClassification()));
    }

    private static List<String> presentEvidenceSignals(
            boolean selectedKnown,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerRouteTradeoffRowV1> alternatives,
            int comparableAlternativeCount,
            List<DecisionExplorerCandidateTradeoffScoringExplanationV1> scoringExplanations,
            boolean factorDeltasPresent,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics) {
        List<String> signals = new ArrayList<>();
        if (selectedKnown) {
            signals.add("selected candidate evidence is present");
        }
        if (!rows.isEmpty()) {
            signals.add(rows.size() + " candidate tradeoff row(s) are present");
        }
        if (!alternatives.isEmpty()) {
            signals.add(alternatives.size() + " alternative candidate row(s) are present");
        }
        if (comparableAlternativeCount > 0) {
            signals.add(comparableAlternativeCount + " score-comparable alternative(s) are present");
        }
        if (!scoringExplanations.isEmpty()) {
            signals.add(scoringExplanations.size() + " candidate scoring explanation(s) are present");
        }
        if (factorDeltasPresent) {
            signals.add("factor-level tradeoff deltas are present");
        }
        if (!routingDiagnostics.sourceReferenceIds().isEmpty()) {
            signals.add("source references are present");
        }
        return distinctSorted(signals);
    }

    private static List<String> partialEvidenceSignals(
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            List<DecisionExplorerCandidateTradeoffScoringExplanationV1> scoringExplanations,
            List<DecisionExplorerFactorTradeoffDeltaV1> deltas) {
        List<String> signals = new ArrayList<>(routingDiagnostics.partialEvidenceReasons());
        scoringExplanations.stream()
                .filter(explanation -> explanation.scoreEvidenceState().endsWith("_UNKNOWN"))
                .map(explanation -> "score evidence is partial for candidate " + explanation.candidateId())
                .forEach(signals::add);
        deltas.stream()
                .filter(delta -> DecisionExplorerFactorTradeoffDeltaV1.DELTA_UNKNOWN
                        .equals(delta.deltaClassification()))
                .map(delta -> "factor delta is unknown for " + delta.selectedCandidateId()
                        + " versus " + delta.alternativeCandidateId() + " on " + delta.factorName())
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static List<String> missingEvidenceSignals(
            boolean selectedKnown,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerRouteTradeoffRowV1> alternatives,
            int comparableAlternativeCount,
            boolean factorDeltasPresent,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics) {
        List<String> signals = new ArrayList<>();
        if (!selectedKnown) {
            signals.add("selected candidate evidence was not returned");
        }
        if (rows.isEmpty()) {
            signals.add("candidate tradeoff rows were not returned");
        }
        if (alternatives.isEmpty()) {
            signals.add("alternative candidate evidence was not returned");
        }
        if (comparableAlternativeCount == 0) {
            signals.add("score-comparable alternative evidence was not returned");
        }
        if (!factorDeltasPresent) {
            signals.add("factor-level tradeoff deltas were not returned");
        }
        if (routingDiagnostics.sourceReferenceIds().isEmpty()) {
            signals.add("source reference evidence was not returned");
        }
        return distinctSorted(signals);
    }

    private static List<String> degradedEvidenceSignals(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerFactorTradeoffDeltaV1> deltas) {
        List<String> signals = new ArrayList<>(routingDiagnostics.degradationReasons());
        if (DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED.equals(confidenceSummary.status())) {
            signals.add("overall confidence status is DEGRADED");
        }
        rows.stream()
                .flatMap(row -> row.degradedSignals().stream())
                .forEach(signals::add);
        deltas.stream()
                .filter(delta -> DecisionExplorerFactorTradeoffDeltaV1.DELTA_DEGRADED
                        .equals(delta.deltaClassification()))
                .map(delta -> "degraded factor delta for " + delta.selectedCandidateId()
                        + " versus " + delta.alternativeCandidateId() + " on " + delta.factorName())
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static List<String> unknownEvidenceSignals(
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerCandidateTradeoffScoringExplanationV1> scoringExplanations,
            List<DecisionExplorerFactorTradeoffDeltaV1> deltas) {
        List<String> signals = new ArrayList<>(routingDiagnostics.unknownEvidenceReasons());
        rows.stream()
                .flatMap(row -> row.unknownSignals().stream())
                .forEach(signals::add);
        scoringExplanations.stream()
                .filter(explanation -> explanation.scoreEvidenceState().endsWith("_UNKNOWN"))
                .map(explanation -> "score evidence unknown for candidate " + explanation.candidateId())
                .forEach(signals::add);
        deltas.stream()
                .filter(delta -> DecisionExplorerFactorTradeoffDeltaV1.DELTA_UNKNOWN
                        .equals(delta.deltaClassification()))
                .map(delta -> "factor tradeoff delta unknown for " + delta.factorName())
                .forEach(signals::add);
        return distinctSorted(signals);
    }

    private static String sufficiencyLevel(
            boolean replayStyleReady,
            boolean tradeoffReady,
            boolean basicReady,
            boolean degradedEvidence) {
        if (degradedEvidence) {
            return DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED;
        }
        if (replayStyleReady) {
            return DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY;
        }
        if (tradeoffReady) {
            return DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY;
        }
        if (basicReady) {
            return DecisionExplorerEvidenceSufficiencyV1.LEVEL_BASIC_DIAGNOSTICS_ONLY;
        }
        return DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT;
    }

    private static int readinessScore(
            boolean selectedKnown,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            int comparableAlternativeCount,
            boolean scoreEvidenceComplete,
            boolean factorDeltasPresent,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            boolean degradedEvidence) {
        int score = 0;
        if (selectedKnown) {
            score += 20;
        }
        if (!rows.isEmpty()) {
            score += 20;
        }
        if (comparableAlternativeCount > 0) {
            score += 20;
        }
        if (scoreEvidenceComplete) {
            score += 20;
        }
        if (factorDeltasPresent) {
            score += 10;
        }
        if (!routingDiagnostics.sourceReferenceIds().isEmpty()) {
            score += 10;
        }
        if (degradedEvidence) {
            score -= 25;
        }
        return Math.max(0, Math.min(100, score));
    }

    private static List<String> readinessReasons(
            String level,
            boolean selectedKnown,
            int comparableAlternativeCount,
            boolean scoreEvidenceComplete,
            int factorDeltaCount,
            boolean degradedEvidence) {
        List<String> reasons = new ArrayList<>();
        reasons.add("EVIDENCE_SUFFICIENCY_LEVEL_" + level);
        reasons.add(selectedKnown ? "SELECTED_CANDIDATE_PRESENT" : "SELECTED_CANDIDATE_UNAVAILABLE");
        reasons.add("COMPARABLE_ALTERNATIVES_" + comparableAlternativeCount);
        reasons.add(scoreEvidenceComplete ? "SCORE_EVIDENCE_COMPLETE" : "SCORE_EVIDENCE_PARTIAL_OR_UNKNOWN");
        reasons.add("FACTOR_TRADEOFF_DELTAS_" + factorDeltaCount);
        if (degradedEvidence) {
            reasons.add("DEGRADED_EVIDENCE_PRESENT");
        }
        return distinctSorted(reasons);
    }

    private static List<String> fingerprintInputs(
            String level,
            int readinessScore,
            boolean basicReady,
            boolean tradeoffReady,
            boolean replayStyleReady,
            int candidateEvidenceCount,
            int comparableAlternativeCount,
            int factorDeltaCount,
            List<String> presentSignals,
            List<String> partialSignals,
            List<String> missingSignals,
            List<String> degradedSignals,
            List<String> unknownSignals,
            List<String> readinessReasons,
            List<String> sourceReferenceIds) {
        return canonicalInputs(List.of(
                input("diagnosticObject", DecisionExplorerEvidenceSufficiencyV1.DIAGNOSTIC_OBJECT),
                input("contractVersion", DecisionExplorerEvidenceSufficiencyV1.CONTRACT_VERSION),
                input("sufficiencyLevel", level),
                input("readinessScore", readinessScore),
                input("basicDiagnosticsReady", basicReady),
                input("tradeoffAnalysisReady", tradeoffReady),
                input("replayStyleAnalysisReady", replayStyleReady),
                input("candidateEvidenceCount", candidateEvidenceCount),
                input("comparableAlternativeCount", comparableAlternativeCount),
                input("factorDeltaCount", factorDeltaCount),
                input("presentEvidenceSignals", presentSignals),
                input("partialEvidenceSignals", partialSignals),
                input("missingEvidenceSignals", missingSignals),
                input("degradedEvidenceSignals", degradedSignals),
                input("unknownEvidenceSignals", unknownSignals),
                input("readinessReasons", readinessReasons),
                input("sourceReferenceIds", sourceReferenceIds)));
    }

    private static String reproducibilityKey(
            String level,
            int readinessScore,
            int candidateEvidenceCount,
            int comparableAlternativeCount,
            int factorDeltaCount) {
        return String.join(":",
                "evidence-sufficiency",
                DecisionExplorerEvidenceSufficiencyV1.CONTRACT_VERSION,
                fingerprintValue(level),
                "score=" + readinessScore,
                "candidates=" + candidateEvidenceCount,
                "alternatives=" + comparableAlternativeCount,
                "factorDeltas=" + factorDeltaCount);
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

    private static String input(String key, Object value) {
        return fingerprintValue(key) + "=" + fingerprintValue(value);
    }

    private static List<String> canonicalInputs(Collection<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(DecisionExplorerEvidenceSufficiencyEvaluator::fingerprintValue)
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
                    .map(DecisionExplorerEvidenceSufficiencyEvaluator::fingerprintValue)
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
        Set<String> distinct = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " "))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return distinct.stream()
                .sorted()
                .toList();
    }
}
