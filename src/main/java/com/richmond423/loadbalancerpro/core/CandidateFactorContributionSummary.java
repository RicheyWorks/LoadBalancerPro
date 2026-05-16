package com.richmond423.loadbalancerpro.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public record CandidateFactorContributionSummary(
        String candidateId,
        boolean selected,
        List<String> knownVisibleSignals,
        List<String> unknownOrUnexposedSignals,
        List<ScoreFactorContribution> factorContributions,
        String selectedVsAlternativeExplanationNote,
        String exactnessBoundary,
        String labProofBoundary,
        String productionNotProvenBoundary) {

    private static final String EXACTNESS_BOUNDARY =
            "Factor contributions explain existing ServerScoreCalculator components for this candidate; "
                    + "hidden scoring and exact production scoring are not inferred.";
    private static final String LAB_PROOF_BOUNDARY =
            "Controlled lab evidence only; candidate summaries organize local state-vector signals for reviewer use.";
    private static final String PRODUCTION_NOT_PROVEN_BOUNDARY =
            "No production certification, production telemetry proof, production monitoring proof, "
                    + "SLA/SLO proof, live-cloud proof, real-tenant proof, registry publication proof, "
                    + "container signing proof, completed replay, or completed what-if proof.";

    public CandidateFactorContributionSummary {
        candidateId = requireNonBlank(candidateId, "candidateId");
        knownVisibleSignals = copyNonBlank(knownVisibleSignals, "knownVisibleSignals");
        unknownOrUnexposedSignals = copyNonBlank(unknownOrUnexposedSignals, "unknownOrUnexposedSignals");
        Objects.requireNonNull(factorContributions, "factorContributions cannot be null");
        factorContributions = List.copyOf(factorContributions);
        for (ScoreFactorContribution contribution : factorContributions) {
            Objects.requireNonNull(contribution, "factorContributions cannot contain null values");
        }
        selectedVsAlternativeExplanationNote = requireNonBlank(
                selectedVsAlternativeExplanationNote, "selectedVsAlternativeExplanationNote");
        exactnessBoundary = requireNonBlank(exactnessBoundary, "exactnessBoundary");
        labProofBoundary = requireNonBlank(labProofBoundary, "labProofBoundary");
        productionNotProvenBoundary = requireNonBlank(productionNotProvenBoundary, "productionNotProvenBoundary");
    }

    public static CandidateFactorContributionSummary fromCandidate(ServerStateVector candidate,
                                                                    boolean selected,
                                                                    ServerScoreCalculator calculator,
                                                                    String selectedVsAlternativeExplanationNote) {
        Objects.requireNonNull(candidate, "candidate cannot be null");
        Objects.requireNonNull(calculator, "calculator cannot be null");
        return new CandidateFactorContributionSummary(
                candidate.serverId(),
                selected,
                knownVisibleSignals(candidate),
                unknownOrUnexposedSignals(candidate),
                calculator.factorContributions(candidate),
                selectedVsAlternativeExplanationNote,
                EXACTNESS_BOUNDARY,
                LAB_PROOF_BOUNDARY,
                PRODUCTION_NOT_PROVEN_BOUNDARY);
    }

    public static List<CandidateFactorContributionSummary> fromCandidates(List<ServerStateVector> candidates,
                                                                          String selectedCandidateId,
                                                                          ServerScoreCalculator calculator,
                                                                          Map<String, String> explanationNotes) {
        Objects.requireNonNull(candidates, "candidates cannot be null");
        selectedCandidateId = requireNonBlank(selectedCandidateId, "selectedCandidateId");
        Objects.requireNonNull(calculator, "calculator cannot be null");
        Objects.requireNonNull(explanationNotes, "explanationNotes cannot be null");
        Map<String, String> copiedNotes = new LinkedHashMap<>(explanationNotes);
        List<CandidateFactorContributionSummary> summaries = new ArrayList<>();
        for (ServerStateVector candidate : candidates) {
            Objects.requireNonNull(candidate, "candidates cannot contain null values");
            String note = copiedNotes.getOrDefault(candidate.serverId(), defaultExplanationNote(candidate, selectedCandidateId));
            summaries.add(fromCandidate(candidate, candidate.serverId().equals(selectedCandidateId), calculator, note));
        }
        return List.copyOf(summaries);
    }

    public double exactContributionTotal() {
        return factorContributions.stream()
                .filter(ScoreFactorContribution::hasExactContributionValue)
                .mapToDouble(contribution -> contribution.contributionValue().orElseThrow())
                .sum();
    }

    public boolean hasUnknownOrUnexposedSignals() {
        return !unknownOrUnexposedSignals.isEmpty();
    }

    public List<String> factorNamesByExactness(ScoreFactorExactness exactness) {
        Objects.requireNonNull(exactness, "exactness cannot be null");
        return factorContributions.stream()
                .filter(contribution -> contribution.exactness() == exactness)
                .map(ScoreFactorContribution::factorName)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static List<String> knownVisibleSignals(ServerStateVector candidate) {
        List<String> signals = new ArrayList<>();
        signals.add("healthState=" + candidate.healthy());
        signals.add("inFlightRequestCount=" + candidate.inFlightRequestCount());
        addOptionalDoubleSignal(signals, "configuredCapacity", candidate.configuredCapacity());
        addOptionalDoubleSignal(signals, "estimatedConcurrencyLimit", candidate.estimatedConcurrencyLimit());
        signals.add("weight=" + format(candidate.weight()));
        signals.add("averageLatencyMillis=" + format(candidate.averageLatencyMillis()));
        signals.add("p95LatencyMillis=" + format(candidate.p95LatencyMillis()));
        signals.add("p99LatencyMillis=" + format(candidate.p99LatencyMillis()));
        signals.add("recentErrorRate=" + format(candidate.recentErrorRate()));
        addOptionalIntSignal(signals, "queueDepth", candidate.queueDepth());
        NetworkAwarenessSignal network = candidate.networkAwarenessSignal();
        signals.add("timeoutRate=" + format(network.timeoutRate()));
        signals.add("retryRate=" + format(network.retryRate()));
        signals.add("connectionFailureRate=" + format(network.connectionFailureRate()));
        signals.add("latencyJitterMillis=" + format(network.latencyJitterMillis()));
        signals.add("recentErrorBurst=" + network.recentErrorBurst());
        signals.add("requestTimeoutCount=" + network.requestTimeoutCount());
        signals.add("networkSampleSize=" + network.sampleSize());
        return List.copyOf(signals);
    }

    private static List<String> unknownOrUnexposedSignals(ServerStateVector candidate) {
        List<String> signals = new ArrayList<>();
        if (candidate.configuredCapacity().isEmpty()) {
            signals.add("configuredCapacity not exposed");
        }
        if (candidate.estimatedConcurrencyLimit().isEmpty()) {
            signals.add("estimatedConcurrencyLimit not exposed");
        }
        if (candidate.queueDepth().isEmpty()) {
            signals.add("queueDepth not exposed");
        }
        signals.add("hidden routing internals not exposed");
        signals.add("exact production scoring not exposed");
        signals.add("production telemetry not exposed");
        return List.copyOf(signals);
    }

    private static String defaultExplanationNote(ServerStateVector candidate, String selectedCandidateId) {
        if (candidate.serverId().equals(selectedCandidateId)) {
            return "Selected candidate; factor contributions describe current calculator inputs without changing "
                    + "routing behavior.";
        }
        return "Non-selected candidate; compare visible factor contributions and keep unknown signals explicit.";
    }

    private static void addOptionalDoubleSignal(List<String> signals, String name, OptionalDouble value) {
        value.ifPresent(present -> signals.add(name + "=" + format(present)));
    }

    private static void addOptionalIntSignal(List<String> signals, String name, OptionalInt value) {
        value.ifPresent(present -> signals.add(name + "=" + present));
    }

    private static List<String> copyNonBlank(List<String> values, String fieldName) {
        Objects.requireNonNull(values, fieldName + " cannot be null");
        List<String> copied = new ArrayList<>();
        for (String value : values) {
            copied.add(requireNonBlank(value, fieldName + " entry"));
        }
        return List.copyOf(copied);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value.trim();
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.6f", value);
    }
}
