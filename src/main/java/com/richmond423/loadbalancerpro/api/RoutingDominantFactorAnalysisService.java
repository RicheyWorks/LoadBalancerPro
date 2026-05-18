package com.richmond423.loadbalancerpro.api;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class RoutingDominantFactorAnalysisService {
    private static final String SOURCE =
            "/api/routing/compare results[].decisionVector candidate factorContributions";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String BOUNDARY_NOTE =
            "Dominant factor analysis is read-only lab explainability derived only from returned "
                    + "ScoreFactorContributionResponse data; it does not change routing behavior, scoring math, "
                    + "strategy weights, server selection logic, or proxy behavior.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "No production certification, live-cloud proof, real-tenant proof, SLA/SLO proof, registry "
                    + "publication proof, container signing proof, governance application proof, or exact "
                    + "production scoring proof is claimed.";
    private static final Comparator<ScoreFactorContributionResponse> BY_IMPACT_THEN_NAME = Comparator
            .comparingDouble(RoutingDominantFactorAnalysisService::absoluteImpact)
            .reversed()
            .thenComparing(RoutingDominantFactorAnalysisService::factorName);

    public DominantFactorAnalysisResponse analyze(RoutingDecisionVectorResponse decisionVector) {
        if (decisionVector == null || decisionVector.candidateSummaries().isEmpty()) {
            return unknownAnalysis(
                    "Dominant factor analysis is unavailable because no Decision Vector candidate contributions "
                            + "were returned.");
        }

        List<CandidateDominantFactorResponse> candidateAnalyses = decisionVector.candidateSummaries().stream()
                .map(this::analyzeCandidate)
                .toList();
        CandidateDominantFactorResponse selected = candidateAnalyses.stream()
                .filter(CandidateDominantFactorResponse::selected)
                .findFirst()
                .orElse(null);
        boolean anyAvailable = candidateAnalyses.stream()
                .anyMatch(CandidateDominantFactorResponse::available);
        String selectedExplanation = selected == null
                ? "No selected candidate contribution data was returned, so selected-decision dominant factors are unknown."
                : "Selected-decision dominant factor analysis uses only contribution data returned for "
                        + selected.candidateId() + ". " + selected.explanation();

        return new DominantFactorAnalysisResponse(
                true,
                SOURCE,
                anyAvailable ? STATUS_AVAILABLE : STATUS_UNKNOWN,
                candidateAnalyses,
                selected,
                selectedExplanation,
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    public DominantFactorAnalysisResponse unknownAnalysis(String explanation) {
        return new DominantFactorAnalysisResponse(
                true,
                SOURCE,
                STATUS_UNKNOWN,
                List.of(),
                null,
                requireNonBlank(explanation, "explanation"),
                BOUNDARY_NOTE,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    private CandidateDominantFactorResponse analyzeCandidate(CandidateDecisionVectorResponse candidate) {
        Objects.requireNonNull(candidate, "candidate cannot be null");
        List<ScoreFactorContributionResponse> contributions = candidate.factorContributions();
        List<ScoreFactorContributionResponse> exactContributions = contributions.stream()
                .filter(RoutingDominantFactorAnalysisService::hasFiniteContributionValue)
                .toList();
        DominantFactorResponse largestPositiveContributor = exactContributions.stream()
                .filter(RoutingDominantFactorAnalysisService::supportsSelection)
                .min(BY_IMPACT_THEN_NAME)
                .map(RoutingDominantFactorAnalysisService::dominantFactor)
                .orElse(null);
        DominantFactorResponse largestPenaltyContributor = exactContributions.stream()
                .filter(RoutingDominantFactorAnalysisService::weakensSelection)
                .min(BY_IMPACT_THEN_NAME)
                .map(RoutingDominantFactorAnalysisService::dominantFactor)
                .orElse(null);
        DominantFactorResponse largestAbsoluteImpact = exactContributions.stream()
                .min(BY_IMPACT_THEN_NAME)
                .map(RoutingDominantFactorAnalysisService::dominantFactor)
                .orElse(null);
        boolean available = !exactContributions.isEmpty();

        return new CandidateDominantFactorResponse(
                candidate.candidateId(),
                candidate.selected(),
                available,
                contributions.size(),
                sourceFactorNames(contributions),
                largestPositiveContributor,
                largestPenaltyContributor,
                largestAbsoluteImpact,
                explanation(candidate, available, largestPositiveContributor,
                        largestPenaltyContributor, largestAbsoluteImpact),
                BOUNDARY_NOTE);
    }

    private static List<String> sourceFactorNames(List<ScoreFactorContributionResponse> contributions) {
        return contributions.stream()
                .map(ScoreFactorContributionResponse::factorName)
                .filter(name -> name != null && !name.isBlank())
                .sorted()
                .toList();
    }

    private static String explanation(CandidateDecisionVectorResponse candidate,
                                      boolean available,
                                      DominantFactorResponse positive,
                                      DominantFactorResponse penalty,
                                      DominantFactorResponse absolute) {
        if (!available) {
            return "No factor contributions with exact numeric values were returned for " + candidate.candidateId()
                    + ", so dominant factors are unknown for this candidate.";
        }
        String positiveText = positive == null
                ? "no support-direction contribution was returned"
                : "largest support contributor is " + describe(positive);
        String penaltyText = penalty == null
                ? "no penalty/risk contribution was returned"
                : "largest penalty/risk contributor is " + describe(penalty);
        String absoluteText = absolute == null
                ? "largest absolute impact is unknown"
                : "largest absolute impact is " + describe(absolute);
        return "Candidate " + candidate.candidateId() + (candidate.selected() ? " is selected; " : " is not selected; ")
                + positiveText + "; " + penaltyText + "; " + absoluteText
                + ". This explanation is derived only from returned contribution data.";
    }

    private static DominantFactorResponse dominantFactor(ScoreFactorContributionResponse contribution) {
        return new DominantFactorResponse(
                contribution.factorName(),
                contribution.direction(),
                contribution.contributionValue(),
                absoluteImpact(contribution),
                contribution.contributionDescription(),
                contribution.explanationText(),
                contribution.boundaryNote());
    }

    private static boolean hasFiniteContributionValue(ScoreFactorContributionResponse contribution) {
        return contribution != null
                && contribution.contributionValue() != null
                && Double.isFinite(contribution.contributionValue());
    }

    private static boolean supportsSelection(ScoreFactorContributionResponse contribution) {
        return "SUPPORTS_SELECTION".equals(contribution.direction())
                || contribution.contributionValue() < 0.0;
    }

    private static boolean weakensSelection(ScoreFactorContributionResponse contribution) {
        return "WEAKENS_SELECTION".equals(contribution.direction())
                || contribution.contributionValue() > 0.0 && !"SUPPORTS_SELECTION".equals(contribution.direction());
    }

    private static double absoluteImpact(ScoreFactorContributionResponse contribution) {
        return Math.abs(contribution.contributionValue());
    }

    private static String factorName(ScoreFactorContributionResponse contribution) {
        return contribution.factorName() == null ? "" : contribution.factorName();
    }

    private static String describe(DominantFactorResponse factor) {
        return factor.factorName() + " (" + format(factor.contributionValue())
                + ", " + factor.direction() + ")";
    }

    private static String format(Double value) {
        if (value == null) {
            return "not exposed";
        }
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
