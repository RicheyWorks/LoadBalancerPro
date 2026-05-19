package com.richmond423.loadbalancerpro.api;

import java.util.List;
import java.util.Locale;

public final class RoutingDecisionReplayEvidenceLaneDependencyMapService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-lane-dependency-map/v1";
    private static final String SOURCE =
            "/api/routing/compare already-built lab evidence dependency metadata: decisionVector, "
                    + "dominantFactorAnalysis, decisionDeltaAnalysis, decisionReplaySnapshot, "
                    + "decisionReplayReconstructionTrace, decisionReplayCapsule, "
                    + "decisionReplayReadinessChecklist, decisionReplayEvidenceSourceMap, "
                    + "decisionReplayEvidenceBoundarySummary, decisionReplayEvidenceFieldInventory, "
                    + "decisionReplayEvidenceNullSafetySummary, decisionReplayEvidenceStatusRollup, and "
                    + "decisionReplayEvidenceLaneNavigationSummary";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision Replay Evidence Lane Dependency Map is read-only lab dependency metadata derived only "
                    + "from already-built routing compare response objects; it does not inspect raw server input, "
                    + "does not inspect raw request payloads, does not execute replay, does not perform what-if "
                    + "mutation, does not persist lane-dependency data or audit logs, does not generate "
                    + "fingerprints, does not use reflective field inspection, does not recompute scores, does "
                    + "not retune weights, does not change routing behavior, does not add telemetry, does not "
                    + "add external calls, does not add upload/share/download flows, and does not add "
                    + "server-side export/PDF/ZIP generation.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "This lane dependency map is not production certification, not live-cloud proof, not real-tenant "
                    + "proof, not SLA/SLO proof, not registry publication proof, not signing proof, not "
                    + "governance application proof, not exact production scoring proof, not cryptographic "
                    + "production proof, not guaranteed replay, and not production traffic validation.";

    private static final String DECISION_VECTOR = "decision-vector";
    private static final String DOMINANT_FACTOR_ANALYSIS = "dominant-factor-analysis";
    private static final String DECISION_DELTA_ANALYSIS = "decision-delta-analysis";
    private static final String REPLAY_SNAPSHOT = "replay-snapshot";
    private static final String RECONSTRUCTION_TRACE = "reconstruction-trace";
    private static final String REPLAY_CAPSULE = "replay-capsule";
    private static final String READINESS_CHECKLIST = "readiness-checklist";
    private static final String EVIDENCE_SOURCE_MAP = "evidence-source-map";
    private static final String EVIDENCE_BOUNDARY_SUMMARY = "evidence-boundary-summary";
    private static final String EVIDENCE_FIELD_INVENTORY = "evidence-field-inventory";
    private static final String EVIDENCE_NULL_SAFETY = "evidence-null-safety";
    private static final String EVIDENCE_STATUS_ROLLUP = "evidence-status-rollup";
    private static final String EVIDENCE_LANE_NAVIGATION = "evidence-lane-navigation";

    public RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap(
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
            RoutingDecisionReplayEvidenceNullSafetySummaryResponse nullSafetySummary,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigationSummary) {
        List<DecisionReplayEvidenceLaneDependencyItemResponse> items = List.of(
                item(
                        "decision-vector-dependency",
                        "Decision Vector dependency",
                        decisionVectorStatus(decisionVector),
                        "results[].decisionVector",
                        List.of(),
                        List.of(DOMINANT_FACTOR_ANALYSIS, DECISION_DELTA_ANALYSIS, REPLAY_SNAPSHOT,
                                RECONSTRUCTION_TRACE, REPLAY_CAPSULE, READINESS_CHECKLIST, EVIDENCE_SOURCE_MAP,
                                EVIDENCE_BOUNDARY_SUMMARY, EVIDENCE_FIELD_INVENTORY, EVIDENCE_NULL_SAFETY,
                                EVIDENCE_STATUS_ROLLUP, EVIDENCE_LANE_NAVIGATION),
                        decisionVector != null && decisionVector.readOnly(),
                        hasBoundary(decisionVector),
                        "Decision Vector is the first already-built compare evidence lane in this dependency map; "
                                + "it has no upstream evidence-lane dependency here."),
                item(
                        "dominant-factor-analysis-dependency",
                        "Dominant Factor Analysis dependency",
                        normalizeStatus(dominantFactorAnalysis == null ? null : dominantFactorAnalysis.status()),
                        "results[].dominantFactorAnalysis",
                        List.of(DECISION_VECTOR),
                        List.of(REPLAY_SNAPSHOT, RECONSTRUCTION_TRACE, REPLAY_CAPSULE, READINESS_CHECKLIST,
                                EVIDENCE_SOURCE_MAP, EVIDENCE_BOUNDARY_SUMMARY, EVIDENCE_FIELD_INVENTORY,
                                EVIDENCE_NULL_SAFETY, EVIDENCE_STATUS_ROLLUP, EVIDENCE_LANE_NAVIGATION),
                        dominantFactorAnalysis != null && dominantFactorAnalysis.readOnly(),
                        hasBoundary(dominantFactorAnalysis),
                        "Dominant Factor Analysis dependency metadata points to the already-built Decision Vector "
                                + "lane without recomputing or inferring scoring data."),
                item(
                        "decision-delta-analysis-dependency",
                        "Decision Delta Analysis dependency",
                        normalizeStatus(decisionDeltaAnalysis == null ? null : decisionDeltaAnalysis.status()),
                        "results[].decisionDeltaAnalysis",
                        List.of(DECISION_VECTOR),
                        List.of(REPLAY_SNAPSHOT, RECONSTRUCTION_TRACE, REPLAY_CAPSULE, READINESS_CHECKLIST,
                                EVIDENCE_SOURCE_MAP, EVIDENCE_BOUNDARY_SUMMARY, EVIDENCE_FIELD_INVENTORY,
                                EVIDENCE_NULL_SAFETY, EVIDENCE_STATUS_ROLLUP, EVIDENCE_LANE_NAVIGATION),
                        decisionDeltaAnalysis != null && decisionDeltaAnalysis.readOnly(),
                        hasBoundary(decisionDeltaAnalysis),
                        "Decision Delta Analysis dependency metadata points to the already-built Decision Vector "
                                + "lane and does not compute unavailable score gaps."),
                item(
                        "replay-snapshot-dependency",
                        "Decision Replay Snapshot dependency",
                        normalizeStatus(replaySnapshot == null ? null : replaySnapshot.status()),
                        "results[].decisionReplaySnapshot",
                        List.of(DECISION_VECTOR, DOMINANT_FACTOR_ANALYSIS, DECISION_DELTA_ANALYSIS),
                        List.of(RECONSTRUCTION_TRACE, REPLAY_CAPSULE, READINESS_CHECKLIST, EVIDENCE_SOURCE_MAP,
                                EVIDENCE_BOUNDARY_SUMMARY, EVIDENCE_FIELD_INVENTORY, EVIDENCE_NULL_SAFETY,
                                EVIDENCE_STATUS_ROLLUP, EVIDENCE_LANE_NAVIGATION),
                        replaySnapshot != null && replaySnapshot.readOnly(),
                        hasBoundary(replaySnapshot),
                        "Decision Replay Snapshot dependency metadata points to already-built vector, dominant "
                                + "factor, and delta evidence and does not generate a new fingerprint."),
                item(
                        "reconstruction-trace-dependency",
                        "Decision Replay Reconstruction Trace dependency",
                        normalizeStatus(reconstructionTrace == null ? null : reconstructionTrace.status()),
                        "results[].decisionReplayReconstructionTrace",
                        List.of(DECISION_VECTOR, DOMINANT_FACTOR_ANALYSIS, DECISION_DELTA_ANALYSIS,
                                REPLAY_SNAPSHOT),
                        List.of(REPLAY_CAPSULE, READINESS_CHECKLIST, EVIDENCE_SOURCE_MAP,
                                EVIDENCE_BOUNDARY_SUMMARY, EVIDENCE_FIELD_INVENTORY, EVIDENCE_NULL_SAFETY,
                                EVIDENCE_STATUS_ROLLUP, EVIDENCE_LANE_NAVIGATION),
                        reconstructionTrace != null && reconstructionTrace.readOnly(),
                        hasBoundary(reconstructionTrace),
                        "Decision Replay Reconstruction Trace dependency metadata points to already-built "
                                + "snapshot and supporting evidence without executing replay."),
                item(
                        "replay-capsule-dependency",
                        "Decision Replay Capsule dependency",
                        normalizeStatus(replayCapsule == null ? null : replayCapsule.status()),
                        "results[].decisionReplayCapsule",
                        List.of(DECISION_VECTOR, DOMINANT_FACTOR_ANALYSIS, DECISION_DELTA_ANALYSIS,
                                REPLAY_SNAPSHOT, RECONSTRUCTION_TRACE),
                        List.of(READINESS_CHECKLIST, EVIDENCE_SOURCE_MAP, EVIDENCE_BOUNDARY_SUMMARY,
                                EVIDENCE_FIELD_INVENTORY, EVIDENCE_NULL_SAFETY, EVIDENCE_STATUS_ROLLUP,
                                EVIDENCE_LANE_NAVIGATION),
                        replayCapsule != null && replayCapsule.readOnly(),
                        hasBoundary(replayCapsule),
                        "Decision Replay Capsule dependency metadata points to already-built snapshot and trace "
                                + "evidence without persisting or exporting capsule data."),
                item(
                        "readiness-checklist-dependency",
                        "Decision Replay Readiness Checklist dependency",
                        normalizeStatus(readinessChecklist == null ? null : readinessChecklist.status()),
                        "results[].decisionReplayReadinessChecklist",
                        List.of(DECISION_VECTOR, DOMINANT_FACTOR_ANALYSIS, DECISION_DELTA_ANALYSIS,
                                REPLAY_SNAPSHOT, RECONSTRUCTION_TRACE, REPLAY_CAPSULE),
                        List.of(EVIDENCE_SOURCE_MAP, EVIDENCE_BOUNDARY_SUMMARY, EVIDENCE_FIELD_INVENTORY,
                                EVIDENCE_NULL_SAFETY, EVIDENCE_STATUS_ROLLUP, EVIDENCE_LANE_NAVIGATION),
                        readinessChecklist != null && readinessChecklist.readOnly(),
                        hasBoundary(readinessChecklist),
                        "Decision Replay Readiness Checklist dependency metadata points to already-built replay "
                                + "evidence lanes and remains reviewer metadata only."),
                item(
                        "evidence-source-map-dependency",
                        "Decision Replay Evidence Source Map dependency",
                        normalizeStatus(evidenceSourceMap == null ? null : evidenceSourceMap.status()),
                        "results[].decisionReplayEvidenceSourceMap",
                        List.of(DECISION_VECTOR, DOMINANT_FACTOR_ANALYSIS, DECISION_DELTA_ANALYSIS,
                                REPLAY_SNAPSHOT, RECONSTRUCTION_TRACE, REPLAY_CAPSULE, READINESS_CHECKLIST),
                        List.of(EVIDENCE_BOUNDARY_SUMMARY, EVIDENCE_FIELD_INVENTORY, EVIDENCE_NULL_SAFETY,
                                EVIDENCE_STATUS_ROLLUP, EVIDENCE_LANE_NAVIGATION),
                        evidenceSourceMap != null && evidenceSourceMap.readOnly(),
                        hasBoundary(evidenceSourceMap),
                        "Decision Replay Evidence Source Map dependency metadata points to already-built source "
                                + "relationship evidence and does not generate a new source-map fingerprint."),
                item(
                        "evidence-boundary-summary-dependency",
                        "Decision Replay Evidence Boundary Summary dependency",
                        normalizeStatus(boundarySummary == null ? null : boundarySummary.status()),
                        "results[].decisionReplayEvidenceBoundarySummary",
                        List.of(DECISION_VECTOR, DOMINANT_FACTOR_ANALYSIS, DECISION_DELTA_ANALYSIS,
                                REPLAY_SNAPSHOT, RECONSTRUCTION_TRACE, REPLAY_CAPSULE, READINESS_CHECKLIST,
                                EVIDENCE_SOURCE_MAP),
                        List.of(EVIDENCE_FIELD_INVENTORY, EVIDENCE_NULL_SAFETY, EVIDENCE_STATUS_ROLLUP,
                                EVIDENCE_LANE_NAVIGATION),
                        boundarySummary != null && boundarySummary.readOnly(),
                        hasBoundary(boundarySummary),
                        "Decision Replay Evidence Boundary Summary dependency metadata points to already-built "
                                + "boundary and source-map evidence without adding approval language."),
                item(
                        "evidence-field-inventory-dependency",
                        "Decision Replay Evidence Field Inventory dependency",
                        normalizeStatus(fieldInventory == null ? null : fieldInventory.status()),
                        "results[].decisionReplayEvidenceFieldInventory",
                        List.of(DECISION_VECTOR, DOMINANT_FACTOR_ANALYSIS, DECISION_DELTA_ANALYSIS,
                                REPLAY_SNAPSHOT, RECONSTRUCTION_TRACE, REPLAY_CAPSULE, READINESS_CHECKLIST,
                                EVIDENCE_SOURCE_MAP, EVIDENCE_BOUNDARY_SUMMARY),
                        List.of(EVIDENCE_NULL_SAFETY, EVIDENCE_STATUS_ROLLUP, EVIDENCE_LANE_NAVIGATION),
                        fieldInventory != null && fieldInventory.readOnly(),
                        hasBoundary(fieldInventory),
                        "Decision Replay Evidence Field Inventory dependency metadata points to already-built "
                                + "field inventory inputs and does not use reflective field inspection."),
                item(
                        "evidence-null-safety-dependency",
                        "Decision Evidence Null-Safety Summary dependency",
                        normalizeStatus(nullSafetySummary == null ? null : nullSafetySummary.status()),
                        "results[].decisionReplayEvidenceNullSafetySummary",
                        List.of(DECISION_VECTOR, DOMINANT_FACTOR_ANALYSIS, DECISION_DELTA_ANALYSIS,
                                REPLAY_SNAPSHOT, RECONSTRUCTION_TRACE, REPLAY_CAPSULE, READINESS_CHECKLIST,
                                EVIDENCE_SOURCE_MAP, EVIDENCE_BOUNDARY_SUMMARY, EVIDENCE_FIELD_INVENTORY),
                        List.of(EVIDENCE_STATUS_ROLLUP, EVIDENCE_LANE_NAVIGATION),
                        nullSafetySummary != null && nullSafetySummary.readOnly(),
                        hasBoundary(nullSafetySummary),
                        "Decision Evidence Null-Safety Summary dependency metadata points to already-built "
                                + "null-safety inputs without inventing unavailable evidence."),
                item(
                        "evidence-status-rollup-dependency",
                        "Decision Evidence Status Rollup dependency",
                        normalizeStatus(statusRollup == null ? null : statusRollup.status()),
                        "results[].decisionReplayEvidenceStatusRollup",
                        List.of(DECISION_VECTOR, DOMINANT_FACTOR_ANALYSIS, DECISION_DELTA_ANALYSIS,
                                REPLAY_SNAPSHOT, RECONSTRUCTION_TRACE, REPLAY_CAPSULE, READINESS_CHECKLIST,
                                EVIDENCE_SOURCE_MAP, EVIDENCE_BOUNDARY_SUMMARY, EVIDENCE_FIELD_INVENTORY,
                                EVIDENCE_NULL_SAFETY),
                        List.of(EVIDENCE_LANE_NAVIGATION),
                        statusRollup != null && statusRollup.readOnly(),
                        hasBoundary(statusRollup),
                        "Decision Evidence Status Rollup dependency metadata points to already-built status "
                                + "inputs without adding scoring or ranking."),
                item(
                        "evidence-lane-navigation-dependency",
                        "Decision Replay Evidence Lane Navigation dependency",
                        normalizeStatus(laneNavigationSummary == null ? null : laneNavigationSummary.status()),
                        "results[].decisionReplayEvidenceLaneNavigationSummary",
                        List.of(DECISION_VECTOR, DOMINANT_FACTOR_ANALYSIS, DECISION_DELTA_ANALYSIS,
                                REPLAY_SNAPSHOT, RECONSTRUCTION_TRACE, REPLAY_CAPSULE, READINESS_CHECKLIST,
                                EVIDENCE_SOURCE_MAP, EVIDENCE_BOUNDARY_SUMMARY, EVIDENCE_FIELD_INVENTORY,
                                EVIDENCE_NULL_SAFETY, EVIDENCE_STATUS_ROLLUP),
                        List.of(),
                        laneNavigationSummary != null && laneNavigationSummary.readOnly(),
                        hasBoundary(laneNavigationSummary),
                        "Decision Replay Evidence Lane Navigation dependency metadata points to already-built "
                                + "navigation inputs and has no downstream evidence lane in this map."));

        int availableCount = countStatus(items, STATUS_AVAILABLE);
        int partialCount = countStatus(items, STATUS_PARTIAL);
        int unknownCount = countStatus(items, STATUS_UNKNOWN);
        String selectedCandidateId = selectedCandidateId(decisionVector, replaySnapshot, reconstructionTrace,
                replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory,
                nullSafetySummary, statusRollup, laneNavigationSummary);
        int candidateCount = candidateCount(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule,
                readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory, nullSafetySummary,
                statusRollup, laneNavigationSummary);
        String resolvedStrategyId = strategyId(strategyId, decisionVector, replaySnapshot, reconstructionTrace,
                replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory,
                nullSafetySummary, statusRollup, laneNavigationSummary);
        String status = mapStatus(items, selectedCandidateId, candidateCount);

        return new RoutingDecisionReplayEvidenceLaneDependencyMapResponse(
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

    private static DecisionReplayEvidenceLaneDependencyItemResponse item(
            String laneId,
            String label,
            String status,
            String responseFieldPath,
            List<String> dependsOnLaneIds,
            List<String> downstreamLaneIds,
            boolean readOnly,
            boolean boundaryPresent,
            String dependencySummary) {
        return new DecisionReplayEvidenceLaneDependencyItemResponse(
                laneId,
                label,
                normalizeStatus(status),
                responseFieldPath,
                dependsOnLaneIds,
                downstreamLaneIds,
                safeSize(dependsOnLaneIds),
                safeSize(downstreamLaneIds),
                readOnly,
                boundaryPresent,
                dependencySummary,
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
                && hasBoundary(decisionVector);
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

    private static String mapStatus(
            List<DecisionReplayEvidenceLaneDependencyItemResponse> items,
            String selectedCandidateId,
            int candidateCount) {
        boolean allUnknown = items.stream().allMatch(item -> STATUS_UNKNOWN.equals(item.status()));
        if (allUnknown && isBlank(selectedCandidateId) && candidateCount == 0) {
            return STATUS_UNKNOWN;
        }
        boolean allAvailable = items.stream().allMatch(item -> STATUS_AVAILABLE.equals(item.status()));
        return allAvailable ? STATUS_AVAILABLE : STATUS_PARTIAL;
    }

    private static int countStatus(List<DecisionReplayEvidenceLaneDependencyItemResponse> items, String status) {
        return (int) items.stream().filter(item -> status.equals(item.status())).count();
    }

    private static String explanation(
            String status,
            String strategyId,
            String selectedCandidateId,
            int candidateCount,
            int itemCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision Replay Evidence Lane Dependency Map is UNKNOWN because already-built lab compare "
                    + "evidence does not include an available selected candidate, candidate set, or evidence-lane "
                    + "status. No replay execution, certification, production proof, guaranteed replay, "
                    + "or dependency value is invented.";
        }
        return "Decision Replay Evidence Lane Dependency Map is " + status
                + " and derived from already-built lab compare evidence only for strategy "
                + safeValue(strategyId)
                + ", selected candidate "
                + (isBlank(selectedCandidateId) ? "unavailable" : selectedCandidateId)
                + ", candidate count "
                + safeCount(candidateCount)
                + ", and "
                + itemCount
                + " deterministic dependency items.";
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
            RoutingDecisionReplayEvidenceNullSafetySummaryResponse nullSafetySummary,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigationSummary) {
        return firstNonBlank(
                laneNavigationSummary == null ? null : laneNavigationSummary.selectedCandidateId(),
                statusRollup == null ? null : statusRollup.selectedCandidateId(),
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
            RoutingDecisionReplayEvidenceNullSafetySummaryResponse nullSafetySummary,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigationSummary) {
        return firstPositive(
                laneNavigationSummary == null ? 0 : laneNavigationSummary.candidateCount(),
                statusRollup == null ? 0 : statusRollup.candidateCount(),
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
            RoutingDecisionReplayEvidenceNullSafetySummaryResponse nullSafetySummary,
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup,
            RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigationSummary) {
        return firstNonBlank(
                strategyId,
                laneNavigationSummary == null ? null : laneNavigationSummary.strategyId(),
                statusRollup == null ? null : statusRollup.strategyId(),
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

    private static boolean hasBoundary(RoutingDecisionVectorResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasBoundary(DominantFactorAnalysisResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasBoundary(RoutingDecisionDeltaAnalysisResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasBoundary(RoutingDecisionReplaySnapshotResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasBoundary(RoutingDecisionReplayReconstructionTraceResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasBoundary(RoutingDecisionReplayCapsuleResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasBoundary(RoutingDecisionReplayReadinessChecklistResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasBoundary(RoutingDecisionReplayEvidenceSourceMapResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasBoundary(RoutingDecisionReplayEvidenceBoundarySummaryResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasBoundary(RoutingDecisionReplayEvidenceFieldInventoryResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasBoundary(RoutingDecisionReplayEvidenceNullSafetySummaryResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasBoundary(RoutingDecisionReplayEvidenceStatusRollupResponse response) {
        return response != null && hasText(response.productionNotProvenBoundary());
    }

    private static boolean hasBoundary(RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse response) {
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
