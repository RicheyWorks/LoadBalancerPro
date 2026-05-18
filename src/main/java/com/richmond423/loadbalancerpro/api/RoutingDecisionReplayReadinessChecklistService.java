package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RoutingDecisionReplayReadinessChecklistService {
    private static final String SCHEMA_VERSION = "decision-replay-readiness-checklist/v1";
    private static final String SOURCE =
            "/api/routing/compare already-built lab evidence: decisionVector, dominantFactorAnalysis, "
                    + "decisionDeltaAnalysis, decisionReplaySnapshot, decisionReplayReconstructionTrace, "
                    + "and decisionReplayCapsule";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision Replay Readiness Checklist is read-only lab evidence readiness packaging derived only from "
                    + "already-built routing compare response data; it does not execute replay, perform what-if "
                    + "mutation, persist checklist state or audit logs, recompute scores, retune weights, change "
                    + "routing behavior, add telemetry, add upload/share/download flows, or add server-side "
                    + "export/PDF/ZIP generation.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "No production certification, live-cloud proof, real-tenant proof, SLA/SLO proof, registry "
                    + "publication proof, container signing proof, governance application proof, production traffic "
                    + "validation, exact production scoring proof, cryptographic production proof, guaranteed replay, "
                    + "or production readiness proof is claimed.";

    public RoutingDecisionReplayReadinessChecklistResponse checklist(
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule) {
        String decisionVectorStatus = decisionVectorStatus(decisionVector);
        String dominantStatus = analysisStatus(dominantFactorAnalysis == null
                ? null
                : dominantFactorAnalysis.status());
        String deltaStatus = analysisStatus(decisionDeltaAnalysis == null
                ? null
                : decisionDeltaAnalysis.status());
        String snapshotStatus = analysisStatus(replaySnapshot == null ? null : replaySnapshot.status());
        String traceStatus = analysisStatus(reconstructionTrace == null ? null : reconstructionTrace.status());
        String capsuleStatus = analysisStatus(replayCapsule == null ? null : replayCapsule.status());

        String strategyId = strategyId(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule);
        String selectedCandidateId = selectedCandidateId(decisionVector, replaySnapshot, reconstructionTrace,
                replayCapsule);
        int candidateCount = candidateCount(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule);
        String snapshotFingerprint = linkedFingerprint(snapshotStatus,
                replaySnapshot == null ? null : replaySnapshot.snapshotFingerprint());
        String traceFingerprint = linkedFingerprint(traceStatus,
                reconstructionTrace == null ? null : reconstructionTrace.traceFingerprint());
        String capsuleFingerprint = linkedFingerprint(capsuleStatus,
                replayCapsule == null ? null : replayCapsule.capsuleFingerprint());

        List<DecisionReplayReadinessChecklistItemResponse> items = List.of(
                item(
                        "decision-vector-evidence",
                        "Decision Vector evidence",
                        decisionVectorStatus,
                        "decisionVector",
                        STATUS_AVAILABLE.equals(decisionVectorStatus)
                                ? "Decision Vector candidate summary evidence was returned."
                                : "Decision Vector candidate summary evidence was not returned.",
                        STATUS_AVAILABLE.equals(decisionVectorStatus) ? null
                                : "decisionVector candidate summaries were absent or empty"),
                item(
                        "dominant-factor-evidence",
                        "Dominant Factor Analysis evidence",
                        dominantStatus,
                        "dominantFactorAnalysis.status",
                        analysisExplanation("Dominant Factor Analysis", dominantStatus),
                        STATUS_AVAILABLE.equals(dominantStatus) ? null
                                : "dominant factor analysis evidence was incomplete or unavailable"),
                item(
                        "decision-delta-evidence",
                        "Decision Delta Analysis evidence",
                        deltaStatus,
                        "decisionDeltaAnalysis.status",
                        analysisExplanation("Decision Delta Analysis", deltaStatus),
                        STATUS_AVAILABLE.equals(deltaStatus) ? null
                                : "decision delta analysis evidence was incomplete or unavailable"),
                item(
                        "replay-snapshot-evidence",
                        "Decision Replay Snapshot evidence",
                        linkedEvidenceStatus(snapshotStatus, snapshotFingerprint),
                        "decisionReplaySnapshot.status, decisionReplaySnapshot.snapshotFingerprint",
                        fingerprintExplanation("Decision Replay Snapshot", snapshotStatus, snapshotFingerprint),
                        snapshotFingerprint == null ? "snapshot fingerprint was absent or unavailable" : null),
                item(
                        "reconstruction-trace-evidence",
                        "Decision Replay Reconstruction Trace evidence",
                        linkedEvidenceStatus(traceStatus, traceFingerprint),
                        "decisionReplayReconstructionTrace.status, decisionReplayReconstructionTrace.traceFingerprint",
                        fingerprintExplanation("Decision Replay Reconstruction Trace", traceStatus, traceFingerprint),
                        traceFingerprint == null ? "reconstruction trace fingerprint was absent or unavailable" : null),
                item(
                        "replay-capsule-evidence",
                        "Decision Replay Capsule evidence",
                        linkedEvidenceStatus(capsuleStatus, capsuleFingerprint),
                        "decisionReplayCapsule.status, decisionReplayCapsule.capsuleFingerprint",
                        fingerprintExplanation("Decision Replay Capsule", capsuleStatus, capsuleFingerprint),
                        capsuleFingerprint == null ? "capsule fingerprint was absent or unavailable" : null),
                item(
                        "candidate-evidence",
                        "Candidate evidence",
                        candidateEvidenceStatus(selectedCandidateId, candidateCount,
                                candidateFinalScoreStatus(reconstructionTrace, candidateCount)),
                        "decisionVector.selectedCandidateVector, decisionVector.candidateSummaries, "
                                + "decisionReplaySnapshot.selectedCandidateId, "
                                + "decisionReplayReconstructionTrace.candidateIdsConsidered, "
                                + "decisionReplayReconstructionTrace.candidateFinalScores, "
                                + "decisionReplayCapsule.selectedCandidateId, decisionReplayCapsule.candidateCount",
                        candidateEvidenceExplanation(selectedCandidateId, candidateCount,
                                candidateFinalScoreStatus(reconstructionTrace, candidateCount)),
                        candidateEvidenceMissingReason(selectedCandidateId, candidateCount,
                                candidateFinalScoreStatus(reconstructionTrace, candidateCount))),
                item(
                        "factor-evidence",
                        "Factor evidence",
                        factorContributionStatus(reconstructionTrace, replayCapsule),
                        "decisionReplayReconstructionTrace.factorContributionStatus, "
                                + "decisionReplayCapsule.factorContributionStatus",
                        analysisExplanation("Factor contribution evidence",
                                factorContributionStatus(reconstructionTrace, replayCapsule)),
                        STATUS_AVAILABLE.equals(factorContributionStatus(reconstructionTrace, replayCapsule))
                                ? null : "finite named factor contribution evidence was incomplete or unavailable"),
                item(
                        "read-only-boundary-evidence",
                        "Read-only boundary evidence",
                        STATUS_AVAILABLE,
                        "decisionReplayReadinessChecklist.boundaryNote, "
                                + "decisionReplayReadinessChecklist.productionNotProvenBoundary",
                        "Read-only lab-only boundary text is present on the checklist response.",
                        null));
        String status = checklistStatus(items, decisionVectorStatus, selectedCandidateId, candidateCount,
                capsuleStatus);
        int availableCount = count(items, STATUS_AVAILABLE);
        int partialCount = count(items, STATUS_PARTIAL);
        int unknownCount = count(items, STATUS_UNKNOWN);

        return new RoutingDecisionReplayReadinessChecklistResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                status,
                safeValue(strategyId),
                isBlank(selectedCandidateId) ? null : selectedCandidateId,
                candidateCount,
                snapshotFingerprint,
                traceFingerprint,
                capsuleFingerprint,
                decisionVectorStatus,
                dominantStatus,
                deltaStatus,
                snapshotStatus,
                traceStatus,
                capsuleStatus,
                availableCount,
                partialCount,
                unknownCount,
                items,
                explanation(status, strategyId, selectedCandidateId, candidateCount, availableCount, partialCount,
                        unknownCount),
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    private static DecisionReplayReadinessChecklistItemResponse item(
            String itemId,
            String label,
            String status,
            String evidenceSourceFieldPath,
            String explanation,
            String missingEvidenceReason) {
        return new DecisionReplayReadinessChecklistItemResponse(
                itemId,
                label,
                analysisStatus(status),
                evidenceSourceFieldPath,
                explanation,
                isBlank(missingEvidenceReason) ? null : missingEvidenceReason);
    }

    private static String decisionVectorStatus(RoutingDecisionVectorResponse decisionVector) {
        if (decisionVector == null || decisionVector.candidateSummaries() == null
                || decisionVector.candidateSummaries().isEmpty()) {
            return STATUS_UNKNOWN;
        }
        return STATUS_AVAILABLE;
    }

    private static String candidateFinalScoreStatus(
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            int candidateCount) {
        Map<String, Double> scores = reconstructionTrace == null ? null : reconstructionTrace.candidateFinalScores();
        if (scores == null || scores.isEmpty()) {
            return STATUS_UNKNOWN;
        }
        long finiteScoreCount = scores.values().stream().filter(RoutingDecisionReplayReadinessChecklistService::isFinite)
                .count();
        if (finiteScoreCount == 0) {
            return STATUS_UNKNOWN;
        }
        if (candidateCount > 0 && finiteScoreCount >= candidateCount) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
    }

    private static String candidateFinalScoreExplanation(
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            int candidateCount) {
        String status = candidateFinalScoreStatus(reconstructionTrace, candidateCount);
        if (STATUS_AVAILABLE.equals(status)) {
            return "Finite candidate final scores were returned by already-built reconstruction trace evidence.";
        }
        if (STATUS_PARTIAL.equals(status)) {
            return "Some finite candidate final scores were returned, but score evidence was incomplete.";
        }
        return "No finite candidate final scores were returned by already-built reconstruction trace evidence.";
    }

    private static String candidateFinalScoreMissingReason(
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            int candidateCount) {
        String status = candidateFinalScoreStatus(reconstructionTrace, candidateCount);
        if (STATUS_AVAILABLE.equals(status)) {
            return null;
        }
        if (STATUS_PARTIAL.equals(status)) {
            return "one or more candidate final scores were absent or non-finite";
        }
        return "finite candidate final score evidence was absent";
    }

    private static String factorContributionStatus(
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule) {
        if (replayCapsule != null && !isBlank(replayCapsule.factorContributionStatus())) {
            return analysisStatus(replayCapsule.factorContributionStatus());
        }
        if (reconstructionTrace != null && !isBlank(reconstructionTrace.factorContributionStatus())) {
            return analysisStatus(reconstructionTrace.factorContributionStatus());
        }
        return STATUS_UNKNOWN;
    }

    private static String linkedEvidenceStatus(String sourceStatus, String linkedFingerprint) {
        String normalizedStatus = analysisStatus(sourceStatus);
        if (STATUS_UNKNOWN.equals(normalizedStatus)) {
            return STATUS_UNKNOWN;
        }
        if (isBlank(linkedFingerprint)) {
            return STATUS_PARTIAL;
        }
        return normalizedStatus;
    }

    private static String candidateEvidenceStatus(
            String selectedCandidateId,
            int candidateCount,
            String candidateFinalScoreStatus) {
        if (isBlank(selectedCandidateId) && candidateCount == 0) {
            return STATUS_UNKNOWN;
        }
        if (!isBlank(selectedCandidateId) && candidateCount > 0
                && !STATUS_UNKNOWN.equals(candidateFinalScoreStatus)) {
            return candidateFinalScoreStatus;
        }
        if (!isBlank(selectedCandidateId) && candidateCount > 0) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
    }

    private static String candidateEvidenceExplanation(
            String selectedCandidateId,
            int candidateCount,
            String candidateFinalScoreStatus) {
        String status = candidateEvidenceStatus(selectedCandidateId, candidateCount, candidateFinalScoreStatus);
        if (STATUS_UNKNOWN.equals(status)) {
            return "No selected candidate id or candidate set was returned by already-built compare evidence.";
        }
        String selectedText = isBlank(selectedCandidateId)
                ? "selected candidate UNKNOWN"
                : "selected candidate " + selectedCandidateId;
        return "Candidate evidence was derived from already-built compare evidence: " + selectedText
                + ", " + candidateCount + " candidate id(s), and candidate final score status "
                + candidateFinalScoreStatus + ".";
    }

    private static String candidateEvidenceMissingReason(
            String selectedCandidateId,
            int candidateCount,
            String candidateFinalScoreStatus) {
        if (STATUS_AVAILABLE.equals(candidateEvidenceStatus(selectedCandidateId, candidateCount,
                candidateFinalScoreStatus))) {
            return null;
        }
        if (STATUS_PARTIAL.equals(candidateEvidenceStatus(selectedCandidateId, candidateCount,
                candidateFinalScoreStatus))) {
            return "selected candidate, candidate set, or finite candidate final score evidence was incomplete";
        }
        return "selected candidate and candidate set evidence were absent";
    }

    private static String linkedFingerprint(String sourceStatus, String fingerprint) {
        if (isBlank(fingerprint) || STATUS_UNKNOWN.equals(analysisStatus(sourceStatus))) {
            return null;
        }
        return fingerprint.trim();
    }

    private static String fingerprintExplanation(String evidenceName, String sourceStatus, String linkedFingerprint) {
        if (linkedFingerprint != null) {
            return evidenceName + " fingerprint was linked from already-built compare evidence.";
        }
        if (STATUS_UNKNOWN.equals(analysisStatus(sourceStatus))) {
            return evidenceName + " fingerprint was not linked because the source evidence status is UNKNOWN.";
        }
        return evidenceName + " fingerprint was not returned.";
    }

    private static String strategyId(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule) {
        if (replayCapsule != null && !isBlank(replayCapsule.strategyId())) {
            return replayCapsule.strategyId();
        }
        if (reconstructionTrace != null && !isBlank(reconstructionTrace.strategyId())) {
            return reconstructionTrace.strategyId();
        }
        if (replaySnapshot != null && !isBlank(replaySnapshot.strategyId())) {
            return replaySnapshot.strategyId();
        }
        if (decisionVector != null && !isBlank(decisionVector.selectedStrategy())) {
            return decisionVector.selectedStrategy();
        }
        return null;
    }

    private static String selectedCandidateId(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule) {
        if (replayCapsule != null && !isBlank(replayCapsule.selectedCandidateId())) {
            return replayCapsule.selectedCandidateId().trim();
        }
        if (reconstructionTrace != null && !isBlank(reconstructionTrace.selectedCandidateId())) {
            return reconstructionTrace.selectedCandidateId().trim();
        }
        if (replaySnapshot != null && !isBlank(replaySnapshot.selectedCandidateId())) {
            return replaySnapshot.selectedCandidateId().trim();
        }
        if (decisionVector != null && decisionVector.selectedCandidateVector() != null
                && !isBlank(decisionVector.selectedCandidateVector().candidateId())) {
            return decisionVector.selectedCandidateVector().candidateId().trim();
        }
        if (decisionVector != null && !isBlank(decisionVector.selectedBackend())) {
            return decisionVector.selectedBackend().trim();
        }
        return null;
    }

    private static int candidateCount(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule) {
        if (replayCapsule != null && replayCapsule.candidateCount() > 0) {
            return replayCapsule.candidateCount();
        }
        if (reconstructionTrace != null && reconstructionTrace.candidateCount() > 0) {
            return reconstructionTrace.candidateCount();
        }
        if (replaySnapshot != null && replaySnapshot.candidateCount() > 0) {
            return replaySnapshot.candidateCount();
        }
        if (decisionVector != null && decisionVector.candidateCount() > 0) {
            return decisionVector.candidateCount();
        }
        return 0;
    }

    private static String analysisExplanation(String evidenceName, String status) {
        if (STATUS_AVAILABLE.equals(status)) {
            return evidenceName + " was returned as AVAILABLE.";
        }
        if (STATUS_PARTIAL.equals(status)) {
            return evidenceName + " was returned as PARTIAL.";
        }
        return evidenceName + " was UNKNOWN or not returned.";
    }

    private static String checklistStatus(
            List<DecisionReplayReadinessChecklistItemResponse> items,
            String decisionVectorStatus,
            String selectedCandidateId,
            int candidateCount,
            String capsuleStatus) {
        if (STATUS_UNKNOWN.equals(decisionVectorStatus)
                && isBlank(selectedCandidateId)
                && candidateCount == 0
                && STATUS_UNKNOWN.equals(capsuleStatus)) {
            return STATUS_UNKNOWN;
        }
        if (items.stream().allMatch(item -> STATUS_AVAILABLE.equals(item.status()))) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
    }

    private static int count(List<DecisionReplayReadinessChecklistItemResponse> items, String status) {
        return (int) items.stream().filter(item -> status.equals(item.status())).count();
    }

    private static String explanation(
            String status,
            String strategyId,
            String selectedCandidateId,
            int candidateCount,
            int availableCount,
            int partialCount,
            int unknownCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision Replay Readiness Checklist is UNKNOWN because selected candidate, candidate set, "
                    + "and Decision Vector evidence were not returned. No replay execution, what-if mutation, "
                    + "persisted checklist, selected candidate, candidate set, alternative, score gap, factor "
                    + "evidence, replay claim, or explanation evidence is invented.";
        }
        return "Decision Replay Readiness Checklist is " + status + " for strategy " + safeValue(strategyId)
                + ": selected candidate " + safeValue(selectedCandidateId) + ", " + candidateCount
                + " candidate id(s), " + availableCount + " available checklist item(s), " + partialCount
                + " partial checklist item(s), and " + unknownCount
                + " unknown checklist item(s) were derived from already-built lab compare evidence only. "
                + "This checklist packages lab replay-readiness signals without executing replay or performing "
                + "what-if mutation.";
    }

    private static String analysisStatus(String status) {
        if (STATUS_AVAILABLE.equals(status) || STATUS_PARTIAL.equals(status) || STATUS_UNKNOWN.equals(status)) {
            return status;
        }
        return STATUS_UNKNOWN;
    }

    private static boolean isFinite(Double value) {
        return value != null && Double.isFinite(value);
    }

    private static String safeValue(String value) {
        return isBlank(value) ? "UNKNOWN" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
