package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandLedger.ApplicationEventDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ApplicationCommitStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.AuthenticationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.DuplicateClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.EventType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.MutationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ResponseClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ValidationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandClassification;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded application-startup repair across the independently replayed command
 * ledgers. It can finish durable application evidence, but the only allocation
 * advancement it delegates is the existing coordinator's exact
 * supervisor-commit recovery path. Ambiguous mutations remain for the existing
 * baseline reconciler and are terminalized only after that gate is ready.
 */
final class EnterpriseLabCommandHistoryReconciler {
    static final int HARD_MAX_UNRESOLVED_COMMANDS = 128;

    private static final List<EventType> APPLICATION_TERMINAL = List.of(
            EventType.APPLICATION_COMMITTED,
            EventType.COMMAND_FAILED,
            EventType.COMMAND_QUARANTINED);
    private static final List<EventType> SUPERVISOR_REJECTION = List.of(
            EventType.AUTHENTICATION_REJECTED,
            EventType.VALIDATION_REJECTED,
            EventType.DUPLICATE_REJECTED,
            EventType.COMMAND_FAILED,
            EventType.COMMAND_QUARANTINED);

    private final EnterpriseLabApplicationCommandLedger applicationLedger;
    private final SupervisorHistoryReader supervisorHistoryReader;
    private final EnterpriseLabAllocationTransactionCoordinator allocationCoordinator;
    private final InstalledStateReader installedStateReader;
    private final Clock clock;

    EnterpriseLabCommandHistoryReconciler(
            EnterpriseLabApplicationCommandLedger applicationLedger,
            SupervisorHistoryReader supervisorHistoryReader,
            EnterpriseLabAllocationTransactionCoordinator allocationCoordinator,
            InstalledStateReader installedStateReader,
            Clock clock) {
        this.applicationLedger = Objects.requireNonNull(
                applicationLedger, "applicationLedger cannot be null");
        this.supervisorHistoryReader = Objects.requireNonNull(
                supervisorHistoryReader, "supervisorHistoryReader cannot be null");
        this.allocationCoordinator = Objects.requireNonNull(
                allocationCoordinator, "allocationCoordinator cannot be null");
        this.installedStateReader = Objects.requireNonNull(
                installedStateReader, "installedStateReader cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    synchronized Checkpoint reconcileBeforeAllocation() {
        EnterpriseLabApplicationCommandLedger.ReadResult application =
                applicationLedger.replay();
        EnterpriseLabSupervisorCommandLedger.ReadResult supervisor =
                supervisorHistoryReader.read();
        Map<String, List<EnterpriseLabCommandLedgerEvent>> applicationCommands =
                group(application.events());
        Map<String, List<EnterpriseLabCommandLedgerEvent>> supervisorCommands =
                group(supervisor.events());

        for (Map.Entry<String, List<EnterpriseLabCommandLedgerEvent>> entry
                : supervisorCommands.entrySet()) {
            if (!applicationCommands.containsKey(entry.getKey())
                    && entry.getValue().stream().anyMatch(event ->
                            event.commandType().classification()
                                    == CommandClassification.ALLOCATION_MUTATION)) {
                throw failure(
                        "supervisor allocation evidence has no durable application intent");
            }
        }
        for (Map.Entry<String, List<EnterpriseLabCommandLedgerEvent>> entry
                : applicationCommands.entrySet()) {
            List<EnterpriseLabCommandLedgerEvent> supervisorEvents =
                    supervisorCommands.getOrDefault(entry.getKey(), List.of());
            if (!supervisorEvents.isEmpty()
                    && supervisorEvents.stream().anyMatch(event ->
                            !sameCommandIdentity(entry.getValue().get(0), event))) {
                quarantineIfOpen(
                        last(entry.getValue()),
                        last(supervisorEvents),
                        "CORRELATION_IDENTITY_MISMATCH");
                throw failure("cross-ledger command identity mismatch");
            }
        }

        List<EnterpriseLabCommandLedgerEvent> unresolved = application.unresolvedHeads();
        if (unresolved.size() > HARD_MAX_UNRESOLVED_COMMANDS) {
            throw failure("bounded unresolved command limit exceeded");
        }

        List<PendingAmbiguousMutation> pending = new ArrayList<>();
        List<CommandRepair> repairs = new ArrayList<>();
        for (EnterpriseLabCommandLedgerEvent applicationHead : unresolved) {
            List<EnterpriseLabCommandLedgerEvent> supervisorEvents =
                    supervisorCommands.getOrDefault(
                            applicationHead.correlationId(), List.of());
            if (supervisorEvents.isEmpty()) {
                completeFailed(
                        applicationHead,
                        Optional.empty(),
                        "INTENT_WITHOUT_SUPERVISOR_RECEIPT",
                        Classification.NOT_EXECUTED);
                repairs.add(CommandRepair.of(
                        applicationHead,
                        Optional.empty(),
                        Classification.NOT_EXECUTED,
                        false));
                continue;
            }

            EnterpriseLabCommandLedgerEvent supervisorHead = last(supervisorEvents);
            Optional<EnterpriseLabCommandLedgerEvent> supervisorCommit = findLast(
                    supervisorEvents, EventType.SUPERVISOR_COMMITTED);
            boolean mutationStarted = containsAny(
                    supervisorEvents,
                    EventType.MUTATION_STARTED,
                    EventType.ALLOCATION_APPLIED,
                    EventType.READ_BACK_VERIFIED);
            boolean rejected = containsAny(
                    supervisorEvents, SUPERVISOR_REJECTION.toArray(EventType[]::new))
                    || (supervisorHead.eventType() == EventType.RESPONSE_SENT
                    && supervisorHead.validationResult() == ValidationResult.REJECTED);

            if (supervisorCommit.isPresent()) {
                EnterpriseLabCommandLedgerEvent commit = supervisorCommit.orElseThrow();
                if (applicationHead.commandType().classification()
                        != CommandClassification.ALLOCATION_MUTATION) {
                    quarantineIfOpen(
                            applicationHead, supervisorHead, "UNEXPECTED_SUPERVISOR_COMMIT");
                    throw failure(
                            "non-allocation command has supervisor mutation commit evidence");
                }
                EnterpriseLabAllocationTransactionCoordinator.TransactionReceipt receipt;
                try {
                    receipt = allocationCoordinator.recoverSupervisorCommitted(commit);
                } catch (RuntimeException exception) {
                    quarantineIfOpen(
                            applicationHead,
                            supervisorHead,
                            "SUPERVISOR_COMMIT_RECONCILIATION_FAILED");
                    throw exception;
                }
                EnterpriseLabInstalledAllocationSnapshot installed = installedStateReader.read();
                if (!installed.allocationFingerprint().equals(
                                commit.installedFingerprintAfter())
                        || receipt.durablePhase().orElse(null)
                                != EnterpriseLabAllocationState.TransactionPhase.COMMITTED) {
                    quarantineIfOpen(
                            applicationHead, supervisorHead, "INSTALLED_STATE_MISMATCH");
                    throw failure(
                            "supervisor commit recovery did not verify exact installed state");
                }
                completeCommitted(applicationHead, supervisorHead, installed);
                repairs.add(CommandRepair.of(
                        applicationHead,
                        Optional.of(supervisorHead),
                        Classification.SUPERVISOR_COMMIT_RECOVERED,
                        false));
                continue;
            }

            if (mutationStarted) {
                if (applicationHead.commandType().classification()
                        != CommandClassification.ALLOCATION_MUTATION) {
                    quarantineIfOpen(
                            applicationHead, supervisorHead, "UNEXPECTED_MUTATION_HISTORY");
                    throw failure("non-allocation command has mutation history");
                }
                pending.add(new PendingAmbiguousMutation(
                        applicationHead.correlationId(),
                        applicationHead.currentFingerprint(),
                        supervisorHead.currentFingerprint()));
                repairs.add(CommandRepair.of(
                        applicationHead,
                        Optional.of(supervisorHead),
                        Classification.AMBIGUOUS_MUTATION,
                        false));
                continue;
            }

            Optional<EnterpriseLabCommandLedgerEvent> acceptedResponse =
                    findLastAcceptedResponse(supervisorEvents);
            if (acceptedResponse.isPresent()) {
                EnterpriseLabCommandLedgerEvent accepted = acceptedResponse.orElseThrow();
                EnterpriseLabInstalledAllocationSnapshot installed =
                        installedStateReader.read();
                completeCommitted(applicationHead, accepted, installed);
                repairs.add(CommandRepair.of(
                        applicationHead,
                        Optional.of(accepted),
                        Classification.OBSERVATION_RECOVERED,
                        false));
                continue;
            }

            if (rejected) {
                completeFailed(
                        applicationHead,
                        Optional.of(supervisorHead),
                        rejected ? "SUPERVISOR_REJECTED_WITHOUT_MUTATION"
                                : "SUPERVISOR_RECEIPT_WITHOUT_MUTATION",
                        rejected ? Classification.REJECTED
                                : Classification.RECEIVED_NOT_EXECUTED);
                repairs.add(CommandRepair.of(
                        applicationHead,
                        Optional.of(supervisorHead),
                        rejected ? Classification.REJECTED
                                : Classification.RECEIVED_NOT_EXECUTED,
                        false));
                continue;
            }

            completeFailed(
                    applicationHead,
                    Optional.of(supervisorHead),
                    "SUPERVISOR_RECEIPT_WITHOUT_MUTATION",
                    Classification.RECEIVED_NOT_EXECUTED);
            repairs.add(CommandRepair.of(
                    applicationHead,
                    Optional.of(supervisorHead),
                    Classification.RECEIVED_NOT_EXECUTED,
                    false));
        }

        auditTerminalHistories(applicationCommands, supervisorCommands);
        return new Checkpoint(pending, repairs, unresolved.size());
    }

    synchronized Report reconcileAfterAllocation(
            Checkpoint checkpoint,
            EnterpriseLabAllocationReconciler.ReconciliationReport allocationReport) {
        Checkpoint safeCheckpoint = Objects.requireNonNull(
                checkpoint, "checkpoint cannot be null");
        EnterpriseLabAllocationReconciler.ReconciliationReport safeReport =
                Objects.requireNonNull(allocationReport, "allocationReport cannot be null");
        if (!safeReport.ready()) {
            throw failure(
                    "ambiguous command history cannot close before allocation readiness");
        }

        List<CommandRepair> repairs = new ArrayList<>(safeCheckpoint.repairs());
        for (PendingAmbiguousMutation pending : safeCheckpoint.pending()) {
            EnterpriseLabApplicationCommandLedger.ReadResult application =
                    applicationLedger.replay();
            List<EnterpriseLabCommandLedgerEvent> applicationEvents =
                    application.eventsFor(pending.correlationId());
            if (applicationEvents.isEmpty()) {
                throw failure("pending application command disappeared during reconciliation");
            }
            EnterpriseLabCommandLedgerEvent applicationHead = last(applicationEvents);
            if (!applicationHead.currentFingerprint().equals(
                    pending.applicationHeadFingerprint())) {
                throw failure("application command changed during startup reconciliation");
            }
            List<EnterpriseLabCommandLedgerEvent> supervisorEvents =
                    supervisorHistoryReader.read().eventsFor(pending.correlationId());
            if (supervisorEvents.isEmpty()) {
                throw failure("supervisor command disappeared during startup reconciliation");
            }
            EnterpriseLabCommandLedgerEvent supervisorHead = last(supervisorEvents);
            if (!supervisorHead.currentFingerprint().equals(
                    pending.supervisorHeadFingerprint())) {
                throw failure("supervisor command changed during startup reconciliation");
            }
            EnterpriseLabInstalledAllocationSnapshot installed = installedStateReader.read();
            completeFailed(
                    applicationHead,
                    Optional.of(supervisorHead),
                    "AMBIGUOUS_MUTATION_BASELINE_RECONCILED",
                    Classification.AMBIGUOUS_MUTATION_RECONCILED,
                    installed);
            repairs.add(CommandRepair.of(
                    applicationHead,
                    Optional.of(supervisorHead),
                    Classification.AMBIGUOUS_MUTATION_RECONCILED,
                    false));
        }
        return new Report(
                safeCheckpoint.unresolvedBefore(),
                repairs.size(),
                safeCheckpoint.pending().size(),
                List.copyOf(repairs),
                true,
                "COMMAND_HISTORY_RECONCILED");
    }

    private void auditTerminalHistories(
            Map<String, List<EnterpriseLabCommandLedgerEvent>> applicationCommands,
            Map<String, List<EnterpriseLabCommandLedgerEvent>> supervisorCommands) {
        for (Map.Entry<String, List<EnterpriseLabCommandLedgerEvent>> entry
                : applicationCommands.entrySet()) {
            EnterpriseLabCommandLedgerEvent applicationHead = last(entry.getValue());
            if (!APPLICATION_TERMINAL.contains(applicationHead.eventType())) {
                continue;
            }
            List<EnterpriseLabCommandLedgerEvent> supervisorEvents =
                    supervisorCommands.getOrDefault(entry.getKey(), List.of());
            if (applicationHead.eventType() == EventType.COMMAND_QUARANTINED) {
                throw failure("quarantined command history keeps readiness failed closed");
            }
            if (applicationHead.eventType() == EventType.APPLICATION_COMMITTED) {
                if (supervisorEvents.isEmpty()
                        || supervisorEvents.stream().noneMatch(event ->
                                event.currentFingerprint().equals(
                                        applicationHead.observedSupervisorEventFingerprint()))) {
                    throw failure(
                            "application terminal commit lacks matching supervisor evidence");
                }
                if (applicationHead.commandType().classification()
                        == CommandClassification.ALLOCATION_MUTATION
                        && findLast(supervisorEvents, EventType.SUPERVISOR_COMMITTED).isEmpty()) {
                    throw failure(
                            "application allocation commit lacks supervisor commit evidence");
                }
            } else if (findLast(
                    supervisorEvents, EventType.SUPERVISOR_COMMITTED).isPresent()) {
                throw failure(
                        "failed application command conflicts with supervisor commit evidence");
            }
        }
    }

    private void completeCommitted(
            EnterpriseLabCommandLedgerEvent applicationHead,
            EnterpriseLabCommandLedgerEvent supervisorHead,
            EnterpriseLabInstalledAllocationSnapshot installed) {
        Classification classification = applicationHead.commandType().classification()
                == CommandClassification.ALLOCATION_MUTATION
                ? Classification.SUPERVISOR_COMMIT_RECOVERED
                : Classification.OBSERVATION_RECOVERED;
        if (applicationHead.eventType() != EventType.RECONCILIATION_COMPLETED) {
            applicationLedger.appendReconciliation(
                    applicationHead.correlationId(),
                    outcomeDraft(
                            EventType.RECONCILIATION_COMPLETED,
                            applicationHead,
                            Optional.of(supervisorHead),
                            installed,
                            ApplicationCommitStatus.PENDING,
                            applicationHead.commandType().classification()
                                    == CommandClassification.ALLOCATION_MUTATION
                                    ? MutationStatus.COMMITTED
                                    : MutationStatus.NOT_ATTEMPTED,
                            "CROSS_PROCESS_HISTORY_VERIFIED",
                            classification));
        }
        applicationLedger.appendReconciliation(
                applicationHead.correlationId(),
                outcomeDraft(
                        EventType.APPLICATION_COMMITTED,
                        applicationHead,
                        Optional.of(supervisorHead),
                        installed,
                        ApplicationCommitStatus.COMMITTED,
                        applicationHead.commandType().classification()
                                == CommandClassification.ALLOCATION_MUTATION
                                ? MutationStatus.COMMITTED
                                : MutationStatus.NOT_ATTEMPTED,
                        "APPLICATION_COMMIT_RECONCILED",
                        classification));
    }

    private void completeFailed(
            EnterpriseLabCommandLedgerEvent applicationHead,
            Optional<EnterpriseLabCommandLedgerEvent> supervisorHead,
            String reasonCode,
            Classification classification) {
        EnterpriseLabInstalledAllocationSnapshot installed = installedStateReader.read();
        completeFailed(
                applicationHead, supervisorHead, reasonCode, classification, installed);
    }

    private void completeFailed(
            EnterpriseLabCommandLedgerEvent applicationHead,
            Optional<EnterpriseLabCommandLedgerEvent> supervisorHead,
            String reasonCode,
            Classification classification,
            EnterpriseLabInstalledAllocationSnapshot installed) {
        if (applicationHead.eventType() != EventType.RECONCILIATION_COMPLETED) {
            applicationLedger.appendReconciliation(
                    applicationHead.correlationId(),
                    outcomeDraft(
                            EventType.RECONCILIATION_COMPLETED,
                            applicationHead,
                            supervisorHead,
                            installed,
                            ApplicationCommitStatus.PENDING,
                            MutationStatus.NOT_ATTEMPTED,
                            reasonCode,
                            classification));
        }
        applicationLedger.appendReconciliation(
                applicationHead.correlationId(),
                outcomeDraft(
                        EventType.COMMAND_FAILED,
                        applicationHead,
                        supervisorHead,
                        installed,
                        ApplicationCommitStatus.FAILED,
                        classification == Classification.AMBIGUOUS_MUTATION_RECONCILED
                                ? MutationStatus.FAILED
                                : MutationStatus.NOT_ATTEMPTED,
                        reasonCode,
                        classification));
    }

    private void quarantineIfOpen(
            EnterpriseLabCommandLedgerEvent applicationHead,
            EnterpriseLabCommandLedgerEvent supervisorHead,
            String reasonCode) {
        if (APPLICATION_TERMINAL.contains(applicationHead.eventType())) {
            return;
        }
        applicationLedger.appendReconciliation(
                applicationHead.correlationId(),
                outcomeDraft(
                        EventType.COMMAND_QUARANTINED,
                        applicationHead,
                        Optional.of(supervisorHead),
                        installedStateReader.read(),
                        ApplicationCommitStatus.FAILED,
                        MutationStatus.QUARANTINED,
                        reasonCode,
                        Classification.QUARANTINED));
    }

    private ApplicationEventDraft outcomeDraft(
            EventType eventType,
            EnterpriseLabCommandLedgerEvent applicationHead,
            Optional<EnterpriseLabCommandLedgerEvent> supervisorHead,
            EnterpriseLabInstalledAllocationSnapshot installed,
            ApplicationCommitStatus commitStatus,
            MutationStatus mutationStatus,
            String reasonCode,
            Classification classification) {
        Optional<EnterpriseLabCommandLedgerEvent> observed = Objects.requireNonNull(
                supervisorHead, "supervisorHead cannot be null");
        EnterpriseLabCommandLedgerEvent supervisor = observed.orElse(null);
        return new ApplicationEventDraft(
                eventType,
                applicationHead.installedFingerprintAfter(),
                installed.allocationFingerprint(),
                installed.routerGeneration(),
                installed.routerGeneration(),
                supervisor == null
                        ? AuthenticationResult.NOT_ATTEMPTED
                        : AuthenticationResult.ACCEPTED,
                supervisor == null
                        ? ValidationResult.NOT_ATTEMPTED
                        : supervisor.validationResult(),
                supervisor == null
                        ? DuplicateClassification.NOT_EVALUATED
                        : supervisor.duplicateClassification(),
                mutationStatus,
                ResponseClassification.NOT_ATTEMPTED,
                supervisor == null
                        ? EnterpriseLabCommandLedgerEvent.NONE
                        : supervisor.responseFingerprint(),
                supervisor == null
                        ? EnterpriseLabCommandLedgerEvent.NONE
                        : supervisor.currentFingerprint(),
                commitStatus,
                applicationHead.retryAttempt(),
                reasonCode,
                clock.instant(),
                Map.of(
                        "boundary", "single-host-command-reconciliation",
                        "classification", classification.name()));
    }

    private static Optional<EnterpriseLabCommandLedgerEvent> findLastAcceptedResponse(
            List<EnterpriseLabCommandLedgerEvent> events) {
        for (int index = events.size() - 1; index >= 0; index--) {
            EnterpriseLabCommandLedgerEvent event = events.get(index);
            if (event.eventType() == EventType.RESPONSE_SENT
                    && event.validationResult() == ValidationResult.ACCEPTED) {
                return Optional.of(event);
            }
        }
        return Optional.empty();
    }

    private static boolean containsAny(
            List<EnterpriseLabCommandLedgerEvent> events,
            EventType... types) {
        List<EventType> accepted = List.of(types);
        return events.stream().anyMatch(event -> accepted.contains(event.eventType()));
    }

    private static Optional<EnterpriseLabCommandLedgerEvent> findLast(
            List<EnterpriseLabCommandLedgerEvent> events,
            EventType type) {
        for (int index = events.size() - 1; index >= 0; index--) {
            if (events.get(index).eventType() == type) {
                return Optional.of(events.get(index));
            }
        }
        return Optional.empty();
    }

    private static Map<String, List<EnterpriseLabCommandLedgerEvent>> group(
            List<EnterpriseLabCommandLedgerEvent> events) {
        Map<String, List<EnterpriseLabCommandLedgerEvent>> grouped =
                new LinkedHashMap<>();
        for (EnterpriseLabCommandLedgerEvent event : events) {
            grouped.computeIfAbsent(event.correlationId(), ignored -> new ArrayList<>())
                    .add(event);
        }
        Map<String, List<EnterpriseLabCommandLedgerEvent>> immutable =
                new LinkedHashMap<>();
        grouped.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        return Map.copyOf(immutable);
    }

    private static EnterpriseLabCommandLedgerEvent last(
            List<EnterpriseLabCommandLedgerEvent> events) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("command event list cannot be empty");
        }
        return events.get(events.size() - 1);
    }

    private static boolean sameCommandIdentity(
            EnterpriseLabCommandLedgerEvent application,
            EnterpriseLabCommandLedgerEvent supervisor) {
        return application.correlationId().equals(supervisor.correlationId())
                && application.requestFingerprint().equals(supervisor.requestFingerprint())
                && application.transactionId().equals(supervisor.transactionId())
                && application.experimentId().equals(supervisor.experimentId())
                && application.commandType() == supervisor.commandType()
                && application.applicationInstanceId().equals(
                        supervisor.applicationInstanceId())
                && application.applicationOwnerGeneration()
                        == supervisor.applicationOwnerGeneration()
                && application.supervisorInstanceId().equals(
                        supervisor.supervisorInstanceId())
                && application.supervisorGeneration() == supervisor.supervisorGeneration()
                && application.allocationGeneration() == supervisor.allocationGeneration()
                && application.requestedAllocationFingerprint().equals(
                        supervisor.requestedAllocationFingerprint())
                && application.previousCommittedFingerprint().equals(
                        supervisor.previousCommittedFingerprint());
    }

    private static IllegalStateException failure(String message) {
        return new IllegalStateException(message);
    }

    enum Classification {
        NOT_EXECUTED,
        RECEIVED_NOT_EXECUTED,
        REJECTED,
        AMBIGUOUS_MUTATION,
        AMBIGUOUS_MUTATION_RECONCILED,
        SUPERVISOR_COMMIT_RECOVERED,
        OBSERVATION_RECOVERED,
        QUARANTINED
    }

    record PendingAmbiguousMutation(
            String correlationId,
            String applicationHeadFingerprint,
            String supervisorHeadFingerprint) {
        PendingAmbiguousMutation {
            Objects.requireNonNull(correlationId, "correlationId cannot be null");
            Objects.requireNonNull(
                    applicationHeadFingerprint,
                    "applicationHeadFingerprint cannot be null");
            Objects.requireNonNull(
                    supervisorHeadFingerprint,
                    "supervisorHeadFingerprint cannot be null");
        }
    }

    record CommandRepair(
            String correlationId,
            Optional<String> supervisorEventFingerprint,
            Classification classification,
            boolean routerMutationPerformed) {
        CommandRepair {
            Objects.requireNonNull(correlationId, "correlationId cannot be null");
            supervisorEventFingerprint = Objects.requireNonNull(
                    supervisorEventFingerprint,
                    "supervisorEventFingerprint cannot be null");
            classification = Objects.requireNonNull(
                    classification, "classification cannot be null");
            if (routerMutationPerformed) {
                throw new IllegalArgumentException(
                        "command-history repair cannot claim a router mutation");
            }
        }

        static CommandRepair of(
                EnterpriseLabCommandLedgerEvent application,
                Optional<EnterpriseLabCommandLedgerEvent> supervisor,
                Classification classification,
                boolean routerMutationPerformed) {
            return new CommandRepair(
                    application.correlationId(),
                    supervisor.map(
                            EnterpriseLabCommandLedgerEvent::currentFingerprint),
                    classification,
                    routerMutationPerformed);
        }
    }

    record Checkpoint(
            List<PendingAmbiguousMutation> pending,
            List<CommandRepair> repairs,
            int unresolvedBefore) {
        Checkpoint {
            pending = List.copyOf(Objects.requireNonNull(
                    pending, "pending cannot be null"));
            repairs = List.copyOf(Objects.requireNonNull(
                    repairs, "repairs cannot be null"));
            if (unresolvedBefore < 0
                    || unresolvedBefore > HARD_MAX_UNRESOLVED_COMMANDS
                    || pending.size() > unresolvedBefore
                    || repairs.size() > unresolvedBefore) {
                throw new IllegalArgumentException(
                        "command reconciliation checkpoint is outside bounds");
            }
        }

        static Checkpoint empty() {
            return new Checkpoint(List.of(), List.of(), 0);
        }
    }

    record Report(
            int unresolvedBefore,
            int repairedCommands,
            int ambiguousCommandsReconciled,
            List<CommandRepair> repairs,
            boolean ready,
            String reasonCode) {
        Report {
            repairs = List.copyOf(Objects.requireNonNull(
                    repairs, "repairs cannot be null"));
            reasonCode = Objects.requireNonNull(reasonCode, "reasonCode cannot be null");
            if (unresolvedBefore < 0
                    || repairedCommands < 0
                    || ambiguousCommandsReconciled < 0
                    || repairedCommands > HARD_MAX_UNRESOLVED_COMMANDS
                    || ambiguousCommandsReconciled > repairedCommands
                    || repairs.size() != repairedCommands
                    || !ready) {
                throw new IllegalArgumentException(
                        "command reconciliation report is inconsistent");
            }
        }

        static Report empty() {
            return new Report(0, 0, 0, List.of(), true, "NOT_APPLICABLE");
        }
    }

    @FunctionalInterface
    interface SupervisorHistoryReader {
        EnterpriseLabSupervisorCommandLedger.ReadResult read();
    }

    @FunctionalInterface
    interface InstalledStateReader {
        EnterpriseLabInstalledAllocationSnapshot read();
    }
}
