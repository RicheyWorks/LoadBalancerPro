package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyMode;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.FailureClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OperationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.Policy;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.AcquisitionAttempt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnershipManager.TakeoverAttempt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.RequestDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseStatus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Coordinates bounded packaged JVM proofs for the independent local supervisor. */
public final class EnterpriseLabIndependentSupervisorProofRunner {
    private static final String EVENT_PREFIX = "LBP_INDEPENDENT_SUPERVISOR_PROOF|";
    private static final Duration START_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration EXIT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration HOLD_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration STALE_WAIT = Duration.ofMillis(10_300);
    private static final Policy PROOF_POLICY = new Policy(
            Duration.ofSeconds(10), Duration.ofSeconds(5), 1, 2, Duration.ofMillis(10));
    private static final List<String> CRASH_WINDOWS = List.of(
            "before-intent",
            "after-intent",
            "before-apply",
            "after-apply",
            "before-readback",
            "after-readback",
            "before-commit",
            "after-commit-before-response");

    public EnterpriseLabIndependentSupervisorProofReport run(Path outputDirectory)
            throws IOException {
        Path output = EnterpriseLabExperimentProofExporter.validateOutputDirectory(
                outputDirectory);
        Files.createDirectories(output);
        String token = UUID.randomUUID().toString().replace("-", "");
        ProcessCounts counts = new ProcessCounts();

        SurvivalEvidence survival = proveApplicationSurvival(output, token, counts);
        boolean crashAfterApply = proveApplicationCrashAfterApply(output, token, counts);
        Map<String, Boolean> crashWindows = proveSupervisorCrashWindows(
                output, token, counts);
        boolean competitors = proveCompetingSupervisors(output, token, counts);
        Map<String, Boolean> ipc = proveIpcBoundaries(
                output, token, counts, survival.staleApplicationRejected());

        return EnterpriseLabIndependentSupervisorProofReport.create(
                Instant.now(),
                survival.installedStateSurvived(),
                survival.staleApplicationRejected(),
                survival.supervisorRestartReconciled(),
                crashAfterApply,
                crashWindows,
                competitors,
                ipc,
                counts.applications,
                counts.supervisors,
                survival.firstApplicationGeneration(),
                survival.recoveredApplicationGeneration(),
                survival.firstSupervisorGeneration(),
                survival.recoveredSupervisorGeneration(),
                survival.candidateFingerprint(),
                survival.baselineFingerprint(),
                List.of(
                        "Separate foreground local JVMs only; no embedded API server is started.",
                        "Supervisor IPC is authenticated one-request-per-connection binary transport on literal IPv4 loopback.",
                        "All generated state remains beneath the validated repository target directory.",
                        "Only repository-owned loopback targets and the fixed safe baseline are accepted.",
                        "Crash injection terminates proof child JVMs only and never alters production defaults.",
                        "No force unlock, arbitrary path, host, address, port, allocation, generation, or shutdown input is exported.",
                        "No external target, cloud, tenant, multi-host, network-filesystem, throughput, latency, load, or production claim."));
    }

    public int runChild(
            Path outputDirectory,
            String runToken,
            String caseId,
            String actionName,
            Optional<String> failureName,
            PrintStream out,
            PrintStream err) {
        Objects.requireNonNull(out, "out cannot be null");
        Objects.requireNonNull(err, "err cannot be null");
        try {
            if (runToken == null || !runToken.matches("[0-9a-f]{32}")) {
                throw new IllegalArgumentException(
                        "independent supervisor proof token must be lowercase hexadecimal");
            }
            if (caseId == null || !caseId.matches("[a-z0-9][a-z0-9-]{0,63}")) {
                throw new IllegalArgumentException(
                        "independent supervisor proof case is invalid");
            }
            ChildAction action = ChildAction.parse(actionName);
            Path output = EnterpriseLabExperimentProofExporter.validateOutputDirectory(
                    outputDirectory);
            Path root = controlledRoot(output, runToken, caseId);
            if (!Files.isDirectory(root)) {
                throw new IllegalArgumentException(
                        "independent supervisor proof child root was not prepared");
            }
            if (action == ChildAction.SUPERVISOR_FAILURE) {
                return runFailureSupervisor(
                        root,
                        failureName.orElseThrow(() -> new IllegalArgumentException(
                                "supervisor failure window is required")),
                        out);
            }
            if (failureName.isPresent()) {
                throw new IllegalArgumentException(
                        "application proof child cannot select a supervisor failure");
            }
            return runApplication(root, action, out);
        } catch (IOException | RuntimeException exception) {
            err.println("Enterprise Lab independent supervisor proof child failed safely: "
                    + safeMessage(exception));
            return 1;
        }
    }

    private SurvivalEvidence proveApplicationSurvival(
            Path output, String token, ProcessCounts counts) throws IOException {
        String proofCase = "application-survival";
        Path root = prepareRoot(output, token, proofCase);
        ManagedProcess supervisor = startNormalSupervisor(root, counts);
        ManagedProcess firstApplication = null;
        ManagedProcess recoveredApplication = null;
        try {
            EnterpriseLabSupervisorConnectionMetadata firstSupervisor =
                    awaitSupervisorReady(root, supervisor);
            firstApplication = startChild(
                    output, token, proofCase, ChildAction.HOLD_CANDIDATE,
                    Optional.empty(), counts);
            ChildEvidence first = firstApplication.awaitEvent(START_TIMEOUT);
            require(first.ready() && "CANDIDATE".equals(first.installedKind()),
                    "first application did not install a candidate: " + first);
            firstApplication.killAbruptly();
            awaitStaleBoundary();

            recoveredApplication = startChild(
                    output, token, proofCase, ChildAction.HOLD_RECONCILE,
                    Optional.empty(), counts);
            ChildEvidence recovered = recoveredApplication.awaitEvent(START_TIMEOUT);
            require(recovered.ready()
                            && recovered.applicationGeneration()
                            > first.applicationGeneration()
                            && isBaseline(recovered.installedKind())
                            && !first.installedFingerprint().equals(
                                    recovered.installedFingerprint()),
                    "replacement application did not reconcile the retained candidate");

            boolean staleRejected = staleApplicationRejected(
                    root, first, recovered);

            supervisor.killAbruptly();
            ChildEvidence loss = recoveredApplication.command("check-loss", START_TIMEOUT);
            require(!loss.ready(),
                    "application readiness remained open after supervisor loss");

            supervisor = startNormalSupervisor(root, counts);
            EnterpriseLabSupervisorConnectionMetadata replacementSupervisor =
                    awaitSupervisorReady(root, supervisor);
            ChildEvidence reconnected = recoveredApplication.command(
                    "reconnect", START_TIMEOUT);
            require(reconnected.ready()
                            && isBaseline(reconnected.installedKind())
                            && reconnected.supervisorGeneration()
                            == replacementSupervisor.supervisorGeneration()
                            && replacementSupervisor.supervisorGeneration()
                            > firstSupervisor.supervisorGeneration(),
                    "application did not reconcile a higher supervisor epoch");
            recoveredApplication.command("release", START_TIMEOUT);
            recoveredApplication.awaitExit(EXIT_TIMEOUT);
            return new SurvivalEvidence(
                    true,
                    staleRejected,
                    true,
                    first.applicationGeneration(),
                    recovered.applicationGeneration(),
                    firstSupervisor.supervisorGeneration(),
                    replacementSupervisor.supervisorGeneration(),
                    first.installedFingerprint(),
                    reconnected.installedFingerprint());
        } finally {
            close(firstApplication);
            close(recoveredApplication);
            close(supervisor);
        }
    }

    private boolean proveApplicationCrashAfterApply(
            Path output, String token, ProcessCounts counts) throws IOException {
        String proofCase = "application-after-supervisor-apply";
        Path root = prepareRoot(output, token, proofCase);
        ManagedProcess supervisor = startNormalSupervisor(root, counts);
        ManagedProcess crashing = null;
        try {
            awaitSupervisorReady(root, supervisor);
            crashing = startChild(
                    output, token, proofCase, ChildAction.CRASH_AFTER_APPLY,
                    Optional.empty(), counts);
            ChildEvidence applied = crashing.awaitEvent(START_TIMEOUT);
            require("CRASHING_AFTER_SUPERVISOR_APPLY".equals(applied.reasonCode()),
                    "application crash seam did not follow supervisor apply: " + applied);
            crashing.awaitExit(EXIT_TIMEOUT);
            require(crashing.exitCode() == 92,
                    "application crash child did not stop at its exact boundary");
            awaitStaleBoundary();

            ChildEvidence firstRecovery = runOneShot(
                    output, token, proofCase, ChildAction.RECONCILE_ONCE,
                    Optional.empty(), counts);
            ChildEvidence repeatedRecovery = runOneShot(
                    output, token, proofCase, ChildAction.RECONCILE_ONCE,
                    Optional.empty(), counts);
            return firstRecovery.ready()
                    && repeatedRecovery.ready()
                    && isBaseline(firstRecovery.installedKind())
                    && firstRecovery.installedFingerprint().equals(
                            repeatedRecovery.installedFingerprint())
                    && repeatedRecovery.applicationGeneration()
                            > firstRecovery.applicationGeneration();
        } finally {
            close(crashing);
            close(supervisor);
        }
    }

    private Map<String, Boolean> proveSupervisorCrashWindows(
            Path output, String token, ProcessCounts counts) throws IOException {
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (String window : CRASH_WINDOWS) {
            String proofCase = "supervisor-crash-" + window;
            Path root = prepareRoot(output, token, proofCase);
            ManagedProcess failingSupervisor = startChild(
                    output, token, proofCase, ChildAction.SUPERVISOR_FAILURE,
                    Optional.of(window), counts);
            ManagedProcess application = null;
            ManagedProcess replacement = null;
            boolean passed = false;
            try {
                EnterpriseLabSupervisorConnectionMetadata first =
                        awaitSupervisorReady(root, failingSupervisor);
                application = startChild(
                        output, token, proofCase, ChildAction.APPLY_ONCE,
                        Optional.empty(), counts);
                failingSupervisor.awaitEvent(START_TIMEOUT);
                failingSupervisor.awaitExit(EXIT_TIMEOUT);
                require(failingSupervisor.exitCode() == 93,
                        "supervisor did not terminate at " + window);
                application.awaitExit(EXIT_TIMEOUT);

                replacement = startNormalSupervisor(root, counts);
                EnterpriseLabSupervisorConnectionMetadata recoveredSupervisor =
                        awaitSupervisorReady(root, replacement);
                ChildEvidence recovery = runOneShot(
                        output, token, proofCase, ChildAction.RECONCILE_ONCE,
                        Optional.empty(), counts);
                passed = recovery.ready()
                        && isBaseline(recovery.installedKind())
                        && recoveredSupervisor.supervisorGeneration()
                                > first.supervisorGeneration();
            } finally {
                close(application);
                close(failingSupervisor);
                close(replacement);
            }
            results.put(window, passed);
        }
        return Map.copyOf(results);
    }

    private boolean proveCompetingSupervisors(
            Path output, String token, ProcessCounts counts) throws IOException {
        String proofCase = "competing-supervisors";
        Path root = prepareRoot(output, token, proofCase);
        ManagedProcess first = startNormalSupervisor(root, counts);
        ManagedProcess second = startNormalSupervisor(root, counts);
        ManagedProcess successor = null;
        ManagedProcess deniedRestart = null;
        try {
            EnterpriseLabSupervisorConnectionMetadata initial = awaitAnySupervisorReady(
                    root, List.of(first, second));
            awaitOneLoser(first, second);
            ManagedProcess winner = first.isAlive() ? first : second;
            ManagedProcess loser = first.isAlive() ? second : first;
            boolean oneWinner = winner.isAlive() && !loser.isAlive()
                    && loser.exitCode() == 2;
            winner.killAbruptly();

            successor = startNormalSupervisor(root, counts);
            EnterpriseLabSupervisorConnectionMetadata next =
                    awaitSupervisorReady(root, successor);
            deniedRestart = startNormalSupervisor(root, counts);
            deniedRestart.awaitExit(EXIT_TIMEOUT);
            return oneWinner
                    && next.supervisorGeneration() > initial.supervisorGeneration()
                    && successor.isAlive()
                    && deniedRestart.exitCode() == 2;
        } finally {
            close(first);
            close(second);
            close(successor);
            close(deniedRestart);
        }
    }

    private Map<String, Boolean> proveIpcBoundaries(
            Path output,
            String token,
            ProcessCounts counts,
            boolean staleApplicationRejected) throws IOException {
        String proofCase = "ipc-boundaries";
        Path root = prepareRoot(output, token, proofCase);
        ManagedProcess supervisor = startNormalSupervisor(root, counts);
        ManagedProcess application = null;
        try {
            EnterpriseLabSupervisorConnectionMetadata metadata =
                    awaitSupervisorReady(root, supervisor);
            application = startChild(
                    output,
                    token,
                    proofCase,
                    ChildAction.HOLD_RECONCILE,
                    Optional.empty(),
                    counts);
            ChildEvidence currentApplication = application.awaitEvent(START_TIMEOUT);
            require(currentApplication.ready()
                            && isBaseline(currentApplication.installedKind()),
                    "IPC proof application did not establish a safe owned baseline");
            EnterpriseLabExperimentTargetCatalog targets =
                    EnterpriseLabSupervisorConfiguration.approvedTargets();
            EnterpriseLabSupervisorProtocolCodec codec =
                    new EnterpriseLabSupervisorProtocolCodec(targets);
            Map<String, Boolean> checks = new TreeMap<>();
            checks.put("non-loopback-bind-rejected", rejects(() -> metadataWithAddress(
                    metadata, "0.0.0.0")));
            checks.put("hostname-target-rejected", rejects(() -> metadataWithAddress(
                    metadata, "localhost")));
            checks.put("redirect-or-proxy-response-rejected", rejects(() ->
                    codec.decodeResponse(
                            "HTTP/1.1 302 Found\r\nLocation: elsewhere\r\n\r\n"
                                    .getBytes(StandardCharsets.US_ASCII),
                            observation(codec, metadata, "redirect-check", Instant.now()))));

            byte[] credential = EnterpriseLabSupervisorServer.readCredentialForTesting(root);
            try {
                checks.put("missing-authentication", rawExchange(
                        metadata, new byte[0], new byte[]{1}, 0, 1).status()
                        == EnterpriseLabSupervisorServer.TRANSPORT_MALFORMED);
                byte[] wrong = new byte[EnterpriseLabSupervisorServer.CREDENTIAL_BYTES];
                Arrays.fill(wrong, (byte) '0');
                checks.put("incorrect-authentication", rawExchange(
                        metadata, wrong, new byte[]{1}, wrong.length, 1).status()
                        == EnterpriseLabSupervisorServer.TRANSPORT_UNAUTHORIZED);
                checks.put("oversized-request", rawExchange(
                        metadata,
                        credential,
                        new byte[0],
                        credential.length,
                        EnterpriseLabSupervisorProtocol.HARD_MAX_REQUEST_BYTES + 1).status()
                        == EnterpriseLabSupervisorServer.TRANSPORT_MALFORMED);

                Request base = observation(
                        codec, metadata, "ipc-base", Instant.now());
                byte[] encoded = codec.encodeRequest(base);
                checks.put("unknown-command", malformed(
                        metadata,
                        credential,
                        replace(
                                encoded,
                                "\"" + base.commandType().name() + "\"",
                                "\"UNKNOWN\"")));
                checks.put("unsupported-schema", malformed(
                        metadata,
                        credential,
                        replace(encoded,
                                EnterpriseLabSupervisorProtocol.SCHEMA_VERSION,
                                "enterprise-lab-allocation-supervisor-ipc/v2")));
                checks.put("invalid-allocation-fingerprint", malformed(
                        metadata,
                        credential,
                        replaceFirstFingerprint(encoded)));

                Request staleSupervisor = observation(
                        codec,
                        new EnterpriseLabSupervisorConnectionMetadata(
                                metadata.schemaVersion(),
                                metadata.address(),
                                metadata.port(),
                                metadata.supervisorInstanceId(),
                                metadata.supervisorGeneration() + 1L,
                                metadata.durableStateGeneration(),
                                metadata.stateFingerprint(),
                                metadata.publishedAt()),
                        "stale-supervisor",
                        Instant.now());
                RawReply staleReply = rawExchange(
                        metadata,
                        credential,
                        codec.encodeRequest(staleSupervisor),
                        credential.length,
                        codec.encodeRequest(staleSupervisor).length);
                Response staleResponse = codec.decodeResponse(
                        staleReply.body(), staleSupervisor);
                checks.put("stale-supervisor-generation",
                        staleResponse.status() == ResponseStatus.REJECTED
                                && "STALE_SUPERVISOR_GENERATION".equals(
                                        staleResponse.reasonCode()));
            } finally {
                Arrays.fill(credential, (byte) 0);
            }

            checks.put("oversized-response", rejects(() -> codec.decodeResponse(
                    new byte[EnterpriseLabSupervisorProtocol.HARD_MAX_RESPONSE_BYTES + 1],
                    observation(codec, metadata, "oversized-response", Instant.now()))));
            checks.put("stale-application-generation", staleApplicationRejected);

            try (EnterpriseLabSupervisorClient client =
                         EnterpriseLabSupervisorClient.connect(root, targets, Clock.systemUTC())) {
                Request duplicateOne = observation(
                        codec, metadata, "duplicate-request", Instant.now());
                Response first = client.execute(duplicateOne);
                Request duplicateChanged = new EnterpriseLabSupervisorProtocolCodec(targets)
                        .issue(new RequestDraft(
                                duplicateOne.requestId(),
                                duplicateOne.commandType(),
                                duplicateOne.applicationInstanceId(),
                                duplicateOne.applicationOwnershipRecordFingerprint(),
                                duplicateOne.applicationOwnerGeneration(),
                                duplicateOne.expectedSupervisorInstanceId(),
                                duplicateOne.expectedSupervisorGeneration(),
                                duplicateOne.transactionId(),
                                duplicateOne.experimentId(),
                                duplicateOne.allocationPurpose(),
                                duplicateOne.allocation(),
                                duplicateOne.allocationFingerprint(),
                                duplicateOne.previousCommittedFingerprint(),
                                duplicateOne.requestedAt(),
                                Map.of("proof", "changed")));
                Response changed = client.execute(duplicateChanged);
                checks.put("conflicting-duplicate",
                        first.status() == ResponseStatus.ACCEPTED
                                && changed.status() == ResponseStatus.REJECTED
                                && "DUPLICATE_REQUEST_CHANGED".equals(
                                        changed.reasonCode()));

                Request expired = observation(
                        codec,
                        metadata,
                        "expired-request",
                        Instant.now().minus(
                                EnterpriseLabSupervisorConfiguration.MAX_REQUEST_AGE)
                                .minusSeconds(1));
                Response expiredResponse = client.execute(expired);
                checks.put("expired-request",
                        expiredResponse.status() == ResponseStatus.REJECTED
                                && "REQUEST_EXPIRED".equals(
                                        expiredResponse.reasonCode()));

                Request correlationOne = observation(
                        codec, metadata, "correlation-one", Instant.now());
                Response correlationResponse = client.execute(correlationOne);
                Request correlationTwo = observation(
                        codec, metadata, "correlation-two", Instant.now());
                checks.put("request-id-mismatch", rejects(() ->
                        codec.decodeResponse(
                                codec.encodeResponse(correlationResponse), correlationTwo)));
                checks.put("supervisor-identity-mismatch", rejects(() -> codec.issue(
                        correlationOne,
                        new ResponseDraft(
                                correlationOne.requestId(),
                                correlationOne.requestFingerprint(),
                                correlationOne.commandType(),
                                "supervisor-wrong-identity",
                                metadata.supervisorGeneration(),
                                0L,
                                correlationOne.commandType().classification(),
                                ResponseStatus.ACCEPTED,
                                false,
                                correlationResponse.installedAllocation(),
                                correlationResponse.installedFingerprint(),
                                correlationResponse.routerGeneration(),
                                correlationResponse.durableStateGeneration(),
                                correlationResponse.verificationResult(),
                                "PROOF_RESPONSE",
                                "Bounded proof response",
                                Instant.now()))));
            }

            checks.put("unknown-backend", rejects(() -> {
                EnterpriseLabLoopbackAllocationSnapshot unknown =
                        new EnterpriseLabLoopbackAllocationSnapshot(
                                EnterpriseLabLoopbackAllocationSnapshot.SCHEMA_VERSION,
                                EnterpriseLabSupervisorConfiguration.SCENARIO_ID,
                                1L,
                                "unknown-backend-proof",
                                EnterpriseLabLoopbackAllocationSnapshot.Kind.CANDIDATE,
                                Map.of("unknown", 1.0));
                codec.issue(new RequestDraft(
                        "unknown-backend-request",
                        CommandType.APPLY_ALLOCATION,
                        "proof-application",
                        "1".repeat(64),
                        1L,
                        metadata.supervisorInstanceId(),
                        metadata.supervisorGeneration(),
                        "unknown-backend-transaction",
                        Optional.of("unknown-backend-experiment"),
                        AllocationPurpose.EXPERIMENT_CANDIDATE,
                        Optional.of(unknown),
                        EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                                unknown.scenarioId(), unknown.allocations()),
                        "2".repeat(64),
                        Instant.now(),
                        Map.of()));
            }));
            checks.put("excessive-concurrency", excessiveConcurrencyRejected(metadata));
            require(checks.size()
                            == EnterpriseLabIndependentSupervisorProofReport
                            .REQUIRED_IPC_BOUNDARY_CHECKS,
                    "IPC proof did not produce exactly eighteen checks");
            return Map.copyOf(checks);
        } finally {
            close(application);
            close(supervisor);
        }
    }

    private boolean staleApplicationRejected(
            Path root, ChildEvidence stale, ChildEvidence current) {
        EnterpriseLabExperimentTargetCatalog targets =
                EnterpriseLabSupervisorConfiguration.approvedTargets();
        EnterpriseLabSupervisorProtocolCodec codec =
                new EnterpriseLabSupervisorProtocolCodec(targets);
        try (EnterpriseLabSupervisorClient client =
                     EnterpriseLabSupervisorClient.connect(root, targets, Clock.systemUTC())) {
            EnterpriseLabSupervisorConnectionMetadata metadata = client.connectionMetadata();
            Response before = client.execute(observation(
                    codec, metadata, "stale-before", Instant.now()));
            Request request = codec.issue(new RequestDraft(
                    "stale-application-handoff",
                    CommandType.ADVANCE_APPLICATION_OWNERSHIP,
                    stale.applicationInstanceId(),
                    stale.applicationOwnershipFingerprint(),
                    stale.applicationGeneration(),
                    metadata.supervisorInstanceId(),
                    metadata.supervisorGeneration(),
                    "stale-application-transaction",
                    Optional.empty(),
                    AllocationPurpose.RECONCILIATION_NO_OP,
                    Optional.empty(),
                    EnterpriseLabSupervisorProtocol.NONE,
                    EnterpriseLabSupervisorProtocol.NONE,
                    Instant.now(),
                    Map.of()));
            Response rejected = client.execute(request);
            Response after = client.execute(observation(
                    codec, metadata, "stale-after", Instant.now()));
            return before.installedFingerprint().equals(after.installedFingerprint())
                    && rejected.status() == ResponseStatus.REJECTED
                    && ("STALE_APPLICATION_GENERATION".equals(rejected.reasonCode())
                    || "STALE_APPLICATION_OWNER".equals(rejected.reasonCode())
                    || "APPLICATION_OWNERSHIP_INVALID".equals(rejected.reasonCode()))
                    && current.applicationGeneration() > stale.applicationGeneration();
        }
    }

    private int runApplication(Path root, ChildAction action, PrintStream out)
            throws IOException {
        ApplicationClaim claim = claimApplication(root);
        EnterpriseLabEvidenceOwnershipLease lease = claim.lease();
        EnterpriseLabSupervisorAllocationBridge bridge = null;
        EnterpriseLabAllocationSupervisor supervisor = null;
        EnterpriseLabExperimentDurableEvidenceRepository durableEvidence = null;
        EnterpriseLabExperimentOperatorService operatorService = null;
        try {
            EnterpriseLabExperimentTargetCatalog targets =
                    EnterpriseLabSupervisorConfiguration.approvedTargets();
            bridge = EnterpriseLabSupervisorAllocationBridge.connect(
                    root, targets, lease.ownershipGate(), Clock.systemUTC());
            renewProofLease(lease, "allocation-supervisor startup");
            EnterpriseLabSupervisorAllocationBridge establishedBridge = bridge;
            EnterpriseLabAdaptiveDecision decision = new EnterpriseLabAdaptiveDecisionService()
                    .decide(
                            EnterpriseLabSupervisorConfiguration.SCENARIO_ID,
                            AdaptiveRoutingPolicyMode.ACTIVE_EXPERIMENT.wireValue(),
                            true,
                            false,
                            false);
            List<EnterpriseLabLoopbackTarget> loopbackTargets = targets
                    .findTargets(EnterpriseLabSupervisorConfiguration.SCENARIO_ID)
                    .orElseThrow();
            EnterpriseLabLoopbackAllocationRouter router =
                    EnterpriseLabLoopbackAllocationRouter.supervised(
                            loopbackTargets,
                            new EnterpriseLabLoopbackObservationIngress(
                                    loopbackTargets.stream()
                                            .map(EnterpriseLabLoopbackTarget::backendId)
                                            .collect(java.util.stream.Collectors.toSet())),
                            decision.decision().guardrailDecision().baselineAllocations(),
                            lease.ownershipGate(),
                            bridge);
            EnterpriseLabAllocationReconciliationGate allocationGate =
                    EnterpriseLabAllocationReconciliationGate.pending();
            EnterpriseLabAllocationTransactionCoordinator.FailureInjector injector =
                    checkpoint -> { };
            if (action == ChildAction.CRASH_AFTER_APPLY) {
                injector = checkpoint -> {
                    if (checkpoint
                            == EnterpriseLabAllocationTransactionCoordinator.Checkpoint
                            .AFTER_ROUTER_APPLY) {
                        out.println(event(claim, establishedBridge,
                                "CRASHING_AFTER_SUPERVISOR_APPLY", false).encode());
                        out.flush();
                        Runtime.getRuntime().halt(92);
                    }
                };
            }
            supervisor = EnterpriseLabAllocationSupervisor.createForProof(
                    root,
                    targets,
                    router,
                    lease.ownershipGate(),
                    allocationGate,
                    claim.replayedExperiments(),
                    injector);
            renewProofLease(lease, "operator-service startup");

            durableEvidence = new EnterpriseLabExperimentDurableEvidenceRepository(
                    claim.directory(), claim.recoveryGate(), Clock.systemUTC());
            operatorService = new EnterpriseLabExperimentOperatorService(
                    targets,
                    claim.recoveryGate(),
                    durableEvidence,
                    lease,
                    allocationGate,
                    supervisor,
                    bridge);

            if (action == ChildAction.HOLD_CANDIDATE
                    || action == ChildAction.CRASH_AFTER_APPLY
                    || action == ChildAction.APPLY_ONCE) {
                String experimentId = "independent-proof-experiment-"
                        + claim.generation();
                var armed = operatorService.arm(
                        new EnterpriseLabExperimentOperatorService.ArmRequest(
                                "independent-proof-arm-" + claim.generation(),
                                experimentId,
                                EnterpriseLabSupervisorConfiguration.SCENARIO_ID,
                                10,
                                Duration.ofSeconds(30),
                                2,
                                1,
                                Duration.ofSeconds(60)),
                        true);
                require(armed.status()
                                == EnterpriseLabExperimentOperatorService.OperatorStatus.APPLIED,
                        "proof experiment arm was denied: " + armed.reasonCode());
                var started = operatorService.start(
                        experimentId,
                        "independent-proof-start-" + claim.generation(),
                        true);
                require(started.status()
                                == EnterpriseLabExperimentOperatorService.OperatorStatus.APPLIED,
                        "proof experiment start was denied: " + started.reasonCode());
            }
            ChildEvidence ready = event(claim, bridge, "READY",
                    allocationGate.admissionAllowed());
            out.println(ready.encode());
            out.flush();

            if (action == ChildAction.HOLD_CANDIDATE
                    || action == ChildAction.HOLD_RECONCILE) {
                return holdApplication(
                        claim, bridge, supervisor, allocationGate, out);
            }
            return ready.ready() ? 0 : 1;
        } finally {
            if (operatorService != null) {
                operatorService.close();
            } else {
                if (supervisor != null) {
                    supervisor.close();
                }
                if (bridge != null) {
                    bridge.close();
                }
                if (durableEvidence != null) {
                    durableEvidence.close();
                }
                if (lease.operatingSystemLockValid()) {
                    lease.close();
                }
            }
        }
    }

    private int holdApplication(
            ApplicationClaim claim,
            EnterpriseLabSupervisorAllocationBridge bridge,
            EnterpriseLabAllocationSupervisor supervisor,
            EnterpriseLabAllocationReconciliationGate allocationGate,
            PrintStream out) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(
                System.in, StandardCharsets.UTF_8));
        Instant deadline = Instant.now().plus(HOLD_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            if (input.ready()) {
                String command = input.readLine();
                if ("check-loss".equals(command)) {
                    try {
                        supervisor.verify();
                    } catch (RuntimeException ignored) {
                        allocationGate.fail("SUPERVISOR_SESSION_UNAVAILABLE");
                    }
                    out.println(event(
                            claim,
                            bridge,
                            "SUPERVISOR_SESSION_UNAVAILABLE",
                            allocationGate.admissionAllowed()).encode());
                    out.flush();
                } else if ("reconnect".equals(command)) {
                    bridge.reconnect();
                    supervisor.verify();
                    out.println(event(
                            claim,
                            bridge,
                            "SUPERVISOR_RECONNECTED",
                            allocationGate.admissionAllowed()).encode());
                    out.flush();
                } else if ("release".equals(command)) {
                    out.println(event(
                            claim,
                            bridge,
                            "RELEASED",
                            allocationGate.admissionAllowed()).encode());
                    out.flush();
                    return 0;
                }
            }
            sleep(Duration.ofMillis(20));
        }
        throw new IllegalStateException("proof application hold exceeded its bound");
    }

    private int runFailureSupervisor(
            Path root, String failureName, PrintStream out) {
        EnterpriseLabSupervisorService.FailurePoint selected = failurePoint(failureName);
        java.util.concurrent.atomic.AtomicInteger selectedOccurrences =
                new java.util.concurrent.atomic.AtomicInteger();
        EnterpriseLabExperimentTargetCatalog targets =
                EnterpriseLabSupervisorConfiguration.approvedTargets();
        Clock clock = Clock.systemUTC();
        try (EnterpriseLabSupervisorOwnership ownership =
                     EnterpriseLabSupervisorOwnership.acquire(root)) {
            EnterpriseLabSupervisorService service =
                    EnterpriseLabSupervisorService.startForProof(
                            ownership,
                            targets,
                            clock,
                            point -> {
                                if (point == selected
                                        && selectedOccurrences.incrementAndGet() == 2) {
                                    out.println(new ChildEvidence(
                                            true,
                                            "NONE",
                                            EnterpriseLabSupervisorProtocol.NONE,
                                            EnterpriseLabSupervisorProtocol.NONE,
                                            0L,
                                            "proof-supervisor",
                                            1L,
                                            EnterpriseLabAllocationState.NO_FINGERPRINT,
                                            EnterpriseLabAllocationState.NO_FINGERPRINT,
                                            "NONE",
                                            "CRASHING_" + failureName
                                                    .toUpperCase(Locale.ROOT)
                                                    .replace('-', '_')).encode());
                                    out.flush();
                                    Runtime.getRuntime().halt(93);
                                }
                            });
            try (EnterpriseLabSupervisorServer server =
                         new EnterpriseLabSupervisorServer(
                                 ownership, service, targets, clock, 0)) {
                server.run();
            }
        }
        return 1;
    }

    private static ApplicationClaim claimApplication(Path root) {
        Clock clock = Clock.systemUTC();
        EnterpriseLabExperimentRecoveryGate recoveryGate =
                EnterpriseLabExperimentRecoveryGate.pending();
        AcquisitionAttempt acquisition = EnterpriseLabEvidenceOwnershipManager.acquire(
                root, PROOF_POLICY, clock);
        EnterpriseLabEvidenceOwnershipLease lease;
        EnterpriseLabExperimentJournalDirectory directory;
        if (acquisition.result().status() == OperationStatus.SUCCEEDED) {
            lease = acquisition.ownership().orElseThrow();
            directory = EnterpriseLabExperimentJournalDirectory.create(
                    root, lease.ownershipGate());
            new EnterpriseLabExperimentStartupReconciler(
                    directory,
                    new EnterpriseLabProcessLocalAllocationRecovery(
                            EnterpriseLabSupervisorConfiguration.approvedTargets()),
                    recoveryGate,
                    clock).initialize();
        } else {
            if (acquisition.result().failure()
                    != FailureClassification.TAKEOVER_NOT_PERMITTED) {
                throw new IllegalStateException(
                        "application proof ownership was denied: "
                                + acquisition.result().failure());
            }
            TakeoverAttempt takeover = EnterpriseLabEvidenceOwnershipManager.takeover(
                    root,
                    PROOF_POLICY,
                    clock,
                    new EnterpriseLabExperimentStartupReconciler(
                            EnterpriseLabExperimentJournalDirectory.create(root),
                            new EnterpriseLabProcessLocalAllocationRecovery(
                                    EnterpriseLabSupervisorConfiguration.approvedTargets()),
                            recoveryGate,
                            clock));
            if (takeover.result().status() != OperationStatus.SUCCEEDED) {
                throw new IllegalStateException(
                        "application proof takeover was denied: "
                                + takeover.result().failure());
            }
            lease = takeover.ownership().orElseThrow();
            directory = EnterpriseLabExperimentJournalDirectory.create(
                    root, lease.ownershipGate());
        }
        lease.completeApplicationReconciliation(recoveryGate);
        List<EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState>
                replayedExperiments = directory.discover().stream()
                .filter(discovery -> discovery.experimentId().isPresent())
                .map(discovery -> directory.replay(
                        discovery.experimentId().orElseThrow()))
                .filter(replay -> replay.reconstructedState().isPresent())
                .map(replay -> replay.reconstructedState().orElseThrow())
                .toList();
        var record = lease.record();
        return new ApplicationClaim(
                lease,
                directory,
                recoveryGate,
                replayedExperiments,
                record.owner().applicationInstanceId(),
                record.recordFingerprint(),
                record.generation());
    }

    private static void renewProofLease(
            EnterpriseLabEvidenceOwnershipLease lease,
            String phase) {
        var renewal = Objects.requireNonNull(
                        lease, "lease cannot be null")
                .ownershipGate()
                .renew();
        require(renewal.status() == OperationStatus.SUCCEEDED,
                "proof application ownership renewal failed before "
                        + phase + ": " + renewal.reasonCode());
    }

    private static ChildEvidence event(
            ApplicationClaim claim,
            EnterpriseLabSupervisorAllocationBridge bridge,
            String reasonCode,
            boolean ready) {
        EnterpriseLabInstalledAllocationSnapshot installed = bridge.read();
        EnterpriseLabLoopbackAllocationSnapshot baseline =
                EnterpriseLabLoopbackAllocationSnapshot.normalized(
                        EnterpriseLabSupervisorConfiguration.SCENARIO_ID,
                        0L,
                        "independent-proof-baseline",
                        EnterpriseLabLoopbackAllocationSnapshot.Kind.BASELINE,
                        EnterpriseLabSupervisorConfiguration.safeBaselineAllocation().keySet(),
                        EnterpriseLabSupervisorConfiguration.safeBaselineAllocation());
        return new ChildEvidence(
                ready,
                claim.applicationInstanceId(),
                claim.ownershipFingerprint(),
                EnterpriseLabSupervisorProtocol.NONE,
                claim.generation(),
                bridge.connectionMetadata().supervisorInstanceId(),
                bridge.connectionMetadata().supervisorGeneration(),
                installed.allocationFingerprint(),
                EnterpriseLabAllocationStateCodec.canonicalAllocationFingerprint(
                        baseline.scenarioId(), baseline.allocations()),
                installed.routingSnapshot().kind().name(),
                reasonCode);
    }

    private static Request observation(
            EnterpriseLabSupervisorProtocolCodec codec,
            EnterpriseLabSupervisorConnectionMetadata metadata,
            String requestId,
            Instant requestedAt) {
        return codec.issue(new RequestDraft(
                requestId,
                CommandType.READ_INSTALLED_ALLOCATION,
                "independent-proof-observer",
                EnterpriseLabSupervisorProtocol.NONE,
                0L,
                metadata.supervisorInstanceId(),
                metadata.supervisorGeneration(),
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                requestedAt,
                Map.of()));
    }

    private static void metadataWithAddress(
            EnterpriseLabSupervisorConnectionMetadata metadata, String address) {
        new EnterpriseLabSupervisorConnectionMetadata(
                metadata.schemaVersion(),
                address,
                metadata.port(),
                metadata.supervisorInstanceId(),
                metadata.supervisorGeneration(),
                metadata.durableStateGeneration(),
                metadata.stateFingerprint(),
                metadata.publishedAt());
    }

    private static boolean malformed(
            EnterpriseLabSupervisorConnectionMetadata metadata,
            byte[] credential,
            byte[] request) throws IOException {
        return rawExchange(
                metadata, credential, request, credential.length, request.length).status()
                == EnterpriseLabSupervisorServer.TRANSPORT_MALFORMED;
    }

    private static RawReply rawExchange(
            EnterpriseLabSupervisorConnectionMetadata metadata,
            byte[] credential,
            byte[] request,
            int declaredCredentialLength,
            int declaredRequestLength) throws IOException {
        try (Socket socket = new Socket(Proxy.NO_PROXY)) {
            socket.connect(new InetSocketAddress(
                            EnterpriseLabSupervisorConfiguration.literalLoopbackAddress(),
                            metadata.port()),
                    Math.toIntExact(EnterpriseLabSupervisorConfiguration
                            .CLIENT_CONNECT_TIMEOUT.toMillis()));
            socket.setSoTimeout(Math.toIntExact(
                    EnterpriseLabSupervisorConfiguration.CLIENT_RESPONSE_IDLE_TIMEOUT
                            .toMillis()));
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            output.writeInt(EnterpriseLabSupervisorServer.FRAME_MAGIC);
            output.writeByte(EnterpriseLabSupervisorServer.FRAME_VERSION);
            output.writeShort(declaredCredentialLength);
            output.writeInt(declaredRequestLength);
            output.write(credential);
            output.write(request);
            output.flush();
            DataInputStream input = new DataInputStream(socket.getInputStream());
            require(input.readInt() == EnterpriseLabSupervisorServer.FRAME_MAGIC,
                    "raw proof response magic differed");
            require(input.readUnsignedByte()
                            == EnterpriseLabSupervisorServer.FRAME_VERSION,
                    "raw proof response version differed");
            int status = input.readUnsignedByte();
            int length = input.readInt();
            require(length >= 0
                            && length <= EnterpriseLabSupervisorProtocol.HARD_MAX_RESPONSE_BYTES,
                    "raw proof response exceeded its bound");
            byte[] body = input.readNBytes(length);
            if (status == EnterpriseLabSupervisorServer.TRANSPORT_OK) {
                require(input.readUnsignedByte()
                                == EnterpriseLabSupervisorServer.TRANSPORT_DELIVERY_RECORDED,
                        "raw proof response lacked durable delivery evidence");
            }
            return new RawReply(status, body);
        }
    }

    private static boolean excessiveConcurrencyRejected(
            EnterpriseLabSupervisorConnectionMetadata metadata) {
        List<Socket> sockets = new ArrayList<>();
        try {
            int attempts = EnterpriseLabSupervisorConfiguration.MAX_CONCURRENT_CONNECTIONS
                    + EnterpriseLabSupervisorConfiguration.MAX_QUEUED_CONNECTIONS + 8;
            for (int index = 0; index < attempts; index++) {
                Socket socket = new Socket(Proxy.NO_PROXY);
                socket.connect(new InetSocketAddress(
                                EnterpriseLabSupervisorConfiguration.literalLoopbackAddress(),
                                metadata.port()),
                        500);
                socket.setSoTimeout(100);
                sockets.add(socket);
            }
            sleep(Duration.ofMillis(300));
            int closed = 0;
            for (Socket socket : sockets) {
                try {
                    if (socket.getInputStream().read() < 0) {
                        closed++;
                    }
                } catch (java.net.SocketTimeoutException ignored) {
                    // Active and queued bounded connections remain open briefly.
                } catch (IOException exception) {
                    closed++;
                }
            }
            return closed > 0;
        } catch (IOException exception) {
            return true;
        } finally {
            sockets.forEach(socket -> {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // Closing proof-only sockets is best effort after the assertion.
                }
            });
        }
    }

    private static byte[] replace(byte[] source, String before, String after) {
        String text = new String(source, StandardCharsets.UTF_8);
        if (!text.contains(before)) {
            throw new IllegalStateException("proof protocol replacement marker was absent");
        }
        return text.replace(before, after).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] replaceFirstFingerprint(byte[] source) {
        String text = new String(source, StandardCharsets.UTF_8);
        int marker = text.indexOf("\"requestFingerprint\":\"");
        require(marker >= 0, "request fingerprint marker was absent");
        int start = marker + "\"requestFingerprint\":\"".length();
        char replacement = text.charAt(start) == '0' ? '1' : '0';
        return (text.substring(0, start) + replacement + text.substring(start + 1))
                .getBytes(StandardCharsets.UTF_8);
    }

    private static boolean rejects(CheckedAction action) {
        try {
            action.run();
            return false;
        } catch (RuntimeException | IOException exception) {
            return true;
        }
    }

    private static EnterpriseLabSupervisorService.FailurePoint failurePoint(
            String window) {
        return switch (window) {
            case "before-intent" ->
                    EnterpriseLabSupervisorService.FailurePoint.BEFORE_INTENT_INSTALL;
            case "after-intent" ->
                    EnterpriseLabSupervisorService.FailurePoint.AFTER_INTENT_INSTALL;
            case "before-apply" ->
                    EnterpriseLabSupervisorService.FailurePoint.BEFORE_APPLY_INSTALL;
            case "after-apply" ->
                    EnterpriseLabSupervisorService.FailurePoint.AFTER_APPLY_INSTALL;
            case "before-readback" ->
                    EnterpriseLabSupervisorService.FailurePoint.BEFORE_READ_BACK;
            case "after-readback" ->
                    EnterpriseLabSupervisorService.FailurePoint.AFTER_READ_BACK;
            case "before-commit" ->
                    EnterpriseLabSupervisorService.FailurePoint.BEFORE_COMMIT_INSTALL;
            case "after-commit-before-response" ->
                    EnterpriseLabSupervisorService.FailurePoint.AFTER_COMMIT_INSTALL;
            default -> throw new IllegalArgumentException(
                    "unsupported supervisor crash window");
        };
    }

    private static ManagedProcess startNormalSupervisor(
            Path root, ProcessCounts counts) throws IOException {
        List<String> command = javaCommand();
        command.add("--enterprise-lab-supervisor");
        command.add("--enterprise-lab-supervisor-data-directory=" + root);
        counts.supervisors++;
        return start(command);
    }

    private static ManagedProcess startChild(
            Path output,
            String token,
            String proofCase,
            ChildAction action,
            Optional<String> failure,
            ProcessCounts counts) throws IOException {
        List<String> command = javaCommand();
        command.add("--enterprise-lab-independent-supervisor-proof-child="
                + action.wireValue);
        command.add("--enterprise-lab-independent-supervisor-proof-output=" + output);
        command.add("--enterprise-lab-independent-supervisor-proof-run=" + token);
        command.add("--enterprise-lab-independent-supervisor-proof-case=" + proofCase);
        failure.ifPresent(value -> command.add(
                "--enterprise-lab-independent-supervisor-proof-failure=" + value));
        if (action == ChildAction.SUPERVISOR_FAILURE) {
            counts.supervisors++;
        } else {
            counts.applications++;
        }
        return start(command);
    }

    private static ChildEvidence runOneShot(
            Path output,
            String token,
            String proofCase,
            ChildAction action,
            Optional<String> failure,
            ProcessCounts counts) throws IOException {
        ManagedProcess process = startChild(
                output, token, proofCase, action, failure, counts);
        try {
            ChildEvidence evidence = process.awaitEvent(START_TIMEOUT);
            process.awaitExit(EXIT_TIMEOUT);
            require(process.exitCode() == 0,
                    "proof child exited unsuccessfully: " + process.output());
            return evidence;
        } finally {
            process.closeIfAlive();
        }
    }

    private static List<String> javaCommand() {
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
        return command;
    }

    private static ManagedProcess start(List<String> command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(Path.of("").toAbsolutePath().normalize().toFile())
                .redirectErrorStream(true);
        return new ManagedProcess(builder.start());
    }

    private static EnterpriseLabSupervisorConnectionMetadata awaitSupervisorReady(
            Path root, ManagedProcess process) {
        return awaitAnySupervisorReady(root, List.of(process));
    }

    private static EnterpriseLabSupervisorConnectionMetadata awaitAnySupervisorReady(
            Path root, List<ManagedProcess> processes) {
        Instant deadline = Instant.now().plus(START_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            try {
                if (EnterpriseLabSupervisorServer.readReadyPortForTesting(root)
                        .isPresent()) {
                    try (EnterpriseLabSupervisorClient client =
                                 EnterpriseLabSupervisorClient.connect(
                                         root,
                                         EnterpriseLabSupervisorConfiguration
                                                 .approvedTargets(),
                                         Clock.systemUTC())) {
                        return client.connectionMetadata();
                    }
                }
            } catch (IOException | RuntimeException ignored) {
                // Atomic readiness publication may be between bounded reads.
            }
            if (processes.stream().noneMatch(ManagedProcess::isAlive)) {
                throw new IllegalStateException(
                        "supervisor exited before readiness: "
                                + processes.stream().map(ManagedProcess::output).toList());
            }
            sleep(Duration.ofMillis(20));
        }
        throw new IllegalStateException("supervisor readiness exceeded its bound");
    }

    private static void awaitOneLoser(
            ManagedProcess first, ManagedProcess second) {
        Instant deadline = Instant.now().plus(EXIT_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            if (first.isAlive() != second.isAlive()) {
                return;
            }
            sleep(Duration.ofMillis(20));
        }
        throw new IllegalStateException(
                "competing supervisor processes did not converge to one owner");
    }

    private static Path prepareRoot(
            Path output, String token, String proofCase) throws IOException {
        Path root = controlledRoot(output, token, proofCase);
        Files.createDirectories(root);
        return root;
    }

    private static Path controlledRoot(
            Path output, String token, String proofCase) {
        Path safeOutput = output.toAbsolutePath().normalize();
        Path root = safeOutput.resolve("independent-supervisor-runs")
                .resolve(token).resolve(proofCase).normalize();
        if (!root.startsWith(safeOutput)) {
            throw new IllegalArgumentException(
                    "independent supervisor proof root escaped the target boundary");
        }
        return root;
    }

    private static void awaitStaleBoundary() {
        sleep(STALE_WAIT);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("proof wait was interrupted", exception);
        }
    }

    private static boolean isBaseline(String kind) {
        return "BASELINE".equals(kind) || "RESTORED_BASELINE".equals(kind);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName() : message;
    }

    private static void close(ManagedProcess process) {
        if (process != null) {
            process.closeIfAlive();
        }
    }

    private enum ChildAction {
        HOLD_CANDIDATE("hold-candidate"),
        HOLD_RECONCILE("hold-reconcile"),
        CRASH_AFTER_APPLY("crash-after-apply"),
        APPLY_ONCE("apply-once"),
        RECONCILE_ONCE("reconcile-once"),
        SUPERVISOR_FAILURE("supervisor-failure");

        private final String wireValue;

        ChildAction(String wireValue) {
            this.wireValue = wireValue;
        }

        private static ChildAction parse(String value) {
            for (ChildAction action : values()) {
                if (action.wireValue.equals(value)) {
                    return action;
                }
            }
            throw new IllegalArgumentException(
                    "unsupported independent supervisor proof child action");
        }
    }

    private record ApplicationClaim(
            EnterpriseLabEvidenceOwnershipLease lease,
            EnterpriseLabExperimentJournalDirectory directory,
            EnterpriseLabExperimentRecoveryGate recoveryGate,
            List<EnterpriseLabExperimentJournalReplayEngine.ReconstructedExperimentState>
                    replayedExperiments,
            String applicationInstanceId,
            String ownershipFingerprint,
            long generation) {
    }

    private record ChildEvidence(
            boolean ready,
            String applicationInstanceId,
            String applicationOwnershipFingerprint,
            String reserved,
            long applicationGeneration,
            String supervisorInstanceId,
            long supervisorGeneration,
            String installedFingerprint,
            String baselineFingerprint,
            String installedKind,
            String reasonCode) {
        private String encode() {
            return EVENT_PREFIX + String.join("|",
                    Boolean.toString(ready),
                    applicationInstanceId,
                    applicationOwnershipFingerprint,
                    reserved,
                    Long.toString(applicationGeneration),
                    supervisorInstanceId,
                    Long.toString(supervisorGeneration),
                    installedFingerprint,
                    baselineFingerprint,
                    installedKind,
                    reasonCode);
        }

        private static ChildEvidence decode(String line) {
            if (line == null || !line.startsWith(EVENT_PREFIX)) {
                throw new IllegalArgumentException("independent supervisor proof event is absent");
            }
            String[] values = line.substring(EVENT_PREFIX.length()).split("\\|", -1);
            if (values.length != 11) {
                throw new IllegalArgumentException(
                        "independent supervisor proof event has an invalid field count");
            }
            return new ChildEvidence(
                    Boolean.parseBoolean(values[0]),
                    values[1],
                    values[2],
                    values[3],
                    Long.parseLong(values[4]),
                    values[5],
                    Long.parseLong(values[6]),
                    values[7],
                    values[8],
                    values[9],
                    values[10]);
        }
    }

    private record SurvivalEvidence(
            boolean installedStateSurvived,
            boolean staleApplicationRejected,
            boolean supervisorRestartReconciled,
            long firstApplicationGeneration,
            long recoveredApplicationGeneration,
            long firstSupervisorGeneration,
            long recoveredSupervisorGeneration,
            String candidateFingerprint,
            String baselineFingerprint) {
    }

    private record RawReply(int status, byte[] body) {
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws IOException;
    }

    private static final class ProcessCounts {
        private int applications;
        private int supervisors;
    }

    private static final class ManagedProcess {
        private final Process process;
        private final BufferedWriter input;
        private final ArrayBlockingQueue<String> events = new ArrayBlockingQueue<>(8);
        private final StringBuilder boundedOutput = new StringBuilder();

        private ManagedProcess(Process process) {
            this.process = process;
            this.input = new BufferedWriter(new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8));
            Thread reader = new Thread(
                    this::readOutput,
                    "enterprise-lab-independent-supervisor-proof-reader");
            reader.setDaemon(true);
            reader.start();
        }

        private void readOutput() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (boundedOutput) {
                        if (boundedOutput.length() < 32_768) {
                            boundedOutput.append(line).append(System.lineSeparator());
                        }
                    }
                    if (line.startsWith(EVENT_PREFIX)) {
                        events.offer(line);
                    }
                }
            } catch (IOException ignored) {
                // Process exit or forced termination closes the bounded stream.
            }
        }

        private ChildEvidence awaitEvent(Duration timeout) {
            try {
                String line = events.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (line == null) {
                    throw new IllegalStateException(
                            "proof child did not report within its bound: " + output());
                }
                return ChildEvidence.decode(line);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("proof child event wait was interrupted", exception);
            }
        }

        private ChildEvidence command(String command, Duration timeout) throws IOException {
            input.write(command);
            input.newLine();
            input.flush();
            return awaitEvent(timeout);
        }

        private void awaitExit(Duration timeout) {
            try {
                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new IllegalStateException(
                            "proof child did not exit within its bound: " + output());
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("proof child exit wait was interrupted", exception);
            }
        }

        private int exitCode() {
            return process.exitValue();
        }

        private boolean isAlive() {
            return process.isAlive();
        }

        private void killAbruptly() {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            awaitExit(EXIT_TIMEOUT);
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
}
