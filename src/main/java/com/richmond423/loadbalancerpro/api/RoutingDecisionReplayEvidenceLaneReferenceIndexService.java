package com.richmond423.loadbalancerpro.api;

import java.util.List;
import java.util.Locale;

public final class RoutingDecisionReplayEvidenceLaneReferenceIndexService {
    private static final String SCHEMA_VERSION = "decision-replay-evidence-lane-reference-index/v1";
    private static final String SOURCE =
            "/api/routing/compare already-built lab evidence reference metadata: decisionVector, "
                    + "dominantFactorAnalysis, decisionDeltaAnalysis, decisionReplaySnapshot, "
                    + "decisionReplayReconstructionTrace, decisionReplayCapsule, "
                    + "decisionReplayReadinessChecklist, decisionReplayEvidenceSourceMap, "
                    + "decisionReplayEvidenceBoundarySummary, decisionReplayEvidenceFieldInventory, "
                    + "decisionReplayEvidenceNullSafetySummary, decisionReplayEvidenceStatusRollup, "
                    + "decisionReplayEvidenceLaneNavigationSummary, and "
                    + "decisionReplayEvidenceLaneDependencyMap";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision Replay Evidence Lane Reference Index is read-only lab reviewer-reference metadata derived "
                    + "only from already-built routing compare response objects; it does not inspect raw server input, "
                    + "does not inspect raw request payloads, does not execute replay, does not "
                    + "perform what-if mutation, does not persist lane-reference-index data or audit logs, "
                    + "does not generate a new fingerprint, does not use reflective field inspection, "
                    + "does not recompute scores, does not infer hidden scoring, does not retune weights, "
                    + "does not change routing behavior, does not add telemetry, does not add external calls, does "
                    + "not add upload/share/download flows, and does not add server-side export/PDF/ZIP "
                    + "generation.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "This lane reference index is not production certification, not live-cloud proof, not real-tenant "
                    + "proof, not SLA/SLO proof, not registry publication proof, not signing proof, not "
                    + "governance application proof, not exact production scoring proof, not cryptographic "
                    + "production proof, not guaranteed replay, and not production traffic validation.";

    private static final List<LaneReferenceSpec> LANE_SPECS = List.of(
            new LaneReferenceSpec(
                    "decision-vector-reference",
                    "Decision Vector reference",
                    "results[].decisionVector",
                    "Decision Vector",
                    "Enterprise Lab Decision Vector",
                    "References the Decision Vector lane for selected candidate, candidate summaries, "
                            + "known signals, and unknown signal metadata."),
            new LaneReferenceSpec(
                    "dominant-factor-analysis-reference",
                    "Dominant Factor Analysis reference",
                    "results[].dominantFactorAnalysis",
                    "Most Influential Signals",
                    "Dominant Factor Analysis",
                    "References the Dominant Factor Analysis lane for already-returned factor interpretation "
                            + "metadata."),
            new LaneReferenceSpec(
                    "decision-delta-analysis-reference",
                    "Decision Delta Analysis reference",
                    "results[].decisionDeltaAnalysis",
                    "Selected vs Closest Alternative",
                    "Decision Delta Analysis",
                    "References the Decision Delta Analysis lane for already-returned selected-vs-alternative "
                            + "delta metadata."),
            new LaneReferenceSpec(
                    "replay-snapshot-reference",
                    "Decision Replay Snapshot reference",
                    "results[].decisionReplaySnapshot",
                    "Decision Replay Snapshot",
                    "Decision Replay Snapshot",
                    "References the Decision Replay Snapshot lane for already-built snapshot metadata and any "
                            + "existing linked fingerprint fields."),
            new LaneReferenceSpec(
                    "reconstruction-trace-reference",
                    "Decision Replay Reconstruction Trace reference",
                    "results[].decisionReplayReconstructionTrace",
                    "Replay Reconstruction Trace",
                    "Decision Replay Reconstruction Trace",
                    "References the Decision Replay Reconstruction Trace lane for already-built reconstruction "
                            + "step metadata."),
            new LaneReferenceSpec(
                    "replay-capsule-reference",
                    "Decision Replay Capsule reference",
                    "results[].decisionReplayCapsule",
                    "Replay Capsule",
                    "Decision Replay Capsule",
                    "References the Decision Replay Capsule lane for already-built capsule packaging metadata."),
            new LaneReferenceSpec(
                    "readiness-checklist-reference",
                    "Decision Replay Readiness Checklist reference",
                    "results[].decisionReplayReadinessChecklist",
                    "Decision Replay Readiness Checklist",
                    "Decision Replay Readiness Checklist",
                    "References the Decision Replay Readiness Checklist lane for already-built checklist "
                            + "status metadata."),
            new LaneReferenceSpec(
                    "evidence-source-map-reference",
                    "Decision Replay Evidence Source Map reference",
                    "results[].decisionReplayEvidenceSourceMap",
                    "Decision Replay Evidence Source Map",
                    "Decision Replay Evidence Source Map",
                    "References the Decision Replay Evidence Source Map lane for already-built source-field "
                            + "relationship metadata."),
            new LaneReferenceSpec(
                    "evidence-boundary-summary-reference",
                    "Decision Replay Evidence Boundary Summary reference",
                    "results[].decisionReplayEvidenceBoundarySummary",
                    "Decision Replay Evidence Boundary Summary",
                    "Decision Replay Evidence Boundary Summary",
                    "References the Decision Replay Evidence Boundary Summary lane for already-built lab-only "
                            + "and not-proven boundary metadata."),
            new LaneReferenceSpec(
                    "evidence-field-inventory-reference",
                    "Decision Replay Evidence Field Inventory reference",
                    "results[].decisionReplayEvidenceFieldInventory",
                    "Decision Replay Evidence Field Inventory",
                    "Decision Replay Evidence Field Inventory",
                    "References the Decision Replay Evidence Field Inventory lane for already-built field-group "
                            + "inventory metadata."),
            new LaneReferenceSpec(
                    "evidence-null-safety-reference",
                    "Decision Evidence Null-Safety Summary reference",
                    "results[].decisionReplayEvidenceNullSafetySummary",
                    "Decision Evidence Null-Safety Summary",
                    "Decision Evidence Null-Safety Summary",
                    "References the Decision Evidence Null-Safety Summary lane for already-built null, "
                            + "missing, unavailable, and no-healthy path metadata."),
            new LaneReferenceSpec(
                    "evidence-status-rollup-reference",
                    "Decision Evidence Status Rollup reference",
                    "results[].decisionReplayEvidenceStatusRollup",
                    "Decision Evidence Status Rollup",
                    "Decision Evidence Status Rollup",
                    "References the Decision Evidence Status Rollup lane for already-built lane status metadata."),
            new LaneReferenceSpec(
                    "evidence-lane-navigation-reference",
                    "Decision Replay Evidence Lane Navigation reference",
                    "results[].decisionReplayEvidenceLaneNavigationSummary",
                    "Decision Evidence Lane Navigation",
                    "Decision Replay Evidence Lane Navigation Summary",
                    "References the Decision Replay Evidence Lane Navigation Summary lane for already-built "
                            + "response path, UI section, and docs metadata."),
            new LaneReferenceSpec(
                    "evidence-lane-dependency-map-reference",
                    "Decision Replay Evidence Lane Dependency Map reference",
                    "results[].decisionReplayEvidenceLaneDependencyMap",
                    "Decision Evidence Lane Dependency Map",
                    "Decision Replay Evidence Lane Dependency Map",
                    "References the Decision Replay Evidence Lane Dependency Map lane for already-built "
                            + "dependency and downstream-count metadata."));

    public RoutingDecisionReplayEvidenceLaneReferenceIndexResponse laneReferenceIndex(
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
            RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigationSummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap) {
        List<DecisionReplayEvidenceLaneReferenceIndexItemResponse> items = LANE_SPECS.stream()
                .map(spec -> item(
                        spec,
                        statusFor(
                                spec.responseFieldPath(),
                                decisionVector,
                                dominantFactorAnalysis,
                                decisionDeltaAnalysis,
                                replaySnapshot,
                                reconstructionTrace,
                                replayCapsule,
                                readinessChecklist,
                                evidenceSourceMap,
                                boundarySummary,
                                fieldInventory,
                                nullSafetySummary,
                                statusRollup,
                                laneNavigationSummary,
                                laneDependencyMap),
                        readOnlyFor(
                                spec.responseFieldPath(),
                                decisionVector,
                                dominantFactorAnalysis,
                                decisionDeltaAnalysis,
                                replaySnapshot,
                                reconstructionTrace,
                                replayCapsule,
                                readinessChecklist,
                                evidenceSourceMap,
                                boundarySummary,
                                fieldInventory,
                                nullSafetySummary,
                                statusRollup,
                                laneNavigationSummary,
                                laneDependencyMap),
                        boundaryFor(
                                spec.responseFieldPath(),
                                decisionVector,
                                dominantFactorAnalysis,
                                decisionDeltaAnalysis,
                                replaySnapshot,
                                reconstructionTrace,
                                replayCapsule,
                                readinessChecklist,
                                evidenceSourceMap,
                                boundarySummary,
                                fieldInventory,
                                nullSafetySummary,
                                statusRollup,
                                laneNavigationSummary,
                                laneDependencyMap),
                        navigationItem(spec.responseFieldPath(), laneNavigationSummary),
                        dependencyItem(spec.responseFieldPath(), laneDependencyMap),
                        laneDependencyMap))
                .toList();

        int availableCount = countStatus(items, STATUS_AVAILABLE);
        int partialCount = countStatus(items, STATUS_PARTIAL);
        int unknownCount = countStatus(items, STATUS_UNKNOWN);
        String selectedCandidateId = selectedCandidateId(decisionVector, replaySnapshot, reconstructionTrace,
                replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory,
                nullSafetySummary, statusRollup, laneNavigationSummary, laneDependencyMap);
        int candidateCount = candidateCount(decisionVector, replaySnapshot, reconstructionTrace, replayCapsule,
                readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory, nullSafetySummary,
                statusRollup, laneNavigationSummary, laneDependencyMap);
        String resolvedStrategyId = strategyId(strategyId, decisionVector, replaySnapshot, reconstructionTrace,
                replayCapsule, readinessChecklist, evidenceSourceMap, boundarySummary, fieldInventory,
                nullSafetySummary, statusRollup, laneNavigationSummary, laneDependencyMap);
        String status = indexStatus(items, selectedCandidateId, candidateCount);

        return new RoutingDecisionReplayEvidenceLaneReferenceIndexResponse(
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

    private static DecisionReplayEvidenceLaneReferenceIndexItemResponse item(
            LaneReferenceSpec spec,
            String status,
            boolean readOnly,
            boolean boundaryPresent,
            DecisionReplayEvidenceLaneNavigationItemResponse navigationItem,
            DecisionReplayEvidenceLaneDependencyItemResponse dependencyItem,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap) {
        int dependencyCount = dependencyCount(spec, dependencyItem, laneDependencyMap);
        int downstreamCount = dependencyItem == null ? 0 : dependencyItem.downstreamCount();
        return new DecisionReplayEvidenceLaneReferenceIndexItemResponse(
                spec.laneId(),
                spec.label(),
                normalizeStatus(status),
                navigationItem == null ? spec.responseFieldPath() : navigationItem.responseFieldPath(),
                navigationItem == null ? spec.uiSectionLabel() : navigationItem.uiSectionLabel(),
                navigationItem == null ? spec.docsReferenceLabel() : navigationItem.docsReferenceLabel(),
                dependencyCount,
                downstreamCount,
                readOnly,
                boundaryPresent,
                spec.referenceSummary(),
                BOUNDARY_NOTE);
    }

    private static int dependencyCount(
            LaneReferenceSpec spec,
            DecisionReplayEvidenceLaneDependencyItemResponse dependencyItem,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap) {
        if (dependencyItem != null) {
            return dependencyItem.dependencyCount();
        }
        if ("results[].decisionReplayEvidenceLaneDependencyMap".equals(spec.responseFieldPath())
                && laneDependencyMap != null
                && laneDependencyMap.dependencyItems() != null) {
            return laneDependencyMap.dependencyItems().size();
        }
        return 0;
    }

    private static DecisionReplayEvidenceLaneNavigationItemResponse navigationItem(
            String responseFieldPath,
            RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigationSummary) {
        if (laneNavigationSummary == null || laneNavigationSummary.navigationItems() == null) {
            return null;
        }
        for (DecisionReplayEvidenceLaneNavigationItemResponse item : laneNavigationSummary.navigationItems()) {
            if (responseFieldPath.equals(item.responseFieldPath())) {
                return item;
            }
        }
        return null;
    }

    private static DecisionReplayEvidenceLaneDependencyItemResponse dependencyItem(
            String responseFieldPath,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap) {
        if (laneDependencyMap == null || laneDependencyMap.dependencyItems() == null) {
            return null;
        }
        for (DecisionReplayEvidenceLaneDependencyItemResponse item : laneDependencyMap.dependencyItems()) {
            if (responseFieldPath.equals(item.responseFieldPath())) {
                return item;
            }
        }
        return null;
    }

    private static String statusFor(
            String responseFieldPath,
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
            RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigationSummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap) {
        return switch (responseFieldPath) {
            case "results[].decisionVector" -> decisionVectorStatus(decisionVector);
            case "results[].dominantFactorAnalysis" ->
                    normalizeStatus(dominantFactorAnalysis == null ? null : dominantFactorAnalysis.status());
            case "results[].decisionDeltaAnalysis" ->
                    normalizeStatus(decisionDeltaAnalysis == null ? null : decisionDeltaAnalysis.status());
            case "results[].decisionReplaySnapshot" ->
                    normalizeStatus(replaySnapshot == null ? null : replaySnapshot.status());
            case "results[].decisionReplayReconstructionTrace" ->
                    normalizeStatus(reconstructionTrace == null ? null : reconstructionTrace.status());
            case "results[].decisionReplayCapsule" ->
                    normalizeStatus(replayCapsule == null ? null : replayCapsule.status());
            case "results[].decisionReplayReadinessChecklist" ->
                    normalizeStatus(readinessChecklist == null ? null : readinessChecklist.status());
            case "results[].decisionReplayEvidenceSourceMap" ->
                    normalizeStatus(evidenceSourceMap == null ? null : evidenceSourceMap.status());
            case "results[].decisionReplayEvidenceBoundarySummary" ->
                    normalizeStatus(boundarySummary == null ? null : boundarySummary.status());
            case "results[].decisionReplayEvidenceFieldInventory" ->
                    normalizeStatus(fieldInventory == null ? null : fieldInventory.status());
            case "results[].decisionReplayEvidenceNullSafetySummary" ->
                    normalizeStatus(nullSafetySummary == null ? null : nullSafetySummary.status());
            case "results[].decisionReplayEvidenceStatusRollup" ->
                    normalizeStatus(statusRollup == null ? null : statusRollup.status());
            case "results[].decisionReplayEvidenceLaneNavigationSummary" ->
                    normalizeStatus(laneNavigationSummary == null ? null : laneNavigationSummary.status());
            case "results[].decisionReplayEvidenceLaneDependencyMap" ->
                    normalizeStatus(laneDependencyMap == null ? null : laneDependencyMap.status());
            default -> STATUS_UNKNOWN;
        };
    }

    private static boolean readOnlyFor(
            String responseFieldPath,
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
            RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigationSummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap) {
        return switch (responseFieldPath) {
            case "results[].decisionVector" -> decisionVector != null && decisionVector.readOnly();
            case "results[].dominantFactorAnalysis" ->
                    dominantFactorAnalysis != null && dominantFactorAnalysis.readOnly();
            case "results[].decisionDeltaAnalysis" ->
                    decisionDeltaAnalysis != null && decisionDeltaAnalysis.readOnly();
            case "results[].decisionReplaySnapshot" -> replaySnapshot != null && replaySnapshot.readOnly();
            case "results[].decisionReplayReconstructionTrace" ->
                    reconstructionTrace != null && reconstructionTrace.readOnly();
            case "results[].decisionReplayCapsule" -> replayCapsule != null && replayCapsule.readOnly();
            case "results[].decisionReplayReadinessChecklist" ->
                    readinessChecklist != null && readinessChecklist.readOnly();
            case "results[].decisionReplayEvidenceSourceMap" -> evidenceSourceMap != null && evidenceSourceMap.readOnly();
            case "results[].decisionReplayEvidenceBoundarySummary" ->
                    boundarySummary != null && boundarySummary.readOnly();
            case "results[].decisionReplayEvidenceFieldInventory" -> fieldInventory != null && fieldInventory.readOnly();
            case "results[].decisionReplayEvidenceNullSafetySummary" ->
                    nullSafetySummary != null && nullSafetySummary.readOnly();
            case "results[].decisionReplayEvidenceStatusRollup" -> statusRollup != null && statusRollup.readOnly();
            case "results[].decisionReplayEvidenceLaneNavigationSummary" ->
                    laneNavigationSummary != null && laneNavigationSummary.readOnly();
            case "results[].decisionReplayEvidenceLaneDependencyMap" ->
                    laneDependencyMap != null && laneDependencyMap.readOnly();
            default -> false;
        };
    }

    private static boolean boundaryFor(
            String responseFieldPath,
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
            RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigationSummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap) {
        return switch (responseFieldPath) {
            case "results[].decisionVector" -> hasBoundary(decisionVector);
            case "results[].dominantFactorAnalysis" -> hasBoundary(dominantFactorAnalysis);
            case "results[].decisionDeltaAnalysis" -> hasBoundary(decisionDeltaAnalysis);
            case "results[].decisionReplaySnapshot" -> hasBoundary(replaySnapshot);
            case "results[].decisionReplayReconstructionTrace" -> hasBoundary(reconstructionTrace);
            case "results[].decisionReplayCapsule" -> hasBoundary(replayCapsule);
            case "results[].decisionReplayReadinessChecklist" -> hasBoundary(readinessChecklist);
            case "results[].decisionReplayEvidenceSourceMap" -> hasBoundary(evidenceSourceMap);
            case "results[].decisionReplayEvidenceBoundarySummary" -> hasBoundary(boundarySummary);
            case "results[].decisionReplayEvidenceFieldInventory" -> hasBoundary(fieldInventory);
            case "results[].decisionReplayEvidenceNullSafetySummary" -> hasBoundary(nullSafetySummary);
            case "results[].decisionReplayEvidenceStatusRollup" -> hasBoundary(statusRollup);
            case "results[].decisionReplayEvidenceLaneNavigationSummary" -> hasBoundary(laneNavigationSummary);
            case "results[].decisionReplayEvidenceLaneDependencyMap" -> hasBoundary(laneDependencyMap);
            default -> false;
        };
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

    private static String indexStatus(
            List<DecisionReplayEvidenceLaneReferenceIndexItemResponse> items,
            String selectedCandidateId,
            int candidateCount) {
        boolean allUnknown = items.stream().allMatch(item -> STATUS_UNKNOWN.equals(item.status()));
        if (allUnknown && isBlank(selectedCandidateId) && candidateCount == 0) {
            return STATUS_UNKNOWN;
        }
        boolean allAvailable = items.stream().allMatch(item -> STATUS_AVAILABLE.equals(item.status()));
        return allAvailable ? STATUS_AVAILABLE : STATUS_PARTIAL;
    }

    private static int countStatus(
            List<DecisionReplayEvidenceLaneReferenceIndexItemResponse> items,
            String status) {
        return (int) items.stream().filter(item -> status.equals(item.status())).count();
    }

    private static String explanation(
            String status,
            String strategyId,
            String selectedCandidateId,
            int candidateCount,
            int itemCount) {
        if (STATUS_UNKNOWN.equals(status)) {
            return "Decision Replay Evidence Lane Reference Index is UNKNOWN because already-built lab compare "
                    + "evidence does not include an available selected candidate, candidate set, or evidence-lane "
                    + "status. No selected candidate, candidate set, closest alternative, score gap, largest "
                    + "delta factor, fingerprint, replay claim, certification claim, or guaranteed replay is "
                    + "invented.";
        }
        return "Decision Replay Evidence Lane Reference Index is " + status
                + " and derived from already-built lab compare evidence only for strategy "
                + safeValue(strategyId)
                + ", selected candidate "
                + (isBlank(selectedCandidateId) ? "unavailable" : selectedCandidateId)
                + ", candidate count "
                + safeCount(candidateCount)
                + ", and "
                + itemCount
                + " deterministic reference items.";
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
            RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigationSummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap) {
        return firstNonBlank(
                laneDependencyMap == null ? null : laneDependencyMap.selectedCandidateId(),
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
            RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigationSummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap) {
        return firstPositive(
                laneDependencyMap == null ? 0 : laneDependencyMap.candidateCount(),
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
            RoutingDecisionReplayEvidenceLaneNavigationSummaryResponse laneNavigationSummary,
            RoutingDecisionReplayEvidenceLaneDependencyMapResponse laneDependencyMap) {
        return firstNonBlank(
                strategyId,
                laneDependencyMap == null ? null : laneDependencyMap.strategyId(),
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

    private static boolean hasBoundary(RoutingDecisionReplayEvidenceLaneDependencyMapResponse response) {
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

    private record LaneReferenceSpec(
            String laneId,
            String label,
            String responseFieldPath,
            String uiSectionLabel,
            String docsReferenceLabel,
            String referenceSummary) {
    }
}
