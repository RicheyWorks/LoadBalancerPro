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
        DecisionExplorerReplayReadinessDiagnosticV1 replayReadinessDiagnostic = replayReadinessDiagnostic(
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

    private static DecisionExplorerReplayReadinessDiagnosticV1 replayReadinessDiagnostic(
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerCandidateTradeoffScoringExplanationV1> scoringExplanations,
            List<DecisionExplorerFactorTradeoffDeltaV1> factorTradeoffDeltas,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            String boundaryNote) {
        List<DecisionExplorerRouteTradeoffRowV1> safeRows = copyNonNull(rows);
        List<DecisionExplorerRouteTradeoffRowV1> alternatives = safeRows.stream()
                .filter(row -> !row.selected())
                .toList();
        List<DecisionExplorerCandidateTradeoffScoringExplanationV1> safeScoring =
                copyNonNull(scoringExplanations);
        List<DecisionExplorerFactorTradeoffDeltaV1> safeDeltas = copyNonNull(factorTradeoffDeltas);
        String candidateEvidenceStatus = candidateReadinessStatus(sufficiency);
        String alternativeEvidenceStatus = alternativeReadinessStatus(alternatives,
                sufficiency.comparableAlternativeCount());
        String scoreEvidenceStatus = scoreReadinessStatus(safeScoring);
        String factorEvidenceStatus = factorReadinessStatus(safeDeltas);
        String fingerprintEvidenceStatus = DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE;
        String readinessStatus = replayReadinessStatus(sufficiency, fingerprintEvidenceStatus);
        List<String> missingSignals = new ArrayList<>(sufficiency.missingEvidenceSignals());
        List<String> incompatibleSignals = List.of(
                "replay execution is intentionally unavailable in this read-only diagnostic",
                "server-side replay storage is intentionally unavailable",
                "server-side replay export is intentionally unavailable");
        List<String> checklist = replayReadinessChecklist(
                candidateEvidenceStatus,
                alternativeEvidenceStatus,
                scoreEvidenceStatus,
                factorEvidenceStatus,
                fingerprintEvidenceStatus);
        List<String> limitationSignals = new ArrayList<>();
        limitationSignals.addAll(sufficiency.partialEvidenceSignals());
        limitationSignals.addAll(missingSignals);
        limitationSignals.addAll(sufficiency.degradedEvidenceSignals());
        limitationSignals.addAll(incompatibleSignals);
        List<String> limitationSignalsSorted = distinctSorted(limitationSignals);
        List<String> sourceReferenceIds = distinctSorted(routingDiagnostics.sourceReferenceIds());
        List<String> fingerprintInputs = replayReadinessFingerprintInputs(
                readinessStatus,
                sufficiency,
                candidateEvidenceStatus,
                alternativeEvidenceStatus,
                scoreEvidenceStatus,
                factorEvidenceStatus,
                fingerprintEvidenceStatus,
                checklist,
                limitationSignalsSorted,
                sourceReferenceIds);
        return new DecisionExplorerReplayReadinessDiagnosticV1(
                true,
                true,
                DecisionExplorerReplayReadinessDiagnosticV1.DIAGNOSTIC_OBJECT,
                DecisionExplorerReplayReadinessDiagnosticV1.CONTRACT_VERSION,
                readinessStatus,
                sufficiency.sufficiencyLevel(),
                sufficiency.readinessScore(),
                false,
                false,
                false,
                candidateEvidenceStatus,
                alternativeEvidenceStatus,
                scoreEvidenceStatus,
                factorEvidenceStatus,
                fingerprintEvidenceStatus,
                sufficiency.presentEvidenceSignals(),
                sufficiency.partialEvidenceSignals(),
                distinctSorted(missingSignals),
                sufficiency.degradedEvidenceSignals(),
                incompatibleSignals,
                checklist,
                limitationSignalsSorted,
                FINGERPRINT_ALGORITHM,
                diagnosticFingerprint("replay-readiness|v1", fingerprintInputs),
                replayReadinessReproducibilityKey(readinessStatus, sufficiency,
                        candidateEvidenceStatus, alternativeEvidenceStatus, scoreEvidenceStatus,
                        factorEvidenceStatus, fingerprintEvidenceStatus),
                fingerprintInputs,
                sourceReferenceIds,
                replayReadinessExplanation(readinessStatus, sufficiency, fingerprintEvidenceStatus),
                boundaryNote);
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

    private static String candidateReadinessStatus(DecisionExplorerEvidenceSufficiencyV1 sufficiency) {
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT.equals(sufficiency.sufficiencyLevel())) {
            return DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_UNKNOWN;
        }
        if (sufficiency.candidateEvidenceCount() > 0 && sufficiency.basicDiagnosticsReady()) {
            return DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE;
        }
        return DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_PARTIAL;
    }

    private static String alternativeReadinessStatus(
            List<DecisionExplorerRouteTradeoffRowV1> alternatives,
            int comparableAlternativeCount) {
        if (alternatives.isEmpty()) {
            return DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_MISSING;
        }
        if (comparableAlternativeCount > 0) {
            return DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE;
        }
        return DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_PARTIAL;
    }

    private static String scoreReadinessStatus(
            List<DecisionExplorerCandidateTradeoffScoringExplanationV1> scoringExplanations) {
        if (scoringExplanations.isEmpty()) {
            return DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_UNKNOWN;
        }
        if (scoreEvidenceComplete(scoringExplanations)) {
            return DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE;
        }
        return DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_PARTIAL;
    }

    private static String factorReadinessStatus(List<DecisionExplorerFactorTradeoffDeltaV1> deltas) {
        if (deltas.isEmpty()) {
            return DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_MISSING;
        }
        if (deltas.stream().anyMatch(delta -> DecisionExplorerFactorTradeoffDeltaV1.DELTA_DEGRADED
                .equals(delta.deltaClassification()))) {
            return DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_DEGRADED;
        }
        if (hasUnknownFactorDelta(deltas)) {
            return DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_PARTIAL;
        }
        return DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE;
    }

    private static String replayReadinessStatus(
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            String fingerprintEvidenceStatus) {
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED.equals(sufficiency.sufficiencyLevel())) {
            return DecisionExplorerReplayReadinessDiagnosticV1.STATUS_DEGRADED;
        }
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT.equals(sufficiency.sufficiencyLevel())) {
            return DecisionExplorerReplayReadinessDiagnosticV1.STATUS_UNKNOWN;
        }
        if (DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY.equals(sufficiency.sufficiencyLevel())
                && DecisionExplorerReplayReadinessDiagnosticV1.EVIDENCE_AVAILABLE.equals(fingerprintEvidenceStatus)) {
            return DecisionExplorerReplayReadinessDiagnosticV1.STATUS_READY;
        }
        return DecisionExplorerReplayReadinessDiagnosticV1.STATUS_PARTIAL;
    }

    private static List<String> replayReadinessChecklist(
            String candidateEvidenceStatus,
            String alternativeEvidenceStatus,
            String scoreEvidenceStatus,
            String factorEvidenceStatus,
            String fingerprintEvidenceStatus) {
        return List.of(
                "candidate evidence: " + candidateEvidenceStatus,
                "alternative evidence: " + alternativeEvidenceStatus,
                "score evidence: " + scoreEvidenceStatus,
                "factor evidence: " + factorEvidenceStatus,
                "diagnostic fingerprint evidence: " + fingerprintEvidenceStatus,
                "replay execution: UNAVAILABLE_READ_ONLY",
                "replay storage/export: UNAVAILABLE_READ_ONLY");
    }

    private static String replayReadinessExplanation(
            String readinessStatus,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            String fingerprintEvidenceStatus) {
        return "Replay-readiness diagnostic is " + readinessStatus + " with evidence sufficiency "
                + sufficiency.sufficiencyLevel() + " and score " + sufficiency.readinessScore()
                + ". Candidate evidence count " + sufficiency.candidateEvidenceCount()
                + ", comparable alternatives " + sufficiency.comparableAlternativeCount()
                + ", factor deltas " + sufficiency.factorDeltaCount()
                + ", and fingerprint evidence " + fingerprintEvidenceStatus
                + " were derived from returned Decision Explorer diagnostics only. This does not execute replay, "
                + "persist replay state, export replay evidence, or prove production readiness.";
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

    private static List<String> replayReadinessFingerprintInputs(
            String readinessStatus,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            String candidateEvidenceStatus,
            String alternativeEvidenceStatus,
            String scoreEvidenceStatus,
            String factorEvidenceStatus,
            String fingerprintEvidenceStatus,
            List<String> readinessChecklist,
            List<String> limitationSignals,
            List<String> sourceReferenceIds) {
        return canonicalInputs(List.of(
                input("diagnosticObject", DecisionExplorerReplayReadinessDiagnosticV1.DIAGNOSTIC_OBJECT),
                input("contractVersion", DecisionExplorerReplayReadinessDiagnosticV1.CONTRACT_VERSION),
                input("readinessStatus", readinessStatus),
                input("sufficiencyLevel", sufficiency.sufficiencyLevel()),
                input("readinessScore", sufficiency.readinessScore()),
                input("candidateEvidenceStatus", candidateEvidenceStatus),
                input("alternativeEvidenceStatus", alternativeEvidenceStatus),
                input("scoreEvidenceStatus", scoreEvidenceStatus),
                input("factorEvidenceStatus", factorEvidenceStatus),
                input("fingerprintEvidenceStatus", fingerprintEvidenceStatus),
                input("sufficiencyFingerprint", sufficiency.diagnosticFingerprint()),
                input("readinessChecklist", readinessChecklist),
                input("limitationSignals", limitationSignals),
                input("sourceReferenceIds", sourceReferenceIds),
                input("replayExecutionAvailable", false),
                input("replayStorageAvailable", false),
                input("replayExportAvailable", false)));
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

    private static String replayReadinessReproducibilityKey(
            String readinessStatus,
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            String candidateEvidenceStatus,
            String alternativeEvidenceStatus,
            String scoreEvidenceStatus,
            String factorEvidenceStatus,
            String fingerprintEvidenceStatus) {
        return String.join(":",
                "replay-readiness",
                DecisionExplorerReplayReadinessDiagnosticV1.CONTRACT_VERSION,
                fingerprintValue(readinessStatus),
                fingerprintValue(sufficiency.sufficiencyLevel()),
                "score=" + sufficiency.readinessScore(),
                "candidate=" + fingerprintValue(candidateEvidenceStatus),
                "alternative=" + fingerprintValue(alternativeEvidenceStatus),
                "scoreEvidence=" + fingerprintValue(scoreEvidenceStatus),
                "factor=" + fingerprintValue(factorEvidenceStatus),
                "fingerprint=" + fingerprintValue(fingerprintEvidenceStatus));
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
