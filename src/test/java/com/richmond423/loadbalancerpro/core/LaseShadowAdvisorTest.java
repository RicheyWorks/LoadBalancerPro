package com.richmond423.loadbalancerpro.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LaseShadowAdvisorTest {
    private static final Instant NOW = Instant.parse("2026-04-30T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @AfterEach
    void clearFeatureFlag() {
        System.clearProperty(LaseShadowAdvisor.ENABLED_PROPERTY);
    }

    @Test
    void disabledAdvisorDoesNotEvaluateOrStoreReport() {
        AtomicInteger evaluations = new AtomicInteger();
        LaseShadowAdvisor advisor = new LaseShadowAdvisor(false, (input, config) -> {
            evaluations.incrementAndGet();
            throw new AssertionError("Disabled advisor must not evaluate LASE components.");
        }, CLOCK);

        Optional<LaseEvaluationReport> report = advisor.observe(
                "CAPACITY_AWARE", servers(), 40.0, new LoadDistributionResult(Map.of("S1", 20.0), 0.0));

        assertTrue(report.isEmpty());
        assertTrue(advisor.lastReport().isEmpty());
        assertEquals(0, evaluations.get());
    }

    @Test
    void enabledAdvisorProducesReportFromCurrentLoadBalancerState() {
        LaseShadowAdvisor advisor = deterministicAdvisor(true);

        Optional<LaseEvaluationReport> report = advisor.observe(
                "CAPACITY_AWARE", servers(), 60.0,
                new LoadDistributionResult(Map.of("S1", 25.0, "S2", 35.0), 0.0));

        assertTrue(report.isPresent());
        assertEquals("lase-shadow-capacity-aware", report.orElseThrow().evaluationId());
        assertNotNull(report.orElseThrow().routingDecision());
        assertNotNull(report.orElseThrow().concurrencyDecision());
        assertNotNull(report.orElseThrow().loadSheddingDecision());
        assertNotNull(report.orElseThrow().autoscalingRecommendation());
        assertNotNull(report.orElseThrow().failureScenarioResult());
        assertTrue(report.orElseThrow().summary().contains("Evaluation lase-shadow-capacity-aware"));
        assertEquals(report, advisor.lastReport());
    }

    @Test
    void enabledAdvisorRecordsSuccessfulShadowEvent() {
        LaseShadowEventLog eventLog = new LaseShadowEventLog(10);
        LaseShadowAdvisor advisor = deterministicAdvisor(true, eventLog);

        advisor.observe("CAPACITY_AWARE", servers(), 60.0,
                new LoadDistributionResult(Map.of("S1", 25.0, "S2", 35.0), 0.0));

        LaseShadowObservabilitySnapshot snapshot = eventLog.snapshot();

        assertEquals(1, snapshot.summary().totalEvaluations());
        assertEquals(0, snapshot.summary().failSafeCount());
        assertEquals(NOW, snapshot.summary().latestEventTimestamp());
        assertEquals(1, snapshot.recentEvents().size());
        LaseShadowEvent event = snapshot.recentEvents().get(0);
        assertEquals("lase-shadow-capacity-aware", event.evaluationId());
        assertEquals("CAPACITY_AWARE", event.strategy());
        assertEquals("S2", event.actualSelectedServerId());
        assertTrue(event.recommendedServerId() == null || !event.recommendedServerId().isBlank());
        assertTrue(event.decisionScore() == null || event.decisionScore() >= 0.0);
        assertEquals(0.0, event.networkAwarenessSignal().timeoutRate(), 0.0);
        assertEquals(0.0, event.networkAwarenessSignal().retryRate(), 0.0);
        assertEquals(0.0, event.networkAwarenessSignal().connectionFailureRate(), 0.0);
        assertEquals(0.0, event.networkAwarenessSignal().latencyJitterMillis(), 0.0);
        assertFalse(event.networkAwarenessSignal().recentErrorBurst());
        assertEquals(0, event.networkAwarenessSignal().requestTimeoutCount());
        assertEquals(0.0, event.networkRiskScore(), 0.0);
        if (event.agreedWithRouting() != null) {
            assertNotNull(event.actualSelectedServerId());
            assertNotNull(event.recommendedServerId());
        }
        assertFalse(event.failSafe());
        assertTrue(event.reason().contains("Evaluation lase-shadow-capacity-aware"));
    }

    @Test
    void advisorFailsSafelyWhenEvaluationThrows() {
        LaseShadowEventLog eventLog = new LaseShadowEventLog(10);
        LaseShadowAdvisor advisor = new LaseShadowAdvisor(true, (input, config) -> {
            throw new IllegalStateException("synthetic failure");
        }, CLOCK, eventLog);

        assertDoesNotThrow(() -> {
            Optional<LaseEvaluationReport> report = advisor.observe(
                    "CAPACITY_AWARE", servers(), 60.0,
                    new LoadDistributionResult(Map.of("S1", 25.0), 0.0));
            assertTrue(report.isEmpty());
        });
        assertTrue(advisor.lastReport().isEmpty());
        LaseShadowObservabilitySnapshot snapshot = eventLog.snapshot();
        assertEquals(1, snapshot.summary().totalEvaluations());
        assertEquals(1, snapshot.summary().failSafeCount());
        assertEquals("FAIL_SAFE", snapshot.recentEvents().get(0).recommendedAction());
        assertEquals(0.0, snapshot.recentEvents().get(0).networkAwarenessSignal().timeoutRate(), 0.0);
        assertEquals(0.0, snapshot.recentEvents().get(0).networkRiskScore(), 0.0);
        assertTrue(snapshot.recentEvents().get(0).failureReason().contains("synthetic failure"));
    }

    @Test
    void advisorRedactsSensitiveFailureReasonBeforeStoringShadowEvent() {
        LaseShadowEventLog eventLog = new LaseShadowEventLog(10);
        LaseShadowAdvisor advisor = new LaseShadowAdvisor(true, (input, config) -> {
            throw new IllegalStateException(
                    "token=raw-token api-key=raw-api-key Bearer raw-bearer-secret credential:raw-credential\nnext");
        }, CLOCK, eventLog);

        Optional<LaseEvaluationReport> report = advisor.observe(
                "CAPACITY_AWARE", servers(), 60.0,
                new LoadDistributionResult(Map.of("S1", 25.0), 0.0));

        assertTrue(report.isEmpty());
        String failureReason = eventLog.snapshot().recentEvents().get(0).failureReason();
        assertTrue(failureReason.contains("[redacted]"));
        assertFalse(failureReason.contains("raw-token"));
        assertFalse(failureReason.contains("raw-api-key"));
        assertFalse(failureReason.contains("raw-bearer-secret"));
        assertFalse(failureReason.contains("raw-credential"));
        assertFalse(failureReason.contains("\n"));
    }

    @Test
    void advisorUsesSafeFallbackForNullFailureMessage() {
        LaseShadowEventLog eventLog = new LaseShadowEventLog(10);
        LaseShadowAdvisor advisor = throwingAdvisor(new IllegalStateException((String) null), eventLog);

        Optional<LaseEvaluationReport> report = advisor.observe(
                "CAPACITY_AWARE", servers(), 60.0,
                new LoadDistributionResult(Map.of("S1", 25.0), 0.0));

        assertTrue(report.isEmpty());
        assertEquals("shadow evaluation failed safely",
                eventLog.snapshot().recentEvents().get(0).failureReason());
    }

    @Test
    void advisorUsesSafeFallbackForBlankFailureMessage() {
        LaseShadowEventLog eventLog = new LaseShadowEventLog(10);
        LaseShadowAdvisor advisor = throwingAdvisor(new IllegalStateException("   "), eventLog);

        Optional<LaseEvaluationReport> report = advisor.observe(
                "CAPACITY_AWARE", servers(), 60.0,
                new LoadDistributionResult(Map.of("S1", 25.0), 0.0));

        assertTrue(report.isEmpty());
        assertEquals("shadow evaluation failed safely",
                eventLog.snapshot().recentEvents().get(0).failureReason());
    }

    @Test
    void advisorPreservesUsefulContextAfterRedaction() {
        LaseShadowEventLog eventLog = new LaseShadowEventLog(10);
        LaseShadowAdvisor advisor = throwingAdvisor(new IllegalStateException(
                "advisor timeout for server api-1 token=raw-token while scoring CAPACITY_AWARE"), eventLog);

        advisor.observe("CAPACITY_AWARE", servers(), 60.0,
                new LoadDistributionResult(Map.of("S1", 25.0), 0.0));

        String failureReason = eventLog.snapshot().recentEvents().get(0).failureReason();
        assertTrue(failureReason.contains("advisor timeout"));
        assertTrue(failureReason.contains("server api-1"));
        assertTrue(failureReason.contains("CAPACITY_AWARE"));
        assertTrue(failureReason.contains("[redacted]"));
        assertFalse(failureReason.contains("raw-token"));
    }

    @Test
    void failureFollowedBySuccessDoesNotLeakStaleFailureReason() {
        LaseShadowEventLog eventLog = new LaseShadowEventLog(10);
        LaseEvaluationEngine engine = deterministicEngine();
        AtomicInteger calls = new AtomicInteger();
        LaseShadowAdvisor advisor = new LaseShadowAdvisor(true, (input, config) -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("first failure token=raw-token");
            }
            return engine.evaluate(input, config);
        }, CLOCK, eventLog);

        advisor.observe("CAPACITY_AWARE", servers(), 60.0,
                new LoadDistributionResult(Map.of("S1", 25.0), 0.0));
        advisor.observe("CAPACITY_AWARE", servers(), 60.0,
                new LoadDistributionResult(Map.of("S1", 25.0, "S2", 35.0), 0.0));

        LaseShadowObservabilitySnapshot snapshot = eventLog.snapshot();
        assertEquals(2, snapshot.recentEvents().size());
        assertTrue(snapshot.recentEvents().get(0).failSafe());
        assertNotNull(snapshot.recentEvents().get(0).failureReason());
        assertFalse(snapshot.recentEvents().get(1).failSafe());
        assertNull(snapshot.recentEvents().get(1).failureReason());
        assertEquals(1, snapshot.summary().failSafeCount());
    }

    @Test
    void advisorNeutralizesControlCharacterOnlyFailureMessages() {
        LaseShadowEventLog eventLog = new LaseShadowEventLog(10);
        LaseShadowAdvisor advisor = throwingAdvisor(new IllegalStateException("\r\n\t"), eventLog);

        advisor.observe("CAPACITY_AWARE", servers(), 60.0,
                new LoadDistributionResult(Map.of("S1", 25.0), 0.0));

        String failureReason = eventLog.snapshot().recentEvents().get(0).failureReason();
        assertEquals("shadow evaluation failed safely", failureReason);
        assertFalse(failureReason.contains("\r"));
        assertFalse(failureReason.contains("\n"));
        assertFalse(failureReason.contains("\t"));
    }

    @Test
    void loadBalancerOuterShadowObservationCatchDoesNotLogRawSensitiveMessage() {
        LaseShadowAdvisor advisor = Mockito.mock(LaseShadowAdvisor.class);
        Mockito.when(advisor.isEnabled()).thenReturn(true);
        Mockito.when(advisor.observe(Mockito.anyString(), Mockito.anyList(), Mockito.anyDouble(), Mockito.any()))
                .thenThrow(new IllegalStateException(
                        "outer observer failed token=raw-token Bearer raw-bearer-secret for CAPACITY_AWARE"));
        LoadBalancer balancer = balancerWithServers();
        balancer.setLaseShadowAdvisorForTesting(advisor);
        ListAppender<ILoggingEvent> appender = attachLoadBalancerAppender();
        try {
            LoadDistributionResult result = balancer.capacityAwareWithResult(60.0);

            assertFalse(result.allocations().isEmpty());
            String logMessages = messages(appender);
            assertTrue(logMessages.contains("outer observer failed"));
            assertTrue(logMessages.contains("CAPACITY_AWARE"));
            assertTrue(logMessages.contains("[redacted]"));
            assertFalse(logMessages.contains("raw-token"));
            assertFalse(logMessages.contains("raw-bearer-secret"));
        } finally {
            detachLoadBalancerAppender(appender);
            balancer.shutdown();
        }
    }

    @Test
    void advisorFailsSafelyWhenDistributionResultIsMissing() {
        Optional<LaseEvaluationReport> report = deterministicAdvisor(true)
                .observe("CAPACITY_AWARE", servers(), 60.0, null);

        assertTrue(report.isEmpty());
    }

    @Test
    void advisorDoesNotMutateServersOrDistributionResult() {
        List<Server> servers = servers();
        LoadDistributionResult result = new LoadDistributionResult(Map.of("S1", 25.0, "S2", 35.0), 0.0);
        double originalCapacity = servers.get(0).getCapacity();
        boolean originalHealth = servers.get(1).isHealthy();

        deterministicAdvisor(true).observe("CAPACITY_AWARE", servers, 60.0, result);

        assertEquals(2, servers.size());
        assertEquals(originalCapacity, servers.get(0).getCapacity(), 0.01);
        assertEquals(originalHealth, servers.get(1).isHealthy());
        assertEquals(Map.of("S1", 25.0, "S2", 35.0), result.allocations());
        assertEquals(0.0, result.unallocatedLoad(), 0.01);
    }

    @Test
    void advisorIsDeterministicWithFixedClockAndSeededRoutingSampler() {
        LoadDistributionResult result = new LoadDistributionResult(Map.of("S1", 25.0, "S2", 35.0), 0.0);

        LaseEvaluationReport first = deterministicAdvisor(true).observe("CAPACITY_AWARE", servers(), 60.0, result)
                .orElseThrow();
        LaseEvaluationReport second = deterministicAdvisor(true).observe("CAPACITY_AWARE", servers(), 60.0, result)
                .orElseThrow();

        assertEquals(first, second);
    }

    @Test
    void featureFlagDefaultsToDisabledAndCanEnableAdvisor() {
        assertFalse(LaseShadowAdvisor.fromSystemProperties().isEnabled());

        System.setProperty(LaseShadowAdvisor.ENABLED_PROPERTY, "true");

        assertTrue(LaseShadowAdvisor.fromSystemProperties().isEnabled());
    }

    @Test
    void loadBalancerShadowObservationPreservesCapacityAwareRoutingAndDoesNotConstructCloudManager() {
        LoadBalancer baseline = balancerWithServers();
        LoadBalancer observed = balancerWithServers();
        observed.setLaseShadowAdvisorForTesting(deterministicAdvisor(true));
        try (MockedConstruction<CloudManager> mockedCloudManager = Mockito.mockConstruction(CloudManager.class)) {
            LoadDistributionResult expected = baseline.capacityAwareWithResult(60.0);

            LoadDistributionResult actual = observed.capacityAwareWithResult(60.0);

            assertEquals(expected.allocations(), actual.allocations());
            assertEquals(expected.unallocatedLoad(), actual.unallocatedLoad(), 0.01);
            assertTrue(observed.getLastLaseShadowReportForTesting().isPresent());
            assertTrue(mockedCloudManager.constructed().isEmpty(),
                    "Shadow LASE observation must not construct CloudManager or call cloud paths.");
        } finally {
            baseline.shutdown();
            observed.shutdown();
        }
    }

    @Test
    void loadBalancerPredictiveShadowObservationUsesPredictiveStrategyName() {
        LaseShadowEventLog eventLog = new LaseShadowEventLog(10);
        LoadBalancer balancer = balancerWithServers();
        balancer.setLaseShadowAdvisorForTesting(deterministicAdvisor(true, eventLog));
        try {
            LoadDistributionResult result = balancer.predictiveLoadBalancingWithResult(60.0);

            assertFalse(result.allocations().isEmpty());
            LaseShadowObservabilitySnapshot snapshot = eventLog.snapshot();
            assertEquals(1, snapshot.recentEvents().size());
            LaseShadowEvent event = snapshot.recentEvents().get(0);
            assertEquals("PREDICTIVE", event.strategy());
            assertEquals("lase-shadow-predictive", event.evaluationId());
            assertTrue(event.reason().contains("Evaluation lase-shadow-predictive"));
        } finally {
            balancer.shutdown();
        }
    }

    @Test
    void allUnhealthyShadowObservationRecordsAdvisoryNoCandidateEvent() {
        LaseShadowEventLog eventLog = new LaseShadowEventLog(10);
        LaseShadowAdvisor advisor = deterministicAdvisor(true, eventLog);

        Optional<LaseEvaluationReport> report = advisor.observe("CAPACITY_AWARE", unhealthyServers(), 80.0,
                new LoadDistributionResult(Map.of(), 80.0));

        assertTrue(report.isPresent());
        assertTrue(report.orElseThrow().routingDecision().chosenServer().isEmpty());
        LaseShadowEvent event = eventLog.snapshot().recentEvents().get(0);
        assertNull(event.recommendedServerId());
        assertNull(event.agreedWithRouting());
        assertFalse(event.failSafe());
        assertTrue(event.reason().contains("no eligible server"));
    }

    @Test
    void disabledLoadBalancerAdvisorLeavesNoShadowReport() {
        LoadBalancer balancer = balancerWithServers();
        balancer.setLaseShadowAdvisorForTesting(LaseShadowAdvisor.disabled());
        try {
            LoadDistributionResult result = balancer.capacityAwareWithResult(60.0);

            assertFalse(result.allocations().isEmpty());
            assertTrue(balancer.getLastLaseShadowReportForTesting().isEmpty());
        } finally {
            balancer.shutdown();
        }
    }

    @Test
    void liveRoutingUsesExactRealTelemetryCandidateSetAndRatioUnits() {
        LaseShadowEventLog eventLog = new LaseShadowEventLog(20);
        LaseShadowAdvisor advisor = deterministicAdvisor(true, eventLog);
        List<ServerStateVector> candidates = List.of(
                liveState("degraded", 8, 300.0, 400.0, 500.0, 0.20, 2),
                liveState("healthy", 1, 40.0, 60.0, 70.0, 0.0, 0));

        LaseEvaluationReport report = null;
        for (int sample = 1; sample <= 10; sample++) {
            report = advisor.observeLiveRouting(new LiveRoutingShadowObservation(
                    "proxy-decision-%08d".formatted(sample),
                    NOW,
                    "checkout",
                    "ROUND_ROBIN",
                    "strategy",
                    "degraded",
                    candidates,
                    20,
                    sample)).orElseThrow();
        }

        assertNotNull(report);
        assertEquals("healthy", report.routingDecision().explanation().chosenServerId().orElseThrow());
        assertEquals(0.45, report.autoscalingRecommendation().utilization(), 0.0001);
        assertEquals(230.0, report.autoscalingRecommendation().observedP95LatencyMillis(), 0.0001);
        assertEquals(0.10, report.autoscalingRecommendation().observedErrorRate(), 0.0001);
        assertEquals(AutoscalingAction.SCALE_UP, report.autoscalingRecommendation().action());

        LaseShadowEvent event = eventLog.snapshot().recentEvents().get(9);
        assertEquals("strategy", event.selectionSource());
        assertEquals(List.of("degraded", "healthy"), event.candidateServerIds());
        assertEquals("degraded", event.actualSelectedServerId());
        assertEquals("healthy", event.recommendedServerId());
        assertEquals(Boolean.FALSE, event.agreedWithRouting());
        assertEquals(10, eventLog.snapshot().summary().comparableEvaluations());
    }

    @Test
    void liveRoutingPersistsAimdLimitAcrossEvaluations() {
        LaseShadowAdvisor advisor = deterministicAdvisor(true);
        List<ServerStateVector> candidates = List.of(
                liveState("S1", 1, 20.0, 40.0, 60.0, 0.0, 0),
                liveState("S2", 1, 25.0, 45.0, 65.0, 0.0, 0));

        LaseEvaluationReport tenth = null;
        for (int sample = 1; sample <= 10; sample++) {
            tenth = advisor.observeLiveRouting(liveObservation(sample, "strategy", "S1", candidates))
                    .orElseThrow();
        }
        LaseEvaluationReport eleventh = advisor.observeLiveRouting(
                liveObservation(11, "strategy", "S1", candidates)).orElseThrow();

        assertNotNull(tenth);
        assertEquals(20, tenth.concurrencyDecision().previousLimit());
        assertEquals(22, tenth.concurrencyDecision().nextLimit());
        assertEquals(22, eleventh.concurrencyDecision().previousLimit());
        assertEquals(24, eleventh.concurrencyDecision().nextLimit());
    }

    @Test
    void liveRoutingBoundsPersistedAimdTargets() {
        LaseShadowAdvisor advisor = deterministicAdvisor(true);
        List<ServerStateVector> candidates = List.of(
                liveState("S1", 1, 20.0, 40.0, 60.0, 0.0, 0));

        for (int route = 1; route <= 101; route++) {
            advisor.observeLiveRouting(new LiveRoutingShadowObservation(
                    "proxy-decision-%08d".formatted(route),
                    NOW,
                    "route-" + route,
                    "ROUND_ROBIN",
                    "strategy",
                    "S1",
                    candidates,
                    10,
                    1)).orElseThrow();
        }

        assertEquals(100, advisor.retainedConcurrencyTargetCount());
    }

    @Test
    void liveRoutingDoesNotClampRealConcurrencyLimitToLegacyHundred() {
        LaseShadowAdvisor advisor = deterministicAdvisor(true);
        ServerStateVector candidate = new ServerStateVector(
                "large-capacity", true, 50, 250.0, 250.0,
                20.0, 40.0, 60.0, 0.0, 0, NOW);
        LiveRoutingShadowObservation observation = new LiveRoutingShadowObservation(
                "proxy-decision-00000001", NOW, "large-route", "ROUND_ROBIN", "strategy",
                "large-capacity", List.of(candidate), 250, 10);

        LaseEvaluationReport report = advisor.observeLiveRouting(observation).orElseThrow();

        assertEquals(250, report.concurrencyDecision().previousLimit());
        assertEquals(252, report.concurrencyDecision().nextLimit());
    }

    @Test
    void affinityChoicesRemainVisibleButAreExcludedFromAgreementDenominator() {
        LaseShadowEventLog eventLog = new LaseShadowEventLog(10);
        LaseShadowAdvisor advisor = deterministicAdvisor(true, eventLog);
        List<ServerStateVector> candidates = List.of(
                liveState("S1", 1, 20.0, 40.0, 60.0, 0.0, 0),
                liveState("S2", 1, 25.0, 45.0, 65.0, 0.0, 0));

        advisor.observeLiveRouting(liveObservation(1, "affinity", "S2", candidates)).orElseThrow();

        LaseShadowObservabilitySnapshot snapshot = eventLog.snapshot();
        assertEquals(1, snapshot.summary().totalEvaluations());
        assertEquals(0, snapshot.summary().comparableEvaluations());
        assertNull(snapshot.recentEvents().get(0).agreedWithRouting());
        assertEquals("affinity", snapshot.recentEvents().get(0).selectionSource());
        assertEquals(List.of("S1", "S2"), snapshot.recentEvents().get(0).candidateServerIds());
    }

    @Test
    void allocationFixtureInputUsesBoundedRatioForAutoscaling() {
        LaseEvaluationInput input = LaseShadowAdvisor.buildInput(
                "CAPACITY_AWARE",
                servers(),
                60.0,
                new LoadDistributionResult(Map.of("S1", 25.0, "S2", 35.0), 0.0),
                NOW);

        assertTrue(input.autoscalingSignal().utilization() >= 0.0);
        assertTrue(input.autoscalingSignal().utilization() <= 1.0);
        assertEquals(60, input.autoscalingSignal().sampleSize());
    }

    private static LiveRoutingShadowObservation liveObservation(
            int sequence,
            String selectionSource,
            String actualServerId,
            List<ServerStateVector> candidates) {
        return new LiveRoutingShadowObservation(
                "proxy-decision-%08d".formatted(sequence),
                NOW,
                "checkout",
                "ROUND_ROBIN",
                selectionSource,
                actualServerId,
                candidates,
                20,
                sequence);
    }

    private static ServerStateVector liveState(
            String serverId,
            int inFlight,
            double averageLatency,
            double p95Latency,
            double p99Latency,
            double errorRate,
            int queueDepth) {
        return new ServerStateVector(
                serverId,
                true,
                inFlight,
                10.0,
                10.0,
                averageLatency,
                p95Latency,
                p99Latency,
                errorRate,
                queueDepth,
                NOW);
    }

    private static LaseShadowAdvisor deterministicAdvisor(boolean enabled) {
        return deterministicAdvisor(enabled, new LaseShadowEventLog(10));
    }

    private static LaseShadowAdvisor deterministicAdvisor(boolean enabled, LaseShadowEventLog eventLog) {
        LaseEvaluationEngine engine = deterministicEngine();
        return new LaseShadowAdvisor(enabled, engine, CLOCK, eventLog);
    }

    private static LaseShadowAdvisor throwingAdvisor(RuntimeException exception, LaseShadowEventLog eventLog) {
        return new LaseShadowAdvisor(true, (input, config) -> {
            throw exception;
        }, CLOCK, eventLog);
    }

    private static LaseEvaluationEngine deterministicEngine() {
        return new LaseEvaluationEngine(
                new TailLatencyPowerOfTwoStrategy(new ServerScoreCalculator(), new Random(7), CLOCK),
                new LoadSheddingPolicy(),
                new ShadowAutoscaler(),
                new FailureScenarioRunner(),
                CLOCK);
    }

    private static LoadBalancer balancerWithServers() {
        LoadBalancer balancer = new LoadBalancer();
        for (Server server : servers()) {
            balancer.addServer(server);
        }
        return balancer;
    }

    private static List<Server> servers() {
        Server first = new Server("S1", 20.0, 20.0, 20.0);
        first.setCapacity(80.0);
        Server second = new Server("S2", 10.0, 10.0, 10.0);
        second.setCapacity(100.0);
        return List.of(first, second);
    }

    private static List<Server> unhealthyServers() {
        Server first = new Server("S1", 95.0, 90.0, 85.0);
        first.setCapacity(80.0);
        first.setHealthy(false);
        Server second = new Server("S2", 99.0, 95.0, 90.0);
        second.setCapacity(100.0);
        second.setHealthy(false);
        return List.of(first, second);
    }

    private static ListAppender<ILoggingEvent> attachLoadBalancerAppender() {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LoadBalancer.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachLoadBalancerAppender(ListAppender<ILoggingEvent> appender) {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LoadBalancer.class);
        logger.detachAppender(appender);
        appender.stop();
    }

    private static String messages(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
    }
}
