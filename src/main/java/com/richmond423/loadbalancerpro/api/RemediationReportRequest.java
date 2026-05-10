package com.richmond423.loadbalancerpro.api;

import jakarta.validation.Valid;

public record RemediationReportRequest(
        String reportId,
        String title,
        RemediationReportFormat format,

        @Valid
        AllocationEvaluationResponse evaluation,

        @Valid
        ScenarioReplayResponse replay) {
}
