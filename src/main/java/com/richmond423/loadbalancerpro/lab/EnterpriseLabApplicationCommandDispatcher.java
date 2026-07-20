package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandLedger.AppendReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandLedger.ApplicationEventDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabCommandLedgerEvent.EventType;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.ResponseStatus;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Application dispatch boundary that persists canonical intent and dispatch
 * evidence before invoking any supervisor transport supplied by the caller.
 */
public final class EnterpriseLabApplicationCommandDispatcher {
    private final EnterpriseLabApplicationCommandLedger ledger;
    private final Clock clock;

    public EnterpriseLabApplicationCommandDispatcher(
            EnterpriseLabApplicationCommandLedger ledger,
            Clock clock) {
        this.ledger = Objects.requireNonNull(ledger, "ledger cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    /**
     * The transport is unreachable until both forced appends verify exactly.
     * A transport failure leaves a durable dispatch-attempt head for later
     * bounded timeout/loss/reconciliation handling; this method does not infer
     * a remote outcome from a local exception.
     */
    public DispatchResult dispatch(
            Request request,
            DispatchEvidence evidence,
            CommandTransport transport) {
        Request safeRequest = Objects.requireNonNull(request, "request cannot be null");
        DispatchEvidence safeEvidence = Objects.requireNonNull(
                evidence, "evidence cannot be null");
        CommandTransport safeTransport = Objects.requireNonNull(
                transport, "transport cannot be null");

        AppendReceipt intent = ledger.append(
                safeRequest,
                ApplicationEventDraft.intent(
                        safeEvidence.installedFingerprintBefore(),
                        safeEvidence.routerGenerationBefore(),
                        clock.instant(),
                        safeEvidence.metadata()));
        AppendReceipt dispatch = ledger.append(
                safeRequest,
                ApplicationEventDraft.dispatch(
                        safeEvidence.installedFingerprintBefore(),
                        safeEvidence.routerGenerationBefore(),
                        clock.instant(),
                        safeEvidence.metadata()));
        Response response = Objects.requireNonNull(
                safeTransport.execute(safeRequest),
                "transport response cannot be null");
        if (!response.validatesAgainst(safeRequest)) {
            throw new IllegalStateException(
                    "transport response does not match the durable command identity");
        }
        return new DispatchResult(intent, dispatch, response);
    }

    /**
     * Executes the live cross-process evidence boundary. Allocation mutations
     * deliberately defer the terminal application append until the existing
     * allocation coordinator reports its own exact durable commit.
     */
    VerifiedDispatchResult dispatchVerified(
            Request request,
            DispatchEvidence evidence,
            CommandTransport transport,
            SupervisorOutcomeReader outcomeReader,
            boolean deferAcceptedCommit) {
        Request safeRequest = Objects.requireNonNull(request, "request cannot be null");
        DispatchEvidence safeEvidence = Objects.requireNonNull(
                evidence, "evidence cannot be null");
        CommandTransport safeTransport = Objects.requireNonNull(
                transport, "transport cannot be null");
        SupervisorOutcomeReader safeReader = Objects.requireNonNull(
                outcomeReader, "outcomeReader cannot be null");
        boolean allocationMutation = safeRequest.commandType().classification()
                == EnterpriseLabSupervisorProtocol.CommandClassification.ALLOCATION_MUTATION;
        if (allocationMutation != deferAcceptedCommit) {
            throw new IllegalArgumentException(
                    "allocation commands must defer application commit to the allocation coordinator");
        }

        List<EnterpriseLabCommandLedgerEvent> prior = ledger.replay()
                .eventsFor(safeRequest.requestId());
        AppendReceipt intent;
        int retryAttempt = 0;
        if (prior.isEmpty()) {
            intent = ledger.append(
                    safeRequest,
                    ApplicationEventDraft.intent(
                            safeEvidence.installedFingerprintBefore(),
                            safeEvidence.routerGenerationBefore(),
                            safeRequest.requestedAt(),
                            safeEvidence.metadata()));
        } else {
            EnterpriseLabCommandLedgerEvent first = prior.get(0);
            EnterpriseLabCommandLedgerEvent head = prior.get(prior.size() - 1);
            if (!first.correlates(safeRequest)) {
                throw new IllegalStateException(
                        "durable correlation was reused with changed command content");
            }
            if (head.eventType() != EventType.RESPONSE_LOST
                    && head.eventType() != EventType.TIMEOUT_OBSERVED) {
                throw new IllegalStateException(
                        "durable command is not eligible for another dispatch attempt");
            }
            retryAttempt = Math.toIntExact(prior.stream()
                    .filter(event -> event.eventType() == EventType.RETRY_ISSUED)
                    .count() + 1L);
            intent = new AppendReceipt(
                    first.sequence(),
                    first.correlationId(),
                    first.currentFingerprint(),
                    ledger.replay().totalBytes(),
                    EnterpriseLabApplicationCommandLedger.SyncPolicy.FORCE_DATA_AND_METADATA,
                    true);
            ledger.append(
                    safeRequest,
                    ApplicationEventDraft.retry(
                            safeEvidence.installedFingerprintBefore(),
                            safeEvidence.routerGenerationBefore(),
                            retryAttempt,
                            clock.instant(),
                            safeEvidence.metadata()));
        }

        AppendReceipt dispatch = ledger.append(
                safeRequest,
                ApplicationEventDraft.dispatch(
                        safeEvidence.installedFingerprintBefore(),
                        safeEvidence.routerGenerationBefore(),
                        clock.instant(),
                        safeEvidence.metadata()));
        Response response;
        try {
            response = Objects.requireNonNull(
                    safeTransport.execute(safeRequest),
                    "transport response cannot be null");
        } catch (RuntimeException exception) {
            boolean timedOut = exception instanceof EnterpriseLabSupervisorClient.ClientException
                    && ((EnterpriseLabSupervisorClient.ClientException) exception).failure()
                    == EnterpriseLabSupervisorClient.Failure.RESPONSE_TIMEOUT;
            ledger.append(
                    safeRequest,
                    ApplicationEventDraft.transportFailure(
                            safeEvidence.installedFingerprintBefore(),
                            safeEvidence.routerGenerationBefore(),
                            timedOut,
                            clock.instant(),
                            safeEvidence.metadata()));
            throw exception;
        }
        if (!response.validatesAgainst(safeRequest)
                || !response.supervisorInstanceId().equals(
                        safeRequest.expectedSupervisorInstanceId())
                || response.supervisorGeneration()
                        != safeRequest.expectedSupervisorGeneration()) {
            throw new IllegalStateException(
                    "transport response does not match the durable command and supervisor epoch");
        }

        EnterpriseLabCommandLedgerEvent supervisorOutcome = Objects.requireNonNull(
                safeReader.read(safeRequest, response),
                "supervisor outcome cannot be null");
        requireSupervisorOutcome(safeRequest, response, supervisorOutcome);
        AppendReceipt responseReceipt = ledger.append(
                safeRequest,
                response,
                ApplicationEventDraft.responseReceived(
                        safeEvidence.installedFingerprintBefore(),
                        safeEvidence.routerGenerationBefore(),
                        response,
                        supervisorOutcome.currentFingerprint(),
                        clock.instant(),
                        safeEvidence.metadata()));

        Optional<AppendReceipt> terminalReceipt = Optional.empty();
        if (response.status() != ResponseStatus.ACCEPTED) {
            terminalReceipt = Optional.of(ledger.append(
                    safeRequest,
                    response,
                    ApplicationEventDraft.responseRejected(
                            safeEvidence.installedFingerprintBefore(),
                            safeEvidence.routerGenerationBefore(),
                            response,
                            supervisorOutcome.currentFingerprint(),
                            clock.instant(),
                            safeEvidence.metadata())));
        } else if (!deferAcceptedCommit) {
            terminalReceipt = Optional.of(commit(
                    safeRequest,
                    safeEvidence,
                    response,
                    supervisorOutcome.currentFingerprint()));
        }
        return new VerifiedDispatchResult(
                intent,
                dispatch,
                responseReceipt,
                terminalReceipt,
                response,
                supervisorOutcome.currentFingerprint(),
                retryAttempt,
                safeRequest,
                safeEvidence);
    }

    AppendReceipt commit(VerifiedDispatchResult result) {
        VerifiedDispatchResult safe = Objects.requireNonNull(
                result, "result cannot be null");
        if (safe.response().status() != ResponseStatus.ACCEPTED
                || safe.terminalReceipt().isPresent()) {
            throw new IllegalStateException(
                    "only an unterminated accepted command can be application-committed");
        }
        EnterpriseLabCommandLedgerEvent responseEvent = ledger.replay()
                .eventsFor(safe.response().requestId()).stream()
                .reduce((first, second) -> second)
                .orElseThrow();
        if (responseEvent.eventType() != EventType.APPLICATION_RESPONSE_RECEIVED
                || !responseEvent.observes(safe.response())
                || !responseEvent.observedSupervisorEventFingerprint().equals(
                        safe.supervisorOutcomeFingerprint())) {
            throw new IllegalStateException(
                    "application commit requires the exact pending response evidence");
        }
        return commit(
                safe.request(),
                safe.evidence(),
                safe.response(),
                responseEvent.observedSupervisorEventFingerprint());
    }

    private AppendReceipt commit(
            Request request,
            DispatchEvidence evidence,
            Response response,
            String supervisorEventFingerprint) {
        return ledger.append(
                request,
                response,
                ApplicationEventDraft.committed(
                        evidence.installedFingerprintBefore(),
                        evidence.routerGenerationBefore(),
                        response,
                        supervisorEventFingerprint,
                        clock.instant(),
                        evidence.metadata()));
    }

    private static void requireSupervisorOutcome(
            Request request,
            Response response,
            EnterpriseLabCommandLedgerEvent outcome) {
        if (outcome.ledgerSide()
                != EnterpriseLabCommandLedgerEvent.LedgerSide.SUPERVISOR
                || !outcome.correlates(request)) {
            throw new IllegalStateException(
                    "supervisor outcome is not bound to the exact durable request");
        }
        if (outcome.eventType() == EventType.RESPONSE_SENT) {
            if (!outcome.observes(response)) {
                throw new IllegalStateException(
                        "supervisor response evidence is not bound to the exact response");
            }
            return;
        }
        if (response.status() == ResponseStatus.ACCEPTED
                && request.commandType().classification()
                == EnterpriseLabSupervisorProtocol.CommandClassification.ALLOCATION_MUTATION
                && outcome.eventType() != EventType.SUPERVISOR_COMMITTED
                && outcome.eventType() != EventType.DUPLICATE_ACCEPTED) {
            throw new IllegalStateException(
                    "accepted allocation response lacks durable supervisor completion");
        }
        if (response.status() == ResponseStatus.REJECTED
                && outcome.eventType() != EventType.VALIDATION_REJECTED
                && outcome.eventType() != EventType.DUPLICATE_REJECTED) {
            throw new IllegalStateException(
                    "rejected response lacks durable supervisor rejection evidence");
        }
        if (response.status() == ResponseStatus.FAILED
                && outcome.eventType() != EventType.COMMAND_FAILED
                && outcome.eventType() != EventType.COMMAND_QUARANTINED) {
            throw new IllegalStateException(
                    "failed response lacks durable supervisor failure evidence");
        }
        if (!EnterpriseLabCommandLedgerEvent.NONE.equals(
                outcome.installedFingerprintAfter())
                && !outcome.installedFingerprintAfter().equals(
                        response.installedFingerprint())) {
            throw new IllegalStateException(
                    "supervisor outcome and response disagree on installed state");
        }
    }

    public record DispatchEvidence(
            String installedFingerprintBefore,
            long routerGenerationBefore,
            Map<String, String> metadata) {

        public DispatchEvidence {
            installedFingerprintBefore = Objects.requireNonNull(
                    installedFingerprintBefore,
                    "installedFingerprintBefore cannot be null");
            if (routerGenerationBefore < 0L
                    || routerGenerationBefore
                    > EnterpriseLabAllocationState.HARD_MAX_ALLOCATION_GENERATION) {
                throw new IllegalArgumentException(
                        "routerGenerationBefore is outside hard bounds");
            }
            metadata = Map.copyOf(Objects.requireNonNull(
                    metadata, "metadata cannot be null"));
        }
    }

    public record DispatchResult(
            AppendReceipt intentReceipt,
            AppendReceipt dispatchReceipt,
            Response response) {

        public DispatchResult {
            intentReceipt = Objects.requireNonNull(
                    intentReceipt, "intentReceipt cannot be null");
            dispatchReceipt = Objects.requireNonNull(
                    dispatchReceipt, "dispatchReceipt cannot be null");
            response = Objects.requireNonNull(response, "response cannot be null");
            if (!intentReceipt.correlationId().equals(dispatchReceipt.correlationId())
                    || dispatchReceipt.sequence() != intentReceipt.sequence() + 1L) {
                throw new IllegalArgumentException(
                        "dispatch receipts must be one contiguous command correlation");
            }
        }
    }

    record VerifiedDispatchResult(
            AppendReceipt intentReceipt,
            AppendReceipt dispatchReceipt,
            AppendReceipt responseReceipt,
            Optional<AppendReceipt> terminalReceipt,
            Response response,
            String supervisorOutcomeFingerprint,
            int retryAttempt,
            Request request,
            DispatchEvidence evidence) {

        VerifiedDispatchResult {
            intentReceipt = Objects.requireNonNull(intentReceipt, "intentReceipt cannot be null");
            dispatchReceipt = Objects.requireNonNull(dispatchReceipt, "dispatchReceipt cannot be null");
            responseReceipt = Objects.requireNonNull(responseReceipt, "responseReceipt cannot be null");
            terminalReceipt = Objects.requireNonNull(terminalReceipt, "terminalReceipt cannot be null");
            response = Objects.requireNonNull(response, "response cannot be null");
            supervisorOutcomeFingerprint = Objects.requireNonNull(
                    supervisorOutcomeFingerprint, "supervisorOutcomeFingerprint cannot be null");
            request = Objects.requireNonNull(request, "request cannot be null");
            evidence = Objects.requireNonNull(evidence, "evidence cannot be null");
            if (retryAttempt < 0) {
                throw new IllegalArgumentException("retryAttempt cannot be negative");
            }
        }
    }

    @FunctionalInterface
    public interface CommandTransport {
        Response execute(Request request);
    }

    @FunctionalInterface
    interface SupervisorOutcomeReader {
        EnterpriseLabCommandLedgerEvent read(Request request, Response response);
    }
}
