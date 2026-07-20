package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReconciliationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.InstalledStateMutation;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandDispatcher.DispatchEvidence;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandDispatcher.VerifiedDispatchResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.RequestDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseStatus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Application-side installed-state holder backed only by the authenticated
 * independently running loopback supervisor. There is deliberately no local
 * mutation fallback in this class.
 *
 * <p>Mutation lock order is: application allocation coordinator, live
 * application-ownership verification, router mutation monitor, this bridge's
 * mutation monitor, bounded supervisor IPC, supervisor ownership verification,
 * supervisor atomic install and durable commit, then application durable
 * commit. The supervisor never calls back into the application. Normal route
 * selection reads only {@link #cached}; it takes no bridge monitor and performs
 * no IPC or filesystem operation.</p>
 */
public final class EnterpriseLabSupervisorAllocationBridge
        implements EnterpriseLabLoopbackAllocationRouter.InstalledStateStore,
        AutoCloseable {
    private static final int MAX_PENDING_APPLICATION_COMMITS = 8;
    private static final int MAX_SHARED_APPLICATION_LEDGERS = 8;
    private static final Object APPLICATION_LEDGER_REGISTRY_MUTEX = new Object();
    private static final Map<Path, SharedApplicationLedger> APPLICATION_LEDGERS =
            new LinkedHashMap<>();

    private final Path trustedRoot;
    private final EnterpriseLabExperimentTargetCatalog targetCatalog;
    private final EnterpriseLabSupervisorProtocolCodec codec;
    private final EnterpriseLabEvidenceOwnershipGate ownershipGate;
    private final Clock clock;
    private final SharedApplicationLedger sharedApplicationLedger;
    private final EnterpriseLabApplicationCommandLedger applicationCommandLedger;
    private final EnterpriseLabApplicationCommandDispatcher applicationDispatcher;
    private final Map<String, PendingApplicationCommit> pendingApplicationCommits =
            new LinkedHashMap<>();
    private final AtomicReference<EnterpriseLabInstalledAllocationSnapshot> cached =
            new AtomicReference<>();

    private EnterpriseLabSupervisorClient client;
    private EnterpriseLabSupervisorConnectionMetadata supervisor;
    private String acceptedOwnershipFingerprint = EnterpriseLabSupervisorProtocol.NONE;
    private long acceptedApplicationGeneration;
    private Optional<Instant> lastSuccessfulIpcVerification = Optional.empty();
    private String lastFailureReasonCode = "NONE";
    private boolean reachable;
    private volatile boolean closed;

    private EnterpriseLabSupervisorAllocationBridge(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabSupervisorClient client,
            EnterpriseLabSupervisorProtocolCodec codec,
            EnterpriseLabEvidenceOwnershipGate ownershipGate,
            Clock clock,
            SharedApplicationLedger sharedApplicationLedger) {
        this.trustedRoot = Objects.requireNonNull(
                trustedRoot, "trustedRoot cannot be null");
        this.targetCatalog = Objects.requireNonNull(
                targetCatalog, "targetCatalog cannot be null");
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.codec = Objects.requireNonNull(codec, "codec cannot be null");
        this.ownershipGate = Objects.requireNonNull(
                ownershipGate, "ownershipGate cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.sharedApplicationLedger = Objects.requireNonNull(
                sharedApplicationLedger, "sharedApplicationLedger cannot be null");
        this.applicationCommandLedger = this.sharedApplicationLedger.ledger;
        this.applicationDispatcher = new EnterpriseLabApplicationCommandDispatcher(
                this.applicationCommandLedger, this.clock);
        this.supervisor = client.connectionMetadata();
    }

    /** Connects, pins one supervisor epoch, and completes the verified handoff. */
    public static EnterpriseLabSupervisorAllocationBridge connect(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabEvidenceOwnershipGate ownershipGate,
            Clock clock) {
        EnterpriseLabExperimentTargetCatalog safeCatalog = Objects.requireNonNull(
                targetCatalog, "targetCatalog cannot be null");
        EnterpriseLabEvidenceOwnershipGate safeGate = Objects.requireNonNull(
                ownershipGate, "ownershipGate cannot be null");
        Clock safeClock = Objects.requireNonNull(clock, "clock cannot be null");
        SharedApplicationLedger applicationLedger = acquireApplicationLedger(
                trustedRoot, safeGate);
        EnterpriseLabSupervisorClient client = null;
        boolean transferred = false;
        try {
            client = EnterpriseLabSupervisorClient.connect(
                    trustedRoot, safeCatalog, safeClock);
            EnterpriseLabSupervisorAllocationBridge bridge =
                    new EnterpriseLabSupervisorAllocationBridge(
                            trustedRoot,
                            safeCatalog,
                            client,
                            new EnterpriseLabSupervisorProtocolCodec(safeCatalog),
                            safeGate,
                            safeClock,
                            applicationLedger);
            bridge.readAuthoritative();
            transferred = true;
            return bridge;
        } finally {
            if (!transferred) {
                if (client != null) {
                    client.close();
                }
                releaseApplicationLedger(applicationLedger);
            }
        }
    }

    @Override
    public EnterpriseLabInstalledAllocationSnapshot read() {
        requireOpen();
        return Objects.requireNonNull(
                cached.get(),
                "external supervisor selection cache is not initialized");
    }

    /** Authenticated supervisor IPC read used only for reconciliation and verification. */
    @Override
    public synchronized EnterpriseLabInstalledAllocationSnapshot readAuthoritative() {
        requireOpen();
        Response response = ownershipGate.withCurrentOwnership(ownership -> {
            ensureOwnershipAccepted(ownership);
            Request request = issue(
                    CommandType.READ_INSTALLED_ALLOCATION,
                    ownership,
                    EnterpriseLabSupervisorProtocol.NONE,
                    Optional.empty(),
                    AllocationPurpose.RECONCILIATION_NO_OP,
                    Optional.empty(),
                    EnterpriseLabSupervisorProtocol.NONE,
                    EnterpriseLabSupervisorProtocol.NONE,
                    0L,
                    requestId("read"));
            Response observed = execute(request);
            requireSameOwnership(ownership);
            return observed;
        });
        EnterpriseLabInstalledAllocationSnapshot installed =
                response.installedAllocation().orElseThrow(
                () -> failure("supervisor accepted an installed-state read without state"));
        cached.set(installed);
        return installed;
    }

    /**
     * Performs one bounded health exchange against the already-pinned process
     * epoch. It never reads or changes candidate-selection state.
     */
    public synchronized EnterpriseLabSupervisorConnectionMetadata verifySession() {
        requireOpen();
        ownershipGate.withCurrentOwnership(ownership -> {
            ensureOwnershipAccepted(ownership);
            Request request = issue(
                    CommandType.HEALTH,
                    ownership,
                    EnterpriseLabSupervisorProtocol.NONE,
                    Optional.empty(),
                    AllocationPurpose.RECONCILIATION_NO_OP,
                    Optional.empty(),
                    EnterpriseLabSupervisorProtocol.NONE,
                    EnterpriseLabSupervisorProtocol.NONE,
                    0L,
                    requestId("health"));
            execute(request);
            requireSameOwnership(ownership);
            return ownership;
        });
        return supervisor;
    }

    /**
     * Explicitly replaces a failed client only after a different, higher
     * supervisor epoch authenticates, accepts the still-current application
     * owner, and returns readable installed state. Allocation reconciliation
     * remains the caller's responsibility and no candidate is resumed here.
     */
    public synchronized EnterpriseLabSupervisorConnectionMetadata reconnect() {
        requireOpen();
        OwnershipRecord ownership = currentOwnership();
        EnterpriseLabSupervisorConnectionMetadata previous = supervisor;
        EnterpriseLabSupervisorClient replacement =
                EnterpriseLabSupervisorClient.connect(
                        trustedRoot, targetCatalog, clock);
        EnterpriseLabSupervisorConnectionMetadata next =
                replacement.connectionMetadata();
        if (next.supervisorGeneration() <= previous.supervisorGeneration()
                || next.supervisorInstanceId().equals(
                        previous.supervisorInstanceId())) {
            replacement.close();
            throw failure(
                    "explicit supervisor reconnection requires a different higher process epoch");
        }

        EnterpriseLabSupervisorClient staleClient = client;
        String staleAcceptedOwnership = acceptedOwnershipFingerprint;
        client = replacement;
        supervisor = next;
        acceptedOwnershipFingerprint = EnterpriseLabSupervisorProtocol.NONE;
        try {
            readAuthoritative();
            requireSameOwnership(ownership);
            staleClient.close();
            lastFailureReasonCode = "NONE";
            return next;
        } catch (RuntimeException exception) {
            replacement.close();
            client = staleClient;
            supervisor = previous;
            acceptedOwnershipFingerprint = staleAcceptedOwnership;
            recordFailure("SUPERVISOR_RECONNECT_FAILED");
            throw exception;
        }
    }

    /** Direct router mutation is forbidden because it lacks application intent evidence. */
    @Override
    public boolean compareAndSet(
            EnterpriseLabInstalledAllocationSnapshot expected,
            EnterpriseLabInstalledAllocationSnapshot update) {
        throw failure(
                "external supervisor mutation requires a durable application transaction context");
    }

    @Override
    public synchronized boolean compareAndSet(
            EnterpriseLabInstalledAllocationSnapshot expected,
            EnterpriseLabInstalledAllocationSnapshot update,
            InstalledStateMutation mutation) {
        requireOpen();
        EnterpriseLabInstalledAllocationSnapshot safeExpected = Objects.requireNonNull(
                expected, "expected cannot be null");
        EnterpriseLabInstalledAllocationSnapshot safeUpdate = Objects.requireNonNull(
                update, "update cannot be null");
        InstalledStateMutation safeMutation = Objects.requireNonNull(
                mutation, "mutation cannot be null");
        if (!safeMutation.durableIntentPersisted()) {
            throw failure(
                    "external supervisor mutation requires durable intent before IPC");
        }
        if (safeMutation.allocationGeneration() < 1L) {
            throw failure("application allocation generation must be positive");
        }

        OwnershipRecord observedOwnership = currentOwnership();
        EnterpriseLabInstalledAllocationSnapshot observed = readAuthoritative();
        if (!observed.equals(safeExpected)) {
            return false;
        }
        requireSameOwnershipEpoch(observedOwnership);

        boolean candidate = safeUpdate.routingSnapshot().kind()
                == EnterpriseLabLoopbackAllocationSnapshot.Kind.CANDIDATE;
        CommandType command = candidate
                ? CommandType.APPLY_ALLOCATION : CommandType.RESTORE_BASELINE;
        AllocationPurpose purpose = candidate
                ? requireCandidatePurpose(safeMutation.purpose())
                : restorationPurpose(safeMutation.purpose());
        Optional<EnterpriseLabLoopbackAllocationSnapshot> allocation = candidate
                ? Optional.of(safeUpdate.routingSnapshot()) : Optional.empty();
        String transactionId = supervisorTransactionId(
                safeMutation.transactionId(),
                command,
                safeMutation.allocationGeneration(),
                safeUpdate.allocationFingerprint());
        DispatchEvidence dispatchEvidence = new DispatchEvidence(
                safeExpected.allocationFingerprint(),
                safeExpected.routerGeneration(),
                Map.of(
                        "boundary", "allocation-transaction",
                        "commitState", "deferred"));
        OwnedMutationDispatch ownedDispatch = ownershipGate.withCurrentOwnership(
                ownership -> {
                    if (!sameOwnershipEpoch(ownership, observedOwnership)) {
                        throw failure(
                                "application ownership epoch changed before supervisor mutation");
                    }
                    ensureOwnershipAccepted(ownership);
                    Request request = issue(
                            command,
                            ownership,
                            transactionId,
                            safeMutation.experimentId(),
                            purpose,
                            allocation,
                            safeUpdate.allocationFingerprint(),
                            safeExpected.allocationFingerprint(),
                            safeMutation.allocationGeneration(),
                            commandCorrelationId(
                                    safeMutation.transactionId(),
                                    command,
                                    safeMutation.allocationGeneration(),
                                    safeUpdate.allocationFingerprint()));
                    if (pendingApplicationCommits.containsKey(request.requestId())) {
                        throw failure(
                                "application command already awaits durable allocation commit");
                    }
                    if (pendingApplicationCommits.size()
                            >= MAX_PENDING_APPLICATION_COMMITS) {
                        throw failure("bounded pending application command limit reached");
                    }
                    VerifiedDispatchResult dispatched;
                    synchronized (sharedApplicationLedger.commandMutex) {
                        dispatched = applicationDispatcher.dispatchVerified(
                                request,
                                dispatchEvidence,
                                client::execute,
                                this::readSupervisorOutcome,
                                true);
                    }
                    return new OwnedMutationDispatch(ownership, request, dispatched);
                });
        OwnershipRecord ownership = ownedDispatch.ownership();
        Request request = ownedDispatch.request();
        VerifiedDispatchResult dispatched = ownedDispatch.dispatch();
        Response action = requireAccepted(dispatched.response(), request);
        if (pendingApplicationCommits.putIfAbsent(
                request.requestId(),
                new PendingApplicationCommit(safeMutation, safeUpdate, dispatched)) != null) {
            throw failure("application command already awaits durable allocation commit");
        }
        EnterpriseLabInstalledAllocationSnapshot acted = action.installedAllocation()
                .orElseThrow(() -> failure(
                        "supervisor accepted an allocation mutation without exact read-back"));
        requireRequestedState(safeUpdate, ownership, acted, action);
        cached.set(acted);
        requireSameOwnershipEpoch(ownership);

        EnterpriseLabInstalledAllocationSnapshot readBack = readAuthoritative();
        if (!readBack.equals(acted)) {
            throw failure(
                    "independent supervisor read-back differed from the action receipt");
        }
        requireRequestedState(safeUpdate, ownership, readBack, action);
        return true;
    }

    synchronized void commitVerifiedMutation(
            InstalledStateMutation mutation,
            EnterpriseLabInstalledAllocationSnapshot installed) {
        requireOpen();
        InstalledStateMutation safeMutation = Objects.requireNonNull(
                mutation, "mutation cannot be null");
        EnterpriseLabInstalledAllocationSnapshot safeInstalled = Objects.requireNonNull(
                installed, "installed cannot be null");
        CommandType command = safeInstalled.routingSnapshot().kind()
                == EnterpriseLabLoopbackAllocationSnapshot.Kind.CANDIDATE
                ? CommandType.APPLY_ALLOCATION : CommandType.RESTORE_BASELINE;
        String correlationId = commandCorrelationId(
                safeMutation.transactionId(),
                command,
                safeMutation.allocationGeneration(),
                safeInstalled.allocationFingerprint());
        PendingApplicationCommit pending = pendingApplicationCommits.get(correlationId);
        if (pending == null
                || !pending.mutation().equals(safeMutation)
                || !pending.requested().allocationFingerprint().equals(
                        safeInstalled.allocationFingerprint())
                || pending.requested().routerGeneration()
                        != safeInstalled.routerGeneration()
                || !pending.requested().routingSnapshot().scenarioId().equals(
                        safeInstalled.routingSnapshot().scenarioId())
                || !pending.requested().routingSnapshot().allocations().equals(
                        safeInstalled.routingSnapshot().allocations())
                || !pending.dispatch().response().installedFingerprint().equals(
                        safeInstalled.allocationFingerprint())
                || pending.dispatch().response().routerGeneration()
                        != safeInstalled.routerGeneration()
                || !pending.dispatch().response().installedAllocation()
                        .equals(Optional.of(safeInstalled))) {
            throw failure(
                    "durable allocation commit does not match the pending supervisor command");
        }
        applicationDispatcher.commit(pending.dispatch());
        pendingApplicationCommits.remove(correlationId);
    }

    public synchronized EnterpriseLabSupervisorConnectionMetadata connectionMetadata() {
        requireOpen();
        return supervisor;
    }

    /**
     * Returns only cached, sanitized session evidence. It performs no IPC and
     * exposes no address, port, credential, or controlled filesystem path.
     */
    public synchronized SessionSnapshot sessionSnapshot() {
        requireOpen();
        EnterpriseLabInstalledAllocationSnapshot installed = cached.get();
        boolean ready = reachable
                && installed != null
                && acceptedApplicationGeneration > 0L
                && !EnterpriseLabSupervisorProtocol.NONE.equals(
                        acceptedOwnershipFingerprint);
        return new SessionSnapshot(
                reachable,
                ready,
                supervisor.supervisorInstanceId(),
                supervisor.supervisorGeneration(),
                supervisor.durableStateGeneration(),
                acceptedApplicationGeneration,
                installed == null
                        ? EnterpriseLabAllocationState.NO_FINGERPRINT
                        : installed.allocationFingerprint(),
                installed == null ? 0L : installed.routerGeneration(),
                lastSuccessfulIpcVerification,
                lastFailureReasonCode);
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            try {
                client.close();
            } finally {
                releaseApplicationLedger(sharedApplicationLedger);
            }
        }
    }

    private OwnershipRecord currentOwnership() {
        OwnershipRecord ownership = ownershipGate.requireCurrentOwnership();
        if (ownership.reconciliationStatus() != ReconciliationStatus.SUCCEEDED) {
            throw failure(
                    "external supervisor handoff requires completed application reconciliation");
        }
        return ownership;
    }

    private void ensureOwnershipAccepted(OwnershipRecord ownership) {
        if (ownership.recordFingerprint().equals(acceptedOwnershipFingerprint)) {
            return;
        }
        Request request = issue(
                CommandType.ADVANCE_APPLICATION_OWNERSHIP,
                ownership,
                supervisorTransactionId(
                        ownership.recordFingerprint(),
                        CommandType.ADVANCE_APPLICATION_OWNERSHIP,
                        ownership.generation(),
                        ownership.recordFingerprint()),
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                ownership.generation(),
                requestId("handoff"));
        Response response = execute(request);
        if (response.observedApplicationGeneration() != ownership.generation()) {
            throw failure(
                    "supervisor handoff response did not accept the current application generation");
        }
        requireSameOwnership(ownership);
        acceptedOwnershipFingerprint = ownership.recordFingerprint();
        acceptedApplicationGeneration = ownership.generation();
    }

    private Response execute(Request request) {
        try {
            EnterpriseLabInstalledAllocationSnapshot installed = cached.get();
            DispatchEvidence evidence = new DispatchEvidence(
                    installed == null
                            ? EnterpriseLabCommandLedgerEvent.NONE
                            : installed.allocationFingerprint(),
                    installed == null ? 0L : installed.routerGeneration(),
                    Map.of("boundary", "supervisor-ipc"));
            VerifiedDispatchResult dispatched;
            synchronized (sharedApplicationLedger.commandMutex) {
                dispatched = applicationDispatcher.dispatchVerified(
                        request,
                        evidence,
                        client::execute,
                        this::readSupervisorOutcome,
                        false);
            }
            return requireAccepted(dispatched.response(), request);
        } catch (RuntimeException exception) {
            recordFailure("SUPERVISOR_IPC_VERIFICATION_FAILED");
            throw exception;
        }
    }

    private Request issue(
            CommandType command,
            OwnershipRecord ownership,
            String transactionId,
            Optional<String> experimentId,
            AllocationPurpose purpose,
            Optional<EnterpriseLabLoopbackAllocationSnapshot> allocation,
            String allocationFingerprint,
            String previousCommittedFingerprint,
            long allocationGeneration,
            String correlationId) {
        String safeCorrelationId = Objects.requireNonNull(
                correlationId, "correlationId cannot be null");
        return codec.issue(new RequestDraft(
                safeCorrelationId,
                command,
                ownership.owner().applicationInstanceId(),
                ownership.recordFingerprint(),
                ownership.generation(),
                supervisor.supervisorInstanceId(),
                supervisor.supervisorGeneration(),
                transactionId,
                experimentId,
                purpose,
                allocation,
                allocationFingerprint,
                previousCommittedFingerprint,
                requestedAt(safeCorrelationId),
                allocationGeneration > 0L
                        ? Map.of(
                                "applicationAllocationGeneration",
                                Long.toString(allocationGeneration),
                                "applicationTransactionBoundary",
                                "durable-intent-before-supervisor-ipc")
                        : Map.of()));
    }

    private Instant requestedAt(String correlationId) {
        return applicationCommandLedger.replay().eventsFor(correlationId).stream()
                .findFirst()
                .map(EnterpriseLabCommandLedgerEvent::occurredAt)
                .orElseGet(clock::instant);
    }

    private EnterpriseLabCommandLedgerEvent readSupervisorOutcome(
            Request request,
            Response response) {
        EnterpriseLabSupervisorCommandLedger inspection =
                EnterpriseLabSupervisorCommandLedger.inspect(trustedRoot);
        return inspection.replay().eventsFor(request.requestId()).stream()
                .filter(event -> event.correlates(request))
                .reduce((first, second) -> second)
                .orElseThrow(() -> failure(
                        "supervisor response lacks independently readable command evidence"));
    }

    private Response requireAccepted(Response response, Request request) {
        Response safe = Objects.requireNonNull(response, "response cannot be null");
        if (!safe.validatesAgainst(request)
                || !safe.supervisorInstanceId().equals(supervisor.supervisorInstanceId())
                || safe.supervisorGeneration() != supervisor.supervisorGeneration()) {
            throw failure("supervisor response changed the pinned request or process epoch");
        }
        if (safe.status() != ResponseStatus.ACCEPTED) {
            throw failure("supervisor rejected the fenced request: " + safe.reasonCode());
        }
        reachable = true;
        lastSuccessfulIpcVerification = Optional.of(clock.instant());
        lastFailureReasonCode = "NONE";
        return safe;
    }

    private void recordFailure(String reasonCode) {
        reachable = false;
        lastFailureReasonCode = reasonCode;
    }

    private void requireSameOwnership(OwnershipRecord expected) {
        OwnershipRecord current = currentOwnership();
        if (!sameOwnershipEpoch(current, expected)
                || !current.recordFingerprint().equals(expected.recordFingerprint())) {
            throw failure(
                    "application ownership changed during supervisor transaction");
        }
    }

    private void requireSameOwnershipEpoch(OwnershipRecord expected) {
        OwnershipRecord current = currentOwnership();
        if (!sameOwnershipEpoch(current, expected)) {
            throw failure(
                    "application ownership epoch changed during supervisor transaction");
        }
    }

    private static boolean sameOwnershipEpoch(
            OwnershipRecord current,
            OwnershipRecord expected) {
        return current.owner().ownerId().equals(expected.owner().ownerId())
                && current.owner().applicationInstanceId().equals(
                expected.owner().applicationInstanceId())
                && current.generation() == expected.generation();
    }

    private static void requireRequestedState(
            EnterpriseLabInstalledAllocationSnapshot requested,
            OwnershipRecord ownership,
            EnterpriseLabInstalledAllocationSnapshot installed,
            Response response) {
        boolean exactRouting = requested.routingSnapshot().kind()
                == EnterpriseLabLoopbackAllocationSnapshot.Kind.CANDIDATE
                ? installed.routingSnapshot().equals(requested.routingSnapshot())
                : installed.routingSnapshot().scenarioId().equals(
                        requested.routingSnapshot().scenarioId())
                && installed.routingSnapshot().allocations().equals(
                        requested.routingSnapshot().allocations())
                && installed.routingSnapshot().kind()
                        == EnterpriseLabLoopbackAllocationSnapshot.Kind.RESTORED_BASELINE
                && installed.routerGeneration() == requested.routerGeneration();
        if (!exactRouting
                || !installed.allocationFingerprint().equals(
                        requested.allocationFingerprint())
                || installed.routerGeneration() != requested.routerGeneration()
                || installed.ownerGeneration() != ownership.generation()
                || response.routerGeneration() != installed.routerGeneration()
                || !response.installedFingerprint().equals(
                        installed.allocationFingerprint())) {
            throw failure(
                    "supervisor receipt did not exactly match the requested allocation and generations");
        }
    }

    private static AllocationPurpose requireCandidatePurpose(AllocationPurpose purpose) {
        if (purpose != AllocationPurpose.EXPERIMENT_CANDIDATE
                && purpose != AllocationPurpose.EXPERIMENT_HOLD) {
            throw failure("candidate mutation requires an experiment allocation purpose");
        }
        return purpose;
    }

    private static AllocationPurpose restorationPurpose(AllocationPurpose purpose) {
        return switch (purpose) {
            case ROLLBACK_RESTORATION,
                    STARTUP_RESTORATION,
                    TAKEOVER_RESTORATION,
                    CANCELLATION_RESTORATION,
                    OPERATOR_REQUESTED_SAFE_RESET -> purpose;
            case INITIAL_SAFE_BASELINE -> AllocationPurpose.STARTUP_RESTORATION;
            case EXPERIMENT_CANDIDATE, EXPERIMENT_HOLD ->
                    AllocationPurpose.ROLLBACK_RESTORATION;
            default -> throw failure(
                    "baseline mutation requires an explicit restoration purpose");
        };
    }

    private static String requestId(String operation) {
        return "application-supervisor-" + operation + "-" + UUID.randomUUID();
    }

    private static String commandCorrelationId(
            String applicationTransactionId,
            CommandType command,
            long generation,
            String allocationFingerprint) {
        return "application-command-" + commandIdentity(
                applicationTransactionId,
                command,
                generation,
                allocationFingerprint);
    }

    private static String supervisorTransactionId(
            String applicationTransactionId,
            CommandType command,
            long generation,
            String allocationFingerprint) {
        return "application-" + commandIdentity(
                applicationTransactionId,
                command,
                generation,
                allocationFingerprint);
    }

    private static String commandIdentity(
            String applicationTransactionId,
            CommandType command,
            long generation,
            String allocationFingerprint) {
        return sha256(applicationTransactionId + "|" + command.name()
                + "|" + generation + "|" + allocationFingerprint);
    }

    private static String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static SharedApplicationLedger acquireApplicationLedger(
            Path trustedRoot,
            EnterpriseLabEvidenceOwnershipGate ownershipGate) {
        Path key = Objects.requireNonNull(trustedRoot, "trustedRoot cannot be null")
                .toAbsolutePath().normalize();
        EnterpriseLabEvidenceMutationAuthority.MutationAuthorization authorization =
                Objects.requireNonNull(ownershipGate, "ownershipGate cannot be null")
                        .requireMutationAuthorization();
        synchronized (APPLICATION_LEDGER_REGISTRY_MUTEX) {
            SharedApplicationLedger existing = APPLICATION_LEDGERS.get(key);
            if (existing != null
                    && existing.ownerId.equals(authorization.ownerId())
                    && existing.ownerGeneration == authorization.generation()) {
                existing.references++;
                return existing;
            }
            if (existing != null) {
                existing.ledger.close();
                APPLICATION_LEDGERS.remove(key);
            }
            if (APPLICATION_LEDGERS.size() >= MAX_SHARED_APPLICATION_LEDGERS) {
                throw failure("bounded application command-ledger registry limit reached");
            }
            SharedApplicationLedger created = new SharedApplicationLedger(
                    key,
                    authorization.ownerId(),
                    authorization.generation(),
                    EnterpriseLabApplicationCommandLedger.create(key, ownershipGate));
            APPLICATION_LEDGERS.put(key, created);
            return created;
        }
    }

    private static void releaseApplicationLedger(SharedApplicationLedger shared) {
        synchronized (APPLICATION_LEDGER_REGISTRY_MUTEX) {
            SharedApplicationLedger current = APPLICATION_LEDGERS.get(shared.trustedRoot);
            if (shared.references < 1) {
                throw failure("application command-ledger registry is inconsistent");
            }
            shared.references--;
            if (current != shared) {
                return;
            }
            if (shared.references == 0) {
                APPLICATION_LEDGERS.remove(shared.trustedRoot);
                shared.ledger.close();
            }
        }
    }

    private static final class SharedApplicationLedger {
        private final Path trustedRoot;
        private final String ownerId;
        private final long ownerGeneration;
        private final EnterpriseLabApplicationCommandLedger ledger;
        private final Object commandMutex = new Object();
        private int references = 1;

        private SharedApplicationLedger(
                Path trustedRoot,
                String ownerId,
                long ownerGeneration,
                EnterpriseLabApplicationCommandLedger ledger) {
            this.trustedRoot = Objects.requireNonNull(
                    trustedRoot, "trustedRoot cannot be null");
            this.ownerId = Objects.requireNonNull(ownerId, "ownerId cannot be null");
            this.ownerGeneration = ownerGeneration;
            this.ledger = Objects.requireNonNull(ledger, "ledger cannot be null");
        }
    }

    private record PendingApplicationCommit(
            InstalledStateMutation mutation,
            EnterpriseLabInstalledAllocationSnapshot requested,
            VerifiedDispatchResult dispatch) {
        private PendingApplicationCommit {
            mutation = Objects.requireNonNull(mutation, "mutation cannot be null");
            requested = Objects.requireNonNull(requested, "requested cannot be null");
            dispatch = Objects.requireNonNull(dispatch, "dispatch cannot be null");
        }
    }

    private record OwnedMutationDispatch(
            OwnershipRecord ownership,
            Request request,
            VerifiedDispatchResult dispatch) {
        private OwnedMutationDispatch {
            ownership = Objects.requireNonNull(ownership, "ownership cannot be null");
            request = Objects.requireNonNull(request, "request cannot be null");
            dispatch = Objects.requireNonNull(dispatch, "dispatch cannot be null");
        }
    }

    private void requireOpen() {
        if (closed) {
            throw failure("external supervisor allocation bridge is closed");
        }
    }

    private static IllegalStateException failure(String message) {
        return new IllegalStateException(message);
    }

    public record SessionSnapshot(
            boolean reachable,
            boolean ready,
            String supervisorInstanceId,
            long supervisorGeneration,
            long durableStateGeneration,
            long acceptedApplicationGeneration,
            String installedAllocationFingerprint,
            long routerGeneration,
            Optional<Instant> lastSuccessfulIpcVerification,
            String failureReasonCode) {
        public SessionSnapshot {
            if (supervisorInstanceId == null
                    || !supervisorInstanceId.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
                    || supervisorGeneration < EnterpriseLabEvidenceOwnership.INITIAL_GENERATION
                    || supervisorGeneration > EnterpriseLabEvidenceOwnership.MAX_GENERATION
                    || durableStateGeneration < 1L
                    || durableStateGeneration
                            > EnterpriseLabSupervisorState.HARD_MAX_DURABLE_STATE_GENERATION
                    || acceptedApplicationGeneration < 0L
                    || acceptedApplicationGeneration
                            > EnterpriseLabEvidenceOwnership.MAX_GENERATION
                    || installedAllocationFingerprint == null
                    || !(EnterpriseLabAllocationState.NO_FINGERPRINT.equals(
                            installedAllocationFingerprint)
                    || installedAllocationFingerprint.matches("[0-9a-f]{64}"))
                    || routerGeneration < 0L
                    || routerGeneration
                            > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION
                    || ready && (!reachable || acceptedApplicationGeneration < 1L)) {
                throw new IllegalArgumentException(
                        "external supervisor session snapshot is inconsistent");
            }
            lastSuccessfulIpcVerification = Objects.requireNonNull(
                    lastSuccessfulIpcVerification,
                    "lastSuccessfulIpcVerification cannot be null");
            if (failureReasonCode == null
                    || !failureReasonCode.matches("[A-Z0-9][A-Z0-9_.:-]{0,63}")) {
                throw new IllegalArgumentException(
                        "external supervisor failure reason must be bounded canonical text");
            }
        }
    }
}
