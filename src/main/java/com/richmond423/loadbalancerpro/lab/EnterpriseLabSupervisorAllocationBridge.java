package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReconciliationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationRouter.InstalledStateMutation;
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
import java.util.HexFormat;
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
    private final Path trustedRoot;
    private final EnterpriseLabExperimentTargetCatalog targetCatalog;
    private final EnterpriseLabSupervisorProtocolCodec codec;
    private final EnterpriseLabEvidenceOwnershipGate ownershipGate;
    private final Clock clock;
    private final AtomicReference<EnterpriseLabInstalledAllocationSnapshot> cached =
            new AtomicReference<>();

    private EnterpriseLabSupervisorClient client;
    private EnterpriseLabSupervisorConnectionMetadata supervisor;
    private String acceptedOwnershipFingerprint = EnterpriseLabSupervisorProtocol.NONE;
    private volatile boolean closed;

    private EnterpriseLabSupervisorAllocationBridge(
            Path trustedRoot,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            EnterpriseLabSupervisorClient client,
            EnterpriseLabSupervisorProtocolCodec codec,
            EnterpriseLabEvidenceOwnershipGate ownershipGate,
            Clock clock) {
        this.trustedRoot = Objects.requireNonNull(
                trustedRoot, "trustedRoot cannot be null");
        this.targetCatalog = Objects.requireNonNull(
                targetCatalog, "targetCatalog cannot be null");
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.codec = Objects.requireNonNull(codec, "codec cannot be null");
        this.ownershipGate = Objects.requireNonNull(
                ownershipGate, "ownershipGate cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
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
        EnterpriseLabSupervisorClient client = EnterpriseLabSupervisorClient.connect(
                trustedRoot, safeCatalog, safeClock);
        boolean transferred = false;
        try {
            EnterpriseLabSupervisorAllocationBridge bridge =
                    new EnterpriseLabSupervisorAllocationBridge(
                            trustedRoot,
                            safeCatalog,
                            client,
                            new EnterpriseLabSupervisorProtocolCodec(safeCatalog),
                            safeGate,
                            safeClock);
            bridge.ensureOwnershipAccepted(bridge.currentOwnership());
            bridge.readAuthoritative();
            transferred = true;
            return bridge;
        } finally {
            if (!transferred) {
                client.close();
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
        OwnershipRecord ownership = currentOwnership();
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
                "read");
        Response response = requireAccepted(client.execute(request), request);
        requireSameOwnership(ownership);
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
        OwnershipRecord ownership = currentOwnership();
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
                "health");
        requireAccepted(client.execute(request), request);
        requireSameOwnership(ownership);
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
            ensureOwnershipAccepted(ownership);
            readAuthoritative();
            requireSameOwnership(ownership);
            staleClient.close();
            return next;
        } catch (RuntimeException exception) {
            replacement.close();
            client = staleClient;
            supervisor = previous;
            acceptedOwnershipFingerprint = staleAcceptedOwnership;
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

        OwnershipRecord ownership = currentOwnership();
        ensureOwnershipAccepted(ownership);
        EnterpriseLabInstalledAllocationSnapshot observed = readAuthoritative();
        if (!observed.equals(safeExpected)) {
            return false;
        }
        requireSameOwnership(ownership);

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
                candidate ? "apply" : "restore");
        Response action = requireAccepted(client.execute(request), request);
        EnterpriseLabInstalledAllocationSnapshot acted = action.installedAllocation()
                .orElseThrow(() -> failure(
                        "supervisor accepted an allocation mutation without exact read-back"));
        requireRequestedState(safeUpdate, ownership, acted, action);
        cached.set(acted);
        requireSameOwnership(ownership);

        EnterpriseLabInstalledAllocationSnapshot readBack = readAuthoritative();
        if (!readBack.equals(acted)) {
            throw failure(
                    "independent supervisor read-back differed from the action receipt");
        }
        requireRequestedState(safeUpdate, ownership, readBack, action);
        return true;
    }

    public synchronized EnterpriseLabSupervisorConnectionMetadata connectionMetadata() {
        requireOpen();
        return supervisor;
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            client.close();
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
                "handoff");
        Response response = requireAccepted(client.execute(request), request);
        if (response.observedApplicationGeneration() != ownership.generation()) {
            throw failure(
                    "supervisor handoff response did not accept the current application generation");
        }
        requireSameOwnership(ownership);
        acceptedOwnershipFingerprint = ownership.recordFingerprint();
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
            String operation) {
        return codec.issue(new RequestDraft(
                requestId(operation),
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
                clock.instant(),
                allocationGeneration > 0L
                        ? Map.of(
                                "applicationAllocationGeneration",
                                Long.toString(allocationGeneration),
                                "applicationTransactionBoundary",
                                "durable-intent-before-supervisor-ipc")
                        : Map.of()));
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
        return safe;
    }

    private void requireSameOwnership(OwnershipRecord expected) {
        OwnershipRecord current = currentOwnership();
        if (!current.owner().applicationInstanceId().equals(
                expected.owner().applicationInstanceId())
                || current.generation() != expected.generation()
                || !current.recordFingerprint().equals(expected.recordFingerprint())) {
            throw failure(
                    "application ownership changed during supervisor transaction");
        }
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

    private static String supervisorTransactionId(
            String applicationTransactionId,
            CommandType command,
            long generation,
            String allocationFingerprint) {
        String content = applicationTransactionId + "|" + command.name()
                + "|" + generation + "|" + allocationFingerprint;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    content.getBytes(StandardCharsets.UTF_8));
            return "application-" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
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
}
