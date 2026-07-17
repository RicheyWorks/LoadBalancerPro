package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.ArmRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentRecoveryGate.InitializationState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalReplayEngine.Outcome;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabExperimentDurableEvidenceRepositoryTest {
    private static final String SCENARIO = "tail-latency-pressure";
    private static final Instant NOW = Instant.parse("2026-07-17T06:00:00Z");

    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptedOperatorLifecycleIsForceSyncedAndReplaysToItsTerminalState() throws Exception {
        try (Fixture fixture = Fixture.start(temporaryDirectory, 128)) {
            fixture.service.arm(arm("arm-terminal", "durable-terminal"), true);
            fixture.clock.advance(Duration.ofSeconds(1));
            fixture.service.start("durable-terminal", "start-terminal", true);
            fixture.clock.advance(Duration.ofSeconds(1));
            fixture.service.cancel(
                    "durable-terminal", "cancel-terminal", "bounded terminal replay proof");

            var verification = fixture.directory.verify("durable-terminal");
            assertEquals(EnterpriseLabExperimentJournalVerifier.Outcome.VALID, verification.outcome());
            assertTrue(verification.verifiedEvents().size() >= 7);
            var replay = fixture.directory.replay("durable-terminal");
            assertEquals(Outcome.RECONSTRUCTED, replay.outcome());
            assertEquals(EnterpriseLabExperimentState.ROLLED_BACK,
                    replay.reconstructedState().orElseThrow().lifecycle().state());
            assertEquals(Kind.RESTORED_BASELINE,
                    replay.reconstructedState().orElseThrow().lastAppliedAllocation().kind());
        }
    }

    @Test
    void interruptionClosesOnlyTheWriterAndStartupRecoveryNeverResumesCandidateTraffic() throws Exception {
        Fixture interrupted = Fixture.start(temporaryDirectory, 128);
        try {
            interrupted.service.arm(arm("arm-interrupt", "durable-interrupt"), true);
            interrupted.clock.advance(Duration.ofSeconds(1));
            interrupted.service.start("durable-interrupt", "start-interrupt", true);
            assertEquals(Kind.CANDIDATE,
                    interrupted.service.findRecord("durable-interrupt").orElseThrow()
                            .currentAllocation().kind());

            interrupted.repository.simulateProcessInterruption();
            EnterpriseLabExperimentRecoveryGate restartedGate = EnterpriseLabExperimentRecoveryGate.pending();
            var report = new EnterpriseLabExperimentStartupReconciler(
                    interrupted.directory,
                    new EnterpriseLabProcessLocalAllocationRecovery(interrupted.targets),
                    restartedGate,
                    interrupted.clock).initialize();

            assertTrue(report.admissionAllowed());
            assertEquals(InitializationState.READY, restartedGate.admissionStatus().state());
            assertEquals(EnterpriseLabExperimentState.ROLLED_BACK,
                    interrupted.directory.replay("durable-interrupt")
                            .reconstructedState().orElseThrow().lifecycle().state());
            assertTrue(report.experiments().stream()
                    .allMatch(result -> result.action()
                            == EnterpriseLabExperimentStartupReconciler.RecoveryAction.NO_OP_RECONCILIATION));
            assertEquals(0, interrupted.backends.stream().mapToInt(Backend::requestCount).sum());
        } finally {
            interrupted.closeAfterInterruption();
        }
    }

    @Test
    void appendCapacityFailureFailsAdmissionAndRestoresTheProcessLocalBaseline() throws Exception {
        try (Fixture fixture = Fixture.start(temporaryDirectory, 1)) {
            fixture.service.arm(arm("arm-full", "durable-full"), true);

            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> fixture.service.start("durable-full", "start-full", true));
            assertTrue(failure.getCause() instanceof EnterpriseLabExperimentJournalStorageException);
            assertEquals(InitializationState.FAILED, fixture.gate.admissionStatus().state());
            assertEquals("DURABLE_APPEND_FAILED", fixture.gate.reasonCode());
            var record = fixture.service.findRecord("durable-full").orElseThrow();
            assertEquals(EnterpriseLabExperimentState.ROLLED_BACK, record.lifecycle().state());
            assertEquals(Kind.RESTORED_BASELINE, record.currentAllocation().kind());
            assertFalse(fixture.gate.admissionAllowed());
            assertEquals(0, fixture.backends.stream().mapToInt(Backend::requestCount).sum());
        }
    }

    @Test
    void terminalCompactionIsVerifiedBoundedAndIdempotentWhileActiveEvidenceIsRejected() throws Exception {
        try (Fixture fixture = Fixture.start(temporaryDirectory, 128)) {
            fixture.service.arm(arm("arm-compact", "durable-compact"), true);
            assertEquals(EnterpriseLabExperimentJournalVerifier.Outcome.UNAVAILABLE,
                    fixture.repository.verify("durable-compact").outcome());
            assertThrows(EnterpriseLabExperimentJournalStorageException.class,
                    () -> fixture.directory.compactTerminal(
                            "durable-compact", fixture.clock, "OPERATOR_REQUESTED"));
            fixture.service.cancel(
                    "durable-compact", "cancel-compact", "terminal compaction proof");

            var compacted = fixture.directory.compactTerminal(
                    "durable-compact", fixture.clock, "OPERATOR_REQUESTED");

            assertEquals(
                    EnterpriseLabExperimentJournalDirectory.CompactionOutcome.COMPACTED,
                    compacted.outcome());
            assertTrue(compacted.sourceRemoved());
            assertEquals(EnterpriseLabExperimentState.CANCELLED,
                    compacted.manifest().terminalState());
            assertEquals(1, fixture.directory.compactedManifests().size());
            assertTrue(fixture.directory.discover().isEmpty());
            assertEquals(
                    EnterpriseLabExperimentJournalDirectory.CompactionOutcome.COMPLETED_IDEMPOTENTLY,
                    fixture.directory.compactTerminal(
                            "durable-compact", fixture.clock, "OPERATOR_REQUESTED").outcome());
        }
    }

    @Test
    void retentionDryRunAndApplyNeverRemoveUnresolvedCorruption() throws Exception {
        try (Fixture fixture = Fixture.start(temporaryDirectory, 128)) {
            fixture.service.arm(arm("arm-corrupt", "durable-corrupt-retention"), true);
            fixture.service.cancel(
                    "durable-corrupt-retention", "cancel-corrupt", "retention corruption proof");
            Path journal = Files.walk(temporaryDirectory)
                    .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .findFirst()
                    .orElseThrow();
            String content = Files.readString(journal, StandardCharsets.UTF_8);
            Files.writeString(journal,
                    content.replaceFirst("tail-latency-pressure", "tail-latency-pressurE"),
                    StandardCharsets.UTF_8);

            var dryRun = fixture.directory.enforceRetention(
                    new EnterpriseLabExperimentJournalDirectory.RetentionPolicy(0),
                    true,
                    fixture.clock);
            var applied = fixture.directory.enforceRetention(
                    new EnterpriseLabExperimentJournalDirectory.RetentionPolicy(0),
                    false,
                    fixture.clock);

            assertTrue(dryRun.actions().isEmpty());
            assertEquals(1, dryRun.unresolvedJournalsRetained());
            assertTrue(applied.actions().isEmpty());
            assertEquals(1, applied.unresolvedJournalsRetained());
            assertTrue(Files.exists(journal));
            assertTrue(fixture.directory.compactedManifests().isEmpty());
        }
    }

    @Test
    void healthyCompletionWithHoldCyclesRemainsExactlyReplayable() throws Exception {
        try (Fixture fixture = Fixture.start(temporaryDirectory, 128)) {
            String experimentId = "durable-completion";
            fixture.service.arm(new ArmRequest(
                    "arm-completion", experimentId, SCENARIO, 60, Duration.ofSeconds(30),
                    15, 2, Duration.ofSeconds(60)), true);
            fixture.service.executeRequests(
                    experimentId, new EnterpriseLabExperimentOperatorService.RequestBatchRequest(
                            "baseline-completion", 30, Duration.ofSeconds(1)), true);
            fixture.service.executeRequests(
                    experimentId, new EnterpriseLabExperimentOperatorService.RequestBatchRequest(
                            "baseline-completion-2", 30, Duration.ofSeconds(1)), true);
            fixture.service.start(experimentId, "start-completion", true);
            fixture.service.executeRequests(
                    experimentId, new EnterpriseLabExperimentOperatorService.RequestBatchRequest(
                            "candidate-completion", 30, Duration.ofSeconds(1)), true);
            fixture.service.executeRequests(
                    experimentId, new EnterpriseLabExperimentOperatorService.RequestBatchRequest(
                            "candidate-completion-2", 30, Duration.ofSeconds(1)), true);
            var evaluated = fixture.service.evaluate(experimentId, "evaluate-completion", true);
            if (!evaluated.experimentRecord().orElseThrow().lifecycle().state().terminal()) {
                evaluated = fixture.service.evaluate(experimentId, "evaluate-completion-2", true);
            }
            assertEquals(EnterpriseLabExperimentState.COMPLETED,
                    evaluated.experimentRecord().orElseThrow().lifecycle().state());
            var replay = fixture.directory.replay(experimentId);
            assertEquals(Outcome.RECONSTRUCTED, replay.outcome(), replay::toString);
            assertEquals(EnterpriseLabExperimentState.COMPLETED,
                    replay.reconstructedState().orElseThrow().lifecycle().state());
        }
    }

    private static ArmRequest arm(String requestId, String experimentId) {
        return new ArmRequest(
                requestId,
                experimentId,
                SCENARIO,
                12,
                Duration.ofSeconds(30),
                2,
                1,
                Duration.ofSeconds(60));
    }

    private static final class Fixture implements AutoCloseable {
        private final List<Backend> backends;
        private final EnterpriseLabExperimentTargetCatalog targets;
        private final MutableClock clock;
        private final EnterpriseLabExperimentJournalDirectory directory;
        private final EnterpriseLabExperimentRecoveryGate gate;
        private final EnterpriseLabExperimentDurableEvidenceRepository repository;
        private final EnterpriseLabExperimentOperatorService service;

        private Fixture(
                List<Backend> backends,
                EnterpriseLabExperimentTargetCatalog targets,
                MutableClock clock,
                EnterpriseLabExperimentJournalDirectory directory,
                EnterpriseLabExperimentRecoveryGate gate,
                EnterpriseLabExperimentDurableEvidenceRepository repository,
                EnterpriseLabExperimentOperatorService service) {
            this.backends = backends;
            this.targets = targets;
            this.clock = clock;
            this.directory = directory;
            this.gate = gate;
            this.repository = repository;
            this.service = service;
        }

        private static Fixture start(Path root, int maximumEntries) throws IOException {
            List<Backend> backends = new ArrayList<>();
            try {
                backends.add(Backend.start("blue"));
                backends.add(Backend.start("green"));
                backends.add(Backend.start("orange"));
                EnterpriseLabExperimentTargetCatalog targets = new EnterpriseLabExperimentTargetCatalog(
                        backends.stream()
                                .map(backend -> new EnterpriseLabLoopbackTarget(
                                        SCENARIO, backend.id, backend.uri()))
                                .toList());
                MutableClock clock = new MutableClock(NOW);
                EnterpriseLabExperimentJournalDirectory directory =
                        EnterpriseLabExperimentJournalDirectory.createForTesting(
                                root, EnterpriseLabExperimentJournalDirectory.HARD_MAX_JOURNAL_BYTES,
                                maximumEntries);
                EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();
                new EnterpriseLabExperimentStartupReconciler(
                        directory,
                        new EnterpriseLabProcessLocalAllocationRecovery(targets),
                        gate,
                        clock).initialize();
                EnterpriseLabExperimentDurableEvidenceRepository repository =
                        new EnterpriseLabExperimentDurableEvidenceRepository(directory, gate, clock);
                AtomicLong nanos = new AtomicLong();
                EnterpriseLabExperimentOperatorService service = new EnterpriseLabExperimentOperatorService(
                        targets,
                        new EnterpriseLabScenarioCatalogService(),
                        new EnterpriseLabAdaptiveDecisionService(),
                        clock,
                        () -> nanos.addAndGet(1_000_000L),
                        8,
                        gate,
                        Optional.of(repository));
                return new Fixture(List.copyOf(backends), targets, clock, directory, gate,
                        repository, service);
            } catch (IOException | RuntimeException exception) {
                backends.forEach(Backend::close);
                throw exception;
            }
        }

        @Override
        public void close() {
            service.close();
            backends.forEach(Backend::close);
        }

        private void closeAfterInterruption() {
            backends.forEach(Backend::close);
        }
    }

    private static final class Backend implements AutoCloseable {
        private final String id;
        private final HttpServer server;
        private int requests;

        private Backend(String id, HttpServer server) {
            this.id = id;
            this.server = server;
        }

        private static Backend start(String id) throws IOException {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            Backend backend = new Backend(id, server);
            server.createContext("/durable-evidence", backend::handle);
            server.start();
            return backend;
        }

        private synchronized void handle(HttpExchange exchange) throws IOException {
            requests++;
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        }

        private URI uri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                    + "/durable-evidence");
        }

        private synchronized int requestCount() {
            return requests;
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
