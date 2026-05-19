package com.richmond423.loadbalancerpro.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RoutingDecisionReplayEvidenceFieldInventoryService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-field-inventory/v1";
    private static final String SOURCE =
            "/api/routing/compare already-built lab evidence field groups: decisionVector, "
                    + "dominantFactorAnalysis, decisionDeltaAnalysis, decisionReplaySnapshot, "
                    + "decisionReplayReconstructionTrace, decisionReplayCapsule, "
                    + "decisionReplayReadinessChecklist, decisionReplayEvidenceSourceMap, and "
                    + "decisionReplayEvidenceBoundarySummary";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision Replay Evidence Field Inventory is read-only lab field metadata derived only from "
                    + "already-built routing compare response objects; it does not execute replay, does not "
                    + "perform what-if mutation, does not persist field-inventory data or audit logs, does not "
                    + "generate fingerprints, does not recompute scores, does not retune weights, does not "
                    + "change routing behavior, does not add telemetry, does not add external calls, does not "
                    + "add upload/share/download flows, and does not add server-side export/PDF/ZIP generation.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "This inventory is not production certification, not live-cloud proof, not real-tenant proof, "
                    + "not SLA/SLO proof, not registry publication proof, not signing proof, not governance "
                    + "application proof, not exact production scoring proof, not cryptographic production proof, "
                    + "not guaranteed replay, and not production traffic validation.";

    public RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory(
            String strategyId,
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary) {
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

        List<DecisionReplayEvidenceFieldInventoryEntryResponse> entries = List.of(
                decisionVectorEntry(decisionVector, decisionVectorStatus),
                dominantEntry(dominantFactorAnalysis, dominantStatus),
                deltaEntry(decisionDeltaAnalysis, deltaStatus),
                snapshotEntry(replaySnapshot, snapshotStatus),
                traceEntry(reconstructionTrace, traceStatus),
                capsuleEntry(replayCapsule, capsuleStatus),
                checklistEntry(readinessChecklist, checklistStatus),
                sourceMapEntry(evidenceSourceMap, sourceMapStatus),
                boundarySummaryEntry(boundarySummary, boundarySummaryStatus),
                linkedFingerprintEntry(replaySnapshot, reconstructionTrace, replayCapsule, readinessChecklist,
                        evidenceSourceMap),
                readOnlyBoundaryEntry(decisionVector, dominantFactorAnalysis, decisionDeltaAnalysis, replaySnapshot,
                        reconstructionTrace, replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary),
                productionBoundaryEntry(decisionVector, dominantFactorAnalysis, decisionDeltaAnalysis, replaySnapshot,
                        reconstructionTrace, replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary));

        int availableCount = countStatus(entries, STATUS_AVAILABLE);
        int partialCount = countStatus(entries, STATUS_PARTIAL);
        int unknownCount = countStatus(entries, STATUS_UNKNOWN);
        String selectedCandidateId = selectedCandidateId(decisionVector, replaySnapshot, reconstructionTrace,
                replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary);
        int candidateCount = candidateCount(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule,
                readinessChecklist, evidenceSourceMap, boundarySummary);
        String resolvedStrategyId = strategyId(strategyId, decisionVector, replaySnapshot, reconstructionTrace,
                replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary);
        String status = inventoryStatus(
                entries,
                decisionVectorStatus,
                dominantStatus,
                deltaStatus,
                snapshotStatus,
                traceStatus,
                capsuleStatus,
                checklistStatus,
                sourceMapStatus,
                boundarySummaryStatus,
                selectedCandidateId,
                candidateCount);

        return new RoutingDecisionReplayEvidenceFieldInventoryResponse(
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
                availableCount,
                partialCount,
                unknownCount,
                entries,
                explanation(status, resolvedStrategyId, selectedCandidateId, candidateCount, entries.size()),
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse decisionVectorEntry(
            RoutingDecisionVectorResponse decisionVector,
            String sourceStatus) {
        return sourceEntry(
                "decision-vector-fields",
                "Decision Vector fields",
                "decisionVector",
                sourceStatus,
                List.of(
                        field("decisionVector.readOnly", decisionVector != null && decisionVector.readOnly()),
                        field("decisionVector.localLabResponsePath",
                                decisionVector != null && hasText(decisionVector.localLabResponsePath())),
                        field("decisionVector.selectedStrategy",
                                decisionVector != null && hasText(decisionVector.selectedStrategy())),
                        field("decisionVector.selectedBackend",
                                decisionVector != null && hasText(decisionVector.selectedBackend())),
                        field("decisionVector.candidateCount",
                                decisionVector != null && decisionVector.candidateCount() > 0),
                        field("decisionVector.candidateSummaries",
                                decisionVector != null && hasItems(decisionVector.candidateSummaries())),
                        field("decisionVector.selectedCandidateVector",
                                decisionVector != null && decisionVector.selectedCandidateVector() != null),
                        field("decisionVector.nonSelectedCandidateVectors",
                                decisionVector != null && hasItems(decisionVector.nonSelectedCandidateVectors())),
                        field("decisionVector.knownVisibleSignals",
                                decisionVector != null && hasItems(decisionVector.knownVisibleSignals())),
                        field("decisionVector.unknownOrUnexposedSignals",
                                decisionVector != null && hasItems(decisionVector.unknownOrUnexposedSignals())),
                        field("decisionVector.factorContributionAvailability",
                                decisionVector != null && hasText(decisionVector.factorContributionAvailability())),
                        field("decisionVector.replayReadiness",
                                decisionVector != null && hasText(decisionVector.replayReadiness())),
                        field("decisionVector.whatIfReadiness",
                                decisionVector != null && hasText(decisionVector.whatIfReadiness())),
                        field("decisionVector.structuredDecisionLoggingReadiness",
                                decisionVector != null && hasText(decisionVector.structuredDecisionLoggingReadiness())),
                        field("decisionVector.productionNotProvenBoundary",
                                decisionVector != null && hasText(decisionVector.productionNotProvenBoundary()))),
                "Inventory of observed Decision Vector field groups already returned by the compare response.");
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse dominantEntry(
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            String sourceStatus) {
        return sourceEntry(
                "dominant-factor-analysis-fields",
                "Dominant Factor Analysis fields",
                "dominantFactorAnalysis",
                sourceStatus,
                List.of(
                        field("dominantFactorAnalysis.readOnly",
                                dominantFactorAnalysis != null && dominantFactorAnalysis.readOnly()),
                        field("dominantFactorAnalysis.source",
                                dominantFactorAnalysis != null && hasText(dominantFactorAnalysis.source())),
                        field("dominantFactorAnalysis.status",
                                dominantFactorAnalysis != null && hasText(dominantFactorAnalysis.status())),
                        field("dominantFactorAnalysis.candidateAnalyses",
                                dominantFactorAnalysis != null && hasItems(dominantFactorAnalysis.candidateAnalyses())),
                        field("dominantFactorAnalysis.selectedDecisionAnalysis",
                                dominantFactorAnalysis != null
                                        && dominantFactorAnalysis.selectedDecisionAnalysis() != null),
                        field("dominantFactorAnalysis.selectedDecisionExplanation",
                                dominantFactorAnalysis != null
                                        && hasText(dominantFactorAnalysis.selectedDecisionExplanation())),
                        field("dominantFactorAnalysis.boundaryNote",
                                dominantFactorAnalysis != null && hasText(dominantFactorAnalysis.boundaryNote())),
                        field("dominantFactorAnalysis.productionNotProvenBoundary",
                                dominantFactorAnalysis != null
                                        && hasText(dominantFactorAnalysis.productionNotProvenBoundary()))),
                "Inventory of observed Dominant Factor Analysis field groups already returned by the compare response.");
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse deltaEntry(
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            String sourceStatus) {
        return sourceEntry(
                "decision-delta-analysis-fields",
                "Decision Delta Analysis fields",
                "decisionDeltaAnalysis",
                sourceStatus,
                List.of(
                        field("decisionDeltaAnalysis.readOnly",
                                decisionDeltaAnalysis != null && decisionDeltaAnalysis.readOnly()),
                        field("decisionDeltaAnalysis.source",
                                decisionDeltaAnalysis != null && hasText(decisionDeltaAnalysis.source())),
                        field("decisionDeltaAnalysis.status",
                                decisionDeltaAnalysis != null && hasText(decisionDeltaAnalysis.status())),
                        field("decisionDeltaAnalysis.comparison",
                                decisionDeltaAnalysis != null && decisionDeltaAnalysis.comparison() != null),
                        field("decisionDeltaAnalysis.factorDeltas",
                                decisionDeltaAnalysis != null && hasItems(decisionDeltaAnalysis.factorDeltas())),
                        field("decisionDeltaAnalysis.largestAbsoluteFactorDelta",
                                decisionDeltaAnalysis != null
                                        && decisionDeltaAnalysis.largestAbsoluteFactorDelta() != null),
                        field("decisionDeltaAnalysis.explanation",
                                decisionDeltaAnalysis != null && hasText(decisionDeltaAnalysis.explanation())),
                        field("decisionDeltaAnalysis.boundaryNote",
                                decisionDeltaAnalysis != null && hasText(decisionDeltaAnalysis.boundaryNote())),
                        field("decisionDeltaAnalysis.productionNotProvenBoundary",
                                decisionDeltaAnalysis != null
                                        && hasText(decisionDeltaAnalysis.productionNotProvenBoundary()))),
                "Inventory of observed Decision Delta Analysis field groups already returned by the compare response.");
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse snapshotEntry(
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            String sourceStatus) {
        return sourceEntry(
                "replay-snapshot-fields",
                "Decision Replay Snapshot fields",
                "decisionReplaySnapshot",
                sourceStatus,
                List.of(
                        field("decisionReplaySnapshot.readOnly", replaySnapshot != null && replaySnapshot.readOnly()),
                        field("decisionReplaySnapshot.snapshotSchemaVersion",
                                replaySnapshot != null && hasText(replaySnapshot.snapshotSchemaVersion())),
                        field("decisionReplaySnapshot.source",
                                replaySnapshot != null && hasText(replaySnapshot.source())),
                        field("decisionReplaySnapshot.status",
                                replaySnapshot != null && hasText(replaySnapshot.status())),
                        field("decisionReplaySnapshot.snapshotFingerprint",
                                replaySnapshot != null && safeFingerprint(replaySnapshot.status(),
                                        replaySnapshot.snapshotFingerprint())),
                        field("decisionReplaySnapshot.fingerprintAlgorithm",
                                replaySnapshot != null && safeFingerprint(replaySnapshot.status(),
                                        replaySnapshot.fingerprintAlgorithm())),
                        field("decisionReplaySnapshot.selectedCandidateId",
                                replaySnapshot != null && hasText(replaySnapshot.selectedCandidateId())),
                        field("decisionReplaySnapshot.candidateIdsConsidered",
                                replaySnapshot != null && hasItems(replaySnapshot.candidateIdsConsidered())),
                        field("decisionReplaySnapshot.candidateCount",
                                replaySnapshot != null && replaySnapshot.candidateCount() > 0),
                        field("decisionReplaySnapshot.strategyId",
                                replaySnapshot != null && hasText(replaySnapshot.strategyId())),
                        field("decisionReplaySnapshot.decisionVectorStatus",
                                replaySnapshot != null && hasText(replaySnapshot.decisionVectorStatus())),
                        field("decisionReplaySnapshot.dominantFactorAnalysisStatus",
                                replaySnapshot != null && hasText(replaySnapshot.dominantFactorAnalysisStatus())),
                        field("decisionReplaySnapshot.decisionDeltaAnalysisStatus",
                                replaySnapshot != null && hasText(replaySnapshot.decisionDeltaAnalysisStatus())),
                        field("decisionReplaySnapshot.closestAlternativeCandidateId",
                                replaySnapshot != null && hasText(replaySnapshot.closestAlternativeCandidateId())),
                        field("decisionReplaySnapshot.finalScoreGap",
                                replaySnapshot != null && isFinite(replaySnapshot.finalScoreGap())),
                        field("decisionReplaySnapshot.largestDeltaFactorName",
                                replaySnapshot != null && hasText(replaySnapshot.largestDeltaFactorName())),
                        field("decisionReplaySnapshot.explanation",
                                replaySnapshot != null && hasText(replaySnapshot.explanation())),
                        field("decisionReplaySnapshot.boundaryNote",
                                replaySnapshot != null && hasText(replaySnapshot.boundaryNote())),
                        field("decisionReplaySnapshot.productionNotProvenBoundary",
                                replaySnapshot != null && hasText(replaySnapshot.productionNotProvenBoundary()))),
                "Inventory of observed Decision Replay Snapshot field groups already returned by the compare response.");
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse traceEntry(
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            String sourceStatus) {
        return sourceEntry(
                "reconstruction-trace-fields",
                "Replay Reconstruction Trace fields",
                "decisionReplayReconstructionTrace",
                sourceStatus,
                List.of(
                        field("decisionReplayReconstructionTrace.readOnly",
                                reconstructionTrace != null && reconstructionTrace.readOnly()),
                        field("decisionReplayReconstructionTrace.traceSchemaVersion",
                                reconstructionTrace != null && hasText(reconstructionTrace.traceSchemaVersion())),
                        field("decisionReplayReconstructionTrace.source",
                                reconstructionTrace != null && hasText(reconstructionTrace.source())),
                        field("decisionReplayReconstructionTrace.status",
                                reconstructionTrace != null && hasText(reconstructionTrace.status())),
                        field("decisionReplayReconstructionTrace.traceFingerprint",
                                reconstructionTrace != null && safeFingerprint(reconstructionTrace.status(),
                                        reconstructionTrace.traceFingerprint())),
                        field("decisionReplayReconstructionTrace.fingerprintAlgorithm",
                                reconstructionTrace != null && safeFingerprint(reconstructionTrace.status(),
                                        reconstructionTrace.fingerprintAlgorithm())),
                        field("decisionReplayReconstructionTrace.snapshotFingerprint",
                                reconstructionTrace != null && safeFingerprint(reconstructionTrace.status(),
                                        reconstructionTrace.snapshotFingerprint())),
                        field("decisionReplayReconstructionTrace.selectedCandidateId",
                                reconstructionTrace != null && hasText(reconstructionTrace.selectedCandidateId())),
                        field("decisionReplayReconstructionTrace.candidateIdsConsidered",
                                reconstructionTrace != null && hasItems(reconstructionTrace.candidateIdsConsidered())),
                        field("decisionReplayReconstructionTrace.candidateCount",
                                reconstructionTrace != null && reconstructionTrace.candidateCount() > 0),
                        field("decisionReplayReconstructionTrace.candidateFinalScores",
                                reconstructionTrace != null && hasItems(reconstructionTrace.candidateFinalScores())),
                        field("decisionReplayReconstructionTrace.strategyId",
                                reconstructionTrace != null && hasText(reconstructionTrace.strategyId())),
                        field("decisionReplayReconstructionTrace.decisionVectorStatus",
                                reconstructionTrace != null && hasText(reconstructionTrace.decisionVectorStatus())),
                        field("decisionReplayReconstructionTrace.factorContributionStatus",
                                reconstructionTrace != null
                                        && hasText(reconstructionTrace.factorContributionStatus())),
                        field("decisionReplayReconstructionTrace.dominantFactorAnalysisStatus",
                                reconstructionTrace != null
                                        && hasText(reconstructionTrace.dominantFactorAnalysisStatus())),
                        field("decisionReplayReconstructionTrace.decisionDeltaAnalysisStatus",
                                reconstructionTrace != null
                                        && hasText(reconstructionTrace.decisionDeltaAnalysisStatus())),
                        field("decisionReplayReconstructionTrace.decisionReplaySnapshotStatus",
                                reconstructionTrace != null
                                        && hasText(reconstructionTrace.decisionReplaySnapshotStatus())),
                        field("decisionReplayReconstructionTrace.closestAlternativeCandidateId",
                                reconstructionTrace != null
                                        && hasText(reconstructionTrace.closestAlternativeCandidateId())),
                        field("decisionReplayReconstructionTrace.finalScoreGap",
                                reconstructionTrace != null && isFinite(reconstructionTrace.finalScoreGap())),
                        field("decisionReplayReconstructionTrace.largestDeltaFactorName",
                                reconstructionTrace != null && hasText(reconstructionTrace.largestDeltaFactorName())),
                        field("decisionReplayReconstructionTrace.reconstructionSteps",
                                reconstructionTrace != null && hasItems(reconstructionTrace.reconstructionSteps())),
                        field("decisionReplayReconstructionTrace.explanation",
                                reconstructionTrace != null && hasText(reconstructionTrace.explanation())),
                        field("decisionReplayReconstructionTrace.boundaryNote",
                                reconstructionTrace != null && hasText(reconstructionTrace.boundaryNote())),
                        field("decisionReplayReconstructionTrace.productionNotProvenBoundary",
                                reconstructionTrace != null
                                        && hasText(reconstructionTrace.productionNotProvenBoundary()))),
                "Inventory of observed Reconstruction Trace field groups already returned by the compare response.");
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse capsuleEntry(
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            String sourceStatus) {
        return sourceEntry(
                "replay-capsule-fields",
                "Decision Replay Capsule fields",
                "decisionReplayCapsule",
                sourceStatus,
                List.of(
                        field("decisionReplayCapsule.readOnly", replayCapsule != null && replayCapsule.readOnly()),
                        field("decisionReplayCapsule.capsuleSchemaVersion",
                                replayCapsule != null && hasText(replayCapsule.capsuleSchemaVersion())),
                        field("decisionReplayCapsule.source",
                                replayCapsule != null && hasText(replayCapsule.source())),
                        field("decisionReplayCapsule.status",
                                replayCapsule != null && hasText(replayCapsule.status())),
                        field("decisionReplayCapsule.capsuleFingerprint",
                                replayCapsule != null && safeFingerprint(replayCapsule.status(),
                                        replayCapsule.capsuleFingerprint())),
                        field("decisionReplayCapsule.fingerprintAlgorithm",
                                replayCapsule != null && safeFingerprint(replayCapsule.status(),
                                        replayCapsule.fingerprintAlgorithm())),
                        field("decisionReplayCapsule.selectedCandidateId",
                                replayCapsule != null && hasText(replayCapsule.selectedCandidateId())),
                        field("decisionReplayCapsule.candidateIdsConsidered",
                                replayCapsule != null && hasItems(replayCapsule.candidateIdsConsidered())),
                        field("decisionReplayCapsule.candidateCount",
                                replayCapsule != null && replayCapsule.candidateCount() > 0),
                        field("decisionReplayCapsule.closestAlternativeCandidateId",
                                replayCapsule != null && hasText(replayCapsule.closestAlternativeCandidateId())),
                        field("decisionReplayCapsule.finalScoreGap",
                                replayCapsule != null && isFinite(replayCapsule.finalScoreGap())),
                        field("decisionReplayCapsule.largestDeltaFactorName",
                                replayCapsule != null && hasText(replayCapsule.largestDeltaFactorName())),
                        field("decisionReplayCapsule.linkedReplaySnapshotFingerprint",
                                replayCapsule != null && safeFingerprint(replayCapsule.status(),
                                        replayCapsule.linkedReplaySnapshotFingerprint())),
                        field("decisionReplayCapsule.linkedReconstructionTraceFingerprint",
                                replayCapsule != null && safeFingerprint(replayCapsule.status(),
                                        replayCapsule.linkedReconstructionTraceFingerprint())),
                        field("decisionReplayCapsule.strategyId",
                                replayCapsule != null && hasText(replayCapsule.strategyId())),
                        field("decisionReplayCapsule.decisionVectorStatus",
                                replayCapsule != null && hasText(replayCapsule.decisionVectorStatus())),
                        field("decisionReplayCapsule.factorContributionStatus",
                                replayCapsule != null && hasText(replayCapsule.factorContributionStatus())),
                        field("decisionReplayCapsule.dominantFactorAnalysisStatus",
                                replayCapsule != null && hasText(replayCapsule.dominantFactorAnalysisStatus())),
                        field("decisionReplayCapsule.decisionDeltaAnalysisStatus",
                                replayCapsule != null && hasText(replayCapsule.decisionDeltaAnalysisStatus())),
                        field("decisionReplayCapsule.decisionReplaySnapshotStatus",
                                replayCapsule != null && hasText(replayCapsule.decisionReplaySnapshotStatus())),
                        field("decisionReplayCapsule.decisionReplayReconstructionTraceStatus",
                                replayCapsule != null
                                        && hasText(replayCapsule.decisionReplayReconstructionTraceStatus())),
                        field("decisionReplayCapsule.reconstructionStepIds",
                                replayCapsule != null && hasItems(replayCapsule.reconstructionStepIds())),
                        field("decisionReplayCapsule.candidateEvidence",
                                replayCapsule != null && hasItems(replayCapsule.candidateEvidence())),
                        field("decisionReplayCapsule.factorEvidence",
                                replayCapsule != null && hasItems(replayCapsule.factorEvidence())),
                        field("decisionReplayCapsule.explanation",
                                replayCapsule != null && hasText(replayCapsule.explanation())),
                        field("decisionReplayCapsule.boundaryNote",
                                replayCapsule != null && hasText(replayCapsule.boundaryNote())),
                        field("decisionReplayCapsule.productionNotProvenBoundary",
                                replayCapsule != null && hasText(replayCapsule.productionNotProvenBoundary()))),
                "Inventory of observed Decision Replay Capsule field groups already returned by the compare response.");
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse checklistEntry(
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            String sourceStatus) {
        return sourceEntry(
                "readiness-checklist-fields",
                "Decision Replay Readiness Checklist fields",
                "decisionReplayReadinessChecklist",
                sourceStatus,
                List.of(
                        field("decisionReplayReadinessChecklist.readOnly",
                                readinessChecklist != null && readinessChecklist.readOnly()),
                        field("decisionReplayReadinessChecklist.checklistSchemaVersion",
                                readinessChecklist != null && hasText(readinessChecklist.checklistSchemaVersion())),
                        field("decisionReplayReadinessChecklist.source",
                                readinessChecklist != null && hasText(readinessChecklist.source())),
                        field("decisionReplayReadinessChecklist.status",
                                readinessChecklist != null && hasText(readinessChecklist.status())),
                        field("decisionReplayReadinessChecklist.strategyId",
                                readinessChecklist != null && hasText(readinessChecklist.strategyId())),
                        field("decisionReplayReadinessChecklist.selectedCandidateId",
                                readinessChecklist != null && hasText(readinessChecklist.selectedCandidateId())),
                        field("decisionReplayReadinessChecklist.candidateCount",
                                readinessChecklist != null && readinessChecklist.candidateCount() > 0),
                        field("decisionReplayReadinessChecklist.linkedReplaySnapshotFingerprint",
                                readinessChecklist != null && safeFingerprint(readinessChecklist.status(),
                                        readinessChecklist.linkedReplaySnapshotFingerprint())),
                        field("decisionReplayReadinessChecklist.linkedReconstructionTraceFingerprint",
                                readinessChecklist != null && safeFingerprint(readinessChecklist.status(),
                                        readinessChecklist.linkedReconstructionTraceFingerprint())),
                        field("decisionReplayReadinessChecklist.linkedReplayCapsuleFingerprint",
                                readinessChecklist != null && safeFingerprint(readinessChecklist.status(),
                                        readinessChecklist.linkedReplayCapsuleFingerprint())),
                        field("decisionReplayReadinessChecklist.decisionVectorStatus",
                                readinessChecklist != null && hasText(readinessChecklist.decisionVectorStatus())),
                        field("decisionReplayReadinessChecklist.dominantFactorAnalysisStatus",
                                readinessChecklist != null
                                        && hasText(readinessChecklist.dominantFactorAnalysisStatus())),
                        field("decisionReplayReadinessChecklist.decisionDeltaAnalysisStatus",
                                readinessChecklist != null && hasText(readinessChecklist.decisionDeltaAnalysisStatus())),
                        field("decisionReplayReadinessChecklist.decisionReplaySnapshotStatus",
                                readinessChecklist != null
                                        && hasText(readinessChecklist.decisionReplaySnapshotStatus())),
                        field("decisionReplayReadinessChecklist.decisionReplayReconstructionTraceStatus",
                                readinessChecklist != null
                                        && hasText(readinessChecklist.decisionReplayReconstructionTraceStatus())),
                        field("decisionReplayReadinessChecklist.decisionReplayCapsuleStatus",
                                readinessChecklist != null && hasText(readinessChecklist.decisionReplayCapsuleStatus())),
                        field("decisionReplayReadinessChecklist.checklistItems",
                                readinessChecklist != null && hasItems(readinessChecklist.checklistItems())),
                        field("decisionReplayReadinessChecklist.explanation",
                                readinessChecklist != null && hasText(readinessChecklist.explanation())),
                        field("decisionReplayReadinessChecklist.boundaryNote",
                                readinessChecklist != null && hasText(readinessChecklist.boundaryNote())),
                        field("decisionReplayReadinessChecklist.productionNotProvenBoundary",
                                readinessChecklist != null
                                        && hasText(readinessChecklist.productionNotProvenBoundary()))),
                "Inventory of observed Readiness Checklist field groups already returned by the compare response.");
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse sourceMapEntry(
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            String sourceStatus) {
        return sourceEntry(
                "evidence-source-map-fields",
                "Decision Replay Evidence Source Map fields",
                "decisionReplayEvidenceSourceMap",
                sourceStatus,
                List.of(
                        field("decisionReplayEvidenceSourceMap.readOnly",
                                evidenceSourceMap != null && evidenceSourceMap.readOnly()),
                        field("decisionReplayEvidenceSourceMap.sourceMapSchemaVersion",
                                evidenceSourceMap != null && hasText(evidenceSourceMap.sourceMapSchemaVersion())),
                        field("decisionReplayEvidenceSourceMap.source",
                                evidenceSourceMap != null && hasText(evidenceSourceMap.source())),
                        field("decisionReplayEvidenceSourceMap.status",
                                evidenceSourceMap != null && hasText(evidenceSourceMap.status())),
                        field("decisionReplayEvidenceSourceMap.strategyId",
                                evidenceSourceMap != null && hasText(evidenceSourceMap.strategyId())),
                        field("decisionReplayEvidenceSourceMap.selectedCandidateId",
                                evidenceSourceMap != null && hasText(evidenceSourceMap.selectedCandidateId())),
                        field("decisionReplayEvidenceSourceMap.candidateCount",
                                evidenceSourceMap != null && evidenceSourceMap.candidateCount() > 0),
                        field("decisionReplayEvidenceSourceMap.linkedReplaySnapshotFingerprint",
                                evidenceSourceMap != null && safeFingerprint(evidenceSourceMap.status(),
                                        evidenceSourceMap.linkedReplaySnapshotFingerprint())),
                        field("decisionReplayEvidenceSourceMap.linkedReconstructionTraceFingerprint",
                                evidenceSourceMap != null && safeFingerprint(evidenceSourceMap.status(),
                                        evidenceSourceMap.linkedReconstructionTraceFingerprint())),
                        field("decisionReplayEvidenceSourceMap.linkedReplayCapsuleFingerprint",
                                evidenceSourceMap != null && safeFingerprint(evidenceSourceMap.status(),
                                        evidenceSourceMap.linkedReplayCapsuleFingerprint())),
                        field("decisionReplayEvidenceSourceMap.decisionVectorStatus",
                                evidenceSourceMap != null && hasText(evidenceSourceMap.decisionVectorStatus())),
                        field("decisionReplayEvidenceSourceMap.dominantFactorAnalysisStatus",
                                evidenceSourceMap != null
                                        && hasText(evidenceSourceMap.dominantFactorAnalysisStatus())),
                        field("decisionReplayEvidenceSourceMap.decisionDeltaAnalysisStatus",
                                evidenceSourceMap != null && hasText(evidenceSourceMap.decisionDeltaAnalysisStatus())),
                        field("decisionReplayEvidenceSourceMap.decisionReplaySnapshotStatus",
                                evidenceSourceMap != null
                                        && hasText(evidenceSourceMap.decisionReplaySnapshotStatus())),
                        field("decisionReplayEvidenceSourceMap.decisionReplayReconstructionTraceStatus",
                                evidenceSourceMap != null
                                        && hasText(evidenceSourceMap.decisionReplayReconstructionTraceStatus())),
                        field("decisionReplayEvidenceSourceMap.decisionReplayCapsuleStatus",
                                evidenceSourceMap != null && hasText(evidenceSourceMap.decisionReplayCapsuleStatus())),
                        field("decisionReplayEvidenceSourceMap.decisionReplayReadinessChecklistStatus",
                                evidenceSourceMap != null
                                        && hasText(evidenceSourceMap.decisionReplayReadinessChecklistStatus())),
                        field("decisionReplayEvidenceSourceMap.sourceMapEntries",
                                evidenceSourceMap != null && hasItems(evidenceSourceMap.sourceMapEntries())),
                        field("decisionReplayEvidenceSourceMap.explanation",
                                evidenceSourceMap != null && hasText(evidenceSourceMap.explanation())),
                        field("decisionReplayEvidenceSourceMap.boundaryNote",
                                evidenceSourceMap != null && hasText(evidenceSourceMap.boundaryNote())),
                        field("decisionReplayEvidenceSourceMap.productionNotProvenBoundary",
                                evidenceSourceMap != null && hasText(evidenceSourceMap.productionNotProvenBoundary()))),
                "Inventory of observed Evidence Source Map field groups already returned by the compare response.");
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse boundarySummaryEntry(
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            String sourceStatus) {
        return sourceEntry(
                "evidence-boundary-summary-fields",
                "Decision Replay Evidence Boundary Summary fields",
                "decisionReplayEvidenceBoundarySummary",
                sourceStatus,
                List.of(
                        field("decisionReplayEvidenceBoundarySummary.readOnly",
                                boundarySummary != null && boundarySummary.readOnly()),
                        field("decisionReplayEvidenceBoundarySummary.boundarySummarySchemaVersion",
                                boundarySummary != null && hasText(boundarySummary.boundarySummarySchemaVersion())),
                        field("decisionReplayEvidenceBoundarySummary.source",
                                boundarySummary != null && hasText(boundarySummary.source())),
                        field("decisionReplayEvidenceBoundarySummary.status",
                                boundarySummary != null && hasText(boundarySummary.status())),
                        field("decisionReplayEvidenceBoundarySummary.strategyId",
                                boundarySummary != null && hasText(boundarySummary.strategyId())),
                        field("decisionReplayEvidenceBoundarySummary.selectedCandidateId",
                                boundarySummary != null && hasText(boundarySummary.selectedCandidateId())),
                        field("decisionReplayEvidenceBoundarySummary.candidateCount",
                                boundarySummary != null && boundarySummary.candidateCount() > 0),
                        field("decisionReplayEvidenceBoundarySummary.decisionVectorStatus",
                                boundarySummary != null && hasText(boundarySummary.decisionVectorStatus())),
                        field("decisionReplayEvidenceBoundarySummary.dominantFactorAnalysisStatus",
                                boundarySummary != null && hasText(boundarySummary.dominantFactorAnalysisStatus())),
                        field("decisionReplayEvidenceBoundarySummary.decisionDeltaAnalysisStatus",
                                boundarySummary != null && hasText(boundarySummary.decisionDeltaAnalysisStatus())),
                        field("decisionReplayEvidenceBoundarySummary.decisionReplaySnapshotStatus",
                                boundarySummary != null && hasText(boundarySummary.decisionReplaySnapshotStatus())),
                        field("decisionReplayEvidenceBoundarySummary.decisionReplayReconstructionTraceStatus",
                                boundarySummary != null
                                        && hasText(boundarySummary.decisionReplayReconstructionTraceStatus())),
                        field("decisionReplayEvidenceBoundarySummary.decisionReplayCapsuleStatus",
                                boundarySummary != null && hasText(boundarySummary.decisionReplayCapsuleStatus())),
                        field("decisionReplayEvidenceBoundarySummary.decisionReplayReadinessChecklistStatus",
                                boundarySummary != null
                                        && hasText(boundarySummary.decisionReplayReadinessChecklistStatus())),
                        field("decisionReplayEvidenceBoundarySummary.decisionReplayEvidenceSourceMapStatus",
                                boundarySummary != null
                                        && hasText(boundarySummary.decisionReplayEvidenceSourceMapStatus())),
                        field("decisionReplayEvidenceBoundarySummary.boundaryItems",
                                boundarySummary != null && hasItems(boundarySummary.boundaryItems())),
                        field("decisionReplayEvidenceBoundarySummary.explanation",
                                boundarySummary != null && hasText(boundarySummary.explanation())),
                        field("decisionReplayEvidenceBoundarySummary.boundaryNote",
                                boundarySummary != null && hasText(boundarySummary.boundaryNote())),
                        field("decisionReplayEvidenceBoundarySummary.productionNotProvenBoundary",
                                boundarySummary != null && hasText(boundarySummary.productionNotProvenBoundary()))),
                "Inventory of observed Evidence Boundary Summary field groups already returned by the compare response.");
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse linkedFingerprintEntry(
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap) {
        return freeStandingEntry(
                "linked-fingerprint-fields",
                "Linked fingerprint fields",
                "decisionReplaySnapshot, decisionReplayReconstructionTrace, decisionReplayCapsule, "
                        + "decisionReplayReadinessChecklist, decisionReplayEvidenceSourceMap",
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
                "Inventory of already-present fingerprint fields only; no field-inventory fingerprint is generated.");
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse readOnlyBoundaryEntry(
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary) {
        return freeStandingEntry(
                "read-only-boundary-fields",
                "Read-only boundary fields",
                "readOnly flags and boundaryNote fields across already-built compare evidence",
                List.of(
                        field("decisionVector.readOnly", decisionVector != null && decisionVector.readOnly()),
                        field("dominantFactorAnalysis.readOnly",
                                dominantFactorAnalysis != null && dominantFactorAnalysis.readOnly()),
                        field("decisionDeltaAnalysis.readOnly",
                                decisionDeltaAnalysis != null && decisionDeltaAnalysis.readOnly()),
                        field("decisionReplaySnapshot.readOnly",
                                replaySnapshot != null && replaySnapshot.readOnly()),
                        field("decisionReplayReconstructionTrace.readOnly",
                                reconstructionTrace != null && reconstructionTrace.readOnly()),
                        field("decisionReplayCapsule.readOnly",
                                replayCapsule != null && replayCapsule.readOnly()),
                        field("decisionReplayReadinessChecklist.readOnly",
                                readinessChecklist != null && readinessChecklist.readOnly()),
                        field("decisionReplayEvidenceSourceMap.readOnly",
                                evidenceSourceMap != null && evidenceSourceMap.readOnly()),
                        field("decisionReplayEvidenceBoundarySummary.readOnly",
                                boundarySummary != null && boundarySummary.readOnly()),
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
                                boundarySummary != null && hasText(boundarySummary.boundaryNote()))),
                "Inventory of read-only and lab-only boundary fields already present in compare evidence.");
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse productionBoundaryEntry(
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary) {
        return freeStandingEntry(
                "production-not-proven-boundary-fields",
                "Production not-proven boundary fields",
                "productionNotProvenBoundary fields across already-built compare evidence",
                List.of(
                        field("decisionVector.productionNotProvenBoundary",
                                decisionVector != null && hasText(decisionVector.productionNotProvenBoundary())),
                        field("dominantFactorAnalysis.productionNotProvenBoundary",
                                dominantFactorAnalysis != null
                                        && hasText(dominantFactorAnalysis.productionNotProvenBoundary())),
                        field("decisionDeltaAnalysis.productionNotProvenBoundary",
                                decisionDeltaAnalysis != null
                                        && hasText(decisionDeltaAnalysis.productionNotProvenBoundary())),
                        field("decisionReplaySnapshot.productionNotProvenBoundary",
                                replaySnapshot != null && hasText(replaySnapshot.productionNotProvenBoundary())),
                        field("decisionReplayReconstructionTrace.productionNotProvenBoundary",
                                reconstructionTrace != null
                                        && hasText(reconstructionTrace.productionNotProvenBoundary())),
                        field("decisionReplayCapsule.productionNotProvenBoundary",
                                replayCapsule != null && hasText(replayCapsule.productionNotProvenBoundary())),
                        field("decisionReplayReadinessChecklist.productionNotProvenBoundary",
                                readinessChecklist != null
                                        && hasText(readinessChecklist.productionNotProvenBoundary())),
                        field("decisionReplayEvidenceSourceMap.productionNotProvenBoundary",
                                evidenceSourceMap != null
                                        && hasText(evidenceSourceMap.productionNotProvenBoundary())),
                        field("decisionReplayEvidenceBoundarySummary.productionNotProvenBoundary",
                                boundarySummary != null && hasText(boundarySummary.productionNotProvenBoundary()))),
                "Inventory of existing not-proven boundary fields; no production certification or guaranteed "
                        + "replay is claimed.");
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse sourceEntry(
            String inventoryId,
            String label,
            String sourceFieldPath,
            String sourceStatus,
            List<FieldAvailability> fields,
            String evidenceSummary) {
        List<String> observed = observed(fields);
        List<String> missing = missing(fields);
        String status = sourceEntryStatus(sourceStatus, observed, missing);
        return new DecisionReplayEvidenceFieldInventoryEntryResponse(
                inventoryId,
                label,
                status,
                sourceFieldPath,
                observed,
                missing,
                observed.size(),
                missing.size(),
                evidenceSummary,
                BOUNDARY_NOTE);
    }

    private static DecisionReplayEvidenceFieldInventoryEntryResponse freeStandingEntry(
            String inventoryId,
            String label,
            String sourceFieldPath,
            List<FieldAvailability> fields,
            String evidenceSummary) {
        List<String> observed = observed(fields);
        List<String> missing = missing(fields);
        String status = freeStandingEntryStatus(observed, missing);
        return new DecisionReplayEvidenceFieldInventoryEntryResponse(
                inventoryId,
                label,
                status,
                sourceFieldPath,
                observed,
                missing,
                observed.size(),
                missing.size(),
                evidenceSummary,
                BOUNDARY_NOTE);
    }

    private static FieldAvailability field(String fieldPath, boolean available) {
        return new FieldAvailability(fieldPath, available);
    }

    private static List<String> observed(List<FieldAvailability> fields) {
        List<String> observed = new ArrayList<>();
        for (FieldAvailability field : fields) {
            if (field.available()) {
                observed.add(field.fieldPath());
            }
        }
        return List.copyOf(observed);
    }

    private static List<String> missing(List<FieldAvailability> fields) {
        List<String> missing = new ArrayList<>();
        for (FieldAvailability field : fields) {
            if (!field.available()) {
                missing.add(field.fieldPath());
            }
        }
        return List.copyOf(missing);
    }

    private static String sourceEntryStatus(String sourceStatus, List<String> observed, List<String> missing) {
        if (STATUS_UNKNOWN.equals(analysisStatus(sourceStatus))) {
            return STATUS_UNKNOWN;
        }
        if (observed.isEmpty()) {
            return STATUS_UNKNOWN;
        }
        if (missing.isEmpty()) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
    }

    private static String freeStandingEntryStatus(List<String> observed, List<String> missing) {
        if (observed.isEmpty()) {
            return STATUS_UNKNOWN;
        }
        if (missing.isEmpty()) {
            return STATUS_AVAILABLE;
        }
        return STATUS_PARTIAL;
    }

    private static String inventoryStatus(
            List<DecisionReplayEvidenceFieldInventoryEntryResponse> entries,
            String decisionVectorStatus,
            String dominantStatus,
            String deltaStatus,
            String snapshotStatus,
            String traceStatus,
            String capsuleStatus,
            String checklistStatus,
            String sourceMapStatus,
            String boundarySummaryStatus,
            String selectedCandidateId,
            int candidateCount) {
        if (allUnknown(decisionVectorStatus, dominantStatus, deltaStatus, snapshotStatus, traceStatus, capsuleStatus,
                checklistStatus, sourceMapStatus, boundarySummaryStatus)
                && isBlank(selectedCandidateId)
                && candidateCount == 0
                && entries.stream().limit(10).noneMatch(entry -> STATUS_AVAILABLE.equals(entry.status()))) {
            return STATUS_UNKNOWN;
        }
        if (entries.stream().allMatch(entry -> STATUS_AVAILABLE.equals(entry.status()))) {
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

    private static int countStatus(List<DecisionReplayEvidenceFieldInventoryEntryResponse> entries, String status) {
        int count = 0;
        for (DecisionReplayEvidenceFieldInventoryEntryResponse entry : entries) {
            if (status.equals(entry.status())) {
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean hasItems(List<?> values) {
        return values != null && !values.isEmpty();
    }

    private static boolean hasItems(java.util.Map<?, ?> values) {
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
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary) {
        return firstNonBlank(
                decisionVector == null ? null : decisionVector.selectedBackend(),
                replaySnapshot == null ? null : replaySnapshot.selectedCandidateId(),
                reconstructionTrace == null ? null : reconstructionTrace.selectedCandidateId(),
                replayCapsule == null ? null : replayCapsule.selectedCandidateId(),
                readinessChecklist == null ? null : readinessChecklist.selectedCandidateId(),
                evidenceSourceMap == null ? null : evidenceSourceMap.selectedCandidateId(),
                boundarySummary == null ? null : boundarySummary.selectedCandidateId());
    }

    private static int candidateCount(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary) {
        int[] counts = {
                decisionVector == null ? 0 : decisionVector.candidateCount(),
                replaySnapshot == null ? 0 : replaySnapshot.candidateCount(),
                reconstructionTrace == null ? 0 : reconstructionTrace.candidateCount(),
                replayCapsule == null ? 0 : replayCapsule.candidateCount(),
                readinessChecklist == null ? 0 : readinessChecklist.candidateCount(),
                evidenceSourceMap == null ? 0 : evidenceSourceMap.candidateCount(),
                boundarySummary == null ? 0 : boundarySummary.candidateCount()
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
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary) {
        return firstNonBlank(
                strategyId,
                decisionVector == null ? null : decisionVector.selectedStrategy(),
                replaySnapshot == null ? null : replaySnapshot.strategyId(),
                reconstructionTrace == null ? null : reconstructionTrace.strategyId(),
                replayCapsule == null ? null : replayCapsule.strategyId(),
                readinessChecklist == null ? null : readinessChecklist.strategyId(),
                evidenceSourceMap == null ? null : evidenceSourceMap.strategyId(),
                boundarySummary == null ? null : boundarySummary.strategyId());
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
            int entryCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "No replay execution, what-if mutation, score recomputation, or field-inventory persistence "
                    + "is performed; field inventory evidence is unavailable from already-built lab compare "
                    + "evidence, and no selected candidate, candidate set, fingerprint, certification, guaranteed "
                    + "replay, or production proof explanation is invented.";
        }
        return "Decision Replay Evidence Field Inventory is derived from already-built lab compare evidence only "
                + "for strategy " + safeValue(strategyId) + ", selectedCandidate="
                + safeValue(selectedCandidateId) + ", candidateCount=" + candidateCount + ", inventoryEntries="
                + entryCount + ". It is informational field metadata only and does not certify production behavior.";
    }

    private record FieldAvailability(String fieldPath, boolean available) {
    }
}
