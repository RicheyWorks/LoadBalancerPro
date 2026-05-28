package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerConfidenceSummaryV1(
        boolean readOnly,
        boolean simulationOnly,
        String summaryObject,
        String contractVersion,
        String status,
        String evidenceQuality,
        String selectedCandidateId,
        int candidateCount,
        int candidateComparisonCount,
        int availableFactorCount,
        int partialFactorCount,
        int unknownFactorCount,
        int warningCount,
        int unknownCount,
        int sourceReferenceCount,
        List<String> evidenceSignals,
        List<String> statusReasons,
        List<String> warnings,
        List<String> unknowns,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String SUMMARY_OBJECT = "DecisionExplorerConfidenceSummaryV1";
    public static final String CONTRACT_VERSION = "v1";
    public static final String STATUS_STRONG = "STRONG";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_UNKNOWN = "UNKNOWN";
    public static final String STATUS_DEGRADED = "DEGRADED";
    public static final String EVIDENCE_QUALITY_COMPLETE = "COMPLETE";
    public static final String EVIDENCE_QUALITY_PARTIAL = "PARTIAL";
    public static final String EVIDENCE_QUALITY_UNKNOWN = "UNKNOWN";
    public static final String EVIDENCE_QUALITY_DEGRADED = "DEGRADED";

    public DecisionExplorerConfidenceSummaryV1 {
        readOnly = true;
        simulationOnly = true;
        summaryObject = DecisionExplorerDtoSupport.valueOrDefault(summaryObject, SUMMARY_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        status = normalizeStatus(status);
        evidenceQuality = normalizeEvidenceQuality(evidenceQuality, status);
        selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateId);
        candidateCount = Math.max(0, candidateCount);
        candidateComparisonCount = Math.max(0, candidateComparisonCount);
        availableFactorCount = Math.max(0, availableFactorCount);
        partialFactorCount = Math.max(0, partialFactorCount);
        unknownFactorCount = Math.max(0, unknownFactorCount);
        warningCount = Math.max(0, warningCount);
        unknownCount = Math.max(0, unknownCount);
        sourceReferenceCount = Math.max(0, sourceReferenceCount);
        evidenceSignals = DecisionExplorerDtoSupport.copyOrEmpty(evidenceSignals);
        statusReasons = DecisionExplorerDtoSupport.copyOrEmpty(statusReasons);
        warnings = DecisionExplorerDtoSupport.copyOrEmpty(warnings);
        unknowns = DecisionExplorerDtoSupport.copyOrEmpty(unknowns);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    public static DecisionExplorerConfidenceSummaryV1 unknown(String boundaryNote) {
        return new DecisionExplorerConfidenceSummaryV1(
                true,
                true,
                SUMMARY_OBJECT,
                CONTRACT_VERSION,
                STATUS_UNKNOWN,
                EVIDENCE_QUALITY_UNKNOWN,
                "UNKNOWN",
                0,
                0,
                0,
                0,
                0,
                0,
                1,
                0,
                List.of("routingEvidence=unavailable"),
                List.of("NO_ROUTING_EVIDENCE_RETURNED"),
                List.of(),
                List.of("routing comparison result evidence was unavailable"),
                List.of(),
                boundaryNote);
    }

    static String evidenceQualityFor(String status) {
        return switch (normalizeStatus(status)) {
            case STATUS_STRONG -> EVIDENCE_QUALITY_COMPLETE;
            case STATUS_PARTIAL -> EVIDENCE_QUALITY_PARTIAL;
            case STATUS_DEGRADED -> EVIDENCE_QUALITY_DEGRADED;
            default -> EVIDENCE_QUALITY_UNKNOWN;
        };
    }

    private static String normalizeStatus(String status) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(status);
        return switch (normalized) {
            case STATUS_STRONG, STATUS_PARTIAL, STATUS_UNKNOWN, STATUS_DEGRADED -> normalized;
            default -> STATUS_UNKNOWN;
        };
    }

    private static String normalizeEvidenceQuality(String evidenceQuality, String status) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(evidenceQuality);
        return switch (normalized) {
            case EVIDENCE_QUALITY_COMPLETE, EVIDENCE_QUALITY_PARTIAL, EVIDENCE_QUALITY_UNKNOWN,
                    EVIDENCE_QUALITY_DEGRADED -> normalized;
            default -> evidenceQualityFor(status);
        };
    }
}
