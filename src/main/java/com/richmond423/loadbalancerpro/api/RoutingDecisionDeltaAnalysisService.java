package com.richmond423.loadbalancerpro.api;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public final class RoutingDecisionDeltaAnalysisService {
    private static final String SOURCE =
            "/api/routing/compare results[].scores and results[].decisionVector candidate factorContributions";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Decision delta analysis is read-only lab explainability derived only from returned score values "
                    + "and ScoreFactorContributionResponse data; it does not change routing behavior, scoring math, "
                    + "strategy weights, server selection logic, or proxy behavior.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "No production certification, live-cloud proof, real-tenant proof, SLA/SLO proof, registry "
                    + "publication proof, container signing proof, governance application proof, production traffic "
                    + "validation, or exact production scoring proof is claimed.";
    private static final Comparator<ScoreFactorDeltaResponse> BY_DELTA_THEN_NAME = Comparator
            .comparingDouble(ScoreFactorDeltaResponse::absoluteDelta)
            .reversed()
            .thenComparing(ScoreFactorDeltaResponse::factorName);

    public RoutingDecisionDeltaAnalysisResponse analyze(
            RoutingDecisionVectorResponse decisionVector, Map<String, Double> scores) {
        if (decisionVector == null || decisionVector.candidateSummaries().isEmpty()) {
            return unknownAnalysis(
                    "Decision delta analysis is unavailable because no Decision Vector candidate data was returned.");
        }
        if (scores == null || scores.isEmpty()) {
            return unknownAnalysis(
                    "Decision delta analysis is unavailable because final score data was not returned.");
        }

        CandidateDecisionVectorResponse selected = selectedCandidate(decisionVector).orElse(null);
        if (selected == null || isBlank(selected.candidateId())) {
            return unknownAnalysis(
                    "Decision delta analysis is unavailable because no selected candidate vector was returned.");
        }
        Double selectedScore = finiteScore(scores, selected.candidateId()).orElse(null);
        if (selectedScore == null) {
            return unknownAnalysis(
                    "Decision delta analysis is unavailable because the selected candidate final score was not returned "
                            + "as a finite value.");
        }

        CandidateDecisionVectorResponse alternative = closestAlternative(decisionVector, scores, selectedScore)
                .orElse(null);
        if (alternative == null) {
            return unknownAnalysis(
                    "Decision delta analysis is unavailable because no non-selected candidate had a finite final score.");
        }
        Double alternativeScore = finiteScore(scores, alternative.candidateId()).orElseThrow();

        ContributionMaps contributionMaps = contributionMaps(selected, alternative);
        List<ScoreFactorDeltaResponse> factorDeltas = contributionMaps.comparableFactorNames().stream()
                .map(factorName -> factorDelta(
                        factorName,
                        contributionMaps.selectedFinite().get(factorName),
                        contributionMaps.alternativeFinite().get(factorName)))
                .sorted(BY_DELTA_THEN_NAME)
                .toList();
        ScoreFactorDeltaResponse largestAbsoluteFactorDelta = factorDeltas.stream()
                .min(BY_DELTA_THEN_NAME)
                .orElse(null);
        List<String> comparedFactorNames = factorDeltas.stream()
                .map(ScoreFactorDeltaResponse::factorName)
                .sorted()
                .toList();
        List<String> omittedFactorNames = contributionMaps.omittedFactorNames();
        double finalScoreGap = selectedScore - alternativeScore;
        String status = factorDeltas.isEmpty() || !omittedFactorNames.isEmpty()
                ? STATUS_PARTIAL
                : STATUS_AVAILABLE;
        CandidateDecisionDeltaResponse comparison = new CandidateDecisionDeltaResponse(
                selected.candidateId(),
                alternative.candidateId(),
                selectedScore,
                alternativeScore,
                finalScoreGap,
                Math.abs(finalScoreGap),
                selected.factorContributions().size(),
                alternative.factorContributions().size(),
                factorDeltas.size(),
                comparedFactorNames,
                omittedFactorNames,
                comparisonExplanation(selected, alternative, selectedScore, alternativeScore, finalScoreGap,
                        factorDeltas.size(), omittedFactorNames.size()));
        String explanation = analysisExplanation(status, comparison, largestAbsoluteFactorDelta);

        return new RoutingDecisionDeltaAnalysisResponse(
                true,
                SOURCE,
                status,
                comparison,
                factorDeltas,
                largestAbsoluteFactorDelta,
                explanation,
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    public RoutingDecisionDeltaAnalysisResponse unknownAnalysis(String explanation) {
        return new RoutingDecisionDeltaAnalysisResponse(
                true,
                SOURCE,
                STATUS_UNKNOWN,
                null,
                List.of(),
                null,
                requireNonBlank(explanation, "explanation"),
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    private static Optional<CandidateDecisionVectorResponse> selectedCandidate(
            RoutingDecisionVectorResponse decisionVector) {
        if (decisionVector.selectedCandidateVector() != null) {
            return Optional.of(decisionVector.selectedCandidateVector());
        }
        return decisionVector.candidateSummaries().stream()
                .filter(CandidateDecisionVectorResponse::selected)
                .findFirst();
    }

    private static Optional<CandidateDecisionVectorResponse> closestAlternative(
            RoutingDecisionVectorResponse decisionVector, Map<String, Double> scores, double selectedScore) {
        return decisionVector.candidateSummaries().stream()
                .filter(candidate -> !candidate.selected())
                .filter(candidate -> finiteScore(scores, candidate.candidateId()).isPresent())
                .min(Comparator
                        .comparingDouble((CandidateDecisionVectorResponse candidate) ->
                                Math.abs(finiteScore(scores, candidate.candidateId()).orElseThrow()
                                        - selectedScore))
                        .thenComparing(candidate -> safeName(candidate.candidateId())));
    }

    private static Optional<Double> finiteScore(Map<String, Double> scores, String candidateId) {
        if (scores == null || isBlank(candidateId)) {
            return Optional.empty();
        }
        Double score = scores.get(candidateId);
        if (score == null || !Double.isFinite(score)) {
            return Optional.empty();
        }
        return Optional.of(score);
    }

    private static ContributionMaps contributionMaps(CandidateDecisionVectorResponse selected,
                                                     CandidateDecisionVectorResponse alternative) {
        Map<String, ScoreFactorContributionResponse> selectedFinite = finiteContributionByName(selected);
        Map<String, ScoreFactorContributionResponse> alternativeFinite = finiteContributionByName(alternative);
        Set<String> allNames = new TreeSet<>();
        allNames.addAll(sourceFactorNames(selected));
        allNames.addAll(sourceFactorNames(alternative));
        List<String> comparable = allNames.stream()
                .filter(name -> selectedFinite.containsKey(name) && alternativeFinite.containsKey(name))
                .toList();
        List<String> omitted = allNames.stream()
                .filter(name -> !selectedFinite.containsKey(name) || !alternativeFinite.containsKey(name))
                .toList();
        return new ContributionMaps(selectedFinite, alternativeFinite, comparable, omitted);
    }

    private static Map<String, ScoreFactorContributionResponse> finiteContributionByName(
            CandidateDecisionVectorResponse candidate) {
        Map<String, ScoreFactorContributionResponse> byName = new LinkedHashMap<>();
        for (ScoreFactorContributionResponse contribution : candidate.factorContributions()) {
            if (hasFiniteNamedContribution(contribution)) {
                byName.putIfAbsent(contribution.factorName(), contribution);
            }
        }
        return Map.copyOf(byName);
    }

    private static List<String> sourceFactorNames(CandidateDecisionVectorResponse candidate) {
        return candidate.factorContributions().stream()
                .filter(Objects::nonNull)
                .map(ScoreFactorContributionResponse::factorName)
                .filter(name -> !isBlank(name))
                .sorted()
                .toList();
    }

    private static ScoreFactorDeltaResponse factorDelta(String factorName,
                                                        ScoreFactorContributionResponse selected,
                                                        ScoreFactorContributionResponse alternative) {
        double contributionDelta = selected.contributionValue() - alternative.contributionValue();
        return new ScoreFactorDeltaResponse(
                factorName,
                selected.contributionValue(),
                alternative.contributionValue(),
                contributionDelta,
                Math.abs(contributionDelta),
                selected.direction(),
                alternative.direction(),
                "For factor " + factorName + ", contributionDelta is selected candidate contribution minus "
                        + "closest alternative contribution: " + format(selected.contributionValue())
                        + " - " + format(alternative.contributionValue()) + " = "
                        + format(contributionDelta) + ". This is a returned contribution difference only; "
                        + "it does not infer causality beyond the lab response data.");
    }

    private static String comparisonExplanation(CandidateDecisionVectorResponse selected,
                                                CandidateDecisionVectorResponse alternative,
                                                double selectedScore,
                                                double alternativeScore,
                                                double finalScoreGap,
                                                int comparedFactorCount,
                                                int omittedFactorCount) {
        return "Selected candidate " + selected.candidateId() + " is compared with closest scored alternative "
                + alternative.candidateId() + ". finalScoreGap is selected score minus alternative score: "
                + format(selectedScore) + " - " + format(alternativeScore) + " = " + format(finalScoreGap)
                + ". " + comparedFactorCount + " shared finite factor contribution(s) were compared; "
                + omittedFactorCount + " factor name(s) were omitted because one side did not return a finite "
                + "contribution. Score sign semantics follow the existing score output and are not reinterpreted.";
    }

    private static String analysisExplanation(String status,
                                              CandidateDecisionDeltaResponse comparison,
                                              ScoreFactorDeltaResponse largestDelta) {
        if (largestDelta == null) {
            return "Decision delta analysis is " + status
                    + ": selected and closest alternative scores were available, but no shared finite factor "
                    + "contribution deltas were returned, so factor-level differences remain unknown.";
        }
        return "Decision delta analysis is " + status + ": selected candidate "
                + comparison.selectedCandidateId() + " and closest alternative "
                + comparison.closestAlternativeCandidateId()
                + " are separated by finalScoreGap=" + format(comparison.finalScoreGap())
                + "; largest absolute factor delta is " + largestDelta.factorName()
                + " with absoluteDelta=" + format(largestDelta.absoluteDelta())
                + ". The comparison is derived only from returned lab score and contribution data.";
    }

    private static boolean hasFiniteNamedContribution(ScoreFactorContributionResponse contribution) {
        return contribution != null
                && !isBlank(contribution.factorName())
                && contribution.contributionValue() != null
                && Double.isFinite(contribution.contributionValue());
    }

    private static String safeName(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String format(Double value) {
        if (value == null) {
            return "not returned";
        }
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private record ContributionMaps(
            Map<String, ScoreFactorContributionResponse> selectedFinite,
            Map<String, ScoreFactorContributionResponse> alternativeFinite,
            List<String> comparableFactorNames,
            List<String> omittedFactorNames) {
    }
}
