package com.richmond423.loadbalancerpro.api.proxy;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.richmond423.loadbalancerpro.core.RoutingDecisionExplanation;
import com.richmond423.loadbalancerpro.core.ServerStateVector;

/** Synchronized bounded ring for process-local live routing decision evidence. */
final class LiveRoutingDecisionStore {
    static final int DEFAULT_MAX_RETAINED = 100;

    private final int maxRetained;
    private final Clock clock;
    private final AtomicLong sequence = new AtomicLong();
    private final ArrayDeque<LiveRoutingDecisionRecord> decisions = new ArrayDeque<>();
    private long totalCaptured;
    private long totalDropped;

    LiveRoutingDecisionStore(Clock clock) {
        this(DEFAULT_MAX_RETAINED, clock);
    }

    LiveRoutingDecisionStore(int maxRetained, Clock clock) {
        if (maxRetained < 1) {
            throw new IllegalArgumentException("maxRetained must be positive");
        }
        this.maxRetained = maxRetained;
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    synchronized LiveRoutingDecisionRecord record(
            long configurationGeneration,
            String routeName,
            String strategy,
            int attempt,
            String selectionSource,
            String chosenUpstreamId,
            List<ServerStateVector> candidateStates,
            RoutingDecisionExplanation selectionExplanation,
            int responseStatus,
            double latencyMillis,
            boolean retriable,
            String outcome) {
        List<LiveRoutingDecisionRecord.CandidateState> candidates = Objects.requireNonNull(
                        candidateStates, "candidateStates cannot be null").stream()
                .map(LiveRoutingDecisionRecord.CandidateState::from)
                .toList();
        LiveRoutingDecisionRecord.SelectionEvidence selectionEvidence =
                LiveRoutingDecisionRecord.SelectionEvidence.capture(
                        strategy,
                        selectionSource,
                        chosenUpstreamId,
                        candidates,
                        selectionExplanation);
        LiveRoutingDecisionRecord record = new LiveRoutingDecisionRecord(
                "proxy-decision-%08d".formatted(sequence.incrementAndGet()),
                clock.instant(),
                configurationGeneration,
                routeName,
                strategy,
                attempt,
                selectionSource,
                chosenUpstreamId,
                candidates,
                selectionEvidence,
                responseStatus,
                latencyMillis,
                retriable,
                outcome);
        decisions.addLast(record);
        totalCaptured++;
        while (decisions.size() > maxRetained) {
            decisions.removeFirst();
            totalDropped++;
        }
        return record;
    }

    synchronized Optional<LiveRoutingDecisionRecord> find(String decisionId) {
        if (decisionId == null || decisionId.isBlank()) {
            return Optional.empty();
        }
        String expected = decisionId.trim();
        return decisions.stream()
                .filter(decision -> decision.decisionId().equals(expected))
                .findFirst();
    }

    synchronized RecentProxyDecisionsResponse snapshot(boolean proxyEnabled) {
        List<LiveRoutingDecisionRecord> retained = List.copyOf(new ArrayList<>(decisions));
        return new RecentProxyDecisionsResponse(
                proxyEnabled,
                RecentProxyDecisionsResponse.PROCESS_LOCAL,
                maxRetained,
                retained.size(),
                totalCaptured,
                totalDropped,
                retained);
    }
}
