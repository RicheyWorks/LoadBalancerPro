package com.richmond423.loadbalancerpro.lab;

import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandLedger.AppendReceipt;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabApplicationCommandLedger.ApplicationEventDraft;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Request;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabSupervisorProtocol.Response;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;

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

    @FunctionalInterface
    public interface CommandTransport {
        Response execute(Request request);
    }
}
