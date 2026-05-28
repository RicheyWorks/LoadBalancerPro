package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerRoutingDiagnosticsV1(
        boolean readOnly,
        boolean simulationOnly,
        String diagnosticsObject,
        String contractVersion,
        String overallStatus,
        String evidenceQuality,
        String selectedCandidateId,
        int diagnosticCount,
        int presentEvidenceCount,
        int partialEvidenceCount,
        int missingEvidenceCount,
        int degradedEvidenceCount,
        int unknownEvidenceCount,
        List<DecisionExplorerEvidenceDiagnosticV1> evidenceDiagnostics,
        List<String> diagnosticReasons,
        List<String> warnings,
        List<String> unknowns,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String DIAGNOSTICS_OBJECT = "DecisionExplorerRoutingDiagnosticsV1";
    public static final String CONTRACT_VERSION = "v1";

    public DecisionExplorerRoutingDiagnosticsV1 {
        readOnly = true;
        simulationOnly = true;
        diagnosticsObject = DecisionExplorerDtoSupport.valueOrDefault(diagnosticsObject, DIAGNOSTICS_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        overallStatus = normalizeConfidenceStatus(overallStatus);
        evidenceQuality = normalizeEvidenceQuality(evidenceQuality, overallStatus);
        selectedCandidateId = DecisionExplorerDtoSupport.valueOrUnknown(selectedCandidateId);
        diagnosticCount = Math.max(0, diagnosticCount);
        presentEvidenceCount = Math.max(0, presentEvidenceCount);
        partialEvidenceCount = Math.max(0, partialEvidenceCount);
        missingEvidenceCount = Math.max(0, missingEvidenceCount);
        degradedEvidenceCount = Math.max(0, degradedEvidenceCount);
        unknownEvidenceCount = Math.max(0, unknownEvidenceCount);
        evidenceDiagnostics = DecisionExplorerDtoSupport.copyOrEmpty(evidenceDiagnostics);
        diagnosticReasons = DecisionExplorerDtoSupport.copyOrEmpty(diagnosticReasons);
        warnings = DecisionExplorerDtoSupport.copyOrEmpty(warnings);
        unknowns = DecisionExplorerDtoSupport.copyOrEmpty(unknowns);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    public static DecisionExplorerRoutingDiagnosticsV1 unknown(String boundaryNote) {
        DecisionExplorerEvidenceDiagnosticV1 diagnostic = new DecisionExplorerEvidenceDiagnosticV1(
                "routing-evidence",
                "EVIDENCE",
                DecisionExplorerEvidenceDiagnosticV1.STATUS_UNKNOWN,
                "REVIEW",
                0,
                "Routing evidence diagnostics could not be computed because no confidence summary was returned.",
                List.of("NO_CONFIDENCE_SUMMARY_RETURNED"),
                List.of(),
                boundaryNote);
        return new DecisionExplorerRoutingDiagnosticsV1(
                true,
                true,
                DIAGNOSTICS_OBJECT,
                CONTRACT_VERSION,
                DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_UNKNOWN,
                "UNKNOWN",
                1,
                0,
                0,
                0,
                0,
                1,
                List.of(diagnostic),
                List.of("NO_CONFIDENCE_SUMMARY_RETURNED"),
                List.of(),
                List.of("confidence summary evidence was unavailable"),
                List.of(),
                boundaryNote);
    }

    private static String normalizeConfidenceStatus(String status) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(status);
        return switch (normalized) {
            case DecisionExplorerConfidenceSummaryV1.STATUS_STRONG,
                    DecisionExplorerConfidenceSummaryV1.STATUS_PARTIAL,
                    DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN,
                    DecisionExplorerConfidenceSummaryV1.STATUS_DEGRADED -> normalized;
            default -> DecisionExplorerConfidenceSummaryV1.STATUS_UNKNOWN;
        };
    }

    private static String normalizeEvidenceQuality(String evidenceQuality, String status) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(evidenceQuality);
        return switch (normalized) {
            case DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_COMPLETE,
                    DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_PARTIAL,
                    DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_UNKNOWN,
                    DecisionExplorerConfidenceSummaryV1.EVIDENCE_QUALITY_DEGRADED -> normalized;
            default -> DecisionExplorerConfidenceSummaryV1.evidenceQualityFor(status);
        };
    }
}
