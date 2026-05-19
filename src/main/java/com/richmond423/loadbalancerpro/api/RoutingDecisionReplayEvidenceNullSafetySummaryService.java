package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RoutingDecisionReplayEvidenceNullSafetySummaryService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-null-safety-summary/v1";
    private static final String SOURCE =
            "/api/routing/compare already-built lab evidence null-safety fields: decisionVector, "
                    + "dominantFactorAnalysis, decisionDeltaAnalysis, decisionReplaySnapshot, "
                    + "decisionReplayReconstructionTrace, decisionReplayCapsule, "
                    + "decisionReplayReadinessChecklist, decisionReplayEvidenceSourceMap, "
                    + "decisionReplayEvidenceBoundarySummary, and decisionReplayEvidenceFieldInventory";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision Evidence Null-Safety Summary is read-only lab null-safety metadata derived only from "
                    + "already-built routing compare response objects; it does not inspect raw server input, "
                    + "does not inspect raw request payloads, does not execute replay, does not perform what-if "
                    + "mutation, does not persist null-safety data or audit logs, does not generate fingerprints, "
                    + "does not use reflective field inspection, does not recompute scores, does not retune "
                    + "weights, does not change routing behavior, does not add telemetry, does not add external "
                    + "calls, does not add upload/share/download flows, and does not add server-side export/PDF/ZIP "
                    + "generation.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "This null-safety summary is not production certification, not live-cloud proof, not real-tenant proof, "
                    + "not SLA/SLO proof, not registry publication proof, not signing proof, not governance "
                    + "application proof, not exact production scoring proof, not cryptographic production proof, "
                    + "not guaranteed replay, and not production traffic validation.";

    public RoutingDecisionReplayEvidenceNullSafetySummaryResponse nullSafetySummary(
            String strategyId,
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory) {
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
        String boundarySummaryStatus = analysisStatus(boundarySummary == null ? null : boundarySummary.status());
        String fieldInventoryStatus = analysisStatus(fieldInventory == null ? null : fieldInventory.status());

        List<DecisionReplayEvidenceNullSafetyItemResponse> items = List.of(
                selectedCandidateEntry(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule,
                        readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory),
                candidateSetEntry(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule,
                        readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory),
                scoreGapEntry(decisionDeltaAnalysis, replaySnapshot, reconstructionTrace, replayCapsule),
                closestAlternativeEntry(decisionDeltaAnalysis, replaySnapshot, reconstructionTrace, replayCapsule),
                largestDeltaFactorEntry(decisionDeltaAnalysis, replaySnapshot, reconstructionTrace, replayCapsule),
                linkedFingerprintEntry(replaySnapshot, reconstructionTrace, replayCapsule, readinessChecklist,
                        evidenceSourceMap),
                candidateEvidenceEntry(replayCapsule, fieldInventory),
                factorEvidenceEntry(replayCapsule, fieldInventory),
                fieldInventoryEntry(fieldInventory),
                noHealthyPathEntry(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule,
                        readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory,
                        decisionVectorStatus, dominantStatus, deltaStatus, snapshotStatus, traceStatus,
                        capsuleStatus, checklistStatus, sourceMapStatus, boundarySummaryStatus,
                        fieldInventoryStatus),
                boundaryTextEntry(decisionVector, dominantFactorAnalysis, decisionDeltaAnalysis, replaySnapshot,
                        reconstructionTrace, replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary,
                        fieldInventory),
                productionBoundaryEntry(decisionVector, dominantFactorAnalysis, decisionDeltaAnalysis, replaySnapshot,
                        reconstructionTrace, replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary,
                        fieldInventory));

        int availableCount = countStatus(items, STATUS_AVAILABLE);
        int partialCount = countStatus(items, STATUS_PARTIAL);
        int unknownCount = countStatus(items, STATUS_UNKNOWN);
        String selectedCandidateId = selectedCandidateId(decisionVector, replaySnapshot, reconstructionTrace,
                replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory);
        int candidateCount = candidateCount(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule,
                readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory);
        String resolvedStrategyId = strategyId(strategyId, decisionVector, replaySnapshot, reconstructionTrace,
                replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory);
        String status = summaryStatus(
                items,
                decisionVectorStatus,
                dominantStatus,
                deltaStatus,
                snapshotStatus,
                traceStatus,
                capsuleStatus,
                checklistStatus,
                sourceMapStatus,
                boundarySummaryStatus,
                fieldInventoryStatus,
                selectedCandidateId,
                candidateCount);

        return new RoutingDecisionReplayEvidenceNullSafetySummaryResponse(
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
                boundarySummaryStatus,
                fieldInventoryStatus,
                availableCount,
                partialCount,
                unknownCount,
                items,
                explanation(status, resolvedStrategyId, selectedCandidateId, candidateCount, items.size()),
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    private static DecisionReplayEvidenceNullSafetyItemResponse selectedCandidateEntry(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory) {
        return entry(
                "selected-candidate-null-safety",
                "Selected candidate null-safety",
                "selected candidate fields across already-built compare evidence",
                List.of(
                        field("decisionVector.selectedBackend",
                                decisionVector != null && hasText(decisionVector.selectedBackend())),
                        field("decisionReplaySnapshot.selectedCandidateId",
                                replaySnapshot != null && hasText(replaySnapshot.selectedCandidateId())),
                        field("decisionReplayReconstructionTrace.selectedCandidateId",
                                reconstructionTrace != null && hasText(reconstructionTrace.selectedCandidateId())),
                        field("decisionReplayCapsule.selectedCandidateId",
                                replayCapsule != null && hasText(replayCapsule.selectedCandidateId())),
                        field("decisionReplayReadinessChecklist.selectedCandidateId",
                                readinessChecklist != null && hasText(readinessChecklist.selectedCandidateId())),
                        field("decisionReplayEvidenceSourceMap.selectedCandidateId",
                                evidenceSourceMap != null && hasText(evidenceSourceMap.selectedCandidateId())),
                        field("decisionReplayEvidenceBoundarySummary.selectedCandidateId",
                                boundarySummary != null && hasText(boundarySummary.selectedCandidateId())),
                        field("decisionReplayEvidenceFieldInventory.selectedCandidateId",
                                fieldInventory != null && hasText(fieldInventory.selectedCandidateId()))),
                "Selected candidate null-safety is summarized from already-built selected-candidate fields only; "
                        + "no selected candidate value is invented.");
    }

    private static DecisionReplayEvidenceNullSafetyItemResponse candidateSetEntry(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory) {
        return entry(
                "candidate-set-null-safety",
                "Candidate set null-safety",
                "candidate count and candidate-id fields across already-built compare evidence",
                List.of(
                        field("decisionVector.candidateCount",
                                decisionVector != null && decisionVector.candidateCount() > 0),
                        field("decisionVector.candidateSummaries",
                                decisionVector != null && hasItems(decisionVector.candidateSummaries())),
                        field("decisionReplaySnapshot.candidateIdsConsidered",
                                replaySnapshot != null && hasItems(replaySnapshot.candidateIdsConsidered())),
                        field("decisionReplaySnapshot.candidateCount",
                                replaySnapshot != null && replaySnapshot.candidateCount() > 0),
                        field("decisionReplayReconstructionTrace.candidateIdsConsidered",
                                reconstructionTrace != null && hasItems(reconstructionTrace.candidateIdsConsidered())),
                        field("decisionReplayReconstructionTrace.candidateFinalScores",
                                reconstructionTrace != null && hasItems(reconstructionTrace.candidateFinalScores())),
                        field("decisionReplayReconstructionTrace.candidateCount",
                                reconstructionTrace != null && reconstructionTrace.candidateCount() > 0),
                        field("decisionReplayCapsule.candidateIdsConsidered",
                                replayCapsule != null && hasItems(replayCapsule.candidateIdsConsidered())),
                        field("decisionReplayCapsule.candidateCount",
                                replayCapsule != null && replayCapsule.candidateCount() > 0),
                        field("decisionReplayReadinessChecklist.candidateCount",
                                readinessChecklist != null && readinessChecklist.candidateCount() > 0),
                        field("decisionReplayEvidenceSourceMap.candidateCount",
                                evidenceSourceMap != null && evidenceSourceMap.candidateCount() > 0),
                        field("decisionReplayEvidenceBoundarySummary.candidateCount",
                                boundarySummary != null && boundarySummary.candidateCount() > 0),
                        field("decisionReplayEvidenceFieldInventory.candidateCount",
                                fieldInventory != null && fieldInventory.candidateCount() > 0)),
                "Candidate set null-safety is summarized from already-built candidate count and candidate-id fields "
                        + "only; candidate sets are not invented.");
    }

    private static DecisionReplayEvidenceNullSafetyItemResponse scoreGapEntry(
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule) {
        return entry(
                "score-gap-null-safety",
                "Score gap null-safety",
                "final score gap fields across already-built compare evidence",
                List.of(
                        field("decisionDeltaAnalysis.comparison.finalScoreGap",
                                decisionDeltaAnalysis != null && decisionDeltaAnalysis.comparison() != null
                                        && isFinite(decisionDeltaAnalysis.comparison().finalScoreGap())),
                        field("decisionReplaySnapshot.finalScoreGap",
                                replaySnapshot != null && isFinite(replaySnapshot.finalScoreGap())),
                        field("decisionReplayReconstructionTrace.finalScoreGap",
                                reconstructionTrace != null && isFinite(reconstructionTrace.finalScoreGap())),
                        field("decisionReplayCapsule.finalScoreGap",
                                replayCapsule != null && isFinite(replayCapsule.finalScoreGap()))),
                "Score gap null-safety is summarized from already-built score gap fields only; gaps are not "
                        + "computed or invented.");
    }

    private static DecisionReplayEvidenceNullSafetyItemResponse closestAlternativeEntry(
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule) {
        return entry(
                "closest-alternative-null-safety",
                "Closest alternative null-safety",
                "closest-alternative fields across already-built compare evidence",
                List.of(
                        field("decisionDeltaAnalysis.comparison.closestAlternativeCandidateId",
                                decisionDeltaAnalysis != null && decisionDeltaAnalysis.comparison() != null
                                        && hasText(decisionDeltaAnalysis.comparison().closestAlternativeCandidateId())),
                        field("decisionReplaySnapshot.closestAlternativeCandidateId",
                                replaySnapshot != null && hasText(replaySnapshot.closestAlternativeCandidateId())),
                        field("decisionReplayReconstructionTrace.closestAlternativeCandidateId",
                                reconstructionTrace != null
                                        && hasText(reconstructionTrace.closestAlternativeCandidateId())),
                        field("decisionReplayCapsule.closestAlternativeCandidateId",
                                replayCapsule != null && hasText(replayCapsule.closestAlternativeCandidateId()))),
                "Closest alternative null-safety is summarized from already-built closest-alternative fields only; "
                        + "alternative candidates are not invented.");
    }

    private static DecisionReplayEvidenceNullSafetyItemResponse largestDeltaFactorEntry(
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule) {
        return entry(
                "largest-delta-factor-null-safety",
                "Largest delta factor null-safety",
                "largest-delta-factor fields across already-built compare evidence",
                List.of(
                        field("decisionDeltaAnalysis.largestAbsoluteFactorDelta.factorName",
                                decisionDeltaAnalysis != null
                                        && decisionDeltaAnalysis.largestAbsoluteFactorDelta() != null
                                        && hasText(decisionDeltaAnalysis.largestAbsoluteFactorDelta().factorName())),
                        field("decisionReplaySnapshot.largestDeltaFactorName",
                                replaySnapshot != null && hasText(replaySnapshot.largestDeltaFactorName())),
                        field("decisionReplayReconstructionTrace.largestDeltaFactorName",
                                reconstructionTrace != null && hasText(reconstructionTrace.largestDeltaFactorName())),
                        field("decisionReplayCapsule.largestDeltaFactorName",
                                replayCapsule != null && hasText(replayCapsule.largestDeltaFactorName()))),
                "Largest delta factor null-safety is summarized from already-built factor-name fields only; factor "
                        + "names and values are not invented.");
    }

    private static DecisionReplayEvidenceNullSafetyItemResponse linkedFingerprintEntry(
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap) {
        return entry(
                "linked-fingerprint-null-safety",
                "Linked fingerprint null-safety",
                "already-present snapshot, trace, and capsule fingerprint fields",
                List.of(
                        field("decisionReplaySnapshot.snapshotFingerprint",
                                replaySnapshot != null && safeFingerprint(replaySnapshot.status(),
                                        replaySnapshot.snapshotFingerprint())),
                        field("decisionReplayReconstructionTrace.traceFingerprint",
                                reconstructionTrace != null && safeFingerprint(reconstructionTrace.status(),
                                        reconstructionTrace.traceFingerprint())),
                        field("decisionReplayReconstructionTrace.snapshotFingerprint",
                                reconstructionTrace != null && safeFingerprint(reconstructionTrace.status(),
                                        reconstructionTrace.snapshotFingerprint())),
                        field("decisionReplayCapsule.capsuleFingerprint",
                                replayCapsule != null && safeFingerprint(replayCapsule.status(),
                                        replayCapsule.capsuleFingerprint())),
                        field("decisionReplayCapsule.linkedReplaySnapshotFingerprint",
                                replayCapsule != null && safeFingerprint(replayCapsule.status(),
                                        replayCapsule.linkedReplaySnapshotFingerprint())),
                        field("decisionReplayCapsule.linkedReconstructionTraceFingerprint",
                                replayCapsule != null && safeFingerprint(replayCapsule.status(),
                                        replayCapsule.linkedReconstructionTraceFingerprint())),
                        field("decisionReplayReadinessChecklist.linkedReplaySnapshotFingerprint",
                                readinessChecklist != null && safeFingerprint(readinessChecklist.status(),
                                        readinessChecklist.linkedReplaySnapshotFingerprint())),
                        field("decisionReplayReadinessChecklist.linkedReconstructionTraceFingerprint",
                                readinessChecklist != null && safeFingerprint(readinessChecklist.status(),
                                        readinessChecklist.linkedReconstructionTraceFingerprint())),
                        field("decisionReplayReadinessChecklist.linkedReplayCapsuleFingerprint",
                                readinessChecklist != null && safeFingerprint(readinessChecklist.status(),
                                        readinessChecklist.linkedReplayCapsuleFingerprint())),
                        field("decisionReplayEvidenceSourceMap.linkedReplaySnapshotFingerprint",
                                evidenceSourceMap != null && safeFingerprint(evidenceSourceMap.status(),
                                        evidenceSourceMap.linkedReplaySnapshotFingerprint())),
                        field("decisionReplayEvidenceSourceMap.linkedReconstructionTraceFingerprint",
                                evidenceSourceMap != null && safeFingerprint(evidenceSourceMap.status(),
                                        evidenceSourceMap.linkedReconstructionTraceFingerprint())),
                        field("decisionReplayEvidenceSourceMap.linkedReplayCapsuleFingerprint",
                                evidenceSourceMap != null && safeFingerprint(evidenceSourceMap.status(),
                                        evidenceSourceMap.linkedReplayCapsuleFingerprint()))),
                "Linked fingerprint null-safety is summarized from already-present fingerprints only; no "
                        + "null-safety fingerprint is generated and no cryptographic production proof is claimed.");
    }

    private static DecisionReplayEvidenceNullSafetyItemResponse candidateEvidenceEntry(
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory) {
        return entry(
                "candidate-evidence-null-safety",
                "Candidate evidence null-safety",
                "capsule candidate evidence and field-inventory candidate evidence paths",
                List.of(
                        field("decisionReplayCapsule.candidateEvidence",
                                replayCapsule != null && hasItems(replayCapsule.candidateEvidence())),
                        field("decisionReplayEvidenceFieldInventory.inventoryEntries.replay-capsule-fields"
                                        + ".observedFieldPaths[decisionReplayCapsule.candidateEvidence]",
                                hasObservedInventoryField(fieldInventory, "decisionReplayCapsule.candidateEvidence")),
                        field("decisionReplayEvidenceFieldInventory.inventoryEntries.decision-vector-fields"
                                        + ".observedFieldPaths[decisionVector.candidateSummaries]",
                                hasObservedInventoryField(fieldInventory, "decisionVector.candidateSummaries"))),
                "Candidate evidence null-safety is summarized from already-built capsule and field-inventory fields "
                        + "only; candidate evidence is not fabricated.");
    }

    private static DecisionReplayEvidenceNullSafetyItemResponse factorEvidenceEntry(
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory) {
        return entry(
                "factor-evidence-null-safety",
                "Factor evidence null-safety",
                "capsule factor evidence and field-inventory factor evidence paths",
                List.of(
                        field("decisionReplayCapsule.factorEvidence",
                                replayCapsule != null && hasItems(replayCapsule.factorEvidence())),
                        field("decisionReplayCapsule.reconstructionStepIds",
                                replayCapsule != null && hasItems(replayCapsule.reconstructionStepIds())),
                        field("decisionReplayEvidenceFieldInventory.inventoryEntries.replay-capsule-fields"
                                        + ".observedFieldPaths[decisionReplayCapsule.factorEvidence]",
                                hasObservedInventoryField(fieldInventory, "decisionReplayCapsule.factorEvidence")),
                        field("decisionReplayEvidenceFieldInventory.inventoryEntries.decision-vector-fields"
                                        + ".observedFieldPaths[decisionVector.candidateSummaries]",
                                hasObservedInventoryField(fieldInventory, "decisionVector.candidateSummaries"))),
                "Factor evidence null-safety is summarized from already-built capsule and field-inventory fields "
                        + "only; factor evidence is not fabricated.");
    }

    private static DecisionReplayEvidenceNullSafetyItemResponse fieldInventoryEntry(
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory) {
        return entry(
                "field-inventory-null-safety",
                "Field inventory null-safety",
                "field inventory observed and missing/unavailable field metadata",
                List.of(
                        field("decisionReplayEvidenceFieldInventory.status",
                                fieldInventory != null && hasText(fieldInventory.status())),
                        field("decisionReplayEvidenceFieldInventory.inventoryEntries",
                                fieldInventory != null && hasItems(fieldInventory.inventoryEntries())),
                        field("decisionReplayEvidenceFieldInventory.availableInventoryGroupCount",
                                fieldInventory != null && fieldInventory.availableInventoryGroupCount() >= 0),
                        field("decisionReplayEvidenceFieldInventory.partialInventoryGroupCount",
                                fieldInventory != null && fieldInventory.partialInventoryGroupCount() >= 0),
                        field("decisionReplayEvidenceFieldInventory.unknownInventoryGroupCount",
                                fieldInventory != null && fieldInventory.unknownInventoryGroupCount() >= 0),
                        field("decisionReplayEvidenceFieldInventory.inventoryEntries.observedFieldPaths",
                                hasAnyObservedInventoryField(fieldInventory)),
                        field("decisionReplayEvidenceFieldInventory.inventoryEntries.missingOrUnavailableFieldPaths",
                                hasAnyUnavailableInventoryField(fieldInventory))),
                "Field inventory null-safety is summarized from already-built field-inventory entries only; raw "
                        + "input is not inspected and reflective field inspection is not used.");
    }

    private static DecisionReplayEvidenceNullSafetyItemResponse noHealthyPathEntry(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory,
            String decisionVectorStatus,
            String dominantStatus,
            String deltaStatus,
            String snapshotStatus,
            String traceStatus,
            String capsuleStatus,
            String checklistStatus,
            String sourceMapStatus,
            String boundarySummaryStatus,
            String fieldInventoryStatus) {
        boolean sourceStatusesUnknown = allUnknown(decisionVectorStatus, dominantStatus, deltaStatus, snapshotStatus,
                traceStatus, capsuleStatus, checklistStatus, sourceMapStatus, boundarySummaryStatus,
                fieldInventoryStatus);
        return entry(
                "no-healthy-path-null-safety",
                "No-healthy path null-safety",
                "no-healthy/failure-path selected candidate, candidate set, gap, factor, and fingerprint fields",
                List.of(
                        field("decisionVector", decisionVector == null && sourceStatusesUnknown),
                        field("decisionReplaySnapshot.selectedCandidateId",
                                replaySnapshot != null && replaySnapshot.selectedCandidateId() == null),
                        field("decisionReplaySnapshot.candidateIdsConsidered",
                                replaySnapshot != null && replaySnapshot.candidateIdsConsidered().isEmpty()),
                        field("decisionReplaySnapshot.candidateCount",
                                replaySnapshot != null && replaySnapshot.candidateCount() == 0),
                        field("decisionReplaySnapshot.closestAlternativeCandidateId",
                                replaySnapshot != null && replaySnapshot.closestAlternativeCandidateId() == null),
                        field("decisionReplaySnapshot.finalScoreGap",
                                replaySnapshot != null && replaySnapshot.finalScoreGap() == null),
                        field("decisionReplaySnapshot.largestDeltaFactorName",
                                replaySnapshot != null && replaySnapshot.largestDeltaFactorName() == null),
                        field("decisionReplaySnapshot.snapshotFingerprint",
                                replaySnapshot != null
                                        && !safeFingerprint(replaySnapshot.status(), replaySnapshot.snapshotFingerprint())),
                        field("decisionReplayReconstructionTrace.traceFingerprint",
                                reconstructionTrace != null
                                        && !safeFingerprint(reconstructionTrace.status(),
                                                reconstructionTrace.traceFingerprint())),
                        field("decisionReplayCapsule.capsuleFingerprint",
                                replayCapsule != null
                                        && !safeFingerprint(replayCapsule.status(), replayCapsule.capsuleFingerprint())),
                        field("decisionReplayReadinessChecklist.linkedReplayCapsuleFingerprint",
                                readinessChecklist != null
                                        && readinessChecklist.linkedReplayCapsuleFingerprint() == null),
                        field("decisionReplayEvidenceSourceMap.linkedReplayCapsuleFingerprint",
                                evidenceSourceMap != null && evidenceSourceMap.linkedReplayCapsuleFingerprint() == null),
                        field("decisionReplayEvidenceBoundarySummary.selectedCandidateId",
                                boundarySummary != null && boundarySummary.selectedCandidateId() == null),
                        field("decisionReplayEvidenceFieldInventory.selectedCandidateId",
                                fieldInventory != null && fieldInventory.selectedCandidateId() == null),
                        field("sourceStatuses.UNKNOWN", sourceStatusesUnknown)),
                "No-healthy path null-safety summarizes safe null, empty, and UNKNOWN evidence behavior only; it "
                        + "does not turn failure-path behavior into replay, certification, or production proof.");
    }

    private static DecisionReplayEvidenceNullSafetyItemResponse boundaryTextEntry(
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory) {
        return entry(
                "boundary-text-null-safety",
                "Boundary text null-safety",
                "read-only lab boundary text across already-built compare evidence",
                List.of(
                        field("decisionVector.labProofBoundary",
                                decisionVector != null && hasText(decisionVector.labProofBoundary())),
                        field("decisionVector.exactnessBoundary",
                                decisionVector != null && hasText(decisionVector.exactnessBoundary())),
                        field("dominantFactorAnalysis.boundaryNote",
                                dominantFactorAnalysis != null && hasText(dominantFactorAnalysis.boundaryNote())),
                        field("decisionDeltaAnalysis.boundaryNote",
                                decisionDeltaAnalysis != null && hasText(decisionDeltaAnalysis.boundaryNote())),
                        field("decisionReplaySnapshot.boundaryNote",
                                replaySnapshot != null && hasText(replaySnapshot.boundaryNote())),
                        field("decisionReplayReconstructionTrace.boundaryNote",
                                reconstructionTrace != null && hasText(reconstructionTrace.boundaryNote())),
                        field("decisionReplayCapsule.boundaryNote",
                                replayCapsule != null && hasText(replayCapsule.boundaryNote())),
                        field("decisionReplayReadinessChecklist.boundaryNote",
                                readinessChecklist != null && hasText(readinessChecklist.boundaryNote())),
                        field("decisionReplayEvidenceSourceMap.boundaryNote",
                                evidenceSourceMap != null && hasText(evidenceSourceMap.boundaryNote())),
                        field("decisionReplayEvidenceBoundarySummary.boundaryNote",
                                boundarySummary != null && hasText(boundarySummary.boundaryNote())),
                        field("decisionReplayEvidenceFieldInventory.boundaryNote",
                                fieldInventory != null && hasText(fieldInventory.boundaryNote()))),
                "Boundary text null-safety summarizes existing lab-only and read-only boundary text; it does not "
                        + "add approval, remediation, enforcement, or production-readiness language.");
    }

    private static DecisionReplayEvidenceNullSafetyItemResponse productionBoundaryEntry(
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory) {
        return entry(
                "production-not-proven-null-safety",
                "Production not-proven null-safety",
                "productionNotProvenBoundary fields across already-built compare evidence",
                List.of(
                        field("decisionVector.productionNotProvenBoundary",
                                hasNotProvenText(decisionVector == null
                                        ? null
                                        : decisionVector.productionNotProvenBoundary())),
                        field("dominantFactorAnalysis.productionNotProvenBoundary",
                                hasNotProvenText(dominantFactorAnalysis == null
                                        ? null
                                        : dominantFactorAnalysis.productionNotProvenBoundary())),
                        field("decisionDeltaAnalysis.productionNotProvenBoundary",
                                hasNotProvenText(decisionDeltaAnalysis == null
                                        ? null
                                        : decisionDeltaAnalysis.productionNotProvenBoundary())),
                        field("decisionReplaySnapshot.productionNotProvenBoundary",
                                hasNotProvenText(replaySnapshot == null
                                        ? null
                                        : replaySnapshot.productionNotProvenBoundary())),
                        field("decisionReplayReconstructionTrace.productionNotProvenBoundary",
                                hasNotProvenText(reconstructionTrace == null
                                        ? null
                                        : reconstructionTrace.productionNotProvenBoundary())),
                        field("decisionReplayCapsule.productionNotProvenBoundary",
                                hasNotProvenText(replayCapsule == null
                                        ? null
                                        : replayCapsule.productionNotProvenBoundary())),
                        field("decisionReplayReadinessChecklist.productionNotProvenBoundary",
                                hasNotProvenText(readinessChecklist == null
                                        ? null
                                        : readinessChecklist.productionNotProvenBoundary())),
                        field("decisionReplayEvidenceSourceMap.productionNotProvenBoundary",
                                hasNotProvenText(evidenceSourceMap == null
                                        ? null
                                        : evidenceSourceMap.productionNotProvenBoundary())),
                        field("decisionReplayEvidenceBoundarySummary.productionNotProvenBoundary",
                                hasNotProvenText(boundarySummary == null
                                        ? null
                                        : boundarySummary.productionNotProvenBoundary())),
                        field("decisionReplayEvidenceFieldInventory.productionNotProvenBoundary",
                                hasNotProvenText(fieldInventory == null
                                        ? null
                                        : fieldInventory.productionNotProvenBoundary()))),
                "Production not-proven null-safety preserves existing not-proven boundary text, including not "
                        + "production certification and not guaranteed replay.");
    }

    private static DecisionReplayEvidenceNullSafetyItemResponse entry(
            String nullSafetyId,
            String label,
            String sourceFieldPath,
            List<FieldAvailability> fields,
            String safetySummary) {
        List<String> checked = checked(fields);
        List<String> unavailable = unavailable(fields);
        String status = itemStatus(checked, unavailable);
        return new DecisionReplayEvidenceNullSafetyItemResponse(
                nullSafetyId,
                label,
                status,
                sourceFieldPath,
                checked,
                unavailable,
                checked.size(),
                unavailable.size(),
                safetySummary,
                BOUNDARY_NOTE);
    }

    private static FieldAvailability field(String fieldPath, boolean available) {
        return new FieldAvailability(fieldPath, available);
    }

    private static List<String> checked(List<FieldAvailability> fields) {
        List<String> checked = new ArrayList<>();
        for (FieldAvailability field : fields) {
            if (field.available()) {
                checked.add(field.fieldPath());
            }
        }
        return List.copyOf(checked);
    }

    private static List<String> unavailable(List<FieldAvailability> fields) {
        List<String> unavailable = new ArrayList<>();
        for (FieldAvailability field : fields) {
            if (!field.available()) {
                unavailable.add(field.fieldPath());
            }
        }
        return List.copyOf(unavailable);
    }

    private static String itemStatus(List<String> checked, List<String> unavailable) {
        if (checked.isEmpty()) {
            return STATUS_UNKNOWN;
        }
        if (unavailable.isEmpty()) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
    }

    private static String summaryStatus(
            List<DecisionReplayEvidenceNullSafetyItemResponse> items,
            String decisionVectorStatus,
            String dominantStatus,
            String deltaStatus,
            String snapshotStatus,
            String traceStatus,
            String capsuleStatus,
            String checklistStatus,
            String sourceMapStatus,
            String boundarySummaryStatus,
            String fieldInventoryStatus,
            String selectedCandidateId,
            int candidateCount) {
        if (allUnknown(decisionVectorStatus, dominantStatus, deltaStatus, snapshotStatus, traceStatus, capsuleStatus,
                checklistStatus, sourceMapStatus, boundarySummaryStatus, fieldInventoryStatus)
                && isBlank(selectedCandidateId)
                && candidateCount == 0) {
            return STATUS_UNKNOWN;
        }
        if (items.stream().allMatch(item -> STATUS_AVAILABLE.equals(item.status()))) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
    }

    private static boolean allUnknown(String... statuses) {
        for (String status : statuses) {
            if (!STATUS_UNKNOWN.equals(analysisStatus(status))) {
                return false;
            }
        }
        return true;
    }

    private static int countStatus(List<DecisionReplayEvidenceNullSafetyItemResponse> items, String status) {
        int count = 0;
        for (DecisionReplayEvidenceNullSafetyItemResponse item : items) {
            if (status.equals(item.status())) {
                count++;
            }
        }
        return count;
    }

    private static String decisionVectorStatus(RoutingDecisionVectorResponse decisionVector) {
        if (decisionVector == null || !hasItems(decisionVector.candidateSummaries())) {
            return STATUS_UNKNOWN;
        }
        return STATUS_AVAILABLE;
    }

    private static String analysisStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_UNKNOWN;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (STATUS_AVAILABLE.equals(normalized) || STATUS_PARTIAL.equals(normalized)
                || STATUS_UNKNOWN.equals(normalized)) {
            return normalized;
        }
        return STATUS_UNKNOWN;
    }

    private static boolean safeFingerprint(String sourceStatus, String value) {
        return !STATUS_UNKNOWN.equals(analysisStatus(sourceStatus)) && hasText(value);
    }

    private static boolean hasObservedInventoryField(
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory,
            String fieldPath) {
        if (fieldInventory == null || fieldPath == null) {
            return false;
        }
        for (DecisionReplayEvidenceFieldInventoryEntryResponse entry : fieldInventory.inventoryEntries()) {
            if (entry.observedFieldPaths().contains(fieldPath)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyObservedInventoryField(
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory) {
        if (fieldInventory == null) {
            return false;
        }
        for (DecisionReplayEvidenceFieldInventoryEntryResponse entry : fieldInventory.inventoryEntries()) {
            if (hasItems(entry.observedFieldPaths())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyUnavailableInventoryField(
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory) {
        if (fieldInventory == null) {
            return false;
        }
        for (DecisionReplayEvidenceFieldInventoryEntryResponse entry : fieldInventory.inventoryEntries()) {
            if (hasItems(entry.missingOrUnavailableFieldPaths())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNotProvenText(String value) {
        if (!hasText(value)) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("not production certification")
                && normalized.contains("not guaranteed replay");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean hasItems(List<?> values) {
        return values != null && !values.isEmpty();
    }

    private static boolean hasItems(Map<?, ?> values) {
        return values != null && !values.isEmpty();
    }

    private static boolean isFinite(Double value) {
        return value != null && Double.isFinite(value);
    }

    private static String safeValue(String value) {
        return isBlank(value) ? "UNKNOWN" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String selectedCandidateId(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory) {
        return firstNonBlank(
                decisionVector == null ? null : decisionVector.selectedBackend(),
                replaySnapshot == null ? null : replaySnapshot.selectedCandidateId(),
                reconstructionTrace == null ? null : reconstructionTrace.selectedCandidateId(),
                replayCapsule == null ? null : replayCapsule.selectedCandidateId(),
                readinessChecklist == null ? null : readinessChecklist.selectedCandidateId(),
                evidenceSourceMap == null ? null : evidenceSourceMap.selectedCandidateId(),
                boundarySummary == null ? null : boundarySummary.selectedCandidateId(),
                fieldInventory == null ? null : fieldInventory.selectedCandidateId());
    }

    private static int candidateCount(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory) {
        int[] counts = {
                decisionVector == null ? 0 : decisionVector.candidateCount(),
                replaySnapshot == null ? 0 : replaySnapshot.candidateCount(),
                reconstructionTrace == null ? 0 : reconstructionTrace.candidateCount(),
                replayCapsule == null ? 0 : replayCapsule.candidateCount(),
                readinessChecklist == null ? 0 : readinessChecklist.candidateCount(),
                evidenceSourceMap == null ? 0 : evidenceSourceMap.candidateCount(),
                boundarySummary == null ? 0 : boundarySummary.candidateCount(),
                fieldInventory == null ? 0 : fieldInventory.candidateCount()
        };
        for (int count : counts) {
            if (count > 0) {
                return count;
            }
        }
        return 0;
    }

    private static String strategyId(
            String strategyId,
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory) {
        return firstNonBlank(
                strategyId,
                decisionVector == null ? null : decisionVector.selectedStrategy(),
                replaySnapshot == null ? null : replaySnapshot.strategyId(),
                reconstructionTrace == null ? null : reconstructionTrace.strategyId(),
                replayCapsule == null ? null : replayCapsule.strategyId(),
                readinessChecklist == null ? null : readinessChecklist.strategyId(),
                evidenceSourceMap == null ? null : evidenceSourceMap.strategyId(),
                boundarySummary == null ? null : boundarySummary.strategyId(),
                fieldInventory == null ? null : fieldInventory.strategyId());
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static String explanation(
            String status,
            String strategyId,
            String selectedCandidateId,
            int candidateCount,
            int itemCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "No replay execution, what-if mutation, score recomputation, null-safety persistence, or "
                    + "fingerprint generation is performed; null-safety evidence is unavailable from "
                    + "already-built lab compare evidence, and no selected candidate, candidate set, closest "
                    + "alternative, score gap, largest delta factor, fingerprint, certification, guaranteed replay, "
                    + "or production proof explanation is invented.";
        }
        return "Decision Evidence Null-Safety Summary is derived from already-built lab compare evidence only for "
                + "strategy " + safeValue(strategyId) + ", selectedCandidate=" + safeValue(selectedCandidateId)
                + ", candidateCount=" + candidateCount + ", nullSafetyItems=" + itemCount
                + ". It is informational null-safety metadata only and does not certify production behavior.";
    }

    private record FieldAvailability(String fieldPath, boolean available) {
    }
}
