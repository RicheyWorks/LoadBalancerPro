package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.RecoveryClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransitionReason;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.VerificationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipRecord;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.OwnershipState;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReconciliationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabEvidenceOwnership.ReleaseStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabLoopbackAllocationSnapshot.Kind;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseStatus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Synchronous supervisor command state machine. Every installed-state mutation
 * is intent-first, atomically published, independently read back, and then
 * committed. Startup restores the immutable baseline from any incomplete phase.
 */
public final class EnterpriseLabSupervisorService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final FailureInjector NO_FAILURE = point -> { };

    private final EnterpriseLabSupervisorOwnership ownership;
    private final EnterpriseLabSupervisorStateStore store;
    private final EnterpriseLabSupervisorProtocolCodec protocolCodec;
    private final ApplicationOwnershipVerifier applicationOwnershipVerifier;
    private final Clock clock;
    private final FailureInjector failureInjector;
    private final AtomicBoolean shutdownRequested = new AtomicBoolean();

    private EnterpriseLabSupervisorState state;

    public static EnterpriseLabSupervisorService start(
            EnterpriseLabSupervisorOwnership ownership,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            Clock clock) {
        EnterpriseLabSupervisorOwnership safeOwnership = Objects.requireNonNull(
                ownership, "ownership cannot be null");
        Clock safeClock = Objects.requireNonNull(clock, "clock cannot be null");
        return startForTesting(
                safeOwnership,
                targetCatalog,
                safeClock,
                new DurableApplicationOwnershipVerifier(
                        safeOwnership.trustedRoot(), safeClock),
                NO_FAILURE);
    }

    static EnterpriseLabSupervisorService startForTesting(
            EnterpriseLabSupervisorOwnership ownership,
            EnterpriseLabExperimentTargetCatalog targetCatalog,
            Clock clock,
            ApplicationOwnershipVerifier applicationOwnershipVerifier,
            FailureInjector failureInjector) {
        EnterpriseLabSupervisorService service = new EnterpriseLabSupervisorService(
                ownership,
                new EnterpriseLabSupervisorStateStore(ownership, targetCatalog),
                new EnterpriseLabSupervisorProtocolCodec(targetCatalog),
                clock,
                applicationOwnershipVerifier,
                failureInjector);
        service.initializeOrRecover(targetCatalog);
        return service;
    }

    private EnterpriseLabSupervisorService(
            EnterpriseLabSupervisorOwnership ownership,
            EnterpriseLabSupervisorStateStore store,
            EnterpriseLabSupervisorProtocolCodec protocolCodec,
            Clock clock,
            ApplicationOwnershipVerifier applicationOwnershipVerifier,
            FailureInjector failureInjector) {
        this.ownership = Objects.requireNonNull(ownership, "ownership cannot be null");
        this.store = Objects.requireNonNull(store, "store cannot be null");
        this.protocolCodec = Objects.requireNonNull(
                protocolCodec, "protocolCodec cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.applicationOwnershipVerifier = Objects.requireNonNull(
                applicationOwnershipVerifier,
                "applicationOwnershipVerifier cannot be null");
        this.failureInjector = Objects.requireNonNull(
                failureInjector, "failureInjector cannot be null");
    }

    public synchronized EnterpriseLabSupervisorState state() {
        ownership.requireHeld();
        return state;
    }

    public boolean shutdownRequested() {
        return shutdownRequested.get();
    }

    public synchronized Response dispatch(Request request) {
        ownership.requireHeld();
        Request safe = Objects.requireNonNull(request, "request cannot be null");
        EnterpriseLabSupervisorState before = state;
        try {
            if (shutdownRequested.get()
                    && safe.commandType().mutation()
                    && safe.commandType() != CommandType.CLEAN_SHUTDOWN) {
                return rejected(
                        safe,
                        "SUPERVISOR_SHUTTING_DOWN",
                        "Supervisor rejects mutations after clean shutdown begins");
            }
            Optional<Response> temporalRejection = rejectExpired(safe);
            if (temporalRejection.isPresent()) {
                return temporalRejection.orElseThrow();
            }
            Optional<Response> supervisorFenceRejection = rejectSupervisorFence(safe);
            if (supervisorFenceRejection.isPresent()) {
                return supervisorFenceRejection.orElseThrow();
            }
            Optional<Response> changedDuplicate = rejectChangedDuplicate(safe);
            if (changedDuplicate.isPresent()) {
                return changedDuplicate.orElseThrow();
            }
            return switch (safe.commandType()) {
                case HEALTH -> accepted(safe, false, VerificationResult.NOT_ATTEMPTED,
                        "HEALTHY", "Supervisor process and durable state are readable");
                case READINESS -> accepted(safe, false, VerificationResult.NOT_ATTEMPTED,
                        ready() ? "READY" : "NOT_READY",
                        ready()
                                ? "Supervisor allocation mutation prerequisites are satisfied"
                                : "Application ownership or transaction reconciliation is incomplete");
                case READ_INSTALLED_ALLOCATION -> accepted(
                        safe, false, VerificationResult.NOT_ATTEMPTED,
                        "INSTALLED_ALLOCATION_READ", "Installed allocation read back atomically");
                case READ_SUPERVISOR_GENERATION -> accepted(
                        safe, false, VerificationResult.NOT_ATTEMPTED,
                        "SUPERVISOR_GENERATION_READ", "Supervisor generation read successfully");
                case READ_STATUS -> accepted(
                        safe, false, VerificationResult.NOT_ATTEMPTED,
                        state.transactionIncomplete() ? "TRANSACTION_INCOMPLETE" : "STATUS_READ",
                        state.transactionIncomplete()
                                ? "Supervisor retains an incomplete transaction for recovery"
                                : "Supervisor durable state is internally consistent");
                case ADVANCE_APPLICATION_OWNERSHIP -> advanceApplicationOwnership(safe);
                case ESTABLISH_INITIAL_BASELINE -> establishInitialBaseline(safe);
                case APPLY_ALLOCATION -> applyAllocation(safe, false);
                case RESTORE_BASELINE -> applyAllocation(safe, true);
                case VERIFY_ALLOCATION -> verifyAllocation(safe);
                case CLEAN_SHUTDOWN -> cleanShutdown(safe);
            };
        } catch (RuntimeException exception) {
            state = store.readIfPresent().orElse(before);
            return failed(
                    safe,
                    "SUPERVISOR_COMMAND_FAILED",
                    "Supervisor command failed closed with durable evidence preserved");
        }
    }

    private void initializeOrRecover(EnterpriseLabExperimentTargetCatalog targetCatalog) {
        ownership.requireHeld();
        String instanceId = newInstanceId();
        Instant now = clock.instant();
        Optional<EnterpriseLabSupervisorState> prior = store.readIfPresent();
        if (prior.isEmpty()) {
            EnterpriseLabLoopbackAllocationSnapshot baselineRouting =
                    EnterpriseLabLoopbackAllocationSnapshot.normalized(
                            EnterpriseLabSupervisorConfiguration.SCENARIO_ID,
                            0L,
                            "supervisor-safe-baseline",
                            Kind.BASELINE,
                            EnterpriseLabSupervisorConfiguration.safeBaselineAllocation().keySet(),
                            EnterpriseLabSupervisorConfiguration.safeBaselineAllocation());
            EnterpriseLabInstalledAllocationSnapshot baseline =
                    EnterpriseLabInstalledAllocationSnapshot.installed(
                            baselineRouting,
                            clock,
                            "repository-owned safe baseline",
                            EnterpriseLabInstalledAllocationSnapshot.UNOWNED_GENERATION);
            EnterpriseLabSupervisorState initial = EnterpriseLabSupervisorState.create(
                    instanceId,
                    EnterpriseLabEvidenceOwnership.INITIAL_GENERATION,
                    EnterpriseLabSupervisorState.NONE,
                    EnterpriseLabSupervisorState.NONE,
                    0L,
                    EnterpriseLabSupervisorState.NONE,
                    EnterpriseLabSupervisorState.NONE,
                    0L,
                    baseline,
                    baseline,
                    Optional.empty(),
                    EnterpriseLabSupervisorState.NONE,
                    EnterpriseLabSupervisorState.NONE,
                    EnterpriseLabSupervisorState.NONE,
                    baseline.allocationFingerprint(),
                    TransactionPhase.COMMITTED,
                    now,
                    RecoveryClassification.NOT_REQUIRED,
                    reason("SUPERVISOR_INITIALIZED",
                            "Supervisor initialized the repository-owned safe baseline"),
                    1L,
                    EnterpriseLabSupervisorState.GENESIS_FINGERPRINT);
            state = store.install(Optional.empty(), initial);
            return;
        }

        EnterpriseLabSupervisorState previous = prior.orElseThrow();
        long supervisorGeneration = EnterpriseLabEvidenceOwnership.nextGeneration(
                previous.supervisorGeneration());
        EnterpriseLabInstalledAllocationSnapshot installed = previous.installedAllocation();
        TransactionPhase phase = previous.transactionPhase();
        RecoveryClassification recovery = RecoveryClassification.NOT_REQUIRED;
        TransitionReason transitionReason = reason(
                "SUPERVISOR_RESTARTED",
                "Supervisor reconstructed the committed installed allocation");
        String previousCommitted = previous.previousCommittedFingerprint();
        Instant lastCommit = previous.lastCommitAt();
        if (previous.transactionIncomplete()) {
            installed = restoredBaseline(previous, "supervisor startup recovery");
            phase = TransactionPhase.RESTORED;
            recovery = RecoveryClassification.BASELINE_RESTORED;
            transitionReason = reason(
                    "INCOMPLETE_TRANSACTION_RESTORED",
                    "Supervisor restart restored and verified the safe baseline");
            previousCommitted = previous.installedAllocation().allocationFingerprint();
            lastCommit = now;
        }
        EnterpriseLabSupervisorState restarted = EnterpriseLabSupervisorState.create(
                instanceId,
                supervisorGeneration,
                previous.acceptedApplicationInstanceId(),
                previous.acceptedApplicationOwnershipFingerprint(),
                previous.acceptedApplicationGeneration(),
                previous.previousApplicationInstanceId(),
                previous.previousApplicationOwnershipFingerprint(),
                previous.previousApplicationGeneration(),
                previous.baselineAllocation(),
                installed,
                Optional.empty(),
                previous.transactionId(),
                previous.lastRequestId(),
                previous.lastRequestFingerprint(),
                previousCommitted,
                phase,
                lastCommit,
                recovery,
                transitionReason,
                nextDurableGeneration(previous),
                previous.currentRecordFingerprint());
        failureInjector.checkpoint(FailurePoint.BEFORE_STARTUP_STATE_INSTALL);
        state = store.install(prior, restarted);
        failureInjector.checkpoint(FailurePoint.AFTER_STARTUP_STATE_INSTALL);
    }

    private Response advanceApplicationOwnership(Request request) {
        OwnershipVerification verification = applicationOwnershipVerifier.verify(request);
        if (!verification.accepted()) {
            return rejected(request, verification.reasonCode(), verification.reason());
        }
        if (state.transactionIncomplete()) {
            return rejected(request, "OWNERSHIP_HANDOFF_BLOCKED",
                    "Ownership handoff is blocked by an incomplete supervisor transaction");
        }
        if (state.acceptedApplicationGeneration() == request.applicationOwnerGeneration()
                && constantTimeEquals(
                state.acceptedApplicationOwnershipFingerprint(),
                request.applicationOwnershipRecordFingerprint())
                && state.acceptedApplicationInstanceId().equals(
                request.applicationInstanceId())) {
            return accepted(request, false, VerificationResult.NOT_ATTEMPTED,
                    "OWNERSHIP_ALREADY_ACCEPTED",
                    "Application ownership generation was already accepted exactly");
        }
        if (state.acceptedApplicationGeneration() == request.applicationOwnerGeneration()
                && state.acceptedApplicationInstanceId().equals(
                request.applicationInstanceId())) {
            EnterpriseLabSupervisorState refreshed = EnterpriseLabSupervisorState.create(
                    state.supervisorInstanceId(),
                    state.supervisorGeneration(),
                    request.applicationInstanceId(),
                    request.applicationOwnershipRecordFingerprint(),
                    request.applicationOwnerGeneration(),
                    state.previousApplicationInstanceId(),
                    state.previousApplicationOwnershipFingerprint(),
                    state.previousApplicationGeneration(),
                    state.baselineAllocation(),
                    state.installedAllocation(),
                    Optional.empty(),
                    request.transactionId(),
                    request.requestId(),
                    request.requestFingerprint(),
                    state.previousCommittedFingerprint(),
                    state.transactionPhase(),
                    state.lastCommitAt(),
                    state.lastRecoveryClassification(),
                    reason("APPLICATION_OWNERSHIP_EVIDENCE_REFRESHED",
                            "Verified renewal evidence refreshed the accepted application owner"),
                    nextDurableGeneration(state),
                    state.currentRecordFingerprint());
            failureInjector.checkpoint(FailurePoint.BEFORE_OWNERSHIP_HANDOFF_INSTALL);
            persist(refreshed);
            failureInjector.checkpoint(FailurePoint.AFTER_OWNERSHIP_HANDOFF_INSTALL);
            return accepted(request, true, VerificationResult.NOT_ATTEMPTED,
                    "APPLICATION_OWNERSHIP_EVIDENCE_REFRESHED",
                    "Verified renewal evidence refreshed the accepted application owner");
        }
        if (request.applicationOwnerGeneration() <= state.acceptedApplicationGeneration()) {
            return rejected(request, "STALE_APPLICATION_GENERATION",
                    "Application ownership generation is stale");
        }
        if (state.acceptedApplicationGeneration() > 0L
                && request.applicationOwnerGeneration()
                != state.acceptedApplicationGeneration() + 1L) {
            return rejected(request, "APPLICATION_GENERATION_SKIP",
                    "Application ownership handoff must advance exactly one generation");
        }

        EnterpriseLabSupervisorState updated = EnterpriseLabSupervisorState.create(
                state.supervisorInstanceId(),
                state.supervisorGeneration(),
                request.applicationInstanceId(),
                request.applicationOwnershipRecordFingerprint(),
                request.applicationOwnerGeneration(),
                state.acceptedApplicationInstanceId(),
                state.acceptedApplicationOwnershipFingerprint(),
                state.acceptedApplicationGeneration(),
                state.baselineAllocation(),
                state.installedAllocation(),
                Optional.empty(),
                request.transactionId(),
                request.requestId(),
                request.requestFingerprint(),
                state.previousCommittedFingerprint(),
                state.transactionPhase(),
                state.lastCommitAt(),
                state.lastRecoveryClassification(),
                reason("APPLICATION_OWNERSHIP_ADVANCED",
                        "Verified application ownership generation was accepted"),
                nextDurableGeneration(state),
                state.currentRecordFingerprint());
        failureInjector.checkpoint(FailurePoint.BEFORE_OWNERSHIP_HANDOFF_INSTALL);
        persist(updated);
        failureInjector.checkpoint(FailurePoint.AFTER_OWNERSHIP_HANDOFF_INSTALL);
        return accepted(request, true, VerificationResult.NOT_ATTEMPTED,
                "APPLICATION_OWNERSHIP_ADVANCED",
                "Verified application ownership generation was accepted");
    }

    private Response establishInitialBaseline(Request request) {
        Optional<Response> fence = requireAcceptedApplication(request);
        if (fence.isPresent()) {
            return fence.orElseThrow();
        }
        if (!state.baselineAllocation().allocationFingerprint()
                .equals(request.allocationFingerprint())) {
            return rejected(request, "BASELINE_MISMATCH",
                    "Requested baseline does not match the repository-owned safe baseline");
        }
        return accepted(request, false, VerificationResult.MATCHED,
                "BASELINE_ALREADY_ESTABLISHED",
                "Repository-owned safe baseline is already installed and verified");
    }

    private Response applyAllocation(Request request, boolean restoration) {
        Optional<Response> fence = requireAcceptedApplication(request);
        if (fence.isPresent()) {
            return fence.orElseThrow();
        }
        Optional<Response> duplicate = idempotentMutation(request);
        if (duplicate.isPresent()) {
            return duplicate.orElseThrow();
        }
        if (!state.installedAllocation().allocationFingerprint()
                .equals(request.previousCommittedFingerprint())) {
            return rejected(request, "PREVIOUS_FINGERPRINT_MISMATCH",
                    "Installed allocation changed before the requested mutation");
        }

        EnterpriseLabInstalledAllocationSnapshot target;
        if (restoration) {
            if (!state.baselineAllocation().allocationFingerprint()
                    .equals(request.allocationFingerprint())) {
                return rejected(request, "BASELINE_FINGERPRINT_MISMATCH",
                        "Restoration fingerprint does not match the durable safe baseline");
            }
            target = restoredBaseline(state, "application requested safe restoration");
        } else {
            EnterpriseLabLoopbackAllocationSnapshot requested = request.allocation().orElseThrow();
            target = installed(
                    requested,
                    nextRouterGeneration(state),
                    requested.kind(),
                    request.applicationOwnerGeneration(),
                    "verified application allocation request");
        }

        EnterpriseLabSupervisorState intent = transactionSuccessor(
                state,
                state.installedAllocation(),
                Optional.of(target),
                request,
                request.previousCommittedFingerprint(),
                TransactionPhase.INTENT_PERSISTED,
                state.lastCommitAt(),
                RecoveryClassification.INCOMPLETE_TRANSACTION,
                reason("SUPERVISOR_INTENT_PERSISTED",
                        "Supervisor durably recorded the bounded allocation intent"));
        failureInjector.checkpoint(FailurePoint.BEFORE_INTENT_INSTALL);
        persist(intent);
        failureInjector.checkpoint(FailurePoint.AFTER_INTENT_INSTALL);

        EnterpriseLabSupervisorState applied = transactionSuccessor(
                state,
                target,
                Optional.of(target),
                request,
                request.previousCommittedFingerprint(),
                TransactionPhase.APPLIED,
                state.lastCommitAt(),
                RecoveryClassification.INCOMPLETE_TRANSACTION,
                reason("SUPERVISOR_ALLOCATION_APPLIED",
                        "Supervisor atomically replaced the installed loopback allocation"));
        failureInjector.checkpoint(FailurePoint.BEFORE_APPLY_INSTALL);
        persist(applied);
        failureInjector.checkpoint(FailurePoint.AFTER_APPLY_INSTALL);

        EnterpriseLabSupervisorState readBack = store.readIfPresent().orElseThrow();
        if (!readBack.installedAllocation().equals(target)
                || !readBack.installedAllocation().allocationFingerprint()
                .equals(request.allocationFingerprint())) {
            throw new IllegalStateException(
                    "supervisor installed allocation did not read back exactly");
        }

        EnterpriseLabSupervisorState committed = transactionSuccessor(
                state,
                target,
                Optional.empty(),
                request,
                request.previousCommittedFingerprint(),
                restoration ? TransactionPhase.RESTORED : TransactionPhase.COMMITTED,
                clock.instant(),
                restoration
                        ? RecoveryClassification.BASELINE_RESTORED
                        : RecoveryClassification.NOT_REQUIRED,
                reason(
                        restoration
                                ? "SUPERVISOR_BASELINE_RESTORED"
                                : "SUPERVISOR_ALLOCATION_COMMITTED",
                        restoration
                                ? "Supervisor restored and verified the durable safe baseline"
                                : "Supervisor committed the independently verified allocation"));
        failureInjector.checkpoint(FailurePoint.BEFORE_COMMIT_INSTALL);
        persist(committed);
        failureInjector.checkpoint(FailurePoint.AFTER_COMMIT_INSTALL);
        return accepted(
                request,
                true,
                VerificationResult.MATCHED,
                restoration
                        ? "BASELINE_RESTORED"
                        : "ALLOCATION_COMMITTED",
                restoration
                        ? "Durable safe baseline was restored and read back exactly"
                        : "Allocation was atomically installed and read back exactly");
    }

    private Response verifyAllocation(Request request) {
        Optional<Response> fence = requireAcceptedApplication(request);
        if (fence.isPresent()) {
            return fence.orElseThrow();
        }
        boolean matched = state.installedAllocation().allocationFingerprint()
                .equals(request.allocationFingerprint());
        return matched
                ? accepted(request, false, VerificationResult.MATCHED,
                "ALLOCATION_VERIFIED", "Installed allocation fingerprint matched exactly")
                : rejected(request, VerificationResult.MISMATCHED,
                "ALLOCATION_MISMATCH", "Installed allocation fingerprint did not match");
    }

    private Response cleanShutdown(Request request) {
        Optional<Response> fence = requireAcceptedApplication(request);
        if (fence.isPresent()) {
            return fence.orElseThrow();
        }
        shutdownRequested.set(true);
        return accepted(request, true, VerificationResult.NOT_ATTEMPTED,
                "CLEAN_SHUTDOWN_ACCEPTED",
                "Supervisor accepted bounded clean shutdown");
    }

    private Optional<Response> requireAcceptedApplication(Request request) {
        if (!state.applicationOwnerEstablished()) {
            return Optional.of(rejected(request, "APPLICATION_OWNER_NOT_ESTABLISHED",
                    "Supervisor has not accepted an application ownership generation"));
        }
        if (request.applicationOwnerGeneration() != state.acceptedApplicationGeneration()
                || !request.applicationInstanceId().equals(
                state.acceptedApplicationInstanceId())
                || !constantTimeEquals(
                request.applicationOwnershipRecordFingerprint(),
                state.acceptedApplicationOwnershipFingerprint())) {
            return Optional.of(rejected(request, "STALE_APPLICATION_OWNER",
                    "Application ownership fence does not match the accepted generation"));
        }
        OwnershipVerification live = applicationOwnershipVerifier.verify(request);
        if (!live.accepted()) {
            return Optional.of(rejected(request, live.reasonCode(), live.reason()));
        }
        return Optional.empty();
    }

    private Optional<Response> idempotentMutation(Request request) {
        if (state.lastRequestId().equals(request.requestId())) {
            if (!constantTimeEquals(
                    state.lastRequestFingerprint(), request.requestFingerprint())) {
                return Optional.of(rejected(request, "DUPLICATE_REQUEST_CHANGED",
                        "Duplicate request identity carried changed canonical content"));
            }
            boolean exact = request.commandType() == CommandType.RESTORE_BASELINE
                    ? state.installedAllocation().allocationFingerprint()
                    .equals(state.baselineAllocation().allocationFingerprint())
                    : state.installedAllocation().allocationFingerprint()
                    .equals(request.allocationFingerprint());
            return Optional.of(exact
                    ? accepted(request, false, VerificationResult.MATCHED,
                    "DUPLICATE_REQUEST_REPLAYED",
                    "Exact completed request was answered idempotently")
                    : rejected(request, "DUPLICATE_REQUEST_NO_LONGER_CURRENT",
                    "Exact prior request no longer matches installed state"));
        }
        if (!EnterpriseLabSupervisorState.NONE.equals(state.transactionId())
                && state.transactionId().equals(request.transactionId())) {
            return Optional.of(rejected(request, "DUPLICATE_TRANSACTION_CHANGED",
                    "Transaction identity was already used by different request content"));
        }
        return Optional.empty();
    }

    private Optional<Response> rejectChangedDuplicate(Request request) {
        if (state.lastRequestId().equals(request.requestId())
                && !constantTimeEquals(
                state.lastRequestFingerprint(), request.requestFingerprint())) {
            return Optional.of(rejected(request, "DUPLICATE_REQUEST_CHANGED",
                    "Duplicate request identity carried changed canonical content"));
        }
        return Optional.empty();
    }

    private Optional<Response> rejectExpired(Request request) {
        Instant now = clock.instant();
        Duration age = Duration.between(request.requestedAt(), now);
        if (age.compareTo(EnterpriseLabSupervisorConfiguration.MAX_REQUEST_AGE) > 0) {
            return Optional.of(rejected(request, "REQUEST_EXPIRED",
                    "Supervisor request exceeded its bounded age"));
        }
        if (age.compareTo(EnterpriseLabSupervisorConfiguration.MAX_REQUEST_CLOCK_SKEW.negated()) < 0) {
            return Optional.of(rejected(request, "REQUEST_FROM_FUTURE",
                    "Supervisor request exceeded bounded clock skew"));
        }
        return Optional.empty();
    }

    private Optional<Response> rejectSupervisorFence(Request request) {
        if (EnterpriseLabSupervisorState.NONE.equals(
                request.expectedSupervisorInstanceId())) {
            return Optional.empty();
        }
        if (!state.supervisorInstanceId().equals(
                request.expectedSupervisorInstanceId())
                || state.supervisorGeneration()
                != request.expectedSupervisorGeneration()) {
            return Optional.of(rejected(request, "STALE_SUPERVISOR_GENERATION",
                    "Supervisor identity or generation fence did not match"));
        }
        return Optional.empty();
    }

    private boolean ready() {
        return state.applicationOwnerEstablished() && !state.transactionIncomplete();
    }

    private void persist(EnterpriseLabSupervisorState replacement) {
        state = store.install(Optional.of(state), replacement);
    }

    private EnterpriseLabSupervisorState transactionSuccessor(
            EnterpriseLabSupervisorState prior,
            EnterpriseLabInstalledAllocationSnapshot installed,
            Optional<EnterpriseLabInstalledAllocationSnapshot> intended,
            Request request,
            String previousCommittedFingerprint,
            TransactionPhase phase,
            Instant commitAt,
            RecoveryClassification recovery,
            TransitionReason reason) {
        return EnterpriseLabSupervisorState.create(
                prior.supervisorInstanceId(),
                prior.supervisorGeneration(),
                prior.acceptedApplicationInstanceId(),
                prior.acceptedApplicationOwnershipFingerprint(),
                prior.acceptedApplicationGeneration(),
                prior.previousApplicationInstanceId(),
                prior.previousApplicationOwnershipFingerprint(),
                prior.previousApplicationGeneration(),
                prior.baselineAllocation(),
                installed,
                intended,
                request.transactionId(),
                request.requestId(),
                request.requestFingerprint(),
                previousCommittedFingerprint,
                phase,
                commitAt,
                recovery,
                reason,
                nextDurableGeneration(prior),
                prior.currentRecordFingerprint());
    }

    private EnterpriseLabInstalledAllocationSnapshot restoredBaseline(
            EnterpriseLabSupervisorState prior,
            String installationReason) {
        EnterpriseLabLoopbackAllocationSnapshot baseline = prior.baselineAllocation()
                .routingSnapshot();
        return installed(
                baseline,
                nextRouterGeneration(prior),
                Kind.RESTORED_BASELINE,
                prior.acceptedApplicationGeneration(),
                installationReason);
    }

    private EnterpriseLabInstalledAllocationSnapshot installed(
            EnterpriseLabLoopbackAllocationSnapshot requested,
            long routerGeneration,
            Kind kind,
            long applicationGeneration,
            String installationReason) {
        EnterpriseLabLoopbackAllocationSnapshot routing =
                new EnterpriseLabLoopbackAllocationSnapshot(
                        EnterpriseLabLoopbackAllocationSnapshot.SCHEMA_VERSION,
                        requested.scenarioId(),
                        routerGeneration,
                        requested.sourceDecisionId(),
                        kind,
                        requested.allocations());
        return EnterpriseLabInstalledAllocationSnapshot.installed(
                routing,
                clock,
                installationReason,
                applicationGeneration);
    }

    private static long nextRouterGeneration(EnterpriseLabSupervisorState state) {
        long current = state.installedAllocation().routerGeneration();
        if (current >= EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION) {
            throw new IllegalStateException("supervisor router generation is exhausted");
        }
        return current + 1L;
    }

    private static long nextDurableGeneration(EnterpriseLabSupervisorState state) {
        long current = state.durableStateGeneration();
        if (current >= EnterpriseLabSupervisorState.HARD_MAX_DURABLE_STATE_GENERATION) {
            throw new IllegalStateException("supervisor durable state generation is exhausted");
        }
        return current + 1L;
    }

    private Response accepted(
            Request request,
            boolean actionPerformed,
            VerificationResult verification,
            String reasonCode,
            String reason) {
        return response(
                request,
                ResponseStatus.ACCEPTED,
                actionPerformed,
                verification,
                reasonCode,
                reason);
    }

    private Response rejected(Request request, String reasonCode, String reason) {
        return rejected(
                request, VerificationResult.NOT_ATTEMPTED, reasonCode, reason);
    }

    private Response rejected(
            Request request,
            VerificationResult verification,
            String reasonCode,
            String reason) {
        return response(
                request,
                ResponseStatus.REJECTED,
                false,
                verification,
                reasonCode,
                reason);
    }

    private Response failed(Request request, String reasonCode, String reason) {
        return response(
                request,
                ResponseStatus.FAILED,
                false,
                VerificationResult.READ_BACK_FAILED,
                reasonCode,
                reason);
    }

    private Response response(
            Request request,
            ResponseStatus status,
            boolean actionPerformed,
            VerificationResult verification,
            String reasonCode,
            String reason) {
        EnterpriseLabInstalledAllocationSnapshot installed = state.installedAllocation();
        ResponseDraft draft = new ResponseDraft(
                request.requestId(),
                request.requestFingerprint(),
                request.commandType(),
                state.supervisorInstanceId(),
                state.supervisorGeneration(),
                state.acceptedApplicationGeneration(),
                request.commandType().classification(),
                status,
                actionPerformed,
                Optional.of(installed),
                installed.allocationFingerprint(),
                installed.routerGeneration(),
                state.durableStateGeneration(),
                verification,
                reasonCode,
                reason,
                clock.instant());
        return protocolCodec.issue(request, draft);
    }

    private static TransitionReason reason(String code, String message) {
        return new TransitionReason(code, message);
    }

    private static String newInstanceId() {
        byte[] random = new byte[16];
        SECURE_RANDOM.nextBytes(random);
        return "supervisor-" + HexFormat.of().formatHex(random);
    }

    private static boolean constantTimeEquals(String first, String second) {
        return MessageDigest.isEqual(
                first.getBytes(StandardCharsets.US_ASCII),
                second.getBytes(StandardCharsets.US_ASCII));
    }

    enum FailurePoint {
        BEFORE_STARTUP_STATE_INSTALL,
        AFTER_STARTUP_STATE_INSTALL,
        BEFORE_OWNERSHIP_HANDOFF_INSTALL,
        AFTER_OWNERSHIP_HANDOFF_INSTALL,
        BEFORE_INTENT_INSTALL,
        AFTER_INTENT_INSTALL,
        BEFORE_APPLY_INSTALL,
        AFTER_APPLY_INSTALL,
        BEFORE_COMMIT_INSTALL,
        AFTER_COMMIT_INSTALL
    }

    @FunctionalInterface
    interface FailureInjector {
        void checkpoint(FailurePoint point);
    }

    @FunctionalInterface
    interface ApplicationOwnershipVerifier {
        OwnershipVerification verify(Request request);
    }

    record OwnershipVerification(
            boolean accepted,
            String reasonCode,
            String reason) {
        static OwnershipVerification allow() {
            return new OwnershipVerification(
                    true,
                    "APPLICATION_OWNERSHIP_VERIFIED",
                    "Application ownership evidence was verified");
        }

        static OwnershipVerification rejected(String code, String reason) {
            return new OwnershipVerification(false, code, reason);
        }
    }

    private static final class DurableApplicationOwnershipVerifier
            implements ApplicationOwnershipVerifier {
        private final EnterpriseLabEvidenceOwnershipRecordStore recordStore;
        private final Clock clock;

        private DurableApplicationOwnershipVerifier(Path trustedRoot, Clock clock) {
            EnterpriseLabEvidenceOwnershipPaths paths =
                    EnterpriseLabEvidenceOwnershipPaths.create(trustedRoot);
            this.recordStore = new EnterpriseLabEvidenceOwnershipRecordStore(
                    paths,
                    new EnterpriseLabEvidenceOwnershipCodec(),
                    point -> { });
            this.clock = clock;
        }

        @Override
        public OwnershipVerification verify(Request request) {
            try {
                Optional<OwnershipRecord> found = recordStore.readIfPresent();
                if (found.isEmpty()) {
                    return OwnershipVerification.rejected(
                            "APPLICATION_OWNERSHIP_MISSING",
                            "Application ownership evidence is not present");
                }
                OwnershipRecord record = found.orElseThrow();
                boolean activeState = record.state() == OwnershipState.OWNED
                        || record.state() == OwnershipState.TAKEOVER_COMPLETE;
                boolean exact = record.generation() == request.applicationOwnerGeneration()
                        && record.owner().applicationInstanceId()
                        .equals(request.applicationInstanceId())
                        && constantTimeEquals(
                        record.recordFingerprint(),
                        request.applicationOwnershipRecordFingerprint());
                boolean ready = record.reconciliationStatus() == ReconciliationStatus.SUCCEEDED
                        && record.releaseStatus() != ReleaseStatus.RELEASED
                        && record.leaseExpiresAt().isAfter(clock.instant());
                return activeState && exact && ready
                        ? OwnershipVerification.allow()
                        : OwnershipVerification.rejected(
                        "APPLICATION_OWNERSHIP_INVALID",
                        "Application ownership evidence is stale, unreconciled, or mismatched");
            } catch (RuntimeException exception) {
                return OwnershipVerification.rejected(
                        "APPLICATION_OWNERSHIP_UNREADABLE",
                        "Application ownership evidence could not be verified safely");
            }
        }
    }
}
