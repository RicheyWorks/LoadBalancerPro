package com.richmond423.loadbalancerpro.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Collision-safe digest of the decision evidence returned by one routing strategy.
 *
 * <p>The digest intentionally excludes timestamps and prose. Candidate and factor counts are
 * encoded explicitly, and every string is length-prefixed by {@link CanonicalDigest}, so
 * delimiter-shaped identifiers cannot produce the same input stream.</p>
 */
public final class RoutingDecisionFingerprint {
    private static final String NAMESPACE = "routing-decision-fingerprint-v1";
    private static final String PREFIX = "sha256:v1:";

    private RoutingDecisionFingerprint() {
    }

    public static String from(RoutingDecisionExplanation explanation) {
        Objects.requireNonNull(explanation, "explanation cannot be null");
        List<String> consideredCandidates = explanation.candidateServersConsidered();
        Map<String, Double> scores = explanation.scores();
        Map<String, List<ScoreFactorContribution>> factorContributions =
                explanation.factorContributions();
        validateEvidenceKeys(consideredCandidates, scores, "score");
        validateEvidenceKeys(consideredCandidates, factorContributions, "factor contribution");

        CanonicalDigest digest = CanonicalDigest.sha256(NAMESPACE)
                .putString(explanation.strategyUsed())
                .putString(explanation.chosenServerId().orElse(null))
                .putInt(consideredCandidates.size());
        for (String candidateId : consideredCandidates) {
            digest.putString(candidateId);
            Double score = scores.get(candidateId);
            digest.putBoolean(score != null);
            if (score != null) {
                digest.putDouble(score);
            }
            List<ScoreFactorContribution> factors =
                    factorContributions.getOrDefault(candidateId, List.of());
            digest.putInt(factors.size());
            for (ScoreFactorContribution factor : factors) {
                Objects.requireNonNull(factor, "factor contributions cannot contain null values");
                digest.putString(factor.factorName())
                        .putString(factor.direction().name())
                        .putOptionalDouble(factor.contributionValue())
                        .putString(factor.exactness().name());
            }
        }
        return PREFIX + digest.hexDigest();
    }

    private static void validateEvidenceKeys(
            List<String> consideredCandidates, Map<String, ?> evidence, String evidenceName) {
        Set<String> considered = new HashSet<>(consideredCandidates);
        for (String candidateId : evidence.keySet()) {
            if (!considered.contains(candidateId)) {
                throw new IllegalArgumentException(
                        evidenceName + " references an unconsidered candidate: " + candidateId);
            }
        }
    }
}
