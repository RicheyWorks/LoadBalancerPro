package com.richmond423.loadbalancerpro.api;

import java.util.List;
import java.util.Locale;

public final class RoutingDecisionReplayEvidenceStatusRollupService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-status-rollup/v1";
    private static final String SOURCE =
            "/api/routing/compare already-built lab evidence statuses: decisionVector, "
                    + "dominantFactorAnalysis, decisionDeltaAnalysis, decisionReplaySnapshot, "
                    + "decisionReplayReconstructionTrace, decisionReplayCapsule, "
                    + "decisionReplayReadinessChecklist, decisionReplayEvidenceSourceMap, "
                    + "decisionReplayEvidenceBoundarySummary, decisionReplayEvidenceFieldInventory, "
                    + "and decisionReplayEvidenceNullSafetySummary";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision Evidence Status Rollup is read-only lab status metadata derived only from already-built "
                    + "routing compare response objects; it does not inspect raw server input, does not inspect raw "
                    + "request payloads, does not execute replay, does not perform what-if mutation, does not "
                    + "persist status-rollup data or audit logs, does not generate fingerprints, does not use "
                    + "reflective field inspection, does not recompute scores, does not retune weights, does not "
                    + "change routing behavior, does not add telemetry, does not add external calls, does not add "
                    + "upload/share/download flows, and does not add server-side export/PDF/ZIP generation.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "This status rollup is not production certification, not live-cloud proof, not real-tenant proof, "
                    + "not SLA/SLO proof, not registry publication proof, not signing proof, not governance "
                    + "application proof, not exact production scoring proof, not cryptographic production proof, "
                    + "not guaranteed replay, and not production traffic validation.";

    public RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup(
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
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory,
            RoutingDecisionReplayEvidenceNullSafetySummaryResponse nullSafetySummary) {
        String decisionVectorStatus = decisionVectorStatus(decisionVector);
        String dominantStatus = normalizeStatus(dominantFactorAnalysis == null
                ? null
                : dominantFactorAnalysis.status());
        String deltaStatus = normalizeStatus(decisionDeltaAnalysis == null ? null : decisionDeltaAnalysis.status());
        String snapshotStatus = normalizeStatus(replaySnapshot == null ? null : replaySnapshot.status());
        String traceStatus = normalizeStatus(reconstructionTrace == null ? null : reconstructionTrace.status());
        String capsuleStatus = normalizeStatus(replayCapsule == null ? null : replayCapsule.status());
        String checklistStatus = normalizeStatus(readinessChecklist == null ? null : readinessChecklist.status());
        String sourceMapStatus = normalizeStatus(evidenceSourceMap == null ? null : evidenceSourceMap.status());
        String boundarySummaryStatus = normalizeStatus(boundarySummary == null ? null : boundarySummary.status());
        String fieldInventoryStatus = normalizeStatus(fieldInventory == null ? null : fieldInventory.status());
        String nullSafetyStatus = normalizeStatus(nullSafetySummary == null ? null : nullSafetySummary.status());

        List<DecisionReplayEvidenceStatusRollupItemResponse> items = List.of(
                decisionVectorItem(decisionVector, decisionVectorStatus),
                dominantItem(dominantFactorAnalysis, dominantStatus),
                deltaItem(decisionDeltaAnalysis, deltaStatus),
                snapshotItem(replaySnapshot, snapshotStatus),
                traceItem(reconstructionTrace, traceStatus),
                capsuleItem(replayCapsule, capsuleStatus),
                checklistItem(readinessChecklist, checklistStatus),
                sourceMapItem(evidenceSourceMap, sourceMapStatus),
                boundarySummaryItem(boundarySummary, boundarySummaryStatus),
                fieldInventoryItem(fieldInventory, fieldInventoryStatus),
                nullSafetyItem(nullSafetySummary, nullSafetyStatus),
                readOnlyBoundaryItem(decisionVector, dominantFactorAnalysis, decisionDeltaAnalysis, replaySnapshot,
                        reconstructionTrace, replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary,
                        fieldInventory, nullSafetySummary),
                productionBoundaryItem(decisionVector, dominantFactorAnalysis, decisionDeltaAnalysis, replaySnapshot,
                        reconstructionTrace, replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary,
                        fieldInventory, nullSafetySummary));

        int availableCount = countStatus(items, STATUS_AVAILABLE);
        int partialCount = countStatus(items, STATUS_PARTIAL);
        int unknownCount = countStatus(items, STATUS_UNKNOWN);
        String selectedCandidateId = selectedCandidateId(decisionVector, replaySnapshot, reconstructionTrace,
                replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory,
                nullSafetySummary);
        int candidateCount = candidateCount(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule,
                readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory, nullSafetySummary);
        String resolvedStrategyId = strategyId(strategyId, decisionVector, replaySnapshot, reconstructionTrace,
                replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory,
                nullSafetySummary);
        String status = rollupStatus(items, selectedCandidateId, candidateCount);

        return new RoutingDecisionReplayEvidenceStatusRollupResponse(
                true,
                SCHEMA_VERSION,
                SOURCE,
                status,
                safeValue(resolvedStrategyId),
                isBlank(selectedCandidateId) ? null : selectedCandidateId,
                candidateCount,
                availableCount,
                partialCount,
                unknownCount,
                items,
                explanation(status, resolvedStrategyId, selectedCandidateId, candidateCount, items.size()),
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse decisionVectorItem(
            RoutingDecisionVectorResponse decisionVector,
            String status) {
        return item(
                "decision-vector-status",
                "Decision Vector status",
                status,
                "decisionVector",
                decisionVector != null && decisionVector.readOnly(),
                decisionVector != null && hasText(decisionVector.selectedBackend()),
                decisionVector == null ? 0 : safeCount(decisionVector.candidateCount()),
                decisionVector != null && hasText(decisionVector.productionNotProvenBoundary()),
                "Decision Vector status, selected-candidate presence, candidate count, and boundary state are "
                        + "summarized from results[].decisionVector only.");
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse dominantItem(
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            String status) {
        return item(
                "dominant-factor-analysis-status",
                "Dominant Factor Analysis status",
                status,
                "dominantFactorAnalysis",
                dominantFactorAnalysis != null && dominantFactorAnalysis.readOnly(),
                dominantFactorAnalysis != null && dominantFactorAnalysis.selectedDecisionAnalysis() != null,
                dominantFactorAnalysis == null ? 0 : safeSize(dominantFactorAnalysis.candidateAnalyses()),
                dominantFactorAnalysis != null && hasText(dominantFactorAnalysis.productionNotProvenBoundary()),
                "Dominant Factor Analysis status, selected analysis presence, candidate analysis count, and "
                        + "boundary state are summarized from results[].dominantFactorAnalysis only.");
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse deltaItem(
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            String status) {
        return item(
                "decision-delta-analysis-status",
                "Decision Delta Analysis status",
                status,
                "decisionDeltaAnalysis",
                decisionDeltaAnalysis != null && decisionDeltaAnalysis.readOnly(),
                decisionDeltaAnalysis != null && decisionDeltaAnalysis.comparison() != null,
                decisionDeltaAnalysis == null ? 0 : safeSize(decisionDeltaAnalysis.factorDeltas()),
                decisionDeltaAnalysis != null && hasText(decisionDeltaAnalysis.productionNotProvenBoundary()),
                "Decision Delta Analysis status, comparison presence, factor-delta count, and boundary state are "
                        + "summarized from results[].decisionDeltaAnalysis only.");
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse snapshotItem(
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            String status) {
        return item(
                "replay-snapshot-status",
                "Decision Replay Snapshot status",
                status,
                "decisionReplaySnapshot",
                replaySnapshot != null && replaySnapshot.readOnly(),
                replaySnapshot != null && hasText(replaySnapshot.selectedCandidateId()),
                replaySnapshot == null ? 0 : safeCount(replaySnapshot.candidateCount()),
                replaySnapshot != null && hasText(replaySnapshot.productionNotProvenBoundary()),
                "Decision Replay Snapshot status, selected-candidate presence, candidate count, existing "
                        + "snapshot fingerprint presence, and boundary state are summarized without generating "
                        + "a new fingerprint.");
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse traceItem(
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            String status) {
        return item(
                "reconstruction-trace-status",
                "Decision Replay Reconstruction Trace status",
                status,
                "decisionReplayReconstructionTrace",
                reconstructionTrace != null && reconstructionTrace.readOnly(),
                reconstructionTrace != null && hasText(reconstructionTrace.selectedCandidateId()),
                reconstructionTrace == null ? 0 : safeCount(reconstructionTrace.candidateCount()),
                reconstructionTrace != null && hasText(reconstructionTrace.productionNotProvenBoundary()),
                "Decision Replay Reconstruction Trace status, selected-candidate presence, candidate count, "
                        + "existing trace fingerprint presence, reconstruction-step count, and boundary state "
                        + "are summarized without generating a new fingerprint.");
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse capsuleItem(
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            String status) {
        return item(
                "replay-capsule-status",
                "Decision Replay Capsule status",
                status,
                "decisionReplayCapsule",
                replayCapsule != null && replayCapsule.readOnly(),
                replayCapsule != null && hasText(replayCapsule.selectedCandidateId()),
                replayCapsule == null ? 0 : safeCount(replayCapsule.candidateCount()),
                replayCapsule != null && hasText(replayCapsule.productionNotProvenBoundary()),
                "Decision Replay Capsule status, selected-candidate presence, candidate count, existing capsule "
                        + "fingerprint presence, candidate evidence count, factor evidence count, and boundary "
                        + "state are summarized without generating a new fingerprint.");
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse checklistItem(
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            String status) {
        return item(
                "readiness-checklist-status",
                "Decision Replay Readiness Checklist status",
                status,
                "decisionReplayReadinessChecklist",
                readinessChecklist != null && readinessChecklist.readOnly(),
                readinessChecklist != null && hasText(readinessChecklist.selectedCandidateId()),
                readinessChecklist == null ? 0 : safeCount(readinessChecklist.candidateCount()),
                readinessChecklist != null && hasText(readinessChecklist.productionNotProvenBoundary()),
                "Decision Replay Readiness Checklist status, checklist item count, selected-candidate presence, "
                        + "candidate count, and boundary state are summarized from the already-built checklist.");
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse sourceMapItem(
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            String status) {
        return item(
                "evidence-source-map-status",
                "Decision Replay Evidence Source Map status",
                status,
                "decisionReplayEvidenceSourceMap",
                evidenceSourceMap != null && evidenceSourceMap.readOnly(),
                evidenceSourceMap != null && hasText(evidenceSourceMap.selectedCandidateId()),
                evidenceSourceMap == null ? 0 : safeCount(evidenceSourceMap.candidateCount()),
                evidenceSourceMap != null && hasText(evidenceSourceMap.productionNotProvenBoundary()),
                "Decision Replay Evidence Source Map status, source-map entry count, selected-candidate "
                        + "presence, candidate count, and boundary state are summarized from the already-built "
                        + "source map.");
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse boundarySummaryItem(
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            String status) {
        return item(
                "evidence-boundary-summary-status",
                "Decision Replay Evidence Boundary Summary status",
                status,
                "decisionReplayEvidenceBoundarySummary",
                boundarySummary != null && boundarySummary.readOnly(),
                boundarySummary != null && hasText(boundarySummary.selectedCandidateId()),
                boundarySummary == null ? 0 : safeCount(boundarySummary.candidateCount()),
                boundarySummary != null && hasText(boundarySummary.productionNotProvenBoundary()),
                "Decision Replay Evidence Boundary Summary status, boundary item count, selected-candidate "
                        + "presence, candidate count, and boundary state are summarized from the already-built "
                        + "boundary summary.");
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse fieldInventoryItem(
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory,
            String status) {
        return item(
                "evidence-field-inventory-status",
                "Decision Replay Evidence Field Inventory status",
                status,
                "decisionReplayEvidenceFieldInventory",
                fieldInventory != null && fieldInventory.readOnly(),
                fieldInventory != null && hasText(fieldInventory.selectedCandidateId()),
                fieldInventory == null ? 0 : safeCount(fieldInventory.candidateCount()),
                fieldInventory != null && hasText(fieldInventory.productionNotProvenBoundary()),
                "Decision Replay Evidence Field Inventory status, inventory entry count, inventory group counts, "
                        + "selected-candidate presence, candidate count, and boundary state are summarized from "
                        + "the already-built field inventory.");
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse nullSafetyItem(
            RoutingDecisionReplayEvidenceNullSafetySummaryResponse nullSafetySummary,
            String status) {
        return item(
                "evidence-null-safety-status",
                "Decision Evidence Null-Safety Summary status",
                status,
                "decisionReplayEvidenceNullSafetySummary",
                nullSafetySummary != null && nullSafetySummary.readOnly(),
                nullSafetySummary != null && hasText(nullSafetySummary.selectedCandidateId()),
                nullSafetySummary == null ? 0 : safeCount(nullSafetySummary.candidateCount()),
                nullSafetySummary != null && hasText(nullSafetySummary.productionNotProvenBoundary()),
                "Decision Evidence Null-Safety Summary status, null-safety item count, null-safety item counts, "
                        + "selected-candidate presence, candidate count, and boundary state are summarized from "
                        + "the already-built null-safety summary.");
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse readOnlyBoundaryItem(
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory,
            RoutingDecisionReplayEvidenceNullSafetySummaryResponse nullSafetySummary) {
        boolean readOnlyPresent = isReadOnly(decisionVector)
                || isReadOnly(dominantFactorAnalysis)
                || isReadOnly(decisionDeltaAnalysis)
                || isReadOnly(replaySnapshot)
                || isReadOnly(reconstructionTrace)
                || isReadOnly(replayCapsule)
                || isReadOnly(readinessChecklist)
                || isReadOnly(evidenceSourceMap)
                || isReadOnly(boundarySummary)
                || isReadOnly(fieldInventory)
                || isReadOnly(nullSafetySummary);
        return item(
                "read-only-boundary-status",
                "Read-only boundary status",
                readOnlyPresent ? STATUS_AVAILABLE : STATUS_UNKNOWN,
                "read-only boundary fields across already-built compare evidence",
                true,
                hasText(selectedCandidateId(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule,
                        readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory, nullSafetySummary)),
                candidateCount(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule, readinessChecklist,
                        evidenceSourceMap, boundarySummary, fieldInventory, nullSafetySummary),
                readOnlyPresent,
                "Read-only evidence is summarized across already-built compare DTOs only; this status does not "
                        + "imply enforcement, approval, or production readiness.");
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse productionBoundaryItem(
            RoutingDecisionVectorResponse decisionVector,
            DominantFactorAnalysisResponse dominantFactorAnalysis,
            RoutingDecisionDeltaAnalysisResponse decisionDeltaAnalysis,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory,
            RoutingDecisionReplayEvidenceNullSafetySummaryResponse nullSafetySummary) {
        boolean boundaryPresent = hasProductionBoundary(decisionVector)
                || hasProductionBoundary(dominantFactorAnalysis)
                || hasProductionBoundary(decisionDeltaAnalysis)
                || hasProductionBoundary(replaySnapshot)
                || hasProductionBoundary(reconstructionTrace)
                || hasProductionBoundary(replayCapsule)
                || hasProductionBoundary(readinessChecklist)
                || hasProductionBoundary(evidenceSourceMap)
                || hasProductionBoundary(boundarySummary)
                || hasProductionBoundary(fieldInventory)
                || hasProductionBoundary(nullSafetySummary);
        return item(
                "production-not-proven-status",
                "Production-not-proven status",
                boundaryPresent ? STATUS_AVAILABLE : STATUS_UNKNOWN,
                "productionNotProvenBoundary fields across already-built compare evidence",
                true,
                hasText(selectedCandidateId(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule,
                        readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory, nullSafetySummary)),
                candidateCount(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule, readinessChecklist,
                        evidenceSourceMap, boundarySummary, fieldInventory, nullSafetySummary),
                boundaryPresent,
                "Production-not-proven boundary text is summarized across already-built compare DTOs only; it "
                        + "preserves not production certification, not live-cloud proof, not real-tenant proof, "
                        + "not SLA/SLO proof, not registry publication proof, not signing proof, not governance "
                        + "application proof, not exact production scoring proof, not cryptographic production "
                        + "proof, not guaranteed replay, and not production traffic validation.");
    }

    private static DecisionReplayEvidenceStatusRollupItemResponse item(
            String laneId,
            String label,
            String status,
            String sourceFieldPath,
            boolean readOnly,
            boolean selectedCandidatePresent,
            int candidateCount,
            boolean boundaryPresent,
            String evidenceSummary) {
        return new DecisionReplayEvidenceStatusRollupItemResponse(
                laneId,
                label,
                normalizeStatus(status),
                sourceFieldPath,
                readOnly,
                selectedCandidatePresent,
                safeCount(candidateCount),
                boundaryPresent,
                evidenceSummary,
                BOUNDARY_NOTE);
    }

    private static String decisionVectorStatus(RoutingDecisionVectorResponse decisionVector) {
        if (decisionVector == null) {
            return STATUS_UNKNOWN;
        }
        boolean candidateSummariesPresent = hasItems(decisionVector.candidateSummaries());
        boolean coreFieldsPresent = decisionVector.readOnly()
                && hasText(decisionVector.selectedStrategy())
                && hasText(decisionVector.selectedBackend())
                && decisionVector.candidateCount() > 0
                && candidateSummariesPresent
                && hasText(decisionVector.productionNotProvenBoundary());
        if (coreFieldsPresent) {
            return STATUS_AVAILABLE;
        }
        return candidateSummariesPresent || hasText(decisionVector.selectedBackend())
                ? STATUS_PARTIAL
                : STATUS_UNKNOWN;
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_UNKNOWN;
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case STATUS_AVAILABLE -> STATUS_AVAILABLE;
            case STATUS_PARTIAL -> STATUS_PARTIAL;
            default -> STATUS_UNKNOWN;
        };
    }

    private static String rollupStatus(
            List<DecisionReplayEvidenceStatusRollupItemResponse> items,
            String selectedCandidateId,
            int candidateCount) {
        List<DecisionReplayEvidenceStatusRollupItemResponse> coreItems = items.subList(0, 11);
        boolean allCoreUnknown = coreItems.stream().allMatch(item -> STATUS_UNKNOWN.equals(item.status()));
        if (allCoreUnknown && isBlank(selectedCandidateId) && candidateCount == 0) {
            return STATUS_UNKNOWN;
        }
        boolean allCoreAvailable = coreItems.stream().allMatch(item -> STATUS_AVAILABLE.equals(item.status()));
        return allCoreAvailable ? STATUS_AVAILABLE : STATUS_PARTIAL;
    }

    private static int countStatus(List<DecisionReplayEvidenceStatusRollupItemResponse> items, String status) {
        return (int) items.stream().filter(item -> status.equals(item.status())).count();
    }

    private static String explanation(
            String status,
            String strategyId,
            String selectedCandidateId,
            int candidateCount,
            int itemCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision Evidence Status Rollup is UNKNOWN because already-built lab compare evidence does not "
                    + "include an available selected candidate, candidate set, or evidence-lane status. No replay "
                    + "execution, certification, production proof, guaranteed replay, or status value is invented.";
        }
        return "Decision Evidence Status Rollup is " + status
                + " and derived from already-built lab compare evidence only for strategy "
                + safeValue(strategyId)
                + ", selected candidate "
                + (isBlank(selectedCandidateId) ? "unavailable" : selectedCandidateId)
                + ", candidate count "
                + safeCount(candidateCount)
                + ", and "
                + itemCount
                + " deterministic status items.";
    }

    private static String selectedCandidateId(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory,
            RoutingDecisionReplayEvidenceNullSafetySummaryResponse nullSafetySummary) {
        return firstNonBlank(
                decisionVector == null ? null : decisionVector.selectedBackend(),
                replaySnapshot == null ? null : replaySnapshot.selectedCandidateId(),
                reconstructionTrace == null ? null : reconstructionTrace.selectedCandidateId(),
                replayCapsule == null ? null : replayCapsule.selectedCandidateId(),
                readinessChecklist == null ? null : readinessChecklist.selectedCandidateId(),
                evidenceSourceMap == null ? null : evidenceSourceMap.selectedCandidateId(),
                boundarySummary == null ? null : boundarySummary.selectedCandidateId(),
                fieldInventory == null ? null : fieldInventory.selectedCandidateId(),
                nullSafetySummary == null ? null : nullSafetySummary.selectedCandidateId());
    }

    private static int candidateCount(
            RoutingDecisionVectorResponse decisionVector,
            RoutingDecisionReplaySnapshotResponse replaySnapshot,
            RoutingDecisionReplayReconstructionTraceResponse reconstructionTrace,
            RoutingDecisionReplayCapsuleResponse replayCapsule,
            RoutingDecisionReplayReadinessChecklistResponse readinessChecklist,
            RoutingDecisionReplayEvidenceSourceMapResponse evidenceSourceMap,
            RoutingDecisionReplayEvidenceBoundarySummaryResponse boundarySummary,
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory,
            RoutingDecisionReplayEvidenceNullSafetySummaryResponse nullSafetySummary) {
        return firstPositive(
                decisionVector == null ? 0 : decisionVector.candidateCount(),
                replaySnapshot == null ? 0 : replaySnapshot.candidateCount(),
                reconstructionTrace == null ? 0 : reconstructionTrace.candidateCount(),
                replayCapsule == null ? 0 : replayCapsule.candidateCount(),
                readinessChecklist == null ? 0 : readinessChecklist.candidateCount(),
                evidenceSourceMap == null ? 0 : evidenceSourceMap.candidateCount(),
                boundarySummary == null ? 0 : boundarySummary.candidateCount(),
                fieldInventory == null ? 0 : fieldInventory.candidateCount(),
                nullSafetySummary == null ? 0 : nullSafetySummary.candidateCount());
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
            RoutingDecisionReplayEvidenceFieldInventoryResponse fieldInventory,
            RoutingDecisionReplayEvidenceNullSafetySummaryResponse nullSafetySummary) {
        return firstNonBlank(
                strategyId,
                decisionVector == null ? null : decisionVector.selectedStrategy(),
                replaySnapshot == null ? null : replaySnapshot.strategyId(),
                reconstructionTrace == null ? null : reconstructionTrace.strategyId(),
                replayCapsule == null ? null : replayCapsule.strategyId(),
                readinessChecklist == null ? null : readinessChecklist.strategyId(),
                evidenceSourceMap == null ? null : evidenceSourceMap.strategyId(),
                boundarySummary == null ? null : boundarySummary.strategyId(),
                fieldInventory == null ? null : fieldInventory.strategyId(),
                nullSafetySummary == null ? null : nullSafetySummary.strategyId());
    }

    private static boolean isReadOnly(RoutingDecisionVectorResponse response) {
        return response != null && response.readOnly();
    }

    private static boolean isReadOnly(DominantFactorAnalysisResponse response) {
        return response != null && response.readOnly();
    }

    private static boolean isReadOnly(RoutingDecisionDeltaAnalysisResponse response) {
        return response != null && response.readOnly();
    }

    private static boolean isReadOnly(RoutingDecisionReplaySnapshotResponse response) {
        return response != null && response.readOnly();
    }

    private static boolean isReadOnly(RoutingDecisionReplayReconstructionTraceResponse response) {
        return response != null && response.readOnly();
    }

    private static boolean isReadOnly(RoutingDecisionReplayCapsuleResponse response) {
        return response != null && response.readOnly();
    }

    private static boolean isReadOnly(RoutingDecisionReplayReadinessChecklistResponse response) {
        return response != null && response.readOnly();
    }

    private static boolean isReadOnly(RoutingDecisionReplayEvidenceSourceMapResponse response) {
        return response != null && response.readOnly();
    }

    private static boolean isReadOnly(RoutingDecisionReplayEvidenceBoundarySummaryResponse response) {
        return response != null && response.readOnly();
    }

    private static boolean isReadOnly(RoutingDecisionReplayEvidenceFieldInventoryResponse response) {
        return response != null && response.readOnly();
    }

    private static boolean isReadOnly(RoutingDecisionReplayEvidenceNullSafetySummaryResponse response) {
        return response != null && response.readOnly();
    }

    private static boolean hasProductionBoundary(RoutingDecisionVectorResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasProductionBoundary(DominantFactorAnalysisResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasProductionBoundary(RoutingDecisionDeltaAnalysisResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasProductionBoundary(RoutingDecisionReplaySnapshotResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasProductionBoundary(RoutingDecisionReplayReconstructionTraceResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasProductionBoundary(RoutingDecisionReplayCapsuleResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasProductionBoundary(RoutingDecisionReplayReadinessChecklistResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasProductionBoundary(RoutingDecisionReplayEvidenceSourceMapResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasProductionBoundary(RoutingDecisionReplayEvidenceBoundarySummaryResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasProductionBoundary(RoutingDecisionReplayEvidenceFieldInventoryResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasProductionBoundary(RoutingDecisionReplayEvidenceNullSafetySummaryResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static int safeSize(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private static int safeCount(int candidateCount) {
        return Math.max(0, candidateCount);
    }

    private static int firstPositive(int... values) {
        for (int value : values) {
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean hasItems(List<?> values) {
        return values != null && !values.isEmpty();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safeValue(String value) {
        return isBlank(value) ? "UNKNOWN" : value.trim();
    }
}
