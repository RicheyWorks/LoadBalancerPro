package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.AllocationPurpose;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabAllocationState.TransactionPhase;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandLedger.ApplicationEventDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandHistoryReconciler.Classification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ApplicationCommitStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.AuthenticationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.Draft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.DuplicateClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.EventType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.LedgerSide;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.MutationStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ResponseClassification;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.ValidationResult;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.CommandType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.RequestDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseLabCommandHistoryReconcilerTest {
    private static final String SCENARIO = "tail-latency-pressure";
    private static final String OWNERSHIP = "a".repeat(64);
    private static final Instant NOW = Instant.parse("2026-07-20T06:00:00Z");

    @TempDir
    Path root;

    private EnterpriseLabExperimentTargetCatalog targetCatalog;
    private EnterpriseLabSupervisorProtocolCodec protocolCodec;
    private EnterpriseLabCommandLedgerEventCodec eventCodec;
    private EnterpriseLabMutationTestAuthority authority;
    private Clock clock;
    private EnterpriseLabAdaptiveDecision decision;
    private EnterpriseLabLoopbackAllocationRouter router;
    private EnterpriseLabInstalledAllocationSnapshot baselineInstalled;
    private EnterpriseLabAllocationStateStore allocationStore;
    private EnterpriseLabAllocationTransactionCoordinator coordinator;

    @BeforeEach
    void setUp() {
        targetCatalog = new EnterpriseLabExperimentTargetCatalog(targets());
        protocolCodec = new EnterpriseLabSupervisorProtocolCodec(targetCatalog);
        eventCodec = new EnterpriseLabCommandLedgerEventCodec();
        authority = new EnterpriseLabMutationTestAuthority(root);
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        decision = new EnterpriseLabAdaptiveDecisionService()
                .decide(SCENARIO, "active-experiment", true, false, false);
        router = new EnterpriseLabLoopbackAllocationRouter(
                targets(),
                new EnterpriseLabLoopbackObservationIngress(
                        List.of("blue", "green", "orange")),
                decision.decision().guardrailDecision().baselineAllocations(),
                Optional.of(authority),
                clock);
        baselineInstalled = router.installedSnapshot();
        allocationStore = EnterpriseLabAllocationStateStore.createOwned(
                root, targetCatalog, authority);
        coordinator = new EnterpriseLabAllocationTransactionCoordinator(
                allocationStore,
                router,
                targetCatalog,
                authority,
                clock,
                checkpoint -> { },
                router::installedSnapshot);
        coordinator.establishSafeBaseline("allocation-baseline-1");
    }

    @Test
    void intentWithoutSupervisorReceiptIsTerminalizedAsNotExecuted() {
        Request request = observation("intent-only");
        try (EnterpriseLabApplicationCommandLedger application = applicationLedger()) {
            application.append(request, intent(router.installedSnapshot()));
            EnterpriseLabCommandHistoryReconciler reconciler = reconciler(
                    application, List.of());

            EnterpriseLabCommandHistoryReconciler.Checkpoint checkpoint =
                    reconciler.reconcileBeforeAllocation();

            assertEquals(1, checkpoint.unresolvedBefore());
            assertTrue(checkpoint.pending().isEmpty());
            assertEquals(Classification.NOT_EXECUTED,
                    checkpoint.repairs().get(0).classification());
            assertEquals(List.of(
                            EventType.APPLICATION_INTENT_PERSISTED,
                            EventType.RECONCILIATION_COMPLETED,
                            EventType.COMMAND_FAILED),
                    phases(application, request));
            assertTrue(application.replay().unresolvedHeads().isEmpty());
            assertEquals(0L, router.installedSnapshot().routerGeneration());
        }
    }

    @Test
    void receiptWithoutMutationIsClosedWithoutRetryOrRouterChange() {
        Request request = observation("receipt-only");
        EnterpriseLabCommandLedgerEvent receipt = supervisorEvent(
                request,
                1L,
                EventType.SUPERVISOR_RECEIPT_PERSISTED,
                EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT,
                router.installedSnapshot(),
                MutationStatus.NOT_ATTEMPTED,
                ValidationResult.NOT_ATTEMPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE);
        try (EnterpriseLabApplicationCommandLedger application = applicationLedger()) {
            application.append(request, intent(router.installedSnapshot()));
            application.append(request, dispatch(router.installedSnapshot()));

            EnterpriseLabCommandHistoryReconciler.Checkpoint checkpoint =
                    reconciler(application, List.of(receipt))
                            .reconcileBeforeAllocation();

            assertEquals(Classification.RECEIVED_NOT_EXECUTED,
                    checkpoint.repairs().get(0).classification());
            assertEquals(EventType.COMMAND_FAILED,
                    application.replay().eventsFor(request.requestId()).stream()
                            .reduce((first, second) -> second).orElseThrow().eventType());
            assertEquals(0L, router.installedSnapshot().routerGeneration());
        }
    }

    @Test
    void staleGenerationRejectionBecomesTerminalWithoutMutation() {
        Request request = observation("stale-generation");
        EnterpriseLabCommandLedgerEvent receipt = supervisorEvent(
                request,
                1L,
                EventType.SUPERVISOR_RECEIPT_PERSISTED,
                EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT,
                baselineSnapshot(),
                MutationStatus.NOT_ATTEMPTED,
                ValidationResult.NOT_ATTEMPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE);
        EnterpriseLabCommandLedgerEvent stale = eventCodec.issue(request, new Draft(
                LedgerSide.SUPERVISOR,
                2L,
                EventType.DUPLICATE_REJECTED,
                request.requestId(),
                request.requestFingerprint(),
                request.transactionId(),
                request.experimentId(),
                request.commandType(),
                request.applicationInstanceId(),
                request.applicationOwnerGeneration(),
                request.expectedSupervisorInstanceId(),
                request.expectedSupervisorGeneration(),
                0L,
                request.allocationFingerprint(),
                request.previousCommittedFingerprint(),
                baselineSnapshot().allocationFingerprint(),
                baselineSnapshot().allocationFingerprint(),
                0L,
                0L,
                AuthenticationResult.ACCEPTED,
                ValidationResult.ACCEPTED,
                DuplicateClassification.STALE_APPLICATION_GENERATION,
                MutationStatus.NOT_ATTEMPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE,
                EnterpriseLabCommandLedgerEvent.NONE,
                ApplicationCommitStatus.NOT_ATTEMPTED,
                0,
                "STALE_APPLICATION_GENERATION",
                NOW.plusMillis(2),
                Map.of(),
                receipt.currentFingerprint()));
        try (EnterpriseLabApplicationCommandLedger application = applicationLedger()) {
            application.append(request, intent(baselineSnapshot()));
            application.append(request, dispatch(baselineSnapshot()));

            EnterpriseLabCommandHistoryReconciler.Checkpoint checkpoint =
                    reconciler(application, List.of(receipt, stale))
                            .reconcileBeforeAllocation();

            assertEquals(Classification.REJECTED,
                    checkpoint.repairs().get(0).classification());
            assertEquals(EventType.COMMAND_FAILED,
                    application.replay().head().orElseThrow().eventType());
            assertEquals(DuplicateClassification.STALE_APPLICATION_GENERATION,
                    application.replay().head().orElseThrow()
                            .duplicateClassification());
            assertEquals(0L, router.installedSnapshot().routerGeneration());
        }
    }

    @Test
    void acceptedObservationMissingApplicationCommitRepairsOnce() {
        Request request = observation("observation-response-missing");
        EnterpriseLabCommandLedgerEvent receipt = supervisorEvent(
                request,
                1L,
                EventType.SUPERVISOR_RECEIPT_PERSISTED,
                EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT,
                router.installedSnapshot(),
                MutationStatus.NOT_ATTEMPTED,
                ValidationResult.NOT_ATTEMPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE);
        EnterpriseLabCommandLedgerEvent response = supervisorEvent(
                request,
                2L,
                EventType.RESPONSE_SENT,
                receipt.currentFingerprint(),
                router.installedSnapshot(),
                MutationStatus.NOT_ATTEMPTED,
                ValidationResult.ACCEPTED,
                ResponseClassification.SENT,
                "b".repeat(64));
        EnterpriseLabCommandLedgerEvent identicalRetryReceipt = supervisorEvent(
                request,
                3L,
                EventType.SUPERVISOR_RECEIPT_PERSISTED,
                response.currentFingerprint(),
                router.installedSnapshot(),
                MutationStatus.NOT_ATTEMPTED,
                ValidationResult.NOT_ATTEMPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE);
        try (EnterpriseLabApplicationCommandLedger application = applicationLedger()) {
            application.append(request, intent(router.installedSnapshot()));
            application.append(request, dispatch(router.installedSnapshot()));
            application.append(
                    request,
                    new ApplicationEventDraft(
                            EventType.APPLICATION_RESPONSE_RECEIVED,
                            baselineSnapshot().allocationFingerprint(),
                            baselineSnapshot().allocationFingerprint(),
                            0L,
                            0L,
                            AuthenticationResult.ACCEPTED,
                            ValidationResult.ACCEPTED,
                            DuplicateClassification.NOT_EVALUATED,
                            MutationStatus.NOT_ATTEMPTED,
                            ResponseClassification.RECEIVED,
                            response.responseFingerprint(),
                            response.currentFingerprint(),
                            ApplicationCommitStatus.PENDING,
                            0,
                            "SUPERVISOR_RESPONSE_RECEIVED",
                            NOW.plusMillis(3),
                            Map.of("boundary", "restart-reconciliation-test")));
            EnterpriseLabCommandHistoryReconciler reconciler = reconciler(
                    application, List.of(receipt, response, identicalRetryReceipt));

            reconciler.reconcileBeforeAllocation();
            int repairedEvents = application.replay().events().size();
            EnterpriseLabCommandHistoryReconciler.Checkpoint repeated =
                    reconciler.reconcileBeforeAllocation();

            assertEquals(List.of(
                            EventType.APPLICATION_INTENT_PERSISTED,
                            EventType.DISPATCH_ATTEMPTED,
                            EventType.APPLICATION_RESPONSE_RECEIVED,
                            EventType.RECONCILIATION_COMPLETED,
                            EventType.APPLICATION_COMMITTED),
                    phases(application, request));
            assertEquals(response.currentFingerprint(),
                    application.replay().head().orElseThrow()
                            .observedSupervisorEventFingerprint());
            assertEquals(repairedEvents, application.replay().events().size());
            assertEquals(0, repeated.unresolvedBefore());
            assertEquals(0L, router.installedSnapshot().routerGeneration());
        }
    }

    @Test
    void supervisorCommitRepairsAllocationAndApplicationWithoutSecondMutation() {
        prepareCandidateCrashAfterRouterApply();
        EnterpriseLabInstalledAllocationSnapshot candidate = router.installedSnapshot();
        EnterpriseLabAllocationState applying = allocationStore.replay()
                .chainHead().orElseThrow();
        Request request = allocationRequest(applying, candidate);
        List<EnterpriseLabCommandLedgerEvent> supervisor = committedHistory(
                request, applying, candidate);

        try (EnterpriseLabApplicationCommandLedger application = applicationLedger()) {
            application.append(request, intent(baselineSnapshot()));
            application.append(request, dispatch(baselineSnapshot()));
            EnterpriseLabCommandHistoryReconciler reconciler = reconciler(
                    application, supervisor);

            EnterpriseLabCommandHistoryReconciler.Checkpoint checkpoint =
                    reconciler.reconcileBeforeAllocation();

            assertEquals(Classification.SUPERVISOR_COMMIT_RECOVERED,
                    checkpoint.repairs().get(0).classification());
            assertEquals(TransactionPhase.COMMITTED,
                    allocationStore.replay().chainHead().orElseThrow()
                            .transactionPhase());
            assertEquals(EventType.APPLICATION_COMMITTED,
                    application.replay().head().orElseThrow().eventType());
            assertEquals(1L, candidate.routerGeneration());
            assertEquals(candidate, router.installedSnapshot());
            int allocationRecords = allocationStore.replay().records().size();
            int applicationEvents = application.replay().events().size();

            reconciler.reconcileBeforeAllocation();
            assertEquals(allocationRecords, allocationStore.replay().records().size());
            assertEquals(applicationEvents, application.replay().events().size());
            assertEquals(candidate, router.installedSnapshot());
        }
    }

    @Test
    void supervisorCommitInstalledMismatchQuarantinesWithoutRepairMutation() {
        prepareCandidateCrashAfterRouterApply();
        EnterpriseLabInstalledAllocationSnapshot candidate = router.installedSnapshot();
        EnterpriseLabAllocationState applying = allocationStore.replay()
                .chainHead().orElseThrow();
        Request request = allocationRequest(applying, candidate);
        List<EnterpriseLabCommandLedgerEvent> committed = new ArrayList<>(
                committedHistory(request, applying, candidate));
        EnterpriseLabCommandLedgerEvent readBack = committed.get(3);
        committed.set(4, supervisorEvent(
                request,
                5L,
                EventType.SUPERVISOR_COMMITTED,
                readBack.currentFingerprint(),
                baselineSnapshot(),
                MutationStatus.COMMITTED,
                ValidationResult.ACCEPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE));

        try (EnterpriseLabApplicationCommandLedger application = applicationLedger()) {
            application.append(request, intent(baselineSnapshot()));
            application.append(request, dispatch(baselineSnapshot()));

            assertThrows(
                    IllegalStateException.class,
                    () -> reconciler(application, committed)
                            .reconcileBeforeAllocation());

            assertEquals(EventType.COMMAND_QUARANTINED,
                    application.replay().head().orElseThrow().eventType());
            assertEquals(TransactionPhase.APPLYING,
                    allocationStore.replay().chainHead().orElseThrow()
                            .transactionPhase());
            assertEquals(candidate, router.installedSnapshot());
            assertEquals(1L, router.installedSnapshot().routerGeneration());
        }
    }

    @Test
    void ambiguousMutationWaitsForExistingBaselineReconciliation() {
        prepareCandidateCrashAfterRouterApply();
        EnterpriseLabInstalledAllocationSnapshot candidate = router.installedSnapshot();
        EnterpriseLabAllocationState applying = allocationStore.replay()
                .chainHead().orElseThrow();
        Request request = allocationRequest(applying, candidate);
        EnterpriseLabCommandLedgerEvent receipt = supervisorEvent(
                request,
                1L,
                EventType.SUPERVISOR_RECEIPT_PERSISTED,
                EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT,
                baselineSnapshot(),
                MutationStatus.NOT_ATTEMPTED,
                ValidationResult.NOT_ATTEMPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE);
        EnterpriseLabCommandLedgerEvent started = supervisorEvent(
                request,
                2L,
                EventType.MUTATION_STARTED,
                receipt.currentFingerprint(),
                candidate,
                MutationStatus.STARTED,
                ValidationResult.ACCEPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE);

        try (EnterpriseLabApplicationCommandLedger application = applicationLedger()) {
            application.append(request, intent(baselineSnapshot()));
            application.append(request, dispatch(baselineSnapshot()));
            EnterpriseLabCommandHistoryReconciler commandReconciler = reconciler(
                    application, List.of(receipt, started));
            EnterpriseLabCommandHistoryReconciler.Checkpoint checkpoint =
                    commandReconciler.reconcileBeforeAllocation();

            assertEquals(1, checkpoint.pending().size());
            assertEquals(EventType.DISPATCH_ATTEMPTED,
                    application.replay().head().orElseThrow().eventType());
            EnterpriseLabAllocationReconciliationGate gate =
                    EnterpriseLabAllocationReconciliationGate.pending();
            EnterpriseLabAllocationReconciler allocationReconciler =
                    new EnterpriseLabAllocationReconciler(
                            allocationStore,
                            coordinator,
                            router,
                            authority,
                            gate,
                            clock,
                            router::installedSnapshot,
                            ignored -> { });
            EnterpriseLabAllocationReconciler.ReconciliationReport allocationReport =
                    allocationReconciler.reconcile(
                            EnterpriseLabAllocationReconciler.ReconciliationTrigger.STARTUP,
                            List.of());
            assertTrue(allocationReport.ready());
            assertTrue(gate.admissionAllowed());
            assertEquals(EnterpriseLabLoopbackAllocationSnapshot.Kind.RESTORED_BASELINE,
                    router.installedSnapshot().routingSnapshot().kind());

            EnterpriseLabCommandHistoryReconciler.Report report =
                    commandReconciler.reconcileAfterAllocation(
                            checkpoint, allocationReport);

            assertTrue(report.ready());
            assertEquals(1, report.ambiguousCommandsReconciled());
            assertEquals(EventType.COMMAND_FAILED,
                    application.replay().head().orElseThrow().eventType());
            assertEquals(2L, router.installedSnapshot().routerGeneration());
        }
    }

    @Test
    void correlationMismatchIsQuarantinedAndFailsClosedWithoutMutation() {
        Request request = observation("conflicting-correlation");
        EnterpriseLabCommandLedgerEvent conflicting = eventCodec.issue(new Draft(
                LedgerSide.SUPERVISOR,
                1L,
                EventType.SUPERVISOR_RECEIPT_PERSISTED,
                request.requestId(),
                "c".repeat(64),
                request.transactionId(),
                request.experimentId(),
                request.commandType(),
                request.applicationInstanceId(),
                request.applicationOwnerGeneration(),
                request.expectedSupervisorInstanceId(),
                request.expectedSupervisorGeneration(),
                0L,
                request.allocationFingerprint(),
                request.previousCommittedFingerprint(),
                baselineSnapshot().allocationFingerprint(),
                baselineSnapshot().allocationFingerprint(),
                0L,
                0L,
                AuthenticationResult.ACCEPTED,
                ValidationResult.NOT_ATTEMPTED,
                DuplicateClassification.NOT_EVALUATED,
                MutationStatus.NOT_ATTEMPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE,
                EnterpriseLabCommandLedgerEvent.NONE,
                ApplicationCommitStatus.NOT_ATTEMPTED,
                0,
                "AUTHENTICATED_RECEIPT_DURABLE",
                NOW,
                Map.of(),
                EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT));
        try (EnterpriseLabApplicationCommandLedger application = applicationLedger()) {
            application.append(request, intent(baselineSnapshot()));

            assertThrows(
                    IllegalStateException.class,
                    () -> reconciler(application, List.of(conflicting))
                            .reconcileBeforeAllocation());

            assertEquals(EventType.COMMAND_QUARANTINED,
                    application.replay().head().orElseThrow().eventType());
            assertEquals(0L, router.installedSnapshot().routerGeneration());
            assertThrows(
                    IllegalStateException.class,
                    () -> reconciler(application, List.of(conflicting))
                            .reconcileBeforeAllocation());
        }
    }

    private EnterpriseLabCommandHistoryReconciler reconciler(
            EnterpriseLabApplicationCommandLedger application,
            List<EnterpriseLabCommandLedgerEvent> supervisorEvents) {
        EnterpriseLabSupervisorCommandLedger.ReadResult supervisor =
                new EnterpriseLabSupervisorCommandLedger.ReadResult(
                        !supervisorEvents.isEmpty(), supervisorEvents, 0L);
        return new EnterpriseLabCommandHistoryReconciler(
                application,
                () -> supervisor,
                coordinator,
                router::installedSnapshot,
                clock);
    }

    private void prepareCandidateCrashAfterRouterApply() {
        EnterpriseLabAllocationTransactionCoordinator crashing =
                new EnterpriseLabAllocationTransactionCoordinator(
                        allocationStore,
                        router,
                        targetCatalog,
                        authority,
                        clock,
                        checkpoint -> {
                            if (checkpoint
                                    == EnterpriseLabAllocationTransactionCoordinator.Checkpoint
                                    .AFTER_ROUTER_APPLY) {
                                throw new SimulatedCrash();
                            }
                        },
                        router::installedSnapshot);
        assertThrows(SimulatedCrash.class, () -> crashing.applyCandidate(
                "allocation-candidate-2", "experiment-1", decision, true));
        assertEquals(TransactionPhase.APPLYING,
                allocationStore.replay().chainHead().orElseThrow().transactionPhase());
    }

    private Request observation(String correlationId) {
        return protocolCodec.issue(new RequestDraft(
                correlationId,
                CommandType.HEALTH,
                "application-instance-1",
                OWNERSHIP,
                1L,
                "supervisor-instance-1",
                1L,
                EnterpriseLabSupervisorProtocol.NONE,
                Optional.empty(),
                AllocationPurpose.RECONCILIATION_NO_OP,
                Optional.empty(),
                EnterpriseLabSupervisorProtocol.NONE,
                EnterpriseLabSupervisorProtocol.NONE,
                NOW,
                Map.of()));
    }

    private Request allocationRequest(
            EnterpriseLabAllocationState applying,
            EnterpriseLabInstalledAllocationSnapshot candidate) {
        String correlationId =
                EnterpriseLabSupervisorAllocationBridge.commandCorrelationId(
                        applying.allocationTransactionId(),
                        CommandType.APPLY_ALLOCATION,
                        applying.allocationGeneration(),
                        candidate.allocationFingerprint());
        String transactionId =
                EnterpriseLabSupervisorAllocationBridge.supervisorTransactionId(
                        applying.allocationTransactionId(),
                        CommandType.APPLY_ALLOCATION,
                        applying.allocationGeneration(),
                        candidate.allocationFingerprint());
        return protocolCodec.issue(new RequestDraft(
                correlationId,
                CommandType.APPLY_ALLOCATION,
                "application-instance-1",
                OWNERSHIP,
                1L,
                "supervisor-instance-1",
                1L,
                transactionId,
                applying.experimentId(),
                AllocationPurpose.EXPERIMENT_CANDIDATE,
                Optional.of(candidate.routingSnapshot()),
                candidate.allocationFingerprint(),
                applying.previousCommittedAllocationFingerprint(),
                NOW,
                Map.of("applicationAllocationGeneration",
                        Long.toString(applying.allocationGeneration()))));
    }

    private List<EnterpriseLabCommandLedgerEvent> committedHistory(
            Request request,
            EnterpriseLabAllocationState applying,
            EnterpriseLabInstalledAllocationSnapshot candidate) {
        List<EnterpriseLabCommandLedgerEvent> events = new ArrayList<>();
        events.add(supervisorEvent(
                request, 1L, EventType.SUPERVISOR_RECEIPT_PERSISTED,
                EnterpriseLabCommandLedgerEvent.GENESIS_FINGERPRINT,
                baselineSnapshot(), MutationStatus.NOT_ATTEMPTED,
                ValidationResult.NOT_ATTEMPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE));
        events.add(supervisorEvent(
                request, 2L, EventType.MUTATION_STARTED,
                events.get(events.size() - 1).currentFingerprint(),
                candidate, MutationStatus.STARTED, ValidationResult.ACCEPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE));
        events.add(supervisorEvent(
                request, 3L, EventType.ALLOCATION_APPLIED,
                events.get(events.size() - 1).currentFingerprint(),
                candidate, MutationStatus.APPLIED, ValidationResult.ACCEPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE));
        events.add(supervisorEvent(
                request, 4L, EventType.READ_BACK_VERIFIED,
                events.get(events.size() - 1).currentFingerprint(),
                candidate, MutationStatus.READ_BACK_VERIFIED,
                ValidationResult.ACCEPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE));
        events.add(supervisorEvent(
                request, 5L, EventType.SUPERVISOR_COMMITTED,
                events.get(events.size() - 1).currentFingerprint(),
                candidate, MutationStatus.COMMITTED, ValidationResult.ACCEPTED,
                ResponseClassification.NOT_ATTEMPTED,
                EnterpriseLabCommandLedgerEvent.NONE));
        assertEquals(applying.normalizedAllocationFingerprint(),
                events.get(events.size() - 1).installedFingerprintAfter());
        return List.copyOf(events);
    }

    private EnterpriseLabCommandLedgerEvent supervisorEvent(
            Request request,
            long sequence,
            EventType type,
            String predecessor,
            EnterpriseLabInstalledAllocationSnapshot installed,
            MutationStatus mutation,
            ValidationResult validation,
            ResponseClassification response,
            String responseFingerprint) {
        long allocationGeneration = request.metadata().containsKey(
                "applicationAllocationGeneration")
                ? Long.parseLong(request.metadata().get(
                        "applicationAllocationGeneration")) : 0L;
        return eventCodec.issue(request, new Draft(
                LedgerSide.SUPERVISOR,
                sequence,
                type,
                request.requestId(),
                request.requestFingerprint(),
                request.transactionId(),
                request.experimentId(),
                request.commandType(),
                request.applicationInstanceId(),
                request.applicationOwnerGeneration(),
                request.expectedSupervisorInstanceId(),
                request.expectedSupervisorGeneration(),
                allocationGeneration,
                request.allocationFingerprint(),
                request.previousCommittedFingerprint(),
                installed.allocationFingerprint(),
                installed.allocationFingerprint(),
                Math.max(0L, installed.routerGeneration() - 1L),
                installed.routerGeneration(),
                AuthenticationResult.ACCEPTED,
                validation,
                DuplicateClassification.FIRST_OBSERVATION,
                mutation,
                response,
                responseFingerprint,
                EnterpriseLabCommandLedgerEvent.NONE,
                ApplicationCommitStatus.NOT_ATTEMPTED,
                0,
                reason(type),
                NOW.plusMillis(sequence),
                Map.of(),
                predecessor));
    }

    private static String reason(EventType type) {
        return switch (type) {
            case SUPERVISOR_RECEIPT_PERSISTED -> "AUTHENTICATED_RECEIPT_DURABLE";
            case MUTATION_STARTED -> "MUTATION_STARTED";
            case ALLOCATION_APPLIED -> "ALLOCATION_APPLIED";
            case READ_BACK_VERIFIED -> "READ_BACK_VERIFIED";
            case SUPERVISOR_COMMITTED -> "SUPERVISOR_COMMITTED";
            case RESPONSE_SENT -> "SUPERVISOR_RESPONSE_SENT";
            default -> throw new IllegalArgumentException("unsupported test event");
        };
    }

    private static ApplicationEventDraft intent(
            EnterpriseLabInstalledAllocationSnapshot installed) {
        return ApplicationEventDraft.intent(
                installed.allocationFingerprint(),
                installed.routerGeneration(),
                NOW,
                Map.of("boundary", "restart-reconciliation-test"));
    }

    private static ApplicationEventDraft dispatch(
            EnterpriseLabInstalledAllocationSnapshot installed) {
        return ApplicationEventDraft.dispatch(
                installed.allocationFingerprint(),
                installed.routerGeneration(),
                NOW.plusMillis(1),
                Map.of("boundary", "restart-reconciliation-test"));
    }

    private EnterpriseLabInstalledAllocationSnapshot baselineSnapshot() {
        return baselineInstalled;
    }

    private EnterpriseLabApplicationCommandLedger applicationLedger() {
        return EnterpriseLabApplicationCommandLedger.createOwned(root, authority);
    }

    private static List<EventType> phases(
            EnterpriseLabApplicationCommandLedger application,
            Request request) {
        return application.replay().eventsFor(request.requestId()).stream()
                .map(EnterpriseLabCommandLedgerEvent::eventType)
                .toList();
    }

    private static List<EnterpriseLabLoopbackTarget> targets() {
        return List.of(
                new EnterpriseLabLoopbackTarget(
                        SCENARIO,
                        "blue",
                        URI.create("http://127.0.0.1:18081/health")),
                new EnterpriseLabLoopbackTarget(
                        SCENARIO,
                        "green",
                        URI.create("http://127.0.0.1:18082/health")),
                new EnterpriseLabLoopbackTarget(
                        SCENARIO,
                        "orange",
                        URI.create("http://127.0.0.1:18083/health")));
    }

    private static final class SimulatedCrash extends RuntimeException {
    }
}
