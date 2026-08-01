package com.richmond423.loadbalancerpro.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.richmond423.loadbalancerpro.core.RoutingDecisionExplanation;
import com.richmond423.loadbalancerpro.core.ServerStateVector;
import org.junit.jupiter.api.Test;

class LiveRoutingDecisionStoreTest {
    private static final Instant NOW = Instant.parse("2026-07-31T12:00:00Z");

    @Test
    void boundedStoreEvictsOldestAndReportsProcessLocalCounters() {
        LiveRoutingDecisionStore store = new LiveRoutingDecisionStore(
                2, Clock.fixed(NOW, ZoneOffset.UTC));

        store.record(7, "api", "ROUND_ROBIN", 1, "strategy", "a",
                List.of(candidate("a", 1)), explanation("ROUND_ROBIN", "a"),
                200, 2.5, false, "upstream_response");
        store.record(7, "api", "ROUND_ROBIN", 1, "strategy", "b",
                List.of(candidate("b", 2)), explanation("ROUND_ROBIN", "b"),
                503, 3.5, true, "upstream_response");
        store.record(7, "api", "ROUND_ROBIN", 2, "affinity", "c",
                List.of(candidate("c", 3)), null,
                502, 4.5, false, "upstream_failure");

        RecentProxyDecisionsResponse snapshot = store.snapshot(true);
        assertEquals(true, snapshot.proxyEnabled());
        assertEquals("process-local", snapshot.retentionScope());
        assertEquals(2, snapshot.maxRetained());
        assertEquals(2, snapshot.retainedCount());
        assertEquals(3, snapshot.totalCaptured());
        assertEquals(1, snapshot.totalDropped());
        assertEquals(List.of("proxy-decision-00000002", "proxy-decision-00000003"),
                snapshot.decisions().stream().map(LiveRoutingDecisionRecord::decisionId).toList());
        assertEquals(2, snapshot.decisions().get(0).candidates().get(0).inFlightRequestCount());
        assertEquals("affinity", snapshot.decisions().get(1).selectionSource());
        assertEquals("NOT_APPLICABLE", snapshot.decisions().get(1).selectionEvidence().status());
        assertEquals(Optional.empty(), store.find("proxy-decision-00000001"));
        assertEquals("proxy-decision-00000002",
                store.find("proxy-decision-00000002").orElseThrow().decisionId());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.decisions().add(snapshot.decisions().get(0)));
    }

    @Test
    void recordCopiesCandidateInputBeforeReturningSnapshot() {
        LiveRoutingDecisionStore store = new LiveRoutingDecisionStore(
                1, Clock.fixed(NOW, ZoneOffset.UTC));
        List<ServerStateVector> candidates = new ArrayList<>();
        candidates.add(candidate("safe", 4));

        store.record(1, "default", "ROUND_ROBIN", 1, "strategy", "safe",
                candidates, explanation("ROUND_ROBIN", "safe"),
                201, 1.0, false, "upstream_response");
        candidates.clear();

        assertEquals(List.of("safe"), store.snapshot(true).decisions().get(0).candidates().stream()
                .map(LiveRoutingDecisionRecord.CandidateState::upstreamId)
                .toList());
    }

    @Test
    void capturesTheDeclaredScorePreferenceForEveryProxyStrategy() {
        Map<String, String> expectedPreferences = Map.of(
                "TAIL_LATENCY_POWER_OF_TWO", "LOWER_WINS",
                "WEIGHTED_LEAST_LOAD", "LOWER_WINS",
                "WEIGHTED_LEAST_CONNECTIONS", "LOWER_WINS",
                "WEIGHTED_ROUND_ROBIN", "HIGHER_WINS",
                "ROUND_ROBIN", "POSITIONAL",
                "CONSISTENT_HASH", "KEYED_RING");

        expectedPreferences.forEach((strategy, expectedPreference) -> {
            LiveRoutingDecisionRecord.SelectionEvidence evidence =
                    LiveRoutingDecisionRecord.SelectionEvidence.capture(
                            strategy,
                            "strategy",
                            "safe",
                            List.of(LiveRoutingDecisionRecord.CandidateState.from(candidate("safe", 1))),
                            explanation(strategy, "safe"));

            assertEquals("CAPTURED", evidence.status());
            assertEquals(expectedPreference, evidence.scorePreference());
        });
    }

    @Test
    void storeRejectsAnUnboundedZeroCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> new LiveRoutingDecisionStore(0, Clock.systemUTC()));
    }

    private static ServerStateVector candidate(String id, int inFlight) {
        return new ServerStateVector(
                id,
                true,
                inFlight,
                OptionalDouble.of(100.0),
                OptionalDouble.of(50.0),
                0.75,
                10.0,
                20.0,
                30.0,
                0.1,
                OptionalInt.of(inFlight),
                NOW);
    }

    private static RoutingDecisionExplanation explanation(String strategy, String chosen) {
        return new RoutingDecisionExplanation(
                strategy,
                List.of(chosen),
                Optional.of(chosen),
                Map.of(),
                "fixture selection",
                NOW);
    }
}
