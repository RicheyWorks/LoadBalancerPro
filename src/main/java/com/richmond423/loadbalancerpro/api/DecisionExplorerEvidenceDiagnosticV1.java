package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerEvidenceDiagnosticV1(
        String diagnosticId,
        String category,
        String status,
        String severity,
        int evidenceCount,
        String summaryText,
        List<String> reasonCodes,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String STATUS_PRESENT = "PRESENT";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_UNKNOWN = "UNKNOWN";
    public static final String STATUS_MISSING = "MISSING";
    public static final String STATUS_DEGRADED = "DEGRADED";

    public DecisionExplorerEvidenceDiagnosticV1 {
        diagnosticId = DecisionExplorerDtoSupport.valueOrUnknown(diagnosticId);
        category = DecisionExplorerDtoSupport.valueOrUnknown(category);
        status = normalizeStatus(status);
        severity = DecisionExplorerDtoSupport.valueOrDefault(severity, severityFor(status));
        evidenceCount = Math.max(0, evidenceCount);
        summaryText = DecisionExplorerDtoSupport.valueOrUnknown(summaryText);
        reasonCodes = DecisionExplorerDtoSupport.copyOrEmpty(reasonCodes);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    static String severityFor(String status) {
        return switch (normalizeStatus(status)) {
            case STATUS_PRESENT -> "INFO";
            case STATUS_PARTIAL, STATUS_UNKNOWN -> "REVIEW";
            case STATUS_MISSING -> "WARNING";
            case STATUS_DEGRADED -> "BLOCKING";
            default -> "REVIEW";
        };
    }

    private static String normalizeStatus(String status) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(status);
        return switch (normalized) {
            case STATUS_PRESENT, STATUS_PARTIAL, STATUS_UNKNOWN, STATUS_MISSING, STATUS_DEGRADED -> normalized;
            default -> STATUS_UNKNOWN;
        };
    }
}
