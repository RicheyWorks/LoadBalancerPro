package com.richmond423.loadbalancerpro.api.proxy;

import java.util.ArrayList;
import java.util.List;

public record PrivateNetworkLiveValidationCommandResponse(
        boolean accepted,
        boolean executable,
        boolean trafficExecuted,
        String status,
        String message,
        String requestPath,
        boolean evidenceRequested,
        boolean evidenceWritten,
        boolean operatorAcknowledged,
        PrivateNetworkLiveValidationStatusResponse gate,
        List<String> reasonCodes,
        List<String> reasons) {
    private static final String NOT_WIRED_MESSAGE =
            "traffic execution is not wired in this release";

    public PrivateNetworkLiveValidationCommandResponse {
        status = status == null ? "" : status;
        message = message == null ? "" : message;
        requestPath = requestPath == null ? "" : requestPath;
        gate = gate == null ? PrivateNetworkLiveValidationStatusResponse.from(new ReverseProxyProperties()) : gate;
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    static PrivateNetworkLiveValidationCommandResponse from(ReverseProxyProperties properties,
                                                            PrivateNetworkLiveValidationCommandRequest request) {
        PrivateNetworkLiveValidationStatusResponse gate =
                PrivateNetworkLiveValidationStatusResponse.from(properties);
        if (request == null) {
            return invalid(gate, false, false,
                    "validation command request is required",
                    List.of("INVALID_REQUEST"));
        }

        String invalidPathReason =
                PrivateNetworkLiveValidationRequestPathValidator.invalidReason(request.requestPath());
        if (!invalidPathReason.isBlank()) {
            return invalid(gate, request.evidenceRequestedFlag(), request.operatorAcknowledgedFlag(),
                    invalidPathReason,
                    List.of("INVALID_REQUEST_PATH"));
        }

        String safeRequestPath =
                PrivateNetworkLiveValidationRequestPathValidator.safePathOrEmpty(request.requestPath());
        if (!gate.allowedByGate()) {
            List<String> reasonCodes = gate.reasonCodes().isEmpty()
                    ? List.of("GATE_BLOCKED")
                    : gate.reasonCodes();
            return new PrivateNetworkLiveValidationCommandResponse(
                    false,
                    false,
                    false,
                    "BLOCKED_BY_GATE",
                    "private-network live validation command is blocked by the offline gate; "
                            + NOT_WIRED_MESSAGE,
                    safeRequestPath,
                    request.evidenceRequestedFlag(),
                    false,
                    request.operatorAcknowledgedFlag(),
                    gate,
                    reasonCodes,
                    gate.reasons());
        }

        return new PrivateNetworkLiveValidationCommandResponse(
                false,
                false,
                false,
                "NOT_IMPLEMENTED",
                NOT_WIRED_MESSAGE,
                safeRequestPath,
                request.evidenceRequestedFlag(),
                false,
                request.operatorAcknowledgedFlag(),
                gate,
                List.of("LIVE_VALIDATION_EXECUTION_NOT_WIRED"),
                List.of(NOT_WIRED_MESSAGE));
    }

    private static PrivateNetworkLiveValidationCommandResponse invalid(
            PrivateNetworkLiveValidationStatusResponse gate,
            boolean evidenceRequested,
            boolean operatorAcknowledged,
            String reason,
            List<String> reasonCodes) {
        List<String> reasons = new ArrayList<>();
        reasons.add(reason);
        reasons.add(NOT_WIRED_MESSAGE);
        return new PrivateNetworkLiveValidationCommandResponse(
                false,
                false,
                false,
                "INVALID_REQUEST",
                "private-network live validation command request is invalid; " + NOT_WIRED_MESSAGE,
                "",
                evidenceRequested,
                false,
                operatorAcknowledged,
                gate,
                reasonCodes,
                reasons);
    }
}
