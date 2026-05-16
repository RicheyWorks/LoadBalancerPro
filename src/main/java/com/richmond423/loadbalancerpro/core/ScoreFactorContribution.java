package com.richmond423.loadbalancerpro.core;

import java.util.Objects;
import java.util.OptionalDouble;

public record ScoreFactorContribution(
        String factorName,
        String rawValueDescription,
        String weightDescription,
        ScoreFactorDirection direction,
        String contributionDescription,
        OptionalDouble contributionValue,
        ScoreFactorExactness exactness,
        String explanationText,
        String boundaryNote) {

    public ScoreFactorContribution {
        factorName = requireNonBlank(factorName, "factorName");
        rawValueDescription = requireNonBlank(rawValueDescription, "rawValueDescription");
        weightDescription = requireNonBlank(weightDescription, "weightDescription");
        Objects.requireNonNull(direction, "direction cannot be null");
        contributionDescription = requireNonBlank(contributionDescription, "contributionDescription");
        Objects.requireNonNull(contributionValue, "contributionValue cannot be null");
        contributionValue.ifPresent(value -> {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("contributionValue must be finite when present");
            }
        });
        Objects.requireNonNull(exactness, "exactness cannot be null");
        explanationText = requireNonBlank(explanationText, "explanationText");
        boundaryNote = requireNonBlank(boundaryNote, "boundaryNote");
    }

    public boolean hasExactContributionValue() {
        return contributionValue.isPresent() && exactness == ScoreFactorExactness.EXACT_FROM_CALCULATOR;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }
}
