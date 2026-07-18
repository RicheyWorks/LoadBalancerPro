package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.ArmRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.RequestBatchRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.RecoveryClassification;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Foreground target-only proof of the shipped durable journal and recovery path. */
public final class EnterpriseLabDurableRecoveryProofRunner {
    private static final String SCENARIO = "tail-latency-pressure";
    private static final Instant EPOCH = Instant.parse("2026-07-17T07:00:00Z");
    private final List<EnterpriseLabEvidenceOwnershipLease> ownerships = new ArrayList<>();

    public EnterpriseLabDurableRecoveryProofReport run(Path outputDirectory) throws IOException {
        Path root = EnterpriseLabExperimentProofExporter.validateOutputDirectory(outputDirectory)
                .resolve("journal-data");
        Files.createDirectories(root);
        try (LoopbackCluster cluster = LoopbackCluster.start()) {
            MutableClock clock = new MutableClock(EPOCH);
            EnterpriseLabExperimentTargetCatalog targets = cluster.targets();

            InterruptedEvidence interrupted = interruptedRecovery(
                    root.resolve("interrupted"), targets, clock);
            TerminalEvidence completed = completedRestart(
                    root.resolve("completed"), targets, clock);
            TerminalEvidence rolledBack = normalRollbackRestart(
                    root.resolve("rolled-back"), targets, clock);
            boolean middleCorruptionQuarantined = corruptAndQuarantine(
                    root.resolve("middle-corruption"), targets, clock, false);
            boolean partialTailQuarantined = corruptAndQuarantine(
                    root.resolve("partial-tail"), targets, clock, true);
            boolean unresolvedRetained = unresolvedQuarantineRetained(
                    root.resolve("middle-corruption"), clock);
            EnterpriseLabExperimentTerminalManifest manifest = compactCompleted(
                    completed.directory, completed.experimentId, clock);

            return EnterpriseLabDurableRecoveryProofReport.create(
                    clock.instant(),
                    cluster.actualRequests(),
                    interrupted.finalState,
                    completed.finalState,
                    rolledBack.finalState,
                    interrupted.firstRecoveryAdmitted,
                    interrupted.secondRecoveryIdempotent,
                    completed.restartPreserved,
                    rolledBack.restartPreserved,
                    middleCorruptionQuarantined,
                    partialTailQuarantined,
                    unresolvedRetained,
                    interrupted.activeCompactionRejected,
                    completed.directory.compactedManifests().size() == 1
                            && completed.directory.discover().isEmpty(),
                    manifest.manifestFingerprint());
        } finally {
            for (int index = ownerships.size() - 1; index >= 0; index--) {
                ownerships.get(index).close();
            }
            ownerships.clear();
        }
    }

    private InterruptedEvidence interruptedRecovery(
            Path root,
            EnterpriseLabExperimentTargetCatalog targets,
            MutableClock clock) throws IOException {
        LiveService live = liveService(root, targets, clock);
        String experimentId = "proof-interrupted";
        live.service.arm(arm("arm-interrupted", experimentId, 12, 2, 1), true);
        live.service.executeRequests(
                experimentId, new RequestBatchRequest("baseline-interrupted", 2, Duration.ofSeconds(1)), true);
        clock.advance(Duration.ofSeconds(1));
        live.service.start(experimentId, "start-interrupted", true);
        live.service.executeRequests(
                experimentId, new RequestBatchRequest("candidate-interrupted", 2, Duration.ofSeconds(1)), true);
        boolean activeCompactionRejected;
        try {
            live.directory.compactTerminal(experimentId, clock, "PROOF_REQUESTED");
            activeCompactionRejected = false;
        } catch (EnterpriseLabExperimentJournalStorageException expected) {
            activeCompactionRejected = true;
        }
        live.repository.simulateProcessInterruption();

        EnterpriseLabExperimentRecoveryGate firstGate = EnterpriseLabExperimentRecoveryGate.pending();
        var first = new EnterpriseLabExperimentStartupReconciler(
                live.directory,
                new EnterpriseLabProcessLocalAllocationRecovery(targets),
                firstGate,
                clock).initialize();
        EnterpriseLabExperimentState finalState = live.directory.replay(experimentId)
                .reconstructedState().orElseThrow().lifecycle().state();
        clock.advance(Duration.ofSeconds(1));
        EnterpriseLabExperimentRecoveryGate secondGate = EnterpriseLabExperimentRecoveryGate.pending();
        var second = new EnterpriseLabExperimentStartupReconciler(
                live.directory,
                new EnterpriseLabProcessLocalAllocationRecovery(targets),
                secondGate,
                clock).initialize();
        boolean secondIdempotent = second.admissionAllowed()
                && second.experiments().size() == 1
                && second.experiments().get(0).classification()
                == RecoveryClassification.TERMINAL_PRESERVED
                && live.directory.replay(experimentId).reconstructedState().orElseThrow()
                        .lifecycle().state() == EnterpriseLabExperimentState.ROLLED_BACK;
        return new InterruptedEvidence(
                finalState, first.admissionAllowed(), secondIdempotent, activeCompactionRejected);
    }

    private TerminalEvidence completedRestart(
            Path root,
            EnterpriseLabExperimentTargetCatalog targets,
            MutableClock clock) throws IOException {
        LiveService live = liveService(root, targets, clock);
        String experimentId = "proof-completed";
        live.service.arm(arm("arm-completed", experimentId, 60, 15, 2), true);
        live.service.executeRequests(
                experimentId, new RequestBatchRequest("baseline-completed", 30, Duration.ofSeconds(1)), true);
        live.service.executeRequests(
                experimentId, new RequestBatchRequest("baseline-completed-2", 30, Duration.ofSeconds(1)), true);
        clock.advance(Duration.ofSeconds(1));
        live.service.start(experimentId, "start-completed", true);
        live.service.executeRequests(
                experimentId, new RequestBatchRequest("candidate-completed", 30, Duration.ofSeconds(1)), true);
        live.service.executeRequests(
                experimentId, new RequestBatchRequest("candidate-completed-2", 30, Duration.ofSeconds(1)), true);
        clock.advance(Duration.ofSeconds(1));
        var evaluated = live.service.evaluate(experimentId, "evaluate-completed", true);
        if (!evaluated.experimentRecord().orElseThrow().lifecycle().state().terminal()) {
            clock.advance(Duration.ofSeconds(1));
            evaluated = live.service.evaluate(experimentId, "evaluate-completed-2", true);
        }
        EnterpriseLabExperimentState state = evaluated.experimentRecord().orElseThrow().lifecycle().state();
        live.service.close();
        boolean preserved = terminalRestartPreserved(live.directory, targets, clock, experimentId, state);
        return new TerminalEvidence(experimentId, live.directory, state, preserved);
    }

    private TerminalEvidence normalRollbackRestart(
            Path root,
            EnterpriseLabExperimentTargetCatalog targets,
            MutableClock clock) throws IOException {
        LiveService live = liveService(root, targets, clock);
        String experimentId = "proof-normal-rollback";
        live.service.arm(arm("arm-normal-rollback", experimentId, 12, 2, 1), true);
        clock.advance(Duration.ofSeconds(1));
        live.service.start(experimentId, "start-normal-rollback", true);
        clock.advance(Duration.ofSeconds(1));
        var cancelled = live.service.cancel(
                experimentId, "cancel-normal-rollback", "packaged normal rollback proof");
        EnterpriseLabExperimentState state = cancelled.experimentRecord().orElseThrow().lifecycle().state();
        live.service.close();
        boolean preserved = terminalRestartPreserved(live.directory, targets, clock, experimentId, state);
        return new TerminalEvidence(experimentId, live.directory, state, preserved);
    }

    private boolean terminalRestartPreserved(
            EnterpriseLabExperimentJournalDirectory directory,
            EnterpriseLabExperimentTargetCatalog targets,
            MutableClock clock,
            String experimentId,
            EnterpriseLabExperimentState expected) {
        EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();
        var report = new EnterpriseLabExperimentStartupReconciler(
                directory,
                new EnterpriseLabProcessLocalAllocationRecovery(targets),
                gate,
                clock).initialize();
        return report.admissionAllowed()
                && directory.replay(experimentId).reconstructedState().orElseThrow()
                        .lifecycle().state() == expected;
    }

    private boolean corruptAndQuarantine(
            Path root,
            EnterpriseLabExperimentTargetCatalog targets,
            MutableClock clock,
            boolean partialTail) throws IOException {
        LiveService live = liveService(root, targets, clock);
        String suffix = partialTail ? "partial" : "middle";
        String experimentId = "proof-corrupt-" + suffix;
        live.service.arm(arm("arm-corrupt-" + suffix, experimentId, 12, 2, 1), true);
        live.repository.simulateProcessInterruption();
        Path journal;
        try (var paths = Files.walk(root)) {
            journal = paths.filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .findFirst()
                    .orElseThrow();
        }
        if (partialTail) {
            Files.writeString(journal, "{\"partial\"", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } else {
            String content = Files.readString(journal, StandardCharsets.UTF_8);
            Files.writeString(journal,
                    content.replaceFirst("tail-latency-pressure", "tail-latency-pressurE"),
                    StandardCharsets.UTF_8);
        }
        EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();
        var report = new EnterpriseLabExperimentStartupReconciler(
                live.directory,
                new EnterpriseLabProcessLocalAllocationRecovery(targets),
                gate,
                clock).initialize();
        return !report.admissionAllowed()
                && report.experiments().size() == 1
                && report.experiments().get(0).classification() == RecoveryClassification.QUARANTINED
                && live.directory.quarantineMetadata().size() == 1
                && !Files.exists(journal);
    }

    private boolean unresolvedQuarantineRetained(Path root, MutableClock clock) {
        EnterpriseLabEvidenceOwnershipLease ownership = ownerships.stream()
                .filter(value -> value.trustedRoot().equals(root.toAbsolutePath().normalize()))
                .findFirst()
                .orElseThrow();
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(
                        root.toAbsolutePath().normalize(), ownership.ownershipGate());
        int before = directory.quarantineMetadata().size();
        var retention = directory.enforceRetention(
                new EnterpriseLabExperimentJournalDirectory.RetentionPolicy(0), false, clock);
        return before == 1
                && retention.quarantineEntriesRetained() == 1
                && directory.quarantineMetadata().size() == 1;
    }

    private EnterpriseLabExperimentTerminalManifest compactCompleted(
            EnterpriseLabExperimentJournalDirectory directory,
            String experimentId,
            MutableClock clock) {
        clock.advance(Duration.ofSeconds(1));
        var result = directory.compactTerminal(experimentId, clock, "PROOF_REQUESTED");
        return result.manifest();
    }

    private LiveService liveService(
            Path root,
            EnterpriseLabExperimentTargetCatalog targets,
            MutableClock clock) throws IOException {
        Files.createDirectories(root);
        var acquisition = EnterpriseLabEvidenceOwnershipManager.acquire(
                root.toAbsolutePath().normalize(),
                EnterpriseLabEvidenceOwnership.Policy.safetyFirstDefaults(),
                clock);
        EnterpriseLabEvidenceOwnershipLease ownership = acquisition.ownership().orElseThrow(
                () -> new IllegalStateException(
                        "durable recovery proof ownership failed: "
                                + acquisition.result().failure().name()));
        ownerships.add(ownership);
        EnterpriseLabExperimentJournalDirectory directory =
                EnterpriseLabExperimentJournalDirectory.create(
                        root.toAbsolutePath().normalize(), ownership.ownershipGate());
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
        return new LiveService(directory, repository, service);
    }

    private static ArmRequest arm(
            String requestId,
            String experimentId,
            int maximumRequests,
            int minimumEvidence,
            int holdCycles) {
        return new ArmRequest(
                requestId, experimentId, SCENARIO, maximumRequests, Duration.ofSeconds(30),
                minimumEvidence, holdCycles, Duration.ofSeconds(60));
    }

    private record LiveService(
            EnterpriseLabExperimentJournalDirectory directory,
            EnterpriseLabExperimentDurableEvidenceRepository repository,
            EnterpriseLabExperimentOperatorService service) {
    }

    private record InterruptedEvidence(
            EnterpriseLabExperimentState finalState,
            boolean firstRecoveryAdmitted,
            boolean secondRecoveryIdempotent,
            boolean activeCompactionRejected) {
    }

    private record TerminalEvidence(
            String experimentId,
            EnterpriseLabExperimentJournalDirectory directory,
            EnterpriseLabExperimentState finalState,
            boolean restartPreserved) {
    }

    private static final class LoopbackCluster implements AutoCloseable {
        private final List<Backend> backends;

        private LoopbackCluster(List<Backend> backends) {
            this.backends = backends;
        }

        private static LoopbackCluster start() throws IOException {
            List<Backend> backends = new ArrayList<>();
            try {
                backends.add(Backend.start("blue"));
                backends.add(Backend.start("green"));
                backends.add(Backend.start("orange"));
                return new LoopbackCluster(List.copyOf(backends));
            } catch (IOException | RuntimeException exception) {
                backends.forEach(Backend::close);
                throw exception;
            }
        }

        private EnterpriseLabExperimentTargetCatalog targets() {
            return new EnterpriseLabExperimentTargetCatalog(backends.stream()
                    .map(backend -> new EnterpriseLabLoopbackTarget(
                            SCENARIO, backend.id, backend.uri()))
                    .toList());
        }

        private int actualRequests() {
            return backends.stream().mapToInt(backend -> backend.requests.get()).sum();
        }

        @Override
        public void close() {
            backends.forEach(Backend::close);
        }
    }

    private static final class Backend implements AutoCloseable {
        private final String id;
        private final HttpServer server;
        private final AtomicInteger requests = new AtomicInteger();

        private Backend(String id, HttpServer server) {
            this.id = id;
            this.server = server;
        }

        private static Backend start(String id) throws IOException {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            Backend backend = new Backend(id, server);
            server.createContext("/durable-recovery-proof", backend::handle);
            server.start();
            return backend;
        }

        private void handle(HttpExchange exchange) throws IOException {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        }

        private URI uri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                    + "/durable-recovery-proof");
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
