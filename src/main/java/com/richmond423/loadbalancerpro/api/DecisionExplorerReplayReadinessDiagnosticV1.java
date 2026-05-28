package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerReplayReadinessDiagnosticV1(
        boolean readOnly,
        boolean simulationOnly,
        String diagnosticObject,
        String contractVersion,
        String readinessStatus,
        String sufficiencyLevel,
        int readinessScore,
        boolean replayExecutionAvailable,
        boolean replayStorageAvailable,
        boolean replayExportAvailable,
        String candidateEvidenceStatus,
        String alternativeEvidenceStatus,
        String scoreEvidenceStatus,
        String factorEvidenceStatus,
        String fingerprintEvidenceStatus,
        List<String> presentEvidenceSignals,
        List<String> partialEvidenceSignals,
        List<String> missingEvidenceSignals,
        List<String> degradedEvidenceSignals,
        List<String> incompatibleEvidenceSignals,
        List<String> readinessChecklist,
        List<String> limitationSignals,
        List<String> sourceReferenceIds,
        String explanationText,
        String boundaryNote) {
    public static final String DIAGNOSTIC_OBJECT = "DecisionExplorerReplayReadinessDiagnosticV1";
    public static final String CONTRACT_VERSION = "v1";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_UNKNOWN = "UNKNOWN";
    public static final String STATUS_DEGRADED = "DEGRADED";
    public static final String EVIDENCE_AVAILABLE = "AVAILABLE";
    public static final String EVIDENCE_PARTIAL = "PARTIAL";
    public static final String EVIDENCE_MISSING = "MISSING";
    public static final String EVIDENCE_UNKNOWN = "UNKNOWN";
    public static final String EVIDENCE_DEGRADED = "DEGRADED";

    public DecisionExplorerReplayReadinessDiagnosticV1 {
        readOnly = true;
        simulationOnly = true;
        diagnosticObject = DecisionExplorerDtoSupport.valueOrDefault(diagnosticObject, DIAGNOSTIC_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        readinessStatus = normalizeStatus(readinessStatus);
        sufficiencyLevel = normalizeSufficiencyLevel(sufficiencyLevel);
        readinessScore = Math.max(0, Math.min(100, readinessScore));
        replayExecutionAvailable = false;
        replayStorageAvailable = false;
        replayExportAvailable = false;
        candidateEvidenceStatus = normalizeEvidenceStatus(candidateEvidenceStatus);
        alternativeEvidenceStatus = normalizeEvidenceStatus(alternativeEvidenceStatus);
        scoreEvidenceStatus = normalizeEvidenceStatus(scoreEvidenceStatus);
        factorEvidenceStatus = normalizeEvidenceStatus(factorEvidenceStatus);
        fingerprintEvidenceStatus = normalizeEvidenceStatus(fingerprintEvidenceStatus);
        presentEvidenceSignals = DecisionExplorerDtoSupport.copyOrEmpty(presentEvidenceSignals);
        partialEvidenceSignals = DecisionExplorerDtoSupport.copyOrEmpty(partialEvidenceSignals);
        missingEvidenceSignals = DecisionExplorerDtoSupport.copyOrEmpty(missingEvidenceSignals);
        degradedEvidenceSignals = DecisionExplorerDtoSupport.copyOrEmpty(degradedEvidenceSignals);
        incompatibleEvidenceSignals = DecisionExplorerDtoSupport.copyOrEmpty(incompatibleEvidenceSignals);
        readinessChecklist = DecisionExplorerDtoSupport.copyOrEmpty(readinessChecklist);
        limitationSignals = DecisionExplorerDtoSupport.copyOrEmpty(limitationSignals);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        explanationText = DecisionExplorerDtoSupport.valueOrUnknown(explanationText);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    public static DecisionExplorerReplayReadinessDiagnosticV1 unknown(String boundaryNote) {
        return new DecisionExplorerReplayReadinessDiagnosticV1(
                true,
                true,
                DIAGNOSTIC_OBJECT,
                CONTRACT_VERSION,
                STATUS_UNKNOWN,
                DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT,
                0,
                false,
                false,
                false,
                EVIDENCE_UNKNOWN,
                EVIDENCE_UNKNOWN,
                EVIDENCE_UNKNOWN,
                EVIDENCE_UNKNOWN,
                EVIDENCE_UNKNOWN,
                List.of(),
                List.of(),
                List.of("route tradeoff evidence was unavailable"),
                List.of(),
                List.of("replay execution, storage, and export are intentionally unavailable"),
                List.of("candidate evidence unavailable", "score evidence unavailable",
                        "factor evidence unavailable"),
                List.of("Replay-readiness diagnostics are read-only and do not execute replay."),
                List.of(),
                "Replay-readiness diagnostics are UNKNOWN because route tradeoff evidence was unavailable. "
                        + "No replay execution, storage, export, or proof is produced.",
                boundaryNote);
    }

    private static String normalizeStatus(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case STATUS_READY, STATUS_PARTIAL, STATUS_UNKNOWN, STATUS_DEGRADED -> normalized;
            default -> STATUS_UNKNOWN;
        };
    }

    private static String normalizeSufficiencyLevel(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case DecisionExplorerEvidenceSufficiencyV1.LEVEL_REPLAY_STYLE_READY,
                    DecisionExplorerEvidenceSufficiencyV1.LEVEL_TRADEOFF_READY,
                    DecisionExplorerEvidenceSufficiencyV1.LEVEL_BASIC_DIAGNOSTICS_ONLY,
                    DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT,
                    DecisionExplorerEvidenceSufficiencyV1.LEVEL_DEGRADED -> normalized;
            default -> DecisionExplorerEvidenceSufficiencyV1.LEVEL_INSUFFICIENT;
        };
    }

    private static String normalizeEvidenceStatus(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case EVIDENCE_AVAILABLE,
                    EVIDENCE_PARTIAL,
                    EVIDENCE_MISSING,
                    EVIDENCE_UNKNOWN,
                    EVIDENCE_DEGRADED -> normalized;
            default -> EVIDENCE_UNKNOWN;
        };
    }
}
