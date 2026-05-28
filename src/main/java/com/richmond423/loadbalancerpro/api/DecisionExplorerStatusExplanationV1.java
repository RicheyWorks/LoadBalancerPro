package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerStatusExplanationV1(
        String explanationObject,
        String contractVersion,
        String status,
        String evidenceQuality,
        String selectedCandidateId,
        String selectedCandidateConfidenceStatus,
        String selectedCandidateHealthEvidenceState,
        String factorStatusRollup,
        String summaryText,
        List<String> reasonCodes,
        List<String> evidenceHighlights,
        List<String> cautionNotes,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String EXPLANATION_OBJECT = "DecisionExplorerStatusExplanationV1";
    public static final String CONTRACT_VERSION = "v1";

    public DecisionExplorerStatusExplanationV1 {
        explanationObject = DecisionExplorerDtoSupport.valueOrDefault(explanationObject, EXPLANATION_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        status = normalizeStatus(status);
        evidenceQuality = DecisionExplorerDtoSupport.valueOrUnknown(evidenceQuality);
        selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateId);
        selectedCandidateConfidenceStatus = normalizeStatus(selectedCandidateConfidenceStatus);
        selectedCandidateHealthEvidenceState = DecisionExplorerDtoSupport.valueOrUnknown(
                selectedCandidateHealthEvidenceState);
        factorStatusRollup = normalizeStatus(factorStatusRollup);
        summaryText = DecisionExplorerDtoSupport.valueOrUnknown(summaryText);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        evidenceHighlights = DecisionExplorerDtoSupport.copyOrEmpty(evidenceHighlights);
        cautionNotes = DecisionExplorerDtoSupport.copyOrEmpty(cautionNotes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    public static DecisionExplorerStatusExplanationV1 unknown(String boundaryNote) {
        return new DecisionExplorerStatusExplanationV1(
                EXPLANATION_OBJECT,
                CONTRACT_VERSION,
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_UNKNOWN,
                "UNKNOWN",
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                "UNKNOWN",
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                "Decision Explorer marks confidence as UNKNOWN because NO_ROUTING_EVIDENCE_RETURNED.",
                List.of("NO_ROUTING_EVIDENCE_RETURNED"),
                List.of("candidateConfidenceDetailCount=0", "factorStatusDetailCount=0"),
                List.of("unknown:routing comparison result evidence was unavailable"),
                List.of(),
                boundaryNote);
    }

    private static String normalizeStatus(String status) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(status);
        return switch (normalized) {
            case DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                    DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                    DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                    DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED -> normalized;
            default -> DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        };
    }
}
