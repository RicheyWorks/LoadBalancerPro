package com.richmond423.loadbalancerpro.api;

import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticFingerprintSupport.canonicalInputs;
import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticFingerprintSupport.diagnosticFingerprint;
import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticFingerprintSupport.fingerprintValue;
import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticFingerprintSupport.input;
import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticListSupport.copyNonNull;
import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticListSupport.distinctSorted;

import java.util.ArrayList;
import java.util.List;

final class DecisionExplorerReplayReadinessEvaluator {
    DecisionExplorerReplayReadinessDiagnosticV1 build(
            DecisionExplorerEvidenceSufficiencyV1 sufficiency,
            List<DecisionExplorerRouteTradeoffRowV1> rows,
            List<DecisionExplorerCandidateTradeoffScoringExplanationV1> scoringExplanations,
            List<DecisionExplorerFactorTradeoffDeltaV1> factorTradeoffDeltas,
            DecisionExplorerRoutingDiagnosticsV1 routingDiagnostics,
            String boundaryNote) {
        if (sufficiency == null || routingDiagnostics == null) {
            return DecisionExplorerReplayReadinessDiagnosticV1.unknown(boundaryNote);
        }

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
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
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

}
