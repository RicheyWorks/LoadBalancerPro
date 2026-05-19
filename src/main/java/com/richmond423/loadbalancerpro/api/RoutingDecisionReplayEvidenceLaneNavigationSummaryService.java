package com.richmond423.loadbalancerpro.api;

import java.util.List;
import java.util.Locale;

public final class RoutingDecisionReplayEvidenceLaneNavigationSummaryService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-lane-navigation-summary/v1";
    private static final String SOURCE =
            "/api/routing/compare already-built lab evidence navigation metadata: decisionVector, "
                    + "dominantFactorAnalysis, decisionDeltaAnalysis, decisionReplaySnapshot, "
                    + "decisionReplayReconstructionTrace, decisionReplayCapsule, "
                    + "decisionReplayReadinessChecklist, decisionReplayEvidenceSourceMap, "
                    + "decisionReplayEvidenceBoundarySummary, decisionReplayEvidenceFieldInventory, "
                    + "decisionReplayEvidenceNullSafetySummary, and decisionReplayEvidenceStatusRollup";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision Replay Evidence Lane Navigation Summary is read-only lab navigation metadata derived only "
                    + "from already-built routing compare response objects; it does not inspect raw server input, "
                    + "does not inspect raw request payloads, does not execute replay, does not perform what-if "
                    + "mutation, does not persist lane-navigation data or audit logs, does not generate "
                    + "fingerprints, does not use reflective field inspection, does not recompute scores, does "
                    + "not retune weights, does not change routing behavior, does not add telemetry, does not "
                    + "add external calls, does not add upload/share/download flows, and does not add "
                    + "server-side export/PDF/ZIP generation.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "This lane navigation summary is not production certification, not live-cloud proof, not real-tenant "
                    + "proof, not SLA/SLO proof, not registry publication proof, not signing proof, not "
                    + "governance application proof, not exact production scoring proof, not cryptographic "
                    + "production proof, not guaranteed replay, and not production traffic validation.";

    public RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigationSummary(
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
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        List<DecisionReplayEvidenceLaneNavigationItemResponse> items = List.of(
                item(
                        "decision-vector-navigation",
                        "Decision Vector navigation",
                        decisionVectorStatus(decisionVector),
                        "results[].decisionVector",
                        "Decision Vector",
                        "Enterprise Lab Decision Vector",
                        decisionVector != null && decisionVector.readOnly(),
                        hasBoundary(decisionVector),
                        "Navigate to the Decision Vector response field and UI section for selected candidate, "
                                + "candidate summary, known signal, and unknown signal metadata."),
                item(
                        "dominant-factor-analysis-navigation",
                        "Dominant Factor Analysis navigation",
                        normalizeStatus(dominantFactorAnalysis == null ? null : dominantFactorAnalysis.status()),
                        "results[].dominantFactorAnalysis",
                        "Most Influential Signals",
                        "Dominant Factor Analysis",
                        dominantFactorAnalysis != null && dominantFactorAnalysis.readOnly(),
                        hasBoundary(dominantFactorAnalysis),
                        "Navigate to the Dominant Factor Analysis response field and UI section for returned "
                                + "factor contribution interpretation metadata."),
                item(
                        "decision-delta-analysis-navigation",
                        "Decision Delta Analysis navigation",
                        normalizeStatus(decisionDeltaAnalysis == null ? null : decisionDeltaAnalysis.status()),
                        "results[].decisionDeltaAnalysis",
                        "Selected vs Closest Alternative",
                        "Decision Delta Analysis",
                        decisionDeltaAnalysis != null && decisionDeltaAnalysis.readOnly(),
                        hasBoundary(decisionDeltaAnalysis),
                        "Navigate to the Decision Delta Analysis response field and UI section for already-returned "
                                + "selected-vs-alternative delta metadata."),
                item(
                        "replay-snapshot-navigation",
                        "Decision Replay Snapshot navigation",
                        normalizeStatus(replaySnapshot == null ? null : replaySnapshot.status()),
                        "results[].decisionReplaySnapshot",
                        "Decision Replay Snapshot",
                        "Decision Replay Snapshot",
                        replaySnapshot != null && replaySnapshot.readOnly(),
                        hasBoundary(replaySnapshot),
                        "Navigate to the Decision Replay Snapshot response field and UI section for already-built "
                                + "snapshot metadata and existing snapshot fingerprint, when available."),
                item(
                        "reconstruction-trace-navigation",
                        "Decision Replay Reconstruction Trace navigation",
                        normalizeStatus(reconstructionTrace == null ? null : reconstructionTrace.status()),
                        "results[].decisionReplayReconstructionTrace",
                        "Replay Reconstruction Trace",
                        "Decision Replay Reconstruction Trace",
                        reconstructionTrace != null && reconstructionTrace.readOnly(),
                        hasBoundary(reconstructionTrace),
                        "Navigate to the Decision Replay Reconstruction Trace response field and UI section for "
                                + "already-built reconstruction-step metadata."),
                item(
                        "replay-capsule-navigation",
                        "Decision Replay Capsule navigation",
                        normalizeStatus(replayCapsule == null ? null : replayCapsule.status()),
                        "results[].decisionReplayCapsule",
                        "Replay Capsule",
                        "Decision Replay Capsule",
                        replayCapsule != null && replayCapsule.readOnly(),
                        hasBoundary(replayCapsule),
                        "Navigate to the Decision Replay Capsule response field and UI section for canonical "
                                + "already-built lab evidence packaging metadata."),
                item(
                        "readiness-checklist-navigation",
                        "Decision Replay Readiness Checklist navigation",
                        normalizeStatus(readinessChecklist == null ? null : readinessChecklist.status()),
                        "results[].decisionReplayReadinessChecklist",
                        "Decision Replay Readiness Checklist",
                        "Decision Replay Readiness Checklist",
                        readinessChecklist != null && readinessChecklist.readOnly(),
                        hasBoundary(readinessChecklist),
                        "Navigate to the Decision Replay Readiness Checklist response field and UI section for "
                                + "already-built checklist status metadata."),
                item(
                        "evidence-source-map-navigation",
                        "Decision Replay Evidence Source Map navigation",
                        normalizeStatus(evidenceSourceMap == null ? null : evidenceSourceMap.status()),
                        "results[].decisionReplayEvidenceSourceMap",
                        "Decision Replay Evidence Source Map",
                        "Decision Replay Evidence Source Map",
                        evidenceSourceMap != null && evidenceSourceMap.readOnly(),
                        hasBoundary(evidenceSourceMap),
                        "Navigate to the Decision Replay Evidence Source Map response field and UI section for "
                                + "already-built source-field relationship metadata."),
                item(
                        "evidence-boundary-summary-navigation",
                        "Decision Replay Evidence Boundary Summary navigation",
                        normalizeStatus(boundarySummary == null ? null : boundarySummary.status()),
                        "results[].decisionReplayEvidenceBoundarySummary",
                        "Decision Replay Evidence Boundary Summary",
                        "Decision Replay Evidence Boundary Summary",
                        boundarySummary != null && boundarySummary.readOnly(),
                        hasBoundary(boundarySummary),
                        "Navigate to the Decision Replay Evidence Boundary Summary response field and UI section "
                                + "for already-built lab-only and not-proven boundary metadata."),
                item(
                        "evidence-field-inventory-navigation",
                        "Decision Replay Evidence Field Inventory navigation",
                        normalizeStatus(fieldInventory == null ? null : fieldInventory.status()),
                        "results[].decisionReplayEvidenceFieldInventory",
                        "Decision Replay Evidence Field Inventory",
                        "Decision Replay Evidence Field Inventory",
                        fieldInventory != null && fieldInventory.readOnly(),
                        hasBoundary(fieldInventory),
                        "Navigate to the Decision Replay Evidence Field Inventory response field and UI section "
                                + "for already-built field-group inventory metadata."),
                item(
                        "evidence-null-safety-navigation",
                        "Decision Evidence Null-Safety Summary navigation",
                        normalizeStatus(nullSafetySummary == null ? null : nullSafetySummary.status()),
                        "results[].decisionReplayEvidenceNullSafetySummary",
                        "Decision Evidence Null-Safety Summary",
                        "Decision Evidence Null-Safety Summary",
                        nullSafetySummary != null && nullSafetySummary.readOnly(),
                        hasBoundary(nullSafetySummary),
                        "Navigate to the Decision Evidence Null-Safety Summary response field and UI section for "
                                + "already-built null, missing, unavailable, and no-healthy path metadata."),
                item(
                        "evidence-status-rollup-navigation",
                        "Decision Evidence Status Rollup navigation",
                        normalizeStatus(statusRollup == null ? null : statusRollup.status()),
                        "results[].decisionReplayEvidenceStatusRollup",
                        "Decision Evidence Status Rollup",
                        "Decision Evidence Status Rollup",
                        statusRollup != null && statusRollup.readOnly(),
                        hasBoundary(statusRollup),
                        "Navigate to the Decision Evidence Status Rollup response field and UI section for "
                                + "already-built lane status metadata."));

        int availableCount = countStatus(items, STATUS_AVAILABLE);
        int partialCount = countStatus(items, STATUS_PARTIAL);
        int unknownCount = countStatus(items, STATUS_UNKNOWN);
        String selectedCandidateId = selectedCandidateId(decisionVector, replaySnapshot, reconstructionTrace,
                replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory,
                nullSafetySummary, statusRollup);
        int candidateCount = candidateCount(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule,
                readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory, nullSafetySummary,
                statusRollup);
        String resolvedStrategyId = strategyId(strategyId, decisionVector, replaySnapshot, reconstructionTrace,
                replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory,
                nullSafetySummary, statusRollup);
        String status = summaryStatus(items, selectedCandidateId, candidateCount);

        return new RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse(
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

    private static DecisionReplayEvidenceLaneNavigationItemResponse item(
            String laneId,
            String label,
            String status,
            String responseFieldPath,
            String uiSectionLabel,
            String docsReferenceLabel,
            boolean readOnly,
            boolean boundaryPresent,
            String navigationSummary) {
        return new DecisionReplayEvidenceLaneNavigationItemResponse(
                laneId,
                label,
                normalizeStatus(status),
                responseFieldPath,
                uiSectionLabel,
                docsReferenceLabel,
                readOnly,
                boundaryPresent,
                navigationSummary,
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

    private static String summaryStatus(
            List<DecisionReplayEvidenceLaneNavigationItemResponse> items,
            String selectedCandidateId,
            int candidateCount) {
        boolean allUnknown = items.stream().allMatch(item -> STATUS_UNKNOWN.equals(item.status()));
        if (allUnknown && isBlank(selectedCandidateId) && candidateCount == 0) {
            return STATUS_UNKNOWN;
        }
        boolean allAvailable = items.stream().allMatch(item -> STATUS_AVAILABLE.equals(item.status()));
        return allAvailable ? STATUS_AVAILABLE : STATUS_PARTIAL;
    }

    private static int countStatus(List<DecisionReplayEvidenceLaneNavigationItemResponse> items, String status) {
        return (int) items.stream().filter(item -> status.equals(item.status())).count();
    }

    private static String explanation(
            String status,
            String strategyId,
            String selectedCandidateId,
            int candidateCount,
            int itemCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision Replay Evidence Lane Navigation Summary is UNKNOWN because already-built lab compare "
                    + "evidence does not include an available selected candidate, candidate set, or evidence-lane "
                    + "status. No replay execution, certification, production proof, guaranteed replay, "
                    + "or navigation value is invented.";
        }
        return "Decision Replay Evidence Lane Navigation Summary is " + status
                + " and derived from already-built lab compare evidence only for strategy "
                + safeValue(strategyId)
                + ", selected candidate "
                + (isBlank(selectedCandidateId) ? "unavailable" : selectedCandidateId)
                + ", candidate count "
                + safeCount(candidateCount)
                + ", and "
                + itemCount
                + " deterministic navigation items.";
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
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return firstNonBlank(
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
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return firstPositive(
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
            RoutingDecisionReplayEvidenceStatusRollupResponse statusRollup) {
        return firstNonBlank(
                strategyId,
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
