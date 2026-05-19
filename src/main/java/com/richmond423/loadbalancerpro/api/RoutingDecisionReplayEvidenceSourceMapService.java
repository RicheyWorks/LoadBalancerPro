package com.richmond423.loadbalancerpro.api;

import java.util.List;

public final class RoutingDecisionReplayEvidenceSourceMapService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-source-map/v1";
    private static final String SOURCE =
            "/api/routing/compare already-built lab evidence objects: decisionVector, dominantFactorAnalysis, "
                    + "decisionDeltaAnalysis, decisionReplaySnapshot, decisionReplayReconstructionTrace, "
                    + "decisionReplayCapsule, and decisionReplayReadinessChecklist";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision Replay Evidence Source Map is a read-only lab source map derived only from already-built "
                    + "routing compare response objects; it does not execute replay, perform what-if mutation, "
                    + "persist source-map data or audit logs, generate fingerprints, recompute scores, retune "
                    + "weights, change routing behavior, add telemetry, add upload/share/download flows, or add "
                    + "server-side export/PDF/ZIP generation.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "No production certification, live-cloud proof, real-tenant proof, SLA/SLO proof, registry "
                    + "publication proof, container signing proof, governance application proof, production traffic "
                    + "validation, exact production scoring proof, cryptographic production proof, guaranteed replay, "
                    + "or production readiness proof is claimed.";

    public RoutingDecisionReplayEvidenceSourceMapResponse sourceMap(
            String strategyId,
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist) {
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
        String checklistStatus = analysisStatus(readinessChecklist == null ? null : readinessChecklist.status());

        String selectedCandidateId = selectedCandidateId(
                decisionVector,
                replaySnapshot,
                reconstructionTrace,
                replayCapsule,
                readinessChecklist);
        int candidateCount = candidateCount(
                decisionVector,
                replaySnapshot,
                reconstructionTrace,
                replayCapsule,
                readinessChecklist);
        String resolvedStrategyId = strategyId(
                strategyId,
                decisionVector,
                replaySnapshot,
                reconstructionTrace,
                replayCapsule,
                readinessChecklist);
        String snapshotFingerprint = linkedFingerprint(snapshotStatus,
                firstNonBlank(
                        readinessChecklist == null ? null : readinessChecklist.linkedReplaySnapshotFingerprint(),
                        replaySnapshot == null ? null : replaySnapshot.snapshotFingerprint()));
        String traceFingerprint = linkedFingerprint(traceStatus,
                firstNonBlank(
                        readinessChecklist == null ? null : readinessChecklist.linkedReconstructionTraceFingerprint(),
                        reconstructionTrace == null ? null : reconstructionTrace.traceFingerprint()));
        String capsuleFingerprint = linkedFingerprint(capsuleStatus,
                firstNonBlank(
                        readinessChecklist == null ? null : readinessChecklist.linkedReplayCapsuleFingerprint(),
                        replayCapsule == null ? null : replayCapsule.capsuleFingerprint()));

        List<DecisionReplayEvidenceSourceMapEntryResponse> entries = List.of(
                entry(
                        "decision-vector-source",
                        "Decision Vector source",
                        decisionVectorStatus,
                        "decisionVector",
                        List.of(
                                "dominantFactorAnalysis",
                                "decisionDeltaAnalysis",
                                "decisionReplaySnapshot.decisionVectorStatus",
                                "decisionReplayReconstructionTrace.decisionVectorStatus",
                                "decisionReplayCapsule.decisionVectorStatus",
                                "decisionReplayReadinessChecklist.decisionVectorStatus"),
                        null,
                        STATUS_AVAILABLE.equals(decisionVectorStatus)
                                ? "Decision Vector candidate evidence was returned and can support downstream lab artifacts."
                                : "Decision Vector candidate evidence was not returned; downstream source mapping stays UNKNOWN."),
                entry(
                        "dominant-factor-analysis-source",
                        "Dominant Factor Analysis source",
                        dominantStatus,
                        "dominantFactorAnalysis",
                        List.of(
                                "decisionReplaySnapshot.dominantFactorAnalysisStatus",
                                "decisionReplayReconstructionTrace.dominantFactorAnalysisStatus",
                                "decisionReplayCapsule.dominantFactorAnalysisStatus",
                                "decisionReplayReadinessChecklist.dominantFactorAnalysisStatus"),
                        null,
                        analysisSummary("Dominant Factor Analysis", dominantStatus)),
                entry(
                        "decision-delta-analysis-source",
                        "Decision Delta Analysis source",
                        deltaStatus,
                        "decisionDeltaAnalysis",
                        List.of(
                                "decisionReplaySnapshot.decisionDeltaAnalysisStatus",
                                "decisionReplayReconstructionTrace.decisionDeltaAnalysisStatus",
                                "decisionReplayCapsule.decisionDeltaAnalysisStatus",
                                "decisionReplayReadinessChecklist.decisionDeltaAnalysisStatus"),
                        null,
                        analysisSummary("Decision Delta Analysis", deltaStatus)),
                entry(
                        "replay-snapshot-source",
                        "Decision Replay Snapshot source",
                        linkedEvidenceStatus(snapshotStatus, snapshotFingerprint),
                        "decisionReplaySnapshot",
                        List.of(
                                "decisionReplayReconstructionTrace.snapshotFingerprint",
                                "decisionReplayCapsule.linkedReplaySnapshotFingerprint",
                                "decisionReplayReadinessChecklist.linkedReplaySnapshotFingerprint",
                                "decisionReplayEvidenceSourceMap.linkedReplaySnapshotFingerprint"),
                        snapshotFingerprint,
                        fingerprintSummary("Decision Replay Snapshot", snapshotStatus, snapshotFingerprint)),
                entry(
                        "reconstruction-trace-source",
                        "Decision Replay Reconstruction Trace source",
                        linkedEvidenceStatus(traceStatus, traceFingerprint),
                        "decisionReplayReconstructionTrace",
                        List.of(
                                "decisionReplayCapsule.linkedReconstructionTraceFingerprint",
                                "decisionReplayReadinessChecklist.linkedReconstructionTraceFingerprint",
                                "decisionReplayEvidenceSourceMap.linkedReconstructionTraceFingerprint"),
                        traceFingerprint,
                        fingerprintSummary("Decision Replay Reconstruction Trace", traceStatus, traceFingerprint)),
                entry(
                        "replay-capsule-source",
                        "Decision Replay Capsule source",
                        linkedEvidenceStatus(capsuleStatus, capsuleFingerprint),
                        "decisionReplayCapsule",
                        List.of(
                                "decisionReplayReadinessChecklist.linkedReplayCapsuleFingerprint",
                                "decisionReplayReadinessChecklist.decisionReplayCapsuleStatus",
                                "decisionReplayEvidenceSourceMap.linkedReplayCapsuleFingerprint"),
                        capsuleFingerprint,
                        fingerprintSummary("Decision Replay Capsule", capsuleStatus, capsuleFingerprint)),
                entry(
                        "readiness-checklist-source",
                        "Decision Replay Readiness Checklist source",
                        checklistStatus,
                        "decisionReplayReadinessChecklist",
                        List.of(
                                "decisionReplayEvidenceSourceMap.decisionReplayReadinessChecklistStatus",
                                "decisionReplayEvidenceSourceMap.linkedReplaySnapshotFingerprint",
                                "decisionReplayEvidenceSourceMap.linkedReconstructionTraceFingerprint",
                                "decisionReplayEvidenceSourceMap.linkedReplayCapsuleFingerprint"),
                        STATUS_UNKNOWN.equals(checklistStatus) ? null : capsuleFingerprint,
                        analysisSummary("Decision Replay Readiness Checklist", checklistStatus)),
                entry(
                        "linked-fingerprint-source",
                        "Linked replay fingerprint source",
                        linkedFingerprintSourceStatus(snapshotFingerprint, traceFingerprint, capsuleFingerprint),
                        "decisionReplaySnapshot.snapshotFingerprint, "
                                + "decisionReplayReconstructionTrace.traceFingerprint, "
                                + "decisionReplayCapsule.capsuleFingerprint",
                        List.of(
                                "decisionReplayEvidenceSourceMap.linkedReplaySnapshotFingerprint",
                                "decisionReplayEvidenceSourceMap.linkedReconstructionTraceFingerprint",
                                "decisionReplayEvidenceSourceMap.linkedReplayCapsuleFingerprint"),
                        firstNonBlank(snapshotFingerprint, traceFingerprint, capsuleFingerprint),
                        linkedFingerprintSummary(snapshotFingerprint, traceFingerprint, capsuleFingerprint)),
                entry(
                        "read-only-boundary-source",
                        "Read-only lab boundary source",
                        STATUS_AVAILABLE,
                        "decisionReplayEvidenceSourceMap.boundaryNote, "
                                + "decisionReplayEvidenceSourceMap.productionNotProvenBoundary",
                        List.of(),
                        null,
                        "Source-map boundary text is present and states that the lane is read-only lab evidence mapping only."));
        String status = sourceMapStatus(
                entries,
                decisionVectorStatus,
                dominantStatus,
                deltaStatus,
                snapshotStatus,
                traceStatus,
                capsuleStatus,
                checklistStatus,
                selectedCandidateId,
                candidateCount);

        return new RoutingDecisionReplayEvidenceSourceMapResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                status,
                safeValue(resolvedStrategyId),
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
                checklistStatus,
                entries,
                explanation(status, resolvedStrategyId, selectedCandidateId, candidateCount, entries.size()),
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    private static DecisionReplayEvidenceSourceMapEntryResponse entry(
            String sourceId,
            String label,
            String status,
            String sourceFieldPath,
            List<String> downstreamEvidenceFieldPaths,
            String linkedFingerprint,
            String evidenceSummary) {
        return new DecisionReplayEvidenceSourceMapEntryResponse(
                sourceId,
                label,
                analysisStatus(status),
                sourceFieldPath,
                downstreamEvidenceFieldPaths,
                isBlank(linkedFingerprint) ? null : linkedFingerprint.trim(),
                evidenceSummary,
                BOUNDARY_NOTE);
    }

    private static String decisionVectorStatus(RoutingDecisionVectorResponse decisionVector) {
        if (decisionVector == null || decisionVector.candidateSummaries() == null
                || decisionVector.candidateSummaries().isEmpty()) {
            return STATUS_UNKNOWN;
        }
        return STATUS_AVAILABLE;
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

    private static String linkedFingerprint(String sourceStatus, String fingerprint) {
        if (isBlank(fingerprint) || STATUS_UNKNOWN.equals(analysisStatus(sourceStatus))) {
            return null;
        }
        return fingerprint.trim();
    }

    private static String linkedFingerprintSourceStatus(
            String snapshotFingerprint,
            String traceFingerprint,
            String capsuleFingerprint) {
        boolean snapshotAvailable = !isBlank(snapshotFingerprint);
        boolean traceAvailable = !isBlank(traceFingerprint);
        boolean capsuleAvailable = !isBlank(capsuleFingerprint);
        if (!snapshotAvailable && !traceAvailable && !capsuleAvailable) {
            return STATUS_UNKNOWN;
        }
        if (snapshotAvailable && traceAvailable && capsuleAvailable) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
    }

    private static String strategyId(
            String strategyId,
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist) {
        return firstNonBlank(
                readinessChecklist == null ? null : readinessChecklist.strategyId(),
                replayCapsule == null ? null : replayCapsule.strategyId(),
                reconstructionTrace == null ? null : reconstructionTrace.strategyId(),
                replaySnapshot == null ? null : replaySnapshot.strategyId(),
                decisionVector == null ? null : decisionVector.selectedStrategy(),
                strategyId);
    }

    private static String selectedCandidateId(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist) {
        String candidateId = firstNonBlank(
                readinessChecklist == null ? null : readinessChecklist.selectedCandidateId(),
                replayCapsule == null ? null : replayCapsule.selectedCandidateId(),
                reconstructionTrace == null ? null : reconstructionTrace.selectedCandidateId(),
                replaySnapshot == null ? null : replaySnapshot.selectedCandidateId(),
                decisionVector != null && decisionVector.selectedCandidateVector() != null
                        ? decisionVector.selectedCandidateVector().candidateId() : null,
                decisionVector == null ? null : decisionVector.selectedBackend());
        return isBlank(candidateId) ? null : candidateId.trim();
    }

    private static int candidateCount(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist) {
        if (readinessChecklist != null && readinessChecklist.candidateCount() > 0) {
            return readinessChecklist.candidateCount();
        }
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

    private static String sourceMapStatus(
            List<DecisionReplayEvidenceSourceMapEntryResponse> entries,
            String decisionVectorStatus,
            String dominantStatus,
            String deltaStatus,
            String snapshotStatus,
            String traceStatus,
            String capsuleStatus,
            String checklistStatus,
            String selectedCandidateId,
            int candidateCount) {
        if (STATUS_UNKNOWN.equals(decisionVectorStatus)
                && STATUS_UNKNOWN.equals(dominantStatus)
                && STATUS_UNKNOWN.equals(deltaStatus)
                && STATUS_UNKNOWN.equals(snapshotStatus)
                && STATUS_UNKNOWN.equals(traceStatus)
                && STATUS_UNKNOWN.equals(capsuleStatus)
                && STATUS_UNKNOWN.equals(checklistStatus)
                && isBlank(selectedCandidateId)
                && candidateCount == 0) {
            return STATUS_UNKNOWN;
        }
        if (entries.stream().allMatch(entry -> STATUS_AVAILABLE.equals(entry.status()))) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
    }

    private static String explanation(
            String status,
            String strategyId,
            String selectedCandidateId,
            int candidateCount,
            int sourceMapEntryCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision Replay Evidence Source Map is UNKNOWN because selected candidate, candidate set, and "
                    + "already-built lab evidence lane statuses were not returned. No replay execution, what-if "
                    + "mutation, persisted source map, selected candidate, candidate set, alternative, score gap, "
                    + "factor values, replay claim, production certification, or explanation evidence is invented.";
        }
        return "Decision Replay Evidence Source Map is " + status + " for strategy " + safeValue(strategyId)
                + ": selected candidate " + safeValue(selectedCandidateId) + ", " + candidateCount
                + " candidate id(s), and " + sourceMapEntryCount
                + " deterministic source-map entry(s) were derived from already-built lab compare evidence only. "
                + "This source map explains evidence relationships without executing replay, performing what-if "
                + "mutation, persisting data, or generating a new fingerprint.";
    }

    private static String analysisSummary(String evidenceName, String status) {
        if (STATUS_AVAILABLE.equals(status)) {
            return evidenceName + " source evidence was returned as AVAILABLE.";
        }
        if (STATUS_PARTIAL.equals(status)) {
            return evidenceName + " source evidence was returned as PARTIAL.";
        }
        return evidenceName + " source evidence was UNKNOWN or not returned.";
    }

    private static String fingerprintSummary(String evidenceName, String status, String linkedFingerprint) {
        if (!isBlank(linkedFingerprint)) {
            return evidenceName + " fingerprint was linked from already-built compare evidence.";
        }
        if (STATUS_UNKNOWN.equals(analysisStatus(status))) {
            return evidenceName + " fingerprint was not linked because the source evidence status is UNKNOWN.";
        }
        return evidenceName + " fingerprint was not returned by already-built compare evidence.";
    }

    private static String linkedFingerprintSummary(
            String snapshotFingerprint,
            String traceFingerprint,
            String capsuleFingerprint) {
        int linkedCount = 0;
        if (!isBlank(snapshotFingerprint)) {
            linkedCount++;
        }
        if (!isBlank(traceFingerprint)) {
            linkedCount++;
        }
        if (!isBlank(capsuleFingerprint)) {
            linkedCount++;
        }
        if (linkedCount == 0) {
            return "No linked replay fingerprints were returned by already-built compare evidence.";
        }
        return linkedCount + " linked replay fingerprint(s) were reused from already-built compare evidence.";
    }

    private static String analysisStatus(String status) {
        if (STATUS_AVAILABLE.equals(status) || STATUS_PARTIAL.equals(status) || STATUS_UNKNOWN.equals(status)) {
            return status;
        }
        return STATUS_UNKNOWN;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String safeValue(String value) {
        return isBlank(value) ? "UNKNOWN" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
