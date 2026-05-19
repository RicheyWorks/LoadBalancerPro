package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RoutingDecisionReplayEvidenceBoundarySummaryService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-boundary-summary/v1";
    private static final String SOURCE =
            "/api/routing/compare already-built lab evidence boundary fields and statuses: decisionVector, "
                    + "dominantFactorAnalysis, decisionDeltaAnalysis, decisionReplaySnapshot, "
                    + "decisionReplayReconstructionTrace, decisionReplayCapsule, "
                    + "decisionReplayReadinessChecklist, and decisionReplayEvidenceSourceMap";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision Replay Evidence Boundary Summary is read-only lab boundary metadata derived only from "
                    + "already-built routing compare response objects; it does not execute replay, perform what-if "
                    + "mutation, persist boundary-summary data or audit logs, generate fingerprints, recompute "
                    + "scores, retune weights, change routing behavior, add telemetry, add external calls, add "
                    + "upload/share/download flows, or add server-side export/PDF/ZIP generation.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "No production behavior proof, live-cloud proof, real-tenant proof, SLA/SLO proof, registry "
                    + "publication proof, container signing proof, governance application proof, production traffic "
                    + "validation, exact production scoring proof, or cryptographic production proof is claimed.";

    public RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary(
            String strategyId,
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap) {
        String decisionVectorStatus = decisionVectorStatus(decisionVector);
        String dominantStatus = analysisStatus(dominantFactorAnalysis == null
                ? null
                : dominantFactorAnalysis.status());
        String deltaStatus = analysisStatus(decisionDeltaAnalysis == null ? null : decisionDeltaAnalysis.status());
        String snapshotStatus = analysisStatus(replaySnapshot == null ? null : replaySnapshot.status());
        String traceStatus = analysisStatus(reconstructionTrace == null ? null : reconstructionTrace.status());
        String capsuleStatus = analysisStatus(replayCapsule == null ? null : replayCapsule.status());
        String checklistStatus = analysisStatus(readinessChecklist == null ? null : readinessChecklist.status());
        String sourceMapStatus = analysisStatus(evidenceSourceMap == null ? null : evidenceSourceMap.status());

        String selectedCandidateId = selectedCandidateId(
                decisionVector,
                replaySnapshot,
                reconstructionTrace,
                replayCapsule,
                readinessChecklist,
                evidenceSourceMap);
        int candidateCount = candidateCount(
                decisionVector,
                replaySnapshot,
                reconstructionTrace,
                replayCapsule,
                readinessChecklist,
                evidenceSourceMap);
        String resolvedStrategyId = strategyId(
                strategyId,
                decisionVector,
                replaySnapshot,
                reconstructionTrace,
                replayCapsule,
                readinessChecklist,
                evidenceSourceMap);
        List<String> boundaryTexts = boundaryTexts(
                decisionVector,
                dominantFactorAnalysis,
                decisionDeltaAnalysis,
                replaySnapshot,
                reconstructionTrace,
                replayCapsule,
                readinessChecklist,
                evidenceSourceMap);

        List<DecisionReplayEvidenceBoundarySummaryItemResponse> items = List.of(
                item(
                        "lab-only-boundary",
                        "Lab-only boundary",
                        phraseStatus(boundaryTexts, "lab"),
                        "decisionVector.labProofBoundary, decisionReplayEvidenceSourceMap.boundaryNote",
                        List.of(
                                "decisionVector.labProofBoundary",
                                "dominantFactorAnalysis.boundaryNote",
                                "decisionDeltaAnalysis.boundaryNote",
                                "decisionReplaySnapshot.boundaryNote",
                                "decisionReplayReconstructionTrace.boundaryNote",
                                "decisionReplayCapsule.boundaryNote",
                                "decisionReplayReadinessChecklist.boundaryNote",
                                "decisionReplayEvidenceSourceMap.boundaryNote"),
                        "Boundary text was checked for already-built lab-only evidence framing."),
                item(
                        "read-only-boundary",
                        "Read-only boundary",
                        readOnlyStatus(
                                decisionVector,
                                dominantFactorAnalysis,
                                decisionDeltaAnalysis,
                                replaySnapshot,
                                reconstructionTrace,
                                replayCapsule,
                                readinessChecklist,
                                evidenceSourceMap),
                        "decisionVector.readOnly and downstream evidence readOnly flags",
                        List.of(
                                "decisionVector.readOnly",
                                "dominantFactorAnalysis.readOnly",
                                "decisionDeltaAnalysis.readOnly",
                                "decisionReplaySnapshot.readOnly",
                                "decisionReplayReconstructionTrace.readOnly",
                                "decisionReplayCapsule.readOnly",
                                "decisionReplayReadinessChecklist.readOnly",
                                "decisionReplayEvidenceSourceMap.readOnly"),
                        "Read-only flags were summarized from already-built compare evidence DTOs."),
                item(
                        "no-replay-execution-boundary",
                        "No replay execution boundary",
                        phraseStatus(boundaryTexts, "does not execute replay", "no replay execution"),
                        "decisionVector.replayReadiness, downstream boundaryNote fields",
                        List.of(
                                "decisionVector.replayReadiness",
                                "decisionReplaySnapshot.boundaryNote",
                                "decisionReplayReconstructionTrace.boundaryNote",
                                "decisionReplayCapsule.boundaryNote",
                                "decisionReplayReadinessChecklist.boundaryNote",
                                "decisionReplayEvidenceSourceMap.boundaryNote"),
                        "Existing boundary text states that the compare evidence does not execute replay."),
                item(
                        "no-what-if-mutation-boundary",
                        "No what-if mutation boundary",
                        phraseStatus(boundaryTexts, "what-if mutation", "what-if experiments"),
                        "decisionVector.whatIfReadiness, downstream boundaryNote fields",
                        List.of(
                                "decisionVector.whatIfReadiness",
                                "decisionReplaySnapshot.boundaryNote",
                                "decisionReplayReconstructionTrace.boundaryNote",
                                "decisionReplayCapsule.boundaryNote",
                                "decisionReplayReadinessChecklist.boundaryNote",
                                "decisionReplayEvidenceSourceMap.boundaryNote"),
                        "Existing boundary text states that the compare evidence does not perform what-if mutation."),
                item(
                        "no-persistence-storage-boundary",
                        "No persistence boundary",
                        phraseStatus(boundaryTexts, "persist", "persistent structured decision logging"),
                        "decisionVector.structuredDecisionLoggingReadiness, downstream boundaryNote fields",
                        List.of(
                                "decisionVector.structuredDecisionLoggingReadiness",
                                "decisionReplaySnapshot.boundaryNote",
                                "decisionReplayReconstructionTrace.boundaryNote",
                                "decisionReplayCapsule.boundaryNote",
                                "decisionReplayReadinessChecklist.boundaryNote",
                                "decisionReplayEvidenceSourceMap.boundaryNote"),
                        "Existing boundary text states that the compare evidence is not persisted as audit logs."),
                item(
                        "no-export-share-download-boundary",
                        "No export/share/download boundary",
                        phraseStatus(boundaryTexts, "upload/share/download", "export/pdf/zip"),
                        "decisionReplayCapsule.boundaryNote, decisionReplayReadinessChecklist.boundaryNote, "
                                + "decisionReplayEvidenceSourceMap.boundaryNote",
                        List.of(
                                "decisionReplayCapsule.boundaryNote",
                                "decisionReplayReadinessChecklist.boundaryNote",
                                "decisionReplayEvidenceSourceMap.boundaryNote"),
                        "Existing boundary text states that no upload, share, download, or server-side export "
                                + "behavior is added."),
                item(
                        "no-routing-behavior-change-boundary",
                        "No routing behavior change boundary",
                        phraseStatus(boundaryTexts, "change routing behavior", "without changing routing behavior"),
                        "decisionVector.factorContributionAvailability, downstream boundaryNote fields",
                        List.of(
                                "decisionVector.factorContributionAvailability",
                                "dominantFactorAnalysis.boundaryNote",
                                "decisionDeltaAnalysis.boundaryNote",
                                "decisionReplaySnapshot.boundaryNote",
                                "decisionReplayReconstructionTrace.boundaryNote",
                                "decisionReplayCapsule.boundaryNote",
                                "decisionReplayReadinessChecklist.boundaryNote",
                                "decisionReplayEvidenceSourceMap.boundaryNote"),
                        "Existing boundary text states that routing behavior is not changed."),
                item(
                        "no-score-recomputation-boundary",
                        "No score recomputation boundary",
                        phraseStatus(boundaryTexts, "recompute scores", "hidden scoring is not inferred",
                                "exact production scoring is not claimed"),
                        "decisionVector.factorContributionAvailability, downstream boundaryNote fields",
                        List.of(
                                "decisionVector.factorContributionAvailability",
                                "decisionReplaySnapshot.boundaryNote",
                                "decisionReplayReconstructionTrace.boundaryNote",
                                "decisionReplayCapsule.boundaryNote",
                                "decisionReplayReadinessChecklist.boundaryNote",
                                "decisionReplayEvidenceSourceMap.boundaryNote"),
                        "Existing boundary text states that scores are not recomputed and hidden scoring is not "
                                + "inferred."),
                item(
                        "fingerprint-boundary",
                        "No new fingerprint boundary",
                        fingerprintBoundaryStatus(replaySnapshot, reconstructionTrace, replayCapsule,
                                evidenceSourceMap, boundaryTexts),
                        "decisionReplaySnapshot.fingerprintAlgorithm, "
                                + "decisionReplayReconstructionTrace.fingerprintAlgorithm, "
                                + "decisionReplayCapsule.fingerprintAlgorithm, "
                                + "decisionReplayEvidenceSourceMap.boundaryNote",
                        List.of(
                                "decisionReplaySnapshot.snapshotFingerprint",
                                "decisionReplayReconstructionTrace.traceFingerprint",
                                "decisionReplayCapsule.capsuleFingerprint",
                                "decisionReplayEvidenceSourceMap.linkedReplaySnapshotFingerprint",
                                "decisionReplayEvidenceSourceMap.linkedReconstructionTraceFingerprint",
                                "decisionReplayEvidenceSourceMap.linkedReplayCapsuleFingerprint",
                                "decisionReplayEvidenceSourceMap.boundaryNote"),
                        "Existing fingerprint fields are summarized only as prior evidence; this summary does not "
                                + "generate a new fingerprint."),
                item(
                        "production-not-proven-boundary",
                        "Production not-proven boundary",
                        productionBoundaryStatus(
                                decisionVector,
                                dominantFactorAnalysis,
                                decisionDeltaAnalysis,
                                replaySnapshot,
                                reconstructionTrace,
                                replayCapsule,
                                readinessChecklist,
                                evidenceSourceMap),
                        "productionNotProvenBoundary fields",
                        List.of(
                                "decisionVector.productionNotProvenBoundary",
                                "dominantFactorAnalysis.productionNotProvenBoundary",
                                "decisionDeltaAnalysis.productionNotProvenBoundary",
                                "decisionReplaySnapshot.productionNotProvenBoundary",
                                "decisionReplayReconstructionTrace.productionNotProvenBoundary",
                                "decisionReplayCapsule.productionNotProvenBoundary",
                                "decisionReplayReadinessChecklist.productionNotProvenBoundary",
                                "decisionReplayEvidenceSourceMap.productionNotProvenBoundary"),
                        "Existing production not-proven boundary text was returned by already-built compare "
                                + "evidence DTOs."));
        String status = boundarySummaryStatus(
                items,
                decisionVectorStatus,
                dominantStatus,
                deltaStatus,
                snapshotStatus,
                traceStatus,
                capsuleStatus,
                checklistStatus,
                sourceMapStatus,
                selectedCandidateId,
                candidateCount);

        return new RoutingDecisionReplayEvidenceBoundarySummaryResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                status,
                safeValue(resolvedStrategyId),
                isBlank(selectedCandidateId) ? null : selectedCandidateId,
                candidateCount,
                decisionVectorStatus,
                dominantStatus,
                deltaStatus,
                snapshotStatus,
                traceStatus,
                capsuleStatus,
                checklistStatus,
                sourceMapStatus,
                items,
                explanation(status, resolvedStrategyId, selectedCandidateId, candidateCount, items.size()),
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    private static DecisionReplayEvidenceBoundarySummaryItemResponse item(
            String boundaryId,
            String label,
            String status,
            String sourceFieldPath,
            List<String> supportingEvidenceFieldPaths,
            String evidenceSummary) {
        return new DecisionReplayEvidenceBoundarySummaryItemResponse(
                boundaryId,
                label,
                analysisStatus(status),
                sourceFieldPath,
                supportingEvidenceFieldPaths,
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

    private static String readOnlyStatus(Object... evidence) {
        int present = 0;
        int readOnly = 0;
        for (Object item : evidence) {
            if (item == null) {
                continue;
            }
            present++;
            if (isReadOnly(item)) {
                readOnly++;
            }
        }
        if (present == 0) {
            return STATUS_UNKNOWN;
        }
        if (present == readOnly) {
            return STATUS_AVAILABLE;
        }
        return readOnly == 0 ? STATUS_UNKNOWN : STATUS_PARTIAL;
    }

    private static boolean isReadOnly(Object evidence) {
        if (evidence instanceof RoutingDecisionVectorResponse response) {
            return response.readOnly();
        }
        if (evidence instanceof DominantFactorAnalysisResponse response) {
            return response.readOnly();
        }
        if (evidence instanceof RoutingDecisionDeltaAnalysisResponse response) {
            return response.readOnly();
        }
        if (evidence instanceof RoutingDecisionReplaySnapshotResponse response) {
            return response.readOnly();
        }
        if (evidence instanceof RoutingDecisionReplayReconstructionTraceResponse response) {
            return response.readOnly();
        }
        if (evidence instanceof RoutingDecisionReplayCapsuleResponse response) {
            return response.readOnly();
        }
        if (evidence instanceof RoutingDecisionReplayReadinessChecklistResponse response) {
            return response.readOnly();
        }
        if (evidence instanceof RoutingDecisionReplayEvidenceSourceMapResponse response) {
            return response.readOnly();
        }
        return false;
    }

    private static String phraseStatus(List<String> boundaryTexts, String... phrases) {
        if (boundaryTexts == null || boundaryTexts.isEmpty() || phrases == null || phrases.length == 0) {
            return STATUS_UNKNOWN;
        }
        for (String phrase : phrases) {
            if (!isBlank(phrase) && containsPhrase(boundaryTexts, phrase)) {
                return STATUS_AVAILABLE;
            }
        }
        return STATUS_UNKNOWN;
    }

    private static boolean containsPhrase(List<String> boundaryTexts, String phrase) {
        String normalizedPhrase = phrase.toLowerCase(Locale.ROOT);
        return boundaryTexts.stream()
                .filter(text -> !isBlank(text))
                .map(text -> text.toLowerCase(Locale.ROOT))
                .anyMatch(text -> text.contains(normalizedPhrase));
    }

    private static String fingerprintBoundaryStatus(
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            List<String> boundaryTexts) {
        if (STATUS_AVAILABLE.equals(phraseStatus(boundaryTexts, "generate fingerprints", "new fingerprint"))) {
            return STATUS_AVAILABLE;
        }
        boolean hasPriorFingerprintEvidence = hasText(replaySnapshot == null ? null : replaySnapshot.snapshotFingerprint())
                || hasText(reconstructionTrace == null ? null : reconstructionTrace.traceFingerprint())
                || hasText(replayCapsule == null ? null : replayCapsule.capsuleFingerprint())
                || hasText(evidenceSourceMap == null ? null : evidenceSourceMap.linkedReplaySnapshotFingerprint())
                || hasText(evidenceSourceMap == null ? null
                        : evidenceSourceMap.linkedReconstructionTraceFingerprint())
                || hasText(evidenceSourceMap == null ? null : evidenceSourceMap.linkedReplayCapsuleFingerprint());
        if (hasPriorFingerprintEvidence) {
            return STATUS_PARTIAL;
        }
        return STATUS_UNKNOWN;
    }

    private static String productionBoundaryStatus(
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap) {
        int present = 0;
        int notProven = 0;
        for (String boundary : List.of(
                decisionVector == null ? "" : safeText(decisionVector.productionNotProvenBoundary()),
                dominantFactorAnalysis == null ? "" : safeText(dominantFactorAnalysis.productionNotProvenBoundary()),
                decisionDeltaAnalysis == null ? "" : safeText(decisionDeltaAnalysis.productionNotProvenBoundary()),
                replaySnapshot == null ? "" : safeText(replaySnapshot.productionNotProvenBoundary()),
                reconstructionTrace == null ? "" : safeText(reconstructionTrace.productionNotProvenBoundary()),
                replayCapsule == null ? "" : safeText(replayCapsule.productionNotProvenBoundary()),
                readinessChecklist == null ? "" : safeText(readinessChecklist.productionNotProvenBoundary()),
                evidenceSourceMap == null ? "" : safeText(evidenceSourceMap.productionNotProvenBoundary()))) {
            if (isBlank(boundary)) {
                continue;
            }
            present++;
            String normalized = boundary.toLowerCase(Locale.ROOT);
            if (normalized.contains("no production") || normalized.contains("not production")
                    || normalized.contains("not-proven")) {
                notProven++;
            }
        }
        if (present == 0) {
            return STATUS_UNKNOWN;
        }
        if (present == notProven) {
            return STATUS_AVAILABLE;
        }
        return notProven == 0 ? STATUS_UNKNOWN : STATUS_PARTIAL;
    }

    private static List<String> boundaryTexts(
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap) {
        List<String> texts = new ArrayList<>();
        addText(texts, decisionVector == null ? null : decisionVector.labProofBoundary());
        addText(texts, decisionVector == null ? null : decisionVector.productionNotProvenBoundary());
        addText(texts, decisionVector == null ? null : decisionVector.factorContributionAvailability());
        addText(texts, decisionVector == null ? null : decisionVector.replayReadiness());
        addText(texts, decisionVector == null ? null : decisionVector.whatIfReadiness());
        addText(texts, decisionVector == null ? null : decisionVector.structuredDecisionLoggingReadiness());
        addText(texts, dominantFactorAnalysis == null ? null : dominantFactorAnalysis.boundaryNote());
        addText(texts, dominantFactorAnalysis == null ? null : dominantFactorAnalysis.productionNotProvenBoundary());
        addText(texts, decisionDeltaAnalysis == null ? null : decisionDeltaAnalysis.boundaryNote());
        addText(texts, decisionDeltaAnalysis == null ? null : decisionDeltaAnalysis.productionNotProvenBoundary());
        addText(texts, replaySnapshot == null ? null : replaySnapshot.boundaryNote());
        addText(texts, replaySnapshot == null ? null : replaySnapshot.productionNotProvenBoundary());
        addText(texts, reconstructionTrace == null ? null : reconstructionTrace.boundaryNote());
        addText(texts, reconstructionTrace == null ? null : reconstructionTrace.productionNotProvenBoundary());
        addText(texts, replayCapsule == null ? null : replayCapsule.boundaryNote());
        addText(texts, replayCapsule == null ? null : replayCapsule.productionNotProvenBoundary());
        addText(texts, readinessChecklist == null ? null : readinessChecklist.boundaryNote());
        addText(texts, readinessChecklist == null ? null : readinessChecklist.productionNotProvenBoundary());
        addText(texts, evidenceSourceMap == null ? null : evidenceSourceMap.boundaryNote());
        addText(texts, evidenceSourceMap == null ? null : evidenceSourceMap.productionNotProvenBoundary());
        if (evidenceSourceMap != null && evidenceSourceMap.sourceMapEntries() != null) {
            evidenceSourceMap.sourceMapEntries().forEach(entry -> addText(texts, entry.boundaryNote()));
        }
        return List.copyOf(texts);
    }

    private static void addText(List<String> texts, String value) {
        if (!isBlank(value)) {
            texts.add(value.trim());
        }
    }

    private static String strategyId(
            String strategyId,
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap) {
        return firstNonBlank(
                evidenceSourceMap == null ? null : evidenceSourceMap.strategyId(),
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
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap) {
        String candidateId = firstNonBlank(
                evidenceSourceMap == null ? null : evidenceSourceMap.selectedCandidateId(),
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
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap) {
        if (evidenceSourceMap != null && evidenceSourceMap.candidateCount() > 0) {
            return evidenceSourceMap.candidateCount();
        }
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

    private static String boundarySummaryStatus(
            List<DecisionReplayEvidenceBoundarySummaryItemResponse> items,
            String decisionVectorStatus,
            String dominantStatus,
            String deltaStatus,
            String snapshotStatus,
            String traceStatus,
            String capsuleStatus,
            String checklistStatus,
            String sourceMapStatus,
            String selectedCandidateId,
            int candidateCount) {
        if (STATUS_UNKNOWN.equals(decisionVectorStatus)
                && STATUS_UNKNOWN.equals(dominantStatus)
                && STATUS_UNKNOWN.equals(deltaStatus)
                && STATUS_UNKNOWN.equals(snapshotStatus)
                && STATUS_UNKNOWN.equals(traceStatus)
                && STATUS_UNKNOWN.equals(capsuleStatus)
                && STATUS_UNKNOWN.equals(checklistStatus)
                && STATUS_UNKNOWN.equals(sourceMapStatus)
                && isBlank(selectedCandidateId)
                && candidateCount == 0) {
            return STATUS_UNKNOWN;
        }
        if (items.stream().allMatch(item -> STATUS_AVAILABLE.equals(item.status()))) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
    }

    private static String explanation(
            String status,
            String strategyId,
            String selectedCandidateId,
            int candidateCount,
            int boundaryItemCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision Replay Evidence Boundary Summary is UNKNOWN because selected candidate, candidate set, "
                    + "and already-built lab evidence lane statuses were not returned. No replay execution, "
                    + "what-if mutation, persisted boundary summary, selected candidate, candidate set, alternative, "
                    + "score gap, factor values, replay claim, production proof claim, or explanation "
                    + "evidence is invented.";
        }
        return "Decision Replay Evidence Boundary Summary is " + status + " for strategy "
                + safeValue(strategyId) + ": selected candidate " + safeValue(selectedCandidateId) + ", "
                + candidateCount + " candidate id(s), and " + boundaryItemCount
                + " deterministic boundary item(s) were derived from already-built lab compare evidence boundary "
                + "fields and statuses only. This summary reports existing lab-only and not-proven boundaries "
                + "without executing replay, performing what-if mutation, persisting data, or generating a "
                + "fingerprint.";
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

    private static String safeText(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private static boolean hasText(String value) {
        return !isBlank(value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
