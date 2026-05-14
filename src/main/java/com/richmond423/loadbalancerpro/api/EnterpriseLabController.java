package com.richmond423.loadbalancerpro.api;

import com.richmond423.loadbalancerpro.api.config.AdaptiveRoutingPolicyProperties;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyAuditEvent;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyAuditLog;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPrometheusFormatter;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingObservabilityMetrics;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingObservabilitySnapshot;
import com.richmond423.loadbalancerpro.core.AdaptiveRoutingPolicyStatus;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabRun;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabRunService;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabRunSummary;
import com.richmond423.loadbalancerpro.lab.EnterpriseLabScenarioMetadata;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lab")
public class EnterpriseLabController {
    private final EnterpriseLabRunService runService;
    private final AdaptiveRoutingPolicyProperties policyProperties;

    public EnterpriseLabController(EnterpriseLabRunService runService,
                                   AdaptiveRoutingPolicyProperties policyProperties) {
        this.runService = runService;
        this.policyProperties = policyProperties;
    }

    @GetMapping("/scenarios")
    public EnterpriseLabScenarioCatalogResponse scenarios() {
        List<EnterpriseLabScenarioMetadata> scenarios = runService.listScenarioMetadata();
        return new EnterpriseLabScenarioCatalogResponse(
                scenarios.size(),
                scenarios,
                "adaptive-routing-fixtures-v1",
                List.of("off", "shadow", "recommend", "active-experiment"),
                "lab evidence only / not production activation");
    }

    @GetMapping("/scenarios/{scenarioId}")
    public ResponseEntity<?> scenario(@PathVariable("scenarioId") String scenarioId, HttpServletRequest request) {
        return runService.findScenarioMetadata(scenarioId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiErrorResponse.notFound("Unknown enterprise lab scenario: " + scenarioId,
                                request.getRequestURI())));
    }

    @PostMapping("/runs")
    public EnterpriseLabRun createRun(@RequestBody(required = false) EnterpriseLabRunRequest request) {
        EnterpriseLabRunRequest safeRequest = request == null
                ? new EnterpriseLabRunRequest(null, null, "summary")
                : request;
        return runService.run(safeRequest.scenarioIds(), safeRequest.mode(), safeRequest.detailLevel());
    }

    @GetMapping("/runs")
    public EnterpriseLabRunListResponse runs() {
        List<EnterpriseLabRunSummary> runs = runService.listRunSummaries();
        return new EnterpriseLabRunListResponse(
                runs.size(),
                runs,
                "process-local in-memory bounded store",
                runService.maxRetainedRuns());
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<?> run(@PathVariable("runId") String runId, HttpServletRequest request) {
        return runService.findRun(runId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiErrorResponse.notFound("Unknown enterprise lab run: " + runId,
                                request.getRequestURI())));
    }

    @GetMapping("/policy")
    public AdaptiveRoutingPolicyStatus policy() {
        return runService.policyStatus(policyProperties.getMode(), policyProperties.isActiveExperimentEnabled());
    }

    @GetMapping("/audit-events")
    public EnterpriseLabAuditEventListResponse auditEvents() {
        List<AdaptiveRoutingPolicyAuditEvent> events = runService.policyAuditEvents();
        return new EnterpriseLabAuditEventListResponse(
                events.size(),
                events,
                "process-local bounded audit log",
                "audit events contain policy decisions and guardrails only; no secrets or production certification");
    }

    @GetMapping("/metrics")
    public AdaptiveRoutingObservabilitySnapshot metrics() {
        return runService.observabilitySnapshot();
    }

    @GetMapping(value = "/metrics/prometheus", produces = MediaType.TEXT_PLAIN_VALUE)
    public String prometheusMetrics() {
        return AdaptiveRoutingPrometheusFormatter.format(runService.observabilitySnapshot());
    }

    public record EnterpriseLabScenarioCatalogResponse(
            int count,
            List<EnterpriseLabScenarioMetadata> scenarios,
            String deterministicFixtureVersion,
            List<String> supportedModes,
            String finalRecommendation) {
    }

    public record EnterpriseLabRunRequest(
            List<String> scenarioIds,
            String mode,
            String detailLevel) {
    }

    public record EnterpriseLabRunListResponse(
            int count,
            List<EnterpriseLabRunSummary> runs,
            String storageMode,
            int maxRetainedRuns) {
    }

    public record EnterpriseLabAuditEventListResponse(
            int count,
            List<AdaptiveRoutingPolicyAuditEvent> events,
            String storageMode,
            String warning) {
    }

    @Configuration
    static class EnterpriseLabConfiguration {
        @Bean
        EnterpriseLabRunService enterpriseLabRunService(
                AdaptiveRoutingPolicyAuditLog policyAuditLog,
                AdaptiveRoutingObservabilityMetrics observabilityMetrics) {
            return new EnterpriseLabRunService(
                    new com.richmond423.loadbalancerpro.lab.EnterpriseLabScenarioCatalogService(),
                    new com.richmond423.loadbalancerpro.core.AdaptiveRoutingExperimentService(),
                    java.time.Clock.fixed(java.time.Instant.parse("2026-05-14T00:00:00Z"),
                            java.time.ZoneOffset.UTC),
                    EnterpriseLabRunService.DEFAULT_MAX_RETAINED_RUNS,
                    EnterpriseLabRunService.DEFAULT_MAX_SCENARIOS_PER_RUN,
                    policyAuditLog,
                    observabilityMetrics);
        }
    }
}
