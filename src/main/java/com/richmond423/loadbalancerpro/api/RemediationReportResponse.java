package com.richmond423.loadbalancerpro.api;

public record RemediationReportResponse(
        RemediationReportFormat format,
        String contentType,
        String report,
        RemediationReportPayload json,
        boolean readOnly,
        boolean advisoryOnly,
        boolean cloudMutation) {
}
