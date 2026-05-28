package com.richmond423.loadbalancerpro.api;

import java.util.List;

public record DecisionExplorerEvidenceSufficiencyV1(
        boolean readOnly,
        boolean simulationOnly,
        String diagnosticObject,
        String contractVersion,
        String sufficiencyLevel,
        int readinessScore,
        boolean basicDiagnosticsReady,
        boolean tradeoffAnalysisReady,
        boolean replayStyleAnalysisReady,
        int candidateEvidenceCount,
        int comparableAlternativeCount,
        int factorDeltaCount,
        int presentEvidenceCount,
        int partialEvidenceCount,
        int missingEvidenceCount,
        int degradedEvidenceCount,
        int unknownEvidenceCount,
        List<String> presentEvidenceSignals,
        List<String> partialEvidenceSignals,
        List<String> missingEvidenceSignals,
        List<String> degradedEvidenceSignals,
        List<String> unknownEvidenceSignals,
        List<String> readinessReasons,
        String fingerprintAlgorithm,
        String diagnosticFingerprint,
        String reproducibilityKey,
        List<String> fingerprintInputs,
        List<String> sourceReferenceIds,
        String boundaryNote) {
    public static final String DIAGNOSTIC_OBJECT = "DecisionExplorerEvidenceSufficiencyV1";
    public static final String CONTRACT_VERSION = "v1";
    public static final String LEVEL_REPLAY_STYLE_READY = "REPLAY_STYLE_READY";
    public static final String LEVEL_TRADEOFF_READY = "TRADEOFF_READY";
    public static final String LEVEL_BASIC_DIAGNOSTICS_ONLY = "BASIC_DIAGNOSTICS_ONLY";
    public static final String LEVEL_INSUFFICIENT = "INSUFFICIENT";
    public static final String LEVEL_DEGRADED = "DEGRADED";

    public DecisionExplorerEvidenceSufficiencyV1 {
        readOnly = true;
        simulationOnly = true;
        diagnosticObject = DecisionExplorerDtoSupport.valueOrDefault(diagnosticObject, DIAGNOSTIC_OBJECT);
        contractVersion = DecisionExplorerDtoSupport.valueOrDefault(contractVersion, CONTRACT_VERSION);
        sufficiencyLevel = normalizeLevel(sufficiencyLevel);
        readinessScore = Math.max(0, Math.min(100, readinessScore));
        candidateEvidenceCount = Math.max(0, candidateEvidenceCount);
        comparableAlternativeCount = Math.max(0, comparableAlternativeCount);
        factorDeltaCount = Math.max(0, factorDeltaCount);
        presentEvidenceSignals = DecisionExplorerDtoSupport.copyOrEmpty(presentEvidenceSignals);
        partialEvidenceSignals = DecisionExplorerDtoSupport.copyOrEmpty(partialEvidenceSignals);
        missingEvidenceSignals = DecisionExplorerDtoSupport.copyOrEmpty(missingEvidenceSignals);
        degradedEvidenceSignals = DecisionExplorerDtoSupport.copyOrEmpty(degradedEvidenceSignals);
        unknownEvidenceSignals = DecisionExplorerDtoSupport.copyOrEmpty(unknownEvidenceSignals);
        presentEvidenceCount = presentEvidenceSignals.size();
        partialEvidenceCount = partialEvidenceSignals.size();
        missingEvidenceCount = missingEvidenceSignals.size();
        degradedEvidenceCount = degradedEvidenceSignals.size();
        unknownEvidenceCount = unknownEvidenceSignals.size();
        readinessReasons = DecisionExplorerDtoSupport.copyOrEmpty(readinessReasons);
        fingerprintAlgorithm = DecisionExplorerDtoSupport.valueOrDefault(fingerprintAlgorithm,
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM);
        diagnosticFingerprint = DecisionExplorerDtoSupport.valueOrUnknown(diagnosticFingerprint);
        reproducibilityKey = DecisionExplorerDtoSupport.valueOrUnknown(reproducibilityKey);
        fingerprintInputs = DecisionExplorerDtoSupport.copyOrEmpty(fingerprintInputs);
        sourceReferenceIds = DecisionExplorerDtoSupport.copyOrEmpty(sourceReferenceIds);
        boundaryNote = DecisionExplorerDtoSupport.valueOrUnknown(boundaryNote);
    }

    public static DecisionExplorerEvidenceSufficiencyV1 unknown(String boundaryNote) {
        return new DecisionExplorerEvidenceSufficiencyV1(
                true,
                true,
                DIAGNOSTIC_OBJECT,
                CONTRACT_VERSION,
                LEVEL_INSUFFICIENT,
                0,
                false,
                false,
                false,
                0,
                0,
                0,
                0,
                0,
                3,
                0,
                1,
                List.of(),
                List.of(),
                List.of(
                        "selected candidate evidence was unavailable",
                        "candidate tradeoff evidence was unavailable",
                        "score-comparable alternative evidence was unavailable"),
                List.of(),
                List.of("route tradeoff evidence sufficiency could not be computed"),
                List.of("EVIDENCE_SUFFICIENCY_INSUFFICIENT"),
                DecisionExplorerRouteTradeoffService.FINGERPRINT_ALGORITHM,
                "evidence-sufficiency|v1|level=INSUFFICIENT|score=0|candidates=0|alternatives=0|"
                        + "factorDeltas=0|present=0|partial=0|missing=3|degraded=0|unknown=1",
                "evidence-sufficiency:v1:INSUFFICIENT:0:0:0:0",
                List.of(
                        "diagnosticObject=" + DIAGNOSTIC_OBJECT,
                        "sufficiencyLevel=INSUFFICIENT",
                        "readinessScore=0",
                        "candidateEvidenceCount=0",
                        "comparableAlternativeCount=0",
                        "factorDeltaCount=0",
                        "missingEvidenceCount=3",
                        "unknownEvidenceCount=1"),
                List.of(),
                boundaryNote);
    }

    private static String normalizeLevel(String value) {
        String normalized = DecisionExplorerDtoSupport.valueOrUnknown(value);
        return switch (normalized) {
            case LEVEL_REPLAY_STYLE_READY,
                    LEVEL_TRADEOFF_READY,
                    LEVEL_BASIC_DIAGNOSTICS_ONLY,
                    LEVEL_INSUFFICIENT,
                    LEVEL_DEGRADED -> normalized;
            default -> LEVEL_INSUFFICIENT;
        };
    }
}
