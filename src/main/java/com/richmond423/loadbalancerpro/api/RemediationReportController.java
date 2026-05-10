package com.richmond423.loadbalancerpro.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/remediation")
public class RemediationReportController {
    private final RemediationReportService remediationReportService;

    public RemediationReportController(RemediationReportService remediationReportService) {
        this.remediationReportService = remediationReportService;
    }

    @PostMapping("/report")
    public RemediationReportResponse report(@Valid @RequestBody RemediationReportRequest request) {
        return remediationReportService.export(request);
    }
}
