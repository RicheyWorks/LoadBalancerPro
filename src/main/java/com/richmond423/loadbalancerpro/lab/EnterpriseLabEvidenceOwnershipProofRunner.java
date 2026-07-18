package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.Policy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReleaseResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.StaleClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.AcquisitionAttempt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.TakeoverAttempt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentJournalDirectory.RetentionPolicy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.ArmRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.OperatorStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentOperatorService.RequestBatchRequest;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.RecoveryAction;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.RecoveryClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabExperimentStartupReconciler.RecoveryReport;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Bounded coordinator and child runtime for truly separate-process ownership proofs. */
public final class EnterpriseLabEvidenceOwnershipProofRunner {
    private static final String EVENT_PREFIX = "LBP_OWNERSHIP_PROOF|";
    private static final String SCENARIO = "tail-latency-pressure";
    private static final Duration CHILD_START_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration CHILD_RELEASE_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration CHILD_HOLD_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration STALE_WAIT = Duration.ofMillis(2_300);
    private static final Policy PROOF_POLICY = new Policy(
            Duration.ofSeconds(2), Duration.ofMillis(500), 1, 2, Duration.ofMillis(10));
    private static final EnterpriseLabEvidenceMutationAuthority REJECTING_AUTHORITY = () -> {
        throw new EnterpriseLabEvidenceOwnershipException(
                FailureClassification.LOCK_LOST,
                "proof process has no live ownership capability");
    };

    public EnterpriseLabEvidenceOwnershipProofReport run(Path outputDirectory) throws IOException {
        Path output = EnterpriseLabExperimentProofExporter.validateOutputDirectory(outputDirectory);
        Files.createDirectories(output);
        String runToken = UUID.randomUUID().toString().replace("-", "");
        Path lifecycleRoot = controlledRoot(output, runToken, ProofCase.LIFECYCLE);
        Path raceRoot = controlledRoot(output, runToken, ProofCase.RACE);
        Files.createDirectories(lifecycleRoot);
        Files.createDirectories(raceRoot);

        ChildEvidence initial;
        ChildEvidence contention;
        ChildEvidence initialRelease;
        ChildEvidence cleanTakeover;
        ChildEvidence restartedDenial;
        ChildEvidence abruptTakeover;
        ChildEvidence firstRestart;
        ChildEvidence repeatedRestart;
        boolean simultaneousAcquisitionSingleWinner;
        boolean competingTakeoverSingleWinner;

        ChildProcess initialOwner = startChild(output, runToken, ProofCase.LIFECYCLE, ChildAction.HOLD_PREPARE);
        try {
            initial = initialOwner.awaitEvidence(CHILD_START_TIMEOUT);
            requireReady(initial, "initial owner");
            contention = runOneShot(output, runToken, ProofCase.LIFECYCLE, ChildAction.CONTEND);
            initialRelease = initialOwner.release();
        } finally {
            initialOwner.closeIfAlive();
        }

        ChildProcess cleanOwner = startChild(output, runToken, ProofCase.LIFECYCLE, ChildAction.HOLD_PREPARE);
        try {
            cleanTakeover = cleanOwner.awaitEvidence(CHILD_START_TIMEOUT);
            requireReady(cleanTakeover, "clean-release takeover owner");
            restartedDenial = runOneShot(output, runToken, ProofCase.LIFECYCLE, ChildAction.CONTEND);
            cleanOwner.killAbruptly();
        } finally {
            cleanOwner.closeIfAlive();
        }
        awaitStaleBoundary();

        ChildProcess recoveredOwner = startChild(output, runToken, ProofCase.LIFECYCLE, ChildAction.HOLD_RECOVER);
        try {
            abruptTakeover = recoveredOwner.awaitEvidence(CHILD_START_TIMEOUT);
            requireReady(abruptTakeover, "abrupt-loss takeover owner");
            recoveredOwner.release();
        } finally {
            recoveredOwner.closeIfAlive();
        }

        ChildProcess restartOne = startChild(output, runToken, ProofCase.LIFECYCLE, ChildAction.HOLD_RECOVER);
        try {
            firstRestart = restartOne.awaitEvidence(CHILD_START_TIMEOUT);
            requireReady(firstRestart, "first idempotent restart");
            restartOne.release();
        } finally {
            restartOne.closeIfAlive();
        }
        ChildProcess restartTwo = startChild(output, runToken, ProofCase.LIFECYCLE, ChildAction.HOLD_RECOVER);
        try {
            repeatedRestart = restartTwo.awaitEvidence(CHILD_START_TIMEOUT);
            requireReady(repeatedRestart, "repeated idempotent restart");
            restartTwo.release();
        } finally {
            restartTwo.closeIfAlive();
        }

        RaceResult acquisitionRace = race(
                output, runToken, ProofCase.RACE, ChildAction.HOLD_RECOVER, false);
        simultaneousAcquisitionSingleWinner = acquisitionRace.singleWinner();
        acquisitionRace.winner().killAbruptly();
        acquisitionRace.closeAll();
        awaitStaleBoundary();
        RaceResult takeoverRace = race(
                output, runToken, ProofCase.RACE, ChildAction.HOLD_RECOVER, true);
        competingTakeoverSingleWinner = takeoverRace.singleWinner()
                && takeoverRace.winnerEvidence().mode() == ClaimMode.TAKEOVER
                && takeoverRace.winnerEvidence().staleClassification()
                        == StaleClassification.STALE_CANDIDATE;
        takeoverRace.winner().release();
        takeoverRace.closeAll();

        boolean repeatedRestartIdempotent = firstRestart.journalCount() == repeatedRestart.journalCount()
                && firstRestart.eventCount() == repeatedRestart.eventCount()
                && firstRestart.lastJournalFingerprint().equals(
                        repeatedRestart.lastJournalFingerprint())
                && firstRestart.finalState().equals(repeatedRestart.finalState());
        return EnterpriseLabEvidenceOwnershipProofReport.create(
                Instant.now(),
                initial.generation(),
                cleanTakeover.generation(),
                abruptTakeover.generation(),
                repeatedRestart.generation(),
                contention.deniedByLiveOwner(),
                initial.preparedActiveExperiment() && initial.recoveryAllowed(),
                contention.appendDenied(),
                contention.compactionDenied(),
                contention.retentionDenied(),
                contention.experimentStartDenied(),
                contention.allocationChangeDenied(),
                initial.renewalSucceeded(),
                initialRelease.cleanRelease(),
                initialRelease.repeatedReleaseIdempotent(),
                cleanTakeover.mode() == ClaimMode.TAKEOVER
                        && cleanTakeover.staleClassification() == StaleClassification.CLEANLY_RELEASED,
                restartedDenial.deniedByLiveOwner(),
                abruptTakeover.mode() == ClaimMode.TAKEOVER
                        && abruptTakeover.staleClassification() == StaleClassification.STALE_CANDIDATE,
                abruptTakeover.journalsVerified(),
                abruptTakeover.interruptedRolledBack(),
                abruptTakeover.baselineVerified(),
                abruptTakeover.takeoverRecoveryRecorded(),
                repeatedRestartIdempotent,
                simultaneousAcquisitionSingleWinner,
                competingTakeoverSingleWinner);
    }

    public int runChild(
            Path outputDirectory,
            String runToken,
            String caseId,
            String actionName,
            java.io.PrintStream out,
            java.io.PrintStream err) {
        Objects.requireNonNull(out, "out cannot be null");
        Objects.requireNonNull(err, "err cannot be null");
        try {
            if (runToken == null || !runToken.matches("[0-9a-f]{32}")) {
                throw new IllegalArgumentException("proof run token must be bounded lowercase hexadecimal");
            }
            ProofCase proofCase = ProofCase.parse(caseId);
            ChildAction action = ChildAction.parse(actionName);
            Path output = EnterpriseLabExperimentProofExporter.validateOutputDirectory(outputDirectory);
            Path root = controlledRoot(output, runToken, proofCase);
            if (!Files.isDirectory(root)) {
                throw new IllegalArgumentException("proof child root was not prepared by the parent coordinator");
            }
            return executeChild(root, action, out);
        } catch (IOException | RuntimeException exception) {
            err.println("Enterprise Lab ownership proof child failed safely: " + safeMessage(exception));
            return 1;
        }
    }

    private int executeChild(Path root, ChildAction action, java.io.PrintStream out) throws IOException {
        if (action == ChildAction.CONTEND) {
            ChildEvidence evidence = contend(root);
            out.println(evidence.encode());
            return evidence.deniedByLiveOwner() ? 0 : 1;
        }

        Claim claim = claim(root);
        if (claim.lease().isEmpty()) {
            ChildEvidence denied = deniedEvidence(claim.failure());
            out.println(denied.encode());
            return 0;
        }

        EnterpriseLabEvidenceOwnershipLease lease = claim.lease().orElseThrow();
        PreparedOwner prepared = null;
        try {
            EnterpriseLabExperimentJournalDirectory directory =
                    EnterpriseLabExperimentJournalDirectory.create(root, lease.ownershipGate());
            if (action == ChildAction.HOLD_PREPARE) {
                prepared = PreparedOwner.start(
                        directory, claim.recoveryGate(), lease, claim.generation());
            }
            var renewal = lease.ownershipGate().renew();
            JournalSummary journals = action == ChildAction.HOLD_PREPARE
                    ? JournalSummary.empty()
                    : summarize(directory);
            RecoverySummary recovery = summarize(claim.recoveryReport());
            ChildEvidence ready = ChildEvidence.ready(
                    claim,
                    renewal.status() == OperationStatus.SUCCEEDED,
                    prepared != null && prepared.activeExperimentPrepared(),
                    journals,
                    recovery);
            out.println(ready.encode());
            out.flush();

            boolean releaseRequested = awaitReleaseRequest();
            if (prepared != null) {
                prepared.close();
                prepared = null;
            }
            ReleaseResult first = lease.release();
            ReleaseResult second = lease.release();
            ChildEvidence released = ready.released(
                    releaseRequested && first.status() == OperationStatus.SUCCEEDED,
                    first.equals(second));
            out.println(released.encode());
            out.flush();
            return released.cleanRelease() ? 0 : 1;
        } finally {
            if (prepared != null) {
                prepared.close();
            } else if (lease.operatingSystemLockValid()) {
                lease.release();
            }
        }
    }

    private static Claim claim(Path root) {
        Clock clock = Clock.systemUTC();
        EnterpriseLabExperimentRecoveryGate gate = EnterpriseLabExperimentRecoveryGate.pending();
        AcquisitionAttempt acquisition = EnterpriseLabEvidenceOwnershipManager.acquire(
                root, PROOF_POLICY, clock);
        if (acquisition.result().status() == OperationStatus.SUCCEEDED) {
            EnterpriseLabEvidenceOwnershipLease lease = acquisition.ownership().orElseThrow();
            EnterpriseLabExperimentJournalDirectory directory =
                    EnterpriseLabExperimentJournalDirectory.create(root, lease.ownershipGate());
            RecoveryReport report = new EnterpriseLabExperimentStartupReconciler(
                    directory,
                    new EnterpriseLabProcessLocalAllocationRecovery(fixedTargets()),
                    gate,
                    clock).initialize();
            return Claim.acquired(lease, gate, report);
        }
        if (acquisition.result().failure() != FailureClassification.TAKEOVER_NOT_PERMITTED) {
            return Claim.denied(acquisition.result().failure());
        }
        TakeoverAttempt takeover = EnterpriseLabEvidenceOwnershipManager.takeover(
                root,
                PROOF_POLICY,
                clock,
                new EnterpriseLabExperimentStartupReconciler(
                        EnterpriseLabExperimentJournalDirectory.create(root),
                        new EnterpriseLabProcessLocalAllocationRecovery(fixedTargets()),
                        gate,
                        clock));
        if (takeover.result().status() != OperationStatus.SUCCEEDED) {
            return Claim.denied(takeover.result().failure());
        }
        return Claim.takenOver(
                takeover.ownership().orElseThrow(),
                gate,
                gate.admissionStatus().recoveryReport().orElseThrow(),
                takeover.staleOwnerFinding().orElseThrow().classification());
    }

    private static ChildEvidence contend(Path root) {
        AcquisitionAttempt attempt = EnterpriseLabEvidenceOwnershipManager.acquire(
                root, PROOF_POLICY, Clock.systemUTC());
        boolean liveDenied = attempt.result().status() != OperationStatus.SUCCEEDED
                && (attempt.result().failure() == FailureClassification.LIVE_COMPETING_OWNER
                || attempt.result().failure() == FailureClassification.DUPLICATE_ACQUISITION);
        boolean appendDenied = mutationDenied(() ->
                EnterpriseLabExperimentJournalDirectory.create(root)
                        .openJournal("non-owner-proof").close());
        boolean compactionDenied = mutationDenied(() ->
                EnterpriseLabExperimentJournalDirectory.create(root)
                        .compactTerminal("non-owner-proof", Clock.systemUTC(), "PROOF_REQUESTED"));
        boolean retentionDenied = mutationDenied(() ->
                EnterpriseLabExperimentJournalDirectory.create(root)
                        .enforceRetention(new RetentionPolicy(0), false, Clock.systemUTC()));
        EnterpriseLabExperimentRecoveryGate pending = EnterpriseLabExperimentRecoveryGate.pending();
        boolean startDenied;
        try (EnterpriseLabExperimentOperatorService service =
                new EnterpriseLabExperimentOperatorService(fixedTargets(), pending)) {
            var receipt = service.start("non-owner-proof", "non-owner-start", true);
            startDenied = receipt.status() == OperatorStatus.DENIED
                    && "RECOVERY_NOT_READY".equals(receipt.reasonCode());
        }
        boolean allocationDenied = mutationDenied(() -> {
            List<EnterpriseLabLoopbackTarget> targets = fixedTargets()
                    .findTargets(SCENARIO).orElseThrow();
            EnterpriseLabLoopbackAllocationRouter router = new EnterpriseLabLoopbackAllocationRouter(
                    targets,
                    new EnterpriseLabLoopbackObservationIngress(Set.of("blue", "green", "orange")),
                    Map.of("blue", 1.0 / 3.0, "green", 1.0 / 3.0, "orange", 1.0 / 3.0),
                    Optional.of(REJECTING_AUTHORITY));
            router.restoreBaseline("non-owner proof");
        });
        return ChildEvidence.contended(
                attempt.result().failure(), liveDenied, appendDenied, compactionDenied,
                retentionDenied, startDenied, allocationDenied);
    }

    private static boolean mutationDenied(CheckedMutation mutation) {
        try {
            mutation.run();
            return false;
        } catch (EnterpriseLabEvidenceOwnershipException exception) {
            return exception.classification() != FailureClassification.NONE;
        } catch (EnterpriseLabExperimentJournalStorageException exception) {
            return true;
        } catch (IOException exception) {
            throw new IllegalStateException("proof mutation close failed", exception);
        }
    }

    private static JournalSummary summarize(EnterpriseLabExperimentJournalDirectory directory) {
        int journals = 0;
        int events = 0;
        String lastFingerprint = EnterpriseLabEvidenceOwnership.NO_RECORD_FINGERPRINT;
        String finalState = "NONE";
        boolean verified = true;
        for (var discovery : directory.discover()) {
            journals++;
            verified &= discovery.verification().isPresent()
                    && discovery.verification().orElseThrow().chainValid();
            if (discovery.verification().isPresent()) {
                var verification = discovery.verification().orElseThrow();
                events += verification.verifiedEvents().size();
                lastFingerprint = verification.lastVerifiedFingerprint();
                var replay = new EnterpriseLabExperimentJournalReplayEngine().replay(verification);
                if (replay.reconstructedState().isPresent()) {
                    finalState = replay.reconstructedState().orElseThrow().lifecycle().state().name();
                }
            }
        }
        return new JournalSummary(journals, events, lastFingerprint, finalState, verified);
    }

    private static RecoverySummary summarize(RecoveryReport report) {
        boolean interrupted = report.experiments().stream()
                .anyMatch(value -> value.classification() == RecoveryClassification.INTERRUPTED_ROLLED_BACK
                        && value.finalState().orElse(null) == EnterpriseLabExperimentState.ROLLED_BACK);
        boolean baseline = report.experiments().stream()
                .filter(value -> value.classification() == RecoveryClassification.INTERRUPTED_ROLLED_BACK)
                .allMatch(value -> value.action() == RecoveryAction.BASELINE_RESTORATION_SUCCEEDED
                        || value.action() == RecoveryAction.NO_OP_RECONCILIATION);
        return new RecoverySummary(
                report.admissionAllowed(), interrupted, interrupted && baseline,
                report.experiments().stream().anyMatch(value ->
                        value.classification() == RecoveryClassification.INTERRUPTED_ROLLED_BACK));
    }

    private static boolean awaitReleaseRequest() throws IOException {
        Instant deadline = Instant.now().plus(CHILD_HOLD_TIMEOUT);
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (Instant.now().isBefore(deadline)) {
            if (input.ready()) {
                return "release".equals(input.readLine());
            }
            try {
                Thread.sleep(20L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static EnterpriseLabExperimentTargetCatalog fixedTargets() {
        return new EnterpriseLabExperimentTargetCatalog(List.of(
                new EnterpriseLabLoopbackTarget(
                        SCENARIO, "blue", URI.create("http://127.0.0.1:1/ownership-proof")),
                new EnterpriseLabLoopbackTarget(
                        SCENARIO, "green", URI.create("http://127.0.0.1:1/ownership-proof")),
                new EnterpriseLabLoopbackTarget(
                        SCENARIO, "orange", URI.create("http://127.0.0.1:1/ownership-proof"))));
    }

    private static RaceResult race(
            Path output,
            String runToken,
            ProofCase proofCase,
            ChildAction action,
            boolean expectTakeover) throws IOException {
        ChildProcess first = startChild(output, runToken, proofCase, action);
        ChildProcess second = startChild(output, runToken, proofCase, action);
        ChildEvidence firstEvidence = first.awaitEvidence(CHILD_START_TIMEOUT);
        ChildEvidence secondEvidence = second.awaitEvidence(CHILD_START_TIMEOUT);
        ChildProcess winner = firstEvidence.status() == ChildStatus.READY ? first : second;
        ChildEvidence winnerEvidence = firstEvidence.status() == ChildStatus.READY
                ? firstEvidence : secondEvidence;
        ChildEvidence loserEvidence = firstEvidence.status() == ChildStatus.DENIED
                ? firstEvidence : secondEvidence;
        boolean singleWinner = firstEvidence.status() != secondEvidence.status()
                && winnerEvidence.status() == ChildStatus.READY
                && loserEvidence.status() == ChildStatus.DENIED
                && loserEvidence.deniedByLiveOwner()
                && (!expectTakeover || winnerEvidence.mode() == ClaimMode.TAKEOVER);
        return new RaceResult(first, second, winner, winnerEvidence, singleWinner);
    }

    private static ChildEvidence runOneShot(
            Path output, String token, ProofCase proofCase, ChildAction action) throws IOException {
        ChildProcess child = startChild(output, token, proofCase, action);
        try {
            ChildEvidence evidence = child.awaitEvidence(CHILD_START_TIMEOUT);
            child.awaitExit(CHILD_RELEASE_TIMEOUT);
            return evidence;
        } finally {
            child.closeIfAlive();
        }
    }

    private static ChildProcess startChild(
            Path output, String token, ProofCase proofCase, ChildAction action) throws IOException {
        List<String> command = new ArrayList<>();
        Path java = Path.of(System.getProperty("java.home"), "bin",
                isWindows() ? "java.exe" : "java");
        command.add(java.toString());
        String classPath = System.getProperty("java.class.path");
        boolean singleEntry = !classPath.contains(System.getProperty("path.separator"));
        Path possibleJar = singleEntry ? Path.of(classPath) : null;
        if (singleEntry && classPath.endsWith(".jar")
                && Files.isRegularFile(Objects.requireNonNull(possibleJar))) {
            command.add("-jar");
            command.add(possibleJar.toString());
        } else {
            command.add("-cp");
            command.add(classPath);
            command.add("com.richmond423.loadbalancerpro.api.LoadBalancerApiApplication");
        }
        command.add("--enterprise-lab-ownership-proof-child=" + action.wireValue);
        command.add("--enterprise-lab-ownership-proof-output=" + output);
        command.add("--enterprise-lab-ownership-proof-run=" + token);
        command.add("--enterprise-lab-ownership-proof-case=" + proofCase.wireValue);
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(Path.of("").toAbsolutePath().normalize().toFile())
                .redirectErrorStream(true);
        return new ChildProcess(builder.start());
    }

    private static Path controlledRoot(Path output, String token, ProofCase proofCase) {
        Path root = output.resolve("ownership-runs").resolve(token)
                .resolve(proofCase.wireValue).toAbsolutePath().normalize();
        Path safeOutput = output.toAbsolutePath().normalize();
        if (!root.startsWith(safeOutput)) {
            throw new IllegalArgumentException("ownership proof root escaped the target output boundary");
        }
        return root;
    }

    private static void requireReady(ChildEvidence evidence, String step) {
        if (evidence.status() != ChildStatus.READY) {
            throw new IllegalStateException(step + " was not ready: " + evidence.failure());
        }
    }

    private static void awaitStaleBoundary() {
        try {
            Thread.sleep(STALE_WAIT.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ownership proof stale wait was interrupted", exception);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private enum ProofCase {
        LIFECYCLE("lifecycle"),
        RACE("race");

        private final String wireValue;

        ProofCase(String wireValue) {
            this.wireValue = wireValue;
        }

        private static ProofCase parse(String value) {
            for (ProofCase candidate : values()) {
                if (candidate.wireValue.equals(value)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("unsupported ownership proof case");
        }
    }

    private enum ChildAction {
        HOLD_PREPARE("hold-prepare"),
        HOLD_RECOVER("hold-recover"),
        CONTEND("contend");

        private final String wireValue;

        ChildAction(String wireValue) {
            this.wireValue = wireValue;
        }

        private static ChildAction parse(String value) {
            for (ChildAction candidate : values()) {
                if (candidate.wireValue.equals(value)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("unsupported ownership proof child action");
        }
    }

    private enum ChildStatus { READY, DENIED, RELEASED }

    private enum ClaimMode { NONE, ACQUIRED, TAKEOVER }

    private record Claim(
            Optional<EnterpriseLabEvidenceOwnershipLease> lease,
            Optional<EnterpriseLabExperimentRecoveryGate> gate,
            Optional<RecoveryReport> report,
            ClaimMode mode,
            StaleClassification staleClassification,
            FailureClassification failure) {
        private static Claim acquired(
                EnterpriseLabEvidenceOwnershipLease lease,
                EnterpriseLabExperimentRecoveryGate gate,
                RecoveryReport report) {
            return new Claim(Optional.of(lease), Optional.of(gate), Optional.of(report),
                    ClaimMode.ACQUIRED, StaleClassification.NO_PREVIOUS_OWNER,
                    FailureClassification.NONE);
        }

        private static Claim takenOver(
                EnterpriseLabEvidenceOwnershipLease lease,
                EnterpriseLabExperimentRecoveryGate gate,
                RecoveryReport report,
                StaleClassification stale) {
            return new Claim(Optional.of(lease), Optional.of(gate), Optional.of(report),
                    ClaimMode.TAKEOVER, stale, FailureClassification.NONE);
        }

        private static Claim denied(FailureClassification failure) {
            return new Claim(Optional.empty(), Optional.empty(), Optional.empty(),
                    ClaimMode.NONE, StaleClassification.NO_PREVIOUS_OWNER, failure);
        }

        private EnterpriseLabExperimentRecoveryGate recoveryGate() {
            return gate.orElseThrow();
        }

        private RecoveryReport recoveryReport() {
            return report.orElseThrow();
        }

        private long generation() {
            return lease.orElseThrow().record().generation();
        }
    }

    private record JournalSummary(
            int journalCount,
            int eventCount,
            String lastFingerprint,
            String finalState,
            boolean verified) {
        private static JournalSummary empty() {
            return new JournalSummary(0, 0,
                    EnterpriseLabEvidenceOwnership.NO_RECORD_FINGERPRINT, "ACTIVE", true);
        }
    }

    private record RecoverySummary(
            boolean admissionAllowed,
            boolean interruptedRolledBack,
            boolean baselineVerified,
            boolean recoveryRecorded) {
    }

    private record ChildEvidence(
            ChildStatus status,
            ClaimMode mode,
            FailureClassification failure,
            StaleClassification staleClassification,
            long generation,
            boolean deniedByLiveOwner,
            boolean renewalSucceeded,
            boolean preparedActiveExperiment,
            boolean recoveryAllowed,
            boolean appendDenied,
            boolean compactionDenied,
            boolean retentionDenied,
            boolean experimentStartDenied,
            boolean allocationChangeDenied,
            boolean journalsVerified,
            boolean interruptedRolledBack,
            boolean baselineVerified,
            boolean takeoverRecoveryRecorded,
            int journalCount,
            int eventCount,
            String lastJournalFingerprint,
            String finalState,
            boolean cleanRelease,
            boolean repeatedReleaseIdempotent) {
        private static ChildEvidence ready(
                Claim claim,
                boolean renewed,
                boolean prepared,
                JournalSummary journals,
                RecoverySummary recovery) {
            return new ChildEvidence(
                    ChildStatus.READY, claim.mode(), FailureClassification.NONE,
                    claim.staleClassification(), claim.generation(), false, renewed, prepared,
                    recovery.admissionAllowed(), false, false, false, false, false,
                    journals.verified(), recovery.interruptedRolledBack(),
                    recovery.baselineVerified(), recovery.recoveryRecorded(),
                    journals.journalCount(), journals.eventCount(), journals.lastFingerprint(),
                    journals.finalState(), false, false);
        }

        private static ChildEvidence contended(
                FailureClassification failure,
                boolean liveDenied,
                boolean appendDenied,
                boolean compactionDenied,
                boolean retentionDenied,
                boolean startDenied,
                boolean allocationDenied) {
            return new ChildEvidence(
                    ChildStatus.DENIED, ClaimMode.NONE, failure,
                    StaleClassification.LIVE_COMPETING_OWNER, 0, liveDenied,
                    false, false, false, appendDenied, compactionDenied,
                    retentionDenied, startDenied, allocationDenied,
                    false, false, false, false, 0, 0,
                    EnterpriseLabEvidenceOwnership.NO_RECORD_FINGERPRINT,
                    "NONE", false, false);
        }

        private ChildEvidence released(boolean clean, boolean repeated) {
            return new ChildEvidence(
                    ChildStatus.RELEASED, mode, failure, staleClassification, generation,
                    deniedByLiveOwner, renewalSucceeded, preparedActiveExperiment,
                    recoveryAllowed, appendDenied, compactionDenied, retentionDenied,
                    experimentStartDenied, allocationChangeDenied, journalsVerified,
                    interruptedRolledBack, baselineVerified, takeoverRecoveryRecorded,
                    journalCount, eventCount, lastJournalFingerprint, finalState,
                    clean, repeated);
        }

        private String encode() {
            return EVENT_PREFIX + String.join("|",
                    status.name(), mode.name(), failure.name(), staleClassification.name(),
                    Long.toString(generation), Boolean.toString(deniedByLiveOwner),
                    Boolean.toString(renewalSucceeded), Boolean.toString(preparedActiveExperiment),
                    Boolean.toString(recoveryAllowed), Boolean.toString(appendDenied),
                    Boolean.toString(compactionDenied), Boolean.toString(retentionDenied),
                    Boolean.toString(experimentStartDenied), Boolean.toString(allocationChangeDenied),
                    Boolean.toString(journalsVerified), Boolean.toString(interruptedRolledBack),
                    Boolean.toString(baselineVerified), Boolean.toString(takeoverRecoveryRecorded),
                    Integer.toString(journalCount), Integer.toString(eventCount),
                    lastJournalFingerprint, finalState, Boolean.toString(cleanRelease),
                    Boolean.toString(repeatedReleaseIdempotent));
        }

        private static ChildEvidence decode(String line) {
            if (line == null || !line.startsWith(EVENT_PREFIX)) {
                throw new IllegalArgumentException("ownership proof child event is absent");
            }
            String[] values = line.substring(EVENT_PREFIX.length()).split("\\|", -1);
            if (values.length != 24) {
                throw new IllegalArgumentException("ownership proof child event has an invalid field count");
            }
            return new ChildEvidence(
                    ChildStatus.valueOf(values[0]), ClaimMode.valueOf(values[1]),
                    FailureClassification.valueOf(values[2]), StaleClassification.valueOf(values[3]),
                    Long.parseLong(values[4]), Boolean.parseBoolean(values[5]),
                    Boolean.parseBoolean(values[6]), Boolean.parseBoolean(values[7]),
                    Boolean.parseBoolean(values[8]), Boolean.parseBoolean(values[9]),
                    Boolean.parseBoolean(values[10]), Boolean.parseBoolean(values[11]),
                    Boolean.parseBoolean(values[12]), Boolean.parseBoolean(values[13]),
                    Boolean.parseBoolean(values[14]), Boolean.parseBoolean(values[15]),
                    Boolean.parseBoolean(values[16]), Boolean.parseBoolean(values[17]),
                    Integer.parseInt(values[18]), Integer.parseInt(values[19]),
                    values[20], values[21], Boolean.parseBoolean(values[22]),
                    Boolean.parseBoolean(values[23]));
        }
    }

    private static ChildEvidence deniedEvidence(FailureClassification failure) {
        boolean live = failure == FailureClassification.LIVE_COMPETING_OWNER
                || failure == FailureClassification.DUPLICATE_ACQUISITION;
        return ChildEvidence.contended(failure, live, false, false, false, false, false);
    }

    private static final class ChildProcess {
        private final Process process;
        private final BufferedWriter input;
        private final ArrayBlockingQueue<String> events = new ArrayBlockingQueue<>(8);
        private final StringBuilder boundedOutput = new StringBuilder();

        private ChildProcess(Process process) {
            this.process = process;
            this.input = new BufferedWriter(new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8));
            Thread reader = new Thread(this::readOutput, "enterprise-lab-ownership-proof-child-reader");
            reader.setDaemon(true);
            reader.start();
        }

        private void readOutput() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (boundedOutput) {
                        if (boundedOutput.length() < 16_384) {
                            boundedOutput.append(line).append(System.lineSeparator());
                        }
                    }
                    if (line.startsWith(EVENT_PREFIX)) {
                        events.offer(line);
                    }
                }
            } catch (IOException ignored) {
                // Process exit or forced termination closes the bounded proof stream.
            }
        }

        private ChildEvidence awaitEvidence(Duration timeout) {
            try {
                String line = events.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (line == null) {
                    throw new IllegalStateException("ownership proof child did not report: " + output());
                }
                return ChildEvidence.decode(line);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("ownership proof child wait was interrupted", exception);
            }
        }

        private ChildEvidence release() throws IOException {
            input.write("release");
            input.newLine();
            input.flush();
            ChildEvidence evidence = awaitEvidence(CHILD_RELEASE_TIMEOUT);
            if (evidence.status() != ChildStatus.RELEASED) {
                throw new IllegalStateException("ownership proof child did not release cleanly: " + output());
            }
            awaitExit(CHILD_RELEASE_TIMEOUT);
            return evidence;
        }

        private void killAbruptly() {
            process.destroyForcibly();
            awaitExit(CHILD_RELEASE_TIMEOUT);
        }

        private void awaitExit(Duration timeout) {
            try {
                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new IllegalStateException("ownership proof child did not exit within its bound");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("ownership proof child exit wait was interrupted", exception);
            }
        }

        private void closeIfAlive() {
            if (process.isAlive()) {
                process.destroyForcibly();
                try {
                    process.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private String output() {
            synchronized (boundedOutput) {
                return boundedOutput.toString();
            }
        }
    }

    private record RaceResult(
            ChildProcess first,
            ChildProcess second,
            ChildProcess winner,
            ChildEvidence winnerEvidence,
            boolean singleWinner) {
        private void closeAll() {
            first.closeIfAlive();
            second.closeIfAlive();
        }
    }

    private static final class PreparedOwner implements AutoCloseable {
        private final LoopbackCluster cluster;
        private final EnterpriseLabExperimentOperatorService service;
        private final boolean activeExperimentPrepared;

        private PreparedOwner(
                LoopbackCluster cluster,
                EnterpriseLabExperimentOperatorService service,
                boolean activeExperimentPrepared) {
            this.cluster = cluster;
            this.service = service;
            this.activeExperimentPrepared = activeExperimentPrepared;
        }

        private static PreparedOwner start(
                EnterpriseLabExperimentJournalDirectory directory,
                EnterpriseLabExperimentRecoveryGate gate,
                EnterpriseLabEvidenceOwnershipLease lease,
                long generation) throws IOException {
            LoopbackCluster cluster = LoopbackCluster.start();
            EnterpriseLabExperimentDurableEvidenceRepository repository =
                    new EnterpriseLabExperimentDurableEvidenceRepository(
                            directory, gate, Clock.systemUTC());
            EnterpriseLabExperimentOperatorService service =
                    new EnterpriseLabExperimentOperatorService(
                            cluster.targets(), gate, repository, lease);
            try {
                String experimentId = "ownership-proof-" + generation;
                var arm = service.arm(new ArmRequest(
                        "arm-" + generation, experimentId, SCENARIO, 12,
                        Duration.ofSeconds(30), 2, 1, Duration.ofSeconds(60)), true);
                var baseline = service.executeRequests(
                        experimentId,
                        new RequestBatchRequest(
                                "baseline-" + generation, 2, Duration.ofSeconds(1)),
                        true);
                var start = service.start(experimentId, "start-" + generation, true);
                boolean prepared = arm.status() == OperatorStatus.APPLIED
                        && baseline.observationsRecorded() == 2
                        && start.status() == OperatorStatus.APPLIED
                        && start.experimentRecord().orElseThrow().lifecycle().state()
                                == EnterpriseLabExperimentState.RUNNING;
                if (!prepared) {
                    throw new IllegalStateException(
                            "proof child could not prepare an interrupted active experiment: arm="
                                    + arm.status() + "/" + arm.reasonCode()
                                    + ", baseline=" + baseline.status() + "/" + baseline.reasonCode()
                                    + ", start=" + start.status() + "/" + start.reasonCode());
                }
                return new PreparedOwner(cluster, service, true);
            } catch (RuntimeException exception) {
                try {
                    service.close();
                } finally {
                    cluster.close();
                }
                throw exception;
            }
        }

        private boolean activeExperimentPrepared() {
            return activeExperimentPrepared;
        }

        @Override
        public void close() {
            try {
                service.close();
            } finally {
                cluster.close();
            }
        }
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
                    .map(value -> new EnterpriseLabLoopbackTarget(
                            SCENARIO, value.id, value.uri()))
                    .toList());
        }

        @Override
        public void close() {
            backends.forEach(Backend::close);
        }
    }

    private static final class Backend implements AutoCloseable {
        private final String id;
        private final HttpServer server;

        private Backend(String id, HttpServer server) {
            this.id = id;
            this.server = server;
        }

        private static Backend start(String id) throws IOException {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            Backend backend = new Backend(id, server);
            server.createContext("/ownership-proof", backend::handle);
            server.start();
            return backend;
        }

        private void handle(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        }

        private URI uri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                    + "/ownership-proof");
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    @FunctionalInterface
    private interface CheckedMutation {
        void run() throws IOException;
    }
}
