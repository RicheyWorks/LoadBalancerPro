package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;
import java.util.stream.Collectors;

class CandidateFactorContributionSummaryTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final ServerScoreCalculator calculator = new ServerScoreCalculator();

    @Test
    void candidateContributionSummariesAreDeterministicAndCarrySelectedState() {
        List<ServerStateVector> candidates = List.of(selectedCandidate(), alternativeCandidate());

        List<CandidateFactorContributionSummary> first = CandidateFactorContributionSummary.fromCandidates(
                candidates,
                "edge-alpha",
                calculator,
                Map.of(
                        "edge-alpha", "Selected candidate; visible score factors are lower.",
                        "edge-beta", "Non-selected candidate; visible latency and pressure are higher."));
        List<CandidateFactorContributionSummary> second = CandidateFactorContributionSummary.fromCandidates(
                candidates,
                "edge-alpha",
                calculator,
                Map.of(
                        "edge-alpha", "Selected candidate; visible score factors are lower.",
                        "edge-beta", "Non-selected candidate; visible latency and pressure are higher."));

        assertEquals(first, second);
        assertEquals(List.of("edge-alpha", "edge-beta"),
                first.stream().map(CandidateFactorContributionSummary::candidateId).toList());
        assertTrue(first.get(0).selected());
        assertFalse(first.get(1).selected());
        assertEquals(calculator.factorContributions(candidates.get(0)), first.get(0).factorContributions());
        assertEquals(calculator.factorContributions(candidates.get(1)), first.get(1).factorContributions());
    }

    @Test
    void candidateSummaryPreservesKnownAndUnknownSignals() {
        CandidateFactorContributionSummary selected = CandidateFactorContributionSummary.fromCandidate(
                selectedCandidate(), true, calculator, "Selected from visible local lab signals.");
        CandidateFactorContributionSummary missing = CandidateFactorContributionSummary.fromCandidate(
                missingSignalsCandidate(), false, calculator, "Non-selected candidate has unavailable signals.");

        String selectedKnown = String.join(" ", selected.knownVisibleSignals());
        assertTrue(selectedKnown.contains("healthState=true"));
        assertTrue(selectedKnown.contains("configuredCapacity=200.000000"));
        assertTrue(selectedKnown.contains("estimatedConcurrencyLimit=100.000000"));
        assertTrue(selectedKnown.contains("queueDepth=5"));
        assertTrue(selectedKnown.contains("timeoutRate=0.100000"));
        assertTrue(selected.hasUnknownOrUnexposedSignals());
        assertTrue(selected.unknownOrUnexposedSignals().contains("hidden routing internals not exposed"));
        assertFalse(selected.unknownOrUnexposedSignals().contains("configuredCapacity not exposed"));

        assertTrue(missing.knownVisibleSignals().contains("healthState=true"));
        assertTrue(missing.unknownOrUnexposedSignals().contains("configuredCapacity not exposed"));
        assertTrue(missing.unknownOrUnexposedSignals().contains("estimatedConcurrencyLimit not exposed"));
        assertTrue(missing.unknownOrUnexposedSignals().contains("queueDepth not exposed"));
    }

    @Test
    void contributionExactnessAndScoreBoundariesArePreserved() {
        ServerStateVector candidate = selectedCandidate();
        CandidateFactorContributionSummary summary = CandidateFactorContributionSummary.fromCandidate(
                candidate, true, calculator, "Selected candidate contribution summary.");

        assertEquals(calculator.score(candidate), summary.exactContributionTotal(), 0.000001);
        assertTrue(summary.factorNamesByExactness(ScoreFactorExactness.EXACT_FROM_CALCULATOR)
                .containsAll(List.of("p95LatencyMillis", "recentErrorRate", "healthPenalty")));
        assertEquals(List.of("hiddenRoutingInternals"),
                summary.factorNamesByExactness(ScoreFactorExactness.NOT_EXPOSED));
        assertTrue(summary.exactnessBoundary().contains("hidden scoring"));
        assertTrue(summary.exactnessBoundary().contains("exact production scoring"));
        assertTrue(summary.labProofBoundary().contains("Controlled lab evidence only"));
        assertTrue(summary.productionNotProvenBoundary().contains("No production certification"));
        assertTrue(summary.productionNotProvenBoundary().contains("completed replay"));
        assertTrue(summary.productionNotProvenBoundary().contains("completed what-if"));
    }

    @Test
    void selectedBackendOutcomeAndScoresRemainUnchangedForRepresentativeFixture() {
        ServerStateVector selected = selectedCandidate();
        ServerStateVector alternative = alternativeCandidate();
        TailLatencyPowerOfTwoStrategy strategy = new TailLatencyPowerOfTwoStrategy(
                calculator,
                new Random(7),
                Clock.fixed(NOW, ZoneOffset.UTC));

        RoutingDecision decision = strategy.choose(List.of(selected, alternative));
        List<CandidateFactorContributionSummary> summaries = CandidateFactorContributionSummary.fromCandidates(
                List.of(selected, alternative),
                decision.explanation().chosenServerId().orElseThrow(),
                calculator,
                Map.of());

        assertEquals("edge-alpha", decision.chosenServer().orElseThrow().serverId());
        assertEquals("edge-alpha", decision.explanation().chosenServerId().orElseThrow());
        assertEquals(calculator.score(selected), summaries.get(0).exactContributionTotal(), 0.000001);
        assertEquals(calculator.score(alternative), summaries.get(1).exactContributionTotal(), 0.000001);
        assertTrue(calculator.score(selected) < calculator.score(alternative));
        assertEquals(
                Map.of("edge-alpha", calculator.score(selected), "edge-beta", calculator.score(alternative)),
                decision.explanation().scores());
    }

    @Test
    void contributionTextDoesNotInventHiddenScoringOrProductionProof() {
        String normalized = CandidateFactorContributionSummary.fromCandidate(
                        selectedCandidate(), true, calculator, "Selected candidate contribution summary.")
                .factorContributions().stream()
                .map(contribution -> String.join(" ",
                        contribution.factorName(),
                        contribution.rawValueDescription(),
                        contribution.weightDescription(),
                        contribution.explanationText(),
                        contribution.boundaryNote()))
                .collect(Collectors.joining(" "))
                .toLowerCase();
        String summaryText = CandidateFactorContributionSummary.fromCandidate(
                selectedCandidate(), true, calculator, "Selected candidate contribution summary.").toString()
                .toLowerCase();

        assertTrue(normalized.contains("hidden routing internals are not inferred"));
        assertTrue(summaryText.contains("production telemetry proof"));
        assertTrue(summaryText.contains("completed replay"));
        assertFalse(summaryText.contains("hidden scoring is available"));
        assertFalse(summaryText.contains("hidden scoring is inferred"));
        assertFalse(summaryText.contains("production certification is proven"));
        assertFalse(summaryText.contains("exact production scoring proof is available"));
    }

    private ServerStateVector selectedCandidate() {
        return new ServerStateVector(
                "edge-alpha",
                true,
                20,
                OptionalDouble.of(200.0),
                OptionalDouble.of(100.0),
                5.0,
                30.0,
                60.0,
                90.0,
                0.02,
                OptionalInt.of(5),
                networkSignal(),
                NOW);
    }

    private ServerStateVector alternativeCandidate() {
        return new ServerStateVector(
                "edge-beta",
                true,
                40,
                OptionalDouble.of(200.0),
                OptionalDouble.of(100.0),
                3.0,
                45.0,
                90.0,
                130.0,
                0.04,
                OptionalInt.of(9),
                new NetworkAwarenessSignal("edge-beta", 0.20, 0.30, 0.10, 50.0, true, 5, 100, NOW),
                NOW);
    }

    private ServerStateVector missingSignalsCandidate() {
        return new ServerStateVector(
                "edge-missing",
                true,
                2,
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                1.0,
                10.0,
                20.0,
                30.0,
                0.0,
                OptionalInt.empty(),
                NetworkAwarenessSignal.neutral("edge-missing", NOW),
                NOW);
    }

    private NetworkAwarenessSignal networkSignal() {
        return new NetworkAwarenessSignal("edge-alpha", 0.10, 0.20, 0.05, 40.0, true, 3, 100, NOW);
    }
}
