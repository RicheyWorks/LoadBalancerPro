package com.richmond423.loadbalancerpro.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

class ServerObservationScoreIntegrationTest {
    private static final Instant BASE_TIME = Instant.parse("2026-07-16T15:00:00Z");
    private static final ServerObservationWindowPolicy POLICY = new ServerObservationWindowPolicy(
            8,
            Duration.ofSeconds(30),
            4,
            4,
            0.20,
            0.75,
            3,
            2,
            0.50);

    @Test
    void rollingStateFeedsExistingExplainableScorePath() {
        Server stableServer = server("stable");
        Server degradedServer = server("degraded");
        ServerRollingSignalState stable = fourSuccesses("stable", 50).snapshot(BASE_TIME.plusSeconds(5));
        ServerRollingSignalState degraded = ServerObservationWindow.create("degraded", POLICY)
                .append(success("degraded-1", "degraded", 1, 50), BASE_TIME.plusSeconds(5))
                .append(timeout("degraded-2", "degraded", 2, 500), BASE_TIME.plusSeconds(5))
                .append(success("degraded-3", "degraded", 3, 50), BASE_TIME.plusSeconds(5))
                .append(success("degraded-4", "degraded", 4, 50), BASE_TIME.plusSeconds(5))
                .snapshot(BASE_TIME.plusSeconds(5));

        ServerStateVector stableVector = vector(stableServer, stable);
        ServerStateVector degradedVector = vector(degradedServer, degraded);
        ServerScoreCalculator calculator = new ServerScoreCalculator();

        assertTrue(stableVector.healthy());
        assertTrue(degradedVector.healthy());
        assertTrue(degradedVector.latencyWindowSignal().hasTailLatencyWindowValues());
        assertTrue(degradedVector.recentErrorRate() > stableVector.recentErrorRate());
        assertTrue(calculator.score(degradedVector) > calculator.score(stableVector));
        assertTrue(calculator.scoreBreakdown(degradedVector).factorContributions().stream()
                .anyMatch(factor -> factor.factorName().equals("recentErrorRate")
                        && factor.contributionValue().orElse(0.0) > 0.0));
    }

    @Test
    void missingAndStaleEvidenceFailClosedBeforeRecommendationScoring() {
        Server server = server("server-a");
        ServerRollingSignalState missing = ServerObservationWindow.create("server-a", POLICY)
                .snapshot(BASE_TIME);
        ServerRollingSignalState stale = fourSuccesses("server-a", 25)
                .snapshot(BASE_TIME.plusSeconds(40));

        ServerStateVector missingVector = vector(server, missing);
        ServerStateVector staleVector = vector(server, stale);

        assertFalse(missing.recommendationEligible());
        assertFalse(stale.recommendationEligible());
        assertFalse(missingVector.healthy());
        assertFalse(staleVector.healthy());
        assertTrue(new ServerScoreCalculator().score(missingVector) >= 1_000_000.0);
        assertTrue(new ServerScoreCalculator().score(staleVector) >= 1_000_000.0);
    }

    private static ServerObservationWindow fourSuccesses(String serverId, double latencyMillis) {
        ServerObservationWindow window = ServerObservationWindow.create(serverId, POLICY);
        for (int index = 1; index <= 4; index++) {
            window = window.append(
                    success(serverId + "-" + index, serverId, index, latencyMillis),
                    BASE_TIME.plusSeconds(5));
        }
        return window;
    }

    private static ServerObservation success(
            String observationId,
            String serverId,
            long offsetSeconds,
            double latencyMillis) {
        return ServerObservation.success(
                observationId,
                serverId,
                ServerObservationSource.ENTERPRISE_LAB,
                latencyMillis,
                BASE_TIME.plusSeconds(offsetSeconds));
    }

    private static ServerObservation timeout(
            String observationId,
            String serverId,
            long offsetSeconds,
            double elapsedMillis) {
        return ServerObservation.timeout(
                observationId,
                serverId,
                ServerObservationSource.ENTERPRISE_LAB,
                elapsedMillis,
                BASE_TIME.plusSeconds(offsetSeconds));
    }

    private static ServerStateVector vector(Server server, ServerRollingSignalState state) {
        return ServerStateVector.fromObservationState(
                server,
                state,
                5,
                OptionalDouble.empty(),
                OptionalInt.of(1));
    }

    private static Server server(String serverId) {
        Server server = new Server(serverId, 10, 10, 10, ServerType.ONSITE);
        server.setCapacity(100);
        server.setHealthy(true);
        return server;
    }
}
