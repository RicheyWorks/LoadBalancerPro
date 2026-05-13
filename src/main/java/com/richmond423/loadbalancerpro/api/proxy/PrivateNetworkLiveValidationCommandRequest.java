package com.richmond423.loadbalancerpro.api.proxy;

public record PrivateNetworkLiveValidationCommandRequest(
        String requestPath,
        Boolean evidenceRequested,
        Boolean operatorAcknowledged,
    String operatorAcknowledgement) {
    public PrivateNetworkLiveValidationCommandRequest {
        requestPath = requestPath == null ? "" : requestPath;
        evidenceRequested = Boolean.TRUE.equals(evidenceRequested);
        boolean acknowledged = Boolean.TRUE.equals(operatorAcknowledged)
                || (operatorAcknowledgement != null && !operatorAcknowledgement.isBlank());
        operatorAcknowledged = acknowledged;
        operatorAcknowledgement = acknowledged ? "<acknowledged>" : "";
    }

    boolean evidenceRequestedFlag() {
        return Boolean.TRUE.equals(evidenceRequested);
    }

    boolean operatorAcknowledgedFlag() {
        return Boolean.TRUE.equals(operatorAcknowledged);
    }
}
