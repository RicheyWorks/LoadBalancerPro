package com.richmond423.loadbalancerpro.api.proxy;

import java.util.ArrayList;
import java.util.List;

public record PrivateNetworkLiveValidationCommandResponse(
        boolean accepted,
        boolean executable,
        boolean trafficExecuted,
        String status,
        String gateStatus,
        boolean allowedByGate,
        String message,
        String requestPath,
        boolean evidenceRequested,
        boolean evidenceWritten,
        boolean evidenceEligible,
        String plannedEvidenceDirectory,
        String plannedEvidenceMarkdown,
        String plannedEvidenceJson,
        boolean redactionRequired,
        String trafficExecution,
        AuditTrailContract auditTrail,
        boolean operatorAcknowledged,
        PrivateNetworkLiveValidationStatusResponse gate,
        List<String> reasonCodes,
        List<String> reasons) {
    private static final String NOT_WIRED_MESSAGE =
            "traffic execution is not wired in this release";
    static final String PLANNED_EVIDENCE_DIRECTORY = "target/proxy-evidence/";
    static final String PLANNED_EVIDENCE_MARKDOWN = "private-network-live-validation.md";
    static final String PLANNED_EVIDENCE_JSON = "private-network-live-validation.json";
    private static final String PLANNED_AUDIT_TRAIL =
            "target/proxy-evidence/private-network-live-validation-audit.jsonl";
    private static final List<String> PLANNED_AUDIT_FIELDS = List.of(
            "requestPath",
            "gateStatus",
            "reasonCodes",
            "trafficExecuted",
            "evidenceWritten",
            "redactionRequired");

    public PrivateNetworkLiveValidationCommandResponse {
        status = status == null ? "" : status;
        message = message == null ? "" : message;
        requestPath = requestPath == null ? "" : requestPath;
        plannedEvidenceDirectory = plannedEvidenceDirectory == null || plannedEvidenceDirectory.isBlank()
                ? PLANNED_EVIDENCE_DIRECTORY
                : plannedEvidenceDirectory;
        plannedEvidenceMarkdown = plannedEvidenceMarkdown == null || plannedEvidenceMarkdown.isBlank()
                ? PLANNED_EVIDENCE_MARKDOWN
                : plannedEvidenceMarkdown;
        plannedEvidenceJson = plannedEvidenceJson == null || plannedEvidenceJson.isBlank()
                ? PLANNED_EVIDENCE_JSON
                : plannedEvidenceJson;
        trafficExecution = trafficExecution == null || trafficExecution.isBlank()
                ? NOT_WIRED_MESSAGE
                : trafficExecution;
        auditTrail = auditTrail == null ? AuditTrailContract.planned(false) : auditTrail;
        gate = gate == null ? PrivateNetworkLiveValidationStatusResponse.from(new ReverseProxyProperties()) : gate;
        gateStatus = gate.gateStatus();
        allowedByGate = gate.allowedByGate();
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
                    gate.gateStatus(),
                    gate.allowedByGate(),
                    "private-network live validation command is blocked by the offline gate; "
                            + NOT_WIRED_MESSAGE,
                    safeRequestPath,
                    request.evidenceRequestedFlag(),
                    false,
                    false,
                    PLANNED_EVIDENCE_DIRECTORY,
                    PLANNED_EVIDENCE_MARKDOWN,
                    PLANNED_EVIDENCE_JSON,
                    true,
                    NOT_WIRED_MESSAGE,
                    AuditTrailContract.planned(false),
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
                gate.gateStatus(),
                gate.allowedByGate(),
                NOT_WIRED_MESSAGE,
                safeRequestPath,
                request.evidenceRequestedFlag(),
                false,
                request.evidenceRequestedFlag(),
                PLANNED_EVIDENCE_DIRECTORY,
                PLANNED_EVIDENCE_MARKDOWN,
                PLANNED_EVIDENCE_JSON,
                true,
                NOT_WIRED_MESSAGE,
                AuditTrailContract.planned(true),
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
                gate.gateStatus(),
                gate.allowedByGate(),
                "private-network live validation command request is invalid; " + NOT_WIRED_MESSAGE,
                "",
                evidenceRequested,
                false,
                false,
                PLANNED_EVIDENCE_DIRECTORY,
                PLANNED_EVIDENCE_MARKDOWN,
                PLANNED_EVIDENCE_JSON,
                true,
                NOT_WIRED_MESSAGE,
                AuditTrailContract.planned(false),
                operatorAcknowledged,
                gate,
                reasonCodes,
                reasons);
    }

    public record AuditTrailContract(
            boolean auditTrailEligible,
            boolean auditTrailWritten,
            String plannedAuditTrail,
            List<String> plannedFields) {
        public AuditTrailContract {
            plannedAuditTrail = plannedAuditTrail == null || plannedAuditTrail.isBlank()
                    ? PLANNED_AUDIT_TRAIL
                    : plannedAuditTrail;
            plannedFields = plannedFields == null ? List.of() : List.copyOf(plannedFields);
        }

        static AuditTrailContract planned(boolean eligible) {
            return new AuditTrailContract(eligible, false, PLANNED_AUDIT_TRAIL, PLANNED_AUDIT_FIELDS);
        }
    }
}
