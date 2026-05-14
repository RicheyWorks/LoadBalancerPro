package com.richmond423.loadbalancerpro.lab;

import java.time.Instant;
import java.util.List;

public record EnterpriseLabRunSummary(
        String runId,
        Instant createdAt,
        String mode,
        List<String> selectedScenarioIds,
        int policyAuditEventCount,
        EnterpriseLabScorecard scorecard) {

    public static EnterpriseLabRunSummary from(EnterpriseLabRun run) {
        return new EnterpriseLabRunSummary(run.runId(), run.createdAt(), run.mode(),
                run.selectedScenarioIds(), run.policyAuditEvents().size(), run.scorecard());
    }
}

