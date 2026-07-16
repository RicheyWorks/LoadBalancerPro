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
        String boundaryNote,
        OptionalDouble rawValue,
        OptionalDouble normalizedValue,
        OptionalDouble weight) {

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
        Objects.requireNonNull(rawValue, "rawValue cannot be null");
        Objects.requireNonNull(normalizedValue, "normalizedValue cannot be null");
        Objects.requireNonNull(weight, "weight cannot be null");
        rawValue.ifPresent(value -> requireFinite(value, "rawValue"));
        normalizedValue.ifPresent(value -> requireUnitInterval(value, "normalizedValue"));
        weight.ifPresent(value -> requireNonNegative(value, "weight"));
        if (normalizedValue.isPresent() && rawValue.isEmpty()) {
            throw new IllegalArgumentException("normalizedValue requires rawValue");
        }
        if (weight.isPresent() && rawValue.isEmpty()) {
            throw new IllegalArgumentException("weight requires rawValue");
        }
        if (normalizedValue.isPresent() && weight.isPresent() && contributionValue.isPresent()) {
            double expectedContribution = normalizedValue.getAsDouble() * weight.getAsDouble();
            double tolerance = Math.max(1.0, Math.abs(expectedContribution)) * 0.000000001;
            if (Math.abs(contributionValue.getAsDouble() - expectedContribution) > tolerance) {
                throw new IllegalArgumentException(
                        "contributionValue must equal normalizedValue multiplied by weight");
            }
        }
    }

    public ScoreFactorContribution(
            String factorName,
            String rawValueDescription,
            String weightDescription,
            ScoreFactorDirection direction,
            String contributionDescription,
            OptionalDouble contributionValue,
            ScoreFactorExactness exactness,
            String explanationText,
            String boundaryNote) {
        this(
                factorName,
                rawValueDescription,
                weightDescription,
                direction,
                contributionDescription,
                contributionValue,
                exactness,
                explanationText,
                boundaryNote,
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty());
    }

    public boolean hasExactContributionValue() {
        return contributionValue.isPresent() && exactness == ScoreFactorExactness.EXACT_FROM_CALCULATOR;
    }

    public boolean hasWeightedNormalizedValue() {
        return rawValue.isPresent() && normalizedValue.isPresent() && weight.isPresent();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static void requireFinite(double value, String fieldName) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be finite when present");
        }
    }

    private static void requireNonNegative(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative when present");
        }
    }

    private static void requireUnitInterval(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0 when present");
        }
    }
}
