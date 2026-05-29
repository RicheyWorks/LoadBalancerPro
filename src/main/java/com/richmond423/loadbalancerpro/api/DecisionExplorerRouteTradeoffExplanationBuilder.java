package com.richmond423.loadbalancerpro.api;

import static com.richmond423.loadbalancerpro.api.DecisionExplorerDiagnosticListSupport.copyNonNull;

import java.util.List;

final class DecisionExplorerRouteTradeoffExplanationBuilder {
    String build(
            DecisionExplorerConfidenceSummaryV1 confidenceSummary,
            DecisionExplorerRouteTradeoffRowV1 closestAlternative,
            String tradeoffCategory,
            DecisionExplorerEvidenceSufficiencyV1 evidenceSufficiency,
            DecisionExplorerReplayReadinessDiagnosticV1 replayReadinessDiagnostic,
            List<String> tradeoffReasons,
            List<String> warnings,
            List<String> unknowns,
            String reproducibilityKey) {
        String alternativeText = closestAlternative == null
                ? "no score-comparable alternative was returned"
                : "closest alternative " + closestAlternative.candidateId() + " has score delta "
                        + DecisionExplorerDtoSupport.valueOrUnknown(
                                closestAlternative.scoreDeltaFromSelected() == null
                                        ? null
                                        : closestAlternative.scoreDeltaFromSelected().toString());
        String primaryReason = firstOrUnknown(tradeoffReasons);
        return "Route tradeoff explanation: selected candidate "
                + DecisionExplorerDtoSupport.valueOrUnknown(
                        confidenceSummary == null ? null : confidenceSummary.selectedCandidateId())
                + " is " + DecisionExplorerDtoSupport.valueOrUnknown(
                        confidenceSummary == null ? null : confidenceSummary.status())
                + " with category " + DecisionExplorerDtoSupport.valueOrUnknown(tradeoffCategory)
                + "; " + alternativeText
                + "; evidence sufficiency " + DecisionExplorerDtoSupport.valueOrUnknown(
                        evidenceSufficiency == null ? null : evidenceSufficiency.sufficiencyLevel())
                + " with readiness score " + (evidenceSufficiency == null ? 0 : evidenceSufficiency.readinessScore())
                + "; replay readiness " + DecisionExplorerDtoSupport.valueOrUnknown(
                        replayReadinessDiagnostic == null ? null : replayReadinessDiagnostic.readinessStatus())
                + " with replay execution "
                + (replayReadinessDiagnostic != null && replayReadinessDiagnostic.replayExecutionAvailable()
                        ? "available"
                        : "unavailable")
                + "; primary reason " + primaryReason
                + "; warnings " + copyNonNull(warnings).size()
                + "; unknowns " + copyNonNull(unknowns).size()
                + "; reproducibility key "
                + DecisionExplorerDtoSupport.valueOrUnknown(reproducibilityKey) + ".";
    }

    private static String firstOrUnknown(List<String> values) {
        return copyNonNull(values).stream()
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("UNKNOWN");
    }

}
